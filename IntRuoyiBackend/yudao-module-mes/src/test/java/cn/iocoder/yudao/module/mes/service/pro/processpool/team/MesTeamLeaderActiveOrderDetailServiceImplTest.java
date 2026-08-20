package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderDetailReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderDetailServiceImplTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderDetailReadMapper detailReadMapper;
    @InjectMocks
    private MesTeamLeaderActiveOrderDetailServiceImpl service;

    @Test
    void shouldGroupMultipleEmployeesAndSubmissionsByFormalProcessSnapshot() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "30", "张三", "生产组长甲",
                        "2026-08-13T08:10:00"),
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7002L, "40", "李四", null,
                        "2026-08-13T09:20:00"),
                row(9102L, 5002L, 6002L, "精洗", "100.000000", null, null, null, null, null)));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        assertEquals(8101L, detail.getActiveOrderId());
        assertEquals("881MO090889", detail.getWorkOrderCode());
        assertEquals("球囊扩张压力泵工艺路线", detail.getRouteName());
        assertEquals(2, detail.getProcesses().size());
        MesTeamLeaderActiveOrderDetail.ProcessDetail roughWash = detail.getProcesses().get(0);
        assertEquals(new BigDecimal("100.000000"), roughWash.getRequiredQuantity());
        assertEquals(new BigDecimal("70"), roughWash.getSubmittedQuantity());
        assertEquals(2, roughWash.getSubmissionCount());
        assertEquals("张三", roughWash.getSubmissions().get(0).getSubmitterName());
        assertEquals("生产组长甲", roughWash.getSubmissions().get(0).getReviewerName());
        assertEquals("李四", roughWash.getSubmissions().get(1).getSubmitterName());
        assertNull(roughWash.getSubmissions().get(1).getReviewerName());
        assertEquals(0, detail.getProcesses().get(1).getSubmissionCount());
        assertEquals(List.of(), detail.getProcesses().get(1).getSubmissions());
    }

    @Test
    void shouldMarkAllSubmissionsOfOverrunProcessAsQuantityConflict() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "1500.000000", 7001L, "1000", "张三", "生产组长甲",
                        "2026-08-19T16:52:08"),
                row(9101L, 5001L, 6001L, "粗洗", "1500.000000", 7002L, "2000", "李四", "生产组长甲",
                        "2026-08-19T16:53:03")));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(Boolean.TRUE, invokeBoolean(process, "getQuantityConflict"));
        assertEquals(0, new BigDecimal("1500").compareTo(invokeBigDecimal(process, "getOverageQuantity")));
        for (MesTeamLeaderActiveOrderDetail.SubmissionDetail submission : process.getSubmissions()) {
            assertEquals(Boolean.TRUE, invokeBoolean(submission, "getQuantityConflict"));
        }
    }

    @Test
    void shouldRejectActiveOrderOutsideCurrentLeaderOrRemovedOrder() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3002L)
                .activeStatus("ACTIVE")
                .build());

        ServiceException error = assertThrows(ServiceException.class, () -> service.getDetail(3001L, 8101L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), error.getCode());
    }

    @Test
    void shouldFailWhenFormalProcessSnapshotsAreMissing() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class, () -> service.getDetail(3001L, 8101L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(), error.getCode());
    }

    private static MesTeamLeaderActiveOrderDetailReadDO row(Long snapshotId,
                                                              Long routeProcessId,
                                                              Long processId,
                                                             String processName,
                                                             String requiredQuantity,
                                                             Long eventId,
                                                             String submittedQuantity,
                                                             String submitterName,
                                                             String reviewerName,
                                                             String submittedAt) {
        return new MesTeamLeaderActiveOrderDetailReadDO()
                .setSnapshotId(snapshotId)
                .setActiveOrderId(8101L)
                .setWorkOrderId(9001L)
                .setWorkOrderCode("881MO090889")
                .setRouteName("球囊扩张压力泵工艺路线")
                .setRouteProcessId(routeProcessId)
                .setProcessId(processId)
                .setProcessCode("P-" + processId)
                .setProcessName(processName)
                .setRequiredQuantity(new BigDecimal(requiredQuantity))
                .setEventId(eventId)
                .setSubmittedQuantity(submittedQuantity == null ? null : new BigDecimal(submittedQuantity))
                .setSubmitterName(submitterName)
                .setReviewerName(reviewerName)
                .setSubmittedAt(submittedAt == null ? null : LocalDateTime.parse(submittedAt));
    }

    private static Boolean invokeBoolean(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (Boolean) method.invoke(target);
    }

    private static BigDecimal invokeBigDecimal(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (BigDecimal) method.invoke(target);
    }
}
