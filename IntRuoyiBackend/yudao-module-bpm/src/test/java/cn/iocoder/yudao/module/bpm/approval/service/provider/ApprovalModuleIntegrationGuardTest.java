package cn.iocoder.yudao.module.bpm.approval.service.provider;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalModuleIntegrationGuardTest {

    @Test
    void guardFailsFastWhenDeclaredModuleHasNoRegisteredProvider() {
        ApprovalTaskProviderRegistry registry = new ApprovalTaskProviderRegistry(List.of(
                provider(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.MY_INITIATED, ApprovalTaskViewType.CC),
                        Set.of(ApprovalTaskCapability.NOTIFICATION, ApprovalTaskCapability.AUDIT))));
        ApprovalModuleIntegrationDeclarations declarations = new ApprovalModuleIntegrationDeclarations(List.of(
                declaration(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.MY_INITIATED, ApprovalTaskViewType.CC),
                        Set.of(ApprovalTaskCapability.NOTIFICATION, ApprovalTaskCapability.AUDIT)),
                declaration(ApprovalModuleCode.DCC, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE))));

        ApprovalModuleIntegrationGuard guard = new ApprovalModuleIntegrationGuard(registry, declarations);

        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validate);
        assertTrue(ex.getMessage().contains("APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED"));
        assertTrue(ex.getMessage().contains("DCC"));
    }

    @Test
    void guardFailsFastWhenModuleCodeHasNoDeclaration() {
        ApprovalTaskProviderRegistry registry = new ApprovalTaskProviderRegistry(List.of(
                provider(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.MY_INITIATED, ApprovalTaskViewType.CC),
                        Set.of(ApprovalTaskCapability.NOTIFICATION, ApprovalTaskCapability.AUDIT)),
                provider(ApprovalModuleCode.DCC, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE)),
                provider(ApprovalModuleCode.EDHR, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE)),
                provider(ApprovalModuleCode.SHOWROOM, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE))));
        ApprovalModuleIntegrationDeclarations declarations = new ApprovalModuleIntegrationDeclarations(List.of(
                declaration(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.MY_INITIATED, ApprovalTaskViewType.CC),
                        Set.of(ApprovalTaskCapability.NOTIFICATION, ApprovalTaskCapability.AUDIT)),
                declaration(ApprovalModuleCode.DCC, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE)),
                declaration(ApprovalModuleCode.EDHR, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE))));

        ApprovalModuleIntegrationGuard guard = new ApprovalModuleIntegrationGuard(registry, declarations);

        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validate);
        assertTrue(ex.getMessage().contains("APPROVAL_MODULE_DECLARATION_MISSING"));
        assertTrue(ex.getMessage().contains("SHOWROOM"));
    }

    @Test
    void guardFailsFastWhenProviderMissesDeclaredCapability() {
        ApprovalTaskProviderRegistry registry = new ApprovalTaskProviderRegistry(List.of(
                provider(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.MY_INITIATED, ApprovalTaskViewType.CC),
                        Set.of(ApprovalTaskCapability.NOTIFICATION, ApprovalTaskCapability.AUDIT)),
                provider(ApprovalModuleCode.DCC, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE)),
                provider(ApprovalModuleCode.EDHR, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE)),
                provider(ApprovalModuleCode.SHOWROOM, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE))));
        ApprovalModuleIntegrationDeclarations declarations = new ApprovalModuleIntegrationDeclarations(List.of(
                declaration(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.MY_INITIATED, ApprovalTaskViewType.CC),
                        Set.of(ApprovalTaskCapability.NOTIFICATION, ApprovalTaskCapability.AUDIT)),
                declaration(ApprovalModuleCode.DCC, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.EVIDENCE_LEDGER)),
                declaration(ApprovalModuleCode.EDHR, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE)),
                declaration(ApprovalModuleCode.SHOWROOM, Set.of(ApprovalTaskViewType.TODO),
                        Set.of(ApprovalTaskCapability.TIMELINE))));

        ApprovalModuleIntegrationGuard guard = new ApprovalModuleIntegrationGuard(registry, declarations);

        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validate);
        assertTrue(ex.getMessage().contains("APPROVAL_ADAPTER_CAPABILITY_MISSING"));
        assertTrue(ex.getMessage().contains("DCC"));
        assertTrue(ex.getMessage().contains("EVIDENCE_LEDGER"));
    }

    @Test
    void baselineProvidersSatisfyPhase4Declarations() {
        ApprovalTaskProviderRegistry registry = new ApprovalTaskProviderRegistry(List.of(
                provider(ApprovalModuleCode.BPM,
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE,
                                ApprovalTaskViewType.MY_INITIATED, ApprovalTaskViewType.CC),
                        Set.of(ApprovalTaskCapability.NOTIFICATION, ApprovalTaskCapability.AUDIT)),
                provider(ApprovalModuleCode.DCC,
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.NOTIFICATION,
                                ApprovalTaskCapability.AUDIT, ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
                                ApprovalTaskCapability.EVIDENCE_LEDGER)),
                provider(ApprovalModuleCode.EDHR,
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT,
                                ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
                                ApprovalTaskCapability.EVIDENCE_LEDGER)),
                provider(ApprovalModuleCode.SHOWROOM,
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE,
                                ApprovalTaskViewType.MY_INITIATED),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.NOTIFICATION,
                                ApprovalTaskCapability.AUDIT, ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
                                ApprovalTaskCapability.EVIDENCE_LEDGER)),
                provider(ApprovalModuleCode.SRM,
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE,
                                ApprovalTaskViewType.MY_INITIATED),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT)),
                provider(ApprovalModuleCode.MES_FEEDBACK,
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE,
                                ApprovalTaskViewType.MY_INITIATED),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT))));
        ApprovalModuleIntegrationGuard guard = new ApprovalModuleIntegrationGuard(registry,
                new ApprovalModuleIntegrationDeclarations());

        assertDoesNotThrow(guard::validate);
    }

    private static ApprovalModuleIntegrationDeclaration declaration(ApprovalModuleCode moduleCode,
                                                                   Set<ApprovalTaskViewType> requiredViews,
                                                                   Set<ApprovalTaskCapability> requiredCapabilities) {
        return ApprovalModuleIntegrationDeclaration.required(moduleCode, moduleCode.name(),
                moduleCode.name().toLowerCase() + "-provider", requiredViews, requiredCapabilities,
                "/" + moduleCode.name().toLowerCase());
    }

    private static ApprovalTaskProvider provider(ApprovalModuleCode moduleCode,
                                                 Set<ApprovalTaskViewType> viewTypes,
                                                 Set<ApprovalTaskCapability> capabilities) {
        return new ApprovalTaskProvider() {

            @Override
            public ApprovalModuleCode getModuleCode() {
                return moduleCode;
            }

            @Override
            public String getModuleName() {
                return moduleCode.name();
            }

            @Override
            public String getProviderCode() {
                return moduleCode.name().toLowerCase() + "-provider";
            }

            @Override
            public String getProviderVersion() {
                return "phase4-test";
            }

            @Override
            public Set<ApprovalTaskViewType> getSupportedViewTypes() {
                return viewTypes;
            }

            @Override
            public Set<ApprovalTaskCapability> getCapabilities() {
                return capabilities;
            }

            @Override
            public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
                return PageResult.empty();
            }

            @Override
            public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
                return List.of();
            }
        };
    }
}
