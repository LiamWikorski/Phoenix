package com.example.context;

import com.example.bigquery.BigQueryIncidentService;
import com.example.bigquery.IncidentRecord;
import com.example.bigquery.PodIncidentCount;
import com.example.config.GithubProperties;
import com.example.config.ContextProperties;
import com.example.github.GithubCommitDto;
import com.example.github.GithubCommitService;
import com.example.llm.history.AgentHistoryStore;
import com.example.llm.history.AnalysisHistoryEntry;
import com.example.llm.history.ApplyHistoryEntry;
import com.example.llm.history.HistorySummary;
import com.example.repo.RepositoryContextAssembler;
import com.example.repo.RepositoryContextResponse;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContextAssemblerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextAssemblerService.class);

    private final BigQueryIncidentService incidentService;
    private final GithubCommitService commitService;
    private final GithubProperties githubProperties;
    private final ContextProperties contextProperties;
    private final RepositoryContextAssembler repositoryContextAssembler;
    private final AgentHistoryStore historyStore;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public ContextAssemblerService(BigQueryIncidentService incidentService,
                                   GithubCommitService commitService,
                                   GithubProperties githubProperties,
                                   ContextProperties contextProperties,
                                   RepositoryContextAssembler repositoryContextAssembler,
                                   AgentHistoryStore historyStore) {
        this.incidentService = incidentService;
        this.commitService = commitService;
        this.githubProperties = githubProperties;
        this.contextProperties = contextProperties;
        this.repositoryContextAssembler = repositoryContextAssembler;
        this.historyStore = historyStore;
    }

    public ContextPayload buildPayload() {
        Duration window = Duration.parse(contextProperties.getIncidentsWindow());
        LOGGER.info("Building context with incidentsWindow={} and incidentsLimit={}", window, contextProperties.getIncidentsLimit());
        List<IncidentRecord> incidents = incidentService.fetchRecentIncidents(window, contextProperties.getIncidentsLimit());
        List<PodIncidentCount> incidentsPerPod = incidentService.fetchIncidentCountsByPod();

        List<GithubCommitDto> commits = commitService.fetchRecentCommits().stream()
                .limit(contextProperties.getCommitsLimit())
                .toList();

        String repository = githubProperties.getOwner() + "/" + githubProperties.getRepo();

        List<String> podTerms = incidentsPerPod.stream()
                .map(PodIncidentCount::pod)
                .filter(p -> p != null)
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .toList();

        RepositoryContextResponse repositoryContext = repositoryContextAssembler.buildPayload(podTerms);

        AgentHistoryStore.HistoryData history = historyStore.load();
        HistorySummary historySummary = computeHistorySummary(incidents, history.applies());

        return new ContextPayload(
                ISO_FORMATTER.format(Instant.now()),
                repository,
                incidents,
                incidentsPerPod,
                commits,
                repositoryContext,
                history.analyses(),
                history.applies(),
                historySummary
        );
    }

    private HistorySummary computeHistorySummary(List<IncidentRecord> incidents, List<ApplyHistoryEntry> applies) {
        Instant latestErrorTs = incidents.stream()
                .map(IncidentRecord::timestamp)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        Instant latestSuccessfulApplyTs = applies.stream()
                .filter(a -> a.success())
                .map(ApplyHistoryEntry::ts)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        boolean newErrorAfterSuccessfulApply = latestErrorTs != null && latestSuccessfulApplyTs != null
                && latestErrorTs.isAfter(latestSuccessfulApplyTs);
        boolean assumedResolved = latestSuccessfulApplyTs != null
                && (latestErrorTs == null || latestSuccessfulApplyTs.isAfter(latestErrorTs));

        return new HistorySummary(latestErrorTs, latestSuccessfulApplyTs, newErrorAfterSuccessfulApply, assumedResolved);
    }
}
