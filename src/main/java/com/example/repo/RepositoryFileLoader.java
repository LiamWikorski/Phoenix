package com.example.repo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import static java.util.Locale.ROOT;

@Service
public class RepositoryFileLoader {

    private static final Set<String> ALLOWED_EXTENSIONS;
    private static final Set<String> DENY_FOLDERS;
    private static final List<String> ROOTS = List.of("src", "services", "apps");
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024; // 50KB
    private static final int MAX_FILES = 20;

    private static final List<String> PREFERRED_PATH_TERMS = List.of(
            "currencyservice", "checkoutservice", "adservice", "cartservice",
            "emailservice", "frontend", "paymentservice", "productcatalogservice",
            "recommendationservice", "redis-cart", "shippingservice");

    private static final List<String> PREFERRED_FILENAME_TERMS = List.of(
            "server", "main", "app", "structurederror", "routes", "controller", "config");

    static {
        ALLOWED_EXTENSIONS = Set.of(
                ".java", ".ts", ".tsx", ".js", ".jsx", ".py", ".md", ".yaml", ".yml");

        DENY_FOLDERS = new HashSet<>();
        Collections.addAll(DENY_FOLDERS,
                ".git", "node_modules", "dist", "target", "bin", "build", ".idea", ".vscode");
    }

    private final RepoProperties repoProperties;

    public RepositoryFileLoader(RepoProperties repoProperties) {
        this.repoProperties = repoProperties;
    }

    public List<RepositoryFilePayload> loadFiles() {
        String rootPath = repoProperties.getPath();
        if (rootPath == null || rootPath.isBlank()) {
            return List.of();
        }

        List<RepositoryFilePayload> collected = new ArrayList<>();
        Path repoRoot = Paths.get(rootPath);

        for (String root : ROOTS) {
            Path start = repoRoot.resolve(root);
            if (!Files.exists(start)) {
                continue;
            }
            walk(start, repoRoot, collected);
            if (collected.size() >= MAX_FILES) {
                break;
            }
        }

        if (collected.isEmpty()) {
            return List.of();
        }

        List<ScoredFile> scored = collected.stream()
                .map(this::score)
                .toList();

        int maxScore = scored.stream()
                .mapToInt(ScoredFile::score)
                .max()
                .orElse(Integer.MIN_VALUE);

        // Fallback: if nothing scores positively, return first files alphabetically
        if (maxScore <= 0) {
            collected.sort((a, b) -> a.getPath().compareToIgnoreCase(b.getPath()));
            int end = Math.min(collected.size(), MAX_FILES);
            return new ArrayList<>(collected.subList(0, end));
        }

        List<ScoredFile> sorted = new ArrayList<>(scored);
        sorted.sort(Comparator.comparingInt(ScoredFile::score).reversed()
                .thenComparing(f -> f.payload().getPath(), String.CASE_INSENSITIVE_ORDER));

        List<RepositoryFilePayload> finalList = new ArrayList<>();
        for (ScoredFile sf : sorted) {
            finalList.add(sf.payload());
            if (finalList.size() >= MAX_FILES) {
                break;
            }
        }

        return finalList;
    }

    private void walk(Path current, Path repoRoot, List<RepositoryFilePayload> results) {
        if (results.size() >= MAX_FILES) {
            return;
        }

        try {
            if (Files.isDirectory(current)) {
                String name = current.getFileName().toString();
                if (shouldSkipDir(name)) {
                    return;
                }
                try (var stream = Files.list(current)) {
                    for (Path child : (Iterable<Path>) stream::iterator) {
                        walk(child, repoRoot, results);
                        if (results.size() >= MAX_FILES) {
                            return;
                        }
                    }
                }
            } else {
                if (shouldSkipFile(current)) {
                    return;
                }
                long size = Files.size(current);
                if (size > MAX_FILE_SIZE_BYTES) {
                    return;
                }
                String content = Files.readString(current, StandardCharsets.UTF_8);
                String relative = repoRoot.relativize(current).toString();
                results.add(new RepositoryFilePayload(relative, content));
            }
        } catch (IOException e) {
            // Skip unreadable paths silently for simplicity
        }
    }

    private ScoredFile score(RepositoryFilePayload payload) {
        String path = payload.getPath();
        String lowerPath = path.toLowerCase(ROOT);
        String fileName = extractFileName(lowerPath);

        int score = 0;

        for (String term : PREFERRED_PATH_TERMS) {
            if (lowerPath.contains(term)) {
                score += 3;
                break;
            }
        }

        for (String term : PREFERRED_FILENAME_TERMS) {
            if (fileName.contains(term)) {
                score += 2;
                break;
            }
        }

        if (fileName.equals("client.js")) {
            score -= 3;
        }

        // simple generated/fixture penalty heuristic
        if (lowerPath.contains("generated") || lowerPath.contains("fixtures") || lowerPath.contains("fixture")) {
            score -= 3;
        }

        return new ScoredFile(score, payload);
    }

    private String extractFileName(String lowerPath) {
        int idx = lowerPath.lastIndexOf('/');
        if (idx >= 0 && idx < lowerPath.length() - 1) {
            return lowerPath.substring(idx + 1);
        }
        idx = lowerPath.lastIndexOf('\\');
        if (idx >= 0 && idx < lowerPath.length() - 1) {
            return lowerPath.substring(idx + 1);
        }
        return lowerPath;
    }

    private record ScoredFile(int score, RepositoryFilePayload payload) {
    }

    private boolean shouldSkipDir(String name) {
        if (name.startsWith(".")) {
            return true;
        }
        return DENY_FOLDERS.contains(name);
    }

    private boolean shouldSkipFile(Path file) {
        String name = file.getFileName().toString();
        String lowerName = name.toLowerCase(ROOT);
        if (lowerName.startsWith(".")) {
            return true;
        }
        if (lowerName.equals("readme.md")) {
            return true;
        }
        if (lowerName.endsWith("_pb2.py") || lowerName.endsWith("_pb2_grpc.py")) {
            return true;
        }
        int idx = name.lastIndexOf('.');
        if (idx == -1) {
            return true;
        }
        String ext = name.substring(idx);
        return !ALLOWED_EXTENSIONS.contains(ext);
    }
}
