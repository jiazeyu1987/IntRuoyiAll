package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupPlanMinimalClosureTest {

    @TempDir
    private Path tempDir;

    private FakeSchedulerGateway schedulerGateway;
    private FakeOperationGateway operationGateway;
    private BackupPlanServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        writeConfig();
        Files.createDirectories(tempDir.resolve("script/backup-ops/scripts"));
        Files.createDirectories(tempDir.resolve("script/backup-ops/actions"));
        Files.writeString(tempDir.resolve("script/backup-ops/scripts/backup-ops.ps1"), "param()\n");
        Files.writeString(tempDir.resolve("script/backup-ops/actions/Register-BackupOpsScheduledTasks.ps1"), "param()\n");
        schedulerGateway = new FakeSchedulerGateway();
        operationGateway = new FakeOperationGateway();
        service = new BackupPlanServiceImpl(properties, schedulerGateway, new EmptyBackupDrillService(), operationGateway);
    }

    @Test
    void saveScheduleShouldPersistFullAndIncrementalSchedules() throws Exception {
        BackupPlanScheduleSaveReqVO reqVO = new BackupPlanScheduleSaveReqVO();
        reqVO.setFullSchedule("SUN 01:00");
        reqVO.setIncrementalSchedule("02:00");

        BackupPlanStatusRespVO status = service.saveSchedule(reqVO);

        String config = Files.readString(configPath(), StandardCharsets.UTF_8);
        assertTrue(config.contains("\"fullSchedule\" : \"SUN 01:00\""));
        assertTrue(config.contains("\"incrementalSchedule\" : \"02:00\""));
        assertEquals("SUN 01:00", schedulerGateway.registered.getFullSchedule());
        assertEquals("02:00", schedulerGateway.registered.getIncrementalSchedule());
        assertEquals("SUN 01:00", status.getFullSchedule());
        assertEquals("02:00", status.getIncrementalSchedule());
    }

    @Test
    void backupNowShouldForwardExplicitKind() {
        service.backupNow(7L, "INCREMENTAL");

        assertEquals(7L, operationGateway.loginUserId);
        assertEquals("INCREMENTAL", operationGateway.backupKind);
    }

    @Test
    void backupNowShouldRejectUnknownKind() {
        assertThrows(ServiceException.class, () -> service.backupNow(7L, "AUTO"));
    }

    private void writeConfig() throws Exception {
        Files.createDirectories(configPath().getParent());
        Files.writeString(configPath(), """
                {
                  "backup": {
                    "fullSchedule": "SUN 01:00",
                    "incrementalSchedule": "02:00",
                    "repositoryEnvironment": "test",
                    "maxFreshnessHours": 25
                  }
                }
                """, StandardCharsets.UTF_8);
    }

    private Path configPath() {
        return tempDir.resolve("script/backup-ops/config/backup-ops.config.json");
    }

    private static class FakeSchedulerGateway implements BackupPlanSchedulerGateway {
        private final BackupPlanSchedulerStatus status = new BackupPlanSchedulerStatus();
        private BackupPlanSchedule registered;

        @Override
        public BackupPlanSchedulerStatus getStatus() {
            return status;
        }

        @Override
        public void registerOrUpdate(BackupPlanSchedule schedule) {
            registered = schedule;
        }

        @Override
        public void enable() {
        }

        @Override
        public void disable() {
        }
    }

    private static class FakeOperationGateway implements BackupPlanOperationGateway {
        private Long loginUserId;
        private String backupKind;

        @Override
        public RuntimeControlOperationRespVO backupNow(Long loginUserId, String backupKind) {
            this.loginUserId = loginUserId;
            this.backupKind = backupKind;
            return new RuntimeControlOperationRespVO();
        }
    }

    private static class EmptyBackupDrillService implements RuntimeBackupDrillService {
        @Override
        public List<RuntimeControlBackupPointRespVO> listBackupPoints() {
            return List.of();
        }

        @Override
        public RuntimeControlBackupPointRespVO getBackupPoint(String backupId) {
            return null;
        }
    }
}
