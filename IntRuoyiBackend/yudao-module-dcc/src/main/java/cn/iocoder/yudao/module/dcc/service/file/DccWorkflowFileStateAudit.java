package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
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
                  effective_mode,effective_time,effective_signature_id,effective_process_instance_id,
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
            result.put("effectiveMode",rs.getString("effective_mode"));result.put("effectiveTime",timestamp(rs,"effective_time"));
            result.put("effectiveSignatureId",rs.getObject("effective_signature_id",Long.class));
            result.put("effectiveProcessInstanceId",rs.getString("effective_process_instance_id"));
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
        if("DELAYED".equals(facts.get("effectiveMode"))) {
            var confirmation=jdbcTemplate.queryForObject("""
                SELECT delayed_effective_time,delayed_effective_confirmed_at,delayed_effective_signature_id,delayed_effective_process_instance_id
                FROM dcc_controlled_file WHERE id=? AND tenant_id=? AND deleted=0 FOR UPDATE
                """,(rs,n)-> {var row=new TreeMap<String,Object>();
                    row.put("delayedEffectiveTime",timestamp(rs,"delayed_effective_time"));row.put("delayedEffectiveConfirmedAt",timestamp(rs,"delayed_effective_confirmed_at"));
                    row.put("delayedEffectiveSignatureId",rs.getObject("delayed_effective_signature_id",Long.class));row.put("delayedEffectiveProcessInstanceId",rs.getString("delayed_effective_process_instance_id"));return row;},fileId,tenant);
            if(confirmation==null)throw new IllegalStateException("延迟确认审计锁定投影缺失");facts.putAll(confirmation);
        }
        var claims = jdbcTemplate.queryForList("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE tenant_id=? AND master_id=? AND deleted=0 AND source_original_file_name IS NOT NULL",
                LocalDateTime.class, tenant, masterId);
        var legacy=jdbcTemplate.query("""
            SELECT e.source_original_file_name,e.obsolete_time,e.retain_until,e.released_time,e.verification_scope_id
            FROM dcc_legacy_source_name_evidence e
            JOIN dcc_controlled_file_name_claim c ON c.tenant_id=e.tenant_id AND c.id=e.legacy_claim_id
            WHERE e.tenant_id=? AND e.legacy_master_id=? AND
            """+"EXISTS(SELECT 1 FROM dcc_legacy_source_name_scope s WHERE s.id=e.verification_scope_id AND "+cn.iocoder.yudao.module.dcc.dal.mysql.file.DccLegacySourceNameSql.VALID_SCOPE+") ORDER BY e.source_name_key,e.controlled_file_id",(rs,n)-> {
            var row=new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs,n);
            for(String column:List.of("retain_until","obsolete_time","released_time"))row.put(column,rs.getObject(column,LocalDateTime.class));return row;
        },tenant,masterId);
        var deadlines=new java.util.LinkedHashSet<LocalDateTime>(claims);
        var retentionFacts=new java.util.ArrayList<TreeMap<String,Object>>();
        for(var row:legacy) {
            LocalDateTime until=(LocalDateTime)row.get("retain_until");
            deadlines.add(until);
            var retained=new TreeMap<String,Object>();retained.put("sourceName",row.get("source_original_file_name"));
            retained.put("scopeId",String.valueOf(row.get("verification_scope_id")));
            retained.put("obsoleteTime",row.get("obsolete_time")==null?null:row.get("obsolete_time").toString());
            retained.put("retainUntil",until==null?null:until.toString());
            retained.put("releasedTime",row.get("released_time")==null?null:row.get("released_time").toString());
            if(!retentionFacts.contains(retained))retentionFacts.add(retained);
        }
        facts.putAll(master);
        facts.put("tenantId", tenant);facts.put("controlledFileId",fileId);facts.put("masterId",masterId);
        facts.put("retainUntil",deadlines.size()==1 && deadlines.iterator().next()!=null?deadlines.iterator().next().toString():null);
        if(!retentionFacts.isEmpty())facts.put("legacyNameRetention",retentionFacts);
        if(claims.size()>1)facts.put("modernNameRetention",claims.stream().map(value->value==null?null:value.toString()).toList());
        facts.put("schemaVersion", "dcc.file-state.v2");
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
        var facts=JsonUtils.parseObject(after(before).json(),TreeMap.class);
        var mode=facts.get("effectiveMode");
        String reason;
        if("DELAYED".equals(mode)) {
            if(facts.get("delayedEffectiveTime")==null || facts.get("delayedEffectiveConfirmedAt")==null || facts.get("delayedEffectiveSignatureId")==null
                    || !Objects.equals(facts.get("delayedEffectiveProcessInstanceId"),round) || facts.get("effectiveTime")==null
                    || !Objects.equals(facts.get("controlledTime"),facts.get("delayedEffectiveConfirmedAt")))
                throw new IllegalStateException("延迟生效日常启用审计缺少同轮确认签名、受控及实际生效事实");
            reason="系统在原编制人签名确认时受控，并在所选生效时刻到期后日常启用";
        } else if(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(mode)) {
            if(facts.get("effectiveTime")==null || facts.get("effectiveSignatureId")==null)
                throw new IllegalStateException("文控签名方式日常启用审计缺少真实已生效事实");
            reason="系统在文控签名已生效且正式受控完成后日常启用本版本";
        } else if(DccWorkflowDatePolicy.PRESET_DATE.equals(mode)) {
            if(facts.get("effectiveDate")==null || facts.get("effectiveTime")==null)
                throw new IllegalStateException("预设日期方式日常启用审计缺少受控到期生效事实");
            reason="系统在正式受控且预设日期到期生效后日常启用本版本";
        } else if(mode==null) reason="系统按已识别的旧日期流程契约日常启用正式受控版本";
        else throw new IllegalStateException("日常启用审计的正式生效方式未识别");
        append("dcc.controlled-file.activate", before, round, reason, true);
    }

    @GxpWriteOperation(operationId = "dcc.controlled-file.standardization-effective")
    public void recordSignatureEffect(Snapshot before,String round,DccControlledFileSignatureDO signature) {
        requireTransaction();
        if(before==null || signature==null || signature.getId()==null || signature.getId()<=0
                || !Objects.equals(before.fileId(),signature.getControlledFileId()) || !Objects.equals(before.fileId(),signature.getRevisionId())
                || !Objects.equals(before.version(),signature.getVersionNo()) || !Objects.equals(round,signature.getProcessInstanceId())
                || !"APPROVE".equals(signature.getActionType()) || !"DOC_CONTROL_REVIEW_APPROVE".equals(signature.getMeaningCode())
                || !"VALID".equals(signature.getEvidenceStatus()) || signature.getSignedAt()==null
                || !Boolean.TRUE.equals(signature.getPasswordVerified()) || signature.getEvidenceHash()==null || signature.getEvidenceHash().isBlank())
            throw new IllegalStateException("签名生效审计缺少同轮真实文控签名projection证据");
        var login=SecurityFrameworkUtils.getLoginUser();
        if(login==null || !Objects.equals(login.getId(),signature.getActorId()) || !Objects.equals(login.getTenantId(),before.tenantId()))
            throw new IllegalStateException("签名生效审计必须由本次真实文控签名账号记录");
        var persistedSignatures=jdbcTemplate.query("""
                SELECT controlled_file_id,revision_id,version_no,process_instance_id,task_id,actor_id,
                  action_type,meaning_code,password_verified,signed_at,evidence_status,evidence_hash,evidence_payload_version
                FROM dcc_controlled_file_signature WHERE id=? AND controlled_file_id=? AND deleted=0 FOR UPDATE
                """,(rs,n)->Objects.equals(rs.getObject("controlled_file_id",Long.class),signature.getControlledFileId())
                    && Objects.equals(rs.getObject("revision_id",Long.class),signature.getRevisionId())
                    && Objects.equals(rs.getString("version_no"),signature.getVersionNo())
                    && Objects.equals(rs.getString("process_instance_id"),round) && Objects.equals(rs.getString("task_id"),signature.getTaskId())
                    && Objects.equals(rs.getObject("actor_id",Long.class),signature.getActorId())
                    && "APPROVE".equals(rs.getString("action_type")) && "DOC_CONTROL_REVIEW_APPROVE".equals(rs.getString("meaning_code"))
                    && rs.getBoolean("password_verified") && Objects.equals(rs.getObject("signed_at",LocalDateTime.class),signature.getSignedAt())
                    && "VALID".equals(rs.getString("evidence_status")) && Objects.equals(rs.getString("evidence_hash"),signature.getEvidenceHash())
                    && "v4-workflow".equals(rs.getString("evidence_payload_version")),signature.getId(),before.fileId());
        if(persistedSignatures.size()!=1 || !Boolean.TRUE.equals(persistedSignatures.get(0)))
            throw new IllegalStateException("签名生效审计证据与锁定的真实DCC projection不一致");
        var beforeFacts=JsonUtils.parseObject(before.json(),TreeMap.class);
        var recorded=after(before);var afterFacts=JsonUtils.parseObject(recorded.json(),TreeMap.class);
        if(!DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(afterFacts.get("effectiveMode"))
                || !Objects.equals(beforeFacts.get("effectiveMode"),afterFacts.get("effectiveMode"))
                || afterFacts.get("effectiveDate")!=null || beforeFacts.get("effectiveTime")!=null
                || beforeFacts.get("effectiveSignatureId")!=null || beforeFacts.get("effectiveProcessInstanceId")!=null
                || !signature.getSignedAt().toString().equals(afterFacts.get("effectiveTime"))
                || !(afterFacts.get("effectiveSignatureId") instanceof Number id) || id.longValue()!=signature.getId()
                || !Objects.equals(round,afterFacts.get("effectiveProcessInstanceId"))
                || !Objects.equals(beforeFacts.get("activatedTime"),afterFacts.get("activatedTime"))
                || !Objects.equals(beforeFacts.get("controlledTime"),afterFacts.get("controlledTime"))
                || !Objects.equals(beforeFacts.get("status"),afterFacts.get("status"))
                || !Objects.equals(beforeFacts.get("currentActiveControlledFileId"),afterFacts.get("currentActiveControlledFileId")))
            throw new IllegalStateException("签名生效审计真实before/after与签名事实不一致");
        // Explicit domain namespace: this is the DCC HMAC projection, never a unified-signature table ID/hash.
        append("dcc.controlled-file.standardization-effective",before,round,"本轮文控标准化审核真实签名完成，记录实际生效事实",false,
                "DCC_CONTROLLED_FILE_SIGNATURE:"+signature.getId(),signature.getEvidenceHash(),recorded);
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
        append(operation,before,round,reason,systemAction,null,null,null);
    }

    @GxpWriteOperation(operationId="dcc.controlled-file.delayed-effective-confirm")
    public void recordDelayedConfirmation(Snapshot before,String round,DccControlledFileSignatureDO signature){
        requireTransaction();
        var login=SecurityFrameworkUtils.getLoginUser();
        if(before==null || signature==null || login==null || !Objects.equals(login.getId(),signature.getActorId())
                || !Objects.equals(login.getTenantId(),before.tenantId()) || !Objects.equals(signature.getControlledFileId(),before.fileId())
                || !Objects.equals(signature.getProcessInstanceId(),round) || signature.getSelectedEffectiveTime()==null || signature.getSignedAt()==null
                || !DccDelayedEffectiveConfirmationEvidence.MEANING.equals(signature.getMeaningCode()) || signature.getComment()==null || signature.getComment().isBlank())
            throw new IllegalStateException("延迟确认审计必须绑定本次原编制人的真实签名与原因");
        var saved=jdbcTemplate.queryForObject("""
            SELECT f.delayed_effective_time,f.delayed_effective_confirmed_at,f.delayed_effective_signature_id,f.delayed_effective_process_instance_id,
              s.selected_effective_time,s.attribute_round,s.signed_at,s.evidence_hash,s.actor_id,s.comment
            FROM dcc_controlled_file f JOIN dcc_controlled_file_signature s ON s.id=f.delayed_effective_signature_id AND s.controlled_file_id=f.id
            WHERE f.id=? AND f.tenant_id=? AND f.deleted=0 AND s.deleted=0 FOR UPDATE
            """,(rs,n)-> {
                if(!Objects.equals(rs.getObject("delayed_effective_signature_id",Long.class),signature.getId())
                        || !Objects.equals(rs.getObject("delayed_effective_time",LocalDateTime.class),signature.getSelectedEffectiveTime())
                        || !Objects.equals(rs.getObject("selected_effective_time",LocalDateTime.class),signature.getSelectedEffectiveTime())
                        || !Objects.equals(rs.getObject("delayed_effective_confirmed_at",LocalDateTime.class),signature.getSignedAt())
                        || !Objects.equals(rs.getObject("signed_at",LocalDateTime.class),signature.getSignedAt())
                        || !Objects.equals(rs.getString("delayed_effective_process_instance_id"),round)
                        || !Objects.equals(rs.getObject("attribute_round",Integer.class),signature.getAttributeRound())
                        || !Objects.equals(rs.getObject("actor_id",Long.class),signature.getActorId())
                        || !Objects.equals(rs.getString("comment"),signature.getComment()) || !Objects.equals(rs.getString("evidence_hash"),signature.getEvidenceHash()))
                    throw new IllegalStateException("延迟确认审计投影与真实锁定签名不一致");
                return Boolean.TRUE;
            },before.fileId(),before.tenantId());
        if(!Boolean.TRUE.equals(saved))throw new IllegalStateException("延迟确认审计证据未持久化");
        var after=after(before);var beforeFacts=JsonUtils.parseObject(before.json(),TreeMap.class);var afterFacts=JsonUtils.parseObject(after.json(),TreeMap.class);
        if(!"DELAYED".equals(afterFacts.get("effectiveMode")) || !Objects.equals(beforeFacts.get("effectiveMode"),afterFacts.get("effectiveMode"))
                || !Objects.equals(beforeFacts.get("controlledTime"),afterFacts.get("controlledTime")) || !Objects.equals(beforeFacts.get("activatedTime"),afterFacts.get("activatedTime")))
            throw new IllegalStateException("延迟确认审计不能隐式受控或启用");
        for(String field:java.util.List.of("delayedEffectiveTime","delayedEffectiveConfirmedAt","delayedEffectiveSignatureId","delayedEffectiveProcessInstanceId")) {
            if(beforeFacts.get(field)!=null)throw new IllegalStateException("延迟确认首次审计前已存在确认投影");
        }
        afterFacts.put("delayedEffectiveTime",signature.getSelectedEffectiveTime().toString());afterFacts.put("delayedEffectiveConfirmedAt",signature.getSignedAt().toString());
        afterFacts.put("delayedEffectiveSignatureId",signature.getId());afterFacts.put("delayedEffectiveProcessInstanceId",round);
        var enrichedBefore=new Snapshot(before.tenantId(),before.fileId(),before.masterId(),before.version(),before.status(),JsonUtils.toJsonString(beforeFacts));
        var enrichedAfter=new Snapshot(after.tenantId(),after.fileId(),after.masterId(),after.version(),after.status(),JsonUtils.toJsonString(afterFacts));
        append("dcc.controlled-file.delayed-effective-confirm",enrichedBefore,round,signature.getComment(),false,
                "DCC_CONTROLLED_FILE_SIGNATURE:"+signature.getId(),signature.getEvidenceHash(),enrichedAfter);
    }

    private void append(String operation, Snapshot before, String round, String reason, boolean systemAction,
                        String signatureRecordId,String signatureContentHash,Snapshot lockedAfter) {
        requireTransaction();
        if (before == null || !Objects.equals(before.tenantId(), TenantContextHolder.getRequiredTenantId())
                || round == null || round.isBlank() || reason == null || reason.isBlank())
            throw new IllegalStateException("文件状态审计真实轮次、原因或租户缺失");
        var login = SecurityFrameworkUtils.getLoginUser();
        if (login != null && !Objects.equals(login.getTenantId(), before.tenantId()))
            throw new IllegalStateException("文件状态审计安全上下文租户不匹配");
        var after = lockedAfter==null?after(before):lockedAfter;
        var command = GxpAuditCommand.builder().operationId(operation)
                .subjectId("CONTROLLED_FILE:" + before.fileId()).subjectVersion(before.version()).reason(reason)
                .beforeState(envelope(before)).afterState(envelope(after))
                .requestId(round).source(DccWorkflowFileStateAudit.class.getName())
                .signatureRecordId(signatureRecordId).signatureContentHash(signatureContentHash)
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
