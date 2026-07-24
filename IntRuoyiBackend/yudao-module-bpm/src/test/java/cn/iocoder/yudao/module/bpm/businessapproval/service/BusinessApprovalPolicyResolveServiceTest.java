package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyResolution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessApprovalPolicyResolveServiceTest {

    @Test
    void resolveFailsFastWhenContextMissesObjectVersion() {
        BusinessApprovalPolicyResolveService service = new BusinessApprovalPolicyResolveService(List.of());

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> service.resolve(baseContext().objectVersion(null).build()));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, ex.getErrorCode());
    }

    @Test
    void resolveFailsFastWhenPublishedPolicyIsMissing() {
        BusinessApprovalPolicyResolveService service = new BusinessApprovalPolicyResolveService(List.of());

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> service.resolve(baseContext().build()));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void resolveFailsFastWhenMoreThanOnePublishedPolicyMatches() {
        BusinessApprovalPolicyResolveService service = new BusinessApprovalPolicyResolveService(List.of(
                basePolicy().policyId(1L).build(),
                basePolicy().policyId(2L).build()));

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> service.resolve(baseContext().build()));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_CONFLICT, ex.getErrorCode());
    }

    @Test
    void resolveReturnsExplicitDirectPolicy() {
        BusinessApprovalPolicyResolveService service = new BusinessApprovalPolicyResolveService(List.of(
                basePolicy().mode(BusinessApprovalPolicyMode.DIRECT).processDefinitionKey(null).build()));

        BusinessApprovalPolicyResolution resolution = service.resolve(baseContext().build());

        assertEquals(BusinessApprovalPolicyMode.DIRECT, resolution.getMode());
        assertEquals("MES_ROUTE_VERSION_PUBLISH", resolution.getEffectExecutorCode());
    }

    @Test
    void resolveReturnsExplicitBpmRequiredPolicy() {
        BusinessApprovalPolicyResolveService service = new BusinessApprovalPolicyResolveService(List.of(
                basePolicy().mode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                        .processDefinitionKey("mes-route-version-approval-v1")
                        .build()));

        BusinessApprovalPolicyResolution resolution = service.resolve(baseContext().build());

        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED, resolution.getMode());
        assertEquals("mes-route-version-approval-v1", resolution.getProcessDefinitionKey());
    }

    @Test
    void resolveMatchesAllObjectStatePolicyForConcreteContextState() {
        BusinessApprovalPolicyResolveService service = new BusinessApprovalPolicyResolveService(List.of(
                basePolicy().objectState("ALL").build()));

        BusinessApprovalPolicyResolution resolution = service.resolve(baseContext().build());

        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED, resolution.getMode());
        assertEquals("MES_ROUTE_VERSION_PUBLISH", resolution.getEffectExecutorCode());
    }

    static BusinessApprovalContext.BusinessApprovalContextBuilder baseContext() {
        return BusinessApprovalContext.builder()
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("ROUTE_VERSION")
                .objectId("1001")
                .objectVersion("V2")
                .actionCode("PUBLISH")
                .objectState("READY_TO_PUBLISH")
                .applicantUserId(501L)
                .reason("publish route version");
    }

    static BusinessApprovalPolicy.BusinessApprovalPolicyBuilder basePolicy() {
        return BusinessApprovalPolicy.builder()
                .policyId(10L)
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("ROUTE_VERSION")
                .actionCode("PUBLISH")
                .objectState("READY_TO_PUBLISH")
                .mode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                .processDefinitionKey("mes-route-version-approval-v1")
                .effectExecutorCode("MES_ROUTE_VERSION_PUBLISH")
                .status(BusinessApprovalPolicy.STATUS_PUBLISHED);
    }

}
