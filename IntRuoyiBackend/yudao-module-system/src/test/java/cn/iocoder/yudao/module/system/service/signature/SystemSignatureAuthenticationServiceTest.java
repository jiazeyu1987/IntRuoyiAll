package cn.iocoder.yudao.module.system.service.signature;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationRequest;
import cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.signature.SignatureAuthenticationEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit coverage of the production authentication algorithm; transaction persistence is a separate gate. */
class SystemSignatureAuthenticationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-10T02:00:00Z");
    private static final LocalDateTime LOCAL = LocalDateTime.parse("2026-10-10T10:00:00");
    private static final String GENERATION = "7bd606e5-51e5-4f5c-9a1c-62619cf1ec9d";
    private final AdminUserMapper users = mock(AdminUserMapper.class);
    private final SignatureAuthenticationEventMapper events = mock(SignatureAuthenticationEventMapper.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final SystemSignatureAuthenticationService service = new SystemSignatureAuthenticationService(
            users, events, encoder, Clock.fixed(NOW, ZoneOffset.UTC));
    private AdminUserDO user;

    @BeforeEach void identity() {
        TenantContextHolder.setTenantId(7L);
        LoginUser operator = new LoginUser(); operator.setId(12L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(operator, null, List.of()));
        user = new AdminUserDO(); user.setId(12L); user.setTenantId(7L); user.setDeleted(false);
        user.setStatus(0); user.setPassword("stored-hash"); user.setPasswordCredentialStatus("ACTIVE");
        user.setPasswordUpdateTime(LOCAL.minusDays(1)); user.setLoginLocked(0); user.setLoginFailureCount(0);
        user.setSignatureCredentialGeneration(GENERATION); user.setSignatureCredentialVersion(1L);
        when(users.selectPermissionSubjectForUpdate(7L, 12L)).thenReturn(user);
        when(users.resetSignatureFailures(7L, 12L)).thenReturn(1);
        when(users.recordSignatureFailure(anyLong(), anyLong(), anyInt(), any(), anyInt(), any())).thenReturn(1);
        when(events.insert(any(SignatureAuthenticationEventDO.class))).thenAnswer(call -> {
            call.<SignatureAuthenticationEventDO>getArgument(0).setId(31L); return 1;
        });
    }
    @AfterEach void cleanContext() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }
    private SignatureAuthenticationRequest request() {
        return new SignatureAuthenticationRequest(7L, "SYSTEM_USER", 12L, 12L, "auth-request", "typed-password");
    }
    private AuthenticatedSigner signer() {
        return new AuthenticatedSigner(7L, "SYSTEM_USER", 12L, 12L, GENERATION, 1L, "auth-request");
    }

    @Test void successfulAuthenticationUsesPersistedMetadataAfterReset() {
        when(encoder.matches("typed-password", "stored-hash")).thenReturn(true);
        AdminUserDO reread = new AdminUserDO(); org.springframework.beans.BeanUtils.copyProperties(user, reread);
        reread.setSignatureCredentialVersion(2L);
        when(users.selectPermissionSubjectForUpdate(7L, 12L)).thenReturn(user, reread);
        var outcome = service.authenticate(request());
        assertTrue(outcome.succeeded()); assertEquals(2L, outcome.signer().credentialVersion());
        verify(events).insert(argThat((SignatureAuthenticationEventDO event) -> "SUCCESS".equals(event.getOutcome())
                && event.getCredentialVersion() == 2L && LOCAL.equals(event.getOccurredAt())));
        verify(encoder, times(1)).matches("typed-password", "stored-hash");
    }
    @Test void fifthFailureLocksAndReturnsRejectionForIndependentCommit() {
        user.setLoginFailureCount(4); user.setLoginFailureWindowStartTime(LOCAL.minusMinutes(2));
        var outcome = service.authenticate(request());
        assertFalse(outcome.succeeded()); assertEquals("BAD_CREDENTIALS", outcome.rejectionCode());
        verify(users).recordSignatureFailure(7L, 12L, 5, LOCAL.minusMinutes(2), 1, LOCAL);
        verify(events).insert(argThat((SignatureAuthenticationEventDO event) -> "BAD_CREDENTIALS".equals(event.getOutcome())));
    }
    @Test void expiredFailureWindowRestartsAtOne() {
        user.setLoginFailureCount(4); user.setLoginFailureWindowStartTime(LOCAL.minusMinutes(15));
        service.authenticate(request());
        verify(users).recordSignatureFailure(7L, 12L, 1, LOCAL, 0, null);
    }
    @Test void activeLockDoesNotExtendOrCheckPassword() {
        user.setLoginLocked(1); user.setLoginLockedTime(LOCAL.minusMinutes(5));
        assertEquals("IDENTITY_LOCKED", service.authenticate(request()).rejectionCode());
        verifyNoInteractions(encoder);
        verify(users, never()).recordSignatureFailure(anyLong(), anyLong(), anyInt(), any(), anyInt(), any());
    }
    @Test void expiryAndInitialCredentialsNeverCheckPassword() {
        user.setPasswordUpdateTime(LOCAL.minusDays(90));
        assertEquals("CREDENTIAL_EXPIRED", service.authenticate(request()).rejectionCode());
        user.setPasswordCredentialStatus("INITIAL");
        assertEquals("CREDENTIAL_CHANGE_REQUIRED", service.authenticate(request()).rejectionCode());
        verifyNoInteractions(encoder);
    }
    @Test void lifecycleEffectiveAtSigningInstantIsDisabled() {
        user.setLifecycleEffectiveTime(LOCAL);
        assertEquals("IDENTITY_DISABLED", service.authenticate(request()).rejectionCode());
        verifyNoInteractions(encoder);
    }
    @Test void auditWriteFailureCannotReturnAnAuthenticatedSigner() {
        when(encoder.matches(anyString(), anyString())).thenReturn(true);
        when(events.insert(any(SignatureAuthenticationEventDO.class))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.authenticate(request()));
    }
    @Test void tenantOrOperatorMismatchStopsBeforeCredentialAccess() {
        TenantContextHolder.setTenantId(8L);
        assertThrows(IllegalArgumentException.class, () -> service.authenticate(request()));
        verifyNoInteractions(users, events, encoder);
    }
    @Test void revalidationChecksVersionAndGenerationWithoutSecondAuthentication() {
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(
                new DriverManagerDataSource("jdbc:h2:mem:signature_revalidation_" + UUID.randomUUID(), "sa", "")));
        transaction.executeWithoutResult(status -> {
            assertEquals(signer(), service.lockAndRevalidate(signer(), NOW));
            SignatureIdentityLockContext.requireSystemIdentityLock(12L);
            user.setSignatureCredentialVersion(2L);
            assertThrows(IllegalStateException.class, () -> service.lockAndRevalidate(signer(), NOW));
            user.setSignatureCredentialVersion(1L);
            user.setSignatureCredentialGeneration("fb09dd59-cd82-4817-8d27-bcf2a7545b85");
            assertThrows(IllegalStateException.class, () -> service.lockAndRevalidate(signer(), NOW));
        });
        verifyNoInteractions(encoder, events);
    }
    @Test void requestDiagnosticsNeverContainCredential() {
        assertFalse(request().toString().contains("typed-password"));
    }
}
