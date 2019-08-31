package com.prescryp.lance.ambulance;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class RideHistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private String rideId, currentUserId, customerId;
    private DatabaseReference historyRideInfoDb;

    private TextView mDateOfRide, mDistance, mPickupLocationName, mDestinationLocationName, mCustomerName;
    private CircleImageView mCustomerProfileImage;

    private LatLng pickupLatLng, destinationLatLng;
    private RatingBar mRatingBar;

    private String distance;
    private Double ridePrice;

    int markerHeight = 64;
    int markerWidth = 64;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_history_single);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        if (getIntent() != null){
            rideId = getIntent().getStringExtra("rideId");
        }

        polylines = new ArrayList<>();

        mDateOfRide = findViewById(R.id.dateOfRide);
        mDistance = findViewById(R.id.distance);
        mPickupLocationName = findViewById(R.id.pickupLocationName);
        mDestinationLocationName = findViewById(R.id.destinationLocationName);
        mCustomerProfileImage = findViewById(R.id.customerProfileImage);
        mCustomerName = findViewById(R.id.customerName);
        mRatingBar = findViewById(R.id.rideRating);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getRideInformation();

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

    }

    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot child : dataSnapshot.getChildren()){
                        if (child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                            getCustomerInformation(customerId);
                            getDriverRelatedObject();
                        }
                        if (child.getKey().equals("timestamp")){
                            mDateOfRide.setText(getDate(Long.valueOf(child.getValue().toString())));
                            toolbar.setSubtitle(getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if (child.getKey().equals("rating")){
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }
                        if (child.getKey().equals("distance")){
                            distance = child.getValue().toString();
                            mDistance.setText(distance.substring(0, Math.min(distance.length(), 5)) + " km");
                        }
                        if (child.getKey().equals("location")){
                            pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()), Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()), Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if (destinationLatLng != new LatLng(0,0)){
                                getRouteToMarker();
                            }

                            mPickupLocationName.setText(getLocationName(pickupLatLng));
                            mDestinationLocationName.setText(getLocationName(destinationLatLng));
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getDriverRelatedObject() {
        mRatingBar.setVisibility(View.VISIBLE);
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

    private void getCustomerInformation(String customerId) {
        DatabaseReference driverUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        driverUserDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null){
                        String customer_name_text = map.get("name").toString();
                        mCustomerName.setText(customer_name_text);
                    }

                    if (map.get("profileImageUrl") != null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }


    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.themeBlue};


    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .key("AIzaSyA6WTOATbfP_G36XKBzRyYVRM0D9PHNGb0")
                .waypoints(pickupLatLng, destinationLatLng)
                .build();
        routing.execute();
    }


    @Override
    public void onRoutingFailure(RouteException e) {
        // The Routing request failed
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLatLng);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width*0.1);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cameraUpdate);

        BitmapDrawable greenPinBitmap = (BitmapDrawable) getResources().getDrawable(R.mipmap.ic_green_pin_foreground);
        Bitmap gb = greenPinBitmap.getBitmap();
        Bitmap smallGreenPinMarker = Bitmap.createScaledBitmap(gb, markerWidth, markerHeight, false);

        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location").icon(BitmapDescriptorFactory.fromBitmap(smallGreenPinMarker)));

        BitmapDrawable redPinBitmap = (BitmapDrawable) getResources().getDrawable(R.mipmap.ic_red_pin_foreground);
        Bitmap rb = redPinBitmap.getBitmap();
        Bitmap smallRedPinMarker = Bitmap.createScaledBitmap(rb, markerWidth, markerHeight, false);

        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Drop Here").icon(BitmapDescriptorFactory.fromBitmap(smallRedPinMarker)));


        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            //Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void erasePolylines(){
        for (Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}
