package cn.iocoder.yudao.module.bpm.formcenter.controller;

import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.FormCenterController;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.FormCenterExceptionAdvice;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessApprovedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessRejectedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmReworkRequiredReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCompletedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCreatedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateImportReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormEffectPendingPageReqVO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FormCenterRuntimeContractTest {

    @Test
    void controllerRoutesAndPermissionsMatchFrontendContract() throws Exception {
        assertArrayEquals(new String[]{"/form-center"},
                FormCenterController.class.getAnnotation(RequestMapping.class).value());

        Method templatePool = FormCenterController.class.getDeclaredMethod("getTemplatePool",
                cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplatePoolPageReqVO.class);
        assertArrayEquals(new String[]{"/template-pool"}, templatePool.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('form:template:query')",
                templatePool.getAnnotation(PreAuthorize.class).value());

        Method templateVersion = FormCenterController.class.getDeclaredMethod("getTemplateVersion",
                Long.class, String.class);
        assertArrayEquals(new String[]{"/templates/{templateId}/versions/{versionNo}"},
                templateVersion.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('form:template:query')",
                templateVersion.getAnnotation(PreAuthorize.class).value());

        Method importDoc = FormCenterController.class.getDeclaredMethod("importDoc", FormCenterTemplateImportReqVO.class);
        assertArrayEquals(new String[]{"/templates/import-doc"}, importDoc.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:template:create')",
                importDoc.getAnnotation(PreAuthorize.class).value());

        Method resolve = FormCenterController.class.getDeclaredMethod("resolveAction",
                cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO.class);
        assertArrayEquals(new String[]{"/actions/resolve"}, resolve.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:instance:create')",
                resolve.getAnnotation(PreAuthorize.class).value());

        Method activeInstance = FormCenterController.class.getDeclaredMethod("findActiveBusinessAction",
                cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO.class);
        assertArrayEquals(new String[]{"/actions/active-instance"},
                activeInstance.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:instance:create')",
                activeInstance.getAnnotation(PreAuthorize.class).value());

        Method saveJimuSchema = FormCenterController.class.getDeclaredMethod("saveJimuSchema", Long.class,
                String.class, cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateJimuSchemaReqVO.class);
        assertArrayEquals(new String[]{"/templates/{templateId}/versions/{versionNo}/jimu-schema"},
                saveJimuSchema.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('form:template:update')",
                saveJimuSchema.getAnnotation(PreAuthorize.class).value());

        Method publishTemplate = FormCenterController.class.getDeclaredMethod("publishTemplate", Long.class,
                String.class);
        assertArrayEquals(new String[]{"/templates/{templateId}/versions/{versionNo}/publish"},
                publishTemplate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:template:publish')",
                publishTemplate.getAnnotation(PreAuthorize.class).value());

        Method disableTemplate = FormCenterController.class.getDeclaredMethod("disableTemplate", Long.class,
                String.class);
        assertArrayEquals(new String[]{"/templates/{templateId}/versions/{versionNo}/disable"},
                disableTemplate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:template:disable')",
                disableTemplate.getAnnotation(PreAuthorize.class).value());

        Method enableTemplate = FormCenterController.class.getDeclaredMethod("enableTemplate", Long.class,
                String.class);
        assertArrayEquals(new String[]{"/templates/{templateId}/versions/{versionNo}/enable"},
                enableTemplate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:template:disable')",
                enableTemplate.getAnnotation(PreAuthorize.class).value());

        Method obsoleteTemplate = FormCenterController.class.getDeclaredMethod("obsoleteTemplate", Long.class,
                String.class);
        assertArrayEquals(new String[]{"/templates/{templateId}/versions/{versionNo}/obsolete"},
                obsoleteTemplate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:template:obsolete')",
                obsoleteTemplate.getAnnotation(PreAuthorize.class).value());

        Method saveDraft = FormCenterController.class.getDeclaredMethod("saveDraft", Long.class,
                cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceDraftReqVO.class);
        assertArrayEquals(new String[]{"/instances/{instanceId}/draft"},
                saveDraft.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('form:instance:update')",
                saveDraft.getAnnotation(PreAuthorize.class).value());

        Method policyPage = FormCenterController.class.getDeclaredMethod("getPolicyPage",
                cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicyPageReqVO.class);
        assertArrayEquals(new String[]{"/policies"}, policyPage.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('form:policy:query')",
                policyPage.getAnnotation(PreAuthorize.class).value());

        Method savePolicy = FormCenterController.class.getDeclaredMethod("savePolicy",
                cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicySaveReqVO.class);
        assertArrayEquals(new String[]{"/policies"}, savePolicy.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:policy:create')",
                savePolicy.getAnnotation(PreAuthorize.class).value());

        Method publishPolicy = FormCenterController.class.getDeclaredMethod("publishPolicy", Long.class);
        assertArrayEquals(new String[]{"/policies/{policyId}/publish"},
                publishPolicy.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:policy:publish')",
                publishPolicy.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void controllerRoutesExposeBpmCallbacksSnapshotsAndEffectRetry() throws Exception {
        Method taskCreated = FormCenterController.class.getDeclaredMethod("onBpmTaskCreated",
                FormBpmTaskCreatedReqVO.class);
        assertArrayEquals(new String[]{"/bpm/task-created"}, taskCreated.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:bpm-callback:handle')",
                taskCreated.getAnnotation(PreAuthorize.class).value());

        Method taskCompleted = FormCenterController.class.getDeclaredMethod("onBpmTaskCompleted",
                FormBpmTaskCompletedReqVO.class);
        assertArrayEquals(new String[]{"/bpm/task-completed"}, taskCompleted.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:bpm-callback:handle')",
                taskCompleted.getAnnotation(PreAuthorize.class).value());

        Method reworkRequired = FormCenterController.class.getDeclaredMethod("onBpmReworkRequired",
                FormBpmReworkRequiredReqVO.class);
        assertArrayEquals(new String[]{"/bpm/rework-required"}, reworkRequired.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:bpm-callback:handle')",
                reworkRequired.getAnnotation(PreAuthorize.class).value());

        Method processRejected = FormCenterController.class.getDeclaredMethod("onBpmProcessRejected",
                FormBpmProcessRejectedReqVO.class);
        assertArrayEquals(new String[]{"/bpm/process-rejected"}, processRejected.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:bpm-callback:handle')",
                processRejected.getAnnotation(PreAuthorize.class).value());

        Method processApproved = FormCenterController.class.getDeclaredMethod("onBpmProcessApproved",
                FormBpmProcessApprovedReqVO.class);
        assertArrayEquals(new String[]{"/bpm/process-approved"}, processApproved.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:bpm-callback:handle')",
                processApproved.getAnnotation(PreAuthorize.class).value());

        Method snapshots = FormCenterController.class.getDeclaredMethod("getInstanceSnapshots", Long.class);
        assertArrayEquals(new String[]{"/instances/{instanceId}/snapshots"},
                snapshots.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('form:instance:snapshot:query')",
                snapshots.getAnnotation(PreAuthorize.class).value());

        Method pendingEffects = FormCenterController.class.getDeclaredMethod("getPendingEffects",
                FormEffectPendingPageReqVO.class);
        assertArrayEquals(new String[]{"/effects/pending"}, pendingEffects.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('form:effect:query')",
                pendingEffects.getAnnotation(PreAuthorize.class).value());

        Method retryEffect = FormCenterController.class.getDeclaredMethod("retryEffect", Long.class);
        assertArrayEquals(new String[]{"/effects/{instanceId}/retry"},
                retryEffect.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:effect:retry')",
                retryEffect.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void formPolicyApprovalModeContractExposesSwitchEndpointAndVos() throws Exception {
        Class<?> policyRespVO = Class.forName(
                "cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicyRespVO");
        Class<?> policySaveReqVO = Class.forName(
                "cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicySaveReqVO");
        Class<?> switchReqVO = Class.forName(
                "cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicySwitchApprovalModeReqVO");

        policyRespVO.getDeclaredField("approvalMode");
        policySaveReqVO.getDeclaredField("approvalMode");
        switchReqVO.getDeclaredField("approvalMode");
        switchReqVO.getDeclaredField("bpmProcessKey");

        Method switchApprovalMode = FormCenterController.class.getDeclaredMethod("switchPolicyApprovalMode",
                Long.class, switchReqVO);
        assertArrayEquals(new String[]{"/policies/{policyId}/switch-approval-mode"},
                switchApprovalMode.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('form:policy:publish')",
                switchApprovalMode.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void submitReturnsUpdatedInstanceSoFrontendCanContinueToBpmApproval() throws Exception {
        Method submit = FormCenterController.class.getDeclaredMethod("submitInstance", Long.class,
                cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO.class);
        ParameterizedType returnType = (ParameterizedType) submit.getGenericReturnType();

        assertEquals("cn.iocoder.yudao.framework.common.pojo.CommonResult<cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO>",
                returnType.getTypeName(),
                "submit must return the updated instance including bpmProcessInstanceId for the real approval UI flow");
    }

    @Test
    void controllerReturnsStructuredFormCenterErrorInsteadOfGenericInternalError() {
        FormCenterController controller = new FormCenterController();

        CommonResult<?> result = controller.handleFormCenterException(new FormCenterException(
                FormCenterErrorCode.FORM_POLICY_NOT_FOUND, "未找到业务动作表单策略"));

        Assertions.assertEquals(FormCenterErrorCode.FORM_POLICY_NOT_FOUND.getCode(), result.getCode());
        Assertions.assertEquals("未找到业务动作表单策略", result.getMsg());
    }

    @Test
    void controllerDeclaresFormCenterExceptionHandler() throws Exception {
        Method handler = FormCenterController.class.getDeclaredMethod("handleFormCenterException",
                FormCenterException.class);

        assertArrayEquals(new Class[]{FormCenterException.class}, handler.getAnnotation(ExceptionHandler.class).value());
    }

    @Test
    void formCenterExceptionAdviceHandlesErrorsFromNonFormCenterControllers() throws Exception {
        assertEquals(0, FormCenterExceptionAdvice.class.getAnnotation(RestControllerAdvice.class).basePackages().length,
                "FormCenterException must be handled globally because MES schedule replan calls form center from a MES controller");
        Method handler = FormCenterExceptionAdvice.class.getDeclaredMethod("handleFormCenterException",
                FormCenterException.class);
        assertArrayEquals(new Class[]{FormCenterException.class}, handler.getAnnotation(ExceptionHandler.class).value());

        CommonResult<?> result = new FormCenterExceptionAdvice().handleFormCenterException(new FormCenterException(
                FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                "No published business approval policy matched action REPLAN"));

        assertEquals(FormCenterErrorCode.FORM_POLICY_NOT_FOUND.getCode(), result.getCode());
        assertEquals("No published business approval policy matched action REPLAN", result.getMsg());
    }

    @Test
    void businessActionTenantIsDerivedFromAuthenticatedTenantContext() throws Exception {
        assertNull(BusinessActionContextReqVO.class.getDeclaredField("tenantId").getAnnotation(NotNull.class),
                "tenantId must not be required from the client; runtime resolves it from TenantContextHolder");
    }

    @Test
    void templateImportContractUsesSystemGeneratedVersionNumber() throws Exception {
        Assertions.assertThrows(NoSuchFieldException.class,
                () -> FormCenterTemplateImportReqVO.class.getDeclaredField("versionNo"),
                "导入模板不允许再暴露手工版本号字段，版本号必须由系统自动生成");
        FormCenterTemplateImportReqVO.class.getDeclaredField("selectedTemplateId");
    }

}
