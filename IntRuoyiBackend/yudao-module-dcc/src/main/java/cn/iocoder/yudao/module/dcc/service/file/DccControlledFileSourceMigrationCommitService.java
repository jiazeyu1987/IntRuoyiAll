package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceMigrationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceMigrationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_MIGRATION_CONFLICT;

@Service
public class DccControlledFileSourceMigrationCommitService {

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileSourceMigrationMapper migrationMapper;
    @Resource
    private DccControlledFileSourceOwnershipService ownershipService;

    @Transactional(rollbackFor = Exception.class)
    public void commitExistingSource(DccControlledFileDO candidate, DccControlledFileSourceMigrationDO migration,
                                     DccControlledFilePreparedSource preparedSource, Long actorId) {
        DccControlledFileDO current = loadCurrent(candidate.getId());
        if (!Objects.equals(current.getSourceFileId(), candidate.getSourceFileId())) {
            throw migrationConflict(candidate.getId());
        }
        ownershipService.claimSubmissionSource(candidate.getId(), preparedSource, actorId, "HISTORICAL_MIGRATION");
        completeMigration(migration, preparedSource, actorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void commitIsolatedSource(DccControlledFileDO candidate, DccControlledFileSourceMigrationDO migration,
                                     DccControlledFilePreparedSource preparedSource, Long actorId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileDO current = loadCurrent(candidate.getId());
        if (Objects.equals(current.getSourceFileId(), preparedSource.sourceFileId())) {
            ownershipService.claimSubmissionSource(candidate.getId(), preparedSource, actorId,
                    "HISTORICAL_MIGRATION");
            completeMigration(migration, preparedSource, actorId);
            return;
        }
        if (!Objects.equals(current.getSourceFileId(), migration.getLegacySourceFileId())) {
            throw migrationConflict(candidate.getId());
        }
        int updated = controlledFileMapper.updateSourceFileIdIncludingDeleted(tenantId, candidate.getId(),
                migration.getLegacySourceFileId(), preparedSource.sourceFileId(), actorId);
        if (updated != 1) {
            throw migrationConflict(candidate.getId());
        }
        ownershipService.claimSubmissionSource(candidate.getId(), preparedSource, actorId, "HISTORICAL_MIGRATION");
        completeMigration(migration, preparedSource, actorId);
    }

    private DccControlledFileDO loadCurrent(Long controlledFileId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileDO current =
                controlledFileMapper.selectByIdAndTenantIncludingDeleted(tenantId, controlledFileId);
        if (current == null) {
            throw migrationConflict(controlledFileId);
        }
        return current;
    }

    private void completeMigration(DccControlledFileSourceMigrationDO migration,
                                   DccControlledFilePreparedSource preparedSource, Long actorId) {
        migration.setIsolatedSourceFileId(preparedSource.sourceFileId());
        migration.setSourceSha256(preparedSource.sourceSha256());
        migration.setMigrationStatus("COMPLETED");
        migration.setErrorMessage(null);
        migration.setMigratedBy(actorId);
        migration.setMigratedTime(LocalDateTime.now());
        migrationMapper.updateById(migration);
    }

    private RuntimeException migrationConflict(Long controlledFileId) {
        return exception(CONTROLLED_FILE_SOURCE_MIGRATION_CONFLICT, controlledFileId);
    }
}
