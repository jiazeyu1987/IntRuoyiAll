package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DccPublicationFollowupBatchMapper extends BaseMapperX<DccPublicationFollowupBatchDO> {

    @Insert("""
            INSERT INTO dcc_publication_followup_batch (
                published_controlled_file_id, published_master_id, previous_active_controlled_file_id,
                dcc_project_code_id, category_id, directory_id, file_type_taxonomy_leaf_id,
                file_number_snapshot, file_name_snapshot, version_no_snapshot, status, published_at,
                creation_token, tenant_id, deleted
            ) VALUES (
                #{record.publishedControlledFileId}, #{record.publishedMasterId},
                #{record.previousActiveControlledFileId}, #{record.dccProjectCodeId}, #{record.categoryId},
                #{record.directoryId}, #{record.fileTypeTaxonomyLeafId}, #{record.fileNumberSnapshot},
                #{record.fileNameSnapshot}, #{record.versionNoSnapshot}, #{record.status}, #{record.publishedAt},
                #{record.creationToken}, #{record.tenantId}, 0
            )
            ON DUPLICATE KEY UPDATE id = id
            """)
    int insertOrKeepExisting(@Param("record") DccPublicationFollowupBatchDO record);

    @Select("""
            SELECT id, published_controlled_file_id, published_master_id, previous_active_controlled_file_id,
                   dcc_project_code_id, category_id, directory_id, file_type_taxonomy_leaf_id,
                   file_number_snapshot, file_name_snapshot, version_no_snapshot, status, published_at,
                   creation_token, tenant_id, create_time, update_time, creator, updater, deleted
            FROM dcc_publication_followup_batch
            WHERE tenant_id = #{tenantId}
              AND published_controlled_file_id = #{publishedControlledFileId}
              AND deleted = 0
            LIMIT 1
            """)
    DccPublicationFollowupBatchDO selectByPublishedControlledFileId(
            @Param("tenantId") Long tenantId,
            @Param("publishedControlledFileId") Long publishedControlledFileId);
}
