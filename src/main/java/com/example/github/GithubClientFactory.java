package com.example.github;

import java.io.IOException;
import org.kohsuke.github.GitHub;
import org.springframework.stereotype.Component;

@Component
public class GithubClientFactory {

    public GitHub createClient(String personalAccessToken) throws IOException {
        return GitHub.connectUsingOAuth(personalAccessToken);
    }
}
