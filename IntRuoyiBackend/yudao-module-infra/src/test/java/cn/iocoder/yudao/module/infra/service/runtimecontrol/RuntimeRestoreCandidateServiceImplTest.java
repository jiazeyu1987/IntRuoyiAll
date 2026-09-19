package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestoreCandidateRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class RuntimeRestoreCandidateServiceImplTest extends BaseMockitoUnitTest {

    @TempDir
    private Path tempDir;

    @Mock
    private RuntimeControlCommandExecutor commandExecutor;

    private RuntimeControlProperties properties;
    private RuntimeOpsCandidateService candidateService;
    private Path backupPointsRoot;

    @BeforeEach
    void setUp() throws Exception {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        properties.getBackupOps().setExecutionMode("linux-local");
        properties.getBackupOps().setLinuxScriptPath("/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh");
        backupPointsRoot = tempDir.resolve("nas-backup-points");
        java.nio.file.Files.createDirectories(backupPointsRoot);
        properties.getBackupOps().setNasBackupPointsRoot("nas-backup-points");
        candidateService = new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
    }

    @Test
    void listRestoreCandidatesShouldReadBackupPointsRootFromNasConfig() throws Exception {
        createRestorePoint("20260526-010203", true, true, true, true);
        java.nio.file.Files.createDirectories(tempDir.resolve("backup-points"));

        List<RuntimeControlRestoreCandidateRespVO> candidates = candidateService.listRestoreCandidates();

        assertEquals(2, candidates.size());
        assertEquals("nas-backup-points/20260526-010203/manifest/manifest.json",
                candidates.get(0).getManifestPath());
    }

    @Test
    void listRestoreCandidatesShouldFailWhenNasBackupPointsRootIsMissingInConfig() {
        properties.getBackupOps().setNasBackupPointsRoot("");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> candidateService.listRestoreCandidates());

        assertTrue(exception.getMessage().contains("nasBackupPointsRoot"));
    }

    @Test
    void listRestoreCandidatesShouldNotRequireRehearsalReportOrSnapshot() throws Exception {
        createRestorePoint("20260526-010203", true, true, false, false);

        List<RuntimeControlRestoreCandidateRespVO> candidates = candidateService.listRestoreCandidates();

        RuntimeControlRestoreCandidateRespVO rehearsal = findCandidate(candidates, "REHEARSAL");
        RuntimeControlRestoreCandidateRespVO controlledRestore = findCandidate(candidates, "CONTROLLED_RESTORE");
        assertEquals("AVAILABLE", rehearsal.getStatus());
        assertEquals("BLOCKED", controlledRestore.getStatus());
        assertFalse(rehearsal.getBlockedReasons().stream().anyMatch(reason -> reason.contains("演练")));
        assertFalse(rehearsal.getBlockedReasons().stream().anyMatch(reason -> reason.contains("现场快照")));
        assertTrue(controlledRestore.getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("rehearsalStatus") || reason.contains("演练")));
    }

    @Test
    void listRestoreCandidatesShouldSplitRehearsalAndControlledRestoreCandidates() throws Exception {
        createRestorePoint("20260526-010203", true, true, true, true);

        List<RuntimeControlRestoreCandidateRespVO> candidates = candidateService.listRestoreCandidates();

        assertEquals(2, candidates.size());
        RuntimeControlRestoreCandidateRespVO rehearsal = findCandidate(candidates, "REHEARSAL");
        RuntimeControlRestoreCandidateRespVO controlledRestore = findCandidate(candidates, "CONTROLLED_RESTORE");
        assertEquals("rehearsal:20260526-010203", rehearsal.getCandidateId());
        assertEquals("restore:20260526-010203", controlledRestore.getCandidateId());
        assertEquals("AVAILABLE", rehearsal.getStatus());
        assertEquals("AVAILABLE", controlledRestore.getStatus());
        assertEquals("PASSED", controlledRestore.getRehearsalStatus());
        assertEquals("2026-09-19T00:00:00+08:00", controlledRestore.getLastRehearsedAt());
        assertFalse(controlledRestore.getManifestDigest().isBlank());
        assertFalse(controlledRestore.getChainDigest().isBlank());
        assertNotEquals(controlledRestore.getSourceFingerprint(), controlledRestore.getTargetFingerprint());
        assertTrue(controlledRestore.getSourceFingerprint().contains("serverHost=172.30.30.58"));
        assertTrue(controlledRestore.getTargetFingerprint().contains("environment=test"));
        assertTrue(controlledRestore.getTargetFingerprint().contains("host=172.30.30.58"));
    }

    @Test
    void listRestoreCandidatesShouldExposeAvailableCandidateOnlyWhenAllEvidencePasses() throws Exception {
        createRestorePoint("20260526-010203", true, true, true, true);

        List<RuntimeControlRestoreCandidateRespVO> candidates = candidateService.listRestoreCandidates();

        assertFalse(candidates.isEmpty());
        RuntimeControlRestoreCandidateRespVO candidate = findCandidate(candidates, "CONTROLLED_RESTORE");
        assertEquals("AVAILABLE", candidate.getStatus());
        assertEquals("20260526-010203", candidate.getBackupId());
        assertFalse(candidate.getCandidateId().isBlank());
    }

    @Test
    void listRestoreCandidatesShouldBlockControlledRestoreWhenRehearsalEvidenceDoesNotBindCandidate()
            throws Exception {
        createRestorePoint("20260526-010203", true, true, true, true);
        Path reportPath = backupPointsRoot.resolve("20260526-010203")
                .resolve("manifest").resolve("rehearsal-report.json");
        java.nio.file.Files.writeString(reportPath, """
                {"status":"PASSED","backupId":"20260526-010203","manifestDigest":"wrong",
                 "chainDigest":"wrong","sourceFingerprint":"wrong","targetFingerprint":"wrong"}
                """);

        RuntimeControlRestoreCandidateRespVO candidate = findCandidate(candidateService.listRestoreCandidates(),
                "CONTROLLED_RESTORE");

        assertEquals("BLOCKED", candidate.getStatus());
        assertTrue(candidate.getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("rehearsal-report manifestDigest")));
        assertTrue(candidate.getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("rehearsal-report targetFingerprint")));
    }

    @Test
    void listRestoreCandidatesShouldAcceptIncrementalManifestObjectSnapshot() throws Exception {
        createRestorePoint("20260606-145029", true, true, true, true);

        RuntimeControlRestoreCandidateRespVO candidate = findCandidate(candidateService.listRestoreCandidates(),
                "CONTROLLED_RESTORE");

        assertEquals("AVAILABLE", candidate.getStatus());
        assertTrue(candidate.getBlockedReasons().isEmpty());
        assertEquals("objects/manifest-object-inventory.json", candidate.getComponentSummary().get("minio"));
        assertEquals("objects/manifest-object-inventory.json", candidate.getComponentSummary().get("businessFiles"));
    }

    @Test
    void listRestoreCandidatesShouldBlockLegacyTarObjectSnapshot() throws Exception {
        createArchiveRestorePoint("20260606-145029");

        RuntimeControlRestoreCandidateRespVO candidate = findCandidate(candidateService.listRestoreCandidates(),
                "CONTROLLED_RESTORE");

        assertEquals("BLOCKED", candidate.getStatus());
        assertTrue(candidate.getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("manifest-object-inventory.json")));
    }

    @Test
    void listRestoreCandidatesShouldBlockLegacyManifestWithoutRecoverySet() throws Exception {
        createLegacyRestorePoint("20260526-010203", "20260526_010203");

        List<RuntimeControlRestoreCandidateRespVO> candidates = candidateService.listRestoreCandidates();

        assertEquals(2, candidates.size());
        RuntimeControlRestoreCandidateRespVO candidate = findCandidate(candidates, "CONTROLLED_RESTORE");
        assertEquals("BLOCKED", candidate.getStatus());
        assertTrue(candidate.getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("recoverySet")));
    }

    @Test
    void listRestoreCandidatesShouldBlockManifestWithoutTestTargetProof() throws Exception {
        createRestorePoint("20260526-010203", true, true, true, true);
        String checksumsText = "sha256  deploy/runtime.env\n";
        java.nio.file.Files.writeString(backupPointsRoot.resolve("20260526-010203")
                        .resolve("manifest").resolve("manifest.json"),
                recoverySetManifestWithoutTargetProof("20260526-010203", "20260526_010203",
                        sha256(checksumsText)));

        RuntimeControlRestoreCandidateRespVO candidate = findCandidate(candidateService.listRestoreCandidates(),
                "CONTROLLED_RESTORE");

        assertEquals("BLOCKED", candidate.getStatus());
        assertTrue(candidate.getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("targetEnvironment") && reason.contains("172.30.30.58")));
    }

    @Test
    void listRestoreCandidatesShouldExposeRecoverySetFields() throws Exception {
        createRestorePoint("20260526-010203", true, true, true, true);

        RuntimeControlRestoreCandidateRespVO candidate = findCandidate(candidateService.listRestoreCandidates(),
                "CONTROLLED_RESTORE");

        assertEquals("20260526-010203", candidate.getRecoverySetId());
        assertEquals("COMPLETE", candidate.getRecoverySetStatus());
        assertEquals("20260526_010203", candidate.getProgramVersion());
        assertEquals("CLEAR_AND_REBUILD", candidate.getRedisPolicy());
        assertEquals("deploy/runtime.env", candidate.getConfigurationManifestPath());
        assertFalse(candidate.getRecoverySetManifestHash().isBlank());
        assertEquals(candidate.getRecoverySetManifestHash(), candidate.getManifestDigest());
        assertFalse(candidate.getChainDigest().isBlank());
        assertEquals("mysql/ruoyi-vue-pro.sql.gz", candidate.getComponentSummary().get("mysql"));
        assertEquals("objects/manifest-object-inventory.json", candidate.getComponentSummary().get("minio"));
        assertEquals("incremental", candidate.getDccBackupMode());
        assertEquals("COMPLETE", candidate.getDccChainStatus());
        assertEquals("1", candidate.getDccChangeSummary().get("addedRecords"));
        assertEquals("2", candidate.getDccChangeSummary().get("reusedObjects"));
    }

    @Test
    void listRestoreCandidatesShouldUseManifestDeployImageTagWhenImageTagFileIsMissing() throws Exception {
        createRestorePointWithManifestDeployImageTag("20260526-010203", null, "20260526_010203");

        List<RuntimeControlRestoreCandidateRespVO> candidates = candidateService.listRestoreCandidates();

        RuntimeControlRestoreCandidateRespVO candidate = findCandidate(candidates, "CONTROLLED_RESTORE");
        assertEquals("AVAILABLE", candidate.getStatus());
        assertEquals("20260526_010203", candidate.getImageTag());
    }

    @Test
    void listRestoreCandidatesShouldIgnoreNonBackupPointDirectories() throws Exception {
        createNonBackupPointDirectory("reference");
        createNonBackupPointDirectory("26-05-30_00-11-31");
        createRestorePoint("20260530-001131", true, true, true, true);

        List<RuntimeControlRestoreCandidateRespVO> candidates = candidateService.listRestoreCandidates();

        assertEquals(2, candidates.size());
        RuntimeControlRestoreCandidateRespVO candidate = findCandidate(candidates, "CONTROLLED_RESTORE");
        assertEquals("restore:20260530-001131", candidate.getCandidateId());
        assertEquals("AVAILABLE", candidate.getStatus());
    }

    @Test
    void listRestoreCandidatesShouldScanOnlyRecentBackupPoints() throws Exception {
        int scanLimit = 5;
        for (int index = 0; index < scanLimit + 5; index++) {
            createRestorePoint("20260608-%06d".formatted(120000 + index), true, true, true, true);
        }

        List<RuntimeControlRestoreCandidateRespVO> candidates = candidateService.listRestoreCandidates();

        assertEquals(scanLimit * 2, candidates.size());
        assertEquals("20260608-120009", candidates.get(0).getBackupId());
        assertEquals("20260608-120005", candidates.get(candidates.size() - 1).getBackupId());
        assertFalse(candidates.stream().anyMatch(candidate -> "20260608-120004".equals(candidate.getBackupId())));
    }

    @Test
    void executeRestoreShouldResolveRecoverySetCandidateBeforeDispatch() throws Exception {
        createRestorePoint("20260526-010203", true, true, true, true);
        RuntimeControlServiceImpl service = new RuntimeControlServiceImpl(properties, commandExecutor,
                new RuntimeControlOperationStore(properties), responsibilityService(), candidateService);
        RuntimeControlActionReqVO reqVO = highRiskAction("restore-data");
        reqVO.setSelectedRecoverySetCandidateId(findCandidate(candidateService.listRestoreCandidates(),
                "CONTROLLED_RESTORE").getCandidateId());

        service.executeAction(reqVO, "1001");

        verify(commandExecutor, timeout(1000)).executeDetachedOperation(argThat(command ->
                command.getArguments().contains("--selected-backup-id")
                        && command.getArguments().contains("20260526-010203")), any(), any(), any());
    }

    @Test
    void executeRestoreShouldDispatchWithoutSnapshotEvidence() throws Exception {
        createRestorePoint("20260526-010203", true, true, true, false);
        RuntimeControlServiceImpl service = new RuntimeControlServiceImpl(properties, commandExecutor,
                new RuntimeControlOperationStore(properties), responsibilityService(), candidateService);
        RuntimeControlActionReqVO reqVO = highRiskAction("restore-data");
        reqVO.setSelectedRecoverySetCandidateId(findCandidate(candidateService.listRestoreCandidates(),
                "CONTROLLED_RESTORE").getCandidateId());

        service.executeAction(reqVO, "1001");

        verify(commandExecutor, timeout(1000)).executeDetachedOperation(argThat(command ->
                command.getArguments().contains("--selected-backup-id")
                        && command.getArguments().contains("20260526-010203")), any(), any(), any());
    }

    @Test
    void executeRestoreShouldRejectRehearsalCandidate() throws Exception {
        createRestorePoint("20260526-010203", true, true, true, true);
        RuntimeControlServiceImpl service = new RuntimeControlServiceImpl(properties, commandExecutor,
                new RuntimeControlOperationStore(properties), responsibilityService(), candidateService);
        RuntimeControlActionReqVO reqVO = highRiskAction("restore-data");
        reqVO.setSelectedRecoverySetCandidateId(findCandidate(candidateService.listRestoreCandidates(), "REHEARSAL")
                .getCandidateId());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.executeAction(reqVO, "1001"));

        assertTrue(exception.getMessage().contains("CONTROLLED_RESTORE"));
    }

    private RuntimeControlRestoreCandidateRespVO findCandidate(
            List<RuntimeControlRestoreCandidateRespVO> candidates, String candidateType) {
        return candidates.stream()
                .filter(candidate -> candidateType.equals(candidate.getCandidateType()))
                .findFirst()
                .orElseThrow();
    }

    private void createRestorePoint(String backupId, boolean manifest, boolean checksum,
                                    boolean rehearsal, boolean snapshot) throws Exception {
        Path root = backupPointsRoot.resolve(backupId);
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
        String checksumsText = "sha256  deploy/runtime.env\n";
        if (manifest) {
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                    recoverySetManifest(backupId, "20260526_010203", sha256(checksumsText), rehearsal));
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("dcc-backup-manifest.json"),
                    dccBackupManifest(backupId));
        }
        if (checksum) {
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("checksums.txt"), checksumsText);
        }
        if (rehearsal) {
            String manifestText = java.nio.file.Files.readString(root.resolve("manifest").resolve("manifest.json"));
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("rehearsal-report.json"),
                    """
                    {"status":"PASSED","backupId":"%s","manifestDigest":"%s","chainDigest":"%s",
                     "sourceFingerprint":"serverHost=172.30.30.58;appDir=/opt/intruoyi/runtime;minioBucket=yudao;imageTag=20260526_010203",
                     "targetFingerprint":"environment=test;host=172.30.30.58;imageTag=20260526_010203"}
                    """.formatted(backupId, sha256(manifestText), sha256(dccBackupManifest(backupId))));
        }
        if (snapshot) {
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("现场快照.md"), "snapshot");
        }
    }

    private void createRestorePointWithManifestDeployImageTag(String backupId, String fileImageTag,
                                                              String manifestImageTag) throws Exception {
        Path root = backupPointsRoot.resolve(backupId);
        java.nio.file.Files.createDirectories(root.resolve("deploy"));
        java.nio.file.Files.createDirectories(root.resolve("manifest"));
        java.nio.file.Files.createDirectories(root.resolve("mysql"));
        java.nio.file.Files.createDirectories(root.resolve("objects"));
        if (fileImageTag != null) {
            java.nio.file.Files.writeString(root.resolve("deploy").resolve("image-tag.txt"), fileImageTag);
        }
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("runtime.env"),
                "IMAGE_TAG=" + manifestImageTag + "\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n");
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("docker-compose.yml"), "services: {}\n");
        java.nio.file.Files.writeString(root.resolve("mysql").resolve("ruoyi-vue-pro.sql.gz"), "dump");
        java.nio.file.Files.writeString(root.resolve("objects").resolve("manifest-object-inventory.json"),
                "{\"mode\":\"incremental-manifest\",\"objects\":[]}");
        String checksumsText = "sha256  deploy/runtime.env\n";
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                recoverySetManifest(backupId, manifestImageTag, sha256(checksumsText), true));
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("dcc-backup-manifest.json"),
                dccBackupManifest(backupId));
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("checksums.txt"), checksumsText);
        String manifestText = java.nio.file.Files.readString(root.resolve("manifest").resolve("manifest.json"));
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("rehearsal-report.json"),
                """
                {"status":"PASSED","backupId":"%s","manifestDigest":"%s","chainDigest":"%s",
                 "sourceFingerprint":"serverHost=172.30.30.58;appDir=/opt/intruoyi/runtime;minioBucket=yudao;imageTag=%s",
                 "targetFingerprint":"environment=test;host=172.30.30.58;imageTag=%s"}
                """.formatted(backupId, sha256(manifestText), sha256(dccBackupManifest(backupId)),
                        manifestImageTag, manifestImageTag));
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("现场快照.md"), "snapshot");
    }

    private void createArchiveRestorePoint(String backupId) throws Exception {
        Path root = backupPointsRoot.resolve(backupId);
        java.nio.file.Files.createDirectories(root.resolve("deploy"));
        java.nio.file.Files.createDirectories(root.resolve("manifest"));
        java.nio.file.Files.createDirectories(root.resolve("mysql"));
        java.nio.file.Files.createDirectories(root.resolve("objects"));
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("image-tag.txt"),
                "20260606_ui_code_only_onlyoffice_A_1138");
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("runtime.env"),
                "IMAGE_TAG=20260606_ui_code_only_onlyoffice_A_1138\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n");
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("docker-compose.yml"), "services: {}\n");
        java.nio.file.Files.writeString(root.resolve("mysql").resolve("ruoyi-vue-pro.sql.gz"), "dump");
        java.nio.file.Files.writeString(root.resolve("objects").resolve("objects-yudao.tar"), "archive");
        String checksumsText = "sha256  deploy/runtime.env\n";
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                recoverySetArchiveManifest(backupId, "20260606_ui_code_only_onlyoffice_A_1138",
                        sha256(checksumsText)));
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("checksums.txt"), checksumsText);
    }

    private String recoverySetArchiveManifest(String backupId, String imageTag, String checksumsHash) {
        return """
                {"schemaVersion":"v2","backupId":"%s","targetEnvironment":"test","targetHost":"172.30.30.58","status":"success","deploy":{"imageTag":"%s"},"recoverySet":{"id":"%s","status":"COMPLETE","program":{"imageTag":"%s"},"mysql":{"dumpPath":"mysql/ruoyi-vue-pro.sql.gz"},"minio":{"bucket":"yudao","snapshotPath":"objects/objects-yudao.tar"},"businessFiles":{"snapshotPath":"objects/objects-yudao.tar"},"redis":{"policy":"CLEAR_AND_REBUILD"},"configuration":{"manifestPath":"deploy/runtime.env","composePath":"deploy/docker-compose.yml"},"checksums":{"path":"manifest/checksums.txt","sha256":"%s"}}}
                """.formatted(backupId, imageTag, backupId, imageTag, checksumsHash);
    }

    private void createNonBackupPointDirectory(String name) throws Exception {
        java.nio.file.Files.createDirectories(backupPointsRoot.resolve(name));
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

    private void writeBackupOpsConfigWithoutBackupPointsRoot() {
        try {
            java.nio.file.Files.writeString(Path.of(properties.getBackupOps().getLinuxConfigPath()), """
                    {
                      "servers": {
                        "test": {
                        }
                      }
                    }
                    """);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private RuntimeControlActionReqVO highRiskAction(String action) {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction(action);
        reqVO.setReason("contract test");
        reqVO.setProdConfirmText("PROD");
        reqVO.setTargetEnvironment("test");
        return reqVO;
    }

    private RuntimeOpsResponsibilityService responsibilityService() {
        RuntimeOpsResponsibilityServiceImpl service =
                new RuntimeOpsResponsibilityServiceImpl(new RuntimeOpsOwnerMatrixStore(properties));
        service.createOwner(owner("restore-data", "data-owner", 1002L));
        return service;
    }

    private cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO owner(
            String action, String role, Long ownerUserId) {
        cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO reqVO =
                new cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO();
        reqVO.setEnvironment("test");
        reqVO.setAction(action);
        reqVO.setRole(role);
        reqVO.setRequired(true);
        reqVO.setOwnerUserId(ownerUserId);
        reqVO.setOwnerName("owner-" + ownerUserId);
        return reqVO;
    }

    private void createLegacyRestorePoint(String backupId, String imageTag) throws Exception {
        Path root = backupPointsRoot.resolve(backupId);
        java.nio.file.Files.createDirectories(root.resolve("deploy"));
        java.nio.file.Files.createDirectories(root.resolve("manifest"));
        java.nio.file.Files.createDirectories(root.resolve("mysql"));
        java.nio.file.Files.createDirectories(root.resolve("objects"));
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("image-tag.txt"), imageTag);
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                "{\"backupId\":\"" + backupId + "\",\"imageTag\":\"" + imageTag + "\"}");
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("checksums.txt"),
                "sha256  deploy/runtime.env\n");
        java.nio.file.Files.writeString(root.resolve("mysql").resolve("ruoyi-vue-pro.sql.gz"), "dump");
    }

    private String recoverySetManifest(String backupId, String imageTag, String checksumsHash, boolean rehearsed) {
        String validation = rehearsed
                ? """
                ,"validation":{"mysqlDumpCreated":true,"objectBackupCreated":true,"checksumsGenerated":true,"rehearsalStatus":"PASSED","lastRehearsedAt":"2026-09-19T00:00:00+08:00"}
                """
                : """
                ,"validation":{"mysqlDumpCreated":true,"objectBackupCreated":true,"checksumsGenerated":true,"rehearsalStatus":"NOT_RUN"}
                """;
        return """
                {"schemaVersion":"v2","backupId":"%s","targetEnvironment":"test","targetHost":"172.30.30.58","status":"success","source":{"serverHost":"172.30.30.58","appDir":"/opt/intruoyi/runtime","minioBucket":"yudao"},"deploy":{"imageTag":"%s"},"recoverySet":{"id":"%s","status":"COMPLETE","program":{"imageTag":"%s"},"mysql":{"dumpPath":"mysql/ruoyi-vue-pro.sql.gz"},"minio":{"bucket":"yudao","snapshotPath":"objects/manifest-object-inventory.json"},"businessFiles":{"snapshotPath":"objects/manifest-object-inventory.json"},"redis":{"policy":"CLEAR_AND_REBUILD"},"configuration":{"manifestPath":"deploy/runtime.env","composePath":"deploy/docker-compose.yml"},"checksums":{"path":"manifest/checksums.txt","sha256":"%s"}}%s}
                """.formatted(backupId, imageTag, backupId, imageTag, checksumsHash, validation);
    }

    private String dccBackupManifest(String backupId) {
        return """
                {"schemaVersion":"dcc-backup-manifest-v1","backupId":"%s","targetEnvironment":"test","backupMode":"incremental","chainStatus":"COMPLETE","changeSummary":{"addedRecords":1,"changedRecords":0,"deletedRecords":0,"invalidatedRecords":0,"addedObjects":1,"changedObjects":0,"reusedObjects":2,"tombstoneObjects":0}}
                """.formatted(backupId);
    }

    private String recoverySetManifestWithoutTargetProof(String backupId, String imageTag, String checksumsHash) {
        return """
                {"schemaVersion":"v2","backupId":"%s","status":"success","deploy":{"imageTag":"%s"},"recoverySet":{"id":"%s","status":"COMPLETE","program":{"imageTag":"%s"},"mysql":{"dumpPath":"mysql/ruoyi-vue-pro.sql.gz"},"minio":{"bucket":"yudao","snapshotPath":"objects/manifest-object-inventory.json"},"businessFiles":{"snapshotPath":"objects/manifest-object-inventory.json"},"redis":{"policy":"CLEAR_AND_REBUILD"},"configuration":{"manifestPath":"deploy/runtime.env","composePath":"deploy/docker-compose.yml"},"checksums":{"path":"manifest/checksums.txt","sha256":"%s"}}}
                """.formatted(backupId, imageTag, backupId, imageTag, checksumsHash);
    }

    private String sha256(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
