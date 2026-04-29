package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "phoenix.context")
public class ContextProperties {

    private int incidentsLimit = 50;

    private int commitsLimit = 10;

    /**
     * Duration window (in ISO-8601 format, e.g. "PT48H") for incidents to include in context.
     */
    private String incidentsWindow = "PT48H";

    public int getIncidentsLimit() {
        return incidentsLimit;
    }

    public void setIncidentsLimit(int incidentsLimit) {
        this.incidentsLimit = incidentsLimit;
    }

    public int getCommitsLimit() {
        return commitsLimit;
    }

    public void setCommitsLimit(int commitsLimit) {
        this.commitsLimit = commitsLimit;
    }

    public String getIncidentsWindow() {
        return incidentsWindow;
    }

    public void setIncidentsWindow(String incidentsWindow) {
        this.incidentsWindow = incidentsWindow;
    }
}
