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
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerRoleService reviewerRoles;

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
            boolean role=roleRequest(request);
            if (context.getViewType() == ApprovalTaskViewType.TODO) {
                if(role){
                    if("PENDING_REVIEW".equals(request.getStatus()) && (context.isGlobalView() || reviewerRoles.canReview(context.getLoginUserId(),request)))rows.add(summary(request,REVIEW,false,context.getLoginUserId()));
                    continue;
                }
                if ("PENDING_REVIEW".equals(request.getStatus()) && request.getConfiguredReviewerUserId() != null
                        && (context.isGlobalView() || Objects.equals(request.getConfiguredReviewerUserId(), context.getLoginUserId())
                        && actors.canUpdate(context.getLoginUserId()))) rows.add(summary(request, REVIEW, false, context.getLoginUserId()));
                if ("PENDING_APPROVAL".equals(request.getStatus()) && (context.isGlobalView() || approver)) {
                    rows.add(summary(request, APPROVAL, false, context.getLoginUserId()));
                }
            } else if (context.getViewType() == ApprovalTaskViewType.DONE) {
                if (request.getReviewerUserId() != null && request.getReviewedTime() != null
                        && (context.isGlobalView() || Objects.equals(request.getReviewerUserId(), context.getLoginUserId()))) {
                    rows.add(summary(request, REVIEW, true, context.getLoginUserId()));
                }
                if (!role && request.getApproverUserId() != null && request.getApprovedTime() != null
                        && (context.isGlobalView() || Objects.equals(request.getApproverUserId(), context.getLoginUserId()))) {
                    rows.add(summary(request, APPROVAL, true, context.getLoginUserId()));
                }
            } else throw new IllegalArgumentException("PROJECT_TASK_VIEW_UNSUPPORTED");
        }
        return rows.stream().filter(row -> matches(row, context.getKeyword())).toList();
    }

    public cn.iocoder.yudao.framework.common.pojo.PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        Long actor=context.getLoginUserId();
        actors.requireAccount(actor);
        if(!actors.canQuery(actor))return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(),0L);
        boolean done=context.getViewType()==ApprovalTaskViewType.DONE;
        if(!done && context.getViewType()!=ApprovalTaskViewType.TODO)throw new IllegalArgumentException("PROJECT_TASK_VIEW_UNSUPPORTED");
        var base=new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DccProjectProductCreateRequestDO>();
        base.eq("tenant_id",tenant).in("status","PENDING_REVIEW","PENDING_APPROVAL","REJECTED","WRITING","WRITE_FAILED","COMPLETED");
        var invalid=base.clone().apply("COALESCE(((review_contract IS NULL AND reviewer_role_id IS NULL AND reviewer_role_code IS NULL) OR (review_contract='ROLE_SINGLE_REVIEW_V1' AND reviewer_role_id>0 AND reviewer_role_code='dcc_project_reviewer')),0)=0");
        if(requests.selectCount(invalid)>0)throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerRoleService.CONTRACT_INVALID);
        Long role=!done && !context.isGlobalView() && requests.selectCount(base.clone().eq("status","PENDING_REVIEW").eq("review_contract","ROLE_SINGLE_REVIEW_V1"))>0?reviewerRoles.reviewableRoleId(actor):null;
        boolean isApprover=actors.isApprover(actor);
        Long approver=!done && (isApprover || context.isGlobalView() && requests.selectCount(base.clone().isNull("review_contract").eq("status","PENDING_APPROVAL"))>0)?actors.requireApprover():null;
        boolean mayUpdate=!done && actors.canUpdate(actor);
        ApprovalQueryWindow window=ApprovalQueryWindow.from(context);
        long head=Math.addExact(window.offset(),window.limit());
        long total=0;
        List<ApprovalTaskSummary> rows=new ArrayList<>();
        for(boolean review:List.of(true,false)) {
            var query=base.clone();
            if(done) {
                query.isNotNull(review?"reviewer_user_id":"approver_user_id").isNotNull(review?"reviewed_time":"approved_time");
                if(!review)query.isNull("review_contract");
                if(!context.isGlobalView())query.eq(review?"reviewer_user_id":"approver_user_id",actor);
            } else if(review) {
                query.eq("status","PENDING_REVIEW");
                query.and(q->{
                    if(context.isGlobalView())q.apply("(review_contract='ROLE_SINGLE_REVIEW_V1' OR (review_contract IS NULL AND configured_reviewer_user_id IS NOT NULL))");
                    else {
                        q.apply("(review_contract='ROLE_SINGLE_REVIEW_V1' AND reviewer_role_id={0} AND reviewer_role_code='dcc_project_reviewer')",role==null?-1L:role);
                        if(mayUpdate)q.or(o->o.isNull("review_contract").eq("configured_reviewer_user_id",actor));
                    }
                });
            } else {
                query.isNull("review_contract").eq("status","PENDING_APPROVAL");
                if(!context.isGlobalView() && approver==null)query.apply("1=0");
            }
            if(context.getKeyword()!=null && !context.getKeyword().isBlank()) {
                String key=context.getKeyword().trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
                query.apply("(LOWER(CONCAT(project_name,' · ',product_name)) LIKE {0} ESCAPE '!' OR LOWER(project_code) LIKE {0} ESCAPE '!' OR CAST(id AS CHAR) LIKE {0} ESCAPE '!')","%"+key+"%");
            }
            cn.iocoder.yudao.framework.common.pojo.PageParam filter=new cn.iocoder.yudao.framework.common.pojo.PageParam();filter.setListFilters(context.getListFilters());
            cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.apply(filter,query,DccApprovalListFilterSql.project(review,done,approver));
            long count=requests.selectCount(query);
            total=Math.addExact(total,count);
            if(head==0 || count==0)continue;
            String time=done?review?"reviewed_time":"approved_time":review?"submitted_time":"reviewed_time";
            query.last("ORDER BY ("+time+" IS NULL) ASC,"+time+" DESC,CAST(CONCAT('DCC_PROJECT_PRODUCT:',tenant_id,':',id,':"+(review?REVIEW:APPROVAL)+"') AS BINARY) ASC LIMIT "+head);
            for(var request:requests.selectList(query))rows.add(summary(request,review?REVIEW:APPROVAL,done,actor));
        }
        List<ApprovalTaskSummary> ordered=ApprovalCenterServiceImpl.sortRows(rows,context.getViewType());
        int from=(int)Math.min(window.offset(),ordered.size()),to=(int)Math.min(head,ordered.size());
        return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(ordered.subList(from,to),total);
    }

    public void review(ApprovalTaskReviewContext context) {
        if (context.getModuleCode() != ApprovalModuleCode.DCC || !supports(context.getSourceTaskType())
                || context.getProcessInstanceId() != null) throw new IllegalArgumentException("PROJECT_TASK_SOURCE_INVALID");
        String key = context.getBusinessKey();
        if (key == null || !key.matches("[1-9][0-9]*")) throw new IllegalArgumentException("PROJECT_TASK_ID_INVALID");
        var request = service.getRequest(context.getLoginUserId(), Long.valueOf(key));
        if (!Objects.equals(context.getSourceTaskId(), taskId(request, context.getSourceTaskType())))
            throw new IllegalArgumentException("PROJECT_TASK_SOURCE_ID_MISMATCH");
        if (context.getResult() != ApprovalTaskReviewResult.APPROVE && context.getResult() != ApprovalTaskReviewResult.REJECT)
            throw new IllegalArgumentException("PROJECT_TASK_RESULT_INVALID");
        boolean review = REVIEW.equals(context.getSourceTaskType());
        if (!canDecide(request, context.getSourceTaskType(), context.getLoginUserId()))
            throw new IllegalArgumentException("PROJECT_TASK_DECISION_FORBIDDEN");
        boolean approve = context.getResult() == ApprovalTaskReviewResult.APPROVE;
        if (review) service.review(context.getLoginUserId(), request.getId(), context.getReason(), approve);
        else service.approve(context.getLoginUserId(), request.getId(), context.getReason(), approve);
    }

    private boolean canDecide(DccProjectProductCreateRequestDO request, String type, Long actor) {
        if (!actors.canQuery(actor)) return false;
        if (REVIEW.equals(type) && "PENDING_REVIEW".equals(request.getStatus())) {
            return roleRequest(request) ? reviewerRoles.canReview(actor, request)
                    : Objects.equals(actor, request.getConfiguredReviewerUserId()) && actors.canUpdate(actor);
        }
        return APPROVAL.equals(type) && !roleRequest(request) && "PENDING_APPROVAL".equals(request.getStatus())
                && actors.isApprover(actor);
    }

    private ApprovalTaskSummary summary(DccProjectProductCreateRequestDO request, String type, boolean done, Long actor) {
        boolean role=roleRequest(request);
        boolean review = REVIEW.equals(type);
        boolean rejected = rejectedAt(request,type);
        String id = taskId(request, type);
        return ApprovalTaskSummary.builder().id("DCC:" + type + ":" + id).moduleCode(ApprovalModuleCode.DCC)
                .sourceTaskType(type).sourceTaskId(id).businessKey(request.getId().toString())
                .businessTitle(request.getProjectName() + " · " + request.getProductName()).businessCode(request.getProjectCode())
                .businessStatus(request.getStatus()).businessDeleted(false)
                .currentNodeCode(type).currentNodeName(role ? "项目审核员" : review ? "项目产品审核" : "项目产品批准")
                .initiatorUserId(request.getApplicantUserId()).assigneeUserId(role ? done ? request.getReviewerUserId() : null : review ? request.getConfiguredReviewerUserId()
                        : done ? request.getApproverUserId() : actors.requireApprover())
                .initiatedAt(request.getSubmittedTime()).taskCreatedAt(review ? request.getSubmittedTime() : request.getReviewedTime())
                .taskCompletedAt(done ? review ? request.getReviewedTime() : request.getApprovedTime() : null)
                .approvalResult(done ? rejected ? ApprovalTaskReviewResult.REJECT : ApprovalTaskReviewResult.APPROVE : null)
                .approvalRemark(done ? rejected ? request.getRejectReason() : review ? request.getReviewReason() : request.getApprovalReason() : null)
                .requiresSignature(true).detailRoute("/mdm/product-catalog")
                .detailQuery(Map.of("requestId", request.getId().toString(), "requestOpen", "records", "from", "approval-center"))
                .availableActions(!done && canDecide(request, type, actor) ? Set.of("APPROVE", "REJECT") : Set.of())
                .capabilities(Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.NOTIFICATION,
                        ApprovalTaskCapability.AUDIT, ApprovalTaskCapability.SIGNATURE_AUTHORIZATION)).build();
    }

    public List<ApprovalTaskTimelineEntry> timeline(ApprovalTaskTimelineQueryContext context) {
        if (!supports(context.getSourceTaskType()) || context.getProcessInstanceId() != null
                && !context.getProcessInstanceId().isBlank()) {
            throw new IllegalArgumentException("PROJECT_TASK_SOURCE_INVALID");
        }
        Long id;
        try {
            if (context.getBusinessKey() == null || !context.getBusinessKey().matches("[1-9][0-9]*")) throw new NumberFormatException();
            id = Long.valueOf(context.getBusinessKey());
        } catch (NumberFormatException ex) { throw new IllegalArgumentException("PROJECT_TASK_ID_INVALID"); }
        var request = service.getRequest(context.getLoginUserId(), id);
        boolean role=roleRequest(request);
        if(role && !REVIEW.equals(context.getSourceTaskType()))throw new IllegalArgumentException("ROLE_PROJECT_HAS_NO_SECOND_APPROVAL");
        if (!Objects.equals(context.getSourceTaskId(), taskId(request, context.getSourceTaskType()))) {
            throw new IllegalArgumentException("PROJECT_TASK_SOURCE_ID_MISMATCH");
        }
        List<ApprovalTaskTimelineEntry> entries = new ArrayList<>();
        add(entries, request, "SUBMIT", "提交申请", request.getApplicantUserId(), request.getSubmittedTime(), request.getCreationReason());
        if (request.getReviewedTime() != null) add(entries, request, REVIEW, role ? "项目审核员" : "项目产品审核", request.getReviewerUserId(), request.getReviewedTime(),
                rejectedAt(request,REVIEW) ? request.getRejectReason() : request.getReviewReason());
        if (!role && request.getApprovedTime() != null) add(entries, request, APPROVAL, "项目产品批准", request.getApproverUserId(), request.getApprovedTime(),
                rejectedAt(request,APPROVAL) ? request.getRejectReason() : request.getApprovalReason());
        return entries;
    }

    private void add(List<ApprovalTaskTimelineEntry> entries, DccProjectProductCreateRequestDO request,
                     String stage, String label, Long actor, LocalDateTime time, String reason) {
        if (actor == null || time == null) throw new IllegalStateException("PROJECT_TASK_HISTORY_INCOMPLETE");
        String status = "SUBMIT".equals(stage) ? "PENDING_REVIEW"
                : rejectedAt(request,stage) ? "REJECTED"
                : REVIEW.equals(stage) ? roleRequest(request) ? "COMPLETED" : "PENDING_APPROVAL" : "WRITING";
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
    private static boolean rejectedAt(DccProjectProductCreateRequestDO request,String type) {
        return "REJECTED".equals(request.getStatus())
                && (APPROVAL.equals(type) || REVIEW.equals(type) && request.getApprovedTime()==null);
    }
    private boolean roleRequest(DccProjectProductCreateRequestDO request){
        if(request.getReviewContract()==null && request.getReviewerRoleId()==null && request.getReviewerRoleCode()==null)return false;
        return reviewerRoles.isRoleContract(request);
    }

    private static boolean matches(ApprovalTaskSummary row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        return List.of(row.getBusinessTitle(), row.getBusinessCode(), row.getBusinessKey()).stream()
                .anyMatch(text -> text != null && text.toLowerCase(Locale.ROOT).contains(query));
    }
}
