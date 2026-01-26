package com.google.amara.chattab;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class ProgressRequestBody extends RequestBody {

    private final RequestBody requestBody;
    private final ProgressListener listener;

    public interface ProgressListener {
        void onProgress(int percent);
    }

    public ProgressRequestBody(RequestBody body, ProgressListener listener) {
        this.requestBody = body;
        this.listener    = listener;
    }

    @Override public MediaType contentType() { return requestBody.contentType(); }
    @Override public long contentLength() throws IOException { return requestBody.contentLength(); }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long contentLength = contentLength();
        Source source = null; //Okio.source(requestBodyToInputStream(requestBody));
        long total = 0;
        long read;
        while ((read = source.read(sink.buffer(), 2048)) != -1) {
            total += read;
            sink.flush();
            int progress = (int) ((100 * total) / contentLength);
            listener.onProgress(progress);
        }
    }
}

