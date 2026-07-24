package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetDraftCommand;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetFiles;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetKey;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterDetailRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterHistoryRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterRepublishReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterRepublishRespVO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseSourceSnapshotDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleasePointerDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.version.ShowroomVersionBundleDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomCompanyMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleasePointerMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseSourceSnapshotMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.version.ShowroomVersionBundleMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudioDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.release.AbstractShowroomReleaseDbTest;
import cn.iocoder.yudao.module.showroom.release.ShowroomMaterializedRelease;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({
        ShowroomVersionCenterService.class,
        ShowroomVersionBundleService.class,
        ShowroomVersionCenterAssembler.class
})
class ShowroomVersionCenterServiceTest extends AbstractShowroomReleaseDbTest {

    private static final String SITE_KEY = "yingtai-showroom";
    private static final String STAGE = "TEST";

    @Resource
    private ShowroomVersionCenterService versionCenterService;

    @Resource
    private ShowroomVersionBundleMapper versionBundleMapper;

    @Resource
    private ShowroomReleasePointerMapper releasePointerMapper;

    @Resource
    private ShowroomReleaseSourceSnapshotMapper releaseSourceSnapshotMapper;

    @Resource
    private ShowroomVersionBundleService versionBundleService;

    @Resource
    private ShowroomCompanyMapper companyMapper;

    @Test
    void companyHistoryShouldIgnoreCurrentReleaseRevisionWhenItBelongsToAnotherCompany() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        ShowroomMaterializedRelease release = publishScopedRelease(fixture.publishedAt());
        insertCompanyBundle(fixture.companyId(), fixture.companyRevisionId(), null, null);
        ShowroomCompanyRevision otherCompanyRevision = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "SUBSIDIARY", "其他公司", "Other Company",
                        Map.of("development_history", "其他公司历史",
                                "development_history_en", "Other history",
                                "cover_image", "/admin-api/infra/file/11/get/showroom/company/other.png")))
                .revisionId(), 905L);
        ShowroomReleaseSourceSnapshotDO sourceSnapshot = releaseSourceSnapshotMapper.selectByReleaseId(release.releaseId());
        sourceSnapshot.setCompanyRevisionId(otherCompanyRevision.revisionId());
        releaseSourceSnapshotMapper.updateById(sourceSnapshot);

        ShowroomVersionCenterHistoryRespVO history = versionCenterService.getHistory("COMPANY", fixture.companyId(),
                SITE_KEY, STAGE);

        assertEquals(fixture.companyRevisionId(), history.currentContentRevisionId());
        assertEquals(null, history.currentPublicRevisionId());
        assertEquals(release.releaseId(), history.currentReleaseId());
        assertEquals(1, history.items().size());
        assertTrue(history.items().stream().anyMatch(item -> item.revisionId().equals(fixture.companyRevisionId())
                && item.currentContent() && !item.currentPublic()));
    }

    @Test
    void historyShouldSeparateCurrentContentAndCurrentPublicForProduct() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        ShowroomMaterializedRelease release = publishScopedRelease(fixture.publishedAt());
        insertProductBundle(fixture.productId(), fixture.productRevisionId(), null);

        mockFile(203L, 11L, "showroom/product/preview-v2.png", "image/png", "product-preview-v2");
        mockFile(206L, 12L, "showroom/audio/product-v2-zh.mp3", "audio/mpeg", "product-v2-audio-zh");
        mockFile(207L, 12L, "showroom/audio/product-v2-en.mp3", "audio/mpeg", "product-v2-audio-en");
        ShowroomProductRevision latestRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(fixture.productId(), "P-101", "导丝系统二代", "Guidewire System V2",
                        Map.of("target_market", "冠脉介入二代",
                                "target_market_en", "Coronary intervention v2",
                                "core_selling_points", "更顺滑二代",
                                "core_selling_points_en", "Smoother v2",
                                "cover_image", "/admin-api/infra/file/11/get/showroom/product/%E4%BA%A7%E5%93%81%E5%B0%81%E9%9D%A2%E5%9B%BE.png"))).revisionId(), 903L);
        publishProductMedia(latestRevision, 203L, 206L, 207L);
        insertProductBundle(fixture.productId(), latestRevision.revisionId(), fixture.productRevisionId(),
                fixture.publishedAt().plusSeconds(120));

        ShowroomVersionCenterHistoryRespVO history = versionCenterService.getHistory("PRODUCT", fixture.productId(),
                SITE_KEY, STAGE);

        assertEquals(latestRevision.revisionId(), history.currentContentRevisionId());
        assertEquals(fixture.productRevisionId(), history.currentPublicRevisionId());
        assertEquals(release.releaseId(), history.currentReleaseId());
        assertEquals(2, history.items().size());
        assertTrue(history.items().stream().anyMatch(item -> item.revisionId().equals(latestRevision.revisionId())
                && item.currentContent() && !item.currentPublic()));
        assertTrue(history.items().stream().anyMatch(item -> item.revisionId().equals(fixture.productRevisionId())
                && !item.currentContent() && item.currentPublic()));
    }

    @Test
    void historyShouldFailFastWhenPublishedRevisionIsMissingReadableBundle() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());
        insertProductBundle(fixture.productId(), fixture.productRevisionId(), null);

        mockFile(213L, 11L, "showroom/product/preview-v3.png", "image/png", "product-preview-v3");
        mockFile(216L, 12L, "showroom/audio/product-v3-zh.mp3", "audio/mpeg", "product-v3-audio-zh");
        mockFile(217L, 12L, "showroom/audio/product-v3-en.mp3", "audio/mpeg", "product-v3-audio-en");
        ShowroomProductRevision newerRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(fixture.productId(), "P-101", "导丝系统三代", "Guidewire System V3",
                        Map.of("target_market", "冠脉介入三代",
                                "target_market_en", "Coronary intervention v3",
                                "core_selling_points", "更顺滑三代",
                                "core_selling_points_en", "Smoother v3",
                                "cover_image", "/admin-api/infra/file/11/get/showroom/product/%E4%BA%A7%E5%93%81%E5%B0%81%E9%9D%A2%E5%9B%BE.png"))).revisionId(), 904L);
        publishProductMedia(newerRevision, 213L, 216L, 217L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> versionCenterService.getHistory("PRODUCT", fixture.productId(), SITE_KEY, STAGE));

        assertTrue(exception.getMessage().contains("SHOWROOM_VERSION_CENTER_NOT_READY"));
        assertTrue(exception.getMessage().contains(String.valueOf(newerRevision.revisionId())));
    }

    @Test
    void historyShouldIgnoreOlderPublishedRevisionWithoutBundleWhenCurrentAndPublicAreReadable() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        ShowroomMaterializedRelease release = publishScopedRelease(fixture.publishedAt());
        insertProductBundle(fixture.productId(), fixture.productRevisionId(), null);

        ShowroomProductRevision legacyMissingBundleRevision = contentService.publishProductRevision(
                contentService.saveProductDraft(
                        new ShowroomProductDraft(fixture.productId(), "P-101", "导丝系统旧迁移版",
                                "Guidewire Legacy Migrated",
                                Map.of("target_market", "旧迁移市场",
                                        "target_market_en", "Legacy migrated market",
                                        "core_selling_points", "旧迁移卖点",
                                        "core_selling_points_en", "Legacy migrated point",
                                        "cover_image",
                                        "/admin-api/infra/file/11/get/showroom/product/%E4%BA%A7%E5%93%81%E5%B0%81%E9%9D%A2%E5%9B%BE.png")))
                        .revisionId(),
                904L);

        mockFile(223L, 11L, "showroom/product/preview-v4.png", "image/png", "product-preview-v4");
        mockFile(226L, 12L, "showroom/audio/product-v4-zh.mp3", "audio/mpeg", "product-v4-audio-zh");
        mockFile(227L, 12L, "showroom/audio/product-v4-en.mp3", "audio/mpeg", "product-v4-audio-en");
        ShowroomProductRevision currentReadableRevision = contentService.publishProductRevision(
                contentService.saveProductDraft(
                        new ShowroomProductDraft(fixture.productId(), "P-101", "导丝系统四代",
                                "Guidewire System V4",
                                Map.of("target_market", "冠脉介入四代",
                                        "target_market_en", "Coronary intervention v4",
                                        "core_selling_points", "更顺滑四代",
                                        "core_selling_points_en", "Smoother v4",
                                        "cover_image",
                                        "/admin-api/infra/file/11/get/showroom/product/%E4%BA%A7%E5%93%81%E5%B0%81%E9%9D%A2%E5%9B%BE.png")))
                        .revisionId(),
                905L);
        publishProductMedia(currentReadableRevision, 223L, 226L, 227L);
        insertProductBundle(fixture.productId(), currentReadableRevision.revisionId(),
                legacyMissingBundleRevision.revisionId(), fixture.publishedAt().plusSeconds(180));

        ShowroomVersionCenterHistoryRespVO history = versionCenterService.getHistory("PRODUCT", fixture.productId(),
                SITE_KEY, STAGE);

        assertEquals(currentReadableRevision.revisionId(), history.currentContentRevisionId());
        assertEquals(fixture.productRevisionId(), history.currentPublicRevisionId());
        assertEquals(release.releaseId(), history.currentReleaseId());
        assertEquals(2, history.items().size());
        assertTrue(history.items().stream()
                .anyMatch(item -> item.revisionId().equals(currentReadableRevision.revisionId())
                        && item.currentContent() && !item.currentPublic()));
        assertTrue(history.items().stream()
                .anyMatch(item -> item.revisionId().equals(fixture.productRevisionId())
                        && !item.currentContent() && item.currentPublic()));
        assertFalse(history.items().stream()
                .anyMatch(item -> item.revisionId().equals(legacyMissingBundleRevision.revisionId())));
    }

    @Test
    void historyShouldReportMissingRevisionIdsWhenNoReadableBundleExists() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> versionCenterService.getHistory("PRODUCT", fixture.productId(), SITE_KEY, STAGE));

        assertTrue(exception.getMessage().contains("SHOWROOM_VERSION_CENTER_NOT_READY"));
        assertTrue(exception.getMessage().contains(String.valueOf(fixture.productRevisionId())));
    }

    @Test
    void productBundleShouldUseCoverWithoutReleasePreviewAsset() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();

        ShowroomVersionBundleDO bundle = versionBundleService.ensureBundleForPublishedRevision(
                "PRODUCT", fixture.productId(), fixture.productRevisionId(), 901L, null);

        assertEquals(null, bundle.getReleasePreviewAssetVersionId());
    }

    @Test
    void productHistoryAndDetailShouldKeepFieldDiffsWhenCoverAndNarrationMaterialsAreMissing() {
        bindDefaultSiteStage();
        ShowroomProductRevision selected = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-MATRIX", "矩阵产品一代", "Matrix Product V1",
                        Map.of("owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "一代市场",
                                "target_market_en", "Market V1"))).revisionId(), 901L);
        insertProductCoreBundle(selected.productId(), selected.revisionId(), null,
                Instant.parse("2026-05-23T10:15:00Z"));
        ShowroomProductRevision current = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(selected.productId(), "P-MATRIX", "矩阵产品二代", "Matrix Product V2",
                        Map.of("owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "二代市场",
                                "target_market_en", "Market V2"))).revisionId(), 902L);
        insertProductCoreBundle(current.productId(), current.revisionId(), selected.revisionId(),
                Instant.parse("2026-05-23T10:16:00Z"));

        ShowroomVersionCenterHistoryRespVO history = versionCenterService.getHistory("PRODUCT", selected.productId(),
                SITE_KEY, STAGE);
        ShowroomVersionCenterHistoryRespVO.HistoryItemRespVO selectedHistory = history.items().stream()
                .filter(item -> item.revisionId().equals(selected.revisionId()))
                .findFirst()
                .orElseThrow();

        assertEquals(2, history.items().size());
        assertTrue(selectedHistory.blockers().stream()
                .anyMatch(blocker -> "PRODUCT_COVER_MISSING".equals(blocker.backendErrorCode())));
        assertTrue(selectedHistory.blockers().stream()
                .anyMatch(blocker -> "ZH".equals(blocker.language())
                        && "PRODUCT_NARRATION_AUDIO_MISSING".equals(blocker.backendErrorCode())));

        ShowroomVersionCenterDetailRespVO detail = versionCenterService.getDetail(
                "PRODUCT", selected.productId(), selected.revisionId(), SITE_KEY, STAGE);

        assertTrue(detail.fieldDiffs().stream()
                .anyMatch(diff -> "target_market".equals(diff.fieldCode()) && diff.changed()));
        assertTrue(detail.republishReadiness().blockers().stream()
                .anyMatch(blocker -> "PRODUCT_COVER_MISSING".equals(blocker.backendErrorCode())));
        assertTrue(detail.republishReadiness().blockers().stream()
                .anyMatch(blocker -> "EN".equals(blocker.language())
                        && "PRODUCT_NARRATION_AUDIO_MISSING".equals(blocker.backendErrorCode())));
    }

    @Test
    void detailShouldExposeCompanyPreviewBlockerWhenHistoricalPreviewLinkageIsUnproven() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());
        insertCompanyBundle(fixture.companyId(), fixture.companyRevisionId(), null, null);

        ShowroomVersionCenterDetailRespVO detail = versionCenterService.getDetail(
                "COMPANY", fixture.companyId(), fixture.companyRevisionId(), SITE_KEY, STAGE);

        assertEquals(fixture.companyRevisionId(), detail.selectedVersion().revisionId());
        assertEquals("/admin-api/infra/file/11/get/showroom/company/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg",
                detail.selectedVersion().image().contentImage().url());
        assertEquals(null, detail.selectedVersion().image().releasePreviewAsset());
        assertFalse(detail.republishReadiness().ready());
        assertTrue(detail.republishReadiness().blockers().stream()
                .anyMatch(blocker -> "PUBLIC_RELEASE".equals(blocker.scope())
                        || blocker.message().contains("preview linkage")));
    }

    @Test
    void detailShouldUseHistoricalCompanySnapshotForPreviewAlt() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());
        Long companyPreviewVersionId = previewAssetService.live(new ShowroomPreviewAssetKey(
                ShowroomPreviewAssetTargetType.COMPANY, fixture.companyId())).orElseThrow().id();
        insertCompanyBundle(fixture.companyId(), fixture.companyRevisionId(), companyPreviewVersionId, null);

        contentService.saveCompanyDraft(
                new cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft(
                        fixture.companyId(), "MAIN", "新的公司名", "New Company Name",
                        Map.of("development_history", "新历史",
                                "development_history_en", "New history",
                                "cover_image", "/admin-api/infra/file/11/get/showroom/company/home.png")));

        ShowroomVersionCenterDetailRespVO detail = versionCenterService.getDetail(
                "COMPANY", fixture.companyId(), fixture.companyRevisionId(), SITE_KEY, STAGE);

        assertEquals("盈泰医疗", detail.selectedVersion().title());
        assertEquals("盈泰医疗", detail.selectedVersion().image().releasePreviewAsset().alt());
    }

    @Test
    void detailShouldKeepSelectedCompanyReadableWhenCurrentContentBundleIsMissing() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());
        insertCompanyBundle(fixture.companyId(), fixture.companyRevisionId(), null, null);

        mockFile(214L, 12L, "showroom/audio/company-v2-zh.mp3", "audio/mpeg", "company-v2-audio-zh");
        mockFile(215L, 12L, "showroom/audio/company-v2-en.mp3", "audio/mpeg", "company-v2-audio-en");
        mockFile(216L, 12L, "showroom/audio/company-v2-zh-duplicate.mp3", "audio/mpeg",
                "company-v2-audio-zh-duplicate");
        mockFile(217L, 12L, "showroom/audio/company-v2-en-duplicate.mp3", "audio/mpeg",
                "company-v2-audio-en-duplicate");
        ShowroomCompanyRevision current = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft(
                        fixture.companyId(), "MAIN", "盈泰医疗二代", "Yingtai Medical V2",
                        Map.of("development_history", "二代发展历程",
                                "development_history_en", "Second history",
                                "park_introduction", "二代园区介绍",
                                "park_introduction_en", "Second park",
                                "cover_image",
                                "/admin-api/infra/file/11/get/showroom/company/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg"))).revisionId(), 904L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, fixture.companyId(), current.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司二代中文讲解", 214L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, fixture.companyId(), current.revisionId(),
                ShowroomNarrationLanguage.EN, "English company v2 narration", 215L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, fixture.companyId(), current.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司二代中文讲解重复候选", 216L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, fixture.companyId(), current.revisionId(),
                ShowroomNarrationLanguage.EN, "English company v2 narration duplicate", 217L);

        ShowroomVersionCenterDetailRespVO detail = versionCenterService.getDetail(
                "COMPANY", fixture.companyId(), fixture.companyRevisionId(), SITE_KEY, STAGE);

        assertEquals(fixture.companyRevisionId(), detail.selectedVersion().revisionId());
        assertEquals(current.revisionId(), detail.targetSummary().currentContentRevisionId());
        assertEquals(null, detail.currentContentVersion());
        assertTrue(detail.republishReadiness().blockers().stream()
                .anyMatch(blocker -> "CURRENT_CONTENT".equals(blocker.scope())
                        && blocker.affectedRevisionIds().contains(current.revisionId())
                        && blocker.message().contains("multiple published candidates")));
    }

    @Test
    void detailShouldKeepSelectedCompanyReadableWhenCurrentContentRevisionIsMissing() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());
        insertCompanyBundle(fixture.companyId(), fixture.companyRevisionId(), null, null);

        Long missingRevisionId = 999_001L;
        ShowroomCompanyDO company = companyMapper.selectById(fixture.companyId());
        company.setCurrentRevisionId(missingRevisionId);
        company.setCurrentRevisionNo(999);
        companyMapper.updateById(company);

        ShowroomVersionCenterDetailRespVO detail = versionCenterService.getDetail(
                "COMPANY", fixture.companyId(), fixture.companyRevisionId(), SITE_KEY, STAGE);

        assertEquals(fixture.companyRevisionId(), detail.selectedVersion().revisionId());
        assertEquals(missingRevisionId, detail.targetSummary().currentContentRevisionId());
        assertEquals(null, detail.currentContentVersion());
        assertTrue(detail.republishReadiness().blockers().stream()
                .anyMatch(blocker -> "CURRENT_CONTENT".equals(blocker.scope())
                        && "SHOWROOM_TARGET_NOT_FOUND".equals(blocker.backendErrorCode())
                        && blocker.affectedRevisionIds().contains(missingRevisionId)
                        && blocker.message().contains("company revision not found")));
    }

    @Test
    void detailShouldKeepSelectedCompanyReadableWhenCurrentPublicRevisionIsMissing() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        ShowroomMaterializedRelease release = publishScopedRelease(fixture.publishedAt());
        insertCompanyBundle(fixture.companyId(), fixture.companyRevisionId(), null, null);

        Long missingRevisionId = 999_021L;
        ShowroomReleaseSourceSnapshotDO snapshot = releaseSourceSnapshotMapper.selectByReleaseId(release.releaseId());
        snapshot.setCompanyRevisionId(missingRevisionId);
        releaseSourceSnapshotMapper.updateById(snapshot);

        ShowroomVersionCenterDetailRespVO detail = versionCenterService.getDetail(
                "COMPANY", fixture.companyId(), fixture.companyRevisionId(), SITE_KEY, STAGE);

        assertEquals(fixture.companyRevisionId(), detail.selectedVersion().revisionId());
        assertEquals(missingRevisionId, detail.targetSummary().currentPublicRevisionId());
        assertEquals(null, detail.currentPublicVersion());
        assertTrue(detail.republishReadiness().blockers().stream()
                .anyMatch(blocker -> "CURRENT_RELEASE".equals(blocker.scope())
                        && "SHOWROOM_TARGET_NOT_FOUND".equals(blocker.backendErrorCode())
                        && blocker.affectedRevisionIds().contains(missingRevisionId)
                        && blocker.message().contains("company revision not found")));
    }

    @Test
    void detailShouldReadSelectedCompanyFromCurrentLoginTenantWhenSiteBindingPointsElsewhere() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());
        insertCompanyBundle(fixture.companyId(), fixture.companyRevisionId(), null, null);

        var binding = siteBindingMapper.selectEnabledBySiteStage(SITE_KEY, STAGE);
        binding.setTenantId(122L);
        siteBindingMapper.updateById(binding);

        ShowroomVersionCenterDetailRespVO detail = versionCenterService.getDetail(
                "COMPANY", fixture.companyId(), fixture.companyRevisionId(), SITE_KEY, STAGE);

        assertEquals(fixture.companyRevisionId(), detail.selectedVersion().revisionId());
        assertEquals("盈泰医疗", detail.selectedVersion().title());
    }

    @Test
    void historyShouldReadCompanyFromCurrentLoginTenantWhenSiteBindingPointsElsewhere() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());
        insertCompanyBundle(fixture.companyId(), fixture.companyRevisionId(), null, null);

        var binding = siteBindingMapper.selectEnabledBySiteStage(SITE_KEY, STAGE);
        binding.setTenantId(122L);
        siteBindingMapper.updateById(binding);

        ShowroomVersionCenterHistoryRespVO history = versionCenterService.getHistory(
                "COMPANY", fixture.companyId(), SITE_KEY, STAGE);

        assertEquals(fixture.companyRevisionId(), history.currentContentRevisionId());
        assertEquals(1, history.items().size());
        assertEquals(fixture.companyRevisionId(), history.items().get(0).revisionId());
    }

    @Test
    void republishShouldCopyPublishedProductPackageAndSwitchCurrentRelease() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        ShowroomMaterializedRelease release = publishScopedRelease(fixture.publishedAt());
        insertProductBundle(fixture.productId(), fixture.productRevisionId(), null);

        ShowroomVersionCenterRepublishRespVO response = versionCenterService.republish(
                new ShowroomVersionCenterRepublishReqVO("PRODUCT", fixture.productId(), fixture.productRevisionId(),
                        SITE_KEY, STAGE),
                901L);

        assertEquals(fixture.productId(), response.targetId());
        assertEquals(fixture.productRevisionId(), response.sourceRevisionId());
        assertNotEquals(fixture.productRevisionId(), response.newRevisionId());
        assertNotEquals(release.releaseId(), response.releaseId());
        assertEquals(response.newRevisionId(), contentService.requireCurrentProductRevision(fixture.productId()).revisionId());
        ShowroomReleasePointerDO pointer = currentScopedPointer();
        assertEquals(response.releaseId(), pointer.getReleaseId());
        ShowroomVersionBundleDO newBundle = versionBundleMapper.selectByTargetAndRevision(
                "PRODUCT", fixture.productId(), response.newRevisionId());
        assertEquals(fixture.productRevisionId(), newBundle.getCopiedFromRevisionId());
        assertEquals(response.newRevisionNo(), newBundle.getRevisionNo());
        assertEquals(null, newBundle.getReleasePreviewAssetVersionId());
    }

    @Test
    void republishShouldRepairSelectedTargetEvenWhenCurrentTargetStateIsBroken() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());
        insertProductBundle(fixture.productId(), fixture.productRevisionId(), null);

        contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(fixture.productId(), "P-101", "导丝系统损坏版", "Guidewire Broken Current",
                        Map.of("target_market", "损坏版本市场",
                                "target_market_en", "Broken market"))).revisionId(), 906L);

        ShowroomVersionCenterRepublishRespVO response = versionCenterService.republish(
                new ShowroomVersionCenterRepublishReqVO("PRODUCT", fixture.productId(), fixture.productRevisionId(),
                        SITE_KEY, STAGE),
                901L);

        assertEquals(response.newRevisionId(), contentService.requireCurrentProductRevision(fixture.productId()).revisionId());
    }

    @Test
    void republishShouldFailFastWhenAnotherMappedLiveSourceIsBroken() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        publishScopedRelease(fixture.publishedAt());
        insertProductBundle(fixture.productId(), fixture.productRevisionId(), null);
        String originalReleaseId = currentScopedPointer().getReleaseId();

        ShowroomProductRevision blocked = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-BLOCKER", "阻断产品", "Blocking Product",
                        Map.of("target_market", "阻断市场", "target_market_en", "Blocking Market"))).revisionId(), 904L);
        contentService.replaceHallCanvasLayout(fixture.hallId(), List.of(
                new ShowroomHallProductMapping(fixture.productId(), 1,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, new java.math.BigDecimal("0.5"),
                        java.math.BigDecimal.ONE),
                new ShowroomHallProductMapping(blocked.productId(), 2,
                        new java.math.BigDecimal("0.5"), java.math.BigDecimal.ZERO, new java.math.BigDecimal("0.5"),
                        java.math.BigDecimal.ONE)
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> versionCenterService.republish(
                        new ShowroomVersionCenterRepublishReqVO("PRODUCT", fixture.productId(),
                                fixture.productRevisionId(), SITE_KEY, STAGE),
                        901L));

        assertTrue(exception.getMessage().contains("SHOWROOM_VERSION_REPUBLISH_PUBLIC_RELEASE_BLOCKED"));
        assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_PRODUCT_BLOCKED"));
        assertTrue(exception.getMessage().contains("P-BLOCKER"));
        assertEquals(originalReleaseId, currentScopedPointer().getReleaseId());
    }

    private ShowroomMaterializedRelease publishScopedRelease(Instant publishedAt) throws Exception {
        bindDefaultSiteStage();
        return publisherService.publishRelease(900L, publishedAt, SITE_KEY, STAGE);
    }

    private ShowroomReleasePointerDO currentScopedPointer() {
        return releasePointerMapper.selectByPointerScope(1L, SITE_KEY, STAGE, ShowroomReleaseConstants.POINTER_KEY);
    }

    private void insertProductBundle(Long productId, Long revisionId, Long copiedFromRevisionId) {
        insertProductBundle(productId, revisionId, copiedFromRevisionId, Instant.parse("2026-05-23T10:15:00Z"));
    }

    private void insertProductBundle(Long productId, Long revisionId, Long copiedFromRevisionId, Instant publishedAt) {
        ShowroomProductRevision revision = contentService.getProductRevision(revisionId);
        Long zhNarrationId = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, productId, ShowroomNarrationAudienceType.PUBLIC,
                ShowroomNarrationLanguage.ZH)).orElseThrow().id();
        Long enNarrationId = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, productId, ShowroomNarrationAudienceType.PUBLIC,
                ShowroomNarrationLanguage.EN)).orElseThrow().id();
        ShowroomVersionBundleDO bundle = ShowroomVersionBundleDO.builder()
                .targetType("PRODUCT")
                .targetId(productId)
                .revisionId(revisionId)
                .revisionNo(revision.revisionNo())
                .releasePreviewAssetVersionId(null)
                .narrationZhVersionId(zhNarrationId)
                .narrationEnVersionId(enNarrationId)
                .copiedFromRevisionId(copiedFromRevisionId)
                .publishedBy(901L)
                .publishedAt(LocalDateTime.ofInstant(publishedAt, java.time.ZoneOffset.UTC))
                .build();
        bundle.setTenantId(TenantContextHolder.getRequiredTenantId());
        versionBundleMapper.insert(bundle);
    }

    private void insertProductCoreBundle(Long productId, Long revisionId, Long copiedFromRevisionId,
                                         Instant publishedAt) {
        ShowroomProductRevision revision = contentService.getProductRevision(revisionId);
        ShowroomVersionBundleDO bundle = ShowroomVersionBundleDO.builder()
                .targetType("PRODUCT")
                .targetId(productId)
                .revisionId(revisionId)
                .revisionNo(revision.revisionNo())
                .releasePreviewAssetVersionId(null)
                .narrationZhVersionId(null)
                .narrationEnVersionId(null)
                .copiedFromRevisionId(copiedFromRevisionId)
                .publishedBy(901L)
                .publishedAt(LocalDateTime.ofInstant(publishedAt, java.time.ZoneOffset.UTC))
                .build();
        bundle.setTenantId(TenantContextHolder.getRequiredTenantId());
        versionBundleMapper.insert(bundle);
    }

    private void insertCompanyBundle(Long companyId, Long revisionId, Long previewVersionId, Long copiedFromRevisionId) {
        Long zhNarrationId = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.COMPANY, companyId, ShowroomNarrationAudienceType.PUBLIC,
                ShowroomNarrationLanguage.ZH)).orElseThrow().id();
        Long enNarrationId = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.COMPANY, companyId, ShowroomNarrationAudienceType.PUBLIC,
                ShowroomNarrationLanguage.EN)).orElseThrow().id();
        ShowroomVersionBundleDO bundle = ShowroomVersionBundleDO.builder()
                .targetType("COMPANY")
                .targetId(companyId)
                .revisionId(revisionId)
                .revisionNo(contentService.getCompanyRevision(revisionId).revisionNo())
                .releasePreviewAssetVersionId(previewVersionId)
                .narrationZhVersionId(zhNarrationId)
                .narrationEnVersionId(enNarrationId)
                .copiedFromRevisionId(copiedFromRevisionId)
                .publishedBy(901L)
                .publishedAt(LocalDateTime.ofInstant(Instant.parse("2026-05-23T10:15:00Z"), java.time.ZoneOffset.UTC))
                .build();
        bundle.setTenantId(TenantContextHolder.getRequiredTenantId());
        versionBundleMapper.insert(bundle);
    }

    private void publishProductMedia(ShowroomProductRevision revision, Long previewFileId, Long zhAudioFileId,
                                     Long enAudioFileId) {
        previewAssetService.publishDirectly(previewAssetService.bindStaticPreviewAssets(
                new ShowroomPreviewAssetDraftCommand(
                        ShowroomPreviewAssetTargetType.PRODUCT,
                        revision.productId(),
                        revision.revisionId(),
                        new ShowroomPreviewAssetFiles(previewFileId, previewFileId, previewFileId))).id());
        var zhDraft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, revision.productId(), revision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH, "产品二代中文讲解", false));
        narrationService.publishDirectly(narrationService.attachAudio(
                new ShowroomNarrationAudioDraftCommand(zhDraft.id(), zhAudioFileId, 61, "ruoxi")).id());
        var enDraft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, revision.productId(), revision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN, "English product v2 narration", false));
        narrationService.publishDirectly(narrationService.attachAudio(
                new ShowroomNarrationAudioDraftCommand(enDraft.id(), enAudioFileId, 62, "ruoxi")).id());
    }
}
