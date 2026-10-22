package cn.iocoder.yudao.module.dcc.dal.mysql.file;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.dcc.workbench.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface DccWorkbenchTodoMapper {
 @Options(timeout=10) Long countDistribution(@Param("q") QueryContext query);
 @Options(timeout=10) List<DccDistributionWorkbenchRow> chunkDistribution(@Param("q") QueryContext query,@Param("after") ChunkCursor after,@Param("fetchLimit") int fetchLimit);
 @Options(timeout=10) List<DccDistributionWorkbenchRow> identityDistribution(@Param("q") QueryContext query,@Param("businessId") String businessId);
 @Options(timeout=10) Long countTraining(@Param("q") QueryContext query);
 @Options(timeout=10) List<DccTrainingWorkbenchRow> chunkTraining(@Param("q") QueryContext query,@Param("after") ChunkCursor after,@Param("fetchLimit") int fetchLimit);
 @Options(timeout=10) List<DccTrainingWorkbenchRow> identityTraining(@Param("q") QueryContext query,@Param("businessId") String businessId);

 @Options(timeout=10)
 @Select("SELECT distribution_id FROM dcc_controlled_file_distribution_recipient WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0")
 Long distributionParent(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT controlled_file_id FROM dcc_controlled_file_distribution WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0")
 Long distributionFile(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT controlled_file_id FROM dcc_controlled_file_training_progress WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0")
 Long trainingFile(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT id FROM dcc_controlled_file WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
 Long lockFile(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT controlled_file_id FROM dcc_controlled_file_distribution WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
 Long lockDistribution(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT distribution_id FROM dcc_controlled_file_distribution_recipient WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
 Long lockRecipient(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT id FROM dcc_controlled_file WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE NOWAIT")
 Long lockTrainingFile(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT controlled_file_id FROM dcc_controlled_file_training_progress WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE NOWAIT")
 Long lockTrainingProgress(@Param("q") QueryContext q,@Param("id") long id);
 default void lockDistributionIdentity(QueryContext q,long id) {
  q.validate();
  Long d=distributionParent(q,id); if(d==null)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
  Long f=distributionFile(q,d); if(f==null || lockFile(q,f)==null)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
  if(!d.equals(lockRecipient(q,id)) || !f.equals(lockDistribution(q,d)))throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
 }
 default void lockTrainingIdentity(QueryContext q,long id) {
  q.validate();
  Long f=trainingFile(q,id); if(f==null || lockTrainingFile(q,f)==null || !f.equals(lockTrainingProgress(q,id)))throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
 }
}
