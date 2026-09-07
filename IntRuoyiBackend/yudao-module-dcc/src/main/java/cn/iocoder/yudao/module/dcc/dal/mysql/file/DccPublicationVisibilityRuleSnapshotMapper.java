package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationVisibilityRuleSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccPublicationVisibilityRuleSnapshotMapper
        extends BaseMapperX<DccPublicationVisibilityRuleSnapshotDO> {
    @Select("SELECT * FROM dcc_publication_visibility_rule_snapshot WHERE tenant_id=#{tenantId} AND batch_id=#{batchId} AND deleted=0 ORDER BY id")
    List<DccPublicationVisibilityRuleSnapshotDO> selectListByBatchId(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId);

    @Select("<script>SELECT * FROM dcc_publication_visibility_rule_snapshot WHERE tenant_id=#{tenantId} AND batch_id IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND deleted=0 ORDER BY id</script>")
    List<DccPublicationVisibilityRuleSnapshotDO> selectListByBatchIds(@Param("tenantId") Long tenantId,
                                                                       @Param("batchIds") List<Long> batchIds);
}
