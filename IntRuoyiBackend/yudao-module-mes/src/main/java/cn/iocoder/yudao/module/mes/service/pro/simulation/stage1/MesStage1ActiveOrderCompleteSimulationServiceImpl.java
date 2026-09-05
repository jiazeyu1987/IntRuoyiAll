package cn.iocoder.yudao.module.mes.service.pro.simulation.stage1;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.batch.MesWmBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
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
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyFieldMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
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
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmProductIssueStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFormalProductionPickListSourceResolver;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_STAGE1_SIMULATION_TEMPLATE_INVALID;

@Service
public class MesStage1ActiveOrderCompleteSimulationServiceImpl
        implements MesStage1ActiveOrderCompleteSimulationService {

    private static final String STAGE = "STAGE1";
    private static final String MARKER = "[STAGE1_SIMULATION]";
    private static final String ACTIVE = "ACTIVE";
    private static final String PLACEHOLDER_MATERIAL_CODE = "/";
    private static final String PQC_SOURCE = "MES_PQC_INSPECTION_TASK";
    private static final String PRODUCTION_SOURCE = "MES_PRO_FEEDBACK";
    private static final BigDecimal PERCENT_DIVISOR = BigDecimal.valueOf(100);
    private static final int PROGRESS_PERCENT_SCALE = 6;

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProWorkOrderService workOrderService;
    private final MesProWorkOrderBomMapper workOrderBomMapper;
    private final MesMdItemMapper itemMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    private final MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;
    private final ErpKingdeeProductionPickListMapper pickListMapper;
    private final ErpKingdeeProductionPickListItemMapper pickListItemMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    private final MesProcessPoolReviewCopyFieldMapper reviewCopyFieldMapper;
    private final MesProcessPoolReviewCopyMapper reviewCopyMapper;
    private final MesProProcessPoolEventRevisionDiffMapper eventRevisionDiffMapper;
    private final MesProProcessPoolEventRevisionMapper eventRevisionMapper;
    private final MesPqcInspectionPieceDetailMapper pieceMapper;
    private final MesProFeedbackMapper feedbackMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper aggregateMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolReportAllocationStateMapper allocationStateMapper;
    private final MesProcessPoolReportAllocationAdjustmentAuditMapper allocationAuditMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesProcessPoolActiveOrderCompletionBackfillMapper completionBackfillMapper;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesWmWarehouseMapper warehouseMapper;
    private final MesWmWarehouseLocationMapper warehouseLocationMapper;
    private final MesWmWarehouseAreaMapper warehouseAreaMapper;
    private final MesWmBatchMapper batchMapper;
    private final MesWmMaterialStockMapper materialStockMapper;
    private final MesWmProductIssueMapper productIssueMapper;
    private final MesWmProductIssueLineMapper productIssueLineMapper;
    private final MesWmProductIssueDetailMapper productIssueDetailMapper;
    private final MesTeamLeaderActiveOrderSimulationService activeOrderSimulationService;

    public MesStage1ActiveOrderCompleteSimulationServiceImpl(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesProWorkOrderService workOrderService,
            MesProWorkOrderBomMapper workOrderBomMapper,
            MesMdItemMapper itemMapper,
            MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper,
            MesPqcInspectionTaskMapper pqcTaskMapper,
            MesProcessPoolActiveOrderPickListBindingMapper bindingMapper,
            MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper,
            ErpKingdeeProductionPickListMapper pickListMapper,
            ErpKingdeeProductionPickListItemMapper pickListItemMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper,
            MesProcessPoolReviewCopyFieldMapper reviewCopyFieldMapper,
            MesProcessPoolReviewCopyMapper reviewCopyMapper,
            MesProProcessPoolEventRevisionDiffMapper eventRevisionDiffMapper,
            MesProProcessPoolEventRevisionMapper eventRevisionMapper,
            MesPqcInspectionPieceDetailMapper pieceMapper,
            MesProFeedbackMapper feedbackMapper,
            MesPqcProcessInspectionAggregateDetailMapper aggregateMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolReportAllocationStateMapper allocationStateMapper,
            MesProcessPoolReportAllocationAdjustmentAuditMapper allocationAuditMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProcessPoolOrderProcessCompletionMapper completionMapper,
            MesProcessPoolActiveOrderCompletionBackfillMapper completionBackfillMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesWmWarehouseMapper warehouseMapper,
            MesWmWarehouseLocationMapper warehouseLocationMapper,
            MesWmWarehouseAreaMapper warehouseAreaMapper,
            MesWmBatchMapper batchMapper,
            MesWmMaterialStockMapper materialStockMapper,
            MesWmProductIssueMapper productIssueMapper,
            MesWmProductIssueLineMapper productIssueLineMapper,
            MesWmProductIssueDetailMapper productIssueDetailMapper,
            MesTeamLeaderActiveOrderSimulationService activeOrderSimulationService) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.workOrderService = workOrderService;
        this.workOrderBomMapper = workOrderBomMapper;
        this.itemMapper = itemMapper;
        this.snapshotMapper = snapshotMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.bindingMapper = bindingMapper;
        this.bindingItemMapper = bindingItemMapper;
        this.pickListMapper = pickListMapper;
        this.pickListItemMapper = pickListItemMapper;
        this.eventMapper = eventMapper;
        this.pqcRecordMapper = pqcRecordMapper;
        this.quantityFragmentMapper = quantityFragmentMapper;
        this.reviewCopyFieldMapper = reviewCopyFieldMapper;
        this.reviewCopyMapper = reviewCopyMapper;
        this.eventRevisionDiffMapper = eventRevisionDiffMapper;
        this.eventRevisionMapper = eventRevisionMapper;
        this.pieceMapper = pieceMapper;
        this.feedbackMapper = feedbackMapper;
        this.aggregateMapper = aggregateMapper;
        this.allocationMapper = allocationMapper;
        this.allocationStateMapper = allocationStateMapper;
        this.allocationAuditMapper = allocationAuditMapper;
        this.reviewMapper = reviewMapper;
        this.completionMapper = completionMapper;
        this.completionBackfillMapper = completionBackfillMapper;
        this.completionReceiptMapper = completionReceiptMapper;
        this.releaseApplicationMapper = releaseApplicationMapper;
        this.batchExecutionMapper = batchExecutionMapper;
        this.warehouseMapper = warehouseMapper;
        this.warehouseLocationMapper = warehouseLocationMapper;
        this.warehouseAreaMapper = warehouseAreaMapper;
        this.batchMapper = batchMapper;
        this.materialStockMapper = materialStockMapper;
        this.productIssueMapper = productIssueMapper;
        this.productIssueLineMapper = productIssueLineMapper;
        this.productIssueDetailMapper = productIssueDetailMapper;
        this.activeOrderSimulationService = activeOrderSimulationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesStage1ActiveOrderCompleteSimulationResult simulate(
            MesStage1ActiveOrderCompleteSimulationCommand command) {
        MesStage1ActiveOrderCompleteSimulationCommand validated =
                MesStage1ActiveOrderCompleteSimulationCommand.validate(
                        command == null ? null : command.getSimulationRunId(),
                        command == null ? null : command.getTemplateActiveOrderId(),
                        command == null ? null : command.getActorUserId());
        requireTenant();
        MesProcessPoolActiveOrderDO template = activeOrderMapper
                .selectByIdForUpdate(validated.getTemplateActiveOrderId());
        requireTemplate(template, validated.getActorUserId());
        MesProWorkOrderDO templateWorkOrder = requireWorkOrder(template.getWorkOrderId());
        List<MesProcessPoolActiveOrderPickListBindingDO> templateBindings = resolveTemplateBindings(template,
                templateWorkOrder, validated);
        MesProcessPoolActiveOrderDO fixture = createFixture(template, templateWorkOrder,
                templateBindings, validated);

        MesTeamLeaderActiveOrderSimulationResult simulation = activeOrderSimulationService
                .simulateActiveOrderCompletion(validated.getActorUserId(), fixture.getId(), STAGE,
                        validated.getSimulationRunId());
        if (simulation == null || simulation.getProductionProgressPercent() == null
                || simulation.getInspectionProgressPercent() == null
                || simulation.getProductionProgressPercent().compareTo(BigDecimal.valueOf(100)) != 0
                || simulation.getInspectionProgressPercent().compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalStateException("STAGE1_DOUBLE_100_REQUIRED");
        }
        verifyPersistedSimulationFacts(fixture, validated.getSimulationRunId());
        assertFormalOrderProcessCompletionFacts(fixture, validated.getSimulationRunId());
        assertNoDownstreamSideEffects(fixture);
        Stage1Progress persistedProgress = calculateStage1PersistedProgress(fixture);
        if (!persistedProgress.complete()) {
            throw new IllegalStateException("STAGE1_PERSISTED_PROGRESS_NOT_100");
        }
        String cleanedRunId = cleanupOwnedRuns(validated.getActorUserId(), validated.getSimulationRunId());
        List<MesProcessPoolActiveOrderPickListBindingDO> fixtureBindings = requireBindings(fixture);
        Map<String, Object> snapshot = buildSnapshot(fixture, templateBindings, fixtureBindings, validated, simulation,
                persistedProgress);
        return new MesStage1ActiveOrderCompleteSimulationResult()
                .setSimulationRunId(validated.getSimulationRunId())
                .setCleanedSimulationRunId(cleanedRunId)
                .setActiveOrderId(fixture.getId())
                .setWorkOrderId(fixture.getWorkOrderId())
                .setPickListId(fixtureBindings.get(0).getPickListId())
                .setPickListIds(fixtureBindings.stream()
                        .map(MesProcessPoolActiveOrderPickListBindingDO::getPickListId).toList())
                .setProductionSubmitCount(simulation.getProductionSubmitCount())
                .setProductionReviewCount(simulation.getProductionReviewCount())
                .setPqcSubmitCount(simulation.getPqcSubmitCount())
                .setPqcReviewCount(simulation.getPqcReviewCount())
                .setProductionProgressPercent(persistedProgress.productionProgressPercent())
                .setInspectionProgressPercent(persistedProgress.inspectionProgressPercent())
                .setProductionProgress100(persistedProgress.productionProgress100())
                .setInspectionProgress100(persistedProgress.inspectionProgress100())
                .setCompletionButtonEnabled(true)
                .setActiveOrderCompleteSnapshot(snapshot);
    }

    private MesProcessPoolActiveOrderDO createFixture(
            MesProcessPoolActiveOrderDO template,
            MesProWorkOrderDO templateWorkOrder,
            List<MesProcessPoolActiveOrderPickListBindingDO> templateBindings,
            MesStage1ActiveOrderCompleteSimulationCommand command) {
        MesProWorkOrderDO workOrder = createWorkOrder(templateWorkOrder, command.getSimulationRunId(),
                command.getActorUserId(), template.getId());
        MesProcessPoolActiveOrderDO activeOrder = BeanUtils.toBean(template, MesProcessPoolActiveOrderDO.class)
                .setId(null)
                .setLeaderUserId(command.getActorUserId())
                .setWorkOrderId(workOrder.getId())
                .setActiveStatus(ACTIVE)
                .setBusinessStatus(ACTIVE)
                .setJoinedAt(LocalDateTime.now())
                .setSortOrder(System.currentTimeMillis())
                .setRemovedAt(null)
                .setReleaseDecisionId(null)
                .setReleasedBy(null)
                .setReleasedAt(null)
                .setVersion(0)
                .setSimulated(Boolean.TRUE)
                .setSimulationStage(STAGE)
                .setSimulationRunId(command.getSimulationRunId());
        activeOrderMapper.insert(activeOrder);
        cloneSnapshots(template.getId(), activeOrder, command.getSimulationRunId());
        clonePqcTasks(template.getId(), activeOrder, command.getSimulationRunId());
        for (MesProcessPoolActiveOrderPickListBindingDO templateBinding : templateBindings) {
            ErpKingdeeProductionPickListDO pickList = clonePickList(templateBinding, workOrder.getCode(),
                    command.getSimulationRunId(), command.getActorUserId());
            cloneBinding(templateBinding, pickList, activeOrder, workOrder, command);
        }
        createFormalProductIssue(activeOrder, workOrder, command);
        return activeOrder;
    }

    private MesProWorkOrderDO createWorkOrder(MesProWorkOrderDO template, String runId, Long actorUserId,
                                              Long sourceActiveOrderId) {
        String safe = shortRunId(runId);
        MesProWorkOrderSaveReqVO request = new MesProWorkOrderSaveReqVO()
                .setCode("STAGE1-WO-" + safe)
                .setName(template.getName() + " Stage1模拟")
                .setType(MesProWorkOrderTypeEnum.SELF.getType())
                .setOrderSourceType(template.getOrderSourceType())
                .setOrderSourceCode("STAGE1:" + safe)
                .setProductId(template.getProductId())
                .setQuantity(template.getQuantity())
                .setQuantityProduced(BigDecimal.ZERO)
                .setQuantityChanged(BigDecimal.ZERO)
                .setQuantityScheduled(BigDecimal.ZERO)
                .setClientId(template.getClientId())
                .setVendorId(template.getVendorId())
                .setBatchCode("STAGE1-BATCH-" + safe)
                .setRequestDate(LocalDateTime.now())
                .setParentId(MesProWorkOrderDO.PARENT_ID_NULL)
                .setRemark(marker(runId, actorUserId, sourceActiveOrderId));
        Long id = workOrderService.createWorkOrder(request);
        workOrderService.confirmWorkOrder(id);
        return requireWorkOrder(id);
    }

    private void cloneSnapshots(Long templateActiveOrderId, MesProcessPoolActiveOrderDO target,
                                String runId) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> source = snapshotMapper
                .selectListByActiveOrderIdForUpdate(templateActiveOrderId);
        if (source == null || source.isEmpty()) {
            throw new IllegalStateException("STAGE1_PROCESS_SNAPSHOT_REQUIRED");
        }
        for (MesProcessPoolActiveOrderProcessSnapshotDO row : source) {
            if (row.getRouteProcessId() == null || row.getProcessId() == null
                    || row.getPlannedQuantitySnapshot() == null
                    || row.getPlannedQuantitySnapshot().signum() <= 0) {
                throw new IllegalStateException("STAGE1_PROCESS_SNAPSHOT_INVALID");
            }
            snapshotMapper.insert(BeanUtils.toBean(row, MesProcessPoolActiveOrderProcessSnapshotDO.class)
                    .setId(null)
                    .setActiveOrderId(target.getId())
                    .setWorkOrderId(target.getWorkOrderId())
                    .setSimulated(Boolean.TRUE)
                    .setSimulationStage(STAGE)
                    .setSimulationRunId(runId));
        }
    }

    private void clonePqcTasks(Long templateActiveOrderId, MesProcessPoolActiveOrderDO target, String runId) {
        List<MesPqcInspectionTaskDO> source = pqcTaskMapper
                .selectListByActiveOrderIdForUpdate(templateActiveOrderId);
        if (source == null || source.isEmpty()) {
            throw new IllegalStateException("STAGE1_PQC_TASK_REQUIRED");
        }
        for (MesPqcInspectionTaskDO row : source) {
            pqcTaskMapper.insert(BeanUtils.toBean(row, MesPqcInspectionTaskDO.class)
                    .setId(null)
                    .setActiveOrderId(target.getId())
                    .setWorkOrderId(target.getWorkOrderId())
                    .setTaskStatus(MesPqcInspectionTaskDO.TASK_STATUS_PENDING)
                    .setActualInspectionQuantity(null)
                    .setSubmittedEventId(null)
                    .setSubmittedContentHash(null)
                    .setSimulated(Boolean.TRUE)
                    .setSimulationStage(STAGE)
                    .setSimulationRunId(runId));
        }
    }

    private List<MesProcessPoolActiveOrderPickListBindingDO> resolveTemplateBindings(
            MesProcessPoolActiveOrderDO activeOrder, MesProWorkOrderDO workOrder,
            MesStage1ActiveOrderCompleteSimulationCommand command) {
        if (Boolean.TRUE.equals(activeOrder.getSimulated())) {
            List<MesProcessPoolActiveOrderPickListBindingDO> bindings = bindingMapper
                    .selectListByActiveOrderId(activeOrder.getId());
            if (bindings != null && !bindings.isEmpty()) {
                for (MesProcessPoolActiveOrderPickListBindingDO binding : bindings) {
                    requireBinding(binding, activeOrder, workOrder);
                }
                return bindings;
            }
        }
        return createSimulationPickLists(workOrder, command);
    }

    private List<MesProcessPoolActiveOrderPickListBindingDO> createSimulationPickLists(
            MesProWorkOrderDO workOrder, MesStage1ActiveOrderCompleteSimulationCommand command) {
        if (blank(workOrder.getCode())) {
            throw exception(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED);
        }
        List<ErpKingdeeProductionPickListItemDO> sourceItems = pickListItemMapper
                .selectListByProductionOrderNo(workOrder.getCode());
        if (sourceItems == null || sourceItems.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED);
        }
        List<Long> pickListIds = sourceItems.stream()
                .map(ErpKingdeeProductionPickListItemDO::getProductionPickListId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (pickListIds.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED);
        }
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = new ArrayList<>();
        for (Long pickListId : pickListIds) {
            ErpKingdeeProductionPickListDO header = pickListMapper.selectById(pickListId);
            List<ErpKingdeeProductionPickListItemDO> items = sourceItems.stream()
                    .filter(item -> Objects.equals(pickListId, item.getProductionPickListId()))
                    .toList();
            if (header == null || blank(header.getSourceFid()) || blank(header.getSourceBillNo())
                    || blank(header.getDocumentStatus()) || items == null || items.isEmpty()) {
                throw exception(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED);
            }
            String snapshotHash = hash(items);
            MesProcessPoolActiveOrderPickListBindingDO binding = MesProcessPoolActiveOrderPickListBindingDO.builder()
                    .pickListId(header.getId()).sourceFid(header.getSourceFid()).sourceBillNo(header.getSourceBillNo())
                    .sourceDocumentStatus(header.getDocumentStatus()).sourceModifyTime(header.getSourceModifyTime())
                    .sourceSnapshotHash(snapshotHash).bindingStatus("BOUND").bindingVersion(1)
                    .requestPayloadHash(hash(workOrder.getCode() + "|" + pickListId + "|" + snapshotHash))
                    .simulated(Boolean.TRUE).simulationStage(STAGE).simulationRunId(command.getSimulationRunId()).build();
            binding.setTenantId(TenantContextHolder.getRequiredTenantId());
            bindings.add(binding);
        }
        return bindings;
    }

    private ErpKingdeeProductionPickListDO clonePickList(
            MesProcessPoolActiveOrderPickListBindingDO source, String workOrderCode,
            String runId, Long actorUserId) {
        ErpKingdeeProductionPickListDO header = pickListMapper.selectById(source.getPickListId());
        List<ErpKingdeeProductionPickListItemDO> items = pickListItemMapper
                .selectListByPickListIds(List.of(source.getPickListId()));
        if (header == null || items == null || items.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED);
        }
        String safe = shortRunId(runId);
        String copiedSourcePrefix = "STAGE1-" + safe + "-PL-" + source.getPickListId();
        String copiedSourceFid = copiedSourcePrefix + "-FID";
        String copiedSourceBillNo = "STAGE1-PL-" + safe + "-" + source.getPickListId();
        ErpKingdeeProductionPickListDO copy = BeanUtils.toBean(header, ErpKingdeeProductionPickListDO.class)
                .setId(null)
                .setSourceFid(copiedSourceFid)
                .setSourceBillNo(copiedSourceBillNo)
                .setDocumentStatus("C")
                .setDescription("Stage1正式领料模拟")
                .setLastSyncTime(LocalDateTime.now())
                .setRawPayload(JsonUtils.toJsonString(Map.of(
                        "simulated", true, "simulationStage", STAGE,
                        "simulationRunId", runId, "source", "MES_STAGE1_SIMULATION_FIXTURE",
                        "formalPickListId", String.valueOf(source.getPickListId()),
                        "formalSourceFid", source.getSourceFid(),
                        "formalSourceBillNo", source.getSourceBillNo())));
        pickListMapper.insert(copy);
        for (ErpKingdeeProductionPickListItemDO item : items) {
            pickListItemMapper.insert(BeanUtils.toBean(item, ErpKingdeeProductionPickListItemDO.class)
                    .setId(null)
                    .setProductionPickListId(copy.getId())
                    .setSourceFid(copiedSourceFid)
                    .setSourceEntryId("STAGE1-" + safe + "-ENTRY-" + item.getId())
                    .setSourceLineKey(copiedSourcePrefix + "-LINE-" + item.getId())
                    .setSourceBillNo(copiedSourceBillNo)
                    .setProductionOrderNo(workOrderCode)
                    .setRawPayload(JsonUtils.toJsonString(Map.of(
                            "simulated", true, "simulationStage", STAGE, "simulationRunId", runId,
                            "source", "MES_STAGE1_SIMULATION_FIXTURE",
                            "formalPickListId", String.valueOf(source.getPickListId()),
                            "formalSourceFid", item.getSourceFid(),
                            "formalSourceEntryId", item.getSourceEntryId(),
                            "formalSourceLineKey", item.getSourceLineKey(),
                            "formalSourceBillNo", item.getSourceBillNo())))
                    .setLastSyncTime(LocalDateTime.now()));
        }
        return copy;
    }

    private void cloneBinding(MesProcessPoolActiveOrderPickListBindingDO source,
                              ErpKingdeeProductionPickListDO header,
                              MesProcessPoolActiveOrderDO target, MesProWorkOrderDO workOrder,
                              MesStage1ActiveOrderCompleteSimulationCommand command) {
        if (header == null || header.getId() == null) {
            throw new IllegalStateException("STAGE1_PICK_LIST_CLONE_INVALID");
        }
        List<ErpKingdeeProductionPickListItemDO> items = pickListItemMapper
                .selectListByPickListIds(List.of(header.getId()));
        if (items == null || items.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED);
        }
        if (bindingMapper.selectByActiveOrderIdAndPickListId(target.getId(), header.getId()) != null) {
            throw new IllegalStateException("STAGE1_PICK_LIST_BINDING_DUPLICATE");
        }
        MesProcessPoolActiveOrderPickListBindingDO binding = BeanUtils.toBean(source,
                        MesProcessPoolActiveOrderPickListBindingDO.class)
                .setId(IdUtil.getSnowflake().nextId())
                .setActiveOrderId(target.getId())
                .setWorkOrderId(workOrder.getId())
                .setPickListId(header.getId())
                .setSourceFid(source.getSourceFid())
                .setSourceBillNo(source.getSourceBillNo())
                .setSourceDocumentStatus(source.getSourceDocumentStatus())
                .setSourceSnapshotHash(MesFormalProductionPickListSourceResolver
                        .snapshotHash(header, items))
                .setBoundBy(command.getActorUserId())
                .setBoundAt(LocalDateTime.now())
                .setIdempotencyKey("STAGE1-" + command.getSimulationRunId() + "-" + source.getPickListId())
                .setBindingVersion(1)
                .setSimulated(Boolean.TRUE)
                .setSimulationStage(STAGE)
                .setSimulationRunId(command.getSimulationRunId());
        bindingMapper.insert(binding);
        for (ErpKingdeeProductionPickListItemDO item : items) {
            bindingItemMapper.insert(MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                    .id(IdUtil.getSnowflake().nextId())
                    .bindingId(binding.getId())
                    .pickListItemId(item.getId())
                    .sourceEntryId(item.getSourceEntryId())
                    .sourceLineKey(item.getSourceLineKey())
                    .materialNumber(item.getMaterialNumber())
                    .materialName(item.getMaterialName())
                    .materialSpecification(item.getMaterialSpecification())
                    .unitName(item.getUnitName())
                    .requestedQuantity(item.getRequestedQuantity())
                    .actualQuantity(item.getActualQuantity())
                    .baseActualQuantity(item.getBaseActualQuantity())
                    .lotNumber(item.getLotNumber())
                    .productionOrderNo(item.getProductionOrderNo())
                    .productionOrderLineNo(item.getProductionOrderLineNo())
                    .sourceModifyTime(item.getSourceModifyTime())
                    .itemSnapshotHash(hash(item))
                    .simulated(Boolean.TRUE)
                    .simulationStage(STAGE)
                    .simulationRunId(command.getSimulationRunId())
                    .build());
        }
    }

    private void verifyPersistedSimulationFacts(MesProcessPoolActiveOrderDO activeOrder, String runId) {
        requireMarker(activeOrder, runId, "activeOrder");
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshotMapper
                .selectListByActiveOrderIdForUpdate(activeOrder.getId())) {
            requireMarker(snapshot, runId, "processSnapshot");
        }
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = requireBindings(activeOrder);
        for (MesProcessPoolActiveOrderPickListBindingDO binding : bindings) {
            requireMarker(binding, runId, "pickListBinding");
            List<MesProcessPoolActiveOrderPickListBindingItemDO> items = bindingItemMapper
                    .selectListByBindingId(binding.getId());
            if (items == null || items.isEmpty()) {
                throw new IllegalStateException("STAGE1_PICK_LIST_BINDING_ITEM_REQUIRED");
            }
            for (MesProcessPoolActiveOrderPickListBindingItemDO item : items) {
                requireMarker(item, runId, "pickListBindingItem");
            }
        }
        validateFormalProductIssue(activeOrder, runId, activeOrder.getLeaderUserId());
        List<MesProProcessPoolEventDO> productionEvents = eventMapper
                .selectProductionSubmitsByWorkOrderAndRouteForUpdate(activeOrder.getWorkOrderId(),
                        activeOrder.getRouteId());
        List<Long> eventIds = new ArrayList<>();
        for (MesProProcessPoolEventDO event : productionEvents) {
            requireMarker(event, runId);
            validateFormalProductionFeedback(activeOrder, event, runId);
            eventIds.add(event.getId());
            for (MesProProcessPoolQuantityFragmentDO fragment : quantityFragmentMapper
                    .selectListByEventId(event.getId())) {
                requireMarker(fragment, runId, "quantityFragment");
            }
            for (MesProcessPoolSubmissionReviewDO review : reviewMapper.selectListByEventId(event.getId())) {
                requireMarker(review, runId, "productionReview");
            }
            for (MesProcessPoolReportAllocationDO allocation : allocationMapper
                    .selectAllListByEventIdForUpdate(event.getId())) {
                requireMarker(allocation, runId, "productionAllocation");
            }
        }
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper
                .selectListByActiveOrderIdForUpdate(activeOrder.getId());
        if (tasks.isEmpty()) {
            throw new IllegalStateException("STAGE1_PQC_TASK_REQUIRED");
        }
        for (MesPqcInspectionTaskDO task : tasks) {
            requireMarker(task, runId, "pqcTask");
            List<MesProProcessPoolEventDO> pqcEvents = eventMapper.selectListPqcByTaskId(PQC_SOURCE, task.getId());
            if (pqcEvents.isEmpty()) {
                throw new IllegalStateException("STAGE1_PQC_EVENT_MISSING");
            }
            for (MesProProcessPoolEventDO event : pqcEvents) {
                requireMarker(event, runId);
                eventIds.add(event.getId());
                requireMarker(pqcRecordMapper.selectByEventId(event.getId()), runId, "pqcRecord");
                for (MesProcessPoolSubmissionReviewDO review : reviewMapper.selectListByEventId(event.getId())) {
                    requireMarker(review, runId, "pqcReview");
                }
            }
            for (MesPqcInspectionPieceDetailDO piece : pieceMapper.selectListByTaskId(task.getId())) {
                requireMarker(piece, runId, "pqcPiece");
            }
        }
        for (var aggregate : aggregateMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId())) {
            requireMarker(aggregate, runId, "pqcAggregate");
        }
        if (eventIds.isEmpty()) {
            throw new IllegalStateException("STAGE1_PRODUCTION_EVENT_MISSING");
        }
    }

    private void validateFormalProductionFeedback(MesProcessPoolActiveOrderDO activeOrder,
                                                  MesProProcessPoolEventDO event,
                                                  String runId) {
        if (!PRODUCTION_SOURCE.equals(event.getFeedbackSourceType()) || event.getFeedbackSourceId() == null) {
            throw new IllegalStateException("STAGE1_FORMAL_PRODUCTION_FEEDBACK_REQUIRED");
        }
        MesProFeedbackDO feedback = feedbackMapper.selectById(event.getFeedbackSourceId());
        String marker = marker(runId, activeOrder.getLeaderUserId());
        if (feedback == null || feedback.getId() == null
                || !Objects.equals(event.getFeedbackSourceId(), feedback.getId())
                || !Objects.equals(activeOrder.getWorkOrderId(), feedback.getWorkOrderId())
                || !Objects.equals(activeOrder.getRouteId(), feedback.getRouteId())
                || !Objects.equals(event.getProcessId(), feedback.getProcessId())
                || !Objects.equals(activeOrder.getLeaderUserId(), feedback.getFeedbackUserId())
                || !Objects.equals(activeOrder.getLeaderUserId(), feedback.getApproveUserId())
                || !Objects.equals(MesProFeedbackStatusEnum.FINISHED.getStatus(), feedback.getStatus())
                || feedback.getFeedbackQuantity() == null || feedback.getFeedbackQuantity().signum() <= 0
                || feedback.getQualifiedQuantity() == null || feedback.getQualifiedQuantity().signum() <= 0
                || !zero(feedback.getUnqualifiedQuantity()) || !zero(feedback.getUncheckQuantity())
                || !zero(feedback.getLaborScrapQuantity()) || !zero(feedback.getMaterialScrapQuantity())
                || !zero(feedback.getOtherScrapQuantity())
                || !Objects.equals(marker, feedback.getRemark())) {
            throw new IllegalStateException("STAGE1_FORMAL_PRODUCTION_FEEDBACK_INVALID");
        }
    }

    private void assertNoDownstreamSideEffects(MesProcessPoolActiveOrderDO activeOrder) {
        if (!ACTIVE.equals(activeOrder.getActiveStatus()) || !ACTIVE.equals(activeOrder.getBusinessStatus())
                || activeOrder.getReleaseDecisionId() != null || activeOrder.getReleasedBy() != null
                || activeOrder.getReleasedAt() != null) {
            throw new IllegalStateException("STAGE1_ACTIVE_ORDER_LEFT_COMPLETE_NODE");
        }
        if (!completionBackfillMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId()).isEmpty()) {
            throw new IllegalStateException("STAGE1_BACKFILL_SIDE_EFFECT");
        }
        if (completionReceiptMapper.selectByActiveOrderIdForUpdate(activeOrder.getId()) != null) {
            throw new IllegalStateException("STAGE1_COMPLETION_RECEIPT_SIDE_EFFECT");
        }
        if (!releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(activeOrder.getId())).isEmpty()) {
            throw new IllegalStateException("STAGE1_RELEASE_SIDE_EFFECT");
        }
        List<MesProEdhrBatchExecutionDO> batches = batchExecutionMapper.selectList(
                new LambdaQueryWrapper<MesProEdhrBatchExecutionDO>()
                        .eq(MesProEdhrBatchExecutionDO::getTenantId, TenantContextHolder.getTenantId())
                        .eq(MesProEdhrBatchExecutionDO::getWorkOrderId, activeOrder.getWorkOrderId()));
        if (!batches.isEmpty()) {
            throw new IllegalStateException("STAGE1_BATCH_EXECUTION_SIDE_EFFECT");
        }
    }

    private Map<String, Object> buildSnapshot(MesProcessPoolActiveOrderDO activeOrder,
                                              List<MesProcessPoolActiveOrderPickListBindingDO> templateBindings,
                                              List<MesProcessPoolActiveOrderPickListBindingDO> bindings,
                                              MesStage1ActiveOrderCompleteSimulationCommand command,
                                              MesTeamLeaderActiveOrderSimulationResult simulation,
                                              Stage1Progress persistedProgress) {
        MesProWorkOrderDO workOrder = requireWorkOrder(activeOrder.getWorkOrderId());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> processSnapshots = snapshotMapper
                .selectListByActiveOrderIdForUpdate(activeOrder.getId());
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper.selectListByActiveOrderId(activeOrder.getId());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schemaVersion", "activeOrderCompleteSnapshot.v2");
        snapshot.put("simulationRunId", command.getSimulationRunId());
        snapshot.put("simulationStage", STAGE);
        snapshot.put("simulated", true);
        snapshot.put("formalSource", "MES_ACTIVE_ORDER_SIMULATION_FORMAL_FACTS");
        snapshot.put("activeOrderId", String.valueOf(activeOrder.getId()));
        snapshot.put("workOrderId", String.valueOf(workOrder.getId()));
        snapshot.put("workOrderCode", workOrder.getCode());
        snapshot.put("pickListSources", bindings.stream().map(binding -> Map.of(
                "bindingId", String.valueOf(binding.getId()),
                "pickListId", String.valueOf(binding.getPickListId()),
                "sourceFid", binding.getSourceFid(),
                "sourceBillNo", binding.getSourceBillNo(),
                "sourceSnapshotHash", binding.getSourceSnapshotHash(),
                "templateSourceSnapshotHash", templateBindings.stream()
                        .filter(templateBinding -> Objects.equals(templateBinding.getSourceFid(), binding.getSourceFid())
                                && Objects.equals(templateBinding.getSourceBillNo(), binding.getSourceBillNo()))
                        .map(MesProcessPoolActiveOrderPickListBindingDO::getSourceSnapshotHash)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("STAGE1_PICK_LIST_SOURCE_TRACE_REQUIRED"))
        )).toList());
        snapshot.put("routeProcessSnapshot", processSnapshots.stream().map(item -> Map.of(
                "routeProcessId", item.getRouteProcessId(),
                "processId", item.getProcessId(),
                "plannedQuantity", item.getPlannedQuantitySnapshot())).toList());
        snapshot.put("productionFacts", Map.of(
                "submitCount", simulation.getProductionSubmitCount(),
                "reviewCount", simulation.getProductionReviewCount(),
                "source", "formal production submit + production leader review"));
        snapshot.put("pqcFacts", Map.of(
                "submitCount", simulation.getPqcSubmitCount(),
                "reviewCount", simulation.getPqcReviewCount(),
                "pieceData", true,
                "source", "formal frontline PQC submit + PQC leader review"));
        snapshot.put("progress", Map.of(
                "productionPercent", persistedProgress.productionProgressPercent(),
                "inspectionPercent", persistedProgress.inspectionProgressPercent(),
                "completionButtonEnabled", persistedProgress.complete()));
        snapshot.put("downstreamSideEffects", Map.of(
                "completion", false, "backfill", false, "batchExecution", false,
                "fileUpload", false, "release", false));
        snapshot.put("stage2InputContract", "activeOrderCompleteSnapshot.v2");
        snapshot.put("independentFixture", true);
        return snapshot;
    }

    private Stage1Progress calculateStage1PersistedProgress(MesProcessPoolActiveOrderDO activeOrder) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> processSnapshots = snapshotMapper
                .selectListByActiveOrderIdForUpdate(activeOrder.getId());
        if (processSnapshots == null || processSnapshots.isEmpty()) {
            throw new IllegalStateException("STAGE1_PROCESS_SNAPSHOT_REQUIRED");
        }
        Map<String, BigDecimal> targetQuantityByProcess = new LinkedHashMap<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : processSnapshots) {
            if (!Objects.equals(activeOrder.getWorkOrderId(), snapshot.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), snapshot.getRouteId())
                    || !Objects.equals(activeOrder.getRouteVersionId(), snapshot.getRouteVersionId())
                    || snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null
                    || snapshot.getPlannedQuantitySnapshot() == null
                    || snapshot.getPlannedQuantitySnapshot().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("STAGE1_PROCESS_SNAPSHOT_INVALID");
            }
            String key = processKey(snapshot.getRouteProcessId(), snapshot.getProcessId());
            if (targetQuantityByProcess.put(key, snapshot.getPlannedQuantitySnapshot()) != null) {
                throw new IllegalStateException("STAGE1_PROCESS_SNAPSHOT_INVALID");
            }
        }
        Map<String, BigDecimal> allocatedQuantityByProcess = new LinkedHashMap<>();
        for (MesProcessPoolReportAllocationDO allocation : allocationMapper
                .selectListByActiveOrderIds(List.of(activeOrder.getId()))) {
            if (allocation.getRouteProcessId() == null || allocation.getProcessId() == null
                    || allocation.getAllocatedQuantity() == null) {
                throw new IllegalStateException("STAGE1_PRODUCTION_PROGRESS_FACT_INVALID");
            }
            String key = processKey(allocation.getRouteProcessId(), allocation.getProcessId());
            if (!targetQuantityByProcess.containsKey(key)) {
                throw new IllegalStateException("STAGE1_PRODUCTION_PROGRESS_FACT_INVALID");
            }
            allocatedQuantityByProcess.merge(key, allocation.getAllocatedQuantity(), BigDecimal::add);
        }
        long completedProductionProcessCount = targetQuantityByProcess.entrySet().stream()
                .filter(entry -> allocatedQuantityByProcess.getOrDefault(entry.getKey(), BigDecimal.ZERO)
                        .compareTo(entry.getValue()) >= 0)
                .count();
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper.selectListByActiveOrderId(activeOrder.getId());
        BigDecimal inspectionProgressPercent;
        if (tasks == null || tasks.isEmpty()) {
            inspectionProgressPercent = zeroProgressPercent();
        } else {
            long confirmedTaskCount = tasks.stream()
                    .filter(task -> MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus()))
                    .count();
            inspectionProgressPercent = toProgressPercent(confirmedTaskCount, tasks.size());
        }
        return new Stage1Progress(
                toProgressPercent(completedProductionProcessCount, targetQuantityByProcess.size()),
                inspectionProgressPercent);
    }

    private void requireTemplate(MesProcessPoolActiveOrderDO activeOrder, Long actorUserId) {
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), actorUserId)
                || !Objects.equals(activeOrder.getTenantId(), TenantContextHolder.getTenantId())
                || !ACTIVE.equals(activeOrder.getActiveStatus()) || activeOrder.getWorkOrderId() == null
                || activeOrder.getRouteId() == null || activeOrder.getRouteVersionId() == null) {
            throw exception(PRO_PROCESS_POOL_STAGE1_SIMULATION_TEMPLATE_INVALID);
        }
    }

    private MesProWorkOrderDO requireWorkOrder(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null || workOrder.getProductId() == null || workOrder.getQuantity() == null
                || workOrder.getQuantity().signum() <= 0 || blank(workOrder.getCode())) {
            throw new IllegalStateException("STAGE1_WORK_ORDER_INVALID");
        }
        return workOrder;
    }

    private void requireBinding(MesProcessPoolActiveOrderPickListBindingDO binding,
                                MesProcessPoolActiveOrderDO activeOrder,
                                MesProWorkOrderDO workOrder) {
        if (binding == null || binding.getPickListId() == null || blank(binding.getSourceSnapshotHash())
                || !Objects.equals(activeOrder.getId(), binding.getActiveOrderId())
                || !Objects.equals(activeOrder.getWorkOrderId(), binding.getWorkOrderId())
                || !Objects.equals(workOrder.getId(), binding.getWorkOrderId())
                || !Objects.equals(activeOrder.getTenantId(), binding.getTenantId())
                || !Objects.equals(activeOrder.getTenantId(), workOrder.getTenantId())) {
            throw exception(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED);
        }
    }

    private List<MesProcessPoolActiveOrderPickListBindingDO> requireBindings(
            MesProcessPoolActiveOrderDO activeOrder) {
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = bindingMapper
                .selectListByActiveOrderId(activeOrder.getId());
        if (bindings == null || bindings.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_STAGE1_SIMULATION_PICK_LIST_SOURCE_REQUIRED);
        }
        return bindings;
    }

    private String cleanupOwnedRuns(Long actorUserId, String excludedRunId) {
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectList(new LambdaQueryWrapper<MesProWorkOrderDO>()
                .eq(MesProWorkOrderDO::getTenantId, TenantContextHolder.getTenantId())
                .like(MesProWorkOrderDO::getRemark, MARKER)
                .like(MesProWorkOrderDO::getRemark, "][actorUserId=" + actorUserId + "]"));
        String cleanedRunId = null;
        for (MesProWorkOrderDO workOrder : workOrders) {
            String runId = runIdFromMarker(workOrder.getRemark(), actorUserId);
            if (Objects.equals(runId, excludedRunId)) {
                continue;
            }
            for (MesProcessPoolActiveOrderDO activeOrder : activeOrderMapper
                    .selectHistoryByWorkOrderIdForUpdate(workOrder.getId())) {
                cleanupRuntime(activeOrder);
                for (MesProcessPoolActiveOrderPickListBindingDO binding : bindingMapper
                        .selectListByActiveOrderId(activeOrder.getId())) {
                    bindingItemMapper.delete(new LambdaQueryWrapper<MesProcessPoolActiveOrderPickListBindingItemDO>()
                            .eq(MesProcessPoolActiveOrderPickListBindingItemDO::getBindingId, binding.getId()));
                    bindingMapper.deleteById(binding.getId());
                }
                activeOrderMapper.deleteById(activeOrder.getId());
            }
            cleanupCopiedPickLists(runId);
            cleanupFormalProductIssueSources(workOrder.getId());
            workOrderBomMapper.deleteByWorkOrderId(workOrder.getId());
            workOrderMapper.deleteById(workOrder.getId());
            cleanedRunId = runId;
        }
        return cleanedRunId;
    }

    private void cleanupCopiedPickLists(String runId) {
        List<ErpKingdeeProductionPickListDO> pickLists = pickListMapper.selectList(
                new LambdaQueryWrapper<ErpKingdeeProductionPickListDO>()
                        .likeRight(ErpKingdeeProductionPickListDO::getSourceFid,
                                "STAGE1-" + shortRunId(runId) + "-PL-"));
        for (ErpKingdeeProductionPickListDO pickList : pickLists) {
            pickListItemMapper.selectListByPickListIds(List.of(pickList.getId()))
                    .forEach(item -> pickListItemMapper.deleteById(item.getId()));
            pickListMapper.deleteById(pickList.getId());
        }
    }

    private void cleanupRuntime(MesProcessPoolActiveOrderDO activeOrder) {
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper.selectListByActiveOrderId(activeOrder.getId());
        Set<Long> taskIds = tasks.stream().map(MesPqcInspectionTaskDO::getId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Long> eventIds = new LinkedHashSet<>();
        allocationMapper.selectAllListByActiveOrderIdForUpdate(activeOrder.getId()).stream()
                .map(MesProcessPoolReportAllocationDO::getEventId).filter(Objects::nonNull).forEach(eventIds::add);
        tasks.forEach(task -> {
            if (task.getSubmittedEventId() != null) {
                eventIds.add(task.getSubmittedEventId());
            }
            eventMapper.selectListPqcByTaskId(PQC_SOURCE, task.getId()).stream()
                    .map(MesProProcessPoolEventDO::getId).filter(Objects::nonNull).forEach(eventIds::add);
        });
        aggregateMapper.deleteByActiveOrderId(activeOrder.getId());
        pieceMapper.deleteByTaskIds(taskIds);
        pqcRecordMapper.deleteByEventIds(eventIds);
        reviewMapper.deleteByEventIds(eventIds);
        reviewCopyFieldMapper.deleteByEventIds(eventIds);
        reviewCopyMapper.deleteByEventIds(eventIds);
        eventRevisionDiffMapper.deleteByEventIds(eventIds);
        eventRevisionMapper.deleteByEventIds(eventIds);
        quantityFragmentMapper.deleteByEventIds(eventIds);
        allocationStateMapper.deleteByEventIds(eventIds);
        allocationAuditMapper.deleteByActiveOrderId(activeOrder.getId());
        allocationMapper.deleteAllByActiveOrderId(activeOrder.getId());
        completionMapper.deleteByWorkOrderId(activeOrder.getWorkOrderId());
        completionBackfillMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId())
                .forEach(item -> completionBackfillMapper.deleteById(item.getId()));
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = completionReceiptMapper
                .selectByActiveOrderIdForUpdate(activeOrder.getId());
        if (receipt != null) {
            completionReceiptMapper.deleteById(receipt.getId());
        }
        feedbackMapper.delete(new LambdaQueryWrapper<MesProFeedbackDO>()
                .eq(MesProFeedbackDO::getWorkOrderId, activeOrder.getWorkOrderId()));
        releaseApplicationMapper.deleteByActiveOrderId(activeOrder.getId());
        pqcTaskMapper.deleteByActiveOrderId(activeOrder.getId());
        snapshotMapper.deleteByActiveOrderId(activeOrder.getId());
        if (!eventIds.isEmpty()) {
            eventMapper.deleteActiveOrderRuntimeEventsByIds(eventIds);
        }
    }

    private void assertFormalOrderProcessCompletionFacts(MesProcessPoolActiveOrderDO activeOrder, String runId) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = snapshotMapper
                .selectListByActiveOrderIdForUpdate(activeOrder.getId());
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalStateException("STAGE1_PROCESS_SNAPSHOT_REQUIRED");
        }
        Set<String> snapshotKeys = new LinkedHashSet<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            if (snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null) {
                throw new IllegalStateException("STAGE1_PROCESS_SNAPSHOT_INVALID");
            }
            snapshotKeys.add(processKey(snapshot.getRouteProcessId(), snapshot.getProcessId()));
        }
        List<MesProcessPoolOrderProcessCompletionDO> completions = completionMapper
                .selectListByWorkOrderIdsForUpdate(List.of(activeOrder.getWorkOrderId()));
        if (completions == null || completions.isEmpty()) {
            throw new IllegalStateException("STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_REQUIRED");
        }
        Map<String, MesProcessPoolOrderProcessCompletionDO> completionByProcess = new LinkedHashMap<>();
        for (MesProcessPoolOrderProcessCompletionDO completion : completions) {
            if (completion == null || completion.getId() == null
                    || !Objects.equals(activeOrder.getWorkOrderId(), completion.getWorkOrderId())
                    || completion.getRouteProcessId() == null || completion.getProcessId() == null) {
                throw new IllegalStateException("STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_INVALID");
            }
            String key = processKey(completion.getRouteProcessId(), completion.getProcessId());
            if (!snapshotKeys.contains(key) || completionByProcess.put(key, completion) != null) {
                throw new IllegalStateException("STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_INVALID");
            }
            validateOrderProcessCompletionTrace(activeOrder, completion, runId);
        }
        if (!completionByProcess.keySet().containsAll(snapshotKeys)) {
            throw new IllegalStateException("STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_REQUIRED");
        }
    }

    private void validateOrderProcessCompletionTrace(MesProcessPoolActiveOrderDO activeOrder,
                                                     MesProcessPoolOrderProcessCompletionDO completion,
                                                     String runId) {
        if (!MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED.equals(completion.getCompletionStatus())
                || completion.getCompletedAt() == null
                || completion.getTargetQuantity() == null || completion.getTargetQuantity().signum() <= 0
                || completion.getConfirmedQuantity() == null
                || completion.getConfirmedQuantity().compareTo(completion.getTargetQuantity()) < 0
                || !MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_NOT_REQUIRED
                .equals(completion.getBackfillStatus())
                || completion.getBackfillExecutionId() != null || !blank(completion.getBackfillError())
                || blank(completion.getAggregateHash()) || blank(completion.getBackfillIdempotencyKey())) {
            throw new IllegalStateException("STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_INVALID");
        }
        List<Long> eventIds = parseIds(completion.getSourceEventIdsJson(),
                "STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_EVENT_IDS_INVALID");
        List<Long> allocationIds = parseIds(completion.getSourceAllocationIdsJson(),
                "STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_ALLOCATION_IDS_INVALID");
        Set<Long> eventIdSet = new LinkedHashSet<>(eventIds);
        List<MesProProcessPoolEventDO> events = eventMapper.selectList(
                new LambdaQueryWrapper<MesProProcessPoolEventDO>()
                        .in(MesProProcessPoolEventDO::getId, eventIds));
        if (events == null || events.size() != eventIdSet.size()
                || !eventIdSet.contains(completion.getLastEventId())) {
            throw new IllegalStateException("STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_EVENT_IDS_INVALID");
        }
        for (MesProProcessPoolEventDO event : events) {
            requireMarker(event, runId);
            if (!MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())
                    || !Objects.equals(activeOrder.getWorkOrderId(), event.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), event.getRouteId())
                    || !Objects.equals(completion.getRouteProcessId(), event.getRouteProcessId())
                    || !Objects.equals(completion.getProcessId(), event.getProcessId())) {
                throw new IllegalStateException("STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_EVENT_IDS_INVALID");
            }
        }
        Set<Long> allocationIdSet = new LinkedHashSet<>(allocationIds);
        List<MesProcessPoolReportAllocationDO> allocations = allocationMapper.selectList(
                new LambdaQueryWrapper<MesProcessPoolReportAllocationDO>()
                        .in(MesProcessPoolReportAllocationDO::getId, allocationIds));
        if (allocations == null || allocations.size() != allocationIdSet.size()) {
            throw new IllegalStateException("STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_ALLOCATION_IDS_INVALID");
        }
        MesProcessPoolSubmissionReviewDO lastReview = reviewMapper.selectById(completion.getLastReviewId());
        requireMarker(lastReview, runId, "orderProcessCompletionReview");
        for (MesProcessPoolReportAllocationDO allocation : allocations) {
            requireMarker(allocation, runId, "orderProcessCompletionAllocation");
            if (!Objects.equals(activeOrder.getId(), allocation.getActiveOrderId())
                    || !Objects.equals(activeOrder.getWorkOrderId(), allocation.getWorkOrderId())
                    || !Objects.equals(completion.getRouteProcessId(), allocation.getRouteProcessId())
                    || !Objects.equals(completion.getProcessId(), allocation.getProcessId())
                    || !eventIdSet.contains(allocation.getEventId())
                    || !Objects.equals(completion.getLastReviewId(), allocation.getReviewId())
                    || allocation.getAllocatedQuantity() == null
                    || allocation.getAllocatedQuantity().signum() <= 0) {
                throw new IllegalStateException("STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_ALLOCATION_IDS_INVALID");
            }
        }
    }

    private void cleanupFormalProductIssueSources(Long workOrderId) {
        List<MesWmProductIssueDO> issues = productIssueMapper.selectListByWorkOrderIdForUpdate(workOrderId);
        if (issues == null || issues.isEmpty()) {
            return;
        }
        Set<Long> batchIds = new LinkedHashSet<>();
        Set<Long> stockIds = new LinkedHashSet<>();
        for (MesWmProductIssueDO issue : issues) {
            if (issue == null || issue.getId() == null) {
                continue;
            }
            for (MesWmProductIssueDetailDO detail : productIssueDetailMapper.selectListByIssueId(issue.getId())) {
                if (detail != null) {
                    if (detail.getBatchId() != null) {
                        batchIds.add(detail.getBatchId());
                    }
                    if (detail.getMaterialStockId() != null) {
                        stockIds.add(detail.getMaterialStockId());
                    }
                }
            }
            productIssueDetailMapper.deleteByIssueId(issue.getId());
            productIssueLineMapper.deleteByIssueId(issue.getId());
            productIssueMapper.deleteById(issue.getId());
        }
        if (!stockIds.isEmpty()) {
            materialStockMapper.delete(new LambdaQueryWrapper<MesWmMaterialStockDO>()
                    .in(MesWmMaterialStockDO::getId, stockIds));
        }
        if (!batchIds.isEmpty()) {
            batchMapper.delete(new LambdaQueryWrapper<MesWmBatchDO>()
                    .in(MesWmBatchDO::getId, batchIds));
        }
    }

    private void createFormalProductIssue(MesProcessPoolActiveOrderDO activeOrder,
                                           MesProWorkOrderDO workOrder,
                                           MesStage1ActiveOrderCompleteSimulationCommand command) {
        MesWmWarehouseDO warehouse = requireWarehouse(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE);
        MesWmWarehouseLocationDO location = requireLocation(warehouse.getId(),
                MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION);
        MesWmWarehouseAreaDO area = requireArea(location.getId(), MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA);
        String marker = marker(command.getSimulationRunId(), command.getActorUserId());
        LocalDateTime now = LocalDateTime.now();
        int issueIndex = 0;
        int batchIndex = 0;
        for (MesProcessPoolActiveOrderPickListBindingDO binding : requireBindings(activeOrder)) {
            List<MesProcessPoolActiveOrderPickListBindingItemDO> bindingItems = bindingItemMapper
                    .selectListByBindingId(binding.getId());
            if (bindingItems == null || bindingItems.isEmpty()) {
                throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_REQUIRED");
            }
            createFormalProductIssueForBinding(activeOrder, workOrder, command, binding, bindingItems,
                    warehouse, location, area, marker, now, issueIndex++, batchIndex);
            batchIndex += bindingItems.size();
        }
    }

    private void createFormalProductIssueForBinding(MesProcessPoolActiveOrderDO activeOrder,
                                                    MesProWorkOrderDO workOrder,
                                                    MesStage1ActiveOrderCompleteSimulationCommand command,
                                                    MesProcessPoolActiveOrderPickListBindingDO binding,
                                                    List<MesProcessPoolActiveOrderPickListBindingItemDO> bindingItems,
                                                    MesWmWarehouseDO warehouse,
                                                    MesWmWarehouseLocationDO location,
                                                    MesWmWarehouseAreaDO area,
                                                    String marker,
                                                    LocalDateTime now,
                                                    int issueIndex,
                                                    int batchIndexStart) {
        List<MesProcessPoolActiveOrderPickListBindingItemDO> materializedItems = bindingItems.stream()
                .filter(item -> item != null && !isPlaceholderMaterialCode(item.getMaterialNumber()))
                .toList();
        if (materializedItems.isEmpty()) {
            return;
        }
        MesWmProductIssueDO issue = MesWmProductIssueDO.builder()
                .code("STAGE1-ISSUE-" + shortRunId(command.getSimulationRunId()) + "-" + binding.getId())
                .name(workOrder.getName() + " Stage1正式领料-" + (issueIndex + 1))
                .workOrderId(workOrder.getId())
                .issueDate(now)
                .requiredTime(now)
                .status(MesWmProductIssueStatusEnum.FINISHED.getStatus())
                .remark(marker)
                .build();
        productIssueMapper.insert(issue);
        int index = batchIndexStart;
        for (MesProcessPoolActiveOrderPickListBindingItemDO bindingItem : materializedItems) {
            MesMdItemDO item = itemMapper.selectByCode(bindingItem.getMaterialNumber());
            if (item == null) {
                throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_ITEM_REQUIRED");
            }
            BigDecimal quantity = resolveProductIssueQuantity(bindingItem);
            MesWmBatchDO batch = MesWmBatchDO.builder()
                    .code("STAGE1-BATCH-" + shortRunId(command.getSimulationRunId()) + "-" + index)
                    .itemId(item.getId())
                    .produceDate(now)
                    .receiptDate(now)
                    .workOrderId(workOrder.getId())
                    .lotNumber(bindingItem.getLotNumber())
                    .remark(marker)
                    .build();
            batchMapper.insert(batch);
            MesWmMaterialStockDO stock = MesWmMaterialStockDO.builder()
                    .itemTypeId(item.getItemTypeId())
                    .itemId(item.getId())
                    .batchId(batch.getId())
                    .batchCode(batch.getCode())
                    .warehouseId(warehouse.getId())
                    .locationId(location.getId())
                    .areaId(area.getId())
                    .vendorId(workOrder.getVendorId())
                    .quantity(BigDecimal.ZERO)
                    .receiptTime(now)
                    .frozen(Boolean.FALSE)
                    .build();
            materialStockMapper.insert(stock);
            MesWmProductIssueLineDO line = MesWmProductIssueLineDO.builder()
                    .issueId(issue.getId())
                    .itemId(item.getId())
                    .quantity(quantity)
                    .batchId(batch.getId())
                    .remark(marker)
                    .build();
            productIssueLineMapper.insert(line);
            MesWmProductIssueDetailDO detail = MesWmProductIssueDetailDO.builder()
                    .issueId(issue.getId())
                    .lineId(line.getId())
                    .materialStockId(stock.getId())
                    .itemId(item.getId())
                    .quantity(quantity)
                    .batchId(batch.getId())
                    .batchCode(batch.getCode())
                    .warehouseId(warehouse.getId())
                    .locationId(location.getId())
                    .areaId(area.getId())
                    .remark(marker)
                    .build();
            productIssueDetailMapper.insert(detail);
            index++;
        }
    }

    private void validateFormalProductIssue(MesProcessPoolActiveOrderDO activeOrder, String runId, Long actorUserId) {
        List<MesWmProductIssueDO> issues = productIssueMapper
                .selectListByWorkOrderIdForUpdate(activeOrder.getWorkOrderId());
        List<MesProcessPoolActiveOrderPickListBindingDO> bindings = requireBindings(activeOrder);
        long expectedIssueCount = bindings.stream()
                .map(binding -> bindingItemMapper.selectListByBindingId(binding.getId()))
                .filter(items -> items != null && items.stream()
                        .anyMatch(item -> item != null && !isPlaceholderMaterialCode(item.getMaterialNumber())))
                .count();
        if (issues == null || issues.size() != expectedIssueCount) {
            throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_REQUIRED");
        }
        String marker = marker(runId, actorUserId);
        Set<Long> batchIds = new LinkedHashSet<>();
        Set<Long> stockIds = new LinkedHashSet<>();
        for (MesWmProductIssueDO issue : issues) {
            if (issue == null || issue.getId() == null
                    || !Objects.equals(activeOrder.getWorkOrderId(), issue.getWorkOrderId())
                    || !Objects.equals(MesWmProductIssueStatusEnum.FINISHED.getStatus(), issue.getStatus())
                    || !Objects.equals(marker, issue.getRemark())) {
                throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_INVALID");
            }
            List<MesWmProductIssueLineDO> lines = productIssueLineMapper.selectListByIssueId(issue.getId());
            List<MesWmProductIssueDetailDO> details = productIssueDetailMapper.selectListByIssueId(issue.getId());
            if (lines == null || lines.isEmpty() || details == null || details.isEmpty()
                    || lines.size() != details.size()) {
                throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_DETAIL_REQUIRED");
            }
            for (MesWmProductIssueLineDO line : lines) {
                if (line == null || line.getId() == null || !Objects.equals(issue.getId(), line.getIssueId())
                        || line.getItemId() == null || line.getQuantity() == null
                        || line.getQuantity().signum() <= 0 || line.getBatchId() == null
                        || !Objects.equals(marker, line.getRemark())) {
                    throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_LINE_INVALID");
                }
                batchIds.add(line.getBatchId());
            }
            for (MesWmProductIssueDetailDO detail : details) {
                if (detail == null || detail.getId() == null || !Objects.equals(issue.getId(), detail.getIssueId())
                        || detail.getLineId() == null || detail.getMaterialStockId() == null
                        || detail.getItemId() == null || detail.getQuantity() == null
                        || detail.getQuantity().signum() <= 0 || detail.getBatchId() == null
                        || blank(detail.getBatchCode()) || detail.getWarehouseId() == null
                        || detail.getLocationId() == null || detail.getAreaId() == null
                        || !Objects.equals(marker, detail.getRemark())) {
                    throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_DETAIL_INVALID");
                }
                batchIds.add(detail.getBatchId());
                stockIds.add(detail.getMaterialStockId());
            }
        }
        List<MesWmBatchDO> batches = batchMapper.selectList(new LambdaQueryWrapper<MesWmBatchDO>()
                .in(MesWmBatchDO::getId, batchIds)
                .orderByAsc(MesWmBatchDO::getId));
        if (batches == null || batches.size() != batchIds.size()) {
            throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_BATCH_REQUIRED");
        }
        for (MesWmBatchDO batch : batches) {
            if (batch == null || batch.getId() == null || !Objects.equals(activeOrder.getWorkOrderId(), batch.getWorkOrderId())
                    || blank(batch.getCode()) || blank(batch.getLotNumber())
                    || !Objects.equals(marker, batch.getRemark())) {
                throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_BATCH_INVALID");
            }
        }
        List<MesWmMaterialStockDO> stocks = materialStockMapper.selectListByIds(stockIds);
        if (stocks == null || stocks.size() != stockIds.size()) {
            throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_STOCK_REQUIRED");
        }
        for (MesWmMaterialStockDO stock : stocks) {
            if (stock == null || stock.getId() == null || stock.getItemId() == null
                    || stock.getBatchId() == null || blank(stock.getBatchCode())
                    || stock.getWarehouseId() == null || stock.getLocationId() == null
                    || stock.getAreaId() == null || stock.getQuantity() == null
                    || stock.getQuantity().signum() < 0) {
                throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_STOCK_INVALID");
            }
        }
    }

    private MesWmWarehouseDO requireWarehouse(String code) {
        MesWmWarehouseDO warehouse = warehouseMapper.selectByCode(code);
        if (warehouse == null || warehouse.getId() == null) {
            throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_WAREHOUSE_REQUIRED");
        }
        return warehouse;
    }

    private MesWmWarehouseLocationDO requireLocation(Long warehouseId, String code) {
        MesWmWarehouseLocationDO location = warehouseLocationMapper.selectByCode(warehouseId, code);
        if (location == null || location.getId() == null) {
            throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_LOCATION_REQUIRED");
        }
        return location;
    }

    private MesWmWarehouseAreaDO requireArea(Long locationId, String code) {
        MesWmWarehouseAreaDO area = warehouseAreaMapper.selectByCode(locationId, code);
        if (area == null || area.getId() == null) {
            throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_AREA_REQUIRED");
        }
        return area;
    }

    private BigDecimal resolveProductIssueQuantity(MesProcessPoolActiveOrderPickListBindingItemDO bindingItem) {
        BigDecimal quantity = bindingItem.getBaseActualQuantity();
        if (quantity == null || quantity.signum() <= 0) {
            quantity = bindingItem.getActualQuantity();
        }
        if (quantity == null || quantity.signum() <= 0) {
            quantity = bindingItem.getRequestedQuantity();
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalStateException("STAGE1_FORMAL_PRODUCT_ISSUE_QUANTITY_REQUIRED");
        }
        return quantity;
    }

    private void requireMarker(Object value, String runId) {
        requireMarker(value, runId, "simulationFact");
    }

    private void requireMarker(Object value, String runId, String name) {
        if (value == null) {
            throw new IllegalStateException("STAGE1_SIMULATION_FACT_MISSING:" + name);
        }
        try {
            Object simulated = value.getClass().getMethod("getSimulated").invoke(value);
            Object stage = value.getClass().getMethod("getSimulationStage").invoke(value);
            Object actualRunId = value.getClass().getMethod("getSimulationRunId").invoke(value);
            if (!Boolean.TRUE.equals(simulated) || !STAGE.equals(stage) || !Objects.equals(runId, actualRunId)) {
                throw new IllegalStateException("STAGE1_SIMULATION_RUN_ID_NOT_PERSISTED:" + name);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("STAGE1_SIMULATION_RUN_ID_NOT_PERSISTED:" + name, ex);
        }
    }

    private void requireTenant() {
        if (TenantContextHolder.getTenantId() == null || TenantContextHolder.getTenantId() <= 0) {
            throw new IllegalStateException("STAGE1_TENANT_REQUIRED");
        }
    }

    private String marker(String runId, Long actorUserId) {
        return MARKER + "[simulationRunId=" + runId + "][actorUserId=" + actorUserId + "]";
    }

    private String marker(String runId, Long actorUserId, Long sourceActiveOrderId) {
        return marker(runId, actorUserId) + "[sourceActiveOrderId=" + sourceActiveOrderId + "]";
    }

    private String runIdFromMarker(String value, Long actorUserId) {
        String prefix = MARKER + "[simulationRunId=";
        String actorToken = "][actorUserId=" + actorUserId + "]";
        if (value == null || !value.startsWith(prefix) || !value.contains(actorToken)) {
            throw new IllegalStateException("STAGE1_CLEANUP_SCOPE_INVALID");
        }
        String runId = value.substring(prefix.length(), value.indexOf(actorToken));
        if (!runId.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalStateException("STAGE1_CLEANUP_SCOPE_INVALID");
        }
        return runId;
    }

    private String shortRunId(String runId) {
        String value = runId.replaceAll("[^A-Za-z0-9]", "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("STAGE1_SIMULATION_RUN_ID_INVALID");
        }
        return value.length() <= 32 ? value : value.substring(value.length() - 32);
    }

    private List<Long> parseIds(String json, String errorCode) {
        if (blank(json)) {
            throw new IllegalStateException(errorCode);
        }
        List<Long> ids = JsonUtils.parseArray(json, Long.class);
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)
                || new LinkedHashSet<>(ids).size() != ids.size()) {
            throw new IllegalStateException(errorCode);
        }
        return ids;
    }

    private String processKey(Long routeProcessId, Long processId) {
        return routeProcessId + "|" + processId;
    }

    private BigDecimal toProgressPercent(long completedProcessCount, int totalProcessCount) {
        if (totalProcessCount <= 0) {
            throw new IllegalStateException("STAGE1_PROGRESS_TARGET_REQUIRED");
        }
        return BigDecimal.valueOf(completedProcessCount)
                .multiply(PERCENT_DIVISOR)
                .divide(BigDecimal.valueOf(totalProcessCount), PROGRESS_PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroProgressPercent() {
        return BigDecimal.ZERO.setScale(PROGRESS_PERCENT_SCALE, RoundingMode.UNNECESSARY);
    }

    private String hash(Object value) {
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(value));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isPlaceholderMaterialCode(String materialCode) {
        return materialCode != null && PLACEHOLDER_MATERIAL_CODE.equals(materialCode.trim());
    }

    private boolean zero(BigDecimal value) {
        return value != null && value.signum() == 0;
    }

    private record Stage1Progress(BigDecimal productionProgressPercent, BigDecimal inspectionProgressPercent) {

        private boolean productionProgress100() {
            return productionProgressPercent != null
                    && productionProgressPercent.compareTo(PERCENT_DIVISOR) == 0;
        }

        private boolean inspectionProgress100() {
            return inspectionProgressPercent != null
                    && inspectionProgressPercent.compareTo(PERCENT_DIVISOR) == 0;
        }

        private boolean complete() {
            return productionProgress100() && inspectionProgress100();
        }
    }
}
