package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ApprovalTaskWindowMergeTest {
    @Test void mergesCompleteOrderedSetsAcross200And1000Boundaries() {
        var bpm=source(ApprovalModuleCode.BPM,1503,0);
        var edhr=source(ApprovalModuleCode.EDHR,1307,1);
        var context=ApprovalTaskQueryContext.of(7L,ApprovalTaskViewType.TODO,null,null,1,10);
        var expected=ApprovalCenterServiceImpl.sortRows(java.util.stream.Stream.concat(bpm.rows.stream(),edhr.rows.stream()).toList(),context.getViewType());
        var page=ApprovalTaskWindowMerge.read(List.of(bpm.provider,edhr.provider),context,new ApprovalQueryWindow(1100,200));
        assertEquals(2810L,page.getTotal());assertEquals(expected.subList(1100,1300),page.getList());
        verify(bpm.provider,never()).page(any());verify(edhr.provider,never()).page(any());
    }
    @Test void keepsLongWindowAndRejectsIncompleteOrUnorderedProvider() {
        var context=ApprovalTaskQueryContext.of(7L,ApprovalTaskViewType.TODO,null,null,Integer.MAX_VALUE,200);
        assertEquals(429496729200L,ApprovalQueryWindow.from(context).offset());
        assertTrue(new ApprovalQueryWindow(Long.MAX_VALUE-1,1).slice(List.of(1,2)).getList().isEmpty());
        assertThrows(ArithmeticException.class,()->new ApprovalQueryWindow(Long.MAX_VALUE,1));
        var broken=source(ApprovalModuleCode.BPM,2,0);
        doReturn(new PageResult<>(List.of(),2L)).when(broken.provider).window(any(),any());
        assertThrows(IllegalStateException.class,()->ApprovalTaskWindowMerge.read(List.of(broken.provider),context,new ApprovalQueryWindow(0,1)));
    }
    @Test void sourceIdentityUsesUtf8TextOrderAtEqualBusinessTime() {
        var rows=List.of(row(ApprovalModuleCode.BPM,"2",0),row(ApprovalModuleCode.BPM,"10",0),row(ApprovalModuleCode.BPM,"1",0));
        assertEquals(List.of("1","10","2"),ApprovalCenterServiceImpl.sortRows(rows,ApprovalTaskViewType.TODO)
                .stream().map(ApprovalTaskSummary::getSourceTaskId).toList());
    }
    private static Source source(ApprovalModuleCode module,int count,int phase) {
        var rows=new ArrayList<ApprovalTaskSummary>();
        for(int i=0;i<count;i++) rows.add(row(module,Integer.toString(i),i*2+phase));
        var ordered=ApprovalCenterServiceImpl.sortRows(rows,ApprovalTaskViewType.TODO);
        var provider=mock(ApprovalTaskProvider.class);when(provider.getModuleCode()).thenReturn(module);
        when(provider.window(any(),any())).thenAnswer(call->((ApprovalQueryWindow)call.getArgument(1)).slice(ordered));
        return new Source(provider,ordered);
    }
    private static ApprovalTaskSummary row(ApprovalModuleCode module,String id,int second) {
        return ApprovalTaskSummary.builder().moduleCode(module).sourceTaskType("TASK").sourceTaskId(id)
                .taskCreatedAt(LocalDateTime.of(2026,10,10,12,0).minusSeconds(second)).build();
    }
    private record Source(ApprovalTaskProvider provider,List<ApprovalTaskSummary> rows) { }
}
