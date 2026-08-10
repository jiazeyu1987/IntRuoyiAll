package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderManualSortTest {

    private static final Long LEADER_USER_ID = 3001L;

    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProWorkOrderService workOrderService;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesMdItemMapper itemMapper;
    @Mock private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    @Mock private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock private MesProRouteProductMapper routeProductMapper;
    @Mock private MesProRouteMapper routeMapper;
    @Mock private MesProRouteVersionMapper routeVersionMapper;
    @Mock private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock private MesProcessPoolReportAllocationMapper reportAllocationMapper;
    @Mock private MesQaInspectionRegulationMapper inspectionRegulationMapper;
    @Mock private MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper;
    @Mock private MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    @Mock private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock private MesWorkOrderAbnormalStateService abnormalStateService;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock private MesReportAllocationOrderChangeService reportAllocationOrderChangeService;

    private MesTeamLeaderActiveOrderService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderServiceImpl(activeOrderMapper, workOrderService, workOrderMapper,
                itemMapper, auditMapper, scheduleOrderMapper, scheduleOrderProcessMapper, routeProductMapper, routeMapper,
                routeVersionMapper, processSnapshotMapper, reportAllocationMapper,
                inspectionRegulationMapper,
                inspectionRegulationVersionMapper, inspectionRegulationItemMapper, pqcInspectionTaskMapper,
                abnormalStateService, releaseApplicationMapper, dccProjectCodeMapper,
                reportAllocationOrderChangeService);
    }

    @Test
    void moveUpSwapsOnlyWithPreviousActiveOrder() {
        MesProcessPoolActiveOrderDO first = activeOrder(8101L, 1L);
        MesProcessPoolActiveOrderDO target = activeOrder(8102L, 2L);
        MesProcessPoolActiveOrderDO last = activeOrder(8103L, 3L);
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(LEADER_USER_ID))
                .thenReturn(List.of(first, target, last));
        when(activeOrderMapper.swapActiveOrderSortOrders(
                LEADER_USER_ID, target.getId(), target.getSortOrder(), first.getId(), first.getSortOrder()))
                .thenReturn(2);

        service.moveActiveOrder(moveRequest(target.getId(), "UP"));

        verify(activeOrderMapper).swapActiveOrderSortOrders(
                LEADER_USER_ID, target.getId(), 2L, first.getId(), 1L);
    }

    @Test
    void moveDownSwapsOnlyWithNextActiveOrder() {
        MesProcessPoolActiveOrderDO first = activeOrder(8101L, 1L);
        MesProcessPoolActiveOrderDO target = activeOrder(8102L, 2L);
        MesProcessPoolActiveOrderDO last = activeOrder(8103L, 3L);
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(LEADER_USER_ID))
                .thenReturn(List.of(first, target, last));
        when(activeOrderMapper.swapActiveOrderSortOrders(
                LEADER_USER_ID, target.getId(), target.getSortOrder(), last.getId(), last.getSortOrder()))
                .thenReturn(2);

        service.moveActiveOrder(moveRequest(target.getId(), "DOWN"));

        verify(activeOrderMapper).swapActiveOrderSortOrders(
                LEADER_USER_ID, target.getId(), 2L, last.getId(), 3L);
    }

    @Test
    void moveAtBoundaryFailsWithoutChangingOrder() {
        MesProcessPoolActiveOrderDO first = activeOrder(8101L, 1L);
        MesProcessPoolActiveOrderDO last = activeOrder(8102L, 2L);
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(LEADER_USER_ID))
                .thenReturn(List.of(first, last));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.moveActiveOrder(moveRequest(first.getId(), "UP")));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_MOVE_INVALID.getCode(), exception.getCode());
        verify(activeOrderMapper, never()).swapActiveOrderSortOrders(
                LEADER_USER_ID, first.getId(), first.getSortOrder(), last.getId(), last.getSortOrder());
    }

    @Test
    void moveRejectsOrderOutsideCurrentLeaderScope() {
        MesProcessPoolActiveOrderDO owned = activeOrder(8101L, 1L);
        when(activeOrderMapper.selectActiveListByLeaderForUpdate(LEADER_USER_ID)).thenReturn(List.of(owned));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.moveActiveOrder(moveRequest(9999L, "DOWN")));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), exception.getCode());
        verify(activeOrderMapper, never()).swapActiveOrderSortOrders(
                LEADER_USER_ID, owned.getId(), owned.getSortOrder(), owned.getId(), owned.getSortOrder());
    }

    private static MesTeamLeaderActiveOrderMoveReqBO moveRequest(Long activeOrderId, String direction) {
        return MesTeamLeaderActiveOrderMoveReqBO.builder()
                .leaderUserId(LEADER_USER_ID)
                .activeOrderId(activeOrderId)
                .direction(direction)
                .build();
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long id, Long sortOrder) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .leaderUserId(LEADER_USER_ID)
                .activeStatus(MesTeamLeaderActiveOrderServiceImpl.STATUS_ACTIVE)
                .sortOrder(sortOrder)
                .version(0)
                .build();
    }
}
