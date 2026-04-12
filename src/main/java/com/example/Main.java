package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.context.ContextProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        com.example.config.BigQueryProperties.class,
        com.example.config.GithubProperties.class,
        ContextProperties.class
})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
