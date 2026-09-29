package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;

@Mapper
public interface MesProEdhrDeviationMapper extends BaseMapperX<MesProEdhrDeviationDO> {

    default Page<MesProEdhrDeviationDO> selectPageByTenant(Page<MesProEdhrDeviationDO> page, Long tenantId,
                                                            String status, String level, Long batchExecutionId,
                                                            String search, String sortField, String sortOrder,
                                                            LocalDateTime initiatedAtStart, LocalDateTime initiatedAtEnd) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MesProEdhrDeviationDO>()
                .eq(MesProEdhrDeviationDO::getTenantId, tenantId)
                .eq(status != null && !status.isBlank(), MesProEdhrDeviationDO::getStatus, status)
                .eq(level != null && !level.isBlank(), MesProEdhrDeviationDO::getLevel, level)
                .eq(batchExecutionId != null, MesProEdhrDeviationDO::getBatchExecutionId, batchExecutionId)
                .and(search != null && !search.isBlank(), w -> w.like(MesProEdhrDeviationDO::getDeviationCode, search)
                        .or().like(MesProEdhrDeviationDO::getBatchExecutionCode, search)
                        .or().like(MesProEdhrDeviationDO::getBatchCode, search))
                ;
        if (initiatedAtStart != null) {
            wrapper.ge(MesProEdhrDeviationDO::getInitiatedAt, initiatedAtStart);
        }
        if (initiatedAtEnd != null) {
            wrapper.le(MesProEdhrDeviationDO::getInitiatedAt, initiatedAtEnd);
        }
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        switch (sortField == null ? "" : sortField) {
            case "deviationCode" -> { if (asc) wrapper.orderByAsc(MesProEdhrDeviationDO::getDeviationCode); else wrapper.orderByDesc(MesProEdhrDeviationDO::getDeviationCode); }
            case "level" -> { if (asc) wrapper.orderByAsc(MesProEdhrDeviationDO::getLevel); else wrapper.orderByDesc(MesProEdhrDeviationDO::getLevel); }
            case "status" -> { if (asc) wrapper.orderByAsc(MesProEdhrDeviationDO::getStatus); else wrapper.orderByDesc(MesProEdhrDeviationDO::getStatus); }
            case "initiatedAt" -> { if (asc) wrapper.orderByAsc(MesProEdhrDeviationDO::getInitiatedAt); else wrapper.orderByDesc(MesProEdhrDeviationDO::getInitiatedAt); }
            default -> wrapper.orderByDesc(MesProEdhrDeviationDO::getId);
        }
        return selectPage(page, wrapper);
    }

    @Select("""
            SELECT *
            FROM mes_pro_edhr_deviation
            WHERE tenant_id = #{tenantId}
              AND create_idempotency_key = #{idempotencyKey}
              AND deleted = 0
            LIMIT 1
            FOR UPDATE
            """)
    MesProEdhrDeviationDO selectByTenantAndCreateIdempotencyKeyForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT *
            FROM mes_pro_edhr_deviation
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND deleted = 0
            """)
    MesProEdhrDeviationDO selectByTenantAndId(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Select("""
            SELECT *
            FROM mes_pro_edhr_deviation
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND deleted = 0
            LIMIT 1
            FOR UPDATE
            """)
    MesProEdhrDeviationDO selectByTenantAndIdForUpdate(@Param("tenantId") Long tenantId,
                                                        @Param("id") Long id);

    @Update("""
            UPDATE mes_pro_edhr_deviation
            SET status = 'CLOSED',
                close_reason = #{closeReason},
                closed_at = #{closedAt},
                version = version + 1
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND status = 'OPEN'
              AND deleted = 0
            """)
    int closeNormally(@Param("tenantId") Long tenantId,
                      @Param("id") Long id,
                      @Param("closeReason") String closeReason,
                      @Param("closedAt") java.time.LocalDateTime closedAt);

    @Select("""
            SELECT id
            FROM mes_pro_edhr_deviation
            WHERE tenant_id = #{tenantId}
              AND batch_execution_id = #{batchExecutionId}
              AND status = 'OPEN'
              AND deleted = 0
            ORDER BY id ASC
            LIMIT 1
            FOR UPDATE
            """)
    Long selectOpenIdByTenantAndBatchExecutionIdForUpdate(@Param("tenantId") Long tenantId,
                                                          @Param("batchExecutionId") Long batchExecutionId);

    @Update("""
            UPDATE mes_pro_edhr_deviation
            SET initiator_signature_id = #{signatureId},
                initiator_content_hash = #{contentHash},
                initiator_name = #{initiatorName}
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND initiator_signature_id IS NULL
              AND deleted = 0
            """)
    int attachInitiatorSignature(@Param("tenantId") Long tenantId,
                                 @Param("id") Long id,
                                 @Param("signatureId") Long signatureId,
                                 @Param("contentHash") String contentHash,
                                 @Param("initiatorName") String initiatorName);

    @Update({
            "<script>",
            "UPDATE mes_pro_edhr_deviation",
            "SET status = 'CLOSED', close_reason = 'TRANSFERRED_TO_NCR',",
            "    nonconformance_review_id = #{reviewId}, closed_at = #{closedAt}, version = version + 1",
            "WHERE tenant_id = #{tenantId} AND batch_execution_id = #{batchExecutionId}",
            "  AND status = 'OPEN' AND deleted = 0 AND id IN",
            "<foreach collection='deviationIds' item='deviationId' open='(' separator=',' close=')'>",
            "  #{deviationId}",
            "</foreach>",
            "</script>"
    })
    int closeToNonconformance(@Param("tenantId") Long tenantId,
                              @Param("batchExecutionId") Long batchExecutionId,
                              @Param("deviationIds") java.util.Collection<Long> deviationIds,
                              @Param("reviewId") Long reviewId,
                              @Param("closedAt") java.time.LocalDateTime closedAt);
}
