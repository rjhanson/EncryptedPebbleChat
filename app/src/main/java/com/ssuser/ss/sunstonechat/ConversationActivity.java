package com.ssuser.ss.sunstonechat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class ConversationActivity extends ActionBarActivity {

    private static final String TAG = "ConversationActivity";

    private static final UUID SSCHAT_UUID = UUID.fromString("E30CF49A-497B-4FDB-B4CD-7D655C0C84FB");

    public static String contactInfo;
    public static final String PREFS_NAME = "SunstoneChatPrefs";
    private static final String BEAT_POET_SETTINGS = "BeatPoetSettings";

    private final ArrayList<MessageDataBase> list = new ArrayList<MessageDataBase>();
    private ConversationListView adapter;
    private ListView convoList;
    private Contacts contacts;

    public static final String EXTRA_NUMBER = "com.sunstone.sunstonechat.extraNumber";
    public static final String EXTRA_NAME = "com.sunstone.sunstonechat.extraName";

    private static final int AM_READ_MESSAGE = 7;
    private static final int AM_SENT_MESSAGE = 12;
    private static final int AM_RESULT_KEY = 50;
    private static final int AM_DECRYPTED_SUCCESS = 0;
    private static final int AM_DISPLAY_SUCCESS = 6;
    private static final int AM_MEM_ERROR = 5;
    private static final int AM_MSG_SETUP = 10;
    private static final int AM_MSG_DISPLAY = 11;
    private static final int AM_DECRYPTED_KEY = 13;
    private static final int AM_SEND_MESSAGE = 14;
    private static final int AM_KEY_NUM_VALS = 0;
    private static final int AM_DEC_PUZZLE = 15;
    private static final int AM_DEC_PUZZLE_PHONE = 17;
    private static final int AM_DEC_PUZZLE_SUCCESS = 7;
    private static final int AM_KEY_TYPE = 18;

    private ProgressDialog ringProgressDialog;
    private ProgressDialog barProgressDialog;

    private MessageDataBase messageClicked;
    private static final int CHUNKSIZE = NewMessageActivity.CHUNKSIZE;
    private static boolean WATCH_BUSY = false;
    private static int chunkCount;
    private static String msgReceived = "";
    private static int lastDataToPebble = 0;
    private static String username = "";
    private static String cName = "";

    private static final String SMS_SENT = "android.provider.Telephony.SMS_SENT";

    private static final Integer[] scram = {2,7,12,8,14,15,3,13,6,4,5,10,0,11,1,9};
    private static final Integer[] secret = {0,9,14,12,8,10,15,4,6,13,5,3,11,2,7,1};
    private FakeReceiver fRecv;

    private boolean beatPoetWords = false;
    private boolean searching = false;
    private int currIndexSearch = -1;
    private String searchStringForMessages;
    private List<MessageDataBase> searchResult = new ArrayList<MessageDataBase>();

    DatabaseHandler db = new DatabaseHandler(this);

    private static final String numberSearch = "###";


    private class SmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            Log.d(TAG, "RECEIVED FROM HOME");

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

                if( sent ){
                    msgout.setMessageStatus(MessageDataBase.MSG_SENT);
                    db.updateMessage(msgout);
                }else{
                    Log.d(TAG, "Message has failed to be SENT");
                    msgout.setMessageStatus(MessageDataBase.MSG_FAILED);
                    db.updateMessage(msgout);
                }

                Log.d(TAG, "Message id = " + msgout.getID() + " | msgBody: " + msgout.getMessageFake() + " | msg.getStatus: " + msgout.getMessageStatus());

            }
            updateMessageList();
        }
    }

    private PebbleKit.PebbleDataReceiver dataReceiver =  new PebbleKit.PebbleDataReceiver(SSCHAT_UUID) {
        @Override
        public void receiveData(final Context context, final int i, final PebbleDictionary data){
            Log.d(TAG, "Got data from Pebble");
            PebbleKit.sendAckToPebble(getApplicationContext(), i);
            if(data.contains(AM_RESULT_KEY)){
                int result = data.getUnsignedIntegerAsLong(AM_RESULT_KEY).intValue();
                if( result == AM_DISPLAY_SUCCESS ) {
                    Log.i(TAG, "Successfully decoded message");
                    //Toast toast = Toast.makeText(getApplicationContext(), "Check Watch for Message!", Toast.LENGTH_LONG);
                    //toast.show();
                    if (messageClicked != null) {
                        messageClicked.setMessageRead(true);
                        db.updateMessage(messageClicked);
                        updateMessageList();
                        // We finished the transaction so clear the last used flag
                        lastDataToPebble = 0;
                    } else {
                        Log.e(TAG, "Something went wrong with setting Message as READ");
                    }
                } else if(result == AM_DEC_PUZZLE_SUCCESS){
                    Log.d(TAG, "Decrypted Puzzle Success");
                    chunkCount++;
                    sendDataToPebble();
                } else if(result == AM_DECRYPTED_SUCCESS){
                    Log.d(TAG, "parsingMessageToWatch");
                    // Reports success for sending a display chunk.
                    // At this point the message is fully decrypted
                    chunkCount++;
                    sendDisplayToPebble();
                } else if(result == AM_MEM_ERROR){
                    Log.e(TAG, "WATCH Memory Error");
                }
                else {
                    Log.e(TAG, "Got unknown data from Pebble. Data = " + result);
                    Toast.makeText(getApplicationContext(), "Error decoding message", Toast.LENGTH_LONG).show();
                }
            }
            if(data.contains(AM_DECRYPTED_KEY)){
                byte byteReturn[] = data.getBytes(AM_DECRYPTED_KEY);
                byteReturn = ShuffleBytes.unshuffleBytes(getApplicationContext(),byteReturn);
                Byte objArr[] = new Byte[byteReturn.length];
                for(int j=0; j < objArr.length; j++){
                    objArr[j] = byteReturn[j];
                }
                MessageConverter messageReceiver = new MessageConverter(objArr);
                msgReceived += messageReceiver.getMsgDisp();
                //Log.d(TAG, "messageReceived from AM_DECRYPTED_KEY: " + msgReceived);
                chunkCount++;
                sendDataToPebble();
            }

        }
    };

    private PebbleKit.PebbleNackReceiver nackReceiver =  new PebbleKit.PebbleNackReceiver(SSCHAT_UUID) {
        @Override
        public void receiveNack(Context context, int i) {
            Log.i(TAG, "Received NACK for transaction " + i);
            //sendMessageToWatch();
            if(lastDataToPebble == 1) {
                sendDataToPebble();
            }
            if(lastDataToPebble == 2){
                sendDisplayToPebble();
            }
        }
    };

    private PebbleKit.PebbleAckReceiver ackReceiver =   new PebbleKit.PebbleAckReceiver(SSCHAT_UUID) {
        @Override
        public void receiveAck(Context context, int i) {
            Log.i(TAG, "Received ACK for transaction " + i);
        }
    };

    private SmsReceiver mySmsReceiver = new SmsReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        if( !WordLibrary.getInstance().isLoaded() ) {
            WordLibrary.getInstance().ensureLoaded(getResources());
        }


        fRecv = new FakeReceiver(scram, secret);

        convoList = (ListView) findViewById(R.id.convoListView);

        SharedPreferences prefs = getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
        beatPoetWords = prefs.getBoolean(BEAT_POET_SETTINGS, true);

        adapter = new ConversationListView(this, beatPoetWords, list);
        convoList.setAdapter(adapter);

        Intent intent = getIntent();
        contacts = (Contacts) intent.getSerializableExtra(contactInfo);

        //Set ActionBar Title
        setTitle(contacts.getName());

        //TextView contactTitle = (TextView) findViewById(R.id.conversationContact);

        final SwipeDetector swipeDetector = new SwipeDetector();
        convoList.setOnTouchListener(swipeDetector);

        convoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                messageClicked = (MessageDataBase) parent.getItemAtPosition(position);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ConversationActivity.this);
                alertDialogBuilder.setTitle("Delete Message");
                alertDialogBuilder.setMessage("Are you sure you want to delete?");
                alertDialogBuilder.setCancelable(true);
                alertDialogBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteMessage(messageClicked);
                        updateMessageList();
                        Log.d(TAG, "Delete Message");
                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Cancel Delete");
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();

                if (!swipeDetector.swipeDetected()) {
                    chunkCount = -1;
                    msgReceived = "";
                    ringProgressDialog = ProgressDialog.show(ConversationActivity.this, "Please wait...", "Decrypting Message...", true);
                    ringProgressDialog.setCancelable(true);
                    ringProgressDialog.setCanceledOnTouchOutside(false);
                    Log.d(TAG, "RingProgressDialog.SHOW");
                    sendDataToPebble();
                } else {
                    alertDialog.show();
                }
            }
        });

    }

    protected void onResume(){
        super.onResume();
        Log.d(TAG, "Update Message List");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        intentFilter.addAction("android.provider.Telephony.SMS_SENT");
        PebbleKit.registerReceivedAckHandler(getApplicationContext(), ackReceiver);
        PebbleKit.registerReceivedNackHandler(getApplicationContext(), nackReceiver);
        PebbleKit.registerReceivedDataHandler(getApplicationContext(), dataReceiver);

        getApplicationContext().registerReceiver(mySmsReceiver, intentFilter);
        updateMessageList();
    }

    protected void onPause(){
        super.onPause();

        SharedPreferences prefs = getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(BEAT_POET_SETTINGS, beatPoetWords);
        editor.commit();

        //LoginManager.setLastActive(getApplicationContext());
        try {
            getApplicationContext().unregisterReceiver(dataReceiver);
            getApplicationContext().unregisterReceiver(ackReceiver);
            getApplicationContext().unregisterReceiver(nackReceiver);
            Log.d(TAG,"Successfully unregister");
        } catch (java.lang.IllegalArgumentException e) {
            //Log.e(TAG, e.getLocalizedMessage());
            Log.e(TAG, e.getMessage());
            Log.e(TAG, "Couldn't unregister dataReceiver");
        }

        try {
            //getApplicationContext().unregisterReceiver(mySmsReceiver);
            Log.d(TAG,"Successfully unregister mySmsReceiver");
        } catch (IllegalArgumentException e){
            Log.e(TAG, "Couldn't unregister the mySmsReceiver");
        }
    }

    private void updateMessageList(){
        list.clear();
        Log.d(TAG, "contact Phone NUmber: " + contacts.getPhoneNumber());
        List<MessageDataBase> dbQuery = db.getAllMessagesFrom("+1" + contacts.getPhoneNumber());
        //FakeReceiver fRecv = new FakeReceiver(-1, -1);
        // Hardcoded for testing
        for(int i = dbQuery.size()-1; i >= 0; i--){
            MessageDataBase currMsg = dbQuery.get(i);
            if(currMsg.getMessageFake().equals("")){
                List<Integer> puzzleMsg = MessageConverter.getIntegersFromHex(currMsg.getMessageBody());
                Log.d(TAG, "MessageConv:  " + puzzleMsg);
                Log.d(TAG, "MessageConv Part 2: " + fRecv.getMessage(puzzleMsg));
                Log.d(TAG, "MessageConv:  " + puzzleMsg);
                Log.d(TAG, "MessageConv Part 3: " + fRecv.getMessage(puzzleMsg));
                Log.d(TAG, "MessageConv:  " + puzzleMsg);
                String fakeDecodeCheck = fRecv.getMessage(puzzleMsg);
                Log.d(TAG, "MessageConv: Fake decode message: " + fakeDecodeCheck);
                currMsg.setMessageFake(fakeDecodeCheck);
                db.updateMessage(currMsg);
            }
            list.add(currMsg);
        }

        adapter.notifyDataSetChanged();

    }

    private void displaySearchDialog(){
        final Dialog searchDialog = new Dialog(ConversationActivity.this);
        searchDialog.setContentView((R.layout.conversation_search_dialog));
        searchDialog.setTitle("Search");
        final EditText searchStringEdit = (EditText) searchDialog.findViewById(R.id.searchStringDialog);

        Button searchDialogButton = (Button) searchDialog.findViewById(R.id.conversationSearchButton);
        searchDialogButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                searchStringForMessages = searchStringEdit.getText().toString();
                if(searchStringForMessages.equals("")){
                    Toast.makeText(getApplicationContext(), "Enter a valid search!", Toast.LENGTH_SHORT).show();
                    searchDialog.dismiss();
                    return;
                }
                if(list.size() == 0){
                    Toast.makeText(getApplicationContext(), "There are no messages to search!", Toast.LENGTH_LONG).show();
                    searchDialog.dismiss();
                    return;
                }
                searchDialog.dismiss();
                startSearch();
                //searchMessages(searchStringForMessages);
            }
        });

        searchDialog.show();
    }

    private void displaySearchedMessagesDialog(){
        final Dialog searchedMessageDialog = new Dialog(ConversationActivity.this);
        searchedMessageDialog.setContentView(R.layout.conversation_messages_dialog);
        searchedMessageDialog.setTitle("Searched Messages");
        final TextView searchedMessages = (TextView) searchedMessageDialog.findViewById(R.id.sMessageText);
        final TextView searchedMessagesDate = (TextView) searchedMessageDialog.findViewById(R.id.conversationSearchDate);
        final TextView searchedMessagesName = (TextView) searchedMessageDialog.findViewById(R.id.conversationSearchName);

        Button prevSearchedMessages = (Button) searchedMessageDialog.findViewById(R.id.conversationPreviousButton);
        Button nextSearchedMessages = (Button) searchedMessageDialog.findViewById(R.id.conversationNextButton);
        if(currIndexSearch < 0){
            Log.e(TAG, "currIndex is out of bounds");
            return;
        }
        Log.d(TAG, "CurrIndexAT: " + currIndexSearch);


        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        username = prefs.getString(LoginActivity.USERNAME_KEY,"");

        if(searchResult.get(currIndexSearch).getMessageAuthored()){
            cName = contacts.getName();
        }else{
            cName = username;
        }

        searchedMessagesName.setText(cName);
        searchedMessagesDate.setText(dateFormatter.getDate(searchResult.get(currIndexSearch).getMessageTime()));
        searchedMessages.setText(searchResult.get(currIndexSearch).getMessageBody());
        convoList.smoothScrollToPosition(Integer.parseInt(searchResult.get(currIndexSearch).getSender()));
        convoList.setItemChecked(Integer.parseInt(searchResult.get(currIndexSearch).getSender()), true);
        adapter.notifyDataSetChanged();

        prevSearchedMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currIndexSearch < 1) {
                    Toast.makeText(getApplicationContext(), "No previous messages", Toast.LENGTH_SHORT).show();
                    return;
                }
                currIndexSearch--;
                if (searchResult.get(currIndexSearch).getMessageAuthored()) {
                    cName = contacts.getName();
                } else {
                    cName = username;
                }
                searchedMessagesName.setText(cName);
                searchedMessagesDate.setText(dateFormatter.getDate(searchResult.get(currIndexSearch).getMessageTime()));
                searchedMessages.setText(searchResult.get(currIndexSearch).getMessageBody());
                convoList.smoothScrollToPosition(Integer.parseInt(searchResult.get(currIndexSearch).getSender()));
                convoList.setItemChecked(Integer.parseInt(searchResult.get(currIndexSearch).getSender()), true);
                adapter.notifyDataSetChanged();
            }
        });

        nextSearchedMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currIndexSearch > searchResult.size() - 2) {
                    Toast.makeText(getApplicationContext(), "No more messages", Toast.LENGTH_SHORT).show();
                    return;
                }
                currIndexSearch++;
                if (searchResult.get(currIndexSearch).getMessageAuthored()) {
                    cName = contacts.getName();
                } else {
                    cName = username;
                }
                searchedMessagesName.setText(cName);
                searchedMessagesDate.setText(dateFormatter.getDate(searchResult.get(currIndexSearch).getMessageTime()));
                searchedMessages.setText(searchResult.get(currIndexSearch).getMessageBody());
                convoList.smoothScrollToPosition(Integer.parseInt(searchResult.get(currIndexSearch).getSender()));
                convoList.setItemChecked(Integer.parseInt(searchResult.get(currIndexSearch).getSender()), true);
                adapter.notifyDataSetChanged();
            }
        });

        searchedMessageDialog.show();
    }

    private void startSearch(){

        searching = true;
        searchResult.clear();
        currIndexSearch = 0;
        try {
            ringProgressDialog.dismiss();
        } catch (Exception e){
            Log.e(TAG, "dismissed fault");
        }

        // Since we are going to scroll through messages as we search them,
        // Move the list view to the top of the message list
        convoList.smoothScrollToPosition(0);

        barProgressDialog = new ProgressDialog(this);
        barProgressDialog.setMessage("Searching...");
        barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        barProgressDialog.setIndeterminate(false);
        barProgressDialog.show();
        searchMessages();

    }

    private void searchMessages(){
        Log.i(TAG, "Searching message " + currIndexSearch);

        msgReceived="";
        chunkCount = -1;

        Log.d(TAG, "progressUpdate: " + (int)(((double)currIndexSearch/list.size())*100));
        // Update the progress bar percentage (0-100)
        barProgressDialog.setProgress(0);
        barProgressDialog.setProgress((int)(((double)currIndexSearch/list.size())*100));
        // Smooth scroll the list view to the message we are currently searching through
        convoList.smoothScrollToPosition(currIndexSearch);
        convoList.setItemChecked(currIndexSearch, true);
        adapter.notifyDataSetChanged();

        if(currIndexSearch >= list.size()){
            finishSearch();
        }else {
            messageClicked = list.get(currIndexSearch);
            sendDataToPebble();
        }
    }

    private boolean customMessageContains(String ogMessage, String searchString){
        // This is used by the message search function to determine if this string is a match
        // Operation:
        //    A normal string is just check to be contained in the ogMessage
        //    the string "###" will look for numbers
        if( searchString.equals(numberSearch) ){
            // This is a special look up, use a regex
            return ogMessage.matches("(.*)\\d+(.*)");
        } else {
            return ogMessage.toLowerCase().contains(searchString.toLowerCase());
        }
    }

    private void finishSearch(){
        searching = false;
        barProgressDialog.dismiss();
        for( int i=0; i<searchResult.size(); i++){
            Log.i(TAG, "Found matching message!");
            Log.i(TAG, searchResult.get(i).getMessageBody());
        }
        if( searchResult.size() == 0 ){
            Toast.makeText(getApplicationContext(), "No messages found matching the search text!", Toast.LENGTH_LONG).show();
            return;
        }
        currIndexSearch = 0;
        displaySearchedMessagesDialog();
        Toast.makeText(getApplicationContext(), "Found " + searchResult.size() + " matching messages!", Toast.LENGTH_LONG).show();
    }

    private void displayDialog(String name, String text, int msgStatus){
        final Dialog dialogConvo = new Dialog(ConversationActivity.this);
        if(msgStatus == MessageDataBase.MSG_FAILED){
            dialogConvo.setContentView(R.layout.conversation_dialog_retry);
        }else {
            dialogConvo.setContentView(R.layout.conversation_dialog);
        }
        dialogConvo.setTitle(name);

        TextView textConvo = (TextView) dialogConvo.findViewById(R.id.conversationText);
        textConvo.setText(text);
        Log.d(TAG, "DIALOG BOX");
        Button dialogButton = (Button) dialogConvo.findViewById(R.id.conversationDialogButton);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogConvo.dismiss();
            }
        });
        if(msgStatus == MessageDataBase.MSG_FAILED){
            Button retryDialogButton = (Button) dialogConvo.findViewById(R.id.conversationDialogRetryButton);
            retryDialogButton.setOnClickListener(new View.OnClickListener(){
               @Override
            public void onClick(View v){
                   Log.d(TAG, "Retrying Send SMS because MSG failed");
                   dialogConvo.dismiss();
                   retrySendSMS retry = new retrySendSMS();
                   retry.execute(messageClicked);
                   //Toast.makeText(getApplicationContext(), "Retrying Send SMS",Toast.LENGTH_SHORT).show();
               }
            });
        }

        dialogConvo.show();
    }

    private void sendDataToPebble(){

        if( WATCH_BUSY ){
            return;
        }
        WATCH_BUSY = true;
        lastDataToPebble = 1;
        if(!PebbleKit.isWatchConnected(getApplicationContext())){
            WATCH_BUSY = false;
            ringProgressDialog.dismiss();
            return;
        }
        String lastMsg = messageClicked.getMessageBody();

        byte[] bytesToPebble = MessageConverter.getMsgBytesFromHex(lastMsg);
        Log.d(TAG, "CHUNKCOUNT: " + chunkCount);
        if(chunkCount == -1){
            // This is the first pass at send the encoded message to pebble
            // We need to start by sending the scrambled puzzle which will need to be included in the message
            // scrambled puzzle will be the first 16 (8?) bytes

            // Watch assumes that these combined 8 bytes will be scrambled, not the 16
            byte[] puzzle = Arrays.copyOfRange(bytesToPebble, 0, 8);
            puzzle = ShuffleBytes.shuffleBytes(getApplicationContext(),puzzle);
            Log.d(TAG, "AM_DEC_PUZZ START");
            PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);
            PebbleDictionary message_data = new PebbleDictionary();
            message_data.addString(AM_DEC_PUZZLE_PHONE, contacts.getPhoneNumber());
            message_data.addInt32(AM_KEY_TYPE, messageClicked.getMessageAuthored() ? 1 : 2);
            message_data.addBytes(AM_DEC_PUZZLE, puzzle);
            PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, message_data);
            WATCH_BUSY = false;
            return;
        }

        //bytesToPebble = Arrays.copyOfRange(bytesToPebble, 8, bytesToPebble.length);

        int totalChunks = (int)Math.ceil((bytesToPebble.length-8)/(double)CHUNKSIZE);
        if(totalChunks == chunkCount) {
            //Toast.makeText(getApplicationContext(), "msgReceived: " + msgReceived, Toast.LENGTH_LONG).show();
            Log.d(TAG, "MessageReceived: " + msgReceived);

            chunkCount = -1;
            WATCH_BUSY = false;
            if(searching) {
                Log.d(TAG,"searching messages for contained word: " + currIndexSearch + " searchString: " + searchStringForMessages);
                if(customMessageContains(msgReceived,searchStringForMessages)) {
                    Log.d(TAG, "Found match: " + msgReceived + " searched: " + searchStringForMessages);
                    // MessageClicked contains the last message we searched through
                    String mTime = messageClicked.getMessageTime();
                    // Since we found a result, store it in a Message object for display later
                    MessageDataBase msg = new MessageDataBase();
                    msg.setMessageAuthored(messageClicked.getMessageAuthored());
                    msg.setMessageTime(mTime);
                    msg.setMessageBody(msgReceived);
                    // Lazy code: use the sender field of the Message object to track the location of this message in the listView
                    msg.setSender(String.valueOf(currIndexSearch));
                    searchResult.add(msg);
                }else{
                    Log.d(TAG, "didn't find: " + msgReceived);
                }

                currIndexSearch++;
                searchMessages();
            }else{
                Log.d(TAG, "sendDisplay from chunk count");
                sendDisplayToPebble();
            }
            return;
        }
        int message_size = CHUNKSIZE;
        if( CHUNKSIZE * (chunkCount+1) > (bytesToPebble.length-8)){
            // Our message chunk to send is smaller than CHUNKSIZE
            message_size = (bytesToPebble.length-8) - (CHUNKSIZE * chunkCount);
            Log.d(TAG, "message_Size: " + message_size + " | chunk_num: " + chunkCount);
        }

        byte byteArr[] = new byte[message_size+10];
        System.arraycopy(contacts.getPhoneNumber().getBytes(),0,byteArr,0,10);
        byte[] byteShuffled = ShuffleBytes.shuffleBytes(getApplicationContext(),Arrays.copyOfRange(bytesToPebble, ((CHUNKSIZE * chunkCount) + 8), (CHUNKSIZE * chunkCount) + message_size + 8));
        System.arraycopy(byteShuffled,0, byteArr, 10, byteShuffled.length);


        PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);
        PebbleDictionary message_data = new PebbleDictionary();
        Log.d(TAG, "CONVERSATION: byteArr divisible by 4?: " + ((byteArr.length - 10) % 4 == 0));
        if( messageClicked.getMessageAuthored() ){
            message_data.addBytes(AM_SENT_MESSAGE, byteArr);
        } else {
            message_data.addBytes(AM_READ_MESSAGE, byteArr);
        }
        message_data.addUint8(AM_KEY_NUM_VALS, (byte)((byteArr.length - 10)*2));
        PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, message_data);

        WATCH_BUSY = false;

    }

    private void sendDisplayToPebble() {

        Log.d(TAG, "Sending Message to display on watch");



        if (WATCH_BUSY) {
            return;
        }
        WATCH_BUSY = true;
        lastDataToPebble = 2;
        if (!PebbleKit.isWatchConnected(getApplicationContext())) {
            Log.d(TAG, "Watch is not connected");
            Toast toast = Toast.makeText(getApplicationContext(), "Watch is not connected", Toast.LENGTH_SHORT);
            toast.show();
            WATCH_BUSY = false;
            return;
        }

        PebbleKit.startAppOnPebble(getApplicationContext(), SSCHAT_UUID);
        PebbleDictionary message_data = new PebbleDictionary();

        if (chunkCount == -1) {
            Log.d(TAG, "Watch display initialization");
            message_data.addUint32(AM_MSG_SETUP, msgReceived.length());
        } else if (chunkCount == (int) Math.ceil(msgReceived.length() / (double) CHUNKSIZE)){
            // Finished sending all the message to the watch so display it
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
            String username = prefs.getString(LoginActivity.USERNAME_KEY,"");
            if(messageClicked != null) {
                if (!messageClicked.getMessageAuthored()) {
                    ringProgressDialog.dismiss();
                    displayDialog(contacts.getName(), msgReceived, messageClicked.getMessageStatus());
                } else {
                    ringProgressDialog.dismiss();
                    displayDialog(username, msgReceived, messageClicked.getMessageStatus());
                }
                message_data.addInt32(AM_MSG_DISPLAY, 0);
            }
        } else {
            // Have a chunk of a message to send
            if( (chunkCount+1) * CHUNKSIZE < msgReceived.length()){
                message_data.addString(AM_SEND_MESSAGE, msgReceived.substring(chunkCount * CHUNKSIZE, (chunkCount+1) * CHUNKSIZE));
            } else {
                message_data.addString(AM_SEND_MESSAGE, msgReceived.substring(chunkCount * CHUNKSIZE, msgReceived.length()-1));
            }
        }
        PebbleKit.sendDataToPebble(getApplicationContext(), SSCHAT_UUID, message_data);
        WATCH_BUSY = false;
    }

    public void onCreateMessage(View view){
        String extraName = contacts.getName();
        String extraContact = contacts.getPhoneNumber();

        Intent intent = new Intent(this, NewMessageActivity.class);
        intent.putExtra(EXTRA_NAME, extraName);
        intent.putExtra(EXTRA_NUMBER, extraContact);
        startActivity(intent);

    }

    private class retrySendSMS extends AsyncTask<MessageDataBase, Void, String>{

        @Override
        protected void onPreExecute(){
            ringProgressDialog = ProgressDialog.show(ConversationActivity.this, "Please Wait...", "Resending Message...",true);
            ringProgressDialog.setCancelable(true);
            ringProgressDialog.setCanceledOnTouchOutside(false);
        }

        @Override
        protected String doInBackground(MessageDataBase... params) {
            MessageDataBase messageToRetry = params[0];

            int messageID = messageToRetry.getID();
            String message = messageToRetry.getMessageBody();
            String recipient = contacts.getPhoneNumber();


            // For marking sent success or failure later
            Intent sentI = new Intent("android.provider.Telephony.SMS_SENT");
            sentI.putExtra("MessageID", messageID);
            Intent deliveredI = new Intent("android.provider.Telephony.SMS_DELIVERED");
            deliveredI.putExtra("MessageID", messageID);

            PendingIntent sentPi = PendingIntent.getBroadcast(getApplicationContext(), messageID, sentI, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent deliveredPi = PendingIntent.getBroadcast(getApplicationContext(), messageID, deliveredI, PendingIntent.FLAG_UPDATE_CURRENT);
            ArrayList<PendingIntent> sentPiMulti = new ArrayList<PendingIntent>();
            ArrayList<PendingIntent> deliveredPiMulti = new ArrayList<PendingIntent>();


            SmsManager sm = SmsManager.getDefault();

            // Check the message length in case we need to split it into multiple parts
            if( NewMessageActivity.SSPrefix.length() + message.length() >= 160 ){
                // Lets split this long message into smaller chunks for sending
                ArrayList<String> parts = sm.divideMessage(NewMessageActivity.SSPrefix + message);
                Log.d(TAG, "parts: " + parts);
                for(int i=0; i < parts.size(); i++){
                    sentPiMulti.add(i, sentPi);
                    deliveredPiMulti.add(i, deliveredPi);
                }
                sm.sendMultipartTextMessage(recipient, null, parts, sentPiMulti, deliveredPiMulti);
            } else {
                // This message will fit in a single text, so just send it
                sm.sendTextMessage(recipient, null, NewMessageActivity.SSPrefix + message, sentPi, deliveredPi);
            }

            return "";
        }



        @Override
        protected void onPostExecute(String result){
            ringProgressDialog.dismiss();
        }
    }


    /*private class retrySendSMS(MessageDataBase messageToRetry) {

        Log.d(TAG, "RingProgressDialog.show()");
        ringProgressDialog = ProgressDialog.show(ConversationActivity.this, "Please Wait...", "Resending Message...",true);
        ringProgressDialog.setCancelable(true);
        ringProgressDialog.setCanceledOnTouchOutside(false);

        int messageID = messageToRetry.getID();
        String message = messageToRetry.getMessageBody();
        String recipient = contacts.getPhoneNumber();


        // For marking sent success or failure later
        Intent sentI = new Intent("android.provider.Telephony.SMS_SENT");
        sentI.putExtra("MessageID", messageID);
        Intent deliveredI = new Intent("android.provider.Telephony.SMS_DELIVERED");
        deliveredI.putExtra("MessageID", messageID);

        PendingIntent sentPi = PendingIntent.getBroadcast(this, messageID, sentI, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent deliveredPi = PendingIntent.getBroadcast(this, messageID, deliveredI, PendingIntent.FLAG_UPDATE_CURRENT);
        ArrayList<PendingIntent> sentPiMulti = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPiMulti = new ArrayList<PendingIntent>();


        SmsManager sm = SmsManager.getDefault();

        // Check the message length in case we need to split it into multiple parts
        if( NewMessageActivity.SSPrefix.length() + message.length() >= 160 ){
            // Lets split this long message into smaller chunks for sending
            ArrayList<String> parts = sm.divideMessage(NewMessageActivity.SSPrefix + message);
            Log.d(TAG, "parts: " + parts);
            for(int i=0; i < parts.size(); i++){
                sentPiMulti.add(i, sentPi);
                deliveredPiMulti.add(i, deliveredPi);
            }
            sm.sendMultipartTextMessage(recipient, null, parts, sentPiMulti, deliveredPiMulti);
        } else {
            // This message will fit in a single text, so just send it
            sm.sendTextMessage(recipient, null, NewMessageActivity.SSPrefix + message, sentPi, deliveredPi);
        }

        ringProgressDialog.dismiss();
    }
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_conversation, menu);

        //MenuItem item = menu.add(Menu.NONE, R.id.action_compose, 1, "Compose");
        //item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        //item.setIcon(R.mipmap.ic_action_new_email);
        Log.d(TAG, "cinosddsdsassssdssdsssklssssasplpld");
        //invalidateOptionsMenu();
        //supportInvalidateOptionsMenu();

        menu.findItem(R.id.action_beatpoet).setChecked(beatPoetWords);

        return super.onCreateOptionsMenu(menu);

        //return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.action_search:
                displaySearchDialog();
                return true;
            case R.id.action_compose:
                onCreateMessage(null);
                return true;
            case R.id.action_beatpoet:
                if(item.isChecked()){
                    beatPoetWords = false;
                    Log.d(TAG, "Updating Message from BeatPoet item checked MENU");
                    adapter.ConversationBeatPoetChange(beatPoetWords);
                    updateMessageList();
                    item.setChecked(false);
                }else{
                    beatPoetWords = true;
                    adapter.ConversationBeatPoetChange(beatPoetWords);
                    updateMessageList();
                    item.setChecked(true);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }


    }

}
