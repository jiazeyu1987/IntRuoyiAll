package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationHandlingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_CLOSED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_CLOSE_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_NODE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_REVISION_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_SIGNATURE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_INITIATOR_REQUIRED;

@Service
public class MesProEdhrDeviationHandlingServiceImpl implements MesProEdhrDeviationHandlingService {

    public static final String SOURCE_TYPE = "EDHR_DEVIATION_HANDLING";
    public static final String NODE_PREPARER = "PREPARER";
    public static final String NODE_VERIFIER = "VERIFIER";
    public static final String NODE_DEPARTMENT_OWNER = "DEPARTMENT_OWNER";
    public static final String NODE_QA = "QA";
    public static final String NODE_QUALITY_OWNER = "QUALITY_OWNER";
    public static final String NODE_MANAGEMENT_REP = "MANAGEMENT_REP";
    public static final String HANDLING_CONCLUSION_CLOSED_LOOP = "CLOSED_LOOP";
    public static final String VERIFICATION_RESULT_PASS = "PASS";

    private static final List<String> COMMON_NODES = List.of(
            NODE_PREPARER, NODE_VERIFIER, NODE_DEPARTMENT_OWNER, NODE_QA, NODE_QUALITY_OWNER);
    private static final Set<String> ALL_NODES = Set.of(
            NODE_PREPARER, NODE_VERIFIER, NODE_DEPARTMENT_OWNER, NODE_QA, NODE_QUALITY_OWNER, NODE_MANAGEMENT_REP);

    private final MesProEdhrDeviationMapper deviationMapper;
    private final MesProEdhrDeviationHandlingMapper handlingMapper;
    private final ElectronicSignatureRecordMapper signatureRecordMapper;
    private final MesProBatchRecordExecutionSignatureService signatureService;
    private final MesProEdhrOperationAuditService operationAuditService;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProEdhrDeviationReviewTaskService reviewTaskService;
    @Resource
    private ElectronicSignatureQueryService signatureQueryService;
    @Resource
    private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService gxpAuditService;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private MesProEdhrOperationAuditEventMapper operationAuditEventMapper;

    public MesProEdhrDeviationHandlingServiceImpl(MesProEdhrDeviationMapper deviationMapper,
                                                  MesProEdhrDeviationHandlingMapper handlingMapper,
                                                  ElectronicSignatureRecordMapper signatureRecordMapper,
                                                  MesProBatchRecordExecutionSignatureService signatureService,
                                                  MesProEdhrOperationAuditService operationAuditService) {
        this.deviationMapper = deviationMapper;
        this.handlingMapper = handlingMapper;
        this.signatureRecordMapper = signatureRecordMapper;
        this.signatureService = signatureService;
        this.operationAuditService = operationAuditService;
    }

    @Override
    public MesProEdhrDeviationHandlingRespVO get(Long deviationId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProEdhrDeviationDO deviation = deviationMapper.selectByTenantAndId(tenantId, deviationId);
        if (deviation == null) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_NOT_EXISTS, deviationId);
        }
        MesProEdhrDeviationHandlingDO handling = handlingMapper
                .selectByTenantAndDeviationId(tenantId, deviationId);
        return handling == null ? null : toResp(deviation, handling);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDeviationHandlingRespVO save(Long actorUserId, Long deviationId,
                                                   MesProEdhrDeviationHandlingSaveReqVO reqVO) {
        requireActor(actorUserId);
        requirePermission("mes:pro-edhr-deviation:handle");
        gxpAuditService.acquireLedgerLock();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProEdhrDeviationDO deviation = deviationMapper.selectByTenantAndIdForUpdate(tenantId, deviationId);
        if (deviation == null) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_NOT_EXISTS, deviationId);
        }
        if (MesProEdhrDeviationServiceImpl.STATUS_CLOSED.equals(deviation.getStatus())) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_CLOSED, deviationId);
        }
        MesProEdhrDeviationHandlingDO current = handlingMapper
                .selectByTenantAndDeviationIdForUpdate(tenantId, deviationId);
        int expectedVersion = reqVO.getExpectedContentVersion();
        MesProEdhrDeviationHandlingDO next = toHandling(deviationId, tenantId, reqVO);
        if (current == null) {
            if (expectedVersion != 0) {
                throw exception(PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT, deviationId);
            }
            validateReviewerAssignments(deviation, next);
            next.setContentVersion(1);
            next.setContentHash(contentHash(next));
            if (handlingMapper.insert(next) != 1 || next.getId() == null) {
                throw exception(PRO_EDHR_DEVIATION_HANDLING_NOT_EXISTS, deviationId);
            }
            syncCorrectiveTask(deviation, next);
            dispatchReviewTasks(deviation, next);
            workTaskService.completeDeviationTask(deviationId,
                    MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE, "已提交偏差调查记录");
            return toResp(next);
        }
        if (!Objects.equals(current.getContentVersion(), expectedVersion)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT, deviationId);
        }
        if (!hasText(reqVO.getRevisionReason())) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_REVISION_REASON_REQUIRED, deviationId);
        }
        validateReviewerAssignments(deviation, next);
        String beforeContent = canonicalContent(current);
        next.setId(current.getId())
                .setContentVersion(current.getContentVersion() + 1)
                .setContentHash(contentHash(next));
        if (handlingMapper.updateContent(tenantId, deviationId, expectedVersion, next) != 1) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT, deviationId);
        }
        syncCorrectiveTask(deviation, next);
        reviewTaskService.cancelAll(deviationId, "偏差处理内容已修订，原版本签署任务失效");
        dispatchReviewTasks(deviation, next);
        workTaskService.completeDeviationTask(deviationId,
                MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE, "已提交偏差调查记录");
        Map<String, Object> revisionMetadata = new TreeMap<>();
        revisionMetadata.put("revisionReason", reqVO.getRevisionReason().trim());
        revisionMetadata.put("beforeContent", beforeContent);
        revisionMetadata.put("afterContent", canonicalContent(next));
        revisionMetadata.put("beforeContentVersion", current.getContentVersion());
        revisionMetadata.put("afterContentVersion", next.getContentVersion());
        revisionMetadata.put("beforeContentHash", current.getContentHash());
        revisionMetadata.put("afterContentHash", next.getContentHash());
        operationAuditService.recordInCallerTransaction(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR_DEVIATION_HANDLING_UPDATE:" + current.getId() + ":" + next.getContentVersion())
                .setObjectType("EDHR_DEVIATION_HANDLING")
                .setObjectId(String.valueOf(current.getId()))
                .setBatchExecutionId(deviation.getBatchExecutionId())
                .setRecordCategory("EDHR_DEVIATION_HANDLING")
                .setOperationType("UPDATE")
                .setActionName("修订偏差处理记录")
                .setActorUserId(actorUserId)
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(current.getContentHash())
                .setAfterSummaryHash(next.getContentHash())
                .setMetadataJson(JSON.toJSONString(revisionMetadata)));
        return toResp(next);
    }

    private void syncCorrectiveTask(MesProEdhrDeviationDO deviation, MesProEdhrDeviationHandlingDO handling) {
        if (handling.getCompletedAt() != null) {
            workTaskService.completeDeviationTask(deviation.getId(),
                    MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_CORRECT, "已提交纠正措施完成记录");
        } else {
            workTaskService.assignDeviationTask(deviation, MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_CORRECT,
                    handling.getCorrectiveOwnerId(), "执行偏差纠正措施：" + deviation.getDeviationCode());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sign(Long actorUserId, Long deviationId, String node, String password, String comment,
              Integer expectedContentVersion, String expectedContentHash) {
        requireActor(actorUserId);
        requirePermission(permissionByNode(node));
        if (!ALL_NODES.contains(node)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_NODE_INVALID, node);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        gxpAuditService.acquireLedgerLock();
        MesProEdhrDeviationDO deviation = deviationMapper.selectByTenantAndIdForUpdate(tenantId, deviationId);
        if (deviation == null) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_NOT_EXISTS, deviationId);
        }
        if (MesProEdhrDeviationServiceImpl.STATUS_CLOSED.equals(deviation.getStatus())) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_CLOSED, deviationId);
        }
        MesProEdhrDeviationHandlingDO handling = handlingMapper
                .selectByTenantAndDeviationIdForUpdate(tenantId, deviationId);
        if (handling == null || handling.getContentVersion() == null || handling.getContentHash() == null) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_NOT_EXISTS, deviationId);
        }
        if (expectedContentVersion == null || !hasText(expectedContentHash)
                || !Objects.equals(handling.getContentVersion(), expectedContentVersion)
                || !Objects.equals(handling.getContentHash(), expectedContentHash)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT, deviationId);
        }
        if (!requiredNodes(deviation).contains(node) || !readyForReview(handling)
                || !Objects.equals(reviewerAssignments(handling).get(node), actorUserId)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, node);
        }
        var signatures = effectiveSignatures(deviation, handling, requiredNodes(deviation));
        if (signatures.containsKey(node)) return signatures.get(node);
        var task = reviewTaskService.require(deviationId, handling, node, actorUserId);
        Long signatureId = signatureService.recordDeviationHandlingSignature(actorUserId, deviationId, handling.getId(),
                deviation.getDeviationCode(), node, handling.getContentVersion(), handling.getContentHash(),
                password, comment);
        if (signatureId == null || signatureId <= 0) {
            throw new IllegalStateException("偏差节点签名未返回有效证据编号");
        }
        reviewTaskService.completeSigned(task, signatureId, comment);
        signatures.put(node, signatureId);
        if (signatures.keySet().containsAll(requiredNodes(deviation))) {
            reviewTaskService.assign(deviation, handling, "CLOSE", reviewerAssignments(handling).get(NODE_QA));
        }
        return signatureId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeNormally(Long actorUserId, Long deviationId, Integer expectedContentVersion, String expectedContentHash) {
        requireActor(actorUserId);
        requireAnyPermission("mes:pro-edhr-deviation:qa-close", "mes:pro-edhr-deviation:quality-approve");
        gxpAuditService.acquireLedgerLock();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProEdhrDeviationDO deviation = deviationMapper.selectByTenantAndIdForUpdate(tenantId, deviationId);
        if (deviation == null) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_NOT_EXISTS, deviationId);
        }
        if (MesProEdhrDeviationServiceImpl.STATUS_CLOSED.equals(deviation.getStatus())) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_CLOSED, deviationId);
        }
        MesProEdhrDeviationHandlingDO handling = handlingMapper
                .selectByTenantAndDeviationIdForUpdate(tenantId, deviationId);
        if (handling == null) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_NOT_EXISTS, deviationId);
        }
        if (expectedContentVersion == null || !hasText(expectedContentHash)
                || !Objects.equals(handling.getContentVersion(), expectedContentVersion)
                || !Objects.equals(handling.getContentHash(), expectedContentHash)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT, deviationId);
        }
        if (!readyForReview(handling)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_REQUIRED, deviationId);
        }
        if (!Objects.equals(reviewerAssignments(handling).get(NODE_QA), actorUserId)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, "CLOSE");
        }
        List<String> requiredNodes = MesProEdhrDeviationServiceImpl.LEVEL_CRITICAL.equals(deviation.getLevel())
                ? List.of(NODE_PREPARER, NODE_VERIFIER, NODE_DEPARTMENT_OWNER, NODE_QA,
                NODE_QUALITY_OWNER, NODE_MANAGEMENT_REP) : COMMON_NODES;
        Map<String, Long> signatures = effectiveSignatures(deviation, handling, requiredNodes);
        for (String node : requiredNodes) {
            if (!signatures.containsKey(node)) {
                throw exception(PRO_EDHR_DEVIATION_HANDLING_SIGNATURE_REQUIRED, node, deviationId);
            }
        }
        if (deviation.getInitiatorSignatureId() == null) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_SIGNATURE_REQUIRED, "INITIATOR", deviationId);
        }
        var closeTask = reviewTaskService.require(deviationId, handling, "CLOSE", actorUserId);
        LocalDateTime closedAt = LocalDateTime.now(MesProEdhrDeviationNumberGenerator.BUSINESS_ZONE);
        signatures.put("INITIATOR", deviation.getInitiatorSignatureId());
        String closeSignatures = JSON.toJSONString(new TreeMap<>(signatures));
        if (deviationMapper.closeNormally(tenantId, deviationId, "NORMAL_COMPLETED", closedAt,
                actorUserId, expectedContentVersion, expectedContentHash, closeSignatures) != 1) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_CLOSE_FAILED, deviationId);
        }
        workTaskService.completeDeviationTask(deviationId, MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE,
                "偏差已正式关闭");
        workTaskService.completeDeviationTask(deviationId, MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_CORRECT,
                "偏差已正式关闭");
        reviewTaskService.complete(closeTask);
        reviewTaskService.cancelAll(deviationId, "偏差已正式关闭");
        operationAuditService.recordInCallerTransaction(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR_DEVIATION_CLOSE:" + deviationId + ":" + expectedContentVersion)
                .setObjectType("EDHR_DEVIATION").setObjectId(deviationId.toString())
                .setBatchExecutionId(deviation.getBatchExecutionId())
                .setRecordCategory("EDHR_DEVIATION_HANDLING").setOperationType("CLOSE")
                .setActionName("按已审核版本关闭偏差").setActorUserId(actorUserId)
                .setPermissionDecision("ALLOW").setResultStatus("SUCCESS")
                .setBeforeSummaryHash(expectedContentHash).setAfterSummaryHash(expectedContentHash)
                .setOccurredAt(closedAt).setMetadataJson(JSON.toJSONString(Map.of(
                        "closedBy", actorUserId, "contentVersion", expectedContentVersion,
                        "contentHash", expectedContentHash, "signatureIds", signatures))));
    }

    String contentHash(MesProEdhrDeviationHandlingDO handling) {
        return DigestUtil.sha256Hex(canonicalContent(handling));
    }

    private List<String> requiredNodes(MesProEdhrDeviationDO deviation) {
        return MesProEdhrDeviationServiceImpl.LEVEL_CRITICAL.equals(deviation.getLevel())
                ? List.of(NODE_PREPARER, NODE_VERIFIER, NODE_DEPARTMENT_OWNER, NODE_QA,
                NODE_QUALITY_OWNER, NODE_MANAGEMENT_REP) : COMMON_NODES;
    }

    private void validateReviewerAssignments(MesProEdhrDeviationDO deviation,
                                               MesProEdhrDeviationHandlingDO handling) {
        var assignments = reviewerAssignments(handling);
        var required = requiredNodes(deviation);
        if (!required.containsAll(assignments.keySet())) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_NODE_INVALID, assignments.keySet());
        }
        if (isComplete(handling) && !assignments.keySet().containsAll(required)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_REQUIRED, deviation.getId());
        }
        assignments.forEach(reviewTaskService::validateAssignee);
    }

    private void dispatchReviewTasks(MesProEdhrDeviationDO deviation, MesProEdhrDeviationHandlingDO handling) {
        if (!readyForReview(handling)) return;
        var assignments = reviewerAssignments(handling);
        for (String node : requiredNodes(deviation)) {
            reviewTaskService.assign(deviation, handling, node, assignments.get(node));
        }
    }

    private String canonicalContent(MesProEdhrDeviationHandlingDO handling) {
        Map<String, Object> content = new TreeMap<>();
        content.put("deviationId", handling.getDeviationId());
        content.put("investigationStartedAt", handling.getInvestigationStartedAt());
        content.put("plannedCompletedAt", handling.getPlannedCompletedAt());
        content.put("completedAt", handling.getCompletedAt());
        content.put("investigationMembersJson", handling.getInvestigationMembersJson());
        content.put("rootCauseAnalysis", handling.getRootCauseAnalysis());
        content.put("impactScope", handling.getImpactScope());
        content.put("riskAssessment", handling.getRiskAssessment());
        content.put("productDisposition", handling.getProductDisposition());
        content.put("nonconformanceReviewCode", handling.getNonconformanceReviewCode());
        content.put("correctiveOwnerId", handling.getCorrectiveOwnerId());
        content.put("correctiveOwnerName", handling.getCorrectiveOwnerName());
        content.put("correctiveDueAt", handling.getCorrectiveDueAt());
        content.put("capaRequired", handling.getCapaRequired());
        content.put("capaCode", handling.getCapaCode());
        content.put("capaAttachmentsJson", handling.getCapaAttachmentsJson());
        content.put("handlingConclusion", handling.getHandlingConclusion());
        content.put("verificationResult", handling.getVerificationResult());
        content.put("verificationContent", handling.getVerificationContent());
        content.put("reviewerAssignments", reviewerAssignments(handling));
        return JSON.toJSONString(content);
    }

    private boolean isComplete(MesProEdhrDeviationHandlingDO handling) {
        return hasText(handling.getRootCauseAnalysis())
                && hasText(handling.getImpactScope())
                && hasText(handling.getRiskAssessment())
                && hasText(handling.getProductDisposition())
                && HANDLING_CONCLUSION_CLOSED_LOOP.equals(handling.getHandlingConclusion())
                && VERIFICATION_RESULT_PASS.equals(handling.getVerificationResult())
                && hasText(handling.getVerificationContent());
    }

    private boolean readyForReview(MesProEdhrDeviationHandlingDO handling) {
        return isComplete(handling)
                && (handling.getCorrectiveOwnerId() == null || handling.getCompletedAt() != null);
    }

    private Map<String, Long> effectiveSignatures(MesProEdhrDeviationDO deviation,
                                                  MesProEdhrDeviationHandlingDO handling,
                                                  List<String> nodes) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String node : nodes) {
            Long designatedActor = reviewerAssignments(handling).get(node);
            String actionType = actionByNode(node);
            String intent = "偏差 " + deviation.getDeviationCode() + " 处理节点 " + node;
            String subjectId = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                    0L, actionType, null, null, null, null, null, null, null,
                    SOURCE_TYPE, handling.getId(), intent, node, (long) handling.getContentVersion(),
                    handling.getContentHash(), handling.getContentHash(), null);
            ElectronicSignatureRecordDO signature = signatureRecordMapper.selectOne(
                    new QueryWrapper<ElectronicSignatureRecordDO>()
                    .eq("tenant_id", TenantContextHolder.getRequiredTenantId())
                    .eq("module_code", MesBatchRecordSignatureSubjectAdapter.MODULE_CODE)
                    .eq("action_code", actionType)
                    .eq("subject_type", MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE)
                    .eq("subject_id", subjectId)
                    .eq("subject_version", MesBatchRecordSignatureSubjectAdapter.subjectVersion(subjectId))
                    .eq("verification_status", "VALID")
                    .eq(designatedActor != null, "actor_id", designatedActor)
                    .orderByDesc("id")
                    .last("LIMIT 1"));
            if (signature == null || signature.getId() == null
                    || (designatedActor != null && !Objects.equals(designatedActor, signature.getActorId()))
                    || !Objects.equals(signature.getSubjectVersion(),
                    MesBatchRecordSignatureSubjectAdapter.subjectVersion(subjectId))) {
                continue;
            }
            result.put(node, signature.getId());
        }
        return result;
    }

    private String actionByNode(String node) {
        return switch (node) {
            case NODE_PREPARER -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_PREPARE;
            case NODE_VERIFIER -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_VERIFY;
            case NODE_DEPARTMENT_OWNER -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_DEPARTMENT;
            case NODE_QA -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_QA;
            case NODE_QUALITY_OWNER -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_QUALITY;
            case NODE_MANAGEMENT_REP -> MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_MANAGEMENT;
            default -> throw new IllegalArgumentException(node);
        };
    }

    private MesProEdhrDeviationHandlingDO toHandling(Long deviationId, Long tenantId,
                                                      MesProEdhrDeviationHandlingSaveReqVO reqVO) {
        return new MesProEdhrDeviationHandlingDO()
                .setTenantId(tenantId)
                .setDeviationId(deviationId)
                .setInvestigationStartedAt(reqVO.getInvestigationStartedAt())
                .setPlannedCompletedAt(reqVO.getPlannedCompletedAt())
                .setCompletedAt(reqVO.getCompletedAt())
                .setInvestigationMembersJson(trimToNull(reqVO.getInvestigationMembersJson()))
                .setRootCauseAnalysis(trimToNull(reqVO.getRootCauseAnalysis()))
                .setImpactScope(trimToNull(reqVO.getImpactScope()))
                .setRiskAssessment(trimToNull(reqVO.getRiskAssessment()))
                .setProductDisposition(trimToNull(reqVO.getProductDisposition()))
                .setNonconformanceReviewCode(trimToNull(reqVO.getNonconformanceReviewCode()))
                .setCorrectiveOwnerId(reqVO.getCorrectiveOwnerId())
                .setCorrectiveOwnerName(trimToNull(reqVO.getCorrectiveOwnerName()))
                .setCorrectiveDueAt(reqVO.getCorrectiveDueAt())
                .setCapaRequired(reqVO.getCapaRequired())
                .setCapaCode(trimToNull(reqVO.getCapaCode()))
                .setCapaAttachmentsJson(trimToNull(reqVO.getCapaAttachmentsJson()))
                .setHandlingConclusion(trimToNull(reqVO.getHandlingConclusion()))
                .setVerificationResult(trimToNull(reqVO.getVerificationResult()))
                .setVerificationContent(trimToNull(reqVO.getVerificationContent()))
                .setReviewerAssignmentsJson(JSON.toJSONString(new TreeMap<>(
                        reqVO.getReviewerAssignments() == null ? Map.of() : reqVO.getReviewerAssignments())));
    }

    private MesProEdhrDeviationHandlingRespVO toResp(MesProEdhrDeviationHandlingDO handling) {
        return new MesProEdhrDeviationHandlingRespVO()
                .setId(handling.getId())
                .setDeviationId(handling.getDeviationId())
                .setInvestigationStartedAt(handling.getInvestigationStartedAt())
                .setPlannedCompletedAt(handling.getPlannedCompletedAt())
                .setCompletedAt(handling.getCompletedAt())
                .setInvestigationMembersJson(handling.getInvestigationMembersJson())
                .setRootCauseAnalysis(handling.getRootCauseAnalysis())
                .setImpactScope(handling.getImpactScope())
                .setRiskAssessment(handling.getRiskAssessment())
                .setProductDisposition(handling.getProductDisposition())
                .setNonconformanceReviewCode(handling.getNonconformanceReviewCode())
                .setCorrectiveOwnerId(handling.getCorrectiveOwnerId())
                .setCorrectiveOwnerName(handling.getCorrectiveOwnerName())
                .setCorrectiveDueAt(handling.getCorrectiveDueAt())
                .setCapaRequired(handling.getCapaRequired())
                .setCapaCode(handling.getCapaCode())
                .setCapaAttachmentsJson(handling.getCapaAttachmentsJson())
                .setHandlingConclusion(handling.getHandlingConclusion())
                .setVerificationResult(handling.getVerificationResult())
                .setVerificationContent(handling.getVerificationContent())
                .setContentVersion(handling.getContentVersion())
                .setContentHash(handling.getContentHash())
                .setReviewReady(readyForReview(handling))
                .setReviewerAssignments(reviewerAssignments(handling));
    }

    private Map<String, Long> reviewerAssignments(MesProEdhrDeviationHandlingDO handling) {
        if (!hasText(handling.getReviewerAssignmentsJson())) {
            return Map.of();
        }
        return JSON.parseObject(handling.getReviewerAssignmentsJson(),
                new com.alibaba.fastjson.TypeReference<TreeMap<String, Long>>() { });
    }

    private MesProEdhrDeviationHandlingRespVO toResp(MesProEdhrDeviationDO deviation,
                                                      MesProEdhrDeviationHandlingDO handling) {
        List<String> nodes = MesProEdhrDeviationServiceImpl.LEVEL_CRITICAL.equals(deviation.getLevel())
                ? List.of(NODE_PREPARER, NODE_VERIFIER, NODE_DEPARTMENT_OWNER, NODE_QA,
                NODE_QUALITY_OWNER, NODE_MANAGEMENT_REP) : COMMON_NODES;
        MesProEdhrDeviationHandlingRespVO response = toResp(handling)
                .setEffectiveSignatureIds(effectiveSignatures(deviation, handling, nodes))
                .setRevisionHistory(readRevisionHistory(handling));
        if (signatureQueryService != null) {
            response.setSignatureEvidence(nodes.stream().map(node -> signatureEvidence(deviation, handling, node))
                    .filter(Objects::nonNull).toList());
            response.setSignatureHistory(signatureHistory(deviation, handling, nodes, response.getRevisionHistory()));
        }
        return response;
    }

    private MesProEdhrDeviationHandlingRespVO.SignatureEvidence signatureEvidence(
            MesProEdhrDeviationDO deviation, MesProEdhrDeviationHandlingDO handling, String node) {
        String subjectId = handlingSubjectId(deviation, handling, node);
        List<ElectronicSignatureEvidenceDTO> records = signatureQueryService.listBySubject(
                MesBatchRecordSignatureSubjectAdapter.MODULE_CODE,
                MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE, subjectId);
        return records.stream()
                .filter(record -> Objects.equals(record.subjectVersion(),
                        MesBatchRecordSignatureSubjectAdapter.subjectVersion(subjectId)))
                .findFirst()
                .map(record -> new MesProEdhrDeviationHandlingRespVO.SignatureEvidence()
                        .setId(record.id()).setNode(node).setActorId(record.actorId())
                        .setActorDisplayName(record.actorDisplayName())
                        .setMeaningLabel(record.meaningLabel()).setSignedAt(record.signedAt())
                        .setTimeZone(record.timeZone())
                        .setTimeEvidenceId(record.timeEvidenceId())
                        .setVerificationStatus(record.verificationStatus()).setContentHash(record.contentHash())
                        .setEvidenceHash(record.evidenceHash())
                        .setPolicyVersion(record.policyVersion()).setSubjectVersion(record.subjectVersion())
                        .setValidityStatus("CURRENT_VALID"))
                .orElse(null);
    }

    private MesProEdhrDeviationHandlingRespVO.SignatureEvidence signatureEvidence(
            MesProEdhrDeviationDO deviation, MesProEdhrDeviationHandlingDO handling, String node,
            Integer contentVersion, String contentHash, String validityStatus) {
        String subjectId = handlingSubjectId(deviation, handling, node, contentVersion, contentHash);
        List<ElectronicSignatureEvidenceDTO> records = signatureQueryService.listBySubject(
                MesBatchRecordSignatureSubjectAdapter.MODULE_CODE,
                MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE, subjectId);
        return records.stream()
                .filter(record -> Objects.equals(record.subjectVersion(),
                        MesBatchRecordSignatureSubjectAdapter.subjectVersion(subjectId)))
                .findFirst()
                .map(record -> new MesProEdhrDeviationHandlingRespVO.SignatureEvidence()
                        .setId(record.id()).setNode(node).setActorId(record.actorId())
                        .setActorDisplayName(record.actorDisplayName())
                        .setMeaningLabel(record.meaningLabel()).setSignedAt(record.signedAt())
                        .setTimeZone(record.timeZone())
                        .setTimeEvidenceId(record.timeEvidenceId())
                        .setVerificationStatus(record.verificationStatus()).setContentHash(record.contentHash())
                        .setEvidenceHash(record.evidenceHash())
                        .setPolicyVersion(record.policyVersion()).setSubjectVersion(record.subjectVersion())
                        .setValidityStatus(validityStatus))
                .orElse(null);
    }

    private List<MesProEdhrDeviationHandlingRespVO.SignatureEvidence> signatureHistory(
            MesProEdhrDeviationDO deviation, MesProEdhrDeviationHandlingDO handling, List<String> nodes,
            List<MesProEdhrDeviationHandlingRespVO.RevisionHistory> revisions) {
        LinkedHashSet<String> seenVersions = new LinkedHashSet<>();
        List<SignatureVersion> versions = new ArrayList<>();
        addSignatureVersion(versions, seenVersions, handling.getContentVersion(), handling.getContentHash(), true);
        for (MesProEdhrDeviationHandlingRespVO.RevisionHistory revision : revisions) {
            addSignatureVersion(versions, seenVersions, revision.getBeforeContentVersion(),
                    revision.getBeforeContentHash(), false);
        }
        List<MesProEdhrDeviationHandlingRespVO.SignatureEvidence> result = new ArrayList<>();
        for (SignatureVersion version : versions) {
            for (String node : nodes) {
                MesProEdhrDeviationHandlingRespVO.SignatureEvidence evidence = signatureEvidence(
                        deviation, handling, node, version.contentVersion(), version.contentHash(),
                        version.current() ? "CURRENT_VALID" : "SUPERSEDED_INVALID");
                if (evidence != null) {
                    result.add(evidence);
                }
            }
        }
        return result;
    }

    private void addSignatureVersion(List<SignatureVersion> versions, LinkedHashSet<String> seenVersions,
                                     Integer contentVersion, String contentHash, boolean current) {
        if (contentVersion == null || contentVersion <= 0 || contentHash == null || contentHash.isBlank()) {
            return;
        }
        String key = contentVersion + "|" + contentHash;
        if (seenVersions.add(key)) {
            versions.add(new SignatureVersion(contentVersion, contentHash, current));
        }
    }

    private List<MesProEdhrDeviationHandlingRespVO.RevisionHistory> readRevisionHistory(
            MesProEdhrDeviationHandlingDO handling) {
        if (operationAuditEventMapper == null) {
            return List.of();
        }
        List<MesProEdhrOperationAuditEventDO> events = operationAuditEventMapper.selectRevisionListByObject(
                SOURCE_TYPE, String.valueOf(handling.getId()));
        List<MesProEdhrDeviationHandlingRespVO.RevisionHistory> result = new ArrayList<>();
        for (MesProEdhrOperationAuditEventDO event : events) {
            if (event.getMetadataJson() == null || event.getMetadataJson().isBlank()) {
                throw new IllegalStateException("EDHR_DEVIATION_REVISION_METADATA_MISSING:" + event.getId());
            }
            var metadata = JSON.parseObject(event.getMetadataJson());
            result.add(new MesProEdhrDeviationHandlingRespVO.RevisionHistory()
                    .setAuditId(event.getId())
                    .setHandlingId(handling.getId())
                    .setRevisionReason(metadata.getString("revisionReason"))
                    .setBeforeContent(metadata.getString("beforeContent"))
                    .setAfterContent(metadata.getString("afterContent"))
                    .setActorUserId(event.getActorUserId())
                    .setActorUsername(event.getActorUsername())
                    .setOccurredAt(event.getOccurredAt())
                    .setBeforeSummaryHash(event.getBeforeSummaryHash())
                    .setAfterSummaryHash(event.getAfterSummaryHash())
                    .setPreviousAuditHash(event.getPreviousAuditHash())
                    .setAuditHash(event.getAuditHash())
                    .setBeforeContentVersion(metadata.getInteger("beforeContentVersion"))
                    .setAfterContentVersion(metadata.getInteger("afterContentVersion"))
                    .setBeforeContentHash(metadata.getString("beforeContentHash"))
                    .setAfterContentHash(metadata.getString("afterContentHash")));
        }
        return result;
    }

    private String handlingSubjectId(MesProEdhrDeviationDO deviation,
                                     MesProEdhrDeviationHandlingDO handling, String node) {
        return handlingSubjectId(deviation, handling, node, handling.getContentVersion(), handling.getContentHash());
    }

    private String handlingSubjectId(MesProEdhrDeviationDO deviation,
                                     MesProEdhrDeviationHandlingDO handling, String node,
                                     Integer contentVersion, String contentHash) {
        String actionType = actionByNode(node);
        String intent = "偏差 " + deviation.getDeviationCode() + " 处理节点 " + node;
        return MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, actionType, null, null, null, null,
                null, null, null, SOURCE_TYPE, handling.getId(), intent, node,
                (long) contentVersion, contentHash, contentHash, null);
    }

    private record SignatureVersion(Integer contentVersion, String contentHash, boolean current) {
    }

    private void requireActor(Long actorUserId) {
        if (actorUserId == null || actorUserId <= 0) {
            throw exception(PRO_EDHR_DEVIATION_INITIATOR_REQUIRED);
        }
    }

    private void requirePermission(String permission) {
        if (securityFrameworkService == null || !securityFrameworkService.hasPermission(permission)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, permission);
        }
    }

    private void requireAnyPermission(String... permissions) {
        if (securityFrameworkService == null || !securityFrameworkService.hasAnyPermissions(permissions)) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, String.join(",", permissions));
        }
    }

    private String permissionByNode(String node) {
        return switch (node) {
            case NODE_PREPARER -> "mes:pro-edhr-deviation:handle";
            case NODE_VERIFIER -> "mes:pro-edhr-deviation:verify";
            case NODE_DEPARTMENT_OWNER -> "mes:pro-edhr-deviation:department-confirm";
            case NODE_QA -> "mes:pro-edhr-deviation:qa-close";
            case NODE_QUALITY_OWNER -> "mes:pro-edhr-deviation:quality-approve";
            case NODE_MANAGEMENT_REP -> "mes:pro-edhr-deviation:critical-management-approve";
            default -> throw exception(PRO_EDHR_DEVIATION_HANDLING_NODE_INVALID, node);
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
