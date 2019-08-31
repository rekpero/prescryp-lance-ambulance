package com.prescryp.lance.ambulance.Session;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;


import com.prescryp.lance.ambulance.SignInActivity;

import java.util.HashMap;

public class MobileNumberSessionManager {

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;

    int PRIVATE_MODE = 0;

    private static final String PREFER_NAME = "AndroidPref";
    private static final String IS_MOB_LOGIN = "IsMobLoggedIn";
    public static final String KEY_USER_ID = "UserId";
    public static final String KEY_MOB = "MobileNumber";

    public MobileNumberSessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREFER_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void createMobileNumberSession(String mobnum, String userid){
        editor.putBoolean(IS_MOB_LOGIN, true);
        editor.putString(KEY_USER_ID, userid);
        editor.putString(KEY_MOB, mobnum);
        editor.commit();
    }

    public void checkLogin(){
        if (!this.isMobLoggedIn()){
            Intent i = new Intent(_context, SignInActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            _context.startActivity(i);
        }
    }

    public HashMap<String, String> geMobileDetails(){
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_USER_ID, pref.getString(KEY_USER_ID, null));
        user.put(KEY_MOB, pref.getString(KEY_MOB, null));

        return user;
    }

    public void logoutUser(){
        editor.clear();
        editor.commit();

        Intent i = new Intent(_context, SignInActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        _context.startActivity(i);
    }

    public boolean isMobLoggedIn(){
        return pref.getBoolean(IS_MOB_LOGIN, false);
    }


}
