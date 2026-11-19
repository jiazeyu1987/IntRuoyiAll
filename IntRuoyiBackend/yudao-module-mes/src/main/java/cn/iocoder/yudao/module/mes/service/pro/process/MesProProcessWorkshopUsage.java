package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.fastjson.JSONObject;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 工序实际使用车间，供解析复用和统一替换共同展示。 */
public final class MesProProcessWorkshopUsage {
    private MesProProcessWorkshopUsage() {}

    public static Map<Long, List<JSONObject>> load(NamedParameterJdbcTemplate jdbc, List<Long> processIds) {
        if (processIds.isEmpty()) return Map.of();
        // 一个共享工序可在多个车间使用，按路线节点实际绑定的工作站查询。
        List<JSONObject> rows = jdbc.query("""
                SELECT DISTINCT rp.process_id, w.id, w.name
                FROM mes_pro_route_process rp
                JOIN mes_pro_route r ON r.id=rp.route_id AND r.tenant_id=rp.tenant_id AND r.deleted=0
                JOIN mes_md_workstation s ON s.id=rp.workstation_id AND s.tenant_id=rp.tenant_id AND s.deleted=0
                JOIN mes_md_workshop w ON w.id=s.workshop_id AND w.tenant_id=rp.tenant_id AND w.deleted=0
                WHERE rp.tenant_id=:tenantId AND rp.deleted=0 AND rp.process_id IN (:processIds)
                ORDER BY rp.process_id,w.id
                """, Map.of("tenantId", TenantContextHolder.getRequiredTenantId(), "processIds", processIds), (rs, index) -> {
            JSONObject row = new JSONObject(true);
            row.put("processId", rs.getLong("process_id")); row.put("id", rs.getLong("id"));
            row.put("name", rs.getString("name")); return row;
        });
        return rows.stream().collect(Collectors.groupingBy(row -> row.getLong("processId")));
    }
}
