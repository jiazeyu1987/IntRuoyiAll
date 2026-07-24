package cn.iocoder.yudao.module.showroom.controller;

import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
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
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomApiRuntimeBatchCoverModeTest {

    @Test
    void batchGenerateProductCoverImageShouldOnlySnapshotMissingPublishedProductsForMissingOnlyMode() {
        ShowroomProductCoverBatchTaskService batchTaskService = mock(ShowroomProductCoverBatchTaskService.class);
        ShowroomApiRuntime runtime = createRuntime(batchTaskService);
        mockProductList(runtime, createRows());
        when(batchTaskService.startTask(any())).thenReturn(new ShowroomAdminController.ProductBatchGenerateRespVO(
                4, 3, 1, 1, 0, 2, 0, false, 0, 91L, "COMPLETED", 0, null, List.of()
        ));

        ShowroomAdminController.ProductBatchGenerateRespVO summary = runtime.batchGenerateProductCoverImage(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-COVER", null, null, null, "MISSING_ONLY"),
                300L
        );

        ArgumentCaptor<ShowroomProductCoverBatchTaskService.StartTaskCommand> commandCaptor =
                ArgumentCaptor.forClass(ShowroomProductCoverBatchTaskService.StartTaskCommand.class);
        verify(batchTaskService).startTask(commandCaptor.capture());
        ShowroomProductCoverBatchTaskService.StartTaskCommand command = commandCaptor.getValue();

        assertEquals(4, summary.matchedCount());
        assertEquals(7001L, command.promptVersionId());
        assertEquals(3, command.publishedCount());
        assertEquals(1, command.skippedExistingCount());
        assertEquals(2, command.items().size());
        assertTrue(command.items().stream().allMatch(item -> item.productCode().startsWith("BATCH-COVER-MISSING-")));
    }

    @Test
    void batchGenerateProductCoverImageShouldSnapshotExistingAndMissingPublishedProductsForAllMode() {
        ShowroomProductCoverBatchTaskService batchTaskService = mock(ShowroomProductCoverBatchTaskService.class);
        ShowroomApiRuntime runtime = createRuntime(batchTaskService);
        mockProductList(runtime, createRows());
        when(batchTaskService.startTask(any())).thenReturn(new ShowroomAdminController.ProductBatchGenerateRespVO(
                4, 3, 1, 0, 0, 3, 0, false, 0, 92L, "COMPLETED", 0, null, List.of()
        ));

        runtime.batchGenerateProductCoverImage(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-COVER", null, null, null, "ALL"),
                300L
        );

        ArgumentCaptor<ShowroomProductCoverBatchTaskService.StartTaskCommand> commandCaptor =
                ArgumentCaptor.forClass(ShowroomProductCoverBatchTaskService.StartTaskCommand.class);
        verify(batchTaskService).startTask(commandCaptor.capture());
        ShowroomProductCoverBatchTaskService.StartTaskCommand command = commandCaptor.getValue();

        assertEquals(0, command.skippedExistingCount());
        assertEquals(3, command.items().size());
        assertTrue(command.items().stream().anyMatch(item -> "BATCH-COVER-MISSING-EXISTING".equals(item.productCode())));
    }

    private static ShowroomApiRuntime createRuntime(ShowroomProductCoverBatchTaskService batchTaskService) {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        ShowroomProductCommentService commentService = mock(ShowroomProductCommentService.class);
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

        when(imagePromptVersionService.requireCurrentVersionId(ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER))
                .thenReturn(7001L);

        return new ShowroomApiRuntime(contentService, commentService, batchTaskService, productCoverImageService,
                imagePromptVersionService, narrationService, companyNarrationCodexService, companyNarrationTranslationService,
                productNarrationCodexService, previewAssetService, previewAssetVersionMapper, fileMapper, configService,
                aliyunNlsCredentialService, yudaoAiProperties, relationMapper, changeRequestMapper, assignmentService,
                versionBundleService);
    }

    private static void mockProductList(ShowroomApiRuntime runtime, List<ShowroomProductSnapshot> snapshots) {
        ShowroomPersistentContentService contentService = (ShowroomPersistentContentService)
                org.springframework.test.util.ReflectionTestUtils.getField(runtime, "contentService");
        ShowroomAssignmentService assignmentService = (ShowroomAssignmentService)
                org.springframework.test.util.ReflectionTestUtils.getField(runtime, "assignmentService");
        ShowroomChangeRequestMapper changeRequestMapper = (ShowroomChangeRequestMapper)
                org.springframework.test.util.ReflectionTestUtils.getField(runtime, "changeRequestMapper");
        ShowroomPersistentNarrationService narrationService = (ShowroomPersistentNarrationService)
                org.springframework.test.util.ReflectionTestUtils.getField(runtime, "narrationService");
        ShowroomProductRevisionRelationMapper relationMapper = (ShowroomProductRevisionRelationMapper)
                org.springframework.test.util.ReflectionTestUtils.getField(runtime, "productRevisionRelationMapper");

        Map<Long, ShowroomProductRevision> revisions = Map.of(
                1L, new ShowroomProductRevision(1L, 1001L, 7, "PUBLISHED", "批量封面已有产品", "Batch Cover Existing Product", false,
                        Map.of("target_market", "中国", "core_selling_points", "已有封面卖点",
                                "cover_image", "/admin-api/infra/file/29/get/showroom/product/cover/existing.png")),
                2L, new ShowroomProductRevision(2L, 2001L, 8, "PUBLISHED", "批量封面缺失产品一", "Batch Cover Missing Product 1", false,
                        Map.of("target_market", "中国", "core_selling_points", "缺失封面卖点一")),
                3L, new ShowroomProductRevision(3L, 3001L, 9, "PUBLISHED", "批量封面缺失产品二", "Batch Cover Missing Product 2", false,
                        Map.of("target_market", "中国", "core_selling_points", "缺失封面卖点二")),
                4L, new ShowroomProductRevision(4L, 4001L, 3, "DRAFT", "批量封面草稿产品", "Batch Cover Draft Product", false,
                        Map.of("target_market", "中国", "core_selling_points", "草稿封面卖点"))
        );

        when(contentService.listProducts()).thenReturn(new ArrayList<>(snapshots));
        when(contentService.getProduct(any())).thenAnswer(invocation -> snapshots.stream()
                .filter(snapshot -> snapshot.productId().equals(invocation.getArgument(0)))
                .findFirst()
                .orElseThrow());
        when(contentService.getLatestProductRevision(any())).thenAnswer(invocation -> revisions.get(invocation.getArgument(0)));
        when(assignmentService.getLatestOpenWholeProductAssignment(any())).thenReturn(null);
        when(changeRequestMapper.selectListByTarget(any(), any())).thenReturn(List.of());
        when(narrationService.live(any())).thenReturn(Optional.empty());
        when(relationMapper.selectListByProductRevisionId(any())).thenReturn(List.of());
    }

    private static List<ShowroomProductSnapshot> createRows() {
        return List.of(
                new ShowroomProductSnapshot(1L, "BATCH-COVER-MISSING-EXISTING", Optional.of(1001L), false, true),
                new ShowroomProductSnapshot(2L, "BATCH-COVER-MISSING-1", Optional.of(2001L), false, true),
                new ShowroomProductSnapshot(3L, "BATCH-COVER-MISSING-2", Optional.of(3001L), false, true),
                new ShowroomProductSnapshot(4L, "BATCH-COVER-MISSING-DRAFT", Optional.of(4001L), false, false)
        );
    }
}
