package com.example.naziur.groupchatandroidapp;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;

import java.io.File;

import cz.msebera.android.httpclient.entity.StringEntity;

public class ImageViewDialogFragment  extends DialogFragment {

    private static final String TAG = "ImageViewDialogFragment";
    private static ImageViewDialogListener listener;

    public interface ImageViewDialogListener {
        void onActionPressed();
    }

    public static File imageFile;
    ProgressBar progressBar;

    public static ImageViewDialogFragment newInstance (File f) {
        imageFile = f;
        return new ImageViewDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.image_view, null);

        progressBar = (ProgressBar) v.findViewById(R.id.upload_progress);

        ImageView display = (ImageView) v.findViewById(R.id.image_viewer);

        Glide.with(getActivity()).load(imageFile)
                .apply(new RequestOptions().error(R.mipmap.ic_launcher))
                .into(display);

        AppCompatImageButton cancelBtn = (AppCompatImageButton) v.findViewById(R.id.cancel_image);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        final AppCompatImageButton sendBtn = (AppCompatImageButton) v.findViewById(R.id.send_image);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onActionPressed();
            }
        });

        builder.setView(v);

        return builder.create();
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (ImageViewDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString());
        }
    }

    public void sendImageAndMessage(final Context context, final JSONArray registrationIds, final String groupKey){
        Uri imgUri = Uri.fromFile(ImageViewDialogFragment.imageFile);
        String folder = "media/";
        StorageReference mStorageRef = FirebaseStorage.getInstance()
                .getReference().child(folder + groupKey + "/pictures/" + imgUri.getLastPathSegment());
       mStorageRef.putFile(imgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(0);
                    }
                }, 5000);

                taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        sendMessage(context, registrationIds, groupKey, uri.toString());
                    }
                });
                getDialog().dismiss();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Image Upload Failed", Toast.LENGTH_SHORT ).show();
                getDialog().dismiss();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progresss = (100.0* taskSnapshot.getBytesTransferred()/ taskSnapshot.getTotalByteCount());
                progressBar.setProgress((int)progresss);
            }
        });
    }


    private void sendMessage(final Context context, final JSONArray registrationIds, final String groupKey, String url){
        DatabaseReference newRef = FirebaseDatabase.getInstance().getReference("messages").child(groupKey).push();
        newRef.setValue(Utility.makeMessageNode(Utility.MEDIA_TYPE_PIC, url), new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                StringEntity entity = Utility.getGroupMessageEntity(registrationIds,  FirebaseAuth.getInstance().getCurrentUser().getDisplayName() + ": PIC", groupKey);
                Utility.createAsyncClient().post(context, Utility.NOTIFICATION_URL, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                        Log.e(TAG, responseString + ": " + throwable.getMessage());
                        Toast.makeText(context, "Failed to send notification", Toast.LENGTH_LONG).show();
                        dismiss();
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        dismiss();
                    }
                });

            }
        });
    }

}
