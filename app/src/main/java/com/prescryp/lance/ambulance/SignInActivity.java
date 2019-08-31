package com.prescryp.lance.ambulance;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.prescryp.lance.ambulance.Session.MobileNumberSessionManager;

import java.util.HashMap;

public class SignInActivity extends AppCompatActivity {

    private EditText mobileNumberEditText;
    private FloatingActionButton continueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        MobileNumberSessionManager session = new MobileNumberSessionManager(getApplicationContext());
        if (session.isMobLoggedIn()){
            HashMap<String, String> user = session.geMobileDetails();
            String session_mob = user.get(MobileNumberSessionManager.KEY_MOB);
            Intent custom = new Intent(SignInActivity.this, BookAmbulanceActivity.class);
            custom.putExtra("mobile_number", session_mob);
            custom.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(custom);
            finish();
        }

        mobileNumberEditText = findViewById(R.id.mobileNumberEditText);
        continueBtn = findViewById(R.id.continueBtn);

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = mobileNumberEditText.getText().toString();

                if (phoneNumber.isEmpty()){
                    mobileNumberEditText.setError("Phone number is required");
                    mobileNumberEditText.requestFocus();
                    return;
                }
                if (phoneNumber.length() < 10){
                    mobileNumberEditText.setError("Please enter a valid phone");
                    mobileNumberEditText.requestFocus();
                    return;
                }

                Intent intent = new Intent(SignInActivity.this, PhoneVerificationActivity.class);
                intent.putExtra("Mobile_Number", phoneNumber);
                startActivity(intent);
            }
        });
    }
}
