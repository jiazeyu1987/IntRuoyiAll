package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserLifecycleDeactivateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.logger.OperateLogMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class UserSupervisorLifecycleTransactionTest extends UserPostRetentionTestSupport {
    @Resource private DeptMapper deptMapper;
    @MockitoSpyBean private OperateLogMapper operateLogMapper;
    @Resource private cn.iocoder.yudao.module.system.service.dept.DeptSupervisorService supervisorService;

    private DeptDO department(AdminUserDO supervisor, String name) {
        DeptDO dept = new DeptDO().setName(name).setParentId(0L).setSort(1).setStatus(0)
                .setLeaderUserId(supervisor.getId());
        deptMapper.insert(dept);
        return dept;
    }

    @Test void emptyReleaseDoesNotClearAnyOtherSupervisors() {
        var user = seedUser(Set.of());
        var dept = department(user, "无目标停用不解除主管");
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                supervisorService.releaseAssignments(java.util.List.of(), "空目标"));
        assertEquals(user.getId(), deptMapper.selectById(dept.getId()).getLeaderUserId());
        assertEquals(0L, operateLogMapper.selectCount());
    }

    @Test void disableClearsAllAssignmentsAndEnableDoesNotRestoreThem() {
        var user = seedUser(Set.of());
        var first = department(user, "销售");
        var second = department(user, "部门 AD");
        userService.updateUserStatus(user.getId(), 1);
        assertNull(deptMapper.selectById(first.getId()).getLeaderUserId());
        assertNull(deptMapper.selectById(second.getId()).getLeaderUserId());
        assertEquals(2L, operateLogMapper.selectCount());
        userService.updateUserStatus(user.getId(), 0);
        assertNull(deptMapper.selectById(first.getId()).getLeaderUserId());
    }

    @Test void auditFailureRollsBackAccountAndAssignments() {
        var user = seedUser(Set.of());
        var dept = department(user, "主管审计回滚");
        doThrow(new IllegalStateException("audit unavailable")).when(operateLogMapper).insert(any(
                cn.iocoder.yudao.module.system.dal.dataobject.logger.OperateLogDO.class));
        assertThrows(IllegalStateException.class, () -> userService.updateUserStatus(user.getId(), 1));
        assertEquals(0, userMapper.selectById(user.getId()).getStatus());
        assertEquals(user.getId(), deptMapper.selectById(dept.getId()).getLeaderUserId());
    }

    @Test void scheduledResignationRetainsSupervisorUntilEffective() {
        var user = seedUser(Set.of());
        var dept = department(user, "未来离职");
        var effective = LocalDateTime.now().plusDays(1);
        userService.recordUserLifecycleDeactivation(new UserLifecycleDeactivateReqVO().setId(user.getId())
                .setDocumentType("RESIGNATION").setDocumentNo("supervisor-scheduled")
                .setDocumentTime(LocalDateTime.now().minusDays(1)).setEffectiveTime(effective));
        assertEquals(user.getId(), deptMapper.selectById(dept.getId()).getLeaderUserId());
        assertEquals(0, userService.processDueLifecycleDeactivations(effective.minusMinutes(1), 10));
        assertEquals(1, userService.processDueLifecycleDeactivations(effective.plusMinutes(1), 10));
        assertNull(deptMapper.selectById(dept.getId()).getLeaderUserId());
        assertEquals(1, userMapper.selectById(user.getId()).getStatus());
    }

    @Test void deletingSupervisorPreservesDepartmentAndClearsItsAssignment() {
        var user = seedUser(Set.of());
        var dept = department(user, "删除主管");
        userService.deleteUser(user.getId());
        assertNull(userMapper.selectById(user.getId()));
        assertNotNull(deptMapper.selectById(dept.getId()));
        assertNull(deptMapper.selectById(dept.getId()).getLeaderUserId());
    }

    @Test void importingDisabledStatusClearsAllSupervisorAssignments() {
        var user = seedUser(Set.of());
        var first = department(user, "导入停用一部");
        var second = department(user, "导入停用二部");
        when(configApi.getConfigValueByKey(AdminUserServiceImpl.USER_INIT_PASSWORD_KEY))
                .thenReturn("Synthetic@2026");
        var imported = cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportExcelVO.builder()
                .username(user.getUsername()).nickname("导入停用主管").status(1).build();
        var result = userService.importUserList(java.util.List.of(imported), true);
        assertTrue(result.getFailureUsernames().isEmpty());
        assertEquals(java.util.List.of(user.getUsername()), result.getUpdateUsernames());
        assertEquals(1, userMapper.selectById(user.getId()).getStatus());
        assertNull(deptMapper.selectById(first.getId()).getLeaderUserId());
        assertNull(deptMapper.selectById(second.getId()).getLeaderUserId());
        assertEquals(2L, operateLogMapper.selectCount());
    }
}
