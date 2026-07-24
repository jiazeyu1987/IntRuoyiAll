package cn.iocoder.yudao.module.showroom.content;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomContentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomHallContentTest {

    private final ShowroomContentService contentService = new ShowroomContentService();

    @Test
    void hallProductMappingsShouldRequireExplicitDisplayOrderAndReadInOrder() {
        Long hallId = contentService.createHall("main", "主展厅", "Main Hall", "主展厅描述", "Main Hall Description")
                .hallId();

        IllegalStateException emptyMappings = assertThrows(IllegalStateException.class,
                () -> contentService.replaceHallProductMappings(hallId, List.of()));
        assertTrue(emptyMappings.getMessage().contains("SHOWROOM_REQUIRED_FIELD_MISSING"));

        IllegalStateException missingOrder = assertThrows(IllegalStateException.class,
                () -> contentService.replaceHallProductMappings(hallId, List.of(
                        new ShowroomHallProductMapping(createProduct("missing-order"), null))));
        assertTrue(missingOrder.getMessage().contains("SHOWROOM_REQUIRED_FIELD_MISSING"));

        Long productId1 = createProduct("ordered-1");
        Long productId2 = createProduct("ordered-2");
        contentService.replaceHallProductMappings(hallId, List.of(
                new ShowroomHallProductMapping(productId2, 20),
                new ShowroomHallProductMapping(productId1, 10)));

        assertEquals(List.of(productId1, productId2), contentService.getHall(hallId).productMappings().stream()
                .map(ShowroomHallProductMapping::productId)
                .toList());
    }

    @Test
    void hallCanvasLayoutShouldPersistCoordinatesAndRejectInvalidRectangles() {
        Long hallId = contentService.createHall("canvas", "画布展柜", "Canvas Hall", "画布描述", "Canvas")
                .hallId();
        Long productId1 = createProduct("canvas-1");
        Long productId2 = createProduct("canvas-2");

        contentService.replaceHallCanvasLayout(hallId, List.of(
                new ShowroomHallProductMapping(productId1, 1,
                        BigDecimal.ZERO, BigDecimal.ZERO, bd("0.5"), BigDecimal.ONE),
                new ShowroomHallProductMapping(productId2, 2,
                        bd("0.5"), BigDecimal.ZERO, bd("0.5"), BigDecimal.ONE)));

        List<ShowroomHallProductMapping> persisted = contentService.getHall(hallId).productMappings();
        assertEquals(bd("0.5"), persisted.get(0).layoutWidth());
        assertEquals(BigDecimal.ONE, persisted.get(1).layoutHeight());

        IllegalStateException missingLayout = assertThrows(IllegalStateException.class,
                () -> contentService.replaceHallCanvasLayout(hallId, List.of(
                        new ShowroomHallProductMapping(productId1, 1))));
        assertTrue(missingLayout.getMessage().contains("SHOWROOM_REQUIRED_FIELD_MISSING"));

        IllegalStateException overlappingLayout = assertThrows(IllegalStateException.class,
                () -> contentService.replaceHallCanvasLayout(hallId, List.of(
                        new ShowroomHallProductMapping(productId1, 1,
                                BigDecimal.ZERO, BigDecimal.ZERO, bd("0.6"), BigDecimal.ONE),
                        new ShowroomHallProductMapping(productId2, 2,
                                bd("0.5"), BigDecimal.ZERO, bd("0.5"), BigDecimal.ONE))));
        assertTrue(overlappingLayout.getMessage().contains("SHOWROOM_CANVAS_LAYOUT_INVALID"));
    }

    @Test
    void selectedHallProductsShouldKeepLayoutEmptyUntilCanvasLayoutIsSaved() {
        Long hallId = contentService.createHall("selected-products", "展项选择", "Selected Products", "", "")
                .hallId();
        List<ShowroomHallProductMapping> mappings = IntStream.rangeClosed(1, 23)
                .mapToObj(index -> new ShowroomHallProductMapping(createProduct("selected-" + index), index))
                .toList();

        contentService.replaceHallProductMappings(hallId, mappings);

        List<ShowroomHallProductMapping> persisted = contentService.getHall(hallId).productMappings();
        assertEquals(23, persisted.size());
        assertEquals(null, persisted.get(0).layoutWidth());
        assertEquals(null, persisted.get(1).layoutWidth());
    }

    @Test
    void hallListShouldSupportSearchDeleteAndMaximumPageSize() {
        IntStream.rangeClosed(1, 25).forEach(index ->
                contentService.createHall(
                        "hall-" + index,
                        "展厅" + index,
                        "Hall " + index,
                        "描述" + index,
                        "Description " + index));

        assertEquals(20, contentService.listHalls("", 1, 99).size());
        assertEquals(7, contentService.listHalls("hall-2", 1, 20).size());

        Long hallId = contentService.listHalls("hall-25", 1, 20).get(0).hallId();
        contentService.deleteHall(hallId);

        assertTrue(contentService.listHalls("hall-25", 1, 20).isEmpty());
        assertThrows(IllegalStateException.class, () -> contentService.getHall(hallId));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private Long createProduct(String code) {
        return contentService.saveProductDraft(new ShowroomProductDraft(null, code, "产品" + code,
                "Product " + code, Map.of("product_owner_type", "YINGTAI"))).productId();
    }

}
