package cn.iocoder.yudao.module.system.service.signature;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationOutcome;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationRequest;
import cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.signature.SignatureAuthenticationEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
public class SystemSignatureAuthenticationService implements SignatureAuthenticationApi {
    private static final ZoneId SIGNING_ZONE = ZoneId.of("Asia/Shanghai");
    private final AdminUserMapper users;
    private final SignatureAuthenticationEventMapper events;
    private final PasswordEncoder encoder;
    private final Clock clock;
    public SystemSignatureAuthenticationService(AdminUserMapper users, SignatureAuthenticationEventMapper events,
            PasswordEncoder encoder, Clock clock) {
        this.users = users; this.events = events; this.encoder = encoder; this.clock = clock;
    }
    @Override public String identityDomain() { return "SYSTEM_USER"; }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public SignatureAuthenticationOutcome authenticate(SignatureAuthenticationRequest request) {
        requireRequest(request.tenantId(), request.domain(), request.operatorId());
        AdminUserDO user = users.selectPermissionSubjectForUpdate(request.tenantId(), request.actorId());
        requireUser(user, request.tenantId(), request.actorId());
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant().truncatedTo(ChronoUnit.SECONDS), SIGNING_ZONE);
        String rejected = eligibility(user, now);
        if (rejected == null && !encoder.matches(request.credential(), user.getPassword())) {
            recordFailure(user, now);
            user = users.selectPermissionSubjectForUpdate(request.tenantId(), request.actorId());
            requireUser(user, request.tenantId(), request.actorId());
            rejected = "BAD_CREDENTIALS";
        } else if (rejected == null) {
            requireOne(users.resetSignatureFailures(request.tenantId(), user.getId()), "SIGNATURE_FAILURE_RESET");
            user = users.selectPermissionSubjectForUpdate(request.tenantId(), request.actorId());
            requireUser(user, request.tenantId(), request.actorId());
        }
        appendEvent(request, user, now, rejected == null ? "SUCCESS" : rejected);
        return rejected == null ? new SignatureAuthenticationOutcome(new AuthenticatedSigner(request.tenantId(),
                identityDomain(), user.getId(), request.operatorId(), user.getSignatureCredentialGeneration(),
                user.getSignatureCredentialVersion(), request.requestIdentity()), null)
                : new SignatureAuthenticationOutcome(null, rejected);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public AuthenticatedSigner lockAndRevalidate(AuthenticatedSigner signer, Instant signingInstant) {
        requireRequest(signer.tenantId(), signer.domain(), signer.operatorId());
        Objects.requireNonNull(signingInstant, "SIGNATURE_SIGNING_INSTANT_REQUIRED");
        AdminUserDO user = users.selectPermissionSubjectForUpdate(signer.tenantId(), signer.actorId());
        requireUser(user, signer.tenantId(), signer.actorId());
        if (!Objects.equals(user.getSignatureCredentialGeneration(), signer.credentialGeneration())
                || !Objects.equals(user.getSignatureCredentialVersion(), signer.credentialVersion())) {
            throw new IllegalStateException("CREDENTIAL_VERSION_CHANGED");
        }
        String rejected = eligibility(user, LocalDateTime.ofInstant(signingInstant, SIGNING_ZONE));
        if (rejected != null) throw new IllegalStateException(rejected);
        SignatureIdentityLockContext.registerSystemIdentityLock(signer.tenantId(), signer.actorId());
        return signer;
    }

    private void requireRequest(Long tenant, String domain, Long operator) {
        if (!identityDomain().equals(domain) || !Objects.equals(tenant, TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(operator, SecurityFrameworkUtils.getLoginUserId())) {
            throw new IllegalArgumentException("SIGNATURE_AUTHENTICATION_SCOPE_MISMATCH");
        }
    }
    private static void requireUser(AdminUserDO user, Long tenant, Long actor) {
        if (user == null || !Objects.equals(user.getId(), actor) || !Objects.equals(user.getTenantId(), tenant)
                || Boolean.TRUE.equals(user.getDeleted())) throw new IllegalStateException("SIGNATURE_IDENTITY_NOT_FOUND");
        // Missing deployed generation/version cannot authenticate under the new contract.
        new AuthenticatedSigner(tenant, "SYSTEM_USER", actor, actor, user.getSignatureCredentialGeneration(),
                user.getSignatureCredentialVersion(), "credential-metadata-check");
    }
    private static String eligibility(AdminUserDO user, LocalDateTime now) {
        if (!CommonStatusEnum.isEnable(user.getStatus()) || user.getLifecycleDeactivatedTime() != null
                || user.getLifecycleEffectiveTime() != null && !user.getLifecycleEffectiveTime().isAfter(now))
            return "IDENTITY_DISABLED";
        if (Objects.equals(user.getLoginLocked(), 1) && user.getLoginLockedTime() == null)
            throw new IllegalStateException("SIGNATURE_LOCK_METADATA_INVALID");
        if (Objects.equals(user.getLoginLocked(), 1)
                && user.getLoginLockedTime().plusMinutes(SignatureCredentialPolicy.LOCK_MINUTES).isAfter(now))
            return "IDENTITY_LOCKED";
        if (!"ACTIVE".equals(user.getPasswordCredentialStatus())) return "CREDENTIAL_CHANGE_REQUIRED";
        if (SignatureCredentialPolicy.isExpired(user.getPasswordUpdateTime(), now)) return "CREDENTIAL_EXPIRED";
        if (user.getPassword() == null || user.getPassword().isBlank())
            throw new IllegalStateException("SIGNATURE_CREDENTIAL_HASH_MISSING");
        return null;
    }
    private void recordFailure(AdminUserDO user, LocalDateTime now) {
        LocalDateTime start = user.getLoginFailureWindowStartTime();
        boolean sameWindow = start != null && start.plusMinutes(SignatureCredentialPolicy.FAILURE_WINDOW_MINUTES).isAfter(now);
        if (sameWindow && (user.getLoginFailureCount() == null || user.getLoginFailureCount() < 0))
            throw new IllegalStateException("SIGNATURE_FAILURE_METADATA_INVALID");
        int count = sameWindow ? Math.addExact(user.getLoginFailureCount(), 1) : 1;
        boolean locked = count >= SignatureCredentialPolicy.FAILURE_LIMIT;
        requireOne(users.recordSignatureFailure(user.getTenantId(), user.getId(), count, sameWindow ? start : now,
                locked ? 1 : 0, locked ? now : null), "SIGNATURE_FAILURE_WRITE");
    }
    private void appendEvent(SignatureAuthenticationRequest request, AdminUserDO user, LocalDateTime now, String outcome) {
        SignatureAuthenticationEventDO event = new SignatureAuthenticationEventDO();
        event.setTenantId(request.tenantId()); event.setActorId(user.getId()); event.setIdentityDomain(identityDomain());
        event.setOperatorId(request.operatorId()); event.setRequestId(request.requestIdentity());
        event.setOperationType("AUTHENTICATE"); event.setOutcome(outcome); event.setOccurredAt(now);
        event.setCredentialVersion(user.getSignatureCredentialVersion());
        event.setCredentialGeneration(user.getSignatureCredentialGeneration());
        requireOne(events.insert(event), "SIGNATURE_AUTHENTICATION_EVENT_WRITE");
        if (event.getId() == null || event.getId() <= 0) throw new IllegalStateException("SIGNATURE_AUTHENTICATION_EVENT_ID_MISSING");
    }
    private static void requireOne(int count, String code) {
        if (count != 1) throw new IllegalStateException(code + "_ROW_COUNT");
    }
}
