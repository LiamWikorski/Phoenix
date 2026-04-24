package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "phoenix.context")
public class ContextProperties {

    private int incidentsLimit = 50;

    private int commitsLimit = 10;

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
}
