package cn.iocoder.yudao.module.showroom.controller;

import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomApiRuntimeProductCoverPersistenceTest {

    @Test
    void generateProductCoverImageShouldOnlyReturnCoverAndNeverPersistOrPublishRevision() {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        ShowroomProductCommentService commentService = mock(ShowroomProductCommentService.class);
        ShowroomProductCoverBatchTaskService batchTaskService = mock(ShowroomProductCoverBatchTaskService.class);
        ShowroomProductCoverImageService productCoverImageService = mock(ShowroomProductCoverImageService.class);
        ShowroomImagePromptVersionService imagePromptVersionService = mock(ShowroomImagePromptVersionService.class);
        ShowroomPersistentNarrationService narrationService = mock(ShowroomPersistentNarrationService.class);
        ShowroomCompanyNarrationCodexService companyNarrationCodexService = mock(ShowroomCompanyNarrationCodexService.class);
        ShowroomCompanyNarrationTranslationService companyNarrationTranslationService =
                mock(ShowroomCompanyNarrationTranslationService.class);
        ShowroomProductNarrationCodexService productNarrationCodexService = mock(ShowroomProductNarrationCodexService.class);
        ShowroomPreviewAssetOperations previewAssetService = mock(ShowroomPreviewAssetOperations.class);
        ShowroomPreviewAssetVersionMapper previewAssetVersionMapper = mock(ShowroomPreviewAssetVersionMapper.class);
        FileMapper fileMapper = mock(FileMapper.class);
        ConfigService configService = mock(ConfigService.class);
        AiTtsAliyunNlsCredentialService aliyunNlsCredentialService = mock(AiTtsAliyunNlsCredentialService.class);
        YudaoAiProperties yudaoAiProperties = mock(YudaoAiProperties.class);
        ShowroomProductRevisionRelationMapper relationMapper = mock(ShowroomProductRevisionRelationMapper.class);
        ShowroomChangeRequestMapper changeRequestMapper = mock(ShowroomChangeRequestMapper.class);
        ShowroomAssignmentService assignmentService = mock(ShowroomAssignmentService.class);
        ShowroomVersionBundleService versionBundleService = mock(ShowroomVersionBundleService.class);

        ShowroomApiRuntime runtime = new ShowroomApiRuntime(contentService, commentService, batchTaskService, productCoverImageService,
                imagePromptVersionService, narrationService, companyNarrationCodexService, companyNarrationTranslationService,
                productNarrationCodexService, previewAssetService, previewAssetVersionMapper, fileMapper, configService,
                aliyunNlsCredentialService, yudaoAiProperties, relationMapper, changeRequestMapper, assignmentService,
                versionBundleService);

        LinkedHashMap<String, String> currentFields = new LinkedHashMap<>();
        currentFields.put("owner_company_id", "124");
        currentFields.put("product_owner_type", "YINGTAI");
        currentFields.put("lifecycle_stage", "REGISTERED");
        ShowroomProductRevision currentRevision = new ShowroomProductRevision(
                1L, 1001L, 14, "PUBLISHED", "审批展示探针95177868", "Manifold for Single use-OFF", false, currentFields
        );
        ShowroomProductSnapshot snapshot = new ShowroomProductSnapshot(1L, "product_001", Optional.of(1001L), false, true);
        ShowroomProductRevision savedDraft = new ShowroomProductRevision(
                1L, 1002L, 15, "DRAFT", "审批展示探针95177868", "Manifold for Single use-OFF", false,
                Map.of(
                        "owner_company_id", "124",
                        "product_owner_type", "YINGTAI",
                        "lifecycle_stage", "REGISTERED",
                        "cover_image", "/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png"
                )
        );

        when(contentService.getLatestProductRevision(1L)).thenReturn(currentRevision);
        when(assignmentService.getLatestOpenWholeProductAssignment(1L)).thenReturn(null);
        when(changeRequestMapper.selectListByTarget("PRODUCT", 1L)).thenReturn(List.of());
        when(contentService.getProduct(1L)).thenReturn(snapshot);
        when(contentService.requireCurrentProductRevision(1L)).thenReturn(currentRevision);
        when(imagePromptVersionService.requireCurrentVersionId(ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER))
                .thenReturn(7001L);
        when(imagePromptVersionService.renderProductCoverPrompt(7001L, "审批展示探针95177868",
                "Manifold for Single use-OFF")).thenReturn("rendered product cover prompt");
        when(productCoverImageService.generateCoverImage(
                "product_001", "rendered product cover prompt"
        )).thenReturn("/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png");
        ShowroomAdminController.ProductCoverGenerateRespVO response = runtime.generateProductCoverImage(
                new ShowroomAdminController.ProductCoverGenerateReqVO(
                        1L,
                        "product_001",
                        "审批展示探针95177868",
                        "Manifold for Single use-OFF",
                        Map.of("owner_company_id", "124", "product_owner_type", "YINGTAI", "lifecycle_stage", "REGISTERED")
                ),
                300L
        );

        assertEquals("/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png",
                response.coverImage());
        verify(contentService, never()).saveProductDraft(any(ShowroomProductDraft.class));
        verify(contentService, never()).publishProductRevision(any(), any());
        verify(imagePromptVersionService).recordUsage(7001L);
    }

    @Test
    void generateProductCoverImageFailureShouldNotPersistOldCoverOrRecordPromptUsage() {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        ShowroomProductCommentService commentService = mock(ShowroomProductCommentService.class);
        ShowroomProductCoverBatchTaskService batchTaskService = mock(ShowroomProductCoverBatchTaskService.class);
        ShowroomProductCoverImageService productCoverImageService = mock(ShowroomProductCoverImageService.class);
        ShowroomImagePromptVersionService imagePromptVersionService = mock(ShowroomImagePromptVersionService.class);
        ShowroomPersistentNarrationService narrationService = mock(ShowroomPersistentNarrationService.class);
        ShowroomCompanyNarrationCodexService companyNarrationCodexService = mock(ShowroomCompanyNarrationCodexService.class);
        ShowroomCompanyNarrationTranslationService companyNarrationTranslationService =
                mock(ShowroomCompanyNarrationTranslationService.class);
        ShowroomProductNarrationCodexService productNarrationCodexService = mock(ShowroomProductNarrationCodexService.class);
        ShowroomPreviewAssetOperations previewAssetService = mock(ShowroomPreviewAssetOperations.class);
        ShowroomPreviewAssetVersionMapper previewAssetVersionMapper = mock(ShowroomPreviewAssetVersionMapper.class);
        FileMapper fileMapper = mock(FileMapper.class);
        ConfigService configService = mock(ConfigService.class);
        AiTtsAliyunNlsCredentialService aliyunNlsCredentialService = mock(AiTtsAliyunNlsCredentialService.class);
        YudaoAiProperties yudaoAiProperties = mock(YudaoAiProperties.class);
        ShowroomProductRevisionRelationMapper relationMapper = mock(ShowroomProductRevisionRelationMapper.class);
        ShowroomChangeRequestMapper changeRequestMapper = mock(ShowroomChangeRequestMapper.class);
        ShowroomAssignmentService assignmentService = mock(ShowroomAssignmentService.class);
        ShowroomVersionBundleService versionBundleService = mock(ShowroomVersionBundleService.class);

        ShowroomApiRuntime runtime = new ShowroomApiRuntime(contentService, commentService, batchTaskService, productCoverImageService,
                imagePromptVersionService, narrationService, companyNarrationCodexService, companyNarrationTranslationService,
                productNarrationCodexService, previewAssetService, previewAssetVersionMapper, fileMapper, configService,
                aliyunNlsCredentialService, yudaoAiProperties, relationMapper, changeRequestMapper, assignmentService,
                versionBundleService);

        LinkedHashMap<String, String> currentFields = new LinkedHashMap<>();
        currentFields.put("owner_company_id", "124");
        currentFields.put("product_owner_type", "YINGTAI");
        currentFields.put("lifecycle_stage", "REGISTERED");
        currentFields.put("cover_image", "/admin-api/infra/file/28/get/showroom/product/old-cover.png");
        ShowroomProductRevision currentRevision = new ShowroomProductRevision(
                1L, 1001L, 14, "PUBLISHED", "审批展示探针95177868", "Manifold for Single use-OFF", false, currentFields
        );

        when(contentService.getLatestProductRevision(1L)).thenReturn(currentRevision);
        when(assignmentService.getLatestOpenWholeProductAssignment(1L)).thenReturn(null);
        when(changeRequestMapper.selectListByTarget("PRODUCT", 1L)).thenReturn(List.of());
        when(contentService.requireCurrentProductRevision(1L)).thenReturn(currentRevision);
        when(imagePromptVersionService.requireCurrentVersionId(ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER))
                .thenReturn(7001L);
        when(imagePromptVersionService.renderProductCoverPrompt(7001L, "审批展示探针95177868",
                "Manifold for Single use-OFF")).thenReturn("rendered product cover prompt");
        when(productCoverImageService.generateCoverImage("product_001", "rendered product cover prompt"))
                .thenThrow(new IllegalStateException("SHOWROOM_COVER_GENERATION_FAILED: file upload failed"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> runtime.generateProductCoverImage(
                        new ShowroomAdminController.ProductCoverGenerateReqVO(
                                1L,
                                "product_001",
                                "审批展示探针95177868",
                                "Manifold for Single use-OFF",
                                Map.of("owner_company_id", "124", "product_owner_type", "YINGTAI",
                                        "lifecycle_stage", "REGISTERED")
                        ),
                        300L
                ));

        assertEquals("SHOWROOM_COVER_GENERATION_FAILED: file upload failed", exception.getMessage());
        verify(contentService, never()).saveProductDraft(any(ShowroomProductDraft.class));
        verify(contentService, never()).publishProductRevision(any(), any());
        verify(imagePromptVersionService, never()).recordUsage(any());
    }
}
