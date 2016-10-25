package com.ssuser.ss.sunstonechat;

import java.io.Serializable;

/**
 * Created by Eric & Russell on 3/18/2015.
 */
public class Contacts implements Serializable {

        int _id;
        String _name;
        String _phone_number;
        boolean _linked;
        boolean _isValid;

        // Empty Constructor
        public Contacts(){

        }

        // Constructor
        public Contacts(int id, String name, String phone_number, boolean linked, boolean valid){
            this._id = id;
            this._name = name;
            this._phone_number = phone_number;
            this._linked = linked;
            this._isValid = valid;
        }

        public Contacts( String name, String phone_number, boolean linked, boolean valid){
            this._name = name;
            this._phone_number = phone_number;
            this._linked = linked;
            this._isValid = valid;
        }

        public int getID(){ return this._id; }

        public void setID(int id){ this._id = id; }

        public String getName(){ return this._name; }

        public void setName(String name){ this._name = name; }

        public String getPhoneNumber(){ return this._phone_number; }

        public void setPhoneNumber(String phone_number){ this._phone_number = phone_number; }

        public boolean getLinked(){ return this._linked; }

        public void setLinked(boolean linked){this._linked = linked; }

        public boolean getValid(){ return this._isValid; }

        public void setValid(boolean valid){ this._isValid = valid; }
}
