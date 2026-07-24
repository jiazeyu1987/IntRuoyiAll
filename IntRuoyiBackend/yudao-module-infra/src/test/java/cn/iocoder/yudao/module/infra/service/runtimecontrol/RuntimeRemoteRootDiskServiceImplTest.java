package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootCleanupReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootCleanupRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootDiskStatusRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeRemoteRootDiskServiceImplTest extends BaseMockitoUnitTest {

    @TempDir
    private Path tempDir;

    @Mock
    private RuntimeControlCommandExecutor commandExecutor;

    private RuntimeControlProperties properties;
    private RuntimeRemoteRootDiskService service;

    @BeforeEach
    void setUp() {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        service = new RuntimeRemoteRootDiskServiceImpl(properties, commandExecutor);
    }

    @Test
    void getStatusShouldDispatchOnlyToWhitelistedTargetHost() {
        when(commandExecutor.executeForOutput(argThat(command ->
                command.getArguments().containsAll(List.of("-Mode", "status", "-TargetEnvironment", "test",
                        "-ServerHost", "172.30.30.58"))), any(Duration.class)))
                .thenReturn(statusJson("172.30.30.58"));

        RuntimeControlRemoteRootDiskStatusRespVO status = service.getStatus("test");

        assertEquals("test", status.getTargetEnvironment());
        assertEquals("172.30.30.58", status.getServerHost());
        assertEquals("/", status.getMountPoint());
        assertEquals(50_000_000_000L, status.getTotalBytes());
        assertEquals(20_000L, status.getAvailableBytes());
    }

    @Test
    void getStatusShouldUseDefaultFixedTestHostWithoutProfileSpecificOverride() {
        RuntimeControlProperties defaultProperties = new RuntimeControlProperties();
        defaultProperties.setRepoRoot(tempDir.toString());
        defaultProperties.afterPropertiesSet();
        RuntimeRemoteRootDiskService defaultService =
                new RuntimeRemoteRootDiskServiceImpl(defaultProperties, commandExecutor);
        when(commandExecutor.executeForOutput(argThat(command ->
                command.getArguments().containsAll(List.of("-Mode", "status", "-TargetEnvironment", "test",
                        "-ServerHost", "172.30.30.58"))), any(Duration.class)))
                .thenReturn(statusJson("172.30.30.58"));

        RuntimeControlRemoteRootDiskStatusRespVO status = defaultService.getStatus("test");

        assertEquals("172.30.30.58", status.getServerHost());
    }

    @Test
    void cleanupShouldRequireExplicitTestTargetAndReturnBeforeAfterEvidence() {
        RuntimeControlRemoteRootCleanupReqVO reqVO = new RuntimeControlRemoteRootCleanupReqVO();
        reqVO.setTargetEnvironment("test");
        reqVO.setReason("清理测试服根分区临时目录");
        when(commandExecutor.executeForOutput(argThat(command ->
                command.getArguments().containsAll(List.of("-Mode", "cleanup", "-TargetEnvironment", "test",
                        "-ServerHost", "172.30.30.58"))
                        && !command.getArguments().contains("172.30.30.57")), any(Duration.class)))
                .thenReturn(cleanupJson());

        RuntimeControlRemoteRootCleanupRespVO result = service.cleanup(reqVO, "1001");

        assertEquals("test", result.getTargetEnvironment());
        assertEquals("172.30.30.58", result.getServerHost());
        assertEquals(List.of("/opt/intruoyi/ops/backup/tmp", "/tmp"), result.getCleanupPaths());
        assertEquals(20_000L, result.getBefore().getAvailableBytes());
        assertEquals(35_000_000_000L, result.getAfter().getAvailableBytes());
    }

    @Test
    void getStatusShouldSupportProdByDispatchingOnlyToFixedProdHost() {
        when(commandExecutor.executeForOutput(argThat(command ->
                command.getArguments().containsAll(List.of("-Mode", "status", "-TargetEnvironment", "prod",
                        "-ServerHost", "172.30.30.57"))), any(Duration.class)))
                .thenReturn(statusJson("prod", "172.30.30.57"));

        RuntimeControlRemoteRootDiskStatusRespVO status = service.getStatus("prod");

        assertEquals("prod", status.getTargetEnvironment());
        assertEquals("172.30.30.57", status.getServerHost());
    }

    @Test
    void getStatusShouldSupportBackupByDispatchingOnlyToFixedBackupHost() {
        when(commandExecutor.executeForOutput(argThat(command ->
                command.getArguments().containsAll(List.of("-Mode", "status", "-TargetEnvironment", "backup",
                        "-ServerHost", "172.30.30.59"))), any(Duration.class)))
                .thenReturn(statusJson("backup", "172.30.30.59"));

        RuntimeControlRemoteRootDiskStatusRespVO status = service.getStatus("backup");

        assertEquals("backup", status.getTargetEnvironment());
        assertEquals("172.30.30.59", status.getServerHost());
    }

    @Test
    void cleanupShouldRejectProdWithoutProdConfirmationBeforeDispatch() {
        RuntimeControlRemoteRootCleanupReqVO reqVO = new RuntimeControlRemoteRootCleanupReqVO();
        reqVO.setTargetEnvironment("prod");
        reqVO.setReason("清理正式服根分区临时目录");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.cleanup(reqVO, "1001"));

        assertTrue(exception.getMessage().contains("PROD"));
        verify(commandExecutor, never()).executeForOutput(any(), any(Duration.class));
    }

    @Test
    void cleanupShouldSupportProdOnlyWithProdConfirmationAndFixedProdHost() {
        RuntimeControlRemoteRootCleanupReqVO reqVO = new RuntimeControlRemoteRootCleanupReqVO();
        reqVO.setTargetEnvironment("prod");
        reqVO.setReason("清理正式服根分区临时目录");
        reqVO.setProdConfirmText("PROD");
        when(commandExecutor.executeForOutput(argThat(command ->
                command.getArguments().containsAll(List.of("-Mode", "cleanup", "-TargetEnvironment", "prod",
                        "-ServerHost", "172.30.30.57", "-ProdConfirmText", "PROD"))), any(Duration.class)))
                .thenReturn(cleanupJson("prod", "172.30.30.57"));

        RuntimeControlRemoteRootCleanupRespVO result = service.cleanup(reqVO, "1001");

        assertEquals("prod", result.getTargetEnvironment());
        assertEquals("172.30.30.57", result.getServerHost());
    }

    @Test
    void cleanupShouldRejectBackupWithoutProdConfirmationBeforeDispatch() {
        RuntimeControlRemoteRootCleanupReqVO reqVO = new RuntimeControlRemoteRootCleanupReqVO();
        reqVO.setTargetEnvironment("backup");
        reqVO.setReason("清理备份服务器根分区临时目录");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.cleanup(reqVO, "1001"));

        assertTrue(exception.getMessage().contains("PROD"));
        verify(commandExecutor, never()).executeForOutput(any(), any(Duration.class));
    }

    @Test
    void cleanupShouldSupportBackupOnlyWithProdConfirmationAndFixedBackupHost() {
        RuntimeControlRemoteRootCleanupReqVO reqVO = new RuntimeControlRemoteRootCleanupReqVO();
        reqVO.setTargetEnvironment("backup");
        reqVO.setReason("清理备份服务器根分区临时目录");
        reqVO.setProdConfirmText("PROD");
        when(commandExecutor.executeForOutput(argThat(command ->
                command.getArguments().containsAll(List.of("-Mode", "cleanup", "-TargetEnvironment", "backup",
                        "-ServerHost", "172.30.30.59", "-ProdConfirmText", "PROD"))), any(Duration.class)))
                .thenReturn(cleanupJson("backup", "172.30.30.59"));

        RuntimeControlRemoteRootCleanupRespVO result = service.cleanup(reqVO, "1001");

        assertEquals("backup", result.getTargetEnvironment());
        assertEquals("172.30.30.59", result.getServerHost());
    }

    @Test
    void cleanupShouldRejectWhenConfiguredHostDoesNotMatchTargetEnvironment() {
        properties.getEnvironments().get("test").setHost("172.30.30.57");
        RuntimeControlRemoteRootCleanupReqVO reqVO = new RuntimeControlRemoteRootCleanupReqVO();
        reqVO.setTargetEnvironment("test");
        reqVO.setReason("清理测试服根分区临时目录");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.cleanup(reqVO, "1001"));

        assertTrue(exception.getMessage().contains("固定服务器 IP"));
        verify(commandExecutor, never()).executeForOutput(any(), any(Duration.class));
    }

    @Test
    void getStatusShouldRejectOutputThatDoesNotProveTestServerHost() {
        when(commandExecutor.executeForOutput(any(), any(Duration.class))).thenReturn(statusJson("172.30.30.57"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.getStatus("test"));

        assertTrue(exception.getMessage().contains("固定服务器 IP"));
    }

    private String statusJson(String serverHost) {
        return statusJson("test", serverHost);
    }

    private String statusJson(String targetEnvironment, String serverHost) {
        return """
                {
                  "targetEnvironment": "%s",
                  "serverHost": "%s",
                  "mountPoint": "/",
                  "filesystem": "/dev/mapper/cl-root",
                  "totalBytes": 50000000000,
                  "usedBytes": 49999980000,
                  "availableBytes": 20000,
                  "usagePercent": 100.0,
                  "inodeTotal": 122000,
                  "inodeUsed": 121746,
                  "inodeAvailable": 254,
                  "inodeUsagePercent": 100.0,
                  "sampledAt": "2026-06-08T10:00:00"
                }
                """.formatted(targetEnvironment, serverHost);
    }

    private String cleanupJson() {
        return cleanupJson("test", "172.30.30.58");
    }

    private String cleanupJson(String targetEnvironment, String serverHost) {
        return """
                {
                  "targetEnvironment": "%s",
                  "serverHost": "%s",
                  "cleanupPaths": ["/opt/intruoyi/ops/backup/tmp", "/tmp"],
                  "deletedEntryCount": 20,
                  "before": %s,
                  "after": {
                    "targetEnvironment": "%s",
                    "serverHost": "%s",
                    "mountPoint": "/",
                    "filesystem": "/dev/mapper/cl-root",
                    "totalBytes": 50000000000,
                    "usedBytes": 15000000000,
                    "availableBytes": 35000000000,
                    "usagePercent": 30.0,
                    "inodeTotal": 122000,
                    "inodeUsed": 90000,
                    "inodeAvailable": 32000,
                    "inodeUsagePercent": 74.0,
                    "sampledAt": "2026-06-08T10:05:00"
                  }
                }
                """.formatted(targetEnvironment, serverHost, statusJson(targetEnvironment, serverHost),
                targetEnvironment, serverHost);
    }
}
