package cn.iocoder.yudao.module.showroom.cover;

import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomProductCoverImageServiceTest {

    private static final byte[] ONE_PIXEL_PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WnXl1QAAAAASUVORK5CYII=");

    @TempDir
    Path tempDir;

    @Test
    void generateCoverImageShouldReturnProxyFileUrlAfterUpload() throws Exception {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);

        Path generatedImage = tempDir.resolve("generated-cover.png");
        Files.write(generatedImage, ONE_PIXEL_PNG_BYTES);
        Path command = writeFakeCodexCommand("fake-codex-cover.cmd", """
                @echo off
                setlocal EnableDelayedExpansion
                > "%~dp0args.txt" echo %*
                set "OUT="
                :parse
                if "%~1"=="" goto afterArgs
                if "%~1"=="-o" (
                  set "OUT=%~2"
                  shift
                )
                if "%~1"=="--output-last-message" (
                  set "OUT=%~2"
                  shift
                )
                shift
                goto parse
                :afterArgs
                if "%OUT%"=="" exit /b 9
                > "%OUT%" echo __IMAGE_PATH__
                exit /b 0
                """.replace("__IMAGE_PATH__", generatedImage.toString()));
        YudaoAiProperties.CodexCli codexCli = new YudaoAiProperties.CodexCli();
        codexCli.setCommand(command.toString());
        codexCli.setModel("gpt-5.4");
        codexCli.setTimeoutMs(5000L);
        codexCli.setWorkingDirectory("D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro");
        when(properties.getCodexCli()).thenReturn(codexCli);
        when(fileService.createFileAndReturnId(eq(ONE_PIXEL_PNG_BYTES), contains("product-PRODUCT-001-cover"),
                eq("showroom/product/cover"), eq("image/png")))
                .thenReturn(99129L);
        when(fileService.getFile(99129L)).thenReturn(FileDO.builder()
                .id(99129L)
                .configId(29L)
                .path("showroom/product/cover/generated-cover.png")
                .url("http://127.0.0.1:9000/yudao/showroom/product/cover/generated-cover.png")
                .type("image/png")
                .build());

        String result = service.generateCoverImage("PRODUCT-001", "rendered cover prompt text");

        assertEquals("/admin-api/infra/file/29/get/showroom/product/cover/generated-cover.png", result);
        String args = Files.readString(tempDir.resolve("args.txt"), StandardCharsets.UTF_8);
        assertTrue(args.contains("exec"));
        assertTrue(args.contains("--ephemeral"));
        assertTrue(args.contains("--output-last-message"));
        assertTrue(args.contains("-m gpt-5.4"));
        assertTrue(args.contains("-C D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro"));
    }

    @Test
    void uploadImportedCoverImageShouldUploadBytesAndReturnProxyFileUrl() {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);
        when(fileService.createFileAndReturnId(eq(ONE_PIXEL_PNG_BYTES),
                contains("product-PRODUCT-001-imported-cover-"),
                eq("showroom/product/cover"), eq("image/png")))
                .thenReturn(88L);
        when(fileService.getFile(88L)).thenReturn(FileDO.builder()
                .id(88L)
                .configId(29L)
                .path("showroom/product/cover/imported-cover-a79e1c2d3f4b.png")
                .url("http://127.0.0.1:9000/yudao/showroom/product/cover/imported-cover.png")
                .type("image/png")
                .build());

        String result = service.uploadImportedCoverImage("PRODUCT-001", ONE_PIXEL_PNG_BYTES, "png", "image/png");

        assertEquals("/admin-api/infra/file/29/get/showroom/product/cover/imported-cover-a79e1c2d3f4b.png", result);
    }

    @Test
    void uploadImportedCoverImageShouldUseDifferentFileNamesForDifferentImageBytes() {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);
        when(fileService.createFileAndReturnId(any(byte[].class), anyString(),
                eq("showroom/product/cover"), eq("image/png")))
                .thenReturn(88L, 89L);
        when(fileService.getFile(88L)).thenReturn(FileDO.builder()
                .id(88L)
                .configId(29L)
                .path("showroom/product/cover/imported-cover-first.png")
                .type("image/png")
                .build());
        when(fileService.getFile(89L)).thenReturn(FileDO.builder()
                .id(89L)
                .configId(29L)
                .path("showroom/product/cover/imported-cover-second.png")
                .type("image/png")
                .build());

        service.uploadImportedCoverImage("PRODUCT-001", ONE_PIXEL_PNG_BYTES, "png", "image/png");
        service.uploadImportedCoverImage("PRODUCT-001", "different-image".getBytes(StandardCharsets.UTF_8),
                "png", "image/png");

        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileService, times(2)).createFileAndReturnId(any(byte[].class), fileNameCaptor.capture(),
                eq("showroom/product/cover"), eq("image/png"));
        assertTrue(fileNameCaptor.getAllValues().get(0)
                .matches("product-PRODUCT-001-imported-cover-[0-9a-f]{16}\\.png"));
        assertTrue(fileNameCaptor.getAllValues().get(1)
                .matches("product-PRODUCT-001-imported-cover-[0-9a-f]{16}\\.png"));
        assertNotEquals(fileNameCaptor.getAllValues().get(0), fileNameCaptor.getAllValues().get(1));
    }

    @Test
    void importedCoverImageMatchesCurrentCoverShouldCompareAdminFileUrlContent() throws Exception {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);
        when(fileService.getFileContent(28L, "showroom/product/cover/current.png"))
                .thenReturn(ONE_PIXEL_PNG_BYTES);

        boolean result = service.importedCoverImageMatchesCurrentCover(
                "https://example.com/admin-api/infra/file/28/get/showroom/product/cover/current.png",
                ONE_PIXEL_PNG_BYTES);

        assertTrue(result);
    }

    @Test
    void importedCoverImageMatchesCurrentCoverShouldReturnFalseForDifferentOrExternalCover() throws Exception {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);
        when(fileService.getFileContent(28L, "showroom/product/cover/current.png"))
                .thenReturn(ONE_PIXEL_PNG_BYTES);

        assertFalse(service.importedCoverImageMatchesCurrentCover(
                "/admin-api/infra/file/28/get/showroom/product/cover/current.png",
                "different-image".getBytes(StandardCharsets.UTF_8)));
        assertFalse(service.importedCoverImageMatchesCurrentCover(
                "https://cdn.example.com/showroom/product/cover/current.png",
                ONE_PIXEL_PNG_BYTES));
    }

    @Test
    void importedCoverImageUrlMatchesContentHashShouldRequireHashBasedImportedCoverUrl() {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);

        assertTrue(service.importedCoverImageUrlMatchesContentHash(
                "/admin-api/infra/file/28/get/showroom/product/cover/"
                        + "product-PRODUCT-001-imported-cover-3ca21777eac7dcac.png",
                ONE_PIXEL_PNG_BYTES));
        assertFalse(service.importedCoverImageUrlMatchesContentHash(
                "/admin-api/infra/file/28/get/showroom/product/cover/product-PRODUCT-001-imported-cover.png",
                ONE_PIXEL_PNG_BYTES));
        assertFalse(service.importedCoverImageUrlMatchesContentHash(
                "/admin-api/infra/file/28/get/showroom/product/cover/current.png",
                ONE_PIXEL_PNG_BYTES));
    }

    @Test
    void importedCoverImageMatchesCurrentCoverShouldFailFastWhenCurrentCoverFileMissing() throws Exception {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);
        when(fileService.getFileContent(28L, "showroom/product/cover/missing.png"))
                .thenReturn(null);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.importedCoverImageMatchesCurrentCover(
                        "/admin-api/infra/file/28/get/showroom/product/cover/missing.png",
                        ONE_PIXEL_PNG_BYTES));

        assertTrue(error.getMessage().contains("SHOWROOM_COVER_GENERATION_FAILED"));
        assertTrue(error.getMessage().contains("current product cover image is empty"));
    }

    @Test
    void resolveCodexCommandShouldDefaultToLocalExecutableWhenConfigMissing() throws Exception {
        var method = ShowroomProductCoverImageService.class
                .getDeclaredMethod("resolveCodexCommand", YudaoAiProperties.CodexCli.class);
        method.setAccessible(true);

        String command = (String) method.invoke(null, new Object[]{null});

        String osName = System.getProperty("os.name", "").toLowerCase();
        assertEquals(osName.contains("win") ? "codex.cmd" : "codex", command);
    }

    @Test
    void resolveBatchParallelismShouldDefaultToEightCapLargerValuesAndFailFastOnNonPositiveValue() {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);

        when(properties.getCodexCli()).thenReturn(null);
        assertEquals(8, service.resolveBatchParallelism());

        YudaoAiProperties.CodexCli largerParallelism = new YudaoAiProperties.CodexCli() {
            public Integer getParallelism() {
                return 16;
            }
        };
        when(properties.getCodexCli()).thenReturn(largerParallelism);
        assertEquals(8, service.resolveBatchParallelism());

        YudaoAiProperties.CodexCli codexCli = new YudaoAiProperties.CodexCli() {
            public Integer getParallelism() {
                return 0;
            }
        };
        when(properties.getCodexCli()).thenReturn(codexCli);

        IllegalStateException error = assertThrows(IllegalStateException.class, service::resolveBatchParallelism);
        assertTrue(error.getMessage().contains("parallelism must be greater than 0"));
    }

    @Test
    void generateCoverImageShouldFailFastWhenCodexCliReturnsMissingFile() throws Exception {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);

        Path command = writeFakeCodexCommand("fake-codex-missing.cmd", """
                @echo off
                setlocal EnableDelayedExpansion
                set "OUT="
                :parse
                if "%~1"=="" goto afterArgs
                if "%~1"=="-o" (
                  set "OUT=%~2"
                  shift
                )
                if "%~1"=="--output-last-message" (
                  set "OUT=%~2"
                  shift
                )
                shift
                goto parse
                :afterArgs
                more > nul
                if "%OUT%"=="" exit /b 9
                > "%OUT%" echo __MISSING_PATH__
                exit /b 0
                """.replace("__MISSING_PATH__", tempDir.resolve("missing-cover.png").toString()));
        YudaoAiProperties.CodexCli codexCli = new YudaoAiProperties.CodexCli();
        codexCli.setCommand(command.toString());
        codexCli.setTimeoutMs(5000L);
        when(properties.getCodexCli()).thenReturn(codexCli);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.generateCoverImage("PRODUCT-001", "rendered cover prompt text"));

        assertTrue(error.getMessage().contains("SHOWROOM_COVER_GENERATION_FAILED"));
        assertTrue(error.getMessage().contains("generated product cover file does not exist"));
    }

    @Test
    void generateCoverImageShouldExposeReadableUpstreamErrorWhenOutputMessageIsNotAPath() throws Exception {
        FileService fileService = mock(FileService.class);
        YudaoAiProperties properties = mock(YudaoAiProperties.class);
        ShowroomProductCoverImageService service = new ShowroomProductCoverImageService(fileService, properties);

        Path command = writeFakeCodexCommand("fake-codex-error-text.cmd", """
                @echo off
                setlocal EnableDelayedExpansion
                set "OUT="
                :parse
                if "%~1"=="" goto afterArgs
                if "%~1"=="-o" (
                  set "OUT=%~2"
                  shift
                )
                if "%~1"=="--output-last-message" (
                  set "OUT=%~2"
                  shift
                )
                shift
                goto parse
                :afterArgs
                more > nul
                if "%OUT%"=="" exit /b 9
                > "%OUT%" echo Generation failed: the single native `image_generation` request returned `503 Service temporarily unavailable`
                exit /b 0
                """);
        YudaoAiProperties.CodexCli codexCli = new YudaoAiProperties.CodexCli();
        codexCli.setCommand(command.toString());
        codexCli.setTimeoutMs(5000L);
        when(properties.getCodexCli()).thenReturn(codexCli);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.generateCoverImage("PRODUCT-503", "rendered cover prompt text"));

        assertTrue(error.getMessage().contains(
                "Generation failed: the single native `image_generation` request returned `503 Service temporarily unavailable`"));
        assertFalse(error.getMessage().contains("Illegal char"));
    }

    private Path writeFakeCodexCommand(String fileName, String scriptBody) throws IOException {
        Path command = tempDir.resolve(fileName);
        Files.writeString(command, scriptBody, StandardCharsets.UTF_8);
        return command;
    }
}
