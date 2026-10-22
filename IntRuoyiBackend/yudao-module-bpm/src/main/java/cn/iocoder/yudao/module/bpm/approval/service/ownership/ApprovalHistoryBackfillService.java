package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.dto.ApprovalProcessAuthorityDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalHistoryBackfillMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalProcessOwnershipMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Explicit, disabled-by-default migration; callers supply a batch and inspected digest, never owner facts. */
@Service
public class ApprovalHistoryBackfillService {
    public record Finding(String processId,String status,String reason,ApprovalProcessAuthorityDTO authority,
                          String sourceProof,List<Long> candidates) { }
    public record Report(String source,String batchId,Long tenantId,String status,String inspectionDigest,
                         String inventoryDigest,int runtimeTasks,int historicTasks,int initiatedInstances,int copies,
                         List<Finding> findings,LocalDateTime migratedAt) { }
    private record Inventory(List<Map<String,Object>> engines,List<Map<String,Object>> runtime,
                             List<Map<String,Object>> history,List<Map<String,Object>> copies,
                             List<Map<String,Object>> links,List<Map<String,Object>> sources,
                             List<Map<String,Object>> effects,List<Map<String,Object>> transitions) { }
    private final ApprovalHistoryBackfillMapper inventory;
    private final BpmApprovalProcessOwnershipMapper owners;
    private final PermissionApi permissions;
    private final HistoryService history;
    private final ProcessEngineConfiguration engine;
    private final SqlSessionFactory sessions;
    private final boolean enabled;

    public ApprovalHistoryBackfillService(ApprovalHistoryBackfillMapper inventory,
            BpmApprovalProcessOwnershipMapper owners,PermissionApi permissions,HistoryService history,
            ProcessEngineConfiguration engine,SqlSessionFactory sessions,
            @Value("${yudao.bpm.approval-history-backfill.enabled:false}") boolean enabled) {
        this.inventory=inventory; this.owners=owners; this.permissions=permissions; this.history=history;
        this.engine=engine; this.sessions=sessions; this.enabled=enabled;
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class)
    public Report inspect(Long actor,String batch) {
        Long tenant=guard(actor,batch); return inspect(tenant,batch,load(tenant),null);
    }

    @Transactional(isolation=Isolation.SERIALIZABLE,rollbackFor=Exception.class)
    public Report execute(Long actor,String batch,String inspectedDigest) {
        Long tenant=guard(actor,batch);
        require(inspectedDigest!=null && inspectedDigest.matches("[a-f0-9]{64}"),"INSPECTION_DIGEST_REQUIRED");
        require(Objects.equals(tenant,inventory.lockTenant(tenant)),"TENANT_LOCK_REQUIRED");
        var before=load(tenant); var report=inspect(tenant,batch,before,null);
        require(report.findings().stream().noneMatch(f->"BLOCKED".equals(f.status())),"TENANT_HISTORY_BLOCKED");
        var previous=inventory.batchLog(tenant,"AHB:"+batch);
        if(!previous.isEmpty()) {
            var summaries=previous.stream().filter(r->"SUMMARY".equals(text(r,"sub_type"))).toList();
            require(summaries.size()==1,"BATCH_AUDIT_INCOMPLETE");
            var summary=object(text(summaries.get(0),"extra"));
            require(inspectedDigest.equals(summary.path("inspectionDigest").asText())
                    && report.inventoryDigest().equals(summary.path("inventoryDigest").asText())
                    && report.findings().stream().allMatch(f->"EXISTING".equals(f.status()))
                    && previous.size()==report.findings().size()+1,"BATCH_REPLAY_MISMATCH");
            verifyAudit(previous,report);
            return inspect(tenant,batch,before,LocalDateTime.parse(summary.path("migratedAt").asText()));
        }
        require(inspectedDigest.equals(report.inspectionDigest()),"INSPECTION_STALE");
        LocalDateTime migratedAt=LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        for(var finding:report.findings()) {
            var engineRow=engineRow(before,finding.processId());
            var row=owners.selectExact(tenant,finding.processId());
            if(row==null) {
                row=fact(tenant,engineRow,finding.authority(),migratedAt);
                require(owners.insert(row)==1 && row.getId()!=null,"OWNER_INSERT_FAILED");
                for(Long user:finding.candidates())
                    require(owners.insertSelectedCandidate(tenant,finding.processId(),user)==1,"CANDIDATE_INSERT_FAILED");
            }
            exact(row,engineRow,finding.authority(),finding.candidates());
            exact(owners.selectExact(tenant,finding.processId()),engineRow,finding.authority(),finding.candidates());
            var audit=new TreeMap<String,Object>();
            audit.put("source","HISTORY_BACKFILL"); audit.put("processId",finding.processId());
            audit.put("originalWriter",row.getWriterCode()); audit.put("sourceProof",finding.sourceProof());
            audit.put("ownerDigest",row.getEvidenceDigest()); audit.put("candidateDigest",hash(finding.candidates()));
            audit.put("ownerRecordedAt",row.getCreatedAt().toString()); audit.put("migratedAt",migratedAt.toString());
            audit.put("operation",finding.status().equals("EXISTING")?"VERIFIED_EXISTING":"INSERTED_HISTORY");
            audit(tenant,batch,actor,"PROCESS",audit);
        }
        var after=load(tenant);
        require(hash(before).equals(hash(after)),"HISTORY_SOURCE_OR_AUTHORIZED_SET_CHANGED");
        var applied=inspect(tenant,batch,after,migratedAt);
        require(applied.findings().stream().allMatch(f->"EXISTING".equals(f.status())),"BACKFILL_READBACK_INCOMPLETE");
        var summary=new TreeMap<String,Object>(); summary.put("source","HISTORY_BACKFILL");
        summary.put("inspectionDigest",inspectedDigest); summary.put("inventoryDigest",report.inventoryDigest());
        summary.put("migratedAt",migratedAt.toString()); summary.put("processCount",report.findings().size());
        summary.put("runtimeTasks",report.runtimeTasks()); summary.put("historicTasks",report.historicTasks());
        summary.put("initiatedInstances",report.initiatedInstances()); summary.put("copies",report.copies());
        audit(tenant,batch,actor,"SUMMARY",summary);
        verifyAudit(inventory.batchLog(tenant,"AHB:"+batch),applied);
        return applied;
    }

    private Long guard(Long actor,String batch) {
        require(enabled,"MANUAL_HISTORY_BACKFILL_DISABLED");
        require(actor!=null && permissions.hasAnyRolesOrSuperAdmin(actor,"super_admin"),"SUPER_ADMIN_REQUIRED");
        require(batch!=null && batch.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,47}"),"CONTROLLED_BATCH_ID_REQUIRED");
        Long tenant=TenantContextHolder.getRequiredTenantId(); require(tenant>0,"CURRENT_TENANT_REQUIRED");
        require(TransactionSynchronizationManager.isActualTransactionActive(),"PHYSICAL_TRANSACTION_REQUIRED");
        require("mysql".equals(engine.getDatabaseType()) && "".equals(engine.getDatabaseTablePrefix()),"MYSQL_UNPREFIXED_ENGINE_REQUIRED");
        require(unwrap(engine.getDataSource())==unwrap(sessions.getConfiguration().getEnvironment().getDataSource()),
                "ENGINE_AND_MAPPER_SHARED_DATASOURCE_REQUIRED");
        return tenant;
    }
    private static DataSource unwrap(DataSource ds) {
        while(ds instanceof TransactionAwareDataSourceProxy proxy) ds=proxy.getTargetDataSource();
        require(ds!=null,"DATASOURCE_REQUIRED"); return ds;
    }
    private Inventory load(Long tenant) {
        String t=tenant.toString(); var sources=new ArrayList<Map<String,Object>>();
        add(sources,"BUSINESS_REQUEST",inventory.business(tenant,t));
        add(sources,"FORM_ACTION",inventory.forms(tenant,t));
        add(sources,"REGISTRATION_REQUEST",inventory.registrations(tenant,t));
        add(sources,"CONTROLLED_FILE",inventory.dccPointers(tenant,t));
        add(sources,"OA_LEAVE",inventory.leaves(tenant,t));
        add(sources,"CRM_CONTRACT",inventory.contracts(tenant,t));
        add(sources,"CRM_RECEIVABLE",inventory.receivables(tenant,t));
        var links=new ArrayList<>(inventory.runtimeLinks(t)); links.addAll(inventory.historicLinks(t));
        return new Inventory(inventory.engines(t),inventory.runtimeTasks(t),inventory.historicTasks(t),
                inventory.copies(tenant),links,sources,inventory.effects(tenant),inventory.dccTransitions(tenant));
    }
    private static void add(List<Map<String,Object>> dest,String type,List<Map<String,Object>> rows) {
        for(var row:rows) { var copy=new TreeMap<>(row); copy.put("source_type",type); dest.add(copy); }
    }

    private Report inspect(Long tenant,String batch,Inventory data,LocalDateTime migratedAt) {
        var required=new TreeSet<String>(); var taskRows=new ArrayList<>(data.runtime());taskRows.addAll(data.history());
        var findings=new ArrayList<Finding>();
        for(var task:taskRows) {
            String process=text(task,"PROC_INST_ID_");
            if(process==null || process.isBlank()) findings.add(block(text(task,"ID_"),"TASK_PROCESS_MISSING"));
            else required.add(process);
        }
        for(String process:required) {
            try {
                var e=engineRow(data,process); String def=text(e,"definition_id");
                require(tenant.toString().equals(text(e,"engine_tenant"))
                        && tenant.toString().equals(text(e,"definition_tenant")),"ENGINE_DEFINITION_TENANT_MISMATCH");
                for(var task:taskRows) if(process.equals(text(task,"PROC_INST_ID_")))
                    require(def.equals(text(task,"PROC_DEF_ID_")) && tenant.toString().equals(text(task,"TENANT_ID_")),"TASK_ENGINE_MISMATCH");
                findings.add(resolve(tenant,process,e,data));
            } catch(HistoryBlocked failure) { findings.add(block(process,failure.getMessage())); }
            // Missing tables, SQL/JSON failures and infrastructure exceptions deliberately propagate.
        }
        String setDigest=hash(data);
        String reportDigest=hash(List.of(tenant,batch,setDigest,findings));
        String status=findings.stream().anyMatch(f->"BLOCKED".equals(f.status()))?"BLOCKED":migratedAt==null?"READY":"APPLIED";
        return new Report("HISTORY_BACKFILL",batch,tenant,status,reportDigest,setDigest,data.runtime().size(),
                data.history().size(),(int)data.engines().stream().filter(e->"HISTORY".equals(text(e,"scope"))).count(),
                data.copies().size(),List.copyOf(findings),migratedAt);
    }

    private Finding resolve(Long tenant,String process,Map<String,Object> e,Inventory data) {
        var rows=data.sources().stream().filter(r->process.equals(sourceProcess(r))).toList();
        for(var r:rows) require(Objects.equals(tenant,number(r,"tenant_id")),"CROSS_TENANT_FORMAL_RELATION");
        var proven=new ArrayList<Finding>();
        for(var r:rows) {
            String type=text(r,"source_type");
            if(Set.of("BUSINESS_REQUEST","FORM_ACTION","REGISTRATION_REQUEST").contains(type))
                proven.add(formal(tenant,process,e,r));
        }
        require(proven.size()<=1,"COMPETING_FORMAL_SUBMISSION_AUTHORITIES");
        var old=owners.selectExact(tenant,process);
        if(old!=null) {
            ApprovalProcessOwnershipService.verify(old);
            var authority=new ApprovalProcessAuthorityDTO(tenant,old.getProjectionCode(),old.getAuthorityType(),
                    old.getAuthorityId(),old.getAuthorityVersion(),old.getWriterCode(),old.getBusinessKey());
            var candidates=owners.selectSelectedCandidates(tenant,process);
            exact(old,e,authority,candidates);
            if(!proven.isEmpty()) require(proven.get(0).authority().equals(authority),"EXISTING_OWNER_FORMAL_CONFLICT");
            // This is the already-controlled immutable start fact, never a guessed legacy owner.
            if(proven.isEmpty()) proven.add(new Finding(process,"EXISTING","CONTROLLED_EXISTING_START_FACT",authority,
                    hash(List.of(old.getEvidenceDigest(),old.getCreatedAt().toString())),List.copyOf(candidates)));
        }
        Finding answer=proven.isEmpty()?null:proven.get(0);
        for(var r:rows) {
            String type=text(r,"source_type");
            if(Set.of("BUSINESS_REQUEST","FORM_ACTION","REGISTRATION_REQUEST").contains(type)) continue;
            if(answer!=null && type.equals(answer.authority().authorityType())
                    && text(r,"id").equals(answer.authority().authorityId())
                    && ("CONTROLLED_FILE".equals(type)?Objects.equals(text(r,"version_no"),answer.authority().authorityVersion()):true)) continue;
            if("CONTROLLED_FILE".equals(type) && answer!=null && provenDccEffect(r,answer,data)) continue;
            throw blocked(type+"_ORIGINAL_SUBMISSION_PROOF_REQUIRED:"+text(r,"id"));
        }
        require(answer!=null,"ORIGINAL_NATIVE_START_PROOF_REQUIRED");
        if(old!=null) exact(old,e,answer.authority(),answer.candidates());
        else require(owners.selectSelectedCandidates(tenant,process).isEmpty(),"ORPHAN_SELECTED_CANDIDATES");
        return new Finding(process,old==null?"INSERT":"EXISTING",answer.reason(),answer.authority(),answer.sourceProof(),answer.candidates());
    }

    private Finding formal(Long tenant,String process,Map<String,Object> e,Map<String,Object> r) {
        String type=text(r,"source_type"),id=text(r,"id"),version,writer,projection,key;
        List<Long> candidates=List.of();
        if("REGISTRATION_REQUEST".equals(type)) {
            id=text(r,"request_id");
            require(Objects.equals(tenant,number(r,"request_tenant")) && process.equals(text(r,"request_process_id")),"REGISTRATION_REQUEST_BINDING_MISMATCH");
            key="DCC_REG_CERT_ACCESS:"+id;
            require(key.equals(text(r,"business_key")) && "dcc-registration-certificate-access".equals(text(e,"definition_key")),"REGISTRATION_ENGINE_MISMATCH");
            version=DigestUtil.sha256Hex(requiredText(r,"request_key"));
            candidates=selected(object(text(r,"detail_json")).path("candidateUserIds"));
            var historic=history.createHistoricVariableInstanceQuery().processInstanceId(process)
                    .variableName("PROCESS_START_USER_SELECT_ASSIGNEES").list();
            require(historic.size()==1,"REGISTRATION_ENGINE_SELECTION_REQUIRED");
            JsonNode selectedMap=JsonUtils.parseTree(JsonUtils.toJsonString(historic.get(0).getValue()));
            require(selectedMap!=null && selectedMap.isObject() && selectedMap.size()==1
                    && selectedMap.has("REG_CERT_ACCESS_APPROVAL"),"REGISTRATION_ENGINE_SELECTION_TASK_MISMATCH");
            require(candidates.equals(selected(selectedMap.path("REG_CERT_ACCESS_APPROVAL"))),"REGISTRATION_ENGINE_SELECTION_MISMATCH");
            writer="REGISTRATION_CERTIFICATE_SUBMIT"; projection="NATIVE_BUSINESS";
        } else {
            var context=object(text(r,"business_context_json"));
            require(Objects.equals(jsonLong(context.path("tenantId"),"SOURCE_CONTEXT_TENANT_MISMATCH"),tenant),"SOURCE_CONTEXT_TENANT_MISMATCH");
            for(String field:List.of("dataDomain","systemCode","objectType","objectId","objectVersion","actionCode","objectState"))
                require(Objects.equals(context.path(field).asText(null),text(r,snake(field))),"SOURCE_CONTEXT_FIELD_MISMATCH:"+field);
            version=requiredText(r,"object_version");
            if("FORM_ACTION".equals(type)) {
                require(r.get("submit_snapshot_id")!=null && context.equals(object(text(r,"submit_context_json"))),"FORM_SUBMIT_SNAPSHOT_REQUIRED");
                key="FORM_ACTION:"+requiredText(r,"instance_code"); writer="FORM_ACTION_SUBMIT"; projection="NATIVE_FORM";
                // Policy keys may change after submission; the engine's concrete definition is cross-checked
                // against the persisted original engine variables below, never today's policy.
                verifyHistoricVariable(process,"formInstanceId",id);
                verifyHistoricVariable(process,"objectVersion",version);
                verifyHistoricVariable(process,"objectId",requiredText(r,"object_id"));
            } else {
                Long applicant=jsonLong(context.path("applicantUserId"),"BUSINESS_REQUEST_ORIGINAL_APPLICANT_MISMATCH");
                require(Objects.equals(applicant,number(r,"applicant_user_id"))
                        && Objects.equals(text(e,"start_user"),applicant.toString()),"BUSINESS_REQUEST_ORIGINAL_APPLICANT_MISMATCH");
                require(requiredText(r,"process_definition_key").equals(text(e,"definition_key")),"BUSINESS_REQUEST_DEFINITION_MISMATCH");
                key="BUSINESS_APPROVAL:"+id;
                boolean edhr="MES".equals(text(r,"system_code")) && "EDHR_BATCH_EXECUTION".equals(text(r,"object_type")) && "SUBMIT_REVIEW".equals(text(r,"action_code"));
                writer=edhr?"EDHR_BUSINESS_APPROVAL_SUBMIT":"BUSINESS_APPROVAL_SUBMIT";
                projection=edhr?"NATIVE_EXTERNAL_EDHR":"NATIVE_BUSINESS";
                require(!edhr || "MES".equals(text(r,"data_domain")),"EDHR_FORMAL_DOMAIN_MISMATCH");
                if(edhr) require(Objects.equals(tenant,number(r,"execution_tenant"))
                        && Objects.equals(text(r,"object_id"),text(r,"execution_id"))
                        && Objects.equals(version,text(r,"execution_revision"))
                        && process.equals(text(r,"execution_process"))
                        && Objects.equals(text(e,"definition_key"),text(r,"execution_definition_key"))
                        && r.get("execution_snapshot_id")!=null
                        && process.equals(text(r,"execution_snapshot_process"))
                        && Objects.equals(text(e,"definition_key"),text(r,"execution_snapshot_key"))
                        && (text(r,"execution_snapshot_definition")==null || Objects.equals(text(e,"definition_id"),text(r,"execution_snapshot_definition"))),
                        "EDHR_EXACT_SUBMIT_OBJECT_EVIDENCE_REQUIRED");
            }
        }
        require(key.equals(text(e,"business_key")),"SOURCE_ENGINE_BUSINESS_KEY_MISMATCH");
        var authority=new ApprovalProcessAuthorityDTO(tenant,projection,type,id,version,writer,key);
        ApprovalProcessOwnershipRegistry.require(authority);
        return new Finding(process,"INSERT","VERIFIED_FORMAL_SUBMISSION",authority,hash(new TreeMap<>(r)),candidates);
    }
    private void verifyHistoricVariable(String process,String name,String expected) {
        var values=history.createHistoricVariableInstanceQuery().processInstanceId(process).variableName(name).list();
        require(values.size()==1 && Objects.equals(expected,String.valueOf(values.get(0).getValue())),"FORM_ENGINE_VARIABLE_MISMATCH:"+name);
    }

    /** Historical DCC_UPLOAD contract returned decimal fileId; its APPLIED row and exact approval event prove an effect. */
    private static boolean provenDccEffect(Map<String,Object> pointer,Finding owner,Inventory data) {
        if(!"FORM_ACTION".equals(owner.authority().authorityType())) return false;
        var forms=data.sources().stream().filter(r->"FORM_ACTION".equals(text(r,"source_type"))
                && owner.authority().authorityId().equals(text(r,"id")) && owner.processId().equals(sourceProcess(r))).toList();
        if(forms.size()!=1) return false;
        var form=forms.get(0); var context=object(text(form,"submit_context_json"));
        if(!"DCC".equals(context.path("systemCode").asText()) || !"CONTROLLED_FILE".equals(context.path("objectType").asText())
                || !"UPLOAD".equals(context.path("actionCode").asText())) return false;
        var submit=object(text(form,"submit_form_json"));
        if(!Objects.equals(text(pointer,"version_no"),submit.path("versionNo").asText(null))) return false;
        var effects=data.effects().stream().filter(r->owner.processId().equals(text(r,"bpm_process_instance_id"))
                && owner.authority().authorityId().equals(text(r,"instance_id")) && "APPLIED".equals(text(r,"status"))
                && Objects.equals(text(pointer,"id"),text(r,"result_ref"))
                && Objects.equals(text(r,"idempotency_key"),text(form,"idempotency_key"))
                && Objects.equals(text(r,"execution_code"),"EFFECT-"+text(form,"idempotency_key"))).toList();
        if(effects.size()!=1) return false;
        return data.transitions().stream().filter(a->Objects.equals(text(pointer,"id"),text(a,"native_version_id"))
                && Objects.equals(text(pointer,"version_no"),text(a,"version_no"))
                && owner.processId().equals(text(a,"approval_process_instance_id"))
                && "APPROVE".equals(text(a,"action"))
                && (text(form,"idempotency_key")+":approve").equals(text(a,"event_key"))).count()==1;
    }

    private void exact(BpmApprovalProcessOwnershipDO row,Map<String,Object> e,ApprovalProcessAuthorityDTO authority,List<Long> candidates) {
        ApprovalProcessOwnershipService.verify(row);
        var expected=fact(authority.tenantId(),e,authority,row.getCreatedAt());
        require(Objects.equals(row.getTenantId(),authority.tenantId())
                && row.getEvidenceDigest().equals(expected.getEvidenceDigest()),"OWNER_EXACT_READBACK_MISMATCH");
        require(candidates.equals(owners.selectSelectedCandidates(authority.tenantId(),row.getProcessInstanceId())),"SELECTED_CANDIDATES_EXACT_MISMATCH");
        require("REGISTRATION_REQUEST".equals(authority.authorityType()) || candidates.isEmpty(),"UNEXPECTED_SELECTED_CANDIDATES");
    }
    private static BpmApprovalProcessOwnershipDO fact(Long tenant,Map<String,Object> e,ApprovalProcessAuthorityDTO a,LocalDateTime recordedAt) {
        require(Objects.equals(text(e,"business_key"),a.expectedBusinessKey()),"OWNER_ENGINE_BUSINESS_KEY_MISMATCH");
        var row=new BpmApprovalProcessOwnershipDO(); row.setTenantId(tenant);row.setProcessInstanceId(requiredText(e,"process_id"));
        row.setProcessDefinitionId(requiredText(e,"definition_id"));row.setProcessDefinitionKey(requiredText(e,"definition_key"));
        row.setBusinessKey(a.expectedBusinessKey());row.setProjectionCode(a.projectionCode());row.setAuthorityType(a.authorityType());
        row.setAuthorityId(a.authorityId());row.setAuthorityVersion(a.authorityVersion());row.setWriterCode(a.writerCode());
        row.setCreatedAt(recordedAt);row.setEvidenceDigest(ApprovalProcessOwnershipService.digest(row));return row;
    }
    private static Map<String,Object> engineRow(Inventory data,String process) {
        var rows=data.engines().stream().filter(r->process.equals(text(r,"process_id"))).toList();
        require(!rows.isEmpty(),"TASK_ENGINE_INSTANCE_MISSING"); var first=rows.get(0);
        require(rows.size()<=2 && rows.stream().allMatch(r->List.of("definition_id","definition_key","business_key","engine_tenant","definition_tenant","start_user").stream()
                .allMatch(k->Objects.equals(text(first,k),text(r,k)))),"ENGINE_HISTORY_RUNTIME_CONFLICT");
        requiredText(first,"definition_id");requiredText(first,"definition_key");return first;
    }
    /** JsonUtils writes large Long values as canonical decimal text; neither branch coerces other JSON types. */
    private static Long jsonLong(JsonNode node,String reason) {
        require(node!=null,reason);
        if(node.isIntegralNumber()) {
            require(node.canConvertToLong(),reason);
            return node.longValue();
        }
        require(node.isTextual() && node.textValue().matches("0|-?[1-9][0-9]*"),reason);
        try { return Long.parseLong(node.textValue()); }
        catch(NumberFormatException invalid) { throw blocked(reason); }
    }

    private static List<Long> selected(JsonNode node) {
        require(node!=null && node.isArray() && !node.isEmpty(),"SELECTED_CANDIDATE_SNAPSHOT_REQUIRED");var users=new TreeSet<Long>();
        for(var value:node) {
            String id=value.isIntegralNumber() && value.canConvertToLong()?value.asText():value.isTextual()?value.textValue():null;
            require(id!=null && id.matches("[1-9][0-9]*"),"SELECTED_CANDIDATE_INVALID");
            long parsed;try { parsed=Long.parseLong(id); } catch(NumberFormatException invalid) { throw blocked("SELECTED_CANDIDATE_INVALID"); }
            require(users.add(parsed),"SELECTED_CANDIDATE_DUPLICATE");
        }return List.copyOf(users);
    }
    private void audit(Long tenant,String batch,Long actor,String kind,Map<String,Object> values) {
        String json=JsonUtils.toJsonString(values);require(json.length()<=2000,"AUDIT_REPORT_TOO_LONG");
        require(inventory.audit(tenant,"AHB:"+batch,actor,kind,json)==1,"MIGRATION_AUDIT_INSERT_FAILED");
    }
    private void verifyAudit(List<Map<String,Object>> logs,Report report) {
        var summaries=logs.stream().filter(r->"SUMMARY".equals(text(r,"sub_type"))).toList();
        require(summaries.size()==1 && logs.size()==report.findings().size()+1,"MIGRATION_BATCH_AUDIT_INCOMPLETE");
        var summary=object(text(summaries.get(0),"extra"));
        require("HISTORY_BACKFILL".equals(summary.path("source").asText())
                && report.inventoryDigest().equals(summary.path("inventoryDigest").asText())
                && report.findings().size()==summary.path("processCount").asInt(-1),"MIGRATION_BATCH_AUDIT_MISMATCH");
        var processLogs=logs.stream().filter(r->"PROCESS".equals(text(r,"sub_type"))).toList();
        require(processLogs.size()==report.findings().size(),"MIGRATION_AUDIT_READBACK_MISMATCH");
        var seen=new HashSet<String>();
        for(var r:processLogs) {
            var json=object(text(r,"extra")); String process=json.path("processId").asText();
            require(seen.add(process),"MIGRATION_AUDIT_DUPLICATE");
            var f=report.findings().stream().filter(v->process.equals(v.processId())).findFirst().orElseThrow(()->blocked("MIGRATION_AUDIT_UNKNOWN_PROCESS"));
            var row=owners.selectExact(report.tenantId(),process);
            require(row!=null,"MIGRATION_AUDIT_OWNER_MISSING");
            require("HISTORY_BACKFILL".equals(json.path("source").asText())
                    && Objects.equals(f.sourceProof(),json.path("sourceProof").asText())
                    && row.getEvidenceDigest().equals(json.path("ownerDigest").asText())
                    && f.authority().writerCode().equals(json.path("originalWriter").asText())
                    && hash(f.candidates()).equals(json.path("candidateDigest").asText()),"MIGRATION_AUDIT_FACT_MISMATCH");
        }
    }
    private static String sourceProcess(Map<String,Object> r) {
        return text(r,Set.of("FORM_ACTION","REGISTRATION_REQUEST").contains(text(r,"source_type"))?"bpm_process_instance_id":"process_instance_id");
    }
    private static String snake(String key) { return key.replaceAll("([A-Z])","_$1").toLowerCase(Locale.ROOT); }
    private static String text(Map<String,Object> row,String key) {Object value=row.get(key);return value==null?null:value.toString();}
    private static String requiredText(Map<String,Object> row,String key) {String value=text(row,key);require(value!=null&&!value.isBlank(),"SOURCE_FIELD_REQUIRED:"+key);return value;}
    private static Long number(Map<String,Object> row,String key) {Object value=row.get(key);return value instanceof Number n?n.longValue():null;}
    private static JsonNode object(String json) {var value=JsonUtils.parseTree(json);require(value!=null&&value.isObject(),"FORMAL_JSON_OBJECT_REQUIRED");return value;}
    private static String hash(Object value) {
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(canonical(JsonUtils.parseTree(JsonUtils.toJsonString(value)))));
    }
    private static Object canonical(JsonNode value) {
        if(value.isObject()) {var result=new TreeMap<String,Object>();value.fields().forEachRemaining(e->result.put(e.getKey(),canonical(e.getValue())));return result;}
        if(value.isArray()) {var result=new ArrayList<Object>();value.forEach(e->result.add(canonical(e)));return result;}
        return value;
    }
    private static Finding block(String process,String reason) {return new Finding(process,"BLOCKED",reason,null,null,List.of());}
    private static void require(boolean condition,String reason) {if(!condition) throw blocked(reason);}
    private static HistoryBlocked blocked(String reason) {return new HistoryBlocked("APPROVAL_HISTORY_"+reason);}
    private static final class HistoryBlocked extends IllegalStateException {private HistoryBlocked(String reason){super(reason);}}
}
