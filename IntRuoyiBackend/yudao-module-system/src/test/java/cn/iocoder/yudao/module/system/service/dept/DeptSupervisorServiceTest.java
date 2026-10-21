package cn.iocoder.yudao.module.system.service.dept;

import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DeptSupervisorServiceTest {
    private final Map<Long, DeptDO> depts = new LinkedHashMap<>();
    private final Map<Long, AdminUserDO> users = new LinkedHashMap<>();
    private DeptDO dept(long id, long parent, Long supervisor) {
        DeptDO dept = new DeptDO().setId(id).setParentId(parent).setName("部门" + id)
                .setStatus(0).setLeaderUserId(supervisor);
        depts.put(id, dept);
        return dept;
    }
    private AdminUserDO user(long id) {
        AdminUserDO user = new AdminUserDO().setId(id).setNickname("主管" + id).setStatus(0);
        users.put(id, user);
        return user;
    }
    @Test void nearestSupervisorWinsAndClearingDoesNotCopyIntoDescendants() {
        dept(100, 0, 10L); dept(110, 100, null); DeptDO ad = dept(111, 110, 20L);
        user(10); user(20);
        assertEquals(20L, DeptSupervisorService.resolve(111L, 30L, depts, users).getUserId());
        ad.setLeaderUserId(null);
        var inherited = DeptSupervisorService.resolve(111L, 30L, depts, users);
        assertEquals(10L, inherited.getUserId());
        assertEquals(100L, inherited.getDeptId());
        assertEquals("INHERITED", inherited.getStatus());
        assertNull(ad.getLeaderUserId());
        assertNull(depts.get(110L).getLeaderUserId());
    }
    @Test void applicantSupervisorIsSkippedAndMissingRootSupervisorIsNotASuccess() {
        dept(100, 0, 10L); dept(111, 100, 20L); user(10); user(20);
        assertEquals(10L, DeptSupervisorService.resolve(111L, 20L, depts, users).getUserId());
        var missing = DeptSupervisorService.resolve(100L, 10L, depts, users);
        assertEquals("MISSING", missing.getStatus());
        assertNull(missing.getUserId());
    }
    @Test void disabledDeletedAndDeactivatedSupervisorsAreVisibleButNeverChosen() {
        dept(100, 0, 10L); dept(111, 100, 20L); user(10); AdminUserDO leader = user(20);
        leader.setStatus(1);
        assertInheritedInvalid();
        leader.setStatus(0).setDeleted(true);
        assertInheritedInvalid();
        leader.setDeleted(false);
        leader.setLifecycleDeactivatedTime(java.time.LocalDateTime.now());
        assertInheritedInvalid();
        users.remove(20L);
        assertInheritedInvalid();
    }
    private void assertInheritedInvalid() {
        var result = DeptSupervisorService.resolve(111L, 30L, depts, users);
        assertTrue(result.isConfiguredUserInvalid());
        assertEquals(10L, result.getUserId());
    }
    @Test void missingParentsAndCyclesAreReportedAsBrokenHierarchy() {
        dept(111, 110, null);
        assertEquals("INVALID_HIERARCHY", DeptSupervisorService.resolve(111L, null, depts, users).getStatus());
        dept(110, 111, null);
        assertEquals("INVALID_HIERARCHY", DeptSupervisorService.resolve(111L, null, depts, users).getStatus());
    }
    @Test void supervisorOfDisabledDepartmentCannotReceiveInheritedApproval() {
        dept(100, 0, 10L); dept(110, 100, 20L).setStatus(1); dept(111, 110, null);
        user(10); user(20);
        assertEquals(10L, DeptSupervisorService.resolve(111L, null, depts, users).getUserId());
    }
}
