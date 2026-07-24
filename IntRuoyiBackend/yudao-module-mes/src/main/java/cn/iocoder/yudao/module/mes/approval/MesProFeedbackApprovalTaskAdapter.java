package cn.iocoder.yudao.module.mes.approval;

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
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class MesProFeedbackApprovalTaskAdapter implements ApprovalTaskProvider {

    private static final String SOURCE_TASK_TYPE = "MES_PRO_FEEDBACK";
    private static final String DETAIL_ROUTE = "/mes/pro/feedback";
    private static final Set<String> PROCESS_ACTIONS = Set.of("PROCESS_IN_MODULE");
    private static final Set<String> REVIEW_ACTIONS = Set.of("PROCESS_IN_MODULE", "APPROVE", "REJECT");
    private static final Set<ApprovalTaskViewType> SUPPORTED_VIEWS = Set.of(
            ApprovalTaskViewType.TODO,
            ApprovalTaskViewType.DONE,
            ApprovalTaskViewType.MY_INITIATED
    );
    private static final Set<ApprovalTaskCapability> CAPABILITIES = Set.of(
            ApprovalTaskCapability.TIMELINE,
            ApprovalTaskCapability.AUDIT
    );
    private static final List<Integer> APPROVED_DONE_STATUSES = List.of(
            MesProFeedbackStatusEnum.UNCHECK.getStatus(),
            MesProFeedbackStatusEnum.FINISHED.getStatus()
    );
    private static final List<Integer> DONE_QUERY_STATUSES = List.of(
            MesProFeedbackStatusEnum.UNCHECK.getStatus(),
            MesProFeedbackStatusEnum.FINISHED.getStatus(),
            MesProFeedbackStatusEnum.PREPARE.getStatus()
    );
    private static final List<Integer> ACTIVE_STATUSES = List.of(
            MesProFeedbackStatusEnum.APPROVING.getStatus(),
            MesProFeedbackStatusEnum.UNCHECK.getStatus(),
            MesProFeedbackStatusEnum.FINISHED.getStatus()
    );

    private final MesProFeedbackMapper feedbackMapper;
    private final MesProFeedbackService feedbackService;

    public MesProFeedbackApprovalTaskAdapter(MesProFeedbackMapper feedbackMapper,
                                             MesProFeedbackService feedbackService) {
        this.feedbackMapper = feedbackMapper;
        this.feedbackService = feedbackService;
    }

    @Override
    public ApprovalModuleCode getModuleCode() {
        return ApprovalModuleCode.MES_FEEDBACK;
    }

    @Override
    public String getModuleName() {
        return "MES 报工审批";
    }

    @Override
    public String getProviderCode() {
        return "mes-feedback-approval";
    }

    @Override
    public String getProviderVersion() {
        return "phase8";
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
    public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
        Objects.requireNonNull(context.getLoginUserId(), "APPROVAL_LOGIN_USER_REQUIRED");
        List<MesProFeedbackDO> rows = switch (context.getViewType()) {
            case TODO -> feedbackMapper.selectUnifiedApprovalList(resolveApproveUserId(context), null,
                    List.of(MesProFeedbackStatusEnum.APPROVING.getStatus()), context.getKeyword());
            case DONE -> feedbackMapper.selectUnifiedApprovalList(resolveApproveUserId(context), null,
                    DONE_QUERY_STATUSES, context.getKeyword()).stream()
                    .filter(MesProFeedbackApprovalTaskAdapter::isApprovalDone)
                    .toList();
            case MY_INITIATED -> feedbackMapper.selectUnifiedApprovalList(null, resolveFeedbackUserId(context),
                    ACTIVE_STATUSES, context.getKeyword());
            default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: MES_FEEDBACK does not support "
                    + context.getViewType());
        };
        return pageRows(rows.stream().map(this::toSummary).toList(), context.getPageNo(), context.getPageSize());
    }

    @Override
    public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
        requireSourceTaskType(context.getSourceTaskType());
        Long feedbackId = resolveFeedbackId(context);
        MesProFeedbackDO feedback = requireFeedback(feedbackId);
        assertTimelineAccess(context, feedback);
        return buildTimeline(feedback);
    }

    private ApprovalTaskSummary toSummary(MesProFeedbackDO feedback) {
        Long feedbackId = requireFeedbackId(feedback);
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("feedbackId", String.valueOf(feedbackId));
        return ApprovalTaskSummary.builder()
                .id("MES_FEEDBACK:" + SOURCE_TASK_TYPE + ":" + feedbackId)
                .moduleCode(ApprovalModuleCode.MES_FEEDBACK)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(feedbackId))
                .businessKey(String.valueOf(feedbackId))
                .businessTitle("生产报工 " + requireText(feedback.getCode(), "MES_FEEDBACK_CODE_REQUIRED"))
                .businessCode(feedback.getCode())
                .businessStatus(resolveStatusName(feedback.getStatus()))
                .businessDeleted(Boolean.FALSE)
                .currentNodeCode(resolveCurrentNodeCode(feedback.getStatus()))
                .currentNodeName(resolveCurrentNodeName(feedback.getStatus()))
                .initiatorUserId(feedback.getFeedbackUserId())
                .assigneeUserId(resolveAssigneeUserId(feedback))
                .initiatedAt(feedback.getCreateTime())
                .taskCreatedAt(feedback.getCreateTime())
                .taskCompletedAt(resolveCompletedAt(feedback))
                .approvalResult(resolveApprovalResult(feedback))
                .approvalRemark(resolveApprovalRemark(feedback))
                .requiresSignature(Boolean.TRUE)
                .detailRoute(DETAIL_ROUTE)
                .detailQuery(detailQuery)
                .availableActions(resolveAvailableActions(feedback.getStatus()))
                .capabilities(CAPABILITIES)
                .build();
    }

    private List<ApprovalTaskTimelineEntry> buildTimeline(MesProFeedbackDO feedback) {
        Long feedbackId = requireFeedbackId(feedback);
        List<ApprovalTaskTimelineEntry> entries = new ArrayList<>();
        entries.add(ApprovalTaskTimelineEntry.builder()
                .id("MES_FEEDBACK:" + feedbackId + ":SUBMITTED")
                .moduleCode(ApprovalModuleCode.MES_FEEDBACK)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(feedbackId))
                .businessKey(String.valueOf(feedbackId))
                .nodeCode("SUBMITTED")
                .nodeName("提交报工审批")
                .action("SUBMITTED")
                .actionLabel("提交报工审批")
                .actorUserId(feedback.getFeedbackUserId())
                .actedAt(feedback.getCreateTime())
                .status("DONE")
                .evidenceType("MES_PRO_FEEDBACK")
                .domainReferenceId("mes_pro_feedback:" + feedbackId)
                .build());
        if (Objects.equals(feedback.getStatus(), MesProFeedbackStatusEnum.APPROVING.getStatus())) {
            entries.add(ApprovalTaskTimelineEntry.builder()
                    .id("MES_FEEDBACK:" + feedbackId + ":APPROVAL:PENDING")
                    .moduleCode(ApprovalModuleCode.MES_FEEDBACK)
                    .sourceTaskType(SOURCE_TASK_TYPE)
                    .sourceTaskId(String.valueOf(feedbackId))
                    .businessKey(String.valueOf(feedbackId))
                    .nodeCode("APPROVING")
                    .nodeName("报工审批")
                    .action("PENDING")
                    .actionLabel("待审批")
                    .actorUserId(feedback.getApproveUserId())
                    .actedAt(feedback.getUpdateTime())
                    .status("PENDING")
                    .evidenceType("MES_PRO_FEEDBACK_APPROVAL")
                    .domainReferenceId("mes_pro_feedback:" + feedbackId)
                    .build());
            return entries;
        }
        ApprovalTaskReviewResult approvalResult = resolveApprovalResult(feedback);
        if (approvalResult != null) {
            entries.add(ApprovalTaskTimelineEntry.builder()
                    .id("MES_FEEDBACK:" + feedbackId + ":" + resolveApprovalTimelineAction(approvalResult))
                    .moduleCode(ApprovalModuleCode.MES_FEEDBACK)
                    .sourceTaskType(SOURCE_TASK_TYPE)
                    .sourceTaskId(String.valueOf(feedbackId))
                    .businessKey(String.valueOf(feedbackId))
                    .nodeCode(resolveCurrentNodeCode(feedback.getStatus()))
                    .nodeName("报工审批")
                    .action(resolveApprovalTimelineAction(approvalResult))
                    .actionLabel(resolveApprovalTimelineLabel(approvalResult))
                    .actorUserId(feedback.getApproveUserId())
                    .actedAt(resolveCompletedAt(feedback))
                    .status(resolveStatusName(feedback.getStatus()))
                    .evidenceType("MES_PRO_FEEDBACK_APPROVAL")
                    .domainReferenceId("mes_pro_feedback:" + feedbackId)
                    .build());
        }
        return entries;
    }

    @Override
    public void review(ApprovalTaskReviewContext context) {
        requireSourceTaskType(context.getSourceTaskType());
        Long feedbackId = resolveFeedbackId(context.getSourceTaskId(), context.getBusinessKey());
        MesProFeedbackDO feedback = requireFeedback(feedbackId);
        assertReviewAccess(context, feedback);
        if (context.getResult() == ApprovalTaskReviewResult.APPROVE) {
            feedbackService.approveFeedback(feedbackId);
            return;
        }
        if (context.getResult() == ApprovalTaskReviewResult.REJECT) {
            feedbackService.rejectFeedback(feedbackId, context.getReason());
            return;
        }
        throw new IllegalArgumentException("APPROVAL_REVIEW_RESULT_UNSUPPORTED: " + context.getResult());
    }

    private MesProFeedbackDO requireFeedback(Long feedbackId) {
        MesProFeedbackDO feedback = feedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new IllegalStateException("MES_FEEDBACK_NOT_FOUND: " + feedbackId);
        }
        return feedback;
    }

    private static void assertTimelineAccess(ApprovalTaskTimelineQueryContext context, MesProFeedbackDO feedback) {
        if (context.isGlobalView()) {
            return;
        }
        Long loginUserId = context.getLoginUserId();
        if (Objects.equals(loginUserId, feedback.getFeedbackUserId())
                || Objects.equals(loginUserId, feedback.getApproveUserId())) {
            return;
        }
        throw new IllegalStateException("MES_FEEDBACK_TIMELINE_ACCESS_DENIED: " + requireFeedbackId(feedback));
    }

    private static void assertReviewAccess(ApprovalTaskReviewContext context, MesProFeedbackDO feedback) {
        if (context.isGlobalView()) {
            return;
        }
        if (Objects.equals(context.getLoginUserId(), feedback.getApproveUserId())) {
            return;
        }
        throw new IllegalStateException("MES_FEEDBACK_REVIEW_ACCESS_DENIED: " + requireFeedbackId(feedback));
    }

    private static void requireSourceTaskType(String sourceTaskType) {
        if (!SOURCE_TASK_TYPE.equals(sourceTaskType)) {
            throw new IllegalArgumentException("APPROVAL_SOURCE_TASK_TYPE_UNSUPPORTED: MES_FEEDBACK does not support "
                    + sourceTaskType);
        }
    }

    private static Long resolveFeedbackId(ApprovalTaskTimelineQueryContext context) {
        return resolveFeedbackId(context.getSourceTaskId(), context.getBusinessKey());
    }

    private static Long resolveFeedbackId(String sourceTaskId, String businessKey) {
        String value = sourceTaskId;
        if (value == null || value.isBlank()) {
            value = businessKey;
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("MES_FEEDBACK_ID_REQUIRED");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("MES_FEEDBACK_ID_INVALID: " + value, ex);
        }
    }

    private static Long requireFeedbackId(MesProFeedbackDO feedback) {
        if (feedback.getId() == null) {
            throw new IllegalStateException("MES_FEEDBACK_ID_REQUIRED");
        }
        return feedback.getId();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static String resolveCurrentNodeCode(Integer status) {
        if (Objects.equals(status, MesProFeedbackStatusEnum.APPROVING.getStatus())) {
            return "APPROVING";
        }
        if (Objects.equals(status, MesProFeedbackStatusEnum.UNCHECK.getStatus())) {
            return "UNCHECK";
        }
        if (Objects.equals(status, MesProFeedbackStatusEnum.FINISHED.getStatus())) {
            return "FINISHED";
        }
        return String.valueOf(status);
    }

    private static String resolveCurrentNodeName(Integer status) {
        if (Objects.equals(status, MesProFeedbackStatusEnum.APPROVING.getStatus())) {
            return "当前审批人";
        }
        return resolveStatusName(status);
    }

    private static String resolveStatusName(Integer status) {
        for (MesProFeedbackStatusEnum item : MesProFeedbackStatusEnum.values()) {
            if (Objects.equals(item.getStatus(), status)) {
                return item.getName();
            }
        }
        return String.valueOf(status);
    }

    private static Long resolveAssigneeUserId(MesProFeedbackDO feedback) {
        if (Objects.equals(feedback.getStatus(), MesProFeedbackStatusEnum.APPROVING.getStatus())) {
            return feedback.getApproveUserId();
        }
        return null;
    }

    private static Set<String> resolveAvailableActions(Integer status) {
        return Objects.equals(status, MesProFeedbackStatusEnum.APPROVING.getStatus())
                ? REVIEW_ACTIONS : PROCESS_ACTIONS;
    }

    private static LocalDateTime resolveCompletedAt(MesProFeedbackDO feedback) {
        if (resolveApprovalResult(feedback) != null) {
            return feedback.getUpdateTime();
        }
        return null;
    }

    private static boolean isApprovalDone(MesProFeedbackDO feedback) {
        return APPROVED_DONE_STATUSES.contains(feedback.getStatus()) || isRejectedFeedback(feedback);
    }

    private static boolean isRejectedFeedback(MesProFeedbackDO feedback) {
        return Objects.equals(feedback.getStatus(), MesProFeedbackStatusEnum.PREPARE.getStatus())
                && feedback.getApproveUserId() != null
                && (hasText(feedback.getRemark()) || isUpdatedAfterCreation(feedback));
    }

    private static ApprovalTaskReviewResult resolveApprovalResult(MesProFeedbackDO feedback) {
        if (APPROVED_DONE_STATUSES.contains(feedback.getStatus())) {
            return ApprovalTaskReviewResult.APPROVE;
        }
        return isRejectedFeedback(feedback) ? ApprovalTaskReviewResult.REJECT : null;
    }

    private static String resolveApprovalRemark(MesProFeedbackDO feedback) {
        return isRejectedFeedback(feedback) && hasText(feedback.getRemark()) ? feedback.getRemark().trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isUpdatedAfterCreation(MesProFeedbackDO feedback) {
        return feedback.getCreateTime() != null
                && feedback.getUpdateTime() != null
                && feedback.getUpdateTime().isAfter(feedback.getCreateTime());
    }

    private static String resolveApprovalTimelineAction(ApprovalTaskReviewResult result) {
        return result == ApprovalTaskReviewResult.REJECT ? "REJECTED" : "APPROVED";
    }

    private static String resolveApprovalTimelineLabel(ApprovalTaskReviewResult result) {
        return result == ApprovalTaskReviewResult.REJECT ? "审批驳回" : "审批通过";
    }

    private static PageResult<ApprovalTaskSummary> pageRows(List<ApprovalTaskSummary> rows,
                                                            Integer pageNo, Integer pageSize) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    private static Long resolveApproveUserId(ApprovalTaskQueryContext context) {
        return context.isGlobalView() ? null : context.getLoginUserId();
    }

    private static Long resolveFeedbackUserId(ApprovalTaskQueryContext context) {
        return context.isGlobalView() ? null : context.getLoginUserId();
    }
}
