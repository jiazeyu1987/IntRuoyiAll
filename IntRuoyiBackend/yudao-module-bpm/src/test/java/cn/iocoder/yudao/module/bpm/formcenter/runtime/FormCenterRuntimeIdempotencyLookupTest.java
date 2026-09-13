package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormCenterRuntimeIdempotencyLookupTest {

    @Mock
    private FormActionInstanceMapper actionInstanceMapper;
    @Mock
    private BusinessApprovalPolicyMapper businessApprovalPolicyMapper;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void findBusinessActionByIdempotency_returnsTheExactCommittedAction() {
        TenantContextHolder.setTenantId(122L);
        FormCenterRuntimeServiceImpl service = new FormCenterRuntimeServiceImpl();
        ReflectionTestUtils.setField(service, "actionInstanceMapper", actionInstanceMapper);
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setDataDomain("DCC");
        context.setSystemCode("DCC");
        context.setObjectType("CONTROLLED_FILE");
        context.setObjectId("920");
        context.setObjectVersion("B/1");
        context.setActionCode("PUBLISH");
        context.setObjectState("ACTIVE");
        context.setReason("文控正式发布 B/1");
        FormActionInstanceDO existing = FormActionInstanceDO.builder()
                .id(58L)
                .instanceCode("FCI-122-58")
                .status("EFFECTIVE")
                .businessContextJson(JsonUtils.toJsonString(context))
                .build();
        when(actionInstanceMapper.selectByBusinessActionAndIdempotency(
                122L, "DCC", "CONTROLLED_FILE", "920", "B/1", "PUBLISH", "DCC-PUBLISH-920"))
                .thenReturn(existing);

        FormInstanceRespVO result = service.findBusinessActionByIdempotency(context, " DCC-PUBLISH-920 ");

        assertEquals(58L, result.getId());
        assertEquals("EFFECTIVE", result.getStatus());
        assertEquals("文控正式发布 B/1", result.getContext().getReason());
        verify(actionInstanceMapper).selectByBusinessActionAndIdempotency(
                122L, "DCC", "CONTROLLED_FILE", "920", "B/1", "PUBLISH", "DCC-PUBLISH-920");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"DCC-PUBLISH-920-K2", "", " "})
    void createInstance_rejectsSameApplicantDraftWhenIdempotencyKeyConflicts(String requestedKey) {
        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> createInstanceWithExistingPublishDraft(requestedKey, "DCC-PUBLISH-920-K1"));

        assertEquals(FormCenterErrorCode.FORM_ACTION_IDEMPOTENCY_CONFLICT, exception.getErrorCode());
        verify(actionInstanceMapper, never()).insert(any(FormActionInstanceDO.class));
        verify(actionInstanceMapper, never()).updateById(any(FormActionInstanceDO.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DCC-PUBLISH-920-K1", " DCC-PUBLISH-920-K1 "})
    void createInstance_sameIdempotencyKeyReturnsExistingDraftWithoutWriting(String requestedKey) {
        FormInstanceRespVO result = createInstanceWithExistingPublishDraft(requestedKey, "DCC-PUBLISH-920-K1");

        assertEquals(58L, result.getId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals("文控正式发布 B/1", result.getContext().getReason());
        verify(actionInstanceMapper, never()).insert(any(FormActionInstanceDO.class));
        verify(actionInstanceMapper, never()).updateById(any(FormActionInstanceDO.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createInstance_missingKeysCannotBeTreatedAsAnIdempotentReplay(String missingKey) {
        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> createInstanceWithExistingPublishDraft(missingKey, missingKey));

        assertEquals(FormCenterErrorCode.FORM_ACTION_IDEMPOTENCY_CONFLICT, exception.getErrorCode());
        verify(actionInstanceMapper, never()).insert(any(FormActionInstanceDO.class));
        verify(actionInstanceMapper, never()).updateById(any(FormActionInstanceDO.class));
    }

    private FormInstanceRespVO createInstanceWithExistingPublishDraft(String requestedKey, String existingKey) {
        TenantContextHolder.setTenantId(122L);
        FormCenterRuntimeServiceImpl service = new FormCenterRuntimeServiceImpl();
        ReflectionTestUtils.setField(service, "actionInstanceMapper", actionInstanceMapper);
        ReflectionTestUtils.setField(service, "businessApprovalPolicyMapper", businessApprovalPolicyMapper);
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setDataDomain("DCC");
        context.setSystemCode("DCC");
        context.setObjectType("CONTROLLED_FILE");
        context.setObjectId("920");
        context.setObjectVersion("B/1");
        context.setActionCode("PUBLISH");
        context.setObjectState("READY_TO_PUBLISH");
        context.setReason("文控正式发布 B/1");
        BusinessApprovalPolicyDO policy = BusinessApprovalPolicyDO.builder()
                .id(7L)
                .tenantId(122L)
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .actionCode("PUBLISH")
                .objectState("READY_TO_PUBLISH")
                .policyMode("BPM_REQUIRED")
                .processDefinitionKey("dcc-controlled-file-publish")
                .status("PUBLISHED")
                .build();
        FormActionInstanceDO staleDraft = FormActionInstanceDO.builder()
                .id(58L)
                .instanceCode("FCI-122-58")
                .tenantId(122L)
                .policyId(7L)
                .applicantUserId(99L)
                .status("DRAFT")
                .dataDomain("DCC")
                .systemCode("DCC")
                .objectType("CONTROLLED_FILE")
                .objectId("920")
                .objectVersion("B/1")
                .actionCode("PUBLISH")
                .objectState("READY_TO_PUBLISH")
                .idempotencyKey(existingKey)
                .businessContextJson(JsonUtils.toJsonString(context))
                .formDataJson(JsonUtils.toJsonString(Map.of("reason", "旧发布原因")))
                .build();
        when(businessApprovalPolicyMapper.selectPublishedByAction(122L, "DCC", "DCC",
                "CONTROLLED_FILE", "PUBLISH", "READY_TO_PUBLISH")).thenReturn(List.of(policy));
        when(actionInstanceMapper.selectSameBusinessAction(122L, "DCC", "CONTROLLED_FILE",
                "920", "B/1", "PUBLISH")).thenReturn(List.of(staleDraft));
        FormInstanceCreateReqVO reqVO = new FormInstanceCreateReqVO();
        reqVO.setContext(context);
        reqVO.setIdempotencyKey(requestedKey);
        reqVO.setFormData(Map.of("reason", "文控正式发布 B/1"));
        return service.createInstance(reqVO, 99L);
    }
}
