package cn.iocoder.yudao.module.mes.service.pro.simulation.stage1;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
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
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_PRODUCT_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
    void simulatedTemplateCanBeUsedForRerunAndReadsItsPersistedBinding() {
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
        when(bindingMapper.selectByActiveOrderId(328L)).thenReturn(persistedBinding);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "requireTemplate",
                simulatedTemplate, 3001L));
        MesProcessPoolActiveOrderPickListBindingDO resolved = ReflectionTestUtils.invokeMethod(
                service, "resolveTemplateBinding", simulatedTemplate, templateWorkOrder, command(328L));

        assertSame(persistedBinding, resolved);
        verify(pickListItemMapper, never()).selectListByProductionOrderNo(any());
    }

    @Test
    void simulatedTemplateWithoutPersistedBindingReturnsBusinessError() {
        TenantContextHolder.setTenantId(1L);
        MesProcessPoolActiveOrderDO simulatedTemplate = activeOrder(150L).setSimulated(Boolean.TRUE);
        when(bindingMapper.selectByActiveOrderId(150L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "resolveTemplateBinding",
                        simulatedTemplate, workOrder(), command(150L)));

        assertEquals(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("领料单来源"));
    }

    @Test
    void missingFormalPickListCreatesSyntheticPickListFromWorkOrderProduct() {
        TenantContextHolder.setTenantId(1L);
        MesProWorkOrderDO workOrder = workOrder();
        MesMdItemDO product = MesMdItemDO.builder()
                .id(1001L)
                .code("PRODUCT-001")
                .name("模拟产品")
                .specification("规格A")
                .build();
        when(pickListItemMapper.selectListByProductionOrderNo(workOrder.getCode())).thenReturn(List.of());
        when(itemMapper.selectById(workOrder.getProductId())).thenReturn(product);
        doAnswer(invocation -> {
            invocation.getArgument(0, ErpKingdeeProductionPickListDO.class).setId(8101L);
            return 1;
        }).when(pickListMapper).insert((ErpKingdeeProductionPickListDO) any());
        doAnswer(invocation -> {
            invocation.getArgument(0, ErpKingdeeProductionPickListItemDO.class).setId(8102L);
            return 1;
        }).when(pickListItemMapper).insert((ErpKingdeeProductionPickListItemDO) any());

        MesProcessPoolActiveOrderPickListBindingDO binding = ReflectionTestUtils.invokeMethod(
                service, "resolveTemplateBinding", activeOrder(328L), workOrder, command(328L));

        assertEquals(8101L, binding.getPickListId());
        assertEquals("BOUND", binding.getBindingStatus());
        assertTrue(Boolean.TRUE.equals(binding.getSimulated()));
        assertEquals("STAGE1", binding.getSimulationStage());
        assertTrue(binding.getSourceBillNo().startsWith("STAGE1-SYNTHETIC-"));
        assertTrue(binding.getSourceSnapshotHash() != null && !binding.getSourceSnapshotHash().isBlank());
        verify(pickListMapper).insert(ArgumentMatchers.<ErpKingdeeProductionPickListDO>argThat(
                (ErpKingdeeProductionPickListDO header) ->
                "PRD_PickMtrl".equals(header.getSourceFormId())
                        && header.getSourceFid().startsWith("STAGE1-SYNTHETIC-")
                        && header.getSourceBillNo().startsWith("STAGE1-SYNTHETIC-")
                        && "C".equals(header.getDocumentStatus())
                        && header.getRawPayload().contains("MES_STAGE1_SIMULATION_SYNTHETIC_PICK_LIST")));
        verify(pickListItemMapper).insert(ArgumentMatchers.<ErpKingdeeProductionPickListItemDO>argThat(
                (ErpKingdeeProductionPickListItemDO item) ->
                Long.valueOf(8101L).equals(item.getProductionPickListId())
                        && "PRODUCT-001".equals(item.getMaterialNumber())
                        && "模拟产品".equals(item.getMaterialName())
                        && workOrder.getQuantity().compareTo(item.getRequestedQuantity()) == 0
                        && workOrder.getQuantity().compareTo(item.getActualQuantity()) == 0
                        && workOrder.getCode().equals(item.getProductionOrderNo())
                        && item.getSourceLineKey().startsWith("STAGE1-SYNTHETIC-")));
    }

    @Test
    void formalPickListSourceRemainsPreferredOverSyntheticSource() {
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
        when(pickListItemMapper.selectListByProductionOrderNo("WO-001")).thenReturn(List.of(item));
        when(pickListMapper.selectById(9001L)).thenReturn(header);

        MesProcessPoolActiveOrderPickListBindingDO binding = ReflectionTestUtils.invokeMethod(
                service, "resolveTemplateBinding", activeOrder(328L), workOrder, command(328L));

        assertEquals(9001L, binding.getPickListId());
        assertEquals("PICK-9001", binding.getSourceBillNo());
        assertTrue(!Boolean.TRUE.equals(binding.getSimulated()));
        verify(itemMapper, never()).selectById(any());
        verify(pickListMapper, never()).insert((ErpKingdeeProductionPickListDO) any());
        verify(pickListItemMapper, never()).insert((ErpKingdeeProductionPickListItemDO) any());
    }

    @Test
    void missingFormalPickListAndProductMasterReturnsBusinessError() {
        TenantContextHolder.setTenantId(1L);
        MesProWorkOrderDO workOrder = workOrder();
        when(pickListItemMapper.selectListByProductionOrderNo("WO-001")).thenReturn(List.of());
        when(itemMapper.selectById(1001L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "resolveTemplateBinding",
                        activeOrder(328L), workOrder, command(328L)));

        assertEquals(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_PRODUCT_REQUIRED.getCode(),
                exception.getCode());
        verify(pickListMapper, never()).insert((ErpKingdeeProductionPickListDO) any());
        verify(pickListItemMapper, never()).insert((ErpKingdeeProductionPickListItemDO) any());
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
}
