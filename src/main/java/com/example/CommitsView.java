package com.example;

import com.example.github.GithubCommitDto;
import com.example.github.GithubCommitService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "commits", layout = MainLayout.class)
@PageTitle("GitHub Commits")
public class CommitsView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final GithubCommitService commitService;
    private final Grid<GithubCommitDto> commitsGrid = new Grid<>(GithubCommitDto.class, false);
    private final Button refreshButton = new Button("Refresh", event -> loadCommits());
    private final Span lastUpdated = new Span("Last updated: never");

    @Autowired
    public CommitsView(GithubCommitService commitService) {
        this.commitService = commitService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureGrid();

        refreshButton.setWidth("120px");

        add(refreshButton, lastUpdated, commitsGrid);
        expand(commitsGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        loadCommits();
    }

    private void configureGrid() {
        commitsGrid.addColumn(GithubCommitDto::authorName)
                .setHeader("Author")
                .setAutoWidth(true)
                .setSortable(true);

        commitsGrid.addColumn(dto -> truncateSha(dto.sha()))
                .setHeader("SHA")
                .setAutoWidth(true)
                .setSortable(true);

        commitsGrid.addColumn(dto -> formatTimestamp(dto.timestamp()))
                .setHeader("Timestamp")
                .setAutoWidth(true)
                .setSortable(true);

        commitsGrid.addColumn(GithubCommitDto::message)
                .setHeader("Message")
                .setFlexGrow(1)
                .setAutoWidth(false);

        commitsGrid.addColumn(new ComponentRenderer<>(dto -> {
            Anchor anchor = new Anchor(dto.url(), "View");
            anchor.setTarget("_blank");
            return anchor;
        }))
                .setHeader("Commit")
                .setAutoWidth(true);

        commitsGrid.setSizeFull();
    }

    private void loadCommits() {
        refreshButton.setEnabled(false);
        try {
            List<GithubCommitDto> commits = commitService.fetchRecentCommits();
            commitsGrid.setItems(commits);
            updateLastUpdated();
        } catch (RuntimeException ex) {
            Notification.show("Unable to load commits. Reason: " + ex.getMessage());
        } finally {
            refreshButton.setEnabled(true);
        }
    }

    private void updateLastUpdated() {
        lastUpdated.setText("Last updated: " + formatTimestamp(Instant.now()));
    }

    private static String formatTimestamp(Instant instant) {
        return TIMESTAMP_FORMATTER.format(instant);
    }

    private static String truncateSha(String sha) {
        if (sha == null) {
            return "";
        }
        return sha.length() <= 7 ? sha : sha.substring(0, 7);
    }
}
