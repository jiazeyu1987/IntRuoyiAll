package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordReportStyleEnhancerTest {

    private final MesProBatchRecordReportJsonBuilder builder = new MesProBatchRecordReportJsonBuilder();
    private final MesProBatchRecordReportStyleEnhancer enhancer = new MesProBatchRecordReportStyleEnhancer();

    @Test
    void enhance_shouldShadeSingleMergedSectionRowByShape() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("\u6d4b\u8bd5\u8868")
                .columnCount(4)
                .rowCount(1)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("\u5de5\u827a\u53c2\u6570\u786e\u8ba4")
                                .colSpan(4)
                                .widthPx(720)
                                .bold(true)
                                .build())
                ))
                .build();

        JSONObject before = JSON.parseObject(builder.build(table, "EBR_STYLE_T01"));
        int styleBefore = styleIndex(before, 0, "0");

        JSONObject after = JSON.parseObject(enhancer.enhance(before.toJSONString(), table));
        JSONArray styles = after.getJSONArray("styles");
        int styleAfter = styleIndex(after, 0, "0");

        assertNotEquals(styleBefore, styleAfter);
        assertEquals("#d9d9d9", styles.getJSONObject(styleAfter).getString("bgcolor"));
        assertEquals("thick", styles.getJSONObject(styleAfter).getJSONObject("border").getJSONArray("top").getString(0));
    }

    @Test
    void enhance_shouldShadeBoldHeaderCellsInStructuredRowWithoutTemplateLookup() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(2)
                .tableTitle("\u6d4b\u8bd5\u8868")
                .columnCount(4)
                .rowCount(1)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("\u6807A").bold(true).widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("\u6807B").bold(true).widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("\u6807C").bold(true).widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("\u6807D").bold(true).widthPx(120).build()
                        )
                ))
                .build();

        JSONObject before = JSON.parseObject(builder.build(table, "EBR_STYLE_T02"));
        int firstStyleBefore = styleIndex(before, 0, "0");
        int lastStyleBefore = styleIndex(before, 0, "3");

        JSONObject after = JSON.parseObject(enhancer.enhance(before.toJSONString(), table));
        JSONArray styles = after.getJSONArray("styles");
        int firstStyleAfter = styleIndex(after, 0, "0");
        int lastStyleAfter = styleIndex(after, 0, "3");

        assertNotEquals(firstStyleBefore, firstStyleAfter);
        assertNotEquals(lastStyleBefore, lastStyleAfter);
        assertEquals("#ececec", styles.getJSONObject(firstStyleAfter).getString("bgcolor"));
        assertEquals("#ececec", styles.getJSONObject(lastStyleAfter).getString("bgcolor"));
        assertEquals("thick", styles.getJSONObject(firstStyleAfter).getJSONObject("border").getJSONArray("top").getString(0));
    }

    @Test
    void enhance_shouldShadeOnlyBoldCellsInMixedStructuredRow() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(3)
                .tableTitle("\u6d4b\u8bd5\u8868")
                .columnCount(4)
                .rowCount(1)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("\u9879A").bold(true).widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("\u503cA").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("\u9879B").bold(true).widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("\u503cB").widthPx(120).build()
                        )
                ))
                .build();

        JSONObject before = JSON.parseObject(builder.build(table, "EBR_STYLE_T03"));
        int labelAStyleBefore = styleIndex(before, 0, "0");
        int valueAStyleBefore = styleIndex(before, 0, "1");

        JSONObject after = JSON.parseObject(enhancer.enhance(before.toJSONString(), table));
        JSONArray styles = after.getJSONArray("styles");
        int labelAStyleAfter = styleIndex(after, 0, "0");
        int valueAStyleAfter = styleIndex(after, 0, "1");

        assertNotEquals(labelAStyleBefore, labelAStyleAfter);
        assertEquals("#ececec", styles.getJSONObject(labelAStyleAfter).getString("bgcolor"));
        assertNull(styles.getJSONObject(valueAStyleAfter).getString("bgcolor"));
    }

    @Test
    void enhance_shouldShadeCompactLabelCellsByRowShapeEvenIfTheyAreNotBold() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(4)
                .tableTitle("\u6d4b\u8bd5\u8868")
                .columnCount(3)
                .rowCount(1)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("\u68c0\u67e5\u9879\u76ee").bold(true).widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("\u64cd\u4f5c\u4eba/\u65e5\u671f").bold(false).widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("100%").bold(false).widthPx(120).build()
                        )
                ))
                .build();

        JSONObject before = JSON.parseObject(builder.build(table, "EBR_STYLE_T04"));
        int boldStyleBefore = styleIndex(before, 0, "0");
        int labelStyleBefore = styleIndex(before, 0, "1");
        int valueStyleBefore = styleIndex(before, 0, "2");

        JSONObject after = JSON.parseObject(enhancer.enhance(before.toJSONString(), table));
        JSONArray styles = after.getJSONArray("styles");
        int boldStyleAfter = styleIndex(after, 0, "0");
        int labelStyleAfter = styleIndex(after, 0, "1");
        int valueStyleAfter = styleIndex(after, 0, "2");

        assertNotEquals(boldStyleBefore, boldStyleAfter);
        assertNotEquals(labelStyleBefore, labelStyleAfter);
        assertEquals("#ececec", styles.getJSONObject(boldStyleAfter).getString("bgcolor"));
        assertEquals("#f7f7f7", styles.getJSONObject(labelStyleAfter).getString("bgcolor"));
        assertNull(styles.getJSONObject(valueStyleAfter).getString("bgcolor"));
    }

    @Test
    void enhance_shouldPreserveOuterBorderWeightForSectionBand() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(5)
                .tableTitle("\u6d4b\u8bd5\u8868")
                .columnCount(4)
                .rowCount(1)
                .rows(List.of(
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("\u751f\u4ea7\u6279\u91cf\u6c47\u603b")
                                .colSpan(4)
                                .widthPx(720)
                                .bold(true)
                                .build())
                ))
                .build();

        JSONObject before = JSON.parseObject(builder.build(table, "EBR_STYLE_T05"));
        JSONObject after = JSON.parseObject(enhancer.enhance(before.toJSONString(), table));
        JSONObject style = after.getJSONArray("styles").getJSONObject(styleIndex(after, 0, "0"));
        JSONObject border = style.getJSONObject("border");

        assertEquals("thick", border.getJSONArray("top").getString(0));
        assertEquals("thick", border.getJSONArray("bottom").getString(0));
        assertEquals("thick", border.getJSONArray("left").getString(0));
        assertEquals("thick", border.getJSONArray("right").getString(0));
    }

    @Test
    void enhance_shouldShadeGenericLabelCellsKeepValueCellsWhiteAndPreservePcsAccent() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(6)
                .tableTitle("\u6d4b\u8bd5\u8868")
                .columnCount(4)
                .rowCount(1)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("\u751f\u4ea7\u6570\u91cf/pcs").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("\u64cd\u4f5c\u4eba/\u65e5\u671f").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(120).build()
                        )
                ))
                .build();

        JSONObject after = JSON.parseObject(enhancer.enhance(builder.build(table, "EBR_STYLE_T06"), table));
        JSONObject pcsLabelStyle = after.getJSONArray("styles").getJSONObject(styleIndex(after, 0, "0"));
        JSONObject pcsValueStyle = after.getJSONArray("styles").getJSONObject(styleIndex(after, 0, "1"));
        JSONObject genericLabelStyle = after.getJSONArray("styles").getJSONObject(styleIndex(after, 0, "2"));
        JSONObject genericValueStyle = after.getJSONArray("styles").getJSONObject(styleIndex(after, 0, "3"));

        assertEquals("#f7f7f7", pcsLabelStyle.getString("bgcolor"));
        assertEquals("#c00000", pcsLabelStyle.getString("color"));
        assertEquals("#f7f7f7", genericLabelStyle.getString("bgcolor"));
        assertTrue(!pcsValueStyle.containsKey("bgcolor"));
        assertTrue(!genericValueStyle.containsKey("bgcolor"));
    }

    @Test
    void enhance_shouldTopAlignAndShrinkLongNarrativeValueCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(7)
                .tableTitle("\u6d4b\u8bd5\u8868")
                .columnCount(2)
                .rowCount(1)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("\u68c0\u9a8c\u65b9\u6cd5").widthPx(120).build(),
                                MesProBatchRecordParsedCell.builder()
                                        .text("\u7528\u6d01\u51c0\u5e03\u3001\u4e03\u5341\u4e94\u767e\u5206\u6bd4\u9152\u7cbe\u6e7f\u6da6\uff0c\u64e6\u62ed\u4ea7\u54c1\u8868\u9762\uff0c\u518d\u4ece\u6b63\u5e38\u89c6\u89d2\u89c2\u5bdf\u6b8b\u7559\u60c5\u51b5\u3002")
                                        .widthPx(520)
                                        .heightPx(48)
                                        .build()
                        )
                ))
                .build();

        JSONObject before = JSON.parseObject(builder.build(table, "EBR_STYLE_T07"));
        JSONObject beforeNarrativeStyle = before.getJSONArray("styles").getJSONObject(styleIndex(before, 0, "1"));

        JSONObject after = JSON.parseObject(enhancer.enhance(before.toJSONString(), table));
        JSONObject afterNarrativeStyle = after.getJSONArray("styles").getJSONObject(styleIndex(after, 0, "1"));
        JSONObject afterLabelStyle = after.getJSONArray("styles").getJSONObject(styleIndex(after, 0, "0"));

        assertEquals("middle", beforeNarrativeStyle.getString("valign"));
        assertEquals("top", afterNarrativeStyle.getString("valign"));
        assertTrue(fontSize(afterNarrativeStyle) < fontSize(beforeNarrativeStyle));
        assertEquals("middle", afterLabelStyle.getString("valign"));
    }

    @Test
    void enhance_shouldShrinkDenseRepeatedDetailTextCells() {
        MesProBatchRecordParsedTable table = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(8)
                .tableTitle("\u6d4b\u8bd5\u8868")
                .columnCount(8)
                .rowCount(1)
                .rows(List.of(
                        List.of(
                                MesProBatchRecordParsedCell.builder().text("/").widthPx(80).build(),
                                MesProBatchRecordParsedCell.builder().text("/").widthPx(90).build(),
                                MesProBatchRecordParsedCell.builder().text("\u25a130atm\u538b\u529b\u8868").widthPx(92).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(100).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(100).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(100).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(96).build(),
                                MesProBatchRecordParsedCell.builder().text("").widthPx(96).build()
                        )
                ))
                .build();

        JSONObject before = JSON.parseObject(builder.build(table, "EBR_STYLE_T08"));
        JSONObject beforeDenseStyle = before.getJSONArray("styles").getJSONObject(styleIndex(before, 0, "2"));
        int blankStyleBefore = styleIndex(before, 0, "3");

        JSONObject after = JSON.parseObject(enhancer.enhance(before.toJSONString(), table));
        JSONObject afterDenseStyle = after.getJSONArray("styles").getJSONObject(styleIndex(after, 0, "2"));
        int blankStyleAfter = styleIndex(after, 0, "3");

        assertTrue(fontSize(afterDenseStyle) < fontSize(beforeDenseStyle));
        assertEquals(blankStyleBefore, blankStyleAfter);
    }

    private static int styleIndex(JSONObject root, int rowIndex, String columnIndex) {
        return root.getJSONObject("rows").getJSONObject(String.valueOf(rowIndex))
                .getJSONObject("cells").getJSONObject(columnIndex).getIntValue("style");
    }

    private static int fontSize(JSONObject style) {
        return style.getJSONObject("font").getIntValue("size");
    }
}
