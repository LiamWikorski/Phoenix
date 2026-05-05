package com.example.repo;

import java.util.List;

import org.springframework.stereotype.Service;
import com.example.config.RepoProperties;

@Service
public class RepositoryContextAssembler {

    private final RepositoryFileLoader loader;
    private final RepoProperties repoProperties;

    public RepositoryContextAssembler(RepositoryFileLoader loader, RepoProperties repoProperties) {
        this.loader = loader;
        this.repoProperties = repoProperties;
    }

    public RepositoryContextResponse buildPayload() {
        return buildPayload(null);
    }

    public RepositoryContextResponse buildPayload(List<String> preferredPathTerms) {
        List<RepositoryFilePayload> files = loader.loadFiles(preferredPathTerms);
        return new RepositoryContextResponse(repoProperties.getPath(), files);
    }
}
