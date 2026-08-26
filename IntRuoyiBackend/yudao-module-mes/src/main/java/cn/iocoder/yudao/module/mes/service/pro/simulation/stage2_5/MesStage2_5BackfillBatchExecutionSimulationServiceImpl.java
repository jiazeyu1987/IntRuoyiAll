package cn.iocoder.yudao.module.mes.service.pro.simulation.stage2_5;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionOpenOrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyFieldMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
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
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesCompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationService;
import cn.iocoder.yudao.module.mes.service.pro.simulation.stage4.MesStage4DossierUploadSimulationContractValidator;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
public class MesStage2_5BackfillBatchExecutionSimulationServiceImpl
        implements MesStage2_5BackfillBatchExecutionSimulationService {

    private static final String SIMULATION_MARKER = "[STAGE2_5_SIMULATION]";
    private static final String DETAIL_PATH = "/mes/pro/feedback/edhr-batch-execution/detail";
    private static final String ENTRY_TYPE = "ACTIVE_ORDER_COMPLETION";
    private static final String CREDENTIAL_TYPE = "CompletionBackfillReceipt";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProWorkOrderService workOrderService;
    private final MesProWorkOrderBomMapper workOrderBomMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
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
    private final MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolReportAllocationStateMapper allocationStateMapper;
    private final MesProcessPoolReportAllocationAdjustmentAuditMapper allocationAuditMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    private final MesProcessPoolActiveOrderCompletionBackfillMapper completionBackfillMapper;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;
    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProBatchRecordExecutionMapper batchRecordExecutionMapper;
    private final MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private final MesTeamLeaderActiveOrderSimulationService activeOrderSimulationService;
    private final MesTeamLeaderActiveOrderCompletionService activeOrderCompletionService;
    private final MesProEdhrBatchExecutionService batchExecutionService;

    public MesStage2_5BackfillBatchExecutionSimulationServiceImpl(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesProWorkOrderService workOrderService,
            MesProWorkOrderBomMapper workOrderBomMapper,
            MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
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
            MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper,
            MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolReportAllocationStateMapper allocationStateMapper,
            MesProcessPoolReportAllocationAdjustmentAuditMapper allocationAuditMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProcessPoolOrderProcessCompletionMapper completionMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesProcessPoolActiveOrderCompletionBackfillMapper completionBackfillMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper,
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProBatchRecordExecutionMapper batchRecordExecutionMapper,
            MesProEdhrBatchExecutionTaskMapper batchTaskMapper,
            MesTeamLeaderActiveOrderSimulationService activeOrderSimulationService,
            MesTeamLeaderActiveOrderCompletionService activeOrderCompletionService,
            MesProEdhrBatchExecutionService batchExecutionService) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.workOrderService = workOrderService;
        this.workOrderBomMapper = workOrderBomMapper;
        this.processSnapshotMapper = processSnapshotMapper;
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
        this.pqcPieceDetailMapper = pqcPieceDetailMapper;
        this.pqcAggregateDetailMapper = pqcAggregateDetailMapper;
        this.allocationMapper = allocationMapper;
        this.allocationStateMapper = allocationStateMapper;
        this.allocationAuditMapper = allocationAuditMapper;
        this.reviewMapper = reviewMapper;
        this.completionMapper = completionMapper;
        this.releaseApplicationMapper = releaseApplicationMapper;
        this.completionBackfillMapper = completionBackfillMapper;
        this.completionReceiptMapper = completionReceiptMapper;
        this.batchExecutionMapper = batchExecutionMapper;
        this.batchRecordExecutionMapper = batchRecordExecutionMapper;
        this.batchTaskMapper = batchTaskMapper;
        this.activeOrderSimulationService = activeOrderSimulationService;
        this.activeOrderCompletionService = activeOrderCompletionService;
        this.batchExecutionService = batchExecutionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesStage2_5BackfillBatchExecutionSimulationResult simulate(
            MesStage2_5BackfillBatchExecutionSimulationCommand command) {
        MesStage2_5BackfillBatchExecutionSimulationCommand validated =
                MesStage2_5BackfillBatchExecutionSimulationCommand.validate(
                        command == null ? null : command.getSimulationRunId(),
                        command == null ? null : command.getActiveOrderId(),
                        command == null ? null : command.getExpectedVersion(),
                        command == null ? null : command.getActorUserId());
        requireTenant();

        String cleanedRunId = cleanupOwnedRuns(validated.getActorUserId());
        MesProcessPoolActiveOrderDO template = activeOrderMapper
                .selectByIdForUpdate(validated.getActiveOrderId());
        requireOwnedActiveOrder(template, validated);
        MesProWorkOrderDO templateWorkOrder = requireWorkOrder(template);
        MesProcessPoolActiveOrderPickListBindingDO templateBinding = requireBinding(template);
        MesProcessPoolActiveOrderDO activeOrder = createFixture(template, templateWorkOrder,
                templateBinding, validated);

        MesTeamLeaderActiveOrderCompletionResult completion;
        MesTeamLeaderActiveOrderSimulationResult simulation = activeOrderSimulationService
                .simulateActiveOrderCompletion(validated.getActorUserId(), activeOrder.getId(),
                        "2.5", validated.getSimulationRunId());
        requireDoubleComplete(simulation);
        completion = activeOrderCompletionService.complete(validated.getActorUserId(),
                new MesTeamLeaderActiveOrderCompletionCommand()
                        .setActiveOrderId(activeOrder.getId())
                        .setExpectedVersion(activeOrder.getVersion())
                        .setIdempotencyKey(completionIdempotencyKey(validated.getSimulationRunId())));

        MesProcessPoolActiveOrderCompletionReceiptDO receipt = completionReceiptMapper
                .selectByIdAndTenantId(completion.getCompletionReceiptId(), TenantContextHolder.getTenantId());
        MesProWorkOrderDO workOrder = requireWorkOrder(activeOrder);
        MesProcessPoolActiveOrderPickListBindingDO binding = requireBinding(activeOrder);
        List<MesProcessPoolActiveOrderPickListBindingItemDO> bindingItems = bindingItemMapper
                .selectListByBindingId(binding.getId());
        MesCompletionBackfillReceipt backfillReceipt = buildBackfillReceipt(
                activeOrder, workOrder, binding, bindingItems, receipt);
        EdhrBatchExecutionRespVO batch = batchExecutionService.openOrCreate(buildBatchRequest(
                validated, activeOrder, workOrder, binding, backfillReceipt));
        if (batch == null || batch.getId() == null) {
            throw new IllegalStateException("BATCH_EXECUTION_CREATE_OR_OPEN_FAILED");
        }
        Map<String, Object> snapshot = buildSnapshot(validated, activeOrder, workOrder, binding,
                bindingItems, receipt, batch, backfillReceipt);
        MesStage4DossierUploadSimulationContractValidator.validateInput(snapshot);
        return new MesStage2_5BackfillBatchExecutionSimulationResult()
                .setSimulationRunId(validated.getSimulationRunId())
                .setCleanedSimulationRunId(cleanedRunId)
                .setBatchExecutionId(batch.getId())
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setCompletionReceiptId(receipt.getId())
                .setDetailPath(DETAIL_PATH + "?id=" + batch.getId()
                        + "&simulationRunId=" + validated.getSimulationRunId())
                .setBatchExecutionSnapshot(snapshot)
                .setBlockers(List.of());
    }

    private MesProcessPoolActiveOrderDO createFixture(
            MesProcessPoolActiveOrderDO template,
            MesProWorkOrderDO templateWorkOrder,
            MesProcessPoolActiveOrderPickListBindingDO templateBinding,
            MesStage2_5BackfillBatchExecutionSimulationCommand command) {
        MesProWorkOrderDO workOrder = createWorkOrder(templateWorkOrder, command.getSimulationRunId(),
                command.getActorUserId());
        clonePickList(templateBinding, workOrder.getCode(), command.getSimulationRunId(),
                command.getActorUserId());
        MesProcessPoolActiveOrderDO activeOrder = BeanUtils.toBean(template,
                        MesProcessPoolActiveOrderDO.class)
                .setId(null)
                .setLeaderUserId(command.getActorUserId())
                .setWorkOrderId(workOrder.getId())
                .setActiveStatus(ACTIVE_STATUS)
                .setBusinessStatus(ACTIVE_STATUS)
                .setJoinedAt(LocalDateTime.now())
                .setSortOrder(System.currentTimeMillis())
                .setRemovedAt(null)
                .setReleaseDecisionId(null)
                .setReleasedBy(null)
                .setReleasedAt(null)
                .setVersion(0);
        activeOrderMapper.insert(activeOrder);
        cloneProcessSnapshots(template.getId(), activeOrder.getId(), workOrder.getId());
        clonePqcTasks(template.getId(), activeOrder);
        cloneBinding(templateBinding, activeOrder, workOrder, command.getSimulationRunId());
        return activeOrder;
    }

    private MesProWorkOrderDO createWorkOrder(MesProWorkOrderDO template, String runId,
                                              Long actorUserId) {
        String safe = shortRunId(runId);
        MesProWorkOrderSaveReqVO request = new MesProWorkOrderSaveReqVO()
                .setCode("STAGE2_5-WO-" + safe)
                .setName(template.getName() + " Stage2.5模拟")
                .setType(MesProWorkOrderTypeEnum.SELF.getType())
                .setOrderSourceType(template.getOrderSourceType())
                .setOrderSourceCode("STAGE2_5:" + safe)
                .setProductId(template.getProductId())
                .setQuantity(template.getQuantity())
                .setQuantityProduced(BigDecimal.ZERO)
                .setQuantityChanged(BigDecimal.ZERO)
                .setQuantityScheduled(BigDecimal.ZERO)
                .setClientId(template.getClientId())
                .setVendorId(template.getVendorId())
                .setBatchCode("STAGE2_5-BATCH-" + safe)
                .setRequestDate(LocalDateTime.now())
                .setParentId(MesProWorkOrderDO.PARENT_ID_NULL)
                .setRemark(marker(runId, actorUserId));
        Long workOrderId = workOrderService.createWorkOrder(request);
        workOrderService.confirmWorkOrder(workOrderId);
        return workOrderService.validateWorkOrderExists(workOrderId);
    }

    private List<MesProcessPoolActiveOrderProcessSnapshotDO> cloneProcessSnapshots(
            Long templateActiveOrderId, Long targetActiveOrderId, Long targetWorkOrderId) {
        List<MesProcessPoolActiveOrderProcessSnapshotDO> source = processSnapshotMapper
                .selectListByActiveOrderIdForUpdate(templateActiveOrderId);
        if (source == null || source.isEmpty()) {
            throw new IllegalStateException("STAGE2_5_PROCESS_SNAPSHOT_REQUIRED");
        }
        List<MesProcessPoolActiveOrderProcessSnapshotDO> result = new ArrayList<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO row : source) {
            if (row.getRouteProcessId() == null || row.getProcessId() == null
                    || row.getPlannedQuantitySnapshot() == null
                    || row.getPlannedQuantitySnapshot().signum() <= 0) {
                throw new IllegalStateException("STAGE2_5_PROCESS_SNAPSHOT_INVALID");
            }
            MesProcessPoolActiveOrderProcessSnapshotDO copy = BeanUtils.toBean(row,
                            MesProcessPoolActiveOrderProcessSnapshotDO.class)
                    .setId(null)
                    .setActiveOrderId(targetActiveOrderId)
                    .setWorkOrderId(targetWorkOrderId);
            processSnapshotMapper.insert(copy);
            result.add(copy);
        }
        return result;
    }

    private void clonePqcTasks(Long templateActiveOrderId, MesProcessPoolActiveOrderDO activeOrder) {
        List<MesPqcInspectionTaskDO> source = pqcTaskMapper
                .selectListByActiveOrderIdForUpdate(templateActiveOrderId);
        if (source == null || source.isEmpty()) {
            throw new IllegalStateException("STAGE2_5_PQC_TASK_REQUIRED");
        }
        for (MesPqcInspectionTaskDO row : source) {
            MesPqcInspectionTaskDO copy = BeanUtils.toBean(row, MesPqcInspectionTaskDO.class)
                    .setId(null)
                    .setActiveOrderId(activeOrder.getId())
                    .setWorkOrderId(activeOrder.getWorkOrderId())
                    .setTaskStatus(MesPqcInspectionTaskDO.TASK_STATUS_PENDING)
                    .setActualInspectionQuantity(null)
                    .setSubmittedEventId(null)
                    .setSubmittedContentHash(null);
            pqcTaskMapper.insert(copy);
        }
    }

    private void clonePickList(MesProcessPoolActiveOrderPickListBindingDO source,
                               String workOrderCode, String runId, Long actorUserId) {
        ErpKingdeeProductionPickListDO sourceHeader = pickListMapper.selectById(source.getPickListId());
        List<ErpKingdeeProductionPickListItemDO> sourceItems = pickListItemMapper
                .selectListByPickListIds(List.of(source.getPickListId()));
        if (sourceHeader == null || sourceItems.isEmpty()) {
            throw new IllegalStateException("STAGE2_5_PICK_LIST_SOURCE_REQUIRED");
        }
        String safe = shortRunId(runId);
        ErpKingdeeProductionPickListDO header = BeanUtils.toBean(sourceHeader,
                        ErpKingdeeProductionPickListDO.class)
                .setId(null)
                .setSourceFid(sourceFid(runId, actorUserId))
                .setSourceBillNo("STAGE2_5-PL-" + safe)
                .setDocumentStatus("C")
                .setDescription("Stage2.5正式领料模拟");
        pickListMapper.insert(header);
        for (ErpKingdeeProductionPickListItemDO row : sourceItems) {
            pickListItemMapper.insert(BeanUtils.toBean(row, ErpKingdeeProductionPickListItemDO.class)
                    .setId(null)
                    .setProductionPickListId(header.getId())
                    .setSourceFid(sourceEntryFid(runId, actorUserId, row.getId()))
                    .setSourceEntryId("STAGE2_5-" + safe + "-ENTRY-" + row.getId())
                    .setSourceLineKey("STAGE2_5-" + safe + "-LINE-" + row.getId())
                    .setSourceBillNo(header.getSourceBillNo())
                    .setProductionOrderNo(workOrderCode));
        }
    }

    private void cloneBinding(MesProcessPoolActiveOrderPickListBindingDO source,
                              MesProcessPoolActiveOrderDO activeOrder,
                              MesProWorkOrderDO workOrder, String runId) {
        ErpKingdeeProductionPickListDO header = pickListMapper.selectOne(
                new LambdaQueryWrapper<ErpKingdeeProductionPickListDO>()
                        .eq(ErpKingdeeProductionPickListDO::getSourceBillNo,
                                "STAGE2_5-PL-" + shortRunId(runId)));
        if (header == null) {
            throw new IllegalStateException("STAGE2_5_PICK_LIST_CLONE_INVALID");
        }
        List<ErpKingdeeProductionPickListItemDO> items = pickListItemMapper
                .selectListByPickListIds(List.of(header.getId()));
        String snapshotHash = hash(List.of(header.getId(), header.getSourceFid(), header.getSourceBillNo(),
                header.getDocumentStatus(), items));
        MesProcessPoolActiveOrderPickListBindingDO copy = BeanUtils.toBean(source,
                MesProcessPoolActiveOrderPickListBindingDO.class)
                .setId(IdUtil.getSnowflake().nextId())
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(workOrder.getId())
                .setPickListId(header.getId())
                .setSourceFid(header.getSourceFid())
                .setSourceBillNo(header.getSourceBillNo())
                .setSourceDocumentStatus(header.getDocumentStatus())
                .setSourceModifyTime(header.getSourceModifyTime())
                .setSourceSnapshotHash(snapshotHash)
                .setBoundAt(LocalDateTime.now())
                .setIdempotencyKey("STAGE2_5-" + runId)
                .setBindingVersion(1);
        bindingMapper.insert(copy);
        for (ErpKingdeeProductionPickListItemDO item : items) {
            bindingItemMapper.insert(MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                    .id(IdUtil.getSnowflake().nextId())
                    .bindingId(copy.getId())
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
                    .build());
        }
    }

    private void requireTenant() {
        if (TenantContextHolder.getTenantId() == null || TenantContextHolder.getTenantId() <= 0) {
            throw new IllegalStateException("STAGE2_5_TENANT_REQUIRED");
        }
    }

    private String cleanupOwnedRuns(Long actorUserId) {
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectList(
                new LambdaQueryWrapper<MesProWorkOrderDO>()
                        .eq(MesProWorkOrderDO::getTenantId, TenantContextHolder.getTenantId())
                        .like(MesProWorkOrderDO::getRemark, SIMULATION_MARKER));
        String cleanedRunId = null;
        for (MesProWorkOrderDO workOrder : workOrders) {
            String runId = runIdFromMarker(workOrder.getRemark(), actorUserId);
            List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper
                    .selectHistoryByWorkOrderIdForUpdate(workOrder.getId());
            for (MesProcessPoolActiveOrderDO activeOrder : activeOrders) {
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
            cleanupBatchExecutions(workOrder.getId(), actorUserId);
            List<ErpKingdeeProductionPickListDO> pickLists = pickListMapper.selectList(
                    new LambdaQueryWrapper<ErpKingdeeProductionPickListDO>()
                            .eq(ErpKingdeeProductionPickListDO::getSourceBillNo,
                                    "STAGE2_5-PL-" + shortRunId(runId)));
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
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper
                .selectListByActiveOrderId(activeOrder.getId());
        Set<Long> taskIds = tasks.stream().map(MesPqcInspectionTaskDO::getId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Long> eventIds = new LinkedHashSet<>();
        allocationMapper.selectAllListByActiveOrderIdForUpdate(activeOrder.getId()).stream()
                .map(item -> item.getEventId()).filter(Objects::nonNull).forEach(eventIds::add);
        tasks.forEach(task -> {
            if (task.getSubmittedEventId() != null) {
                eventIds.add(task.getSubmittedEventId());
            }
            eventMapper.selectListPqcByTaskId("MES_PQC_INSPECTION_TASK", task.getId()).stream()
                    .map(MesProProcessPoolEventDO::getId).filter(Objects::nonNull).forEach(eventIds::add);
        });
        pqcAggregateDetailMapper.deleteByActiveOrderId(activeOrder.getId());
        pqcPieceDetailMapper.deleteByTaskIds(taskIds);
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
        pqcTaskMapper.deleteByActiveOrderId(activeOrder.getId());
        processSnapshotMapper.deleteByActiveOrderId(activeOrder.getId());
        releaseApplicationMapper.deleteByActiveOrderId(activeOrder.getId());
        if (!eventIds.isEmpty()) {
            eventMapper.deleteActiveOrderRuntimeEventsByIds(eventIds);
        }
    }

    private void cleanupBatchExecutions(Long workOrderId, Long actorUserId) {
        List<MesProEdhrBatchExecutionDO> batches = batchExecutionMapper.selectList(
                new LambdaQueryWrapper<MesProEdhrBatchExecutionDO>()
                        .eq(MesProEdhrBatchExecutionDO::getTenantId, TenantContextHolder.getTenantId())
                        .eq(MesProEdhrBatchExecutionDO::getWorkOrderId, workOrderId)
                        .like(MesProEdhrBatchExecutionDO::getRemark, SIMULATION_MARKER));
        for (MesProEdhrBatchExecutionDO batch : batches) {
            runIdFromMarker(batch.getRemark(), actorUserId);
            batchRecordExecutionMapper.delete(new LambdaQueryWrapper<MesProBatchRecordExecutionDO>()
                    .eq(MesProBatchRecordExecutionDO::getBatchExecutionId, batch.getId()));
            batchTaskMapper.selectListByBatchExecutionId(batch.getId())
                    .forEach(task -> batchTaskMapper.deleteById(task.getId()));
            batchExecutionMapper.deleteById(batch.getId());
        }
    }

    private void requireOwnedActiveOrder(MesProcessPoolActiveOrderDO activeOrder,
                                         MesStage2_5BackfillBatchExecutionSimulationCommand command) {
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), command.getActorUserId())
                || !Objects.equals(activeOrder.getTenantId(), TenantContextHolder.getTenantId())
                || activeOrder.getWorkOrderId() == null || activeOrder.getRouteId() == null
                || activeOrder.getRouteVersionId() == null) {
            throw new IllegalStateException("STAGE2_5_FIXTURE_INVALID");
        }
        if ("ACTIVE".equals(activeOrder.getActiveStatus())
                && !Objects.equals(activeOrder.getVersion(), command.getExpectedVersion())) {
            throw new IllegalStateException("ACTIVE_ORDER_COMPLETION_VERSION_CONFLICT");
        }
    }

    private void requireDoubleComplete(MesTeamLeaderActiveOrderSimulationResult simulation) {
        if (simulation == null || simulation.getProductionProgressPercent() == null
                || simulation.getInspectionProgressPercent() == null
                || simulation.getProductionProgressPercent().compareTo(BigDecimal.valueOf(100)) != 0
                || simulation.getInspectionProgressPercent().compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalStateException("STAGE2_5_FIXTURE_INVALID");
        }
    }

    private MesProWorkOrderDO requireWorkOrder(MesProcessPoolActiveOrderDO activeOrder) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(activeOrder.getWorkOrderId());
        if (workOrder == null || workOrder.getId() == null
                || !Objects.equals(workOrder.getTenantId(), TenantContextHolder.getTenantId())
                || workOrder.getProductId() == null || workOrder.getQuantity() == null
                || workOrder.getQuantity().signum() <= 0 || blank(workOrder.getCode())
                || blank(workOrder.getOrderSourceCode()) || blank(workOrder.getBatchCode())) {
            throw new IllegalStateException("STAGE2_5_FIXTURE_INVALID");
        }
        return workOrder;
    }

    private MesProcessPoolActiveOrderPickListBindingDO requireBinding(
            MesProcessPoolActiveOrderDO activeOrder) {
        MesProcessPoolActiveOrderPickListBindingDO binding = bindingMapper
                .selectByActiveOrderId(activeOrder.getId());
        if (binding == null || binding.getId() == null || binding.getPickListId() == null
                || binding.getBindingVersion() == null || blank(binding.getSourceSnapshotHash())) {
            throw new IllegalStateException("STAGE2_5_FIXTURE_INVALID");
        }
        return binding;
    }

    private MesCompletionBackfillReceipt buildBackfillReceipt(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            MesProcessPoolActiveOrderPickListBindingDO binding,
            List<MesProcessPoolActiveOrderPickListBindingItemDO> bindingItems,
            MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        if (receipt == null || !MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED
                .equals(receipt.getReceiptStatus())) {
            throw new IllegalStateException("ACTIVE_ORDER_COMPLETION_BACKFILL_NOT_IMPLEMENTED");
        }
        List<Long> productionIds = parseIds(receipt.getBatchRecordSourceIdsJson());
        List<Long> inspectionIds = parseIds(receipt.getProcessInspectionSourceIdsJson());
        if (productionIds.isEmpty() || inspectionIds.isEmpty()) {
            throw new IllegalStateException("ACTIVE_ORDER_COMPLETION_BACKFILL_NOT_IMPLEMENTED");
        }
        String lineHash = hash(bindingItems);
        String sourceVersion = "completion-" + receipt.getCompletedVersion()
                + "|pick-binding-" + binding.getBindingVersion();
        String sourceContextHash = hash(List.of(activeOrder.getTenantId(), activeOrder.getId(),
                workOrder.getId(), receipt.getBatchCode(), receipt.getRouteId(),
                receipt.getRouteVersionId(), binding.getId(), binding.getBindingVersion()));
        String sourceBundleHash = hash(List.of(receipt.getFormalSourceSnapshotJson(),
                receipt.getSignatureSnapshotJson(), binding.getSourceSnapshotHash(), lineHash));
        boolean hasLoss = Boolean.TRUE.equals(receipt.getHasActualLoss());
        String lossDecision = hasLoss ? "REQUIRED" : "NO_LOSS";
        String lossSourceId = hasLoss ? String.valueOf(receipt.getLossRecordId())
                : "NO_LOSS:" + activeOrder.getId();
        String lossPayload = hasLoss ? receipt.getLossConditionFactsJson()
                : receipt.getZeroLossConfirmationSnapshot();
        List<MesBatchExecutionSourceEvidence> evidence = List.of(
                evidence("PRODUCTION", String.valueOf(productionIds.get(0)), receipt, sourceVersion,
                        receipt.getFormalSourceSnapshotJson(), "PRODUCTION"),
                evidence("PQC", String.valueOf(inspectionIds.get(0)), receipt, sourceVersion,
                        receipt.getSignatureSnapshotJson(), "PQC"),
                evidence("LOSS", lossSourceId, receipt, sourceVersion, lossPayload, "LOSS"));
        String payloadHash = hash(List.of(sourceContextHash, sourceBundleHash, evidence,
                receipt.getReceiptHash(), receipt.getRequestIdempotencyKey()));
        return new MesCompletionBackfillReceipt()
                .setReceiptId(String.valueOf(receipt.getId()))
                .setTenantId(receipt.getTenantId())
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId())
                .setCompletionTransactionId("ACTIVE_ORDER_COMPLETION:" + receipt.getId())
                .setExpectedActiveOrderVersion(receipt.getExpectedVersion().longValue())
                .setSourceVersion(sourceVersion)
                .setPickListBindingId(binding.getId())
                .setPickListId(binding.getPickListId())
                .setBatchPickListRelationId(binding.getId())
                .setSourceContextHash(sourceContextHash)
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setPickListHeaderSnapshotHash(binding.getSourceSnapshotHash())
                .setPickListLineSnapshotHash(lineHash)
                .setSourceBundleHash(sourceBundleHash)
                .setBindingVersion(binding.getBindingVersion().longValue())
                .setProductionProgress(100)
                .setInspectionProgress(100)
                .setCompletionVersion(receipt.getCompletedVersion().longValue())
                .setCompletionEventId("ACTIVE_ORDER_COMPLETION:" + receipt.getId())
                .setBatchRecordId(productionIds.get(0))
                .setProcessInspectionId(inspectionIds.get(0))
                .setHasActualLoss(hasLoss)
                .setLossDecision(lossDecision)
                .setLossReportStatus(receipt.getLossReportStatus())
                .setLossRecordId(receipt.getLossRecordId())
                .setLossQuantity(receipt.getLossQuantity())
                .setStatus("BACKFILL_SUCCEEDED")
                .setReceiptVersion("1")
                .setReceiptHash(receipt.getReceiptHash())
                .setProductionBackfillStatus("BACKFILL_SUCCEEDED")
                .setInspectionBackfillStatus("BACKFILL_SUCCEEDED")
                .setLossBackfillStatus(hasLoss ? "BACKFILL_SUCCEEDED" : "NO_LOSS")
                .setPayloadHash(payloadHash)
                .setAuditEventId("ACTIVE_ORDER_COMPLETION_RECEIPT:" + receipt.getId())
                .setIdempotencyKey(receipt.getRequestIdempotencyKey())
                .setSourceEvidence(evidence);
    }

    private MesBatchExecutionSourceEvidence evidence(String type, String sourceId,
                                                     MesProcessPoolActiveOrderCompletionReceiptDO receipt,
                                                     String sourceVersion, String payload, String signatureType) {
        return new MesBatchExecutionSourceEvidence()
                .setSourceType(type)
                .setSourceId(sourceId)
                .setSourceVersion(sourceVersion)
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setPayloadHash(hash(payload))
                .setSignature(hash(receipt.getSignatureSnapshotJson() + "|" + signatureType));
    }

    private EdhrBatchExecutionOpenOrCreateReqVO buildBatchRequest(
            MesStage2_5BackfillBatchExecutionSimulationCommand command,
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            MesProcessPoolActiveOrderPickListBindingDO binding,
            MesCompletionBackfillReceipt receipt) {
        String batchIdempotencyKey = "STAGE2_5-BATCH-" + command.getSimulationRunId();
        return new EdhrBatchExecutionOpenOrCreateReqVO()
                .setWorkOrderId(workOrder.getId())
                .setWorkOrderCode(workOrder.getCode())
                .setTenantId(TenantContextHolder.getTenantId())
                .setBatchCode(receipt.getBatchCode())
                .setRouteId(receipt.getRouteId())
                .setRouteVersionId(receipt.getRouteVersionId())
                .setRemark(marker(command.getSimulationRunId(), command.getActorUserId()))
                .setEntryType(ENTRY_TYPE)
                .setEntryBusinessId(receipt.getCompletionTransactionId())
                .setSourceCredentialType(CREDENTIAL_TYPE)
                .setSourceCredentialId(receipt.getReceiptId())
                .setSourceContextHash(receipt.getSourceContextHash())
                .setActiveOrderId(activeOrder.getId())
                .setPickListBindingId(binding.getId())
                .setPickListId(binding.getPickListId())
                .setBindingVersion(receipt.getBindingVersion())
                .setBatchPickListRelationId(receipt.getBatchPickListRelationId())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setCompletionTransactionId(receipt.getCompletionTransactionId())
                .setExpectedActiveOrderVersion(receipt.getExpectedActiveOrderVersion())
                .setCompletionVersion(receipt.getCompletionVersion())
                .setSourceVersion(receipt.getSourceVersion())
                .setSourceBundleHash(receipt.getSourceBundleHash())
                .setCompletionBackfillReceiptId(receipt.getReceiptId())
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                .setPickListHeaderSnapshotHash(receipt.getPickListHeaderSnapshotHash())
                .setPickListLineSnapshotHash(receipt.getPickListLineSnapshotHash())
                .setSourceEvidence(receipt.getSourceEvidence())
                .setIdempotencyKey(batchIdempotencyKey)
                .setExpectedSourceVersion(receipt.getSourceVersion())
                .setPayloadHash(receipt.getPayloadHash())
                .setCompletionBackfillReceipt(receipt);
    }

    private Map<String, Object> buildSnapshot(
            MesStage2_5BackfillBatchExecutionSimulationCommand command,
            MesProcessPoolActiveOrderDO activeOrder,
            MesProWorkOrderDO workOrder,
            MesProcessPoolActiveOrderPickListBindingDO binding,
            List<MesProcessPoolActiveOrderPickListBindingItemDO> bindingItems,
            MesProcessPoolActiveOrderCompletionReceiptDO receipt,
            EdhrBatchExecutionRespVO batch,
            MesCompletionBackfillReceipt backfillReceipt) {
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper
                .selectListByBatchExecutionId(batch.getId());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schemaVersion", MesStage4DossierUploadSimulationContractValidator.SCHEMA_VERSION);
        snapshot.put("simulationRunId", command.getSimulationRunId());
        snapshot.put("sourceInputContract", "backfillResultSnapshot.v1");
        snapshot.put("batchExecutionId", String.valueOf(batch.getId()));
        snapshot.put("batchExecutionCode", batch.getBatchExecutionCode());
        snapshot.put("batchCode", batch.getBatchCode());
        snapshot.put("activeContextKey", activeContextKey(batch));
        snapshot.put("activeOrderId", String.valueOf(activeOrder.getId()));
        snapshot.put("activeOrderCode", String.valueOf(activeOrder.getId()));
        snapshot.put("workOrderId", String.valueOf(workOrder.getId()));
        snapshot.put("workOrderCode", workOrder.getCode());
        snapshot.put("erpWorkOrderNo", workOrder.getOrderSourceCode());
        snapshot.put("routeId", String.valueOf(activeOrder.getRouteId()));
        snapshot.put("routeVersionId", String.valueOf(activeOrder.getRouteVersionId()));
        snapshot.put("routeVersionNo", batch.getRouteVersionNo());
        Map<String, Object> materialIssueSource = new LinkedHashMap<>();
        materialIssueSource.put("bindingId", String.valueOf(binding.getId()));
        materialIssueSource.put("pickListId", String.valueOf(binding.getPickListId()));
        materialIssueSource.put("sourceFid", binding.getSourceFid());
        materialIssueSource.put("sourceBillNo", binding.getSourceBillNo());
        materialIssueSource.put("sourceSnapshotHash", binding.getSourceSnapshotHash());
        materialIssueSource.put("bindingVersion", binding.getBindingVersion());
        materialIssueSource.put("items", bindingItems.stream().map(this::toMaterialIssueItem).toList());
        snapshot.put("materialIssueSource", materialIssueSource);
        snapshot.put("batchRecordLinks", linkList(List.of(requiredEvidenceId(
                backfillReceipt.getBatchRecordId(), "batchRecordId")),
                receipt, "FORMAL_PRODUCTION_CONFIRMATION", "BATCH_RECORD"));
        snapshot.put("processInspectionLinks", linkList(List.of(requiredEvidenceId(
                backfillReceipt.getProcessInspectionId(), "processInspectionId")),
                receipt, "CONFIRMED_PQC_AGGREGATION_DETAIL", "PROCESS_INSPECTION"));
        boolean hasLoss = Boolean.TRUE.equals(receipt.getHasActualLoss());
        snapshot.put("hasLoss", hasLoss);
        Map<String, Object> lossRequirement = new LinkedHashMap<>();
        lossRequirement.put("required", hasLoss);
        lossRequirement.put("status", hasLoss ? "COMPLETED" : "NOT_REQUIRED");
        lossRequirement.put("sourceHash", hasLoss ? receipt.getLossSourceHash() : "not-required:no-loss");
        snapshot.put("lossReportRequirement", lossRequirement);
        snapshot.put("optionalLossReportLinks", hasLoss
                ? linkList(List.of(requiredEvidenceId(backfillReceipt.getLossRecordId(), "lossRecordId")),
                receipt, "FORMAL_LOSS_SOURCE", "LOSS_REPORT")
                : List.of());
        snapshot.put("specialNodeUploadStatus", specialNodeStatus(tasks));
        Map<String, Object> hashes = new LinkedHashMap<>();
        hashes.put("stage3BatchExecutionSnapshot", hash(snapshotWithoutHash(snapshot)));
        hashes.put("backfillResultSnapshot", receipt.getSourceSnapshotHash());
        hashes.put("materialIssueSource", hash(materialIssueSource));
        hashes.put("batchRecordLinks", hash(snapshot.get("batchRecordLinks")));
        hashes.put("processInspectionLinks", hash(snapshot.get("processInspectionLinks")));
        hashes.put("optionalLossReportLinks", hasLoss
                ? hash(snapshot.get("optionalLossReportLinks")) : "not-required:no-loss");
        hashes.put("specialNodeUploadStatus", hash(snapshot.get("specialNodeUploadStatus")));
        snapshot.put("sourceHash", hashes);
        snapshot.put("status", "DOSSIER_UPLOAD_PENDING");
        snapshot.put("blockers", List.of());
        return snapshot;
    }

    private Long requiredEvidenceId(Long id, String field) {
        if (id == null || id <= 0) {
            throw new IllegalStateException("COMPLETION_RECEIPT_EVIDENCE_MISSING:" + field);
        }
        return id;
    }

    private Map<String, Object> specialNodeStatus(List<MesProEdhrBatchExecutionTaskDO> tasks) {
        List<String> nodeTypes = List.of(
                "INCOMING_INSPECTION_REPORT", "STERILIZATION_REPORT",
                "FINISHED_PRODUCT_INSPECTION_REPORT", "FINISHED_PRODUCT_INSPECTION_RECORD");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overallStatus", "PENDING_UPLOAD");
        List<Map<String, Object>> requiredNodes = new ArrayList<>();
        for (String nodeType : nodeTypes) {
            MesProEdhrBatchExecutionTaskDO task = tasks.stream()
                    .filter(item -> Objects.equals(nodeType, item.getNodeType()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("BATCH_EXECUTION_DOSSIER_VISIBILITY_MISSING"));
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("nodeType", nodeType);
            node.put("nodeName", task.getProcessName());
            node.put("batchExecutionTaskId", String.valueOf(task.getId()));
            node.put("status", "PENDING_UPLOAD");
            node.put("savedAttachmentCount", 0);
            requiredNodes.add(node);
        }
        result.put("requiredNodes", requiredNodes);
        return result;
    }

    private List<Map<String, Object>> linkList(List<Long> ids,
                                                MesProcessPoolActiveOrderCompletionReceiptDO receipt,
                                                String sourceType, String category) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            throw new IllegalStateException("BATCH_EXECUTION_DOSSIER_VISIBILITY_MISSING");
        }
        for (Long id : ids) {
            Map<String, Object> link = new LinkedHashMap<>();
            link.put("backfillStatus", "COMPLETED");
            link.put("sourceType", sourceType);
            link.put("recordCategory", category);
            link.put(category.equals("BATCH_RECORD") ? "batchRecordId" : "processInspectionId",
                    String.valueOf(id));
            link.put("sourceHash", receipt.getSourceSnapshotHash());
            result.add(link);
        }
        return result;
    }

    private Map<String, Object> toMaterialIssueItem(MesProcessPoolActiveOrderPickListBindingItemDO item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bindingItemId", String.valueOf(item.getId()));
        result.put("pickListItemId", String.valueOf(item.getPickListItemId()));
        result.put("materialNumber", item.getMaterialNumber());
        result.put("materialName", item.getMaterialName());
        result.put("requestedQuantity", item.getRequestedQuantity());
        result.put("actualQuantity", item.getActualQuantity());
        result.put("lotNumber", item.getLotNumber());
        result.put("sourceEntryId", item.getSourceEntryId());
        result.put("itemSnapshotHash", item.getItemSnapshotHash());
        return result;
    }

    private Map<String, Object> snapshotWithoutHash(Map<String, Object> snapshot) {
        Map<String, Object> copy = new LinkedHashMap<>(snapshot);
        copy.remove("sourceHash");
        return copy;
    }

    private String activeContextKey(EdhrBatchExecutionRespVO batch) {
        return batch.getWorkOrderId() + "|" + batch.getRouteId() + "|" + batch.getBatchCode();
    }

    private List<Long> parseIds(String json) {
        if (blank(json)) {
            return List.of();
        }
        List<Long> ids = JSON.parseArray(json, Long.class);
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).toList();
    }

    private String completionIdempotencyKey(String runId) {
        return "STAGE2_5-COMPLETE-" + runId;
    }

    private String marker(String runId, Long actorUserId) {
        return SIMULATION_MARKER + "[simulationRunId=" + runId + "][actorUserId=" + actorUserId + "]";
    }

    private String runIdFromMarker(String value, Long actorUserId) {
        String prefix = SIMULATION_MARKER + "[simulationRunId=";
        String actorToken = "][actorUserId=" + actorUserId + "]";
        if (value == null || !value.startsWith(prefix) || !value.endsWith(actorToken)) {
            throw new IllegalStateException("STAGE2_5_CLEANUP_SCOPE_INVALID");
        }
        String runId = value.substring(prefix.length(), value.length() - actorToken.length());
        if (!runId.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalStateException("STAGE2_5_CLEANUP_SCOPE_INVALID");
        }
        return runId;
    }

    private String shortRunId(String runId) {
        String value = runId.replaceAll("[^A-Za-z0-9]", "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("simulationRunId contains no usable characters");
        }
        return value.length() <= 32 ? value : value.substring(value.length() - 32);
    }

    private String sourceFid(String runId, Long actorUserId) {
        return "S25-" + DigestUtil.sha256Hex(runId + "|" + actorUserId).substring(0, 60);
    }

    private String sourceEntryFid(String runId, Long actorUserId, Long sourceItemId) {
        return "S25E-" + DigestUtil.sha256Hex(runId + "|" + actorUserId + "|" + sourceItemId).substring(0, 59);
    }

    private String hash(Object value) {
        return DigestUtil.sha256Hex(value == null ? "null" : JsonUtils.toJsonString(value));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
