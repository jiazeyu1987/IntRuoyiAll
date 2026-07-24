package cn.iocoder.yudao.module.showroom.content;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomContentService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomHallMixedItemContentTest {

    private final ShowroomContentService contentService = new ShowroomContentService();

    @Test
    void awardPublishShouldBindSplitCompanyHonorHallsAndKeepNormalHallProductOnly() {
        Long hallId = contentService.createHall("mixed", "混合展柜", "Mixed Hall", "混合展柜描述",
                "Mixed hall description").hallId();
        Long productId = contentService.saveProductDraft(new ShowroomProductDraft(null, "P-MIXED-001",
                "产品一", "Product One", Map.of("product_owner_type", "YINGTAI"))).productId();
        Long awardId = contentService.saveAwardDraft(new ShowroomAwardDraft(null, "AWARD-001",
                "创新奖", "", "中文讲解", "", "颁发单位", "2026", "/cover/award.png")).awardId();
        Long awardRevisionId = contentService.getLatestAwardRevision(awardId).revisionId();
        Long secondAwardId = contentService.saveAwardDraft(new ShowroomAwardDraft(null, "AWARD-002",
                "质量奖", "", "中文讲解", "", "颁发单位", "2026", "/cover/award-2.png")).awardId();
        Long secondAwardRevisionId = contentService.getLatestAwardRevision(secondAwardId).revisionId();

        contentService.replaceHallItemMappings(hallId, List.of(
                new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_PRODUCT, productId, 10)));
        contentService.publishAwardRevision(awardRevisionId, 901L);
        contentService.publishAwardRevision(secondAwardRevisionId, 901L);

        assertEquals(List.of(ShowroomHallItemMapping.TYPE_PRODUCT),
                contentService.getHall(hallId).itemMappings().stream()
                        .map(ShowroomHallItemMapping::itemType)
                        .toList());
        ShowroomHallItemMapping firstHonorAward = contentService.listHalls().stream()
                .filter(hall -> "hall_09".equals(hall.hallCode()))
                .findFirst()
                .orElseThrow()
                .itemMappings()
                .get(0);
        ShowroomHallItemMapping secondHonorAward = contentService.listHalls().stream()
                .filter(hall -> "hall_10".equals(hall.hallCode()))
                .findFirst()
                .orElseThrow()
                .itemMappings()
                .get(0);
        assertEquals(ShowroomHallItemMapping.TYPE_AWARD, firstHonorAward.itemType());
        assertEquals(awardId, firstHonorAward.itemId());
        assertEquals(ShowroomHallItemMapping.TYPE_AWARD, secondHonorAward.itemType());
        assertEquals(secondAwardId, secondHonorAward.itemId());
        assertTrue(firstHonorAward.hasCompleteLayout());
        assertTrue(secondHonorAward.hasCompleteLayout());
    }

    @Test
    void normalHallShouldRejectAwardMappings() {
        Long hallId = contentService.createHall("mixed-reject", "普通展柜", "Normal Hall", "描述",
                "Description").hallId();
        Long awardId = contentService.saveAwardDraft(new ShowroomAwardDraft(null, "AWARD-REJECT-001",
                "创新奖", "", "中文讲解", "", "颁发单位", "2026", "/cover/award.png")).awardId();

        IllegalStateException forbidden = assertThrows(IllegalStateException.class,
                () -> contentService.replaceHallItemMappings(hallId, List.of(
                        new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_AWARD, awardId, 1))));

        assertTrue(forbidden.getMessage().contains("SHOWROOM_AWARD_HALL_FORBIDDEN"));
    }

    @Test
    void hallShouldRejectDuplicateMixedItemByTypeAndId() {
        Long awardId = contentService.saveAwardDraft(new ShowroomAwardDraft(null, "AWARD-002",
                "质量奖", "", "", "", "颁发单位", "2026", "/cover/award-2.png")).awardId();
        Long awardRevisionId = contentService.getLatestAwardRevision(awardId).revisionId();
        contentService.publishAwardRevision(awardRevisionId, 901L);
        Long hallId = contentService.listHalls().stream()
                .filter(hall -> "hall_09".equals(hall.hallCode()))
                .findFirst()
                .orElseThrow()
                .hallId();

        IllegalStateException duplicate = assertThrows(IllegalStateException.class,
                () -> contentService.replaceHallItemMappings(hallId, List.of(
                        new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_AWARD, awardId, 1),
                        new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_AWARD, awardId, 2))));

        assertTrue(duplicate.getMessage().contains("SHOWROOM_DUPLICATE_ITEM"));
    }
}
