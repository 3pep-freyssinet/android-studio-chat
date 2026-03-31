package com.google.amara.chattab.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public class TimeUtils {

    public static String formatSmartTime(String backendTime) {
        OffsetDateTime odt = OffsetDateTime.parse(backendTime);
        long timeMillis = odt.toInstant().toEpochMilli();

        long nowMillis = System.currentTimeMillis();
        long diff = nowMillis - timeMillis;

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes <= 0) return "just now";

        if (minutes < 60) {
            if (minutes <= 0) return "just now";
            return minutes + "m ago";
        }

        if (hours < 24) {
            return hours + "h ago";
        }

        // >= 1 day → show date
        Instant instant = odt.toInstant();
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("MMM d, HH:mm")
                .withZone(ZoneId.systemDefault());

        return formatter.format(instant);
    }
}
