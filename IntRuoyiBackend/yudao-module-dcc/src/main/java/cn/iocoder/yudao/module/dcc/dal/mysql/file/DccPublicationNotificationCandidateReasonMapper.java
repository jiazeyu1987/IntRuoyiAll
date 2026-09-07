package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateReasonDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccPublicationNotificationCandidateReasonMapper
        extends BaseMapperX<DccPublicationNotificationCandidateReasonDO> {

    @Select("SELECT * FROM dcc_publication_notification_candidate_reason WHERE tenant_id=#{tenantId} AND candidate_id=#{candidateId} AND deleted=0 ORDER BY id")
    List<DccPublicationNotificationCandidateReasonDO> selectListByCandidateId(@Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId);

    @Select("<script>SELECT * FROM dcc_publication_notification_candidate_reason WHERE tenant_id=#{tenantId} AND candidate_id IN <foreach collection='candidateIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND deleted=0 ORDER BY id</script>")
    List<DccPublicationNotificationCandidateReasonDO> selectListByCandidateIds(@Param("tenantId") Long tenantId, @Param("candidateIds") List<Long> candidateIds);
}
