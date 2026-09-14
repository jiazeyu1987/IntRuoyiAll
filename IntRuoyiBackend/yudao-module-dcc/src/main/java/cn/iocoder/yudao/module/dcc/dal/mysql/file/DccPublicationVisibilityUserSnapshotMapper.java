package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationVisibilityUserSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccPublicationVisibilityUserSnapshotMapper
        extends BaseMapperX<DccPublicationVisibilityUserSnapshotDO> {
    @Select("<script>SELECT * FROM dcc_publication_visibility_user_snapshot WHERE tenant_id=#{tenantId} AND rule_snapshot_id IN <foreach collection='ruleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND deleted=0 ORDER BY id</script>")
    List<DccPublicationVisibilityUserSnapshotDO> selectListByRuleIds(@Param("tenantId") Long tenantId,
                                                                     @Param("ruleIds") List<Long> ruleIds);
}
