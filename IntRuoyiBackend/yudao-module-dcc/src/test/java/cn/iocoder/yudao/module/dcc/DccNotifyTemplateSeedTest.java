package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccNotifyTemplateSeedTest {

    @Test
    void notifyTemplateSeed_containsDistributionTrainingAndObsoleteTemplates() throws IOException {
        String content = Files.readString(Path.of("..", "sql", "mysql", "20260513_dcc_notify_template_seed.sql"));

        assertTrue(content.contains("dcc_distribution"),
                "DCC notify template seed must contain distribution template");
        assertTrue(content.contains("dcc_training"),
                "DCC notify template seed must contain training template");
        assertTrue(content.contains("dcc_task_assigned"),
                "DCC notify template seed must contain task-assigned template");
        assertTrue(content.contains("dcc_controlled_file_approved"),
                "DCC notify template seed must contain process-approve template");
        assertTrue(content.contains("dcc_controlled_file_rejected"),
                "DCC notify template seed must contain process-reject template");
        assertTrue(content.contains("dcc_task_timeout"),
                "DCC notify template seed must contain task-timeout template");
        assertTrue(content.contains("dcc_obsolete"),
                "DCC notify template seed must contain obsolete template");
    }
}
