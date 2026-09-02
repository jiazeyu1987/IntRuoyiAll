package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderVersionUpgradeRequestDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderVersionUpgradeRequestMapper
        extends BaseMapperX<MesProcessPoolActiveOrderVersionUpgradeRequestDO> {

    @Select("""
            SELECT *
            FROM mes_pro_process_pool_active_order_version_upgrade_request
            WHERE id = #{id}
              AND deleted = b'0'
            FOR UPDATE
            """)
    MesProcessPoolActiveOrderVersionUpgradeRequestDO selectByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE mes_pro_process_pool_active_order_version_upgrade_request
            SET approval_process_instance_id = #{processInstanceId},
                result_message = #{resultMessage},
                updater = CAST(#{actorUserId} AS CHAR),
                update_time = #{updatedAt}
            WHERE id = #{id}
              AND deleted = b'0'
              AND request_status = 'PENDING_APPROVAL'
              AND approval_status = 'PENDING'
              AND freeze_status = 'OLD_ORDER_FROZEN'
              AND (approval_process_instance_id IS NULL OR approval_process_instance_id = '')
            """)
    int markApprovalPending(@Param("id") Long id,
                            @Param("processInstanceId") String processInstanceId,
                            @Param("actorUserId") Long actorUserId,
                            @Param("updatedAt") java.time.LocalDateTime updatedAt,
                            @Param("resultMessage") String resultMessage);

    default MesProcessPoolActiveOrderVersionUpgradeRequestDO selectByIdempotencyKey(
            Long sourceActiveOrderId, String idempotencyKey) {
        if (sourceActiveOrderId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderVersionUpgradeRequestDO>()
                .eq(MesProcessPoolActiveOrderVersionUpgradeRequestDO::getSourceActiveOrderId, sourceActiveOrderId)
                .eq(MesProcessPoolActiveOrderVersionUpgradeRequestDO::getIdempotencyKey, idempotencyKey)
                .orderByDesc(MesProcessPoolActiveOrderVersionUpgradeRequestDO::getId)
                .last("LIMIT 1"));
    }

    default MesProcessPoolActiveOrderVersionUpgradeRequestDO selectOngoingBySourceActiveOrderId(
            Long sourceActiveOrderId) {
        if (sourceActiveOrderId == null) {
            return null;
        }
        List<MesProcessPoolActiveOrderVersionUpgradeRequestDO> requests =
                selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderVersionUpgradeRequestDO>()
                        .eq(MesProcessPoolActiveOrderVersionUpgradeRequestDO::getSourceActiveOrderId,
                                sourceActiveOrderId)
                        .in(MesProcessPoolActiveOrderVersionUpgradeRequestDO::getRequestStatus,
                                List.of("PENDING_APPROVAL", "APPROVED"))
                        .orderByDesc(MesProcessPoolActiveOrderVersionUpgradeRequestDO::getId)
                        .last("LIMIT 1"));
        return requests == null || requests.isEmpty() ? null : requests.get(0);
    }

    @Update("""
            UPDATE mes_pro_process_pool_active_order_version_upgrade_request
            SET request_status = 'APPLIED',
                approval_status = 'APPROVED',
                freeze_status = 'APPLIED',
                target_active_order_id = #{targetActiveOrderId},
                target_batch_execution_id = #{targetBatchExecutionId},
                applied_at = #{appliedAt},
                result_message = #{resultMessage},
                updater = CAST(#{actorUserId} AS CHAR),
                update_time = #{appliedAt}
            WHERE id = #{id}
              AND deleted = b'0'
              AND request_status = 'PENDING_APPROVAL'
              AND approval_status = 'PENDING'
            """)
    int markApplied(@Param("id") Long id,
                    @Param("targetActiveOrderId") Long targetActiveOrderId,
                    @Param("targetBatchExecutionId") Long targetBatchExecutionId,
                    @Param("actorUserId") Long actorUserId,
                    @Param("appliedAt") java.time.LocalDateTime appliedAt,
                    @Param("resultMessage") String resultMessage);

    @Update("""
            UPDATE mes_pro_process_pool_active_order_version_upgrade_request
            SET request_status = #{requestStatus},
                approval_status = #{approvalStatus},
                freeze_status = 'RELEASED',
                cancelled_at = #{cancelledAt},
                result_message = #{resultMessage},
                updater = CAST(#{actorUserId} AS CHAR),
                update_time = #{cancelledAt}
            WHERE id = #{id}
              AND deleted = b'0'
              AND request_status = 'PENDING_APPROVAL'
              AND approval_status = 'PENDING'
              AND freeze_status = 'OLD_ORDER_FROZEN'
            """)
    int markRejectedOrCancelled(@Param("id") Long id,
                                @Param("requestStatus") String requestStatus,
                                @Param("approvalStatus") String approvalStatus,
                                @Param("actorUserId") Long actorUserId,
                                @Param("cancelledAt") java.time.LocalDateTime cancelledAt,
                                @Param("resultMessage") String resultMessage);
}
