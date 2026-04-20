package com.example.llm;

import java.util.List;

public record PatchValidationResult(
        boolean valid,
        List<String> errors,
        List<String> filesTouched
) {}
