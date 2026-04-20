package com.example.llm;

import java.util.List;

public record LlmApplyResult(
        boolean success,
        boolean partialSuccess,
        String branchName,
        List<String> appliedFiles,
        List<SkippedFile> skippedFiles,
        String log,
        String message
) {
    public record SkippedFile(String path, String reason) {}
}
