package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
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
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditChange;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderBatchRecordBackfillServiceTest {

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
    void shouldBackfillReportPayloadFieldsThroughFormalBatchRecordBindingAndCellRules() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(
                        rule(1L, "outputQuantity", 5, 2, MesProBatchRecordExecutionFieldAuditValueType.NUMBER),
                        rule(2L, "pressure", 6, 2, MesProBatchRecordExecutionFieldAuditValueType.NUMBER)));
        when(fieldAuditService.saveSystemCellLinkChanges(any(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class)))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult()
                        .setFieldAuditRevision(2L)
                        .setCellValuesHash("after-hash")
                        .setFieldAuditHeadHash("after-head")
                        .setChangedFieldCount(2));

        MesTeamLeaderBatchRecordBackfillResult result = service.backfillCompletedProcess(command());

        assertEquals(8801L, result.getExecutionId());
        assertEquals(2, result.getAppliedFieldCount());

        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> openCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(executionService).openOrCreateByContext(openCaptor.capture());
        MesProBatchRecordExecutionOpenOrCreateByContextReqVO openReq = openCaptor.getValue();
        assertEquals(9001L, openReq.getWorkOrderId());
        assertEquals(7001L, openReq.getRouteId());
        assertEquals(5001L, openReq.getRouteProcessId());
        assertEquals(6001L, openReq.getProcessId());
        assertEquals("BR-FORM-A", openReq.getBatchRecordReportId());
        assertEquals(3001L, openReq.getRouteBindingId());
        assertEquals("BATCH_RECORD", openReq.getRecordCategory());
        assertEquals("BATCH-9001", openReq.getBatchCode());

        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService).saveSystemCellLinkChanges(auditCaptor.capture());
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand saveCommand = auditCaptor.getValue();
        assertEquals(8801L, saveCommand.getExecutionId());
        assertEquals("PROCESS_POOL_REPORT_BACKFILL:1001:9001:5001", saveCommand.getIdempotencyKey());
        List<MesProBatchRecordExecutionFieldAuditChange> changes = saveCommand.getChanges();
        assertEquals(2, changes.size());
        assertEquals("report.outputQuantity", changes.get(0).getFieldPath());
        assertEquals("outputQuantity", changes.get(0).getFieldKey());
        assertEquals(new BigDecimal("80"), changes.get(0).getNewValueJson());
        assertEquals("report.pressure", changes.get(1).getFieldPath());
        assertEquals("pressure", changes.get(1).getFieldKey());
        assertEquals(new BigDecimal("15"), changes.get(1).getNewValueJson());
    }

    @Test
    void shouldBackfillCompletedProcessOnlyOnceWhenConcurrentAuditAlreadyApplied() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(
                        rule(1L, "outputQuantity", 5, 2, MesProBatchRecordExecutionFieldAuditValueType.NUMBER),
                        rule(2L, "pressure", 6, 2, MesProBatchRecordExecutionFieldAuditValueType.NUMBER)));
        when(fieldAuditService.saveSystemCellLinkChanges(any(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class)))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult()
                                .setAuditBatchId(99001L)
                                .setFieldAuditRevision(2L)
                                .setCellValuesHash("after-hash")
                                .setFieldAuditHeadHash("after-head")
                                .setChangedFieldCount(2),
                        new MesProBatchRecordExecutionFieldAuditSaveResult()
                                .setAuditBatchId(99001L)
                                .setFieldAuditRevision(2L)
                                .setCellValuesHash("after-hash")
                                .setFieldAuditHeadHash("after-head")
                                .setChangedFieldCount(2));

        MesTeamLeaderBatchRecordBackfillResult first = service.backfillCompletedProcess(command());
        MesTeamLeaderBatchRecordBackfillResult concurrentReplay = service.backfillCompletedProcess(command());

        assertEquals(2, first.getAppliedFieldCount());
        assertEquals(2, concurrentReplay.getAppliedFieldCount());

        String expectedIdempotencyKey = "PROCESS_POOL_REPORT_BACKFILL:1001:9001:5001";
        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService, times(2)).saveSystemCellLinkChanges(auditCaptor.capture());
        assertEquals(expectedIdempotencyKey, auditCaptor.getAllValues().get(0).getIdempotencyKey());
        assertEquals(expectedIdempotencyKey, auditCaptor.getAllValues().get(1).getIdempotencyKey());
    }

    @Test
    void shouldBlockWhenFormalBatchRecordBindingIsMissing() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.backfillCompletedProcess(command()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_BINDING_REQUIRED.getCode(), ex.getCode());
        verify(executionService, never()).openOrCreateByContext(any());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
    }

    @Test
    void shouldBlockWhenFormalFieldMappingIsMissing() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.backfillCompletedProcess(command()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED.getCode(), ex.getCode());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
    }

    private static MesTeamLeaderBatchRecordBackfillCommand command() {
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
                .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation() {
        return MesProcessPoolReportAllocationDO.builder()
                .eventId(1001L)
                .reviewId(7001L)
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal("80"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 1, 9, 1))
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

    private static MesProRouteFlowProcessBatchRecordDO binding() {
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

    private static MesProBatchRecordExecutionDO execution() {
        return MesProBatchRecordExecutionDO.builder()
                .id(8801L)
                .status(0)
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash("genesis")
                .cellValuesHash("before-hash")
                .executionSnapshotJson("""
                        {"fields":[
                          {"fieldPath":"report.outputQuantity","fieldKey":"outputQuantity","rowIndex":5,"columnIndex":2,"valueType":"NUMBER"},
                          {"fieldPath":"report.pressure","fieldKey":"pressure","rowIndex":6,"columnIndex":2,"valueType":"NUMBER"}
                        ]}
                        """)
                .cellValuesJson("[]")
                .build();
    }

    private static MesProBatchRecordCellLinkRuleDO rule(Long id, String sourceFieldCode, Integer rowIndex,
                                                        Integer columnIndex,
                                                        MesProBatchRecordExecutionFieldAuditValueType valueType) {
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
        rule.setTargetValueType(valueType.name());
        rule.setEnabled(true);
        return rule;
    }
}
