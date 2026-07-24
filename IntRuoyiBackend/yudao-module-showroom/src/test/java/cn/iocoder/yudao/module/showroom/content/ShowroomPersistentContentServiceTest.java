package cn.iocoder.yudao.module.showroom.content;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseAutoPublishService;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomCompanyRevisionMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@Import(ShowroomPersistentContentService.class)
class ShowroomPersistentContentServiceTest extends BaseDbUnitTest {

    @Resource
    private ShowroomPersistentContentService contentService;

    @Resource
    private ShowroomCompanyRevisionMapper companyRevisionMapper;

    @MockBean
    private ShowroomReleaseAutoPublishService releaseAutoPublishService;

    @Test
    void companyDraftShouldPersistAndPublishToCurrentRevision() {
        assertTrue(contentService.findCurrentOrLatestCompanyRevision().isEmpty());

        var draft = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "瑛泰展厅", "Yingtai Showroom", Map.of("development_history", "草稿发展史")));

        var latestDraft = contentService.findCurrentOrLatestCompanyRevision().orElseThrow();
        assertEquals(draft.revisionId(), latestDraft.revisionId());
        assertEquals("DRAFT", latestDraft.status());

        var published = contentService.publishCompanyRevision(draft.revisionId(), 901L);
        assertEquals(published.revisionId(), contentService.requireCurrentCompanyRevision().revisionId());
        assertFalse(contentService.versionAudits("COMPANY", published.companyId()).isEmpty());
        assertEquals("草稿发展史", JsonUtils.parseObject(
                contentService.versionAudits("COMPANY", published.companyId()).get(0).newValueJson(),
                Map.class).get("value"));
    }

    @Test
    void companyDraftShouldPersistOptionalCoverImageField() {
        var draft = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "盈泰展厅", "Yingtai Showroom",
                Map.of("cover_image", "/admin-api/infra/file/28/get/showroom/company/cover.png")));

        var revision = contentService.findCurrentOrLatestCompanyRevision().orElseThrow();
        assertEquals(draft.revisionId(), revision.revisionId());
        assertEquals("/admin-api/infra/file/28/get/showroom/company/cover.png",
                revision.fields().get("cover_image"));
    }

    @Test
    void companyDraftShouldPersistEnglishFieldVariants() {
        var draft = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "盈泰展厅", "Yingtai Showroom",
                Map.ofEntries(
                        Map.entry("development_history", "公司发展历程"),
                        Map.entry("development_history_en", "Company development history"),
                        Map.entry("park_introduction", "园区介绍中文"),
                        Map.entry("park_introduction_en", "Park introduction in English"),
                        Map.entry("incubation_platform", "孵化平台中文"),
                        Map.entry("incubation_platform_en", "Incubation platform in English"),
                        Map.entry("subsidiary_overview", "子公司概览中文"),
                        Map.entry("subsidiary_overview_en", "Subsidiary overview in English"),
                        Map.entry("stock_info", "上市信息中文"),
                        Map.entry("stock_info_en", "Listing information in English"),
                        Map.entry("core_manufacturing_capability", "核心制造能力中文"),
                        Map.entry("core_manufacturing_capability_en", "Core manufacturing capability in English"),
                        Map.entry("honors_awards", "荣誉资质中文"),
                        Map.entry("honors_awards_en", "Honors and awards in English"))));

        var revision = contentService.findCurrentOrLatestCompanyRevision().orElseThrow();
        assertEquals(draft.revisionId(), revision.revisionId());
        assertEquals("Company development history", revision.fields().get("development_history_en"));
        assertEquals("Park introduction in English", revision.fields().get("park_introduction_en"));
        assertEquals("Incubation platform in English", revision.fields().get("incubation_platform_en"));
        assertEquals("Subsidiary overview in English", revision.fields().get("subsidiary_overview_en"));
        assertEquals("Listing information in English", revision.fields().get("stock_info_en"));
        assertEquals("Core manufacturing capability in English",
                revision.fields().get("core_manufacturing_capability_en"));
        assertEquals("Honors and awards in English", revision.fields().get("honors_awards_en"));
    }

    @Test
    void companyDraftShouldPersistDisplaySnapshotColumns() {
        var draft = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "盈泰展厅快照", "Yingtai Snapshot", Map.of("development_history", "快照内容")));

        var revision = companyRevisionMapper.selectById(draft.revisionId());
        assertEquals("盈泰展厅快照", revision.getDisplayNameSnapshot());
        assertEquals("Yingtai Snapshot", revision.getDisplayNameEnSnapshot());
        assertEquals("MAIN", revision.getCompanyTypeSnapshot());
    }

    @Test
    void productDraftShouldPersistLatestRevisionAndAllowBlankChineseNameBeforePublish() {
        var incomplete = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-001", "", null, Map.of("target_market", "public")));

        assertTrue(contentService.getProduct(incomplete.productId()).incomplete());
        assertEquals(incomplete.revisionId(),
                contentService.getCurrentOrLatestProductRevision(incomplete.productId()).revisionId());
        IllegalStateException missingName = assertThrows(IllegalStateException.class,
                () -> contentService.publishProductRevision(incomplete.revisionId(), 902L));
        assertTrue(missingName.getMessage().contains("SHOWROOM_REQUIRED_FIELD_MISSING"));

        var complete = contentService.saveProductDraft(new ShowroomProductDraft(
                incomplete.productId(), "P-001", "", "Introducer Sheath Set",
                Map.of("target_market", "public", "registration_certificate", "注册证信息")));
        var published = contentService.publishProductRevision(complete.revisionId(), 902L);

        assertEquals(published.revisionId(),
                contentService.requireCurrentProductRevision(complete.productId()).revisionId());
        assertEquals("", published.nameCn());
        assertTrue(published.incomplete());
        assertTrue(contentService.versionAudits("PRODUCT", complete.productId()).stream()
                .anyMatch(audit -> "name_cn".equals(audit.fieldCode())));
        assertTrue(contentService.versionAudits("PRODUCT", complete.productId()).stream()
                .anyMatch(audit -> "registration_certificate".equals(audit.fieldCode())));
        verify(releaseAutoPublishService).markDirty("PRODUCT_REVISION_PUBLISHED", 902L);
    }

    @Test
    void productDraftShouldPersistOptionalCoverImageField() {
        var draft = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-COVER-001", "封面产品", "Covered Product",
                Map.of("cover_image", "/admin-api/infra/file/28/get/showroom/product/cover.png")));

        var revision = contentService.getCurrentOrLatestProductRevision(draft.productId());
        assertEquals("/admin-api/infra/file/28/get/showroom/product/cover.png",
                revision.fields().get("cover_image"));
    }

    @Test
    void productDraftShouldPersistEnglishFieldVariants() {
        var draft = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-BI-001", "双语产品", "Bilingual Product",
                Map.ofEntries(
                        Map.entry("target_market", "冠脉市场"),
                        Map.entry("target_market_en", "Coronary market"),
                        Map.entry("pipeline_layout", "结构布局中文"),
                        Map.entry("pipeline_layout_en", "Pipeline layout in English"),
                        Map.entry("indication_content", "适应症中文"),
                        Map.entry("indication_content_en", "Indication in English"),
                        Map.entry("core_selling_points", "卖点中文"),
                        Map.entry("core_selling_points_en", "Selling points in English"),
                        Map.entry("model_specification", "型号中文"),
                        Map.entry("model_specification_en", "Model specification in English"),
                        Map.entry("registration_certificate", "注册证中文"),
                        Map.entry("registration_certificate_en", "Registration certificate in English"),
                        Map.entry("clinical_effect", "临床效果中文"),
                        Map.entry("clinical_effect_en", "Clinical effect in English"),
                        Map.entry("fim_status", "FIM中文"),
                        Map.entry("fim_status_en", "FIM in English"))));

        var revision = contentService.getCurrentOrLatestProductRevision(draft.productId());
        assertEquals("Coronary market", revision.fields().get("target_market_en"));
        assertEquals("Pipeline layout in English", revision.fields().get("pipeline_layout_en"));
        assertEquals("Indication in English", revision.fields().get("indication_content_en"));
        assertEquals("Selling points in English", revision.fields().get("core_selling_points_en"));
        assertEquals("Model specification in English", revision.fields().get("model_specification_en"));
        assertEquals("Registration certificate in English", revision.fields().get("registration_certificate_en"));
        assertEquals("Clinical effect in English", revision.fields().get("clinical_effect_en"));
        assertEquals("FIM in English", revision.fields().get("fim_status_en"));
    }

    @Test
    void productPageShouldOrderRowsByLegacyProductCodeNaturally() {
        var product020 = contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "YT-LEGACY-020", "Legacy Sort 20", "Legacy Sort 20",
                "product_020", Map.of("target_market", "legacy sort market")));
        var product003 = contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "YT-LEGACY-003", "Legacy Sort 3", "Legacy Sort 3",
                "product_003", Map.of("target_market", "legacy sort market")));
        var product010 = contentService.saveProductDraft(new ShowroomProductDraft(
                null, null, "YT-LEGACY-010", "Legacy Sort 10", "Legacy Sort 10",
                "product_010", Map.of("target_market", "legacy sort market")));
        var productWithoutLegacy = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "YT-LEGACY-BLANK", "Legacy Sort Blank", "Legacy Sort Blank", Map.of()));

        var page = contentService.pageProducts(1, 20);

        assertEquals(List.of(product003.productId(), product010.productId(), product020.productId(),
                        productWithoutLegacy.productId()),
                page.getList().stream().map(row -> row.productId()).toList());
        assertEquals(List.of("product_003", "product_010", "product_020"),
                page.getList().subList(0, 3).stream().map(row -> row.legacyProductCode()).toList());
        assertEquals(null, page.getList().get(3).legacyProductCode());
    }

    @Test
    void hallMappingsShouldPersistInDisplayOrder() {
        var productA = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-101", "产品A", "Product A", Map.of()));
        var productB = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-102", "产品B", "Product B", Map.of()));

        var hall = contentService.createHall("hall_01", "心内介植入展厅", "Cardiology Implant Hall", "", "");
        IllegalStateException emptyMappings = assertThrows(IllegalStateException.class,
                () -> contentService.replaceHallProductMappings(hall.hallId(), List.of()));
        assertTrue(emptyMappings.getMessage().contains("SHOWROOM_REQUIRED_FIELD_MISSING"));

        contentService.replaceHallProductMappings(hall.hallId(), List.of(
                new ShowroomHallProductMapping(productB.productId(), 2),
                new ShowroomHallProductMapping(productA.productId(), 1)
        ));

        var persisted = contentService.getHall(hall.hallId());
        assertEquals(List.of(productA.productId(), productB.productId()),
                persisted.productMappings().stream().map(ShowroomHallProductMapping::productId).toList());
        assertEquals(1, contentService.listHalls().size());
    }

    @Test
    void hallCanvasLayoutShouldPersistCoordinatesInDatabase() {
        var productA = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-CANVAS-101", "画布产品A", "Canvas Product A", Map.of()));
        var productB = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-CANVAS-102", "画布产品B", "Canvas Product B", Map.of()));

        var hall = contentService.createHall("hall_canvas", "画布展柜", "Canvas Hall", "", "");
        contentService.replaceHallCanvasLayout(hall.hallId(), List.of(
                new ShowroomHallProductMapping(productA.productId(), 1,
                        BigDecimal.ZERO, BigDecimal.ZERO, bd("0.5"), BigDecimal.ONE),
                new ShowroomHallProductMapping(productB.productId(), 2,
                        bd("0.5"), BigDecimal.ZERO, bd("0.5"), BigDecimal.ONE)));

        var persisted = contentService.getHall(hall.hallId()).productMappings();
        assertEquals(BigDecimal.ZERO, persisted.get(0).layoutX());
        assertEquals(bd("0.5"), persisted.get(1).layoutX());
        assertEquals(BigDecimal.ONE, persisted.get(1).layoutHeight());
    }

    @Test
    void hallMappingsShouldAllowSavingTheSameProductSetTwice() {
        var productA = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-201", "产品C", "Product C", Map.of()));
        var productB = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "P-202", "产品D", "Product D", Map.of()));

        var hall = contentService.createHall("hall_02", "反复保存展厅", "Repeat Save Hall", "", "");
        var mappings = List.of(
                new ShowroomHallProductMapping(productA.productId(), 1),
                new ShowroomHallProductMapping(productB.productId(), 2)
        );

        contentService.replaceHallProductMappings(hall.hallId(), mappings);
        var persisted = contentService.replaceHallProductMappings(hall.hallId(), mappings);

        assertEquals(List.of(productA.productId(), productB.productId()),
                persisted.productMappings().stream().map(ShowroomHallProductMapping::productId).toList());
    }

    @Test
    void awardPublishShouldEnsureSplitCompanyHonorHallsAndBindAwardsByHalf() {
        var firstDraft = contentService.saveAwardDraft(new ShowroomAwardDraft(
                null, "AWARD-HONOR-001", "公司荣誉奖", "Company Honor Award",
                "中文讲解", "English narration", "颁发单位", "2026",
                "/admin-api/infra/file/28/get/showroom/award/honor.png"));
        var secondDraft = contentService.saveAwardDraft(new ShowroomAwardDraft(
                null, "AWARD-HONOR-002", "质量荣誉奖", "Quality Honor Award",
                "中文讲解", "English narration", "颁发单位", "2026",
                "/admin-api/infra/file/28/get/showroom/award/honor-2.png"));

        contentService.publishAwardRevision(firstDraft.revisionId(), 901L);
        contentService.publishAwardRevision(secondDraft.revisionId(), 902L);

        var firstHonorHall = contentService.listHalls().stream()
                .filter(hall -> "hall_09".equals(hall.hallCode()))
                .findFirst()
                .orElseThrow();
        var secondHonorHall = contentService.listHalls().stream()
                .filter(hall -> "hall_10".equals(hall.hallCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("企业荣誉展柜1", firstHonorHall.name());
        assertEquals("Corporate Honors Showcase 1", firstHonorHall.nameEn());
        assertEquals("企业荣誉展柜2", secondHonorHall.name());
        assertEquals("Corporate Honors Showcase 2", secondHonorHall.nameEn());
        assertEquals(List.of(firstDraft.awardId()), firstHonorHall.itemMappings().stream()
                .filter(mapping -> ShowroomHallItemMapping.TYPE_AWARD.equals(mapping.itemType()))
                .map(ShowroomHallItemMapping::itemId)
                .toList());
        assertEquals(List.of(secondDraft.awardId()), secondHonorHall.itemMappings().stream()
                .filter(mapping -> ShowroomHallItemMapping.TYPE_AWARD.equals(mapping.itemType()))
                .map(ShowroomHallItemMapping::itemId)
                .toList());
        assertTrue(firstHonorHall.itemMappings().get(0).hasCompleteLayout());
        assertTrue(secondHonorHall.itemMappings().get(0).hasCompleteLayout());
        assertTrue(contentService.listHalls().stream()
                .noneMatch(hall -> "company_honor".equals(hall.hallCode())));
        verify(releaseAutoPublishService).markDirty("AWARD_REVISION_PUBLISHED", 902L);
    }

    @Test
    void nonHonorHallShouldRejectAwardMappings() {
        var award = contentService.saveAwardDraft(new ShowroomAwardDraft(
                null, "AWARD-HONOR-002", "质量荣誉奖", "Quality Honor Award",
                "中文讲解", "English narration", "颁发单位", "2026",
                "/admin-api/infra/file/28/get/showroom/award/honor-2.png"));
        contentService.publishAwardRevision(award.revisionId(), 902L);
        var normalHall = contentService.createHall("normal-hall", "普通展柜", "Normal Hall", "", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> contentService.replaceHallItemMappings(normalHall.hallId(), List.of(
                        new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_AWARD, award.awardId(), 1))));

        assertTrue(exception.getMessage().contains("SHOWROOM_AWARD_HALL_FORBIDDEN"));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

}
