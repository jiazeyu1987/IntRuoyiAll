package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachment;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ShowroomReleaseWebsiteIndexAssemblyTest extends AbstractShowroomReleaseDbTest {

    @Resource
    private DataSource dataSource;

    @Test
    void shouldAssembleWebsiteIndexFromFrozenSnapshot() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        Long productId = release.sourceSnapshot().productsById().keySet().iterator().next();

        Map<String, Object> body = JsonUtils.parseObject(
                manifestQueryService.queryDocumentJson(defaultReleaseScope(), release.releaseId(),
                        ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX),
                Map.class);

        assertEquals("website-index", body.get("documentId"));
        Map<String, Object> company = castMap(body.get("company"));
        assertEquals("盈泰医疗", company.get("name"));
        Map<String, Object> homeImage = castMap(company.get("homeImage"));
        assertFalse(homeImage.containsKey("url"));
        List<Map<String, Object>> showrooms = castList(body.get("showrooms"));
        assertEquals(1, showrooms.size());
        assertEquals("CARDIOLOGY", showrooms.getFirst().get("hallCode"));
        Long hallId = release.sourceSnapshot().halls().getFirst().hall().hallId();
        Map<String, Object> hallAudioZh = castMap(showrooms.getFirst().get("audioZh"));
        Map<String, Object> hallAudioEn = castMap(showrooms.getFirst().get("audioEn"));
        assertEquals("hall-" + hallId + "-audio-zh", hallAudioZh.get("assetId"));
        assertEquals("hall-" + hallId + "-audio-en", hallAudioEn.get("assetId"));
        List<Map<String, Object>> products = castList(showrooms.getFirst().get("products"));
        assertEquals(1, products.size());
        assertEquals("product-detail-" + productId, products.getFirst().get("detailDocumentId"));
    }

    @Test
    void shouldPublishHallProductCanvasLayoutInWebsiteIndex() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        mockFile(110L, 11L, "showroom/product/balloon-cover.png", "image/png", "balloon-cover");
        mockFile(111L, 12L, "showroom/audio/balloon-zh.mp3", "audio/mpeg", "balloon-zh");
        mockFile(112L, 12L, "showroom/audio/balloon-en.mp3", "audio/mpeg", "balloon-en");

        var balloon = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-102", "球囊导管", "Balloon Catheter",
                        Map.of(
                                "target_market", "冠脉介入",
                                "target_market_en", "Coronary intervention",
                                "pipeline_layout", "心内介入BU",
                                "pipeline_layout_en", "Cardiology BU",
                                "core_selling_points", "精准扩张",
                                "core_selling_points_en", "Precise dilation",
                                "cover_image", "/admin-api/infra/file/11/get/showroom/product/balloon-cover.png")))
                .revisionId(), 903L);
        publishPreviewAsset(cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType.PRODUCT,
                balloon.productId(), balloon.revisionId(), 110L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, balloon.productId(), balloon.revisionId(),
                ShowroomNarrationLanguage.ZH, "球囊导管中文讲解", 111L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, balloon.productId(), balloon.revisionId(),
                ShowroomNarrationLanguage.EN, "Balloon catheter narration", 112L);
        contentService.replaceHallCanvasLayout(fixture.hallId(), List.of(
                new ShowroomHallProductMapping(fixture.productId(), 1, bd("0"), bd("0"), bd("0.3"), bd("1")),
                new ShowroomHallProductMapping(balloon.productId(), 2, bd("0.3"), bd("0"), bd("0.7"), bd("1"))
        ));
        bindDefaultSiteStage();

        ShowroomMaterializedRelease release = publisherService.publishRelease(900L,
                Instant.parse("2026-05-24T10:15:00Z"), DEFAULT_SITE_KEY, DEFAULT_STAGE);

        Map<String, Object> body = JsonUtils.parseObject(
                manifestQueryService.queryDocumentJson(defaultReleaseScope(), release.releaseId(),
                        ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX),
                Map.class);
        List<Map<String, Object>> showrooms = castList(body.get("showrooms"));
        List<Map<String, Object>> products = castList(showrooms.getFirst().get("products"));
        assertEquals(2, products.size());
        assertDecimal("0", products.getFirst().get("layoutX"));
        assertDecimal("0", products.getFirst().get("layoutY"));
        assertDecimal("0.3", products.getFirst().get("layoutWidth"));
        assertDecimal("1", products.getFirst().get("layoutHeight"));
        assertDecimal("0.3", products.get(1).get("layoutX"));
        assertDecimal("0", products.get(1).get("layoutY"));
        assertDecimal("0.7", products.get(1).get("layoutWidth"));
        assertDecimal("1", products.get(1).get("layoutHeight"));
    }

    @Test
    void shouldNotExportHallCanvasBackgroundInWebsiteIndex() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        contentService.updateHallCanvasBackground(fixture.hallId(),
                "/admin-api/infra/file/28/get/showroom/hall/canvas-background/layout-reference.png");
        bindDefaultSiteStage();

        ShowroomMaterializedRelease release = publisherService.publishRelease(900L,
                Instant.parse("2026-05-24T10:15:00Z"), DEFAULT_SITE_KEY, DEFAULT_STAGE);

        Map<String, Object> body = JsonUtils.parseObject(
                manifestQueryService.queryDocumentJson(defaultReleaseScope(), release.releaseId(),
                        ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX),
                Map.class);
        String json = JsonUtils.toJsonString(body);
        assertFalse(json.contains("canvasBackgroundImageUrl"));
        assertFalse(json.contains("layout-reference.png"));
    }

    @Test
    void shouldFailFastWhenPublishedHallProductLayoutIsMissing() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        new JdbcTemplate(dataSource).update("UPDATE showroom_hall_item SET layout_width = NULL WHERE hall_id = ?",
                fixture.hallId());
        bindDefaultSiteStage();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> publisherService.publishRelease(900L, Instant.parse("2026-05-24T10:15:00Z"),
                        DEFAULT_SITE_KEY, DEFAULT_STAGE));

        assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_HALL_BLOCKED"));
        assertTrue(exception.getMessage().contains("hall canvas layout"));
    }

    @Test
    void shouldFailFastWhenHallDescriptionsAreMissing() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        contentService.updateHall(fixture.hallId(), "心内介入展厅", "Cardiology Hall", "", "Hall summary");
        bindDefaultSiteStage();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> publisherService.publishRelease(900L, Instant.parse("2026-05-24T10:15:00Z"),
                        DEFAULT_SITE_KEY, DEFAULT_STAGE));

        assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_HALL_BLOCKED"));
        assertTrue(exception.getMessage().contains("hallId=" + fixture.hallId()));
        assertTrue(exception.getMessage().contains("SHOWROOM_REQUIRED_FIELD_MISSING: hall description is required"));
    }

    @Test
    void shouldAssembleWebsiteIndexFromReplacedHallMappings() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        var retainedProduct = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(fixture.productId(), "product_001", "三通旋塞",
                        "Manifold for Single use",
                        Map.of(
                                "target_market", "冠脉介入",
                                "target_market_en", "Coronary intervention",
                                "pipeline_layout", "心内介入BU",
                                "pipeline_layout_en", "Cardiology BU",
                                "core_selling_points", "中国",
                                "core_selling_points_en", "China",
                                "cover_image",
                                "/admin-api/infra/file/11/get/showroom/product/%E4%BA%A7%E5%93%81%E5%B0%81%E9%9D%A2%E5%9B%BE.png")))
                .revisionId(), 903L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, retainedProduct.productId(),
                retainedProduct.revisionId(), ShowroomNarrationLanguage.ZH, "三通旋塞中文讲解", 106L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, retainedProduct.productId(),
                retainedProduct.revisionId(), ShowroomNarrationLanguage.EN, "Manifold narration", 107L);
        var removedProduct = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "product_002", "三通旋塞-ON", "Manifold for Single use-ON",
                        Map.of())).revisionId(), 904L);
        contentService.replaceHallProductMappings(fixture.hallId(), List.of(
                new ShowroomHallProductMapping(retainedProduct.productId(), 1),
                new ShowroomHallProductMapping(removedProduct.productId(), 2)
        ));

        contentService.replaceHallProductMappings(fixture.hallId(), List.of(
                new ShowroomHallProductMapping(retainedProduct.productId(), 1)
        ));
        contentService.replaceHallCanvasLayout(fixture.hallId(), List.of(
                new ShowroomHallProductMapping(retainedProduct.productId(), 1,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE)
        ));
        bindDefaultSiteStage();
        ShowroomMaterializedRelease release = publisherService.publishRelease(900L,
                Instant.parse("2026-05-24T10:15:00Z"), DEFAULT_SITE_KEY, DEFAULT_STAGE);

        Map<String, Object> body = JsonUtils.parseObject(
                manifestQueryService.queryDocumentJson(defaultReleaseScope(), release.releaseId(),
                        ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX),
                Map.class);
        List<Map<String, Object>> showrooms = castList(body.get("showrooms"));
        List<Map<String, Object>> products = castList(showrooms.getFirst().get("products"));
        assertEquals(1, products.size());
        assertEquals("product_001", products.getFirst().get("productCode"));
        assertEquals("三通旋塞", products.getFirst().get("nameCn"));
        assertFalse(JsonUtils.toJsonString(body).contains("product_002"));
    }

    @Test
    void shouldPublishProductDetailAttachmentsAsUploadedFileReferencesWithoutReadingBinaryContent() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        mockFile(110L, 11L, "showroom/product/intro.png", "image/png", "attachment-image");
        mockFile(111L, 11L, "showroom/product/demo.mp4", "video/mp4", "attachment-video");
        mockFile(112L, 11L, "showroom/product/manual.pdf", "application/pdf", "attachment-text");

        var revision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(fixture.productId(), "P-101", "导丝系统附件版", "Guidewire System Attachments",
                        Map.of(
                                "target_market", "冠脉介入",
                                "target_market_en", "Coronary intervention",
                                "pipeline_layout", "心内介入BU",
                                "pipeline_layout_en", "Cardiology BU",
                                "core_selling_points", "中国",
                                "core_selling_points_en", "China",
                                "cover_image",
                                "/admin-api/infra/file/11/get/showroom/product/%E4%BA%A7%E5%93%81%E5%B0%81%E9%9D%A2%E5%9B%BE.png"),
                        List.of(
                                attachment("image", 110L, "intro.png", "image/png", 100L, 1),
                                attachment("video", 111L, "demo.mp4", "video/mp4", 200L, 2),
                                attachment("text", 112L, "manual.pdf", "application/pdf", 300L, 3)
                        ))).revisionId(), 905L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, revision.productId(), revision.revisionId(),
                ShowroomNarrationLanguage.ZH, "附件产品中文讲解", 106L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, revision.productId(), revision.revisionId(),
                ShowroomNarrationLanguage.EN, "Attachment product narration", 107L);
        bindDefaultSiteStage();

        ShowroomMaterializedRelease release = publisherService.publishRelease(900L,
                Instant.parse("2026-05-24T10:15:00Z"), DEFAULT_SITE_KEY, DEFAULT_STAGE);
        Map<String, Object> detail = JsonUtils.parseObject(
                manifestQueryService.queryDocumentJson(defaultReleaseScope(), release.releaseId(),
                        "product-detail-" + fixture.productId()),
                Map.class);
        List<Map<String, Object>> attachments = castList(detail.get("attachments"));

        assertEquals(List.of("intro.png", "demo.mp4", "manual.pdf"),
                attachments.stream().map(value -> value.get("originalName")).toList());
        assertEquals(List.of(
                        "/admin-api/infra/file/11/get/showroom/product/intro.png",
                        "/admin-api/infra/file/11/get/showroom/product/demo.mp4",
                        "/admin-api/infra/file/11/get/showroom/product/manual.pdf"),
                attachments.stream().map(value -> value.get("url")).toList());
        assertTrue(attachments.stream().allMatch(value -> !value.containsKey("assetId")));
        assertTrue(attachments.stream().allMatch(value -> !value.containsKey("contentHash")));
        assertTrue(release.assets().stream().noneMatch(asset -> "video".equals(asset.assetType())));
        assertTrue(release.assets().stream().noneMatch(asset -> "text".equals(asset.assetType())));
        verify(fileService, never()).getFileContent(11L, "showroom/product/demo.mp4");
        verify(fileService, never()).getFileContent(11L, "showroom/product/manual.pdf");
    }

    @Test
    void shouldFailFastWhenPublishedProductAttachmentFileIsMissing() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        var revision = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(fixture.productId(), "P-101", "导丝系统缺附件", "Guidewire Missing Attachment",
                        Map.of(
                                "target_market", "冠脉介入",
                                "target_market_en", "Coronary intervention",
                                "pipeline_layout", "心内介入BU",
                                "pipeline_layout_en", "Cardiology BU",
                                "core_selling_points", "中国",
                                "core_selling_points_en", "China",
                                "cover_image",
                                "/admin-api/infra/file/11/get/showroom/product/%E4%BA%A7%E5%93%81%E5%B0%81%E9%9D%A2%E5%9B%BE.png"),
                        List.of(attachment("text", 199L, "missing.pdf", "application/pdf", 300L, 1))))
                .revisionId(), 906L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, revision.productId(), revision.revisionId(),
                ShowroomNarrationLanguage.ZH, "缺附件产品中文讲解", 106L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, revision.productId(), revision.revisionId(),
                ShowroomNarrationLanguage.EN, "Missing attachment narration", 107L);
        bindDefaultSiteStage();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> publisherService.publishRelease(900L, Instant.parse("2026-05-24T10:15:00Z"),
                        DEFAULT_SITE_KEY, DEFAULT_STAGE));

        assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_PRODUCT_BLOCKED"));
        assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_SOURCE_MISSING"));
    }

    private static ShowroomProductAttachment attachment(String assetType, Long fileId, String originalName,
                                                       String mimeType, Long fileSize, int displayOrder) {
        return new ShowroomProductAttachment(null, null, null, assetType, fileId, originalName, mimeType, fileSize,
                displayOrder);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertDecimal(String expected, Object actual) {
        assertEquals(new BigDecimal(expected).stripTrailingZeros(), new BigDecimal(String.valueOf(actual)).stripTrailingZeros());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
