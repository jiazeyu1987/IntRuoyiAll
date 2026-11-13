package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import cn.iocoder.yudao.module.system.service.signature.SignatureCredentialPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reads the selected worker's method within the same production scope used for employee switching. */
@Service
public class MesFrontlineSignatureSettingsService {
    public record Settings(String credentialType) {}

    private final MesFrontlineEmployeeSwitchService employees;
    private final List<SignatureAuthenticationApi> providers;

    public MesFrontlineSignatureSettingsService(MesFrontlineEmployeeSwitchService employees,
                                                List<SignatureAuthenticationApi> providers) {
        this.employees = employees;
        this.providers = providers;
    }

    @Transactional(rollbackFor = Exception.class)
    public Settings get(Long activeOrderId, Long routeId, Long routeProcessId, Long processId,
                        Long actualEmployeeId, String identityDomain) {
        employees.switchActualEmployee(new MesFrontlineEmployeeSwitchCommand(SecurityFrameworkUtils.getLoginUserId(),
                activeOrderId, routeId, routeProcessId, processId, actualEmployeeId, identityDomain));
        var candidates = providers.stream().filter(provider -> provider.identityDomain().equals(identityDomain)).toList();
        if (candidates.size() != 1) {
            throw new IllegalStateException("SIGNATURE_IDENTITY_PROVIDER_NOT_UNIQUE: " + identityDomain);
        }
        return new Settings(SignatureCredentialPolicy.requireType(candidates.get(0).credentialType(actualEmployeeId)));
    }
}
