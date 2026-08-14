package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasFileReadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeBackupDrillServiceImplTest {

    @TempDir
    private Path tempDir;

    private RuntimeControlProperties properties;
    private RuntimeBackupDrillServiceImpl backupDrillService;
    private Path backupPointsRoot;

    @BeforeEach
    void setUp() {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        properties.getBackupOps().setExecutionMode("linux-local");
        backupPointsRoot = tempDir.resolve("nas-backup-points");
        properties.getBackupOps().setNasBackupPointsRoot("nas-backup-points");
        backupDrillService = new RuntimeBackupDrillServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
    }

    @Test
    void listBackupPointsShouldReadBackupFolderThroughNasBrowserService() {
        NasBrowserService nasBrowserService = mock(NasBrowserService.class);
        properties.getBackupOps().setNasBackupPointsRoot("Backup");
        RuntimeBackupDrillServiceImpl service = new RuntimeBackupDrillServiceImpl(properties, nasBrowserService);
        when(nasBrowserService.listFiles(any(), eq("Backup"))).thenReturn(nasList("Backup",
                dir("20260526-010203", "Backup/20260526-010203")));
        when(nasBrowserService.listFiles(any(), eq("Backup/20260526-010203/manifest"))).thenReturn(nasList(
                "Backup/20260526-010203/manifest",
                file("manifest.json", "Backup/20260526-010203/manifest/manifest.json"),
                file("checksums.txt", "Backup/20260526-010203/manifest/checksums.txt"),
                file("rehearsal-report.json", "Backup/20260526-010203/manifest/rehearsal-report.json"),
                file("现场快照.md", "Backup/20260526-010203/manifest/现场快照.md")));
        when(nasBrowserService.readFile(any(), eq("Backup/20260526-010203/manifest/manifest.json")))
                .thenReturn(textFile("manifest.json", "{\"backupId\":\"20260526-010203\"}"));
        when(nasBrowserService.readFile(any(), eq("Backup/20260526-010203/manifest/checksums.txt")))
                .thenReturn(textFile("checksums.txt", "sha256  manifest.json"));
        when(nasBrowserService.readFile(any(), eq("Backup/20260526-010203/manifest/rehearsal-report.json")))
                .thenReturn(textFile("rehearsal-report.json", "{\"status\":\"PASSED\",\"verifiedAt\":\"2026-05-26T01:02:03\"}"));

        List<RuntimeControlBackupPointRespVO> backupPoints = service.listBackupPoints();

        assertEquals(1, backupPoints.size());
        assertEquals("20260526-010203", backupPoints.get(0).getBackupId());
        assertEquals("Backup/20260526-010203/manifest/manifest.json", backupPoints.get(0).getManifestPath());
        verify(nasBrowserService).listFiles(any(), eq("Backup"));
    }

    @Test
    void listBackupPointsShouldReadNasRootAndExposeRecoverableEvidence() throws Exception {
        createBackupPoint("20260526-010203", true, true, true, true, true);
        Files.createDirectories(tempDir.resolve("stateDir-backup-points"));

        List<RuntimeControlBackupPointRespVO> backupPoints = backupDrillService.listBackupPoints();

        assertEquals(1, backupPoints.size());
        RuntimeControlBackupPointRespVO backupPoint = backupPoints.get(0);
        assertEquals("20260526-010203", backupPoint.getBackupId());
        assertEquals("RECOVERABLE", backupPoint.getRecoverabilityStatus());
        assertEquals("nas-backup-points/20260526-010203/manifest/manifest.json",
                backupPoint.getManifestPath());
        assertEquals("20260526_010203", backupPoint.getImageTag());
        assertEquals(LocalDateTime.of(2026, 5, 26, 1, 2, 3), backupPoint.getCompletedAt());
        assertEquals("incremental-manifest", backupPoint.getBackupMode());
        assertEquals(5, backupPoint.getRetentionKeepLast());
        assertEquals(30, backupPoint.getRetentionKeepDays());
        assertEquals(90, backupPoint.getRetentionMaxNasUsedPercent());
        assertEquals(1, backupPoint.getObjectAddedCount());
        assertEquals(2, backupPoint.getObjectModifiedCount());
        assertEquals(3, backupPoint.getObjectDeletedCount());
        assertEquals(4, backupPoint.getObjectReusedCount());
        assertEquals("incremental", backupPoint.getDccBackupMode());
        assertEquals("COMPLETE", backupPoint.getDccChainStatus());
        assertEquals("1", backupPoint.getDccChangeSummary().get("addedRecords"));
        assertEquals("2", backupPoint.getDccChangeSummary().get("changedRecords"));
        assertEquals("3", backupPoint.getDccChangeSummary().get("deletedRecords"));
        assertEquals("4", backupPoint.getDccChangeSummary().get("invalidatedRecords"));
        assertEquals("PASSED", backupPoint.getRehearsalStatus());
        assertNotNull(backupPoint.getLastVerifiedAt());
        assertTrue(backupPoint.getUnrecoverableReasons().isEmpty());
    }

    @Test
    void listBackupPointsShouldExposeNullCompletedAtWhenManifestCompletedAtIsInvalid() throws Exception {
        createBackupPoint("20260526-010203", true, true, false, false, false);
        Files.writeString(backupPointsRoot.resolve("20260526-010203").resolve("manifest").resolve("manifest.json"),
                """
                        {"backupId":"20260526-010203","targetEnvironment":"test","targetHost":"172.30.30.58","time":{"completedAt":"not-a-date"},"deploy":{"imageTag":"20260526_010203"},"backupStrategy":{"mode":"incremental-manifest"},"retentionPolicy":{"keepLast":5,"keepDays":30,"maxNasUsedPercent":90},"objectDeltaStats":{"addedCount":1,"modifiedCount":2,"deletedCount":3,"reusedCount":4}}
                        """, StandardCharsets.UTF_8);

        RuntimeControlBackupPointRespVO backupPoint = backupDrillService.listBackupPoints().get(0);

        assertNull(backupPoint.getCompletedAt());
        assertEquals("RECOVERABLE", backupPoint.getRecoverabilityStatus());
    }

    @Test
    void listBackupPointsShouldNotRequireRehearsalEvidenceForRecoverability() throws Exception {
        createBackupPoint("20260526-010203", true, true, false, false, false);

        RuntimeControlBackupPointRespVO backupPoint = backupDrillService.listBackupPoints().get(0);

        assertEquals("RECOVERABLE", backupPoint.getRecoverabilityStatus());
        assertTrue(backupPoint.getUnrecoverableReasons().isEmpty());
    }

    @Test
    void listBackupPointsShouldExposeUnrecoverableWhenEvidenceIsMissingOrInvalid() throws Exception {
        createBackupPoint("20260526-010203", false, false, false, false, false);
        Files.writeString(backupPointsRoot.resolve("20260526-010203").resolve("manifest").resolve("manifest.json"),
                "{invalid-json", StandardCharsets.UTF_8);

        RuntimeControlBackupPointRespVO backupPoint = backupDrillService.listBackupPoints().get(0);

        assertEquals("UNRECOVERABLE", backupPoint.getRecoverabilityStatus());
        assertTrue(backupPoint.getUnrecoverableReasons().stream().anyMatch(reason -> reason.contains("manifest")));
        assertTrue(backupPoint.getUnrecoverableReasons().stream().anyMatch(reason -> reason.contains("checksum")));
        assertFalse(backupPoint.getUnrecoverableReasons().stream().anyMatch(reason -> reason.contains("演练")));
        assertFalse(backupPoint.getUnrecoverableReasons().stream().anyMatch(reason -> reason.contains("现场快照")));
    }

    @Test
    void listBackupPointsShouldExposeUnrecoverableWhenManifestTargetProofMissing() throws Exception {
        createBackupPoint("20260526-010203", true, true, false, false, false);
        Files.writeString(backupPointsRoot.resolve("20260526-010203").resolve("manifest").resolve("manifest.json"),
                """
                        {"backupId":"20260526-010203","deploy":{"imageTag":"20260526_010203"},"backupStrategy":{"mode":"incremental-manifest"},"retentionPolicy":{"keepLast":5,"keepDays":30,"maxNasUsedPercent":90},"objectDeltaStats":{"addedCount":1,"modifiedCount":2,"deletedCount":3,"reusedCount":4}}
                        """, StandardCharsets.UTF_8);

        RuntimeControlBackupPointRespVO backupPoint = backupDrillService.listBackupPoints().get(0);

        assertEquals("UNRECOVERABLE", backupPoint.getRecoverabilityStatus());
        assertTrue(backupPoint.getUnrecoverableReasons().stream()
                .anyMatch(reason -> reason.contains("targetEnvironment") && reason.contains("172.30.30.58")));
    }

    @Test
    void getBackupPointShouldFailWhenBackupIdDoesNotExist() throws Exception {
        createBackupPoint("20260526-010203", true, true, true, true, true);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> backupDrillService.getBackupPoint("missing-backup"));

        assertTrue(exception.getMessage().contains("备份点"));
    }

    private void createBackupPoint(String backupId, boolean manifest, boolean checksum,
                                   boolean rehearsal, boolean snapshot, boolean rehearsalPassed) throws Exception {
        Path root = backupPointsRoot.resolve(backupId);
        Files.createDirectories(root.resolve("manifest"));
        if (manifest) {
            Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                    """
                            {"backupId":"%s","targetEnvironment":"test","targetHost":"172.30.30.58","time":{"completedAt":"2026-05-26T01:02:03"},"deploy":{"imageTag":"20260526_010203"},"backupStrategy":{"mode":"incremental-manifest"},"retentionPolicy":{"keepLast":5,"keepDays":30,"maxNasUsedPercent":90},"objectDeltaStats":{"addedCount":1,"modifiedCount":2,"deletedCount":3,"reusedCount":4}}
                            """.formatted(backupId), StandardCharsets.UTF_8);
            Files.writeString(root.resolve("manifest").resolve("dcc-backup-manifest.json"),
                    """
                            {"schemaVersion":"dcc-backup-manifest-v1","backupId":"%s","targetEnvironment":"test","backupMode":"incremental","chainStatus":"COMPLETE","changeSummary":{"addedRecords":1,"changedRecords":2,"deletedRecords":3,"invalidatedRecords":4,"addedObjects":5,"changedObjects":6,"reusedObjects":7,"tombstoneObjects":8}}
                            """.formatted(backupId), StandardCharsets.UTF_8);
        }
        if (checksum) {
            Files.writeString(root.resolve("manifest").resolve("checksums.txt"),
                    "sha256  manifest.json", StandardCharsets.UTF_8);
        }
        if (rehearsal) {
            Files.writeString(root.resolve("manifest").resolve("rehearsal-report.json"),
                    """
                            {"status":"%s","verifiedAt":"%s"}
                            """.formatted(rehearsalPassed ? "PASSED" : "FAILED", LocalDateTime.now()),
                    StandardCharsets.UTF_8);
        }
        if (snapshot) {
            Files.writeString(root.resolve("manifest").resolve("现场快照.md"), "snapshot", StandardCharsets.UTF_8);
        }
    }

    private Path writeBackupOpsConfig(Path backupPointsRoot) {
        try {
            Files.createDirectories(tempDir.resolve("backup-ops-config"));
            Path configPath = tempDir.resolve("backup-ops-config").resolve("backup-ops.config.json");
            Files.writeString(configPath, """
                    {
                      "servers": {
                        "test": {
                          "backupPointsRoot": "%s"
                        }
                      }
                    }
                    """.formatted(backupPointsRoot.toString().replace("\\", "\\\\")), StandardCharsets.UTF_8);
            return configPath;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static FileNasListRespVO nasList(String currentPath, FileNasListRespVO.Item... items) {
        return new FileNasListRespVO()
                .setCurrentPath(currentPath)
                .setItems(List.of(items));
    }

    private static FileNasListRespVO.Item dir(String name, String path) {
        return new FileNasListRespVO.Item()
                .setName(name)
                .setPath(path)
                .setDir(true)
                .setSize(0L);
    }

    private static FileNasListRespVO.Item file(String name, String path) {
        return new FileNasListRespVO.Item()
                .setName(name)
                .setPath(path)
                .setDir(false)
                .setSize(1L);
    }

    private static NasFileReadResult textFile(String name, String text) {
        return new NasFileReadResult(name, name, "text/plain", text.getBytes(StandardCharsets.UTF_8));
    }
}
