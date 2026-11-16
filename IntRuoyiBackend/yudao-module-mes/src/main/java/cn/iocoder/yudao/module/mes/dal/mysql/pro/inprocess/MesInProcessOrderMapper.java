package cn.iocoder.yudao.module.mes.dal.mysql.pro.inprocess;

import cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess.vo.MesInProcessOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

@Mapper
public interface MesInProcessOrderMapper {
    /** Only open tasks bound to this exact execution/application, never role membership guesses. */
    @Select("""
        <script>
        <choose>
          <when test="businessStatus == 'VERSION_UPGRADE_PENDING'">
            SELECT DISTINCT COALESCE(NULLIF(t.ASSIGNEE_,''), il.USER_ID_, membership.USER_ID_)
            FROM mes_pro_process_pool_active_order_version_upgrade_request r
            JOIN ACT_RU_TASK t ON t.PROC_INST_ID_=r.approval_process_instance_id
              AND t.TENANT_ID_=CAST(#{tenantId} AS CHAR) AND t.SUSPENSION_STATE_=1
            LEFT JOIN ACT_RU_IDENTITYLINK il ON il.TASK_ID_=t.ID_ AND il.TYPE_='candidate'
            LEFT JOIN ACT_ID_MEMBERSHIP membership ON membership.GROUP_ID_=il.GROUP_ID_
            WHERE r.tenant_id=#{tenantId} AND r.deleted=0 AND r.source_active_order_id=#{activeOrderId}
          </when>
          <when test="releaseStatus != null and releaseStatus != ''">
            SELECT CASE WHEN t.status='DOING' AND t.assignee_user_id IS NOT NULL
                        THEN CAST(t.assignee_user_id AS CHAR) ELSE t.candidate_user_snapshot END
            FROM mes_pro_edhr_work_task t
            JOIN mes_pro_process_pool_active_order_release_application r
              ON r.id=#{applicationId} AND r.tenant_id=t.tenant_id AND r.deleted=0
                AND r.active_order_id=#{activeOrderId} AND r.work_order_id=t.work_order_id
            WHERE t.tenant_id=#{tenantId} AND t.deleted=0 AND t.status IN ('TODO','DOING','OVERDUE')
              AND (
                (#{releaseStatus}='PQC_RELEASE_PENDING' AND t.id=r.pqc_release_work_task_id)
                OR (#{releaseStatus}='MANAGER_RELEASE_PENDING' AND t.id=r.release_approval_work_task_id)
                OR (#{releaseStatus}='REPORT_UPLOAD_PENDING' AND t.business_scope_type='ACTIVE_ORDER_DOSSIER'
                  AND t.business_scope_id=r.active_order_id AND t.review_source_id=r.id)
                OR (#{releaseStatus} IN ('PQC_RELEASE_REJECTED','MARKET_RELEASE_REJECTED','MARKET_RELEASE_WITHDRAWN')
                  AND t.business_scope_type='RELEASE_APPLICATION' AND t.business_scope_id=r.id)
              )
          </when>
          <otherwise>
            SELECT h.candidate_user_snapshot FROM mes_active_order_handoff_task h
            WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.active_order_id=#{activeOrderId}
              AND h.status='TODO' AND h.notification_only=0
              AND h.task_type IN ('PRODUCTION_HANDOFF','PRODUCTION_REVIEW','PRODUCTION_RETURN',
                                  'PQC_HANDOFF','PQC_REVIEW','PQC_RETURN','QA_DECISION_HANDOFF')
          </otherwise>
        </choose>
        </script>
        """)
    List<String> selectResponsibilityCandidates(@Param("tenantId") Long tenantId,
            @Param("activeOrderId") Long activeOrderId, @Param("applicationId") Long applicationId,
            @Param("releaseStatus") String releaseStatus, @Param("businessStatus") String businessStatus);

    Map<String, Field> STATIC_FIELDS = Map.of(
            "workOrderCode", text("w.code"), "productName", text("p.name"),
            "productCode", text("p.code"), "batchCode", text("w.batch_code"),
            "leaderName", text("leader.nickname"), "quantity", number("w.quantity"),
            "joinedAt", dateTime("a.joined_at"));

    String FROM_AND_FILTERS = """
        FROM mes_pro_process_pool_active_order a
        LEFT JOIN mes_pro_work_order w ON w.id=a.work_order_id AND w.tenant_id=a.tenant_id AND w.deleted=0
        LEFT JOIN mes_md_item p ON p.id=w.product_id AND p.tenant_id=a.tenant_id AND p.deleted=0
        LEFT JOIN system_users leader ON leader.id=a.leader_user_id AND leader.tenant_id=a.tenant_id AND leader.deleted=0
        WHERE a.tenant_id=#{tenantId} AND a.deleted=0
          AND a.active_status='ACTIVE' AND a.business_status IN ('ACTIVE','COMPLETED','VERSION_UPGRADE_PENDING')
          AND NOT EXISTS (
            SELECT 1 FROM mes_pro_edhr_nonconformance_review n
            WHERE n.tenant_id=a.tenant_id AND n.active_order_id=a.id AND n.work_order_id=a.work_order_id
              AND n.deleted=0 AND n.review_status='closed' AND n.disposition='void'
          )
        <if test="q.workOrderCode != null and q.workOrderCode.trim() != ''">
          AND LOCATE(TRIM(#{q.workOrderCode}),w.code)>0
        </if>
        <if test="q.productName != null and q.productName.trim() != ''">
          AND (LOCATE(TRIM(#{q.productName}),p.name)>0 OR LOCATE(TRIM(#{q.productName}),w.name)>0)
        </if>
        <if test="q.batchCode != null and q.batchCode.trim() != ''">
          AND LOCATE(TRIM(#{q.batchCode}),w.batch_code)>0
        </if>
        <if test="ew != null and !ew.emptyOfNormal"> AND (${ew.sqlSegment}) </if>
        """;

    @Select("<script>SELECT COUNT(*) " + FROM_AND_FILTERS + "</script>")
    long countCandidates(@Param("tenantId") Long tenantId, @Param("q") MesInProcessOrderPageReqVO request,
                         @Param("ew") QueryWrapper<Object> filters);

    /** An ordinary SQL page, or a fixed-size keyset window for live equipment/progress validation. */
    @Select("<script>SELECT a.* " + FROM_AND_FILTERS + """
        <if test="cursorId != null">
          <choose>
            <when test="cursorJoinedAt != null">
              AND (a.joined_at &lt; #{cursorJoinedAt} OR (a.joined_at=#{cursorJoinedAt} AND a.id &lt; #{cursorId}) OR a.joined_at IS NULL)
            </when>
            <otherwise>AND a.joined_at IS NULL AND a.id &lt; #{cursorId}</otherwise>
          </choose>
        </if>
        ORDER BY a.joined_at DESC,a.id DESC LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<MesProcessPoolActiveOrderDO> selectCandidates(@Param("tenantId") Long tenantId,
            @Param("q") MesInProcessOrderPageReqVO request, @Param("ew") QueryWrapper<Object> filters,
            @Param("cursorJoinedAt") LocalDateTime cursorJoinedAt, @Param("cursorId") Long cursorId,
            @Param("offset") long offset, @Param("limit") int limit);
}
