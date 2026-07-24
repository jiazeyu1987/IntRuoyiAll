package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentUpdateReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeploymentService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrDeploymentContractTest {

    @Test
    void deploymentControllerMappings_matchDeploymentLicenseInterfaceContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrDeploymentController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-deployment"}, requestMapping.value());

        assertGet("getPage", "mes:pro-edhr-deployment:query", "/page", MesProEdhrDeploymentPageReqVO.class);
        assertPost("createEvidence", "mes:pro-edhr-deployment:create", "/create", MesProEdhrDeploymentCreateReqVO.class);
        assertGet("getDetail", "mes:pro-edhr-deployment:query", "/detail", Long.class);
        assertPost("updateEvidence", "mes:pro-edhr-deployment:update", "/update-evidence", MesProEdhrDeploymentUpdateReqVO.class);
        assertPost("precheckEvidence", "mes:pro-edhr-deployment:precheck", "/precheck", Long.class);
    }

    @Test
    void serviceContract_declaresDeploymentGateMethods() throws Exception {
        MesProEdhrDeploymentService.class.getDeclaredMethod("getPage", MesProEdhrDeploymentPageReqVO.class);
        MesProEdhrDeploymentService.class.getDeclaredMethod("createEvidence", MesProEdhrDeploymentCreateReqVO.class);
        MesProEdhrDeploymentService.class.getDeclaredMethod("getDetail", Long.class);
        MesProEdhrDeploymentService.class.getDeclaredMethod("updateEvidence", MesProEdhrDeploymentUpdateReqVO.class);
        MesProEdhrDeploymentService.class.getDeclaredMethod("precheckEvidence", Long.class);
    }

    @Test
    void updateRequestContract_acceptsDeploymentLicenseAndInterfaceEvidence() {
        Set<String> fieldNames = Set.of(MesProEdhrDeploymentUpdateReqVO.class.getDeclaredFields()).stream()
                .map(field -> field.getName())
                .collect(java.util.stream.Collectors.toSet());

        for (String fieldName : Set.of(
                "deploymentId",
                "targetEnvironment",
                "environmentAuthorized",
                "environmentCheckSummary",
                "serverSummary",
                "networkSummary",
                "objectStorageSummary",
                "capacitySummary",
                "permissionSummary",
                "releaseTag",
                "artifactVersion",
                "artifactChecksum",
                "schemaVersion",
                "migrationManifest",
                "requiredSqlManifest",
                "appImportResult",
                "licenseScope",
                "licenseValidUntil",
                "licenseFileEvidence",
                "licenseCheckResult",
                "customerLicenseConfirmation",
                "interfaceScope",
                "interfaceVersion",
                "integrationEnvironment",
                "requestEvidence",
                "responseEvidence",
                "interfaceFailureCount",
                "remediationAction",
                "retestEvidence",
                "interfaceConfirmedBy"
        )) {
            assertTrue(fieldNames.contains(fieldName), "update request must accept " + fieldName);
        }
    }

    private void assertGet(String methodName, String permission, String path, Class<?> parameterType) throws Exception {
        Method method = MesProEdhrDeploymentController.class.getDeclaredMethod(methodName, parameterType);
        assertArrayEquals(new String[]{path}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')", method.getAnnotation(PreAuthorize.class).value());
        if (parameterType == Long.class) {
            assertNotNull(method.getParameters()[0].getAnnotation(RequestParam.class));
        }
    }

    private void assertPost(String methodName, String permission, String path, Class<?> parameterType) throws Exception {
        Method method = MesProEdhrDeploymentController.class.getDeclaredMethod(methodName, parameterType);
        assertArrayEquals(new String[]{path}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')", method.getAnnotation(PreAuthorize.class).value());
        if (parameterType == Long.class) {
            assertNotNull(method.getParameters()[0].getAnnotation(RequestParam.class));
        }
    }
}
