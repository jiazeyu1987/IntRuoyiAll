package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.dept;

import cn.hutool.core.lang.Assert;
import cn.iocoder.yudao.framework.common.util.string.StrUtils;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 连续多级部门的负责人 {@link BpmTaskCandidateStrategy} 实现类
 *
 * @author jason
 */
@Component
public class BpmTaskCandidateDeptLeaderMultiStrategy extends AbstractBpmTaskCandidateDeptLeaderStrategy {
    @jakarta.annotation.Resource @org.springframework.context.annotation.Lazy
    private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService processInstanceService;

    @Override
    public Set<Long> calculateUsersByTask(org.flowable.engine.delegate.DelegateExecution execution, String param) {
        var process = processInstanceService.getProcessInstance(execution.getProcessInstanceId());
        Assert.notNull(process, "流程实例不存在");
        return resolve(param, Long.valueOf(process.getStartUserId()));
    }

    @Override
    public Set<Long> calculateUsersByActivity(org.flowable.bpmn.model.BpmnModel model, String activityId,
            String param, Long startUserId, String definitionId, java.util.Map<String, Object> variables) {
        return resolve(param, startUserId);
    }


    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.MULTI_DEPT_LEADER_MULTI;
    }

    @Override
    public void validateParam(String param) {
        // 参数格式: | 分隔：1）左边为部门（多个部门用 , 分隔）。2）右边为部门层级
        String[] params = param.split("\\|");
        Assert.isTrue(params.length == 2, "参数格式不匹配");
        List<Long> deptIds = StrUtils.splitToLong(params[0], ",");
        int level = Integer.parseInt(params[1]);
        // 校验部门存在
        deptApi.validateDeptList(deptIds);
        Assert.isTrue(level > 0, "部门层级必须大于 0");
    }

    @Override
    public Set<Long> calculateUsers(String param) {
        return resolve(param, null);
    }

    private Set<Long> resolve(String param, Long startUserId) {
        String[] params = param.split("\\|");
        List<Long> deptIds = StrUtils.splitToLong(params[0], ",");
        int level = Integer.parseInt(params[1]);
        return super.getMultiLevelDeptLeaderIds(deptIds, level, startUserId);
    }

}
