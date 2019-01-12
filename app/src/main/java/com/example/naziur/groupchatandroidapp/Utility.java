package com.example.naziur.groupchatandroidapp;

import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.entity.StringEntity;

public class Utility {

    static String MEDIA_TYPE_TEXT = "TEXT";
    public static String MEDIA_TYPE_PIC = "PIC";
    private static String SERVER_KEY = "ENTER SERVER KEY";
    public static final String NOTIFICATION_URL = "https://fcm.googleapis.com/fcm/send";

    private Utility () {}

    static AsyncHttpClient createAsyncClient() {

        AsyncHttpClient client = new AsyncHttpClient();

        client.addHeader(HttpHeaders.AUTHORIZATION, "key="+SERVER_KEY);
        client.addHeader(HttpHeaders.CONTENT_TYPE, RequestParams.APPLICATION_JSON);
        return client;
    }

    static FirebaseMessageModel makeMessageNode(String type, String msg){
        FirebaseMessageModel firebaseMessageModel = new FirebaseMessageModel();
        firebaseMessageModel.setBody(msg);
        firebaseMessageModel.setMediaType(type);
        firebaseMessageModel.setSenderEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        return firebaseMessageModel;
    }

    static StringEntity getGroupMessageEntity(JSONArray registrationIds, String msg, String groupKey){
        JSONObject params = new JSONObject();
        StringEntity entity = null;

        try{
            params.put("registration_ids", registrationIds);
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("click_action", ".MainActivity");
            notificationObject.put("body", msg);
            notificationObject.put("title", groupKey);
            params.put("notification", notificationObject);
            entity = new StringEntity(params.toString());
        } catch(Exception e){
            e.printStackTrace();
        }

        return entity;
    }
}
