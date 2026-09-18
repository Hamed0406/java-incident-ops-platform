# Network Incident Platform

Java 21 and Spring Boot implementation of the first Incident Service vertical slice.

```bash
mvn test
mvn spring-boot:run
```

The service exposes:

- `GET /health/live`
- `GET /health/ready`
- `POST /api/incidents`
- `GET /api/incidents/{id}`
- `POST /api/incidents/{id}/resolve`
- `POST /api/incidents/{id}/acknowledge`
- `POST /api/incidents/{id}/investigate`
- `POST /api/incidents/{id}/close`
- `PUT /api/incidents/{id}/severity`
- `PUT /api/incidents/{id}/owner`

Incident data is deliberately in memory for this first slice. PostgreSQL, Kafka, authentication, and deployment infrastructure follow only when the preceding slice is complete.
