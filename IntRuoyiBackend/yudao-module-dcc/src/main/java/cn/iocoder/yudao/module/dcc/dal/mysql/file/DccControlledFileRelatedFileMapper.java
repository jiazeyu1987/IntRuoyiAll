package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccControlledFileRelatedFileMapper extends BaseMapperX<DccControlledFileRelatedFileDO> {

    default List<DccControlledFileRelatedFileDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileRelatedFileDO>()
                .eq(DccControlledFileRelatedFileDO::getControlledFileId, controlledFileId)
                .orderByAsc(DccControlledFileRelatedFileDO::getId));
    }

    @Select("""
            SELECT relation.*
            FROM dcc_controlled_file_related_file relation
            INNER JOIN dcc_controlled_file source_file
                    ON source_file.tenant_id = relation.tenant_id
                   AND source_file.id = relation.controlled_file_id
                   AND source_file.deleted = 0
            INNER JOIN dcc_controlled_file_master source_master
                    ON source_master.tenant_id = relation.tenant_id
                   AND source_master.id = source_file.master_id
                   AND source_master.current_active_controlled_file_id = source_file.id
                   AND source_master.deleted = 0
            WHERE relation.tenant_id = #{tenantId}
              AND relation.related_master_id = #{relatedMasterId}
              AND relation.deleted = 0
              AND source_master.id <> #{relatedMasterId}
            ORDER BY relation.id
            """)
    List<DccControlledFileRelatedFileDO> selectReverseCurrentActiveRelations(
            @Param("tenantId") Long tenantId,
            @Param("relatedMasterId") Long relatedMasterId);

}
