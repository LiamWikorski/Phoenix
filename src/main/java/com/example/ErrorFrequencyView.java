package com.example;

import com.example.bigquery.BigQueryIncidentService;
import com.example.bigquery.ErrorMessageCount;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import elemental.json.Json;
import elemental.json.JsonArray;

@SpringComponent
@UIScope
@Route(value = "error-frequency", layout = MainLayout.class)
@PageTitle("Frequency of Errors")
public class ErrorFrequencyView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final BigQueryIncidentService incidentService;
    private final Button refreshButton = new Button("Refresh", event -> loadData());
    private final Span lastUpdated = new Span("Last updated: never");
    private final Div emptyState = new Div();
    private final ErrorChart chart = new ErrorChart();

    @Autowired
    public ErrorFrequencyView(BigQueryIncidentService incidentService) {
        this.incidentService = incidentService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureEmptyState();
        refreshButton.setWidth("120px");

        add(refreshButton, lastUpdated, chart, emptyState);
        chart.setWidthFull();
        chart.setHeight("250px");
        chart.setVisible(false);
        emptyState.setVisible(false);
        expand(chart);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        loadData();
    }

    private void configureEmptyState() {
        emptyState.setText("No error messages found across the latest 100 entries.");
        emptyState.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);
    }

    private void loadData() {
        refreshButton.setEnabled(false);
        try {
            List<ErrorMessageCount> counts = incidentService.fetchErrorMessageCounts();
            if (counts.isEmpty()) {
                chart.setVisible(false);
                emptyState.setVisible(true);
            } else {
                updateChart(counts);
                chart.setVisible(true);
                emptyState.setVisible(false);
            }
            updateLastUpdated();
        } catch (RuntimeException ex) {
            Notification.show("Unable to load error frequency data. Reason: " + ex.getMessage());
        } finally {
            refreshButton.setEnabled(true);
        }
    }

    private void updateChart(List<ErrorMessageCount> counts) {
        JsonArray labels = Json.createArray();
        JsonArray values = Json.createArray();
        for (int i = 0; i < counts.size(); i++) {
            ErrorMessageCount count = counts.get(i);
            labels.set(i, Json.create(truncate(count.message(), 24)));
            values.set(i, Json.create(count.count()));
        }
        chart.setData(labels, values);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private void updateLastUpdated() {
        lastUpdated.setText("Last updated: " + TIMESTAMP_FORMATTER.format(Instant.now()));
    }

    @Tag("error-frequency-chart")
    @JsModule("./error-frequency-chart.ts")
    private static class ErrorChart extends Div {

        public void setData(JsonArray labels, JsonArray values) {
            getElement().callJsFunction("setChartData", labels, values);
        }
    }
}
