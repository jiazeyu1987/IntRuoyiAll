package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetDraftCommand;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetFiles;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetKey;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetVersion;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterDetailRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterHistoryRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterRepublishReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterRepublishRespVO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.asset.ShowroomPreviewAssetVersionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDocumentDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleasePointerDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseSourceSnapshotDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.version.ShowroomVersionBundleDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomCompanyMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomCompanyRevisionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseDocumentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleasePointerMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseSourceSnapshotMapper;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldDisplaySupport;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudioDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationOperations;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ShowroomVersionCenterService {

    private static final Logger log = LoggerFactory.getLogger(ShowroomVersionCenterService.class);

    private static final List<String> COMPANY_FIELD_ORDER = List.of(
            "development_history",
            "park_introduction",
            "incubation_platform",
            "subsidiary_overview",
            "stock_info",
            "core_manufacturing_capability",
            "honors_awards"
    );
    private static final List<String> PRODUCT_FIELD_ORDER = List.of(
            "owner_company_id",
            "product_owner_type",
            "lifecycle_stage",
            "target_market",
            "pipeline_layout",
            "indication_content",
            "core_selling_points",
            "model_specification",
            "registration_certificate",
            "clinical_effect",
            "fim_status",
            "cover_image"
    );

    private final ShowroomVersionBundleService bundleService;
    private final ShowroomVersionCenterAssembler assembler;
    private final ShowroomPersistentContentService contentService;
    private final ShowroomCompanyMapper companyMapper;
    private final ShowroomCompanyRevisionMapper companyRevisionMapper;
    private final ShowroomProductMapper productMapper;
    private final ShowroomProductRevisionMapper productRevisionMapper;
    private final ShowroomNarrationOperations narrationService;
    private final ShowroomPreviewAssetOperations previewAssetService;
    private final ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;
    private final FileMapper fileMapper;
    private final ShowroomReleasePublisherService releasePublisherService;
    private final ShowroomReleaseAutoPublishService releaseAutoPublishService;
    private final ShowroomReleaseAssembler releaseAssembler;
    private final ShowroomReleasePointerMapper releasePointerMapper;
    private final ShowroomReleaseMapper releaseMapper;
    private final ShowroomReleaseSourceSnapshotMapper releaseSourceSnapshotMapper;
    private final ShowroomReleaseDocumentMapper releaseDocumentMapper;
    private final ShowroomPublicReleaseScopeResolver scopeResolver;

    public ShowroomVersionCenterService(ShowroomVersionBundleService bundleService,
                                        ShowroomVersionCenterAssembler assembler,
                                        ShowroomPersistentContentService contentService,
                                        ShowroomCompanyMapper companyMapper,
                                        ShowroomCompanyRevisionMapper companyRevisionMapper,
                                        ShowroomProductMapper productMapper,
                                        ShowroomProductRevisionMapper productRevisionMapper,
                                        ShowroomNarrationOperations narrationService,
                                        ShowroomPreviewAssetOperations previewAssetService,
                                        ShowroomPreviewAssetVersionMapper previewAssetVersionMapper,
                                        FileMapper fileMapper,
                                        ShowroomReleasePublisherService releasePublisherService,
                                        ObjectProvider<ShowroomReleaseAutoPublishService> releaseAutoPublishServiceProvider,
                                        ShowroomReleaseAssembler releaseAssembler,
                                        ShowroomReleasePointerMapper releasePointerMapper,
                                        ShowroomReleaseMapper releaseMapper,
                                        ShowroomReleaseSourceSnapshotMapper releaseSourceSnapshotMapper,
                                        ShowroomReleaseDocumentMapper releaseDocumentMapper,
                                        ShowroomPublicReleaseScopeResolver scopeResolver) {
        this.bundleService = bundleService;
        this.assembler = assembler;
        this.contentService = contentService;
        this.companyMapper = companyMapper;
        this.companyRevisionMapper = companyRevisionMapper;
        this.productMapper = productMapper;
        this.productRevisionMapper = productRevisionMapper;
        this.narrationService = narrationService;
        this.previewAssetService = previewAssetService;
        this.previewAssetVersionMapper = previewAssetVersionMapper;
        this.fileMapper = fileMapper;
        this.releasePublisherService = releasePublisherService;
        this.releaseAutoPublishService = releaseAutoPublishServiceProvider == null ? null
                : releaseAutoPublishServiceProvider.getIfAvailable();
        this.releaseAssembler = releaseAssembler;
        this.releasePointerMapper = releasePointerMapper;
        this.releaseMapper = releaseMapper;
        this.releaseSourceSnapshotMapper = releaseSourceSnapshotMapper;
        this.releaseDocumentMapper = releaseDocumentMapper;
        this.scopeResolver = scopeResolver;
    }

    public ShowroomVersionCenterHistoryRespVO getHistory(String targetType, Long targetId, String siteKey,
                                                         String stage) {
        ShowroomReleaseScope scope = scopeResolver.resolve(siteKey, stage);
        String normalizedTargetType = bundleService.normalizeTargetType(targetType);
        requirePositiveId(targetId, "SHOWROOM_TARGET_NOT_FOUND: target id is required");
        List<ShowroomVersionBundleDO> bundles = bundleService.listBundles(normalizedTargetType, targetId);
        ReleaseState releaseState = resolveCurrentReleaseState(scope);
        Long currentContentRevisionId = resolveCurrentContentRevisionId(normalizedTargetType, targetId);
        Long currentPublicRevisionId = resolveCurrentPublicRevisionId(normalizedTargetType, targetId, releaseState);
        assertCurrentHistoryAnchorsReadable(normalizedTargetType, targetId, bundles,
                currentContentRevisionId, currentPublicRevisionId, releaseState);
        List<ShowroomVersionCenterHistoryRespVO.HistoryItemRespVO> items = new ArrayList<>();
        for (ShowroomVersionBundleDO bundle : bundles) {
            List<ShowroomVersionBundleService.ShowroomVersionBlocker> bundleBlockers =
                    bundleService.diagnoseBundleIntegrity(normalizedTargetType, targetId, bundle);
            if (bundleBlockers.stream().anyMatch(this::isCoreHistoryBlocker)) {
                throw historyBundleIntegrityException(bundleBlockers);
            }
            items.add(new ShowroomVersionCenterHistoryRespVO.HistoryItemRespVO(
                    bundle.getRevisionId(),
                    bundle.getRevisionNo(),
                    toInstantString(bundle.getPublishedAt()),
                    bundle.getPublishedBy(),
                    bundle.getCopiedFromRevisionId(),
                    bundle.getRevisionId().equals(currentContentRevisionId),
                    bundle.getRevisionId().equals(currentPublicRevisionId),
                    true,
                    resolveSummaryImageUrl(normalizedTargetType, bundle),
                    List.of(),
                    bundleBlockers.stream().map(assembler::blocker).toList()
            ));
        }
        return assembler.history(normalizedTargetType, targetId, currentContentRevisionId, currentPublicRevisionId,
                releaseState == null ? null : releaseState.releaseId(), items);
    }

    public ShowroomVersionCenterDetailRespVO getDetail(String targetType, Long targetId, Long revisionId,
                                                       String siteKey, String stage) {
        ShowroomReleaseScope scope = scopeResolver.resolve(siteKey, stage);
        String normalizedTargetType = bundleService.normalizeTargetType(targetType);
        requirePositiveId(targetId, "SHOWROOM_TARGET_NOT_FOUND: target id is required");
        requirePositiveId(revisionId, "SHOWROOM_TARGET_NOT_FOUND: revision id is required");
        ShowroomVersionBundleDO selectedBundle = bundleService.requireBundle(normalizedTargetType, targetId, revisionId);
        ReleaseState releaseState = null;
        List<ShowroomVersionBundleService.ShowroomVersionBlocker> blockers = new ArrayList<>();
        blockers.addAll(bundleService.diagnoseBundleIntegrity(normalizedTargetType, targetId, selectedBundle));
        try {
            releaseState = resolveCurrentReleaseState(scope);
        } catch (IllegalStateException exception) {
            blockers.add(new ShowroomVersionBundleService.ShowroomVersionBlocker(
                    "SHOWROOM_VERSION_REPUBLISH_PUBLIC_RELEASE_BLOCKED",
                    exception.getMessage(),
                    List.of(revisionId),
                    "PUBLIC_RELEASE"));
        }
        appendBundleSpecificBlockers(blockers, normalizedTargetType, targetId, revisionId, selectedBundle);
        Long currentContentRevisionId = resolveCurrentContentRevisionId(normalizedTargetType, targetId);
        Long currentPublicRevisionId = resolveCurrentPublicRevisionIdForDetail(normalizedTargetType, targetId, releaseState);
        ShowroomVersionCenterDetailRespVO.SnapshotRespVO selectedVersion =
                buildSnapshot(normalizedTargetType, targetId, selectedBundle, currentContentRevisionId, currentPublicRevisionId);
        ShowroomVersionCenterDetailRespVO.SnapshotRespVO currentContentVersion =
                currentContentRevisionId == null ? null : findSnapshot(normalizedTargetType, targetId, currentContentRevisionId,
                        currentContentRevisionId, currentPublicRevisionId, blockers, "CURRENT_CONTENT");
        ShowroomVersionCenterDetailRespVO.SnapshotRespVO currentPublicVersion =
                currentPublicRevisionId == null ? null : findSnapshot(normalizedTargetType, targetId, currentPublicRevisionId,
                        currentContentRevisionId, currentPublicRevisionId, blockers, "CURRENT_RELEASE");
        List<ShowroomVersionCenterDetailRespVO.FieldDiffRespVO> fieldDiffs = buildFieldDiffs(selectedVersion, currentContentVersion);
        List<ShowroomVersionCenterDetailRespVO.BlockerRespVO> blockerResp = blockers.stream().map(assembler::blocker).toList();
        for (ShowroomVersionBundleService.ShowroomVersionBlocker blocker : blockers) {
            log.warn("SHOWROOM_VERSION_CENTER_BLOCKER targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={} blockerScope={} blockerCode={}",
                    normalizedTargetType, targetId, null, revisionId, currentContentRevisionId, currentPublicRevisionId,
                    releaseState == null ? null : releaseState.releaseId(), null, null, blocker.scope(), blocker.blockerCode());
        }
        String disabledReason = blockerResp.isEmpty() ? null : blockerResp.get(0).message();
        return assembler.detail(
                new ShowroomVersionCenterDetailRespVO.TargetSummaryRespVO(
                        normalizedTargetType,
                        targetId,
                        selectedVersion.title(),
                        selectedVersion.titleEn(),
                        currentContentRevisionId,
                        currentPublicRevisionId
                ),
                selectedVersion,
                currentContentVersion,
                currentPublicVersion,
                releaseState == null ? null : new ShowroomVersionCenterDetailRespVO.ReleaseSummaryRespVO(
                        releaseState.releaseId(),
                        releaseState.manifestHash(),
                        releaseState.publishedAt().toString(),
                        releaseState.companyRevisionId(),
                        ShowroomVersionBundleService.TARGET_PRODUCT.equals(normalizedTargetType)
                                ? releaseState.productRevisionByProductId().containsKey(targetId)
                                : null,
                        ShowroomVersionBundleService.TARGET_PRODUCT.equals(normalizedTargetType)
                                ? releaseState.productRevisionByProductId().get(targetId)
                                : null
                ),
                fieldDiffs,
                new ShowroomVersionCenterDetailRespVO.PermissionRespVO(blockerResp.isEmpty(), disabledReason),
                new ShowroomVersionCenterDetailRespVO.RepublishReadinessRespVO(blockerResp.isEmpty(), blockerResp)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomVersionCenterRepublishRespVO republish(ShowroomVersionCenterRepublishReqVO req, Long operatorUserId) {
        String normalizedTargetType = bundleService.normalizeTargetType(req.targetType());
        Long targetId = requirePositiveId(req.targetId(), "SHOWROOM_TARGET_NOT_FOUND: target id is required");
        Long sourceRevisionId = requirePositiveId(req.sourceRevisionId(),
                "SHOWROOM_TARGET_NOT_FOUND: source revision id is required");
        ShowroomReleaseScope scope = scopeResolver.resolve(req.siteKey(), req.stage());
        return scopeResolver.executeInTenant(scope, () -> {
            ShowroomVersionBundleDO sourceBundle = bundleService.requireBundle(normalizedTargetType, targetId, sourceRevisionId);
            if (ShowroomVersionBundleService.TARGET_COMPANY.equals(normalizedTargetType)) {
                return republishCompany(targetId, sourceRevisionId, sourceBundle, operatorUserId, scope);
            }
            return republishProduct(targetId, sourceRevisionId, sourceBundle, operatorUserId, scope);
        });
    }

    private ShowroomVersionCenterRepublishRespVO republishProduct(Long productId,
                                                                  Long sourceRevisionId,
                                                                  ShowroomVersionBundleDO sourceBundle,
                                                                  Long operatorUserId,
                                                                  ShowroomReleaseScope scope) {
        ShowroomProductDO product = requireProduct(productId);
        ShowroomProductRevisionDO sourceRevision = requireProductRevision(sourceRevisionId);
        if (!productId.equals(sourceRevision.getProductId())) {
            throw new IllegalStateException("SHOWROOM_VERSION_REPUBLISH_SOURCE_MISMATCH: product revision mismatch");
        }
        ShowroomProductRevision draft = contentService.saveProductDraft(new ShowroomProductDraft(
                productId,
                product.getProductCode(),
                sourceRevision.getNameCn(),
                sourceRevision.getNameEn(),
                productFields(sourceRevision)));
        copyNarrationPair(sourceBundle, draft.revisionId(), ShowroomNarrationTargetType.PRODUCT, productId);
        ShowroomProductRevision published = contentService.publishProductRevision(draft.revisionId(), operatorUserId);
        ShowroomVersionBundleDO newBundle = buildBundleForPublishedProduct(sourceBundle, published, productId, operatorUserId);
        bundleService.insertBundle(newBundle);
        assertScopedReleasePublishable(ShowroomVersionBundleService.TARGET_PRODUCT, productId, published.revisionId());
        try {
            ShowroomMaterializedRelease release = releaseAutoPublishService == null
                    ? releasePublisherService.publishRelease(operatorUserId, Instant.now(), scope.siteKey(), scope.stage())
                    : releaseAutoPublishService.publishNow(operatorUserId, Instant.now(), scope.siteKey(), scope.stage());
            return assembler.republish(ShowroomVersionBundleService.TARGET_PRODUCT, productId, sourceRevisionId,
                    published.revisionId(), published.revisionNo(), release);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("SHOWROOM_VERSION_REPUBLISH_RELEASE_FAILED: " + exception.getMessage(), exception);
        }
    }

    private ShowroomVersionCenterRepublishRespVO republishCompany(Long companyId,
                                                                  Long sourceRevisionId,
                                                                  ShowroomVersionBundleDO sourceBundle,
                                                                  Long operatorUserId,
                                                                  ShowroomReleaseScope scope) {
        ShowroomCompanyRevisionDO sourceRevision = requireCompanyRevision(sourceRevisionId);
        if (!companyId.equals(sourceRevision.getCompanyId())) {
            throw new IllegalStateException("SHOWROOM_VERSION_REPUBLISH_SOURCE_MISMATCH: company revision mismatch");
        }
        if (!hasText(sourceRevision.getDisplayNameSnapshot())
                || !hasText(sourceRevision.getDisplayNameEnSnapshot())
                || !hasText(sourceRevision.getCompanyTypeSnapshot())) {
            throw new IllegalStateException("SHOWROOM_VERSION_COMPANY_SNAPSHOT_MISSING: company snapshot is incomplete");
        }
        if (sourceBundle.getReleasePreviewAssetVersionId() == null) {
            throw new IllegalStateException("SHOWROOM_VERSION_CENTER_NOT_READY: company preview linkage is not provable");
        }
        ShowroomCompanyRevision draft = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                companyId,
                sourceRevision.getCompanyTypeSnapshot(),
                sourceRevision.getDisplayNameSnapshot(),
                sourceRevision.getDisplayNameEnSnapshot(),
                companyFields(sourceRevision)));
        copyNarrationPair(sourceBundle, draft.revisionId(), ShowroomNarrationTargetType.COMPANY, companyId);
        copyPreviewAsset(sourceBundle.getReleasePreviewAssetVersionId(), draft.revisionId(), ShowroomPreviewAssetTargetType.COMPANY,
                companyId);
        ShowroomCompanyRevision published = contentService.publishCompanyRevision(draft.revisionId(), operatorUserId);
        ShowroomVersionBundleDO newBundle = buildBundleForPublishedCompany(sourceBundle, published, companyId, operatorUserId);
        bundleService.insertBundle(newBundle);
        assertScopedReleasePublishable(ShowroomVersionBundleService.TARGET_COMPANY, companyId, published.revisionId());
        try {
            ShowroomMaterializedRelease release = releaseAutoPublishService == null
                    ? releasePublisherService.publishRelease(operatorUserId, Instant.now(), scope.siteKey(), scope.stage())
                    : releaseAutoPublishService.publishNow(operatorUserId, Instant.now(), scope.siteKey(), scope.stage());
            return assembler.republish(ShowroomVersionBundleService.TARGET_COMPANY, companyId, sourceRevisionId,
                    published.revisionId(), published.revisionNo(), release);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("SHOWROOM_VERSION_REPUBLISH_RELEASE_FAILED: " + exception.getMessage(), exception);
        }
    }

    private void copyNarrationPair(ShowroomVersionBundleDO bundle, Long newRevisionId,
                                   ShowroomNarrationTargetType targetType, Long targetId) {
        copyNarration(bundle.getNarrationZhVersionId(), newRevisionId, targetType, targetId, ShowroomNarrationLanguage.ZH);
        copyNarration(bundle.getNarrationEnVersionId(), newRevisionId, targetType, targetId, ShowroomNarrationLanguage.EN);
    }

    private void copyNarration(Long versionId, Long newRevisionId, ShowroomNarrationTargetType targetType, Long targetId,
                               ShowroomNarrationLanguage language) {
        ShowroomNarrationVersion source = narrationService.version(versionId);
        ShowroomNarrationVersion draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                targetType, targetId, newRevisionId, ShowroomNarrationAudienceType.PUBLIC, language,
                source.scriptText(), source.generatedByAi()));
        if (source.audioFileId() != null) {
            narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                    draft.id(), source.audioFileId(), source.audioDurationSeconds(), source.voice()));
        }
        narrationService.publishDirectly(draft.id());
    }

    private void copyPreviewAsset(Long sourcePreviewVersionId, Long newRevisionId, ShowroomPreviewAssetTargetType targetType,
                                  Long targetId) {
        ShowroomPreviewAssetVersion source = previewAssetService.version(sourcePreviewVersionId);
        Long desktopFileId = source.files().desktopFileId();
        if (desktopFileId == null) {
            throw new IllegalStateException("SHOWROOM_VERSION_CENTER_NOT_READY: preview asset desktop file is missing");
        }
        ShowroomPreviewAssetVersion draft = previewAssetService.bindStaticPreviewAssets(new ShowroomPreviewAssetDraftCommand(
                targetType, targetId, newRevisionId,
                new ShowroomPreviewAssetFiles(desktopFileId, desktopFileId, desktopFileId)));
        previewAssetService.publishDirectly(draft.id());
    }

    private ShowroomVersionCenterDetailRespVO.SnapshotRespVO findSnapshot(String targetType, Long targetId, Long revisionId,
                                                                          Long currentContentRevisionId,
                                                                          Long currentPublicRevisionId,
                                                                          List<ShowroomVersionBundleService.ShowroomVersionBlocker> blockers,
                                                                          String scope) {
        ShowroomVersionBundleDO bundle;
        try {
            bundle = bundleService.requireBundle(targetType, targetId, revisionId);
        } catch (IllegalStateException exception) {
            if (!isMissingBundleException(exception)) {
                throw exception;
            }
            blockers.addAll(diagnoseSnapshotUnavailable(targetType, targetId, revisionId, scope));
            return null;
        }
        return buildSnapshot(targetType, targetId, bundle, currentContentRevisionId, currentPublicRevisionId);
    }

    private List<ShowroomVersionBundleService.ShowroomVersionBlocker> diagnoseSnapshotUnavailable(
            String targetType, Long targetId, Long revisionId, String scope) {
        List<ShowroomVersionBundleService.ShowroomVersionBlocker> diagnosed;
        try {
            diagnosed = bundleService.diagnoseMissingBundle(targetType, targetId, revisionId);
        } catch (IllegalStateException exception) {
            return List.of(new ShowroomVersionBundleService.ShowroomVersionBlocker(
                    blockerCode(exception),
                    exception.getMessage(),
                    List.of(revisionId),
                    scope));
        }
        if (diagnosed.isEmpty()) {
            return List.of(new ShowroomVersionBundleService.ShowroomVersionBlocker(
                    "SHOWROOM_VERSION_BUNDLE_NOT_FOUND",
                    "readable version bundle is missing",
                    List.of(revisionId),
                    scope));
        }
        return diagnosed.stream()
                .map(blocker -> relabelBlockerScope(blocker, scope))
                .toList();
    }

    private String blockerCode(IllegalStateException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "SHOWROOM_VERSION_CENTER_NOT_READY";
        }
        int delimiter = message.indexOf(':');
        return delimiter > 0 ? message.substring(0, delimiter) : message;
    }

    private boolean isMissingBundleException(IllegalStateException exception) {
        return exception.getMessage() != null
                && exception.getMessage().startsWith("SHOWROOM_VERSION_BUNDLE_NOT_FOUND:");
    }

    private ShowroomVersionBundleService.ShowroomVersionBlocker relabelBlockerScope(
            ShowroomVersionBundleService.ShowroomVersionBlocker blocker, String scope) {
        return new ShowroomVersionBundleService.ShowroomVersionBlocker(
                blocker.blockerCode(),
                blocker.message(),
                blocker.affectedRevisionIds(),
                scope,
                blocker.targetType(),
                blocker.targetId(),
                blocker.language(),
                blocker.missingFields(),
                blocker.fileId(),
                blocker.assetId(),
                blocker.contentHash(),
                blocker.backendErrorCode());
    }

    private ShowroomVersionCenterDetailRespVO.SnapshotRespVO buildSnapshot(String targetType, Long targetId,
                                                                           ShowroomVersionBundleDO bundle,
                                                                           Long currentContentRevisionId,
                                                                           Long currentPublicRevisionId) {
        if (ShowroomVersionBundleService.TARGET_COMPANY.equals(targetType)) {
            ShowroomCompanyRevisionDO revision = requireCompanyRevision(bundle.getRevisionId());
            List<ShowroomVersionCenterDetailRespVO.FieldValueRespVO> fields = new ArrayList<>();
            for (int index = 0; index < COMPANY_FIELD_ORDER.size(); index++) {
                String fieldCode = COMPANY_FIELD_ORDER.get(index);
                fields.add(new ShowroomVersionCenterDetailRespVO.FieldValueRespVO(
                        fieldCode,
                        ShowroomFieldDisplaySupport.fieldLabel(targetType, fieldCode),
                        ShowroomFieldDisplaySupport.fieldLabelEn(targetType, fieldCode),
                        index + 1,
                        revisionValue(revision, fieldCode),
                        revisionValue(revision, fieldCode + "_en")
                ));
            }
            return new ShowroomVersionCenterDetailRespVO.SnapshotRespVO(
                    bundle.getRevisionId(),
                    bundle.getRevisionNo(),
                    toInstantString(bundle.getPublishedAt()),
                    bundle.getPublishedBy(),
                    bundle.getCopiedFromRevisionId(),
                    bundle.getRevisionId().equals(currentContentRevisionId),
                    bundle.getRevisionId().equals(currentPublicRevisionId),
                    revision.getDisplayNameSnapshot(),
                    revision.getDisplayNameEnSnapshot(),
                    revision.getCompanyTypeSnapshot(),
                    fields,
                    new ShowroomVersionCenterDetailRespVO.ImageRespVO(
                            new ShowroomVersionCenterDetailRespVO.ContentImageRespVO(
                                    "COMPANY_REVISION_COVER_IMAGE",
                                    revision.getCoverImage(),
                                    revision.getDisplayNameSnapshot(),
                                    null,
                                    null,
                                    null
                            ),
                        buildReleasePreviewAsset(bundle, ShowroomPreviewAssetTargetType.COMPANY,
                                revision.getDisplayNameSnapshot())
                    ),
                    buildNarrations(bundle)
            );
        }
        ShowroomProductRevisionDO revision = requireProductRevision(bundle.getRevisionId());
        List<ShowroomVersionCenterDetailRespVO.FieldValueRespVO> fields = new ArrayList<>();
        for (int index = 0; index < PRODUCT_FIELD_ORDER.size(); index++) {
            String fieldCode = PRODUCT_FIELD_ORDER.get(index);
            fields.add(new ShowroomVersionCenterDetailRespVO.FieldValueRespVO(
                    fieldCode,
                    ShowroomFieldDisplaySupport.fieldLabel(targetType, fieldCode),
                    ShowroomFieldDisplaySupport.fieldLabelEn(targetType, fieldCode),
                    index + 1,
                    productFieldValue(revision, fieldCode, false),
                    productFieldValue(revision, fieldCode, true)
            ));
        }
        return new ShowroomVersionCenterDetailRespVO.SnapshotRespVO(
                bundle.getRevisionId(),
                bundle.getRevisionNo(),
                toInstantString(bundle.getPublishedAt()),
                bundle.getPublishedBy(),
                bundle.getCopiedFromRevisionId(),
                bundle.getRevisionId().equals(currentContentRevisionId),
                bundle.getRevisionId().equals(currentPublicRevisionId),
                revision.getNameCn(),
                revision.getNameEn(),
                null,
                fields,
                new ShowroomVersionCenterDetailRespVO.ImageRespVO(
                        new ShowroomVersionCenterDetailRespVO.ContentImageRespVO(
                                "PRODUCT_REVISION_COVER_IMAGE",
                                revision.getCoverImage(),
                                revision.getNameCn(),
                                null,
                                null,
                                null
                        ),
                        buildReleasePreviewAsset(bundle, ShowroomPreviewAssetTargetType.PRODUCT,
                                revision.getNameCn())
                ),
                buildNarrations(bundle)
        );
    }

    private List<ShowroomVersionCenterDetailRespVO.FieldDiffRespVO> buildFieldDiffs(
            ShowroomVersionCenterDetailRespVO.SnapshotRespVO selected,
            ShowroomVersionCenterDetailRespVO.SnapshotRespVO currentContent) {
        if (currentContent == null) {
            return List.of();
        }
        Map<String, ShowroomVersionCenterDetailRespVO.FieldValueRespVO> currentByCode = new LinkedHashMap<>();
        for (ShowroomVersionCenterDetailRespVO.FieldValueRespVO field : currentContent.fields()) {
            currentByCode.put(field.fieldCode(), field);
        }
        List<ShowroomVersionCenterDetailRespVO.FieldDiffRespVO> diffs = new ArrayList<>();
        for (ShowroomVersionCenterDetailRespVO.FieldValueRespVO field : selected.fields()) {
            ShowroomVersionCenterDetailRespVO.FieldValueRespVO current = currentByCode.get(field.fieldCode());
            String currentZh = current == null ? null : current.valueZh();
            String currentEn = current == null ? null : current.valueEn();
            diffs.add(new ShowroomVersionCenterDetailRespVO.FieldDiffRespVO(
                    field.fieldCode(),
                    field.label(),
                    field.labelEn(),
                    field.order(),
                    field.valueZh(),
                    field.valueEn(),
                    currentZh,
                    currentEn,
                    !equalsText(field.valueZh(), currentZh) || !equalsText(field.valueEn(), currentEn)
            ));
        }
        return List.copyOf(diffs);
    }

    private List<ShowroomVersionCenterDetailRespVO.NarrationRespVO> buildNarrations(ShowroomVersionBundleDO bundle) {
        ShowroomNarrationVersion zh = bundle.getNarrationZhVersionId() == null ? null
                : narrationService.version(bundle.getNarrationZhVersionId());
        ShowroomNarrationVersion en = bundle.getNarrationEnVersionId() == null ? null
                : narrationService.version(bundle.getNarrationEnVersionId());
        return List.of(
                toNarrationResp("ZH", zh),
                toNarrationResp("EN", en)
        );
    }

    private ShowroomVersionCenterDetailRespVO.NarrationRespVO toNarrationResp(String language,
                                                                              ShowroomNarrationVersion narration) {
        if (narration == null) {
            return new ShowroomVersionCenterDetailRespVO.NarrationRespVO(language, null, null, null, null, null);
        }
        return new ShowroomVersionCenterDetailRespVO.NarrationRespVO(language, narration.id(), narration.scriptText(),
                narration.audioFileId() == null ? null : fileUrl(narration.audioFileId()),
                narration.audioDurationSeconds(), narration.voice());
    }

    private ShowroomVersionCenterDetailRespVO.ReleasePreviewAssetRespVO buildReleasePreviewAsset(
            ShowroomVersionBundleDO bundle, ShowroomPreviewAssetTargetType targetType, String altText) {
        if (bundle.getReleasePreviewAssetVersionId() == null) {
            return null;
        }
        ShowroomPreviewAssetVersion version = previewAssetService.version(bundle.getReleasePreviewAssetVersionId());
        Long fileId = version.files().desktopFileId();
        return new ShowroomVersionCenterDetailRespVO.ReleasePreviewAssetRespVO(
                targetType == ShowroomPreviewAssetTargetType.COMPANY
                        ? "COMPANY_PREVIEW_ASSET_VERSION"
                        : "PRODUCT_PREVIEW_ASSET_VERSION",
                fileId == null ? null : fileUrl(fileId),
                altText,
                version.id(),
                fileId,
                version.sourceRevisionId()
        );
    }

    private ReleaseState resolveCurrentReleaseState(ShowroomReleaseScope scope) {
        return scopeResolver.executeInTenant(scope, () -> {
            ShowroomReleasePointerDO pointer = releasePointerMapper.selectByPointerScope(scope.tenantId(),
                    scope.siteKey(), scope.stage(), ShowroomReleaseConstants.POINTER_KEY);
            if (pointer == null) {
                return null;
            }
            ShowroomReleaseDO release = releaseMapper.selectByReleaseId(pointer.getReleaseId());
            if (release == null) {
                throw new IllegalStateException("SHOWROOM_RELEASE_UNAVAILABLE: current release record is missing");
            }
            ShowroomReleaseSourceSnapshotDO snapshot = releaseSourceSnapshotMapper.selectByReleaseId(pointer.getReleaseId());
            if (snapshot == null) {
                throw new IllegalStateException("SHOWROOM_RELEASE_BROKEN: current release source snapshot is missing");
            }
            Map<Long, Long> productRevisionByProductId = new LinkedHashMap<>();
            Set<Long> productIdsInDocuments = new LinkedHashSet<>();
            for (Long previewVersionId : JsonUtils.parseArray(snapshot.getPreviewAssetVersionIdsJson(), Long.class)) {
                ShowroomPreviewAssetVersionDO preview = previewAssetVersionMapper.selectById(previewVersionId);
                if (preview == null) {
                    throw new IllegalStateException("SHOWROOM_RELEASE_BROKEN: preview asset version is missing");
                }
                if (ShowroomPreviewAssetTargetType.PRODUCT.name().equals(preview.getTargetType())) {
                    productRevisionByProductId.put(preview.getTargetId(), preview.getSourceRevisionId());
                }
            }
            for (ShowroomReleaseDocumentDO document : releaseDocumentMapper.selectListByReleaseId(pointer.getReleaseId())) {
                if (document.getProductId() != null) {
                    productIdsInDocuments.add(document.getProductId());
                }
            }
            return new ReleaseState(pointer.getReleaseId(), pointer.getManifestHash(),
                    release.getPublishedAt().toInstant(ZoneOffset.UTC), snapshot.getCompanyRevisionId(),
                    Map.copyOf(productRevisionByProductId), Set.copyOf(productIdsInDocuments));
        });
    }

    private Long resolveCurrentContentRevisionId(String targetType, Long targetId) {
        if (ShowroomVersionBundleService.TARGET_COMPANY.equals(targetType)) {
            return requireCompany(targetId).getCurrentRevisionId();
        }
        return requireProduct(targetId).getCurrentRevisionId();
    }

    private Long resolveCurrentPublicRevisionId(String targetType, Long targetId, ReleaseState releaseState) {
        if (releaseState == null) {
            return null;
        }
        if (ShowroomVersionBundleService.TARGET_COMPANY.equals(targetType)) {
            return resolveCurrentPublicCompanyRevisionId(targetId, releaseState);
        }
        Long releaseRevisionId = releaseState.productRevisionByProductId().get(targetId);
        if (!releaseState.productIdsInDocuments().contains(targetId)) {
            return null;
        }
        if (releaseRevisionId != null) {
            return releaseRevisionId;
        }
        return bundleService.listBundles(ShowroomVersionBundleService.TARGET_PRODUCT, targetId).stream()
                .filter(bundle -> bundle.getPublishedAt() != null)
                .filter(bundle -> !bundle.getPublishedAt().toInstant(ZoneOffset.UTC).isAfter(releaseState.publishedAt()))
                .sorted((left, right) -> {
                    int publishedAtOrder = right.getPublishedAt().compareTo(left.getPublishedAt());
                    if (publishedAtOrder != 0) {
                        return publishedAtOrder;
                    }
                    int revisionNoOrder = Integer.compare(
                            right.getRevisionNo() == null ? Integer.MIN_VALUE : right.getRevisionNo(),
                            left.getRevisionNo() == null ? Integer.MIN_VALUE : left.getRevisionNo());
                    if (revisionNoOrder != 0) {
                        return revisionNoOrder;
                    }
                    return Long.compare(right.getRevisionId(), left.getRevisionId());
                })
                .map(ShowroomVersionBundleDO::getRevisionId)
                .findFirst()
                .orElse(null);
    }

    private Long resolveCurrentPublicRevisionIdForDetail(String targetType, Long targetId, ReleaseState releaseState) {
        if (releaseState == null) {
            return null;
        }
        if (ShowroomVersionBundleService.TARGET_COMPANY.equals(targetType)) {
            Long releaseRevisionId = releaseState.companyRevisionId();
            if (releaseRevisionId == null) {
                return null;
            }
            ShowroomCompanyRevisionDO revision = companyRevisionMapper.selectById(releaseRevisionId);
            if (revision == null || !Objects.equals(targetId, revision.getCompanyId())) {
                return releaseRevisionId;
            }
            return releaseRevisionId;
        }
        return resolveCurrentPublicRevisionId(targetType, targetId, releaseState);
    }

    private Long resolveCurrentPublicCompanyRevisionId(Long companyId, ReleaseState releaseState) {
        Long releaseRevisionId = releaseState.companyRevisionId();
        if (releaseRevisionId == null) {
            return null;
        }
        ShowroomCompanyRevisionDO revision = companyRevisionMapper.selectById(releaseRevisionId);
        if (revision == null || !Objects.equals(companyId, revision.getCompanyId())) {
            log.warn("SHOWROOM_VERSION_CENTER_BLOCKER targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={} blockerScope={} blockerCode={}",
                    ShowroomVersionBundleService.TARGET_COMPANY, companyId, null, null, null, releaseRevisionId,
                    releaseState.releaseId(), null, null, "CURRENT_RELEASE",
                    "SHOWROOM_VERSION_PUBLIC_RELEASE_TARGET_MISMATCH");
            return null;
        }
        return releaseRevisionId;
    }

    private boolean hasPublishedRevision(String targetType, Long targetId) {
        if (ShowroomVersionBundleService.TARGET_COMPANY.equals(targetType)) {
            return !companyRevisionMapper.selectPublishedByCompanyId(targetId).isEmpty();
        }
        return !productRevisionMapper.selectPublishedByProductId(targetId).isEmpty();
    }

    private void assertCurrentHistoryAnchorsReadable(String targetType, Long targetId,
                                                    List<ShowroomVersionBundleDO> bundles,
                                                    Long currentContentRevisionId,
                                                    Long currentPublicRevisionId,
                                                    ReleaseState releaseState) {
        if (!hasPublishedRevision(targetType, targetId)) {
            return;
        }
        Set<Long> bundledRevisionIds = new LinkedHashSet<>();
        for (ShowroomVersionBundleDO bundle : bundles) {
            bundledRevisionIds.add(bundle.getRevisionId());
        }
        Set<Long> requiredRevisionIds = new LinkedHashSet<>();
        if (currentContentRevisionId != null) {
            requiredRevisionIds.add(currentContentRevisionId);
        }
        if (currentPublicRevisionId != null) {
            requiredRevisionIds.add(currentPublicRevisionId);
        }
        if (requiredRevisionIds.isEmpty()) {
            requiredRevisionIds.addAll(publishedRevisionIds(targetType, targetId));
        }
        List<Long> missingRevisionIds = requiredRevisionIds.stream()
                .filter(revisionId -> !bundledRevisionIds.contains(revisionId))
                .toList();
        if (!missingRevisionIds.isEmpty()) {
            log.warn("SHOWROOM_VERSION_CENTER_BLOCKER targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={} blockerScope={} blockerCode={}",
                    targetType, targetId, null, null, currentContentRevisionId, currentPublicRevisionId,
                    releaseState == null ? null : releaseState.releaseId(), null, null, "SELECTED_VERSION",
                    "SHOWROOM_VERSION_CENTER_NOT_READY");
            throw new IllegalStateException("SHOWROOM_VERSION_CENTER_NOT_READY: current revisions missing readable bundle "
                    + missingRevisionIds);
        }
    }

    private IllegalStateException historyBundleIntegrityException(
            List<ShowroomVersionBundleService.ShowroomVersionBlocker> blockers) {
        ShowroomVersionBundleService.ShowroomVersionBlocker blocker = blockers.get(0);
        return new IllegalStateException(blocker.blockerCode() + ": " + blocker.message());
    }

    private boolean isCoreHistoryBlocker(ShowroomVersionBundleService.ShowroomVersionBlocker blocker) {
        return "SHOWROOM_VERSION_BUNDLE_NOT_FOUND".equals(blocker.blockerCode())
                || "SHOWROOM_VERSION_COMPANY_SNAPSHOT_MISSING".equals(blocker.blockerCode())
                || "SHOWROOM_VERSION_CENTER_NOT_READY".equals(blocker.blockerCode());
    }

    private List<Long> publishedRevisionIds(String targetType, Long targetId) {
        if (ShowroomVersionBundleService.TARGET_COMPANY.equals(targetType)) {
            return companyRevisionMapper.selectPublishedByCompanyId(targetId).stream()
                    .map(ShowroomCompanyRevisionDO::getId)
                    .toList();
        }
        return productRevisionMapper.selectPublishedByProductId(targetId).stream()
                .map(ShowroomProductRevisionDO::getId)
                .toList();
    }

    private void assertScopedReleasePublishable(String targetType, Long targetId, Long stagedRevisionId) {
        try {
            ShowroomReleaseSourceSnapshot snapshot = releaseAssembler.resolveSourceSnapshot();
            assertSnapshotUsesStagedTargetState(snapshot, targetType, targetId, stagedRevisionId);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("SHOWROOM_VERSION_REPUBLISH_PUBLIC_RELEASE_BLOCKED: PUBLIC_RELEASE: "
                    + exception.getMessage() + "; targetType=" + targetType + "; targetId=" + targetId
                    + "; stagedRevisionId=" + stagedRevisionId, exception);
        }
    }

    private void assertSnapshotUsesStagedTargetState(ShowroomReleaseSourceSnapshot snapshot,
                                                     String targetType,
                                                     Long targetId,
                                                     Long stagedRevisionId) {
        if (ShowroomVersionBundleService.TARGET_COMPANY.equals(targetType)) {
            if (!Objects.equals(snapshot.companyRevision().revisionId(), stagedRevisionId)) {
                throw new IllegalStateException("staged company revision was not selected into release snapshot");
            }
            return;
        }
        ShowroomReleaseSourceSnapshot.ResolvedProduct product = snapshot.productsById().get(targetId);
        if (product == null) {
            throw new IllegalStateException("staged product is absent from release snapshot");
        }
        if (!Objects.equals(product.revision().revisionId(), stagedRevisionId)) {
            throw new IllegalStateException("staged product revision was not selected into release snapshot");
        }
    }

    private void appendBundleSpecificBlockers(List<ShowroomVersionBundleService.ShowroomVersionBlocker> blockers,
                                              String targetType, Long targetId, Long revisionId,
                                              ShowroomVersionBundleDO bundle) {
        if (ShowroomVersionBundleService.TARGET_COMPANY.equals(targetType) && bundle.getReleasePreviewAssetVersionId() == null) {
            blockers.add(new ShowroomVersionBundleService.ShowroomVersionBlocker(
                    "SHOWROOM_VERSION_CENTER_NOT_READY",
                    "company preview linkage is not provable for the selected historical revision",
                    List.of(revisionId),
                    "SELECTED_VERSION"));
        }
    }

    private ShowroomVersionBundleDO buildBundleForPublishedProduct(ShowroomVersionBundleDO sourceBundle,
                                                                   ShowroomProductRevision published,
                                                                   Long productId,
                                                                   Long operatorUserId) {
        return ShowroomVersionBundleDO.builder()
                .targetType(ShowroomVersionBundleService.TARGET_PRODUCT)
                .targetId(productId)
                .revisionId(published.revisionId())
                .revisionNo(published.revisionNo())
                .releasePreviewAssetVersionId(null)
                .narrationZhVersionId(narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.PRODUCT, productId,
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH)).orElseThrow().id())
                .narrationEnVersionId(narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.PRODUCT, productId,
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN)).orElseThrow().id())
                .copiedFromRevisionId(sourceBundle.getRevisionId())
                .publishedBy(operatorUserId)
                .publishedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private ShowroomVersionBundleDO buildBundleForPublishedCompany(ShowroomVersionBundleDO sourceBundle,
                                                                   ShowroomCompanyRevision published,
                                                                   Long companyId,
                                                                   Long operatorUserId) {
        return ShowroomVersionBundleDO.builder()
                .targetType(ShowroomVersionBundleService.TARGET_COMPANY)
                .targetId(companyId)
                .revisionId(published.revisionId())
                .revisionNo(published.revisionNo())
                .releasePreviewAssetVersionId(previewAssetService.live(new ShowroomPreviewAssetKey(
                        ShowroomPreviewAssetTargetType.COMPANY, companyId)).orElseThrow().id())
                .narrationZhVersionId(narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.COMPANY, companyId,
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH)).orElseThrow().id())
                .narrationEnVersionId(narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.COMPANY, companyId,
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN)).orElseThrow().id())
                .copiedFromRevisionId(sourceBundle.getRevisionId())
                .publishedBy(operatorUserId)
                .publishedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private String resolveSummaryImageUrl(String targetType, ShowroomVersionBundleDO bundle) {
        if (ShowroomVersionBundleService.TARGET_PRODUCT.equals(targetType)) {
            return requireProductRevision(bundle.getRevisionId()).getCoverImage();
        }
        if (bundle.getReleasePreviewAssetVersionId() != null) {
            ShowroomPreviewAssetVersion preview = previewAssetService.version(bundle.getReleasePreviewAssetVersionId());
            if (preview.files().desktopFileId() != null) {
                return fileUrl(preview.files().desktopFileId());
            }
        }
        return requireCompanyRevision(bundle.getRevisionId()).getCoverImage();
    }

    private ShowroomCompanyDO requireCompany(Long companyId) {
        ShowroomCompanyDO company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company not found");
        }
        return company;
    }

    private ShowroomProductDO requireProduct(Long productId) {
        ShowroomProductDO product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product not found");
        }
        return product;
    }

    private ShowroomCompanyRevisionDO requireCompanyRevision(Long revisionId) {
        ShowroomCompanyRevisionDO revision = companyRevisionMapper.selectById(revisionId);
        if (revision == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision not found");
        }
        return revision;
    }

    private ShowroomProductRevisionDO requireProductRevision(Long revisionId) {
        ShowroomProductRevisionDO revision = productRevisionMapper.selectById(revisionId);
        if (revision == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
        }
        return revision;
    }

    private String fileUrl(Long fileId) {
        FileDO file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: file not found: " + fileId);
        }
        return "/admin-api/infra/file/" + file.getConfigId() + "/get/" + file.getPath();
    }

    private Map<String, String> companyFields(ShowroomCompanyRevisionDO revision) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("development_history", revision.getDevelopmentHistory());
        fields.put("development_history_en", revision.getDevelopmentHistoryEn());
        fields.put("park_introduction", revision.getParkIntroduction());
        fields.put("park_introduction_en", revision.getParkIntroductionEn());
        fields.put("incubation_platform", revision.getIncubationPlatform());
        fields.put("incubation_platform_en", revision.getIncubationPlatformEn());
        fields.put("subsidiary_overview", revision.getSubsidiaryOverview());
        fields.put("subsidiary_overview_en", revision.getSubsidiaryOverviewEn());
        fields.put("stock_info", revision.getStockInfo());
        fields.put("stock_info_en", revision.getStockInfoEn());
        fields.put("cover_image", revision.getCoverImage());
        fields.put("core_manufacturing_capability", revision.getCoreManufacturingCapability());
        fields.put("core_manufacturing_capability_en", revision.getCoreManufacturingCapabilityEn());
        fields.put("honors_awards", revision.getHonorsAwards());
        fields.put("honors_awards_en", revision.getHonorsAwardsEn());
        return fields;
    }

    private Map<String, String> productFields(ShowroomProductRevisionDO revision) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("owner_company_id", revision.getOwnerCompanyId() == null ? null : String.valueOf(revision.getOwnerCompanyId()));
        fields.put("product_owner_type", revision.getProductOwnerType());
        fields.put("lifecycle_stage", revision.getLifecycleStage());
        fields.put("target_market", revision.getTargetMarket());
        fields.put("target_market_en", revision.getTargetMarketEn());
        fields.put("pipeline_layout", revision.getPipelineLayout());
        fields.put("pipeline_layout_en", revision.getPipelineLayoutEn());
        fields.put("registration_certificate", revision.getRegistrationCertificate());
        fields.put("registration_certificate_en", revision.getRegistrationCertificateEn());
        fields.put("indication_content", revision.getIndicationContent());
        fields.put("indication_content_en", revision.getIndicationContentEn());
        fields.put("core_selling_points", revision.getCoreSellingPoints());
        fields.put("core_selling_points_en", revision.getCoreSellingPointsEn());
        fields.put("model_specification", revision.getModelSpecification());
        fields.put("model_specification_en", revision.getModelSpecificationEn());
        fields.put("cover_image", revision.getCoverImage());
        fields.put("clinical_effect", revision.getClinicalEffect());
        fields.put("clinical_effect_en", revision.getClinicalEffectEn());
        fields.put("fim_status", revision.getFimStatus());
        fields.put("fim_status_en", revision.getFimStatusEn());
        return fields;
    }

    private String revisionValue(ShowroomCompanyRevisionDO revision, String fieldCode) {
        return switch (fieldCode) {
            case "development_history" -> revision.getDevelopmentHistory();
            case "development_history_en" -> revision.getDevelopmentHistoryEn();
            case "park_introduction" -> revision.getParkIntroduction();
            case "park_introduction_en" -> revision.getParkIntroductionEn();
            case "incubation_platform" -> revision.getIncubationPlatform();
            case "incubation_platform_en" -> revision.getIncubationPlatformEn();
            case "subsidiary_overview" -> revision.getSubsidiaryOverview();
            case "subsidiary_overview_en" -> revision.getSubsidiaryOverviewEn();
            case "stock_info" -> revision.getStockInfo();
            case "stock_info_en" -> revision.getStockInfoEn();
            case "core_manufacturing_capability" -> revision.getCoreManufacturingCapability();
            case "core_manufacturing_capability_en" -> revision.getCoreManufacturingCapabilityEn();
            case "honors_awards" -> revision.getHonorsAwards();
            case "honors_awards_en" -> revision.getHonorsAwardsEn();
            default -> null;
        };
    }

    private String productFieldValue(ShowroomProductRevisionDO revision, String fieldCode, boolean english) {
        String raw = switch (fieldCode) {
            case "owner_company_id" -> revision.getOwnerCompanyId() == null ? null : String.valueOf(revision.getOwnerCompanyId());
            case "product_owner_type" -> revision.getProductOwnerType();
            case "lifecycle_stage" -> revision.getLifecycleStage();
            case "target_market" -> english ? revision.getTargetMarketEn() : revision.getTargetMarket();
            case "pipeline_layout" -> english ? revision.getPipelineLayoutEn() : revision.getPipelineLayout();
            case "indication_content" -> english ? revision.getIndicationContentEn() : revision.getIndicationContent();
            case "core_selling_points" -> english ? revision.getCoreSellingPointsEn() : revision.getCoreSellingPoints();
            case "model_specification" -> english ? revision.getModelSpecificationEn() : revision.getModelSpecification();
            case "registration_certificate" -> english ? revision.getRegistrationCertificateEn() : revision.getRegistrationCertificate();
            case "clinical_effect" -> english ? revision.getClinicalEffectEn() : revision.getClinicalEffect();
            case "fim_status" -> english ? revision.getFimStatusEn() : revision.getFimStatus();
            case "cover_image" -> english ? null : revision.getCoverImage();
            default -> null;
        };
        return raw == null ? null : ShowroomFieldDisplaySupport.formatStoredFieldValue(
                ShowroomVersionBundleService.TARGET_PRODUCT, fieldCode, raw, contentService, english);
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean equalsText(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String toInstantString(LocalDateTime value) {
        return value == null ? null : DateTimeFormatter.ISO_INSTANT.format(value.toInstant(ZoneOffset.UTC));
    }

    private record ReleaseState(String releaseId,
                                String manifestHash,
                                Instant publishedAt,
                                Long companyRevisionId,
                                Map<Long, Long> productRevisionByProductId,
                                Set<Long> productIdsInDocuments) {
    }
}
