package com.prescryp.lance.ambulance;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.prescryp.lance.ambulance.Misc.RunTimePermission;
import com.prescryp.lance.ambulance.Session.MobileNumberSessionManager;

import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.prescryp.lance.ambulance.Misc.Common.mLastLocation;

public class BookAmbulanceActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private static String TAG = "BookAmbulanceActivity";

    private static final float DEFAULT_ZOOM = 15f;


    private FusedLocationProviderClient mFusedLocationClient;

    private Boolean isLoggingOut = false;

    private static final String MY_PREFS_NAME = "DriverOnline";
    private String isDriverOnline;
    private SharedPreferences prefs;
    private SupportMapFragment mapFragment;

    private CircleImageView driverProfileImage;
    private TextView driverFullName, driverPhoneNumber, textForDutyStatus;
    private Switch switchForDutyStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_ambulance);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        driverProfileImage = headerView.findViewById(R.id.driverProfileImage);
        driverFullName = headerView.findViewById(R.id.driverFullName);
        driverPhoneNumber = headerView.findViewById(R.id.driverPhoneNumber);
        textForDutyStatus = headerView.findViewById(R.id.textForDutyStatus);
        switchForDutyStatus = headerView.findViewById(R.id.switchForDutyStatus);

        prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        isDriverOnline = prefs.getString("isDriverOnline", "No");
        if (isDriverOnline.equals("Yes")) {
            switchForDutyStatus.setChecked(true);
            textForDutyStatus.setText("On Duty");
        } else if (isDriverOnline.equals("No")) {
            switchForDutyStatus.setChecked(false);
            textForDutyStatus.setText("Off Duty");
        }
        switchForDutyStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something based on isChecked
                //Toast.makeText(getApplicationContext(), "Working Switch", Toast.LENGTH_SHORT).show();
                if (isChecked) {
                    connectDriver();
                    Snackbar snackbar = Snackbar.make(mapFragment.getView(), "You are online", Snackbar.LENGTH_SHORT);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(Color.parseColor("#008000"));
                    snackbar.show();

                    textForDutyStatus.setText("On Duty");
                } else {
                    disconnectDriver();
                    Snackbar snackbar = Snackbar.make(mapFragment.getView(), "You are offline", Snackbar.LENGTH_SHORT);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(Color.parseColor("#DC143C"));
                    snackbar.show();

                    textForDutyStatus.setText("Off Duty");
                }
            }
        });

        getUserInformation();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        getAssignedCustomer();


        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(BookAmbulanceActivity.this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String newToken = instanceIdResult.getToken();
                sendTokenToDatabase(newToken);
            }
        });

    }

    private void sendTokenToDatabase(String newToken) {
        String driverId = FirebaseAuth.getInstance().getUid();
        DatabaseReference tokenRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId);

        Map userInfo = new HashMap();
        userInfo.put("token", newToken);

        tokenRef.updateChildren(userInfo);
    }

    private void getUserInformation(){

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);
        mDriverDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null){
                        String mName = map.get("name").toString();
                        driverFullName.setText(mName);
                    }
                    if (map.get("profileImageUrl") != null){
                        String mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(driverProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        String mPhone = mAuth.getCurrentUser().getPhoneNumber();
        driverPhoneNumber.setText(mPhone);
    }


    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    String customerId = dataSnapshot.getValue().toString();
                    final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("currentRequest").child("rideStatus");
                    assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()){
                                if (mFusedLocationClient != null) {
                                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                                }

                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driverAvailable").child(userId);
                                ref.removeValue();

                                String rideStatus = dataSnapshot.getValue().toString();

                                if (rideStatus.equalsIgnoreCase("Confirmed") || rideStatus.equalsIgnoreCase("Picked Up")){
                                    Intent intent = new Intent(BookAmbulanceActivity.this, RideStartedActivity.class);
                                    startActivity(intent);
                                    finish();
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


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private BroadcastReceiver mReceiver;
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.NEWRIDE");
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //extract our message from intent
                String customerId = intent.getStringExtra("customerId");
                String ridePrice = intent.getStringExtra("ridePrice");
                if (mFusedLocationClient != null) {
                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                }
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driverAvailable").child(userId);
                ref.removeValue();

                Intent intent1 = new Intent(BookAmbulanceActivity.this, CustomerCallActivity.class);
                intent1.putExtra("rideId", customerId);
                intent1.putExtra("ridePrice", ridePrice);
                startActivity(intent1);


            }
        };
        //registering our receiver
        this.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.book_ambulance, menu);
        MenuItem item = menu.findItem(R.id.mySwitch);
        item.setActionView(R.layout.switch_layout);
        Switch mySwitch = item.getActionView().findViewById(R.id.switchForActionBar);
        isDriverOnline = prefs.getString("isDriverOnline", "No");
        if (isDriverOnline.equals("Yes")) {
            mySwitch.setChecked(true);
        } else if (isDriverOnline.equals("No")) {
            mySwitch.setChecked(false);
        }
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something based on isChecked
                //Toast.makeText(getApplicationContext(), "Working Switch", Toast.LENGTH_SHORT).show();
                if (isChecked) {
                    connectDriver();
                    Snackbar snackbar = Snackbar.make(mapFragment.getView(), "You are online", Snackbar.LENGTH_SHORT);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(Color.parseColor("#008000"));
                    snackbar.show();
                } else {
                    disconnectDriver();
                    Snackbar snackbar = Snackbar.make(mapFragment.getView(), "You are offline", Snackbar.LENGTH_SHORT);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(Color.parseColor("#DC143C"));
                    snackbar.show();

                }
            }
        });
        return true;
    }
*/

    public void logoutDialog() {


        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Log Out");
        builder.setMessage("Do you want to log out?");

        builder.setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                LogOut();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
        builder.show();
    }

    private void LogOut() {
        String driverId = FirebaseAuth.getInstance().getUid();
        DatabaseReference tokenRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId);

        Map<String, Object> tokenMapRemove = new HashMap<>();
        tokenMapRemove.put("token", "");

        tokenRef.updateChildren(tokenMapRemove).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                isLoggingOut = true;

                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                MobileNumberSessionManager sessionManager = new MobileNumberSessionManager(BookAmbulanceActivity.this);
                sessionManager.logoutUser();
            }
        });

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_details) {
            // Handle the camera action
            Intent intent = new Intent(BookAmbulanceActivity.this, DriverAccountActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_booking) {
            Intent intent = new Intent(BookAmbulanceActivity.this, RideHistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_get_support) {

        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_logout) {
            logoutDialog();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                isDriverOnline = prefs.getString("isDriverOnline", "No");
                if (isDriverOnline.equals("Yes")) {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                checkLocationPermission();
            }
        }
    }


    private Boolean firstLocation = false;

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {


                    mLastLocation = location;

                    if (!firstLocation) {
                        LatLng currentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM), 1000, null);
                        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                            @Override
                            public void onCameraMove() {
                                Log.e(TAG, "Final location: lat " + mMap.getCameraPosition().target.latitude + " lon " + mMap.getCameraPosition().target.longitude);

                            }
                        });
                        firstLocation = true;
                    }


                    final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    final DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("diverWorking").child(userId);
                    refWorking.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && !userId.equals("")){
                                refWorking.removeValue();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driverAvailable");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);

                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {

                        @Override
                        public void onComplete(String key, DatabaseError error) {

                        }
                    });





                }
            }
        }
    };

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(BookAmbulanceActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        }).create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(BookAmbulanceActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_SHORT).show();
                }
        }
    }


    private void connectDriver() {
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM), 1000, null);
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("isDriverOnline", "Yes");
        editor.apply();
    }

    private void disconnectDriver() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        if (mMap != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(false);
            mMap.animateCamera(CameraUpdateFactory.zoomTo(0), 1000, null);
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driverAvailable").child(userId);
        ref.removeValue();

        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("isDriverOnline", "No");
        editor.apply();
    }









}
