package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED;

/**
 * A single fixed-fixture import plan. Source reads always name the reference tenant;
 * inserts always name the current tenant. No arbitrary table or tenant is accepted by the HTTP entry.
 * The entire plan is resolved before the first insert, including existing identity conflicts.
 */
final class MesFixedTestOrderReferenceGraph {
    static final long SOURCE_TENANT = 1L;
    static final String MARKER = "[固定测试订单初始化，参考租户1；仅用于测试]";
    // These fields participate in identity checks but MySQL derives their values.
    private static final Map<String, String> GENERATED_COLUMNS = Map.of(
            "mes_pro_route_dcc_project_binding", "active_route_id",
            "mes_qa_inspection_regulation", "active_dcc_project_code_id",
            "mes_qa_common_regulation_product_binding", "active_binding_key");
    private final JdbcTemplate jdbc;
    private final long tenant;
    private final long actor;
    private final Map<Key, Entry> entries = new LinkedHashMap<>();
    private final Map<Key, Object> identities = new HashMap<>();
    private final Map<Key, Key> aliases = new HashMap<>();
    private final Map<String, Spec> specs = new LinkedHashMap<>();

    record Key(String table, Object id) { }
    record Spec(String columns, String naturalKey, Map<String, String> references) { }
    static final class Entry {
        final Key key;
        final Map<String, Object> source;
        final Map<String, Object> overrides = new LinkedHashMap<>();
        Map<String, Object> existing;
        boolean resolving;
        Entry(Key key, Map<String, Object> source) { this.key = key; this.source = source; }
    }

    MesFixedTestOrderReferenceGraph(JdbcTemplate jdbc, long tenant, long actor) {
        this.jdbc = jdbc;
        this.tenant = tenant;
        this.actor = actor;
        registerSpecs();
    }

    private void spec(String table, String columns, String naturalKey, String... references) {
        Map<String, String> refs = new LinkedHashMap<>();
        for (String reference : references) {
            String[] pair = reference.split(":", 2);
            refs.put(pair[0], pair[1]);
        }
        specs.put(table, new Spec(columns, naturalKey, refs));
    }

    private void registerSpecs() {
        spec("mes_pro_edhr_permission_scope", "scope_name,object_type,object_id,parent_scope_id,status,version", "object_type,object_id");
        spec("mes_pro_edhr_permission_rule", "scope_id,subject_type,subject_id,ability,decision,priority,effective_from,effective_to,status,version", "scope_id,subject_type,subject_id,ability,priority",
                "scope_id:mes_pro_edhr_permission_scope");
        spec("jimu_report_category", "name,parent_id,iz_leaf,source_type,sort_no", "parent_id,name", "parent_id:jimu_report_category");
        spec("jimu_report", "code,name,note,status,type,json_str,template,css_str,submit_form,is_multi_sheet", "code", "type:jimu_report_category");
        spec("mdm_product", "product_code,dcc_product_code,name_cn,name_en,model_specification,category,status", "product_code");
        spec("mes_md_unit_measure", "code,name,primary_flag,primary_id,change_rate,status,remark", "name,primary_flag,primary_id,change_rate,status",
                "primary_id:mes_md_unit_measure");
        spec("mes_md_item_type", "code,name,parent_id,item_or_product,sort,status,remark", "code", "parent_id:mes_md_item_type");
        spec("mes_md_item", "product_master_id,code,name,specification,unit_measure_id,item_type_id,status,safe_stock_flag,min_stock,max_stock,high_value,batch_flag,remark", "code",
                "product_master_id:mdm_product", "unit_measure_id:mes_md_unit_measure", "item_type_id:mes_md_item_type");
        // Contact information, credentials and tenant users are deliberately outside the fixture graph.
        spec("mes_md_client", "code,name,status,remark", "code");
        spec("mes_md_vendor", "code,name,status,remark", "code");
        spec("dcc_project_code", "product_master_id,doc_control_no,project_name,project_code,category,commissioned_production,storage_location,priority,status,batch_record_total_recognition_json,default_attributes_json", "project_code",
                "product_master_id:mdm_product");
        spec("mes_pro_work_order", "code,name,type,order_source_type,order_source_code,demand_bill_no,product_id,material_specification,quantity,quantity_produced,quantity_changed,quantity_scheduled,client_id,vendor_id,batch_code,workshop_name,bom_version,pick_mode,auxiliary_code,business_status,drawing_number,ref_no,schedule_status,planned_start_time,planned_end_time,request_date,parent_id,status,temporary_frozen,remark", "code",
                "product_id:mes_md_item", "client_id:mes_md_client", "vendor_id:mes_md_vendor");
        spec("mes_pro_work_order_bom", "work_order_id,item_id,quantity,remark", "work_order_id,item_id",
                "work_order_id:mes_pro_work_order", "item_id:mes_md_item");
        spec("mes_md_workshop", "code,name,area,status,remark", "code");
        spec("mes_md_production_line", "code,name,workshop_id,status,remark", "code", "workshop_id:mes_md_workshop");
        spec("mes_pro_process", "product_name,code,name,attention,sop_file_url,status,manual_shift_capacity,remark", "code");
        spec("mes_dv_machinery_type", "code,name,parent_id,status,sort,remark", "code", "parent_id:mes_dv_machinery_type");
        spec("mes_dv_machinery", "code,name,brand,specification,machinery_type_id,workshop_id,process_name,standard_hourly_capacity,status,remark", "code",
                "machinery_type_id:mes_dv_machinery_type", "workshop_id:mes_md_workshop");
        spec("mes_wm_warehouse", "code,name,address,area,frozen,remark", "code");
        spec("mes_wm_warehouse_location", "code,name,warehouse_id,area,frozen,remark", "warehouse_id,code", "warehouse_id:mes_wm_warehouse");
        spec("mes_wm_warehouse_area", "code,name,location_id,area,max_load,position_x,position_y,position_z,status,frozen,allow_item_mixing,allow_batch_mixing,remark", "location_id,code", "location_id:mes_wm_warehouse_location");
        spec("mes_md_workstation", "code,name,address,workshop_id,process_id,production_line_id,warehouse_id,location_id,area_id,single_standard_hourly_capacity,shift_hours,status,remark", "code",
                "workshop_id:mes_md_workshop", "process_id:mes_pro_process", "production_line_id:mes_md_production_line", "warehouse_id:mes_wm_warehouse", "location_id:mes_wm_warehouse_location", "area_id:mes_wm_warehouse_area");
        spec("mes_pro_craft", "code,name,process_id,parameters_json,fingerprint", "process_id,fingerprint", "process_id:mes_pro_process");
        spec("mes_pro_route", "code,name,description,status,remark", "code");
        spec("mes_pro_route_product", "route_id,item_id,quantity,production_time,time_unit_type,remark", "route_id,item_id", "route_id:mes_pro_route", "item_id:mes_md_item");
        spec("mes_pro_route_process", "route_id,process_id,workstation_id,sort,next_process_id,link_type,prepare_time,wait_time,color_code,key_flag,check_flag,batch_record_report_id,remark", "route_id,sort",
                "route_id:mes_pro_route", "process_id:mes_pro_process", "workstation_id:mes_md_workstation", "next_process_id:mes_pro_process", "batch_record_report_id:jimu_report");
        spec("mes_pro_route_version", "route_id,version_no,active,lifecycle_status,route_snapshot_json,route_snapshot_sha256,route_snapshot_format_version,remark", "route_id,version_no", "route_id:mes_pro_route");
        spec("mes_pro_route_dcc_project_binding", "route_id,dcc_project_code_id,version,active_route_id", "route_id",
                "route_id:mes_pro_route", "dcc_project_code_id:dcc_project_code", "active_route_id:mes_pro_route");
        spec("mes_pro_route_flow_config", "route_id,use_type,enabled,config_version,remark", "route_id,use_type", "route_id:mes_pro_route");
        spec("mes_pro_route_flow_process_config", "route_flow_config_id,route_id,route_process_id,use_type,enabled,execution_mode,production_quantity_factor,batch_record_report_id,remark", "route_flow_config_id,route_process_id",
                "route_flow_config_id:mes_pro_route_flow_config", "route_id:mes_pro_route", "route_process_id:mes_pro_route_process", "batch_record_report_id:jimu_report");
        spec("mes_pro_route_flow_process_batch_record", "route_flow_process_config_id,route_id,route_process_id,use_type,batch_record_report_id,batch_record_definition_id,batch_record_version_id,form_slot_type,form_binding_key,global_sync_key,form_template_id,form_template_name_snapshot,last_published_template_version_id,last_published_template_version_no,instance_scope,shared_form_key,fillable_scope_json,record_category,validation_profile,recordbook_enabled,permission_scope_id,record_category_snapshot_hash,required_policy,required_condition_json,owner_role_key,archive_visibility,slot_config_snapshot_hash,candidate_source_type,candidate_source_ids,candidate_source_names,report_sort,remark,form_definition_id", "route_flow_process_config_id,batch_record_report_id",
                "route_flow_process_config_id:mes_pro_route_flow_process_config", "route_id:mes_pro_route", "route_process_id:mes_pro_route_process", "batch_record_definition_id:mes_pro_batch_record_definition", "batch_record_version_id:mes_pro_batch_record_version", "batch_record_report_id:jimu_report");
        spec("mes_pro_route_schedule_config", "route_version_id,item_id,route_process_id,capacity_mode,hourly_capacity,infinite_duration_quantity_factor,infinite_duration_base_minutes,night_shift_enabled,calendar_rule_id,config_version,remark", "route_version_id,item_id,route_process_id",
                "route_version_id:mes_pro_route_version", "item_id:mes_md_item", "route_process_id:mes_pro_route_process");
        spec("mes_pro_route_process_flow_edge", "route_id,graph_version,source_route_process_id,target_route_process_id,relation_type,sort", "route_id,source_route_process_id,target_route_process_id",
                "route_id:mes_pro_route", "source_route_process_id:mes_pro_route_process", "target_route_process_id:mes_pro_route_process");
        spec("mes_pro_route_process_flow_boundary_edge", "route_id,graph_version,boundary_type,route_process_id,sort", "route_id,boundary_type,route_process_id",
                "route_id:mes_pro_route", "route_process_id:mes_pro_route_process");
        spec("mes_pro_route_process_flow_layout", "route_id,route_process_id,x,y,width,height,graph_version", "route_id,route_process_id", "route_id:mes_pro_route", "route_process_id:mes_pro_route_process");
        spec("mes_pro_route_process_loss_reason", "route_version_id,route_process_id,process_id,reason_code,reason_name,enabled,remark,sort", "route_version_id,route_process_id,reason_code",
                "route_version_id:mes_pro_route_version", "route_process_id:mes_pro_route_process", "process_id:mes_pro_process");
        spec("mes_pro_process_pool_defect_reason", "leader_user_id,reason_type,reason_code,reason_name,route_process_id,process_id,enabled,remark", "leader_user_id,route_process_id,reason_type,reason_code",
                "leader_user_id:@user", "route_process_id:mes_pro_route_process", "process_id:mes_pro_process");
        spec("mes_pro_process_pool_device_parameter_rule", "leader_user_id,route_process_id,process_id,device_id,parameter_code,parameter_name,unit,lower_limit,upper_limit,default_value,value_type,standard_text,option_values_json,default_text,decimal_scale,enabled,remark", "leader_user_id,route_process_id,device_id,parameter_code",
                "leader_user_id:@user", "route_process_id:mes_pro_route_process", "process_id:mes_pro_process", "device_id:mes_dv_machinery");
        spec("mes_pro_batch_record_definition", "batch_record_name,route_key,current_version_id,remark", "batch_record_name,route_key", "current_version_id:mes_pro_batch_record_version");
        spec("mes_pro_batch_record_version", "definition_id,version_no,status,source_file_name,source_file_sha256,route_id,remark,child_form_member_count,child_form_member_hash", "definition_id,version_no",
                "definition_id:mes_pro_batch_record_definition", "route_id:mes_pro_route");
        spec("mes_pro_batch_record_report", "sample_key,batch_record_name,form_slot_type,route_key,source_file_name,source_file_sha256,source_table_index,table_title,report_id,report_code,report_name,report_category_id,batch_record_definition_id,batch_record_version_id,form_definition_id,form_version_id,product_name,project_code", "batch_record_version_id,source_table_index",
                "batch_record_definition_id:mes_pro_batch_record_definition", "batch_record_version_id:mes_pro_batch_record_version", "report_id:jimu_report", "report_category_id:jimu_report_category");
        spec("mes_qa_inspection_regulation", "dcc_project_code_id,product_id,route_id,route_version_id,route_process_id,process_id,owner_module,regulation_code,regulation_name,lifecycle_status,current_version_id,active_dcc_project_code_id", "regulation_code",
                "dcc_project_code_id:dcc_project_code", "product_id:mdm_product", "route_id:mes_pro_route", "route_version_id:mes_pro_route_version", "route_process_id:mes_pro_route_process", "process_id:mes_pro_process", "current_version_id:mes_qa_inspection_regulation_version", "active_dcc_project_code_id:dcc_project_code");
        spec("mes_qa_inspection_regulation_version", "regulation_id,version_no,lifecycle_status,effective_date,inspection_type_rules_json,published_at,retired_at,final_inspection_applicable,final_inspection_not_applicable_reason,snapshot_json,draft_revision", "regulation_id,version_no", "regulation_id:mes_qa_inspection_regulation");
        spec("mes_qa_inspection_regulation_process", "regulation_version_id,process_code,process_name,sort", "regulation_version_id,process_code", "regulation_version_id:mes_qa_inspection_regulation_version");
        spec("mes_qa_inspection_regulation_item", "regulation_version_id,qa_process_id,item_sort,inspection_type,item_code,item_name,inspection_method,inspection_tool,standard_text,sampling_plan_text,standard_lower_limit,standard_upper_limit,standard_unit,standard_precision,equipment_required,result_type,first_inspection_quantity,inspection_level,aql,critical,failure_rule,source_note,source_original_page,source_original_item,source_original_excerpt,source_original_method", "regulation_version_id,qa_process_id,inspection_type,item_code",
                "regulation_version_id:mes_qa_inspection_regulation_version", "qa_process_id:mes_qa_inspection_regulation_process");
        spec("mes_qa_inspection_regulation_item_equipment", "regulation_version_id,inspection_type,item_code,equipment_id,equipment_code,equipment_name,equipment_number,default_flag,sort", "regulation_version_id,inspection_type,item_code,equipment_id,equipment_number",
                "regulation_version_id:mes_qa_inspection_regulation_version", "equipment_id:mes_dv_machinery");
        spec("mes_qa_common_regulation_product_binding", "product_id,dcc_project_code_id,common_regulation_set_id,common_regulation_set_version_id,regulation_id,regulation_version_id,scope_code,binding_status,active_binding_key,version,idempotency_key,change_reason", "active_binding_key",
                "product_id:mdm_product", "dcc_project_code_id:dcc_project_code", "common_regulation_set_id:mes_qa_common_regulation_set", "common_regulation_set_version_id:mes_qa_common_regulation_set_version", "regulation_id:mes_qa_inspection_regulation", "regulation_version_id:mes_qa_inspection_regulation_version");
        spec("mes_qa_common_regulation_set", "set_code,set_name,set_status,current_version_id,remark", "set_code", "current_version_id:mes_qa_common_regulation_set_version");
        spec("mes_qa_common_regulation_set_version", "set_id,version_no,lifecycle_status,effective_date,published_at,retired_at,remark", "set_id,version_no", "set_id:mes_qa_common_regulation_set");
        spec("mes_qa_common_regulation_set_version_member", "set_version_id,regulation_id,regulation_version_id,sort,member_role,remark", "set_version_id,sort",
                "set_version_id:mes_qa_common_regulation_set_version", "regulation_id:mes_qa_inspection_regulation", "regulation_version_id:mes_qa_inspection_regulation_version");
        spec("mes_wm_batch", "code,item_id,produce_date,expire_date,receipt_date,vendor_id,client_id,sales_order_code,purchase_order_code,work_order_id,lot_number,quality_status,remark", "item_id,code",
                "item_id:mes_md_item", "vendor_id:mes_md_vendor", "client_id:mes_md_client", "work_order_id:mes_pro_work_order");
        spec("mes_wm_material_stock", "item_type_id,item_id,batch_id,batch_code,warehouse_id,location_id,area_id,vendor_id,quantity,receipt_time,frozen", "item_id,batch_id,warehouse_id,location_id,area_id,vendor_id",
                "item_type_id:mes_md_item_type", "item_id:mes_md_item", "batch_id:mes_wm_batch", "warehouse_id:mes_wm_warehouse", "location_id:mes_wm_warehouse_location", "area_id:mes_wm_warehouse_area", "vendor_id:mes_md_vendor");
        spec("mes_wm_product_issue", "code,name,work_order_id,issue_date,required_time,status,remark", "code", "work_order_id:mes_pro_work_order");
        spec("mes_wm_product_issue_line", "issue_id,item_id,quantity,batch_id,remark", "issue_id,item_id,batch_id", "issue_id:mes_wm_product_issue", "item_id:mes_md_item", "batch_id:mes_wm_batch");
        spec("mes_wm_product_issue_detail", "issue_id,line_id,material_stock_id,item_id,quantity,batch_id,batch_code,warehouse_id,location_id,area_id,remark", "issue_id,line_id,material_stock_id",
                "issue_id:mes_wm_product_issue", "line_id:mes_wm_product_issue_line", "material_stock_id:mes_wm_material_stock", "item_id:mes_md_item", "batch_id:mes_wm_batch", "warehouse_id:mes_wm_warehouse", "location_id:mes_wm_warehouse_location", "area_id:mes_wm_warehouse_area");
        String header = "source_form_id,source_fid,source_bill_no,bill_date,document_status,stock_org_number,stock_org_name,production_org_number,production_org_name,owner_number,owner_name,department_number,department_name,description,source_modify_time,last_sync_time";
        String item = "source_form_id,source_fid,source_entry_id,source_line_key,source_bill_no,material_number,material_name,material_specification,unit_name,requested_quantity,actual_quantity,base_actual_quantity,warehouse_number,warehouse_name,stock_location_number,stock_location_name,lot_number,production_order_no,production_order_line_no,production_material_list_no,production_material_list_line_no,workshop_number,workshop_name,stock_status_number,stock_status_name,source_modify_time,last_sync_time";
        spec("erp_kingdee_production_pick_list", header, "source_form_id,source_fid");
        spec("erp_kingdee_production_pick_list_item", "production_pick_list_id," + item, "source_line_key", "production_pick_list_id:erp_kingdee_production_pick_list");
        spec("erp_kingdee_production_replenishment_list", header, "source_form_id,source_fid");
        spec("erp_kingdee_production_replenishment_list_item", "production_replenishment_list_id," + item, "source_line_key", "production_replenishment_list_id:erp_kingdee_production_replenishment_list");
    }

    List<Map<String, Object>> sourceRows(String table, String predicate, Object... args) {
        requireSpec(table);
        List<Object> params = new ArrayList<>(); params.add(SOURCE_TENANT); params.addAll(Arrays.asList(args));
        return jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=? AND " + deletion(table) + "=0 AND " + predicate + " ORDER BY id", params.toArray());
    }

    Entry include(String table, Object sourceId) {
        if (emptyId(sourceId)) return null;
        sourceId = normalize(sourceId);
        Key key = new Key(table, sourceId);
        if (aliases.containsKey(key)) return entries.get(aliases.get(key));
        if (entries.containsKey(key)) return entries.get(key);
        List<Map<String, Object>> rows = sourceRows(table, "id=?", sourceId);
        if (rows.size() != 1) throw blocked("参考数据缺失或不唯一：" + table + "/" + sourceId);
        return plan(table, sourceId, rows.get(0));
    }

    Entry plan(String table, Object sourceId, Map<String, Object> source) {
        Key key = new Key(table, normalize(sourceId));
        if (entries.containsKey(key)) return entries.get(key);
        Spec spec = requireSpec(table);
        Entry entry = new Entry(key, new LinkedHashMap<>(source));
        entries.put(key, entry);
        for (var ref : spec.references.entrySet()) {
            Object value = entry.source.get(ref.getKey());
            if (ref.getValue().startsWith("@")) requirePrincipal(ref.getValue(), value);
            else include(ref.getValue(), value);
        }
        return entry;
    }

    void alias(String table, Object oldId, Entry current) {
        if (emptyId(oldId) || equalValue(oldId, current.key.id)) return;
        Key alias = new Key(table, normalize(oldId));
        if (entries.containsKey(alias) || aliases.putIfAbsent(alias, current.key) != null
                && !aliases.get(alias).equals(current.key))
            throw blocked("正式快照关联身份冲突：" + alias);
    }

    Entry entry(String table, Object sourceId) { return entries.get(new Key(table, normalize(sourceId))); }

    List<Entry> entries(String table) { return entries.values().stream().filter(e -> e.key.table.equals(table)).toList(); }

    private static String deletion(String table) { return table.startsWith("jimu_") ? "del_flag" : "deleted"; }
    private static boolean emptyId(Object id) {
        return id == null || "0".equals(id) || "".equals(id) || id instanceof Number n && n.longValue() <= 0;
    }

    void children(String table, String fk, Object sourceId) {
        for (var row : sourceRows(table, fk + "=?", sourceId)) include(table, row.get("id"));
    }

    Collection<Entry> entries() { return List.copyOf(entries.values()); }

    Object id(String table, Object sourceId) {
        if (emptyId(sourceId)) return sourceId;
        Key key = new Key(table, normalize(sourceId));
        key = aliases.getOrDefault(key, key);
        if (table.startsWith("@")) { requirePrincipal(table, sourceId); return identities.get(key); }
        Entry entry = entries.get(key);
        if (entry == null) throw blocked("未纳入固定测试数据依赖：" + table + "/" + sourceId);
        resolve(entry);
        return identities.get(key);
    }

    void resolveAll() { for (Entry entry : entries.values()) resolve(entry); }

    private void resolve(Entry entry) {
        if (identities.containsKey(entry.key)) return;
        if (entry.resolving) throw blocked("测试数据自然身份存在循环：" + entry.key);
        entry.resolving = true;
        if (entry.key.table.equals("mes_pro_craft")) {
            resolveCraft(entry);
            entry.resolving = false;
            return;
        }
        Spec spec = requireSpec(entry.key.table);
        List<Object> params = new ArrayList<>(); params.add(tenant);
        StringJoiner where = new StringJoiner(" AND ");
        for (String field : spec.naturalKey.split(",")) {
            Object value = mappedField(entry, field);
            where.add(field + (value == null ? " IS NULL" : "=?"));
            if (value != null) params.add(value);
        }
        List<Map<String, Object>> existing = jdbc.queryForList("SELECT * FROM " + entry.key.table
                + " WHERE tenant_id=? AND " + deletion(entry.key.table) + "=0 AND " + where + " FOR UPDATE", params.toArray());
        if (existing.size() > 1) throw blocked("当前租户数据自然身份不唯一：" + entry.key.table + "/" + where);
        entry.existing = existing.isEmpty() ? null : existing.get(0);
        if (entry.key.table.equals("mes_md_unit_measure"))
            entry.overrides.put("code", entry.existing == null
                    ? "FTU_" + UUID.nameUUIDFromBytes((tenant + ":" + where + ":" + params).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-", "")
                    : entry.existing.get("code"));
        identities.put(entry.key, entry.existing == null
                ? entry.key.table.startsWith("jimu_") ? IdUtil.fastSimpleUUID() : IdUtil.getSnowflakeNextId()
                : normalize(entry.existing.get("id")));
        entry.resolving = false;
    }

    /** Craft codes and display names are tenant-local; the execution schema defines identity. */
    private void resolveCraft(Entry entry) {
        Object process = mappedField(entry, "process_id");
        String fingerprint = craftFingerprint(process, mappedField(entry, "parameters_json"));
        entry.overrides.put("fingerprint", fingerprint);
        List<Map<String, Object>> matches = jdbc.queryForList(
                "SELECT * FROM mes_pro_craft WHERE tenant_id=? AND deleted=0 AND process_id=? FOR UPDATE", tenant, process)
                .stream().filter(row -> fingerprint.equals(craftFingerprint(process, row.get("parameters_json")))).toList();
        if (matches.size() > 1) throw blocked("当前工序存在多个参数完全相同的工艺，不能自动选择：" + process);
        entry.existing = matches.isEmpty() ? null : matches.get(0);
        entry.overrides.put("code", entry.existing == null
                ? "FT_" + UUID.nameUUIDFromBytes((tenant + ":" + process + ":" + fingerprint).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-", "")
                : entry.existing.get("code"));
        if (entry.existing != null) entry.overrides.put("name", entry.existing.get("name"));
        for (Entry other : entries("mes_pro_craft")) {
            if (other == entry || !identities.containsKey(other.key)) continue;
            if (equalValue(mappedField(other, "process_id"), process)
                    && fingerprint.equals(other.overrides.get("fingerprint")))
                throw blocked("参考正式路线存在重复工艺参数身份，请明确工艺绑定：" + process);
        }
        identities.put(entry.key, entry.existing == null ? IdUtil.getSnowflakeNextId() : normalize(entry.existing.get("id")));
    }

    private String craftFingerprint(Object process, Object parameters) {
        return MesProCraftParameters.fingerprint(((Number) process).longValue(), JSON.parseArray((String) parameters));
    }

    Object mappedField(Entry entry, String field) {
        if (entry.overrides.containsKey(field)) return entry.overrides.get(field);
        Object source = entry.source.get(field);
        String ref = requireSpec(entry.key.table).references.get(field);
        return ref == null ? source : id(ref, source);
    }

    void insertAll() {
        resolveAll();
        // Validate every reused reference before writing anything. Existing formal data is never updated.
        for (Entry entry : entries.values()) {
            if (entry.existing == null) continue;
            retainErpMaterialSpecification(entry);
            Spec spec = requireSpec(entry.key.table);
            for (String field : spec.columns.split(",")) {
                // Work-order progress is reset by the existing runtime reset, never imported.
                if (Set.of("remark", "description", "last_sync_time", "source_modify_time", "published_at", "retired_at").contains(field)
                        || entry.key.table.equals("mes_pro_work_order") && Set.of("status", "quantity_produced", "quantity_scheduled", "temporary_frozen").contains(field)) continue;
                // The machine's operating state belongs to this tenant, outside the route definition.
                if (entry.key.table.equals("mes_dv_machinery") && field.equals("status")) continue;
                // Retain the local material label; specification, unit, category and product must still match.
                if (entry.key.table.equals("mes_md_item") && field.equals("name")) continue;
                if (entry.key.table.equals("mes_md_unit_measure") && field.equals("code")) continue;
                if (entry.key.table.equals("mes_pro_craft")) {
                    if (Set.of("code", "name", "fingerprint").contains(field)) continue;
                    if (field.equals("parameters_json")) {
                        if (!craftFingerprint(mappedField(entry, "process_id"), mappedField(entry, field))
                                .equals(craftFingerprint(entry.existing.get("process_id"), entry.existing.get(field))))
                            throw blocked("当前租户工艺参数已变化，保留原数据");
                        continue;
                    }
                }
                if (!equalValue(mappedField(entry, field), entry.existing.get(field)))
                    throw blocked("当前租户已有数据内容冲突，保留原数据：" + entry.key.table + "/" + field);
            }
            if (entry.key.table.equals("mes_pro_route_version")
                    && !equalValue(mappedField(entry, "route_snapshot_sha256"), entry.existing.get("route_snapshot_sha256")))
                throw blocked("当前租户已有同名路线版本内容不同，保留原版本");
        }
        Set<Key> inserted = new HashSet<>();
        for (Entry entry : entries.values()) insert(entry, inserted, new HashSet<>());
        for (Entry entry : entries.values()) {
            if (entry.existing != null || !requireSpec(entry.key.table).references.containsKey("current_version_id")) continue;
            jdbc.update("UPDATE " + entry.key.table + " SET current_version_id=? WHERE id=? AND tenant_id=?",
                    mappedField(entry, "current_version_id"), id(entry.key.table, entry.key.id), tenant);
        }
    }

    private void retainErpMaterialSpecification(Entry entry) {
        if (!entry.key.table.equals("mes_md_item")
                || equalValue(mappedField(entry, "specification"), entry.existing.get("specification"))) return;
        Entry category = entry("mes_md_item_type", entry.source.get("item_type_id"));
        if (category == null || !"KINGDEE_PRODUCT".equals(category.source.get("code"))) return;
        // MES ERP sync treats specifications as the tenant's current ERP snapshot, not material identity.
        // Both master snapshots must be verified; never infer equivalence from descriptive text or dimensions.
        for (long owner : List.of(SOURCE_TENANT, tenant)) {
            Map<String, Object> material = owner == SOURCE_TENANT ? entry.source : entry.existing;
            List<Map<String, Object>> products = jdbc.queryForList(
                    "SELECT name,standard,status FROM erp_product WHERE tenant_id=? AND deleted=0 AND bar_code=? FOR UPDATE",
                    owner, material.get("code"));
            if (products.size() != 1 || !equalValue(products.get(0).get("status"), 0)
                    || !Objects.equals(products.get(0).get("name"), material.get("name"))
                    || !Objects.equals(products.get(0).get("standard"), material.get("specification"))
                    || !Objects.equals(entry.source.get("name"), entry.existing.get("name")))
                throw blocked("物料规格差异无法由两边有效ERP主档核实，保留原物料：" + entry.source.get("code"));
        }
        entry.overrides.put("specification", entry.existing.get("specification"));
    }

    private void insert(Entry entry, Set<Key> inserted, Set<Key> path) {
        if (entry.existing != null || inserted.contains(entry.key)) return;
        if (!path.add(entry.key)) throw blocked("初始化依赖存在循环：" + entry.key);
        Spec spec = requireSpec(entry.key.table);
        for (var ref : spec.references.entrySet()) {
            if (ref.getKey().equals("current_version_id") || ref.getValue().startsWith("@")) continue;
            Object sourceId = entry.source.get(ref.getKey());
            if (emptyId(sourceId)) continue;
            Key key = new Key(ref.getValue(), normalize(sourceId));
            Entry dependency = entries.get(aliases.getOrDefault(key, key));
            if (dependency != null) insert(dependency, inserted, path);
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", identities.get(entry.key));
        for (String field : requireSpec(entry.key.table).columns.split(",")) {
            if (!entry.source.containsKey(field)) throw blocked("初始化所需字段不存在：" + entry.key.table + "/" + field);
            if (field.equals(GENERATED_COLUMNS.get(entry.key.table))) continue;
            row.put(field, field.equals("current_version_id") ? null : mappedField(entry, field));
        }
        row.put("tenant_id", tenant); row.put(deletion(entry.key.table), false);
        if (entry.key.table.startsWith("jimu_")) {
            row.put("create_by", String.valueOf(actor)); row.put("update_by", String.valueOf(actor));
            row.put("create_time", new java.sql.Timestamp(System.currentTimeMillis()));
            row.put("update_time", row.get("create_time"));
        } else {
            row.put("creator", String.valueOf(actor)); row.put("updater", String.valueOf(actor));
        }
        if (row.containsKey("remark")) row.put("remark", MARKER);
        if (entry.key.table.equals("mes_pro_batch_record_report")) row.put("last_import_time", new java.sql.Timestamp(System.currentTimeMillis()));
        String columns = String.join(",", row.keySet());
        String placeholders = String.join(",", Collections.nCopies(row.size(), "?"));
        if (jdbc.update("INSERT INTO " + entry.key.table + " (" + columns + ") VALUES (" + placeholders + ")", row.values().toArray()) != 1)
            throw blocked("固定测试数据插入失败：" + entry.key.table);
        inserted.add(entry.key);
        path.remove(entry.key);
    }

    private void requirePrincipal(String type, Object sourceId) {
        if (emptyId(sourceId)) return;
        Key key = new Key(type, normalize(sourceId));
        if (identities.containsKey(key)) return;
        String table = type.equals("@user") ? "system_users" : "system_role";
        String natural = type.equals("@user") ? "username" : "code";
        List<Map<String, Object>> source = jdbc.queryForList("SELECT " + natural + " FROM " + table + " WHERE tenant_id=? AND id=? AND deleted=0", SOURCE_TENANT, sourceId);
        if (source.size() != 1) throw blocked("参考租户身份不存在：" + type + "/" + sourceId);
        Object code = source.get(0).get(natural);
        List<Long> targets = jdbc.queryForList("SELECT id FROM " + table + " WHERE tenant_id=? AND " + natural + "=? AND deleted=0 AND status=0", Long.class, tenant, code);
        if (targets.size() != 1) throw blocked("当前租户缺少唯一启用" + (type.equals("@user") ? "用户" : "角色") + "：" + code + "；请先配置，不复制账号或授权");
        identities.put(key, targets.get(0));
    }

    JsonNode parse(String json) {
        try { return JsonUtils.getObjectMapper().readTree(json); }
        catch (Exception ex) { throw new IllegalArgumentException("固定测试参考快照不是有效 JSON", ex); }
    }

    static RuntimeException blocked(String detail) { return exception(PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED, detail); }
    private Spec requireSpec(String table) {
        Spec spec = specs.get(table);
        if (spec == null) throw blocked("不允许初始化的数据类型：" + table);
        return spec;
    }
    private static Object normalize(Object id) { return id instanceof Number number ? number.longValue() : id; }
    static boolean equalValue(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString())) == 0;
        if (a instanceof Boolean bool && b instanceof Number number) return bool == (number.intValue() != 0);
        if (b instanceof Boolean && a instanceof Number) return equalValue(b, a);
        return Objects.equals(a, b);
    }
}
