package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceMigrationDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceMigrationMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@Service
public class DccControlledFileSourceGovernanceExecutionService {

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileSourceGovernanceItemMapper itemMapper;
    @Resource
    private DccControlledFileSourceMigrationMapper migrationMapper;
    @Resource
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Resource
    private DccControlledFileSourceOwnershipService ownershipService;
    @Resource
    private DccControlledFileSourceMigrationCommitService commitService;
    @Resource
    private DccControlledFileSourceGovernanceManifestService manifestService;
    @Resource
    private FileMapper fileMapper;

    @Transactional(rollbackFor = Exception.class)
    public List<DccControlledFileSourceGovernanceExecutionResult> executeSharedGroup(
            DccControlledFileSourceGovernanceBatchDO batch,
            List<DccControlledFileSourceGovernanceItemDO> items,
            Set<Long> tenantScope,
            String manifestSha256,
            String requestSha256,
            Long actorId) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        manifestService.requireVersioned(batch);
        manifestService.requireConfirmed(batch, manifestSha256, requestSha256);
        DccControlledFileSourceGovernanceItemDO first = items.get(0);
        if (!Objects.equals(first.getGovernanceAction(), "COPY_SHARED_SOURCE")
                || items.stream().anyMatch(item -> !Objects.equals(item.getItemStatus(), "READY")
                || !Objects.equals(item.getBatchId(), batch.getId())
                || !Objects.equals(item.getTenantId(), first.getTenantId())
                || !Objects.equals(item.getSnapshotSourceFileId(), first.getSnapshotSourceFileId()))) {
            throw new IllegalArgumentException("shared governance group is not homogeneous and READY");
        }
        for (DccControlledFileSourceGovernanceItemDO item : items) {
            manifestService.requireItemInScope(batch, item, tenantScope);
            manifestService.requireProcessable(item);
        }
        List<DccControlledFileMapper.GlobalSourceReference> frozenReferences =
                controlledFileMapper.selectGlobalEffectiveSourceReferences(
                        first.getSnapshotSourceFileId(), batch.getSnapshotMaxControlledFileId());
        if (frozenReferences == null) {
            return blockGroup(items, "SOURCE_GLOBAL_REFERENCE_CHECK_UNAVAILABLE",
                    "共享组全局引用核验不可用");
        }
        if (frozenReferences.stream().anyMatch(reference -> !tenantScope.contains(reference.getTenantId()))) {
            return blockGroup(items, "SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE",
                    "共享组包含冻结租户范围外的有效引用");
        }
        Set<Long> manifestFileIds = items.stream()
                .map(DccControlledFileSourceGovernanceItemDO::getControlledFileId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> globalFileIds = frozenReferences.stream()
                .map(DccControlledFileMapper.GlobalSourceReference::getControlledFileId)
                .collect(java.util.stream.Collectors.toSet());
        if (items.size() != frozenReferences.size() || !manifestFileIds.equals(globalFileIds)) {
            return blockGroup(items, "SOURCE_GROUP_MANIFEST_INCOMPLETE",
                    "共享组清单与冻结全局引用集合不一致");
        }
        List<Long> newlyCreatedCopies = new ArrayList<>();
        List<DccControlledFileSourceGovernanceExecutionResult> results = new ArrayList<>();
        try {
            for (DccControlledFileSourceGovernanceItemDO item : items) {
                Long previousIsolatedSourceFileId = item.getIsolatedSourceFileId();
                DccControlledFileSourceGovernanceExecutionResult result = executeItemInternal(
                        batch, item, tenantScope, manifestSha256, requestSha256, actorId, frozenReferences);
                if (previousIsolatedSourceFileId == null && item.getIsolatedSourceFileId() != null) {
                    newlyCreatedCopies.add(item.getIsolatedSourceFileId());
                }
                results.add(result);
                if (!Objects.equals(result.status(), "COMPLETED")) {
                    throw new IllegalStateException("shared governance group did not complete: "
                            + result.controlledFileId() + ":" + result.status());
                }
            }
            return List.copyOf(results);
        } catch (RuntimeException failure) {
            for (Long copyId : newlyCreatedCopies) {
                ownershipService.cleanupFailedCopy(copyId, failure);
            }
            throw failure;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileSourceGovernanceExecutionResult executeItem(
            DccControlledFileSourceGovernanceBatchDO batch,
            DccControlledFileSourceGovernanceItemDO item,
            Set<Long> tenantScope,
            String manifestSha256,
            String requestSha256,
            Long actorId) {
        return executeItemInternal(batch, item, tenantScope, manifestSha256, requestSha256, actorId, null);
    }

    private DccControlledFileSourceGovernanceExecutionResult executeItemInternal(
            DccControlledFileSourceGovernanceBatchDO batch,
            DccControlledFileSourceGovernanceItemDO item,
            Set<Long> tenantScope,
            String manifestSha256,
            String requestSha256,
            Long actorId,
            List<DccControlledFileMapper.GlobalSourceReference> frozenReferences) {
        manifestService.requireVersioned(batch);
        manifestService.requireConfirmed(batch, manifestSha256, requestSha256);
        manifestService.requireItemInScope(batch, item, tenantScope);
        if (manifestService.isCompleted(item)) {
            return result(item, "COMPLETED", item.getGovernanceAction(), null, "已完成，幂等返回");
        }
        manifestService.requireProcessable(item);

        Long tenantId = item.getTenantId();
        DccControlledFileDO candidate = controlledFileMapper.selectByIdAndTenantIncludingDeleted(
                tenantId, item.getControlledFileId());
        if (candidate == null || Boolean.TRUE.equals(candidate.getDeleted())
                || !Objects.equals(candidate.getTenantId(), tenantId)
                || !Objects.equals(candidate.getSourceFileId(), item.getSnapshotSourceFileId())
                || !Objects.equals(candidate.getSourceFileId(), item.getLegacySourceFileId())) {
            return block(item, "SNAPSHOT_DRIFTED", "受控记录已删除或 source_file_id 与清单快照不一致");
        }

        Long sourceFileId = candidate.getSourceFileId();
        List<DccControlledFileMapper.GlobalSourceReference> references = frozenReferences != null
                ? frozenReferences
                : controlledFileMapper.selectGlobalEffectiveSourceReferences(
                        sourceFileId, batch.getSnapshotMaxControlledFileId());
        FileDO sourceFile = fileMapper.selectById(sourceFileId);
        boolean sourceExists = sourceFile != null;
        boolean sourceDeleted = sourceExists && Boolean.TRUE.equals(sourceFile.getDeleted());
        boolean locationComplete = sourceExists && sourceFile.getConfigId() != null
                && sourceFile.getPath() != null && !sourceFile.getPath().isBlank();
        boolean contentReadable = false;
        String actualSha256 = null;
        DccControlledFilePreparedSource inspectedSource = null;
        if (sourceExists && !sourceDeleted && locationComplete) {
            try {
                inspectedSource = ownershipService.inspectSource(sourceFileId);
                actualSha256 = inspectedSource.sourceSha256();
                contentReadable = true;
            } catch (RuntimeException ex) {
                return fail(item, ex);
            }
        }
        DccControlledFileSourceOwnershipDO ownership = ownershipMapper.selectByControlledFileId(
                tenantId, item.getControlledFileId());
        DccControlledFileSourceGovernanceDecision decision = new DccControlledFileSourceGovernanceClassifier()
                .classify(tenantId, item.getControlledFileId(), sourceFileId, sourceExists, sourceDeleted,
                        locationComplete, contentReadable, ownership, actualSha256,
                        references == null ? null : references.stream()
                                .map(reference -> new DccControlledFileSourceGovernanceClassifier.GlobalReference(
                                        reference.getTenantId(), reference.getSourceFileId(), reference.getControlledFileId()))
                                .toList(), tenantScope);
        if ("BLOCKED".equals(decision.status())) {
            return block(item, decision.reasonCode(), decision.detail());
        }
        if (!Objects.equals(item.getSnapshotSourceSha256(), actualSha256)) {
            return block(item, "SNAPSHOT_DRIFTED", "源文件正文 SHA-256 与治理清单快照不一致");
        }
        if (item.getSnapshotSourceDeleted() != null
                && !Objects.equals(item.getSnapshotSourceDeleted(), sourceDeleted)) {
            return block(item, "SNAPSHOT_DRIFTED", "源文件删除状态与治理清单快照不一致");
        }
        if (item.getSnapshotSourceConfigId() != null
                && !Objects.equals(item.getSnapshotSourceConfigId(), sourceFile.getConfigId())) {
            return block(item, "SNAPSHOT_DRIFTED", "源文件配置与治理清单快照不一致");
        }
        if (item.getSnapshotSourcePath() != null
                && !Objects.equals(item.getSnapshotSourcePath(), sourceFile.getPath())) {
            return block(item, "SNAPSHOT_DRIFTED", "源文件路径与治理清单快照不一致");
        }
        if (item.getSnapshotLocationHash() != null
                && !Objects.equals(item.getSnapshotLocationHash(), locationHash(sourceFile))) {
            return block(item, "SNAPSHOT_DRIFTED", "源文件定位摘要与治理清单快照不一致");
        }
        if (!Objects.equals(item.getGovernanceAction(), decision.action())) {
            return block(item, "SNAPSHOT_DRIFTED", "治理动作与最新全局引用核验结果不一致");
        }

        DccControlledFileSourceMigrationDO migration = migrationMapper.selectByControlledFileId(
                tenantId, item.getControlledFileId());
        if (migration == null) {
            migration = DccControlledFileSourceMigrationDO.builder()
                    .tenantId(tenantId)
                    .controlledFileId(item.getControlledFileId())
                    .legacySourceFileId(sourceFileId)
                    .migrationStatus("PENDING")
                    .build();
            migrationMapper.insert(migration);
        }
        DccControlledFilePreparedSource preparedSource;
        Long newlyCreatedCopyId = null;
        if ("CLAIM_SOURCE".equals(decision.action())) {
            preparedSource = inspectedSource;
            commitService.commitExistingSource(candidate, migration, preparedSource, actorId,
                    batch.getId(), item.getId());
        } else {
            boolean copyCreated = false;
            if (migration.getIsolatedSourceFileId() != null) {
                try {
                    preparedSource = ownershipService.inspectSource(migration.getIsolatedSourceFileId());
                } catch (RuntimeException ex) {
                    return fail(item, ex);
                }
                if (!Objects.equals(preparedSource.originSourceFileId(), sourceFileId)
                        || !Objects.equals(preparedSource.sourceSha256(), item.getSnapshotSourceSha256())) {
                    return block(item, "SNAPSHOT_DRIFTED", "已存在的独立副本与治理清单证据不一致");
                }
            } else {
                try {
                    preparedSource = ownershipService.createVerifiedCopy(sourceFileId);
                    copyCreated = true;
                    newlyCreatedCopyId = preparedSource.sourceFileId();
                } catch (RuntimeException ex) {
                    return fail(item, ex);
                }
                if (!Objects.equals(preparedSource.sourceSha256(), item.getSnapshotSourceSha256())) {
                    ownershipService.cleanupFailedCopy(preparedSource.sourceFileId(),
                            new IllegalStateException("复制源文件 SHA-256 与治理清单快照不一致"));
                    return block(item, "SNAPSHOT_DRIFTED", "复制源文件 SHA-256 与治理清单快照不一致");
                }
                migration.setIsolatedSourceFileId(preparedSource.sourceFileId());
                migration.setSourceSha256(preparedSource.sourceSha256());
                migration.setMigrationStatus("COPY_VERIFIED");
            }
            try {
                if (copyCreated) {
                    migrationMapper.updateById(migration);
                }
                commitService.commitIsolatedSource(candidate, migration, preparedSource, actorId,
                        batch.getId(), item.getId());
            } catch (RuntimeException ex) {
                if (copyCreated) {
                    ownershipService.cleanupFailedCopy(preparedSource.sourceFileId(), ex);
                }
                throw ex;
            }
        }
        item.setIsolatedSourceFileId(preparedSource.sourceFileId());
        item.setOriginSourceFileId(preparedSource.originSourceFileId());
        item.setSourceSha256(preparedSource.sourceSha256());
        item.setItemStatus("COMPLETED");
        item.setBlockerReasonCode(null);
        item.setBlockerDetail(null);
        item.setLastError(null);
        item.setProcessedBy(actorId);
        item.setProcessedTime(LocalDateTime.now());
        try {
            itemMapper.updateById(item);
        } catch (RuntimeException ex) {
            if (newlyCreatedCopyId != null) {
                ownershipService.cleanupFailedCopy(newlyCreatedCopyId, ex);
            }
            throw ex;
        }
        return result(item, "COMPLETED", decision.action(), null, null);
    }

    private DccControlledFileSourceGovernanceExecutionResult block(
            DccControlledFileSourceGovernanceItemDO item, String reasonCode, String detail) {
        item.setItemStatus("BLOCKED");
        item.setBlockerReasonCode(reasonCode);
        item.setBlockerDetail(detail);
        item.setProcessedTime(LocalDateTime.now());
        itemMapper.updateById(item);
        return result(item, "BLOCKED", item.getGovernanceAction(), reasonCode, detail);
    }

    private List<DccControlledFileSourceGovernanceExecutionResult> blockGroup(
            List<DccControlledFileSourceGovernanceItemDO> items, String reasonCode, String detail) {
        List<DccControlledFileSourceGovernanceExecutionResult> results = new ArrayList<>();
        for (DccControlledFileSourceGovernanceItemDO item : items) {
            results.add(block(item, reasonCode, detail));
        }
        return List.copyOf(results);
    }

    private DccControlledFileSourceGovernanceExecutionResult fail(
            DccControlledFileSourceGovernanceItemDO item, RuntimeException failure) {
        item.setItemStatus("FAILED");
        item.setLastError(failure.getMessage());
        item.setProcessedTime(LocalDateTime.now());
        itemMapper.updateById(item);
        return result(item, "FAILED", item.getGovernanceAction(), null, failure.getMessage());
    }

    private String locationHash(FileDO file) {
        String value = String.join("|", String.valueOf(file.getConfigId()), String.valueOf(file.getPath()),
                String.valueOf(file.getName()), String.valueOf(file.getType()), String.valueOf(file.getSize()));
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private DccControlledFileSourceGovernanceExecutionResult result(
            DccControlledFileSourceGovernanceItemDO item, String status, String action,
            String reasonCode, String detail) {
        return new DccControlledFileSourceGovernanceExecutionResult(
                item.getControlledFileId(), status, action, reasonCode, detail);
    }
}
