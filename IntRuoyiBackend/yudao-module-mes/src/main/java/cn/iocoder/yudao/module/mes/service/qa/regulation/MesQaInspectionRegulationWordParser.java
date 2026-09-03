package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID;

@Component
public class MesQaInspectionRegulationWordParser {

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(20\\d{2})\\s*[./年-]\\s*(\\d{1,2})\\s*[./月-]\\s*(\\d{1,2})\\s*日?");
    private static final Pattern FIRST_QUANTITY_PATTERN = Pattern.compile(
            "(?:首件|首检)\\s*[：:]?\\s*(\\d+)\\s*件");
    private static final Pattern AQL_PATTERN = Pattern.compile(
            "(?i)AQL\\s*[=＝:]\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern SERIAL_PATTERN = Pattern.compile("\\d+");
    private static final Pattern PROCESS_NAME_SEPARATOR_PATTERN = Pattern.compile("[/／]+|\\s+");
    private static final Pattern ADJACENT_ROMAN_NUMBERED_PROCESS_PATTERN = Pattern.compile(
            "[\\p{IsHan}A-Za-z0-9（）()\\-]+?(?:[ⅠⅡⅢⅣⅤⅥⅦⅧⅨⅩ]+|[IVX]+)(?=\\p{IsHan}|$)");
    private static final Pattern EXPLICIT_STANDARD_ITEM_PREFIX_PATTERN = Pattern.compile(
            "^([^，。；：:\\r\\n]{1,24}(?:检测|检验))\\s*[：:]");
    private static final String CELL_PARAGRAPH_SEPARATOR = "\uE000";
    private static final int EXPECTED_INSPECTION_COLUMNS = 8;

    public ParsedRegulation parse(byte[] content, String fileName) {
        if (content == null || content.length == 0) {
            throw invalid("上传文件不能为空");
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            HeaderMetadata header = parseHeader(document);
            LocalDate effectiveDate = parseEffectiveDate(document, header.versionNo());
            List<ParsedItem> items = parseInspectionItems(document);
            return new ParsedRegulation(header.regulationCode(), header.regulationName(),
                    header.versionNo(), effectiveDate, items, normalizeText(fileName));
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("文件不是可读取的 DOCX 文档：" + normalizeText(ex.getMessage()));
        }
    }

    private HeaderMetadata parseHeader(XWPFDocument document) {
        List<List<String>> rows = document.getHeaderList().stream()
                .flatMap(header -> header.getTables().stream())
                .flatMap(table -> table.getRows().stream())
                .map(this::physicalRowTexts)
                .toList();
        if (rows.isEmpty()) {
            throw invalid("首页页眉表格不存在");
        }

        Set<String> regulationCodes = new LinkedHashSet<>();
        Set<String> versions = new LinkedHashSet<>();
        Set<String> nameCandidates = new LinkedHashSet<>();
        for (List<String> row : rows) {
            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                String cell = row.get(cellIndex);
                collectLabelValue(row, cellIndex, List.of("文件编号", "规程编号"), regulationCodes);
                collectLabelValue(row, cellIndex, List.of("版本"), versions);
                if (isRegulationNameCandidate(cell)) {
                    nameCandidates.add(cell);
                }
            }
        }
        String regulationCode = requireUnique(regulationCodes, "首页规程编号");
        String versionNo = normalizeVersion(requireUnique(versions, "首页版本"));
        String regulationName = resolveRegulationName(nameCandidates);
        return new HeaderMetadata(regulationCode, regulationName, versionNo);
    }

    private void collectLabelValue(List<String> row, int cellIndex, List<String> labels,
                                   Set<String> values) {
        String cell = row.get(cellIndex);
        for (String label : labels) {
            if (!cell.startsWith(label)) {
                continue;
            }
            String inlineValue = normalizeText(cell.substring(label.length())
                    .replaceFirst("^[：:]", ""));
            if (!inlineValue.isEmpty()) {
                values.add(inlineValue);
                return;
            }
            for (int nextIndex = cellIndex + 1; nextIndex < row.size(); nextIndex++) {
                String next = row.get(nextIndex);
                if (!next.isEmpty()) {
                    values.add(next);
                    return;
                }
            }
        }
    }

    private static boolean isRegulationNameCandidate(String text) {
        return text.length() >= 6
                && text.contains("检验规程")
                && !Objects.equals(text, "过程检验规程")
                && !text.startsWith("文件编号")
                && !text.startsWith("规程编号");
    }

    private static String resolveRegulationName(Set<String> candidates) {
        if (candidates.isEmpty()) {
            throw invalid("首页规程名称不存在");
        }
        int longestLength = candidates.stream().mapToInt(String::length).max().orElse(0);
        List<String> longest = candidates.stream()
                .filter(candidate -> candidate.length() == longestLength)
                .toList();
        if (longest.size() != 1) {
            throw invalid("首页规程名称不唯一：" + longest);
        }
        return longest.get(0);
    }

    private LocalDate parseEffectiveDate(XWPFDocument document, String versionNo) {
        Set<LocalDate> effectiveDates = new LinkedHashSet<>();
        for (XWPFTable table : document.getTables()) {
            List<List<String>> grid = buildLogicalGrid(table);
            for (int headerIndex = 0; headerIndex < grid.size(); headerIndex++) {
                List<String> header = grid.get(headerIndex);
                int versionColumn = findExactColumn(header, "版本");
                int effectiveDateColumn = findExactColumn(header, "生效日期");
                if (versionColumn < 0 || effectiveDateColumn < 0) {
                    continue;
                }
                for (int rowIndex = headerIndex + 1; rowIndex < grid.size(); rowIndex++) {
                    List<String> row = grid.get(rowIndex);
                    if (!Objects.equals(normalizeVersion(valueAt(row, versionColumn)), versionNo)) {
                        continue;
                    }
                    effectiveDates.add(parseDate(valueAt(row, effectiveDateColumn)));
                }
            }
        }
        if (effectiveDates.isEmpty()) {
            throw invalid("修订记录中不存在版本 " + versionNo + " 的生效日期");
        }
        if (effectiveDates.size() != 1) {
            throw invalid("修订记录中版本 " + versionNo + " 的生效日期不唯一：" + effectiveDates);
        }
        return effectiveDates.iterator().next();
    }

    private List<ParsedItem> parseInspectionItems(XWPFDocument document) {
        List<InspectionTable> matches = new ArrayList<>();
        for (XWPFTable table : document.getTables()) {
            List<List<String>> grid = buildLogicalGrid(table);
            for (int rowIndex = 0; rowIndex < grid.size(); rowIndex++) {
                InspectionColumns columns = resolveInspectionColumns(grid.get(rowIndex));
                if (columns != null) {
                    matches.add(new InspectionTable(grid, rowIndex, columns));
                    break;
                }
            }
        }
        if (matches.isEmpty()) {
            throw invalid("未找到包含检验项目、接受标准、检验方法、检验器具及设备和抽样方案的检验内容表格");
        }
        if (matches.size() != 1) {
            throw invalid("检验内容表格不唯一：" + matches.size());
        }

        InspectionTable inspectionTable = matches.get(0);
        List<ParsedItem> items = new ArrayList<>();
        for (int rowIndex = inspectionTable.headerRowIndex() + 1;
             rowIndex < inspectionTable.grid().size(); rowIndex++) {
            List<String> row = inspectionTable.grid().get(rowIndex);
            String serial = valueAt(row, inspectionTable.columns().serialColumn());
            if (serial.isEmpty() && row.stream()
                    .map(MesQaInspectionRegulationWordParser::displayCellText)
                    .allMatch(String::isEmpty)) {
                continue;
            }
            if (serial.startsWith("备注")) {
                continue;
            }
            if (resolveInspectionColumns(row) != null) {
                continue;
            }
            if (!serial.isEmpty() && !SERIAL_PATTERN.matcher(serial).matches()) {
                throw invalid("检验内容表第 " + (rowIndex + 1) + " 行序号无效：" + serial);
            }
            String rowIdentity = serial.isEmpty() ? String.valueOf(items.size() + 1) : serial;

            InspectionColumns columns = inspectionTable.columns();
            String processName = processValueAt(row, columns.processColumn());
            List<String> processNames = splitProcessNames(processName);
            List<String> itemPath = new ArrayList<>();
            for (int itemColumn = columns.processColumn() + 1;
                 itemColumn < columns.standardColumn(); itemColumn++) {
                String segment = valueAt(row, itemColumn);
                if (!segment.isEmpty()
                        && (itemPath.isEmpty() || !Objects.equals(itemPath.get(itemPath.size() - 1), segment))) {
                    itemPath.add(segment);
                }
            }
            String itemName = String.join(" / ", itemPath);
            String standardText = valueAt(row, columns.standardColumn());
            String inspectionMethod = valueAt(row, columns.methodColumn());
            String inspectionTool = valueAt(row, columns.toolColumn());
            String samplingPlanText = valueAt(row, columns.samplingColumn());
            if (processNames.isEmpty() || itemName.isEmpty() || standardText.isEmpty()
                    || inspectionMethod.isEmpty() || inspectionTool.isEmpty() || samplingPlanText.isEmpty()) {
                throw invalid("检验内容表第 " + (rowIndex + 1) + " 行字段不完整");
            }
            SamplingRule samplingRule = parseSamplingRule(samplingPlanText, rowIdentity);
            for (String resolvedProcessName : processNames) {
                items.add(new ParsedItem(resolvedProcessName, itemName, standardText, inspectionMethod,
                        inspectionTool, samplingPlanText, samplingRule.firstInspectionQuantity(),
                        samplingRule.patrolInspectionRatio()));
            }
        }
        if (items.isEmpty()) {
            throw invalid("检验内容表格没有有效检验项目");
        }
        return disambiguateRepeatedItems(items);
    }

    private static List<ParsedItem> disambiguateRepeatedItems(List<ParsedItem> items) {
        Map<String, List<Integer>> indexesByItem = new HashMap<>();
        for (int index = 0; index < items.size(); index++) {
            ParsedItem item = items.get(index);
            String key = normalizeText(item.processName()) + "\u0000" + normalizeText(item.itemName());
            indexesByItem.computeIfAbsent(key, ignored -> new ArrayList<>()).add(index);
        }

        List<ParsedItem> resolvedItems = new ArrayList<>(items);
        for (List<Integer> indexes : indexesByItem.values()) {
            if (indexes.size() < 2) {
                continue;
            }
            List<String> explicitChildNames = indexes.stream()
                    .map(index -> explicitStandardItemPrefix(items.get(index).standardText()))
                    .toList();
            if (explicitChildNames.stream().anyMatch(String::isEmpty)
                    || new LinkedHashSet<>(explicitChildNames).size() != indexes.size()) {
                continue;
            }
            for (int offset = 0; offset < indexes.size(); offset++) {
                int index = indexes.get(offset);
                ParsedItem source = items.get(index);
                String childName = explicitChildNames.get(offset);
                resolvedItems.set(index, new ParsedItem(
                        source.processName(), source.itemName() + " / " + childName,
                        source.standardText(), source.inspectionMethod(), source.inspectionTool(),
                        source.samplingPlanText(), source.firstInspectionQuantity(),
                        source.patrolInspectionRatio()));
            }
        }
        return List.copyOf(resolvedItems);
    }

    private static String explicitStandardItemPrefix(String standardText) {
        Matcher matcher = EXPLICIT_STANDARD_ITEM_PREFIX_PATTERN.matcher(normalizeText(standardText));
        return matcher.find() ? normalizeText(matcher.group(1)) : "";
    }

    private static List<String> splitProcessNames(String processName) {
        String normalized = normalizeText(processName);
        if (normalized.isEmpty()) {
            return List.of();
        }
        Set<String> processNames = new LinkedHashSet<>();
        for (String segment : PROCESS_NAME_SEPARATOR_PATTERN.split(normalized)) {
            String resolved = normalizeText(segment);
            if (resolved.isEmpty()) {
                continue;
            }
            List<String> adjacentRomanNumberedProcesses =
                    splitAdjacentRomanNumberedProcesses(resolved);
            if (adjacentRomanNumberedProcesses.isEmpty()) {
                processNames.add(resolved);
            } else {
                processNames.addAll(adjacentRomanNumberedProcesses);
            }
        }
        return List.copyOf(processNames);
    }

    private static List<String> splitAdjacentRomanNumberedProcesses(String segment) {
        Matcher matcher = ADJACENT_ROMAN_NUMBERED_PROCESS_PATTERN.matcher(segment);
        List<String> processes = new ArrayList<>();
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() != cursor) {
                return List.of();
            }
            processes.add(matcher.group());
            cursor = matcher.end();
        }
        if (cursor == segment.length() && processes.size() > 1) {
            return List.copyOf(processes);
        }
        return List.of();
    }

    private static SamplingRule parseSamplingRule(String samplingPlanText, String serial) {
        Set<Integer> firstQuantities = collectIntegers(FIRST_QUANTITY_PATTERN, samplingPlanText);
        if (firstQuantities.size() > 1) {
            throw invalid("检验项目序号 " + serial + " 的首检数量不唯一：" + firstQuantities);
        }
        Integer firstQuantity = firstQuantities.stream().findFirst().orElse(null);
        if (firstQuantity != null && firstQuantity <= 0) {
            throw invalid("检验项目序号 " + serial + " 的首检数量必须大于 0");
        }

        Set<BigDecimal> patrolRatios = collectDecimals(AQL_PATTERN, samplingPlanText);
        if (patrolRatios.size() != 1) {
            throw invalid("检验项目序号 " + serial + " 必须包含唯一有效的 AQL 比例");
        }
        BigDecimal patrolRatio = patrolRatios.iterator().next();
        if (patrolRatio.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalid("检验项目序号 " + serial + " 的 AQL 比例必须大于 0");
        }
        return new SamplingRule(firstQuantity, patrolRatio);
    }

    private static Set<Integer> collectIntegers(Pattern pattern, String text) {
        Set<Integer> values = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            values.add(Integer.parseInt(matcher.group(1)));
        }
        return values;
    }

    private static Set<BigDecimal> collectDecimals(Pattern pattern, String text) {
        Set<BigDecimal> values = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            values.add(new BigDecimal(matcher.group(1)).stripTrailingZeros());
        }
        return values;
    }

    private static LocalDate parseDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw invalid("生效日期格式无效：" + text);
        }
        try {
            return LocalDate.of(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        } catch (DateTimeException ex) {
            throw invalid("生效日期无效：" + text);
        }
    }

    private InspectionColumns resolveInspectionColumns(List<String> row) {
        int serialColumn = findExactColumn(row, "序号");
        int inspectionItemColumn = findExactColumn(row, "检验项目");
        int standardColumn = findExactColumn(row, "接受标准");
        int methodColumn = findExactColumn(row, "检验方法");
        int toolColumn = findExactColumn(row, "检验器具及设备");
        int samplingColumn = findExactColumn(row, "抽样方案");
        if (serialColumn < 0 || inspectionItemColumn < 0 || standardColumn < 0
                || methodColumn < 0 || toolColumn < 0 || samplingColumn < 0) {
            return null;
        }
        if (!(serialColumn < inspectionItemColumn && inspectionItemColumn < standardColumn
                && standardColumn < methodColumn && methodColumn < toolColumn
                && toolColumn < samplingColumn)) {
            throw invalid("检验内容表头列顺序无效");
        }
        if (samplingColumn + 1 != EXPECTED_INSPECTION_COLUMNS) {
            throw invalid("检验内容表必须包含 8 个逻辑列，实际末列位置为 " + (samplingColumn + 1));
        }
        return new InspectionColumns(serialColumn, inspectionItemColumn,
                standardColumn, methodColumn, toolColumn, samplingColumn);
    }

    private List<List<String>> buildLogicalGrid(XWPFTable table) {
        List<List<String>> grid = new ArrayList<>();
        Map<Integer, String> verticalMergeValues = new HashMap<>();
        for (XWPFTableRow row : table.getRows()) {
            List<String> logicalRow = new ArrayList<>();
            int logicalColumn = 0;
            for (XWPFTableCell cell : row.getTableCells()) {
                int span = gridSpan(cell);
                VerticalMerge verticalMerge = verticalMerge(cell);
                String cellText = cellText(cell);
                for (int offset = 0; offset < span; offset++) {
                    int targetColumn = logicalColumn + offset;
                    String resolvedText = cellText;
                    if (verticalMerge == VerticalMerge.CONTINUE) {
                        resolvedText = verticalMergeValues.getOrDefault(targetColumn, "");
                    } else if (verticalMerge == VerticalMerge.RESTART) {
                        verticalMergeValues.put(targetColumn, cellText);
                    } else {
                        verticalMergeValues.remove(targetColumn);
                    }
                    ensureSize(logicalRow, targetColumn + 1);
                    logicalRow.set(targetColumn, resolvedText);
                }
                logicalColumn += span;
            }
            grid.add(List.copyOf(logicalRow));
        }
        return grid;
    }

    private List<String> physicalRowTexts(XWPFTableRow row) {
        return row.getTableCells().stream()
                .map(this::cellText)
                .map(MesQaInspectionRegulationWordParser::displayCellText)
                .toList();
    }

    private String cellText(XWPFTableCell cell) {
        if (cell == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            String paragraphText = normalizeText(paragraph.getText());
            if (paragraphText.isEmpty()) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append(CELL_PARAGRAPH_SEPARATOR);
            }
            text.append(paragraphText);
        }
        return text.toString();
    }

    private static int gridSpan(XWPFTableCell cell) {
        CTTcPr properties = cell.getCTTc().getTcPr();
        if (properties == null || !properties.isSetGridSpan()) {
            return 1;
        }
        return Math.max(1, properties.getGridSpan().getVal().intValue());
    }

    private static VerticalMerge verticalMerge(XWPFTableCell cell) {
        CTTcPr properties = cell.getCTTc().getTcPr();
        if (properties == null || !properties.isSetVMerge()) {
            return VerticalMerge.NONE;
        }
        Object value = properties.getVMerge().getVal();
        if (value == null || STMerge.CONTINUE.equals(value)) {
            return VerticalMerge.CONTINUE;
        }
        return VerticalMerge.RESTART;
    }

    private static void ensureSize(List<String> row, int size) {
        while (row.size() < size) {
            row.add("");
        }
    }

    private static int findExactColumn(List<String> row, String expected) {
        for (int index = 0; index < row.size(); index++) {
            if (Objects.equals(displayCellText(row.get(index)), expected)) {
                return index;
            }
        }
        return -1;
    }

    private static String valueAt(List<String> row, int index) {
        return displayCellText(rawValueAt(row, index));
    }

    private static String processValueAt(List<String> row, int index) {
        return normalizeText(rawValueAt(row, index).replace(CELL_PARAGRAPH_SEPARATOR, ""));
    }

    private static String rawValueAt(List<String> row, int index) {
        return index >= 0 && index < row.size() ? row.get(index) : "";
    }

    private static String displayCellText(String text) {
        return normalizeText(Objects.toString(text, "").replace(CELL_PARAGRAPH_SEPARATOR, " "));
    }

    private static String requireUnique(Set<String> values, String label) {
        if (values.isEmpty()) {
            throw invalid(label + "不存在");
        }
        if (values.size() != 1) {
            throw invalid(label + "不唯一：" + values);
        }
        return values.iterator().next();
    }

    static String normalizeText(String text) {
        return text == null ? "" : text.replace('\u00A0', ' ')
                .replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeVersion(String versionNo) {
        return normalizeText(versionNo).replaceAll("\\s+", "");
    }

    private static ServiceException invalid(String detail) {
        return exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID, detail);
    }

    public record ParsedRegulation(String regulationCode, String regulationName,
                                   String versionNo, LocalDate effectiveDate,
                                   List<ParsedItem> items, String fileName) {
    }

    public record ParsedItem(String processName, String itemName, String standardText,
                             String inspectionMethod, String inspectionTool,
                             String samplingPlanText, Integer firstInspectionQuantity,
                             BigDecimal patrolInspectionRatio) {
    }

    private record HeaderMetadata(String regulationCode, String regulationName, String versionNo) {
    }

    private record InspectionTable(List<List<String>> grid, int headerRowIndex,
                                   InspectionColumns columns) {
    }

    private record InspectionColumns(int serialColumn, int processColumn, int standardColumn,
                                     int methodColumn, int toolColumn, int samplingColumn) {
    }

    private record SamplingRule(Integer firstInspectionQuantity, BigDecimal patrolInspectionRatio) {
    }

    private enum VerticalMerge {
        NONE,
        RESTART,
        CONTINUE
    }
}
