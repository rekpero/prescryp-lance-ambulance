package com.prescryp.lance.ambulance;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chaos.view.PinView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prescryp.lance.ambulance.Session.MobileNumberSessionManager;

import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {
    private ImageView backBtn;
    private TextView phoneNumberTextView, timer, contact_us_header, contact_us_title, contact_us, change_phone_number;
    private PinView otp_value;
    private FloatingActionButton continueBtn;
    private CountDownTimer countDownTimer;
    private long timeLeftInMilliSeconds = 30000; // 30sec
    private boolean timerFinished = false;
    private String verificationCode;
    private FirebaseAuth mAuth;
    private String newPhoneNumber;
    private ProgressBar loading;
    private static String TAG = "PhoneVerificationActivity";
    private boolean isRegistered = true;
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        backBtn = findViewById(R.id.backBtn);
        phoneNumberTextView = findViewById(R.id.phoneNumberTextView);
        otp_value = findViewById(R.id.otp_value);
        continueBtn = findViewById(R.id.continueBtn);
        timer = findViewById(R.id.timer);
        loading = findViewById(R.id.loading);
        contact_us_header = findViewById(R.id.contact_us_header);
        contact_us_title = findViewById(R.id.contact_us_title);
        contact_us = findViewById(R.id.contact_us);
        change_phone_number = findViewById(R.id.change_phone_number);

        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();


        String phoneNumber = "";
        if (getIntent() != null){
            phoneNumber = getIntent().getStringExtra("Mobile_Number");
        }
        newPhoneNumber = "+91" + phoneNumber;
        sendVerificationCode(newPhoneNumber);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        String fullPhoneNumber = "+91 " + phoneNumber;
        phoneNumberTextView.setText(fullPhoneNumber);

        startTimer();

        final String finalPhoneNumber = newPhoneNumber;
        timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerFinished){
                    timer.setTextColor(getResources().getColor(R.color.themeColor));
                    resendVerificationCode(finalPhoneNumber);
                    startTimer();
                }
            }
        });



        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRegistered){
                    loading.setVisibility(View.VISIBLE);
                    String otp = otp_value.getText().toString();
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode, otp);
                    signInWithPhone(credential);
                }
            }
        });

        change_phone_number.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void signInWithPhone(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            final String user_id = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id).child("phone");
                            /*ValueEventListener eventListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists()) {
                                        //create new user
                                        MobileNumberSessionManager session = new MobileNumberSessionManager(getApplicationContext());
                                        session.createMobileNumberSession(newPhoneNumber, user_id);

                                        Intent intent = new Intent(PhoneVerificationActivity.this, BookAmbulanceActivity.class);
                                        intent.putExtra("mobile_number", newPhoneNumber);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    }else {
                                        contact_us_header.setVisibility(View.VISIBLE);
                                        contact_us_title.setVisibility(View.VISIBLE);
                                        contact_us.setVisibility(View.VISIBLE);
                                        change_phone_number.setVisibility(View.VISIBLE);
                                        isRegistered = false;
                                    }
                                    loading.setVisibility(View.GONE);
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.d(TAG, databaseError.getMessage()); //Don't ignore errors!
                                }
                            };
                            current_user_db.addListenerForSingleValueEvent(eventListener);
                            */
                            current_user_db.setValue(newPhoneNumber);

                            MobileNumberSessionManager session = new MobileNumberSessionManager(getApplicationContext());
                            session.createMobileNumberSession(newPhoneNumber, user_id);

                            Intent intent = new Intent(PhoneVerificationActivity.this, BookAmbulanceActivity.class);
                            intent.putExtra("mobile_number", newPhoneNumber);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();


                        } else {
                            Toast.makeText(PhoneVerificationActivity.this,"Incorrect OTP",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

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
                timerFinished = true;
                timer.setText("Resend code");
                timer.setTextColor(getResources().getColor(R.color.themeBlue));
            }
        }.start();
    }

    private void updateTimer() {
        int second = (int) timeLeftInMilliSeconds / 1000;
        String timeLeftText;

        timeLeftText = "00:";
        if (second < 10) timeLeftText += "0";
        timeLeftText += second;
        String timeLeftFullText = "Resend code in " + timeLeftText;
        timer.setText(timeLeftFullText);

    }

    private void resendVerificationCode(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                30,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                resendingToken);
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                30,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            if (e instanceof FirebaseAuthInvalidCredentialsException){
                Log.d(TAG, "Invalid credential: " + e.getLocalizedMessage());
            }else if (e instanceof FirebaseTooManyRequestsException){
                Log.d(TAG, "SMS QUota exceeded.");
            }
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            verificationCode = s;
            resendingToken = forceResendingToken;
        }

        @Override
        public void onCodeAutoRetrievalTimeOut(String s) {
            super.onCodeAutoRetrievalTimeOut(s);
            timerFinished = true;
            timer.setText("Resend code");
            timer.setTextColor(getResources().getColor(R.color.themeBlue));
        }
    };

}
