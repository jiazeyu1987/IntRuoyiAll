package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderWorkbenchServiceImplTest {

    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService;
    @Mock
    private ProcessPoolTimelineService timelineService;
    @Mock
    private MesActiveOrderHandoffService handoffService;

    private MesTeamLeaderWorkbenchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderWorkbenchServiceImpl(scopeService, routeStartAuthorizationService, timelineService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffService", handoffService);
    }

    @Test
    void shouldRestrictPqcLeaderManagementPageToPqcInspectionEvents() {
        when(handoffService.listOwnPqcReviewEventIds(7001L)).thenReturn(Set.of(9001L, 9002L));
        when(timelineService.getTimelinePage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageResult<ProcessPoolTimelineEventRespVO>(java.util.List.of(), 0L));
        MesTeamLeaderSubmissionPageReqVO reqVO = new MesTeamLeaderSubmissionPageReqVO()
                .setLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC);

        service.getSubmissionPage(7001L, MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, reqVO);

        ArgumentCaptor<MesTeamLeaderSubmissionPageReqVO> captor =
                ArgumentCaptor.forClass(MesTeamLeaderSubmissionPageReqVO.class);
        verify(timelineService).getTimelinePage(captor.capture());
        assertEquals(Set.of(9001L, 9002L), captor.getValue().getEventIds());
        assertNull(captor.getValue().getEmployeeUserIds());
        assertEquals(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION, captor.getValue().getEventType());
        org.mockito.Mockito.verifyNoInteractions(scopeService);
    }

    @Test
    void shouldRestrictProductionWorkbenchToProductionReportsAndKeepAllocationView() {
        when(routeStartAuthorizationService.listAuthorizedRouteProcesses(7001L))
                .thenReturn(java.util.List.of(
                        MesProRouteProcessDO.builder().id(9101L).processId(8101L).build(),
                        MesProRouteProcessDO.builder().id(9102L).processId(8102L).build()));
        when(timelineService.getTimelinePage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageResult<ProcessPoolTimelineEventRespVO>(java.util.List.of(), 0L));
        MesTeamLeaderSubmissionPageReqVO reqVO = new MesTeamLeaderSubmissionPageReqVO()
                .setLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION);
        reqVO.setAllocationView("WORKBENCH");

        service.getSubmissionPage(7001L, MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, reqVO);

        ArgumentCaptor<MesTeamLeaderSubmissionPageReqVO> captor =
                ArgumentCaptor.forClass(MesTeamLeaderSubmissionPageReqVO.class);
        verify(timelineService).getTimelinePage(captor.capture());
        assertEquals(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT, captor.getValue().getEventType());
        assertEquals(Boolean.TRUE, captor.getValue().getRequirePositiveOutputQuantity());
        assertEquals("WORKBENCH", captor.getValue().getAllocationView());
        assertEquals(Set.of(8101L, 8102L), captor.getValue().getProcessIds());
        assertNull(captor.getValue().getRouteProcessIds());
        assertEquals(null, captor.getValue().getEmployeeUserIds());
    }

    @Test
    void shouldKeepHistoricalProductionReportVisibleAfterEmployeeBindingIsRemoved() {
        when(routeStartAuthorizationService.listAuthorizedRouteProcesses(7001L))
                .thenReturn(java.util.List.of(MesProRouteProcessDO.builder().id(928611L).processId(922987L).build()));
        when(timelineService.getTimelinePage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageResult<ProcessPoolTimelineEventRespVO>(java.util.List.of(), 0L));
        MesTeamLeaderSubmissionPageReqVO reqVO = new MesTeamLeaderSubmissionPageReqVO()
                .setLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION);
        reqVO.setAllocationView("HISTORY");

        service.getSubmissionPage(7001L, MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, reqVO);

        ArgumentCaptor<MesTeamLeaderSubmissionPageReqVO> captor =
                ArgumentCaptor.forClass(MesTeamLeaderSubmissionPageReqVO.class);
        verify(timelineService).getTimelinePage(captor.capture());
        assertEquals(Set.of(922987L), captor.getValue().getProcessIds());
        assertNull(captor.getValue().getRouteProcessIds());
        assertEquals(Boolean.TRUE, captor.getValue().getRequirePositiveOutputQuantity());
        assertEquals(null, captor.getValue().getEmployeeUserIds());
    }

    @Test
    void shouldKeepOldRouteProcessReportReadableAfterPublishedProcessIdsChange() {
        when(routeStartAuthorizationService.listAuthorizedRouteProcesses(7001L)).thenReturn(java.util.List.of(
                MesProRouteProcessDO.builder().id(9902L).processId(8101L).build()));
        ProcessPoolTimelineDetailRespVO oldReport = new ProcessPoolTimelineDetailRespVO();
        oldReport.setId(282240L).setRouteProcessId(9101L).setProcessId(8101L);
        when(timelineService.getTimelineDetail(282240L)).thenReturn(oldReport);

        assertSame(oldReport, service.getSubmissionDetail(7001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, 282240L));
        assertEquals(9101L, oldReport.getRouteProcessId());
        verify(scopeService, never()).assertCanAccessEmployee(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectDifferentProcessEvenWhenRouteProcessIdMatchesCurrentAuthorization() {
        when(routeStartAuthorizationService.listAuthorizedRouteProcesses(7001L)).thenReturn(java.util.List.of(
                MesProRouteProcessDO.builder().id(9101L).processId(8101L).build()));
        ProcessPoolTimelineDetailRespVO outsideReport = new ProcessPoolTimelineDetailRespVO();
        outsideReport.setId(282241L).setRouteProcessId(9101L).setProcessId(8102L);
        when(timelineService.getTimelineDetail(282241L)).thenReturn(outsideReport);

        assertThrows(ServiceException.class, () -> service.getSubmissionDetail(7001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, 282241L));
    }

    @Test
    void shouldNotReadUnrestrictedTimelineWhenStartScopeIsEmpty() {
        when(routeStartAuthorizationService.listAuthorizedRouteProcesses(7001L)).thenReturn(java.util.List.of());
        MesTeamLeaderSubmissionPageReqVO reqVO = new MesTeamLeaderSubmissionPageReqVO()
                .setLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION);

        assertEquals(0L, service.getSubmissionPage(7001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, reqVO).getTotal());
        verify(timelineService, never()).getTimelinePage(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReplaceCallerSuppliedScopesWithAuthorizedSharedProcessScope() {
        when(routeStartAuthorizationService.listAuthorizedRouteProcesses(7001L)).thenReturn(java.util.List.of(
                MesProRouteProcessDO.builder().id(9902L).processId(8101L).build()));
        when(timelineService.getTimelinePage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PageResult.empty());
        MesTeamLeaderSubmissionPageReqVO reqVO = new MesTeamLeaderSubmissionPageReqVO()
                .setLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION);
        reqVO.setProcessIds(Set.of(9999L));
        reqVO.setRouteProcessIds(Set.of(9101L));
        reqVO.setEventIds(Set.of(9999L));

        service.getSubmissionPage(7001L, MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, reqVO);

        assertEquals(Set.of(8101L), reqVO.getProcessIds());
        assertNull(reqVO.getRouteProcessIds());
        assertNull(reqVO.getEventIds());
    }

    @Test
    void shouldReadPqcDetailFromFrozenResponsibilityWithoutCurrentEmployeeScope() {
        ProcessPoolTimelineDetailRespVO detail = new ProcessPoolTimelineDetailRespVO();
        detail.setId(282242L).setActualEmployeeUserId(8001L);
        when(timelineService.getTimelineDetail(282242L)).thenReturn(detail);
        when(handoffService.listOwnPqcReviewEventIds(7001L)).thenReturn(Set.of(282242L));

        assertSame(detail, service.getSubmissionDetail(7001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, 282242L));
        org.mockito.Mockito.verifyNoInteractions(scopeService);
        verify(routeStartAuthorizationService, never()).listAuthorizedRouteProcesses(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldOverrideClientEventScopeAndRetainOnlyOptionalEmployeeFilter() {
        when(handoffService.listOwnPqcReviewEventIds(7001L)).thenReturn(Set.of(282242L));
        when(timelineService.getTimelinePage(org.mockito.ArgumentMatchers.any())).thenReturn(PageResult.empty());
        var req = new MesTeamLeaderSubmissionPageReqVO().setLeaderType("PQC");
        req.setEventIds(Set.of(999L)); req.setEmployeeUserIds(Set.of(888L)); req.setEmployeeUserId(8001L);
        service.getSubmissionPage(7001L, "PQC", req);
        assertEquals(Set.of(282242L), req.getEventIds());
        assertNull(req.getEmployeeUserIds()); assertEquals(8001L, req.getEmployeeUserId());
        org.mockito.Mockito.verifyNoInteractions(scopeService);
    }

    @Test
    void shouldNotQueryTimelineOrDetailWithoutFrozenPqcResponsibility() {
        when(handoffService.listOwnPqcReviewEventIds(7001L)).thenReturn(Set.of());
        assertEquals(0L, service.getSubmissionPage(7001L, "PQC", new MesTeamLeaderSubmissionPageReqVO()).getTotal());
        assertThrows(ServiceException.class, () -> service.getSubmissionDetail(7001L, "PQC", 282242L));
        org.mockito.Mockito.verifyNoInteractions(timelineService, scopeService);
    }
}
