package com.example.context;

import com.example.bigquery.IncidentRecord;
import com.example.bigquery.PodIncidentCount;
import com.example.github.GithubCommitDto;
import com.example.repo.RepositoryContextResponse;
import com.example.llm.history.AnalysisHistoryEntry;
import com.example.llm.history.ApplyHistoryEntry;
import com.example.llm.history.HistorySummary;
import java.util.List;

public record ContextPayload(
        String generatedAt,
        String repository,
        List<IncidentRecord> incidents,
        List<PodIncidentCount> incidentsPerPod,
        List<GithubCommitDto> commits,
        RepositoryContextResponse repositoryContext,
        List<AnalysisHistoryEntry> analysisHistory,
        List<ApplyHistoryEntry> applyHistory,
        HistorySummary historySummary
) {
}
