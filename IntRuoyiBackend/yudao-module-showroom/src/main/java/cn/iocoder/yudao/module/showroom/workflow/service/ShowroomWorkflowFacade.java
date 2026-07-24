package cn.iocoder.yudao.module.showroom.workflow.service;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldCatalog;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalDetail;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequestItem;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomWorkflowStart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ShowroomWorkflowFacade {

    private static final String REQUEST_TYPE_CONTENT_UPDATE = "CONTENT_UPDATE";
    private static final String SUBMISSION_SOURCE_MANUAL = "MANUAL";
    private static final String TARGET_COMPANY = "COMPANY";
    private static final String TARGET_PRODUCT = "PRODUCT";
    private static final Set<String> COMPANY_FIELDS = Set.of(
            "development_history",
            "park_introduction",
            "incubation_platform",
            "subsidiary_overview",
            "stock_info",
            "core_manufacturing_capability",
            "honors_awards"
    );

    private final ShowroomPersistentContentService contentService;
    private final ShowroomPersistentWorkflowService workflowService;
    private final ShowroomAssignmentService assignmentService;
    private final ShowroomApprovalSignatureService approvalSignatureService;
    private final ShowroomWorkflowNotifyService workflowNotifyService;
    private final ShowroomVersionBundleService versionBundleService;

    public ShowroomWorkflowFacade(ShowroomPersistentContentService contentService,
                                  ShowroomPersistentWorkflowService workflowService,
                                  ShowroomAssignmentService assignmentService,
                                  ShowroomApprovalSignatureService approvalSignatureService,
                                  ShowroomWorkflowNotifyService workflowNotifyService,
                                  ShowroomVersionBundleService versionBundleService) {
        this.contentService = contentService;
        this.workflowService = workflowService;
        this.assignmentService = assignmentService;
        this.approvalSignatureService = approvalSignatureService;
        this.workflowNotifyService = workflowNotifyService;
        this.versionBundleService = versionBundleService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest submit(String targetType, Long targetId, Long targetRevisionId,
                                        List<String> fieldCodes, String moduleCode, Long submittedBy,
                                        Long submitterDeptId, Long supervisorUserId, Long gaoxinUserId) {
        requireText(targetType, "SHOWROOM_TARGET_NOT_FOUND: target type is required");
        requireNonNull(targetId, "SHOWROOM_TARGET_NOT_FOUND: target id is required");
        requireNonNull(targetRevisionId, "SHOWROOM_TARGET_NOT_FOUND: target revision id is required");
        Map<String, String> liveFields = currentFields(targetType, targetId);
        Map<String, String> targetFields = revisionFields(targetType, targetId, targetRevisionId);
        List<ShowroomChangeRequestItem> items = resolveFieldCodes(fieldCodes, liveFields, targetFields).stream()
                .map(fieldCode -> {
                    validateFieldCode(targetType, fieldCode);
                    return new ShowroomChangeRequestItem(fieldCode, jsonValue(liveFields.get(fieldCode)),
                            jsonValue(targetFields.get(fieldCode)));
                })
                .toList();
        ShowroomChangeRequest changeRequest = workflowService.submit(new ShowroomWorkflowStart(targetType, targetId, targetRevisionId, moduleCode,
                REQUEST_TYPE_CONTENT_UPDATE, SUBMISSION_SOURCE_MANUAL, null, submittedBy, submitterDeptId,
                supervisorUserId, gaoxinUserId, null, items));
        if (TARGET_PRODUCT.equals(targetType)) {
            assignmentService.markWholeProductAssignmentSubmitted(targetId, submittedBy, targetRevisionId,
                    changeRequest.changeRequestId());
        }
        workflowNotifyService.notifyPendingApproval(changeRequest);
        return changeRequest;
    }

    public List<ShowroomChangeRequest> listApprovals() {
        return workflowService.listChangeRequests();
    }

    public List<ShowroomChangeRequest> listPendingApprovalsForReviewer(Long reviewerUserId) {
        requireNonNull(reviewerUserId, "SHOWROOM_APPROVAL_ACCESS_DENIED: 当前登录用户不存在，无法查看审批");
        return workflowService.listChangeRequests().stream()
                .filter(changeRequest -> isPendingForReviewer(changeRequest, reviewerUserId))
                .toList();
    }

    public ShowroomApprovalDetail getApproval(Long changeRequestId) {
        return workflowService.getApprovalDetail(changeRequestId);
    }

    public ShowroomApprovalDetail getApprovalForReviewer(Long changeRequestId, Long reviewerUserId) {
        requireNonNull(reviewerUserId, "SHOWROOM_APPROVAL_ACCESS_DENIED: 当前登录用户不存在，无法查看审批");
        ShowroomChangeRequest changeRequest = workflowService.getChangeRequest(changeRequestId);
        if (!isPendingForReviewer(changeRequest, reviewerUserId)) {
            throw new IllegalStateException("SHOWROOM_APPROVAL_ACCESS_DENIED: 当前用户只能查看分配给自己的审批");
        }
        return workflowService.getApprovalDetail(changeRequestId);
    }

    public ShowroomApprovalDetail getApprovalForParticipant(Long changeRequestId, Long participantUserId) {
        requireNonNull(participantUserId, "SHOWROOM_APPROVAL_ACCESS_DENIED: 当前登录用户不存在，无法查看审批");
        ShowroomChangeRequest changeRequest = workflowService.getChangeRequest(changeRequestId);
        if (!isApprovalParticipant(changeRequest, participantUserId)) {
            throw new IllegalStateException("SHOWROOM_APPROVAL_ACCESS_DENIED: 当前用户不是该审批单参与人，无法查看审批");
        }
        return workflowService.getApprovalDetail(changeRequestId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest supervisorApprove(Long changeRequestId, Long reviewerUserId,
                                                   String password, String comment) {
        approvalSignatureService.recordSignedDecision(changeRequestId,
                ShowroomApprovalSignatureService.APPROVAL_STAGE_SUPERVISOR,
                ShowroomApprovalSignatureService.ACTION_APPROVE,
                reviewerUserId, password, comment);
        ShowroomChangeRequest approved = workflowService.supervisorApprove(changeRequestId, reviewerUserId);
        workflowNotifyService.notifyPendingApproval(approved);
        return approved;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest supervisorReject(Long changeRequestId, Long reviewerUserId,
                                                  String password, String reason) {
        approvalSignatureService.recordSignedDecision(changeRequestId,
                ShowroomApprovalSignatureService.APPROVAL_STAGE_SUPERVISOR,
                ShowroomApprovalSignatureService.ACTION_REJECT,
                reviewerUserId, password, reason);
        ShowroomChangeRequest rejected = workflowService.supervisorReject(changeRequestId, reviewerUserId, reason);
        assignmentService.reopenWholeProductAssignmentForRejectedChangeRequest(changeRequestId);
        workflowNotifyService.notifyRejected(rejected, "主管审核");
        return rejected;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest gaoxinApproveAndPublish(Long changeRequestId, Long reviewerUserId,
                                                         String password, String comment) {
        approvalSignatureService.recordSignedDecision(changeRequestId,
                ShowroomApprovalSignatureService.APPROVAL_STAGE_PUBLICITY,
                ShowroomApprovalSignatureService.ACTION_APPROVE,
                reviewerUserId, password, comment);
        ShowroomChangeRequest approved = workflowService.gaoxinApprove(changeRequestId, reviewerUserId);
        if (TARGET_COMPANY.equals(approved.targetType())) {
            contentService.publishCompanyRevision(approved.targetRevisionId(), reviewerUserId);
        } else if (TARGET_PRODUCT.equals(approved.targetType())) {
            contentService.publishProductRevision(approved.targetRevisionId(), reviewerUserId);
            versionBundleService.ensureBundleForPublishedRevision(TARGET_PRODUCT, approved.targetId(),
                    approved.targetRevisionId(), reviewerUserId, null);
        } else {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported approval target "
                    + approved.targetType());
        }
        ShowroomChangeRequest published = workflowService.markPublished(changeRequestId, reviewerUserId);
        workflowNotifyService.notifyPublished(published);
        return published;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest gaoxinReject(Long changeRequestId, Long reviewerUserId,
                                              String password, String reason) {
        approvalSignatureService.recordSignedDecision(changeRequestId,
                ShowroomApprovalSignatureService.APPROVAL_STAGE_PUBLICITY,
                ShowroomApprovalSignatureService.ACTION_REJECT,
                reviewerUserId, password, reason);
        ShowroomChangeRequest rejected = workflowService.gaoxinReject(changeRequestId, reviewerUserId, reason);
        assignmentService.reopenWholeProductAssignmentForRejectedChangeRequest(changeRequestId);
        workflowNotifyService.notifyRejected(rejected, "企宣审批");
        return rejected;
    }

    private Map<String, String> currentFields(String targetType, Long targetId) {
        if (TARGET_COMPANY.equals(targetType)) {
            ShowroomCompanySnapshot company = contentService.getCompany(targetId);
            return company.currentRevisionId().map(contentService::getCompanyRevision)
                    .map(revision -> new LinkedHashMap<>(revision.fields()))
                    .orElseGet(LinkedHashMap::new);
        }
        if (TARGET_PRODUCT.equals(targetType)) {
            ShowroomProductSnapshot product = contentService.getProduct(targetId);
            return product.currentRevisionId().map(contentService::getProductRevision)
                    .map(ShowroomWorkflowFacade::productFields)
                    .orElseGet(LinkedHashMap::new);
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported approval target " + targetType);
    }

    private Map<String, String> revisionFields(String targetType, Long targetId, Long targetRevisionId) {
        requireNonNull(targetId, "SHOWROOM_TARGET_NOT_FOUND: target id is required");
        if (TARGET_COMPANY.equals(targetType)) {
            return new LinkedHashMap<>(contentService.getCompanyRevision(targetRevisionId).fields());
        }
        if (TARGET_PRODUCT.equals(targetType)) {
            return productFields(contentService.getProductRevision(targetRevisionId));
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported approval target " + targetType);
    }

    private static LinkedHashMap<String, String> productFields(ShowroomProductRevision revision) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>(revision.fields());
        fields.put("name_cn", revision.nameCn());
        fields.put("name_en", revision.nameEn());
        return fields;
    }

    private static void validateFieldCode(String targetType, String fieldCode) {
        requireText(fieldCode, "SHOWROOM_REQUIRED_FIELD_MISSING: field code is required");
        if (TARGET_PRODUCT.equals(targetType)) {
            ShowroomFieldCatalog.productField(fieldCode);
            return;
        }
        if (TARGET_COMPANY.equals(targetType) && COMPANY_FIELDS.contains(fieldCode)) {
            return;
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported approval field " + fieldCode);
    }

    private static List<String> resolveFieldCodes(List<String> fieldCodes, Map<String, String> liveFields,
                                                  Map<String, String> targetFields) {
        if (fieldCodes != null && !fieldCodes.isEmpty()) {
            return List.copyOf(fieldCodes);
        }
        LinkedHashSet<String> candidateFields = new LinkedHashSet<>();
        candidateFields.addAll(targetFields.keySet());
        candidateFields.addAll(liveFields.keySet());
        List<String> changedFieldCodes = candidateFields.stream()
                .filter(fieldCode -> !Objects.equals(liveFields.get(fieldCode), targetFields.get(fieldCode)))
                .toList();
        if (changedFieldCodes.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: field codes are required");
        }
        return changedFieldCodes;
    }

    private static boolean isPendingForReviewer(ShowroomChangeRequest changeRequest, Long reviewerUserId) {
        if ("PENDING_SUPERVISOR_REVIEW".equals(changeRequest.status())) {
            return Objects.equals(changeRequest.supervisorUserId(), reviewerUserId);
        }
        if ("PENDING_GAOXIN_APPROVAL".equals(changeRequest.status())) {
            return Objects.equals(changeRequest.gaoxinUserId(), reviewerUserId);
        }
        return false;
    }

    private static boolean isApprovalParticipant(ShowroomChangeRequest changeRequest, Long participantUserId) {
        return Objects.equals(changeRequest.submittedBy(), participantUserId)
                || Objects.equals(changeRequest.supervisorUserId(), participantUserId)
                || Objects.equals(changeRequest.gaoxinUserId(), participantUserId);
    }

    private static String jsonValue(String value) {
        if (value == null) {
            return null;
        }
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        payload.put("value", value);
        return JsonUtils.toJsonString(payload);
    }

    private static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

}
