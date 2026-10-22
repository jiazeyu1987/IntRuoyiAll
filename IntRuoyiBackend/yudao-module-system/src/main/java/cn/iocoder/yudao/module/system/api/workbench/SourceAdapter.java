package cn.iocoder.yudao.module.system.api.workbench;
public interface SourceAdapter {
 SourceDescriptor descriptor();
 RowOrderContract rowOrderContract();
 SourceIdentityContract identityContract();
 default String sourceId() { return descriptor().sourceId(); }
 boolean isAuthorized(long userId,long tenantId);
 /** Exact count, rejecting duplicate stable task keys in the business base (fixed SQL helper). */
 long count(QueryContext query);
 TaskChunk readChunk(QueryContext query,ChunkCursor after,int limit);
 /** Current locking read, exact key only; ignores personal hidden partition. */
 TaskSummary resolveVisibilityIdentity(QueryContext query,String businessId);
}
