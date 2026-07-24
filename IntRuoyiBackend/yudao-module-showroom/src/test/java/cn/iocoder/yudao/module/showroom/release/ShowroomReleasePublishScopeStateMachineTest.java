package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomPublicSiteBindingMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseLegacyProjectionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseSourceSnapshotMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@Import({
        ShowroomApiRuntime.class,
        ShowroomReleaseAutoPublishService.class
})
class ShowroomReleasePublishScopeStateMachineTest extends AbstractShowroomReleaseDbTest {

    private static final Long TENANT_ID = 1L;
    private static final String SITE_KEY = "yingtai-showroom";

    @Resource
    private ShowroomApiRuntime runtime;
    @Resource
    private ShowroomPublicSiteBindingMapper siteBindingMapper;
    @Resource
    private ShowroomReleaseSourceSnapshotMapper sourceSnapshotMapper;
    @Resource
    private ShowroomReleaseLegacyProjectionMapper legacyProjectionMapper;

    @MockBean
    private ShowroomProductCommentService commentService;
    @MockBean
    private ShowroomProductCoverBatchTaskService productCoverBatchTaskService;
    @MockBean
    private ShowroomProductCoverImageService productCoverImageService;
    @MockBean
    private ShowroomCompanyNarrationCodexService narrationCodexService;
    @MockBean
    private ShowroomCompanyNarrationTranslationService narrationTranslationService;
    @MockBean
    private ShowroomProductNarrationCodexService productNarrationCodexService;
    @MockBean
    private ShowroomImagePromptVersionService imagePromptVersionService;
    @MockBean
    private ShowroomVersionBundleService versionBundleService;
    @MockBean
    private ShowroomProductRevisionRelationMapper productRevisionRelationMapper;
    @MockBean
    private ShowroomChangeRequestMapper changeRequestMapper;
    @MockBean
    private ShowroomAssignmentService assignmentService;
    @MockBean
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    @MockBean
    private YudaoAiProperties yudaoAiProperties;
    @MockBean
    private ConfigService configService;

    @Test
    void releasePublishRequestShouldFailFastWithoutExplicitSiteStage() {
        assertThrows(RuntimeException.class,
                () -> runtime.publishRelease(new ShowroomAdminController.ReleasePublishReqVO("", "TEST"), 900L));
        assertThrows(RuntimeException.class,
                () -> runtime.publishRelease(new ShowroomAdminController.ReleasePublishReqVO(SITE_KEY, ""), 900L));
        assertThrows(RuntimeException.class,
                () -> runtime.publishRelease(new ShowroomAdminController.ReleasePublishReqVO(SITE_KEY, "STAGING"), 900L));
    }

    @Test
    void scopedReleasePublishShouldWriteScopeAndKeepTestProdPointersIsolated() throws Exception {
        bindSite("TEST");
        bindSite("PROD");
        seedPublishedFixture();

        ShowroomMaterializedRelease testRelease = publisherService.publishRelease(900L,
                Instant.parse("2026-05-23T10:15:00Z"), SITE_KEY, "TEST");
        ShowroomMaterializedRelease prodRelease = publisherService.publishRelease(900L,
                Instant.parse("2026-05-23T10:16:00Z"), SITE_KEY, "PROD");

        assertEquals(testRelease.releaseId(), scopedPointer("TEST").getReleaseId());
        assertEquals(prodRelease.releaseId(), scopedPointer("PROD").getReleaseId());
        assertReleaseScopeWritten(testRelease.releaseId(), "TEST");
        assertReleaseScopeWritten(prodRelease.releaseId(), "PROD");
    }

    @Test
    void scopedReleaseIdShouldIncludeSiteStageWhenSnapshotAndTimestampMatch() throws Exception {
        String alternateSiteKey = "yingtai-showroom-alt";
        bindSite(SITE_KEY, "TEST");
        bindSite(alternateSiteKey, "TEST");
        seedPublishedFixture();
        Instant publishedAt = Instant.parse("2026-05-23T10:15:00Z");

        ShowroomMaterializedRelease primaryRelease = publisherService.publishRelease(900L,
                publishedAt, SITE_KEY, "TEST");
        ShowroomMaterializedRelease alternateRelease = publisherService.publishRelease(900L,
                publishedAt, alternateSiteKey, "TEST");

        assertNotEquals(primaryRelease.releaseId(), alternateRelease.releaseId());
        assertEquals(primaryRelease.releaseId(), releasePointerMapper.selectByPointerScope(
                TENANT_ID, SITE_KEY, "TEST", ShowroomReleaseConstants.POINTER_KEY).getReleaseId());
        assertEquals(alternateRelease.releaseId(), releasePointerMapper.selectByPointerScope(
                TENANT_ID, alternateSiteKey, "TEST", ShowroomReleaseConstants.POINTER_KEY).getReleaseId());
    }

    @Test
    void failedScopedReleasePublishShouldKeepOldPointer() throws Exception {
        bindSite("TEST");
        PublishedFixture fixture = seedPublishedFixture();
        ShowroomMaterializedRelease firstRelease = publisherService.publishRelease(900L,
                Instant.parse("2026-05-23T10:15:00Z"), SITE_KEY, "TEST");

        mockFile(108L, 11L, "showroom/product/preview-only.png", "image/png", "product-preview-only");
        mockFile(109L, 12L, "showroom/audio/product-preview-only-zh.mp3", "audio/mpeg", "product-preview-only-zh");
        mockFile(110L, 12L, "showroom/audio/product-preview-only-en.mp3", "audio/mpeg", "product-preview-only-en");
        ShowroomProductRevision previewOnlyProduct = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-102", "输送导管", "Delivery Catheter",
                        Map.of(
                                "target_market", "外周介入",
                                "target_market_en", "Peripheral intervention",
                                "core_selling_points", "更稳定",
                                "core_selling_points_en", "More stable"))).revisionId(), 903L);
        contentService.replaceHallCanvasLayout(fixture.hallId(), List.of(
                new ShowroomHallProductMapping(fixture.productId(), 1,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, new java.math.BigDecimal("0.5"),
                        java.math.BigDecimal.ONE),
                new ShowroomHallProductMapping(previewOnlyProduct.productId(), 2,
                        new java.math.BigDecimal("0.5"), java.math.BigDecimal.ZERO, new java.math.BigDecimal("0.5"),
                        java.math.BigDecimal.ONE)));
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, previewOnlyProduct.productId(),
                previewOnlyProduct.revisionId(), 108L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, previewOnlyProduct.productId(),
                previewOnlyProduct.revisionId(), ShowroomNarrationLanguage.ZH, "预览图产品中文讲解", 109L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, previewOnlyProduct.productId(),
                previewOnlyProduct.revisionId(), ShowroomNarrationLanguage.EN, "Preview-only product narration", 110L);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> publisherService.publishRelease(900L, Instant.parse("2026-05-23T10:16:00Z"), SITE_KEY, "TEST"));

        assertTrue(failure.getMessage().contains("product cover_image is required"));
        assertEquals(firstRelease.releaseId(), scopedPointer("TEST").getReleaseId());
    }

    @Test
    void companyPublishShouldOnlyDirtyBackendUntilScopedReleasePublishSwitchesPointer() throws Exception {
        bindSite("TEST");
        PublishedFixture fixture = seedPublishedFixture();
        ShowroomMaterializedRelease firstRelease = publisherService.publishRelease(900L,
                Instant.parse("2026-05-23T10:15:00Z"), SITE_KEY, "TEST");

        runtime.publishCompany(new ShowroomAdminController.CompanyDraftReqVO(
                fixture.companyId(),
                "MAIN",
                "盈泰医疗更新",
                "Yingtai Medical Updated",
                Map.of(
                        "development_history", "更新后的发展历程",
                        "development_history_en", "Updated history",
                        "park_introduction", "更新后的园区介绍",
                        "park_introduction_en", "Updated park",
                        "cover_image",
                        "/admin-api/infra/file/11/get/showroom/company/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg")),
                901L);

        assertEquals(firstRelease.releaseId(), scopedPointer("TEST").getReleaseId());
        verify(configService, atLeastOnce()).createConfig(argThat(dirtyState("COMPANY_REVISION_PUBLISHED")));

        ShowroomMaterializedRelease secondRelease = publisherService.publishRelease(900L,
                Instant.parse("2026-05-23T10:16:00Z"), SITE_KEY, "TEST");

        assertEquals(secondRelease.releaseId(), scopedPointer("TEST").getReleaseId());
    }

    private void bindSite(String stage) {
        bindSite(SITE_KEY, stage);
    }

    private void bindSite(String siteKey, String stage) {
        if (siteBindingMapper.selectEnabledBySiteStage(siteKey, stage) != null) {
            return;
        }
        siteBindingMapper.insert(ShowroomPublicSiteBindingDO.builder()
                .siteKey(siteKey)
                .stage(stage)
                .tenantId(TENANT_ID)
                .displayName("Yingtai " + stage)
                .enabled(true)
                .build());
    }

    private cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleasePointerDO scopedPointer(String stage) {
        return releasePointerMapper.selectByPointerScope(TENANT_ID, SITE_KEY, stage,
                ShowroomReleaseConstants.POINTER_KEY);
    }

    private void assertReleaseScopeWritten(String releaseId, String stage) {
        assertEquals(TENANT_ID, releaseMapper.selectByReleaseId(releaseId).getTenantId());
        assertEquals(SITE_KEY, releaseMapper.selectByReleaseId(releaseId).getSiteKey());
        assertEquals(stage, releaseMapper.selectByReleaseId(releaseId).getStage());
        assertEquals(TENANT_ID, sourceSnapshotMapper.selectByReleaseId(releaseId).getTenantId());
        assertEquals(SITE_KEY, sourceSnapshotMapper.selectByReleaseId(releaseId).getSiteKey());
        assertEquals(stage, sourceSnapshotMapper.selectByReleaseId(releaseId).getStage());
        assertEquals(TENANT_ID, releaseDocumentMapper.selectListByReleaseId(releaseId).get(0).getTenantId());
        assertEquals(SITE_KEY, releaseDocumentMapper.selectListByReleaseId(releaseId).get(0).getSiteKey());
        assertEquals(stage, releaseDocumentMapper.selectListByReleaseId(releaseId).get(0).getStage());
        assertEquals(TENANT_ID, releaseAssetRefMapper.selectListByReleaseId(releaseId).get(0).getTenantId());
        assertEquals(SITE_KEY, releaseAssetRefMapper.selectListByReleaseId(releaseId).get(0).getSiteKey());
        assertEquals(stage, releaseAssetRefMapper.selectListByReleaseId(releaseId).get(0).getStage());
        assertEquals(TENANT_ID, legacyProjectionMapper.selectByReleaseId(releaseId).getTenantId());
        assertEquals(SITE_KEY, legacyProjectionMapper.selectByReleaseId(releaseId).getSiteKey());
        assertEquals(stage, legacyProjectionMapper.selectByReleaseId(releaseId).getStage());
        assertEquals(TENANT_ID, scopedPointer(stage).getTenantId());
        assertEquals(SITE_KEY, scopedPointer(stage).getSiteKey());
        assertEquals(stage, scopedPointer(stage).getStage());
        var assetRef = releaseAssetRefMapper.selectListByReleaseId(releaseId).get(0);
        var asset = releaseAssetMapper.selectByScopeAssetIdAndContentHash(TENANT_ID, SITE_KEY, stage,
                assetRef.getAssetId(), assetRef.getContentHash());
        assertEquals(TENANT_ID, asset.getTenantId());
        assertEquals(SITE_KEY, asset.getSiteKey());
        assertEquals(stage, asset.getStage());
    }

    private static ArgumentMatcher<ConfigSaveReqVO> dirtyState(String reason) {
        return req -> ShowroomReleaseAutoPublishService.AUTO_PUBLISH_STATE_KEY.equals(req.getKey())
                && req.getValue() != null
                && req.getValue().contains("\"dirty\":true")
                && req.getValue().contains("\"lastDirtyReason\":\"" + reason + "\"");
    }
}
