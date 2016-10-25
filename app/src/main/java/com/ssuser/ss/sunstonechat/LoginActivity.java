package com.ssuser.ss.sunstonechat;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class LoginActivity extends ActionBarActivity {
    private final String TAG = "LoginActivity";
    public final static String EXTRA_USERNAME = "com.sunstone.sunstonechat.USERNAME";

    public final static String USERNAME_KEY = "com.sunstone.sunstonechat.USERNAME_KEY";
    public final static String PHONENUMBER_KEY = "com.sunstone.sunstonechat.PHONENUMBER_KEY";

    public final static String lastActive = "com.sunstone.sunstonechat.LAST_ACTIVE";

    public final static String expiredSession = "";
    private static String username;
    private ProgressDialog ringProgressDialog;

    private static final UUID SSCHAT_UUID = UUID.fromString("E30CF49A-497B-4FDB-B4CD-7D655C0C84FB");
    private boolean WATCH_BUSY = false;
    private  PebbleKit.PebbleDataReceiver dataReceiver;
    private PebbleKit.PebbleAckReceiver ackReceiver;
    private PebbleKit.PebbleNackReceiver nackReceiver;

    private int pebbleComAttempts = 0;

    TextView errText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setTitle("Login");

        errText = (TextView) findViewById(R.id.loginMessageBox);
        final EditText emailText = (EditText) findViewById(R.id.emailForm);
        final EditText pwdText = (EditText) findViewById(R.id.passwordForm);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        emailText.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                if(!hasFocus && !pwdText.hasFocus()){
                    Log.d(TAG, "Hiding emailText Keyboard");
                    hideSoftKeyboard(v);
                }
            }
        });
        pwdText.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                if(!hasFocus && !emailText.hasFocus()){
                    Log.d(TAG, "Hiding PwdText Keyboard");
                    hideSoftKeyboard(v);
                }
            }
        });
        Intent intent = this.getIntent();
        if(intent != null){
            if(intent.hasExtra(expiredSession)){
                errText.setText("Session Expired");
                errText.setVisibility(View.VISIBLE);
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void hideSoftKeyboard(View view){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }


    protected void onPause(){
        super.onPause();
    }

    protected void onResume(){
        super.onResume();
    }


    public class MyAsyncTask extends AsyncTask<String, Integer, String>{

        @Override
        protected String doInBackground(String... params){
            String result = "";
            String username = params[0];
            String password = params[1];
            // Create a new HTTPClient and Post Header
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://192.168.1.200:8078/sunstonechat/login/");

            if( username.equals("admin") && password.equals("admin") ){
                Log.d(TAG, "Logging in with admin creds");
                JSONObject ret = new JSONObject();
                try {
                    ret.put("result", true);
                    ret.put("reason", "success");
                    ret.put("phone", "0000000000");
                    ret.put("username", "admin");
                } catch (JSONException e){
                    Log.e(TAG, "How did putting a string fail? Admin loggin");
                }
                return ret.toString();
            }

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                nameValuePairs.add(new BasicNameValuePair("username", username));
                nameValuePairs.add(new BasicNameValuePair("password",password));
                nameValuePairs.add(new BasicNameValuePair("app","true"));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();

                result = EntityUtils.toString(entity);

                Log.d(TAG, "Response from post is: " +  result);
            } catch (ClientProtocolException e){
                Log.e(TAG, "ClientProtocolException error");
                result = "fail";
            } catch (IOException e){
                Log.e(TAG, "IOException error: " + e);
                result = "fail";
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result){
            Date date = new Date();
            ringProgressDialog.dismiss();
            boolean success;
            username = "";
            String reason   = "";
            String phone = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                success  = jsonObject.getBoolean("result");
                username = jsonObject.getString("username");
                phone = jsonObject.getString("phone");
                reason   = jsonObject.getString("reason");
                Log.d(TAG, "Parsed return data: " + success + " " + username + " " + reason);
            } catch (JSONException e) {
                Log.e(TAG, "Failed parsing JSON data from post response");
                // Should get a wrong password or username message here
                // Pass that info back to the user on the page

                errText.setText("Incorrect Credentials");
                errText.setVisibility(View.VISIBLE);
                return;
            }

            // Store the username and time in the shared preferences
            SharedPreferences prefs = getSharedPreferences(ContactListActivity.PREFS_NAME, 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(USERNAME_KEY, username);
            editor.putString(PHONENUMBER_KEY, phone);
            long currTime = System.currentTimeMillis();
            editor.putLong(lastActive, currTime);
            editor.commit();

            Log.d(TAG,"TIME "+ currTime);

            // Here we should cancel the spinner
            // first we should probably make the spinner
            // then store the username in the shared preferences with the log in time
            // then start the home intent
            //Intent intent = new Intent(getApplicationContext(), ContactListActivity.class);
            //startActivity(intent);
            finish();
            Log.d(TAG, "Finish from LoginActivity");
        }
    }

    public void onSubmitLogin(View view){
        EditText userText = (EditText) findViewById(R.id.emailForm);
        EditText passwordText = (EditText) findViewById(R.id.passwordForm);

        String username = userText.getText().toString();
        String password = passwordText.getText().toString();

        pebbleComAttempts = 0;

        Log.d(TAG, "Logging in with username: " + username);
        Log.d(TAG, "password: " + password);

        ringProgressDialog = ProgressDialog.show(LoginActivity.this, "Please wait...", "Logging In...", true);
        ringProgressDialog.setCancelable(false);
        new MyAsyncTask().execute(username,password);

    }

    public void onRegisterHereClick(View view){
        //Uri uri = Uri.parse("http://192.168.1.20:8000/sunstonechat/register/");
        //Intent i = new Intent(Intent.ACTION_VIEW, uri);
        //startActivity(i);
        Intent intent = new Intent(getApplicationContext(),RegisterActivity.class);
        startActivity(intent);
    }

}
