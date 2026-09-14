package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardRecommendationReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardRecommendationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardScenarioRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeOpsGuideServiceImplTest {

    @TempDir
    private Path tempDir;

    @Test
    void getScenariosShouldExposeDecisionWizardScenariosWithoutExecutingActions() {
        RuntimeOpsGuideService service = new RuntimeOpsGuideServiceImpl(candidateService());

        List<RuntimeControlWizardScenarioRespVO> scenarios = service.getScenarios();

        List<String> scenarioCodes = scenarios.stream()
                .map(RuntimeControlWizardScenarioRespVO::getScenario)
                .toList();
        assertTrue(scenarioCodes.containsAll(List.of("app-exception", "data-exception", "pre-release-check",
                "post-release-observation", "backup-drill", "disk-risk")));
        RuntimeControlWizardScenarioRespVO dataScenario = scenarios.stream()
                .filter(scenario -> "data-exception".equals(scenario.getScenario()))
                .findFirst()
                .orElseThrow();
        assertEquals("restore-data", dataScenario.getRecommendedAction());
        assertTrue(dataScenario.getRequiredEvidence().contains("backup-manifest"));
        assertTrue(dataScenario.getRequiredEvidence().contains("checksum"));
        assertFalse(dataScenario.getRequiredEvidence().contains("rehearsal-report"));
        assertFalse(dataScenario.getRequiredEvidence().contains("现场快照"));
        assertFalse(dataScenario.getBlockingConditions().stream().anyMatch(reason -> reason.contains("演练")));
        assertFalse(dataScenario.getBlockingConditions().stream().anyMatch(reason -> reason.contains("现场快照")));
        assertTrue(dataScenario.getRequiredOwnerRoles().contains("data-owner"));
    }

    @Test
    void recommendShouldExposeInspectionEntryForPreReleaseCheckWithoutFakeEndpoint() {
        RuntimeOpsGuideService service = new RuntimeOpsGuideServiceImpl(candidateService());
        RuntimeControlWizardRecommendationReqVO reqVO = new RuntimeControlWizardRecommendationReqVO();
        reqVO.setScenario("pre-release-check");

        RuntimeControlWizardRecommendationRespVO recommendation = service.recommend(reqVO);

        assertEquals("inspection-run", recommendation.getRecommendedAction());
        assertTrue(recommendation.getRequiredEvidence().contains("inspection-report"));
        assertTrue(recommendation.getBlockingReasons().stream()
                .anyMatch(reason -> reason.contains("关键证据")));
        assertTrue(recommendation.getRollbackCandidates().isEmpty());
        assertTrue(recommendation.getRestoreCandidates().isEmpty());
    }

    @Test
    void recommendShouldNotBlockDataExceptionWhenRehearsalEvidenceIsMissing() throws Exception {
        createRestoreCandidate("20260526-010203", true, false, true);
        RuntimeOpsGuideService service = new RuntimeOpsGuideServiceImpl(candidateService());
        RuntimeControlWizardRecommendationReqVO reqVO = new RuntimeControlWizardRecommendationReqVO();
        reqVO.setScenario("data-exception");

        RuntimeControlWizardRecommendationRespVO recommendation = service.recommend(reqVO);

        assertEquals("restore-data", recommendation.getRecommendedAction());
        assertTrue(recommendation.getRequiredEvidence().contains("backup-manifest"));
        assertTrue(recommendation.getRequiredEvidence().contains("checksum"));
        assertFalse(recommendation.getRequiredEvidence().contains("rehearsal-report"));
        assertFalse(recommendation.getRequiredEvidence().contains("现场快照"));
        assertFalse(recommendation.getRestoreCandidates().isEmpty());
        assertFalse(recommendation.getBlockingReasons().stream().anyMatch(reason -> reason.contains("演练")));
        assertFalse(recommendation.getBlockingReasons().stream().anyMatch(reason -> reason.contains("现场快照")));
        assertEquals("AVAILABLE", recommendation.getRestoreCandidates().get(0).getStatus());
    }

    @Test
    void recommendShouldStillBlockDataExceptionWhenManifestIsMissing() throws Exception {
        createRestoreCandidate("20260526-010203", false, false, false);
        RuntimeOpsGuideService service = new RuntimeOpsGuideServiceImpl(candidateService());
        RuntimeControlWizardRecommendationReqVO reqVO = new RuntimeControlWizardRecommendationReqVO();
        reqVO.setScenario("data-exception");

        RuntimeControlWizardRecommendationRespVO recommendation = service.recommend(reqVO);

        assertEquals("restore-data", recommendation.getRecommendedAction());
        assertFalse(recommendation.getRestoreCandidates().isEmpty());
        assertEquals("BLOCKED", recommendation.getRestoreCandidates().get(0).getStatus());
        assertTrue(recommendation.getBlockingReasons().stream().anyMatch(reason -> reason.contains("manifest")));
    }

    @Test
    void recommendShouldBlockDataExceptionWhenRecoveryConfigurationComposePathIsMissing() throws Exception {
        createRestoreCandidate("20260526-010203", true, false, true, false);
        RuntimeOpsGuideService service = new RuntimeOpsGuideServiceImpl(candidateService());
        RuntimeControlWizardRecommendationReqVO reqVO = new RuntimeControlWizardRecommendationReqVO();
        reqVO.setScenario("data-exception");

        RuntimeControlWizardRecommendationRespVO recommendation = service.recommend(reqVO);

        assertEquals("restore-data", recommendation.getRecommendedAction());
        assertFalse(recommendation.getRestoreCandidates().isEmpty());
        assertEquals("BLOCKED", recommendation.getRestoreCandidates().get(0).getStatus());
        assertTrue(recommendation.getRestoreCandidates().get(0).getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("recoverySet.configuration.composePath")));
    }

    private RuntimeOpsCandidateService candidateService() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        properties.getBackupOps().setNasBackupPointsRoot("backup-points");
        return new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
    }

    private void createRestoreCandidate(String backupId, boolean manifest, boolean rehearsal, boolean snapshot) throws Exception {
        createRestoreCandidate(backupId, manifest, rehearsal, snapshot, true);
    }

    private void createRestoreCandidate(String backupId, boolean manifest, boolean rehearsal, boolean snapshot,
                                        boolean includeComposePath) throws Exception {
        Path root = tempDir.resolve("backup-points").resolve(backupId);
        java.nio.file.Files.createDirectories(root.resolve("deploy"));
        java.nio.file.Files.createDirectories(root.resolve("manifest"));
        java.nio.file.Files.createDirectories(root.resolve("mysql"));
        java.nio.file.Files.createDirectories(root.resolve("objects"));
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("image-tag.txt"), "20260526_010203");
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("runtime.env"),
                "IMAGE_TAG=20260526_010203\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n");
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("docker-compose.yml"), "services: {}\n");
        java.nio.file.Files.writeString(root.resolve("mysql").resolve("ruoyi-vue-pro.sql.gz"), "dump");
        java.nio.file.Files.writeString(root.resolve("objects").resolve("manifest-object-inventory.json"),
                "{\"mode\":\"incremental-manifest\",\"objects\":[]}");
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("checksums.txt"),
                "sha256  deploy/runtime.env");
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("dcc-backup-manifest.json"),
                "{\"schemaVersion\":\"dcc-backup-manifest-v1\",\"backupMode\":\"FULL\","
                        + "\"chainStatus\":\"COMPLETE\",\"changeSummary\":{\"business\":\"none\"}}");
        if (manifest) {
            String configuration = includeComposePath
                    ? "\"configuration\":{\"manifestPath\":\"deploy/runtime.env\",\"composePath\":\"deploy/docker-compose.yml\"}"
                    : "\"configuration\":{\"manifestPath\":\"deploy/runtime.env\"}";
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                    "{\"schemaVersion\":\"v2\",\"backupId\":\"" + backupId
                            + "\",\"targetEnvironment\":\"test\",\"targetHost\":\"172.30.30.58\",\"status\":\"success\",\"deploy\":{\"imageTag\":\"20260526_010203\"},\"recoverySet\":{\"id\":\""
                            + backupId + "\",\"status\":\"COMPLETE\",\"program\":{\"imageTag\":\"20260526_010203\"},\"mysql\":{\"dumpPath\":\"mysql/ruoyi-vue-pro.sql.gz\"},\"minio\":{\"bucket\":\"yudao\",\"snapshotPath\":\"objects/manifest-object-inventory.json\"},\"businessFiles\":{\"snapshotPath\":\"objects/manifest-object-inventory.json\"},\"redis\":{\"policy\":\"CLEAR_AND_REBUILD\"},"
                            + configuration + ",\"checksums\":{\"path\":\"manifest/checksums.txt\",\"sha256\":\"abc\"}}}");
        }
        if (rehearsal) {
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("rehearsal-report.json"),
                    "{\"status\":\"PASSED\"}");
        }
        if (snapshot) {
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("现场快照.md"), "snapshot");
        }
    }

    private Path writeBackupOpsConfig(Path backupPointsRoot) {
        try {
            java.nio.file.Files.createDirectories(tempDir.resolve("backup-ops-config"));
            Path configPath = tempDir.resolve("backup-ops-config").resolve("backup-ops.config.json");
            java.nio.file.Files.writeString(configPath, """
                    {
                      "servers": {
                        "test": {
                          "backupPointsRoot": "%s"
                        }
                      }
                    }
                    """.formatted(backupPointsRoot.toString().replace("\\", "\\\\")));
            return configPath;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
