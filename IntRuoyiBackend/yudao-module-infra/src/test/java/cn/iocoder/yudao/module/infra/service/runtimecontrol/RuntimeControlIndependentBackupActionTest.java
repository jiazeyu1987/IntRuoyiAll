package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeControlIndependentBackupActionTest {
    @TempDir Path tempDir;

    @Test
    void independentDeployCarriesTheDurableJavaOperationIdentity() {
        var request = request();
        request.setPreassignedOperationId("op-review-12345678");
        var args = RuntimeControlOperationAction.PUBLISH_BACKUP.buildArguments(request, "operator", approvedProperties());
        assertEquals("op-review-12345678", argument(args, "-JavaOperationId"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"preview", "arguments"})
    void disabledProductionWriteGateRejectsReviewPublication(String entry) {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        assertFalse(properties.getReleaseWorkflow().isProductionWriteEnabled());
        var failure = assertThrows(IllegalArgumentException.class, () -> {
            if ("preview".equals(entry)) {
                new cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowBackupAuthorizationService(properties)
                        .preview("operator", "review publication", "approved-source", "request-12345678");
            } else {
                RuntimeControlOperationAction.fromAction("publish-backup").buildArguments(request(), "operator", properties);
            }
        });
        assertEquals("RELEASE_WORKFLOW_BACKUP_WRITE_DISABLED", failure.getMessage());
    }

    @Test
    void independentBackupRequiresWorkflowAndProductionConfirmation() {
        var action = RuntimeControlOperationAction.fromAction("publish-backup");
        assertNotNull(action, "independent review publication must be a distinct action");
        assertEquals("backup", action.getEnvironment());
        assertTrue(action.requiresReleaseWorkflowContext());
        assertTrue(action.isProdConfirmRequired(new RuntimeControlActionReqVO()));
        assertTrue(action.requiresNasReleaseRepository());
        assertTrue(action.requiresReleaseTag());
        assertTrue(action.requiresResponsibilityGate());
    }

    @Test
    void peopleSeeTheFullReviewServerName() {
        assertEquals("审查服务器", approvedProperties()
                .getEnvironments().get("backup").getLabel());
        assertEquals("上线审查服务器", RuntimeControlOperationAction.PROMOTE_BACKUP.getLabel());
    }

    @ParameterizedTest
    @ValueSource(strings = {"intruoyi-frontend", "intruoyi-backend", "website-frontend", "frontend-url"})
    void fixedReviewEndpointsCannotDrift(String target) {
        var properties = approvedProperties();
        var backup = properties.getEnvironments().get("backup");
        if ("frontend-url".equals(target)) {
            backup.getTargets().get("intruoyi-frontend").setUrl("http://172.30.30.57:8081/");
        } else {
            backup.getTargets().get(target).setPort(9999);
        }
        assertThrows(IllegalArgumentException.class, properties::requireBackupPublishTarget);
    }

    @Test
    void independentBackupUsesServerTargetAndDoesNotInventTestEvidence() {
        var action = RuntimeControlOperationAction.fromAction("publish-backup");
        assertNotNull(action);
        var request = request();
        var args = action.buildArguments(request, "operator", approvedProperties());
        assertEquals("backup", argument(args, "-Environment"));
        assertEquals("independent-backup", argument(args, "-DeployIntent"));
        assertEquals("PROD", argument(args, "-ConfirmText"));
        assertEquals("172.30.30.59", argument(args, "-ServerHost"));
        assertEquals("/mnt/intruoyi-data", argument(args, "-RemoteDataDiskMount"));
        assertEquals("/dev/mapper/cl-home", argument(args, "-RemoteDataDiskDevice"));
        assertEquals("/mnt/intruoyi-data/runtime-data", argument(args, "-RemoteDataRoot"));
        assertEquals("/mnt/intruoyi-data/intruoyi-releases", argument(args, "-RemoteReleaseRoot"));
        assertEquals("intruoyi-minio", argument(args, "-RemoteMinioContainer"));
        assertEquals("a".repeat(64), argument(args, "-ExpectedPackageDigest"));
        assertEquals("b".repeat(64), argument(args, "-ExpectedManifestDigest"));
        assertEquals("rw-12345678", argument(args, "-ReleaseWorkflowId"));
        assertEquals("source-review-r1", argument(args, "-SourceSelectionId"));
        assertEquals("a".repeat(40), argument(args, "-ExpectedMaintenanceCommit"));
        assertEquals("b".repeat(40), argument(args, "-ExpectedApplicationCommit"));
        assertEquals("b".repeat(40), argument(args, "-ExpectedFrontendCommit"));
        assertFalse(args.contains("-TestOperationId"));
        assertFalse(args.contains("-TestOperationEvidencePath"));
    }

    @Test
    void reviewTargetCannotBeReboundToProductionOrTestDiskDefaults() {
        var action = RuntimeControlOperationAction.fromAction("publish-backup");
        assertNotNull(action);
        var properties = approvedProperties();
        properties.getEnvironments().get("backup").setHost("172.30.30.57");
        assertThrows(IllegalArgumentException.class, () -> action.buildArguments(request(), "operator", properties));
        properties.getEnvironments().get("backup").setHost("172.30.30.59");
        properties.getEnvironments().get("backup").setRemoteDataDiskDevice("/dev/vdb");
        assertThrows(IllegalArgumentException.class, () -> action.buildArguments(request(), "operator", properties));
    }

    @Test
    void independentBuildRunsItsPreflightAgainstReviewTargetWithoutCopyingBusinessData() {
        var request = request();
        request.setTargetEnvironment("backup");
        request.setPublishScope("app-release");
        request.setIncludeOnlyOffice(false);
        request.setIncludeShowroomBuildPackage(false);
        var action = RuntimeControlOperationAction.BUILD_RELEASE;
        var args = action.buildArguments(request, "operator", approvedProperties());
        assertEquals("backup", action.resolveEnvironment(request));
        assertTrue(action.isProdConfirmRequired(request));
        assertEquals("build-release", argument(args, "-Mode"));
        assertEquals("backup", argument(args, "-Environment"));
        assertEquals("172.30.30.59", argument(args, "-ServerHost"));
        assertEquals("independent-backup", argument(args, "-DeployIntent"));
        assertTrue(args.contains("-SkipDatabaseSync"));
        assertTrue(args.contains("-SkipMinioSync"));
        assertFalse(args.contains("-ExpectedPackageDigest"));
        assertFalse(args.contains("-TestOperationId"));
    }

    private RuntimeControlActionReqVO request() {
        var request = new RuntimeControlActionReqVO();
        request.setAction("publish-backup");
        request.setPreassignedOperationId("op-review-12345678");
        request.setReleaseTag("release-review-r1");
        request.setProdConfirmText("PROD");
        request.setReleaseWorkflowId("rw-12345678");
        request.setSourceSelectionId("source-review-r1");
        request.setReleaseAuthorizationId("auth-review-r1");
        request.setExpectedMaintenanceCommit("a".repeat(40));
        request.setExpectedApplicationCommit("b".repeat(40));
        request.setExpectedFrontendCommit("b".repeat(40));
        request.setExpectedPackageDigest("a".repeat(64));
        request.setExpectedManifestDigest("b".repeat(64));
        return request;
    }

    private RuntimeControlProperties approvedProperties() {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        return properties;
    }

    private String argument(List<String> args, String name) {
        assertTrue(args.contains(name), name);
        return args.get(args.indexOf(name) + 1);
    }
}
