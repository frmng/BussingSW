package com.example.bussingsw.ui.Home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bussingsw.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ScannedTicketAdapter extends RecyclerView.Adapter<ScannedTicketAdapter.CardViewHolder> {

    private Context context;
    private ArrayList<ScannedTicketLists> scannedTicketListsArrayList;
    private FirebaseFirestore db;

    public ScannedTicketAdapter(Context context, ArrayList<ScannedTicketLists> scannedTicketLists) {
        this.context = context;
        this.scannedTicketListsArrayList = scannedTicketLists;
        this.db = FirebaseFirestore.getInstance();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public TextView ticketNo, scannedFrom, scannedTo, date, time, passengerType, discount, total;
        public LinearLayout detailsLayout;
        public ImageView dropdownImage;

        public CardViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.cardScannedTicket);
            ticketNo = view.findViewById(R.id.ticketNo);
            scannedFrom = view.findViewById(R.id.from);
            scannedTo = view.findViewById(R.id.toWhere);
            date = view.findViewById(R.id.bookingDate);
            time = view.findViewById(R.id.bookingTime);
            passengerType = view.findViewById(R.id.passenger);
            discount = view.findViewById(R.id.discount);
            total = view.findViewById(R.id.totalPrice);
            detailsLayout = view.findViewById(R.id.details);
            dropdownImage = view.findViewById(R.id.dropdown);
        }
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.scanned_ticket_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        ScannedTicketLists ticket = scannedTicketListsArrayList.get(position);

        holder.ticketNo.setText(ticket.getTicketNo());
        holder.scannedFrom.setText(ticket.getTicketFrom());
        holder.scannedTo.setText(ticket.getTicketTo());
        holder.date.setText(ticket.getBookingDate());
        holder.time.setText(ticket.getBookingTime());
        holder.passengerType.setText(ticket.getPassengerType());
        holder.discount.setText("₱" + ticket.getDiscount());
        holder.total.setText("₱" + ticket.getTicketPrice());

        // Initially hide details
        holder.detailsLayout.setVisibility(View.GONE);
        holder.detailsLayout.getLayoutParams().height = 0;

        // Toggle dropdown
        holder.cardView.setOnClickListener(v -> {
            if (holder.detailsLayout.getVisibility() == View.GONE) {
                holder.detailsLayout.setVisibility(View.VISIBLE);
                holder.detailsLayout.measure(View.MeasureSpec.makeMeasureSpec(holder.itemView.getWidth(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                int targetHeight = holder.detailsLayout.getMeasuredHeight();

                ValueAnimator expandAnimator = ValueAnimator.ofInt(0, targetHeight);
                expandAnimator.addUpdateListener(valueAnimator -> {
                    holder.detailsLayout.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
                    holder.detailsLayout.requestLayout();
                });
                expandAnimator.setDuration(300);
                expandAnimator.start();

                ObjectAnimator.ofFloat(holder.dropdownImage, "rotation", 0f, 180f).setDuration(300).start();
            } else {
                ValueAnimator collapseAnimator = ValueAnimator.ofInt(holder.detailsLayout.getHeight(), 0);
                collapseAnimator.addUpdateListener(valueAnimator -> {
                    holder.detailsLayout.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
                    holder.detailsLayout.requestLayout();
                });
                collapseAnimator.setDuration(300);
                collapseAnimator.start();

                collapseAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        holder.detailsLayout.setVisibility(View.GONE);
                        holder.detailsLayout.getLayoutParams().height = 0;
                        holder.detailsLayout.requestLayout();
                    }
                });

                ObjectAnimator.ofFloat(holder.dropdownImage, "rotation", 180f, 0f).setDuration(300).start();
            }
        });
    }

    @Override
    public int getItemCount() {
        return scannedTicketListsArrayList != null ? scannedTicketListsArrayList.size() : 0;
    }
}
