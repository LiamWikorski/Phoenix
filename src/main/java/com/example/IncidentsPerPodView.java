package com.example;

import com.example.bigquery.BigQueryIncidentService;
import com.example.bigquery.PodIncidentCount;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
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
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import elemental.json.Json;
import elemental.json.JsonArray;

@SpringComponent
@UIScope
@Route(value = "incidents-per-pod", layout = MainLayout.class)
@PageTitle("Incidents per Service")
public class IncidentsPerPodView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final BigQueryIncidentService incidentService;
    private final H3 title = new H3("Incidents per Pod");
    private final Button refreshButton = new Button("Refresh", event -> loadData());
    private final Span lastUpdated = new Span("Last updated: never");
    private final Div emptyState = new Div();
    private final PodChart chart = new PodChart();

    @Autowired
    public IncidentsPerPodView(BigQueryIncidentService incidentService) {
        this.incidentService = incidentService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureEmptyState();
        refreshButton.setWidth("120px");

        title.getStyle().set("margin", "0");

        add(title, refreshButton, lastUpdated, chart, emptyState);
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
        emptyState.setText("No service incidents found across the latest 100 entries.");
        emptyState.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);
    }

    private void loadData() {
        refreshButton.setEnabled(false);
        try {
            List<PodIncidentCount> counts = incidentService.fetchIncidentCountsByPod();
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
            Notification.show("Unable to load incidents-per-pod data. Reason: " + ex.getMessage());
        } finally {
            refreshButton.setEnabled(true);
        }
    }

    private void updateChart(List<PodIncidentCount> counts) {
        JsonArray labels = Json.createArray();
        JsonArray values = Json.createArray();
        for (int i = 0; i < counts.size(); i++) {
            PodIncidentCount count = counts.get(i);
            labels.set(i, Json.create(count.pod()));
            values.set(i, Json.create(count.count()));
        }
        chart.setData(labels, values);
    }

    private void updateLastUpdated() {
        lastUpdated.setText("Last updated: " + TIMESTAMP_FORMATTER.format(Instant.now()));
    }

    @Tag("incidents-per-pod-chart")
    @JsModule("./incidents-per-pod-chart.ts")
    private static class PodChart extends Div {

        public void setData(JsonArray labels, JsonArray values) {
            getElement().callJsFunction("setChartData", labels, values);
        }
    }
}
