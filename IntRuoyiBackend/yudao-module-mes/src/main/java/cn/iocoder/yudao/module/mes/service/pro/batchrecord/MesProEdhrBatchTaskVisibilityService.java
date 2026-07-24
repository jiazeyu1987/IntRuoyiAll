package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MesProEdhrBatchTaskVisibilityService {

    public static final String OVERVIEW_PERMISSION = "mes:pro-edhr-batch-execution:overview";
    public static final String VISIBILITY_MODE_ALL = "ALL";
    public static final String VISIBILITY_MODE_ASSIGNED = "ASSIGNED";

    private static final String WORK_TASK_TYPE_FILL = "FILL";
    private static final String WORK_TASK_TYPE_CLOSE = "CLOSE";
    private static final String RULE_SCOPE_TYPE_ROUTE = "ROUTE";
    private static final String CANDIDATE_SOURCE_TYPE_USER = "USER";
    private static final String CANDIDATE_SOURCE_TYPE_ROLE_GROUP = "ROLE_GROUP";
    private static final String CANDIDATE_SOURCE_TYPE_DEPT_GROUP = "DEPT_GROUP";

    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper assignmentRuleMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;

    public VisibilityScope resolve(MesProEdhrBatchExecutionDO batch,
                                   List<MesProEdhrBatchExecutionTaskDO> tasks,
                                   Long currentUserId) {
        List<MesProEdhrBatchExecutionTaskDO> safeTasks = tasks == null ? List.of() : List.copyOf(tasks);
        if (currentUserId == null || currentUserId <= 0
                || permissionApi.hasAnyPermissions(currentUserId, OVERVIEW_PERMISSION)) {
            return VisibilityScope.all(safeTasks);
        }
        Map<Long, List<MesProEdhrWorkTaskDO>> workTasksByBatchTask =
                workTaskMapper.selectTimelineListByBatchExecutionId(batch.getId()).stream()
                        .filter(workTask -> workTask.getBatchTaskId() != null)
                        .collect(Collectors.groupingBy(MesProEdhrWorkTaskDO::getBatchTaskId));
        MesProEdhrWorkTaskAssignmentRuleDO closeRule = batch.getRouteId() == null ? null
                : assignmentRuleMapper.selectEnabledByScopeAndType(
                RULE_SCOPE_TYPE_ROUTE, batch.getRouteId(), WORK_TASK_TYPE_CLOSE);
        List<MesProEdhrBatchExecutionTaskDO> visibleTasks = safeTasks.stream()
                .filter(task -> isVisible(batch, task, currentUserId,
                        workTasksByBatchTask.getOrDefault(task.getId(), List.of()), closeRule))
                .toList();
        return VisibilityScope.assigned(visibleTasks);
    }

    private boolean isVisible(MesProEdhrBatchExecutionDO batch,
                              MesProEdhrBatchExecutionTaskDO task,
                              Long currentUserId,
                              List<MesProEdhrWorkTaskDO> historicalWorkTasks,
                              MesProEdhrWorkTaskAssignmentRuleDO closeRule) {
        if (Objects.equals(task.getOpenedBy(), currentUserId)) {
            return true;
        }
        if (historicalWorkTasks.stream().anyMatch(workTask -> isAssignedOrCandidate(workTask, currentUserId))) {
            return true;
        }
        MesProRouteProcessDO frozenRouteProcess = resolveFrozenRouteProcess(batch, task);
        if (frozenRouteProcess != null && frozenRouteProcess.getId() != null) {
            MesProEdhrWorkTaskAssignmentRuleDO fillRule =
                    assignmentRuleMapper.selectEnabledByRouteProcessAndType(
                            frozenRouteProcess.getId(), WORK_TASK_TYPE_FILL);
            if (matchesRule(fillRule, currentUserId)) {
                return true;
            }
        }
        return task.getBatchRecordReportId() == null && matchesRule(closeRule, currentUserId);
    }

    private boolean isAssignedOrCandidate(MesProEdhrWorkTaskDO workTask, Long currentUserId) {
        return MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(workTask, currentUserId);
    }

    private boolean matchesRule(MesProEdhrWorkTaskAssignmentRuleDO rule, Long currentUserId) {
        if (rule == null) {
            return false;
        }
        String sourceType = StrUtil.blankToDefault(rule.getCandidateSourceType(), CANDIDATE_SOURCE_TYPE_USER);
        Long sourceId = rule.getCandidateSourceId() == null ? rule.getAssigneeUserId() : rule.getCandidateSourceId();
        if (sourceId == null) {
            return false;
        }
        if (CANDIDATE_SOURCE_TYPE_USER.equals(sourceType)) {
            return Objects.equals(sourceId, currentUserId);
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE_GROUP.equals(sourceType)) {
            Set<Long> userIds = Objects.requireNonNull(
                    permissionApi.getUserRoleIdListByRoleIds(Set.of(sourceId)),
                    "EDHR_VISIBILITY_ROLE_USER_IDS_REQUIRED: role user ids are required");
            return userIds.contains(currentUserId);
        }
        if (CANDIDATE_SOURCE_TYPE_DEPT_GROUP.equals(sourceType)) {
            List<AdminUserRespDTO> users = Objects.requireNonNull(
                    adminUserApi.getUserListByDeptIds(Set.of(sourceId)),
                    "EDHR_VISIBILITY_DEPT_USERS_REQUIRED: dept users are required");
            return users.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(user -> Objects.equals(user.getId(), currentUserId)
                            && CommonStatusEnum.isEnable(user.getStatus()));
        }
        throw new IllegalStateException("EDHR_VISIBILITY_RULE_SOURCE_INVALID: ruleId=" + rule.getId()
                + ", sourceType=" + sourceType);
    }

    private MesProRouteProcessDO resolveFrozenRouteProcess(MesProEdhrBatchExecutionDO batch,
                                                           MesProEdhrBatchExecutionTaskDO task) {
        if (batch == null || batch.getRouteId() == null || task == null
                || (task.getRouteProcessId() == null && task.getProcessId() == null)) {
            return null;
        }
        return routeProcessService.resolveFrozenRouteProcess(
                task.getRouteProcessId(), batch.getRouteId(), task.getProcessId());
    }

    public record VisibilityScope(String mode,
                                  List<MesProEdhrBatchExecutionTaskDO> tasks,
                                  Set<Long> taskIds) {

        private static VisibilityScope all(List<MesProEdhrBatchExecutionTaskDO> tasks) {
            return new VisibilityScope(VISIBILITY_MODE_ALL, tasks, taskIds(tasks));
        }

        private static VisibilityScope assigned(List<MesProEdhrBatchExecutionTaskDO> tasks) {
            return new VisibilityScope(VISIBILITY_MODE_ASSIGNED, tasks, taskIds(tasks));
        }

        private static Set<Long> taskIds(List<MesProEdhrBatchExecutionTaskDO> tasks) {
            return tasks.stream()
                    .map(MesProEdhrBatchExecutionTaskDO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        public boolean canView(Long taskId) {
            return taskId != null && taskIds.contains(taskId);
        }
    }
}
