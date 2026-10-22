package cn.iocoder.yudao.module.bpm.dal.mysql.approval;

import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewPrepareDO;
import org.apache.ibatis.annotations.*;

/** No update/delete contract. Database grants must likewise restrict this table to INSERT/SELECT. */
@Mapper
public interface BpmApprovalReviewPrepareMapper {
    @Insert("INSERT INTO bpm_approval_review_prepare (tenant_id, actor_id, prepare_id, owner_code, source_task_type, source_task_id, process_instance_id, subject_type, subject_id, content_policy_version, subject_version, preview_content_hash, scope_code, canonical_business_content_json, prepare_record_hash, prepared_at, expires_at) VALUES (#{tenantId}, #{actorId}, #{prepareId}, #{ownerCode}, #{sourceTaskType}, #{sourceTaskId}, #{processInstanceId}, #{subjectType}, #{subjectId}, #{contentPolicyVersion}, #{subjectVersion}, #{previewContentHash}, #{scopeCode}, #{canonicalBusinessContentJson}, #{prepareRecordHash}, #{preparedAt}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id") int insert(BpmApprovalReviewPrepareDO row);
    @Select("SELECT * FROM bpm_approval_review_prepare WHERE tenant_id = #{tenantId} AND prepare_id = #{prepareId}")
    BpmApprovalReviewPrepareDO selectExact(@Param("tenantId") Long tenantId, @Param("prepareId") String prepareId);
}
