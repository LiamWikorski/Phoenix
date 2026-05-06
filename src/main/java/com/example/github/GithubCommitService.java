package com.example.github;

import com.example.config.GithubProperties;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.GitUser;
import org.kohsuke.github.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GithubCommitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GithubCommitService.class);

    private final GithubProperties properties;
    private final GithubClientFactory clientFactory;

    public GithubCommitService(GithubProperties properties, GithubClientFactory clientFactory) {
        this.properties = properties;
        this.clientFactory = clientFactory;
    }

    public List<GithubCommitDto> fetchRecentCommits() {
        if (properties.getPat() == null || properties.getPat().isBlank()) {
            throw new IllegalStateException("GitHub PAT is required to fetch commits");
        }
        if (properties.getOwner() == null || properties.getOwner().isBlank()) {
            throw new IllegalStateException("GitHub repository owner is required");
        }
        if (properties.getRepo() == null || properties.getRepo().isBlank()) {
            throw new IllegalStateException("GitHub repository name is required");
        }
        try {
            GitHub gitHub = clientFactory.createClient(properties.getPat());
            String repositoryFullName = properties.getOwner() + "/" + properties.getRepo();
            GHRepository repository = gitHub.getRepository(repositoryFullName);
            PagedIterable<GHCommit> pagedCommits = repository.listCommits().withPageSize(properties.getLimit());
            List<GHCommit> commits = pagedCommits.toList();
            return commits.stream()
                    .limit(properties.getLimit())
                    .map(GithubCommitService::mapCommit)
                    .collect(Collectors.toList());
        } catch (HttpException ex) {
            LOGGER.warn("GitHub API returned an error while fetching commits (likely rate limit or service issue): {}", ex.getMessage());
            return Collections.emptyList();
        } catch (IOException ex) {
            LOGGER.warn("Failed to fetch commits from GitHub: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private static GithubCommitDto mapCommit(GHCommit commit) {
        try {
            GHCommit.ShortInfo info = commit.getCommitShortInfo();
            GitUser author = info != null ? info.getAuthor() : null;
            String authorName = author != null && author.getName() != null ? author.getName() : "Unknown";
            String message = info != null && info.getMessage() != null ? info.getMessage() : "";
            GitUser committer = info != null ? info.getCommitter() : null;
            Instant timestamp = committer != null && committer.getDate() != null
                    ? committer.getDate().toInstant()
                    : Instant.now();
            URL htmlUrl = commit.getHtmlUrl();
            String url = htmlUrl != null ? htmlUrl.toString() : "";
            String sha = commit.getSHA1();
            return new GithubCommitDto(authorName, message, timestamp, url, sha);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to map GitHub commit", ex);
        }
    }
}
