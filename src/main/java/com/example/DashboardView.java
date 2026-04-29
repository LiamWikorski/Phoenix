package com.example;

import com.example.dashboard.DashboardMetrics;
import com.example.dashboard.DashboardMetricsService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard")
public class DashboardView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    @Autowired
    public DashboardView(DashboardMetricsService metricsService,
                         IncidentsView incidentsView,
                         IncidentsPerPodView incidentsPerPodView,
                         ErrorFrequencyView errorFrequencyView,
                         ErrorVolumeView errorVolumeView,
                         CommitsView commitsView) {

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        DashboardMetrics metrics = metricsService.load();

        FlexLayout kpis = new FlexLayout(
                card("Incidents (24h)", formatLong(metrics.incidents24h()),
                        deltaBadge(metrics.incidentsDeltaPct(), false),
                        "vs prev 24h"),
                card("Errors (last hour)", formatLong(metrics.errors1h()),
                        neutralBadge(),
                        "Time window: 1h"),
                card("Top pod", ellipsize(metrics.topPod(), 18),
                        mutedBadge(formatPct(metrics.topPodPct())),
                        formatLong(metrics.topPodCount()) + " incidents"),
                card("Top error", ellipsize(metrics.topError(), 24),
                        mutedBadge(formatPct(metrics.topErrorPct())),
                        formatLong(metrics.topErrorCount()) + " occurrences"),
                card("Error volume (7d)", formatLong(metrics.errorVolume7d()),
                        deltaBadge(metrics.errorVolumeDeltaPct(), false),
                        "vs prior 7d"),
                card("Commits", formatLong(metrics.commits24h()) + " / " + formatLong(metrics.commits7d()),
                        mutedBadge("24h / 7d"),
                        "Recent commit activity"),
                mtbfCard(metrics.mtbfHours7d(), metrics.mtbfDeltaPct())
        );
        kpis.setWidthFull();
        kpis.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        kpis.getStyle().set("gap", "var(--lumo-space-m)");
        add(kpis);

        HorizontalLayout topRow = new HorizontalLayout(incidentsPerPodView, errorFrequencyView);
        topRow.setWidthFull();
        topRow.setSpacing(true);
        topRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.START);
        incidentsPerPodView.setWidth("50%");
        errorFrequencyView.setWidth("50%");
        add(topRow);

        errorVolumeView.setWidthFull();
        add(errorVolumeView);

        incidentsView.setWidth("50%");
        commitsView.setWidth("50%");
        incidentsView.setMinHeight("360px");
        commitsView.setMinHeight("360px");
        HorizontalLayout bottomRow = new HorizontalLayout(incidentsView, commitsView);
        bottomRow.setWidthFull();
        bottomRow.setSpacing(true);
        bottomRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.START);
        add(bottomRow);

        setFlexGrow(1, topRow, errorVolumeView, bottomRow);
        bottomRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.STRETCH);
    }

    private Div card(String title, String value, Span badge, String subtitle) {
        Div card = new Div();
        card.getStyle()
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("background", "var(--lumo-base-color)")
                .set("min-width", "240px")
                .set("flex", "1 1 240px");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.02em");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "700")
                .set("line-height", "1.1");

        Span subtitleSpan = new Span(subtitle);
        subtitleSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Div top = new Div(titleSpan, badge);
        top.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("gap", "var(--lumo-space-s)");

        card.add(top, valueSpan, subtitleSpan);
        card.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "var(--lumo-space-s)");
        return card;
    }

    private Div mtbfCard(double mtbfHours7d, double mtbfDeltaPct) {
        String title = "MTBF";
        String value;
        String subtitle = "vs prior 7d";
        Span badge = deltaBadge(mtbfDeltaPct, true);

        if (Double.compare(mtbfHours7d, 0.0) == 0) {
            value = "No data";
            badge = neutralBadge();
        } else {
            value = formatHours(mtbfHours7d);
        }

        return card(title, value, badge, subtitle);
    }

    private Span deltaBadge(double deltaPct, boolean higherIsBetter) {
        String text;
        String color;
        if (Math.abs(deltaPct) < 0.0001) {
            text = "0%";
            color = "var(--lumo-secondary-text-color)";
        } else {
            boolean positive = deltaPct > 0;
            boolean good = higherIsBetter ? positive : !positive;
            color = good ? "var(--lumo-success-color)" : "var(--lumo-error-color)";
            text = String.format(Locale.ENGLISH, "%+.1f%%", deltaPct);
        }
        Span badge = new Span(text);
        badge.getStyle()
                .set("padding", "0.1rem 0.45rem")
                .set("border-radius", "999px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "600")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 12%, transparent)");
        return badge;
    }

    private Span mutedBadge(String text) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("padding", "0.1rem 0.45rem")
                .set("border-radius", "999px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("background", "color-mix(in srgb, var(--lumo-secondary-text-color) 10%, transparent)");
        return badge;
    }

    private Span neutralBadge() {
        return mutedBadge("–");
    }

    private static String formatLong(long value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ENGLISH, "%.1fm", value / 1_000_000.0);
        }
        if (value >= 1_000) {
            return String.format(Locale.ENGLISH, "%.1fk", value / 1_000.0);
        }
        return Long.toString(value);
    }

    private static String formatHours(double hours) {
        if (hours >= 24 * 7) {
            return String.format(Locale.ENGLISH, "%.1fd", hours / 24.0);
        }
        if (hours >= 24) {
            return String.format(Locale.ENGLISH, "%.1fd", hours / 24.0);
        }
        return String.format(Locale.ENGLISH, "%.1fh", hours);
    }

    private static String formatPct(double pct) {
        return String.format(Locale.ENGLISH, "%.1f%%", pct);
    }

    private static String relativeAge(Duration age) {
        long minutes = age.toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = age.toHours();
        if (hours < 24) return hours + "h ago";
        long days = age.toDays();
        return days + "d ago";
    }

    private static String ellipsize(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(0, max - 1)) + "…";
    }
}
