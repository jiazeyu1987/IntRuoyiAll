package cn.iocoder.yudao.module.dcc.signature.service.adapter;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SignatureGovernanceAdapterRegistry {

    private final Map<SignatureGovernanceModuleCode, SignatureGovernanceAdapter> adapterMap;

    public SignatureGovernanceAdapterRegistry(List<SignatureGovernanceAdapter> adapters) {
        Map<SignatureGovernanceModuleCode, SignatureGovernanceAdapter> result = new LinkedHashMap<>();
        if (adapters != null) {
            for (SignatureGovernanceAdapter adapter : adapters) {
                if (adapter == null || adapter.getModuleCode() == null) {
                    throw new IllegalArgumentException("Signature governance adapter registration requires module");
                }
                SignatureGovernanceAdapter previous = result.putIfAbsent(adapter.getModuleCode(), adapter);
                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate signature governance adapter for "
                            + adapter.getModuleCode());
                }
            }
        }
        this.adapterMap = Map.copyOf(result);
    }

    public Optional<SignatureGovernanceAdapter> findByModule(SignatureGovernanceModuleCode moduleCode) {
        return Optional.ofNullable(adapterMap.get(moduleCode));
    }
}
