package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeBackupDrillService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeBackupNasRepository;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class BackupEvidenceExportServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BackupPlanService backupPlanService;
    private RuntimeBackupDrillService backupDrillService;
    private RuntimeBackupNasRepository backupRepository;
    private RuntimeControlOperationStore operationStore;
    private BackupEvidenceExportService service;

    @BeforeEach
    void setUp() {
        backupPlanService = mock(BackupPlanService.class);
        backupDrillService = mock(RuntimeBackupDrillService.class);
        backupRepository = mock(RuntimeBackupNasRepository.class);
        operationStore = mock(RuntimeControlOperationStore.class);
        service = new BackupEvidenceExportService(backupPlanService, backupDrillService, backupRepository, operationStore);
    }

    @Test
    void exportLatestShouldCreateFixedPassEvidencePackage() throws Exception {
        BackupPlanStatusRespVO status = new BackupPlanStatusRespVO();
        status.setHealthStatus("正常");
        status.setPlanStatus("已开启");
        status.setFullSchedule("SUN 01:00");
        status.setIncrementalSchedule("02:00");
        status.setRetentionSource("质量批准的记录保存期限矩阵 QA-RET-2026-01");
        status.setQualityApprovalRef("QA-SIGN-20260907");
        status.setRepositoryEnvironment("test");
        status.setMaxFreshnessHours(25);
        RuntimeControlBackupPointRespVO point = backupPoint("20260907-020000", "RECOVERABLE", "PASSED");
        status.setLatestBackupPoint(point);
        when(backupPlanService.getStatus()).thenReturn(status);
        when(backupDrillService.listBackupPoints()).thenReturn(List.of(point));
        when(backupRepository.isRegularFile(anyString())).thenReturn(true);
        when(backupRepository.readText("Backup/manifest.json")).thenReturn("""
                {"backupId":"20260907-020000","backupKind":"INCREMENTAL","baseBackupId":"20260907-010000",
                 "parentBackupId":"20260907-010000","status":"success",
                 "mysqlEvidence":{"segments":[{"path":"mysql/binlog/mysql-bin.000001.sql.gz","size":123,"sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]},
                 "objects":[{"path":"dcc/sample.pdf","repositoryKey":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","size":456,"sha256":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","status":"active"}]}
                """);
        when(backupRepository.readText("Backup/checksums.txt")).thenReturn("abc  deploy/runtime.env\n");
        when(backupRepository.readText("Backup/rehearsal-report.json")).thenReturn("""
                {"status":"PASSED","completedAt":"2026-09-07T02:30:00+08:00"}
                """);
        RuntimeControlOperationRespVO operation = new RuntimeControlOperationRespVO();
        operation.setOperationId("op-1");
        operation.setAction("backup-now");
        operation.setStatus("SUCCESS");
        operation.setSummary("password=test-password");
        when(operationStore.listLatest(100)).thenReturn(List.of(operation));

        BackupEvidenceExportService.ExportResult result = service.exportLatest();
        Map<String, byte[]> entries = unzip(result.content());

        assertEquals(List.of(
                "审查摘要.txt",
                "backup-plan.json",
                "scheduler-status.json",
                "backup-chain.json",
                "integrity-result.json",
                "rehearsal-report.json",
                "operations.json",
                "evidence-manifest.json",
                "evidence-manifest.sha256"
        ), entries.keySet().stream().toList());
        assertTrue(new String(entries.get("审查摘要.txt"), StandardCharsets.UTF_8).contains("总体结论: PASS"));
        JsonNode manifest = objectMapper.readTree(entries.get("evidence-manifest.json"));
        assertEquals("PASS", manifest.path("overallVerdict").asText());
        assertTrue(manifest.path("chainId").asText().contains("20260907-010000"));
        assertEquals(7, manifest.path("files").size());
        for (JsonNode entry : manifest.path("files")) {
            byte[] file = entries.get(entry.path("path").asText());
            assertEquals(java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file)),
                    entry.path("sha256").asText());
        }
        assertEquals(java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(entries.get("evidence-manifest.json"))),
                new String(entries.get("evidence-manifest.sha256"), StandardCharsets.US_ASCII).trim());
        JsonNode chain = objectMapper.readTree(entries.get("backup-chain.json"));
        assertEquals(3, chain.path("payloadIndex").size());
        assertTrue(chain.path("payloadIndex").toString().contains("mysql-bin.000001.sql.gz"));
        assertTrue(chain.path("payloadIndex").toString().contains("dcc/sample.pdf"));
        assertTrue(result.filename().startsWith("IntRuoyi-test-backup-evidence-"));
        assertTrue(result.filename().endsWith(".zip"));
        assertFalse(new String(entries.get("operations.json"), StandardCharsets.UTF_8).contains("test-password"));
        assertFalse(new String(result.content(), StandardCharsets.ISO_8859_1).contains("test-password"));
    }

    @Test
    void exportLatestShouldExposeBlockedInsteadOfDefaultPass() throws Exception {
        BackupPlanStatusRespVO status = new BackupPlanStatusRespVO();
        status.setHealthStatus("配置异常");
        status.setPlanStatus("已关闭");
        status.setBlockedReason("最近成功备份点缺失");
        when(backupPlanService.getStatus()).thenReturn(status);
        when(backupDrillService.listBackupPoints()).thenReturn(List.of());
        when(operationStore.listLatest(100)).thenReturn(List.of());

        BackupEvidenceExportService.ExportResult result = service.exportLatest();
        Map<String, byte[]> entries = unzip(result.content());

        String summary = new String(entries.get("审查摘要.txt"), StandardCharsets.UTF_8);
        assertTrue(summary.contains("总体结论: BLOCKED"));
        assertTrue(summary.contains("最近成功备份点缺失"));
        JsonNode manifest = objectMapper.readTree(entries.get("evidence-manifest.json"));
        assertEquals("BLOCKED", manifest.path("overallVerdict").asText());
        assertTrue(manifest.path("blockers").toString().contains("最近成功备份点缺失"));
        assertEquals(9, entries.size());
    }

    @Test
    void exportLatestShouldBlockWhenSourceManifestOrChecksumIsMissing() throws Exception {
        BackupPlanStatusRespVO status = new BackupPlanStatusRespVO();
        status.setHealthStatus("正常");
        status.setPlanStatus("已开启");
        status.setFullSchedule("SUN 01:00");
        status.setIncrementalSchedule("02:00");
        status.setRetentionSource("质量批准的记录保存期限矩阵 QA-RET-2026-01");
        status.setQualityApprovalRef("QA-SIGN-20260907");
        status.setRepositoryEnvironment("test");
        status.setMaxFreshnessHours(25);
        RuntimeControlBackupPointRespVO point = backupPoint("20260907-020000", "RECOVERABLE", "PASSED");
        when(backupPlanService.getStatus()).thenReturn(status);
        when(backupDrillService.listBackupPoints()).thenReturn(List.of(point));
        when(backupRepository.isRegularFile("Backup/manifest.json")).thenReturn(false);
        when(backupRepository.isRegularFile("Backup/checksums.txt")).thenReturn(false);
        when(backupRepository.isRegularFile("Backup/rehearsal-report.json")).thenReturn(true);
        when(backupRepository.readText("Backup/rehearsal-report.json")).thenReturn("""
                {"status":"PASSED","completedAt":"2026-09-07T02:30:00+08:00"}
                """);
        when(operationStore.listLatest(100)).thenReturn(List.of());

        BackupEvidenceExportService.ExportResult result = service.exportLatest();
        Map<String, byte[]> entries = unzip(result.content());

        JsonNode manifest = objectMapper.readTree(entries.get("evidence-manifest.json"));
        assertEquals("BLOCKED", manifest.path("overallVerdict").asText());
        assertTrue(manifest.path("blockers").toString().contains("最新备份点 manifest 缺失"));
        assertTrue(manifest.path("blockers").toString().contains("最新备份点 checksum 清单缺失"));
    }

    private RuntimeControlBackupPointRespVO backupPoint(String backupId, String recoverability, String rehearsal) {
        RuntimeControlBackupPointRespVO point = new RuntimeControlBackupPointRespVO();
        point.setBackupId(backupId);
        point.setCompletedAt(LocalDateTime.of(2026, 9, 7, 2, 0));
        point.setRecoverabilityStatus(recoverability);
        point.setRehearsalStatus(rehearsal);
        point.setBackupKind("INCREMENTAL");
        point.setBaseBackupId("20260907-010000");
        point.setParentBackupId("20260907-010000");
        point.setManifestPath("Backup/manifest.json");
        point.setChecksumPath("Backup/checksums.txt");
        point.setRehearsalReportPath("Backup/rehearsal-report.json");
        return point;
    }

    private Map<String, byte[]> unzip(byte[] content) throws Exception {
        Map<String, byte[]> entries = new java.util.LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                entries.put(entry.getName(), input.readAllBytes());
            }
        }
        return entries;
    }
}
