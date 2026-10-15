package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccRelationStageSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccRelationStageMemberDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccRelationStageSnapshotMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationRoundService;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.FileVersion;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Internal signed-node relation service. W05 supplies real validated task identity; no HTTP command. */
@Service
public class DccRelationStageSnapshotService {
    private final DccRelationStore store;
    private final DccRelationStageSnapshotMapper mapper;
    private final DccControlledFileQueryService query;
    private final DccLatestControlledFileResolver files;
    private final DccRelationAccessPolicy access;
    @jakarta.annotation.Resource private DccApplicationRoundService rounds;
    public DccRelationStageSnapshotService(DccRelationStore store,DccRelationStageSnapshotMapper mapper,
            DccControlledFileQueryService query,DccLatestControlledFileResolver files,DccRelationAccessPolicy access) {
        this.store=store;this.mapper=mapper;this.query=query;this.files=files;this.access=access;
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Prepared prepare(Long actorId,Long sourceFileId,ValidatedTaskIdentity task,List<Long> supplements,
            List<Arrangement> arrangements,String commandKey,String expectedDigest,String reason) {
        requireTransaction(); validateContext(actorId,sourceFileId,task,commandKey,reason);
        if(supplements==null || arrangements==null)throw fail("DCC_RELATION_STAGE_EXPLICIT_SELECTION_REQUIRED");
        var ids=new TreeSet<Long>();
        for(var id:supplements) if(id==null || id<=0 || !ids.add(id))throw fail("DCC_RELATION_STAGE_DUPLICATE_SELECTION");
        var source=lockSource(actorId,sourceFileId,task);
        var previous=mapper.findNode(task.tenantId(),task.processInstanceId(),task.taskId(),task.obligationId());
        if(previous.size()>1)throw fail("DCC_RELATION_STAGE_NODE_IDENTITY_INVALID");
        if(!previous.isEmpty()) {
            var old=previous.get(0);var members=mapper.members(task.tenantId(),old.id());
            var complete=selections(members);
            var oldIds=complete.stream().filter(Selection::supplemental).map(Selection::selectedControlledFileId).sorted().toList();
            if(!oldIds.equals(List.copyOf(ids)) || expectedDigest!=null && !Objects.equals(expectedDigest,old.expectedDigest()))
                throw fail("DCC_RELATION_STAGE_REPLAY_CONFLICT");
            var normalized=normalizeArrangements(arrangements,complete,actorId,false);
            var result=prepared(actorId,sourceFileId,task,commandKey,old.expectedDigest(),reason,complete,normalized);
            if(!Objects.equals(old.payloadHash(),result.payloadHash()) || !Objects.equals(old.completeDigest(),result.completeDigest()))
                throw fail("DCC_RELATION_STAGE_REPLAY_CONFLICT");
            return result;
        }
        if(!store.jdbc().queryForList("SELECT id FROM dcc_relation_controlled_event WHERE tenant_id=? AND controlled_file_id=? AND application_round=? FOR UPDATE",
                Long.class,task.tenantId(),sourceFileId,task.processInstanceId()).isEmpty())
            throw fail("DCC_RELATION_STAGE_ALREADY_CONTROLLED");
        boolean assignment="ASSIGN".equals(task.actionType()) && "MATRIX_REVIEW".equals(task.stageCode());
        if("OBSOLETE".equals(task.applicationType()) && (!ids.isEmpty() || !arrangements.isEmpty()))
            throw fail("DCC_RELATION_STAGE_OBSOLETE_READONLY");
        if(!assignment && (!ids.isEmpty() || !arrangements.isEmpty()))throw fail("DCC_RELATION_STAGE_SUPPLEMENT_REQUIRES_ASSIGNMENT");
        var baseline=baseline(sourceFileId,task);
        for(var row:baseline.selections()) {
            var selected=files.resolveSelected(row.selectedControlledFileId());
            if(selected==null || !Objects.equals(selected.tenantId(),task.tenantId())
                    || !Objects.equals(selected.controlledFileId(),row.selectedControlledFileId())
                    || !Objects.equals(selected.masterId(),row.relatedMasterId()) || Objects.equals(selected.masterId(),source.masterId())
                    || !Objects.equals(selected.projectId(),source.projectId()) || !Objects.equals(row.projectId(),source.projectId()))
                throw fail("DCC_RELATION_STAGE_FROZEN_IDENTITY_INVALID");
            access.assertNameVisible(actorId,row.selectedControlledFileId());
        }
        if(expectedDigest!=null && !Objects.equals(expectedDigest,baseline.digest()))throw fail("DCC_RELATION_STAGE_STALE_DIGEST");
        var complete=new TreeMap<Long,Selection>();baseline.selections().forEach(row->complete.put(row.relatedMasterId(),row));
        // Lock candidate masters in stable order before the authority's exact current projections are read.
        var candidateIdentities=ids.stream().map(files::resolveSelected).toList();
        candidateIdentities.stream().map(FileVersion::masterId).filter(Objects::nonNull).distinct().sorted()
                .forEach(master->store.lockMaster(task.tenantId(),master));
        for(var id:ids) {
            var file=files.resolveSelectedForUpdate(id);
            var authority=query.getRelationPermissions(actorId,id);
            if(file==null || authority==null || !Objects.equals(file.tenantId(),task.tenantId())
                    || !Objects.equals(file.controlledFileId(),id) || !Objects.equals(file.projectId(),source.projectId())
                    || Objects.equals(file.masterId(),source.masterId()) || file.masterId()==null
                    || !file.controlled() || !file.executable() || file.pendingEffect() || !"ACTIVE".equals(file.status())
                    || !Objects.equals(authority.tenantId(),task.tenantId()) || !Objects.equals(authority.controlledFileId(),id)
                    || !Objects.equals(authority.masterId(),file.masterId()) || !Objects.equals(authority.projectId(),source.projectId())
                    || !authority.hasCurrentControlledSource() || !Objects.equals(authority.currentActiveControlledFileId(),id)
                    || !authority.controlled() || !authority.executable() || authority.pendingEffect() || !"ACTIVE".equals(authority.status())
                    || !authority.canPreview())throw fail("DCC_RELATION_STAGE_CANDIDATE_INVALID");
            access.assertNameVisible(actorId,id);access.assertContentReadable(actorId,id);
            var selection=new Selection(file.masterId(),id,file.projectId(),text(file.fileNumber(),128),
                    text(file.fileName(),256),text(file.versionNo(),64),"SUPPLEMENT",true);
            if(complete.putIfAbsent(file.masterId(),selection)!=null)throw fail("DCC_RELATION_STAGE_DUPLICATE_MASTER");
        }
        var all=List.copyOf(complete.values());
        return prepared(actorId,sourceFileId,task,commandKey,baseline.digest(),reason,all,
                normalizeArrangements(arrangements,all,actorId,true));
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    @cn.iocoder.yudao.module.system.service.gxpaudit.GxpWriteOperation(operationId="dcc.relation.stage.freeze")
    public Saved freeze(Prepared prepared,SignatureIdentity signature) {
        requireTransaction();
        if(prepared==null || signature==null || signature.signatureId()==null || signature.signatureId()<=0
                || signature.signedAt()==null || signature.signedAt().getNano()!=0 || signature.evidenceHash()==null
                || signature.evidenceHash().isBlank())throw fail("DCC_RELATION_STAGE_SIGNATURE_REQUIRED");
        var t=prepared.task();validateContext(prepared.actorId(),prepared.sourceFileId(),t,prepared.commandKey(),prepared.reason());
        var source=lockSource(prepared.actorId(),prepared.sourceFileId(),t);
        var integrity=prepared(prepared.actorId(),prepared.sourceFileId(),t,prepared.commandKey(),prepared.expectedDigest(),
                prepared.reason(),prepared.complete(),prepared.arrangements());
        if(!Objects.equals(integrity,prepared))throw fail("DCC_RELATION_STAGE_PREPARED_TAMPERED");
        var signatures=store.jdbc().queryForList("SELECT comment FROM dcc_controlled_file_signature WHERE id=? AND controlled_file_id=? AND process_instance_id=? AND task_id=? AND actor_id=? AND signed_at=? AND action_type=? AND evidence_status='VALID' AND evidence_hash=? AND deleted=0 FOR UPDATE",
                String.class,signature.signatureId(),prepared.sourceFileId(),t.processInstanceId(),t.taskId(),prepared.actorId(),
                signature.signedAt(),t.actionType(),signature.evidenceHash());
        String marker=prepared.signedReasonMarker();
        if(signatures.size()!=1 || signatures.get(0)==null || !java.util.regex.Pattern.compile(
                "(?:^|[；;\\s])"+java.util.regex.Pattern.quote(marker)+"(?=$|[；;\\s])").matcher(signatures.get(0)).find())
            throw fail("DCC_RELATION_STAGE_SIGNATURE_IDENTITY_INVALID");
        var existing=mapper.findNode(t.tenantId(),t.processInstanceId(),t.taskId(),t.obligationId());
        if(!existing.isEmpty()) {
            var old=existing.get(0);
            if(existing.size()!=1 || !Objects.equals(old.sourceFileId(),prepared.sourceFileId())
                    || !Objects.equals(old.actorId(),prepared.actorId()) || !Objects.equals(old.payloadHash(),prepared.payloadHash())
                    || !Objects.equals(old.commandKey(),prepared.commandKey()) || !Objects.equals(old.signatureId(),signature.signatureId())
                    || !Objects.equals(old.signedAt(),signature.signedAt()))throw fail("DCC_RELATION_STAGE_REPLAY_CONFLICT");
            var members=mapper.members(t.tenantId(),old.id());
            if(!Objects.equals(selections(members),prepared.complete()) || !Objects.equals(savedArrangements(members),prepared.arrangements()))
                throw fail("DCC_RELATION_STAGE_STORED_MEMBERS_INVALID");
            return new Saved(old,members);
        }
        var ids=prepared.complete().stream().filter(Selection::supplemental).map(Selection::selectedControlledFileId).toList();
        var current=prepare(prepared.actorId(),prepared.sourceFileId(),t,ids,prepared.arrangements(),prepared.commandKey(),
                prepared.expectedDigest(),prepared.reason());
        if(!Objects.equals(current,prepared))throw fail("DCC_RELATION_STAGE_PREPARED_CHANGED");
        var h=new DccRelationStageSnapshotDO(null,t.tenantId(),prepared.sourceFileId(),source.masterId(),source.projectId(),t.processInstanceId(),
                t.taskId(),t.obligationId(),t.stageCode(),t.nodeInstanceId(),t.departmentId(),prepared.actorId(),
                signature.signatureId(),signature.signedAt(),prepared.commandKey(),prepared.payloadHash(),
                prepared.expectedDigest(),prepared.completeDigest(),t.actionType(),prepared.reason(),t.applicationType(),t.attributeRound(),t.formActionInstanceId());
        Long id=mapper.appendHeader(h);
        for(var row:prepared.complete()) {
            var arrangement=prepared.arrangements().stream().filter(a->a.relatedMasterId().equals(row.relatedMasterId())).findFirst().orElse(null);
            mapper.appendMember(new DccRelationStageMemberDO(t.tenantId(),id,row.relatedMasterId(),row.selectedControlledFileId(),row.projectId(),
                    row.fileNumber(),row.fileName(),row.versionNo(),row.origin(),row.supplemental(),
                    arrangement==null?null:arrangement.assigneeUserId(),arrangement==null?null:arrangement.dueAt()));
        }
        var result=new Saved(mapper.findNode(t.tenantId(),t.processInstanceId(),t.taskId(),t.obligationId()).get(0),mapper.members(t.tenantId(),id));
        store.auditSigned("dcc.relation.stage.freeze","RELATION_STAGE:"+id,prepared.actorId(),prepared.reason(),null,result,signature,
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Arrays.asList(prepared.task().tenantId(),prepared.sourceFileId(),prepared.task(),prepared.commandKey(),prepared.payloadHash(),signature.signatureId())));
        return result;
    }
    private FileVersion lockSource(Long actor,Long id,ValidatedTaskIdentity t) {
        var identity=files.resolveSelected(id);
        if(identity==null || !Objects.equals(identity.tenantId(),t.tenantId()) || identity.masterId()==null)throw fail("DCC_RELATION_STAGE_SOURCE_INVALID");
        store.lockMaster(t.tenantId(),identity.masterId());
        var source=files.resolveSelectedForUpdate(id);
        if(source==null || !Objects.equals(source.controlledFileId(),id) || !Objects.equals(source.tenantId(),t.tenantId())
                || !Objects.equals(source.masterId(),identity.masterId()) || source.projectId()==null)throw fail("DCC_RELATION_STAGE_SOURCE_INVALID");
        var context=store.jdbc().queryForList("SELECT id FROM dcc_controlled_file WHERE tenant_id=? AND id=? AND master_id=? AND dcc_project_code_id=? AND deleted=0 FOR UPDATE",
                Long.class,t.tenantId(),id,source.masterId(),source.projectId());
        if(context.size()!=1)throw fail("DCC_RELATION_STAGE_ROUND_IDENTITY_INVALID");
        if(!Objects.equals(rounds.require(t.applicationType(),id,t.processInstanceId()),t.attributeRound()))
            throw fail("DCC_RELATION_STAGE_ROUND_IDENTITY_INVALID");
        if("OBSOLETE".equals(t.applicationType())) {
            var form=store.jdbc().queryForList("SELECT id FROM bpm_form_action_instance WHERE id=? AND tenant_id=? AND system_code='DCC' "
                    + "AND object_type='CONTROLLED_FILE' AND action_code='OBSOLETE' AND object_id=? AND object_version=? "
                    + "AND bpm_process_instance_id=? AND status='IN_APPROVAL' AND deleted=0 FOR UPDATE",
                    Long.class,t.formActionInstanceId(),t.tenantId(),id.toString(),source.versionNo(),t.processInstanceId());
            if(form.size()!=1)throw fail("DCC_RELATION_STAGE_FORM_IDENTITY_INVALID");
        } else {
            var nativeRound=store.jdbc().queryForList("SELECT process_instance_id FROM dcc_controlled_file WHERE tenant_id=? AND id=? FOR UPDATE",String.class,t.tenantId(),id);
            if(t.formActionInstanceId()!=null || nativeRound.size()!=1 || !Objects.equals(nativeRound.get(0),t.processInstanceId()))
                throw fail("DCC_RELATION_STAGE_ROUND_IDENTITY_INVALID");
        }
        access.assertNameVisible(actor,id);return source;
    }
    private Baseline baseline(Long source,ValidatedTaskIdentity t) {
        var rows=uploadSelections(t.tenantId(),source,true);
        var all=new TreeMap<Long,Selection>();
        for(var row:rows) if(row.relatedMasterId()<=0 || row.selectedControlledFileId()<=0 || all.putIfAbsent(row.relatedMasterId(),row)!=null)
            throw fail("DCC_RELATION_STAGE_UPLOAD_IDENTITY_INVALID");
        var sourceFile=files.resolveSelected(source);
        if("OBSOLETE".equals(t.applicationType())) {
            var nativeEvidence=nativeOriginalEvidence(t.tenantId(),source,new HashSet<>(),true);
            requireEffectiveEvidence(nativeEvidence);
            nativeEvidence.completeSelections().forEach(row->putFixed(all,new Selection(row.relatedMasterId(),row.selectedControlledFileId(),row.projectId(),
                    row.fileNumber(),row.fileName(),row.versionNo(),row.origin(),false)));
        }
        var inherited=rounds.readRelationSourceForUpdate(sourceFile.projectId(),t.applicationType(),source,t.attributeRound());
        if(inherited!=null) {
            var evidence=buildRoundEvidence(t.tenantId(),inherited.sourceApplicationId(),inherited.sourceApplicationType(),
                    inherited.sourceBpmRound(),new HashSet<>(),true);
            requireEffectiveEvidence(evidence);
            if(!Objects.equals(evidence.relationDigest(),inherited.sourceRelationDigest()))throw fail("DCC_RELATION_STAGE_SOURCE_DIGEST_CHANGED");
            for(var selected:evidence.completeSelections())putFixed(all,new Selection(selected.relatedMasterId(),selected.selectedControlledFileId(),
                    selected.projectId(),selected.fileNumber(),selected.fileName(),selected.versionNo(),selected.origin(),false));
        }
        var heads=mapper.listRound(t.tenantId(),source,t.processInstanceId());
        if(mapper.countRound(t.tenantId(),source,t.processInstanceId())!=heads.size())throw fail("DCC_RELATION_STAGE_PRIOR_SIGNATURE_INVALID");
        for(var head:heads) {
            var members=mapper.members(t.tenantId(),head.id());
            if(!Objects.equals(hash(selections(members)),head.completeDigest()))throw fail("DCC_RELATION_STAGE_STORED_MEMBERS_INVALID");
            for(var member:members) if(member.supplemental()) {
            var selected=new Selection(member.relatedMasterId(),member.selectedControlledFileId(),member.projectId(),member.fileNumberSnapshot(),
                    member.fileNameSnapshot(),member.versionNoSnapshot(),"SUPPLEMENT",false);
            var old=all.putIfAbsent(selected.relatedMasterId(),selected);
            if(old!=null && !old.equals(selected))throw fail("DCC_RELATION_STAGE_PRIOR_SELECTION_CONFLICT");
            }
        }
        return new Baseline(List.copyOf(all.values()),roundDigest(t.applicationType(),source,t.processInstanceId(),t.attributeRound(),inherited,all.values(),heads));
    }
    private Prepared prepared(Long actor,Long source,ValidatedTaskIdentity t,String key,String digest,String reason,List<Selection> complete,List<Arrangement> arrangements) {
        if(digest==null || !digest.matches("[a-f0-9]{64}"))throw fail("DCC_RELATION_STAGE_DIGEST_INVALID");
        var sorted=complete.stream().sorted(Comparator.comparing(Selection::relatedMasterId)).toList();
        var payload=new TreeMap<String,Object>();payload.put("actor",actor);payload.put("source",source);payload.put("task",t);
        payload.put("commandKey",key);payload.put("expectedDigest",digest);payload.put("complete",sorted);
        payload.put("arrangements",arrangements);payload.put("reason",reason.trim());
        return new Prepared(actor,source,t,key,digest,hash(payload),hash(sorted),reason.trim(),sorted,arrangements);
    }
    private List<Arrangement> normalizeArrangements(List<Arrangement> rows,List<Selection> complete,Long actor,boolean validateAssignee) {
        var masters=new HashSet<Long>();var available=complete.stream().map(Selection::relatedMasterId).toList();
        for(var row:rows) {
            if(row==null || row.relatedMasterId()==null || row.assigneeUserId()==null || row.assigneeUserId()<=0
                    || row.dueAt()==null || row.dueAt().getNano()!=0 || !masters.add(row.relatedMasterId()) || !available.contains(row.relatedMasterId()))
                throw fail("DCC_RELATION_STAGE_ARRANGEMENT_INVALID");
            if(validateAssignee)access.assertAssigneeAvailable(row.assigneeUserId(),row.relatedMasterId());
        }
        return rows.stream().sorted(Comparator.comparing(Arrangement::relatedMasterId)).toList();
    }
    private List<Selection> selections(List<DccRelationStageMemberDO> members) {
        return members.stream().map(m->new Selection(m.relatedMasterId(),m.selectedControlledFileId(),m.projectId(),m.fileNumberSnapshot(),
                m.fileNameSnapshot(),m.versionNoSnapshot(),m.origin(),m.supplemental())).toList();
    }
    private List<Arrangement> savedArrangements(List<DccRelationStageMemberDO> members) {
        return members.stream().filter(m->m.assigneeUserId()!=null).map(m->new Arrangement(m.relatedMasterId(),m.assigneeUserId(),m.dueAt())).toList();
    }
    private void validateContext(Long actor,Long source,ValidatedTaskIdentity t,String key,String reason) {
        if(actor==null || actor<=0 || source==null || source<=0 || t==null || !Objects.equals(t.tenantId(),TenantContextHolder.getRequiredTenantId()))
            throw fail("DCC_RELATION_STAGE_CONTEXT_INVALID");
        text(t.processInstanceId(),128);text(t.taskId(),128);text(t.obligationId(),128);text(t.stageCode(),64);text(t.nodeInstanceId(),128);
        text(t.actionType(),32);text(key,128);text(reason,2048);
        if(!Set.of("UPLOAD","REVISION","OBSOLETE").contains(t.applicationType()==null?"":t.applicationType())
                || t.attributeRound()==null || t.attributeRound()<=0 || ("OBSOLETE".equals(t.applicationType())
                    ? t.formActionInstanceId()==null || t.formActionInstanceId()<=0 : t.formActionInstanceId()!=null))
            throw fail("DCC_RELATION_STAGE_APPLICATION_IDENTITY_INVALID");
        if(!Set.of("ASSIGN","APPROVE","REJECT").contains(t.actionType()) || "ASSIGN".equals(t.actionType())
                && (!"MATRIX_REVIEW".equals(t.stageCode()) || t.departmentId()==null || t.departmentId()<=0))
            throw fail("DCC_RELATION_STAGE_CONTEXT_INVALID");
    }
    private static String text(String s,int max) {if(s==null || s.isBlank() || s.length()>max)throw fail("DCC_RELATION_STAGE_TEXT_INVALID");return s;}
    private static String hash(Object value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JsonUtils.toJsonString(value).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private static void requireTransaction(){if(!TransactionSynchronizationManager.isActualTransactionActive())throw fail("DCC_RELATION_STAGE_TRANSACTION_REQUIRED");}
    private static DccRelationFailure fail(String code){return new DccRelationFailure(code);}
    private record Baseline(List<Selection> selections,String digest) {}

    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public DccRelationRoundEvidence readRoundEvidence(Long actorId,Long fileId,String applicationType,String bpmRound) {
        if(actorId==null || actorId<=0)throw fail("DCC_RELATION_STAGE_CONTEXT_INVALID");
        access.assertNameVisible(actorId,fileId);
        var result=buildRoundEvidence(TenantContextHolder.getRequiredTenantId(),fileId,applicationType,bpmRound,new HashSet<>(),false);
        result.completeSelections().forEach(row->access.assertNameVisible(actorId,row.selectedControlledFileId()));
        return result;
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    @cn.iocoder.yudao.module.system.service.gxpaudit.GxpWriteOperation(operationId="dcc.relation.round.inherit")
    public DccRelationRoundSource inheritRoundSelections(Long actorId,Long sourceFileId,String sourceType,String sourceBpmRound,
            Long targetFileId,String targetType,Integer targetAttributeRound,String expectedSourceDigest) {
        requireTransaction();
        if(actorId==null || actorId<=0 || targetAttributeRound==null || targetAttributeRound<=0
                || sourceFileId==null || targetFileId==null)
            throw fail("DCC_RELATION_STAGE_SOURCE_INPUT_INVALID");
        var source=files.resolveSelected(sourceFileId);var target=files.resolveSelected(targetFileId);
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(source==null || target==null || !Objects.equals(source.tenantId(),tenant) || !Objects.equals(target.tenantId(),tenant)
                || !Objects.equals(source.masterId(),target.masterId()) || !Objects.equals(source.projectId(),target.projectId()))
            throw fail("DCC_RELATION_STAGE_SOURCE_IDENTITY_INVALID");
        if(store.jdbc().queryForList("SELECT id FROM dcc_project_code WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE",
                Long.class,tenant,target.projectId()).size()!=1)throw fail("DCC_RELATION_STAGE_SOURCE_PROJECT_INVALID");
        store.lockMaster(tenant,target.masterId());
        source=files.resolveSelectedForUpdate(sourceFileId);target=files.resolveSelectedForUpdate(targetFileId);
        if(source==null || target==null || !Objects.equals(source.tenantId(),tenant) || !Objects.equals(target.tenantId(),tenant)
                || !Objects.equals(source.masterId(),target.masterId()) || !Objects.equals(source.projectId(),target.projectId()))
            throw fail("DCC_RELATION_STAGE_SOURCE_IDENTITY_INVALID");
        access.assertNameVisible(actorId,sourceFileId);access.assertNameVisible(actorId,targetFileId);
        var before=rounds.readRelationSourceForUpdate(target.projectId(),targetType,targetFileId,targetAttributeRound);
        var evidence=buildRoundEvidence(tenant,sourceFileId,sourceType,sourceBpmRound,new HashSet<>(),true);
        if(sourceFileId.equals(targetFileId) && sourceType.equals(targetType) && evidence.attributeRound().equals(targetAttributeRound))
            throw fail("DCC_RELATION_STAGE_SOURCE_CYCLE");
        if(!Objects.equals(evidence.relationDigest(),expectedSourceDigest))throw fail("DCC_RELATION_STAGE_SOURCE_DIGEST_CHANGED");
        // Exact replay of an already-bound source keeps its historical selection; a new bind requires valid signed facts.
        if(before==null)requireEffectiveEvidence(evidence);
        var requested=new DccApplicationRoundService.RelationSource(sourceType,sourceFileId,sourceBpmRound,expectedSourceDigest);
        if(before!=null && !before.equals(requested))throw fail("DCC_RELATION_STAGE_SOURCE_BINDING_CONFLICT");
        evidence.completeSelections().forEach(row->access.assertNameVisible(actorId,row.selectedControlledFileId()));
        var targetBefore=before==null?store.jdbc().queryForMap("SELECT tenant_id,project_id,application_type,application_id,attribute_round,bpm_round,source_application_type,source_application_id,source_bpm_round,source_relation_digest "
                + "FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND attribute_round=? FOR UPDATE",
                tenant,target.projectId(),targetType,targetFileId,targetAttributeRound):null;
        var bound=rounds.bindRelationSource(target.projectId(),targetType,targetFileId,targetAttributeRound,sourceType,sourceFileId,sourceBpmRound,expectedSourceDigest);
        var result=new DccRelationRoundSource(tenant,target.projectId(),targetType,targetFileId,targetAttributeRound,
                bound.sourceApplicationType(),bound.sourceApplicationId(),bound.sourceBpmRound(),bound.sourceRelationDigest());
        if(before==null)store.auditHumanSource("dcc.relation.round.inherit","RELATION_ROUND_SOURCE:"+targetFileId+":"+targetAttributeRound,actorId,
                "继承正式来源申请关系 "+sourceFileId+" / "+sourceBpmRound,targetBefore,result,
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Arrays.asList(actorId,result)));
        return result;
    }
    private DccRelationRoundEvidence buildRoundEvidence(Long tenant,Long fileId,String type,String bpm,Set<String> visiting,boolean locked) {
        var file=files.resolveSelected(fileId);
        if(file==null || !Objects.equals(file.tenantId(),tenant))throw fail("DCC_RELATION_STAGE_SOURCE_IDENTITY_INVALID");
        Integer attributeRound=rounds.require(type,fileId,bpm);
        rounds.assertBoundRound(file.projectId(),type,fileId,attributeRound);
        String key=type+":"+fileId+":"+bpm;
        if(!visiting.add(key))throw fail("DCC_RELATION_STAGE_SOURCE_CYCLE");
        try {
            var original=uploadSelections(tenant,fileId,locked);
            var all=new TreeMap<Long,Selection>();original.forEach(row->putFixed(all,row));
            if("OBSOLETE".equals(type))nativeOriginalEvidence(tenant,fileId,visiting,locked).completeSelections().forEach(row->putFixed(all,
                    new Selection(row.relatedMasterId(),row.selectedControlledFileId(),row.projectId(),row.fileNumber(),row.fileName(),row.versionNo(),row.origin(),false)));
            var source=locked?rounds.readRelationSourceForUpdate(file.projectId(),type,fileId,attributeRound):rounds.readRelationSource(file.projectId(),type,fileId,attributeRound);
            if(source!=null) {
                var inherited=buildRoundEvidence(tenant,source.sourceApplicationId(),source.sourceApplicationType(),source.sourceBpmRound(),visiting,locked);
                if(!Objects.equals(inherited.masterId(),file.masterId()) || !Objects.equals(inherited.projectId(),file.projectId()))
                    throw fail("DCC_RELATION_STAGE_SOURCE_IDENTITY_INVALID");
                if(!Objects.equals(source.sourceRelationDigest(),inherited.relationDigest()))throw fail("DCC_RELATION_STAGE_SOURCE_DIGEST_CHANGED");
                inherited.completeSelections().forEach(row->putFixed(all,new Selection(row.relatedMasterId(),row.selectedControlledFileId(),row.projectId(),
                        row.fileNumber(),row.fileName(),row.versionNo(),row.origin(),false)));
            }
            var heads=locked?mapper.listAllRoundForUpdate(tenant,fileId,bpm):mapper.listAllRoundForRead(tenant,fileId,bpm);
            Long formId=null;
            if("OBSOLETE".equals(type)) {
                var form=store.jdbc().queryForList("SELECT id FROM bpm_form_action_instance WHERE tenant_id=? AND system_code='DCC' AND object_type='CONTROLLED_FILE' "
                        + "AND action_code='OBSOLETE' AND object_id=? AND object_version=? AND bpm_process_instance_id=? AND deleted=0",
                        Long.class,tenant,fileId.toString(),file.versionNo(),bpm);
                if(form.size()!=1)throw fail("DCC_RELATION_STAGE_FORM_IDENTITY_INVALID");
                formId=form.get(0);
                var mappedForm=store.jdbc().queryForList("SELECT form_instance_id FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? "
                        + "AND application_type=? AND application_id=? AND attribute_round=? AND bpm_round=?",Long.class,
                        tenant,file.projectId(),type,fileId,attributeRound,bpm);
                // IC29 records Form ownership for new drafts; pre-migration historical rows explicitly retain NULL.
                if(mappedForm.size()!=1 || mappedForm.get(0)!=null && !Objects.equals(mappedForm.get(0),formId))
                    throw fail("DCC_RELATION_STAGE_FORM_IDENTITY_INVALID");
            }
            var stages=new ArrayList<DccRelationRoundEvidence.StageEvidence>();
            for(var head:heads) {
                if(!Objects.equals(head.tenantId(),tenant) || !Objects.equals(head.sourceFileId(),fileId)
                        || !Objects.equals(head.sourceMasterId(),file.masterId()) || !Objects.equals(head.projectId(),file.projectId())
                        || !Objects.equals(head.processInstanceId(),bpm))
                    throw fail("DCC_RELATION_STAGE_SOURCE_IDENTITY_INVALID");
                boolean applicationIdentityRecorded=head.applicationType()!=null || head.attributeRound()!=null || head.formActionInstanceId()!=null;
                if(applicationIdentityRecorded && (!Objects.equals(head.applicationType(),type) || !Objects.equals(head.attributeRound(),attributeRound)))
                    throw fail("DCC_RELATION_STAGE_APPLICATION_IDENTITY_INVALID");
                if(applicationIdentityRecorded && !Objects.equals(head.formActionInstanceId(),formId))
                    throw fail("DCC_RELATION_STAGE_FORM_IDENTITY_INVALID");
                var members=locked?mapper.membersForUpdate(tenant,head.id()):mapper.members(tenant,head.id());
                if(!Objects.equals(hash(selections(members)),head.completeDigest()))throw fail("DCC_RELATION_STAGE_STORED_MEMBERS_INVALID");
                var signature=store.jdbc().queryForList("SELECT controlled_file_id,process_instance_id,task_id,actor_id,signed_at,action_type,evidence_status,deleted FROM dcc_controlled_file_signature WHERE id=?"+(locked?" FOR UPDATE":""),head.signatureId());
                boolean exact=false;String state="MISSING";
                if(signature.size()==1) {
                    var row=new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);row.putAll(signature.get(0));state=(String)row.get("evidence_status");
                    // Case-independent JDBC metadata lookup avoids depending on a driver's label casing.
                    exact=signatureMatches(head,signature.get(0));
                    if(!exact)state="IDENTITY_MISMATCH";
                    else if(state==null || state.isBlank())state="UNRECORDED";
                }
                stages.add(new DccRelationRoundEvidence.StageEvidence(new Saved(head,members),state,exact));
                for(var member:members)if(member.supplemental())putFixed(all,new Selection(member.relatedMasterId(),member.selectedControlledFileId(),member.projectId(),
                        member.fileNumberSnapshot(),member.fileNameSnapshot(),member.versionNoSnapshot(),member.origin(),false));
            }
            return new DccRelationRoundEvidence(tenant,fileId,file.masterId(),file.projectId(),type,attributeRound,bpm,!heads.isEmpty(),
                    heads.isEmpty()?"本申请轮次未记录签名关联节点快照":null,original,stages,List.copyOf(all.values()),
                    roundDigest(type,fileId,bpm,attributeRound,source,all.values(),heads));
        } finally {visiting.remove(key);}
    }
    private boolean signatureMatches(DccRelationStageSnapshotDO h,Map<String,Object> source) {
        var row=new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);row.putAll(source);
        Object time=row.get("signed_at");if(time instanceof java.sql.Timestamp timestamp)time=timestamp.toLocalDateTime();
        boolean deleted=row.get("deleted") instanceof Boolean b?b:row.get("deleted") instanceof Number n && n.intValue()!=0;
        return !deleted && Objects.equals(h.sourceFileId(),number(row.get("controlled_file_id")))
                && Objects.equals(h.processInstanceId(),row.get("process_instance_id")) && Objects.equals(h.taskId(),row.get("task_id"))
                && Objects.equals(h.actorId(),number(row.get("actor_id"))) && Objects.equals(h.signedAt(),time)
                && Objects.equals(h.actionType(),row.get("action_type"));
    }
    private DccRelationRoundEvidence nativeOriginalEvidence(Long tenant,Long file,Set<String> visiting,boolean locked) {
        var exact=store.jdbc().queryForList("SELECT round_link.application_type,round_link.bpm_round FROM dcc_application_round_link round_link "
                + "INNER JOIN dcc_controlled_file file ON file.tenant_id=round_link.tenant_id AND file.id=round_link.application_id "
                + "AND file.dcc_project_code_id=round_link.project_id AND file.process_instance_id=round_link.bpm_round "
                + "WHERE round_link.tenant_id=? AND round_link.application_id=? AND round_link.application_type IN ('UPLOAD','REVISION') "
                + "AND round_link.bpm_round IS NOT NULL AND file.deleted=0"+(locked?" FOR UPDATE":""),tenant,file);
        if(exact.size()!=1)throw fail("DCC_RELATION_STAGE_NATIVE_SOURCE_MAPPING_INVALID");
        var row=new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);row.putAll(exact.get(0));
        return buildRoundEvidence(tenant,file,(String)row.get("application_type"),(String)row.get("bpm_round"),visiting,locked);
    }
    private Long number(Object value){return value instanceof Number n?n.longValue():null;}
    private void requireEffectiveEvidence(DccRelationRoundEvidence evidence) {
        requireEffectiveEvidence(evidence,new HashSet<>());
    }
    private void requireEffectiveEvidence(DccRelationRoundEvidence evidence,Set<String> visited) {
        String key=evidence.applicationType()+":"+evidence.fileId()+":"+evidence.bpmRound();
        if(!visited.add(key))throw fail("DCC_RELATION_STAGE_SOURCE_CYCLE");
        if(evidence.stages().stream().anyMatch(stage->!stage.signatureIdentityMatches() || !"VALID".equals(stage.signatureEvidenceStatus())))
            throw fail("DCC_RELATION_STAGE_PRIOR_SIGNATURE_INVALID");
        var source=rounds.readRelationSource(evidence.projectId(),evidence.applicationType(),evidence.fileId(),evidence.attributeRound());
        if(source!=null) {
            var upstream=buildRoundEvidence(evidence.tenantId(),source.sourceApplicationId(),source.sourceApplicationType(),source.sourceBpmRound(),new HashSet<>(),true);
            if(!Objects.equals(upstream.relationDigest(),source.sourceRelationDigest()))throw fail("DCC_RELATION_STAGE_SOURCE_DIGEST_CHANGED");
            requireEffectiveEvidence(upstream,visited);
        } else if("OBSOLETE".equals(evidence.applicationType())) {
            // The first obsolete round includes native signed supplements; those signatures remain authoritative.
            requireEffectiveEvidence(nativeOriginalEvidence(evidence.tenantId(),evidence.fileId(),new HashSet<>(),true),visited);
        }
        visited.remove(key);
    }
    private List<Selection> uploadSelections(Long tenant,Long file,boolean locked) {
        return store.jdbc().query("SELECT related_master_id,related_controlled_file_id,project_code_id,related_file_number_snapshot,related_file_name_snapshot,related_version_no_snapshot "
                + "FROM dcc_controlled_file_related_file WHERE tenant_id=? AND controlled_file_id=? AND deleted=0 ORDER BY related_master_id"+(locked?" FOR UPDATE":""),
                (r,n)->new Selection(r.getLong(1),r.getLong(2),r.getLong(3),text(r.getString(4),128),text(r.getString(5),256),text(r.getString(6),64),"UPLOAD",false),tenant,file);
    }
    private void putFixed(Map<Long,Selection> rows,Selection selected) {
        var old=rows.putIfAbsent(selected.relatedMasterId(),selected);
        if(old!=null && (!Objects.equals(old.selectedControlledFileId(),selected.selectedControlledFileId())
                || !Objects.equals(old.projectId(),selected.projectId()) || !Objects.equals(old.fileNumber(),selected.fileNumber())
                || !Objects.equals(old.fileName(),selected.fileName()) || !Objects.equals(old.versionNo(),selected.versionNo())))
            throw fail("DCC_RELATION_STAGE_PRIOR_SELECTION_CONFLICT");
    }
    private String roundDigest(String type,Long file,String bpm,Integer attribute,DccApplicationRoundService.RelationSource source,
            Collection<Selection> complete,List<DccRelationStageSnapshotDO> heads) {
        return hash(Arrays.asList(type,file,bpm,attribute,source,complete,heads.stream().map(h->List.of(h.id(),h.payloadHash())).toList()));
    }
}
