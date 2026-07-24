package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionAddSignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionAdminReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionTransferReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionWithdrawReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFlowEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFlowInterventionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrFlowEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrFlowInterventionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_FLOW_INTERVENTION_ACTION_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_FLOW_INTERVENTION_AUTHORIZATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_FLOW_INTERVENTION_BACKEND_MUTATION_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_FLOW_INTERVENTION_IDEMPOTENCY_KEY_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_FLOW_INTERVENTION_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_FLOW_INTERVENTION_SIGNOFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_STATUS_INVALID;

@Service
public class MesProEdhrFlowInterventionServiceImpl implements MesProEdhrFlowInterventionService {

    public static final String ACTION_RETURN = "RETURN";
    public static final String ACTION_WITHDRAW = "WITHDRAW";
    public static final String ACTION_TRANSFER = "TRANSFER";
    public static final String ACTION_ADD_SIGN = "ADD_SIGN";
    public static final String ACTION_ADMIN_INTERVENE = "ADMIN_INTERVENE";

    public static final String STATUS_RECORDED = "RECORDED";
    public static final String EVENT_TYPE_FLOW_INTERVENTION = "FLOW_INTERVENTION";
    public static final String INTEGRITY_RECHECK_REQUIRED = "RECHECK_REQUIRED";
    public static final String INTEGRITY_PASS = "PASS";
    public static final String PERMISSION_ALLOW_RECORDED = "ALLOW_RECORDED";

    private static final String BUSINESS_OBJECT_TYPE_WORK_TASK = "WORK_TASK";
    private static final String SOURCE_SQL = "SQL";
    private static final String SOURCE_MANUAL_SQL = "MANUAL_SQL";
    private static final String SOURCE_BACKEND_MUTATION = "BACKEND_MUTATION";
    private static final String SOURCE_STATUS_MUTATION = "STATUS_MUTATION";

    @Resource
    private MesProEdhrFlowInterventionMapper flowInterventionMapper;
    @Resource
    private MesProEdhrFlowEventMapper flowEventMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;

    @Override
    public PageResult<MesProEdhrFlowInterventionRespVO> getPage(MesProEdhrFlowInterventionPageReqVO reqVO) {
        return BeanUtils.toBean(flowInterventionMapper.selectPage(reqVO), MesProEdhrFlowInterventionRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrFlowEventRespVO> getEventPage(MesProEdhrFlowEventPageReqVO reqVO) {
        return BeanUtils.toBean(flowEventMapper.selectPage(reqVO), MesProEdhrFlowEventRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFlowInterventionRespVO returnBack(MesProEdhrFlowInterventionReturnReqVO reqVO) {
        return recordIntervention(ACTION_RETURN, "mes:pro-edhr-flow-intervention:return", reqVO.getBusinessObjectType(),
                reqVO.getBusinessObjectId(), reqVO.getBusinessObjectCode(), reqVO.getFlowInstanceId(), reqVO.getTaskId(),
                reqVO.getNodeKey(), reqVO.getFromStatus(), reqVO.getToStatus(), reqVO.getTargetTaskId(),
                reqVO.getTargetUserId(), reqVO.getReasonCategory(), reqVO.getReason(), null,
                reqVO.getSignoffEvidenceHash(), reqVO.getIdempotencyKey(), reqVO.getInterventionSource());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFlowInterventionRespVO withdraw(MesProEdhrFlowInterventionWithdrawReqVO reqVO) {
        return recordIntervention(ACTION_WITHDRAW, "mes:pro-edhr-flow-intervention:withdraw", reqVO.getBusinessObjectType(),
                reqVO.getBusinessObjectId(), reqVO.getBusinessObjectCode(), reqVO.getFlowInstanceId(), reqVO.getTaskId(),
                reqVO.getNodeKey(), reqVO.getFromStatus(), reqVO.getToStatus(), reqVO.getTargetTaskId(),
                reqVO.getTargetUserId(), reqVO.getReasonCategory(), reqVO.getReason(), null,
                reqVO.getSignoffEvidenceHash(), reqVO.getIdempotencyKey(), reqVO.getInterventionSource());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFlowInterventionRespVO transfer(MesProEdhrFlowInterventionTransferReqVO reqVO) {
        return recordIntervention(ACTION_TRANSFER, "mes:pro-edhr-flow-intervention:transfer", reqVO.getBusinessObjectType(),
                reqVO.getBusinessObjectId(), reqVO.getBusinessObjectCode(), reqVO.getFlowInstanceId(), reqVO.getTaskId(),
                reqVO.getNodeKey(), reqVO.getFromStatus(), reqVO.getToStatus(), reqVO.getTargetTaskId(),
                requireTargetUserId(reqVO.getTargetUserId()), reqVO.getReasonCategory(), reqVO.getReason(), null,
                reqVO.getSignoffEvidenceHash(), reqVO.getIdempotencyKey(), reqVO.getInterventionSource());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFlowInterventionRespVO addSign(MesProEdhrFlowInterventionAddSignReqVO reqVO) {
        return recordIntervention(ACTION_ADD_SIGN, "mes:pro-edhr-flow-intervention:add-sign", reqVO.getBusinessObjectType(),
                reqVO.getBusinessObjectId(), reqVO.getBusinessObjectCode(), reqVO.getFlowInstanceId(), reqVO.getTaskId(),
                reqVO.getNodeKey(), reqVO.getFromStatus(), reqVO.getToStatus(), reqVO.getTargetTaskId(),
                requireTargetUserId(reqVO.getTargetUserId()), reqVO.getReasonCategory(), reqVO.getReason(), null,
                reqVO.getSignoffEvidenceHash(), reqVO.getIdempotencyKey(), reqVO.getInterventionSource());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFlowInterventionRespVO adminIntervene(MesProEdhrFlowInterventionAdminReqVO reqVO) {
        String authorizationBasis = requireAuthorizationBasis(reqVO.getAuthorizationBasis());
        return recordIntervention(ACTION_ADMIN_INTERVENE, "mes:pro-edhr-flow-intervention:admin-intervene",
                reqVO.getBusinessObjectType(), reqVO.getBusinessObjectId(), reqVO.getBusinessObjectCode(),
                reqVO.getFlowInstanceId(), reqVO.getTaskId(), reqVO.getNodeKey(), reqVO.getFromStatus(),
                reqVO.getToStatus(), reqVO.getTargetTaskId(), reqVO.getTargetUserId(), reqVO.getReasonCategory(),
                reqVO.getReason(), authorizationBasis, reqVO.getSignoffEvidenceHash(), reqVO.getIdempotencyKey(),
                reqVO.getInterventionSource());
    }

    private MesProEdhrFlowInterventionRespVO recordIntervention(String action,
                                                               String permissionCode,
                                                               String businessObjectType,
                                                               String businessObjectId,
                                                               String businessObjectCode,
                                                               String flowInstanceId,
                                                               String taskId,
                                                               String nodeKey,
                                                               String fromStatus,
                                                               String toStatus,
                                                               String targetTaskId,
                                                               Long targetUserId,
                                                               String reasonCategory,
                                                               String rawReason,
                                                               String authorizationBasis,
                                                               String rawSignoffEvidenceHash,
                                                               String rawIdempotencyKey,
                                                               String interventionSource) {
        rejectBackendMutationPath(interventionSource);
        String reason = requireReason(rawReason);
        String signoffEvidenceHash = requireSignoffEvidence(rawSignoffEvidenceHash);
        String idempotencyKey = requireIdempotencyKey(rawIdempotencyKey);
        requireActionContext(action, businessObjectType, businessObjectId, fromStatus, toStatus);

        MesProEdhrFlowInterventionDO existing = flowInterventionMapper
                .selectByBusinessObjectTypeAndBusinessObjectIdAndInterventionActionAndIdempotencyKey(
                        StrUtil.trim(businessObjectType), StrUtil.trim(businessObjectId), action, idempotencyKey);
        if (existing != null) {
            return BeanUtils.toBean(existing, MesProEdhrFlowInterventionRespVO.class);
        }

        MesProEdhrWorkTaskDO changedWorkTask = applyWorkTaskTransferIfRequired(action, businessObjectType,
                businessObjectId, taskId, targetUserId, reason);
        MesProEdhrWorkTaskDO addSignTask = applyWorkTaskAddSignIfRequired(action, businessObjectType,
                businessObjectId, taskId, targetUserId, reason);
        if (addSignTask != null) {
            targetTaskId = String.valueOf(addSignTask.getId());
            changedWorkTask = addSignTask;
        }
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        String integrityCheckResult = resolveIntegrityCheckResult(action, changedWorkTask, businessObjectType,
                businessObjectId, flowInstanceId, taskId);
        String integrityCheckSnapshotJson = buildIntegritySnapshot(action, businessObjectType, businessObjectId,
                flowInstanceId, taskId, integrityCheckResult, occurredAt);
        String interventionCode = "EDHR-FLOW-" + action + "-" + occurredAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String evidenceHash = buildInterventionHash(action, businessObjectType, businessObjectId, fromStatus, toStatus,
                actorUserId, targetUserId, reason, authorizationBasis, signoffEvidenceHash, idempotencyKey,
                integrityCheckResult, integrityCheckSnapshotJson);

        MesProEdhrFlowInterventionDO intervention = MesProEdhrFlowInterventionDO.builder()
                .interventionCode(interventionCode)
                .businessObjectType(StrUtil.trim(businessObjectType))
                .businessObjectId(StrUtil.trim(businessObjectId))
                .businessObjectCode(StrUtil.trim(businessObjectCode))
                .flowInstanceId(StrUtil.trim(flowInstanceId))
                .interventionAction(action)
                .interventionStatus(STATUS_RECORDED)
                .fromStatus(StrUtil.trim(fromStatus))
                .toStatus(StrUtil.trim(toStatus))
                .sourceTaskId(StrUtil.trim(taskId))
                .targetTaskId(StrUtil.trim(targetTaskId))
                .nodeKey(StrUtil.trim(nodeKey))
                .targetUserId(targetUserId)
                .requestedBy(actorUserId)
                .requestedAt(occurredAt)
                .reasonCategory(StrUtil.trim(reasonCategory))
                .reason(reason)
                .authorizationBasis(authorizationBasis)
                .signoffEvidenceHash(signoffEvidenceHash)
                .idempotencyKey(idempotencyKey)
                .integrityCheckResult(integrityCheckResult)
                .integrityCheckSnapshotJson(integrityCheckSnapshotJson)
                .evidenceHash(evidenceHash)
                .build();
        flowInterventionMapper.insert(intervention);
        recordFlowEvent(intervention, permissionCode, PERMISSION_ALLOW_RECORDED, occurredAt);
        return BeanUtils.toBean(intervention, MesProEdhrFlowInterventionRespVO.class);
    }

    private MesProEdhrWorkTaskDO applyWorkTaskTransferIfRequired(String action,
                                                                 String businessObjectType,
                                                                 String businessObjectId,
                                                                 String taskId,
                                                                 Long targetUserId,
                                                                 String reason) {
        if (!ACTION_TRANSFER.equals(action) || !Objects.equals(StrUtil.trim(businessObjectType), BUSINESS_OBJECT_TYPE_WORK_TASK)) {
            return null;
        }
        Long workTaskId = parseWorkTaskId(StrUtil.blankToDefault(taskId, businessObjectId));
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!isActiveWorkTask(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        workTaskMapper.updateById(new MesProEdhrWorkTaskDO()
                .setId(workTask.getId())
                .setAssigneeUserId(targetUserId)
                .setSourceUserId(workTask.getAssigneeUserId())
                .setCandidateSourceType("USER")
                .setCandidateSourceId(targetUserId)
                .setCandidateUserSnapshot(String.valueOf(targetUserId))
                .setReason(reason)
                .setRemark("TRANSFER: " + workTask.getAssigneeUserId() + " -> " + targetUserId + "; " + reason));
        return workTask;
    }

    private MesProEdhrWorkTaskDO applyWorkTaskAddSignIfRequired(String action,
                                                                String businessObjectType,
                                                                String businessObjectId,
                                                                String taskId,
                                                                Long targetUserId,
                                                                String reason) {
        if (!ACTION_ADD_SIGN.equals(action) || !Objects.equals(StrUtil.trim(businessObjectType), BUSINESS_OBJECT_TYPE_WORK_TASK)) {
            return null;
        }
        Long workTaskId = parseWorkTaskId(StrUtil.blankToDefault(taskId, businessObjectId));
        MesProEdhrWorkTaskDO sourceTask = workTaskMapper.selectById(workTaskId);
        if (sourceTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!isActiveWorkTask(sourceTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        MesProEdhrWorkTaskDO addSignTask = new MesProEdhrWorkTaskDO()
                .setTaskCode("EDHR-AS-" + sourceTask.getId() + "-" + targetUserId)
                .setTaskType(sourceTask.getTaskType())
                .setBatchExecutionId(sourceTask.getBatchExecutionId())
                .setBatchTaskId(sourceTask.getBatchTaskId())
                .setBusinessScopeType(sourceTask.getBusinessScopeType())
                .setBusinessScopeId(sourceTask.getBusinessScopeId())
                .setExecutionId(sourceTask.getExecutionId())
                .setSourceExecutionId(sourceTask.getSourceExecutionId())
                .setWorkOrderId(sourceTask.getWorkOrderId())
                .setWorkOrderCode(sourceTask.getWorkOrderCode())
                .setBatchCode(sourceTask.getBatchCode())
                .setRouteId(sourceTask.getRouteId())
                .setRouteProcessId(sourceTask.getRouteProcessId())
                .setProcessId(sourceTask.getProcessId())
                .setProcessName(sourceTask.getProcessName())
                .setAssigneeUserId(targetUserId)
                .setCandidateSourceType("USER")
                .setCandidateSourceId(targetUserId)
                .setCandidateUserSnapshot(String.valueOf(targetUserId))
                .setSourceUserId(sourceTask.getAssigneeUserId())
                .setSignatureCellKey(sourceTask.getSignatureCellKey())
                .setSignatureRowIndex(sourceTask.getSignatureRowIndex())
                .setSignatureColumnIndex(sourceTask.getSignatureColumnIndex())
                .setReviewSourceType(sourceTask.getReviewSourceType())
                .setReviewSourceId(sourceTask.getReviewSourceId())
                .setReviewSourceName(sourceTask.getReviewSourceName())
                .setBpmTaskId(sourceTask.getBpmTaskId())
                .setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setDueTime(sourceTask.getDueTime())
                .setActionUrl(sourceTask.getActionUrl())
                .setReason(reason)
                .setRemark("ADD_SIGN:" + sourceTask.getId());
        workTaskMapper.insert(addSignTask);
        return addSignTask;
    }

    private Long parseWorkTaskId(String rawWorkTaskId) {
        try {
            return Long.valueOf(StrUtil.trim(rawWorkTaskId));
        } catch (NumberFormatException ex) {
            throw exception(PRO_EDHR_FLOW_INTERVENTION_ACTION_INVALID);
        }
    }

    private boolean isActiveWorkTask(String status) {
        return Objects.equals(status, MesProEdhrWorkTaskStatus.TODO)
                || Objects.equals(status, MesProEdhrWorkTaskStatus.DOING)
                || Objects.equals(status, MesProEdhrWorkTaskStatus.OVERDUE);
    }

    private void requireActionContext(String action, String businessObjectType, String businessObjectId,
                                      String fromStatus, String toStatus) {
        if (StrUtil.isNotBlank(action) && StrUtil.isNotBlank(businessObjectType) && StrUtil.isNotBlank(businessObjectId)
                && StrUtil.isNotBlank(fromStatus) && StrUtil.isNotBlank(toStatus)) {
            return;
        }
        throw exception(PRO_EDHR_FLOW_INTERVENTION_ACTION_INVALID);
    }

    private Long requireTargetUserId(Long targetUserId) {
        if (targetUserId != null) {
            return targetUserId;
        }
        throw exception(PRO_EDHR_FLOW_INTERVENTION_ACTION_INVALID);
    }

    private String requireReason(String rawReason) {
        String reason = StrUtil.trim(rawReason);
        if (StrUtil.isBlank(reason)) {
            throw exception(PRO_EDHR_FLOW_INTERVENTION_REASON_REQUIRED);
        }
        return reason;
    }

    private String requireSignoffEvidence(String rawSignoffEvidenceHash) {
        String signoffEvidenceHash = StrUtil.trim(rawSignoffEvidenceHash);
        if (StrUtil.isBlank(signoffEvidenceHash)) {
            throw exception(PRO_EDHR_FLOW_INTERVENTION_SIGNOFF_REQUIRED);
        }
        return signoffEvidenceHash;
    }

    private String requireAuthorizationBasis(String rawAuthorizationBasis) {
        String authorizationBasis = StrUtil.trim(rawAuthorizationBasis);
        if (StrUtil.isBlank(authorizationBasis)) {
            throw exception(PRO_EDHR_FLOW_INTERVENTION_AUTHORIZATION_REQUIRED);
        }
        return authorizationBasis;
    }

    private String requireIdempotencyKey(String rawIdempotencyKey) {
        String idempotencyKey = StrUtil.trim(rawIdempotencyKey);
        if (StrUtil.isBlank(idempotencyKey)) {
            throw exception(PRO_EDHR_FLOW_INTERVENTION_IDEMPOTENCY_KEY_REQUIRED);
        }
        return idempotencyKey;
    }

    private void rejectBackendMutationPath(String interventionSource) {
        String source = StrUtil.trim(interventionSource);
        if (Objects.equals(source, SOURCE_SQL) || Objects.equals(source, SOURCE_MANUAL_SQL)
                || Objects.equals(source, SOURCE_BACKEND_MUTATION) || Objects.equals(source, SOURCE_STATUS_MUTATION)) {
            throw exception(PRO_EDHR_FLOW_INTERVENTION_BACKEND_MUTATION_FORBIDDEN);
        }
    }

    private String runIntegrityRecheck(String action, String businessObjectType, String businessObjectId,
                                       String flowInstanceId, String taskId) {
        requireActionContext(action, businessObjectType, businessObjectId, "RECORDED_FROM", "RECORDED_TO");
        if (StrUtil.isBlank(flowInstanceId) || StrUtil.isBlank(taskId)) {
            return INTEGRITY_RECHECK_REQUIRED;
        }
        return INTEGRITY_RECHECK_REQUIRED;
    }

    private String resolveIntegrityCheckResult(String action, MesProEdhrWorkTaskDO changedWorkTask,
                                               String businessObjectType, String businessObjectId,
                                               String flowInstanceId, String taskId) {
        if ((ACTION_TRANSFER.equals(action) || ACTION_ADD_SIGN.equals(action)) && changedWorkTask != null) {
            return INTEGRITY_PASS;
        }
        return runIntegrityRecheck(action, businessObjectType, businessObjectId, flowInstanceId, taskId);
    }

    private String buildIntegritySnapshot(String action, String businessObjectType, String businessObjectId,
                                          String flowInstanceId, String taskId, String integrityCheckResult,
                                          LocalDateTime occurredAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("action", action);
        snapshot.put("businessObjectType", StrUtil.trim(businessObjectType));
        snapshot.put("businessObjectId", StrUtil.trim(businessObjectId));
        snapshot.put("flowInstanceId", StrUtil.trim(flowInstanceId));
        snapshot.put("taskId", StrUtil.trim(taskId));
        snapshot.put("integrityCheckResult", integrityCheckResult);
        snapshot.put("occurredAt", occurredAt);
        snapshot.put("nextRequiredCapability", "BPM task operation and electronic signature verification engine");
        return JSON.toJSONString(snapshot);
    }

    private void recordFlowEvent(MesProEdhrFlowInterventionDO intervention,
                                 String permissionCode,
                                 String permissionDecision,
                                 LocalDateTime occurredAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("interventionId", intervention.getId());
        snapshot.put("interventionCode", intervention.getInterventionCode());
        snapshot.put("businessObjectType", intervention.getBusinessObjectType());
        snapshot.put("businessObjectId", intervention.getBusinessObjectId());
        snapshot.put("businessObjectCode", intervention.getBusinessObjectCode());
        snapshot.put("flowInstanceId", intervention.getFlowInstanceId());
        snapshot.put("eventType", EVENT_TYPE_FLOW_INTERVENTION);
        snapshot.put("action", intervention.getInterventionAction());
        snapshot.put("fromStatus", intervention.getFromStatus());
        snapshot.put("toStatus", intervention.getToStatus());
        snapshot.put("actorUserId", intervention.getRequestedBy());
        snapshot.put("targetUserId", intervention.getTargetUserId());
        snapshot.put("permissionCode", permissionCode);
        snapshot.put("permissionDecision", permissionDecision);
        snapshot.put("reason", intervention.getReason());
        snapshot.put("signoffEvidenceHash", intervention.getSignoffEvidenceHash());
        snapshot.put("integrityCheckResult", intervention.getIntegrityCheckResult());
        snapshot.put("occurredAt", occurredAt);
        String eventSnapshotJson = JSON.toJSONString(snapshot);
        String evidenceHash = DigestUtil.sha256Hex(String.join("|",
                String.valueOf(intervention.getId()),
                intervention.getInterventionAction(),
                intervention.getBusinessObjectType(),
                intervention.getBusinessObjectId(),
                intervention.getFromStatus(),
                intervention.getToStatus(),
                String.valueOf(intervention.getRequestedBy()),
                permissionCode,
                intervention.getIdempotencyKey(),
                eventSnapshotJson));

        flowEventMapper.insert(MesProEdhrFlowEventDO.builder()
                .businessObjectType(intervention.getBusinessObjectType())
                .businessObjectId(intervention.getBusinessObjectId())
                .businessObjectCode(intervention.getBusinessObjectCode())
                .interventionId(intervention.getId())
                .flowInstanceId(intervention.getFlowInstanceId())
                .taskId(intervention.getSourceTaskId())
                .nodeKey(intervention.getNodeKey())
                .eventType(EVENT_TYPE_FLOW_INTERVENTION)
                .fromStatus(intervention.getFromStatus())
                .toStatus(intervention.getToStatus())
                .actorUserId(intervention.getRequestedBy())
                .targetUserId(intervention.getTargetUserId())
                .permissionCode(permissionCode)
                .permissionDecision(permissionDecision)
                .reason(intervention.getReason())
                .signoffEvidenceHash(intervention.getSignoffEvidenceHash())
                .integrityCheckResult(intervention.getIntegrityCheckResult())
                .integrityCheckSnapshotJson(intervention.getIntegrityCheckSnapshotJson())
                .eventSnapshotJson(eventSnapshotJson)
                .evidenceHash(evidenceHash)
                .occurredAt(occurredAt)
                .build());
    }

    private String buildInterventionHash(String action, String businessObjectType, String businessObjectId,
                                         String fromStatus, String toStatus, Long actorUserId, Long targetUserId,
                                         String reason, String authorizationBasis, String signoffEvidenceHash,
                                         String idempotencyKey, String integrityCheckResult,
                                         String integrityCheckSnapshotJson) {
        return DigestUtil.sha256Hex(String.join("|",
                action,
                StrUtil.nullToEmpty(businessObjectType),
                StrUtil.nullToEmpty(businessObjectId),
                StrUtil.nullToEmpty(fromStatus),
                StrUtil.nullToEmpty(toStatus),
                String.valueOf(actorUserId),
                String.valueOf(targetUserId),
                StrUtil.nullToEmpty(reason),
                StrUtil.nullToEmpty(authorizationBasis),
                signoffEvidenceHash,
                idempotencyKey,
                integrityCheckResult,
                integrityCheckSnapshotJson));
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
