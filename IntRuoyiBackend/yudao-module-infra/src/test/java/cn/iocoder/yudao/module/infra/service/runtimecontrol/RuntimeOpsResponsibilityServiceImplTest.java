package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class RuntimeOpsResponsibilityServiceImplTest extends BaseMockitoUnitTest {

    @TempDir
    private Path tempDir;

    @Mock
    private RuntimeControlCommandExecutor commandExecutor;

    @Mock
    private NasSettingsService nasSettingsService;

    private RuntimeOpsResponsibilityServiceImpl responsibilityService;

    @BeforeEach
    void setUp() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        responsibilityService = new RuntimeOpsResponsibilityServiceImpl(new RuntimeOpsOwnerMatrixStore(properties));
    }

    @Test
    void ownerMatrixShouldBeConfigurableByEnvironmentActionAndRole() {
        RuntimeControlOwnerMatrixRespVO owner = responsibilityService.createOwner(owner("prod", "promote-prod",
                "release-owner", 1001L));

        RuntimeControlOwnerMatrixSaveReqVO updateReqVO = owner("prod", "promote-prod", "release-owner", 1002L);
        updateReqVO.setOwnerName("release owner 2");
        RuntimeControlOwnerMatrixRespVO updated = responsibilityService.updateOwner(owner.getId(), updateReqVO);

        assertEquals(owner.getId(), updated.getId());
        assertEquals("prod", updated.getEnvironment());
        assertEquals("promote-prod", updated.getAction());
        assertEquals("release-owner", updated.getRole());
        assertEquals(true, updated.getRequired());
        assertEquals(1002L, updated.getOwnerUserId());
        List<RuntimeControlOwnerMatrixRespVO> owners = responsibilityService.getOwnerMatrix("prod", "promote-prod");
        assertEquals(1, owners.size());
        assertEquals("release owner 2", owners.get(0).getOwnerName());
    }

    @Test
    void defaultReleaseOwnerShouldBeVisibleForProductionReleaseActions() {
        List<RuntimeControlOwnerMatrixRespVO> owners = responsibilityService.getOwnerMatrix("prod", "rollback-app");

        assertEquals(1, owners.size());
        RuntimeControlOwnerMatrixRespVO owner = owners.get(0);
        assertEquals("prod", owner.getEnvironment());
        assertEquals("rollback-app", owner.getAction());
        assertEquals("release-owner", owner.getRole());
        assertEquals(true, owner.getRequired());
        assertEquals(1L, owner.getOwnerUserId());
        assertEquals("admin", owner.getOwnerName());
    }

    @Test
    void defaultDataOwnerShouldBeVisibleForRestoreDataTargets() {
        List<RuntimeControlOwnerMatrixRespVO> owners = responsibilityService.getOwnerMatrix(null, "restore-data");

        assertTrue(owners.stream().anyMatch(owner -> "test".equals(owner.getEnvironment())
                && "restore-data".equals(owner.getAction())
                && "data-owner".equals(owner.getRole())
                && Boolean.TRUE.equals(owner.getRequired())
                && 1L == owner.getOwnerUserId()
                && "admin".equals(owner.getOwnerName())));
        assertTrue(owners.stream().anyMatch(owner -> "backup".equals(owner.getEnvironment())
                && "restore-data".equals(owner.getAction())
                && "data-owner".equals(owner.getRole())
                && Boolean.TRUE.equals(owner.getRequired())
                && 1L == owner.getOwnerUserId()
                && "admin".equals(owner.getOwnerName())));
    }

    @Test
    void defaultReleaseOwnerShouldBeVisibleForRollbackTargets() {
        List<RuntimeControlOwnerMatrixRespVO> owners = responsibilityService.getOwnerMatrix(null, "rollback-app");

        assertTrue(owners.stream().anyMatch(owner -> "test".equals(owner.getEnvironment())
                && "rollback-app".equals(owner.getAction())
                && "release-owner".equals(owner.getRole())
                && Boolean.TRUE.equals(owner.getRequired())
                && 1L == owner.getOwnerUserId()
                && "admin".equals(owner.getOwnerName())));
        assertTrue(owners.stream().anyMatch(owner -> "backup".equals(owner.getEnvironment())
                && "rollback-app".equals(owner.getAction())
                && "release-owner".equals(owner.getRole())
                && Boolean.TRUE.equals(owner.getRequired())
                && 1L == owner.getOwnerUserId()
                && "admin".equals(owner.getOwnerName())));
    }

    @Test
    void defaultOpsOwnersShouldCoverCapacityBackupAndRehearsalRoutes() {
        assertDefaultOwner("prod", "promote-prod", "release-owner");
        assertDefaultOwner("backup", "promote-backup", "release-owner");
        assertDefaultOwner("local", "storage-capacity-warning", "ops-owner");
        assertDefaultOwner("test", "storage-capacity-warning", "ops-owner");
        assertDefaultOwner("backup", "storage-capacity-warning", "ops-owner");
        assertDefaultOwner("prod", "storage-capacity-warning", "ops-owner");
        assertDefaultOwner("prod", "backup-now", "ops-owner");
        assertDefaultOwner("test", "backup-now", "ops-owner");
        assertDefaultOwner("backup", "backup-now", "ops-owner");
        assertDefaultOwner("prod", "backup-scheduled", "ops-owner");
        assertDefaultOwner("test", "rehearsal", "ops-owner");
        assertDefaultOwner("backup", "rehearsal", "ops-owner");
        assertDefaultOwner("test", "restore-data-started", "data-owner");
        assertDefaultOwner("backup", "restore-data-started", "data-owner");
        assertDefaultOwner("test", "restore-data-finished", "data-owner");
        assertDefaultOwner("backup", "restore-data-finished", "data-owner");
    }

    @Test
    void configuredDataOwnerShouldOverrideDefaultAdminForRestoreData() {
        responsibilityService.createOwner(owner("backup", "restore-data", "data-owner", 1008L));

        List<RuntimeControlOwnerMatrixRespVO> owners = responsibilityService.getOwnerMatrix("backup", "restore-data");

        assertEquals(1, owners.size());
        assertEquals(1008L, owners.get(0).getOwnerUserId());
        assertEquals("release owner", owners.get(0).getOwnerName());
    }

    @Test
    void executeRollbackWithDefaultReleaseOwnerShouldReachCandidateValidation() throws Exception {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        java.nio.file.Files.createDirectories(tempDir.resolve("Backup").resolve("ReleasePackage"));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        RuntimeControlServiceImpl runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor,
                operationStore, responsibilityService,
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)));

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("rollback-app");
        reqVO.setReason("混滚版本");
        reqVO.setTargetEnvironment("test");
        reqVO.setProdConfirmText("PROD");
        reqVO.setSelectedImageCandidateId("rollback:missing");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));

        assertTrue(exception.getMessage().contains("候选"), exception.getMessage());
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void restoreDataWithDefaultDataOwnerShouldReachCandidateValidation() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        try {
            java.nio.file.Files.createDirectories(tempDir.resolve(properties.getBackupOps().getNasBackupPointsRoot()));
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        RuntimeControlServiceImpl runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor,
                operationStore, responsibilityService,
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)));

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("restore-data");
        reqVO.setReason("恢复测试服数据");
        reqVO.setTargetEnvironment("test");
        reqVO.setSelectedRecoverySetCandidateId("restore:missing");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));

        assertTrue(exception.getMessage().contains("候选"), exception.getMessage());
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void configuredRequiredOwnerShouldAllowProductionGateToReachDispatch() {
        responsibilityService.createOwner(owner("prod", "promote-prod", "release-owner", 1001L));
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        createTestedReleasePackage("20260528_220000");
        doReturn(new NasConnectionConfig("172.30.30.4", 1445, "IT共享", "WORKGROUP", "nas-user", "nas-secret"))
                .when(nasSettingsService).getRequiredNasConfig();
        RuntimeControlServiceImpl runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor,
                operationStore, responsibilityService,
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)),
                nasSettingsService, new RuntimeControlNasBrowserServiceStub(tempDir));

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("promote-prod");
        reqVO.setReason("上线正式服");
        reqVO.setProdConfirmText("PROD");
        reqVO.setReleaseTag("20260528_220000");

        RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(reqVO, "1001");

        verify(commandExecutor, org.mockito.Mockito.timeout(1000)).executeOperation(any(), any());
        waitOperationStatus(runtimeControlService, operation.getOperationId(), "succeeded");
    }

    private void createTestedReleasePackage(String releaseTag) {
        try {
            Path root = tempDir.resolve("Backup").resolve("ReleasePackage").resolve(releaseTag);
            java.nio.file.Files.createDirectories(root);
            java.nio.file.Files.writeString(root.resolve("release-manifest.json"),
                    "{\"releaseTag\":\"" + releaseTag + "\",\"packageDirectoryName\":\"" + releaseTag
                            + "\",\"onlyOfficeIncluded\":false,\"artifacts\":[{\"path\":\"image.tar\",\"sha256\":\"abc\"}]}");
            java.nio.file.Files.writeString(root.resolve("tested.json"),
                    "{\"releaseTag\":\"" + releaseTag + "\",\"packageDirectoryName\":\"" + releaseTag
                            + "\",\"testedAt\":\"2026-05-30T00:00:00Z\",\"operatorName\":\"tester\"}");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void waitOperationStatus(RuntimeControlServiceImpl runtimeControlService, String operationId, String status) {
        for (int i = 0; i < 20; i++) {
            boolean matched = runtimeControlService.getOperations().stream()
                    .anyMatch(operation -> operationId.equals(operation.getOperationId())
                            && status.equals(operation.getStatus()));
            if (matched) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }
        assertTrue(false, "operation did not reach status " + status);
    }

    private RuntimeControlOwnerMatrixSaveReqVO owner(String environment, String action, String role, Long ownerUserId) {
        RuntimeControlOwnerMatrixSaveReqVO reqVO = new RuntimeControlOwnerMatrixSaveReqVO();
        reqVO.setEnvironment(environment);
        reqVO.setAction(action);
        reqVO.setRole(role);
        reqVO.setRequired(true);
        reqVO.setOwnerUserId(ownerUserId);
        reqVO.setOwnerName("release owner");
        return reqVO;
    }

    private void assertDefaultOwner(String environment, String action, String role) {
        List<RuntimeControlOwnerMatrixRespVO> owners = responsibilityService.getOwnerMatrix(environment, action);
        assertEquals(1, owners.size(), environment + "/" + action);
        RuntimeControlOwnerMatrixRespVO owner = owners.get(0);
        assertEquals(environment, owner.getEnvironment());
        assertEquals(action, owner.getAction());
        assertEquals(role, owner.getRole());
        assertEquals(true, owner.getRequired());
        assertEquals(1L, owner.getOwnerUserId());
        assertEquals("admin", owner.getOwnerName());
    }
}
