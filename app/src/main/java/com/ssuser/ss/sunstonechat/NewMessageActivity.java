package com.ssuser.ss.sunstonechat;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;


public class NewMessageActivity extends ActionBarActivity {
    private static final String TAG = "NewMessageActivity";

    public static final String SSPrefix = "snstChat";

    private static final UUID SSCHAT_UUID = UUID.fromString("E30CF49A-497B-4FDB-B4CD-7D655C0C84FB");

    // Flags for async watch commands
    private static boolean WATCH_BUSY = false;
    private static String lastMsg = "";
    public static int CHUNKSIZE = 80;

    private static final int AM_ENCRYPTED_KEY = 80;
    private static final int AM_SEND_MESSAGE = 6;
    private static final int AM_KEY_NUM_VALS = 0;
    private static final int AM_ENC_PUZZLE = 16;
    private static final int AM_ENC_PHONE_PUZZLE = 17;

    private String contactName   = "";
    private String contactNumber = "";

    private String[] msg_arr;
    private int msgRecCount = 0;
    private int cur_pebble_chunk;
    private String enc_msg = "";

    private static final Integer[] basePuzzle = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};

    private MessageConverter msgConverter;


    // Progress waiting dialog object
    private ProgressDialog ringProgressDialog;

    // Broadcast receivers for Pebble messages
    private PebbleKit.PebbleDataReceiver dataReceiver =  new PebbleKit.PebbleDataReceiver(SSCHAT_UUID) {
        @Override
        public void receiveData(final Context context, final int i, final PebbleDictionary data){
            Log.d(TAG, "Got data from Pebble");
            PebbleKit.sendAckToPebble(getApplicationContext(), i);
            if( data.contains(AM_ENCRYPTED_KEY)){
                byte byteReturn[] = data.getBytes(AM_ENCRYPTED_KEY);
                byteReturn = ShuffleBytes.unshuffleBytes(context, byteReturn);
                if(byteReturn.length % 4 != 0){
                    Log.e(TAG, "Bad Length Array byteReturn");
                    ringProgressDialog.dismiss();
                } else {
                    Byte objArr[] = new Byte[byteReturn.length];
                    for(int idx = 0; idx < objArr.length; ++idx) {
                        objArr[idx] = byteReturn[idx];
                    }
                    enc_msg += MessageConverter.getMsgHex(objArr);
                    cur_pebble_chunk++;
                    sendToPebble();

                }

            }
        }
    };

    private PebbleKit.PebbleNackReceiver nackReceiver =  new PebbleKit.PebbleNackReceiver(SSCHAT_UUID) {
        @Override
        public void receiveNack(Context context, int i) {
            //Log.d(TAG,"FROM NACK: msgRecCount: " + msgRecCount + " | msg_arr.length: " + msg_arr.length);
            Log.i(TAG, "Received NACK for transaction " + i);
            sendToPebble();
        }
    };

    private PebbleKit.PebbleAckReceiver ackReceiver =   new PebbleKit.PebbleAckReceiver(SSCHAT_UUID) {
        @Override
        public void receiveAck(Context context, int i) {
            Log.i(TAG, "Received ACK for transaction " + i);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);
        WordLibrary.getInstance().ensureLoaded(getResources());
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        setTitle("New Message");

        final EditText messageBdy = (EditText) findViewById(R.id.newMessageContainer);
        messageBdy.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                if(!hasFocus){
                    Log.d(TAG, "Hiding messageBdy Keyboard");
                    hideSoftKeyboard(v);
                }else{
                    Log.d(TAG, "Showing messageBdy Keyboard");
                }
            }
        });
        /*
        messageBdy.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if(actionId == EditorInfo.IME_ACTION_SEND){
                    onSendMessage(null);
                    handled = true;
                }
                return handled;
            }
        });
        */
        TextView toEmailForm = (TextView) findViewById(R.id.toEmailForm);
        Intent intent = getIntent();
        if(intent.hasExtra(ContactListActivity.EXTRA_NUMBER) &&
           intent.hasExtra(ContactListActivity.EXTRA_NAME)){
            contactNumber = intent.getStringExtra(ContactListActivity.EXTRA_NUMBER);
            contactName   = intent.getStringExtra(ContactListActivity.EXTRA_NAME);
            toEmailForm.setText("To: " + contactName);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        PebbleKit.registerReceivedAckHandler(getApplicationContext(), ackReceiver);
        PebbleKit.registerReceivedNackHandler(getApplicationContext(), nackReceiver);
        PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //LoginManager.setLastActive(getApplicationContext());
        try {
            getApplicationContext().unregisterReceiver(dataReceiver);
            getApplicationContext().unregisterReceiver(ackReceiver);
            getApplicationContext().unregisterReceiver(nackReceiver);
        } catch (java.lang.IllegalArgumentException e) {
            Log.e(TAG, "Couldn't unregister dataReceiver");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.action_send:
                onSendMessage(null);
                return true;
            case android.R.id.home:
                finish();
                return true;
            /*
            case R.id.action_settings:
                return true;
            */
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void hideSoftKeyboard(View view){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }


    public void onSendMessage(View view){

        EditText body = (EditText) findViewById(R.id.newMessageContainer);
        Log.i(TAG, "Sending message to watch");

        if( WATCH_BUSY ){
            return;
        }
        WATCH_BUSY = true;

        if(!PebbleKit.isWatchConnected(getApplicationContext())){
            Log.d(TAG, "Watch is not connected");
            Toast toast = Toast.makeText(getApplicationContext(), "Watch is not connected", Toast.LENGTH_SHORT);
            toast.show();
            WATCH_BUSY = false;
            return;
        }

        // Get the message the user typed into the box
        String message = body.getText().toString();

        // To minimize receiver confusion, lets not send an empty message
        if( message == null || message.trim().isEmpty() ){
            Log.d(TAG, "Message to send was empty: " + message);
            Toast.makeText(getApplicationContext(), "Type a message to send!", Toast.LENGTH_LONG).show();
            WATCH_BUSY = false;
            return;
        }

        if(msgConverter == null){
            msgConverter = new MessageConverter(message);
        }else{
            msgConverter.setMsg(message);
        }


        Log.d(TAG, "message: " + message + " contactStr.length: " + contactNumber.length());
        Log.d(TAG, "msgConverter.getMsg():" + msgConverter.getMsg());
        // Start the wait spinner while the watch works
        ringProgressDialog = ProgressDialog.show(NewMessageActivity.this, "Please wait...", "Encrypting Message...", true);
        ringProgressDialog.setCancelable(true);

        // Starting a new message to the watch so set the current chunk to 0
        cur_pebble_chunk = -1;
        enc_msg = "";

        WATCH_BUSY = false;

        sendToPebble();
    }

    private void sendToPebble(){
        if( WATCH_BUSY ) return;
        WATCH_BUSY = true;
        Byte[] byteList = msgConverter.getBytes();
        int totalChunks = (int)Math.ceil(byteList.length/(double)CHUNKSIZE);
        if( totalChunks == 0 ){
            Log.e(TAG, "Total messages to pebble was 0, can't send empty message");
            return;
        }
        if(cur_pebble_chunk == -1){
            byte dec_bytes[] = getNewPuzzle();
            dec_bytes = ShuffleBytes.shuffleBytes(getApplicationContext(),dec_bytes);
            PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);
            PebbleDictionary message_puzzle_data = new PebbleDictionary();
            message_puzzle_data.addString(AM_ENC_PHONE_PUZZLE, contactNumber);
            message_puzzle_data.addBytes(AM_ENC_PUZZLE,dec_bytes);
            Log.d(TAG, "contactNumber and AM_ENCPUZZLE");
            PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, message_puzzle_data);
            WATCH_BUSY = false;
            return;
        }
        Log.d(TAG, "TotalChunks: " + totalChunks + " | cur_pebble_chunk: " + cur_pebble_chunk);
        if( totalChunks == cur_pebble_chunk){
            Log.d(TAG, "sending SMS after totalChunks: " + totalChunks);
            sendMessageAsSMS(contactNumber,enc_msg);
            ringProgressDialog.dismiss();
            finish();
        }else{
            PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);
            PebbleDictionary message_data = new PebbleDictionary();

            int message_size = CHUNKSIZE;
            if( CHUNKSIZE * (cur_pebble_chunk+1) > byteList.length){
                // Our message chunk to send is smaller than CHUNKSIZE
                message_size = byteList.length - (CHUNKSIZE * cur_pebble_chunk);
            }

            byte byteArr[] = new byte[message_size+10];
            // Copy the phone number into the array
            System.arraycopy(contactNumber.getBytes(),0,byteArr,0,10);
            Log.d(TAG, "contactNumber from system Array copy: " + contactNumber);

            byte[] shuffledBytes = ShuffleBytes.shuffleBytes(getApplicationContext(),Arrays.copyOfRange(byteList, (CHUNKSIZE * cur_pebble_chunk), (CHUNKSIZE * cur_pebble_chunk) + message_size));
            System.arraycopy(shuffledBytes, 0, byteArr, 10, shuffledBytes.length);

            //byte[] byteShuffledArray = ShuffleBytes.shuffleBytes(Arrays.copyOfRange(byteArr, 10, byteArr.length));

            //byte[] byteUnshuffledArray = ShuffleBytes.unshuffleBytes(byteShuffledArray);

            String byteStr = "[";
            for(int i = 0; i < byteArr.length; ++i) {
                byteStr += Integer.toString(byteArr[i] & 0xFF);
                byteStr += ", ";
            }
            byteStr += "]";
            Log.d(TAG, "***sendToPebble: " + byteStr);

            Log.d(TAG, " NEWMESSAGE: byteArr divisible by 4?: " + ((byteArr.length - 10) % 4 == 0));
            message_data.addUint8(AM_KEY_NUM_VALS, (byte)((byteArr.length - 10)*2));
            message_data.addBytes(AM_SEND_MESSAGE, byteArr);

            PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, message_data);

            //Log.d(TAG,"FROM SEND: msgRecCount: " + msgRecCount + " | msg_arr.length: " + msg_arr.length);
        }
        WATCH_BUSY = false;
    }

    private void sendMessageAsSMS(String recipient, String message){
        Log.d(TAG, "Sending message\n" + message + "\nTo\n" + recipient);

        int messageID;

        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
        MessageDataBase messagePut = new MessageDataBase("+1" + recipient, message, String.valueOf(System.currentTimeMillis()) ,true, true);
        messageID = (int) db.addMessageWithReturn(messagePut);
        Log.d(TAG, "messageID from new messageActivity: " + messageID);


        // For marking sent success or failure later
        Intent sentI = new Intent("android.provider.Telephony.SMS_SENT");
        sentI.putExtra("MessageID", messageID);
        Intent deliveredI = new Intent("android.provider.Telephony.SMS_DELIVERED");
        deliveredI.putExtra("MessageID", messageID);

        PendingIntent sentPi = PendingIntent.getBroadcast(this, messageID, sentI, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent deliveredPi = PendingIntent.getBroadcast(this, messageID, deliveredI, PendingIntent.FLAG_CANCEL_CURRENT);
        ArrayList<PendingIntent> sentPiMulti = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPiMulti = new ArrayList<PendingIntent>();


        SmsManager sm = SmsManager.getDefault();

        // Check the message length in case we need to split it into multiple parts
        if( SSPrefix.length() + message.length() >= 160 ){
            // Lets split this long message into smaller chunks for sending
            ArrayList<String> parts = sm.divideMessage(SSPrefix + message);
            Log.d(TAG, "parts: " + parts);
            for(int i=0; i < parts.size(); i++){
                sentPiMulti.add(i, sentPi);
                deliveredPiMulti.add(i, deliveredPi);
            }
            sm.sendMultipartTextMessage(recipient, null, parts, sentPiMulti, deliveredPiMulti);
        } else {
            // This message will fit in a single text, so just send it
            sm.sendTextMessage(recipient, null, SSPrefix + message, sentPi, deliveredPi);
        }



        Log.d(TAG, "Finished sending text message: " + (SSPrefix + message));
    }

    private byte[] getNewPuzzle(){
        byte bytes[] = new byte[basePuzzle.length/2];
        List<Integer> puzzleList = new ArrayList<Integer>();
        puzzleList.addAll(Arrays.asList(basePuzzle));
        Collections.shuffle(puzzleList, new Random(System.nanoTime()));

        for( int i=0; i<puzzleList.size(); i+=2){
            Log.d(TAG, "New Puzzle: " + puzzleList.get(i));
            Log.d(TAG, "New Puzzle: " + puzzleList.get(i+1));
            int upper = puzzleList.get(i);
            int lower = puzzleList.get(i+1);
            bytes[i/2] = (byte) ((upper << 4) | lower);
        }
        return bytes;
    }

}
