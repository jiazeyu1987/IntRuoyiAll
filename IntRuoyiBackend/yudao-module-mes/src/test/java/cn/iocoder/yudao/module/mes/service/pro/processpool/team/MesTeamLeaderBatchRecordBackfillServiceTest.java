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
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProductionPickListSourceService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock
    private MesProductionPickListSourceService productionPickListSourceService;

    private MesTeamLeaderBatchRecordBackfillService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderBatchRecordBackfillServiceImpl(bindingMapper, executionService, executionMapper,
                ruleMapper, fieldAuditService, productionPickListSourceService);
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
        assertFieldAuditIdempotencyKey(saveCommand.getIdempotencyKey());
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
    void shouldBackfillProcessPoolReportWithoutDccProjectContext() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(
                        rule(1L, "outputQuantity", 5, 2, MesProBatchRecordExecutionFieldAuditValueType.NUMBER)));
        when(fieldAuditService.saveSystemCellLinkChanges(any(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class)))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult().setChangedFieldCount(1));

        MesTeamLeaderBatchRecordBackfillResult result = service.backfillCompletedProcess(
                command().setDccProjectCodeId(null));

        assertEquals(8801L, result.getExecutionId());
        assertEquals(1, result.getAppliedFieldCount());
        verify(productionPickListSourceService, never()).resolveValue(any());
    }

    @Test
    void shouldRejectDuplicateSourceEventsInBackfillContext() {
        ServiceException ex = assertThrows(ServiceException.class, () -> service.backfillCompletedProcess(
                command().setSourceEvents(List.of(
                        event(1001L, "{\"outputQuantity\":80,\"pressure\":15}", LocalDateTime.of(2026, 8, 1, 9, 0)),
                        event(1001L, "{\"outputQuantity\":81,\"pressure\":16}", LocalDateTime.of(2026, 8, 1, 9, 5))))));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verifyNoInteractions(bindingMapper, executionService, executionMapper, ruleMapper,
                fieldAuditService, productionPickListSourceService);
    }

    @Test
    void shouldBackfillTargetOrderProcessWhenSourceEventRouteProcessDiffers() {
        MesProcessPoolReportAllocationDO targetAllocation = allocation().setRouteProcessId(5101L);
        MesTeamLeaderBatchRecordBackfillCommand targetCommand = command()
                .setAllocation(targetAllocation)
                .setAllocations(List.of(targetAllocation));
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5101L), "BATCH"))
                .thenReturn(List.of(binding().setRouteProcessId(5101L)));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(execution().setRouteProcessId(5101L));
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(rule(1L, "allocatedQuantity", 5, 2,
                        MesProBatchRecordExecutionFieldAuditValueType.NUMBER)));
        when(fieldAuditService.saveSystemCellLinkChanges(any(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class)))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult().setChangedFieldCount(1));

        service.backfillCompletedProcess(targetCommand);

        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> openCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(executionService).openOrCreateByContext(openCaptor.capture());
        assertEquals(5101L, openCaptor.getValue().getRouteProcessId());
        assertEquals(6001L, openCaptor.getValue().getProcessId());
        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService).saveSystemCellLinkChanges(auditCaptor.capture());
        assertFieldAuditIdempotencyKey(auditCaptor.getValue().getIdempotencyKey());
    }

    @Test
    void shouldOpenAndValidateExecutionInCurrentEdhrBatchTaskContext() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(currentBatchExecution());
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(rule(1L, "outputQuantity", 5, 2,
                        MesProBatchRecordExecutionFieldAuditValueType.NUMBER)));
        when(fieldAuditService.saveSystemCellLinkChanges(any(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class)))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult()
                        .setAuditBatchId(99001L)
                        .setCellValuesHash("after-hash")
                        .setFieldAuditHeadHash("after-head")
                        .setChangedFieldCount(1));

        MesTeamLeaderBatchRecordBackfillResult result = service.backfillCompletedProcess(
                command().setBatchExecutionId(9701L).setBatchExecutionTaskId(9801L));

        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> openCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(executionService).openOrCreateByContext(openCaptor.capture());
        assertEquals(9701L, openCaptor.getValue().getBatchExecutionId());
        assertEquals(9801L, openCaptor.getValue().getTaskId());
        assertEquals(8801L, result.getExecutionId());
        assertEquals(99001L, result.getAuditBatchId());
        assertEquals("after-hash", result.getCellValuesHash());
        assertEquals("after-head", result.getFieldAuditHeadHash());
    }

    @Test
    void shouldRejectOpenedExecutionOutsideCurrentEdhrBatchTaskContext() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(rule(1L, "outputQuantity", 5, 2,
                        MesProBatchRecordExecutionFieldAuditValueType.NUMBER)));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(execution()
                .setBatchExecutionId(9701L)
                .setTaskId(9999L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.backfillCompletedProcess(
                command().setBatchExecutionId(9701L).setBatchExecutionTaskId(9801L)));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_EXECUTION_REQUIRED.getCode(), ex.getCode());
        verify(ruleMapper).selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A");
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
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

        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService, times(2)).saveSystemCellLinkChanges(auditCaptor.capture());
        String firstIdempotencyKey = auditCaptor.getAllValues().get(0).getIdempotencyKey();
        assertFieldAuditIdempotencyKey(firstIdempotencyKey);
        assertEquals(firstIdempotencyKey, auditCaptor.getAllValues().get(1).getIdempotencyKey());
    }

    @Test
    void shouldAggregateAllConfirmedReportsIntoFormalBatchRecordWithConfiguredStrategies() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(aggregateExecution());
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(
                        rule(1L, "allocatedQuantity", 5, 2,
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, "SUM"),
                        rule(2L, "pressure", 6, 2,
                                MesProBatchRecordExecutionFieldAuditValueType.STRING, "LIST"),
                        rule(3L, "deviceCode", 7, 2,
                                MesProBatchRecordExecutionFieldAuditValueType.STRING, "DISTINCT_LIST")));
        when(fieldAuditService.saveSystemCellLinkChanges(any(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class)))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult()
                        .setFieldAuditRevision(2L)
                        .setCellValuesHash("after-hash")
                        .setFieldAuditHeadHash("after-head")
                        .setChangedFieldCount(3));

        MesTeamLeaderBatchRecordBackfillResult result = service.backfillCompletedProcess(aggregateCommand());

        assertEquals(8801L, result.getExecutionId());
        assertEquals(3, result.getAppliedFieldCount());
        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService).saveSystemCellLinkChanges(auditCaptor.capture());
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand saveCommand = auditCaptor.getValue();
        assertFieldAuditIdempotencyKey(saveCommand.getIdempotencyKey());
        List<MesProBatchRecordExecutionFieldAuditChange> changes = saveCommand.getChanges();
        assertEquals(new BigDecimal("200"), changes.get(0).getNewValueJson());
        assertEquals("15,18", changes.get(1).getNewValueJson());
        assertEquals("DEV-A,DEV-B", changes.get(2).getNewValueJson());
    }

    @Test
    void shouldBlockMultiSourceBackfillWhenAggregationStrategyIsMissing() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(aggregateExecution());
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(ruleWithoutAggregationStrategy(2L, "pressure", 6, 2,
                        MesProBatchRecordExecutionFieldAuditValueType.STRING)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.backfillCompletedProcess(aggregateCommand()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED.getCode(), ex.getCode());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
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
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.backfillCompletedProcess(command()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_FIELD_MAPPING_REQUIRED.getCode(), ex.getCode());
        verify(executionService, never()).openOrCreateByContext(any());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
    }

    @Test
    void shouldResolveProductionPickListBeforeOpeningExecutionAndWriteFirstLotValue() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        MesProBatchRecordCellLinkRuleDO pickRule = ruleWithoutAggregationStrategy(3L,
                "material.3201.lotNumber", 7, 2, MesProBatchRecordExecutionFieldAuditValueType.STRING);
        pickRule.setSourceType("PRODUCTION_PICK_LIST");
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(pickRule));
        when(productionPickListSourceService.resolveValue(any()))
                .thenReturn(new MesProductionPickListSourceService.ResolvedValue(9001L, 9101L,
                        "LOT-FIRST", "pick-evidence"));
        when(executionService.openOrCreateByContext(any(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(8801L));
        when(executionMapper.selectById(8801L)).thenReturn(execution().setExecutionSnapshotJson("""
                {"fields":[{"fieldPath":"report.materialLot","fieldKey":"materialLot",
                "rowIndex":7,"columnIndex":2,"valueType":"STRING"}]}
                """));
        when(fieldAuditService.saveSystemCellLinkChanges(any()))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult().setChangedFieldCount(1));

        service.backfillCompletedProcess(command().setWorkOrder(workOrder().setProductId(3101L)));

        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService).saveSystemCellLinkChanges(captor.capture());
        assertEquals("LOT-FIRST", captor.getValue().getChanges().get(0).getNewValueJson());
        assertFieldAuditIdempotencyKey(captor.getValue().getIdempotencyKey());
    }

    @Test
    void shouldRequireDccProjectContextWhenProductionPickListRuleIsConfigured() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(5001L), "BATCH"))
                .thenReturn(List.of(binding()));
        MesProBatchRecordCellLinkRuleDO pickRule = ruleWithoutAggregationStrategy(3L,
                "material.3201.lotNumber", 7, 2, MesProBatchRecordExecutionFieldAuditValueType.STRING);
        pickRule.setSourceType("PRODUCTION_PICK_LIST");
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(pickRule));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.backfillCompletedProcess(command().setDccProjectCodeId(null)));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(ruleMapper).selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A");
        verify(productionPickListSourceService, never()).resolveValue(any());
        verify(executionService, never()).openOrCreateByContext(any());
    }

    private static MesTeamLeaderBatchRecordBackfillCommand command() {
        return new MesTeamLeaderBatchRecordBackfillCommand()
                .setPickListBindingId(8801L)
                .setEvent(event())
                .setAllocation(allocation())
                .setSourceEvents(List.of(event()))
                .setAllocations(List.of(allocation()))
                .setWorkOrder(workOrder())
                .setDccProjectCodeId(8001L);
    }

    private static void assertFieldAuditIdempotencyKey(String idempotencyKey) {
        assertEquals(64, idempotencyKey.length());
        assertTrue(idempotencyKey.matches("[0-9a-f]{64}"));
    }

    private static MesTeamLeaderBatchRecordBackfillCommand aggregateCommand() {
        MesProProcessPoolEventDO first = event(1001L,
                "{\"pressure\":15,\"deviceCode\":\"DEV-A\"}",
                LocalDateTime.of(2026, 8, 1, 8, 30));
        MesProProcessPoolEventDO second = event(1002L,
                "{\"pressure\":18,\"deviceCode\":\"DEV-B\"}",
                LocalDateTime.of(2026, 8, 1, 9, 0));
        return new MesTeamLeaderBatchRecordBackfillCommand()
                .setPickListBindingId(8801L)
                .setEvent(second)
                .setAllocation(allocation(7102L, 1002L, "80",
                        LocalDateTime.of(2026, 8, 1, 9, 1)))
                .setSourceEvents(List.of(first, second))
                .setAllocations(List.of(
                        allocation(7101L, 1001L, "120", LocalDateTime.of(2026, 8, 1, 8, 31)),
                        allocation(7102L, 1002L, "80", LocalDateTime.of(2026, 8, 1, 9, 1))))
                .setWorkOrder(workOrder())
                .setDccProjectCodeId(8001L)
                .setAggregateHash("agg-two-events")
                .setIdempotencyKey("PROCESS_POOL_REPORT_BACKFILL_AGG:9001:5001:6001:agg-two-events");
    }

    private static MesProProcessPoolEventDO event() {
        return event(1001L, "{\"outputQuantity\":80,\"pressure\":15}", LocalDateTime.of(2026, 8, 1, 9, 0));
    }

    private static MesProProcessPoolEventDO event(Long id, String rawPayload, LocalDateTime serverSubmitTime) {
        return MesProProcessPoolEventDO.builder()
                .id(id)
                .routeId(7001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .rawPayload(rawPayload)
                .serverSubmitTime(serverSubmitTime)
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation() {
        return allocation(7101L, 1001L, "80", LocalDateTime.of(2026, 8, 1, 9, 1));
    }

    private static MesProcessPoolReportAllocationDO allocation(Long id, Long eventId, String quantity,
                                                               LocalDateTime confirmedAt) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(id)
                .eventId(eventId)
                .reviewId(7001L)
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .allocatedQuantity(new BigDecimal(quantity))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(confirmedAt)
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
                .workOrderId(9001L)
                .routeProcessId(5001L)
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .recordCategory("BATCH_RECORD")
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

    private static MesProBatchRecordExecutionDO currentBatchExecution() {
        return execution()
                .setBatchExecutionId(9701L)
                .setTaskId(9801L);
    }

    private static MesProBatchRecordExecutionDO aggregateExecution() {
        return MesProBatchRecordExecutionDO.builder()
                .id(8801L)
                .workOrderId(9001L)
                .routeProcessId(5001L)
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .recordCategory("BATCH_RECORD")
                .status(0)
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash("genesis")
                .cellValuesHash("before-hash")
                .executionSnapshotJson("""
                        {"fields":[
                          {"fieldPath":"report.allocatedQuantity","fieldKey":"allocatedQuantity","rowIndex":5,"columnIndex":2,"valueType":"NUMBER"},
                          {"fieldPath":"report.pressure","fieldKey":"pressure","rowIndex":6,"columnIndex":2,"valueType":"STRING"},
                          {"fieldPath":"report.deviceCode","fieldKey":"deviceCode","rowIndex":7,"columnIndex":2,"valueType":"STRING"}
                        ]}
                        """)
                .cellValuesJson("[]")
                .build();
    }

    private static MesProBatchRecordCellLinkRuleDO rule(Long id, String sourceFieldCode, Integer rowIndex,
                                                        Integer columnIndex,
                                                        MesProBatchRecordExecutionFieldAuditValueType valueType) {
        return rule(id, sourceFieldCode, rowIndex, columnIndex, valueType,
                valueType == MesProBatchRecordExecutionFieldAuditValueType.NUMBER ? "SUM" : "LAST");
    }

    private static MesProBatchRecordCellLinkRuleDO rule(Long id, String sourceFieldCode, Integer rowIndex,
                                                        Integer columnIndex,
                                                        MesProBatchRecordExecutionFieldAuditValueType valueType,
                                                        String aggregationStrategy) {
        MesProBatchRecordCellLinkRuleDO rule = ruleWithoutAggregationStrategy(id, sourceFieldCode, rowIndex,
                columnIndex, valueType);
        rule.setAggregationStrategy(aggregationStrategy);
        return rule;
    }

    private static MesProBatchRecordCellLinkRuleDO ruleWithoutAggregationStrategy(
            Long id, String sourceFieldCode, Integer rowIndex, Integer columnIndex,
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
