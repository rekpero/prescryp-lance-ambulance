package com.prescryp.lance.ambulance;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prescryp.lance.ambulance.Misc.RunTimePermission;
import com.prescryp.lance.ambulance.Misc.WorkaroundMapFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import ng.max.slideview.SlideView;

import static com.prescryp.lance.ambulance.Misc.Common.mLastLocation;

public class RideStartedActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {


    private static String TAG = "RideStartedActivity";

    private GoogleMap mMap;
    private LocationRequest mLocationRequest;

    private static final float DEFAULT_ZOOM = 15f;

    private String customerId = "", destination;

    private LatLng destinationLatLng, pickupLatLng;
    private int status = 0;

    private FusedLocationProviderClient mFusedLocationClient;

    private CircleImageView mCustomerProfileImage;
    private TextView mCustomerName, mRideStatusTextView;

    private SlideView mRideStatus;

    private Boolean isAssignedCustomerLocation = false;
    private Boolean isPickedUpCustomer = false;
    private float rideDistance;

    private TextView mDistanceLocation, mEtaLocation;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    private TextView mLocationName, mLocationHeader;
    private ImageView mLocationNavImg, mDistanceImg, mEtaImg;

    Marker pickupMarker, destinationMarker;
    int height = 100;
    int width = 100;
    private DatabaseReference assignedCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerPickupLocationRefListener;
    private ScrollView mScrollView;
    private ImageView mCallCustomer, mCallHelp;
    private String customerPhoneNumber, helpPhoneNumber = "+917679009722";

    private RunTimePermission photoRunTimePermission;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_started);
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

        mCustomerProfileImage = findViewById(R.id.customerProfileImage);
        mCustomerName = findViewById(R.id.customerName);

        mRideStatus = findViewById(R.id.rideStatus);/*
        mRideStatusTextView = findViewById(R.id.rideStatusText);*/

        mLocationName = findViewById(R.id.location_name);
        mLocationHeader = findViewById(R.id.location_header);
        mLocationNavImg = findViewById(R.id.location_nav_img);

        mDistanceImg = findViewById(R.id.distance_img);
        mEtaImg = findViewById(R.id.eta_img);

        mDistanceLocation = findViewById(R.id.distanceLocation);
        mEtaLocation = findViewById(R.id.etaLocation);

        mCallCustomer = findViewById(R.id.call_customer);
        mCallHelp = findViewById(R.id.call_help);

        polylines = new ArrayList<>();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        /*mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
*/
        SupportMapFragment mapFragment = (WorkaroundMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getAssignedCustomer();

/*
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status){
                    case 1:
                        status = 2;
                        erasePolylines();
                        if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0){
                            setCustomerDestinationLocation();
                        }
                        sendSendPickupNotification();
                        break;
                    case 2:
                        recordRide();
                        break;
                }

            }
        });
*/

        mRideStatus.setOnSlideCompleteListener(new SlideView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideView slideView) {
                // vibrate the device

                // go to a new activity
                switch (status){
                    case 1:
                        status = 2;
                        mRideStatus.setSlideBackgroundColor(ColorStateList.valueOf(R.color.red));
                        erasePolylines();
                        if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0){
                            setCustomerDestinationLocation();
                        }
                        sendSendPickupNotification();
                        break;
                    case 2:
                        recordRide();
                        break;
                }
            }
        });

        mLocationNavImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status){
                    case 1:
                        String pickup_nav_uri = "google.navigation:q=" + pickupLatLng.latitude + "," + pickupLatLng.longitude + "&avoid=tf";
                        Uri pickupIntentUri = Uri.parse(pickup_nav_uri);
                        Intent pickupIntent = new Intent(Intent.ACTION_VIEW, pickupIntentUri);
                        pickupIntent.setPackage("com.google.android.apps.maps");
                        startActivity(pickupIntent);
                        break;
                    case 2:
                        String drop_nav_uri = "google.navigation:q=" + destinationLatLng.latitude + "," + destinationLatLng.longitude + "&avoid=tf";
                        Uri dropIntentUri = Uri.parse(drop_nav_uri);
                        Intent dropIntent = new Intent(Intent.ACTION_VIEW, dropIntentUri);
                        dropIntent.setPackage("com.google.android.apps.maps");
                        startActivity(dropIntent);
                        break;
                }
            }
        });

        mCallCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_CALL);

                if (forWho.equalsIgnoreCase("For_Me")){
                    intent.setData(Uri.parse("tel:" + customerPhoneNumber));
                }else if (forWho.equalsIgnoreCase("For_Other")){
                    intent.setData(Uri.parse("tel:" + requestedMobNumOfOthers));
                }
                if (ActivityCompat.checkSelfPermission(RideStartedActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(intent);
                } else {
                    photoRunTimePermission = new RunTimePermission(RideStartedActivity.this);
                    photoRunTimePermission.requestPermission(new String[]{
                            Manifest.permission.CALL_PHONE
                    }, new RunTimePermission.RunTimePermissionListener() {

                        @Override
                        public void permissionGranted() {
                            if (ActivityCompat.checkSelfPermission(RideStartedActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                startActivity(intent);
                            }

                        }

                        @Override
                        public void permissionDenied() {
                        }
                    });
                }
            }
        });

        mCallHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_CALL);

                intent.setData(Uri.parse("tel:" + helpPhoneNumber));
                if (ActivityCompat.checkSelfPermission(RideStartedActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(intent);
                } else {
                    photoRunTimePermission = new RunTimePermission(RideStartedActivity.this);
                    photoRunTimePermission.requestPermission(new String[]{
                            Manifest.permission.CALL_PHONE
                    }, new RunTimePermission.RunTimePermissionListener() {

                        @Override
                        public void permissionGranted() {
                            if (ActivityCompat.checkSelfPermission(RideStartedActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                startActivity(intent);
                            }

                        }

                        @Override
                        public void permissionDenied() {
                        }
                    });
                }
            }
        });
    }

    private void sendSendPickupNotification() {
        DatabaseReference customerRideStatusRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("currentRequest");
        HashMap customer_map = new HashMap();
        customer_map.put("rideStatus", "Picked Up");
        customerRideStatusRef.updateChildren(customer_map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
/*
                mRideStatusTextView.setText("Drive Complete");*/
                mRideStatus.setText("DRIVE COMPLETE");
            }
        });
    }

    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    status = 1;
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedPickupLocation();
                    getAssignedInformation();
                    getAssignedPickupInfo();
                    getAssignedCustomerDestination();


                }else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private String paymentMethod, paid, forWho, requestedMobNumOfOthers;
    private void getAssignedInformation() {
        String driverId = FirebaseAuth.getInstance().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("paymentMethod") != null){
                        paymentMethod = map.get("paymentMethod").toString();
                    }
                    if (map.get("paid") != null){
                        paid = map.get("paid").toString();
                    }
                    if (map.get("forWho") != null){
                        forWho = map.get("forWho").toString();
                    }
                    if (map.get("requestedMobNumOfOthers") != null){
                        requestedMobNumOfOthers = map.get("requestedMobNumOfOthers").toString();
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    pickupLatLng = new LatLng(locationLat, locationLng);

                    setPickupLocationNavigation();

                    BitmapDrawable greenPinBitmap = (BitmapDrawable) getResources().getDrawable(R.mipmap.ic_green_pin_foreground);
                    Bitmap gb = greenPinBitmap.getBitmap();
                    Bitmap smallGreenPinMarker = Bitmap.createScaledBitmap(gb, width, height, false);

                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("pickup location").icon(BitmapDescriptorFactory.fromBitmap(smallGreenPinMarker)));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, DEFAULT_ZOOM));

                    isAssignedCustomerLocation = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setPickupLocationNavigation() {
        mLocationHeader.setText("Pickup at");
        mLocationName.setText(getLocationName(pickupLatLng));
        mLocationNavImg.setImageResource(R.drawable.green_nav);

        mDistanceImg.setImageResource(R.drawable.green_distance);
        mEtaImg.setImageResource(R.drawable.green_eta);
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

    private void getAssignedPickupInfo(){
        DatabaseReference mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null){
                        mCustomerName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null){
                        customerPhoneNumber = map.get("phone").toString();
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

    private void getAssignedCustomerDestination() {
        String driverId = FirebaseAuth.getInstance().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("destinationName") != null){
                        destination = map.get("destinationName").toString();

                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;

                    if (map.get("destinationLatitude") != null){
                        destinationLat = Double.valueOf(map.get("destinationLatitude").toString());
                    }
                    if (map.get("destinationLongitude") != null){
                        destinationLng = Double.valueOf(map.get("destinationLongitude").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setCustomerDestinationLocation() {
        BitmapDrawable redPinBitmap = (BitmapDrawable) getResources().getDrawable(R.mipmap.ic_red_pin_foreground);
        Bitmap rb = redPinBitmap.getBitmap();
        Bitmap smallRedPinMarker = Bitmap.createScaledBitmap(rb, width, height, false);

        if (pickupMarker != null){
            pickupMarker.remove();
        }

        setDestinationLocationNavigation();

        destinationMarker = mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Drop Here").icon(BitmapDescriptorFactory.fromBitmap(smallRedPinMarker)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, DEFAULT_ZOOM));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                Log.e(TAG, "Final location: lat " + mMap.getCameraPosition().target.latitude + " lon " + mMap.getCameraPosition().target.longitude);

            }
        });

        isPickedUpCustomer = true;
        firstRoute = true;

    }

    private void setDestinationLocationNavigation() {
        mLocationHeader.setText("Drop at");
        mLocationName.setText(getLocationName(destinationLatLng));
        mLocationNavImg.setImageResource(R.drawable.red_nav);

        mDistanceImg.setImageResource(R.drawable.red_distance);
        mEtaImg.setImageResource(R.drawable.red_eta);
    }

    private void endRide() {
        erasePolylines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driveRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
        driveRef.removeValue();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("customerRequest").child(customerId);
        reference.removeValue();

        DatabaseReference customerRequestRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("currentRequest");
        customerRequestRef.removeValue();

        customerId = "";
        rideDistance = 0;

        if (pickupMarker != null){
            pickupMarker.remove();
        }
        if (destinationMarker != null){
            destinationMarker.remove();
        }

        if (assignedCustomerPickupLocationRefListener != null){
            assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
        }
        mCustomerName.setText("");
        mCustomerProfileImage.setImageResource(R.drawable.user_black);

        mRideStatus.setVisibility(View.GONE);


        isAssignedCustomerLocation = false;
        isPickedUpCustomer = false;

        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("diverWorking").child(userId);
        ref.removeValue();

        Intent intent = new Intent(RideStartedActivity.this, BookAmbulanceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private String rideId;
    private void recordRide() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driveRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");

        rideId = historyRef.push().getKey();
        driveRef.child(rideId).setValue(true);
        customerRef.child(rideId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", customerId);
        map.put("rating", 0);
        map.put("ratingComment", "No Comment");
        map.put("timestamp", getCurrentTimestamp());
        map.put("destination", destination);
        map.put("location/from/lat", pickupLatLng.latitude);
        map.put("location/from/lng", pickupLatLng.longitude);
        map.put("location/to/lat", destinationLatLng.latitude);
        map.put("location/to/lng", destinationLatLng.longitude);
        map.put("distance", rideDistance);
        map.put("customerPaid", true);
        historyRef.child(rideId).updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                sendRequestToCustomer();
            }
        });
    }

    private void sendRequestToCustomer() {

        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference().child("notifications");
        String requestId = notificationsRef.push().getKey();

        DatabaseReference driverNotificationRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("Notification");
        driverNotificationRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("from", driverId);
        map.put("rideStatus", "Dropped");
        map.put("rideId", rideId);
        map.put("title", "You have reached your destination");
        map.put("body", "You have reached your destination. Thank you for using our services. Rate us for your experience.");
        map.put("click", "com.prescryp.lance.RIDERATING");


        notificationsRef.child(requestId).updateChildren(map).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()){
                    endRide();
                }
            }
        });

    }


    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }

    private void getRouteToMarker(LatLng pickupLatLng) {
        if (mLastLocation != null && pickupLatLng != null){
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .key("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                    .build();
            routing.execute();
        }
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



            mDistanceLocation.setText(route.get(i).getDistanceText());
            mEtaLocation.setText(route.get(i).getDurationText());
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(false);

        mScrollView = findViewById(R.id.scrollView); //parent scrollview in xml, give your scrollview id value
        ((WorkaroundMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .setListener(new WorkaroundMapFragment.OnTouchListener() {
                    @Override
                    public void onTouch()
                    {
                        mScrollView.requestDisallowInterceptTouchEvent(true);
                    }
                });

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }else {
                checkLocationPermission();
            }
        }


        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    private Boolean firstLocation = false, firstRoute = true;
    private float distanceTraveled = 0;

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()){
                if (getApplicationContext() != null){

                    if (!customerId.equals("")){
                        rideDistance += mLastLocation.distanceTo(location)/1000;
                        distanceTraveled = rideDistance*1000;
                    }

                    mLastLocation = location;


                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("diverWorking");
                    GeoFire geoFireWorking = new GeoFire(refWorking);

                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener(){

                        @Override
                        public void onComplete(String key, DatabaseError error) {

                        }
                    });

                    if (isAssignedCustomerLocation){
                        if (distanceTraveled > 100 || firstRoute){
                            if (isPickedUpCustomer){
                                getRouteToMarker(destinationLatLng);
                            }else {
                                getRouteToMarker(pickupLatLng);
                            }
                            if (!firstRoute){
                                distanceTraveled = 0;
                            }
                            firstRoute = false;
                        }

                    }else {
                        erasePolylines();
                    }


                }
            }
        }
    };


    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(RideStartedActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        }).create()
                        .show();
            }
            else {
                ActivityCompat.requestPermissions(RideStartedActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_SHORT).show();
                }
        }
    }


}
