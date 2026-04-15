package com.example.repo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("phoenix.repo")
public class RepoProperties {

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
