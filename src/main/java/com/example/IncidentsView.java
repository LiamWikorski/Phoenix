package com.example;

import com.example.bigquery.BigQueryIncidentService;
import com.example.bigquery.IncidentRecord;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@UIScope
@Route(value = "incidents", layout = MainLayout.class)
@PageTitle("Recent Incidents")
public class IncidentsView extends VerticalLayout {

    private static final int DEFAULT_LIMIT = 100;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentsView.class);

    private final BigQueryIncidentService incidentService;
    private final Grid<IncidentRecord> incidentsGrid = new Grid<>(IncidentRecord.class, false);
    private final H3 title = new H3("Recent Errors");
    private final Button refreshButton = new Button("Refresh", event -> loadIncidents());
    private final Button expandButton = new Button("Expand", event -> openDialog());
    private final Span lastUpdated = new Span("Last updated: never");

    @Autowired
    public IncidentsView(BigQueryIncidentService incidentService) {
        this.incidentService = incidentService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureGrid();

        refreshButton.setWidth("120px");
        expandButton.setWidth("120px");

        title.getStyle().set("margin", "0");

        HorizontalLayout actions = new HorizontalLayout(refreshButton, expandButton, lastUpdated);
        actions.setWidthFull();
        actions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        actions.setSpacing(true);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        add(title, actions, incidentsGrid);
        expand(incidentsGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        loadIncidents();
    }

    private void configureGrid() {
        configureGrid(this.incidentsGrid, false);
    }

    private void configureGrid(Grid<IncidentRecord> grid, boolean dialogInstance) {
        grid.addColumn(record -> TIMESTAMP_FORMATTER.format(record.timestamp()))
                .setHeader("Timestamp")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(IncidentRecord::logType)
                .setHeader("Log Type")
                .setAutoWidth(true);

        grid.addColumn(IncidentRecord::pod)
                .setHeader("Pod")
                .setAutoWidth(true);

        grid.addColumn(IncidentRecord::severity)
                .setHeader("Severity")
                .setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(incident -> {
            Div message = new Div();
            message.setText(incident.message());
            message.getStyle()
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("max-width", dialogInstance ? "100%" : "420px");
            message.getElement().setProperty("title", incident.message());
            return message;
        }))
                .setHeader("Message")
                .setFlexGrow(1)
                .setAutoWidth(false)
                .setWidth(dialogInstance ? "100%" : "480px");

        grid.setSizeFull();
    }

    private void loadIncidents() {
        refreshButton.setEnabled(false);
        expandButton.setEnabled(false);
        try {
            List<IncidentRecord> incidents = incidentService.fetchRecentIncidentsNoWindow(DEFAULT_LIMIT);
            incidentsGrid.setItems(incidents);
            updateLastUpdated();
        } catch (RuntimeException ex) {
            LOGGER.error("Unable to load incidents", ex);
            Notification.show("Unable to load incidents. Reason: " + ex.getMessage());
        } finally {
            refreshButton.setEnabled(true);
            expandButton.setEnabled(true);
        }
    }

    private void openDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Recent Incidents");
        dialog.setWidth("90vw");
        dialog.setHeight("80vh");

        Button close = new Button("Close", e -> dialog.close());
        close.setWidth("120px");

        Grid<IncidentRecord> dialogGrid = new Grid<>(IncidentRecord.class, false);
        configureGrid(dialogGrid, true);
        dialogGrid.setItems(incidentsGrid.getListDataView().getItems().toList());
        dialogGrid.setSizeFull();

        dialog.getHeader().add(close);
        dialog.add(dialogGrid);
        dialog.open();
    }

    private void updateLastUpdated() {
        lastUpdated.setText("Last updated: " + TIMESTAMP_FORMATTER.format(Instant.now()));
    }
}
