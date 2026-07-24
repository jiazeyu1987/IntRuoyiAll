package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRollbackCandidateRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class RuntimeRollbackCandidateServiceImplTest extends BaseMockitoUnitTest {

    @TempDir
    private Path tempDir;

    @Mock
    private RuntimeControlCommandExecutor commandExecutor;

    private RuntimeControlProperties properties;
    private RuntimeOpsCandidateService candidateService;
    private Path backupPointsRoot;
    private Path releasePackagesRoot;

    @BeforeEach
    void setUp() throws Exception {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        properties.getBackupOps().setExecutionMode("linux-local");
        properties.getBackupOps().setLinuxScriptPath("/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh");
        backupPointsRoot = tempDir.resolve("nas-backup-points");
        java.nio.file.Files.createDirectories(backupPointsRoot);
        properties.getBackupOps().setNasBackupPointsRoot("nas-backup-points");
        releasePackagesRoot = tempDir.resolve("nas-release-packages");
        java.nio.file.Files.createDirectories(releasePackagesRoot);
        properties.getReleasePackage().setNasReleaseRoot("nas-release-packages");
        candidateService = new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
    }

    @Test
    void listRollbackCandidatesShouldReadStandardReleasePackagesFromNasReleaseRoot() throws Exception {
        createReleasePackage("26-05-30_00-11-31", "26-05-30 00:11:31", "26-05-30_00-11-31");
        createBackupPoint("20260530-001131", "20260530_001131", true);

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(1, candidates.size());
        RuntimeControlRollbackCandidateRespVO candidate = candidates.get(0);
        assertEquals("rollback:26-05-30_00-11-31", candidate.getCandidateId());
        assertEquals("26-05-30 00:11:31", candidate.getReleaseTag());
        assertEquals("26-05-30_00-11-31", candidate.getImageTag());
        assertEquals("nas-release-packages/26-05-30_00-11-31/release-manifest.json",
                candidate.getManifestPath());
        assertEquals("nas-release-packages/26-05-30_00-11-31/prod-latest.json",
                candidate.getProdHistoryPath());
        assertEquals("COMPATIBLE", candidate.getCompatibilityStatus());
        assertEquals("nas-release-packages/26-05-30_00-11-31/rollback-compatibility.json",
                candidate.getCompatibilityEvidencePath());
        assertEquals("2026-05-30T00:12:00Z", candidate.getCompatibilityCheckedAt());
        assertEquals("db=minio=redis=config=onlyoffice compatible",
                candidate.getCompatibilitySummary());
        assertEquals("AVAILABLE", candidate.getStatus());
    }

    @Test
    void listRollbackCandidatesShouldBlockReleasePackageWhenReleaseManifestIsMissing() throws Exception {
        createReleasePackageDirectory("26-05-30_00-11-31");

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(1, candidates.size());
        assertEquals("BLOCKED", candidates.get(0).getStatus());
        assertTrue(candidates.get(0).getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("release-manifest.json")));
        assertTrue(candidates.get(0).getBlockedReasons().stream()
                .noneMatch(reason -> reason.equals("缺少 manifest.json")));
    }

    @Test
    void listRollbackCandidatesShouldBlockReleasePackageWithoutProductionHistory() throws Exception {
        createReleasePackageWithoutProdHistory("26-05-30_00-11-31", "26-05-30 00:11:31",
                "26-05-30_00-11-31");

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(1, candidates.size());
        assertEquals("BLOCKED", candidates.get(0).getStatus());
        assertTrue(candidates.get(0).getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("正式服发布历史")));
    }

    @Test
    void listRollbackCandidatesShouldBlockReleasePackageWithoutCompatibilityEvidence() throws Exception {
        createReleasePackageWithoutCompatibility("26-05-30_00-11-31", "26-05-30 00:11:31",
                "26-05-30_00-11-31");

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(1, candidates.size());
        assertEquals("BLOCKED", candidates.get(0).getStatus());
        assertTrue(candidates.get(0).getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("rollback-compatibility.json")));
    }

    @Test
    void listRollbackCandidatesShouldBlockCompatibilityEvidenceWithoutPackageDirectoryName() throws Exception {
        Path root = createReleasePackageDirectory("26-05-30_00-11-31");
        java.nio.file.Files.writeString(root.resolve("release-manifest.json"),
                "{\"releaseTag\":\"26-05-30 00:11:31\",\"packageDirectoryName\":\"26-05-30_00-11-31\"}");
        java.nio.file.Files.writeString(root.resolve("prod-latest.json"),
                "{\"releaseTag\":\"26-05-30 00:11:31\",\"packageDirectoryName\":\"26-05-30_00-11-31\","
                        + "\"action\":\"deploy\",\"environment\":\"prod\"}");
        java.nio.file.Files.writeString(root.resolve("rollback-compatibility.json"),
                "{\"schemaVersion\":\"v1\",\"status\":\"COMPATIBLE\",\"checkedAt\":\"2026-05-30T00:12:00Z\","
                        + "\"summary\":\"compatible\"}");

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(1, candidates.size());
        assertEquals("BLOCKED", candidates.get(0).getStatus());
        assertTrue(candidates.get(0).getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("packageDirectoryName")));
    }

    @Test
    void listRollbackCandidatesShouldReadReleasePackageRootFromNasConfig() throws Exception {
        createReleasePackage("26-05-29_21-05-42", "26-05-29 21:05:42", "26-05-29_21-05-42");
        java.nio.file.Files.createDirectories(tempDir.resolve("release-packages"));

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(1, candidates.size());
        assertEquals("nas-release-packages/26-05-29_21-05-42/release-manifest.json",
                candidates.get(0).getManifestPath());
    }

    @Test
    void listRollbackCandidatesShouldFailWhenNasReleaseRootConfigIsMissing() {
        properties.getReleasePackage().setNasReleaseRoot("");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> candidateService.listRollbackCandidates());

        assertTrue(exception.getMessage().contains("nasReleaseRoot"));
    }

    @Test
    void listRollbackCandidatesShouldBlockWhenReleaseManifestPackageDirectoryNameIsMissing() throws Exception {
        Path root = createReleasePackageDirectory("26-05-30_00-11-31");
        java.nio.file.Files.writeString(root.resolve("release-manifest.json"),
                "{\"releaseTag\":\"26-05-30 00:11:31\"}");

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(1, candidates.size());
        assertEquals("BLOCKED", candidates.get(0).getStatus());
        assertTrue(candidates.get(0).getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("packageDirectoryName")));
    }

    @Test
    void listRollbackCandidatesShouldExposeServerGeneratedCandidateIdAndImageTag() throws Exception {
        createReleasePackage("26-05-29_21-05-42", "26-05-29 21:05:42", "26-05-29_21-05-42");

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertFalse(candidates.isEmpty());
        RuntimeControlRollbackCandidateRespVO candidate = candidates.get(0);
        assertEquals("AVAILABLE", candidate.getStatus());
        assertEquals("26-05-29_21-05-42", candidate.getBackupId());
        assertEquals("26-05-29 21:05:42", candidate.getReleaseTag());
        assertEquals("26-05-29_21-05-42", candidate.getImageTag());
        assertFalse(candidate.getCandidateId().isBlank());
    }

    @Test
    void listRollbackCandidatesShouldSortReleasePackagesDescending() throws Exception {
        createReleasePackage("26-05-29_19-12-42", "26-05-29 19:12:42", "26-05-29_19-12-42");
        createReleasePackage("26-05-30_00-11-31", "26-05-30 00:11:31", "26-05-30_00-11-31");

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(List.of("rollback:26-05-30_00-11-31", "rollback:26-05-29_19-12-42"),
                candidates.stream().map(RuntimeControlRollbackCandidateRespVO::getCandidateId).toList());
    }

    @Test
    void listRollbackCandidatesShouldScanOnlyRecentReleasePackages() throws Exception {
        int scanLimit = 5;
        for (int index = 0; index < scanLimit + 5; index++) {
            String directoryName = "26-06-08_12-00-%02d".formatted(index);
            createReleasePackage(directoryName, "26-06-08 12:00:%02d".formatted(index), directoryName);
        }

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(scanLimit, candidates.size());
        assertEquals("26-06-08_12-00-09", candidates.get(0).getBackupId());
        assertEquals("26-06-08_12-00-05", candidates.get(scanLimit - 1).getBackupId());
        assertFalse(candidates.stream().anyMatch(candidate -> "26-06-08_12-00-04".equals(candidate.getBackupId())));
    }

    @Test
    void listRollbackCandidatesShouldBlockWhenManifestPackageDirectoryDiffersFromDirectory() throws Exception {
        createReleasePackage("26-05-30_00-11-31", "26-05-30 00:11:31", "26-05-30_00-11-32");

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(1, candidates.size());
        assertEquals("BLOCKED", candidates.get(0).getStatus());
        assertTrue(candidates.get(0).getBlockedReasons().stream()
                .anyMatch(reason -> reason.contains("packageDirectoryName")));
    }

    @Test
    void listRollbackCandidatesShouldIgnoreBackupPointDirectories() throws Exception {
        createBackupPoint("20260530-001131", "20260530_001131", true);
        createReleasePackage("26-05-30_00-11-31", "26-05-30 00:11:31", "26-05-30_00-11-31");

        List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();

        assertEquals(1, candidates.size());
        assertEquals("rollback:26-05-30_00-11-31", candidates.get(0).getCandidateId());
        assertEquals("AVAILABLE", candidates.get(0).getStatus());
    }

    @Test
    void executeRollbackShouldResolveImageTagFromCandidateBeforeDispatch() throws Exception {
        createReleasePackage("26-05-30_00-11-31", "26-05-30 00:11:31", "26-05-30_00-11-31");
        RuntimeControlServiceImpl service = new RuntimeControlServiceImpl(properties, commandExecutor,
                new RuntimeControlOperationStore(properties), responsibilityService(), candidateService);
        RuntimeControlActionReqVO reqVO = highRiskAction("rollback-app");
        reqVO.setSelectedImageCandidateId(candidateService.listRollbackCandidates().get(0).getCandidateId());

        service.executeAction(reqVO, "1001");

        verify(commandExecutor, timeout(1000)).executeDetachedOperation(argThat(command ->
                command.getArguments().contains("--selected-image-tag")
                        && command.getArguments().contains("26-05-30_00-11-31")), any(), any(), any());
    }

    @Test
    void executeRollbackShouldRejectUnknownCandidateBeforeDispatch() {
        RuntimeControlServiceImpl service = new RuntimeControlServiceImpl(properties, commandExecutor,
                new RuntimeControlOperationStore(properties), responsibilityService(), candidateService);
        RuntimeControlActionReqVO reqVO = highRiskAction("rollback-app");
        reqVO.setSelectedImageCandidateId("rollback:unknown");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.executeAction(reqVO, "1001"));

        assertTrue(exception.getMessage().contains("候选"));
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeRollbackShouldRejectBlockedCandidateBeforeDispatch() throws Exception {
        createReleasePackageDirectory("26-05-30_00-11-31");
        RuntimeControlServiceImpl service = new RuntimeControlServiceImpl(properties, commandExecutor,
                new RuntimeControlOperationStore(properties), responsibilityService(), candidateService);
        RuntimeControlActionReqVO reqVO = highRiskAction("rollback-app");
        reqVO.setSelectedImageCandidateId(candidateService.listRollbackCandidates().get(0).getCandidateId());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.executeAction(reqVO, "1001"));
        assertTrue(exception.getMessage().contains("selectedImageCandidateId"));
        assertTrue(exception.getMessage().contains("manifest"));
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    private void createBackupPoint(String backupId, String imageTag, boolean manifest) throws Exception {
        Path root = backupPointsRoot.resolve(backupId);
        java.nio.file.Files.createDirectories(root.resolve("deploy"));
        java.nio.file.Files.createDirectories(root.resolve("manifest"));
        java.nio.file.Files.writeString(root.resolve("deploy").resolve("image-tag.txt"), imageTag);
        if (manifest) {
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                    "{\"backupId\":\"" + backupId + "\",\"imageTag\":\"" + imageTag + "\"}");
        }
    }

    private void createBackupPointWithManifestDeployImageTag(String backupId, String fileImageTag,
                                                             String manifestImageTag) throws Exception {
        Path root = backupPointsRoot.resolve(backupId);
        java.nio.file.Files.createDirectories(root.resolve("deploy"));
        java.nio.file.Files.createDirectories(root.resolve("manifest"));
        if (fileImageTag != null) {
            java.nio.file.Files.writeString(root.resolve("deploy").resolve("image-tag.txt"), fileImageTag);
        }
        java.nio.file.Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                "{\"backupId\":\"" + backupId + "\",\"deploy\":{\"imageTag\":\"" + manifestImageTag + "\"}}");
    }

    private void createNonBackupPointDirectory(String name) throws Exception {
        java.nio.file.Files.createDirectories(backupPointsRoot.resolve(name));
    }

    private void createReleasePackage(String directoryName, String releaseTag, String packageDirectoryName) throws Exception {
        createReleasePackageWithoutProdHistory(directoryName, releaseTag, packageDirectoryName);
        Path root = releasePackagesRoot.resolve(directoryName);
        java.nio.file.Files.writeString(root.resolve("prod-latest.json"),
                "{\"releaseTag\":\"" + releaseTag + "\",\"packageDirectoryName\":\"" + directoryName
                        + "\",\"action\":\"deploy\",\"environment\":\"prod\"}");
    }

    private void createReleasePackageWithoutProdHistory(String directoryName, String releaseTag,
                                                        String packageDirectoryName) throws Exception {
        Path root = createReleasePackageDirectory(directoryName);
        java.nio.file.Files.writeString(root.resolve("release-manifest.json"),
                "{\"releaseTag\":\"" + releaseTag + "\",\"packageDirectoryName\":\"" + packageDirectoryName + "\"}");
        writeRollbackCompatibility(root, packageDirectoryName);
    }

    private void createReleasePackageWithoutCompatibility(String directoryName, String releaseTag,
                                                          String packageDirectoryName) throws Exception {
        createReleasePackageWithoutProdHistory(directoryName, releaseTag, packageDirectoryName);
        Path root = releasePackagesRoot.resolve(directoryName);
        java.nio.file.Files.writeString(root.resolve("prod-latest.json"),
                "{\"releaseTag\":\"" + releaseTag + "\",\"packageDirectoryName\":\"" + directoryName
                        + "\",\"action\":\"deploy\",\"environment\":\"prod\"}");
        java.nio.file.Files.deleteIfExists(root.resolve("rollback-compatibility.json"));
    }

    private void writeRollbackCompatibility(Path root, String packageDirectoryName) throws Exception {
        java.nio.file.Files.writeString(root.resolve("rollback-compatibility.json"),
                """
                        {"schemaVersion":"v1","packageDirectoryName":"%s","status":"COMPATIBLE","checkedAt":"2026-05-30T00:12:00Z","summary":"db=minio=redis=config=onlyoffice compatible","compatibility":{"databaseSchema":"compatible","minio":"compatible","redis":"compatible","businessFiles":"compatible","config":"compatible","onlyOffice":"compatible"}}
                        """.formatted(packageDirectoryName));
    }

    private Path createReleasePackageDirectory(String directoryName) throws Exception {
        Path root = releasePackagesRoot.resolve(directoryName);
        java.nio.file.Files.createDirectories(root);
        return root;
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
        service.createOwner(owner("rollback-app", "release-owner", 1001L));
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
}
