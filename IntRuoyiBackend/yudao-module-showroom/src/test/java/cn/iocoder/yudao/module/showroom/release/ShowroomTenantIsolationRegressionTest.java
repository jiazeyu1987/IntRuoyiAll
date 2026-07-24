package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(ShowroomTenantIsolationRegressionTest.TenantLineTestConfiguration.class)
class ShowroomTenantIsolationRegressionTest extends AbstractShowroomReleaseDbTest {

    private static final Long ADMIN_TENANT_ID = 1L;
    private static final Long TEST_TENANT_ID = 122L;
    private static final String SITE_KEY = "yingtai-showroom";
    private static final String STAGE = "TEST";

    @Test
    void productCurrentHistoryAndHallMappingsShouldBeTenantScoped() {
        var adminProduct = TenantUtils.execute(ADMIN_TENANT_ID, () ->
                seedProductAndHall("product_001", "admin-product", "Admin Product", "hall_01"));
        var testProduct = TenantUtils.execute(TEST_TENANT_ID, () ->
                seedProductAndHall("product_001", "三通旋塞-OFF", "Manifold for Single use-OFF", "hall_01"));

        TenantUtils.execute(ADMIN_TENANT_ID, () -> {
            assertEquals("admin-product",
                    contentService.requireCurrentProductRevision(adminProduct.productId()).nameCn());
            assertEquals(1, contentService.versionAudits("PRODUCT", adminProduct.productId()).stream()
                    .filter(audit -> audit.newValueJson().contains("admin-product"))
                    .count());
            assertFalse(contentService.listProducts().stream()
                    .anyMatch(product -> product.productId().equals(testProduct.productId())));
            List<ShowroomHall> halls = contentService.listHalls();
            assertEquals(1, halls.size());
            assertEquals("hall_01", halls.get(0).hallCode());
            assertEquals(List.of(new ShowroomHallProductMapping(adminProduct.productId(), 1)),
                    halls.get(0).productMappings());
        });
    }

    @Test
    void testReleaseShouldMaterializeOnlyBoundTenantContent() throws Exception {
        var adminProduct = TenantUtils.execute(ADMIN_TENANT_ID, () ->
                seedProductAndHall("product_001", "admin-product", "Admin Product", "hall_01"));
        TenantUtils.execute(TEST_TENANT_ID, () -> {
            try {
                return seedReleaseReadyContent("product_001", "三通旋塞-OFF",
                        "Manifold for Single use-OFF", "hall_01");
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        bindSiteStage(TEST_TENANT_ID);

        ShowroomMaterializedRelease release = TenantUtils.execute(TEST_TENANT_ID, () ->
                publisherService.publishRelease(900L, Instant.parse("2026-05-27T01:00:00Z"), SITE_KEY, STAGE));

        String publicDocument = manifestQueryService.queryDocumentJson(
                new ShowroomReleaseScope(TEST_TENANT_ID, SITE_KEY, STAGE),
                release.releaseId(), ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX);
        assertTrue(publicDocument.contains("三通旋塞-OFF"));
        assertFalse(publicDocument.contains("admin-product"));

        TenantUtils.execute(ADMIN_TENANT_ID, () ->
                assertEquals("admin-product",
                        contentService.requireCurrentProductRevision(adminProduct.productId()).nameCn()));
    }

    @Test
    void tenantContextIsRequiredForTenantManagedShowroomContent() {
        TenantContextHolder.clear();
        try {
            contentService.listProducts();
        } catch (RuntimeException exception) {
            assertTrue(exception.getMessage().contains("TenantContextHolder")
                    || exception.getCause() != null
                    && String.valueOf(exception.getCause().getMessage()).contains("TenantContextHolder"));
            return;
        }
        throw new AssertionError("tenant-scoped showroom content query must fail without tenant context");
    }

    private SeededProduct seedProductAndHall(String productCode, String nameCn, String nameEn, String hallCode) {
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, productCode, nameCn, nameEn, Map.of(
                "owner_company_id", "1",
                "product_owner_type", "YINGTAI",
                "lifecycle_stage", "REGISTERED",
                "cover_image", "/admin-api/infra/file/11/get/showroom/product/product.png"))).revisionId(), 902L);
        ShowroomHall hall = contentService.createHall(hallCode, "测试展柜", "Test Hall", "展柜简介", "Hall summary");
        contentService.replaceHallProductMappings(hall.hallId(), List.of(new ShowroomHallProductMapping(
                product.productId(), 1)));
        return new SeededProduct(product.productId());
    }

    private SeededProduct seedReleaseReadyContent(String productCode, String nameCn, String nameEn,
                                                  String hallCode) throws Exception {
        mockFile(101L, 11L, "showroom/company/company.png", "image/png", "company-home");
        mockFile(102L, 11L, "showroom/hall/hall.png", "image/png", "hall-preview");
        mockFile(103L, 11L, "showroom/product/product.png", "image/png", "product-cover");
        mockFile(104L, 12L, "showroom/audio/company-zh.mp3", "audio/mpeg", "company-audio-zh");
        mockFile(105L, 12L, "showroom/audio/company-en.mp3", "audio/mpeg", "company-audio-en");
        mockFile(106L, 12L, "showroom/audio/product-zh.mp3", "audio/mpeg", "product-audio-zh");
        mockFile(107L, 12L, "showroom/audio/product-en.mp3", "audio/mpeg", "product-audio-en");
        mockFile(108L, 12L, "showroom/audio/hall-zh.mp3", "audio/mpeg", "hall-audio-zh");
        mockFile(109L, 12L, "showroom/audio/hall-en.mp3", "audio/mpeg", "hall-audio-en");

        var company = contentService.publishCompanyRevision(contentService.saveCompanyDraft(
                new ShowroomCompanyDraft(null, "MAIN", "盈泰医疗", "Yingtai Medical", Map.of(
                        "development_history", "发展历程",
                        "development_history_en", "History",
                        "park_introduction", "园区介绍",
                        "park_introduction_en", "Park",
                        "cover_image", "/admin-api/infra/file/11/get/showroom/company/company.png")))
                .revisionId(), 901L);
        var product = contentService.publishProductRevision(contentService.saveProductDraft(new ShowroomProductDraft(
                null, productCode, nameCn, nameEn, Map.of(
                "owner_company_id", String.valueOf(company.companyId()),
                "product_owner_type", "YINGTAI",
                "lifecycle_stage", "REGISTERED",
                "target_market", "冠脉介入",
                "target_market_en", "Coronary intervention",
                "core_selling_points", "更顺滑",
                "core_selling_points_en", "Smoother",
                "cover_image", "/admin-api/infra/file/11/get/showroom/product/product.png"))).revisionId(), 902L);
        ShowroomHall hall = contentService.createHall(hallCode, "心内介入展厅", "Cardiology Hall", "展厅简介", "Hall summary");
        contentService.replaceHallCanvasLayout(hall.hallId(), List.of(new ShowroomHallProductMapping(
                product.productId(), 1, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ONE, java.math.BigDecimal.ONE)));

        publishPreviewAsset(ShowroomPreviewAssetTargetType.COMPANY, company.companyId(), company.revisionId(), 101L);
        publishPreviewAsset(ShowroomPreviewAssetTargetType.HALL, hall.hallId(), hall.hallId(), 102L);
        publishPreviewAsset(ShowroomPreviewAssetTargetType.PRODUCT, product.productId(), product.revisionId(), 103L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.ZH, "公司中文讲解", 104L);
        publishNarration(ShowroomNarrationTargetType.COMPANY, company.companyId(), company.revisionId(),
                ShowroomNarrationLanguage.EN, "English company narration", 105L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, product.productId(), product.revisionId(),
                ShowroomNarrationLanguage.ZH, "产品中文讲解", 106L);
        publishNarration(ShowroomNarrationTargetType.PRODUCT, product.productId(), product.revisionId(),
                ShowroomNarrationLanguage.EN, "English product narration", 107L);
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.ZH, "展厅中文讲解", 108L);
        publishNarration(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationLanguage.EN, "English hall narration", 109L);
        return new SeededProduct(product.productId());
    }

    private void bindSiteStage(Long tenantId) {
        siteBindingMapper.insert(ShowroomPublicSiteBindingDO.builder()
                .siteKey(SITE_KEY)
                .stage(STAGE)
                .tenantId(tenantId)
                .displayName("Yingtai TEST")
                .enabled(true)
                .build());
    }

    private record SeededProduct(Long productId) {
    }

    @TestConfiguration
    static class TenantLineTestConfiguration {

        @Bean
        TenantProperties tenantProperties() {
            return new TenantProperties();
        }

        @Bean
        TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties properties,
                                                              MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor inner = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(properties));
            MyBatisUtils.addInterceptor(interceptor, inner, 0);
            return inner;
        }
    }
}
