package com.example.user.preconsumerapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import static com.example.user.preconsumerapp.MainActivity.PostConnectionAlert;

public class Transaction extends AppCompatActivity {
    //Connectivity checking
    ConnectivityManager connectivityManager;
    NetworkInfo activeNetworkInfo;

    //string for nxt link
    String link1, link2, link3, link4;

    //string for QR scanning result
    String batchID,productName,nxtAccNum,nxtTransactionAccNum;

    int errorCounter;
    int quantity = 0;

    AlertDialog SucessTransactionAlert;
    JSONObject toPost1 = null;
    JSONObject toPost2 = null;
    JSONObject toPost3 = null;
    JSONObject toPost4 = null;
    JSONObject responseData = null;
    JsonObjectRequest postRequest;
    RequestQueue queue;


    Button btnPost;
    TextView tvProduct,tvBatch;

    //nxt url parts
    String secretPhrase = null;

    private static final String nxtPostLinkPart1 = "http://174.140.168.136:6876/nxt?requestType=sendMessage&secretPhrase=";
    private static final String nxtPostLinkPart2 = "&recipient=";
    private static final String nxtPostLinkPart3 = "&message=";
    private static final String nxtPostLinkPart4 = "&deadline=60&feeNQT=0";

    //Local Server IP
    private static String getInfoUrl;
    Socket s;
    String serverResponse;
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.activity_transaction);

        // get local server IP from text file
        //getInfoUrl = readRawTextFile(Transaction.this,R.raw.serverip).replaceAll("\\s+","")+":7080/";
        getInfoUrl = "http://128.199.127.154:7080/";

        queue= Volley.newRequestQueue(this);
        pDialog = new ProgressDialog(this);
        tvProduct = (TextView)findViewById(R.id.productName);
        tvBatch = (TextView)findViewById(R.id.batchID);

        //initialize dialog 1 - connect to wifi for action
        AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
        builder.setMessage("No WIFI connection")
                .setCancelable(false)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        PostConnectionAlert = builder.create();
        PostConnectionAlert.setCanceledOnTouchOutside(false);

        //initialize dialog 2 - successful transaction
        AlertDialog.Builder builder2 = new AlertDialog.Builder(Transaction.this);
        builder2.setMessage(R.string.successful_transaction)
                .setCancelable(false)
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Transaction.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                });
        SucessTransactionAlert = builder2.create();
        SucessTransactionAlert.setCanceledOnTouchOutside(false);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        nxtAccNum = intent.getStringExtra("nxtAccNum");
        productName = intent.getStringExtra("productName");
        batchID = intent.getStringExtra("batchID");
        quantity = intent.getIntExtra("Quantity",0);
        nxtTransactionAccNum = intent.getStringExtra("nxtTransactionAccNum");

        tvProduct.setText(productName);
        tvBatch.setText("BatchID: "+batchID);

        btnPost = (Button) findViewById(R.id.button);
        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // check if wifi is available or not
                if(!isNetworkAvailable()){
                    PostConnectionAlert.show();
                }else{
                    // get secret phrase from local server
                    getSecretPhrase(nxtTransactionAccNum);
                }
            }
        });
    }

    // get secret phrase from local server
    private void getSecretPhrase(String nxtAcc){
        // Showing progress dialog before making http request
        //System.setProperty("http.keepAlive", "false");
        pDialog.setMessage("Getting response from server...");
        pDialog.setCancelable(false);
        pDialog.show();

        String url = readRawTextFile(Transaction.this,R.raw.serverip).replaceAll("\\s+","")+"/generate2/getSecret.php?nxtAccountNumber="+nxtAcc;
        //String url = "http://192.168.0.104/generate/getSecret.php?nxtAccountNumber="+nxtAcc;

        Log.d ("aa",url);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url ,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try{
                            Log.d ("secretPhraseResponse","yes");
                            JSONObject res = new JSONObject(response);
                            if(res.has("secretPhrase")){
                                secretPhrase = res.getString("secretPhrase");
                                // remove all the space characters
                                secretPhrase = secretPhrase.replaceAll(" ","%20");
                                getLocalInfoFromServer();
                            }
                            else{
                                pDialog.dismiss();
                                Toast.makeText(Transaction.this, R.string.secret_error, Toast.LENGTH_LONG).show();
                            }

                        }catch(JSONException e){
                            e.printStackTrace();
                            pDialog.dismiss();
                            Toast.makeText(Transaction.this, R.string.secret_error, Toast.LENGTH_LONG).show();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pDialog.dismiss();
                Toast.makeText(Transaction.this, R.string.secret_error, Toast.LENGTH_LONG).show();
                Log.d("secret error", error.toString());
            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    //get response from local server
    public void getLocalInfoFromServer(){
        //System.setProperty("http.keepAlive", "false");
        Log.d ("Local java server",getInfoUrl);
        communicateServer request = new communicateServer();
        request.execute();
//        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, getInfoUrl, (String) null, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                try {
//                    //get json Object
//                    responseData = response;
//                    Log.d("Response: ", responseData.toString());
//
//                    //assign data to post objects
//                    toPost1 = new JSONObject();
//                    toPost2 = new JSONObject();
//                    toPost3 = new JSONObject();
//                    toPost4 = new JSONObject();
//
//                    try {
//                        //first post data
//                        toPost1.put("batchID", batchID);
//                        toPost1.put("Quantity",quantity);
//                        toPost1.put("unhashedData", responseData.getString("unhashedData"));
//                        link1 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
//                                URLEncoder.encode(toPost1.toString()) + nxtPostLinkPart4;
//
//                        //second post data
//                        toPost2.put("batchID", batchID);
//                        toPost2.put("encryptedHash1", responseData.getString("encryptedHash1"));
//                        link2 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
//                                URLEncoder.encode(toPost2.toString()) + nxtPostLinkPart4;
//
//                        //third post data
//                        toPost3.put("batchID", batchID);
//                        toPost3.put("encryptedHash2", responseData.getString("encryptedHash2"));
//                        link3 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
//                                URLEncoder.encode(toPost3.toString()) + nxtPostLinkPart4;
//
//                        //fourth post data
//                        toPost4.put("batchID", batchID);
//                        toPost4.put("encryptedHash3", responseData.getString("encryptedHash3"));
//                        link4 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
//                                URLEncoder.encode(toPost4.toString()) + nxtPostLinkPart4;
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        // error
//                        pDialog.dismiss();
//                        if(!isNetworkAvailable()){
//                            Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
//                        }else{
//                            Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
//                        }
//                    }
//
//                    // call the first post function
//                    //firstPost(link1);
//
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    // error
//                    pDialog.dismiss();
//                    if(!isNetworkAvailable()){
//                        Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
//                    }else{
//                        Toast.makeText(Transaction.this, R.string.server_error, Toast.LENGTH_LONG).show();
//                    }
//                }
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.d("VolleyError: ", error.toString());
//
//                if(errorCounter <=3){
//                    getLocalInfoFromServer();
//                    errorCounter ++;
//                }else{
//                    // error
//                    pDialog.dismiss();
//                    if(!isNetworkAvailable()){
//                        Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
//                    }else{
//                        Toast.makeText(Transaction.this, R.string.server_error, Toast.LENGTH_LONG).show();
//                    }
//                }
//            }
//        });
//        getRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//        queue.add(getRequest);






    }

    private class communicateServer extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {
            try {
                s = new Socket("128.199.127.154",7080);

                // testing only - trying to get the response back from the server
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                while(true) {
                    if ((serverResponse = in.readLine()) != null) {
                        Log.i("server says", serverResponse);
                        //assign data to post objects
                        try {
                            responseData = new JSONObject(serverResponse);
                            Log.i("server says", responseData.toString());
                            toPost1 = new JSONObject();
                            toPost2 = new JSONObject();
                            toPost3 = new JSONObject();
                            toPost4 = new JSONObject();


                            //first post data
                            toPost1.put("batchID", batchID);
                            toPost1.put("Quantity",quantity);
                            Log.i("server says", responseData.getString("unhashedData"));
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
                            toPost3.put("encryptedHash2", responseData.getString("encryptedHash2"));
                            link3 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                    URLEncoder.encode(toPost3.toString()) + nxtPostLinkPart4;

                            //fourth post data
                            toPost4.put("batchID", batchID);
                            toPost4.put("encryptedHash3", responseData.getString("encryptedHash3"));
                            link4 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                    URLEncoder.encode(toPost4.toString()) + nxtPostLinkPart4;

                        } catch (Exception e) {
                            e.printStackTrace();
                            // error
                            pDialog.dismiss();
                            if(!isNetworkAvailable()){
                                Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                            }
                        }
                        break;
                    }
                }
                s.close();

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "some message";
        }

        @Override
        protected void onPostExecute(String message) {
            // call the first post function
            firstPost(link1);
        }
    }

    //post first json object to blockchain (batchID, movement, unhashed data)
    public void firstPost(String urlString) {
        pDialog.setMessage("Sending to blockchain...");
        Log.d("Url", urlString);
        postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        try {
                            Log.d("Response", response.toString(4));
                            Log.d("response", response.toString());

                            if(response.has("transaction")){
                                // on response call the second post function
                                secondPost(link2);
                            }
                            else{
                                pDialog.dismiss();
                                Toast.makeText(Transaction.this, R.string.accerror, Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            // error
                            pDialog.dismiss();
                            if(!isNetworkAvailable()){
                                Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                            }
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        pDialog.dismiss();
                        if(!isNetworkAvailable()){
                            Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                        }
                        Log.d("Error.PostResponse", error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(postRequest);
    }

    //post second json object to blockchain (batchID, encryptedhashPart1)
    public void secondPost(String urlString) {

        postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        try {
                            Log.d("Response", response.toString(4));
                            Log.d("response", response.toString());
                            // on response call the third post function

                            if(response.has("transaction")){
                                // on response call the second post function
                                thirdPost(link3);
                            }
                            else{
                                pDialog.dismiss();
                                Toast.makeText(Transaction.this, R.string.accerror, Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            // error
                            pDialog.dismiss();
                            if(!isNetworkAvailable()){
                                Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        pDialog.dismiss();
                        if(!isNetworkAvailable()){
                            Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(postRequest);
    }

    //post third json object to blockchain (batchID, encryptedhashPart2)
    public void thirdPost(String urlString) {

        postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        try {
                            Log.d("Response", response.toString(4));
                            Log.d("response", response.toString());
                            // on response call the fourth post function

                            if(response.has("transaction")){
                                // on response call the second post function
                                fourthPost(link4);
                            }
                            else{
                                pDialog.dismiss();
                                Toast.makeText(Transaction.this, R.string.accerror, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // error
                            pDialog.dismiss();
                            if(!isNetworkAvailable()){
                                Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        pDialog.dismiss();
                        if(!isNetworkAvailable()){
                            Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                        }
                        Log.d("Error.PostResponse", error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(postRequest);
    }

    //post third json object to blockchain (batchID, encryptedhashPart3)
    public void fourthPost(String urlString) {

        postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        try {
                            pDialog.dismiss();
                            Log.d("Response", response.toString(4));
                            Log.d("response", response.toString());

                            if(response.has("transaction")){
                                // on response call the second post function
                                SucessTransactionAlert.show();
                            }
                            else{
                                pDialog.dismiss();
                                Toast.makeText(Transaction.this, R.string.accerror, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // error
                            pDialog.dismiss();
                            if(!isNetworkAvailable()){
                                Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        pDialog.dismiss();
                        if(!isNetworkAvailable()){
                            Toast.makeText(Transaction.this, R.string.lost_connection, Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                        }
                        Log.d("Error.PostResponse", error.toString());
                    }
                }
        );
        postRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(postRequest);
    }

    // Checks the network connection
    private boolean isNetworkAvailable() {
        connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    // read string from text file
    public static String readRawTextFile(Context ctx, int resId)
    {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }
}
