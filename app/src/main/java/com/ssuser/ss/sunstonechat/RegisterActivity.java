package com.ssuser.ss.sunstonechat;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class RegisterActivity extends ActionBarActivity {

    private static final String TAG = "RegisterActivity";

    private ProgressDialog ringProgressDialog;

    private EditText usernameEntry;
    private EditText emailEntry;
    private EditText phoneEntry;
    private EditText passwordEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameEntry = (EditText) findViewById(R.id.usernameRegisterId);
        emailEntry    = (EditText) findViewById(R.id.emailRegisterId);
        phoneEntry    = (EditText) findViewById(R.id.phoneRegisterId);
        passwordEntry = (EditText) findViewById(R.id.passwordRegisterId);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /*noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        */
        return super.onOptionsItemSelected(item);
    }

    public void onRegisterSubmit(View view){
        String username = usernameEntry.getText().toString();
        String email    = emailEntry.getText().toString();
        String phone    = phoneEntry.getText().toString();
        String password = passwordEntry.getText().toString();

        new submitRegister().execute(username,email,phone,password);
        Log.d(TAG, "ring Progress dialog in Register");
        ringProgressDialog = ProgressDialog.show(RegisterActivity.this,"Registering", "Please wait...",true);
        ringProgressDialog.setCancelable(false);

    }

    public class submitRegister extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params){
            String result = null;
            // Create a new HTTPClient and Post Header
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://192.168.1.200:8078/sunstonechat/register/");
            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
                nameValuePairs.add(new BasicNameValuePair("username", params[0]));
                nameValuePairs.add(new BasicNameValuePair("email", params[1]));
                nameValuePairs.add(new BasicNameValuePair("phone_number", params[2]));
                nameValuePairs.add(new BasicNameValuePair("password", params[3]));
                nameValuePairs.add(new BasicNameValuePair("app","true"));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();

                result = EntityUtils.toString(entity);

                Log.d(TAG, "Response from post is: " + result);
            } catch (ClientProtocolException e){
                Log.e(TAG, "ClientProtocolException error");
            } catch (IOException e){
                Log.e(TAG, "IOException error: " + e);
            }
            return result;
        }

        protected void onPostExecute(String result) {
            boolean success = false;
            ringProgressDialog.dismiss();
            if (result == null) {
                Log.e(TAG, "POST failed");
                return;
            }
            String resultMessage = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                success = jsonObject.getBoolean("success");
                resultMessage = jsonObject.getString("reason");
                Log.d(TAG, "Did registration succeed? " + success);
            } catch (JSONException e) {
                Log.e(TAG, "Failed parsing JSON data from post response");
                // Should get a wrong password or username message here
                // Pass that info back to the user on the page
                return;
            }

            if( success ){
                finish();
            } else {
                Toast.makeText(getApplicationContext(), resultMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}
