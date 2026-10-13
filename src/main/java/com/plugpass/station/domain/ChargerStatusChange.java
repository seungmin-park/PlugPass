package com.plugpass.station.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.DateTimeException;

/** Provider change time is compared as a local value, never claimed as an observation instant. */
public record ChargerStatusChange(ChargerStatus status, String rawStatus, String changedAtRaw,
        String lastStartedAtRaw, String lastEndedAtRaw, String startedAtRaw, Instant collectedAt) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);
    public ChargerStatusChange {
        if (status == null || collectedAt == null) { throw new IllegalArgumentException("status and collection time are required"); }
        if (!validTime(changedAtRaw)) { throw new IllegalArgumentException("status change time must be a valid yyyyMMddHHmmss value"); }
    }
    public boolean precedes(String existing) {
        return validTime(existing) && changedAtRaw.compareTo(existing) < 0;
    }
    public static boolean preservesExisting(String incoming, String existing) {
        return validTime(existing) && (!validTime(incoming) || incoming.compareTo(existing) < 0);
    }
    private static boolean validTime(String value) {
        if (value == null || !value.matches("[0-9]{14}")) { return false; }
        try { LocalDateTime.parse(value, FORMAT); return true; }
        catch (DateTimeException invalid) { return false; }
    }
}
