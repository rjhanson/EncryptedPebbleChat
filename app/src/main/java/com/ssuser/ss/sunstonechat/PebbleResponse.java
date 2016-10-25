package com.ssuser.ss.sunstonechat;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

/**
 * Created by Eric and Russell on 3/17/2015.
 */
public class PebbleResponse {

   // PebbleKit.PebbleDataReceiver dataReceiver;
    //PebbleKit.PebbleAckReceiver ackReceiver;
    //PebbleKit.PebbleNackReceiver nackReceiver;

    // Key Compare
    private static final int AM_NEW_CONTACT = 5;            // New Contact Key
    private static final int AM_CREATED_CONTACT = 9;        // Created Contact
    private static final int AM_RESULT_CODE = 50;           // Add Contact result
    private static final int AM_READ_MESSAGE = 7;           // Read Message
    private static final int AM_ENCRYPTED_KEY = 80;         // Encrypted
    private static final int AM_SEND_MESSAGE = 6;           // Send Message

    private static final String TAG = "PebbleResponseClass";

    // Add resuilt codes
    private static final int AM_SUCCESS = 0;
    private static final int AM_FULL    = 1;


    private static final UUID SSCHAT_UUID = UUID.fromString("E30CF49A-497B-4FDB-B4CD-7D655C0C84FB");

    PebbleKit.PebbleDataReceiver dataReceiver =  new PebbleKit.PebbleDataReceiver(SSCHAT_UUID) {
        @Override
        public void receiveData(final Context context, final int i, final PebbleDictionary data){
            Log.d(TAG, "Got data from Pebble");
            PebbleKit.sendAckToPebble(context, i);

            if(data.contains(AM_RESULT_CODE)){

                int result = data.getUnsignedIntegerAsLong(AM_RESULT_CODE).intValue();
                if( result == 0 ){
                    Log.i(TAG, "Successfully decoded message");
                    Toast toast = Toast.makeText(context, "Check Watch for Message!", Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    Log.e(TAG, "Got unknown data from Pebble. Data = " + result);
                    Toast toast = Toast.makeText(context, "Error decoding message", Toast.LENGTH_LONG);
                }

            } else if (data.contains(AM_NEW_CONTACT)){

            } else if(data.contains(AM_CREATED_CONTACT)){

            } else if(data.contains(AM_READ_MESSAGE)){

            } else if(data.contains(AM_ENCRYPTED_KEY)){

            } else if(data.contains(AM_SEND_MESSAGE)){

            } else {

            }
        }
    };

    PebbleKit.PebbleNackReceiver nackReceiver =  new PebbleKit.PebbleNackReceiver(SSCHAT_UUID) {
        @Override
        public void receiveNack(Context context, int i){

        }
    };

    PebbleKit.PebbleAckReceiver ackReceiver =  new PebbleKit.PebbleAckReceiver(SSCHAT_UUID) {
        @Override
        public void receiveAck(Context context, int i) {
            Log.i(TAG, "Received ACK for transaction " + i);
        }
    };

}
