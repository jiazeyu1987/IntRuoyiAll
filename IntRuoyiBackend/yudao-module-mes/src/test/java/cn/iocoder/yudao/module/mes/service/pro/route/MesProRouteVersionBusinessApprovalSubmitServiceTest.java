package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionBusinessApprovalSubmitServiceTest {

    @InjectMocks
    private MesProRouteVersionBusinessApprovalSubmitServiceImpl service;

    @Mock
    private MesProRouteVersionWorkflowService workflowService;
    @Mock
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void submitAndPublishCandidateSubmitsApprovalWithoutElectronicSignaturePassword() {
        MesProRouteVersionDO draft = draftCandidate();
        MesProRouteVersionDO pending = draftCandidate();
        pending.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        when(workflowService.getVersion(1002L)).thenReturn(draft, pending);
        when(businessApprovalOrchestrator.submit(any(BusinessApprovalContext.class)))
                .thenReturn(BusinessApprovalRequest.builder()
                        .requestId(3001L)
                        .status(BusinessApprovalRequestStatus.PENDING_BPM)
                        .build());
        TenantContextHolder.setTenantId(122L);

        MesProRouteVersionDO result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(501L);
            result = service.submitAndPublishCandidate(1002L);
        }

        ArgumentCaptor<BusinessApprovalContext> contextCaptor =
                ArgumentCaptor.forClass(BusinessApprovalContext.class);
        verify(businessApprovalOrchestrator).submit(contextCaptor.capture());
        BusinessApprovalContext context = contextCaptor.getValue();
        assertEquals(122L, context.getTenantId());
        assertEquals("MES", context.getDataDomain());
        assertEquals("MES", context.getSystemCode());
        assertEquals("ROUTE_VERSION", context.getObjectType());
        assertEquals("1002", context.getObjectId());
        assertEquals("V2", context.getObjectVersion());
        assertEquals("PUBLISH", context.getActionCode());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, context.getObjectState());
        assertEquals(501L, context.getApplicantUserId());
        Map<String, Object> variables = context.getVariables();
        assertEquals(9001L, variables.get("routeId"));
        assertEquals(1002L, variables.get("routeVersionId"));
        assertEquals("V2", variables.get("routeVersionNo"));
        assertEquals("RT-001", variables.get("routeCode"));
        assertEquals("球囊扩张压力泵工艺路线", variables.get("routeName"));
        assertSame(pending, result);

        InOrder order = inOrder(workflowService, businessApprovalOrchestrator);
        order.verify(workflowService).getVersion(1002L);
        order.verify(businessApprovalOrchestrator).submit(any(BusinessApprovalContext.class));
        order.verify(workflowService).getVersion(1002L);
    }

    @Test
    void submitAndPublishCandidateReturnsAlreadyActiveVersionWithoutSubmittingApprovalAgain() {
        MesProRouteVersionDO active = activeCandidate();
        when(workflowService.getVersion(1002L)).thenReturn(active, active);
        TenantContextHolder.setTenantId(122L);

        MesProRouteVersionDO result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(501L);
            result = service.submitAndPublishCandidate(1002L);
        }

        assertSame(active, result);
        verify(businessApprovalOrchestrator, never()).submit(any(BusinessApprovalContext.class));
    }

    @Test
    void submitAndPublishCandidateRejectsNonDraftNonActiveVersionWithoutSubmittingApproval() {
        MesProRouteVersionDO pending = draftCandidate();
        pending.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        when(workflowService.getVersion(1002L)).thenReturn(pending);
        TenantContextHolder.setTenantId(122L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.submitAndPublishCandidate(1002L));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
        verify(businessApprovalOrchestrator, never()).submit(any(BusinessApprovalContext.class));
    }

    @Test
    void submitAndPublishCandidateFailsFastWhenApplicantMissing() {
        when(workflowService.getVersion(1002L)).thenReturn(draftCandidate());
        TenantContextHolder.setTenantId(122L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.submitAndPublishCandidate(1002L));

            assertEquals("route version publish applicant is required", ex.getMessage());
        }
        verify(businessApprovalOrchestrator, never()).submit(any(BusinessApprovalContext.class), any());
    }

    private MesProRouteVersionDO draftCandidate() {
        return MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(1001L)
                .routeSnapshotJson("""
                        {
                          "routeId": 9001,
                          "routeCode": "RT-001",
                          "routeName": "球囊扩张压力泵工艺路线"
                        }
                        """)
                .build();
    }

    private MesProRouteVersionDO activeCandidate() {
        return MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .sourceRouteVersionId(1001L)
                .build();
    }

}
