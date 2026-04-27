package com.example.context;

import com.example.llm.AgentLlmClient;
import com.example.llm.AgentPlan;
import com.example.llm.history.AgentHistoryStore;
import com.example.llm.history.AnalysisHistoryEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

@Service
public class ContextAnalysisService {

    private final ContextAssemblerService assemblerService;
    private final AgentLlmClient llmClient;
    private final ObjectMapper mapper;
    private final AgentHistoryStore historyStore;

    public ContextAnalysisService(ContextAssemblerService assemblerService, AgentLlmClient llmClient, AgentHistoryStore historyStore) {
        this.assemblerService = assemblerService;
        this.llmClient = llmClient;
        this.historyStore = historyStore;
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public AgentPlan analyze() {
        try {
            String contextJson = mapper.writeValueAsString(assemblerService.buildPayload());
            AgentPlan plan = llmClient.generate(contextJson);
            AnalysisHistoryEntry entry = new AnalysisHistoryEntry(
                    java.time.Instant.now(),
                    plan.summary(),
                    plan.patches() == null ? java.util.List.of() : plan.patches().stream().map(AgentPlan.Patch::path).toList(),
                    "patch",
                    "medium"
            );
            historyStore.appendAnalysis(entry);
            return plan;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to analyze context", e);
        }
    }
}
