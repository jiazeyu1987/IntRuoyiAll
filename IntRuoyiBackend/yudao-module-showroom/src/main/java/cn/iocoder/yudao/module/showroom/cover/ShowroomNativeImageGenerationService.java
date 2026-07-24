package cn.iocoder.yudao.module.showroom.cover;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShowroomNativeImageGenerationService {

    private static final String ERROR_CODE = "SHOWROOM_COVER_GENERATION_FAILED";
    private static final long CODEX_TIMEOUT_DEFAULT_MS = 240000L;
    private static final int CODEX_BATCH_PARALLELISM_MAX = 8;
    private static final List<String> FORBIDDEN_FALLBACK_MARKERS = List.of(
            "image_gen.py",
            "local image enhancement pass",
            "non-generative enhancement",
            "creating the final local png",
            "applying a non-generative enhancement"
    );

    private final YudaoAiProperties yudaoAiProperties;

    public ShowroomNativeImageGenerationService(YudaoAiProperties yudaoAiProperties) {
        this.yudaoAiProperties = yudaoAiProperties;
    }

    public Path generatePng(String promptText, String errorCode, String entityLabel) {
        return generatePng(promptText, null, errorCode, entityLabel);
    }

    public Path generatePng(String promptText, Path sourceImagePath, String errorCode, String entityLabel) {
        String resolvedErrorCode = StrUtil.blankToDefault(errorCode, ERROR_CODE).trim();
        String resolvedEntityLabel = StrUtil.blankToDefault(entityLabel, "showroom image").trim();
        if (StrUtil.isBlank(promptText)) {
            throw new IllegalStateException(resolvedErrorCode + ": rendered prompt text is required");
        }
        if (sourceImagePath != null) {
            if (!sourceImagePath.isAbsolute()) {
                throw new IllegalStateException(resolvedErrorCode + ": source image path must be absolute");
            }
            if (!Files.isRegularFile(sourceImagePath)) {
                throw new IllegalStateException(resolvedErrorCode + ": source image file does not exist: "
                        + sourceImagePath);
            }
        }
        YudaoAiProperties.CodexCli codexCli = yudaoAiProperties.getCodexCli();
        String command = resolveCodexCommand(codexCli);
        long timeoutMs = resolveTimeoutMs(codexCli);
        Path outputFile = null;
        Path stdoutFile = null;
        try {
            outputFile = Files.createTempFile("codex-cli-cover-path-", ".txt");
            stdoutFile = Files.createTempFile("codex-cli-cover-stdout-", ".log");
            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(command, codexCli, outputFile));
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(stdoutFile.toFile());
            processBuilder.directory(Path.of(resolveWorkingDirectory(codexCli)).toFile());
            applyOpenAiEnvironment(processBuilder, codexCli);

            Process process = processBuilder.start();
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(buildPrompt(promptText, sourceImagePath));
            }

            long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
            while (true) {
                assertNoForbiddenFallback(stdoutFile, resolvedErrorCode);
                Path readyFile = tryResolveReadyGeneratedFile(outputFile, resolvedErrorCode);
                if (readyFile != null) {
                    assertNoForbiddenFallback(stdoutFile, resolvedErrorCode);
                    destroyProcessQuietly(process);
                    return readyFile;
                }
                if (!process.isAlive()) {
                    break;
                }
                if (System.nanoTime() >= deadlineNanos) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                    throw new IllegalStateException(resolvedErrorCode + ": local codex cli timed out after "
                            + timeoutMs + " ms");
                }
                process.waitFor(Math.min(1000L, timeoutMs), TimeUnit.MILLISECONDS);
            }
            process.waitFor(5, TimeUnit.SECONDS);
            String stdout = readTextSafely(stdoutFile);
            assertNoForbiddenFallback(stdout, resolvedErrorCode);
            if (process.exitValue() != 0) {
                throw new IllegalStateException(resolvedErrorCode + ": local codex cli failed with exit code "
                        + process.exitValue() + ", stdout: " + trimForError(stdout));
            }

            String generatedPathText = readGeneratedPath(outputFile, resolvedErrorCode);
            Path generatedFile = resolveGeneratedPath(generatedPathText, resolvedErrorCode);
            if (!Files.isRegularFile(generatedFile)) {
                throw new IllegalStateException(resolvedErrorCode + ": generated " + resolvedEntityLabel
                        + " file does not exist: " + generatedFile);
            }
            if (!generatedFile.getFileName().toString().toLowerCase().endsWith(".png")) {
                throw new IllegalStateException(resolvedErrorCode + ": generated " + resolvedEntityLabel
                        + " file must be png: " + generatedFile);
            }
            return generatedFile;
        } catch (IOException e) {
            throw new IllegalStateException(resolvedErrorCode + ": failed to execute local codex cli command `"
                    + command + "`", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(resolvedErrorCode + ": interrupted while waiting for local codex cli", e);
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary codex cli output file {}", outputFile, e);
                }
            }
            if (stdoutFile != null) {
                try {
                    Files.deleteIfExists(stdoutFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary codex cli stdout file {}", stdoutFile, e);
                }
            }
        }
    }

    public int resolveBatchParallelism() {
        YudaoAiProperties.CodexCli codexCli = yudaoAiProperties.getCodexCli();
        Integer parallelism = resolveCodexCliParallelism(codexCli);
        if (parallelism == null) {
            return CODEX_BATCH_PARALLELISM_MAX;
        }
        if (parallelism <= 0) {
            throw new IllegalStateException(ERROR_CODE + ": codex cli parallelism must be greater than 0");
        }
        return Math.min(parallelism, CODEX_BATCH_PARALLELISM_MAX);
    }

    static String resolveCodexCommand(YudaoAiProperties.CodexCli codexCli) {
        if (codexCli != null && StrUtil.isNotBlank(codexCli.getCommand())) {
            return codexCli.getCommand().trim();
        }
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win") ? "codex.cmd" : "codex";
    }

    private static long resolveTimeoutMs(YudaoAiProperties.CodexCli codexCli) {
        if (codexCli != null && codexCli.getTimeoutMs() != null && codexCli.getTimeoutMs() > 0) {
            return codexCli.getTimeoutMs();
        }
        return CODEX_TIMEOUT_DEFAULT_MS;
    }

    private static String resolveWorkingDirectory(YudaoAiProperties.CodexCli codexCli) {
        if (codexCli != null && StrUtil.isNotBlank(codexCli.getWorkingDirectory())) {
            return codexCli.getWorkingDirectory().trim();
        }
        return System.getProperty("user.dir");
    }

    private static List<String> buildCommand(String command, YudaoAiProperties.CodexCli codexCli, Path outputFile) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(command);
        commandLine.add("exec");
        commandLine.add("-");
        commandLine.add("--skip-git-repo-check");
        commandLine.add("--dangerously-bypass-approvals-and-sandbox");
        commandLine.add("--ephemeral");
        commandLine.add("--output-last-message");
        commandLine.add(outputFile.toString());
        if (codexCli != null && StrUtil.isNotBlank(codexCli.getModel())) {
            commandLine.add("-m");
            commandLine.add(codexCli.getModel().trim());
        }
        if (codexCli != null && StrUtil.isNotBlank(codexCli.getWorkingDirectory())) {
            commandLine.add("-C");
            commandLine.add(codexCli.getWorkingDirectory().trim());
        }
        return commandLine;
    }

    private static void assertNoForbiddenFallback(Path stdoutFile, String errorCode) {
        assertNoForbiddenFallback(readTextSafely(stdoutFile), errorCode);
    }

    private static void assertNoForbiddenFallback(String stdout, String errorCode) {
        String normalized = StrUtil.blankToDefault(stdout, "").toLowerCase(Locale.ROOT);
        for (String marker : FORBIDDEN_FALLBACK_MARKERS) {
            if (normalized.contains(marker)) {
                throw new IllegalStateException(errorCode + ": codex cli used forbidden fallback path: "
                        + marker + ", stdout: " + trimForError(stdout));
            }
        }
    }

    static void applyOpenAiEnvironment(ProcessBuilder processBuilder, YudaoAiProperties.CodexCli codexCli) {
        if (processBuilder == null || codexCli == null) {
            return;
        }
        String openAiApiKey = readCodexCliStringProperty(codexCli, "getOpenAiApiKey");
        if (StrUtil.isNotBlank(openAiApiKey)) {
            processBuilder.environment().put("OPENAI_API_KEY", openAiApiKey.trim());
        }
        String openAiBaseUrl = readCodexCliStringProperty(codexCli, "getOpenAiBaseUrl");
        if (StrUtil.isNotBlank(openAiBaseUrl)) {
            processBuilder.environment().put("OPENAI_BASE_URL", openAiBaseUrl.trim());
        }
    }

    private static String readTextSafely(Path path) {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String trimForError(String stdout) {
        return StrUtil.maxLength(StrUtil.blankToDefault(stdout, ""), 4000);
    }

    private static String readGeneratedPath(Path outputFile, String errorCode) throws IOException {
        if (!Files.isRegularFile(outputFile)) {
            throw new IllegalStateException(errorCode + ": codex cli output path file was not created");
        }
        String content = Files.readString(outputFile, StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            throw new IllegalStateException(errorCode + ": codex cli returned blank generated path");
        }
        for (String line : content.split("\\R")) {
            if (StrUtil.isNotBlank(line)) {
                return line.trim();
            }
        }
        throw new IllegalStateException(errorCode + ": codex cli returned blank generated path");
    }

    private static Path tryResolveReadyGeneratedFile(Path outputFile, String errorCode) throws IOException {
        if (outputFile == null || !Files.isRegularFile(outputFile)) {
            return null;
        }
        String content = Files.readString(outputFile, StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return null;
        }
        for (String line : content.split("\\R")) {
            if (StrUtil.isBlank(line)) {
                continue;
            }
            Path generatedFile = resolveGeneratedPath(line.trim(), errorCode);
            if (Files.isRegularFile(generatedFile)
                    && generatedFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                return generatedFile;
            }
        }
        return null;
    }

    private static void destroyProcessQuietly(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS) && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static Path resolveGeneratedPath(String generatedPathText, String errorCode) {
        try {
            Path generatedFile = Path.of(generatedPathText);
            if (!generatedFile.isAbsolute()) {
                throw new IllegalStateException(errorCode + ": generated image path must be absolute");
            }
            return generatedFile;
        } catch (InvalidPathException exception) {
            throw new IllegalStateException(errorCode + ": " + generatedPathText, exception);
        }
    }

    private static Integer resolveCodexCliParallelism(YudaoAiProperties.CodexCli codexCli) {
        if (codexCli == null) {
            return CODEX_BATCH_PARALLELISM_MAX;
        }
        try {
            Object value = codexCli.getClass().getMethod("getParallelism").invoke(codexCli);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text && StrUtil.isNotBlank(text)) {
                return Integer.parseInt(text.trim());
            }
            return CODEX_BATCH_PARALLELISM_MAX;
        } catch (ReflectiveOperationException ignored) {
            return CODEX_BATCH_PARALLELISM_MAX;
        }
    }

    private static String readCodexCliStringProperty(YudaoAiProperties.CodexCli codexCli, String methodName) {
        if (codexCli == null) {
            return null;
        }
        try {
            Object value = codexCli.getClass().getMethod(methodName).invoke(codexCli);
            return value instanceof String text ? text : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String buildPrompt(String promptText, Path sourceImagePath) {
        String normalizedPrompt = promptText.trim();
        if (sourceImagePath == null) {
            return normalizedPrompt;
        }
        return """
                Use the $generate-ai-scene-image skill immediately.
                Do not inspect the repository, do not create task records, do not explain a plan, and do not run shell commands unless strictly required to call the image tool.
                Use only the built-in image generation tool path from that skill.
                Do not use imagegen, scripts/image_gen.py, Python Pillow enhancement, local non-generative image editing, or any other fallback path.
                If the built-in image generation tool cannot complete, fail explicitly instead of using any fallback.
                Use the existing local source image as the required visual input reference.
                Source image absolute path: %s
                Preserve the original subject and semantic identity from that source image.
                Do not refuse because the source image is local; the path above is the provided input image for this task.
                Return exactly one absolute local PNG path and nothing else.

                %s
                """.formatted(normalizePathForPrompt(sourceImagePath), normalizedPrompt).trim();
    }

    private static String normalizePathForPrompt(Path sourceImagePath) {
        return sourceImagePath.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
