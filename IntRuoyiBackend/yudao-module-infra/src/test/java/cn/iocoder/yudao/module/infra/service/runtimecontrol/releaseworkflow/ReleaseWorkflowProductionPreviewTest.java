package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReleaseWorkflowProductionPreviewTest {

    @TempDir
    Path tempDir;

    @Test
    void previewBlocksUntilTestedAndProductionWriteIsEnabled() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(false);
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
        ReleaseWorkflowProductionPreviewService previewService =
                previewService(properties);
        ReleaseWorkflowRecord workflow = workflowService.create("preview", "approved-source");

        var preview = previewService.create(workflow, workflow.stateVersion());

        assertFalse(preview.isEligible());
        assertEquals(2, preview.getBlockers().stream()
                .filter(item -> item.startsWith("WORKFLOW_TESTED") || item.startsWith("PRODUCTION_WRITE_ENABLED"))
                .count());
    }

    @Test
    void staleStateVersionIsRejectedBeforePreviewPersistence() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
        ReleaseWorkflowProductionPreviewService previewService =
                previewService(properties);
        ReleaseWorkflowRecord workflow = workflowService.create("preview-stale", "approved-source");

        assertThrows(IllegalStateException.class,
                () -> previewService.create(workflow, workflow.stateVersion() + 1));
    }

    @Test
    void productionTargetChangeInvalidatesPreviouslyEligiblePreview() {
        Fixture fixture = testedFixture();
        var preview = fixture.previewService().create(fixture.workflow(), fixture.workflow().stateVersion());
        assertTrue(preview.isEligible());

        fixture.properties().getEnvironments().get("prod").setHost("198.51.100.57");
        assertThrows(IllegalStateException.class,
                () -> fixture.previewService().requireBinding(preview.getPreviewId(), fixture.workflow(),
                        fixture.workflow().stateVersion()));
    }

    @Test
    void testedRecordWithoutReadableReleaseEvidenceCannotBecomeEligible() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
        ReleaseWorkflowProductionPreviewService previewService =
                previewService(properties);
        ReleaseWorkflowRecord workflow = workflowService.create("unverified evidence", "approved-source");
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, true);
        workflow = workflowService.bindArtifacts(workflow.workflowId(), workflow.stateVersion(),
                "a".repeat(64), "b".repeat(64));
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTING, "TESTING", true, true);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, true);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.READY, "READY", true, true);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TEST_DEPLOYING, "TEST_DEPLOYING", true, false);
        workflow = workflowService.bindTestOperation(workflow.workflowId(), workflow.stateVersion(),
                "op-test-12345678", tempDir.resolve("missing-operation-evidence.json").toString());
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TEST_DEPLOYED, "TEST_DEPLOYED", true, false);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTED, "TESTED", true, false);

        var preview = previewService.create(workflow, workflow.stateVersion());

        assertFalse(preview.isEligible());
        assertTrue(preview.getBlockers().stream().anyMatch(value -> value.startsWith("PACKAGE_EVIDENCE")));
        assertTrue(preview.getBlockers().stream().anyMatch(value -> value.startsWith("TEST_OPERATION_EVIDENCE")));
        assertTrue(preview.getBlockers().stream().anyMatch(value -> value.startsWith("RELEASE_EXECUTOR")));
        assertEquals(ReleaseWorkflowRecord.State.TESTED, workflowService.require(workflow.workflowId()).state());
    }

    @Test
    void productionPreviewBlocksWhenEnvironmentAccessIsDisabled() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
        ReleaseWorkflowProductionPreviewService previewService =
                previewService(properties);
        ReleaseWorkflowRecord workflow = workflowService.create("preview-target-disabled", "approved-source");

        var preview = previewService.create(workflow, workflow.stateVersion());

        assertTrue(preview.getBlockers().stream().anyMatch(item -> item.startsWith("PRODUCTION_ACCESS_ENABLED")));
    }

    @Test
    void missingProductionTargetReturnsBlockedPreview() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getEnvironments().remove("prod");
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);

        var preview = previewService(properties).create(
                workflowService.create("missing target", "approved-source"), 0);

        assertFalse(preview.isEligible());
        assertTrue(preview.getBlockers().stream().anyMatch(value -> value.startsWith("PRODUCTION_TARGET_CONFIGURED")));
    }

    @Test
    void changedManifestAndPackageDigestBlockBeforeAuthorization() {
        Fixture fixture = testedFixture();
        fixture.releasePackage().setManifestDigest("0".repeat(64));

        var preview = fixture.previewService().create(fixture.workflow(), fixture.workflow().stateVersion());

        assertFalse(preview.isEligible());
        assertTrue(preview.getBlockers().stream().anyMatch(value -> value.startsWith("PACKAGE_EVIDENCE")));
        assertThrows(IllegalStateException.class,
                () -> fixture.previewService().requireBinding(preview.getPreviewId(), fixture.workflow(),
                        fixture.workflow().stateVersion()));
    }

    @Test
    void testedAttestationMismatchAndMissingEvidenceBothBlockPreview() throws Exception {
        Fixture fixture = testedFixture();
        fixture.releasePackage().setTestedOperationId("op-different-12345678");
        var mismatched = fixture.previewService().create(fixture.workflow(), fixture.workflow().stateVersion());
        assertFalse(mismatched.isEligible());
        assertTrue(mismatched.getBlockers().stream().anyMatch(value -> value.startsWith("TESTED_ATTESTATION")));

        fixture.releasePackage().setTestedOperationId(fixture.workflow().testOperationId());
        Files.delete(fixture.evidencePath());
        var missing = fixture.previewService().create(fixture.workflow(), fixture.workflow().stateVersion());
        assertFalse(missing.isEligible());
        assertTrue(missing.getBlockers().stream().anyMatch(value -> value.startsWith("TEST_OPERATION_EVIDENCE")));
    }

    @Test
    void executorFingerprintMismatchBlocksPreview() {
        Fixture fixture = testedFixture();
        fixture.properties().getReleaseWorkflow().setExpectedPublishScriptSha256("0".repeat(64));

        var preview = fixture.previewService().create(fixture.workflow(), fixture.workflow().stateVersion());

        assertFalse(preview.isEligible());
        assertTrue(preview.getBlockers().stream().anyMatch(value -> value.startsWith("RELEASE_EXECUTOR")));
    }

    @Test
    void evidenceChangedAfterPreviewInvalidatesBinding() throws Exception {
        Fixture fixture = testedFixture();
        var preview = fixture.previewService().create(fixture.workflow(), fixture.workflow().stateVersion());
        assertTrue(preview.isEligible());

        fixture.releasePackage().setTestedDigest("f".repeat(64));
        assertThrows(IllegalStateException.class,
                () -> fixture.previewService().requireBinding(preview.getPreviewId(), fixture.workflow(),
                        fixture.workflow().stateVersion()));
        fixture.releasePackage().setTestedDigest("c".repeat(64));
        RuntimeControlOperationRespVO persisted = fixture.operationStore().findById(fixture.workflow().testOperationId());
        persisted.setStatus("failed");
        fixture.operationStore().save(persisted);
        assertThrows(IllegalStateException.class,
                () -> fixture.previewService().requireBinding(preview.getPreviewId(), fixture.workflow(),
                        fixture.workflow().stateVersion()));
    }

    @Test
    void corruptPersistedOperationIsReportedAsPreviewBlocker() throws Exception {
        Fixture fixture = testedFixture();
        Files.writeString(fixture.operationStore().getOperationPath(fixture.workflow().testOperationId()), "{");

        var preview = fixture.previewService().create(fixture.workflow(), fixture.workflow().stateVersion());

        assertFalse(preview.isEligible());
        assertTrue(preview.getBlockers().stream().anyMatch(value -> value.startsWith("TEST_OPERATION_EVIDENCE")));
    }

    private ReleaseWorkflowProductionPreviewService previewService(RuntimeControlProperties properties) {
        RuntimeControlOperationStore store = new RuntimeControlOperationStore(properties);
        return new ReleaseWorkflowProductionPreviewService(properties, mock(RuntimeControlService.class),
                new ReleaseWorkflowTestEvidenceStore(properties, store));
    }

    private Fixture testedFixture() {
        try {
            RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
            properties.getEnvironments().get("prod").setAccessEnabled(true);
            properties.getReleaseWorkflow().setProductionWriteEnabled(true);
            Path script = tempDir.resolve("maintenance/ops/deploy/publish-int-ruoyi.ps1");
            Files.createDirectories(script.getParent());
            byte[] scriptBytes = "param([string]$Mode)\n".getBytes(StandardCharsets.UTF_8);
            Files.write(script, scriptBytes);
            properties.getReleaseWorkflow().setMaintenanceRepoRoot(tempDir.resolve("maintenance").toString());
            properties.getReleaseWorkflow().setExpectedPublishScriptSha256(HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(scriptBytes)));
            RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
            ReleaseWorkflowTestEvidenceStore evidenceStore = new ReleaseWorkflowTestEvidenceStore(properties, operationStore);
            ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
            ReleaseWorkflowRecord workflow = workflowService.create("preview-target-binding", "approved-source");
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, true);
            workflow = workflowService.bindArtifacts(workflow.workflowId(), workflow.stateVersion(),
                    "a".repeat(64), "b".repeat(64));
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.TESTING, "TESTING", true, true);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, true);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.READY, "READY", true, true);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.TEST_DEPLOYING, "TEST_DEPLOYING", true, false);
            RuntimeControlOperationRespVO publish = new RuntimeControlOperationRespVO();
            publish.setOperationId("op-test-12345678");
            publish.setRequestedAt(LocalDateTime.now());
            publish.setAction("publish-test");
            publish.setEnvironment("test");
            publish.setStatus("succeeded");
            publish.setParameters(Map.of("releaseTag", workflow.releaseTag()));
            operationStore.save(publish);
            Path evidencePath = evidenceStore.write(workflow.workflowId(), workflow.releaseTag(), publish);
            workflow = workflowService.bindTestOperation(workflow.workflowId(), workflow.stateVersion(),
                    publish.getOperationId(), evidencePath.toString());
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.TEST_DEPLOYED, "TEST_DEPLOYED", true, false);
            workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                    ReleaseWorkflowRecord.State.TESTED, "TESTED", true, false);
            RuntimeControlReleasePackageRespVO releasePackage = new RuntimeControlReleasePackageRespVO();
            releasePackage.setStatus("AVAILABLE");
            releasePackage.setChecksumPresent(true);
            releasePackage.setPublishScope("app-release");
            releasePackage.setReleaseTag(workflow.releaseTag());
            releasePackage.setPackageDirectoryName(workflow.releaseTag());
            releasePackage.setPackageDigest(workflow.packageDigest());
            releasePackage.setManifestDigest(workflow.manifestDigest());
            releasePackage.setTested(true);
            releasePackage.setTestedDigest("c".repeat(64));
            releasePackage.setTestedSchemaVersion("v2");
            releasePackage.setTestedReleaseTag(workflow.releaseTag());
            releasePackage.setTestedPackageDirectoryName(workflow.releaseTag());
            releasePackage.setTestedPackageDigest(workflow.packageDigest());
            releasePackage.setTestedManifestDigest(workflow.manifestDigest());
            releasePackage.setTestedEnvironment("test");
            releasePackage.setTestedOperationId(publish.getOperationId());
            releasePackage.setTestedOperationStatus("SUCCESS");
            releasePackage.setTestedOperationRequestedAt(evidenceStore.verify(workflow).requestedAt());
            releasePackage.setTestedResult("PASS");
            releasePackage.setTestedAt(Instant.now().toString());
            releasePackage.setOperatorName("qa");
            releasePackage.setTestedConclusion("validated");
            RuntimeControlService runtimeService = mock(RuntimeControlService.class);
            when(runtimeService.getReleasePackage(workflow.releaseTag())).thenReturn(Optional.of(releasePackage));
            return new Fixture(properties, workflow,
                    new ReleaseWorkflowProductionPreviewService(properties, runtimeService, evidenceStore),
                    runtimeService, releasePackage, evidencePath, script, operationStore);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private record Fixture(RuntimeControlProperties properties, ReleaseWorkflowRecord workflow,
                           ReleaseWorkflowProductionPreviewService previewService,
                           RuntimeControlService runtimeService, RuntimeControlReleasePackageRespVO releasePackage,
                           Path evidencePath, Path script, RuntimeControlOperationStore operationStore) {
    }
}
