package cn.iocoder.yudao.module.showroom.controller;

import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomApiRuntimeHallBuCanvasLayoutTest {

    @Test
    void calculateHallBuCanvasLayoutShouldSortProductsByBuAndKeepAwardsInPlace() {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        ShowroomApiRuntime runtime = createRuntime(contentService);
        when(contentService.getCurrentOrLatestProductRevision(101L)).thenReturn(revision(101L, "BU-B"));
        when(contentService.getCurrentOrLatestProductRevision(102L)).thenReturn(revision(102L, "BU-A"));
        when(contentService.getCurrentOrLatestProductRevision(103L)).thenReturn(revision(103L, "BU-B"));
        when(contentService.getCurrentOrLatestProductRevision(104L)).thenReturn(revision(104L, ""));

        ShowroomAdminController.HallItemMappingReqVO result = runtime.calculateHallBuCanvasLayout(
                new ShowroomAdminController.HallItemMappingReqVO(9L, List.of(
                        item("PRODUCT", 101L, 1, "0.000000", "0.000000", "0.500000", "0.500000"),
                        item("AWARD", 301L, 2, "0.700000", "0.700000", "0.100000", "0.100000"),
                        item("PRODUCT", 102L, 3, "0.500000", "0.000000", "0.500000", "0.500000"),
                        item("PRODUCT", 103L, 4, "0.000000", "0.500000", "0.500000", "0.500000"),
                        item("PRODUCT", 104L, 5, "0.500000", "0.500000", "0.500000", "0.500000")
                )));

        assertEquals(9L, result.hallId());
        assertEquals(List.of("PRODUCT:101", "PRODUCT:103", "PRODUCT:102", "PRODUCT:104", "AWARD:301"),
                result.items().stream().map(item -> item.itemType() + ":" + item.itemId()).toList());
        assertRect(result.items().get(0), "0.000000", "0.000000", "0.500000", "0.500000");
        assertRect(result.items().get(1), "0.500000", "0.000000", "0.500000", "0.500000");
        assertRect(result.items().get(2), "0.000000", "0.500000", "0.500000", "0.500000");
        assertRect(result.items().get(3), "0.500000", "0.500000", "0.500000", "0.500000");
        assertRect(result.items().get(4), "0.700000", "0.700000", "0.100000", "0.100000");
        verify(contentService, never()).replaceHallItemCanvasLayout(any(), any());
    }

    @Test
    void calculateHallBuCanvasLayoutShouldKeepAwardsWhenNoProducts() {
        ShowroomPersistentContentService contentService = mock(ShowroomPersistentContentService.class);
        ShowroomApiRuntime runtime = createRuntime(contentService);

        ShowroomAdminController.HallItemMappingReqVO result = runtime.calculateHallBuCanvasLayout(
                new ShowroomAdminController.HallItemMappingReqVO(9L, List.of(
                        item("AWARD", 301L, 1, "0.700000", "0.700000", "0.100000", "0.100000")
                )));

        assertEquals(9L, result.hallId());
        assertEquals(1, result.items().size());
        assertEquals("AWARD", result.items().get(0).itemType());
        assertEquals(301L, result.items().get(0).itemId());
        assertEquals(1, result.items().get(0).displayOrder());
        assertRect(result.items().get(0), "0.700000", "0.700000", "0.100000", "0.100000");
        verify(contentService, never()).getCurrentOrLatestProductRevision(any());
    }

    private static ShowroomAdminController.HallItemMappingItemReqVO item(
            String itemType, Long itemId, Integer displayOrder,
            String layoutX, String layoutY, String layoutWidth, String layoutHeight) {
        return new ShowroomAdminController.HallItemMappingItemReqVO(itemType, itemId, displayOrder,
                new BigDecimal(layoutX), new BigDecimal(layoutY), new BigDecimal(layoutWidth),
                new BigDecimal(layoutHeight));
    }

    private static ShowroomProductRevision revision(Long productId, String pipelineLayout) {
        return new ShowroomProductRevision(productId, 1000L + productId, 1, "APPROVED",
                "产品" + productId, "Product " + productId, false,
                Map.of("pipeline_layout", pipelineLayout));
    }

    private static void assertRect(ShowroomAdminController.HallItemMappingItemReqVO item,
                                   String x, String y, String width, String height) {
        assertEquals(new BigDecimal(x), item.layoutX());
        assertEquals(new BigDecimal(y), item.layoutY());
        assertEquals(new BigDecimal(width), item.layoutWidth());
        assertEquals(new BigDecimal(height), item.layoutHeight());
    }

    private static ShowroomApiRuntime createRuntime(ShowroomPersistentContentService contentService) {
        ShowroomProductCommentService commentService = mock(ShowroomProductCommentService.class);
        ShowroomPersistentNarrationService narrationService = mock(ShowroomPersistentNarrationService.class);
        ShowroomProductRevisionRelationMapper relationMapper = mock(ShowroomProductRevisionRelationMapper.class);
        ShowroomChangeRequestMapper changeRequestMapper = mock(ShowroomChangeRequestMapper.class);
        ShowroomAssignmentService assignmentService = mock(ShowroomAssignmentService.class);
        when(commentService.pageByProduct(any(), any(), any(), any())).thenReturn(List.of());
        when(relationMapper.selectListByProductRevisionId(any())).thenReturn(List.of());
        when(narrationService.live(any())).thenReturn(Optional.empty());
        when(narrationService.latest(any(), any())).thenReturn(Optional.empty());
        when(assignmentService.getLatestOpenWholeProductAssignment(any())).thenReturn(null);
        when(changeRequestMapper.selectListByTarget(any(), any())).thenReturn(List.of());
        return new ShowroomApiRuntime(contentService, commentService,
                mock(ShowroomProductCoverBatchTaskService.class), mock(ShowroomProductCoverImageService.class),
                mock(ShowroomImagePromptVersionService.class), narrationService,
                mock(ShowroomCompanyNarrationCodexService.class),
                mock(ShowroomCompanyNarrationTranslationService.class),
                mock(ShowroomProductNarrationCodexService.class),
                mock(ShowroomPreviewAssetOperations.class), mock(ShowroomPreviewAssetVersionMapper.class),
                mock(FileMapper.class), mock(ConfigService.class), mock(AiTtsAliyunNlsCredentialService.class),
                mock(YudaoAiProperties.class), relationMapper, changeRequestMapper, assignmentService,
                mock(ShowroomVersionBundleService.class));
    }
}
