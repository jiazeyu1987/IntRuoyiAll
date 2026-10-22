package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import cn.iocoder.yudao.module.system.api.signature.dto.*;
import cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.signature.SignatureAuthenticationEventMapper;
import cn.iocoder.yudao.module.system.service.signature.SignatureCredentialPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Profile credentials remain in MES; the event store records no credential material. */
@Service
public class MesEmployeeSignatureAuthenticationService implements SignatureAuthenticationApi {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final MesProcessPoolTeamEmployeeProfileMapper profiles;
    private final SignatureAuthenticationEventMapper events;
    private final PasswordEncoder encoder;
    private final Clock clock;
    public MesEmployeeSignatureAuthenticationService(MesProcessPoolTeamEmployeeProfileMapper profiles,
            SignatureAuthenticationEventMapper events, PasswordEncoder encoder, Clock clock) {
        this.profiles=profiles; this.events=events; this.encoder=encoder; this.clock=clock;
    }
    @Override public String identityDomain() { return "MES_EMPLOYEE_PROFILE"; }
    @Override @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class)
    public SignatureAuthenticationOutcome authenticate(SignatureAuthenticationRequest request) {
        return authenticateCredential(request,false);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class)
    public SignatureAuthenticationOutcome authenticatePasswordChange(SignatureAuthenticationRequest request) {
        return authenticateCredential(request,true);
    }
    private SignatureAuthenticationOutcome authenticateCredential(SignatureAuthenticationRequest request,boolean change) {
        scope(request.tenantId(),request.domain(),request.operatorId());
        var row=locked(request.tenantId(),request.actorId());
        var now=LocalDateTime.ofInstant(clock.instant().truncatedTo(ChronoUnit.SECONDS),ZONE);
        String rejection=eligibility(row,now);
        if(change && ("CREDENTIAL_CHANGE_REQUIRED".equals(rejection) || "CREDENTIAL_EXPIRED".equals(rejection))) rejection=null;
        if(rejection==null && !encoder.matches(request.credential(),row.getSignaturePasswordHash())) {
            boolean same=row.getSignatureFailureWindowStartAt()!=null && row.getSignatureFailureWindowStartAt().plusMinutes(SignatureCredentialPolicy.FAILURE_WINDOW_MINUTES).isAfter(now);
            if(same && (row.getSignatureFailureCount()==null || row.getSignatureFailureCount()<0)) throw new IllegalStateException("SIGNATURE_FAILURE_METADATA_INVALID");
            int count=same?Math.addExact(row.getSignatureFailureCount(),1):1;
            one(profiles.writeSignatureFailures(request.tenantId(),row.getId(),count,same?row.getSignatureFailureWindowStartAt():now,count>=SignatureCredentialPolicy.FAILURE_LIMIT?now.plusMinutes(SignatureCredentialPolicy.LOCK_MINUTES):null));
            rejection="BAD_CREDENTIALS";
        } else if(rejection==null) one(profiles.writeSignatureFailures(request.tenantId(),row.getId(),0,null,null));
        row=locked(request.tenantId(),request.actorId()); // Read the DB-maintained version after the write.
        var event=new SignatureAuthenticationEventDO(); event.setTenantId(request.tenantId());event.setActorId(row.getId());event.setIdentityDomain(identityDomain());event.setOperatorId(request.operatorId());event.setRequestId(request.requestIdentity());event.setOperationType("AUTHENTICATE");event.setOutcome(rejection==null?"SUCCESS":rejection);event.setOccurredAt(now);event.setCredentialGeneration(row.getSignatureCredentialGeneration());event.setCredentialVersion(row.getSignatureCredentialVersion());one(events.insert(event));
        if(event.getId()==null || event.getId()<=0) throw new IllegalStateException("SIGNATURE_AUTHENTICATION_EVENT_ID_MISSING");
        return rejection==null?new SignatureAuthenticationOutcome(new AuthenticatedSigner(request.tenantId(),identityDomain(),row.getId(),request.operatorId(),row.getSignatureCredentialGeneration(),row.getSignatureCredentialVersion(),request.requestIdentity()),null):new SignatureAuthenticationOutcome(null,rejection);
    }
    @Override @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public AuthenticatedSigner lockAndRevalidate(AuthenticatedSigner signer,Instant instant) {
        scope(signer.tenantId(),signer.domain(),signer.operatorId());
        var row=locked(signer.tenantId(),signer.actorId());
        if(!Objects.equals(row.getSignatureCredentialGeneration(),signer.credentialGeneration()) || !Objects.equals(row.getSignatureCredentialVersion(),signer.credentialVersion())) throw new IllegalStateException("CREDENTIAL_VERSION_CHANGED");
        var rejection=eligibility(row,LocalDateTime.ofInstant(Objects.requireNonNull(instant),ZONE));
        if(rejection!=null) throw new IllegalStateException(rejection);
        return signer;
    }
    private void scope(Long tenant,String domain,Long operator) {
        if(!identityDomain().equals(domain) || !Objects.equals(tenant,TenantContextHolder.getRequiredTenantId()) || !Objects.equals(operator,SecurityFrameworkUtils.getLoginUserId())) throw new IllegalArgumentException("SIGNATURE_AUTHENTICATION_SCOPE_MISMATCH");
    }
    private MesProcessPoolTeamEmployeeProfileDO locked(Long tenant,Long actor) {
        var row=profiles.selectSignatureIdentityForUpdate(tenant,actor);
        if(row==null || !Objects.equals(row.getTenantId(),tenant) || !Objects.equals(row.getId(),actor) || row.getSystemUserId()!=null) throw new IllegalStateException("SIGNATURE_IDENTITY_NOT_FOUND");
        new AuthenticatedSigner(tenant,identityDomain(),actor,actor,row.getSignatureCredentialGeneration(),row.getSignatureCredentialVersion(),"metadata-check");
        return row;
    }
    private static String eligibility(MesProcessPoolTeamEmployeeProfileDO row,LocalDateTime now) {
        if(!Boolean.TRUE.equals(row.getEnabled()) || row.getDisabledAt()!=null) return "IDENTITY_DISABLED";
        if(row.getSignatureLockedUntil()!=null && row.getSignatureLockedUntil().isAfter(now)) return "IDENTITY_LOCKED";
        if(!"ACTIVE".equals(row.getSignatureCredentialStatus())) return "CREDENTIAL_CHANGE_REQUIRED";
        if(SignatureCredentialPolicy.isExpired(row.getSignaturePasswordUpdatedAt(),now)) return "CREDENTIAL_EXPIRED";
        if(row.getSignaturePasswordHash()==null || row.getSignaturePasswordHash().isBlank()) throw new IllegalStateException("SIGNATURE_CREDENTIAL_HASH_MISSING");
        return null;
    }
    private static void one(int count) { if(count!=1) throw new IllegalStateException("SIGNATURE_AUTHENTICATION_WRITE_ROW_COUNT"); }
}
