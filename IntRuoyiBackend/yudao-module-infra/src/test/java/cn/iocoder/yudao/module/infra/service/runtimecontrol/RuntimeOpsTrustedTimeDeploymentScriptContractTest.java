package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeOpsTrustedTimeDeploymentScriptContractTest {

    @Test
    void scriptShouldCollectRequiredReadonlyTimeEvidenceWithoutPrintingCredentials() throws Exception {
        String script = Files.readString(Path.of("..", "script", "deploy", "show-int-ruoyi-trusted-time.ps1"));

        assertTrue(script.contains("chronyc tracking"));
        assertTrue(script.contains("chronyc sources -n"));
        assertTrue(script.contains("timedatectl status"));
        assertTrue(script.contains("date -u"));
        assertTrue(script.contains("UTC_TIMESTAMP(6)"));
        assertTrue(script.contains("BatchMode=yes"));
        assertTrue(script.contains("ConvertTo-Json"));
        assertFalse(script.contains("Write-Host $MYSQL_ROOT_PASSWORD"));
        assertFalse(script.contains("Write-Output $MYSQL_ROOT_PASSWORD"));
    }
}
