package cn.iocoder.yudao.module.system.service.signature;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserSignatureSettingsReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.signature.SignatureAuthenticationEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;
import java.util.UUID;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
@Service
public class UserSignatureSettingsService {
    public record Settings(String credentialType) {}
    private final AdminUserMapper users;
    private final PasswordEncoder encoder;
    private final SignatureAuthenticationEventMapper events;
    private final Clock clock;
    public UserSignatureSettingsService(AdminUserMapper users, PasswordEncoder encoder, SignatureAuthenticationEventMapper events, Clock clock) {
        this.users=users; this.encoder=encoder; this.events=events; this.clock=clock;
    }
    @cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission(enable=false)
    public Settings get() {
        var user = users.selectSessionSubject(TenantContextHolder.getRequiredTenantId(), SecurityFrameworkUtils.getLoginUserId());
        if (user == null) throw exception(USER_NOT_EXISTS);
        SignatureCredentialPolicy.credentialHash(user.getSignatureCredentialType(), user.getSignaturePinHash(), user.getPassword());
        return new Settings(SignatureCredentialPolicy.requireType(user.getSignatureCredentialType()));
    }
    @Transactional(rollbackFor=Exception.class)
    @cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission(enable=false)
    public void update(UserSignatureSettingsReqVO request) {
        Long tenant = TenantContextHolder.getRequiredTenantId(), actor = SecurityFrameworkUtils.getLoginUserId();
        // Confirm the current user with the login password; no PIN or password is persisted in audit events.
        var user = users.selectPermissionSubjectForUpdate(tenant, actor);
        if (user == null) throw exception(USER_NOT_EXISTS);
        if (!Integer.valueOf(0).equals(user.getStatus())) throw exception(ESIGN_IDENTITY_DISABLED);
        if (!encoder.matches(request.getPassword(), user.getPassword())) throw exception(USER_PASSWORD_FAILED);
        String type = SignatureCredentialPolicy.requireType(request.getCredentialType());
        if (request.getNewPin()!=null && !SignatureCredentialPolicy.validPin(request.getNewPin())) throw new IllegalArgumentException("PIN 码必须为六位数字");
        String hash = request.getNewPin()==null ? user.getSignaturePinHash() : encoder.encode(request.getNewPin());
        SignatureCredentialPolicy.credentialHash("PIN", hash, null);
        if (type.equals(user.getSignatureCredentialType()) && hash.equals(user.getSignaturePinHash())) return;
        if (users.updateSignatureSettings(tenant, actor, type, hash)!=1) throw new IllegalStateException("SIGNATURE_SETTINGS_WRITE_FAILED");
        var stored=users.selectPermissionSubjectForUpdate(tenant,actor);
        if (stored==null || !type.equals(stored.getSignatureCredentialType()) || !hash.equals(stored.getSignaturePinHash())
                || stored.getSignatureCredentialVersion()==null || stored.getSignatureCredentialVersion()<=user.getSignatureCredentialVersion())
            throw new IllegalStateException("SIGNATURE_SETTINGS_READBACK_FAILED");
        var event=new SignatureAuthenticationEventDO(); event.setTenantId(tenant); event.setActorId(actor); event.setOperatorId(actor);
        event.setIdentityDomain("SYSTEM_USER"); event.setRequestId(UUID.randomUUID().toString()); event.setOperationType(request.getNewPin()==null?"CHANGE_METHOD":"CHANGE_PIN_AND_METHOD");
        event.setOutcome("SUCCESS"); event.setOccurredAt(LocalDateTime.ofInstant(clock.instant(),ZoneId.of("Asia/Shanghai")).withNano(0));
        event.setCredentialVersion(stored.getSignatureCredentialVersion()); event.setCredentialGeneration(stored.getSignatureCredentialGeneration());
        if(events.insert(event)!=1) throw new IllegalStateException("SIGNATURE_SETTINGS_AUDIT_FAILED");
    }
}
