package cn.iocoder.yudao.module.dcc.signature.service.portal;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SignatureGovernancePortalAdapterRegistry {

    private final Map<SignatureGovernanceModuleCode, SignatureGovernancePortalAdapter> adapterMap;

    public SignatureGovernancePortalAdapterRegistry(List<SignatureGovernancePortalAdapter> adapters) {
        Map<SignatureGovernanceModuleCode, SignatureGovernancePortalAdapter> result = new LinkedHashMap<>();
        if (adapters != null) {
            for (SignatureGovernancePortalAdapter adapter : adapters) {
                if (adapter == null || adapter.getModuleCode() == null) {
                    throw new IllegalArgumentException("Signature governance portal adapter registration requires module");
                }
                SignatureGovernancePortalAdapter previous = result.putIfAbsent(adapter.getModuleCode(), adapter);
                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate signature governance portal adapter for "
                            + adapter.getModuleCode());
                }
            }
        }
        this.adapterMap = Collections.unmodifiableMap(result);
    }

    public List<SignatureGovernancePortalAdapter> listAdapters() {
        return List.copyOf(adapterMap.values());
    }

    public Optional<SignatureGovernancePortalAdapter> findByModule(SignatureGovernanceModuleCode moduleCode) {
        return Optional.ofNullable(adapterMap.get(moduleCode));
    }
}
