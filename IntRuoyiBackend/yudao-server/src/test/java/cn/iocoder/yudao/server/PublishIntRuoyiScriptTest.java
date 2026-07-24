package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishIntRuoyiScriptTest {

    private final Path projectDir = findProjectDir();

    @Test
    void nasDisconnectShouldTreatMissingMappingAsCompletedCleanupOnly() throws IOException {
        String script = Files.readString(projectDir.resolve("script/deploy/publish-int-ruoyi.ps1"));

        assertTrue(script.contains("function Invoke-NasReleaseShareDisconnect"),
                "publish script must route NAS disconnect through a dedicated cleanup helper");
        assertTrue(script.contains("NET HELPMSG 2250"),
                "NAS disconnect cleanup must explicitly recognize the exact missing-mapping result");
        assertTrue(script.contains("$result.ExitCode -eq 2 -and $cleanOutput -match 'NET HELPMSG 2250'"),
                "NAS disconnect cleanup must only accept the exact net-use missing-mapping exit and message");
        assertTrue(script.contains("Fail \"Shell command failed with exit code $($result.ExitCode): $displayCommand"),
                "NAS disconnect cleanup must continue to fail on non-2250 cleanup errors");
        assertTrue(script.contains("Invoke-NasReleaseShareDisconnect -Root"),
                "Disconnect-NasReleaseShare must use the guarded NAS disconnect helper");
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-server".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
