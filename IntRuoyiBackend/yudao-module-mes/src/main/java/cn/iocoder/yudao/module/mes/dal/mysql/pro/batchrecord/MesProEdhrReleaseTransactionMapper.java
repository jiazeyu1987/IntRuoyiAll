package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProEdhrReleaseTransactionMapper extends BaseMapperX<MesProEdhrReleaseTransactionDO> {

    default MesProEdhrReleaseTransactionDO selectByBatchExecutionId(Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .eq(MesProEdhrReleaseTransactionDO::getBatchExecutionId, batchExecutionId)
                .orderByDesc(MesProEdhrReleaseTransactionDO::getId)
                .last("LIMIT 1"));
    }

    default MesProEdhrReleaseTransactionDO selectCurrentByBatchExecutionId(Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .eq(MesProEdhrReleaseTransactionDO::getBatchExecutionId, batchExecutionId)
                .notIn(MesProEdhrReleaseTransactionDO::getReleaseStatus, "REJECTED", "WITHDRAWN")
                .orderByDesc(MesProEdhrReleaseTransactionDO::getId)
                .last("LIMIT 1"));
    }

    @Select("SELECT * FROM mes_pro_edhr_release_transaction WHERE tenant_id = #{tenantId} "
            + "AND batch_execution_id = #{batchExecutionId} AND deleted = 0 ORDER BY id ASC FOR UPDATE")
    List<MesProEdhrReleaseTransactionDO> selectListByTenantAndBatchExecutionIdForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("batchExecutionId") Long batchExecutionId);

    default List<MesProEdhrReleaseTransactionDO> selectListByBatchExecutionIds(Collection<Long> batchExecutionIds) {
        if (batchExecutionIds == null || batchExecutionIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .in(MesProEdhrReleaseTransactionDO::getBatchExecutionId, batchExecutionIds)
                .orderByDesc(MesProEdhrReleaseTransactionDO::getId));
    }

    /** Latest persisted release, including rejected/withdrawn, matching descending history's first row. */
    default List<MesProEdhrReleaseTransactionDO> selectLatestByBatchExecutionIds(Collection<Long> batchExecutionIds) {
        if (batchExecutionIds == null || batchExecutionIds.isEmpty()) return List.of();
        return selectLatestByBatchExecutionIdsInternal(batchExecutionIds,
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
    }

    @Select("""
            <script>
            SELECT r.* FROM mes_pro_edhr_release_transaction r
            WHERE r.tenant_id = #{tenantId} AND r.deleted = 0
              AND r.batch_execution_id IN
              <foreach collection="batchIds" item="id" open="(" separator="," close=")">#{id}</foreach>
              AND r.id = (SELECT MAX(latest.id) FROM mes_pro_edhr_release_transaction latest
                  WHERE latest.tenant_id = r.tenant_id AND latest.deleted = 0
                    AND latest.batch_execution_id = r.batch_execution_id)
            ORDER BY r.id DESC
            </script>
            """)
    List<MesProEdhrReleaseTransactionDO> selectLatestByBatchExecutionIdsInternal(
            @Param("batchIds") Collection<Long> batchIds, @Param("tenantId") Long tenantId);

    default MesProEdhrReleaseTransactionDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .eq(MesProEdhrReleaseTransactionDO::getId, id)
                .last("FOR UPDATE"));
    }

    default List<MesProEdhrReleaseTransactionDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .in(MesProEdhrReleaseTransactionDO::getId, ids)
                .orderByAsc(MesProEdhrReleaseTransactionDO::getId));
    }

    default List<MesProEdhrReleaseTransactionDO> selectListByIdsForUpdate(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .in(MesProEdhrReleaseTransactionDO::getId, ids)
                .orderByAsc(MesProEdhrReleaseTransactionDO::getId)
                .last("FOR UPDATE"));
    }

    @Update("""
            UPDATE mes_pro_edhr_release_transaction
            SET release_status = 'RELEASED',
                approval_idempotency_key = #{idempotencyKey},
                approved_by = #{approvedBy},
                approved_at = #{approvedAt},
                approval_signoff_evidence_hash = #{signoffEvidenceHash},
                approval_signature_id = #{signatureId},
                approval_opinion = #{approvalOpinion},
                version = version + 1
            WHERE id = #{id}
              AND deleted = 0
              AND version = #{expectedVersion}
              AND release_status = 'PENDING_APPROVAL'
            """)
    int approveProductionRelease(@Param("id") Long id,
                                 @Param("expectedVersion") Integer expectedVersion,
                                 @Param("approvedBy") Long approvedBy,
                                 @Param("idempotencyKey") String idempotencyKey,
                                 @Param("signoffEvidenceHash") String signoffEvidenceHash,
                                 @Param("signatureId") Long signatureId,
                                 @Param("approvalOpinion") String approvalOpinion,
                                 @Param("approvedAt") LocalDateTime approvedAt);

}
