package cn.iocoder.yudao.module.mes.service.pro.simulation.stage1;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
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
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
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
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MesStage1ActiveOrderCompleteSimulationServiceImpl
        implements MesStage1ActiveOrderCompleteSimulationService {

    private static final String STAGE = "STAGE1";
    private static final String MARKER = "[STAGE1_SIMULATION]";
    private static final String ACTIVE = "ACTIVE";
    private static final String PQC_SOURCE = "MES_PQC_INSPECTION_TASK";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProWorkOrderService workOrderService;
    private final MesProWorkOrderBomMapper workOrderBomMapper;
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
    private final MesTeamLeaderActiveOrderSimulationService activeOrderSimulationService;

    public MesStage1ActiveOrderCompleteSimulationServiceImpl(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesProWorkOrderService workOrderService,
            MesProWorkOrderBomMapper workOrderBomMapper,
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
            MesTeamLeaderActiveOrderSimulationService activeOrderSimulationService) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.workOrderService = workOrderService;
        this.workOrderBomMapper = workOrderBomMapper;
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
        String cleanedRunId = cleanupOwnedRuns(validated.getActorUserId());
        MesProcessPoolActiveOrderDO template = activeOrderMapper
                .selectByIdForUpdate(validated.getTemplateActiveOrderId());
        requireTemplate(template, validated.getActorUserId());
        MesProWorkOrderDO templateWorkOrder = requireWorkOrder(template.getWorkOrderId());
        MesProcessPoolActiveOrderPickListBindingDO templateBinding = createSimulationPickList(templateWorkOrder);
        MesProcessPoolActiveOrderDO fixture = createFixture(template, templateWorkOrder,
                templateBinding, validated);

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
        assertNoDownstreamSideEffects(fixture);
        Map<String, Object> snapshot = buildSnapshot(fixture, templateBinding, validated, simulation);
        return new MesStage1ActiveOrderCompleteSimulationResult()
                .setSimulationRunId(validated.getSimulationRunId())
                .setCleanedSimulationRunId(cleanedRunId)
                .setActiveOrderId(fixture.getId())
                .setWorkOrderId(fixture.getWorkOrderId())
                .setPickListId(bindingMapper.selectByActiveOrderId(fixture.getId()).getPickListId())
                .setProductionSubmitCount(simulation.getProductionSubmitCount())
                .setProductionReviewCount(simulation.getProductionReviewCount())
                .setPqcSubmitCount(simulation.getPqcSubmitCount())
                .setPqcReviewCount(simulation.getPqcReviewCount())
                .setProductionProgress100(true)
                .setInspectionProgress100(true)
                .setCompletionButtonEnabled(true)
                .setActiveOrderCompleteSnapshot(snapshot);
    }

    private MesProcessPoolActiveOrderDO createFixture(
            MesProcessPoolActiveOrderDO template,
            MesProWorkOrderDO templateWorkOrder,
            MesProcessPoolActiveOrderPickListBindingDO templateBinding,
            MesStage1ActiveOrderCompleteSimulationCommand command) {
        MesProWorkOrderDO workOrder = createWorkOrder(templateWorkOrder, command.getSimulationRunId(),
                command.getActorUserId());
        clonePickList(templateBinding, workOrder.getCode(), command.getSimulationRunId(),
                command.getActorUserId());
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
        cloneBinding(templateBinding, activeOrder, workOrder, command);
        return activeOrder;
    }

    private MesProWorkOrderDO createWorkOrder(MesProWorkOrderDO template, String runId, Long actorUserId) {
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
                .setRemark(marker(runId, actorUserId));
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

    private MesProcessPoolActiveOrderPickListBindingDO createSimulationPickList(MesProWorkOrderDO workOrder) {
        if (blank(workOrder.getOrderSourceCode())) {
            throw new IllegalStateException("STAGE1_PICK_LIST_SOURCE_REQUIRED");
        }
        List<Long> pickListIds = pickListItemMapper
                .selectPickListIdsByProductionOrderNo(workOrder.getOrderSourceCode());
        for (Long pickListId : pickListIds) {
            ErpKingdeeProductionPickListDO header = pickListMapper.selectById(pickListId);
            List<ErpKingdeeProductionPickListItemDO> items = pickListItemMapper
                    .selectListByPickListIds(List.of(pickListId));
            if (header != null && !blank(header.getSourceFid()) && !blank(header.getSourceBillNo())
                    && !blank(header.getDocumentStatus()) && items != null && !items.isEmpty()) {
                String snapshotHash = hash(items);
                return MesProcessPoolActiveOrderPickListBindingDO.builder()
                        .pickListId(header.getId())
                        .sourceFid(header.getSourceFid())
                        .sourceBillNo(header.getSourceBillNo())
                        .sourceDocumentStatus(header.getDocumentStatus())
                        .sourceModifyTime(header.getSourceModifyTime())
                        .sourceSnapshotHash(snapshotHash)
                        .bindingStatus("BOUND")
                        .bindingVersion(1)
                        .requestPayloadHash(hash(workOrder.getCode() + "|" + header.getId() + "|"
                                + snapshotHash))
                        .build();
            }
        }
        throw new IllegalStateException("STAGE1_PICK_LIST_SOURCE_REQUIRED");
    }

    private ErpKingdeeProductionPickListDO clonePickList(
            MesProcessPoolActiveOrderPickListBindingDO source, String workOrderCode,
            String runId, Long actorUserId) {
        ErpKingdeeProductionPickListDO header = pickListMapper.selectById(source.getPickListId());
        List<ErpKingdeeProductionPickListItemDO> items = pickListItemMapper
                .selectListByPickListIds(List.of(source.getPickListId()));
        if (header == null || items == null || items.isEmpty()) {
            throw new IllegalStateException("STAGE1_PICK_LIST_SOURCE_REQUIRED");
        }
        String safe = shortRunId(runId);
        ErpKingdeeProductionPickListDO copy = BeanUtils.toBean(header, ErpKingdeeProductionPickListDO.class)
                .setId(null)
                .setSourceFid("STAGE1-" + safe + "-FID")
                .setSourceBillNo("STAGE1-PL-" + safe)
                .setDocumentStatus("C")
                .setDescription("Stage1正式领料模拟")
                .setLastSyncTime(LocalDateTime.now())
                .setRawPayload(JsonUtils.toJsonString(Map.of(
                        "simulated", true, "simulationStage", STAGE,
                        "simulationRunId", runId, "source", "MES_STAGE1_SIMULATION_FIXTURE")));
        pickListMapper.insert(copy);
        for (ErpKingdeeProductionPickListItemDO item : items) {
            pickListItemMapper.insert(BeanUtils.toBean(item, ErpKingdeeProductionPickListItemDO.class)
                    .setId(null)
                    .setProductionPickListId(copy.getId())
                    .setSourceFid("STAGE1-" + safe + "-E-" + item.getId())
                    .setSourceEntryId("STAGE1-" + safe + "-ENTRY-" + item.getId())
                    .setSourceLineKey("STAGE1-" + safe + "-LINE-" + item.getId())
                    .setSourceBillNo(copy.getSourceBillNo())
                    .setProductionOrderNo(workOrderCode)
                    .setLastSyncTime(LocalDateTime.now()));
        }
        return copy;
    }

    private void cloneBinding(MesProcessPoolActiveOrderPickListBindingDO source,
                              MesProcessPoolActiveOrderDO target, MesProWorkOrderDO workOrder,
                              MesStage1ActiveOrderCompleteSimulationCommand command) {
        ErpKingdeeProductionPickListDO header = pickListMapper.selectOne(
                new LambdaQueryWrapper<ErpKingdeeProductionPickListDO>()
                        .eq(ErpKingdeeProductionPickListDO::getSourceBillNo,
                                "STAGE1-PL-" + shortRunId(command.getSimulationRunId())));
        if (header == null) {
            throw new IllegalStateException("STAGE1_PICK_LIST_CLONE_INVALID");
        }
        List<ErpKingdeeProductionPickListItemDO> items = pickListItemMapper
                .selectListByPickListIds(List.of(header.getId()));
        MesProcessPoolActiveOrderPickListBindingDO binding = BeanUtils.toBean(source,
                        MesProcessPoolActiveOrderPickListBindingDO.class)
                .setId(IdUtil.getSnowflake().nextId())
                .setActiveOrderId(target.getId())
                .setWorkOrderId(workOrder.getId())
                .setPickListId(header.getId())
                .setSourceFid(header.getSourceFid())
                .setSourceBillNo(header.getSourceBillNo())
                .setSourceDocumentStatus(header.getDocumentStatus())
                .setSourceSnapshotHash(hash(items))
                .setBoundBy(command.getActorUserId())
                .setBoundAt(LocalDateTime.now())
                .setIdempotencyKey("STAGE1-" + command.getSimulationRunId())
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
        MesProcessPoolActiveOrderPickListBindingDO binding = bindingMapper
                .selectByActiveOrderId(activeOrder.getId());
            requireMarker(binding, runId, "pickListBinding");
        for (MesProcessPoolActiveOrderPickListBindingItemDO item : bindingItemMapper
                .selectListByBindingId(binding.getId())) {
            requireMarker(item, runId, "pickListBindingItem");
        }
        List<MesProProcessPoolEventDO> productionEvents = eventMapper
                .selectProductionSubmitsByWorkOrderAndRouteForUpdate(activeOrder.getWorkOrderId(),
                        activeOrder.getRouteId());
        List<Long> eventIds = new ArrayList<>();
        for (MesProProcessPoolEventDO event : productionEvents) {
            requireMarker(event, runId);
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

    private void assertNoDownstreamSideEffects(MesProcessPoolActiveOrderDO activeOrder) {
        if (!ACTIVE.equals(activeOrder.getActiveStatus()) || !ACTIVE.equals(activeOrder.getBusinessStatus())
                || activeOrder.getReleaseDecisionId() != null || activeOrder.getReleasedBy() != null
                || activeOrder.getReleasedAt() != null) {
            throw new IllegalStateException("STAGE1_ACTIVE_ORDER_LEFT_COMPLETE_NODE");
        }
        if (!completionMapper.selectListByWorkOrderIds(List.of(activeOrder.getWorkOrderId())).isEmpty()) {
            throw new IllegalStateException("STAGE1_COMPLETION_SIDE_EFFECT");
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
                                              MesProcessPoolActiveOrderPickListBindingDO templateBinding,
                                              MesStage1ActiveOrderCompleteSimulationCommand command,
                                              MesTeamLeaderActiveOrderSimulationResult simulation) {
        MesProcessPoolActiveOrderPickListBindingDO binding = bindingMapper
                .selectByActiveOrderId(activeOrder.getId());
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
        snapshot.put("pickListSource", Map.of(
                "bindingId", String.valueOf(binding.getId()),
                "pickListId", String.valueOf(binding.getPickListId()),
                "sourceFid", binding.getSourceFid(),
                "sourceBillNo", binding.getSourceBillNo(),
                "sourceSnapshotHash", binding.getSourceSnapshotHash(),
                "templateSourceSnapshotHash", templateBinding.getSourceSnapshotHash()));
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
        snapshot.put("progress", Map.of("productionPercent", 100, "inspectionPercent", 100,
                "completionButtonEnabled", true));
        snapshot.put("downstreamSideEffects", Map.of(
                "completion", false, "backfill", false, "batchExecution", false,
                "fileUpload", false, "release", false));
        snapshot.put("stage2InputContract", "activeOrderCompleteSnapshot.v2");
        snapshot.put("independentFixture", true);
        return snapshot;
    }

    private void requireTemplate(MesProcessPoolActiveOrderDO activeOrder, Long actorUserId) {
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), actorUserId)
                || !Objects.equals(activeOrder.getTenantId(), TenantContextHolder.getTenantId())
                || !ACTIVE.equals(activeOrder.getActiveStatus()) || activeOrder.getWorkOrderId() == null
                || activeOrder.getRouteId() == null || activeOrder.getRouteVersionId() == null) {
            throw new IllegalStateException("STAGE1_TEMPLATE_INVALID");
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

    private void requireBinding(MesProcessPoolActiveOrderPickListBindingDO binding) {
        if (binding == null || binding.getPickListId() == null || blank(binding.getSourceSnapshotHash())) {
            throw new IllegalStateException("STAGE1_PICK_LIST_BINDING_REQUIRED");
        }
    }

    private String cleanupOwnedRuns(Long actorUserId) {
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectList(new LambdaQueryWrapper<MesProWorkOrderDO>()
                .eq(MesProWorkOrderDO::getTenantId, TenantContextHolder.getTenantId())
                .like(MesProWorkOrderDO::getRemark, MARKER)
                .like(MesProWorkOrderDO::getRemark, "][actorUserId=" + actorUserId + "]"));
        String cleanedRunId = null;
        for (MesProWorkOrderDO workOrder : workOrders) {
            String runId = runIdFromMarker(workOrder.getRemark(), actorUserId);
            for (MesProcessPoolActiveOrderDO activeOrder : activeOrderMapper
                    .selectHistoryByWorkOrderIdForUpdate(workOrder.getId())) {
                cleanupRuntime(activeOrder);
                MesProcessPoolActiveOrderPickListBindingDO binding = bindingMapper
                        .selectByActiveOrderId(activeOrder.getId());
                if (binding != null) {
                    bindingItemMapper.delete(new LambdaQueryWrapper<MesProcessPoolActiveOrderPickListBindingItemDO>()
                            .eq(MesProcessPoolActiveOrderPickListBindingItemDO::getBindingId, binding.getId()));
                    bindingMapper.deleteById(binding.getId());
                }
                activeOrderMapper.deleteById(activeOrder.getId());
            }
            List<ErpKingdeeProductionPickListDO> pickLists = pickListMapper.selectList(
                    new LambdaQueryWrapper<ErpKingdeeProductionPickListDO>()
                            .eq(ErpKingdeeProductionPickListDO::getSourceBillNo,
                                    "STAGE1-PL-" + shortRunId(runId)));
            for (ErpKingdeeProductionPickListDO pickList : pickLists) {
                pickListItemMapper.selectListByPickListIds(List.of(pickList.getId()))
                        .forEach(item -> pickListItemMapper.deleteById(item.getId()));
                pickListMapper.deleteById(pickList.getId());
            }
            workOrderBomMapper.deleteByWorkOrderId(workOrder.getId());
            workOrderMapper.deleteById(workOrder.getId());
            cleanedRunId = runId;
        }
        return cleanedRunId;
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
        releaseApplicationMapper.deleteByActiveOrderId(activeOrder.getId());
        pqcTaskMapper.deleteByActiveOrderId(activeOrder.getId());
        snapshotMapper.deleteByActiveOrderId(activeOrder.getId());
        if (!eventIds.isEmpty()) {
            eventMapper.deleteActiveOrderRuntimeEventsByIds(eventIds);
        }
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

    private String runIdFromMarker(String value, Long actorUserId) {
        String prefix = MARKER + "[simulationRunId=";
        String suffix = "][actorUserId=" + actorUserId + "]";
        if (value == null || !value.startsWith(prefix) || !value.endsWith(suffix)) {
            throw new IllegalStateException("STAGE1_CLEANUP_SCOPE_INVALID");
        }
        String runId = value.substring(prefix.length(), value.length() - suffix.length());
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

    private String hash(Object value) {
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(value));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
