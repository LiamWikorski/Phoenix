package com.example.llm;

import com.example.context.ContextAnalysisService;
import com.example.llm.AgentPlan;
import com.example.llm.AgentApplyResult;
import com.example.llm.AgentPatchApplier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {

    private final ContextAnalysisService analysisService;
    private final AgentPatchApplier applyService;

    public AgentController(ContextAnalysisService analysisService, AgentPatchApplier applyService) {
        this.analysisService = analysisService;
        this.applyService = applyService;
    }

    @GetMapping("/api/llm/analysis")
    public AgentPlan analyze() {
        return analysisService.analyze();
    }

    @PostMapping("/api/llm/apply")
    public AgentApplyResult apply(@RequestBody AgentPlan response) {
        return applyService.apply(response);
    }
}
