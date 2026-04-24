package com.example.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Service;
import com.example.config.RepoProperties;

@Service
public class LlmApplyService {

    private static final List<String> ALLOW_PREFIXES = List.of(
            "src/adservice/",
            "src/cartservice/",
            "src/checkoutservice/",
            "src/currencyservice/",
            "src/emailservice/",
            "src/frontend/",
            "src/loadgenerator/",
            "src/paymentservice/",
            "src/productcatalogservice/",
            "src/recommendationservice/",
            "src/shippingservice/",
            "src/shoppingassistantservice/"
    );
    private static final List<String> BLOCK_PREFIXES = List.of(
            ".git/", "target/", "build/", "dist/", "node_modules/", "out/"
    );
    private static final List<String> BLOCK_NAMES = List.of(
            ".env", ".env.local", ".envrc"
    );

    private final RepoProperties repoProperties;

    public LlmApplyService(RepoProperties repoProperties) {
        this.repoProperties = repoProperties;
    }

    public LlmApplyResult apply(LlmResponse response) {
        String branchName = "autofix/" + Instant.now().toEpochMilli();
        List<String> filesTouched = new ArrayList<>();
        StringBuilder log = new StringBuilder();

        try (Repository repo = openRepo(repoProperties.getPath());
             Git git = new Git(repo)) {

            for (LlmResponse.Patch patch : response.patches()) {
                validatePatch(patch);
                if (patch.patch() != null) {
                    collectTouchedFiles(patch.patch(), filesTouched);
                }
            }

            git.checkout().setCreateBranch(true).setName(branchName).call();

            List<String> appliedFiles = new ArrayList<>();
            List<LlmApplyResult.SkippedFile> skippedFiles = new ArrayList<>();

            for (LlmResponse.Patch patch : response.patches()) {
                boolean applied = false;
                if (patch.find() != null && patch.replace() != null) {
                    applied = applyFindReplace(patch, log, filesTouched);
                }
                if (!applied) {
                    if (patch.patch() != null) {
                        try {
                            applyUnifiedDiff(git, patch.patch(), log);
                            applied = true;
                        } catch (Exception ex) {
                            skippedFiles.add(new LlmApplyResult.SkippedFile(patch.path(), "Diff apply failed: " + ex.getMessage()));
                        }
                    } else {
                        skippedFiles.add(new LlmApplyResult.SkippedFile(patch.path(), "Find text not present and no diff provided"));
                    }
                }

                if (applied && !appliedFiles.contains(patch.path())) {
                    appliedFiles.add(patch.path());
                } else if (!applied && skippedFiles.stream().noneMatch(s -> s.path().equals(patch.path()))) {
                    skippedFiles.add(new LlmApplyResult.SkippedFile(patch.path(), "Not applied"));
                }
            }

            Integer exit = null;
            try {
                exit = runCommand("mvn -q test", log);
            } catch (IOException ioe) {
                log.append("warn: mvn not found; skipping validation\n");
            }
            if (exit != null && exit != 0) {
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call();
                return new LlmApplyResult(false, false, branchName, List.copyOf(appliedFiles), List.copyOf(skippedFiles), log.toString(), "Validation failed");
            }

            if (appliedFiles.isEmpty()) {
                return new LlmApplyResult(false, false, branchName, List.copyOf(appliedFiles), List.copyOf(skippedFiles), log.toString(), "No edits applied");
            }

            git.commit().setAll(true).setMessage("chore: apply llm fixes").call();
            return new LlmApplyResult(true, skippedFiles.isEmpty() ? false : true, branchName, List.copyOf(appliedFiles), List.copyOf(skippedFiles), log.toString(), skippedFiles.isEmpty() ? "Applied successfully" : "Applied with skips");
        } catch (Exception e) {
            log.append("error: ").append(e.getMessage());
            return new LlmApplyResult(false, false, branchName, List.of(), List.of(), log.toString(), "Apply failed");
        }
    }

    private Repository openRepo(String repoPath) throws IOException {
        return new FileRepositoryBuilder()
                .setGitDir(new java.io.File(repoPath, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
    }

    private void validatePatch(LlmResponse.Patch patch) {
        String path = patch.path();
        boolean allowed = ALLOW_PREFIXES.stream().anyMatch(path::startsWith);
        if (!allowed) {
            throw new IllegalArgumentException("Path not allowed: " + path);
        }
        for (String block : BLOCK_PREFIXES) {
            if (path.startsWith(block)) {
                throw new IllegalArgumentException("Blocked path: " + path);
            }
        }
        for (String name : BLOCK_NAMES) {
            if (path.contains(name)) {
                throw new IllegalArgumentException("Blocked filename: " + path);
            }
        }
        boolean hasFindReplace = patch.find() != null && patch.replace() != null;
        boolean hasDiff = patch.patch() != null && patch.patch().contains("+++ ") && patch.patch().contains("--- ");
        if (!hasFindReplace && !hasDiff) {
            throw new IllegalArgumentException("Invalid patch payload for " + path);
        }
    }

    private boolean applyFindReplace(LlmResponse.Patch patch, StringBuilder log, List<String> filesTouched) throws IOException {
        java.nio.file.Path target = Paths.get(repoProperties.getPath()).resolve(patch.path()).toAbsolutePath();
        log.append("repoRoot=").append(repoProperties.getPath())
                .append(" patchPath=").append(patch.path())
                .append(" resolved=").append(target)
                .append(" exists=").append(java.nio.file.Files.exists(target))
                .append(" mode=find-replace")
                .append('\n');

        if (!java.nio.file.Files.exists(target)) {
            throw new IllegalArgumentException("Target file missing: " + patch.path());
        }

        String contentRaw = java.nio.file.Files.readString(target, StandardCharsets.UTF_8);
        String lineEnding = contentRaw.contains("\r\n") ? "\r\n" : "\n";
        String content = normalizeNewlines(contentRaw);
        String find = normalizeNewlines(patch.find());
        String replace = normalizeNewlines(patch.replace());

        int idx = content.indexOf(find);
        if (idx < 0) {
            log.append("warn: find text not present in ").append(patch.path()).append("; find=<<<").append(snippet(find)).append(">>>\n");
            return false;
        }

        String updated = content.replaceFirst(java.util.regex.Pattern.quote(find), java.util.regex.Matcher.quoteReplacement(replace));
        updated = updated.replace("\n", lineEnding);
        java.nio.file.Files.writeString(target, updated, StandardCharsets.UTF_8);

        if (!filesTouched.contains(patch.path())) {
            filesTouched.add(patch.path());
        }
        return true;
    }

    private String normalizeNewlines(String text) {
        if (text == null) return "";
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    private String snippet(String text) {
        if (text == null) return "";
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    private void collectTouchedFiles(String unifiedDiff, List<String> filesTouched) throws IOException {
        for (String line : unifiedDiff.split("\n")) {
            if (line.startsWith("+++ ") || line.startsWith("--- ")) {
                String name = normalizeDiffPath(line.substring(4).trim());
                if (!name.equals("/dev/null") && !filesTouched.contains(name)) {
                    filesTouched.add(name);
                }
            }
        }
    }

    private void applyUnifiedDiff(Git git, String unifiedDiff, StringBuilder log) throws IOException, InterruptedException {
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("llm-patch", ".diff");
        java.nio.file.Files.writeString(temp, unifiedDiff, StandardCharsets.UTF_8);

        int stripCount = detectStripCount(unifiedDiff);
        String targetPath = findFirstTargetPath(unifiedDiff);
        if (targetPath != null) {
            java.nio.file.Path resolved = Paths.get(repoProperties.getPath()).resolve(targetPath).toAbsolutePath();
            log.append("repoRoot=").append(repoProperties.getPath())
                    .append(" patchPath=").append(targetPath)
                    .append(" resolved=").append(resolved)
                    .append(" exists=").append(java.nio.file.Files.exists(resolved))
                    .append(" strip=-p").append(stripCount)
                    .append('\n');
        }

        try {
            String stripArg = "-p" + stripCount;
            runGit(List.of("git", "apply", "--check", "--recount", stripArg, temp.toString()), repoProperties.getPath(), log);
            runGit(List.of("git", "apply", "--recount", stripArg, temp.toString()), repoProperties.getPath(), log);
        } finally {
            java.nio.file.Files.deleteIfExists(temp);
        }
    }

    private int runCommand(String command, StringBuilder log) throws IOException, InterruptedException {
        Process process = new ProcessBuilder()
                .command(splitCommand(command))
                .redirectErrorStream(true)
                .start();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append('\n');
            }
        }
        int exit = process.waitFor();
        log.append("exit code: ").append(exit).append('\n');
        return exit;
    }

    private void runGit(List<String> cmd, String workingDir, StringBuilder log) throws IOException, InterruptedException {
        List<String> effective = new ArrayList<>();
        if (workingDir != null && !workingDir.isBlank()) {
            effective.add("git");
            effective.add("-C");
            effective.add(workingDir);
            effective.addAll(cmd.subList(1, cmd.size()));
        } else {
            effective.addAll(cmd);
        }

        ProcessBuilder pb = new ProcessBuilder(effective);
        pb.redirectErrorStream(true);

        Process p = pb.start();
        StringBuilder local = new StringBuilder();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                local.append(line).append('\n');
            }
        }
        int exit = p.waitFor();
        if (local.length() > 0) {
            log.append(local);
        }
        if (exit != 0) {
            log.append("git command failed (exit ").append(exit).append("): ").append(String.join(" ", effective)).append('\n');
            throw new IllegalStateException("git apply failed with exit code " + exit);
        }
    }

    private List<String> splitCommand(String command) {
        String[] parts = command.split(" ");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) list.add(p);
        }
        return list;
    }

    private String normalizeDiffPath(String name) {
        if (name.startsWith("a/")) return name.substring(2);
        if (name.startsWith("b/")) return name.substring(2);
        return name;
    }

    private String findFirstTargetPath(String diff) {
        for (String line : diff.split("\n")) {
            if (line.startsWith("+++ ") || line.startsWith("--- ")) {
                String name = line.substring(4).trim();
                if (!"/dev/null".equals(name)) {
                    return normalizeDiffPath(name);
                }
            }
        }
        return null;
    }

    private int detectStripCount(String diff) {
        int abCount = 0;
        int rawCount = 0;
        for (String line : diff.split("\n")) {
            if (line.startsWith("+++ ") || line.startsWith("--- ")) {
                String name = line.substring(4).trim();
                if (name.startsWith("a/") || name.startsWith("b/")) {
                    abCount++;
                } else {
                    rawCount++;
                }
            }
        }
        if (abCount > 0 && rawCount > 0) {
            throw new IllegalArgumentException("Patch contains mixed header styles (a/b and raw paths). Use consistent headers.");
        }
        return abCount > 0 ? 1 : 0;
    }
}
