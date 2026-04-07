package com.example.bigquery;

import java.time.Instant;

public record IncidentRecord(
        Instant timestamp,
        String logType,
        String service,
        String pod,
        String namespace,
        String severity,
        String message
) {
}
