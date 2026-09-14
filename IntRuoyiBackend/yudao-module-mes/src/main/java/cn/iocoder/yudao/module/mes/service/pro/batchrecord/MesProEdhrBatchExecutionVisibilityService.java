package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
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
        if (hasOverviewPermission(currentUserId)) {
            return true;
        }
        MesProEdhrBatchExecutionTaskDO currentProcessTask = resolveCurrentProcessTask(tasks);
        if (currentProcessTask == null) {
            return false;
        }
        Map<String, List<Long>> fillerUserIdMap;
        try {
            fillerUserIdMap = resolveCurrentProcessFillerUserIdMap(currentProcessTask);
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
        if (currentProcessTask == null || currentProcessTask.getRouteProcessId() == null
                || StrUtil.isBlank(currentProcessTask.getBatchRecordReportId())) {
            return Map.of();
        }
        ResolvedTaskFormBinding resolvedBinding = resolveFrozenTaskFormBinding(currentProcessTask);
        List<MesProEdhrProcessFormPermissionRuleDO> rules =
                processFormPermissionRuleMapper.selectEnabledFillRulesForRouteOrReport(
                        currentProcessTask.getRouteProcessId(), resolvedBinding.reportId(),
                        resolvedBinding.versionId());
        if (rules.isEmpty()) {
            // Read-only current-filler display keeps legacy null-version rules visible.
            // Fill task dispatch and entitlement synchronization remain version-strict.
            rules = processFormPermissionRuleMapper.selectEnabledFillRulesForRouteOrReport(
                    currentProcessTask.getRouteProcessId(), resolvedBinding.reportId());
        }
        Map<String, MesProEdhrProcessFormPermissionRuleDO> latestRuleMap = rules.stream()
                .collect(Collectors.toMap(MesProEdhrProcessFormPermissionRuleDO::getRuleType,
                        rule -> rule, (first, ignored) -> first, LinkedHashMap::new));
        Map<String, List<Long>> ruleUserIdMap = new LinkedHashMap<>();
        for (Map.Entry<String, MesProEdhrProcessFormPermissionRuleDO> entry : latestRuleMap.entrySet()) {
            MesProEdhrCandidateResolver.MesProEdhrCandidateContract candidate =
                    candidateResolver.resolveProcessFormRule(entry.getValue());
            ruleUserIdMap.put(entry.getKey(), parseCommaSeparatedIds(candidate.userSnapshot(),
                    "EDHR_CURRENT_PROCESS_FILLER_SNAPSHOT_INVALID: ruleId=" + entry.getValue().getId()));
        }
        return ruleUserIdMap;
    }

    private ResolvedTaskFormBinding resolveFrozenTaskFormBinding(MesProEdhrBatchExecutionTaskDO task) {
        MesProBatchRecordReportDO boundReport = reportMapper.selectByReportId(task.getBatchRecordReportId());
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
