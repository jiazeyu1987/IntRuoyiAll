package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderInitializer.*;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderReferenceGraph.*;

/** Supplies the fixed test order's missing local issue evidence inside the completion transaction. */
@Service
@RequiredArgsConstructor
@Slf4j
public class MesFixedTestOrderCompletionSourceInitializer {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void prepare(Long workOrderId, Long actor) {
        Long tenant = TenantContextHolder.getTenantId();
        if (tenant == null || tenant <= 0 || actor == null || actor <= 0 || TenantContextHolder.isIgnore())
            throw blocked("固定测试完工来源初始化需要明确的当前租户和操作人");
        Map<String, Object> order = one(jdbc.queryForList(
                "SELECT id,code FROM mes_pro_work_order WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE",
                tenant, workOrderId), "当前租户完工工单");
        if (!WORK_ORDER_CODE.equals(order.get("code"))) return;
        one(jdbc.queryForList("SELECT id FROM system_tenant WHERE id=? AND deleted=0 FOR UPDATE", tenant),
                "固定测试完工所属租户");
        // Existing local issues remain authoritative and undergo the normal completion validation.
        if (!jdbc.queryForList("SELECT id FROM mes_wm_product_issue WHERE tenant_id=? AND work_order_id=? AND deleted=0 FOR UPDATE",
                tenant, workOrderId).isEmpty()) return;

        MesFixedTestOrderReferenceGraph graph = new MesFixedTestOrderReferenceGraph(jdbc, tenant, actor);
        Object sourceOrderId = one(graph.sourceRows("mes_pro_work_order", "code=?", WORK_ORDER_CODE),
                "参考固定测试工单").get("id");
        List<Map<String, Object>> issues = graph.sourceRows("mes_wm_product_issue", "work_order_id=?", sourceOrderId);
        if (issues.isEmpty()) throw blocked("参考测试工单没有已完成本地领料单");
        for (Map<String, Object> issue : issues) {
            if (number(issue.get("status")) != 4) throw blocked("参考测试本地领料单不是已完成状态");
            if (issue.get("task_id") != null || issue.get("workstation_id") != null)
                throw blocked("参考测试本地领料单限定了其他任务或工作站");
            // Keep the current tenant's work order, product and route; do not import reference definitions.
            graph.plan("mes_wm_product_issue", issue.get("id"), issue, Map.of("work_order_id", workOrderId));
            List<Map<String, Object>> lines = graph.sourceRows("mes_wm_product_issue_line", "issue_id=?", issue.get("id"));
            List<Map<String, Object>> details = graph.sourceRows("mes_wm_product_issue_detail", "issue_id=?", issue.get("id"));
            if (lines.isEmpty() || details.isEmpty()) throw blocked("参考测试本地领料单缺少行或库存批次明细");
            for (Map<String, Object> detail : details) {
                positive(detail, "quantity");
                Map<String, Object> line = one(lines.stream()
                        .filter(candidate -> equalValue(candidate.get("id"), detail.get("line_id"))).toList(),
                        "参考测试领料明细对应行");
                Map<String, Object> stock = one(graph.sourceRows("mes_wm_material_stock", "id=?",
                        detail.get("material_stock_id")), "参考测试领料库存");
                for (Map<String, Object> row : List.of(line, stock)) {
                    if (!equalValue(row.get("item_id"), detail.get("item_id"))
                            || !equalValue(row.get("batch_id"), detail.get("batch_id")))
                        throw blocked("参考测试领料行、库存与明细物料批次不一致");
                }
                Map<String, Object> batch = one(graph.sourceRows("mes_wm_batch", "id=?", detail.get("batch_id")),
                        "参考测试领料批次");
                if (!equalValue(batch.get("work_order_id"), sourceOrderId)
                        || !equalValue(batch.get("item_id"), detail.get("item_id"))
                        || !equalValue(batch.get("code"), detail.get("batch_code")))
                    throw blocked("参考测试领料批次不属于该测试工单或明细身份不一致");
                for (String field : List.of("task_id", "workstation_id", "tool_id", "mold_id")) {
                    Object value = batch.get(field);
                    if (value != null && !equalValue(value, 0L)) throw blocked("参考测试领料批次限定了其他业务：" + field);
                }
                graph.plan("mes_wm_batch", batch.get("id"), batch, Map.of("work_order_id", workOrderId));
                graph.include("mes_wm_product_issue_detail", detail.get("id"));
            }
            for (Map<String, Object> line : lines) {
                positive(line, "quantity");
                if (details.stream().noneMatch(detail -> equalValue(detail.get("line_id"), line.get("id"))))
                    throw blocked("参考测试领料行缺少对应库存明细");
            }
        }
        graph.resolveAll();
        graph.insertAll();
        log.info("Fixed test completion sources staged: tenantId={}, workOrderId={}, issueCount={}, actor={}",
                tenant, workOrderId, issues.size(), actor);
    }

    private static void positive(Map<String, Object> row, String field) {
        if (!(row.get(field) instanceof Number value) || new BigDecimal(value.toString()).signum() <= 0)
            throw blocked("参考测试领料数量无效：" + field);
    }
}
