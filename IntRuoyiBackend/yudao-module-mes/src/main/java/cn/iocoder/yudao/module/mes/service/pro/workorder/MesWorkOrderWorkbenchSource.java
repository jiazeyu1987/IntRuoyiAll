package cn.iocoder.yudao.module.mes.service.pro.workorder;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.MesWorkbenchTodoMapper;
import cn.iocoder.yudao.module.mes.workbench.MesWorkOrderWorkbenchRow;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
public class MesWorkOrderWorkbenchSource implements SourceAdapter {
 @Resource private MesWorkbenchTodoMapper mapper;
 @Resource private PermissionApi permissionApi;
 @Override public SourceDescriptor descriptor() { return new SourceDescriptor("WORK_ORDER","mes",1,3,"待排产工单","排产","mes.workOrder.policy","mes.workOrder.open",1); }
 @Override public RowOrderContract rowOrderContract() { return RowOrderContract.UTF8_BINARY_DESC; }
 @Override public SourceIdentityContract identityContract() { return SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY; }
 @Override public boolean isAuthorized(long userId,long tenantId) { return permissionApi.hasAnyPermissions(userId, "mes:pro-work-order:query"); }
 @Override public long count(QueryContext query) {
  query.validate();

  Long count=mapper.countWorkOrder(query);
  if(count==null || count<0)throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
  return count;
 }
 @Override public TaskChunk readChunk(QueryContext query,ChunkCursor after,int limit) {
  query.validate();
  if(limit!=100)throw WorkbenchFailure.INVALID_QUERY.exception();
  return TaskChunk.from(mapper.chunkWorkOrder(query,after,limit+1).stream().map(this::project).toList(),limit);
 }
 private TaskSummary project(MesWorkOrderWorkbenchRow row) {
  return row.summary(new ActionEnvelope(descriptor().actionKind(),descriptor().payloadVersion(),navigation(row)));
 }
 private Map<String,String> navigation(MesWorkOrderWorkbenchRow row) { return row.getCode()==null ? Map.of() : Map.of("code",row.getCode()); }
 @Override public TaskSummary resolveVisibilityIdentity(QueryContext query,String businessId) {
  query.validate();
  if(businessId==null || !businessId.matches("[1-9][0-9]*"))throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
  long id; try { id=Long.parseLong(businessId); } catch (NumberFormatException failure) { throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception(failure); }
  mapper.lockWorkOrderIdentity(query,id);

  java.util.List<MesWorkOrderWorkbenchRow> rows=mapper.identityWorkOrder(query,businessId);
  if(rows.size()!=1)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
  return project(rows.get(0));
 }
 private static String required(String value) { if(value==null || value.isBlank())throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception(); return value; }
 private static void put(Map<String,String> out,String key,String value) { if(value!=null)out.put(key,value); }
}
