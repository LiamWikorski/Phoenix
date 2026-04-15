package com.example.repo;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(RepoProperties.class)
public class RepositoryFileLoaderConfig {
}
