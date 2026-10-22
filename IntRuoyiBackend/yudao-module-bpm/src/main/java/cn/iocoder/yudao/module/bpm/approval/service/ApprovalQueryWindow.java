package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import java.util.List;

/** Internal database window. HTTP pageSize validation does not apply to this value. */
public record ApprovalQueryWindow(long offset, long limit) {
    public ApprovalQueryWindow {
        if (offset < 0 || limit < 0) throw new IllegalArgumentException("QUERY_WINDOW_RANGE_UNSUPPORTED");
        Math.addExact(offset, limit);
    }
    public static ApprovalQueryWindow from(ApprovalTaskQueryContext context) {
        if (context.getWindow() != null) return context.getWindow();
        long page = context.getPageNo() == null ? 1 : context.getPageNo();
        long size = context.getPageSize() == null ? 10 : context.getPageSize();
        if (page < 1 || size < 1) throw new IllegalArgumentException("QUERY_WINDOW_RANGE_UNSUPPORTED");
        return new ApprovalQueryWindow(Math.multiplyExact(page - 1,size),size);
    }
    public <T> PageResult<T> slice(List<T> orderedCompleteSet) {
        int from = (int)Math.min(offset, orderedCompleteSet.size());
        int to = (int)Math.min(Math.addExact(offset,limit),orderedCompleteSet.size());
        return new PageResult<>(orderedCompleteSet.subList(from,to),(long)orderedCompleteSet.size());
    }
}
