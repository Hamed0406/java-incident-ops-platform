package com.networkincident.incident;

import java.time.Instant;
import java.util.UUID;

public final class Incident {
    private final UUID id = UUID.randomUUID();
    private final String siteId;
    private final String assetId;
    private final IncidentSeverity severity;
    private final String description;
    private final Instant createdAt = Instant.now();
    private IncidentStatus status = IncidentStatus.NEW;
    private Instant resolvedAt;

    public Incident(String siteId, String assetId, IncidentSeverity severity, String description) {
        this.siteId = required(siteId, "Site ID");
        this.assetId = required(assetId, "Asset ID");
        this.severity = java.util.Objects.requireNonNull(severity, "Severity is required.");
        this.description = required(description, "Description");
    }

    public void resolve(Instant occurredAt) {
        if (status == IncidentStatus.CLOSED) {
            throw new IllegalStateException("A closed incident cannot be resolved.");
        }
        status = IncidentStatus.RESOLVED;
        resolvedAt = java.util.Objects.requireNonNull(occurredAt, "Resolution time is required.");
    }

    public UUID id() {
        return id;
    }

    public String siteId() {
        return siteId;
    }

    public String assetId() {
        return assetId;
    }

    public IncidentSeverity severity() {
        return severity;
    }

    public IncidentStatus status() {
        return status;
    }

    public String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }
}
