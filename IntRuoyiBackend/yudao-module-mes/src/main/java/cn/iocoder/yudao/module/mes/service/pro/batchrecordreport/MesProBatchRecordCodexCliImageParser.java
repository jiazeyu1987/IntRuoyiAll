package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Component
@Slf4j
public class MesProBatchRecordCodexCliImageParser implements MesProBatchRecordImageParser {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".bmp");
    private static final String IMAGE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "confidence": { "type": "number" },
                "issues": {
                  "type": "array",
                  "items": { "type": "string" }
                },
                "tables": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "title": { "type": "string" },
                      "templateName": { "type": "string" },
                      "productName": { "type": "string" },
                      "rows": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "cells": {
                              "type": "array",
                              "items": {
                                "type": "object",
                                "properties": {
                                  "text": { "type": "string" },
                                  "rowSpan": { "type": "integer", "minimum": 1 },
                                  "colSpan": { "type": "integer", "minimum": 1 }
                                },
                                "required": ["text", "rowSpan", "colSpan"],
                                "additionalProperties": false
                              }
                            }
                          },
                          "required": ["cells"],
                          "additionalProperties": false
                        }
                      }
                    },
                    "required": ["title", "templateName", "productName", "rows"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["confidence", "issues", "tables"],
              "additionalProperties": false
            }
            """;
    private static final String PROMPT = """
            你正在把一张电子批记录图片识别成系统报表表格。
            只返回符合 schema 的 JSON，不要输出解释。
            规则：
            1. tables 按图片从上到下排序。
            2. 每个 table 的 title 使用该区域最明显的表格标题。
            3. templateName 与 title 保持一致。
            4. productName 能确认就填写，不能确认就输出空字符串。
            5. rows 按从上到下排列，cells 按从左到右排列。
            6. cell.text 保留图片中能确认的原文；不能确认时输出空字符串，不要猜测。
            7. rowSpan 和 colSpan 至少为 1；遇到明显合并单元格时要体现出来。
            8. confidence 输出 0 到 1 之间的小数。
            9. issues 列出不确定点；没有不确定点时返回空数组。
            10. 不要把表格最右侧外边框、页面边距或无文字空白竖线识别成额外单元格。
            """;
    private static final double DEFAULT_MIN_CONFIDENCE = 0.60D;
    private static final long DEFAULT_TIMEOUT_MS = 600000L;
    private static final long STDOUT_DRAIN_TIMEOUT_MS = 10000L;

    @Value("${yudao.mes.batch-record-report.image.codex-command:}")
    private String codexCommand;
    @Value("${yudao.mes.batch-record-report.image.codex-model:}")
    private String codexModel;
    @Value("${yudao.mes.batch-record-report.image.codex-reasoning-effort:minimal}")
    private String codexReasoningEffort;
    @Value("${yudao.mes.batch-record-report.image.codex-working-directory:}")
    private String codexWorkingDirectory;
    @Value("${yudao.mes.batch-record-report.image.codex-timeout-ms:" + DEFAULT_TIMEOUT_MS + "}")
    private long timeoutMs;
    @Value("${yudao.mes.batch-record-report.image.min-confidence:" + DEFAULT_MIN_CONFIDENCE + "}")
    private double minConfidence;

    @Override
    public List<MesProBatchRecordParsedTable> parse(String originalFileName, byte[] bytes) {
        String extension = normalizeExtension(originalFileName);
        String fileName = Objects.toString(originalFileName, "").trim();
        long startNanos = System.nanoTime();
        Path tempDir = null;
        ExecutorService stdoutExecutor = Executors.newSingleThreadExecutor();
        try {
            tempDir = Files.createTempDirectory("mes-batch-record-image-");
            Path imagePath = tempDir.resolve("source" + extension);
            Path schemaPath = tempDir.resolve("schema.json");
            Files.write(imagePath, bytes);
            Files.writeString(schemaPath, IMAGE_SCHEMA, StandardCharsets.UTF_8);

            List<String> command = buildCommand(imagePath, schemaPath);
            log.info("Batch record image recognition started, fileName={}, sizeBytes={}, timeoutMs={}, model={}, codexWorkDir={}, tempDir={}",
                    fileName, bytes.length, timeoutMs, StrUtil.blankToDefault(codexModel, "<default>"),
                    StrUtil.blankToDefault(codexWorkingDirectory, "<unset>"), tempDir);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(tempDir.toFile());
            Process process;
            try {
                process = processBuilder.start();
            } catch (IOException ex) {
                throw new IOException("codex_cli_process_start_failed:" + ex.getMessage(), ex);
            }
            closeStdin(process);

            long pid = process.pid();
            log.info("Batch record image recognition process started, fileName={}, pid={}, command={}",
                    fileName, pid, buildCommandSummary(command));
            Future<String> stdoutFuture = stdoutExecutor.submit(() -> readStdout(process));

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long elapsedMs = elapsedMs(startNanos);
            if (!finished) {
                process.destroyForcibly();
                String stdoutPreview = tryGetStdout(stdoutFuture);
                log.warn("Batch record image recognition timed out, fileName={}, pid={}, timeoutMs={}, elapsedMs={}, stdoutPreview={}",
                        fileName, pid, timeoutMs, elapsedMs, trimForLog(stdoutPreview));
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_CODEX_TIMEOUT);
            }

            String stdout = getStdout(stdoutFuture, remainingTimeoutMs(startNanos));
            int exitCode = process.exitValue();
            log.info("Batch record image recognition process finished, fileName={}, pid={}, exitCode={}, elapsedMs={}, stdoutChars={}",
                    fileName, pid, exitCode, elapsedMs, stdout.length());
            if (exitCode != 0) {
                log.warn("Codex CLI image parse failed, fileName={}, pid={}, exitCode={}, stdoutPreview={}",
                        fileName, pid, exitCode, trimForLog(stdout));
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_PARSE_FAILED,
                        "codex_exit_" + exitCode);
            }

            CodexImageResponse response = extractStructuredResponse(stdout);
            if (response.getConfidence() < minConfidence) {
                log.warn("Batch record image recognition confidence too low, fileName={}, confidence={}, minConfidence={}, issueCount={}",
                        fileName, response.getConfidence(), minConfidence, sizeOf(response.getIssues()));
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_CONFIDENCE_LOW,
                        response.getConfidence());
            }
            if (response.getTables() == null || response.getTables().isEmpty()) {
                log.warn("Batch record image recognition returned no tables, fileName={}, stdoutPreview={}",
                        fileName, trimForLog(stdout));
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID);
            }

            List<MesProBatchRecordParsedTable> tables = toParsedTables(response);
            log.info("Batch record image recognition parsed structured output, fileName={}, confidence={}, issueCount={}, tableCount={}, elapsedMs={}",
                    fileName, response.getConfidence(), sizeOf(response.getIssues()), tables.size(), elapsedMs(startNanos));
            return tables;
        } catch (IOException ex) {
            log.warn("Batch record image recognition I/O failed, fileName={}, message={}", fileName, ex.getMessage(), ex);
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_PARSE_FAILED,
                    ex.getMessage());
        } catch (TimeoutException ex) {
            log.warn("Batch record image recognition stdout wait timed out, fileName={}, timeoutMs={}, elapsedMs={}",
                    fileName, timeoutMs, elapsedMs(startNanos), ex);
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_CODEX_TIMEOUT);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Batch record image recognition interrupted, fileName={}", fileName, ex);
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_CODEX_TIMEOUT);
        } finally {
            stdoutExecutor.shutdownNow();
            cleanupTempDir(tempDir);
        }
    }

    private List<String> buildCommand(Path imagePath, Path schemaPath) {
        List<String> command = new ArrayList<>();
        command.add(resolveCodexCommand());
        command.add("exec");
        command.add("--json");
        command.add("--ephemeral");
        command.add("--skip-git-repo-check");
        command.add("--dangerously-bypass-approvals-and-sandbox");
        command.add("-i");
        command.add(imagePath.toString());
        command.add("--output-schema");
        command.add(schemaPath.toString());
        if (StrUtil.isNotBlank(codexModel)) {
            command.add("-m");
            command.add(codexModel);
        }
        if (StrUtil.isNotBlank(codexReasoningEffort)) {
            command.add("-c");
            command.add("model_reasoning_effort=\"" + codexReasoningEffort.trim() + "\"");
        }
        if (StrUtil.isNotBlank(codexWorkingDirectory)) {
            command.add("-C");
            command.add(codexWorkingDirectory.trim());
        }
        command.add(PROMPT);
        return command;
    }

    private String resolveCodexCommand() {
        if (StrUtil.isBlank(codexCommand)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_PARSE_FAILED,
                    "codex_cli_command_missing");
        }
        return codexCommand.trim();
    }

    private String normalizeExtension(String originalFileName) {
        String fileName = Objects.toString(originalFileName, "").trim().toLowerCase(Locale.ROOT);
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return extension;
            }
        }
        throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_FILE_EXTENSION_INVALID);
    }

    private String readStdout(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
            return builder.toString();
        }
    }

    private CodexImageResponse extractStructuredResponse(String stdout) {
        String structuredText = null;
        for (String line : stdout.split("\\R")) {
            JsonNode node = JsonUtils.parseObjectQuietly(line, JsonNode.class);
            if (node == null || !"item.completed".equals(node.path("type").asText())) {
                continue;
            }
            JsonNode itemNode = node.path("item");
            if (!"agent_message".equals(itemNode.path("type").asText())) {
                continue;
            }
            structuredText = itemNode.path("text").asText(null);
        }
        if (StrUtil.isBlank(structuredText)) {
            log.warn("Codex CLI image parse missing structured item.completed output, stdoutPreview={}", trimForLog(stdout));
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID);
        }
        CodexImageResponse response = parseStructuredJsonPayload(structuredText);
        if (response == null) {
            log.warn("Codex CLI image parse output is not valid JSON payload, payloadPreview={}",
                    trimForLog(structuredText));
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID);
        }
        return response;
    }

    private CodexImageResponse parseStructuredJsonPayload(String structuredText) {
        CodexImageResponse directResponse = JsonUtils.parseObjectQuietly(structuredText, CodexImageResponse.class);
        if (isValidStructuredResponse(directResponse)) {
            return directResponse;
        }
        for (String candidate : extractBalancedJsonObjectCandidates(structuredText)) {
            CodexImageResponse response = JsonUtils.parseObjectQuietly(candidate, CodexImageResponse.class);
            if (isValidStructuredResponse(response)) {
                return response;
            }
        }
        return null;
    }

    private boolean isValidStructuredResponse(CodexImageResponse response) {
        return response != null && response.getTables() != null && !response.getTables().isEmpty();
    }

    private List<String> extractBalancedJsonObjectCandidates(String text) {
        List<String> candidates = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        int startIndex = -1;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    inString = false;
                }
                continue;
            }
            if (character == '"') {
                inString = true;
                continue;
            }
            if (character == '{') {
                if (depth == 0) {
                    startIndex = index;
                }
                depth++;
                continue;
            }
            if (character == '}' && depth > 0) {
                depth--;
                if (depth == 0 && startIndex >= 0) {
                    candidates.add(text.substring(startIndex, index + 1));
                    startIndex = -1;
                }
            }
        }
        return candidates;
    }

    private List<MesProBatchRecordParsedTable> toParsedTables(CodexImageResponse response) {
        List<MesProBatchRecordParsedTable> tables = new ArrayList<>();
        int tableIndex = 1;
        for (CodexTable table : response.getTables()) {
            List<List<MesProBatchRecordParsedCell>> rows = new ArrayList<>();
            int maxColumnCount = 0;
            for (CodexRow row : table.getRows()) {
                List<MesProBatchRecordParsedCell> cells = new ArrayList<>();
                int columnCount = 0;
                for (CodexCell cell : row.getCells()) {
                    int rowSpan = cell.getRowSpan() == null || cell.getRowSpan() < 1 ? 1 : cell.getRowSpan();
                    int colSpan = cell.getColSpan() == null || cell.getColSpan() < 1 ? 1 : cell.getColSpan();
                    cells.add(MesProBatchRecordParsedCell.builder()
                            .text(StrUtil.blankToDefault(cell.getText(), ""))
                            .rowSpan(rowSpan)
                            .colSpan(colSpan)
                            .build());
                    columnCount += colSpan;
                }
                maxColumnCount = Math.max(maxColumnCount, columnCount);
                rows.add(cells);
            }
            rows = trimTrailingRightEdgeBlankColumns(rows, maxColumnCount);
            maxColumnCount = rows.stream().mapToInt(this::sumColSpan).max().orElse(0);
            tables.add(MesProBatchRecordParsedTable.builder()
                    .sourceTableIndex(tableIndex++)
                    .tableTitle(StrUtil.blankToDefault(table.getTitle(), table.getTemplateName()))
                    .rowCount(rows.size())
                    .columnCount(maxColumnCount)
                    .rows(rows)
                    .build());
        }
        return tables;
    }

    private List<List<MesProBatchRecordParsedCell>> trimTrailingRightEdgeBlankColumns(
            List<List<MesProBatchRecordParsedCell>> rows, int maxColumnCount) {
        int meaningfulColumnCount = resolveMeaningfulColumnCount(rows);
        if (meaningfulColumnCount <= 0 || meaningfulColumnCount >= maxColumnCount) {
            return rows;
        }
        List<List<MesProBatchRecordParsedCell>> trimmedRows = new ArrayList<>();
        for (List<MesProBatchRecordParsedCell> row : rows) {
            trimmedRows.add(trimTrailingBlankCellsToColumnBudget(row, meaningfulColumnCount));
        }
        return trimmedRows;
    }

    private int resolveMeaningfulColumnCount(List<List<MesProBatchRecordParsedCell>> rows) {
        int meaningfulColumnCount = 0;
        for (List<MesProBatchRecordParsedCell> row : rows) {
            if (row.size() == 1) {
                continue;
            }
            int cursor = 0;
            for (MesProBatchRecordParsedCell cell : row) {
                int colSpan = normalizeSpan(cell.getColSpan());
                if (StrUtil.isNotBlank(cell.getText())) {
                    meaningfulColumnCount = Math.max(meaningfulColumnCount, cursor + colSpan);
                }
                cursor += colSpan;
            }
        }
        return meaningfulColumnCount;
    }

    private List<MesProBatchRecordParsedCell> trimTrailingBlankCellsToColumnBudget(
            List<MesProBatchRecordParsedCell> row, int columnBudget) {
        if (row.isEmpty()) {
            return row;
        }
        int columnCount = sumColSpan(row);
        if (columnCount <= columnBudget) {
            return row;
        }
        if (row.size() == 1 && StrUtil.isNotBlank(row.get(0).getText())) {
            MesProBatchRecordParsedCell cell = row.get(0);
            return List.of(MesProBatchRecordParsedCell.builder()
                    .text(StrUtil.blankToDefault(cell.getText(), ""))
                    .rowSpan(normalizeSpan(cell.getRowSpan()))
                    .colSpan(columnBudget)
                    .build());
        }
        List<MesProBatchRecordParsedCell> trimmedRow = new ArrayList<>(row);
        while (columnCount > columnBudget && !trimmedRow.isEmpty()) {
            int lastIndex = trimmedRow.size() - 1;
            MesProBatchRecordParsedCell lastCell = trimmedRow.get(lastIndex);
            if (StrUtil.isNotBlank(lastCell.getText())) {
                return trimmedRow;
            }
            int colSpan = normalizeSpan(lastCell.getColSpan());
            int overflow = columnCount - columnBudget;
            if (colSpan > overflow) {
                trimmedRow.set(lastIndex, MesProBatchRecordParsedCell.builder()
                        .text(StrUtil.blankToDefault(lastCell.getText(), ""))
                        .rowSpan(normalizeSpan(lastCell.getRowSpan()))
                        .colSpan(colSpan - overflow)
                        .build());
                columnCount = columnBudget;
            } else {
                trimmedRow.remove(lastIndex);
                columnCount -= colSpan;
            }
        }
        return trimmedRow;
    }

    private int sumColSpan(List<MesProBatchRecordParsedCell> row) {
        return row.stream().mapToInt(cell -> normalizeSpan(cell.getColSpan())).sum();
    }

    private int normalizeSpan(Integer span) {
        return span == null || span < 1 ? 1 : span;
    }

    private void cleanupTempDir(Path tempDir) {
        if (tempDir == null) {
            return;
        }
        try (var stream = Files.list(tempDir)) {
            stream.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    log.warn("Failed to delete temporary image parse file {}", path, ex);
                }
            });
            Files.deleteIfExists(tempDir);
        } catch (IOException ex) {
            log.warn("Failed to delete temporary image parse directory {}", tempDir, ex);
        }
    }

    private String trimForLog(String stdout) {
        return StrUtil.maxLength(StrUtil.blankToDefault(stdout, ""), 4000);
    }

    private void closeStdin(Process process) throws IOException {
        try (OutputStream outputStream = process.getOutputStream()) {
            outputStream.flush();
        }
    }

    private String buildCommandSummary(List<String> command) {
        if (command.isEmpty()) {
            return "";
        }
        List<String> summary = new ArrayList<>();
        int limit = Math.min(command.size(), 10);
        for (int index = 0; index < limit; index++) {
            summary.add(command.get(index));
        }
        if (command.size() > limit) {
            summary.add("...");
        }
        return String.join(" ", summary);
    }

    private String getStdout(Future<String> stdoutFuture, long remainingTimeoutMs)
            throws InterruptedException, IOException, TimeoutException {
        try {
            return stdoutFuture.get(Math.max(1L, remainingTimeoutMs), TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex) {
            throw new IOException("failed_to_read_codex_stdout", ex.getCause());
        }
    }

    private String tryGetStdout(Future<String> stdoutFuture) {
        try {
            return stdoutFuture.get(1000L, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            return "";
        }
    }

    private long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private long remainingTimeoutMs(long startNanos) {
        return timeoutMs - elapsedMs(startNanos);
    }

    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    @Data
    public static class CodexImageResponse {
        private double confidence;
        private List<String> issues;
        private List<CodexTable> tables;
    }

    @Data
    public static class CodexTable {
        private String title;
        private String templateName;
        private String productName;
        private List<CodexRow> rows;
    }

    @Data
    public static class CodexRow {
        private List<CodexCell> cells;
    }

    @Data
    public static class CodexCell {
        private String text;
        private Integer rowSpan;
        private Integer colSpan;
    }


}
