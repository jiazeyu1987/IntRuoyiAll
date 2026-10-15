package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccRelationStageSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccRelationStageMemberDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/** Dedicated append-only JDBC mapper. Every query explicitly scopes the tenant. */
@Repository
public class DccRelationStageSnapshotMapper {
    private final JdbcTemplate jdbc;
    public DccRelationStageSnapshotMapper(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final RowMapper<DccRelationStageSnapshotDO> HEADER=(r,n)->new DccRelationStageSnapshotDO(
            r.getLong("id"),r.getLong("tenant_id"),r.getLong("source_file_id"),r.getLong("source_master_id"),
            r.getLong("project_id"),r.getString("process_instance_id"),r.getString("task_id"),r.getString("obligation_id"),
            r.getString("stage_code"),r.getString("node_instance_id"),r.getObject("department_id",Long.class),
            r.getLong("actor_id"),r.getLong("signature_id"),r.getObject("signed_at",LocalDateTime.class),
            r.getString("command_key"),r.getString("payload_hash"),r.getString("expected_digest"),
            r.getString("complete_digest"),r.getString("action_type"),r.getString("reason"),
            r.getString("application_type"),r.getObject("attribute_round",Integer.class),r.getObject("form_action_instance_id",Long.class));
    private static final RowMapper<DccRelationStageMemberDO> MEMBER=(r,n)->new DccRelationStageMemberDO(
            r.getLong("tenant_id"),r.getLong("snapshot_id"),r.getLong("related_master_id"),r.getLong("selected_file_id"),
            r.getLong("project_id"),r.getString("file_number_snapshot"),r.getString("file_name_snapshot"),
            r.getString("version_no_snapshot"),r.getString("origin"),r.getBoolean("supplemental"),
            r.getObject("assignee_user_id",Long.class),r.getObject("due_at",LocalDateTime.class));
    public List<DccRelationStageSnapshotDO> findNode(Long tenant,String process,String task,String obligation) {
        return jdbc.query("SELECT * FROM dcc_relation_stage_snapshot WHERE tenant_id=? AND process_instance_id=? AND task_id=? AND obligation_id=? FOR UPDATE",
                HEADER,tenant,process,task,obligation);
    }
    public List<DccRelationStageSnapshotDO> listRound(Long tenant,Long file,String process) {
        return jdbc.query("SELECT head.* FROM dcc_relation_stage_snapshot head INNER JOIN dcc_controlled_file_signature signature "
                + "ON signature.id=head.signature_id AND signature.controlled_file_id=head.source_file_id "
                + "AND signature.process_instance_id=head.process_instance_id AND signature.task_id=head.task_id "
                + "AND signature.actor_id=head.actor_id AND signature.signed_at=head.signed_at AND signature.action_type=head.action_type "
                + "AND signature.evidence_status='VALID' AND signature.deleted=0 "
                + "WHERE head.tenant_id=? AND head.source_file_id=? AND head.process_instance_id=? ORDER BY head.id FOR UPDATE",
                HEADER,tenant,file,process);
    }
    public long countRound(Long tenant,Long file,String process) {
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_stage_snapshot WHERE tenant_id=? AND source_file_id=? AND process_instance_id=?",Long.class,tenant,file,process);
        if(count==null)throw new IllegalStateException("DCC_RELATION_STAGE_COUNT_MISSING");
        return count;
    }
    public List<DccRelationStageMemberDO> members(Long tenant,Long snapshot) {
        return jdbc.query("SELECT * FROM dcc_relation_stage_member WHERE tenant_id=? AND snapshot_id=? ORDER BY related_master_id",
                MEMBER,tenant,snapshot);
    }
    public Long appendHeader(DccRelationStageSnapshotDO h) {
        int rows=jdbc.update("INSERT INTO dcc_relation_stage_snapshot (tenant_id,source_file_id,source_master_id,project_id,process_instance_id,task_id,obligation_id,stage_code,node_instance_id,department_id,actor_id,signature_id,signed_at,command_key,payload_hash,expected_digest,complete_digest,action_type,reason,application_type,attribute_round,form_action_instance_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                h.tenantId(),h.sourceFileId(),h.sourceMasterId(),h.projectId(),h.processInstanceId(),h.taskId(),h.obligationId(),
                h.stageCode(),h.nodeInstanceId(),h.departmentId(),h.actorId(),h.signatureId(),h.signedAt(),h.commandKey(),
                h.payloadHash(),h.expectedDigest(),h.completeDigest(),h.actionType(),h.reason(),h.applicationType(),h.attributeRound(),h.formActionInstanceId());
        if(rows!=1)throw new IllegalStateException("DCC_RELATION_STAGE_HEADER_WRITE_FAILED");
        var saved=findNode(h.tenantId(),h.processInstanceId(),h.taskId(),h.obligationId());
        if(saved.size()!=1)throw new IllegalStateException("DCC_RELATION_STAGE_HEADER_ID_MISSING");
        return saved.get(0).id();
    }
    public void appendMember(DccRelationStageMemberDO m) {
        if(jdbc.update("INSERT INTO dcc_relation_stage_member (tenant_id,snapshot_id,related_master_id,selected_file_id,project_id,file_number_snapshot,file_name_snapshot,version_no_snapshot,origin,supplemental,assignee_user_id,due_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                m.tenantId(),m.snapshotId(),m.relatedMasterId(),m.selectedControlledFileId(),m.projectId(),m.fileNumberSnapshot(),
                m.fileNameSnapshot(),m.versionNoSnapshot(),m.origin(),m.supplemental(),m.assigneeUserId(),m.dueAt())!=1)
            throw new IllegalStateException("DCC_RELATION_STAGE_MEMBER_WRITE_FAILED");
    }
    public List<DccRelationStageSnapshotDO> listAllRoundForRead(Long tenant,Long file,String process) {
        return jdbc.query("SELECT * FROM dcc_relation_stage_snapshot WHERE tenant_id=? AND source_file_id=? AND process_instance_id=? ORDER BY id",
                HEADER,tenant,file,process);
    }
    public List<DccRelationStageSnapshotDO> listAllRoundForUpdate(Long tenant,Long file,String process) {
        return jdbc.query("SELECT * FROM dcc_relation_stage_snapshot WHERE tenant_id=? AND source_file_id=? AND process_instance_id=? ORDER BY id FOR UPDATE",
                HEADER,tenant,file,process);
    }
    public List<DccRelationStageMemberDO> membersForUpdate(Long tenant,Long snapshot) {
        return jdbc.query("SELECT * FROM dcc_relation_stage_member WHERE tenant_id=? AND snapshot_id=? ORDER BY related_master_id FOR UPDATE",
                MEMBER,tenant,snapshot);
    }
}
