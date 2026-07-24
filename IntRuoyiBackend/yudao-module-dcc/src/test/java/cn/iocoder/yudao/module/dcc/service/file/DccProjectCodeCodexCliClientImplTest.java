package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccProjectCodeCodexCliClientImplTest {

    @Test
    void recognizeProjectCode_missingCodexCommandFailsFast() {
        DccProjectCodeRecognitionProperties properties = new DccProjectCodeRecognitionProperties();
        properties.setCodexCliCommand(" ");
        DccProjectCodeCodexCliClientImpl client = new DccProjectCodeCodexCliClientImpl(properties);

        assertServiceException(() -> client.recognizeProjectCode(command()),
                CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING,
                "codex-cli-command is required");
    }

    @Test
    void recognizeProjectCode_passesApprovalPolicyAndParsesStrictJson() {
        DccProjectCodeRecognitionProperties properties = new DccProjectCodeRecognitionProperties();
        properties.setCodexCliCommand(fakeCodexCliCommand(""));
        properties.setTimeoutSeconds(30);
        DccProjectCodeCodexCliClientImpl client = new DccProjectCodeCodexCliClientImpl(properties);

        DccProjectCodeRecognitionResult result = client.recognizeProjectCode(command());

        assertEquals(700L, result.projectCodeId());
        assertEquals(DccProjectCodeRecognitionMatchType.PROJECT_CODE, result.matchType());
        assertEquals("CODE-A", result.matchText());
    }

    @Test
    void recognizeProjectCode_usesStrictOutputWhenCliExitsNonZeroAfterWritingResult() {
        DccProjectCodeRecognitionProperties properties = new DccProjectCodeRecognitionProperties();
        properties.setCodexCliCommand(fakeCodexCliCommand(" exit-nonzero-after-output"));
        properties.setTimeoutSeconds(30);
        DccProjectCodeCodexCliClientImpl client = new DccProjectCodeCodexCliClientImpl(properties);

        DccProjectCodeRecognitionResult result = client.recognizeProjectCode(command());

        assertEquals(700L, result.projectCodeId());
        assertEquals(DccProjectCodeRecognitionMatchType.PROJECT_CODE, result.matchType());
        assertEquals("CODE-A", result.matchText());
    }

    @Test
    void recognizeProjectCode_nonJsonOutputFailsFast() {
        DccProjectCodeRecognitionProperties properties = new DccProjectCodeRecognitionProperties();
        properties.setCodexCliCommand(fakeCodexCliCommand(" non-json"));
        properties.setTimeoutSeconds(30);
        DccProjectCodeCodexCliClientImpl client = new DccProjectCodeCodexCliClientImpl(properties);

        assertServiceException(() -> client.recognizeProjectCode(command()),
                CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                "Codex CLI returned non-JSON project-code payload");
    }

    @Test
    void recognizeProjectCode_keepsSuccessfulResultWhenTemporaryCleanupFails() {
        DccProjectCodeRecognitionProperties properties = new DccProjectCodeRecognitionProperties();
        properties.setCodexCliCommand(fakeCodexCliCommand(" hold-output"));
        properties.setTimeoutSeconds(30);
        DccProjectCodeCodexCliClientImpl client = new DccProjectCodeCodexCliClientImpl(properties);

        DccProjectCodeRecognitionResult result = client.recognizeProjectCode(command());

        assertEquals(700L, result.projectCodeId());
        assertEquals(DccProjectCodeRecognitionMatchType.PROJECT_CODE, result.matchType());
        assertEquals("CODE-A", result.matchText());
    }

    @Test
    void buildPrompt_prefersContentButAllowsSourceFileNameForProjectCodeEvidence() throws Exception {
        DccProjectCodeRecognitionProperties properties = new DccProjectCodeRecognitionProperties();
        DccProjectCodeCodexCliClientImpl client = new DccProjectCodeCodexCliClientImpl(properties);
        Method method = DccProjectCodeCodexCliClientImpl.class.getDeclaredMethod(
                "buildPrompt", DccProjectCodeRecognitionCommand.class, String.class);
        method.setAccessible(true);

        String prompt = (String) method.invoke(client, command(), "RE-VER-CR-43.pdf");

        assertTrue(prompt.contains("优先根据源文件内容判断"));
        assertTrue(prompt.contains("也可以结合源文件名判断"));
        assertFalse(prompt.contains("禁止从文件名猜测"));
    }

    @Test
    void buildPrompt_surfacesDirectoryContextFromCommandSourceFileName() throws Exception {
        DccProjectCodeRecognitionProperties properties = new DccProjectCodeRecognitionProperties();
        DccProjectCodeCodexCliClientImpl client = new DccProjectCodeCodexCliClientImpl(properties);
        Method method = DccProjectCodeCodexCliClientImpl.class.getDeclaredMethod(
                "buildPrompt", DccProjectCodeRecognitionCommand.class, String.class);
        method.setAccessible(true);
        DccProjectCodeRecognitionCommand command = new DccProjectCodeRecognitionCommand(
                900L,
                321L,
                "质量管理/一次性使用导管鞘套装（FDA) IKFDA/包装运输/"
                        + "Appendix 3 Raw Data of All Performance Tests/"
                        + "07 Compatibility (RE-VER-CR-43).pdf",
                "application/pdf",
                "正文".getBytes(StandardCharsets.UTF_8),
                List.of(new DccProjectCodeRecognitionCommand.Candidate(
                        700L, "一次性使用导管鞘套装（FDA)", "IKFDA", "导管鞘类", "高")));

        String prompt = (String) method.invoke(client, command, "07 Compatibility (RE-VER-CR-43).pdf");

        assertTrue(prompt.contains("质量管理/一次性使用导管鞘套装（FDA) IKFDA/包装运输/"
                + "Appendix 3 Raw Data of All Performance Tests/"
                + "07 Compatibility (RE-VER-CR-43).pdf"));
        assertTrue(prompt.contains("目录路径也属于有效证据"));
    }

    private DccProjectCodeRecognitionCommand command() {
        return new DccProjectCodeRecognitionCommand(
                900L,
                321L,
                "6.4-51.xls",
                "application/vnd.ms-excel",
                "项目代码：CODE-A".getBytes(StandardCharsets.UTF_8),
                List.of(new DccProjectCodeRecognitionCommand.Candidate(
                        700L, "回归测试项目", "CODE-A", "类别", "高")));
    }

    private String fakeCodexCliCommand(String extraArg) {
        Path javaBinary = Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java");
        Path testClasses = Path.of("target", "test-classes").toAbsolutePath();
        return quote(javaBinary.toString()) + " -cp " + quote(testClasses.toString())
                + " cn.iocoder.yudao.module.dcc.service.file.DccProjectCodeCodexCliTestMain" + extraArg;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }
}
