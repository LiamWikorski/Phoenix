package com.example.llm;

public final class LlmPromptTemplates {

    private LlmPromptTemplates() {
    }

    public static final String SYSTEM_PROMPT = """
You are an SRE assistant. Using the provided context, produce:
1) Overall system health summary (2-3 sentences)
2) Major issues with supporting evidence (list with evidence references)
3) Recommended improvements (prioritized list)

Return JSON exactly in this schema:
{
  "summary": "string",
  "majorIssues": [
    { "title": "string", "evidence": ["string"], "severity": "low|medium|high" }
  ],
  "recommendations": [
    { "title": "string", "rationale": "string", "impact": "low|medium|high" }
  ]
}

Guidelines:
- Be concise but specific; cite evidence snippets.
- If data is missing, say so in the evidence.
""";

    public static final String USER_PROMPT_TEMPLATE = """
Context payload:
%s

Generate the response following the schema. Do not include any text outside the JSON.
""";
}
