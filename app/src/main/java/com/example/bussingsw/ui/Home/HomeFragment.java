package com.example.bussingsw.ui.Home;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bussingsw.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ScannedTicketAdapter adapter;
    private ArrayList<ScannedTicketLists> scannedTickets;

    private TextView walletBalance, totalRevenue, passengerCount, scannedTicketAmount;
    private ImageView download;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private int scannedPassengerCount = 0;
    private double totalTicketAmount = 0;

    private ListenerRegistration verifiedTicketsListener;
    private ListenerRegistration ticketGeneratedListener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = root.findViewById(R.id.scannedTicketRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        scannedTickets = new ArrayList<>();
        adapter = new ScannedTicketAdapter(getContext(), scannedTickets);
        recyclerView.setAdapter(adapter);

        walletBalance = root.findViewById(R.id.walletBalance);
        totalRevenue = root.findViewById(R.id.totalRevenue);
        passengerCount = root.findViewById(R.id.passengerCount);
        scannedTicketAmount = root.findViewById(R.id.scannedTicketAmount);
        download = root.findViewById(R.id.download);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Set up download button click listener
        download.setOnClickListener(v -> {
            String data = "Scanned Passengers: " + scannedPassengerCount + "\n" +
                    "Total Amount: ₱" + String.format("%.2f", totalTicketAmount);

            // Save data to a .txt file
            saveDataToFile();
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (auth.getCurrentUser() == null) return;
        String currentDriverUid = auth.getCurrentUser().getUid();

        // Detach if already attached
        if (verifiedTicketsListener != null) verifiedTicketsListener.remove();
        if (ticketGeneratedListener != null) ticketGeneratedListener.remove();

        // Load tickets scanned by the driver
        verifiedTicketsListener = db.collection("VerifiedTicketsCollection")
                .whereEqualTo("uid", currentDriverUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "VerifiedTickets listen failed.", error);
                        return;
                    }

                    if (snapshots != null) {
                        scannedTickets.clear();
                        scannedPassengerCount = 0;
                        totalTicketAmount = 0;

                        String todayDate = new SimpleDateFormat("ddMMMMyyyy", Locale.getDefault())
                                .format(Calendar.getInstance().getTime());

                        double todayTotalPrice = 0;
                        int todayScannedCount = 0;

                        for (QueryDocumentSnapshot doc : snapshots) {
                            String ticketCode = doc.getId();
                            Map<String, Object> ticketData = doc.getData();

                            String priceStr = (String) ticketData.get("price");
                            double price = 0;
                            try {
                                price = Double.parseDouble(priceStr);
                            } catch (Exception e) {
                                Log.w("ParsePrice", "Failed to parse price: " + priceStr);
                            }

                            totalTicketAmount += price;
                            scannedPassengerCount++;

                            String bookingDate = (String) ticketData.get("bookingDate");
                            SimpleDateFormat dbFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                            SimpleDateFormat compareFormat = new SimpleDateFormat("ddMMMMyyyy", Locale.getDefault());

                            if (bookingDate != null) {
                                try {
                                    Date parsedBookingDate = dbFormat.parse(bookingDate);
                                    String normalizedBookingDate = compareFormat.format(parsedBookingDate);

                                    if (todayDate.equals(normalizedBookingDate)) {
                                        todayTotalPrice += price;
                                        todayScannedCount++;
                                    }
                                } catch (ParseException e) {
                                    Log.w("DateParse", "Failed to parse bookingDate: " + bookingDate, e);
                                }
                            }

                            ScannedTicketLists ticket = new ScannedTicketLists(
                                    (String) ticketData.get("from"),
                                    (String) ticketData.get("to"),
                                    bookingDate,
                                    (String) ticketData.get("bookingTime"),
                                    (String) ticketData.get("passenger"),
                                    (String) ticketData.get("discount"),
                                    ticketCode,
                                    priceStr
                            );

                            scannedTickets.add(ticket);
                        }

                        adapter.notifyDataSetChanged();
                        walletBalance.setText("₱" + String.format("%.2f", totalTicketAmount));
                        passengerCount.setText(String.valueOf(todayScannedCount));
                        scannedTicketAmount.setText("₱" + String.format("%.2f", todayTotalPrice));
                    }
                });

        // Load revenue from all generated tickets
        ticketGeneratedListener = db.collection("TicketGeneratedCollection")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "TicketGenerated listen failed.", error);
                        return;
                    }

                    if (snapshots != null) {
                        double totalRevenueEarned = 0;

                        for (QueryDocumentSnapshot doc : snapshots) {
                            Object priceObj = doc.get("price");

                            if (priceObj instanceof Number) {
                                totalRevenueEarned += ((Number) priceObj).doubleValue();
                            } else if (priceObj instanceof String) {
                                try {
                                    totalRevenueEarned += Double.parseDouble((String) priceObj);
                                } catch (NumberFormatException e) {
                                    Log.w("ParsePrice", "Invalid price string: " + priceObj);
                                }
                            }
                        }

                        totalRevenue.setText("₱" + String.format("%.2f", totalRevenueEarned));
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (verifiedTicketsListener != null) {
            verifiedTicketsListener.remove();
            verifiedTicketsListener = null;
        }
        if (ticketGeneratedListener != null) {
            ticketGeneratedListener.remove();
            ticketGeneratedListener = null;
        }
    }

    private void saveDataToFile() {
        try {
            // Get the current date for the filename (e.g., "tickets_22Apr2025.txt")
            String date = new SimpleDateFormat("ddMMMMyyyy").format(Calendar.getInstance().getTime());
            String todayDate = new SimpleDateFormat("ddMMMMyyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());

            // Get the Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            // Create a file in the Downloads folder
            File file = new File(downloadsDir, "tickets_" + date + ".txt");

            // Prepare data to save: only tickets scanned today
            StringBuilder data = new StringBuilder();
            data.append("Scanned Tickets for Today\n");

            double totalPriceToday = 0;
            int todayTicketCount = 0;

            for (ScannedTicketLists ticket : scannedTickets) {
                // Check if the ticket's booking date is today
                String bookingDate = ticket.getBookingDate();
                SimpleDateFormat dbFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                SimpleDateFormat compareFormat = new SimpleDateFormat("ddMMMMyyyy", Locale.getDefault());

                if (bookingDate != null) {
                    try {
                        Date parsedBookingDate = dbFormat.parse(bookingDate);
                        String normalizedBookingDate = compareFormat.format(parsedBookingDate);

                        if (todayDate.equals(normalizedBookingDate)) {
                            // Add ticket details to data
                            data.append("From: ").append(ticket.getTicketFrom()).append("\n")
                                    .append("To: ").append(ticket.getTicketTo()).append("\n")
                                    .append("Passenger: ").append(ticket.getPassengerType()).append("\n")
                                    .append("Price: P").append(ticket.getTicketPrice()).append("\n\n");

                            totalPriceToday += Double.parseDouble(ticket.getTicketPrice());
                            todayTicketCount++;
                        }
                    } catch (ParseException e) {
                        Log.w("DateParse", "Failed to parse bookingDate: " + bookingDate, e);
                    }
                }
            }

            // Append total summary
            data.append("Total Tickets Scanned Today: ").append(todayTicketCount).append("\n");
            data.append("Total Price: P").append(String.format("%.2f", totalPriceToday));

            // Write data to the file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data.toString().getBytes());
            fos.close();

            Toast.makeText(getContext(), "Data saved as " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("FileError", "Error saving file", e);
            Toast.makeText(getContext(), "Failed to save data", Toast.LENGTH_SHORT).show();
        }
    }

}

