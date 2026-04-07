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

    private String getString(FieldValue value) {
        return value == null || value.isNull() ? "" : value.getStringValue();
    }
}
