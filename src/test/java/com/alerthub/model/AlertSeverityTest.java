package com.alerthub.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlertSeverity 测试类
 */
class AlertSeverityTest {

    @Test
    void testFromString() {
        assertEquals(AlertSeverity.CRITICAL, AlertSeverity.fromString("CRITICAL"));
        assertEquals(AlertSeverity.HIGH, AlertSeverity.fromString("HIGH"));
        assertEquals(AlertSeverity.MEDIUM, AlertSeverity.fromString("MEDIUM"));
        assertEquals(AlertSeverity.LOW, AlertSeverity.fromString("LOW"));
        assertEquals(AlertSeverity.INFO, AlertSeverity.fromString("INFO"));
    }

    @Test
    void testFromStringCaseInsensitive() {
        assertEquals(AlertSeverity.HIGH, AlertSeverity.fromString("high"));
        assertEquals(AlertSeverity.MEDIUM, AlertSeverity.fromString("Medium"));
    }

    @Test
    void testFromStringInvalid() {
        assertEquals(AlertSeverity.INFO, AlertSeverity.fromString("invalid"));
        assertEquals(AlertSeverity.INFO, AlertSeverity.fromString(""));
        assertEquals(AlertSeverity.INFO, AlertSeverity.fromString(null));
    }

    @Test
    void testIsAtLeast() {
        assertTrue(AlertSeverity.CRITICAL.isAtLeast(AlertSeverity.HIGH));
        assertTrue(AlertSeverity.HIGH.isAtLeast(AlertSeverity.HIGH));
        assertFalse(AlertSeverity.LOW.isAtLeast(AlertSeverity.HIGH));
        assertFalse(AlertSeverity.INFO.isAtLeast(AlertSeverity.LOW));
    }

    @Test
    void testGetLevel() {
        assertEquals(4, AlertSeverity.CRITICAL.getLevel());
        assertEquals(3, AlertSeverity.HIGH.getLevel());
        assertEquals(2, AlertSeverity.MEDIUM.getLevel());
        assertEquals(1, AlertSeverity.LOW.getLevel());
        assertEquals(0, AlertSeverity.INFO.getLevel());
    }
}
