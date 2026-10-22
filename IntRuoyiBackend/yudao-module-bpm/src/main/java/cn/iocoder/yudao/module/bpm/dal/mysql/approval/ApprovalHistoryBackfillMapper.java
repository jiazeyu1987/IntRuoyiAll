package cn.iocoder.yudao.module.bpm.dal.mysql.approval;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

/** Manual migration inventory. No LIMIT, deleted filter, or inferred business-key owner. */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApprovalHistoryBackfillMapper {
    String LOCAL_PROCESSES="(SELECT BINARY PROC_INST_ID_ FROM ACT_HI_PROCINST WHERE BINARY TENANT_ID_=BINARY #{tenantText} "
            +"UNION SELECT BINARY ID_ FROM ACT_RU_EXECUTION WHERE BINARY ID_=BINARY PROC_INST_ID_ AND BINARY TENANT_ID_=BINARY #{tenantText})";
    @Select("SELECT id FROM system_tenant WHERE id=#{tenant} AND deleted=0 FOR UPDATE")
    Long lockTenant(@Param("tenant") Long tenant);

    @Select("""
        SELECT p.PROC_INST_ID_ process_id,p.PROC_DEF_ID_ definition_id,d.KEY_ definition_key,
          p.BUSINESS_KEY_ business_key,p.TENANT_ID_ engine_tenant,d.TENANT_ID_ definition_tenant,
          p.START_USER_ID_ start_user,p.START_TIME_ start_time,'HISTORY' scope
        FROM ACT_HI_PROCINST p LEFT JOIN ACT_RE_PROCDEF d ON BINARY d.ID_=BINARY p.PROC_DEF_ID_
        WHERE BINARY p.TENANT_ID_=BINARY #{tenantText}
        UNION ALL
        SELECT p.ID_,p.PROC_DEF_ID_,d.KEY_,p.BUSINESS_KEY_,p.TENANT_ID_,d.TENANT_ID_,
          p.START_USER_ID_,p.START_TIME_,'RUNTIME'
        FROM ACT_RU_EXECUTION p LEFT JOIN ACT_RE_PROCDEF d ON BINARY d.ID_=BINARY p.PROC_DEF_ID_
        WHERE BINARY p.ID_=BINARY p.PROC_INST_ID_ AND BINARY p.TENANT_ID_=BINARY #{tenantText}
        ORDER BY process_id,scope
        """)
    List<Map<String,Object>> engines(@Param("tenantText") String tenantText);

    @Select("SELECT t.*,'RUNTIME' inventory_scope FROM ACT_RU_TASK t WHERE BINARY t.TENANT_ID_=BINARY #{tenantText} "
            +"OR BINARY t.PROC_INST_ID_ IN "+LOCAL_PROCESSES+" ORDER BY BINARY t.ID_")
    List<Map<String,Object>> runtimeTasks(@Param("tenantText") String tenantText);
    @Select("SELECT t.* FROM ACT_HI_TASKINST t WHERE BINARY t.TENANT_ID_=BINARY #{tenantText} "
            +"OR BINARY t.PROC_INST_ID_ IN "+LOCAL_PROCESSES+" ORDER BY BINARY t.ID_")
    List<Map<String,Object>> historicTasks(@Param("tenantText") String tenantText);
    @Select("""
        SELECT i.* FROM ACT_RU_IDENTITYLINK i WHERE i.TASK_ID_ IN
          (SELECT ID_ FROM ACT_RU_TASK WHERE BINARY TENANT_ID_=BINARY #{tenantText})
          OR BINARY i.PROC_INST_ID_ IN
        """+LOCAL_PROCESSES+" ORDER BY BINARY i.ID_")
    List<Map<String,Object>> runtimeLinks(@Param("tenantText") String tenantText);
    @Select("""
        SELECT i.* FROM ACT_HI_IDENTITYLINK i WHERE i.TASK_ID_ IN
          (SELECT ID_ FROM ACT_HI_TASKINST WHERE BINARY TENANT_ID_=BINARY #{tenantText})
          OR BINARY i.PROC_INST_ID_ IN
        """+LOCAL_PROCESSES+" ORDER BY BINARY i.ID_")
    List<Map<String,Object>> historicLinks(@Param("tenantText") String tenantText);
    @Select("SELECT * FROM bpm_process_instance_copy WHERE tenant_id=#{tenant} ORDER BY id")
    List<Map<String,Object>> copies(@Param("tenant") Long tenant);

    // The predicate also returns foreign-tenant rows claiming a local process; validation blocks them.
    String SCOPE = " (tenant_id=#{tenant} OR BINARY process_instance_id IN " +LOCAL_PROCESSES+") ";
    @Select("""
        SELECT r.*,e.id execution_id,e.tenant_id execution_tenant,e.revision_no execution_revision,
          e.process_instance_id execution_process,e.process_definition_key execution_definition_key,
          s.id execution_snapshot_id,s.process_instance_id execution_snapshot_process,
          s.process_definition_id execution_snapshot_definition,s.process_definition_key execution_snapshot_key
        FROM bpm_business_approval_request r LEFT JOIN mes_pro_batch_record_execution e
          ON r.system_code='MES' AND r.object_type='EDHR_BATCH_EXECUTION' AND r.action_code='SUBMIT_REVIEW'
          AND e.tenant_id=r.tenant_id AND BINARY CAST(e.id AS CHAR)=BINARY r.object_id
        LEFT JOIN mes_pro_batch_record_approval_snapshot s ON s.tenant_id=e.tenant_id AND s.execution_id=e.id
        WHERE r.process_instance_id IS NOT NULL AND (r.tenant_id=#{tenant} OR BINARY r.process_instance_id IN
        """+LOCAL_PROCESSES+") ORDER BY r.tenant_id,r.id,s.id")
    List<Map<String,Object>> business(@Param("tenant") Long tenant,@Param("tenantText") String tenantText);
    @Select("""
        SELECT f.*,s.id submit_snapshot_id,s.business_context_json submit_context_json,
          s.form_data_json submit_form_json,s.snapshot_version submit_snapshot_version
        FROM bpm_form_action_instance f LEFT JOIN bpm_form_action_snapshot s
          ON s.tenant_id=f.tenant_id AND s.instance_id=f.id AND s.snapshot_type='SUBMIT'
        WHERE f.bpm_process_instance_id IS NOT NULL AND (f.tenant_id=#{tenant} OR BINARY f.bpm_process_instance_id IN
        """+LOCAL_PROCESSES+") ORDER BY f.tenant_id,f.id,s.id")
    List<Map<String,Object>> forms(@Param("tenant") Long tenant,@Param("tenantText") String tenantText);
    @Select("""
        SELECT b.*,r.tenant_id request_tenant,r.request_key,r.bpm_process_instance_id request_process_id
        FROM dcc_registration_certificate_bpm_binding b LEFT JOIN dcc_registration_certificate_access_request r
          ON r.tenant_id=b.tenant_id AND r.id=b.request_id
        WHERE b.bpm_process_instance_id IS NOT NULL AND (b.tenant_id=#{tenant} OR BINARY b.bpm_process_instance_id IN
        """+LOCAL_PROCESSES+") ORDER BY b.tenant_id,b.id")
    List<Map<String,Object>> registrations(@Param("tenant") Long tenant,@Param("tenantText") String tenantText);
    @Select("""
        SELECT f.id,f.tenant_id,f.version_no,f.process_instance_id,'CURRENT_FILE' pointer_kind
        FROM dcc_controlled_file f WHERE f.process_instance_id IS NOT NULL
          AND (f.tenant_id=#{tenant} OR BINARY f.process_instance_id IN
        """+LOCAL_PROCESSES+") "+"""
        UNION
        SELECT v.native_version_id,v.tenant_id,v.version_no,v.approval_process_instance_id,'VERSION_REF'
        FROM controlled_content_version_ref v WHERE v.content_type='DCC_CONTROLLED_FILE'
          AND v.approval_process_instance_id IS NOT NULL
          AND (v.tenant_id=#{tenant} OR BINARY v.approval_process_instance_id IN
        """+LOCAL_PROCESSES+") ORDER BY tenant_id,id,pointer_kind")
    List<Map<String,Object>> dccPointers(@Param("tenant") Long tenant,@Param("tenantText") String tenantText);
    @Select("""
        SELECT e.*,f.bpm_process_instance_id,f.idempotency_key form_idempotency_key
        FROM bpm_form_effect_execution e JOIN bpm_form_action_instance f
          ON f.tenant_id=e.tenant_id AND f.id=e.instance_id
        WHERE e.tenant_id=#{tenant} ORDER BY e.id
        """)
    List<Map<String,Object>> effects(@Param("tenant") Long tenant);
    @Select("""
        SELECT a.*,v.native_version_id,v.version_no,v.approval_process_instance_id
        FROM controlled_content_transition_audit a JOIN controlled_content_version_ref v
          ON v.tenant_id=a.tenant_id AND v.id=a.version_ref_id
        WHERE a.tenant_id=#{tenant} AND v.content_type='DCC_CONTROLLED_FILE' ORDER BY a.id
        """)
    List<Map<String,Object>> dccTransitions(@Param("tenant") Long tenant);
    @Select("SELECT * FROM bpm_oa_leave WHERE process_instance_id IS NOT NULL AND " + SCOPE + " ORDER BY tenant_id,id")
    List<Map<String,Object>> leaves(@Param("tenant") Long tenant,@Param("tenantText") String tenantText);
    @Select("SELECT * FROM crm_contract WHERE process_instance_id IS NOT NULL AND " + SCOPE + " ORDER BY tenant_id,id")
    List<Map<String,Object>> contracts(@Param("tenant") Long tenant,@Param("tenantText") String tenantText);
    @Select("SELECT * FROM crm_receivable WHERE process_instance_id IS NOT NULL AND " + SCOPE + " ORDER BY tenant_id,id")
    List<Map<String,Object>> receivables(@Param("tenant") Long tenant,@Param("tenantText") String tenantText);

    @Select("SELECT * FROM system_operate_log WHERE tenant_id=#{tenant} AND BINARY trace_id=BINARY #{trace} AND type='HISTORY_BACKFILL' ORDER BY id")
    List<Map<String,Object>> batchLog(@Param("tenant") Long tenant,@Param("trace") String trace);
    @Insert("""
        INSERT INTO system_operate_log(tenant_id,trace_id,user_id,user_type,type,sub_type,biz_id,action,extra,creator,updater)
        VALUES(#{tenant},#{trace},#{actor},2,'HISTORY_BACKFILL',#{kind},#{tenant},
          'Explicit manual approval ownership history migration',#{json},#{actor},#{actor})
        """)
    int audit(@Param("tenant") Long tenant,@Param("trace") String trace,@Param("actor") Long actor,
              @Param("kind") String kind,@Param("json") String json);
}
