package cn.iocoder.yudao.module.showroom.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalDetail;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalSignatureRecord;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalTargetPreview;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequestItem;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowroomApprovalTaskAdapterTest {

    @Mock
    private ShowroomWorkflowFacade workflowFacade;
    @InjectMocks
    private ShowroomApprovalTaskAdapter adapter;

    @Test
    void pageTodoMapsPendingReviewerChangeRequestsToUnifiedSummary() {
        ShowroomChangeRequest request = new ShowroomChangeRequest(9001L, "PRODUCT", 7001L,
                8001L, "showroom", "MANUAL", "PENDING_SUPERVISOR_REVIEW",
                501L, 60L, 100L, 200L, null,
                List.of(new ShowroomChangeRequestItem("name_cn", "{\"value\":\"旧名称\"}",
                        "{\"value\":\"新名称\"}")));
        when(workflowFacade.listPendingApprovalsForReviewer(100L)).thenReturn(List.of(request));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.SHOWROOM, null, 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("SHOWROOM:SHOWROOM_CHANGE_REQUEST:9001", summary.getId());
        assertEquals(ApprovalModuleCode.SHOWROOM, summary.getModuleCode());
        assertEquals("SHOWROOM_CHANGE_REQUEST", summary.getSourceTaskType());
        assertEquals("9001", summary.getSourceTaskId());
        assertEquals("展厅内容变更 #9001", summary.getBusinessTitle());
        assertEquals("PENDING_SUPERVISOR_REVIEW", summary.getBusinessStatus());
        assertEquals("主管审核", summary.getCurrentNodeName());
        assertEquals(Boolean.TRUE, summary.getRequiresSignature());
        assertEquals("/showroom/approval", summary.getDetailRoute());
        assertEquals("9001", summary.getDetailQuery().get("changeRequestId"));
        assertTrue(summary.getAvailableActions().contains("PROCESS_IN_MODULE"));
        assertTrue(summary.getCapabilities().contains(ApprovalTaskCapability.SIGNATURE_AUTHORIZATION));
        assertTrue(summary.getCapabilities().contains(ApprovalTaskCapability.EVIDENCE_LEDGER));

        verify(workflowFacade).listPendingApprovalsForReviewer(100L);
    }

    @Test
    void pageTodoUsesGlobalPendingApprovalsWhenGlobalViewEnabled() {
        ShowroomChangeRequest request = new ShowroomChangeRequest(9002L, "PRODUCT", 7002L,
                8002L, "showroom", "MANUAL", "PENDING_GAOXIN_APPROVAL",
                501L, 60L, 100L, 200L, null, List.of());
        when(workflowFacade.listApprovals()).thenReturn(List.of(request));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.SHOWROOM, null, 1, 10, true));

        assertEquals(1L, page.getTotal());
        assertEquals("SHOWROOM:SHOWROOM_CHANGE_REQUEST:9002", page.getList().get(0).getId());
    }

    @Test
    void getCapabilitiesAdvertiseTimelineAfterShowroomUnifiedPlatformAdapterLands() {
        assertTrue(adapter.getCapabilities().contains(ApprovalTaskCapability.TIMELINE));
    }

    @Test
    void listTimelineMapsRealShowroomWorkflowAndSignatureRecordsToUnifiedTimeline() {
        Instant submittedAt = Instant.parse("2026-06-23T10:00:00Z");
        Instant supervisorSignedAt = Instant.parse("2026-06-23T10:30:00Z");
        ShowroomChangeRequest request = new ShowroomChangeRequest(9001L, "PRODUCT", 7001L,
                8001L, "showroom", "CONTENT_UPDATE", "MANUAL", "PENDING_GAOXIN_APPROVAL",
                "pi-showroom-9001", 501L, 60L, submittedAt, 100L, 60L, supervisorSignedAt,
                200L, null, null, null,
                List.of(new ShowroomChangeRequestItem("name_cn", "{\"value\":\"旧名称\"}",
                        "{\"value\":\"新名称\"}")));
        ShowroomApprovalSignatureRecord supervisorSignature = new ShowroomApprovalSignatureRecord(
                3001L, 9001L, "SUPERVISOR", "APPROVE", 100L, "PASSWORD",
                Boolean.TRUE, "主管确认通过", supervisorSignedAt);
        ShowroomApprovalDetail detail = new ShowroomApprovalDetail(request, request.items(),
                new ShowroomApprovalTargetPreview("PRODUCT", 7001L, 7000L, 8001L, Map.of(), Map.of()),
                List.of(), List.of(supervisorSignature));
        when(workflowFacade.getApproval(9001L)).thenReturn(detail);

        List<ApprovalTaskTimelineEntry> timeline = adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(
                200L, ApprovalModuleCode.SHOWROOM, "SHOWROOM_CHANGE_REQUEST", "9001", "9001",
                "pi-showroom-9001"));

        assertEquals(3, timeline.size());
        ApprovalTaskTimelineEntry submitted = timeline.get(0);
        assertEquals(ApprovalModuleCode.SHOWROOM, submitted.getModuleCode());
        assertEquals("SHOWROOM_CHANGE_REQUEST", submitted.getSourceTaskType());
        assertEquals("9001", submitted.getSourceTaskId());
        assertEquals("SUBMITTED", submitted.getAction());
        assertEquals("提交审批", submitted.getActionLabel());
        assertEquals(501L, submitted.getActorUserId());
        assertEquals("SHOWROOM_CHANGE_REQUEST", submitted.getEvidenceType());
        assertEquals("showroom_change_request:9001", submitted.getDomainReferenceId());

        ApprovalTaskTimelineEntry supervisor = timeline.get(1);
        assertEquals("SUPERVISOR", supervisor.getNodeCode());
        assertEquals("主管通过", supervisor.getActionLabel());
        assertEquals(100L, supervisor.getActorUserId());
        assertEquals("主管确认通过", supervisor.getComment());
        assertEquals("SHOWROOM_SIGNATURE", supervisor.getEvidenceType());
        assertEquals("showroom_change_request_signature:3001", supervisor.getDomainReferenceId());

        ApprovalTaskTimelineEntry publicityPending = timeline.get(2);
        assertEquals("PUBLICITY", publicityPending.getNodeCode());
        assertEquals("企宣审批", publicityPending.getNodeName());
        assertEquals("PENDING", publicityPending.getStatus());
        assertEquals(200L, publicityPending.getActorUserId());

        verify(workflowFacade).getApproval(9001L);
    }

    @Test
    void listTimelineAllowsGlobalViewForNonParticipant() {
        Instant submittedAt = Instant.parse("2026-06-23T10:00:00Z");
        ShowroomChangeRequest request = new ShowroomChangeRequest(9003L, "PRODUCT", 7003L,
                8003L, "showroom", "CONTENT_UPDATE", "MANUAL", "PENDING_SUPERVISOR_REVIEW",
                "pi-showroom-9003", 501L, 60L, submittedAt, 100L, 60L, null,
                200L, null, null, null, List.of());
        ShowroomApprovalDetail detail = new ShowroomApprovalDetail(request, request.items(),
                new ShowroomApprovalTargetPreview("PRODUCT", 7003L, 7002L, 8003L, Map.of(), Map.of()),
                List.of(), List.of());
        when(workflowFacade.getApproval(9003L)).thenReturn(detail);

        List<ApprovalTaskTimelineEntry> timeline = adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(
                999L, ApprovalModuleCode.SHOWROOM, "SHOWROOM_CHANGE_REQUEST", "9003", "9003",
                "pi-showroom-9003", true));

        assertEquals(3, timeline.size());
        assertEquals("SUBMITTED", timeline.get(0).getNodeCode());
        assertEquals("SUPERVISOR", timeline.get(1).getNodeCode());
        assertEquals("PUBLICITY", timeline.get(2).getNodeCode());
        assertEquals("APPROVED", timeline.get(2).getStatus());
    }
}
