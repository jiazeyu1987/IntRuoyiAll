package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DccControlledFileMasterMapper extends BaseMapperX<DccControlledFileMasterDO> {

    default DccControlledFileMasterDO selectByCategoryIdAndDirectoryIdAndFileName(
            Long categoryId, Long directoryId, String fileName) {
        return selectOne(DccControlledFileMasterDO::getCategoryId, categoryId,
                DccControlledFileMasterDO::getDirectoryId, directoryId,
                DccControlledFileMasterDO::getFileName, fileName);
    }

    default List<DccControlledFileMasterDO> selectListByFileNumber(String fileNumber) {
        return selectList(DccControlledFileMasterDO::getFileNumber, fileNumber);
    }
    default List<DccControlledFileMasterDO> selectListByLogicalIdentity(Long projectCodeId, Long taxonomyLeafId,
                                                                          String normalizedFileNumber) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DccControlledFileMasterDO>()
                .eq(DccControlledFileMasterDO::getDccProjectCodeId, projectCodeId)
                .eq(DccControlledFileMasterDO::getFileTypeTaxonomyLeafId, taxonomyLeafId)
                .eq(DccControlledFileMasterDO::getNormalizedFileNumber, normalizedFileNumber));
    }

    @Select("""
            SELECT id,
                   category_id,
                   directory_id,
                   file_name,
                   file_number,
                   tenant_id,
                   dcc_project_code_id,
                   file_type_taxonomy_leaf_id,
                   normalized_file_number,
                   current_active_controlled_file_id,
                   status,
                   create_time,
                   update_time,
                   creator,
                   updater,
                   deleted
            FROM dcc_controlled_file_master
            WHERE tenant_id = #{tenantId}
              AND dcc_project_code_id = #{dccProjectCodeId}
              AND file_type_taxonomy_leaf_id = #{fileTypeTaxonomyLeafId}
              AND normalized_file_number = #{normalizedFileNumber}
              AND deleted = b'0'
            LIMIT 1
            """)
    DccControlledFileMasterDO selectByNewLogicalIdentity(@Param("tenantId") Long tenantId,
                                                         @Param("dccProjectCodeId") Long dccProjectCodeId,
                                                         @Param("fileTypeTaxonomyLeafId") Long fileTypeTaxonomyLeafId,
                                                         @Param("normalizedFileNumber") String normalizedFileNumber);

    @Select("""
            SELECT id,
                   category_id,
                   directory_id,
                   file_name,
                   file_number,
                   tenant_id,
                   dcc_project_code_id,
                   file_type_taxonomy_leaf_id,
                   normalized_file_number,
                   current_active_controlled_file_id,
                   status,
                   create_time,
                   update_time,
                   creator,
                   updater,
                   deleted
            FROM dcc_controlled_file_master
            WHERE id = #{id}
              AND deleted = 0
            FOR UPDATE
            """)
    DccControlledFileMasterDO selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT id,
                   category_id,
                   directory_id,
                   file_name,
                   file_number,
                   current_active_controlled_file_id,
                   status,
                   create_time,
                   update_time,
                   creator,
                   updater,
                   deleted
            FROM dcc_controlled_file_master
            WHERE category_id = #{categoryId}
              AND directory_id = #{directoryId}
              AND file_name = #{fileName}
              AND deleted = b'1'
            LIMIT 1
            """)
    DccControlledFileMasterDO selectDeletedByCategoryIdAndDirectoryIdAndFileName(
            @Param("categoryId") Long categoryId,
            @Param("directoryId") Long directoryId,
            @Param("fileName") String fileName);

    @Update("""
            UPDATE dcc_controlled_file_master
            SET deleted = b'0',
                directory_id = #{directoryId},
                file_number = #{fileNumber},
                current_active_controlled_file_id = NULL,
                status = #{status},
                update_time = NOW()
            WHERE id = #{id}
              AND category_id = #{categoryId}
              AND directory_id = #{directoryId}
              AND file_name = #{fileName}
              AND deleted = b'1'
            """)
    int restoreDeletedNasMaster(@Param("id") Long id,
                                @Param("categoryId") Long categoryId,
                                @Param("directoryId") Long directoryId,
                                @Param("fileName") String fileName,
                                @Param("fileNumber") String fileNumber,
                                @Param("status") String status);
}
