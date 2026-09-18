package com.networkincident.incident;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class IncidentTest {
    @Test
    void resolvesAnOpenIncidentAndRejectsMissingFields() {
        Incident incident = new Incident("STH-001", "RTR-1001", IncidentSeverity.CRITICAL, "Link unavailable");

        incident.resolve(Instant.parse("2026-09-18T12:00:00Z"));

        assertEquals(IncidentStatus.RESOLVED, incident.status());
        assertThrows(IllegalArgumentException.class,
                () -> new Incident("", "RTR-1001", IncidentSeverity.CRITICAL, "Link unavailable"));
    }
}
