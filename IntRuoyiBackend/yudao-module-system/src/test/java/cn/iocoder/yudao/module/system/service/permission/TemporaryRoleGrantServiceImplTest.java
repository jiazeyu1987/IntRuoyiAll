package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.TemporaryRoleGrantAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.TemporaryRoleGrantDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.TemporaryRoleGrantAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.TemporaryRoleGrantMapper;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantReviewSummaryRespVO;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import cn.iocoder.yudao.module.system.service.permission.bo.TemporaryRoleGrantCreateCommand;
import jakarta.annotation.Resource;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.TEMPORARY_ROLE_GRANT_EXPIRE_TIME_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Import({TemporaryRoleGrantServiceImpl.class, PermissionServiceImpl.class})
class TemporaryRoleGrantServiceImplTest extends BaseDbUnitTest {

    private static final Long USER_ID = 2001L;
    private static final Long ROLE_ID = 3001L;
    private static final Long MENU_ID = 4001L;
    private static final String PERMISSION = "system:temporary-role-grant:test";

    @Resource
    private TemporaryRoleGrantService temporaryRoleGrantService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private TemporaryRoleGrantMapper temporaryRoleGrantMapper;
    @Resource
    private TemporaryRoleGrantAuditMapper temporaryRoleGrantAuditMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;

    @MockitoBean
    private RoleService roleService;
    @MockitoBean
    private MenuService menuService;
    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private AdminUserService userService;
    @MockitoBean
    private SystemEntitlementService systemEntitlementService;
    @MockitoBean
    private NotifySendService notifySendService;

    @Test
    void createGrant_shouldRejectMissingOrPastExpireTime() {
        insertRoleAndMenu();

        assertServiceException(() -> temporaryRoleGrantService.createGrant(createCommand(null)),
                TEMPORARY_ROLE_GRANT_EXPIRE_TIME_INVALID);
        assertServiceException(() -> temporaryRoleGrantService.createGrant(createCommand(LocalDateTime.now().minusMinutes(1))),
                TEMPORARY_ROLE_GRANT_EXPIRE_TIME_INVALID);
    }

    @Test
    void approveGrant_shouldMakeRoleEffectiveUntilRevokedOrExpired() {
        insertRoleAndMenu();
        Long grantId = temporaryRoleGrantService.createGrant(createCommand(LocalDateTime.now().plusHours(2)));

        assertFalse(permissionService.hasAnyPermissions(USER_ID, PERMISSION));

        temporaryRoleGrantService.approveGrant(grantId, 1L, "approver");
        assertTrue(permissionService.hasAnyPermissions(USER_ID, PERMISSION));

        temporaryRoleGrantService.revokeGrant(grantId, 2L, "revoker", "测试完成");
        assertFalse(permissionService.hasAnyPermissions(USER_ID, PERMISSION));
    }

    @Test
    void expireOverdueGrants_shouldRemoveTemporaryRoleAndWriteAudit() {
        insertRoleAndMenu();
        Long grantId = temporaryRoleGrantService.createGrant(createCommand(LocalDateTime.now().plusMinutes(10)));
        temporaryRoleGrantService.approveGrant(grantId, 1L, "approver");
        TemporaryRoleGrantDO grant = temporaryRoleGrantMapper.selectById(grantId);
        grant.setExpireTime(LocalDateTime.now().minusSeconds(1));
        temporaryRoleGrantMapper.updateById(grant);

        int expired = temporaryRoleGrantService.expireOverdueGrants(LocalDateTime.now(), 9L, "job");

        assertEquals(1, expired);
        assertFalse(permissionService.hasAnyPermissions(USER_ID, PERMISSION));
        assertTrue(temporaryRoleGrantAuditMapper.selectListByGrantId(grantId).stream()
                .anyMatch(audit -> "EXPIRE".equals(audit.getEventType())));
    }

    @Test
    void hasAnyPermissions_shouldRecordUseAuditWhenTemporaryRoleAllowsPermission() {
        insertRoleAndMenu();
        Long grantId = temporaryRoleGrantService.createGrant(createCommand(LocalDateTime.now().plusHours(2)));
        temporaryRoleGrantService.approveGrant(grantId, 1L, "approver");

        assertTrue(permissionService.hasAnyPermissions(USER_ID, PERMISSION));

        List<TemporaryRoleGrantAuditDO> audits = temporaryRoleGrantAuditMapper.selectListByGrantId(grantId);
        assertTrue(audits.stream().anyMatch(audit -> "USE".equals(audit.getEventType())
                && PERMISSION.equals(audit.getPermissionCode())));
    }

    @Test
    void remindExpiringSoonGrants_shouldSendReminderWriteAuditAndNotRepeat() {
        insertRoleAndMenu();
        LocalDateTime now = LocalDateTime.now();
        Long grantId = temporaryRoleGrantService.createGrant(createCommand(now.plusHours(2)));
        temporaryRoleGrantService.approveGrant(grantId, 1L, "approver");

        int reminded = temporaryRoleGrantService.remindExpiringSoonGrants(now, 24, 9L, "reminder-job");
        int remindedAgain = temporaryRoleGrantService.remindExpiringSoonGrants(now.plusMinutes(1), 24, 9L, "reminder-job");

        TemporaryRoleGrantDO grant = temporaryRoleGrantMapper.selectById(grantId);
        assertEquals(1, reminded);
        assertEquals(0, remindedAgain);
        assertNotNull(grant.getRemindTime());
        assertFalse(grant.getRemindTime().isBefore(now.minusSeconds(1)));
        assertFalse(grant.getRemindTime().isAfter(now.plusSeconds(1)));
        assertTrue(temporaryRoleGrantAuditMapper.selectListByGrantId(grantId).stream()
                .anyMatch(audit -> "REMIND".equals(audit.getEventType())));
        verify(notifySendService).sendSingleNotifyToAdminIdempotently(eq(USER_ID),
                eq("SYSTEM_TEMPORARY_ROLE_GRANT_EXPIRING"), anyMap(), contains(":grantee:"));
        verify(notifySendService).sendSingleNotifyToAdminIdempotently(eq(100L),
                eq("SYSTEM_TEMPORARY_ROLE_GRANT_EXPIRING"), anyMap(), contains(":applicant:"));
        verify(notifySendService).sendSingleNotifyToAdminIdempotently(eq(1L),
                eq("SYSTEM_TEMPORARY_ROLE_GRANT_EXPIRING"), anyMap(), contains(":approver:"));
        verifyNoMoreInteractions(notifySendService);
    }

    @Test
    void getReviewSummaryAndPage_shouldAggregateActiveExpiringSoonAndOverdueGrants() {
        LocalDateTime now = LocalDateTime.now();
        insertActiveGrant(4101L, now.minusHours(1), now.plusHours(30));
        insertActiveGrant(4102L, now.minusHours(1), now.plusHours(2));
        insertActiveGrant(4103L, now.minusHours(2), now.minusMinutes(1));

        TemporaryRoleGrantReviewSummaryRespVO summary = temporaryRoleGrantService.getReviewSummary(now, 24);
        TemporaryRoleGrantPageReqVO reqVO = new TemporaryRoleGrantPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setReviewCategory("EXPIRING_SOON");

        assertEquals(1L, summary.getActiveCount());
        assertEquals(1L, summary.getExpiringSoonCount());
        assertEquals(1L, summary.getOverdueCount());
        assertEquals(1, temporaryRoleGrantService.getGrantPage(reqVO).getList().size());
        assertEquals("EXPIRING_SOON", temporaryRoleGrantService.getGrantPage(reqVO).getList().get(0).getReviewCategory());
    }

    private TemporaryRoleGrantCreateCommand createCommand(LocalDateTime expireTime) {
        return TemporaryRoleGrantCreateCommand.builder()
                .userId(USER_ID)
                .roleId(ROLE_ID)
                .reason("临时处理生产异常")
                .expireTime(expireTime)
                .applicantUserId(100L)
                .applicantUsername("applicant")
                .build();
    }

    private void insertRoleAndMenu() {
        RoleDO role = new RoleDO()
                .setId(ROLE_ID)
                .setName("临时测试角色")
                .setCode("temporary_test_role")
                .setSort(1)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setType(RoleTypeEnum.CUSTOM.getType());
        roleMapper.insert(role);
        when(roleService.getRoleListFromCache(org.mockito.ArgumentMatchers.anyCollection())).thenAnswer(invocation -> {
            java.util.Collection<Long> roleIds = invocation.getArgument(0);
            return roleIds.contains(ROLE_ID) ? List.of(role) : List.of();
        });
        when(menuService.getMenuIdListByPermissionFromCache(eq(PERMISSION))).thenReturn(List.of(MENU_ID));
        menuMapper.insert(new MenuDO()
                .setId(MENU_ID)
                .setName("临时权限测试")
                .setPermission(PERMISSION)
                .setType(3)
                .setSort(1)
                .setParentId(0L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setVisible(true)
                .setKeepAlive(true)
                .setAlwaysShow(true));
        roleMenuMapper.insert(new RoleMenuDO().setRoleId(ROLE_ID).setMenuId(MENU_ID));
    }

    private void insertActiveGrant(Long id, LocalDateTime effectiveTime, LocalDateTime expireTime) {
        temporaryRoleGrantMapper.insert(new TemporaryRoleGrantDO()
                .setId(id)
                .setUserId(USER_ID)
                .setRoleId(ROLE_ID)
                .setReason("审查归集测试")
                .setStatus("ACTIVE")
                .setApplyTime(effectiveTime.minusMinutes(10))
                .setApplicantUserId(100L)
                .setApplicantUsername("applicant")
                .setApproveTime(effectiveTime)
                .setApproverUserId(1L)
                .setApproverUsername("approver")
                .setEffectiveTime(effectiveTime)
                .setExpireTime(expireTime));
    }

}
