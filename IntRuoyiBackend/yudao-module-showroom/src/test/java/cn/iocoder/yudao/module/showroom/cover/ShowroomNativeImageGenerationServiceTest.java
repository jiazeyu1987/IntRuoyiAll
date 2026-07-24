package cn.iocoder.yudao.module.showroom.cover;

import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShowroomNativeImageGenerationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generatePngShouldReadOutputPathWithoutBlockingOnStdoutPipe() throws Exception {
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        YudaoAiProperties.CodexCli codexCli = new YudaoAiProperties.CodexCli();
        codexCli.setCommand(writeFakeCodexCommand("fake-codex-success.cmd", """
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
                echo generating showroom cover
                > "%OUT%" echo __PNG_PATH__
                exit /b 0
                """.replace("__PNG_PATH__", tempDir.resolve("generated-cover.png").toString())).toString());
        codexCli.setTimeoutMs(5000L);
        when(properties.getCodexCli()).thenReturn(codexCli);
        Files.write(tempDir.resolve("generated-cover.png"), new byte[] {1, 2, 3});

        ShowroomNativeImageGenerationService service = new ShowroomNativeImageGenerationService(properties);

        Path generated = service.generatePng("render image", "SHOWROOM_TEST_FAILED", "award cover");

        assertEquals(tempDir.resolve("generated-cover.png"), generated);
    }

    @Test
    void generatePngShouldFailFastWhenCodexCliExceedsTimeout() throws Exception {
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        YudaoAiProperties.CodexCli codexCli = new YudaoAiProperties.CodexCli();
        codexCli.setCommand(writeFakeCodexCommand("fake-codex-timeout.cmd", """
                @echo off
                ping 127.0.0.1 -n 6 > nul
                exit /b 0
                """).toString());
        codexCli.setTimeoutMs(300L);
        when(properties.getCodexCli()).thenReturn(codexCli);

        ShowroomNativeImageGenerationService service = new ShowroomNativeImageGenerationService(properties);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.generatePng("render image", "SHOWROOM_TEST_FAILED", "award cover"));

        assertTrue(error.getMessage().contains("local codex cli timed out"));
    }

    @Test
    void generatePngShouldReturnAsSoonAsReadyPngPathExistsEvenIfProcessKeepsRunning() throws Exception {
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        Path generated = tempDir.resolve("ready-before-exit.png");
        Files.write(generated, new byte[] {7, 8, 9});
        YudaoAiProperties.CodexCli codexCli = new YudaoAiProperties.CodexCli();
        codexCli.setCommand(writeFakeCodexCommand("fake-codex-ready-then-hang.cmd", """
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
                > "%OUT%" echo __PNG_PATH__
                ping 127.0.0.1 -n 30 > nul
                exit /b 0
                """.replace("__PNG_PATH__", generated.toString())).toString());
        codexCli.setTimeoutMs(4000L);
        when(properties.getCodexCli()).thenReturn(codexCli);

        ShowroomNativeImageGenerationService service = new ShowroomNativeImageGenerationService(properties);

        Path result = service.generatePng("render image", "SHOWROOM_TEST_FAILED", "award cover");

        assertEquals(generated, result);
    }

    @Test
    void generatePngShouldFailFastWhenCodexCliUsesForbiddenFallbackPath() throws Exception {
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        Path generated = tempDir.resolve("fallback-produced.png");
        Files.write(generated, new byte[] {4, 5, 6});
        YudaoAiProperties.CodexCli codexCli = new YudaoAiProperties.CodexCli();
        codexCli.setCommand(writeFakeCodexCommand("fake-codex-fallback.cmd", """
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
                echo Running image_gen.py fallback
                echo Applying a non-generative enhancement
                > "%OUT%" echo __PNG_PATH__
                exit /b 0
                """.replace("__PNG_PATH__", generated.toString())).toString());
        codexCli.setTimeoutMs(5000L);
        when(properties.getCodexCli()).thenReturn(codexCli);

        ShowroomNativeImageGenerationService service = new ShowroomNativeImageGenerationService(properties);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.generatePng("render image", "SHOWROOM_TEST_FAILED", "award cover"));

        assertTrue(error.getMessage().contains("forbidden fallback path"));
        assertTrue(error.getMessage().contains("image_gen.py"));
    }

    @Test
    void applyOpenAiEnvironmentShouldPropagateConfiguredImageApiCredentials() {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "echo");
        TestCodexCli codexCli = new TestCodexCli();
        codexCli.setOpenAiApiKey(" sk-test-image-key ");
        codexCli.setOpenAiBaseUrl(" https://api.example.com/v1 ");

        ShowroomNativeImageGenerationService.applyOpenAiEnvironment(processBuilder, codexCli);

        Map<String, String> environment = processBuilder.environment();
        assertEquals("sk-test-image-key", environment.get("OPENAI_API_KEY"));
        assertEquals("https://api.example.com/v1", environment.get("OPENAI_BASE_URL"));
    }

    @Test
    void applyOpenAiEnvironmentShouldLeaveProcessUntouchedWhenImageApiConfigMissing() {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "echo");
        TestCodexCli codexCli = new TestCodexCli();
        String originalApiKey = processBuilder.environment().get("OPENAI_API_KEY");
        String originalBaseUrl = processBuilder.environment().get("OPENAI_BASE_URL");

        ShowroomNativeImageGenerationService.applyOpenAiEnvironment(processBuilder, codexCli);

        assertEquals(originalApiKey, processBuilder.environment().get("OPENAI_API_KEY"));
        assertEquals(originalBaseUrl, processBuilder.environment().get("OPENAI_BASE_URL"));
    }

    @Test
    void buildPromptShouldForceGenerateAiSceneImageSkillAndBanFallbackPaths() throws Exception {
        var method = ShowroomNativeImageGenerationService.class
                .getDeclaredMethod("buildPrompt", String.class, Path.class);
        method.setAccessible(true);
        Path sourceImage = tempDir.resolve("award-source.png").toAbsolutePath();
        Files.write(sourceImage, new byte[] {1, 1, 1});

        String prompt = (String) method.invoke(null, "render image", sourceImage);

        assertTrue(prompt.contains("$generate-ai-scene-image"));
        assertTrue(prompt.contains("Do not use imagegen"));
        assertTrue(prompt.contains("If the built-in image generation tool cannot complete, fail explicitly"));
    }

    private Path writeFakeCodexCommand(String fileName, String scriptBody) throws IOException {
        Path command = tempDir.resolve(fileName);
        Files.writeString(command, scriptBody, StandardCharsets.UTF_8);
        return command;
    }

    private static final class TestCodexCli extends YudaoAiProperties.CodexCli {

        private String openAiApiKey;
        private String openAiBaseUrl;

        public String getOpenAiApiKey() {
            return openAiApiKey;
        }

        public TestCodexCli setOpenAiApiKey(String openAiApiKey) {
            this.openAiApiKey = openAiApiKey;
            return this;
        }

        public String getOpenAiBaseUrl() {
            return openAiBaseUrl;
        }

        public TestCodexCli setOpenAiBaseUrl(String openAiBaseUrl) {
            this.openAiBaseUrl = openAiBaseUrl;
            return this;
        }
    }
}
