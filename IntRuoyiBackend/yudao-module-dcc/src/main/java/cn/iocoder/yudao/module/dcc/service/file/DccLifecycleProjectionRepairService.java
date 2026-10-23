package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import cn.iocoder.yudao.module.system.service.controlledcontent.*;
import cn.iocoder.yudao.module.system.enums.controlledcontent.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** Repairs only a precisely proven native controlled version's existing unfinished platform ref. */
@Service
public class DccLifecycleProjectionRepairService {
    public static final String OPERATION = "dcc.controlled-file.lifecycle-projection.repair";
    @Resource private DccControlledFileMapper files;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccControlledFileRouteSnapshotMapper routes;
    @Resource private DccControlledFileTaskAssigneeSnapshotMapper assignments;
    @Resource private DccControlledFileSignatureMapper signatures;
    @Resource private ControlledContentVersionRefMapper refs;
    @Resource private ControlledContentLifecycleCoreService core;
    @Resource private DccControlledFileAssignmentScopeService scope;
    @Resource private DccControlledFileVersionPolicy versionPolicy;
    @Resource private AdminUserApi users;
    @Resource private RoleApi roles;
    @Resource private PermissionApi permissions;
    @Resource private BpmProcessInstanceService processes;
    @Resource private BpmTaskService tasks;
    @Resource private DccElectronicSignatureManagementService verification;
    @Resource private FileService storage;
    @Resource private JdbcTemplate jdbc;
    @Resource private GxpAuditService audit;
    @Resource private GxpAuditEventMapper auditEvents;

    @Transactional(readOnly=true)
    public DccLifecycleProjectionRepairPreviewRespVO preview(Long actor,Long id) {
        requireActor(actor,id);
        DccControlledFileDO file=files.selectById(id);
        var proof=prove(file,file==null?null:masters.selectById(file.getMasterId()),false);
        var ref=proof.ref();
        boolean stale="FINALIZING".equals(ref.getCanonicalStatus()) && "FINALIZING".equals(ref.getDomainStatus())
                && Objects.equals(ref.getOpenCandidateUniqueFlag(),1) && ref.getActiveUniqueFlag()==null;
        boolean consistent=file.getStatus().equals(ref.getCanonicalStatus()) && file.getStatus().equals(ref.getDomainStatus())
                && ref.getOpenCandidateUniqueFlag()==null && Objects.equals(ref.getActiveUniqueFlag(),"ACTIVE".equals(file.getStatus())?1:null);
        if(!stale && !consistent)throw failure("DCC_REPAIR_UNSUPPORTED_PROJECTION_STATE");
        return new DccLifecycleProjectionRepairPreviewRespVO(str(file.getId()),str(file.getMasterId()),str(ref.getId()),str(file.getTenantId()),
                file.getVersionNo(),file.getStatus(),ref.getCanonicalStatus(),ref.getDomainStatus(),file.getProcessInstanceId(),
                str(file.getControlledTime()),str(file.getActivatedTime()),str(file.getApprovedTime()),str(file.getPublishedTime()),str(file.getEffectiveDate()),
                str(file.getPublishedFileId()),str(file.getStampedFileId()),proof.signatureIds(),str(proof.master().getLatestControlledFileId()),
                str(proof.master().getCurrentActiveControlledFileId()),proof.sourceHash(),proof.preimageHash(),stale,file.getStatus(),
                "ACTIVE".equals(file.getStatus())?List.of("COMPLETE_CONTROL","ACTIVATE_CONTROLLED"):List.of("COMPLETE_CONTROL"));
    }

    @GxpWriteOperation(operationId="dcc.controlled-file.lifecycle-projection.repair")
    @Transactional(rollbackFor=Exception.class)
    public DccLifecycleProjectionRepairRespVO repair(Long actor,Long id,DccLifecycleProjectionRepairReqVO request) {
        requireRequest(request);requireActor(actor,id);
        var identity=files.selectById(id);
        if(identity==null || identity.getMasterId()==null)throw failure("DCC_REPAIR_FILE_REQUIRED");
        var master=masters.selectByIdForUpdate(identity.getMasterId());
        var file=files.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),id);
        var proof=prove(file,master,true);
        if(!Objects.equals(request.getMasterId(),file.getMasterId()) || !Objects.equals(request.getVersionRefId(),proof.ref().getId())
                || !Objects.equals(request.getProcessInstanceId(),file.getProcessInstanceId())
                || !Objects.equals(request.getSourceFactsHash(),proof.sourceHash()))throw failure("DCC_REPAIR_SOURCE_FACTS_CHANGED");
        String commandHash=hash(List.of(actor,id,request));
        String key="DCC:PROJECTION:REPAIR:"+hash(List.of(file.getTenantId(),id,request.getIdempotencyKey()));
        var prior=auditEvents.selectByIdempotencyKey(file.getTenantId(),key);
        if(prior!=null) {
            var before=JsonUtils.parseObject(prior.getBeforeStateJson(),com.fasterxml.jackson.databind.JsonNode.class);
            var after=JsonUtils.parseObject(prior.getAfterStateJson(),com.fasterxml.jackson.databind.JsonNode.class);
            if(!OPERATION.equals(prior.getOperationId()) || !Objects.equals(prior.getActorId(),actor)
                    || !Objects.equals(prior.getSubjectId(),"CONTROLLED_FILE:"+id)
                    || !Objects.equals(prior.getReason(),request.getReason())
                    || !commandHash.equals(before.path("commandHash").asText())
                    || !request.getPreimageHash().equals(before.path("preimageHash").asText())
                    || !hash(proof.ref()).equals(after.path("projectionHash").asText())
                    || !file.getStatus().equals(proof.ref().getCanonicalStatus()))throw failure("DCC_REPAIR_REPLAY_CONFLICT");
            return receipt(file,proof.ref(),request,"REPLAY",prior.getId(),prior.getServerOccurredAt());
        }
        if(!request.getPreimageHash().equals(proof.preimageHash())
                || !"FINALIZING".equals(proof.ref().getCanonicalStatus()) || !"FINALIZING".equals(proof.ref().getDomainStatus())
                || !Objects.equals(proof.ref().getOpenCandidateUniqueFlag(),1) || proof.ref().getActiveUniqueFlag()!=null)
            throw failure("DCC_REPAIR_PREIMAGE_CHANGED");
        String event="DCC:REPAIR:"+hash(List.of(file.getTenantId(),file.getId(),request.getIdempotencyKey()));
        var content=key(file);
        core.transitionDccControlledRef(content,id,file.getVersionNo(),file.getProcessInstanceId(),ControlledContentCanonicalStatus.FINALIZING,
                ControlledContentCanonicalStatus.CONTROLLED_PENDING_EFFECTIVE,ControlledContentTransitionAction.COMPLETE_CONTROL,
                actor,request.getReason(),event+":CONTROLLED",null);
        if("ACTIVE".equals(file.getStatus()))core.transitionDccControlledRef(content,id,file.getVersionNo(),file.getProcessInstanceId(),
                ControlledContentCanonicalStatus.CONTROLLED_PENDING_EFFECTIVE,ControlledContentCanonicalStatus.ACTIVE,
                ControlledContentTransitionAction.ACTIVATE_CONTROLLED,actor,request.getReason(),event+":ACTIVATED",null);
        var actual=core.getVersionRef(content,id);
        var before=new TreeMap<String,Object>();before.put("commandHash",commandHash);before.put("preimageHash",proof.preimageHash());
        before.put("sourceFactsHash",proof.sourceHash());before.put("projection",proof.ref());
        var after=new TreeMap<String,Object>();after.put("projectionHash",hash(actual));after.put("projection",actual);
        after.put("sourceFactsHash",proof.sourceHash());
        var result=audit.append(GxpAuditCommand.builder().operationId(OPERATION).subjectId("CONTROLLED_FILE:"+id)
                .subjectVersion(file.getVersionNo()).reason(request.getReason()).requestId(file.getProcessInstanceId()).source(getClass().getName())
                .idempotencyKey(key).beforeState(envelope(file,before)).afterState(envelope(file,after)).build());
        if(result==null || result.eventId()==null || result.eventId()<=0)throw failure("DCC_REPAIR_AUDIT_EVENT_REQUIRED");
        var saved=auditEvents.selectById(result.eventId());
        if(saved==null || saved.getServerOccurredAt()==null)throw failure("DCC_REPAIR_AUDIT_TIME_REQUIRED");
        return receipt(file,actual,request,"REPAIRED",saved.getId(),saved.getServerOccurredAt());
    }

    private Proof prove(DccControlledFileDO file,DccControlledFileMasterDO master,boolean lock) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(file==null || master==null || !Objects.equals(file.getTenantId(),tenant) || !Objects.equals(master.getTenantId(),tenant)
                || !Objects.equals(file.getMasterId(),master.getId()) || !Objects.equals(file.getDccProjectCodeId(),master.getDccProjectCodeId())
                || Boolean.TRUE.equals(file.getDeleted()) || Boolean.TRUE.equals(master.getDeleted())
                || !Set.of("ACTIVE","CONTROLLED_PENDING_EFFECTIVE").contains(file.getStatus())
                || !Set.of(DccControlledFileProcessDefinitionKeys.UPLOAD,DccControlledFileProcessDefinitionKeys.REVISION)
                    .contains(Objects.toString(file.getProcessDefinitionKey(),""))
                || file.getApprovedTime()==null || file.getControlledTime()==null || file.getPublishedTime()==null
                || file.getEffectiveDate()==null || file.getProcessInstanceId()==null || file.getProcessInstanceId().isBlank()
                || file.getApprovedTime().isAfter(file.getControlledTime())
                || "ACTIVE".equals(file.getStatus()) && (file.getActivatedTime()==null || file.getControlledTime().isAfter(file.getActivatedTime())
                    || !Objects.equals(master.getCurrentActiveControlledFileId(),file.getId()))
                || "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()) && (file.getActivatedTime()!=null
                    || !Objects.equals(master.getLatestControlledFileId(),file.getId())))throw failure("DCC_REPAIR_NATIVE_FACTS_INVALID");
        var version=versionPolicy.parseStored(file);
        if(version==null || version.isWorkingIteration())throw failure("DCC_REPAIR_FORMAL_VERSION_REQUIRED");
        var history=processes.getHistoricProcessInstance(file.getProcessInstanceId());
        if(history==null || history.getEndTime()==null || processes.getProcessInstance(file.getProcessInstanceId())!=null
                || !file.getProcessInstanceId().equals(history.getId()) || !tenant.toString().equals(history.getTenantId())
                || !file.getId().toString().equals(history.getBusinessKey()) || !file.getProcessDefinitionKey().equals(history.getProcessDefinitionKey())
                || !Objects.equals(str(file.getRequesterId()),history.getStartUserId()) || history.getProcessVariables()==null
                || !Objects.equals(history.getProcessVariables().get("PROCESS_STATUS"),2))throw failure("DCC_REPAIR_APPROVED_BPM_REQUIRED");
        var signed=signatures.selectListByControlledFileId(file.getId());
        var signatureTenants=jdbc.queryForList("SELECT id FROM dcc_controlled_file_signature WHERE controlled_file_id=? AND tenant_id=?",Long.class,file.getId(),tenant);
        var actualSignatures=signed.stream().filter(row->signatureTenants.contains(row.getId())
                && Objects.equals(row.getProcessInstanceId(),file.getProcessInstanceId()) && "VALID".equals(row.getEvidenceStatus())
                && Set.of("APPROVE","ASSIGN").contains(Objects.toString(row.getActionType(),"")))
                .sorted(Comparator.comparing(DccControlledFileSignatureDO::getId)).toList();
        DccFrozenApprovalSignatures.requireComplete(file,routes.selectListByControlledFileId(file.getId()),actualSignatures,
                assignments.selectListByControlledFileId(file.getId()));
        for(var signature:actualSignatures) {
            var result=verification.verifySignatureEvidence(signature.getId());
            if(result==null || !Objects.equals(result.getSignatureId(),signature.getId()) || !"VALID".equals(result.getVerificationStatus()))
                throw failure("DCC_REPAIR_SIGNATURE_INVALID");
            if("APPROVE".equals(signature.getActionType())) {
                var task=tasks.getHistoricTask(signature.getTaskId());
                String meaning=signature.getMeaningCode();
                String stage=meaning!=null && meaning.endsWith("_APPROVE")?meaning.substring(0,meaning.length()-8):null;
                if(task==null || task.getEndTime()==null || !file.getProcessInstanceId().equals(task.getProcessInstanceId())
                        || !Objects.equals(task.getId(),signature.getTaskId()) || !Objects.equals(task.getTaskDefinitionKey(),stage)
                        || !tenant.toString().equals(task.getTenantId()) || !str(signature.getActorId()).equals(task.getAssignee())
                        || task.getTaskLocalVariables()==null || !Objects.equals(task.getTaskLocalVariables().get("TASK_STATUS"),2))
                    throw failure("DCC_REPAIR_SIGNED_TASK_REQUIRED");
            }
        }
        String published=artifactHash(file.getPublishedFileId()),stamped=artifactHash(file.getStampedFileId());
        var events=jdbc.queryForList("SELECT * FROM dcc_workflow_lifecycle_event WHERE tenant_id=? AND controlled_file_id=? AND approval_process_instance_id=? ORDER BY id",
                tenant,file.getId(),file.getProcessInstanceId());
        var controlled=events.stream().filter(event->"CONTROLLED".equals(event.get("event_type"))).toList();
        var activated=events.stream().filter(event->"ACTIVATED".equals(event.get("event_type"))).toList();
        if(controlled.size()!=1 || !exactEvent(controlled.get(0),file,"CONTROLLED",file.getControlledTime())
                || "ACTIVE".equals(file.getStatus()) && (activated.size()!=1 || !exactEvent(activated.get(0),file,"ACTIVATED",file.getActivatedTime()))
                || "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()) && !activated.isEmpty())throw failure("DCC_REPAIR_LIFECYCLE_EVENT_REQUIRED");
        var content=key(file);
        var chain=lock?refs.selectChainForUpdate(tenant,content.getContentType().name(),content.getContentKey())
                :refs.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ControlledContentVersionRefDO>()
                    .eq(ControlledContentVersionRefDO::getTenantId,tenant).eq(ControlledContentVersionRefDO::getContentType,content.getContentType().name())
                    .eq(ControlledContentVersionRefDO::getContentKey,content.getContentKey()).orderByAsc(ControlledContentVersionRefDO::getId));
        var matches=chain.stream().filter(row->Objects.equals(row.getNativeVersionId(),file.getId())).toList();
        if(matches.size()!=1)throw failure("DCC_REPAIR_EXACT_REF_REQUIRED");
        var ref=matches.get(0);
        if(!Objects.equals(ref.getNativeMasterId(),file.getMasterId()) || !Objects.equals(ref.getVersionNo(),file.getVersionNo())
                || !Objects.equals(ref.getApprovalProcessInstanceId(),file.getProcessInstanceId())
                || chain.stream().anyMatch(row->!Objects.equals(row.getId(),ref.getId()) && row.getOpenCandidateUniqueFlag()!=null)
                || "ACTIVE".equals(file.getStatus()) && chain.stream().anyMatch(row->!Objects.equals(row.getId(),ref.getId()) && row.getActiveUniqueFlag()!=null))
            throw failure("DCC_REPAIR_PLATFORM_CHAIN_CONFLICT");
        var facts=new TreeMap<String,Object>();facts.put("file",file);facts.put("master",master);facts.put("signatures",actualSignatures);
        facts.put("events",events);facts.put("publishedSHA256",published);facts.put("stampedSHA256",stamped);
        facts.put("historicBpm",List.of(history.getId(),history.getBusinessKey(),history.getProcessDefinitionId(),history.getEndTime().getTime()));
        return new Proof(file,master,ref,hash(facts),hash(chain.stream().sorted(Comparator.comparing(ControlledContentVersionRefDO::getId)).toList()),
                actualSignatures.stream().map(row->str(row.getId())).toList());
    }

    private void requireActor(Long actor,Long file) {
        var login=SecurityFrameworkUtils.getLoginUser();Long tenant=TenantContextHolder.getRequiredTenantId();
        var user=actor==null?null:users.getUser(actor);var role=roles.getRoleByCode("doc_control");
        if(login==null || !Objects.equals(login.getId(),actor) || !Objects.equals(login.getTenantId(),tenant)
                || user==null || !Objects.equals(user.getTenantId(),tenant) || !Objects.equals(user.getStatus(),0)
                || role==null || !Objects.equals(role.getStatus(),0) || !"doc_control".equals(role.getCode())
                || !permissions.getUserRoleIdListByUserId(actor).contains(role.getId())
                || !permissions.hasAnyPermissions(actor,"dcc:controlled-file:category:manage")
                || !permissions.hasAnyPermissions(actor,"dcc:controlled-file:query") || !permissions.hasAnyPermissions(actor,"dcc:controlled-file:update")
                || !scope.isWithinAssignedFileScope(actor,file))throw failure("DCC_REPAIR_ACTOR_FORBIDDEN");
    }

    private String artifactHash(Long id) {
        try {
            var file=id==null?null:storage.getFile(id);
            if(file==null || !Objects.equals(file.getId(),id) || file.getConfigId()==null || file.getPath()==null)
                throw failure("DCC_REPAIR_ARTIFACT_REQUIRED");
            byte[] bytes=storage.getFileContent(file.getConfigId(),file.getPath());
            if(bytes==null || bytes.length==0 || file.getSize()==null || file.getSize()!=bytes.length)
                throw failure("DCC_REPAIR_ARTIFACT_INVALID");
            return DigestUtil.sha256Hex(bytes);
        } catch(RuntimeException failure) {throw failure;}
        catch(Exception failure) {throw new IllegalStateException("DCC_REPAIR_ARTIFACT_READ_FAILED",failure);}
    }

    private void requireRequest(DccLifecycleProjectionRepairReqVO r) {
        if(r==null || r.getMasterId()==null || r.getVersionRefId()==null || !"FINALIZING".equals(r.getExpectedCanonicalStatus())
                || r.getProcessInstanceId()==null || !validHash(r.getPreimageHash()) || !validHash(r.getSourceFactsHash())
                || r.getReason()==null || r.getReason().isBlank() || r.getReason().length()>500 || !r.getReason().equals(r.getReason().trim())
                || r.getIdempotencyKey()==null || r.getIdempotencyKey().isBlank() || r.getIdempotencyKey().length()>128
                || !r.getIdempotencyKey().equals(r.getIdempotencyKey().trim()))throw failure("DCC_REPAIR_REQUEST_INVALID");
    }

    private DccLifecycleProjectionRepairRespVO receipt(DccControlledFileDO f,ControlledContentVersionRefDO ref,
            DccLifecycleProjectionRepairReqVO r,String status,Long event,LocalDateTime time) {
        return new DccLifecycleProjectionRepairRespVO(str(f.getId()),str(f.getMasterId()),str(ref.getId()),f.getProcessInstanceId(),status,
                ref.getCanonicalStatus(),ref.getDomainStatus(),r.getSourceFactsHash(),r.getPreimageHash(),r.getIdempotencyKey(),str(event),str(time));
    }
    private static ControlledContentKey key(DccControlledFileDO f) {return ControlledContentKey.of(f.getTenantId(),ControlledContentType.DCC_CONTROLLED_FILE,str(f.getMasterId()));}
    private static GxpAuditStateEnvelope envelope(DccControlledFileDO f,Object state) {return GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion(f.getVersionNo()).canonicalJson(JsonUtils.toJsonString(state)).build();}
    private static boolean sameTime(Object value,LocalDateTime time) {
        if(value instanceof LocalDateTime dateTime)return dateTime.equals(time);
        return value instanceof java.sql.Timestamp timestamp && timestamp.toLocalDateTime().equals(time);
    }
    private static boolean exactEvent(Map<String,Object> event,DccControlledFileDO file,String type,LocalDateTime time) {
        String expected="DCC:"+file.getTenantId()+":"+file.getId()+":"+file.getProcessInstanceId()+":"+type;
        return Objects.equals(str(event.get("tenant_id")),str(file.getTenantId()))
                && Objects.equals(str(event.get("controlled_file_id")),str(file.getId()))
                && Objects.equals(str(event.get("master_id")),str(file.getMasterId()))
                && Objects.equals(event.get("version_no"),file.getVersionNo())
                && Objects.equals(event.get("approval_process_instance_id"),file.getProcessInstanceId())
                && Objects.equals(event.get("event_type"),type) && Objects.equals(event.get("event_key"),expected)
                && sameTime(event.get("occurred_at"),time);
    }
    private static boolean validHash(String s){return s!=null && s.matches("[a-f0-9]{64}");}
    private static String hash(Object value){return DigestUtil.sha256Hex(JsonUtils.toJsonString(value));}
    private static String str(Object value){return value==null?null:value.toString();}
    private static IllegalStateException failure(String code){return new IllegalStateException(code);}
    private record Proof(DccControlledFileDO file,DccControlledFileMasterDO master,ControlledContentVersionRefDO ref,
                         String sourceHash,String preimageHash,List<String> signatureIds) { }
}
