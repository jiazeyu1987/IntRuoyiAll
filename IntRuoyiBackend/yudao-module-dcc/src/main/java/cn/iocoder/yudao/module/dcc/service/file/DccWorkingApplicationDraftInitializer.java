package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectApplicationSnapshotService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;
import java.util.Set;

/** One internal creation hook. Callers acquire project before Master/File; never inject Query or Workflow here. */
@Service
public class DccWorkingApplicationDraftInitializer {
    @Resource private DccProjectCodeMapper projectMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource(name = "dccControlledFileMapper") private DccControlledFileMapper fileMapper;
    @Resource private DccApplicationRoundService rounds;
    @Resource private DccProjectApplicationSnapshotService snapshots;
    /** Read-only existence/provenance check; all snapshot writes belong to B's official service. */
    @Resource private DccProjectApplicationAttributesMapper attributesMapper;
    @Resource private DccProjectAccessService projectAccess;
    @Resource private DccControlledFileCategoryPermissionSupport categoryPermission;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO initialize(Long actorId, Long sourceFileId, Long targetFileId,
                                                        DccProjectAttributes actual) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("工作稿属性初始化必须与真实文件创建同事务");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (actorId == null || actorId <= 0 || targetFileId == null || targetFileId <= 0
                || Objects.equals(sourceFileId, targetFileId)) throw invalid();
        var identity = fileMapper.selectById(targetFileId);
        if (identity == null || !Objects.equals(identity.getTenantId(), tenant)
                || identity.getDccProjectCodeId() == null || identity.getMasterId() == null) throw invalid();

        // Read the routing identity without locking, then take the shared project -> Master -> sorted File locks.
        var project = projectMapper.selectByIdForUpdate(identity.getDccProjectCodeId());
        if (project == null || !Objects.equals(project.getTenantId(), tenant) || !"ENABLE".equals(project.getStatus()))
            throw invalid();
        var master = masterMapper.selectByIdForUpdate(identity.getMasterId());
        DccControlledFileDO source = null;
        DccControlledFileDO target;
        if (sourceFileId == null) {
            target = fileMapper.selectByIdAndTenantForUpdate(tenant, targetFileId);
        } else {
            if (sourceFileId <= 0) throw invalid();
            var first = fileMapper.selectByIdAndTenantForUpdate(tenant, Math.min(sourceFileId, targetFileId));
            var second = fileMapper.selectByIdAndTenantForUpdate(tenant, Math.max(sourceFileId, targetFileId));
            source = sourceFileId < targetFileId ? first : second;
            target = sourceFileId < targetFileId ? second : first;
            if (source == null) throw invalid();
        }
        requireFile(target, tenant, project.getId(), master);
        if (!Objects.equals(actorId, target.getRequesterId()) || target.getControlledTime() != null
                || target.getCheckedOutBy() != null) throw invalid();
        String type = typeOf(target);
        if (!Objects.equals(target.getPredecessorControlledFileId(), sourceFileId)) throw invalid();
        projectAccess.assertProjectEditorOrOwner(actorId, project.getId());
        if (!categoryPermission.hasCategoryPermission(target.getCategoryId(), actorId, DccFileCategoryPermissionActionEnum.UPLOAD))
            throw new IllegalArgumentException("申请人不具备工作稿属性初始化权限");

        Integer sourceRound = null;
        boolean inherit = false;
        boolean fork = false;
        if (source == null) {
            if (!"UPLOAD".equals(type) || target.getRevisionBaseActiveControlledFileId() != null
                    || target.getSelectedIterationControlledFileId() != null || master.getLatestControlledFileId() != null
                    || master.getCurrentActiveControlledFileId() != null
                    || target.getRevisionChangeType() != null && !"INITIAL".equals(target.getRevisionChangeType())) throw invalid();
        } else {
            requireFile(source, tenant, project.getId(), master);
            if (!Objects.equals(source.getFileNumber(), target.getFileNumber())
                    || !Objects.equals(source.getFileTypeTaxonomyId(), target.getFileTypeTaxonomyId())
                    || !Objects.equals(source.getCategoryId(), target.getCategoryId())
                    || !Objects.equals(source.getSourceOriginalFileName(), target.getSourceOriginalFileName())) throw invalid();
            if (target.getSelectedIterationControlledFileId() != null
                    && (!Objects.equals(sourceFileId, target.getSelectedIterationControlledFileId())
                    || !Objects.equals(source.getVersionNo(), target.getSelectedIterationVersionNo())
                    || !Objects.equals(source.getSourceSha256(), target.getSourceSha256()))) throw invalid();
            if ("WORKING".equals(source.getStatus()) && source.getProcessInstanceId() == null) {
                boolean ownerReplacement="REPLACEMENT".equals(target.getRevisionChangeType())
                        && "REVISION".equals(type) && Objects.equals(sourceFileId,target.getSelectedIterationControlledFileId())
                        && Objects.equals(master.getLatestControlledFileId(),target.getRevisionSourceControlledFileId())
                        && Objects.equals(source.getRevisionBaseActiveControlledFileId(),target.getRevisionSourceControlledFileId());
                if(ownerReplacement)projectAccess.assertProjectOwner(actorId,project.getId());
                if (!(Objects.equals(actorId,source.getRequesterId()) || ownerReplacement) || !type.equals(typeOf(source))) throw invalid();
                sourceRound = rounds.requireDraft(type, sourceFileId);
                snapshots.readReservedDraft(actorId, project.getId(), type, sourceFileId); // Missing saved source fails before reserving target.
                inherit = true;
            } else if (Set.of("REJECTED", "PENDING_APPLICANT_REWORK", "WITHDRAWN").contains(source.getStatus())) {
                String expectedKey = "UPLOAD".equals(type) ? DccControlledFileProcessDefinitionKeys.UPLOAD : DccControlledFileProcessDefinitionKeys.REVISION;
                if (!Objects.equals(actorId, source.getRequesterId()) || !type.equals(typeOf(source))
                        || !expectedKey.equals(source.getProcessDefinitionKey()) || blank(source.getProcessInstanceId())) throw invalid();
                sourceRound = rounds.require(type, sourceFileId, source.getProcessInstanceId());
                snapshots.read(actorId, project.getId(), type, sourceFileId, source.getProcessInstanceId());
                fork = true;
            } else if (Set.of("ACTIVE", "CONTROLLED_PENDING_EFFECTIVE").contains(source.getStatus())
                    && source.getControlledTime() != null) {
                // A new revision application starts from current project defaults, never the controlled history's attributes.
                if (!"REVISION".equals(type) || !Objects.equals(master.getLatestControlledFileId(), sourceFileId)
                        || !Objects.equals(target.getRevisionBaseActiveControlledFileId(), sourceFileId)
                        || target.getSelectedIterationControlledFileId() != null) throw invalid();
            } else throw invalid();
        }

        // Replays after binding are read-only as well; never reserve another round for an already bound target.
        boolean bound = target.getProcessInstanceId() != null;
        if (bound && blank(target.getProcessInstanceId())) throw invalid();
        if (!bound && !"WORKING".equals(target.getStatus())) throw invalid();
        int targetRound = bound ? rounds.require(type, targetFileId, target.getProcessInstanceId())
                : rounds.reserveDraft(project.getId(), type, targetFileId);
        var existing = attributesMapper.find(type, targetFileId, targetRound);
        if (existing != null) {
            if (!Objects.equals(existing.getTenantId(), tenant) || !Objects.equals(existing.getProjectCodeId(), project.getId())
                    || !Objects.equals(existing.getSourceApplicationId(), inherit || fork ? sourceFileId : null)
                    || !Objects.equals(existing.getSourceApplicationRound(), sourceRound)) throw invalid();
            // B checks exact inherited source facts and reads back the target without covering subsequent manual edits.
            if (inherit) return snapshots.inheritReservedDraftToNewApplication(actorId, project.getId(), type,
                    sourceFileId, sourceRound, targetFileId, targetRound);
            if (fork) return snapshots.forkToNewApplication(actorId, project.getId(), type,
                    sourceFileId, sourceRound, targetFileId, targetRound);
            return bound ? snapshots.read(actorId, project.getId(), type, targetFileId, target.getProcessInstanceId())
                    : snapshots.readReservedDraft(actorId, project.getId(), type, targetFileId);
        }
        if (bound) throw invalid();
        if (!inherit && !fork) return snapshots.prepareDraft(actorId, project.getId(), type, targetFileId, actual);
        var saved = inherit ? snapshots.inheritReservedDraftToNewApplication(actorId, project.getId(), type,
                sourceFileId, sourceRound, targetFileId, targetRound)
                : snapshots.forkToNewApplication(actorId, project.getId(), type, sourceFileId, sourceRound, targetFileId, targetRound);
        return actual == null ? saved : snapshots.saveReservedDraft(actorId, project.getId(), type, targetFileId, actual);
    }

    private void requireFile(DccControlledFileDO file, Long tenant, Long project, DccControlledFileMasterDO master) {
        if (master == null || file == null || !Objects.equals(master.getTenantId(), tenant)
                || !Objects.equals(master.getDccProjectCodeId(), project) || !Objects.equals(file.getTenantId(), tenant)
                || !Objects.equals(file.getDccProjectCodeId(), project) || !Objects.equals(file.getMasterId(), master.getId())
                || file.getSourceFileId() == null || file.getSourceFileId() <= 0 || blank(file.getSourceSha256())
                || blank(file.getSourceOriginalFileName()) || blank(file.getFileNumber()) || blank(file.getVersionNo())) throw invalid();
    }

    private String typeOf(DccControlledFileDO file) {
        if ("NEW".equals(file.getChangeType())) return "UPLOAD";
        if ("REVISION".equals(file.getChangeType())) return "REVISION";
        throw invalid();
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private IllegalArgumentException invalid() { return new IllegalArgumentException("工作稿初始化的直接前驱、正文、申请人或正式身份不匹配"); }
}
