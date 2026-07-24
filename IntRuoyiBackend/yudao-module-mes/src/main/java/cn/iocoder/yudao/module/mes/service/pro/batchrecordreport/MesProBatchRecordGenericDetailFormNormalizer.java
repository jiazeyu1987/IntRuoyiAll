package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Expands Word tables where one merged narrative cell encodes a repeated detail form.
 */
final class MesProBatchRecordGenericDetailFormNormalizer {

    private static final int MIN_COLUMN_WIDTH_PX = 72;

    private final Spec spec;

    MesProBatchRecordGenericDetailFormNormalizer(Spec spec) {
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    boolean supportsSourceTable(MesProBatchRecordParsedTable table) {
        return table != null && table.getRows() != null && !table.getRows().isEmpty()
                && containsAll(table, spec.sourceRequiredTexts())
                && isMergedDetailBodyCell(firstBodyCell(table.getRows()));
    }

    List<List<MesProBatchRecordParsedCell>> normalizeSourceRows(MesProBatchRecordParsedTable sourceTable) {
        List<List<MesProBatchRecordParsedCell>> sourceRows = sourceTable == null ? null : sourceTable.getRows();
        MesProBatchRecordParsedCell bodyCell = firstBodyCell(sourceRows);
        if (!isMergedDetailBodyCell(bodyCell)) {
            return cloneRows(sourceRows);
        }
        MesProBatchRecordParsedCell footerCell = firstCellContaining(sourceRows, spec.footerAnchorText());
        int detailRowCount = countDetailLines(bodyCell);
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        if (sourceRows != null && !sourceRows.isEmpty()) {
            rows.add(markAlternatingBlankCellsFillable(sourceRows.get(0)));
        }
        rows.add(descriptionRow(bodyCell));
        rows.add(detailHeaderRow(bodyCell));
        for (int index = 0; index < detailRowCount; index++) {
            rows.add(detailDataRow(bodyCell, index, detailRowCount));
        }
        rows.add(footerRow(footerCell == null ? bodyCell : footerCell));
        appendSourceTailRows(sourceRows, rows);
        return rows;
    }

    private boolean containsAll(MesProBatchRecordParsedTable table, List<String> expectedTexts) {
        for (String expectedText : expectedTexts) {
            if (!tableContainsText(table, expectedText)) {
                return false;
            }
        }
        return true;
    }

    private boolean tableContainsText(MesProBatchRecordParsedTable table, String expectedText) {
        if (StrUtil.isBlank(expectedText)) {
            return true;
        }
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            if (row == null) {
                continue;
            }
            for (MesProBatchRecordParsedCell cell : row) {
                if (textOf(cell).contains(expectedText)) {
                    return true;
                }
            }
        }
        return false;
    }

    private MesProBatchRecordParsedCell firstBodyCell(List<List<MesProBatchRecordParsedCell>> rows) {
        return firstCellContaining(rows, spec.bodyAnchorText());
    }

    private MesProBatchRecordParsedCell firstCellContaining(List<List<MesProBatchRecordParsedCell>> rows,
                                                           String expectedText) {
        if (rows == null || StrUtil.isBlank(expectedText)) {
            return null;
        }
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null) {
                continue;
            }
            for (MesProBatchRecordParsedCell cell : row) {
                if (textOf(cell).contains(expectedText)) {
                    return cell;
                }
            }
        }
        return null;
    }

    private boolean isMergedDetailBodyCell(MesProBatchRecordParsedCell bodyCell) {
        String text = textOf(bodyCell);
        if (bodyCell == null || Math.max(1, bodyCell.getColSpan()) < 4) {
            return false;
        }
        for (String marker : spec.bodyRequiredTexts()) {
            if (!text.contains(marker)) {
                return false;
            }
        }
        return true;
    }

    private List<MesProBatchRecordParsedCell> markAlternatingBlankCellsFillable(List<MesProBatchRecordParsedCell> sourceRow) {
        if (sourceRow == null || sourceRow.isEmpty()) {
            return List.of();
        }
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        for (int index = 0; index < sourceRow.size(); index++) {
            MesProBatchRecordParsedCell source = sourceRow.get(index);
            if (index % 2 == 1 && textOf(source).isBlank()) {
                row.add(fillableCell("", Math.max(1, source.getColSpan()), source.getWidthPx(), source.getHeightPx(),
                        MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT));
            } else {
                row.add(cloneCell(source));
            }
        }
        return row;
    }

    private List<MesProBatchRecordParsedCell> descriptionRow(MesProBatchRecordParsedCell sourceCell) {
        return List.of(MesProBatchRecordParsedCell.builder()
                .text(spec.descriptionLabel())
                .rowSpan(1)
                .colSpan(spec.columnCount())
                .widthPx(resolveFullWidth(sourceCell))
                .heightPx(28)
                .bold(true)
                .horizontalAlign("left")
                .verticalAlign("middle")
                .build());
    }

    private List<MesProBatchRecordParsedCell> detailHeaderRow(MesProBatchRecordParsedCell sourceCell) {
        int width = resolveFullWidth(sourceCell);
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        for (HeaderCellSpec headerCell : spec.headerCells()) {
            row.add(labelCell(headerCell.text(), headerCell.colSpan(),
                    width * headerCell.colSpan() / spec.columnCount(), 44));
        }
        return row;
    }

    private List<MesProBatchRecordParsedCell> detailDataRow(MesProBatchRecordParsedCell sourceCell,
                                                            int detailIndex,
                                                            int detailRowCount) {
        int width = resolveFullWidth(sourceCell);
        List<MesProBatchRecordParsedCell> row = new ArrayList<>();
        for (DetailCellSpec detailCell : spec.detailCells()) {
            if (detailCell.spanAllDetailRows() && detailIndex > 0) {
                continue;
            }
            if (detailCell.kind() == DetailCellKind.TRAILING_UNDERLINE_CHOICES) {
                row.addAll(MesProBatchRecordFillablePatternSupport.splitTrailingUnderlineFillable(
                        detailCell.text(),
                        width * detailCell.colSpan() / spec.columnCount(),
                        32,
                        detailCell.inputType(),
                        detailCell.reviewedCheckboxChoices()));
                continue;
            }
            int rowSpan = detailCell.spanAllDetailRows() ? detailRowCount : 1;
            row.add(fillableCell("", detailCell.colSpan(),
                    width * detailCell.colSpan() / spec.columnCount(),
                    32 * rowSpan,
                    rowSpan,
                    detailCell.inputType()));
        }
        return row;
    }

    private List<MesProBatchRecordParsedCell> footerRow(MesProBatchRecordParsedCell sourceCell) {
        int width = resolveFullWidth(sourceCell);
        int height = sourceCell == null || sourceCell.getHeightPx() <= 0 ? 48 : sourceCell.getHeightPx();
        return List.of(
                labelCell(spec.footerLabel(), spec.footerLabelColSpan(),
                        width * spec.footerLabelColSpan() / spec.columnCount(), height),
                fillableCell("", spec.footerInputColSpan(),
                        width * spec.footerInputColSpan() / spec.columnCount(), height,
                        MesProBatchRecordReportShapeRules.INPUT_TYPE_INPUT)
        );
    }

    private MesProBatchRecordParsedCell labelCell(String text, int colSpan, int widthPx, int heightPx) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(1)
                .colSpan(colSpan)
                .widthPx(Math.max(MIN_COLUMN_WIDTH_PX, widthPx))
                .heightPx(heightPx)
                .bold(true)
                .horizontalAlign("center")
                .verticalAlign("middle")
                .build();
    }

    private MesProBatchRecordParsedCell fillableCell(String text, int colSpan, int widthPx, int heightPx,
                                                     String inputType) {
        return fillableCell(text, colSpan, widthPx, heightPx, 1, inputType);
    }

    private MesProBatchRecordParsedCell fillableCell(String text, int colSpan, int widthPx, int heightPx,
                                                     int rowSpan, String inputType) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(rowSpan)
                .colSpan(colSpan)
                .widthPx(Math.max(MIN_COLUMN_WIDTH_PX, widthPx))
                .heightPx(heightPx)
                .fillable(true)
                .placeholder("")
                .inputType(inputType)
                .horizontalAlign("left")
                .verticalAlign("middle")
                .build();
    }

    private int countDetailLines(MesProBatchRecordParsedCell bodyCell) {
        String text = textOf(bodyCell);
        int count = 0;
        int start = 0;
        while (start >= 0 && start < text.length()) {
            int index = text.indexOf(spec.detailLineMarker(), start);
            if (index < 0) {
                break;
            }
            count++;
            start = index + spec.detailLineMarker().length();
        }
        return Math.max(1, count);
    }

    private int resolveFullWidth(MesProBatchRecordParsedCell sourceCell) {
        return sourceCell == null || sourceCell.getWidthPx() <= 0 ? 960 : sourceCell.getWidthPx();
    }

    private void appendSourceTailRows(List<List<MesProBatchRecordParsedCell>> sourceRows,
                                      List<List<MesProBatchRecordParsedCell>> rows) {
        if (sourceRows == null) {
            return;
        }
        for (List<MesProBatchRecordParsedCell> sourceRow : sourceRows) {
            if (sourceRow == null || sourceRow.isEmpty()) {
                continue;
            }
            String rowText = String.join("\n", nonBlankTexts(sourceRow));
            if (rowText.contains(spec.bodyAnchorText()) || rowText.contains(spec.footerAnchorText())) {
                continue;
            }
            if (rowText.startsWith("生效日期") || rowText.startsWith("打印日期")) {
                rows.add(cloneRow(sourceRow));
            }
        }
    }

    private List<List<MesProBatchRecordParsedCell>> cloneRows(List<List<MesProBatchRecordParsedCell>> sourceRows) {
        List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
        if (sourceRows == null) {
            return rows;
        }
        for (List<MesProBatchRecordParsedCell> row : sourceRows) {
            rows.add(row == null ? List.of() : cloneRow(row));
        }
        return rows;
    }

    private List<MesProBatchRecordParsedCell> cloneRow(List<MesProBatchRecordParsedCell> row) {
        List<MesProBatchRecordParsedCell> clones = new ArrayList<>();
        for (MesProBatchRecordParsedCell cell : row) {
            clones.add(cloneCell(cell));
        }
        return clones;
    }

    private MesProBatchRecordParsedCell cloneCell(MesProBatchRecordParsedCell source) {
        if (source == null) {
            return null;
        }
        return MesProBatchRecordParsedCell.builder()
                .text(source.getText())
                .rowSpan(source.getRowSpan())
                .colSpan(source.getColSpan())
                .columnIndex(source.getColumnIndex())
                .logicalColumnIndex(source.getLogicalColumnIndex())
                .logicalColSpan(source.getLogicalColSpan())
                .bold(source.isBold())
                .fontSize(source.getFontSize())
                .horizontalAlign(source.getHorizontalAlign())
                .verticalAlign(source.getVerticalAlign())
                .widthPx(source.getWidthPx())
                .heightPx(source.getHeightPx())
                .fillable(source.isFillable())
                .visualBlank(source.isVisualBlank())
                .borderless(source.isBorderless())
                .diagonalSlash(source.isDiagonalSlash())
                .topBorderStyle(source.getTopBorderStyle())
                .bottomBorderStyle(source.getBottomBorderStyle())
                .leftBorderStyle(source.getLeftBorderStyle())
                .rightBorderStyle(source.getRightBorderStyle())
                .backgroundColor(source.getBackgroundColor())
                .documentFrameRole(source.getDocumentFrameRole())
                .placeholder(source.getPlaceholder())
                .inputType(source.getInputType())
                .build();
    }

    private List<String> nonBlankTexts(List<MesProBatchRecordParsedCell> row) {
        List<String> texts = new ArrayList<>();
        if (row == null) {
            return texts;
        }
        for (MesProBatchRecordParsedCell cell : row) {
            String text = textOf(cell);
            if (!text.isBlank()) {
                texts.add(text);
            }
        }
        return texts;
    }

    private String textOf(MesProBatchRecordParsedCell cell) {
        return cell == null || cell.getText() == null ? "" : cell.getText().trim();
    }

    record Spec(int columnCount,
                List<String> sourceRequiredTexts,
                String bodyAnchorText,
                List<String> bodyRequiredTexts,
                String descriptionLabel,
                List<HeaderCellSpec> headerCells,
                List<DetailCellSpec> detailCells,
                String detailLineMarker,
                String footerAnchorText,
                String footerLabel,
                int footerLabelColSpan,
                int footerInputColSpan) {
    }

    record HeaderCellSpec(String text, int colSpan) {
    }

    record DetailCellSpec(DetailCellKind kind,
                          String text,
                          int colSpan,
                          String inputType,
                          boolean spanAllDetailRows,
                          boolean reviewedCheckboxChoices) {

        static DetailCellSpec fillable(int colSpan, String inputType, boolean spanAllDetailRows) {
            return new DetailCellSpec(DetailCellKind.FILLABLE, "", colSpan, inputType, spanAllDetailRows, false);
        }

        static DetailCellSpec trailingUnderlineChoices(String text, int colSpan, String inputType) {
            return new DetailCellSpec(DetailCellKind.TRAILING_UNDERLINE_CHOICES, text, colSpan, inputType, false,
                    false);
        }

        static DetailCellSpec reviewedTrailingUnderlineChoices(String text, int colSpan, String inputType) {
            return new DetailCellSpec(DetailCellKind.TRAILING_UNDERLINE_CHOICES, text, colSpan, inputType, false,
                    true);
        }
    }

    enum DetailCellKind {
        FILLABLE,
        TRAILING_UNDERLINE_CHOICES
    }
}
