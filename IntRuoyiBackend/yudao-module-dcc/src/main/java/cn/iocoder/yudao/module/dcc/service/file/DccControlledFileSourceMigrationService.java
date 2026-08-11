package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceMigrationDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceMigrationMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_MIGRATION_CONFLICT;

@Service
public class DccControlledFileSourceMigrationService {

    public static final int MAX_BATCH_SIZE = 200;

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileSourceMigrationMapper migrationMapper;
    @Resource
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Resource
    private DccControlledFileSourceOwnershipService ownershipService;
    @Resource
    private DccControlledFileSourceMigrationCommitService commitService;

    public DccControlledFileSourceMigrationReadiness getReadiness() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        long total = controlledFileMapper.countAllSourceReferences(tenantId);
        long unowned = controlledFileMapper.countUnownedSourceReferences(tenantId);
        return new DccControlledFileSourceMigrationReadiness(total, total - unowned, unowned,
                controlledFileMapper.countSharedSourceGroups(tenantId),
                controlledFileMapper.countSharedSourceRecords(tenantId),
                migrationMapper.countByStatus(tenantId, "FAILED"));
    }

    public DccControlledFileSourceMigrationResult migrateBatch(Long actorId, int batchSize) {
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("batchSize must be between 1 and " + MAX_BATCH_SIZE);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<DccControlledFileDO> candidates =
                controlledFileMapper.selectUnownedSourceReferences(tenantId, batchSize);
        int processed = 0;
        for (DccControlledFileDO candidate : candidates) {
            migrateOne(tenantId, candidate, actorId);
            processed++;
        }
        return new DccControlledFileSourceMigrationResult(processed,
                controlledFileMapper.countUnownedSourceReferences(tenantId));
    }

    private void migrateOne(Long tenantId, DccControlledFileDO candidate, Long actorId) {
        DccControlledFileSourceMigrationDO migration = getOrCreateMigration(tenantId, candidate);
        try {
            if (migration.getIsolatedSourceFileId() != null) {
                DccControlledFilePreparedSource prepared = new DccControlledFilePreparedSource(
                        migration.getIsolatedSourceFileId(), migration.getLegacySourceFileId(),
                        migration.getSourceSha256(), true);
                commitService.commitIsolatedSource(candidate, migration, prepared, actorId);
                return;
            }
            DccControlledFileSourceOwnershipDO existingOwner =
                    ownershipMapper.selectBySourceFileId(tenantId, candidate.getSourceFileId());
            if (existingOwner == null || candidate.getId().equals(existingOwner.getControlledFileId())) {
                DccControlledFilePreparedSource prepared = ownershipService.inspectSource(candidate.getSourceFileId());
                commitService.commitExistingSource(candidate, migration, prepared, actorId);
                return;
            }
            DccControlledFilePreparedSource prepared =
                    ownershipService.createVerifiedCopy(candidate.getSourceFileId());
            migration.setIsolatedSourceFileId(prepared.sourceFileId());
            migration.setSourceSha256(prepared.sourceSha256());
            migration.setMigrationStatus("COPY_VERIFIED");
            migration.setErrorMessage(null);
            migrationMapper.updateById(migration);
            commitService.commitIsolatedSource(candidate, migration, prepared, actorId);
        } catch (RuntimeException ex) {
            markFailed(migration, ex);
            throw ex;
        }
    }

    private DccControlledFileSourceMigrationDO getOrCreateMigration(Long tenantId,
                                                                     DccControlledFileDO candidate) {
        DccControlledFileSourceMigrationDO existing =
                migrationMapper.selectByControlledFileId(tenantId, candidate.getId());
        if (existing != null) {
            validateMigrationSource(existing, candidate);
            return existing;
        }
        DccControlledFileSourceMigrationDO migration = DccControlledFileSourceMigrationDO.builder()
                .tenantId(tenantId)
                .controlledFileId(candidate.getId())
                .legacySourceFileId(candidate.getSourceFileId())
                .migrationStatus("PENDING")
                .build();
        try {
            migrationMapper.insert(migration);
            return migration;
        } catch (DuplicateKeyException ex) {
            DccControlledFileSourceMigrationDO concurrent =
                    migrationMapper.selectByControlledFileId(tenantId, candidate.getId());
            if (concurrent == null) {
                throw ex;
            }
            validateMigrationSource(concurrent, candidate);
            return concurrent;
        }
    }

    private void validateMigrationSource(DccControlledFileSourceMigrationDO migration,
                                         DccControlledFileDO candidate) {
        if (candidate.getSourceFileId().equals(migration.getLegacySourceFileId())
                || candidate.getSourceFileId().equals(migration.getIsolatedSourceFileId())) {
            return;
        }
        throw exception(CONTROLLED_FILE_SOURCE_MIGRATION_CONFLICT, candidate.getId());
    }

    private void markFailed(DccControlledFileSourceMigrationDO migration, RuntimeException failure) {
        migration.setMigrationStatus("FAILED");
        migration.setErrorMessage(StrUtil.subWithLength(failure.getMessage(), 0, 1000));
        migrationMapper.updateById(migration);
    }
}
