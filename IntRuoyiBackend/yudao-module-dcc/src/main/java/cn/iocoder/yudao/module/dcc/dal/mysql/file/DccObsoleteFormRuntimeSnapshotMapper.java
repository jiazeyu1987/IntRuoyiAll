package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccObsoleteFormRuntimeSnapshotDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

/** Append-only repository; exact tenant identities and complete collections, no update/delete API. */
@Repository
public class DccObsoleteFormRuntimeSnapshotMapper {
    private final JdbcTemplate jdbc;
    public DccObsoleteFormRuntimeSnapshotMapper(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final RowMapper<DccObsoleteFormRuntimeSnapshotDO> ROW=(r,n)->new DccObsoleteFormRuntimeSnapshotDO(
        r.getObject("id",Long.class),
        r.getObject("tenant_id",Long.class),
        r.getObject("form_instance_id",Long.class),
        r.getObject("controlled_file_id",Long.class),
        r.getObject("project_id",Long.class),
        r.getObject("applicant_user_id",Long.class),
        r.getString("form_instance_code"),
        r.getString("controlled_file_version_no"),
        r.getString("bpm_process_instance_id"),
        r.getObject("attribute_round",Integer.class),
        r.getString("prepared_process_definition_id"),
        r.getString("prepared_definition_digest"),
        r.getString("execution_contract"),
        r.getString("canonical_preparation_json"),
        r.getString("preparation_digest"),
        r.getObject("created_at",LocalDateTime.class));
    public long insert(DccObsoleteFormRuntimeSnapshotDO row) {
        if(row==null || row.id()!=null) throw new IllegalArgumentException("New submitted evidence must not supply a row id");
        var keys=new GeneratedKeyHolder();
        int count=jdbc.update(connection->{
            var statement=connection.prepareStatement("INSERT INTO dcc_obsolete_form_runtime_snapshot (tenant_id,form_instance_id,controlled_file_id,project_id,applicant_user_id,form_instance_code,controlled_file_version_no,bpm_process_instance_id,attribute_round,prepared_process_definition_id,prepared_definition_digest,execution_contract,canonical_preparation_json,preparation_digest,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
            statement.setObject(1,row.tenantId());
            statement.setObject(2,row.formInstanceId());
            statement.setObject(3,row.controlledFileId());
            statement.setObject(4,row.projectId());
            statement.setObject(5,row.applicantUserId());
            statement.setObject(6,row.formInstanceCode());
            statement.setObject(7,row.controlledFileVersionNo());
            statement.setObject(8,row.bpmProcessInstanceId());
            statement.setObject(9,row.attributeRound());
            statement.setObject(10,row.preparedProcessDefinitionId());
            statement.setObject(11,row.preparedDefinitionDigest());
            statement.setObject(12,row.executionContract());
            statement.setObject(13,row.canonicalPreparationJson());
            statement.setObject(14,row.preparationDigest());
            statement.setObject(15,row.createdAt());
            return statement;
        },keys);
        if(count!=1 || keys.getKey()==null || keys.getKey().longValue()<=0) throw new IllegalStateException("Submitted form evidence insert did not return one real row id");
        return keys.getKey().longValue();
    }
    public List<DccObsoleteFormRuntimeSnapshotDO> listByForm(Long tenant,Long form) {
        return jdbc.query("SELECT * FROM dcc_obsolete_form_runtime_snapshot WHERE tenant_id=? AND form_instance_id=? ORDER BY id",ROW,tenant,form);
    }
    public List<DccObsoleteFormRuntimeSnapshotDO> listByBpm(Long tenant,Long file,String bpm) {
        return jdbc.query("SELECT * FROM dcc_obsolete_form_runtime_snapshot WHERE tenant_id=? AND controlled_file_id=? AND bpm_process_instance_id=? ORDER BY id",ROW,tenant,file,bpm);
    }
}
