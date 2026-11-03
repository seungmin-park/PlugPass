package com.plugpass.station.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;

/** Provider change time is compared as a local value, never claimed as an observation instant. */
public record ChargerStatusChange(ChargerStatus status, String rawStatus, String changedAtRaw,
        String lastStartedAtRaw, String lastEndedAtRaw, String startedAtRaw, Instant collectedAt) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);
    /** The provider time zone is unconfirmed; one day past Korean collection time covers UTC/KST readings. */
    private static final ZoneId PROVIDER_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration FUTURE_TOLERANCE = Duration.ofDays(1);
    public ChargerStatusChange {
        if (status == null || collectedAt == null) { throw new IllegalArgumentException("status and collection time are required"); }
        if (!validTime(changedAtRaw)) { throw new IllegalArgumentException("status change time must be a valid yyyyMMddHHmmss value"); }
    }
    public boolean precedes(String existing) {
        return trustedOrdering(existing, collectedAt) && changedAtRaw.compareTo(existing) < 0;
    }
    public static boolean preservesExisting(String incoming, String existing, Instant collectedAt) {
        return trustedOrdering(existing, collectedAt) && (!validTime(incoming) || incoming.compareTo(existing) < 0);
    }
    /** A stored change time far beyond collection time cannot keep winning every later comparison. */
    private static boolean trustedOrdering(String existing, Instant collectedAt) {
        if (!validTime(existing)) { return false; }
        LocalDateTime latestPlausible = LocalDateTime.ofInstant(collectedAt.plus(FUTURE_TOLERANCE), PROVIDER_ZONE);
        return !LocalDateTime.parse(existing, FORMAT).isAfter(latestPlausible);
    }
    private static boolean validTime(String value) {
        if (value == null || !value.matches("[0-9]{14}")) { return false; }
        try { LocalDateTime.parse(value, FORMAT); return true; }
        catch (DateTimeException invalid) { return false; }
    }
}
