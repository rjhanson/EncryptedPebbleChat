package com.ssuser.ss.sunstonechat;

/**
 * Created by Eric on 3/17/2015.
 */
public class MessageDataBase {

    int _id;
    String _sender;
    String _message_body;
    String _message_time;
    String _message_fake;
    boolean _message_read;
    boolean _message_authored;          // TRUE, if COMPOSED, FALSE, if RECEIVED
    int _message_status;

    // Defines for message status
    public static final int MSG_SENT = 0;
    public static final int MSG_SENDING = 1;
    public static final int MSG_FAILED = 2;
    public static final int MSG_RECEIVED = 3;

    // Empty Constructor
    public MessageDataBase(){

    }

    // Constructor
    public MessageDataBase(int id, String sender, String message_body, String message_time, boolean message_read, boolean message_authored, int message_status){
        this._id = id;
        this._sender = sender;
        this._message_body = message_body;
        this._message_time = message_time;
        this._message_fake = "";
        this._message_read = message_read;
        this._message_authored = message_authored;
        this._message_status = message_status;
    }

    public MessageDataBase( String sender, String message_body, String message_time, boolean message_read, boolean message_authored){
        this._sender = sender;
        this._message_body = message_body;
        this._message_time = message_time;
        this._message_fake = "";
        this._message_read = message_read;
        this._message_authored = message_authored;
        this._message_status = MSG_SENDING;
    }

    public int getID(){ return this._id; }

    public void setID(int id){ this._id = id; }

    public String getSender(){ return this._sender; }

    public void setSender(String sender){ this._sender = sender; }

    public String getMessageBody(){ return this._message_body; }

    public void setMessageBody(String message_body){ this._message_body = message_body; }

    public String getMessageFake(){ return this._message_fake; }

    public void setMessageFake(String message_fake){ this._message_fake = message_fake; }

    public String getMessageTime(){return this._message_time;}

    public void setMessageTime(String message_time){this._message_time = message_time;}

    public boolean getMessageRead(){ return this._message_read; }

    public void setMessageRead(boolean message_read){this._message_read = message_read; }

    public boolean getMessageAuthored(){return this._message_authored;}

    public void setMessageAuthored(boolean message_authored){this._message_authored = message_authored;}

    public int getMessageStatus(){return this._message_status;}

    public void setMessageStatus(int message_status){ this._message_status = message_status;}

    public String toString(){
        String str = "";
        str += "From: " + this._sender + '\n';
        str += "Body: " + this._message_body + '\n';
        str += "Time: " + this._message_time + '\n';
        str += "Fake: " + this._message_fake + '\n';
        str += "Read: " + this._message_read + '\n';
        str += "Authored: " + this._message_authored;
        return str;
    }


}
