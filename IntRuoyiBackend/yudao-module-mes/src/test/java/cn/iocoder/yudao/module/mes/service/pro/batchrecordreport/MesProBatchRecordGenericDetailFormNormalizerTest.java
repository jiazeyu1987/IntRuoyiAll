package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordGenericDetailFormNormalizerTest {

    @Test
    void normalizeSourceRows_whenMergedChecklistDetailBodyUsesNonLossLabels_expandsByStructure() {
        MesProBatchRecordGenericDetailFormNormalizer normalizer =
                new MesProBatchRecordGenericDetailFormNormalizer(genericDeviationSpec());
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("偏差处置记录")
                .rowCount(3)
                .columnCount(8)
                .rows(List.of(
                        List.of(
                                sourceCell("产品名称", 2, 240, 30),
                                sourceCell("型号规格", 2, 240, 30),
                                sourceCell("批号", 2, 240, 30),
                                sourceCell("生产数量", 2, 240, 30)),
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("""
                                        异常描述：
                                        发生日期
                                        工序名称
                                        数量
                                        异常原因
                                        处理方式
                                        生产人员/日期
                                        质量人员/日期

                                        □返工   □让步接收：______________
                                        □返工   □让步接收：______________
                                        """)
                                .colSpan(8)
                                .widthPx(960)
                                .heightPx(220)
                                .build()),
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("批准人/日期：")
                                .colSpan(8)
                                .widthPx(960)
                                .heightPx(48)
                                .build())
                ))
                .build();

        assertTrue(normalizer.supportsSourceTable(source));

        List<List<MesProBatchRecordParsedCell>> rows = normalizer.normalizeSourceRows(source);

        assertEquals(6, rows.size());
        assertEquals("异常描述：", rows.get(1).get(0).getText());
        assertEquals(9, rows.get(1).get(0).getColSpan());
        assertEquals(List.of("发生日期", "工序名称", "数量", "异常原因", "处理方式",
                        "生产人员/日期", "质量人员/日期"),
                rows.get(2).stream().map(MesProBatchRecordParsedCell::getText).toList());

        List<MesProBatchRecordParsedCell> firstDetailRow = rows.get(3);
        assertEquals(9, firstDetailRow.size());
        assertTrue(firstDetailRow.get(0).isFillable());
        assertTrue(firstDetailRow.get(3).isFillable());
        assertEquals(MesProBatchRecordReportShapeRules.INPUT_TYPE_TEXTAREA, firstDetailRow.get(3).getInputType());
        assertEquals("□返工", firstDetailRow.get(4).getText());
        assertFalse(firstDetailRow.get(4).isFillable());
        assertEquals("□让步接收：", firstDetailRow.get(5).getText());
        assertFalse(firstDetailRow.get(5).isFillable());
        assertTrue(firstDetailRow.get(6).isFillable());
        assertEquals(2, firstDetailRow.get(7).getRowSpan());
        assertEquals(2, firstDetailRow.get(8).getRowSpan());
        assertTrue(firstDetailRow.get(7).isFillable());
        assertTrue(firstDetailRow.get(8).isFillable());

        assertEquals("批准人/日期：", rows.get(5).get(0).getText());
        assertTrue(rows.get(5).get(1).isFillable());
    }

    @Test
    void normalizeSourceRows_whenSourceHasEffectiveDateTail_preservesTailByGenericRule() {
        MesProBatchRecordGenericDetailFormNormalizer normalizer =
                new MesProBatchRecordGenericDetailFormNormalizer(genericDeviationSpec());
        MesProBatchRecordParsedTable source = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("偏差处置记录")
                .rowCount(4)
                .columnCount(8)
                .rows(List.of(
                        List.of(
                                sourceCell("产品名称", 2, 240, 30),
                                sourceCell("", 2, 240, 30),
                                sourceCell("生产数量", 2, 240, 30),
                                sourceCell("", 2, 240, 30)),
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("""
                                        异常描述：
                                        发生日期
                                        工序名称
                                        数量
                                        异常原因
                                        处理方式
                                        生产人员/日期
                                        质量人员/日期

                                        □返工   □让步接收：______________
                                        """)
                                .colSpan(8)
                                .widthPx(960)
                                .heightPx(180)
                                .build()),
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("批准人/日期：")
                                .colSpan(8)
                                .widthPx(960)
                                .heightPx(48)
                                .build()),
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("生效日期：2026年7月17日")
                                .colSpan(8)
                                .widthPx(960)
                                .heightPx(24)
                                .build())
                ))
                .build();

        List<List<MesProBatchRecordParsedCell>> rows = normalizer.normalizeSourceRows(source);

        assertEquals(6, rows.size());
        assertEquals("生效日期：2026年7月17日", rows.get(5).get(0).getText());
        assertEquals(8, rows.get(5).get(0).getColSpan());
    }

    private static MesProBatchRecordGenericDetailFormNormalizer.Spec genericDeviationSpec() {
        return new MesProBatchRecordGenericDetailFormNormalizer.Spec(
                9,
                List.of("产品名称", "型号规格", "批号", "生产数量", "异常描述", "批准人"),
                "异常描述",
                List.of("发生日期", "工序名称", "数量", "异常原因", "处理方式",
                        "生产人员/日期", "质量人员/日期", "□返工", "□让步接收"),
                "异常描述：",
                List.of(
                        new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("发生日期", 1),
                        new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("工序名称", 1),
                        new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("数量", 1),
                        new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("异常原因", 1),
                        new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("处理方式", 3),
                        new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("生产人员/日期", 1),
                        new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("质量人员/日期", 1)
                ),
                List.of(
                        MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.fillable(
                                1, MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT, false),
                        MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.fillable(
                                1, MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT, false),
                        MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.fillable(
                                1, MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT, false),
                        MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.fillable(
                                1, MesProBatchRecordReportShapeRules.INPUT_TYPE_TEXTAREA, false),
                        MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.trailingUnderlineChoices(
                                "□返工   □让步接收：______________", 3,
                                MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT),
                        MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.fillable(
                                1, MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT, true),
                        MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.fillable(
                                1, MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT, true)
                ),
                "□返工",
                "批准人",
                "批准人/日期：",
                2,
                7);
    }

    private static MesProBatchRecordParsedCell sourceCell(String text, int colSpan, int widthPx, int heightPx) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .colSpan(colSpan)
                .widthPx(widthPx)
                .heightPx(heightPx)
                .build();
    }
}
