package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;

/** All SQL carries the tenant explicitly, including lock reads and count queries. */
@Repository
public class DccRelationStore {
    private final JdbcTemplate jdbc;
    private final GxpAuditService audit;
    public DccRelationStore(JdbcTemplate jdbc, GxpAuditService audit) { this.jdbc = jdbc; this.audit = audit; }
    public JdbcTemplate jdbc() { return jdbc; }

    /** Stabilize B-owned facts before invoking B's authority checks; no duplicate permission rules. */
    public void lockReferenceContext(Long tenantId,Long projectId,Long folderId){
        jdbc.queryForList("SELECT id FROM dcc_project_code WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE",
                Long.class,tenantId,projectId);
        jdbc.queryForList("SELECT id FROM dcc_project_folder WHERE tenant_id=? AND project_code_id=? AND id=? AND deleted=0 FOR UPDATE",
                Long.class,tenantId,projectId,folderId);
        // Missing or inactive identities are rejected by the same formal B services, with B's errors.
    }

    public void lockMaster(Long tenantId, Long masterId) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM dcc_controlled_file_master "
                + "WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE", Long.class, tenantId, masterId);
        if (ids.size() != 1) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_MASTER_NOT_FOUND");
    }
    public void assertCurrentRelationsInitialized(Long tenantId, Long masterId) {
        if(jdbc.queryForList("SELECT controlled_file_id FROM dcc_current_file_relation_set WHERE tenant_id=? AND source_master_id=?",
                Long.class,tenantId,masterId).size()!=1) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_CURRENT_SET_NOT_INITIALIZED");
    }
    public Reference reference(Long tenantId, Long projectId, Long folderId, Long masterId) {
        return jdbc.query("SELECT * FROM dcc_project_file_reference WHERE tenant_id=? AND project_id=? "
                        + "AND folder_id=? AND master_id=? FOR UPDATE", (rs, n) -> new Reference(rs.getLong("id"),
                        rs.getLong("project_id"), rs.getLong("folder_id"), rs.getLong("master_id"),
                        rs.getLong("selected_controlled_file_id"), rs.getLong("created_by")),
                tenantId, projectId, folderId, masterId).stream().findFirst().orElse(null);
    }
    public List<Reference> references(Long tenantId, Long projectId, Long folderId) {
        return jdbc.query("SELECT * FROM dcc_project_file_reference WHERE tenant_id=? AND project_id=? "
                        + "AND folder_id=? ORDER BY id", (rs, n) -> new Reference(rs.getLong("id"),
                        rs.getLong("project_id"), rs.getLong("folder_id"), rs.getLong("master_id"),
                        rs.getLong("selected_controlled_file_id"), rs.getLong("created_by")),
                tenantId, projectId, folderId);
    }
    public long referenceProjectCount(Long tenantId, Long masterId, Long selectedFileId) {
        return jdbc.queryForObject("SELECT COUNT(DISTINCT project_id) FROM dcc_project_file_reference "
                + "WHERE tenant_id=? AND master_id=? AND selected_controlled_file_id=?", Long.class, tenantId, masterId, selectedFileId);
    }
    private static final String USAGE_SCOPE=" WHERE r.tenant_id=? AND r.master_id=? AND r.selected_controlled_file_id=? AND r.project_id IN (%s) "
            + "AND EXISTS (SELECT 1 FROM dcc_project_code readable WHERE readable.id=r.project_id AND readable.tenant_id=r.tenant_id AND readable.deleted=0 AND readable.status='ENABLE') ";
    /** Count only integrity facts across the same tenant/Master; no forbidden destination labels are projected. */
    public void assertUsageIdentity(Long tenant,FileVersion source) {
        Long sourceCount=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file f JOIN dcc_controlled_file_master m ON m.id=f.master_id AND m.tenant_id=f.tenant_id AND m.deleted=0 WHERE f.id=? AND f.tenant_id=? AND f.deleted=0 AND f.master_id=? AND f.dcc_project_code_id=m.dcc_project_code_id",Long.class,source.controlledFileId(),tenant,source.masterId());
        if(sourceCount==null || sourceCount!=1)throw new DccRelationFailure("DCC_REFERENCE_SOURCE_IDENTITY_INVALID");
        Long invalid=jdbc.queryForObject("""
            SELECT COUNT(*) FROM dcc_project_file_reference r
            LEFT JOIN dcc_project_code p ON p.id=r.project_id AND p.tenant_id=r.tenant_id AND p.deleted=0
            LEFT JOIN dcc_project_folder d ON d.id=r.folder_id AND d.project_code_id=r.project_id AND d.tenant_id=r.tenant_id AND d.deleted=0
            LEFT JOIN dcc_controlled_file f ON f.id=r.selected_controlled_file_id AND f.master_id=r.master_id AND f.tenant_id=r.tenant_id AND f.deleted=0
            LEFT JOIN dcc_controlled_file_master m ON m.id=f.master_id AND m.tenant_id=f.tenant_id AND m.dcc_project_code_id=f.dcc_project_code_id AND m.deleted=0
            WHERE r.tenant_id=? AND r.selected_controlled_file_id=?
              AND (r.master_id<>? OR p.id IS NULL OR d.id IS NULL OR f.id IS NULL OR m.id IS NULL
                OR p.project_name IS NULL OR TRIM(p.project_name)='' OR d.name IS NULL OR TRIM(d.name)=''
                OR f.file_name IS NULL OR TRIM(f.file_name)='' OR f.file_number IS NULL OR TRIM(f.file_number)=''
                OR f.version_no IS NULL OR TRIM(f.version_no)='' OR f.status IS NULL OR TRIM(f.status)='')
            """,Long.class,tenant,source.controlledFileId(),source.masterId());
        if(invalid==null || invalid!=0)throw new DccRelationFailure("DCC_REFERENCE_USAGE_IDENTITY_INVALID");
    }
    public record UsageCounts(long projects,long rows) {}
    public UsageCounts visibleUsageCounts(Long tenant,Long master,Long selectedFileId,List<Long> projectIds) {
        if(projectIds.isEmpty())return new UsageCounts(0,0);
        String from=usageScope(projectIds);Object[] params=usageParameters(tenant,master,selectedFileId,projectIds).toArray();
        return jdbc.queryForObject("SELECT COUNT(DISTINCT r.project_id) AS projects,COUNT(*) AS reference_count FROM dcc_project_file_reference r"+from,
                (rs,n)->new UsageCounts(rs.getLong("projects"),rs.getLong("reference_count")),params);
    }
    /** Same authorization scope as count; fixed selected version, never a latest pointer join. */
    public List<ReferenceUsageRow> usagePage(Long tenant,Long master,Long selectedFileId,List<Long> projectIds,int size,long offset) {
        if(projectIds.isEmpty())return List.of();
        String from=usageScope(projectIds);var parameters=usageParameters(tenant,master,selectedFileId,projectIds);parameters.add(size);parameters.add(offset);
        String projection="""
            SELECT r.id,r.project_id,p.project_name,r.folder_id,d.name AS folder_name,r.master_id,r.selected_controlled_file_id,
              f.file_number,f.file_name,f.version_no,f.status,f.controlled_time,f.activated_time
            FROM dcc_project_file_reference r
            JOIN dcc_project_code p ON p.id=r.project_id AND p.tenant_id=r.tenant_id AND p.deleted=0
            JOIN dcc_project_folder d ON d.id=r.folder_id AND d.project_code_id=r.project_id AND d.tenant_id=r.tenant_id AND d.deleted=0
            JOIN dcc_controlled_file f ON f.id=r.selected_controlled_file_id AND f.master_id=r.master_id AND f.tenant_id=r.tenant_id AND f.deleted=0
            """;
        return jdbc.query(projection+from+" ORDER BY r.project_id,r.folder_id,r.id LIMIT ? OFFSET ?",(rs,n)->{
            String status=rs.getString("status");boolean controlled=rs.getTimestamp("controlled_time")!=null
                    && ("ACTIVE".equals(status)||"CONTROLLED_PENDING_EFFECTIVE".equals(status));
            return new ReferenceUsageRow(rs.getLong("id"),rs.getLong("project_id"),rs.getString("project_name"),rs.getLong("folder_id"),rs.getString("folder_name"),
                    rs.getLong("master_id"),rs.getLong("selected_controlled_file_id"),rs.getString("file_number"),rs.getString("file_name"),rs.getString("version_no"),status,
                    controlled,controlled && "CONTROLLED_PENDING_EFFECTIVE".equals(status),controlled && "ACTIVE".equals(status) && rs.getTimestamp("activated_time")!=null,false);
        },parameters.toArray());
    }
    private String usageScope(List<Long> projectIds){return USAGE_SCOPE.formatted(String.join(",",java.util.Collections.nCopies(projectIds.size(),"?")));}
    private java.util.ArrayList<Object> usageParameters(Long tenant,Long master,Long selectedFileId,List<Long> projectIds){var params=new java.util.ArrayList<Object>();params.add(tenant);params.add(master);params.add(selectedFileId);params.addAll(projectIds);return params;}
    /** Lock read excludes a stale MySQL REPEATABLE READ snapshot after waiting for the master lock. */
    public long referenceProjectCountForUpdate(Long tenantId, Long masterId, Long selectedFileId) {
        return jdbc.queryForList("SELECT project_id FROM dcc_project_file_reference WHERE tenant_id=? AND master_id=? AND selected_controlled_file_id=? FOR UPDATE",
                Long.class,tenantId,masterId,selectedFileId).stream().distinct().count();
    }
    public void audit(String operation, String subject, String reason, Object before, Object after) {
        if (reason == null || reason.isBlank()) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_RELATION_REASON_REQUIRED");
        audit.append(GxpAuditCommand.builder().operationId(operation).subjectId(subject).subjectVersion("1")
                .reason(reason).source("DCC_RELATIONS")
                .idempotencyKey(java.util.UUID.randomUUID().toString())
                .beforeState(envelope(before)).afterState(envelope(after)).build());
    }
    /** Signed relation evidence references the existing DCC projection, never the unified table ID. */
    public void auditSigned(String operation,String subject,Long actorId,String reason,Object before,Object after,
            DccRelationStageContracts.SignatureIdentity signature,String commandScope) {
        requireHumanActor(actorId);
        if(signature==null || signature.signatureId()==null || signature.signatureId()<=0 || signature.signedAt()==null
                || signature.evidenceHash()==null || signature.evidenceHash().isBlank())throw new DccRelationFailure("DCC_RELATION_AUDIT_SIGNATURE_REQUIRED");
        String reference="DCC_CONTROLLED_FILE_SIGNATURE:"+signature.signatureId();
        var afterFacts=new java.util.TreeMap<String,Object>();afterFacts.put("relation",after);
        afterFacts.put("signatureNamespace","DCC_CONTROLLED_FILE_SIGNATURE");afterFacts.put("signatureReference",reference);
        afterFacts.put("signatureEvidenceHash",signature.evidenceHash());
        afterFacts.put("signedAt",signature.signedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        audit.append(auditCommand(operation,subject,reason,before,afterFacts,"DCC_RELATION_STAGE:",commandScope)
                .signatureRecordId(reference).signatureContentHash(signature.evidenceHash()).build());
    }
    /** Draft source identity is a human-authorized metadata write without a new electronic signature. */
    public void auditHumanSource(String operation,String subject,Long actorId,String reason,Object before,Object after,String commandScope) {
        requireHumanActor(actorId);
        audit.append(auditCommand(operation,subject,reason,before,after,"DCC_RELATION_SOURCE:",commandScope).build());
    }
    private GxpAuditCommand.GxpAuditCommandBuilder auditCommand(String operation,String subject,String reason,Object before,Object after,String prefix,String scope) {
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new DccRelationFailure("DCC_RELATION_AUDIT_TRANSACTION_REQUIRED");
        if(reason==null || reason.isBlank() || scope==null || scope.isBlank())throw new DccRelationInputFailure("DCC_RELATION_AUDIT_SCOPE_REQUIRED");
        String key=prefix+cn.hutool.crypto.digest.DigestUtil.sha256Hex(scope);
        return GxpAuditCommand.builder().operationId(operation).subjectId(subject).subjectVersion("1").reason(reason)
                .source("DCC_RELATIONS").idempotencyKey(key).beforeState(envelope(before)).afterState(envelope(after));
    }
    private void requireHumanActor(Long actorId) {
        var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        Long tenant=cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        if(actorId==null || actorId<=0 || login==null || !java.util.Objects.equals(login.getId(),actorId)
                || !java.util.Objects.equals(login.getTenantId(),tenant) || !Integer.valueOf(2).equals(login.getUserType()))
            throw new DccRelationFailure("DCC_RELATION_AUDIT_ACTOR_MISMATCH");
    }
    private GxpAuditStateEnvelope envelope(Object value) {
        return GxpAuditStateEnvelope.builder().state(value == null ? "ABSENT" : "PRESENT")
                .objectVersion("1").canonicalJson(JsonUtils.toJsonString(value == null ? Map.of() : value)).build();
    }
}
