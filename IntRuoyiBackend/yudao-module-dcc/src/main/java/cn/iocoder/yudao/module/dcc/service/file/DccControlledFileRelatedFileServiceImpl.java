package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRelatedFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRelatedFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.FileVersion;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.RelationChange;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.CurrentRelations;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import org.springframework.transaction.annotation.Transactional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_DUPLICATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum.ACTIVE;

@Service
@Validated
public class DccControlledFileRelatedFileServiceImpl implements DccControlledFileRelatedFileService {

    private static final String RELATION_SOURCE_UPLOAD = "UPLOAD";
    private static final String RELATION_SOURCE_CHECKIN_INHERITED = "CHECKIN_INHERITED";

    @Resource
    private DccControlledFileRelatedFileMapper relatedFileMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore relationStore;
    @Resource
    private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationAccessPolicy relationAccessPolicy;
    @Resource
    private cn.iocoder.yudao.module.dcc.service.file.relations.DccLatestControlledFileResolver latestFileResolver;

    @Override
    public List<FileVersion> listCurrentRelatedFiles(Long actorId, Long sourceFileId) {
        FileVersion source=requireSelected(sourceFileId);
        relationAccessPolicy.assertNameVisible(actorId,sourceFileId);
        relationStore.assertCurrentRelationsInitialized(TenantContextHolder.getRequiredTenantId(),source.masterId());
        return relationStore.jdbc().queryForList("SELECT related_master_id FROM dcc_current_file_relation WHERE tenant_id=? AND source_master_id=? ORDER BY related_master_id FOR UPDATE",
                Long.class,TenantContextHolder.getRequiredTenantId(),source.masterId()).stream().map(master->{
            FileVersion latest=requireLatest(master); relationAccessPolicy.assertNameVisible(actorId,latest.controlledFileId()); return latest;
        }).toList();
    }
    @Override
    @Transactional(rollbackFor=Exception.class)
    public CurrentRelations getCurrentRelationView(Long actorId,Long sourceFileId){
        var source=requireSelected(sourceFileId);
        relationAccessPolicy.assertNameVisible(actorId,sourceFileId);
        relationStore.lockMaster(TenantContextHolder.getRequiredTenantId(),source.masterId());
        var rows=relationStore.jdbc().query("SELECT controlled_file_id,row_version FROM dcc_current_file_relation_set WHERE tenant_id=? AND source_master_id=? FOR UPDATE",
                (rs,n)->new CurrentRelations(rs.getLong(1),rs.getLong(2),List.of()),TenantContextHolder.getRequiredTenantId(),source.masterId());
        if(rows.size()!=1) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_CURRENT_SET_NOT_INITIALIZED");
        var currentSource = requireSelected(rows.get(0).sourceControlledFileId());
        if (!Objects.equals(currentSource.masterId(), source.masterId()))
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_SOURCE_IDENTITY_MISMATCH");
        relationAccessPolicy.assertNameVisible(actorId,rows.get(0).sourceControlledFileId());
        return new CurrentRelations(rows.get(0).sourceControlledFileId(),rows.get(0).rowVersion(),listCurrentRelatedFiles(actorId,sourceFileId));
    }
    @Override
    public List<DccControlledFileRelatedFileRespVO> listHistoricalRelatedFiles(Long actorId, Long sourceFileId) {
        requireSelected(sourceFileId); relationAccessPolicy.assertNameVisible(actorId,sourceFileId);
        var rows=relatedFileMapper.selectListByControlledFileId(sourceFileId);
        rows.forEach(row->relationAccessPolicy.assertNameVisible(actorId,row.getRelatedControlledFileId()));
        return rows.stream().map(this::toSnapshotRespVO).toList();
    }
    @Override
    @Transactional(rollbackFor=Exception.class)
    public RelationChange replaceCurrentRelations(Long actorId, Long sourceFileId, List<Long> ids, List<Long> expected,Long expectedVersion,String idempotencyKey,String reason) {
        if(actorId==null || actorId<=0 || ids==null || expected==null || reason==null || reason.isBlank()
                || expectedVersion==null || expectedVersion<0 || idempotencyKey==null || idempotencyKey.isBlank()
                || idempotencyKey.length()>128 || !idempotencyKey.equals(idempotencyKey.trim())
                || ids.stream().anyMatch(id->id==null || id<=0) || expected.stream().anyMatch(id->id==null || id<=0)
                || new LinkedHashSet<>(ids).size()!=ids.size() || new LinkedHashSet<>(expected).size()!=expected.size())
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_RELATION_COMMAND_REQUIRED");
        var source=requireSelected(sourceFileId); Long tenant=TenantContextHolder.getRequiredTenantId();
        relationStore.lockMaster(tenant,source.masterId());
        relationAccessPolicy.assertCanEditRelations(actorId,sourceFileId);
        var payload=new java.util.TreeMap<String,Object>();
        payload.put("actorId",actorId);payload.put("sourceFileId",sourceFileId);payload.put("selectedFileIds",ids.stream().sorted().toList());
        payload.put("expectedMasterIds",expected.stream().sorted().toList());payload.put("expectedVersion",expectedVersion);payload.put("reason",reason);
        String hash=org.apache.commons.codec.digest.DigestUtils.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(payload));
        var committed=relationStore.jdbc().query("SELECT source_file_id,payload_hash,resulting_version,related_master_ids FROM dcc_relation_change_command WHERE tenant_id=? AND source_master_id=? AND idempotency_key=? FOR UPDATE",
                (rs,n)->new RelationCommand(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getString(4)),tenant,source.masterId(),idempotencyKey);
        if(!committed.isEmpty()){
            var saved=committed.get(0);
            if(committed.size()!=1 || !Objects.equals(saved.sourceFileId(),sourceFileId) || !Objects.equals(saved.payloadHash(),hash))
                throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_COMMAND_REPLAY_CONFLICT");
            return new RelationChange(sourceFileId,saved.resultingVersion(),List.copyOf(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(saved.masterIds(),Long.class)));
        }
        relationStore.assertCurrentRelationsInitialized(tenant,source.masterId());
        var relationSet=relationStore.jdbc().queryForObject("SELECT controlled_file_id,row_version FROM dcc_current_file_relation_set WHERE tenant_id=? AND source_master_id=? FOR UPDATE",
                (rs,n)->new CurrentRelations(rs.getLong(1),rs.getLong(2),List.of()),tenant,source.masterId());
        if(!Objects.equals(relationSet.sourceControlledFileId(),sourceFileId))
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_SOURCE_VERSION_CHANGED");
        if(relationSet.rowVersion()!=expectedVersion) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_CONCURRENT_CHANGE");
        var current=relationStore.jdbc().queryForList("SELECT related_master_id FROM dcc_current_file_relation WHERE tenant_id=? AND source_master_id=? ORDER BY related_master_id FOR UPDATE",Long.class,tenant,source.masterId());
        if(expected.stream().anyMatch(Objects::isNull) || new LinkedHashSet<>(expected).size()!=expected.size()
                || !new LinkedHashSet<>(current).equals(new LinkedHashSet<>(expected))) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_CONCURRENT_CHANGE");
        var selected=new LinkedHashSet<Long>();
        for(Long id:ids) {
            var candidate=requireSelected(id);var latest=requireLatest(candidate.masterId());
            relationAccessPolicy.assertNameVisible(actorId,id);
            if(!latest.controlled() || !Objects.equals(latest.controlledFileId(),id) || Objects.equals(source.masterId(),candidate.masterId()))
                throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
            if(!selected.add(candidate.masterId())) throw exception(CONTROLLED_FILE_RELATED_FILE_DUPLICATE);
        }
        long nextVersion=expectedVersion;
        if(!new LinkedHashSet<>(current).equals(selected)){
            nextVersion=Math.addExact(expectedVersion,1);
            relationStore.jdbc().update("DELETE FROM dcc_current_file_relation WHERE tenant_id=? AND source_master_id=?",tenant,source.masterId());
            selected.forEach(master->relationStore.jdbc().update("INSERT INTO dcc_current_file_relation VALUES (?,?,?)",tenant,source.masterId(),master));
            if(relationStore.jdbc().update("UPDATE dcc_current_file_relation_set SET row_version=? WHERE tenant_id=? AND source_master_id=? AND row_version=?",
                    nextVersion,tenant,source.masterId(),expectedVersion)!=1) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_CONCURRENT_CHANGE");
        }
        String selectedJson=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(List.copyOf(selected));
        if(selectedJson.length()>8192) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_RELATION_SELECTION_TOO_LARGE");
        if(relationStore.jdbc().update("INSERT INTO dcc_relation_change_command (tenant_id,source_master_id,source_file_id,actor_id,idempotency_key,payload_hash,resulting_version,related_master_ids) VALUES (?,?,?,?,?,?,?,?)",
                tenant,source.masterId(),sourceFileId,actorId,idempotencyKey,hash,nextVersion,selectedJson)!=1)
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_COMMAND_WRITE_FAILED");
        relationStore.audit("dcc.relation.replace","MASTER_RELATIONS:"+source.masterId(),reason,
                new RelationChange(sourceFileId,expectedVersion,current),new RelationChange(sourceFileId,nextVersion,List.copyOf(selected)));
        return new RelationChange(sourceFileId,nextVersion,List.copyOf(selected));
    }
    @Override
    public void assertRelatedContentReadable(Long actorId, Long relatedFileId) {
        // The content policy must repeat exact tenant/identity/state authorization at the binary endpoint.
        relationAccessPolicy.assertContentReadable(actorId,relatedFileId);
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public void validateAndBindRelatedFiles(Long controlledFileId, Long projectCodeId,
                                            List<Long> relatedControlledFileIds) {
        List<Long> normalizedIds = normalizeRelatedFileIds(controlledFileId, projectCodeId, relatedControlledFileIds);
        if (normalizedIds.isEmpty()) {
            return;
        }
        DccControlledFileDO owner = controlledFileMapper.selectById(controlledFileId);
        if (owner == null || owner.getMasterId() == null
                || !Objects.equals(owner.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(owner.getDccProjectCodeId(), projectCodeId)) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        owner=requireWritableSnapshotTarget(owner);
        if(!Objects.equals(owner.getDccProjectCodeId(),projectCodeId))throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        var chosen=new java.util.ArrayList<FileVersion>();var masters=new LinkedHashSet<Long>();
        for(Long id:normalizedIds) {
            var file=requireSelected(id);var latest=requireLatest(file.masterId());
            relationAccessPolicy.assertNameVisible(SecurityFrameworkUtils.getLoginUserId(),id);
            if(!latest.controlled() || !Objects.equals(id,latest.controlledFileId()) || Objects.equals(owner.getMasterId(),file.masterId()))
                throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
            if(!masters.add(file.masterId())) throw exception(CONTROLLED_FILE_RELATED_FILE_DUPLICATE);
            chosen.add(latest);
        }
        for (FileVersion relatedFile : chosen) {
            var relation=DccControlledFileRelatedFileDO.builder()
                    .controlledFileId(controlledFileId)
                    .relatedControlledFileId(relatedFile.controlledFileId())
                    .projectCodeId(projectCodeId)
                    .relatedMasterId(relatedFile.masterId())
                    .relatedFileNumberSnapshot(relatedFile.fileNumber())
                    .relatedFileNameSnapshot(relatedFile.fileName())
                    .relatedVersionNoSnapshot(relatedFile.versionNo())
                    .relationSource(RELATION_SOURCE_UPLOAD)
                    .build();
            relation.setTenantId(TenantContextHolder.getRequiredTenantId());
            relatedFileMapper.insert(relation);
        }
    }

    @Override
    public List<DccControlledFileRelatedFileRespVO> listRelatedFiles(Long controlledFileId) {
        // Existing Query callers remain historical until C explicitly connects the current projection.
        return listHistoricalRelatedFiles(SecurityFrameworkUtils.getLoginUserId(),controlledFileId);
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public void inheritRelatedFiles(Long sourceControlledFileId, Long targetControlledFileId) {
        if (sourceControlledFileId == null || targetControlledFileId == null
                || Objects.equals(sourceControlledFileId, targetControlledFileId)) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        DccControlledFileDO target = controlledFileMapper.selectById(targetControlledFileId);
        Long tenant=TenantContextHolder.getRequiredTenantId();
        DccControlledFileDO source=controlledFileMapper.selectById(sourceControlledFileId);
        if (target == null || source==null || !Objects.equals(tenant,target.getTenantId())
                || !Objects.equals(tenant,source.getTenantId()) || !Objects.equals(source.getMasterId(),target.getMasterId())) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        target=requireWritableSnapshotTarget(target);
        List<DccControlledFileRelatedFileDO> sourceRelations =
                relatedFileMapper.selectListByControlledFileId(sourceControlledFileId);
        for (DccControlledFileRelatedFileDO sourceRelation : sourceRelations) {
            if(!Objects.equals(tenant,sourceRelation.getTenantId())) throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
            var inherited=DccControlledFileRelatedFileDO.builder()
                    .controlledFileId(targetControlledFileId)
                    .relatedControlledFileId(sourceRelation.getRelatedControlledFileId())
                    .projectCodeId(target.getDccProjectCodeId())
                    .relatedMasterId(sourceRelation.getRelatedMasterId())
                    .relatedFileNumberSnapshot(sourceRelation.getRelatedFileNumberSnapshot())
                    .relatedFileNameSnapshot(sourceRelation.getRelatedFileNameSnapshot())
                    .relatedVersionNoSnapshot(sourceRelation.getRelatedVersionNoSnapshot())
                    .relationSource(RELATION_SOURCE_CHECKIN_INHERITED)
                    .build();
            inherited.setTenantId(tenant);
            relatedFileMapper.insert(inherited);
        }
    }

    @Override
    public List<DccControlledFileRelatedFileDO> listForwardRelations(Long controlledFileId) {
        return relatedFileMapper.selectListByControlledFileId(controlledFileId);
    }

    @Override
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY,rollbackFor=Exception.class)
    public void freezeCurrentRelationsForRevision(Long actorId, Long baselineId, Long candidateId) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
                || actorId == null || actorId <= 0 || baselineId == null || candidateId == null
                || baselineId <= 0 || candidateId <= 0 || baselineId.equals(candidateId))
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_REVISION_RELATION_CONTEXT_REQUIRED");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var target = controlledFileMapper.selectByIdAndTenantForUpdate(tenant, candidateId);
        if (target == null) throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        target = requireWritableSnapshotTarget(target);
        // RevisionService already holds project -> same Master -> source/body locks. Reenter the
        // same Master, then lock its authoritative current tuple and members before freezing rows.
        var master = controlledFileMasterMapper.selectByIdForUpdate(target.getMasterId());
        var baseline = controlledFileMapper.selectByIdAndTenantForUpdate(tenant, baselineId);
        if (master == null || baseline == null || !Objects.equals(master.getTenantId(), tenant)
                || !Objects.equals(master.getId(), baseline.getMasterId())
                || !Objects.equals(master.getLatestControlledFileId(), baselineId)
                || !Objects.equals(target.getDccProjectCodeId(), master.getDccProjectCodeId())
                || !Objects.equals(baseline.getDccProjectCodeId(), master.getDccProjectCodeId())
                || !Objects.equals(target.getRevisionSourceControlledFileId(), baselineId)
                || target.getSelectedIterationControlledFileId() == null || !"REVISION".equals(target.getChangeType())
                || !"WORKING".equals(target.getStatus()) || baseline.getControlledTime() == null
                || !DccControlledFileVersionPolicy.isCurrentControlledStatus(baseline.getStatus()))
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REVISION_RELATION_SOURCE_INVALID");
        relationAccessPolicy.assertNameVisible(actorId, baselineId);
        var sets = relationStore.jdbc().query("SELECT controlled_file_id,row_version FROM dcc_current_file_relation_set "
                        + "WHERE tenant_id=? AND source_master_id=? FOR UPDATE",
                (rs,n)->new CurrentRelations(rs.getLong(1),rs.getLong(2),List.of()),tenant,master.getId());
        if (sets.size() != 1 || !Objects.equals(sets.get(0).sourceControlledFileId(), baselineId)
                || sets.get(0).rowVersion() < 0)
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REVISION_RELATION_CURRENT_TUPLE_INVALID");
        if (!relatedFileMapper.selectListByControlledFileId(candidateId).isEmpty())
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REVISION_RELATION_SNAPSHOT_ALREADY_EXISTS");
        var ids = relationStore.jdbc().queryForList("SELECT related_master_id FROM dcc_current_file_relation "
                + "WHERE tenant_id=? AND source_master_id=? ORDER BY related_master_id FOR UPDATE",Long.class,tenant,master.getId());
        if (ids.stream().anyMatch(id->id==null || id<=0 || id.equals(master.getId()))
                || ids.stream().distinct().count()!=ids.size()) throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        var chosen = new java.util.ArrayList<FileVersion>();
        for (Long id : ids) {
            var file = requireLatest(id);
            relationAccessPolicy.assertNameVisible(actorId, file.controlledFileId());
            if (!file.controlled() || file.fileName()==null || file.fileNumber()==null || file.versionNo()==null)
                throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
            chosen.add(file);
        }
        // The source lock stays held until submission commits. Verify the exact consumed tuple,
        // without a no-op UPDATE or changing the current set's version/history.
        Long tuple = relationStore.jdbc().queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation_set "
                + "WHERE tenant_id=? AND source_master_id=? AND controlled_file_id=? AND row_version=?",
                Long.class,tenant,master.getId(),baselineId,sets.get(0).rowVersion());
        if (!Objects.equals(tuple,1L)) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REVISION_RELATION_CURRENT_TUPLE_INVALID");
        for (var file : chosen) {
            var row = DccControlledFileRelatedFileDO.builder().controlledFileId(candidateId)
                    .relatedControlledFileId(file.controlledFileId()).projectCodeId(target.getDccProjectCodeId())
                    .relatedMasterId(file.masterId()).relatedFileNumberSnapshot(file.fileNumber())
                    .relatedFileNameSnapshot(file.fileName()).relatedVersionNoSnapshot(file.versionNo())
                    .relationSource("REVISION_CURRENT").build();
            row.setTenantId(tenant);
            if (relatedFileMapper.insert(row)!=1) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REVISION_RELATION_SNAPSHOT_WRITE_FAILED");
        }
    }

    @Override
    public List<Long> resolveCurrentActiveRelatedFileIds(Long controlledFileId, Long projectCodeId) {
        if (controlledFileId == null || projectCodeId == null) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        List<DccControlledFileRelatedFileDO> relations = relatedFileMapper.selectListByControlledFileId(controlledFileId);
        if (relations.isEmpty()) {
            return List.of();
        }
        // Retained caller signature; resubmission must consume latest controlled, including pending effect.
        return relations.stream().map(row->requireLatest(row.getRelatedMasterId())).map(file->{
            if(!file.controlled()) throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
            relationAccessPolicy.assertNameVisible(SecurityFrameworkUtils.getLoginUserId(),file.controlledFileId());return file.controlledFileId();
        }).distinct().toList();
    }

    @Override
    public List<DccControlledFileRelatedFileDO> listReverseCurrentActiveRelations(Long tenantId,
                                                                                  Long relatedMasterId) {
        if (tenantId == null || relatedMasterId == null) {
            throw new IllegalArgumentException("tenantId and relatedMasterId are required for reverse relation lookup");
        }
        return relatedFileMapper.selectReverseCurrentActiveRelations(tenantId, relatedMasterId);
    }

    private List<Long> normalizeRelatedFileIds(Long controlledFileId, Long projectCodeId,
                                               List<Long> relatedControlledFileIds) {
        if (relatedControlledFileIds == null || relatedControlledFileIds.isEmpty()) {
            return List.of();
        }
        if (projectCodeId == null) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long fileId : relatedControlledFileIds) {
            if (fileId == null || fileId <= 0 || Objects.equals(fileId, controlledFileId)) {
                throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
            }
            if (!uniqueIds.add(fileId)) {
                throw exception(CONTROLLED_FILE_RELATED_FILE_DUPLICATE);
            }
        }
        return List.copyOf(uniqueIds);
    }

    private DccControlledFileRelatedFileRespVO toSnapshotRespVO(DccControlledFileRelatedFileDO relation) {
        DccControlledFileRelatedFileRespVO respVO = new DccControlledFileRelatedFileRespVO();
        respVO.setRelationId(relation.getId());
        respVO.setControlledFileId(relation.getRelatedControlledFileId());
        respVO.setMasterId(relation.getRelatedMasterId());
        respVO.setProjectCodeId(relation.getProjectCodeId());
        respVO.setFileNumber(relation.getRelatedFileNumberSnapshot());
        respVO.setFileName(relation.getRelatedFileNameSnapshot());
        respVO.setVersionNo(relation.getRelatedVersionNoSnapshot());
        // Historical status is not frozen in the original schema; do not invent one from current metadata.
        return respVO;
    }

    private FileVersion requireSelected(Long fileId) {
        FileVersion file=latestFileResolver.resolveSelected(fileId);
        if(file==null || !Objects.equals(file.tenantId(),TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(file.controlledFileId(),fileId) || file.masterId()==null) throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        return file;
    }
    private DccControlledFileDO requireWritableSnapshotTarget(DccControlledFileDO identity){
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(identity.getMasterId()==null)throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        relationStore.lockMaster(tenant,identity.getMasterId());
        var target=controlledFileMapper.selectByIdAndTenantForUpdate(tenant,identity.getId());
        if(target==null || !Objects.equals(tenant,target.getTenantId())
                || !Objects.equals(identity.getMasterId(),target.getMasterId()))
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        if(target.getControlledTime()!=null || target.getActivatedTime()!=null
                || (target.getProcessInstanceId()!=null && !target.getProcessInstanceId().isBlank())
                || java.util.Set.of("ACTIVE","CONTROLLED_PENDING_EFFECTIVE","OBSOLETE","SUPERSEDED",
                    "PENDING_MATRIX_REVIEW","PENDING_MATRIX_APPROVAL","PENDING_DOC_CONTROL_APPROVAL",
                    "PENDING_APPLICANT_REWORK","PENDING_APPLICANT_TRAINING_RECORD","TRAINING_IN_PROGRESS",
                    "PENDING_MANUAL_DISTRIBUTION","REJECTED","WITHDRAWN","APPROVING","APPROVED")
                    .contains(target.getStatus()==null?"":target.getStatus()))
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_APPROVAL_SNAPSHOT_FROZEN");
        return target;
    }
    private FileVersion requireLatest(Long masterId) {
        if(masterId==null) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_STABLE_IDENTITY_MISSING");
        FileVersion file=latestFileResolver.resolveLatest(masterId);
        if(file==null || !Objects.equals(file.tenantId(),TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(file.masterId(),masterId) || file.controlledFileId()==null)
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_RELATION_LATEST_CONTROLLED_MISSING");
        return file;
    }
    private record RelationCommand(Long sourceFileId,String payloadHash,long resultingVersion,String masterIds){}

}
