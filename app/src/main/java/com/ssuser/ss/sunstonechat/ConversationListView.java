package com.ssuser.ss.sunstonechat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Eric on 5/5/2015.
 */
public class ConversationListView extends BaseAdapter{

    private static final String TAG = "ConversationListView";
    private List<MessageDataBase> list = new ArrayList<MessageDataBase>();
    private boolean beatpoetWords;
    private Context context;

    public ConversationListView(Context mContext, boolean mBeatpoetWords, ArrayList<MessageDataBase> mList ){
        context = mContext;
        list = mList;
        beatpoetWords = mBeatpoetWords;
    }

    public void ConversationBeatPoetChange(boolean mBeatpoetWords){
        beatpoetWords = mBeatpoetWords;
    }


    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Get the combined sender|body string from the list
        MessageDataBase msg = list.get(position);
        String messageBody;
        String messageTime = msg.getMessageTime();

        // Try to figure out if this view (row) is currently selected
        ListView parentView = (ListView) parent;
        boolean selected = parentView.isItemChecked(position);

        // Check if we are displaying rows with words or hex characters
        if( beatpoetWords ){
            messageBody = msg.getMessageFake();
            String msgArr[] = messageBody.split("\\s+");
            if(msgArr.length > 8){
                messageBody = TextUtils.join(" ", Arrays.copyOfRange(msgArr, 0, 7));
            }
        } else {
            messageBody = msg.getMessageBody();
            if(messageBody.length() > 18){
                messageBody = messageBody.substring(0,15) + "...";
            }
        }

        // Chose the appropriate row design for a sent or received message
        if(msg.getMessageAuthored()){
            view = vi.inflate(R.layout.conversation_listview_item_left,null);
        }else{
            view = vi.inflate(R.layout.conversation_listview_item_right,null);
        }

        // References to the two TextViews in our conversation list item
        TextView subject = (TextView) view.findViewById(R.id.messageBody);
        TextView timestamp = (TextView) view.findViewById(R.id.messageDate);
        ImageView searchImage = (ImageView) view.findViewById(R.id.searchIndex);

        // Set the datetime text as specified in the date formatter
        timestamp.setText(dateFormatter.getDate(messageTime));

        // If the message is unread, set the text to be BOLD!
        subject.setText(messageBody);
        if(!msg.getMessageRead()){
            subject.setTypeface(null, Typeface.BOLD);
            timestamp.setTypeface(null, Typeface.BOLD);
        }else{
            subject.setTypeface(null, Typeface.NORMAL);
            timestamp.setTypeface(null, Typeface.NORMAL);
        }
        Log.d(TAG, "messageBody: " + messageBody + " | msg.id: " + msg.getID() + " | msgStatus: " + msg.getMessageStatus());

        // What is this messages status?
        switch (msg.getMessageStatus()){
            case MessageDataBase.MSG_SENDING:
                // The message is still in the process of sending... (spinner animation or something?)
                Log.d(TAG, "MSG_SENDING");
                break;
            case MessageDataBase.MSG_SENT:
                // A successfully send message should likely also be left alone
                Log.d(TAG, "MSG_SENT");
                view.setBackgroundColor(Color.TRANSPARENT);
                break;
            case MessageDataBase.MSG_RECEIVED:
                // A message that came from a contact should be left alone
                Log.d(TAG, "MSG_RECEIVED");
                break;
            case MessageDataBase.MSG_FAILED:
                // A message that failed to send, should flag the message in the list view with a failure warning
                Log.d(TAG, "MSG_FAILED");
                view.setBackgroundResource(R.drawable.custom_bg_1);
                // Try setting a red exclamation point on the views that haven't sent yet
                searchImage.setImageResource(R.mipmap.exclamation_point);
                searchImage.setVisibility(View.VISIBLE);
                break;
        }

        // Display an arrow to the currently selected message
        // The selected message currently has priority over the failed to send icon
        if( selected ){
            // in case this message is newly resent, lets make sure this is the arrow icon
            searchImage.setImageResource(R.mipmap.ic_action_next_item);
            searchImage.setVisibility(View.VISIBLE);
        } else {
            // We don't want the not sent image to be invisible even if it isn't selected
            if( !(msg.getMessageStatus() == MessageDataBase.MSG_FAILED) )
                searchImage.setVisibility(View.INVISIBLE);
        }

        return view;
    }

}
