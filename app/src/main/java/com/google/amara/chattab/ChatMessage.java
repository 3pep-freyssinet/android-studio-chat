package com.google.amara.chattab;

import org.json.JSONObject;

/**
 * Created by
 */

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