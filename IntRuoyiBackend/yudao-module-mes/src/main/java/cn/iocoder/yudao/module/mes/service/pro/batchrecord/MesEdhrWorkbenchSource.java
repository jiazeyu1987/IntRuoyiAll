package cn.iocoder.yudao.module.mes.service.pro.batchrecord;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.MesWorkbenchTodoMapper;
import cn.iocoder.yudao.module.mes.workbench.MesEdhrWorkbenchRow;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
public class MesEdhrWorkbenchSource implements SourceAdapter {
 @Resource private MesWorkbenchTodoMapper mapper;
 @Resource private PermissionApi permissionApi;
 @Override public SourceDescriptor descriptor() { return new SourceDescriptor("EDHR_WORK_TASK","mes",1,2,"eDHR工作任务","批记录","mes.edhr.policy","mes.edhr.open",1); }
 @Override public RowOrderContract rowOrderContract() { return RowOrderContract.UTF8_BINARY_DESC; }
 @Override public SourceIdentityContract identityContract() { return SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY; }
 @Override public boolean isAuthorized(long userId,long tenantId) { return permissionApi.hasAnyPermissions(userId, "mes:pro-edhr-work-task:query", "mes:pro-edhr-batch-execution:query","mes:pro-process-pool-team-leader:query","mes:pro-edhr-nonconformance-review:query"); }
 @Override public long count(QueryContext query) {
  query.validate();

  Long count=mapper.countEdhr(query);
  if(count==null || count<0)throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
  return count;
 }
 @Override public TaskChunk readChunk(QueryContext query,ChunkCursor after,int limit) {
  query.validate();
  if(limit!=100)throw WorkbenchFailure.INVALID_QUERY.exception();
  return TaskChunk.from(mapper.chunkEdhr(query,after,limit+1).stream().map(this::project).toList(),limit);
 }
 private TaskSummary project(MesEdhrWorkbenchRow row) {
  return row.summary(new ActionEnvelope(descriptor().actionKind(),descriptor().payloadVersion(),navigation(row)));
 }
 private Map<String,String> navigation(MesEdhrWorkbenchRow row) { Map<String,String> out=new java.util.LinkedHashMap<>(); out.put("id",row.getBusinessId()); out.put("taskType",required(row.getDomainTaskType())); if("批次冻结，暂不可处理".equals(row.getStatusLabel())) out.put("blockedReason","批次已冻结，待不合格/偏差处理解除后才能继续此任务"); put(out,"actionUrl",row.getActionUrl()); put(out,"batchExecutionId",row.getBatchExecutionId()); put(out,"batchTaskId",row.getBatchTaskId()); put(out,"executionId",row.getExecutionId()); put(out,"businessScopeType",row.getBusinessScopeType()); put(out,"businessScopeId",row.getBusinessScopeId()); return Map.copyOf(out); }
 @Override public TaskSummary resolveVisibilityIdentity(QueryContext query,String businessId) {
  query.validate();
  if(businessId!=null && businessId.matches("handoff:[1-9][0-9]*")) {
   long id; try { id=Long.parseLong(businessId.substring(8)); } catch(NumberFormatException invalid) { throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception(invalid); }
   var rows=mapper.identityHandoff(query,id); if(rows.size()!=1)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception(); return project(rows.get(0));
  }
  if(businessId==null || !businessId.matches("[1-9][0-9]*"))throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
  long id; try { id=Long.parseLong(businessId); } catch (NumberFormatException failure) { throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception(failure); }
  Long lockedBatchStatus=mapper.lockEdhrIdentity(query,id);

  java.util.List<MesEdhrWorkbenchRow> rows=mapper.identityEdhr(query,businessId,lockedBatchStatus);
  if(rows.size()!=1)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
  return project(rows.get(0));
 }
 private static String required(String value) { if(value==null || value.isBlank())throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception(); return value; }
 private static void put(Map<String,String> out,String key,String value) { if(value!=null)out.put(key,value); }
}
