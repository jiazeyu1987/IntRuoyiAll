package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodexTestLocalConfigTest {

    private static final String RUNTIME_ARTIFACT_DIR =
            "E:/IntRuoyi/output/runtime/${INTRUOYI_RUNTIME_PROFILE:int_main}/codex-test-artifacts";
    private static final String ARTIFACT_TEMP_DIR =
            "${CODEX_TEST_ARTIFACT_TEMP_DIR:${INTRUOYI_RUNTIME_ARTIFACT_DIR:" + RUNTIME_ARTIFACT_DIR + "}}";
    private static final String ARTIFACT_RETENTION_HOURS = "${CODEX_TEST_ARTIFACT_RETENTION_HOURS:24}";

    private final Path projectDir = findProjectDir();

    @Test
    void localProfileShouldConfigureCodexTestArtifactTempDir() throws IOException {
        String content = Files.readString(projectDir.resolve(
                "yudao-server/src/main/resources/application-local.yaml"), StandardCharsets.UTF_8);

        assertTrue(content.contains("codex-test:"),
                "application-local.yaml must configure Codex test management");
        assertTrue(content.contains("artifact-temp-dir: " + ARTIFACT_TEMP_DIR),
                "local profile must provide a runtime-specific Codex test artifact temp directory");
        assertTrue(content.contains("artifact-retention-hours: " + ARTIFACT_RETENTION_HOURS),
                "local profile must make Codex test artifact retention explicit");
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-server".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
