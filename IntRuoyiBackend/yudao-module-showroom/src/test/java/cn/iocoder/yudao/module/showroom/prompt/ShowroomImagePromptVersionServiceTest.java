package cn.iocoder.yudao.module.showroom.prompt;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.showroom.dal.dataobject.prompt.ShowroomImagePromptVersionDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.prompt.ShowroomImagePromptVersionMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(ShowroomImagePromptVersionService.class)
class ShowroomImagePromptVersionServiceTest extends BaseDbUnitTest {

    @Resource
    private ShowroomImagePromptVersionService service;
    @Resource
    private ShowroomImagePromptVersionMapper mapper;

    @Test
    void saveNewVersionShouldCreateIncrementedCurrentAndHistory() {
        ShowroomImagePromptVersion version1 = service.saveNewVersion(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER,
                "主体是“{{product_name_cn}}”",
                "V1"
        );
        ShowroomImagePromptVersion version2 = service.saveNewVersion(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER,
                "主体是“{{product_name_cn}}”，英文名参考“{{product_name_en}}”",
                "V2"
        );

        ShowroomImagePromptVersion current = service.requireCurrent(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER);
        List<ShowroomImagePromptVersion> history = service.history(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER);

        assertEquals(1, version1.versionNo());
        assertEquals(2, version2.versionNo());
        assertEquals(version2.id(), current.id());
        assertEquals(List.of("product_name_cn", "product_name_en"), current.placeholderCodes());
        assertEquals(2, history.size());
        assertEquals(version2.id(), history.get(0).id());
        assertEquals(version1.id(), history.get(1).id());
    }

    @Test
    void requireCurrentShouldSeedDefaultAwardCoverPromptWhenMissing() {
        ShowroomImagePromptVersion current = service.requireCurrent(
                ShowroomImagePromptVersionService.SCENE_AWARD_COVER);

        assertEquals(1, current.versionNo());
        assertEquals(ShowroomImagePromptVersionService.SCENE_AWARD_COVER, current.sceneCode());
        assertTrue(current.templateText().contains("{{award_name_cn}}"));
        assertTrue(current.templateText().contains("centered subject"));
        assertEquals(List.of(
                "award_name_cn",
                "award_name_en",
                "award_issuer",
                "award_date_text",
                "award_description_zh"
        ), current.placeholderCodes());
    }

    @Test
    void saveNewVersionShouldFailWhenTemplateBlankOrPlaceholderInvalid() {
        IllegalStateException blank = assertThrows(IllegalStateException.class,
                () -> service.saveNewVersion(
                        ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER,
                        "   ",
                        "blank"));
        assertTrue(blank.getMessage().contains("SHOWROOM_IMAGE_PROMPT_TEMPLATE_INVALID"));

        IllegalStateException unsupported = assertThrows(IllegalStateException.class,
                () -> service.saveNewVersion(
                        ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER,
                        "主体是“{{product_name_cn}}”，卖点是“{{core_selling_points}}”",
                        "unsupported"));
        assertTrue(unsupported.getMessage().contains("SHOWROOM_IMAGE_PROMPT_PLACEHOLDER_UNSUPPORTED"));

        IllegalStateException missingRequired = assertThrows(IllegalStateException.class,
                () -> service.saveNewVersion(
                        ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER,
                        "这是一段没有产品名占位符的模板",
                        "missing"));
        assertTrue(missingRequired.getMessage().contains("SHOWROOM_IMAGE_PROMPT_PLACEHOLDER_REQUIRED"));
    }

    @Test
    void renderProductCoverPromptShouldReplacePlaceholdersAndRecordUsage() {
        ShowroomImagePromptVersion version = service.saveNewVersion(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER,
                "主体是“{{product_name_cn}}”，英文名参考“{{product_name_en}}”",
                "render"
        );

        String rendered = service.renderProductCoverPrompt(version.id(), "冠脉导丝", "Coronary Guidewire");
        service.recordUsage(version.id());
        ShowroomImagePromptVersion current = service.requireCurrent(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER);

        assertEquals("主体是“冠脉导丝”，英文名参考“Coronary Guidewire”", rendered);
        assertEquals(1, current.useCount());
        assertTrue(current.lastUsedAt() != null);
    }

    @Test
    void requireCurrentAndHistoryShouldRepairStoredUtf8Mojibake() {
        String correctTemplate = ShowroomImagePromptVersionService.DEFAULT_PRODUCT_COVER_TEMPLATE;
        String mojibakeTemplate = new String(correctTemplate.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.ISO_8859_1);
        mapper.insert(ShowroomImagePromptVersionDO.builder()
                .sceneCode(ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER)
                .versionNo(1)
                .templateText(mojibakeTemplate)
                .changeNote("seed")
                .placeholderCodesJson("[\"product_name_cn\",\"product_name_en\"]")
                .useCount(0)
                .build());

        ShowroomImagePromptVersion current = service.requireCurrent(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER);
        String rendered = service.renderProductCoverPrompt(current.id(), "冠脉导丝", "Coronary Guidewire");

        assertTrue(current.templateText().startsWith("生成一张横向医疗器械产品展示图"));
        assertTrue(rendered.contains("主体是“冠脉导丝”"));
        assertTrue(service.history(ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER).get(0).templateText()
                .contains("最终只返回一个本地 PNG 绝对路径"));
    }

    @Test
    void requireCurrentAndHistoryShouldRepairStoredWindows1252Utf8Mojibake() {
        String correctTemplate = ShowroomImagePromptVersionService.DEFAULT_PRODUCT_COVER_TEMPLATE;
        String mojibakeTemplate = toHybridWindowsMojibake(correctTemplate);
        mapper.insert(ShowroomImagePromptVersionDO.builder()
                .sceneCode(ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER)
                .versionNo(1)
                .templateText(mojibakeTemplate)
                .changeNote("seed")
                .placeholderCodesJson("[\"product_name_cn\",\"product_name_en\"]")
                .useCount(0)
                .build());

        ShowroomImagePromptVersion current = service.requireCurrent(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER);
        String rendered = service.renderProductCoverPrompt(current.id(), "冠脉导丝", "Coronary Guidewire");

        assertTrue(current.templateText().startsWith("生成一张横向医疗器械产品展示图"));
        assertFalse(current.templateText().contains("�"));
        assertFalse(current.templateText().contains("?"));
        assertTrue(rendered.contains("主体是“冠脉导丝”"));
        assertTrue(service.history(ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER).get(0).templateText()
                .contains("最终只返回一个本地 PNG 绝对路径"));
    }

    @Test
    void requireCurrentShouldRepairCorruptedLinesAndPreserveHealthyLines() {
        String correctTemplate = ShowroomImagePromptVersionService.DEFAULT_PRODUCT_COVER_TEMPLATE;
        String mixedTemplate = toHybridWindowsMojibake(correctTemplate)
                + "\n补充要求：继续保持医疗器械主体居中，并保留足够留白。";
        mapper.insert(ShowroomImagePromptVersionDO.builder()
                .sceneCode(ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER)
                .versionNo(1)
                .templateText(mixedTemplate)
                .changeNote("mixed")
                .placeholderCodesJson("[\"product_name_cn\",\"product_name_en\"]")
                .useCount(0)
                .build());

        ShowroomImagePromptVersion current = service.requireCurrent(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER);

        assertTrue(current.templateText().startsWith("生成一张横向医疗器械产品展示图"));
        assertTrue(current.templateText().contains("补充要求：继续保持医疗器械主体居中，并保留足够留白。"));
        assertFalse(current.templateText().contains("�"));
        assertFalse(current.templateText().contains("?"));
    }

    private static String toHybridWindowsMojibake(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder(bytes.length);
        for (byte raw : bytes) {
            int current = raw & 0xFF;
            builder.append(switch (current) {
                case 0x80 -> '€';
                case 0x82 -> '‚';
                case 0x83 -> 'ƒ';
                case 0x84 -> '„';
                case 0x85 -> '…';
                case 0x86 -> '†';
                case 0x87 -> '‡';
                case 0x88 -> 'ˆ';
                case 0x89 -> '‰';
                case 0x8A -> 'Š';
                case 0x8B -> '‹';
                case 0x8C -> 'Œ';
                case 0x8E -> 'Ž';
                case 0x91 -> '‘';
                case 0x92 -> '’';
                case 0x93 -> '“';
                case 0x94 -> '”';
                case 0x95 -> '•';
                case 0x96 -> '–';
                case 0x97 -> '—';
                case 0x98 -> '˜';
                case 0x99 -> '™';
                case 0x9A -> 'š';
                case 0x9B -> '›';
                case 0x9C -> 'œ';
                case 0x9E -> 'ž';
                case 0x9F -> 'Ÿ';
                default -> (char) current;
            });
        }
        return builder.toString();
    }
}
