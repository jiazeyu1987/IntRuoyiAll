package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderInitializer.WORK_ORDER_CODE;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderReferenceGraph.*;

/** Rebuild the fixed order's three material document types from tenant 1 in one transaction. */
@Service
@RequiredArgsConstructor
@Slf4j
public class MesFixedTestOrderDataService {
    static final String HEADER_COLUMNS = "source_form_id,source_fid,source_bill_no,bill_date,document_status,stock_org_number,stock_org_name,production_org_number,production_org_name,owner_number,owner_name,department_number,department_name,description,source_modify_time,last_sync_time,raw_payload";
    static final String ITEM_COLUMNS = "source_form_id,source_fid,source_entry_id,source_line_key,source_bill_no,material_number,material_name,material_specification,unit_name,requested_quantity,actual_quantity,base_actual_quantity,warehouse_number,warehouse_name,stock_location_number,stock_location_name,lot_number,production_order_no,production_order_line_no,production_material_list_no,production_material_list_line_no,workshop_number,workshop_name,stock_status_number,stock_status_name,source_modify_time,last_sync_time,raw_payload";
    static final String MATERIAL_COLUMNS = "source_form_id,source_bill_no,source_entry_id,product_code,production_order_no,production_order_line_no,production_order_status,child_material_code,child_material_name,child_material_specification,child_material_type,numerator,denominator,child_unit_name,drawing_number,required_quantity,issue_method,demand_time,work_order_id,work_order_code,work_order_bom_id,product_id,child_material_id,source_modify_time,last_sync_time,raw_payload";
    private final JdbcTemplate jdbc;
    private final MesFixedTestOrderModeGuard modeGuard;
    private final MesFixedTestOrderInitializer orderInitializer;

    public record DocumentResult(String name, int createdCount, int deletedCount) { }
    public record Result(String workOrderCode, List<DocumentResult> documents) { }

    @Transactional(rollbackFor = Exception.class)
    public Result prepare(Long actor) {
        modeGuard.requireEnabled(actor);
        long tenant = TenantContextHolder.getTenantId();
        if (jdbc.queryForList("SELECT id FROM system_tenant WHERE id=? AND deleted=0 FOR UPDATE", Long.class, tenant).size() != 1)
            throw blocked("当前租户不存在，不能补齐测试单据");
        var existingOrder = existing("mes_pro_work_order", tenant, Map.of("code", WORK_ORDER_CODE));
        orderInitializer.createWorkOrderIfMissing(actor);
        var order = existing("mes_pro_work_order", tenant, Map.of("code", WORK_ORDER_CODE));
        if (order == null) throw blocked("测试生产订单创建后不存在");
        List<DocumentResult> results = new ArrayList<>();
        results.add(new DocumentResult("生产订单", existingOrder == null ? 1 : 0, 0));
        results.add(prepareIssues("pick", "领料单明细", tenant, actor));
        results.add(prepareIssues("replenishment", "补料单明细", tenant, actor));
        results.add(prepareMaterials(order, tenant, actor));
        log.info("Fixed test documents prepared: tenant={}, actor={}, order={}, result={}", tenant, actor, WORK_ORDER_CODE, results);
        return new Result(WORK_ORDER_CODE, List.copyOf(results));
    }

    private DocumentResult prepareIssues(String kind, String name, long tenant, long actor) {
        String headerTable = "erp_kingdee_production_" + kind + "_list";
        String itemTable = headerTable + "_item";
        String fk = "production_" + kind + "_list_id";
        List<Map<String, Object>> items = source(itemTable, "production_order_no", WORK_ORDER_CODE);
        if (items.isEmpty()) throw blocked("参考租户缺少固定测试订单的" + name);
        // Capture the complete source before deleting anything, including when tenant 1 runs this action.
        Map<Object, Map<String, Object>> headers = new LinkedHashMap<>();
        Map<Object, Object> headerIds = new LinkedHashMap<>();
        Map<Object, Object> itemIds = new LinkedHashMap<>();
        for (var item : items) {
            var header = single(source(headerTable, "id", required(item, fk)), name + "表头");
            if (!"C".equals(header.get("document_status"))
                    || !equalValue(header.get("source_form_id"), item.get("source_form_id"))
                    || !equalValue(header.get("source_bill_no"), item.get("source_bill_no")))
                throw blocked("参考" + name + "未审核或表头关联不一致");
            headers.put(header.get("id"), header);
            var localHeader = existing(headerTable, tenant, Map.of("source_form_id", required(header, "source_form_id"),
                    "source_fid", required(header, "source_fid")));
            headerIds.put(header.get("id"), localHeader == null ? IdUtil.getSnowflakeNextId() : localHeader.get("id"));
            var localItem = existing(itemTable, tenant, Map.of("source_line_key", required(item, "source_line_key")));
            if (localItem != null && !WORK_ORDER_CODE.equals(localItem.get("production_order_no")))
                throw blocked(name + "身份已用于其他订单");
            itemIds.put(item.get("id"), localItem == null ? IdUtil.getSnowflakeNextId() : localItem.get("id"));
        }
        var oldHeaderIds = jdbc.queryForList("SELECT DISTINCT " + fk + " FROM " + itemTable
                + " WHERE tenant_id=? AND production_order_no=?", Long.class, tenant, WORK_ORDER_CODE);
        int deleted = jdbc.update("DELETE FROM " + itemTable + " WHERE tenant_id=? AND production_order_no=?", tenant, WORK_ORDER_CODE);
        // Reuse local identities so existing formal references remain attached to the rebuilt rows.
        for (var entry : headers.entrySet()) {
            Object localId = headerIds.get(entry.getKey());
            jdbc.update("DELETE FROM " + headerTable + " WHERE tenant_id=? AND id=?", tenant, localId);
            insert(headerTable, HEADER_COLUMNS, entry.getValue(), Map.of(), localId, tenant, actor);
        }
        for (Object oldId : oldHeaderIds) {
            if (headerIds.containsValue(oldId)) continue;
            jdbc.update("DELETE FROM " + headerTable + " WHERE tenant_id=? AND id=? AND NOT EXISTS (SELECT 1 FROM "
                    + itemTable + " WHERE tenant_id=? AND " + fk + "=?)", tenant, oldId, tenant, oldId);
        }
        int created = 0;
        for (var item : items) {
            insert(itemTable, fk + "," + ITEM_COLUMNS, item, Map.of(fk, headerIds.get(item.get(fk))),
                    itemIds.get(item.get("id")), tenant, actor);
            created++;
        }
        return new DocumentResult(name, created, deleted);
    }

    private DocumentResult prepareMaterials(Map<String, Object> order, long tenant, long actor) {
        String table = "mes_kingdee_production_material_list";
        var rows = source(table, "production_order_no", WORK_ORDER_CODE);
        if (rows.isEmpty()) throw blocked("参考租户缺少固定测试订单的生产用料清单");
        Map<Object, Object> rowIds = new LinkedHashMap<>();
        for (var row : rows) {
            var present = existing(table, tenant, Map.of("source_form_id", required(row, "source_form_id"),
                    "source_bill_no", required(row, "source_bill_no"), "source_entry_id", required(row, "source_entry_id")));
            if (present != null && !WORK_ORDER_CODE.equals(present.get("production_order_no")))
                throw blocked("生产用料清单身份已用于其他订单");
            rowIds.put(row.get("id"), present == null ? IdUtil.getSnowflakeNextId() : present.get("id"));
        }
        int deleted = jdbc.update("DELETE FROM " + table + " WHERE tenant_id=? AND production_order_no=?", tenant, WORK_ORDER_CODE);
        int created = 0;
        for (var row : rows) {
            var graph = new MesFixedTestOrderReferenceGraph(jdbc, tenant, actor);
            Map<String, Object> overrides = new LinkedHashMap<>();
            overrides.put("work_order_id", order.get("id"));
            overrides.put("work_order_code", WORK_ORDER_CODE);
            for (String field : List.of("product_id", "child_material_id"))
                overrides.put(field, orderInitializer.orderReference(graph, tenant, "mes_md_item", row.get(field)));
            Object sourceBomId = row.get("work_order_bom_id");
            if (sourceBomId != null && ((Number) sourceBomId).longValue() > 0) {
                var bom = single(graph.sourceRows("mes_pro_work_order_bom", "id=?", sourceBomId), "用料清单关联 BOM");
                var sourceOrder = single(source("mes_pro_work_order", "id", bom.get("work_order_id")), "BOM 生产订单");
                if (!WORK_ORDER_CODE.equals(sourceOrder.get("code"))) throw blocked("用料清单 BOM 不属于固定测试订单");
                Object itemId = orderInitializer.orderReference(graph, tenant, "mes_md_item", bom.get("item_id"));
                var localBom = existing("mes_pro_work_order_bom", tenant, Map.of("work_order_id", order.get("id"), "item_id", itemId));
                if (localBom == null) {
                    graph.plan("mes_pro_work_order_bom", sourceBomId, bom, Map.of("work_order_id", order.get("id"), "item_id", itemId));
                    overrides.put("work_order_bom_id", graph.id("mes_pro_work_order_bom", sourceBomId));
                } else overrides.put("work_order_bom_id", localBom.get("id"));
            }
            graph.insertAll();
            insert(table, MATERIAL_COLUMNS, row, overrides, rowIds.get(row.get("id")), tenant, actor);
            created++;
        }
        return new DocumentResult("生产用料清单明细", created, deleted);
    }

    private void insert(String table, String columns, Map<String, Object> source, Map<String, Object> overrides,
                        Object id, long tenant, long actor) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        for (String field : columns.split(",")) {
            if (!source.containsKey(field)) throw blocked("测试单据缺少字段：" + table + "/" + field);
            values.put(field, overrides.containsKey(field) ? overrides.get(field) : source.get(field));
        }
        values.put("tenant_id", tenant); values.put("deleted", false);
        values.put("creator", String.valueOf(actor)); values.put("updater", String.valueOf(actor));
        if (jdbc.update("INSERT INTO " + table + " (" + String.join(",", values.keySet()) + ") VALUES ("
                + String.join(",", Collections.nCopies(values.size(), "?")) + ")", values.values().toArray()) != 1)
            throw blocked("测试单据写入失败：" + table);
    }

    private Map<String, Object> existing(String table, long tenant, Map<String, Object> key) {
        List<Object> args = new ArrayList<>(); args.add(tenant);
        StringJoiner where = new StringJoiner(" AND ");
        key.forEach((field, value) -> { where.add(field + "=?"); args.add(value); });
        var rows = jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=? AND deleted=0 AND " + where + " FOR UPDATE", args.toArray());
        if (rows.size() > 1) throw blocked("当前租户测试单据身份不唯一：" + table);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> source(String table, String field, Object value) {
        return jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=? AND deleted=0 AND " + field + "=? ORDER BY id", SOURCE_TENANT, value);
    }

    private static Map<String, Object> single(List<Map<String, Object>> rows, String name) {
        if (rows.size() != 1) throw blocked("参考" + name + "缺失或不唯一");
        return rows.get(0);
    }

    private static Object required(Map<String, Object> row, String field) {
        Object value = row.get(field);
        if (value == null || value.toString().isBlank()) throw blocked("测试单据身份字段为空：" + field);
        return value;
    }
}
