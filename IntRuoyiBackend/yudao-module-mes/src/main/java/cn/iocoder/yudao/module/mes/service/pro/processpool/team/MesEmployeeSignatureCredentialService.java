package cn.iocoder.yudao.module.mes.service.pro.processpool.team;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.system.service.signature.SignatureCredentialPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Objects;
@Service
public class MesEmployeeSignatureCredentialService {
    public record Qualification(String status,boolean changeRequired,boolean expired,boolean locked,String identityDomain,String credentialType) {}
    public Qualification qualification(Long profileId) {
        var row=profiles.selectById(profileId);var now=LocalDateTime.ofInstant(clock.instant(),ZoneId.of("Asia/Shanghai"));
        if(row==null || !Objects.equals(row.getTenantId(),TenantContextHolder.getRequiredTenantId()) || row.getSystemUserId()!=null || !Objects.equals(row.getLeaderUserId(),contextService.resolveResponsibleLeaderUserId(SecurityFrameworkUtils.getLoginUserId()))) throw new IllegalStateException("SIGNATURE_PROFILE_SCOPE_MISMATCH");
        if(row.getSignatureCredentialGeneration()==null || row.getSignatureCredentialVersion()==null) throw new IllegalStateException("SIGNATURE_CREDENTIAL_METADATA_MISSING");
        boolean pin=SignatureCredentialPolicy.isPin(row.getSignatureCredentialType());
        return new Qualification(pin?"ACTIVE":row.getSignatureCredentialStatus(),!pin && !"ACTIVE".equals(row.getSignatureCredentialStatus()),!pin && SignatureCredentialPolicy.isExpired(row.getSignaturePasswordUpdatedAt(),now),!pin && row.getSignatureLockedUntil()!=null && row.getSignatureLockedUntil().isAfter(now),"MES_EMPLOYEE_PROFILE",row.getSignatureCredentialType());
    }
    private final MesProcessPoolTeamEmployeeProfileMapper profiles;
    private final PasswordEncoder encoder;
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.system.dal.mysql.signature.SignatureAuthenticationEventMapper events;
    private final Clock clock;
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceAccountContextService contextService;
    public MesEmployeeSignatureCredentialService(MesProcessPoolTeamEmployeeProfileMapper profiles,PasswordEncoder encoder,Clock clock) {this.profiles=profiles;this.encoder=encoder;this.clock=clock;}
    @Transactional(rollbackFor=Exception.class)
    public void change(cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner signer,String replacement) {
        Long profileId=signer.actorId();
        if(!SignatureCredentialPolicy.isStrong(replacement)) throw new IllegalArgumentException("SIGNATURE_PASSWORD_TOO_WEAK");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var row=profiles.selectSignatureIdentityForUpdate(tenant,profileId);
        if(row==null || row.getSystemUserId()!=null || !Boolean.TRUE.equals(row.getEnabled()) || row.getDisabledAt()!=null || !Objects.equals(row.getLeaderUserId(),contextService.resolveResponsibleLeaderUserId(SecurityFrameworkUtils.getLoginUserId()))) throw new IllegalStateException("SIGNATURE_PROFILE_SCOPE_MISMATCH");
        var now=LocalDateTime.ofInstant(clock.instant(),ZoneId.of("Asia/Shanghai")).withNano(0);
        if(!SignatureCredentialPolicy.isPin(row.getSignatureCredentialType()) && row.getSignatureLockedUntil()!=null && row.getSignatureLockedUntil().isAfter(now)) throw new IllegalStateException("IDENTITY_LOCKED");
        if(!Objects.equals(row.getSignatureCredentialGeneration(),signer.credentialGeneration()) || !Objects.equals(row.getSignatureCredentialVersion(),signer.credentialVersion()) || !Objects.equals(signer.operatorId(),SecurityFrameworkUtils.getLoginUserId())) throw new IllegalArgumentException("CREDENTIAL_VERSION_CHANGED");
        if((row.getSignaturePasswordHash()!=null && !row.getSignaturePasswordHash().isBlank() && encoder.matches(replacement,row.getSignaturePasswordHash())) || profiles.selectSignaturePasswordHistory(tenant,profileId).stream().anyMatch(hash->encoder.matches(replacement,hash))) throw new IllegalArgumentException("SIGNATURE_PASSWORD_HISTORY_REUSED");
        if(row.getSignaturePasswordHash()!=null && !row.getSignaturePasswordHash().isBlank() && profiles.insertSignaturePasswordHistory(tenant,profileId,row.getSignaturePasswordHash(),now)!=1) throw new IllegalStateException("SIGNATURE_PASSWORD_HISTORY_WRITE_FAILED");
        var update=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO();update.setId(profileId);update.setSignaturePasswordHash(encoder.encode(replacement));update.setSignaturePasswordUpdatedAt(now);update.setSignatureCredentialStatus("ACTIVE");
        if(profiles.updateById(update)!=1 || profiles.writeSignatureFailures(tenant,profileId,0,null,null)!=1) throw new IllegalStateException("SIGNATURE_PASSWORD_CHANGE_WRITE_FAILED");
        var stored=profiles.selectSignatureIdentityForUpdate(tenant,profileId);
        if(stored==null || row.getSignatureCredentialVersion()==null || stored.getSignatureCredentialVersion()==null || stored.getSignatureCredentialVersion()<=row.getSignatureCredentialVersion() || !"ACTIVE".equals(stored.getSignatureCredentialStatus()) || !Objects.equals(stored.getSignatureCredentialGeneration(),row.getSignatureCredentialGeneration())) throw new IllegalStateException("SIGNATURE_PASSWORD_CHANGE_READBACK_FAILED");
    }
    @Transactional(rollbackFor=Exception.class)
    public void updateSettings(cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner signer,
            cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesEmployeeSignaturePasswordChangeReqVO request) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var row=profiles.selectSignatureIdentityForUpdate(tenant,signer.actorId());
        if(row==null || row.getSystemUserId()!=null || !Boolean.TRUE.equals(row.getEnabled()) || row.getDisabledAt()!=null
            || !Objects.equals(row.getLeaderUserId(),contextService.resolveResponsibleLeaderUserId(SecurityFrameworkUtils.getLoginUserId()))
            || !Objects.equals(signer.operatorId(),SecurityFrameworkUtils.getLoginUserId())
            || !Objects.equals(row.getSignatureCredentialGeneration(),signer.credentialGeneration())
            || !Objects.equals(row.getSignatureCredentialVersion(),signer.credentialVersion())) throw new IllegalArgumentException("SIGNATURE_PROFILE_SCOPE_OR_VERSION_MISMATCH");
        String type=SignatureCredentialPolicy.requireType(request.getCredentialType());
        String pinHash=row.getSignaturePinHash();
        if(request.getNewPin()!=null) {
            if(!SignatureCredentialPolicy.validPin(request.getNewPin())) throw new IllegalArgumentException("PIN 码必须为六位数字");
            pinHash=encoder.encode(request.getNewPin());
        }
        if(request.getNewPassword()!=null) change(signer,request.getNewPassword());
        row=profiles.selectSignatureIdentityForUpdate(tenant,signer.actorId());
        if("PASSWORD".equals(type) && (!"ACTIVE".equals(row.getSignatureCredentialStatus()) || SignatureCredentialPolicy.isExpired(row.getSignaturePasswordUpdatedAt(),LocalDateTime.ofInstant(clock.instant(),ZoneId.of("Asia/Shanghai")))))
            throw new IllegalArgumentException("请先设置有效签名密码，再切换为密码签名");
        SignatureCredentialPolicy.credentialHash(type,pinHash,row.getSignaturePasswordHash());
        if(profiles.updateSignatureSettings(tenant,row.getId(),type,pinHash)!=1) throw new IllegalStateException("SIGNATURE_SETTINGS_WRITE_FAILED");
        var stored=profiles.selectSignatureIdentityForUpdate(tenant,row.getId());
        if(stored==null || !type.equals(stored.getSignatureCredentialType()) || !pinHash.equals(stored.getSignaturePinHash())
                || ((!type.equals(row.getSignatureCredentialType()) || !pinHash.equals(row.getSignaturePinHash())) && (stored.getSignatureCredentialVersion()==null || stored.getSignatureCredentialVersion()<=row.getSignatureCredentialVersion()))) throw new IllegalStateException("SIGNATURE_SETTINGS_READBACK_FAILED");
        var event=new cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO();
        event.setTenantId(tenant); event.setActorId(stored.getId()); event.setOperatorId(signer.operatorId()); event.setIdentityDomain("MES_EMPLOYEE_PROFILE");
        event.setRequestId(java.util.UUID.randomUUID().toString()); event.setOperationType("CHANGE_SIGNATURE_SETTINGS"); event.setOutcome("SUCCESS");
        event.setOccurredAt(LocalDateTime.ofInstant(clock.instant(),ZoneId.of("Asia/Shanghai")).withNano(0));
        event.setCredentialGeneration(stored.getSignatureCredentialGeneration()); event.setCredentialVersion(stored.getSignatureCredentialVersion());
        if(events.insert(event)!=1) throw new IllegalStateException("SIGNATURE_SETTINGS_AUDIT_FAILED");
    }

}
