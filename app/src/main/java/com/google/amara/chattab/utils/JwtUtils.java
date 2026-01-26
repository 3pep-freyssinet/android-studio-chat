package com.google.amara.chattab.utils;

import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class JwtUtils {

    private JwtUtils() {}

    public static String getUserId(String token) {
        try {
            if (token == null) return null;

            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payloadJson = new String(
                    Base64.decode(
                            parts[1],
                            Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING
                    ),
                    StandardCharsets.UTF_8
            );

            JSONObject payload = new JSONObject(payloadJson);

            // backend uses "userId"
            return payload.optString("userId", null);

        } catch (Exception e) {
            return null;
        }
    }

    public static String getUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            String payloadJson = new String(
                    Base64.decode(parts[1], Base64.URL_SAFE),
                    StandardCharsets.UTF_8
            );
            JSONObject payload = new JSONObject(payloadJson);
            return payload.optString("username", null);
        } catch (Exception e) {
            return null;
        }
    }
}
