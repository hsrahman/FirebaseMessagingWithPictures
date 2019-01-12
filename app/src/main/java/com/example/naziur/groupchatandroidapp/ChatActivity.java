package com.example.naziur.groupchatandroidapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.entity.StringEntity;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements ImageViewDialogFragment.ImageViewDialogListener{
    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private String groupKey;
    private ArrayList<FirebaseMessageModel> messages;
    private ValueEventListener messageListener;
    public static final String TAG = "ChatActivity";
    private EditText messageEt;
    private ImageButton sendBtn, mediaBtn;
    private JSONArray registrationIds;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private ImageViewDialogFragment imageViewDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Bundle extra = getIntent().getExtras();
        if (extra == null) {
            finish();
            return;
        }
        registrationIds = new JSONArray();
        messages = new ArrayList<>();
        groupKey = extra.getString("group_id");
        getSupportActionBar().setTitle(groupKey);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    messages.clear();
                    for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                        FirebaseMessageModel firebaseMessageModel = snapshot.getValue(FirebaseMessageModel.class);
                        messages.add(firebaseMessageModel);
                    }
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
                databaseError.toException().printStackTrace();
            }
        };

        messageEt = findViewById(R.id.message_et);

        mediaBtn = findViewById(R.id.media_btn);
        mediaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EasyImage.openChooserWithGallery(ChatActivity.this, "Picture", REQUEST_CODE_GALLERY_CAMERA);
            }
        });

        sendBtn = findViewById(R.id.send_btn);
        sendBtn.setEnabled(false);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!messageEt.getText().toString().isEmpty() && registrationIds != null){
                    sendBtn.setEnabled(false);
                    sendMessage();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        getGroupInfo();
        showAllMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() == null) {
            Intent home = new Intent(this, MainActivity.class);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
        }
    }

    private void showAllMessages(){
        Query reference = database.getReference("messages").child(groupKey);
        reference.addValueEventListener(messageListener);
    }

    private void updateUI(){
        ListView listView = findViewById(R.id.message_list);
        FirebaseMessageModel [] allMessages = new FirebaseMessageModel[messages.size()];
        listView.setAdapter(new MessageAdapter(this, messages.toArray(allMessages)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                Toast.makeText(ChatActivity.this, "Error choosing file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                // Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    Toast.makeText(ChatActivity.this, "Deleting captured image...", Toast.LENGTH_SHORT).show();
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(ChatActivity.this);
                    if (photoFile != null) photoFile.delete();
                }
            }

            @Override
            public void onImagesPicked(@NonNull List<File> imageFiles, EasyImage.ImageSource source, int type) {
                switch (type){
                    case REQUEST_CODE_GALLERY_CAMERA:
                        imageViewDialog = ImageViewDialogFragment.newInstance(imageFiles.get(0));
                        imageViewDialog.setCancelable(true);
                        imageViewDialog.show(getSupportFragmentManager(), "ImageViewDialogFragment");
                        break;
                }
            }

        });
    }

    private void sendMessage(){
        DatabaseReference newRef = database.getReference("messages").child(groupKey).push();
        newRef.setValue(Utility.makeMessageNode(Utility.MEDIA_TYPE_TEXT, messageEt.getText().toString()), new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                StringEntity entity = Utility.getGroupMessageEntity(registrationIds, mAuth.getCurrentUser().getDisplayName()+": "+messageEt.getText().toString(), groupKey);
                Utility.createAsyncClient().post(getApplicationContext(), Utility.NOTIFICATION_URL, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                        sendBtn.setEnabled(true);
                        Log.e(TAG, responseString + ": " + throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        sendBtn.setEnabled(true);
                        messageEt.setText("");
                    }
                });

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }



    private void getGroupInfo(){
        Query newRef = database.getReference("groups").orderByChild("groupName").equalTo(groupKey);
        newRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseGroupModel firebaseGroupModel =snapshot.getValue(FirebaseGroupModel.class);
                        if(firebaseGroupModel.getGroupName().equals(groupKey)){
                            getDeviceTokenForGroup(firebaseGroupModel);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }

    private void getDeviceTokenForGroup(final FirebaseGroupModel firebaseGroupModel){
        Query newRef = database.getReference("users").orderByChild("email");
        newRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        String[] members = firebaseGroupModel.getMembers().split(",");
                        for(String s : members){
                            if(s.equals(firebaseUserModel.getEmail()) && !s.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())){
                                registrationIds.put(firebaseUserModel.getDeviceToken());
                            }
                        }
                    }
                    sendBtn.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }


    @Override
    public void onActionPressed() {
        if (registrationIds != null)
            imageViewDialog.sendImageAndMessage(this, registrationIds , groupKey);
    }
}
