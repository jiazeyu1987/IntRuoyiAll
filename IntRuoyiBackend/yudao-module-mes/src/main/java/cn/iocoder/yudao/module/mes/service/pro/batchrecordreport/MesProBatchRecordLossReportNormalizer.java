package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class MesProBatchRecordLossReportNormalizer implements MesProBatchRecordFormProfile {

    private static final int LOSS_REPORT_COLUMN_COUNT = 9;
    private static final String LOSS_REPORT_DISPOSAL_LABEL_TEXT = "□报废   □其他：";
    private static final MesProBatchRecordGenericDetailFormNormalizer LOSS_REPORT_DETAIL_NORMALIZER =
            new MesProBatchRecordGenericDetailFormNormalizer(new MesProBatchRecordGenericDetailFormNormalizer.Spec(
                    LOSS_REPORT_COLUMN_COUNT,
                    List.of("产品名称", "型号规格", "批号", "生产数量", "损耗描述", "批准人"),
                    "损耗描述",
                    List.of("不合格日期", "工序名称", "不合格数量", "不合格原因", "处置方式",
                            "生产人员/日期", "检验人员", "确认/日期", "□报废", "□其他"),
                    "损耗描述：",
                    List.of(
                            new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("不合格日期", 1),
                            new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("工序名称", 1),
                            new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("不合格数量", 1),
                            new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("不合格原因", 1),
                            new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("处置方式", 3),
                            new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("生产人员/日期", 1),
                            new MesProBatchRecordGenericDetailFormNormalizer.HeaderCellSpec("检验人员\n确认/日期", 1)
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
                            MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.reviewedTrailingUnderlineChoices(
                                    LOSS_REPORT_DISPOSAL_LABEL_TEXT + "______________",
                                    3,
                                    MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT),
                            MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.fillable(
                                    1, MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT, true),
                            MesProBatchRecordGenericDetailFormNormalizer.DetailCellSpec.fillable(
                                    1, MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT, true)
                    ),
                    "□报废",
                    "批准人",
                    "批准人/日期：",
                    2,
                    7));

    @Override
    public String formSlotType() {
        return MesProBatchRecordFormSlotType.LOSS_REPORT.getType();
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean supportsSourceTable(MesProBatchRecordParsedTable table) {
        return LOSS_REPORT_DETAIL_NORMALIZER.supportsSourceTable(table);
    }

    @Override
    public MesProBatchRecordParsedTable normalizeSourceTable(int templateIndex,
                                                            MesProBatchRecordParsedTable sourceTable) {
        return normalizeLossReportSourceTable(templateIndex, sourceTable);
    }

    private MesProBatchRecordParsedTable normalizeLossReportSourceTable(int templateIndex,
                                                                        MesProBatchRecordParsedTable sourceTable) {
        List<List<MesProBatchRecordParsedCell>> rows =
                LOSS_REPORT_DETAIL_NORMALIZER.normalizeSourceRows(sourceTable);
        int columnCount = resolveColumnCount(rows);
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(templateIndex)
                .sourceTopLevelTableIndex(sourceTable.getSourceTopLevelTableIndex())
                .sourceSplitIndex(sourceTable.getSourceSplitIndex())
                .tableTitle(sourceTable.getTableTitle())
                .rowCount(rows.size())
                .columnCount(columnCount)
                .columnWidths(normalizeColumnWidths(sourceTable.getColumnWidths(), columnCount))
                .preserveSourceGrid(Boolean.TRUE)
                .routeBSource(sourceTable.getRouteBSource())
                .documentFrame(sourceTable.getDocumentFrame())
                .rows(rows)
                .build();
    }

    @Override
    public boolean supportsLegacyLayout(MesProBatchRecordReportDO metadata, JSONObject root) {
        if (metadata == null || !formSlotType().equals(metadata.getFormSlotType())) {
            return false;
        }
        return isLegacyMergedLossReportLayout(root)
                || isLegacyVerticalLossReportLayout(root)
                || isLegacyWholeDisposalLossReportLayout(root);
    }

    @Override
    public MesProBatchRecordParsedTable normalizeLegacyLayout(MesProBatchRecordReportDO metadata, JSONObject root) {
        boolean legacyVerticalLayout = isLegacyVerticalLossReportLayout(root);
        boolean legacyWholeDisposalLayout = isLegacyWholeDisposalLossReportLayout(root);
        MesProBatchRecordParsedTable sourceTable = legacyVerticalLayout || legacyWholeDisposalLayout
                ? toLegacyVerticalLossReportParsedTable(metadata, root)
                : toLegacyLossReportParsedTable(metadata, root);
        return normalizeLossReportSourceTable(
                metadata.getSourceTableIndex() == null ? 1 : metadata.getSourceTableIndex(), sourceTable);
    }

    private List<Integer> normalizeColumnWidths(List<Integer> sourceColumnWidths, int columnCount) {
        if (sourceColumnWidths == null || sourceColumnWidths.isEmpty() || sourceColumnWidths.size() == columnCount) {
            return sourceColumnWidths;
        }
        if (sourceColumnWidths.size() == 8 && columnCount == LOSS_REPORT_COLUMN_COUNT) {
            int disposalWidth = Math.max(3, sourceColumnWidths.get(4) + sourceColumnWidths.get(5));
            int firstDisposalWidth = Math.max(1, disposalWidth / 3);
            int secondDisposalWidth = Math.max(1, disposalWidth / 3);
            int otherTextWidth = Math.max(1, disposalWidth - firstDisposalWidth - secondDisposalWidth);
            List<Integer> widths = new ArrayList<>();
            widths.addAll(sourceColumnWidths.subList(0, 4));
            widths.add(firstDisposalWidth);
            widths.add(secondDisposalWidth);
            widths.add(otherTextWidth);
            widths.add(sourceColumnWidths.get(6));
            widths.add(sourceColumnWidths.get(7));
            return widths;
        }
        return sourceColumnWidths;
    }

    private int resolveColumnCount(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 1;
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .mapToInt(this::resolveEstimatedColumnCount)
                .max()
                .orElse(1);
    }

    private int resolveEstimatedColumnCount(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return 0;
        }
        return row.stream()
                .filter(Objects::nonNull)
                .mapToInt(cell -> Math.max(1, cell.getColSpan()))
                .sum();
    }

    private boolean isLegacyMergedLossReportLayout(JSONObject root) {
        JSONObject rows = root == null ? null : root.getJSONObject("rows");
        if (rows == null || isHorizontalLossReportLayout(root)) {
            return false;
        }
        for (String rowKey : rows.keySet()) {
            if (!StrUtil.isNumeric(rowKey)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            for (String columnKey : cells.keySet()) {
                JSONObject cell = cells.getJSONObject(columnKey);
                if (isLegacyMergedLossReportBodyCell(cell)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isLegacyVerticalLossReportLayout(JSONObject root) {
        JSONObject rows = root == null ? null : root.getJSONObject("rows");
        if (rows == null || isHorizontalLossReportLayout(root)) {
            return false;
        }
        return hasLegacyVerticalLabelValueRow(rows, "不合格日期")
                && hasLegacyVerticalLabelValueRow(rows, "工序名称")
                && hasLegacyVerticalLabelValueRow(rows, "不合格数量")
                && hasLegacyVerticalLabelValueRow(rows, "不合格原因")
                && hasLegacyVerticalLabelValueRow(rows, "处置方式")
                && hasLegacyVerticalLabelValueRow(rows, "生产人员/日期")
                && hasLegacyVerticalLabelValueRow(rows, "检验人员")
                && hasLegacyVerticalLabelValueRow(rows, "确认/日期")
                && countLegacyVerticalDisposalRows(rows) >= 1
                && rowTextContains(rows, "批准人/日期");
    }

    private boolean isHorizontalLossReportLayout(JSONObject root) {
        JSONObject rows = root == null ? null : root.getJSONObject("rows");
        if (rows == null) {
            return false;
        }
        JSONObject descriptionCell = cellAt(rows, 1, 0);
        JSONObject headerRow = rows.getJSONObject("2");
        JSONObject headerCells = headerRow == null ? null : headerRow.getJSONObject("cells");
        boolean oldEightColumnTail = headerCells != null
                && "生产人员/日期".equals(cellText(headerCells.getJSONObject("6")))
                && cellText(headerCells.getJSONObject("7")).contains("检验人员");
        boolean splitDisposalTail = headerCells != null
                && "生产人员/日期".equals(cellText(headerCells.getJSONObject("7")))
                && cellText(headerCells.getJSONObject("8")).contains("检验人员");
        return cellText(descriptionCell).contains("损耗描述")
                && headerCells != null
                && "不合格日期".equals(cellText(headerCells.getJSONObject("0")))
                && "工序名称".equals(cellText(headerCells.getJSONObject("1")))
                && "不合格数量".equals(cellText(headerCells.getJSONObject("2")))
                && "不合格原因".equals(cellText(headerCells.getJSONObject("3")))
                && "处置方式".equals(cellText(headerCells.getJSONObject("4")))
                && (oldEightColumnTail || splitDisposalTail);
    }

    private boolean isLegacyWholeDisposalLossReportLayout(JSONObject root) {
        JSONObject rows = root == null ? null : root.getJSONObject("rows");
        if (rows == null || !isHorizontalLossReportLayout(root)) {
            return false;
        }
        JSONObject firstDetailCells = rows.getJSONObject("3") == null
                ? null : rows.getJSONObject("3").getJSONObject("cells");
        if (firstDetailCells == null) {
            return false;
        }
        JSONObject disposalCell = firstDetailCells.getJSONObject("4");
        return disposalCell != null
                && cellText(disposalCell).contains("□报废")
                && cellText(disposalCell).contains("□其他")
                && cellText(disposalCell).contains("_")
                && disposalCell.containsKey("fillForm");
    }

    private boolean hasLegacyVerticalLabelValueRow(JSONObject rows, String label) {
        for (String rowKey : rows.keySet()) {
            if (!StrUtil.isNumeric(rowKey)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null || cells.size() > 3) {
                continue;
            }
            if (!label.equals(cellText(cells.getJSONObject("0")))) {
                continue;
            }
            if (cells.keySet().stream()
                    .filter(StrUtil::isNumeric)
                    .map(cells::getJSONObject)
                    .anyMatch(cell -> cell != null && cell.containsKey("fillForm"))) {
                return true;
            }
        }
        return false;
    }

    private int countLegacyVerticalDisposalRows(JSONObject rows) {
        int count = 0;
        for (String rowKey : rows.keySet()) {
            if (!StrUtil.isNumeric(rowKey)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            String rowText = rowText(cells);
            if (cells != null && rowText.contains("□报废") && rowText.contains("□其他")) {
                count++;
            }
        }
        return count;
    }

    private boolean rowTextContains(JSONObject rows, String expectedText) {
        for (String rowKey : rows.keySet()) {
            if (!StrUtil.isNumeric(rowKey)) {
                continue;
            }
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells != null && rowText(cells).contains(expectedText)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLegacyMergedLossReportBodyCell(JSONObject cell) {
        if (cell == null) {
            return false;
        }
        String text = StrUtil.blankToDefault(cell.getString("text"), "");
        int colSpan = resolveJsonCellColSpan(cell);
        return colSpan >= 4
                && text.contains("损耗描述")
                && text.contains("不合格日期")
                && text.contains("工序名称")
                && text.contains("不合格数量")
                && text.contains("不合格原因")
                && text.contains("处置方式")
                && text.contains("生产人员/日期")
                && text.contains("检验人员")
                && text.contains("确认/日期")
                && text.contains("□报废")
                && text.contains("□其他");
    }

    private MesProBatchRecordParsedTable toLegacyLossReportParsedTable(MesProBatchRecordReportDO metadata,
                                                                       JSONObject root) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        JSONObject rowsObject = root.getJSONObject("rows");
        rowsObject.keySet().stream()
                .filter(StrUtil::isNumeric)
                .map(Integer::valueOf)
                .sorted()
                .forEach(rowIndex -> {
                    JSONObject rowObject = rowsObject.getJSONObject(String.valueOf(rowIndex));
                    List<MesProBatchRecordParsedCell> row = toLegacyLossReportParsedRow(rowObject);
                    if (isLossReportSemanticSourceRow(row)) {
                        rows.add(row);
                    }
                });
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(metadata.getSourceTableIndex())
                .tableTitle(metadata.getReportName())
                .rowCount(rows.size())
                .columnCount(resolveLegacyParsedColumnCount(rows))
                .columnWidths(readLegacyColumnWidths(root))
                .preserveSourceGrid(Boolean.TRUE)
                .rows(rows)
                .build();
    }

    private MesProBatchRecordParsedTable toLegacyVerticalLossReportParsedTable(MesProBatchRecordReportDO metadata,
                                                                               JSONObject root) {
        JSONObject rowsObject = root.getJSONObject("rows");
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        List<MesProBatchRecordParsedCell> metadataRow = firstLegacyRowContaining(rowsObject, "产品名称");
        if (metadataRow != null) {
            rows.add(metadataRow);
        }
        int fullWidth = resolveLegacyFullWidth(root);
        int disposalLineCount = Math.max(1, countLegacyVerticalDisposalRows(rowsObject));
        rows.add(List.of(MesProBatchRecordParsedCell.builder()
                .text(buildLegacyVerticalLossReportBodyText(disposalLineCount))
                .rowSpan(1)
                .colSpan(LOSS_REPORT_COLUMN_COUNT)
                .widthPx(fullWidth)
                .heightPx(220)
                .build()));
        rows.add(List.of(MesProBatchRecordParsedCell.builder()
                .text("批准人/日期：")
                .rowSpan(1)
                .colSpan(LOSS_REPORT_COLUMN_COUNT)
                .widthPx(fullWidth)
                .heightPx(48)
                .build()));
        List<MesProBatchRecordParsedCell> footerRow = firstLegacyRowStartingWith(rowsObject, "生效日期");
        if (footerRow != null) {
            rows.add(footerRow);
        }
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(metadata.getSourceTableIndex())
                .tableTitle(metadata.getReportName())
                .rowCount(rows.size())
                .columnCount(LOSS_REPORT_COLUMN_COUNT)
                .columnWidths(readLegacyColumnWidths(root))
                .preserveSourceGrid(Boolean.TRUE)
                .rows(rows)
                .build();
    }

    private List<MesProBatchRecordParsedCell> firstLegacyRowContaining(JSONObject rowsObject, String expectedText) {
        return firstLegacyRowMatching(rowsObject, expectedText, false);
    }

    private List<MesProBatchRecordParsedCell> firstLegacyRowStartingWith(JSONObject rowsObject, String expectedText) {
        return firstLegacyRowMatching(rowsObject, expectedText, true);
    }

    private List<MesProBatchRecordParsedCell> firstLegacyRowMatching(JSONObject rowsObject, String expectedText,
                                                                    boolean startsWith) {
        if (rowsObject == null) {
            return null;
        }
        return rowsObject.keySet().stream()
                .filter(StrUtil::isNumeric)
                .map(Integer::valueOf)
                .sorted()
                .map(rowIndex -> toLegacyLossReportParsedRow(rowsObject.getJSONObject(String.valueOf(rowIndex))))
                .filter(row -> startsWith
                        ? rowText(row).startsWith(expectedText)
                        : rowText(row).contains(expectedText))
                .findFirst()
                .orElse(null);
    }

    private String buildLegacyVerticalLossReportBodyText(int disposalLineCount) {
        StringBuilder text = new StringBuilder("""
                损耗描述：
                不合格日期
                工序名称
                不合格数量
                不合格原因
                处置方式
                生产人员/日期
                检验人员
                确认/日期
                """);
        for (int index = 0; index < disposalLineCount; index++) {
            text.append("\n□报废   □其他：______________");
        }
        return text.toString();
    }

    private List<MesProBatchRecordParsedCell> toLegacyLossReportParsedRow(JSONObject rowObject) {
        JSONObject cells = rowObject == null ? null : rowObject.getJSONObject("cells");
        if (cells == null) {
            return List.of();
        }
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        cells.keySet().stream()
                .filter(StrUtil::isNumeric)
                .map(Integer::valueOf)
                .sorted()
                .forEach(columnIndex -> row.add(toLegacyLossReportParsedCell(
                        cells.getJSONObject(String.valueOf(columnIndex)), columnIndex)));
        return row;
    }

    private MesProBatchRecordParsedCell toLegacyLossReportParsedCell(JSONObject cell, int columnIndex) {
        return MesProBatchRecordParsedCell.builder()
                .text(cell == null ? "" : StrUtil.blankToDefault(cell.getString("text"), ""))
                .rowSpan(resolveJsonCellRowSpan(cell))
                .colSpan(resolveJsonCellColSpan(cell))
                .columnIndex(columnIndex)
                .logicalColumnIndex(columnIndex)
                .logicalColSpan(resolveJsonCellColSpan(cell))
                .bold(cell != null && Boolean.TRUE.equals(cell.getBoolean("bold")))
                .fontSize(resolveJsonCellFontSize(cell))
                .horizontalAlign(cell == null ? "center" : StrUtil.blankToDefault(cell.getString("align"), "center"))
                .verticalAlign(cell == null ? "middle" : StrUtil.blankToDefault(cell.getString("valign"), "middle"))
                .widthPx(resolveJsonCellWidthPx(cell))
                .heightPx(resolveJsonCellHeightPx(cell))
                .fillable(cell != null && cell.containsKey("fillForm"))
                .inputType(MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT)
                .build();
    }

    private boolean isLossReportSemanticSourceRow(List<MesProBatchRecordParsedCell> row) {
        String rowText = rowText(row);
        return rowText.contains("产品名称")
                || rowText.contains("损耗描述")
                || rowText.contains("批准人")
                || rowText.startsWith("生效日期")
                || rowText.startsWith("打印日期");
    }

    private String rowText(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (MesProBatchRecordParsedCell cell : row) {
            text.append(parsedCellText(cell));
        }
        return text.toString();
    }

    private String parsedCellText(MesProBatchRecordParsedCell cell) {
        return cell == null ? "" : StrUtil.blankToDefault(cell.getText(), "").trim();
    }

    private int resolveJsonCellRowSpan(JSONObject cell) {
        JSONArray merge = cell == null ? null : cell.getJSONArray("merge");
        return merge == null || merge.isEmpty() ? 1 : merge.getIntValue(0) + 1;
    }

    private int resolveJsonCellColSpan(JSONObject cell) {
        JSONArray merge = cell == null ? null : cell.getJSONArray("merge");
        return merge == null || merge.size() < 2 ? 1 : merge.getIntValue(1) + 1;
    }

    private int resolveJsonCellFontSize(JSONObject cell) {
        Integer fontSize = cell == null ? null : cell.getInteger("fontSize");
        return fontSize == null || fontSize <= 0 ? 10 : fontSize;
    }

    private int resolveJsonCellWidthPx(JSONObject cell) {
        Integer width = cell == null ? null : cell.getInteger("width");
        return width == null || width <= 0 ? 960 : width;
    }

    private int resolveJsonCellHeightPx(JSONObject cell) {
        Integer height = cell == null ? null : cell.getInteger("height");
        return height == null || height <= 0 ? 36 : height;
    }

    private int resolveLegacyParsedColumnCount(List<List<MesProBatchRecordParsedCell>> rows) {
        return rows.stream()
                .mapToInt(row -> row.stream().mapToInt(cell -> Math.max(1, cell.getColSpan())).sum())
                .max()
                .orElse(1);
    }

    private int resolveLegacyFullWidth(JSONObject root) {
        return readLegacyColumnWidths(root).stream()
                .filter(width -> width != null && width > 0)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private List<Integer> readLegacyColumnWidths(JSONObject root) {
        JSONObject cols = root.getJSONObject("cols");
        if (cols == null) {
            return List.of();
        }
        int len = cols.getIntValue("len");
        List<Integer> widths = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < len; columnIndex++) {
            JSONObject col = cols.getJSONObject(String.valueOf(columnIndex));
            int width = col == null ? 120 : col.getIntValue("width");
            widths.add(width <= 0 ? 120 : width);
        }
        return widths;
    }

    private JSONObject cellAt(JSONObject rows, int rowIndex, int columnIndex) {
        JSONObject row = rows == null ? null : rows.getJSONObject(String.valueOf(rowIndex));
        JSONObject cells = row == null ? null : row.getJSONObject("cells");
        return cells == null ? null : cells.getJSONObject(String.valueOf(columnIndex));
    }

    private String cellText(JSONObject cell) {
        return cell == null ? "" : StrUtil.blankToDefault(cell.getString("text"), "").trim();
    }

    private String rowText(JSONObject cells) {
        if (cells == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        cells.keySet().stream()
                .filter(StrUtil::isNumeric)
                .map(Integer::valueOf)
                .sorted()
                .forEach(columnIndex -> text.append(cellText(cells.getJSONObject(String.valueOf(columnIndex)))));
        return text.toString();
    }
}
