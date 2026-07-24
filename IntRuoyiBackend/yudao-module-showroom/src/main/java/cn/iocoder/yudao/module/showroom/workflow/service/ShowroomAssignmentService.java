package cn.iocoder.yudao.module.showroom.workflow.service;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomFieldAssignmentDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomFieldAssignmentMapper;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomRoleModelContract;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldCatalog;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomAssignmentCreate;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomAssignmentDetail;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomAssignmentSubmitResult;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequestItem;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomFieldAssignment;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomWorkflowStart;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ShowroomAssignmentService {

    public static final String PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE = "__PRODUCT_ALL_FIELDS__";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_AUTO_SUBMITTED = "AUTO_SUBMITTED";
    private static final String TARGET_PRODUCT = "PRODUCT";
    private static final String TARGET_COMPANY = "COMPANY";
    private static final String ASSIGNMENT_TEMPLATE_CODE = "SHOWROOM_ASSIGNMENT";
    private static final Set<String> COMPANY_EDITABLE_FIELDS = Set.of(
            "development_history",
            "park_introduction",
            "incubation_platform",
            "subsidiary_overview",
            "stock_info",
            "core_manufacturing_capability",
            "honors_awards"
    );

    private final ShowroomFieldAssignmentMapper assignmentMapper;
    private final ShowroomPersistentWorkflowService workflowService;
    private final ShowroomApprovalActorResolver approvalActorResolver;
    private final ShowroomPersistentContentService contentService;
    private final AdminUserMapper adminUserMapper;
    private final DeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final NotifyMessageMapper notifyMessageMapper;
    private final NotifyMessageSendApi notifyMessageSendApi;

    public ShowroomAssignmentService(ShowroomFieldAssignmentMapper assignmentMapper,
                                     ShowroomPersistentWorkflowService workflowService,
                                     ShowroomApprovalActorResolver approvalActorResolver,
                                     ShowroomPersistentContentService contentService,
                                     AdminUserMapper adminUserMapper,
                                     DeptMapper deptMapper,
                                     RoleMapper roleMapper,
                                     UserRoleMapper userRoleMapper,
                                     NotifyMessageMapper notifyMessageMapper,
                                     NotifyMessageSendApi notifyMessageSendApi) {
        this.assignmentMapper = assignmentMapper;
        this.workflowService = workflowService;
        this.approvalActorResolver = approvalActorResolver;
        this.contentService = contentService;
        this.adminUserMapper = adminUserMapper;
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.notifyMessageMapper = notifyMessageMapper;
        this.notifyMessageSendApi = notifyMessageSendApi;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomFieldAssignment createAssignment(ShowroomAssignmentCreate create) {
        requireNonNull(create, "SHOWROOM_REQUIRED_FIELD_MISSING: assignment create payload is required");
        validateEditableField(create.targetType(), create.fieldCode());
        requireNonNull(create.targetId(), "SHOWROOM_TARGET_NOT_FOUND: assignment target id is required");
        requireNonNull(create.assigneeUserId(), "SHOWROOM_ROLE_BINDING_MISSING: assignment assignee is required");
        requireNonNull(create.assignedBy(), "SHOWROOM_ROLE_BINDING_MISSING: assignment creator is required");
        requireEditorAssignee(create.assigneeUserId());
        requireTargetExists(create.targetType(), create.targetId());
        ensureNoOpenWholeProductAssignmentConflict(create.targetType(), create.targetId(), create.fieldCode());

        Long notifyMessageId = sendAssignmentNotify(create);
        LocalDateTime now = LocalDateTime.now();
        ShowroomFieldAssignmentDO assignment = ShowroomFieldAssignmentDO.builder()
                .targetType(create.targetType())
                .targetId(create.targetId())
                .fieldCode(create.fieldCode())
                .assigneeUserId(create.assigneeUserId())
                .assignedBy(create.assignedBy())
                .status(STATUS_OPEN)
                .notifyMessageId(notifyMessageId)
                .createdAt(now)
                .build();
        assignment.setTenantId(TenantContextHolder.getRequiredTenantId());
        assignmentMapper.insert(assignment);
        return toAssignment(assignment);
    }

    public ShowroomAssignmentDetail getLatestOpenWholeProductAssignmentDetail(Long productId) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        ShowroomFieldAssignmentDO assignment = assignmentMapper.selectLatestOpenByTargetAndField(
                TARGET_PRODUCT, productId, PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE);
        return assignment == null ? null : toDetail(assignment);
    }

    public ShowroomFieldAssignment getLatestOpenWholeProductAssignment(Long productId) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        ShowroomFieldAssignmentDO assignment = assignmentMapper.selectLatestOpenByTargetAndField(
                TARGET_PRODUCT, productId, PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE);
        return assignment == null ? null : toAssignment(assignment);
    }

    public ShowroomAssignmentDetail getAssignment(Long assignmentId) {
        return toDetail(requireAssignment(assignmentId));
    }

    public List<Long> listAssignedProductIdsForAssignee(Long assigneeUserId) {
        requireNonNull(assigneeUserId, "SHOWROOM_ROLE_BINDING_MISSING: assignment assignee is required");
        return listLatestWholeProductAssignments().stream()
                .filter(assignment -> assigneeUserId.equals(assignment.getAssigneeUserId()))
                .map(ShowroomFieldAssignmentDO::getTargetId)
                .distinct()
                .toList();
    }

    public List<ShowroomAssignmentDetail> pageAssignments(String targetType, Long targetId, Long assigneeUserId,
                                                          String status, Integer pageNo, Integer pageSize) {
        List<ShowroomFieldAssignmentDO> assignments = assignmentMapper.selectList().stream()
                .filter(this::matchesCurrentTenant)
                .filter(assignment -> targetType == null || targetType.equals(assignment.getTargetType()))
                .filter(assignment -> targetId == null || targetId.equals(assignment.getTargetId()))
                .filter(assignment -> assigneeUserId == null || assigneeUserId.equals(assignment.getAssigneeUserId()))
                .filter(assignment -> status == null || status.equals(assignment.getStatus()))
                .sorted((left, right) -> {
                    LocalDateTime leftCreatedAt = left.getCreatedAt();
                    LocalDateTime rightCreatedAt = right.getCreatedAt();
                    if (leftCreatedAt == null && rightCreatedAt == null) {
                        return Long.compare(right.getId(), left.getId());
                    }
                    if (leftCreatedAt == null) {
                        return 1;
                    }
                    if (rightCreatedAt == null) {
                        return -1;
                    }
                    int compare = rightCreatedAt.compareTo(leftCreatedAt);
                    return compare != 0 ? compare : Long.compare(right.getId(), left.getId());
                })
                .toList();
        return page(assignments, pageNo, pageSize).stream()
                .map(this::toDetail)
                .toList();
    }

    public List<Long> listOpenProductIdsForAssignee(Long assigneeUserId) {
        requireNonNull(assigneeUserId, "SHOWROOM_ROLE_BINDING_MISSING: assignment assignee is required");
        return listLatestWholeProductAssignments().stream()
                .filter(assignment -> STATUS_OPEN.equals(assignment.getStatus()))
                .filter(assignment -> assigneeUserId.equals(assignment.getAssigneeUserId()))
                .map(ShowroomFieldAssignmentDO::getTargetId)
                .distinct()
                .toList();
    }

    public List<Long> listVisibleProductIdsForUser(Long operatorUserId) {
        requireNonNull(operatorUserId, "SHOWROOM_ROLE_BINDING_MISSING: visible product operator is required");
        LinkedHashSet<Long> visibleProductIds = new LinkedHashSet<>(listAssignedProductIdsForAssignee(operatorUserId));
        Map<Long, ShowroomFieldAssignmentDO> latestAssignments = latestWholeProductAssignmentsByProductId();
        latestAssignments.values().forEach(assignment -> {
            Long supervisorUserId = resolveDepartmentLeaderUserId(assignment.getAssigneeUserId());
            if (Objects.equals(supervisorUserId, operatorUserId)) {
                visibleProductIds.add(assignment.getTargetId());
            }
        });
        latestProductChangeRequestsByProductId().forEach((productId, request) -> {
            ShowroomFieldAssignmentDO latestAssignment = latestAssignments.get(productId);
            if (!shouldCountChangeRequestVisibility(request, latestAssignment)) {
                return;
            }
            if (Objects.equals(request.submittedBy(), operatorUserId)
                    || Objects.equals(request.supervisorUserId(), operatorUserId)
                    || Objects.equals(request.gaoxinUserId(), operatorUserId)) {
                visibleProductIds.add(productId);
            }
        });
        return List.copyOf(visibleProductIds);
    }

    public boolean hasAssignedProductHistory(Long assigneeUserId, Long productId) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        return listAssignedProductIdsForAssignee(assigneeUserId).stream()
                .anyMatch(targetId -> targetId.equals(productId));
    }

    public boolean hasVisibleProductAccess(Long operatorUserId, Long productId) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        return listVisibleProductIdsForUser(operatorUserId).stream()
                .anyMatch(targetId -> targetId.equals(productId));
    }

    public boolean hasOpenProductAssignment(Long assigneeUserId, Long productId) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        return listOpenProductIdsForAssignee(assigneeUserId).stream()
                .anyMatch(targetId -> targetId.equals(productId));
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomFieldAssignment reopenWholeProductAssignmentForRejectedChangeRequest(Long changeRequestId) {
        requireNonNull(changeRequestId, "SHOWROOM_TARGET_NOT_FOUND: change request id is required");
        ShowroomFieldAssignmentDO assignment = assignmentMapper.selectLatestByLastChangeRequestId(
                TARGET_PRODUCT, PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, changeRequestId);
        if (assignment == null) {
            return null;
        }
        if (STATUS_OPEN.equals(assignment.getStatus())) {
            return toAssignment(assignment);
        }
        if (!STATUS_AUTO_SUBMITTED.equals(assignment.getStatus())) {
            throw new IllegalStateException(
                    "SHOWROOM_ASSIGNED_FIELD_AUTO_SUBMIT_FAILED: whole product assignment is not reopenable");
        }
        assignmentMapper.update(null, new LambdaUpdateWrapper<ShowroomFieldAssignmentDO>()
                .eq(ShowroomFieldAssignmentDO::getId, assignment.getId())
                .set(ShowroomFieldAssignmentDO::getStatus, STATUS_OPEN)
                .set(ShowroomFieldAssignmentDO::getClosedAt, null));
        assignment.setStatus(STATUS_OPEN);
        assignment.setClosedAt(null);
        return toAssignment(assignment);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markWholeProductAssignmentSubmitted(Long productId, Long operatorUserId, Long savedRevisionId,
                                                    Long changeRequestId) {
        closeWholeProductAssignment(productId, operatorUserId, savedRevisionId, changeRequestId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markWholeProductAssignmentDirectPublished(Long productId, Long operatorUserId, Long savedRevisionId) {
        closeWholeProductAssignment(productId, operatorUserId, savedRevisionId, null);
    }

    private void closeWholeProductAssignment(Long productId, Long operatorUserId, Long savedRevisionId,
                                             Long changeRequestId) {
        requireNonNull(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        ShowroomFieldAssignmentDO assignment = assignmentMapper.selectLatestOpenByTargetAndField(
                TARGET_PRODUCT, productId, PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE);
        if (assignment == null) {
            return;
        }
        if (!assignment.getAssigneeUserId().equals(operatorUserId)
                && !approvalActorResolver.hasPublicityRole(operatorUserId)) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ACCESS_DENIED: 当前产品已指派给其他用户填写");
        }
        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(STATUS_AUTO_SUBMITTED);
        assignment.setLastSavedRevisionId(savedRevisionId);
        assignment.setLastChangeRequestId(changeRequestId);
        assignment.setLatestAutoSavedAt(now);
        assignment.setSubmittedAt(now);
        assignment.setClosedAt(now);
        assignmentMapper.updateById(assignment);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAssignmentSubmitResult completeAndSubmit(Long assignmentId, String fieldValue, Long operatorUserId,
                                                            Long ignoredGaoxinUserId) {
        ShowroomFieldAssignmentDO assignment = requireAssignment(assignmentId);
        if (!STATUS_OPEN.equals(assignment.getStatus())) {
            throw new IllegalStateException("SHOWROOM_ASSIGNED_FIELD_AUTO_SUBMIT_FAILED: assignment is not open");
        }
        if (!assignment.getAssigneeUserId().equals(operatorUserId)) {
            throw new IllegalStateException("SHOWROOM_ROLE_BINDING_MISSING: assignment can only be completed by assignee");
        }
        if (isWholeProductAssignmentField(assignment.getFieldCode())) {
            throw new IllegalStateException("SHOWROOM_ASSIGNED_FIELD_AUTO_SUBMIT_FAILED: 请到产品管理中完成整产品填写后再提交审批");
        }
        requireText(fieldValue, "SHOWROOM_REQUIRED_FIELD_MISSING: assigned field value is required");
        Long publicityApproverUserId = approvalActorResolver.resolvePublicityApproverUserId();

        SubmissionRoute route = resolveSubmissionRoute(operatorUserId);
        String oldFieldValue = resolveFieldValue(assignment.getTargetType(), assignment.getFieldCode(),
                assignment.getTargetId(), null);
        Long savedRevisionId = saveAssignedFieldDraft(assignment, fieldValue);
        ShowroomChangeRequest changeRequest = workflowService.submit(new ShowroomWorkflowStart(
                assignment.getTargetType(),
                assignment.getTargetId(),
                savedRevisionId,
                assignment.getFieldCode(),
                "ASSIGNMENT_AUTO_SUBMIT",
                operatorUserId,
                route.submitterDeptId(),
                route.supervisorUserId(),
                publicityApproverUserId,
                assignment.getId(),
                List.of(new ShowroomChangeRequestItem(assignment.getFieldCode(), jsonValue(oldFieldValue),
                        jsonValue(fieldValue)))
        ));

        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(STATUS_AUTO_SUBMITTED);
        assignment.setLastSavedRevisionId(savedRevisionId);
        assignment.setLastChangeRequestId(changeRequest.changeRequestId());
        assignment.setLatestAutoSavedAt(now);
        assignment.setSubmittedAt(now);
        assignmentMapper.updateById(assignment);
        return new ShowroomAssignmentSubmitResult(toAssignment(assignment), changeRequest);
    }

    private void requireTargetExists(String targetType, Long targetId) {
        if (TARGET_PRODUCT.equals(targetType)) {
            contentService.getProduct(targetId);
            return;
        }
        if (TARGET_COMPANY.equals(targetType)) {
            contentService.getCompany(targetId);
            return;
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported assignment target " + targetType);
    }

    private void requireEditorAssignee(Long assigneeUserId) {
        AdminUserDO assignee = adminUserMapper.selectById(assigneeUserId);
        if (assignee == null || !CommonStatusEnum.isEnable(assignee.getStatus())) {
            throw new IllegalStateException("SHOWROOM_ROLE_BINDING_MISSING: assignment assignee must be an enabled editor");
        }
        List<Long> roleIds = userRoleMapper.selectListByUserId(assigneeUserId).stream()
                .map(UserRoleDO::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_ROLE_BINDING_MISSING: assignment assignee must have editor role");
        }
        boolean editorRoleMatched = roleMapper.selectList(RoleDO::getId, roleIds).stream()
                .filter(role -> CommonStatusEnum.isEnable(role.getStatus()))
                .anyMatch(role -> ShowroomRoleModelContract.editorRoleCode().equals(role.getCode()));
        if (!editorRoleMatched) {
            throw new IllegalStateException("SHOWROOM_ROLE_BINDING_MISSING: assignment assignee must have editor role");
        }
    }

    private SubmissionRoute resolveSubmissionRoute(Long operatorUserId) {
        AdminUserDO submitter = adminUserMapper.selectById(operatorUserId);
        if (submitter == null || !CommonStatusEnum.isEnable(submitter.getStatus())) {
            throw new IllegalStateException("SHOWROOM_ROLE_BINDING_MISSING: assignment assignee must be an enabled editor");
        }
        if (submitter.getDeptId() == null) {
            return new SubmissionRoute(null, null);
        }
        DeptDO dept = deptMapper.selectById(submitter.getDeptId());
        if (dept == null || dept.getLeaderUserId() == null) {
            return new SubmissionRoute(submitter.getDeptId(), null);
        }
        AdminUserDO supervisor = adminUserMapper.selectById(dept.getLeaderUserId());
        if (supervisor == null || !CommonStatusEnum.isEnable(supervisor.getStatus())) {
            return new SubmissionRoute(submitter.getDeptId(), null);
        }
        return new SubmissionRoute(submitter.getDeptId(), supervisor.getId());
    }

    private Long saveAssignedFieldDraft(ShowroomFieldAssignmentDO assignment, String fieldValue) {
        if (TARGET_PRODUCT.equals(assignment.getTargetType())) {
            ShowroomProductSnapshot snapshot = contentService.getProduct(assignment.getTargetId());
            ShowroomProductRevision revision = contentService.getCurrentOrLatestProductRevision(assignment.getTargetId());
            Map<String, String> fields = new LinkedHashMap<>(revision.fields());
            String nameCn = revision.nameCn();
            String nameEn = revision.nameEn();
            if ("name_cn".equals(assignment.getFieldCode())) {
                nameCn = fieldValue;
            } else if ("name_en".equals(assignment.getFieldCode())) {
                nameEn = fieldValue;
            } else {
                fields.put(assignment.getFieldCode(), fieldValue);
            }
            ShowroomProductRevision saved = contentService.saveProductDraft(new ShowroomProductDraft(
                    assignment.getTargetId(),
                    snapshot.productCode(),
                    nameCn,
                    nameEn,
                    fields
            ));
            return saved.revisionId();
        }
        if (TARGET_COMPANY.equals(assignment.getTargetType())) {
            ShowroomCompanySnapshot snapshot = contentService.getCompany(assignment.getTargetId());
            ShowroomCompanyRevision revision = snapshot.currentRevisionId()
                    .map(contentService::getCompanyRevision)
                    .orElseGet(() -> contentService.findCurrentOrLatestCompanyRevision()
                            .filter(candidate -> candidate.companyId().equals(assignment.getTargetId()))
                            .orElseThrow(() -> new IllegalStateException(
                                    "SHOWROOM_TARGET_NOT_FOUND: company revision not found")));
            Map<String, String> fields = new LinkedHashMap<>(revision.fields());
            fields.put(assignment.getFieldCode(), fieldValue);
            ShowroomCompanyRevision saved = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                    assignment.getTargetId(),
                    snapshot.companyType(),
                    snapshot.displayName(),
                    snapshot.displayNameEn(),
                    fields
            ));
            return saved.revisionId();
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported assignment target "
                + assignment.getTargetType());
    }

    private Long sendAssignmentNotify(ShowroomAssignmentCreate create) {
        NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
        reqDTO.setUserId(create.assigneeUserId());
        reqDTO.setTemplateCode(ASSIGNMENT_TEMPLATE_CODE);
        LinkedHashMap<String, Object> templateParams = new LinkedHashMap<>();
        templateParams.put("fieldCode", create.fieldCode());
        templateParams.put("targetType", create.targetType());
        templateParams.put("targetId", create.targetId());
        templateParams.put("assignedBy", create.assignedBy());
        templateParams.put("notifyTargetType", create.targetType());
        templateParams.put("notifyTargetId", create.targetId());
        if (TARGET_PRODUCT.equals(create.targetType())) {
            templateParams.put("notifyOpen", "list");
        }
        reqDTO.setTemplateParams(templateParams);
        try {
            Long notifyMessageId = notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
            if (notifyMessageId == null) {
                throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: notify message was not created");
            }
            if (notifyMessageMapper.selectById(notifyMessageId) == null) {
                throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: persisted notify message is required");
            }
            return notifyMessageId;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: " + ex.getMessage(), ex);
        }
    }

    private ShowroomFieldAssignmentDO requireAssignment(Long assignmentId) {
        requireNonNull(assignmentId, "SHOWROOM_TARGET_NOT_FOUND: assignment id is required");
        ShowroomFieldAssignmentDO assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: assignment not found");
        }
        if (!matchesCurrentTenant(assignment)) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: assignment not found");
        }
        return assignment;
    }

    private ShowroomAssignmentDetail toDetail(ShowroomFieldAssignmentDO assignment) {
        NotifyMessageDO notifyMessage = notifyMessageMapper.selectById(assignment.getNotifyMessageId());
        if (notifyMessage == null) {
            throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: linked notify message not found");
        }
        String latestChangeRequestStatus = assignment.getLastChangeRequestId() == null ? null
                : workflowService.getChangeRequest(assignment.getLastChangeRequestId()).status();
        return new ShowroomAssignmentDetail(assignment.getId(), assignment.getTargetType(), assignment.getTargetId(),
                assignment.getFieldCode(), assignment.getAssigneeUserId(), assignment.getAssignedBy(),
                assignment.getStatus(), assignment.getNotifyMessageId(), notifyMessage.getTemplateCode(),
                notifyMessage.getTemplateContent(), resolveCurrentDraftValue(assignment),
                assignment.getLastSavedRevisionId(), assignment.getLastChangeRequestId(), latestChangeRequestStatus);
    }

    private String resolveCurrentDraftValue(ShowroomFieldAssignmentDO assignment) {
        Long revisionId = assignment.getLastSavedRevisionId();
        return resolveFieldValue(assignment.getTargetType(), assignment.getFieldCode(), assignment.getTargetId(), revisionId);
    }

    private String resolveFieldValue(String targetType, String fieldCode, Long targetId, Long revisionId) {
        if (TARGET_PRODUCT.equals(targetType)) {
            ShowroomProductRevision revision = revisionId == null
                    ? contentService.getCurrentOrLatestProductRevision(targetId)
                    : contentService.getProductRevision(revisionId);
            if ("name_cn".equals(fieldCode)) {
                return revision.nameCn();
            }
            if ("name_en".equals(fieldCode)) {
                return revision.nameEn();
            }
            return revision.fields().get(fieldCode);
        }
        if (TARGET_COMPANY.equals(targetType)) {
            ShowroomCompanyRevision revision = revisionId == null
                    ? contentService.findCurrentOrLatestCompanyRevision()
                    .filter(candidate -> candidate.companyId().equals(targetId))
                    .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision not found"))
                    : contentService.getCompanyRevision(revisionId);
            return revision.fields().get(fieldCode);
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported assignment target " + targetType);
    }

    private static ShowroomFieldAssignment toAssignment(ShowroomFieldAssignmentDO assignment) {
        return new ShowroomFieldAssignment(assignment.getId(), assignment.getTargetType(), assignment.getTargetId(),
                assignment.getFieldCode(), assignment.getAssigneeUserId(), assignment.getAssignedBy(),
                assignment.getStatus(), assignment.getNotifyMessageId(), assignment.getLastSavedRevisionId(),
                assignment.getLastChangeRequestId());
    }

    private static void validateEditableField(String targetType, String fieldCode) {
        requireText(targetType, "SHOWROOM_TARGET_NOT_FOUND: assignment target type is required");
        requireText(fieldCode, "SHOWROOM_TARGET_NOT_FOUND: assignment field code is required");
        if (TARGET_PRODUCT.equals(targetType)) {
            if (isWholeProductAssignmentField(fieldCode)) {
                return;
            }
            ShowroomFieldCatalog.productField(fieldCode);
            return;
        }
        if (TARGET_COMPANY.equals(targetType) && COMPANY_EDITABLE_FIELDS.contains(fieldCode)) {
            return;
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: assignment field is not editable");
    }

    private void ensureNoOpenWholeProductAssignmentConflict(String targetType, Long targetId, String fieldCode) {
        if (!TARGET_PRODUCT.equals(targetType) || !isWholeProductAssignmentField(fieldCode)) {
            return;
        }
        ShowroomFieldAssignmentDO existing = assignmentMapper.selectLatestOpenByTargetAndField(
                TARGET_PRODUCT, targetId, PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE);
        if (existing != null) {
            throw new IllegalStateException("SHOWROOM_ASSIGNED_FIELD_AUTO_SUBMIT_FAILED: 当前产品已存在进行中的整产品指派");
        }
    }

    public static boolean isWholeProductAssignmentField(String fieldCode) {
        return PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE.equals(fieldCode);
    }

    private static boolean isWholeProductAssignment(ShowroomFieldAssignmentDO assignment) {
        return assignment != null && isWholeProductAssignmentField(assignment.getFieldCode());
    }

    private List<ShowroomFieldAssignmentDO> listLatestWholeProductAssignments() {
        return List.copyOf(latestWholeProductAssignmentsByProductId().values());
    }

    private Map<Long, ShowroomFieldAssignmentDO> latestWholeProductAssignmentsByProductId() {
        LinkedHashMap<Long, ShowroomFieldAssignmentDO> latestAssignments = new LinkedHashMap<>();
        assignmentMapper.selectList().stream()
                .filter(this::matchesCurrentTenant)
                .filter(assignment -> TARGET_PRODUCT.equals(assignment.getTargetType()))
                .filter(ShowroomAssignmentService::isWholeProductAssignment)
                .sorted(Comparator
                        .comparing(ShowroomFieldAssignmentDO::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ShowroomFieldAssignmentDO::getId, Comparator.reverseOrder()))
                .forEach(assignment -> latestAssignments.putIfAbsent(assignment.getTargetId(), assignment));
        return latestAssignments;
    }

    private Map<Long, ShowroomChangeRequest> latestProductChangeRequestsByProductId() {
        LinkedHashMap<Long, ShowroomChangeRequest> latestRequests = new LinkedHashMap<>();
        workflowService.listChangeRequests().stream()
                .filter(request -> TARGET_PRODUCT.equals(request.targetType()))
                .forEach(request -> latestRequests.putIfAbsent(request.targetId(), request));
        return latestRequests;
    }

    private Long resolveDepartmentLeaderUserId(Long assigneeUserId) {
        AdminUserDO assignee = adminUserMapper.selectById(assigneeUserId);
        if (assignee == null || !CommonStatusEnum.isEnable(assignee.getStatus()) || assignee.getDeptId() == null) {
            return null;
        }
        DeptDO dept = deptMapper.selectById(assignee.getDeptId());
        if (dept == null || !CommonStatusEnum.isEnable(dept.getStatus())) {
            return null;
        }
        return dept.getLeaderUserId();
    }

    private boolean shouldCountChangeRequestVisibility(ShowroomChangeRequest request,
                                                       ShowroomFieldAssignmentDO latestAssignment) {
        if (latestAssignment == null) {
            return true;
        }
        if (Objects.equals(latestAssignment.getLastChangeRequestId(), request.changeRequestId())) {
            return true;
        }
        LocalDateTime assignmentCreatedAt = latestAssignment.getCreatedAt();
        Instant requestSubmittedAt = request.submittedAt();
        if (assignmentCreatedAt == null || requestSubmittedAt == null) {
            return false;
        }
        return !assignmentCreatedAt.isAfter(LocalDateTime.ofInstant(requestSubmittedAt, ZoneOffset.UTC));
    }

    private boolean matchesCurrentTenant(ShowroomFieldAssignmentDO assignment) {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null || Objects.equals(tenantId, assignment.getTenantId());
    }

    private static String jsonValue(String value) {
        if (value == null) {
            return null;
        }
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        payload.put("value", value);
        return JsonUtils.toJsonString(payload);
    }

    private static <T> List<T> page(List<T> values, Integer pageNo, Integer pageSize) {
        int resolvedPageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int resolvedPageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 20);
        int fromIndex = Math.min((resolvedPageNo - 1) * resolvedPageSize, values.size());
        int toIndex = Math.min(fromIndex + resolvedPageSize, values.size());
        return values.subList(fromIndex, toIndex);
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

    private record SubmissionRoute(Long submitterDeptId, Long supervisorUserId) {
    }

}
