package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Data
@Component
@ConfigurationProperties(prefix = "signature.governance.policy")
public class ConfigurableSignatureGovernancePolicySourceProvider implements SignatureGovernancePolicySourceProvider {

    private Map<SignatureGovernanceModuleCode, ModulePolicySourceProperties> modules = Map.of();

    @Override
    public Optional<SignatureGovernancePolicySourceStatus> findByModule(SignatureGovernanceModuleCode moduleCode) {
        ModulePolicySourceProperties source = modules.get(moduleCode);
        if (source == null || isBlank(source.getSourceCode()) || isBlank(source.getPolicyVersion())) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(source.getAuthorityConfirmed())) {
            if (isBlank(source.getOwner()) || isBlank(source.getApprovalRef())) {
                return Optional.of(SignatureGovernancePolicySourceStatus.unconfirmed(moduleCode,
                        source.getSourceCode(), source.getPolicyVersion(),
                        "Confirmed policy source is missing owner or approval reference"));
            }
            return Optional.of(SignatureGovernancePolicySourceStatus.confirmed(moduleCode,
                    source.getSourceCode(), source.getPolicyVersion(), source.getOwner(), source.getApprovalRef()));
        }
        return Optional.of(SignatureGovernancePolicySourceStatus.unconfirmed(moduleCode,
                source.getSourceCode(), source.getPolicyVersion(),
                isBlank(source.getBlockerReason()) ? "Policy source authority is not confirmed"
                        : source.getBlockerReason()));
    }

    @Data
    public static class ModulePolicySourceProperties {

        private String sourceCode;
        private String policyVersion;
        private Boolean authorityConfirmed;
        private String owner;
        private String approvalRef;
        private String blockerReason;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
