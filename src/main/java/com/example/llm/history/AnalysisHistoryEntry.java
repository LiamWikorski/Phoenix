package com.example.llm.history;

import java.time.Instant;
import java.util.List;

public record AnalysisHistoryEntry(
        Instant ts,
        String summary,
        List<String> targetFiles,
        String recommendedAction,
        String confidence
) {}

