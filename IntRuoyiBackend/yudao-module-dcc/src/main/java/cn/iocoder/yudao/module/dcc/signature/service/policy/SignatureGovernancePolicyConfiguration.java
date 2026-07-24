package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapter;
import cn.iocoder.yudao.module.dcc.signature.service.adapter.SignatureGovernanceAdapterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SignatureGovernancePolicyConfiguration {

    @Bean
    public SignatureGovernanceAdapterRegistry signatureGovernanceAdapterRegistry(
            List<SignatureGovernanceAdapter> adapters) {
        return new SignatureGovernanceAdapterRegistry(adapters);
    }

    @Bean
    public SignatureGovernancePolicyService signatureGovernancePolicyService(
            SignatureGovernancePolicySourceProvider policySourceProvider,
            SignatureGovernanceAdapterRegistry adapterRegistry) {
        return new SignatureGovernancePolicyServiceImpl(policySourceProvider, adapterRegistry);
    }
}
