package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
            WHERE current_batch_execution_id = #{batchExecutionId}
              AND deleted = b'0'
            FOR UPDATE
            """)
    MesProcessPoolActiveOrderReleaseApplicationDO selectByBatchExecutionIdForUpdate(
            @Param("batchExecutionId") Long batchExecutionId);

    default List<MesProcessPoolActiveOrderReleaseApplicationDO> selectListByBoundBatchForUpdate(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getBatchExecutionId, batchExecutionId).last("FOR UPDATE"));
    }

    @Update("""
            UPDATE mes_pro_process_pool_active_order_release_application
            SET application_status = 'BATCH_VOIDED', dossier_summary_json = #{dossierSummaryJson}, version = version + 1
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0
              AND version = #{expectedVersion} AND application_status = 'PQC_RELEASE_PENDING'
              AND batch_execution_id = #{batchExecutionId} AND pqc_release_work_task_id = #{pqcReleaseWorkTaskId}
            """)
    int closeFromBatchVoid(@Param("id") Long id, @Param("tenantId") Long tenantId,
            @Param("expectedVersion") Integer expectedVersion, @Param("batchExecutionId") Long batchExecutionId,
            @Param("pqcReleaseWorkTaskId") Long pqcReleaseWorkTaskId, @Param("dossierSummaryJson") String dossierSummaryJson);

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
            SET batch_execution_id = #{batchExecutionId},
                version = version + 1
            WHERE id = #{id}
              AND deleted = 0
              AND version = #{expectedVersion}
              AND application_status = 'PQC_RELEASE_PENDING'
              AND batch_execution_id IS NULL
            """)
    int bindP3BatchExecution(@Param("id") Long id,
                             @Param("expectedVersion") Integer expectedVersion,
                             @Param("batchExecutionId") Long batchExecutionId);

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
              AND application_status IN ('REPORT_UPLOAD_PENDING', 'MANAGER_RELEASE_PENDING')
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
            SET application_status = 'MANAGER_RELEASE_PENDING',
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

    @Update("UPDATE mes_pro_process_pool_active_order_release_application SET application_status = #{closureStatus}, qa_closure_review_id = #{reviewId}, "
            + "version = version + 1 WHERE id = #{id} AND deleted = 0 AND version = #{expectedVersion} "
            + "AND #{reviewId} > 0 AND #{closureStatus} IN ('NONCONFORMANCE_REWORK','NONCONFORMANCE_VOID') "
            + "AND application_status IN ('PQC_RELEASE_PENDING','REPORT_UPLOAD_PENDING','MANAGER_RELEASE_PENDING','RELEASED')")
    int closeLifecycleFromNonconformance(@Param("id") Long id, @Param("expectedVersion") Integer expectedVersion,
                                         @Param("closureStatus") String closureStatus, @Param("reviewId") Long reviewId);

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

    default MesProcessPoolActiveOrderReleaseApplicationDO selectByBatchExecutionId(Long batchExecutionId) {
        if (batchExecutionId == null || batchExecutionId <= 0) {
            throw new IllegalArgumentException("正式批次编号无效");
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getBatchExecutionId, batchExecutionId));
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

    default MesProcessPoolActiveOrderReleaseApplicationDO selectLatestReworkClosedByActiveOrderId(Long activeOrderId) {
        if (activeOrderId == null) {
            return null;
        }
        return selectLatestReworkClosedByActiveOrderIdInternal(activeOrderId);
    }

    @Select("""
            SELECT * FROM mes_pro_process_pool_active_order_release_application
            WHERE active_order_id = #{activeOrderId} AND deleted = b'0'
              AND application_status = 'NONCONFORMANCE_REWORK'
            ORDER BY id DESC LIMIT 1
            """)
    MesProcessPoolActiveOrderReleaseApplicationDO selectLatestReworkClosedByActiveOrderIdInternal(
            @Param("activeOrderId") Long activeOrderId);

    default List<Long> selectReworkClosedApplicationIds(Collection<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .in(MesProcessPoolActiveOrderReleaseApplicationDO::getId, applicationIds)
                .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getApplicationStatus, "NONCONFORMANCE_REWORK"))
                .stream().map(MesProcessPoolActiveOrderReleaseApplicationDO::getId).toList();
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
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .in("current_batch_execution_id", batchExecutionIds)
                .orderByDesc("id"));
    }

    default List<MesProcessPoolActiveOrderReleaseApplicationDO> selectListForPqcReleasePage(
            String workOrderCode, String batchCode) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                .likeIfPresent(MesProcessPoolActiveOrderReleaseApplicationDO::getWorkOrderCode, workOrderCode)
                .likeIfPresent(MesProcessPoolActiveOrderReleaseApplicationDO::getBatchCode, batchCode)
                .orderByDesc(MesProcessPoolActiveOrderReleaseApplicationDO::getId));
    }

    default PageResult<MesProcessPoolActiveOrderReleaseApplicationDO> selectPqcReleasePage(
            PageParam pageParam, Long tenantId, Long actorUserId, String viewStatus,
            String workOrderCode, String batchCode, Long applicationId, Long pqcReleaseWorkTaskId) {
        if (pageParam == null || tenantId == null || actorUserId == null || viewStatus == null) {
            throw new IllegalArgumentException("PQC release page query is incomplete");
        }
        IPage<MesProcessPoolActiveOrderReleaseApplicationDO> page =
                new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        selectPqcReleasePage(page, tenantId, actorUserId, viewStatus, workOrderCode, batchCode,
                applicationId, pqcReleaseWorkTaskId);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    /**
     * PQC release list query. The candidate, derived view status and pagination are all pushed into SQL so the
     * service does not load the tenant's complete release history before slicing the requested page.
     */
    @org.apache.ibatis.annotations.Select({
            "<script>",
            "SELECT a.id, a.active_order_id, a.work_order_id, a.work_order_code, a.route_id, a.route_version_id,",
            "       a.product_id, a.batch_code, a.batch_execution_id, a.release_transaction_id,",
            "       a.release_approval_work_task_id, a.pqc_release_work_task_id, a.pqc_decision, a.pqc_decided_by,",
            "       a.pqc_decided_at, a.pqc_reject_reason, a.application_status, a.source_snapshot_hash,",
            "       a.report_snapshot_hash, a.version, a.applied_by, a.applied_at, a.last_precheck_at, a.remark,",
            "       a.dossier_summary_json, a.creator, a.create_time, a.updater, a.update_time, a.deleted, a.tenant_id",
            "FROM mes_pro_process_pool_active_order_release_application a",
            "INNER JOIN mes_pro_edhr_work_task t",
            "        ON t.id = a.pqc_release_work_task_id",
            "       AND t.tenant_id = #{tenantId}",
            "       AND t.deleted = 0",
            "       AND t.task_type = 'PQC_PRODUCTION_RELEASE'",
            "       AND t.business_scope_type = 'RELEASE_APPLICATION'",
            "       AND t.business_scope_id = a.id",
            "LEFT JOIN mes_pro_edhr_nonconformance_review r",
            "       ON r.source_type = 'ACTIVE_ORDER'",
            "      AND r.source_id = a.active_order_id",
            "      AND r.active_order_id = a.active_order_id",
            "      AND r.deleted = 0 AND r.tenant_id = #{tenantId}",
            "      AND r.frozen_at &gt;= a.applied_at",
            "      AND NOT EXISTS (",
            "          SELECT 1 FROM mes_pro_process_pool_active_order_release_application next_a",
            "          WHERE next_a.active_order_id = a.active_order_id",
            "            AND next_a.deleted = 0 AND next_a.tenant_id = #{tenantId}",
            "            AND next_a.applied_at &gt; a.applied_at",
            "            AND next_a.applied_at &lt;= r.frozen_at",
            "      )",
            "      AND NOT EXISTS (",
            "          SELECT 1 FROM mes_pro_edhr_nonconformance_review newer_r",
            "          WHERE newer_r.source_type = 'ACTIVE_ORDER'",
            "            AND newer_r.source_id = a.active_order_id",
            "            AND newer_r.active_order_id = a.active_order_id",
            "            AND newer_r.deleted = 0 AND newer_r.tenant_id = #{tenantId}",
            "            AND newer_r.frozen_at &gt;= a.applied_at",
            "            AND NOT EXISTS (",
            "                SELECT 1 FROM mes_pro_process_pool_active_order_release_application newer_a",
            "                WHERE newer_a.active_order_id = a.active_order_id",
            "                  AND newer_a.deleted = 0 AND newer_a.tenant_id = #{tenantId}",
            "                  AND newer_a.applied_at &gt; a.applied_at",
            "                  AND newer_a.applied_at &lt;= newer_r.frozen_at",
            "            )",
            "            AND newer_r.id &gt; r.id",
            "      )",
            "WHERE a.tenant_id = #{tenantId} AND a.deleted = 0",
            "  AND CONCAT(',', REPLACE(COALESCE(t.candidate_user_snapshot, ''), ' ', ''), ',')",
            "      LIKE CONCAT('%,', #{actorUserId}, ',%')",
            "<if test='applicationId != null'>",
            "  AND a.id = #{applicationId}",
            "</if>",
            "<if test='pqcReleaseWorkTaskId != null'>",
            "  AND a.pqc_release_work_task_id = #{pqcReleaseWorkTaskId}",
            "</if>",
            "<if test='workOrderCode != null and workOrderCode != \"\"'>",
            "  AND a.work_order_code LIKE CONCAT('%', #{workOrderCode}, '%')",
            "</if>",
            "<if test='batchCode != null and batchCode != \"\"'>",
            "  AND a.batch_code LIKE CONCAT('%', #{batchCode}, '%')",
            "</if>",
            "<choose>",
            "  <when test='viewStatus == \"PENDING\"'>",
            "    AND a.application_status = 'PQC_RELEASE_PENDING'",
            "    AND (r.id IS NULL OR r.review_status &lt;&gt; 'closed' OR r.closed_at &lt; a.applied_at",
            "         OR (r.review_status = 'closed' AND r.disposition = 'concession_release'))",
            "  </when>",
            "  <when test='viewStatus == \"RELEASED\"'>",
            "    AND a.application_status IN ('REPORT_UPLOAD_PENDING', 'MANAGER_RELEASE_PENDING', 'RELEASED')",
            "    AND (r.id IS NULL OR r.review_status &lt;&gt; 'closed' OR r.closed_at &lt; a.applied_at)",
            "  </when>",
            "  <when test='viewStatus == \"VOIDED\"'>",
            "    AND ((r.review_status = 'closed' AND r.disposition = 'void' AND r.closed_at &gt;= a.applied_at)",
            "      OR (a.application_status = 'BATCH_VOIDED' AND EXISTS (",
            "          SELECT 1 FROM mes_pro_edhr_record_change_event v",
            "          WHERE v.tenant_id = a.tenant_id AND v.deleted = 0 AND v.batch_execution_id = a.batch_execution_id",
            "            AND v.change_type = 'VOID' AND v.target_scope = 'BATCH' AND v.change_status = 'EFFECTIVE'",
            "            AND v.id = CAST(JSON_UNQUOTE(JSON_EXTRACT(a.dossier_summary_json, '$.batchVoid.changeEventId')) AS UNSIGNED)",
            "      )))",
            "  </when>",
            "  <when test='viewStatus == \"REWORKED\"'>",
            "    AND r.review_status = 'closed' AND r.disposition = 'rework' AND r.closed_at &gt;= a.applied_at",
            "  </when>",
            "  <when test='viewStatus == \"CONCESSION_RELEASED\"'>",
            "    AND a.application_status IN ('REPORT_UPLOAD_PENDING', 'MANAGER_RELEASE_PENDING', 'RELEASED')",
            "    AND r.review_status = 'closed' AND r.disposition = 'concession_release' AND r.closed_at &gt;= a.applied_at",
            "  </when>",
            "</choose>",
            "ORDER BY a.id DESC",
            "</script>"
    })
    IPage<MesProcessPoolActiveOrderReleaseApplicationDO> selectPqcReleasePage(
            IPage<MesProcessPoolActiveOrderReleaseApplicationDO> page,
            @Param("tenantId") Long tenantId,
            @Param("actorUserId") Long actorUserId,
            @Param("viewStatus") String viewStatus,
            @Param("workOrderCode") String workOrderCode,
            @Param("batchCode") String batchCode,
            @Param("applicationId") Long applicationId,
            @Param("pqcReleaseWorkTaskId") Long pqcReleaseWorkTaskId);

    default int deleteByActiveOrderId(Long activeOrderId) {
        return activeOrderId == null ? 0 : physicalDeleteByActiveOrderId(activeOrderId);
    }

    @Delete("DELETE FROM mes_pro_process_pool_active_order_release_application WHERE active_order_id = #{activeOrderId}")
    int physicalDeleteByActiveOrderId(@Param("activeOrderId") Long activeOrderId);
}
