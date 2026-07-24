package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomReleaseProductDetailAssemblyTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldAssembleProductDetailDocument() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        Long productId = release.sourceSnapshot().productsById().keySet().iterator().next();

        Map<String, Object> body = JsonUtils.parseObject(
                manifestQueryService.queryDocumentJson(defaultReleaseScope(), release.releaseId(),
                        "product-detail-" + productId), Map.class);

        assertEquals("product-detail", body.get("kind"));
        assertEquals(String.valueOf(productId), body.get("productId"));
        assertEquals("产品中文讲解", body.get("subtitleZh"));
        assertEquals("English product narration", body.get("subtitleEn"));
        Map<String, Object> audioZh = castMap(body.get("audioZh"));
        assertEquals("product-" + productId + "-audio-zh", audioZh.get("assetId"));
        List<Map<String, Object>> bilingualFields = castList(body.get("bilingualPublicFields"));
        assertTrue(bilingualFields.stream().anyMatch(field -> "name".equals(field.get("fieldCode"))));
        assertTrue(bilingualFields.stream().anyMatch(field -> "target_market".equals(field.get("fieldCode"))));
        Map<String, Object> bu = requireField(bilingualFields, "pipeline_layout");
        assertEquals("BU", bu.get("labelZh"));
        assertEquals("BU", bu.get("labelEn"));
        assertEquals("心内介入BU", bu.get("valueZh"));
        assertEquals("Cardiology BU", bu.get("valueEn"));
        Map<String, Object> salesCountry = requireField(bilingualFields, "target_market");
        assertEquals("在售国家", salesCountry.get("labelZh"));
        assertEquals("Countries on Sale", salesCountry.get("labelEn"));
        assertEquals("冠脉介入", salesCountry.get("valueZh"));
        assertEquals("Coronary intervention", salesCountry.get("valueEn"));
        Map<String, Object> sellingPoints = requireField(bilingualFields, "core_selling_points");
        assertEquals("卖点文案", sellingPoints.get("labelZh"));
        assertEquals("Selling Points Copy", sellingPoints.get("labelEn"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private static Map<String, Object> requireField(List<Map<String, Object>> fields, String fieldCode) {
        return fields.stream()
                .filter(field -> fieldCode.equals(field.get("fieldCode")))
                .findFirst()
                .orElseThrow();
    }
}
