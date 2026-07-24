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

    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @InjectMocks
    private MesProEdhrApprovalTaskAdapter adapter;

    @Test
    void listTimelineMapsWorkTasksToUnifiedTimelineEntries() {
        MesProEdhrWorkTaskDO current = MesProEdhrWorkTaskDO.builder()
                .id(66L)
                .taskCode("EDHR-WT-66")
                .taskType("REVIEW")
                .executionId(880L)
                .processName("终检")
                .status("DONE")
                .assigneeUserId(100L)
                .signatureCellKey("QA_APPROVE")
                .completedAt(LocalDateTime.parse("2026-06-23T10:30:00"))
                .build();
        current.setCreateTime(LocalDateTime.parse("2026-06-23T10:20:00"));
        when(workTaskService.getApprovalCenterTimelineTasks(66L, 880L, false)).thenReturn(List.of(current));

        List<ApprovalTaskTimelineEntry> entries = adapter.listTimeline(
                ApprovalTaskTimelineQueryContext.of(100L, ApprovalModuleCode.EDHR,
                        "EDHR_WORK_TASK", "66", "66", "880"));

        assertEquals(1, entries.size());
        assertEquals("EDHR:EDHR_WORK_TASK:66", entries.get(0).getId());
        assertEquals("审批通过", entries.get(0).getActionLabel());
        assertEquals("REAL_WORK_TASK", entries.get(0).getEvidenceType());
    }

    @Test
    void listTimelineFailsFastWhenTimelineSourceMissing() {
        when(workTaskService.getApprovalCenterTimelineTasks(66L, 880L, false)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(100L, ApprovalModuleCode.EDHR,
                        "EDHR_WORK_TASK", "66", "66", "880")));

        assertEquals("APPROVAL_TIMELINE_SOURCE_REQUIRED: EDHR work task timeline is empty", ex.getMessage());
    }
}
