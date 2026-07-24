package cn.iocoder.yudao.module.showroom.workflow.service;

import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomApprovalRouteContract;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequestItem;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomVersionAudit;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomWorkflowStart;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShowroomWorkflowService {

    private static final String STATUS_PENDING_SUPERVISOR_REVIEW = "PENDING_SUPERVISOR_REVIEW";
    private static final String STATUS_PENDING_GAOXIN_APPROVAL = "PENDING_GAOXIN_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String ACTION_PUBLISH = "PUBLISH";

    private long changeRequestIdSequence = 1L;
    private final Map<Long, ShowroomChangeRequest> changeRequests = new LinkedHashMap<>();
    private final Map<String, List<ShowroomVersionAudit>> versionAudits = new LinkedHashMap<>();

    public ShowroomChangeRequest submit(ShowroomWorkflowStart start) {
        requireNonNull(start, "SHOWROOM_REQUIRED_FIELD_MISSING: workflow start is required");
        ShowroomApprovalRouteContract.validatePrerequisites(start.submittedBy(), start.submitterDeptId(),
                start.supervisorUserId(), start.gaoxinUserId());
        requireText(start.targetType(), "SHOWROOM_TARGET_NOT_FOUND: target type is required");
        requireNonNull(start.targetId(), "SHOWROOM_TARGET_NOT_FOUND: target id is required");
        requireNonNull(start.targetRevisionId(), "SHOWROOM_TARGET_NOT_FOUND: target revision id is required");
        requireText(start.moduleCode(), "SHOWROOM_REQUIRED_FIELD_MISSING: module code is required");
        requireText(start.submissionSource(), "SHOWROOM_REQUIRED_FIELD_MISSING: submission source is required");
        if (start.items() == null || start.items().isEmpty()) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: change request item is required");
        }
        boolean skipSupervisorStep = ShowroomApprovalRouteContract.shouldSkipSupervisorStep(
                start.submitterDeptId(), start.supervisorUserId());
        ShowroomChangeRequest changeRequest = new ShowroomChangeRequest(changeRequestIdSequence++, start.targetType(),
                start.targetId(), start.targetRevisionId(), start.moduleCode(), start.requestType(),
                start.submissionSource(),
                skipSupervisorStep ? STATUS_PENDING_GAOXIN_APPROVAL : STATUS_PENDING_SUPERVISOR_REVIEW,
                start.processInstanceId(), start.submittedBy(), start.submitterDeptId(), null,
                skipSupervisorStep ? null : start.supervisorUserId(),
                skipSupervisorStep ? null : start.submitterDeptId(), null, start.gaoxinUserId(),
                null, null, start.sourceAssignmentId(), List.copyOf(start.items()));
        changeRequests.put(changeRequest.changeRequestId(), changeRequest);
        return changeRequest;
    }

    public ShowroomChangeRequest supervisorApprove(Long changeRequestId, Long reviewerUserId) {
        ShowroomChangeRequest changeRequest = requireChangeRequest(changeRequestId);
        requireStatus(changeRequest, STATUS_PENDING_SUPERVISOR_REVIEW);
        if (!changeRequest.supervisorUserId().equals(reviewerUserId)) {
            throw new IllegalStateException("SHOWROOM_ROLE_BINDING_MISSING: reviewer is not department supervisor");
        }
        return updateStatus(changeRequest, STATUS_PENDING_GAOXIN_APPROVAL);
    }

    public ShowroomChangeRequest gaoxinApprove(Long changeRequestId, Long reviewerUserId) {
        ShowroomChangeRequest changeRequest = requireChangeRequest(changeRequestId);
        requireStatus(changeRequest, STATUS_PENDING_GAOXIN_APPROVAL);
        if (!changeRequest.gaoxinUserId().equals(reviewerUserId)) {
            throw new IllegalStateException("SHOWROOM_ROLE_BINDING_MISSING: reviewer is not publicity approver");
        }
        return updateStatus(changeRequest, STATUS_APPROVED);
    }

    public ShowroomChangeRequest markPublished(Long changeRequestId, Long operatorId) {
        ShowroomChangeRequest changeRequest = requireChangeRequest(changeRequestId);
        requireNonNull(operatorId, "SHOWROOM_ROLE_BINDING_MISSING: publish operator is required");
        requireStatus(changeRequest, STATUS_APPROVED);
        appendAudits(changeRequest, operatorId);
        return updateStatus(changeRequest, STATUS_PUBLISHED);
    }

    public List<ShowroomVersionAudit> versionAudits(String targetType, Long targetId) {
        return List.copyOf(versionAudits.getOrDefault(targetKey(targetType, targetId), List.of()));
    }

    private ShowroomChangeRequest updateStatus(ShowroomChangeRequest current, String nextStatus) {
        ShowroomChangeRequest updated = current.withStatus(nextStatus);
        changeRequests.put(updated.changeRequestId(), updated);
        return updated;
    }

    private void appendAudits(ShowroomChangeRequest changeRequest, Long operatorId) {
        List<ShowroomVersionAudit> audits = versionAudits.computeIfAbsent(
                targetKey(changeRequest.targetType(), changeRequest.targetId()), key -> new ArrayList<>());
        for (ShowroomChangeRequestItem item : changeRequest.items()) {
            audits.add(new ShowroomVersionAudit(changeRequest.targetType(), changeRequest.targetId(),
                    changeRequest.targetRevisionId(), item.fieldCode(), item.oldValueJson(), item.newValueJson(),
                    operatorId, ACTION_PUBLISH, Instant.now()));
        }
    }

    private ShowroomChangeRequest requireChangeRequest(Long changeRequestId) {
        requireNonNull(changeRequestId, "SHOWROOM_TARGET_NOT_FOUND: change request id is required");
        ShowroomChangeRequest changeRequest = changeRequests.get(changeRequestId);
        if (changeRequest == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: change request not found");
        }
        return changeRequest;
    }

    private static void requireStatus(ShowroomChangeRequest changeRequest, String expectedStatus) {
        if (!expectedStatus.equals(changeRequest.status())) {
            throw new IllegalStateException("SHOWROOM_APPROVAL_ROUTE_MISSING: expected status " + expectedStatus);
        }
    }

    private static String targetKey(String targetType, Long targetId) {
        return targetType + ":" + targetId;
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
