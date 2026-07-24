package cn.iocoder.yudao.module.showroom.content;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomContentService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomProductContentTest {

    private final ShowroomContentService contentService = new ShowroomContentService();

    @Test
    void productMayPublishWithBlankChineseNameButRequiresEnglishName() {
        ShowroomProductRevision incomplete = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-001", "", null, Map.of("target_market", "global")));

        assertTrue(contentService.getProduct(incomplete.productId()).incomplete());
        IllegalStateException missingName = assertThrows(IllegalStateException.class,
                () -> contentService.publishProductRevision(incomplete.revisionId(), 902L));
        assertTrue(missingName.getMessage().contains("SHOWROOM_REQUIRED_FIELD_MISSING"));

        ShowroomProductRevision englishNamed = contentService.saveProductDraft(new ShowroomProductDraft(
                incomplete.productId(), "P-001", "", "Insulin Injection",
                Map.of("target_market", "global")));
        ShowroomProductRevision published = contentService.publishProductRevision(englishNamed.revisionId(), 902L);

        assertEquals(englishNamed.revisionId(), published.revisionId());
        assertEquals("", published.nameCn());
        assertTrue(published.incomplete());
        assertEquals(englishNamed.revisionId(), contentService.getProduct(englishNamed.productId()).currentRevisionId().orElseThrow());
        assertTrue(contentService.versionAudits("PRODUCT", englishNamed.productId()).stream()
                .anyMatch(audit -> "name_cn".equals(audit.fieldCode())));
        assertTrue(contentService.versionAudits("PRODUCT", englishNamed.productId()).stream()
                .anyMatch(audit -> "name_en".equals(audit.fieldCode())));
    }

    @Test
    void productListShouldSupportSearchDeleteAndMaximumPageSize() {
        IntStream.rangeClosed(1, 25).forEach(index -> contentService.saveProductDraft(new ShowroomProductDraft(
                null, "code-" + index, "产品" + index, "Product " + index,
                Map.of("product_owner_type", "YINGTAI", "lifecycle_stage", "REGISTERED"))));

        assertEquals(20, contentService.listProducts("", 1, 99).size());
        assertEquals(7, contentService.listProducts("code-2", 1, 20).size());

        Long productId = contentService.listProducts("code-25", 1, 20).get(0).productId();
        contentService.deleteProduct(productId);

        assertTrue(contentService.listProducts("code-25", 1, 20).isEmpty());
        assertThrows(IllegalStateException.class, () -> contentService.getProduct(productId));
    }

}
