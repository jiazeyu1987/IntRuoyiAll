package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

import java.util.Optional;

@FunctionalInterface
public interface SignatureGovernancePolicySourceProvider {

    Optional<SignatureGovernancePolicySourceStatus> findByModule(SignatureGovernanceModuleCode moduleCode);
}
