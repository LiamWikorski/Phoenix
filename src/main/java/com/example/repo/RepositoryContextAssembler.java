package com.example.repo;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class RepositoryContextAssembler {

    private final RepositoryFileLoader loader;
    private final RepoProperties repoProperties;

    public RepositoryContextAssembler(RepositoryFileLoader loader, RepoProperties repoProperties) {
        this.loader = loader;
        this.repoProperties = repoProperties;
    }

    public RepositoryContextResponse buildPayload() {
        List<RepositoryFilePayload> files = loader.loadFiles();
        return new RepositoryContextResponse(repoProperties.getPath(), files);
    }
}
