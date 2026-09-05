package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_BATCH_SIZE_SPLITS_GROUP;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID;
import static cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSourceGovernanceWriteGuard.requireExactlyOne;

@Service
public class DccControlledFileSourceGovernancePreparationService {

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileSourceGovernanceBatchMapper batchMapper;
    @Resource
    private DccControlledFileSourceGovernanceItemMapper itemMapper;
    @Resource
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Resource
    private DccControlledFileSourceOwnershipService ownershipService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private DccControlledFileSourceGovernanceManifestHasher manifestHasher;
    @Resource
    private DccControlledFileSourceGovernanceManifestService manifestService;

    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileSourceGovernancePreparationResult prepareBatch(
            String taskKey, int batchSize, Long startAfterControlledFileId) {
        if (StrUtil.isBlank(taskKey) || batchSize < 1
                || batchSize > DccControlledFileSourceGovernanceBatchService.MAX_BATCH_SIZE) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
        long normalizedStartAfterId = startAfterControlledFileId == null ? 0L : startAfterControlledFileId;
        if (normalizedStartAfterId < 0) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String requestSha256 = sha256(String.join("|", taskKey, String.valueOf(tenantId),
                String.valueOf(batchSize), String.valueOf(normalizedStartAfterId),
                DccControlledFileSourceGovernanceManifestService.CURRENT_RULE_VERSION,
                DccControlledFileSourceGovernanceManifestService.CURRENT_SCHEMA_VERSION));
        DccControlledFileSourceGovernanceBatchDO existing = batchMapper.selectByTaskKey(taskKey);
        if (existing != null) {
            if (!Objects.equals(existing.getRequestSha256(), requestSha256)) {
                throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
            }
            List<DccControlledFileSourceGovernanceItemDO> existingItems =
                    itemMapper.selectByBatchAndTenant(existing.getId(), tenantId);
            manifestService.requireVersioned(existing);
            manifestService.requireTenantInScope(existing, tenantId);
            manifestService.requireManifestContent(existing, existingItems);
            return summarize(existing, existingItems, normalizedStartAfterId);
        }

        Long snapshotMaxControlledFileId = controlledFileMapper.selectGlobalMaxControlledFileId();
        List<DccControlledFileDO> candidates = controlledFileMapper.selectEffectiveSourceGovernanceCandidates(
                tenantId, snapshotMaxControlledFileId, normalizedStartAfterId, batchSize);
        List<PreparedItem> preparedItems = prepareItems(tenantId, snapshotMaxControlledFileId,
                batchSize, candidates);
        String tenantScopeJson = "[" + tenantId + "]";
        long blockedCount = preparedItems.stream()
                .filter(item -> Objects.equals(item.decision().status(), "BLOCKED")).count();
        DccControlledFileSourceGovernanceBatchDO batch =
                DccControlledFileSourceGovernanceBatchDO.builder()
                        .taskKey(taskKey)
                        .tenantScopeJson(tenantScopeJson)
                        .tenantScopeSha256(sha256(tenantScopeJson))
                        .snapshotMaxControlledFileId(snapshotMaxControlledFileId)
                        .effectiveControlledFileCount((long) preparedItems.size())
                        .ruleVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_RULE_VERSION)
                        .schemaVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_SCHEMA_VERSION)
                        .manifestSha256("")
                        .requestSha256(requestSha256)
                        .batchStatus("PREPARED")
                        .completedCount(0L)
                        .blockedCount(blockedCount)
                        .failedCount(0L)
                        .build();
        List<DccControlledFileSourceGovernanceItemDO> itemRows = preparedItems.stream()
                .map(prepared -> toItem(null, tenantId, prepared)).toList();
        batch.setManifestSha256(manifestHasher.sha256(batch, itemRows));
        requireExactlyOne(batchMapper.insert(batch), "insert source governance batch");
        for (DccControlledFileSourceGovernanceItemDO item : itemRows) {
            item.setBatchId(batch.getId());
            requireExactlyOne(itemMapper.insert(item), "insert source governance item");
        }
        return summarize(batch, itemRows, normalizedStartAfterId);
    }

    private List<PreparedItem> prepareItems(Long tenantId, Long snapshotMaxControlledFileId, int batchSize,
                                             List<DccControlledFileDO> candidates) {
        List<PreparedItem> result = new ArrayList<>();
        Set<Long> preparedSourceIds = new HashSet<>();
        for (DccControlledFileDO candidate : candidates) {
            Long sourceFileId = candidate.getSourceFileId();
            if (sourceFileId != null && !preparedSourceIds.add(sourceFileId)) {
                continue;
            }
            List<DccControlledFileMapper.GlobalSourceReference> references = sourceFileId == null ? List.of()
                    : controlledFileMapper.selectGlobalEffectiveSourceReferences(
                    sourceFileId, snapshotMaxControlledFileId);
            List<DccControlledFileDO> groupCandidates = loadTenantGroup(tenantId, candidate, references);
            if (groupCandidates.size() > batchSize) {
                throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_BATCH_SIZE_SPLITS_GROUP,
                        "source:" + sourceFileId);
            }
            if (!result.isEmpty() && result.size() + groupCandidates.size() > batchSize) {
                break;
            }
            result.addAll(classifyGroup(tenantId, groupCandidates, references));
        }
        return result;
    }

    private List<DccControlledFileDO> loadTenantGroup(Long tenantId, DccControlledFileDO candidate,
                                                       List<DccControlledFileMapper.GlobalSourceReference> references) {
        if (references == null || references.isEmpty()) {
            return List.of(candidate);
        }
        List<DccControlledFileDO> group = new ArrayList<>();
        for (DccControlledFileMapper.GlobalSourceReference reference : references) {
            if (!Objects.equals(reference.getTenantId(), tenantId)) {
                continue;
            }
            DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantIncludingDeleted(
                    tenantId, reference.getControlledFileId());
            if (file != null && !Boolean.TRUE.equals(file.getDeleted())) {
                group.add(file);
            }
        }
        return group.isEmpty() ? List.of(candidate) : group;
    }

    private List<PreparedItem> classifyGroup(Long tenantId, List<DccControlledFileDO> group,
                                              List<DccControlledFileMapper.GlobalSourceReference> references) {
        Long sourceFileId = group.get(0).getSourceFileId();
        FileDO sourceFile = sourceFileId == null ? null : fileMapper.selectById(sourceFileId);
        boolean exists = sourceFile != null;
        boolean deleted = exists && Boolean.TRUE.equals(sourceFile.getDeleted());
        boolean locationComplete = exists && sourceFile.getConfigId() != null
                && StrUtil.isNotBlank(sourceFile.getPath());
        String actualSha256 = null;
        boolean readable = false;
        String readFailure = null;
        if (exists && !deleted && locationComplete) {
            try {
                actualSha256 = ownershipService.inspectSource(sourceFileId).sourceSha256();
                readable = true;
            } catch (RuntimeException failure) {
                readable = false;
                readFailure = failure.getClass().getSimpleName() + ": " + StrUtil.blankToDefault(
                        failure.getMessage(), "source read failed");
            }
        }
        List<DccControlledFileSourceGovernanceClassifier.GlobalReference> classifierReferences =
                references == null ? null : references.stream()
                        .map(reference -> new DccControlledFileSourceGovernanceClassifier.GlobalReference(
                                reference.getTenantId(), reference.getSourceFileId(), reference.getControlledFileId()))
                        .toList();
        List<PreparedItem> result = new ArrayList<>();
        for (DccControlledFileDO candidate : group) {
            DccControlledFileSourceOwnershipDO ownership = ownershipMapper.selectByControlledFileId(
                    tenantId, candidate.getId());
            DccControlledFileSourceGovernanceDecision decision = new DccControlledFileSourceGovernanceClassifier()
                    .classify(tenantId, candidate.getId(), sourceFileId, exists, deleted, locationComplete,
                            readable, ownership, actualSha256, classifierReferences, Set.of(tenantId));
            if (Objects.equals(decision.reasonCode(), "SOURCE_CONTENT_UNREADABLE") && readFailure != null) {
                decision = new DccControlledFileSourceGovernanceDecision(
                        decision.status(), decision.action(), decision.reasonCode(),
                        StrUtil.subWithLength(readFailure, 0, 1000));
            }
            result.add(new PreparedItem(candidate, sourceFile, actualSha256, references, decision));
        }
        return result;
    }

    private DccControlledFileSourceGovernanceItemDO toItem(
            Long batchId, Long tenantId, PreparedItem prepared) {
        DccControlledFileDO candidate = prepared.candidate();
        FileDO sourceFile = prepared.sourceFile();
        DccControlledFileSourceGovernanceDecision decision = prepared.decision();
        Long sourceFileId = candidate.getSourceFileId();
        return DccControlledFileSourceGovernanceItemDO.builder()
                .batchId(batchId)
                .tenantId(tenantId)
                .controlledFileId(candidate.getId())
                .legacySourceFileId(sourceFileId)
                .snapshotSourceFileId(sourceFileId)
                .snapshotSourceSha256(prepared.actualSha256())
                .snapshotSourceConfigId(sourceFile == null ? null : sourceFile.getConfigId())
                .snapshotSourcePath(sourceFile == null ? null : sourceFile.getPath())
                .snapshotSourceDeleted(sourceFile != null && Boolean.TRUE.equals(sourceFile.getDeleted()))
                .snapshotLocationHash(sourceFile == null ? null : locationHash(sourceFile))
                .snapshotHistoryEvidenceHash(DccControlledFileSourceGovernancePostflightService
                        .historyEvidenceHash(candidate))
                .sharedGroupKey(prepared.references() != null && prepared.references().size() > 1
                        ? "source:" + sourceFileId : null)
                .governanceAction(decision.action() == null ? "NO_ACTION" : decision.action())
                .itemStatus(decision.status())
                .blockerReasonCode(decision.reasonCode())
                .blockerDetail(decision.detail())
                .build();
    }

    private DccControlledFileSourceGovernancePreparationResult summarize(
            DccControlledFileSourceGovernanceBatchDO batch,
            List<DccControlledFileSourceGovernanceItemDO> items,
            Long startAfterControlledFileId) {
        int ready = (int) items.stream().filter(item -> Objects.equals(item.getItemStatus(), "READY")).count();
        int blocked = (int) items.stream().filter(item -> Objects.equals(item.getItemStatus(), "BLOCKED")).count();
        Long lastControlledFileId = items.stream()
                .map(DccControlledFileSourceGovernanceItemDO::getControlledFileId)
                .filter(Objects::nonNull).max(Long::compareTo).orElse(startAfterControlledFileId);
        return new DccControlledFileSourceGovernancePreparationResult(
                batch.getTaskKey(), batch.getBatchStatus(), batch.getRuleVersion(), batch.getSchemaVersion(),
                batch.getManifestSha256(), batch.getRequestSha256(), batch.getSnapshotMaxControlledFileId(),
                startAfterControlledFileId, lastControlledFileId,
                items.size(), ready, blocked);
    }

    private String locationHash(FileDO file) {
        return sha256(String.join("|", String.valueOf(file.getConfigId()), String.valueOf(file.getPath()),
                String.valueOf(file.getName()), String.valueOf(file.getType()), String.valueOf(file.getSize())));
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private record PreparedItem(DccControlledFileDO candidate, FileDO sourceFile, String actualSha256,
                                List<DccControlledFileMapper.GlobalSourceReference> references,
                                DccControlledFileSourceGovernanceDecision decision) {
    }
}
