package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceMigrationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DccControlledFileSourceMigrationMapper extends BaseMapperX<DccControlledFileSourceMigrationDO> {

    @Select("""
            SELECT *
            FROM dcc_controlled_file_source_migration
            WHERE tenant_id = #{tenantId}
              AND controlled_file_id = #{controlledFileId}
              AND deleted = 0
            LIMIT 1
            """)
    DccControlledFileSourceMigrationDO selectByControlledFileId(@Param("tenantId") Long tenantId,
                                                                 @Param("controlledFileId") Long controlledFileId);

    @Select("""
            SELECT COUNT(1)
            FROM dcc_controlled_file_source_migration
            WHERE tenant_id = #{tenantId}
              AND migration_status = #{status}
              AND deleted = 0
            """)
    long countByStatus(@Param("tenantId") Long tenantId, @Param("status") String status);

}
