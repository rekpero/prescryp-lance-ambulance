package com.prescryp.lance.ambulance;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prescryp.lance.ambulance.Adapter.RideHistoryAdapter;
import com.prescryp.lance.ambulance.Model.RideHistoryItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RideHistoryActivity extends AppCompatActivity {

    private RecyclerView mRideHistoryRecyclerView;
    private String userId;
    private RideHistoryAdapter mRideHistoryAdapter;
    private TextView noRide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_history);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        noRide = findViewById(R.id.noRide);

        mRideHistoryRecyclerView = findViewById(R.id.rideHistoryRecyclerView);
        mRideHistoryRecyclerView.setHasFixedSize(true);
        mRideHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mRideHistoryAdapter = new RideHistoryAdapter(getRideHistory(), getApplicationContext());
        mRideHistoryRecyclerView.setAdapter(mRideHistoryAdapter);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserRideHistoryId();
    }

    private void getUserRideHistoryId() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    noRide.setVisibility(View.GONE);
                    for (DataSnapshot history: dataSnapshot.getChildren()){
                        fetchRideInformation(history.getKey());
                    }
                }else {
                    noRide.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void fetchRideInformation(String rideKey) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(rideKey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String rideId = dataSnapshot.getKey();
                    Long timestamp = 0L;
                    String customerId = "", rating = "", distance = "";
                    LatLng pickupLatLng = new LatLng(0,0), destinationLatLng = new LatLng(0,0);
                    for (DataSnapshot child : dataSnapshot.getChildren()){
                        if (child.getKey().equals("timestamp")){
                            timestamp = Long.valueOf(child.getValue().toString());
                        }
                        if (child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                        }
                        if (child.getKey().equals("rating")){
                            rating = child.getValue().toString();
                        }
                        if (child.getKey().equals("distance")){
                            distance = child.getValue().toString();
                        }
                        if (child.getKey().equals("location")){
                            pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()), Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()), Double.valueOf(child.child("to").child("lng").getValue().toString()));
                        }
                    }

                    getCustomerInformation(rideId, timestamp, customerId, rating, distance, pickupLatLng, destinationLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getCustomerInformation(final String rideId, final Long timestamp, String customerId, final String rating, final String distance, final LatLng pickupLatLng, final LatLng destinationLatLng) {
        DatabaseReference customerUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        customerUserDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    String customerName = "", customerProfileImageUrl = "";
                    if (map.get("name") != null){
                        customerName = map.get("name").toString();
                    }
                    if (map.get("profileImageUrl") != null){
                        customerProfileImageUrl = map.get("profileImageUrl").toString();
                    }

                    RideHistoryItem historyItem = new RideHistoryItem(rideId, timestamp, getDate(timestamp), customerProfileImageUrl, customerName, rating, distance, getLocationName(pickupLatLng), getLocationName(destinationLatLng));

                    resultsHistory.add(historyItem);

                    Collections.sort(resultsHistory, new Comparator<RideHistoryItem>() {
                        @Override
                        public int compare(RideHistoryItem o1, RideHistoryItem o2) {
                            return new Date(o2.getTimestamp()).compareTo(new Date(o1.getTimestamp()));
                        }
                    });

                    mRideHistoryAdapter.notifyDataSetChanged();
                    if (resultsHistory.isEmpty()){
                        noRide.setVisibility(View.VISIBLE);
                    }else {
                        noRide.setVisibility(View.GONE);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private String getDate(Long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(timestamp*1000);
        String date = DateFormat.format("dd/MM/yyyy, HH:mm", calendar).toString();
        return date;
    }

    Geocoder geocoder;
    List<Address> addresses;
    String locationAddress;

    private String  getLocationName(LatLng locationLatLng) {
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(locationLatLng.latitude, locationLatLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            locationAddress = addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return locationAddress;
    }

    private ArrayList resultsHistory = new ArrayList<RideHistoryActivity>();
    private ArrayList<RideHistoryItem> getRideHistory() {
        return resultsHistory;
    }

}
