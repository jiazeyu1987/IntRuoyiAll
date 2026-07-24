package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class IntGyRouteMarkdownParser {

    private static final Pattern ROUTE_HEADING_PATTERN = Pattern.compile("^##\\s+\\d+\\.\\s+(.+?)\\s*$");
    private static final List<String> REQUIRED_STEP_HEADERS = List.of(
            "sequenceNo", "processCode", "processNameCn", "nodeId", "dependencyType", "isFinalProcess");

    public IntGyRouteMarkdownParser() {
    }

    public ParseResult parse(Path markdownPath) {
        try {
            return parseMarkdown(Files.readString(markdownPath, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read Markdown file: " + markdownPath, ex);
        }
    }

    public ParseResult parse(String markdown) {
        return parseMarkdown(markdown);
    }

    private ParseResult parseMarkdown(String markdown) {
        if (markdown == null) {
            throw new IllegalArgumentException("Markdown content cannot be null");
        }
        List<String> lines = markdown.lines().toList();
        List<Route> routes = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            Matcher matcher = ROUTE_HEADING_PATTERN.matcher(lines.get(index).trim());
            if (!matcher.matches()) {
                continue;
            }
            int blockEnd = findNextRouteHeading(lines, index + 1);
            routes.add(parseRouteBlock(matcher.group(1).trim(), lines.subList(index + 1, blockEnd)));
            index = blockEnd - 1;
        }
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("No IntGY route blocks found");
        }
        return new ParseResult(routes);
    }

    private int findNextRouteHeading(List<String> lines, int startIndex) {
        for (int index = startIndex; index < lines.size(); index++) {
            if (ROUTE_HEADING_PATTERN.matcher(lines.get(index).trim()).matches()) {
                return index;
            }
        }
        return lines.size();
    }

    private Route parseRouteBlock(String headingRouteCode, List<String> blockLines) {
        String routeCode = null;
        String routeName = null;
        for (String line : blockLines) {
            List<String> cells = parseTableCells(line);
            if (cells.size() != 2 || isSeparatorRow(cells)) {
                continue;
            }
            String key = cleanCell(cells.get(0));
            String value = cleanCell(cells.get(1));
            if ("routeCode".equals(key)) {
                routeCode = value;
            } else if ("routeName".equals(key)) {
                routeName = value;
            }
        }
        if (isBlank(routeCode)) {
            throw new IllegalArgumentException("Missing routeCode for route block " + headingRouteCode);
        }
        if (isBlank(routeName)) {
            throw new IllegalArgumentException("Missing routeName for route " + routeCode);
        }
        List<Step> steps = parseSteps(routeCode, blockLines);
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Missing steps for route " + routeCode);
        }
        return new Route(routeCode, routeName, steps);
    }

    private List<Step> parseSteps(String routeCode, List<String> blockLines) {
        for (int index = 0; index < blockLines.size(); index++) {
            List<String> headerCells = parseTableCells(blockLines.get(index));
            if (!isRequiredStepHeader(headerCells)) {
                continue;
            }
            Map<String, Integer> headerIndex = new LinkedHashMap<>();
            for (int i = 0; i < headerCells.size(); i++) {
                headerIndex.put(cleanCell(headerCells.get(i)), i);
            }
            List<Step> steps = new ArrayList<>();
            for (int rowIndex = index + 1; rowIndex < blockLines.size(); rowIndex++) {
                String rowLine = blockLines.get(rowIndex).trim();
                if (rowLine.isEmpty()) {
                    break;
                }
                if (!rowLine.startsWith("|")) {
                    break;
                }
                List<String> rowCells = parseTableCells(rowLine);
                if (rowCells.isEmpty() || isSeparatorRow(rowCells)) {
                    continue;
                }
                String finalProcessText = readRequiredCell(routeCode, "isFinalProcess", rowCells, headerIndex);
                steps.add(new Step(
                        parseInteger(routeCode, "sequenceNo", rowCells, headerIndex),
                        readRequiredCell(routeCode, "processCode", rowCells, headerIndex),
                        readRequiredCell(routeCode, "processNameCn", rowCells, headerIndex),
                        parseFinalProcess(routeCode, finalProcessText)));
            }
            if (steps.isEmpty()) {
                throw new IllegalArgumentException("Missing steps rows for route " + routeCode);
            }
            return steps;
        }
        throw new IllegalArgumentException("Missing steps table for route " + routeCode);
    }

    private boolean isRequiredStepHeader(List<String> headerCells) {
        if (headerCells.size() < REQUIRED_STEP_HEADERS.size()) {
            return false;
        }
        List<String> normalized = headerCells.stream().map(this::cleanCell).toList();
        return normalized.containsAll(REQUIRED_STEP_HEADERS);
    }

    private Integer parseInteger(String routeCode, String columnName, List<String> rowCells, Map<String, Integer> headerIndex) {
        String value = readRequiredCell(routeCode, columnName, rowCells, headerIndex);
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + columnName + " for route " + routeCode + ": " + value, ex);
        }
    }

    private boolean parseFinalProcess(String routeCode, String value) {
        if ("1".equals(value)) {
            return true;
        }
        if ("0".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid isFinalProcess for route " + routeCode + ": " + value);
    }

    private String readRequiredCell(String routeCode, String columnName, List<String> rowCells, Map<String, Integer> headerIndex) {
        Integer index = headerIndex.get(columnName);
        if (index == null || index >= rowCells.size()) {
            throw new IllegalArgumentException("Missing column " + columnName + " for route " + routeCode);
        }
        String value = cleanCell(rowCells.get(index));
        if (isBlank(value)) {
            throw new IllegalArgumentException("Blank " + columnName + " for route " + routeCode);
        }
        return value;
    }

    private List<String> parseTableCells(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("|")) {
            return List.of();
        }
        String body = trimmed.substring(1);
        if (body.endsWith("|")) {
            body = body.substring(0, body.length() - 1);
        }
        String[] rawCells = body.split("\\|", -1);
        List<String> cells = new ArrayList<>(rawCells.length);
        for (String rawCell : rawCells) {
            cells.add(rawCell.trim());
        }
        return cells;
    }

    private boolean isSeparatorRow(List<String> cells) {
        if (cells.isEmpty()) {
            return false;
        }
        for (String cell : cells) {
            if (!cleanCell(cell).matches("[:\\-\\s]+")) {
                return false;
            }
        }
        return true;
    }

    private String cleanCell(String value) {
        String cleaned = value == null ? "" : value.trim();
        while (cleaned.length() >= 2 && cleaned.startsWith("`") && cleaned.endsWith("`")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record ParseResult(List<Route> routes) {
    }

    public record Route(String routeCode, String routeName, List<Step> steps) {
    }

    public record Step(Integer sequenceNo, String processCode, String processNameCn, boolean finalProcess) {
    }
}
