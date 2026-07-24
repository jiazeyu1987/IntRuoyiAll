package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrFormFillLogServicePerformanceTest {

    @Mock
    private MesProBatchRecordExecutionFieldAuditBatchMapper auditBatchMapper;
    @Mock
    private MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper;
    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper;

    @InjectMocks
    private MesProEdhrFormFillLogServiceImpl formFillLogService;

    @Test
    void getPage_usesBatchContextLookupsAndBoundedSummaryItems() {
        MesProEdhrFormFillLogPageReqVO reqVO = new MesProEdhrFormFillLogPageReqVO();
        MesProBatchRecordExecutionFieldAuditBatchDO firstBatch = auditBatch(1001L, 2001L);
        MesProBatchRecordExecutionFieldAuditBatchDO secondBatch = auditBatch(1002L, 2002L);
        when(auditBatchMapper.selectPage(eq(reqVO), any()))
                .thenReturn(new PageResult<>(List.of(firstBatch, secondBatch), 2L));
        when(executionMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                execution(2001L, "EXEC-001", "BATCH-001", "WO-001"),
                execution(2002L, "EXEC-002", "BATCH-002", "WO-002")
        ));
        when(batchExecutionTaskMapper.selectListByExecutionIds(anyCollection())).thenReturn(List.of(
                task(3001L, 2001L, 9001L, "RPT-001", "生产记录一"),
                task(3002L, 2002L, 9002L, "RPT-002", "生产记录二")
        ));
        when(auditItemMapper.selectSummaryListByBatchIds(anyCollection(), eq(3))).thenReturn(List.of(
                item(4001L, 1001L, "温度", "37.5"),
                item(4002L, 1002L, "压力", "1.2")
        ));

        PageResult<MesProEdhrFormFillLogPageRespVO> page = formFillLogService.getPage(reqVO);

        assertEquals(2L, page.getTotal());
        assertEquals("BATCH-001", page.getList().get(0).getBatchCode());
        assertEquals("WO-002", page.getList().get(1).getWorkOrderCode());
        verify(executionMapper, never()).selectById(any());
        verify(batchExecutionTaskMapper, never()).selectByExecutionId(any());
        verify(auditItemMapper, never()).selectListByBatchId(any());
        verify(executionMapper).selectListByIds(
                argThat((Collection<Long> ids) -> ids.containsAll(List.of(2001L, 2002L)) && ids.size() == 2));
        verify(batchExecutionTaskMapper).selectListByExecutionIds(
                argThat((Collection<Long> ids) -> ids.containsAll(List.of(2001L, 2002L)) && ids.size() == 2));
        verify(auditItemMapper).selectSummaryListByBatchIds(
                argThat((Collection<Long> ids) -> ids.containsAll(List.of(1001L, 1002L)) && ids.size() == 2),
                eq(3));
    }

    private static MesProBatchRecordExecutionFieldAuditBatchDO auditBatch(Long id, Long executionId) {
        return new MesProBatchRecordExecutionFieldAuditBatchDO()
                .setId(id)
                .setExecutionId(executionId)
                .setActorId(101L)
                .setActorName("测试填写人")
                .setChangedAt(LocalDateTime.of(2026, 7, 13, 9, 30))
                .setFieldCount(1)
                .setHashVerificationJson("{\"status\":\"VALID\"}");
    }

    private static MesProBatchRecordExecutionDO execution(Long id, String executionCode,
                                                          String batchCode, String workOrderCode) {
        return new MesProBatchRecordExecutionDO()
                .setId(id)
                .setExecutionCode(executionCode)
                .setBatchRecordReportId("RPT-" + id)
                .setTemplateName("表单-" + id)
                .setBatchCode(batchCode)
                .setWorkOrderCode(workOrderCode);
    }

    private static MesProEdhrBatchExecutionTaskDO task(Long id, Long executionId, Long batchExecutionId,
                                                       String reportId, String reportName) {
        return new MesProEdhrBatchExecutionTaskDO()
                .setId(id)
                .setExecutionId(executionId)
                .setBatchExecutionId(batchExecutionId)
                .setBatchRecordReportId(reportId)
                .setBatchRecordReportName(reportName);
    }

    private static MesProBatchRecordExecutionFieldAuditItemDO item(Long id, Long auditBatchId,
                                                                   String label, String value) {
        return new MesProBatchRecordExecutionFieldAuditItemDO()
                .setId(id)
                .setAuditBatchId(auditBatchId)
                .setBatchItemIndex(1)
                .setFieldKey(label)
                .setFieldLabel(label)
                .setNewValueDisplay(value);
    }
}
