package com.example.context;

import com.example.bigquery.IncidentRecord;
import com.example.bigquery.PodIncidentCount;
import com.example.github.GithubCommitDto;
import com.example.repo.RepositoryContextResponse;
import java.util.List;

/**
 * Aggregated context payload for LLM usage.
 */
public record ContextPayload(
        String generatedAt,
        String repository,
        List<IncidentRecord> incidents,
        List<PodIncidentCount> incidentsPerPod,
        List<GithubCommitDto> commits,
        RepositoryContextResponse repositoryContext
) {
}
