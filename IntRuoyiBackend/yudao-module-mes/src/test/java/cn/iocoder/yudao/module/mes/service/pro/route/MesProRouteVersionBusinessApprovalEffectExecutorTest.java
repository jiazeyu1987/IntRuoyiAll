package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionBusinessApprovalEffectExecutorTest {

    private MesProRouteVersionBusinessApprovalEffectExecutor executor;

    @Mock
    private MesProRouteVersionLifecycleService lifecycleService;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteControlledContentAdapter platformAdapter;

    @BeforeEach
    void setUp() {
        executor = new MesProRouteVersionBusinessApprovalEffectExecutor();
        ReflectionTestUtils.setField(executor, "lifecycleService", lifecycleService);
        ReflectionTestUtils.setField(executor, "routeVersionMapper", routeVersionMapper);
        ReflectionTestUtils.setField(executor, "platformAdapter", platformAdapter);
    }

    @Test
    void executorCodeIsStablePlatformCode() {
        assertEquals("MES_ROUTE_VERSION_PUBLISH", executor.getExecutorCode());
    }

    @Test
    void terminalApprovalEffectsRunInSingleRollbackTransaction() {
        Transactional transactional = MesProRouteVersionBusinessApprovalEffectExecutor.class
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(1, transactional.rollbackFor().length);
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }

    @Test
    void directExecutionMovesDraftToReadyAndPublishesCandidateWithApplicantUser() {
        when(routeVersionMapper.selectById(1001L)).thenReturn(draftVersion());
        when(routeVersionMapper.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);
        when(lifecycleService.publishCandidate(1001L, 501L)).thenReturn(activeVersion());
        doAnswer(invocation -> {
            MesProRouteVersionDO submitted = invocation.getArgument(0);
            assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, submitted.getLifecycleStatus());
            return null;
        }).when(platformAdapter).recordSubmitted(any(MesProRouteVersionDO.class), eq(501L), isNull());

        BusinessApprovalEffectResult result = executor.executeDirect(context(), request());

        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE, result.getResultState());
        verify(platformAdapter).recordSubmitted(any(MesProRouteVersionDO.class), eq(501L), isNull());
        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        assertEquals(1001L, updateCaptor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                updateCaptor.getValue().getLifecycleStatus());
        assertEquals(501L, updateCaptor.getValue().getSubmittedBy());
        assertNotNull(updateCaptor.getValue().getSubmittedTime());
        ArgumentCaptor<MesProRouteVersionDO> approvedCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(platformAdapter).recordApproved(approvedCaptor.capture(), eq(501L),
                eq("BUSINESS_APPROVAL:2001:DIRECT_APPROVED"));
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                approvedCaptor.getValue().getLifecycleStatus());
        verify(lifecycleService).publishCandidate(1001L, 501L);
    }

    @Test
    void approvedExecutionPublishesCandidateWithTerminalActor() {
        when(routeVersionMapper.selectById(1001L)).thenReturn(pendingVersion());
        when(routeVersionMapper.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);
        when(lifecycleService.publishCandidate(1001L, 902L)).thenReturn(activeVersion());

        BusinessApprovalEffectResult result = executor.executeApproved(context(), requestWithProcessInstance(), 902L);

        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE, result.getResultState());
        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        assertEquals(1001L, updateCaptor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                updateCaptor.getValue().getLifecycleStatus());
        ArgumentCaptor<MesProRouteVersionDO> approvedCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(platformAdapter).recordApproved(approvedCaptor.capture(), eq(902L),
                eq("BUSINESS_APPROVAL:bpm-2001:APPROVED"));
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                approvedCaptor.getValue().getLifecycleStatus());
        verify(lifecycleService).publishCandidate(1001L, 902L);
    }

    @Test
    void markPendingPersistsDomainPendingStateWithoutPublishing() {
        when(routeVersionMapper.selectById(1001L)).thenReturn(draftVersion());
        when(routeVersionMapper.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);

        BusinessApprovalEffectResult result = executor.markPending(context(), requestWithProcessInstance());

        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL, result.getResultState());
        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        assertEquals(1001L, updateCaptor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL,
                updateCaptor.getValue().getLifecycleStatus());
        assertEquals(501L, updateCaptor.getValue().getSubmittedBy());
        assertEquals("bpm-2001", updateCaptor.getValue().getApprovalProcessInstanceId());
        ArgumentCaptor<MesProRouteVersionDO> submittedCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(platformAdapter).recordSubmitted(submittedCaptor.capture(), eq(501L), eq("bpm-2001"));
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL,
                submittedCaptor.getValue().getLifecycleStatus());
        verify(lifecycleService, never()).publishCandidate(1001L, 501L);
    }

    @Test
    void rejectedExecutionPersistsRejectedStateAndDoesNotPublish() {
        when(routeVersionMapper.selectById(1001L)).thenReturn(pendingVersion());
        when(routeVersionMapper.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);

        BusinessApprovalEffectResult result = executor.reject(context(), requestWithProcessInstance(), 903L,
                "snapshot incomplete");

        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED, result.getResultState());
        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        assertEquals(1001L, updateCaptor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED,
                updateCaptor.getValue().getLifecycleStatus());
        assertEquals("snapshot incomplete", updateCaptor.getValue().getRemark());
        ArgumentCaptor<MesProRouteVersionDO> rejectedCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(platformAdapter).recordRejected(rejectedCaptor.capture(), eq(903L), eq("snapshot incomplete"),
                eq("BUSINESS_APPROVAL:bpm-2001:REJECTED"));
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED,
                rejectedCaptor.getValue().getLifecycleStatus());
        verify(lifecycleService, never()).publishCandidate(1001L, 903L);
    }

    @Test
    void cancelledExecutionClearsDomainApprovalFieldsAndDoesNotPublish() {
        when(routeVersionMapper.selectById(1001L)).thenReturn(pendingVersion());
        when(routeVersionMapper.updateApprovalFieldsToDraft(1001L)).thenReturn(1);

        BusinessApprovalEffectResult result = executor.cancel(context(), requestWithProcessInstance(), 904L,
                "withdrawn");

        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, result.getResultState());
        verify(routeVersionMapper).updateApprovalFieldsToDraft(1001L);
        verify(platformAdapter).recordWithdrawn(pendingVersion(), 904L);
        verify(lifecycleService, never()).publishCandidate(1001L, 904L);
    }

    private static BusinessApprovalContext context() {
        return BusinessApprovalContext.builder()
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("ROUTE_VERSION")
                .objectId("1001")
                .objectVersion("V2")
                .actionCode("PUBLISH")
                .objectState(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .applicantUserId(501L)
                .reason("publish route version")
                .build();
    }

    private static BusinessApprovalRequest request() {
        return BusinessApprovalRequest.builder()
                .requestId(2001L)
                .tenantId(122L)
                .effectExecutorCode("MES_ROUTE_VERSION_PUBLISH")
                .status(BusinessApprovalRequestStatus.PENDING_BPM)
                .context(context())
                .build();
    }

    private static BusinessApprovalRequest requestWithProcessInstance() {
        return request().toBuilder()
                .processInstanceId("bpm-2001")
                .build();
    }

    private static MesProRouteVersionDO draftVersion() {
        return MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(1000L)
                .build();
    }

    private static MesProRouteVersionDO pendingVersion() {
        return MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL)
                .sourceRouteVersionId(1000L)
                .approvalProcessInstanceId("bpm-2001")
                .build();
    }

    private static MesProRouteVersionDO activeVersion() {
        return MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
    }

}
