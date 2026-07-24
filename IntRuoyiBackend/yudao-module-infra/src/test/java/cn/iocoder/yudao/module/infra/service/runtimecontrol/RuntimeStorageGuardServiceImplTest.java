package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlCapacityStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeStorageGuardServiceImplTest {

    @TempDir
    private Path tempDir;

    private RuntimeControlProperties properties;
    private RuntimeStorageGuardServiceImpl storageGuardService;
    private RuntimeOpsCapturingSiteMessageSender siteMessageSender;

    @BeforeEach
    void setUp() {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        RuntimeControlProperties.StorageGuard storageGuard = properties.getStorageGuard();
        storageGuard.setMonitorPath(tempDir.toString());
        storageGuard.setLogDir(tempDir.resolve("logs").toString());
        storageGuard.setDiskUsageWarnPercent(100.0);
        storageGuard.setLogDirWarnBytes(8L);
        storageGuard.setLogGrowthWarnBytes(1024L);

        RuntimeOpsResponsibilityServiceImpl responsibilityService =
                new RuntimeOpsResponsibilityServiceImpl(new RuntimeOpsOwnerMatrixStore(properties));
        responsibilityService.createOwner(owner("local", "storage-capacity-warning", 1001L));
        siteMessageSender = new RuntimeOpsCapturingSiteMessageSender();
        RuntimeOpsAlertServiceImpl alertService = new RuntimeOpsAlertServiceImpl(new RuntimeOpsAlertStore(properties),
                responsibilityService, siteMessageSender);
        storageGuardService = new RuntimeStorageGuardServiceImpl(properties,
                new RuntimeStorageGuardSnapshotStore(properties), alertService);
    }

    @Test
    void getCapacityStatusShouldExposeDiskAndLogMetricsWithoutWriteSideEffects() throws Exception {
        Files.createDirectories(Path.of(properties.getStorageGuard().getLogDir()));
        Files.writeString(Path.of(properties.getStorageGuard().getLogDir()).resolve("app.log"),
                "log-data-over-threshold", StandardCharsets.UTF_8);

        RuntimeControlCapacityStatusRespVO status = storageGuardService.getCapacityStatus();

        assertEquals(RuntimeOpsInspectionStatus.WARN, status.getStatus());
        assertNotNull(status.getSampledAt());
        assertTrue(status.getDisk().getTotalBytes() > 0);
        assertTrue(status.getLogDirectory().getSizeBytes() > 8L);
        assertTrue(status.getReasons().stream().anyMatch(reason -> reason.contains("日志目录")));
        assertNull(status.getAlert());
        assertEquals(0, siteMessageSender.callCount.get());
        assertFalse(Files.exists(Path.of(properties.getStateDir())
                .resolve("runtime-ops").resolve("capacity-status.json")));
    }

    @Test
    void getCapacityStatusShouldReturnBlockedReasonWhenLogDirectoryIsUnreadable() {
        properties.getStorageGuard().setLogDir(tempDir.resolve("missing-logs").toString());

        RuntimeControlCapacityStatusRespVO status = storageGuardService.getCapacityStatus();

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, status.getStatus());
        assertTrue(status.getReasons().stream().anyMatch(reason -> reason.contains("日志目录")));
        assertEquals(0, siteMessageSender.callCount.get());
    }

    @Test
    void refreshCapacityStatusShouldAlertWhenLogThresholdExceededAndDiskMetricIsBlocked() throws Exception {
        Files.createDirectories(Path.of(properties.getStorageGuard().getLogDir()));
        Files.writeString(Path.of(properties.getStorageGuard().getLogDir()).resolve("app.log"),
                "log-data-over-threshold", StandardCharsets.UTF_8);
        properties.getStorageGuard().setMonitorPath(tempDir.resolve("missing-monitor-path").toString());

        Method refreshMethod = RuntimeStorageGuardService.class.getMethod("refreshCapacityStatus");
        RuntimeControlCapacityStatusRespVO status =
                (RuntimeControlCapacityStatusRespVO) refreshMethod.invoke(storageGuardService);

        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, status.getStatus());
        assertEquals(RuntimeOpsInspectionStatus.BLOCKED, status.getDisk().getStatus());
        assertEquals(RuntimeOpsInspectionStatus.WARN, status.getLogDirectory().getStatus());
        assertTrue(status.getReasons().stream().anyMatch(reason -> reason.contains("磁盘采样路径")));
        assertTrue(status.getReasons().stream().anyMatch(reason -> reason.contains("日志目录超过阈值")));
        assertNotNull(status.getAlert());
        assertEquals("WARN", status.getAlert().getSeverity());
        assertTrue(status.getAlert().getContent().contains("磁盘采样路径"));
        assertTrue(status.getAlert().getContent().contains("日志目录超过阈值"));
        assertEquals(RuntimeControlSiteMessageStatus.SENT, status.getAlert().getSiteMessageStatus());
        assertEquals(1, siteMessageSender.callCount.get());
    }

    private RuntimeControlOwnerMatrixSaveReqVO owner(String environment, String action, Long ownerUserId) {
        RuntimeControlOwnerMatrixSaveReqVO reqVO = new RuntimeControlOwnerMatrixSaveReqVO();
        reqVO.setEnvironment(environment);
        reqVO.setAction(action);
        reqVO.setRole("ops-owner");
        reqVO.setRequired(true);
        reqVO.setOwnerUserId(ownerUserId);
        reqVO.setOwnerName("owner-" + ownerUserId);
        return reqVO;
    }

    private static final class RuntimeOpsCapturingSiteMessageSender implements RuntimeOpsSiteMessageSender {

        private final AtomicInteger callCount = new AtomicInteger();

        @Override
        public Long sendSingleMessageToAdmin(Long userId, String templateCode, Map<String, Object> templateParams) {
            callCount.incrementAndGet();
            assertEquals(1001L, userId);
            assertEquals("RUNTIME_OPS_ALERT", templateCode);
            assertTrue(String.valueOf(templateParams.get("content")).contains("日志目录"));
            return 9101L;
        }
    }
}
