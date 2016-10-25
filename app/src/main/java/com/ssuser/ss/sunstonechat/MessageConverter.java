package com.ssuser.ss.sunstonechat;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.ssuser.ss.sunstonechat.WordLibrary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Eric on 4/7/2015.
 */
public class MessageConverter {

    private static final String TAG = "MessageConverterClass";
    private static final String NULL_STRING = "___";
    private static final String UNKWN_START = "[[[";
    private static final String UNKWN_END   = "]]]";

    private String mMsg;
    private List<Byte> mBytes;

    public MessageConverter() {
        mMsg = "";
        mBytes = null;
    }

    public MessageConverter(String msg) {
        //mMsg = msg + NULL_STRING;
        updateData(msg + " " + NULL_STRING);
    }

    public MessageConverter(Byte[] bytes) {
        if(mBytes == null) {
            mBytes = new ArrayList<>(Arrays.asList(bytes));
        }
        else {
            mBytes.clear();
            mBytes.addAll(Arrays.asList(bytes));
        }
        updateMsg();
    }

    public void setMsg(String msg) {
        //mMsg = msg + NULL_STRING;
        updateData(msg + " " + NULL_STRING);
    }

    public void setBytes(Byte[] bytes) {
        if(mBytes == null) {
            mBytes = new ArrayList<>(Arrays.asList(bytes));
        }
        else {
            mBytes.clear();
            mBytes.addAll(Arrays.asList(bytes));
        }
        updateMsg();
    }

    public Byte[] getBytes(){
        Byte[] bytes = new Byte[mBytes.size()];
        mBytes.toArray(bytes);
        return bytes;
    }

    public String getMsgDisp() {
        StringBuilder sb = new StringBuilder();
        if(mMsg == null){
            return null;
        }
        String msgChunks[] = mMsg.split(" ");
        boolean unknown = false;
        for( String chunk: msgChunks){
            if( unknown ){
                if( chunk.equals(UNKWN_END)){
                    sb.append(" ");
                    unknown = false;
                } else {
                    sb.append(chunk);
                }
            } else if(chunk.equals(UNKWN_END)) {
                Log.e(TAG, "This Shouldn't happen!!!");
            } else if( chunk.equals(UNKWN_START)) {
                unknown = true;
            } else if( !chunk.equals(NULL_STRING)) {
                sb.append(chunk);
                sb.append(" ");
            }else if(chunk.equals(NULL_STRING)){
                break;
            }
        }
        return sb.toString();
    }

    public String getMsg() {
        return mMsg;
    }

    public static String getMsgHex(Byte[] bytes){
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for( int i=0; i<bytes.length; i++){
            int v = bytes[i] & 0xFF;
            hexChars[i*2] = hexArray[ v >>> 4];
            hexChars[i*2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] getMsgBytesFromHex(String msgHex){
        if( msgHex.length() % 2 != 0) Log.e(TAG, "Error: Invalid hex length!");
        byte[] bytes = new byte[msgHex.length() / 2];
        for( int i=0; i<bytes.length; i++){
            bytes[i] = (byte) ((Character.digit(msgHex.charAt(i*2), 16) << 4) +
                                Character.digit(msgHex.charAt(i*2 + 1), 16));
        }
        Log.d(TAG, "Hex length[" + msgHex.length() + "] bytes length [" + bytes.length + "]");
        return bytes;
    }

    public static List<Integer> getIntegersFromHex(String msgHex){

        List<Integer> integerList = new ArrayList<Integer>();
        for (int i = 0; i < msgHex.length(); i+=2){
            int val = Integer.parseInt(msgHex.substring(i, i+2),16);
            integerList.add(val);
        }

        return integerList;
    }

    public static List<Integer> getFakeIntegersFromHex(String msgHex){

        List<Integer> integerList = new ArrayList<Integer>();
        for (int i = 0; i < msgHex.length(); i+=2){
            int val = Integer.parseInt(msgHex.substring(i, i+2),16);
            integerList.add(val);
        }

        return integerList;
    }


    public static String getMsgSlim(Byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < bytes.length; i += 4) {
            //byte[] iBytes = {bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3]};
            byte[] lBytes = {0,0,0,0, bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3]};
            //int iWordVal = java.nio.ByteBuffer.wrap(iBytes).getInt();
            long lWordVal = java.nio.ByteBuffer.wrap(lBytes).getLong();
            Log.d(TAG, "getMsgSlim bytes[" + i + " - " + (i + 3) + "] as long = " + lWordVal);
            String word = WordLibrary.getInstance().getWordFromPosSlim(lWordVal);
            sb.append(word);
            sb.append(" ");
        }
        return sb.toString();
    }

    public static Byte[] getBytesSlim(String msg){

        List<Byte> byteList = new ArrayList<Byte>();

        String msgArr[] = msg.split(" ");
        Log.d(TAG, "getBytesSlim() msg:\"" + msg + "\"");
        Log.d(TAG, "getBytesSlim() words in msg:" + msgArr.length);
        int total_bytes = 0;
        int div = (int)(0xFFFFFFFFL / WordLibrary.getInstance().getTotalNumWords()) + 1;
        for(int i = 0; i < msgArr.length; ){
            int pos;
            if(msgArr[i].charAt(msgArr[i].length() - 1) == '.') {
                 pos = WordLibrary.getInstance().getPositionFromWordSlim(msgArr[i].substring(0, msgArr[i].length()-1));
                 if(pos < 0){
                     Log.e(TAG, "getBytesSlim - Couldnt find word: " + msgArr[i]);
                     return null;
                 }
                 i++;
            } else {
                pos = WordLibrary.getInstance().getPositionFromWordSlim(msgArr[i]);
                int remainder = WordLibrary.getInstance().getPositionFromWordSlim(msgArr[i+1]);
                if(pos < 0 || remainder < 0){
                    Log.e(TAG, "getBytesSlim - Couldnt find word: msgArr[i]: " + msgArr[i] + " | msgArr[i+1]: " + msgArr[i+1]);
                }
                pos *= div;
                pos += remainder;
                i += 2;
            }
            // Convert pos to bytes to send to pebble
            byte[] byteBuf = ByteBuffer.allocate(4).putInt(pos).array();
            byteList.add(byteBuf[0]);
            byteList.add(byteBuf[1]);
            byteList.add(byteBuf[2]);
            byteList.add(byteBuf[3]);
        }
        Byte[] ret_bytes = new Byte[byteList.size()];
        byteList.toArray(ret_bytes);
        return ret_bytes;
    }

    // assumes mMsg is already set
    private void updateData(String newMsg) {
        /*
        1) split mMsg to string array of words
        2) populate mBytes with values from WordLibrary
         */
        List<Integer> wordVals = new ArrayList<>();
        String[] msgWords = newMsg.split(" |\n|\t");

        for(int i = 0; i < msgWords.length; ++i) {
            Log.d("calculateWordValues()", "msgWords[" + i + "]: " + "\"" + msgWords[i] + "\"");
            // TODO: check for ALL special characters that are used in split.
            if(!msgWords[i].isEmpty() && !msgWords[i].equals(".") &&
                    !msgWords[i].equals("\n")) {
                int val = WordLibrary.getInstance().getPositionOfWord(msgWords[i], 0);

                if(val < 0) {
                    Log.d("calculateWordValues()", "NEG VAL");
                    val = WordLibrary.getInstance().getPositionOfWord(UNKWN_START, 0);
                    wordVals.add(val);

                    for(int j = 0; j < msgWords[i].length(); ++j) {
                        String curChar = msgWords[i].substring(j, j+1);
                        val = WordLibrary.getInstance().getPositionOfWord(curChar, 0);
                        if(val < 0) {
                            Log.d(TAG,"Oops, couldn't find word: \""+curChar+"\"");
                            Log.d("calculateWordValues()", "returning on word:" + i + ", char:" + j + " msg:\"" + mMsg +"\"");
                            return;
                        }
                        wordVals.add(val);
                    }

                    val = WordLibrary.getInstance().getPositionOfWord(UNKWN_END, 0);
                    wordVals.add(val);
                }
                else {
                    Log.d("calculateWordValues()", "POS VAL");
                    wordVals.add(val);
                }
            }
        }


        if(mBytes == null) {
            mBytes = new ArrayList<>();
        }
        else {
            mBytes.clear();
        }

        for(int i = 0; i < wordVals.size(); ++i) {
            byte[] bytes = ByteBuffer.allocate(4).putInt(wordVals.get((int)i)).array();
            mBytes.add(bytes[0]);
            mBytes.add(bytes[1]);
            mBytes.add(bytes[2]);
            mBytes.add(bytes[3]);
        }

        updateMsg();

    }

    // assumes mBytes is already set
    private void updateMsg() {
        /*
        1) iterate mBytes to get int values
        2) foreach generated int value, get a string from WordLibrary
        3) concat all strings into a msg and assign it to mMsg
         */

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < mBytes.size(); i += 4){

            byte[] bytes = {0,0,0,0,mBytes.get((int)i), mBytes.get((int)(i+1)),
                    mBytes.get((int)(i+2)), mBytes.get((int)(i+3))};

            long lWordVal = java.nio.ByteBuffer.wrap(bytes).getLong();
            String curStr = WordLibrary.getInstance().getWordFromPosition(lWordVal);
            //Log.d(TAG, "MESSSAGE CONVERTER: curString: " + curStr);
            sb.append(curStr);
            sb.append( " " );
        }
        //Log.d(TAG, "MESSSAGE CONVERTER - mBytes.size(): " + mBytes.size());

        mMsg = sb.toString();

    }


}
