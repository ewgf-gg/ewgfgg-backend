package org.ewgf.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    public DateTimeUtils() {}

    public static String toReadableTime(long unixTimestamp) {
        return Instant.ofEpochSecond(unixTimestamp)
                .atZone(UTC)
                .format(FORMATTER);
    }
}
