package com.networkincident.incident;

import java.time.Instant;
import java.util.Map;
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

    @PostMapping("/{id}/resolve")
    public IncidentResponse resolve(@PathVariable UUID id) {
        Incident incident = incidents.get(id);
        if (incident == null) {
            throw new IncidentNotFoundException();
        }
        incident.resolve(Instant.now());
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

    record CreateIncidentRequest(String siteId, String assetId, IncidentSeverity severity, String description) {
    }

    record IncidentResponse(UUID id, String siteId, String assetId, IncidentSeverity severity, IncidentStatus status,
                            String description, Instant createdAt, Instant resolvedAt) {
        static IncidentResponse from(Incident incident) {
            return new IncidentResponse(incident.id(), incident.siteId(), incident.assetId(), incident.severity(),
                    incident.status(), incident.description(), incident.createdAt(), incident.resolvedAt());
        }
    }

    private static final class IncidentNotFoundException extends RuntimeException {
    }
}
