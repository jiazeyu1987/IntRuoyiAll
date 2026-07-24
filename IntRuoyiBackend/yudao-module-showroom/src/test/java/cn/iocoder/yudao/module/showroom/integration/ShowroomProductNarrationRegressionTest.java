package cn.iocoder.yudao.module.showroom.integration;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.AiModelFactory;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.ai.service.tts.AliyunNlsTtsSynthesizer;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
import cn.iocoder.yudao.module.showroom.configpackage.ShowroomHallConfigPackageService;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomAliyunNlsAudioGenerationAdapter;
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
import cn.iocoder.yudao.module.showroom.release.ShowroomLegacyWebsiteConfigProjector;
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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        ShowroomAdminController.class,
        ShowroomApiRuntime.class,
        ShowroomPersistentContentService.class,
        ShowroomPersistentNarrationService.class,
        ShowroomAliyunNlsAudioGenerationAdapter.class
})
class ShowroomProductNarrationRegressionTest extends BaseDbUnitTest {

    @Resource
    private ShowroomAdminController adminController;

    @Resource
    private ShowroomPersistentContentService contentService;

    @Resource
    private ShowroomApiRuntime runtime;

    @Resource
    private ShowroomPersistentNarrationService narrationService;

    private final Map<String, ConfigDO> configStore = new HashMap<>();
    private final AtomicLong configIdSequence = new AtomicLong(1L);

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
    private ShowroomImagePromptVersionService imagePromptVersionService;
    @MockBean
    private ShowroomVersionBundleService versionBundleService;

    @MockBean
    private ShowroomHallConfigPackageService hallConfigPackageService;
    @MockBean
    private ShowroomProductCoverBatchTaskService productCoverBatchTaskService;
    @MockBean
    private ShowroomPreviewAssetOperations previewAssetService;
    @MockBean
    private ShowroomCompanyNarrationCodexService narrationCodexService;
    @MockBean
    private ShowroomCompanyNarrationTranslationService narrationTranslationService;
    @MockBean
    private ShowroomProductNarrationCodexService productNarrationCodexService;
    @MockBean
    private ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;
    @MockBean
    private FileMapper fileMapper;
    @MockBean
    private FileService fileService;
    @MockBean
    private ConfigService configService;
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
    @MockBean
    private AliyunNlsTtsSynthesizer aliyunNlsTtsSynthesizer;
    @MockBean
    private ShowroomLegacyWebsiteConfigProjector legacyWebsiteConfigProjector;

    @BeforeEach
    void setUpConfigStore() {
        configStore.clear();
        configIdSequence.set(1L);
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
    }

    @Test
    void productGenerateNarrationScriptShouldPersistZhAndEnDrafts() {
        var liveRevision = publishBaselineProduct();
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        when(productNarrationCodexService.generateScript(contentService.getProduct(liveRevision.productId()), liveRevision))
                .thenReturn("基于产品资料生成的中文讲解稿");
        when(productNarrationCodexService.translateZhToEn("基于产品资料生成的中文讲解稿"))
                .thenReturn("Generated English narration script");

        var generated = withLoginUser(1131L, () -> adminController.generateProductNarrationScript(
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
    void productGenerateNarrationAudioShouldReuseRecordedEnglishNarrationAndSharedDefaultVoice() throws Exception {
        var liveRevision = publishBaselineProduct();
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
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

        var generated = withLoginUser(1131L, () -> adminController.generateProductNarrationAudio(
                new ShowroomAdminController.ProductNarrationGenerateReqVO(liveRevision.productId(), liveRevision.revisionId())))
                .getCheckedData();
        var zhNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var enNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();

        assertEquals(liveRevision.productId(), generated.productId());
        assertEquals(zhDraft.id(), generated.zhNarrationVersionId());
        assertEquals(enDraft.id(), generated.enNarrationVersionId());
        assertEquals("ruoxi", generated.voice());
        assertEquals(99211L, zhNarration.audioFileId());
        assertEquals(99212L, enNarration.audioFileId());
        assertEquals("Recorded English narration script", enNarration.scriptText());
        assertEquals("ruoxi", zhNarration.voice());
        assertEquals("ruoxi", enNarration.voice());
        verify(productNarrationCodexService, never()).translateZhToEn(anyString());
    }

    @Test
    void productRowGenerateNarrationAudioShouldCompleteEnglishDraftForLatestProductDraftBeforePublish() throws Exception {
        var liveRevision = publishBaselineProduct();
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        var latestDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 V2", "Edited Introducer Sheath Set",
                Map.of("owner_company_id", "124",
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "target_market", "冠脉市场 V2",
                        "target_market_en", "Coronary market V2",
                        "registration_certificate", "注册证 V2",
                        "registration_certificate_en", "Certificate V2",
                        "core_selling_points", "草稿中文卖点",
                        "core_selling_points_en", "Edited English selling points",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/preview/draft-product.png")));
        ShowroomNarrationVersion zhDraft = adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), latestDraft.revisionId(), "PUBLIC", "ZH",
                "草稿中文讲解稿", null, null, true)).getCheckedData();
        when(productNarrationCodexService.translateZhToEn("草稿中文讲解稿"))
                .thenReturn("Edited English narration from latest draft");

        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("草稿中文讲解稿", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(2))
                .thenReturn(buildSilentWavBytes(2));
        when(aliyunNlsTtsSynthesizer.synthesize("Edited English narration from latest draft", tts, "ruoxi",
                "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(3))
                .thenReturn(buildSilentWavBytes(3));
        when(fileService.createFileAndReturnId(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.contains("product-" + liveRevision.productId()),
                org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav")))
                .thenReturn(99311L)
                .thenReturn(99312L)
                .thenReturn(99313L)
                .thenReturn(99314L);

        var generated = withLoginUser(1131L, () -> adminController.generateProductNarrationAudio(
                new ShowroomAdminController.ProductNarrationGenerateReqVO(liveRevision.productId(),
                        latestDraft.revisionId())))
                .getCheckedData();
        var latestDraftZh = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var latestDraftEn = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();

        assertEquals(liveRevision.productId(), generated.productId());
        assertEquals(zhDraft.id(), generated.zhNarrationVersionId());
        assertEquals(latestDraft.revisionId(), latestDraftZh.sourceRevisionId());
        assertEquals(latestDraft.revisionId(), latestDraftEn.sourceRevisionId());
        assertEquals(99311L, latestDraftZh.audioFileId());
        assertEquals(99312L, latestDraftEn.audioFileId());
        assertEquals("Edited English narration from latest draft", latestDraftEn.scriptText());
        assertEquals("ruoxi", latestDraftZh.voice());
        assertEquals("ruoxi", latestDraftEn.voice());

        var published = withLoginUser(1131L, () -> adminController.publishProduct(
                new ShowroomAdminController.ProductPublishReqVO(
                        liveRevision.productId(), "YT-GW-001", "导管鞘组 V2", "Edited Introducer Sheath Set",
                        latestDraft.fields(), latestDraft.revisionId(), null, false)))
                .getCheckedData();
        var publishedZh = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var publishedEn = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();

        assertEquals("PUBLISHED", published.status());
        assertEquals(published.revisionId(), publishedZh.sourceRevisionId());
        assertEquals(published.revisionId(), publishedEn.sourceRevisionId());
        assertEquals(99313L, publishedZh.audioFileId());
        assertEquals(99314L, publishedEn.audioFileId());
        assertEquals("草稿中文讲解稿", publishedZh.scriptText());
        assertEquals("Edited English narration from latest draft", publishedEn.scriptText());
    }

    @Test
    void productPublishShouldCarryForwardNarrationAudioWhenLatestDraftScriptIsUnchanged() {
        var liveRevision = publishBaselineProduct();
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        publishNarrationWithAudio(liveRevision, ShowroomNarrationLanguage.ZH,
                "不变中文讲解稿", 99321L, "ruoxi");
        publishNarrationWithAudio(liveRevision, ShowroomNarrationLanguage.EN,
                "Unchanged English narration", 99322L, "ruoxi");

        var latestDraft = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 V2", "Edited Introducer Sheath Set",
                Map.of("owner_company_id", "124",
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "target_market", "冠脉市场 V2",
                        "target_market_en", "Coronary market V2",
                        "registration_certificate", "注册证 V2",
                        "registration_certificate_en", "Certificate V2",
                        "core_selling_points", "只改产品公开字段",
                        "core_selling_points_en", "Only product public fields changed",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/preview/draft-product.png")));
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), latestDraft.revisionId(), "PUBLIC", "ZH",
                "不变中文讲解稿", 99321L, 60, false)).getCheckedData();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), latestDraft.revisionId(), "PUBLIC", "EN",
                "Unchanged English narration", 99322L, 60, false)).getCheckedData();

        var published = withLoginUser(1131L, () -> adminController.publishProduct(
                new ShowroomAdminController.ProductPublishReqVO(
                        liveRevision.productId(), "YT-GW-001", "导管鞘组 V2", "Edited Introducer Sheath Set",
                        latestDraft.fields(), latestDraft.revisionId(), null, false)))
                .getCheckedData();
        var publishedZh = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var publishedEn = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();

        assertEquals("PUBLISHED", published.status());
        assertEquals(published.revisionId(), publishedZh.sourceRevisionId());
        assertEquals(published.revisionId(), publishedEn.sourceRevisionId());
        assertEquals(99321L, publishedZh.audioFileId());
        assertEquals(99322L, publishedEn.audioFileId());
        assertEquals("不变中文讲解稿", publishedZh.scriptText());
        assertEquals("Unchanged English narration", publishedEn.scriptText());
        verify(aliyunNlsTtsSynthesizer, never()).synthesize(anyString(),
                org.mockito.ArgumentMatchers.any(YudaoAiProperties.Tts.class),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void productPublishShouldCarryForwardPublishedNarrationAudioWhenOnlyAttachmentsChange() {
        var liveRevision = publishBaselineProduct();
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        publishNarrationWithAudio(liveRevision, ShowroomNarrationLanguage.ZH,
                "已发布中文讲解稿", 99331L, "ruoxi");
        publishNarrationWithAudio(liveRevision, ShowroomNarrationLanguage.EN,
                "Published English narration", 99332L, "ruoxi");
        mockFile(99333L, "showroom/product-attachments/manual.pdf", "application/pdf", 1024L);
        Map<String, String> publishFields = new LinkedHashMap<>(liveRevision.fields());
        publishFields.put("owner_company_id", "124");
        publishFields.put("product_owner_type", "YINGTAI");
        publishFields.put("lifecycle_stage", "REGISTERED");

        var published = withLoginUser(1131L, () -> adminController.publishProduct(
                new ShowroomAdminController.ProductPublishReqVO(
                        liveRevision.productId(), "YT-GW-001", "导管鞘组 V1", "Introducer Sheath Set",
                        publishFields, liveRevision.revisionId(), null, false,
                        List.of(new ShowroomAdminController.ProductAttachmentReqVO(
                                "text", 99333L, "manual.pdf", "application/pdf", 1024L, 0)))))
                .getCheckedData();
        var publishedZh = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var publishedEn = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();

        assertEquals("PUBLISHED", published.status());
        assertEquals(published.revisionId(), publishedZh.sourceRevisionId());
        assertEquals(published.revisionId(), publishedEn.sourceRevisionId());
        assertEquals(99331L, publishedZh.audioFileId());
        assertEquals(99332L, publishedEn.audioFileId());
        assertEquals("已发布中文讲解稿", publishedZh.scriptText());
        assertEquals("Published English narration", publishedEn.scriptText());
        assertEquals(1, published.attachments().size());
        verify(aliyunNlsTtsSynthesizer, never()).synthesize(anyString(),
                org.mockito.ArgumentMatchers.any(YudaoAiProperties.Tts.class),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void productGenerateNarrationAudioShouldFailWhenEnglishNarrationMissing() {
        var liveRevision = publishBaselineProduct();
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "只有中文讲解稿", null, null, false)).getCheckedData();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> withLoginUser(1131L, () -> adminController.generateProductNarrationAudio(
                        new ShowroomAdminController.ProductNarrationGenerateReqVO(liveRevision.productId(), liveRevision.revisionId())))
                        .getCheckedData());

        assertTrue(exception.getMessage().contains("SHOWROOM_TRANSLATION_FAILED"));
    }

    @Test
    void getProductNarrationShouldReturnBusinessErrorWhenNarrationMissing() {
        var liveRevision = publishBaselineProduct();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                        .getCheckedData());

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("SHOWROOM_TARGET_NOT_FOUND: narration not found"));
    }

    @Test
    void batchGenerateNarrationAudioShouldSkipExistingAudioAndMissingScriptsThenStopAutoCheck() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        var existingRevision = publishProduct("BATCH-SKIP-EXISTING", "已有双语音频产品");
        var missingScriptRevision = publishProduct("BATCH-SKIP-SCRIPT", "缺讲解稿产品");

        publishNarrationWithAudio(existingRevision, ShowroomNarrationLanguage.ZH, "已有中文讲解", 88101L, "ruoxi");
        publishNarrationWithAudio(existingRevision, ShowroomNarrationLanguage.EN, "Existing English narration", 88102L, "ruoxi");
        narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, missingScriptRevision.productId(), missingScriptRevision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH, "只有中文讲解稿", false));

        var summary = withLoginUser(300L, () -> adminController.batchGenerateProductNarrationAudio(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-SKIP", null, null, null)))
                .getCheckedData();
        var state = withLoginUser(300L, () -> adminController.getProductBatchGenerateNarrationAudioState())
                .getCheckedData();

        assertEquals(2, summary.matchedCount());
        assertEquals(2, summary.publishedCount());
        assertEquals(0, summary.skippedUnpublishedCount());
        assertEquals(1, summary.skippedExistingCount());
        assertEquals(1, summary.skippedMissingScriptCount());
        assertEquals(0, summary.succeededCount());
        assertEquals(0, summary.failedCount());
        assertFalse(summary.autoCheckEnabled());
        assertEquals(0, summary.remainingActionableCount());
        assertTrue(summary.failures().isEmpty());
        assertFalse(state.enabled());
        assertEquals(1, state.skippedExistingCount());
        assertEquals(1, state.skippedMissingScriptCount());
        assertEquals(0, state.remainingActionableCount());
    }

    @Test
    void batchGenerateNarrationAudioShouldKeepAutoCheckEnabledAfterFailureAndResumeOnSchedulerRetry() throws Exception {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        var liveRevision = publishProduct("BATCH-RETRY-001", "定时续跑产品");
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "ZH",
                "定时续跑中文讲解", null, null, false)).getCheckedData();
        adminController.saveNarrationDraft(new ShowroomAdminController.NarrationDraftReqVO(
                "PRODUCT", liveRevision.productId(), liveRevision.revisionId(), "PUBLIC", "EN",
                "Scheduled retry english narration", null, null, false)).getCheckedData();

        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize("定时续跑中文讲解", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenThrow(new IllegalStateException("SHOWROOM_AUDIO_GENERATION_FAILED: first run failed"))
                .thenReturn(buildSilentWavBytes(2));
        when(aliyunNlsTtsSynthesizer.synthesize("Scheduled retry english narration", tts, "ruoxi",
                "saved-token", "saved-appkey"))
                .thenReturn(buildSilentWavBytes(3));
        when(fileService.createFileAndReturnId(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.contains("product-" + liveRevision.productId()),
                org.mockito.ArgumentMatchers.eq("showroom/narration"),
                org.mockito.ArgumentMatchers.eq("audio/wav")))
                .thenReturn(99221L)
                .thenReturn(99222L);

        var firstSummary = withLoginUser(300L, () -> adminController.batchGenerateProductNarrationAudio(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-RETRY", null, null, null)))
                .getCheckedData();
        var firstState = withLoginUser(300L, () -> adminController.getProductBatchGenerateNarrationAudioState())
                .getCheckedData();

        assertEquals(1, firstSummary.matchedCount());
        assertEquals(1, firstSummary.failedCount());
        assertTrue(firstSummary.autoCheckEnabled());
        assertEquals(1, firstSummary.remainingActionableCount());
        assertTrue(firstSummary.failures().get(0).reason().contains("first run failed"));
        assertTrue(firstState.enabled());
        assertEquals(1, firstState.failedCount());
        assertEquals(1, firstState.remainingActionableCount());
        assertTrue(firstState.lastFailureMessage().contains("first run failed"));

        runtime.runScheduledProductBatchNarrationAudioAutoCheck();

        var retryState = withLoginUser(300L, () -> adminController.getProductBatchGenerateNarrationAudioState())
                .getCheckedData();
        var zhLive = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH)).orElseThrow();
        var enLive = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, liveRevision.productId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN)).orElseThrow();

        assertFalse(retryState.enabled());
        assertEquals(1, retryState.succeededCount());
        assertEquals(0, retryState.failedCount());
        assertEquals(0, retryState.remainingActionableCount());
        assertEquals(99221L, zhLive.audioFileId());
        assertEquals(99222L, enLive.audioFileId());
    }

    @Test
    void startBatchGenerateNarrationScriptShouldSkipCompletedAndFillMissingLanguagesThenStopTask() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        var completedRevision = publishProduct("BATCH-SCRIPT-COMPLETE", "已有双语讲解产品");
        var zhOnlyRevision = publishProduct("BATCH-SCRIPT-ZH", "只有中文讲解产品");
        var enOnlyRevision = publishProduct("BATCH-SCRIPT-EN", "只有英文讲解产品");

        narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, completedRevision.productId(), completedRevision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH, "现成中文讲解稿", false));
        narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, completedRevision.productId(), completedRevision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN, "Existing English narration", false));
        narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, zhOnlyRevision.productId(), zhOnlyRevision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH, "已有中文讲解稿", false));
        narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, enOnlyRevision.productId(), enOnlyRevision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN, "Existing EN only script", false));

        when(productNarrationCodexService.translateZhToEn("已有中文讲解稿"))
                .thenReturn("Generated English from existing zh script");
        when(productNarrationCodexService.generateScript(contentService.getProduct(enOnlyRevision.productId()), enOnlyRevision))
                .thenReturn("补齐中文讲解稿");

        var started = withLoginUser(300L, () -> adminController.startBatchGenerateNarrationScript(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-SCRIPT-", null, null, null)))
                .getCheckedData();

        runtime.runScheduledProductBatchNarrationScriptAutoCheck();

        var state = waitForNarrationScriptTaskState(task -> !task.active() && task.completedAt() != null);
        var zhOnlyEn = adminController.getNarration("PRODUCT", zhOnlyRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();
        var enOnlyZh = adminController.getNarration("PRODUCT", enOnlyRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var completedZh = adminController.getNarration("PRODUCT", completedRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var completedEn = adminController.getNarration("PRODUCT", completedRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();

        assertTrue(started.active());
        assertFalse(started.running());
        assertEquals("BATCH-SCRIPT-", started.keyword());

        assertFalse(state.active());
        assertFalse(state.running());
        assertEquals(3, state.matchedCount());
        assertEquals(1, state.skippedCompletedCount());
        assertEquals(2, state.generatedLanguageCount());
        assertEquals(0, state.failedCount());
        assertEquals(0, state.remainingCount());
        assertNotNull(state.startedAt());
        assertNotNull(state.lastRunAt());
        assertNotNull(state.completedAt());
        assertNull(state.lastFailure());

        assertEquals("Generated English from existing zh script", zhOnlyEn.scriptText());
        assertEquals("补齐中文讲解稿", enOnlyZh.scriptText());
        assertEquals("现成中文讲解稿", completedZh.scriptText());
        assertEquals("Existing English narration", completedEn.scriptText());
        verify(productNarrationCodexService).translateZhToEn("已有中文讲解稿");
        verify(productNarrationCodexService).generateScript(contentService.getProduct(enOnlyRevision.productId()), enOnlyRevision);
    }

    @Test
    void getProductBatchGenerateNarrationScriptStatusShouldExposeCurrentProductWhileRunningThenClearAfterCompletion() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        var liveRevision = publishProduct("BATCH-SCRIPT-CURRENT", "当前执行产品");
        AtomicReference<ShowroomAdminController.ProductNarrationScriptBatchTaskRespVO> runningStateRef =
                new AtomicReference<>();
        when(productNarrationCodexService.generateScript(contentService.getProduct(liveRevision.productId()), liveRevision))
                .thenAnswer(invocation -> {
                    var runningState = withLoginUser(300L,
                            () -> adminController.getProductBatchGenerateNarrationScriptStatus())
                            .getCheckedData();
                    runningStateRef.set(runningState);
                    return "运行中中文讲解稿";
                });
        when(productNarrationCodexService.translateZhToEn("运行中中文讲解稿"))
                .thenReturn("Running english narration script");

        withLoginUser(300L, () -> adminController.startBatchGenerateNarrationScript(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-SCRIPT-CURRENT", null, null, null)))
                .getCheckedData();
        runtime.runScheduledProductBatchNarrationScriptAutoCheck();

        var completedState = waitForNarrationScriptTaskState(task -> !task.active() && task.completedAt() != null);
        var runningState = runningStateRef.get();

        assertNotNull(runningState);
        assertTrue(runningState.running());
        assertNotNull(runningState.currentProduct());
        assertEquals(liveRevision.productId(), runningState.currentProduct().productId());
        assertEquals("BATCH-SCRIPT-CURRENT", runningState.currentProduct().productCode());
        assertEquals("当前执行产品", runningState.currentProduct().nameCn());
        assertNull(completedState.currentProduct());
    }

    @Test
    void startBatchGenerateNarrationScriptShouldResumeAfterRuntimeRestartUntilAllScriptsExist() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        var liveRevision = publishProduct("BATCH-SCRIPT-RETRY", "讲解定时续跑产品");
        when(productNarrationCodexService.generateScript(contentService.getProduct(liveRevision.productId()), liveRevision))
                .thenThrow(new IllegalStateException("SHOWROOM_SCRIPT_GENERATION_FAILED: first script run failed"))
                .thenReturn("定时续跑中文讲解稿");
        when(productNarrationCodexService.translateZhToEn("定时续跑中文讲解稿"))
                .thenReturn("Scheduled retry english narration script");

        withLoginUser(300L, () -> adminController.startBatchGenerateNarrationScript(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-SCRIPT-RETRY", null, null, null)))
                .getCheckedData();
        runtime.runScheduledProductBatchNarrationScriptAutoCheck();

        var failedState = waitForNarrationScriptTaskState(task -> task.active() && task.lastFailure() != null);

        assertTrue(failedState.active());
        assertFalse(failedState.running());
        assertEquals(1, failedState.failedCount());
        assertEquals(1, failedState.remainingCount());
        assertNotNull(failedState.lastFailure());
        assertTrue(failedState.lastFailure().reason().contains("first script run failed"));

        ShowroomApiRuntime restartedRuntime = new ShowroomApiRuntime(
                contentService,
                commentService,
                productCoverBatchTaskService,
                productCoverImageService,
                imagePromptVersionService,
                narrationService,
                narrationCodexService,
                narrationTranslationService,
                productNarrationCodexService,
                org.mockito.Mockito.mock(ShowroomPreviewAssetOperations.class),
                previewAssetVersionMapper,
                fileMapper,
                configService,
                aliyunNlsCredentialService,
                yudaoAiProperties,
                productRevisionRelationMapper,
                changeRequestMapper,
                assignmentService,
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService.class)
        );
        restartedRuntime.runScheduledProductBatchNarrationScriptAutoCheck();

        var recoveredState = waitForNarrationScriptTaskState(task -> !task.active() && task.completedAt() != null);
        var zhNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "ZH")
                .getCheckedData();
        var enNarration = adminController.getNarration("PRODUCT", liveRevision.productId(), "PUBLIC", "EN")
                .getCheckedData();

        assertFalse(recoveredState.active());
        assertFalse(recoveredState.running());
        assertEquals(2, recoveredState.generatedLanguageCount());
        assertEquals(0, recoveredState.failedCount());
        assertEquals(0, recoveredState.remainingCount());
        assertEquals("定时续跑中文讲解稿", zhNarration.scriptText());
        assertEquals("Scheduled retry english narration script", enNarration.scriptText());
    }

    @Test
    void startBatchGenerateNarrationScriptShouldReturnExistingActiveTaskWithoutReplacingFilters() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        var firstRevision = publishProduct("SCRIPT-SCOPE-A-001", "脚本范围A");
        publishProduct("SCRIPT-SCOPE-B-001", "脚本范围B");
        when(productNarrationCodexService.generateScript(contentService.getProduct(firstRevision.productId()), firstRevision))
                .thenThrow(new IllegalStateException("SHOWROOM_SCRIPT_GENERATION_FAILED: scope keep failure"));

        withLoginUser(300L, () -> adminController.startBatchGenerateNarrationScript(
                new ShowroomAdminController.ProductBatchGenerateReqVO("SCRIPT-SCOPE-A", null, null, null)))
                .getCheckedData();
        runtime.runScheduledProductBatchNarrationScriptAutoCheck();

        var activeState = waitForNarrationScriptTaskState(task -> task.active() && task.lastFailure() != null);
        var reusedState = withLoginUser(300L, () -> adminController.startBatchGenerateNarrationScript(
                new ShowroomAdminController.ProductBatchGenerateReqVO("SCRIPT-SCOPE-B", null, null, null)))
                .getCheckedData();

        assertTrue(activeState.active());
        assertEquals("SCRIPT-SCOPE-A", activeState.keyword());
        assertEquals(1, activeState.remainingCount());
        assertNotNull(activeState.lastFailure());
        assertTrue(activeState.lastFailure().reason().contains("scope keep failure"));

        assertTrue(reusedState.active());
        assertEquals("SCRIPT-SCOPE-A", reusedState.keyword());
        assertEquals(1, reusedState.matchedCount());
        assertEquals(1, reusedState.remainingCount());
    }

    @Test
    void batchGenerateProductSalesCountriesShouldSkipCompletedAndFillMissingLanguages() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        var completedRevision = publishProductWithFields("BATCH-COUNTRY-COMPLETE", "国家已齐全产品",
                Map.of("target_market", "中国", "registration_certificate", "注册证",
                        "target_market_en", "China",
                        "core_selling_points", "已齐全卖点"));
        var zhOnlyRevision = publishProduct("BATCH-COUNTRY-ZH", "只有中文国家产品");
        var missingBothRevision = publishProductWithFields("BATCH-COUNTRY-MISS", "缺双语国家产品",
                Map.of("registration_certificate", "注册证"));

        when(productNarrationCodexService.translateZhToEn("只有中文国家产品在售国家"))
                .thenReturn("China from existing zh");
        when(productNarrationCodexService.generateSalesCountries(
                contentService.getProduct(missingBothRevision.productId()), missingBothRevision))
                .thenReturn("中国;欧盟");
        when(productNarrationCodexService.translateZhToEn("中国;欧盟"))
                .thenReturn("China; EU");

        var summary = withLoginUser(300L, () -> adminController.batchGenerateProductSalesCountries(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-COUNTRY-", null, null, null)))
                .getCheckedData();

        var completedLatestRevision = contentService.getLatestProductRevision(completedRevision.productId());
        var zhOnlyLatestRevision = contentService.getLatestProductRevision(zhOnlyRevision.productId());
        var missingBothLatestRevision = contentService.getLatestProductRevision(missingBothRevision.productId());

        assertEquals(3, summary.matchedCount());
        assertEquals(1, summary.skippedCompletedCount());
        assertEquals(2, summary.updatedProductCount());
        assertEquals(3, summary.generatedLanguageCount());
        assertEquals(0, summary.failedCount());
        assertTrue(summary.failures().isEmpty());

        assertEquals(completedRevision.revisionId(), completedLatestRevision.revisionId());
        assertEquals("China from existing zh",
                zhOnlyLatestRevision.fields().get("target_market_en"));
        assertEquals("中国;欧盟", missingBothLatestRevision.fields().get("target_market"));
        assertEquals("China; EU",
                missingBothLatestRevision.fields().get("target_market_en"));
    }

    @Test
    void batchGenerateProductSalesCountriesShouldExposeFailuresWithoutFallbackContent() {
        when(securityFrameworkService.hasRole(anyString())).thenReturn(true);
        var failedRevision = publishProductWithFields("BATCH-COUNTRY-FAIL", "国家失败产品",
                Map.of("registration_certificate", "注册证"));

        when(productNarrationCodexService.generateSalesCountries(
                contentService.getProduct(failedRevision.productId()), failedRevision))
                .thenThrow(new IllegalStateException("SHOWROOM_SALES_COUNTRIES_GENERATION_FAILED: codex timeout"));

        var summary = withLoginUser(300L, () -> adminController.batchGenerateProductSalesCountries(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-COUNTRY-FAIL", null, null, null)))
                .getCheckedData();

        var latestRevision = contentService.getLatestProductRevision(failedRevision.productId());

        assertEquals(1, summary.matchedCount());
        assertEquals(0, summary.skippedCompletedCount());
        assertEquals(0, summary.updatedProductCount());
        assertEquals(0, summary.generatedLanguageCount());
        assertEquals(1, summary.failedCount());
        assertEquals(1, summary.failures().size());
        assertTrue(summary.failures().get(0).reason().contains("codex timeout"));
        assertEquals(failedRevision.revisionId(), latestRevision.revisionId());
        assertNull(latestRevision.fields().get("target_market"));
        assertNull(latestRevision.fields().get("target_market_en"));
    }

    private ShowroomProductRevision publishBaselineProduct() {
        var baseline = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "YT-GW-001", "导管鞘组 V1", "Introducer Sheath Set",
                Map.of("target_market", "旧国家", "registration_certificate", "注册证 V1",
                        "core_selling_points", "旧卖点")));
        return contentService.publishProductRevision(baseline.revisionId(), 901L);
    }

    private ShowroomProductRevision publishProduct(String productCode, String nameCn) {
        var draft = contentService.saveProductDraft(new ShowroomProductDraft(
                null, productCode, nameCn, productCode + "-EN",
                Map.of("target_market", nameCn + "在售国家", "registration_certificate", "注册证",
                        "core_selling_points", nameCn + "卖点")));
        return contentService.publishProductRevision(draft.revisionId(), 901L);
    }

    private ShowroomProductRevision publishProductWithFields(String productCode, String nameCn,
                                                             Map<String, String> fields) {
        LinkedHashMap<String, String> revisionFields = new LinkedHashMap<>(fields);
        var draft = contentService.saveProductDraft(new ShowroomProductDraft(
                null, productCode, nameCn, productCode + "-EN", revisionFields));
        return contentService.publishProductRevision(draft.revisionId(), 901L);
    }

    private void publishNarrationWithAudio(ShowroomProductRevision revision, ShowroomNarrationLanguage language,
                                           String scriptText, Long audioFileId, String voice) {
        mockFile(audioFileId, "showroom/narration/product-" + revision.productId() + "-"
                + language.name().toLowerCase() + ".wav", "audio/wav", 2048L);
        ShowroomNarrationVersion draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, revision.productId(), revision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, language, scriptText, false));
        draft = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                draft.id(), audioFileId, 60, voice));
        narrationService.publishDirectly(draft.id());
    }

    private void mockFile(Long fileId, String path, String mimeType, Long size) {
        when(fileMapper.selectById(fileId)).thenReturn(FileDO.builder()
                .id(fileId)
                .configId(28L)
                .name(path.substring(path.lastIndexOf('/') + 1))
                .path(path)
                .type(mimeType)
                .size(size)
                .build());
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

    private ShowroomAdminController.ProductNarrationScriptBatchTaskRespVO waitForNarrationScriptTaskState(
            Predicate<ShowroomAdminController.ProductNarrationScriptBatchTaskRespVO> predicate) {
        ShowroomAdminController.ProductNarrationScriptBatchTaskRespVO latest = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            latest = withLoginUser(300L, () -> adminController.getProductBatchGenerateNarrationScriptStatus())
                    .getCheckedData();
            if (predicate.test(latest)) {
                return latest;
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(exception);
            }
        }
        throw new AssertionError("narration script batch task did not reach expected state: " + latest);
    }

    private <T> T withLoginUser(Long userId, CheckedSupplier<T> supplier) {
        try (MockedStatic<SecurityFrameworkUtils> security = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
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
