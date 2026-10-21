package cn.iocoder.yudao.module.system.service.profileworkbench;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.*;
import cn.iocoder.yudao.module.system.dal.dataobject.profileworkbench.ProfileWorkbenchTaskVisibilityDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.sql.SQLException;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;
@Service @DS("master")
public class WorkbenchVisibilityWriteTransaction {
 public record Result(boolean changed,String visibilityVersion) { }
 @Resource private WorkbenchVisibilityVersionMapper versions;
 @Resource private ProfileWorkbenchTaskVisibilityMapper hidden;
 @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
 public Result write(long tenant,long user,long expected,String taskKey,boolean hide,Supplier<TaskSummary> resolver) {
  return writeLocked(tenant,user,expected,taskKey,hide,resolver);
 }
 /** S4 caller already owns global -> tenant locks; never suspend its fenced transaction. */
 @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
 public Result writeJoined(long tenant,long user,long expected,String taskKey,boolean hide,Supplier<TaskSummary> resolver) {
  return writeLocked(tenant,user,expected,taskKey,hide,resolver);
 }
 private Result writeLocked(long tenant,long user,long expected,String taskKey,boolean hide,Supplier<TaskSummary> resolver) {
  Long actual=versions.exclusive(tenant,user);
  if(actual==null || actual<1)throw VISIBILITY_CONTEXT_MISSING.exception();
  if(actual!=expected)throw VISIBILITY_VERSION_CONFLICT.exception();
  TaskSummary row;
  try { row=resolver.get(); }
  catch (RuntimeException failure) {
   for (Throwable cause=failure;cause!=null;cause=cause.getCause()) {
    if(cause instanceof SQLException sql && sql.getErrorCode()==3572 && "HY000".equals(sql.getSQLState()))throw VISIBILITY_WRITE_CONFLICT.exception(failure);
   }
   throw failure;
  }
  if(row==null || !taskKey.equals(row.getTaskKey()))throw TASK_NOT_ELIGIBLE.exception();
  ProfileWorkbenchTaskVisibilityDO existing=hidden.lockPersonal(tenant,user,taskKey);
  boolean changed=hide ? existing==null : existing!=null;
  if(changed) {
   if(hide) {
    ProfileWorkbenchTaskVisibilityDO create=new ProfileWorkbenchTaskVisibilityDO().setUserId(user).setTaskKey(taskKey)
     .setTaskType(row.getTaskType()).setSource(row.getSource()).setBusinessId(row.getBusinessId()).setDetail(row.getDetail()).setHiddenAt(LocalDateTime.now());
    create.setTenantId(tenant);
    if(hidden.insert(create)!=1)throw VISIBILITY_WRITE_FAILED.exception();
   } else if(hidden.deletePersonal(tenant,user,taskKey)!=1)throw VISIBILITY_WRITE_FAILED.exception();
   long next=Math.addExact(actual,1);
   if(versions.advance(tenant,user,actual,next)!=1)throw VISIBILITY_WRITE_FAILED.exception();
   actual=next;
  }
  return new Result(changed,Long.toString(actual));
 }
}
