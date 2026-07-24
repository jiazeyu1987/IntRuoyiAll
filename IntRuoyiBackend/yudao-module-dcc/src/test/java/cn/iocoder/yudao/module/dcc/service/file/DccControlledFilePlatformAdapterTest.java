package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledFilePlatformAdapterTest {

    @Test
    void workflowFinalizationAndObsoleteServices_shouldDelegateLifecycleFactsToPlatformAdapter() throws Exception {
        String workflow = readSource("DccControlledFileWorkflowServiceImpl.java");
        String finalization = readSource("DccControlledFileFinalizationServiceImpl.java");
        String obsolete = readSource("DccControlledFileObsoleteServiceImpl.java");

        assertTrue(workflow.contains("DccControlledContentAdapter platformAdapter"),
                "DCC workflow must inject platform adapter");
        assertTrue(workflow.contains("platformAdapter.recordSubmitted"),
                "DCC submit must create and submit platform candidate ref");
        assertTrue(workflow.contains("platformAdapter.recordWithdrawn"),
                "DCC withdraw must close the old platform candidate ref");
        assertTrue(workflow.contains("platformAdapter.recordResubmitted"),
                "DCC resubmit must link old WITHDRAWN revision to the new DRAFT successor");
        assertTrue(finalization.contains("platformAdapter.recordFinalizationStarted"),
                "DCC finalization start must be mirrored to platform lifecycle");
        assertTrue(finalization.contains("platformAdapter.recordFinalizationFailed"),
                "DCC finalization failure must keep the candidate open in platform lifecycle");
        assertTrue(finalization.contains("platformAdapter.recordFinalized"),
                "DCC finalization success must atomically activate platform ref");
        assertTrue(obsolete.contains("platformAdapter.recordObsoleted"),
                "DCC active obsolete must release platform active ref");
    }

    private String readSource(String fileName) throws Exception {
        return Files.readString(sourcePath("yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/",
                fileName), StandardCharsets.UTF_8);
    }

    private Path sourcePath(String first, String... more) {
        Path moduleRelativePath = Path.of(first, more);
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path direct = current.resolve(moduleRelativePath);
            if (Files.exists(direct)) {
                return direct;
            }
            Path nestedBackend = current.resolve("ruoyi-vue-pro").resolve(moduleRelativePath);
            if (Files.exists(nestedBackend)) {
                return nestedBackend;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate source file: " + moduleRelativePath);
    }

}
