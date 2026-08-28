package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceManifestDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTraceManifestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.BATCH_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.FLOW8_SOURCE_PRECHECK_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.FLOW8_SOURCE_PRECHECK_STALE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.FLOW8_TRACE_LINK_ORIGIN_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.RELEASE_DECISION_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.TRACE_CAPTURE_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.TRACE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.TRACE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.TRACE_SOURCE_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.TRACE_MAPPING_BLOCKED;

@Service
@RequiredArgsConstructor
public class MesProEdhrBatchTraceabilityServiceImpl implements MesProEdhrBatchTraceabilityService {

    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionOriginMapper originMapper;
    private final MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper;
    private final MesProEdhrBatchExecutionTraceManifestMapper manifestMapper;
    private final MesProEdhrBatchTraceabilityValidator validator = new MesProEdhrBatchTraceabilityValidator();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrBatchTraceabilityRespVO capture(MesProEdhrBatchTraceCaptureCommand command) {
        requireBatch(command == null ? null : command.getBatchExecutionId());
        MesProEdhrBatchTraceValidationResult validation = validator.validate(command);
        if (!validation.valid()) {
            throw exception(TRACE_CAPTURE_BLOCKED, validation.blockerCode());
        }
        Long batchExecutionId = command.getBatchExecutionId();
        String entryType = MesProEdhrBatchTraceFormalSourceResolver.isActiveOrderEntryType(command.getEntryType())
                ? MesProEdhrBatchTraceFormalSourceResolver.ACTIVE_ORDER_COMPLETION
                : command.getEntryType();
        MesProEdhrBatchExecutionOriginDO existing = originMapper.selectByBatchAndOriginKey(
                batchExecutionId, command.getOriginKey());
        if (existing != null) {
            if (Objects.equals(existing.getIdempotencyKey(), command.getIdempotencyKey())
                    && Objects.equals(existing.getSourceBundleHash(), command.getSourceBundleHash())) {
                return getTraceability(batchExecutionId);
            }
            throw exception(TRACE_IDEMPOTENCY_CONFLICT);
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime now = LocalDateTime.now();
        MesProEdhrBatchExecutionOriginDO origin = MesProEdhrBatchExecutionOriginDO.builder()
                .tenantId(tenantId).batchExecutionId(batchExecutionId)
                .entryType(entryType).originKey(command.getOriginKey())
                .activeOrderId(command.getActiveOrderId()).workOrderId(command.getWorkOrderId())
                .completionTransactionId(command.getCompletionTransactionId()).completionVersion(command.getCompletionVersion())
                .completionBackfillReceiptId(command.getCompletionBackfillReceiptId())
                .completionBackfillReceiptHash(command.getCompletionBackfillReceiptHash())
                .pickListBindingId(command.getPickListBindingId()).pickListId(command.getPickListId())
                .pickListBindingVersion(command.getPickListBindingVersion()).hasActualLoss(command.getHasActualLoss())
                .sourceSnapshotHash(command.getSourceSnapshotHash())
                .batchProvisionReceiptId(command.getBatchProvisionReceiptId()).batchProvisionStatus(command.getBatchProvisionStatus())
                .sourceCredentialId(command.getSourceCredentialId()).sourceCredentialHash(command.getSourceCredentialHash())
                .sourceBundleHash(command.getSourceBundleHash())
                .idempotencyKey(command.getIdempotencyKey()).relationStatus("CAPTURED")
                .capturedBy(command.getCapturedBy()).capturedAt(now).build();
        if (originMapper.insert(origin) != 1 || origin.getId() == null) {
            throw exception(TRACE_PERSIST_FAILED);
        }
        insertLinks(batchExecutionId, origin.getId(), command.getSources(), command.getCapturedBy(), now);
        if (!MesProEdhrBatchTraceFormalSourceResolver.isActiveOrderEntryType(entryType)) {
            insertNotApplicableLinks(batchExecutionId, origin.getId(), entryType, command.getCapturedBy(), now);
        }
        appendManifest(batchExecutionId, command.getCapturedBy(), "TRACE_CAPTURED");
        return getTraceability(batchExecutionId);
    }

    @Override
    @Transactional(readOnly = true)
    public MesProEdhrBatchTraceabilityRespVO getTraceability(Long batchExecutionId) {
        requireBatch(batchExecutionId);
        List<MesProEdhrBatchExecutionOriginDO> origins = originMapper.selectListByBatchExecutionId(batchExecutionId);
        List<MesProEdhrBatchExecutionTraceLinkDO> links = traceLinkMapper.selectListByBatchExecutionId(batchExecutionId);
        List<MesProEdhrBatchExecutionTraceManifestDO> manifests = manifestMapper.selectListByBatchExecutionId(batchExecutionId);
        if (!isTraceCaptured(origins, links, manifests)) {
            throw exception(TRACE_CAPTURE_BLOCKED, TRACE_MAPPING_BLOCKED);
        }
        MesProEdhrBatchExecutionTraceManifestDO manifest = manifests.isEmpty() ? null : manifests.get(manifests.size() - 1);
        return new MesProEdhrBatchTraceabilityRespVO().setBatchExecutionId(batchExecutionId)
                .setOrigins(origins.stream().map(this::toOrigin).toList())
                .setTraceLinks(links.stream().map(this::toTraceLink).toList())
                .setLatestManifest(manifest == null ? null : toManifest(manifest))
                .setManifestHistory(manifests.stream().map(this::toManifest).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MesProEdhrBatchTraceabilityRespVO> listTraceability(Long activeOrderId, Long workOrderId,
                                                                     Long pickListId, Long releaseApplicationId,
                                                                     String entryType) {
        Set<Long> releaseBatchIds = releaseApplicationId == null ? null
                : new HashSet<>(traceLinkMapper.selectBatchExecutionIdsByReleaseApplicationId(releaseApplicationId));
        if (releaseBatchIds != null && releaseBatchIds.isEmpty()) {
            return List.of();
        }
        return originMapper.selectListByTraceFilter(activeOrderId, workOrderId, pickListId, entryType).stream()
                .map(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId).distinct()
                .filter(batchId -> releaseBatchIds == null || releaseBatchIds.contains(batchId))
                .map(this::getTraceability).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrBatchTraceabilityRespVO appendReleaseDecision(MesProEdhrBatchTraceReleaseDecisionCommand command) {
        requireBatch(command == null ? null : command.getBatchExecutionId());
        if (command == null || command.getOriginId() == null || command.getReleaseApplicationId() == null
                || command.getReleaseDecisionId() == null
                || isBlank(command.getSourceSnapshotJson()) || isBlank(command.getSourceSnapshotHash())
                || isBlank(command.getIdempotencyKey())) {
            throw exception(RELEASE_DECISION_SOURCE_REQUIRED);
        }
        if (!canonicalHash(command.getSourceSnapshotJson()).equalsIgnoreCase(command.getSourceSnapshotHash())) {
            throw exception(TRACE_SOURCE_CONFLICT);
        }
        List<MesProEdhrBatchExecutionOriginDO> origins = originMapper.selectListByBatchExecutionId(command.getBatchExecutionId());
        if (origins.isEmpty()) {
            throw exception(TRACE_CAPTURE_BLOCKED, "TRACE_ORIGIN_REQUIRED");
        }
        Long originId = resolveReleaseOriginId(origins, command.getOriginId());
        if (originId == null) {
            throw exception(TRACE_CAPTURE_BLOCKED, "TRACE_ORIGIN_REQUIRED");
        }
        String identity = identityOf(MesProEdhrBatchTraceLinkType.RELEASE_DECISION,
                "RELEASE_APPLICATION", command.getReleaseApplicationId(), null,
                command.getReleaseDecisionId());
        MesProEdhrBatchExecutionTraceLinkDO existing = traceLinkMapper.selectByIdentityKey(command.getBatchExecutionId(), identity);
        if (existing != null) {
            if (Objects.equals(existing.getSnapshotHash(), command.getSourceSnapshotHash())
                    && Objects.equals(existing.getIdempotencyKey(), command.getIdempotencyKey())) {
                return getTraceability(command.getBatchExecutionId());
            }
            throw exception(TRACE_IDEMPOTENCY_CONFLICT);
        }
        MesProEdhrBatchExecutionTraceLinkDO link = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId()).batchExecutionId(command.getBatchExecutionId())
                .originId(originId).linkType(MesProEdhrBatchTraceLinkType.RELEASE_DECISION)
                .sourceObjectType("RELEASE_APPLICATION").sourceObjectId(command.getReleaseApplicationId())
                .sourceEventId(command.getReleaseDecisionId()).sourceIdentityKey(identity)
                .idempotencyKey(command.getIdempotencyKey())
                .snapshotJson(command.getSourceSnapshotJson()).snapshotHash(command.getSourceSnapshotHash())
                .relationStatus("CAPTURED").capturedBy(command.getCapturedBy()).capturedAt(LocalDateTime.now()).build();
        if (traceLinkMapper.insert(link) != 1) {
            throw exception(TRACE_PERSIST_FAILED);
        }
        appendManifest(command.getBatchExecutionId(), command.getCapturedBy(), "RELEASE_DECISION_CAPTURED");
        return getTraceability(command.getBatchExecutionId());
    }

    @Override
    @Transactional(readOnly = true)
    public MesProEdhrBatchTraceSourcePrecheckRespVO resolveSourcePrecheck(
            MesProEdhrBatchTraceSourcePrecheckCommand command) {
        if (command == null || command.getBatchExecutionId() == null) {
            throw exception(FLOW8_SOURCE_PRECHECK_REQUIRED);
        }
        requireBatch(command.getBatchExecutionId());
        if (command.getOriginLinkId() == null) {
            return resolveSourcePrecheckWithoutLinkId(command,
                    originMapper.selectListByBatchExecutionId(command.getBatchExecutionId()),
                    traceLinkMapper.selectListByBatchExecutionId(command.getBatchExecutionId()),
                    LocalDateTime.now());
        }
        MesProEdhrBatchExecutionTraceLinkDO link = traceLinkMapper.selectByIdAndBatchExecutionId(
                command.getOriginLinkId(), command.getBatchExecutionId());
        if (link == null) {
            throw exception(FLOW8_TRACE_LINK_ORIGIN_MISMATCH);
        }
        MesProEdhrBatchExecutionOriginDO origin = originMapper.selectListByBatchExecutionId(
                        command.getBatchExecutionId()).stream()
                .filter(candidate -> Objects.equals(candidate.getId(), link.getOriginId()))
                .findFirst().orElse(null);
        return resolveSourcePrecheck(command, origin, link, LocalDateTime.now());
    }

    static MesProEdhrBatchTraceSourcePrecheckRespVO resolveSourcePrecheckWithoutLinkId(
            MesProEdhrBatchTraceSourcePrecheckCommand command,
            List<MesProEdhrBatchExecutionOriginDO> origins,
            List<MesProEdhrBatchExecutionTraceLinkDO> links,
            LocalDateTime readAt) {
        if (command == null || command.getBatchExecutionId() == null || origins == null || links == null) {
            throw exception(FLOW8_SOURCE_PRECHECK_REQUIRED);
        }
        List<MesProEdhrBatchExecutionTraceLinkDO> candidates = links.stream()
                .filter(Objects::nonNull)
                .filter(link -> Objects.equals(command.getBatchExecutionId(), link.getBatchExecutionId()))
                .filter(link -> MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT.equals(link.getLinkType()))
                .filter(link -> "CAPTURED".equalsIgnoreCase(link.getRelationStatus()))
                .filter(MesProEdhrBatchTraceabilityServiceImpl::isTraceLinkIntegrityValid)
                .filter(link -> origins.stream().anyMatch(origin ->
                        Objects.equals(origin.getId(), link.getOriginId())
                                && Objects.equals(origin.getBatchExecutionId(), link.getBatchExecutionId())
                                && !isBlank(origin.getSourceSnapshotHash())
                                && !Objects.equals("NOT_APPLICABLE", link.getRelationStatus())))
                .toList();
        if (candidates.size() != 1) {
            throw exception(FLOW8_SOURCE_PRECHECK_REQUIRED);
        }
        MesProEdhrBatchExecutionTraceLinkDO link = candidates.get(0);
        MesProEdhrBatchExecutionOriginDO origin = origins.stream()
                .filter(candidate -> Objects.equals(candidate.getId(), link.getOriginId()))
                .findFirst().orElse(null);
        return resolveSourcePrecheck(command.setOriginLinkId(link.getId()), origin, link, readAt);
    }

    static MesProEdhrBatchTraceSourcePrecheckRespVO resolveSourcePrecheck(
            MesProEdhrBatchTraceSourcePrecheckCommand command,
            MesProEdhrBatchExecutionOriginDO origin,
            MesProEdhrBatchExecutionTraceLinkDO link,
            LocalDateTime readAt) {
        if (command == null || command.getBatchExecutionId() == null || command.getOriginLinkId() == null
                || origin == null || link == null
                || !Objects.equals(command.getBatchExecutionId(), link.getBatchExecutionId())
                || !Objects.equals(command.getOriginLinkId(), link.getId())
                || !Objects.equals(command.getBatchExecutionId(), origin.getBatchExecutionId())
                || !Objects.equals(origin.getId(), link.getOriginId())) {
            throw exception(FLOW8_TRACE_LINK_ORIGIN_MISMATCH);
        }
        if (isBlank(origin.getSourceSnapshotHash()) || !isTraceLinkIntegrityValid(link)
                || "NOT_APPLICABLE".equalsIgnoreCase(link.getRelationStatus())) {
            throw exception(FLOW8_SOURCE_PRECHECK_REQUIRED);
        }
        boolean hasExpectedWitness = !isBlank(command.getExpectedTraceLinkHash())
                || !isBlank(command.getExpectedSourceSnapshotHash())
                || command.getExpectedSourceVersion() != null;
        if (hasExpectedWitness && (isBlank(command.getExpectedTraceLinkHash())
                || isBlank(command.getExpectedSourceSnapshotHash())
                || (link.getSourceVersion() != null && command.getExpectedSourceVersion() == null))) {
            throw exception(FLOW8_SOURCE_PRECHECK_REQUIRED);
        }
        boolean versionChanged = link.getSourceVersion() != null
                && !Objects.equals(link.getSourceVersion(), command.getExpectedSourceVersion());
        if (hasExpectedWitness && (!Objects.equals(link.getSnapshotHash(), command.getExpectedTraceLinkHash())
                || !Objects.equals(origin.getSourceSnapshotHash(), command.getExpectedSourceSnapshotHash())
                || versionChanged)) {
            throw exception(FLOW8_SOURCE_PRECHECK_STALE);
        }
        return new MesProEdhrBatchTraceSourcePrecheckRespVO()
                .setBatchExecutionId(link.getBatchExecutionId())
                .setOriginLinkId(link.getId())
                .setTraceLinkHash(link.getSnapshotHash())
                .setSourceSnapshotHash(origin.getSourceSnapshotHash())
                .setSourceVersion(link.getSourceVersion())
                .setRelationStatus(link.getRelationStatus())
                .setReadAt(readAt == null ? LocalDateTime.now() : readAt);
    }

    private void requireBatch(Long batchExecutionId) {
        if (batchExecutionId == null || batchExecutionMapper.selectById(batchExecutionId) == null) {
            throw exception(BATCH_NOT_EXISTS);
        }
    }

    static Long resolveReleaseOriginId(List<MesProEdhrBatchExecutionOriginDO> origins, Long originId) {
        if (origins == null || originId == null) {
            return null;
        }
        return origins.stream().map(MesProEdhrBatchExecutionOriginDO::getId)
                .filter(originId::equals).findFirst().orElse(null);
    }

    static List<Long> batchIdsForReleaseApplication(List<MesProEdhrBatchExecutionTraceLinkDO> links,
                                                     Long releaseApplicationId) {
        if (links == null || releaseApplicationId == null) {
            return List.of();
        }
        return links.stream()
                .filter(link -> MesProEdhrBatchTraceLinkType.RELEASE_DECISION.equals(link.getLinkType()))
                .filter(link -> "RELEASE_APPLICATION".equals(link.getSourceObjectType()))
                .filter(link -> releaseApplicationId.equals(link.getSourceObjectId()))
                .map(MesProEdhrBatchExecutionTraceLinkDO::getBatchExecutionId).distinct().toList();
    }

    static boolean isTraceCaptured(List<MesProEdhrBatchExecutionOriginDO> origins,
                                   List<MesProEdhrBatchExecutionTraceLinkDO> links,
                                   List<MesProEdhrBatchExecutionTraceManifestDO> manifests) {
        if (origins == null || origins.isEmpty() || links == null || links.isEmpty()
                || !isManifestHistoryValid(manifests)) {
            return false;
        }
        Set<Long> originIds = origins.stream().map(MesProEdhrBatchExecutionOriginDO::getId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (originIds.size() != origins.size()
                || origins.stream().anyMatch(origin -> origin.getBatchExecutionId() == null)) {
            return false;
        }
        Map<Long, Long> batchIdByOrigin = origins.stream().collect(java.util.stream.Collectors.toMap(
                MesProEdhrBatchExecutionOriginDO::getId,
                MesProEdhrBatchExecutionOriginDO::getBatchExecutionId));
        Map<Long, Set<String>> linkTypesByOrigin = new LinkedHashMap<>();
        for (MesProEdhrBatchExecutionTraceLinkDO link : links) {
            if (link == null || link.getOriginId() == null || !originIds.contains(link.getOriginId())
                    || isBlank(link.getLinkType())
                    || !Objects.equals(batchIdByOrigin.get(link.getOriginId()), link.getBatchExecutionId())
                    || !isTraceLinkIntegrityValid(link)) {
                return false;
            }
            Set<String> linkTypes = linkTypesByOrigin.computeIfAbsent(link.getOriginId(), ignored -> new HashSet<>());
            if (!linkTypes.add(link.getLinkType())) {
                return false;
            }
        }
        for (MesProEdhrBatchExecutionOriginDO origin : origins) {
            Set<String> linkTypes = linkTypesByOrigin.get(origin.getId());
            if (linkTypes == null || !linkTypes.containsAll(
                    MesProEdhrBatchTraceabilityValidator.requiredLinkTypesFor(origin.getEntryType()))) {
                return false;
            }
            if (!isLossRelationConsistent(origin, links)) {
                return false;
            }
        }
        return true;
    }

    static boolean isLossRelationConsistent(MesProEdhrBatchExecutionOriginDO origin,
                                            List<MesProEdhrBatchExecutionTraceLinkDO> links) {
        if (origin == null || !MesProEdhrBatchTraceFormalSourceResolver.isActiveOrderEntryType(origin.getEntryType())) {
            return true;
        }
        List<MesProEdhrBatchExecutionTraceLinkDO> originLinks = links == null ? List.of() : links.stream()
                .filter(Objects::nonNull).filter(link -> Objects.equals(origin.getId(), link.getOriginId())).toList();
        Set<String> linkTypes = originLinks.stream().map(MesProEdhrBatchExecutionTraceLinkDO::getLinkType)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        long lossFactCount = originLinks.stream()
                .filter(link -> MesProEdhrBatchTraceLinkType.LOSS_FACT.equals(link.getLinkType()))
                .count();
        long lossReportCount = originLinks.stream()
                .filter(link -> MesProEdhrBatchTraceLinkType.LOSS_REPORT_RECEIPT.equals(link.getLinkType()))
                .count();
        long noLossCount = originLinks.stream()
                .filter(link -> MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED.equals(link.getLinkType()))
                .count();
        boolean hasLossFact = linkTypes.contains(MesProEdhrBatchTraceLinkType.LOSS_FACT);
        boolean hasLossReport = linkTypes.contains(MesProEdhrBatchTraceLinkType.LOSS_REPORT_RECEIPT);
        boolean hasNoLossFact = linkTypes.contains(MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED);
        if (Boolean.TRUE.equals(origin.getHasActualLoss())) {
            return lossFactCount == 1 && lossReportCount == 1 && noLossCount == 0
                    && hasLossFact && hasLossReport && !hasNoLossFact
                    && originLinks.stream().filter(link -> MesProEdhrBatchTraceLinkType.LOSS_FACT.equals(link.getLinkType()))
                    .anyMatch(link -> "HAS_LOSS".equals(link.getRelationStatus()));
        }
        if (Boolean.FALSE.equals(origin.getHasActualLoss())) {
            return noLossCount == 1 && lossFactCount == 0 && lossReportCount == 0
                    && hasNoLossFact && !hasLossFact && !hasLossReport
                    && originLinks.stream().filter(link -> MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED.equals(link.getLinkType()))
                    .anyMatch(link -> "NO_LOSS".equals(link.getRelationStatus())
                            || "NO_LOSS_CONFIRMED".equals(link.getRelationStatus()));
        }
        return false;
    }

    static boolean isManifestHistoryValid(List<MesProEdhrBatchExecutionTraceManifestDO> manifests) {
        if (manifests == null || manifests.isEmpty()) {
            return false;
        }
        List<MesProEdhrBatchExecutionTraceManifestDO> ordered = manifests.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MesProEdhrBatchExecutionTraceManifestDO::getManifestVersion,
                        Comparator.nullsFirst(Integer::compareTo)))
                .toList();
        if (ordered.size() != manifests.size()) {
            return false;
        }
        Long batchExecutionId = ordered.get(0).getBatchExecutionId();
        String previousHash = null;
        int expectedVersion = 1;
        for (MesProEdhrBatchExecutionTraceManifestDO manifest : ordered) {
            if (!Objects.equals(batchExecutionId, manifest.getBatchExecutionId())
                    || manifest.getManifestVersion() == null
                    || manifest.getManifestVersion() != expectedVersion
                    || isBlank(manifest.getManifestJson())
                    || isBlank(manifest.getManifestHash())) {
                return false;
            }
            String expectedHash = DigestUtil.sha256Hex(
                    MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(manifest.getManifestJson()));
            if (!expectedHash.equalsIgnoreCase(manifest.getManifestHash())
                    || !Objects.equals(previousHash, manifest.getPreviousManifestHash())) {
                return false;
            }
            previousHash = manifest.getManifestHash();
            expectedVersion++;
        }
        return true;
    }

    private void insertLinks(Long batchExecutionId, Long originId, List<MesProEdhrBatchTraceSource> sources,
                             Long capturedBy, LocalDateTime capturedAt) {
        for (MesProEdhrBatchTraceSource source : sources) {
            String identity = identityOf(source);
            MesProEdhrBatchExecutionTraceLinkDO existing = traceLinkMapper.selectByIdentityKey(batchExecutionId, identity);
            if (existing != null) {
                if (isIdempotentTraceLink(existing, originId, source.getSnapshotHash())) {
                    continue;
                }
                throw exception(TRACE_SOURCE_CONFLICT);
            }
            MesProEdhrBatchExecutionTraceLinkDO link = MesProEdhrBatchExecutionTraceLinkDO.builder()
                    .tenantId(TenantContextHolder.getRequiredTenantId()).batchExecutionId(batchExecutionId).originId(originId)
                    .linkType(source.getLinkType()).sourceObjectType(source.getSourceObjectType())
                    .sourceObjectId(source.getSourceObjectId()).sourceLineId(source.getSourceLineId())
                    .sourceEventId(source.getSourceEventId()).sourceVersion(source.getSourceVersion())
                    .sourceIdentityKey(identity).snapshotJson(source.getSnapshotJson()).snapshotHash(source.getSnapshotHash())
                    .relationStatus(source.getRelationStatus() == null ? "CAPTURED" : source.getRelationStatus())
                    .relationReason(source.getRelationReason()).capturedBy(capturedBy).capturedAt(capturedAt).build();
            if (traceLinkMapper.insert(link) != 1) {
                throw exception(TRACE_PERSIST_FAILED);
            }
        }
    }

    static boolean isIdempotentTraceLink(MesProEdhrBatchExecutionTraceLinkDO existing, Long originId,
                                         String snapshotHash) {
        return existing != null && Objects.equals(existing.getOriginId(), originId)
                && Objects.equals(existing.getSnapshotHash(), snapshotHash);
    }

    private void insertNotApplicableLinks(Long batchExecutionId, Long originId, String entryType,
                                          Long capturedBy, LocalDateTime capturedAt) {
        String snapshotJson = JsonUtils.toJsonString(Map.of(
                "entryType", entryType,
                "relationStatus", "NOT_APPLICABLE",
                "reasonCode", "ENTRY_TYPE_NOT_APPLICABLE"));
        String snapshotHash = canonicalHash(snapshotJson);
        insertNotApplicableLink(batchExecutionId, originId, MesProEdhrBatchTraceLinkType.ACTIVE_ORDER,
                "ACTIVE_ORDER", identityOf(MesProEdhrBatchTraceLinkType.ACTIVE_ORDER, "ACTIVE_ORDER", null,
                        null, originId), snapshotJson, snapshotHash, capturedBy, capturedAt);
        insertNotApplicableLink(batchExecutionId, originId, MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE,
                "MATERIAL_ISSUE", identityOf(MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE, "MATERIAL_ISSUE", null,
                        null, originId), snapshotJson, snapshotHash, capturedBy, capturedAt);
        insertNotApplicableLink(batchExecutionId, originId, MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT,
                "COMPLETION_BACKFILL_RECEIPT", identityOf(MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT,
                        "COMPLETION_BACKFILL_RECEIPT", null, null, originId), snapshotJson, snapshotHash,
                capturedBy, capturedAt);
    }

    private void insertNotApplicableLink(Long batchExecutionId, Long originId, String linkType,
                                         String sourceObjectType, String identity, String snapshotJson,
                                         String snapshotHash, Long capturedBy, LocalDateTime capturedAt) {
        if (traceLinkMapper.selectByIdentityKey(batchExecutionId, identity) != null) {
            return;
        }
        MesProEdhrBatchExecutionTraceLinkDO link = MesProEdhrBatchExecutionTraceLinkDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId()).batchExecutionId(batchExecutionId).originId(originId)
                .linkType(linkType).sourceObjectType(sourceObjectType).sourceIdentityKey(identity)
                .sourceEventId(originId)
                .snapshotJson(snapshotJson).snapshotHash(snapshotHash).relationStatus("NOT_APPLICABLE")
                .relationReason("ENTRY_TYPE_NOT_APPLICABLE").capturedBy(capturedBy).capturedAt(capturedAt).build();
        if (traceLinkMapper.insert(link) != 1) {
            throw exception(TRACE_PERSIST_FAILED);
        }
    }

    private void appendManifest(Long batchExecutionId, Long sealedBy, String sealReason) {
        List<MesProEdhrBatchExecutionOriginDO> origins = originMapper.selectListByBatchExecutionId(batchExecutionId);
        List<MesProEdhrBatchExecutionTraceLinkDO> links = traceLinkMapper.selectListByBatchExecutionId(batchExecutionId);
        MesProEdhrBatchExecutionTraceManifestDO previous = manifestMapper.selectLatestByBatchExecutionId(batchExecutionId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("batchExecutionId", batchExecutionId);
        payload.put("origins", origins.stream().sorted(Comparator.comparing(MesProEdhrBatchExecutionOriginDO::getId)).map(this::originMap).toList());
        payload.put("traceLinks", links.stream().sorted(Comparator.comparing(MesProEdhrBatchExecutionTraceLinkDO::getId)).map(this::traceLinkMap).toList());
        String manifestJson = JsonUtils.toJsonString(payload);
        MesProEdhrBatchExecutionTraceManifestDO manifest = MesProEdhrBatchExecutionTraceManifestDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId()).batchExecutionId(batchExecutionId)
                .manifestVersion(previous == null ? 1 : previous.getManifestVersion() + 1)
                .previousManifestHash(previous == null ? null : previous.getManifestHash()).manifestJson(manifestJson)
                .manifestHash(canonicalHash(manifestJson)).sealReason(sealReason).sealedBy(sealedBy)
                .sealedAt(LocalDateTime.now()).build();
        if (manifestMapper.insert(manifest) != 1) {
            throw exception(TRACE_PERSIST_FAILED);
        }
    }

    private String identityOf(MesProEdhrBatchTraceSource source) {
        return identityOf(source.getLinkType(), source.getSourceObjectType(), source.getSourceObjectId(),
                source.getSourceLineId(), source.getSourceEventId());
    }

    private static String identityOf(String linkType, String sourceObjectType, Long sourceObjectId,
                                     Long sourceLineId, Long sourceEventId) {
        return String.join(":", Objects.toString(linkType, ""), Objects.toString(sourceObjectType, ""),
                Objects.toString(sourceObjectId, ""), Objects.toString(sourceLineId, ""),
                Objects.toString(sourceEventId, ""));
    }

    private static boolean isTraceLinkIntegrityValid(MesProEdhrBatchExecutionTraceLinkDO link) {
        if (isBlank(link.getSourceObjectType())
                || (link.getSourceObjectId() == null && link.getSourceLineId() == null
                && link.getSourceEventId() == null)
                || isBlank(link.getSourceIdentityKey()) || isBlank(link.getSnapshotJson())
                || isBlank(link.getSnapshotHash())) {
            return false;
        }
        String expectedIdentity = identityOf(link.getLinkType(), link.getSourceObjectType(),
                link.getSourceObjectId(), link.getSourceLineId(), link.getSourceEventId());
        if (!expectedIdentity.equals(link.getSourceIdentityKey())) {
            return false;
        }
        return MesProEdhrBatchTraceSourceHash.isValid(
                link.getLinkType(), link.getSnapshotJson(), link.getSnapshotHash());
    }

    private String canonicalHash(String json) {
        return DigestUtil.sha256Hex(MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(json));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private MesProEdhrBatchTraceabilityRespVO.Origin toOrigin(MesProEdhrBatchExecutionOriginDO source) {
        return new MesProEdhrBatchTraceabilityRespVO.Origin().setId(source.getId()).setBatchExecutionId(source.getBatchExecutionId())
                .setEntryType(source.getEntryType()).setOriginKey(source.getOriginKey()).setActiveOrderId(source.getActiveOrderId())
                .setWorkOrderId(source.getWorkOrderId()).setCompletionTransactionId(source.getCompletionTransactionId())
                .setCompletionVersion(source.getCompletionVersion()).setCompletionBackfillReceiptId(source.getCompletionBackfillReceiptId())
                .setCompletionBackfillReceiptHash(source.getCompletionBackfillReceiptHash()).setPickListBindingId(source.getPickListBindingId())
                .setPickListId(source.getPickListId()).setPickListBindingVersion(source.getPickListBindingVersion())
                .setHasActualLoss(source.getHasActualLoss())
                .setSourceSnapshotHash(source.getSourceSnapshotHash()).setBatchProvisionReceiptId(source.getBatchProvisionReceiptId())
                .setBatchProvisionStatus(source.getBatchProvisionStatus()).setSourceCredentialId(source.getSourceCredentialId())
                .setSourceCredentialHash(source.getSourceCredentialHash())
                .setSourceBundleHash(source.getSourceBundleHash()).setIdempotencyKey(source.getIdempotencyKey())
                .setRelationStatus(source.getRelationStatus()).setRelationReason(source.getRelationReason())
                .setCapturedBy(source.getCapturedBy()).setCapturedAt(source.getCapturedAt());
    }

    private MesProEdhrBatchTraceabilityRespVO.TraceLink toTraceLink(MesProEdhrBatchExecutionTraceLinkDO source) {
        return new MesProEdhrBatchTraceabilityRespVO.TraceLink().setId(source.getId()).setOriginId(source.getOriginId())
                .setBatchExecutionId(source.getBatchExecutionId()).setLinkType(source.getLinkType())
                .setSourceObjectType(source.getSourceObjectType()).setSourceObjectId(source.getSourceObjectId())
                .setSourceLineId(source.getSourceLineId()).setSourceEventId(source.getSourceEventId())
                .setSourceVersion(source.getSourceVersion()).setSourceIdentityKey(source.getSourceIdentityKey())
                .setIdempotencyKey(source.getIdempotencyKey())
                .setSnapshotJson(source.getSnapshotJson()).setSnapshotHash(source.getSnapshotHash())
                .setRelationStatus(source.getRelationStatus()).setRelationReason(source.getRelationReason())
                .setCapturedBy(source.getCapturedBy()).setCapturedAt(source.getCapturedAt());
    }

    private MesProEdhrBatchTraceabilityRespVO.Manifest toManifest(MesProEdhrBatchExecutionTraceManifestDO source) {
        return new MesProEdhrBatchTraceabilityRespVO.Manifest().setId(source.getId()).setBatchExecutionId(source.getBatchExecutionId())
                .setManifestVersion(source.getManifestVersion()).setPreviousManifestHash(source.getPreviousManifestHash())
                .setManifestJson(source.getManifestJson()).setManifestHash(source.getManifestHash())
                .setSealReason(source.getSealReason()).setSealedBy(source.getSealedBy()).setSealedAt(source.getSealedAt());
    }

    private Map<String, Object> originMap(MesProEdhrBatchExecutionOriginDO source) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", source.getId()); map.put("entryType", source.getEntryType()); map.put("originKey", source.getOriginKey());
        map.put("activeOrderId", source.getActiveOrderId()); map.put("workOrderId", source.getWorkOrderId());
        map.put("completionTransactionId", source.getCompletionTransactionId());
        map.put("completionVersion", source.getCompletionVersion());
        map.put("completionBackfillReceiptId", source.getCompletionBackfillReceiptId());
        map.put("completionBackfillReceiptHash", source.getCompletionBackfillReceiptHash());
        map.put("pickListBindingId", source.getPickListBindingId()); map.put("pickListId", source.getPickListId());
        map.put("pickListBindingVersion", source.getPickListBindingVersion());
        map.put("hasActualLoss", source.getHasActualLoss());
        map.put("sourceSnapshotHash", source.getSourceSnapshotHash()); map.put("batchProvisionReceiptId", source.getBatchProvisionReceiptId());
        map.put("batchProvisionStatus", source.getBatchProvisionStatus()); map.put("sourceCredentialId", source.getSourceCredentialId());
        map.put("sourceCredentialHash", source.getSourceCredentialHash());
        map.put("sourceBundleHash", source.getSourceBundleHash()); map.put("idempotencyKey", source.getIdempotencyKey());
        map.put("relationStatus", source.getRelationStatus()); map.put("relationReason", source.getRelationReason());
        map.put("capturedBy", source.getCapturedBy()); map.put("capturedAt", source.getCapturedAt());
        return map;
    }

    private Map<String, Object> traceLinkMap(MesProEdhrBatchExecutionTraceLinkDO source) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", source.getId()); map.put("originId", source.getOriginId()); map.put("linkType", source.getLinkType());
        map.put("sourceObjectType", source.getSourceObjectType()); map.put("sourceObjectId", source.getSourceObjectId());
        map.put("sourceLineId", source.getSourceLineId()); map.put("sourceEventId", source.getSourceEventId());
        map.put("sourceVersion", source.getSourceVersion()); map.put("sourceIdentityKey", source.getSourceIdentityKey());
        map.put("idempotencyKey", source.getIdempotencyKey());
        map.put("snapshotJson", source.getSnapshotJson()); map.put("snapshotHash", source.getSnapshotHash());
        map.put("relationStatus", source.getRelationStatus()); map.put("relationReason", source.getRelationReason());
        map.put("capturedBy", source.getCapturedBy()); map.put("capturedAt", source.getCapturedAt());
        return map;
    }
}
