package com.example;

import com.example.context.ContextAnalysisService;
import com.example.context.ContextAssemblerService;
import com.example.context.ContextPayload;
import com.example.llm.LlmApplyResult;
import com.example.llm.LlmApplyService;
import com.example.llm.LlmResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "context", layout = MainLayout.class)
@PageTitle("AI Context")
public class ContextView extends VerticalLayout {

    private final ContextAssemblerService assemblerService;
    private final ContextAnalysisService analysisService;
    private final LlmApplyService applyService;
    private final ObjectMapper mapper;
    private final TextArea output;
    private final TextArea llmOutput;
    private final TextArea applyOutput;

    @Autowired
    public ContextView(ContextAssemblerService assemblerService, ContextAnalysisService analysisService, LlmApplyService applyService) {
        this.assemblerService = assemblerService;
        this.analysisService = analysisService;
        this.applyService = applyService;
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.output = new TextArea("Context JSON");
        this.llmOutput = new TextArea("LLM Analysis");
        this.applyOutput = new TextArea("Apply Result");

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Button generate = new Button("Build AI context", event -> build());
        generate.setWidth("160px");

        Button analyze = new Button("Analyze with LLM", event -> analyze());
        analyze.setWidth("160px");

        Button apply = new Button("Apply patches", event -> apply());
        apply.setWidth("160px");

        output.setWidthFull();
        output.setHeight("600px");
        output.setReadOnly(true);
        output.getStyle().set("font-family", "monospace");

        llmOutput.setWidthFull();
        llmOutput.setHeight("300px");
        llmOutput.setReadOnly(true);
        llmOutput.getStyle().set("font-family", "monospace");

        applyOutput.setWidthFull();
        applyOutput.setHeight("240px");
        applyOutput.setReadOnly(true);
        applyOutput.getStyle().set("font-family", "monospace");

        add(generate, analyze, apply, output, llmOutput, applyOutput);
        expand(output, llmOutput, applyOutput);
    }

    private void build() {
        try {
            ContextPayload payload = assemblerService.buildPayload();
            output.setValue(mapper.writeValueAsString(payload));
        } catch (Exception ex) {
            Notification.show("Failed to build context: " + ex.getMessage());
        }
    }

    private void analyze() {
        try {
            LlmResponse response = analysisService.analyze();
            llmOutput.setValue(mapper.writeValueAsString(response));
        } catch (Exception ex) {
            Notification.show("LLM analysis failed: " + ex.getMessage());
        }
    }

    private void apply() {
        try {
            String llmText = llmOutput.getValue();
            if (llmText == null || llmText.isBlank()) {
                Notification.show("Run analysis first.");
                return;
            }
            LlmResponse response = mapper.readValue(llmText, LlmResponse.class);
            LlmApplyResult result = applyService.apply(response);
            applyOutput.setValue(mapper.writeValueAsString(result));
        } catch (Exception ex) {
            Notification.show("Apply failed: " + ex.getMessage());
        }
    }
}
