package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProBatchRecordSharedRowTypeRulesTest {

    @Test
    void classifyRow_returnsTitleForTopStandaloneProcessAndSummaryTitles() {
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.TITLE, classify(List.of(
                row("精洗工序生产记录"),
                row("工序名称", "操作人员", "装配日期")
        ), 0));
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.TITLE, classify(List.of(
                row("生产记录汇总表"),
                row("工序名称", "操作人员", "装配日期")
        ), 0));
    }

    @Test
    void classifyRow_returnsFieldForMetadataRows() {
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.FIELD, classify(List.of(
                row("通用工序生产记录"),
                row("生产批号", "", "产品规格", "", "生产依据", "")
        ), 1));
    }

    @Test
    void classifyRow_returnsLongDescriptionForNarrativeRows() {
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION, classify(List.of(
                row("通用工序生产记录"),
                row("备注：检查结果符合要求后进行以下生产操作")
        ), 1));
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION, classify(List.of(
                row("通用工序生产记录"),
                row("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；"
                                + "2、墙面、地面、天花板、灯具等环境清洁应符合要求。",
                        "□符合要求\n□不符合要求", "", "")
        ), 1));
    }

    @Test
    void classifyRow_returnsLongDescriptionForChecklistNarrativeRows() {
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.LONG_DESCRIPTION, classify(List.of(
                row("精洗工序生产记录"),
                row("1、工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具；"
                                + "2、墙面、地面、天花板、灯具等环境清洁应符合要求。",
                        "□符合要求\n□不符合要求", "", "")
        ), 1));
    }

    @Test
    void classifyRow_returnsTableHeaderForColumnLabelAndValueHeaderRows() {
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER, classify(List.of(
                row("通用工序生产记录"),
                row("操作日期", "物料编码", "物料名称", "批号", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人")
        ), 1));
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER, classify(List.of(
                row("通用工序生产记录"),
                row("参考值", "实际", "参考值", "实际", "参考值", "实际")
        ), 1));
    }

    @Test
    void classifyRow_returnsDetailDataForRepeatedSparseChecklistRowsAfterStructuredHeader() {
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA, classify(List.of(
                row("清洁工序生产记录"),
                row("检查项目", "结果", "操作人/日期", "复核人/日期"),
                row("/", "□30atm压力表", "", ""),
                row("/", "□30atm压力表", "", "")
        ), 2));
    }

    @Test
    void classifyRow_returnsDetailDataForActualRecordRows() {
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.DETAIL_DATA, classify(List.of(
                row("通用工序生产记录"),
                row("操作日期", "物料编码", "物料名称", "批号", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人"),
                row("2026-05-17", "A001", "弹簧", "B-01", "120", "118", "2", "张三", "李四")
        ), 2));
    }

    @Test
    void classifyRow_returnsSummaryForTotalsRowsAfterDetails() {
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.SUMMARY, classify(List.of(
                row("通用工序生产记录"),
                row("操作日期", "物料编码", "物料名称", "批号", "生产数量/pcs", "自检合格数量/pcs", "不合格数量/pcs", "操作人", "复核人"),
                row("2026-05-17", "A001", "弹簧", "B-01", "120", "118", "2", "张三", "李四"),
                row("生产批量汇总", "合格数量", "118", "不合格数量", "2")
        ), 3));
    }

    @Test
    void classifyRow_returnsFooterForEffectiveDateRows() {
        assertEquals(MesProBatchRecordSharedRowTypeRules.RowType.FOOTER, classify(List.of(
                row("通用工序生产记录"),
                row("生效日期：2026年02月02日")
        ), 1));
    }

    private static MesProBatchRecordSharedRowTypeRules.RowType classify(
            List<List<MesProBatchRecordParsedCell>> rows, int rowIndex) {
        return MesProBatchRecordSharedRowTypeRules.classifyRow(rows, rowIndex);
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
