package com.networkincident.incident;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final Map<UUID, Incident> incidents = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@RequestBody CreateIncidentRequest request) {
        Incident incident = new Incident(request.siteId(), request.assetId(), request.severity(), request.description());
        incidents.put(incident.id(), incident);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentResponse.from(incident));
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable UUID id) {
        Incident incident = incidents.get(id);
        if (incident == null) {
            throw new IncidentNotFoundException();
        }
        return IncidentResponse.from(incident);
    }

    @GetMapping
    public PageResponse list(
            @RequestParam Optional<IncidentStatus> status,
            @RequestParam Optional<IncidentSeverity> severity,
            @RequestParam Optional<String> siteId,
            @RequestParam Optional<String> assetId,
            @RequestParam Optional<Instant> createdFrom,
            @RequestParam Optional<Instant> createdTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Page must be at least 1 and page size must be between 1 and 100.");
        }
        if (createdFrom.isPresent() && createdTo.isPresent() && createdFrom.get().isAfter(createdTo.get())) {
            throw new IllegalArgumentException("createdFrom must not be after createdTo.");
        }

        List<IncidentResponse> matches = incidents.values().stream()
                .filter(incident -> status.map(value -> incident.status() == value).orElse(true))
                .filter(incident -> severity.map(value -> incident.severity() == value).orElse(true))
                .filter(incident -> siteId.map(value -> incident.siteId().equals(value.trim())).orElse(true))
                .filter(incident -> assetId.map(value -> incident.assetId().equals(value.trim())).orElse(true))
                .filter(incident -> createdFrom.map(value -> !incident.createdAt().isBefore(value)).orElse(true))
                .filter(incident -> createdTo.map(value -> !incident.createdAt().isAfter(value)).orElse(true))
                .sorted(Comparator.comparing(Incident::createdAt).thenComparing(Incident::id))
                .map(IncidentResponse::from)
                .toList();
        int from = Math.min((page - 1) * pageSize, matches.size());
        int to = Math.min(from + pageSize, matches.size());
        return new PageResponse(matches.size(), page, pageSize, matches.subList(from, to));
    }

    @PostMapping("/{id}/resolve")
    public IncidentResponse resolve(@PathVariable UUID id) {
        Incident incident = incident(id);
        incident.resolve(Instant.now());
        return IncidentResponse.from(incident);
    }

    @PostMapping("/{id}/acknowledge")
    public IncidentResponse acknowledge(@PathVariable UUID id) {
        Incident incident = incident(id);
        incident.acknowledge(Instant.now());
        return IncidentResponse.from(incident);
    }

    @PostMapping("/{id}/investigate")
    public IncidentResponse investigate(@PathVariable UUID id) {
        Incident incident = incident(id);
        incident.investigate();
        return IncidentResponse.from(incident);
    }

    @PostMapping("/{id}/close")
    public IncidentResponse close(@PathVariable UUID id) {
        Incident incident = incident(id);
        incident.close(Instant.now());
        return IncidentResponse.from(incident);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/severity")
    public IncidentResponse changeSeverity(@PathVariable UUID id, @RequestBody ChangeSeverityRequest request) {
        Incident incident = incident(id);
        incident.changeSeverity(request.severity());
        return IncidentResponse.from(incident);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/owner")
    public IncidentResponse assignOwner(@PathVariable UUID id, @RequestBody AssignOwnerRequest request) {
        Incident incident = incident(id);
        incident.assignOwner(request.owner());
        return IncidentResponse.from(incident);
    }

    @ExceptionHandler(IncidentNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Incident not found."));
    }

    @ExceptionHandler({IllegalArgumentException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> badRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ProblemDetail> conflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage()));
    }

    private Incident incident(UUID id) {
        Incident incident = incidents.get(id);
        if (incident == null) {
            throw new IncidentNotFoundException();
        }
        return incident;
    }

    record CreateIncidentRequest(String siteId, String assetId, IncidentSeverity severity, String description) {
    }

    record ChangeSeverityRequest(IncidentSeverity severity) {
    }

    record AssignOwnerRequest(String owner) {
    }

    record PageResponse(int totalElements, int page, int pageSize, List<IncidentResponse> items) {
    }

    record IncidentResponse(UUID id, String siteId, String assetId, IncidentSeverity severity, IncidentStatus status,
                            String description, String owner, Instant createdAt, Instant acknowledgedAt,
                            Instant resolvedAt, Instant closedAt) {
        static IncidentResponse from(Incident incident) {
            return new IncidentResponse(incident.id(), incident.siteId(), incident.assetId(), incident.severity(),
                    incident.status(), incident.description(), incident.owner(), incident.createdAt(),
                    incident.acknowledgedAt(), incident.resolvedAt(), incident.closedAt());
        }
    }

    private static final class IncidentNotFoundException extends RuntimeException {
    }
}
