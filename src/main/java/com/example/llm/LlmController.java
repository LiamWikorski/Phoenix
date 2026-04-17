package com.example.llm;

import com.example.context.ContextAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LlmController {

    private final ContextAnalysisService analysisService;

    public LlmController(ContextAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/api/llm/analysis")
    public LlmResponse analyze() {
        return analysisService.analyze();
    }
}
