package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationRelationDirectionSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccPublicationRelationDirectionSnapshotMapper
        extends BaseMapperX<DccPublicationRelationDirectionSnapshotDO> {
    @Select("<script>SELECT * FROM dcc_publication_relation_direction_snapshot WHERE tenant_id=#{tenantId} AND relation_snapshot_id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND deleted=0 ORDER BY id</script>")
    List<DccPublicationRelationDirectionSnapshotDO> selectListByRelationSnapshotIds(
            @Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
}
