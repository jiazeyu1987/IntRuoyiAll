package cn.iocoder.yudao.module.dcc.signature.service.portal;

import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SignatureGovernancePortalConfiguration {

    @Bean
    public SignatureGovernancePortalAdapterRegistry signatureGovernancePortalAdapterRegistry(
            List<SignatureGovernancePortalAdapter> adapters) {
        return new SignatureGovernancePortalAdapterRegistry(adapters);
    }

    @Bean
    public SignatureGovernancePortalService signatureGovernancePortalService(
            SignatureGovernancePolicyService policyService,
            SignatureGovernancePortalAdapterRegistry portalAdapterRegistry,
            DccElectronicSignatureAuthorizationMapper authorizationMapper) {
        return new SignatureGovernancePortalServiceImpl(policyService, portalAdapterRegistry, authorizationMapper);
    }
}
