package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrApprovalPreflightTest {
    private final MesProEdhrReleaseService release = mock(MesProEdhrReleaseService.class);
    private final MesProEdhrWorkTaskService tasks = mock(MesProEdhrWorkTaskService.class);
    private final MesProEdhrApprovalTaskAdapter adapter = new MesProEdhrApprovalTaskAdapter(tasks, release, null);

    @Test
    void canonicalApproveDelegatesToReadOnlyDomainPreflight() {
        adapter.preflightReview(context());
        verify(release).preflightApprovalCenterReview(100L, 177L);
        verifyNoMoreInteractions(release);
        verifyNoInteractions(tasks);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0177", " 177", "invalid", ""})
    void invalidOrNoncanonicalTaskIdentityFailsBeforeDomain(String id) {
        assertThrows(RuntimeException.class, () -> adapter.preflightReview(context().setSourceTaskId(id)));
        verifyNoInteractions(release, tasks);
    }

    @Test
    void rejectModuleOnlySourceAndMismatchedIdentity() {
        assertThrows(RuntimeException.class, () -> adapter.preflightReview(context().setSourceTaskId(null)));
        assertThrows(RuntimeException.class, () -> adapter.preflightReview(context().setBusinessKey(null)));
        assertThrows(RuntimeException.class, () -> adapter.preflightReview(context().setSourceTaskType(null)));
        assertThrows(RuntimeException.class, () -> adapter.preflightReview(context().setResult(ApprovalTaskReviewResult.REJECT)));
        assertThrows(RuntimeException.class, () -> adapter.preflightReview(context().setSourceTaskType("PQC_PRODUCTION_RELEASE")));
        assertThrows(RuntimeException.class, () -> adapter.preflightReview(context().setBusinessKey("178")));
        assertThrows(RuntimeException.class, () -> adapter.preflightReview(context().setProcessInstanceId("177")));
        verifyNoInteractions(release, tasks);
    }

    private ApprovalTaskReviewContext context() {
        return ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.EDHR, "EDHR_WORK_TASK", "177", "177",
                null, ApprovalTaskReviewResult.APPROVE, "reviewed", "test-only", false);
    }
}
