package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "phoenix.github")
public class GithubProperties {

    private String pat;
    private String owner;
    private String repo;
    private int limit = 20;

    public String getPat() {
        return pat;
    }

    public void setPat(String pat) {
        this.pat = pat;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
