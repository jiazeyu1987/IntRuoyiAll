package cn.iocoder.yudao.module.bpm.approval.service.provider;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;

import java.util.Objects;
import java.util.Set;

/**
 * Canonical declaration for an approval-capable module.
 */
public final class ApprovalModuleIntegrationDeclaration {

    private final ApprovalModuleCode moduleCode;
    private final String moduleName;
    private final String providerCode;
    private final Set<ApprovalTaskViewType> requiredViewTypes;
    private final Set<ApprovalTaskCapability> requiredCapabilities;
    private final String detailRouteBoundary;

    private ApprovalModuleIntegrationDeclaration(ApprovalModuleCode moduleCode,
                                                 String moduleName,
                                                 String providerCode,
                                                 Set<ApprovalTaskViewType> requiredViewTypes,
                                                 Set<ApprovalTaskCapability> requiredCapabilities,
                                                 String detailRouteBoundary) {
        this.moduleCode = Objects.requireNonNull(moduleCode, "APPROVAL_MODULE_DECLARATION_CODE_REQUIRED");
        this.moduleName = requireText(moduleName, "APPROVAL_MODULE_DECLARATION_NAME_REQUIRED");
        this.providerCode = requireText(providerCode, "APPROVAL_MODULE_DECLARATION_PROVIDER_CODE_REQUIRED");
        this.requiredViewTypes = Set.copyOf(Objects.requireNonNull(requiredViewTypes,
                "APPROVAL_MODULE_DECLARATION_VIEWS_REQUIRED"));
        this.requiredCapabilities = Set.copyOf(Objects.requireNonNull(requiredCapabilities,
                "APPROVAL_MODULE_DECLARATION_CAPABILITIES_REQUIRED"));
        this.detailRouteBoundary = requireText(detailRouteBoundary,
                "APPROVAL_MODULE_DECLARATION_DETAIL_ROUTE_REQUIRED");
        if (this.requiredViewTypes.isEmpty()) {
            throw new IllegalArgumentException("APPROVAL_MODULE_DECLARATION_VIEWS_REQUIRED: " + moduleCode);
        }
    }

    public static ApprovalModuleIntegrationDeclaration required(ApprovalModuleCode moduleCode,
                                                               String moduleName,
                                                               String providerCode,
                                                               Set<ApprovalTaskViewType> requiredViewTypes,
                                                               Set<ApprovalTaskCapability> requiredCapabilities,
                                                               String detailRouteBoundary) {
        return new ApprovalModuleIntegrationDeclaration(moduleCode, moduleName, providerCode, requiredViewTypes,
                requiredCapabilities, detailRouteBoundary);
    }

    public ApprovalModuleCode getModuleCode() {
        return moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public Set<ApprovalTaskViewType> getRequiredViewTypes() {
        return requiredViewTypes;
    }

    public Set<ApprovalTaskCapability> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public String getDetailRouteBoundary() {
        return detailRouteBoundary;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
