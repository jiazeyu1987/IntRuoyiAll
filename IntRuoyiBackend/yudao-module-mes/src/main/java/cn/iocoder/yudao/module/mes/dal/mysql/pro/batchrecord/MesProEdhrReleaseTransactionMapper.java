package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePageReqVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;
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

    /** Both history and current-batch views use the same SQL predicate before count and limit. */
    default QueryWrapper<Object> buildReleaseListQuery(MesProEdhrReleasePageReqVO req, boolean history) {
        QueryWrapper<Object> query = new QueryWrapper<>();
        String source = history ? "rt." : "b.";
        query.like(cn.hutool.core.util.StrUtil.isNotBlank(req.getBatchExecutionCode()), source + "batch_execution_code", req.getBatchExecutionCode())
                .like(cn.hutool.core.util.StrUtil.isNotBlank(req.getWorkOrderCode()), source + "work_order_code", req.getWorkOrderCode())
                .like(cn.hutool.core.util.StrUtil.isNotBlank(req.getBatchCode()), source + "batch_code", req.getBatchCode())
                .like(cn.hutool.core.util.StrUtil.isNotBlank(req.getProductCode()), source + "product_code", req.getProductCode());
        if (req.getCreateTime() != null) {
            if (req.getCreateTime().length > 0 && req.getCreateTime()[0] != null) query.ge(source + "create_time", req.getCreateTime()[0]);
            if (req.getCreateTime().length > 1 && req.getCreateTime()[1] != null) query.le(source + "create_time", req.getCreateTime()[1]);
        }
        if (req.getBatchExecutionStatuses() != null && !req.getBatchExecutionStatuses().isEmpty()) query.in("b.status", req.getBatchExecutionStatuses());
        if (req.getExcludeBatchExecutionStatuses() != null && !req.getExcludeBatchExecutionStatuses().isEmpty()) query.notIn("b.status", req.getExcludeBatchExecutionStatuses());
        if (Boolean.TRUE.equals(req.getCompletedTraceOnly())) {
            query.and(q -> q.eq("rt.release_status", "RELEASED").or().in("b.status", 40, 50, 60));
        } else if (!history) {
            query.ne("b.status", 60);
        }
        query.eq(cn.hutool.core.util.StrUtil.isNotBlank(req.getReleaseStatus()), "CASE WHEN rt.id IS NULL THEN 'PRECHECK_REQUIRED' ELSE rt.release_status END", req.getReleaseStatus())
                .eq(cn.hutool.core.util.StrUtil.isNotBlank(req.getDhrStatus()), "CASE WHEN rt.id IS NULL THEN 'PRECHECK_REQUIRED' ELSE rt.dhr_status END", req.getDhrStatus())
                .eq(cn.hutool.core.util.StrUtil.isNotBlank(req.getInspectionStatus()), "CASE WHEN rt.id IS NULL THEN 'PRECHECK_REQUIRED' ELSE rt.inspection_status END", req.getInspectionStatus());
        String status = "CASE WHEN rt.id IS NULL THEN 'PRECHECK_REQUIRED' ELSE rt.release_status END";
        String statusLabel = "CASE " + status + " WHEN 'PRECHECK_REQUIRED' THEN '待预检' WHEN 'PRECHECK_FAILED' THEN '预检失败' WHEN 'PRECHECK_PASSED' THEN '预检通过' WHEN 'PENDING_APPROVAL' THEN '待审批' WHEN 'RELEASED' THEN '已放行' WHEN 'REJECTED' THEN '已驳回' WHEN 'WITHDRAWN' THEN '已撤回' ELSE '未知状态' END";
        String batchStatus = "CASE b.status WHEN 0 THEN '已创建' WHEN 10 THEN '执行中' WHEN 20 THEN '待关闭' WHEN 25 THEN '需返工' WHEN 30 THEN '已关闭' WHEN 40 THEN '已放行' WHEN 50 THEN '已返工' WHEN 60 THEN '已作废' ELSE COALESCE(CAST(b.status AS CHAR),'0') END";
        String summary = "CASE " + status + " WHEN 'PRECHECK_REQUIRED' THEN '尚未执行放行前检查' WHEN 'PRECHECK_PASSED' THEN '放行前检查通过' WHEN 'PENDING_APPROVAL' THEN '放行审批待处理' WHEN 'RELEASED' THEN '放行已批准' WHEN 'REJECTED' THEN CONCAT('放行已驳回：',COALESCE(rt.reject_reason,'未记录原因')) WHEN 'WITHDRAWN' THEN CONCAT('放行已撤回：',COALESCE(rt.withdraw_reason,'未记录原因')) ELSE CONCAT('放行前检查失败：',COALESCE(CAST(rt.blocking_check_count AS CHAR),'null'),' 个阻塞项，',COALESCE(CAST(rt.failed_check_count AS CHAR),'null'),' 个失败项') END";
        String quality = java.util.stream.Stream.of("dhr_status", "inspection_status", "inventory_status", "deviation_status", "rework_status", "scrap_status")
                .map(column -> "CASE TRIM(COALESCE(rt." + column + ",'')) WHEN 'PASS' THEN '通过' WHEN 'FAIL' THEN '失败' WHEN 'BLOCKER' THEN '阻塞' WHEN 'NOT_APPLICABLE' THEN '不适用' WHEN 'PRECHECK_REQUIRED' THEN '待检' WHEN '' THEN '待检' ELSE '未知结果' END")
                .collect(java.util.stream.Collectors.joining(",' ',", "CONCAT(", ")"));
        return ListFilterQueryUtils.apply(req, query, java.util.Map.ofEntries(
                java.util.Map.entry("releaseObject", text("CONCAT_WS(' ',COALESCE(b.batch_execution_code,''),COALESCE(b.work_order_code,''),COALESCE(b.batch_code,''))")),
                java.util.Map.entry("productRoute", text("CONCAT(COALESCE(NULLIF(b.product_name,''),'--'),' ',COALESCE(NULLIF(b.product_code,''),'--'),' ',COALESCE(NULLIF(b.route_name,''),NULLIF(b.route_code,''),'--'))")),
                java.util.Map.entry("releaseStatus", text("CONCAT(" + statusLabel + ",' '," + batchStatus + ",' '," + summary + ")")),
                java.util.Map.entry("checkSummary", text("CONCAT(COALESCE(rt.blocking_check_count,0),' ',COALESCE(rt.failed_check_count,0),' ',COALESCE(DATE_FORMAT(rt.last_precheck_at,'%Y-%m-%d %H:%i:%s'),'--'))")),
                java.util.Map.entry("qualityGate", text(quality)),
                java.util.Map.entry("transactionTime", text("CONCAT(COALESCE(DATE_FORMAT(rt.submitted_at,'%Y-%m-%d %H:%i:%s'),'--'),' ',COALESCE(DATE_FORMAT(rt.approved_at,'%Y-%m-%d %H:%i:%s'),'--'))")),
                java.util.Map.entry("traceActions", text("'打印'"))));
    }

    String RELEASE_LIST_FROM = """
            FROM mes_pro_edhr_batch_execution b
            LEFT JOIN mes_pro_edhr_release_transaction rt ON rt.batch_execution_id = b.id
              AND rt.tenant_id = b.tenant_id AND rt.deleted = 0
              <if test="!history">AND rt.id = (SELECT MAX(latest.id) FROM mes_pro_edhr_release_transaction latest
                WHERE latest.tenant_id = b.tenant_id AND latest.batch_execution_id = b.id AND latest.deleted = 0)</if>
            WHERE b.tenant_id = #{tenantId} AND b.deleted = 0
              <if test="history">AND rt.id IS NOT NULL</if>
              <if test="ew != null and !ew.emptyOfNormal">AND (${ew.sqlSegment})</if>
              <if test="access != null and !access.emptyOfNormal">AND (${access.sqlSegment})</if>
            """;

    @Select("<script>SELECT COUNT(*) " + RELEASE_LIST_FROM + "</script>")
    long countReleaseList(@Param("tenantId") Long tenantId, @Param("history") boolean history,
                          @Param("ew") QueryWrapper<Object> query,
                          @Param("access") LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> access);

    @Select("<script>SELECT b.id AS batch_execution_id, rt.id AS release_transaction_id " + RELEASE_LIST_FROM
            + "<choose><when test='history'>ORDER BY rt.id DESC</when><otherwise>ORDER BY b.id DESC</otherwise></choose>"
            + " LIMIT #{limit} OFFSET #{offset}</script>")
    List<ReleaseListRow> selectReleaseList(@Param("tenantId") Long tenantId, @Param("history") boolean history,
                          @Param("ew") QueryWrapper<Object> query,
                          @Param("access") LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> access,
                          @Param("offset") long offset, @Param("limit") int limit);

    @lombok.Data
    class ReleaseListRow {
        private Long batchExecutionId;
        private Long releaseTransactionId;
    }

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
