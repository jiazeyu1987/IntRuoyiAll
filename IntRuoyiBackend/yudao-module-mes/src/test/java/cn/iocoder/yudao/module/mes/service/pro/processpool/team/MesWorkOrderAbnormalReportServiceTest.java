package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesWorkOrderAbnormalReportServiceTest {

    @Mock
    private MesProcessPoolWorkOrderAbnormalMapper abnormalMapper;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesWorkOrderAbnormalStateService abnormalStateService;

    private MesWorkOrderAbnormalReportService service;

    @BeforeEach
    void setUp() {
        service = new MesWorkOrderAbnormalReportServiceImpl(abnormalMapper, activeOrderMapper, abnormalStateService);
    }

    @Test
    void shouldMarkAndReportCurrentLeaderActiveOrderWithDescriptionOnly() {
        whenInsertId(8101L);
        when(activeOrderMapper.selectActiveByLeaderAndWorkOrderForUpdate(3001L, 5001L)).thenReturn(
                MesProcessPoolActiveOrderDO.builder().id(7001L).leaderUserId(3001L).workOrderId(5001L).build());

        Long abnormalId = service.markAndReport(MesWorkOrderAbnormalReportReqBO.builder()
                .workOrderId(5001L)
                .markerUserId(3001L)
                .abnormalDescription("设备停机，影响工单交付")
                .build());

        assertEquals(8101L, abnormalId);
        ArgumentCaptor<MesProcessPoolWorkOrderAbnormalDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolWorkOrderAbnormalDO.class);
        verify(abnormalMapper).insert(captor.capture());
        MesProcessPoolWorkOrderAbnormalDO abnormal = captor.getValue();
        assertEquals(5001L, abnormal.getWorkOrderId());
        assertNull(abnormal.getRouteProcessId());
        assertNull(abnormal.getProcessId());
        assertNull(abnormal.getSourceEventId());
        assertEquals("ACTIVE_ORDER_ABNORMAL", abnormal.getAbnormalReasonCode());
        assertEquals("设备停机，影响工单交付", abnormal.getAbnormalDescription());
        assertEquals(MesProcessPoolWorkOrderAbnormalDO.REPORT_STATUS_REPORTED, abnormal.getReportStatus());
        assertEquals(3001L, abnormal.getMarkerUserId());
        assertEquals(3001L, abnormal.getReporterUserId());
        assertNotNull(abnormal.getMarkedAt());
        assertNotNull(abnormal.getReportedAt());
    }

    @Test
    void shouldRejectDuplicateOpenAbnormalForActiveOrder() {
        when(activeOrderMapper.selectActiveByLeaderAndWorkOrderForUpdate(3001L, 5001L)).thenReturn(
                MesProcessPoolActiveOrderDO.builder().id(7001L).leaderUserId(3001L).workOrderId(5001L).build());
        when(abnormalStateService.hasOpenAbnormal(5001L)).thenReturn(true);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.markAndReport(
                MesWorkOrderAbnormalReportReqBO.builder()
                        .workOrderId(5001L)
                        .markerUserId(3001L)
                        .abnormalDescription("设备停机")
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_WORK_ORDER_ABNORMAL_OPEN_EXISTS.getCode(), ex.getCode());
    }

    private void whenInsertId(Long id) {
        org.mockito.Mockito.when(abnormalMapper.insert(any(MesProcessPoolWorkOrderAbnormalDO.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(0, MesProcessPoolWorkOrderAbnormalDO.class).setId(id);
                    return 1;
                });
    }
}
