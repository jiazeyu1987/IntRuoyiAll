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
