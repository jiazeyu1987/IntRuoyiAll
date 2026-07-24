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
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomApiRuntimeBatchPublishTest {

    @Test
    void batchPublishProductsShouldOnlyAttemptDirectPublishableRowsAndExposeFailures() {
        ShowroomApiRuntime runtime = spy(createRuntime());
        mockProductList(runtime, createRows());

        doAnswer(invocation -> null).when(runtime).publishProduct(argThatProductId(1L), eq(300L));
        doThrow(new IllegalStateException("当前英文讲解稿不存在，请先在 English tab 中保存英文讲解稿后再发布"))
                .when(runtime).publishProduct(argThatProductId(2L), eq(300L));

        ShowroomAdminController.ProductBatchGenerateRespVO summary = runtime.batchPublishProducts(
                new ShowroomAdminController.ProductBatchGenerateReqVO("BATCH-PUBLISH", null, null, null),
                300L
        );

        assertEquals(4, summary.matchedCount());
        assertEquals(2, summary.publishedCount());
        assertEquals(2, summary.skippedUnpublishedCount());
        assertEquals(1, summary.succeededCount());
        assertEquals(1, summary.failedCount());
        assertEquals(1, summary.failures().size());
        assertEquals(2L, summary.failures().get(0).productId());
        assertTrue(summary.failures().get(0).reason().contains("英文讲解稿"));

        ArgumentCaptor<ShowroomAdminController.ProductPublishReqVO> reqCaptor =
                ArgumentCaptor.forClass(ShowroomAdminController.ProductPublishReqVO.class);
        verify(runtime, times(2)).publishProduct(reqCaptor.capture(), eq(300L));

        List<ShowroomAdminController.ProductPublishReqVO> requests = reqCaptor.getAllValues();
        assertTrue(requests.stream().anyMatch(req ->
                req.productId().equals(1L)
                        && req.productCode().equals("BATCH-PUBLISH-DRAFT")
                        && req.sourceRevisionId() == null
                        && req.fields().get("core_selling_points").equals("草稿可直发卖点")
        ));
        assertTrue(requests.stream().anyMatch(req ->
                req.productId().equals(2L)
                        && req.productCode().equals("BATCH-PUBLISH-REJECTED")
                        && req.sourceRevisionId() == null
                        && req.fields().get("core_selling_points").equals("驳回后可直发卖点")
        ));
    }

    private static ShowroomAdminController.ProductPublishReqVO argThatProductId(Long productId) {
        return org.mockito.ArgumentMatchers.argThat(req -> req != null && productId.equals(req.productId()));
    }

    private static ShowroomApiRuntime createRuntime() {
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
                1L, new ShowroomProductRevision(1L, 1001L, 7, "DRAFT",
                        "批量发布草稿产品", "Batch Publish Draft Product", false,
                        Map.of("target_market", "中国", "core_selling_points", "草稿可直发卖点")),
                2L, new ShowroomProductRevision(2L, 2001L, 8, "REJECTED",
                        "批量发布驳回产品", "Batch Publish Rejected Product", false,
                        Map.of("target_market", "中国", "core_selling_points", "驳回后可直发卖点")),
                3L, new ShowroomProductRevision(3L, 3001L, 9, "PUBLISHED",
                        "批量发布已发布产品", "Batch Publish Live Product", false,
                        Map.of("target_market", "中国", "core_selling_points", "已发布无需再直发")),
                4L, new ShowroomProductRevision(4L, 4001L, 10, "APPROVED",
                        "批量发布审批通过产品", "Batch Publish Approved Product", false,
                        Map.of("target_market", "中国", "core_selling_points", "审批通过但不走直发"))
        );

        when(contentService.listProducts()).thenReturn(new ArrayList<>(snapshots));
        when(contentService.getProduct(any())).thenAnswer(invocation -> snapshots.stream()
                .filter(snapshot -> snapshot.productId().equals(invocation.getArgument(0)))
                .findFirst()
                .orElseThrow());
        when(contentService.getLatestProductRevision(any())).thenAnswer(invocation -> revisions.get(invocation.getArgument(0)));
        when(contentService.getProductRevision(any())).thenAnswer(invocation ->
                revisions.values().stream()
                        .filter(revision -> revision.revisionId().equals(invocation.getArgument(0)))
                        .findFirst()
                        .orElseThrow());
        when(assignmentService.getLatestOpenWholeProductAssignment(any())).thenReturn(null);
        when(changeRequestMapper.selectListByTarget(any(), any())).thenReturn(List.of());
        when(narrationService.live(any())).thenReturn(Optional.empty());
        when(relationMapper.selectListByProductRevisionId(any())).thenReturn(List.of());
    }

    private static List<ShowroomProductSnapshot> createRows() {
        return List.of(
                new ShowroomProductSnapshot(1L, "BATCH-PUBLISH-DRAFT", Optional.of(1001L), false, true),
                new ShowroomProductSnapshot(2L, "BATCH-PUBLISH-REJECTED", Optional.of(2001L), false, true),
                new ShowroomProductSnapshot(3L, "BATCH-PUBLISH-PUBLISHED", Optional.of(3001L), false, true),
                new ShowroomProductSnapshot(4L, "BATCH-PUBLISH-APPROVED", Optional.of(4001L), false, true)
        );
    }
}
