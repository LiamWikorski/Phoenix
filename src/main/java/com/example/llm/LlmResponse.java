package com.example.llm;

import java.util.List;

public record LlmResponse(
        String summary,
        List<MajorIssue> majorIssues,
        List<Recommendation> recommendations
) {
    public record MajorIssue(String title, List<String> evidence, String severity) {}
    public record Recommendation(String title, String rationale, String impact) {}
}
