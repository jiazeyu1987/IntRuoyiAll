package cn.iocoder.yudao.module.system.api.workbench;
import lombok.Data;
/** Internal context constructed from authenticated identity, not bound from HTTP. */
@Data
public class QueryContext {
 private Long userId, tenantId;
 private String visibility, taskType, filterField, filterOperator, filterValue, sortKey, sortOrder;
 private VisibilityFilter visibilityFilter;
 public boolean validated() { validate(); return true; }
 public boolean validatedChunk(ChunkCursor after,int fetchLimit) {
  validate();
  if(fetchLimit!=101 || (after!=null && (after.sourceId()==null || after.rowOrderKey()==null || after.rowOrderKey().isEmpty())))
   throw WorkbenchFailure.INVALID_QUERY.exception();
  if(after!=null) { RowOrderContract.validate(after.rowOrderKey()); SortContract.validateTime(after.createdAt()); SortContract.validateTime(after.dueAt()); }
  return true;
 }
 public void validate() {
  if(tenantId==null || tenantId<0 || userId==null || userId<=0 || visibilityFilter==null ||
   visibilityFilter.tenantId()!=tenantId || visibilityFilter.userId()!=userId ||
   !java.util.Objects.equals(visibility,visibilityFilter.partition()))throw WorkbenchFailure.INVALID_QUERY.exception();
  if(taskType!=null && (taskType.isBlank() || taskType.length()>100))throw WorkbenchFailure.INVALID_QUERY.exception();
  if(filterField!=null || filterOperator!=null || filterValue!=null) {
   if(filterField==null || filterOperator==null || !java.util.List.of("source","detail","statusLabel").contains(filterField) ||
    !java.util.List.of("contains","eq").contains(filterOperator) || filterValue==null || filterValue.length()>1000)
    throw WorkbenchFailure.INVALID_QUERY.exception();
  }
  if(sortKey!=null || sortOrder!=null) {
   if(sortKey==null || sortOrder==null || !java.util.List.of("taskType","source","detail","statusLabel").contains(sortKey) ||
    !java.util.List.of("asc","desc").contains(sortOrder))throw WorkbenchFailure.INVALID_QUERY.exception();
  }
 }
 public QueryContext partition(String partition) {
  QueryContext out = new QueryContext();
  out.setUserId(userId); out.setTenantId(tenantId); out.setVisibility(partition);
  if (visibilityFilter == null) throw WorkbenchFailure.VISIBILITY_CONTEXT_MISSING.exception();
  out.setVisibilityFilter(new VisibilityFilter(tenantId,userId,visibilityFilter.version(),partition));
  return out;
 }
 public void freezeVisibility(long version) {
  visibilityFilter = new VisibilityFilter(tenantId,userId,version,visibility);
 }
 public String querySpec() {
  return cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
   java.util.Arrays.asList(visibility,taskType,filterField,filterOperator,filterValue,sortKey,sortOrder));
 }
}
