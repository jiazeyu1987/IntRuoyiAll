package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RuntimeControlHighRiskActionContractTest extends BaseMockitoUnitTest {

    @TempDir
    private Path tempDir;

    @Mock
    private RuntimeControlCommandExecutor commandExecutor;

    private RuntimeControlProperties properties;
    private RuntimeControlServiceImpl runtimeControlService;

    @BeforeEach
    void setUp() {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        properties.getBackupOps().setExecutionMode("linux-local");
        properties.getBackupOps().setLinuxScriptPath("/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh");
        properties.getBackupOps().setLinuxConfigPath(writeBackupOpsConfig(tempDir.resolve("nas-backup-points")).toString());
        properties.getBackupOps().setNasBackupPointsRoot("nas-backup-points");
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        RuntimeOpsResponsibilityService responsibilityService =
                new RuntimeOpsResponsibilityServiceImpl(new RuntimeOpsOwnerMatrixStore(properties));
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                responsibilityService,
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)));
    }

    @Test
    void rollbackShouldRejectFreeTextImageTagAndRequireServerGeneratedCandidateBeforeDispatch() {
        RuntimeControlActionReqVO reqVO = highRiskAction("rollback-app");
        reqVO.setSelectedImageTag("20260525_200033");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertTrue(messageContainsAny(exception, "selectedImageCandidateId", "candidate", "候选"),
                () -> "Free text image tag must be rejected in favor of selectedImageCandidateId, actual message: "
                        + exception.getMessage());

        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void restoreShouldRejectFreeTextBackupIdAndRequireServerGeneratedCandidateBeforeDispatch() {
        RuntimeControlActionReqVO reqVO = highRiskAction("restore-data");
        reqVO.setTargetEnvironment("test");
        reqVO.setSelectedBackupId("20260525_215449");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertTrue(messageContainsAny(exception, "selectedRecoverySetCandidateId", "candidate", "候选"),
                () -> "Free text backup id must be rejected in favor of selectedRecoverySetCandidateId, actual message: "
                        + exception.getMessage());

        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void rollbackShouldRejectFreeTextImageTagEvenWhenCandidateIdIsPresent() {
        createRollbackCandidateFixture("20260525_215449", "20260525_200033");
        RuntimeControlActionReqVO reqVO = highRiskAction("rollback-app");
        reqVO.setSelectedImageCandidateId(new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir))
                .listRollbackCandidates().get(0).getCandidateId());
        reqVO.setSelectedImageTag("manual-injected-tag");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertTrue(messageContainsAny(exception, "selectedImageTag", "手填", "自由文本"),
                () -> "Free text image tag must be rejected even with candidateId, actual message: "
                        + exception.getMessage());

        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void restoreShouldRejectFreeTextBackupIdEvenWhenCandidateIdIsPresent() {
        createRestorePoint("20260526-010203");
        RuntimeControlActionReqVO reqVO = highRiskAction("restore-data");
        reqVO.setTargetEnvironment("test");
        reqVO.setSelectedRecoverySetCandidateId(new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir))
                .listRestoreCandidates().get(0).getCandidateId());
        reqVO.setSelectedBackupId("manual-injected-backup");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertTrue(messageContainsAny(exception, "selectedBackupId", "手填", "自由文本"),
                () -> "Free text backup id must be rejected even with candidateId, actual message: "
                        + exception.getMessage());

        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void defaultOwnerMatrixShouldAllowHighRiskRollbackToDispatchSelectedCandidate() {
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        createRollbackCandidateFixture("20260525_215449", "20260525_200033");
        RuntimeControlActionReqVO reqVO = highRiskAction("rollback-app");
        setPropertyIfPresent(reqVO, "selectedImageCandidateId", new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir))
                .listRollbackCandidates().get(0).getCandidateId());

        runtimeControlService.executeAction(reqVO, "1001");

        verify(commandExecutor, org.mockito.Mockito.timeout(1000)).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    private RuntimeControlActionReqVO highRiskAction(String action) {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction(action);
        reqVO.setReason("contract test");
        reqVO.setProdConfirmText("PROD");
        if ("rollback-app".equals(action)) {
            reqVO.setTargetEnvironment("test");
        }
        return reqVO;
    }

    private boolean messageContainsAny(ServiceException exception, String... expectedParts) {
        String message = String.valueOf(exception.getMessage()).toLowerCase();
        return Arrays.stream(expectedParts)
                .map(part -> part.toLowerCase())
                .anyMatch(message::contains);
    }

    private void setPropertyIfPresent(Object target, String propertyName, String value) {
        try {
            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Method setter = Arrays.stream(target.getClass().getMethods())
                    .filter(method -> setterName.equals(method.getName()))
                    .filter(method -> method.getParameterCount() == 1)
                    .filter(method -> String.class.equals(method.getParameterTypes()[0]))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing writable property: " + propertyName));
            setter.invoke(target, value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void createRollbackCandidateFixture(String backupId, String imageTag) {
        try {
            Path root = tempDir.resolve("Backup").resolve("ReleasePackage").resolve(imageTag);
            java.nio.file.Files.createDirectories(root);
            java.nio.file.Files.writeString(root.resolve("release-manifest.json"),
                    "{\"releaseTag\":\"" + backupId + "\",\"packageDirectoryName\":\"" + imageTag + "\"}");
            java.nio.file.Files.writeString(root.resolve("prod-latest.json"),
                    "{\"releaseTag\":\"" + backupId + "\",\"packageDirectoryName\":\"" + imageTag
                            + "\",\"action\":\"deploy\",\"environment\":\"prod\"}");
            java.nio.file.Files.writeString(root.resolve("rollback-compatibility.json"),
                    "{\"schemaVersion\":\"v1\",\"packageDirectoryName\":\"" + imageTag
                            + "\",\"status\":\"COMPATIBLE\",\"checkedAt\":\"2026-05-30T00:12:00Z\",\"summary\":\"compatible\"}");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void createRestorePoint(String backupId) {
        try {
            Path root = tempDir.resolve("nas-backup-points").resolve(backupId);
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
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                    "{\"schemaVersion\":\"v2\",\"backupId\":\"" + backupId + "\",\"status\":\"success\",\"deploy\":{\"imageTag\":\"20260526_010203\"},\"recoverySet\":{\"id\":\""
                            + backupId + "\",\"status\":\"COMPLETE\",\"program\":{\"imageTag\":\"20260526_010203\"},\"mysql\":{\"dumpPath\":\"mysql/ruoyi-vue-pro.sql.gz\"},\"minio\":{\"bucket\":\"yudao\",\"snapshotPath\":\"objects/manifest-object-inventory.json\"},\"businessFiles\":{\"snapshotPath\":\"objects/manifest-object-inventory.json\"},\"redis\":{\"policy\":\"CLEAR_AND_REBUILD\"},\"configuration\":{\"manifestPath\":\"deploy/runtime.env\",\"composePath\":\"deploy/docker-compose.yml\"},\"checksums\":{\"path\":\"manifest/checksums.txt\",\"sha256\":\"abc\"}}}");
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("checksums.txt"), "sha256  deploy/runtime.env");
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("rehearsal-report.json"),
                    "{\"status\":\"PASSED\"}");
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("现场快照.md"), "snapshot");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
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
}
