package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Mapper
public interface MesProcessPoolActiveOrderMapper extends BaseMapperX<MesProcessPoolActiveOrderDO> {

    @Update("UPDATE mes_pro_process_pool_active_order " +
            "SET simulated = #{simulated}, simulation_stage = #{simulationStage}, " +
            "simulation_run_id = #{simulationRunId}, update_time = NOW() " +
            "WHERE id = #{id} AND deleted = 0")
    int updateSimulationMetadata(@Param("id") Long id,
                                 @Param("simulated") Boolean simulated,
                                 @Param("simulationStage") String simulationStage,
                                 @Param("simulationRunId") String simulationRunId);

    default List<MesProcessPoolActiveOrderDO> selectActiveListByLeader(Long leaderUserId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .orderByAsc(MesProcessPoolActiveOrderDO::getSortOrder)
                .orderByAsc(MesProcessPoolActiveOrderDO::getJoinedAt)
                .orderByAsc(MesProcessPoolActiveOrderDO::getId));
    }

    default List<MesProcessPoolActiveOrderDO> selectActiveListByLeaderForUpdate(Long leaderUserId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .orderByAsc(MesProcessPoolActiveOrderDO::getSortOrder)
                .orderByAsc(MesProcessPoolActiveOrderDO::getJoinedAt)
                .orderByAsc(MesProcessPoolActiveOrderDO::getId)
                .last("FOR UPDATE"));
    }

    default MesProcessPoolActiveOrderDO selectLastByLeaderForUpdate(Long leaderUserId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getLeaderUserId, leaderUserId)
                .orderByDesc(MesProcessPoolActiveOrderDO::getSortOrder)
                .orderByDesc(MesProcessPoolActiveOrderDO::getId)
                .last("LIMIT 1 FOR UPDATE"));
    }

    @Update("""
            UPDATE `mes_pro_process_pool_active_order`
            SET `sort_order` = CASE
                    WHEN `id` = #{firstId} THEN #{secondSortOrder}
                    WHEN `id` = #{secondId} THEN #{firstSortOrder}
                END,
                `version` = `version` + 1,
                `updater` = CAST(#{leaderUserId} AS CHAR),
                `update_time` = NOW()
            WHERE `leader_user_id` = #{leaderUserId}
              AND `active_status` = 'ACTIVE'
              AND `deleted` = b'0'
              AND ((`id` = #{firstId} AND `sort_order` = #{firstSortOrder})
                OR (`id` = #{secondId} AND `sort_order` = #{secondSortOrder}))
            """)
    int swapActiveOrderSortOrders(@Param("leaderUserId") Long leaderUserId,
                                  @Param("firstId") Long firstId,
                                  @Param("firstSortOrder") Long firstSortOrder,
                                  @Param("secondId") Long secondId,
                                  @Param("secondSortOrder") Long secondSortOrder);

    default MesProcessPoolActiveOrderDO selectByIdForUpdate(Long activeOrderId) {
        if (activeOrderId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getId, activeOrderId)
                .last("FOR UPDATE"));
    }

    default int markCompleted(Long activeOrderId, Integer expectedVersion, Long leaderUserId) {
        if (activeOrderId == null || expectedVersion == null || leaderUserId == null) {
            return 0;
        }
        return update(null, new LambdaUpdateWrapper<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getId, activeOrderId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .eq(MesProcessPoolActiveOrderDO::getVersion, expectedVersion)
                .set(MesProcessPoolActiveOrderDO::getBusinessStatus, "COMPLETED")
                .set(MesProcessPoolActiveOrderDO::getUpdateTime, LocalDateTime.now())
                .set(MesProcessPoolActiveOrderDO::getUpdater, leaderUserId.toString())
                .setSql("version = version + 1"));
    }

    default List<MesProcessPoolActiveOrderDO> selectListByWorkOrderIdForUpdate(Long workOrderId) {
        if (workOrderId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .orderByAsc(MesProcessPoolActiveOrderDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolActiveOrderDO> selectHistoryByWorkOrderIdForUpdate(Long workOrderId) {
        if (workOrderId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .in(MesProcessPoolActiveOrderDO::getActiveStatus, List.of("ACTIVE", "REMOVED"))
                .orderByAsc(MesProcessPoolActiveOrderDO::getId)
                .last("FOR UPDATE"));
    }

    default List<MesProcessPoolActiveOrderDO> selectHistoryByWorkOrderIds(Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .in(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderIds)
                .in(MesProcessPoolActiveOrderDO::getActiveStatus, List.of("ACTIVE", "REMOVED"))
                .orderByAsc(MesProcessPoolActiveOrderDO::getWorkOrderId)
                .orderByAsc(MesProcessPoolActiveOrderDO::getId));
    }

    default List<MesProcessPoolActiveOrderDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .orderByDesc(MesProcessPoolActiveOrderDO::getJoinedAt)
                .orderByAsc(MesProcessPoolActiveOrderDO::getId));
    }

    default Long selectCountByQaRegulationOrVersionIds(Long qaRegulationId,
                                                       Collection<Long> qaRegulationVersionIds) {
        if (qaRegulationId == null && (qaRegulationVersionIds == null || qaRegulationVersionIds.isEmpty())) {
            return 0L;
        }
        LambdaQueryWrapperX<MesProcessPoolActiveOrderDO> query = new LambdaQueryWrapperX<>();
        query.and(wrapper -> {
            if (qaRegulationId != null) {
                wrapper.eq(MesProcessPoolActiveOrderDO::getQaRegulationId, qaRegulationId);
            }
            if (qaRegulationVersionIds != null && !qaRegulationVersionIds.isEmpty()) {
                if (qaRegulationId != null) {
                    wrapper.or();
                }
                wrapper.in(MesProcessPoolActiveOrderDO::getQaRegulationVersionId, qaRegulationVersionIds);
            }
        });
        return selectCount(query);
    }

    default MesProcessPoolActiveOrderDO selectActiveByWorkOrderAndRoute(Long workOrderId, Long routeId) {
        List<MesProcessPoolActiveOrderDO> activeOrders = selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getRouteId, routeId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .orderByDesc(MesProcessPoolActiveOrderDO::getJoinedAt)
                .orderByDesc(MesProcessPoolActiveOrderDO::getId)
                .last("LIMIT 1"));
        return activeOrders == null || activeOrders.isEmpty() ? null : activeOrders.get(0);
    }

    default MesProcessPoolActiveOrderDO selectActiveByLeaderAndWorkOrderForUpdate(Long leaderUserId,
                                                                                   Long workOrderId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .last("FOR UPDATE"));
    }

    default MesProcessPoolActiveOrderDO selectActiveByWorkOrderRouteVersion(Long workOrderId, Long routeId,
                                                                           Long routeVersionId) {
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrderId)
                .eq(MesProcessPoolActiveOrderDO::getRouteId, routeId)
                .eq(MesProcessPoolActiveOrderDO::getRouteVersionId, routeVersionId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE"));
    }

    default int refreshActiveOrderSnapshot(MesProcessPoolActiveOrderDO snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(snapshot.getId(), "snapshot.id");
        Objects.requireNonNull(snapshot.getLeaderUserId(), "snapshot.leaderUserId");
        Objects.requireNonNull(snapshot.getVersion(), "snapshot.version");
        return update(null, new LambdaUpdateWrapper<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getId, snapshot.getId())
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .eq(MesProcessPoolActiveOrderDO::getVersion, snapshot.getVersion())
                .set(MesProcessPoolActiveOrderDO::getRouteId, snapshot.getRouteId())
                .set(MesProcessPoolActiveOrderDO::getRouteVersionId, snapshot.getRouteVersionId())
                .set(MesProcessPoolActiveOrderDO::getDccProjectCodeId, snapshot.getDccProjectCodeId())
                .set(MesProcessPoolActiveOrderDO::getQaRegulationId, snapshot.getQaRegulationId())
                .set(MesProcessPoolActiveOrderDO::getQaRegulationVersionId, snapshot.getQaRegulationVersionId())
                .set(MesProcessPoolActiveOrderDO::getErpFixedQuantitySnapshot,
                        snapshot.getErpFixedQuantitySnapshot())
                .set(MesProcessPoolActiveOrderDO::getActiveStatus, snapshot.getActiveStatus())
                .set(MesProcessPoolActiveOrderDO::getBusinessStatus, snapshot.getBusinessStatus())
                .set(MesProcessPoolActiveOrderDO::getRemovedAt, snapshot.getRemovedAt())
                .set(MesProcessPoolActiveOrderDO::getUpdateTime, LocalDateTime.now())
                .set(MesProcessPoolActiveOrderDO::getUpdater, snapshot.getLeaderUserId().toString())
                .setSql("version = version + 1"));
    }

    default int reactivateRemovedActiveOrder(Long activeOrderId, Long leaderUserId, Integer version,
                                              LocalDateTime joinedAt, Long sortOrder) {
        return update(null, new LambdaUpdateWrapper<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getId, activeOrderId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "REMOVED")
                .eq(version != null, MesProcessPoolActiveOrderDO::getVersion, version)
                .set(MesProcessPoolActiveOrderDO::getLeaderUserId, leaderUserId)
                .set(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .set(MesProcessPoolActiveOrderDO::getBusinessStatus, "ACTIVE")
                .set(MesProcessPoolActiveOrderDO::getJoinedAt, joinedAt)
                .set(MesProcessPoolActiveOrderDO::getSortOrder, sortOrder)
                .set(MesProcessPoolActiveOrderDO::getRemovedAt, null)
                .setSql("version = version + 1"));
    }

    default int removeActiveOrder(Long activeOrderId, Integer version, LocalDateTime removedAt) {
        return update(null, new LambdaUpdateWrapper<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getId, activeOrderId)
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus, "ACTIVE")
                .eq(version != null, MesProcessPoolActiveOrderDO::getVersion, version)
                .set(MesProcessPoolActiveOrderDO::getActiveStatus, "REMOVED")
                .set(MesProcessPoolActiveOrderDO::getBusinessStatus, "REMOVED")
                .set(MesProcessPoolActiveOrderDO::getRemovedAt, removedAt)
                .setSql("version = version + 1"));
    }

    @Update("""
            UPDATE mes_pro_process_pool_active_order
            SET active_status = 'CLOSED',
                business_status = 'RELEASED',
                release_decision_id = #{releaseDecisionId},
                released_by = #{actorUserId},
                released_at = #{releasedAt},
                version = version + 1,
                updater = CAST(#{actorUserId} AS CHAR),
                update_time = #{releasedAt}
            WHERE id = #{activeOrderId}
              AND deleted = 0
              AND active_status = 'ACTIVE'
              AND version = #{expectedVersion}
            """)
    int closeForRelease(@Param("activeOrderId") Long activeOrderId,
                        @Param("expectedVersion") Integer expectedVersion,
                        @Param("releaseDecisionId") Long releaseDecisionId,
                        @Param("actorUserId") Long actorUserId,
                        @Param("releasedAt") LocalDateTime releasedAt);
}
