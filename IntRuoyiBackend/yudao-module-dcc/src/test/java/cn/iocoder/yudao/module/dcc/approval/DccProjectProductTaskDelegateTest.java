package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccProjectProductTaskDelegateTest {
    @Test void completedReviewAndApprovalNeverBecomeRejectedBecauseOpinionIsAbsent() {
        var service=mock(DccProjectProductCreateService.class);
        var delegate=new DccProjectProductTaskDelegate(mock(DccProjectProductCreateRequestMapper.class),mock(DccProjectProductActorSupport.class),service);
        var request=DccProjectProductCreateRequestDO.builder().id(10L).applicantUserId(99L).status("COMPLETED")
                .submittedTime(LocalDateTime.of(2026,10,26,9,0)).creationReason("创建项目")
                .reviewerUserId(7L).reviewedTime(LocalDateTime.of(2026,10,26,10,0))
                .approverUserId(8L).approvedTime(LocalDateTime.of(2026,10,26,11,0)).build();
        request.setTenantId(1L);when(service.getRequest(99L,10L)).thenReturn(request);
        var context=ApprovalTaskTimelineQueryContext.of(99L,ApprovalModuleCode.DCC,DccProjectProductTaskDelegate.REVIEW,
                "DCC_PROJECT_PRODUCT:1:10:DCC_PROJECT_PRODUCT_REVIEW","10",null);
        var entries=delegate.timeline(context);
        assertEquals("APPROVE",entries.get(1).getAction());assertNull(entries.get(1).getComment());
        assertEquals("APPROVE",entries.get(2).getAction());assertNull(entries.get(2).getComment());
        request.setStatus("REJECTED");request.setRejectReason("批准节点退回");
        entries=delegate.timeline(context);
        assertEquals("APPROVE",entries.get(1).getAction());assertNull(entries.get(1).getComment());
        assertEquals("REJECT",entries.get(2).getAction());assertEquals("批准节点退回",entries.get(2).getComment());
    }
    @Test void nativeTimelineAcceptsOmittedAndHttpBoundBlankProcessButRejectsRealBpmAndWrongTask() {
        var service=mock(DccProjectProductCreateService.class);
        var delegate=new DccProjectProductTaskDelegate(mock(DccProjectProductCreateRequestMapper.class),
                mock(DccProjectProductActorSupport.class),service);
        var request=DccProjectProductCreateRequestDO.builder().id(10L).applicantUserId(99L)
                .submittedTime(LocalDateTime.of(2026,10,26,9,0)).creationReason("创建项目").build();
        request.setTenantId(1L);
        when(service.getRequest(99L,10L)).thenReturn(request);
        var context=ApprovalTaskTimelineQueryContext.of(99L,ApprovalModuleCode.DCC,DccProjectProductTaskDelegate.REVIEW,
                "DCC_PROJECT_PRODUCT:1:10:DCC_PROJECT_PRODUCT_REVIEW","10",null);
        assertEquals("提交申请",delegate.timeline(context).get(0).getNodeName());
        context.setProcessInstanceId("");
        assertEquals(99L,delegate.timeline(context).get(0).getActorUserId());
        context.setProcessInstanceId("foreign-bpm");
        assertThrows(IllegalArgumentException.class,()->delegate.timeline(context));
        context.setProcessInstanceId("").setSourceTaskId("foreign-task");
        assertThrows(IllegalArgumentException.class,()->delegate.timeline(context));
        context.setSourceTaskId("DCC_PROJECT_PRODUCT:1:10:DCC_PROJECT_PRODUCT_REVIEW");
        when(service.getRequest(99L,10L)).thenThrow(new IllegalStateException("permission denied"));
        assertThrows(IllegalStateException.class,()->delegate.timeline(context));
    }
}
