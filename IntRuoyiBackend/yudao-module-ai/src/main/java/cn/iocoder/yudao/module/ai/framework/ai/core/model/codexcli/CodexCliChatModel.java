package cn.iocoder.yudao.module.ai.framework.ai.core.model.codexcli;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ChatModel backed by the local Codex CLI.
 */
@Slf4j
public class CodexCliChatModel implements ChatModel {

    public static final String MODEL_DEFAULT = "codex-cli";
    private static final long TIMEOUT_DEFAULT_MS = 240000L;

    private final String command;
    private final Long timeoutMs;
    private final String workingDirectory;
    private final String model;
    private final ChatOptions defaultOptions;

    public CodexCliChatModel(YudaoAiProperties.CodexCli properties) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        this.command = properties != null && StrUtil.isNotBlank(properties.getCommand())
                ? properties.getCommand()
                : (osName.contains("win") ? "codex.cmd" : "codex");
        this.timeoutMs = properties != null && properties.getTimeoutMs() != null
                ? properties.getTimeoutMs()
                : TIMEOUT_DEFAULT_MS;
        this.workingDirectory = properties != null && StrUtil.isNotBlank(properties.getWorkingDirectory())
                ? properties.getWorkingDirectory()
                : System.getProperty("user.dir");
        this.model = properties != null ? properties.getModel() : null;
        this.defaultOptions = OpenAiChatOptions.builder().model(MODEL_DEFAULT).build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        String content = executePrompt(buildPromptText(prompt));
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return defaultOptions;
    }

    static String buildPromptText(Prompt prompt) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are the local Codex CLI backend for an enterprise admin application.\n");
        builder.append("Answer the latest user request directly in plain text unless the request explicitly asks for another format.\n\n");
        for (Message message : prompt.getInstructions()) {
            builder.append(resolveRole(message)).append(":\n");
            builder.append(StrUtil.nullToEmpty(message.getText()).trim()).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String executePrompt(String promptText) {
        Path outputFile = null;
        Path stdoutFile = null;
        try {
            outputFile = Files.createTempFile("codex-cli-output-", ".txt");
            stdoutFile = Files.createTempFile("codex-cli-stdout-", ".log");
            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(outputFile));
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(stdoutFile.toFile());
            if (StrUtil.isNotBlank(workingDirectory)) {
                processBuilder.directory(Path.of(workingDirectory).toFile());
            }
            Process process = processBuilder.start();
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(promptText);
            }

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                throw new IllegalStateException("Local codex cli timed out after " + timeoutMs + " ms, stdout: "
                        + trimForError(readTextSafely(stdoutFile)));
            }
            String stdout = readTextSafely(stdoutFile);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Local codex cli failed with exit code " + process.exitValue()
                        + ", stdout: " + trimForError(stdout));
            }

            String content = Files.exists(outputFile)
                    ? Files.readString(outputFile, StandardCharsets.UTF_8).trim()
                    : "";
            if (content.isEmpty()) {
                throw new IllegalStateException("Local codex cli returned blank output, stdout: " + trimForError(stdout));
            }
            return content;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute local codex cli command `" + command + "`", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for local codex cli", e);
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

    private List<String> buildCommand(Path outputFile) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(command);
        commandLine.add("exec");
        commandLine.add("-");
        commandLine.add("--skip-git-repo-check");
        commandLine.add("--dangerously-bypass-approvals-and-sandbox");
        commandLine.add("--ephemeral");
        commandLine.add("--output-last-message");
        commandLine.add(outputFile.toString());
        if (StrUtil.isNotBlank(model)) {
            commandLine.add("-m");
            commandLine.add(model);
        }
        if (StrUtil.isNotBlank(workingDirectory)) {
            commandLine.add("-C");
            commandLine.add(workingDirectory);
        }
        return commandLine;
    }

    private static String resolveRole(Message message) {
        if (message.getMessageType() == MessageType.SYSTEM) {
            return "SYSTEM";
        }
        if (message.getMessageType() == MessageType.ASSISTANT) {
            return "ASSISTANT";
        }
        if (message.getMessageType() == MessageType.TOOL) {
            return "TOOL";
        }
        return "USER";
    }

    private static String trimForError(String stdout) {
        return StrUtil.maxLength(StrUtil.blankToDefault(stdout, ""), 4000);
    }

    private static String readTextSafely(Path path) {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

}
