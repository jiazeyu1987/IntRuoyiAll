package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordService;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProviderRegistry;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ApprovalCenterTimelineContractTest {

    @Test
    void listTaskTimelineReturnsTimelineFromProvider() {
        ApprovalTaskProvider provider = providerWithTimeline();
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), mock(PermissionApi.class),
                mock(AdminUserApi.class), mock(ApprovalSignatureRecordService.class));
        ApprovalTaskTimelineQuery query = new ApprovalTaskTimelineQuery()
                .setModuleCode(ApprovalModuleCode.DCC)
                .setSourceTaskType("DCC_CONTROLLED_FILE_TASK")
                .setSourceTaskId("task-1")
                .setBusinessKey("6001")
                .setProcessInstanceId("pi-1");

        List<ApprovalTaskTimelineEntry> entries = service.listTaskTimeline(100L, query);

        assertEquals(1, entries.size());
        assertEquals("timeline-1", entries.get(0).getId());
        assertEquals("审批通过", entries.get(0).getActionLabel());
    }

    @Test
    void listTaskTimelineFailsFastWhenProviderDoesNotSupportTimeline() {
        ApprovalTaskProvider provider = new ApprovalTaskProvider() {
            @Override
            public ApprovalModuleCode getModuleCode() {
                return ApprovalModuleCode.SHOWROOM;
            }

            @Override
            public String getModuleName() {
                return "Showroom";
            }

            @Override
            public String getProviderCode() {
                return "showroom";
            }

            @Override
            public String getProviderVersion() {
                return "phase2-red";
            }

            @Override
            public Set<ApprovalTaskViewType> getSupportedViewTypes() {
                return Set.of(ApprovalTaskViewType.TODO);
            }

            @Override
            public Set<ApprovalTaskCapability> getCapabilities() {
                return Set.of();
            }

            @Override
            public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
                return new PageResult<>(List.of(), 0L);
            }

            @Override
            public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
                throw new UnsupportedOperationException("should not be called");
            }
        };
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), mock(PermissionApi.class),
                mock(AdminUserApi.class), mock(ApprovalSignatureRecordService.class));
        ApprovalTaskTimelineQuery query = new ApprovalTaskTimelineQuery()
                .setModuleCode(ApprovalModuleCode.SHOWROOM)
                .setSourceTaskType("SHOWROOM_CHANGE_REQUEST")
                .setSourceTaskId("9001");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.listTaskTimeline(100L, query));

        assertTrue(ex.getMessage().contains("APPROVAL_TIMELINE_UNSUPPORTED"));
        assertTrue(ex.getMessage().contains("SHOWROOM"));
    }

    private static ApprovalTaskProvider providerWithTimeline() {
        return new ApprovalTaskProvider() {
            @Override
            public ApprovalModuleCode getModuleCode() {
                return ApprovalModuleCode.DCC;
            }

            @Override
            public String getModuleName() {
                return "DCC";
            }

            @Override
            public String getProviderCode() {
                return "dcc";
            }

            @Override
            public String getProviderVersion() {
                return "phase2-red";
            }

            @Override
            public Set<ApprovalTaskViewType> getSupportedViewTypes() {
                return Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE);
            }

            @Override
            public Set<ApprovalTaskCapability> getCapabilities() {
                return Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT);
            }

            @Override
            public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
                return new PageResult<>(List.of(), 0L);
            }

            @Override
            public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
                return List.of(ApprovalTaskTimelineEntry.builder()
                        .id("timeline-1")
                        .moduleCode(ApprovalModuleCode.DCC)
                        .sourceTaskType("DCC_CONTROLLED_FILE_TASK")
                        .sourceTaskId("task-1")
                        .businessKey("6001")
                        .nodeCode("DOC_CONTROL_REVIEW")
                        .nodeName("文控审核")
                        .action("APPROVED")
                        .actionLabel("审批通过")
                        .actorUserId(100L)
                        .actedAt(LocalDateTime.parse("2026-06-23T10:30:00"))
                        .comment("通过")
                        .status("DONE")
                        .evidenceType("FLOWABLE_HISTORY")
                        .domainReferenceId("task-1")
                        .build());
            }
        };
    }
}
