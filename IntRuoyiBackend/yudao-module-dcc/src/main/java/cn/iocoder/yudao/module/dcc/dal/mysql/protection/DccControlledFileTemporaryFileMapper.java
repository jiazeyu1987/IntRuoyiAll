package cn.iocoder.yudao.module.dcc.dal.mysql.protection;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * DCC controlled file temporary upload file mapper.
 */
@Mapper
public interface DccControlledFileTemporaryFileMapper extends BaseMapperX<DccControlledFileTemporaryFileDO> {

    @Select("""
            SELECT COUNT(1)
              FROM (
                    SELECT id
                      FROM dcc_controlled_file
                     WHERE deleted = 0
                       AND tenant_id = #{tenantId}
                       AND (source_file_id = #{storageFileId}
                        OR original_file_id = #{storageFileId}
                        OR drawing_pdf_file_id = #{storageFileId}
                        OR training_record_file_id = #{storageFileId}
                        OR published_file_id = #{storageFileId}
                        OR stamped_file_id = #{storageFileId})
                    UNION ALL
                    SELECT id
                      FROM dcc_external_file_review
                     WHERE deleted = 0
                       AND tenant_id = #{tenantId}
                       AND output_file_id = #{storageFileId}
                    UNION ALL
                    SELECT id
                      FROM dcc_controlled_file_signature
                     WHERE deleted = 0
                       AND tenant_id = #{tenantId}
                       AND (source_file_id = #{storageFileId}
                        OR controlled_copy_file_id = #{storageFileId}
                        OR signature_image_file_id = #{storageFileId})
                    UNION ALL
                    SELECT id
                      FROM dcc_controlled_file_stamp
                     WHERE deleted = 0
                       AND tenant_id = #{tenantId}
                       AND (source_file_id = #{storageFileId}
                        OR output_file_id = #{storageFileId})
                   ) referenced_file
            """)
    long countActiveDccStorageReferencesByStorageFileId(@Param("tenantId") Long tenantId,
                                                        @Param("storageFileId") Long storageFileId);
}
