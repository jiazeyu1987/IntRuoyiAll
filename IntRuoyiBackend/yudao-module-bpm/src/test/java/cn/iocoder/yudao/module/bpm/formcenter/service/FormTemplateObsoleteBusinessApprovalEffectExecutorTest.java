package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormTemplateObsoleteBusinessApprovalEffectExecutorTest extends BaseMockitoUnitTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;

    @InjectMocks
    private FormTemplateObsoleteBusinessApprovalEffectExecutor executor;

    @Test
    void exposesDedicatedFormTemplateObsoleteExecutorCode() {
        assertEquals("FORM_TEMPLATE_OBSOLETE", executor.getExecutorCode());
    }

    @Test
    void markPendingLocksCurrentPublishedVersionWhenBpmProcessStarted() {
        mockVersion(FormTemplateStatus.PUBLISHED);
        when(templateVersionMapper.updateById((FormTemplateVersionDO) any())).thenReturn(1);

        BusinessApprovalEffectResult result = executor.markPending(context(FormTemplateStatus.PUBLISHED),
                request("PI-FORM-TPL-OBSOLETE-301", FormTemplateStatus.PUBLISHED));

        assertEquals(FormTemplateStatus.PENDING_APPROVAL.name(), result.getResultState());
        assertUpdatedStatus(FormTemplateStatus.PENDING_APPROVAL);
    }

    @Test
    void executeApprovedObsoletesPendingVersion() {
        mockVersion(FormTemplateStatus.PENDING_APPROVAL);
        when(templateVersionMapper.updateById((FormTemplateVersionDO) any())).thenReturn(1);

        BusinessApprovalEffectResult result = executor.executeApproved(context(FormTemplateStatus.PUBLISHED),
                request("PI-FORM-TPL-OBSOLETE-301", FormTemplateStatus.PUBLISHED), 102L);

        assertEquals(FormTemplateStatus.OBSOLETE.name(), result.getResultState());
        assertUpdatedStatus(FormTemplateStatus.OBSOLETE);
    }

    @Test
    void rejectRestoresOriginalTemplateStatus() {
        mockVersion(FormTemplateStatus.PENDING_APPROVAL);
        when(templateVersionMapper.updateById((FormTemplateVersionDO) any())).thenReturn(1);

        BusinessApprovalEffectResult result = executor.reject(context(FormTemplateStatus.DISABLED),
                request("PI-FORM-TPL-OBSOLETE-301", FormTemplateStatus.DISABLED), 102L, "驳回");

        assertEquals(FormTemplateStatus.DISABLED.name(), result.getResultState());
        assertUpdatedStatus(FormTemplateStatus.DISABLED);
    }

    @Test
    void cancelRestoresOriginalTemplateStatus() {
        mockVersion(FormTemplateStatus.PENDING_APPROVAL);
        when(templateVersionMapper.updateById((FormTemplateVersionDO) any())).thenReturn(1);

        BusinessApprovalEffectResult result = executor.cancel(context(FormTemplateStatus.PUBLISHED),
                request("PI-FORM-TPL-OBSOLETE-301", FormTemplateStatus.PUBLISHED), 101L, "撤回");

        assertEquals(FormTemplateStatus.PUBLISHED.name(), result.getResultState());
        assertUpdatedStatus(FormTemplateStatus.PUBLISHED);
    }

    @Test
    void executeDirectIsRejectedBecauseObsoleteMustGoThroughBpmApproval() {
        mockVersion(FormTemplateStatus.PUBLISHED);

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> executor.executeDirect(context(FormTemplateStatus.PUBLISHED),
                        request(null, FormTemplateStatus.PUBLISHED)));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_MODE_INVALID, ex.getErrorCode());
        verify(templateVersionMapper, never()).updateById((FormTemplateVersionDO) any());
    }

    @Test
    void precheckFailsFastWhenReasonIsBlank() {
        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> executor.precheck(context(FormTemplateStatus.PUBLISHED).toBuilder().reason(" ").build()));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, ex.getErrorCode());
        verify(templateVersionMapper, never()).updateById((FormTemplateVersionDO) any());
    }

    @Test
    void markPendingFailsFastWhenTargetAlreadyPending() {
        mockVersion(FormTemplateStatus.PENDING_APPROVAL);

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> executor.markPending(context(FormTemplateStatus.PUBLISHED),
                        request("PI-FORM-TPL-OBSOLETE-301", FormTemplateStatus.PUBLISHED)));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, ex.getErrorCode());
        verify(templateVersionMapper, never()).updateById((FormTemplateVersionDO) any());
    }

    @Test
    void markPendingFailsFastWhenTargetAlreadyObsolete() {
        mockVersion(FormTemplateStatus.OBSOLETE);

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> executor.markPending(context(FormTemplateStatus.PUBLISHED),
                        request("PI-FORM-TPL-OBSOLETE-301", FormTemplateStatus.PUBLISHED)));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, ex.getErrorCode());
        verify(templateVersionMapper, never()).updateById((FormTemplateVersionDO) any());
    }

    private void mockVersion(FormTemplateStatus status) {
        when(templateVersionMapper.selectById(301L)).thenReturn(FormTemplateVersionDO.builder()
                .id(301L)
                .tenantId(122L)
                .templateId(200L)
                .templateName("损耗单")
                .versionNo("V2.0")
                .status(status.name())
                .build());
    }

    private void assertUpdatedStatus(FormTemplateStatus status) {
        ArgumentCaptor<FormTemplateVersionDO> captor = ArgumentCaptor.forClass(FormTemplateVersionDO.class);
        verify(templateVersionMapper).updateById((FormTemplateVersionDO) captor.capture());
        assertEquals(301L, captor.getValue().getId());
        assertEquals(status.name(), captor.getValue().getStatus());
    }

    private BusinessApprovalContext context(FormTemplateStatus objectState) {
        return BusinessApprovalContext.builder()
                .tenantId(122L)
                .dataDomain("FORM_CENTER")
                .systemCode("FORM_CENTER")
                .objectType("FORM_TEMPLATE")
                .objectId("301")
                .objectVersion("V2.0")
                .actionCode("OBSOLETE")
                .objectState(objectState.name())
                .applicantUserId(101L)
                .reason("模板内容已停止使用")
                .build();
    }

    private BusinessApprovalRequest request(String processInstanceId, FormTemplateStatus objectState) {
        return BusinessApprovalRequest.builder()
                .requestId(9101L)
                .tenantId(122L)
                .processInstanceId(processInstanceId)
                .context(context(objectState))
                .build();
    }

}
