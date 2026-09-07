package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactTaskDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Mapper
public interface DccPublicationImpactTaskMapper extends BaseMapperX<DccPublicationImpactTaskDO> {

    @Insert("""
            INSERT INTO dcc_publication_impact_task (
                batch_id, publication_relation_snapshot_id, published_controlled_file_id,
                related_master_id, related_active_controlled_file_id, related_file_number_snapshot,
                related_file_name_snapshot, related_version_no_snapshot, assignee_user_id,
                assignee_user_name_snapshot, task_status, revision_tracking_status, row_version,
                creation_token, tenant_id, deleted
            ) VALUES (
                #{record.batchId}, #{record.publicationRelationSnapshotId}, #{record.publishedControlledFileId},
                #{record.relatedMasterId}, #{record.relatedActiveControlledFileId},
                #{record.relatedFileNumberSnapshot}, #{record.relatedFileNameSnapshot},
                #{record.relatedVersionNoSnapshot}, #{record.assigneeUserId},
                #{record.assigneeUserNameSnapshot}, #{record.taskStatus}, #{record.revisionTrackingStatus},
                0, #{record.creationToken}, #{record.tenantId}, 0
            )
            ON DUPLICATE KEY UPDATE id = id
            """)
    int insertOrKeepExisting(@Param("record") DccPublicationImpactTaskDO record);

    @Select("""
            SELECT * FROM dcc_publication_impact_task
            WHERE tenant_id = #{tenantId} AND batch_id = #{batchId}
              AND related_master_id = #{relatedMasterId} AND deleted = 0
            LIMIT 1
            """)
    DccPublicationImpactTaskDO selectByBatchIdAndRelatedMasterId(
            @Param("tenantId") Long tenantId, @Param("batchId") Long batchId,
            @Param("relatedMasterId") Long relatedMasterId);

    @Select("""
            SELECT * FROM dcc_publication_impact_task
            WHERE tenant_id = #{tenantId} AND id = #{taskId} AND deleted = 0
            LIMIT 1
            """)
    DccPublicationImpactTaskDO selectByIdAndTenant(@Param("tenantId") Long tenantId,
                                                   @Param("taskId") Long taskId);

    @Select("""
            SELECT * FROM dcc_publication_impact_task
            WHERE tenant_id = #{tenantId} AND id = #{taskId} AND deleted = 0
            LIMIT 1
            FOR UPDATE
            """)
    DccPublicationImpactTaskDO selectByIdAndTenantForUpdate(@Param("tenantId") Long tenantId,
                                                            @Param("taskId") Long taskId);

    @Select("""
            SELECT * FROM dcc_publication_impact_task
            WHERE tenant_id = #{tenantId}
              AND linked_revision_controlled_file_id = #{revisionId}
              AND revision_tracking_status = 'REVISION_LINKED'
              AND deleted = 0
            ORDER BY id
            """)
    List<DccPublicationImpactTaskDO> selectListByLinkedRevisionId(@Param("tenantId") Long tenantId,
                                                                  @Param("revisionId") Long revisionId);

    @Select("SELECT * FROM dcc_publication_impact_task WHERE tenant_id=#{tenantId} AND batch_id=#{batchId} AND deleted=0 ORDER BY id")
    List<DccPublicationImpactTaskDO> selectListByBatchId(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId);

    @Select("<script>SELECT * FROM dcc_publication_impact_task WHERE tenant_id=#{tenantId} AND batch_id IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND deleted=0 ORDER BY id</script>")
    List<DccPublicationImpactTaskDO> selectListByBatchIds(@Param("tenantId") Long tenantId,
                                                           @Param("batchIds") List<Long> batchIds);

    @Select("SELECT * FROM dcc_publication_impact_task WHERE tenant_id=#{tenantId} AND batch_id=#{batchId} AND deleted=0 ORDER BY id FOR UPDATE")
    List<DccPublicationImpactTaskDO> selectListByBatchIdForUpdate(
            @Param("tenantId") Long tenantId, @Param("batchId") Long batchId);

    @Select("SELECT * FROM dcc_publication_impact_task WHERE tenant_id=#{tenantId} AND assignee_user_id=#{assigneeUserId} AND deleted=0 ORDER BY id")
    List<DccPublicationImpactTaskDO> selectListByAssignee(@Param("tenantId") Long tenantId, @Param("assigneeUserId") Long assigneeUserId);

    @Select("""
            <script>
            SELECT * FROM dcc_publication_impact_task
            WHERE tenant_id=#{tenantId} AND assignee_user_id=#{assigneeUserId} AND deleted=0
            <choose>
              <when test='taskStatus != null and taskStatus != ""'>AND task_status=#{taskStatus}</when>
              <otherwise>
                AND (task_status &lt;&gt; 'COMPLETED'
                  OR revision_tracking_status IN ('NOT_STARTED','REVISION_LINKED'))
              </otherwise>
            </choose>
            <if test='trackingStatus != null and trackingStatus != ""'>
              AND revision_tracking_status=#{trackingStatus}
            </if>
            ORDER BY id DESC
            </script>
            """)
    Page<DccPublicationImpactTaskDO> selectAssigneePage(
            Page<DccPublicationImpactTaskDO> page, @Param("tenantId") Long tenantId,
            @Param("assigneeUserId") Long assigneeUserId, @Param("taskStatus") String taskStatus,
            @Param("trackingStatus") String trackingStatus);

    @Update("""
            UPDATE dcc_publication_impact_task
            SET task_status = 'IN_REVIEW', row_version = row_version + 1, update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{taskId} AND deleted = 0
              AND row_version = #{expectedVersion} AND task_status = 'PENDING'
              AND assignee_user_id = #{actorId}
            """)
    int startTask(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId,
                  @Param("expectedVersion") Integer expectedVersion, @Param("actorId") Long actorId);

    @Update("""
            UPDATE dcc_publication_impact_task
            SET task_status = 'COMPLETED', decision = #{decision}, decision_reason = #{reason},
                decided_by = #{actorId}, decided_at = CURRENT_TIMESTAMP,
                revision_tracking_status = #{trackingStatus}, row_version = row_version + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{taskId} AND deleted = 0
              AND row_version = #{expectedVersion} AND task_status = 'IN_REVIEW'
              AND assignee_user_id = #{actorId}
            """)
    int completeDecision(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId,
                         @Param("expectedVersion") Integer expectedVersion, @Param("actorId") Long actorId,
                         @Param("decision") String decision, @Param("reason") String reason,
                         @Param("trackingStatus") String trackingStatus);

    @Update("""
            UPDATE dcc_publication_impact_task
            SET assignee_user_id = #{newAssigneeId}, assignee_user_name_snapshot = #{newAssigneeName},
                task_status = 'PENDING', row_version = row_version + 1, update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{taskId} AND deleted = 0
              AND row_version = #{expectedVersion}
              AND task_status IN ('PENDING', 'UNASSIGNED', 'IN_REVIEW')
            """)
    int reassignTask(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId,
                     @Param("expectedVersion") Integer expectedVersion,
                     @Param("newAssigneeId") Long newAssigneeId,
                     @Param("newAssigneeName") String newAssigneeName,
                     @Param("reason") String reason);

    @Update("""
            UPDATE dcc_publication_impact_task
            SET task_status = CASE WHEN assignee_user_id IS NULL THEN 'UNASSIGNED' ELSE 'PENDING' END,
                decision = NULL, decision_reason = NULL, decided_by = NULL, decided_at = NULL,
                revision_tracking_status = 'NOT_APPLICABLE', linked_revision_controlled_file_id = NULL,
                linked_revision_version_snapshot = NULL, resolved_at = NULL,
                row_version = row_version + 1, update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{taskId} AND deleted = 0
              AND row_version = #{expectedVersion} AND task_status = 'COMPLETED'
            """)
    int reopenTask(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId,
                   @Param("expectedVersion") Integer expectedVersion, @Param("reason") String reason);

    @Update("""
            UPDATE dcc_publication_impact_task
            SET linked_revision_controlled_file_id = #{revisionId},
                linked_revision_version_snapshot = #{revisionVersion},
                revision_tracking_status = 'REVISION_LINKED', row_version = row_version + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{taskId} AND deleted = 0
              AND row_version = #{expectedVersion} AND task_status = 'COMPLETED'
              AND decision = 'REVISION_REQUIRED' AND revision_tracking_status = 'NOT_STARTED'
              AND linked_revision_controlled_file_id IS NULL
            """)
    int linkRevision(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId,
                     @Param("expectedVersion") Integer expectedVersion,
                     @Param("revisionId") Long revisionId, @Param("revisionVersion") String revisionVersion,
                     @Param("reason") String reason);

    @Update("""
            UPDATE dcc_publication_impact_task
            SET revision_tracking_status = 'RESOLVED', resolved_at = CURRENT_TIMESTAMP,
                row_version = row_version + 1, update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{taskId} AND deleted = 0
              AND row_version = #{expectedVersion} AND task_status = 'COMPLETED'
              AND decision = 'REVISION_REQUIRED' AND revision_tracking_status = 'REVISION_LINKED'
              AND linked_revision_controlled_file_id = #{revisionId}
            """)
    int resolveRevision(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId,
                        @Param("expectedVersion") Integer expectedVersion,
                        @Param("revisionId") Long revisionId);
}
