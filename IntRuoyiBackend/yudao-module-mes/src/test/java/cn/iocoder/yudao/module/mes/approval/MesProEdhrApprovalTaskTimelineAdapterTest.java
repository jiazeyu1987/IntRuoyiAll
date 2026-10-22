package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrApprovalTaskTimelineAdapterTest {
    @org.mockito.Mock private cn.iocoder.yudao.module.mes.approval.MesActiveOrderHandoffApprovalProjection handoffProjection;


    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @InjectMocks
    private MesProEdhrApprovalTaskAdapter adapter;

    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper persisted;
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper executions;

    @org.junit.jupiter.api.BeforeEach
    void injectFormalReader() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        var finalReader = new MesEdhrExecutionFinalOutcomeReader(executions,
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordApprovalSnapshotMapper.class),
                persisted, org.mockito.Mockito.mock(cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService.class),
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper.class));
        var reader = new MesEdhrApprovalOutcomeReader(persisted,
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper.class),
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseDecisionMapper.class),
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper.class),
                org.mockito.Mockito.mock(MesManagerReleaseCompletedActorResolver.class),
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService.class),
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService.class),
                finalReader);
        org.springframework.test.util.ReflectionTestUtils.setField(adapter,"outcomeReader",reader);
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }

    @Test
    void listTimelineMapsWorkTasksToUnifiedTimelineEntries() {
        MesProEdhrWorkTaskDO current = MesProEdhrWorkTaskDO.builder()
                .id(66L)
                .taskCode("EDHR-WT-66")
                .taskType("REVIEW")
                .executionId(880L)
                .workOrderId(93L)
                .processName("终检")
                .status("DONE")
                .assigneeUserId(100L)
                .signatureCellKey("QA_APPROVE")
                .completedAt(LocalDateTime.parse("2026-06-23T10:30:00"))
                .build();
        current.setCreateTime(LocalDateTime.parse("2026-06-23T10:20:00"));
        when(workTaskService.getApprovalCenterTimelineTasks(66L, 880L, false)).thenReturn(List.of(current));
        when(persisted.selectOne(any())).thenReturn(current);
        when(executions.selectOne(any())).thenReturn(null);

        List<ApprovalTaskTimelineEntry> entries = adapter.listTimeline(
                ApprovalTaskTimelineQueryContext.of(100L, ApprovalModuleCode.EDHR,
                        "EDHR_WORK_TASK", "66", "66", "880"));

        assertEquals(1, entries.size());
        assertEquals("EDHR:EDHR_WORK_TASK:66", entries.get(0).getId());
        assertEquals("任务已完成", entries.get(0).getActionLabel());
        assertEquals("UNKNOWN", entries.get(0).getDecisionState());
        org.junit.jupiter.api.Assertions.assertNull(entries.get(0).getApprovalResult());
        org.junit.jupiter.api.Assertions.assertNull(entries.get(0).getActorUserId());
        org.junit.jupiter.api.Assertions.assertNull(entries.get(0).getEvidenceType());
        assertEquals(current.getCompletedAt(),entries.get(0).getTaskCompletedAt());
    }

    @Test
    void listTimelineFailsFastWhenTimelineSourceMissing() {
        when(workTaskService.getApprovalCenterTimelineTasks(66L, 880L, false)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(100L, ApprovalModuleCode.EDHR,
                        "EDHR_WORK_TASK", "66", "66", "880")));

        assertEquals("APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline is empty", ex.getMessage());
    }

    @Test void mismatchedOrNonCanonicalTimelineIdentityFailsBeforeService() {
        for (String id : List.of("066","-66","0")) {
            assertThrows(IllegalArgumentException.class,() -> adapter.listTimeline(
                    ApprovalTaskTimelineQueryContext.of(100L,ApprovalModuleCode.EDHR,"EDHR_WORK_TASK",id,id,"880")));
        }
        assertThrows(IllegalArgumentException.class,() -> adapter.listTimeline(
                ApprovalTaskTimelineQueryContext.of(100L,ApprovalModuleCode.EDHR,"EDHR_WORK_TASK","66","67","880")));
        org.mockito.Mockito.verifyNoInteractions(workTaskService);
    }
}
