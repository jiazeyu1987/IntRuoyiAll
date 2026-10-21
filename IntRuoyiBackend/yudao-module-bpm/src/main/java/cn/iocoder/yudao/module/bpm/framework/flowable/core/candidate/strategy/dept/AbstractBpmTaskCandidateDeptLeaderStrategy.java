package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.dept;

import cn.hutool.core.lang.Assert;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import java.util.*;

/** 所有部门主管策略共用组织模块的实时解析规则。 */
public abstract class AbstractBpmTaskCandidateDeptLeaderStrategy implements BpmTaskCandidateStrategy {
    @Resource protected DeptApi deptApi;
    @Resource protected AdminUserApi adminUserApi;

    protected Long getAssignLevelDeptLeaderId(DeptRespDTO dept, Integer level, Long excludedUserId) {
        Assert.isTrue(level > 0, "部门层级必须大于 0");
        Assert.notNull(dept, "部门不存在，无法确定审批主管");
        Set<Long> visited = new HashSet<>();
        DeptRespDTO current = dept;
        for (int i = 1; i < level; i++) {
            Assert.isTrue(visited.add(current.getId()), "部门层级存在循环");
            Assert.isTrue(current.getParentId() != null && current.getParentId() != 0,
                    "审批配置的部门层级超过组织根部门，请修改审批配置");
            current = deptApi.getDept(current.getParentId());
            Assert.notNull(current, "上级部门不存在，无法确定审批主管");
        }
        return deptApi.requireSupervisor(current.getId(), excludedUserId).getUserId();
    }

    protected Set<Long> getMultiLevelDeptLeaderIds(List<Long> deptIds, Integer level, Long excludedUserId) {
        Assert.isTrue(level > 0, "部门层级必须大于 0");
        Assert.notEmpty(deptIds, "未指定部门，无法确定审批主管");
        Set<Long> result = new LinkedHashSet<>();
        for (Long deptId : deptIds) {
            DeptRespDTO dept = deptApi.getDept(deptId);
            Assert.notNull(dept, "部门不存在，无法确定审批主管");
            Set<Long> visited = new HashSet<>();
            for (int i = 0; i < level; i++) {
                Assert.isTrue(visited.add(dept.getId()), "部门层级存在循环");
                result.add(deptApi.requireSupervisor(dept.getId(), excludedUserId).getUserId());
                if (dept.getParentId() == null || dept.getParentId() == 0) break;
                dept = deptApi.getDept(dept.getParentId());
                Assert.notNull(dept, "上级部门不存在，无法确定审批主管");
            }
        }
        return result;
    }

    protected DeptRespDTO getStartUserDept(Long startUserId) {
        var user = adminUserApi.getUser(startUserId);
        Assert.notNull(user, "申请人不存在，无法确定审批主管");
        Assert.notNull(user.getDeptId(), "申请人未分配部门，无法确定审批主管");
        DeptRespDTO dept = deptApi.getDept(user.getDeptId());
        Assert.notNull(dept, "申请人的部门不存在，无法确定审批主管");
        return dept;
    }
}
