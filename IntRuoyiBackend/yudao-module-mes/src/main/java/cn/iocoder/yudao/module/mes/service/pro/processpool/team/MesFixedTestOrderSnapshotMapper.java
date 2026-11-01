package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderReferenceGraph.*;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderInitializer.value;

/** Typed JSON references only: an unknown non-empty identity stops the import before any write. */
final class MesFixedTestOrderSnapshotMapper {
    private static final Logger log = LoggerFactory.getLogger(MesFixedTestOrderSnapshotMapper.class);
    private static final Map<String, String> REFERENCES = Map.ofEntries(
            Map.entry("routeId", "mes_pro_route"), Map.entry("routeVersionId", "mes_pro_route_version"),
            Map.entry("routeProcessId", "mes_pro_route_process"), Map.entry("sourceRouteProcessId", "mes_pro_route_process"),
            Map.entry("targetRouteProcessId", "mes_pro_route_process"), Map.entry("processId", "mes_pro_process"),
            Map.entry("routeProcessWorkstationId", "mes_md_workstation"), Map.entry("craftId", "mes_pro_craft"),
            Map.entry("deviceId", "mes_dv_machinery"), Map.entry("equipmentId", "mes_dv_machinery"),
            Map.entry("itemId", "mes_md_item"), Map.entry("dccProjectCodeId", "dcc_project_code"),
            Map.entry("regulationId", "mes_qa_inspection_regulation"),
            Map.entry("routeFlowConfigId", "mes_pro_route_flow_config"),
            Map.entry("batchRecordDefinitionId", "mes_pro_batch_record_definition"),
            Map.entry("batchRecordVersionId", "mes_pro_batch_record_version"),
            Map.entry("batchRecordReportId", "jimu_report"), Map.entry("routeBindingId", "mes_pro_route_flow_process_batch_record"),
            Map.entry("productionLineId", "mes_pro_route")); // Published start-owner metadata uses the route identity.
    private static final Map<String, String> ARRAYS = Map.of(
            "inputMaterialIds", "mes_md_item", "outputMaterialIds", "mes_md_item",
            "craftDeviceIds", "mes_dv_machinery", "deviceIds", "mes_dv_machinery");
    private static final Set<String> AUDIT = Set.of("creator", "updater", "createTime", "updateTime", "deleted");
    private final MesFixedTestOrderReferenceGraph graph;
    private final JdbcTemplate jdbc;
    private final long tenant;
    private final Map<Object, Entry> scopes = new LinkedHashMap<>();
    private final Map<Object, Entry> repairedBindingScopes = new LinkedHashMap<>();

    MesFixedTestOrderSnapshotMapper(MesFixedTestOrderReferenceGraph graph, JdbcTemplate jdbc, long tenant) {
        this.graph = graph; this.jdbc = jdbc; this.tenant = tenant;
    }

    void collect(JsonNode node) { visit(node, "", false); }

    /** Authorized repair of the fixed fixture's known dangling scopes; never changes reference data. */
    void repairFixedOrderPermissions(ObjectNode snapshot, long sharedLeader) {
        for (JsonNode config : snapshot.path("configSnapshots").path("batchUseConfigs")) {
            for (JsonNode report : config.path("batchRecordReports")) {
                JsonNode scopeId = report.get("permissionScopeId");
                if (scopeId == null || !scopeId.isIntegralNumber()
                        || scopeId.longValue() < 9908090475L || scopeId.longValue() > 9908090489L) continue;
                long oldScope = scopeId.longValue();
                if (!graph.sourceRows("mes_pro_edhr_permission_scope", "id=?", oldScope).isEmpty()) continue;
                if (!graph.sourceRows("mes_pro_edhr_permission_rule", "scope_id=?", oldScope).isEmpty())
                    throw blocked("失效批记录权限范围仍有孤立规则，不能覆盖：" + oldScope);
                Entry binding = graph.entry("mes_pro_route_flow_process_batch_record", value(report.get("routeBindingId")));
                if (binding == null || !equalValue(binding.source.get("permission_scope_id"), oldScope)
                        || !equalValue(binding.source.get("route_id"), value(snapshot.get("id")))
                        || !equalValue(binding.source.get("route_process_id"), value(config.get("routeProcessId")))
                        || !equalValue(binding.source.get("batch_record_report_id"), value(report.get("batchRecordReportId")))
                        || !"MAIN".equals(binding.source.get("form_slot_type"))
                        || !"PRODUCTION".equals(binding.source.get("owner_role_key")))
                    throw blocked("固定测试批记录权限修复对象不一致：" + oldScope);
                Object subject = graph.id("@user", sharedLeader);
                String key = "FIXED_TEST_SCOPE_" + binding.key.id();
                Map<String, Object> definition = new LinkedHashMap<>();
                definition.put("scope_name", "固定测试批记录权限修复");
                definition.put("object_type", "ROUTE_PROCESS_BATCH_RECORD");
                definition.put("object_id", binding.source.get("route_process_id") + "|" + binding.source.get("batch_record_report_id"));
                definition.put("parent_scope_id", null); definition.put("status", "ENABLED"); definition.put("version", 1);
                Entry scope = graph.plan("mes_pro_edhr_permission_scope", key, definition);
                scopes.put(key, scope);
                if (repairedBindingScopes.putIfAbsent(binding.key.id(), scope) != null)
                    throw blocked("固定测试批记录权限修复绑定重复：" + binding.key.id());
                // Matches new formal bindings' default abilities; signature and approval remain separate.
                for (String ability : List.of("VIEW", "FILL")) {
                    Map<String, Object> rule = new LinkedHashMap<>();
                    rule.put("scope_id", key); rule.put("subject_type", "USER"); rule.put("subject_id", sharedLeader);
                    rule.put("ability", ability); rule.put("decision", "ALLOW"); rule.put("priority", 10);
                    rule.put("effective_from", null); rule.put("effective_to", null);
                    rule.put("status", "ENABLED"); rule.put("version", 1);
                    graph.plan("mes_pro_edhr_permission_rule", key + "_" + ability, rule).overrides.put("subject_id", subject);
                }
                log.info("Planned fixed test permission repair: tenant={}, binding={}, missingScope={}, subject={}, abilities=VIEW,FILL",
                        tenant, binding.key.id(), oldScope, subject);
            }
        }
    }

    void prepareScopes() {
        for (Entry scope : scopes.values()) {
            String[] identity = String.valueOf(scope.source.get("object_id")).split("\\|", -1);
            String object = graph.id("mes_pro_route_process", Long.valueOf(identity[0])) + "|" + graph.id("jimu_report", identity[1]);
            scope.overrides.put("object_id", object);
            scope.overrides.put("scope_name", (repairedBindingScopes.containsValue(scope) ? "固定测试批记录权限修复-" : "固定测试批记录-") + object);
        }
    }

    void remap(JsonNode node) {
        visit(node, "", true);
        for (JsonNode config : node.path("configSnapshots").path("productionProcessConfigs")) refreshCraftFingerprints((ObjectNode) config);
    }

    private void visit(JsonNode node, String context, boolean remap) {
        if (node.isArray()) {
            node.forEach(child -> visit(child, context, remap));
            return;
        }
        if (!node.isObject()) return;
        ObjectNode object = (ObjectNode) node;
        Entry repairedScope = object.hasNonNull("routeBindingId")
                ? repairedBindingScopes.get(value(object.get("routeBindingId"))) : null;
        Entry craft = remap && object.hasNonNull("craftId")
                ? graph.entry("mes_pro_craft", value(object.get("craftId"))) : null;
        List<String> fields = new ArrayList<>(); object.fieldNames().forEachRemaining(fields::add);
        for (String field : fields) {
            JsonNode child = object.get(field);
            if (AUDIT.contains(field)) { if (remap) object.remove(field); continue; }
            if (field.equals("scheduleConfigs")) {
                if (!child.isObject()) throw blocked("参考排程配置必须按工序节点索引");
                ObjectNode renamed = object.objectNode();
                Iterator<Map.Entry<String, JsonNode>> rows = child.fields();
                while (rows.hasNext()) {
                    var row = rows.next();
                    Long rp;
                    try { rp = Long.valueOf(row.getKey()); }
                    catch (NumberFormatException ex) { throw blocked("参考排程工序身份无效：" + row.getKey()); }
                    reference("mes_pro_route_process", rp, remap);
                    visit(row.getValue(), field, remap);
                    if (remap) renamed.set(String.valueOf(graph.id("mes_pro_route_process", rp)), row.getValue());
                }
                if (remap) object.set(field, renamed);
                continue;
            }
            if (field.equals("permissionScopeId") && present(child)) {
                if (repairedScope != null) {
                    if (remap) object.set(field, object.numberNode(resolveScope(repairedScope.key.id())));
                } else if (remap) object.set(field, object.numberNode(resolveScope(value(child))));
                else collectScope(value(child));
                continue;
            }
            if (field.equals("candidateSourceIds")) {
                String type = object.path("candidateSourceType").asText();
                String table = switch (type) { case "USERS", "USER" -> "@user"; case "ROLE" -> "@role";
                    default -> throw blocked("参考候选来源类型不支持：" + type); };
                mapArray(object, field, table, remap);
                continue;
            }
            if (ARRAYS.containsKey(field)) { mapArray(object, field, ARRAYS.get(field), remap); continue; }
            String table = REFERENCES.get(field);
            if (field.equals("id")) table = switch (context) {
                case "" -> object.has("configSnapshots") ? "mes_pro_route" : null;
                case "products" -> "mes_pro_route_product";
                case "batchUseConfigs", "scheduleUseConfigs" -> "mes_pro_route_flow_process_config";
                case "scheduleConfigs" -> "mes_pro_route_schedule_config";
                case "lossReasons" -> "mes_pro_route_process_loss_reason";
                default -> null;
            };
            if (table != null && present(child)) {
                Object mapped = reference(table, value(child), remap);
                if (remap) object.set(field, identityNode(mapped));
            } else if ((field.equals("id") || field.endsWith("Id") || field.endsWith("Ids")) && present(child)) {
                throw blocked("参考快照包含未声明的身份：" + context + "/" + field);
            } else if (field.equals("deviceGroupKey") && remap) {
                // This is a logical group key, not an external permission identity.
                object.put(field, "FIXED-" + UUID.nameUUIDFromBytes((tenant + ":" + child.asText()).getBytes(StandardCharsets.UTF_8)));
            } else if (field.equals("remark") && remap) object.put(field, MARKER);
            else visit(child, field, remap);
        }
        if (craft != null) object.put("craftCode", String.valueOf(graph.mappedField(craft, "code")));
        if (remap && object.has("routeBindingId")) {
            Entry binding = graph.entries("mes_pro_route_flow_process_batch_record").stream()
                    .filter(e -> Objects.equals(graph.id(e.key.table(), e.key.id()), value(object.get("routeBindingId"))))
                    .findFirst().orElseThrow(() -> blocked("重映射批记录绑定不存在"));
            object.put("recordCategorySnapshotHash", bindingHash(binding, false));
            object.put("slotConfigSnapshotHash", bindingHash(binding, true));
        }
    }

    private Object reference(String table, Object id, boolean remap) {
        if (table.startsWith("@")) return graph.id(table, id);
        if (!remap) { graph.include(table, id); return id; }
        return graph.id(table, id);
    }

    private void mapArray(ObjectNode object, String field, String table, boolean remap) {
        JsonNode values = object.get(field);
        if (!values.isArray()) throw blocked("参考快照身份列表格式无效：" + field);
        ArrayNode mapped = object.arrayNode();
        for (JsonNode id : values) {
            if (!present(id) || !id.isValueNode()) throw blocked("参考快照身份列表包含无效值：" + field);
            mapped.add(identityNode(reference(table, value(id), remap)));
        }
        if (remap) object.set(field, mapped);
    }

    private void collectScope(Object id) {
        if (scopes.containsKey(id)) return;
        List<Map<String, Object>> rows = graph.sourceRows("mes_pro_edhr_permission_scope", "id=?", id);
        if (rows.size() != 1) throw blocked("参考批记录权限范围不存在或不唯一：" + id + "；请先修复参考正式绑定");
        Map<String, Object> row = rows.get(0);
        if (!"ROUTE_PROCESS_BATCH_RECORD".equals(row.get("object_type"))) throw blocked("参考批记录权限范围对象类型不支持：" + id);
        if (!"ENABLED".equals(row.get("status")) || row.get("parent_scope_id") != null && !equalValue(row.get("parent_scope_id"), 0L))
            throw blocked("参考批记录权限范围未启用或包含范围外的父级权限：" + id);
        String[] identity = String.valueOf(row.get("object_id")).split("\\|", -1);
        if (identity.length != 2 || identity[0].isBlank() || identity[1].isBlank()) throw blocked("参考批记录权限范围对象身份无效：" + id);
        try { graph.include("mes_pro_route_process", Long.valueOf(identity[0])); }
        catch (NumberFormatException ex) { throw blocked("参考批记录权限范围工序身份无效：" + id); }
        graph.include("jimu_report", identity[1]);
        Entry scope = graph.include("mes_pro_edhr_permission_scope", id);
        scopes.put(id, scope);
        List<Map<String, Object>> rules = graph.sourceRows("mes_pro_edhr_permission_rule", "scope_id=?", id);
        if (rules.isEmpty()) throw blocked("参考批记录权限范围没有明确规则：" + id);
        for (Map<String, Object> rule : rules) {
            String principal = switch (String.valueOf(rule.get("subject_type"))) {
                case "USER" -> "@user"; case "ROLE" -> "@role";
                default -> throw blocked("参考批记录权限主体类型不支持：" + rule.get("subject_type"));
            };
            Entry planned = graph.include("mes_pro_edhr_permission_rule", rule.get("id"));
            planned.overrides.put("subject_id", graph.id(principal, rule.get("subject_id")));
        }
    }

    private long resolveScope(Object id) {
        Entry scope = scopes.get(id);
        if (scope == null) throw blocked("未校验的批记录权限范围：" + id);
        return ((Number) graph.id("mes_pro_edhr_permission_scope", id)).longValue();
    }

    void remapBindings() {
        for (Entry scope : repairedBindingScopes.values()) {
            if (scope.existing == null) continue;
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_edhr_permission_rule WHERE tenant_id=? AND scope_id=? AND deleted=0",
                    Integer.class, tenant, graph.id("mes_pro_edhr_permission_scope", scope.key.id()));
            boolean complete = graph.entries("mes_pro_edhr_permission_rule").stream()
                    .filter(rule -> Objects.equals(rule.source.get("scope_id"), scope.key.id()))
                    .allMatch(rule -> rule.existing != null);
            if (!Objects.equals(count, 2) || !complete)
                throw blocked("当前租户已有测试批记录权限不同，保留原规则：" + scope.key.id());
        }
        for (Entry binding : graph.entries("mes_pro_route_flow_process_batch_record")) {
            Object scope = binding.source.get("permission_scope_id");
            if (scope != null && !equalValue(scope, 0L)) {
                Entry source = repairedBindingScopes.getOrDefault(binding.key.id(), scopes.get(scope));
                String object = binding.source.get("route_process_id") + "|" + binding.source.get("batch_record_report_id");
                if (source == null || !object.equals(source.source.get("object_id"))) throw blocked("参考批记录权限范围与绑定对象不一致：" + scope);
                binding.overrides.put("permission_scope_id", resolveScope(source.key.id()));
            }
            binding.overrides.put("record_category_snapshot_hash", bindingHash(binding, false));
            binding.overrides.put("slot_config_snapshot_hash", bindingHash(binding, true));
        }
    }

    private String bindingHash(Entry binding, boolean slot) {
        Object enabled = binding.source.get("recordbook_enabled");
        if (!(enabled instanceof Boolean) && !(enabled instanceof Number)) throw blocked("参考批记录缺少明确的记录本开关");
        String flag = String.valueOf(Boolean.TRUE.equals(enabled) || equalValue(enabled, 1));
        List<String> fields = slot
                ? List.of("route_id", "route_process_id", "batch_record_report_id", "form_slot_type", "record_category", "validation_profile",
                    "recordbook_enabled", "permission_scope_id", "required_policy", "required_condition_json", "owner_role_key", "archive_visibility",
                    "report_sort", "instance_scope", "shared_form_key", "fillable_scope_json")
                : List.of("route_id", "route_process_id", "batch_record_report_id", "record_category", "validation_profile", "recordbook_enabled", "permission_scope_id", "report_sort");
        return DigestUtil.sha256Hex(String.join("|", fields.stream().map(field -> {
            if (field.equals("recordbook_enabled")) return flag;
            Object value = graph.mappedField(binding, field);
            return value == null ? "" : value.toString();
        }).toList()));
    }

    private void refreshCraftFingerprints(ObjectNode config) {
        JSONObject json = JSON.parseObject(config.toString());
        List<MesProCraftParameters.Extracted> schemes = MesProCraftParameters.extractSchemes(json);
        List<ObjectNode> bindings = new ArrayList<>();
        if (config.hasNonNull("craftId")) bindings.add(config);
        for (JsonNode extra : config.path("additionalCraftBindings")) bindings.add((ObjectNode) extra);
        for (ObjectNode binding : bindings) {
            Set<Long> devices = new HashSet<>(); binding.path("craftDeviceIds").forEach(id -> devices.add(id.asLong()));
            List<MesProCraftParameters.Extracted> matches = schemes.stream().filter(s -> new HashSet<>(s.deviceIds()).equals(devices)).toList();
            if (matches.size() != 1) throw blocked("正式工艺与设备参数方案不一致");
            binding.put("craftFingerprint", MesProCraftParameters.fingerprint(config.path("processId").longValue(), matches.get(0).parameters()));
        }
    }

    private static boolean present(JsonNode node) {
        return node != null && !node.isNull() && !(node.isArray() && node.isEmpty())
                && !(node.isTextual() && (node.asText().isEmpty() || node.asText().equals("0")))
                && !(node.isNumber() && node.asLong() == 0);
    }

    private static JsonNode identityNode(Object value) {
        if (value instanceof Number number) return JsonNodeFactory.instance.numberNode(number.longValue());
        if (value instanceof String text) return JsonNodeFactory.instance.textNode(text);
        throw blocked("重映射身份类型无效：" + value);
    }
}
