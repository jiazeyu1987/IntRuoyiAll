package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordResult;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordService;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProviderRegistry;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.APPROVAL_SIGNATURE_IMAGE_REQUIRED;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.APPROVAL_VIEW_TYPE_UNSUPPORTED;

@Service
public class ApprovalCenterServiceImpl implements ApprovalCenterService {

    private final ApprovalTaskProviderRegistry providerRegistry;
    private final PermissionApi permissionApi;
    private final AdminUserApi adminUserApi;
    private final ApprovalSignatureRecordService signatureRecordService;

    public ApprovalCenterServiceImpl(ApprovalTaskProviderRegistry providerRegistry, PermissionApi permissionApi,
                                     AdminUserApi adminUserApi,
                                     ApprovalSignatureRecordService signatureRecordService) {
        this.providerRegistry = providerRegistry;
        this.permissionApi = permissionApi;
        this.adminUserApi = adminUserApi;
        this.signatureRecordService = signatureRecordService;
    }

    @Override
    public List<ApprovalProviderDescriptor> listProviders(Long loginUserId) {
        Objects.requireNonNull(loginUserId, "APPROVAL_LOGIN_USER_REQUIRED");
        return providerRegistry.listProviders().stream()
                .filter(provider -> provider.isVisibleTo(loginUserId))
                .map(provider -> new ApprovalProviderDescriptor()
                        .setModuleCode(provider.getModuleCode())
                        .setModuleName(provider.getModuleName())
                        .setProviderCode(provider.getProviderCode())
                        .setProviderVersion(provider.getProviderVersion())
                        .setSupportedViewTypes(Objects.requireNonNull(provider.getSupportedViewTypes()))
                        .setCapabilities(Objects.requireNonNull(provider.getCapabilities())))
                .toList();
    }

    @Override
    public PageResult<ApprovalTaskSummary> getTaskPage(Long loginUserId, ApprovalTaskQuery query) {
        Objects.requireNonNull(loginUserId, "APPROVAL_LOGIN_USER_REQUIRED");
        boolean globalView = hasApprovalAdminRole(loginUserId);
        ApprovalTaskQuery safeQuery = query == null ? new ApprovalTaskQuery() : query;
        ApprovalTaskViewType viewType = Objects.requireNonNull(safeQuery.getViewType(),
                "APPROVAL_VIEW_TYPE_REQUIRED");
        if (safeQuery.getModuleCode() != null) {
            ApprovalTaskProvider provider = providerRegistry.requireProvider(safeQuery.getModuleCode());
            assertProviderVisible(provider, loginUserId);
            assertViewSupported(provider, viewType);
            return enrichAssigneeUserNames(requirePage(provider.page(
                    toContext(loginUserId, provider, safeQuery, globalView))));
        }

        List<ApprovalTaskProvider> matchedProviders = providerRegistry.listProviders().stream()
                .filter(provider -> provider.isVisibleTo(loginUserId))
                .filter(provider -> provider.getSupportedViewTypes().contains(viewType))
                .toList();
        if (matchedProviders.isEmpty()) {
            throw exception(APPROVAL_VIEW_TYPE_UNSUPPORTED, "全部可见模块", viewType);
        }
        ApprovalTaskQuery globalWindowQuery = toGlobalWindowQuery(safeQuery);
        List<PageResult<ApprovalTaskSummary>> providerPages = matchedProviders.stream()
                .map(provider -> requirePage(provider.page(toContext(loginUserId, provider, globalWindowQuery,
                        globalView))))
                .toList();
        List<ApprovalTaskSummary> rows = providerPages.stream()
                .flatMap(page -> page.getList().stream())
                .sorted(Comparator.comparing(ApprovalCenterServiceImpl::sortTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        enrichAssigneeUserNames(rows);
        return pageRows(rows, safeQuery.getPageNo(), safeQuery.getPageSize(), sumTotal(providerPages));
    }

    @Override
    public List<ApprovalTaskTimelineEntry> listTaskTimeline(Long loginUserId, ApprovalTaskTimelineQuery query) {
        Objects.requireNonNull(loginUserId, "APPROVAL_LOGIN_USER_REQUIRED");
        boolean globalView = hasApprovalAdminRole(loginUserId);
        Objects.requireNonNull(query, "APPROVAL_TIMELINE_QUERY_REQUIRED");
        Objects.requireNonNull(query.getModuleCode(), "APPROVAL_MODULE_REQUIRED");
        requireText(query.getSourceTaskType(), "APPROVAL_SOURCE_TASK_TYPE_REQUIRED");
        ApprovalTaskProvider provider = providerRegistry.requireProvider(query.getModuleCode());
        assertProviderVisible(provider, loginUserId);
        if (!Objects.requireNonNull(provider.getCapabilities(), "APPROVAL_ADAPTER_CAPABILITIES_REQUIRED")
                .contains(ApprovalTaskCapability.TIMELINE)) {
            throw new IllegalArgumentException("APPROVAL_TIMELINE_UNSUPPORTED: "
                    + query.getModuleCode() + " does not support timeline");
        }
        List<ApprovalTaskTimelineEntry> entries = provider.listTimeline(ApprovalTaskTimelineQueryContext.of(
                loginUserId, query.getModuleCode(), query.getSourceTaskType(), query.getSourceTaskId(),
                query.getBusinessKey(), query.getProcessInstanceId(), globalView));
        Objects.requireNonNull(entries, "APPROVAL_ADAPTER_TIMELINE_REQUIRED: " + query.getModuleCode());
        if (entries.isEmpty()) {
            throw new IllegalStateException("APPROVAL_ADAPTER_TIMELINE_EMPTY: " + query.getModuleCode());
        }
        return entries;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewTask(Long loginUserId, ApprovalTaskReviewCommand command) {
        Objects.requireNonNull(loginUserId, "APPROVAL_LOGIN_USER_REQUIRED");
        Objects.requireNonNull(command, "APPROVAL_REVIEW_COMMAND_REQUIRED");
        Objects.requireNonNull(command.getModuleCode(), "APPROVAL_MODULE_REQUIRED");
        requireText(command.getSourceTaskType(), "APPROVAL_SOURCE_TASK_TYPE_REQUIRED");
        Objects.requireNonNull(command.getResult(), "APPROVAL_REVIEW_RESULT_REQUIRED");
        if (command.getResult() == ApprovalTaskReviewResult.REJECT
                && (command.getReason() == null || command.getReason().isBlank())) {
            throw new IllegalArgumentException("APPROVAL_REJECT_REASON_REQUIRED");
        }
        String signaturePassword = command.getSignaturePassword();
        if (signaturePassword == null || signaturePassword.isBlank()) {
            throw new IllegalArgumentException("APPROVAL_SIGNATURE_PASSWORD_REQUIRED");
        }
        signaturePassword = signaturePassword.trim();
        boolean globalView = hasApprovalAdminRole(loginUserId);
        ApprovalTaskProvider provider = providerRegistry.requireProvider(command.getModuleCode());
        assertProviderVisible(provider, loginUserId);
        adminUserApi.validatePassword(loginUserId, signaturePassword);
        ApprovalTaskReviewContext reviewContext = ApprovalTaskReviewContext.of(loginUserId, command.getModuleCode(),
                command.getSourceTaskType(),
                command.getSourceTaskId(), command.getBusinessKey(), command.getProcessInstanceId(),
                command.getResult(), command.getReason() == null ? null : command.getReason().trim(),
                signaturePassword, globalView);
        ApprovalSignatureRecordResult signatureRecord =
                Objects.requireNonNull(signatureRecordService.recordReviewSignature(reviewContext),
                        "APPROVAL_SIGNATURE_RECORD_RESULT_REQUIRED");
        reviewContext.setSignatureImageFileUrl(requireSignatureImageFileUrl(signatureRecord));
        provider.review(reviewContext);
    }

    private static ApprovalTaskQueryContext toContext(Long loginUserId, ApprovalTaskProvider provider,
                                                      ApprovalTaskQuery query, boolean globalView) {
        return ApprovalTaskQueryContext.of(loginUserId, query.getViewType(), provider.getModuleCode(),
                query.getKeyword(), query.getPageNo(), query.getPageSize(), globalView);
    }

    private boolean hasApprovalAdminRole(Long loginUserId) {
        return permissionApi.hasAnyRoles(loginUserId, RoleCodeEnum.APPROVAL_ADMIN.getCode());
    }

    private static void assertViewSupported(ApprovalTaskProvider provider, ApprovalTaskViewType viewType) {
        if (!provider.getSupportedViewTypes().contains(viewType)) {
            throw exception(APPROVAL_VIEW_TYPE_UNSUPPORTED, provider.getModuleCode(), viewType);
        }
    }

    private static void assertProviderVisible(ApprovalTaskProvider provider, Long loginUserId) {
        if (!provider.isVisibleTo(loginUserId)) {
            throw new IllegalArgumentException("APPROVAL_MODULE_FORBIDDEN: "
                    + provider.getModuleCode() + " is not visible to user " + loginUserId);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new NullPointerException(message);
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String requireSignatureImageFileUrl(ApprovalSignatureRecordResult signatureRecord) {
        String signatureImageFileUrl = signatureRecord.getSignatureImageFileUrl();
        if (signatureImageFileUrl == null || signatureImageFileUrl.isBlank()) {
            throw exception(APPROVAL_SIGNATURE_IMAGE_REQUIRED);
        }
        return signatureImageFileUrl.trim();
    }

    private static PageResult<ApprovalTaskSummary> requirePage(PageResult<ApprovalTaskSummary> page) {
        Objects.requireNonNull(page, "APPROVAL_ADAPTER_PAGE_REQUIRED");
        Objects.requireNonNull(page.getList(), "APPROVAL_ADAPTER_PAGE_LIST_REQUIRED");
        Objects.requireNonNull(page.getTotal(), "APPROVAL_ADAPTER_PAGE_TOTAL_REQUIRED");
        return page;
    }

    private static ApprovalTaskQuery toGlobalWindowQuery(ApprovalTaskQuery query) {
        int safePageNo = safePageNo(query.getPageNo());
        int safePageSize = safePageSize(query.getPageSize());
        long requestedRows = (long) safePageNo * safePageSize;
        int globalWindowSize = requestedRows > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) requestedRows;
        return new ApprovalTaskQuery()
                .setViewType(query.getViewType())
                .setModuleCode(query.getModuleCode())
                .setKeyword(query.getKeyword())
                .setPageNo(1)
                .setPageSize(globalWindowSize);
    }

    private static PageResult<ApprovalTaskSummary> pageRows(List<ApprovalTaskSummary> rows,
                                                            Integer pageNo, Integer pageSize) {
        return pageRows(rows, pageNo, pageSize, (long) rows.size());
    }

    private static PageResult<ApprovalTaskSummary> pageRows(List<ApprovalTaskSummary> rows,
                                                            Integer pageNo, Integer pageSize, Long total) {
        int safePageNo = safePageNo(pageNo);
        int safePageSize = safePageSize(pageSize);
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), total);
    }

    private static int safePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private static int safePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    private static long sumTotal(List<PageResult<ApprovalTaskSummary>> pages) {
        return pages.stream().mapToLong(PageResult::getTotal).sum();
    }

    private PageResult<ApprovalTaskSummary> enrichAssigneeUserNames(PageResult<ApprovalTaskSummary> page) {
        enrichAssigneeUserNames(page.getList());
        return page;
    }

    private void enrichAssigneeUserNames(List<ApprovalTaskSummary> rows) {
        Set<Long> assigneeUserIds = new LinkedHashSet<>();
        rows.forEach(row -> {
            if (row.getAssigneeUserId() != null) {
                assigneeUserIds.add(row.getAssigneeUserId());
            }
        });
        if (assigneeUserIds.isEmpty()) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = Objects.requireNonNull(adminUserApi.getUserMap(assigneeUserIds),
                "APPROVAL_ASSIGNEE_USER_MAP_REQUIRED");
        rows.forEach(row -> {
            Long assigneeUserId = row.getAssigneeUserId();
            if (assigneeUserId == null) {
                return;
            }
            AdminUserRespDTO user = userMap.get(assigneeUserId);
            row.setAssigneeUserName(user == null ? resolveMissingUserName(assigneeUserId) : resolveUserName(user));
        });
    }

    private static String resolveMissingUserName(Long assigneeUserId) {
        return "用户不存在(" + assigneeUserId + ")";
    }

    private static String resolveUserName(AdminUserRespDTO user) {
        String username = trimToNull(user.getUsername());
        String nickname = trimToNull(user.getNickname());
        if (isUnreadableText(nickname)) {
            return username != null ? username : nickname;
        }
        if (nickname != null && username != null && !Objects.equals(nickname, username)) {
            return nickname + "(" + username + ")";
        }
        return nickname != null ? nickname : username;
    }

    private static boolean isUnreadableText(String value) {
        return value != null && value.matches(".*\\?{2,}.*");
    }

    private static LocalDateTime sortTime(ApprovalTaskSummary summary) {
        if (summary.getTaskCreatedAt() != null) {
            return summary.getTaskCreatedAt();
        }
        return summary.getInitiatedAt();
    }
}
