package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.dept;

import cn.iocoder.yudao.framework.common.util.string.StrUtils;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class BpmTaskCandidateDeptLeaderStrategy extends AbstractBpmTaskCandidateDeptLeaderStrategy {
    @Resource @Lazy private BpmProcessInstanceService processInstanceService;
    @Override public BpmTaskCandidateStrategyEnum getStrategy() { return BpmTaskCandidateStrategyEnum.DEPT_LEADER; }
    @Override public void validateParam(String param) { deptApi.validateDeptList(StrUtils.splitToLongSet(param)); }
    @Override public Set<Long> calculateUsers(String param) { return resolve(param, null); }
    private Set<Long> resolve(String param, Long startUserId) {
        Set<Long> result = new LinkedHashSet<>();
        for (Long deptId : StrUtils.splitToLongSet(param)) {
            result.add(deptApi.requireSupervisor(deptId, startUserId).getUserId());
        }
        cn.hutool.core.lang.Assert.notEmpty(result, "未指定部门，无法确定审批主管");
        return result;
    }
    @Override public Set<Long> calculateUsersByTask(DelegateExecution execution, String param) {
        var process = processInstanceService.getProcessInstance(execution.getProcessInstanceId());
        cn.hutool.core.lang.Assert.notNull(process, "流程实例不存在");
        return resolve(param, Long.valueOf(process.getStartUserId()));
    }
    @Override public Set<Long> calculateUsersByActivity(BpmnModel model, String activityId, String param,
            Long startUserId, String definitionId, Map<String, Object> variables) {
        return resolve(param, startUserId);
    }
}
