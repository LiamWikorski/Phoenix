package com.example.repo;

public class RepositoryFilePayload {
    private final String path;
    private final String content;

    public RepositoryFilePayload(String path, String content) {
        this.path = path;
        this.content = content;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }
}
