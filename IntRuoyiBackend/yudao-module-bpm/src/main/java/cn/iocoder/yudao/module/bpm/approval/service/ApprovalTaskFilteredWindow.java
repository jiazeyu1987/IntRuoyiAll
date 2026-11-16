package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import java.util.*;
import java.util.function.Predicate;

/** Fixed-size database cursors; only the requested result page is retained during exact proof filtering. */
public final class ApprovalTaskFilteredWindow {
    private ApprovalTaskFilteredWindow() { }
    @FunctionalInterface public interface Source { PageResult<ApprovalTaskSummary> page(long offset,long limit); }
    public static PageResult<ApprovalTaskSummary> read(List<Source> sources,ApprovalTaskQueryContext context,
            ApprovalQueryWindow requested,boolean exactFilteredCount,Predicate<ApprovalTaskSummary> matches) {
        var queue=new PriorityQueue<Cursor>((a,b)->ApprovalCenterServiceImpl.compareRows(a.peek(),b.peek(),context.getViewType()));
        long unfilteredTotal=0;
        for(var source:sources){var cursor=new Cursor(source);unfilteredTotal=Math.addExact(unfilteredTotal,cursor.total);if(cursor.hasNext())queue.add(cursor);}
        if(!exactFilteredCount&&requested.limit()==0)return new PageResult<>(List.of(),unfilteredTotal);
        long count=0,end=Math.addExact(requested.offset(),requested.limit());var result=new ArrayList<ApprovalTaskSummary>();
        while(!queue.isEmpty()) {
            var cursor=queue.remove();var row=cursor.next();
            if(matches.test(row)) {if(count>=requested.offset()&&count<end)result.add(row);count++;}
            if(!exactFilteredCount&&count>=end) return new PageResult<>(result,unfilteredTotal);
            if(cursor.hasNext())queue.add(cursor);
        }
        return new PageResult<>(result,exactFilteredCount?count:unfilteredTotal);
    }
    private static final class Cursor {
        private static final long BATCH=200;
        final Source source;long offset,total=-1;List<ApprovalTaskSummary> rows=List.of();int index;
        Cursor(Source source){this.source=source;load();}
        void load(){
            do {
                var page=Objects.requireNonNull(source.page(offset,BATCH),"APPROVAL_QUERY_PAGE_REQUIRED");
                long next=Objects.requireNonNull(page.getTotal(),"APPROVAL_QUERY_COUNT_REQUIRED");
                if(total>=0&&total!=next)throw new IllegalStateException("APPROVAL_QUERY_COUNT_CHANGED");
                total=next;rows=Objects.requireNonNull(page.getList(),"APPROVAL_QUERY_ROWS_REQUIRED");index=0;
                offset=Math.addExact(offset,Math.min(BATCH,Math.max(0,total-offset)));
                if(rows.size()>BATCH)throw new IllegalStateException("APPROVAL_QUERY_BATCH_TOO_LARGE");
            }while(rows.isEmpty()&&offset<total);
        }
        boolean hasNext(){if(index==rows.size()&&offset<total)load();return index<rows.size();}
        ApprovalTaskSummary peek(){return rows.get(index);}
        ApprovalTaskSummary next(){return rows.get(index++);}
    }
}
