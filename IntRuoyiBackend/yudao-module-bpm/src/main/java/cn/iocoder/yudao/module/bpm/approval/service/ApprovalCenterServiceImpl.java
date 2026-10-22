package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalCenterReviewPreflight;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public PageResult<ApprovalTaskSummary> getTaskPage(Long loginUserId, ApprovalTaskQuery query) {
        Objects.requireNonNull(loginUserId, "APPROVAL_LOGIN_USER_REQUIRED");
        boolean globalView = hasApprovalAdminRole(loginUserId);
        ApprovalTaskQuery safeQuery = query == null ? new ApprovalTaskQuery() : query;
        ApprovalTaskViewType viewType = Objects.requireNonNull(safeQuery.getViewType(),
                "APPROVAL_VIEW_TYPE_REQUIRED");
        queryWindowSize(safeQuery);
        var context=ApprovalTaskQueryContext.of(loginUserId,viewType,safeQuery.getModuleCode(),safeQuery.getKeyword(),
                safeQuery.getPageNo(),safeQuery.getPageSize(),globalView);
        List<ApprovalTaskProvider> providers;
        if(safeQuery.getModuleCode()!=null) {
            var provider=providerRegistry.requireProvider(safeQuery.getModuleCode());
            assertProviderVisible(provider,loginUserId);assertViewSupported(provider,viewType);providers=List.of(provider);
        } else {
            providers=providerRegistry.listProviders().stream().filter(p->p.isVisibleTo(loginUserId))
                    .filter(p->p.getSupportedViewTypes().contains(viewType)).toList();
            if(providers.isEmpty()) throw exception(APPROVAL_VIEW_TYPE_UNSUPPORTED,"全部可见模块",viewType);
        }
        return enrichTaskUserNames(ApprovalTaskWindowMerge.read(providers,context,ApprovalQueryWindow.from(context)));
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
        String reason = command.getReason() == null ? null
                : command.getReason().replaceAll(
                        "^[\\p{javaWhitespace}\\p{javaSpaceChar}\\x00-\\x20\\uFEFF]+|[\\p{javaWhitespace}\\p{javaSpaceChar}\\x00-\\x20\\uFEFF]+$", "");
        if (reason == null || reason.isEmpty()) {
            throw new IllegalArgumentException("APPROVAL_REVIEW_REASON_REQUIRED");
        }
        String signaturePassword = command.getSignaturePassword();
        if (signaturePassword == null || signaturePassword.isBlank()) {
            throw new IllegalArgumentException("APPROVAL_SIGNATURE_PASSWORD_REQUIRED");
        }
        signaturePassword = signaturePassword.trim();
        boolean globalView = hasApprovalAdminRole(loginUserId);
        ApprovalTaskProvider provider = providerRegistry.requireProvider(command.getModuleCode());
        assertProviderVisible(provider, loginUserId);
        ApprovalTaskReviewContext reviewContext = ApprovalTaskReviewContext.of(loginUserId, command.getModuleCode(),
                command.getSourceTaskType(),
                command.getSourceTaskId(), command.getBusinessKey(), command.getProcessInstanceId(),
                command.getResult(), reason,
                signaturePassword, globalView);
        if (command.getModuleCode() == ApprovalModuleCode.EDHR) {
            if (!(provider instanceof ApprovalCenterReviewPreflight preflight)) {
                throw new IllegalStateException("APPROVAL_REVIEW_PREFLIGHT_REQUIRED: EDHR");
            }
            preflight.preflightReview(reviewContext);
        }
        if (command.getModuleCode() == ApprovalModuleCode.BPM && provider instanceof ApprovalCenterReviewPreflight preflight) {
            preflight.preflightReview(reviewContext);
        }
        ApprovalSignatureRecordResult signatureRecord =
                Objects.requireNonNull(signatureRecordService.recordReviewSignature(reviewContext),
                        "APPROVAL_SIGNATURE_RECORD_RESULT_REQUIRED");
        reviewContext.setSignatureImageFileUrl(requireSignatureImageFileUrl(signatureRecord));
        reviewContext.setSignatureSubjectId(requireText(signatureRecord.getSubjectId(),
                "APPROVAL_SIGNATURE_SUBJECT_REQUIRED"));
        reviewContext.setSignatureEvidenceHash(requireText(signatureRecord.getEvidenceHash(),
                "APPROVAL_SIGNATURE_EVIDENCE_REQUIRED"));
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

    private static PageResult<ApprovalTaskSummary> requireConsistentPage(ApprovalTaskProvider provider,
                                                                         ApprovalTaskQueryContext context,
                                                                         PageResult<ApprovalTaskSummary> page) {
        PageResult<ApprovalTaskSummary> requiredPage = requirePage(page);
        if (requiredPage.getTotal() > 0 && requiredPage.getList().isEmpty()
                && safePageNo(context.getPageNo()) == 1) {
            throw new IllegalStateException("APPROVAL_ADAPTER_PAGE_INCONSISTENT: "
                    + provider.getModuleCode() + " reported total " + requiredPage.getTotal()
                    + " but returned no rows for the first page");
        }
        return requiredPage;
    }

    private static ApprovalTaskQueryContext toGlobalWindowQuery(Long loginUserId, ApprovalTaskQuery query,
                                                                boolean globalView, long window) {
        // Internal context carries K independently of the HTTP request's pageSize <= 200 validation.
        return ApprovalTaskQueryContext.of(loginUserId,query.getViewType(),null,query.getKeyword(),1,10,globalView)
                .setWindow(new ApprovalQueryWindow(0,window));
    }

    private static long queryWindowSize(ApprovalTaskQuery query) {
        long window = Math.multiplyExact((long)safePageNo(query.getPageNo()),safePageSize(query.getPageSize()));
        return window;
    }

    private static PageResult<ApprovalTaskSummary> pageRows(List<ApprovalTaskSummary> rows,
                                                            Integer pageNo, Integer pageSize) {
        return pageRows(rows, pageNo, pageSize, (long) rows.size());
    }

    private static PageResult<ApprovalTaskSummary> pageRows(List<ApprovalTaskSummary> rows,
                                                            Integer pageNo, Integer pageSize, Long total) {
        int safePageNo = safePageNo(pageNo);
        int safePageSize = safePageSize(pageSize);
        int fromIndex = (int)Math.min(Math.multiplyExact((long)safePageNo - 1,safePageSize),rows.size());
        int toIndex = (int)Math.min((long)fromIndex + safePageSize,rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), total);
    }

    private static int safePageNo(Integer pageNo) {
        if (pageNo != null && pageNo < 1) throw new IllegalArgumentException("QUERY_WINDOW_RANGE_UNSUPPORTED");
        return pageNo == null ? 1 : pageNo;
    }

    private static int safePageSize(Integer pageSize) {
        if (pageSize != null && pageSize < 1) throw new IllegalArgumentException("QUERY_WINDOW_RANGE_UNSUPPORTED");
        return pageSize == null ? 10 : pageSize;
    }

    private static long sumTotal(List<PageResult<ApprovalTaskSummary>> pages) {
        long total=0; for(var page:pages) total=Math.addExact(total,page.getTotal()); return total;
    }

    private PageResult<ApprovalTaskSummary> enrichTaskUserNames(PageResult<ApprovalTaskSummary> page) {
        enrichTaskUserNames(page.getList());
        return page;
    }

    private void enrichTaskUserNames(List<ApprovalTaskSummary> rows) {
        Set<Long> userIds = new LinkedHashSet<>();
        rows.forEach(row -> {
            if (row.getInitiatorUserId() != null) {
                userIds.add(row.getInitiatorUserId());
            }
            if (row.getAssigneeUserId() != null) {
                userIds.add(row.getAssigneeUserId());
            }
        });
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = Objects.requireNonNull(adminUserApi.getUserMap(userIds),
                "APPROVAL_TASK_USER_MAP_REQUIRED");
        rows.forEach(row -> {
            row.setInitiatorUserName(resolveTaskUserName(userMap, row.getInitiatorUserId()));
            row.setAssigneeUserName(resolveTaskUserName(userMap, row.getAssigneeUserId()));
        });
    }

    private static String resolveTaskUserName(Map<Long, AdminUserRespDTO> userMap, Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = userMap.get(userId);
        return user == null ? resolveMissingUserName(userId) : resolveUserName(user);
    }

    private static String resolveMissingUserName(Long userId) {
        return "用户不存在(" + userId + ")";
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

    public static List<ApprovalTaskSummary> sortRows(List<ApprovalTaskSummary> rows,ApprovalTaskViewType view) {
        rows.forEach(row->row.setSortTimeMissing(sortTime(row,view)==null));
        return rows.stream().sorted((left,right)->compareRows(left,right,view)).toList();
    }
    public static int compareRows(ApprovalTaskSummary left,ApprovalTaskSummary right,ApprovalTaskViewType view) {
        int time=Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()).compare(sortTime(left,view),sortTime(right,view));
        if(time!=0) return time;
        int module=compareUtf8(Objects.requireNonNull(left.getModuleCode()).name(),Objects.requireNonNull(right.getModuleCode()).name());
        if(module!=0) return module;
        int type=compareUtf8(requireText(left.getSourceTaskType(),"APPROVAL_SORT_SOURCE_TYPE_REQUIRED"),
                requireText(right.getSourceTaskType(),"APPROVAL_SORT_SOURCE_TYPE_REQUIRED"));
        if(type!=0) return type;
        return compareUtf8(requireText(left.getSourceTaskId(),"APPROVAL_SORT_SOURCE_ID_REQUIRED"),
                requireText(right.getSourceTaskId(),"APPROVAL_SORT_SOURCE_ID_REQUIRED"));
    }

    private static int compareUtf8(String left,String right) {
        return Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8),right.getBytes(StandardCharsets.UTF_8));
    }

    private static LocalDateTime sortTime(ApprovalTaskSummary summary,ApprovalTaskViewType view) {
        return switch (view) {
            case TODO, CC -> summary.getTaskCreatedAt();
            case DONE -> summary.getTaskCompletedAt();
            case MY_INITIATED -> summary.getInitiatedAt();
        };
    }
}
