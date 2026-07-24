package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_CODEX_TIMEOUT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_PARSE_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordCodexCliImageParserTest {

    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WnXl1QAAAAASUVORK5CYII=");

    @Test
    void parse_closesStdinSoCodexProcessCanFinish() throws Exception {
        Path tempDir = Files.createTempDirectory("fake-codex-cli-");
        Path cmdScript = tempDir.resolve("fake-codex.cmd");
        try {
            Files.writeString(cmdScript, """
                    @echo off
                    more >nul
                    echo {"type":"item.completed","item":{"type":"agent_message","text":"{\\"confidence\\":0.95,\\"issues\\":[],\\"tables\\":[{\\"title\\":\\"Test Table\\",\\"templateName\\":\\"Test Table\\",\\"productName\\":\\"\\",\\"rows\\":[{\\"cells\\":[{\\"text\\":\\"A\\",\\"rowSpan\\":1,\\"colSpan\\":1}]}]}]}"}}
                    """, StandardCharsets.UTF_8);

            MesProBatchRecordCodexCliImageParser parser = new MesProBatchRecordCodexCliImageParser();
            setField(parser, "codexCommand", cmdScript.toString());
            setField(parser, "codexModel", "");
            setField(parser, "timeoutMs", 5000L);
            setField(parser, "minConfidence", 0.6D);

            List<MesProBatchRecordParsedTable> tables = parser.parse("sample.png", PNG_BYTES);

            assertEquals(1, tables.size());
            assertEquals("Test Table", tables.get(0).getTableTitle());
            assertEquals(1, tables.get(0).getRowCount());
            assertEquals(1, tables.get(0).getColumnCount());
            assertEquals("A", tables.get(0).getRows().get(0).get(0).getText());
        } finally {
            Files.deleteIfExists(cmdScript);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void extractStructuredResponse_acceptsJsonPayloadWithNarrativePrefix() throws Exception {
        String structuredText = """
                识别结果如下，可作为系统报表表格字段结构：
                {"confidence":0.95,"issues":[],"tables":[{"title":"生产过程损耗报告单","templateName":"生产过程损耗报告单","productName":"","rows":[{"cells":[{"text":"产品名称","rowSpan":1,"colSpan":1},{"text":"","rowSpan":1,"colSpan":1}]}]}]}
                """;
        String stdout = JsonUtils.toJsonString(new java.util.LinkedHashMap<String, Object>() {{
            put("type", "item.completed");
            put("item", new java.util.LinkedHashMap<String, Object>() {{
                put("type", "agent_message");
                put("text", structuredText);
            }});
        }});

        MesProBatchRecordCodexCliImageParser.CodexImageResponse response =
                extractStructuredResponse(new MesProBatchRecordCodexCliImageParser(), stdout);

        assertEquals(0.95D, response.getConfidence());
        assertEquals(1, response.getTables().size());
        assertEquals("生产过程损耗报告单", response.getTables().get(0).getTitle());
        assertEquals("产品名称", response.getTables().get(0).getRows().get(0).getCells().get(0).getText());
        assertEquals("", response.getTables().get(0).getRows().get(0).getCells().get(1).getText());
    }

    @Test
    void extractStructuredResponse_rejectsNonSchemaDomainPayload() throws Exception {
        String structuredText = """
                {"confidence":0.95,"loss_description_fields":["nonconformingDate"],"disposal_options":[{"scrap":false,"other":false,"other_text":""}]}
                """;

        ServiceException exception = assertStructuredResponseRejected(structuredText);

        assertEquals(PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID.getCode(), exception.getCode());
    }

    @Test
    void extractStructuredResponse_rejectsNonSchemaMarkdownPayload() throws Exception {
        String structuredText = """
                **record fields**
                | field |
                |---|
                | nonconformingDate |
                | disposal |
                | checkbox scrap / other |
                """;

        ServiceException exception = assertStructuredResponseRejected(structuredText);

        assertEquals(PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID.getCode(), exception.getCode());
    }

    @Test
    void parse_timesOutWhenProcessExceedsBudget() throws Exception {
        Path tempDir = Files.createTempDirectory("fake-codex-cli-timeout-");
        Path cmdScript = tempDir.resolve("fake-codex-timeout.cmd");
        try {
            Files.writeString(cmdScript, """
                    @echo off
                    more >nul
                    powershell -NoProfile -Command "Start-Sleep -Seconds 3"
                    echo {"type":"item.completed","item":{"type":"agent_message","text":"{\\"confidence\\":0.95,\\"issues\\":[],\\"tables\\":[{\\"title\\":\\"Late Table\\",\\"templateName\\":\\"Late Table\\",\\"productName\\":\\"\\",\\"rows\\":[{\\"cells\\":[{\\"text\\":\\"A\\",\\"rowSpan\\":1,\\"colSpan\\":1}]}]}]}"}}
                    """, StandardCharsets.UTF_8);

            MesProBatchRecordCodexCliImageParser parser = new MesProBatchRecordCodexCliImageParser();
            setField(parser, "codexCommand", cmdScript.toString());
            setField(parser, "codexModel", "");
            setField(parser, "timeoutMs", 500L);
            setField(parser, "minConfidence", 0.6D);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> parser.parse("sample.png", PNG_BYTES));

            assertEquals(PRO_BATCH_RECORD_REPORT_IMAGE_CODEX_TIMEOUT.getCode(), exception.getCode());
        } finally {
            Files.deleteIfExists(cmdScript);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void parse_timesOutWhenInheritedStdoutOutlivesOverallBudget() throws Exception {
        Path tempDir = Files.createTempDirectory("fake-codex-cli-stdout-timeout-");
        Path cmdScript = tempDir.resolve("fake-codex-stdout-timeout.cmd");
        try {
            Files.writeString(cmdScript, """
                    @echo off
                    more >nul
                    start "" /B powershell -NoProfile -Command "Start-Sleep -Milliseconds 1500; Write-Output '{\\"type\\":\\"item.completed\\",\\"item\\":{\\"type\\":\\"agent_message\\",\\"text\\":\\"{\\\\\\"confidence\\\\\\":0.95,\\\\\\"issues\\\\\\":[],\\\\\\"tables\\\\\\":[{\\\\\\"title\\\\\\":\\\\\\"Late Table\\\\\\",\\\\\\"templateName\\\\\\":\\\\\\"Late Table\\\\\\",\\\\\\"productName\\\\\\":\\\\\\"\\\\\\",\\\\\\"rows\\\\\\":[{\\\\\\"cells\\\\\\":[{\\\\\\"text\\\\\\":\\\\\\"A\\\\\\",\\\\\\"rowSpan\\\\\\":1,\\\\\\"colSpan\\\\\\":1}]}]}]}\\"}}'"
                    """, StandardCharsets.UTF_8);

            MesProBatchRecordCodexCliImageParser parser = new MesProBatchRecordCodexCliImageParser();
            setField(parser, "codexCommand", cmdScript.toString());
            setField(parser, "codexModel", "");
            setField(parser, "timeoutMs", 500L);
            setField(parser, "minConfidence", 0.6D);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> parser.parse("sample.png", PNG_BYTES));

            assertEquals(PRO_BATCH_RECORD_REPORT_IMAGE_CODEX_TIMEOUT.getCode(), exception.getCode());
        } finally {
            Files.deleteIfExists(cmdScript);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void buildCommand_whenCodexCommandMissing_failsFastWithToolchainMessage() throws Exception {
        MesProBatchRecordCodexCliImageParser parser = new MesProBatchRecordCodexCliImageParser();
        setField(parser, "codexCommand", "   ");
        setField(parser, "codexModel", "");
        setField(parser, "codexReasoningEffort", "minimal");

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> buildCommand(parser));

        ServiceException cause = (ServiceException) exception.getCause();
        assertEquals(PRO_BATCH_RECORD_REPORT_IMAGE_PARSE_FAILED.getCode(), cause.getCode());
        assertTrue(cause.getMessage().contains("codex_cli_command_missing"));
    }

    @Test
    void parse_whenCodexCommandCannotStart_failsFastWithToolchainMessage() throws Exception {
        MesProBatchRecordCodexCliImageParser parser = new MesProBatchRecordCodexCliImageParser();
        setField(parser, "codexCommand", Path.of("missing-codex-cli.cmd").toString());
        setField(parser, "codexModel", "");
        setField(parser, "timeoutMs", 5000L);
        setField(parser, "minConfidence", 0.6D);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> parser.parse("sample.png", PNG_BYTES));

        assertEquals(PRO_BATCH_RECORD_REPORT_IMAGE_PARSE_FAILED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("codex_cli_process_start_failed"));
    }

    @Test
    void parse_passesEphemeralFlagToCodexCommand() throws Exception {
        Path tempDir = Files.createTempDirectory("fake-codex-cli-ephemeral-");
        Path cmdScript = tempDir.resolve("fake-codex-ephemeral.cmd");
        Path argsFile = tempDir.resolve("args.txt");
        try {
            Files.writeString(cmdScript, """
                    @echo off
                    setlocal
                    set "OUT=%~dp0args.txt"
                    > "%OUT%" echo %*
                    more >nul
                    echo {"type":"item.completed","item":{"type":"agent_message","text":"{\\"confidence\\":0.95,\\"issues\\":[],\\"tables\\":[{\\"title\\":\\"Test Table\\",\\"templateName\\":\\"Test Table\\",\\"productName\\":\\"\\",\\"rows\\":[{\\"cells\\":[{\\"text\\":\\"A\\",\\"rowSpan\\":1,\\"colSpan\\":1}]}]}]}"}}
                    """, StandardCharsets.UTF_8);

            MesProBatchRecordCodexCliImageParser parser = new MesProBatchRecordCodexCliImageParser();
            setField(parser, "codexCommand", cmdScript.toString());
            setField(parser, "codexModel", "");
            setField(parser, "timeoutMs", 5000L);
            setField(parser, "minConfidence", 0.6D);

            parser.parse("sample.png", PNG_BYTES);

            String args = new String(Files.readAllBytes(argsFile), StandardCharsets.ISO_8859_1);
            assertEquals(true, args.contains("--ephemeral"));
            assertEquals(false, args.contains(" -C "));
        } finally {
            Files.deleteIfExists(cmdScript);
            Files.deleteIfExists(argsFile);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void parse_usesStructureFirstChinesePromptContract() throws Exception {
        var promptField = MesProBatchRecordCodexCliImageParser.class.getDeclaredField("PROMPT");
        promptField.setAccessible(true);
        String prompt = (String) promptField.get(null);

        assertEquals(true, prompt.contains("你正在把一张电子批记录图片识别成系统报表表格。"));
        assertEquals(true, prompt.contains("只返回符合 schema 的 JSON"));
        assertEquals(true, prompt.contains("rowSpan 和 colSpan 至少为 1"));
        assertEquals(true, prompt.contains("issues 列出不确定点"));
        assertEquals(true, prompt.contains("不要把表格最右侧外边框"));
    }

    @Test
    void parse_trimsImageRecognizedTrailingPhantomColumnsOnRightEdge() throws Exception {
        MesProBatchRecordCodexCliImageParser.CodexImageResponse response =
                new MesProBatchRecordCodexCliImageParser.CodexImageResponse();
        response.setConfidence(0.95D);
        response.setIssues(List.of());

        MesProBatchRecordCodexCliImageParser.CodexTable table =
                new MesProBatchRecordCodexCliImageParser.CodexTable();
        table.setTitle("精洗工序生产记录");
        table.setTemplateName("精洗工序生产记录");
        table.setProductName("");
        table.setRows(List.of(
                imageRow(imageCell("精洗工序生产记录", 1, 24)),
                imageRow(
                        imageCell("生产前检查记录", 2, 1),
                        imageCell("检查要求", 1, 10),
                        imageCell("结果", 1, 3),
                        imageCell("操作人/日期", 1, 3),
                        imageCell("复核人/日期", 1, 3),
                        imageCell("", 1, 1),
                        imageCell("", 1, 1),
                        imageCell("", 1, 1),
                        imageCell("", 1, 1)),
                imageRow(
                        imageCell("工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具。", 1, 10),
                        imageCell("□符合要求\n□不符合要求", 1, 3),
                        imageCell("", 1, 3),
                        imageCell("", 1, 3),
                        imageCell("", 1, 1),
                        imageCell("", 1, 1),
                        imageCell("", 1, 1),
                        imageCell("", 1, 1)),
                imageRow(
                        imageCell("备注：检查结果符合要求后进行以下生产操作", 1, 20),
                        imageCell("", 1, 1),
                        imageCell("", 1, 1),
                        imageCell("", 1, 1),
                        imageCell("", 1, 1))
        ));
        response.setTables(List.of(table));

        List<MesProBatchRecordParsedTable> tables =
                toParsedTables(new MesProBatchRecordCodexCliImageParser(), response);

        MesProBatchRecordParsedTable parsedTable = tables.get(0);
        assertEquals(20, parsedTable.getColumnCount());
        assertEquals(20, sumColSpan(parsedTable.getRows().get(0)));
        assertEquals(20, sumColSpan(parsedTable.getRows().get(1)));
        assertEquals(5, parsedTable.getRows().get(1).size());
        assertEquals("复核人/日期", parsedTable.getRows().get(1).get(4).getText());
        assertEquals(20, sumColSpan(parsedTable.getRows().get(2)));
        assertEquals(20, sumColSpan(parsedTable.getRows().get(3)));
    }

    @Test
    void parse_trimsSingleImageRecognizedRightEdgeBlankColumnAcrossRows() throws Exception {
        MesProBatchRecordCodexCliImageParser.CodexImageResponse response =
                new MesProBatchRecordCodexCliImageParser.CodexImageResponse();
        response.setConfidence(0.95D);
        response.setIssues(List.of());

        MesProBatchRecordCodexCliImageParser.CodexTable table =
                new MesProBatchRecordCodexCliImageParser.CodexTable();
        table.setTitle("清洗工序生产记录");
        table.setTemplateName("清洗工序生产记录");
        table.setProductName("");
        table.setRows(List.of(
                imageRow(imageCell("清洗工序生产记录", 1, 21)),
                imageRow(
                        imageCell("清洗生产操作及自检记录", 2, 1),
                        imageCell("设备编码", 1, 2),
                        imageCell("超声波清洗机：B09353", 1, 12),
                        imageCell("是否在计量效期内", 1, 2),
                        imageCell("□是\n□否", 1, 3),
                        imageCell("", 1, 1)),
                imageRow(
                        imageCell("操作日期", 1, 1),
                        imageCell("物料编码", 1, 1),
                        imageCell("物料名称", 1, 1),
                        imageCell("批号", 1, 1),
                        imageCell("清洗次数", 1, 2),
                        imageCell("清洗介质", 1, 2),
                        imageCell("清洗功率", 1, 2),
                        imageCell("清洗温度", 1, 2),
                        imageCell("清洗时间", 1, 2),
                        imageCell("生产数量/pcs", 1, 2),
                        imageCell("自检合格数量/pcs", 1, 1),
                        imageCell("不合格数量/pcs", 1, 1),
                        imageCell("操作人", 1, 1),
                        imageCell("复核人", 1, 1),
                        imageCell("", 1, 1)),
                imageRow(
                        imageCell("烘干温度℃", 1, 10),
                        imageCell("烘干时间", 1, 10),
                        imageCell("", 1, 1))
        ));
        response.setTables(List.of(table));

        List<MesProBatchRecordParsedTable> tables =
                toParsedTables(new MesProBatchRecordCodexCliImageParser(), response);

        MesProBatchRecordParsedTable parsedTable = tables.get(0);
        assertEquals(20, parsedTable.getColumnCount());
        assertEquals(20, sumColSpan(parsedTable.getRows().get(0)));
        assertEquals(20, sumColSpan(parsedTable.getRows().get(1)));
        assertEquals(20, sumColSpan(parsedTable.getRows().get(2)));
        assertEquals("复核人", parsedTable.getRows().get(2).get(13).getText());
        assertEquals(20, sumColSpan(parsedTable.getRows().get(3)));
    }

    @Test
    void buildCommand_doesNotAppendWorkingDirectoryFlagWhenConfigBlank() throws Exception {
        MesProBatchRecordCodexCliImageParser parser = new MesProBatchRecordCodexCliImageParser();
        setField(parser, "codexCommand", "codex-fixture.cmd");
        setField(parser, "codexModel", "");
        setField(parser, "codexReasoningEffort", "minimal");
        setField(parser, "codexWorkingDirectory", "   ");

        List<String> command = buildCommand(parser);

        assertEquals(false, command.contains("-C"));
    }

    @Test
    void buildCommand_appendsWorkingDirectoryFlagWhenConfigProvided() throws Exception {
        MesProBatchRecordCodexCliImageParser parser = new MesProBatchRecordCodexCliImageParser();
        setField(parser, "codexCommand", "codex-fixture.cmd");
        setField(parser, "codexModel", "");
        setField(parser, "codexReasoningEffort", "minimal");
        setField(parser, "codexWorkingDirectory", "D:\\ProjectPackage\\Int\\IntRuoyi");

        List<String> command = buildCommand(parser);

        assertEquals(true, command.contains("-C"));
        assertEquals("D:\\ProjectPackage\\Int\\IntRuoyi", command.get(command.indexOf("-C") + 1));
    }

    @Test
    void codexWorkingDirectoryProperty_defaultsToBlankSoCFlagNeedsExplicitConfig() throws Exception {
        var field = MesProBatchRecordCodexCliImageParser.class.getDeclaredField("codexWorkingDirectory");
        Value value = field.getAnnotation(Value.class);

        assertEquals("${yudao.mes.batch-record-report.image.codex-working-directory:}", value.value());
    }

    @Test
    void timeoutDefault_restoresConservativeBudget() throws Exception {
        var field = MesProBatchRecordCodexCliImageParser.class.getDeclaredField("DEFAULT_TIMEOUT_MS");
        field.setAccessible(true);

        assertEquals(600000L, field.getLong(null));
    }


    private static ServiceException assertStructuredResponseRejected(String structuredText) throws Exception {
        String stdout = JsonUtils.toJsonString(new java.util.LinkedHashMap<String, Object>() {{
            put("type", "item.completed");
            put("item", new java.util.LinkedHashMap<String, Object>() {{
                put("type", "agent_message");
                put("text", structuredText);
            }});
        }});
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> extractStructuredResponse(new MesProBatchRecordCodexCliImageParser(), stdout));
        return (ServiceException) exception.getCause();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static MesProBatchRecordCodexCliImageParser.CodexImageResponse extractStructuredResponse(
            MesProBatchRecordCodexCliImageParser parser, String stdout) throws Exception {
        var method = MesProBatchRecordCodexCliImageParser.class.getDeclaredMethod("extractStructuredResponse", String.class);
        method.setAccessible(true);
        return (MesProBatchRecordCodexCliImageParser.CodexImageResponse) method.invoke(parser, stdout);
    }

    @SuppressWarnings("unchecked")
    private static List<MesProBatchRecordParsedTable> toParsedTables(
            MesProBatchRecordCodexCliImageParser parser,
            MesProBatchRecordCodexCliImageParser.CodexImageResponse response) throws Exception {
        var method = MesProBatchRecordCodexCliImageParser.class.getDeclaredMethod(
                "toParsedTables", MesProBatchRecordCodexCliImageParser.CodexImageResponse.class);
        method.setAccessible(true);
        return (List<MesProBatchRecordParsedTable>) method.invoke(parser, response);
    }

    private static MesProBatchRecordCodexCliImageParser.CodexRow imageRow(
            MesProBatchRecordCodexCliImageParser.CodexCell... cells) {
        MesProBatchRecordCodexCliImageParser.CodexRow row = new MesProBatchRecordCodexCliImageParser.CodexRow();
        row.setCells(List.of(cells));
        return row;
    }

    private static MesProBatchRecordCodexCliImageParser.CodexCell imageCell(String text, int rowSpan, int colSpan) {
        MesProBatchRecordCodexCliImageParser.CodexCell cell = new MesProBatchRecordCodexCliImageParser.CodexCell();
        cell.setText(text);
        cell.setRowSpan(rowSpan);
        cell.setColSpan(colSpan);
        return cell;
    }

    private static int sumColSpan(List<MesProBatchRecordParsedCell> row) {
        return row.stream()
                .mapToInt(cell -> cell.getColSpan() < 1 ? 1 : cell.getColSpan())
                .sum();
    }

    @SuppressWarnings("unchecked")
    private static List<String> buildCommand(MesProBatchRecordCodexCliImageParser parser) throws Exception {
        var method = MesProBatchRecordCodexCliImageParser.class.getDeclaredMethod("buildCommand", Path.class, Path.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(parser, Path.of("sample.png"), Path.of("schema.json"));
    }
}
