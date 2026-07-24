package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionClaimDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface DccControlledFileRecognitionClaimMapper extends BaseMapperX<DccControlledFileRecognitionClaimDO> {

    @Insert("""
            INSERT IGNORE INTO dcc_controlled_file_recognition_claim (
                tenant_id, controlled_file_id, recognition_scope, claimed_by, claim_task_id, claimed_at,
                creator, updater, deleted
            ) VALUES (
                #{tenantId}, #{controlledFileId}, #{recognitionScope}, #{claimedBy}, #{claimTaskId}, #{claimedAt},
                '', '', 0
            )
            """)
    int tryClaimBasicInfo(@Param("tenantId") Long tenantId,
                          @Param("controlledFileId") Long controlledFileId,
                          @Param("recognitionScope") String recognitionScope,
                          @Param("claimedBy") Long claimedBy,
                          @Param("claimTaskId") Long claimTaskId,
                          @Param("claimedAt") java.time.LocalDateTime claimedAt);

    @Select("""
            SELECT *
            FROM dcc_controlled_file_recognition_claim
            WHERE controlled_file_id = #{controlledFileId}
              AND recognition_scope = #{recognitionScope}
              AND deleted = 0
            LIMIT 1
            """)
    DccControlledFileRecognitionClaimDO selectByFileAndScope(@Param("controlledFileId") Long controlledFileId,
                                                             @Param("recognitionScope") String recognitionScope);

    @Delete("""
            DELETE FROM dcc_controlled_file_recognition_claim
            WHERE controlled_file_id = #{controlledFileId}
              AND recognition_scope = #{recognitionScope}
              AND claimed_by = #{claimedBy}
              AND ((#{claimTaskId} IS NULL AND claim_task_id IS NULL) OR claim_task_id = #{claimTaskId})
            """)
    int releaseClaim(@Param("controlledFileId") Long controlledFileId,
                     @Param("recognitionScope") String recognitionScope,
                     @Param("claimedBy") Long claimedBy,
                     @Param("claimTaskId") Long claimTaskId);

    @Delete("""
            DELETE FROM dcc_controlled_file_recognition_claim
            WHERE claim_task_id = #{claimTaskId}
            """)
    int releaseClaimsByTaskId(@Param("claimTaskId") Long claimTaskId);

    @Delete("""
            DELETE claim
            FROM dcc_controlled_file_recognition_claim claim
            INNER JOIN dcc_controlled_file_batch_recognition_task task
                    ON task.id = claim.claim_task_id
                   AND task.tenant_id = claim.tenant_id
            WHERE claim.deleted = 0
              AND task.status IN ('COMPLETED', 'FAILED', 'STOPPED')
            """)
    int releaseClaimsOwnedByTerminalTasks();
}
