package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DccControlledFileSourceOwnershipMapper extends BaseMapperX<DccControlledFileSourceOwnershipDO> {

    @Select("""
            SELECT *
            FROM dcc_controlled_file_source_ownership
            WHERE tenant_id = #{tenantId}
              AND controlled_file_id = #{controlledFileId}
              AND deleted = 0
            LIMIT 1
            """)
    DccControlledFileSourceOwnershipDO selectByControlledFileId(@Param("tenantId") Long tenantId,
                                                                 @Param("controlledFileId") Long controlledFileId);

    @Select("""
            SELECT *
            FROM dcc_controlled_file_source_ownership
            WHERE tenant_id = #{tenantId}
              AND source_file_id = #{sourceFileId}
              AND deleted = 0
            LIMIT 1
            """)
    DccControlledFileSourceOwnershipDO selectBySourceFileId(@Param("tenantId") Long tenantId,
                                                             @Param("sourceFileId") Long sourceFileId);

}
