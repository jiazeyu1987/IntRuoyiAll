package cn.iocoder.yudao.module.bpm.approval.service.provider;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Startup guard for the unified approval platform integration standard.
 */
@Component
public class ApprovalModuleIntegrationGuard implements SmartInitializingSingleton {

    private final ApprovalTaskProviderRegistry providerRegistry;
    private final ApprovalModuleIntegrationDeclarations declarations;

    public ApprovalModuleIntegrationGuard(ApprovalTaskProviderRegistry providerRegistry,
                                          ApprovalModuleIntegrationDeclarations declarations) {
        this.providerRegistry = providerRegistry;
        this.declarations = declarations;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validate();
    }

    void validate() {
        List<ApprovalModuleIntegrationDeclaration> requiredProviders = declarations.listRequiredProviders();
        Map<ApprovalModuleCode, ApprovalModuleIntegrationDeclaration> declarationMap =
                toDeclarationMap(requiredProviders);
        Map<ApprovalModuleCode, ApprovalTaskProvider> providerMap = toProviderMap(providerRegistry.listProviders());

        for (ApprovalModuleIntegrationDeclaration declaration : requiredProviders) {
            ApprovalTaskProvider provider = providerMap.get(declaration.getModuleCode());
            if (provider == null) {
                throw new IllegalStateException("APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: "
                        + declaration.getModuleCode());
            }
            validateProviderMetadata(provider);
            validateViews(provider, declaration.getRequiredViewTypes());
            validateCapabilities(provider, declaration.getRequiredCapabilities());
        }

        EnumSet<ApprovalModuleCode> missingDeclarations = EnumSet.allOf(ApprovalModuleCode.class);
        missingDeclarations.removeAll(declarationMap.keySet());
        if (!missingDeclarations.isEmpty()) {
            throw new IllegalStateException("APPROVAL_MODULE_DECLARATION_MISSING: " + missingDeclarations);
        }

        for (ApprovalModuleCode providerModule : providerMap.keySet()) {
            if (!declarationMap.containsKey(providerModule)) {
                throw new IllegalStateException("APPROVAL_ADAPTER_DECLARATION_REQUIRED: " + providerModule);
            }
        }
    }

    private static Map<ApprovalModuleCode, ApprovalModuleIntegrationDeclaration> toDeclarationMap(
            List<ApprovalModuleIntegrationDeclaration> declarations) {
        Map<ApprovalModuleCode, ApprovalModuleIntegrationDeclaration> byModule =
                new EnumMap<>(ApprovalModuleCode.class);
        for (ApprovalModuleIntegrationDeclaration declaration : declarations) {
            ApprovalModuleIntegrationDeclaration duplicate =
                    byModule.putIfAbsent(declaration.getModuleCode(), declaration);
            if (duplicate != null) {
                throw new IllegalStateException("APPROVAL_MODULE_DECLARATION_DUPLICATE: "
                        + declaration.getModuleCode());
            }
        }
        return byModule;
    }

    private static Map<ApprovalModuleCode, ApprovalTaskProvider> toProviderMap(List<ApprovalTaskProvider> providers) {
        Map<ApprovalModuleCode, ApprovalTaskProvider> byModule = new EnumMap<>(ApprovalModuleCode.class);
        for (ApprovalTaskProvider provider : providers) {
            byModule.put(provider.getModuleCode(), provider);
        }
        return byModule;
    }

    private static void validateProviderMetadata(ApprovalTaskProvider provider) {
        requireText(provider.getModuleName(), "APPROVAL_ADAPTER_MODULE_NAME_REQUIRED: " + provider.getModuleCode());
        requireText(provider.getProviderCode(), "APPROVAL_ADAPTER_PROVIDER_CODE_REQUIRED: " + provider.getModuleCode());
        requireText(provider.getProviderVersion(),
                "APPROVAL_ADAPTER_PROVIDER_VERSION_REQUIRED: " + provider.getModuleCode());
        Objects.requireNonNull(provider.getSupportedViewTypes(),
                "APPROVAL_ADAPTER_VIEW_TYPES_REQUIRED: " + provider.getModuleCode());
        Objects.requireNonNull(provider.getCapabilities(),
                "APPROVAL_ADAPTER_CAPABILITIES_REQUIRED: " + provider.getModuleCode());
    }

    private static void validateViews(ApprovalTaskProvider provider, Set<ApprovalTaskViewType> requiredViews) {
        EnumSet<ApprovalTaskViewType> missing = EnumSet.copyOf(requiredViews);
        missing.removeAll(provider.getSupportedViewTypes());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("APPROVAL_ADAPTER_VIEW_TYPE_MISSING: "
                    + provider.getModuleCode() + " missing " + missing);
        }
    }

    private static void validateCapabilities(ApprovalTaskProvider provider,
                                             Set<ApprovalTaskCapability> requiredCapabilities) {
        EnumSet<ApprovalTaskCapability> missing = requiredCapabilities.isEmpty()
                ? EnumSet.noneOf(ApprovalTaskCapability.class)
                : EnumSet.copyOf(requiredCapabilities);
        missing.removeAll(provider.getCapabilities());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("APPROVAL_ADAPTER_CAPABILITY_MISSING: "
                    + provider.getModuleCode() + " missing " + missing);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
