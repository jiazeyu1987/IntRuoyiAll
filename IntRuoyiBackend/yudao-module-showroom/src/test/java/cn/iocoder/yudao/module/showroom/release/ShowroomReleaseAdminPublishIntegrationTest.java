package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.configpackage.ShowroomHallConfigPackageService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardPublishReqVO;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomDisplayController;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalActorResolver;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.module.showroom.release.ShowroomReleaseAutoPublishService.AUTO_PUBLISH_STATE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        ShowroomAdminController.class,
        ShowroomApiRuntime.class,
        ShowroomDisplayController.class,
        ShowroomReleaseAutoPublishService.class
})
class ShowroomReleaseAdminPublishIntegrationTest extends AbstractShowroomReleaseDbTest {

    private static final Long TEST_TENANT_ID = 122L;

    @Resource
    private ShowroomAdminController adminController;
    @Resource
    private ShowroomLegacyWebsiteConfigProjector legacyProjector;
    @Resource
    private ShowroomDisplayController displayController;

    @MockBean
    private ShowroomWorkflowFacade workflowFacade;
    @MockBean
    private ShowroomApprovalActorResolver approvalActorResolver;
    @MockBean
    private ShowroomAssignmentService assignmentService;
    @MockBean
    private ShowroomProductCommentService commentService;
    @MockBean
    private SecurityFrameworkService securityFrameworkService;
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
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    @MockBean
    private YudaoAiProperties yudaoAiProperties;
    @MockBean
    private ConfigService configService;
    @MockBean
    private ShowroomHallConfigPackageService hallConfigPackageService;
    @MockBean
    private ShowroomPublicReleaseReadbackVerifier publicReleaseReadbackVerifier;

    @BeforeEach
    void setUpSecurityRoles() {
        when(securityFrameworkService.hasRole(anyString())).thenAnswer(invocation -> {
            String roleCode = invocation.getArgument(0);
            return ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE.equals(roleCode);
        });
    }

    @Test
    void publishReleaseEndpointShouldSwitchCurrentPointerAndExposeReadableContracts() throws Exception {
        seedPublishedFixture();
        bindDefaultSiteStage();

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            var publish = adminController.publishRelease(scopedPublishReq());

            assertEquals(0, publish.getCode());
            assertNotNull(publish.getData());
            assertNotNull(publish.getData().releaseId());
            assertNotNull(publish.getData().manifestHash());
            verify(publicReleaseReadbackVerifier).verify(eq(DEFAULT_SITE_KEY), eq(DEFAULT_STAGE),
                    eq(publish.getData().releaseId()), eq(publish.getData().manifestHash()),
                    eq(publish.getData().rootDocumentId()));

            var current = manifestQueryService.getCurrentResponse(DEFAULT_SITE_KEY, DEFAULT_STAGE, new HttpHeaders());
            assertEquals(HttpStatus.OK, current.getStatusCode());
            assertTrue(current.getBody().contains("\"releaseId\":\"" + publish.getData().releaseId() + "\""));

            var websiteConfig = legacyProjector.projectCurrentPayload(defaultReleaseScope());
            assertEquals(0, websiteConfig.getCode());
            assertEquals("盈泰医疗", websiteConfig.getData().company().name());
            assertEquals(1, websiteConfig.getData().showrooms().size());

            var displayWebsiteConfig = displayController.getWebsiteConfig(DEFAULT_SITE_KEY, DEFAULT_STAGE);
            assertEquals(0, displayWebsiteConfig.getCode());
            assertTrue(displayWebsiteConfig.getData().company().homeImageUrl().startsWith("/showroom/sites/"));
        }
    }

    @Test
    void hallUpdateShouldMarkReleaseDirtyForAutoPublish() throws Exception {
        var fixture = seedPublishedFixture();
        reset(configService);

        contentService.updateHall(fixture.hallId(), "神经介植入展厅", "Neuro Intervention Hall", "主分支联调", "Main branch");

        verify(configService).createConfig(argThat(req ->
                AUTO_PUBLISH_STATE_KEY.equals(req.getKey())
                        && req.getValue() != null
                        && req.getValue().contains("\"dirty\":true")
                        && req.getValue().contains("\"lastDirtyReason\":\"HALL_UPDATED\"")));
    }

    @Test
    void publishReleaseShouldFailFastWhenMappedProductHasNoLiveRevision() throws Exception {
        var fixture = seedPublishedFixture();
        bindDefaultSiteStage();
        var invalidProduct = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-SKIP-001", "缺少发布版本产品", "Product Without Live Revision",
                java.util.Map.of("target_market", "must-block")));

        contentService.replaceHallCanvasLayout(fixture.hallId(), java.util.List.of(
                new ShowroomHallProductMapping(fixture.productId(), 1,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, new java.math.BigDecimal("0.5"),
                        java.math.BigDecimal.ONE),
                new ShowroomHallProductMapping(invalidProduct.productId(), 2,
                        new java.math.BigDecimal("0.5"), java.math.BigDecimal.ZERO, new java.math.BigDecimal("0.5"),
                        java.math.BigDecimal.ONE)
        ));
        var invalidOnlyHall = contentService.createHall("HALL_SKIP_ONLY", "仅坏产品展厅", "Invalid Only Hall",
                "仅坏产品展厅说明", "Invalid only hall summary");
        contentService.replaceHallCanvasLayout(invalidOnlyHall.hallId(), java.util.List.of(
                new ShowroomHallProductMapping(invalidProduct.productId(), 1,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ONE,
                        java.math.BigDecimal.ONE)
        ));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> adminController.publishRelease(scopedPublishReq()));

            assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_PRODUCT_BLOCKED"));
            assertTrue(exception.getMessage().contains("P-SKIP-001"));
            assertTrue(exception.getMessage().contains("live product revision not found"));
        }
    }

    @Test
    void publishAwardEndpointShouldFailFastWhenAwardNarrationAudioMissing() {
        ShowroomAwardRevision draft = contentService.saveAwardDraft(new ShowroomAwardDraft(
                null, "AWARD-API-001", "社会贡献奖", "Social Contribution Award",
                "中文讲解", "English narration", "嘉定区江桥镇人民政府", "2022年度",
                "/admin-api/infra/file/11/get/showroom/award/social-award.png"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> adminController.publishAward(new AwardPublishReqVO(draft.awardId(), draft.revisionId(),
                            draft.awardCode(), draft.nameCn(), draft.nameEn(), draft.fields().get("description_zh"),
                            draft.fields().get("description_en"), draft.fields().get("issuer"),
                            draft.fields().get("award_date_text"), draft.fields().get("cover_image"))));

            assertTrue(exception.getMessage().contains("AWARD_NARRATION_ZH_MISSING"));
        }
    }

    @Test
    void publishAwardEndpointShouldPublishTheNarrationBackedDraftRevisionWithoutCreatingAnotherRevision() {
        ShowroomAwardRevision draft = contentService.saveAwardDraft(new ShowroomAwardDraft(
                null, "AWARD-API-002", "质量奖", "Quality Award",
                "质量奖中文讲解", "Quality award English narration", "行业协会", "2026",
                "/admin-api/infra/file/11/get/showroom/award/quality-award.png"));
        publishNarration(ShowroomNarrationTargetType.AWARD, draft.awardId(), draft.revisionId(),
                ShowroomNarrationLanguage.ZH, draft.fields().get("description_zh"), 2301L);
        publishNarration(ShowroomNarrationTargetType.AWARD, draft.awardId(), draft.revisionId(),
                ShowroomNarrationLanguage.EN, draft.fields().get("description_en"), 2302L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            var result = adminController.publishAward(new AwardPublishReqVO(draft.awardId(), draft.revisionId(),
                    draft.awardCode(), draft.nameCn(), draft.nameEn(), draft.fields().get("description_zh"),
                    draft.fields().get("description_en"), draft.fields().get("issuer"),
                    draft.fields().get("award_date_text"), draft.fields().get("cover_image")));

            assertEquals(0, result.getCode());
            assertEquals(draft.revisionId(), result.getData().revisionId());
            assertEquals(draft.revisionId(), contentService.requireCurrentAwardRevision(draft.awardId()).revisionId());
        }
    }

    @Test
    void publishReleaseShouldExposeAwardDocumentFieldsRequiredByWebsiteRuntime() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        mockFile(110L, 11L, "showroom/award/social-award.png", "image/png", "award-cover");
        mockFile(111L, 12L, "showroom/audio/award-zh.mp3", "audio/mpeg", "award-audio-zh");
        mockFile(112L, 12L, "showroom/audio/award-en.mp3", "audio/mpeg", "award-audio-en");
        mockFile(113L, 11L, "showroom/hall/company-honor.png", "image/png", "company-honor-preview");
        mockFile(114L, 12L, "showroom/audio/company-honor-zh.mp3", "audio/mpeg", "company-honor-audio-zh");
        mockFile(115L, 12L, "showroom/audio/company-honor-en.mp3", "audio/mpeg", "company-honor-audio-en");
        mockFile(116L, 11L, "showroom/award/social-award-2.png", "image/png", "award-cover-2");
        mockFile(117L, 12L, "showroom/audio/award-zh-2.mp3", "audio/mpeg", "award-audio-zh-2");
        mockFile(118L, 12L, "showroom/audio/award-en-2.mp3", "audio/mpeg", "award-audio-en-2");
        mockFile(119L, 11L, "showroom/hall/company-honor-2.png", "image/png", "company-honor-preview-2");
        mockFile(120L, 12L, "showroom/audio/company-honor-zh-2.mp3", "audio/mpeg", "company-honor-audio-zh-2");
        mockFile(121L, 12L, "showroom/audio/company-honor-en-2.mp3", "audio/mpeg", "company-honor-audio-en-2");
        ShowroomAwardRevision award = contentService.publishAwardRevision(contentService.saveAwardDraft(
                new ShowroomAwardDraft(null, "AWARD-API-003", "社会贡献奖", "Social Contribution Award",
                        "社会贡献奖中文讲解", "Social contribution award English narration",
                        "嘉定区江桥镇人民政府", "2022年度",
                        "/admin-api/infra/file/11/get/showroom/award/social-award.png")).revisionId(), 903L);
        publishNarration(ShowroomNarrationTargetType.AWARD, award.awardId(), award.revisionId(),
                ShowroomNarrationLanguage.ZH, award.fields().get("description_zh"), 111L);
        publishNarration(ShowroomNarrationTargetType.AWARD, award.awardId(), award.revisionId(),
                ShowroomNarrationLanguage.EN, award.fields().get("description_en"), 112L);
        ShowroomHall honorHall = contentService.listHalls().stream()
                .filter(hall -> "hall_09".equals(hall.hallCode()))
                .findFirst()
                .orElseThrow();
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, honorHall.hallId(), honorHall.hallId(), 113L);
        publishNarration(ShowroomNarrationTargetType.HALL, honorHall.hallId(), honorHall.hallId(),
                ShowroomNarrationLanguage.ZH, honorHall.description(), 114L);
        publishNarration(ShowroomNarrationTargetType.HALL, honorHall.hallId(), honorHall.hallId(),
                ShowroomNarrationLanguage.EN, honorHall.descriptionEn(), 115L);
        ShowroomAwardRevision award2 = contentService.publishAwardRevision(contentService.saveAwardDraft(
                new ShowroomAwardDraft(null, "AWARD-API-004", "质量荣誉奖", "Quality Honor Award",
                        "质量荣誉奖中文讲解", "Quality honor award English narration",
                        "嘉定区江桥镇人民政府", "2023年度",
                        "/admin-api/infra/file/11/get/showroom/award/social-award-2.png")).revisionId(), 904L);
        publishNarration(ShowroomNarrationTargetType.AWARD, award2.awardId(), award2.revisionId(),
                ShowroomNarrationLanguage.ZH, award2.fields().get("description_zh"), 117L);
        publishNarration(ShowroomNarrationTargetType.AWARD, award2.awardId(), award2.revisionId(),
                ShowroomNarrationLanguage.EN, award2.fields().get("description_en"), 118L);
        ShowroomHall honorHall2 = contentService.listHalls().stream()
                .filter(hall -> "hall_10".equals(hall.hallCode()))
                .findFirst()
                .orElseThrow();
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, honorHall2.hallId(), honorHall2.hallId(), 119L);
        publishNarration(ShowroomNarrationTargetType.HALL, honorHall2.hallId(), honorHall2.hallId(),
                ShowroomNarrationLanguage.ZH, honorHall2.description(), 120L);
        publishNarration(ShowroomNarrationTargetType.HALL, honorHall2.hallId(), honorHall2.hallId(),
                ShowroomNarrationLanguage.EN, honorHall2.descriptionEn(), 121L);
        bindDefaultSiteStage();

        ShowroomMaterializedRelease release = publisherService.publishRelease(900L, fixture.publishedAt(),
                DEFAULT_SITE_KEY, DEFAULT_STAGE);

        @SuppressWarnings("unchecked")
        Map<String, Object> manifest = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                release.manifestJson(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> awardEntry = ((java.util.List<Map<String, Object>>) manifest.get("documents")).stream()
                .filter(document -> "award-detail".equals(document.get("kind")))
                .findFirst()
                .orElseThrow();
        assertEquals(String.valueOf(award.awardId()), awardEntry.get("awardId"));
        assertTrue(!awardEntry.containsKey("productId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> persistedManifest = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                manifestQueryService.queryManifestJson(defaultReleaseScope(), release.releaseId()), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> persistedAwardEntry = ((java.util.List<Map<String, Object>>) persistedManifest.get("documents")).stream()
                .filter(document -> "award-detail".equals(document.get("kind")))
                .findFirst()
                .orElseThrow();
        assertEquals(String.valueOf(award.awardId()), persistedAwardEntry.get("awardId"));
        assertTrue(!persistedAwardEntry.containsKey("productId"));
        String awardDetail = release.documents().stream()
                .filter(document -> document.documentId().equals("award-detail-" + award.awardId()))
                .findFirst()
                .orElseThrow()
                .payloadJson();
        assertTrue(awardDetail.contains("\"awardId\":\"" + award.awardId() + "\""));
        assertTrue(awardDetail.contains("\"nameCn\":\"社会贡献奖\""));
        assertTrue(awardDetail.contains("\"nameEn\":\"Social Contribution Award\""));
        assertTrue(awardDetail.contains("\"descriptionZh\":\"社会贡献奖中文讲解\""));
        assertTrue(awardDetail.contains("\"descriptionEn\":\"Social contribution award English narration\""));
        assertTrue(awardDetail.contains("\"issuer\":\"嘉定区江桥镇人民政府\""));
        assertTrue(awardDetail.contains("\"awardDateText\":\"2022年度\""));
        @SuppressWarnings("unchecked")
        Map<String, Object> awardDetailPayload = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                awardDetail, Map.class);
        Map<String, Object> websiteIntegrityPayload = new LinkedHashMap<>();
        websiteIntegrityPayload.put("documentId", awardDetailPayload.get("documentId"));
        websiteIntegrityPayload.put("kind", awardDetailPayload.get("kind"));
        websiteIntegrityPayload.put("releaseId", awardDetailPayload.get("releaseId"));
        websiteIntegrityPayload.put("awardId", awardDetailPayload.get("awardId"));
        websiteIntegrityPayload.put("nameCn", awardDetailPayload.get("nameCn"));
        websiteIntegrityPayload.put("nameEn", awardDetailPayload.get("nameEn"));
        websiteIntegrityPayload.put("descriptionZh", awardDetailPayload.get("descriptionZh"));
        websiteIntegrityPayload.put("descriptionEn", awardDetailPayload.get("descriptionEn"));
        websiteIntegrityPayload.put("issuer", awardDetailPayload.get("issuer"));
        websiteIntegrityPayload.put("awardDateText", awardDetailPayload.get("awardDateText"));
        websiteIntegrityPayload.put("audioZh", awardDetailPayload.get("audioZh"));
        websiteIntegrityPayload.put("audioEn", awardDetailPayload.get("audioEn"));
        assertEquals(ShowroomReleaseHashSupport.sha256Hex(
                        cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(websiteIntegrityPayload)),
                awardEntry.get("contentHash"));
    }

    @Test
    void publishReleaseShouldFailWhenPublicWebsiteReadbackFails() throws Exception {
        seedPublishedFixture();
        bindDefaultSiteStage();
        doThrow(new IllegalStateException("SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED: Website current readback failed"))
                .when(publicReleaseReadbackVerifier)
                .verify(eq(DEFAULT_SITE_KEY), eq(DEFAULT_STAGE), anyString(), anyString(), anyString());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> adminController.publishRelease(scopedPublishReq()));

            assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED"));
        }
    }

    @Test
    void publishReleaseShouldUseCurrentLoginTenantInsteadOfExistingBindingTenant() throws Exception {
        seedDistinctReleaseReadyContentForCurrentTenant("product_001", "芋道源码产品", "Yudao Product", "hall_01");
        TenantUtils.execute(TEST_TENANT_ID, () -> {
            try {
                seedDistinctReleaseReadyContentForCurrentTenant("product_001", "测试租户产品", "Test Tenant Product",
                        "hall_01");
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        bindSiteStageToTenant(TEST_TENANT_ID);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            var publish = adminController.publishRelease(scopedPublishReq());

            assertEquals(0, publish.getCode());
            assertEquals(DEFAULT_TENANT_ID,
                    siteBindingMapper.selectEnabledBySiteStage(DEFAULT_SITE_KEY, DEFAULT_STAGE).getTenantId());
            String publicDocument = manifestQueryService.queryDocumentJson(defaultReleaseScope(),
                    publish.getData().releaseId(), ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX);
            assertTrue(publicDocument.contains("芋道源码产品"));
            assertTrue(!publicDocument.contains("测试租户产品"));
        }
    }

    @Test
    void publishReleaseShouldCreateBindingForCurrentLoginTenantWhenMissing() throws Exception {
        seedDistinctReleaseReadyContentForCurrentTenant("product_001", "芋道源码产品", "Yudao Product", "hall_01");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            var publish = adminController.publishRelease(scopedPublishReq());

            assertEquals(0, publish.getCode());
            assertEquals(DEFAULT_TENANT_ID,
                    siteBindingMapper.selectEnabledBySiteStage(DEFAULT_SITE_KEY, DEFAULT_STAGE).getTenantId());
        }
    }

    @Test
    void publishReleaseShouldReuseLogicallyDeletedAssetRows() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        bindDefaultSiteStage();
        ShowroomMaterializedRelease firstRelease = publisherService.publishRelease(900L, fixture.publishedAt(),
                DEFAULT_SITE_KEY, DEFAULT_STAGE);
        ShowroomMaterializedRelease.MaterializedAsset productPreviewAsset = firstRelease.assets().stream()
                .filter(asset -> ("product-" + fixture.productId() + "-preview").equals(asset.assetId()))
                .findFirst()
                .orElseThrow();
        var existingAsset = releaseAssetMapper.selectByAssetIdAndContentHash(
                productPreviewAsset.assetId(), productPreviewAsset.contentHash());
        assertNotNull(existingAsset);
        releaseAssetMapper.deleteById(existingAsset.getId());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            var publish = adminController.publishRelease(scopedPublishReq());

            assertEquals(0, publish.getCode());
            assertNotNull(releaseAssetMapper.selectByAssetIdAndContentHash(
                    productPreviewAsset.assetId(), productPreviewAsset.contentHash()));
        }
    }

    @Test
    void selectAnyByAssetIdAndContentHashShouldSeeLogicallyDeletedAssets() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        bindDefaultSiteStage();
        ShowroomMaterializedRelease firstRelease = publisherService.publishRelease(900L, fixture.publishedAt(),
                DEFAULT_SITE_KEY, DEFAULT_STAGE);
        ShowroomMaterializedRelease.MaterializedAsset productPreviewAsset = firstRelease.assets().stream()
                .filter(asset -> ("product-" + fixture.productId() + "-preview").equals(asset.assetId()))
                .findFirst()
                .orElseThrow();
        var existingAsset = releaseAssetMapper.selectByAssetIdAndContentHash(
                productPreviewAsset.assetId(), productPreviewAsset.contentHash());
        assertNotNull(existingAsset);
        releaseAssetMapper.deleteById(existingAsset.getId());

        var deletedAsset = releaseAssetMapper.selectAnyByAssetIdAndContentHash(
                productPreviewAsset.assetId(), productPreviewAsset.contentHash());

        assertNotNull(deletedAsset);
        assertTrue(Boolean.TRUE.equals(deletedAsset.getDeleted()));
    }

    private ShowroomAdminController.ReleasePublishReqVO scopedPublishReq() {
        return new ShowroomAdminController.ReleasePublishReqVO(DEFAULT_SITE_KEY, DEFAULT_STAGE);
    }

    private void bindSiteStageToTenant(Long tenantId) {
        ShowroomPublicSiteBindingDO binding = siteBindingMapper.selectEnabledBySiteStage(DEFAULT_SITE_KEY, DEFAULT_STAGE);
        if (binding == null) {
            siteBindingMapper.insert(ShowroomPublicSiteBindingDO.builder()
                    .siteKey(DEFAULT_SITE_KEY)
                    .stage(DEFAULT_STAGE)
                    .tenantId(tenantId)
                    .displayName("Yingtai TEST")
                    .enabled(true)
                    .build());
            return;
        }
        binding.setTenantId(tenantId);
        binding.setEnabled(true);
        siteBindingMapper.updateById(binding);
    }

    private void seedDistinctReleaseReadyContentForCurrentTenant(String productCode, String nameCn, String nameEn,
                                                                 String hallCode) throws Exception {
        mockFile(101L, 11L, "showroom/company/company.png", "image/png", "company-home");
        mockFile(102L, 11L, "showroom/hall/hall.png", "image/png", "hall-preview");
        mockFile(103L, 11L, "showroom/product/product.png", "image/png", "product-cover");
        mockFile(104L, 12L, "showroom/audio/company-zh.mp3", "audio/mpeg", "company-audio-zh");
        mockFile(105L, 12L, "showroom/audio/company-en.mp3", "audio/mpeg", "company-audio-en");
        mockFile(106L, 12L, "showroom/audio/product-zh.mp3", "audio/mpeg", "product-audio-zh");
        mockFile(107L, 12L, "showroom/audio/product-en.mp3", "audio/mpeg", "product-audio-en");
        mockFile(108L, 12L, "showroom/audio/hall-zh.mp3", "audio/mpeg", "hall-audio-zh");
        mockFile(109L, 12L, "showroom/audio/hall-en.mp3", "audio/mpeg", "hall-audio-en");

        ShowroomCompanyRevision company = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical", Map.of(
                        "development_history", "发展历程",
                        "development_history_en", "History",
                        "park_introduction", "园区介绍",
                        "park_introduction_en", "Park",
                        "cover_image", "/admin-api/infra/file/11/get/showroom/company/company.png")))
                .revisionId(), 901L);

        ShowroomProductRevision product = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, productCode, nameCn, nameEn, Map.of(
                        "owner_company_id", String.valueOf(company.companyId()),
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "target_market", "冠脉介入",
                        "target_market_en", "Coronary intervention",
                        "core_selling_points", "更顺滑",
                        "core_selling_points_en", "Smoother",
                        "cover_image", "/admin-api/infra/file/11/get/showroom/product/product.png")))
                .revisionId(), 902L);
        ShowroomHall hall = contentService.createHall(hallCode, "心内介入展厅", "Cardiology Hall", "展厅简介", "Hall summary");
        contentService.replaceHallCanvasLayout(hall.hallId(), java.util.List.of(new ShowroomHallProductMapping(
                product.productId(), 1, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ONE, java.math.BigDecimal.ONE)));

        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, company.companyId(), company.revisionId(), 101L);
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 102L);
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, product.productId(), product.revisionId(), 103L);

        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 104L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 105L);
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "展厅中文讲解", 108L);
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "English hall narration", 109L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, product.productId(), product.revisionId(),
                ShowroomNarrationLanguage.ZH, "产品中文讲解", 106L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, product.productId(), product.revisionId(),
                ShowroomNarrationLanguage.EN, "English product narration", 107L);
    }
}
