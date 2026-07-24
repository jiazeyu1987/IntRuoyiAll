package cn.iocoder.yudao.module.showroom.integration;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.AiModelFactory;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPersistentPreviewAssetService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetDraftCommand;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetFiles;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomDisplayController;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudioDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomPublicSiteBindingMapper;
import cn.iocoder.yudao.module.showroom.release.ShowroomLegacyWebsiteConfigProjector;
import cn.iocoder.yudao.module.showroom.release.ShowroomPublicReleaseScopeResolver;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseAssembler;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseManifestQueryService;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleasePublisherService;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleasePurgeService;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseRegistryService;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseScope;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseSourceFileReader;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalActorResolver;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Import({
        ShowroomDisplayController.class,
        ShowroomApiRuntime.class,
        ShowroomPersistentContentService.class,
        ShowroomPersistentNarrationService.class,
        ShowroomPersistentPreviewAssetService.class,
        ShowroomReleaseSourceFileReader.class,
        ShowroomReleaseAssembler.class,
        ShowroomReleaseRegistryService.class,
        ShowroomReleaseManifestQueryService.class,
        ShowroomPublicReleaseScopeResolver.class,
        ShowroomLegacyWebsiteConfigProjector.class,
        ShowroomReleasePurgeService.class,
        ShowroomReleasePublisherService.class
})
class ShowroomAppConfigCompanyFieldsContractTest extends BaseDbUnitTest {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final String DEFAULT_SITE_KEY = "yingtai-showroom";
    private static final String DEFAULT_STAGE = "TEST";

    @Resource
    private ShowroomDisplayController displayController;

    @Resource
    private ShowroomPersistentContentService contentService;

    @Resource
    private ShowroomPersistentPreviewAssetService previewAssetService;

    @Resource
    private ShowroomPersistentNarrationService narrationService;
    @Resource
    private ShowroomReleasePublisherService releasePublisherService;
    @Resource
    private ShowroomLegacyWebsiteConfigProjector legacyWebsiteConfigProjector;
    @Resource
    private ShowroomPublicSiteBindingMapper siteBindingMapper;

    @MockBean
    private ShowroomWorkflowFacade workflowFacade;
    @MockBean
    private ShowroomAssignmentService assignmentService;
    @MockBean
    private ShowroomApprovalActorResolver approvalActorResolver;
    @MockBean
    private ShowroomProductCommentService commentService;
    @MockBean
    private SecurityFrameworkService securityFrameworkService;
    @MockBean
    private ShowroomProductCoverImageService productCoverImageService;
    @MockBean
    private ShowroomProductCoverBatchTaskService productCoverBatchTaskService;
    @MockBean
    private ShowroomImagePromptVersionService imagePromptVersionService;
    @MockBean
    private ShowroomVersionBundleService versionBundleService;
    @MockBean
    private ShowroomCompanyNarrationCodexService narrationCodexService;
    @MockBean
    private ShowroomCompanyNarrationTranslationService narrationTranslationService;
    @MockBean
    private ShowroomProductNarrationCodexService productNarrationCodexService;
    @MockBean
    private FileMapper fileMapper;
    @MockBean
    private ConfigService configService;
    @MockBean
    private FileService fileService;
    @MockBean
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    @MockBean
    private YudaoAiProperties yudaoAiProperties;
    @MockBean
    private AiModelFactory aiModelFactory;
    @MockBean
    private AiModelService aiModelService;
    @MockBean
    private ShowroomProductRevisionRelationMapper productRevisionRelationMapper;
    @MockBean
    private ShowroomChangeRequestMapper changeRequestMapper;

    @Test
    void websiteConfigEndpointShouldRemainPublicWhileLegacyWebsiteDetailEndpointsAreRetired()
            throws Exception {
        Method websiteConfigMethod = ShowroomDisplayController.class.getMethod("getWebsiteConfig");
        Method homeMethod = ShowroomDisplayController.class.getMethod("getHome");
        Method hallMethod = ShowroomDisplayController.class.getMethod("getHall", Long.class);
        Method narrationMethod = ShowroomDisplayController.class.getMethod(
                "getNarration", String.class, Long.class, String.class, String.class);

        assertTrue(websiteConfigMethod.isAnnotationPresent(PermitAll.class));
        assertFalse(hallMethod.isAnnotationPresent(PermitAll.class));
        assertFalse(narrationMethod.isAnnotationPresent(PermitAll.class));
        assertFalse(homeMethod.isAnnotationPresent(PermitAll.class));
        assertFalse(hasMethod("getAppConfig"));
        assertFalse(hasMethod("getCompany"));
        assertFalse(hasMethod("getProduct", Long.class));
    }

    @Test
    void websiteConfigShouldAggregateCompanyPublicFieldsInDisplayOrder() {
        var liveCompany = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.ofEntries(
                                Map.entry("development_history", "盈泰医疗发展历程"),
                                Map.entry("development_history_en", "Yingtai medical development history"),
                                Map.entry("park_introduction", "园区介绍中文"),
                                Map.entry("park_introduction_en", ""),
                                Map.entry("incubation_platform", "孵化平台中文"),
                                Map.entry("incubation_platform_en", ""),
                                Map.entry("subsidiary_overview", "子公司概览中文"),
                                Map.entry("subsidiary_overview_en", ""),
                                Map.entry("stock_info", "上市信息中文"),
                                Map.entry("stock_info_en", ""),
                                Map.entry("core_manufacturing_capability", "核心制造能力中文"),
                                Map.entry("core_manufacturing_capability_en", ""),
                                Map.entry("honors_awards", "国家高新技术企业"),
                                Map.entry("honors_awards_en", "National high-tech enterprise"),
                                Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-home.png")
                        )))
                .revisionId(), 901L);
        mockFile(501L, 28L, "showroom/preview/company-home.png", "image/png");
        mockFile(511L, 29L, "showroom/narration/company-zh.wav", "audio/wav");
        mockFile(512L, 29L, "showroom/narration/company-en.wav", "audio/wav");

        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, liveCompany.companyId(),
                liveCompany.revisionId(), 501L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 511L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 512L, "ruoxi");

        Object payload = getWebsiteConfigPayload();
        Object company = accessor(payload, "company");
        List<?> publicFields = listAccessor(company, "publicFields");
        List<?> bilingualPublicFields = listAccessor(company, "bilingualPublicFields");

        assertEquals("盈泰医疗", accessor(company, "name"));
        assertEquals("Yingtai Medical", accessor(company, "nameEn"));
        assertTrue(String.valueOf((Object) accessor(company, "homeImageUrl")).startsWith("/showroom/sites/"));
        assertEquals("公司中文讲解", accessor(company, "subtitleZh"));
        assertEquals("English company narration", accessor(company, "subtitleEn"));
        assertTrue(String.valueOf((Object) accessor(company, "audioZhUrl")).startsWith("/showroom/sites/"));
        assertTrue(String.valueOf((Object) accessor(company, "audioEnUrl")).startsWith("/showroom/sites/"));
        assertEquals(5, publicFields.size());
        assertEquals("发展历程", accessor(publicFields.get(0), "label"));
        assertEquals("盈泰医疗发展历程", accessor(publicFields.get(0), "value"));
        assertEquals("园区介绍", accessor(publicFields.get(1), "label"));
        assertEquals("园区介绍中文", accessor(publicFields.get(1), "value"));
        assertEquals("孵化平台", accessor(publicFields.get(2), "label"));
        assertEquals("子公司概览", accessor(publicFields.get(3), "label"));
        assertEquals("上市信息", accessor(publicFields.get(4), "label"));
        assertEquals(5, bilingualPublicFields.size());
        assertEquals("development_history", accessor(bilingualPublicFields.get(0), "fieldCode"));
        assertEquals("发展历程", accessor(bilingualPublicFields.get(0), "labelZh"));
        assertEquals("Development History", accessor(bilingualPublicFields.get(0), "labelEn"));
        assertEquals("盈泰医疗发展历程", accessor(bilingualPublicFields.get(0), "valueZh"));
        assertEquals("Yingtai medical development history",
                accessor(bilingualPublicFields.get(0), "valueEn"));
        assertEquals("park_introduction",
                accessor(bilingualPublicFields.get(1), "fieldCode"));
        assertEquals("园区介绍中文",
                accessor(bilingualPublicFields.get(1), "valueZh"));
        assertEquals("",
                accessor(bilingualPublicFields.get(1), "valueEn"));
        assertEquals("incubation_platform",
                accessor(bilingualPublicFields.get(2), "fieldCode"));
        assertEquals("subsidiary_overview",
                accessor(bilingualPublicFields.get(3), "fieldCode"));
        assertEquals("stock_info",
                accessor(bilingualPublicFields.get(4), "fieldCode"));
    }

    @Test
    void websiteConfigShouldReturnExplicitEmptyCompanyPublicFieldsWhenNoFieldHasValue() {
        var liveCompany = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.of("cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-home.png")))
                .revisionId(), 901L);
        mockFile(801L, 28L, "showroom/preview/company-home.png", "image/png");
        mockFile(811L, 29L, "showroom/narration/company-zh.wav", "audio/wav");
        mockFile(812L, 29L, "showroom/narration/company-en.wav", "audio/wav");

        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, liveCompany.companyId(),
                liveCompany.revisionId(), 801L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 811L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 812L, "ruoxi");

        Object company = accessor(getWebsiteConfigPayload(), "company");

        assertNotNull(listAccessor(company, "publicFields"));
        assertTrue(listAccessor(company, "publicFields").isEmpty());
        assertNotNull(listAccessor(company, "bilingualPublicFields"));
        assertTrue(listAccessor(company, "bilingualPublicFields").isEmpty());
    }

    @Test
    void websiteConfigShouldNotExposeCompanyCoverImageAsPublicTextField() {
        var liveCompany = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.of(
                                "development_history", "盈泰医疗发展历程",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-home.png"
                        )))
                .revisionId(), 901L);
        mockFile(901L, 28L, "showroom/preview/company-home.png", "image/png");
        mockFile(911L, 29L, "showroom/narration/company-zh.wav", "audio/wav");
        mockFile(912L, 29L, "showroom/narration/company-en.wav", "audio/wav");

        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, liveCompany.companyId(),
                liveCompany.revisionId(), 901L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 911L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 912L, "ruoxi");

        Object company = accessor(getWebsiteConfigPayload(), "company");
        List<?> publicFields = listAccessor(company, "publicFields");
        List<?> bilingualPublicFields = listAccessor(company, "bilingualPublicFields");

        assertEquals(1, publicFields.size());
        assertEquals("发展历程", accessor(publicFields.get(0), "label"));
        assertEquals("盈泰医疗发展历程", accessor(publicFields.get(0), "value"));
        assertEquals(1, bilingualPublicFields.size());
        assertEquals("盈泰医疗发展历程", accessor(bilingualPublicFields.get(0), "valueZh"));
        assertEquals("", accessor(bilingualPublicFields.get(0), "valueEn"));
    }

    private void mockFile(Long fileId, Long configId, String path, String type) {
        FileDO file = FileDO.builder()
                .id(fileId)
                .configId(configId)
                .name(path.substring(path.lastIndexOf('/') + 1))
                .path(path)
                .url("http://127.0.0.1:9000/yudao/" + path)
                .type(type)
                .size(128L)
                .build();
        when(fileMapper.selectById(fileId)).thenReturn(file);
        when(fileService.getFile(fileId)).thenReturn(file);
        try {
            when(fileService.getFileContent(configId, path))
                    .thenReturn(path.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void publishPreviewAsset(ShowroomPreviewAssetTargetType targetType, Long targetId,
                                     Long sourceRevisionId, Long imageFileId) {
        var draft = previewAssetService.bindStaticPreviewAssets(new ShowroomPreviewAssetDraftCommand(
                targetType, targetId, sourceRevisionId,
                new ShowroomPreviewAssetFiles(imageFileId, imageFileId, imageFileId)));
        previewAssetService.publish(previewAssetService.gaoxinApprove(
                previewAssetService.supervisorApprove(previewAssetService.submit(draft.id()).id(), 200L).id(),
                300L).id());
    }

    private void publishNarration(ShowroomNarrationTargetType targetType, Long targetId, Long sourceRevisionId,
                                  ShowroomNarrationLanguage language, String scriptText, Long audioFileId,
                                  String voice) {
        ShowroomNarrationVersion draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                targetType, targetId, sourceRevisionId, ShowroomNarrationAudienceType.PUBLIC, language,
                scriptText, false));
        draft = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                draft.id(), audioFileId, 60, voice));
        narrationService.publishDirectly(draft.id());
    }

    private Object getWebsiteConfigPayload() {
        try {
            ensureMinimalReleaseSupport();
            bindDefaultSiteStage();
            releasePublisherService.publishRelease(900L, Instant.parse("2026-05-23T10:15:00Z"),
                    DEFAULT_SITE_KEY, DEFAULT_STAGE);
            CommonResult<?> result = legacyWebsiteConfigProjector.projectCurrentPayload(defaultReleaseScope());
            return result.getCheckedData();
        } catch (Exception exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(exception);
        }
    }

    private void bindDefaultSiteStage() {
        if (siteBindingMapper.selectEnabledBySiteStage(DEFAULT_SITE_KEY, DEFAULT_STAGE) != null) {
            return;
        }
        siteBindingMapper.insert(ShowroomPublicSiteBindingDO.builder()
                .siteKey(DEFAULT_SITE_KEY)
                .stage(DEFAULT_STAGE)
                .tenantId(DEFAULT_TENANT_ID)
                .displayName("Yingtai TEST")
                .enabled(true)
                .build());
    }

    private ShowroomReleaseScope defaultReleaseScope() {
        return new ShowroomReleaseScope(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE);
    }

    private void ensureMinimalReleaseSupport() {
        if (!contentService.listHalls().isEmpty()) {
            return;
        }
        mockFile(981L, 28L, "showroom/preview/minimal-product.png", "image/png");
        mockFile(982L, 29L, "showroom/narration/minimal-product-zh.wav", "audio/wav");
        mockFile(983L, 29L, "showroom/narration/minimal-product-en.wav", "audio/wav");
        mockFile(984L, 28L, "showroom/preview/minimal-hall.png", "image/png");
        mockFile(985L, 29L, "showroom/narration/minimal-hall-zh.wav", "audio/wav");
        mockFile(986L, 29L, "showroom/narration/minimal-hall-en.wav", "audio/wav");

        ShowroomProductRevision productRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "YT-APPCFG-001", "公司字段占位产品", "Company Field Placeholder Product",
                        Map.of(
                                "owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "占位市场",
                                "core_selling_points", "占位卖点",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/minimal-product.png")))
                .revisionId(), 901L);
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, productRevision.productId(),
                productRevision.revisionId(), 981L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, productRevision.productId(), productRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "占位产品中文讲解", 982L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, productRevision.productId(), productRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "Placeholder product English narration", 983L, "ruoxi");

        ShowroomHall hall = contentService.createHall("APP_CFG", "公司字段展厅", "Company Field Hall",
                "占位展厅说明", "Placeholder hall overview");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 984L);
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "占位展厅说明", 985L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "Placeholder hall overview", 986L, "ruoxi");
        contentService.replaceHallCanvasLayout(hall.hallId(),
                List.of(new ShowroomHallProductMapping(productRevision.productId(), 1,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ONE, java.math.BigDecimal.ONE)));
    }

    private static boolean hasMethod(String name, Class<?>... parameterTypes) {
        return findMethod(name, parameterTypes) != null;
    }

    private static Method findMethod(String name, Class<?>... parameterTypes) {
        try {
            return ShowroomDisplayController.class.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T accessor(Object target, String methodName) {
        try {
            return (T) target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static List<?> listAccessor(Object target, String methodName) {
        return accessor(target, methodName);
    }
}
