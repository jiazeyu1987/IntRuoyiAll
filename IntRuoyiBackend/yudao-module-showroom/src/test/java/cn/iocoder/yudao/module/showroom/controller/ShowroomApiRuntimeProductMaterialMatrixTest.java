package cn.iocoder.yudao.module.showroom.controller;

import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachment;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomApiRuntimeProductMaterialMatrixTest {

    @Test
    void publishProductFieldsShouldNotRequireCoverOrNarrationAudioAndShouldReturnMaterialBlockers() {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        ShowroomProductCommentService commentService = mock(ShowroomProductCommentService.class);
        ShowroomProductCoverBatchTaskService batchTaskService = mock(ShowroomProductCoverBatchTaskService.class);
        ShowroomProductCoverImageService coverImageService = mock(ShowroomProductCoverImageService.class);
        ShowroomImagePromptVersionService promptVersionService = mock(ShowroomImagePromptVersionService.class);
        ShowroomPersistentNarrationService narrationService = mock(ShowroomPersistentNarrationService.class);
        ShowroomCompanyNarrationCodexService companyNarrationCodexService = mock(ShowroomCompanyNarrationCodexService.class);
        ShowroomCompanyNarrationTranslationService companyTranslationService =
                mock(ShowroomCompanyNarrationTranslationService.class);
        ShowroomProductNarrationCodexService productNarrationCodexService = mock(ShowroomProductNarrationCodexService.class);
        ShowroomPreviewAssetOperations previewAssetService = mock(ShowroomPreviewAssetOperations.class);
        ShowroomPreviewAssetVersionMapper previewAssetVersionMapper = mock(ShowroomPreviewAssetVersionMapper.class);
        FileMapper fileMapper = mock(FileMapper.class);
        ConfigService configService = mock(ConfigService.class);
        AiTtsAliyunNlsCredentialService credentialService = mock(AiTtsAliyunNlsCredentialService.class);
        YudaoAiProperties aiProperties = mock(YudaoAiProperties.class);
        ShowroomProductRevisionRelationMapper relationMapper = mock(ShowroomProductRevisionRelationMapper.class);
        ShowroomChangeRequestMapper changeRequestMapper = mock(ShowroomChangeRequestMapper.class);
        ShowroomAssignmentService assignmentService = mock(ShowroomAssignmentService.class);
        ShowroomVersionBundleService versionBundleService = mock(ShowroomVersionBundleService.class);
        ShowroomApiRuntime runtime = new ShowroomApiRuntime(contentService, commentService, batchTaskService,
                coverImageService, promptVersionService, narrationService, companyNarrationCodexService,
                companyTranslationService, productNarrationCodexService, previewAssetService, previewAssetVersionMapper,
                fileMapper, configService, credentialService, aiProperties, relationMapper, changeRequestMapper,
                assignmentService, versionBundleService);

        Map<String, String> coreFieldsWithoutMaterials = Map.of(
                "owner_company_id", "124",
                "product_owner_type", "YINGTAI",
                "lifecycle_stage", "REGISTERED",
                "target_market", "冠脉介入"
        );
        ShowroomProductRevision draft = new ShowroomProductRevision(
                1L, 2001L, 15, "DRAFT", "审批展示探针95177868", "Manifold for Single use-OFF",
                false, coreFieldsWithoutMaterials);
        ShowroomProductRevision published = new ShowroomProductRevision(
                1L, 2001L, 15, "PUBLISHED", "审批展示探针95177868", "Manifold for Single use-OFF",
                false, coreFieldsWithoutMaterials);
        when(contentService.saveProductDraft(any(ShowroomProductDraft.class))).thenReturn(draft);
        when(contentService.publishProductRevision(2001L, 300L)).thenReturn(published);
        when(contentService.getProduct(1L)).thenReturn(new ShowroomProductSnapshot(1L, "product_001",
                Optional.of(2001L), false, true));
        when(commentService.pageByProduct(1L, null, null, null)).thenReturn(List.of());
        when(relationMapper.selectListByProductRevisionId(2001L)).thenReturn(List.of());
        when(changeRequestMapper.selectListByTarget("PRODUCT", 1L)).thenReturn(List.of());
        when(narrationService.live(any())).thenReturn(Optional.empty());

        ShowroomAdminController.ProductDetailRespVO response = runtime.publishProduct(
                new ShowroomAdminController.ProductPublishReqVO(1L, "product_001",
                        "审批展示探针95177868", "Manifold for Single use-OFF", coreFieldsWithoutMaterials, null,
                        null, false),
                300L);

        assertEquals("PUBLISHED", response.status());
        assertTrue(response.materialBlockers().stream()
                .anyMatch(blocker -> "PRODUCT_COVER_MISSING".equals(blocker.backendErrorCode())));
        assertTrue(response.materialBlockers().stream()
                .anyMatch(blocker -> "ZH".equals(blocker.language())
                        && "PRODUCT_NARRATION_AUDIO_MISSING".equals(blocker.backendErrorCode())));
        assertTrue(response.materialBlockers().stream()
                .anyMatch(blocker -> "EN".equals(blocker.language())
                        && "PRODUCT_NARRATION_AUDIO_MISSING".equals(blocker.backendErrorCode())));
        verify(versionBundleService).ensureBundleForPublishedRevision("PRODUCT", 1L, 2001L, 300L, null);
        verify(narrationService, never()).generateAudio(any());
        verify(narrationService, never()).publishDirectly(any());
    }

    @Test
    void productDetailShouldExposeAttachmentFileUrl() {
        RuntimeFixture fixture = runtimeFixture();
        ShowroomProductRevision revision = productRevisionWithAttachment(99101L);
        when(fixture.contentService.getProduct(1L)).thenReturn(new ShowroomProductSnapshot(1L, "ATTACH-001",
                Optional.of(2001L), false, true));
        when(fixture.contentService.getLatestProductRevision(1L)).thenReturn(revision);
        when(fixture.commentService.pageByProduct(1L, null, null, null)).thenReturn(List.of());
        when(fixture.relationMapper.selectListByProductRevisionId(2001L)).thenReturn(List.of());
        when(fixture.changeRequestMapper.selectListByTarget("PRODUCT", 1L)).thenReturn(List.of());
        when(fixture.narrationService.live(any())).thenReturn(Optional.empty());
        when(fixture.fileMapper.selectById(99101L)).thenReturn(FileDO.builder()
                .id(99101L)
                .configId(28L)
                .path("showroom/product-attachments/20260606/manual file.pdf")
                .build());

        ShowroomAdminController.ProductDetailRespVO detail = fixture.runtime.getProductDetail(1L, null, true);

        assertEquals("manual file.pdf", detail.attachments().get(0).originalName());
        assertEquals("/admin-api/infra/file/28/get/showroom/product-attachments/20260606/manual%20file.pdf",
                detail.attachments().get(0).url());
    }

    @Test
    void productDetailShouldFailWhenAttachmentFileIsMissing() {
        RuntimeFixture fixture = runtimeFixture();
        ShowroomProductRevision revision = productRevisionWithAttachment(99102L);
        when(fixture.contentService.getProduct(1L)).thenReturn(new ShowroomProductSnapshot(1L, "ATTACH-001",
                Optional.of(2001L), false, true));
        when(fixture.contentService.getLatestProductRevision(1L)).thenReturn(revision);
        when(fixture.commentService.pageByProduct(1L, null, null, null)).thenReturn(List.of());
        when(fixture.relationMapper.selectListByProductRevisionId(2001L)).thenReturn(List.of());
        when(fixture.changeRequestMapper.selectListByTarget("PRODUCT", 1L)).thenReturn(List.of());
        when(fixture.narrationService.live(any())).thenReturn(Optional.empty());
        when(fixture.fileMapper.selectById(99102L)).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> fixture.runtime.getProductDetail(1L, null, true));

        assertTrue(exception.getMessage().contains("SHOWROOM_TARGET_NOT_FOUND: file not found: 99102"));
    }

    private static ShowroomProductRevision productRevisionWithAttachment(Long fileId) {
        return new ShowroomProductRevision(1L, 2001L, 15, "DRAFT", "附件产品",
                "Attachment Product", false, Map.of("target_market", "冠脉介入"),
                List.of(new ShowroomProductAttachment(9001L, 1L, 2001L, "text", fileId,
                        "manual file.pdf", "application/pdf", 2048L, 1)));
    }

    private static RuntimeFixture runtimeFixture() {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        ShowroomProductCommentService commentService = mock(ShowroomProductCommentService.class);
        ShowroomProductCoverBatchTaskService batchTaskService = mock(ShowroomProductCoverBatchTaskService.class);
        ShowroomProductCoverImageService coverImageService = mock(ShowroomProductCoverImageService.class);
        ShowroomImagePromptVersionService promptVersionService = mock(ShowroomImagePromptVersionService.class);
        ShowroomPersistentNarrationService narrationService = mock(ShowroomPersistentNarrationService.class);
        ShowroomCompanyNarrationCodexService companyNarrationCodexService =
                mock(ShowroomCompanyNarrationCodexService.class);
        ShowroomCompanyNarrationTranslationService companyTranslationService =
                mock(ShowroomCompanyNarrationTranslationService.class);
        ShowroomProductNarrationCodexService productNarrationCodexService =
                mock(ShowroomProductNarrationCodexService.class);
        ShowroomPreviewAssetOperations previewAssetService = mock(ShowroomPreviewAssetOperations.class);
        ShowroomPreviewAssetVersionMapper previewAssetVersionMapper = mock(ShowroomPreviewAssetVersionMapper.class);
        FileMapper fileMapper = mock(FileMapper.class);
        ConfigService configService = mock(ConfigService.class);
        AiTtsAliyunNlsCredentialService credentialService = mock(AiTtsAliyunNlsCredentialService.class);
        YudaoAiProperties aiProperties = mock(YudaoAiProperties.class);
        ShowroomProductRevisionRelationMapper relationMapper = mock(ShowroomProductRevisionRelationMapper.class);
        ShowroomChangeRequestMapper changeRequestMapper = mock(ShowroomChangeRequestMapper.class);
        ShowroomAssignmentService assignmentService = mock(ShowroomAssignmentService.class);
        ShowroomVersionBundleService versionBundleService = mock(ShowroomVersionBundleService.class);
        ShowroomApiRuntime runtime = new ShowroomApiRuntime(contentService, commentService, batchTaskService,
                coverImageService, promptVersionService, narrationService, companyNarrationCodexService,
                companyTranslationService, productNarrationCodexService, previewAssetService, previewAssetVersionMapper,
                fileMapper, configService, credentialService, aiProperties, relationMapper, changeRequestMapper,
                assignmentService, versionBundleService);
        return new RuntimeFixture(contentService, commentService, narrationService, fileMapper, relationMapper,
                changeRequestMapper, runtime);
    }

    private record RuntimeFixture(ShowroomPersistentContentService contentService,
                                  ShowroomProductCommentService commentService,
                                  ShowroomPersistentNarrationService narrationService,
                                  FileMapper fileMapper,
                                  ShowroomProductRevisionRelationMapper relationMapper,
                                  ShowroomChangeRequestMapper changeRequestMapper,
                                  ShowroomApiRuntime runtime) {
    }
}
