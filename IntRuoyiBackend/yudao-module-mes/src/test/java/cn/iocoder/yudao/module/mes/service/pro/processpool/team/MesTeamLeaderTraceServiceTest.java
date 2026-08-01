package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderAllocationTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderBatchRecordTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderOrderProcessTraceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderTraceServiceTest {

    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper;

    private MesTeamLeaderTraceService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderTraceServiceImpl(allocationMapper, completionMapper, executionMapper,
                auditItemMapper);
    }

    @Test
    void allocationTraceReturnsLinesAndTotalForExactEventAndOrderProcess() {
        when(allocationMapper.selectListByTrace(1001L, 9001L, 5001L, 6001L)).thenReturn(List.of(
                allocation(7101L, "50"), allocation(7102L, "30")));

        MesTeamLeaderAllocationTraceRespVO trace =
                service.getAllocationTrace(1001L, 9001L, 5001L, 6001L);

        assertEquals(new BigDecimal("80"), trace.getTotalAllocatedQuantity());
        assertEquals(2, trace.getLines().size());
        assertEquals(7101L, trace.getLines().get(0).getAllocationId());
        assertEquals("FIFO", trace.getLines().get(0).getAllocationMode());
    }

    @Test
    void orderProcessTraceReturnsCompletionAndBackfillStatus() {
        when(completionMapper.selectByWorkOrderAndProcess(9001L, 5001L, 6001L))
                .thenReturn(completion());

        MesTeamLeaderOrderProcessTraceRespVO trace =
                service.getOrderProcessTrace(9001L, 5001L, 6001L);

        assertEquals(new BigDecimal("80"), trace.getConfirmedQuantity());
        assertEquals("COMPLETED", trace.getCompletionStatus());
        assertEquals("SUCCESS", trace.getBackfillStatus());
        assertEquals(8801L, trace.getBackfillExecutionId());
    }

    @Test
    void batchRecordTraceReturnsExecutionAndFieldAuditProjection() {
        when(completionMapper.selectByWorkOrderAndProcess(9001L, 5001L, 6001L))
                .thenReturn(completion());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of(auditItem()));

        MesTeamLeaderBatchRecordTraceRespVO trace =
                service.getBatchRecordTrace(9001L, 5001L, 6001L);

        assertEquals(8801L, trace.getExecutionId());
        assertEquals("BR-FORM-A", trace.getBatchRecordReportId());
        assertEquals(9901L, trace.getFieldAuditLastBatchId());
        assertEquals(1, trace.getCells().size());
        assertEquals("pressure", trace.getCells().get(0).getFieldKey());
        assertEquals("15", trace.getCells().get(0).getValueDisplay());
    }

    @Test
    void batchRecordTraceFailsFastWhenFieldAuditProjectionIsMissing() {
        when(completionMapper.selectByWorkOrderAndProcess(9001L, 5001L, 6001L))
                .thenReturn(completion());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getBatchRecordTrace(9001L, 5001L, 6001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_TRACE_REQUIRED.getCode(), ex.getCode());
    }

    private static MesProcessPoolReportAllocationDO allocation(Long id, String quantity) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(id)
                .eventId(1001L)
                .reviewId(7001L)
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal(quantity))
                .allocationMode("FIFO")
                .confirmedAt(LocalDateTime.of(2026, 8, 1, 9, 1))
                .build();
    }

    private static MesProcessPoolOrderProcessCompletionDO completion() {
        return MesProcessPoolOrderProcessCompletionDO.builder()
                .workOrderId(9001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .targetQuantity(new BigDecimal("80"))
                .confirmedQuantity(new BigDecimal("80"))
                .completionStatus("COMPLETED")
                .backfillStatus("SUCCESS")
                .backfillExecutionId(8801L)
                .lastEventId(1001L)
                .lastReviewId(7001L)
                .build();
    }

    private static MesProBatchRecordExecutionDO execution() {
        return MesProBatchRecordExecutionDO.builder()
                .id(8801L)
                .executionCode("EXE-8801")
                .workOrderId(9001L)
                .workOrderCode("WO-9001")
                .routeProcessId(5001L)
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .fieldAuditRevision(2L)
                .fieldAuditHeadHash("head-hash")
                .fieldAuditLastBatchId(9901L)
                .cellValuesJson("[{\"rowIndex\":6,\"columnIndex\":2,\"value\":15}]")
                .build();
    }

    private static MesProBatchRecordExecutionFieldAuditItemDO auditItem() {
        return MesProBatchRecordExecutionFieldAuditItemDO.builder()
                .id(99011L)
                .auditBatchId(9901L)
                .executionId(8801L)
                .fieldAuditRevision(2L)
                .fieldPath("report.pressure")
                .fieldKey("pressure")
                .rowIndex(6)
                .columnIndex(2)
                .valueType("NUMBER")
                .newValueJson("15")
                .newValueDisplay("15")
                .build();
    }
}
