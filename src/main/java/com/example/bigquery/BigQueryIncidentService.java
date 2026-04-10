package com.example.bigquery;

import com.example.config.BigQueryProperties;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BigQueryIncidentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryIncidentService.class);

    private static final String INCIDENTS_QUERY = """
            SELECT
              *
            FROM `{unifiedTable}`
            LIMIT @limit;
            """;

    private static final String INCIDENTS_PER_POD_QUERY = """
            WITH latest_incidents AS (
                SELECT COALESCE(pod, '') AS pod
                FROM `{unifiedTable}`
                ORDER BY timestamp DESC
                LIMIT 100
            ),
            service_names AS (
                SELECT
                  CASE
                    WHEN STARTS_WITH(pod, 'adservice') THEN 'adservice'
                    WHEN STARTS_WITH(pod, 'cartservice') THEN 'cartservice'
                    WHEN STARTS_WITH(pod, 'checkoutservice') THEN 'checkoutservice'
                    WHEN STARTS_WITH(pod, 'currencyservice') THEN 'currencyservice'
                    WHEN STARTS_WITH(pod, 'emailservice') THEN 'emailservice'
                    WHEN STARTS_WITH(pod, 'frontend') THEN 'frontend'
                    WHEN STARTS_WITH(pod, 'paymentservice') THEN 'paymentservice'
                    WHEN STARTS_WITH(pod, 'productcatalogservice') THEN 'productcatalogservice'
                    WHEN STARTS_WITH(pod, 'recommendationservice') THEN 'recommendationservice'
                    WHEN STARTS_WITH(pod, 'redis-cart') THEN 'redis-cart'
                    WHEN STARTS_WITH(pod, 'shippingservice') THEN 'shippingservice'
                    ELSE pod
                  END AS pod
                FROM latest_incidents
            )
            SELECT
              pod,
              COUNT(*) AS incident_count
            FROM service_names
            GROUP BY pod
            ORDER BY incident_count DESC;
            """;

    private final BigQuery bigQuery;
    private final BigQueryProperties properties;

    public BigQueryIncidentService(BigQueryProperties properties) {
        this.properties = properties;
        this.bigQuery = BigQueryOptions.newBuilder()
                .setProjectId(properties.getProjectId())
                .build()
                .getService();
    }

    public List<IncidentRecord> fetchRecentIncidents(Duration window, int limit) {
        String unifiedTable = String.format("%s.%s.%s",
                properties.getProjectId(), properties.getDataset(), properties.getUnifiedTable());

        String queryText = INCIDENTS_QUERY.replace("{unifiedTable}", unifiedTable);

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryText)
                .addNamedParameter("limit", QueryParameterValue.int64(limit))
                .setUseLegacySql(false)
                .build();

        try {
            TableResult result;
            if (properties.getLocation() != null && !properties.getLocation().isBlank()) {
                JobId.Builder jobIdBuilder = JobId.newBuilder()
                        .setJob("phoenix_incidents_" + UUID.randomUUID());
                if (properties.getProjectId() != null && !properties.getProjectId().isBlank()) {
                    jobIdBuilder.setProject(properties.getProjectId());
                }
                jobIdBuilder.setLocation(properties.getLocation());
                result = bigQuery.query(queryConfig, jobIdBuilder.build());
            } else {
                result = bigQuery.query(queryConfig);
            }
            List<IncidentRecord> incidents = new ArrayList<>();
            result.iterateAll().forEach(row -> incidents.add(mapRow(row)));
            return incidents;
        } catch (BigQueryException ex) {
            LOGGER.error("Failed to load incidents from BigQuery", ex);
            throw new RuntimeException("Unable to load incidents from BigQuery: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.error("BigQuery incident query was interrupted", ex);
            throw new RuntimeException("BigQuery incident query was interrupted", ex);
        }
    }

    public List<PodIncidentCount> fetchIncidentCountsByPod() {
        String unifiedTable = String.format("%s.%s.%s",
                properties.getProjectId(), properties.getDataset(), properties.getUnifiedTable());

        String queryText = INCIDENTS_PER_POD_QUERY.replace("{unifiedTable}", unifiedTable);

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryText)
                .setUseLegacySql(false)
                .build();

        try {
            TableResult result;
            if (properties.getLocation() != null && !properties.getLocation().isBlank()) {
                JobId.Builder jobIdBuilder = JobId.newBuilder()
                        .setJob("phoenix_incidents_per_pod_" + UUID.randomUUID());
                if (properties.getProjectId() != null && !properties.getProjectId().isBlank()) {
                    jobIdBuilder.setProject(properties.getProjectId());
                }
                jobIdBuilder.setLocation(properties.getLocation());
                result = bigQuery.query(queryConfig, jobIdBuilder.build());
            } else {
                result = bigQuery.query(queryConfig);
            }

            List<PodIncidentCount> counts = new ArrayList<>();
            result.iterateAll().forEach(row -> counts.add(mapPodCountRow(row)));
            return counts;
        } catch (BigQueryException ex) {
            LOGGER.error("Failed to load incidents-per-pod data from BigQuery", ex);
            throw new RuntimeException("Unable to load incidents-per-pod data from BigQuery: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.error("BigQuery incidents-per-pod query was interrupted", ex);
            throw new RuntimeException("BigQuery incidents-per-pod query was interrupted", ex);
        }
    }

    private IncidentRecord mapRow(FieldValueList row) {
        FieldValue timestampValue = row.get("timestamp");
        if (timestampValue == null || timestampValue.isNull()) {
            throw new IllegalStateException("Timestamp is required in incident rows");
        }

        long micros = timestampValue.getTimestampValue();
        Instant timestamp = Instant.ofEpochSecond(micros / 1_000_000L, (micros % 1_000_000L) * 1000);
        return new IncidentRecord(
                timestamp,
                getString(row.get("log_type")),
                getString(row.get("service")),
                getString(row.get("pod")),
                getString(row.get("namespace")),
                getString(row.get("severity")),
                getString(row.get("message"))
        );
    }

    private PodIncidentCount mapPodCountRow(FieldValueList row) {
        String pod = normalisePod(row.get("pod"));
        FieldValue countValue = row.get("incident_count");
        if (countValue == null || countValue.isNull()) {
            throw new IllegalStateException("Incident count is required for incidents-per-pod rows");
        }
        long count = countValue.getLongValue();
        return new PodIncidentCount(pod, count);
    }

    private String normalisePod(FieldValue podValue) {
        String pod = getString(podValue);
        return (pod == null || pod.isBlank()) ? "unknown" : pod;
    }

    private String getString(FieldValue value) {
        return value == null || value.isNull() ? "" : value.getStringValue();
    }
}
