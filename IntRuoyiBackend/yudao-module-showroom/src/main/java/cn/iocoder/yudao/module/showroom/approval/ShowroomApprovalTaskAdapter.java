package cn.iocoder.yudao.module.showroom.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskResultSupport;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalDetail;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalSignatureRecord;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class ShowroomApprovalTaskAdapter implements ApprovalTaskProvider {

    private static final String SOURCE_TASK_TYPE = "SHOWROOM_CHANGE_REQUEST";
    private static final Set<ApprovalTaskViewType> SUPPORTED_VIEWS = Set.of(
            ApprovalTaskViewType.TODO,
            ApprovalTaskViewType.DONE,
            ApprovalTaskViewType.MY_INITIATED
    );
    private static final Set<ApprovalTaskCapability> CAPABILITIES = Set.of(
            ApprovalTaskCapability.TIMELINE,
            ApprovalTaskCapability.NOTIFICATION,
            ApprovalTaskCapability.AUDIT,
            ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
            ApprovalTaskCapability.EVIDENCE_LEDGER
    );

    private final ShowroomWorkflowFacade workflowFacade;

    public ShowroomApprovalTaskAdapter(ShowroomWorkflowFacade workflowFacade) {
        this.workflowFacade = workflowFacade;
    }

    @Override
    public ApprovalModuleCode getModuleCode() {
        return ApprovalModuleCode.SHOWROOM;
    }

    @Override
    public String getModuleName() {
        return "Showroom 审批";
    }

    @Override
    public String getProviderCode() {
        return "showroom-approval";
    }

    @Override
    public String getProviderVersion() {
        return "phase3";
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
        List<ShowroomChangeRequest> rows = switch (context.getViewType()) {
            case TODO -> context.isGlobalView()
                    ? listGlobalPendingApprovals()
                    : workflowFacade.listPendingApprovalsForReviewer(context.getLoginUserId());
            case MY_INITIATED -> context.isGlobalView()
                    ? workflowFacade.listApprovals()
                    : workflowFacade.listApprovals().stream()
                            .filter(request -> Objects.equals(request.submittedBy(), context.getLoginUserId()))
                            .toList();
            case DONE -> context.isGlobalView()
                    ? workflowFacade.listApprovals().stream().filter(ShowroomApprovalTaskAdapter::isHandled).toList()
                    : workflowFacade.listApprovals().stream()
                            .filter(request -> isHandledBy(request, context.getLoginUserId()))
                            .toList();
            default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: SHOWROOM does not support "
                    + context.getViewType());
        };
        List<ApprovalTaskSummary> summaries = rows.stream()
                .filter(request -> matchesKeyword(request, context.getKeyword()))
                .map(this::toSummary)
                .toList();
        return pageRows(summaries, context.getPageNo(), context.getPageSize());
    }

    @Override
    public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
        Long changeRequestId = resolveChangeRequestId(context);
        ShowroomApprovalDetail detail = workflowFacade.getApproval(changeRequestId);
        ShowroomChangeRequest request = detail.changeRequest();
        if (!context.isGlobalView() && !isAccessible(request, context.getLoginUserId())) {
            throw new IllegalStateException("SHOWROOM_APPROVAL_ACCESS_DENIED: 当前用户无权查看该审批轨迹");
        }
        List<ApprovalTaskTimelineEntry> entries = new ArrayList<>();
        entries.add(buildSubmissionEntry(request));
        addSupervisorTimeline(entries, request, detail.signatureRecords());
        addPublicityTimeline(entries, request, detail.signatureRecords());
        return entries;
    }

    private ApprovalTaskSummary toSummary(ShowroomChangeRequest request) {
        if (request.changeRequestId() == null) {
            throw new IllegalStateException("APPROVAL_BUSINESS_KEY_REQUIRED: Showroom change request id is required");
        }
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("changeRequestId", String.valueOf(request.changeRequestId()));
        ApprovalTaskReviewResult approvalResult = resolveApprovalResult(request.status());
        return ApprovalTaskSummary.builder()
                .id("SHOWROOM:" + SOURCE_TASK_TYPE + ":" + request.changeRequestId())
                .moduleCode(ApprovalModuleCode.SHOWROOM)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(request.changeRequestId()))
                .businessKey(String.valueOf(request.changeRequestId()))
                .businessTitle("展厅内容变更 #" + request.changeRequestId())
                .businessCode(request.targetType() + "-" + request.targetId())
                .businessStatus(request.status())
                .currentNodeCode(request.status())
                .currentNodeName(resolveCurrentNodeName(request.status()))
                .initiatorUserId(request.submittedBy())
                .assigneeUserId(resolveAssigneeUserId(request))
                .processInstanceId(request.processInstanceId())
                .initiatedAt(toLocalDateTime(request.submittedAt()))
                .taskCreatedAt(toLocalDateTime(request.submittedAt()))
                .approvalResult(approvalResult)
                .approvalRemark(ApprovalTaskResultSupport.rejectRemark(approvalResult,
                        resolveRejectedComment(request)))
                .requiresSignature(Boolean.TRUE)
                .detailRoute("/showroom/approval")
                .detailQuery(detailQuery)
                .availableActions(Set.of("PROCESS_IN_MODULE"))
                .capabilities(CAPABILITIES)
                .build();
    }

    private static boolean isHandledBy(ShowroomChangeRequest request, Long loginUserId) {
        return Objects.equals(request.supervisorUserId(), loginUserId) && request.supervisorActionAt() != null
                || Objects.equals(request.gaoxinUserId(), loginUserId) && request.gaoxinActionAt() != null;
    }

    private static boolean isHandled(ShowroomChangeRequest request) {
        return request.supervisorActionAt() != null || request.gaoxinActionAt() != null;
    }

    private List<ShowroomChangeRequest> listGlobalPendingApprovals() {
        return workflowFacade.listApprovals().stream()
                .filter(request -> "PENDING_SUPERVISOR_REVIEW".equals(request.status())
                        || "PENDING_GAOXIN_APPROVAL".equals(request.status()))
                .toList();
    }

    private static boolean matchesKeyword(ShowroomChangeRequest request, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String value = keyword.trim();
        return String.valueOf(request.changeRequestId()).contains(value)
                || String.valueOf(request.status()).contains(value)
                || String.valueOf(request.targetType()).contains(value);
    }

    private static boolean isAccessible(ShowroomChangeRequest request, Long loginUserId) {
        if (loginUserId == null) {
            return false;
        }
        return Objects.equals(request.submittedBy(), loginUserId)
                || Objects.equals(request.supervisorUserId(), loginUserId)
                || Objects.equals(request.gaoxinUserId(), loginUserId);
    }

    private static Long resolveChangeRequestId(ApprovalTaskTimelineQueryContext context) {
        if (context.getSourceTaskId() != null && !context.getSourceTaskId().isBlank()) {
            return Long.valueOf(context.getSourceTaskId().trim());
        }
        if (context.getBusinessKey() != null && !context.getBusinessKey().isBlank()) {
            return Long.valueOf(context.getBusinessKey().trim());
        }
        throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: change request id is required");
    }

    private static void addSupervisorTimeline(List<ApprovalTaskTimelineEntry> entries, ShowroomChangeRequest request,
                                              List<ShowroomApprovalSignatureRecord> signatures) {
        if (request.supervisorUserId() == null) {
            return;
        }
        ShowroomApprovalSignatureRecord signature = findSignature(signatures, "SUPERVISOR");
        if (request.supervisorActionAt() == null && "PENDING_SUPERVISOR_REVIEW".equals(request.status())) {
            entries.add(buildPendingEntry(request, "SUPERVISOR", "主管审核", request.supervisorUserId(),
                    toLocalDateTime(request.submittedAt()), "SHOWROOM_PENDING"));
            return;
        }
        String action = resolveSupervisorAction(request.status(), signature);
        entries.add(buildDecisionEntry(request, "SUPERVISOR", "主管审核", request.supervisorUserId(),
                toLocalDateTime(request.supervisorActionAt()), signature, action, "SHOWROOM_SIGNATURE"));
    }

    private static void addPublicityTimeline(List<ApprovalTaskTimelineEntry> entries, ShowroomChangeRequest request,
                                             List<ShowroomApprovalSignatureRecord> signatures) {
        if (request.gaoxinUserId() == null) {
            return;
        }
        ShowroomApprovalSignatureRecord signature = findSignature(signatures, "PUBLICITY");
        if (request.gaoxinActionAt() == null && "PENDING_GAOXIN_APPROVAL".equals(request.status())) {
            entries.add(buildPendingEntry(request, "PUBLICITY", "企宣审批", request.gaoxinUserId(),
                    toLocalDateTime(request.supervisorActionAt()), "SHOWROOM_PENDING"));
            return;
        }
        String action = resolvePublicityAction(request.status(), signature);
        entries.add(buildDecisionEntry(request, "PUBLICITY", "企宣审批", request.gaoxinUserId(),
                toLocalDateTime(request.gaoxinActionAt()), signature, action, "SHOWROOM_SIGNATURE"));
    }

    private static ShowroomApprovalSignatureRecord findSignature(List<ShowroomApprovalSignatureRecord> signatures,
                                                                 String approvalStage) {
        return signatures.stream()
                .filter(signature -> approvalStage.equals(signature.approvalStage()))
                .findFirst()
                .orElse(null);
    }

    private static String resolveSupervisorAction(String status, ShowroomApprovalSignatureRecord signature) {
        if (signature != null) {
            return "REJECT".equalsIgnoreCase(signature.actionType()) ? "REJECTED" : "APPROVED";
        }
        if ("REJECTED".equals(status)) {
            return "REJECTED";
        }
        return "APPROVED";
    }

    private static String resolvePublicityAction(String status, ShowroomApprovalSignatureRecord signature) {
        if ("PUBLISHED".equals(status)) {
            return "PUBLISHED";
        }
        if (signature != null) {
            return "REJECT".equalsIgnoreCase(signature.actionType()) ? "REJECTED" : "APPROVED";
        }
        if ("REJECTED".equals(status)) {
            return "REJECTED";
        }
        return "APPROVED";
    }

    private static ApprovalTaskTimelineEntry buildSubmissionEntry(ShowroomChangeRequest request) {
        return ApprovalTaskTimelineEntry.builder()
                .id("SHOWROOM:" + request.changeRequestId() + ":SUBMITTED")
                .moduleCode(ApprovalModuleCode.SHOWROOM)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(request.changeRequestId()))
                .businessKey(String.valueOf(request.changeRequestId()))
                .nodeCode("SUBMITTED")
                .nodeName("提交审批")
                .action("SUBMITTED")
                .actionLabel("提交审批")
                .actorUserId(request.submittedBy())
                .actedAt(toLocalDateTime(request.submittedAt()))
                .status("PENDING")
                .evidenceType("SHOWROOM_CHANGE_REQUEST")
                .domainReferenceId("showroom_change_request:" + request.changeRequestId())
                .build();
    }

    private static ApprovalTaskTimelineEntry buildPendingEntry(ShowroomChangeRequest request, String nodeCode,
                                                               String nodeName, Long actorUserId,
                                                               LocalDateTime actedAt, String evidenceType) {
        return ApprovalTaskTimelineEntry.builder()
                .id("SHOWROOM:" + request.changeRequestId() + ':' + nodeCode + ":PENDING")
                .moduleCode(ApprovalModuleCode.SHOWROOM)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(request.changeRequestId()))
                .businessKey(String.valueOf(request.changeRequestId()))
                .nodeCode(nodeCode)
                .nodeName(nodeName)
                .action("PENDING")
                .actionLabel("待处理")
                .actorUserId(actorUserId)
                .actedAt(actedAt)
                .status("PENDING")
                .evidenceType(evidenceType)
                .domainReferenceId("showroom_change_request:" + request.changeRequestId())
                .build();
    }

    private static ApprovalTaskTimelineEntry buildDecisionEntry(ShowroomChangeRequest request, String nodeCode,
                                                                String nodeName, Long actorUserId,
                                                                LocalDateTime actedAt,
                                                                ShowroomApprovalSignatureRecord signature,
                                                                String action, String evidenceType) {
        LocalDateTime resolvedActedAt = signature == null ? actedAt : toLocalDateTime(signature.signedAt());
        return ApprovalTaskTimelineEntry.builder()
                .id("SHOWROOM:" + request.changeRequestId() + ':' + nodeCode + ':' + action)
                .moduleCode(ApprovalModuleCode.SHOWROOM)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(request.changeRequestId()))
                .businessKey(String.valueOf(request.changeRequestId()))
                .nodeCode(nodeCode)
                .nodeName(nodeName)
                .action(action)
                .actionLabel(resolveActionLabel(action))
                .actorUserId(signature == null ? actorUserId : signature.actorId())
                .actedAt(resolvedActedAt)
                .comment(signature == null ? null : signature.comment())
                .status(action)
                .evidenceType(evidenceType)
                .domainReferenceId("showroom_change_request_signature:" + (signature == null ? request.changeRequestId() : signature.id()))
                .build();
    }

    private static String resolveActionLabel(String action) {
        return switch (action) {
            case "APPROVED" -> "主管通过";
            case "REJECTED" -> "主管驳回";
            case "PUBLISHED" -> "已发布";
            case "SUBMITTED" -> "提交审批";
            default -> action;
        };
    }

    private static Long resolveAssigneeUserId(ShowroomChangeRequest request) {
        if ("PENDING_SUPERVISOR_REVIEW".equals(request.status())) {
            return request.supervisorUserId();
        }
        if ("PENDING_GAOXIN_APPROVAL".equals(request.status())) {
            return request.gaoxinUserId();
        }
        return null;
    }

    private static ApprovalTaskReviewResult resolveApprovalResult(String status) {
        if ("REJECTED".equals(status)) {
            return ApprovalTaskReviewResult.REJECT;
        }
        if ("PUBLISHED".equals(status) || "APPROVED".equals(status)) {
            return ApprovalTaskReviewResult.APPROVE;
        }
        return null;
    }

    private String resolveRejectedComment(ShowroomChangeRequest request) {
        if (!"REJECTED".equals(request.status()) || request.changeRequestId() == null) {
            return null;
        }
        return workflowFacade.getApproval(request.changeRequestId()).signatureRecords().stream()
                .filter(signature -> "REJECT".equalsIgnoreCase(signature.actionType()))
                .map(ShowroomApprovalSignatureRecord::comment)
                .filter(comment -> comment != null && !comment.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static String resolveCurrentNodeName(String status) {
        if ("PENDING_SUPERVISOR_REVIEW".equals(status)) {
            return "主管审核";
        }
        if ("PENDING_GAOXIN_APPROVAL".equals(status)) {
            return "企宣审批";
        }
        if ("PUBLISHED".equals(status)) {
            return "已发布";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        return status;
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private static PageResult<ApprovalTaskSummary> pageRows(List<ApprovalTaskSummary> rows,
                                                            Integer pageNo, Integer pageSize) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }
}
