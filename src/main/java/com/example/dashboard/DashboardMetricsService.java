package com.example.dashboard;

import com.example.bigquery.BigQueryIncidentService;
import com.example.bigquery.DailyErrorCount;
import com.example.bigquery.ErrorMessageCount;
import com.example.bigquery.IncidentRecord;
import com.example.github.GithubCommitDto;
import com.example.github.GithubCommitService;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardMetricsService {

    private static final Duration WINDOW_24H = Duration.ofHours(24);
    private static final Duration WINDOW_48H = Duration.ofHours(48);
    private static final Duration WINDOW_1H = Duration.ofHours(1);
    private static final Duration WINDOW_7D = Duration.ofDays(7);
    private static final Duration WINDOW_14D = Duration.ofDays(14);
    private static final int INCIDENT_LIMIT = 500;

    private final BigQueryIncidentService incidentService;
    private final GithubCommitService commitService;

    public DashboardMetricsService(BigQueryIncidentService incidentService, GithubCommitService commitService) {
        this.incidentService = incidentService;
        this.commitService = commitService;
    }

    public DashboardMetrics load() {
        List<IncidentRecord> last48h = incidentService.fetchRecentIncidents(WINDOW_48H, INCIDENT_LIMIT);
        Instant now = Instant.now();

        Instant cutoff24h = now.minus(WINDOW_24H);
        Instant cutoff48h = now.minus(WINDOW_48H);

        long incidents24h = last48h.stream().filter(r -> !r.timestamp().isBefore(cutoff24h)).count();
        long incidentsPrev24h = last48h.stream()
                .filter(r -> r.timestamp().isBefore(cutoff24h) && !r.timestamp().isBefore(cutoff48h))
                .count();

        long errors1h = last48h.stream()
                .filter(r -> !r.timestamp().isBefore(now.minus(WINDOW_1H)))
                .count();

        Map<String, Long> podCounts = last48h.stream()
                .filter(r -> !r.timestamp().isBefore(cutoff48h))
                .collect(Collectors.groupingBy(r -> normalizePod(r.pod()), Collectors.counting()));
        String topPod = podCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
        long topPodCount = podCounts.getOrDefault(topPod, 0L);
        long totalIncidents = incidents24h + incidentsPrev24h;
        double topPodPct = totalIncidents == 0 ? 0 : (double) topPodCount / totalIncidents * 100.0;
        long podsWithIncidents = podCounts.keySet().size();
        long totalPodsSeen = podsWithIncidents;

        List<ErrorMessageCount> messages = incidentService.fetchErrorMessageCounts();
        ErrorMessageCount top = messages.stream().findFirst().orElse(new ErrorMessageCount("", 0));
        String topError = top.message();
        long topErrorCount = top.count();
        long totalErrorsCounted = messages.stream().mapToLong(ErrorMessageCount::count).sum();
        double topErrorPct = totalErrorsCounted == 0 ? 0 : (double) topErrorCount / totalErrorsCounted * 100.0;

        List<DailyErrorCount> daily = incidentService.fetchDailyErrorCounts();
        List<DailyErrorCount> last7 = daily.stream().limit(7).toList();
        List<DailyErrorCount> prev7 = daily.stream().skip(7).limit(7).toList();
        long errorVolume7d = last7.stream().mapToLong(DailyErrorCount::count).sum();
        long errorVolumePrev7d = prev7.stream().mapToLong(DailyErrorCount::count).sum();

        double mtbfHours7d = computeMtbfHours(last7);
        double mtbfHoursPrev7d = computeMtbfHours(prev7);

        Instant lastIncidentAt = last48h.stream()
                .map(IncidentRecord::timestamp)
                .max(Comparator.naturalOrder())
                .orElse(null);

        List<GithubCommitDto> commits = commitService.fetchRecentCommits();
        long commits24h = commits.stream()
                .filter(c -> !c.timestamp().isBefore(now.minus(WINDOW_24H)))
                .count();
        long commits7d = commits.stream()
                .filter(c -> !c.timestamp().isBefore(now.minus(WINDOW_7D)))
                .count();

        return new DashboardMetrics(
                incidents24h,
                incidentsPrev24h,
                errors1h,
                topPod,
                topPodCount,
                topPodPct,
                topError,
                topErrorCount,
                topErrorPct,
                errorVolume7d,
                errorVolumePrev7d,
                lastIncidentAt,
                commits24h,
                commits7d,
                podsWithIncidents,
                totalPodsSeen,
                mtbfHours7d,
                mtbfHoursPrev7d
        );
    }

    private double computeMtbfHours(List<DailyErrorCount> window) {
        long totalErrors = window.stream().mapToLong(DailyErrorCount::count).sum();
        if (window.isEmpty()) {
            return 0.0;
        }
        if (totalErrors == 0L) {
            return WINDOW_7D.toHours();
        }
        long hours = window.size() * 24L;
        return (double) hours / (double) totalErrors;
    }

    private String normalizePod(String pod) {
        if (pod == null || pod.isBlank()) {
            return "unknown";
        }
        if (pod.startsWith("adservice")) return "adservice";
        if (pod.startsWith("cartservice")) return "cartservice";
        if (pod.startsWith("checkoutservice")) return "checkoutservice";
        if (pod.startsWith("currencyservice")) return "currencyservice";
        if (pod.startsWith("emailservice")) return "emailservice";
        if (pod.startsWith("frontend")) return "frontend";
        if (pod.startsWith("paymentservice")) return "paymentservice";
        if (pod.startsWith("productcatalogservice")) return "productcatalogservice";
        if (pod.startsWith("recommendationservice")) return "recommendationservice";
        if (pod.startsWith("redis-cart")) return "redis-cart";
        if (pod.startsWith("shippingservice")) return "shippingservice";
        return pod;
    }
}
