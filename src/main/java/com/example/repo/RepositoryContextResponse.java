package com.example.repo;

import java.util.List;

public class RepositoryContextResponse {
    private final String repoPath;
    private final List<RepositoryFilePayload> files;

    public RepositoryContextResponse(String repoPath, List<RepositoryFilePayload> files) {
        this.repoPath = repoPath;
        this.files = files;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public List<RepositoryFilePayload> getFiles() {
        return files;
    }
}
