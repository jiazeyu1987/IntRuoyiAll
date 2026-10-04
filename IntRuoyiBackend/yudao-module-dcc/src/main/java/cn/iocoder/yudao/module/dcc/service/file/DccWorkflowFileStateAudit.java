package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/** File-state evidence adapter. The official GxP kernel owns policy, actor, time and ledger hashes. */
@Service
public class DccWorkflowFileStateAudit {
    @Resource private GxpAuditService auditService;
    @Resource private JdbcTemplate jdbcTemplate;

    public record Snapshot(Long tenantId, Long fileId, Long masterId, String version, String status, String json) {}

    Snapshot capture(DccControlledFileDO file, DccControlledFileMasterDO master) {
        requireTransaction();
        if (file == null || master == null || file.getId() == null || master.getId() == null)
            throw new IllegalStateException("文件状态审计身份缺失");
        var persisted = snapshot(file.getId(),master.getId());
        if (!Objects.equals(persisted.version(),file.getVersionNo()))
            throw new IllegalStateException("文件状态审计锁定事实不匹配");
        return persisted;
    }

    private Snapshot snapshot(Long fileId, Long masterId) {
        requireTransaction();
        Long tenant = TenantContextHolder.getRequiredTenantId();
        // Direct locking reads on the same Spring connection are deliberate: MyBatis SESSION cache
        // can return the caller's in-memory object after finalization mutates its artifact fields.
        var master = jdbcTemplate.queryForObject("""
                SELECT status,latest_controlled_file_id,current_active_controlled_file_id
                FROM dcc_controlled_file_master WHERE id=? AND tenant_id=? AND deleted=0 FOR UPDATE
                """, (rs,n)-> {
            var result=new TreeMap<String,Object>();result.put("masterStatus",rs.getString("status"));
            result.put("latestControlledFileId",rs.getObject("latest_controlled_file_id",Long.class));
            result.put("currentActiveControlledFileId",rs.getObject("current_active_controlled_file_id",Long.class));return result;
        },masterId,tenant);
        var facts = jdbcTemplate.queryForObject("""
                SELECT master_id,file_number,version_no,status,process_instance_id,effective_date,
                  controlled_time,activated_time,published_time,published_file_id,stamped_file_id,
                  distributed_time,obsoleted_time,obsoleted_by,obsolete_reason,superseded_by_file_id
                FROM dcc_controlled_file WHERE id=? AND tenant_id=? AND deleted=0 FOR UPDATE
                """, (rs,n)-> {
            if (!Objects.equals(masterId,rs.getObject("master_id",Long.class)))
                throw new IllegalStateException("文件状态审计Master身份不匹配");
            var result=new TreeMap<String,Object>();
            result.put("fileNumber",rs.getString("file_number"));result.put("versionNo",rs.getString("version_no"));
            result.put("status",rs.getString("status"));result.put("approvalProcessInstanceId",rs.getString("process_instance_id"));
            LocalDate date=rs.getObject("effective_date",LocalDate.class);result.put("effectiveDate",date==null?null:date.toString());
            result.put("controlledTime",timestamp(rs,"controlled_time"));result.put("activatedTime",timestamp(rs,"activated_time"));
            result.put("publishedTime",timestamp(rs,"published_time"));result.put("distributedTime",timestamp(rs,"distributed_time"));
            result.put("obsoletedTime",timestamp(rs,"obsoleted_time"));result.put("obsoleteReason",rs.getString("obsolete_reason"));
            result.put("obsoletedBy",rs.getObject("obsoleted_by",Long.class));result.put("publishedFileId",rs.getObject("published_file_id",Long.class));
            result.put("stampedFileId",rs.getObject("stamped_file_id",Long.class));result.put("supersededByFileId",rs.getObject("superseded_by_file_id",Long.class));
            return result;
        },fileId,tenant);
        if (master == null || facts == null || facts.get("versionNo") == null || facts.get("versionNo").toString().isBlank()
                || facts.get("status") == null || facts.get("status").toString().isBlank())
            throw new IllegalStateException("文件状态审计版本或状态缺失");
        var claims = jdbcTemplate.queryForList("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE tenant_id=? AND master_id=? AND deleted=0 AND source_original_file_name IS NOT NULL",
                LocalDateTime.class, tenant, masterId);
        var legacy=jdbcTemplate.queryForList("""
            SELECT e.source_original_file_name,e.obsolete_time,e.retain_until,e.released_time,e.verification_scope_id
            FROM dcc_legacy_source_name_evidence e
            JOIN dcc_controlled_file_name_claim c ON c.tenant_id=e.tenant_id AND c.id=e.legacy_claim_id
            WHERE e.tenant_id=? AND e.legacy_master_id=? AND
            """+"EXISTS(SELECT 1 FROM dcc_legacy_source_name_scope s WHERE s.id=e.verification_scope_id AND "+cn.iocoder.yudao.module.dcc.dal.mysql.file.DccLegacySourceNameSql.VALID_SCOPE+") ORDER BY e.source_name_key,e.controlled_file_id",tenant,masterId);
        var deadlines=new java.util.LinkedHashSet<LocalDateTime>(claims);
        var retentionFacts=new java.util.ArrayList<TreeMap<String,Object>>();
        for(var row:legacy) {
            LocalDateTime until=row.get("retain_until")==null?null:((java.sql.Timestamp)row.get("retain_until")).toLocalDateTime();
            deadlines.add(until);
            var retained=new TreeMap<String,Object>();retained.put("sourceName",row.get("source_original_file_name"));
            retained.put("scopeId",String.valueOf(row.get("verification_scope_id")));
            retained.put("obsoleteTime",row.get("obsolete_time")==null?null:((java.sql.Timestamp)row.get("obsolete_time")).toLocalDateTime().toString());
            retained.put("retainUntil",until==null?null:until.toString());
            retained.put("releasedTime",row.get("released_time")==null?null:((java.sql.Timestamp)row.get("released_time")).toLocalDateTime().toString());
            if(!retentionFacts.contains(retained))retentionFacts.add(retained);
        }
        facts.putAll(master);
        facts.put("tenantId", tenant);facts.put("controlledFileId",fileId);facts.put("masterId",masterId);
        facts.put("retainUntil",deadlines.size()==1 && deadlines.iterator().next()!=null?deadlines.iterator().next().toString():null);
        if(!retentionFacts.isEmpty())facts.put("legacyNameRetention",retentionFacts);
        if(claims.size()>1)facts.put("modernNameRetention",claims.stream().map(value->value==null?null:value.toString()).toList());
        facts.put("schemaVersion", "dcc.file-state.v1");
        return new Snapshot(tenant,fileId,masterId,facts.get("versionNo").toString(),facts.get("status").toString(),JsonUtils.toJsonString(facts));
    }

    private String timestamp(ResultSet rs,String column) throws SQLException {
        LocalDateTime value=rs.getObject(column,LocalDateTime.class);return value==null?null:value.toString();
    }

    private Snapshot after(Snapshot before) {
        // The caller already holds this Master and its selected File locks in the business transaction.
        var result = snapshot(before.fileId(),before.masterId());
        if (!Objects.equals(before.masterId(), result.masterId()) || !Objects.equals(before.version(), result.version()))
            throw new IllegalStateException("文件状态审计前后身份不匹配");
        return result;
    }

    @GxpWriteOperation(operationId = "dcc.controlled-file.control")
    public void recordControl(Snapshot before, String round) {
        append("dcc.controlled-file.control", before, round, "正式审批轮次受控文件生成完成", false);
    }

    @GxpWriteOperation(operationId = "dcc.controlled-file.activate")
    public void recordActivation(Snapshot before, String round) {
        append("dcc.controlled-file.activate", before, round, "系统按预设生效日期完成正式版本生效", true);
    }

    @GxpWriteOperation(operationId = "dcc.controlled-file.auto-obsolete")
    public void recordAutomaticObsolete(Snapshot before, String newVersionRound) {
        append("dcc.controlled-file.auto-obsolete", before, newVersionRound, "新版生效自动作废", true);
    }

    @GxpWriteOperation(operationId = "dcc.controlled-file.obsolete")
    public void recordApprovedObsolete(Snapshot before, String approvedRound, String reason) {
        append("dcc.controlled-file.obsolete", before, approvedRound, reason, false);
    }

    private void append(String operation, Snapshot before, String round, String reason, boolean systemAction) {
        requireTransaction();
        if (before == null || !Objects.equals(before.tenantId(), TenantContextHolder.getRequiredTenantId())
                || round == null || round.isBlank() || reason == null || reason.isBlank())
            throw new IllegalStateException("文件状态审计真实轮次、原因或租户缺失");
        var login = SecurityFrameworkUtils.getLoginUser();
        if (login != null && !Objects.equals(login.getTenantId(), before.tenantId()))
            throw new IllegalStateException("文件状态审计安全上下文租户不匹配");
        var after = after(before);
        var command = GxpAuditCommand.builder().operationId(operation)
                .subjectId("CONTROLLED_FILE:" + before.fileId()).subjectVersion(before.version()).reason(reason)
                .beforeState(envelope(before)).afterState(envelope(after))
                .requestId(round).source(DccWorkflowFileStateAudit.class.getName())
                .idempotencyKey("DCC:A:STATE:" + DigestUtil.sha256Hex(JsonUtils.toJsonString(
                        List.of(before.tenantId(), operation, before.fileId(), before.version(), round))))
                .build();
        if (!systemAction) { auditService.append(command); return; }
        // Automatic effects are system facts even when triggered during a human doc-control request.
        // Do not impersonate the approver or attach a manufactured electronic signature.
        var original = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
            auditService.append(command);
        } finally { SecurityContextHolder.setContext(original); }
    }

    private GxpAuditStateEnvelope envelope(Snapshot value) {
        return GxpAuditStateEnvelope.builder().state("OBSOLETE".equals(value.status()) ? "VOIDED" : "PRESENT")
                .objectVersion(value.version()).canonicalJson(value.json()).build();
    }

    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("文件状态账本必须与业务同事务");
    }
}
