package cn.iocoder.yudao.module.mes.service.pro.simulation.stage1;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
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
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
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
    private MesMdItemMapper itemMapper;
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
    void missingFormalPickListsFailsWithoutProductOrBomInference() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO simulatedTemplate = activeOrder(150L).setSimulated(Boolean.TRUE);
        when(bindingMapper.selectListByActiveOrderId(150L)).thenReturn(List.of());
        when(pickListItemMapper.selectListByProductionOrderNo("WO-001")).thenReturn(List.of());
        ServiceException exception = assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "resolveTemplateBindings",
                        simulatedTemplate, workOrder(), command(150L)));

        assertEquals(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED.getCode(), exception.getCode());
        verify(itemMapper, never()).selectById(any());
        verify(pickListMapper, never()).insert((ErpKingdeeProductionPickListDO) any());
        verify(pickListItemMapper, never()).insert((ErpKingdeeProductionPickListItemDO) any());
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
                .productionOrderNo("WO-001")
                .build();
        ErpKingdeeProductionPickListDO secondHeader = ErpKingdeeProductionPickListDO.builder()
                .id(9002L).sourceFormId("PRD_PickMtrl").sourceFid("FID-9002")
                .sourceBillNo("PICK-9002").documentStatus("C").build();
        ErpKingdeeProductionPickListItemDO secondItem = ErpKingdeeProductionPickListItemDO.builder()
                .id(9102L).productionPickListId(9002L).sourceFid("FID-9002")
                .sourceEntryId("1").sourceLineKey("PICK-9002-LINE-1").sourceBillNo("PICK-9002")
                .materialNumber("MAT-002").materialName("正式物料2").requestedQuantity(new BigDecimal("6"))
                .actualQuantity(new BigDecimal("6")).productionOrderNo("WO-001").build();
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
    void incompleteFormalPickListFailsTheEntireResolution() {
        TenantContextHolder.setTenantId(1L);
        MesProWorkOrderDO workOrder = workOrder();
        ErpKingdeeProductionPickListItemDO item = ErpKingdeeProductionPickListItemDO.builder()
                .id(9101L).productionPickListId(9001L).productionOrderNo("WO-001").build();
        when(pickListItemMapper.selectListByProductionOrderNo("WO-001")).thenReturn(List.of(item));
        when(pickListMapper.selectById(9001L)).thenReturn(null);

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

    private static MesStage1ActiveOrderCompleteSimulationCommand command(Long templateActiveOrderId) {
        return new MesStage1ActiveOrderCompleteSimulationCommand()
                .setSimulationRunId("STAGE1-unit")
                .setTemplateActiveOrderId(templateActiveOrderId)
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
                .actualQuantity(BigDecimal.ONE).build();
    }
}
