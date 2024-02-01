package com.example.lvarolpezfueyo_tfm.backend;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class CheckEmail {

    public interface EmailVerificationListener {
        void onEmailVerified(boolean isValid);
    }

    public void verifyEmailAddress(Context context, String email, EmailVerificationListener listener) {
        String url = "https://emailverification.whoisxmlapi.com/api/v3?apiKey=at_RQj3Y4NHnNNPxyMwEtqfPXkd0gt1m&emailAddress=" + email;
        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, response -> {
                    try {
                        boolean isValid = response.getBoolean("smtpCheck");
                        listener.onEmailVerified(isValid);
                    } catch (JSONException e) {
                        listener.onEmailVerified(false);
                        e.printStackTrace();
                    }

                }, error -> {
                    listener.onEmailVerified(false);
                });

        queue.add(jsonObjectRequest);
    }

}
