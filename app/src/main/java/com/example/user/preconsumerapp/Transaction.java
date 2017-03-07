package com.example.user.preconsumerapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import static com.example.user.preconsumerapp.MainActivity.ConnectionAlert;
import static com.example.user.preconsumerapp.R.id.spinner;

public class Transaction extends AppCompatActivity {
    ConnectivityManager connectivityManager;
    NetworkInfo activeNetworkInfo;

    //string for nxt link
    String link1, link2, link3, link4;

    //string for QR scanning result
    String batchID,movement,productName;

    JsonObjectRequest postRequest;
    JSONObject responseData = null;
    RequestQueue queue;
    Button btnPost;

    public static AlertDialog PostConnectionAlert;

    //nxt url parts
    String nxtAccNum ="NXT-2N9Y-MQ6D-WAAS-G88VH";
    String secretPhrase = "appear morning crap became fire liquid probably tease rare swear shut grief";
    private static final String nxtPostLinkPart1 = "http://174.140.168.136:6876/nxt?requestType=sendMessage&secretPhrase=";
    private static final String nxtPostLinkPart2 = "&recipient=";
    private static final String nxtPostLinkPart3 = "&message=";
    private static final String nxtPostLinkPart4 = "&deadline=60&feeNQT=0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        queue= Volley.newRequestQueue(this);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        try {
            responseData = new JSONObject(intent.getStringExtra("jsonObjInString"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        nxtAccNum = intent.getStringExtra("nxtAccNum");
        productName = intent.getStringExtra("productName");
        batchID = intent.getStringExtra("batchID");
        movement = intent.getStringExtra("movement");

        btnPost = (Button) findViewById(R.id.button);
        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject toPost1 = new JSONObject();
                JSONObject toPost2 = new JSONObject();
                JSONObject toPost3 = new JSONObject();
                JSONObject toPost4 = new JSONObject();

                secretPhrase = secretPhrase.replaceAll(" ","%20");

                /* link = "http://174.140.168.136:6876/nxt?requestType=sendMessage&secretPhrase="+ secret +"&recipient="+ nxtAccNum +"&message=" + toPost +"&deadline=60&feeNQT=0";  // nxt api call for sending message
                    Log.d("asdf",link);*/

                if (responseData == null) {
                    Log.d("responseData: ", "Null");
                    Toast.makeText(getApplicationContext(), "Please ensure that you are connected to a network with a working server", Toast.LENGTH_LONG).show();
                } else if (nxtAccNum == null) {
                    Log.d("nxtAcc", "Null");
                    Toast.makeText(getApplicationContext(), "Please scan a valid QR before trying to make a transaction", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("Response and Acc: ", "Not Null");
                    try {
                        //first post data
                        toPost1.put("batchID", batchID);
                        toPost1.put("movement", movement);
                        toPost1.put("unhashedData", responseData.getString("unhashedData"));
                        link1 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                URLEncoder.encode(toPost1.toString()) + nxtPostLinkPart4;

                        //second post data
                        toPost2.put("batchID", batchID);
                        toPost2.put("encryptedHash1", responseData.getString("encryptedHash1"));
                        link2 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                URLEncoder.encode(toPost2.toString()) + nxtPostLinkPart4;

                        //third post data
                        toPost3.put("batchID", batchID);
                        toPost3.put("encryptedHash1", responseData.getString("encryptedHash2"));
                        link3 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                URLEncoder.encode(toPost3.toString()) + nxtPostLinkPart4;

                        //fourth post data
                        toPost4.put("batchID", batchID);
                        toPost4.put("encryptedHash1", responseData.getString("encryptedHash3"));
                        link4 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                URLEncoder.encode(toPost4.toString()) + nxtPostLinkPart4;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    firstPost(link1);
                }
            }
        });
    }

    public void firstPost(String urlString) {
        try {
            URL url = new URL(urlString);  // convert string to proper url
            Log.d("url", url.toString());
            postRequest = new JsonObjectRequest(Request.Method.POST, url.toString(), (String) null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            try {
                                Log.d("Response", response.toString(4));
                                Log.d("response", response.toString());

                                secondPost(link2);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            if(!isNetworkAvailable()){
                                AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
                                builder.setMessage("Connect network to proceed")
                                        .setCancelable(false)
                                        .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {

                                            }
                                        });
                                ConnectionAlert = builder.create();
                                ConnectionAlert.setCanceledOnTouchOutside(false);
                                ConnectionAlert.show();
                            }else{
                                Toast.makeText(getApplicationContext(), "Server error occured, please try again", Toast.LENGTH_LONG).show();
                            }
                            Log.d("Error.PostResponse", error.toString());
                        }
                    }
            );

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        queue.add(postRequest);
    }

    public void secondPost(String urlString) {

        try {
            URL url = new URL(urlString);  // convert string to proper url
            Log.d("url", url.toString());
            postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            try {
                                Log.d("Response", response.toString(4));
                                Log.d("response", response.toString());

                                thirdPost(link3);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            if(!isNetworkAvailable()){
                                AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
                                builder.setMessage("Connect network to proceed")
                                        .setCancelable(false)
                                        .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {

                                            }
                                        });
                                ConnectionAlert = builder.create();
                                ConnectionAlert.setCanceledOnTouchOutside(false);
                                ConnectionAlert.show();
                            }else{
                                Toast.makeText(getApplicationContext(), "Server error occured, please try again", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            );

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        queue.add(postRequest);
    }

    public void thirdPost(String urlString) {

        try {
            final URL url = new URL(urlString);  // convert string to proper url
            Log.d("url", url.toString());
            postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            try {
                                Log.d("Response", response.toString(4));
                                Log.d("response", response.toString());
                                fourthPost(link4);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            if(!isNetworkAvailable()){
                                AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
                                builder.setMessage("Connect network to proceed")
                                        .setCancelable(false)
                                        .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {

                                            }
                                        });
                                ConnectionAlert = builder.create();
                                ConnectionAlert.setCanceledOnTouchOutside(false);
                                ConnectionAlert.show();
                            }else{
                                Toast.makeText(getApplicationContext(), "Server error occured, please try again", Toast.LENGTH_LONG).show();
                            }
                            Log.d("Error.PostResponse", error.toString());
                        }
                    }
            );

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        queue.add(postRequest);
    }

    public void fourthPost(String urlString) {

        try {
            URL url = new URL(urlString);  // convert string to proper url
            Log.d("url", url.toString());
            postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            try {
                                Log.d("Response", response.toString(4));
                                Log.d("response", response.toString());

                                AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
                                builder.setMessage("Succcessfully sent to blockchain")
                                        .setCancelable(false)
                                        .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                Intent intent = new Intent(Transaction.this, MainActivity.class);
                                                startActivity(intent);
                                            }
                                        });
                                ConnectionAlert = builder.create();
                                ConnectionAlert.setCanceledOnTouchOutside(false);
                                ConnectionAlert.show();

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            if(!isNetworkAvailable()){
                                AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
                                builder.setMessage("Connect WIFI to proceed")
                                        .setCancelable(false)
                                        .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {

                                            }
                                        });
                                ConnectionAlert = builder.create();
                                ConnectionAlert.setCanceledOnTouchOutside(false);
                                ConnectionAlert.show();
                            }else{
                                Toast.makeText(getApplicationContext(), "Server error occured, please try again", Toast.LENGTH_LONG).show();
                            }
                            Log.d("Error.PostResponse", error.toString());
                        }
                    }
            );

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        queue.add(postRequest);
    }

    private boolean isNetworkAvailable() {
        connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }
}
