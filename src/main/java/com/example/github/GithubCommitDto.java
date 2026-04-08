package com.example.github;

import java.time.Instant;

public record GithubCommitDto(
        String authorName,
        String message,
        Instant timestamp,
        String url,
        String sha
) {
}
