package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesOrderReleaseCompletenessServiceTest {

    @InjectMocks
    private MesOrderReleaseCompletenessServiceImpl service;

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock
    private MesProcessPoolWorkOrderAbnormalMapper workOrderAbnormalMapper;
    @Mock
    private MesProcessPoolActiveOrderTransferTraceMapper transferTraceMapper;
    @Mock
    private MesWmMaterialStockMapper materialStockMapper;

    @Test
    void evaluateInspectionResultSummarizesLargePendingPqcTaskSetWithinReleaseCheckColumnBudget() {
        MesProEdhrBatchExecutionDO batch = MesProEdhrBatchExecutionDO.builder()
                .workOrderId(980008L)
                .workOrderCode("RRM-20260801-PP-MO-001")
                .routeId(922119L)
                .build();
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .id(12L)
                .workOrderId(batch.getWorkOrderId())
                .routeId(batch.getRouteId())
                .activeStatus("ACTIVE")
                .build();
        List<MesPqcInspectionTaskDO> pendingTasks = LongStream.rangeClosed(1, 120)
                .mapToObj(id -> MesPqcInspectionTaskDO.builder()
                        .id(id)
                        .activeOrderId(activeOrder.getId())
                        .taskStatus("PENDING")
                        .build())
                .toList();
        when(activeOrderMapper.selectActiveByWorkOrderAndRoute(batch.getWorkOrderId(), batch.getRouteId()))
                .thenReturn(activeOrder);
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(activeOrder.getId()))
                .thenReturn(pendingTasks);

        MesOrderReleaseCompletenessCheck result = service.evaluateInspectionResult(batch);

        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_BLOCKER, result.checkResult());
        assertTrue(result.failureReason().contains("共 120 个"));
        assertTrue(result.failureReason().contains("示例"));
        assertTrue(result.failureReason().length() <= 500);
    }
}
