package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import java.util.*;

/** Bounded, ordered provider cursors; offsets/counts remain long throughout. */
public final class ApprovalTaskWindowMerge {
    private ApprovalTaskWindowMerge() { }
    public static PageResult<ApprovalTaskSummary> read(List<ApprovalTaskProvider> providers,
            ApprovalTaskQueryContext context,ApprovalQueryWindow target) {
        var queue=new PriorityQueue<Cursor>((a,b)->ApprovalCenterServiceImpl.compareRows(a.peek(),b.peek(),context.getViewType()));
        long total=0;
        for(var provider:providers) {
            var cursor=new Cursor(provider,context); total=Math.addExact(total,cursor.total);
            if(cursor.hasNext()) queue.add(cursor);
        }
        if(target.offset()>=total || target.limit()==0) return new PageResult<>(List.of(),total);
        var result=new ArrayList<ApprovalTaskSummary>(); long position=0;
        long end=Math.min(total,Math.addExact(target.offset(),target.limit()));
        while(position<end) {
            Cursor cursor=Objects.requireNonNull(queue.poll(),"APPROVAL_QUERY_WINDOW_INCOMPLETE");
            var row=cursor.next();
            if(position>=target.offset()) result.add(row);
            position++;
            if(cursor.hasNext()) queue.add(cursor);
        }
        return new PageResult<>(result,total);
    }
    private static final class Cursor {
        final ApprovalTaskProvider provider; final ApprovalTaskQueryContext context; long total=-1,offset=0;
        List<ApprovalTaskSummary> rows=List.of(); int index; ApprovalTaskSummary previous;
        Cursor(ApprovalTaskProvider provider,ApprovalTaskQueryContext input) {
            this.provider=provider;
            this.context=ApprovalTaskQueryContext.of(input.getLoginUserId(),input.getViewType(),provider.getModuleCode(),
                    input.getKeyword(),1,200,input.isGlobalView()); load();
        }
        void load() {
            var page=Objects.requireNonNull(provider.window(context,new ApprovalQueryWindow(offset,200)),"APPROVAL_QUERY_PAGE_REQUIRED");
            long count=Objects.requireNonNull(page.getTotal(),"APPROVAL_QUERY_COUNT_REQUIRED");
            if(count<0 || total>=0 && count!=total) throw new IllegalStateException("APPROVAL_QUERY_COUNT_CHANGED");
            total=count; rows=Objects.requireNonNull(page.getList(),"APPROVAL_QUERY_ROWS_REQUIRED"); index=0;
            if(rows.size()!=Math.min(200,Math.max(0,total-offset))) throw new IllegalStateException("APPROVAL_QUERY_WINDOW_INCOMPLETE");
            for(var row:rows) {
                row.setSortTimeMissing((context.getViewType()==cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType.DONE
                        ? row.getTaskCompletedAt() : context.getViewType()==cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType.MY_INITIATED
                        ? row.getInitiatedAt() : row.getTaskCreatedAt())==null);
                if(row.getModuleCode()!=provider.getModuleCode() || previous!=null
                        && ApprovalCenterServiceImpl.compareRows(previous,row,context.getViewType())>=0)
                    throw new IllegalStateException("APPROVAL_QUERY_ORDER_OR_IDENTITY_INVALID");
                previous=row;
            }
        }
        boolean hasNext() { return offset+index<total; }
        ApprovalTaskSummary peek() { return rows.get(index); }
        ApprovalTaskSummary next() {
            var row=rows.get(index++);
            if(index==rows.size() && offset+index<total) { offset+=index;load(); }
            return row;
        }
    }
}
