package com.google.amara.chattab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.bumptech.glide.signature.ObjectKey;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

//import com.example.aymen.androidchat.sql.MessagesContract;


//public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MyViewHolder> {

public class ChatMessageAdapter
        extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ME          = 1;
    private static final int VIEW_TYPE_OTHER       = 2;
    private static final int VIEW_TYPE_TYPING      = 3;
    private static final int VIEW_TYPE_DATE_HEADER = 4 ;

    private boolean showTyping = false;

    private final String myUserId; // = SocketManager.getUserId();

    //Constructor
    public ChatMessageAdapter(String myUserId) {
        super(DIFF_CALLBACK);

        Log.d("ADAPTER", "ChatMessageAdapter constructor");

        this.myUserId = myUserId;
    }

    @Override
    public int getItemCount() {
        int count = super.getItemCount();
        return showTyping ? count + 1 : count;
    }

    public void showTypingIndicator(boolean show) {
        if (this.showTyping == show) return;

        this.showTyping = show;

        if (show) {
            notifyItemInserted(getItemCount());
        } else {
            notifyItemRemoved(getItemCount());
        }
    }

    @Override
    public int getItemViewType(int position) {

        // ⭐ LAST ITEM = typing
        if (showTyping && position == getItemCount() - 1) {
            return VIEW_TYPE_TYPING;
        }

        ChatMessage msg = getItem(position);

        if (msg.getItemType() == ChatMessage.TYPE_DATE_HEADER) {
            return VIEW_TYPE_DATE_HEADER;
        }
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

        if (viewType == VIEW_TYPE_DATE_HEADER) {
            View view = inflater.inflate(
                    R.layout.item_date_header, parent, false);
            return new DateViewHolder(view);
        }

        if (viewType == VIEW_TYPE_TYPING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_typing, parent, false);
            return new TypingViewHolder(view);
        }

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
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position) {

        // ✅ HANDLE TYPING FIRST
        if (getItemViewType(position) == VIEW_TYPE_TYPING) {
            return; // nothing to bind
        }

        ChatMessage msg = getItem(position);

        if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).bind(msg);
            return;
        }
        Log.d("ADAPTER", "position=" + position + " typing=" + msg.isTyping());

        if (holder instanceof TypingViewHolder) {
            //TypingViewHolder vh = (TypingViewHolder) holder;
            //animateDots(vh);
            return;
        }

        if (holder instanceof MeViewHolder) {
            ((MeViewHolder) holder).bind(msg);
        } else if (holder instanceof OtherViewHolder) {
            ((OtherViewHolder) holder).bind(msg);
        }
    }

    private void animateDots(TypingViewHolder vh) {

        Handler handler = new Handler(Looper.getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                vh.dot1.animate().alpha(1f).setDuration(200).withEndAction(() ->
                        vh.dot1.animate().alpha(0.3f).setDuration(200)).start();

                vh.dot2.animate().alpha(1f).setStartDelay(150).setDuration(200).withEndAction(() ->
                        vh.dot2.animate().alpha(0.3f).setDuration(200)).start();

                vh.dot3.animate().alpha(1f).setStartDelay(300).setDuration(200).withEndAction(() ->
                        vh.dot3.animate().alpha(0.3f).setDuration(200)).start();

                handler.postDelayed(this, 900);
            }
        };

        handler.post(runnable);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position,
            @NonNull List<Object> payloads) {

        if (payloads.isEmpty()) {
            // No partial update → do full bind
            onBindViewHolder(holder, position);
            return;
        }

        ChatMessage msg = getItem(position);
        Bundle diff     = (Bundle) payloads.get(0);

        if (!payloads.isEmpty()) {

            if (diff.containsKey("status")) {
                msg = getItem(position);

                if (holder instanceof DateViewHolder) {
                    ((DateViewHolder) holder).bind(msg);
                    return;
                }

                if (holder instanceof MeViewHolder) {
                    ((MeViewHolder) holder).updateStatusOnly(msg);
                }

                return; // 🔥 STOP full rebind
            }

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

                    Log.d("DIFF_ITEMS",
                            "A: type=" + a.getItemType() + ", id=" + a.getLocalId() +
                                    " | B: type=" + b.getItemType() + ", id=" + b.getLocalId()+
                                    " " + (a.getItemType() == b.getItemType()) + " " +
                                    Objects.equals(a.getLocalId(), b.getLocalId())
                    );

                    return a.getItemType() == b.getItemType()
                            && Objects.equals(a.getLocalId(), b.getLocalId());

                }

                /*
                @Override
                public boolean areContentsTheSame(@NonNull ChatMessage oldItem,
                                                  @NonNull ChatMessage newItem) {
                    return oldItem.equals(newItem);
                }
                */

                @Override
                public boolean areContentsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
                    return Objects.equals(a.getSent_at(), b.getSent_at()) &&
                            Objects.equals(a.getRemoteUrl(), b.getRemoteUrl()) &&
                            Objects.equals(a.getLocalImageUri(), b.getLocalImageUri()) &&
                            Objects.equals(a.getMessage(), b.getMessage()) &&
                            Objects.equals(a.getStatus(), b.getStatus()) &&
                            a.isPending() == b.isPending();
                }

                @Override
                public Object getChangePayload(@NonNull ChatMessage oldItem,
                                               @NonNull ChatMessage newItem) {

                    Bundle diff = new Bundle();

                    if (!Objects.equals(oldItem.getStatus(), newItem.getStatus())) {
                        diff.putString("status", newItem.getStatus());
                    }

                    if (!Objects.equals(oldItem.getSent_at(), newItem.getSent_at())) {
                        diff.putString("time", newItem.getSent_at());
                    }

                    return diff.size() == 0 ? null : diff;
                }


                private boolean safeEquals(String a, String b) {
                    if (a == null && b == null) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };
    ////////////////////////////////////////////////////////////////////////////////////////////////
    class TypingViewHolder extends RecyclerView.ViewHolder {

        View dot1, dot2, dot3;

        public TypingViewHolder(@NonNull View itemView) {
            super(itemView);

            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static class MeViewHolder extends RecyclerView.ViewHolder {
        TextView    message, time, statusText;
        ImageView   imagePhoto, statusIcon;
        //ProgressBar progressBar;


        MeViewHolder(View itemView) {
            super(itemView);
            message     = itemView.findViewById(R.id.tv_message);
            time        = itemView.findViewById(R.id.tv_time);
            imagePhoto  = itemView.findViewById(R.id.image_photo);
            statusIcon  = itemView.findViewById(R.id.iv_status);    //online/offline
            //statusText  = itemView.findViewById(R.id.status_text);  //pending, accept, reject friendship


            //holder.statusText = itemView.findViewById(R.id.status_text);

            //progressBar = itemView.findViewById(R.id.image_upload_progress);
        }

        void bind(ChatMessage msg) {
            /*
            switch (msg.getStatus()) {
                case ChatMessage.STATUS_SENDING:
                    time.setText("⏳");
                    break;

                case ChatMessage.STATUS_SENT:
                    time.setText("✓");
                    break;

                case ChatMessage.STATUS_DELIVERED:
                    time.setText("✓✓");
                    break;

                case ChatMessage.STATUS_SEEN:
                    time.setText("✓✓");
                    time.setTextColor(Color.BLUE);
                    break;
            }
            */

            switch (msg.getStatus()) {
                case "sending": //"pending":
                    statusIcon.setImageResource(R.drawable.hourglass_icon);
                    break;

                case "sent":
                    statusIcon.setImageResource(R.drawable.check_mark_icon_black);
                    break;

                case "delivered":
                    statusIcon.setImageResource(R.drawable.check_mark_icon_double_black);
                    break;

                case "seen":
                    statusIcon.setImageResource(R.drawable.check_mark_icon_blue);
                    break;

                case "failed":
                    statusIcon.setImageResource(R.drawable.error_icon);
                    break;
            }

            // ----- MESSAGE TEXT -----
            if (msg.getMessage() != null && !msg.getMessage().trim().isEmpty()) {
                message.setVisibility(View.VISIBLE);
                message.setText(msg.getMessage());
            } else {
                message.setText("");
                message.setVisibility(View.GONE);
            }

            String myUserId = SocketManager.getUserId();
            boolean isMine  = msg.getId_from().equals(myUserId);

           // ----- IMAGE -----
            /*
            String imageToLoad;

            if (msg.getLocalImageUri() != null && !msg.getLocalImageUri().isEmpty()) {
                imageToLoad = msg.getLocalImageUri();
            } else if (msg.getRemoteUrl() != null && !msg.getRemoteUrl().isEmpty()) {
                imageToLoad = msg.getRemoteUrl();
            } else {
                imageToLoad = null;
            }
            */

            String imageToLoad = msg.getDisplayImageSource(myUserId);

            if (imageToLoad != null) {
                Uri uri = Uri.parse(imageToLoad);
                imagePhoto.setVisibility(View.VISIBLE);

                Glide.with(imagePhoto.getContext())
                        .load(uri)
                        //.signature(new ObjectKey(msg.getLocalImageUri()))
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
                //imagePhoto.setImageDrawable(null);
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
            time.setVisibility(View.VISIBLE);

            // format time
            String time_ = formatMessageTime(msg.getSent_at()); //msg.getSent_at(); //);
            time.setText(time_);

            // ----- PENDING INDICATOR -----
            showPending(msg.isPending());

            // 🔥 FORCE LAYOUT RE-CALCULATION
            //itemView.requestLayout();
        }

        public static String formatMessageTime(String isoTime) {

            if (isoTime == null || isoTime.isEmpty()) return "";

            Instant instant;

            try {
                // Handle invalid "now" or bad formats
                instant = Instant.parse(isoTime);
            } catch (Exception e) {
                Log.e("TIME", "Invalid time format: " + isoTime);

                // fallback → current time
                instant = Instant.now();
            }

            ZonedDateTime messageTime =
                    instant.atZone(ZoneId.systemDefault());

            ZonedDateTime now =
                    ZonedDateTime.now();

            LocalDate messageDate = messageTime.toLocalDate();
            LocalDate today = now.toLocalDate();

            if (messageDate.equals(today)) {
                return messageTime.format(
                        DateTimeFormatter.ofPattern("HH:mm"));
            }

            if (messageDate.equals(today.minusDays(1))) {
                return "Yesterday " +
                        messageTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            }

            if (messageDate.isAfter(today.minusDays(7))) {
                return messageTime.format(
                        DateTimeFormatter.ofPattern("EEEE HH:mm", Locale.getDefault()));
            }

            return messageTime.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
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

        void updateStatusOnly(ChatMessage msg) {

            switch (msg.getStatus()) {
                case "sending": //"pending":
                    statusIcon.setImageResource(R.drawable.hourglass_icon);
                    break;

                case "sent":
                    statusIcon.setImageResource(R.drawable.check_mark_icon_black);
                    break;

                case "delivered":
                    statusIcon.setImageResource(R.drawable.check_mark_icon_double_black);
                    break;

                case "seen":
                    statusIcon.setImageResource(R.drawable.check_mark_icon_blue);
                    break;

                case "failed":
                    statusIcon.setImageResource(R.drawable.error_icon);
                    break;
            }
        }

    }
////////////////////////////////////////////////////////////////////////////////////////////////////
    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView  message, time;
        ImageView imagePhoto, statusIcon;

        OtherViewHolder(View itemView) {
            super(itemView);
            message    = itemView.findViewById(R.id.tv_message);
            time       = itemView.findViewById(R.id.tv_time);
            imagePhoto = itemView.findViewById(R.id.image_photo);
            //statusIcon = itemView.findViewById(R.id.iv_status);
        }

        void bind(ChatMessage msg) {
        /*
        switch (msg.getStatus()) {
            case "sending": //"pending":
                statusIcon.setImageResource(R.drawable.hourglass_icon);
                break;

            case "sent":
                statusIcon.setImageResource(R.drawable.check_mark_icon_black);
                break;

            case "delivered":
                statusIcon.setImageResource(R.drawable.check_mark_icon_double_black);
                break;

            case "seen":
                statusIcon.setImageResource(R.drawable.check_mark_icon_blue);
                break;

            case "failed":
                statusIcon.setImageResource(R.drawable.error_icon);
                break;
        }
        */

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
        time.setVisibility(View.VISIBLE);
        // format time
        String time_ = formatMessageTime(msg.getSent_at()); //msg.getSent_at(); //
        time.setText(time_);
    }


    public static String formatMessageTime(String isoTime) {

        if (isoTime == null || isoTime.isEmpty()) return "";

        // Parse ISO UTC time
        Instant instant = Instant.parse(isoTime);

        // Convert to user's local timezone
        ZonedDateTime messageTime =
                instant.atZone(ZoneId.systemDefault());

        ZonedDateTime now =
                ZonedDateTime.now();

        LocalDate messageDate = messageTime.toLocalDate();
        LocalDate today = now.toLocalDate();

        // 1️⃣ Today → show HH:mm
        if (messageDate.equals(today)) {
            return messageTime.format(
                    DateTimeFormatter.ofPattern("HH:mm"));
        }

        // 2️⃣ Yesterday
        if (messageDate.equals(today.minusDays(1))) {
            return "Yesterday " +
                    messageTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        // 3️⃣ Same week → show day name
        if (messageDate.isAfter(today.minusDays(7))) {
            return messageTime.format(
                    DateTimeFormatter.ofPattern("EEEE HH:mm", Locale.getDefault()));
        }

        // 4️⃣ Older → show date
        return messageTime.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }


    void updateStatusOnly(ChatMessage msg) {

        switch (msg.getStatus()) {
            case "sending": //"pending":
                statusIcon.setImageResource(R.drawable.hourglass_icon);
                break;

            case "sent":
                statusIcon.setImageResource(R.drawable.check_mark_icon_black);
                break;

            case "delivered":
                statusIcon.setImageResource(R.drawable.check_mark_icon_double_black);
                break;

            case "seen":
                statusIcon.setImageResource(R.drawable.check_mark_icon_blue);
                break;

            case "failed":
                statusIcon.setImageResource(R.drawable.error_icon);
                break;
        }
    }
  }
  //////////////////////////////////////////////////////////////////////////////////////////////////
  class DateViewHolder extends RecyclerView.ViewHolder {
      TextView dateText;

      DateViewHolder(View itemView) {
          super(itemView);
          dateText = itemView.findViewById(R.id.dateText);
      }

      void bind(ChatMessage msg) {

          Log.d("BIND", "binding header: " + msg.getDisplayText());

          itemView.setVisibility(View.VISIBLE);
          itemView.setMinimumHeight(100);
          dateText.setHeight(100);
          itemView.post(() -> {
              Log.d("BIND", "POST height=" + itemView.getHeight());
          });
          dateText.setVisibility(View.VISIBLE);

          dateText.setText(msg.getDisplayText());

          Log.d("BIND", "itemView height=" + itemView.getHeight());
      }
  }
  //////////////////////////////////////////////////////////////////////////////////////////////////
}