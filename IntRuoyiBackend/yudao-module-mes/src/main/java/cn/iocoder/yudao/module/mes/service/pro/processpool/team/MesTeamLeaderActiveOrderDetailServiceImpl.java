package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderEventPartyReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderDetailReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.PqcResultValueValidator;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesTeamLeaderActiveOrderDetailServiceImpl implements MesTeamLeaderActiveOrderDetailService {

    private static final Function<MesTeamLeaderActiveOrderDetailReadDO, BigDecimal> READ_ROW_REQUIRED_QUANTITY =
            MesTeamLeaderActiveOrderDetailReadDO::getRequiredQuantity;
    private static final Function<MesTeamLeaderActiveOrderDetail.ProcessDetail, BigDecimal> READ_DETAIL_REQUIRED_QUANTITY =
            MesTeamLeaderActiveOrderDetail.ProcessDetail::getRequiredQuantity;

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderDetailReadMapper detailReadMapper;
    private final MesFrontlineProcessMaterialService processMaterialService;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper;
    private final MesQaInspectionRegulationProcessMapper qaProcessMapper;
    private final ErpKingdeeProductionReplenishmentListItemMapper replenishmentListItemMapper;
    private final ErpKingdeeProductionReplenishmentListMapper replenishmentListMapper;
    private final ErpKingdeeProductionPickListMapper pickListMapper;
    private final ErpKingdeeProductionPickListItemMapper pickListItemMapper;
    private final MesMdItemMapper itemMapper;
    private final MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolEventRevisionMapper eventRevisionMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    private final MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper;
    private final MesProEdhrOperationAuditEventMapper operationAuditEventMapper;
    private final MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper maintenanceAuditMapper;
    private final ElectronicSignatureQueryService signatureQueryService;
    private final AdminUserService adminUserService;

    public MesTeamLeaderActiveOrderDetailServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                                       MesProcessPoolActiveOrderDetailReadMapper detailReadMapper,
                                                       MesFrontlineProcessMaterialService processMaterialService,
                                                       MesPqcInspectionTaskMapper pqcTaskMapper,
                                                       MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper,
                                                       MesQaInspectionRegulationProcessMapper qaProcessMapper,
                                                       ErpKingdeeProductionReplenishmentListItemMapper replenishmentListItemMapper,
                                                       ErpKingdeeProductionReplenishmentListMapper replenishmentListMapper,
                                                       ErpKingdeeProductionPickListMapper pickListMapper,
                                                       ErpKingdeeProductionPickListItemMapper pickListItemMapper,
                                                       MesMdItemMapper itemMapper,
                                                       MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper,
                                                       MesProProcessPoolEventMapper eventMapper,
                                                       MesProProcessPoolEventRevisionMapper eventRevisionMapper,
                                                       MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
                                                       MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper,
                                                       MesProEdhrOperationAuditEventMapper operationAuditEventMapper,
                                                       MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
                                                       MesProcessPoolTeamMaintenanceAuditMapper maintenanceAuditMapper,
                                                       ElectronicSignatureQueryService signatureQueryService,
                                                       AdminUserService adminUserService) {
        this.activeOrderMapper = activeOrderMapper;
        this.detailReadMapper = detailReadMapper;
        this.processMaterialService = processMaterialService;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pqcAggregateDetailMapper = pqcAggregateDetailMapper;
        this.qaProcessMapper = qaProcessMapper;
        this.replenishmentListItemMapper = replenishmentListItemMapper;
        this.replenishmentListMapper = replenishmentListMapper;
        this.pickListMapper = pickListMapper;
        this.pickListItemMapper = pickListItemMapper;
        this.itemMapper = itemMapper;
        this.backfillMapper = backfillMapper;
        this.eventMapper = eventMapper;
        this.eventRevisionMapper = eventRevisionMapper;
        this.releaseApplicationMapper = releaseApplicationMapper;
        this.nonconformanceReviewMapper = nonconformanceReviewMapper;
        this.operationAuditEventMapper = operationAuditEventMapper;
        this.releaseTransactionMapper = releaseTransactionMapper;
        this.maintenanceAuditMapper = maintenanceAuditMapper;
        this.signatureQueryService = signatureQueryService;
        this.adminUserService = adminUserService;
    }

    @Override
    public MesTeamLeaderActiveOrderDetail getDetail(Long leaderUserId, Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        if (activeOrder == null
                || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)
                || !MesTeamLeaderActiveOrderServiceImpl.STATUS_ACTIVE.equals(activeOrder.getActiveStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        return buildDetail(activeOrder, activeOrderId, false);
    }

    @Override
    public MesTeamLeaderActiveOrderDetail getFormalDetail(Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        if (activeOrder == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        return buildDetail(activeOrder, activeOrderId, false);
    }

    @Override
    public MesTeamLeaderActiveOrderDetail getArchivedFormalDetail(Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdIgnoreDeleted(activeOrderId);
        if (activeOrder == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        return buildDetail(activeOrder, activeOrderId, true);
    }

    private MesTeamLeaderActiveOrderDetail buildDetail(MesProcessPoolActiveOrderDO activeOrder, Long activeOrderId,
                                                       boolean archivedFormalSource) {
        List<MesTeamLeaderActiveOrderDetailReadDO> rows = detailReadMapper.selectByActiveOrderId(activeOrderId);
        if (rows == null || rows.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        MesTeamLeaderActiveOrderDetailReadDO first = rows.get(0);
        requireText(first.getWorkOrderCode(), activeOrderId);
        requireText(first.getRouteName(), activeOrderId);

        Map<ProcessIdentity, ProcessAccumulator> accumulators = new LinkedHashMap<>();
        for (MesTeamLeaderActiveOrderDetailReadDO row : rows) {
            validateProcessRow(row, activeOrderId);
            ProcessIdentity identity = new ProcessIdentity(row.getRouteProcessId(), row.getProcessId());
            ProcessAccumulator accumulator = accumulators.computeIfAbsent(identity,
                    ignored -> new ProcessAccumulator(row));
            accumulator.addSubmission(row, activeOrderId);
        }
        FormalInputSourceSnapshot inputSourceSnapshot = resolveInputSourceSnapshot(activeOrder, activeOrderId);
        attachInputMaterials(activeOrder, activeOrderId, accumulators, inputSourceSnapshot, archivedFormalSource);
        attachSupplementMaterials(first.getWorkOrderCode(), activeOrderId, accumulators);
        attachPqcSubmissions(activeOrder, activeOrderId, accumulators);
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                releaseApplicationMapper.selectLatestByActiveOrderId(activeOrderId);
        MesTeamLeaderActiveOrderDetail detail = new MesTeamLeaderActiveOrderDetail()
                .setActiveOrderId(activeOrderId)
                .setVersion(activeOrder.getVersion())
                .setWorkOrderId(first.getWorkOrderId())
                .setWorkOrderCode(first.getWorkOrderCode())
                .setBatchCode(first.getBatchCode())
                .setWorkOrderQuantity(first.getWorkOrderQuantity())
                .setDemandBillNo(first.getDemandBillNo())
                .setDrawingNumber(first.getDrawingNumber())
                .setProductCode(first.getProductCode())
                .setProductName(first.getProductName())
                .setProductSpecification(first.getProductSpecification())
                .setUdiControlDocumentNo(activeOrder.getUdiControlDocumentNo())
                .setWorkOrderCreateTime(first.getWorkOrderCreateTime())
                .setRouteName(first.getRouteName())
                .setInputMaterialUsages(resolveInputMaterialUsages(inputSourceSnapshot))
                .setProcesses(accumulators.values().stream().map(ProcessAccumulator::toDetail).toList())
                .setActiveOrderStatus(resolveActiveOrderStatus(application))
                .setOperationFacts(resolveOperationFacts(activeOrderId));
        attachPickListMetadata(detail, activeOrderId);
        attachPqcProductionRelease(detail, application);
        return detail;
    }

    private MesTeamLeaderActiveOrderDetail.ActiveOrderStatusSummary resolveActiveOrderStatus(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (application == null) {
            return new MesTeamLeaderActiveOrderDetail.ActiveOrderStatusSummary()
                    .setStatus("NOT_APPLIED")
                    .setStatusLabel("未申请");
        }
        String status = application.getApplicationStatus();
        if (MesReleaseFlowStatus.PQC_RELEASE_REJECTED.equals(status)) {
            MesProEdhrNonconformanceReviewDO review =
                    nonconformanceReviewMapper.selectLatestBySource("PQC_RELEASE", application.getId());
            if (review != null && "closed".equals(review.getReviewStatus())
                    && "void".equals(review.getDisposition())) {
                return new MesTeamLeaderActiveOrderDetail.ActiveOrderStatusSummary()
                        .setStatus("VOIDED")
                        .setStatusLabel("已作废");
            }
        }
        String label = switch (status) {
            case MesReleaseFlowStatus.PQC_RELEASE_PENDING -> "待PQC放行";
            case MesReleaseFlowStatus.PQC_RELEASE_REJECTED -> "待审查";
            case MesReleaseFlowStatus.REPORT_UPLOAD_PENDING, MesReleaseFlowStatus.MANAGER_RELEASE_PENDING ->
                    "待上市放行";
            case MesReleaseFlowStatus.RELEASED -> "已上市放行";
            default -> throw new IllegalStateException("ACTIVE_ORDER_RELEASE_STATUS_INVALID: " + status);
        };
        return new MesTeamLeaderActiveOrderDetail.ActiveOrderStatusSummary()
                .setStatus(status)
                .setStatusLabel(label);
    }

    private List<MesTeamLeaderActiveOrderDetail.OperationFact> resolveOperationFacts(Long activeOrderId) {
        List<MesProEdhrOperationAuditEventDO> events =
                operationAuditEventMapper.selectSuccessfulListByActiveOrderId(activeOrderId);
        List<MesProcessPoolTeamMaintenanceAuditDO> maintenanceAudits =
                maintenanceAuditMapper.selectSuccessfulListByActiveOrderId(activeOrderId);
        List<MesTeamLeaderActiveOrderDetail.OperationFact> facts = new ArrayList<>();
        if (events != null) {
            facts.addAll(events.stream().map(event -> toOperationFact(event, activeOrderId)).toList());
        }
        if (maintenanceAudits != null) {
            facts.addAll(maintenanceAudits.stream()
                    .map(audit -> toMaintenanceOperationFact(audit, activeOrderId))
                    .toList());
        }
        return facts.stream()
                .sorted(Comparator.comparing(MesTeamLeaderActiveOrderDetail.OperationFact::getOccurredAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MesTeamLeaderActiveOrderDetail.OperationFact::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private MesTeamLeaderActiveOrderDetail.OperationFact toMaintenanceOperationFact(
            MesProcessPoolTeamMaintenanceAuditDO audit, Long activeOrderId) {
        if (audit == null || audit.getId() == null || audit.getAuditTime() == null
                || audit.getOperatorUserId() == null || audit.getResultStatus() == null
                || !Objects.equals(activeOrderId, audit.getTargetId())
                || !"ACTIVE_ORDER".equals(audit.getTargetType())) {
            throw new IllegalStateException("ACTIVE_ORDER_OPERATION_FACT_INCOMPLETE");
        }
        AdminUserDO operator = adminUserService.getUser(audit.getOperatorUserId());
        if (operator == null || operator.getNickname() == null || operator.getNickname().isBlank()) {
            throw new IllegalStateException("ACTIVE_ORDER_OPERATION_FACT_ACTOR_MISSING");
        }
        return new MesTeamLeaderActiveOrderDetail.OperationFact()
                .setId(audit.getId())
                .setOperationType(requireTextValue(audit.getActionType(), activeOrderId))
                .setOperationName(requireTextValue(audit.getChangeSummary(), activeOrderId))
                .setSourceType(requireTextValue(audit.getTargetType(), activeOrderId))
                .setSourceId(String.valueOf(audit.getTargetId()))
                .setActorUserId(audit.getOperatorUserId())
                .setActorName(operator.getNickname())
                .setResultStatus(audit.getResultStatus())
                .setOccurredAt(audit.getAuditTime());
    }

    private MesTeamLeaderActiveOrderDetail.OperationFact toOperationFact(
            MesProEdhrOperationAuditEventDO event, Long activeOrderId) {
        if (event == null || event.getId() == null || event.getOccurredAt() == null
                || event.getActorUserId() == null || event.getResultStatus() == null
                || event.getAfterSummaryHash() == null || event.getAfterSummaryHash().isBlank()
                || event.getMetadataJson() == null || event.getMetadataJson().isBlank()) {
            throw new IllegalStateException("ACTIVE_ORDER_OPERATION_FACT_INCOMPLETE"
                    + " eventId=" + (event == null ? null : event.getId())
                    + " objectType=" + (event == null ? null : event.getObjectType())
                    + " objectId=" + (event == null ? null : event.getObjectId())
                    + " operationType=" + (event == null ? null : event.getOperationType())
                    + " actorUserId=" + (event == null ? null : event.getActorUserId())
                    + " resultStatus=" + (event == null ? null : event.getResultStatus())
                    + " occurredAt=" + (event == null ? null : event.getOccurredAt())
                    + " afterSummaryHashPresent=" + (event != null
                    && event.getAfterSummaryHash() != null
                    && !event.getAfterSummaryHash().isBlank())
                    + " metadataPresent=" + (event != null
                    && event.getMetadataJson() != null
                    && !event.getMetadataJson().isBlank()));
        }
        JSONObject metadata = JSON.parseObject(event.getMetadataJson());
        Long metadataActiveOrderId = metadata == null ? null : metadata.getLong("activeOrderId");
        if (!Objects.equals(metadataActiveOrderId, activeOrderId)) {
            throw new IllegalStateException("ACTIVE_ORDER_OPERATION_FACT_SOURCE_MISMATCH");
        }
        Long signatureId = signatureIdOf(metadata);
        if (Objects.equals(event.getOperationType(),
                cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditEventType
                        .BATCH_RECORD_RELEASE_APPROVED)) {
            Long batchExecutionId = event.getBatchExecutionId();
            if (batchExecutionId == null || batchExecutionId <= 0) {
                throw new IllegalStateException("ACTIVE_ORDER_MARKET_RELEASE_BATCH_EXECUTION_ID_MISSING");
            }
            MesProEdhrReleaseTransactionDO transaction =
                    releaseTransactionMapper.selectByBatchExecutionId(batchExecutionId);
            if (transaction == null || transaction.getId() == null) {
                throw new IllegalStateException("ACTIVE_ORDER_MARKET_RELEASE_TRANSACTION_MISSING");
            }
            if (!Objects.equals(batchExecutionId, transaction.getBatchExecutionId())) {
                throw new IllegalStateException("ACTIVE_ORDER_MARKET_RELEASE_TRANSACTION_BATCH_MISMATCH");
            }
            Long metadataReleaseTransactionId = metadata == null ? null : metadata.getLong("releaseTransactionId");
            if (metadataReleaseTransactionId != null
                    && !Objects.equals(metadataReleaseTransactionId, transaction.getId())) {
                throw new IllegalStateException("ACTIVE_ORDER_MARKET_RELEASE_TRANSACTION_ID_MISMATCH");
            }
            signatureId = transaction.getApprovalSignatureId();
        }
        return new MesTeamLeaderActiveOrderDetail.OperationFact()
                .setId(event.getId())
                .setOperationType(requireTextValue(event.getOperationType(), activeOrderId))
                .setOperationName(requireTextValue(event.getActionName(), activeOrderId))
                .setSourceType(requireTextValue(event.getObjectType(), activeOrderId))
                .setSourceId(requireTextValue(event.getObjectId(), activeOrderId))
                .setActorUserId(event.getActorUserId())
                .setActorName(requireTextValue(event.getActorUsername(), activeOrderId))
                .setSignatureId(signatureId)
                .setNonconformanceReason(metadata.getString("nonconformanceReason"))
                .setReviewMaterialUrl(metadata.getString("reviewMaterialUrl"))
                .setReviewMaterialFileId(reviewMaterialFileIdOf(metadata))
                .setReviewMaterialsJson(metadata.getString("reviewMaterialsJson"))
                .setReviewOpinion(metadata.getString("reviewOpinion"))
                .setDisposition(metadata.getString("disposition"))
                .setQaSignature(metadata.getString("qaSignature"))
                .setQaUserId(metadata.getLong("qaUserId"))
                .setResultStatus(event.getResultStatus())
                .setOccurredAt(event.getOccurredAt())
                .setSourceSnapshotHash(event.getAfterSummaryHash());
    }

    private Long reviewMaterialFileIdOf(JSONObject metadata) {
        if (metadata == null) {
            return null;
        }
        Long fileId = metadata.getLong("reviewMaterialFileId");
        if (fileId != null && fileId > 0) {
            return fileId;
        }
        String materialsJson = metadata.getString("reviewMaterialsJson");
        if (materialsJson == null || materialsJson.isBlank()) {
            return null;
        }
        JSONObject payload = JSON.parseObject(materialsJson);
        JSONArray activeMaterials = payload == null ? null : payload.getJSONArray("activeMaterials");
        if (activeMaterials == null || activeMaterials.isEmpty()) {
            return null;
        }
        JSONObject firstMaterial = activeMaterials.getJSONObject(0);
        Long parsedFileId = firstMaterial == null ? null : firstMaterial.getLong("fileId");
        return parsedFileId != null && parsedFileId > 0 ? parsedFileId : null;
    }

    private Long signatureIdOf(JSONObject metadata) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get("signatureId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank() && !"NOT_APPLICABLE".equals(text)) {
            return Long.valueOf(text);
        }
        return null;
    }

    private void attachPqcProductionRelease(MesTeamLeaderActiveOrderDetail detail,
                                            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (application == null
                || !"APPROVE".equals(application.getPqcDecision())
                || application.getBatchExecutionId() == null
                || application.getBatchExecutionId() <= 0
                || application.getDossierSummaryJson() == null
                || application.getDossierSummaryJson().isBlank()) {
            return;
        }
        JSONObject dossier = JSON.parseObject(application.getDossierSummaryJson());
        Long signatureId = dossier == null ? null : dossier.getLong("signatureId");
        if (signatureId == null || signatureId <= 0) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_ID_MISSING");
        }
        String subjectId = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                application.getBatchExecutionId(),
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                null, null, null, null, null, null, null,
                "PQC_RELEASE_APPLICATION", application.getId(), "PQC生产放行",
                MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                null, null, null, null);
        ElectronicSignatureEvidenceDTO signature = signatureQueryService.getById(signatureId);
        if (signature == null
                || !Objects.equals(signature.subjectId(), subjectId)
                || !Objects.equals(signature.actionCode(), MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE)
                || !Objects.equals(signature.verificationStatus(), "VALID")
                || signature.actorId() == null
                || signature.signedAt() == null) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_RECORD_MISSING");
        }
        ElectronicSignatureVerificationDTO verification = signatureQueryService.verifyEvidence(signatureId);
        if (verification == null
                || !Objects.equals(verification.signatureId(), signatureId)
                || !Objects.equals(verification.verificationStatus(), "VALID")
                || !Objects.equals(verification.storedEvidenceHash(), signature.evidenceHash())
                || !Objects.equals(verification.calculatedEvidenceHash(), signature.evidenceHash())) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_RECORD_MISSING");
        }
        AdminUserDO signer = adminUserService.getUser(signature.actorId());
        if (signer == null || signer.getNickname() == null || signer.getNickname().isBlank()) {
            throw new IllegalStateException("PQC_RELEASE_SIGNATURE_ACTOR_MISSING");
        }
        detail.setPqcProductionRelease(new MesTeamLeaderActiveOrderDetail.PqcProductionReleaseSummary()
                .setStatus(application.getApplicationStatus())
                .setStatusLabel("已生产放行")
                .setSignature(new MesTeamLeaderActiveOrderDetail.SignatureDetail()
                        .setSignatureId(signature.id())
                        .setSignerName(signer.getNickname())
                        .setSignedAt(signature.signedAt())
                        .setRole(MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE)));
    }

    private FormalInputSourceSnapshot resolveInputSourceSnapshot(MesProcessPoolActiveOrderDO activeOrder,
                                                                 Long activeOrderId) {
        MesProcessPoolActiveOrderCompletionBackfillDO backfill = backfillMapper.selectByActiveOrderAndType(
                activeOrderId, MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD);
        return readCompletedSources(activeOrder, backfill);
    }

    private void attachInputMaterials(MesProcessPoolActiveOrderDO activeOrder, Long activeOrderId,
                                       Map<ProcessIdentity, ProcessAccumulator> accumulators,
                                       FormalInputSourceSnapshot inputSourceSnapshot,
                                       boolean archivedFormalSource) {
        FormalInputSourceSnapshot resolvedInputSourceSnapshot = inputSourceSnapshot;
        for (ProcessAccumulator accumulator : accumulators.values()) {
            MesTeamLeaderActiveOrderDetail.ProcessDetail process = accumulator.process;
            List<MesFrontlineProcessMaterial> frozenMaterials = archivedFormalSource
                    ? processMaterialService.listArchivedFrozenMaterials(activeOrderId, activeOrder.getRouteId(),
                    process.getRouteProcessId(), process.getProcessId())
                    : processMaterialService.listFrozenMaterials(activeOrderId, activeOrder.getRouteId(),
                    process.getRouteProcessId(), process.getProcessId());
            List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> inputMaterials =
                    frozenMaterials.stream()
                            .filter(material -> MesFrontlineProcessMaterial.ROLE_INPUT.equals(material.materialRole()))
                            .map(material -> toCompletedInputMaterialDetail(material, resolvedInputSourceSnapshot))
                            .toList();
            accumulator.setInputMaterials(inputMaterials);
        }
    }

    private void attachPickListMetadata(MesTeamLeaderActiveOrderDetail detail, Long activeOrderId) {
        List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> materials = new ArrayList<>();
        materials.addAll(detail.getInputMaterialUsages());
        detail.getProcesses().forEach(process -> materials.addAll(process.getInputMaterials()));
        List<Long> pickListIds = materials.stream()
                .flatMap(material -> material.getSourcePickListIds().stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (pickListIds.isEmpty()) {
            return;
        }
        List<ErpKingdeeProductionPickListDO> headers = pickListMapper.selectBatchIds(pickListIds);
        Map<Long, ErpKingdeeProductionPickListDO> headersById = headers.stream()
                .collect(Collectors.toMap(ErpKingdeeProductionPickListDO::getId, Function.identity(),
                        (left, right) -> left));
        if (headersById.size() != pickListIds.size()) {
            throw invalidCompletedInput("领料单正式表头缺失：" + pickListIds);
        }
        List<ErpKingdeeProductionPickListItemDO> items = pickListItemMapper.selectListByPickListIds(pickListIds);
        Map<Long, List<String>> productionOrderNosByPickListId = items.stream()
                .filter(item -> item.getProductionPickListId() != null)
                .collect(Collectors.groupingBy(ErpKingdeeProductionPickListItemDO::getProductionPickListId,
                        LinkedHashMap::new,
                        Collectors.mapping(ErpKingdeeProductionPickListItemDO::getProductionOrderNo,
                                Collectors.filtering(Objects::nonNull, Collectors.toList()))));
        Map<Long, MesTeamLeaderActiveOrderDetail.SourcePickListDocument> documentsById = new LinkedHashMap<>();
        for (Long pickListId : pickListIds) {
            ErpKingdeeProductionPickListDO header = headersById.get(pickListId);
            documentsById.put(pickListId, new MesTeamLeaderActiveOrderDetail.SourcePickListDocument()
                    .setId(header.getId())
                    .setBillNo(header.getSourceBillNo())
                    .setDocumentStatus(header.getDocumentStatus())
                    .setBillDate(header.getBillDate())
                    .setProductionOrderNos(productionOrderNosByPickListId.getOrDefault(pickListId, List.of()).stream()
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList()));
        }
        for (MesTeamLeaderActiveOrderDetail.InputMaterialDetail material : materials) {
            material.setSourcePickListDocuments(material.getSourcePickListIds().stream()
                    .map(documentsById::get)
                    .filter(Objects::nonNull)
                    .toList());
        }
    }

    private FormalInputSourceSnapshot readCompletedSources(MesProcessPoolActiveOrderDO order,
                                                           MesProcessPoolActiveOrderCompletionBackfillDO backfill) {
        if (backfill == null) return null; // Completion has not materialized formal inputs yet.
        JSONObject payload = JSON.parseObject(backfill.getPayloadJson());
        if (!Objects.equals(order.getId(), backfill.getActiveOrderId())
                || !Objects.equals(order.getWorkOrderId(), backfill.getWorkOrderId())
                || !"SUCCESS".equals(backfill.getStatus()) || payload == null
                || !"SUCCESS".equals(payload.getString("status"))
                || !"BATCH_RECORD".equals(payload.getString("type"))
                || trimToNull(backfill.getSourceSnapshotHash()) == null
                || !Objects.equals(backfill.getSourceSnapshotHash(), payload.getString("sourceSnapshotHash"))) {
            throw invalidCompletedInput("完工领料回填身份或状态不一致");
        }
        JSONObject sources = JSON.parseObject(payload.getString("formalSourceSnapshot"));
        JSONObject orderBinding = sources == null ? null : sources.getJSONObject("activeOrderBinding");
        if (orderBinding == null || !Objects.equals(order.getId(), orderBinding.getLong("id"))
                || !Objects.equals(order.getWorkOrderId(), orderBinding.getLong("workOrderId"))
                || sources.getJSONArray("pickListBindings") == null
                || sources.getJSONObject("pickListBindingItems") == null) {
            throw invalidCompletedInput("完工领料回填缺少正式来源");
        }
        return new FormalInputSourceSnapshot(sources, backfill.getSourceSnapshotHash());
    }

    private MesTeamLeaderActiveOrderDetail.InputMaterialDetail toCompletedInputMaterialDetail(
            MesFrontlineProcessMaterial material, FormalInputSourceSnapshot inputSourceSnapshot) {
        var detail = toInputMaterialDetail(material);
        if (inputSourceSnapshot == null) return detail;
        JSONObject sources = inputSourceSnapshot.sources();
        Set<String> batches = new java.util.TreeSet<>();
        Set<Long> pickIds = new java.util.TreeSet<>();
        Set<String> pickNos = new java.util.TreeSet<>();
        Set<Long> itemIds = new java.util.TreeSet<>();
        BigDecimal requested = BigDecimal.ZERO, actual = BigDecimal.ZERO, baseActual = BigDecimal.ZERO;
        JSONObject rowsByBinding = sources.getJSONObject("pickListBindingItems");
        for (Object rawHeader : sources.getJSONArray("pickListBindings")) {
            JSONObject header = (JSONObject) rawHeader;
            var rows = rowsByBinding.getJSONArray(header.getString("id"));
            if (rows == null) throw invalidCompletedInput("完工领料回填缺少来源明细");
            for (Object rawItem : rows) {
                JSONObject item = (JSONObject) rawItem;
                if (!Objects.equals(material.materialCode(), trimToNull(item.getString("materialNumber")))) continue;
                String lot = trimToNull(item.getString("lotNumber"));
                String billNo = trimToNull(header.getString("sourceBillNo"));
                Long pickId = header.getLong("pickListId"), itemId = item.getLong("pickListItemId");
                BigDecimal itemActualQuantity = item.getBigDecimal("actualQuantity");
                if (lot == null || billNo == null || pickId == null || itemId == null
                        || itemActualQuantity == null || !itemIds.add(itemId)) {
                    throw invalidCompletedInput("完工输入物料批号或来源身份缺失、重复：" + material.materialCode());
                }
                batches.add(lot);
                pickIds.add(pickId);
                pickNos.add(billNo);
                if (item.getBigDecimal("requestedQuantity") != null) requested = requested.add(item.getBigDecimal("requestedQuantity"));
                actual = actual.add(itemActualQuantity);
                if (item.getBigDecimal("baseActualQuantity") != null) baseActual = baseActual.add(item.getBigDecimal("baseActualQuantity"));
            }
        }
        if (batches.isEmpty()) throw invalidCompletedInput("完工领料回填未匹配输入物料：" + material.materialCode());
        return detail.setBatchCodes(List.copyOf(batches)).setSourcePickListIds(List.copyOf(pickIds))
                .setSourcePickListNos(List.copyOf(pickNos)).setSourcePickListItemIds(List.copyOf(itemIds))
                .setRequestedQuantity(requested).setActualQuantity(actual).setBaseActualQuantity(baseActual)
                .setSourceSnapshotHash(inputSourceSnapshot.sourceSnapshotHash());
    }

    private List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> resolveInputMaterialUsages(
            FormalInputSourceSnapshot inputSourceSnapshot) {
        if (inputSourceSnapshot == null) {
            return List.of();
        }
        JSONObject sources = inputSourceSnapshot.sources();
        JSONArray headers = sources.getJSONArray("pickListBindings");
        JSONObject rowsByBinding = sources.getJSONObject("pickListBindingItems");
        if (headers == null || rowsByBinding == null) {
            throw invalidCompletedInput("订单级输入物料来源缺少正式领料明细");
        }
        Map<String, InputMaterialUsageAccumulator> usagesByCode = new LinkedHashMap<>();
        for (Object rawHeader : headers) {
            if (!(rawHeader instanceof JSONObject header)) {
                throw invalidCompletedInput("订单级输入物料来源表头结构无效");
            }
            JSONArray rows = rowsByBinding.getJSONArray(header.getString("id"));
            if (rows == null) {
                throw invalidCompletedInput("订单级输入物料来源缺少表体明细");
            }
            for (Object rawItem : rows) {
                if (!(rawItem instanceof JSONObject item)) {
                    throw invalidCompletedInput("订单级输入物料来源表体结构无效");
                }
                String materialCode = trimToNull(item.getString("materialNumber"));
                if (materialCode == null) {
                    throw invalidCompletedInput("订单级输入物料来源缺少物料编码");
                }
                usagesByCode.computeIfAbsent(materialCode, InputMaterialUsageAccumulator::new)
                        .add(header, item, inputSourceSnapshot.sourceSnapshotHash());
            }
        }
        return usagesByCode.values().stream()
                .map(InputMaterialUsageAccumulator::toDetail)
                .toList();
    }

    private static RuntimeException invalidCompletedInput(String reason) {
        return exception(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, reason);
    }

    private void attachSupplementMaterials(String workOrderCode, Long activeOrderId,
                                           Map<ProcessIdentity, ProcessAccumulator> accumulators) {
        List<ErpKingdeeProductionReplenishmentListItemDO> items = replenishmentListItemMapper
                .selectListByProductionOrderNo(workOrderCode);
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, ErpKingdeeProductionReplenishmentListDO> headersById = loadReplenishmentHeaders(activeOrderId, items);
        Map<String, MesMdItemDO> materialByCode = loadMaterialByCode(activeOrderId, items);
        Map<String, List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail>> supplementsByMaterialCode =
                groupSupplementMaterials(activeOrderId, items, headersById, materialByCode);
        for (ProcessAccumulator accumulator : accumulators.values()) {
            Set<String> inputMaterialCodes = accumulator.inputMaterials.stream()
                    .map(MesTeamLeaderActiveOrderDetail.InputMaterialDetail::getMaterialCode)
                    .map(MesTeamLeaderActiveOrderDetailServiceImpl::trimToNull)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (inputMaterialCodes.isEmpty()) {
                accumulator.setSupplementMaterials(List.of());
                continue;
            }
            List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail> matched = inputMaterialCodes.stream()
                    .flatMap(code -> supplementsByMaterialCode.getOrDefault(code, List.of()).stream())
                    .toList();
            accumulator.setSupplementMaterials(matched);
        }
    }

    private Map<Long, ErpKingdeeProductionReplenishmentListDO> loadReplenishmentHeaders(
            Long activeOrderId, List<ErpKingdeeProductionReplenishmentListItemDO> items) {
        List<Long> headerIds = items.stream()
                .map(ErpKingdeeProductionReplenishmentListItemDO::getProductionReplenishmentListId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (headerIds.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<ErpKingdeeProductionReplenishmentListDO> headers = replenishmentListMapper.selectBatchIds(headerIds);
        Map<Long, ErpKingdeeProductionReplenishmentListDO> headersById = mapById(headers,
                ErpKingdeeProductionReplenishmentListDO::getId, activeOrderId, "replenishmentList");
        if (!headersById.keySet().containsAll(headerIds)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        for (ErpKingdeeProductionReplenishmentListDO header : headersById.values()) {
            requireText(header.getSourceBillNo(), activeOrderId);
            if (!"C".equals(header.getDocumentStatus())) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
        }
        return headersById;
    }

    private Map<String, MesMdItemDO> loadMaterialByCode(Long activeOrderId,
                                                        List<ErpKingdeeProductionReplenishmentListItemDO> items) {
        Set<String> materialCodes = items.stream()
                .map(ErpKingdeeProductionReplenishmentListItemDO::getMaterialNumber)
                .map(MesTeamLeaderActiveOrderDetailServiceImpl::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (materialCodes.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        Map<String, MesMdItemDO> result = new LinkedHashMap<>();
        for (String materialCode : materialCodes) {
            MesMdItemDO item = itemMapper.selectByCode(materialCode);
            if (item == null || item.getId() == null || trimToNull(item.getCode()) == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            result.put(materialCode, item);
        }
        return result;
    }

    private Map<String, List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail>> groupSupplementMaterials(
            Long activeOrderId,
            List<ErpKingdeeProductionReplenishmentListItemDO> items,
            Map<Long, ErpKingdeeProductionReplenishmentListDO> headersById,
            Map<String, MesMdItemDO> materialByCode) {
        Map<String, SupplementAccumulator> byMaterialAndSource = new LinkedHashMap<>();
        for (ErpKingdeeProductionReplenishmentListItemDO item : items) {
            String materialCode = trimToNull(item.getMaterialNumber());
            String lotNumber = trimToNull(item.getLotNumber());
            if (item.getId() == null || item.getProductionReplenishmentListId() == null
                    || materialCode == null || trimToNull(item.getMaterialName()) == null || lotNumber == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            ErpKingdeeProductionReplenishmentListDO header = headersById.get(item.getProductionReplenishmentListId());
            MesMdItemDO material = materialByCode.get(materialCode);
            if (header == null || material == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            String key = item.getProductionReplenishmentListId() + "|" + materialCode;
            byMaterialAndSource.computeIfAbsent(key, ignored -> new SupplementAccumulator(material, item, header))
                    .add(item, header);
        }
        Map<String, List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail>> byMaterialCode =
                new LinkedHashMap<>();
        for (SupplementAccumulator accumulator : byMaterialAndSource.values()) {
            MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail detail = accumulator.toDetail();
            byMaterialCode.computeIfAbsent(detail.getMaterialCode(), ignored -> new ArrayList<>()).add(detail);
        }
        return byMaterialCode;
    }

    private void attachPqcSubmissions(MesProcessPoolActiveOrderDO activeOrder, Long activeOrderId,
                                      Map<ProcessIdentity, ProcessAccumulator> accumulators) {
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper.selectListByActiveOrderId(activeOrderId);
        if (tasks == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<MesPqcProcessInspectionAggregateDetailDO> details =
                pqcAggregateDetailMapper.selectListByActiveOrderId(activeOrderId);
        if (details == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        for (MesPqcProcessInspectionAggregateDetailDO detail : details) {
            if (detail == null || detail.getPqcTaskId() == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
        }
        Map<Long, List<MesPqcProcessInspectionAggregateDetailDO>> detailsByTask = details.stream()
                .collect(Collectors.groupingBy(MesPqcProcessInspectionAggregateDetailDO::getPqcTaskId,
                        LinkedHashMap::new, Collectors.toList()));
        Set<Long> taskIds = tasks.stream().map(MesPqcInspectionTaskDO::getId).collect(Collectors.toSet());
        for (Long pqcTaskId : detailsByTask.keySet()) {
            if (pqcTaskId == null || !taskIds.contains(pqcTaskId)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
        }
        Map<Long, MesQaInspectionRegulationProcessDO> qaProcessesById = mapById(
                qaProcessMapper.selectBatchIds(distinctIds(tasks, MesPqcInspectionTaskDO::getQaProcessId)),
                MesQaInspectionRegulationProcessDO::getId, activeOrderId, "qaProcess");
        Map<Long, MesTeamLeaderActiveOrderEventPartyReadDO> eventPartiesById =
                loadEventParties(activeOrderId, tasks);
        Map<Long, PqcSubmittedSnapshot> submittedItemsByEventId =
                loadPqcSubmittedItemsByEventId(activeOrderId, tasks);
        Map<PqcSubmissionIdentity, PqcSubmissionAccumulator> pqcSubmissionAccumulators = new LinkedHashMap<>();
        boolean collapseStage1Copies = isStage1Simulation(activeOrder);
        Set<PqcStage1SubmissionDisplayIdentity> displayedStage1Identities = new LinkedHashSet<>();
        for (MesPqcInspectionTaskDO task : tasks) {
            if (task == null || task.getId() == null || task.getRouteProcessId() == null
                    || task.getProcessId() == null || task.getQaProcessId() == null
                    || trimToNull(task.getQaItemCode()) == null
                    || trimToNull(task.getInspectionRuleKey()) == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            MesQaInspectionRegulationProcessDO qaProcess = qaProcessesById.get(task.getQaProcessId());
            if (qaProcess == null || !Objects.equals(qaProcess.getRegulationVersionId(), task.getRegulationVersionId())) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            requireText(qaProcess.getProcessName(), activeOrderId);
            List<MesPqcProcessInspectionAggregateDetailDO> taskDetails =
                    detailsByTask.getOrDefault(task.getId(), List.of());
            if (task.getSubmittedEventId() == null) {
                if (taskDetails.isEmpty()) {
                    continue;
                }
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            if (collapseStage1Copies
                    && !displayedStage1Identities.add(PqcStage1SubmissionDisplayIdentity.of(task))) {
                continue;
            }
            ProcessAccumulator accumulator = accumulators.get(
                    new ProcessIdentity(task.getRouteProcessId(), task.getProcessId()));
            if (accumulator == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            MesTeamLeaderActiveOrderEventPartyReadDO eventParty = eventPartiesById.get(task.getSubmittedEventId());
            PqcSubmissionIdentity submissionIdentity = new PqcSubmissionIdentity(
                    new ProcessIdentity(task.getRouteProcessId(), task.getProcessId()),
                    task.getQaProcessId(), trimToNull(task.getQaItemCode()), task.getInspectionRuleKey(),
                    task.getBusinessDate(), trimToNull(task.getShiftCode()), task.getRoundNo(),
                    task.getSubmittedEventId(), eventParty == null ? null : eventParty.getProductionEventId());
            pqcSubmissionAccumulators.computeIfAbsent(submissionIdentity,
                            ignored -> new PqcSubmissionAccumulator(task, qaProcess,
                                    isProductQaPqcSubmission(activeOrder, task, activeOrderId)))
                    .add(task, taskDetails, submittedItemsByEventId.get(task.getSubmittedEventId()), eventParty,
                            activeOrderId);
        }
        List<PqcSubmissionAccumulator> orderedPqcSubmissions = arrangePqcSubmissionDisplayOrder(
                activeOrder, activeOrderId, pqcSubmissionAccumulators.values());
        for (PqcSubmissionAccumulator pqcSubmission : orderedPqcSubmissions) {
            ProcessAccumulator accumulator = accumulators.get(pqcSubmission.processIdentity());
            if (accumulator == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            accumulator.addPqcSubmission(pqcSubmission.toDetail(
                    accumulator.productionSubmitterSignaturesByEventId()));
        }
    }

    private static List<PqcSubmissionAccumulator> arrangePqcSubmissionDisplayOrder(
            MesProcessPoolActiveOrderDO activeOrder, Long activeOrderId,
            Collection<PqcSubmissionAccumulator> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return List.of();
        }
        List<PqcSubmissionAccumulator> productProcesses = new ArrayList<>();
        List<PqcSubmissionAccumulator> commonPackagingProcesses = new ArrayList<>();
        for (PqcSubmissionAccumulator submission : submissions) {
            if (submission.productQaSource) {
                productProcesses.add(submission);
            } else {
                commonPackagingProcesses.add(submission);
            }
        }
        productProcesses.sort(Comparator
                .comparingInt((PqcSubmissionAccumulator submission) ->
                        requireQaProcessSort(submission, activeOrderId))
                .thenComparing(PqcSubmissionAccumulator::qaProcessId,
                        Comparator.nullsLast(Long::compareTo))
                .thenComparing(PqcSubmissionAccumulator::firstTaskId,
                        Comparator.nullsLast(Long::compareTo)));
        commonPackagingProcesses.sort(Comparator
                .comparing(PqcSubmissionAccumulator::firstTaskId,
                        Comparator.nullsLast(Long::compareTo))
                .thenComparingInt(submission -> requireQaProcessSort(submission, activeOrderId))
                .thenComparing(PqcSubmissionAccumulator::qaProcessId,
                        Comparator.nullsLast(Long::compareTo)));
        int productMaxSort = 0;
        Map<Long, Integer> displaySortByQaProcessId = new LinkedHashMap<>();
        for (PqcSubmissionAccumulator submission : productProcesses) {
            int qaProcessSort = requireQaProcessSort(submission, activeOrderId);
            displaySortByQaProcessId.putIfAbsent(submission.qaProcessId(), qaProcessSort);
            productMaxSort = Math.max(productMaxSort, qaProcessSort);
        }
        int nextSort = productMaxSort;
        for (PqcSubmissionAccumulator submission : commonPackagingProcesses) {
            Integer displaySort = displaySortByQaProcessId.get(submission.qaProcessId());
            if (displaySort == null) {
                submission.setQaProcessDisplaySort(++nextSort);
                displaySortByQaProcessId.put(submission.qaProcessId(), submission.qaProcessDisplaySort);
            } else {
                submission.setQaProcessDisplaySort(displaySort);
            }
        }
        for (PqcSubmissionAccumulator submission : productProcesses) {
            Integer displaySort = displaySortByQaProcessId.get(submission.qaProcessId());
            if (displaySort == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            submission.setQaProcessDisplaySort(displaySort);
        }
        List<PqcSubmissionAccumulator> orderedProcesses = new ArrayList<>(
                productProcesses.size() + commonPackagingProcesses.size());
        orderedProcesses.addAll(productProcesses);
        orderedProcesses.addAll(commonPackagingProcesses);
        return orderedProcesses;
    }

    private static boolean isProductQaPqcSubmission(MesProcessPoolActiveOrderDO activeOrder,
                                                    MesPqcInspectionTaskDO task,
                                                    Long activeOrderId) {
        if (activeOrder == null || activeOrder.getQaRegulationVersionId() == null
                || task == null || task.getRegulationVersionId() == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return Objects.equals(activeOrder.getQaRegulationVersionId(), task.getRegulationVersionId());
    }

    private static int requireQaProcessSort(PqcSubmissionAccumulator submission, Long activeOrderId) {
        if (submission == null || submission.qaProcess == null || submission.qaProcess.getSort() == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return submission.qaProcess.getSort();
    }

    private static boolean isStage1Simulation(MesProcessPoolActiveOrderDO activeOrder) {
        return activeOrder != null
                && Boolean.TRUE.equals(activeOrder.getSimulated())
                && "STAGE1".equals(activeOrder.getSimulationStage());
    }

    private Map<Long, MesTeamLeaderActiveOrderEventPartyReadDO> loadEventParties(
            Long activeOrderId, List<MesPqcInspectionTaskDO> tasks) {
        List<Long> eventIds = tasks.stream()
                .map(MesPqcInspectionTaskDO::getSubmittedEventId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        List<MesTeamLeaderActiveOrderEventPartyReadDO> parties =
                detailReadMapper.selectEventPartiesByEventIds(eventIds);
        Map<Long, MesTeamLeaderActiveOrderEventPartyReadDO> partiesById = mapById(parties,
                MesTeamLeaderActiveOrderEventPartyReadDO::getEventId, activeOrderId, "pqcEventParty");
        if (!partiesById.keySet().containsAll(eventIds)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        for (MesTeamLeaderActiveOrderEventPartyReadDO party : partiesById.values()) {
            requireText(party.getSubmitterName(), activeOrderId);
        }
        return partiesById;
    }

    private Map<Long, PqcSubmittedSnapshot> loadPqcSubmittedItemsByEventId(
            Long activeOrderId, List<MesPqcInspectionTaskDO> tasks) {
        List<Long> eventIds = tasks.stream()
                .map(MesPqcInspectionTaskDO::getSubmittedEventId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        List<MesProProcessPoolEventDO> events = eventMapper.selectBatchIds(eventIds);
        Map<Long, MesProProcessPoolEventDO> eventsById = mapById(events,
                MesProProcessPoolEventDO::getId, activeOrderId, "pqcEvent");
        if (!eventsById.keySet().containsAll(eventIds)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        Map<Long, PqcSubmittedSnapshot> itemsByEventId =
                new LinkedHashMap<>();
        for (Long eventId : eventIds) {
            MesProProcessPoolEventDO event = eventsById.get(eventId);
            if (event == null
                    || !MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            itemsByEventId.put(eventId, resolvePqcOriginalSubmittedItems(event, activeOrderId));
        }
        return itemsByEventId;
    }

    private record PqcSubmittedSnapshot(int inspectionQuantity, int scrapQuantity,
                                        List<MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail> items) {
    }

    private PqcSubmittedSnapshot resolvePqcOriginalSubmittedItems(
            MesProProcessPoolEventDO event, Long activeOrderId) {
        String payloadJson = resolvePqcOriginalSubmittedPayloadJson(event, activeOrderId);
        Map<?, ?> payload = parseOriginalPayload(payloadJson, activeOrderId);
        if (payload == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        Object rootPayload = payload.get("rawPayload");
        if (rootPayload instanceof Map<?, ?> rawPayload) {
            payload = rawPayload;
        }
        int inspectionQuantity = requirePqcSnapshotQuantity(payload, "actualInspectionQuantity", 1);
        int scrapQuantity = requirePqcSnapshotQuantity(payload, "scrapQuantity", 0);
        if (scrapQuantity > inspectionQuantity) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "originalPqcPayload.scrapQuantity");
        }
        Object pqcItemDetails = payload.get("pqcItemDetails");
        if (pqcItemDetails != null) {
            return new PqcSubmittedSnapshot(inspectionQuantity, scrapQuantity,
                    resolvePqcSubmittedItemDetails(pqcItemDetails, activeOrderId));
        }
        Object itemResults = payload.get("itemResults");
        if (itemResults != null) {
            return new PqcSubmittedSnapshot(inspectionQuantity, scrapQuantity,
                    resolvePqcSubmittedItemDetails(itemResults, activeOrderId));
        }
        throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
    }

    private static int requirePqcSnapshotQuantity(Map<?, ?> payload, String field, int minimum) {
        try {
            String value = trimToNull(payload.get(field));
            if (value == null) {
                throw new IllegalArgumentException("missing quantity");
            }
            int quantity = new BigDecimal(value).intValueExact();
            if (quantity < minimum) {
                throw new IllegalArgumentException("invalid quantity");
            }
            return quantity;
        } catch (IllegalArgumentException | ArithmeticException ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "originalPqcPayload." + field);
        }
    }

    private String resolvePqcOriginalSubmittedPayloadJson(MesProProcessPoolEventDO event, Long activeOrderId) {
        List<MesProProcessPoolEventRevisionDO> revisions = eventRevisionMapper.selectListByEventId(event.getId());
        MesProProcessPoolEventRevisionDO firstRevision = revisions == null ? null : revisions.stream()
                .filter(Objects::nonNull)
                .filter(revision -> MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE.equals(
                        revision.getRevisionStatus()))
                .min(Comparator
                        .comparing(MesProProcessPoolEventRevisionDO::getServerRevisionTime,
                                Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(MesProProcessPoolEventRevisionDO::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
        String payloadJson = firstRevision == null ? event.getRawPayload() : firstRevision.getBeforePayload();
        if (trimToNull(payloadJson) == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return payloadJson;
    }

    private static List<MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail> resolvePqcSubmittedItemDetails(
            Object value, Long activeOrderId) {
        if (!(value instanceof List<?> sourceItems) || sourceItems.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail> rows = new ArrayList<>();
        for (Object sourceItem : sourceItems) {
            if (!(sourceItem instanceof Map<?, ?> item)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            String itemCode = requireStringValue(item.get("itemCode"), activeOrderId);
            String itemName = stringValue(item.get("itemName"));
            String inspectionMethod = stringValue(item.get("inspectionMethod"));
            String standardText = stringValue(item.get("standardText"));
            String judgement = requireStringValue(item.get("judgement"), activeOrderId);
            String itemResult = stringValue(item.get("itemResult"));
            String selectedEquipmentName = stringValue(item.get("selectedEquipmentName"));
            String selectedEquipmentNumber = stringValue(item.get("selectedEquipmentNumber"));
            List<String> sampleValues = resolvePqcSubmittedSampleValues(item, activeOrderId);
            for (int index = 0; index < sampleValues.size(); index++) {
                String measuredValue = sampleValues.get(index);
                rows.add(new MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail()
                        .setSampleNo(index + 1)
                        .setItemCode(itemCode)
                        .setItemName(itemName)
                        .setInspectionMethod(inspectionMethod)
                        .setStandardText(standardText)
                        .setMeasuredValue(measuredValue)
                        .setItemResult(itemResult)
                        .setJudgement(resolvePqcSubmittedSampleJudgement(item, sampleValues.get(0),
                                measuredValue, judgement))
                        .setSelectedEquipmentName(selectedEquipmentName)
                        .setSelectedEquipmentNumber(selectedEquipmentNumber));
            }
        }
        if (rows.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return List.copyOf(rows);
    }

    private static String resolvePqcSubmittedSampleJudgement(Map<?, ?> item, String firstValue,
                                                            String measuredValue, String firstJudgement) {
        // The snapshot judgement belongs to the first value, not to every value in the item.
        if (Objects.equals(firstValue, measuredValue)) {
            return firstJudgement;
        }
        try {
            String resultType = trimToNull(item.get("resultType"));
            if (resultType == null) {
                throw new IllegalArgumentException("resultType is required");
            }
            BigDecimal lower = item.get("standardLowerLimit") == null ? null
                    : new BigDecimal(item.get("standardLowerLimit").toString());
            BigDecimal upper = item.get("standardUpperLimit") == null ? null
                    : new BigDecimal(item.get("standardUpperLimit").toString());
            Integer precision = item.get("standardPrecision") == null ? null
                    : new BigDecimal(item.get("standardPrecision").toString()).intValueExact();
            return PqcResultValueValidator.validate(resultType, measuredValue, lower, upper, precision).judgement();
        } catch (IllegalArgumentException | ArithmeticException ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                    "originalPqcPayload.pqcItemDetails." + item.get("itemCode") + ": " + ex.getMessage());
        }
    }

    private static List<String> resolvePqcSubmittedSampleValues(Map<?, ?> item, Long activeOrderId) {
        Object sampleValues = item.get("sampleValues");
        if (sampleValues instanceof List<?> values) {
            List<String> normalizedValues = values.stream()
                    .map(MesTeamLeaderActiveOrderDetailServiceImpl::trimToNull)
                    .filter(Objects::nonNull)
                    .toList();
            if (normalizedValues.isEmpty()) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            return normalizedValues;
        }
        String measuredValue = firstNonBlankString(item.get("measuredValue"), item.get("itemResult"));
        if (measuredValue == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return List.of(measuredValue);
    }

    private static MesTeamLeaderActiveOrderDetail.InputMaterialDetail toInputMaterialDetail(
            MesFrontlineProcessMaterial material) {
        return new MesTeamLeaderActiveOrderDetail.InputMaterialDetail()
                .setMaterialId(material.materialId())
                .setMaterialCode(material.materialCode())
                .setMaterialName(material.materialName())
                .setMaterialSpecification(material.materialSpecification())
                .setBatchCodes(List.copyOf(material.batchCodes()))
                .setRequestedQuantity(material.requestedQuantity())
                .setActualQuantity(material.actualQuantity())
                .setBaseActualQuantity(material.baseActualQuantity())
                .setSourcePickListIds(List.copyOf(material.sourcePickListIds()))
                .setSourcePickListNos(List.copyOf(material.sourcePickListNos()))
                .setSourcePickListItemIds(List.copyOf(material.sourcePickListItemIds()))
                .setSourceSnapshotHash(material.sourceSnapshotHash());
    }

    private static MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail toPqcSubmissionItemDetail(
            MesPqcProcessInspectionAggregateDetailDO detail) {
        return new MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail()
                .setAggregateDetailId(detail.getId())
                .setSampleNo(detail.getSampleNo())
                .setItemCode(detail.getItemCode())
                .setItemName(detail.getItemName())
                .setInspectionMethod(detail.getInspectionMethod())
                .setStandardText(detail.getStandardText())
                .setMeasuredValue(detail.getMeasuredValue())
                .setItemResult(detail.getItemResult())
                .setJudgement(detail.getJudgement())
                .setSelectedEquipmentName(detail.getSelectedEquipmentName())
                .setSelectedEquipmentNumber(detail.getSelectedEquipmentNumber());
    }

    private static void validateProcessRow(MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        if (row == null || row.getSnapshotId() == null || row.getRouteProcessId() == null || row.getProcessId() == null
                || requiredQuantityOf(row) == null || requiredQuantityOf(row).compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        requireText(row.getProcessName(), activeOrderId);
    }

    private static BigDecimal requiredQuantityOf(MesTeamLeaderActiveOrderDetailReadDO row) {
        return READ_ROW_REQUIRED_QUANTITY.apply(row);
    }

    private static BigDecimal requiredQuantityOf(MesTeamLeaderActiveOrderDetail.ProcessDetail process) {
        return READ_DETAIL_REQUIRED_QUANTITY.apply(process);
    }

    private static void requireText(String value, Long activeOrderId) {
        if (value == null || value.isBlank()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private static String requireTextValue(String value, Long activeOrderId) {
        requireText(value, activeOrderId);
        return value;
    }

    private static <T> List<Long> distinctIds(List<T> rows, Function<T, Long> idGetter) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .map(idGetter)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static <T> Map<Long, T> mapById(List<T> rows, Function<T, Long> idGetter,
                                             Long activeOrderId, String sourceName) {
        if (rows == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        try {
            return rows.stream().collect(Collectors.toMap(idGetter, Function.identity(),
                    (left, right) -> {
                        throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
                    }, LinkedHashMap::new));
        } catch (NullPointerException exception) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record FormalInputSourceSnapshot(JSONObject sources, String sourceSnapshotHash) {
    }

    private record PqcSubmissionIdentity(ProcessIdentity processIdentity, Long qaProcessId, String qaItemCode,
                                         String inspectionRuleKey, LocalDate businessDate, String shiftCode,
                                         Integer roundNo, Long submittedEventId, Long productionEventId) {
    }

    private record PqcStage1SubmissionDisplayIdentity(Long regulationVersionId, Long qaProcessId, String qaItemCode,
                                                      String inspectionRuleKey, String inspectionType,
                                                      LocalDate businessDate, String shiftCode, Integer roundNo) {

        private static PqcStage1SubmissionDisplayIdentity of(MesPqcInspectionTaskDO task) {
            return new PqcStage1SubmissionDisplayIdentity(task.getRegulationVersionId(), task.getQaProcessId(),
                    trimToNull(task.getQaItemCode()), trimToNull(task.getInspectionRuleKey()),
                    normalizePqcInspectionType(task.getInspectionType()), task.getBusinessDate(),
                    trimToNull(task.getShiftCode()), task.getRoundNo());
        }
    }

    private static final class SupplementAccumulator {
        private final MesMdItemDO material;
        private final String materialName;
        private final String materialSpecification;
        private final LinkedHashSet<String> batchCodes = new LinkedHashSet<>();
        private final LinkedHashSet<Long> replenishmentListIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> replenishmentListNos = new LinkedHashSet<>();
        private final LinkedHashSet<Long> replenishmentListItemIds = new LinkedHashSet<>();
        private BigDecimal requestedQuantity = BigDecimal.ZERO;
        private BigDecimal actualQuantity = BigDecimal.ZERO;
        private BigDecimal baseActualQuantity = BigDecimal.ZERO;

        private SupplementAccumulator(MesMdItemDO material, ErpKingdeeProductionReplenishmentListItemDO firstItem,
                                      ErpKingdeeProductionReplenishmentListDO firstHeader) {
            this.material = material;
            this.materialName = trimToNull(firstItem.getMaterialName());
            this.materialSpecification = trimToNull(firstItem.getMaterialSpecification());
            add(firstItem, firstHeader);
        }

        private void add(ErpKingdeeProductionReplenishmentListItemDO item,
                         ErpKingdeeProductionReplenishmentListDO header) {
            batchCodes.add(trimToNull(item.getLotNumber()));
            replenishmentListIds.add(item.getProductionReplenishmentListId());
            replenishmentListNos.add(trimToNull(header.getSourceBillNo()));
            replenishmentListItemIds.add(item.getId());
            requestedQuantity = requestedQuantity.add(zeroIfNull(item.getRequestedQuantity()));
            actualQuantity = actualQuantity.add(zeroIfNull(item.getActualQuantity()));
            baseActualQuantity = baseActualQuantity.add(zeroIfNull(item.getBaseActualQuantity()));
        }

        private MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail toDetail() {
            return new MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail()
                    .setMaterialId(material.getId())
                    .setMaterialCode(trimToNull(material.getCode()))
                    .setMaterialName(materialName)
                    .setMaterialSpecification(materialSpecification)
                    .setBatchCodes(batchCodes.stream().filter(Objects::nonNull).sorted().toList())
                    .setRequestedQuantity(requestedQuantity)
                    .setActualQuantity(actualQuantity)
                    .setBaseActualQuantity(baseActualQuantity)
                    .setSourceReplenishmentListIds(replenishmentListIds.stream().sorted().toList())
                    .setSourceReplenishmentListNos(replenishmentListNos.stream().filter(Objects::nonNull).sorted().toList())
                    .setSourceReplenishmentListItemIds(replenishmentListItemIds.stream().sorted().toList());
        }

        private static BigDecimal zeroIfNull(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    private static final class InputMaterialUsageAccumulator {
        private final String materialCode;
        private String materialName;
        private String materialSpecification;
        private String sourceSnapshotHash;
        private final LinkedHashSet<String> batchCodes = new LinkedHashSet<>();
        private final LinkedHashSet<Long> pickListIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> pickListNos = new LinkedHashSet<>();
        private final LinkedHashSet<Long> pickListItemIds = new LinkedHashSet<>();
        private BigDecimal requestedQuantity = BigDecimal.ZERO;
        private BigDecimal actualQuantity = BigDecimal.ZERO;
        private BigDecimal baseActualQuantity = BigDecimal.ZERO;

        private InputMaterialUsageAccumulator(String materialCode) {
            this.materialCode = materialCode;
        }

        private void add(JSONObject header, JSONObject item, String sourceSnapshotHash) {
            String lotNumber = trimToNull(item.getString("lotNumber"));
            String sourceBillNo = trimToNull(header.getString("sourceBillNo"));
            Long pickListId = header.getLong("pickListId");
            Long pickListItemId = item.getLong("pickListItemId");
            BigDecimal actualQuantity = item.getBigDecimal("actualQuantity");
            if (lotNumber == null || sourceBillNo == null || pickListId == null
                    || pickListItemId == null || actualQuantity == null) {
                throw invalidCompletedInput("订单级输入物料来源缺少批号、来源身份或实发数量：" + materialCode);
            }
            if (!pickListItemIds.add(pickListItemId)) {
                throw invalidCompletedInput("订单级输入物料来源重复：" + materialCode);
            }
            batchCodes.add(lotNumber);
            pickListIds.add(pickListId);
            pickListNos.add(sourceBillNo);
            if (materialName == null) {
                materialName = trimToNull(item.getString("materialName"));
            }
            if (materialSpecification == null) {
                materialSpecification = trimToNull(item.getString("materialSpecification"));
            }
            if (this.sourceSnapshotHash == null) {
                this.sourceSnapshotHash = sourceSnapshotHash;
            } else if (!Objects.equals(this.sourceSnapshotHash, sourceSnapshotHash)) {
                throw invalidCompletedInput("订单级输入物料来源快照不一致：" + materialCode);
            }
            BigDecimal itemRequestedQuantity = zeroIfNull(item.getBigDecimal("requestedQuantity"));
            BigDecimal itemBaseActualQuantity = zeroIfNull(item.getBigDecimal("baseActualQuantity"));
            if (actualQuantity.compareTo(this.actualQuantity) > 0) {
                requestedQuantity = itemRequestedQuantity;
                this.actualQuantity = actualQuantity;
                baseActualQuantity = itemBaseActualQuantity;
            }
        }

        private MesTeamLeaderActiveOrderDetail.InputMaterialDetail toDetail() {
            return new MesTeamLeaderActiveOrderDetail.InputMaterialDetail()
                    .setMaterialCode(materialCode)
                    .setMaterialName(materialName)
                    .setMaterialSpecification(materialSpecification)
                    .setBatchCodes(batchCodes.stream().filter(Objects::nonNull).sorted().toList())
                    .setRequestedQuantity(requestedQuantity)
                    .setActualQuantity(actualQuantity)
                    .setBaseActualQuantity(baseActualQuantity)
                    .setSourcePickListIds(pickListIds.stream().sorted().toList())
                    .setSourcePickListNos(pickListNos.stream().filter(Objects::nonNull).sorted().toList())
                    .setSourcePickListItemIds(pickListItemIds.stream().sorted().toList())
                    .setSourceSnapshotHash(sourceSnapshotHash);
        }

        private static BigDecimal zeroIfNull(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    private static final class PqcSubmissionAccumulator {
        private final MesPqcInspectionTaskDO firstTask;
        private final MesQaInspectionRegulationProcessDO qaProcess;
        private final boolean productQaSource;
        private final String qaProcessDisplayName;
        private final LinkedHashSet<Long> pqcTaskIds = new LinkedHashSet<>();
        private final LinkedHashSet<Long> submittedEventIds = new LinkedHashSet<>();
        private final LinkedHashSet<Long> productionEventIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> submitterNames = new LinkedHashSet<>();
        private final LinkedHashSet<String> reviewerNames = new LinkedHashSet<>();
        private final List<MesTeamLeaderActiveOrderDetail.SignatureDetail> submitterSignatures = new ArrayList<>();
        private final List<MesTeamLeaderActiveOrderDetail.SignatureDetail> reviewerSignatures = new ArrayList<>();
        private final List<MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail> submittedItems = new ArrayList<>();
        private final List<MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail> processInspectionItems =
                new ArrayList<>();
        private Integer actualInspectionQuantity;
        private Integer scrapQuantity;
        private Integer submittedInspectionQuantity;
        private Integer submittedScrapQuantity;
        private Integer qaProcessDisplaySort;

        private PqcSubmissionAccumulator(MesPqcInspectionTaskDO firstTask,
                                         MesQaInspectionRegulationProcessDO qaProcess,
                                         boolean productQaSource) {
            this.firstTask = firstTask;
            this.qaProcess = qaProcess;
            this.productQaSource = productQaSource;
            this.qaProcessDisplayName = resolvePqcSubmissionProcessDisplayName(productQaSource,
                    qaProcess.getProcessName());
        }

        private void add(MesPqcInspectionTaskDO task, List<MesPqcProcessInspectionAggregateDetailDO> details,
                         PqcSubmittedSnapshot submittedSnapshot,
                         MesTeamLeaderActiveOrderEventPartyReadDO eventParty, Long activeOrderId) {
            pqcTaskIds.add(task.getId());
            if (task.getSubmittedEventId() != null) {
                if (submittedSnapshot == null || submittedSnapshot.items().isEmpty()) {
                    throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
                }
                this.submittedItems.addAll(submittedSnapshot.items());
                submittedInspectionQuantity = submittedSnapshot.inspectionQuantity();
                submittedScrapQuantity = submittedSnapshot.scrapQuantity();
                submittedEventIds.add(task.getSubmittedEventId());
                if (eventParty == null) {
                    throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
                }
                submitterNames.add(trimToNull(eventParty.getSubmitterName()));
                reviewerNames.add(trimToNull(eventParty.getReviewerName()));
                addSignature(submitterSignatures, eventParty.getSubmitterSignatureId(),
                        eventParty.getSubmitterName(), eventParty.getSubmitterSignedAt(), "PQC_SUBMIT");
                addSignature(reviewerSignatures, eventParty.getReviewerSignatureId(),
                        eventParty.getReviewerName(), eventParty.getReviewerSignedAt(), "FORM_REVIEW");
                scrapQuantity = addScrapQuantity(scrapQuantity, eventParty.getScrapQuantity());
                if (eventParty.getProductionEventId() != null) {
                    productionEventIds.add(eventParty.getProductionEventId());
                }
            }
            if (actualInspectionQuantity == null
                    || (task.getActualInspectionQuantity() != null
                    && task.getActualInspectionQuantity() > actualInspectionQuantity)) {
                actualInspectionQuantity = task.getActualInspectionQuantity();
            }
            details.stream()
                    .sorted(Comparator
                            .comparing(MesPqcProcessInspectionAggregateDetailDO::getSampleNo,
                                    Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getItemCode,
                                    Comparator.nullsLast(String::compareTo))
                            .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getId,
                                    Comparator.nullsLast(Long::compareTo)))
                    .map(MesTeamLeaderActiveOrderDetailServiceImpl::toPqcSubmissionItemDetail)
                    .forEach(processInspectionItems::add);
        }

        private MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail toDetail(
                Map<Long, MesTeamLeaderActiveOrderDetail.SignatureDetail> productionSignaturesByEventId) {
            List<Long> taskIds = List.copyOf(pqcTaskIds);
            List<Long> eventIds = List.copyOf(submittedEventIds);
            List<Long> productionIds = List.copyOf(productionEventIds);
            List<MesTeamLeaderActiveOrderDetail.SignatureDetail> productionSignatures = productionEventIds.stream()
                    .map(productionSignaturesByEventId::get)
                    .filter(Objects::nonNull)
                    .filter(signature -> signature.getSignatureId() != null)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(MesTeamLeaderActiveOrderDetail.SignatureDetail::getSignatureId,
                                    Function.identity(), (left, right) -> left, LinkedHashMap::new),
                            signaturesById -> List.copyOf(signaturesById.values())));
            return new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail()
                    .setPqcTaskId(taskIds.isEmpty() ? null : taskIds.get(0))
                    .setPqcTaskIds(taskIds)
                    .setSubmittedEventId(eventIds.isEmpty() ? null : eventIds.get(0))
                    .setSubmittedEventIds(eventIds)
                    .setProductionEventId(productionIds.isEmpty() ? null : productionIds.get(0))
                    .setProductionEventIds(productionIds)
                    .setProductionSubmitterSignatures(productionSignatures)
                    .setQaProcessId(qaProcess.getId())
                    .setQaProcessCode(qaProcess.getProcessCode())
                    .setQaProcessName(qaProcessDisplayName)
                    .setQaProcessSort(qaProcessDisplaySort)
                    .setQaItemCode(firstTask.getQaItemCode())
                    .setInspectionRuleKey(firstTask.getInspectionRuleKey())
                    .setInspectionType(firstTask.getInspectionType())
                    .setBusinessDate(firstTask.getBusinessDate())
                    .setShiftCode(firstTask.getShiftCode())
                    .setRoundNo(firstTask.getRoundNo())
                    .setActualInspectionQuantity(actualInspectionQuantity)
                    .setScrapQuantity(scrapQuantity)
                    .setSubmittedInspectionQuantity(submittedInspectionQuantity)
                    .setSubmittedScrapQuantity(submittedScrapQuantity)
                    .setTaskStatus(firstTask.getTaskStatus())
                    .setSubmitterName(joinDistinctTexts(submitterNames))
                    .setReviewerName(joinDistinctTexts(reviewerNames))
                    .setSubmitterSignatures(List.copyOf(submitterSignatures))
                    .setReviewerSignatures(List.copyOf(reviewerSignatures))
                    .setSubmittedItems(List.copyOf(submittedItems))
                    .setProcessInspectionItems(List.copyOf(processInspectionItems));
        }

        private Long firstTaskId() {
            return firstTask.getId();
        }

        private Long qaProcessId() {
            return qaProcess.getId();
        }

        private ProcessIdentity processIdentity() {
            return new ProcessIdentity(firstTask.getRouteProcessId(), firstTask.getProcessId());
        }

        private void setQaProcessDisplaySort(Integer qaProcessDisplaySort) {
            this.qaProcessDisplaySort = qaProcessDisplaySort;
        }
    }

    private static String resolvePqcSubmissionProcessDisplayName(boolean productQaSource, String processName) {
        if (productQaSource) {
            return processName;
        }
        String normalized = trimToNull(processName);
        if (Objects.equals("初包装", normalized) || Objects.equals("初包装过程检验规程", normalized)) {
            return "小包装";
        }
        if (Objects.equals("大中包装", normalized) || Objects.equals("大中包装过程检验规程", normalized)) {
            return "中大包装";
        }
        return processName;
    }

    private static Integer addScrapQuantity(Integer current, Integer next) {
        if (next == null) {
            return current;
        }
        return (current == null ? 0 : current) + next;
    }

    private static void addSignature(List<MesTeamLeaderActiveOrderDetail.SignatureDetail> signatures,
                                     Long signatureId, String signerName, LocalDateTime signedAt, String role) {
        if (signatureId == null) {
            return;
        }
        boolean existed = signatures.stream()
                .anyMatch(signature -> Objects.equals(signature.getSignatureId(), signatureId));
        if (existed) {
            return;
        }
        signatures.add(toSignatureDetail(signatureId, signerName, signedAt, role));
    }

    private static MesTeamLeaderActiveOrderDetail.SignatureDetail toSignatureDetail(
            Long signatureId, String signerName, LocalDateTime signedAt, String role) {
        if (signatureId == null) {
            return null;
        }
        return new MesTeamLeaderActiveOrderDetail.SignatureDetail()
                .setSignatureId(signatureId)
                .setSignerName(trimToNull(signerName))
                .setSignedAt(signedAt)
                .setRole(role);
    }

    private static String joinDistinctTexts(Set<String> values) {
        String joined = values.stream()
                .map(MesTeamLeaderActiveOrderDetailServiceImpl::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("、"));
        return joined.isBlank() ? null : joined;
    }

    private static final class ProcessAccumulator {
        private final MesTeamLeaderActiveOrderDetail.ProcessDetail process;
        private final List<MesTeamLeaderActiveOrderDetail.SubmissionDetail> submissions = new ArrayList<>();
        private final List<MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail> pqcSubmissions = new ArrayList<>();
        private List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> inputMaterials = List.of();
        private List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail> supplementMaterials = List.of();
        private BigDecimal submittedQuantity = BigDecimal.ZERO;

        private ProcessAccumulator(MesTeamLeaderActiveOrderDetailReadDO row) {
            this.process = new MesTeamLeaderActiveOrderDetail.ProcessDetail()
                    .setRouteProcessId(row.getRouteProcessId())
                    .setProcessId(row.getProcessId())
                    .setProcessCode(row.getProcessCode())
                    .setProcessName(row.getProcessName())
                    .setKeyFlag(Boolean.TRUE.equals(row.getKeyFlag()))
                    .setRequiredQuantity(requiredQuantityOf(row));
        }

        private void addSubmission(MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
            if (row.getEventId() == null) {
                return;
            }
            if (row.getSubmittedQuantity() == null || row.getSubmittedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            requireText(row.getSubmitterName(), activeOrderId);
            if (row.getSubmittedAt() == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            submissions.add(new MesTeamLeaderActiveOrderDetail.SubmissionDetail()
                    .setEventId(row.getEventId())
                    .setSubmittedQuantity(row.getSubmittedQuantity())
                    .setSubmitterName(row.getSubmitterName())
                    .setReviewerName(row.getReviewerName())
                    .setSubmittedAt(row.getSubmittedAt())
                    .setSubmitterSignature(toSignatureDetail(row.getSubmitterSignatureId(),
                            row.getSubmitterName(), row.getSubmitterSignedAt(), "PRODUCTION_SUBMIT"))
                    .setReviewerSignature(toSignatureDetail(row.getReviewerSignatureId(),
                            row.getReviewerName(), row.getReviewerSignedAt(), "FORM_REVIEW"))
                    .setDevices(resolveSubmissionDevices(row, activeOrderId))
                    .setDeviceParameters(resolveSubmissionDeviceParameters(row, activeOrderId))
                    .setClearanceConfirmations(resolveClearanceConfirmations(row, activeOrderId))
                    .setMaterials(resolveSubmissionMaterials(row, activeOrderId)));
            submittedQuantity = submittedQuantity.add(row.getSubmittedQuantity());
        }

        private void setInputMaterials(List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> inputMaterials) {
            this.inputMaterials = List.copyOf(inputMaterials);
        }

        private void setSupplementMaterials(
                List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail> supplementMaterials) {
            this.supplementMaterials = List.copyOf(supplementMaterials);
        }

        private void addPqcSubmission(MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail submission) {
            this.pqcSubmissions.add(submission);
        }

        private Map<Long, MesTeamLeaderActiveOrderDetail.SignatureDetail> productionSubmitterSignaturesByEventId() {
            Map<Long, MesTeamLeaderActiveOrderDetail.SignatureDetail> signaturesByEventId = new LinkedHashMap<>();
            for (MesTeamLeaderActiveOrderDetail.SubmissionDetail submission : submissions) {
                if (submission.getEventId() == null || submission.getSubmitterSignature() == null
                        || submission.getSubmitterSignature().getSignatureId() == null) {
                    continue;
                }
                signaturesByEventId.put(submission.getEventId(), submission.getSubmitterSignature());
            }
            return signaturesByEventId;
        }

        private MesTeamLeaderActiveOrderDetail.ProcessDetail toDetail() {
            BigDecimal overageQuantity = submittedQuantity.subtract(requiredQuantityOf(process));
            if (overageQuantity.compareTo(BigDecimal.ZERO) < 0) {
                overageQuantity = BigDecimal.ZERO;
            }
            boolean quantityConflict = overageQuantity.compareTo(BigDecimal.ZERO) > 0;
            submissions.forEach(submission -> submission.setQuantityConflict(quantityConflict));
            return process
                    .setSubmittedQuantity(submittedQuantity)
                    .setSubmissionCount(submissions.size())
                    .setQuantityConflict(quantityConflict)
                    .setOverageQuantity(overageQuantity)
                    .setInputMaterials(inputMaterials)
                    .setSupplementMaterials(supplementMaterials)
                    .setSubmissions(List.copyOf(submissions))
                    .setPqcSubmissions(List.copyOf(pqcSubmissions));
        }
    }

    private static List<MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> resolveSubmissionDevices(
            MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices = new LinkedHashMap<>();
        Map<?, ?> payload = parseOriginalPayload(row.getOriginalPayloadJson(), activeOrderId);
        if (payload != null) {
            addDevicesFromValue(devices, payload.get("selectedDevice"), activeOrderId);
            addDevicesFromValue(devices, payload.get("selectedDevices"), activeOrderId);
            addDevicesFromMaterialDetails(devices, payload.get("materialDetails"), activeOrderId);
            addDevicesFromValue(devices, payload.get("deviceParameterReadings"), activeOrderId);
            addDeviceMeteringValidityFromValue(devices, payload.get("deviceMeteringValidity"), activeOrderId);
        }
        addDevice(devices, row.getEventDeviceId(), row.getEventDeviceCode(), row.getEventDeviceName());
        return List.copyOf(devices.values());
    }

    private static List<MesTeamLeaderActiveOrderDetail.SubmissionMaterialDetail> resolveSubmissionMaterials(
            MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        Map<?, ?> payload = parseOriginalPayload(row.getOriginalPayloadJson(), activeOrderId);
        if (payload == null) {
            return List.of();
        }
        Object value = payload.get("materialDetails");
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> materialDetails)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<MesTeamLeaderActiveOrderDetail.SubmissionMaterialDetail> rows = new ArrayList<>();
        for (Object materialDetail : materialDetails) {
            if (!(materialDetail instanceof Map<?, ?> detail)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices = new LinkedHashMap<>();
            addDevicesFromValue(devices, detail.get("selectedDevice"), activeOrderId);
            addDevicesFromValue(devices, detail.get("selectedDevices"), activeOrderId);
            addDevicesFromValue(devices, detail.get("deviceParameterReadings"), activeOrderId);
            addDeviceMeteringValidityFromValue(devices, detail.get("deviceMeteringValidity"), activeOrderId);
            rows.add(new MesTeamLeaderActiveOrderDetail.SubmissionMaterialDetail()
                    .setMaterialId(requirePositiveLongValue(detail.get("materialId"), activeOrderId))
                    .setMaterialCode(requireStringValue(detail.get("materialCode"), activeOrderId))
                    .setMaterialName(requireStringValue(detail.get("materialName"), activeOrderId))
                    .setMaterialSpecification(stringValue(detail.get("materialSpecification")))
                    .setOutputQuantity(bigDecimalValue(detail.get("outputQuantity"), activeOrderId))
                    .setLossQuantity(bigDecimalValue(detail.get("lossQuantity"), activeOrderId))
                    .setDevices(List.copyOf(devices.values()))
                    .setDeviceParameters(resolveSubmissionDeviceParameters(detail.get("deviceParameterReadings"),
                            activeOrderId))
                    .setClearanceConfirmations(resolveClearanceConfirmations(detail.get("clearanceConfirmations"),
                            activeOrderId)));
        }
        return List.copyOf(rows);
    }

    private static List<MesTeamLeaderActiveOrderDetail.SubmissionDeviceParameterDetail>
    resolveSubmissionDeviceParameters(MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        Map<?, ?> payload = parseOriginalPayload(row.getOriginalPayloadJson(), activeOrderId);
        if (payload == null) {
            return List.of();
        }
        return resolveSubmissionDeviceParameters(payload.get("deviceParameterReadings"), activeOrderId);
    }

    private static List<MesTeamLeaderActiveOrderDetail.SubmissionDeviceParameterDetail>
    resolveSubmissionDeviceParameters(Object value, Long activeOrderId) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> readings)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<MesTeamLeaderActiveOrderDetail.SubmissionDeviceParameterDetail> rows = new ArrayList<>();
        for (Object reading : readings) {
            if (!(reading instanceof Map<?, ?> detail)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            rows.add(new MesTeamLeaderActiveOrderDetail.SubmissionDeviceParameterDetail()
                    .setDeviceId(longValue(detail.get("deviceId"), activeOrderId))
                    .setDeviceCode(stringValue(detail.get("deviceCode")))
                    .setDeviceName(stringValue(detail.get("deviceName")))
                    .setParameterCode(requireStringValue(detail.get("parameterCode"), activeOrderId))
                    .setParameterName(stringValue(detail.get("parameterName")))
                    .setUnit(stringValue(detail.get("unit")))
                    .setValue(bigDecimalValueOrNull(detail.get("value"), activeOrderId))
                    .setTextValue(firstNonBlankString(detail.get("textValue"), detail.get("value")))
                    .setLowerLimit(bigDecimalValueOrNull(detail.get("lowerLimit"), activeOrderId))
                    .setUpperLimit(bigDecimalValueOrNull(detail.get("upperLimit"), activeOrderId))
                    .setParameterStatus(stringValue(detail.get("parameterStatus"))));
        }
        return List.copyOf(rows);
    }

    private static List<MesTeamLeaderActiveOrderDetail.ClearanceConfirmationDetail> resolveClearanceConfirmations(
            MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        Map<?, ?> payload = parseOriginalPayload(row.getOriginalPayloadJson(), activeOrderId);
        if (payload == null) {
            return List.of();
        }
        return resolveClearanceConfirmations(payload.get("clearanceConfirmations"), activeOrderId);
    }

    private static List<MesTeamLeaderActiveOrderDetail.ClearanceConfirmationDetail> resolveClearanceConfirmations(
            Object value, Long activeOrderId) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> confirmations)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<MesTeamLeaderActiveOrderDetail.ClearanceConfirmationDetail> rows = new ArrayList<>();
        for (Object confirmation : confirmations) {
            if (!(confirmation instanceof Map<?, ?> detail)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            rows.add(new MesTeamLeaderActiveOrderDetail.ClearanceConfirmationDetail()
                    .setKey(requireStringValue(detail.get("key"), activeOrderId))
                    .setLabel(requireStringValue(detail.get("label"), activeOrderId))
                    .setConfirmed(booleanValue(detail.get("confirmed"), activeOrderId))
                    .setDescription(stringValue(detail.get("description"))));
        }
        return List.copyOf(rows);
    }

    private static Map<?, ?> parseOriginalPayload(String originalPayloadJson, Long activeOrderId) {
        if (originalPayloadJson == null || originalPayloadJson.isBlank()) {
            return null;
        }
        try {
            Map<?, ?> payload = JsonUtils.parseObject(originalPayloadJson, Map.class);
            if (payload == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            return payload;
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private static void addDevicesFromValue(
            Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices, Object value,
            Long activeOrderId) {
        if (value instanceof List<?> rows) {
            for (Object item : rows) {
                addDevicesFromValue(devices, item, activeOrderId);
            }
            return;
        }
        if (value instanceof Map<?, ?> row) {
            addDevice(devices, longValue(row.get("deviceId"), activeOrderId), stringValue(row.get("deviceCode")),
                    stringValue(row.get("deviceName")), optionalBooleanValue(row.get("inMeteringValidityPeriod"), activeOrderId));
        }
    }

    private static void addDeviceMeteringValidityFromValue(
            Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices, Object value,
            Long activeOrderId) {
        if (value instanceof List<?> rows) {
            for (Object item : rows) {
                addDeviceMeteringValidityFromValue(devices, item, activeOrderId);
            }
            return;
        }
        if (value instanceof Map<?, ?> row && row.get("inMeteringValidityPeriod") instanceof Boolean inPeriod) {
            addDevice(devices, longValue(row.get("deviceId"), activeOrderId), stringValue(row.get("deviceCode")),
                    stringValue(row.get("deviceName")), inPeriod);
        }
    }

    private static void addDevicesFromMaterialDetails(
            Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices, Object value,
            Long activeOrderId) {
        if (!(value instanceof List<?> materialDetails)) {
            return;
        }
        for (Object materialDetail : materialDetails) {
            if (!(materialDetail instanceof Map<?, ?> detail)) {
                continue;
            }
            addDevicesFromValue(devices, detail.get("selectedDevice"), activeOrderId);
            addDevicesFromValue(devices, detail.get("selectedDevices"), activeOrderId);
            addDevicesFromValue(devices, detail.get("deviceParameterReadings"), activeOrderId);
            addDeviceMeteringValidityFromValue(devices, detail.get("deviceMeteringValidity"), activeOrderId);
        }
    }

    private static void addDevice(Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices,
                                  Long deviceId, String deviceCode, String deviceName) {
        addDevice(devices, deviceId, deviceCode, deviceName, null);
    }

    private static void addDevice(Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices,
                                  Long deviceId, String deviceCode, String deviceName,
                                  Boolean inMeteringValidityPeriod) {
        String normalizedCode = trimToNull(deviceCode);
        String normalizedName = trimToNull(deviceName);
        String key = deviceKey(deviceId, normalizedCode, normalizedName);
        if (key == null) {
            return;
        }
        MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail existing = devices.get(key);
        if (existing == null) {
            devices.put(key, new MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail()
                    .setDeviceId(deviceId)
                    .setDeviceCode(normalizedCode)
                    .setDeviceName(normalizedName)
                    .setInMeteringValidityPeriod(inMeteringValidityPeriod));
            return;
        }
        if (existing.getDeviceCode() == null && normalizedCode != null) {
            existing.setDeviceCode(normalizedCode);
        }
        if (existing.getDeviceName() == null && normalizedName != null) {
            existing.setDeviceName(normalizedName);
        }
        if (existing.getInMeteringValidityPeriod() == null && inMeteringValidityPeriod != null) {
            existing.setInMeteringValidityPeriod(inMeteringValidityPeriod);
        }
    }

    private static String deviceKey(Long deviceId, String deviceCode, String deviceName) {
        if (deviceId != null && deviceId > 0) {
            return "id:" + deviceId;
        }
        if (deviceCode != null) {
            return "code:" + deviceCode;
        }
        if (deviceName != null) {
            return "name:" + deviceName;
        }
        return null;
    }

    private static Long longValue(Object value, Long activeOrderId) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private static Long requirePositiveLongValue(Object value, Long activeOrderId) {
        Long parsed = longValue(value, activeOrderId);
        if (parsed == null || parsed <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return parsed;
    }

    private static String stringValue(Object value) {
        return trimToNull(value);
    }

    private static String requireStringValue(Object value, Long activeOrderId) {
        String text = trimToNull(value);
        if (text == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return text;
    }

    private static BigDecimal bigDecimalValue(Object value, Long activeOrderId) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = trimToNull(value);
        if (text == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private static BigDecimal bigDecimalValueOrNull(Object value, Long activeOrderId) {
        if (value == null) {
            return null;
        }
        String text = trimToNull(value);
        if (text == null || "true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return null;
        }
        return bigDecimalValue(value, activeOrderId);
    }

    private static Boolean booleanValue(Object value, Long activeOrderId) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = trimToNull(value);
        if (text == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
    }

    private static Boolean optionalBooleanValue(Object value, Long activeOrderId) {
        if (value == null) {
            return null;
        }
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        return booleanValue(value, activeOrderId);
    }

    private static String firstNonBlankString(Object... values) {
        for (Object value : values) {
            String text = trimToNull(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static String normalizePqcInspectionType(String inspectionType) {
        String text = trimToNull(inspectionType);
        return text != null && text.startsWith("PATROL") ? "PATROL" : text;
    }
}
