package cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MesReleaseTaskNotifyDeliveryMapper extends BaseMapperX<MesReleaseTaskNotifyDeliveryDO> {
    @Select("SELECT * FROM mes_pro_edhr_release_task_notify_delivery WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=FALSE")
    MesReleaseTaskNotifyDeliveryDO selectDelivery(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Select("SELECT * FROM mes_pro_edhr_release_task_notify_delivery WHERE tenant_id=#{tenantId} AND work_task_id=#{taskId} AND deleted=FALSE ORDER BY id")
    List<MesReleaseTaskNotifyDeliveryDO> selectTaskDeliveries(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId);

    @Update("""
            UPDATE mes_pro_edhr_release_task_notify_delivery
               SET attempt_count=attempt_count+1, row_version=row_version+1, last_attempt_at=#{at},
                   update_time=#{at}
             WHERE tenant_id=#{tenantId} AND id=#{id} AND row_version=#{version}
               AND status IN ('PENDING','FAILED') AND deleted=FALSE
            """)
    int beginAttempt(@Param("tenantId") Long tenantId, @Param("id") Long id,
                     @Param("version") Integer version, @Param("at") LocalDateTime at);

    @Update("""
            UPDATE mes_pro_edhr_release_task_notify_delivery
               SET status='SENT', system_message_id=#{messageId}, sent_at=#{at},
                   last_error_summary=NULL, row_version=row_version+1, update_time=#{at}
             WHERE tenant_id=#{tenantId} AND id=#{id} AND row_version=#{version}
               AND attempt_count>0 AND status IN ('PENDING','FAILED') AND deleted=FALSE
            """)
    int markSent(@Param("tenantId") Long tenantId, @Param("id") Long id,
                 @Param("version") Integer version, @Param("messageId") Long messageId,
                 @Param("at") LocalDateTime at);

    @Update("""
            UPDATE mes_pro_edhr_release_task_notify_delivery
               SET status='FAILED', last_error_summary=#{error}, row_version=row_version+1, update_time=#{at}
             WHERE tenant_id=#{tenantId} AND id=#{id} AND row_version=#{version}
               AND attempt_count>0 AND status IN ('PENDING','FAILED') AND deleted=FALSE
            """)
    int markFailed(@Param("tenantId") Long tenantId, @Param("id") Long id,
                   @Param("version") Integer version, @Param("error") String error,
                   @Param("at") LocalDateTime at);
}
