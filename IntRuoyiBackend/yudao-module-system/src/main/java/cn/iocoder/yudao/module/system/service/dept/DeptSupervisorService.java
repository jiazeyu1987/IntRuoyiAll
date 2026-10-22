package cn.iocoder.yudao.module.system.service.dept;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptSupervisorRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.logger.OperateLogDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.logger.OperateLogMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/** 组织主管的唯一解析与解除入口。跨部门读取仅限组织关系，租户隔离始终生效。 */
@Service
@DataPermission(enable = false)
public class DeptSupervisorService {
    @Resource private DeptMapper deptMapper;
    @Resource private AdminUserMapper userMapper;
    @Resource private OperateLogMapper operateLogMapper;

    public Map<Long, DeptSupervisorRespDTO> describe(Collection<DeptDO> requested) {
        if (requested.isEmpty()) return Map.of();
        Map<Long, DeptDO> departments = departmentMap();
        Map<Long, AdminUserDO> users = supervisorMap(departments.values());
        Map<Long, DeptSupervisorRespDTO> result = new LinkedHashMap<>();
        for (DeptDO dept : requested) {
            result.put(dept.getId(), resolve(dept.getId(), null, departments, users));
        }
        return result;
    }

    public DeptSupervisorRespDTO requireSupervisor(Long deptId, Long excludedUserId) {
        if (deptId == null) throw exception(DEPT_SUPERVISOR_REQUIRED, "未分配部门");
        Map<Long, DeptDO> departments = departmentMap();
        DeptSupervisorRespDTO result = resolve(deptId, excludedUserId, departments,
                supervisorMap(departments.values()));
        if ("INVALID_HIERARCHY".equals(result.getStatus())) throw exception(DEPT_HIERARCHY_INVALID, deptId);
        if (result.getUserId() == null) {
            DeptDO dept = departments.get(deptId);
            throw exception(DEPT_SUPERVISOR_REQUIRED, dept == null ? deptId : dept.getName());
        }
        return result;
    }

    private Map<Long, DeptDO> departmentMap() {
        return deptMapper.selectList().stream().collect(Collectors.toMap(DeptDO::getId, Function.identity()));
    }

    private Map<Long, AdminUserDO> supervisorMap(Collection<DeptDO> departments) {
        Set<Long> ids = departments.stream().map(DeptDO::getLeaderUserId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectByIds(ids).stream().collect(Collectors.toMap(AdminUserDO::getId, Function.identity()));
    }

    static DeptSupervisorRespDTO resolve(Long deptId, Long excludedUserId,
            Map<Long, DeptDO> departments, Map<Long, AdminUserDO> users) {
        DeptSupervisorRespDTO result = new DeptSupervisorRespDTO().setStatus("MISSING");
        DeptDO original = departments.get(deptId);
        if (original == null) return result.setStatus("INVALID_HIERARCHY");
        AdminUserDO configured = original.getLeaderUserId() == null ? null : users.get(original.getLeaderUserId());
        result.setConfiguredUserId(original.getLeaderUserId())
                .setConfiguredUserName(configured == null ? null : configured.getNickname())
                .setConfiguredUserInvalid(original.getLeaderUserId() != null && !available(configured));
        Set<Long> visited = new HashSet<>();
        DeptDO current = original;
        while (current != null) {
            if (!visited.add(current.getId())) return result.setStatus("INVALID_HIERARCHY");
            AdminUserDO supervisor = current.getLeaderUserId() == null ? null : users.get(current.getLeaderUserId());
            if (CommonStatusEnum.isEnable(current.getStatus()) && available(supervisor)
                    && !Objects.equals(supervisor.getId(), excludedUserId)) {
                return result.setStatus(Objects.equals(current.getId(), deptId) ? "DIRECT" : "INHERITED")
                        .setUserId(supervisor.getId()).setUserName(supervisor.getNickname())
                        .setDeptId(current.getId()).setDeptName(current.getName());
            }
            Long parentId = current.getParentId();
            if (parentId == null || DeptDO.PARENT_ID_ROOT.equals(parentId)) return result;
            current = departments.get(parentId);
            if (current == null) return result.setStatus("INVALID_HIERARCHY");
        }
        return result;
    }

    private static boolean available(AdminUserDO user) {
        return user != null && !Boolean.TRUE.equals(user.getDeleted())
                && CommonStatusEnum.isEnable(user.getStatus()) && user.getLifecycleDeactivatedTime() == null;
    }

    public void validateSupervisor(Long userId) {
        if (userId != null && !available(userMapper.selectById(userId))) {
            throw exception(DEPT_SUPERVISOR_INVALID, userId);
        }
    }

    public List<DeptDO> getAssignments(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return deptMapper.selectList(DeptDO::getLeaderUserId, userIds);
    }

    /** 调用方先锁定用户、再持有租户资格锁；与账号变更在同一事务内完成。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void releaseAssignments(Collection<Long> userIds, String reason) {
        if (userIds == null || userIds.isEmpty()) return;
        // 导入/定时任务可能已建立 InnoDB 一致性快照；资格锁内用当前读，不能漏掉快照之后的新任命。
        List<DeptDO> assignments = deptMapper.selectList(Wrappers.lambdaQuery(DeptDO.class)
                .in(DeptDO::getLeaderUserId, userIds).orderByAsc(DeptDO::getId).last("FOR UPDATE"));
        for (DeptDO dept : assignments) {
            if (deptMapper.update(null, Wrappers.lambdaUpdate(DeptDO.class).eq(DeptDO::getId, dept.getId())
                    .eq(DeptDO::getLeaderUserId, dept.getLeaderUserId()).set(DeptDO::getLeaderUserId, null)) != 1) {
                throw new IllegalStateException("DEPT_SUPERVISOR_RELEASE_ROW_COUNT");
            }
            recordChange(dept, null, reason);
        }
    }

    /** 同步写入现有操作日志，失败时主管修改一并回滚。定时生效记录为系统任务。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void recordChange(DeptDO previous, Long nextUserId, String reason) {
        if (Objects.equals(previous.getLeaderUserId(), nextUserId)) return;
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        OperateLogDO log = new OperateLogDO().setUserId(actorId == null ? 0L : actorId)
                .setUserType(UserTypeEnum.ADMIN.getValue()).setType("部门主管").setSubType("调整主管")
                .setBizId(previous.getId())
                .setAction("部门“" + previous.getName() + "”主管由用户编号 " + previous.getLeaderUserId()
                        + " 调整为 " + nextUserId + "；原因：" + reason
                        + (actorId == null ? "；执行方：系统定时任务" : ""));
        var request = ServletUtils.getRequest();
        if (request != null) {
            log.setRequestMethod(request.getMethod()).setRequestUrl(request.getRequestURI())
                    .setUserIp(ServletUtils.getClientIP(request)).setUserAgent(ServletUtils.getUserAgent(request));
        }
        if (operateLogMapper.insert(log) != 1) throw new IllegalStateException("DEPT_SUPERVISOR_AUDIT_ROW_COUNT");
    }
}
