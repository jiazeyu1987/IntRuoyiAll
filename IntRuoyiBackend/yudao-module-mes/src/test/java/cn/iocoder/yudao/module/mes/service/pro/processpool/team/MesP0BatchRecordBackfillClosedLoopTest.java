package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditChange;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditValueType;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesP0BatchRecordBackfillClosedLoopTest {

    @Mock
    private MesProRouteFlowProcessBatchRecordMapper bindingMapper;
    @Mock
    private MesProBatchRecordExecutionService executionService;
    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProBatchRecordCellLinkRuleMapper ruleMapper;
    @Mock
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;

    private MesTeamLeaderBatchRecordBackfillService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderBatchRecordBackfillServiceImpl(bindingMapper, executionService, executionMapper,
                ruleMapper, fieldAuditService);
    }

    @Test
    void shouldWriteAuditableBackfillCommandWithSourceValuesOldValueHashCellLocationAndIdempotencyKey() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(formalBinding()));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(executionWithExistingPressure());
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(reportRule(2L, "pressure", 6, 2)));
        when(fieldAuditService.saveSystemCellLinkChanges(any(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class)))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult()
                        .setAuditBatchId(9901L)
                        .setFieldAuditRevision(2L)
                        .setFieldAuditHeadHash("after-head")
                        .setCellValuesHash("after-hash")
                        .setChangedFieldCount(1));

        MesTeamLeaderBatchRecordBackfillResult result = service.backfillCompletedProcess(backfillCommand());

        assertEquals(8801L, result.getExecutionId());
        assertEquals(1, result.getAppliedFieldCount());
        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService).saveSystemCellLinkChanges(auditCaptor.capture());
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command = auditCaptor.getValue();
        assertEquals("PROCESS_POOL_REPORT_BACKFILL:1001:9001:5001", command.getIdempotencyKey());
        assertEquals("before-hash", command.getBaseCellValuesHash());
        assertEquals(1L, command.getBaseFieldAuditRevision());
        assertEquals("before-head", command.getBaseFieldAuditHeadHash());
        assertEquals("生产组长确认报工后自动回填正式批记录", command.getReasonText());

        MesProBatchRecordExecutionFieldAuditChange change = command.getChanges().get(0);
        assertEquals("report.pressure", change.getFieldPath());
        assertEquals("pressure", change.getFieldKey());
        assertEquals(6, change.getRowIndex());
        assertEquals(2, change.getColumnIndex());
        assertEquals(MesProBatchRecordExecutionFieldAuditValueType.NUMBER, change.getValueType());
        assertEquals(new BigDecimal("15"), change.getNewValueJson());
        assertEquals("15", change.getNewValueDisplay());
        assertNotNull(change.getExpectedOldValueHash());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("10")),
                change.getExpectedOldValueHash());
    }

    private static MesTeamLeaderBatchRecordBackfillCommand backfillCommand() {
        return new MesTeamLeaderBatchRecordBackfillCommand()
                .setEvent(event())
                .setAllocation(allocation())
                .setWorkOrder(workOrder());
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .routeId(7001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .rawPayload("{\"outputQuantity\":80,\"pressure\":15}")
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 10, 0))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation() {
        return MesProcessPoolReportAllocationDO.builder()
                .id(7101L)
                .eventId(1001L)
                .reviewId(7001L)
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal("80"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 3, 10, 1))
                .build();
    }

    private static MesProWorkOrderDO workOrder() {
        return MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .batchCode("BATCH-9001")
                .quantity(new BigDecimal("200"))
                .build();
    }

    private static MesProRouteFlowProcessBatchRecordDO formalBinding() {
        return MesProRouteFlowProcessBatchRecordDO.builder()
                .id(3001L)
                .routeId(7001L)
                .routeProcessId(5001L)
                .useType("BATCH")
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .recordCategory("BATCH_RECORD")
                .formSlotType("MAIN")
                .permissionScopeId(9901L)
                .build();
    }

    private static MesProBatchRecordExecutionDO executionWithExistingPressure() {
        return MesProBatchRecordExecutionDO.builder()
                .id(8801L)
                .status(0)
                .fieldAuditRevision(1L)
                .fieldAuditHeadHash("before-head")
                .cellValuesHash("before-hash")
                .executionSnapshotJson("""
                        {"fields":[
                          {"fieldPath":"report.pressure","fieldKey":"pressure","rowIndex":6,"columnIndex":2,"valueType":"NUMBER","defaultValue":0}
                        ]}
                        """)
                .cellValuesJson("""
                        [
                          {"rowIndex":6,"columnIndex":2,"value":10,"valueDisplay":"10"}
                        ]
                        """)
                .build();
    }

    private static MesProBatchRecordCellLinkRuleDO reportRule(Long id, String sourceFieldCode, Integer rowIndex,
                                                              Integer columnIndex) {
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO();
        rule.setId(id);
        rule.setRuleVersion(1L);
        rule.setScopeType("ROUTE_VERSION");
        rule.setScopeId(401L);
        rule.setSourceType("PROCESS_POOL_REPORT");
        rule.setSourceFieldCode(sourceFieldCode);
        rule.setTargetReportId("BR-FORM-A");
        rule.setTargetRowIndex(rowIndex);
        rule.setTargetColumnIndex(columnIndex);
        rule.setTargetCellKey("R" + rowIndex + "C" + columnIndex);
        rule.setTargetValueType(MesProBatchRecordExecutionFieldAuditValueType.NUMBER.name());
        rule.setEnabled(true);
        return rule;
    }
}
