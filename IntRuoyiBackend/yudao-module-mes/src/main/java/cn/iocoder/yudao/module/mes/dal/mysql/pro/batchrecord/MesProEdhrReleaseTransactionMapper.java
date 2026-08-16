package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProEdhrReleaseTransactionMapper extends BaseMapperX<MesProEdhrReleaseTransactionDO> {

    default MesProEdhrReleaseTransactionDO selectByBatchExecutionId(Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .eq(MesProEdhrReleaseTransactionDO::getBatchExecutionId, batchExecutionId)
                .orderByDesc(MesProEdhrReleaseTransactionDO::getId));
    }

    default List<MesProEdhrReleaseTransactionDO> selectListByBatchExecutionIds(Collection<Long> batchExecutionIds) {
        if (batchExecutionIds == null || batchExecutionIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .in(MesProEdhrReleaseTransactionDO::getBatchExecutionId, batchExecutionIds)
                .orderByDesc(MesProEdhrReleaseTransactionDO::getId));
    }

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
                approval_opinion = #{approvalOpinion},
                version = version + 1
            WHERE id = #{id}
              AND deleted = b'0'
              AND version = #{expectedVersion}
              AND release_status = 'PENDING_APPROVAL'
            """)
    int approveProductionRelease(@Param("id") Long id,
                                 @Param("expectedVersion") Integer expectedVersion,
                                 @Param("approvedBy") Long approvedBy,
                                 @Param("idempotencyKey") String idempotencyKey,
                                 @Param("signoffEvidenceHash") String signoffEvidenceHash,
                                 @Param("approvalOpinion") String approvalOpinion,
                                 @Param("approvedAt") LocalDateTime approvedAt);
}
