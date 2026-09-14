package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalRequestStore;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormTemplateObsoletePendingRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormTemplateObsoleteReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormTemplateObsoleteRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormCenterTemplateObsoleteRuntimeTest extends BaseMockitoUnitTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;
    @Mock
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;
    @Mock
    private BusinessApprovalRequestStore businessApprovalRequestStore;
    @Mock
    private BpmProcessInstanceApi processInstanceApi;

    @InjectMocks
    private FormCenterRuntimeServiceImpl runtimeService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void obsoleteTemplateRejectsDirectBypass() {
        TenantContextHolder.setTenantId(122L);
        mockTemplate(FormTemplateStatus.PUBLISHED);

        assertThrows(FormCenterException.class, () -> runtimeService.obsoleteTemplate(200L, "V2.0"));

        verify(templateVersionMapper, never()).updateById((FormTemplateVersionDO) any());
    }

    @Test
    void submitTemplateObsoleteRequestStartsBusinessApprovalAndReturnsPendingBinding() {
        TenantContextHolder.setTenantId(122L);
        mockTemplate(FormTemplateStatus.PUBLISHED);
        when(businessApprovalOrchestrator.submit(any())).thenAnswer(invocation -> {
            BusinessApprovalContext context = invocation.getArgument(0);
            return pendingRequest(context);
        });

        FormTemplateObsoleteReqVO reqVO = new FormTemplateObsoleteReqVO();
        reqVO.setReason("模板内容已停止使用");

        FormTemplateObsoleteRespVO result = runtimeService.submitTemplateObsoleteRequest(200L, "V2.0", reqVO, 101L);

        ArgumentCaptor<BusinessApprovalContext> contextCaptor = ArgumentCaptor.forClass(BusinessApprovalContext.class);
        verify(businessApprovalOrchestrator).submit(contextCaptor.capture());
        BusinessApprovalContext context = contextCaptor.getValue();
        assertEquals(122L, context.getTenantId());
        assertEquals("FORM_CENTER", context.getDataDomain());
        assertEquals("FORM_CENTER", context.getSystemCode());
        assertEquals("FORM_TEMPLATE", context.getObjectType());
        assertEquals("301", context.getObjectId());
        assertEquals("V2.0", context.getObjectVersion());
        assertEquals("OBSOLETE", context.getActionCode());
        assertEquals(FormTemplateStatus.PUBLISHED.name(), context.getObjectState());
        assertEquals(101L, context.getApplicantUserId());
        assertEquals("模板内容已停止使用", context.getReason());
        assertEquals(9101L, result.getApprovalRequestId());
        assertEquals("PI-FORM-TPL-OBSOLETE-301", result.getApprovalProcessInstanceId());
        assertEquals(FormTemplateStatus.PENDING_APPROVAL.name(), result.getStatus());
    }

    @Test
    void findTemplateObsoletePendingRequestProjectsWithdrawPermissionForApplicant() {
        TenantContextHolder.setTenantId(122L);
        mockTemplate(FormTemplateStatus.PENDING_APPROVAL);
        when(businessApprovalRequestStore.findPendingByBusinessAction(any())).thenReturn(Optional.of(
                pendingRequest(context(FormTemplateStatus.PUBLISHED, 101L, "模板内容已停止使用"))));

        FormTemplateObsoletePendingRespVO result = runtimeService.findTemplateObsoletePendingRequest(200L, "V2.0", 101L);

        assertEquals(9101L, result.getApprovalRequestId());
        assertEquals("PI-FORM-TPL-OBSOLETE-301", result.getApprovalProcessInstanceId());
        assertTrue(result.getCanWithdraw());
        assertEquals(101L, result.getApplicantUserId());
    }

    @Test
    void withdrawTemplateObsoleteRequestCancelsBpmProcessAsApplicant() {
        TenantContextHolder.setTenantId(122L);
        mockTemplate(FormTemplateStatus.PENDING_APPROVAL);
        when(businessApprovalRequestStore.findPendingByBusinessAction(any())).thenReturn(Optional.of(
                pendingRequest(context(FormTemplateStatus.PUBLISHED, 101L, "模板内容已停止使用"))));

        runtimeService.withdrawTemplateObsoleteRequest(200L, "V2.0", "申请人撤回作废", 101L);

        verify(processInstanceApi).cancelProcessInstance(101L, "PI-FORM-TPL-OBSOLETE-301", "申请人撤回作废");
    }

    private void mockTemplate(FormTemplateStatus status) {
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(200L, "V2.0")).thenReturn(FormTemplateVersionDO.builder()
                .id(301L)
                .tenantId(122L)
                .templateId(200L)
                .templateName("损耗单")
                .versionNo("V2.0")
                .status(status.name())
                .build());
    }

    private BusinessApprovalContext context(FormTemplateStatus objectState, Long applicantUserId, String reason) {
        return BusinessApprovalContext.builder()
                .tenantId(122L)
                .dataDomain("FORM_CENTER")
                .systemCode("FORM_CENTER")
                .objectType("FORM_TEMPLATE")
                .objectId("301")
                .objectVersion("V2.0")
                .actionCode("OBSOLETE")
                .objectState(objectState.name())
                .applicantUserId(applicantUserId)
                .reason(reason)
                .build();
    }

    private BusinessApprovalRequest pendingRequest(BusinessApprovalContext context) {
        return BusinessApprovalRequest.builder()
                .requestId(9101L)
                .tenantId(122L)
                .policyId(88L)
                .policyMode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                .processDefinitionKey("form-template-obsolete-v1")
                .effectExecutorCode("FORM_TEMPLATE_OBSOLETE")
                .status(BusinessApprovalRequestStatus.PENDING_BPM)
                .context(context)
                .processInstanceId("PI-FORM-TPL-OBSOLETE-301")
                .resultState(FormTemplateStatus.PENDING_APPROVAL.name())
                .build();
    }

}
