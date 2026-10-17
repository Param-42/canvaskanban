package com.canvaskanban.dao;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Small helpers for storing dates in SQLite. Dates are kept as ISO text
 * ("2026-10-17") because that is readable and sorts correctly.
 */
final class SqlDates {

    private SqlDates() {
        // Helpers only.
    }

    static String toText(LocalDate date) {
        return date == null ? null : date.toString();
    }

    static LocalDate fromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            // A damaged row should not stop the whole board from loading.
            return null;
        }
    }
}
