package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductActorSupport;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateService;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;

/** A native source inside the existing single DCC provider, not another provider or Flowable task. */
@Component
public class DccProjectProductTaskDelegate {
    public static final String REVIEW = "DCC_PROJECT_PRODUCT_REVIEW";
    public static final String APPROVAL = "DCC_PROJECT_PRODUCT_APPROVAL";
    private final DccProjectProductCreateRequestMapper requests;
    private final DccProjectProductActorSupport actors;
    private final DccProjectProductCreateService service;

    public DccProjectProductTaskDelegate(DccProjectProductCreateRequestMapper requests,
                                       DccProjectProductActorSupport actors, DccProjectProductCreateService service) {
        this.requests = requests;this.actors = actors;this.service = service;
    }

    public static boolean supports(String type) { return REVIEW.equals(type) || APPROVAL.equals(type); }

    public List<ApprovalTaskSummary> list(ApprovalTaskQueryContext context) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        actors.requireAccount(context.getLoginUserId());
        // Native request permission filters only this source; a legitimate file task remains independent.
        if (!actors.canQuery(context.getLoginUserId())) return List.of();
        boolean approver = actors.isApprover(context.getLoginUserId());
        List<ApprovalTaskSummary> rows = new ArrayList<>();
        for (var request : requests.selectNativeTaskRows()) {
            if (!Objects.equals(request.getTenantId(), tenant)) throw new IllegalStateException("PROJECT_TASK_TENANT_MISMATCH");
            if (context.getViewType() == ApprovalTaskViewType.TODO) {
                if ("PENDING_REVIEW".equals(request.getStatus()) && request.getConfiguredReviewerUserId() != null
                        && (context.isGlobalView() || Objects.equals(request.getConfiguredReviewerUserId(), context.getLoginUserId())
                        && actors.canUpdate(context.getLoginUserId()))) rows.add(summary(request, REVIEW, false));
                if ("PENDING_APPROVAL".equals(request.getStatus()) && (context.isGlobalView() || approver)) {
                    rows.add(summary(request, APPROVAL, false));
                }
            } else if (context.getViewType() == ApprovalTaskViewType.DONE) {
                if (request.getReviewerUserId() != null && request.getReviewedTime() != null
                        && (context.isGlobalView() || Objects.equals(request.getReviewerUserId(), context.getLoginUserId()))) {
                    rows.add(summary(request, REVIEW, true));
                }
                if (request.getApproverUserId() != null && request.getApprovedTime() != null
                        && (context.isGlobalView() || Objects.equals(request.getApproverUserId(), context.getLoginUserId()))) {
                    rows.add(summary(request, APPROVAL, true));
                }
            } else throw new IllegalArgumentException("PROJECT_TASK_VIEW_UNSUPPORTED");
        }
        return rows.stream().filter(row -> matches(row, context.getKeyword())).toList();
    }

    private ApprovalTaskSummary summary(DccProjectProductCreateRequestDO request, String type, boolean done) {
        boolean review = REVIEW.equals(type);
        boolean rejected = review ? request.getReviewReason() == null && "REJECTED".equals(request.getStatus())
                && request.getApproverUserId() == null : request.getApprovalReason() == null && "REJECTED".equals(request.getStatus());
        String id = taskId(request, type);
        return ApprovalTaskSummary.builder().id("DCC:" + type + ":" + id).moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(type).sourceTaskId(id).businessKey(request.getId().toString())
                .businessTitle(request.getProjectName() + " · " + request.getProductName()).businessCode(request.getProjectCode())
                .businessStatus(request.getStatus()).businessDeleted(false)
                .currentNodeCode(type).currentNodeName(review ? "项目产品审核" : "项目产品批准")
                .initiatorUserId(request.getApplicantUserId()).assigneeUserId(review ? request.getConfiguredReviewerUserId()
                        : done ? request.getApproverUserId() : actors.requireApprover())
                .initiatedAt(request.getSubmittedTime()).taskCreatedAt(review ? request.getSubmittedTime() : request.getReviewedTime())
                .taskCompletedAt(done ? review ? request.getReviewedTime() : request.getApprovedTime() : null)
                .approvalResult(done ? rejected ? ApprovalTaskReviewResult.REJECT : ApprovalTaskReviewResult.APPROVE : null)
                .approvalRemark(done ? rejected ? request.getRejectReason() : review ? request.getReviewReason() : request.getApprovalReason() : null)
                .requiresSignature(false).detailRoute("/mdm/product-catalog")
                .detailQuery(Map.of("requestId", request.getId().toString(), "requestOpen", "records", "from", "approval-center"))
                .availableActions(Set.of("PROCESS_IN_MODULE"))
                .capabilities(Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.NOTIFICATION, ApprovalTaskCapability.AUDIT)).build();
    }

    public List<ApprovalTaskTimelineEntry> timeline(ApprovalTaskTimelineQueryContext context) {
        if (!supports(context.getSourceTaskType()) || context.getProcessInstanceId() != null) {
            throw new IllegalArgumentException("PROJECT_TASK_SOURCE_INVALID");
        }
        Long id;
        try {
            if (context.getBusinessKey() == null || !context.getBusinessKey().matches("[1-9][0-9]*")) throw new NumberFormatException();
            id = Long.valueOf(context.getBusinessKey());
        } catch (NumberFormatException ex) { throw new IllegalArgumentException("PROJECT_TASK_ID_INVALID"); }
        var request = service.getRequest(context.getLoginUserId(), id);
        if (!Objects.equals(context.getSourceTaskId(), taskId(request, context.getSourceTaskType()))) {
            throw new IllegalArgumentException("PROJECT_TASK_SOURCE_ID_MISMATCH");
        }
        List<ApprovalTaskTimelineEntry> entries = new ArrayList<>();
        add(entries, request, "SUBMIT", "提交申请", request.getApplicantUserId(), request.getSubmittedTime(), request.getCreationReason());
        if (request.getReviewedTime() != null) add(entries, request, REVIEW, "项目产品审核", request.getReviewerUserId(), request.getReviewedTime(),
                request.getReviewReason() == null ? request.getRejectReason() : request.getReviewReason());
        if (request.getApprovedTime() != null) add(entries, request, APPROVAL, "项目产品批准", request.getApproverUserId(), request.getApprovedTime(),
                request.getApprovalReason() == null ? request.getRejectReason() : request.getApprovalReason());
        return entries;
    }

    private void add(List<ApprovalTaskTimelineEntry> entries, DccProjectProductCreateRequestDO request,
                     String stage, String label, Long actor, LocalDateTime time, String reason) {
        if (actor == null || time == null) throw new IllegalStateException("PROJECT_TASK_HISTORY_INCOMPLETE");
        String status = "SUBMIT".equals(stage) ? "PENDING_REVIEW"
                : REVIEW.equals(stage) ? request.getReviewReason() == null ? "REJECTED" : "PENDING_APPROVAL"
                : request.getApprovalReason() == null ? "REJECTED" : "WRITING";
        String action = "SUBMIT".equals(stage) ? "SUBMIT" : "REJECTED".equals(status) ? "REJECT" : "APPROVE";
        String actionLabel = "SUBMIT".equals(stage) ? label : label + ("REJECT".equals(action) ? "驳回" : "通过");
        entries.add(ApprovalTaskTimelineEntry.builder().id(taskId(request, stage)).moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(stage).sourceTaskId(taskId(request, stage)).businessKey(request.getId().toString())
                .nodeCode(stage).nodeName(label).action(action).actionLabel(actionLabel).actorUserId(actor).actedAt(time)
                .comment(reason).status(status).evidenceType("DCC_PROJECT_PRODUCT_REQUEST")
                .domainReferenceId(request.getId().toString()).build());
    }

    private static String taskId(DccProjectProductCreateRequestDO request, String type) {
        return "DCC_PROJECT_PRODUCT:" + request.getTenantId() + ":" + request.getId() + ":" + type;
    }

    private static boolean matches(ApprovalTaskSummary row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        return List.of(row.getBusinessTitle(), row.getBusinessCode(), row.getBusinessKey()).stream()
                .anyMatch(text -> text != null && text.toLowerCase(Locale.ROOT).contains(query));
    }
}
