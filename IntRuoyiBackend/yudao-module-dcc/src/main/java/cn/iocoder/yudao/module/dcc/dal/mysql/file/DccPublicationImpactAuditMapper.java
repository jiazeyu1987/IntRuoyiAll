package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactAuditDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccPublicationImpactAuditMapper extends BaseMapperX<DccPublicationImpactAuditDO> {
    @Select("<script>SELECT * FROM dcc_publication_impact_audit WHERE tenant_id=#{tenantId} AND batch_id IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND deleted=0 ORDER BY occurred_at,id</script>")
    List<DccPublicationImpactAuditDO> selectListByBatchIds(
            @Param("tenantId") Long tenantId, @Param("batchIds") List<Long> batchIds);
}
