package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordReportShapeRulesTest {

    @Test
    void resolveTargetRenderWidth_shouldUseSharedBudgetByColumnDensity() {
        assertEquals(1120, MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(8));
        assertEquals(MesProBatchRecordReportShapeRules.TARGET_RENDER_WIDTH_PX,
                MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(12));
        assertEquals(1044, MesProBatchRecordReportShapeRules.resolveTargetRenderWidth(20));
    }

    @Test
    void resolveSharedPageWidthBudget_shouldFollowGenericDensityBands() {
        assertEquals(1120, MesProBatchRecordReportShapeRules.resolveSharedPageWidthBudget(6));
        assertEquals(1120, MesProBatchRecordReportShapeRules.resolveSharedPageWidthBudget(8));
        assertEquals(1044, MesProBatchRecordReportShapeRules.resolveSharedPageWidthBudget(9));
        assertEquals(1044, MesProBatchRecordReportShapeRules.resolveSharedPageWidthBudget(14));
        assertEquals(1044, MesProBatchRecordReportShapeRules.resolveSharedPageWidthBudget(15));
    }

    @Test
    void resolveDenseTailColumnWidthFloor_shouldReserveMoreBreathingRoomForRightSideDenseLabels() {
        assertEquals(44, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "\u751f\u4ea7\u6570\u91cf/pcs", 16, 20));
        assertEquals(40, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "\u64cd\u4f5c\u4eba", 18, 20));
        assertEquals(44, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "\u590d\u6838\u4eba/\u65e5\u671f", 19, 20));
        assertEquals(0, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "\u7269\u6599\u7f16\u7801", 2, 20));
    }

    @Test
    void resolveDenseTailColumnWidthFloor_shouldAlsoProtectMediumDensitySemanticTailColumns() {
        assertEquals(44, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "\u751f\u4ea7\u6570\u91cf/pcs", 6, 10));
        assertEquals(40, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "\u590d\u6838\u4eba", 9, 10));
        assertEquals(0, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "\u751f\u4ea7\u6570\u91cf/pcs", 6, 8));
    }

    @Test
    void resolveDenseTailColumnWidthFloor_shouldProtectChecklistChoiceColumns() {
        assertEquals(36, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "□符合要求\n□不符合要求", 7, 10));
        assertEquals(36, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "□是 □否", 8, 12));
        assertEquals(0, MesProBatchRecordReportShapeRules.resolveDenseTailColumnWidthFloor(
                "□符合要求\n□不符合要求", 2, 8));
    }

    @Test
    void estimateRowHeight_shouldReserveExtraWhitespaceForNarrativeBlocks() {
        int compactHeight = MesProBatchRecordReportShapeRules.estimateRowHeight(
                "\u751f\u4ea7\u6279\u53f7", 220, 9);
        int narrativeHeight = MesProBatchRecordReportShapeRules.estimateRowHeight(
                "1\u3001\u8f6c\u5165\u6d01\u51c0\u533a\u540e\u5b8c\u6210\u64e6\u62ed\uff0c\u5e76\u572830\u5206\u949f\u5185\u56de\u4f20\u3002\n"
                        + "2\u3001\u786e\u8ba4\u5de5\u5177\u3001\u7269\u6599\u548c\u573a\u5730\u72b6\u6001\u6e05\u695a\u3002",
                220, 9);

        assertTrue(narrativeHeight > compactHeight);
        assertTrue(narrativeHeight >= 40);
    }

    @Test
    void resolveNarrativeRowHeightFloor_shouldReserveStepwiseWhitespace() {
        assertEquals(40, MesProBatchRecordReportShapeRules.resolveNarrativeRowHeightFloor(1));
        assertEquals(44, MesProBatchRecordReportShapeRules.resolveNarrativeRowHeightFloor(2));
        assertEquals(48, MesProBatchRecordReportShapeRules.resolveNarrativeRowHeightFloor(3));
    }

    @Test
    void resolveRowHeightFloor_shouldVaryBySharedRowType() {
        assertEquals(18, MesProBatchRecordReportShapeRules.resolveRowHeightFloor(
                MesProBatchRecordSharedRowTypeRules.RowType.FIELD, 1));
        assertEquals(18, MesProBatchRecordReportShapeRules.resolveRowHeightFloor(
                MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA, 1));
        assertEquals(20, MesProBatchRecordReportShapeRules.resolveRowHeightFloor(
                MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER, 1));
        assertEquals(22, MesProBatchRecordReportShapeRules.resolveRowHeightFloor(
                MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY, 1));
        assertEquals(20, MesProBatchRecordReportShapeRules.resolveRowHeightFloor(
                MesProBatchRecordSharedRowTypeRules.RowType.FOOTER, 1));
        assertTrue(MesProBatchRecordReportShapeRules.resolveRowHeightFloor(
                MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION, 2) >= 40);
    }

    @Test
    void resolveHorizontalAlign_shouldLeftAlignGenericNarrativeSentences() {
        MesProBatchRecordParsedCell cell = MesProBatchRecordParsedCell.builder()
                .text("\u8f6c\u5165\u6d01\u51c0\u533a\u540e\u5b8c\u6210\u64e6\u62ed\uff0c\u5e76\u572830\u5206\u949f\u5185\u56de\u4f20\u3002")
                .horizontalAlign("left")
                .build();

        assertEquals("left", MesProBatchRecordReportShapeRules.resolveHorizontalAlign(cell));
    }

    @Test
    void compactFillRules_shouldShrinkDenseControlChromeAndHideCompactPlaceholders() {
        MesProBatchRecordParsedCell compactBlank = MesProBatchRecordParsedCell.builder()
                .text("")
                .widthPx(176)
                .heightPx(24)
                .build();
        MesProBatchRecordParsedCell wideBlank = MesProBatchRecordParsedCell.builder()
                .text("")
                .colSpan(3)
                .widthPx(360)
                .heightPx(36)
                .inputType(MesProBatchRecordReportShapeRules.INPUT_TYPE_TEXTAREA)
                .build();

        assertTrue(MesProBatchRecordReportShapeRules.isCompactFillableCell(compactBlank, 176));
        assertTrue(MesProBatchRecordReportShapeRules.shouldHideCompactFillPlaceholder(compactBlank, 176));
        assertTrue(!MesProBatchRecordReportShapeRules.isCompactFillableCell(wideBlank, 360));
        assertTrue(!MesProBatchRecordReportShapeRules.shouldHideCompactFillPlaceholder(wideBlank, 360));
        assertEquals(96, MesProBatchRecordReportShapeRules.resolveCompactFillLayoutWidth(List.of(176, 176, 176, 176)));
        assertEquals(20, MesProBatchRecordReportShapeRules.resolveCompactFillLayoutHeight(List.of(24, 24, 24, 24)));
        assertEquals(20, MesProBatchRecordReportShapeRules.resolveCompactFillRowHeightFloor(20));
        assertTrue(MesProBatchRecordReportShapeRules.shouldUseCompactFillLayout(8, 8));
    }
}
