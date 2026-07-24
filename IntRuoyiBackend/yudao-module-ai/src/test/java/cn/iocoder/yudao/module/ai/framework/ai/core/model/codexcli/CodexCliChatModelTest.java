package cn.iocoder.yudao.module.ai.framework.ai.core.model.codexcli;

import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodexCliChatModelTest {

    @Test
    void buildPromptTextShouldIncludeAllMessageRoles() {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage("system instruction"),
                new UserMessage("latest user request"),
                new AssistantMessage("previous answer")
        ));

        String promptText = CodexCliChatModel.buildPromptText(prompt);

        assertTrue(promptText.contains("SYSTEM:"));
        assertTrue(promptText.contains("USER:"));
        assertTrue(promptText.contains("ASSISTANT:"));
        assertTrue(promptText.contains("system instruction"));
        assertTrue(promptText.contains("latest user request"));
        assertTrue(promptText.contains("previous answer"));
    }

    @Test
    void callShouldHonorTimeoutEvenWhenCliKeepsStdoutOpen() throws Exception {
        Path tempDir = Files.createTempDirectory("codex-cli-test-");
        try {
            YudaoAiProperties.CodexCli properties = new YudaoAiProperties.CodexCli();
            properties.setCommand(writeSlowFakeCodexCommand(tempDir).toString());
            properties.setTimeoutMs(200L);
            properties.setWorkingDirectory(tempDir.toString());
            CodexCliChatModel model = new CodexCliChatModel(properties);
            Prompt prompt = new Prompt(List.of(new UserMessage("latest user request")));

            IllegalStateException error = assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                    assertThrows(IllegalStateException.class, () -> model.call(prompt)));

            assertTrue(error.getMessage().contains("timed out after 200 ms"));
        } finally {
            deleteDirectoryQuietly(tempDir);
        }
    }

    @Test
    void callShouldInvokeCliWithEphemeralFlag() throws Exception {
        Path tempDir = Files.createTempDirectory("codex-cli-ephemeral-");
        try {
            YudaoAiProperties.CodexCli properties = new YudaoAiProperties.CodexCli();
            properties.setCommand(writeEphemeralRequiredFakeCodexCommand(tempDir).toString());
            properties.setTimeoutMs(1000L);
            properties.setWorkingDirectory(tempDir.toString());
            CodexCliChatModel model = new CodexCliChatModel(properties);
            Prompt prompt = new Prompt(List.of(new UserMessage("latest user request")));

            String content = model.call(prompt).getResult().getOutput().getText();

            assertEquals("ephemeral-ok", content);
        } finally {
            deleteDirectoryQuietly(tempDir);
        }
    }

    private Path writeSlowFakeCodexCommand(Path tempDir) throws IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path command = tempDir.resolve(windows ? "fake-codex-timeout.cmd" : "fake-codex-timeout.sh");
        String scriptBody = windows
                ? """
                @echo off
                setlocal EnableDelayedExpansion
                set "OUT="
                :parse
                if "%~1"=="" goto afterArgs
                if "%~1"=="--output-last-message" (
                  set "OUT=%~2"
                  shift
                )
                shift
                goto parse
                :afterArgs
                if "%OUT%"=="" exit /b 9
                echo slow-cli-start
                ping -n 4 127.0.0.1 > nul
                > "%OUT%" echo finished-too-late
                exit /b 0
                """
                : """
                #!/usr/bin/env bash
                set -euo pipefail
                OUT=""
                while [[ $# -gt 0 ]]; do
                  if [[ "$1" == "--output-last-message" ]]; then
                    OUT="$2"
                    shift 2
                    continue
                  fi
                  shift
                done
                if [[ -z "$OUT" ]]; then
                  exit 9
                fi
                echo slow-cli-start
                sleep 3
                printf 'finished-too-late' > "$OUT"
                exit 0
                """;
        Files.writeString(command, scriptBody, StandardCharsets.UTF_8);
        if (!windows) {
            command.toFile().setExecutable(true);
        }
        return command;
    }

    private Path writeEphemeralRequiredFakeCodexCommand(Path tempDir) throws IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path command = tempDir.resolve(windows ? "fake-codex-ephemeral.cmd" : "fake-codex-ephemeral.sh");
        String scriptBody = windows
                ? """
                @echo off
                setlocal EnableDelayedExpansion
                set "OUT="
                set "HAS_EPHEMERAL=0"
                :parse
                if "%~1"=="" goto afterArgs
                if "%~1"=="--ephemeral" set "HAS_EPHEMERAL=1"
                if "%~1"=="--output-last-message" (
                  set "OUT=%~2"
                  shift
                )
                shift
                goto parse
                :afterArgs
                if "%HAS_EPHEMERAL%"=="0" (
                  echo missing-ephemeral
                  exit /b 17
                )
                if "%OUT%"=="" exit /b 9
                > "%OUT%" echo ephemeral-ok
                exit /b 0
                """
                : """
                #!/usr/bin/env bash
                set -euo pipefail
                OUT=""
                HAS_EPHEMERAL=0
                while [[ $# -gt 0 ]]; do
                  if [[ "$1" == "--ephemeral" ]]; then
                    HAS_EPHEMERAL=1
                  fi
                  if [[ "$1" == "--output-last-message" ]]; then
                    OUT="$2"
                    shift 2
                    continue
                  fi
                  shift
                done
                if [[ "$HAS_EPHEMERAL" != "1" ]]; then
                  echo missing-ephemeral
                  exit 17
                fi
                if [[ -z "$OUT" ]]; then
                  exit 9
                fi
                printf 'ephemeral-ok' > "$OUT"
                exit 0
                """;
        Files.writeString(command, scriptBody, StandardCharsets.UTF_8);
        if (!windows) {
            command.toFile().setExecutable(true);
        }
        return command;
    }

    private void deleteDirectoryQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(target -> {
                        try {
                            Files.deleteIfExists(target);
                        } catch (IOException ignored) {
                            // Best effort cleanup only; never fail the regression because Windows is releasing handles.
                        }
                    });
        } catch (IOException ignored) {
            // Best effort cleanup only.
        }
    }

}
