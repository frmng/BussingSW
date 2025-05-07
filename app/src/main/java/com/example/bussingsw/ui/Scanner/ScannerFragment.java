package com.example.bussingsw.ui.Scanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bussingsw.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerFragment extends Fragment {

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private static final int CAMERA_REQUEST_CODE = 1001;
    private boolean isScanning = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_scanner, container, false);
        previewView = root.findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            startCamera();
        }

        return root;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Log.e("ScannerFragment", "Camera permission denied");
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("ScannerFragment", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::scanBarcode);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e("ScannerFragment", "Error binding camera", e);
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void scanBarcode(ImageProxy imageProxy) {
        if (!isScanning || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        BarcodeScanning.getClient().process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        Barcode barcode = barcodes.get(0);
                        String rawValue = barcode.getRawValue();

                        Log.d("QRCode", "Scanned rawValue: [" + rawValue + "]");

                        if (rawValue == null) {
                            imageProxy.close();
                            return;
                        }

                        final String[] ticketIdHolder = new String[1]; // effectively final

                        for (String line : rawValue.split("\n")) {
                            if (line.trim().startsWith("Ticket Number:")) {
                                String[] parts = line.split(":");
                                if (parts.length >= 2) {
                                    ticketIdHolder[0] = parts[1].trim();
                                }
                                break;
                            }
                        }

                        if (ticketIdHolder[0] == null || ticketIdHolder[0].isEmpty()) {
                            Toast.makeText(requireContext(), "Invalid QR code format", Toast.LENGTH_SHORT).show();
                            imageProxy.close();
                            return;
                        }

                        isScanning = false;
                        Log.d("QRCode", "Ticket ID: " + ticketIdHolder[0]);
                        Toast.makeText(requireContext(), "Scanned: " + ticketIdHolder[0], Toast.LENGTH_SHORT).show();

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        String currentUid = auth.getCurrentUser().getUid();

                        db.collection("TicketGeneratedCollection")
                                .whereEqualTo("ticketCode", ticketIdHolder[0])  // Match ticket number (ticketCode)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                        Map<String, Object> ticketData = document.getData();
                                        if (ticketData == null) return;

                                        ticketData.put("scannedAt", Timestamp.now());
                                        ticketData.put("uid", currentUid);

                                        String formattedDate = new SimpleDateFormat("HH:mm ddMMMyy", Locale.getDefault())
                                                .format(new Date());
                                        ticketData.put("scannedAtFormatted", formattedDate);

                                        // Use the ticket number (ticketIdHolder[0]) for VerifiedTicketsCollection document ID
                                        String ticketNumber = ticketIdHolder[0];

                                        db.collection("VerifiedTicketsCollection").document(ticketNumber) // Use ticket number as document ID
                                                .get()
                                                .addOnSuccessListener(verifiedDoc -> {
                                                    if (verifiedDoc.exists()) {
                                                        Toast.makeText(requireContext(), "Ticket already scanned", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        db.collection("VerifiedTicketsCollection").document(ticketNumber)
                                                                .set(ticketData)
                                                                .addOnSuccessListener(unused -> {
                                                                    Log.d("Scanner", "Ticket verified and saved.");
                                                                    Toast.makeText(requireContext(), "Ticket verified", Toast.LENGTH_SHORT).show();

                                                                    // ✅ Send notification to user
                                                                    String userId = (String) ticketData.get("userId"); // Make sure userId exists
                                                                    if (userId != null && !userId.isEmpty()) {
                                                                        Map<String, Object> notification = new java.util.HashMap<>();
                                                                        notification.put("message", "Your ticket " + ticketNumber + " has been scanned and verified.");
                                                                        notification.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                                                                        FirebaseFirestore.getInstance()
                                                                                .collection("Notifications")
                                                                                .document(userId)
                                                                                .collection("UserNotifications")
                                                                                .add(notification)
                                                                                .addOnSuccessListener(docRef -> {
                                                                                    Log.d("Notification", "Notification sent to user: " + userId);
                                                                                })
                                                                                .addOnFailureListener(e -> {
                                                                                    Log.w("Notification", "Failed to send notification", e);
                                                                                });
                                                                    } else {
                                                                        Log.w("Notification", "User ID not found in ticketData");
                                                                    }
                                                                })

                                                                .addOnFailureListener(e -> Log.e("Scanner", "Failed to save ticket", e));
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(requireContext(), "Ticket not found", Toast.LENGTH_SHORT).show();
                                        Log.w("Firestore", "No ticket matching code: " + ticketIdHolder[0]);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), "Error fetching ticket", Toast.LENGTH_SHORT).show();
                                    Log.e("Scanner", "Error fetching ticket", e);
                                });

                        new Handler(Looper.getMainLooper()).postDelayed(() -> isScanning = true, 3000);
                    }
                })
                .addOnFailureListener(e -> Log.e("QRCode", "Scanning failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}