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
                    "frequency": "DAILY",
                    "schedule": "01:30",
                    "weekday": "MON",
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
        backupDrillService.backupPoints.add(point);
        schedulerGateway.status.setEnabled(true);
        schedulerGateway.status.setNextRunTime(LocalDateTime.of(2026, 7, 26, 1, 30));
        schedulerGateway.status.setLastRunTime(LocalDateTime.of(2026, 7, 25, 1, 30));
        schedulerGateway.status.setLastResultCode(0);

        BackupPlanStatusRespVO status = service.getStatus();

        assertEquals("DAILY", status.getFrequency());
        assertEquals("01:30", status.getTime());
        assertEquals("已开启", status.getPlanStatus());
        assertEquals("正常", status.getHealthStatus());
        assertEquals(LocalDateTime.of(2026, 7, 26, 1, 30), status.getNextRunTime());
        assertNotNull(status.getLatestBackupPoint());
        assertEquals("20260725-013000", status.getLatestBackupPoint().getBackupId());
    }

    @Test
    void saveDailyScheduleShouldWriteConfigAndRegisterRealTask() throws Exception {
        BackupPlanScheduleSaveReqVO reqVO = new BackupPlanScheduleSaveReqVO();
        reqVO.setFrequency("DAILY");
        reqVO.setTime("02:15");

        BackupPlanStatusRespVO status = service.saveSchedule(reqVO);

        String config = Files.readString(configPath(), StandardCharsets.UTF_8);
        assertTrue(config.contains("\"frequency\" : \"DAILY\""));
        assertTrue(config.contains("\"schedule\" : \"02:15\""));
        assertEquals("DAILY", schedulerGateway.registeredSchedule.getFrequency());
        assertEquals("02:15", schedulerGateway.registeredSchedule.getTime());
        assertEquals("02:15", status.getTime());
    }

    @Test
    void saveWeeklyScheduleShouldRequireWeekdayAndRegisterWeeklyTask() throws Exception {
        BackupPlanScheduleSaveReqVO reqVO = new BackupPlanScheduleSaveReqVO();
        reqVO.setFrequency("WEEKLY");
        reqVO.setTime("03:05");
        reqVO.setWeekday("SUN");

        service.saveSchedule(reqVO);

        String config = Files.readString(configPath(), StandardCharsets.UTF_8);
        assertTrue(config.contains("\"frequency\" : \"WEEKLY\""));
        assertTrue(config.contains("\"weekday\" : \"SUN\""));
        assertEquals("WEEKLY", schedulerGateway.registeredSchedule.getFrequency());
        assertEquals("SUN", schedulerGateway.registeredSchedule.getWeekday());
    }

    @Test
    void saveWeeklyScheduleShouldFailFastWhenWeekdayMissing() {
        BackupPlanScheduleSaveReqVO reqVO = new BackupPlanScheduleSaveReqVO();
        reqVO.setFrequency("WEEKLY");
        reqVO.setTime("03:05");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.saveSchedule(reqVO));

        assertTrue(exception.getMessage().contains("weekday"));
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
        RuntimeControlOperationRespVO operation = service.backupNow(7L);

        assertEquals(7L, operationGateway.operatorUserId);
        assertEquals("backup-now", operation.getAction());
    }

    private RuntimeControlBackupPointRespVO backupPoint(String backupId) {
        RuntimeControlBackupPointRespVO point = new RuntimeControlBackupPointRespVO();
        point.setBackupId(backupId);
        point.setRecoverabilityStatus("RECOVERABLE");
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

        @Override
        public RuntimeControlOperationRespVO backupNow(Long loginUserId) {
            operatorUserId = loginUserId;
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
