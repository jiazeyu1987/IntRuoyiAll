package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserPasswordHistoryDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserPasswordHistoryMapper;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminUserSignatureWriterLockTest {
    private final AdminUserMapper users = mock(AdminUserMapper.class);
    private final ApprovalAuthorityApi authority = mock(ApprovalAuthorityApi.class);
    private final AdminUserPasswordHistoryMapper history = mock(AdminUserPasswordHistoryMapper.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final OAuth2TokenService tokens = mock(OAuth2TokenService.class);
    private final AdminUserServiceImpl service = new AdminUserServiceImpl();
    private AdminUserDO user;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "approvalAuthority", authority);
        ReflectionTestUtils.setField(service, "passwordHistoryMapper", history);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(service, "oauth2TokenService", tokens);
        user = new AdminUserDO(); user.setId(12L); user.setTenantId(7L); user.setPassword("old-hash");
        user.setStatus(0); user.setLoginFailureCount(0); user.setPasswordUpdateTime(LocalDateTime.now().minusDays(1));
        when(users.selectPermissionSubjectForUpdate(7L, 12L)).thenReturn(user);
        when(users.selectById(12L)).thenReturn(user);
        when(users.updateById(any(AdminUserDO.class))).thenReturn(1);
        when(history.selectLatestListForUpdate(7L, 12L, 5)).thenReturn(List.of());
        when(history.insert(any(AdminUserPasswordHistoryDO.class))).thenReturn(1);
        when(encoder.encode(anyString())).thenReturn("new-hash");
        when(authority.lockTenantApprovalAuthority(7L)).thenReturn(new ApprovalAuthorityRevisionDTO(7L, 3L));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); TransactionSynchronizationManager.clear(); }
    @Test void disableLocksIdentityBeforeAuthorityAndTokenRevocation() {
        service.updateUserStatus(12L, 1);
        var order = inOrder(users, authority, tokens);
        order.verify(users).selectPermissionSubjectForUpdate(7L, 12L);
        order.verify(authority).lockTenantApprovalAuthority(7L);
        order.verify(users).updateById(argThat((AdminUserDO row) -> row.getStatus() == 1));
        order.verify(authority).incrementLockedAuthorityRevision(7L, 3L);
        order.verify(tokens).removeAccessToken(eq(12L), anyInt());
    }
    @Test void passwordResetReadsIdentityUnderLockBeforeHistoryAndUpdate() {
        service.updateUserPassword(12L, "NewPass123!");
        var order = inOrder(users, history, tokens);
        order.verify(users).selectPermissionSubjectForUpdate(7L, 12L);
        order.verify(history).insert(argThat((AdminUserPasswordHistoryDO row) -> "old-hash".equals(row.getPasswordHash())));
        order.verify(users).updateById(argThat((AdminUserDO row) -> "RESET_REQUIRED".equals(row.getPasswordCredentialStatus())
                && row.getSignatureCredentialVersion() == null && row.getSignatureCredentialGeneration() == null));
        order.verify(tokens).removeAccessToken(eq(12L), anyInt());
        verifyNoInteractions(authority);
    }
    @Test void failedOldPasswordCannotRewriteCredentialOrHistory() {
        UserProfileUpdatePasswordReqVO request = new UserProfileUpdatePasswordReqVO();
        request.setOldPassword("wrong"); request.setNewPassword("NewPass123!");
        assertThrows(RuntimeException.class, () -> service.updateUserPassword(12L, request));
        verify(users).selectPermissionSubjectForUpdate(7L, 12L);
        verify(users, never()).updateById(any(AdminUserDO.class));
        verify(history, never()).insert(any(AdminUserPasswordHistoryDO.class));
    }
    @Test void passwordChangeChecksLockedPasswordAndRevokesExistingSessions() {
        UserProfileUpdatePasswordReqVO request = new UserProfileUpdatePasswordReqVO();
        request.setOldPassword("old"); request.setNewPassword("NewPass123!");
        when(encoder.matches("old", "old-hash")).thenReturn(true);
        service.updateUserPassword(12L, request);
        var order = inOrder(users, encoder, history, tokens);
        order.verify(users).selectPermissionSubjectForUpdate(7L, 12L);
        order.verify(encoder).matches("old", "old-hash");
        order.verify(history).selectLatestListForUpdate(7L, 12L, 5);
        order.verify(history).insert(any(AdminUserPasswordHistoryDO.class));
        order.verify(users).updateById(argThat((AdminUserDO row) -> "ACTIVE".equals(row.getPasswordCredentialStatus())));
        order.verify(tokens).removeAccessToken(eq(12L), anyInt());
        verify(users, never()).selectById(anyLong());
    }
    @Test void failedPasswordWriteCannotRevokeSessions() {
        when(users.updateById(any(AdminUserDO.class))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.updateUserPassword(12L, "NewPass123!"));
        verifyNoInteractions(tokens, authority);
    }
    @Test void lifecycleLocksAllCandidateIdentitiesBeforeAuthorityAndRechecksCurrentState() {
        LocalDateTime now = LocalDateTime.now();
        AdminUserDO first = new AdminUserDO().setId(10L);
        AdminUserDO current = new AdminUserDO().setId(10L).setLifecycleEffectiveTime(now.plusDays(1));
        when(users.selectListByPendingLifecycleDeactivation(now, 5)).thenReturn(List.of(user, first));
        when(users.selectPermissionSubjectForUpdate(7L, 10L)).thenReturn(current);
        assertEquals(0, service.processDueLifecycleDeactivations(now, 5));
        var order = inOrder(users, authority);
        order.verify(users).selectPermissionSubjectForUpdate(7L, 10L);
        order.verify(users).selectPermissionSubjectForUpdate(7L, 12L);
        order.verify(authority).lockTenantApprovalAuthority(7L);
        verify(authority, never()).incrementLockedAuthorityRevision(anyLong(), anyLong());
        verify(users, never()).update(any(), any());
        verifyNoInteractions(tokens);
    }
}