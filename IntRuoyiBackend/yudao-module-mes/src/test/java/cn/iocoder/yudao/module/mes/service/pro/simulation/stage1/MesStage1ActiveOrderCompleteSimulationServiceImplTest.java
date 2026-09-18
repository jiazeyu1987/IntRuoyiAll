package cn.iocoder.yudao.module.mes.service.pro.simulation.stage1;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.batch.MesWmBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyFieldMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.batch.MesWmBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.warehouse.MesWmWarehouseAreaMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.warehouse.MesWmWarehouseLocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.warehouse.MesWmWarehouseMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotCodec;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceSelectionSnapshotCodec;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.hutool.crypto.digest.DigestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesStage1ActiveOrderCompleteSimulationServiceImplTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesProWorkOrderBomMapper workOrderBomMapper;
    @Mock
    private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;
    @Mock
    private ErpKingdeeProductionPickListMapper pickListMapper;
    @Mock
    private ErpKingdeeProductionPickListItemMapper pickListItemMapper;
    @Mock
    private ErpKingdeeProductionReplenishmentListMapper replenishmentListMapper;
    @Mock
    private ErpKingdeeProductionReplenishmentListItemMapper replenishmentListItemMapper;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Mock
    private MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    @Mock
    private MesProcessPoolReviewCopyFieldMapper reviewCopyFieldMapper;
    @Mock
    private MesProcessPoolReviewCopyMapper reviewCopyMapper;
    @Mock
    private MesProProcessPoolEventRevisionDiffMapper eventRevisionDiffMapper;
    @Mock
    private MesProProcessPoolEventRevisionMapper eventRevisionMapper;
    @Mock
    private MesPqcInspectionPieceDetailMapper pieceMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesPqcProcessInspectionAggregateDetailMapper aggregateMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesProcessPoolReportAllocationStateMapper allocationStateMapper;
    @Mock
    private MesProcessPoolReportAllocationAdjustmentAuditMapper allocationAuditMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock
    private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock
    private MesProcessPoolActiveOrderCompletionBackfillMapper completionBackfillMapper;
    @Mock
    private MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;
    @Mock
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock
    private MesWmWarehouseMapper warehouseMapper;
    @Mock
    private MesWmWarehouseLocationMapper warehouseLocationMapper;
    @Mock
    private MesWmWarehouseAreaMapper warehouseAreaMapper;
    @Mock
    private MesWmBatchMapper batchMapper;
    @Mock
    private MesWmMaterialStockMapper materialStockMapper;
    @Mock
    private MesWmProductIssueMapper productIssueMapper;
    @Mock
    private MesWmProductIssueLineMapper productIssueLineMapper;
    @Mock
    private MesWmProductIssueDetailMapper productIssueDetailMapper;
    @Mock
    private MesTeamLeaderActiveOrderSimulationService activeOrderSimulationService;

    @InjectMocks
    private MesStage1ActiveOrderCompleteSimulationServiceImpl service;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void publicStage1AllowsExistingFormalMaterialSourcesWithoutMaterializingP2() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(328L);
        MesProWorkOrderDO workOrder = workOrder();
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = processSnapshot();
        MesPqcInspectionTaskDO confirmedTask = confirmedPqcTask();
        MesProcessPoolReportAllocationDO allocation = MesProcessPoolReportAllocationDO.builder()
                .id(81001L)
                .activeOrderId(328L)
                .workOrderId(9001L)
                .routeProcessId(1001L)
                .processId(201L)
                .allocatedQuantity(new BigDecimal("100.000000"))
                .build();
        when(activeOrderMapper.selectByIdForUpdate(328L)).thenReturn(activeOrder);
        when(workOrderMapper.selectById(9001L)).thenReturn(workOrder);
        when(activeOrderSimulationService.simulateActiveOrderCompletion(3001L, 328L, "STAGE1", "STAGE1-unit"))
                .thenReturn(new MesTeamLeaderActiveOrderSimulationResult()
                        .setActiveOrderId(328L)
                        .setProductionSubmitCount(1)
                        .setProductionReviewCount(1)
                        .setPqcSubmitCount(1)
                        .setPqcReviewCount(1)
                        .setProductionProgressPercent(BigDecimal.valueOf(100))
                        .setInspectionProgressPercent(BigDecimal.valueOf(100)));
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(328L)).thenReturn(List.of(snapshot));
        when(allocationMapper.selectListByActiveOrderIds(List.of(328L))).thenReturn(List.of(allocation));
        when(pqcTaskMapper.selectListByActiveOrderId(328L)).thenReturn(List.of(confirmedTask));
        when(activeOrderMapper.updateSimulationMetadata(328L, Boolean.TRUE, "STAGE1", "STAGE1-unit"))
                .thenReturn(1);
        when(completionBackfillMapper.selectListByActiveOrderIdForUpdate(328L)).thenReturn(List.of());
        when(completionReceiptMapper.selectByActiveOrderIdForUpdate(328L)).thenReturn(null);
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(328L))).thenReturn(List.of());
        when(aggregateMapper.selectListByActiveOrderIdForUpdate(328L)).thenReturn(List.of());
        when(batchExecutionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(workOrderMapper.selectList(any())).thenReturn(List.of());

        MesStage1ActiveOrderCompleteSimulationResult result = service.simulate(command(328L));

        assertEquals(0, BigDecimal.valueOf(100).compareTo(result.getProductionProgressPercent()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(result.getInspectionProgressPercent()));
        assertEquals(List.of(), result.getPickListIds());
        verify(productIssueMapper, never()).selectListByWorkOrderIdForUpdate(9001L);
        verify(bindingMapper, never()).selectListByActiveOrderId(328L);
        verify(productIssueMapper, never()).insert(any(MesWmProductIssueDO.class));
        verify(bindingMapper, never()).insert(any(MesProcessPoolActiveOrderPickListBindingDO.class));
        verify(completionBackfillMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionBackfillDO.class));
    }

    @Test
    void simulatedTemplateCanBeUsedForRerunAndReadsAllPersistedBindings() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO simulatedTemplate = activeOrder(328L)
                .setSimulated(Boolean.TRUE)
                .setSimulationStage("STAGE1")
                .setSimulationRunId("STAGE1-source");
        MesProWorkOrderDO templateWorkOrder = workOrder();
        MesProcessPoolActiveOrderPickListBindingDO persistedBinding =
                MesProcessPoolActiveOrderPickListBindingDO.builder()
                        .id(7001L)
                        .activeOrderId(328L)
                        .workOrderId(9001L)
                        .pickListId(8001L)
                        .sourceSnapshotHash("source-hash")
                        .build();
        persistedBinding.setTenantId(1L);
        MesProcessPoolActiveOrderPickListBindingDO secondBinding = MesProcessPoolActiveOrderPickListBindingDO.builder()
                .id(7002L).activeOrderId(328L).workOrderId(9001L).pickListId(8002L)
                .sourceSnapshotHash("source-hash-2").build();
        secondBinding.setTenantId(1L);
        when(bindingMapper.selectListByActiveOrderId(328L)).thenReturn(List.of(persistedBinding, secondBinding));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "requireTemplate",
                simulatedTemplate, 3001L));
        @SuppressWarnings("unchecked")
        List<MesProcessPoolActiveOrderPickListBindingDO> resolved = ReflectionTestUtils.invokeMethod(
                service, "resolveTemplateBindings", simulatedTemplate, templateWorkOrder, command(328L));

        assertIterableEquals(List.of(persistedBinding, secondBinding), resolved);
        verify(pickListItemMapper, never()).selectListByProductionOrderNo(any());
    }

    @Test
    void missingFormalPickListsCreateExplicitSimulationFromWorkOrderBom() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO simulatedTemplate = activeOrder(150L).setSimulated(Boolean.TRUE);
        when(bindingMapper.selectListByActiveOrderId(150L)).thenReturn(List.of());
        when(pickListItemMapper.selectListByProductionOrderNo("WO-001")).thenReturn(List.of());
        when(workOrderBomMapper.selectListByWorkOrderId(9001L)).thenReturn(List.of(
                MesProWorkOrderBomDO.builder().id(5001L).workOrderId(9001L).itemId(6001L)
                        .quantity(new BigDecimal("12")).build()));
        when(itemMapper.selectById(6001L)).thenReturn(MesMdItemDO.builder().id(6001L)
                .code("MAT-SIM-001").name("模拟物料").specification("S1").build());
        AtomicLong ids = new AtomicLong(9901L);
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, ErpKingdeeProductionPickListDO.class).setId(ids.getAndIncrement());
            return 1;
        }).when(pickListMapper).insert(any(ErpKingdeeProductionPickListDO.class));
        org.mockito.Mockito.doAnswer(invocation -> 1)
                .when(pickListItemMapper).insert(any(ErpKingdeeProductionPickListItemDO.class));

        @SuppressWarnings("unchecked")
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = ReflectionTestUtils.invokeMethod(
                service, "resolveTemplateBindings", simulatedTemplate, workOrder(), command(150L));

        assertEquals(1, bindings.size());
        assertEquals(9901L, bindings.get(0).getPickListId());
        assertTrue(Boolean.TRUE.equals(bindings.get(0).getSimulated()));
        assertTrue(bindings.get(0).getSourceBillNo().startsWith("STAGE1-PL-"));
        verify(pickListMapper).insert(any(ErpKingdeeProductionPickListDO.class));
        verify(pickListItemMapper).insert(any(ErpKingdeeProductionPickListItemDO.class));
    }

    @Test
    void missingFormalPickListsPreferRealProductionMaterialListBeforeBomSimulation() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO simulatedTemplate = activeOrder(151L).setSimulated(Boolean.TRUE);
        when(bindingMapper.selectListByActiveOrderId(151L)).thenReturn(List.of());
        when(pickListItemMapper.selectListByProductionOrderNo("WO-001")).thenReturn(List.of());
        when(productionMaterialListMapper.selectListByProductionOrderNo("WO-001")).thenReturn(List.of(
                MesKingdeeProductionMaterialListDO.builder()
                        .id(5101L)
                        .sourceBillNo("PML-001")
                        .sourceEntryId("PML-E1")
                        .productionOrderNo("WO-001")
                        .productionOrderLineNo(7)
                        .childMaterialCode("MAT-PML-001")
                        .childMaterialName("真实用料清单物料")
                        .childMaterialSpecification("PML-S1")
                        .childUnitName("个")
                        .requiredQuantity(new BigDecimal("8"))
                        .build()));
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, ErpKingdeeProductionPickListDO.class).setId(9902L);
            return 1;
        }).when(pickListMapper).insert(any(ErpKingdeeProductionPickListDO.class));
        org.mockito.Mockito.doAnswer(invocation -> 1)
                .when(pickListItemMapper).insert(any(ErpKingdeeProductionPickListItemDO.class));

        @SuppressWarnings("unchecked")
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = ReflectionTestUtils.invokeMethod(
                service, "resolveTemplateBindings", simulatedTemplate, workOrder(), command(151L));

        assertEquals(List.of(9902L), bindings.stream()
                .map(MesProcessPoolActiveOrderPickListBindingDO::getPickListId).toList());
        ArgumentCaptor<ErpKingdeeProductionPickListItemDO> itemCaptor =
                ArgumentCaptor.forClass(ErpKingdeeProductionPickListItemDO.class);
        verify(pickListItemMapper).insert(itemCaptor.capture());
        assertEquals("MAT-PML-001", itemCaptor.getValue().getMaterialNumber());
        assertEquals(new BigDecimal("8"), itemCaptor.getValue().getActualQuantity());
        assertEquals(7, itemCaptor.getValue().getProductionOrderLineNo());
        assertTrue(itemCaptor.getValue().getLotNumber().startsWith("STAGE1-LOT-"));
        assertTrue(itemCaptor.getValue().getRawPayload().contains("PML-001"));
        verify(workOrderBomMapper, never()).selectListByWorkOrderId(any());
    }

    @Test
    void formalPickListsAreResolvedAsIndependentSources() {
        TenantContextHolder.setTenantId(1L);
        MesProWorkOrderDO workOrder = workOrder();
        ErpKingdeeProductionPickListDO header = ErpKingdeeProductionPickListDO.builder()
                .id(9001L)
                .sourceFormId("PRD_PickMtrl")
                .sourceFid("FID-9001")
                .sourceBillNo("PICK-9001")
                .documentStatus("C")
                .build();
        ErpKingdeeProductionPickListItemDO item = ErpKingdeeProductionPickListItemDO.builder()
                .id(9101L)
                .productionPickListId(9001L)
                .sourceFid("FID-9001")
                .sourceEntryId("1")
                .sourceLineKey("PICK-9001-LINE-1")
                .sourceBillNo("PICK-9001")
                .materialNumber("MAT-001")
                .materialName("正式物料")
                .requestedQuantity(new BigDecimal("5"))
                .actualQuantity(new BigDecimal("5"))
                .baseActualQuantity(new BigDecimal("5"))
                .lotNumber("LOT-001")
                .productionOrderNo("WO-001")
                .build();
        ErpKingdeeProductionPickListDO secondHeader = ErpKingdeeProductionPickListDO.builder()
                .id(9002L).sourceFormId("PRD_PickMtrl").sourceFid("FID-9002")
                .sourceBillNo("PICK-9002").documentStatus("C").build();
        ErpKingdeeProductionPickListItemDO secondItem = ErpKingdeeProductionPickListItemDO.builder()
                .id(9102L).productionPickListId(9002L).sourceFid("FID-9002")
                .sourceEntryId("1").sourceLineKey("PICK-9002-LINE-1").sourceBillNo("PICK-9002")
                .materialNumber("MAT-002").materialName("正式物料2").requestedQuantity(new BigDecimal("6"))
                .actualQuantity(new BigDecimal("6")).baseActualQuantity(new BigDecimal("6"))
                .lotNumber("LOT-002").productionOrderNo("WO-001").build();
        when(pickListItemMapper.selectListByProductionOrderNo("WO-001")).thenReturn(List.of(item, secondItem));
        when(pickListMapper.selectById(9001L)).thenReturn(header);
        when(pickListMapper.selectById(9002L)).thenReturn(secondHeader);

        @SuppressWarnings("unchecked")
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = ReflectionTestUtils.invokeMethod(
                service, "resolveTemplateBindings", activeOrder(328L), workOrder, command(328L));

        assertEquals(List.of(9001L, 9002L), bindings.stream()
                .map(MesProcessPoolActiveOrderPickListBindingDO::getPickListId).toList());
        assertEquals(List.of("PICK-9001", "PICK-9002"), bindings.stream()
                .map(MesProcessPoolActiveOrderPickListBindingDO::getSourceBillNo).toList());
        verify(itemMapper, never()).selectById(any());
        verify(pickListMapper, never()).insert((ErpKingdeeProductionPickListDO) any());
        verify(pickListItemMapper, never()).insert((ErpKingdeeProductionPickListItemDO) any());
    }

    @Test
    void simulatedTemplateWithoutBindingsUsesCurrentWorkOrderPickListsBeforeSourceActiveOrder() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO copiedTemplate = activeOrder(396L)
                .setSimulated(Boolean.TRUE);
        MesProWorkOrderDO copiedWorkOrder = workOrder()
                .setCode("SIM-COPY-FORMAL-WO-001")
                .setRemark("[MES_STAGE1_SIMULATION][simulationRunId=copy][actorUserId=3001][sourceActiveOrderId=1009200001]");
        ErpKingdeeProductionPickListDO firstHeader = formalPickList(9001L, "FID-9001", "PICK-9001");
        ErpKingdeeProductionPickListDO secondHeader = formalPickList(9002L, "FID-9002", "PICK-9002");
        ErpKingdeeProductionPickListItemDO firstItem = formalPickListItem(9101L, 9001L)
                .setProductionOrderNo("SIM-COPY-FORMAL-WO-001");
        ErpKingdeeProductionPickListItemDO secondItem = formalPickListItem(9102L, 9002L)
                .setProductionOrderNo("SIM-COPY-FORMAL-WO-001");
        when(bindingMapper.selectListByActiveOrderId(396L)).thenReturn(List.of());
        when(pickListItemMapper.selectListByProductionOrderNo("SIM-COPY-FORMAL-WO-001"))
                .thenReturn(List.of(firstItem, secondItem));
        when(pickListMapper.selectById(9001L)).thenReturn(firstHeader);
        when(pickListMapper.selectById(9002L)).thenReturn(secondHeader);

        @SuppressWarnings("unchecked")
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = ReflectionTestUtils.invokeMethod(
                service, "resolveTemplateBindings", copiedTemplate, copiedWorkOrder, command(396L));

        assertEquals(List.of(9001L, 9002L), bindings.stream()
                .map(MesProcessPoolActiveOrderPickListBindingDO::getPickListId).toList());
        verify(pickListItemMapper, times(2)).selectListByProductionOrderNo("SIM-COPY-FORMAL-WO-001");
        verify(pickListItemMapper, never()).selectListByProductionOrderNo("FORMAL-WO-001");
        verify(activeOrderMapper, never()).selectById(1009200001L);
    }

    @Test
    void simulatedTemplateWithoutCurrentSourcesFallsBackToSourceActiveOrderPickLists() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO copiedTemplate = activeOrder(397L)
                .setSimulated(Boolean.TRUE);
        MesProWorkOrderDO copiedWorkOrder = workOrder()
                .setCode("SIM-COPY-NO-CURRENT-SOURCE")
                .setRemark("[MES_STAGE1_SIMULATION][simulationRunId=copy][actorUserId=3001][sourceActiveOrderId=1009200001]");
        MesProcessPoolActiveOrderDO sourceActiveOrder = activeOrder(1009200001L)
                .setWorkOrderId(9101L);
        MesProWorkOrderDO sourceWorkOrder = workOrder()
                .setId(9101L)
                .setCode("FORMAL-WO-001");
        ErpKingdeeProductionPickListDO firstHeader = formalPickList(9001L, "FID-9001", "PICK-9001");
        ErpKingdeeProductionPickListItemDO firstItem = formalPickListItem(9101L, 9001L)
                .setProductionOrderNo("FORMAL-WO-001");
        when(bindingMapper.selectListByActiveOrderId(397L)).thenReturn(List.of());
        when(pickListItemMapper.selectListByProductionOrderNo("SIM-COPY-NO-CURRENT-SOURCE"))
                .thenReturn(List.of());
        when(productionMaterialListMapper.selectListByProductionOrderNo("SIM-COPY-NO-CURRENT-SOURCE"))
                .thenReturn(List.of());
        when(activeOrderMapper.selectById(1009200001L)).thenReturn(sourceActiveOrder);
        when(workOrderMapper.selectById(9101L)).thenReturn(sourceWorkOrder);
        when(pickListItemMapper.selectListByProductionOrderNo("FORMAL-WO-001"))
                .thenReturn(List.of(firstItem));
        when(pickListMapper.selectById(9001L)).thenReturn(firstHeader);

        @SuppressWarnings("unchecked")
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = ReflectionTestUtils.invokeMethod(
                service, "resolveTemplateBindings", copiedTemplate, copiedWorkOrder, command(397L));

        assertEquals(List.of(9001L), bindings.stream()
                .map(MesProcessPoolActiveOrderPickListBindingDO::getPickListId).toList());
        verify(pickListItemMapper).selectListByProductionOrderNo("FORMAL-WO-001");
    }

    @Test
    void stage1FixturePointsToUltimateFormalSourceActiveOrderForSimulationCopy() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO copiedTemplate = activeOrder(396L)
                .setSimulated(Boolean.TRUE)
                .setSimulationStage("LATEST_VERSION_COPY");
        MesProWorkOrderDO copiedWorkOrder = workOrder()
                .setCode("SIM-COPY-FORMAL-WO-001")
                .setRemark("[SIM-COPY][sourceActiveOrderId=1009200001][simulationRunId=SIMCOPY-unit]");
        MesProcessPoolActiveOrderDO sourceActiveOrder = activeOrder(1009200001L)
                .setWorkOrderId(9101L)
                .setSimulated(Boolean.FALSE);
        MesProWorkOrderDO sourceWorkOrder = workOrder()
                .setId(9101L)
                .setCode("FORMAL-WO-001");
        when(activeOrderMapper.selectById(1009200001L)).thenReturn(sourceActiveOrder);
        when(workOrderMapper.selectById(9101L)).thenReturn(sourceWorkOrder);

        Long sourceActiveOrderId = ReflectionTestUtils.invokeMethod(service,
                "resolveFormalPickListSourceActiveOrderId", copiedTemplate, copiedWorkOrder);

        assertEquals(1009200001L, sourceActiveOrderId);
    }

    @Test
    void incompleteFormalPickListFailsTheEntireResolution() {
        TenantContextHolder.setTenantId(1L);
        MesProWorkOrderDO workOrder = workOrder();
        ErpKingdeeProductionPickListItemDO item = ErpKingdeeProductionPickListItemDO.builder()
                .id(9101L).productionPickListId(9001L).productionOrderNo("WO-001")
                .sourceFid("FID-9001").sourceEntryId("1").sourceLineKey("PICK-9001-LINE-1")
                .sourceBillNo("PICK-9001").materialNumber("MAT-001").materialName("正式物料")
                .requestedQuantity(BigDecimal.ONE).build();
        when(pickListItemMapper.selectListByProductionOrderNo("WO-001")).thenReturn(List.of(item));
        when(pickListMapper.selectById(9001L)).thenReturn(formalPickList(9001L, "FID-9001", "PICK-9001"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "resolveTemplateBindings",
                        activeOrder(328L), workOrder, command(328L)));

        assertEquals(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED.getCode(), exception.getCode());
        verify(pickListMapper, never()).insert((ErpKingdeeProductionPickListDO) any());
        verify(pickListItemMapper, never()).insert((ErpKingdeeProductionPickListItemDO) any());
    }

    @Test
    void formalProductIssueCreatesOneIssuePerPickListBinding() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO activeOrder = activeOrder(328L);
        MesProcessPoolActiveOrderPickListBindingDO first = MesProcessPoolActiveOrderPickListBindingDO.builder()
                .id(7001L).activeOrderId(328L).pickListId(8001L).build();
        MesProcessPoolActiveOrderPickListBindingDO second = MesProcessPoolActiveOrderPickListBindingDO.builder()
                .id(7002L).activeOrderId(328L).pickListId(8002L).build();
        when(bindingMapper.selectListByActiveOrderId(328L)).thenReturn(List.of(first, second));
        when(bindingItemMapper.selectListByBindingId(7001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                        .bindingId(7001L).materialNumber("MAT-001").requestedQuantity(BigDecimal.ONE)
                        .lotNumber("LOT-A").build()));
        when(bindingItemMapper.selectListByBindingId(7002L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                        .bindingId(7002L).materialNumber("MAT-002").requestedQuantity(BigDecimal.ONE)
                        .lotNumber("LOT-B").build()));
        when(warehouseMapper.selectByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE))
                .thenReturn(MesWmWarehouseDO.builder().id(8101L).build());
        when(warehouseLocationMapper.selectByCode(8101L, MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION))
                .thenReturn(MesWmWarehouseLocationDO.builder().id(8201L).build());
        when(warehouseAreaMapper.selectByCode(8201L, MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA))
                .thenReturn(MesWmWarehouseAreaDO.builder().id(8301L).build());
        when(itemMapper.selectByCode("MAT-001")).thenReturn(MesMdItemDO.builder().id(1001L).itemTypeId(2001L).build());
        when(itemMapper.selectByCode("MAT-002")).thenReturn(MesMdItemDO.builder().id(1002L).itemTypeId(2002L).build());
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, MesWmProductIssueDO.class)
                    .setId(invocation.getArgument(0, MesWmProductIssueDO.class).getCode().endsWith("-7001")
                            ? 90001L : 90002L);
            return 1;
        }).when(productIssueMapper).insert(any(MesWmProductIssueDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, MesWmProductIssueLineDO.class).setId(91001L);
            return 1;
        }).when(productIssueLineMapper).insert(any(MesWmProductIssueLineDO.class));
        AtomicLong batchIds = new AtomicLong(92001L);
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, MesWmBatchDO.class).setId(batchIds.getAndIncrement());
            return 1;
        }).when(batchMapper).insert(any(MesWmBatchDO.class));

        ReflectionTestUtils.invokeMethod(service, "createFormalProductIssue", activeOrder,
                workOrder(), command(328L));

        verify(bindingItemMapper).selectListByBindingId(7001L);
        verify(bindingItemMapper).selectListByBindingId(7002L);
        ArgumentCaptor<MesWmProductIssueDO> issues = ArgumentCaptor.forClass(MesWmProductIssueDO.class);
        verify(productIssueMapper, times(2)).insert(issues.capture());
        assertEquals(List.of("STAGE1-ISSUE-STAGE1unit-7001", "STAGE1-ISSUE-STAGE1unit-7002"),
                issues.getAllValues().stream().map(MesWmProductIssueDO::getCode).toList());
        ArgumentCaptor<MesWmBatchDO> batches = ArgumentCaptor.forClass(MesWmBatchDO.class);
        verify(batchMapper, times(2)).insert(batches.capture());
        assertEquals(List.of("LOT-A", "LOT-B"), batches.getAllValues().stream()
                .map(MesWmBatchDO::getLotNumber).toList());
        verify(productIssueDetailMapper, times(2)).insert(any(MesWmProductIssueDetailDO.class));
    }

    @Test
    void copiedPickListsUseDistinctSourceIdentitiesAndRetainFormalTrace() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderPickListBindingDO first = sourceBinding(9001L, "FID-9001", "PICK-9001");
        MesProcessPoolActiveOrderPickListBindingDO second = sourceBinding(9002L, "FID-9002", "PICK-9002");
        when(pickListMapper.selectById(9001L)).thenReturn(formalPickList(9001L, "FID-9001", "PICK-9001"));
        when(pickListMapper.selectById(9002L)).thenReturn(formalPickList(9002L, "FID-9002", "PICK-9002"));
        when(pickListItemMapper.selectListByPickListIds(List.of(9001L))).thenReturn(List.of(formalPickListItem(9101L, 9001L)));
        when(pickListItemMapper.selectListByPickListIds(List.of(9002L))).thenReturn(List.of(formalPickListItem(9102L, 9002L)));
        AtomicLong nextId = new AtomicLong(9201L);
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, ErpKingdeeProductionPickListDO.class).setId(nextId.getAndIncrement());
            return 1;
        }).when(pickListMapper).insert(any(ErpKingdeeProductionPickListDO.class));

        ReflectionTestUtils.invokeMethod(service, "clonePickList", first, "STAGE1-WO-unit", "STAGE1-unit", 3001L);
        ReflectionTestUtils.invokeMethod(service, "clonePickList", second, "STAGE1-WO-unit", "STAGE1-unit", 3001L);

        ArgumentCaptor<ErpKingdeeProductionPickListDO> copies = ArgumentCaptor.forClass(ErpKingdeeProductionPickListDO.class);
        verify(pickListMapper, times(2)).insert(copies.capture());
        List<ErpKingdeeProductionPickListDO> copiedHeaders = copies.getAllValues();
        assertEquals(2, copiedHeaders.stream().map(ErpKingdeeProductionPickListDO::getSourceFid).distinct().count());
        assertTrue(copiedHeaders.get(0).getSourceFid().endsWith("-PL-9001-FID"));
        assertTrue(copiedHeaders.get(1).getSourceFid().endsWith("-PL-9002-FID"));
        assertTrue(copiedHeaders.get(0).getSourceBillNo().endsWith("-9001"));
        assertTrue(copiedHeaders.get(1).getSourceBillNo().endsWith("-9002"));
        assertTrue(copiedHeaders.get(0).getRawPayload().contains("FID-9001"));
        assertTrue(copiedHeaders.get(1).getRawPayload().contains("FID-9002"));
    }

    @Test
    void cleanupDeletesEveryCopiedPickListForTheRun() {
        TenantContextHolder.setTenantId(1L);
        ErpKingdeeProductionPickListDO first = formalPickList(9201L, "STAGE1-STAGE1unit-PL-9001-FID",
                "STAGE1-PL-STAGE1unit-9001");
        ErpKingdeeProductionPickListDO second = formalPickList(9202L, "STAGE1-STAGE1unit-PL-9002-FID",
                "STAGE1-PL-STAGE1unit-9002");
        when(pickListMapper.selectList(any())).thenReturn(List.of(first, second));
        when(pickListItemMapper.selectListByPickListIds(List.of(9201L))).thenReturn(List.of());
        when(pickListItemMapper.selectListByPickListIds(List.of(9202L))).thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(service, "cleanupCopiedPickLists", "STAGE1-unit");

        verify(pickListMapper).deleteById(9201L);
        verify(pickListMapper).deleteById(9202L);
    }

    @Test
    void clonedLegacyProcessSnapshotsRebuildProductionConfigEnvelopeForResetOrder() {
        TenantContextHolder.setTenantId(1L);
        String parameterJson = "[]";
        String deviceSelectionJson = "[]";
        String lossReasonJson = "[]";
        MesProcessPoolActiveOrderProcessSnapshotDO legacySnapshot =
                new MesProcessPoolActiveOrderProcessSnapshotDO()
                        .setId(5101L)
                        .setActiveOrderId(328L)
                        .setWorkOrderId(9001L)
                        .setRouteId(922119L)
                        .setRouteVersionId(448L)
                        .setRouteProcessId(1001L)
                        .setProcessId(201L)
                        .setProcessCodeSnapshot("ROUGH_WASH")
                        .setProcessNameSnapshot("粗洗工序")
                        .setErpFixedQuantitySnapshot(new BigDecimal("100.000000"))
                        .setProductionQuantityFactorSnapshot(new BigDecimal("1.000000"))
                        .setPlannedQuantitySnapshot(new BigDecimal("100.000000"))
                        .setParameterSnapshotJson(parameterJson)
                        .setParameterSnapshotSha256(MesDeviceParameterSnapshotCodec.sha256(parameterJson))
                        .setParameterSnapshotState(MesDeviceParameterSnapshotCodec.STATE_FROZEN)
                        .setDeviceSelectionSnapshotJson(deviceSelectionJson)
                        .setDeviceSelectionSnapshotSha256(MesDeviceSelectionSnapshotCodec.sha256(deviceSelectionJson))
                        .setLossReasonSnapshotJson(lossReasonJson)
                        .setLossReasonSnapshotSha256(DigestUtil.sha256Hex(lossReasonJson))
                        .setOveragePercentSnapshot(new BigDecimal("0.000000"));
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(328L)).thenReturn(List.of(legacySnapshot));
        when(routeVersionMapper.selectById(448L)).thenReturn(routeVersion());

        ReflectionTestUtils.invokeMethod(service, "cloneSnapshots", 328L,
                activeOrder(901L).setWorkOrderId(9901L), "STAGE1-unit");

        ArgumentCaptor<MesProcessPoolActiveOrderProcessSnapshotDO> snapshotCaptor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderProcessSnapshotDO.class);
        verify(snapshotMapper).insert(snapshotCaptor.capture());
        MesProcessPoolActiveOrderProcessSnapshotDO cloned = snapshotCaptor.getValue();
        assertEquals(901L, cloned.getActiveOrderId());
        assertEquals(9901L, cloned.getWorkOrderId());
        assertNotNull(cloned.getProductionConfigSnapshotJson());
        assertEquals(DigestUtil.sha256Hex(cloned.getProductionConfigSnapshotJson()),
                cloned.getProductionConfigSnapshotSha256());
        assertEquals("STAGE1_TEMPLATE_PROCESS_SNAPSHOT", cloned.getProductionConfigMigrationSource());
        assertNotNull(cloned.getProductionConfigMigratedAt());
        assertTrue(cloned.getProductionConfigSnapshotJson().contains("\"routeProcessId\":1001"));
        assertTrue(cloned.getProductionConfigSnapshotJson().contains("\"processId\":201"));
        assertTrue(cloned.getProductionConfigSnapshotJson().contains("\"parameterRules\":[]"));
        assertTrue(cloned.getProductionConfigSnapshotJson().contains("\"deviceSelectionGroups\":[]"));
    }

    private static MesStage1ActiveOrderCompleteSimulationCommand command(Long activeOrderId) {
        return new MesStage1ActiveOrderCompleteSimulationCommand()
                .setSimulationRunId("STAGE1-unit")
                .setActiveOrderId(activeOrderId)
                .setActorUserId(3001L);
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long id) {
        MesProcessPoolActiveOrderDO activeOrder = MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .version(1)
                .build();
        activeOrder.setTenantId(1L);
        return activeOrder;
    }

    private static MesProWorkOrderDO workOrder() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-001")
                .productId(1001L)
                .quantity(new BigDecimal("100.000000"))
                .build();
        workOrder.setTenantId(1L);
        return workOrder;
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot() {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = new MesProcessPoolActiveOrderProcessSnapshotDO()
                .setId(5101L)
                .setActiveOrderId(328L)
                .setWorkOrderId(9001L)
                .setRouteId(922119L)
                .setRouteVersionId(448L)
                .setRouteProcessId(1001L)
                .setProcessId(201L)
                .setPlannedQuantitySnapshot(new BigDecimal("100.000000"));
        snapshot.setTenantId(1L);
        return snapshot;
    }

    private static MesPqcInspectionTaskDO confirmedPqcTask() {
        MesPqcInspectionTaskDO task = MesPqcInspectionTaskDO.builder()
                .id(6101L)
                .activeOrderId(328L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(1001L)
                .processId(201L)
                .regulationVersionId(7101L)
                .qaProcessId(7201L)
                .qaItemCode("QA-ITEM-001")
                .inspectionRuleKey("FIRST")
                .inspectionType("FIRST")
                .businessDate(LocalDate.of(2026, 9, 17))
                .shiftCode("DAY")
                .roundNo(1)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                .build();
        task.setTenantId(1L);
        return task;
    }

    private static MesProRouteVersionDO routeVersion() {
        return MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .routeSnapshotJson("""
                        {"configSnapshots":{
                          "flowGraph":{"nodes":[{"routeProcessId":1001,"processId":201}]},
                          "productionProcessConfigSchemaVersion":1,
                          "productionProcessConfigs":[{
                            "routeProcessId":1001,
                            "processId":201,
                            "overagePercent":0,
                            "lossReasons":[],
                            "deviceSelectionGroups":[],
                            "parameterRules":[]
                          }],
                          "batchUseConfigs":[{
                            "routeProcessId":1001,
                            "inputMaterialIds":[],
                            "outputMaterialIds":[]
                          }]
                        }}
                        """)
                .build();
    }

    private static MesProcessPoolActiveOrderPickListBindingDO sourceBinding(Long pickListId,
                                                                              String sourceFid,
                                                                              String sourceBillNo) {
        return MesProcessPoolActiveOrderPickListBindingDO.builder()
                .pickListId(pickListId).sourceFid(sourceFid).sourceBillNo(sourceBillNo).build();
    }

    private static ErpKingdeeProductionPickListDO formalPickList(Long id, String sourceFid, String sourceBillNo) {
        return ErpKingdeeProductionPickListDO.builder().id(id).sourceFormId("PRD_PickMtrl")
                .sourceFid(sourceFid).sourceBillNo(sourceBillNo).documentStatus("C").build();
    }

    private static ErpKingdeeProductionPickListItemDO formalPickListItem(Long id, Long pickListId) {
        return ErpKingdeeProductionPickListItemDO.builder().id(id).productionPickListId(pickListId)
                .sourceFid("FID-" + pickListId).sourceEntryId("1")
                .sourceLineKey("PICK-" + pickListId + "-LINE-1").sourceBillNo("PICK-" + pickListId)
                .materialNumber("MAT-" + pickListId).materialName("正式物料").requestedQuantity(BigDecimal.ONE)
                .actualQuantity(BigDecimal.ONE).baseActualQuantity(BigDecimal.ONE).lotNumber("LOT-" + pickListId)
                .productionOrderNo("WO-001").build();
    }
}
