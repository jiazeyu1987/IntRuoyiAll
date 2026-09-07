package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccPublicationNotificationCandidateMapper
        extends BaseMapperX<DccPublicationNotificationCandidateDO> {

    @Select("SELECT * FROM dcc_publication_notification_candidate WHERE tenant_id=#{tenantId} AND batch_id=#{batchId} AND deleted=0 ORDER BY id")
    List<DccPublicationNotificationCandidateDO> selectListByBatchId(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId);

    @Select("<script>SELECT * FROM dcc_publication_notification_candidate WHERE tenant_id=#{tenantId} AND id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND deleted=0 ORDER BY id</script>")
    List<DccPublicationNotificationCandidateDO> selectListByIds(@Param("tenantId") Long tenantId,
                                                                 @Param("ids") List<Long> ids);
}
