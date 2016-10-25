package com.ssuser.ss.sunstonechat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Eric on 3/17/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHandlerActivity";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "messageDatabase";

    // Messages Table Name
    private static final String TABLE_MESSAGES = "messages";

    // Messages Tables column names
    private static final String KEY_ID = "id";
    private static final String KEY_SENDER = "sender";
    private static final String KEY_MESSAGE_BODY = "message_body";
    private static final String KEY_MESSAGE_TIME = "message_time";
    private static final String KEY_MESSAGE_FAKE = "message_fake";
    private static final String KEY_MESSAGE_READ = "message_read";
    private static final String KEY_MESSAGE_AUTHORED = "message_authored";
    private static final String KEY_MESSAGE_STATUS = "message_status";

    public DatabaseHandler(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db){
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_SENDER + " TEXT,"
                + KEY_MESSAGE_BODY + " TEXT," + KEY_MESSAGE_TIME + " TEXT,"
                + KEY_MESSAGE_FAKE + " TEXT,"
                + KEY_MESSAGE_READ + " INTEGER,"
                + KEY_MESSAGE_AUTHORED + " INTEGER,"
                + KEY_MESSAGE_STATUS + " INTEGER"
                + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    // Upgrading Database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);

        // Create table again
        onCreate(db);
    }

    /**
     *  ALL CRUD ( CREATE, READ, UPDATE, DELETE ) Operations
     */

    void addMessage(MessageDataBase messages ){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_SENDER, messages.getSender());
        values.put(KEY_MESSAGE_BODY, messages.getMessageBody());
        values.put(KEY_MESSAGE_TIME, messages.getMessageTime());
        values.put(KEY_MESSAGE_FAKE, messages.getMessageFake());
        values.put(KEY_MESSAGE_READ, messages.getMessageRead());
        values.put(KEY_MESSAGE_AUTHORED, messages.getMessageAuthored());
        values.put(KEY_MESSAGE_STATUS, messages.getMessageStatus());

        // Inserting Row
        long result = db.insert(TABLE_MESSAGES, null, values);
        Log.d(TAG, "Result from databaseHandler: " + result);
        db.close();

    }

    long addMessageWithReturn(MessageDataBase messages){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_SENDER, messages.getSender());
        values.put(KEY_MESSAGE_BODY, messages.getMessageBody());
        values.put(KEY_MESSAGE_TIME, messages.getMessageTime());
        values.put(KEY_MESSAGE_FAKE, messages.getMessageFake());
        values.put(KEY_MESSAGE_READ, messages.getMessageRead());
        values.put(KEY_MESSAGE_AUTHORED, messages.getMessageAuthored());
        values.put(KEY_MESSAGE_STATUS, messages.getMessageStatus());

        // Inserting Row
        long result = db.insert(TABLE_MESSAGES, null, values);
        Log.d(TAG, "Result from databaseHandlerWithReturn: " + result);
        db.close();

        return result;
    }

    MessageDataBase getMessage(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES,
                new String[]{ KEY_ID, KEY_SENDER, KEY_MESSAGE_BODY, KEY_MESSAGE_TIME, KEY_MESSAGE_FAKE, KEY_MESSAGE_READ, KEY_MESSAGE_AUTHORED, KEY_MESSAGE_STATUS}, KEY_ID + "=?",
                new String[]{ String.valueOf(id) }, null, null, null, null);
        if(cursor != null) {
            boolean hasElement = cursor.moveToFirst();
            if( !hasElement ){
                Log.e(TAG, "The database is empty!");
                return null;
            }
        }


        MessageDataBase messages = new MessageDataBase(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_ID))),
                cursor.getString(cursor.getColumnIndex(KEY_SENDER)), cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_BODY)),
                cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_TIME)), cursor.getInt(cursor.getColumnIndex(KEY_MESSAGE_READ))==1,
                cursor.getInt(cursor.getColumnIndex(KEY_MESSAGE_AUTHORED))==1, Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_STATUS))));

        messages.setMessageFake(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_FAKE)));

        return messages;
    }

    public List<MessageDataBase> getAllMessages(){
        List<MessageDataBase> messagesList = new ArrayList<MessageDataBase>();

        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Looping through all rows and adding to list
        if(cursor.moveToFirst()){
            do {
                MessageDataBase messages = new MessageDataBase();
                messages.setID(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_ID))));
                messages.setSender(cursor.getString(cursor.getColumnIndex(KEY_SENDER)));
                messages.setMessageBody(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_BODY)));
                messages.setMessageTime(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_TIME)));
                messages.setMessageFake(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_FAKE)));
                messages.setMessageRead(cursor.getInt(cursor.getColumnIndex(KEY_MESSAGE_READ))==1);
                messages.setMessageAuthored(cursor.getInt(cursor.getColumnIndex(KEY_MESSAGE_AUTHORED))==1);
                messages.setMessageStatus(cursor.getInt(cursor.getColumnIndex(KEY_MESSAGE_STATUS)));

                //Adding messages to list
                messagesList.add(messages);
            } while (cursor.moveToNext());
        }

        return messagesList;
    }

    public List<MessageDataBase> getAllMessagesFrom(String phoneNumber){
        List<MessageDataBase> messagesList = new ArrayList<MessageDataBase>();

        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Looping through all rows and adding to list
        if(cursor.moveToFirst()){
            do {
                //Log.d(TAG, "cursor.getString(1): " + cursor.getString(cursor.getColumnIndex(KEY_SENDER)) + " | phoneNumber: " + phoneNumber);
                if( !cursor.getString(1).equals(phoneNumber)) continue;
                //Log.d(TAG, "" + (cursor.getString(1) == phoneNumber));
                MessageDataBase messages = new MessageDataBase();
                messages.setID(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_ID))));
                messages.setSender(cursor.getString(cursor.getColumnIndex(KEY_SENDER)));
                messages.setMessageBody(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_BODY)));
                messages.setMessageTime(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_TIME)));
                messages.setMessageFake(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_FAKE)));
                messages.setMessageRead(cursor.getInt(cursor.getColumnIndex(KEY_MESSAGE_READ))==1);
                messages.setMessageAuthored(cursor.getInt(cursor.getColumnIndex(KEY_MESSAGE_AUTHORED))==1);
                messages.setMessageStatus(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_MESSAGE_STATUS))));

                //Adding messages to list
                messagesList.add(messages);
            } while (cursor.moveToNext());
        }

        return messagesList;
    }

    public int getUnreadMessagesCount(){

        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        int count = 0;

        if(cursor.moveToFirst()){
            do {
                if(cursor.getInt(5)==0){
                    count++;
                }
            } while (cursor.moveToNext());

        }

        return count;
    }


    public long getAllMessageRecentTimestamp(String phoneNumber){
        List<MessageDataBase> msgs = this.getAllMessagesFrom(phoneNumber);
        if(msgs.size() == 0) return 0;

        Collections.sort(msgs, new Comparator<MessageDataBase>(){
            public int compare(MessageDataBase m1, MessageDataBase m2){
                long m1_time = Long.valueOf(m1.getMessageTime());
                long m2_time = Long.valueOf(m2.getMessageTime());
                if( m1_time > m2_time) return 1;
                if( m1_time == m2_time) return 0;
                return -1;
            }
        });
        // The messages are now sorted by message time in ascending order
        // So the last message in the list is the most recent one.
        //Log.d(TAG, "Most recent TS from phoneNumber: " + phoneNumber + " | and time: " + Long.valueOf(msgs.get(msgs.size()-1).getMessageTime()));
        return Long.valueOf(msgs.get(msgs.size()-1).getMessageTime());
    }

    public void deleteAllMessagesFrom(String phoneNumber){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, KEY_SENDER + " =?",
                new String[]{String.valueOf(phoneNumber)});
        db.close();
    }

    public boolean hasUnreadMessages(String phoneNumber){

        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Looping through all rows and adding to list
        if(cursor.moveToFirst()){
            do {
                //Log.d(TAG, "cursor.getString(1): " + cursor.getString(1) + " | phoneNumber: " + phoneNumber);
                if( !cursor.getString(1).equals(phoneNumber)) continue;
                //Log.d(TAG, "" + (cursor.getString(1) == phoneNumber));
                if(cursor.getInt(5)==0){
                    return true;
                }

            } while (cursor.moveToNext());
        }

        return false;
    }

    // Updating Single Message
    public int updateMessage(MessageDataBase messages){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_SENDER, messages.getSender());
        values.put(KEY_MESSAGE_BODY, messages.getMessageBody());
        values.put(KEY_MESSAGE_TIME, messages.getMessageTime());
        values.put(KEY_MESSAGE_FAKE, messages.getMessageFake());
        values.put(KEY_MESSAGE_READ, messages.getMessageRead());
        values.put(KEY_MESSAGE_AUTHORED, messages.getMessageAuthored());
        values.put(KEY_MESSAGE_STATUS, messages.getMessageStatus());

        return db.update(TABLE_MESSAGES, values, KEY_ID + " =?",
                new String[]{ String.valueOf(messages.getID()) });
    }

    public void deleteMessage(MessageDataBase messages){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, KEY_ID + " =?",
                new String[]{String.valueOf(messages.getID())});
        db.close();
    }

    public int getMessagesCount(){
        String countQuery = "SELECT * FROM " + TABLE_MESSAGES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();

        return count;
    }


}
