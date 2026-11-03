package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccProjectProductTimelineRegressionTest {
    private final DccProjectProductCreateService service = mock(DccProjectProductCreateService.class);
    private final DccProjectProductTaskDelegate delegate = new DccProjectProductTaskDelegate(null, null, service);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void nativeTimelineAcceptsAbsentBpmProcessAndRetainsSubmitAndReviewEvidence(String processId) {
        var request = DccProjectProductCreateRequestDO.builder().id(8L).applicantUserId(10L)
                .submittedTime(LocalDateTime.of(2026, 11, 3, 18, 0))
                .creationReason("新建虚拟产品").reviewerUserId(11L)
                .reviewedTime(LocalDateTime.of(2026, 11, 3, 18, 5)).reviewReason("通过").build();
        request.setTenantId(1L);
        when(service.getRequest(11L, 8L)).thenReturn(request);
        var entries = delegate.timeline(context(processId, "DCC_PROJECT_PRODUCT:1:8:DCC_PROJECT_PRODUCT_REVIEW"));
        assertEquals(2, entries.size());
        assertEquals("新建虚拟产品", entries.get(0).getComment());
        assertEquals("通过", entries.get(1).getComment());
        assertEquals("DCC_PROJECT_PRODUCT_REQUEST", entries.get(1).getEvidenceType());
    }

    @Test
    void nativeTimelineRejectsForeignProcessAndForgedSourceIdentity() {
        assertThrows(IllegalArgumentException.class, () -> delegate.timeline(context("bpm-1", "forged")));
        verifyNoInteractions(service);
        var request = DccProjectProductCreateRequestDO.builder().id(8L).build(); request.setTenantId(1L);
        when(service.getRequest(11L, 8L)).thenReturn(request);
        assertThrows(IllegalArgumentException.class, () -> delegate.timeline(context("", "forged")));
    }

    private ApprovalTaskTimelineQueryContext context(String processId, String sourceId) {
        return ApprovalTaskTimelineQueryContext.of(11L, ApprovalModuleCode.DCC,
                DccProjectProductTaskDelegate.REVIEW, sourceId, "8", processId);
    }
}
