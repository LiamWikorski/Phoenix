package com.example.llm.history;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentHistoryStore {

    private static final int CAP = 10;
    private static final Path HISTORY_PATH = Path.of("src/main/llm/resources/agent-history.json");

    private final ObjectMapper mapper;

    public AgentHistoryStore() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public synchronized HistoryData load() {
        ensureFile();
        try {
            Map<String, Object> raw = mapper.readValue(Files.readString(HISTORY_PATH), new TypeReference<>() {});
            List<AnalysisHistoryEntry> analyses = readList(raw.get("analyses"), new TypeReference<>() {});
            List<ApplyHistoryEntry> applies = readList(raw.get("applies"), new TypeReference<>() {});
            return new HistoryData(analyses, applies);
        } catch (Exception ex) {
            return new HistoryData(new ArrayList<>(), new ArrayList<>());
        }
    }

    public synchronized void appendAnalysis(AnalysisHistoryEntry entry) {
        HistoryData data = load();
        List<AnalysisHistoryEntry> analyses = new ArrayList<>(data.analyses());
        analyses.add(0, entry);
        trim(analyses);
        save(new HistoryData(analyses, data.applies()));
    }

    public synchronized void appendApply(ApplyHistoryEntry entry) {
        HistoryData data = load();
        List<ApplyHistoryEntry> applies = new ArrayList<>(data.applies());
        applies.add(0, entry);
        trim(applies);
        save(new HistoryData(data.analyses(), applies));
    }

    private void ensureFile() {
        try {
            if (!Files.exists(HISTORY_PATH)) {
                Files.createDirectories(HISTORY_PATH.getParent());
                Map<String, Object> empty = new HashMap<>();
                empty.put("analyses", Collections.emptyList());
                empty.put("applies", Collections.emptyList());
                Files.writeString(HISTORY_PATH, mapper.writeValueAsString(empty));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize history file", e);
        }
    }

    private <T> List<T> readList(Object source, TypeReference<List<T>> ref) {
        if (source == null) return new ArrayList<>();
        try {
            String json = mapper.writeValueAsString(source);
            return mapper.readValue(json, ref);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void save(HistoryData data) {
        try {
            Map<String, Object> payload = Map.of(
                    "analyses", data.analyses(),
                    "applies", data.applies()
            );
            Files.writeString(HISTORY_PATH, mapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist history", e);
        }
    }

    private <T> void trim(List<T> list) {
        if (list.size() > CAP) {
            list.subList(CAP, list.size()).clear();
        }
    }

    public record HistoryData(List<AnalysisHistoryEntry> analyses, List<ApplyHistoryEntry> applies) {}
}

