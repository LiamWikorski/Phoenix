package com.example.context;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "phoenix.context")
public class ContextProperties {

    /** Number of incidents to include. */
    private int incidentsLimit = 50;

    /** Number of commits to include. */
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
