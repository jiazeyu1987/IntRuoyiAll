package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrApprovalTaskAdapterTest {

    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @Mock
    private MesProEdhrReleaseService releaseService;
    @InjectMocks
    private MesProEdhrApprovalTaskAdapter adapter;

    @BeforeEach
    void setUp() {
        lenient().when(workTaskService.countApprovalCenterTodoDuplicateTasks(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(0L);
    }

    @Test
    void pageTodoMapsCandidateSignatureTasksToUnifiedSummary() {
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO()
                .setId(66L)
                .setTaskCode("EDHR-WT-66")
                .setTaskType("REVIEW")
                .setBatchCode("BATCH-001")
                .setWorkOrderCode("WO-001")
                .setProcessName("终检")
                .setExecutionId(880L)
                .setBpmTaskId("bpm-task-66")
                .setStatus("TODO")
                .setSignatureCellKey("QA_APPROVE")
                .setCreateTime(LocalDateTime.parse("2026-06-23T10:20:00"));
        when(workTaskService.getApprovalCenterTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(PageResult.empty());
        when(workTaskService.getApprovalCenterCandidateSignatureTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, "BATCH-001", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("EDHR:EDHR_WORK_TASK:66", summary.getId());
        assertEquals(ApprovalModuleCode.EDHR, summary.getModuleCode());
        assertEquals("EDHR_WORK_TASK", summary.getSourceTaskType());
        assertEquals("66", summary.getSourceTaskId());
        assertEquals("BATCH-001 / 终检", summary.getBusinessTitle());
        assertEquals("QA_APPROVE", summary.getCurrentNodeCode());
        assertEquals("880", summary.getProcessInstanceId());
        assertEquals(Boolean.TRUE, summary.getRequiresSignature());
        assertEquals("/mes/pro/feedback/edhr-work-task", summary.getDetailRoute());
        assertEquals("66", summary.getDetailQuery().get("workTaskId"));
        assertEquals("880", summary.getDetailQuery().get("executionId"));
        assertEquals("/mes/pro/feedback/edhr-approval/detail", summary.getDecisionDetailRoute());
        assertEquals("880", summary.getDecisionDetailQuery().get("id"));
        assertEquals("66", summary.getDecisionDetailQuery().get("workTaskId"));
        assertEquals("bpm-task-66", summary.getDecisionDetailQuery().get("bpmTaskId"));
        assertEquals(Set.of("REVIEW_IN_MODULE", "PROCESS_IN_MODULE"), summary.getAvailableActions());

        ArgumentCaptor<MesProEdhrWorkTaskPageReqVO> captor =
                ArgumentCaptor.forClass(MesProEdhrWorkTaskPageReqVO.class);
        verify(workTaskService).getApprovalCenterCandidateSignatureTodoPage(captor.capture(),
                org.mockito.ArgumentMatchers.eq(false));
        assertEquals("BATCH-001", captor.getValue().getBatchCode());
    }

    @Test
    void pageTodoUsesFillActionUrlAsDecisionDetailInsteadOfApprovalDetail() {
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO()
                .setId(1166L)
                .setTaskCode("EDHR-WT-1166")
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .setBatchCode("BATCH-FILL-001")
                .setWorkOrderCode("WO-FILL-001")
                .setProcessName("填写")
                .setExecutionId(1040L)
                .setBatchExecutionId(900000000524L)
                .setBatchTaskId(3757L)
                .setStatus("TODO")
                .setActionUrl("/mes/pro/feedback/edhr-execution/form?id=1040"
                        + "&executionId=1040"
                        + "&workTaskId=1166"
                        + "&fillCarrier=FORM"
                        + "&recordCategory=BATCH_RECORD"
                        + "&batchExecutionId=900000000524"
                        + "&batchTaskId=3757")
                .setCreateTime(LocalDateTime.parse("2026-07-20T13:20:00"));
        when(workTaskService.getApprovalCenterTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(workTaskService.getApprovalCenterCandidateSignatureTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(PageResult.empty());

        ApprovalTaskSummary summary = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, "BATCH-FILL-001", 1, 10))
                .getList().get(0);

        assertEquals("/mes/pro/feedback/edhr-execution/form", summary.getDecisionDetailRoute());
        assertNotEquals("/mes/pro/feedback/edhr-approval/detail", summary.getDecisionDetailRoute());
        assertEquals("1040", summary.getDecisionDetailQuery().get("id"));
        assertEquals("1040", summary.getDecisionDetailQuery().get("executionId"));
        assertEquals("1166", summary.getDecisionDetailQuery().get("workTaskId"));
        assertEquals("FORM", summary.getDecisionDetailQuery().get("fillCarrier"));
        assertEquals("BATCH_RECORD", summary.getDecisionDetailQuery().get("recordCategory"));
        assertEquals("900000000524", summary.getDecisionDetailQuery().get("batchExecutionId"));
        assertEquals("3757", summary.getDecisionDetailQuery().get("batchTaskId"));
        assertEquals(Set.of("PROCESS_IN_MODULE"), summary.getAvailableActions());
    }

    @Test
    void pageTodoMapsReviewTaskToExplicitModuleReviewAction() {
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO()
                .setId(1266L)
                .setTaskCode("EDHR-WT-1266")
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_REVIEW)
                .setBatchCode("BATCH-REVIEW-001")
                .setWorkOrderCode("WO-REVIEW-001")
                .setProcessName("审核")
                .setExecutionId(2040L)
                .setBpmTaskId("bpm-task-1266")
                .setStatus("TODO")
                .setSignatureCellKey("QA_REVIEW")
                .setCreateTime(LocalDateTime.parse("2026-07-20T14:20:00"));
        when(workTaskService.getApprovalCenterTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(workTaskService.getApprovalCenterCandidateSignatureTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(PageResult.empty());

        ApprovalTaskSummary summary = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, "BATCH-REVIEW-001", 1, 10))
                .getList().get(0);

        assertEquals(Set.of("REVIEW_IN_MODULE", "PROCESS_IN_MODULE"), summary.getAvailableActions());
    }

    @Test
    void listTimelineUsesExecutionIdFromSummaryInsteadOfNonNumericBpmTaskId() {
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO()
                .setId(77L)
                .setTaskCode("EDHR-WT-77")
                .setBatchCode("BATCH-002")
                .setProcessName("复核")
                .setExecutionId(990L)
                .setBpmTaskId("bpm-task-non-numeric-77")
                .setStatus("TODO")
                .setSignatureCellKey("QA_REVIEW")
                .setCreateTime(LocalDateTime.parse("2026-06-23T11:20:00"));
        when(workTaskService.getApprovalCenterTodoPage(org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(workTaskService.getApprovalCenterCandidateSignatureTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(PageResult.empty());
        MesProEdhrWorkTaskDO timelineTask = new MesProEdhrWorkTaskDO()
                .setId(77L)
                .setTaskType("REVIEW")
                .setStatus("TODO")
                .setAssigneeUserId(188L)
                .setProcessName("复核")
                .setSignatureCellKey("QA_REVIEW");
        timelineTask.setCreateTime(LocalDateTime.parse("2026-06-23T11:20:00"));
        when(workTaskService.getApprovalCenterTimelineTasks(77L, 990L, false))
                .thenReturn(List.of(timelineTask));

        ApprovalTaskSummary summary = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, "BATCH-002", 1, 10)).getList().get(0);
        List<ApprovalTaskTimelineEntry> timeline = adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(
                100L, ApprovalModuleCode.EDHR, summary.getSourceTaskType(), summary.getSourceTaskId(),
                summary.getBusinessKey(), summary.getProcessInstanceId()));

        assertEquals(1, timeline.size());
        assertEquals("EDHR:EDHR_WORK_TASK:77", timeline.get(0).getId());
        verify(workTaskService).getApprovalCenterTimelineTasks(77L, 990L, false);
    }

    @Test
    void pageTodoUsesApprovalCenterGlobalQueryWhenGlobalViewEnabled() {
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO()
                .setId(88L)
                .setTaskCode("EDHR-WT-88")
                .setBatchCode("BATCH-003")
                .setProcessName("审批")
                .setExecutionId(991L)
                .setStatus("TODO")
                .setCreateTime(LocalDateTime.parse("2026-06-23T12:20:00"));
        when(workTaskService.getApprovalCenterTodoPage(org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(workTaskService.getApprovalCenterCandidateSignatureTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(PageResult.empty());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, "BATCH-003", 1, 10, true));

        assertEquals(1L, page.getTotal());
        assertEquals("EDHR:EDHR_WORK_TASK:88", page.getList().get(0).getId());
    }

    @Test
    void pageTodoIncludesCandidateSignatureTasksInUnifiedTodo() {
        MesProEdhrWorkTaskRespVO assignedTask = new MesProEdhrWorkTaskRespVO()
                .setId(91L)
                .setTaskCode("EDHR-WT-91")
                .setBatchCode("BATCH-004")
                .setProcessName("填写")
                .setExecutionId(991L)
                .setStatus("TODO")
                .setCreateTime(LocalDateTime.parse("2026-06-23T12:20:00"));
        MesProEdhrWorkTaskRespVO signatureTask = new MesProEdhrWorkTaskRespVO()
                .setId(92L)
                .setTaskCode("EDHR-WT-92")
                .setBatchCode("BATCH-004")
                .setProcessName("复核签名")
                .setExecutionId(992L)
                .setStatus("TODO")
                .setSignatureCellKey("QA_REVIEW")
                .setCreateTime(LocalDateTime.parse("2026-06-23T12:25:00"));
        when(workTaskService.getApprovalCenterTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new PageResult<>(List.of(assignedTask), 1L));
        when(workTaskService.getApprovalCenterCandidateSignatureTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new PageResult<>(List.of(signatureTask), 1L));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, "BATCH-004", 1, 10));

        assertEquals(2L, page.getTotal());
        assertEquals(List.of("EDHR:EDHR_WORK_TASK:92", "EDHR:EDHR_WORK_TASK:91"),
                page.getList().stream().map(ApprovalTaskSummary::getId).toList());
        assertEquals(Boolean.TRUE, page.getList().get(0).getRequiresSignature());
    }

    @Test
    void pageTodoKeepsTotalStableWhenDuplicateAppearsOnlyInLargerFetchWindow() {
        MesProEdhrWorkTaskRespVO assignedOnly = new MesProEdhrWorkTaskRespVO()
                .setId(301L)
                .setTaskCode("EDHR-WT-301")
                .setBatchCode("BATCH-STABLE")
                .setProcessName("Fill")
                .setStatus("TODO")
                .setCreateTime(LocalDateTime.parse("2026-07-20T09:00:00"));
        MesProEdhrWorkTaskRespVO candidateOnly = new MesProEdhrWorkTaskRespVO()
                .setId(302L)
                .setTaskCode("EDHR-WT-302")
                .setBatchCode("BATCH-STABLE")
                .setProcessName("Sign")
                .setStatus("TODO")
                .setSignatureCellKey("QA_SIGN")
                .setCreateTime(LocalDateTime.parse("2026-07-20T09:05:00"));
        MesProEdhrWorkTaskRespVO duplicate = new MesProEdhrWorkTaskRespVO()
                .setId(303L)
                .setTaskCode("EDHR-WT-303")
                .setBatchCode("BATCH-STABLE")
                .setProcessName("Review")
                .setStatus("TODO")
                .setSignatureCellKey("QA_REVIEW")
                .setCreateTime(LocalDateTime.parse("2026-07-20T09:10:00"));
        when(workTaskService.getApprovalCenterTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenAnswer(invocation -> {
                    MesProEdhrWorkTaskPageReqVO reqVO = invocation.getArgument(0);
                    return reqVO.getPageSize() == 1
                            ? new PageResult<>(List.of(assignedOnly), 2L)
                            : new PageResult<>(List.of(assignedOnly, duplicate), 2L);
                });
        when(workTaskService.getApprovalCenterCandidateSignatureTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenAnswer(invocation -> {
                    MesProEdhrWorkTaskPageReqVO reqVO = invocation.getArgument(0);
                    return reqVO.getPageSize() == 1
                            ? new PageResult<>(List.of(candidateOnly), 2L)
                            : new PageResult<>(List.of(candidateOnly, duplicate), 2L);
                });
        when(workTaskService.countApprovalCenterTodoDuplicateTasks(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(1L);

        PageResult<ApprovalTaskSummary> firstPage = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, "BATCH-STABLE", 1, 1));
        PageResult<ApprovalTaskSummary> secondPage = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, "BATCH-STABLE", 2, 1));

        assertEquals(3L, firstPage.getTotal());
        assertEquals(3L, secondPage.getTotal());
    }

    @Test
    void pageTodoMapsReleaseApprovalTaskToReviewableApprovalCenterSummary() {
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO()
                .setId(166L)
                .setTaskCode("EDHR-REL-TASK-166")
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE)
                .setBatchExecutionId(7100L)
                .setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(9100L)
                .setBatchCode("BATCH-REL-001")
                .setWorkOrderCode("WO-REL-001")
                .setProcessName("最终放行审批")
                .setAssigneeUserId(188L)
                .setStatus("TODO")
                .setCreateTime(LocalDateTime.parse("2026-07-19T10:20:00"));
        when(workTaskService.getApprovalCenterTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(workTaskService.getApprovalCenterCandidateSignatureTodoPage(
                org.mockito.ArgumentMatchers.any(MesProEdhrWorkTaskPageReqVO.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(PageResult.empty());

        ApprovalTaskSummary summary = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.EDHR, "BATCH-REL-001", 1, 10))
                .getList().get(0);

        assertEquals("EDHR:EDHR_WORK_TASK:166", summary.getId());
        assertEquals("BATCH-REL-001 / 最终放行审批", summary.getBusinessTitle());
        assertEquals("RELEASE_APPROVE", summary.getCurrentNodeCode());
        assertNull(summary.getProcessInstanceId());
        assertEquals(Boolean.TRUE, summary.getRequiresSignature());
        assertEquals("/mes/pro/feedback/edhr-batch-execution/detail", summary.getDetailRoute());
        assertEquals("7100", summary.getDetailQuery().get("id"));
        assertEquals("166", summary.getDetailQuery().get("workTaskId"));
        assertEquals("approval", summary.getDetailQuery().get("focus"));
        assertEquals("9100", summary.getDetailQuery().get("releaseTransactionId"));
        assertEquals("/mes/pro/feedback/edhr-batch-execution/detail", summary.getDecisionDetailRoute());
        assertEquals("7100", summary.getDecisionDetailQuery().get("id"));
        assertEquals("166", summary.getDecisionDetailQuery().get("workTaskId"));
        assertEquals("approval", summary.getDecisionDetailQuery().get("focus"));
        assertEquals("9100", summary.getDecisionDetailQuery().get("releaseTransactionId"));
        assertEquals(Set.of("APPROVE", "REJECT", "PROCESS_IN_MODULE"), summary.getAvailableActions());
    }

    @Test
    void reviewApprovesReleaseTaskThroughExistingReleaseDomainService() {
        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setId(177L)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE)
                .setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(9200L);
        when(workTaskService.validateReleaseApprovalTask(177L, null)).thenReturn(task);

        ApprovalTaskReviewContext context = ApprovalTaskReviewContext.of(188L, ApprovalModuleCode.EDHR,
                "EDHR_WORK_TASK", "177", "177", null, ApprovalTaskReviewResult.APPROVE,
                "符合放行要求", "111111", false);
        context.setSignatureImageFileUrl("http://localhost/signature/177.png");
        adapter.review(context);

        ArgumentCaptor<MesProEdhrReleaseApproveReqVO> captor =
                ArgumentCaptor.forClass(MesProEdhrReleaseApproveReqVO.class);
        verify(releaseService).approve(captor.capture());
        assertEquals(9200L, captor.getValue().getReleaseTransactionId());
        assertEquals("APPROVAL-CENTER-APPROVE-177", captor.getValue().getIdempotencyKey());
        assertEquals("符合放行要求", captor.getValue().getApprovalOpinion());
        assertNotEquals("http://localhost/signature/177.png", captor.getValue().getSignoffEvidenceHash());
        assertTrue(captor.getValue().getSignoffEvidenceHash().matches("[0-9a-f]{64}"));
    }

}
