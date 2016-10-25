package com.ssuser.ss.sunstonechat;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Eric on 3/13/2015.
 */
public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String SMS_SENT = "android.provider.Telephony.SMS_SENT";
    private static final String SMS_DELIVERED = "android.provider.Telephony.SMS_DELIVERED";

    public static final String SENDER = "com.ssuser.ss.sunstonechat.SENDER";
    public static final String MESSAGE_BODY = "com.ssuser.ss.sunstonechat.MESSAGE_BODY";

    @Override
    public void onReceive(Context context, Intent intent){
        Log.i(TAG, "Intent received! " + intent.getAction());

        DatabaseHandler db = new DatabaseHandler(context);

        // Holder string for the received message
        String msg = "";
        String address = null;

        if(intent.getAction().equals(SMS_SENT)){
            boolean sent = false;

            switch (getResultCode()){
                case Activity.RESULT_OK:
                    sent = true;
                    Log.d(TAG, "SMS Sent OK");
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Log.d(TAG, "SMS Not Sent Generic failure");
                    Toast.makeText(context, "SMS Not Sent: Generic Failure", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Log.d(TAG, "SMS Sent No Service");
                    Toast.makeText(context, "SMS Not Sent: No Service", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(context, "SMS Not Sent: Null PDU",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(context, "SMS Not Sent: Radio off",
                            Toast.LENGTH_SHORT).show();
                    break;
            }

            int messageID = intent.getIntExtra("MessageID", -1);
            Log.d(TAG, "MESSAGE ID: " + messageID + "sent?: " + sent);
            if( messageID < 0 ) return; // Not a sunstone chat message?
            MessageDataBase msgout = db.getMessage(messageID);

        }
        if(intent.getAction().equals(SMS_DELIVERED)){
            boolean delivered = false;
            switch (getResultCode()){
                case Activity.RESULT_OK:
                    delivered = true;
                    Toast.makeText(context, "SMS delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(context, "SMS not delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
            }

            int messageID = intent.getIntExtra("MessageID", -1);
            if( messageID < 0 ) return; // Not a sunstone chat message?
            MessageDataBase msgout = db.getMessage(messageID);
            Log.d(TAG, "Message ID from sms_delivered " + messageID);
            if( delivered ){
                msgout.setMessageStatus(MessageDataBase.MSG_SENT);
            }else{
                Log.d(TAG, "Message has failed to be DELIVERED");
                msgout.setMessageStatus(MessageDataBase.MSG_FAILED);
            }
            db.updateMessage(msgout);
        }

        if(intent.getAction().equals(SMS_RECEIVED)){
            //abortBroadcast();
            Bundle bundle = intent.getExtras();
            Log.i(TAG, "Got SMS");
            if(bundle != null){
                Log.d(TAG, "Got bundle");
                Object[] pdus = (Object[]) bundle.get("pdus");
                // here is what I need, just combine them all  :-)
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                Log.d(TAG, String.format("message count = %s", messages.length));
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    Log.d(TAG, "From bundle message[" + i+ "]: " + messages[i].getMessageBody());
                }

            }
            SmsMessage[] msgs = getMessagesFromIntent(intent);
            //SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if( msgs == null ) return;
            for( int i=0; i<msgs.length; i++){
                Log.i(TAG, "msg[" + i + "] To: " + msgs[i].getOriginatingAddress());
                Log.i(TAG, "msg[" + i + "] Body: " + msgs[i].getMessageBody().toString());
                address = msgs[i].getOriginatingAddress();
                //TODO:
                // msgs[i].getDisplayOriginatingAddress()
                msg += msgs[i].getMessageBody().toString();
            }

            if( msg.contains("snstChat")){
                // Here we have found a sunstone chat message
                // Open our app with a bundled extra containing the message
                // Stop the SMSRecieved broadcast
                Log.d(TAG, "Sunstone Message!");
                msg = msg.replace("snstChat", "");
                //InvokeAbortBroadcast()
                //abortBroadcast();
                setResultData(null);
                MessageDataBase messagePut;
                Log.d(TAG, "snstChat TimeLog23: " + String.valueOf(msgs[0].getTimestampMillis()));
                long timestamp = msgs[0].getTimestampMillis();
                if( timestamp == 0 ) timestamp = System.currentTimeMillis(); // In case there was no timestamp with the message, default to the current system time
                if(address.substring(0,2).equals("+1")) {
                    messagePut = new MessageDataBase(address, msg, String.valueOf(timestamp), false, false);
                } else {
                    messagePut = new MessageDataBase("+1" + address, msg, String.valueOf(timestamp), false, false);
                }
                messagePut.setMessageStatus(MessageDataBase.MSG_RECEIVED);
                db.addMessage(messagePut);

                int unread = db.getUnreadMessagesCount();
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher_2)
                        .setContentTitle("New BeatPoet Message")
                        .setAutoCancel(true)
                        .setContentText("You have " + unread + " unread messages");

                Intent resultIntent = new Intent(context, ContactListActivity.class);

                PendingIntent resultPendingIntent = PendingIntent.getActivity(
                        context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT
                );

                mBuilder.setContentIntent(resultPendingIntent);

                int mNotificationId = 001;
                NotificationManager mNotifyMgr =
                        (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

                mNotifyMgr.notify(mNotificationId,mBuilder.build());

            } else {
                // Not a sunstone chat message so propagate the broadcast
                Log.d(TAG, "Not a sunstone message... Continue on");
            }

        }
    }

    public static SmsMessage[] getMessagesFromIntent(Intent intent){
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        byte[][] pduObjs = new byte[messages.length][];

        for( int i=0; i<messages.length; i++){
            pduObjs[i] = (byte[]) messages[i];
        }
        byte[][] pdus = new byte[pduObjs.length][];
        int pduCount = pdus.length;
        SmsMessage[] mgs = new SmsMessage[pduCount];
        for( int i=0; i < pduCount; i++){
            pdus[i] = pduObjs[i];
            mgs[i] = SmsMessage.createFromPdu(pdus[i]);
        }
        return mgs;
    }
}
