package cn.iocoder.yudao.module.bpm.approval.service.provider;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalProviderDescriptor;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalTaskProviderRegistryTest {

    @Test
    void registryResolvesProviderAndDescriptorByModuleCode() {
        ApprovalTaskProvider provider = provider(ApprovalModuleCode.SHOWROOM, "Showroom 审批",
                ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE);

        ApprovalTaskProviderRegistry registry = new ApprovalTaskProviderRegistry(List.of(provider));

        assertSame(provider, registry.requireProvider(ApprovalModuleCode.SHOWROOM));
        List<ApprovalProviderDescriptor> descriptors = registry.listProviderDescriptors();
        assertEquals(1, descriptors.size());
        assertEquals(ApprovalModuleCode.SHOWROOM, descriptors.get(0).getModuleCode());
        assertEquals("Showroom 审批", descriptors.get(0).getModuleName());
        assertTrue(descriptors.get(0).getSupportedViewTypes().contains(ApprovalTaskViewType.TODO));
        assertTrue(descriptors.get(0).getCapabilities().contains(ApprovalTaskCapability.SIGNATURE_AUTHORIZATION));
    }

    @Test
    void registryRejectsDuplicateModuleCode() {
        ApprovalTaskProvider first = provider(ApprovalModuleCode.DCC, "DCC 文控", ApprovalTaskViewType.TODO);
        ApprovalTaskProvider duplicate = provider(ApprovalModuleCode.DCC, "DCC 复制项", ApprovalTaskViewType.DONE);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ApprovalTaskProviderRegistry(List.of(first, duplicate)));

        assertTrue(ex.getMessage().contains("Duplicate approval task provider"));
        assertTrue(ex.getMessage().contains("DCC"));
    }

    @Test
    void registryRejectsProviderWithoutModuleCode() {
        ApprovalTaskProvider provider = provider(null, "缺少模块码", ApprovalTaskViewType.TODO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ApprovalTaskProviderRegistry(List.of(provider)));

        assertTrue(ex.getMessage().contains("requires module code"));
    }

    private static ApprovalTaskProvider provider(ApprovalModuleCode moduleCode, String moduleName,
                                                 ApprovalTaskViewType... viewTypes) {
        return new ApprovalTaskProvider() {

            @Override
            public ApprovalModuleCode getModuleCode() {
                return moduleCode;
            }

            @Override
            public String getModuleName() {
                return moduleName;
            }

            @Override
            public String getProviderCode() {
                return "test-provider";
            }

            @Override
            public String getProviderVersion() {
                return "phase1";
            }

            @Override
            public Set<ApprovalTaskViewType> getSupportedViewTypes() {
                return Set.of(viewTypes);
            }

            @Override
            public Set<ApprovalTaskCapability> getCapabilities() {
                return Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
                        ApprovalTaskCapability.EVIDENCE_LEDGER);
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
