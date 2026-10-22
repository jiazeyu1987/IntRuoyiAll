package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccObsoleteFormRuntimeObligationDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

/** Append-only repository; exact tenant identities and complete collections, no update/delete API. */
@Repository
public class DccObsoleteFormRuntimeObligationMapper {
    private final JdbcTemplate jdbc;
    public DccObsoleteFormRuntimeObligationMapper(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final RowMapper<DccObsoleteFormRuntimeObligationDO> ROW=(r,n)->new DccObsoleteFormRuntimeObligationDO(
        r.getObject("id",Long.class),
        r.getObject("tenant_id",Long.class),
        r.getObject("head_id",Long.class),
        r.getObject("form_instance_id",Long.class),
        r.getObject("controlled_file_id",Long.class),
        r.getString("bpm_process_instance_id"),
        r.getString("obligation_id"),
        r.getString("stage_code"),
        r.getString("node_instance_id"),
        r.getObject("stage_no",Integer.class),
        r.getObject("stage_order",Integer.class),
        r.getObject("department_id",Long.class),
        r.getString("candidate_source_type"),
        r.getString("candidate_source_ids_json"),
        r.getString("frozen_user_ids_json"),
        r.getString("approve_method"),
        r.getObject("approve_ratio",Integer.class),
        r.getObject("require_all_approvals",Boolean.class),
        r.getObject("created_at",LocalDateTime.class));
    public long insert(DccObsoleteFormRuntimeObligationDO row) {
        if(row==null || row.id()!=null) throw new IllegalArgumentException("New submitted evidence must not supply a row id");
        var keys=new GeneratedKeyHolder();
        int count=jdbc.update(connection->{
            var statement=connection.prepareStatement("INSERT INTO dcc_obsolete_form_runtime_obligation (tenant_id,head_id,form_instance_id,controlled_file_id,bpm_process_instance_id,obligation_id,stage_code,node_instance_id,stage_no,stage_order,department_id,candidate_source_type,candidate_source_ids_json,frozen_user_ids_json,approve_method,approve_ratio,require_all_approvals,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
            statement.setObject(1,row.tenantId());
            statement.setObject(2,row.headId());
            statement.setObject(3,row.formInstanceId());
            statement.setObject(4,row.controlledFileId());
            statement.setObject(5,row.bpmProcessInstanceId());
            statement.setObject(6,row.obligationId());
            statement.setObject(7,row.stageCode());
            statement.setObject(8,row.nodeInstanceId());
            statement.setObject(9,row.stageNo());
            statement.setObject(10,row.stageOrder());
            statement.setObject(11,row.departmentId());
            statement.setObject(12,row.candidateSourceType());
            statement.setObject(13,row.candidateSourceIdsJson());
            statement.setObject(14,row.frozenUserIdsJson());
            statement.setObject(15,row.approveMethod());
            statement.setObject(16,row.approveRatio());
            statement.setObject(17,row.requireAllApprovals());
            statement.setObject(18,row.createdAt());
            return statement;
        },keys);
        if(count!=1 || keys.getKey()==null || keys.getKey().longValue()<=0) throw new IllegalStateException("Submitted form evidence insert did not return one real row id");
        return keys.getKey().longValue();
    }
    public List<DccObsoleteFormRuntimeObligationDO> listByHead(Long tenant,Long head) {
        return jdbc.query("SELECT * FROM dcc_obsolete_form_runtime_obligation WHERE tenant_id=? AND head_id=? ORDER BY stage_no,stage_order,id",ROW,tenant,head);
    }
    public List<DccObsoleteFormRuntimeObligationDO> listByBpm(Long tenant,Long file,String bpm) {
        return jdbc.query("SELECT * FROM dcc_obsolete_form_runtime_obligation WHERE tenant_id=? AND controlled_file_id=? AND bpm_process_instance_id=? ORDER BY stage_no,stage_order,id",ROW,tenant,file,bpm);
    }
}
