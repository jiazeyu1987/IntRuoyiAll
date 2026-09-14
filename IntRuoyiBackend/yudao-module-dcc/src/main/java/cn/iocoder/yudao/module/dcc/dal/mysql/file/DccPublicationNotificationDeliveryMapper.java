package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DccPublicationNotificationDeliveryMapper extends BaseMapperX<DccPublicationNotificationDeliveryDO> {
    @Insert("""
            INSERT INTO dcc_publication_notification_delivery
              (batch_id, candidate_id, user_id, business_key, template_code, status, attempt_count,
               row_version, creation_token, tenant_id, deleted)
            VALUES (#{record.batchId}, #{record.candidateId}, #{record.userId}, #{record.businessKey},
                    #{record.templateCode}, #{record.status}, 0, 0, #{record.creationToken}, #{record.tenantId}, 0)
            ON DUPLICATE KEY UPDATE id = id
            """)
    int insertOrKeepExisting(@Param("record") DccPublicationNotificationDeliveryDO record);

    @Select("SELECT * FROM dcc_publication_notification_delivery WHERE tenant_id=#{tenantId} AND candidate_id=#{candidateId} AND deleted=0 LIMIT 1")
    DccPublicationNotificationDeliveryDO selectByCandidateId(@Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId);

    @Select("SELECT * FROM dcc_publication_notification_delivery WHERE tenant_id=#{tenantId} AND business_key=#{businessKey} AND deleted=0 LIMIT 1")
    DccPublicationNotificationDeliveryDO selectByBusinessKey(@Param("tenantId") Long tenantId, @Param("businessKey") String businessKey);

    @Select("SELECT * FROM dcc_publication_notification_delivery WHERE tenant_id=#{tenantId} AND batch_id=#{batchId} AND deleted=0 ORDER BY id")
    List<DccPublicationNotificationDeliveryDO> selectListByBatchId(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId);

    @Select("<script>SELECT * FROM dcc_publication_notification_delivery WHERE tenant_id=#{tenantId} AND batch_id IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND deleted=0 ORDER BY id</script>")
    List<DccPublicationNotificationDeliveryDO> selectListByBatchIds(@Param("tenantId") Long tenantId,
                                                                     @Param("batchIds") List<Long> batchIds);

    @Select("SELECT * FROM dcc_publication_notification_delivery WHERE tenant_id=#{tenantId} AND batch_id=#{batchId} AND deleted=0 ORDER BY id FOR UPDATE")
    List<DccPublicationNotificationDeliveryDO> selectListByBatchIdForUpdate(
            @Param("tenantId") Long tenantId, @Param("batchId") Long batchId);

    @Select("SELECT * FROM dcc_publication_notification_delivery WHERE tenant_id=#{tenantId} AND batch_id=#{batchId} AND status IN ('PENDING','FAILED') AND deleted=0 ORDER BY id")
    List<DccPublicationNotificationDeliveryDO> selectDispatchableByBatchId(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId);

    @Update("""
            UPDATE dcc_publication_notification_delivery
            SET attempt_count=attempt_count+1, last_attempt_at=#{at}, row_version=row_version+1,
                update_time=#{at}, updater=#{actorId}
            WHERE tenant_id=#{tenantId} AND id=#{deliveryId} AND row_version=#{expectedVersion}
              AND status IN ('PENDING','FAILED') AND deleted=0
            """)
    int beginAttempt(@Param("tenantId") Long tenantId, @Param("deliveryId") Long deliveryId,
                     @Param("expectedVersion") Integer expectedVersion, @Param("at") java.time.LocalDateTime at,
                     @Param("actorId") Long actorId);

    @Update("UPDATE dcc_publication_notification_delivery SET status='SENT', sent_at=#{at}, system_message_id=#{messageId}, last_error_summary=NULL, row_version=row_version+1, update_time=#{at}, updater=#{actorId} WHERE tenant_id=#{tenantId} AND id=#{deliveryId} AND row_version=#{expectedVersion} AND status IN ('PENDING','FAILED') AND deleted=0")
    int markSent(@Param("tenantId") Long tenantId, @Param("deliveryId") Long deliveryId, @Param("expectedVersion") Integer expectedVersion, @Param("messageId") Long messageId, @Param("at") java.time.LocalDateTime at, @Param("actorId") Long actorId);

    @Update("UPDATE dcc_publication_notification_delivery SET status='FAILED', last_error_summary=#{errorSummary}, sent_at=NULL, row_version=row_version+1, update_time=#{at}, updater=#{actorId} WHERE tenant_id=#{tenantId} AND id=#{deliveryId} AND row_version=#{expectedVersion} AND status IN ('PENDING','FAILED') AND deleted=0")
    int markFailed(@Param("tenantId") Long tenantId, @Param("deliveryId") Long deliveryId, @Param("expectedVersion") Integer expectedVersion, @Param("errorSummary") String errorSummary, @Param("at") java.time.LocalDateTime at, @Param("actorId") Long actorId);
}
