package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesTeamLeaderReviewSignatureContext;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import jakarta.annotation.Resource;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_PRODUCTION_REVIEW_ALLOCATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_PQC_LEADER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_REJECT_REMARK_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS;

@Service
@Validated
public class MesTeamLeaderSubmissionReviewServiceImpl implements MesTeamLeaderSubmissionReviewService {

    private static final Set<String> VALID_REVIEW_STATUSES = Set.of(
            MesProcessPoolSubmissionReviewDO.STATUS_APPROVED,
            MesProcessPoolSubmissionReviewDO.STATUS_REJECTED);

    private final MesTeamLeaderScopeService scopeService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesPqcProcessInspectionAggregationService processInspectionAggregationService;

    @Resource
    private MesReportAllocationCommandService reportAllocationCommandService;

    @Resource
    private MesProBatchRecordExecutionSignatureService signatureService;

    @Resource
    private MesProProcessPoolEventRevisionMapper revisionMapper;

    @Resource
    private GxpAuditService gxpAuditService;

    @Resource
    private MesReleaseAffectedStateCollector affectedStateCollector;

    @Resource
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    public MesTeamLeaderSubmissionReviewServiceImpl(MesTeamLeaderScopeService scopeService,
                                                    MesProProcessPoolEventMapper eventMapper,
                                                    MesProcessPoolSubmissionReviewMapper reviewMapper,
                                                    MesPqcProcessInspectionAggregationService processInspectionAggregationService) {
        this.scopeService = scopeService;
        this.eventMapper = eventMapper;
        this.reviewMapper = reviewMapper;
        this.processInspectionAggregationService = processInspectionAggregationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reviewSubmission(MesTeamLeaderSubmissionReviewReqBO reqBO) {
        validateReq(reqBO);
        gxpAuditService.acquireLedgerLock();
        if (MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION.equals(reqBO.getLeaderType())
                && MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(reqBO.getReviewStatus())) {
            return reportAllocationCommandService.rejectProductionSubmission(
                    reqBO.getEventId(), reqBO.getLeaderUserId(), reqBO.getReviewRemark(),
                    reqBO.getSignaturePassword());
        }
        MesProProcessPoolEventDO event = eventMapper.selectByIdForUpdate(reqBO.getEventId());
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, reqBO.getEventId());
        }
        List<MesProProcessPoolEventDO> members = resolveReviewMembers(event);
        // Validate the whole group before creating any signature, review or aggregate.
        List<ReviewContext> contexts = new ArrayList<>();
        for (MesProProcessPoolEventDO member : members) {
            contexts.add(prepareReview(reqBO, member));
        }
        Long requestedReviewId = null;
        for (ReviewContext context : contexts) {
            Long reviewId = reviewMember(reqBO, context);
            if (Objects.equals(context.event().getId(), reqBO.getEventId())) {
                requestedReviewId = reviewId;
            }
        }
        return requestedReviewId;
    }

    private List<MesProProcessPoolEventDO> resolveReviewMembers(MesProProcessPoolEventDO event) {
        String groupId = submissionGroupId(event);
        if (groupId == null) {
            return List.of(event);
        }
        if (event.getTenantId() == null || event.getWorkOrderId() == null
                || event.getRouteId() == null || event.getQaProcessId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcReview.submissionGroup.context");
        }
        List<MesProProcessPoolEventDO> members = eventMapper.selectList(
                new LambdaQueryWrapperX<MesProProcessPoolEventDO>()
                        .eq(MesProProcessPoolEventDO::getTenantId, event.getTenantId())
                        .eq(MesProProcessPoolEventDO::getEventType, MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .apply("JSON_UNQUOTE(JSON_EXTRACT(raw_payload, '$.pqcSubmissionGroupId')) = {0}", groupId)
                        .orderByAsc(MesProProcessPoolEventDO::getId).last("FOR UPDATE"));
        if (members.isEmpty() || members.stream().noneMatch(member -> Objects.equals(event.getId(), member.getId()))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcReview.submissionGroup.members");
        }
        Long activeOrderId = resolvePqcInspectionTask(event).getActiveOrderId();
        for (MesProProcessPoolEventDO member : members) {
            if (!Objects.equals(event.getTenantId(), member.getTenantId())
                    || !Objects.equals(groupId, submissionGroupId(member))
                    || !Objects.equals(event.getWorkOrderId(), member.getWorkOrderId())
                    || !Objects.equals(event.getRouteId(), member.getRouteId())
                    || !Objects.equals(event.getQaProcessId(), member.getQaProcessId())
                    || !Objects.equals(event.getActualEmployeeId(), member.getActualEmployeeId())
                    || !Objects.equals(event.getFeedbackSourceType(), member.getFeedbackSourceType())
                    || !Objects.equals(activeOrderId, resolvePqcInspectionTask(member).getActiveOrderId())) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcReview.submissionGroup.memberContext");
            }
        }
        return members;
    }

    private String submissionGroupId(MesProProcessPoolEventDO event) {
        if (!MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())
                || StrUtil.isBlank(event.getRawPayload())) {
            return null;
        }
        var group = JsonUtils.parseTree(event.getRawPayload()).path("pqcSubmissionGroupId");
        if (group.isMissingNode() || group.isNull() || (group.isTextual() && StrUtil.isBlank(group.textValue()))) {
            return null;
        }
        if (!group.isTextual()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcReview.submissionGroup.id");
        }
        return group.textValue();
    }

    private ReviewContext prepareReview(MesTeamLeaderSubmissionReviewReqBO reqBO, MesProProcessPoolEventDO event) {
        if (MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())
                && !MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC.equals(reqBO.getLeaderType())) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_PQC_LEADER_REQUIRED,
                    reqBO.getEventId(), reqBO.getLeaderType());
        }
        scopeService.assertCanAccessEmployee(reqBO.getLeaderUserId(), reqBO.getLeaderType(),
                event.getActualEmployeeId());
        MesProcessPoolSubmissionReviewDO existingReview =
                reviewMapper.selectLatestByEventIdForUpdate(event.getId());
        MesProProcessPoolEventRevisionDO correction = findSignedCorrection(event, existingReview);
        if (existingReview != null && correction == null) {
            boolean alreadyApprovedGroupMember = !Objects.equals(reqBO.getEventId(), event.getId())
                    && MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(existingReview.getReviewStatus());
            if (alreadyApprovedGroupMember || isIdempotentReplay(reqBO, existingReview)) {
                return new ReviewContext(event, existingReview, null);
            }
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS,
                    event.getId(), existingReview.getReviewStatus());
        }
        if (MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(reqBO.getReviewStatus())
                && MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_PRODUCTION_REVIEW_ALLOCATION_REQUIRED, reqBO.getEventId());
        }
        return new ReviewContext(event, existingReview, correction);
    }

    private Long reviewMember(MesTeamLeaderSubmissionReviewReqBO reqBO, ReviewContext context) {
        MesProProcessPoolEventDO event = context.event();
        if (context.existingReview() != null && context.correction() == null) {
            return context.existingReview().getId();
        }
        String affectedRowsBefore = JsonUtils.toJsonString(affectedStateCollector.capturePqcSubmission(
                event.getId(), event.getFeedbackSourceId()));
        ReviewSignaturePayload reviewSignature = recordReviewSignature(reqBO, event, context.correction());
        MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
                .eventId(event.getId())
                .leaderUserId(reqBO.getLeaderUserId())
                .leaderType(reqBO.getLeaderType())
                .reviewStatus(reqBO.getReviewStatus())
                .reviewRemark(reqBO.getReviewRemark())
                .reviewedAt(LocalDateTime.now())
                .reviewSignatureId(reviewSignature.reviewSignatureId())
                .reviewSignatureUserId(reviewSignature.reviewSignatureUserId())
                .reviewSignatureSnapshotJson(reviewSignature.reviewSignatureSnapshotJson())
                .build();
        reviewMapper.insert(review);
        if (MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(reqBO.getReviewStatus())
                && MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())) {
            processInspectionAggregationService.aggregateApprovedPqcSubmission(event.getId(), review.getId());
        }
        appendPqcReviewGxpAudit(event, review, affectedRowsBefore);
        return review.getId();
    }

    private record ReviewContext(MesProProcessPoolEventDO event,
                                 MesProcessPoolSubmissionReviewDO existingReview,
                                 MesProProcessPoolEventRevisionDO correction) { }

    private MesProProcessPoolEventRevisionDO findSignedCorrection(
            MesProProcessPoolEventDO event, MesProcessPoolSubmissionReviewDO previous) {
        if (previous == null || !MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(previous.getReviewStatus())
                || !MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())) {
            return null;
        }
        var revisions = revisionMapper.selectListByEventId(event.getId());
        if (revisions.isEmpty()) {
            return null;
        }
        MesProProcessPoolEventRevisionDO revision = revisions.get(0);
        if (!Objects.equals(event.getId(), revision.getEventId())
                || !Objects.equals(event.getTenantId(), revision.getTenantId())
                || !MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE.equals(revision.getRevisionStatus())
                || revision.getId() == null || revision.getRevisionSignatureId() == null
                || revision.getRevisionSignatureId() <= 0 || revision.getRevisionSignatureUserId() == null
                || !Objects.equals(revision.getRevisionSignatureUserId(), revision.getModifiedByUserId())
                || StrUtil.isBlank(revision.getRevisionSignatureSnapshot())
                || StrUtil.isBlank(revision.getAfterPayload())) {
            return null;
        }
        var payload = JsonUtils.parseTree(revision.getAfterPayload());
        if (!payload.isObject() || !payload.path("supersededReviewId").isIntegralNumber()
                || !Objects.equals(previous.getId(), payload.path("supersededReviewId").longValue())
                || !Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload()))) {
            return null;
        }
        var signature = JsonUtils.parseObject(revision.getRevisionSignatureSnapshot(),
                MesProBatchRecordExecutionFieldAuditSignatureResult.class);
        return signature != null && signature.getSignedAt() != null
                && Objects.equals(signature.getSignatureId(), revision.getRevisionSignatureId())
                && Objects.equals(signature.getActorId(), revision.getRevisionSignatureUserId()) ? revision : null;
    }

    private void appendPqcReviewGxpAudit(MesProProcessPoolEventDO event,
                                         MesProcessPoolSubmissionReviewDO review,
                                         String affectedRowsBefore) {
        if (review.getId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "submissionReview.reviewId");
        }
        boolean approved = MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(review.getReviewStatus());
        String operationId = approved ? "mes.pqc.review.approve" : "mes.pqc.review.reject";
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("eventId", event.getId());
        before.put("state", "ABSENT");
        before.put("affectedRows", JsonUtils.parseObject(affectedRowsBefore, Map.class));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("event", pqcReviewEventSnapshot(event));
        after.put("review", pqcReviewSnapshot(review));
        after.put("aggregationState", approved ? "AGGREGATED" : "NOT_APPLICABLE");
        after.put("affectedRows", affectedStateCollector.capturePqcSubmission(
                event.getId(), event.getFeedbackSourceId()));
        MesPqcInspectionTaskDO pqcTask = resolvePqcInspectionTask(event);
        List<GxpAuditRelation> links = new ArrayList<>();
        links.add(new GxpAuditRelation("SUBJECT", "PROCESS_POOL_EVENT",
                String.valueOf(event.getId()), String.valueOf(review.getId()), null));
        links.add(new GxpAuditRelation("REVIEW", "PQC_REVIEW",
                String.valueOf(review.getId()), String.valueOf(review.getId()), null));
        links.add(new GxpAuditRelation("SOURCE", "PQC_INSPECTION_TASK",
                String.valueOf(pqcTask.getId()), null, null));
        links.add(new GxpAuditRelation("SUBJECT", "ACTIVE_ORDER",
                String.valueOf(pqcTask.getActiveOrderId()), null, null));
        links.add(new GxpAuditRelation("SIGNATURE", "SIGNATURE",
                String.valueOf(review.getReviewSignatureId()), null, null));
        gxpAuditService.append(GxpAuditCommand.builder()
                .eventSchemaVersion(2)
                .operationId(operationId)
                .subjectId("MES_PROCESS_POOL_EVENT:" + event.getId())
                .subjectVersion(String.valueOf(review.getId()))
                .reason(approved ? "PQC复核已通过" : review.getReviewRemark())
                .reasonCode(approved ? "MES_PQC_REVIEW_APPROVE" : "MES_PQC_REVIEW_REJECT")
                .reasonSource(approved ? "SYSTEM" : "USER")
                .beforeState(GxpAuditStateEnvelope.builder()
                        .state("ABSENT")
                        .objectVersion(String.valueOf(event.getId()))
                        .canonicalJson(JsonUtils.toJsonString(before))
                        .build())
                .afterState(GxpAuditStateEnvelope.builder()
                        .state(approved ? "PQC_REVIEW_APPROVED" : "PQC_REVIEW_REJECTED")
                        .objectVersion(String.valueOf(review.getId()))
                        .canonicalJson(JsonUtils.toJsonString(after))
                        .build())
                .idempotencyKey("PQC_REVIEW:" + review.getId())
                .requestId("MES-PQC-REVIEW:" + event.getId() + ":" + review.getId())
                .resultStatus("SUCCESS")
                .sourceType("SERVICE_METHOD")
                .sourceLocator("cn.iocoder.yudao.module.mes.service.pro.processpool.team."
                        + "MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission")
                .signatureRecordId(String.valueOf(review.getReviewSignatureId()))
                .links(links)
                .evidences(List.of(new GxpAuditEvidence("FORMAL_PQC_REVIEW_SOURCE",
                        String.valueOf(event.getId()), String.valueOf(review.getId()), null, "PQC_REVIEW")))
                .build());
    }

    private MesPqcInspectionTaskDO resolvePqcInspectionTask(MesProProcessPoolEventDO event) {
        Long feedbackSourceId = event.getFeedbackSourceId();
        if (feedbackSourceId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcReview.feedbackSourceId");
        }
        MesPqcInspectionTaskDO pqcTask = pqcTaskMapper.selectById(feedbackSourceId);
        if (pqcTask == null || !Objects.equals(feedbackSourceId, pqcTask.getId())
                || pqcTask.getActiveOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                    "pqcReview.pqcInspectionTask.activeOrderId");
        }
        return pqcTask;
    }

    private Map<String, Object> pqcReviewEventSnapshot(MesProProcessPoolEventDO event) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("eventId", event.getId());
        snapshot.put("eventType", event.getEventType());
        snapshot.put("workOrderId", event.getWorkOrderId());
        snapshot.put("routeId", event.getRouteId());
        snapshot.put("routeProcessId", event.getRouteProcessId());
        snapshot.put("processId", event.getProcessId());
        snapshot.put("qaProcessId", event.getQaProcessId());
        snapshot.put("feedbackSourceType", event.getFeedbackSourceType());
        snapshot.put("feedbackSourceId", event.getFeedbackSourceId());
        snapshot.put("actualEmployeeId", event.getActualEmployeeId());
        snapshot.put("signatureId", event.getSignatureId());
        snapshot.put("serverSubmitTime", event.getServerSubmitTime());
        snapshot.put("rawPayload", event.getRawPayload());
        return snapshot;
    }

    private Map<String, Object> pqcReviewSnapshot(MesProcessPoolSubmissionReviewDO review) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("reviewId", review.getId());
        snapshot.put("eventId", review.getEventId());
        snapshot.put("leaderUserId", review.getLeaderUserId());
        snapshot.put("leaderType", review.getLeaderType());
        snapshot.put("reviewStatus", review.getReviewStatus());
        snapshot.put("reviewRemark", review.getReviewRemark());
        snapshot.put("reviewedAt", review.getReviewedAt());
        snapshot.put("reviewSignatureId", review.getReviewSignatureId());
        snapshot.put("reviewSignatureUserId", review.getReviewSignatureUserId());
        snapshot.put("reviewSignatureSnapshotJson", review.getReviewSignatureSnapshotJson());
        return snapshot;
    }
    private boolean isIdempotentReplay(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                       MesProcessPoolSubmissionReviewDO existingReview) {
        return existingReview.getId() != null
                && Objects.equals(reqBO.getLeaderUserId(), existingReview.getLeaderUserId())
                && Objects.equals(reqBO.getLeaderType(), existingReview.getLeaderType())
                && Objects.equals(reqBO.getReviewStatus(), existingReview.getReviewStatus())
                && Objects.equals(normalizeRemark(reqBO.getReviewRemark()),
                normalizeRemark(existingReview.getReviewRemark()));
    }

    private String normalizeRemark(String remark) {
        return StrUtil.isBlank(remark) ? null : StrUtil.trim(remark);
    }

    private void validateReq(MesTeamLeaderSubmissionReviewReqBO reqBO) {
        if (reqBO == null || reqBO.getEventId() == null || reqBO.getLeaderUserId() == null
                || StrUtil.isBlank(reqBO.getLeaderType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "submissionReview");
        }
        if (StrUtil.isBlank(reqBO.getSignaturePassword())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "submissionReview.signaturePassword");
        }
        if (!VALID_REVIEW_STATUSES.contains(reqBO.getReviewStatus())) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_STATUS_INVALID, reqBO.getReviewStatus());
        }
        if (MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(reqBO.getReviewStatus())
                && StrUtil.isBlank(reqBO.getReviewRemark())) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_REJECT_REMARK_REQUIRED, reqBO.getEventId());
        }
    }

    private ReviewSignaturePayload recordReviewSignature(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                                         MesProProcessPoolEventDO event,
                                                         MesProProcessPoolEventRevisionDO correction) {
        Long signatureId = signatureService.recordTeamLeaderReviewSignature(
                reqBO.getLeaderUserId(),
                reqBO.getSignaturePassword(),
                buildReviewSignatureComment(reqBO, event),
                new MesTeamLeaderReviewSignatureContext(
                        event.getId(), reqBO.getReviewStatus(),
                        MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                        correction == null ? null : correction.getId(),
                        correction == null ? null : correction.getRevisionSignatureId(),
                        correction == null ? null : JsonUtils.parseTree(correction.getAfterPayload())
                                .path("supersededReviewId").longValue()));
        return new ReviewSignaturePayload(
                signatureId,
                reqBO.getLeaderUserId(),
                buildReviewSignatureSnapshot(reqBO, event, signatureId, correction));
    }

    private String buildReviewSignatureComment(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                               MesProProcessPoolEventDO event) {
        return "组长复核:" + reqBO.getLeaderType() + ":" + event.getId() + ":" + reqBO.getReviewStatus();
    }

    private String buildReviewSignatureSnapshot(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                                MesProProcessPoolEventDO event,
                                                Long signatureId,
                                                MesProProcessPoolEventRevisionDO correction) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("signatureId", signatureId);
        payload.put("actorId", reqBO.getLeaderUserId());
        payload.put("actionType", MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW);
        payload.put("processPoolEventId", event.getId());
        payload.put("eventType", event.getEventType());
        payload.put("leaderType", reqBO.getLeaderType());
        payload.put("reviewStatus", reqBO.getReviewStatus());
        payload.put("payloadHash", MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()));
        if (correction != null) {
            payload.put("revisionId", correction.getId());
            payload.put("revisionSignatureId", correction.getRevisionSignatureId());
            payload.put("supersededReviewId", JsonUtils.parseTree(correction.getAfterPayload())
                    .path("supersededReviewId").longValue());
        }
        return JsonUtils.toJsonString(payload);
    }

    private record ReviewSignaturePayload(Long reviewSignatureId,
                                          Long reviewSignatureUserId,
                                          String reviewSignatureSnapshotJson) {
    }

}
