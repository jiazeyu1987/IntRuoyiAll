package cn.iocoder.yudao.module.showroom.controller;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomApiRuntimeProductPageTest {

    @Test
    void listProductsPageShouldAssembleOnlyCurrentPageRows() {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        RuntimeFixture fixture = createRuntime(contentService);
        List<ShowroomProductSnapshot> snapshots = createSnapshots(21);
        Map<Long, ShowroomProductRevision> revisions = createRevisions(20);

        when(contentService.listProducts()).thenReturn(snapshots);
        when(contentService.getLatestProductRevision(any())).thenAnswer(invocation -> {
            Long productId = invocation.getArgument(0);
            if (productId == 21L) {
                throw new IllegalStateException("off-page product should not be assembled");
            }
            return revisions.get(productId);
        });
        when(contentService.getProductRevision(any())).thenAnswer(invocation -> {
            Long revisionId = invocation.getArgument(0);
            return revisions.values().stream()
                    .filter(revision -> revision.revisionId().equals(revisionId))
                    .findFirst()
                    .orElseThrow();
        });

        when(contentService.pageProducts(1, 20)).thenReturn(new PageResult<>(snapshots.subList(0, 20), 21L));
        when(contentService.latestProductRevisions(any())).thenReturn(revisions);
        when(contentService.productRevisions(any())).thenReturn(revisionsByRevisionId(revisions));

        PageResult<ShowroomAdminController.ProductPageRespVO> page = fixture.runtime().listProducts(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20));

        assertEquals(21L, page.getTotal());
        assertEquals(20, page.getList().size());
        assertEquals(1L, page.getList().get(0).productId());
        assertEquals(20L, page.getList().get(19).productId());
        verify(contentService).pageProducts(1, 20);
        verify(contentService, never()).getLatestProductRevision(21L);
    }

    @Test
    void listProductsFilteredPageShouldAssembleOnlyMatchedCurrentPageRows() {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        RuntimeFixture fixture = createRuntime(contentService);
        List<ShowroomProductSnapshot> snapshots = createSnapshots(25);
        Map<Long, ShowroomProductRevision> revisions = createRevisions(25);

        when(contentService.listProducts()).thenReturn(snapshots);
        when(contentService.latestProductRevisions(any())).thenReturn(revisions);
        when(contentService.productRevisions(any())).thenReturn(revisionsByRevisionId(revisions));

        PageResult<ShowroomAdminController.ProductPageRespVO> page = fixture.runtime().listProducts(
                new ShowroomAdminController.PageQueryReqVO("分页产品", 1, 20));

        assertEquals(25L, page.getTotal());
        assertEquals(20, page.getList().size());
        assertEquals(1L, page.getList().get(0).productId());
        assertEquals(20L, page.getList().get(19).productId());
        verify(fixture.commentService(), never()).pageByProduct(eq(21L), any(), any(), any());
        verify(contentService, never()).getLatestProductRevision(21L);
    }

    @Test
    void listProductsPageShouldRespectVisibleAndEditableScopes() {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        RuntimeFixture fixture = createRuntime(contentService);
        List<ShowroomProductSnapshot> snapshots = createSnapshots(5);
        Map<Long, ShowroomProductRevision> revisions = createRevisions(5);

        when(contentService.listProducts()).thenReturn(snapshots);
        when(contentService.latestProductRevisions(any())).thenReturn(revisions);
        when(contentService.productRevisions(any())).thenReturn(revisionsByRevisionId(revisions));

        PageResult<ShowroomAdminController.ProductPageRespVO> page = fixture.runtime().listProducts(
                new ShowroomAdminController.PageQueryReqVO(null, 1, 20),
                Set.of(2L, 3L, 4L),
                Set.of(3L));

        assertEquals(3L, page.getTotal());
        assertEquals(List.of(2L, 3L, 4L), page.getList().stream()
                .map(ShowroomAdminController.ProductPageRespVO::productId)
                .toList());
        assertEquals(List.of(false, true, false), page.getList().stream()
                .map(ShowroomAdminController.ProductPageRespVO::editable)
                .toList());
    }

    @Test
    void productPagePayloadShouldNotExposeFreezeState() {
        assertFalse(hasRecordComponent(ShowroomAdminController.ProductPageRespVO.class, "frozen"));
        assertFalse(hasRecordComponent(ShowroomAdminController.ProductDetailRespVO.class, "frozen"));
    }

    private static RuntimeFixture createRuntime(ShowroomPersistentContentService contentService) {
        ShowroomProductCommentService commentService = mock(ShowroomProductCommentService.class);
        ShowroomProductCoverBatchTaskService batchTaskService = mock(ShowroomProductCoverBatchTaskService.class);
        ShowroomProductCoverImageService productCoverImageService = mock(ShowroomProductCoverImageService.class);
        ShowroomImagePromptVersionService imagePromptVersionService = mock(ShowroomImagePromptVersionService.class);
        ShowroomPersistentNarrationService narrationService = mock(ShowroomPersistentNarrationService.class);
        ShowroomCompanyNarrationCodexService companyNarrationCodexService =
                mock(ShowroomCompanyNarrationCodexService.class);
        ShowroomCompanyNarrationTranslationService companyNarrationTranslationService =
                mock(ShowroomCompanyNarrationTranslationService.class);
        ShowroomProductNarrationCodexService productNarrationCodexService =
                mock(ShowroomProductNarrationCodexService.class);
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

        when(commentService.pageByProduct(any(), any(), any(), any())).thenReturn(List.of());
        when(relationMapper.selectListByProductRevisionId(any())).thenReturn(List.of());
        when(narrationService.live(any())).thenReturn(Optional.empty());
        when(narrationService.latest(any(), any())).thenReturn(Optional.empty());
        when(assignmentService.getLatestOpenWholeProductAssignment(any())).thenReturn(null);
        when(changeRequestMapper.selectListByTarget(any(), any())).thenReturn(List.of());

        ShowroomApiRuntime runtime = new ShowroomApiRuntime(contentService, commentService, batchTaskService,
                productCoverImageService, imagePromptVersionService, narrationService, companyNarrationCodexService,
                companyNarrationTranslationService, productNarrationCodexService, previewAssetService,
                previewAssetVersionMapper, fileMapper, configService, aliyunNlsCredentialService, yudaoAiProperties,
                relationMapper, changeRequestMapper, assignmentService, versionBundleService);
        return new RuntimeFixture(runtime, commentService);
    }

    private static List<ShowroomProductSnapshot> createSnapshots(int count) {
        List<ShowroomProductSnapshot> snapshots = new ArrayList<>();
        for (long productId = 1L; productId <= count; productId++) {
            snapshots.add(new ShowroomProductSnapshot(productId, "PAGE-PRODUCT-" + productId,
                    Optional.of(1000L + productId), false, true));
        }
        return snapshots;
    }

    private static Map<Long, ShowroomProductRevision> createRevisions(int count) {
        Map<Long, ShowroomProductRevision> revisions = new LinkedHashMap<>();
        for (long productId = 1L; productId <= count; productId++) {
            revisions.put(productId, new ShowroomProductRevision(productId, 1000L + productId, 1,
                    "APPROVED", "分页产品" + productId, "Page Product " + productId, false,
                    Map.of("owner_company_id", "1", "product_owner_type", "INTERNAL",
                            "lifecycle_stage", "REGISTERED")));
        }
        return revisions;
    }

    private static Map<Long, ShowroomProductRevision> revisionsByRevisionId(
            Map<Long, ShowroomProductRevision> revisionsByProductId) {
        Map<Long, ShowroomProductRevision> revisionsByRevisionId = new LinkedHashMap<>();
        for (ShowroomProductRevision revision : revisionsByProductId.values()) {
            revisionsByRevisionId.put(revision.revisionId(), revision);
        }
        return revisionsByRevisionId;
    }

    private static boolean hasRecordComponent(Class<?> type, String componentName) {
        return Arrays.stream(type.getRecordComponents())
                .anyMatch(component -> component.getName().equals(componentName));
    }

    private record RuntimeFixture(ShowroomApiRuntime runtime,
                                  ShowroomProductCommentService commentService) {
    }
}
