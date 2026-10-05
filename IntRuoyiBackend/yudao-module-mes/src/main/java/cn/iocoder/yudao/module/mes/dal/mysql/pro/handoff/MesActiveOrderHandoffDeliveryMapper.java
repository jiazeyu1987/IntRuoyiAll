package cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffDeliveryDO;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MesActiveOrderHandoffDeliveryMapper extends BaseMapperX<MesActiveOrderHandoffDeliveryDO> {
    @Select("SELECT * FROM mes_active_order_handoff_delivery WHERE tenant_id=#{tenant} AND handoff_task_id=#{task} AND deleted=FALSE ORDER BY id")
    List<MesActiveOrderHandoffDeliveryDO> forTask(@Param("tenant")Long tenant,@Param("task")Long task);
    @Select("SELECT * FROM mes_active_order_handoff_delivery WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=FALSE")
    MesActiveOrderHandoffDeliveryDO exact(@Param("tenant")Long tenant,@Param("id")Long id);
    @Update("UPDATE mes_active_order_handoff_delivery SET attempt_count=attempt_count+1,row_version=row_version+1,last_attempt_at=#{at},update_time=#{at} WHERE tenant_id=#{tenant} AND id=#{id} AND row_version=#{version} AND status IN ('PENDING','FAILED') AND deleted=FALSE")
    int attempt(@Param("tenant")Long tenant,@Param("id")Long id,@Param("version")Integer version,@Param("at")LocalDateTime at);
    @Update("UPDATE mes_active_order_handoff_delivery SET status='SENT',system_message_id=#{message},sent_at=#{at},last_error_summary=NULL,row_version=row_version+1,update_time=#{at} WHERE tenant_id=#{tenant} AND id=#{id} AND row_version=#{version} AND attempt_count>0 AND status IN ('PENDING','FAILED') AND deleted=FALSE")
    int sent(@Param("tenant")Long tenant,@Param("id")Long id,@Param("version")Integer version,@Param("message")Long message,@Param("at")LocalDateTime at);
    @Update("UPDATE mes_active_order_handoff_delivery SET status='FAILED',last_error_summary=#{error},row_version=row_version+1,update_time=#{at} WHERE tenant_id=#{tenant} AND id=#{id} AND row_version=#{version} AND attempt_count>0 AND status IN ('PENDING','FAILED') AND deleted=FALSE")
    int failed(@Param("tenant")Long tenant,@Param("id")Long id,@Param("version")Integer version,@Param("error")String error,@Param("at")LocalDateTime at);
}
