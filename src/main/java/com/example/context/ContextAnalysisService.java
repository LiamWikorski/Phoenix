package com.example.context;

import com.example.llm.AgentLlmClient;
import com.example.llm.AgentPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

@Service
public class ContextAnalysisService {

    private final ContextAssemblerService assemblerService;
    private final AgentLlmClient llmClient;
    private final ObjectMapper mapper;

    public ContextAnalysisService(ContextAssemblerService assemblerService, AgentLlmClient llmClient) {
        this.assemblerService = assemblerService;
        this.llmClient = llmClient;
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public AgentPlan analyze() {
        try {
            String contextJson = mapper.writeValueAsString(assemblerService.buildPayload());
            return llmClient.generate(contextJson);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to analyze context", e);
        }
    }
}
