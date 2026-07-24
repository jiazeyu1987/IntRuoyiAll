package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionApprovalServiceTest {

    @InjectMocks
    private MesProRouteVersionApprovalServiceImpl approvalService;

    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteVersionLifecycleService lifecycleService;
    @Mock
    private MesProRouteControlledContentAdapter platformAdapter;

    @Test
    void approveCallback_shouldPublishOnlyMatchingCandidateIdempotently() {
        MesProRouteVersionDO pending = pendingCandidate();
        MesProRouteVersionDO published = MesProRouteVersionDO.builder()
                .id(pending.getId())
                .routeId(pending.getRouteId())
                .versionNo(pending.getVersionNo())
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .approvalProcessInstanceId(pending.getApprovalProcessInstanceId())
                .build();
        when(routeVersionMapper.selectByApprovalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8"))
                .thenReturn(pending);
        when(lifecycleService.publishCandidate(pending.getId(), 501L)).thenReturn(published);

        MesProRouteVersionApprovalResult result = approvalService.handleApprovalCallback(
                "fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-APPROVE", "APPROVED", null, 501L);

        assertEquals(pending.getId(), result.routeVersionId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE, result.lifecycleStatus());
        assertEquals("APPROVED", result.approvalResult());
        assertEquals("PROCESSED", result.processedResult());
        verify(platformAdapter).recordApproved(pending, 501L,
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-APPROVE");
        verify(lifecycleService).publishCandidate(pending.getId(), 501L);

        MesProRouteVersionDO alreadyActive = published;
        when(routeVersionMapper.selectByApprovalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8"))
                .thenReturn(alreadyActive);

        MesProRouteVersionApprovalResult duplicate = approvalService.handleApprovalCallback(
                "fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-APPROVE", "APPROVED", null, 501L);

        assertEquals("DUPLICATE", duplicate.processedResult());
        verify(lifecycleService).publishCandidate(pending.getId(), 501L);
    }

    @Test
    void rejectCallback_shouldKeepCurrentActiveVersion() {
        MesProRouteVersionDO pending = pendingCandidate();
        when(routeVersionMapper.selectByApprovalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8"))
                .thenReturn(pending);

        MesProRouteVersionApprovalResult result = approvalService.handleApprovalCallback(
                "fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-REJECT", "REJECTED", "资料不完整", 502L);

        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(captor.capture());
        assertEquals(pending.getId(), captor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED, captor.getValue().getLifecycleStatus());
        assertEquals("资料不完整", captor.getValue().getRemark());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED, result.lifecycleStatus());
        assertEquals("REJECTED", result.approvalResult());
        verify(platformAdapter).recordRejected(pending, 502L, "资料不完整",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-REJECT");
        verify(lifecycleService, never()).publishCandidate(pending.getId(), 502L);
    }

    @Test
    void cancelCallback_shouldReturnDraftWhenPendingApprovalCanceledExternally() {
        MesProRouteVersionDO pending = pendingCandidate();
        when(routeVersionMapper.selectByApprovalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8"))
                .thenReturn(pending);

        MesProRouteVersionApprovalResult result = approvalService.handleApprovalCallback(
                "fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-CANCEL", "CANCELED", "申请人撤回", 503L);

        verify(routeVersionMapper).updateApprovalFieldsToDraft(pending.getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, result.lifecycleStatus());
        assertEquals("CANCELED", result.approvalResult());
        assertEquals("PROCESSED", result.processedResult());
        verify(platformAdapter).recordWithdrawn(pending, 503L);
        verify(lifecycleService, never()).publishCandidate(pending.getId(), 503L);
    }

    @Test
    void cancelCallback_shouldIgnoreAlreadyWithdrawnDraftWithStaleProcessId() {
        MesProRouteVersionDO draft = pendingCandidate();
        draft.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        when(routeVersionMapper.selectByApprovalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8"))
                .thenReturn(draft);

        MesProRouteVersionApprovalResult result = approvalService.handleApprovalCallback(
                "fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-CANCEL", "CANCELED", "申请人撤回", 503L);

        verify(routeVersionMapper).updateApprovalFieldsToDraft(draft.getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, result.lifecycleStatus());
        assertEquals("CANCELED", result.approvalResult());
        assertEquals("DUPLICATE", result.processedResult());
        verify(lifecycleService, never()).publishCandidate(draft.getId(), 503L);
    }

    @Test
    void cancelCallback_shouldIgnoreAlreadyWithdrawnCandidateWhenApprovalLinkCleared() {
        when(routeVersionMapper.selectByApprovalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8"))
                .thenReturn(null);

        MesProRouteVersionApprovalResult result = approvalService.handleApprovalCallback(
                "fbede791-8138-11f1-80b5-00155d3585b8",
                "BPM-fbede791-8138-11f1-80b5-00155d3585b8-CANCEL", "CANCELED", "申请人撤回", 503L);

        assertEquals("CANCELED", result.approvalResult());
        assertEquals("DUPLICATE", result.processedResult());
        verify(lifecycleService, never()).publishCandidate(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void approveCallback_shouldFailFastWhenApprovalActorMissing() {
        MesProRouteVersionDO pending = pendingCandidate();
        when(routeVersionMapper.selectByApprovalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8"))
                .thenReturn(pending);

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> approvalService.handleApprovalCallback(
                        "fbede791-8138-11f1-80b5-00155d3585b8",
                        "BPM-fbede791-8138-11f1-80b5-00155d3585b8-APPROVE", "APPROVED", null, null));

        assertEquals("ROUTE_VERSION_APPROVAL_ACTOR_REQUIRED", ex.getMessage());
        verify(lifecycleService, never()).publishCandidate(pending.getId(), null);
    }

    @Test
    void callback_shouldRejectUnknownApprovalInstance() {
        when(routeVersionMapper.selectByApprovalProcessInstanceId("unknown-process")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> approvalService.handleApprovalCallback("unknown-process", "BPM-unknown-process-APPROVE",
                        "APPROVED", null, 501L));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_APPROVAL_NOT_EXISTS.getCode(), ex.getCode());
    }

    private MesProRouteVersionDO pendingCandidate() {
        return MesProRouteVersionDO.builder()
                .id(77001L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL)
                .sourceRouteVersionId(77000L)
                .approvalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8")
                .routeSnapshotJson("{\"routeId\":9001,\"configSnapshots\":{\"flowGraph\":{},\"products\":[],\"scheduleConfigs\":[],\"batchUseConfigs\":[]}}")
                .build();
    }
}
