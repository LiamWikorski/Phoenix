package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.config.ContextProperties;
import com.example.config.LlmProperties;
import com.example.config.RepoProperties;
import com.example.config.BigQueryProperties;
import com.example.config.GithubProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        BigQueryProperties.class,
        GithubProperties.class,
        ContextProperties.class,
        RepoProperties.class,
        LlmProperties.class
})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
