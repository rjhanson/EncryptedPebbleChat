package com.ssuser.ss.sunstonechat;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by Eric on 4/22/2015.
 */
public class ShuffleBytes {

    private static final String TAG = "ShuffleBytesClass";
    private static byte[] byteOrder = null;

    private static final String[] byteKeys = {"byte0", "byte1", "byte2", "byte3"};

    public static void setShuffleBytes(Context context, byte[] byteIncoming){

        SharedPreferences prefs = context.getSharedPreferences(ContactListActivity.PREFS_NAME,0);
        SharedPreferences.Editor editor = prefs.edit();

        byteOrder = byteIncoming;

        for( int i=0; i<4; i++){
            editor.putInt(byteKeys[i], byteIncoming[i]);
        }
        //editor.putString("ByteOrder", );

        editor.commit();
        Log.d(TAG, "byteOrder: " + Arrays.toString(byteOrder));

    }

    public boolean isReady(){
        return !(byteOrder == null);
    }

    public static byte[] shuffleBytes(Context context, byte[] byteMessage){
        if( byteMessage.length % 4 != 0){
            Log.e(TAG, "This message is not a multiple of 4 and cannot be shuffled!");
            return byteMessage;
        }
        // Create a new byte array to return
        byte[] shuffledBytes = new byte[byteMessage.length];

        //Log.d(TAG, "Pre shuffle: " + Arrays.toString(byteMessage));

        // Check that byteOrder still exists in memory
        if( byteOrder == null ){
            SharedPreferences prefs = context.getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
            byteOrder = new byte[4];
            for( int i=0; i<4; i++){
                byteOrder[i] = (byte)prefs.getInt(byteKeys[i], 8);
                if( byteOrder[i] == 8){
                    Log.e(TAG, "Shared Preferences has no reference to a byte order!!!");
                    return byteMessage;
                }
            }
        }

        for( int i = 0; i<byteMessage.length; i+=4){
            for( int j=0; j<4; j++){
                shuffledBytes[i+j] = byteMessage[i+byteOrder[j]];
            }
        }
        //Log.d(TAG, "Post shuffle: " + Arrays.toString(shuffledBytes));
        return shuffledBytes;
    }

    public static byte[] shuffleBytes(Context context, Byte[] byteMessage){
        if( byteMessage.length % 4 != 0){
            Log.e(TAG, "This message is not a multiple of 4 and cannot be shuffled!");
            return null;
        }
        // Create a new byte array to return
        byte[] shuffledBytes = new byte[byteMessage.length];

        // Check that byteOrder still exists in memory
        if( byteOrder == null ){
            SharedPreferences prefs = context.getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
            byteOrder = new byte[4];
            for( int i=0; i<4; i++){
                byteOrder[i] = (byte)prefs.getInt(byteKeys[i], 8);
                if( byteOrder[i] == 8){
                    Log.e(TAG, "Shared Preferences has no reference to a byte order!!!");
                    for( int j=0; j<byteMessage.length; j++) shuffledBytes[j] = byteMessage[j];
                    return shuffledBytes;
                }
            }
        }

        for( int i = 0; i<byteMessage.length; i+=4){
            for( int j=0; j<4; j++){
                shuffledBytes[i+j] = byteMessage[i+byteOrder[j]];
            }
        }
        return shuffledBytes;
    }

    public static byte[] unshuffleBytes(Context context, byte[] byteMessage){
        if( byteMessage.length % 4 != 0){
            Log.e(TAG, "This message is not a multiple of 4 and cannot be shuffled!");
            return byteMessage;
        }
        // Create a new byte array to return
        byte[] shuffledBytes = new byte[byteMessage.length];

        // Check that byteOrder still exists in memory
        if( byteOrder == null ){
            SharedPreferences prefs = context.getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
            byteOrder = new byte[4];
            for( int i=0; i<4; i++){
                byteOrder[i] = (byte)prefs.getInt(byteKeys[i], 8);
                if( byteOrder[i] == 8){
                    Log.e(TAG, "Shared Preferences has no reference to a byte order!!!");
                    return byteMessage;
                }
            }
        }

        for( int i = 0; i<byteMessage.length; i+=4){
            for( int j=0; j<4; j++){
                shuffledBytes[i+byteOrder[j]] = byteMessage[i+j];
            }
        }
       // Log.d(TAG, "Post unshuffle: " + Arrays.toString(shuffledBytes));
        return shuffledBytes;



    }


}
