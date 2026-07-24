package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPersistentPreviewAssetService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetDraftCommand;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetFiles;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomReleaseAssetController;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomReleaseController;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomScopedReleaseAssetController;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomScopedReleaseController;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetRefMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseDocumentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleasePointerMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseTombstoneMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomPublicSiteBindingMapper;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.when;

@Import({
        ShowroomPersistentContentService.class,
        ShowroomPersistentPreviewAssetService.class,
        ShowroomPersistentNarrationService.class,
        ShowroomReleaseSourceFileReader.class,
        ShowroomReleasePublisherService.class,
        ShowroomReleaseAssembler.class,
        ShowroomReleaseRegistryService.class,
        ShowroomReleaseManifestQueryService.class,
        ShowroomPublicReleaseScopeResolver.class,
        ShowroomLegacyWebsiteConfigProjector.class,
        ShowroomReleasePurgeService.class,
        ShowroomReleaseController.class,
        ShowroomReleaseAssetController.class,
        ShowroomScopedReleaseController.class,
        ShowroomScopedReleaseAssetController.class
})
public abstract class AbstractShowroomReleaseDbTest extends BaseDbUnitTest {

    protected static final Long DEFAULT_TENANT_ID = 1L;
    protected static final String DEFAULT_SITE_KEY = "yingtai-showroom";
    protected static final String DEFAULT_STAGE = "TEST";

    @Resource
    protected ShowroomPersistentContentService contentService;
    @Resource
    protected ShowroomPersistentPreviewAssetService previewAssetService;
    @Resource
    protected ShowroomPersistentNarrationService narrationService;
    @Resource
    protected ShowroomReleasePublisherService publisherService;
    @Resource
    protected ShowroomReleaseManifestQueryService manifestQueryService;
    @Resource
    protected ShowroomReleaseController releaseController;
    @Resource
    protected ShowroomReleaseAssetController assetController;
    @Resource
    protected ShowroomScopedReleaseController scopedReleaseController;
    @Resource
    protected ShowroomScopedReleaseAssetController scopedAssetController;
    @Resource
    protected ShowroomReleaseDocumentMapper releaseDocumentMapper;
    @Resource
    protected ShowroomReleaseAssetMapper releaseAssetMapper;
    @Resource
    protected ShowroomReleaseAssetRefMapper releaseAssetRefMapper;
    @Resource
    protected ShowroomReleaseMapper releaseMapper;
    @Resource
    protected ShowroomReleasePointerMapper releasePointerMapper;
    @Resource
    protected ShowroomReleaseTombstoneMapper tombstoneMapper;
    @Resource
    protected ShowroomPublicSiteBindingMapper siteBindingMapper;

    @MockBean
    protected FileMapper fileMapper;
    @MockBean
    protected FileService fileService;

    @BeforeEach
    void setUpTenantContext() {
        TenantContextHolder.setTenantId(DEFAULT_TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    protected PublishedFixture seedPublishedFixture() throws Exception {
        mockFile(101L, 11L, "showroom/company/开园活动图-压缩版.jpg", "image/png", "company-home");
        mockFile(102L, 11L, "showroom/hall/preview.png", "image/png", "hall-preview");
        mockFile(103L, 11L, "showroom/product/产品封面图.png", "image/png", "product-cover");
        mockFile(104L, 12L, "showroom/audio/company-zh.mp3", "audio/mpeg", "company-audio-zh");
        mockFile(105L, 12L, "showroom/audio/company-en.mp3", "audio/mpeg", "company-audio-en");
        mockFile(106L, 12L, "showroom/audio/product-zh.mp3", "audio/mpeg", "product-audio-zh");
        mockFile(107L, 12L, "showroom/audio/product-en.mp3", "audio/mpeg", "product-audio-en");
        mockFile(108L, 12L, "showroom/audio/hall-zh.mp3", "audio/mpeg", "hall-audio-zh");
        mockFile(109L, 12L, "showroom/audio/hall-en.mp3", "audio/mpeg", "hall-audio-en");

        ShowroomCompanyRevision company = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical",
                        Map.of(
                                "development_history", "发展历程",
                                "development_history_en", "History",
                                "park_introduction", "园区介绍",
                                "park_introduction_en", "Park",
                                "cover_image",
                                "/admin-api/infra/file/11/get/showroom/company/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg"))).revisionId(), 901L);

        ShowroomProductRevision product = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-101", "导丝系统", "Guidewire System",
                        Map.of(
                                "target_market", "冠脉介入",
                                "target_market_en", "Coronary intervention",
                                "pipeline_layout", "心内介入BU",
                                "pipeline_layout_en", "Cardiology BU",
                                "core_selling_points", "中国",
                                "core_selling_points_en", "China",
                                "cover_image",
                                "/admin-api/infra/file/11/get/showroom/product/%E4%BA%A7%E5%93%81%E5%B0%81%E9%9D%A2%E5%9B%BE.png"))).revisionId(), 902L);

        ShowroomHall hall = contentService.createHall("CARDIOLOGY", "心内介入展厅", "Cardiology Hall", "展厅简介", "Hall summary");
        contentService.replaceHallCanvasLayout(hall.hallId(), java.util.List.of(new ShowroomHallProductMapping(
                product.productId(), 1, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE)));

        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, company.companyId(), company.revisionId(), 101L);
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 102L);
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, product.productId(), product.revisionId(), 103L);

        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 104L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 105L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, product.productId(), product.revisionId(),
                ShowroomNarrationLanguage.ZH, "产品中文讲解", 106L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, product.productId(), product.revisionId(),
                ShowroomNarrationLanguage.EN, "English product narration", 107L);
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "展厅简介", 108L);
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "Hall summary", 109L);

        return new PublishedFixture(company.companyId(), company.revisionId(), hall.hallId(), product.productId(),
                product.revisionId(), Instant.parse("2026-05-23T10:15:00Z"));
    }

    protected void mockFile(Long fileId, Long configId, String path, String type, String contentText) throws Exception {
        FileDO file = FileDO.builder()
                .id(fileId)
                .configId(configId)
                .name(path.substring(path.lastIndexOf('/') + 1))
                .path(path)
                .url("http://127.0.0.1:9000/yudao/" + path)
                .type(type)
                .size((long) contentText.getBytes(StandardCharsets.UTF_8).length)
                .build();
        when(fileMapper.selectById(fileId)).thenReturn(file);
        when(fileService.getFile(fileId)).thenReturn(file);
        when(fileService.getFileContent(configId, path)).thenReturn(contentText.getBytes(StandardCharsets.UTF_8));
    }

    protected void publishPreviewAsset(ShowroomPreviewAssetTargetType targetType, Long targetId,
                                       Long sourceRevisionId, Long fileId) {
        var draft = previewAssetService.bindStaticPreviewAssets(new ShowroomPreviewAssetDraftCommand(
                targetType, targetId, sourceRevisionId,
                new ShowroomPreviewAssetFiles(fileId, fileId, fileId)));
        previewAssetService.publish(previewAssetService.gaoxinApprove(
                previewAssetService.supervisorApprove(previewAssetService.submit(draft.id()).id(), 200L).id(), 300L).id());
    }

    protected void publishNarration(ShowroomNarrationTargetType targetType, Long targetId, Long sourceRevisionId,
                                    ShowroomNarrationLanguage language, String scriptText, Long audioFileId) {
        var draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                targetType, targetId, sourceRevisionId, ShowroomNarrationAudienceType.PUBLIC, language, scriptText,
                false));
        draft = narrationService.attachAudio(new cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudioDraftCommand(
                draft.id(), audioFileId, 60, "ruoxi"));
        narrationService.publishDirectly(draft.id());
    }

    protected record PublishedFixture(Long companyId, Long companyRevisionId, Long hallId, Long productId,
                                      Long productRevisionId, Instant publishedAt) {
    }

    protected ShowroomMaterializedRelease publishReleaseFixture() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        bindDefaultSiteStage();
        return publisherService.publishRelease(900L, fixture.publishedAt(), DEFAULT_SITE_KEY, DEFAULT_STAGE);
    }

    protected ShowroomMaterializedRelease publishReleaseFixture(Instant publishedAt) throws Exception {
        bindDefaultSiteStage();
        return publisherService.publishRelease(900L, publishedAt, DEFAULT_SITE_KEY, DEFAULT_STAGE);
    }

    protected void bindDefaultSiteStage() {
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

    protected ShowroomReleaseScope defaultReleaseScope() {
        return new ShowroomReleaseScope(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE);
    }
}
