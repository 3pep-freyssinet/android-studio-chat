package com.google.amara.chattab.upload;

import org.json.JSONException;

public interface UploadFileMoreDataReqListener {

    void uploadChunck(int place, int percent);

    void err(JSONException e);

}
