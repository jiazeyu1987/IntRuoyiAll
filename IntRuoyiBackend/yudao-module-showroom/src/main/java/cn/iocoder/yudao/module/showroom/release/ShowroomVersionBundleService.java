package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.narration.ShowroomNarrationVersionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.version.ShowroomVersionBundleDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomCompanyRevisionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.narration.ShowroomNarrationVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.version.ShowroomVersionBundleMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class ShowroomVersionBundleService {

    private static final Logger log = LoggerFactory.getLogger(ShowroomVersionBundleService.class);

    public static final String TARGET_COMPANY = "COMPANY";
    public static final String TARGET_PRODUCT = "PRODUCT";
    public static final String BLOCKER_SCOPE_SELECTED = "SELECTED_VERSION";

    private final ShowroomVersionBundleMapper bundleMapper;
    private final ShowroomCompanyRevisionMapper companyRevisionMapper;
    private final ShowroomProductRevisionMapper productRevisionMapper;
    private final ShowroomNarrationVersionMapper narrationVersionMapper;

    public ShowroomVersionBundleService(ShowroomVersionBundleMapper bundleMapper,
                                         ShowroomCompanyRevisionMapper companyRevisionMapper,
                                         ShowroomProductRevisionMapper productRevisionMapper,
                                         ShowroomNarrationVersionMapper narrationVersionMapper) {
        this.bundleMapper = bundleMapper;
        this.companyRevisionMapper = companyRevisionMapper;
        this.productRevisionMapper = productRevisionMapper;
        this.narrationVersionMapper = narrationVersionMapper;
    }

    public String normalizeTargetType(String targetType) {
        if (TARGET_COMPANY.equalsIgnoreCase(targetType)) {
            return TARGET_COMPANY;
        }
        if (TARGET_PRODUCT.equalsIgnoreCase(targetType)) {
            return TARGET_PRODUCT;
        }
        throw new IllegalStateException("SHOWROOM_VERSION_TARGET_TYPE_INVALID: " + targetType);
    }

    public ShowroomVersionBundleDO requireBundle(String targetType, Long targetId, Long revisionId) {
        ShowroomVersionBundleDO bundle = bundleMapper.selectByTargetAndRevision(targetType, targetId, revisionId);
        if (bundle == null) {
            throw new IllegalStateException("SHOWROOM_VERSION_BUNDLE_NOT_FOUND: " + targetType + ":" + targetId + ":" + revisionId);
        }
        return bundle;
    }

    public List<ShowroomVersionBundleDO> listBundles(String targetType, Long targetId) {
        return bundleMapper.selectListByTarget(targetType, targetId);
    }

    public void insertBundle(ShowroomVersionBundleDO bundle) {
        assignCurrentTenant(bundle);
        bundleMapper.insert(bundle);
    }

    public ShowroomVersionBundleDO ensureBundleForPublishedRevision(String targetType,
                                                                    Long targetId,
                                                                    Long revisionId,
                                                                    Long publishedBy,
                                                                    Long copiedFromRevisionId) {
        return ensureBundleForPublishedRevision(targetType, targetId, revisionId, publishedBy,
                copiedFromRevisionId, null);
    }

    public ShowroomVersionBundleDO ensureBundleForPublishedRevision(String targetType,
                                                                    Long targetId,
                                                                    Long revisionId,
                                                                    Long publishedBy,
                                                                    Long copiedFromRevisionId,
                                                                    Long releasePreviewAssetVersionId) {
        String normalizedTargetType = normalizeTargetType(targetType);
        ShowroomVersionBundleDO existing = bundleMapper.selectByTargetAndRevision(normalizedTargetType, targetId,
                revisionId);
        if (existing != null) {
            return existing;
        }
        ShowroomVersionBundleDO bundle = TARGET_COMPANY.equals(normalizedTargetType)
                ? buildCompanyBundle(targetId, revisionId, publishedBy, copiedFromRevisionId,
                releasePreviewAssetVersionId)
                : buildProductBundle(targetId, revisionId, publishedBy, copiedFromRevisionId);
        assignCurrentTenant(bundle);
        bundleMapper.insert(bundle);
        return bundle;
    }

    private void assignCurrentTenant(ShowroomVersionBundleDO bundle) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (bundle.getTenantId() != null && !Objects.equals(bundle.getTenantId(), tenantId)) {
            throw new IllegalStateException("SHOWROOM_VERSION_BUNDLE_TENANT_MISMATCH: bundle tenant does not match current tenant");
        }
        bundle.setTenantId(tenantId);
    }

    public List<ShowroomVersionBlocker> diagnoseMissingBundle(String targetType, Long targetId, Long revisionId) {
        List<ShowroomVersionBlocker> blockers = new ArrayList<>();
        if (TARGET_COMPANY.equals(targetType)) {
            ShowroomCompanyRevisionDO revision = companyRevisionMapper.selectById(revisionId);
            if (revision == null || !targetId.equals(revision.getCompanyId())) {
                throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision not found");
            }
            if (!hasText(revision.getDisplayNameSnapshot())
                    || !hasText(revision.getDisplayNameEnSnapshot())
                    || !hasText(revision.getCompanyTypeSnapshot())) {
                blockers.add(new ShowroomVersionBlocker("SHOWROOM_VERSION_COMPANY_SNAPSHOT_MISSING",
                        "company revision is missing authoritative display-name/type snapshot",
                        List.of(revisionId), BLOCKER_SCOPE_SELECTED));
            }
            appendNarrationBlockers(blockers, targetType, targetId, revisionId);
            return blockers;
        }
        ShowroomProductRevisionDO revision = productRevisionMapper.selectById(revisionId);
        if (revision == null || !targetId.equals(revision.getProductId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
        }
        appendNarrationBlockers(blockers, targetType, targetId, revisionId);
        if (!hasText(revision.getCoverImage())) {
            blockers.add(blocker("PRODUCT_COVER_MISSING", "historical product cover_image is missing",
                    revisionId, targetType, targetId, null, List.of("cover_image"), null, null, null));
        }
        return blockers;
    }

    public List<ShowroomVersionBlocker> diagnoseBundleIntegrity(String targetType,
                                                                Long targetId,
                                                                ShowroomVersionBundleDO bundle) {
        List<ShowroomVersionBlocker> blockers = new ArrayList<>();
        Long revisionId = bundle.getRevisionId();
        if (TARGET_COMPANY.equals(targetType)) {
            ShowroomCompanyRevisionDO revision = companyRevisionMapper.selectById(revisionId);
            if (revision == null || !Objects.equals(targetId, revision.getCompanyId())) {
                blockers.add(blocker("SHOWROOM_VERSION_BUNDLE_NOT_FOUND",
                        "company revision no longer matches bundle target", revisionId));
                return blockers;
            }
            if (!hasText(revision.getDisplayNameSnapshot())
                    || !hasText(revision.getDisplayNameEnSnapshot())
                    || !hasText(revision.getCompanyTypeSnapshot())) {
                blockers.add(blocker("SHOWROOM_VERSION_COMPANY_SNAPSHOT_MISSING",
                        "company revision is missing authoritative display-name/type snapshot", revisionId));
            }
        } else {
            ShowroomProductRevisionDO revision = productRevisionMapper.selectById(revisionId);
            if (revision == null || !Objects.equals(targetId, revision.getProductId())) {
                blockers.add(blocker("SHOWROOM_VERSION_BUNDLE_NOT_FOUND",
                        "product revision no longer matches bundle target", revisionId));
                return blockers;
            }
            validateProductCoverIntegrity(revision, targetType, targetId, blockers);
        }
        validateNarrationIntegrity(targetType, targetId, bundle.getNarrationZhVersionId(), revisionId,
                ShowroomNarrationLanguage.ZH, blockers);
        validateNarrationIntegrity(targetType, targetId, bundle.getNarrationEnVersionId(), revisionId,
                ShowroomNarrationLanguage.EN, blockers);
        for (ShowroomVersionBlocker blocker : blockers) {
            log.warn("SHOWROOM_VERSION_CENTER_BLOCKER targetType={} targetId={} sourceRevisionId={} selectedRevisionId={} currentContentRevisionId={} currentPublicRevisionId={} releaseId={} operatorUserId={} durationMs={} blockerScope={} blockerCode={}",
                    targetType, targetId, null, revisionId, null, null, null, null, null, blocker.scope(),
                    blocker.blockerCode());
        }
        return blockers;
    }

    private void appendNarrationBlockers(List<ShowroomVersionBlocker> blockers,
                                         String targetType,
                                         Long targetId,
                                         Long revisionId) {
        appendNarrationBlocker(blockers, targetType, targetId, revisionId, ShowroomNarrationLanguage.ZH);
        appendNarrationBlocker(blockers, targetType, targetId, revisionId, ShowroomNarrationLanguage.EN);
    }

    private void appendNarrationBlocker(List<ShowroomVersionBlocker> blockers,
                                        String targetType,
                                        Long targetId,
                                        Long revisionId,
                                        ShowroomNarrationLanguage language) {
        List<?> versions = narrationVersionMapper.selectPublishedByTargetAndSourceRevision(
                targetType, targetId, ShowroomNarrationAudienceType.PUBLIC.name(), language.name(), revisionId);
        if (versions.isEmpty()) {
            blockers.add(blocker("PRODUCT_NARRATION_AUDIO_MISSING",
                    "historical " + language.name() + " narration audio is missing", revisionId,
                    targetType, targetId, language.name(), List.of("audioFileId"), null, null, null));
        } else if (versions.size() > 1) {
            blockers.add(new ShowroomVersionBlocker("SHOWROOM_VERSION_CENTER_NOT_READY",
                    "historical " + language.name() + " narration has multiple published candidates",
                    List.of(revisionId), BLOCKER_SCOPE_SELECTED));
        }
    }

    private void validateNarrationIntegrity(String targetType,
                                            Long targetId,
                                            Long bundleNarrationVersionId,
                                            Long revisionId,
                                            ShowroomNarrationLanguage language,
                                            List<ShowroomVersionBlocker> blockers) {
        if (bundleNarrationVersionId == null) {
            blockers.add(blocker("PRODUCT_NARRATION_AUDIO_MISSING",
                    "bundle " + language.name() + " narration audio is missing", revisionId,
                    targetType, targetId, language.name(), List.of("audioFileId"), null, null, null));
            return;
        }
        ShowroomNarrationVersionDO narration = narrationVersionMapper.selectById(bundleNarrationVersionId);
        if (narration == null
                || !Objects.equals(targetId, narration.getTargetId())
                || !Objects.equals(revisionId, narration.getSourceRevisionId())
                || !ShowroomNarrationAudienceType.PUBLIC.name().equals(narration.getAudienceType())
                || !language.name().equals(narration.getLanguage())
                || !"PUBLISHED".equals(narration.getStatus())) {
            blockers.add(blocker("PRODUCT_NARRATION_AUDIO_MISSING",
                    "bundle " + language.name() + " narration version is missing or no longer readable", revisionId,
                    targetType, targetId, language.name(), List.of("audioFileId"), null, null, null));
            return;
        }
        if (narration.getAudioFileId() == null) {
            blockers.add(blocker("PRODUCT_NARRATION_AUDIO_MISSING",
                    "bundle " + language.name() + " narration audio file is missing", revisionId,
                    targetType, targetId, language.name(), List.of("audioFileId"), null, null, null));
        }
    }

    private void validateProductCoverIntegrity(ShowroomProductRevisionDO revision,
                                               String targetType,
                                               Long targetId,
                                               List<ShowroomVersionBlocker> blockers) {
        if (!hasText(revision.getCoverImage())) {
            blockers.add(blocker("PRODUCT_COVER_MISSING",
                    "bundle product cover_image is missing", revision.getId(), targetType, targetId, null,
                    List.of("cover_image"), null, null, null));
        }
    }

    private static ShowroomVersionBlocker blocker(String blockerCode, String message, Long revisionId) {
        return new ShowroomVersionBlocker(blockerCode, message, List.of(revisionId), BLOCKER_SCOPE_SELECTED);
    }

    private static ShowroomVersionBlocker blocker(String blockerCode, String message, Long revisionId,
                                                  String targetType, Long targetId, String language,
                                                  List<String> missingFields, Long fileId, String assetId,
                                                  String contentHash) {
        return new ShowroomVersionBlocker(blockerCode, message, List.of(revisionId), BLOCKER_SCOPE_SELECTED,
                targetType, targetId, language, missingFields, fileId, assetId, contentHash, blockerCode);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private ShowroomVersionBundleDO buildCompanyBundle(Long companyId,
                                                       Long revisionId,
                                                       Long publishedBy,
                                                       Long copiedFromRevisionId,
                                                       Long releasePreviewAssetVersionId) {
        ShowroomCompanyRevisionDO revision = companyRevisionMapper.selectById(revisionId);
        if (revision == null || !Objects.equals(companyId, revision.getCompanyId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision not found");
        }
        if (!"PUBLISHED".equals(revision.getStatus())) {
            throw new IllegalStateException("SHOWROOM_VERSION_CENTER_NOT_READY: company revision is not published");
        }
        ShowroomNarrationVersionDO zhNarration = requireSinglePublishedNarration(TARGET_COMPANY, companyId,
                revisionId, ShowroomNarrationLanguage.ZH);
        ShowroomNarrationVersionDO enNarration = requireSinglePublishedNarration(TARGET_COMPANY, companyId,
                revisionId, ShowroomNarrationLanguage.EN);
        return ShowroomVersionBundleDO.builder()
                .targetType(TARGET_COMPANY)
                .targetId(companyId)
                .revisionId(revisionId)
                .revisionNo(revision.getRevisionNo())
                .releasePreviewAssetVersionId(releasePreviewAssetVersionId)
                .narrationZhVersionId(zhNarration.getId())
                .narrationEnVersionId(enNarration.getId())
                .copiedFromRevisionId(copiedFromRevisionId)
                .publishedBy(publishedBy)
                .publishedAt(requirePublishedAt(revision.getPublishedAt(), TARGET_COMPANY, revisionId))
                .build();
    }

    private ShowroomVersionBundleDO buildProductBundle(Long productId,
                                                       Long revisionId,
                                                       Long publishedBy,
                                                       Long copiedFromRevisionId) {
        ShowroomProductRevisionDO revision = productRevisionMapper.selectById(revisionId);
        if (revision == null || !Objects.equals(productId, revision.getProductId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
        }
        if (!"PUBLISHED".equals(revision.getStatus())) {
            throw new IllegalStateException("SHOWROOM_VERSION_CENTER_NOT_READY: product revision is not published");
        }
        return ShowroomVersionBundleDO.builder()
                .targetType(TARGET_PRODUCT)
                .targetId(productId)
                .revisionId(revisionId)
                .revisionNo(revision.getRevisionNo())
                .releasePreviewAssetVersionId(null)
                .narrationZhVersionId(findSinglePublishedNarrationId(TARGET_PRODUCT, productId,
                        revisionId, ShowroomNarrationLanguage.ZH))
                .narrationEnVersionId(findSinglePublishedNarrationId(TARGET_PRODUCT, productId,
                        revisionId, ShowroomNarrationLanguage.EN))
                .copiedFromRevisionId(copiedFromRevisionId)
                .publishedBy(publishedBy)
                .publishedAt(requirePublishedAt(revision.getPublishedAt(), TARGET_PRODUCT, revisionId))
                .build();
    }

    private Long findSinglePublishedNarrationId(String targetType,
                                                Long targetId,
                                                Long revisionId,
                                                ShowroomNarrationLanguage language) {
        List<ShowroomNarrationVersionDO> versions = narrationVersionMapper.selectPublishedByTargetAndSourceRevision(
                targetType, targetId, ShowroomNarrationAudienceType.PUBLIC.name(), language.name(), revisionId);
        if (versions.isEmpty() || versions.size() > 1) {
            return null;
        }
        return versions.get(0).getId();
    }

    private ShowroomNarrationVersionDO requireSinglePublishedNarration(String targetType,
                                                                       Long targetId,
                                                                       Long revisionId,
                                                                       ShowroomNarrationLanguage language) {
        List<ShowroomNarrationVersionDO> versions = narrationVersionMapper.selectPublishedByTargetAndSourceRevision(
                targetType, targetId, ShowroomNarrationAudienceType.PUBLIC.name(), language.name(), revisionId);
        if (versions.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_VERSION_CENTER_NOT_READY: historical " + language.name()
                    + " narration is missing");
        }
        if (versions.size() > 1) {
            throw new IllegalStateException("SHOWROOM_VERSION_CENTER_NOT_READY: historical " + language.name()
                    + " narration has multiple published candidates");
        }
        return versions.get(0);
    }

    private static LocalDateTime requirePublishedAt(LocalDateTime publishedAt, String targetType, Long revisionId) {
        if (publishedAt == null) {
            throw new IllegalStateException("SHOWROOM_VERSION_CENTER_NOT_READY: " + targetType
                    + " revision published_at is missing for " + revisionId);
        }
        return publishedAt;
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    public record ShowroomVersionBlocker(String blockerCode,
                                         String message,
                                         List<Long> affectedRevisionIds,
                                         String scope,
                                         String targetType,
                                         Long targetId,
                                         String language,
                                         List<String> missingFields,
                                         Long fileId,
                                         String assetId,
                                         String contentHash,
                                         String backendErrorCode) {
        public ShowroomVersionBlocker(String blockerCode, String message, List<Long> affectedRevisionIds,
                                      String scope) {
            this(blockerCode, message, affectedRevisionIds, scope, null, null, null, List.of(), null, null, null,
                    blockerCode);
        }
    }
}
