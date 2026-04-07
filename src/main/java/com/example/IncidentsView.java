package com.example;

import com.example.bigquery.BigQueryIncidentService;
import com.example.bigquery.IncidentRecord;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Route("")
@PageTitle("Recent Incidents")
public class IncidentsView extends VerticalLayout {

    private static final Duration DEFAULT_WINDOW = Duration.ofHours(24);
    private static final int DEFAULT_LIMIT = 100;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentsView.class);

    private final BigQueryIncidentService incidentService;
    private final Grid<IncidentRecord> incidentsGrid = new Grid<>(IncidentRecord.class, false);
    private final Button refreshButton = new Button("Refresh", event -> loadIncidents());
    private final Span lastUpdated = new Span("Last updated: never");

    @Autowired
    public IncidentsView(BigQueryIncidentService incidentService) {
        this.incidentService = incidentService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureGrid();

        refreshButton.setWidth("120px");

        add(refreshButton, lastUpdated, incidentsGrid);
        expand(incidentsGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        loadIncidents();
    }

    private void configureGrid() {
        incidentsGrid.addColumn(record -> TIMESTAMP_FORMATTER.format(record.timestamp()))
                .setHeader("Timestamp")
                .setSortable(true)
                .setAutoWidth(true);

        incidentsGrid.addColumn(IncidentRecord::logType)
                .setHeader("Log Type")
                .setAutoWidth(true);

        incidentsGrid.addColumn(IncidentRecord::service)
                .setHeader("Service")
                .setAutoWidth(true);

        incidentsGrid.addColumn(IncidentRecord::pod)
                .setHeader("Pod")
                .setAutoWidth(true);

        incidentsGrid.addColumn(IncidentRecord::namespace)
                .setHeader("Namespace")
                .setAutoWidth(true);

        incidentsGrid.addColumn(IncidentRecord::severity)
                .setHeader("Severity")
                .setAutoWidth(true);

        incidentsGrid.addColumn(new ComponentRenderer<>(incident -> {
            Div message = new Div();
            message.setText(incident.message());
            message.getStyle().set("white-space", "pre-wrap");
            return message;
        }))
                .setHeader("Message")
                .setFlexGrow(1)
                .setAutoWidth(false);

        incidentsGrid.setSizeFull();
    }

    private void loadIncidents() {
        refreshButton.setEnabled(false);
        try {
            List<IncidentRecord> incidents = incidentService.fetchRecentIncidents(DEFAULT_WINDOW, DEFAULT_LIMIT);
            incidentsGrid.setItems(incidents);
            updateLastUpdated();
        } catch (RuntimeException ex) {
            LOGGER.error("Unable to load incidents", ex);
            Notification.show("Unable to load incidents. Reason: " + ex.getMessage());
        } finally {
            refreshButton.setEnabled(true);
        }
    }

    private void updateLastUpdated() {
        lastUpdated.setText("Last updated: " + TIMESTAMP_FORMATTER.format(Instant.now()));
    }
}
