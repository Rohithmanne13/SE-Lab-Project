package edu.iiitb.team007.ledger.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Timestamps are RFC 3339 UTC strings with milliseconds ending in Z, e.g. 2026-11-23T10:15:30.123Z
 * (DR-5, SRS 3.3.2). The time comes from the injected {@link Clock} provider (CON-5).
 */
public final class Timestamps {

    private static final DateTimeFormatter RFC_3339_MILLIS =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private Timestamps() {
    }

    public static String now(Clock clock) {
        return format(clock.instant());
    }

    public static String format(Instant instant) {
        return RFC_3339_MILLIS.format(instant);
    }
}
