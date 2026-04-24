package com.example.context;

import com.example.bigquery.BigQueryIncidentService;
import com.example.bigquery.IncidentRecord;
import com.example.bigquery.PodIncidentCount;
import com.example.config.GithubProperties;
import com.example.config.ContextProperties;
import com.example.github.GithubCommitDto;
import com.example.github.GithubCommitService;
import com.example.repo.RepositoryContextAssembler;
import com.example.repo.RepositoryContextResponse;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContextAssemblerService {

    private final BigQueryIncidentService incidentService;
    private final GithubCommitService commitService;
    private final GithubProperties githubProperties;
    private final ContextProperties contextProperties;
    private final RepositoryContextAssembler repositoryContextAssembler;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public ContextAssemblerService(BigQueryIncidentService incidentService,
                                   GithubCommitService commitService,
                                   GithubProperties githubProperties,
                                   ContextProperties contextProperties,
                                   RepositoryContextAssembler repositoryContextAssembler) {
        this.incidentService = incidentService;
        this.commitService = commitService;
        this.githubProperties = githubProperties;
        this.contextProperties = contextProperties;
        this.repositoryContextAssembler = repositoryContextAssembler;
    }

    public ContextPayload buildPayload() {
        List<IncidentRecord> incidents = incidentService.fetchRecentIncidents(null, contextProperties.getIncidentsLimit());
        List<PodIncidentCount> incidentsPerPod = incidentService.fetchIncidentCountsByPod();

        List<GithubCommitDto> commits = commitService.fetchRecentCommits().stream()
                .limit(contextProperties.getCommitsLimit())
                .toList();

        String repository = githubProperties.getOwner() + "/" + githubProperties.getRepo();

        RepositoryContextResponse repositoryContext = repositoryContextAssembler.buildPayload();

        return new ContextPayload(
                ISO_FORMATTER.format(Instant.now()),
                repository,
                incidents,
                incidentsPerPod,
                commits,
                repositoryContext
        );
    }
}
