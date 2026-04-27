package com.example.llm.history;

import java.time.Instant;

public record HistorySummary(
        Instant latestErrorTs,
        Instant latestSuccessfulApplyTs,
        boolean newErrorAfterSuccessfulApply,
        boolean assumedResolved
) {}

