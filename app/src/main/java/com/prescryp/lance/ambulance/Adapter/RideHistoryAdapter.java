package com.prescryp.lance.ambulance.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.prescryp.lance.ambulance.Model.RideHistoryItem;
import com.prescryp.lance.ambulance.R;
import com.prescryp.lance.ambulance.RideHistorySingleActivity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.ViewHolder> {

    private List<RideHistoryItem> listItems;
    private Context context;

    public RideHistoryAdapter(List<RideHistoryItem> listItems, Context context) {
        this.listItems = listItems;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ride_history_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final RideHistoryItem listItem = listItems.get(position);

        holder.dateOfRide.setText(listItem.getDateOfRide());
        holder.customerName.setText(listItem.getCustomerName());
        holder.distance.setText(listItem.getDistance().substring(0, Math.min(listItem.getDistance().length(), 5)) + " km");
        holder.pickupLocationName.setText(listItem.getPickupLocationName());
        holder.destinationLocationName.setText(listItem.getDestinationLocationName());
        if (listItem.getCustomerProfileImageUrl() != ""){
            Glide.with(context).load(listItem.getCustomerProfileImageUrl()).into(holder.customerProfileImage);
        }
        holder.rideRating.setRating(Integer.valueOf(listItem.getRideRating()));


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, RideHistorySingleActivity.class);
                intent.putExtra("rideId", listItem.getRideId());
                context.startActivity(intent);
            }
        });


    }


    @Override
    public int getItemCount() {
        return listItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView dateOfRide, customerName, distance, pickupLocationName, destinationLocationName;
        private CircleImageView customerProfileImage;
        private RatingBar rideRating;

        public ViewHolder(View itemview){
            super(itemview);

            dateOfRide = itemview.findViewById(R.id.dateOfRide);
            customerProfileImage = itemview.findViewById(R.id.customerProfileImage);
            customerName = itemview.findViewById(R.id.customerName);
            rideRating = itemview.findViewById(R.id.rideRating);
            distance = itemview.findViewById(R.id.distance);
            pickupLocationName = itemview.findViewById(R.id.pickupLocationName);
            destinationLocationName = itemview.findViewById(R.id.destinationLocationName);
        }
    }
}
