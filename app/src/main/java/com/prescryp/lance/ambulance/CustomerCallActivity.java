package com.prescryp.lance.ambulance;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prescryp.lance.ambulance.Misc.ProgressBarAnimation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class CustomerCallActivity extends AppCompatActivity {

    private TextView txtTime, txtDistance, txtAddress;
    private MediaPlayer mediaPlayer;
    private CardView confirm, decline;
    private String customerId;
    private TextView mTimer;
    private String ridePrice;
    private TextView mRidePriceText;
    private ConstraintLayout mRidePriceEstimateLayout;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_call);

        txtTime = findViewById(R.id.txtTime);
        txtDistance = findViewById(R.id.txtDistance);
        txtAddress = findViewById(R.id.txtAddress);
        confirm = findViewById(R.id.confirm);
        decline = findViewById(R.id.decline);
        mTimer = findViewById(R.id.timer);
        mRidePriceText = findViewById(R.id.ridePrice);
        mRidePriceEstimateLayout = findViewById(R.id.ridePriceEstimateLayout);
        mProgressBar = findViewById(R.id.progressBar);

        mediaPlayer = MediaPlayer.create(this, R.raw.siren_ambulance);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        if (getIntent() != null){
            ridePrice = getIntent().getStringExtra("ridePrice");
        }

        if (ridePrice != null){
            mRidePriceText.setText(ridePrice);
            mRidePriceEstimateLayout.setVisibility(View.VISIBLE);
        }else {
            mRidePriceEstimateLayout.setVisibility(View.GONE);
        }

        getAssignedCustomer();

        startTimer();

        ProgressBarAnimation anim = new ProgressBarAnimation(mProgressBar, 0, 100);
        anim.setDuration(30000);
        mProgressBar.startAnimation(anim);


        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatabaseReference confirmCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("currentRequest").child("rideStatus");
                confirmCustomerRef.setValue("Confirmed").addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            sendRequestToCustomer("Confirmed", "Your ride has been confirmed", "You can reach the ambulance for live updates.", "com.prescryp.lance.RIDESTARTED");
                        }
                    }
                });
            }
        });

        decline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatabaseReference confirmCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("currentRequest").child("rideStatus");
                confirmCustomerRef.setValue("Declined").addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            sendRequestToCustomer("Declined", "Your ride has been declined", "You are continuing to search for ambulance for you.", "com.prescryp.lance.RIDESTARTED");
                        }
                    }
                });
            }
        });

    }
    private void sendRequestToCustomer(final String rideStatus, String rideStatusTitle, String rideStatusMessage, String rideStatusClick) {
        if (customerId != null){
            String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference().child("notifications");
            String requestId = notificationsRef.push().getKey();

            DatabaseReference driverNotificationRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("Notification");
            driverNotificationRef.child(requestId).setValue(true);

            HashMap map = new HashMap();
            map.put("from", driverId);
            map.put("rideStatus", rideStatus);
            map.put("rideId", null);
            map.put("title", rideStatusTitle);
            map.put("body", rideStatusMessage);
            map.put("click", rideStatusClick);

            notificationsRef.child(requestId).updateChildren(map).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()){
                        mediaPlayer.stop();
                        if(countDownTimer != null) {
                            countDownTimer.cancel();
                            countDownTimer = null;
                        }
                        if (rideStatus.equals("Confirmed")){
                            Intent intent = new Intent(CustomerCallActivity.this, RideStartedActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }else if (rideStatus.equals("Declined")){
                            endRide();
                        }

                    }
                }
            });

        }


    }


    @Override
    protected void onStop() {
        super.onStop();
        if(countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void endRide() {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driveRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
        driveRef.removeValue();

        DatabaseReference customerRequestRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("currentRequest");
        customerRequestRef.removeValue();

        customerId = "";

        mediaPlayer.stop();

        Intent intent = new Intent(CustomerCallActivity.this, BookAmbulanceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }


    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    customerId = dataSnapshot.getValue().toString();
                    final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("currentRequest").child("rideStatus");
                    assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()){

                                String rideStatus = dataSnapshot.getValue().toString();

                                if (rideStatus.equalsIgnoreCase("Confirmed")){
                                    mediaPlayer.stop();
                                    Intent intent = new Intent(CustomerCallActivity.this, RideStartedActivity.class);
                                    startActivity(intent);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private CountDownTimer countDownTimer;
    private long timeLeftInMilliSeconds = 30000; // 30sec
    private long timeRequiredInMilliSeconds = 30000; // 30sec
    private void startTimer() {
        timeLeftInMilliSeconds = 30000;
        countDownTimer = new CountDownTimer(timeLeftInMilliSeconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMilliSeconds = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {
                sendRequestToCustomer("Declined", "Your ride has been declined", "You are continuing to search for ambulance for you.", "com.prescryp.lance.RIDESTARTED");
            }
        }.start();
    }

    private void updateProgressBar() {
        float progressDone = (1-(timeLeftInMilliSeconds/timeRequiredInMilliSeconds))*100;
        mProgressBar.setProgress((int) progressDone);
    }

    private void updateTimer() {
        int second = (int) timeLeftInMilliSeconds / 1000;
        String timeLeftText;

        timeLeftText = "00:";
        if (second < 10) timeLeftText += "0";
        timeLeftText += second;
        String timeLeftFullText = timeLeftText;
        mTimer.setText(timeLeftFullText);

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
}
