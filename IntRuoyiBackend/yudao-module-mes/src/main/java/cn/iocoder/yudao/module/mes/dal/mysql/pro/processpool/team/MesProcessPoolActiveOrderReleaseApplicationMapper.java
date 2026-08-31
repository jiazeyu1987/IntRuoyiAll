package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.Collections;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderReleaseApplicationMapper
        extends BaseMapperX<MesProcessPoolActiveOrderReleaseApplicationDO> {

    default MesProcessPoolActiveOrderReleaseApplicationDO selectByRemarkForUpdate(String remark, Long tenantId) {
        if (remark == null || remark.isBlank() || tenantId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getRemark, remark)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getTenantId, tenantId)
                .last("FOR UPDATE"));
    }

    @Select("""
            SELECT *
            FROM mes_pro_process_pool_active_order_release_application
            WHERE id = #{id}
              AND deleted = b'0'
            FOR UPDATE
            """)
    MesProcessPoolActiveOrderReleaseApplicationDO selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT *
            FROM mes_pro_process_pool_active_order_release_application
            WHERE batch_execution_id = #{batchExecutionId}
              AND deleted = b'0'
            FOR UPDATE
            """)
    MesProcessPoolActiveOrderReleaseApplicationDO selectByBatchExecutionIdForUpdate(
            @Param("batchExecutionId") Long batchExecutionId);

    @Select("""
            SELECT *
            FROM mes_pro_process_pool_active_order_release_application
            WHERE release_transaction_id = #{releaseTransactionId}
              AND deleted = b'0'
            FOR UPDATE
            """)
    MesProcessPoolActiveOrderReleaseApplicationDO selectByReleaseTransactionIdForUpdate(
            @Param("releaseTransactionId") Long releaseTransactionId);

    @Update("""
            UPDATE mes_pro_process_pool_active_order_release_application
            SET application_status = #{targetStatus},
                version = version + 1
            WHERE id = #{id}
              AND deleted = b'0'
              AND version = #{expectedVersion}
              AND application_status = #{expectedStatus}
            """)
    int compareAndSetStatus(@Param("id") Long id,
                            @Param("expectedVersion") Integer expectedVersion,
                            @Param("expectedStatus") String expectedStatus,
                            @Param("targetStatus") String targetStatus);

    @Update("""
            UPDATE mes_pro_process_pool_active_order_release_application
            SET version = version + 1
            WHERE id = #{id}
              AND deleted = b'0'
              AND version = #{expectedVersion}
              AND application_status = 'REPORT_UPLOAD_PENDING'
            """)
    int advanceReportVersion(@Param("id") Long id,
                             @Param("expectedVersion") Integer expectedVersion);

    @Update("""
            UPDATE mes_pro_process_pool_active_order_release_application
            SET application_status = 'MANAGER_RELEASE_PENDING',
                report_snapshot_hash = #{reportSnapshotHash},
                release_transaction_id = #{releaseTransactionId},
                release_approval_work_task_id = #{managerReleaseWorkTaskId},
                dossier_summary_json = JSON_SET(
                    COALESCE(dossier_summary_json, JSON_OBJECT()),
                    '$.managerCandidateSnapshotHash', #{managerCandidateSnapshotHash}),
                version = version + 1
            WHERE id = #{id}
              AND deleted = b'0'
              AND version = #{expectedVersion}
              AND application_status = 'REPORT_UPLOAD_PENDING'
            """)
    int handoffReportsToManager(@Param("id") Long id,
                                @Param("expectedVersion") Integer expectedVersion,
                                @Param("reportSnapshotHash") String reportSnapshotHash,
                                @Param("releaseTransactionId") Long releaseTransactionId,
                                @Param("managerReleaseWorkTaskId") Long managerReleaseWorkTaskId,
                                @Param("managerCandidateSnapshotHash") String managerCandidateSnapshotHash);

    @Update("""
            UPDATE mes_pro_process_pool_active_order_release_application
            SET application_status = 'RELEASED',
                version = version + 1
            WHERE id = #{id}
              AND deleted = b'0'
              AND version = #{expectedVersion}
              AND application_status = 'MANAGER_RELEASE_PENDING'
              AND report_snapshot_hash = #{reportSnapshotHash}
              AND release_transaction_id = #{releaseTransactionId}
              AND release_approval_work_task_id = #{managerReleaseWorkTaskId}
            """)
    int releaseFromManager(@Param("id") Long id,
                           @Param("expectedVersion") Integer expectedVersion,
                           @Param("reportSnapshotHash") String reportSnapshotHash,
                           @Param("releaseTransactionId") Long releaseTransactionId,
                           @Param("managerReleaseWorkTaskId") Long managerReleaseWorkTaskId);

    @Update("""
            UPDATE mes_pro_process_pool_active_order_release_application
            SET application_status = 'REPORT_UPLOAD_PENDING',
                batch_execution_id = #{batchExecutionId},
                pqc_decision = 'APPROVE',
                pqc_decided_by = #{decidedBy},
                pqc_decided_at = #{decidedAt},
                pqc_reject_reason = NULL,
                report_snapshot_hash = #{reportSnapshotHash},
                dossier_summary_json = #{dossierSummaryJson},
                version = version + 1
            WHERE id = #{id}
              AND deleted = b'0'
              AND version = #{expectedVersion}
              AND application_status = 'PQC_RELEASE_PENDING'
            """)
    int approveFromPending(@Param("id") Long id,
                           @Param("expectedVersion") Integer expectedVersion,
                           @Param("batchExecutionId") Long batchExecutionId,
                           @Param("decidedBy") Long decidedBy,
                           @Param("decidedAt") LocalDateTime decidedAt,
                           @Param("reportSnapshotHash") String reportSnapshotHash,
                           @Param("dossierSummaryJson") String dossierSummaryJson);

    @Update("""
            UPDATE mes_pro_process_pool_active_order_release_application
            SET application_status = 'PQC_RELEASE_REJECTED',
                pqc_decision = 'REJECT',
                pqc_decided_by = #{decidedBy},
                pqc_decided_at = #{decidedAt},
                pqc_reject_reason = #{rejectReason},
                dossier_summary_json = #{dossierSummaryJson},
                version = version + 1
            WHERE id = #{id}
              AND deleted = b'0'
              AND version = #{expectedVersion}
              AND application_status = 'PQC_RELEASE_PENDING'
            """)
    int rejectFromPending(@Param("id") Long id,
                          @Param("expectedVersion") Integer expectedVersion,
                          @Param("decidedBy") Long decidedBy,
                          @Param("decidedAt") LocalDateTime decidedAt,
                          @Param("rejectReason") String rejectReason,
                          @Param("dossierSummaryJson") String dossierSummaryJson);

    @Update("""
            UPDATE mes_pro_process_pool_active_order_release_application
            SET application_status = 'PQC_RELEASE_REJECTED',
                pqc_decision = #{pqcDecision},
                pqc_decided_by = #{decidedBy},
                pqc_decided_at = #{decidedAt},
                pqc_reject_reason = #{rejectReason},
                dossier_summary_json = #{dossierSummaryJson},
                version = version + 1
            WHERE id = #{id}
              AND deleted = b'0'
              AND version = #{expectedVersion}
              AND application_status = 'PQC_RELEASE_PENDING'
            """)
    int closeFromNonconformance(@Param("id") Long id,
                                @Param("expectedVersion") Integer expectedVersion,
                                @Param("pqcDecision") String pqcDecision,
                                @Param("decidedBy") Long decidedBy,
                                @Param("decidedAt") LocalDateTime decidedAt,
                                @Param("rejectReason") String rejectReason,
                                @Param("dossierSummaryJson") String dossierSummaryJson);

    default MesProcessPoolActiveOrderReleaseApplicationDO selectByRequestIdempotencyKey(
            Long activeOrderId, String requestIdempotencyKey) {
        if (activeOrderId == null || requestIdempotencyKey == null || requestIdempotencyKey.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getRequestIdempotencyKey, requestIdempotencyKey)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId)
                .last("LIMIT 1"));
    }

    default MesProcessPoolActiveOrderReleaseApplicationDO selectByBusinessIdempotencyKey(
            Long activeOrderId, String businessIdempotencyKey) {
        if (activeOrderId == null || businessIdempotencyKey == null || businessIdempotencyKey.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, activeOrderId)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getBusinessIdempotencyKey, businessIdempotencyKey)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId)
                .last("LIMIT 1"));
    }

    default MesProcessPoolActiveOrderReleaseApplicationDO selectLatestByActiveOrderId(Long activeOrderId) {
        if (activeOrderId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, activeOrderId)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId)
                .last("LIMIT 1"));
    }

    default List<MesProcessPoolActiveOrderReleaseApplicationDO> selectLatestByActiveOrderIds(
            Collection<Long> activeOrderIds) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .in(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, activeOrderIds)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId));
    }

    default List<MesProcessPoolActiveOrderReleaseApplicationDO> selectListByActiveOrderIds(
            Collection<Long> activeOrderIds) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .in(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, activeOrderIds)
                .orderByAsc(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId)
                .orderByAsc(MesProcessPoolActiveOrderReleaseApplicationDO::getId));
    }

    default List<MesProcessPoolActiveOrderReleaseApplicationDO> selectListByActiveOrderIdsForUpdate(
            Collection<Long> activeOrderIds) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .in(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId, activeOrderIds)
                .orderByAsc(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId)
                .orderByAsc(MesProcessPoolActiveOrderReleaseApplicationDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolActiveOrderReleaseApplicationDO> selectListByReleaseTransactionId(
            Long releaseTransactionId) {
        if (releaseTransactionId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getReleaseTransactionId, releaseTransactionId)
                .orderByAsc(MesProcessPoolActiveOrderReleaseApplicationDO::getActiveOrderId)
                .orderByAsc(MesProcessPoolActiveOrderReleaseApplicationDO::getId));
    }

    default List<MesProcessPoolActiveOrderReleaseApplicationDO> selectListByBatchExecutionIds(
            Collection<Long> batchExecutionIds) {
        if (batchExecutionIds == null || batchExecutionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .in(MesProcessPoolActiveOrderReleaseApplicationDO::getBatchExecutionId, batchExecutionIds)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId));
    }

    default List<MesProcessPoolActiveOrderReleaseApplicationDO> selectListForPqcReleasePage(
            String workOrderCode, String batchCode) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .likeIfPresent(MesProcessPoolActiveOrderReleaseApplicationDO::getWorkOrderCode, workOrderCode)
                .likeIfPresent(MesProcessPoolActiveOrderReleaseApplicationDO::getBatchCode, batchCode)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId));
    }

    default int deleteByActiveOrderId(Long activeOrderId) {
        return activeOrderId == null ? 0 : physicalDeleteByActiveOrderId(activeOrderId);
    }

    @Delete("DELETE FROM mes_pro_process_pool_active_order_release_application WHERE active_order_id = #{activeOrderId}")
    int physicalDeleteByActiveOrderId(@Param("activeOrderId") Long activeOrderId);
}
