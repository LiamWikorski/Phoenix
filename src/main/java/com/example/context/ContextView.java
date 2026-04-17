package com.example.context;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.example.MainLayout;
import com.example.llm.LlmResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "context", layout = MainLayout.class)
@PageTitle("AI Context")
public class ContextView extends VerticalLayout {

    private final ContextAssemblerService assemblerService;
    private final ContextAnalysisService analysisService;
    private final ObjectMapper mapper;
    private final TextArea output;
    private final TextArea llmOutput;

    @Autowired
    public ContextView(ContextAssemblerService assemblerService, ContextAnalysisService analysisService) {
        this.assemblerService = assemblerService;
        this.analysisService = analysisService;
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.output = new TextArea("Context JSON");
        this.llmOutput = new TextArea("LLM Analysis");

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Button generate = new Button("Build AI context", event -> build());
        generate.setWidth("160px");

        Button analyze = new Button("Analyze with LLM", event -> analyze());
        analyze.setWidth("160px");

        output.setWidthFull();
        output.setHeight("600px");
        output.setReadOnly(true);
        output.getStyle().set("font-family", "monospace");

        llmOutput.setWidthFull();
        llmOutput.setHeight("300px");
        llmOutput.setReadOnly(true);
        llmOutput.getStyle().set("font-family", "monospace");

        add(generate, analyze, output, llmOutput);
        expand(output, llmOutput);
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
}
