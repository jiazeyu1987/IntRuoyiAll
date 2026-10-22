package cn.iocoder.yudao.module.showroom.dal.mysql.workflow;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.showroom.workbench.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface ShowroomWorkbenchTodoMapper {
 @Options(timeout=10) Long countAssignment(@Param("q") QueryContext query);
 @Options(timeout=10) List<ShowroomAssignmentWorkbenchRow> chunkAssignment(@Param("q") QueryContext query,@Param("after") ChunkCursor after,@Param("fetchLimit") int fetchLimit);
 @Options(timeout=10) List<ShowroomAssignmentWorkbenchRow> identityAssignment(@Param("q") QueryContext query,@Param("businessId") String businessId);

 @Options(timeout=10) Long countInvalidNotifications(@Param("q") QueryContext q);
 @Options(timeout=10)
 @Select("SELECT notify_message_id FROM showroom_field_assignment WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
 Long lockAssignment(@Param("q") QueryContext q,@Param("id") long id);
 @Options(timeout=10)
 @Select("SELECT id FROM system_notify_message WHERE tenant_id=#{q.tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
 Long lockNotification(@Param("q") QueryContext q,@Param("id") long id);
 default void lockAssignmentIdentity(QueryContext q,long id) {
  q.validate(); Long n=lockAssignment(q,id); if(n==null || lockNotification(q,n)==null)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
 }
}
