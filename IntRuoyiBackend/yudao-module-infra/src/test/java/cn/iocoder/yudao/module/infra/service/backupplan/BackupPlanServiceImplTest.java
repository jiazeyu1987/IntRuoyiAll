package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanHistoryPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanScheduleSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeBackupDrillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupPlanServiceImplTest {

    @TempDir
    private Path tempDir;

    private RuntimeControlProperties properties;
    private FakeBackupPlanSchedulerGateway schedulerGateway;
    private FakeBackupPlanOperationGateway operationGateway;
    private FakeRuntimeBackupDrillService backupDrillService;
    private BackupPlanServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        properties.getBackupOps().setExecutionMode("powershell");
        writeBackupConfig("""
                {
                  "backup": {
                    "fullSchedule": "SUN 01:00",
                    "incrementalSchedule": "02:00",
                    "repositoryEnvironment": "test",
                    "retentionSource": "质量批准的记录保存期限矩阵 QA-RET-2026-01",
                    "qualityApprovalRef": "QA-SIGN-20260907",
                    "maxFreshnessHours": 48,
                    "keepDaysRemote": 30,
                    "keepDaysLocal": 3
                  }
                }
                """);
        createBackupScripts();
        schedulerGateway = new FakeBackupPlanSchedulerGateway();
        operationGateway = new FakeBackupPlanOperationGateway();
        backupDrillService = new FakeRuntimeBackupDrillService();
        service = new BackupPlanServiceImpl(properties, schedulerGateway, backupDrillService, operationGateway);
    }

    @Test
    void getStatusShouldExposeSimpleScheduleTaskHealthAndLatestBackupPoint() {
        RuntimeControlBackupPointRespVO point = new RuntimeControlBackupPointRespVO();
        point.setBackupId("20260725-013000");
        point.setRecoverabilityStatus("RECOVERABLE");
        point.setCompletedAt(LocalDateTime.now().minusHours(1));
        backupDrillService.backupPoints.add(point);
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastRunTime(LocalDateTime.of(2026, 7, 25, 1, 30));
        schedulerGateway.status.setLastResultCode(0);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("SUN 01:00", status.getFullSchedule());
        assertEquals("02:00", status.getIncrementalSchedule());
        assertEquals("test", status.getRepositoryEnvironment());
        assertEquals("质量批准的记录保存期限矩阵 QA-RET-2026-01", status.getRetentionSource());
        assertEquals("QA-SIGN-20260907", status.getQualityApprovalRef());
        assertEquals(48, status.getMaxFreshnessHours());
        assertEquals("已开启", status.getPlanStatus());
        assertEquals("正常", status.getHealthStatus());
        assertEquals(LocalDateTime.of(2026, 7, 26, 1, 30), status.getNextRunTime());
        assertNotNull(status.getLatestBackupPoint());
        assertEquals("20260725-013000", status.getLatestBackupPoint().getBackupId());
    }

    @Test
    void saveSchedulesShouldWriteConfigAndRegisterRealTasks() throws Exception {
        BackupPlanScheduleSaveReqVO reqVO = new BackupPlanScheduleSaveReqVO();
        reqVO.setFullSchedule("SAT 02:15");
        reqVO.setIncrementalSchedule("03:05");
        reqVO.setRetentionSource("质量批准的记录保存期限矩阵 QA-RET-2026-02");
        reqVO.setQualityApprovalRef("QA-SIGN-20260908");

        BackupPlanStatusRespVO status = service.saveSchedule(reqVO);

        String config = Files.readString(configPath(), StandardCharsets.UTF_8);
        assertTrue(config.contains("\"fullSchedule\" : \"SAT 02:15\""));
        assertTrue(config.contains("\"incrementalSchedule\" : \"03:05\""));
        assertTrue(config.contains("\"retentionSource\" : \"质量批准的记录保存期限矩阵 QA-RET-2026-02\""));
        assertTrue(config.contains("\"qualityApprovalRef\" : \"QA-SIGN-20260908\""));
        assertEquals("SAT 02:15", schedulerGateway.registeredSchedule.getFullSchedule());
        assertEquals("03:05", schedulerGateway.registeredSchedule.getIncrementalSchedule());
        assertEquals("质量批准的记录保存期限矩阵 QA-RET-2026-02", schedulerGateway.registeredSchedule.getRetentionSource());
        assertEquals("QA-SIGN-20260908", schedulerGateway.registeredSchedule.getQualityApprovalRef());
        assertEquals("test", schedulerGateway.registeredSchedule.getRepositoryEnvironment());
        assertEquals("03:05", status.getIncrementalSchedule());
    }

    @Test
    void saveScheduleShouldNormalizeFullWeekday() throws Exception {
        BackupPlanScheduleSaveReqVO reqVO = new BackupPlanScheduleSaveReqVO();
        reqVO.setFullSchedule("sun 03:05");
        reqVO.setIncrementalSchedule("04:10");

        service.saveSchedule(reqVO);

        String config = Files.readString(configPath(), StandardCharsets.UTF_8);
        assertTrue(config.contains("\"fullSchedule\" : \"SUN 03:05\""));
        assertEquals("SUN 03:05", schedulerGateway.registeredSchedule.getFullSchedule());
        assertEquals("test", schedulerGateway.registeredSchedule.getRepositoryEnvironment());
    }

    @Test
    void enableShouldFailFastWhenRepositoryEnvironmentIsMissing() throws Exception {
        writeBackupConfig("""
                {
                  "backup": {
                    "fullSchedule": "SUN 01:00",
                    "incrementalSchedule": "02:00",
                    "maxFreshnessHours": 48
                  }
                }
                """);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.enable());

        assertTrue(exception.getMessage().contains("backup.repositoryEnvironment"));
    }

    @Test
    void getStatusShouldExposeConfigAbnormalWhenRepositoryEnvironmentIsMissing() throws Exception {
        writeBackupConfig("""
                {
                  "backup": {
                    "fullSchedule": "SUN 01:00",
                    "incrementalSchedule": "02:00",
                    "maxFreshnessHours": 48
                  }
                }
                """);
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastResultCode(0);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("配置异常", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("backup.repositoryEnvironment"));
    }

    @Test
    void getStatusShouldExposeConfigAbnormalWhenFreshnessThresholdIsMissing() throws Exception {
        writeBackupConfig("""
                {
                  "backup": {
                    "fullSchedule": "SUN 01:00",
                    "incrementalSchedule": "02:00",
                    "repositoryEnvironment": "test"
                  }
                }
                """);
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastResultCode(0);
        RuntimeControlBackupPointRespVO point = backupPoint("20260725-020000");
        point.setCompletedAt(LocalDateTime.now().minusHours(1));
        backupDrillService.backupPoints.add(point);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("配置异常", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("backup.maxFreshnessHours"));
    }

    @Test
    void enableShouldFailFastWhenRetentionApprovalIsMissing() throws Exception {
        writeBackupConfig("""
                {
                  "backup": {
                    "fullSchedule": "SUN 01:00",
                    "incrementalSchedule": "02:00",
                    "repositoryEnvironment": "test",
                    "maxFreshnessHours": 48
                  }
                }
                """);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.enable());

        assertTrue(exception.getMessage().contains("backup.retentionSource"));
    }

    @Test
    void getStatusShouldExposeBlockedWhenRetentionApprovalIsMissing() throws Exception {
        writeBackupConfig("""
                {
                  "backup": {
                    "fullSchedule": "SUN 01:00",
                    "incrementalSchedule": "02:00",
                    "repositoryEnvironment": "test",
                    "maxFreshnessHours": 48
                  }
                }
                """);
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastResultCode(0);
        RuntimeControlBackupPointRespVO point = backupPoint("20260725-020000");
        point.setCompletedAt(LocalDateTime.now().minusHours(1));
        backupDrillService.backupPoints.add(point);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("配置异常", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("保存期限来源"));
    }

    @Test
    void getStatusShouldUseCompletedAtInsteadOfLastVerifiedAtForFreshness() {
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastResultCode(0);
        RuntimeControlBackupPointRespVO point = backupPoint("20260725-020000");
        point.setCompletedAt(null);
        point.setLastVerifiedAt(LocalDateTime.now());
        backupDrillService.backupPoints.add(point);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("配置异常", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("completedAt"));
    }

    @Test
    void getStatusShouldBlockWhenLatestBackupPointExceedsFreshnessThreshold() {
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastResultCode(0);
        RuntimeControlBackupPointRespVO point = backupPoint("20260725-020000");
        point.setCompletedAt(LocalDateTime.now().minusHours(49));
        backupDrillService.backupPoints.add(point);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("配置异常", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("超过"));
    }

    @Test
    void getStatusShouldBlockWhenLatestBackupPointIsMissing() {
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastResultCode(0);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("配置异常", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("最近成功备份点缺失"));
    }

    @Test
    void getStatusShouldBlockWhenTaskCommandPathDrifts() {
        RuntimeControlBackupPointRespVO point = backupPoint("20260725-020000");
        point.setCompletedAt(LocalDateTime.now().minusHours(1));
        backupDrillService.backupPoints.add(point);
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastResultCode(0);
        schedulerGateway.status.setTaskToRun("powershell.exe -File D:\\legacy\\backup-ops.ps1");

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("配置异常", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("脚本路径异常"));
    }

    @Test
    void getStatusShouldExposeConfigAbnormalWhenSchedulerQueryFails() {
        backupDrillService.backupPoints.add(backupPoint("20260725-020000"));
        schedulerGateway.status.setEnabled(false);
        schedulerGateway.status.setQueryExitCode(1);
        schedulerGateway.status.setBlockedReason("计划任务查询失败");

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("配置异常", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("计划任务查询失败"));
    }

    @Test
    void getStatusShouldExposeClosedWhenSchedulerTaskIsDisabled() {
        backupDrillService.backupPoints.add(backupPoint("20260725-020000"));
        schedulerGateway.status.setEnabled(false);
        schedulerGateway.status.setQueryExitCode(0);
        schedulerGateway.status.setBlockedReason("计划任务已禁用");

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("已关闭", status.getPlanStatus());
        assertEquals("已关闭", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("计划任务已禁用"));
    }

    @Test
    void getStatusShouldBlockWhenEnabledTaskMissingNextRunTime() {
        backupDrillService.backupPoints.add(backupPoint("20260725-020000"));
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setQueryExitCode(0);
        schedulerGateway.status.setLastResultCode(0);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("配置异常", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("下次运行时间缺失"));
    }

    @Test
    void getStatusShouldExposeLastFailureWhenSchedulerLastResultIsNonZero() {
        backupDrillService.backupPoints.add(backupPoint("20260725-020000"));
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setQueryExitCode(0);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastResultCode(1);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("上次失败", status.getHealthStatus());
        assertTrue(status.getBlockedReason().contains("上次运行失败"));
    }

    @Test
    void saveScheduleShouldFailFastWhenFullWeekdayMissing() {
        BackupPlanScheduleSaveReqVO reqVO = new BackupPlanScheduleSaveReqVO();
        reqVO.setFullSchedule("03:05");
        reqVO.setIncrementalSchedule("04:10");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.saveSchedule(reqVO));

        assertTrue(exception.getMessage().contains("fullSchedule"));
    }

    @Test
    void enableShouldFailFastWhenBackupScriptIsMissing() throws Exception {
        Files.delete(tempDir.resolve("script/backup-ops/scripts/backup-ops.ps1"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.enable());

        assertTrue(exception.getMessage().contains("backup-ops.ps1"));
    }

    @Test
    void getHistoryPageShouldReturnPagedBackupPoints() {
        backupDrillService.backupPoints.add(backupPoint("20260725-020000"));
        backupDrillService.backupPoints.add(backupPoint("20260724-020000"));
        BackupPlanHistoryPageReqVO reqVO = new BackupPlanHistoryPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);

        PageResult<RuntimeControlBackupPointRespVO> page = service.getHistoryPage(reqVO);

        assertEquals(2L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals("20260725-020000", page.getList().get(0).getBackupId());
    }

    @Test
    void backupNowShouldDelegateToRuntimeOperationGateway() {
        RuntimeControlOperationRespVO operation = service.backupNow(7L, "FULL");

        assertEquals(7L, operationGateway.operatorUserId);
        assertEquals("FULL", operationGateway.backupKind);
        assertEquals("backup-now", operation.getAction());
    }

    private RuntimeControlBackupPointRespVO backupPoint(String backupId) {
        RuntimeControlBackupPointRespVO point = new RuntimeControlBackupPointRespVO();
        point.setBackupId(backupId);
        point.setRecoverabilityStatus("RECOVERABLE");
        point.setCompletedAt(LocalDateTime.now().minusHours(1));
        return point;
    }

    private void createBackupScripts() throws Exception {
        Files.createDirectories(tempDir.resolve("script/backup-ops/scripts"));
        Files.createDirectories(tempDir.resolve("script/backup-ops/actions"));
        Files.writeString(tempDir.resolve("script/backup-ops/scripts/backup-ops.ps1"),
                "param()\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("script/backup-ops/actions/Register-BackupOpsScheduledTasks.ps1"),
                "param()\n", StandardCharsets.UTF_8);
    }

    private void writeBackupConfig(String json) throws Exception {
        Files.createDirectories(configPath().getParent());
        Files.writeString(configPath(), json, StandardCharsets.UTF_8);
    }

    private Path configPath() {
        return tempDir.resolve("script/backup-ops/config/backup-ops.config.json");
    }

    private static class FakeBackupPlanSchedulerGateway implements BackupPlanSchedulerGateway {

        private BackupPlanSchedulerStatus status = new BackupPlanSchedulerStatus();
        private BackupPlanSchedule registeredSchedule;
        private boolean enableCalled;
        private boolean disableCalled;

        @Override
        public BackupPlanSchedulerStatus getStatus() {
            return status;
        }

        @Override
        public void registerOrUpdate(BackupPlanSchedule schedule) {
            registeredSchedule = schedule;
            status.setEnabled(true);
        }

        @Override
        public void enable() {
            enableCalled = true;
            status.setEnabled(true);
        }

        @Override
        public void disable() {
            disableCalled = true;
            status.setEnabled(false);
        }
    }

    private static class FakeBackupPlanOperationGateway implements BackupPlanOperationGateway {

        private Long operatorUserId;
        private String backupKind;

        @Override
        public RuntimeControlOperationRespVO backupNow(Long loginUserId, String backupKind) {
            operatorUserId = loginUserId;
            this.backupKind = backupKind;
            RuntimeControlOperationRespVO operation = new RuntimeControlOperationRespVO();
            operation.setAction("backup-now");
            return operation;
        }
    }

    private static class FakeRuntimeBackupDrillService implements RuntimeBackupDrillService {

        private final List<RuntimeControlBackupPointRespVO> backupPoints = new ArrayList<>();

        @Override
        public List<RuntimeControlBackupPointRespVO> listBackupPoints() {
            return backupPoints;
        }

        @Override
        public RuntimeControlBackupPointRespVO getBackupPoint(String backupId) {
            return backupPoints.stream()
                    .filter(item -> backupId.equals(item.getBackupId()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
