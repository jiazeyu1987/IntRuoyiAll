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

class FormTemplateUpgradeBusinessApprovalEffectExecutorTest extends BaseMockitoUnitTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;

    @InjectMocks
    private FormTemplateUpgradeBusinessApprovalEffectExecutor executor;

    @Test
    void markPendingLocksDraftVersionWhenBpmProcessStarted() {
        mockVersion(FormTemplateStatus.DRAFT);
        when(templateVersionMapper.updateById((FormTemplateVersionDO) any())).thenReturn(1);

        BusinessApprovalEffectResult result = executor.markPending(context(), request("PI-FORM-TPL-301"));

        assertEquals(FormTemplateStatus.PENDING_APPROVAL.name(), result.getResultState());
        assertUpdatedStatus(FormTemplateStatus.PENDING_APPROVAL);
    }

    @Test
    void executeApprovedPublishesPendingVersion() {
        mockVersion(FormTemplateStatus.PENDING_APPROVAL);
        when(templateVersionMapper.updateById((FormTemplateVersionDO) any())).thenReturn(1);

        BusinessApprovalEffectResult result = executor.executeApproved(context(), request("PI-FORM-TPL-301"), 101L);

        assertEquals(FormTemplateStatus.PUBLISHED.name(), result.getResultState());
        assertUpdatedStatus(FormTemplateStatus.PUBLISHED);
    }

    @Test
    void rejectMovesPendingVersionToRejected() {
        mockVersion(FormTemplateStatus.PENDING_APPROVAL);
        when(templateVersionMapper.updateById((FormTemplateVersionDO) any())).thenReturn(1);

        BusinessApprovalEffectResult result = executor.reject(context(), request("PI-FORM-TPL-301"), 101L, "驳回");

        assertEquals(FormTemplateStatus.REJECTED.name(), result.getResultState());
        assertUpdatedStatus(FormTemplateStatus.REJECTED);
    }

    @Test
    void cancelMovesPendingVersionToRejected() {
        mockVersion(FormTemplateStatus.PENDING_APPROVAL);
        when(templateVersionMapper.updateById((FormTemplateVersionDO) any())).thenReturn(1);

        BusinessApprovalEffectResult result = executor.cancel(context(), request("PI-FORM-TPL-301"), 101L, "撤回");

        assertEquals(FormTemplateStatus.REJECTED.name(), result.getResultState());
        assertUpdatedStatus(FormTemplateStatus.REJECTED);
    }

    @Test
    void executeDirectIsRejectedBecauseUpgradeMustGoThroughBpmApproval() {
        mockVersion(FormTemplateStatus.DRAFT);

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> executor.executeDirect(context(), request(null)));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_MODE_INVALID, ex.getErrorCode());
        verify(templateVersionMapper, never()).updateById((FormTemplateVersionDO) any());
    }

    @Test
    void markPendingFailsFastWhenTargetAlreadyPending() {
        mockVersion(FormTemplateStatus.PENDING_APPROVAL);

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> executor.markPending(context(), request("PI-FORM-TPL-301")));

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

    private BusinessApprovalContext context() {
        return BusinessApprovalContext.builder()
                .tenantId(122L)
                .dataDomain("FORM_CENTER")
                .systemCode("FORM_CENTER")
                .objectType("FORM_TEMPLATE")
                .objectId("301")
                .objectVersion("V2.0")
                .actionCode("UPGRADE")
                .objectState(FormTemplateStatus.DRAFT.name())
                .applicantUserId(101L)
                .reason("升版")
                .build();
    }

    private BusinessApprovalRequest request(String processInstanceId) {
        return BusinessApprovalRequest.builder()
                .requestId(9001L)
                .tenantId(122L)
                .processInstanceId(processInstanceId)
                .context(context())
                .build();
    }

}
