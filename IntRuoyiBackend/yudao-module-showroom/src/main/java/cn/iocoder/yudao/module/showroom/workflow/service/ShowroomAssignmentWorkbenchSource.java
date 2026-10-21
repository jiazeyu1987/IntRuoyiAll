package cn.iocoder.yudao.module.showroom.workflow.service;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomWorkbenchTodoMapper;
import cn.iocoder.yudao.module.showroom.workbench.ShowroomAssignmentWorkbenchRow;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
public class ShowroomAssignmentWorkbenchSource implements SourceAdapter {
 @Resource private ShowroomWorkbenchTodoMapper mapper;
 @Resource private PermissionApi permissionApi;
 @Override public SourceDescriptor descriptor() { return new SourceDescriptor("SHOWROOM_ASSIGNMENT","showroom",1,4,"展厅补充指派","展厅","showroom.assignment.policy","showroom.assignment.open",1); }
 @Override public RowOrderContract rowOrderContract() { return RowOrderContract.UTF8_BINARY_DESC; }
 @Override public SourceIdentityContract identityContract() { return SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY; }
 @Override public boolean isAuthorized(long userId,long tenantId) { return permissionApi.hasAnyRolesOrSuperAdmin(userId, cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomRoleModelContract.editorRoleCode()); }
 @Override public long count(QueryContext query) {
  query.validate();
  Long invalid=mapper.countInvalidNotifications(query); if(invalid==null || invalid!=0) throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
  Long count=mapper.countAssignment(query);
  if(count==null || count<0)throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
  return count;
 }
 @Override public TaskChunk readChunk(QueryContext query,ChunkCursor after,int limit) {
  query.validate();
  if(limit!=100)throw WorkbenchFailure.INVALID_QUERY.exception();
  return TaskChunk.from(mapper.chunkAssignment(query,after,limit+1).stream().map(this::project).toList(),limit);
 }
 private TaskSummary project(ShowroomAssignmentWorkbenchRow row) {
  return row.summary(new ActionEnvelope(descriptor().actionKind(),descriptor().payloadVersion(),navigation(row)));
 }
 private Map<String,String> navigation(ShowroomAssignmentWorkbenchRow row) { return Map.of("assignmentId",row.getBusinessId()); }
 @Override public TaskSummary resolveVisibilityIdentity(QueryContext query,String businessId) {
  query.validate();
  if(businessId==null || !businessId.matches("[1-9][0-9]*"))throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
  long id; try { id=Long.parseLong(businessId); } catch (NumberFormatException failure) { throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception(failure); }
  mapper.lockAssignmentIdentity(query,id);
  java.util.List<ShowroomAssignmentWorkbenchRow> rows=mapper.identityAssignment(query,businessId);
  if(rows.size()!=1)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
  return project(rows.get(0));
 }
 private static String required(String value) { if(value==null || value.isBlank())throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception(); return value; }
 private static void put(Map<String,String> out,String key,String value) { if(value!=null)out.put(key,value); }
}
