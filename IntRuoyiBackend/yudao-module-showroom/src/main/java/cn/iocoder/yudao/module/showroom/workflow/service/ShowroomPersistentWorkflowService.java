package cn.iocoder.yudao.module.showroom.workflow.service;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestItemDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestSignatureDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestItemMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestSignatureMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomVersionAuditMapper;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomApprovalRouteContract;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldDisplaySupport;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalDetail;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalSignatureRecord;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalTargetPreview;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequestItem;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomWorkflowStart;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShowroomPersistentWorkflowService {

    private static final String STATUS_PENDING_SUPERVISOR_REVIEW = "PENDING_SUPERVISOR_REVIEW";
    private static final String STATUS_PENDING_GAOXIN_APPROVAL = "PENDING_GAOXIN_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String ITEM_STATUS_PENDING = "PENDING";
    private static final String ITEM_STATUS_APPROVED = "APPROVED";
    private static final String ITEM_STATUS_REJECTED = "REJECTED";
    private static final String TARGET_COMPANY = "COMPANY";
    private static final String TARGET_PRODUCT = "PRODUCT";

    private final ShowroomChangeRequestMapper changeRequestMapper;
    private final ShowroomChangeRequestItemMapper changeRequestItemMapper;
    private final ShowroomChangeRequestSignatureMapper changeRequestSignatureMapper;
    private final ShowroomVersionAuditMapper versionAuditMapper;
    private final ShowroomPersistentContentService contentService;

    public ShowroomPersistentWorkflowService(ShowroomChangeRequestMapper changeRequestMapper,
                                             ShowroomChangeRequestItemMapper changeRequestItemMapper,
                                             ShowroomChangeRequestSignatureMapper changeRequestSignatureMapper,
                                             ShowroomVersionAuditMapper versionAuditMapper,
                                             ShowroomPersistentContentService contentService) {
        this.changeRequestMapper = changeRequestMapper;
        this.changeRequestItemMapper = changeRequestItemMapper;
        this.changeRequestSignatureMapper = changeRequestSignatureMapper;
        this.versionAuditMapper = versionAuditMapper;
        this.contentService = contentService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest submit(ShowroomWorkflowStart start) {
        requireNonNull(start, "SHOWROOM_REQUIRED_FIELD_MISSING: workflow start is required");
        ShowroomApprovalRouteContract.validatePrerequisites(start.submittedBy(), start.submitterDeptId(),
                start.supervisorUserId(), start.gaoxinUserId());
        requireText(start.targetType(), "SHOWROOM_TARGET_NOT_FOUND: target type is required");
        requireNonNull(start.targetId(), "SHOWROOM_TARGET_NOT_FOUND: target id is required");
        requireNonNull(start.targetRevisionId(), "SHOWROOM_TARGET_NOT_FOUND: target revision id is required");
        requireText(start.moduleCode(), "SHOWROOM_REQUIRED_FIELD_MISSING: module code is required");
        requireText(start.requestType(), "SHOWROOM_REQUIRED_FIELD_MISSING: request type is required");
        requireText(start.submissionSource(), "SHOWROOM_REQUIRED_FIELD_MISSING: submission source is required");
        requireNonNull(start.submittedBy(), "SHOWROOM_ROLE_BINDING_MISSING: submitter is required");
        List<ShowroomChangeRequestItem> items = requireItems(start.items());
        boolean skipSupervisorStep = ShowroomApprovalRouteContract.shouldSkipSupervisorStep(
                start.submitterDeptId(), start.supervisorUserId());

        LocalDateTime now = LocalDateTime.now();
        ShowroomChangeRequestDO request = ShowroomChangeRequestDO.builder()
                .targetType(start.targetType())
                .targetId(start.targetId())
                .targetRevisionId(start.targetRevisionId())
                .moduleCode(start.moduleCode())
                .requestType(start.requestType())
                .submissionSource(start.submissionSource())
                .status(skipSupervisorStep ? STATUS_PENDING_GAOXIN_APPROVAL : STATUS_PENDING_SUPERVISOR_REVIEW)
                .processInstanceId(nullableText(start.processInstanceId()))
                .submittedBy(start.submittedBy())
                .submitterDeptId(start.submitterDeptId())
                .submittedAt(now)
                // User-approved showroom-specific branch: no department means skip supervisor and submit directly to publicity approval.
                .supervisorUserId(skipSupervisorStep ? null : start.supervisorUserId())
                .supervisorDeptId(skipSupervisorStep ? null : start.submitterDeptId())
                .gaoxinUserId(start.gaoxinUserId())
                .sourceAssignmentId(start.sourceAssignmentId())
                .build();
        changeRequestMapper.insert(request);
        for (ShowroomChangeRequestItem item : items) {
            changeRequestItemMapper.insert(ShowroomChangeRequestItemDO.builder()
                    .changeRequestId(request.getId())
                    .fieldCode(requireText(item.fieldCode(),
                            "SHOWROOM_REQUIRED_FIELD_MISSING: change request field code is required"))
                    .oldValueJson(item.oldValueJson())
                    .newValueJson(item.newValueJson())
                    .approvalStatus(nullableText(item.approvalStatus()) == null
                            ? ITEM_STATUS_PENDING : item.approvalStatus())
                    .approvedBy(item.approvedBy())
                    .approvedAt(toLocalDateTime(item.approvedAt()))
                    .comment(item.comment())
                    .build());
        }
        return getChangeRequest(request.getId());
    }

    public List<ShowroomChangeRequest> listChangeRequests() {
        return changeRequestMapper.selectListOrdered().stream()
                .map(this::toChangeRequest)
                .toList();
    }

    public ShowroomChangeRequest getChangeRequest(Long changeRequestId) {
        return toChangeRequest(requireChangeRequestDO(changeRequestId));
    }

    public ShowroomApprovalDetail getApprovalDetail(Long changeRequestId) {
        ShowroomChangeRequest request = getChangeRequest(changeRequestId);
        return new ShowroomApprovalDetail(request, request.items(),
                buildTargetPreview(request.targetType(), request.targetId(), request.targetRevisionId()),
                contentService.versionAudits(request.targetType(), request.targetId()),
                changeRequestSignatureMapper == null
                        ? List.of()
                        : changeRequestSignatureMapper.selectListByChangeRequestId(changeRequestId).stream()
                        .map(this::toSignatureRecord)
                        .toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest attachProcessInstance(Long changeRequestId, String processInstanceId) {
        requireText(processInstanceId, "SHOWROOM_REQUIRED_FIELD_MISSING: process instance id is required");
        ShowroomChangeRequestDO request = requireChangeRequestDO(changeRequestId);
        request.setProcessInstanceId(processInstanceId);
        changeRequestMapper.updateById(request);
        return getChangeRequest(changeRequestId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest supervisorApprove(Long changeRequestId, Long reviewerUserId) {
        ShowroomChangeRequestDO request = requireChangeRequestDO(changeRequestId);
        requireStatus(request.getStatus(), STATUS_PENDING_SUPERVISOR_REVIEW);
        requireReviewer(request.getSupervisorUserId(), reviewerUserId,
                "SHOWROOM_ROLE_BINDING_MISSING: reviewer is not department supervisor");
        request.setStatus(STATUS_PENDING_GAOXIN_APPROVAL);
        request.setSupervisorActionAt(LocalDateTime.now());
        changeRequestMapper.updateById(request);
        return getChangeRequest(changeRequestId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest supervisorReject(Long changeRequestId, Long reviewerUserId, String reason) {
        ShowroomChangeRequestDO request = requireChangeRequestDO(changeRequestId);
        requireStatus(request.getStatus(), STATUS_PENDING_SUPERVISOR_REVIEW);
        requireReviewer(request.getSupervisorUserId(), reviewerUserId,
                "SHOWROOM_ROLE_BINDING_MISSING: reviewer is not department supervisor");
        updateRejectedRequest(request, reviewerUserId, reason, true);
        return getChangeRequest(changeRequestId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest gaoxinApprove(Long changeRequestId, Long reviewerUserId) {
        ShowroomChangeRequestDO request = requireChangeRequestDO(changeRequestId);
        requireStatus(request.getStatus(), STATUS_PENDING_GAOXIN_APPROVAL);
        requireReviewer(request.getGaoxinUserId(), reviewerUserId,
                "SHOWROOM_ROLE_BINDING_MISSING: reviewer is not publicity approver");
        LocalDateTime now = LocalDateTime.now();
        request.setStatus(STATUS_APPROVED);
        request.setGaoxinActionAt(now);
        changeRequestMapper.updateById(request);
        updateItemDecision(changeRequestId, ITEM_STATUS_APPROVED, reviewerUserId, now, null);
        return getChangeRequest(changeRequestId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest gaoxinReject(Long changeRequestId, Long reviewerUserId, String reason) {
        ShowroomChangeRequestDO request = requireChangeRequestDO(changeRequestId);
        requireStatus(request.getStatus(), STATUS_PENDING_GAOXIN_APPROVAL);
        requireReviewer(request.getGaoxinUserId(), reviewerUserId,
                "SHOWROOM_ROLE_BINDING_MISSING: reviewer is not publicity approver");
        updateRejectedRequest(request, reviewerUserId, reason, false);
        return getChangeRequest(changeRequestId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomChangeRequest markPublished(Long changeRequestId, Long operatorId) {
        requireNonNull(operatorId, "SHOWROOM_ROLE_BINDING_MISSING: publish operator is required");
        ShowroomChangeRequestDO request = requireChangeRequestDO(changeRequestId);
        requireStatus(request.getStatus(), STATUS_APPROVED);
        request.setStatus(STATUS_PUBLISHED);
        changeRequestMapper.updateById(request);
        return getChangeRequest(changeRequestId);
    }

    private void updateRejectedRequest(ShowroomChangeRequestDO request, Long reviewerUserId, String reason,
                                       boolean supervisorStage) {
        LocalDateTime now = LocalDateTime.now();
        request.setStatus(STATUS_REJECTED);
        request.setRejectionReason(requireText(reason, "SHOWROOM_REQUIRED_FIELD_MISSING: rejection reason is required"));
        if (supervisorStage) {
            request.setSupervisorActionAt(now);
        } else {
            request.setGaoxinActionAt(now);
        }
        changeRequestMapper.updateById(request);
        updateItemDecision(request.getId(), ITEM_STATUS_REJECTED, reviewerUserId, now, reason);
    }

    private void updateItemDecision(Long changeRequestId, String approvalStatus, Long approvedBy,
                                    LocalDateTime approvedAt, String comment) {
        changeRequestItemMapper.update(null, new LambdaUpdateWrapper<ShowroomChangeRequestItemDO>()
                .eq(ShowroomChangeRequestItemDO::getChangeRequestId, changeRequestId)
                .set(ShowroomChangeRequestItemDO::getApprovalStatus, approvalStatus)
                .set(ShowroomChangeRequestItemDO::getApprovedBy, approvedBy)
                .set(ShowroomChangeRequestItemDO::getApprovedAt, approvedAt)
                .set(ShowroomChangeRequestItemDO::getComment, comment));
    }

    private ShowroomApprovalTargetPreview buildTargetPreview(String targetType, Long targetId, Long targetRevisionId) {
        if (TARGET_COMPANY.equals(targetType)) {
            ShowroomCompanySnapshot snapshot = contentService.getCompany(targetId);
            Long liveRevisionId = snapshot.currentRevisionId().orElse(null);
            Map<String, String> liveFields = liveRevisionId == null ? Map.of()
                    : contentService.getCompanyRevision(liveRevisionId).fields();
            Map<String, String> targetFields = contentService.getCompanyRevision(targetRevisionId).fields();
            return new ShowroomApprovalTargetPreview(targetType, targetId, liveRevisionId, targetRevisionId,
                    liveFields, targetFields,
                    ShowroomFieldDisplaySupport.buildPreviewRows(targetType, liveFields, targetFields, contentService));
        }
        if (TARGET_PRODUCT.equals(targetType)) {
            ShowroomProductSnapshot snapshot = contentService.getProduct(targetId);
            Long liveRevisionId = snapshot.currentRevisionId().orElse(null);
            Map<String, String> liveFields = liveRevisionId == null ? Map.of()
                    : toProductFields(contentService.getProductRevision(liveRevisionId));
            Map<String, String> targetFields = toProductFields(contentService.getProductRevision(targetRevisionId));
            return new ShowroomApprovalTargetPreview(targetType, targetId, liveRevisionId, targetRevisionId,
                    liveFields, targetFields,
                    ShowroomFieldDisplaySupport.buildPreviewRows(targetType, liveFields, targetFields, contentService));
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported approval target " + targetType);
    }

    private ShowroomChangeRequestDO requireChangeRequestDO(Long changeRequestId) {
        requireNonNull(changeRequestId, "SHOWROOM_TARGET_NOT_FOUND: change request id is required");
        ShowroomChangeRequestDO request = changeRequestMapper.selectById(changeRequestId);
        if (request == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: change request not found");
        }
        return request;
    }

    private ShowroomChangeRequest toChangeRequest(ShowroomChangeRequestDO request) {
        List<ShowroomChangeRequestItem> items = changeRequestItemMapper.selectListByChangeRequestId(request.getId()).stream()
                .map(item -> toChangeRequestItem(request.getTargetType(), item))
                .toList();
        return new ShowroomChangeRequest(request.getId(), request.getTargetType(), request.getTargetId(),
                request.getTargetRevisionId(), request.getModuleCode(), request.getRequestType(),
                request.getSubmissionSource(), request.getStatus(), request.getProcessInstanceId(),
                request.getSubmittedBy(), request.getSubmitterDeptId(), toInstant(request.getSubmittedAt()),
                request.getSupervisorUserId(), request.getSupervisorDeptId(),
                toInstant(request.getSupervisorActionAt()), request.getGaoxinUserId(),
                toInstant(request.getGaoxinActionAt()), request.getRejectionReason(),
                request.getSourceAssignmentId(), items);
    }

    private ShowroomChangeRequestItem toChangeRequestItem(String targetType, ShowroomChangeRequestItemDO item) {
        return new ShowroomChangeRequestItem(item.getFieldCode(), item.getOldValueJson(), item.getNewValueJson(),
                item.getApprovalStatus(), item.getApprovedBy(), toInstant(item.getApprovedAt()), item.getComment())
                .withDisplay(
                        ShowroomFieldDisplaySupport.fieldLabel(targetType, item.getFieldCode()),
                        ShowroomFieldDisplaySupport.formatJsonWrappedFieldValue(
                                targetType, item.getFieldCode(), item.getOldValueJson(), contentService),
                        ShowroomFieldDisplaySupport.formatJsonWrappedFieldValue(
                                targetType, item.getFieldCode(), item.getNewValueJson(), contentService)
                );
    }

    private ShowroomApprovalSignatureRecord toSignatureRecord(ShowroomChangeRequestSignatureDO record) {
        return new ShowroomApprovalSignatureRecord(record.getId(), record.getChangeRequestId(),
                record.getApprovalStage(), record.getActionType(), record.getActorId(),
                record.getSignatureMode(), record.getPasswordVerified(), record.getComment(),
                toInstant(record.getSignedAt()));
    }

    private static Map<String, String> toProductFields(ShowroomProductRevision revision) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>(revision.fields());
        fields.put("name_cn", revision.nameCn());
        fields.put("name_en", revision.nameEn());
        return fields;
    }

    private static List<ShowroomChangeRequestItem> requireItems(List<ShowroomChangeRequestItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: change request item is required");
        }
        return List.copyOf(items);
    }

    private static void requireReviewer(Long expectedUserId, Long reviewerUserId, String message) {
        requireNonNull(reviewerUserId, message);
        if (!reviewerUserId.equals(expectedUserId)) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireStatus(String actualStatus, String expectedStatus) {
        if (!expectedStatus.equals(actualStatus)) {
            throw new IllegalStateException("SHOWROOM_APPROVAL_ROUTE_MISSING: expected status " + expectedStatus);
        }
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

    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

}
