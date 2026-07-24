package cn.iocoder.yudao.module.showroom.integration;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.AiModelFactory;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.ai.service.tts.AliyunNlsTtsSynthesizer;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPersistentPreviewAssetService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetKey;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetDraftCommand;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetFiles;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.configpackage.ShowroomHallConfigPackageService;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCommentAnchorType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomNativeImageGenerationService;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomDisplayController;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomFieldAssignmentDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomFieldAssignmentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestSignatureMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.version.ShowroomVersionBundleMapper;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomPublicSiteBindingMapper;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomRoleModelContract;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudioDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.narration.ShowroomAliyunNlsAudioGenerationAdapter;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
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
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalDetail;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomFieldAssignment;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalActorResolver;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalSignatureService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomPersistentWorkflowService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowNotifyService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyTemplateMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifySendServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifyTemplateServiceImpl;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.security.PermitAll;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Import({
        ShowroomAdminController.class,
        ShowroomDisplayController.class,
        ShowroomApiRuntime.class,
        ShowroomProductCoverBatchTaskService.class,
        ShowroomProductCoverImageService.class,
        ShowroomImagePromptVersionService.class,
        ShowroomPersistentContentService.class,
        ShowroomPersistentNarrationService.class,
        ShowroomAliyunNlsAudioGenerationAdapter.class,
        ShowroomCompanyNarrationCodexService.class,
        ShowroomCompanyNarrationTranslationService.class,
        ShowroomPersistentPreviewAssetService.class,
        ShowroomReleaseSourceFileReader.class,
        ShowroomReleaseAssembler.class,
        ShowroomReleaseRegistryService.class,
        ShowroomReleaseManifestQueryService.class,
        ShowroomPublicReleaseScopeResolver.class,
        ShowroomLegacyWebsiteConfigProjector.class,
        ShowroomReleasePurgeService.class,
        ShowroomReleasePublisherService.class,
        ShowroomVersionBundleService.class,
        ShowroomPersistentWorkflowService.class,
        ShowroomWorkflowNotifyService.class,
        ShowroomWorkflowFacade.class,
        ShowroomApprovalSignatureService.class,
        ShowroomAssignmentService.class,
        ShowroomApprovalActorResolver.class,
        ShowroomProductCommentService.class,
        NotifyMessageSendApiImpl.class,
        NotifySendServiceImpl.class,
        NotifyMessageServiceImpl.class,
        NotifyTemplateServiceImpl.class
})
class ShowroomHttpApiIntegrationTest extends BaseDbUnitTest {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final String DEFAULT_SITE_KEY = "yingtai-showroom";
    private static final String DEFAULT_STAGE = "TEST";

    private static final String BASELINE_PRODUCT_COVER_IMAGE =
            "/admin-api/infra/file/28/get/showroom/preview/baseline-product-cover.png";

    private static final byte[] ONE_PIXEL_PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WnXl1QAAAAASUVORK5CYII=");

    private static final String ASSIGNMENT_TEMPLATE_CODE = "SHOWROOM_ASSIGNMENT";
    private static final String PENDING_APPROVAL_TEMPLATE_CODE = "SHOWROOM_APPROVAL_PENDING";
    private static final String PUBLISHED_APPROVAL_TEMPLATE_CODE = "SHOWROOM_APPROVAL_PUBLISHED";
    private static final String REJECTED_APPROVAL_TEMPLATE_CODE = "SHOWROOM_APPROVAL_REJECTED";

    @Resource
    private ShowroomAdminController adminController;

    @Resource
    private ShowroomPersistentContentService contentService;

    @Resource
    private ShowroomDisplayController displayController;

    @Resource
    private ShowroomPersistentPreviewAssetService previewAssetService;
    @Resource
    private ShowroomPersistentNarrationService narrationService;
    @Resource
    private ShowroomReleasePublisherService releasePublisherService;
    @Resource
    private ShowroomImagePromptVersionService imagePromptVersionService;
    @Resource
    private ShowroomVersionBundleMapper versionBundleMapper;
    @Resource
    private ShowroomLegacyWebsiteConfigProjector legacyWebsiteConfigProjector;
    @Resource
    private ShowroomPublicSiteBindingMapper siteBindingMapper;

    private final Map<String, ConfigDO> configStore = new HashMap<>();
    private final AtomicLong configIdSequence = new AtomicLong(1L);

    @MockBean
    private FileMapper fileMapper;
    @MockBean
    private ConfigService configService;
    @MockBean
    private FileService fileService;
    @MockBean
    private FileApi fileApi;
    @MockBean
    private AliyunNlsTtsSynthesizer aliyunNlsTtsSynthesizer;
    @MockBean
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    @MockBean
    private YudaoAiProperties yudaoAiProperties;
    @MockBean
    private AiModelFactory aiModelFactory;
    @MockBean
    private AiModelService aiModelService;
    @MockBean
    private SecurityFrameworkService securityFrameworkService;
    @MockBean
    private ShowroomCompanyNarrationCodexService narrationCodexService;
    @MockBean
    private ShowroomCompanyNarrationTranslationService narrationTranslationService;
    @MockBean
    private ShowroomProductNarrationCodexService productNarrationCodexService;
    @MockBean
    private ShowroomNativeImageGenerationService nativeImageGenerationService;
    @MockBean
    private ShowroomHallConfigPackageService hallConfigPackageService;
    @MockBean
    private AdminUserService adminUserService;
    @MockBean
    private DccElectronicSignatureAuthorizationService electronicSignatureAuthorizationService;
    @MockBean
    private ShowroomChangeRequestSignatureMapper changeRequestSignatureMapper;

    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @BeforeEach
    void setUpApprovalSignatureMocks() {
        configStore.clear();
        configIdSequence.set(1L);
        if (imagePromptVersionService.history(ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER).isEmpty()) {
            imagePromptVersionService.saveNewVersion(
                    ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER,
                    ShowroomImagePromptVersionService.DEFAULT_PRODUCT_COVER_TEMPLATE,
                    "integration test seed");
        }
        when(configService.getConfigByKey(anyString())).thenAnswer(invocation -> configStore.get(invocation.getArgument(0)));
        when(configService.createConfig(org.mockito.ArgumentMatchers.any(ConfigSaveReqVO.class)))
                .thenAnswer(invocation -> {
                    ConfigSaveReqVO reqVO = invocation.getArgument(0);
                    ConfigDO config = new ConfigDO();
                    config.setId(configIdSequence.getAndIncrement());
                    config.setCategory(reqVO.getCategory());
                    config.setName(reqVO.getName());
                    config.setConfigKey(reqVO.getKey());
                    config.setValue(reqVO.getValue());
                    config.setVisible(reqVO.getVisible());
                    config.setRemark(reqVO.getRemark());
                    configStore.put(reqVO.getKey(), config);
                    return config.getId();
                });
        doAnswer(invocation -> {
            ConfigSaveReqVO reqVO = invocation.getArgument(0);
            ConfigDO config = configStore.get(reqVO.getKey());
            if (config == null) {
                config = new ConfigDO();
                config.setId(reqVO.getId() == null ? configIdSequence.getAndIncrement() : reqVO.getId());
            }
            config.setCategory(reqVO.getCategory());
            config.setName(reqVO.getName());
            config.setConfigKey(reqVO.getKey());
            config.setValue(reqVO.getValue());
            config.setVisible(reqVO.getVisible());
            config.setRemark(reqVO.getRemark());
            configStore.put(reqVO.getKey(), config);
            return null;
        }).when(configService).updateConfig(org.mockito.ArgumentMatchers.any(ConfigSaveReqVO.class));
        mockShowroomRoleChecks();
        when(electronicSignatureAuthorizationService.isElectronicSignatureEnabled(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(true);
        when(adminUserService.getUser(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(0);
                    AdminUserDO user = adminUserMapper.selectById(userId);
                    if (user != null) {
                        return user;
                    }
                    AdminUserDO fallback = new AdminUserDO();
                    fallback.setId(userId);
                    fallback.setPassword("encoded-password");
                    return fallback;
                });
        when(adminUserService.isPasswordMatch(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "111111".equals(invocation.getArgument(0)));
        when(changeRequestSignatureMapper.insert(
                org.mockito.ArgumentMatchers.<cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestSignatureDO>any()))
                .thenReturn(1);
    }
    @Resource
    private NotifyTemplateMapper notifyTemplateMapper;
    @Resource
    private NotifyMessageMapper notifyMessageMapper;
    @Resource
    private ShowroomFieldAssignmentMapper assignmentMapper;
    @Resource
    private ShowroomChangeRequestMapper changeRequestMapper;

    @Test
    void controllersShouldExposeRealAdminAndDisplayRoutes() {
        assertTrue(ShowroomAdminController.class.isAnnotationPresent(RestController.class));
        assertTrue(ShowroomDisplayController.class.isAnnotationPresent(RestController.class));
        assertEquals("/showroom", ShowroomAdminController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertEquals("/showroom/display",
                ShowroomDisplayController.class.getAnnotation(RequestMapping.class).value()[0]);
    }

    @Test
    void companyCurrentShouldReturnFrontendContractStringsWhenTenantHasNoCompanyRevision() {
        var current = TenantUtils.execute(162L, () -> adminController.getCompanyCurrent().getCheckedData());

        assertEquals(0L, current.companyId());
        assertEquals("DRAFT", current.status());
        assertEquals("", current.companyType());
        assertEquals("", current.displayName());
        assertEquals("", current.displayNameEn());
        assertFalse(current.live());
        assertNotNull(current.fields());
    }

    @Test
    void websiteConfigShouldBePublicWhileLegacyWebsiteDetailEndpointsAreRetired() throws Exception {
        Method websiteConfigMethod = ShowroomDisplayController.class.getMethod("getWebsiteConfig",
                String.class, String.class);
        Method getRuntimeClientSettingsMethod = ShowroomDisplayController.class.getMethod(
                "getRuntimeClientSettings", String.class, String.class);
        Method saveRuntimeClientSettingsMethod = ShowroomDisplayController.class.getMethod(
                "saveRuntimeClientSettings", ShowroomDisplayController.RuntimeClientSettingsSaveReqVO.class);
        Method homeMethod = ShowroomDisplayController.class.getMethod("getHome");
        Method hallMethod = ShowroomDisplayController.class.getMethod("getHall", Long.class);
        Method narrationMethod = ShowroomDisplayController.class.getMethod("getNarration",
                String.class, Long.class, String.class, String.class);

        assertTrue(websiteConfigMethod.isAnnotationPresent(PermitAll.class));
        assertTrue(getRuntimeClientSettingsMethod.isAnnotationPresent(PermitAll.class));
        assertTrue(saveRuntimeClientSettingsMethod.isAnnotationPresent(PermitAll.class));
        assertFalse(hallMethod.isAnnotationPresent(PermitAll.class));
        assertFalse(narrationMethod.isAnnotationPresent(PermitAll.class));
        assertFalse(homeMethod.isAnnotationPresent(PermitAll.class));
        assertFalse(hasDisplayMethod("getAppConfig"));
        assertFalse(hasDisplayMethod("getCompany"));
        assertFalse(hasDisplayMethod("getProduct", Long.class));
    }

    @Test
    void runtimeClientSettingsShouldPersistProductItemGapsInTenantScopedConfig() {
        bindDefaultSiteStage();

        var defaults = displayController.getRuntimeClientSettings(DEFAULT_SITE_KEY, DEFAULT_STAGE).getCheckedData();
        assertEquals(12, defaults.companyDetailSettings().productItemHorizontalGap());
        assertEquals(12, defaults.companyDetailSettings().productItemVerticalGap());

        var saved = displayController.saveRuntimeClientSettings(
                new ShowroomDisplayController.RuntimeClientSettingsSaveReqVO(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                        new ShowroomDisplayController.RuntimeClientCompanyDetailSettings(26, 14)))
                .getCheckedData();

        assertEquals(26, saved.companyDetailSettings().productItemHorizontalGap());
        assertEquals(14, saved.companyDetailSettings().productItemVerticalGap());

        var restored = displayController.getRuntimeClientSettings(DEFAULT_SITE_KEY, DEFAULT_STAGE).getCheckedData();
        assertEquals(26, restored.companyDetailSettings().productItemHorizontalGap());
        assertEquals(14, restored.companyDetailSettings().productItemVerticalGap());
        assertFalse(configStore.get("showroom.runtime.client.settings").getVisible());
    }

    @Test
    void websiteConfigShouldAggregateCompanyHallProductDetailAndBilingualMedia() {
        var liveCompany = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.ofEntries(
                                Map.entry("development_history", "盈泰医疗发展历程"),
                                Map.entry("development_history_en", "Yingtai medical development history"),
                                Map.entry("park_introduction", "园区介绍中文"),
                                Map.entry("park_introduction_en", "Park introduction in English"),
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
                                Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-home.png"))))
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

        var firstProduct = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-101", "导丝系统", "Guidewire System",
                        Map.of("target_market", "冠脉市场", "core_selling_points", "更顺滑",
                                "registration_certificate", "注册证 A",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/product-101.png")))
                .revisionId(), 902L);
        var secondProduct = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-102", "导管系统", "Catheter System",
                        Map.of("target_market", "外周市场", "core_selling_points", "更稳定",
                                "registration_certificate", "注册证 B",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/product-102.png")))
                .revisionId(), 903L);

        mockFile(601L, 28L, "showroom/preview/hall-cardiology.png", "image/png");
        mockFile(611L, 28L, "showroom/preview/product-101.png", "image/png");
        mockFile(612L, 28L, "showroom/preview/product-102.png", "image/png");
        mockFile(621L, 29L, "showroom/narration/product-101-zh.wav", "audio/wav");
        mockFile(622L, 29L, "showroom/narration/product-101-en.wav", "audio/wav");
        mockFile(623L, 29L, "showroom/narration/product-102-zh.wav", "audio/wav");
        mockFile(624L, 29L, "showroom/narration/product-102-en.wav", "audio/wav");
        mockFile(625L, 29L, "showroom/narration/hall-cardiology-zh.wav", "audio/wav");
        mockFile(626L, 29L, "showroom/narration/hall-cardiology-en.wav", "audio/wav");

        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, firstProduct.productId(),
                firstProduct.revisionId(), 611L);
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, secondProduct.productId(),
                secondProduct.revisionId(), 612L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, firstProduct.productId(), firstProduct.revisionId(),
                ShowroomNarrationLanguage.ZH, "产品一中文讲解", 621L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, firstProduct.productId(), firstProduct.revisionId(),
                ShowroomNarrationLanguage.EN, "English product narration one", 622L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, secondProduct.productId(), secondProduct.revisionId(),
                ShowroomNarrationLanguage.ZH, "产品二中文讲解", 623L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, secondProduct.productId(), secondProduct.revisionId(),
                ShowroomNarrationLanguage.EN, "English product narration two", 624L, "ruoxi");

        var hall = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "CARDIOLOGY", "心内介入展厅", "Cardiology Hall", "心内展厅说明", "Cardiology hall overview")).getCheckedData();
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 601L);
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "心内展厅说明", 625L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "Cardiology hall overview", 626L, "ruoxi");
        saveHallCanvasLayout(hall.hallId(), firstProduct.productId(), secondProduct.productId());

        Object payload = getWebsiteConfigPayload();
        Object company = recordAccessor(payload, "company");
        List<?> showrooms = recordListAccessor(payload, "showrooms");

        assertEquals("盈泰医疗", recordAccessor(company, "name"));
        assertEquals("Yingtai Medical", recordAccessor(company, "nameEn"));
        assertTrue(String.valueOf((Object) recordAccessor(company, "homeImageUrl")).startsWith("/showroom/sites/"));
        assertEquals("公司中文讲解", recordAccessor(company, "subtitleZh"));
        assertEquals("English company narration", recordAccessor(company, "subtitleEn"));
        assertTrue(String.valueOf((Object) recordAccessor(company, "audioZhUrl")).startsWith("/showroom/sites/"));
        assertTrue(String.valueOf((Object) recordAccessor(company, "audioEnUrl")).startsWith("/showroom/sites/"));
        assertEquals(5, recordListAccessor(company, "publicFields").size());
        assertEquals("发展历程", recordAccessor(recordListAccessor(company, "publicFields").get(0), "label"));
        assertEquals("盈泰医疗发展历程", recordAccessor(recordListAccessor(company, "publicFields").get(0), "value"));
        assertEquals("园区介绍", recordAccessor(recordListAccessor(company, "publicFields").get(1), "label"));
        assertEquals("园区介绍中文", recordAccessor(recordListAccessor(company, "publicFields").get(1), "value"));
        assertEquals("孵化平台", recordAccessor(recordListAccessor(company, "publicFields").get(2), "label"));
        assertEquals("子公司概览", recordAccessor(recordListAccessor(company, "publicFields").get(3), "label"));
        assertEquals("上市信息", recordAccessor(recordListAccessor(company, "publicFields").get(4), "label"));

        assertEquals(1, showrooms.size());
        Object showroom = showrooms.get(0);
        List<?> products = recordListAccessor(showroom, "products");
        assertEquals("CARDIOLOGY", recordAccessor(showroom, "hallCode"));
        assertEquals("心内介入展厅", recordAccessor(showroom, "name"));
        assertEquals("Cardiology Hall", recordAccessor(showroom, "nameEn"));
        assertEquals("心内展厅说明", recordAccessor(showroom, "description"));
        assertEquals("Cardiology hall overview", recordAccessor(showroom, "descriptionEn"));
        assertTrue(String.valueOf((Object) recordAccessor(showroom, "previewImageUrl")).startsWith("/showroom/sites/"));
        assertTrue(String.valueOf((Object) recordAccessor(showroom, "audioZhUrl")).startsWith("/showroom/sites/"));
        assertTrue(String.valueOf((Object) recordAccessor(showroom, "audioEnUrl")).startsWith("/showroom/sites/"));

        assertEquals(2, products.size());
        assertEquals(firstProduct.productId(), recordAccessor(products.get(0), "productId"));
        assertEquals(secondProduct.productId(), recordAccessor(products.get(1), "productId"));
        assertEquals("导丝系统", recordAccessor(products.get(0), "nameCn"));
        assertEquals("Guidewire System", recordAccessor(products.get(0), "nameEn"));
        assertTrue(String.valueOf((Object) recordAccessor(products.get(0), "previewImageUrl")).startsWith("/showroom/sites/"));
        assertEquals("产品一中文讲解", recordAccessor(products.get(0), "subtitleZh"));
        assertEquals("English product narration one", recordAccessor(products.get(0), "subtitleEn"));
        assertTrue(String.valueOf((Object) recordAccessor(products.get(0), "audioZhUrl")).startsWith("/showroom/sites/"));
        assertTrue(String.valueOf((Object) recordAccessor(products.get(0), "audioEnUrl")).startsWith("/showroom/sites/"));
        assertEquals("冠脉市场", recordAccessor(findBilingualField(products.get(0), "target_market"), "valueZh"));
        assertEquals("Guidewire System", recordAccessor(findBilingualField(products.get(0), "name"), "valueEn"));
        assertTrue(recordListAccessor(products.get(0), "bilingualPublicFields").stream()
                .noneMatch(field -> "registration_certificate".equals(recordAccessor(field, "fieldCode"))));
    }

    @Test
    void websiteConfigShouldReturnExplicitEmptyCompanyPublicFieldsWhenNoCompanyDisplayFieldHasValue() {
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

        Object company = recordAccessor(getWebsiteConfigPayload(), "company");

        assertNotNull(recordListAccessor(company, "publicFields"));
        assertTrue(recordListAccessor(company, "publicFields").isEmpty());
    }

    @Test
    void websiteConfigCompanyShouldExposeBilingualCompanyFieldsWithoutEnglishFallback() {
        var liveCompany = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.of(
                                "development_history", "盈泰医疗发展历程",
                                "development_history_en", "Yingtai medical development history",
                                "park_introduction", "园区介绍中文",
                                "incubation_platform", "孵化平台中文",
                                "subsidiary_overview", "子公司概览中文",
                                "stock_info", "上市信息中文",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-home.png"
                        )))
                .revisionId(), 901L);
        mockFile(821L, 28L, "showroom/preview/company-home.png", "image/png");
        mockFile(831L, 29L, "showroom/narration/company-zh.wav", "audio/wav");
        mockFile(832L, 29L, "showroom/narration/company-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, liveCompany.companyId(),
                liveCompany.revisionId(), 821L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 831L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 832L, "ruoxi");

        Object company = recordAccessor(getWebsiteConfigPayload(), "company");
        List<?> bilingualPublicFields = recordListAccessor(company, "bilingualPublicFields");

        assertEquals(5, bilingualPublicFields.size());
        assertEquals("development_history", recordAccessor(bilingualPublicFields.get(0), "fieldCode"));
        assertEquals("发展历程", recordAccessor(bilingualPublicFields.get(0), "labelZh"));
        assertEquals("Development History", recordAccessor(bilingualPublicFields.get(0), "labelEn"));
        assertEquals("盈泰医疗发展历程", recordAccessor(bilingualPublicFields.get(0), "valueZh"));
        assertEquals("Yingtai medical development history", recordAccessor(bilingualPublicFields.get(0), "valueEn"));
        assertEquals("park_introduction", recordAccessor(bilingualPublicFields.get(1), "fieldCode"));
        assertEquals("园区介绍中文", recordAccessor(bilingualPublicFields.get(1), "valueZh"));
        assertEquals("", recordAccessor(bilingualPublicFields.get(1), "valueEn"));
        assertEquals("incubation_platform", recordAccessor(bilingualPublicFields.get(2), "fieldCode"));
        assertEquals("subsidiary_overview", recordAccessor(bilingualPublicFields.get(3), "fieldCode"));
        assertEquals("stock_info", recordAccessor(bilingualPublicFields.get(4), "fieldCode"));
    }

    @Test
    void websiteConfigShouldFailFastWhenMappedProductDisplayImageIsMissing() {
        var liveCompany = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.of("development_history", "盈泰医疗发展历程",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-home.png")))
                .revisionId(), 901L);
        mockFile(701L, 28L, "showroom/preview/company-home.png", "image/png");
        mockFile(711L, 29L, "showroom/narration/company-zh.wav", "audio/wav");
        mockFile(712L, 29L, "showroom/narration/company-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, liveCompany.companyId(),
                liveCompany.revisionId(), 701L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 711L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 712L, "ruoxi");

        var missingCoverProduct = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-201", "缺封面产品", "Missing Cover Product",
                        Map.of("target_market", "冠脉市场",
                                "core_selling_points", "存在预览图但没有封面",
                                "registration_certificate", "注册证 C")))
                .revisionId(), 904L);
        var validProduct = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-202", "正常封面产品", "Valid Cover Product",
                        Map.of("target_market", "冠脉市场",
                                "core_selling_points", "有封面，可以继续展示",
                                "registration_certificate", "注册证 D",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/product-202-cover.png")))
                .revisionId(), 905L);
        mockFile(721L, 28L, "showroom/preview/hall-cover-only.png", "image/png");
        mockFile(731L, 29L, "showroom/narration/product-201-zh.wav", "audio/wav");
        mockFile(732L, 29L, "showroom/narration/product-201-en.wav", "audio/wav");
        mockFile(741L, 28L, "showroom/preview/product-201-preview.png", "image/png");
        mockFile(742L, 28L, "showroom/preview/product-202-cover.png", "image/png");
        mockFile(751L, 29L, "showroom/narration/product-202-zh.wav", "audio/wav");
        mockFile(752L, 29L, "showroom/narration/product-202-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, missingCoverProduct.productId(), missingCoverProduct.revisionId(),
                ShowroomNarrationLanguage.ZH, "产品中文讲解", 731L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, missingCoverProduct.productId(), missingCoverProduct.revisionId(),
                ShowroomNarrationLanguage.EN, "English product narration", 732L, "ruoxi");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, missingCoverProduct.productId(),
                missingCoverProduct.revisionId(), 741L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, validProduct.productId(), validProduct.revisionId(),
                ShowroomNarrationLanguage.ZH, "有效产品中文讲解", 751L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, validProduct.productId(), validProduct.revisionId(),
                ShowroomNarrationLanguage.EN, "Valid product English narration", 752L, "ruoxi");

        var hall = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "COVER_ONLY", "仅封面展厅", "Cover Only Hall", "用于验证缺封面产品失败", "Fail on missing cover product")).getCheckedData();
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 721L);
        mockFile(725L, 29L, "showroom/narration/hall-cover-only-zh.wav", "audio/wav");
        mockFile(726L, 29L, "showroom/narration/hall-cover-only-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "仅封面展厅说明", 725L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "Cover only hall overview", 726L, "ruoxi");
        saveHallCanvasLayout(hall.hallId(), missingCoverProduct.productId(), validProduct.productId());

        RuntimeException exception = assertThrows(RuntimeException.class, this::getWebsiteConfigPayload);
        assertTrue(String.valueOf(exception.getMessage()).contains("product cover_image is required"));
    }

    @Test
    void companyPublishShouldAllowLoggedInUserAndIncrementRevisionWithoutApproval() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(false);
        var baseline = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "瑛泰", "Yingtai", Map.of("development_history", "初始版本")));
        var liveBaseline = contentService.publishCompanyRevision(baseline.revisionId(), 901L);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            var published = adminController.publishCompany(new ShowroomAdminController.CompanyDraftReqVO(
                    liveBaseline.companyId(), "MAIN", "瑛泰医疗", "Yingtai Medical",
                    Map.of("development_history", "菜单可见即保存版本",
                            "development_history_en", "Menu-visible direct-save version"))).getCheckedData();

            assertEquals("瑛泰医疗", published.displayName());
            assertEquals("PUBLISHED", published.status());
            assertTrue(published.live());
            assertEquals("菜单可见即保存版本", published.fields().get("development_history"));
            assertEquals("Menu-visible direct-save version", published.fields().get("development_history_en"));
            assertEquals(liveBaseline.revisionNo() + 1, published.revisionNo());
            assertEquals(published.revisionId(),
                    contentService.requireCurrentCompanyRevision().revisionId());
            assertEquals("Menu-visible direct-save version",
                    adminController.getCompanyCurrent().getCheckedData().fields().get("development_history_en"));
            assertTrue(adminController.getApprovalPage().getCheckedData().isEmpty());
            assertFalse(contentService.versionAudits("COMPANY", liveBaseline.companyId()).isEmpty());
        }
    }

    @Test
    void companyPublishShouldStayWithinCurrentTenantWhenAnotherTenantAlsoHasMainCompany() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(false);
        var tenantOneBaseline = TenantUtils.execute(1L, () -> {
            var draft = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                    null, "MAIN", "租户一公司", "Tenant One Company",
                    Map.of("development_history", "租户一初始版本")));
            return contentService.publishCompanyRevision(draft.revisionId(), 901L);
        });
        var testTenantBaseline = TenantUtils.execute(122L, () -> {
            var draft = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                    null, "MAIN", "测试租户公司", "Test Tenant Company",
                    Map.of("development_history", "测试租户初始版本")));
            return contentService.publishCompanyRevision(draft.revisionId(), 902L);
        });

        var published = TenantUtils.execute(122L, () -> withLoginUser(1131L, () ->
                adminController.publishCompany(new ShowroomAdminController.CompanyDraftReqVO(
                        testTenantBaseline.companyId(), "MAIN", "测试租户公司", "Test Tenant Company",
                        Map.of("development_history", "测试租户发布内容",
                                "development_history_en", "Test tenant published content")))
                        .getCheckedData()));

        assertEquals(testTenantBaseline.companyId(), published.companyId());
        assertTrue(published.live());
        assertEquals(testTenantBaseline.revisionNo() + 1, published.revisionNo());
        assertEquals("测试租户发布内容", published.fields().get("development_history"));

        var testTenantCurrent = TenantUtils.execute(122L,
                () -> adminController.getCompanyCurrent().getCheckedData());
        assertEquals(published.revisionId(), testTenantCurrent.revisionId());
        assertEquals("测试租户发布内容", testTenantCurrent.fields().get("development_history"));

        var tenantOneCurrent = TenantUtils.execute(1L, contentService::requireCurrentCompanyRevision);
        assertEquals(tenantOneBaseline.companyId(), tenantOneCurrent.companyId());
        assertEquals(tenantOneBaseline.revisionId(), tenantOneCurrent.revisionId());
        assertEquals("租户一初始版本", tenantOneCurrent.fields().get("development_history"));

        TenantUtils.execute(1L, () -> {
            IllegalStateException crossTenantRevision = assertThrows(IllegalStateException.class,
                    () -> contentService.getCompanyRevision(published.revisionId()));
            assertTrue(crossTenantRevision.getMessage()
                    .contains("SHOWROOM_TARGET_NOT_FOUND: company revision not found"));
        });
    }

    @Test
    void companyPublishShouldCarryForwardLiveNarrationsWhenSavingNewRevision() throws Exception {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(false);
        var baseline = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "瑛泰", "Yingtai", Map.of(
                "development_history", "初始版本",
                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-carry-forward.png")));
        var liveBaseline = contentService.publishCompanyRevision(baseline.revisionId(), 901L);
        mockFile(6010L, 28L, "showroom/preview/company-carry-forward.png", "image/png");
        mockFile(6011L, 29L, "showroom/narration/company-carry-forward-zh.wav", "audio/wav");
        mockFile(6012L, 29L, "showroom/narration/company-carry-forward-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(), liveBaseline.revisionId(),
                ShowroomNarrationLanguage.ZH, "沿用中文讲解", 6011L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(), liveBaseline.revisionId(),
                ShowroomNarrationLanguage.EN, "Carry forward English narration", 6012L, "ruoxi");

        ShowroomAdminController.CompanyCurrentRespVO published;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            published = adminController.publishCompany(new ShowroomAdminController.CompanyDraftReqVO(
                    liveBaseline.companyId(), "MAIN", "瑛泰医疗", "Yingtai Medical",
                    Map.of("development_history", "保存后新版本",
                            "cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-carry-forward.png")))
                    .getCheckedData();
        }

        var liveZh = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH)).orElseThrow();
        var liveEn = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN)).orElseThrow();
        assertEquals(published.revisionId(), liveZh.sourceRevisionId());
        assertEquals(published.revisionId(), liveEn.sourceRevisionId());
    }

    @Test
    void companyPublishShouldCreateReadableVersionBundleForVersionCenter() throws Exception {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(false);
        var baseline = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "瑛泰", "Yingtai", Map.of(
                "development_history", "初始版本",
                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-version-bundle.png")));
        var liveBaseline = contentService.publishCompanyRevision(baseline.revisionId(), 901L);
        mockFile(6030L, 28L, "showroom/preview/company-version-bundle.png", "image/png");
        mockFile(6031L, 29L, "showroom/narration/company-version-bundle-zh.wav", "audio/wav");
        mockFile(6032L, 29L, "showroom/narration/company-version-bundle-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, liveBaseline.companyId(),
                liveBaseline.revisionId(), 6030L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(), liveBaseline.revisionId(),
                ShowroomNarrationLanguage.ZH, "版本包中文讲解", 6031L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(), liveBaseline.revisionId(),
                ShowroomNarrationLanguage.EN, "Version bundle English narration", 6032L, "ruoxi");

        ShowroomAdminController.CompanyCurrentRespVO published;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            published = adminController.publishCompany(new ShowroomAdminController.CompanyDraftReqVO(
                            liveBaseline.companyId(), "MAIN", "瑛泰医疗", "Yingtai Medical",
                            Map.of("development_history", "公司普通发布生成版本包",
                                    "development_history_en", "Company publish creates version bundle",
                                    "cover_image",
                                    "/admin-api/infra/file/28/get/showroom/preview/company-version-bundle.png")))
                    .getCheckedData();
        }

        var bundle = versionBundleMapper.selectByTargetAndRevision("COMPANY", published.companyId(),
                published.revisionId());
        assertNotNull(bundle);
        assertEquals(published.revisionNo(), bundle.getRevisionNo());
        assertNotNull(bundle.getReleasePreviewAssetVersionId());
        assertNotNull(bundle.getNarrationZhVersionId());
        assertNotNull(bundle.getNarrationEnVersionId());
    }

    @Test
    void companyGetShouldReturnSelectedHistoricalRevisionDetailWithoutOverwritingCurrentLivePointer() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(false);
        var baseline = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "瑛泰", "Yingtai", Map.of(
                "development_history", "历史版本一",
                "development_history_en", "History version one")));
        var liveBaseline = contentService.publishCompanyRevision(baseline.revisionId(), 901L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            adminController.publishCompany(new ShowroomAdminController.CompanyDraftReqVO(
                    liveBaseline.companyId(), "MAIN", "瑛泰医疗", "Yingtai Medical",
                    Map.of("development_history", "当前版本二",
                            "development_history_en", "Current version two"))).getCheckedData();
        }

        var current = adminController.getCompanyCurrent().getCheckedData();
        var historical = adminController.getCompany(liveBaseline.companyId(), liveBaseline.revisionId()).getCheckedData();

        assertEquals(liveBaseline.companyId(), historical.companyId());
        assertEquals(liveBaseline.revisionId(), historical.revisionId());
        assertEquals(liveBaseline.revisionNo(), historical.revisionNo());
        assertEquals("历史版本一", historical.fields().get("development_history"));
        assertEquals("History version one", historical.fields().get("development_history_en"));
        assertFalse(historical.live());
        assertTrue(current.live());
        assertTrue(current.revisionNo() > historical.revisionNo());
        assertEquals(current.revisionId(), contentService.requireCurrentCompanyRevision().revisionId());
    }

    @Test
    void companyRestoreShouldPublishSelectedHistoricalRevisionAndReuseHistoricalNarrations() throws Exception {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(false);
        var baseline = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "瑛泰", "Yingtai", Map.of("development_history", "历史版本一")));
        var liveBaseline = contentService.publishCompanyRevision(baseline.revisionId(), 901L);
        mockFile(6021L, 29L, "showroom/narration/company-restore-v1-zh.wav", "audio/wav");
        mockFile(6022L, 29L, "showroom/narration/company-restore-v1-en.wav", "audio/wav");
        mockFile(6023L, 29L, "showroom/narration/company-restore-v2-zh.wav", "audio/wav");
        mockFile(6024L, 29L, "showroom/narration/company-restore-v2-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(), liveBaseline.revisionId(),
                ShowroomNarrationLanguage.ZH, "历史版本一中文讲解", 6021L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(), liveBaseline.revisionId(),
                ShowroomNarrationLanguage.EN, "History version one narration", 6022L, "ruoxi");

        ShowroomAdminController.CompanyCurrentRespVO currentRevision;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            currentRevision = adminController.publishCompany(new ShowroomAdminController.CompanyDraftReqVO(
                    liveBaseline.companyId(), "MAIN", "瑛泰医疗", "Yingtai Medical",
                    Map.of("development_history", "当前版本二"))).getCheckedData();
        }
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(), currentRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "当前版本二中文讲解", 6023L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(), currentRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "Current version two narration", 6024L, "ruoxi");

        ShowroomAdminController.CompanyCurrentRespVO restored;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1132L);
            restored = adminController.restoreCompanyRevision(
                    new ShowroomAdminController.CompanyRevisionRestoreReqVO(
                            liveBaseline.companyId(), liveBaseline.revisionId()))
                    .getCheckedData();
        }

        assertEquals(liveBaseline.companyId(), restored.companyId());
        assertTrue(restored.live());
        assertEquals("历史版本一", restored.fields().get("development_history"));
        assertEquals(currentRevision.revisionNo() + 1, restored.revisionNo());
        assertEquals(restored.revisionId(), contentService.requireCurrentCompanyRevision().revisionId());

        var liveZh = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH)).orElseThrow();
        var liveEn = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.COMPANY, liveBaseline.companyId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN)).orElseThrow();
        assertEquals(restored.revisionId(), liveZh.sourceRevisionId());
        assertEquals(restored.revisionId(), liveEn.sourceRevisionId());
        assertEquals("历史版本一中文讲解", liveZh.scriptText());
        assertEquals("History version one narration", liveEn.scriptText());
    }

    @Test
    void companyFieldTranslationShouldTranslateRequestedChineseFieldsIntoEnglishFieldKeys() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(false);
        var liveCompany = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "瑛泰医疗", "Yingtai Medical",
                        Map.of("development_history", "公司发展历程",
                                "park_introduction", "园区介绍",
                                "subsidiary_overview", "")))
                .revisionId(), 901L);

        when(narrationTranslationService.translateZhToEn("公司发展历程"))
                .thenReturn("Company development history");
        when(narrationTranslationService.translateZhToEn("园区介绍"))
                .thenReturn("Industrial park introduction");
        when(narrationTranslationService.translateZhToEn("公司发展介绍"))
                .thenReturn("Company development history");
        when(narrationTranslationService.translateZhToEn("公司发展介绍"))
                .thenReturn("Company development history");
        when(narrationTranslationService.translateZhToEn("中文公司语音介绍"))
                .thenReturn("English company narration");

        ShowroomAdminController.CompanyFieldTranslateRespVO translated;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            translated = adminController.translateCompanyFieldsToEn(
                    new ShowroomAdminController.CompanyFieldTranslateReqVO(
                            liveCompany.companyId(),
                            List.of("development_history", "park_introduction", "subsidiary_overview"),
                            Map.of("development_history", "公司发展历程",
                                    "park_introduction", "园区介绍",
                                    "subsidiary_overview", ""),
                            "中文公司语音介绍"))
                    .getCheckedData();
        }

        assertEquals(liveCompany.companyId(), translated.companyId());
        assertEquals("Company development history", translated.translatedFields().get("development_history_en"));
        assertEquals("Industrial park introduction", translated.translatedFields().get("park_introduction_en"));
        assertFalse(translated.translatedFields().containsKey("subsidiary_overview_en"));
        assertEquals("English company narration", translated.introTextEn());
        verify(narrationTranslationService).translateZhToEn("公司发展历程");
        verify(narrationTranslationService).translateZhToEn("园区介绍");
        verify(narrationTranslationService).translateZhToEn("中文公司语音介绍");
    }

    @Test
    void productFieldTranslationShouldTranslateChineseFieldsAndNarrationIntoEnglishDrafts() throws Exception {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(false);
        var liveRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "YT-BI-001", "双语导管产品", "Bilingual Product",
                        Map.of(
                                "target_market", "冠脉市场",
                                "pipeline_layout", "心血管BU",
                                "core_selling_points", "中国;欧盟",
                                "model_specification", "三通旋塞",
                                "registration_certificate", "注册证中文",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/product-cover.png")))
                .revisionId(), 901L);

        when(productNarrationCodexService.translateZhToEn("双语导管产品"))
                .thenReturn("Bilingual Product");
        when(productNarrationCodexService.translateZhToEn("冠脉市场"))
                .thenReturn("Coronary market");
        when(productNarrationCodexService.translateZhToEn("心血管BU"))
                .thenReturn("Cardiovascular BU");
        when(productNarrationCodexService.translateZhToEn("中国;欧盟"))
                .thenReturn("China; European Union");
        when(productNarrationCodexService.translateZhToEn("三通旋塞"))
                .thenReturn("Three-way stopcock");
        when(productNarrationCodexService.translateZhToEn("注册证中文"))
                .thenReturn("Registration certificate in English");
        when(productNarrationCodexService.translateZhToEn("翰凌讲解稿"))
                .thenReturn("Healing narration script");
        when(productNarrationCodexService.translateZhToEn("中文讲解稿"))
                .thenReturn("English narration script");

        Object translated;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            Method translateMethod = java.util.Arrays.stream(ShowroomAdminController.class.getDeclaredMethods())
                    .filter(method -> "translateProductFieldsToEn".equals(method.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("missing translateProductFieldsToEn route"));
            Object reqVO = translateMethod.getParameterTypes()[0].getDeclaredConstructors()[0].newInstance(
                    liveRevision.productId(),
                    "双语导管产品",
                    Map.of(
                            "target_market", "冠脉市场",
                            "pipeline_layout", "心血管BU",
                            "core_selling_points", "中国;欧盟",
                            "model_specification", "三通旋塞",
                            "registration_certificate", "注册证中文",
                            "cover_image", "/admin-api/infra/file/28/get/showroom/preview/product-cover.png"),
                    "翰凌讲解稿");
            @SuppressWarnings("unchecked")
            cn.iocoder.yudao.framework.common.pojo.CommonResult<Object> result =
                    (cn.iocoder.yudao.framework.common.pojo.CommonResult<Object>) translateMethod.invoke(adminController, reqVO);
            translated = result.getCheckedData();
        }

        Long productId = (Long) translated.getClass().getMethod("productId").invoke(translated);
        String nameEn = (String) translated.getClass().getMethod("nameEn").invoke(translated);
        @SuppressWarnings("unchecked")
        Map<String, String> translatedFields =
                (Map<String, String>) translated.getClass().getMethod("translatedFields").invoke(translated);
        String narrationScriptEn =
                (String) translated.getClass().getMethod("narrationScriptEn").invoke(translated);

        assertEquals(liveRevision.productId(), productId);
        assertEquals("Bilingual Product", nameEn);
        assertEquals("Coronary market", translatedFields.get("target_market_en"));
        assertEquals("Cardiovascular BU", translatedFields.get("pipeline_layout_en"));
        assertEquals("China; European Union", translatedFields.get("core_selling_points_en"));
        assertEquals("Three-way stopcock", translatedFields.get("model_specification_en"));
        assertEquals("Registration certificate in English", translatedFields.get("registration_certificate_en"));
        assertFalse(translatedFields.containsKey("cover_image_en"));
        assertEquals("Healing narration script", narrationScriptEn);
    }

    @Test
    void companyNarrationGenerateAndPublishShouldAllowLoggedInUserWithoutChangingCompanyRevision() throws Exception {
        var baseline = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "瑛泰", "Yingtai", Map.of("development_history", "公司发展介绍")));
        var liveCompany = contentService.publishCompanyRevision(baseline.revisionId(), 901L);
        Long revisionIdBeforeNarration = liveCompany.revisionId();
        int revisionNoBeforeNarration = liveCompany.revisionNo();
        String manualEnglishNarration = "Manually edited English narration";

        when(securityFrameworkService.hasRole(anyString())).thenReturn(false);
        when(narrationCodexService.generateScript("MAIN", "瑛泰", Map.of("development_history", "公司发展介绍"), 180))
                .thenReturn("中文公司语音介绍");
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("中文公司语音介绍", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(2));
        when(aliyunNlsTtsSynthesizer.synthesize(manualEnglishNarration, tts, "ruoxi", "saved-token",
                "saved-appkey"))
                .thenReturn(buildSilentWavBytes(3));
        when(fileService.createFileAndReturnId(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.contains("company-" + liveCompany.companyId()),
                org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav")))
                .thenReturn(99311L)
                .thenReturn(99312L);

        FileDO zhAudioFile = FileDO.builder()
                .id(99311L)
                .configId(29L)
                .name("company-zh-ruoxi.wav")
                .path("showroom/narration/generated/company-zh-ruoxi.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/generated/company-zh-ruoxi.wav")
                .type("audio/wav")
                .size(500L)
                .build();
        FileDO enAudioFile = FileDO.builder()
                .id(99312L)
                .configId(29L)
                .name("company-en-ruoxi.wav")
                .path("showroom/narration/generated/company-en-ruoxi.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/generated/company-en-ruoxi.wav")
                .type("audio/wav")
                .size(700L)
                .build();
        when(fileMapper.selectById(99311L)).thenReturn(zhAudioFile);
        when(fileMapper.selectById(99312L)).thenReturn(enAudioFile);

        ShowroomAdminController.CompanyNarrationScriptGenerateRespVO generatedScript;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            generatedScript = adminController.generateCompanyNarrationScript(
                    new ShowroomAdminController.CompanyNarrationScriptGenerateReqVO(
                            liveCompany.companyId(), liveCompany.revisionId(), "MAIN", "瑛泰",
                            Map.of("development_history", "公司发展介绍"), 180))
                    .getCheckedData();
        }

        assertEquals(liveCompany.companyId(), generatedScript.companyId());
        assertEquals("中文公司语音介绍", generatedScript.introTextZh());

        when(narrationTranslationService.translateZhToEn("公司发展介绍"))
                .thenReturn("Company development history");
        when(narrationTranslationService.translateZhToEn("中文公司语音介绍"))
                .thenReturn("English company narration");
        ShowroomAdminController.CompanyFieldTranslateRespVO translated;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            translated = adminController.translateCompanyFieldsToEn(
                    new ShowroomAdminController.CompanyFieldTranslateReqVO(
                            liveCompany.companyId(),
                            List.of("development_history"),
                            Map.of("development_history", "公司发展介绍"),
                            generatedScript.introTextZh()))
                    .getCheckedData();
        }

        assertEquals("English company narration", translated.introTextEn());

        ShowroomAdminController.CompanyNarrationGenerateRespVO generatedZh;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            generatedZh = adminController.generateCompanyNarrationAudio(
                    new ShowroomAdminController.CompanyNarrationGenerateReqVO(
                            liveCompany.companyId(), liveCompany.revisionId(), "ZH", "中文公司语音介绍"))
                    .getCheckedData();
        }

        ShowroomAdminController.CompanyNarrationGenerateRespVO generatedEn;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            generatedEn = adminController.generateCompanyNarrationAudio(
                    new ShowroomAdminController.CompanyNarrationGenerateReqVO(
                            liveCompany.companyId(), liveCompany.revisionId(), "EN", manualEnglishNarration))
                    .getCheckedData();
        }

        assertEquals(liveCompany.companyId(), generatedZh.companyId());
        assertEquals("中文公司语音介绍", generatedZh.scriptText());
        assertEquals("ruoxi", generatedZh.voice());
        assertEquals("ZH", generatedZh.narration().language());
        assertEquals(99311L, generatedZh.narration().audioFileId());

        assertEquals(liveCompany.companyId(), generatedEn.companyId());
        assertEquals(manualEnglishNarration, generatedEn.scriptText());
        assertEquals("ruoxi", generatedEn.voice());
        assertEquals("EN", generatedEn.narration().language());
        assertEquals(99312L, generatedEn.narration().audioFileId());

        ShowroomAdminController.CompanyNarrationPublishRespVO published;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            published = adminController.publishCompanyNarration(
                    new ShowroomAdminController.CompanyNarrationPublishReqVO(
                            generatedZh.narration().narrationVersionId(),
                            generatedEn.narration().narrationVersionId()))
                    .getCheckedData();
        }

        assertEquals(liveCompany.companyId(), published.companyId());
        var zhLive = displayController.getNarration("COMPANY", liveCompany.companyId(), "PUBLIC", "ZH")
                .getCheckedData();
        var enLive = displayController.getNarration("COMPANY", liveCompany.companyId(), "PUBLIC", "EN")
                .getCheckedData();
        assertEquals("中文公司语音介绍", zhLive.text());
        assertEquals(manualEnglishNarration, enLive.text());
        assertEquals("/admin-api/infra/file/29/get/showroom/narration/generated/company-zh-ruoxi.wav",
                zhLive.audioUrl());
        assertEquals("/admin-api/infra/file/29/get/showroom/narration/generated/company-en-ruoxi.wav",
                enLive.audioUrl());

        var companyAfterNarration = adminController.getCompanyCurrent().getCheckedData();
        assertEquals(revisionIdBeforeNarration, companyAfterNarration.revisionId());
        assertEquals(revisionNoBeforeNarration, companyAfterNarration.revisionNo());
        assertTrue(withLoginUser(1131L, () -> adminController.getApprovalPage()).getCheckedData().isEmpty());
    }

    @Test
    void companyNarrationPublishShouldAllowSingleLanguageDraft() throws Exception {
        var baseline = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "瑛泰", "Yingtai", Map.of("development_history", "公司发展介绍")));
        var liveCompany = contentService.publishCompanyRevision(baseline.revisionId(), 901L);

        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("中文公司语音介绍", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(2));
        when(fileService.createFileAndReturnId(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.contains("company-" + liveCompany.companyId()),
                org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav")))
                .thenReturn(99411L);
        FileDO zhAudioFile = FileDO.builder()
                .id(99411L)
                .configId(29L)
                .name("company-zh-ruoxi.wav")
                .path("showroom/narration/generated/company-zh-ruoxi.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/generated/company-zh-ruoxi.wav")
                .type("audio/wav")
                .size(500L)
                .build();
        when(fileMapper.selectById(99411L)).thenReturn(zhAudioFile);

        ShowroomAdminController.CompanyNarrationGenerateRespVO generatedZh;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            generatedZh = adminController.generateCompanyNarrationAudio(
                    new ShowroomAdminController.CompanyNarrationGenerateReqVO(
                            liveCompany.companyId(), liveCompany.revisionId(), "ZH", "中文公司语音介绍"))
                    .getCheckedData();
        }

        ShowroomAdminController.CompanyNarrationPublishRespVO published;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1131L);
            published = adminController.publishCompanyNarration(
                    new ShowroomAdminController.CompanyNarrationPublishReqVO(
                            generatedZh.narration().narrationVersionId(), null))
                    .getCheckedData();
        }

        assertEquals(liveCompany.companyId(), published.companyId());
        assertEquals(generatedZh.narration().narrationVersionId(), published.zhNarrationVersionId());
        assertEquals(null, published.enNarrationVersionId());
    }

    @Test
    void publicityPublishProductShouldReuseCurrentRevisionBilingualNarrationCloseWholeAssignmentAndSkipApproval() throws Exception {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();
        var liveRevision = publishBaselineProduct();
        mockFile(98099L, 28L, "showroom/preview/live/product-preview.png", "image/png");
        mockFile(98101L, 29L, "showroom/narration/live/product-zh.wav", "audio/wav");
        mockFile(98102L, 29L, "showroom/narration/live/product-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, liveRevision.productId(),
                liveRevision.revisionId(), 98099L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), liveRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "当前 live 中文讲解", 98101L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), liveRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "Current live English narration", 98102L, "ruoxi");

        ShowroomFieldAssignment assignment = withLoginUser(300L, () -> adminController.createAssignment(
                new ShowroomAdminController.AssignmentCreateReqVO(
                        "PRODUCT", liveRevision.productId(),
                        ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 300L)))
                .getCheckedData();

        mockProductNarrationAudioGeneration(liveRevision.productId(), "当前 live 中文讲解",
                "Current live English narration", 98211L, 98212L);

        var published = withLoginUser(300L, () -> adminController.publishProduct(
                new ShowroomAdminController.ProductPublishReqVO(
                        liveRevision.productId(), "YT-GW-001", "企宣直发产品 V2", "Introducer Sheath Set",
                        productFieldsWithCoreAndBaselineCover(Map.of(
                                "target_market", "企宣直发市场",
                                "registration_certificate", "注册证 V1",
                                "core_selling_points", "企宣直发卖点"
                        )),
                        liveRevision.revisionId(),
                        null,
                        false)))
                .getCheckedData();

        assertEquals("PUBLISHED", published.status());
        assertTrue(published.live());
        assertTrue(published.editable());
        assertEquals(liveRevision.revisionNo() + 1, published.revisionNo());
        assertEquals(published.revisionId(), published.currentRevisionId());
        assertEquals(published.revisionId(),
                contentService.requireCurrentProductRevision(liveRevision.productId()).revisionId());
        assertTrue(changeRequestMapper.selectListByTarget("PRODUCT", liveRevision.productId()).isEmpty());

        ShowroomFieldAssignmentDO assignmentDO = assignmentMapper.selectById(assignment.assignmentId());
        assertEquals("AUTO_SUBMITTED", assignmentDO.getStatus());
        assertEquals(published.revisionId(), assignmentDO.getLastSavedRevisionId());
        assertEquals(null, assignmentDO.getLastChangeRequestId());
        assertNotNull(assignmentDO.getSubmittedAt());
        assertNotNull(assignmentDO.getClosedAt());

        var row = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData()
                .getList().stream()
                .filter(item -> item.productId().equals(liveRevision.productId()))
                .findFirst()
                .orElseThrow();
        assertEquals("PUBLISHED", row.revision().status());
        assertEquals(published.revisionId(), row.displayRevision().revisionId());
        assertEquals("企宣直发产品 V2", row.displayRevision().nameCn());
        assertEquals("PUBLISHED", row.displayRevision().status());
        assertEquals(null, row.revision().activeAssignment());

        var zhNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var enNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();
        assertEquals(published.revisionId(), zhNarration.sourceRevisionId());
        assertEquals(published.revisionId(), enNarration.sourceRevisionId());
        assertEquals("当前 live 中文讲解", zhNarration.scriptText());
        assertEquals("Current live English narration", enNarration.scriptText());
        assertEquals(98211L, zhNarration.audioFileId());
        assertEquals(98212L, enNarration.audioFileId());

        var livePreview = previewAssetService.live(new ShowroomPreviewAssetKey(
                ShowroomPreviewAssetTargetType.PRODUCT, liveRevision.productId())).orElseThrow();
        assertEquals(liveRevision.revisionId(), livePreview.sourceRevisionId());
        assertEquals(98099L, livePreview.files().desktopFileId());
        var versionBundle = versionBundleMapper.selectByTargetAndRevision("PRODUCT", liveRevision.productId(),
                published.revisionId());
        assertNotNull(versionBundle);
        assertEquals(published.revisionNo(), versionBundle.getRevisionNo());
        assertEquals(null, versionBundle.getReleasePreviewAssetVersionId());
        assertEquals(zhNarration.id(), versionBundle.getNarrationZhVersionId());
        assertEquals(enNarration.id(), versionBundle.getNarrationEnVersionId());
    }

    @Test
    void publicityPublishProductShouldUseCurrentRevisionEditedEnglishNarrationWhenPublishing() throws Exception {
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();
        var liveRevision = publishBaselineProduct();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "企宣新的中文讲解稿", null, null, true)).getCheckedData();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "EN",
                "Publicity English narration manual", null, null, true)).getCheckedData();
        mockProductNarrationAudioGeneration(liveRevision.productId(), "企宣新的中文讲解稿",
                "Publicity English narration manual", 98311L, 98312L);

        var published = withLoginUser(300L, () -> adminController.publishProduct(
                new ShowroomAdminController.ProductPublishReqVO(
                        liveRevision.productId(), "YT-GW-001", "企宣直发产品 V3", "Introducer Sheath Set",
                        productFieldsWithCoreAndBaselineCover(Map.of(
                                "target_market", "企宣带讲解稿直发市场",
                                "registration_certificate", "注册证 V1",
                                "core_selling_points", "企宣带讲解稿直发卖点"
                        )),
                        liveRevision.revisionId(),
                        null,
                        false)))
                .getCheckedData();

        assertEquals("PUBLISHED", published.status());
        assertEquals(published.revisionId(),
                contentService.requireCurrentProductRevision(liveRevision.productId()).revisionId());

        var zhNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var enNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();
        assertEquals(published.revisionId(), zhNarration.sourceRevisionId());
        assertEquals(published.revisionId(), enNarration.sourceRevisionId());
        assertEquals("企宣新的中文讲解稿", zhNarration.scriptText());
        assertEquals("Publicity English narration manual", enNarration.scriptText());
        assertTrue(zhNarration.generatedByAi());
        assertTrue(enNarration.generatedByAi());
        assertEquals(98311L, zhNarration.audioFileId());
        assertEquals(98312L, enNarration.audioFileId());
    }

    @Test
    void publicityPublishProductShouldRejectNonPublicityUser() {
        mockShowroomRoleChecks();
        var liveRevision = publishBaselineProduct();

        IllegalStateException denied = assertThrows(IllegalStateException.class,
                () -> withLoginUser(100L, () -> adminController.publishProduct(
                        new ShowroomAdminController.ProductPublishReqVO(
                                liveRevision.productId(), "YT-GW-001", "越权直发产品", "Introducer Sheath Set",
                                Map.of(
                                        "target_market", "越权市场",
                                        "registration_certificate", "注册证 V1",
                                        "core_selling_points", "越权卖点"
                                ),
                                liveRevision.revisionId(),
                                null,
                                false))));

        assertTrue(denied.getMessage().contains("SHOWROOM_PRODUCT_ACCESS_DENIED"));
    }

    @Test
    void publicityPublishProductShouldFailWhenCurrentRevisionEnglishNarrationMissing() {
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();
        var liveRevision = publishBaselineProduct();

        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "只有中文讲解稿", null, null, false)).getCheckedData();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> withLoginUser(300L, () -> adminController.publishProduct(
                        new ShowroomAdminController.ProductPublishReqVO(
                                liveRevision.productId(), "YT-GW-001", "缺英文讲解产品", "Introducer Sheath Set",
                                productFieldsWithCoreAndBaselineCover(Map.of(
                                        "target_market", "缺英文讲解市场",
                                        "registration_certificate", "注册证 V1",
                                        "core_selling_points", "缺英文讲解卖点"
                                )),
                                liveRevision.revisionId(),
                                null,
                                false))));

        assertTrue(exception.getMessage().contains("EN")
                || exception.getMessage().contains("英文"));
    }

    @Test
    void batchPublishProductsShouldReuseCurrentPublishedNarrationWhenLatestRevisionOnlyChangesFields() throws Exception {
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();
        var liveRevision = publishBaselineProduct();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "batch publish reused zh narration", null, null, false)).getCheckedData();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "EN",
                "Batch publish reused English narration", null, null, false)).getCheckedData();

        var latestDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "Introducer Sheath Set V2", "Introducer Sheath Set",
                productFieldsWithCoreAndBaselineCover(Map.of("target_market", "Batch publish updated market",
                        "registration_certificate", "Certificate V2",
                        "core_selling_points", "Batch publish updated selling points"))));

        mockProductNarrationAudioGeneration(liveRevision.productId(), "batch publish reused zh narration",
                "Batch publish reused English narration", 98411L, 98412L);

        var summary = withLoginUser(300L, () -> adminController.batchPublishProducts(
                new ShowroomAdminController.ProductBatchGenerateReqVO("YT-GW-001", null, null, null)))
                .getCheckedData();

        assertEquals(1, summary.matchedCount());
        assertEquals(1, summary.publishedCount());
        assertEquals(0, summary.skippedUnpublishedCount());
        assertEquals(1, summary.succeededCount());
        assertEquals(0, summary.failedCount());
        assertTrue(summary.failures().isEmpty());

        var currentRevision = contentService.requireCurrentProductRevision(liveRevision.productId());
        assertTrue(currentRevision.revisionNo() > latestDraft.revisionNo());
        assertEquals("Batch publish updated market", currentRevision.fields().get("target_market"));
        assertEquals("Certificate V2", currentRevision.fields().get("registration_certificate"));
        assertEquals("Batch publish updated selling points", currentRevision.fields().get("core_selling_points"));

        var zhNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var enNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();
        assertEquals(currentRevision.revisionId(), zhNarration.sourceRevisionId());
        assertEquals(currentRevision.revisionId(), enNarration.sourceRevisionId());
        assertEquals("batch publish reused zh narration", zhNarration.scriptText());
        assertEquals("Batch publish reused English narration", enNarration.scriptText());
        assertEquals(98411L, zhNarration.audioFileId());
        assertEquals(98412L, enNarration.audioFileId());
    }

    @Test
    void publicityPublishProductShouldRollbackWhenNarrationAudioGenerationFails() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();
        var liveRevision = publishBaselineProduct();
        ShowroomFieldAssignment assignment = withLoginUser(300L, () -> adminController.createAssignment(
                new ShowroomAdminController.AssignmentCreateReqVO(
                        "PRODUCT", liveRevision.productId(),
                        ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 300L)))
                .getCheckedData();

        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "企宣失败中文讲解稿", null, null, true)).getCheckedData();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "EN",
                "Failure English narration", null, null, true)).getCheckedData();
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("企宣失败中文讲解稿", tts, "ruoxi",
                "saved-token", "saved-appkey"))
                .thenThrow(new RuntimeException("tts broken"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> withLoginUser(300L, () -> adminController.publishProduct(
                        new ShowroomAdminController.ProductPublishReqVO(
                                liveRevision.productId(), "YT-GW-001", "企宣直发失败产品", "Introducer Sheath Set",
                                productFieldsWithCoreAndBaselineCover(Map.of(
                                "target_market", "失败市场",
                                "registration_certificate", "注册证 V1",
                                "core_selling_points", "失败卖点"
                                )),
                                liveRevision.revisionId(),
                                null,
                                false))));

        assertTrue(exception.getMessage().contains("SHOWROOM_AUDIO_GENERATION_FAILED")
                || exception.getMessage().contains("tts broken"));
        assertEquals(liveRevision.revisionId(),
                contentService.requireCurrentProductRevision(liveRevision.productId()).revisionId());
        assertTrue(changeRequestMapper.selectListByTarget("PRODUCT", liveRevision.productId()).isEmpty());

        ShowroomFieldAssignmentDO assignmentDO = assignmentMapper.selectById(assignment.assignmentId());
        assertEquals("OPEN", assignmentDO.getStatus());
        assertEquals(null, assignmentDO.getClosedAt());
    }

    @Test
    void approvalGetShouldReturnDiffRichDetailInsteadOfBareRequest() {
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 V2", "Introducer Sheath Set",
                Map.of("target_market", "新市场", "registration_certificate", "注册证 V2")));

        var submitted = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                draftRevision.productId(), draftRevision.revisionId(),
                List.of("name_cn", "target_market", "registration_certificate"),
                "product", 100L, 8L, 200L, null))).getCheckedData();

        ShowroomApprovalDetail detail = withLoginUser(200L,
                () -> adminController.getApproval(submitted.changeRequestId())).getCheckedData();

        assertEquals(submitted.changeRequestId(), detail.changeRequest().changeRequestId());
        assertEquals("PENDING_SUPERVISOR_REVIEW", detail.changeRequest().status());
        assertEquals(300L, detail.changeRequest().gaoxinUserId());
        assertEquals("CONTENT_UPDATE", detail.changeRequest().requestType());
        assertEquals("MANUAL", detail.changeRequest().submissionSource());
        assertEquals(3, detail.fieldDiffs().size());
        assertEquals("PENDING", detail.fieldDiffs().get(0).approvalStatus());
        assertEquals(liveRevision.revisionId(), detail.targetPreview().liveRevisionId());
        assertEquals(draftRevision.revisionId(), detail.targetPreview().targetRevisionId());
        assertEquals("导管鞘组 V1", detail.targetPreview().liveFields().get("name_cn"));
        assertEquals("导管鞘组 V2", detail.targetPreview().targetFields().get("name_cn"));
        assertTrue(detail.versionDiffs().stream().anyMatch(audit -> "name_cn".equals(audit.fieldCode())));
    }

    @Test
    void approvalDetailShouldExposeHumanizedFieldDiffAndPreviewValues() {
        var liveCompany = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "审批展示测试公司", "Approval Demo Company",
                        Map.of("development_history", "公司发展历程")))
                .revisionId(), 901L);
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 可读版", "Introducer Sheath Set",
                Map.of(
                        "owner_company_id", String.valueOf(liveCompany.companyId()),
                        "product_owner_type", "SUBSIDIARY",
                        "lifecycle_stage", "REGISTERED",
                        "target_market", "审批展示目标市场",
                        "registration_certificate", "注册证 V2",
                        "core_selling_points", "审批展示卖点"
                )));

        var submitted = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                draftRevision.productId(), draftRevision.revisionId(),
                List.of("name_cn", "owner_company_id", "product_owner_type", "lifecycle_stage"),
                "product", 100L, 8L, 200L, null))).getCheckedData();

        ShowroomApprovalDetail detail = withLoginUser(200L,
                () -> adminController.getApproval(submitted.changeRequestId())).getCheckedData();

        var nameDiff = detail.fieldDiffs().stream()
                .filter(item -> "name_cn".equals(item.fieldCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("中文名称", nameDiff.label());
        assertEquals("导管鞘组 V1", nameDiff.oldValue());
        assertEquals("导管鞘组 可读版", nameDiff.newValue());

        var lifecycleDiff = detail.fieldDiffs().stream()
                .filter(item -> "lifecycle_stage".equals(item.fieldCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("生命周期", lifecycleDiff.label());
        assertEquals("空", lifecycleDiff.oldValue());
        assertEquals("已注册", lifecycleDiff.newValue());

        var ownerDiff = detail.fieldDiffs().stream()
                .filter(item -> "owner_company_id".equals(item.fieldCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("所属公司", ownerDiff.label());
        assertEquals("空", ownerDiff.oldValue());
        assertEquals("审批展示测试公司", ownerDiff.newValue());

        var ownerPreview = detail.targetPreview().rows().stream()
                .filter(row -> "owner_company_id".equals(row.fieldCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("所属公司", ownerPreview.label());
        assertEquals("空", ownerPreview.liveValue());
        assertEquals("审批展示测试公司", ownerPreview.targetValue());

        var lifecyclePreview = detail.targetPreview().rows().stream()
                .filter(row -> "lifecycle_stage".equals(row.fieldCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("生命周期", lifecyclePreview.label());
        assertEquals("空", lifecyclePreview.liveValue());
        assertEquals("已注册", lifecyclePreview.targetValue());
    }

    @Test
    void productWorkflowShouldCreateNotifyMessagesForReviewersAndSubmitter() {
        seedPublicityApproverRole(300L);
        seedWorkflowNotifyTemplates();
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 Notify", "Introducer Sheath Set",
                productFieldsWithBaselineCover(Map.of("target_market", "通知市场"))));
        mockFile(98501L, 29L, "showroom/narration/product-workflow-zh.wav", "audio/wav");
        mockFile(98502L, 29L, "showroom/narration/product-workflow-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), draftRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "工作流产品中文讲解", 98501L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), draftRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "Workflow product English narration", 98502L, "ruoxi");

        var submitted = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                draftRevision.productId(), draftRevision.revisionId(), List.of("name_cn", "target_market"),
                "product", 100L, 8L, 200L, null))).getCheckedData();

        NotifyMessageDO supervisorNotify = latestNotifyMessageForUser(200L, PENDING_APPROVAL_TEMPLATE_CODE);
        assertEquals("主管审核", String.valueOf(supervisorNotify.getTemplateParams().get("approvalStage")));
        assertEquals("PRODUCT", String.valueOf(supervisorNotify.getTemplateParams().get("targetType")));
        assertEquals(liveRevision.productId(), asLong(supervisorNotify.getTemplateParams().get("targetId")));
        assertEquals(submitted.changeRequestId(),
                asLong(supervisorNotify.getTemplateParams().get("changeRequestId")));
        assertEquals("导管鞘组 Notify", String.valueOf(supervisorNotify.getTemplateParams().get("targetName")));
        assertEquals("list", String.valueOf(supervisorNotify.getTemplateParams().get("notifyOpen")));

        adminController.supervisorApprove(new ShowroomAdminController.ApprovalActionReqVO(
                submitted.changeRequestId(), 200L, "111111", "主管签名通过"));

        NotifyMessageDO publicityNotify = latestNotifyMessageForUser(300L, PENDING_APPROVAL_TEMPLATE_CODE);
        assertEquals("企宣审批", String.valueOf(publicityNotify.getTemplateParams().get("approvalStage")));
        assertEquals(submitted.changeRequestId(),
                asLong(publicityNotify.getTemplateParams().get("changeRequestId")));
        assertEquals(liveRevision.productId(), asLong(publicityNotify.getTemplateParams().get("targetId")));
        assertEquals("list", String.valueOf(publicityNotify.getTemplateParams().get("notifyOpen")));

        adminController.gaoxinApprove(new ShowroomAdminController.ApprovalActionReqVO(
                submitted.changeRequestId(), 300L, "111111", "企宣签名通过"));

        NotifyMessageDO submitterNotify = latestNotifyMessageForUser(100L, PUBLISHED_APPROVAL_TEMPLATE_CODE);
        assertEquals("已发布", String.valueOf(submitterNotify.getTemplateParams().get("approvalStage")));
        assertEquals(submitted.changeRequestId(),
                asLong(submitterNotify.getTemplateParams().get("changeRequestId")));
        assertEquals(liveRevision.productId(), asLong(submitterNotify.getTemplateParams().get("targetId")));
        assertEquals("导管鞘组 Notify", String.valueOf(submitterNotify.getTemplateParams().get("targetName")));
    }

    @Test
    void productSubmitWithoutDeptShouldNotifyPublicityApproverDirectly() {
        seedPublicityApproverRole(300L);
        seedWorkflowNotifyTemplates();
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 Direct Notify", "Introducer Sheath Set",
                Map.of("target_market", "直达企宣市场")));

        var submitted = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                draftRevision.productId(), draftRevision.revisionId(), List.of("target_market"),
                "product", 100L, null, null, null))).getCheckedData();

        NotifyMessageDO publicityNotify = latestNotifyMessageForUser(300L, PENDING_APPROVAL_TEMPLATE_CODE);
        assertEquals("企宣审批", String.valueOf(publicityNotify.getTemplateParams().get("approvalStage")));
        assertEquals(submitted.changeRequestId(),
                asLong(publicityNotify.getTemplateParams().get("changeRequestId")));
        assertEquals(liveRevision.productId(), asLong(publicityNotify.getTemplateParams().get("targetId")));
    }

    @Test
    void productPageShouldExposePublishedDisplayRevisionWhileDetailCanReadHistoricalRevision() {
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 Draft", "Introducer Sheath Set",
                Map.of("target_market", "草稿市场")));

        var latestDetail = withLoginUser(300L, () -> adminController.getProduct(liveRevision.productId(), null))
                .getCheckedData();
        assertEquals(liveRevision.revisionId(), latestDetail.currentRevisionId());
        assertEquals(draftRevision.revisionId(), latestDetail.revisionId());
        assertEquals("导管鞘组 Draft", latestDetail.nameCn());
        assertEquals("DRAFT", latestDetail.status());

        var publishedDetail = withLoginUser(300L,
                () -> adminController.getProduct(liveRevision.productId(), liveRevision.revisionId()))
                .getCheckedData();
        assertEquals(liveRevision.revisionId(), publishedDetail.currentRevisionId());
        assertEquals(liveRevision.revisionId(), publishedDetail.revisionId());
        assertEquals("导管鞘组 V1", publishedDetail.nameCn());
        assertEquals("PUBLISHED", publishedDetail.status());
        assertFalse(publishedDetail.editable());

        var page = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();
        var row = page.getList().stream()
                .filter(item -> item.productId().equals(liveRevision.productId()))
                .findFirst()
                .orElseThrow();
        assertEquals(liveRevision.revisionId(), row.currentRevisionId());
        assertEquals(draftRevision.revisionId(), row.revision().revisionId());
        assertEquals("DRAFT", row.revision().status());
        assertEquals(liveRevision.revisionId(), row.displayRevision().revisionId());
        assertEquals("导管鞘组 V1", row.displayRevision().nameCn());
        assertEquals("PUBLISHED", row.displayRevision().status());
    }

    @Test
    void productSubmitShouldAllowWholeDraftDiffWithoutExplicitFieldCodes() {
        seedPublicityApproverRole(300L);
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 Full Draft", "Introducer Sheath Set",
                Map.of("target_market", "整版草稿市场", "registration_certificate", "整版注册证")));

        var submitted = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                draftRevision.productId(), draftRevision.revisionId(), List.of(),
                "product", 100L, 8L, 200L, null))).getCheckedData();

        assertEquals("PENDING_SUPERVISOR_REVIEW", submitted.status());
        assertEquals(300L, submitted.gaoxinUserId());
        assertTrue(submitted.items().stream().anyMatch(item -> "name_cn".equals(item.fieldCode())));
        assertTrue(submitted.items().stream().anyMatch(item -> "target_market".equals(item.fieldCode())));
        assertTrue(submitted.items().stream()
                .anyMatch(item -> "registration_certificate".equals(item.fieldCode())));
    }

    @Test
    void productPageShouldReturnTotalAndRespectRequestedPageSlice() {
        for (int i = 1; i <= 21; i++) {
            var draft = contentService.saveProductDraft(new ShowroomProductDraft(
                    null, "YT-PAGE-" + i, "分页产品 " + i, "Paged Product " + i,
                    Map.of("target_market", "分页市场 " + i, "registration_certificate", "分页证书 " + i,
                            "core_selling_points", "分页卖点 " + i)));
            contentService.publishProductRevision(draft.revisionId(), 920L + i);
        }

        var firstPage = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO("YT-PAGE-", 1, 20, null, null, null, null, null)))
                .getCheckedData();
        var secondPage = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO("YT-PAGE-", 2, 20, null, null, null, null, null)))
                .getCheckedData();

        assertEquals(21L, firstPage.getTotal());
        assertEquals(20, firstPage.getList().size());
        assertEquals(21L, secondPage.getTotal());
        assertEquals(1, secondPage.getList().size());
    }

    @Test
    void productPageShouldMarkRowsMissingOwnershipFieldsAsIncomplete() {
        mockShowroomRoleChecks();
        var published = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(
                        null,
                        "YT-MISSING-OWNER-001",
                        "缺少归属字段产品",
                        "Missing Ownership Fields Product",
                        Map.of(
                                "target_market", "缺少归属字段市场",
                                "core_selling_points", "缺少归属字段卖点")))
                .revisionId(), 901L);

        var page = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(
                        "YT-MISSING-OWNER-001", 1, 20, null, null, null, null, null)))
                .getCheckedData();
        var row = page.getList().stream()
                .filter(item -> item.productId().equals(published.productId()))
                .findFirst()
                .orElseThrow();

        assertTrue(row.incomplete());
        assertTrue(row.revision().incomplete());
        assertTrue(row.displayRevision().incomplete());
    }

    @Test
    void hallProductOptionsShouldReturnCandidateRowsWithHallIdsInSingleCall() {
        var productA = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "YT-HALL-OPT-001", "展柜候选产品 A", "Hall Option Product A",
                        Map.of("target_market", "候选市场 A", "registration_certificate", "候选证书 A",
                                "core_selling_points", "候选卖点 A", "cover_image", "/admin-api/infra/file/28/get/showroom/product/a.png"))).revisionId(), 901L);
        var productB = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "YT-HALL-OPT-002", "展柜候选产品 B", "Hall Option Product B",
                        Map.of("target_market", "候选市场 B", "registration_certificate", "候选证书 B",
                                "core_selling_points", "候选卖点 B"))).revisionId(), 902L);
        var hallOne = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "HALL_OPT_A", "候选展柜 A", "Hall Option A", "候选展柜 A 说明", "Hall Option A description")).getCheckedData();
        var hallTwo = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "HALL_OPT_B", "候选展柜 B", "Hall Option B", "候选展柜 B 说明", "Hall Option B description")).getCheckedData();

        adminController.updateHallProductMapping(new ShowroomAdminController.HallMappingReqVO(
                hallOne.hallId(), List.of(new ShowroomAdminController.HallProductMappingReqVO(productA.productId(), 1))));
        adminController.updateHallProductMapping(new ShowroomAdminController.HallMappingReqVO(
                hallTwo.hallId(), List.of(new ShowroomAdminController.HallProductMappingReqVO(productA.productId(), 1))));

        var rows = withLoginUser(300L, () -> adminController.getHallProductOptions()).getCheckedData();
        var productARow = rows.stream()
                .filter(item -> item.productId().equals(productA.productId()))
                .findFirst()
                .orElseThrow();
        var productBRow = rows.stream()
                .filter(item -> item.productId().equals(productB.productId()))
                .findFirst()
                .orElseThrow();

        assertEquals("YT-HALL-OPT-001", productARow.productCode());
        assertEquals("展柜候选产品 A", productARow.nameCn());
        assertEquals(productA.revisionNo(), productARow.revisionNo());
        assertEquals("/admin-api/infra/file/28/get/showroom/product/a.png", productARow.previewImageUrl());
        assertEquals(List.of(hallOne.hallId(), hallTwo.hallId()), productARow.hallIds());

        assertEquals("YT-HALL-OPT-002", productBRow.productCode());
        assertEquals("展柜候选产品 B", productBRow.nameCn());
        assertEquals("", productBRow.previewImageUrl());
        assertEquals(List.of(), productBRow.hallIds());
    }

    @Test
    void assignedEditorShouldOnlySeeAssignedProductAndBeDeniedForOtherProductDetail() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var assignedProduct = publishBaselineProduct();
        var otherDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "YT-OTHER-001", "未指派产品", "Unassigned Product",
                Map.of("target_market", "未指派市场", "registration_certificate", "未指派证书",
                        "core_selling_points", "未指派卖点")));
        contentService.publishProductRevision(otherDraft.revisionId(), 901L);

        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", assignedProduct.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 300L))).getCheckedData();

        var page = withLoginUser(700L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(assignedProduct.productId(), page.getList().get(0).productId());

        IllegalStateException denied = assertThrows(IllegalStateException.class, () ->
                withLoginUser(700L, () -> adminController.getProduct(otherDraft.productId(), null)));
        assertTrue(denied.getMessage().contains("SHOWROOM_PRODUCT_ACCESS_DENIED"));
    }

    @Test
    void wholeProductAssignmentShouldExposeFillingStatusAndAssignedEditorAccess() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var assignedProduct = publishBaselineProduct();
        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", assignedProduct.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 300L))).getCheckedData();

        var adminPage = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();
        var adminRow = adminPage.getList().stream()
                .filter(item -> item.productId().equals(assignedProduct.productId()))
                .findFirst()
                .orElseThrow();

        assertEquals("IN_FILLING", adminRow.revision().status());
        assertNotNull(adminRow.revision().activeAssignment());
        assertEquals(700L, adminRow.revision().activeAssignment().assigneeUserId());
        assertEquals("OPEN", adminRow.revision().activeAssignment().status());

        var scopedEditorPage = withLoginUser(700L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();
        assertEquals(1L, scopedEditorPage.getTotal());
        assertEquals(assignedProduct.productId(), scopedEditorPage.getList().get(0).productId());
        assertEquals("IN_FILLING", scopedEditorPage.getList().get(0).revision().status());
        assertNotNull(withLoginUser(700L, () -> adminController.getProduct(assignedProduct.productId(), null))
                .getCheckedData());
    }

    @Test
    void superAdminShouldBypassScopedVisibilityForProductPage() {
        mockShowroomRoleChecks();

        var liveRevision = publishBaselineProduct();

        var page = withLoginUser(1L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();

        assertEquals(1L, page.getTotal());
        assertEquals(liveRevision.productId(), page.getList().get(0).productId());
        assertNotNull(withLoginUser(1L, () -> adminController.getProduct(liveRevision.productId(), null))
                .getCheckedData());
    }

    @Test
    void assignedEditorShouldKeepLifecycleVisibilityForAssignedProduct() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var liveRevision = publishBaselineProduct();
        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", liveRevision.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 100L)));

        var draftRevision = withLoginUser(700L, () -> adminController.saveProductDraft(
                new ShowroomAdminController.ProductDraftReqVO(
                        liveRevision.productId(), "YT-GW-001",
                        "生命周期可见产品", liveRevision.nameEn(),
                        productFieldsWithBaselineCover(Map.of(
                                "owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "生命周期可见市场",
                                "pipeline_layout", "",
                                "indication_content", "",
                                "core_selling_points", "生命周期可见卖点",
                                "model_specification", ""
                        ))))).getCheckedData();
        mockFile(98511L, 29L, "showroom/narration/assigned-editor-lifecycle-zh.wav", "audio/wav");
        mockFile(98512L, 29L, "showroom/narration/assigned-editor-lifecycle-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), draftRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "生命周期产品中文讲解", 98511L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), draftRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "Lifecycle product English narration", 98512L, "ruoxi");

        var submitted = withLoginUser(700L, () -> adminController.submitProduct(
                new ShowroomAdminController.SubmitReqVO(
                        draftRevision.productId(), draftRevision.revisionId(), List.of(),
                        "product", 700L, 10L, 200L, null))).getCheckedData();

        var pendingPage = withLoginUser(700L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();
        assertEquals(1L, pendingPage.getTotal());
        assertEquals(liveRevision.productId(), pendingPage.getList().get(0).productId());
        assertEquals("PENDING_SUPERVISOR_APPROVAL", pendingPage.getList().get(0).revision().status());
        assertNotNull(withLoginUser(700L, () -> adminController.getProduct(liveRevision.productId(), null))
                .getCheckedData());
        assertFalse(withLoginUser(700L, () -> adminController.getProductHistory(liveRevision.productId())).getCheckedData().isEmpty());

        AdminUserDO supervisor = new AdminUserDO();
        supervisor.setId(200L);
        supervisor.setPassword("encoded-supervisor-password");
        AdminUserDO publicity = new AdminUserDO();
        publicity.setId(300L);
        publicity.setPassword("encoded-publicity-password");
        when(electronicSignatureAuthorizationService.isElectronicSignatureEnabled(200L)).thenReturn(true);
        when(electronicSignatureAuthorizationService.isElectronicSignatureEnabled(300L)).thenReturn(true);
        when(adminUserService.getUser(200L)).thenReturn(supervisor);
        when(adminUserService.getUser(300L)).thenReturn(publicity);
        when(adminUserService.isPasswordMatch("111111", "encoded-supervisor-password")).thenReturn(true);
        when(adminUserService.isPasswordMatch("111111", "encoded-publicity-password")).thenReturn(true);
        when(changeRequestSignatureMapper.insert(org.mockito.ArgumentMatchers.<cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestSignatureDO>any()))
                .thenReturn(1);

        adminController.supervisorApprove(new ShowroomAdminController.ApprovalActionReqVO(
                submitted.changeRequestId(), 200L, "111111", "主管签名通过"));
        adminController.gaoxinApprove(new ShowroomAdminController.ApprovalActionReqVO(
                submitted.changeRequestId(), 300L, "111111", "企宣签名通过"));

        var publishedPage = withLoginUser(700L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();
        assertEquals(1L, publishedPage.getTotal());
        assertEquals(liveRevision.productId(), publishedPage.getList().get(0).productId());
        assertEquals("PUBLISHED", publishedPage.getList().get(0).revision().status());
        assertNotNull(withLoginUser(700L, () -> adminController.getProduct(liveRevision.productId(), null))
                .getCheckedData());
    }

    @Test
    void assignedEditorShouldBeReadOnlyAfterWholeAssignmentClosed() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var liveRevision = publishBaselineProduct();
        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", liveRevision.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 100L)));

        var draftRevision = withLoginUser(700L, () -> adminController.saveProductDraft(
                new ShowroomAdminController.ProductDraftReqVO(
                        liveRevision.productId(), "YT-GW-001",
                        "只读关闭后产品", liveRevision.nameEn(),
                        Map.of(
                                "owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "关闭后只读市场",
                                "pipeline_layout", "",
                                "indication_content", "",
                                "core_selling_points", "关闭后只读卖点",
                                "model_specification", ""
                        )))).getCheckedData();

        withLoginUser(700L, () -> adminController.submitProduct(
                new ShowroomAdminController.SubmitReqVO(
                        draftRevision.productId(), draftRevision.revisionId(), List.of(),
                        "product", 700L, 10L, 200L, null))).getCheckedData();

        assertNotNull(withLoginUser(700L, () -> adminController.getProduct(liveRevision.productId(), null))
                .getCheckedData());

        IllegalStateException saveDenied = assertThrows(IllegalStateException.class, () ->
                withLoginUser(700L, () -> adminController.saveProductDraft(
                        new ShowroomAdminController.ProductDraftReqVO(
                                liveRevision.productId(), "YT-GW-001",
                                "关闭后禁止修改", liveRevision.nameEn(),
                                Map.of(
                                        "owner_company_id", "124",
                                        "product_owner_type", "YINGTAI",
                                        "lifecycle_stage", "REGISTERED",
                                        "target_market", "关闭后禁止修改市场",
                                        "pipeline_layout", "",
                                        "indication_content", "",
                                        "core_selling_points", "关闭后禁止修改卖点",
                                        "model_specification", ""
                                )))));
        assertTrue(saveDenied.getMessage().contains("SHOWROOM_PRODUCT_ACCESS_DENIED"));

        IllegalStateException narrationDenied = assertThrows(IllegalStateException.class, () ->
                withLoginUser(700L, () -> adminController.generateProductNarrationScript(
                        new ShowroomAdminController.ProductNarrationGenerateReqVO(liveRevision.productId(), liveRevision.revisionId()))));
        assertTrue(narrationDenied.getMessage().contains("SHOWROOM_PRODUCT_ACCESS_DENIED"));
    }

    @Test
    void supervisorShouldSeeOnlyAssignedProductBeforeSubmissionAndStayReadOnly() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var assignedProduct = publishBaselineProduct();
        var otherDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "YT-SUP-OTHER-001", "主管不应看到的产品", "Supervisor Hidden Product",
                Map.of("target_market", "主管不应看到的市场", "registration_certificate", "主管不应看到的证书",
                        "core_selling_points", "主管不应看到的卖点")));
        contentService.publishProductRevision(otherDraft.revisionId(), 901L);

        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", assignedProduct.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 300L))).getCheckedData();

        var supervisorPage = withLoginUser(200L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();
        assertEquals(1L, supervisorPage.getTotal());
        assertEquals(assignedProduct.productId(), supervisorPage.getList().get(0).productId());

        assertNotNull(withLoginUser(200L, () -> adminController.getProduct(assignedProduct.productId(), null))
                .getCheckedData());

        IllegalStateException draftDenied = assertThrows(IllegalStateException.class, () ->
                withLoginUser(200L, () -> adminController.saveProductDraft(
                        new ShowroomAdminController.ProductDraftReqVO(
                                assignedProduct.productId(), "YT-GW-001",
                                "主管只读产品", assignedProduct.nameEn(),
                                Map.of(
                                        "owner_company_id", "124",
                                        "product_owner_type", "YINGTAI",
                                        "lifecycle_stage", "REGISTERED",
                                        "target_market", "主管越权修改市场",
                                        "pipeline_layout", "",
                                        "indication_content", "",
                                        "core_selling_points", "主管越权修改卖点",
                                        "model_specification", ""
                                )))));
        assertTrue(draftDenied.getMessage().contains("SHOWROOM_PRODUCT_ACCESS_DENIED"));
    }

    @Test
    void publicityShouldSeeAllProductsWhileUnrelatedUserShouldSeeNoneAndCannotManage() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var publicityViewer = adminUserMapper.selectById(500L);
        if (publicityViewer == null) {
            publicityViewer = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
            publicityViewer.setId(500L);
            publicityViewer.setUsername("showroom-unrelated");
            publicityViewer.setPassword("pwd");
            publicityViewer.setNickname("无关用户");
            publicityViewer.setStatus(ENABLE.getStatus());
            publicityViewer.setCreateTime(LocalDateTime.now());
            adminUserMapper.insert(publicityViewer);
        }

        var assignedProduct = publishBaselineProduct();
        var otherDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "YT-PUB-OTHER-001", "企宣可见第二产品", "Publicity Visible Product",
                Map.of("target_market", "企宣可见市场", "registration_certificate", "企宣可见证书",
                        "core_selling_points", "企宣可见卖点")));
        var otherProduct = contentService.publishProductRevision(otherDraft.revisionId(), 902L);

        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", assignedProduct.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 300L))).getCheckedData();

        var publicityPage = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();
        assertEquals(2L, publicityPage.getTotal());

        var unrelatedPage = withLoginUser(500L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();
        assertEquals(0L, unrelatedPage.getTotal());
        assertTrue(unrelatedPage.getList().isEmpty());

        IllegalStateException detailDenied = assertThrows(IllegalStateException.class, () ->
                withLoginUser(500L, () -> adminController.getProduct(otherProduct.productId(), null)));
        assertTrue(detailDenied.getMessage().contains("SHOWROOM_PRODUCT_ACCESS_DENIED"));

        IllegalStateException assignmentDenied = assertThrows(IllegalStateException.class, () ->
                withLoginUser(500L, () -> adminController.createAssignment(
                        new ShowroomAdminController.AssignmentCreateReqVO(
                                "PRODUCT", assignedProduct.productId(),
                                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 500L))));
        assertTrue(assignmentDenied.getMessage().contains("SHOWROOM_PRODUCT_ACCESS_DENIED"));
    }

    @Test
    void productPageShouldIgnoreWholeProductAssignmentsFromOtherTenant() {
        TenantUtils.execute(1L, () -> seedPublicityApproverRole(300L));
        mockShowroomRoleChecks();

        var liveRevision = TenantUtils.execute(1L, this::publishBaselineProduct);

        TenantUtils.execute(122L, () -> {
            NotifyMessageDO notifyMessage = NotifyMessageDO.builder()
                    .userId(910202L)
                    .userType(UserTypeEnum.ADMIN.getValue())
                    .templateId(1L)
                    .templateCode(ASSIGNMENT_TEMPLATE_CODE)
                    .templateNickname("系统通知")
                    .templateContent("请处理__PRODUCT_ALL_FIELDS__")
                    .templateType(1)
                    .templateParams(Map.of(
                            "fieldCode", ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE,
                            "targetType", "PRODUCT",
                            "targetId", liveRevision.productId()))
                    .readStatus(false)
                    .build();
            notifyMessageMapper.insert(notifyMessage);

            assignmentMapper.insert(ShowroomFieldAssignmentDO.builder()
                    .targetType("PRODUCT")
                    .targetId(liveRevision.productId())
                    .fieldCode(ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE)
                    .assigneeUserId(910202L)
                    .assignedBy(910202L)
                    .status("OPEN")
                    .notifyMessageId(notifyMessage.getId())
                    .createdAt(LocalDateTime.now())
                    .build());
        });

        var productPage = TenantUtils.execute(1L, () -> withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData());

        assertEquals(1L, productPage.getTotal());
        assertEquals(liveRevision.productId(), productPage.getList().get(0).productId());
        assertEquals("PUBLISHED", productPage.getList().get(0).revision().status());
        assertTrue(productPage.getList().get(0).revision().activeAssignment() == null);
    }

    @Test
    void showroomProductNotifyShouldUseListNavigation() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var liveRevision = publishBaselineProduct();
        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", liveRevision.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 100L))).getCheckedData();
        NotifyMessageDO assignmentNotify = latestNotifyMessageForUser(700L, ASSIGNMENT_TEMPLATE_CODE);
        assertEquals("PRODUCT", String.valueOf(assignmentNotify.getTemplateParams().get("targetType")));
        assertEquals(liveRevision.productId(), asLong(assignmentNotify.getTemplateParams().get("targetId")));
        assertEquals("list", String.valueOf(assignmentNotify.getTemplateParams().get("notifyOpen")));

        var draftRevision = withLoginUser(700L, () -> adminController.saveProductDraft(
                new ShowroomAdminController.ProductDraftReqVO(
                        liveRevision.productId(), "YT-GW-001",
                        "列表站内信产品", liveRevision.nameEn(),
                        Map.of(
                                "owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "列表站内信市场",
                                "pipeline_layout", "",
                                "indication_content", "",
                                "core_selling_points", "列表站内信卖点",
                                "model_specification", ""
                        )))).getCheckedData();

        var submitted = withLoginUser(700L, () -> adminController.submitProduct(
                new ShowroomAdminController.SubmitReqVO(
                        draftRevision.productId(), draftRevision.revisionId(), List.of(),
                        "product", 700L, 10L, 200L, null))).getCheckedData();

        NotifyMessageDO supervisorNotify = latestNotifyMessageForUser(200L, PENDING_APPROVAL_TEMPLATE_CODE);
        assertEquals("list", String.valueOf(supervisorNotify.getTemplateParams().get("notifyOpen")));
        assertEquals(submitted.changeRequestId(), asLong(supervisorNotify.getTemplateParams().get("changeRequestId")));
        ShowroomApprovalDetail detail = withLoginUser(200L,
                () -> adminController.getApproval(submitted.changeRequestId())).getCheckedData();
        assertTrue(detail.signatureRecords().isEmpty());
    }

    @Test
    void wholeProductAssignmentSubmitShouldCloseAssignmentAndEnterReview() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var liveRevision = publishBaselineProduct();
        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", liveRevision.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 100L))).getCheckedData();

        var draftRevision = withLoginUser(700L, () -> adminController.saveProductDraft(
                new ShowroomAdminController.ProductDraftReqVO(
                        liveRevision.productId(), "YT-GW-001",
                        "整单指派后产品", liveRevision.nameEn(),
                        Map.of(
                                "owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "整单指派市场",
                                "pipeline_layout", "",
                                "indication_content", "",
                                "core_selling_points", "整单指派卖点",
                                "model_specification", ""
                        )))).getCheckedData();

        var submitted = withLoginUser(700L, () -> adminController.submitProduct(
                new ShowroomAdminController.SubmitReqVO(
                        draftRevision.productId(), draftRevision.revisionId(), List.of(),
                        "product", 700L, 10L, 200L, null))).getCheckedData();

        var assignmentPage = adminController.getAssignmentPage(
                new ShowroomAdminController.AssignmentPageReqVO("PRODUCT", liveRevision.productId(), 700L, null, 1, 20))
                .getCheckedData();
        assertEquals("AUTO_SUBMITTED", assignmentPage.get(0).status());
        assertEquals(submitted.changeRequestId(), assignmentPage.get(0).lastChangeRequestId());

        var page = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData();
        var row = page.getList().stream()
                .filter(item -> item.productId().equals(liveRevision.productId()))
                .findFirst()
                .orElseThrow();
        assertEquals("PENDING_SUPERVISOR_APPROVAL", row.revision().status());
        assertEquals(null, row.revision().activeAssignment());
    }

    @Test
    void wholeProductAssignmentRejectedBySupervisorShouldReopenAssignmentAndNotifySubmitterForEdit() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var liveRevision = publishBaselineProduct();
        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", liveRevision.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 100L))).getCheckedData();

        var draftRevision = withLoginUser(700L, () -> adminController.saveProductDraft(
                new ShowroomAdminController.ProductDraftReqVO(
                        liveRevision.productId(), "YT-GW-001",
                        "主管驳回后回改产品", liveRevision.nameEn(),
                        Map.of(
                                "owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "主管驳回后回改市场",
                                "pipeline_layout", "",
                                "indication_content", "",
                                "core_selling_points", "主管驳回后回改卖点",
                                "model_specification", ""
                        )))).getCheckedData();

        var submitted = withLoginUser(700L, () -> adminController.submitProduct(
                new ShowroomAdminController.SubmitReqVO(
                        draftRevision.productId(), draftRevision.revisionId(), List.of(),
                        "product", 700L, 10L, 200L, null))).getCheckedData();

        withLoginUser(200L, () -> adminController.supervisorReject(new ShowroomAdminController.ApprovalRejectReqVO(
                submitted.changeRequestId(), 200L, "111111", "主管驳回"))).getCheckedData();

        NotifyMessageDO rejectedNotify = latestNotifyMessageForUser(700L, REJECTED_APPROVAL_TEMPLATE_CODE);
        assertEquals("主管审核", String.valueOf(rejectedNotify.getTemplateParams().get("approvalStage")));
        assertEquals("主管驳回", String.valueOf(rejectedNotify.getTemplateParams().get("rejectionReason")));
        assertEquals("edit", String.valueOf(rejectedNotify.getTemplateParams().get("notifyOpen")));
        assertEquals(submitted.changeRequestId(), asLong(rejectedNotify.getTemplateParams().get("changeRequestId")));

        var assignmentPage = adminController.getAssignmentPage(
                new ShowroomAdminController.AssignmentPageReqVO("PRODUCT", liveRevision.productId(), 700L, null, 1, 20))
                .getCheckedData();
        assertEquals("OPEN", assignmentPage.get(0).status());
        assertEquals(submitted.changeRequestId(), assignmentPage.get(0).lastChangeRequestId());

        var row = withLoginUser(700L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData()
                .getList().stream()
                .filter(item -> item.productId().equals(liveRevision.productId()))
                .findFirst()
                .orElseThrow();
        assertEquals("IN_FILLING", row.revision().status());
        assertNotNull(row.revision().activeAssignment());
        assertEquals(700L, row.revision().activeAssignment().assigneeUserId());
    }

    @Test
    void wholeProductAssignmentRejectedByGaoxinShouldReopenAssignmentAndNotifySubmitterForEdit() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var liveRevision = publishBaselineProduct();
        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", liveRevision.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 700L, 100L))).getCheckedData();

        var draftRevision = withLoginUser(700L, () -> adminController.saveProductDraft(
                new ShowroomAdminController.ProductDraftReqVO(
                        liveRevision.productId(), "YT-GW-001",
                        "企宣驳回后回改产品", liveRevision.nameEn(),
                        Map.of(
                                "owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "企宣驳回后回改市场",
                                "pipeline_layout", "",
                                "indication_content", "",
                                "core_selling_points", "企宣驳回后回改卖点",
                                "model_specification", ""
                        )))).getCheckedData();

        var submitted = withLoginUser(700L, () -> adminController.submitProduct(
                new ShowroomAdminController.SubmitReqVO(
                        draftRevision.productId(), draftRevision.revisionId(), List.of(),
                        "product", 700L, 10L, 200L, null))).getCheckedData();

        withLoginUser(200L, () -> adminController.supervisorApprove(new ShowroomAdminController.ApprovalActionReqVO(
                submitted.changeRequestId(), 200L, "111111", "主管签名通过"))).getCheckedData();
        withLoginUser(300L, () -> adminController.gaoxinReject(new ShowroomAdminController.ApprovalRejectReqVO(
                submitted.changeRequestId(), 300L, "111111", "企宣驳回"))).getCheckedData();

        NotifyMessageDO rejectedNotify = latestNotifyMessageForUser(700L, REJECTED_APPROVAL_TEMPLATE_CODE);
        assertEquals("企宣审批", String.valueOf(rejectedNotify.getTemplateParams().get("approvalStage")));
        assertEquals("企宣驳回", String.valueOf(rejectedNotify.getTemplateParams().get("rejectionReason")));
        assertEquals("edit", String.valueOf(rejectedNotify.getTemplateParams().get("notifyOpen")));
        assertEquals(submitted.changeRequestId(), asLong(rejectedNotify.getTemplateParams().get("changeRequestId")));

        var assignmentPage = adminController.getAssignmentPage(
                new ShowroomAdminController.AssignmentPageReqVO("PRODUCT", liveRevision.productId(), 700L, null, 1, 20))
                .getCheckedData();
        assertEquals("OPEN", assignmentPage.get(0).status());
        assertEquals(submitted.changeRequestId(), assignmentPage.get(0).lastChangeRequestId());

        var row = withLoginUser(700L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData()
                .getList().stream()
                .filter(item -> item.productId().equals(liveRevision.productId()))
                .findFirst()
                .orElseThrow();
        assertEquals("IN_FILLING", row.revision().status());
        assertNotNull(row.revision().activeAssignment());
        assertEquals(700L, row.revision().activeAssignment().assigneeUserId());
    }

    @Test
    void manualRejectedSubmissionShouldStayRejectedWithoutReopenedAssignment() {
        seedPublicityApproverRole(300L);
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "手工驳回产品", "Introducer Sheath Set",
                Map.of("target_market", "手工驳回市场")));

        var submitted = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                draftRevision.productId(), draftRevision.revisionId(), List.of("target_market"),
                "product", 300L, 8L, 200L, null))).getCheckedData();

        withLoginUser(200L, () -> adminController.supervisorReject(new ShowroomAdminController.ApprovalRejectReqVO(
                submitted.changeRequestId(), 200L, "111111", "手工提交通知驳回"))).getCheckedData();

        NotifyMessageDO rejectedNotify = latestNotifyMessageForUser(300L, REJECTED_APPROVAL_TEMPLATE_CODE);
        assertEquals("edit", String.valueOf(rejectedNotify.getTemplateParams().get("notifyOpen")));
        assertEquals("手工提交通知驳回", String.valueOf(rejectedNotify.getTemplateParams().get("rejectionReason")));

        var row = withLoginUser(300L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null)))
                .getCheckedData()
                .getList().stream()
                .filter(item -> item.productId().equals(liveRevision.productId()))
                .findFirst()
                .orElseThrow();
        assertEquals("REJECTED", row.revision().status());
        assertEquals(null, row.revision().activeAssignment());
    }

    @Test
    void productSubmitShouldStartAtGaoxinApprovalWhenSubmitterDeptMissing() {
        seedPublicityApproverRole(300L);
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 No Dept", "Introducer Sheath Set",
                Map.of("target_market", "无部门市场")));

        var submitted = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                draftRevision.productId(), draftRevision.revisionId(), List.of("target_market"),
                "product", 100L, null, null, null))).getCheckedData();

        ShowroomApprovalDetail detail = withLoginUser(300L,
                () -> adminController.getApproval(submitted.changeRequestId())).getCheckedData();

        assertEquals("PENDING_GAOXIN_APPROVAL", submitted.status());
        assertEquals("PENDING_GAOXIN_APPROVAL", detail.changeRequest().status());
        assertEquals(300L, detail.changeRequest().gaoxinUserId());
        assertEquals(null, detail.changeRequest().supervisorUserId());
        assertEquals(null, detail.changeRequest().submitterDeptId());
    }

    @Test
    void approvalRejectAndPublishEndpointsShouldPersistWorkflowState() {
        seedPublicityApproverRole(300L);
        var liveRevision = publishBaselineProduct();

        var rejectDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 Reject", "Introducer Sheath Set",
                Map.of("target_market", "驳回市场")));
        var rejectRequest = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                rejectDraft.productId(), rejectDraft.revisionId(), List.of("name_cn"),
                "product", 100L, 8L, 200L, null))).getCheckedData();
        var supervisorRejected = withLoginUser(200L, () -> adminController.supervisorReject(
                new ShowroomAdminController.ApprovalRejectReqVO(
                        rejectRequest.changeRequestId(), 200L, "111111", "主管驳回"))).getCheckedData();
        assertEquals("REJECTED", supervisorRejected.status());
        assertEquals("主管驳回", supervisorRejected.rejectionReason());
        assertEquals(liveRevision.revisionId(),
                contentService.requireCurrentProductRevision(liveRevision.productId()).revisionId());

        var gaoxinRejectDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 Gaoxin Reject", "Introducer Sheath Set",
                Map.of("target_market", "高新驳回市场")));
        var gaoxinRejectRequest = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                gaoxinRejectDraft.productId(), gaoxinRejectDraft.revisionId(), List.of("target_market"),
                "product", 100L, 8L, 200L, null))).getCheckedData();
        withLoginUser(200L, () -> adminController.supervisorApprove(new ShowroomAdminController.ApprovalActionReqVO(
                gaoxinRejectRequest.changeRequestId(), 200L, "111111", "主管签名通过")));
        var gaoxinRejected = withLoginUser(300L, () -> adminController.gaoxinReject(new ShowroomAdminController.ApprovalRejectReqVO(
                gaoxinRejectRequest.changeRequestId(), 300L, "111111", "高新驳回"))).getCheckedData();
        assertEquals("REJECTED", gaoxinRejected.status());
        assertEquals("高新驳回", gaoxinRejected.rejectionReason());
        assertEquals(liveRevision.revisionId(),
                contentService.requireCurrentProductRevision(liveRevision.productId()).revisionId());

        var publishDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 Publish", "Introducer Sheath Set",
                productFieldsWithBaselineCover(Map.of("target_market", "已发布市场"))));
        mockFile(98521L, 29L, "showroom/narration/approval-publish-zh.wav", "audio/wav");
        mockFile(98522L, 29L, "showroom/narration/approval-publish-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), publishDraft.revisionId(),
                ShowroomNarrationLanguage.ZH, "审批发布产品中文讲解", 98521L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), publishDraft.revisionId(),
                ShowroomNarrationLanguage.EN, "Approval publish product English narration", 98522L, "ruoxi");
        var publishRequest = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                publishDraft.productId(), publishDraft.revisionId(), List.of("name_cn", "target_market"),
                "product", 100L, 8L, 200L, null))).getCheckedData();
        withLoginUser(200L, () -> adminController.supervisorApprove(new ShowroomAdminController.ApprovalActionReqVO(
                publishRequest.changeRequestId(), 200L, "111111", "主管签名通过")));
        var published = withLoginUser(300L, () -> adminController.gaoxinApprove(new ShowroomAdminController.ApprovalActionReqVO(
                publishRequest.changeRequestId(), 300L, "111111", "企宣签名通过"))).getCheckedData();

        assertEquals("PUBLISHED", published.status());
        assertEquals(publishDraft.revisionId(),
                contentService.requireCurrentProductRevision(liveRevision.productId()).revisionId());
        assertFalse(contentService.versionAudits("PRODUCT", liveRevision.productId()).isEmpty());
        assertTrue(contentService.versionAudits("PRODUCT", liveRevision.productId()).stream()
                .anyMatch(audit -> "target_market".equals(audit.fieldCode())));
    }

    @Test
    void approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer() {
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var supervisorLive = publishBaselineProduct();
        var supervisorDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                supervisorLive.productId(), "YT-GW-001", "主管待审产品", "Supervisor Pending Product",
                Map.of("target_market", "主管待审市场")));
        var supervisorPending = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                supervisorDraft.productId(), supervisorDraft.revisionId(), List.of("target_market"),
                "product", 100L, 8L, 200L, null))).getCheckedData();

        var gaoxinBaseDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "YT-GX-001", "高新待审产品", "Gaoxin Pending Product",
                Map.of("target_market", "高新待审市场", "registration_certificate", "高新待审证书",
                        "core_selling_points", "高新待审卖点")));
        var gaoxinLive = contentService.publishProductRevision(gaoxinBaseDraft.revisionId(), 930L);
        var gaoxinDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                gaoxinLive.productId(), "YT-GX-001", "高新待审产品 V2", "Gaoxin Pending Product",
                Map.of("target_market", "高新终审市场")));
        var gaoxinPending = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                gaoxinDraft.productId(), gaoxinDraft.revisionId(), List.of("target_market"),
                "product", 100L, null, null, null))).getCheckedData();

        var supervisorPage = withLoginUser(200L, () -> adminController.getApprovalPage()).getCheckedData();
        assertEquals(1, supervisorPage.size());
        assertEquals(supervisorPending.changeRequestId(), supervisorPage.get(0).changeRequestId());

        var gaoxinPage = withLoginUser(300L, () -> adminController.getApprovalPage()).getCheckedData();
        assertEquals(1, gaoxinPage.size());
        assertEquals(gaoxinPending.changeRequestId(), gaoxinPage.get(0).changeRequestId());

        IllegalStateException denied = assertThrows(IllegalStateException.class, () ->
                withLoginUser(200L, () -> adminController.getApproval(gaoxinPending.changeRequestId())));
        assertTrue(denied.getMessage().contains("SHOWROOM_APPROVAL_ACCESS_DENIED"));
    }

    @Test
    void approvalDetailShouldAllowUnifiedCenterParticipantsToOpenOfficialPage() {
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-UC-001", "统一中心跳转产品", "Unified Center Product",
                Map.of("target_market", "统一中心跳转市场")));
        var submitted = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                draftRevision.productId(), draftRevision.revisionId(), List.of("target_market"),
                "product", 300L, 8L, 200L, null))).getCheckedData();

        ShowroomApprovalDetail initiatorDetail = withLoginUser(300L,
                () -> adminController.getApproval(submitted.changeRequestId())).getCheckedData();
        assertEquals(submitted.changeRequestId(), initiatorDetail.changeRequest().changeRequestId());

        withLoginUser(200L, () -> adminController.supervisorApprove(new ShowroomAdminController.ApprovalActionReqVO(
                submitted.changeRequestId(), 200L, "111111", "主管签名通过")));

        ShowroomApprovalDetail supervisorDoneDetail = withLoginUser(200L,
                () -> adminController.getApproval(submitted.changeRequestId())).getCheckedData();
        assertEquals("PENDING_GAOXIN_APPROVAL", supervisorDoneDetail.changeRequest().status());

        IllegalStateException denied = assertThrows(IllegalStateException.class, () ->
                withLoginUser(999L, () -> adminController.getApproval(submitted.changeRequestId())));
        assertTrue(denied.getMessage().contains("SHOWROOM_APPROVAL_ACCESS_DENIED"));
    }

    @Test
    void supervisorPendingApprovalProductShouldStayVisibleEvenIfLaterWholeAssignmentExists() {
        seedAssignmentActors(true);
        seedNotifyTemplate();
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();

        deptMapper.deleteById(11L);
        adminUserMapper.deleteById(201L);
        adminUserMapper.deleteById(701L);
        userRoleMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getUserId, 701L));

        var otherLeader = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
        otherLeader.setId(201L);
        otherLeader.setUsername("other-leader");
        otherLeader.setPassword("pwd");
        otherLeader.setNickname("其他部门负责人");
        otherLeader.setDeptId(11L);
        otherLeader.setStatus(ENABLE.getStatus());
        otherLeader.setCreateTime(LocalDateTime.now());
        adminUserMapper.insert(otherLeader);

        var otherAssignee = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
        otherAssignee.setId(701L);
        otherAssignee.setUsername("other-editor");
        otherAssignee.setPassword("pwd");
        otherAssignee.setNickname("其他编辑");
        otherAssignee.setDeptId(11L);
        otherAssignee.setStatus(ENABLE.getStatus());
        otherAssignee.setCreateTime(LocalDateTime.now());
        adminUserMapper.insert(otherAssignee);

        var otherDept = new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO();
        otherDept.setId(11L);
        otherDept.setName("外部展厅部");
        otherDept.setParentId(0L);
        otherDept.setSort(2);
        otherDept.setLeaderUserId(201L);
        otherDept.setStatus(ENABLE.getStatus());
        otherDept.setCreateTime(LocalDateTime.now());
        deptMapper.insert(otherDept);

        UserRoleDO otherEditorRole = new UserRoleDO();
        otherEditorRole.setUserId(701L);
        otherEditorRole.setRoleId(30L);
        userRoleMapper.insert(otherEditorRole);

        var liveRevision = publishBaselineProduct();
        var pendingDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "主管待审产品可见", "Supervisor Pending Visible Product",
                Map.of("target_market", "主管待审可见市场")));
        var pending = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                pendingDraft.productId(), pendingDraft.revisionId(), List.of("target_market"),
                "product", 100L, 8L, 200L, null))).getCheckedData();

        withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", liveRevision.productId(),
                ShowroomAssignmentService.PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE, 701L, 300L))).getCheckedData();

        var supervisorApprovals = withLoginUser(200L, () -> adminController.getApprovalPage()).getCheckedData();
        assertEquals(1, supervisorApprovals.size());
        assertEquals(pending.changeRequestId(), supervisorApprovals.get(0).changeRequestId());

        var supervisorPage = withLoginUser(200L, () -> adminController.getProductPage(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20, null, null, null, null, null,
                        liveRevision.productId()))).getCheckedData();
        assertEquals(1L, supervisorPage.getTotal());
        assertEquals(liveRevision.productId(), supervisorPage.getList().get(0).productId());

        var detail = withLoginUser(200L, () -> adminController.getProduct(liveRevision.productId(), null))
                .getCheckedData();
        assertEquals(liveRevision.productId(), detail.productId());
        assertFalse(detail.editable());

        IllegalStateException draftDenied = assertThrows(IllegalStateException.class, () ->
                withLoginUser(200L, () -> adminController.saveProductDraft(
                        new ShowroomAdminController.ProductDraftReqVO(
                                liveRevision.productId(), "YT-GW-001", "主管待审产品可见", "Supervisor Pending Visible Product",
                                Map.of(
                                        "owner_company_id", "124",
                                        "product_owner_type", "YINGTAI",
                                        "lifecycle_stage", "REGISTERED",
                                        "target_market", "主管越权修改市场",
                                        "pipeline_layout", "",
                                        "indication_content", "",
                                        "core_selling_points", "主管越权修改卖点",
                                        "model_specification", ""
                                )))));
        assertTrue(draftDenied.getMessage().contains("SHOWROOM_PRODUCT_ACCESS_DENIED"));
    }

    @Test
    void assignmentEndpointsShouldPersistNotifyLinkageAndAutoSubmit() {
        seedAssignmentActors(true);
        seedPublicityApproverRole(300L);
        seedNotifyTemplate();
        var liveRevision = publishBaselineProduct();

        var assignment = withLoginUser(300L, () -> adminController.createAssignment(new ShowroomAdminController.AssignmentCreateReqVO(
                "PRODUCT", liveRevision.productId(), "core_selling_points", 700L, 100L)))
                .getCheckedData();
        assertNotNull(assignment.notifyMessageId());
        assertEquals("OPEN", assignment.status());
        assertNotNull(notifyMessageMapper.selectById(assignment.notifyMessageId()));

        var detail = adminController.getAssignment(assignment.assignmentId()).getCheckedData();
        assertEquals("旧卖点", detail.currentDraftValue());
        assertEquals(ASSIGNMENT_TEMPLATE_CODE, detail.notifyTemplateCode());

        var page = adminController.getAssignmentPage(new ShowroomAdminController.AssignmentPageReqVO(
                "PRODUCT", liveRevision.productId(), 700L, null, 1, 20)).getCheckedData();
        assertEquals(1, page.size());

        var result = adminController.completeAndSubmitAssignment(
                new ShowroomAdminController.AssignmentCompleteReqVO(
                        assignment.assignmentId(), "补充后的卖点", 700L, null)).getCheckedData();
        assertEquals("AUTO_SUBMITTED", result.assignment().status());
        assertEquals("PENDING_SUPERVISOR_REVIEW", result.changeRequest().status());
        assertEquals(300L, result.changeRequest().gaoxinUserId());
        assertEquals(assignment.assignmentId(), result.changeRequest().sourceAssignmentId());

        var updated = adminController.getAssignment(assignment.assignmentId()).getCheckedData();
        assertEquals("AUTO_SUBMITTED", updated.status());
        assertEquals("补充后的卖点", updated.currentDraftValue());
        assertNotNull(updated.lastChangeRequestId());
    }

    @Test
    void productCommentEndpointsShouldCreateReplyAndResolveThread() {
        var liveRevision = publishBaselineProduct();
        ShowroomChangeRequest request = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                liveRevision.productId(), liveRevision.revisionId(), List.of("target_market"),
                "product", 100L, 8L, 200L, 300L))).getCheckedData();

        var thread = withLoginUser(100L, () -> adminController.createProductComment(
                new ShowroomAdminController.ProductCommentCreateReqVO(
                        liveRevision.productId(), liveRevision.revisionId(), request.changeRequestId(),
                        ShowroomCommentAnchorType.CHANGE_REQUEST.name(), null, 100L, "审批讨论")))
                .getCheckedData();
        var reply = withLoginUser(200L, () -> adminController.replyProductComment(
                new ShowroomAdminController.ProductCommentReplyReqVO(
                        thread.commentId(), 200L, "已补充"))).getCheckedData();
        var resolved = withLoginUser(300L, () -> adminController.resolveProductComment(
                new ShowroomAdminController.ProductCommentResolveReqVO(
                        thread.commentId(), 300L))).getCheckedData();

        assertEquals(thread.productId(), reply.productId());
        assertEquals(thread.changeRequestId(), reply.changeRequestId());
        assertEquals("RESOLVED", resolved.status());

        var page = withLoginUser(200L, () -> adminController.getProductCommentPage(
                new ShowroomAdminController.ProductCommentPageReqVO(
                        liveRevision.productId(), ShowroomCommentAnchorType.CHANGE_REQUEST.name(), null,
                        request.changeRequestId(), "RESOLVED"))).getCheckedData();
        assertEquals(2, page.size());
    }

    @Test
    void productCommentEndpointsShouldDenyUsersOutsideProductModificationParticipants() {
        var liveRevision = publishBaselineProduct();
        ShowroomChangeRequest request = withLoginUser(300L, () -> adminController.submitProduct(new ShowroomAdminController.SubmitReqVO(
                liveRevision.productId(), liveRevision.revisionId(), List.of("target_market"),
                "product", 100L, 8L, 200L, 300L))).getCheckedData();

        var thread = withLoginUser(100L, () -> adminController.createProductComment(
                new ShowroomAdminController.ProductCommentCreateReqVO(
                        liveRevision.productId(), liveRevision.revisionId(), request.changeRequestId(),
                        ShowroomCommentAnchorType.CHANGE_REQUEST.name(), null, 100L, "审批讨论")))
                .getCheckedData();

        IllegalStateException deniedCreate = assertThrows(IllegalStateException.class,
                () -> withLoginUser(999L, () -> adminController.createProductComment(
                        new ShowroomAdminController.ProductCommentCreateReqVO(
                                liveRevision.productId(), liveRevision.revisionId(), request.changeRequestId(),
                                ShowroomCommentAnchorType.CHANGE_REQUEST.name(), null, 999L, "越权发起")))
                        .getCheckedData());
        assertTrue(deniedCreate.getMessage().contains("SHOWROOM_DISCUSSION_ACCESS_DENIED"));

        IllegalStateException deniedPage = assertThrows(IllegalStateException.class,
                () -> withLoginUser(999L, () -> adminController.getProductCommentPage(
                        new ShowroomAdminController.ProductCommentPageReqVO(
                                liveRevision.productId(), ShowroomCommentAnchorType.CHANGE_REQUEST.name(),
                                null, request.changeRequestId(), null))).getCheckedData());
        assertTrue(deniedPage.getMessage().contains("SHOWROOM_DISCUSSION_ACCESS_DENIED"));

        IllegalStateException deniedReply = assertThrows(IllegalStateException.class,
                () -> withLoginUser(999L, () -> adminController.replyProductComment(
                        new ShowroomAdminController.ProductCommentReplyReqVO(
                                thread.commentId(), 999L, "越权回复"))).getCheckedData());
        assertTrue(deniedReply.getMessage().contains("SHOWROOM_DISCUSSION_ACCESS_DENIED"));

        var supervisorPage = withLoginUser(200L, () -> adminController.getProductCommentPage(
                new ShowroomAdminController.ProductCommentPageReqVO(
                        liveRevision.productId(), ShowroomCommentAnchorType.CHANGE_REQUEST.name(),
                        null, request.changeRequestId(), null))).getCheckedData();
        assertEquals(1, supervisorPage.size());

        var reply = withLoginUser(200L, () -> adminController.replyProductComment(
                new ShowroomAdminController.ProductCommentReplyReqVO(
                        thread.commentId(), 200L, "主管已查看"))).getCheckedData();
        assertEquals(thread.commentId(), reply.parentCommentId());

        IllegalStateException deniedResolve = assertThrows(IllegalStateException.class,
                () -> withLoginUser(999L, () -> adminController.resolveProductComment(
                        new ShowroomAdminController.ProductCommentResolveReqVO(
                                thread.commentId(), 999L))).getCheckedData());
        assertTrue(deniedResolve.getMessage().contains("SHOWROOM_DISCUSSION_ACCESS_DENIED"));

        var resolved = withLoginUser(300L, () -> adminController.resolveProductComment(
                new ShowroomAdminController.ProductCommentResolveReqVO(
                        thread.commentId(), 300L))).getCheckedData();
        assertEquals("RESOLVED", resolved.status());
    }

    @Test
    void narrationSubmitShouldStayPendingUntilManualApprovalsAndPublish() {
        var liveRevision = publishBaselineProduct();
        mockFile(99121L, 29L, "showroom/narration/pending-review-product-zh.wav", "audio/wav");
        var draft = adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "待人工审批的产品中文讲解", 99121L, 66, false)).getCheckedData();

        var submitted = adminController.submitNarration(new ShowroomAdminController.NarrationSubmitReqVO(
                draft.id(), 200L, 300L, true)).getCheckedData();

        assertEquals("PENDING_SUPERVISOR_REVIEW", submitted.status().name());
        assertFalse(submitted.live());

        var latest = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        assertEquals("PENDING_SUPERVISOR_REVIEW", latest.status());
        assertFalse(latest.live());

        assertTrue(narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH)).isEmpty());
    }

    @Test
    void narrationSubmitShouldRequireManualConfirmation() {
        var liveRevision = publishBaselineProduct();
        var draft = adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "未人工确认的产品中文讲解", 99122L, 67, false)).getCheckedData();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> adminController.submitNarration(new ShowroomAdminController.NarrationSubmitReqVO(
                        draft.id(), 200L, 300L, false)));

        assertTrue(exception.getMessage().contains("SHOWROOM_MANUAL_CONFIRMATION_REQUIRED"));
        assertTrue(narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH)).isEmpty());
    }

    @Test
    void narrationGetAndDisplayPayloadShouldExposePersistedLiveAssets() {
        var liveRevision = publishBaselineProduct();
        FileDO previewFile = FileDO.builder()
                .id(99001L)
                .configId(28L)
                .name("preview.png")
                .path("showroom/preview/temp/20260519/preview.png")
                .url("http://127.0.0.1:9000/yudao/showroom/preview/temp/20260519/preview.png")
                .type("image/png")
                .size(123L)
                .build();
        when(fileMapper.selectById(99001L)).thenReturn(previewFile);
        when(fileService.getFile(99001L)).thenReturn(previewFile);
        try {
            when(fileService.getFileContent(28L, "showroom/preview/temp/20260519/preview.png"))
                    .thenReturn("showroom/preview/temp/20260519/preview.png".getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        FileDO narrationAudioFile = FileDO.builder()
                .id(99101L)
                .configId(29L)
                .name("product-audio.wav")
                .path("showroom/narration/test/product-audio.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/test/product-audio.wav")
                .type("audio/wav")
                .size(456L)
                .build();
        when(fileMapper.selectById(99101L)).thenReturn(narrationAudioFile);
        when(fileService.getFile(99101L)).thenReturn(narrationAudioFile);
        try {
            when(fileService.getFileContent(29L, "showroom/narration/test/product-audio.wav"))
                    .thenReturn("showroom/narration/test/product-audio.wav".getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        FileDO narrationAudioFileEn = FileDO.builder()
                .id(99102L)
                .configId(29L)
                .name("product-audio-en.wav")
                .path("showroom/narration/test/product-audio-en.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/test/product-audio-en.wav")
                .type("audio/wav")
                .size(457L)
                .build();
        when(fileMapper.selectById(99102L)).thenReturn(narrationAudioFileEn);
        when(fileService.getFile(99102L)).thenReturn(narrationAudioFileEn);
        try {
            when(fileService.getFileContent(29L, "showroom/narration/test/product-audio-en.wav"))
                    .thenReturn("showroom/narration/test/product-audio-en.wav".getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        var narrationDraft = adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "产品中文讲解", 99101L, 66, true)).getCheckedData();
        var submitted = adminController.submitNarration(new ShowroomAdminController.NarrationSubmitReqVO(
                narrationDraft.id(), 200L, 300L, true)).getCheckedData();
        assertEquals("PENDING_SUPERVISOR_REVIEW", submitted.status().name());
        var supervisorApproved = adminController.supervisorApproveNarration(
                new ShowroomAdminController.NarrationApprovalReqVO(narrationDraft.id(), 200L)).getCheckedData();
        assertEquals("PENDING_GAOXIN_APPROVAL", supervisorApproved.status().name());
        var gaoxinApproved = adminController.gaoxinApproveNarration(
                new ShowroomAdminController.NarrationApprovalReqVO(narrationDraft.id(), 300L)).getCheckedData();
        assertEquals("APPROVED", gaoxinApproved.status().name());
        var published = adminController.publishNarration(
                new ShowroomAdminController.NarrationPublishReqVO(narrationDraft.id())).getCheckedData();
        assertEquals("PUBLISHED", published.status().name());
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), liveRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "English product narration", 99102L, "ruoxi");

        var previewDraft = previewAssetService.bindStaticPreviewAssets(new ShowroomPreviewAssetDraftCommand(
                ShowroomPreviewAssetTargetType.PRODUCT, liveRevision.productId(), liveRevision.revisionId(),
                new ShowroomPreviewAssetFiles(99001L, 99002L, 99003L)));
        previewAssetService.publish(previewAssetService.gaoxinApprove(
                previewAssetService.supervisorApprove(previewAssetService.submit(previewDraft.id()).id(), 200L).id(),
                300L).id());

        var hall = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "cardiology", "心内介入类", "Cardiology Hall", "心内介入产品展厅", "Cardiology product hall")).getCheckedData();
        mockFile(99014L, 28L, "showroom/preview/hall-runtime-test.png", "image/png");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 99014L);
        mockFile(99015L, 29L, "showroom/narration/hall-runtime-test-zh.wav", "audio/wav");
        mockFile(99016L, 29L, "showroom/narration/hall-runtime-test-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "心内介入产品展厅", 99015L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "Cardiology product hall", 99016L, "ruoxi");
        saveHallCanvasLayout(hall.hallId(), liveRevision.productId());

        var liveCompany = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.of("development_history", "盈泰医疗发展历程",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-runtime-test.png")))
                .revisionId(), 901L);
        mockFile(99011L, 28L, "showroom/preview/company-runtime-test.png", "image/png");
        mockFile(99012L, 29L, "showroom/narration/company-runtime-test-zh.wav", "audio/wav");
        mockFile(99013L, 29L, "showroom/narration/company-runtime-test-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, liveCompany.companyId(),
                liveCompany.revisionId(), 99011L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 99012L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, liveCompany.companyId(), liveCompany.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 99013L, "ruoxi");

        var narration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        assertEquals("产品中文讲解", narration.scriptText());
        assertTrue(narration.audioFileId() != null);

        Object payload = getWebsiteConfigPayload();
        Object product = recordListAccessor(recordListAccessor(payload, "showrooms").get(0), "products").get(0);
        assertTrue(String.valueOf((Object) recordAccessor(product, "previewImageUrl")).startsWith("/showroom/sites/"));

        var displayHall = displayController.getHall(hall.hallId()).getCheckedData();
        assertEquals(BASELINE_PRODUCT_COVER_IMAGE,
                displayHall.productCards().get(0).previewImageUrl());
        assertFalse(displayHall.productCards().get(0).previewImageUrl().contains("/infra/file/get?id="));
    }

    @Test
    void websiteConfigProductShouldExposeBilingualBasicFieldsAndKeepAdvancedFieldsExcluded() {
        var company = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.of("cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-bilingual-product.png")))
                .revisionId(), 901L);
        mockFile(771L, 28L, "showroom/preview/company-bilingual-product.png", "image/png");
        mockFile(772L, 29L, "showroom/narration/company-bilingual-product-zh.wav", "audio/wav");
        mockFile(773L, 29L, "showroom/narration/company-bilingual-product-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, company.companyId(),
                company.revisionId(), 771L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 772L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 773L, "ruoxi");

        var liveRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "YT-BI-DISPLAY-001", "双语展示产品", "Bilingual Display Product",
                        Map.ofEntries(
                                Map.entry("owner_company_id", String.valueOf(company.companyId())),
                                Map.entry("product_owner_type", "YINGTAI"),
                                Map.entry("lifecycle_stage", "REGISTERED"),
                                Map.entry("target_market", "冠脉市场"),
                                Map.entry("target_market_en", "Coronary market"),
                                Map.entry("pipeline_layout", "结构布局中文"),
                                Map.entry("pipeline_layout_en", "Pipeline layout in English"),
                                Map.entry("core_selling_points", "卖点中文"),
                                Map.entry("core_selling_points_en", "Selling points in English"),
                                Map.entry("registration_certificate", "注册证中文"),
                                Map.entry("registration_certificate_en", "Registration certificate in English"),
                                Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/preview/product-bilingual-display.png"))))
                .revisionId(), 901L);

        mockFile(781L, 28L, "showroom/preview/product-bilingual-display.png", "image/png");
        mockFile(791L, 29L, "showroom/narration/product-bilingual-display-zh.wav", "audio/wav");
        mockFile(792L, 29L, "showroom/narration/product-bilingual-display-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, liveRevision.productId(),
                liveRevision.revisionId(), 781L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), liveRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "双语展示中文讲解", 791L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), liveRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "Bilingual display English narration", 792L, "ruoxi");

        var hall = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "BILINGUAL_PRODUCT", "双语产品展厅", "Bilingual Product Hall",
                "双语产品展厅说明", "Bilingual product hall overview")).getCheckedData();
        mockFile(793L, 28L, "showroom/preview/hall-bilingual-product.png", "image/png");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 793L);
        mockFile(794L, 29L, "showroom/narration/hall-bilingual-product-zh.wav", "audio/wav");
        mockFile(795L, 29L, "showroom/narration/hall-bilingual-product-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "双语产品展厅说明", 794L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "Bilingual product hall overview", 795L, "ruoxi");
        saveHallCanvasLayout(hall.hallId(), liveRevision.productId());

        Object payload = getWebsiteConfigPayload();
        Object product = recordListAccessor(recordListAccessor(payload, "showrooms").get(0), "products").get(0);
        List<?> bilingualPublicFields = recordListAccessor(product, "bilingualPublicFields");

        assertTrue(String.valueOf((Object) recordAccessor(product, "previewImageUrl")).startsWith("/showroom/sites/"));
        assertEquals("Coronary market", recordAccessor(findBilingualField(product, "target_market"), "valueEn"));
        assertEquals("Yingtai Medical", recordAccessor(findBilingualField(product, "owner_company_id"), "valueEn"));
        assertEquals("Yingtai Product", recordAccessor(findBilingualField(product, "product_owner_type"), "valueEn"));
        assertTrue(bilingualPublicFields.stream()
                .noneMatch(field -> "registration_certificate".equals(recordAccessor(field, "fieldCode"))));
    }

    @Test
    void websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset() {
        var company = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.of("cover_image", "/admin-api/infra/file/28/get/showroom/preview/company-cover-priority.png")))
                .revisionId(), 901L);
        mockFile(881L, 28L, "showroom/preview/company-cover-priority.png", "image/png");
        mockFile(882L, 29L, "showroom/narration/company-cover-priority-zh.wav", "audio/wav");
        mockFile(883L, 29L, "showroom/narration/company-cover-priority-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, company.companyId(), company.revisionId(), 881L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 882L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 883L, "ruoxi");

        var liveRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "YT-COVER-FIRST-001", "封面优先产品", "Cover First Product",
                        Map.ofEntries(
                                Map.entry("owner_company_id", String.valueOf(company.companyId())),
                                Map.entry("product_owner_type", "YINGTAI"),
                                Map.entry("lifecycle_stage", "REGISTERED"),
                                Map.entry("target_market", "冠脉市场"),
                                Map.entry("target_market_en", "Coronary market"),
                                Map.entry("cover_image", "/admin-api/infra/file/28/get/showroom/preview/product-cover-priority-cover.png"))))
                .revisionId(), 901L);

        mockFile(890L, 28L, "showroom/preview/product-cover-priority-cover.png", "image/png");
        mockFile(891L, 28L, "showroom/preview/product-cover-priority-preview.png", "image/png");
        mockFile(892L, 29L, "showroom/narration/product-cover-priority-zh.wav", "audio/wav");
        mockFile(893L, 29L, "showroom/narration/product-cover-priority-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, liveRevision.productId(),
                liveRevision.revisionId(), 891L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), liveRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "产品中文讲解", 892L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(), liveRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "English product narration", 893L, "ruoxi");

        var hall = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "COVER_PRIORITY", "封面优先展厅", "Cover Priority Hall",
                "封面优先展厅说明", "Cover priority hall overview")).getCheckedData();
        mockFile(894L, 28L, "showroom/preview/hall-cover-priority.png", "image/png");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 894L);
        mockFile(895L, 29L, "showroom/narration/hall-cover-priority-zh.wav", "audio/wav");
        mockFile(896L, 29L, "showroom/narration/hall-cover-priority-en.wav", "audio/wav");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "封面优先展厅说明", 895L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "Cover priority hall overview", 896L, "ruoxi");
        saveHallCanvasLayout(hall.hallId(), liveRevision.productId());

        Object payload = getWebsiteConfigPayload();
        Object product = recordListAccessor(recordListAccessor(payload, "showrooms").get(0), "products").get(0);
        assertTrue(String.valueOf((Object) recordAccessor(product, "previewImageUrl")).startsWith("/showroom/sites/"));

        var displayHall = displayController.getHall(hall.hallId()).getCheckedData();
        assertEquals("/admin-api/infra/file/28/get/showroom/preview/product-cover-priority-cover.png",
                displayHall.productCards().get(0).previewImageUrl());
    }

    @Test
    void productCoverImageShouldRequireApprovedProductBeforeGeneration() {
        var draft = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "YT-COVER-DRAFT", "待审核封面产品", "Draft Cover Product",
                Map.of("target_market", "中国", "core_selling_points", "待审卖点")));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> withLoginUser(300L, () -> adminController.generateProductCoverImage(new ShowroomAdminController.ProductCoverGenerateReqVO(
                        draft.productId(), "YT-COVER-DRAFT", "待审核封面产品", "Draft Cover Product",
                        Map.of("target_market", "中国", "core_selling_points", "待审卖点")))));

        assertTrue(error.getMessage().contains("需要产品基础信息经过审核之后才可以AI生成封面"));
    }

    @Test
    void imagePromptManagementShouldSaveNewCurrentVersionAndExposeHistory() {
        mockShowroomRoleChecks();

        var saved = withLoginUser(300L, () -> adminController.saveImagePromptVersion(
                new ShowroomAdminController.ImagePromptVersionSaveReqVO(
                        "PRODUCT_COVER",
                        "主体是“{{product_name_cn}}”，英文名参考“{{product_name_en}}”",
                        "integration saved prompt"))).getCheckedData();
        var current = withLoginUser(300L, () -> adminController.getImagePromptCurrent("PRODUCT_COVER"))
                .getCheckedData();
        var history = withLoginUser(300L, () -> adminController.getImagePromptHistory("PRODUCT_COVER"))
                .getCheckedData();

        assertEquals(saved.promptVersionId(), current.promptVersionId());
        assertEquals("PRODUCT_COVER", current.sceneCode());
        assertTrue(current.versionNo() >= 2);
        assertEquals(List.of("product_name_cn", "product_name_en"), current.placeholderCodes());
        assertEquals(saved.promptVersionId(), history.get(0).promptVersionId());
        assertTrue(history.get(0).current());
        assertTrue(history.size() >= 2);
    }

    @Test
    void imagePromptManagementShouldRejectNonPublicityUsers() {
        mockShowroomRoleChecks();

        ServiceException denied = assertThrows(ServiceException.class,
                () -> withLoginUser(100L, () -> adminController.getImagePromptCurrent("PRODUCT_COVER")));

        assertTrue(denied.getMessage().contains("当前用户无权执行查看提示管理"));
    }

    @Test
    void productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct() {
        var liveRevision = publishBaselineProduct();
        var snapshot = contentService.getProduct(liveRevision.productId());
        mockCoverCodexCli();
        when(fileService.createFileAndReturnId(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.contains("product-" + snapshot.productCode()),
                org.mockito.ArgumentMatchers.eq("showroom/product/cover"),
                org.mockito.ArgumentMatchers.eq("image/png")))
                .thenReturn(99221L);
        mockFile(99221L, 29L, "showroom/product/cover/generated-product-cover.png", "image/png");

        var generated = withLoginUser(300L, () -> adminController.generateProductCoverImage(
                new ShowroomAdminController.ProductCoverGenerateReqVO(
                        liveRevision.productId(), snapshot.productCode(), liveRevision.nameCn(), liveRevision.nameEn(),
                        Map.of("target_market", "旧市场", "registration_certificate", "注册证 V1",
                                "core_selling_points", "旧卖点")))).getCheckedData();

        assertEquals("/admin-api/infra/file/29/get/showroom/product/cover/generated-product-cover.png",
                generated.coverImage());
    }

    @Test
    void batchGenerateProductNarrationAudioShouldProcessPublishedProductsAndExposeAutoCheckState() throws Exception {
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();
        var successRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "BATCH-AUDIO-OK", "批量语音成功产品", "Batch Audio Success Product",
                        Map.of("target_market", "中国", "core_selling_points", "批量语音成功卖点")))
                .revisionId(), 901L);
        var existingRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "BATCH-AUDIO-EXISTING", "批量语音已有音频产品", "Batch Audio Existing Product",
                        Map.of("target_market", "中国", "core_selling_points", "批量语音已有音频卖点")))
                .revisionId(), 901L);
        var missingScriptRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "BATCH-AUDIO-SCRIPT", "批量语音缺稿产品", "Batch Audio Script Product",
                        Map.of("target_market", "中国", "core_selling_points", "批量语音缺稿卖点")))
                .revisionId(), 901L);
        var failureRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "BATCH-AUDIO-FAIL", "批量语音失败产品", "Batch Audio Failure Product",
                        Map.of("target_market", "中国", "core_selling_points", "批量语音失败卖点")))
                .revisionId(), 901L);
        contentService.saveProductDraft(new ShowroomProductDraft(
                null, "BATCH-AUDIO-DRAFT", "批量语音草稿产品", "Batch Audio Draft Product",
                Map.of("target_market", "中国", "core_selling_points", "未发布卖点")));

        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", successRevision.productId(), successRevision.revisionId(), "PUBLIC", "ZH",
                "批量语音中文讲解", null, null, true)).getCheckedData();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", successRevision.productId(), successRevision.revisionId(), "PUBLIC", "EN",
                "Batch audio English script", null, null, true)).getCheckedData();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", failureRevision.productId(), failureRevision.revisionId(), "PUBLIC", "ZH",
                "批量失败中文讲解", null, null, false)).getCheckedData();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", failureRevision.productId(), failureRevision.revisionId(), "PUBLIC", "EN",
                "Batch audio failure English script", null, null, false)).getCheckedData();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", missingScriptRevision.productId(), missingScriptRevision.revisionId(), "PUBLIC", "ZH",
                "只有中文讲解稿", null, null, false)).getCheckedData();
        publishNarration(ShowroomNarrationTargetType.PRODUCT, existingRevision.productId(), existingRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "已有中文讲解", 99141L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, existingRevision.productId(), existingRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "Existing English narration", 99142L, "ruoxi");

        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("批量语音中文讲解", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(2));
        when(aliyunNlsTtsSynthesizer.synthesize("Batch audio English script", tts, "ruoxi", "saved-token",
                "saved-appkey")).thenReturn(buildSilentWavBytes(3));
        when(aliyunNlsTtsSynthesizer.synthesize("批量失败中文讲解", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenThrow(new IllegalStateException("SHOWROOM_AUDIO_GENERATION_FAILED: mock batch audio failure"));
        when(fileService.createFileAndReturnId(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav"))).thenReturn(99131L, 99132L);
        mockFile(99131L, 29L, "showroom/narration/20260520/batch-audio-ok-zh.wav", "audio/wav");
        mockFile(99132L, 29L, "showroom/narration/20260520/batch-audio-ok-en.wav", "audio/wav");
        mockFile(99141L, 29L, "showroom/narration/20260520/batch-audio-existing-zh.wav", "audio/wav");
        mockFile(99142L, 29L, "showroom/narration/20260520/batch-audio-existing-en.wav", "audio/wav");

        var summary = withLoginUser(300L, () -> adminController.batchGenerateProductNarrationAudio(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-AUDIO", null, null, null)))
                .getCheckedData();
        var state = withLoginUser(300L, () -> adminController.getProductBatchGenerateNarrationAudioState())
                .getCheckedData();

        assertEquals(5, summary.matchedCount());
        assertEquals(4, summary.publishedCount());
        assertEquals(1, summary.skippedUnpublishedCount());
        assertEquals(1, summary.skippedExistingCount());
        assertEquals(1, summary.skippedMissingScriptCount());
        assertEquals(1, summary.succeededCount());
        assertEquals(1, summary.failedCount());
        assertTrue(summary.autoCheckEnabled());
        assertEquals(1, summary.remainingActionableCount());
        assertEquals(1, summary.failures().size());
        assertEquals(failureRevision.productId(), summary.failures().get(0).productId());
        assertTrue(summary.failures().get(0).reason().contains("mock batch audio failure"));
        assertTrue(state.enabled());
        assertEquals(1, state.skippedExistingCount());
        assertEquals(1, state.skippedMissingScriptCount());
        assertEquals(1, state.failedCount());
        assertEquals(1, state.remainingActionableCount());
        assertTrue(state.lastFailureMessage().contains("mock batch audio failure"));

        var liveZh = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, successRevision.productId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH)).orElseThrow();
        var liveEn = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, successRevision.productId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN)).orElseThrow();
        assertEquals(successRevision.revisionId(), liveZh.sourceRevisionId());
        assertEquals(successRevision.revisionId(), liveEn.sourceRevisionId());
        assertEquals("ruoxi", liveZh.voice());
        assertEquals("ruoxi", liveEn.voice());
    }

    @Test
    void batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts() {
        seedPublicityApproverRole(300L);
        mockShowroomRoleChecks();
        var successRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "BATCH-COVER-OK", "批量封面成功产品", "Batch Cover Success Product",
                        Map.of("target_market", "中国", "core_selling_points", "批量封面成功卖点")))
                .revisionId(), 901L);
        var failureRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "BATCH-COVER-FAIL", "批量封面失败产品", "Batch Cover Failure Product",
                        Map.of("target_market", "中国", "core_selling_points", "批量封面失败卖点")))
                .revisionId(), 901L);
        contentService.saveProductDraft(new ShowroomProductDraft(
                null, "BATCH-COVER-DRAFT", "批量封面草稿产品", "Batch Cover Draft Product",
                Map.of("target_market", "中国", "core_selling_points", "未发布封面卖点")));

        mockFile(99261L, 28L, "showroom/preview/batch-cover-success.png", "image/png");
        mockFile(99262L, 29L, "showroom/narration/batch-cover-success-zh.wav", "audio/wav");
        mockFile(99263L, 29L, "showroom/narration/batch-cover-success-en.wav", "audio/wav");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, successRevision.productId(),
                successRevision.revisionId(), 99261L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, successRevision.productId(), successRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "批量封面成功产品中文讲解", 99262L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, successRevision.productId(), successRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "Batch cover success product narration", 99263L, "ruoxi");

        mockCoverCodexCli();
        when(fileService.createFileAndReturnId(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.contains("product-BATCH-COVER-OK"),
                org.mockito.ArgumentMatchers.eq("showroom/product/cover"),
                org.mockito.ArgumentMatchers.eq("image/png")))
                .thenReturn(99251L);
        mockFile(99251L, 29L, "showroom/product/cover/batch-cover-ok.png", "image/png");

        var summary = withLoginUser(300L, () -> adminController.batchGenerateProductCoverImage(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-COVER", null, null, null)))
                .getCheckedData();

        assertEquals(3, summary.matchedCount());
        assertEquals(2, summary.publishedCount());
        assertEquals(1, summary.skippedUnpublishedCount());
        assertEquals(1, summary.succeededCount());
        assertEquals(1, summary.failedCount());
        assertEquals(1, summary.failures().size());
        assertEquals(failureRevision.productId(), summary.failures().get(0).productId());
        assertTrue(summary.failures().get(0).reason().contains("SHOWROOM_COVER_GENERATION_FAILED"));
        assertNotNull(summary.taskId());
        assertEquals("WAITING", summary.taskStatus());
        assertEquals(1, summary.remainingPendingCount());
        assertNotNull(summary.nextCheckAt());

        var currentSuccessRevision = contentService.requireCurrentProductRevision(successRevision.productId());
        assertTrue(currentSuccessRevision.revisionNo() > successRevision.revisionNo());
        assertEquals("/admin-api/infra/file/29/get/showroom/product/cover/batch-cover-ok.png",
                currentSuccessRevision.fields().get("cover_image"));
        assertEquals(currentSuccessRevision.revisionId(), narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, successRevision.productId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH)).orElseThrow().sourceRevisionId());
        assertEquals(currentSuccessRevision.revisionId(), narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, successRevision.productId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN)).orElseThrow().sourceRevisionId());
        assertEquals(successRevision.revisionId(), previewAssetService.live(new ShowroomPreviewAssetKey(
                ShowroomPreviewAssetTargetType.PRODUCT, successRevision.productId())).orElseThrow().sourceRevisionId());
        assertEquals(failureRevision.revisionId(),
                contentService.requireCurrentProductRevision(failureRevision.productId()).revisionId());
    }

    @Test
    void batchGenerateProductMediaShouldRejectNonPublicityUsers() {
        mockShowroomRoleChecks();

        ServiceException audioDenied = assertThrows(ServiceException.class,
                () -> withLoginUser(100L, () -> adminController.batchGenerateProductNarrationAudio(
                        new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH", null, null, null))));
        assertTrue(audioDenied.getMessage().contains("当前用户无权执行批量生成产品语音"));

        ServiceException coverDenied = assertThrows(ServiceException.class,
                () -> withLoginUser(100L, () -> adminController.batchGenerateProductCoverImage(
                        new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH", null, null, null))));
        assertTrue(coverDenied.getMessage().contains("当前用户无权执行批量生成产品封面"));
    }


    @Test
    void narrationGenerateAudioShouldUseSharedAliyunNlsDefaultsAndPersistMetadata() throws Exception {
        var liveRevision = publishBaselineProduct();
        var draft = adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "产品自动讲解", null, null, true)).getCheckedData();

        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("产品自动讲解", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(2));
        when(fileService.createFileAndReturnId(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav"))).thenReturn(99111L);
        FileDO narrationAudioFile = FileDO.builder()
                .id(99111L)
                .configId(29L)
                .name("product-88-zh-ruoxi.wav")
                .path("showroom/narration/20260519/product-88-zh-ruoxi.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/20260519/product-88-zh-ruoxi.wav")
                .type("audio/wav")
                .size(2048L)
                .build();
        when(fileMapper.selectById(99111L)).thenReturn(narrationAudioFile);

        var generated = adminController.generateNarrationAudio(
                new ShowroomAdminController.NarrationAudioGenerateReqVO(draft.id())).getCheckedData();

        assertEquals(99111L, generated.audioFileId());
        assertEquals(2, generated.audioDurationSeconds());
        assertEquals("ruoxi", generated.voice());
        assertEquals("AUDIO_GENERATED", generated.generationStatus().name());

        narrationService.publish(narrationService.gaoxinApprove(
                narrationService.supervisorApprove(narrationService.submit(generated.id()).id(), 200L).id(), 300L).id());

        var row = withLoginUser(300L,
                () -> adminController.getProductPage(new ShowroomAdminController.PageQueryReqVO(null, 1, 20)))
                .getCheckedData().getList().stream()
                .filter(item -> item.productId().equals(liveRevision.productId()))
                .findFirst()
                .orElseThrow();
        assertNotNull(row.latestNarration());
        assertTrue(row.latestNarration().audioReady());
        assertEquals("ruoxi", row.latestNarration().voice());
        assertEquals("/admin-api/infra/file/29/get/showroom/narration/20260519/product-88-zh-ruoxi.wav",
                row.latestNarration().audioUrl());
    }

    @Test
    void narrationTtsDefaultsEndpointsShouldExposeAndSaveSharedConfig() {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.getVoiceStatus(tts))
                .thenReturn(new AiTtsAliyunNlsCredentialService.VoiceStatus(true, true, "saved", "ruoxi"));
        when(aliyunNlsCredentialService.getAccessTokenStatus(tts))
                .thenReturn(new AiTtsAliyunNlsCredentialService.AccessTokenStatus(true, true, "saved", "test****abcd"));
        when(aliyunNlsCredentialService.getAppKeyStatus(tts))
                .thenReturn(new AiTtsAliyunNlsCredentialService.AppKeyStatus(true, true, "saved", "i0nm****UXM9"));

        var defaults = adminController.getNarrationTtsDefaults().getCheckedData();

        assertEquals("ruoxi", defaults.defaultVoice());
        assertTrue(defaults.voiceSaved());
        assertEquals("saved", defaults.voiceSource());
        assertTrue(defaults.tokenConfigured());
        assertEquals("saved", defaults.tokenSource());
        assertEquals("test****abcd", defaults.maskedAccessToken());
        assertTrue(defaults.appKeyConfigured());
        assertEquals("saved", defaults.appKeySource());
        assertEquals("i0nm****UXM9", defaults.maskedAppKey());

        adminController.saveNarrationTtsDefaultVoice(
                new ShowroomAdminController.NarrationTtsDefaultVoiceReqVO("xiaoyun"));
        adminController.saveNarrationTtsDefaultToken(
                new ShowroomAdminController.NarrationTtsDefaultTokenReqVO("new-token"));
        adminController.saveNarrationTtsDefaultAppKey(
                new ShowroomAdminController.NarrationTtsDefaultAppKeyReqVO("new-appkey"));
    }

    @Test
    void productGenerateNarrationAudioShouldReuseRecordedEnglishNarrationAndSharedDefaultVoice() throws Exception {
        var liveRevision = publishBaselineProduct();
        ShowroomNarrationVersion zhDraft = adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "中文讲解稿", null, null, false)).getCheckedData();
        ShowroomNarrationVersion enDraft = adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "EN",
                "Recorded English narration script", null, null, false)).getCheckedData();
        when(productNarrationCodexService.translateZhToEn("中文讲解稿"))
                .thenReturn("Fallback generated English narration script");

        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("中文讲解稿", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(2));
        when(aliyunNlsTtsSynthesizer.synthesize("Recorded English narration script", tts, "ruoxi",
                "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(3));
        when(fileService.createFileAndReturnId(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.contains("product-" + liveRevision.productId()),
                org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav")))
                .thenReturn(99211L)
                .thenReturn(99212L);

        FileDO zhAudioFile = FileDO.builder()
                .id(99211L)
                .configId(29L)
                .name("product-zh-ruoxi.wav")
                .path("showroom/narration/generated/product-zh-ruoxi.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/generated/product-zh-ruoxi.wav")
                .type("audio/wav")
                .size(500L)
                .build();
        FileDO enAudioFile = FileDO.builder()
                .id(99212L)
                .configId(29L)
                .name("product-en-ruoxi.wav")
                .path("showroom/narration/generated/product-en-ruoxi.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/generated/product-en-ruoxi.wav")
                .type("audio/wav")
                .size(700L)
                .build();
        when(fileMapper.selectById(99211L)).thenReturn(zhAudioFile);
        when(fileMapper.selectById(99212L)).thenReturn(enAudioFile);

        var generated = withLoginUser(300L, () -> adminController.generateProductNarrationAudio(
                new ShowroomAdminController.ProductNarrationGenerateReqVO(liveRevision.productId(), liveRevision.revisionId())))
                .getCheckedData();

        assertEquals(liveRevision.productId(), generated.productId());
        assertEquals(zhDraft.id(), generated.zhNarrationVersionId());
        assertEquals(enDraft.id(), generated.enNarrationVersionId());
        assertEquals("ruoxi", generated.voice());

        var zhNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var enNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();

        assertEquals(99211L, zhNarration.audioFileId());
        assertEquals(99212L, enNarration.audioFileId());
        assertEquals("/admin-api/infra/file/29/get/showroom/narration/generated/product-zh-ruoxi.wav",
                zhNarration.audioUrl());
        assertEquals("/admin-api/infra/file/29/get/showroom/narration/generated/product-en-ruoxi.wav",
                enNarration.audioUrl());
        assertEquals("Recorded English narration script", enNarration.scriptText());
        assertEquals("ruoxi", zhNarration.voice());
        assertEquals("ruoxi", enNarration.voice());
        verify(productNarrationCodexService, never()).translateZhToEn(anyString());

        var row = withLoginUser(300L,
                () -> adminController.getProductPage(new ShowroomAdminController.PageQueryReqVO(null, 1, 20)))
                .getCheckedData().getList().stream()
                .filter(item -> item.productId().equals(liveRevision.productId()))
                .findFirst()
                .orElseThrow();
        assertNotNull(row.latestNarration());
        assertEquals("ruoxi", row.latestNarration().voice());
        assertTrue(row.latestNarration().audioReady());
    }

    @Test
    void productGenerateNarrationAudioShouldFailWhenChineseNarrationMissing() {
        var liveRevision = publishBaselineProduct();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> withLoginUser(300L, () -> adminController.generateProductNarrationAudio(
                        new ShowroomAdminController.ProductNarrationGenerateReqVO(liveRevision.productId(), liveRevision.revisionId()))));

        assertTrue(exception.getMessage().contains("SHOWROOM_SCRIPT_GENERATION_FAILED"));
    }

    @Test
    void productGenerateNarrationAudioShouldFailWhenEnglishNarrationMissing() {
        var liveRevision = publishBaselineProduct();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "只有中文讲解稿", null, null, false)).getCheckedData();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> withLoginUser(300L, () -> adminController.generateProductNarrationAudio(
                        new ShowroomAdminController.ProductNarrationGenerateReqVO(liveRevision.productId(), liveRevision.revisionId()))));

        assertTrue(exception.getMessage().contains("SHOWROOM_TRANSLATION_FAILED"));
    }

    @Test
    void productGenerateNarrationScriptShouldPersistZhAndEnDrafts() {
        var liveRevision = publishBaselineProduct();
        when(productNarrationCodexService.generateScript(contentService.getProduct(liveRevision.productId()), liveRevision))
                .thenReturn("基于产品资料生成的中文讲解稿");
        when(productNarrationCodexService.translateZhToEn("基于产品资料生成的中文讲解稿"))
                .thenReturn("Generated English narration script");

        var generated = withLoginUser(300L, () -> adminController.generateProductNarrationScript(
                new ShowroomAdminController.ProductNarrationGenerateReqVO(liveRevision.productId(), liveRevision.revisionId())))
                .getCheckedData();
        var generatedEn = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();

        assertEquals(ShowroomNarrationTargetType.PRODUCT, generated.key().targetType());
        assertEquals(ShowroomNarrationLanguage.ZH, generated.key().language());
        assertEquals(liveRevision.productId(), generated.key().targetId());
        assertEquals(liveRevision.revisionId(), generated.sourceRevisionId());
        assertEquals("基于产品资料生成的中文讲解稿", generated.scriptText());
        assertTrue(generated.generatedByAi());
        assertEquals(ShowroomNarrationLanguage.EN, generatedEn.key().language());
        assertEquals(liveRevision.productId(), generatedEn.key().targetId());
        assertEquals(liveRevision.revisionId(), generatedEn.sourceRevisionId());
        assertEquals("Generated English narration script", generatedEn.scriptText());
        assertTrue(generatedEn.generatedByAi());
    }

    @Test
    void productPageShouldExposeLatestZhNarrationAudioAndVoiceWithoutFallback() {
        var withAudioRevision = publishBaselineProduct();
        FileDO zhAudioFile = FileDO.builder()
                .id(99101L)
                .configId(29L)
                .name("product-audio.wav")
                .path("showroom/narration/test/product-audio.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/test/product-audio.wav")
                .type("audio/wav")
                .size(456L)
                .build();
        FileDO enAudioFile = FileDO.builder()
                .id(99102L)
                .configId(29L)
                .name("product-audio-en.wav")
                .path("showroom/narration/test/product-audio-en.wav")
                .url("http://127.0.0.1:9000/yudao/showroom/narration/test/product-audio-en.wav")
                .type("audio/wav")
                .size(457L)
                .build();
        when(fileMapper.selectById(99101L)).thenReturn(zhAudioFile);
        when(fileMapper.selectById(99102L)).thenReturn(enAudioFile);
        ShowroomNarrationKey zhKeyWithAudio = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT,
                withAudioRevision.productId(), ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);
        ShowroomNarrationKey enKeyWithAudio = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT,
                withAudioRevision.productId(), ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN);

        ShowroomNarrationVersion zhWithAudio = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                zhKeyWithAudio.targetType(), zhKeyWithAudio.targetId(), withAudioRevision.revisionId(),
                zhKeyWithAudio.audienceType(), zhKeyWithAudio.language(), "中文讲解", true));
        zhWithAudio = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                zhWithAudio.id(), 99101L, 65, "ruoxi"));
        narrationService.publish(narrationService.gaoxinApprove(
                narrationService.supervisorApprove(narrationService.submit(zhWithAudio.id()).id(), 200L).id(), 300L).id());

        ShowroomNarrationVersion enWithAudio = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                enKeyWithAudio.targetType(), enKeyWithAudio.targetId(), withAudioRevision.revisionId(),
                enKeyWithAudio.audienceType(), enKeyWithAudio.language(), "English narration", true));
        enWithAudio = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                enWithAudio.id(), 99102L, 66, "xiaoyun"));
        narrationService.publish(narrationService.gaoxinApprove(
                narrationService.supervisorApprove(narrationService.submit(enWithAudio.id()).id(), 201L).id(), 301L).id());

        var noAudioLatestRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "YT-GW-002", "导管鞘组 V2", "Introducer Sheath Set II",
                        Map.of("target_market", "市场二", "registration_certificate", "注册证 V2",
                                "core_selling_points", "卖点二")))
                .revisionId(), 902L);
        ShowroomNarrationKey zhKeyNoAudio = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT,
                noAudioLatestRevision.productId(), ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);

        ShowroomNarrationVersion zhPublishedOld = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                zhKeyNoAudio.targetType(), zhKeyNoAudio.targetId(), noAudioLatestRevision.revisionId(),
                zhKeyNoAudio.audienceType(), zhKeyNoAudio.language(), "旧中文讲解", false));
        zhPublishedOld = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                zhPublishedOld.id(), 99201L, 67, "ruoxi"));
        narrationService.publish(narrationService.gaoxinApprove(
                narrationService.supervisorApprove(narrationService.submit(zhPublishedOld.id()).id(), 202L).id(), 302L).id());

        ShowroomNarrationVersion zhLatestWithoutAudio = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                zhKeyNoAudio.targetType(), zhKeyNoAudio.targetId(), noAudioLatestRevision.revisionId(),
                zhKeyNoAudio.audienceType(), zhKeyNoAudio.language(), "新中文讲解无音频", false));

        var page = withLoginUser(300L,
                () -> adminController.getProductPage(new ShowroomAdminController.PageQueryReqVO(null, 1, 20)))
                .getCheckedData();
        var withAudioRow = page.getList().stream()
                .filter(row -> row.productId().equals(withAudioRevision.productId()))
                .findFirst()
                .orElseThrow();
        var noAudioRow = page.getList().stream()
                .filter(row -> row.productId().equals(noAudioLatestRevision.productId()))
                .findFirst()
                .orElseThrow();

        assertNotNull(withAudioRow.latestNarration());
        assertEquals(zhWithAudio.id(), withAudioRow.latestNarration().narrationVersionId());
        assertEquals("ZH", withAudioRow.latestNarration().language());
        assertEquals("PUBLIC", withAudioRow.latestNarration().audienceType());
        assertTrue(withAudioRow.latestNarration().audioReady());
        assertEquals("/admin-api/infra/file/29/get/showroom/narration/test/product-audio.wav",
                withAudioRow.latestNarration().audioUrl());
        assertEquals("ruoxi", withAudioRow.latestNarration().voice());

        assertNotNull(noAudioRow.latestNarration());
        assertEquals(zhLatestWithoutAudio.id(), noAudioRow.latestNarration().narrationVersionId());
        assertFalse(noAudioRow.latestNarration().audioReady());
        assertEquals("", noAudioRow.latestNarration().audioUrl());
        assertEquals("", noAudioRow.latestNarration().voice());
    }

    @Test
    void hallGenerateNarrationAudioShouldPublishBothLanguagesAndExposeListAudio() throws Exception {
        seedPublicityApproverRole(300L);
        var hall = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "HALL_AUDIO", "语音展柜", "Audio Hall", "中文展柜讲解", "English hall narration")).getCheckedData();

        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("中文展柜讲解", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(2));
        when(aliyunNlsTtsSynthesizer.synthesize("English hall narration", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(3));
        when(fileService.createFileAndReturnId(any(byte[].class), org.mockito.ArgumentMatchers.contains("hall-" + hall.hallId()),
                org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav")))
                .thenReturn(99301L)
                .thenReturn(99302L);
        mockFile(99301L, 29L, "showroom/narration/generated/hall-" + hall.hallId() + "-zh-ruoxi.wav",
                "audio/wav");
        mockFile(99302L, 29L, "showroom/narration/generated/hall-" + hall.hallId() + "-en-ruoxi.wav",
                "audio/wav");

        var generated = withLoginUser(300L, () -> adminController.generateHallNarrationAudio(
                new ShowroomAdminController.HallNarrationGenerateReqVO(hall.hallId()))).getCheckedData();

        assertEquals(hall.hallId(), generated.hallId());
        assertEquals("ruoxi", generated.voice());

        ShowroomNarrationVersion liveZh = narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.HALL, hall.hallId(),
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH))
                .orElseThrow();
        ShowroomNarrationVersion liveEn = narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.HALL, hall.hallId(),
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN))
                .orElseThrow();
        assertEquals(hall.hallId(), liveZh.sourceRevisionId());
        assertEquals(hall.hallId(), liveEn.sourceRevisionId());
        assertEquals("中文展柜讲解", liveZh.scriptText());
        assertEquals("English hall narration", liveEn.scriptText());
        assertEquals(99301L, liveZh.audioFileId());
        assertEquals(99302L, liveEn.audioFileId());

        var row = withLoginUser(300L, () -> adminController.getHallPage(
                        new ShowroomAdminController.PageQueryReqVO(null, 1, 20)))
                .getCheckedData().stream()
                .filter(item -> item.hallId().equals(hall.hallId()))
                .findFirst()
                .orElseThrow();
        assertNotNull(row.zhNarration());
        assertNotNull(row.enNarration());
        assertTrue(row.zhNarration().audioReady());
        assertTrue(row.enNarration().audioReady());
        assertTrue(row.zhNarration().audioUrl().contains("showroom/narration/generated/hall-" + hall.hallId()
                + "-zh-ruoxi.wav"));
        assertTrue(row.enNarration().audioUrl().contains("showroom/narration/generated/hall-" + hall.hallId()
                + "-en-ruoxi.wav"));
    }

    @Test
    void hallCanvasBackgroundShouldSaveOverwriteClearAndExposeListValue() {
        var hall = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "HALL_CANVAS_BG", "背景图展柜", "Canvas Background Hall", "中文描述", "English description")).getCheckedData();

        var first = withLoginUser(300L, () -> adminController.updateHallCanvasBackground(
                new ShowroomAdminController.HallCanvasBackgroundReqVO(hall.hallId(),
                        " /admin-api/infra/file/28/get/showroom/hall/canvas-background/first.png ")))
                .getCheckedData();
        assertEquals("/admin-api/infra/file/28/get/showroom/hall/canvas-background/first.png",
                first.canvasBackgroundImageUrl());

        var overwritten = withLoginUser(300L, () -> adminController.updateHallCanvasBackground(
                new ShowroomAdminController.HallCanvasBackgroundReqVO(hall.hallId(),
                        "/admin-api/infra/file/28/get/showroom/hall/canvas-background/second.png")))
                .getCheckedData();
        assertEquals("/admin-api/infra/file/28/get/showroom/hall/canvas-background/second.png",
                overwritten.canvasBackgroundImageUrl());

        var row = withLoginUser(300L, () -> adminController.getHallPage(
                        new ShowroomAdminController.PageQueryReqVO(null, 1, 20)))
                .getCheckedData().stream()
                .filter(item -> item.hallId().equals(hall.hallId()))
                .findFirst()
                .orElseThrow();
        assertEquals("/admin-api/infra/file/28/get/showroom/hall/canvas-background/second.png",
                row.canvasBackgroundImageUrl());

        var cleared = withLoginUser(300L, () -> adminController.updateHallCanvasBackground(
                new ShowroomAdminController.HallCanvasBackgroundReqVO(hall.hallId(), "")))
                .getCheckedData();
        assertEquals("", cleared.canvasBackgroundImageUrl());

        var clearedRow = withLoginUser(300L, () -> adminController.getHallPage(
                        new ShowroomAdminController.PageQueryReqVO(null, 1, 20)))
                .getCheckedData().stream()
                .filter(item -> item.hallId().equals(hall.hallId()))
                .findFirst()
                .orElseThrow();
        assertEquals("", clearedRow.canvasBackgroundImageUrl());
    }

    @Test
    void hallBatchGenerateNarrationAudioShouldPublishValidHallsAndReportMissingDescriptions() throws Exception {
        seedPublicityApproverRole(300L);
        var validHall = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "HALL_BATCH_OK", "批量语音展柜", "Batch Audio Hall", "批量中文展柜讲解",
                "Batch English hall narration")).getCheckedData();
        var invalidHall = adminController.createHall(new ShowroomAdminController.HallSaveReqVO(
                "HALL_BATCH_MISSING", "缺英文描述展柜", "Missing English Hall", "缺少英文描述的中文讲解",
                "")).getCheckedData();

        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("批量中文展柜讲解", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(2));
        when(aliyunNlsTtsSynthesizer.synthesize("Batch English hall narration", tts, "ruoxi", "saved-token",
                "saved-appkey")).thenReturn(buildSilentWavBytes(3));
        when(fileService.createFileAndReturnId(any(byte[].class), org.mockito.ArgumentMatchers.contains("hall-"
                        + validHall.hallId()), org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav")))
                .thenReturn(99311L)
                .thenReturn(99312L);
        mockFile(99311L, 29L, "showroom/narration/generated/hall-" + validHall.hallId() + "-zh-ruoxi.wav",
                "audio/wav");
        mockFile(99312L, 29L, "showroom/narration/generated/hall-" + validHall.hallId() + "-en-ruoxi.wav",
                "audio/wav");

        var batch = withLoginUser(300L, adminController::batchGenerateHallNarrationAudio).getCheckedData();

        assertEquals(2, batch.matchedCount());
        assertEquals(1, batch.succeededCount());
        assertEquals(1, batch.failedCount());
        assertEquals(invalidHall.hallId(), batch.failures().getFirst().hallId());
        assertEquals("HALL_BATCH_MISSING", batch.failures().getFirst().hallCode());
        assertTrue(batch.failures().getFirst().reason().contains("hall EN description is required"));

        ShowroomNarrationVersion liveZh = narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.HALL, validHall.hallId(),
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH))
                .orElseThrow();
        ShowroomNarrationVersion liveEn = narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.HALL, validHall.hallId(),
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN))
                .orElseThrow();
        assertEquals(99311L, liveZh.audioFileId());
        assertEquals(99312L, liveEn.audioFileId());
    }

    private ShowroomProductRevision publishBaselineProduct() {
        seedPublicityApproverRole(300L);
        mockFile(98000L, 28L, "showroom/preview/baseline-product-cover.png", "image/png");
        var baseline = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "YT-GW-001", "导管鞘组 V1", "Introducer Sheath Set",
                productFieldsWithBaselineCover(Map.of("target_market", "旧市场", "registration_certificate", "注册证 V1",
                        "core_selling_points", "旧卖点"))));
        return contentService.publishProductRevision(baseline.revisionId(), 901L);
    }

    private static Map<String, String> productFieldsWithBaselineCover(Map<String, String> fields) {
        Map<String, String> result = new HashMap<>(fields);
        result.put("cover_image", BASELINE_PRODUCT_COVER_IMAGE);
        return result;
    }

    private static Map<String, String> productFieldsWithCoreAndBaselineCover(Map<String, String> fields) {
        Map<String, String> result = productFieldsWithBaselineCover(fields);
        result.putIfAbsent("owner_company_id", "124");
        result.putIfAbsent("product_owner_type", "YINGTAI");
        result.putIfAbsent("lifecycle_stage", "REGISTERED");
        return result;
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
            when(fileService.getFileContent(configId, path)).thenReturn(path.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void mockCoverCodexCli() {
        try {
            Path tempDir = Files.createTempDirectory("showroom-cover-codex-");
            Path generatedImage = tempDir.resolve("generated-cover.png");
            Files.write(generatedImage, ONE_PIXEL_PNG_BYTES);
            Path command = tempDir.resolve("fake-codex-cover.cmd");
            String script = """
                    @echo off
                    setlocal EnableDelayedExpansion
                    set "OUT="
                    :parse
                    if "%~1"=="" goto afterArgs
                    if "%~1"=="-o" (
                      set "OUT=%~2"
                      shift
                    )
                    if "%~1"=="--output-last-message" (
                      set "OUT=%~2"
                      shift
                    )
                    shift
                    goto parse
                    :afterArgs
                    if "%OUT%"=="" exit /b 9
                    set "STDIN_FILE=%OUT%.stdin.txt"
                    more > "%STDIN_FILE%"
                    findstr /C:"BATCH-COVER-FAIL" "%STDIN_FILE%" >nul
                    if not errorlevel 1 (
                      echo SHOWROOM_COVER_GENERATION_FAILED: mock cover generation failure
                      exit /b 7
                    )
                    > "%OUT%" echo __IMAGE_PATH__
                    exit /b 0
                    """.replace("__IMAGE_PATH__", generatedImage.toString());
            Files.writeString(command, script, StandardCharsets.UTF_8);
            YudaoAiProperties.CodexCli codexCli = new YudaoAiProperties.CodexCli();
            codexCli.setCommand(command.toString());
            codexCli.setTimeoutMs(5000L);
            codexCli.setWorkingDirectory("D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro");
            when(yudaoAiProperties.getCodexCli()).thenReturn(codexCli);
        } catch (IOException exception) {
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

    private void mockProductNarrationAudioGeneration(Long productId, String zhScriptText, String enScriptText,
                                                     Long zhAudioFileId, Long enAudioFileId) throws IOException {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize(zhScriptText, tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(2));
        when(aliyunNlsTtsSynthesizer.synthesize(enScriptText, tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(3));
        when(fileService.createFileAndReturnId(any(byte[].class), org.mockito.ArgumentMatchers.contains("product-" + productId),
                org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav")))
                .thenReturn(zhAudioFileId)
                .thenReturn(enAudioFileId);
        mockFile(zhAudioFileId, 29L, "showroom/narration/generated/product-" + productId + "-zh-ruoxi.wav",
                "audio/wav");
        mockFile(enAudioFileId, 29L, "showroom/narration/generated/product-" + productId + "-en-ruoxi.wav",
                "audio/wav");
    }

    private void seedNotifyTemplate() {
        NotifyTemplateDO template = NotifyTemplateDO.builder()
                .name("展厅指派提醒")
                .code(ASSIGNMENT_TEMPLATE_CODE)
                .nickname("展厅系统")
                .content("请处理{fieldCode}")
                .params(List.of("fieldCode"))
                .type(1)
                .status(ENABLE.getStatus())
                .remark("assignment notify")
                .build();
        notifyTemplateMapper.insert(template);
    }

    private void seedWorkflowNotifyTemplates() {
        if (notifyTemplateMapper.selectByCode(PENDING_APPROVAL_TEMPLATE_CODE) == null) {
            NotifyTemplateDO pendingTemplate = NotifyTemplateDO.builder()
                    .name("展厅审批待办通知")
                    .code(PENDING_APPROVAL_TEMPLATE_CODE)
                    .nickname("展厅系统")
                    .content("展厅{targetTypeText}【{targetName}】待{approvalStage}，点击查看对应内容。")
                    .params(List.of("targetTypeText", "targetName", "approvalStage"))
                    .type(2)
                    .status(ENABLE.getStatus())
                    .remark("workflow pending notify")
                    .build();
            notifyTemplateMapper.insert(pendingTemplate);
        }

        if (notifyTemplateMapper.selectByCode(PUBLISHED_APPROVAL_TEMPLATE_CODE) == null) {
            NotifyTemplateDO publishedTemplate = NotifyTemplateDO.builder()
                    .name("展厅发布完成通知")
                    .code(PUBLISHED_APPROVAL_TEMPLATE_CODE)
                    .nickname("展厅系统")
                    .content("展厅{targetTypeText}【{targetName}】已审批通过并发布，点击查看对应内容。")
                    .params(List.of("targetTypeText", "targetName"))
                    .type(2)
                    .status(ENABLE.getStatus())
                    .remark("workflow published notify")
                    .build();
            notifyTemplateMapper.insert(publishedTemplate);
        }

        if (notifyTemplateMapper.selectByCode(REJECTED_APPROVAL_TEMPLATE_CODE) == null) {
            NotifyTemplateDO rejectedTemplate = NotifyTemplateDO.builder()
                    .name("展厅审批驳回通知")
                    .code(REJECTED_APPROVAL_TEMPLATE_CODE)
                    .nickname("展厅系统")
                    .content("展厅{targetTypeText}【{targetName}】在{approvalStage}被驳回，原因：{rejectionReason}。点击后可继续修改原提交内容。")
                    .params(List.of("targetTypeText", "targetName", "approvalStage", "rejectionReason"))
                    .type(2)
                    .status(ENABLE.getStatus())
                    .remark("workflow rejected notify")
                    .build();
            notifyTemplateMapper.insert(rejectedTemplate);
        }
    }

    private void seedAssignmentActors(boolean assignEditorRole) {
        deptMapper.deleteById(10L);
        adminUserMapper.deleteById(200L);
        adminUserMapper.deleteById(700L);
        roleMapper.deleteById(30L);
        userRoleMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getUserId, 700L));

        var leader = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
        leader.setId(200L);
        leader.setUsername("leader");
        leader.setPassword("pwd");
        leader.setNickname("部门负责人");
        leader.setDeptId(10L);
        leader.setStatus(ENABLE.getStatus());
        leader.setCreateTime(LocalDateTime.now());
        adminUserMapper.insert(leader);

        var assignee = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
        assignee.setId(700L);
        assignee.setUsername("editor");
        assignee.setPassword("pwd");
        assignee.setNickname("编辑");
        assignee.setDeptId(10L);
        assignee.setStatus(ENABLE.getStatus());
        assignee.setCreateTime(LocalDateTime.now());
        adminUserMapper.insert(assignee);

        var dept = new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO();
        dept.setId(10L);
        dept.setName("展厅部");
        dept.setParentId(0L);
        dept.setSort(1);
        dept.setLeaderUserId(200L);
        dept.setStatus(ENABLE.getStatus());
        dept.setCreateTime(LocalDateTime.now());
        deptMapper.insert(dept);

        var role = new cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO();
        role.setId(30L);
        role.setName("编辑角色");
        role.setCode("EDITOR");
        role.setSort(1);
        role.setStatus(ENABLE.getStatus());
        role.setType(2);
        role.setDataScope(1);
        role.setCreateTime(LocalDateTime.now());
        role.setTenantId(TenantContextHolder.getRequiredTenantId());
        roleMapper.insert(role);

        if (assignEditorRole) {
            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(700L);
            userRole.setRoleId(30L);
            userRoleMapper.insert(userRole);
        }
    }

    private void seedPublicityApproverRole(Long userId) {
        seedWorkflowNotifyTemplates();
        var user = adminUserMapper.selectById(userId);
        if (user == null) {
            user = new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();
            user.setId(userId);
            user.setUsername("publicity-" + userId);
            user.setPassword("pwd");
            user.setNickname("企宣审批人");
            user.setDeptId(20L);
            user.setStatus(ENABLE.getStatus());
            user.setCreateTime(LocalDateTime.now());
            adminUserMapper.insert(user);
        }

        if (deptMapper.selectById(20L) == null) {
            var publicityDept = new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO();
            publicityDept.setId(20L);
            publicityDept.setName("企宣部");
            publicityDept.setParentId(0L);
            publicityDept.setSort(2);
            publicityDept.setLeaderUserId(userId);
            publicityDept.setStatus(ENABLE.getStatus());
            publicityDept.setCreateTime(LocalDateTime.now());
            deptMapper.insert(publicityDept);
        }

        var role = roleMapper.selectByCode("showroom_publicity");
        if (role == null) {
            role = new cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO();
            role.setId(31L);
            role.setName("企宣角色");
            role.setCode("showroom_publicity");
            role.setSort(2);
            role.setStatus(ENABLE.getStatus());
            role.setType(2);
            role.setDataScope(1);
            role.setCreateTime(LocalDateTime.now());
            role.setTenantId(TenantContextHolder.getRequiredTenantId());
            roleMapper.insert(role);
        } else if (!TenantContextHolder.getRequiredTenantId().equals(role.getTenantId())) {
            role.setTenantId(TenantContextHolder.getRequiredTenantId());
            roleMapper.updateById(role);
        }

        Long publicityRoleId = role.getId();
        boolean bound = userRoleMapper.selectListByUserId(userId).stream()
                .anyMatch(userRole -> publicityRoleId.equals(userRole.getRoleId()));
        if (!bound) {
            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(userId);
            userRole.setRoleId(publicityRoleId);
            userRoleMapper.insert(userRole);
        }
    }

    private NotifyMessageDO latestNotifyMessageForUser(Long userId, String templateCode) {
        return notifyMessageMapper.selectList().stream()
                .filter(message -> userId.equals(message.getUserId()))
                .filter(message -> templateCode.equals(message.getTemplateCode()))
                .max(Comparator.comparing(NotifyMessageDO::getId))
                .orElseThrow();
    }

    private Object getWebsiteConfigPayload() {
        try {
            ensureMinimalReleaseSupport();
            publishCurrentRelease();
            var result = legacyWebsiteConfigProjector.projectCurrentPayload(defaultReleaseScope());
            return result.getCheckedData();
        } catch (Exception exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(exception);
        }
    }

    private void publishCurrentRelease() {
        bindDefaultSiteStage();
        releasePublisherService.publishRelease(900L, Instant.parse("2026-05-23T10:15:00Z"),
                DEFAULT_SITE_KEY, DEFAULT_STAGE);
    }

    private void saveHallCanvasLayout(Long hallId, Long... productIds) {
        List<ShowroomAdminController.HallProductMappingReqVO> products = new java.util.ArrayList<>();
        java.math.BigDecimal width = java.math.BigDecimal.ONE.divide(
                java.math.BigDecimal.valueOf(productIds.length), 6, java.math.RoundingMode.HALF_UP);
        for (int index = 0; index < productIds.length; index++) {
            products.add(new ShowroomAdminController.HallProductMappingReqVO(
                    productIds[index], index + 1,
                    width.multiply(java.math.BigDecimal.valueOf(index)),
                    java.math.BigDecimal.ZERO,
                    index == productIds.length - 1
                            ? java.math.BigDecimal.ONE.subtract(width.multiply(java.math.BigDecimal.valueOf(index)))
                            : width,
                    java.math.BigDecimal.ONE));
        }
        adminController.updateHallCanvasLayout(new ShowroomAdminController.HallMappingReqVO(hallId, products));
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
        mockFile(98001L, 28L, "showroom/preview/http-minimal-product.png", "image/png");
        mockFile(98002L, 29L, "showroom/narration/http-minimal-product-zh.wav", "audio/wav");
        mockFile(98003L, 29L, "showroom/narration/http-minimal-product-en.wav", "audio/wav");
        mockFile(98004L, 28L, "showroom/preview/http-minimal-hall.png", "image/png");
        mockFile(98005L, 29L, "showroom/narration/http-minimal-hall-zh.wav", "audio/wav");
        mockFile(98006L, 29L, "showroom/narration/http-minimal-hall-en.wav", "audio/wav");

        ShowroomProductRevision productRevision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "YT-HTTPCFG-001", "HTTP 展示占位产品", "HTTP Placeholder Product",
                        Map.of("owner_company_id", "124",
                                "product_owner_type", "YINGTAI",
                                "lifecycle_stage", "REGISTERED",
                                "target_market", "占位市场",
                                "core_selling_points", "占位卖点",
                                "cover_image", "/admin-api/infra/file/28/get/showroom/preview/http-minimal-product.png"))).revisionId(), 901L);
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, productRevision.productId(),
                productRevision.revisionId(), 98001L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, productRevision.productId(), productRevision.revisionId(),
                ShowroomNarrationLanguage.ZH, "占位产品中文讲解", 98002L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.PRODUCT, productRevision.productId(), productRevision.revisionId(),
                ShowroomNarrationLanguage.EN, "Placeholder product English narration", 98003L, "ruoxi");

        ShowroomHall hall = contentService.createHall("HTTP_CFG", "HTTP 展示展厅", "HTTP Display Hall",
                "占位展厅说明", "Placeholder hall overview");
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 98004L);
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "占位展厅说明", 98005L, "ruoxi");
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "Placeholder hall overview", 98006L, "ruoxi");
        contentService.replaceHallCanvasLayout(hall.hallId(),
                List.of(new ShowroomHallProductMapping(productRevision.productId(), 1,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ONE, java.math.BigDecimal.ONE)));
    }

    private static boolean hasDisplayMethod(String methodName, Class<?>... parameterTypes) {
        try {
            ShowroomDisplayController.class.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T recordAccessor(Object target, String accessorName) {
        try {
            return (T) target.getClass().getMethod(accessorName).invoke(target);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static List<?> recordListAccessor(Object target, String accessorName) {
        return recordAccessor(target, accessorName);
    }

    private static Object findBilingualField(Object productRecord, String fieldCode) {
        return recordListAccessor(productRecord, "bilingualPublicFields").stream()
                .filter(field -> fieldCode.equals(recordAccessor(field, "fieldCode")))
                .findFirst()
                .orElseThrow();
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private void mockShowroomRoleChecks() {
        when(securityFrameworkService.hasRole(anyString())).thenAnswer(invocation -> {
            String roleCode = invocation.getArgument(0);
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null) {
                return false;
            }
            return switch (roleCode) {
                case "EDITOR" -> loginUserId == 700L;
                case "DEPARTMENT_SUPERVISOR" -> loginUserId == 200L;
                case "showroom_publicity" -> loginUserId == 300L;
                case "super_admin" -> loginUserId == 1L;
                default -> false;
            };
        });
    }

    private static byte[] buildSilentWavBytes(int durationSeconds) throws IOException {
        AudioFormat format = new AudioFormat(8000F, 16, 1, true, false);
        int frameCount = 8000 * durationSeconds;
        byte[] pcm = new byte[frameCount * format.getFrameSize()];
        try (ByteArrayInputStream input = new ByteArrayInputStream(pcm);
             AudioInputStream audioInputStream = new AudioInputStream(input, format, frameCount);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        }
    }

    private <T> T withLoginUser(Long userId, CheckedSupplier<T> supplier) {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            return supplier.get();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
