package cn.iocoder.yudao.module.bpm.approval.service.provider;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalProviderDescriptor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ApprovalTaskProviderRegistry {

    private final Map<ApprovalModuleCode, ApprovalTaskProvider> providers;

    public ApprovalTaskProviderRegistry(List<ApprovalTaskProvider> providerList) {
        Map<ApprovalModuleCode, ApprovalTaskProvider> byModule = new EnumMap<>(ApprovalModuleCode.class);
        for (ApprovalTaskProvider provider : providerList == null ? List.<ApprovalTaskProvider>of() : providerList) {
            if (provider.getModuleCode() == null) {
                throw new IllegalArgumentException("Approval task provider requires module code: "
                        + provider.getClass().getName());
            }
            if (provider.getSupportedViewTypes() == null || provider.getSupportedViewTypes().isEmpty()) {
                throw new IllegalArgumentException("Approval task provider requires supported view types: "
                        + provider.getModuleCode());
            }
            ApprovalTaskProvider duplicated = byModule.putIfAbsent(provider.getModuleCode(), provider);
            if (duplicated != null) {
                throw new IllegalArgumentException("Duplicate approval task provider for module "
                        + provider.getModuleCode());
            }
        }
        this.providers = Map.copyOf(byModule);
    }

    public ApprovalTaskProvider requireProvider(ApprovalModuleCode moduleCode) {
        ApprovalTaskProvider provider = providers.get(moduleCode);
        if (provider == null) {
            throw new IllegalArgumentException("APPROVAL_ADAPTER_NOT_REGISTERED: " + moduleCode);
        }
        return provider;
    }

    public List<ApprovalTaskProvider> listProviders() {
        return new ArrayList<>(providers.values());
    }

    public List<ApprovalProviderDescriptor> listProviderDescriptors() {
        return providers.values().stream()
                .map(provider -> new ApprovalProviderDescriptor()
                        .setModuleCode(provider.getModuleCode())
                        .setModuleName(provider.getModuleName())
                        .setProviderCode(provider.getProviderCode())
                        .setProviderVersion(provider.getProviderVersion())
                        .setSupportedViewTypes(Objects.requireNonNull(provider.getSupportedViewTypes()))
                        .setCapabilities(Objects.requireNonNull(provider.getCapabilities())))
                .toList();
    }
}
