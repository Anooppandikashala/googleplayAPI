package edu.cs4730.firebasemessagedemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// using https://www.simplifiedcoding.net/firebase-cloud-messaging-tutorial-android/
//   and https://www.simplifiedcoding.net/firebase-cloud-messaging-android/
//   and https://www.androidtutorialpoint.com/firebase/firebase-android-tutorial-getting-started/
//   and https://www.androidtutorialpoint.com/firebase/firebase-cloud-messaging-tutorial/
//   and https://firebase.google.com/docs/cloud-messaging/


public class MainActivity extends AppCompatActivity {

    //defining views
    private Button buttonSendPush;
    private Button buttonRegister;
    private EditText editTextName;
    private ProgressDialog progressDialog;

    //URL to RegisterDevice.php
    private static final String URL_REGISTER_DEVICE = "http://10.216.218.12/FcmDemo/RegisterDevice.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getting views from xml
        //getting views from xml
        editTextName = (EditText) findViewById(R.id.editTextName);
        buttonRegister = (Button) findViewById(R.id.buttonRegister);
        buttonSendPush = (Button) findViewById(R.id.buttonSendNotification);

        //adding listener to view
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTokenToServer();

            }
        });
        buttonSendPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SendActivity.class));

            }
        });
    }


    //storing token to mysql server
    private void sendTokenToServer() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering Device...");
        progressDialog.show();

        final String token = SharedPrefManager.getInstance(this).getDeviceToken();
        final String name = editTextName.getText().toString();

        if (token == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Token not generated", Toast.LENGTH_LONG).show();
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_REGISTER_DEVICE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        try {
                            JSONObject obj = new JSONObject(response);
                            Toast.makeText(MainActivity.this, obj.getString("message"), Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("token", token);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
}
