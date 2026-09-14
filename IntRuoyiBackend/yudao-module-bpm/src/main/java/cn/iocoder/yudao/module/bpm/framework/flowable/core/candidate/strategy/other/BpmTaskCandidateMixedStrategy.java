package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.other;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.string.StrUtils;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.dept.AbstractBpmTaskCandidateDeptLeaderStrategy;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.experimental.Accessors;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * 混合审批对象候选人策略，用于同一个或签节点中同时支持用户、角色、部门、发起人直属主管。
 */
@Component
public class BpmTaskCandidateMixedStrategy extends AbstractBpmTaskCandidateDeptLeaderStrategy {

    private static final Set<Integer> SUPPORTED_STRATEGIES = Set.of(
            BpmTaskCandidateStrategyEnum.USER.getStrategy(),
            BpmTaskCandidateStrategyEnum.ROLE.getStrategy(),
            BpmTaskCandidateStrategyEnum.DEPT_MEMBER.getStrategy(),
            BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy());

    @Resource
    private RoleApi roleApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    @Lazy
    private BpmProcessInstanceService processInstanceService;

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.MIXED;
    }

    @Override
    public void validateParam(String param) {
        parseEntries(param).forEach(this::validateEntry);
    }

    @Override
    public Set<Long> calculateUsers(String param) {
        return calculateUsers(param, null);
    }

    @Override
    public Set<Long> calculateUsersByTask(DelegateExecution execution, String param) {
        Long startUserId = getStartUserId(execution);
        return calculateUsers(param, startUserId);
    }

    @Override
    public Set<Long> calculateUsersByActivity(BpmnModel bpmnModel, String activityId, String param,
                                              Long startUserId, String processDefinitionId,
                                              Map<String, Object> processVariables) {
        return calculateUsers(param, startUserId);
    }

    private Set<Long> calculateUsers(String param, Long startUserId) {
        List<Entry> entries = parseEntries(param);
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        entries.forEach(entry -> userIds.addAll(calculateUsers(entry, startUserId)));
        return userIds;
    }

    private Set<Long> calculateUsers(Entry entry, Long startUserId) {
        validateEntry(entry);
        Integer strategy = entry.getStrategy();
        String param = entry.getParam();
        if (BpmTaskCandidateStrategyEnum.USER.getStrategy().equals(strategy)) {
            return StrUtils.splitToLongSet(param);
        }
        if (BpmTaskCandidateStrategyEnum.ROLE.getStrategy().equals(strategy)) {
            return permissionApi.getUserRoleIdListByRoleIds(StrUtils.splitToLongSet(param));
        }
        if (BpmTaskCandidateStrategyEnum.DEPT_MEMBER.getStrategy().equals(strategy)) {
            List<AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(StrUtils.splitToLongSet(param));
            return convertSet(users, AdminUserRespDTO::getId);
        }
        Assert.notNull(startUserId, "发起对象直属主管需要流程发起人");
        DeptRespDTO dept = getStartUserDept(startUserId);
        if (dept == null) {
            return new LinkedHashSet<>();
        }
        Long leaderUserId = getAssignLevelDeptLeaderId(dept, Integer.parseInt(param));
        return leaderUserId == null ? new LinkedHashSet<>() : new LinkedHashSet<>(Set.of(leaderUserId));
    }

    private Long getStartUserId(DelegateExecution execution) {
        ProcessInstance processInstance = processInstanceService.getProcessInstance(execution.getProcessInstanceId());
        Assert.notNull(processInstance, "流程实例({})不存在", execution.getProcessInstanceId());
        return NumberUtils.parseLong(processInstance.getStartUserId());
    }

    private void validateEntry(Entry entry) {
        Assert.notNull(entry, "混合审批对象不能为空");
        Assert.notNull(entry.getStrategy(), "混合审批对象类型不能为空");
        Assert.isTrue(SUPPORTED_STRATEGIES.contains(entry.getStrategy()), "混合审批对象类型({})不支持", entry.getStrategy());
        Assert.notEmpty(entry.getParam(), "混合审批对象参数不能为空");
        Integer strategy = entry.getStrategy();
        String param = entry.getParam();
        if (BpmTaskCandidateStrategyEnum.USER.getStrategy().equals(strategy)) {
            adminUserApi.validateUserList(StrUtils.splitToLongSet(param));
        } else if (BpmTaskCandidateStrategyEnum.ROLE.getStrategy().equals(strategy)) {
            roleApi.validRoleList(StrUtils.splitToLongSet(param));
        } else if (BpmTaskCandidateStrategyEnum.DEPT_MEMBER.getStrategy().equals(strategy)) {
            deptApi.validateDeptList(StrUtils.splitToLongSet(param));
        } else if (BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy().equals(strategy)) {
            Assert.isTrue(StrUtil.isNotBlank(param) && Integer.parseInt(param) > 0, "发起对象直属主管层级必须大于 0");
        }
    }

    private List<Entry> parseEntries(String param) {
        List<Entry> entries = JsonUtils.parseArray(param, Entry.class);
        Assert.isTrue(CollUtil.isNotEmpty(entries), "混合审批对象不能为空");
        return entries;
    }

    @Data
    @Accessors(chain = true)
    public static class Entry {

        private Integer strategy;

        private String param;

    }

}
