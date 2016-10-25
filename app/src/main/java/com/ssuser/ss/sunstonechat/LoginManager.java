package com.ssuser.ss.sunstonechat;

import android.content.SharedPreferences;

import android.content.Context;
import android.util.Log;

/**
 * Created by Eric & Russell on 3/30/2015.
 */
public class LoginManager {

    private static final int loginTimeout = 15; // Active timeout
    private static final String TAG = "LoginManager";
    public static boolean isExpired(Context context){
        // Retrieve the shared preferences last active time
        // Check that (active - now) < timeout
        SharedPreferences prefs = context.getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
        long lastActive = prefs.getLong(LoginActivity.lastActive, 0);
        long dt = System.currentTimeMillis() - lastActive;
        dt  /= (1000 * 60); // Convert milliseconds to minutes
        Log.d(TAG, "dt: "+dt);
        if( dt > loginTimeout )
            return true;
        return false;
    }

    public static void setLastActive(Context context){
        SharedPreferences prefs = context.getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LoginActivity.lastActive, System.currentTimeMillis());
        editor.commit();
    }
}
