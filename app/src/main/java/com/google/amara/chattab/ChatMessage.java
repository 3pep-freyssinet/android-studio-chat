package com.google.amara.chattab;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by
 */

@Entity(
        tableName = "messages",
        indices = {
                @Index(value = {"localId"},  unique = true),
                @Index(value = {"serverId"}, unique = true)
        }
)
public class ChatMessage {

    @PrimaryKey(autoGenerate = true)
    public int uid;   // local DB id

    @NonNull
    public String  localId;
    public Integer serverId;
    public String  id_from;
    public String  id_to;
    public String  message;
    public String  localImageUri;
    public String  remoteUrl;
    public String  sent_at;
    public String  status;
    public String  type;
    public boolean pending;

    @Ignore
    private String displayImageSource;

    public static final String STATUS_SENDING   = "sending";
    public static final String STATUS_SENT      = "sent";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_SEEN      = "seen";

    //public String status;

    // Needed empty constructor
    public ChatMessage() {}

    @Ignore
    public ChatMessage(Integer serverId, @NonNull String localId, String id_from, String id_to,
                       String message, String localImageUri, String remoteUrl,
                       String sent_at, String status, String type, boolean pending) {
        this.serverId       = serverId;
        this.localId        = localId;
        this.id_from        = id_from;
        this.id_to          = id_to;
        this.message        = message;
        this.localImageUri  = localImageUri;
        this.remoteUrl      = remoteUrl;
        this.sent_at        = sent_at;
        this.status         = status;
        this.type           = type;
        this.pending        = pending;
    }

    @NonNull
    public String getLocalId() { return localId; }
    public String getId_from() { return id_from; }
    public String getId_to() { return id_to; }
    public String getMessage() { return message; }
    public String getLocalImageUri() { return localImageUri; }
    public String getRemoteUrl() { return remoteUrl; }
    public String getSent_at() { return sent_at; }
    //public String getSeen() { return seen; }
    public String getType(){return type;}
    public boolean isPending() { return pending; }
    //public int    getUploadProgress() { return uploadProgress; }
    public String getStatus() { return status; }
    public Integer getServerId() {return serverId;}


    public void setLocalId(@NonNull String localId) { this.localId = localId; }
    public void setId_from(String id_from) {this.id_from = id_from;}
    public void setId_to(String id_tod) {this.id_to = id_tod; }
    public void setLocalImageUri(String localImageUri) {this.localImageUri = localImageUri; }
    public void setRemoteUrl(String remoteUrl) {this.remoteUrl = remoteUrl; }
    public void setSent_at(String sent_at) {this.sent_at = sent_at; }
    //public void setSeen(String seen) {this.seen = seen; }
    public void setType(String type) {this.type = type; }
    public void setMessage(String message) {this.message = message; }
    public void setPending(boolean pending) {this.pending = pending; }
    //public void setUploadProgress(int uploadProgress) { this.uploadProgress = uploadProgress; }
    public void setStatus(String status) { this.status = status; }
    public void setServerId(int id) {this.serverId = id;}

    public static ChatMessage fromJson(JSONObject o) {
        return new ChatMessage(
                o.optInt("id"),
                o.optString("localId", null),
                o.optString("id_from"),
                o.optString("id_to"),

                o.isNull("message") ? null : o.optString("message"),

                //o.optString("message", null),

                o.isNull("local_image_uri") ? null : o.optString("local_image_uri"),

                o.isNull("image_url") ? null : o.optString("image_url"), //remote_image_url = image_url

                //o.optString("image_url", null),
                o.optString("sent_at"),
                o.optString("seen"),
                o.optString("type", "text"),
                false   // server messages are never pending
        );
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("serverId", serverId);
            obj.put("localId", localId);      // ⭐ used to match optimistic message
            obj.put("fromUserId", id_from);
            obj.put("toUserId", id_to);
            obj.put("message", message);
            obj.put("type", type);    // "text", "image", or "text-image"

            // Image handling
            if (remoteUrl != null) {
                obj.put("image_url", remoteUrl);  // what backend expects
            }

            // Optional fields (backend may overwrite)
            if (sent_at != null) obj.put("sent_at", sent_at);
            if (status != null) obj.put("seen", status);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    public String getDisplayImageSource(String myUserId) {
        if (displayImageSource != null) return displayImageSource;

        boolean isMine = myUserId != null && myUserId.equals(id_from);

        if (isMine && localImageUri != null && !localImageUri.isEmpty()) {
            // 🔥 Sender always sees local image
            displayImageSource = localImageUri;
        } else if (remoteUrl != null && !remoteUrl.isEmpty()) {
            // Receiver or restored message
            displayImageSource = remoteUrl;
        } else if (localImageUri != null && !localImageUri.isEmpty()) {
            // Fallback (edge cases)
            displayImageSource = localImageUri;
        }

        return displayImageSource;
    }

}

////////////////////////////////////////////////////////////////////////////////////////////////////
/*
public class ChatMessage {
    private String localId;
    private String id_from;
    private String id_to;
    private String message;
    private String localImageUri;  // image on device (instant)
    private String remoteUrl;;
    private String sent_at;
    private String seen;
    private String type;
    private boolean pending;


    private int uploadProgress;

    public ChatMessage(String localId, String id_from, String id_to, String message, String localImageUri, String remoteUrl, String sent_at, String seen, String type, boolean pending) {
        this.localId = localId;
        this.id_from = id_from;
        this.id_to   = id_to;
        this.message = message;
        this.localImageUri= localImageUri;
        this.remoteUrl= remoteUrl;
        this.sent_at = sent_at;
        this.seen    = seen;
        this.type    = type;
        this.pending = pending;;
    }

    public ChatMessage() {}

    public boolean isPending() { return pending; }

    public String getLocalId() { return localId; }
    public String getId_from() { return id_from; }
    public String getId_to() { return id_to; }
    public String getMessage() { return message; }
    public String getLocalImageUri() { return localImageUri; }
    public String getRemoteUrl() { return remoteUrl; }
    public String getSent_at() { return sent_at; }
    public String getSeen() { return seen; }
    public String getType(){return type;}
    public int    getUploadProgress() { return uploadProgress; }


    public void setLocalId(String localId) { this.localId = localId; }
    public void setId_from(String id_from) {this.id_from = id_from;}
    public void setId_to(String id_tod) {this.id_to = id_tod; }
    public void setLocalImageUri(String localImageUri) {this.localImageUri = localImageUri; }
    public void setRemoteUrl(String remoteUrl) {this.remoteUrl = remoteUrl; }
    public void setSent_at(String sent_at) {this.sent_at = sent_at; }
    public void setSeen(String seen) {this.seen = seen; }
    public void setType(String type) {this.type = type; }
    public void setMessage(String message) {this.message = message; }
    public void setPending(boolean pending) {this.pending = pending; }
    public void setUploadProgress(int uploadProgress) { this.uploadProgress = uploadProgress; }

    public static ChatMessage fromJson(JSONObject o) {
        return new ChatMessage(
                o.optString("localId", null),
                o.optString("id_from"),
                o.optString("id_to"),

                o.isNull("message") ? null : o.optString("message"),

                //o.optString("message", null),

                o.isNull("local_image_uri") ? null : o.optString("local_image_uri"),

                o.isNull("image_url") ? null : o.optString("image_url"), //remote_image_url = image_url

                //o.optString("image_url", null),
                o.optString("sent_at"),
                o.optString("seen"),
                o.optString("type", "text"),
                false   // server messages are never pending
        );
    }
}
*/