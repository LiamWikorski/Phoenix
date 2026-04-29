package com.example;

import com.example.context.ContextAnalysisService;
import com.example.context.ContextAssemblerService;
import com.example.context.ContextPayload;
import com.example.llm.AgentApplyResult;
import com.example.llm.AgentPatchApplier;
import com.example.llm.AgentPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "context", layout = MainLayout.class)
@PageTitle("Phoenix Agent")
public class ContextView extends VerticalLayout {

    private final ContextAssemblerService assemblerService;
    private final ContextAnalysisService analysisService;
    private final AgentPatchApplier applyService;
    private final ObjectMapper mapper;

    private ContextPayload contextPayload;
    private AgentPlan agentPlan;
    private AgentApplyResult applyResult;

    private final Button startButton;
    private final Button applyButton;

    private final Div contextStatus;
    private final Div analysisStatus;
    private final Div applyStatus;

    private final Div contextSummary;
    private final Div analysisSummary;
    private final Div applySummary;

    private final TextArea contextRaw;
    private final TextArea analysisRaw;
    private final TextArea applyRaw;

    private final Details contextRawDetails;
    private final Details analysisRawDetails;
    private final Details applyRawDetails;

    @Autowired
    public ContextView(ContextAssemblerService assemblerService, ContextAnalysisService analysisService, AgentPatchApplier applyService) {
        this.assemblerService = assemblerService;
        this.analysisService = analysisService;
        this.applyService = applyService;
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        startButton = new Button("Start Phoenix", event -> startPhoenix());
        startButton.setWidth("180px");

        applyButton = new Button("Apply patches", event -> applyPatches());
        applyButton.setWidth("160px");
        applyButton.setVisible(false);
        applyButton.setEnabled(false);

        contextStatus = new Div();
        analysisStatus = new Div();
        applyStatus = new Div();

        contextSummary = new Div();
        analysisSummary = new Div();
        applySummary = new Div();

        contextRaw = createRawTextArea();
        analysisRaw = createRawTextArea();
        applyRaw = createRawTextArea();

        contextRawDetails = createRawDetails("Show raw context", contextRaw);
        analysisRawDetails = createRawDetails("Show raw analysis", analysisRaw);
        applyRawDetails = createRawDetails("Show raw apply result", applyRaw);

        add(buildHeader(), buildContextSection(), buildAnalysisSection(), buildApplySection());
        resetAll();
    }

    private HorizontalLayout buildHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.setSpacing(true);
        header.add(startButton);
        return header;
    }

    private VerticalLayout buildContextSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.getStyle().set("border", "1px solid #e0e0e0");
        section.getStyle().set("border-radius", "6px");
        section.getStyle().set("background", "#fafafa");

        H4 title = new H4("Context assembly");
        section.add(title, contextStatus, contextSummary, contextRawDetails);
        return section;
    }

    private VerticalLayout buildAnalysisSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.getStyle().set("border", "1px solid #e0e0e0");
        section.getStyle().set("border-radius", "6px");
        section.getStyle().set("background", "#fafafa");

        H4 title = new H4("Analyze with LLM");
        section.add(title, analysisStatus, analysisSummary, analysisRawDetails);
        return section;
    }

    private VerticalLayout buildApplySection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.getStyle().set("border", "1px solid #e0e0e0");
        section.getStyle().set("border-radius", "6px");
        section.getStyle().set("background", "#fafafa");

        H4 title = new H4("Apply patches");
        HorizontalLayout actions = new HorizontalLayout(applyButton);
        actions.setAlignItems(Alignment.CENTER);
        section.add(title, actions, applyStatus, applySummary, applyRawDetails);
        return section;
    }

    private void startPhoenix() {
        resetAll();
        startButton.setEnabled(false);
        updateStatus(contextStatus, StepStatus.IDLE, "Building the AI context…", null);
        updateStatus(analysisStatus, StepStatus.IDLE, "Analysis will start after context is ready…", null);
        try {
            contextPayload = assemblerService.buildPayload();
            contextRaw.setValue(mapper.writeValueAsString(contextPayload));
            renderContextSummary();
            updateStatus(contextStatus, StepStatus.DONE, "Context assembled", null);
            runAnalysis();
        } catch (Exception ex) {
            updateStatus(contextStatus, StepStatus.ERROR, "Failed to build context", ex.getMessage());
            Notification.show("Failed to build context: " + ex.getMessage());
            startButton.setEnabled(true);
        }
    }

    private void runAnalysis() {
        updateStatus(analysisStatus, StepStatus.IDLE, "Analyzing context…", null);
        try {
            agentPlan = analysisService.analyze();
            analysisRaw.setValue(mapper.writeValueAsString(agentPlan));
            renderAnalysisSummary();
            updateStatus(analysisStatus, StepStatus.DONE, "Context analyzed", null);
            applyButton.setVisible(true);
            applyButton.setEnabled(true);
            startButton.setEnabled(true);
        } catch (Exception ex) {
            updateStatus(analysisStatus, StepStatus.ERROR, "LLM analysis failed", ex.getMessage());
            Notification.show("LLM analysis failed: " + ex.getMessage());
            applyButton.setVisible(false);
            startButton.setEnabled(true);
        }
    }

    private void applyPatches() {
        if (agentPlan == null) {
            Notification.show("Run analysis first.");
            return;
        }
        applyButton.setEnabled(false);
        updateStatus(applyStatus, StepStatus.LOADING, "Applying patches…", null);
        try {
            applyResult = applyService.apply(agentPlan);
            applyRaw.setValue(mapper.writeValueAsString(applyResult));
            renderApplySummary();
            updateStatus(applyStatus, StepStatus.DONE, "Patches applied", null);
            applyButton.setEnabled(true);
            startButton.setEnabled(true);
        } catch (Exception ex) {
            updateStatus(applyStatus, StepStatus.ERROR, "Apply failed", ex.getMessage());
            Notification.show("Apply failed: " + ex.getMessage());
            applyButton.setEnabled(true);
            startButton.setEnabled(true);
        }
    }

    private void renderContextSummary() {
        contextSummary.removeAll();
        if (contextPayload == null) {
            contextSummary.add(new Span("No context yet."));
            return;
        }
        Div repo = new Div(new Span("Repository: " + contextPayload.repository()));
        Div counts = new Div(new Span("Incidents: " + (contextPayload.incidents() == null ? 0 : contextPayload.incidents().size())
                + " · Commits: " + (contextPayload.commits() == null ? 0 : contextPayload.commits().size())
                + " · Files: " + (contextPayload.repositoryContext() == null || contextPayload.repositoryContext().getFiles() == null ? 0 : contextPayload.repositoryContext().getFiles().size())));
        Div generated = new Div(new Span("Generated at: " + contextPayload.generatedAt()));
        contextSummary.add(repo, counts, generated);
    }

    private void renderAnalysisSummary() {
        analysisSummary.removeAll();
        if (agentPlan == null) {
            analysisSummary.add(new Span("No analysis yet."));
            return;
        }
        Div summary = new Div(new Span(agentPlan.summary()));

        Div issues = new Div();
        issues.add(new Span("Major issues:"));
        if (agentPlan.majorIssues() != null && !agentPlan.majorIssues().isEmpty()) {
            VerticalLayout list = new VerticalLayout();
            list.setPadding(false);
            list.setSpacing(false);
            agentPlan.majorIssues().forEach(i -> {
                Span item = new Span("• " + i.title() + " [" + i.severity() + "]");
                list.add(item);
            });
            issues.add(list);
        } else {
            issues.add(new Span(" none"));
        }

        Div recs = new Div();
        recs.add(new Span("Recommendations:"));
        if (agentPlan.recommendations() != null && !agentPlan.recommendations().isEmpty()) {
            VerticalLayout list = new VerticalLayout();
            list.setPadding(false);
            list.setSpacing(false);
            agentPlan.recommendations().forEach(r -> list.add(new Span("• " + r.title() + " [" + r.impact() + "]")));
            recs.add(list);
        } else {
            recs.add(new Span(" none"));
        }

        Div patches = new Div();
        int patchCount = agentPlan.patches() == null ? 0 : agentPlan.patches().size();
        patches.add(new Span("Suggested patches: " + patchCount));
        if (patchCount > 0) {
            VerticalLayout list = new VerticalLayout();
            list.setPadding(false);
            list.setSpacing(false);
            agentPlan.patches().forEach(p -> list.add(new Span("• " + p.path())));
            patches.add(list);
        }

        analysisSummary.add(summary, issues, recs, patches);
    }

    private void renderApplySummary() {
        applySummary.removeAll();
        if (applyResult == null) {
            applySummary.add(new Span("No apply attempt yet."));
            return;
        }
        String statusText = applyResult.success() ? "Success" : (applyResult.partialSuccess() ? "Partial success" : "Failure");
        Span status = new Span("Status: " + statusText);
        status.getStyle().set("color", applyResult.success() ? "green" : (applyResult.partialSuccess() ? "#c77f00" : "red"));

        Div branch = new Div(new Span("Branch: " + applyResult.branchName()));
        Div files = new Div();
        files.add(new Span("Files applied: "));
        if (applyResult.appliedFiles() != null && !applyResult.appliedFiles().isEmpty()) {
            VerticalLayout list = new VerticalLayout();
            list.setPadding(false);
            list.setSpacing(false);
            applyResult.appliedFiles().forEach(f -> list.add(new Span("• " + f)));
            files.add(list);
        } else {
            files.add(new Span(" none"));
        }

        if (applyResult.skippedFiles() != null && !applyResult.skippedFiles().isEmpty()) {
            VerticalLayout skipped = new VerticalLayout();
            skipped.setPadding(false);
            skipped.setSpacing(false);
            skipped.add(new Span("Skipped:"));
            applyResult.skippedFiles().forEach(s -> skipped.add(new Span("• " + s.path() + " (" + s.reason() + ")")));
            applySummary.add(status, branch, files, skipped, new Div(new Span(applyResult.message())));
        } else {
            applySummary.add(status, branch, files, new Div(new Span(applyResult.message())));
        }
    }

    private TextArea createRawTextArea() {
        TextArea area = new TextArea();
        area.setWidthFull();
        area.setHeight("240px");
        area.setReadOnly(true);
        area.getStyle().set("font-family", "monospace");
        return area;
    }

    private Details createRawDetails(String summary, TextArea area) {
        Details details = new Details();
        details.setSummaryText(summary);
        details.add(area);
        details.addThemeVariants(DetailsVariant.REVERSE);
        details.setOpened(false);
        details.setWidthFull();
        return details;
    }

    private void resetAll() {
        contextPayload = null;
        agentPlan = null;
        applyResult = null;
        contextRaw.clear();
        analysisRaw.clear();
        applyRaw.clear();
        applyButton.setVisible(false);
        applyButton.setEnabled(false);
        renderContextSummary();
        renderAnalysisSummary();
        renderApplySummary();
        updateStatus(contextStatus, StepStatus.IDLE, "Ready to build context", null);
        updateStatus(analysisStatus, StepStatus.IDLE, "Ready to analyze", null);
        updateStatus(applyStatus, StepStatus.IDLE, "Ready to apply patches", null);
    }

    private void updateStatus(Div target, StepStatus status, String message, String error) {
        target.removeAll();
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);

        switch (status) {
            case LOADING -> {
                ProgressBar bar = new ProgressBar();
                bar.setIndeterminate(true);
                row.add(bar, new Span(message));
            }
            case DONE -> {
                Icon check = VaadinIcon.CHECK_CIRCLE.create();
                check.setColor("green");
                row.add(check, new Span(message));
            }
            case ERROR -> {
                Icon cross = VaadinIcon.CLOSE_CIRCLE.create();
                cross.setColor("red");
                Span msg = new Span(message + (error != null ? ": " + error : ""));
                msg.getStyle().set("color", "red");
                row.add(cross, msg);
            }
            case IDLE -> row.add(new Span(message));
            default -> row.add(new Span(message));
        }

        target.add(row);
    }

    private enum StepStatus { IDLE, LOADING, DONE, ERROR }
}
