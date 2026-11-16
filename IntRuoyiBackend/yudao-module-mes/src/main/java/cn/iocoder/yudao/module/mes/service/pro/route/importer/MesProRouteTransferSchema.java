package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import java.util.*;

/** Explicit allowlist for the route resource workbook. Audit and generated columns never cross tenants. */
final class MesProRouteTransferSchema {
    record Table(String name, String identity, List<String> columns) {}
    static final Map<String, Table> TABLES = new LinkedHashMap<>();
    static {
        register("mdm_product", "product_code", "id,product_code,dcc_product_code,name_cn,name_en,model_specification,category,status");
        register("dcc_project_code", "project_code", "id,product_master_id,doc_control_no,project_name,project_code,category,commissioned_production,project_leader,project_engineer,storage_location,priority,status,batch_record_total_recognition_json,project_leader_user_id,default_attributes_json,library_scope");
        register("mes_pro_process", "code", "id,product_name,code,name,attention,sop_file_url,status,manual_shift_capacity,remark");
        register("mes_pro_craft", "code", "id,code,name,process_id,parameters_json,fingerprint");
        register("mes_md_workshop", "code", "id,code,name,area,charge_user_id,status,remark");
        register("mes_md_production_line", "code", "id,code,name,workshop_id,calendar_plan_id,status,remark");
        register("mes_dv_machinery_type", "code", "id,code,name,parent_id,status,sort,remark");
        register("mes_dv_machinery", "code", "id,code,name,brand,specification,machinery_type_id,workshop_id,process_name,standard_hourly_capacity,status,last_mainten_time,last_check_time,remark");
        register("mes_dv_machinery_process", "", "id,machinery_id,process_id,process_code,machinery_code,line_name,process_name,device_name,device_quantity,ten_half_hour_daily_capacity,standard_hourly_capacity,source_row_no,remark");
        register("mes_md_workstation", "code", "id,code,name,address,workshop_id,process_id,production_line_id,warehouse_id,location_id,area_id,single_standard_hourly_capacity,shift_hours,status,remark");
        register("mes_md_workstation_machine", "", "id,workstation_id,machinery_id,quantity,remark");
        register("mes_md_unit_measure", "code", "id,code,name,primary_flag,primary_id,change_rate,status,remark");
        register("mes_md_item_type", "code", "id,code,name,parent_id,item_or_product,sort,status,remark");
        register("mes_md_item", "code", "id,product_master_id,code,name,specification,unit_measure_id,item_type_id,status,safe_stock_flag,min_stock,max_stock,high_value,batch_flag,remark");
        register("mes_wm_warehouse", "code", "id,code,name,address,area,charge_user_id,frozen,remark");
        register("mes_wm_warehouse_location", "code", "id,code,name,warehouse_id,area,frozen,remark");
        register("mes_wm_warehouse_area", "code", "id,code,name,location_id,area,max_load,position_x,position_y,position_z,status,frozen,allow_item_mixing,allow_batch_mixing,remark");
        register("mes_pro_schedule_calendar_rule", "", "id,skip_statutory_holidays,weekend_rest_mode,date_shift_mode_by_date_json,temporary_freeze_enabled,remark");
        register("mes_pro_process_pool_team_device", "device_code", "id,leader_user_id,device_code,device_name,device_status,enabled,status_changed_at,remark");
        register("mes_pro_process_pool_team_process_device", "", "id,leader_user_id,process_id,device_id,device_group_key,selection_mode,enabled,disabled_at,remark");
        register("mes_pro_process_pool_device_parameter_rule", "", "id,leader_user_id,route_process_id,process_id,device_id,parameter_code,parameter_name,unit,lower_limit,upper_limit,default_value,value_type,standard_text,option_values_json,default_text,decimal_scale,enabled,remark");
        register("mes_pro_route", "code", "id,code,name,description,status,remark");
        register("mes_pro_route_dcc_project_binding", "", "id,route_id,dcc_project_code_id,version");
        register("mes_pro_route_version", "", "id,route_id,version_no,active,lifecycle_status,route_snapshot_json,route_snapshot_sha256,route_snapshot_format_version,remark");
        register("mes_pro_route_process", "", "id,route_id,process_id,workstation_id,sort,next_process_id,link_type,prepare_time,wait_time,color_code,key_flag,check_flag,batch_record_report_id,remark");
        register("mes_pro_route_process_flow_edge", "", "id,route_id,graph_version,source_route_process_id,target_route_process_id,relation_type,sort");
        register("mes_pro_route_process_flow_boundary_edge", "", "id,route_id,graph_version,boundary_type,route_process_id,sort");
        register("mes_pro_route_process_flow_layout", "", "id,route_id,route_process_id,x,y,width,height,graph_version");
        register("mes_pro_route_product", "", "id,route_id,item_id,quantity,production_time,time_unit_type,remark");
        register("mes_pro_route_product_bom", "", "id,route_id,process_id,product_id,item_id,quantity,remark");
        register("mes_pro_route_flow_config", "", "id,route_id,use_type,enabled,config_version,remark");
        register("mes_pro_route_flow_process_config", "", "id,route_flow_config_id,route_id,route_process_id,use_type,enabled,execution_mode,production_quantity_factor,batch_record_report_id,remark");
        register("mes_pro_route_flow_process_batch_record", "", "id,route_flow_process_config_id,route_id,route_process_id,use_type,batch_record_report_id,batch_record_definition_id,batch_record_version_id,form_slot_type,form_binding_key,global_sync_key,form_template_id,form_template_name_snapshot,last_published_template_version_id,last_published_template_version_no,instance_scope,shared_form_key,fillable_scope_json,record_category,validation_profile,recordbook_enabled,permission_scope_id,record_category_snapshot_hash,required_policy,required_condition_json,owner_role_key,archive_visibility,slot_config_snapshot_hash,candidate_source_type,candidate_source_ids,candidate_source_names,report_sort,remark,form_definition_id");
        register("mes_pro_route_schedule_config", "", "id,route_version_id,item_id,route_process_id,capacity_mode,hourly_capacity,infinite_duration_quantity_factor,infinite_duration_base_minutes,night_shift_enabled,calendar_rule_id,config_version,remark");
        register("mes_pro_route_process_loss_reason", "", "id,route_version_id,route_process_id,process_id,reason_code,reason_name,enabled,remark,sort");
    }
    private static void register(String table, String identity, String columns) {
        TABLES.put(table, new Table(table, identity, List.of(columns.split(","))));
    }
    private MesProRouteTransferSchema() {}
}
