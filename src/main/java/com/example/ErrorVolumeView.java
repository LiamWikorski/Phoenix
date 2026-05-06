package com.example;

import com.example.bigquery.BigQueryIncidentService;
import com.example.bigquery.DailyErrorCount;
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
import elemental.json.Json;
import elemental.json.JsonArray;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@UIScope
@Route(value = "error-volume", layout = MainLayout.class)
@PageTitle("Error Volume (Last 14 Days)")
public class ErrorVolumeView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final BigQueryIncidentService incidentService;
    private final Button refreshButton = new Button("Refresh", event -> loadData());
    private final Span lastUpdated = new Span("Last updated: never");
    private final Div emptyState = new Div();
    private final H3 last7Title = new H3("Errors per Day (Last 7 Days)");
    private final H3 prev7Title = new H3("Errors per Day (Days 8 to 14)");
    private final ErrorVolumeChart last7Chart = new ErrorVolumeChart();
    private final ErrorVolumeChart prev7Chart = new ErrorVolumeChart();

    @Autowired
    public ErrorVolumeView(BigQueryIncidentService incidentService) {
        this.incidentService = incidentService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureEmptyState();
        refreshButton.setWidth("120px");

        last7Chart.setWidthFull();
        last7Chart.setHeight("220px");
        prev7Chart.setWidthFull();
        prev7Chart.setHeight("220px");

        add(refreshButton, lastUpdated,
                last7Title, last7Chart,
                prev7Title, prev7Chart,
                emptyState);

        emptyState.setVisible(false);
        last7Chart.setVisible(false);
        prev7Chart.setVisible(false);
        expand(last7Chart, prev7Chart);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        loadData();
    }

    private void configureEmptyState() {
        emptyState.setText("No errors found in the last 14 days.");
        emptyState.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);
    }

    private void loadData() {
        refreshButton.setEnabled(false);
        try {
            List<DailyErrorCount> counts = incidentService.fetchDailyErrorCounts();
            if (counts.isEmpty()) {
                last7Chart.setVisible(false);
                prev7Chart.setVisible(false);
                emptyState.setVisible(true);
            } else {
                updateCharts(counts);
                last7Chart.setVisible(true);
                prev7Chart.setVisible(true);
                emptyState.setVisible(false);
            }
            updateLastUpdated();
        } catch (RuntimeException ex) {
            Notification.show("Unable to load error volume data. Reason: " + ex.getMessage());
        } finally {
            refreshButton.setEnabled(true);
        }
    }

    private void updateCharts(List<DailyErrorCount> counts) {
        JsonArray last7Labels = Json.createArray();
        JsonArray last7Values = Json.createArray();
        JsonArray prev7Labels = Json.createArray();
        JsonArray prev7Values = Json.createArray();

        int limit = Math.min(14, counts.size());
        for (int i = 0; i < limit; i++) {
            DailyErrorCount count = counts.get(i);
            if (i < 7) {
                last7Labels.set(i, Json.create(count.day()));
                last7Values.set(i, Json.create(count.count()));
            } else {
                int idx = i - 7;
                prev7Labels.set(idx, Json.create(count.day()));
                prev7Values.set(idx, Json.create(count.count()));
            }
        }

        last7Chart.setData(last7Labels, last7Values);
        prev7Chart.setData(prev7Labels, prev7Values);
    }

    private void updateLastUpdated() {
        lastUpdated.setText("Last updated: " + TIMESTAMP_FORMATTER.format(Instant.now()));
    }

    @Tag("error-volume-chart")
    @JsModule("./error-volume-chart.ts")
    private static class ErrorVolumeChart extends Div {

        public void setData(JsonArray labels, JsonArray values) {
            getElement().callJsFunction("setChartData", labels, values);
        }
    }
}

