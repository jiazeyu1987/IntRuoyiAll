package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE;

@Service
public class MesProEdhrBatchExecutionVisibilityService {

    @Resource private MesMarketReleaseAccess marketReleaseAccess;

    private static final String FORM_SLOT_MAIN = "MAIN";

    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;
    @Resource
    private MesProEdhrCandidateResolver candidateResolver;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrBatchExecutionOriginMapper batchOriginMapper;
    @Resource
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Resource
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;

    public boolean hasOverviewPermission(Long currentUserId) {
        return currentUserId == null || currentUserId <= 0
                || permissionApi.hasAnyPermissions(currentUserId, MesProEdhrBatchTaskVisibilityService.OVERVIEW_PERMISSION);
    }

    public boolean canViewBatch(MesProEdhrBatchExecutionDO batch, Long currentUserId) {
        return canViewBatch(batch, batchTaskMapper.selectListByBatchExecutionId(batch.getId()), currentUserId);
    }

    public boolean canViewBatch(MesProEdhrBatchExecutionDO batch,
                                List<MesProEdhrBatchExecutionTaskDO> tasks,
                                Long currentUserId) {
        return canViewBatch(batch, tasks, currentUserId, marketReleaseAccess.get(batch.getId(), currentUserId));
    }

    public boolean canViewBatch(MesProEdhrBatchExecutionDO batch,
                                List<MesProEdhrBatchExecutionTaskDO> tasks,
                                Long currentUserId, MesMarketReleaseAccess.Access releaseAccess) {
        return canViewBatch(batch, tasks, currentUserId, releaseAccess, new VisibilityChecks(
                () -> hasOverviewPermission(currentUserId),
                item -> isCurrentManagerReleaseCandidate(item, currentUserId),
                this::resolveFormalActiveOrder,
                (item, active) -> isFormalPqcReleaseCandidate(item, active, currentUserId),
                this::resolveCurrentProcessFillerUserIdMap));
    }

    private boolean canViewBatch(MesProEdhrBatchExecutionDO batch,
                                 List<MesProEdhrBatchExecutionTaskDO> tasks, Long currentUserId,
                                 MesMarketReleaseAccess.Access releaseAccess, VisibilityChecks checks) {
        if(!releaseAccess.visible()) return false;
        if(releaseAccess.original() || Objects.equals(releaseAccess.currentUserId(),currentUserId) && currentUserId!=null) return true;
        if (checks.overview().get()) {
            return true;
        }
        if (checks.manager().test(batch)) {
            return true;
        }
        MesProcessPoolActiveOrderDO active = checks.active().apply(batch);
        if (active != null && Objects.equals(active.getLeaderUserId(), currentUserId)) {
            return true;
        }
        if (active != null && checks.pqc().test(batch, active)) {
            return true;
        }
        return canViewCurrentProcess(tasks, currentUserId, checks.fillers());
    }

    private boolean canViewCurrentProcess(List<MesProEdhrBatchExecutionTaskDO> tasks, Long currentUserId,
            java.util.function.Function<MesProEdhrBatchExecutionTaskDO, Map<String, List<Long>>> fillers) {
        MesProEdhrBatchExecutionTaskDO currentProcessTask = resolveCurrentProcessTask(tasks);
        if (currentProcessTask == null) {
            return false;
        }
        Map<String, List<Long>> fillerUserIdMap;
        try {
            fillerUserIdMap = fillers.apply(currentProcessTask);
        } catch (ServiceException ex) {
            if (isDefaultReportRequired(ex)) {
                return false;
            }
            throw ex;
        }
        return fillerUserIdMap.values().stream()
                .flatMap(List::stream)
                .anyMatch(userId -> Objects.equals(userId, currentUserId));
    }

    public List<MesProEdhrBatchExecutionDO> filterVisible(List<MesProEdhrBatchExecutionDO> batches, Long userId) {
        return newVisibilitySession(userId).filterVisible(batches);
    }

    /** Request-local only: reuse across candidate chunks, never across requests or tenants. */
    public VisibilitySession newVisibilitySession(Long userId) {
        return new VisibilitySession(userId);
    }

    public final class VisibilitySession {
        private final Long userId;
        private final Long tenantId = TenantContextHolder.getTenantId();
        private Boolean overview;
        private final Set<String> loadedReports = new LinkedHashSet<>();
        private final Map<String, List<MesProBatchRecordReportDO>> reports = new LinkedHashMap<>();
        private final Map<String, List<MesProEdhrProcessFormPermissionRuleDO>> rules = new LinkedHashMap<>();
        private final Map<RuleSource, MesProEdhrCandidateResolver.MesProEdhrCandidateContract> candidates =
                new LinkedHashMap<>();

        private VisibilitySession(Long userId) { this.userId = userId; }

        public List<MesProEdhrBatchExecutionDO> filterVisible(List<MesProEdhrBatchExecutionDO> batches) {
            if (!Objects.equals(tenantId, TenantContextHolder.getTenantId())) {
                throw new IllegalStateException("EDHR_VISIBILITY_SESSION_TENANT_CHANGED");
            }
            if (batches.isEmpty()) return List.of();
            List<Long> ids = batches.stream().map(MesProEdhrBatchExecutionDO::getId).toList();
            var access = marketReleaseAccess.getAll(ids, userId);
            if (overview == null) overview = hasOverviewPermission(userId);
            if (overview) {
                return batches.stream().filter(batch -> access.get(batch.getId()).visible()).toList();
            }
            // Follow the single-batch short-circuit order. Each next query only loads unresolved batches.
            Set<Long> visibleIds = new LinkedHashSet<>();
            List<MesProEdhrBatchExecutionDO> remaining = batches.stream().filter(batch -> {
                var item = access.get(batch.getId());
                if (!item.visible()) return false;
                if (item.original() || userId != null && Objects.equals(item.currentUserId(), userId)) {
                    visibleIds.add(batch.getId());
                    return false;
                }
                return true;
            }).toList();
            if (remaining.isEmpty()) return visibleInOrder(batches, visibleIds);
            ids = remaining.stream().map(MesProEdhrBatchExecutionDO::getId).toList();
            var transactions = releaseTransactionMapper.selectLatestByBatchExecutionIds(ids).stream()
                    .collect(Collectors.toMap(MesProEdhrReleaseTransactionDO::getBatchExecutionId, item -> item));
            Map<Long, List<MesProEdhrWorkTaskDO>> timeline = transactions.isEmpty() ? Map.of()
                    : workTaskMapper.selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                            .in(MesProEdhrWorkTaskDO::getBatchExecutionId, transactions.keySet())
                            .eq(MesProEdhrWorkTaskDO::getTaskType, "RELEASE_APPROVE")
                            .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "RELEASE_TRANSACTION")
                            .in(MesProEdhrWorkTaskDO::getBusinessScopeId, transactions.values().stream()
                                    .map(MesProEdhrReleaseTransactionDO::getId).toList())
                            .eq(MesProEdhrWorkTaskDO::getOwnershipLocked, true)
                            .in(MesProEdhrWorkTaskDO::getStatus, List.of("TODO", "DOING", "DONE"))
                            .orderByAsc(MesProEdhrWorkTaskDO::getCreateTime)
                            .orderByAsc(MesProEdhrWorkTaskDO::getId)).stream()
                            .collect(Collectors.groupingBy(MesProEdhrWorkTaskDO::getBatchExecutionId));
            remaining = remaining.stream().filter(batch -> {
                if (!isCurrentManagerReleaseCandidate(batch, userId, transactions.get(batch.getId()),
                        () -> timeline.getOrDefault(batch.getId(), List.of()))) return true;
                visibleIds.add(batch.getId());
                return false;
            }).toList();
            if (remaining.isEmpty()) return visibleInOrder(batches, visibleIds);
            ids = remaining.stream().map(MesProEdhrBatchExecutionDO::getId).toList();
            var origins = batchOriginMapper.selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionOriginDO>()
                    .in(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId, ids)
                    .orderByAsc(MesProEdhrBatchExecutionOriginDO::getId)).stream()
                    .collect(Collectors.groupingBy(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId));
            Set<Long> activeIds = origins.values().stream().flatMap(List::stream)
                    .map(MesProEdhrBatchExecutionOriginDO::getActiveOrderId).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, MesProcessPoolActiveOrderDO> activeOrders = activeIds.isEmpty() ? Map.of()
                    : activeOrderMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                            .in(MesProcessPoolActiveOrderDO::getId, activeIds)).stream()
                            .collect(Collectors.toMap(MesProcessPoolActiveOrderDO::getId, item -> item));
            Map<Long, MesProcessPoolActiveOrderDO> formalOrders = new LinkedHashMap<>();
            remaining = remaining.stream().filter(batch -> {
                var active = resolveFormalActiveOrder(batch,
                        id -> origins.getOrDefault(id, List.of()), activeOrders::get);
                if (active != null && Objects.equals(active.getLeaderUserId(), userId)) {
                    visibleIds.add(batch.getId());
                    return false;
                }
                if (active != null) formalOrders.put(batch.getId(), active);
                return true;
            }).toList();
            if (remaining.isEmpty()) return visibleInOrder(batches, visibleIds);
            if (!formalOrders.isEmpty()) {
                var applications = releaseApplicationMapper.selectList(
                        new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                                .in(MesProcessPoolActiveOrderReleaseApplicationDO::getBatchExecutionId,
                                        formalOrders.keySet())).stream()
                        .collect(Collectors.groupingBy(MesProcessPoolActiveOrderReleaseApplicationDO::getBatchExecutionId));
                Set<Long> pqcIds = applications.values().stream().flatMap(List::stream)
                        .map(MesProcessPoolActiveOrderReleaseApplicationDO::getPqcReleaseWorkTaskId)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                Map<Long, MesProEdhrWorkTaskDO> workById = pqcIds.isEmpty() ? Map.of()
                        : workTaskMapper.selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                                .in(MesProEdhrWorkTaskDO::getId, pqcIds)).stream()
                                .collect(Collectors.toMap(MesProEdhrWorkTaskDO::getId, item -> item));
                remaining = remaining.stream().filter(batch -> {
                    var active = formalOrders.get(batch.getId());
                    if (active == null || !isFormalPqcReleaseCandidate(batch, active, userId,
                            singleOrNull(applications.getOrDefault(batch.getId(), List.of())), workById::get)) return true;
                    visibleIds.add(batch.getId());
                    return false;
                }).toList();
            }
            if (remaining.isEmpty()) return visibleInOrder(batches, visibleIds);
            ids = remaining.stream().map(MesProEdhrBatchExecutionDO::getId).toList();
            var tasks = batchTaskMapper.selectVisibilityTasksByBatchExecutionIds(ids).stream()
                    .collect(Collectors.groupingBy(MesProEdhrBatchExecutionTaskDO::getBatchExecutionId));
            Set<String> newReports = tasks.values().stream().map(MesProEdhrBatchExecutionVisibilityService.this::resolveCurrentProcessTask)
                    .filter(Objects::nonNull).map(MesProEdhrBatchExecutionTaskDO::getBatchRecordReportId)
                    .filter(StrUtil::isNotBlank).filter(id -> !loadedReports.contains(id)).collect(Collectors.toSet());
            if (!newReports.isEmpty()) {
                reports.putAll(reportMapper.selectList(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                        .in(MesProBatchRecordReportDO::getReportId, newReports)).stream()
                        .collect(Collectors.groupingBy(MesProBatchRecordReportDO::getReportId)));
                rules.putAll(processFormPermissionRuleMapper.selectList(
                        new LambdaQueryWrapperX<MesProEdhrProcessFormPermissionRuleDO>()
                                .in(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordReportId, newReports)
                                .in(MesProEdhrProcessFormPermissionRuleDO::getRuleType,
                                        MesProEdhrProcessFormPermissionRuleMapper.PROCESS_FORM_FILL_RULE_TYPES)
                                .eq(MesProEdhrProcessFormPermissionRuleDO::getEnabled, true)
                                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getRuleType)
                                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getScopeKey)
                                .orderByDesc(MesProEdhrProcessFormPermissionRuleDO::getId)).stream()
                        .collect(Collectors.groupingBy(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordReportId)));
                loadedReports.addAll(newReports);
            }
            remaining.stream().filter(batch -> canViewCurrentProcess(
                    tasks.getOrDefault(batch.getId(), List.of()), userId,
                    task -> resolveCurrentProcessFillerUserIdMap(task,
                            id -> singleOrNull(reports.getOrDefault(id, List.of())), this::rulesForBinding,
                            this::candidateForRule)))
                    .forEach(batch -> visibleIds.add(batch.getId()));
            // Long scans must not retain every report encountered after its candidate window is consumed.
            if (loadedReports.size() > 512) {
                loadedReports.clear();
                reports.clear();
                rules.clear();
            }
            return visibleInOrder(batches, visibleIds);
        }

        private List<MesProEdhrBatchExecutionDO> visibleInOrder(
                List<MesProEdhrBatchExecutionDO> batches, Set<Long> visibleIds) {
            return batches.stream().filter(batch -> visibleIds.contains(batch.getId())).toList();
        }

        private MesProEdhrCandidateResolver.MesProEdhrCandidateContract candidateForRule(
                MesProEdhrProcessFormPermissionRuleDO rule) {
            RuleSource source = new RuleSource(rule.getCandidateSourceType(), rule.getCandidateSourceIds());
            var candidate = candidates.get(source);
            if (candidate != null) return candidate;
            candidate = candidateResolver.resolveProcessFormRule(rule);
            if (candidates.size() >= 512) candidates.clear();
            candidates.put(source, candidate);
            return candidate;
        }

        private List<MesProEdhrProcessFormPermissionRuleDO> rulesForBinding(
                MesProEdhrBatchExecutionTaskDO task, ResolvedTaskFormBinding binding) {
            List<MesProEdhrProcessFormPermissionRuleDO> all = rules.getOrDefault(binding.reportId(), List.of());
            List<MesProEdhrProcessFormPermissionRuleDO> selected = rulesInScope(all, task.getRouteProcessId(), binding.versionId());
            return selected.isEmpty() ? rulesInScope(all, task.getRouteProcessId(), null) : selected;
        }

        private List<MesProEdhrProcessFormPermissionRuleDO> rulesInScope(
                List<MesProEdhrProcessFormPermissionRuleDO> all, Long routeProcessId, Long versionId) {
            List<MesProEdhrProcessFormPermissionRuleDO> versionRules = all.stream()
                    .filter(rule -> Objects.equals(rule.getBatchRecordVersionId(), versionId)).toList();
            List<MesProEdhrProcessFormPermissionRuleDO> routeRules = versionRules.stream()
                    .filter(rule -> Objects.equals(rule.getRouteProcessId(), routeProcessId)).toList();
            return routeRules.isEmpty() ? versionRules.stream()
                    .filter(rule -> Objects.equals(rule.getRouteProcessId(),
                            MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID)).toList() : routeRules;
        }
    }

    private static <T> T singleOrNull(List<T> rows) {
        if (rows.size() > 1) throw new org.apache.ibatis.exceptions.TooManyResultsException(
                "Expected one visibility association, found " + rows.size());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private record RuleSource(String type, String ids) {}

    private record VisibilityChecks(java.util.function.Supplier<Boolean> overview,
            java.util.function.Predicate<MesProEdhrBatchExecutionDO> manager,
            java.util.function.Function<MesProEdhrBatchExecutionDO, MesProcessPoolActiveOrderDO> active,
            java.util.function.BiPredicate<MesProEdhrBatchExecutionDO, MesProcessPoolActiveOrderDO> pqc,
            java.util.function.Function<MesProEdhrBatchExecutionTaskDO, Map<String, List<Long>>> fillers) {}

    private MesProcessPoolActiveOrderDO resolveFormalActiveOrder(MesProEdhrBatchExecutionDO batch) {
        return resolveFormalActiveOrder(batch, id -> batchOriginMapper.selectListByBatchExecutionId(id),
                id -> activeOrderMapper.selectById(id));
    }

    private MesProcessPoolActiveOrderDO resolveFormalActiveOrder(MesProEdhrBatchExecutionDO batch,
            java.util.function.Function<Long, List<MesProEdhrBatchExecutionOriginDO>> originsByBatch,
            java.util.function.Function<Long, MesProcessPoolActiveOrderDO> activeById) {
        if (batch == null || batch.getId() == null || batch.getId() <= 0
                || batch.getTenantId() == null
                || !Objects.equals(batch.getTenantId(), TenantContextHolder.getTenantId())
                || batch.getWorkOrderId() == null || batch.getWorkOrderId() <= 0
                || batch.getRouteId() == null || batch.getRouteId() <= 0
                || batch.getRouteVersionId() == null || batch.getRouteVersionId() <= 0
                || Boolean.TRUE.equals(batch.getDeleted())) {
            return null;
        }
        List<MesProEdhrBatchExecutionOriginDO> origins = Objects.requireNonNull(
                originsByBatch.apply(batch.getId()),
                "EDHR_BATCH_OWNER_ORIGINS_REQUIRED: batchId=" + batch.getId());
        if (origins.isEmpty()) {
            return null;
        }
        MesProEdhrBatchExecutionOriginDO first = origins.get(0);
        Long activeOrderId = first == null ? null : first.getActiveOrderId();
        // Multiple material origins may share one active order; every association must agree.
        if (activeOrderId == null || activeOrderId <= 0 || origins.stream().anyMatch(origin ->
                origin == null || origin.getId() == null || origin.getId() <= 0
                        || Boolean.TRUE.equals(origin.getDeleted())
                        || !Objects.equals(origin.getTenantId(), batch.getTenantId())
                        || !Objects.equals(origin.getBatchExecutionId(), batch.getId())
                        || !Objects.equals(origin.getWorkOrderId(), batch.getWorkOrderId())
                        || !Objects.equals(origin.getActiveOrderId(), activeOrderId))) {
            return null;
        }
        var active = activeById.apply(activeOrderId);
        // The persisted holder owns this batch's read scope, including after completion/release.
        // Current route candidates, creators and form/task assignments are separate contracts.
        return active != null && !Boolean.TRUE.equals(active.getDeleted())
                && Objects.equals(active.getId(), activeOrderId)
                && Objects.equals(active.getTenantId(), batch.getTenantId())
                && Objects.equals(active.getWorkOrderId(), batch.getWorkOrderId())
                && Objects.equals(active.getRouteId(), batch.getRouteId())
                && Objects.equals(active.getRouteVersionId(), batch.getRouteVersionId()) ? active : null;
    }

    private boolean isFormalPqcReleaseCandidate(MesProEdhrBatchExecutionDO batch,
                                               MesProcessPoolActiveOrderDO active, Long userId) {
        var application = releaseApplicationMapper.selectByBatchExecutionId(batch.getId());
        return isFormalPqcReleaseCandidate(batch, active, userId, application, id -> workTaskMapper.selectById(id));
    }

    private boolean isFormalPqcReleaseCandidate(MesProEdhrBatchExecutionDO batch,
            MesProcessPoolActiveOrderDO active, Long userId,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            java.util.function.Function<Long, MesProEdhrWorkTaskDO> taskById) {
        if (application == null || application.getId() == null || application.getId() <= 0
                || Boolean.TRUE.equals(application.getDeleted())
                || !Objects.equals(application.getTenantId(), batch.getTenantId())
                || !Objects.equals(application.getBatchExecutionId(), batch.getId())
                || !Objects.equals(application.getActiveOrderId(), active.getId())
                || !Objects.equals(application.getWorkOrderId(), batch.getWorkOrderId())
                || !Objects.equals(application.getRouteId(), batch.getRouteId())
                || !Objects.equals(application.getRouteVersionId(), batch.getRouteVersionId())
                || application.getPqcReleaseWorkTaskId() == null || application.getPqcReleaseWorkTaskId() <= 0) {
            return false;
        }
        var task = taskById.apply(application.getPqcReleaseWorkTaskId());
        if (task == null || Boolean.TRUE.equals(task.getDeleted())
                || !Objects.equals(task.getId(), application.getPqcReleaseWorkTaskId())
                || task.getBatchExecutionId() != null && !Objects.equals(task.getBatchExecutionId(), batch.getId())
                || !Objects.equals(task.getWorkOrderId(), batch.getWorkOrderId())
                || !Objects.equals(task.getRouteId(), batch.getRouteId())
                || !"PQC_PRODUCTION_RELEASE".equals(task.getTaskType())
                || !"RELEASE_APPLICATION".equals(task.getBusinessScopeType())
                || !Objects.equals(task.getBusinessScopeId(), application.getId())
                || !Boolean.TRUE.equals(task.getOwnershipLocked())
                || !MesProEdhrWorkTaskAuthorization.containsCandidate(task.getCandidateUserSnapshot(), userId)) {
            return false;
        }
        if ("PQC_RELEASE_PENDING".equals(application.getApplicationStatus())) {
            return "TODO".equals(task.getStatus()) || "DOING".equals(task.getStatus())
                    || "OVERDUE".equals(task.getStatus());
        }
        // The original frozen responsibility remains readable after approval, including history.
        // This grants no upload, decision, form-fill or task-completion authority.
        return Objects.equals(task.getBatchExecutionId(), batch.getId())
                && "DONE".equals(task.getStatus()) && "APPROVE".equals(task.getReason())
                && "APPROVE".equals(application.getPqcDecision())
                && ("REPORT_UPLOAD_PENDING".equals(application.getApplicationStatus())
                    || "MANAGER_RELEASE_PENDING".equals(application.getApplicationStatus())
                    || "RELEASED".equals(application.getApplicationStatus()));
    }

    private boolean isCurrentManagerReleaseCandidate(MesProEdhrBatchExecutionDO batch, Long userId) {
        var transaction = releaseTransactionMapper.selectByBatchExecutionId(batch.getId());
        return isCurrentManagerReleaseCandidate(batch, userId, transaction,
                () -> workTaskMapper.selectTimelineListByBatchExecutionId(batch.getId()));
    }

    private boolean isCurrentManagerReleaseCandidate(MesProEdhrBatchExecutionDO batch, Long userId,
            MesProEdhrReleaseTransactionDO transaction,
            java.util.function.Supplier<List<MesProEdhrWorkTaskDO>> timeline) {
        if (transaction == null || transaction.getId() == null
                || !Objects.equals(transaction.getBatchExecutionId(), batch.getId())) {
            return false;
        }
        return timeline.get().stream()
                .filter(task -> Objects.equals(task.getBatchExecutionId(), batch.getId()))
                .filter(task -> "RELEASE_APPROVE".equals(task.getTaskType()))
                .filter(task -> "RELEASE_TRANSACTION".equals(task.getBusinessScopeType()))
                .filter(task -> Objects.equals(task.getBusinessScopeId(), transaction.getId()))
                .filter(task -> Boolean.TRUE.equals(task.getOwnershipLocked()))
                .filter(task -> "TODO".equals(task.getStatus()) || "DOING".equals(task.getStatus())
                        || "DONE".equals(task.getStatus()))
                .anyMatch(task -> MesProEdhrWorkTaskAuthorization.containsCandidate(
                        task.getCandidateUserSnapshot(), userId));
    }

    private boolean isDefaultReportRequired(ServiceException ex) {
        return Objects.equals(ex.getCode(), PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED.getCode());
    }
    public void requireVisibleBatch(MesProEdhrBatchExecutionDO batch, Long currentUserId) {
        if (!canViewBatch(batch, currentUserId)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE);
        }
    }

    public void requireVisibleBatch(MesProEdhrBatchExecutionDO batch,
                                    List<MesProEdhrBatchExecutionTaskDO> tasks,
                                    Long currentUserId) {
        if (!canViewBatch(batch, tasks, currentUserId)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE);
        }
    }

    public MesProEdhrBatchExecutionTaskDO resolveCurrentProcessTask(List<MesProEdhrBatchExecutionTaskDO> tasks) {
        List<MesProEdhrBatchExecutionTaskDO> routeTasks = (tasks == null ? List.<MesProEdhrBatchExecutionTaskDO>of() : tasks)
                .stream()
                .filter(this::isRouteForm)
                .filter(task -> !Boolean.FALSE.equals(task.getRequiredFlag()))
                .toList();
        if (routeTasks.isEmpty()) {
            return null;
        }
        MesProEdhrBatchExecutionTaskDO currentTask = routeTasks.stream()
                .filter(task -> !isTaskApproved(task))
                .filter(task -> !Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING))
                .findFirst()
                .or(() -> routeTasks.stream().filter(task -> !isTaskApproved(task)).findFirst())
                .or(() -> routeTasks.stream().reduce((previous, current) -> current))
                .orElse(null);
        if (currentTask == null || FORM_SLOT_MAIN.equals(currentTask.getFormSlotType())) {
            return currentTask;
        }
        return routeTasks.stream()
                .filter(task -> Objects.equals(task.getRouteProcessId(), currentTask.getRouteProcessId()))
                .filter(task -> FORM_SLOT_MAIN.equals(task.getFormSlotType()))
                .min(java.util.Comparator.comparing(MesProEdhrBatchExecutionTaskDO::getBatchRecordSort,
                        java.util.Comparator.nullsLast(Integer::compareTo)))
                .orElse(currentTask);
    }

    public Map<String, List<EdhrBatchExecutionRespVO.CurrentProcessFiller>> resolveCurrentProcessFillerMap(
            MesProEdhrBatchExecutionTaskDO currentProcessTask) {
        Map<String, List<Long>> ruleUserIdMap = resolveCurrentProcessFillerUserIdMap(currentProcessTask);
        Set<Long> userIds = ruleUserIdMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? Map.of()
                : Objects.requireNonNull(adminUserApi.getUserMap(userIds),
                "EDHR_CURRENT_PROCESS_FILLER_USER_MAP_REQUIRED: admin user map is required");
        Map<String, List<EdhrBatchExecutionRespVO.CurrentProcessFiller>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> entry : ruleUserIdMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream()
                    .map(userId -> new EdhrBatchExecutionRespVO.CurrentProcessFiller()
                            .setUserId(userId)
                            .setDisplayName(resolveFillableUserDisplayName(userMap, userId)))
                    .toList());
        }
        return result;
    }

    private Map<String, List<Long>> resolveCurrentProcessFillerUserIdMap(
            MesProEdhrBatchExecutionTaskDO currentProcessTask) {
        return resolveCurrentProcessFillerUserIdMap(currentProcessTask, id -> reportMapper.selectByReportId(id),
                (task, binding) -> {
                    List<MesProEdhrProcessFormPermissionRuleDO> rules =
                            processFormPermissionRuleMapper.selectEnabledFillRulesForRouteOrReport(
                                    task.getRouteProcessId(), binding.reportId(), binding.versionId());
                    if (rules.isEmpty()) {
                        // Existing read contract also exposes null-version rules; dispatch stays version-strict.
                        rules = processFormPermissionRuleMapper.selectEnabledFillRulesForRouteOrReport(
                                task.getRouteProcessId(), binding.reportId());
                    }
                    return rules;
                }, rule -> candidateResolver.resolveProcessFormRule(rule));
    }

    private Map<String, List<Long>> resolveCurrentProcessFillerUserIdMap(
            MesProEdhrBatchExecutionTaskDO currentProcessTask,
            java.util.function.Function<String, MesProBatchRecordReportDO> reportById,
            java.util.function.BiFunction<MesProEdhrBatchExecutionTaskDO, ResolvedTaskFormBinding,
                    List<MesProEdhrProcessFormPermissionRuleDO>> rulesForBinding,
            java.util.function.Function<MesProEdhrProcessFormPermissionRuleDO,
                    MesProEdhrCandidateResolver.MesProEdhrCandidateContract> candidateForRule) {
        if (currentProcessTask == null || currentProcessTask.getRouteProcessId() == null
                || StrUtil.isBlank(currentProcessTask.getBatchRecordReportId())) {
            return Map.of();
        }
        ResolvedTaskFormBinding resolvedBinding = resolveFrozenTaskFormBinding(currentProcessTask,
                reportById.apply(currentProcessTask.getBatchRecordReportId()));
        List<MesProEdhrProcessFormPermissionRuleDO> rules = rulesForBinding.apply(currentProcessTask, resolvedBinding);
        Map<String, MesProEdhrProcessFormPermissionRuleDO> latestRuleMap = rules.stream()
                .collect(Collectors.toMap(MesProEdhrProcessFormPermissionRuleDO::getRuleType,
                        rule -> rule, (first, ignored) -> first, LinkedHashMap::new));
        Map<String, List<Long>> ruleUserIdMap = new LinkedHashMap<>();
        for (Map.Entry<String, MesProEdhrProcessFormPermissionRuleDO> entry : latestRuleMap.entrySet()) {
            MesProEdhrCandidateResolver.MesProEdhrCandidateContract candidate =
                    candidateForRule.apply(entry.getValue());
            ruleUserIdMap.put(entry.getKey(), parseCommaSeparatedIds(candidate.userSnapshot(),
                    "EDHR_CURRENT_PROCESS_FILLER_SNAPSHOT_INVALID: ruleId=" + entry.getValue().getId()));
        }
        return ruleUserIdMap;
    }

    private ResolvedTaskFormBinding resolveFrozenTaskFormBinding(MesProEdhrBatchExecutionTaskDO task,
                                                                 MesProBatchRecordReportDO boundReport) {
        if (boundReport == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        Long definitionId = task.getBatchRecordDefinitionId();
        Long versionId = task.getBatchRecordVersionId();
        if (definitionId == null || versionId == null
                || !Objects.equals(definitionId, boundReport.getBatchRecordDefinitionId())
                || !Objects.equals(versionId, boundReport.getBatchRecordVersionId())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED);
        }
        return new ResolvedTaskFormBinding(boundReport.getReportId(), versionId);
    }

    private List<Long> parseCommaSeparatedIds(String rawIds, String errorPrefix) {
        if (StrUtil.isBlank(rawIds)) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String item : rawIds.split(",")) {
            String token = StrUtil.trim(item);
            if (StrUtil.isBlank(token)) {
                continue;
            }
            try {
                ids.add(Long.valueOf(token));
            } catch (NumberFormatException ex) {
                throw new IllegalStateException(errorPrefix + ", token=" + token, ex);
            }
        }
        return List.copyOf(ids);
    }

    private String resolveFillableUserDisplayName(Map<Long, AdminUserRespDTO> userMap, Long userId) {
        AdminUserRespDTO user = userMap.get(userId);
        if (user == null) {
            return String.valueOf(userId);
        }
        return StrUtil.blankToDefault(user.getNickname(), String.valueOf(userId));
    }

    private boolean isTaskApproved(MesProEdhrBatchExecutionTaskDO task) {
        if (Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)) {
            return true;
        }
        if (!Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED)) {
            return false;
        }
        return !"SKIPPABLE_CONTROLLED".equals(task.getRequiredPolicy())
                || (task.getSkippedBy() != null && task.getSkippedAt() != null);
    }

    private boolean isRouteForm(MesProEdhrBatchExecutionTaskDO task) {
        return MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM.equals(resolveNodeType(task));
    }

    private String resolveNodeType(MesProEdhrBatchExecutionTaskDO task) {
        if (StrUtil.isNotBlank(task.getNodeType())) {
            return task.getNodeType();
        }
        return StrUtil.isBlank(task.getBatchRecordReportId())
                ? task.getProcessCode()
                : MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM;
    }

    private record ResolvedTaskFormBinding(String reportId,
                                           Long versionId) {
    }
}
