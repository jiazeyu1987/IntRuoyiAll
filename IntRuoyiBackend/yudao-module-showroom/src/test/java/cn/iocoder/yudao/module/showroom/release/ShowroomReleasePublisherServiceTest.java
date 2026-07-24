package cn.iocoder.yudao.module.showroom.release;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ShowroomReleasePublisherServiceTest extends AbstractShowroomReleaseDbTest {

    @Resource
    private ShowroomReleasePublisherService publisherService;

    @Test
    void shouldPublishImmutableReleaseFromPublishedSourceSnapshot() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        bindDefaultSiteStage();

        var release = publisherService.publishRelease(900L, fixture.publishedAt(), DEFAULT_SITE_KEY, DEFAULT_STAGE);

        assertNotNull(release);
    }

    @Test
    void shouldFailFastWhenMappedProductUsesPreviewAssetWithoutCover() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        mockFile(108L, 11L, "showroom/product/preview-only.png", "image/png", "product-preview-only");
        mockFile(109L, 12L, "showroom/audio/product-preview-only-zh.mp3", "audio/mpeg", "product-preview-only-zh");
        mockFile(110L, 12L, "showroom/audio/product-preview-only-en.mp3", "audio/mpeg", "product-preview-only-en");

        ShowroomProductRevision previewOnlyProduct = contentService.publishProductRevision(contentService.saveProductDraft(
                new ShowroomProductDraft(null, "P-102", "输送导管", "Delivery Catheter",
                        Map.of(
                                "target_market", "外周介入",
                                "target_market_en", "Peripheral intervention",
                                "core_selling_points", "更稳定",
                                "core_selling_points_en", "More stable"))).revisionId(), 903L);

        contentService.replaceHallCanvasLayout(fixture.hallId(), List.of(
                new ShowroomHallProductMapping(fixture.productId(), 1,
                        BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.5"), BigDecimal.ONE),
                new ShowroomHallProductMapping(previewOnlyProduct.productId(), 2,
                        new BigDecimal("0.5"), BigDecimal.ZERO, new BigDecimal("0.5"), BigDecimal.ONE)));
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, previewOnlyProduct.productId(),
                previewOnlyProduct.revisionId(), 108L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, previewOnlyProduct.productId(),
                previewOnlyProduct.revisionId(), ShowroomNarrationLanguage.ZH, "预览图产品中文讲解", 109L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, previewOnlyProduct.productId(),
                previewOnlyProduct.revisionId(), ShowroomNarrationLanguage.EN, "Preview-only product narration", 110L);

        bindDefaultSiteStage();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> publisherService.publishRelease(900L, fixture.publishedAt(), DEFAULT_SITE_KEY, DEFAULT_STAGE));

        assertTrue(exception.getMessage().contains("product cover_image is required"));
    }

    @Test
    void shouldFailFastWhenMappedProductNarrationIsMissing() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        mockFile(113L, 11L, "showroom/product/missing-narration-cover.png", "image/png",
                "missing-narration-cover");
        mockFile(114L, 12L, "showroom/audio/product-missing-narration-zh.mp3", "audio/mpeg",
                "product-missing-narration-zh");

        ShowroomProductRevision missingNarrationProduct = contentService.publishProductRevision(
                contentService.saveProductDraft(new ShowroomProductDraft(null, "P-104", "缺少英文讲解产品",
                        "Missing English Narration Product",
                        Map.of(
                                "target_market", "冠脉介入",
                                "target_market_en", "Coronary intervention",
                                "core_selling_points", "用于验证缺失英文讲解必须阻断发布",
                                "core_selling_points_en", "Verifies missing English narration blocks release",
                                "cover_image", "/admin-api/infra/file/11/get/showroom/product/missing-narration-cover.png")))
                        .revisionId(), 905L);

        contentService.replaceHallCanvasLayout(fixture.hallId(), List.of(
                new ShowroomHallProductMapping(fixture.productId(), 1,
                        BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.5"), BigDecimal.ONE),
                new ShowroomHallProductMapping(missingNarrationProduct.productId(), 2,
                        new BigDecimal("0.5"), BigDecimal.ZERO, new BigDecimal("0.5"), BigDecimal.ONE)));
        publishNarration(ShowroomNarrationTargetType.PRODUCT, missingNarrationProduct.productId(),
                missingNarrationProduct.revisionId(), ShowroomNarrationLanguage.ZH, "缺少英文讲解产品中文讲解", 114L);

        bindDefaultSiteStage();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> publisherService.publishRelease(900L, fixture.publishedAt(), DEFAULT_SITE_KEY, DEFAULT_STAGE));

        assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_PRODUCT_BLOCKED"));
        assertTrue(exception.getMessage().contains("P-104"));
        assertTrue(exception.getMessage().contains("live product EN narration not found"));
    }

    @Test
    void shouldFailFastWhenCompanyNarrationAudioObjectIsMissing() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        when(fileService.getFileContent(12L, "showroom/audio/company-zh.mp3"))
                .thenThrow(new IllegalStateException("source object missing"));

        bindDefaultSiteStage();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> publisherService.publishRelease(900L, fixture.publishedAt(), DEFAULT_SITE_KEY, DEFAULT_STAGE));

        assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_COMPANY_BLOCKED"));
        assertTrue(exception.getMessage().contains("companyId=" + fixture.companyId()));
        assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_SOURCE_MISSING"));
    }

    @Test
    void shouldPublishReleaseWhenProductCoverImageUsesAbsoluteHttpUrl() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        byte[] body = "external-product-cover".getBytes(StandardCharsets.UTF_8);
        server.createContext("/cover.png", exchange -> writeBinary(exchange, body));
        server.start();
        try {
            String coverUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/cover.png";
            mockFile(111L, 12L, "showroom/audio/http-cover-zh.mp3", "audio/mpeg", "http-cover-zh");
            mockFile(112L, 12L, "showroom/audio/http-cover-en.mp3", "audio/mpeg", "http-cover-en");

            ShowroomProductRevision httpCoverProduct = contentService.publishProductRevision(contentService.saveProductDraft(
                    new ShowroomProductDraft(null, "P-103", "球囊扩张导管", "Balloon Dilatation Catheter",
                            Map.of(
                                    "target_market", "冠脉介入",
                                    "target_market_en", "Coronary intervention",
                                    "core_selling_points", "更精准",
                                    "core_selling_points_en", "More precise",
                                    "cover_image", coverUrl))).revisionId(), 904L);

            contentService.replaceHallCanvasLayout(fixture.hallId(), List.of(
                    new ShowroomHallProductMapping(fixture.productId(), 1,
                            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.5"), BigDecimal.ONE),
                    new ShowroomHallProductMapping(httpCoverProduct.productId(), 2,
                            new BigDecimal("0.5"), BigDecimal.ZERO, new BigDecimal("0.5"), BigDecimal.ONE)));
            publishNarration(ShowroomNarrationTargetType.PRODUCT, httpCoverProduct.productId(),
                    httpCoverProduct.revisionId(), ShowroomNarrationLanguage.ZH, "HTTP封面产品中文讲解", 111L);
            publishNarration(ShowroomNarrationTargetType.PRODUCT, httpCoverProduct.productId(),
                    httpCoverProduct.revisionId(), ShowroomNarrationLanguage.EN, "HTTP cover product narration", 112L);

            bindDefaultSiteStage();
            var release = publisherService.publishRelease(900L, fixture.publishedAt(), DEFAULT_SITE_KEY,
                    DEFAULT_STAGE);

            assertNotNull(release);
        } finally {
            server.stop(0);
        }
    }

    private static void writeBinary(HttpExchange exchange, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
