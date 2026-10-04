package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNameClaimMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NAME_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;

/**
 * Owns the minimum tenant-wide name reservation used by DCC uploads.
 */
@Service
@Validated
public class DccControlledFileNameClaimService {

    @Resource
    private DccControlledFileNameClaimMapper claimMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper reservationMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource(name = "dccControlledFileMapper") private DccControlledFileMapper fileMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper temporaryFileMapper;

    /** Read-only early gate. A bound ticket may replay only its exact own unsent NEW draft. */
    @Transactional(readOnly=true)
    public cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreated preflightNewSourceName(Long userId,Long categoryId,
            Long projectId,Long leafId,String originalName,String scopedSessionId,String bodySha256) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(userId==null || categoryId==null || projectId==null || leafId==null || StrUtil.isBlank(originalName)
                || StrUtil.isBlank(scopedSessionId) || StrUtil.isBlank(bodySha256))
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        if(originalName.codePointCount(0,originalName.length())>256 || originalName.indexOf('/')>=0 || originalName.indexOf('\\')>=0)
            throw new IllegalArgumentException("full source filename must be a basename of at most 256 characters");
        if(claimMapper.countUnresolvedNames(tenant)!=0)throw new IllegalStateException("historical name claims require verified original source names");
        var reservation=reservationMapper.readName(tenant,originalName.getBytes(StandardCharsets.UTF_8));
        if(reservation!=null && Integer.valueOf(1).equals(reservation.getActive()) && "LEGACY_GROUP".equals(reservation.getReservationKind()))
            throw exception(CONTROLLED_FILE_NAME_EXISTS);
        var occupied=claimMapper.selectActiveByName(tenant,originalName);
        if(occupied==null) {
            if(reservation!=null && Integer.valueOf(1).equals(reservation.getActive()))throw new IllegalStateException("active name reservation has no modern claim");
            return null;
        }
        var candidates=temporaryFileMapper.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO>()
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getTenantId,tenant)
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getUploaderId,userId)
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getCategoryId,categoryId)
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getSessionId,scopedSessionId)
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getPurpose,"SOURCE")
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getStatus,"BOUND")
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getCleanupStatus,"BOUND"));
        var exact=candidates.stream().filter(row->originalName.equals(row.getOriginalFileName()) && bodySha256.equals(row.getFileSha256())).toList();
        if(exact.size()!=1)throw exception(CONTROLLED_FILE_NAME_EXISTS);
        var ticket=exact.get(0);var draft=ticket.getBoundControlledFileId()==null?null:fileMapper.selectById(ticket.getBoundControlledFileId());
        var master=occupied.getMasterId()==null?null:masterMapper.selectById(occupied.getMasterId());
        if(draft==null || master==null || !Objects.equals(tenant,occupied.getTenantId()) || !Objects.equals(tenant,master.getTenantId())
                || !Objects.equals(tenant,draft.getTenantId()) || !Objects.equals(draft.getMasterId(),master.getId())
                || !Objects.equals(userId,draft.getRequesterId()) || !"NEW".equals(draft.getChangeType()) || !"WORKING".equals(draft.getStatus())
                || StrUtil.isNotBlank(draft.getProcessInstanceId()) || draft.getControlledTime()!=null
                || master.getLatestControlledFileId()!=null || master.getCurrentActiveControlledFileId()!=null
                || !Objects.equals(projectId,draft.getDccProjectCodeId()) || !Objects.equals(projectId,master.getDccProjectCodeId())
                || !Objects.equals(projectId,occupied.getDccProjectCodeId()) || !Objects.equals(leafId,draft.getFileTypeTaxonomyId())
                || !Objects.equals(leafId,master.getFileTypeTaxonomyLeafId()) || !Objects.equals(leafId,occupied.getFileTypeTaxonomyLeafId())
                || !Objects.equals(categoryId,draft.getCategoryId()) || !Objects.equals(draft.getFileNumber(),master.getNormalizedFileNumber())
                || !Objects.equals(draft.getFileNumber(),occupied.getNormalizedFileNumber()) || !originalName.equals(draft.getSourceOriginalFileName())
                || !bodySha256.equals(draft.getSourceSha256()) || StrUtil.isBlank(draft.getCreationIdempotencyKey())
                || StrUtil.isBlank(draft.getCreationPayloadHash()) || ticket.getStorageFileId()==null || StrUtil.isBlank(ticket.getUploadTicket()))
            throw exception(CONTROLLED_FILE_NAME_EXISTS);
        return new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreated(ticket.getUploadTicket(),ticket.getSessionId(),ticket.getPurpose(),ticket.getStatus(),
                ticket.getExpireTime(),ticket.getStorageFileId(),ticket.getOriginalFileName(),ticket.getContentType(),ticket.getFileSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public void claim(Long tenantId, String fileName, Long masterId) {
        throw new IllegalStateException("name-only reservation is retired; original source name and complete number identity require claimIdentity");
    }

    /** Number scope remains the formal project + leaf + normalized-number identity. */
    @Transactional(rollbackFor = Exception.class)
    public void claimIdentity(Long tenantId, String fileName, Long projectId, Long leafId, String number, Long masterId) {
        claimIdentity(tenantId,fileName,projectId,leafId,number,masterId,DccNameReservationContext.newLogicalFile());
    }

    @Transactional(rollbackFor=Exception.class)
    public void claimExistingVersion(Long tenantId,String name,Long projectId,Long leafId,String number,Long masterId,Long selectedId) {
        claimIdentity(tenantId,name,projectId,leafId,number,masterId,DccNameReservationContext.existingVersion(selectedId));
    }

    /** Actual persisted unsent NEW identity is required for replay. */
    @Transactional(rollbackFor=Exception.class)
    public void claimNewSubmission(Long tenantId,String name,Long projectId,Long leafId,String number,Long masterId,Long actorId,Long sourceFileId) {
        assertTenant(tenantId);
        lockMaster(tenantId,masterId);
        var existing=claimMapper.selectActiveByName(tenantId,name);
        if(existing!=null && Objects.equals(masterId,existing.getMasterId())) {
            var chain=Objects.requireNonNull(fileMapper.selectListByMasterIdForUpdate(masterId));
            var own=chain.stream().filter(f->Objects.equals(tenantId,f.getTenantId()) && Objects.equals(masterId,f.getMasterId())
                && Objects.equals(actorId,f.getRequesterId()) && Objects.equals(sourceFileId,f.getSourceFileId())
                && Objects.equals(name,f.getSourceOriginalFileName()) && "NEW".equals(f.getChangeType()) && "WORKING".equals(f.getStatus())
                && StrUtil.isBlank(f.getProcessInstanceId()) && f.getControlledTime()==null
                && StrUtil.isNotBlank(f.getCreationIdempotencyKey()) && StrUtil.isNotBlank(f.getCreationPayloadHash())).toList();
            if(own.size()!=1)throw exception(CONTROLLED_FILE_NAME_EXISTS);
            claimExistingVersion(tenantId,name,projectId,leafId,number,masterId,own.get(0).getId());
            return;
        }
        claimIdentity(tenantId,name,projectId,leafId,number,masterId);
    }

    private void claimIdentity(Long tenantId,String fileName,Long projectId,Long leafId,String number,Long masterId,DccNameReservationContext context) {
        if (tenantId == null || masterId == null || StrUtil.isBlank(fileName)) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        var master = lockMaster(tenantId, masterId);
        if (fileName.codePointCount(0, fileName.length()) > 256 || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("full source filename must be a basename of at most 256 characters");
        }
        if (claimMapper.countUnresolvedNames(tenantId) != 0) {
            throw new IllegalStateException("historical name claims require verified original source names");
        }
        if (projectId == null || leafId == null || StrUtil.isBlank(number) || number.length() > 128) {
            throw new IllegalArgumentException("complete formal number identity is required");
        }
        if (!Objects.equals(projectId, master.getDccProjectCodeId())
                || !Objects.equals(leafId, master.getFileTypeTaxonomyLeafId())
                || !Objects.equals(number, master.getNormalizedFileNumber())) {
            throw new IllegalArgumentException("reservation identity must match the locked formal master");
        }
        var numbered = claimMapper.selectActiveByNumber(tenantId, projectId, leafId, number.getBytes(StandardCharsets.UTF_8));
        if (numbered != null && !masterId.equals(numbered.getMasterId())) throw exception(CONTROLLED_FILE_NAME_EXISTS);
        if(reservationMapper.activeLegacyNumberOwners(tenantId,projectId,leafId,java.util.HexFormat.of().withUpperCase().formatHex(number.getBytes(StandardCharsets.UTF_8)),masterId)>0)
            throw exception(CONTROLLED_FILE_NAME_EXISTS);
        if(context.intent()==DccNameReservationContext.Intent.EXISTING_MASTER_VERSION) {
            var selected=fileMapper.selectById(context.selectedControlledFileId());
            if(selected==null || !Objects.equals(tenantId,selected.getTenantId()) || !Objects.equals(masterId,selected.getMasterId())
                || !Objects.equals(projectId,selected.getDccProjectCodeId()) || !Objects.equals(leafId,selected.getFileTypeTaxonomyId())
                || !Objects.equals(number,selected.getFileNumber()) || !fileName.equals(requireSourceName(selected)))
                throw new IllegalArgumentException("reservation requires the actual own selected source identity");
        }
        var reservation=reservationMapper.lockName(tenantId,fileName.getBytes(StandardCharsets.UTF_8));
        if(reservation!=null && Integer.valueOf(1).equals(reservation.getActive()) && "LEGACY_GROUP".equals(reservation.getReservationKind())) {
            boolean exactLegacy=context.intent()==DccNameReservationContext.Intent.EXISTING_MASTER_VERSION
                && reservationMapper.verifiedNames(tenantId,masterId,context.selectedControlledFileId()).contains(fileName);
            boolean ownedNewVersion=context.intent()==DccNameReservationContext.Intent.EXISTING_MASTER_VERSION
                && reservationMapper.verifiedOwnerName(tenantId,masterId,fileName.getBytes(StandardCharsets.UTF_8))>0
                && reservationMapper.currentVersionOwnsSource(tenantId,context.selectedControlledFileId())==1;
            if(!exactLegacy && !ownedNewVersion)
                throw exception(CONTROLLED_FILE_NAME_EXISTS);
            return;
        }
        if (numbered != null && !masterId.equals(numbered.getMasterId())) throw exception(CONTROLLED_FILE_NAME_EXISTS);
        String normalizedName = normalize(fileName);
        DccControlledFileNameClaimDO existing = claimMapper.selectActiveByName(tenantId, normalizedName);
        if (existing != null) {
            if (masterId.equals(existing.getMasterId()) && context.intent()==DccNameReservationContext.Intent.EXISTING_MASTER_VERSION) {
                if (!Objects.equals(projectId, existing.getDccProjectCodeId())
                        || !Objects.equals(leafId, existing.getFileTypeTaxonomyLeafId())
                        || !Objects.equals(number, existing.getNormalizedFileNumber())) {
                    throw new IllegalStateException("name claim has a different formal number identity");
                }
                reserveModern(tenantId,fileName,masterId,existing.getId(),reservation);
                return;
            }
            throw exception(CONTROLLED_FILE_NAME_EXISTS);
        }
        DccControlledFileNameClaimDO claim = DccControlledFileNameClaimDO.builder()
                .tenantId(tenantId)
                .normalizedName(normalizedName)
                .sourceOriginalFileName(fileName)
                .dccProjectCodeId(projectId).fileTypeTaxonomyLeafId(leafId).normalizedFileNumber(number)
                .masterId(masterId)
                .build();
        try {
            if (claimMapper.insert(claim) != 1) {
                throw exception(CONTROLLED_FILE_NAME_EXISTS);
            }
            reserveModern(tenantId,fileName,masterId,Objects.requireNonNull(claim.getId(),"persisted claim id"),reservation);
        } catch (DuplicateKeyException ex) {
            throw exception(CONTROLLED_FILE_NAME_EXISTS);
        }
    }

    public String requireSourceName(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO selected) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(selected==null || !Objects.equals(tenant,selected.getTenantId()) || selected.getMasterId()==null || selected.getId()==null)
            throw new IllegalArgumentException("selected source is not in current tenant");
        var actual=fileMapper.selectById(selected.getId());
        if(actual==null || !Objects.equals(actual.getTenantId(),tenant) || !Objects.equals(actual.getMasterId(),selected.getMasterId())
            || !Objects.equals(actual.getSourceFileId(),selected.getSourceFileId()) || !Objects.equals(actual.getSourceSha256(),selected.getSourceSha256())
            || !Objects.equals(actual.getVersionNo(),selected.getVersionNo()) || !Objects.equals(actual.getSourceOriginalFileName(),selected.getSourceOriginalFileName()))
            throw new IllegalArgumentException("selected source projection is not the actual persisted identity");
        var verified=reservationMapper.verifiedNames(tenant,selected.getMasterId(),selected.getId());
        if(StrUtil.isNotBlank(selected.getSourceOriginalFileName())) {
            if(!verified.isEmpty() && (verified.size()!=1 || !verified.get(0).equals(selected.getSourceOriginalFileName())))
                throw new IllegalStateException("modern source identity contradicts sealed legacy evidence");
            return selected.getSourceOriginalFileName();
        }
        if(verified.size()!=1)throw new IllegalStateException("selected historical source has no complete verified identity");
        return verified.get(0);
    }

    private void reserveModern(Long tenant,String name,Long master,Long claim,cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccSourceNameReservationDO row) {
        if(row==null) {
            if(reservationMapper.insertModern(tenant,name,claim,master)!=1)throw new IllegalStateException("name registry insert failed");
        } else if(Integer.valueOf(1).equals(row.getActive())) {
            if(!"MODERN".equals(row.getReservationKind()) || !Objects.equals(master,row.getModernMasterId()) || !Objects.equals(claim,row.getModernClaimId()))
                throw exception(CONTROLLED_FILE_NAME_EXISTS);
        } else if(!"NONE".equals(row.getReservationKind()) || reservationMapper.acquireReleased(tenant,row.getId(),row.getGeneration(),claim,master)!=1)
            throw exception(CONTROLLED_FILE_NAME_EXISTS);
    }

    public void release(Long tenantId, Long masterId) {
        throw new IllegalStateException("immediate name release is forbidden; use retainObsoleteIdentity/releaseExpiredIdentity");
    }

    /** A calls in the obsolete transaction with an explicitly approved retention deadline. */
    @Transactional(rollbackFor = Exception.class)
    public void retainObsoleteIdentity(Long tenantId, Long masterId, LocalDateTime obsoleteTime, LocalDateTime retainUntil) {
        lockMaster(tenantId, masterId);
        if (obsoleteTime == null || retainUntil == null || !retainUntil.isAfter(obsoleteTime)) {
            throw new IllegalArgumentException("explicit obsolete time and positive retention deadline are required");
        }
        var legacy=reservationMapper.lockLegacyNames(tenantId,masterId);
        if(!legacy.isEmpty()) {
            if(!obsoleteTime.plusYears(20).equals(retainUntil))throw new IllegalArgumentException("legacy retention requires twenty calendar years from actual obsolescence");
            for(var scope:legacy.stream().map(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccSourceNameReservationDO::getVerificationScopeId).collect(java.util.stream.Collectors.toSet())) {
                long expected=reservationMapper.activeLegacyEvidenceCount(tenantId,scope,masterId);
                if(expected<1 || reservationMapper.retainLegacy(tenantId,scope,masterId,obsoleteTime,retainUntil)!=expected)throw new IllegalStateException("legacy retention identity changed");
            }
            claimMapper.retainByMasterId(tenantId,masterId,obsoleteTime,retainUntil);
            return;
        }
        if(reservationMapper.rawLegacyClaimCount(tenantId,masterId)>0)throw new IllegalStateException("legacy retention requires complete verified scope");
        if (claimMapper.retainByMasterId(tenantId, masterId, obsoleteTime, retainUntil) < 1) {
            throw new IllegalStateException("no reserved source name/number identity for obsolete master");
        }
    }

    /** A must authorize the release operation; this service never deletes a history row. */
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseExpiredIdentity(Long tenantId, Long masterId, LocalDateTime now) {
        var master = lockMaster(tenantId, masterId);
        Objects.requireNonNull(now, "explicit release time");
        if (master.getCurrentActiveControlledFileId() != null) return false;
        var legacy=reservationMapper.lockLegacyNames(tenantId,masterId);
        if(!legacy.isEmpty()) {
            var scopes=legacy.stream().map(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccSourceNameReservationDO::getVerificationScopeId).collect(java.util.stream.Collectors.toSet());
            for(var scope:scopes)if(reservationMapper.unexpiredLegacy(tenantId,scope,masterId,now)!=0)return false;
            var versions=Objects.requireNonNull(fileMapper.selectListByMasterIdForUpdate(masterId));
            if(versions.stream().anyMatch(v->!Objects.equals(tenantId,v.getTenantId()) || !Set.of("OBSOLETE","SUPERSEDED","WITHDRAWN","REJECTED").contains(v.getStatus())))return false;
            if(claimMapper.countUnexpiredByMasterId(tenantId,masterId,now)!=0)return false;
            for(var scope:scopes) {
                long expected=reservationMapper.activeLegacyEvidenceCount(tenantId,scope,masterId);
                if(expected<1 || reservationMapper.releaseLegacyOwner(tenantId,scope,masterId,now)!=expected)throw new IllegalStateException("legacy release identity changed");
            }
            for(var row:legacy)reservationMapper.releaseEmptyGroup(tenantId,row.getId(),now);
            claimMapper.releaseByMasterId(tenantId,masterId);
            reservationMapper.releaseModern(tenantId,masterId,now);
            return true;
        }
        if(reservationMapper.rawLegacyClaimCount(tenantId,masterId)>0)throw new IllegalStateException("legacy release requires complete verified scope");
        if (claimMapper.countUnexpiredByMasterId(tenantId, masterId, now) != 0) return false;
        var versions = Objects.requireNonNull(fileMapper.selectListByMasterIdForUpdate(masterId), "version chain");
        if (versions.stream().anyMatch(v -> !Objects.equals(tenantId, v.getTenantId())
                || !Set.of("OBSOLETE", "SUPERSEDED", "WITHDRAWN", "REJECTED").contains(v.getStatus()))) return false;
        boolean released=claimMapper.releaseByMasterId(tenantId, masterId) > 0;
        if(released)reservationMapper.releaseModern(tenantId,masterId,now);
        return released;
    }

    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO lockMaster(Long tenantId, Long masterId) {
        assertTenant(tenantId);
        var master = masterId == null ? null : masterMapper.selectByIdForUpdate(masterId);
        if (master == null || !Objects.equals(tenantId, master.getTenantId())) throw new IllegalArgumentException("master not in current tenant");
        return master;
    }

    private void assertTenant(Long tenantId) {
        if (!Objects.equals(tenantId, TenantContextHolder.getRequiredTenantId())) throw new IllegalArgumentException("wrong tenant identity");
    }

    static String normalize(String fileName) {
        return fileName;
    }
}
