package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.bpm.controller.admin.approval.ApprovalTaskTimelineReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccProjectProductTimelineTest {
    private final DccProjectProductCreateService service = mock(DccProjectProductCreateService.class);
    private final DccProjectReviewerRoleService roles = mock(DccProjectReviewerRoleService.class);
    private final DccProjectProductTaskDelegate delegate = new DccProjectProductTaskDelegate(
            mock(DccProjectProductCreateRequestMapper.class), mock(DccProjectProductActorSupport.class), service);
    private DccProjectProductCreateRequestDO request;

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(delegate, "reviewerRoles", roles);
        request = new DccProjectProductCreateRequestDO();
        request.setTenantId(1L); request.setId(19L); request.setApplicantUserId(1L);
        request.setSubmittedTime(LocalDateTime.of(2026, 11, 5, 12, 0));
        request.setCreationReason("新增项目产品");
        request.setReviewerUserId(2L); request.setReviewedTime(request.getSubmittedTime().plusMinutes(1));
        request.setReviewReason("资料完整，审核通过"); request.setStatus("COMPLETED");
        request.setReviewContract("ROLE_SINGLE_REVIEW_V1"); request.setReviewerRoleId(10L);
        request.setReviewerRoleCode("dcc_project_reviewer");
        when(roles.isRoleContract(request)).thenAnswer(invocation -> request.getReviewContract() != null);
        when(service.getRequest(eq(2L), anyLong())).thenAnswer(invocation -> {
            assertEquals(request.getId(), invocation.getArgument(1));
            return request;
        });
    }

    private ApprovalTaskTimelineQueryContext context(String type, String processId) {
        var query = new ApprovalTaskTimelineReqVO().setModuleCode(ApprovalModuleCode.DCC)
                .setSourceTaskType(type).setSourceTaskId("DCC_PROJECT_PRODUCT:1:" + request.getId() + ":" + type)
                .setBusinessKey(request.getId().toString()).setProcessInstanceId(processId).toQuery();
        return ApprovalTaskTimelineQueryContext.of(2L, query.getModuleCode(), query.getSourceTaskType(),
                query.getSourceTaskId(), query.getBusinessKey(), query.getProcessInstanceId(), false);
    }

    @Test void completedRoleRequestWithEmptyGetProcessIdReturnsOnlyItsPersistedSubmitAndReview() {
        var entries = delegate.timeline(context(DccProjectProductTaskDelegate.REVIEW, ""));
        assertEquals(List.of("SUBMIT", DccProjectProductTaskDelegate.REVIEW),
                entries.stream().map(e -> e.getNodeCode()).toList());
        assertEquals(List.of("SUBMIT", "APPROVE"), entries.stream().map(e -> e.getAction()).toList());
        assertEquals("COMPLETED", entries.get(1).getStatus());
        assertEquals(request.getReviewReason(), entries.get(1).getComment());
        assertEquals(request.getReviewerUserId(), entries.get(1).getActorUserId());
        assertEquals(request.getReviewedTime(), entries.get(1).getActedAt());
        verify(service).getRequest(2L, 19L);
    }

    @Test void resubmittedRequestAndRejectedPredecessorKeepTheirOwnDecisionHistory() {
        request.setId(21L); request.setPreviousRequestId(20L);
        var approved = delegate.timeline(context(DccProjectProductTaskDelegate.REVIEW, ""));
        assertTrue(approved.stream().allMatch(e -> "21".equals(e.getBusinessKey())));
        assertEquals("APPROVE", approved.get(1).getAction());
        request.setId(20L); request.setPreviousRequestId(null); request.setResubmittedRequestId(21L);
        request.setStatus("REJECTED"); request.setReviewReason(null); request.setRejectReason("资料有误，请重提");
        var rejected = delegate.timeline(context(DccProjectProductTaskDelegate.REVIEW, ""));
        assertTrue(rejected.stream().allMatch(e -> "20".equals(e.getBusinessKey())));
        assertEquals("REJECT", rejected.get(1).getAction());
        assertEquals("REJECTED", rejected.get(1).getStatus());
        assertEquals(request.getRejectReason(), rejected.get(1).getComment());
    }

    @Test void legacyTwoStageRequestRetainsItsActualApprovalNode() {
        request.setReviewContract(null); request.setReviewerRoleId(null); request.setReviewerRoleCode(null);
        request.setApproverUserId(3L); request.setApprovedTime(request.getReviewedTime().plusMinutes(1));
        request.setApprovalReason("批准建档");
        var entries = delegate.timeline(context(DccProjectProductTaskDelegate.APPROVAL, ""));
        assertEquals(3, entries.size());
        assertEquals(DccProjectProductTaskDelegate.APPROVAL, entries.get(2).getNodeCode());
        assertEquals("APPROVE", entries.get(2).getAction());
        assertEquals(3L, entries.get(2).getActorUserId());
        assertEquals("批准建档", entries.get(2).getComment());
    }

    @Test void actualProcessIdWrongSourceMissingEvidenceAndUnreadableRequestStillFail() {
        assertThrows(IllegalArgumentException.class,
                () -> delegate.timeline(context(DccProjectProductTaskDelegate.REVIEW, "flowable-19")));
        var wrongTenantSource = context(DccProjectProductTaskDelegate.REVIEW, "");
        wrongTenantSource.setSourceTaskId("DCC_PROJECT_PRODUCT:9:19:DCC_PROJECT_PRODUCT_REVIEW");
        assertThrows(IllegalArgumentException.class, () -> delegate.timeline(wrongTenantSource));
        assertThrows(IllegalArgumentException.class,
                () -> delegate.timeline(context(DccProjectProductTaskDelegate.APPROVAL, "")));
        request.setReviewerUserId(null);
        assertThrows(IllegalStateException.class,
                () -> delegate.timeline(context(DccProjectProductTaskDelegate.REVIEW, "")));
        when(service.getRequest(2L, 19L)).thenThrow(new IllegalStateException("unreadable"));
        assertEquals("unreadable", assertThrows(IllegalStateException.class,
                () -> delegate.timeline(context(DccProjectProductTaskDelegate.REVIEW, ""))).getMessage());
    }
}
