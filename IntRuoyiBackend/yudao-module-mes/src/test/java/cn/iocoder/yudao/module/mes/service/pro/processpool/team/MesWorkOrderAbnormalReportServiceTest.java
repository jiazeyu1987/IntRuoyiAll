package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MesWorkOrderAbnormalReportServiceTest {

    @Mock
    private MesProcessPoolWorkOrderAbnormalMapper abnormalMapper;

    private MesWorkOrderAbnormalReportService service;

    @BeforeEach
    void setUp() {
        service = new MesWorkOrderAbnormalReportServiceImpl(abnormalMapper);
    }

    @Test
    void shouldMarkAndReportWorkOrderAbnormalWithReasonCode() {
        whenInsertId(8101L);

        Long abnormalId = service.markAndReport(MesWorkOrderAbnormalReportReqBO.builder()
                .workOrderId(5001L)
                .markerUserId(3001L)
                .abnormalReasonCode("DEVICE_DOWN")
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
        assertEquals("DEVICE_DOWN", abnormal.getAbnormalReasonCode());
        assertEquals("设备停机，影响工单交付", abnormal.getAbnormalDescription());
        assertEquals(MesProcessPoolWorkOrderAbnormalDO.REPORT_STATUS_REPORTED, abnormal.getReportStatus());
        assertEquals(3001L, abnormal.getMarkerUserId());
        assertEquals(3001L, abnormal.getReporterUserId());
        assertNotNull(abnormal.getMarkedAt());
        assertNotNull(abnormal.getReportedAt());
    }

    private void whenInsertId(Long id) {
        org.mockito.Mockito.when(abnormalMapper.insert(any(MesProcessPoolWorkOrderAbnormalDO.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(0, MesProcessPoolWorkOrderAbnormalDO.class).setId(id);
                    return 1;
                });
    }
}
