package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.expression;

import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import jakarta.annotation.Resource;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;

/**
 * 分配给发起人的 Leader 审批的 Expression 流程表达式
 * 目前 Leader 的定义是，发起人所在部门的 Leader
 *
 * @author 瑛泰源码
 */
@Component
@Deprecated // 仅仅是表达式的示例，建议使用 BpmTaskCandidateStartUserDeptLeaderStrategy 替代
public class BpmTaskAssignLeaderExpression {

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Resource
    private BpmProcessInstanceService processInstanceService;

    /**
     * 计算审批的候选人
     *
     * @param execution 流程执行实体
     * @param level 指定级别
     * @return 指定级别的领导
     */
    public Set<Long> calculateUsers(DelegateExecution execution, int level) {
        Assert.isTrue(level > 0, "level 必须大于 0");
        // 获得发起人
        ProcessInstance processInstance = processInstanceService.getProcessInstance(execution.getProcessInstanceId());
        Long startUserId = NumberUtils.parseLong(processInstance.getStartUserId());
        DeptRespDTO dept = getStartUserDept(startUserId);
        Set<Long> visited = new java.util.HashSet<>();
        for (int i = 1; i < level; i++) {
            Assert.isTrue(visited.add(dept.getId()), "部门层级存在循环");
            Assert.isTrue(dept.getParentId() != null && dept.getParentId() != 0,
                    "审批配置的部门层级超过组织根部门");
            dept = deptApi.getDept(dept.getParentId());
            Assert.notNull(dept, "上级部门不存在，无法确定审批主管");
        }
        return asSet(deptApi.requireSupervisor(dept.getId(), startUserId).getUserId());
    }

    private DeptRespDTO getStartUserDept(Long startUserId) {
        AdminUserRespDTO startUser = adminUserApi.getUser(startUserId);
        Assert.notNull(startUser, "申请人不存在，无法确定审批主管");
        Assert.notNull(startUser.getDeptId(), "申请人未分配部门，无法确定审批主管");
        DeptRespDTO dept = deptApi.getDept(startUser.getDeptId());
        Assert.notNull(dept, "申请人的部门不存在，无法确定审批主管");
        return dept;
    }

}
