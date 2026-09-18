package com.networkincident.incident;

import java.time.Instant;
import java.util.UUID;

public final class Incident {
    private final UUID id = UUID.randomUUID();
    private final String siteId;
    private final String assetId;
    private IncidentSeverity severity;
    private final String description;
    private final Instant createdAt = Instant.now();
    private IncidentStatus status = IncidentStatus.NEW;
    private String owner;
    private Instant acknowledgedAt;
    private Instant resolvedAt;
    private Instant closedAt;

    public Incident(String siteId, String assetId, IncidentSeverity severity, String description) {
        this.siteId = required(siteId, "Site ID");
        this.assetId = required(assetId, "Asset ID");
        this.severity = java.util.Objects.requireNonNull(severity, "Severity is required.");
        this.description = required(description, "Description");
    }

    public void acknowledge(Instant occurredAt) {
        if (status != IncidentStatus.NEW) {
            throw new IllegalStateException("Only a new incident can be acknowledged.");
        }
        status = IncidentStatus.ACKNOWLEDGED;
        acknowledgedAt = requiredTime(occurredAt, "Acknowledgement time");
    }

    public void investigate() {
        if (status != IncidentStatus.ACKNOWLEDGED) {
            throw new IllegalStateException("Only an acknowledged incident can enter investigation.");
        }
        status = IncidentStatus.INVESTIGATING;
    }

    public void changeSeverity(IncidentSeverity severity) {
        ensureNotClosed();
        this.severity = java.util.Objects.requireNonNull(severity, "Severity is required.");
    }

    public void assignOwner(String owner) {
        ensureNotClosed();
        this.owner = required(owner, "Owner");
    }

    public void resolve(Instant occurredAt) {
        ensureNotClosed();
        if (status != IncidentStatus.RESOLVED) {
            status = IncidentStatus.RESOLVED;
            resolvedAt = requiredTime(occurredAt, "Resolution time");
        }
    }

    public void close(Instant occurredAt) {
        if (status != IncidentStatus.RESOLVED) {
            throw new IllegalStateException("Only a resolved incident can be closed.");
        }
        status = IncidentStatus.CLOSED;
        closedAt = requiredTime(occurredAt, "Closure time");
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

    public String owner() {
        return owner;
    }

    public Instant acknowledgedAt() {
        return acknowledgedAt;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    private void ensureNotClosed() {
        if (status == IncidentStatus.CLOSED) {
            throw new IllegalStateException("A closed incident cannot be modified.");
        }
    }

    private static Instant requiredTime(Instant value, String field) {
        return java.util.Objects.requireNonNull(value, field + " is required.");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }
}
