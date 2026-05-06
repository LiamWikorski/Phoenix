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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import com.example.config.RepoProperties;

import static java.util.Locale.ROOT;

@Service
public class RepositoryFileLoader {

    private static final Set<String> ALLOWED_EXTENSIONS;
    private static final Set<String> DENY_FOLDERS;
    private static final String ROOT_DIR = "src";
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024; // 50KB
    private static final int MAX_PRECOLLECT = 200;
    private static final int MAX_FILES = 20;

    private static final List<String> PREFERRED_PATH_TERMS = List.of(
            "currencyservice", "checkoutservice", "adservice", "cartservice",
            "emailservice", "frontend", "paymentservice", "productcatalogservice",
            "recommendationservice", "redis-cart", "shippingservice");

    private static final List<String> PREFERRED_FILENAME_TERMS = List.of(
            "server", "main", "app", "structurederror", "routes", "controller", "config");

    static {
        ALLOWED_EXTENSIONS = Set.of(
                ".java", ".ts", ".tsx", ".js", ".jsx", ".py", ".md", ".yaml", ".yml", ".go", ".sh", ".cs", ".html",
                ".json", ".proto", ".gradle", ".xml");

        DENY_FOLDERS = new HashSet<>();
        Collections.addAll(DENY_FOLDERS,
                ".git", "node_modules", "dist", "target", "bin", "build", ".idea", ".vscode");
    }

    private final RepoProperties repoProperties;

    public RepositoryFileLoader(RepoProperties repoProperties) {
        this.repoProperties = repoProperties;
    }

    public List<RepositoryFilePayload> loadFiles() {
        return loadFiles(null);
    }

    public List<RepositoryFilePayload> loadFiles(List<String> preferredPathTerms) {
        String rootPath = repoProperties.getPath();
        if (rootPath == null || rootPath.isBlank()) {
            return List.of();
        }

        List<RepositoryFilePayload> collected = new ArrayList<>();
        Path repoRoot = Paths.get(rootPath);
        Path srcRoot = repoRoot.resolve(ROOT_DIR);

        List<String> effectivePreferredPathTerms = effectivePreferredPathTerms(preferredPathTerms);
        List<Path> roots = resolveRoots(srcRoot, effectivePreferredPathTerms);

        for (Path root : roots) {
            walk(root, repoRoot, collected);
            if (collected.size() >= MAX_PRECOLLECT) {
                break;
            }
        }

        if (collected.isEmpty()) {
            return List.of();
        }

        List<ScoredFile> scored = collected.stream()
                .map(payload -> score(payload, effectivePreferredPathTerms))
                .toList();

        int maxScore = scored.stream()
                .mapToInt(ScoredFile::score)
                .max()
                .orElse(Integer.MIN_VALUE);

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
        if (results.size() >= MAX_PRECOLLECT) {
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
                        if (results.size() >= MAX_PRECOLLECT) {
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
        }
    }

    private ScoredFile score(RepositoryFilePayload payload, List<String> preferredPathTerms) {
        String path = payload.getPath();
        String lowerPath = path.toLowerCase(ROOT);
        String fileName = extractFileName(lowerPath);

        int score = 0;

        for (String term : preferredPathTerms) {
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

        if (lowerPath.contains("generated") || lowerPath.contains("fixtures") || lowerPath.contains("fixture")) {
            score -= 3;
        }

        return new ScoredFile(score, payload);
    }

    private List<Path> resolveRoots(Path srcRoot, List<String> preferredPathTerms) {
        Set<Path> roots = new LinkedHashSet<>();

        if (preferredPathTerms != null) {
            for (String term : preferredPathTerms) {
                if (term == null || term.isBlank()) {
                    continue;
                }
                Path candidate = srcRoot.resolve(term);
                if (Files.isDirectory(candidate)) {
                    roots.add(candidate);
                }
            }
        }

        if (roots.isEmpty() && Files.isDirectory(srcRoot)) {
            roots.add(srcRoot);
        }

        return new ArrayList<>(roots);
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

    private List<String> effectivePreferredPathTerms(List<String> dynamicTerms) {
        if (dynamicTerms == null) {
            return PREFERRED_PATH_TERMS;
        }

        List<String> normalized = dynamicTerms.stream()
                .map(term -> term == null ? "" : term.trim().toLowerCase(ROOT))
                .filter(term -> !term.isBlank())
                .toList();

        if (normalized.isEmpty()) {
            return PREFERRED_PATH_TERMS;
        }

        return normalized;
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
