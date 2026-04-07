package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "phoenix.bigquery")
public class BigQueryProperties {

    private String projectId;

    private String location;

    private String dataset;

    private String unifiedTable;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getUnifiedTable() {
        return unifiedTable;
    }

    public void setUnifiedTable(String unifiedTable) {
        this.unifiedTable = unifiedTable;
    }
}
