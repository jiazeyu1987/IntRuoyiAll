package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormActionResolutionRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicySaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicySlotReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormApprovalMode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersionRef;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormTemplateRecognizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class FormCenterRuntimeLatestTemplateVersionTest extends BaseMockitoUnitTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;
    @Mock
    private FormActionPolicyMapper actionPolicyMapper;
    @Mock
    private BusinessApprovalPolicyMapper businessApprovalPolicyMapper;
    @Mock
    private FormActionInstanceMapper actionInstanceMapper;
    @Mock
    private FormTemplateRecognizer templateRecognizer;
    @Mock
    private BpmProcessInstanceApi processInstanceApi;

    @InjectMocks
    private FormCenterRuntimeServiceImpl runtimeService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void resolveAction_readsPublishedBusinessApprovalPolicyForDccUpload() {
        TenantContextHolder.setTenantId(122L);
        BusinessApprovalPolicyDO policy = BusinessApprovalPolicyDO.builder()
                .id(10L)
                .tenantId(122L)
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .actionCode("UPLOAD")
                .objectState("DRAFT")
                .policyMode(BusinessApprovalPolicyMode.BPM_REQUIRED.name())
                .processDefinitionKey("form-change-approval")
                .effectExecutorCode("DCC_UPLOAD")
                .status(BusinessApprovalPolicy.STATUS_PUBLISHED)
                .build();
        when(businessApprovalPolicyMapper.selectPublishedByAction(122L, "DCC", "DCC", "CONTROLLED_FILE", "UPLOAD", "DRAFT"))
                .thenReturn(List.of(policy));

        FormActionResolutionRespVO resolution = runtimeService.resolveAction(contextReq());

        assertEquals(10L, resolution.getPolicyId());
        assertEquals(FormPolicyType.NONE.name(), resolution.getPolicyType());
        assertEquals(FormApprovalMode.BPM_REQUIRED.name(), resolution.getApprovalMode());
        assertEquals("form-change-approval", resolution.getBpmProcessKey());
        assertEquals(0, resolution.getSlots().size());
        verify(actionPolicyMapper, never()).selectPublishedByAction(122L, "DCC", "DCC", "CONTROLLED_FILE",
                "UPLOAD", "DRAFT");
    }

    @Test
    void resolveAction_mapsPublishedBusinessApprovalPolicyToBpmOnlyPolicy() {
        TenantContextHolder.setTenantId(122L);
        BusinessApprovalPolicyDO policy = BusinessApprovalPolicyDO.builder()
                .id(11L)
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("EDHR_BATCH_EXECUTION")
                .actionCode("RELEASE")
                .objectState("PRECHECK_PASSED")
                .policyMode(BusinessApprovalPolicyMode.BPM_REQUIRED.name())
                .processDefinitionKey("mes-edhr-approval-v1")
                .effectExecutorCode("EDHR_RELEASE")
                .status(BusinessApprovalPolicy.STATUS_PUBLISHED)
                .build();
        when(businessApprovalPolicyMapper.selectPublishedByAction(122L, "MES", "MES", "EDHR_BATCH_EXECUTION",
                "RELEASE", "PRECHECK_PASSED")).thenReturn(List.of(policy));

        FormActionResolutionRespVO resolution = runtimeService.resolveAction(edhrReleaseContextReq());

        assertEquals(FormPolicyType.NONE.name(), resolution.getPolicyType());
        assertEquals("mes-edhr-approval-v1", resolution.getBpmProcessKey());
        assertEquals(0, resolution.getSlots().size());
        verify(actionPolicyMapper, never()).selectPublishedByAction(122L, "MES", "MES", "EDHR_BATCH_EXECUTION",
                "RELEASE", "PRECHECK_PASSED");
    }

    @Test
    void resolveAction_readsPublishedBusinessApprovalPolicyForBatchExecutionVoid() {
        TenantContextHolder.setTenantId(122L);
        BusinessApprovalPolicyDO policy = BusinessApprovalPolicyDO.builder()
                .id(2026072301L)
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("EDHR_BATCH_EXECUTION")
                .actionCode("VOID")
                .objectState("CLOSED")
                .policyMode(BusinessApprovalPolicyMode.BPM_REQUIRED.name())
                .processDefinitionKey("mes-edhr-batch-execution-void-v1")
                .effectExecutorCode("EDHR_BATCH_VOID")
                .status(BusinessApprovalPolicy.STATUS_PUBLISHED)
                .build();
        when(businessApprovalPolicyMapper.selectPublishedByAction(122L, "MES", "MES", "EDHR_BATCH_EXECUTION",
                "VOID", "CLOSED")).thenReturn(List.of(policy));

        FormActionResolutionRespVO resolution = runtimeService.resolveAction(edhrVoidContextReq());

        assertEquals(2026072301L, resolution.getPolicyId());
        assertEquals(FormPolicyType.NONE.name(), resolution.getPolicyType());
        assertEquals(FormApprovalMode.BPM_REQUIRED.name(), resolution.getApprovalMode());
        assertEquals("mes-edhr-batch-execution-void-v1", resolution.getBpmProcessKey());
        assertEquals(0, resolution.getSlots().size());
        verify(actionPolicyMapper, never()).selectPublishedByAction(122L, "MES", "MES", "EDHR_BATCH_EXECUTION",
                "VOID", "CLOSED");
    }

    @Test
    void savePolicy_bindsStableTemplateIdentityInsteadOfTreatingTemplateIdAsVersionRowId() {
        TenantContextHolder.setTenantId(122L);
        when(templateVersionMapper.selectLatestPublishedByTemplateId(122L, 200L))
                .thenReturn(FormTemplateVersionDO.builder()
                        .id(1002L)
                        .templateId(200L)
                        .tenantId(122L)
                        .templateName("Change Form")
                        .versionNo("V2")
                        .status(FormTemplateStatus.PUBLISHED.name())
                        .build());

        runtimeService.savePolicy(policySaveReq(200L));

        ArgumentCaptor<FormActionPolicyDO> policyCaptor = ArgumentCaptor.forClass(FormActionPolicyDO.class);
        verify(actionPolicyMapper).insert(policyCaptor.capture());
        List<FormPolicySlot> slots = JsonUtils.parseArray(policyCaptor.getValue().getSlotsJson(), FormPolicySlot.class);
        FormTemplateVersionRef savedRef = slots.get(0).getTemplateVersionRef();
        assertEquals(1002L, savedRef.getVersionId());
        assertEquals("200", savedRef.getTemplateCode());
        assertEquals("V2", savedRef.getVersionNo());
    }

    private BusinessActionContextReqVO contextReq() {
        BusinessActionContextReqVO reqVO = new BusinessActionContextReqVO();
        reqVO.setTenantId(122L);
        reqVO.setDataDomain("DCC");
        reqVO.setSystemCode("DCC");
        reqVO.setObjectType("CONTROLLED_FILE");
        reqVO.setObjectId("FILE-1001");
        reqVO.setObjectVersion("V1");
        reqVO.setActionCode("UPLOAD");
        reqVO.setObjectState("DRAFT");
        return reqVO;
    }

    private BusinessActionContextReqVO edhrReleaseContextReq() {
        BusinessActionContextReqVO reqVO = new BusinessActionContextReqVO();
        reqVO.setTenantId(122L);
        reqVO.setDataDomain("MES");
        reqVO.setSystemCode("MES");
        reqVO.setObjectType("EDHR_BATCH_EXECUTION");
        reqVO.setObjectId("900000000702");
        reqVO.setObjectVersion("89");
        reqVO.setActionCode("RELEASE");
        reqVO.setObjectState("PRECHECK_PASSED");
        return reqVO;
    }

    private BusinessActionContextReqVO edhrVoidContextReq() {
        BusinessActionContextReqVO reqVO = new BusinessActionContextReqVO();
        reqVO.setTenantId(122L);
        reqVO.setDataDomain("MES");
        reqVO.setSystemCode("MES");
        reqVO.setObjectType("EDHR_BATCH_EXECUTION");
        reqVO.setObjectId("900000000702");
        reqVO.setObjectVersion("89");
        reqVO.setActionCode("VOID");
        reqVO.setObjectState("CLOSED");
        return reqVO;
    }

    private FormPolicySaveReqVO policySaveReq(Long templateId) {
        FormPolicySlotReqVO slotReqVO = new FormPolicySlotReqVO();
        slotReqVO.setSlotCode("change-request");
        slotReqVO.setRequired(true);
        slotReqVO.setTemplateId(templateId);

        FormPolicySaveReqVO reqVO = new FormPolicySaveReqVO();
        reqVO.setDataDomain("DCC");
        reqVO.setSystemCode("DCC");
        reqVO.setObjectType("CONTROLLED_FILE");
        reqVO.setActionCode("UPLOAD");
        reqVO.setObjectState("DRAFT");
        reqVO.setPolicyType(FormPolicyType.REQUIRED.name());
        reqVO.setBpmProcessKey("form-change-approval");
        reqVO.setEffectExecutorCode("DCC_UPLOAD");
        reqVO.setSlots(List.of(slotReqVO));
        return reqVO;
    }
}
