package com.ssuser.ss.sunstonechat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Eric & Russell on 3/18/2015.
 */
public class ContactDatabaseHandler extends SQLiteOpenHelper {

    private static final String TAG = "ContactDatabaseHandlerActivity";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "contactDatabase";

    // Messages Table Name
    private static final String TABLE_CONTACTS = "contacts";

    // Messages Tables column names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_LINKED = "linked";
    private static final String KEY_VALID = "valid";

    public ContactDatabaseHandler(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db){
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_PHONE_NUMBER + " TEXT," + KEY_LINKED + " INTEGER,"
                + KEY_VALID + " INTEGER" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    // Upgrading Database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);

        // Create table again
        onCreate(db);
    }

    /**
     *  ALL CRUD ( CREATE, READ, UPDATE, DELETE ) Operations
     */

    void addContacts(Contacts contacts ){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, contacts.getName());
        values.put(KEY_PHONE_NUMBER, contacts.getPhoneNumber());
        values.put(KEY_LINKED, contacts.getLinked());
        values.put(KEY_VALID, contacts.getValid());

        // Inserting Row
        db.insert(TABLE_CONTACTS, null, values);
        db.close();

    }

    Contacts getContacts(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CONTACTS,
                new String[]{ KEY_ID, KEY_NAME, KEY_PHONE_NUMBER, KEY_LINKED, KEY_VALID}, KEY_ID + "=?",
                new String[]{ String.valueOf(id) }, null, null, null, null);
        if(cursor != null)
            cursor.moveToFirst();

        Contacts contacts = new Contacts(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2), cursor.getInt(3)==1,
                cursor.getInt(4)==1);

        return contacts;
    }

    Contacts getContacts(String phoneNumber){
        SQLiteDatabase db = this.getReadableDatabase();
        //Cursor cursor = db.query(TABLE_CONTACTS,
        //        new String[]{ KEY_ID, KEY_NAME, KEY_PHONE_NUMBER, KEY_LINKED, KEY_VALID}, KEY_ID + "=?",
         //       new String[]{ String.valueOf(id) }, null, null, null, null);
        Cursor cursor = db.query(TABLE_CONTACTS,
                new String[]{ KEY_ID, KEY_NAME, KEY_PHONE_NUMBER, KEY_LINKED, KEY_VALID}, KEY_PHONE_NUMBER + "=?",
                new String[]{phoneNumber}, null, null, null, null);
        if(cursor != null){
            if(cursor.moveToFirst()){
                Log.d(TAG, "Successfully moved to first item");
            } else {
                Log.d(TAG, "DB Query was empty...");
                return null;
            }
        }

        Contacts contacts = new Contacts(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2), cursor.getInt(3)==1,
                cursor.getInt(4)==1);

        return contacts;
    }

    public List<Contacts> getAllContacts(){
        List<Contacts> contactList = new ArrayList<Contacts>();

        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Looping through all rows and adding to list
        if(cursor.moveToFirst()){
            do {
                Contacts contacts = new Contacts();
                contacts.setID(Integer.parseInt(cursor.getString(0)));
                contacts.setName(cursor.getString(1));
                contacts.setPhoneNumber(cursor.getString(2));
                contacts.setLinked(cursor.getInt(3)==1);
                contacts.setValid(cursor.getInt(4)==1);

                //Adding messages to list
                contactList.add(contacts);
            } while (cursor.moveToNext());
        }

        return contactList;
    }

    public List<Contacts> getAllSortedContacts(Context context){
        final List<Contacts> contactsList = this.getAllContacts();
        final DatabaseHandler msgDB = new DatabaseHandler(context);

        // Comparator sorts in descending order
        Collections.sort(contactsList, new Comparator<Contacts>() {
            @Override
            public int compare(Contacts lhs, Contacts rhs) {
                long t1 = msgDB.getAllMessageRecentTimestamp("+1"+lhs.getPhoneNumber());
                Log.d(TAG, "getAllSortedt1: " + t1);
                long t2 = msgDB.getAllMessageRecentTimestamp("+1"+rhs.getPhoneNumber());
                Log.d(TAG, "getAllSortedt2: " + t2);
                if( t1 < t2 ) return 1;
                if( t1 == t2 ) return 0;
                return -1;
            }
        });

        return contactsList;

    }

    // Updating Single Message
    public int updateContact(Contacts contacts){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, contacts.getName());
        values.put(KEY_PHONE_NUMBER, contacts.getPhoneNumber());
        values.put(KEY_LINKED, contacts.getLinked());
        values.put(KEY_VALID, contacts.getValid());

        return db.update(TABLE_CONTACTS, values, KEY_ID + " =?",
                new String[]{ String.valueOf(contacts.getID()) });
    }

    public void deleteContact(Contacts contacts){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACTS, KEY_ID + " =?",
                new String[]{String.valueOf(contacts.getID())});
        db.close();
    }

    public int getContactsCount(){
        String countQuery = "SELECT * FROM " + TABLE_CONTACTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        return cursor.getCount();
    }


}
