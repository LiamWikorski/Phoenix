package com.example.dashboard;

import java.time.Instant;
import java.util.Optional;

public record DashboardMetrics(
        long incidents24h,
        long incidentsPrev24h,
        long errors1h,
        String topPod,
        long topPodCount,
        double topPodPct,
        String topError,
        long topErrorCount,
        double topErrorPct,
        long errorVolume7d,
        long errorVolumePrev7d,
        Instant lastIncidentAt,
        long commits24h,
        long commits7d,
        long podsWithIncidents,
        long totalPodsSeen,
        double mtbfHours7d,
        double mtbfHoursPrev7d
) {
    public double incidentsDeltaPct() {
        return deltaPct(incidentsPrev24h, incidents24h);
    }

    public double errorVolumeDeltaPct() {
        return deltaPct(errorVolumePrev7d, errorVolume7d);
    }

    public double mtbfDeltaPct() {
        return deltaPct(mtbfHoursPrev7d, mtbfHours7d);
    }

    public static double deltaPct(long prev, long current) {
        if (prev == 0L && current == 0L) {
            return 0.0;
        }
        if (prev == 0L) {
            return 100.0;
        }
        return ((double) current - prev) / prev * 100.0;
    }

    public static double deltaPct(double prev, double current) {
        if (Double.compare(prev, 0.0) == 0 && Double.compare(current, 0.0) == 0) {
            return 0.0;
        }
        if (Double.compare(prev, 0.0) == 0) {
            return 100.0;
        }
        return (current - prev) / prev * 100.0;
    }

    public Optional<Instant> lastIncidentOpt() {
        return Optional.ofNullable(lastIncidentAt);
    }
}
