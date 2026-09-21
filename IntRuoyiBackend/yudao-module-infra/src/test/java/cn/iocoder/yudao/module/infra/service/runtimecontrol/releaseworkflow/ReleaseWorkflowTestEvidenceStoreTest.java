package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseWorkflowTestEvidenceStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void successfulApplicationOperationProducesEvidenceAcceptedByActualPublisherVerifier() throws Exception {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        ReleaseWorkflowTestEvidenceStore evidenceStore = new ReleaseWorkflowTestEvidenceStore(properties, operationStore);
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
        ReleaseWorkflowRecord workflow = workflowService.create("verify release evidence", "approved-source");
        RuntimeControlOperationRespVO publish = new RuntimeControlOperationRespVO();
        publish.setOperationId("op-test-evidence-12345678");
        publish.setRequestedAt(LocalDateTime.now());
        publish.setAction("publish-test");
        publish.setEnvironment("test");
        publish.setStatus("succeeded");
        publish.setParameters(Map.of("releaseTag", workflow.releaseTag()));
        operationStore.save(publish);
        Path path = evidenceStore.write(workflow.workflowId(), workflow.releaseTag(), publish);
        workflow = workflowService.bindTestOperation(workflow.workflowId(), workflow.stateVersion(),
                publish.getOperationId(), path.toString());

        assertTrue(Files.isRegularFile(path));
        assertEquals(Instant.parse(evidenceStore.verify(workflow).requestedAt()).toString(),
                evidenceStore.verify(workflow).requestedAt());
        Path module = Path.of(properties.getReleaseWorkflow().getMaintenanceRepoRoot())
                .resolve("ops/release/lib/ReleasePackageGovernance.psm1");
        assertTrue(Files.isRegularFile(module), "the production release verifier must be available");
        ProcessBuilder command = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                "$ErrorActionPreference='Stop'; Import-Module -Force $env:RELEASE_GOVERNANCE_MODULE; "
                        + "$null = Assert-PublishTestOperationEvidence -EvidencePath $env:RELEASE_EVIDENCE_PATH "
                        + "-ExpectedOperationId $env:RELEASE_OPERATION_ID -ExpectedReleaseTag $env:RELEASE_TAG; "
                        + "Write-Output 'EVIDENCE_VALID'");
        command.environment().put("RELEASE_GOVERNANCE_MODULE", module.toString());
        command.environment().put("RELEASE_EVIDENCE_PATH", path.toString());
        command.environment().put("RELEASE_OPERATION_ID", publish.getOperationId());
        command.environment().put("RELEASE_TAG", workflow.releaseTag());
        command.redirectErrorStream(true);
        Process process = command.start();
        boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor();
        }
        assertTrue(finished, "release evidence verifier must terminate");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("EVIDENCE_VALID"));

        publish.setStatus("failed");
        operationStore.save(publish);
        ReleaseWorkflowRecord tested = workflow;
        assertThrows(IllegalStateException.class, () -> evidenceStore.verify(tested));
        assertFalse(Files.readString(path, StandardCharsets.UTF_8).contains("failed"));
    }
}
