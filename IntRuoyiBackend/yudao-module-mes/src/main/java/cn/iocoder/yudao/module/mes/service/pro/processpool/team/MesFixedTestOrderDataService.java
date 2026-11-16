package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.math.BigDecimal;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderInitializer.WORK_ORDER_CODE;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderReferenceGraph.*;

/** Fill missing fixed-order documents; reset separately restores the reference documents. */
@Service
@RequiredArgsConstructor
@Slf4j
public class MesFixedTestOrderDataService {
    @jakarta.annotation.Resource private MesActiveOrderActionLog actionLog;
    static final String HEADER_COLUMNS = "source_form_id,source_fid,source_bill_no,bill_date,document_status,stock_org_number,stock_org_name,production_org_number,production_org_name,owner_number,owner_name,department_number,department_name,description,source_modify_time,last_sync_time,raw_payload";
    static final String ITEM_COLUMNS = "source_form_id,source_fid,source_entry_id,source_line_key,source_bill_no,material_number,material_name,material_specification,unit_name,requested_quantity,actual_quantity,base_actual_quantity,warehouse_number,warehouse_name,stock_location_number,stock_location_name,lot_number,production_order_no,production_order_line_no,production_material_list_no,production_material_list_line_no,workshop_number,workshop_name,stock_status_number,stock_status_name,source_modify_time,last_sync_time,raw_payload";
    static final String MATERIAL_COLUMNS = "source_form_id,source_bill_no,source_entry_id,product_code,production_order_no,production_order_line_no,production_order_status,child_material_code,child_material_name,child_material_specification,child_material_type,numerator,denominator,child_unit_name,drawing_number,required_quantity,issue_method,demand_time,work_order_id,work_order_code,work_order_bom_id,product_id,child_material_id,source_modify_time,last_sync_time,raw_payload";
    private final JdbcTemplate jdbc;
    private final MesFixedTestOrderModeGuard modeGuard;
    private final MesFixedTestOrderInitializer orderInitializer;

    public record DocumentResult(String name, int createdCount, int deletedCount, int skippedCount, int repairedCount) { }
    public record Result(String workOrderCode, List<DocumentResult> documents) { }

    @Transactional(rollbackFor = Exception.class)
    public Result prepare(Long actor) {
        return prepare(actor, false, MesFixedTestOrderDataSet.FIRST);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result prepareSecond(Long actor) {
        return prepare(actor, false, MesFixedTestOrderDataSet.SECOND);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result rebuildForReset(Long actor) {
        return prepare(actor, true, MesFixedTestOrderDataSet.FIRST);
    }

    private Result prepare(Long actor, boolean rebuild, MesFixedTestOrderDataSet dataSet) {
        modeGuard.requireEnabled(actor);
        long tenant = TenantContextHolder.getTenantId();
        if (tenant == SOURCE_TENANT) throw blocked("参考租户只读，不能在参考租户生成测试单据");
        if (jdbc.queryForList("SELECT id FROM system_tenant WHERE id=? AND deleted=0 FOR UPDATE", Long.class, tenant).size() != 1)
            throw blocked("当前租户不存在，不能补齐测试单据");
        var existingOrder = existing("mes_pro_work_order", tenant, Map.of("code", dataSet.workOrderCode()));
        orderInitializer.createWorkOrderIfMissing(actor, dataSet);
        var order = existing("mes_pro_work_order", tenant, Map.of("code", dataSet.workOrderCode()));
        if (order == null) throw blocked("测试生产订单创建后不存在");
        String materialBillNo = secondMaterialBillNo(dataSet);
        // Material documents must exist before the second fixture's issue references are written.
        var materials = prepareMaterials(order, tenant, actor, rebuild, dataSet);
        List<DocumentResult> results = new ArrayList<>();
        results.add(new DocumentResult("生产订单", existingOrder == null ? 1 : 0, 0, existingOrder == null ? 0 : 1, 0));
        results.add(prepareIssues("pick", "领料单明细", tenant, actor, rebuild, dataSet, materialBillNo, order));
        results.add(prepareIssues("replenishment", "补料单明细", tenant, actor, rebuild, dataSet, materialBillNo, order));
        results.add(materials);
        actionLog.recordPreparation(actor, ((Number) required(order, "id")).longValue(), rebuild, results);
        log.info("固定测试工单来源单据准备完成：租户={}，操作人={}，工单={}，结果={}", tenant, actor, dataSet.workOrderCode(), results);
        return new Result(dataSet.workOrderCode(), List.copyOf(results));
    }

    private DocumentResult prepareIssues(String kind, String name, long tenant, long actor, boolean rebuild,
                                        MesFixedTestOrderDataSet dataSet, String materialBillNo, Map<String, Object> order) {
        String headerTable = "erp_kingdee_production_" + kind + "_list";
        String itemTable = headerTable + "_item";
        String fk = "production_" + kind + "_list_id";
        var sourceItems = source(itemTable, "production_order_no", WORK_ORDER_CODE);
        int removedSimulations = "replenishment".equals(kind) && dataSet == MesFixedTestOrderDataSet.FIRST
                ? removeHistoricalReplenishments(tenant, actor, order, sourceItems) : 0;
        List<Map<String, Object>> items = sourceItems.stream()
                .filter(item -> !"replenishment".equals(kind)
                        || !MesFixedTestOrderReplenishmentPolicy.isHistoricalCopy(item))
                .map(dataSet::document).toList();
        if (items.isEmpty()) {
            if ("replenishment".equals(kind)) return new DocumentResult(name, 0, removedSimulations, 0, 0);
            throw blocked("参考租户缺少固定测试订单的" + name);
        }
        // Validate the complete source before writing; existing documents stay untouched in fill mode.
        Map<Object, Map<String, Object>> headers = new LinkedHashMap<>();
        Map<Object, Object> headerIds = new LinkedHashMap<>();
        Map<Object, Object> itemIds = new LinkedHashMap<>();
        Set<Object> existingHeaderIds = new HashSet<>();
        Set<Object> existingItemIds = new HashSet<>();
        int repaired = 0;
        for (var item : items) {
            var header = dataSet.document(single(source(headerTable, "id", required(item, fk)), name + "表头"));
            if (!"C".equals(header.get("document_status"))
                    || !equalValue(header.get("source_form_id"), item.get("source_form_id"))
                    || !equalValue(header.get("source_bill_no"), item.get("source_bill_no")))
                throw blocked("参考" + name + "未审核或表头关联不一致");
            headers.put(header.get("id"), header);
            var localHeader = existing(headerTable, tenant, Map.of("source_form_id", required(header, "source_form_id"),
                    "source_fid", required(header, "source_fid")));
            headerIds.put(header.get("id"), localHeader == null ? IdUtil.getSnowflakeNextId() : localHeader.get("id"));
            if (localHeader != null) existingHeaderIds.add(localHeader.get("id"));
            var localItem = existing(itemTable, tenant, Map.of("source_line_key", required(item, "source_line_key")));
            if (localItem != null && (!dataSet.workOrderCode().equals(localItem.get("production_order_no"))
                    || dataSet == MesFixedTestOrderDataSet.SECOND
                    && !equalValue(localItem.get(fk), headerIds.get(header.get("id")))))
                throw blocked(name + "身份已用于其他订单");
            itemIds.put(item.get("id"), localItem == null ? IdUtil.getSnowflakeNextId() : localItem.get("id"));
            if (localItem != null) existingItemIds.add(localItem.get("id"));
            if (dataSet == MesFixedTestOrderDataSet.SECOND) {
                if (localItem != null && repairSecondMaterialReference(itemTable, localItem, item,
                        materialBillNo, order, tenant, actor)) repaired++;
                item.put("production_material_list_no", materialBillNo);
            }
        }
        var oldHeaderIds = rebuild ? jdbc.queryForList("SELECT DISTINCT " + fk + " FROM " + itemTable
                + " WHERE tenant_id=? AND production_order_no=?", Long.class, tenant, dataSet.workOrderCode()) : List.of();
        int deleted = rebuild ? jdbc.update("DELETE FROM " + itemTable + " WHERE tenant_id=? AND production_order_no=?", tenant, dataSet.workOrderCode()) : 0;
        // Reuse local identities so existing formal references remain attached to the rebuilt rows.
        for (var entry : headers.entrySet()) {
            Object localId = headerIds.get(entry.getKey());
            if (!rebuild && existingHeaderIds.contains(localId)) continue;
            if (rebuild) jdbc.update("DELETE FROM " + headerTable + " WHERE tenant_id=? AND id=?", tenant, localId);
            insert(headerTable, HEADER_COLUMNS, entry.getValue(), Map.of(), localId, tenant, actor, dataSet);
        }
        for (Object oldId : oldHeaderIds) {
            if (headerIds.containsValue(oldId)) continue;
            jdbc.update("DELETE FROM " + headerTable + " WHERE tenant_id=? AND id=? AND NOT EXISTS (SELECT 1 FROM "
                    + itemTable + " WHERE tenant_id=? AND " + fk + "=?)", tenant, oldId, tenant, oldId);
        }
        int created = 0;
        int skipped = 0;
        for (var item : items) {
            if (!rebuild && existingItemIds.contains(itemIds.get(item.get("id")))) {
                skipped++;
                continue;
            }
            insert(itemTable, fk + "," + ITEM_COLUMNS, item, Map.of(fk, headerIds.get(item.get(fk))),
                    itemIds.get(item.get("id")), tenant, actor, dataSet);
            created++;
        }
        return new DocumentResult(name, created, deleted + removedSimulations, skipped - repaired, repaired);
    }

    private int removeHistoricalReplenishments(long tenant, long actor, Map<String, Object> order,
                                               List<Map<String, Object>> sourceItems) {
        var copies = sourceItems.stream().filter(MesFixedTestOrderReplenishmentPolicy::isHistoricalCopy).toList();
        if (copies.isEmpty()) return 0;
        List<Map<String, Object>> localCopies = new ArrayList<>();
        for (var sourceItem : copies) {
            var local = existing("erp_kingdee_production_replenishment_list_item", tenant,
                    Map.of("source_line_key", required(sourceItem, "source_line_key")));
            if (local == null) continue;
            if (!Objects.equals(local.get("production_order_no"), order.get("code"))
                    || !MesFixedTestOrderReplenishmentPolicy.isHistoricalCopy(local)
                    || !equalValue(local.get("material_number"), sourceItem.get("material_number"))
                    || !equalValue(local.get("actual_quantity"), sourceItem.get("actual_quantity")))
                throw blocked("历史模拟补料已变更，不能自动移除");
            localCopies.add(local);
        }
        if (localCopies.isEmpty()) return 0;
        var activeOrders = jdbc.queryForList("SELECT id,active_status FROM mes_pro_process_pool_active_order"
                + " WHERE tenant_id=? AND work_order_id=? AND deleted=0 FOR UPDATE", tenant, required(order, "id"));
        for (var active : activeOrders) {
            Object activeId = required(active, "id");
            if (!"ACTIVE".equals(active.get("active_status"))
                    || !jdbc.queryForList("SELECT id FROM mes_pro_process_pool_active_order_completion_receipt"
                    + " WHERE tenant_id=? AND active_order_id=? AND deleted=0", tenant, activeId).isEmpty()
                    || !jdbc.queryForList("SELECT id FROM mes_pro_edhr_operation_audit_event"
                    + " WHERE tenant_id=? AND object_type='ACTIVE_ORDER_REPLENISHMENT' AND object_id=? AND deleted=0",
                    tenant, activeId.toString()).isEmpty())
                throw blocked("订单已有完工或签名补料证据，不能移除历史模拟补料");
        }
        actionLog.recordPreparation(actor, ((Number) required(order, "id")).longValue(), false,
                Map.of("reason", "移除旧工具复制的模拟补料，固定测试订单没有该补料需求", "removedItems", localCopies));
        for (var item : localCopies) {
            int changed = jdbc.update("UPDATE erp_kingdee_production_replenishment_list_item SET deleted=1,updater=?"
                    + " WHERE id=? AND tenant_id=? AND deleted=0", Long.toString(actor), required(item, "id"), tenant);
            if (changed != 1) throw blocked("历史模拟补料移除失败");
            Object headerId = required(item, "production_replenishment_list_id");
            jdbc.update("UPDATE erp_kingdee_production_replenishment_list SET deleted=1,updater=?"
                    + " WHERE id=? AND tenant_id=? AND deleted=0 AND NOT EXISTS"
                    + " (SELECT 1 FROM erp_kingdee_production_replenishment_list_item WHERE tenant_id=?"
                    + " AND production_replenishment_list_id=? AND deleted=0)",
                    Long.toString(actor), headerId, tenant, tenant, headerId);
        }
        return localCopies.size();
    }

    private DocumentResult prepareMaterials(Map<String, Object> order, long tenant, long actor, boolean rebuild,
                                           MesFixedTestOrderDataSet dataSet) {
        String table = "mes_kingdee_production_material_list";
        var rows = source(table, "production_order_no", WORK_ORDER_CODE).stream().map(dataSet::document).toList();
        if (rows.isEmpty()) throw blocked("参考租户缺少固定测试订单的生产用料清单");
        Map<Object, Object> rowIds = new LinkedHashMap<>();
        Set<Object> existingIds = new HashSet<>();
        for (var row : rows) {
            var present = existing(table, tenant, Map.of("source_form_id", required(row, "source_form_id"),
                    "source_bill_no", required(row, "source_bill_no"), "source_entry_id", required(row, "source_entry_id")));
            if (present != null && (!dataSet.workOrderCode().equals(present.get("production_order_no"))
                    || dataSet == MesFixedTestOrderDataSet.SECOND
                    && (!equalValue(order.get("id"), present.get("work_order_id"))
                    || !dataSet.workOrderCode().equals(present.get("work_order_code")))))
                throw blocked("生产用料清单身份已用于其他订单");
            rowIds.put(row.get("id"), present == null ? IdUtil.getSnowflakeNextId() : present.get("id"));
            if (present != null) existingIds.add(present.get("id"));
        }
        int deleted = rebuild ? jdbc.update("DELETE FROM " + table + " WHERE tenant_id=? AND production_order_no=?", tenant, dataSet.workOrderCode()) : 0;
        int created = 0;
        int skipped = 0;
        for (var row : rows) {
            if (!rebuild && existingIds.contains(rowIds.get(row.get("id")))) {
                skipped++;
                continue;
            }
            var graph = new MesFixedTestOrderReferenceGraph(jdbc, tenant, actor);
            Map<String, Object> overrides = new LinkedHashMap<>();
            overrides.put("work_order_id", order.get("id"));
            overrides.put("work_order_code", dataSet.workOrderCode());
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
            insert(table, MATERIAL_COLUMNS, row, overrides, rowIds.get(row.get("id")), tenant, actor, dataSet);
            created++;
        }
        return new DocumentResult("生产用料清单明细", created, deleted, skipped, 0);
    }

    private String secondMaterialBillNo(MesFixedTestOrderDataSet dataSet) {
        if (dataSet != MesFixedTestOrderDataSet.SECOND) return null;
        var bills = source("mes_kingdee_production_material_list", "production_order_no", WORK_ORDER_CODE).stream()
                .map(row -> required(row, "source_bill_no").toString()).distinct().toList();
        if (bills.size() != 1) throw blocked("第二套测试订单必须有且仅有一份明确的来源用料清单，不能推断领补料关联");
        return dataSet.document(Map.of("source_bill_no", bills.get(0))).get("source_bill_no").toString();
    }

    /** Correct only the known old generator output, before it becomes production evidence. */
    private boolean repairSecondMaterialReference(String table, Map<String, Object> local, Map<String, Object> expected,
                                                   String materialBillNo, Map<String, Object> order, long tenant, long actor) {
        Object previous = local.get("production_material_list_no");
        if (Objects.equals(previous, materialBillNo)) return false;
        if (!Objects.equals(previous, expected.get("production_material_list_no")))
            throw blocked("第二套领补料用料单关联已被修改，不能自动覆盖");
        var raw = JsonUtils.parseTree(Objects.toString(local.get("raw_payload"), ""));
        if (!(raw instanceof ObjectNode snapshot) || raw.path("testDataSet").asInt() != 2
                || !MesFixedTestOrderDataSet.SECOND.workOrderCode().equals(raw.path("workOrderCode").asText())
                || raw.path("reference").path("tenantId").asLong() != SOURCE_TENANT
                || !Objects.toString(expected.get("id")).equals(raw.path("reference").path("recordId").asText())
                || !(raw.path("document") instanceof ObjectNode document)
                || !Objects.toString(local.get("source_line_key")).equals(document.path("source_line_key").asText())
                || !MesFixedTestOrderDataSet.SECOND.workOrderCode().equals(document.path("production_order_no").asText())
                || !Objects.equals(Objects.toString(previous, null), document.path("production_material_list_no").asText(null)))
            throw blocked("第二套领补料缺少本工具生成的来源凭据，不能自动修复");
        if (new BigDecimal(required(order, "quantity_produced").toString()).signum() != 0
                || !jdbc.queryForList("SELECT id FROM mes_pro_process_pool_active_order WHERE tenant_id=? AND work_order_id=? AND deleted=0",
                Long.class, tenant, required(order, "id")).isEmpty())
            throw blocked("第二套订单已进入生产，不能自动更改领补料用料单关联");
        document.put("production_material_list_no", materialBillNo);
        ObjectNode repair = snapshot.putObject("materialListReferenceRepair");
        if (previous == null) repair.putNull("previous"); else repair.put("previous", previous.toString());
        repair.put("corrected", materialBillNo);
        repair.put("actor", actor);
        if (jdbc.update("UPDATE " + table + " SET production_material_list_no=?,raw_payload=?,updater=?"
                        + " WHERE id=? AND tenant_id=? AND production_order_no=? AND source_line_key=? AND deleted=0",
                materialBillNo, snapshot.toString(), String.valueOf(actor), required(local, "id"), tenant,
                MesFixedTestOrderDataSet.SECOND.workOrderCode(), required(local, "source_line_key")) != 1)
            throw blocked("第二套领补料用料单关联修复失败");
        return true;
    }

    private void insert(String table, String columns, Map<String, Object> source, Map<String, Object> overrides,
                        Object id, long tenant, long actor, MesFixedTestOrderDataSet dataSet) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        for (String field : columns.split(",")) {
            if (!source.containsKey(field)) throw blocked("测试单据缺少字段：" + table + "/" + field);
            values.put(field, overrides.containsKey(field) ? overrides.get(field) : source.get(field));
        }
        values.put("tenant_id", tenant); values.put("deleted", false);
        values.put("creator", String.valueOf(actor)); values.put("updater", String.valueOf(actor));
        if (dataSet == MesFixedTestOrderDataSet.SECOND) values.put("raw_payload", dataSet.snapshot(values, source));
        if (jdbc.update("INSERT INTO " + table + " (" + String.join(",", values.keySet()) + ") VALUES ("
                + String.join(",", Collections.nCopies(values.size(), "?")) + ")", values.values().toArray()) != 1)
            throw blocked("测试单据写入失败：" + table);
    }

    private Map<String, Object> existing(String table, long tenant, Map<String, Object> key) {
        List<Object> args = new ArrayList<>(); args.add(tenant);
        StringJoiner where = new StringJoiner(" AND ");
        key.forEach((field, value) -> { where.add(field + "=?"); args.add(value); });
        // The tenant row serializes fixture preparation. Do not gap-lock missing document
        // identities: ERP synchronization inserts unrelated headers and lines concurrently.
        String query = "SELECT * FROM " + table + " WHERE tenant_id=? AND deleted=0 AND " + where;
        var rows = jdbc.queryForList(query, args.toArray());
        if (rows.size() > 1) throw blocked("当前租户测试单据身份不唯一：" + table);
        if (rows.isEmpty()) return null;
        // Lock the exact existing row by primary key and recheck its business identity.
        List<Object> lockedArgs = new ArrayList<>();
        lockedArgs.add(required(rows.get(0), "id"));
        lockedArgs.addAll(args);
        var locked = jdbc.queryForList("SELECT * FROM " + table + " WHERE id=? AND tenant_id=? AND deleted=0 AND "
                + where + " FOR UPDATE", lockedArgs.toArray());
        if (locked.size() != 1) throw blocked("当前租户测试单据已变化，请核对后重新操作：" + table);
        return locked.get(0);
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
