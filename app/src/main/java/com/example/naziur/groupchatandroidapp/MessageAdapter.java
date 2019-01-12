package com.example.naziur.groupchatandroidapp;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

public class MessageAdapter extends ArrayAdapter<FirebaseMessageModel> {
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseMessageModel[] messageModels;
    private Context context;

    public MessageAdapter(@NonNull Context context, @NonNull FirebaseMessageModel[] messages) {
        super(context, android.R.layout.simple_list_item_1, messages);
        messageModels = messages;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();
        FirebaseMessageModel message = messageModels[position];
        if (message.getMediaType().equals(Utility.MEDIA_TYPE_TEXT)) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            TextView msgText = convertView.findViewById(android.R.id.text1);
            if (message.getSenderEmail().equals(mAuth.getCurrentUser().getEmail())) {
                msgText.setText(message.getBody());
                msgText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            } else {
                msgText.setText(message.getSenderEmail() + ": " + message.getBody());
                msgText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }
        } else if (message.getMediaType().equals(Utility.MEDIA_TYPE_PIC)){
            convertView = inflater.inflate(R.layout.simple_list_image_item_1, parent, false);
            LinearLayout container = convertView.findViewById(R.id.msg_container);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            TextView msgText = convertView.findViewById(R.id.img_sender);
            ImageView msgImg = convertView.findViewById(R.id.img);
            Glide.with(context).load(message.getBody()).into(msgImg);
            if (message.getSenderEmail().equals(mAuth.getCurrentUser().getEmail())) {
                msgText.setVisibility(View.GONE);
                params.gravity = Gravity.END;
                container.setLayoutParams(params);
                msgText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            } else {
                params.gravity = Gravity.START;
                container.setLayoutParams(params);
                msgText.setText(message.getSenderEmail());
                msgText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }
        }
        return convertView;
    }
}
