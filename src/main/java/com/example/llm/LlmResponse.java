package com.example.llm;

import java.util.List;

public record LlmResponse(
        String summary,
        List<MajorIssue> majorIssues,
        List<Recommendation> recommendations,
        List<Patch> patches
) {
    public record MajorIssue(String title, List<String> evidence, String severity) {}
    public record Recommendation(String title, String rationale, String impact) {}
    public record Patch(String path, String patch, String description, String find, String replace) {}
}
