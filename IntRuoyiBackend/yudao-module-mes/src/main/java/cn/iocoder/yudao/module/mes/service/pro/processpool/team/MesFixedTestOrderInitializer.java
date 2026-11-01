package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderReferenceGraph.*;

/** Imports only the fixed order's definitions and actual material sources, in the reset transaction. */
@Service
@RequiredArgsConstructor
public class MesFixedTestOrderInitializer {
    static final String WORK_ORDER_CODE = "SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891";
    private final JdbcTemplate jdbc;
    private final MesProRouteSnapshotCanonicalizer canonicalizer;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void initialize(Long actor) {
        Long tenant = TenantContextHolder.getTenantId();
        if (tenant == null || tenant <= 0 || actor == null || actor <= 0 || TenantContextHolder.isIgnore())
            throw blocked("固定测试订单初始化需要明确的当前租户和操作人");
        // The tenant row exists even before the fixed order. It serializes missing-row initialization.
        if (jdbc.queryForList("SELECT id FROM system_tenant WHERE id=? AND deleted=0 FOR UPDATE", Long.class, tenant).size() != 1)
            throw blocked("当前租户不存在，不能初始化测试订单");
        if (tenant == SOURCE_TENANT) return;
        MesFixedTestOrderReferenceGraph graph = new MesFixedTestOrderReferenceGraph(jdbc, tenant, actor);
        Map<String, Object> sourceOrder = one(graph.sourceRows("mes_pro_work_order", "code=?", WORK_ORDER_CODE), "固定测试工单");
        Entry order = graph.include("mes_pro_work_order", sourceOrder.get("id"));
        requireEmpty(order, "parent_id");
        order.overrides.put("status", MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
        order.overrides.put("quantity_produced", java.math.BigDecimal.ZERO);
        order.overrides.put("quantity_scheduled", java.math.BigDecimal.ZERO);
        order.overrides.put("temporary_frozen", false);
        graph.children("mes_pro_work_order_bom", "work_order_id", order.key.id());

        Map<String, Object> routeProduct = one(graph.sourceRows("mes_pro_route_product", "item_id=?", sourceOrder.get("product_id")), "产品正式路线");
        Entry product = graph.include("mes_pro_route_product", routeProduct.get("id"));
        Object routeId = routeProduct.get("route_id");
        Map<String, Object> routeVersion = one(graph.sourceRows("mes_pro_route_version", "route_id=? AND active=1 AND lifecycle_status='ACTIVE'", routeId), "已发布路线版本");
        Entry version = graph.include("mes_pro_route_version", routeVersion.get("id"));
        String sourceJson = (String) routeVersion.get("route_snapshot_json");
        if (!canonicalizer.validate(number(routeId), sourceJson).ready()
                || !canonicalizer.sha256(sourceJson).equals(routeVersion.get("route_snapshot_sha256")))
            throw blocked("参考正式路线快照校验失败");
        ObjectNode snapshot = (ObjectNode) graph.parse(sourceJson);
        ObjectNode configs = (ObjectNode) snapshot.path("configSnapshots");
        // Other products sharing the reference route are outside this fixed fixture.
        ArrayNode products = (ArrayNode) configs.path("products");
        List<JsonNode> matching = new ArrayList<>();
        products.forEach(p -> { if (p.path("itemId").asLong() == number(sourceOrder.get("product_id"))) matching.add(p); });
        if (matching.size() != 1) throw blocked("正式路线快照缺少唯一测试产品");
        products.removeAll().add(matching.get(0));
        graph.alias("mes_pro_route_product", value(matching.get(0).get("id")), product);
        requireFormalFields(product, matching.get(0), Map.of("quantity", "quantity", "productionTime", "production_time", "timeUnitType", "time_unit_type"));
        requireEmptyArray(configs, "productBoms");
        prepareRoute(graph, configs, routeId, routeVersion.get("id"));
        long sharedLeader = replaceStartLeaders(configs, tenant);
        repairKnownDeviceReferences(graph, configs);
        prepareCrafts(graph, configs);

        Entry binding = graph.include("mes_pro_route_dcc_project_binding", one(graph.sourceRows(
                "mes_pro_route_dcc_project_binding", "route_id=?", routeId), "路线DCC绑定").get("id"));
        Object dccId = binding.source.get("dcc_project_code_id");
        Object productMaster = graph.include("mes_md_item", sourceOrder.get("product_id")).source.get("product_master_id");
        Entry dcc = graph.include("dcc_project_code", dccId);
        if (!equalValue(productMaster, dcc.source.get("product_master_id"))) throw blocked("参考产品与DCC产品身份不一致");
        Map<String, Object> regulation = one(graph.sourceRows("mes_qa_inspection_regulation",
                "dcc_project_code_id=? AND owner_module='MES_QA' AND lifecycle_status='PUBLISHED'", dccId), "产品QA规程");
        prepareProductQa(graph, regulation, productMaster, dccId, tenant);
        prepareCommonQaBinding(graph, productMaster, dccId, tenant);
        prepareQaAndReports(graph);
        prepareMaterials(graph, order.key.id());
        MesFixedTestOrderSnapshotMapper mapper = new MesFixedTestOrderSnapshotMapper(graph, jdbc, tenant);
        mapper.repairFixedOrderPermissions(snapshot, sharedLeader);
        mapper.collect(snapshot);
        // QA equipment and all other definition JSON dependencies are collected before identities resolve.
        for (Entry qa : graph.entries("mes_qa_inspection_regulation_version")) mapper.collect(graph.parse((String) qa.source.get("snapshot_json")));
        Map<Entry, JsonNode> reportJson = new LinkedHashMap<>();
        for (Entry report : graph.entries("jimu_report")) {
            JsonNode json = graph.parse((String) report.source.get("json_str"));
            requireStaticReport(json); mapper.collect(json); reportJson.put(report, json);
        }
        Map<String, JsonNode> dccJson = new LinkedHashMap<>();
        for (String field : List.of("default_attributes_json", "batch_record_total_recognition_json")) {
            String raw = (String) dcc.source.get(field);
            if (raw != null && !raw.isBlank()) {
                JsonNode json = graph.parse(raw); mapper.collect(json); dccJson.put(field, json);
            }
        }
        prepareReports(graph, tenant);
        for (Entry common : graph.entries("mes_qa_common_regulation_product_binding"))
            common.overrides.put("active_binding_key", String.valueOf(graph.mappedField(common, "product_id")));
        mapper.prepareScopes();
        graph.resolveAll();
        for (Entry craft : graph.entries("mes_pro_craft")) {
            craft.overrides.put("fingerprint", MesProCraftParameters.fingerprint(number(graph.mappedField(craft, "process_id")),
                    JSON.parseArray((String) graph.mappedField(craft, "parameters_json"))));
        }
        mapper.remapBindings();
        mapper.remap(snapshot);
        String canonical = canonicalizer.canonicalize(snapshot.toString());
        if (!canonicalizer.validate(number(graph.id("mes_pro_route", routeId)), canonical).ready())
            throw blocked("重映射后的正式路线快照校验失败");
        version.overrides.put("route_snapshot_json", canonical);
        version.overrides.put("route_snapshot_sha256", canonicalizer.sha256(canonical));
        for (Entry qa : graph.entries("mes_qa_inspection_regulation_version")) {
            JsonNode json = graph.parse((String) qa.source.get("snapshot_json"));
            mapper.remap(json);
            qa.overrides.put("snapshot_json", canonicalizer.canonicalize(json.toString()));
        }
        dccJson.forEach((field, json) -> { mapper.remap(json); dcc.overrides.put(field, canonicalizer.canonicalize(json.toString())); });
        reportJson.forEach((report, json) -> { mapper.remap(json); report.overrides.put("json_str", canonicalizer.canonicalize(json.toString())); });
        for (Entry report : graph.entries("mes_pro_batch_record_report")) {
            Entry template = graph.entry("jimu_report", report.source.get("report_id"));
            report.overrides.put("report_code", template.overrides.get("code"));
            report.overrides.put("sample_key", "FIXED_TEST_TN" + tenant + "_VERSION_" + graph.mappedField(report, "batch_record_version_id"));
        }
        graph.insertAll();
    }

    private void prepareProductQa(MesFixedTestOrderReferenceGraph graph, Map<String, Object> reference,
                                  Object product, Object dcc, long tenant) {
        List<Map<String, Object>> targets = jdbc.queryForList("SELECT * FROM mes_qa_inspection_regulation WHERE tenant_id=? AND deleted=0 AND dcc_project_code_id=? AND owner_module='MES_QA' AND lifecycle_status='PUBLISHED' FOR UPDATE", tenant, graph.id("dcc_project_code", dcc));
        if (targets.isEmpty()) {
            graph.include("mes_qa_inspection_regulation", reference.get("id"));
            return;
        }
        Map<String, Object> target = one(targets, "当前产品DCC已发布QA规程");
        if (target.get("product_id") != null && !equalValue(target.get("product_id"), graph.id("mdm_product", product)))
            throw blocked("当前DCC已发布QA规程不属于目标产品，保留原规程");
        for (var field : Map.of("route_id", "mes_pro_route", "route_version_id", "mes_pro_route_version",
                "route_process_id", "mes_pro_route_process", "process_id", "mes_pro_process").entrySet()) {
            Object source = reference.get(field.getKey());
            Object expected = source == null ? null : graph.id(field.getValue(), source);
            if (!equalValue(expected, target.get(field.getKey())))
                throw blocked("当前产品QA的路线或工序适用范围不同，保留原规程：" + field.getKey());
        }
        validateTargetQaVersion(graph, tenant, target.get("id"), target.get("current_version_id"), "MES_QA", true);
    }

    /** The enabled binding of this product is authoritative in its tenant, regardless of catalog codes. */
    private void prepareCommonQaBinding(MesFixedTestOrderReferenceGraph graph, Object product, Object dcc, long tenant) {
        List<Map<String, Object>> references = graph.sourceRows("mes_qa_common_regulation_product_binding",
                "product_id=? AND binding_status='ENABLED'", product);
        if (references.isEmpty()) return;
        Map<String, Object> reference = one(references, "参考产品启用的通用QA绑定");
        Object targetProduct = graph.id("mdm_product", product);
        List<Map<String, Object>> targets = jdbc.queryForList("SELECT * FROM mes_qa_common_regulation_product_binding WHERE tenant_id=? AND deleted=0 AND product_id=? AND binding_status='ENABLED' FOR UPDATE", tenant, targetProduct);
        if (targets.isEmpty()) {
            graph.include("mes_qa_common_regulation_product_binding", reference.get("id"));
            return;
        }
        Map<String, Object> target = one(targets, "当前产品启用的通用QA绑定");
        if (!equalValue(target.get("dcc_project_code_id"), graph.id("dcc_project_code", dcc))
                || !Objects.equals(reference.get("scope_code"), target.get("scope_code"))
                || !String.valueOf(targetProduct).equals(target.get("active_binding_key")))
            throw blocked("当前产品通用QA的产品、DCC或适用范围不一致，保留原绑定");
        validateTargetQaVersion(graph, tenant, target.get("regulation_id"), target.get("regulation_version_id"), "MES_QA_COMMON", false);
        if (target.get("common_regulation_set_id") == null || target.get("common_regulation_set_version_id") == null) {
            if (target.get("common_regulation_set_id") != null || target.get("common_regulation_set_version_id") != null
                    || reference.get("common_regulation_set_id") != null)
                throw blocked("当前产品通用QA缺少完整规程套关联，保留原绑定");
            return;
        }
        Map<String, Object> set = targetDefinition("mes_qa_common_regulation_set", tenant, target.get("common_regulation_set_id"));
        Map<String, Object> version = targetDefinition("mes_qa_common_regulation_set_version", tenant, target.get("common_regulation_set_version_id"));
        if (!"ENABLED".equals(set.get("set_status")) || !"PUBLISHED".equals(version.get("lifecycle_status"))
                || !equalValue(version.get("set_id"), set.get("id")))
            throw blocked("当前产品通用QA规程套未启用、未发布或版本归属不符，保留原绑定");
        List<Map<String, Object>> members = jdbc.queryForList("SELECT * FROM mes_qa_common_regulation_set_version_member WHERE tenant_id=? AND deleted=0 AND set_version_id=? ORDER BY sort,id FOR UPDATE", tenant, version.get("id"));
        if (members.isEmpty()) throw blocked("当前产品通用QA规程套成员为空，保留原绑定");
        Set<String> roles = new HashSet<>();
        boolean includesPrimary = false;
        for (Map<String, Object> member : members) {
            validateTargetQaVersion(graph, tenant, member.get("regulation_id"), member.get("regulation_version_id"), "MES_QA_COMMON", false);
            roles.add(String.valueOf(member.get("member_role")));
            includesPrimary |= equalValue(member.get("regulation_id"), target.get("regulation_id"))
                    && equalValue(member.get("regulation_version_id"), target.get("regulation_version_id"));
        }
        if (!includesPrimary) throw blocked("当前产品通用QA主规程不属于绑定的规程套版本，保留原绑定");
        if (reference.get("common_regulation_set_version_id") != null) {
            for (Map<String, Object> member : graph.sourceRows("mes_qa_common_regulation_set_version_member", "set_version_id=?", reference.get("common_regulation_set_version_id")))
                if (!roles.contains(String.valueOf(member.get("member_role"))))
                    throw blocked("当前产品通用QA规程套缺少所需适用环节：" + member.get("member_role"));
        }
    }

    private void validateTargetQaVersion(MesFixedTestOrderReferenceGraph graph, long tenant, Object regulationId, Object versionId,
                                         String owner, boolean currentPublished) {
        Map<String, Object> regulation = targetDefinition("mes_qa_inspection_regulation", tenant, regulationId);
        Map<String, Object> version = targetDefinition("mes_qa_inspection_regulation_version", tenant, versionId);
        if (!owner.equals(regulation.get("owner_module"))
                || !Set.of("PUBLISHED", "RETIRED").contains(String.valueOf(version.get("lifecycle_status")))
                || !equalValue(version.get("regulation_id"), regulationId)
                || currentPublished && (!"PUBLISHED".equals(version.get("lifecycle_status"))
                    || !equalValue(regulation.get("current_version_id"), versionId)))
            throw blocked("当前产品QA规程版本类型、发布状态或归属不符，保留原绑定");
        Object raw = version.get("snapshot_json");
        if (!(raw instanceof String json) || json.isBlank()) throw blocked("当前产品QA缺少正式版本快照");
        JsonNode snapshot = graph.parse(json);
        // SaveReqVO.regulationId is optional when a regulation is first created; the version FK owns identity.
        if (snapshot.hasNonNull("regulationId") && !equalValue(value(snapshot.get("regulationId")), regulationId)
                || !owner.equals(snapshot.path("ownerModule").asText())
                || !equalValue(value(snapshot.get("dccProjectCodeId")), regulation.get("dcc_project_code_id")))
            throw blocked("当前产品QA正式快照与规程身份不一致，保留原绑定");
    }

    private Map<String, Object> targetDefinition(String table, long tenant, Object id) {
        return one(jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=? AND deleted=0 AND id=? FOR UPDATE", tenant, id), "当前租户通用QA定义：" + table + "/" + id);
    }

    private long replaceStartLeaders(ObjectNode configs, long tenant) {
        // Explicitly selected for this fixed test fixture; never create users or grant roles.
        final String username = "aidongxiang";
        List<Map<String, Object>> source = jdbc.queryForList("SELECT id FROM system_users WHERE tenant_id=? AND username=? AND status=0 AND deleted=0", SOURCE_TENANT, username);
        List<Map<String, Object>> target = jdbc.queryForList("SELECT id,nickname FROM system_users WHERE tenant_id=? AND username=? AND status=0 AND deleted=0", tenant, username);
        if (source.size() != 1 || target.size() != 1) throw blocked("两个租户必须均有唯一启用的开始工序负责人账号：" + username);
        JsonNode leaders = configs.get("routeStartProductionLeaders");
        if (leaders == null || !leaders.isArray() || leaders.isEmpty()) throw blocked("参考正式路线未配置开始工序负责人");
        for (JsonNode leader : leaders) {
            ObjectNode row = (ObjectNode) leader;
            if (!equalValue(value(row.get("productionLineId")), value(configs.path("flowGraph").get("routeId"))))
                throw blocked("参考开始工序负责人不属于当前正式路线");
            row.put("candidateSourceType", "USERS");
            row.putArray("candidateSourceIds").add(number(source.get(0).get("id")));
            row.putArray("candidateSourceNames").add(String.valueOf(target.get(0).get("nickname")));
        }
        return number(source.get(0).get("id"));
    }

    private void prepareCrafts(MesFixedTestOrderReferenceGraph graph, ObjectNode configs) {
        for (JsonNode node : configs.path("productionProcessConfigs")) {
            JSONObject config = JSON.parseObject(node.toString());
            List<MesProCraftParameters.Extracted> schemes = MesProCraftParameters.extractSchemes(config);
            for (JSONObject binding : MesProCraftParameters.bindings(config)) {
                Set<Long> devices = new HashSet<>(binding.getJSONArray("craftDeviceIds").toJavaList(Long.class));
                List<MesProCraftParameters.Extracted> matches = schemes.stream()
                        .filter(s -> new HashSet<>(s.deviceIds()).equals(devices)).toList();
                if (matches.size() != 1) throw blocked("参考正式工艺与执行参数方案不一致");
                Entry craft = graph.include("mes_pro_craft", binding.getLong("craftId"));
                if (!equalValue(craft.source.get("process_id"), config.getLong("processId"))) throw blocked("参考工艺不属于正式工序");
                String parameters = matches.get(0).parameters().toJSONString();
                if (craft.overrides.containsKey("parameters_json") && !parameters.equals(craft.overrides.get("parameters_json")))
                    throw blocked("参考正式路线中同一工艺对应不同参数定义");
                // The immutable formal snapshot, rather than the mutable craft library, defines this fixture.
                craft.overrides.put("parameters_json", parameters);
            }
        }
    }

    /** Corrects the fixed reference snapshot's five verified legacy equipment identities by code. */
    private void repairKnownDeviceReferences(MesFixedTestOrderReferenceGraph graph, ObjectNode configs) {
        Map<Long, List<String>> known = Map.of(
                980023L, List.of("气源", "高压检测机"),
                980024L, List.of("AL150158", "扭力扳手"),
                980025L, List.of("FM-E-24", "15N砝码"),
                980026L, List.of("A03274", "自动热合机"),
                980027L, List.of("A05048", "热合机"));
        Set<Long> referenced = new LinkedHashSet<>();
        for (JsonNode process : configs.path("productionProcessConfigs"))
            for (JsonNode group : process.path("deviceSelectionGroups"))
                for (JsonNode device : group.path("deviceIds"))
                    if (device.isIntegralNumber() && known.containsKey(device.longValue())) referenced.add(device.longValue());
        for (Long oldId : referenced) {
            List<String> identity = known.get(oldId);
            if (!graph.sourceRows("mes_dv_machinery", "id=?", oldId).isEmpty())
                throw blocked("参考旧设备编号的归属已变化，不能沿用已核定映射：" + oldId);
            Map<String, Object> current = one(graph.sourceRows("mes_dv_machinery", "code=?", identity.get(0)), "固定测试设备：" + identity.get(0));
            if (!identity.get(1).equals(current.get("name"))) throw blocked("参考设备编码与已核定名称不一致：" + identity.get(0));
            graph.alias("mes_dv_machinery", oldId, graph.include("mes_dv_machinery", current.get("id")));
        }
    }

    private void prepareRoute(MesFixedTestOrderReferenceGraph graph, ObjectNode configs, Object routeId, Object versionId) {
        for (JsonNode node : configs.path("flowGraph").path("nodes")) {
            Entry process = graph.include("mes_pro_route_process", value(node.get("routeProcessId")));
            if (!equalValue(process.source.get("route_id"), routeId)
                    || !equalValue(process.source.get("process_id"), value(node.get("processId"))))
                throw blocked("正式路线节点与关系数据不一致");
            requireFormalFields(process, node, Map.of("sort", "sort", "keyFlag", "key_flag", "checkFlag", "check_flag", "routeProcessWorkstationId", "workstation_id"));
        }
        for (String collection : List.of("batchUseConfigs", "scheduleUseConfigs")) {
            for (JsonNode config : configs.path(collection)) {
                Object use = config.path("useType").asText();
                Entry flow = graph.include("mes_pro_route_flow_config", one(graph.sourceRows("mes_pro_route_flow_config",
                        "route_id=? AND use_type=? AND enabled=1", routeId, use), "路线用途").get("id"));
                graph.alias("mes_pro_route_flow_config", value(config.get("routeFlowConfigId")), flow);
                Entry process = graph.include("mes_pro_route_flow_process_config", one(graph.sourceRows("mes_pro_route_flow_process_config",
                        "route_flow_config_id=? AND route_process_id=?", flow.key.id(), value(config.get("routeProcessId"))), "工序用途").get("id"));
                graph.alias("mes_pro_route_flow_process_config", value(config.get("id")), process);
                requireFormalFields(process, config, Map.of("enabled", "enabled", "executionMode", "execution_mode", "productionQuantityFactor", "production_quantity_factor"));
                requireEmptyArray(config, "formBindings"); // Slot bindings are independent; never infer them from batch records.
                for (JsonNode record : config.path("batchRecordReports")) {
                    Entry relation = graph.include("mes_pro_route_flow_process_batch_record", value(record.get("routeBindingId")));
                    if (!equalValue(relation.source.get("route_process_id"), value(config.get("routeProcessId")))
                            || !equalValue(relation.source.get("route_flow_process_config_id"), process.key.id())
                            || !equalValue(relation.source.get("batch_record_report_id"), value(record.get("batchRecordReportId"))))
                        throw blocked("正式逐工序批记录绑定不一致");
                    requireEmpty(relation, "form_template_id", "last_published_template_version_id", "form_definition_id", "fillable_scope_json", "required_condition_json", "candidate_source_ids");
                    requireFormalFields(relation, record, Map.of("batchRecordDefinitionId", "batch_record_definition_id", "batchRecordVersionId", "batch_record_version_id",
                            "permissionScopeId", "permission_scope_id", "formSlotType", "form_slot_type", "instanceScope", "instance_scope", "recordCategory", "record_category",
                            "validationProfile", "validation_profile", "requiredPolicy", "required_policy", "ownerRoleKey", "owner_role_key", "archiveVisibility", "archive_visibility"));
                }
            }
        }
        for (JsonNode schedule : configs.path("scheduleConfigs")) {
            Entry row = graph.include("mes_pro_route_schedule_config", one(graph.sourceRows("mes_pro_route_schedule_config",
                    "route_version_id=? AND route_process_id=? AND item_id IS NULL", versionId, value(schedule.get("routeProcessId"))), "路线排程配置").get("id"));
            requireEmpty(row, "calendar_rule_id");
            graph.alias("mes_pro_route_schedule_config", value(schedule.get("id")), row);
            requireFormalFields(row, schedule, Map.of("capacityMode", "capacity_mode", "nightShiftEnabled", "night_shift_enabled"));
        }
        for (JsonNode config : configs.path("productionProcessConfigs")) {
            for (JsonNode reason : config.path("lossReasons")) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", value(reason.get("id"))); row.put("route_version_id", versionId);
                row.put("route_process_id", value(config.get("routeProcessId"))); row.put("process_id", value(config.get("processId")));
                row.put("reason_code", value(reason.get("reasonCode"))); row.put("reason_name", value(reason.get("reasonName")));
                row.put("enabled", value(reason.get("enabled"))); row.put("remark", MARKER); row.put("sort", reason.path("sort").asInt(0));
                graph.plan("mes_pro_route_process_loss_reason", row.get("id"), row);
            }
        }
        for (String table : List.of("mes_pro_route_process_flow_edge", "mes_pro_route_process_flow_boundary_edge", "mes_pro_route_process_flow_layout"))
            graph.children(table, "route_id", routeId);
    }

    private void prepareQaAndReports(MesFixedTestOrderReferenceGraph graph) {
        Set<Key> prepared = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (Entry entry : graph.entries()) {
                if (!prepared.add(entry.key)) continue;
                changed = true;
                switch (entry.key.table()) {
                    case "mes_pro_batch_record_version" -> {
                        if (number(entry.source.get("child_form_member_count")) != 0) throw blocked("参考批记录包含独立子表单，当前固定测试初始化不支持该参考形态");
                        graph.children("mes_pro_batch_record_report", "batch_record_version_id", entry.key.id());
                    }
                    case "mes_pro_batch_record_report" -> requireEmpty(entry, "form_definition_id", "form_version_id");
                    case "mes_qa_common_regulation_set_version" -> graph.children("mes_qa_common_regulation_set_version_member", "set_version_id", entry.key.id());
                    case "mes_qa_inspection_regulation_version" -> {
                        graph.children("mes_qa_inspection_regulation_process", "regulation_version_id", entry.key.id());
                        graph.children("mes_qa_inspection_regulation_item", "regulation_version_id", entry.key.id());
                        graph.children("mes_qa_inspection_regulation_item_equipment", "regulation_version_id", entry.key.id());
                        // Definition lifecycle is copied; source publication actors and approval history are not.
                        entry.overrides.put("published_at", null); entry.overrides.put("retired_at", null);
                    }
                    default -> { }
                }
            }
        } while (changed);
    }

    private void prepareReports(MesFixedTestOrderReferenceGraph graph, long tenant) {
        for (Entry report : graph.entries("jimu_report")) {
            requireEmpty(report, "api_url", "js_str", "py_str");
            Long sources = jdbc.queryForObject("SELECT COUNT(*) FROM jimu_report_db WHERE jimu_report_id=?", Long.class, report.key.id());
            if (sources == null || sources != 0) throw blocked("固定测试表单包含动态数据源，不能复制连接或执行配置");
            report.overrides.put("code", "FT_" + UUID.nameUUIDFromBytes(("fixed:" + tenant + ":" + report.key.id()).getBytes(StandardCharsets.UTF_8)).toString().replace("-", ""));
        }
    }

    private void requireStaticReport(JsonNode json) {
        if (json.isObject()) {
            json.fields().forEachRemaining(field -> {
                JsonNode value = field.getValue();
                if (Set.of("sql", "sqlStr", "apiUrl", "dbCode", "dataSource", "javascript", "script", "url", "href").contains(field.getKey())
                        && !value.isNull() && !value.asText().isBlank()) throw blocked("固定测试表单包含动态执行或外部连接配置：" + field.getKey());
                requireStaticReport(value);
            });
        } else if (json.isArray()) json.forEach(this::requireStaticReport);
    }

    private void prepareMaterials(MesFixedTestOrderReferenceGraph graph, Object orderId) {
        for (String kind : List.of("pick", "replenishment")) {
            String table = "erp_kingdee_production_" + kind + "_list_item";
            String header = "erp_kingdee_production_" + kind + "_list";
            String fk = "production_" + kind + "_list_id";
            List<Map<String, Object>> items = graph.sourceRows(table, "production_order_no=?", WORK_ORDER_CODE);
            if (kind.equals("pick") && items.isEmpty()) throw blocked("参考工单没有正式ERP领料明细");
            for (Map<String, Object> item : items) {
                Entry entry = graph.include(table, item.get("id"));
                Entry head = graph.include(header, item.get(fk));
                if (!"C".equals(head.source.get("document_status"))) throw blocked("参考ERP领补料单未审核");
                // The formal pick-list resolver joins by the header FK. Preserve each row's
                // original source FID; imported historical lines can have a different source FID.
                if (!equalValue(head.source.get("source_form_id"), item.get("source_form_id"))
                        || !equalValue(head.source.get("source_bill_no"), item.get("source_bill_no")))
                    throw blocked("参考ERP领补料单头与明细来源身份不一致");
                // ERP material_number is external source text, not an MES item FK. MES material
                // masters are collected from the actual BOM, stock and local issue references.
                // A shared ERP header cannot bring other work orders' lines into the fixture.
                requirePositive(entry, "actual_quantity");
                head.overrides.put("description", MARKER);
            }
        }
        List<Map<String, Object>> issues = graph.sourceRows("mes_wm_product_issue", "work_order_id=?", orderId);
        if (issues.isEmpty()) throw blocked("参考工单没有已完成本地领料单");
        for (Map<String, Object> issue : issues) {
            if (number(issue.get("status")) != 4) throw blocked("参考本地领料单不是已完成状态");
            Entry entry = graph.include("mes_wm_product_issue", issue.get("id"));
            requireEmpty(entry, "task_id", "workstation_id");
            graph.children("mes_wm_product_issue_line", "issue_id", issue.get("id"));
            List<Map<String, Object>> details = graph.sourceRows("mes_wm_product_issue_detail", "issue_id=?", issue.get("id"));
            if (details.isEmpty()) throw blocked("参考领料单缺少库存批次明细");
            for (Map<String, Object> detail : details) {
                Entry row = graph.include("mes_wm_product_issue_detail", detail.get("id"));
                requirePositive(row, "quantity");
                Entry line = graph.include("mes_wm_product_issue_line", detail.get("line_id"));
                if (!equalValue(line.source.get("issue_id"), issue.get("id")) || !equalValue(line.source.get("item_id"), detail.get("item_id")))
                    throw blocked("参考领料行与明细身份不一致");
            }
        }
        for (Entry batch : graph.entries("mes_wm_batch")) {
            requireEmpty(batch, "task_id", "workstation_id", "tool_id", "mold_id");
            if (batch.source.get("work_order_id") != null && !equalValue(batch.source.get("work_order_id"), 0L)
                    && !equalValue(batch.source.get("work_order_id"), orderId))
                throw blocked("参考库存批次属于其他工单");
        }
    }

    /** With no old active owner there is no provenance that could authorize runtime deletion. */
    public void requireNoOrphanRuntime(long tenant, long workOrder) {
        for (String table : List.of("mes_pro_process_pool_event", "mes_pro_edhr_batch_execution", "mes_pro_feedback",
                "mes_pro_process_pool", "mes_pqc_inspection_task", "mes_pro_process_pool_order_process_completion",
                "mes_pro_process_pool_work_order_abnormal")) {
            if (!jdbc.queryForList("SELECT id FROM " + table + " WHERE tenant_id=? AND work_order_id=? AND deleted=0 FOR UPDATE", tenant, workOrder).isEmpty())
                throw blocked("指定工单没有历史活跃归属，但存在业务记录；不能自动清除：" + table);
        }
    }

    static Map<String, Object> one(List<Map<String, Object>> rows, String name) {
        if (rows.size() != 1) throw blocked("参考数据缺失或不唯一：" + name);
        return rows.get(0);
    }
    private static void requireFormalFields(Entry row, JsonNode snapshot, Map<String, String> fields) {
        fields.forEach((json, column) -> {
            if (snapshot.has(json) && !equalValue(value(snapshot.get(json)), row.source.get(column)))
                throw blocked("参考关系数据与正式快照内容不同：" + row.key.table() + "/" + column);
        });
    }
    static long number(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
    static Object value(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isIntegralNumber()) return node.longValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isNumber()) return node.decimalValue();
        return node.asText();
    }
    static void requireEmpty(Entry row, String... fields) {
        for (String field : fields) {
            Object v = row.source.get(field);
            if (v != null && !"".equals(v) && !equalValue(v, 0L)) throw blocked("参考数据包含当前初始化范围外的关联：" + row.key.table() + "/" + field);
        }
    }
    private static void requirePositive(Entry row, String field) {
        if (!(row.source.get(field) instanceof Number n) || new java.math.BigDecimal(n.toString()).signum() <= 0)
            throw blocked("参考领补料数量无效：" + row.key.table() + "/" + field);
    }
    static void requireEmptyArray(JsonNode node, String field) {
        if (node.has(field) && (!node.get(field).isArray() || !node.get(field).isEmpty()))
            throw blocked("参考快照包含当前固定测试初始化范围外的配置：" + field);
    }
}
