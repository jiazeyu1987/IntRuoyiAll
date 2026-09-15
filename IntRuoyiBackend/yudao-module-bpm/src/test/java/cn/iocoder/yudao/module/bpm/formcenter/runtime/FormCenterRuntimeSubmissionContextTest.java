package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.*;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FormCenterRuntimeSubmissionContextTest extends BaseMockitoUnitTest {
    @org.junit.jupiter.api.BeforeEach
    void tenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(122L); }
    @org.junit.jupiter.api.AfterEach
    void clearTenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
    @Mock private FormActionInstanceMapper actionInstanceMapper;
    @Mock private BusinessApprovalPolicyMapper businessApprovalPolicyMapper;
    @Mock private FormActionSnapshotMapper actionSnapshotMapper;
    @Mock private FormEffectExecutionMapper effectExecutionMapper;
    @InjectMocks private FormCenterRuntimeServiceImpl runtime;

    @Test
    void publicSubmissionUsesActualSubmitterAndCannotClaimAutomaticBackfill() {
        var instance = FormActionInstanceDO.builder().id(10L).tenantId(122L).policyId(20L)
                .instanceCode("FC-10").applicantUserId(99L).idempotencyKey("submit-10").status("DRAFT")
                .businessContextJson("{\"tenantId\":122,\"dataDomain\":\"MES\",\"systemCode\":\"MES\","
                        + "\"objectType\":\"EDHR_ROUTE_FORM\",\"objectId\":\"700\",\"objectVersion\":\"100\","
                        + "\"actionCode\":\"EDHR_RF_100_FORM\",\"objectState\":\"ACTIVE\"}").formDataJson("{}").build();
        when(actionInstanceMapper.selectById(10L)).thenReturn(instance);
        when(businessApprovalPolicyMapper.selectById(20L)).thenReturn(BusinessApprovalPolicyDO.builder()
                .id(20L).tenantId(122L).dataDomain("MES").systemCode("MES").objectType("EDHR_ROUTE_FORM")
                .actionCode("EDHR_RF_100_FORM").objectState("ACTIVE").policyMode("DIRECT")
                .effectExecutorCode("MES_EDHR_ROUTE_FORM_FILL").status("PUBLISHED").build());
        var adapter = mock(FormControlledActionLifecycleAdapter.class);
        when(adapter.supports(any())).thenReturn(true);
        when(adapter.preflight(any())).thenReturn(FormBusinessEffectPrecheck.pass());
        ReflectionTestUtils.setField(runtime, "lifecycleAdapters", List.of(adapter));
        var executor = mock(FormBusinessEffectExecutor.class);
        when(executor.getExecutorCode()).thenReturn("MES_EDHR_ROUTE_FORM_FILL");
        when(executor.execute(any(), any())).thenAnswer(call -> {
            FormActionInstance domain = call.getArgument(0);
            var json = JsonUtils.getObjectMapper().readTree(JsonUtils.toJsonString(domain));
            var context = json.get("executionContext");
            if (context == null || context.path("actorUserId").asLong() != 77L
                    || !"MANUAL".equals(context.path("kind").asText())) {
                return FormBusinessEffectResult.failure("actual submitter/manual authority was not propagated");
            }
            assertEquals(99L, domain.getApplicantUserId(), "creator identity remains historical");
            return FormBusinessEffectResult.success("700");
        });
        ReflectionTestUtils.setField(runtime, "effectExecutors", List.of(executor));
        var request = new FormInstanceSubmitReqVO();
        request.setFormData(Map.of("executionContext", Map.of("kind", "VERIFIED_BACKFILL", "actorUserId", 99),
                "automaticBackfill", true));
        var response = runtime.submitInstance(10L, request, 77L);
        assertEquals("EFFECTIVE", response.getStatus());
    }
}
