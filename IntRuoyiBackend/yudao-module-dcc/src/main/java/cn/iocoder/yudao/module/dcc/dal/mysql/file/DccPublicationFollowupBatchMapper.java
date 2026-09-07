package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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

    @Select("SELECT * FROM dcc_publication_followup_batch WHERE tenant_id=#{tenantId} AND published_controlled_file_id=#{controlledFileId} AND deleted=0 ORDER BY id DESC LIMIT 1")
    DccPublicationFollowupBatchDO selectLatestByPublishedControlledFileId(
            @Param("tenantId") Long tenantId, @Param("controlledFileId") Long controlledFileId);

    @Update("UPDATE dcc_publication_followup_batch SET status=#{status}, update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND id=#{batchId} AND deleted=0")
    int updateStatus(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId,
                     @Param("status") String status);

    @Select("SELECT * FROM dcc_publication_followup_batch WHERE tenant_id=#{tenantId} AND id=#{batchId} AND deleted=0 LIMIT 1 FOR UPDATE")
    DccPublicationFollowupBatchDO selectByIdAndTenantForUpdate(@Param("tenantId") Long tenantId,
                                                                @Param("batchId") Long batchId);

    @Select("""
            <script>
            SELECT b.* FROM dcc_publication_followup_batch b
            WHERE b.tenant_id=#{tenantId} AND b.deleted=0
            <if test='fileNumber != null and fileNumber != ""'>
              AND b.file_number_snapshot LIKE CONCAT('%', #{fileNumber}, '%')
            </if>
            <if test='versionNo != null and versionNo != ""'>AND b.version_no_snapshot=#{versionNo}</if>
            <if test='batchStatus != null and batchStatus != ""'>AND b.status=#{batchStatus}</if>
            <if test='taskStatus != null and taskStatus != ""'>
              AND EXISTS (SELECT 1 FROM dcc_publication_impact_task t WHERE t.tenant_id=b.tenant_id
                AND t.batch_id=b.id AND t.deleted=0 AND t.task_status=#{taskStatus})
            </if>
            <if test='notificationStatus != null and notificationStatus != ""'>
              AND EXISTS (SELECT 1 FROM dcc_publication_notification_delivery d WHERE d.tenant_id=b.tenant_id
                AND d.batch_id=b.id AND d.deleted=0 AND d.status=#{notificationStatus})
            </if>
            <if test='assigneeUserId != null and assigneeUserId != ""'>
              AND EXISTS (SELECT 1 FROM dcc_publication_impact_task a WHERE a.tenant_id=b.tenant_id
                AND a.batch_id=b.id AND a.deleted=0 AND a.assignee_user_id=#{assigneeUserId})
            </if>
            ORDER BY b.id DESC
            </script>
            """)
    Page<DccPublicationFollowupBatchDO> selectManagementPage(
            Page<DccPublicationFollowupBatchDO> page, @Param("tenantId") Long tenantId,
            @Param("fileNumber") String fileNumber, @Param("versionNo") String versionNo,
            @Param("batchStatus") String batchStatus, @Param("taskStatus") String taskStatus,
            @Param("notificationStatus") String notificationStatus,
            @Param("assigneeUserId") Long assigneeUserId);
}
