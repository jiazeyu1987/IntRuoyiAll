package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED;

@Service
@Slf4j
public class DccProjectCodeCodexCliClientImpl implements DccProjectCodeRecognitionCodexCliClient {

    private static final int OUTPUT_WAIT_SECONDS = 5;
    private static final int MAX_ERROR_DETAIL_LENGTH = 1000;

    private final DccProjectCodeRecognitionProperties properties;

    public DccProjectCodeCodexCliClientImpl(DccProjectCodeRecognitionProperties properties) {
        this.properties = properties;
    }

    @Override
    public DccProjectCodeRecognitionResult recognizeProjectCode(DccProjectCodeRecognitionCommand command) {
        List<String> codexCliCommand = normalizeRequiredCommand();
        int timeoutSeconds = normalizeTimeoutSeconds();

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("dcc-project-code-recognition-");
            String sourceFileName = safeFileName(command.sourceFileName());
            Files.write(tempDir.resolve(sourceFileName), command.sourceContent());
            Files.writeString(tempDir.resolve("dcc-project-code-candidates.json"),
                    JsonUtils.toJsonString(command.candidates()), StandardCharsets.UTF_8);
            Path outputFile = tempDir.resolve("codex-project-code-output.json");

            Process process = startCodexProcess(codexCliCommand, tempDir, outputFile);
            CompletableFuture<String> stdoutFuture = readAsync(process.getInputStream());
            CompletableFuture<String> stderrFuture = readAsync(process.getErrorStream());
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(buildPrompt(command, sourceFileName).getBytes(StandardCharsets.UTF_8));
            }

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                        "Codex CLI timed out after " + timeoutSeconds + " seconds");
            }
            String stdout = readFuture(stdoutFuture, "stdout");
            String stderr = readFuture(stderrFuture, "stderr");
            if (process.exitValue() != 0) {
                if (Files.exists(outputFile)) {
                    return parseProjectCodeRecognition(Files.readString(outputFile, StandardCharsets.UTF_8));
                }
                throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                        "Codex CLI exited with code " + process.exitValue() + detailSuffix(stdout, stderr));
            }
            if (!Files.exists(outputFile)) {
                throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                        "Codex CLI did not create output-last-message file");
            }
            return parseProjectCodeRecognition(Files.readString(outputFile, StandardCharsets.UTF_8));
        } catch (ServiceException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "interrupted while waiting for Codex CLI");
        } catch (IOException ex) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "Codex CLI file operation failed: "
                            + StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        } finally {
            deleteTempDirectory(tempDir);
        }
    }

    private List<String> normalizeRequiredCommand() {
        String command = StrUtil.trimToNull(properties.getCodexCliCommand());
        if (command == null) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING,
                    "codex-cli-command is required");
        }
        if (command.startsWith("\"") && command.endsWith("\"") && command.length() > 1) {
            command = command.substring(1, command.length() - 1);
        }
        return splitCommandLine(command);
    }

    private int normalizeTimeoutSeconds() {
        Integer configuredTimeoutSeconds = properties.getTimeoutSeconds();
        if (configuredTimeoutSeconds == null || configuredTimeoutSeconds <= 0) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING,
                    "timeout-seconds must be greater than 0");
        }
        return configuredTimeoutSeconds;
    }

    private Process startCodexProcess(List<String> codexCliCommand, Path tempDir, Path outputFile) {
        List<String> commandLine = new ArrayList<>(codexCliCommand);
        commandLine.addAll(List.of(
                "--ask-for-approval",
                "never",
                "exec",
                "--skip-git-repo-check",
                "--ephemeral",
                "--ignore-rules",
                "--sandbox",
                "read-only",
                "--output-last-message",
                outputFile.toString(),
                "-C",
                tempDir.toString(),
                "-"));
        try {
            return new ProcessBuilder(commandLine)
                    .directory(tempDir.toFile())
                    .start();
        } catch (IOException ex) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "failed to start Codex CLI command " + codexCliCommand + ": "
                            + StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private String buildPrompt(DccProjectCodeRecognitionCommand command, String sourceFileName) {
        return """
                你是 DCC 基础信息识别助手。请读取当前目录下的源文件，并且只能从 dcc-project-code-candidates.json 中选择一个启用的 DCC 基础数据候选。

                源文件上下文：%s
                工作目录中的实际文件名：%s
                文件 MIME 类型：%s
                受控文件 ID：%s
                候选文件：dcc-project-code-candidates.json

                要求：
                1. 优先根据源文件内容判断；如果内容不足，但源文件名中存在可验证的项目编码或项目名称，也可以结合源文件名判断。
                2. 当源文件内容是通用检验记录、附录或模板时，目录路径也属于有效证据。
                3. 只能返回候选文件中已经存在的 id，禁止自由生成产品名称或编码。
                4. 如果通过项目名称识别，matchType 必须为 PROJECT_NAME，matchText 必须等于候选 projectName。
                5. 如果通过项目代码识别，matchType 必须为 PROJECT_CODE，matchText 必须等于候选 projectCode。
                6. 无法确认时输出 {"projectCodeId":null,"matchType":null,"matchText":""}。
                7. 只输出严格 JSON，格式必须是 {"projectCodeId":123,"matchType":"PROJECT_NAME","matchText":"..."}。
                8. 禁止输出 Markdown、代码块、解释文字或额外字段。
                """.formatted(StrUtil.blankToDefault(command.sourceFileName(), sourceFileName),
                sourceFileName,
                StrUtil.blankToDefault(command.contentType(), "-"),
                command.controlledFileId());
    }

    private DccProjectCodeRecognitionResult parseProjectCodeRecognition(String output) {
        ProjectCodePayload payload;
        try {
            payload = JsonUtils.parseObject(StrUtil.trim(output), ProjectCodePayload.class);
        } catch (RuntimeException ex) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "Codex CLI returned non-JSON project-code payload");
        }
        if (payload == null) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "Codex CLI returned empty output");
        }
        if (payload.getProjectCodeId() == null || StrUtil.isBlank(payload.getMatchType())) {
            return null;
        }
        DccProjectCodeRecognitionMatchType matchType;
        try {
            matchType = DccProjectCodeRecognitionMatchType.valueOf(StrUtil.trim(payload.getMatchType()));
        } catch (IllegalArgumentException ex) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "Codex CLI returned unsupported matchType: " + payload.getMatchType());
        }
        return new DccProjectCodeRecognitionResult(
                payload.getProjectCodeId(),
                matchType,
                StrUtil.trim(payload.getMatchText()));
    }

    private CompletableFuture<String> readAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream stream = inputStream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    private String readFuture(CompletableFuture<String> future, String streamName) {
        try {
            return future.get(OUTPUT_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "interrupted while reading Codex CLI " + streamName);
        } catch (ExecutionException ex) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "failed to read Codex CLI " + streamName + ": "
                            + StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        } catch (TimeoutException ex) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "timed out while reading Codex CLI " + streamName);
        }
    }

    private String detailSuffix(String stdout, String stderr) {
        String detail = StrUtil.trimToEmpty(stderr);
        if (StrUtil.isBlank(detail)) {
            detail = StrUtil.trimToEmpty(stdout);
        }
        if (StrUtil.isBlank(detail)) {
            return "";
        }
        return ": " + abbreviate(detail.replaceAll("\\s+", " "));
    }

    private String abbreviate(String value) {
        if (value.length() <= MAX_ERROR_DETAIL_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_DETAIL_LENGTH) + "...";
    }

    private String safeFileName(String fileName) {
        String resolved = StrUtil.blankToDefault(fileName, "source-file");
        resolved = resolved.replace('\\', '/');
        int lastSeparator = resolved.lastIndexOf('/');
        if (lastSeparator >= 0) {
            resolved = resolved.substring(lastSeparator + 1);
        }
        resolved = resolved.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_");
        return StrUtil.blankToDefault(StrUtil.trim(resolved), "source-file");
    }

    private void deleteTempDirectory(Path tempDir) {
        if (tempDir == null || !Files.exists(tempDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(tempDir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            log.warn("[deleteTempDirectory][tempDir({}) cleanup failed]", tempDir,
                    ex);
        }
    }

    private List<String> splitCommandLine(String commandLine) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < commandLine.length(); i++) {
            char ch = commandLine.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (Character.isWhitespace(ch) && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        if (parts.isEmpty()) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING,
                    "codex-cli-command is required");
        }
        return parts;
    }

    private static final class ProjectCodePayload {
        private Long projectCodeId;
        private String matchType;
        private String matchText;

        public Long getProjectCodeId() {
            return projectCodeId;
        }

        public void setProjectCodeId(Long projectCodeId) {
            this.projectCodeId = projectCodeId;
        }

        public String getMatchType() {
            return matchType;
        }

        public void setMatchType(String matchType) {
            this.matchType = matchType;
        }

        public String getMatchText() {
            return matchText;
        }

        public void setMatchText(String matchText) {
            this.matchText = matchText;
        }
    }
}
