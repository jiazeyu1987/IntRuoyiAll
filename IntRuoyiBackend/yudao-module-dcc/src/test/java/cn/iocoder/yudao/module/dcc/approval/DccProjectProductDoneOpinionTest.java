package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccProjectProductDoneOpinionTest {
    private final DccProjectProductCreateRequestMapper requests = mock(DccProjectProductCreateRequestMapper.class);
    private final DccProjectProductActorSupport actors = mock(DccProjectProductActorSupport.class);
    private final DccProjectReviewerRoleService roles = mock(DccProjectReviewerRoleService.class);
    private final DccProjectProductTaskDelegate delegate = new DccProjectProductTaskDelegate(
            requests, actors, mock(DccProjectProductCreateService.class));
    private DccProjectProductCreateRequestDO row;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(delegate, "reviewerRoles", roles);
        when(actors.canQuery(2L)).thenReturn(true);
        row = new DccProjectProductCreateRequestDO();
        row.setId(19L); row.setTenantId(1L); row.setStatus("COMPLETED");
        row.setProjectName("项目A"); row.setProductName("产品A"); row.setProjectCode("P19");
        row.setReviewerUserId(2L); row.setConfiguredReviewerUserId(2L);
        row.setReviewedTime(LocalDateTime.of(2026, 11, 5, 11, 51));
        row.setReviewReason("RUN20261105115100A7K2项目产品资料核对通过");
        row.setWriteReason("后续写入重试的原因"); row.setCreationReason("申请原因");
        when(requests.selectNativeTaskRows()).thenReturn(List.of(row));
    }

    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    private List<ApprovalTaskSummary> done() {
        return delegate.list(ApprovalTaskQueryContext.of(2L, ApprovalTaskViewType.DONE,
                ApprovalModuleCode.DCC, null, 1, 20));
    }

    @Test void roleApprovalReadsTheFormalReviewOpinionInsteadOfWriteReason() {
        row.setReviewContract("ROLE_SINGLE_REVIEW_V1");
        when(roles.isRoleContract(row)).thenReturn(true);
        var result = done();
        assertEquals(1, result.size());
        assertEquals(ApprovalTaskReviewResult.APPROVE, result.get(0).getApprovalResult());
        assertEquals(row.getReviewReason(), result.get(0).getApprovalRemark());
        assertEquals(DccProjectProductTaskDelegate.REVIEW, result.get(0).getSourceTaskType());
    }

    @Test void legacyApprovalKeepsEachNodesOpinionSeparate() {
        row.setApproverUserId(2L); row.setApprovedTime(row.getReviewedTime().plusMinutes(1));
        row.setApprovalReason("批准节点意见");
        var result = done();
        assertEquals(2, result.size());
        assertEquals(row.getReviewReason(), result.get(0).getApprovalRemark());
        assertEquals(row.getApprovalReason(), result.get(1).getApprovalRemark());
    }

    @Test void rejectionUsesTheRejectedNodesReasonAndDoesNotOverwriteEarlierApproval() {
        row.setStatus("REJECTED"); row.setApproverUserId(2L);
        row.setApprovedTime(row.getReviewedTime().plusMinutes(1)); row.setRejectReason("批准驳回意见");
        var result = done();
        assertEquals(ApprovalTaskReviewResult.APPROVE, result.get(0).getApprovalResult());
        assertEquals(row.getReviewReason(), result.get(0).getApprovalRemark());
        assertEquals(ApprovalTaskReviewResult.REJECT, result.get(1).getApprovalResult());
        assertEquals(row.getRejectReason(), result.get(1).getApprovalRemark());
    }
}
