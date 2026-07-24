package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Component
@RequiredArgsConstructor
@Slf4j
public class MesProBatchRecordRouteERecognizer implements MesProBatchRecordRouteRecognizer {

    public static final String ROUTE_KEY = MesProBatchRecordRecognitionRouteKeys.E;

    private static final int DEFAULT_COLUMN_WIDTH_PX = 160;
    private static final int DEFAULT_ROW_HEIGHT_PX = 28;
    private static final int MIN_COLUMN_WIDTH_PX = 72;
    private static final int MAX_TABLE_WIDTH_PX = 1000;
    private static final int TABLE_PADDING_PX = 20;
    private static final int TITLE_HEIGHT_PX = 40;
    private static final int CELL_PADDING_X = 8;
    private static final int CELL_PADDING_Y = 6;
    private static final int BATCH_TEMPLATE_COUNT = 5;
    private static final int BATCH_SPACING_PX = 24;
    private static final String DEFAULT_TABLE_TITLE = "\u7535\u5b50\u6279\u8bb0\u5f55\u6a21\u677f";
    private static final double MIN_NON_BLANK_ROW_COVERAGE_RATIO = 0.80D;
    private static final double MIN_COLUMN_COVERAGE_RATIO = 0.80D;

    private final MesProBatchRecordDocParser docParser;
    private final MesProBatchRecordImageParser imageParser;
    private final MesProBatchRecordFormProfileRegistry formProfileRegistry;

    @Override
    public String routeKey() {
        return ROUTE_KEY;
    }

    @Override
    public List<MesProBatchRecordParsedTable> recognize(Path sourcePath, byte[] sourceBytes, String originalFileName) {
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_e_word_bytes_empty");
        }
        List<MesProBatchRecordParsedTable> sourceTables = docParser.parse(sourceBytes);
        if (sourceTables.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_e_source_tables_empty");
        }
        log.info("Route E batch record recognition started, sourcePath={}, fileName={}, sourceTemplateCount={}",
                sourcePath, originalFileName, sourceTables.size());
        List<MesProBatchRecordParsedTable> profileSourceTables = recognizeProfileSourceWordTables(sourceTables);
        if (!profileSourceTables.isEmpty()) {
            log.info("Route E form profile recognized from source Word, fileName={}, sourceTemplateCount={}",
                    originalFileName, profileSourceTables.size());
            return profileSourceTables;
        }
        List<MesProBatchRecordParsedTable> recognizedTables = new ArrayList<>();
        for (int batchStart = 0; batchStart < sourceTables.size(); batchStart += BATCH_TEMPLATE_COUNT) {
            int batchEnd = Math.min(batchStart + BATCH_TEMPLATE_COUNT, sourceTables.size());
            List<MesProBatchRecordParsedTable> sourceBatch = sourceTables.subList(batchStart, batchEnd);
            byte[] imageBytes = renderBatchPng(sourceBatch, batchStart + 1);
            log.info("Route E rendered summary batch, batchStartTemplateIndex={}, batchTemplateCount={}, sizeBytes={}",
                    batchStart + 1, sourceBatch.size(), imageBytes.length);
            List<MesProBatchRecordParsedTable> imageTables = imageParser.parse(
                    buildRenderedBatchFileName(originalFileName, batchStart + 1, batchEnd), imageBytes);
            if (imageTables.size() != sourceBatch.size()) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID,
                        "route_e_expected_batch_table_count_" + sourceBatch.size() + "_actual_" + imageTables.size()
                                + "_batch_start_" + (batchStart + 1));
            }
            for (int batchOffset = 0; batchOffset < imageTables.size(); batchOffset++) {
                int templateIndex = batchStart + batchOffset + 1;
                MesProBatchRecordParsedTable sourceTable = sourceBatch.get(batchOffset);
                MesProBatchRecordParsedTable recognized = imageTables.get(batchOffset);
                if (recognized.getRows() == null || recognized.getRows().isEmpty()) {
                    throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID,
                            "route_e_template_rows_empty_" + templateIndex);
                }
                MesProBatchRecordParsedTable restored = restoreStructuredEntryRows(sourceTable, recognized);
                validateRecognizedStructure(sourceTable, restored, templateIndex);
                recognizedTables.add(copyRecognizedTable(templateIndex, restored));
            }
        }
        if (recognizedTables.size() != sourceTables.size()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID,
                    "route_e_expected_recognized_templates_" + sourceTables.size()
                            + "_actual_" + recognizedTables.size());
        }
        log.info("Route E batch record recognition finished, fileName={}, recognizedTemplateCount={}",
                originalFileName, recognizedTables.size());
        return recognizedTables;
    }

    private List<MesProBatchRecordParsedTable> recognizeProfileSourceWordTables(
            List<MesProBatchRecordParsedTable> sourceTables) {
        MesProBatchRecordFormProfile profile = formProfileRegistry.findSourceProfile(sourceTables).orElse(null);
        if (profile == null) {
            return List.of();
        }
        List<MesProBatchRecordParsedTable> recognizedTables = new ArrayList<>();
        for (int index = 0; index < sourceTables.size(); index++) {
            MesProBatchRecordParsedTable sourceTable = sourceTables.get(index);
            int templateIndex = index + 1;
            MesProBatchRecordParsedTable recognizedTable =
                    profile.normalizeSourceTable(templateIndex, sourceTable);
            validateRecognizedStructure(sourceTable, recognizedTable, templateIndex);
            recognizedTables.add(recognizedTable);
        }
        return recognizedTables;
    }

    protected byte[] renderTemplatePng(MesProBatchRecordParsedTable sourceTable, int templateIndex) {
        RenderPlan renderPlan = buildSummaryRenderPlan(sourceTable, templateIndex);
        int[] columnWidths = renderPlan.columnWidths();
        int[] rowHeights = renderPlan.rowHeights();
        int imageWidth = TABLE_PADDING_PX * 2 + Arrays.stream(columnWidths).sum();
        int imageHeight = TABLE_PADDING_PX * 2 + TITLE_HEIGHT_PX + Arrays.stream(rowHeights).sum();
        BufferedImage image = new BufferedImage(Math.max(imageWidth, 1), Math.max(imageHeight, 1),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

            int tableLeft = TABLE_PADDING_PX;
            int titleTop = TABLE_PADDING_PX;
            int titleWidth = Arrays.stream(columnWidths).sum();
            drawTitle(graphics, renderPlan.tableTitle(), tableLeft, titleTop, titleWidth, TITLE_HEIGHT_PX);

            int[] columnOffsets = buildOffsets(columnWidths, tableLeft);
            int[] rowOffsets = buildOffsets(rowHeights, titleTop + TITLE_HEIGHT_PX);
            for (RenderedCellPlacement placement : renderPlan.placements()) {
                drawCell(graphics, placement, columnOffsets, rowOffsets, columnWidths, rowHeights);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_e_render_png_failed_template_" + templateIndex + ":" + ex.getMessage());
        }
    }

    protected byte[] renderBatchPng(List<MesProBatchRecordParsedTable> batchTables, int batchStartTemplateIndex) {
        List<BufferedImage> images = new ArrayList<>();
        int batchWidth = 0;
        int batchHeight = 0;
        for (int index = 0; index < batchTables.size(); index++) {
            byte[] bytes = renderTemplatePng(batchTables.get(index), batchStartTemplateIndex + index);
            BufferedImage image = decodePng(bytes, batchStartTemplateIndex + index);
            images.add(image);
            batchWidth = Math.max(batchWidth, image.getWidth());
            batchHeight += image.getHeight();
            if (index > 0) {
                batchHeight += BATCH_SPACING_PX;
            }
        }
        BufferedImage batchImage = new BufferedImage(Math.max(batchWidth, 1), Math.max(batchHeight, 1),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = batchImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, batchImage.getWidth(), batchImage.getHeight());
            int top = 0;
            for (BufferedImage image : images) {
                graphics.drawImage(image, 0, top, null);
                top += image.getHeight() + BATCH_SPACING_PX;
            }
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(batchImage, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_e_render_batch_png_failed_template_" + batchStartTemplateIndex + ":" + ex.getMessage());
        }
    }

    private RenderPlan buildSummaryRenderPlan(MesProBatchRecordParsedTable table, int templateIndex) {
        List<List<MesProBatchRecordParsedCell>> rows = table.getRows();
        if (rows == null || rows.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_e_source_rows_empty_template_" + templateIndex);
        }
        List<RenderedCellPlacement> placements = buildCellPlacements(rows);
        int columnCount = placements.stream()
                .mapToInt(placement -> placement.columnIndex() + placement.colSpan())
                .max()
                .orElse(Math.max(1, table.getColumnCount() == null ? 1 : table.getColumnCount()));
        int[] columnWidths = resolveColumnWidths(columnCount, placements);
        int[] rowHeights = resolveRowHeights(rows.size(), placements, columnWidths);
        return new RenderPlan(resolveTableTitle(table), columnWidths, rowHeights, placements);
    }

    private void drawTitle(Graphics2D graphics, String title, int left, int top, int width, int height) {
        graphics.setColor(new Color(236, 240, 245));
        graphics.fillRect(left, top, width, height);
        graphics.setColor(new Color(160, 174, 192));
        graphics.drawRect(left, top, width, height);
        Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);
        graphics.setFont(titleFont);
        FontMetrics metrics = graphics.getFontMetrics(titleFont);
        String text = StrUtil.blankToDefault(title, DEFAULT_TABLE_TITLE);
        int textX = left + CELL_PADDING_X;
        int textY = top + Math.max((height - metrics.getHeight()) / 2 + metrics.getAscent(), metrics.getAscent() + 4);
        graphics.setColor(new Color(31, 41, 55));
        graphics.drawString(text, textX, textY);
    }

    private void drawCell(Graphics2D graphics, RenderedCellPlacement placement, int[] columnOffsets, int[] rowOffsets,
                          int[] columnWidths, int[] rowHeights) {
        int x = columnOffsets[placement.columnIndex()];
        int y = rowOffsets[placement.rowIndex()];
        int width = spanSize(columnWidths, placement.columnIndex(), placement.colSpan());
        int height = spanSize(rowHeights, placement.rowIndex(), placement.rowSpan());

        graphics.setColor(Color.WHITE);
        graphics.fillRect(x, y, width, height);
        graphics.setColor(new Color(148, 163, 184));
        graphics.setStroke(new BasicStroke(1f));
        graphics.drawRect(x, y, width, height);

        MesProBatchRecordParsedCell cell = placement.sourceCell();
        int fontSize = Math.max(10, cell.getFontSize());
        Font font = new Font(Font.SANS_SERIF, cell.isBold() ? Font.BOLD : Font.PLAIN, fontSize);
        graphics.setFont(font);
        graphics.setColor(new Color(15, 23, 42));
        FontMetrics metrics = graphics.getFontMetrics(font);
        int contentWidth = Math.max(12, width - CELL_PADDING_X * 2);
        List<String> lines = wrapLines(cell.getText(), metrics, contentWidth);
        int lineHeight = metrics.getHeight();
        int textBlockHeight = Math.max(lineHeight, lines.size() * lineHeight);
        int baseline = y + resolveTextTop(height, textBlockHeight, cell.getVerticalAlign()) + metrics.getAscent();
        for (String line : lines) {
            int textX = resolveTextLeft(x, width, metrics.stringWidth(line), cell.getHorizontalAlign());
            graphics.drawString(line, textX, baseline);
            baseline += lineHeight;
        }
    }

    private int[] buildOffsets(int[] sizes, int start) {
        int[] offsets = new int[sizes.length];
        int cursor = start;
        for (int index = 0; index < sizes.length; index++) {
            offsets[index] = cursor;
            cursor += sizes[index];
        }
        return offsets;
    }

    private int spanSize(int[] sizes, int startIndex, int span) {
        int total = 0;
        for (int index = 0; index < span && startIndex + index < sizes.length; index++) {
            total += sizes[startIndex + index];
        }
        return total;
    }

    private List<RenderedCellPlacement> buildCellPlacements(List<List<MesProBatchRecordParsedCell>> rows) {
        int estimatedColumnCount = Math.max(1, rows.stream()
                .filter(Objects::nonNull)
                .mapToInt(this::resolveEstimatedColumnCount)
                .max()
                .orElse(1));
        int[] occupiedColumns = new int[estimatedColumnCount];
        List<RenderedCellPlacement> placements = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            if (rowIndex > 0) {
                decrementOccupiedColumns(occupiedColumns);
            }
            List<MesProBatchRecordParsedCell> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            int columnIndex = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                while (columnIndex < occupiedColumns.length && occupiedColumns[columnIndex] > 0) {
                    columnIndex++;
                }
                int rowSpan = Math.max(1, cell.getRowSpan());
                int colSpan = Math.max(1, cell.getColSpan());
                if (columnIndex + colSpan > occupiedColumns.length) {
                    occupiedColumns = Arrays.copyOf(occupiedColumns, columnIndex + colSpan);
                }
                placements.add(new RenderedCellPlacement(cell, rowIndex, columnIndex, rowSpan, colSpan));
                for (int spanOffset = 0; spanOffset < colSpan; spanOffset++) {
                    occupiedColumns[columnIndex + spanOffset] = Math.max(occupiedColumns[columnIndex + spanOffset], rowSpan);
                }
                columnIndex += colSpan;
            }
        }
        return placements;
    }

    private int resolveEstimatedColumnCount(List<MesProBatchRecordParsedCell> row) {
        int count = 0;
        for (MesProBatchRecordParsedCell cell : row) {
            count += Math.max(1, cell.getColSpan());
        }
        return Math.max(1, count);
    }

    private void decrementOccupiedColumns(int[] occupiedColumns) {
        for (int index = 0; index < occupiedColumns.length; index++) {
            if (occupiedColumns[index] > 0) {
                occupiedColumns[index]--;
            }
        }
    }

    private int[] resolveColumnWidths(int columnCount, List<RenderedCellPlacement> placements) {
        int[] widths = new int[Math.max(1, columnCount)];
        for (RenderedCellPlacement placement : placements) {
            int cellWidth = Math.max(MIN_COLUMN_WIDTH_PX, placement.sourceCell().getWidthPx());
            int widthPerColumn = Math.max(MIN_COLUMN_WIDTH_PX,
                    (int) Math.ceil(cellWidth / (double) Math.max(1, placement.colSpan())));
            for (int offset = 0; offset < placement.colSpan() && placement.columnIndex() + offset < widths.length; offset++) {
                widths[placement.columnIndex() + offset] = Math.max(widths[placement.columnIndex() + offset], widthPerColumn);
            }
        }
        for (int index = 0; index < widths.length; index++) {
            if (widths[index] <= 0) {
                widths[index] = DEFAULT_COLUMN_WIDTH_PX;
            }
        }
        return scaleDownWidths(widths);
    }

    private int[] scaleDownWidths(int[] widths) {
        int totalWidth = Arrays.stream(widths).sum();
        if (totalWidth <= MAX_TABLE_WIDTH_PX) {
            return widths;
        }
        double scale = MAX_TABLE_WIDTH_PX / (double) totalWidth;
        int[] scaledWidths = new int[widths.length];
        for (int index = 0; index < widths.length; index++) {
            scaledWidths[index] = Math.max(MIN_COLUMN_WIDTH_PX, (int) Math.floor(widths[index] * scale));
        }
        return scaledWidths;
    }

    private void validateRecognizedStructure(MesProBatchRecordParsedTable sourceTable,
                                             MesProBatchRecordParsedTable recognizedTable,
                                             int templateIndex) {
        int sourceNonBlankRows = countNonBlankRows(sourceTable.getRows());
        int recognizedNonBlankRows = countNonBlankRows(recognizedTable.getRows());
        int requiredNonBlankRows = Math.max(1,
                (int) Math.ceil(sourceNonBlankRows * MIN_NON_BLANK_ROW_COVERAGE_RATIO));
        if (recognizedNonBlankRows < requiredNonBlankRows) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID,
                    "route_e_template_row_coverage_too_low_" + templateIndex
                            + "_source_" + sourceNonBlankRows
                            + "_actual_" + recognizedNonBlankRows);
        }

        int sourceColumnCount = resolveColumnCount(sourceTable);
        int recognizedColumnCount = resolveColumnCount(recognizedTable);
        int requiredColumnCount = Math.max(1,
                (int) Math.ceil(sourceColumnCount * MIN_COLUMN_COVERAGE_RATIO));
        if (recognizedColumnCount < requiredColumnCount) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID,
                    "route_e_template_column_coverage_too_low_" + templateIndex
                            + "_source_" + sourceColumnCount
                            + "_actual_" + recognizedColumnCount);
        }
    }

    private MesProBatchRecordParsedTable restoreStructuredEntryRows(MesProBatchRecordParsedTable sourceTable,
                                                                    MesProBatchRecordParsedTable recognizedTable) {
        if (sourceTable == null || recognizedTable == null
                || sourceTable.getRows() == null || recognizedTable.getRows() == null
                || sourceTable.getRows().size() != recognizedTable.getRows().size()) {
            return recognizedTable;
        }
        List<List<MesProBatchRecordParsedCell>> restoredRows = new ArrayList<>();
        boolean changed = false;
        for (int rowIndex = 0; rowIndex < sourceTable.getRows().size(); rowIndex++) {
            List<MesProBatchRecordParsedCell> sourceRow = sourceTable.getRows().get(rowIndex);
            List<MesProBatchRecordParsedCell> recognizedRow = recognizedTable.getRows().get(rowIndex);
            if (shouldRestoreStructuredEntryRow(sourceTable.getRows(), rowIndex, sourceRow, recognizedRow)) {
                restoredRows.add(cloneRow(sourceRow));
                changed = true;
                continue;
            }
            restoredRows.add(recognizedRow);
        }
        return changed ? copyRecognizedTable(recognizedTable, restoredRows) : recognizedTable;
    }

    private boolean shouldRestoreStructuredEntryRow(List<List<MesProBatchRecordParsedCell>> sourceRows,
                                                    int rowIndex,
                                                    List<MesProBatchRecordParsedCell> sourceRow,
                                                    List<MesProBatchRecordParsedCell> recognizedRow) {
        if (sourceRow == null || recognizedRow == null || sourceRow.isEmpty() || recognizedRow.isEmpty()) {
            return false;
        }
        if (sourceRow.size() <= recognizedRow.size()) {
            return false;
        }
        MesProBatchRecordSharedRowTypeRules.RowType rowType =
                MesProBatchRecordSharedRowTypeRules.classifyRow(sourceRows, rowIndex);
        if (rowType != MesProBatchRecordSharedRowTypeRules.RowType.FIELD
                && rowType != MesProBatchRecordSharedRowTypeRules.RowType.TABLE_HEADER) {
            return false;
        }
        if (resolveEstimatedColumnCount(sourceRow) != resolveEstimatedColumnCount(recognizedRow)) {
            return false;
        }
        return hasAlternatingLabelBlankShape(sourceRow)
                && nonBlankTexts(sourceRow).equals(nonBlankTexts(recognizedRow));
    }

    private boolean hasAlternatingLabelBlankShape(List<MesProBatchRecordParsedCell> row) {
        if (row == null || row.size() < 4 || row.size() % 2 != 0) {
            return false;
        }
        int labelCount = 0;
        for (int index = 0; index < row.size(); index++) {
            String text = textOf(row.get(index));
            if (index % 2 == 0) {
                if (text.isBlank()) {
                    return false;
                }
                labelCount++;
                continue;
            }
            if (!text.isBlank()) {
                return false;
            }
        }
        return labelCount >= 2;
    }

    private MesProBatchRecordParsedTable copyRecognizedTable(int templateIndex,
                                                             MesProBatchRecordParsedTable recognizedTable) {
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(templateIndex)
                .tableTitle(StrUtil.blankToDefault(recognizedTable.getTableTitle(), "").trim())
                .rowCount(recognizedTable.getRows() == null ? 0 : recognizedTable.getRows().size())
                .columnCount(resolveColumnCount(recognizedTable))
                .columnWidths(recognizedTable.getColumnWidths())
                .preserveSourceGrid(recognizedTable.getPreserveSourceGrid())
                .routeBSource(recognizedTable.getRouteBSource())
                .documentFrame(recognizedTable.getDocumentFrame())
                .rows(recognizedTable.getRows())
                .build();
    }

    private MesProBatchRecordParsedTable copyRecognizedTable(MesProBatchRecordParsedTable recognizedTable,
                                                             List<List<MesProBatchRecordParsedCell>> rows) {
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(recognizedTable.getSourceTableIndex())
                .sourceTopLevelTableIndex(recognizedTable.getSourceTopLevelTableIndex())
                .sourceSplitIndex(recognizedTable.getSourceSplitIndex())
                .tableTitle(recognizedTable.getTableTitle())
                .rowCount(rows.size())
                .columnCount(resolveColumnCount(rows))
                .columnWidths(recognizedTable.getColumnWidths())
                .preserveSourceGrid(recognizedTable.getPreserveSourceGrid())
                .routeBSource(recognizedTable.getRouteBSource())
                .documentFrame(recognizedTable.getDocumentFrame())
                .rows(rows)
                .build();
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

    private int countNonBlankRows(List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            boolean hasVisibleText = false;
            for (MesProBatchRecordParsedCell cell : row) {
                if (cell != null && StrUtil.isNotBlank(cell.getText())) {
                    hasVisibleText = true;
                    break;
                }
            }
            if (hasVisibleText) {
                count++;
            }
        }
        return count;
    }

    private int resolveColumnCount(MesProBatchRecordParsedTable table) {
        if (table == null) {
            return 1;
        }
        if (table.getColumnCount() != null && table.getColumnCount() > 0) {
            return table.getColumnCount();
        }
        if (table.getRows() == null || table.getRows().isEmpty()) {
            return 1;
        }
        return table.getRows().stream()
                .filter(Objects::nonNull)
                .mapToInt(this::resolveEstimatedColumnCount)
                .max()
                .orElse(1);
    }

    private int[] resolveRowHeights(int rowCount, List<RenderedCellPlacement> placements, int[] columnWidths) {
        int[] rowHeights = new int[Math.max(1, rowCount)];
        Arrays.fill(rowHeights, DEFAULT_ROW_HEIGHT_PX);
        BufferedImage measureImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = measureImage.createGraphics();
        try {
            for (RenderedCellPlacement placement : placements) {
                MesProBatchRecordParsedCell cell = placement.sourceCell();
                int fontSize = Math.max(10, cell.getFontSize());
                Font font = new Font(Font.SANS_SERIF, cell.isBold() ? Font.BOLD : Font.PLAIN, fontSize);
                graphics.setFont(font);
                FontMetrics metrics = graphics.getFontMetrics(font);
                int contentWidth = Math.max(24, spanSize(columnWidths, placement.columnIndex(), placement.colSpan()) - CELL_PADDING_X * 2);
                int requiredHeight = Math.max(cell.getHeightPx(),
                        wrapLines(cell.getText(), metrics, contentWidth).size() * metrics.getHeight() + CELL_PADDING_Y * 2);
                int heightPerRow = Math.max(DEFAULT_ROW_HEIGHT_PX,
                        (int) Math.ceil(requiredHeight / (double) Math.max(1, placement.rowSpan())));
                for (int rowOffset = 0; rowOffset < placement.rowSpan()
                        && placement.rowIndex() + rowOffset < rowHeights.length; rowOffset++) {
                    int targetRowIndex = placement.rowIndex() + rowOffset;
                    rowHeights[targetRowIndex] = Math.max(rowHeights[targetRowIndex], heightPerRow);
                }
            }
        } finally {
            graphics.dispose();
        }
        return rowHeights;
    }

    private List<String> wrapLines(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String normalized = Objects.toString(text, "").replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.isBlank()) {
            lines.add("");
            return lines;
        }
        for (String paragraph : normalized.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int index = 0; index < paragraph.length(); index++) {
                char ch = paragraph.charAt(index);
                String candidate = current + String.valueOf(ch);
                if (!current.isEmpty() && metrics.stringWidth(candidate) > maxWidth) {
                    lines.add(current.toString());
                    current.setLength(0);
                    if (!Character.isWhitespace(ch)) {
                        current.append(ch);
                    }
                } else {
                    current.append(ch);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private int resolveTextTop(int cellHeight, int textBlockHeight, String verticalAlign) {
        if ("top".equalsIgnoreCase(verticalAlign)) {
            return CELL_PADDING_Y;
        }
        if ("bottom".equalsIgnoreCase(verticalAlign)) {
            return Math.max(CELL_PADDING_Y, cellHeight - textBlockHeight - CELL_PADDING_Y);
        }
        return Math.max(CELL_PADDING_Y, (cellHeight - textBlockHeight) / 2);
    }

    private int resolveTextLeft(int cellX, int cellWidth, int textWidth, String horizontalAlign) {
        if ("center".equalsIgnoreCase(horizontalAlign)) {
            return cellX + Math.max(CELL_PADDING_X, (cellWidth - textWidth) / 2);
        }
        if ("right".equalsIgnoreCase(horizontalAlign)) {
            return cellX + Math.max(CELL_PADDING_X, cellWidth - textWidth - CELL_PADDING_X);
        }
        return cellX + CELL_PADDING_X;
    }

    private String buildRenderedFileName(String originalFileName, int templateIndex) {
        String baseName = Objects.toString(originalFileName, "").trim();
        if (baseName.toLowerCase(Locale.ROOT).endsWith(".doc")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        if (baseName.isBlank()) {
            baseName = "batch-record-route-e";
        }
        String safeBaseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return safeBaseName + "-route-e-template-" + String.format("%02d", templateIndex) + ".png";
    }

    private String buildRenderedBatchFileName(String originalFileName, int startTemplateIndex, int endTemplateIndex) {
        String baseName = Objects.toString(originalFileName, "").trim();
        if (baseName.toLowerCase(Locale.ROOT).endsWith(".doc")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        if (baseName.isBlank()) {
            baseName = "batch-record-route-e";
        }
        String safeBaseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return safeBaseName + "-route-e-batch-" + String.format("%02d", startTemplateIndex)
                + "-" + String.format("%02d", endTemplateIndex) + ".png";
    }

    private BufferedImage decodePng(byte[] bytes, int templateIndex) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IOException("decoded_image_null");
            }
            return image;
        } catch (IOException ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    "route_e_decode_png_failed_template_" + templateIndex + ":" + ex.getMessage());
        }
    }

    private String resolveTableTitle(MesProBatchRecordParsedTable table) {
        String title = Objects.toString(table.getTableTitle(), "").trim();
        if (!title.isBlank()) {
            return title;
        }
        if (table.getRows() == null) {
            return DEFAULT_TABLE_TITLE;
        }
        for (List<MesProBatchRecordParsedCell> row : table.getRows()) {
            for (MesProBatchRecordParsedCell cell : row) {
                String text = Objects.toString(cell.getText(), "").trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return DEFAULT_TABLE_TITLE;
    }

    private record RenderPlan(String tableTitle, int[] columnWidths, int[] rowHeights,
                              List<RenderedCellPlacement> placements) {
    }

    private record RenderedCellPlacement(MesProBatchRecordParsedCell sourceCell, int rowIndex, int columnIndex,
                                         int rowSpan, int colSpan) {
    }
}
