package com.example.llm;

import com.example.context.ContextAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LlmController {

    private final ContextAnalysisService analysisService;
    private final LlmApplyService applyService;

    public LlmController(ContextAnalysisService analysisService, LlmApplyService applyService) {
        this.analysisService = analysisService;
        this.applyService = applyService;
    }

    @GetMapping("/api/llm/analysis")
    public LlmResponse analyze() {
        return analysisService.analyze();
    }

    @PostMapping("/api/llm/apply")
    public LlmApplyResult apply(@RequestBody LlmResponse response) {
        return applyService.apply(response);
    }
}
