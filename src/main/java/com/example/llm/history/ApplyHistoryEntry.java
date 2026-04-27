package com.example.llm.history;

import java.time.Instant;
import java.util.List;

public record ApplyHistoryEntry(
        Instant ts,
        boolean success,
        boolean partialSuccess,
        String branch,
        List<String> appliedFiles,
        List<String> skippedFiles,
        String message,
        String logSummary
) {}

