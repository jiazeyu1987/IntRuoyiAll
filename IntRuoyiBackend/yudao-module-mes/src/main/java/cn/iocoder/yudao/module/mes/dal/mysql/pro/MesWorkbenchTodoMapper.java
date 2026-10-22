package cn.iocoder.yudao.module.mes.dal.mysql.pro;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.mes.workbench.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface MesWorkbenchTodoMapper {
 @Options(timeout=10) Long countEdhr(@Param("q") QueryContext query);
 @Options(timeout=10) List<MesEdhrWorkbenchRow> chunkEdhr(@Param("q") QueryContext query,@Param("after") ChunkCursor after,@Param("fetchLimit") int fetchLimit);
 @Options(timeout=10) List<MesEdhrWorkbenchRow> identityEdhr(@Param("q") QueryContext query,@Param("businessId") String businessId,@Param("lockedBatchStatus") Long lockedBatchStatus);
 @Options(timeout=10) Long countWorkOrder(@Param("q") QueryContext query);
 @Options(timeout=10) List<MesWorkOrderWorkbenchRow> chunkWorkOrder(@Param("q") QueryContext query,@Param("after") ChunkCursor after,@Param("fetchLimit") int fetchLimit);
 @Options(timeout=10) List<MesWorkOrderWorkbenchRow> identityWorkOrder(@Param("q") QueryContext query,@Param("businessId") String businessId);

 @Options(timeout=10)
 @Select("SELECT batch_execution_id FROM mes_pro_edhr_work_task WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0")
 Long edhrBatch(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT status FROM mes_pro_edhr_batch_execution WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
 Long lockBatch(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT id FROM mes_pro_edhr_work_task WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
 Long lockEdhr(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT batch_execution_id FROM mes_pro_edhr_work_task WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
 Long lockedEdhrBatch(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT id FROM mes_pro_work_order WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
 Long lockWorkOrder(@Param("q") QueryContext q,@Param("id") long id);
 default Long lockEdhrIdentity(QueryContext q,long id) {
  q.validate();
  Long batch=edhrBatch(q,id); Long status=batch==null?null:lockBatch(q,batch);
  if(lockEdhr(q,id)==null || !java.util.Objects.equals(batch,lockedEdhrBatch(q,id)))throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
  return status;
 }
 default void lockWorkOrderIdentity(QueryContext q,long id) { q.validate(); if(lockWorkOrder(q,id)==null)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception(); }
}
