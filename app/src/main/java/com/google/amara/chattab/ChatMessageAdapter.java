package com.google.amara.chattab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

//import com.example.aymen.androidchat.sql.MessagesContract;


//public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MyViewHolder> {

public class ChatMessageAdapter
        extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ME    = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    private final String myUserId; // = SocketManager.getUserId();

    public ChatMessageAdapter(String myUserId) {
        super(DIFF_CALLBACK);
        this.myUserId = myUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = getItem(position);
        return msg.getId_from().equals(myUserId)
                ? VIEW_TYPE_ME
                : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_ME) {
            View v = inflater.inflate(
                    R.layout.item_message_me, parent, false);
            return new MeViewHolder(v);
        } else {
            View v = inflater.inflate(
                    R.layout.item_message_other, parent, false);
            return new OtherViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Fallback full bind when there are no payloads
        ChatMessage msg = getItem(position);

        if (holder instanceof MeViewHolder) {
            ((MeViewHolder) holder).bind(msg);
        } else if (holder instanceof OtherViewHolder) {
            ((OtherViewHolder) holder).bind(msg);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position,  @NonNull List<Object> payloads) {

        if (payloads.isEmpty()) {
            // No partial update → do full bind
            onBindViewHolder(holder, position);
            return;
        }

        ChatMessage msg = getItem(position);
        Bundle diff     = (Bundle) payloads.get(0);

        if (!payloads.isEmpty()) {

            // 🔹 Only update time
            if (diff.containsKey("KEY_TIME")) {
                assert holder instanceof MeViewHolder;
                ((MeViewHolder) holder).time.setText(msg.getSent_at());
            }

            // 🔹 Only update pending indicator if you have one
            if (diff.containsKey("KEY_PENDING")) {
                assert holder instanceof MeViewHolder;
                ((MeViewHolder) holder).showPending(msg.isPending());
            }

            // 🔹 Only reload image if URL actually changed
            if (diff.containsKey("KEY_IMAGE")) {
                ((MeViewHolder) holder).bindImage(msg.getRemoteUrl());
            }
            return; // 🚀 Skip full bind
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatMessage>() {

                @Override
                public boolean areItemsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
                    return a.getLocalId().equals(b.getLocalId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
                    return Objects.equals(a.getSent_at(), b.getSent_at()) &&
                            Objects.equals(a.getRemoteUrl(), b.getRemoteUrl()) &&
                            Objects.equals(a.getLocalImageUri(), b.getLocalImageUri()) &&
                            Objects.equals(a.getMessage(), b.getMessage()) &&
                            Objects.equals(a.getSeen(), b.getSeen()) &&
                            a.isPending() == b.isPending();
                }


                private boolean safeEquals(String a, String b) {
                    if (a == null && b == null) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };


    /////////////////////////////////////////////////////////
    static class MeViewHolder extends RecyclerView.ViewHolder {
        TextView    message, time;
        ImageView   imagePhoto;
        ProgressBar progressBar;


        MeViewHolder(View itemView) {
            super(itemView);
            message     = itemView.findViewById(R.id.tv_message);
            time        = itemView.findViewById(R.id.tv_time);
            imagePhoto  = itemView.findViewById(R.id.image_photo);
            progressBar = itemView.findViewById(R.id.image_upload_progress);
        }

        void bind(ChatMessage msg) {

            // ----- MESSAGE TEXT -----
            if (msg.getMessage() != null && !msg.getMessage().trim().isEmpty()) {
                message.setVisibility(View.VISIBLE);
                message.setText(msg.getMessage());
            } else {
                message.setText("");
                message.setVisibility(View.GONE);
            }

            String myUserId = SocketManager.getUserId();
            boolean isMine = msg.getId_from().equals(myUserId);

// ----- IMAGE -----
            String imageToLoad;

            if (msg.getLocalImageUri() != null && !msg.getLocalImageUri().isEmpty()) {
                imageToLoad = msg.getLocalImageUri();
            } else if (msg.getRemoteUrl() != null && !msg.getRemoteUrl().isEmpty()) {
                imageToLoad = msg.getRemoteUrl();
            } else {
                imageToLoad = null;
            }

            if (imageToLoad != null) {
                imagePhoto.setVisibility(View.VISIBLE);

                Glide.with(imagePhoto.getContext())
                        .load(imageToLoad)
                        .dontAnimate()
                        .into(imagePhoto);

                if (!isMine) {   // ✅ ONLY received messages can zoom
                    imagePhoto.setOnClickListener(v ->
                            ImageViewerActivity.start(v.getContext(), imageToLoad)
                    );
                } else {
                    imagePhoto.setOnClickListener(null); // ❌ disable zoom for sender
                }

            } else {
                imagePhoto.setImageDrawable(null);
                imagePhoto.setVisibility(View.GONE);
                imagePhoto.setOnClickListener(null);
            }


            /*
            if (msg.getRemoteUrl() != null && !msg.getRemoteUrl().isEmpty()) {
                imagePhoto.setVisibility(View.VISIBLE);
                Glide.with(imagePhoto.getContext())
                        .load(msg.getRemoteUrl())
                        .dontAnimate()                 // no fade = no flash
                        .placeholder(imagePhoto.getDrawable()) // 🔥 keep current image while reloading
                        .error(R.drawable.bg_message_me) // optional
                        .into(imagePhoto);

            } else {
                imagePhoto.setImageDrawable(null);     // 🔥 clears recycled image
                imagePhoto.setVisibility(View.GONE);
            }
            */

            // ----- TIME -----
            time.setVisibility(View.VISIBLE);          // 🔥 force visible every time
            time.setText(msg.getSent_at());

            // ----- PENDING INDICATOR -----
            showPending(msg.isPending());

            // 🔥 FORCE LAYOUT RE-CALCULATION
            //itemView.requestLayout();
        }

        private static String formatTime(long timestamp) {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        void bindImage(String url) {
            if (url == null || url.isEmpty()) {
                imagePhoto.setVisibility(View.GONE);
                return;
            }

            imagePhoto.setVisibility(View.VISIBLE);

            Glide.with(imagePhoto.getContext())
                    .load(url)
                    .dontAnimate()
                    .into(imagePhoto);
        }

        public void showPending(boolean pending) {
        }
    }
////////////////////////////////////////////////////////////////////////////////////////////////////
    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView  message, time;
        ImageView imagePhoto;

        OtherViewHolder(View itemView) {
            super(itemView);
            message    = itemView.findViewById(R.id.tv_message);
            time       = itemView.findViewById(R.id.tv_time);
            imagePhoto = itemView.findViewById(R.id.image_photo);
        }

    void bind(ChatMessage msg) {

        // ----- TEXT -----
        if (msg.getMessage() != null && !msg.getMessage().trim().isEmpty()) {
            message.setVisibility(View.VISIBLE);
            message.setText(msg.getMessage());
        } else {
            message.setText("");
            message.setVisibility(View.GONE);
        }

        // ----- IMAGE  -----
        String imageToLoad = null;

        if (msg.getLocalImageUri() != null && !msg.getLocalImageUri().isEmpty()) {
            imageToLoad = msg.getLocalImageUri();   // ⚡ instant, no network
        } else if (msg.getRemoteUrl() != null && !msg.getRemoteUrl().isEmpty()) {
            imageToLoad = msg.getRemoteUrl();       // 🌍 cloud fallback
        }

        if (imageToLoad != null) {
            imagePhoto.setVisibility(View.VISIBLE);

            Glide.with(imagePhoto.getContext())
                    .load(imageToLoad)
                    .dontAnimate()
                    .into(imagePhoto);

            imagePhoto.setOnClickListener(v -> {
                if (msg.getRemoteUrl() != null) {
                    ImageViewerActivity.start(v.getContext(), msg.getRemoteUrl());
                }
            });

        } else {
            imagePhoto.setVisibility(View.GONE);
        }

        // ----- TIME -----
        time.setText(msg.getSent_at());
    }


    private static String formatTime(long timestamp) {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

    }
}