package cn.iocoder.yudao.module.mes.approval;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalCenterReviewPreflight;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class MesProEdhrApprovalTaskAdapter implements ApprovalTaskProvider, ApprovalCenterReviewPreflight,
        cn.iocoder.yudao.module.bpm.approval.content.ApprovalTrustedDecisionProvider {

    private static final String SOURCE_TASK_TYPE = "EDHR_WORK_TASK";
    private static final String EDHR_WORK_TASK_ROUTE = "/mes/pro/feedback/edhr-work-task";
    private static final String EDHR_APPROVAL_DETAIL_ROUTE = "/mes/pro/feedback/edhr-approval/detail";
    private static final String EDHR_BATCH_EXECUTION_DETAIL_ROUTE = "/mes/pro/feedback/edhr-batch-execution/detail";
    private static final Set<ApprovalTaskViewType> SUPPORTED_VIEWS = Set.of(
            ApprovalTaskViewType.TODO,
            ApprovalTaskViewType.DONE
    );
    private static final Set<ApprovalTaskCapability> CAPABILITIES = Set.of(
            ApprovalTaskCapability.TIMELINE,
            ApprovalTaskCapability.AUDIT,
            ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
            ApprovalTaskCapability.EVIDENCE_LEDGER
    );

    private final MesProEdhrWorkTaskService workTaskService;
    private final MesProEdhrReleaseService releaseService;
    private final MesManagerReleaseCompletedActorResolver completedActorResolver;
    @jakarta.annotation.Resource
    private MesActiveOrderHandoffApprovalProjection handoffProjection;
    @jakarta.annotation.Resource
    private MesEdhrApprovalOutcomeReader outcomeReader;
    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions;

    public MesProEdhrApprovalTaskAdapter(MesProEdhrWorkTaskService workTaskService,
                                         MesProEdhrReleaseService releaseService,
                                         MesManagerReleaseCompletedActorResolver completedActorResolver) {
        this.workTaskService = workTaskService;
        this.releaseService = releaseService;
        this.completedActorResolver = completedActorResolver;
    }

    @Override
    public ApprovalModuleCode getModuleCode() {
        return ApprovalModuleCode.EDHR;
    }

    @Override
    public String getModuleName() {
        return "eDHR 审批";
    }

    @Override
    public String getProviderCode() {
        return "edhr-work-task-approval";
    }

    @Override
    public String getProviderVersion() {
        return "phase1";
    }

    @Override
    public Set<ApprovalTaskViewType> getSupportedViewTypes() {
        return SUPPORTED_VIEWS;
    }

    @Override
    public Set<ApprovalTaskCapability> getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true,
            isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
        if (!SUPPORTED_VIEWS.contains(context.getViewType())) {
            throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: EDHR does not support " + context.getViewType());
        }
        long limit = prefixLimit(context);
        PageResult<MesEdhrAuthorizedTaskProjection> page = workTaskService.getApprovalCenterAuthorizedPrefix(toReqVO(context),
                context.isGlobalView(), context.getViewType() == ApprovalTaskViewType.DONE, limit);
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED: EDHR");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: EDHR");
        PageResult<ApprovalTaskSummary> handoffs = Objects.requireNonNull(handoffProjection.prefix(context, limit),
                "APPROVAL_ADAPTER_PAGE_REQUIRED: EDHR handoff");
        var summaries = new ArrayList<ApprovalTaskSummary>(page.getList().stream()
                .map(task -> toSummary(task, context.getLoginUserId())).toList());
        summaries.addAll(Objects.requireNonNull(handoffs.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED: EDHR handoff"));
        sortSummaries(summaries, context.getViewType());
        return new PageResult<>(cn.iocoder.yudao.module.bpm.approval.service.ApprovalQueryWindow.from(context).slice(summaries).getList(),
                Math.addExact(Objects.requireNonNull(page.getTotal()), Objects.requireNonNull(handoffs.getTotal())));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true,
            isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
        if(MesActiveOrderHandoffApprovalProjection.SOURCE_TASK_TYPE.equals(context.getSourceTaskType()))return handoffProjection.timeline(context);
        requireSourceTaskType(context.getSourceTaskType());
        Long workTaskId = parseRequiredLong(context.getSourceTaskId(),
                "APPROVAL_BUSINESS_KEY_REQUIRED: eDHR work task id is required");
        if (workTaskId <= 0 || !Objects.equals(workTaskId.toString(), context.getSourceTaskId())
                || !Objects.equals(context.getSourceTaskId(), context.getBusinessKey())) {
            throw new IllegalArgumentException("APPROVAL_TASK_IDENTITY_MISMATCH: EDHR timeline");
        }
        Long executionId = parseOptionalLong(context.getProcessInstanceId(),
                "APPROVAL_PROCESS_INSTANCE_INVALID: eDHR execution id must be numeric ");
        List<MesProEdhrWorkTaskDO> tasks = workTaskService.getApprovalCenterTimelineTasks(workTaskId, executionId,
                context.isGlobalView());
        Objects.requireNonNull(tasks, "APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline list is required");
        if (tasks.isEmpty()) {
            throw new IllegalStateException("APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline is empty");
        }
        return tasks.stream()
                .map(this::toTimelineEntry)
                .toList();
    }

    @Override
    public void preflightReview(ApprovalTaskReviewContext context) {
        Objects.requireNonNull(context, "APPROVAL_REVIEW_CONTEXT_REQUIRED");
        requireSourceTaskType(context.getSourceTaskType());
        if (context.getResult() != ApprovalTaskReviewResult.APPROVE) {
            throw new IllegalArgumentException("APPROVAL_REVIEW_RESULT_UNSUPPORTED: EDHR center only supports APPROVE");
        }
        Long workTaskId = parseRequiredLong(context.getSourceTaskId(), "APPROVAL_TASK_ID_REQUIRED: EDHR");
        if (workTaskId <= 0 || !Objects.equals(String.valueOf(workTaskId), context.getSourceTaskId())
                || !Objects.equals(context.getSourceTaskId(), context.getBusinessKey())
                || context.getProcessInstanceId() != null) {
            throw new IllegalArgumentException("APPROVAL_TASK_IDENTITY_MISMATCH: EDHR release");
        }
        releaseService.preflightApprovalCenterReview(context.getLoginUserId(), workTaskId);
    }

    @Override
    public void review(ApprovalTaskReviewContext context) {
        requireSourceTaskType(context.getSourceTaskType());
        Long workTaskId = parseRequiredLong(context.getSourceTaskId(),
                "APPROVAL_BUSINESS_KEY_REQUIRED: eDHR work task id is required");
        MesProEdhrWorkTaskDO task = workTaskService.getReleaseApprovalTaskForReview(workTaskId, null);
        Long releaseTransactionId = task.getBusinessScopeId();
        if (ApprovalTaskReviewResult.APPROVE.equals(context.getResult())) {
            releaseService.approve(new MesProEdhrReleaseApproveReqVO()
                    .setReleaseTransactionId(releaseTransactionId)
                    .setWorkTaskId(workTaskId)
                    .setExpectedVersion(releaseService.get(releaseTransactionId).getVersion())
                    .setPassword(context.getSignaturePassword())
                    .setSignoffSubjectId(context.getSignatureSubjectId())
                    .setIdempotencyKey(buildReviewIdempotencyKey(context.getResult(), workTaskId))
                    .setSignoffEvidenceHash(buildSignoffEvidenceHash(context))
                    .setApprovalOpinion(context.getReason()));
            return;
        }
        if (ApprovalTaskReviewResult.REJECT.equals(context.getResult())) {
            releaseService.reject(new MesProEdhrReleaseRejectReqVO()
                    .setReleaseTransactionId(releaseTransactionId)
                    .setIdempotencyKey(buildReviewIdempotencyKey(context.getResult(), workTaskId))
                    .setRejectReason(context.getReason()));
            return;
        }
        throw new IllegalArgumentException("APPROVAL_REVIEW_RESULT_UNSUPPORTED: EDHR release "
                + context.getResult());
    }

    private static MesProEdhrWorkTaskPageReqVO toReqVO(ApprovalTaskQueryContext context) {
        MesProEdhrWorkTaskPageReqVO reqVO = new MesProEdhrWorkTaskPageReqVO();
        reqVO.setBatchCode(context.getKeyword());
        return reqVO;
    }

    private static long prefixLimit(ApprovalTaskQueryContext context) {
        var window=cn.iocoder.yudao.module.bpm.approval.service.ApprovalQueryWindow.from(context);
        return Math.addExact(window.offset(),window.limit());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true,
            isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public PageResult<ApprovalTaskSummary> window(ApprovalTaskQueryContext context,
            cn.iocoder.yudao.module.bpm.approval.service.ApprovalQueryWindow window) {
        return page(context.withWindow(window));
    }

    @Override
    public cn.iocoder.yudao.module.bpm.approval.content.ApprovalDomainDecision decide(Long actorId, Long receiptId,
            cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewCommand command,
            cn.iocoder.yudao.module.bpm.approval.content.PreparedApprovalContent content,
            cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService.VerifiedSignatureFact signature) {
        if (command.getResult() != ApprovalTaskReviewResult.APPROVE) throw new IllegalArgumentException("APPROVAL_REVIEW_RESULT_UNSUPPORTED");
        var auditFact = creationAudits.requireVerifiedEvent(content.identity().tenantId(), signature.auditEventId());
        var raw = cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson.parse(content.canonicalBusinessContentJson());
        var result = releaseService.approveFromCenter(new cn.iocoder.yudao.module.mes.approval.content.EdhrCenterReviewCommand()
                .setActorId(actorId).setWorkTaskId(Long.valueOf(content.identity().sourceTaskId()))
                .setExpectedVersion(Integer.valueOf(raw.at("/release/version").textValue())).setIdempotencyKey(command.getIdempotencyKey())
                .setReason(command.getReason()).setSignatureId(signature.id()).setSignatureCreationEventId(signature.auditEventId())
                .setSignatureCreationEventHash(auditFact.eventHash()).setCenterReceiptId(receiptId).setPrepareId(command.getPrepareId())
                .setCanonicalHash(content.contentHash()).setRenderedHash(content.contentHash()).setSubjectId(signature.subjectId())
                .setSubjectVersion(signature.subjectVersion()).setEvidenceHash(signature.evidenceHash()));
        var events = releaseEvents.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO>()
                .eq(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO::getReleaseTransactionId, Long.valueOf(content.identity().domainSubjectId()))
                .eq(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO::getIdempotencyKey,command.getIdempotencyKey()));
        if (events.size()!=1 || result.getReleaseDecisionId()==null) throw new IllegalStateException("EDHR_CENTER_DOMAIN_LINKS_MISSING");
        var links = new LinkedHashMap<String,Object>(); links.put("releaseId", content.identity().domainSubjectId());links.put("decisionId", result.getReleaseDecisionId().toString());links.put("eventId",events.get(0).getId().toString());
        var candidates=creationAudits.readCandidateEventIdsByTarget(content.identity().tenantId(),"RELEASE_DECISION",result.getReleaseDecisionId().toString());
        var verifiedDomain=new java.util.ArrayList<cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService.VerifiedAuditEventFact>();
        for(var candidate:candidates) {
            var fact=creationAudits.requireVerifiedEvent(content.identity().tenantId(),candidate);
            var envelope=cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson.parse(fact.canonicalEventJson());
            if("mes.market-release.approve".equals(envelope.path("operationId").asText()) && signature.id().toString().equals(envelope.path("signatureRecordId").asText())) verifiedDomain.add(fact);
        }
        if(verifiedDomain.size()!=1) throw new IllegalStateException("EDHR_CENTER_DOMAIN_CREATION_NOT_UNIQUE");
        links.put("businessAuditEventId",verifiedDomain.get(0).eventId().toString());links.put("businessAuditEventHash",verifiedDomain.get(0).eventHash());
        return new cn.iocoder.yudao.module.bpm.approval.content.ApprovalDomainDecision("APPROVE",result.getReleaseStatus(),cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(links));
    }
    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService creationAudits;
    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionEventMapper releaseEvents;
    private ApprovalTaskSummary toSummary(MesEdhrAuthorizedTaskProjection projected, Long loginUserId) {
        MesProEdhrWorkTaskRespVO task = projected.display();
        MesEdhrApprovalOutcome outcome = outcomeReader.readAuthorizedTask(projected.task());
        if (task.getId() == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_REQUIRED: eDHR work task id is required");
        }
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("workTaskId", String.valueOf(task.getId()));
        if (isReleaseApprovalTask(task)) {
            if (task.getBatchExecutionId() != null) {
                detailQuery.put("id", String.valueOf(task.getBatchExecutionId()));
            }
            detailQuery.put("focus", "approval");
            if (task.getBusinessScopeId() != null) {
                detailQuery.put("releaseTransactionId", String.valueOf(task.getBusinessScopeId()));
            }
        } else if (task.getExecutionId() != null) {
            detailQuery.put("executionId", String.valueOf(task.getExecutionId()));
        }
        ActionUrlParts decisionActionUrl = resolveDecisionActionUrl(task);
        Map<String, String> decisionDetailQuery = resolveDecisionDetailQuery(task, detailQuery, decisionActionUrl);
        String decisionRoute = resolveDecisionDetailRoute(task, decisionActionUrl);
        String targetRoute = decisionRoute == null ? resolveDetailRoute(task) : decisionRoute;
        List<String> requiredPermissions = requiredNavigationPermissions(targetRoute);
        List<String> missing = requiredPermissions.stream()
                .filter(permission -> !permissions.hasAnyPermissions(loginUserId, permission)).toList();
        if (!missing.isEmpty()) {
            decisionDetailQuery.put("requiredPermissionsMissing", String.join(",", missing));
        }
        if (requiredPermissions.isEmpty()) {
            decisionDetailQuery.put("navigationContractMissing", "正式目标页权限合同未配置");
        }
        boolean navigationAllowed = !requiredPermissions.isEmpty() && missing.isEmpty();
        return MesEdhrApprovalOutcomeProjection.apply(ApprovalTaskSummary.builder()
                .id("EDHR:" + SOURCE_TASK_TYPE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.EDHR)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(task.getId()))
                .businessKey(String.valueOf(task.getId()))
                .businessTitle(resolveTitle(task))
                .businessCode(task.getTaskCode())
                .currentNodeCode(hasText(task.getSignatureCellKey()) ? task.getSignatureCellKey() : task.getTaskType())
                .currentNodeName(task.getProcessName())
                .initiatorUserId(task.getSourceUserId())
                .initiatorUserName(task.getSourceUserName())
                .assigneeUserId(task.getAssigneeUserId())
                .processInstanceId(resolveProcessInstanceId(task))
                .taskCreatedAt(task.getCreateTime())
                .taskCompletedAt(task.getCompletedAt())
                .requiresSignature(isReleaseApprovalTask(task) || hasText(task.getSignatureCellKey()))
                .detailRoute(resolveDetailRoute(task))
                .detailQuery(detailQuery)
                .decisionDetailRoute(decisionRoute)
                .decisionDetailQuery(decisionDetailQuery)
                .availableActions(navigationAllowed ? resolveAvailableActions(task) : Set.of())
                .capabilities(CAPABILITIES)
                .build(), outcome);
    }

    // Contracts follow the actual mandatory page loads, not candidate membership or action tokens.
    private static List<String> requiredNavigationPermissions(String route) {
        if (EDHR_APPROVAL_DETAIL_ROUTE.equals(route)) {
            return List.of("mes:pro-batch-record-execution:approve", "mes:pro-batch-record-execution:track",
                    "mes:pro-batch-record-execution:signature-query");
        }
        if (EDHR_BATCH_EXECUTION_DETAIL_ROUTE.equals(route)) {
            return List.of("mes:pro-edhr-batch-execution:query");
        }
        if (EDHR_WORK_TASK_ROUTE.equals(route)) {
            return List.of("mes:pro-edhr-work-task:query");
        }
        if ("/mes/production-release/pqc".equals(route)) {
            return List.of("mes:pro-production-release:query");
        }
        if ("/mes/pro/feedback/edhr-execution/form".equals(route)) {
            return List.of("mes:pro-batch-record-execution:query", "mes:pro-batch-record-execution:track",
                    "mes:pro-batch-record-execution:signature-query");
        }
        return List.of();
    }

    private ApprovalTaskTimelineEntry toTimelineEntry(MesProEdhrWorkTaskDO task) {
        MesEdhrApprovalOutcome outcome = outcomeReader.readAuthorizedTask(task);
        if (task.getId() == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_REQUIRED: eDHR work task id is required");
        }
        return MesEdhrApprovalOutcomeProjection.apply(ApprovalTaskTimelineEntry.builder()
                .id("EDHR:" + SOURCE_TASK_TYPE + ":" + task.getId())
                .moduleCode(ApprovalModuleCode.EDHR)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(task.getId()))
                .businessKey(String.valueOf(task.getId()))
                .nodeCode(hasText(task.getSignatureCellKey()) ? task.getSignatureCellKey() : task.getTaskType())
                .nodeName(hasText(task.getProcessName()) ? task.getProcessName() : task.getTaskType())
                .action(MesEdhrApprovalOutcomeProjection.action(outcome))
                .actionLabel(MesEdhrApprovalOutcomeProjection.actionLabel(outcome))
                .taskCreatedAt(task.getCreateTime())
                .taskCompletedAt(task.getCompletedAt())
                .status(task.getStatus())
                .domainReferenceId(String.valueOf(task.getId()))
                .build(), outcome);
    }

    private static String resolveTitle(MesProEdhrWorkTaskRespVO task) {
        if (hasText(task.getBatchCode()) && hasText(task.getProcessName())) {
            return task.getBatchCode() + " / " + task.getProcessName();
        }
        if (hasText(task.getTaskCode())) {
            return task.getTaskCode();
        }
        return "eDHR 工作任务 #" + task.getId();
    }

    private static String resolveProcessInstanceId(MesProEdhrWorkTaskRespVO task) {
        if (isReleaseApprovalTask(task)) {
            return null;
        }
        return task.getExecutionId() == null ? null : String.valueOf(task.getExecutionId());
    }

    private static String resolveDetailRoute(MesProEdhrWorkTaskRespVO task) {
        if (isReleaseApprovalTask(task)) {
            return EDHR_BATCH_EXECUTION_DETAIL_ROUTE;
        }
        return EDHR_WORK_TASK_ROUTE;
    }

    private static String resolveDecisionDetailRoute(MesProEdhrWorkTaskRespVO task,
                                                     ActionUrlParts decisionActionUrl) {
        if (isReleaseApprovalTask(task)) {
            return EDHR_BATCH_EXECUTION_DETAIL_ROUTE;
        }
        if (isApprovalDecisionTask(task)) {
            return task.getExecutionId() == null ? null : EDHR_APPROVAL_DETAIL_ROUTE;
        }
        return decisionActionUrl == null ? null : decisionActionUrl.route();
    }

    private static Map<String, String> resolveDecisionDetailQuery(MesProEdhrWorkTaskRespVO task,
                                                                  Map<String, String> detailQuery,
                                                                  ActionUrlParts decisionActionUrl) {
        if (isReleaseApprovalTask(task)) {
            return new LinkedHashMap<>(detailQuery);
        }
        if (!isApprovalDecisionTask(task)) {
            return decisionActionUrl == null ? new LinkedHashMap<>() : new LinkedHashMap<>(decisionActionUrl.query());
        }
        Map<String, String> query = new LinkedHashMap<>();
        if (task.getExecutionId() != null) {
            query.put("id", String.valueOf(task.getExecutionId()));
        }
        query.put("workTaskId", String.valueOf(task.getId()));
        if (hasText(task.getBpmTaskId())) {
            query.put("bpmTaskId", task.getBpmTaskId());
        }
        return query;
    }

    private static ActionUrlParts resolveDecisionActionUrl(MesProEdhrWorkTaskRespVO task) {
        if (isReleaseApprovalTask(task) || isApprovalDecisionTask(task) || !hasText(task.getActionUrl())) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(task.getActionUrl().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("APPROVAL_ACTION_URL_INVALID: eDHR work task " + task.getId(), ex);
        }
        if (!hasText(uri.getPath())) {
            throw new IllegalStateException("APPROVAL_ACTION_URL_ROUTE_REQUIRED: eDHR work task " + task.getId());
        }
        return new ActionUrlParts(uri.getPath(), parseActionUrlQuery(uri.getRawQuery(), task.getId()));
    }

    private static Map<String, String> parseActionUrlQuery(String rawQuery, Long workTaskId) {
        Map<String, String> query = new LinkedHashMap<>();
        if (!hasText(rawQuery)) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int separatorIndex = pair.indexOf('=');
            String rawKey = separatorIndex < 0 ? pair : pair.substring(0, separatorIndex);
            String rawValue = separatorIndex < 0 ? "" : pair.substring(separatorIndex + 1);
            String key = decodeActionUrlPart(rawKey, workTaskId);
            if (!hasText(key)) {
                throw new IllegalStateException("APPROVAL_ACTION_URL_QUERY_KEY_REQUIRED: eDHR work task "
                        + workTaskId);
            }
            query.put(key, decodeActionUrlPart(rawValue, workTaskId));
        }
        return query;
    }

    private static String decodeActionUrlPart(String value, Long workTaskId) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("APPROVAL_ACTION_URL_QUERY_INVALID: eDHR work task " + workTaskId, ex);
        }
    }

    private static Set<String> resolveAvailableActions(MesProEdhrWorkTaskRespVO task) {
        if (isReleaseApprovalTask(task) && isProcessableStatus(task.getStatus())) {
            return Set.of("APPROVE", "PROCESS_IN_MODULE");
        }
        if (isApprovalDecisionTask(task) && isProcessableStatus(task.getStatus())) {
            String reviewAction = MesProEdhrWorkTaskService.TASK_TYPE_APPROVE.equals(task.getTaskType())
                    ? "APPROVE_IN_MODULE" : "REVIEW_IN_MODULE";
            return Set.of(reviewAction, "PROCESS_IN_MODULE");
        }
        return Set.of("PROCESS_IN_MODULE");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static List<ApprovalTaskSummary> slicePageRows(List<ApprovalTaskSummary> rows,
                                                           Integer pageNo,
                                                           Integer pageSize) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = (int) Math.min((long) (safePageNo - 1) * safePageSize, rows.size());
        int toIndex = (int) Math.min((long) fromIndex + safePageSize, rows.size());
        return rows.subList(fromIndex, toIndex);
    }

    private static void sortSummaries(List<ApprovalTaskSummary> rows, ApprovalTaskViewType view) {
        var ordered=cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterServiceImpl.sortRows(rows,view);
        rows.clear(); rows.addAll(ordered);
    }
    private static void requireSourceTaskType(String sourceTaskType) {
        if (!SOURCE_TASK_TYPE.equals(sourceTaskType)) {
            throw new IllegalArgumentException("APPROVAL_SOURCE_TASK_TYPE_UNSUPPORTED: EDHR does not support "
                    + sourceTaskType);
        }
    }

    private static Long parseRequiredLong(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return parseOptionalLong(value, message);
    }

    private static Long parseOptionalLong(String value, String message) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(message + value, ex);
        }
    }

    private static String buildReviewIdempotencyKey(ApprovalTaskReviewResult result, Long workTaskId) {
        return "APPROVAL-CENTER-" + result.name() + "-" + workTaskId;
    }

    private static String buildSignoffEvidenceHash(ApprovalTaskReviewContext context) {
        if (!hasText(context.getSignatureSubjectId()) || !hasText(context.getSignatureEvidenceHash())) {
            throw new IllegalStateException("APPROVAL_SIGNATURE_EVIDENCE_REQUIRED: EDHR release approval");
        }
        return context.getSignatureEvidenceHash();
    }

    private static boolean isProcessableStatus(String status) {
        return MesProEdhrWorkTaskStatus.TODO.equals(status) || MesProEdhrWorkTaskStatus.OVERDUE.equals(status);
    }

    private static boolean isReleaseApprovalTask(MesProEdhrWorkTaskRespVO task) {
        return task != null && MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType());
    }

    private static boolean isApprovalDecisionTask(MesProEdhrWorkTaskRespVO task) {
        return task != null && (MesProEdhrWorkTaskService.TASK_TYPE_REVIEW.equals(task.getTaskType())
                || MesProEdhrWorkTaskService.TASK_TYPE_APPROVE.equals(task.getTaskType()));
    }

    private record ActionUrlParts(String route, Map<String, String> query) {
    }
}
