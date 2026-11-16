package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.ibatis.annotations.Mapper;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MesMarketReleaseHandoverMapper extends BaseMapperX<MesMarketReleaseHandoverMapper.Row> {
    static <T> LambdaQueryWrapperX<T> readable(LambdaQueryWrapperX<T> query,String column) {
        Long actor=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        if(actor==null) return query;
        Long tenant=cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        query.apply("("+column+" IS NULL OR "+column+" NOT IN (SELECT h_prior.batch_execution_id "
                +"FROM mes_pro_edhr_market_release_handover h_prior JOIN mes_pro_edhr_market_release_handover h_current "
                +"ON h_current.tenant_id=h_prior.tenant_id AND h_current.batch_execution_id=h_prior.batch_execution_id "
                +"WHERE h_prior.tenant_id={1} AND h_prior.recipient_user_id={0} AND h_prior.deleted=0 AND h_current.deleted=0 "
                +"AND h_current.id=(SELECT MAX(h_latest.id) FROM mes_pro_edhr_market_release_handover h_latest "
                +"WHERE h_latest.tenant_id=h_current.tenant_id AND h_latest.batch_execution_id=h_current.batch_execution_id AND h_latest.deleted=0) "
                +"AND h_current.original_user_id<>{0} AND (h_current.recalled_at IS NOT NULL OR h_current.recipient_user_id<>{0})))",actor,tenant);
        return query;
    }
    default List<Row> history(Long batchId) {
        return selectList(new LambdaQueryWrapperX<Row>().eq(Row::getBatchExecutionId, batchId).orderByDesc(Row::getId));
    }
    default List<Row> historyForUpdate(Long batchId) {
        return selectList(new LambdaQueryWrapperX<Row>().eq(Row::getBatchExecutionId,batchId).orderByDesc(Row::getId).last("FOR UPDATE"));
    }
    default boolean hasActorHistory(Long actor) {
        return actor!=null && selectCount(new LambdaQueryWrapperX<Row>().and(q -> q.eq(Row::getOriginalUserId,actor).or().eq(Row::getRecipientUserId,actor)))>0;
    }
    default List<Row> activeOrderHistory(Long activeOrderId) {
        return selectList(new LambdaQueryWrapperX<Row>().eq(Row::getActiveOrderId, activeOrderId).orderByDesc(Row::getId));
    }
    default Row command(String key) {
        return selectOne(new LambdaQueryWrapperX<Row>().eq(Row::getCommandKey,key));
    }
    default List<AccessRow> selectAccessByBatchIds(java.util.Collection<Long> batchIds, Long actor) {
        if (batchIds.isEmpty()) return List.of();
        return selectAccessByBatchIdsInternal(batchIds, actor,
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
    }

    @org.apache.ibatis.annotations.Select("""
            <script>
            SELECT h.id, h.batch_execution_id, h.original_user_id, h.original_user_name,
                   h.recipient_user_id, h.recipient_user_name, h.recalled_at, CASE WHEN EXISTS (SELECT 1 FROM mes_pro_edhr_market_release_handover h_prior
                WHERE h_prior.tenant_id = h.tenant_id AND h_prior.deleted = 0
                  AND h_prior.batch_execution_id = h.batch_execution_id
                  <choose><when test="actor != null">AND h_prior.recipient_user_id = #{actor}</when>
                  <otherwise>AND h_prior.recipient_user_id IS NULL</otherwise></choose>) THEN 1 ELSE 0 END AS actor_recipient
            FROM mes_pro_edhr_market_release_handover h
            WHERE h.tenant_id = #{tenantId} AND h.deleted = 0
              AND h.batch_execution_id IN
              <foreach collection="batchIds" item="id" open="(" separator="," close=")">#{id}</foreach>
              AND h.id = (SELECT MAX(latest.id) FROM mes_pro_edhr_market_release_handover latest
                  WHERE latest.tenant_id = h.tenant_id AND latest.deleted = 0
                    AND latest.batch_execution_id = h.batch_execution_id)
            </script>
            """)
    List<AccessRow> selectAccessByBatchIdsInternal(
            @org.apache.ibatis.annotations.Param("batchIds") java.util.Collection<Long> batchIds,
            @org.apache.ibatis.annotations.Param("actor") Long actor,
            @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);

    @Data
    @EqualsAndHashCode(callSuper=true)
    class AccessRow extends Row {
        private boolean actorRecipient;
    }

    @Data
    @EqualsAndHashCode(callSuper=true)
    @Accessors(chain=true)
    @TableName("mes_pro_edhr_market_release_handover")
    class Row extends TenantBaseDO {
        @TableId private Long id;
        private Long batchExecutionId;
        private Long activeOrderId;
        private Long releaseTransactionId;
        private Long workTaskId;
        private Long originalUserId;
        private String originalUserName;
        private Long recipientUserId;
        private String recipientUserName;
        private String originalCandidateSnapshot;
        private String commandKey;
        private String reason;
        private LocalDateTime handedAt;
        private LocalDateTime recalledAt;
        private String recallReason;
        private String recallCommandKey;
    }
}
