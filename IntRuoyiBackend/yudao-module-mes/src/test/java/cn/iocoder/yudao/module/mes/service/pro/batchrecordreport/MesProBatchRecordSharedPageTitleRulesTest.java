package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordSharedPageTitleRulesTest {

    @Test
    void isSharedPageTitleRow_acceptsSingleLineShortInfoProcessRecordAndSummaryPages() {
        assertTrue(MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(row(
                "\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f")));
        assertTrue(MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(row(
                "\u7cbe\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55")));
        assertTrue(MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(row(
                "\u751f\u4ea7\u8bb0\u5f55\u6c47\u603b\u8868")));
    }

    @Test
    void isSharedPageTitleRow_rejectsDocumentHeaderAndFieldLabelRows() {
        assertFalse(MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(row(
                "\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5\u751f\u4ea7\u8bb0\u5f55",
                "\u8bb0\u5f55\u7f16\u53f7",
                "RE-PP-ID-01")));
        assertFalse(MesProBatchRecordSharedPageTitleRules.isSharedPageTitleRow(row(
                "\u68c0\u67e5\u8981\u6c42",
                "\u7ed3\u679c",
                "\u64cd\u4f5c\u4eba/\u65e5\u671f",
                "\u590d\u6838\u4eba/\u65e5\u671f")));
    }

    @Test
    void detectTitleType_distinguishesSummaryProcessRecordAndOtherShortTitlePages() {
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.INFORMATION_SUMMARY,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row("\u751f\u4ea7\u8bb0\u5f55\u6c47\u603b\u8868")));
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row("\u7cbe\u6d17\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55")));
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.OTHER_SHORT_TITLE,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f")));
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.NONE,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row(
                        "\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5\u751f\u4ea7\u8bb0\u5f55",
                        "\u8bb0\u5f55\u7f16\u53f7",
                        "RE-PP-ID-01")));
    }

    @Test
    void detectTitleType_rejectsInternalSectionTitlesThatLookLikeSingleLineShortHeaders() {
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.NONE,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row("\u751f\u4ea7\u524d\u68c0\u67e5\u8bb0\u5f55")));
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.NONE,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row("\u751f\u4ea7\u540e\u6e05\u573a\u8bb0\u5f55")));
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.NONE,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row("\u7c97\u6d17\u751f\u4ea7\u64cd\u4f5c\u53ca\u81ea\u68c0\u8bb0\u5f55")));
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.NONE,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row("\u751f\u4ea7\u6279\u91cf\u6c47\u603b")));
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.NONE,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row("\u8fc7\u7a0b\u653e\u884c\u4fe1\u606f")));
        assertEquals(MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.NONE,
                MesProBatchRecordSharedPageTitleRules.detectTitleType(row("\u914d\u4ef6\u8fdb\u8d27\u6279\u53f7\u4fe1\u606f")));
    }

    @Test
    void normalizeSharedTitle_trimsChecklistSuffixFromGenericProcessTitle() {
        assertEquals("\u706d\u83cc\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55",
                MesProBatchRecordSharedPageTitleRules.normalizeSharedTitle(
                        "\u706d\u83cc\u5de5\u5e8f\u751f\u4ea7\u8bb0\u5f55 \u25a1\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f \u2611\u975e\u5173\u952e/\u7279\u6b8a\u5de5\u5e8f"));
        assertEquals("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f",
                MesProBatchRecordSharedPageTitleRules.normalizeSharedTitle("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f"));
    }

    @Test
    void resolveRepresentativeTitle_prefersSharedStandaloneShortTitleOverFallback() {
        List<List<MesProBatchRecordParsedCell>> rows = List.of(
                row(
                        "\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5\u751f\u4ea7\u8bb0\u5f55",
                        "\u8bb0\u5f55\u7f16\u53f7",
                        "RE-PP-ID-01"),
                row("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f"),
                row("\u5de5\u5e8f\u540d\u79f0", "\u64cd\u4f5c\u4eba\u5458", "\u88c5\u914d\u65e5\u671f")
        );

        String title = MesProBatchRecordSharedPageTitleRules.resolveRepresentativeTitle(
                "\u4ea7\u54c1\u4fe1\u606f", rows);

        assertEquals("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f", title);
    }

    @Test
    void resolveRepresentativeTitle_prefersTrailingSharedInfoSectionWhenOneSummaryPageContainsMultipleShortTitles() {
        List<List<MesProBatchRecordParsedCell>> rows = List.of(
                row(
                        "\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5\u751f\u4ea7\u8bb0\u5f55",
                        "\u8bb0\u5f55\u7f16\u53f7",
                        "RE-PP-ID-01"),
                row("\u4ea7\u54c1\u4fe1\u606f"),
                row("\u4ea7\u54c1\u540d\u79f0", "\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5", "\u578b\u53f7\u89c4\u683c"),
                row("\u914d\u4ef6\u8fdb\u8d27\u6279\u53f7\u4fe1\u606f"),
                row("\u7269\u6599\u7f16\u7801", "\u7269\u6599\u540d\u79f0", "\u7269\u6599\u6279\u53f7"),
                row("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f"),
                row("\u5de5\u5e8f\u540d\u79f0", "\u64cd\u4f5c\u4eba\u5458", "\u88c5\u914d\u65e5\u671f"),
                row("\u8fc7\u7a0b\u653e\u884c\u4fe1\u606f"),
                row("\u8fc7\u7a0b\u653e\u884c\u4eba/\u653e\u884c\u65e5\u671f\uff1a")
        );

        String title = MesProBatchRecordSharedPageTitleRules.resolveRepresentativeTitle(
                "\u4ea7\u54c1\u4fe1\u606f", rows);

        assertEquals("\u88c5\u914d\u53ca\u5305\u88c5\u4fe1\u606f", title);
    }

    @Test
    void shouldStartNewTemplate_allowsOnlyFirstShortInfoOrSummaryHeaderInsideOneTemplate() {
        assertTrue(MesProBatchRecordSharedPageTitleRules.shouldStartNewTemplate(
                MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.INFORMATION_SUMMARY, false));
        assertTrue(MesProBatchRecordSharedPageTitleRules.shouldStartNewTemplate(
                MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.OTHER_SHORT_TITLE, false));
        assertTrue(MesProBatchRecordSharedPageTitleRules.shouldStartNewTemplate(
                MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.PROCESS_RECORD, true));

        assertFalse(MesProBatchRecordSharedPageTitleRules.shouldStartNewTemplate(
                MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.INFORMATION_SUMMARY, true));
        assertFalse(MesProBatchRecordSharedPageTitleRules.shouldStartNewTemplate(
                MesProBatchRecordSharedPageTitleRules.SharedPageTitleType.OTHER_SHORT_TITLE, true));
    }

    private static List<MesProBatchRecordParsedCell> row(String... texts) {
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        for (String text : texts) {
            row.add(MesProBatchRecordParsedCell.builder()
                    .text(text)
                    .rowSpan(1)
                    .colSpan(1)
                    .bold(false)
                    .fontSize(10)
                    .horizontalAlign("center")
                    .verticalAlign("middle")
                    .widthPx(120)
                    .heightPx(24)
                    .build());
        }
        return row;
    }
}
