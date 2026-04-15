package com.example.repo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repo-files")
public class RepositoryContextController {

    private final RepositoryFileLoader loader;
    private final RepoProperties repoProperties;

    public RepositoryContextController(RepositoryFileLoader loader, RepoProperties repoProperties) {
        this.loader = loader;
        this.repoProperties = repoProperties;
    }

    @GetMapping
    public RepositoryContextResponse getRepositoryFiles() {
        List<RepositoryFilePayload> files = loader.loadFiles();
        return new RepositoryContextResponse(repoProperties.getPath(), files);
    }
}
