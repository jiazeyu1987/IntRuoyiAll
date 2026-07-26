package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordCellLinkServiceImplTest {

    @Mock
    private MesProBatchRecordCellLinkRuleMapper ruleMapper;
    @Mock
    private MesProBatchRecordReportMapper reportMapper;
    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProBatchRecordReportService reportService;
    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;

    @InjectMocks
    private MesProBatchRecordCellLinkServiceImpl service;

    @Test
    void getPrefill_resolvesProductionWorkOrderFieldWithoutSourceExecution() {
        MesProBatchRecordExecutionDO targetExecution = new MesProBatchRecordExecutionDO()
                .setId(9001L)
                .setWorkOrderId(1001L)
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L)
                .setBatchRecordReportId("target-report")
                .setBatchCode("BATCH-001")
                .setCellValuesJson("[]");
        MesProBatchRecordReportDO targetReport = new MesProBatchRecordReportDO()
                .setReportId("target-report")
                .setReportName("批次执行记录")
                .setBatchRecordDefinitionId(2001L)
                .setBatchRecordVersionId(3001L);
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO()
                .setId(11L)
                .setScopeType("ROUTE_VERSION")
                .setScopeId(3001L)
                .setSourceType("PRODUCTION_WORK_ORDER")
                .setSourceReportId("PRODUCTION_WORK_ORDER")
                .setSourceReportName("生产工单")
                .setSourceFieldCode("quantity")
                .setSourceFieldName("生产数量")
                .setSourceCellKey("quantity")
                .setSourceLabel("生产数量")
                .setSourceValueType("NUMBER")
                .setTargetReportId("target-report")
                .setTargetReportName("批次执行记录")
                .setTargetRowIndex(1)
                .setTargetColumnIndex(2)
                .setTargetCellKey("1:2")
                .setOverwritePolicy("ONLY_WHEN_EMPTY")
                .setRuleVersion(7L);
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(1001L)
                .code("MO-001")
                .quantity(new BigDecimal("128.5"))
                .build();

        when(executionMapper.selectById(9001L)).thenReturn(targetExecution);
        when(reportMapper.selectByReportId("target-report")).thenReturn(targetReport);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 3001L, "target-report"))
                .thenReturn(List.of(rule));
        when(workOrderMapper.selectById(1001L)).thenReturn(workOrder);

        BatchRecordCellLinkPrefillRespVO result = service.getPrefill(9001L, null);

        assertEquals(1, result.getPrefills().size());
        assertEquals(new BigDecimal("128.5"), result.getPrefills().get(0).getValue());
        assertEquals("PRODUCTION_WORK_ORDER", result.getPrefills().get(0).getSourceType());
        assertEquals("quantity", result.getPrefills().get(0).getSourceFieldCode());
        assertEquals("生产数量", result.getPrefills().get(0).getSourceFieldName());
        assertEquals(0, result.getConflicts().size());
    }
}
