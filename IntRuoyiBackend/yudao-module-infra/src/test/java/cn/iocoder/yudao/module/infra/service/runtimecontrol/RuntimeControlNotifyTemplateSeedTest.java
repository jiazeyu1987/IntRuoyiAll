package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RuntimeControlNotifyTemplateSeedTest {

    @Test
    void notifyTemplateSeedContainsRuntimeOpsAlertTemplate() throws IOException {
        String content = Files.readString(Path.of("..", "sql", "mysql",
                "20260527_infra_runtime_control_notify_template_seed.sql"));

        assertTrue(content.contains("RUNTIME_OPS_ALERT"),
                "Runtime control notify seed must contain runtime ops alert template");
        assertTrue(content.contains("environment"),
                "Runtime control notify seed must contain environment parameter");
        assertTrue(content.contains("action"),
                "Runtime control notify seed must contain action parameter");
        assertTrue(content.contains("severity"),
                "Runtime control notify seed must contain severity parameter");
        assertTrue(content.contains("title"),
                "Runtime control notify seed must contain title parameter");
        assertTrue(content.contains("content"),
                "Runtime control notify seed must contain content parameter");
        assertTrue(content.contains("WHERE NOT EXISTS"),
                "Runtime control notify seed must be idempotent when template already exists");
        assertTrue(content.contains("WHERE `code` = 'RUNTIME_OPS_ALERT'"),
                "Runtime control notify seed must scope existence check to runtime ops alert code");
    }

    @Test
    void notifyTemplateSeedShouldNotOverwriteExistingRuntimeOpsAlertTemplate() throws IOException {
        String content = Files.readString(Path.of("..", "sql", "mysql",
                "20260527_infra_runtime_control_notify_template_seed.sql"));
        String normalized = content.toUpperCase(Locale.ROOT);

        assertFalse(normalized.contains("UPDATE `SYSTEM_NOTIFY_TEMPLATE`"),
                "Runtime control notify seed must not overwrite, re-enable, or revive existing templates");
        assertFalse(normalized.contains("ON DUPLICATE KEY UPDATE"),
                "Runtime control notify seed must not depend on overwrite-style upsert behavior");
    }
}
