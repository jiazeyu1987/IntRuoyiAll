package cn.iocoder.yudao.module.srm.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
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
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierPortalApplicationStatusEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class SrmSupplierPortalApprovalTaskAdapter implements ApprovalTaskProvider {

    private static final String SOURCE_TASK_TYPE = "SRM_SUPPLIER_PORTAL_APPLICATION";
    private static final String REVIEW_PERMISSION = "srm:supplier-portal:review";
    private static final String AUDIT_PERMISSION = "srm:supplier-portal:audit";
    private static final String DETAIL_ROUTE = "/srm/supplier-portal-review";
    private static final Set<ApprovalTaskViewType> SUPPORTED_VIEWS = Set.of(
            ApprovalTaskViewType.TODO,
            ApprovalTaskViewType.DONE,
            ApprovalTaskViewType.MY_INITIATED
    );
    private static final Set<ApprovalTaskCapability> CAPABILITIES = Set.of(
            ApprovalTaskCapability.TIMELINE,
            ApprovalTaskCapability.AUDIT
    );
    private static final List<String> ACTIVE_STATUSES = List.of(
            SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(),
            SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus(),
            SrmSupplierPortalApplicationStatusEnum.REJECTED.getStatus()
    );
    private static final List<String> DONE_STATUSES = List.of(
            SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus(),
            SrmSupplierPortalApplicationStatusEnum.REJECTED.getStatus()
    );

    private final SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;
    private final PermissionApi permissionApi;

    public SrmSupplierPortalApprovalTaskAdapter(SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper,
                                                PermissionApi permissionApi) {
        this.supplierPortalApplicationMapper = supplierPortalApplicationMapper;
        this.permissionApi = permissionApi;
    }

    @Override
    public ApprovalModuleCode getModuleCode() {
        return ApprovalModuleCode.SRM;
    }

    @Override
    public String getModuleName() {
        return "SRM 供应商门户审核";
    }

    @Override
    public String getProviderCode() {
        return "srm-supplier-portal-approval";
    }

    @Override
    public String getProviderVersion() {
        return "phase6";
    }

    @Override
    public Set<ApprovalTaskViewType> getSupportedViewTypes() {
        return SUPPORTED_VIEWS;
    }

    @Override
    public Set<ApprovalTaskCapability> getCapabilities() {
        return CAPABILITIES;
    }

    public boolean isVisibleTo(Long loginUserId) {
        return true;
    }

    @Override
    public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<SrmSupplierPortalApplicationDO> rows = switch (context.getViewType()) {
            case TODO -> supplierPortalApplicationMapper.selectUnifiedApprovalList(tenantId,
                    SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(),
                    context.isGlobalView() ? null : context.getLoginUserId(), null, context.getKeyword());
            case DONE -> supplierPortalApplicationMapper.selectUnifiedApprovalList(tenantId, DONE_STATUSES,
                    null,
                    context.isGlobalView() ? null : context.getLoginUserId(), context.getKeyword());
            case MY_INITIATED -> supplierPortalApplicationMapper.selectUnifiedApprovalList(tenantId, ACTIVE_STATUSES,
                    context.isGlobalView() ? null : context.getLoginUserId(), null, context.getKeyword());
            default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: SRM does not support "
                    + context.getViewType());
        };
        List<ApprovalTaskSummary> summaries = rows.stream().map(this::toSummary).toList();
        return pageRows(summaries, context.getPageNo(), context.getPageSize());
    }

    @Override
    public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
        requireSourceTaskType(context.getSourceTaskType());
        Long applicationId = resolveApplicationId(context);
        SrmSupplierPortalApplicationDO application = requireApplication(applicationId);
        assertTimelineAccess(context, application);
        return buildTimeline(application);
    }

    private ApprovalTaskSummary toSummary(SrmSupplierPortalApplicationDO application) {
        Long applicationId = requireApplicationId(application);
        Map<String, String> detailQuery = new LinkedHashMap<>();
        detailQuery.put("applicationId", String.valueOf(applicationId));
        String status = application.getApplicationStatus();
        ApprovalTaskReviewResult approvalResult = ApprovalTaskResultSupport.fromApprovedRejectedStatus(status,
                SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus(),
                SrmSupplierPortalApplicationStatusEnum.REJECTED.getStatus());
        return ApprovalTaskSummary.builder()
                .id("SRM:" + SOURCE_TASK_TYPE + ":" + applicationId)
                .moduleCode(ApprovalModuleCode.SRM)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(applicationId))
                .businessKey(String.valueOf(applicationId))
                .businessTitle("供应商门户申请 - " + requireText(application.getCompanyName(),
                        "SRM_APPROVAL_BUSINESS_TITLE_REQUIRED"))
                .businessCode(application.getUnifiedSocialCreditCode())
                .businessStatus(SrmSupplierPortalApplicationStatusEnum.getLabel(status))
                .businessDeleted(Boolean.FALSE)
                .currentNodeCode(status)
                .currentNodeName(resolveCurrentNodeName(status))
                .initiatorUserId(application.getUserId())
                .initiatedAt(application.getSubmittedTime())
                .taskCreatedAt(application.getSubmittedTime())
                .taskCompletedAt(isDone(status) ? application.getAuditTime() : null)
                .approvalResult(approvalResult)
                .approvalRemark(ApprovalTaskResultSupport.rejectRemark(approvalResult,
                        application.getAuditRemark()))
                .requiresSignature(Boolean.FALSE)
                .detailRoute(DETAIL_ROUTE)
                .detailQuery(detailQuery)
                .availableActions(Set.of("PROCESS_IN_MODULE"))
                .capabilities(CAPABILITIES)
                .build();
    }

    private List<ApprovalTaskTimelineEntry> buildTimeline(SrmSupplierPortalApplicationDO application) {
        requireSubmittedTime(application);
        Long applicationId = requireApplicationId(application);
        List<ApprovalTaskTimelineEntry> entries = new ArrayList<>();
        entries.add(ApprovalTaskTimelineEntry.builder()
                .id("SRM:" + applicationId + ":SUBMITTED")
                .moduleCode(ApprovalModuleCode.SRM)
                .sourceTaskType(SOURCE_TASK_TYPE)
                .sourceTaskId(String.valueOf(applicationId))
                .businessKey(String.valueOf(applicationId))
                .nodeCode("SUBMITTED")
                .nodeName("提交门户申请")
                .action("SUBMITTED")
                .actionLabel("提交门户申请")
                .actorUserId(application.getUserId())
                .actedAt(application.getSubmittedTime())
                .status("DONE")
                .evidenceType("SRM_SUPPLIER_PORTAL_APPLICATION")
                .domainReferenceId("srm_supplier_portal_application:" + applicationId)
                .build());
        if (SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus().equals(application.getApplicationStatus())) {
            entries.add(ApprovalTaskTimelineEntry.builder()
                    .id("SRM:" + applicationId + ":AUDIT:PENDING")
                    .moduleCode(ApprovalModuleCode.SRM)
                    .sourceTaskType(SOURCE_TASK_TYPE)
                    .sourceTaskId(String.valueOf(applicationId))
                    .businessKey(String.valueOf(applicationId))
                    .nodeCode("AUDIT")
                    .nodeName("门户审核")
                    .action("PENDING")
                    .actionLabel("待审核")
                    .actedAt(application.getSubmittedTime())
                    .status("PENDING")
                    .evidenceType("SRM_SUPPLIER_PORTAL_AUDIT")
                    .domainReferenceId("srm_supplier_portal_application:" + applicationId)
                    .build());
            return entries;
        }
        if (isDone(application.getApplicationStatus())) {
            requireAuditEvidence(application);
            entries.add(ApprovalTaskTimelineEntry.builder()
                    .id("SRM:" + applicationId + ":AUDIT:" + application.getApplicationStatus())
                    .moduleCode(ApprovalModuleCode.SRM)
                    .sourceTaskType(SOURCE_TASK_TYPE)
                    .sourceTaskId(String.valueOf(applicationId))
                    .businessKey(String.valueOf(applicationId))
                    .nodeCode("AUDIT")
                    .nodeName("门户审核")
                    .action(application.getApplicationStatus())
                    .actionLabel(SrmSupplierPortalApplicationStatusEnum.getLabel(application.getApplicationStatus()))
                    .actorUserId(application.getAuditBy())
                    .actedAt(application.getAuditTime())
                    .comment(application.getAuditRemark())
                    .status(application.getApplicationStatus())
                    .evidenceType("SRM_SUPPLIER_PORTAL_AUDIT")
                    .domainReferenceId("srm_supplier_portal_application:" + applicationId)
                    .build());
        }
        return entries;
    }

    private SrmSupplierPortalApplicationDO requireApplication(Long applicationId) {
        SrmSupplierPortalApplicationDO application = supplierPortalApplicationMapper.selectById(applicationId);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (application == null || !Objects.equals(application.getTenantId(), tenantId)) {
            throw new IllegalStateException("SRM_APPROVAL_APPLICATION_NOT_FOUND: " + applicationId);
        }
        return application;
    }

    private void assertTimelineAccess(ApprovalTaskTimelineQueryContext context,
                                      SrmSupplierPortalApplicationDO application) {
        if (context.isGlobalView()) {
            return;
        }
        Long loginUserId = context.getLoginUserId();
        if (Objects.equals(loginUserId, application.getUserId())
                || Objects.equals(loginUserId, application.getAuditBy())
                || hasReviewPermission(loginUserId)) {
            return;
        }
        throw new IllegalStateException("SRM_APPROVAL_TIMELINE_ACCESS_DENIED: " + requireApplicationId(application));
    }

    private void requireReviewPermission(Long loginUserId) {
        if (!hasReviewPermission(loginUserId)) {
            throw new IllegalStateException("SRM_APPROVAL_PERMISSION_REQUIRED: user " + loginUserId
                    + " cannot review supplier portal applications");
        }
    }

    private boolean hasReviewPermission(Long loginUserId) {
        Objects.requireNonNull(loginUserId, "APPROVAL_LOGIN_USER_REQUIRED");
        return permissionApi.hasAnyPermissions(loginUserId, REVIEW_PERMISSION, AUDIT_PERMISSION);
    }

    private static void requireSourceTaskType(String sourceTaskType) {
        if (!SOURCE_TASK_TYPE.equals(sourceTaskType)) {
            throw new IllegalArgumentException("APPROVAL_SOURCE_TASK_TYPE_UNSUPPORTED: SRM does not support "
                    + sourceTaskType);
        }
    }

    private static Long resolveApplicationId(ApprovalTaskTimelineQueryContext context) {
        String value = context.getSourceTaskId();
        if (value == null || value.isBlank()) {
            value = context.getBusinessKey();
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("SRM_APPROVAL_APPLICATION_ID_REQUIRED");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("SRM_APPROVAL_APPLICATION_ID_INVALID: " + value, ex);
        }
    }

    private static Long requireApplicationId(SrmSupplierPortalApplicationDO application) {
        if (application.getId() == null) {
            throw new IllegalStateException("SRM_APPROVAL_APPLICATION_ID_REQUIRED");
        }
        return application.getId();
    }

    private static void requireSubmittedTime(SrmSupplierPortalApplicationDO application) {
        if (application.getSubmittedTime() == null) {
            throw new IllegalStateException("SRM_APPROVAL_TIMELINE_SOURCE_REQUIRED: submittedTime is required");
        }
    }

    private static void requireAuditEvidence(SrmSupplierPortalApplicationDO application) {
        if (application.getAuditBy() == null || application.getAuditTime() == null) {
            throw new IllegalStateException("SRM_APPROVAL_TIMELINE_SOURCE_REQUIRED: audit evidence is required");
        }
    }

    private static boolean isDone(String status) {
        return SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus().equals(status)
                || SrmSupplierPortalApplicationStatusEnum.REJECTED.getStatus().equals(status);
    }

    private static String resolveCurrentNodeName(String status) {
        if (SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus().equals(status)) {
            return "待审核";
        }
        return SrmSupplierPortalApplicationStatusEnum.getLabel(status);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
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
