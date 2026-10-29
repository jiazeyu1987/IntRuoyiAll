package cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MesActiveOrderHandoffTaskMapper extends BaseMapperX<MesActiveOrderHandoffTaskDO> {
    @Select("SELECT h.* FROM mes_active_order_handoff_task h WHERE h.tenant_id=#{tenant} AND h.deleted=FALSE"
            + " AND h.status='TODO' AND h.notification_only=FALSE AND h.task_type IN ('QA_REVIEW','QA_DECISION_HANDOFF')"
            + " AND h.due_time IS NOT NULL AND h.due_time<=#{now} AND h.overdue_notified_at IS NULL"
            + " AND EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order ao WHERE ao.id=h.active_order_id"
            + " AND ao.tenant_id=h.tenant_id AND ao.deleted=FALSE AND ao.active_status='ACTIVE')"
            + " AND (h.task_type='QA_REVIEW' OR EXISTS (SELECT 1 FROM mes_pro_work_order w WHERE w.id=h.work_order_id"
            + " AND w.tenant_id=h.tenant_id AND w.deleted=FALSE AND w.temporary_frozen=FALSE))"
            + " ORDER BY h.due_time,h.id LIMIT #{limit} FOR UPDATE")
    List<MesActiveOrderHandoffTaskDO> due(@Param("tenant") Long tenant, @Param("now") LocalDateTime now, @Param("limit") int limit);
    @Update("UPDATE mes_active_order_handoff_task SET overdue_notified_at=#{now},update_time=#{now}"
            + " WHERE id=#{id} AND tenant_id=#{tenant} AND deleted=FALSE AND status='TODO' AND overdue_notified_at IS NULL")
    int markOverdueNotified(@Param("tenant") Long tenant,@Param("id") Long id,@Param("now") LocalDateTime now);
    default LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO> approvalCenterQuery(
            Long tenant, Long actor, boolean global, boolean done, String keyword) {
        var query = new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>();
        query.eq(MesActiveOrderHandoffTaskDO::getTenantId, tenant)
                .eq(MesActiveOrderHandoffTaskDO::getDeleted, false)
                .apply("CAST(status AS BINARY) = CAST({0} AS BINARY)", done ? "DONE" : "TODO")
                .and(notification -> notification.isNull(MesActiveOrderHandoffTaskDO::getNotificationOnly)
                        .or().eq(MesActiveOrderHandoffTaskDO::getNotificationOnly, false));
        if (!done) {
            query.apply("CAST(task_type AS BINARY) <> CAST('QA_DECISION_HANDOFF' AS BINARY)");
        }
        if (!global) {
            query.apply("FIND_IN_SET({0}, candidate_user_snapshot) > 0", actor);
            if (done) query.eq(MesActiveOrderHandoffTaskDO::getCompletedBy, actor);
        }
        if (!done) query.apply("EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order ao"
                + " WHERE ao.id = mes_active_order_handoff_task.active_order_id"
                + " AND ao.tenant_id = mes_active_order_handoff_task.tenant_id"
                + " AND ao.deleted = 0 AND CAST(ao.active_status AS BINARY) = CAST('ACTIVE' AS BINARY))");
        if (keyword != null && !keyword.isBlank()) {
            String labels = cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.TYPES.stream()
                    .sorted().map(type -> "WHEN '" + type + "' THEN '"
                            + cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.label(type) + "'")
                    .collect(java.util.stream.Collectors.joining(" "));
            // LOCATE treats %, _ and backslashes literally, just like the prior String.contains.
            query.apply("LOCATE(CAST({0} AS BINARY), CAST(CONCAT(CASE task_type " + labels
                    + " END, ' ', COALESCE(CAST(active_order_id AS CHAR), 'null'), ' ',"
                    + " COALESCE(CAST(work_order_id AS CHAR), 'null'), ' ', COALESCE(reason, 'null')) AS BINARY)) > 0",
                    keyword.trim());
        }
        return query;
    }

    default cn.iocoder.yudao.framework.common.pojo.PageResult<MesActiveOrderHandoffTaskDO> selectApprovalCenterPrefix(
            Long tenant, Long actor, boolean global, boolean done, String keyword, long limit) {
        if (limit < 1) throw new IllegalArgumentException("QUERY_WINDOW_RANGE_UNSUPPORTED");
        // A SQL CASE must not silently hide the corruption that label() previously rejected.
        var invalidType = approvalCenterQuery(tenant, actor, global, done, null);
        // Formal CI/PAD SPACE columns must retain the exact Java enum contract, including trailing spaces.
        var types = cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.TYPES
                .stream().sorted().toList();
        String binaryTypes = java.util.stream.IntStream.range(0, types.size())
                .mapToObj(i -> "CAST({" + i + "} AS BINARY)")
                .collect(java.util.stream.Collectors.joining(", "));
        invalidType.and(type -> type.isNull(MesActiveOrderHandoffTaskDO::getTaskType).or()
                .apply("CAST(task_type AS BINARY) NOT IN (" + binaryTypes + ")", types.toArray()));
        cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.require(
                selectCount(invalidType) == 0, "未知交接类型");
        long total = selectCount(approvalCenterQuery(tenant, actor, global, done, keyword));
        var query = approvalCenterQuery(tenant, actor, global, done, keyword);
        String time = done ? "completed_at" : "create_time";
        query.last("ORDER BY " + time + " IS NULL ASC, " + time
                + " DESC, CAST(id AS BINARY) ASC LIMIT " + limit);
        return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(selectList(query), total);
    }

    default MesActiveOrderHandoffTaskDO lock(Long id) {
        return selectOne(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getId,id).last("FOR UPDATE"));
    }
    default MesActiveOrderHandoffTaskDO byIdentity(Long activeId,String type,String source,Long sourceId,Long roundId) {
        return selectOne(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getActiveOrderId,activeId)
                .eq(MesActiveOrderHandoffTaskDO::getTaskType,type).eq(MesActiveOrderHandoffTaskDO::getSourceType,source)
                .eq(MesActiveOrderHandoffTaskDO::getSourceId,sourceId).eq(MesActiveOrderHandoffTaskDO::getRoundId,roundId));
    }
    @Select("SELECT * FROM mes_active_order_handoff_task WHERE tenant_id=#{tenant} AND deleted=FALSE AND FIND_IN_SET(#{actor},candidate_user_snapshot)>0 ORDER BY create_time DESC,id DESC")
    List<MesActiveOrderHandoffTaskDO> own(@Param("tenant") Long tenant,@Param("actor") Long actor);
    @Update("UPDATE mes_active_order_handoff_task SET status=#{status},completed_by=#{actor},completion_source_id=#{source},completed_at=#{at},row_version=row_version+1,update_time=#{at} WHERE tenant_id=#{tenant} AND id=#{id} AND row_version=#{version} AND status='TODO' AND deleted=FALSE")
    int close(@Param("tenant") Long tenant,@Param("id") Long id,@Param("version") Integer version,
              @Param("status") String status,@Param("actor") Long actor,@Param("source") Long source,@Param("at") LocalDateTime at);
}
