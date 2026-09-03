package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderVersionUpgradeRequestDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderVersionUpgradeRequestMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_STATE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_CONFIRM_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_FREEZE_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_ONGOING_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_TARGET_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;

@Service
@Validated
public class MesTeamLeaderActiveOrderVersionUpgradeServiceImpl
        implements MesTeamLeaderActiveOrderVersionUpgradeService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_VERSION_UPGRADE_PENDING = "VERSION_UPGRADE_PENDING";
    private static final String REQUEST_STATUS_APPLIED = "APPLIED";
    private static final String REQUEST_STATUS_REJECTED = "REJECTED";
    private static final String REQUEST_STATUS_CANCELLED = "CANCELLED";
    private static final String REQUEST_STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String APPROVAL_STATUS_APPROVED = "APPROVED";
    private static final String APPROVAL_STATUS_PENDING = "PENDING";
    private static final String APPROVAL_STATUS_REJECTED = "REJECTED";
    private static final String APPROVAL_STATUS_CANCELLED = "CANCELLED";
    private static final String FREEZE_STATUS_APPLIED = "APPLIED";
    private static final String FREEZE_STATUS_RELEASED = "RELEASED";
    private static final String FREEZE_STATUS_OLD_ORDER_FROZEN = "OLD_ORDER_FROZEN";
    private static final String OBJECT_TYPE_ROUTE = "PROCESS_ROUTE";
    private static final String OBJECT_TYPE_QA = "QA_INSPECTION_REGULATION";
    private static final String BUSINESS_APPROVAL_DATA_DOMAIN = "MES";
    private static final String BUSINESS_APPROVAL_SYSTEM_CODE = "MES";
    private static final String BUSINESS_APPROVAL_OBJECT_TYPE = "MES_ACTIVE_ORDER";
    private static final String BUSINESS_APPROVAL_ACTION_CODE = "VERSION_UPGRADE_RESTART";
    private static final String BUSINESS_APPROVAL_OBJECT_STATE = STATUS_VERSION_UPGRADE_PENDING;
    private static final String BUSINESS_APPROVAL_BUSINESS_TYPE = "MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderVersionUpgradeRequestMapper versionUpgradeRequestMapper;
    private final MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    private final MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrWorkTaskService workTaskService;
    private final MesReportAllocationOrderChangeService reportAllocationOrderChangeService;
    private final MesTeamLeaderActiveOrderService activeOrderService;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper regulationVersionMapper;
    private final ObjectProvider<BusinessApprovalOrchestrator> approvalOrchestratorProvider;

    public MesTeamLeaderActiveOrderVersionUpgradeServiceImpl(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProcessPoolActiveOrderVersionUpgradeRequestMapper versionUpgradeRequestMapper,
            MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper,
            MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper,
            MesProcessPoolTeamMaintenanceAuditMapper auditMapper,
            MesProEdhrBatchExecutionMapper batchExecutionMapper,
            MesProEdhrWorkTaskService workTaskService,
            MesReportAllocationOrderChangeService reportAllocationOrderChangeService,
            MesTeamLeaderActiveOrderService activeOrderService,
            MesProWorkOrderMapper workOrderMapper,
            MesProRouteMapper routeMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesQaInspectionRegulationMapper regulationMapper,
            MesQaInspectionRegulationVersionMapper regulationVersionMapper,
            ObjectProvider<BusinessApprovalOrchestrator> approvalOrchestratorProvider) {
        this.activeOrderMapper = activeOrderMapper;
        this.versionUpgradeRequestMapper = versionUpgradeRequestMapper;
        this.pickListBindingMapper = pickListBindingMapper;
        this.pickListBindingItemMapper = pickListBindingItemMapper;
        this.auditMapper = auditMapper;
        this.batchExecutionMapper = batchExecutionMapper;
        this.workTaskService = workTaskService;
        this.reportAllocationOrderChangeService = reportAllocationOrderChangeService;
        this.activeOrderService = activeOrderService;
        this.workOrderMapper = workOrderMapper;
        this.routeMapper = routeMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.regulationMapper = regulationMapper;
        this.regulationVersionMapper = regulationVersionMapper;
        this.approvalOrchestratorProvider = approvalOrchestratorProvider;
    }

    @Override
    public MesTeamLeaderActiveOrderVersionUpgradePreview preview(Long leaderUserId, Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = requireOwnedActiveOrder(leaderUserId, activeOrderId);
        MesProWorkOrderDO workOrder = activeOrder.getWorkOrderId() == null
                ? null : workOrderMapper.selectById(activeOrder.getWorkOrderId());
        MesProRouteDO route = activeOrder.getRouteId() == null ? null : routeMapper.selectById(activeOrder.getRouteId());
        MesProRouteVersionDO currentRouteVersion = activeOrder.getRouteVersionId() == null
                ? null : routeVersionMapper.selectById(activeOrder.getRouteVersionId());
        MesProRouteVersionDO targetRouteVersion = activeOrder.getRouteId() == null
                ? null : routeVersionMapper.selectActiveByRouteId(activeOrder.getRouteId());
        MesQaInspectionRegulationDO regulation = activeOrder.getQaRegulationId() == null
                ? null : regulationMapper.selectById(activeOrder.getQaRegulationId());
        MesQaInspectionRegulationVersionDO currentQaVersion = activeOrder.getQaRegulationVersionId() == null
                ? null : regulationVersionMapper.selectById(activeOrder.getQaRegulationVersionId());
        MesQaInspectionRegulationVersionDO targetQaVersion = activeOrder.getQaRegulationId() == null
                ? null : regulationVersionMapper.selectLatestPublishedByRegulationId(activeOrder.getQaRegulationId());

        List<String> blockers = new ArrayList<>();
        if (targetRouteVersion == null) {
            blockers.add("工艺路线缺少当前 ACTIVE 正式版本");
        }
        if (targetQaVersion == null) {
            blockers.add("QA 检验规程缺少最新 PUBLISHED 正式版本");
        }
        List<MesTeamLeaderActiveOrderVersionUpgradeVersionLine> lines = List.of(
                buildRouteLine(route, currentRouteVersion, targetRouteVersion),
                buildQaLine(regulation, currentQaVersion, targetQaVersion));
        boolean changed = lines.stream().anyMatch(line -> Boolean.TRUE.equals(line.getChanged()));
        if (!changed && blockers.isEmpty()) {
            blockers.add("全部受控对象已是最新正式版本，无需发起版本升级重启");
        }
        return new MesTeamLeaderActiveOrderVersionUpgradePreview()
                .setActiveOrderId(activeOrder.getId())
                .setWorkOrderId(activeOrder.getWorkOrderId())
                .setWorkOrderCode(workOrder == null ? null : workOrder.getCode())
                .setAllLatestFormalVersions(blockers.isEmpty())
                .setPerVersionSelectionAllowed(false)
                .setSubmittable(blockers.isEmpty() && changed)
                .setBlockers(blockers)
                .setCurrentVersions(lines)
                .setTargetVersions(lines);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderVersionUpgradeSubmitResult submit(
            Long leaderUserId, MesTeamLeaderActiveOrderVersionUpgradeSubmitCommand command) {
        if (command == null || command.getActiveOrderId() == null || StrUtil.isBlank(command.getIdempotencyKey())
                || StrUtil.isBlank(command.getUpgradeReason())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderVersionUpgradeSubmit");
        }
        if (!Boolean.TRUE.equals(command.getConfirmRestartFromBeginning())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_CONFIRM_REQUIRED, command.getActiveOrderId());
        }
        MesProcessPoolActiveOrderVersionUpgradeRequestDO idempotentRequest =
                versionUpgradeRequestMapper.selectByIdempotencyKey(
                        command.getActiveOrderId(), command.getIdempotencyKey());
        if (idempotentRequest != null) {
            return toResult(idempotentRequest);
        }
        MesTeamLeaderActiveOrderVersionUpgradePreview preview = preview(leaderUserId, command.getActiveOrderId());
        if (!Boolean.TRUE.equals(preview.getSubmittable())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_TARGET_REQUIRED,
                    command.getActiveOrderId(), preview.getBlockers());
        }
        MesProcessPoolActiveOrderVersionUpgradeRequestDO ongoingRequest =
                versionUpgradeRequestMapper.selectOngoingBySourceActiveOrderId(command.getActiveOrderId());
        if (ongoingRequest != null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_ONGOING_EXISTS,
                    command.getActiveOrderId(), ongoingRequest.getRequestCode());
        }
        MesProcessPoolActiveOrderDO lockedActiveOrder = activeOrderMapper.selectByIdForUpdate(command.getActiveOrderId());
        if (lockedActiveOrder == null || !Objects.equals(lockedActiveOrder.getLeaderUserId(), leaderUserId)
                || !STATUS_ACTIVE.equals(lockedActiveOrder.getActiveStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, command.getActiveOrderId());
        }
        LocalDateTime now = LocalDateTime.now();
        int frozen = activeOrderMapper.freezeForVersionUpgrade(
                lockedActiveOrder.getId(), lockedActiveOrder.getVersion(), leaderUserId, now);
        if (frozen != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_FREEZE_CONFLICT,
                    lockedActiveOrder.getId(), lockedActiveOrder.getVersion());
        }
        String currentSnapshotJson = toJson(buildCurrentSnapshot(lockedActiveOrder, preview));
        String targetSnapshotJson = toJson(buildTargetSnapshot(preview));
        String snapshotHash = sha256(currentSnapshotJson + "\n" + targetSnapshotJson);
        MesProcessPoolActiveOrderVersionUpgradeRequestDO request =
                new MesProcessPoolActiveOrderVersionUpgradeRequestDO()
                        .setSourceActiveOrderId(lockedActiveOrder.getId())
                        .setSourceWorkOrderId(lockedActiveOrder.getWorkOrderId())
                        .setSourceBatchExecutionId(null)
                        .setRequestCode(buildRequestCode(lockedActiveOrder.getId(), now))
                        .setIdempotencyKey(command.getIdempotencyKey())
                        .setRequestStatus(REQUEST_STATUS_PENDING_APPROVAL)
                        .setApprovalStatus(APPROVAL_STATUS_PENDING)
                        .setFreezeStatus(FREEZE_STATUS_OLD_ORDER_FROZEN)
                        .setUpgradeReason(command.getUpgradeReason())
                        .setCurrentSnapshotJson(currentSnapshotJson)
                        .setTargetSnapshotJson(targetSnapshotJson)
                        .setSnapshotHash(snapshotHash)
                        .setRequestedBy(leaderUserId)
                        .setRequestedAt(now);
        versionUpgradeRequestMapper.insert(request);
        BusinessApprovalOrchestrator approvalOrchestrator = approvalOrchestratorProvider.getObject();
        BusinessApprovalRequest approvalRequest = approvalOrchestrator.submit(
                buildApprovalContext(lockedActiveOrder, request, preview, targetSnapshotJson));
        if (approvalRequest != null && StrUtil.isNotBlank(approvalRequest.getProcessInstanceId())) {
            request.setApprovalProcessInstanceId(approvalRequest.getProcessInstanceId());
        }
        return toResult(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markApprovalPending(Long requestId, String processInstanceId, Long actorUserId) {
        if (requestId == null || StrUtil.isBlank(processInstanceId) || actorUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderVersionUpgradeMarkPending");
        }
        MesProcessPoolActiveOrderVersionUpgradeRequestDO request =
                versionUpgradeRequestMapper.selectByIdForUpdate(requestId);
        if (request == null
                || !REQUEST_STATUS_PENDING_APPROVAL.equals(request.getRequestStatus())
                || !APPROVAL_STATUS_PENDING.equals(request.getApprovalStatus())
                || !FREEZE_STATUS_OLD_ORDER_FROZEN.equals(request.getFreezeStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_STATE_INVALID,
                    requestId, request == null ? null : request.getRequestStatus(),
                    request == null ? null : request.getApprovalStatus(),
                    request == null ? null : request.getFreezeStatus());
        }
        if (StrUtil.isNotBlank(request.getApprovalProcessInstanceId())) {
            if (!StrUtil.equals(request.getApprovalProcessInstanceId(), processInstanceId)) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                        requestId, request.getSourceActiveOrderId());
            }
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = versionUpgradeRequestMapper.markApprovalPending(requestId, StrUtil.trim(processInstanceId),
                actorUserId, now, "版本升级重启审批流程已发起");
        if (updated != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                    requestId, request.getSourceActiveOrderId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderVersionUpgradeApplyResult applyApprovedUpgrade(
            Long requestId, Long actorUserId) {
        if (requestId == null || actorUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderVersionUpgradeApply");
        }
        MesProcessPoolActiveOrderVersionUpgradeRequestDO request =
                versionUpgradeRequestMapper.selectByIdForUpdate(requestId);
        if (request == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_STATE_INVALID,
                    requestId, null, null, null);
        }
        if (REQUEST_STATUS_APPLIED.equals(request.getRequestStatus())
                && APPROVAL_STATUS_APPROVED.equals(request.getApprovalStatus())
                && FREEZE_STATUS_APPLIED.equals(request.getFreezeStatus())) {
            return toApplyResult(request, request.getSourceBatchExecutionId());
        }
        if (!REQUEST_STATUS_PENDING_APPROVAL.equals(request.getRequestStatus())
                || !APPROVAL_STATUS_PENDING.equals(request.getApprovalStatus())
                || !FREEZE_STATUS_OLD_ORDER_FROZEN.equals(request.getFreezeStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_STATE_INVALID,
                    request.getId(), request.getRequestStatus(), request.getApprovalStatus(),
                    request.getFreezeStatus());
        }
        MesProcessPoolActiveOrderDO sourceActiveOrder =
                activeOrderMapper.selectByIdForUpdate(request.getSourceActiveOrderId());
        if (sourceActiveOrder == null
                || !Objects.equals(sourceActiveOrder.getLeaderUserId(), request.getRequestedBy())
                || !STATUS_VERSION_UPGRADE_PENDING.equals(sourceActiveOrder.getActiveStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                    request.getId(), request.getSourceActiveOrderId());
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(request.getSourceWorkOrderId());
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
        }
        MesProEdhrBatchExecutionDO oldBatch = selectCurrentBatch(sourceActiveOrder, workOrder);
        if (oldBatch != null) {
            workTaskService.cancelActiveTasksByBatch(oldBatch.getId(),
                    "活跃订单版本升级审批通过，旧批次作废");
            int voided = batchExecutionMapper.voidForVersionUpgrade(oldBatch.getId(), actorUserId,
                    "\n[VERSION_UPGRADE_VOID][requestCode=" + request.getRequestCode()
                            + "][sourceActiveOrderId=" + sourceActiveOrder.getId() + "]");
            if (voided != 1) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                        request.getId(), sourceActiveOrder.getId());
            }
        }
        reportAllocationOrderChangeService.invalidateActiveOrder(sourceActiveOrder.getId(), actorUserId,
                "活跃订单版本升级审批通过，旧订单作废");
        LocalDateTime now = LocalDateTime.now();
        Long targetRouteVersionId = extractTargetVersionId(request, OBJECT_TYPE_ROUTE);
        Long targetQaRegulationVersionId = extractTargetVersionId(request, OBJECT_TYPE_QA);
        int removed = activeOrderMapper.removePendingVersionUpgradeOrder(sourceActiveOrder.getId(),
                sourceActiveOrder.getLeaderUserId(), sourceActiveOrder.getVersion(), actorUserId, now);
        if (removed != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                    request.getId(), sourceActiveOrder.getId());
        }
        List<MesProcessPoolActiveOrderPickListBindingDO> oldPickListBindings =
                pickListBindingMapper.selectListByActiveOrderId(sourceActiveOrder.getId());
        MesTeamLeaderActiveOrderAddResult addResult = activeOrderService.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(sourceActiveOrder.getLeaderUserId())
                        .workOrderId(sourceActiveOrder.getWorkOrderId())
                        .idempotencyKey("VERSION-UPGRADE-" + request.getId())
                        .forceNewVersionUpgradeOrder(Boolean.TRUE)
                        .targetRouteVersionId(targetRouteVersionId)
                        .targetQaRegulationVersionId(targetQaRegulationVersionId)
                        .build());
        for (int index = 0; index < oldPickListBindings.size(); index++) {
            copyPickListBinding(oldPickListBindings.get(index), addResult.getActiveOrderId(), actorUserId,
                    request.getId(), now);
        }
        int applied = versionUpgradeRequestMapper.markApplied(request.getId(), addResult.getActiveOrderId(),
                null, actorUserId, now, "审批通过后已作废旧订单并按全部最新正式版本重新加入活跃订单");
        if (applied != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                    request.getId(), sourceActiveOrder.getId());
        }
        request.setRequestStatus(REQUEST_STATUS_APPLIED)
                .setApprovalStatus(APPROVAL_STATUS_APPROVED)
                .setFreezeStatus(FREEZE_STATUS_APPLIED)
                .setTargetActiveOrderId(addResult.getActiveOrderId())
                .setTargetBatchExecutionId(null)
                .setAppliedAt(now);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, sourceActiveOrder.getLeaderUserId(), actorUserId,
                "APPLY_ACTIVE_ORDER_VERSION_UPGRADE", "ACTIVE_ORDER", sourceActiveOrder.getId(),
                "SUCCESS", "活跃订单版本升级审批通过后重启",
                "requestCode=" + request.getRequestCode() + ",sourceActiveOrderId=" + sourceActiveOrder.getId(),
                "targetActiveOrderId=" + addResult.getActiveOrderId()
                        + ",voidedBatchExecutionId=" + (oldBatch == null ? null : oldBatch.getId()));
        return toApplyResult(request, oldBatch == null ? null : oldBatch.getId());
    }

    private void copyPickListBinding(MesProcessPoolActiveOrderPickListBindingDO source, Long targetActiveOrderId,
                                     Long actorUserId, Long requestId, LocalDateTime boundAt) {
        if (source == null || source.getId() == null || source.getPickListId() == null
                || StrUtil.isBlank(source.getSourceSnapshotHash())
                || !"BOUND".equalsIgnoreCase(StrUtil.trim(source.getBindingStatus()))
                || source.getBindingVersion() == null || source.getBindingVersion() <= 0) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT, requestId,
                    targetActiveOrderId);
        }
        MesProcessPoolActiveOrderPickListBindingDO target = MesProcessPoolActiveOrderPickListBindingDO.builder()
                .id(IdUtil.getSnowflake().nextId())
                .activeOrderId(targetActiveOrderId)
                .workOrderId(source.getWorkOrderId())
                .pickListId(source.getPickListId())
                .sourceFid(source.getSourceFid())
                .sourceBillNo(source.getSourceBillNo())
                .sourceDocumentStatus(source.getSourceDocumentStatus())
                .sourceModifyTime(source.getSourceModifyTime())
                .sourceSnapshotHash(source.getSourceSnapshotHash())
                .bindingStatus(source.getBindingStatus())
                .boundBy(actorUserId)
                .boundAt(boundAt)
                .idempotencyKey("VERSION-UPGRADE-PICK-BINDING-" + requestId + "-" + source.getId())
                .requestPayloadHash(source.getRequestPayloadHash())
                .bindingVersion(source.getBindingVersion())
                .build();
        if (pickListBindingMapper.insert(target) != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT, requestId,
                    targetActiveOrderId);
        }
        List<MesProcessPoolActiveOrderPickListBindingItemDO> sourceItems =
                pickListBindingItemMapper.selectListByBindingId(source.getId());
        if (sourceItems == null || sourceItems.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT, requestId,
                    targetActiveOrderId);
        }
        for (MesProcessPoolActiveOrderPickListBindingItemDO sourceItem : sourceItems) {
            if (sourceItem == null || sourceItem.getId() == null || sourceItem.getPickListItemId() == null
                    || StrUtil.isBlank(sourceItem.getItemSnapshotHash())) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT, requestId,
                        targetActiveOrderId);
            }
            MesProcessPoolActiveOrderPickListBindingItemDO targetItem = MesProcessPoolActiveOrderPickListBindingItemDO
                    .builder()
                    .id(IdUtil.getSnowflake().nextId())
                    .bindingId(target.getId())
                    .pickListItemId(sourceItem.getPickListItemId())
                    .sourceEntryId(sourceItem.getSourceEntryId())
                    .sourceLineKey(sourceItem.getSourceLineKey())
                    .materialNumber(sourceItem.getMaterialNumber())
                    .materialName(sourceItem.getMaterialName())
                    .materialSpecification(sourceItem.getMaterialSpecification())
                    .unitName(sourceItem.getUnitName())
                    .requestedQuantity(sourceItem.getRequestedQuantity())
                    .actualQuantity(sourceItem.getActualQuantity())
                    .baseActualQuantity(sourceItem.getBaseActualQuantity())
                    .lotNumber(sourceItem.getLotNumber())
                    .productionOrderNo(sourceItem.getProductionOrderNo())
                    .productionOrderLineNo(sourceItem.getProductionOrderLineNo())
                    .sourceModifyTime(sourceItem.getSourceModifyTime())
                    .itemSnapshotHash(sourceItem.getItemSnapshotHash())
                    .build();
            if (pickListBindingItemMapper.insert(targetItem) != 1) {
                throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT, requestId,
                        targetActiveOrderId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrCancelApproval(Long requestId, Long actorUserId, String reason, boolean cancelled) {
        if (requestId == null || actorUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderVersionUpgradeRejectOrCancel");
        }
        MesProcessPoolActiveOrderVersionUpgradeRequestDO request =
                versionUpgradeRequestMapper.selectByIdForUpdate(requestId);
        if (request == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_STATE_INVALID,
                    requestId, null, null, null);
        }
        if (FREEZE_STATUS_RELEASED.equals(request.getFreezeStatus())
                && (REQUEST_STATUS_REJECTED.equals(request.getRequestStatus())
                || REQUEST_STATUS_CANCELLED.equals(request.getRequestStatus()))) {
            return;
        }
        if (!REQUEST_STATUS_PENDING_APPROVAL.equals(request.getRequestStatus())
                || !APPROVAL_STATUS_PENDING.equals(request.getApprovalStatus())
                || !FREEZE_STATUS_OLD_ORDER_FROZEN.equals(request.getFreezeStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_STATE_INVALID,
                    request.getId(), request.getRequestStatus(), request.getApprovalStatus(),
                    request.getFreezeStatus());
        }
        MesProcessPoolActiveOrderDO sourceActiveOrder =
                activeOrderMapper.selectByIdForUpdate(request.getSourceActiveOrderId());
        if (sourceActiveOrder == null
                || !Objects.equals(sourceActiveOrder.getLeaderUserId(), request.getRequestedBy())
                || !STATUS_VERSION_UPGRADE_PENDING.equals(sourceActiveOrder.getActiveStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                    request.getId(), request.getSourceActiveOrderId());
        }
        LocalDateTime now = LocalDateTime.now();
        int released = activeOrderMapper.releaseVersionUpgradeFreeze(sourceActiveOrder.getId(),
                sourceActiveOrder.getLeaderUserId(), sourceActiveOrder.getVersion(), actorUserId, now);
        if (released != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                    request.getId(), sourceActiveOrder.getId());
        }
        String requestStatus = cancelled ? REQUEST_STATUS_CANCELLED : REQUEST_STATUS_REJECTED;
        String approvalStatus = cancelled ? APPROVAL_STATUS_CANCELLED : APPROVAL_STATUS_REJECTED;
        String message = (cancelled ? "版本升级重启审批已取消，旧订单恢复活跃" : "版本升级重启审批被驳回，旧订单恢复活跃")
                + (StrUtil.isBlank(reason) ? "" : "：" + StrUtil.trim(reason));
        int updated = versionUpgradeRequestMapper.markRejectedOrCancelled(request.getId(), requestStatus,
                approvalStatus, actorUserId, now, message);
        if (updated != 1) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                    request.getId(), sourceActiveOrder.getId());
        }
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, sourceActiveOrder.getLeaderUserId(), actorUserId,
                cancelled ? "CANCEL_ACTIVE_ORDER_VERSION_UPGRADE" : "REJECT_ACTIVE_ORDER_VERSION_UPGRADE",
                "ACTIVE_ORDER", sourceActiveOrder.getId(), "SUCCESS", "活跃订单版本升级审批未通过后恢复",
                "requestCode=" + request.getRequestCode() + ",sourceActiveOrderId=" + sourceActiveOrder.getId(),
                "requestStatus=" + requestStatus + ",reason=" + (reason == null ? "" : reason));
    }

    private MesProcessPoolActiveOrderDO requireOwnedActiveOrder(Long leaderUserId, Long activeOrderId) {
        if (leaderUserId == null || activeOrderId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderVersionUpgradePreview");
        }
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)
                || !STATUS_ACTIVE.equals(activeOrder.getActiveStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        return activeOrder;
    }

    private static MesTeamLeaderActiveOrderVersionUpgradeVersionLine buildRouteLine(
            MesProRouteDO route, MesProRouteVersionDO currentVersion, MesProRouteVersionDO targetVersion) {
        return new MesTeamLeaderActiveOrderVersionUpgradeVersionLine()
                .setObjectType(OBJECT_TYPE_ROUTE)
                .setObjectId(route == null ? null : route.getId())
                .setObjectName(route == null ? "工艺路线" : route.getName())
                .setCurrentVersionId(currentVersion == null ? null : currentVersion.getId())
                .setCurrentVersionNo(currentVersion == null ? null : currentVersion.getVersionNo())
                .setTargetVersionId(targetVersion == null ? null : targetVersion.getId())
                .setTargetVersionNo(targetVersion == null ? null : targetVersion.getVersionNo())
                .setChanged(targetVersion != null && !Objects.equals(
                        currentVersion == null ? null : currentVersion.getId(), targetVersion.getId()));
    }

    private static MesTeamLeaderActiveOrderVersionUpgradeVersionLine buildQaLine(
            MesQaInspectionRegulationDO regulation, MesQaInspectionRegulationVersionDO currentVersion,
            MesQaInspectionRegulationVersionDO targetVersion) {
        return new MesTeamLeaderActiveOrderVersionUpgradeVersionLine()
                .setObjectType(OBJECT_TYPE_QA)
                .setObjectId(regulation == null ? null : regulation.getId())
                .setObjectName(regulation == null ? "QA 检验规程" : regulation.getRegulationName())
                .setCurrentVersionId(currentVersion == null ? null : currentVersion.getId())
                .setCurrentVersionNo(currentVersion == null ? null : currentVersion.getVersionNo())
                .setTargetVersionId(targetVersion == null ? null : targetVersion.getId())
                .setTargetVersionNo(targetVersion == null ? null : targetVersion.getVersionNo())
                .setChanged(targetVersion != null && !Objects.equals(
                        currentVersion == null ? null : currentVersion.getId(), targetVersion.getId()));
    }

    private static Map<String, Object> buildCurrentSnapshot(
            MesProcessPoolActiveOrderDO activeOrder, MesTeamLeaderActiveOrderVersionUpgradePreview preview) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("activeOrderId", activeOrder.getId());
        snapshot.put("workOrderId", activeOrder.getWorkOrderId());
        snapshot.put("workOrderCode", preview.getWorkOrderCode());
        snapshot.put("routeId", activeOrder.getRouteId());
        snapshot.put("routeVersionId", activeOrder.getRouteVersionId());
        snapshot.put("qaRegulationId", activeOrder.getQaRegulationId());
        snapshot.put("qaRegulationVersionId", activeOrder.getQaRegulationVersionId());
        snapshot.put("activeStatus", activeOrder.getActiveStatus());
        snapshot.put("businessStatus", activeOrder.getBusinessStatus());
        snapshot.put("version", activeOrder.getVersion());
        snapshot.put("currentVersions", preview.getCurrentVersions());
        return snapshot;
    }

    private static Map<String, Object> buildTargetSnapshot(MesTeamLeaderActiveOrderVersionUpgradePreview preview) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("activeOrderId", preview.getActiveOrderId());
        snapshot.put("workOrderId", preview.getWorkOrderId());
        snapshot.put("workOrderCode", preview.getWorkOrderCode());
        snapshot.put("allLatestFormalVersions", preview.getAllLatestFormalVersions());
        snapshot.put("perVersionSelectionAllowed", false);
        snapshot.put("restartFromBeginning", true);
        snapshot.put("targetVersions", preview.getTargetVersions());
        return snapshot;
    }

    private static BusinessApprovalContext buildApprovalContext(MesProcessPoolActiveOrderDO activeOrder,
                                                                MesProcessPoolActiveOrderVersionUpgradeRequestDO request,
                                                                MesTeamLeaderActiveOrderVersionUpgradePreview preview,
                                                                String targetSnapshotJson) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("businessType", BUSINESS_APPROVAL_BUSINESS_TYPE);
        variables.put("requestId", request.getId());
        variables.put("requestCode", request.getRequestCode());
        variables.put("sourceActiveOrderId", request.getSourceActiveOrderId());
        variables.put("sourceWorkOrderId", request.getSourceWorkOrderId());
        variables.put("workOrderCode", preview.getWorkOrderCode());
        variables.put("reasonText", request.getUpgradeReason());
        variables.put("targetVersionsSummary", summarizeTargetVersions(preview.getTargetVersions()));
        variables.put("targetSnapshotJson", targetSnapshotJson);
        return BusinessApprovalContext.builder()
                .tenantId(activeOrder.getTenantId())
                .dataDomain(BUSINESS_APPROVAL_DATA_DOMAIN)
                .systemCode(BUSINESS_APPROVAL_SYSTEM_CODE)
                .objectType(BUSINESS_APPROVAL_OBJECT_TYPE)
                .objectId(String.valueOf(request.getId()))
                .objectVersion(request.getSnapshotHash())
                .actionCode(BUSINESS_APPROVAL_ACTION_CODE)
                .objectState(BUSINESS_APPROVAL_OBJECT_STATE)
                .applicantUserId(request.getRequestedBy())
                .reason(request.getUpgradeReason())
                .variables(variables)
                .build();
    }

    private static String summarizeTargetVersions(List<MesTeamLeaderActiveOrderVersionUpgradeVersionLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        return lines.stream()
                .filter(line -> line != null && Boolean.TRUE.equals(line.getChanged()))
                .map(line -> line.getObjectName() + " " + nullToDash(line.getCurrentVersionNo())
                        + " -> " + nullToDash(line.getTargetVersionNo()))
                .reduce((left, right) -> left + "；" + right)
                .orElse("全部受控对象已是最新正式版本");
    }

    private static String nullToDash(Object value) {
        String text = value == null ? null : String.valueOf(value);
        return StrUtil.isBlank(text) ? "-" : StrUtil.trim(text);
    }

    private static String toJson(Object value) {
        try {
            return JsonUtils.getObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("active-order version-upgrade snapshot JSON serialization failed", ex);
        }
    }

    private static Long extractTargetVersionId(MesProcessPoolActiveOrderVersionUpgradeRequestDO request,
                                               String objectType) {
        Map<String, Object> snapshot;
        try {
            snapshot = JsonUtils.getObjectMapper().readValue(request.getTargetSnapshotJson(),
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("active-order version-upgrade target snapshot JSON is invalid", ex);
        }
        Object targetVersions = snapshot.get("targetVersions");
        if (!(targetVersions instanceof List<?> lines)) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                    request.getId(), request.getSourceActiveOrderId());
        }
        for (Object item : lines) {
            if (!(item instanceof Map<?, ?> line) || !Objects.equals(objectType, line.get("objectType"))) {
                continue;
            }
            Object targetVersionId = line.get("targetVersionId");
            if (targetVersionId instanceof Number number) {
                return number.longValue();
            }
            if (targetVersionId instanceof String text && StrUtil.isNotBlank(text)) {
                return Long.valueOf(StrUtil.trim(text));
            }
            break;
        }
        throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPLY_CONFLICT,
                request.getId(), request.getSourceActiveOrderId());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private static String buildRequestCode(Long activeOrderId, LocalDateTime requestedAt) {
        return "AOVU-" + activeOrderId + "-" + requestedAt.toString()
                .replace("-", "")
                .replace(":", "")
                .replace(".", "")
                .replace("T", "");
    }

    private static MesTeamLeaderActiveOrderVersionUpgradeSubmitResult toResult(
            MesProcessPoolActiveOrderVersionUpgradeRequestDO request) {
        return new MesTeamLeaderActiveOrderVersionUpgradeSubmitResult()
                .setActiveOrderId(request.getSourceActiveOrderId())
                .setRequestCode(request.getRequestCode())
                .setApprovalStatus(request.getApprovalStatus())
                .setFreezeStatus(request.getFreezeStatus());
    }

    private MesProEdhrBatchExecutionDO selectCurrentBatch(
            MesProcessPoolActiveOrderDO activeOrder, MesProWorkOrderDO workOrder) {
        String batchCode = StrUtil.trim(workOrder.getBatchCode());
        if (StrUtil.isBlank(batchCode) || activeOrder.getWorkOrderId() == null || activeOrder.getRouteId() == null) {
            return null;
        }
        return batchExecutionMapper.selectByActiveContextKey(
                buildActiveContextKey(activeOrder.getWorkOrderId(), batchCode, activeOrder.getRouteId()));
    }

    private static String buildActiveContextKey(Long workOrderId, String batchCode, Long routeId) {
        return workOrderId + "|" + routeId + "|" + StrUtil.trim(batchCode);
    }

    private static MesTeamLeaderActiveOrderVersionUpgradeApplyResult toApplyResult(
            MesProcessPoolActiveOrderVersionUpgradeRequestDO request, Long voidedBatchExecutionId) {
        return new MesTeamLeaderActiveOrderVersionUpgradeApplyResult()
                .setRequestId(request.getId())
                .setRequestCode(request.getRequestCode())
                .setSourceActiveOrderId(request.getSourceActiveOrderId())
                .setSourceWorkOrderId(request.getSourceWorkOrderId())
                .setTargetActiveOrderId(request.getTargetActiveOrderId())
                .setTargetBatchExecutionId(request.getTargetBatchExecutionId())
                .setVoidedBatchExecutionId(voidedBatchExecutionId)
                .setRequestStatus(request.getRequestStatus())
                .setApprovalStatus(request.getApprovalStatus())
                .setFreezeStatus(request.getFreezeStatus());
    }
}
