package cn.iocoder.yudao.module.mes.service.qa.regulation;

import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesQaInspectionRegulationWordParserTest {

    private final MesQaInspectionRegulationWordParser parser =
            new MesQaInspectionRegulationWordParser();

    @Test
    void parse_reconstructsHeaderRevisionAndMergedInspectionRows() throws Exception {
        MesQaInspectionRegulationWordParser.ParsedRegulation parsed = parser.parse(
                buildQaTemplate(true), "测试QA模板.docx");

        assertEquals("PQC-TEST-001", parsed.regulationCode());
        assertEquals("测试产品组装过程检验规程", parsed.regulationName());
        assertEquals("B/1", parsed.versionNo());
        assertEquals(LocalDate.of(2026, 8, 17), parsed.effectiveDate());
        assertEquals(2, parsed.items().size());

        MesQaInspectionRegulationWordParser.ParsedItem first = parsed.items().get(0);
        assertEquals("清洗", first.processName());
        assertEquals("外观", first.itemName());
        assertEquals("表面清洁，无异物", first.standardText());
        assertEquals("目视检查", first.inspectionMethod());
        assertEquals("目测", first.inspectionTool());
        assertEquals(5, first.firstInspectionQuantity());
        assertEquals(new BigDecimal("0.4"), first.patrolInspectionRatio());

        MesQaInspectionRegulationWordParser.ParsedItem second = parsed.items().get(1);
        assertEquals("清洗", second.processName());
        assertEquals("旋转接头 / 牢固度", second.itemName());
        assertNull(second.firstInspectionQuantity());
        assertEquals(new BigDecimal("0.65"), second.patrolInspectionRatio());
        assertTrue(parsed.items().stream().noneMatch(item -> item.itemName().contains("备注")));
    }

    @Test
    void parse_splitsCompoundProcessNamesAndDuplicatesTheSameInspectionItem() throws Exception {
        MesQaInspectionRegulationWordParser.ParsedRegulation parsed = parser.parse(
                buildQaTemplateWithCompoundProcesses(), "复合工序QA模板.docx");

        assertEquals(4, parsed.items().size());
        assertEquals(List.of("清洗", "精洗", "组装Ⅱ", "硅化Ⅰ"),
                parsed.items().stream()
                        .map(MesQaInspectionRegulationWordParser.ParsedItem::processName)
                        .toList());
        assertEquals(List.of("外观", "外观", "尺寸", "尺寸"),
                parsed.items().stream()
                        .map(MesQaInspectionRegulationWordParser.ParsedItem::itemName)
                        .toList());
        assertTrue(parsed.items().stream()
                .filter(item -> item.itemName().equals("外观"))
                .allMatch(item -> item.standardText().equals("表面清洁，无异物")));
        assertTrue(parsed.items().stream()
                .filter(item -> item.itemName().equals("尺寸"))
                .allMatch(item -> item.standardText().equals("符合图纸要求")));
    }

    @Test
    void parse_splitsAdjacentRomanNumberedProcessNamesAndDuplicatesTheSameInspectionItem()
            throws Exception {
        MesQaInspectionRegulationWordParser.ParsedRegulation parsed = parser.parse(
                buildQaTemplateWithAdjacentRomanNumberedProcess(), "连写复合工序QA模板.docx");

        assertEquals(2, parsed.items().size());
        assertEquals(List.of("组装II", "硅化I"),
                parsed.items().stream()
                        .map(MesQaInspectionRegulationWordParser.ParsedItem::processName)
                        .toList());
        assertEquals(List.of("外观", "外观"),
                parsed.items().stream()
                        .map(MesQaInspectionRegulationWordParser.ParsedItem::itemName)
                        .toList());
        assertTrue(parsed.items().stream()
                .allMatch(item -> item.standardText().equals("表面完整，无毛刺、异物")));
    }

    @Test
    void parse_disambiguatesRepeatedMergedParentItemsFromExplicitStandardPrefixes()
            throws Exception {
        MesQaInspectionRegulationWordParser.ParsedRegulation parsed = parser.parse(
                buildQaTemplateWithRepeatedMergedParentItems(), "层级检验项目QA模板.docx");

        assertEquals(List.of(
                        "气密性 / 负压检测",
                        "气密性 / 高压检测",
                        "气密性 / 低压检测",
                        "外观"),
                parsed.items().stream()
                        .map(MesQaInspectionRegulationWordParser.ParsedItem::itemName)
                        .toList());
        assertEquals(List.of(5, 5, 5, 13),
                parsed.items().stream()
                        .map(MesQaInspectionRegulationWordParser.ParsedItem::firstInspectionQuantity)
                        .toList());
        assertTrue(parsed.items().stream()
                .allMatch(item -> item.processName().equals("整体粘结")));
        assertEquals("外观检测：表面应无黑点、杂质和划痕。",
                parsed.items().get(3).standardText());
    }

    @Test
    void parse_rejectsMissingEffectiveDateForHeaderVersion() throws Exception {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> parser.parse(buildQaTemplate(false), "测试QA模板.docx"));

        assertTrue(exception.getMessage().contains("生效日期"));
    }

    @Test
    void parse_rejectsMissingHeaderCode() throws Exception {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> parser.parse(buildQaTemplate(true, false, true, true), "测试QA模板.docx"));

        assertTrue(exception.getMessage().contains("首页规程编号"));
    }

    @Test
    void parse_rejectsMissingInspectionContentTable() throws Exception {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> parser.parse(buildQaTemplate(true, true, false, false), "测试QA模板.docx"));

        assertTrue(exception.getMessage().contains("检验内容表格"));
    }

    @Test
    void parse_rejectsInspectionTableWithoutValidItems() throws Exception {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> parser.parse(buildQaTemplate(true, true, true, false), "测试QA模板.docx"));

        assertTrue(exception.getMessage().contains("没有有效检验项目"));
    }

    private static byte[] buildQaTemplate(boolean includeMatchingRevision) throws Exception {
        return buildQaTemplate(includeMatchingRevision, true, true, true);
    }

    private static byte[] buildQaTemplate(boolean includeMatchingRevision, boolean includeHeaderCode,
                                          boolean includeInspectionTable, boolean includeInspectionItems)
            throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFTable headerTable = header.createTable(3, 3);
            setRow(headerTable.getRow(0), "过程检验规程", "文件编号",
                    includeHeaderCode ? "PQC-TEST-001" : "");
            setRow(headerTable.getRow(1), "测试产品组装过程检验规程", "版本", "B/1");
            setRow(headerTable.getRow(2), "", "页码", "1 of 3");

            XWPFTable revision = document.createTable(3, 5);
            setRow(revision.getRow(0), "版本", "修订内容", "修订编号", "修订日期", "生效日期");
            setRow(revision.getRow(1), "B/0", "首次发布", "REV-001", "2026.01.01", "2026.01.04");
            setRow(revision.getRow(2), includeMatchingRevision ? "B/1" : "B/2",
                    "升版", "REV-002", "2026.08.12", "2026.08.17");

            document.createParagraph().createRun().setText("5.1 检验内容");
            if (!includeInspectionTable) {
                document.write(output);
                return output.toByteArray();
            }
            XWPFTable inspection = document.createTable(includeInspectionItems ? 4 : 2, 8);
            XWPFTableRow heading = inspection.getRow(0);
            heading.getCell(0).setText("序号");
            heading.getCell(1).setText("检验项目");
            setGridSpan(heading.getCell(1), 3);
            heading.removeCell(3);
            heading.removeCell(2);
            heading.getCell(2).setText("接受标准");
            heading.getCell(3).setText("检验方法");
            heading.getCell(4).setText("检验器具及设备");
            heading.getCell(5).setText("抽样方案");

            if (includeInspectionItems) {
                XWPFTableRow first = inspection.getRow(1);
                first.getCell(0).setText("1");
                first.getCell(1).setText("清洗");
                setVerticalMerge(first.getCell(1), STMerge.RESTART);
                first.getCell(2).setText("外观");
                setGridSpan(first.getCell(2), 2);
                first.removeCell(3);
                first.getCell(3).setText("表面清洁，无异物");
                first.getCell(4).setText("目视检查");
                first.getCell(5).setText("目测");
                first.getCell(6).setText("首件：5件；GB/T 2828.1，I，AQL=0.4");

                XWPFTableRow second = inspection.getRow(2);
                second.getCell(0).setText("2");
                setVerticalMerge(second.getCell(1), STMerge.CONTINUE);
                second.getCell(2).setText("旋转接头");
                second.getCell(3).setText("牢固度");
                second.getCell(4).setText("连接牢固，无松动");
                second.getCell(5).setText("手动检查");
                second.getCell(6).setText("手感");
                second.getCell(7).setText("GB/T 2828.1，I，AQL=0.65");
            }

            XWPFTableRow remark = inspection.getRow(includeInspectionItems ? 3 : 1);
            remark.getCell(0).setText("备注");
            setGridSpan(remark.getCell(0), 8);
            for (int cellIndex = 7; cellIndex >= 1; cellIndex--) {
                remark.removeCell(cellIndex);
            }

            document.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] buildQaTemplateWithCompoundProcesses() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFTable headerTable = header.createTable(3, 3);
            setRow(headerTable.getRow(0), "过程检验规程", "文件编号", "PQC-TEST-001");
            setRow(headerTable.getRow(1), "测试产品组装过程检验规程", "版本", "B/1");
            setRow(headerTable.getRow(2), "", "页码", "1 of 3");

            XWPFTable revision = document.createTable(2, 5);
            setRow(revision.getRow(0), "版本", "修订内容", "修订编号", "修订日期", "生效日期");
            setRow(revision.getRow(1), "B/1", "升版", "REV-002", "2026.08.12", "2026.08.17");

            document.createParagraph().createRun().setText("5.1 检验内容");
            XWPFTable inspection = document.createTable(4, 8);
            XWPFTableRow heading = inspection.getRow(0);
            heading.getCell(0).setText("序号");
            heading.getCell(1).setText("检验项目");
            setGridSpan(heading.getCell(1), 3);
            heading.removeCell(3);
            heading.removeCell(2);
            heading.getCell(2).setText("接受标准");
            heading.getCell(3).setText("检验方法");
            heading.getCell(4).setText("检验器具及设备");
            heading.getCell(5).setText("抽样方案");

            XWPFTableRow slashRow = inspection.getRow(1);
            setRow(slashRow, "1", "清洗/精洗", "外观", "", "表面清洁，无异物",
                    "目视检查", "目测", "首检：3件；GB/T 2828.1，I，AQL=0.4");

            XWPFTableRow whitespaceRow = inspection.getRow(2);
            setRow(whitespaceRow, "2", "组装Ⅱ 硅化Ⅰ", "尺寸", "", "符合图纸要求",
                    "卡尺测量", "游标卡尺", "GB/T 2828.1，I，AQL=0.65");

            XWPFTableRow remark = inspection.getRow(3);
            remark.getCell(0).setText("备注");
            setGridSpan(remark.getCell(0), 8);
            for (int cellIndex = 7; cellIndex >= 1; cellIndex--) {
                remark.removeCell(cellIndex);
            }

            document.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] buildQaTemplateWithAdjacentRomanNumberedProcess() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFTable headerTable = header.createTable(3, 3);
            setRow(headerTable.getRow(0), "过程检验规程", "文件编号", "PQC-TEST-001");
            setRow(headerTable.getRow(1), "测试产品组装过程检验规程", "版本", "B/1");
            setRow(headerTable.getRow(2), "", "页码", "1 of 3");

            XWPFTable revision = document.createTable(2, 5);
            setRow(revision.getRow(0), "版本", "修订内容", "修订编号", "修订日期", "生效日期");
            setRow(revision.getRow(1), "B/1", "升版", "REV-002", "2026.08.12", "2026.08.17");

            document.createParagraph().createRun().setText("5.1 检验内容");
            XWPFTable inspection = document.createTable(3, 8);
            XWPFTableRow heading = inspection.getRow(0);
            heading.getCell(0).setText("序号");
            heading.getCell(1).setText("检验项目");
            setGridSpan(heading.getCell(1), 3);
            heading.removeCell(3);
            heading.removeCell(2);
            heading.getCell(2).setText("接受标准");
            heading.getCell(3).setText("检验方法");
            heading.getCell(4).setText("检验器具及设备");
            heading.getCell(5).setText("抽样方案");

            XWPFTableRow adjacentRow = inspection.getRow(1);
            setRow(adjacentRow, "1", "组装II硅化I", "外观", "", "表面完整，无毛刺、异物",
                    "目视检查", "目测", "首检：13件；GB/T 2828.1，I，AQL=1.0");

            XWPFTableRow remark = inspection.getRow(2);
            remark.getCell(0).setText("备注");
            setGridSpan(remark.getCell(0), 8);
            for (int cellIndex = 7; cellIndex >= 1; cellIndex--) {
                remark.removeCell(cellIndex);
            }

            document.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] buildQaTemplateWithRepeatedMergedParentItems() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFTable headerTable = header.createTable(3, 3);
            setRow(headerTable.getRow(0), "过程检验规程", "文件编号", "PQC-TEST-001");
            setRow(headerTable.getRow(1), "测试产品组装过程检验规程", "版本", "B/1");
            setRow(headerTable.getRow(2), "", "页码", "1 of 3");

            XWPFTable revision = document.createTable(2, 5);
            setRow(revision.getRow(0), "版本", "修订内容", "修订编号", "修订日期", "生效日期");
            setRow(revision.getRow(1), "B/1", "升版", "REV-002", "2026.08.12", "2026.08.17");

            XWPFTable inspection = document.createTable(6, 8);
            XWPFTableRow heading = inspection.getRow(0);
            heading.getCell(0).setText("序号");
            heading.getCell(1).setText("检验项目");
            setGridSpan(heading.getCell(1), 3);
            heading.removeCell(3);
            heading.removeCell(2);
            heading.getCell(2).setText("接受标准");
            heading.getCell(3).setText("检验方法");
            heading.getCell(4).setText("检验器具及设备");
            heading.getCell(5).setText("抽样方案");

            setMergedInspectionRow(inspection.getRow(1), "1", "整体粘结", STMerge.RESTART,
                    "气密性", STMerge.RESTART,
                    "负压检测：抽负压-80±5kPa，不应有泄漏。", "连接工装检测",
                    "气密性检测工装", STMerge.RESTART, 5);
            setCellParagraphs(inspection.getRow(1).getCell(1), "整体", "粘结");
            setMergedInspectionRow(inspection.getRow(2), "2", "", STMerge.CONTINUE,
                    "", STMerge.CONTINUE,
                    "高压检测：压力表应匀速上升到指定压力。", "连接工装检测",
                    "", STMerge.CONTINUE, 5);
            setMergedInspectionRow(inspection.getRow(3), "3", "", STMerge.CONTINUE,
                    "", STMerge.CONTINUE,
                    "低压检测：压力表不应直接跳到8atm。", "连接工装检测",
                    "", STMerge.CONTINUE, 5);
            setMergedInspectionRow(inspection.getRow(4), "4", "", STMerge.CONTINUE,
                    "外观", null,
                    "外观检测：表面应无黑点、杂质和划痕。", "目视检查",
                    "目测", null, 13);

            XWPFTableRow remark = inspection.getRow(5);
            remark.getCell(0).setText("备注");
            setGridSpan(remark.getCell(0), 8);
            for (int cellIndex = 7; cellIndex >= 1; cellIndex--) {
                remark.removeCell(cellIndex);
            }

            document.write(output);
            return output.toByteArray();
        }
    }

    private static void setMergedInspectionRow(XWPFTableRow row,
                                               String serial,
                                               String processName,
                                               STMerge.Enum processMerge,
                                               String itemName,
                                               STMerge.Enum itemMerge,
                                               String standard,
                                               String method,
                                               String tool,
                                               STMerge.Enum toolMerge,
                                               int firstInspectionQuantity) {
        row.getCell(0).setText(serial);
        row.getCell(1).setText(processName);
        setVerticalMerge(row.getCell(1), processMerge);
        row.getCell(2).setText(itemName);
        setGridSpan(row.getCell(2), 2);
        if (itemMerge != null) {
            setVerticalMerge(row.getCell(2), itemMerge);
        }
        row.removeCell(3);
        row.getCell(3).setText(standard);
        row.getCell(4).setText(method);
        row.getCell(5).setText(tool);
        if (toolMerge != null) {
            setVerticalMerge(row.getCell(5), toolMerge);
        }
        row.getCell(6).setText("首件：" + firstInspectionQuantity + "件；GB/T 2828.1，S-3，AQL=0.4");
    }

    private static void setRow(XWPFTableRow row, String... values) {
        for (int index = 0; index < values.length; index++) {
            row.getCell(index).setText(values[index]);
        }
    }

    private static void setCellParagraphs(XWPFTableCell cell, String... values) {
        while (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }
        for (String value : values) {
            cell.addParagraph().createRun().setText(value);
        }
    }

    private static void setGridSpan(XWPFTableCell cell, int span) {
        if (cell.getCTTc().getTcPr() == null) {
            cell.getCTTc().addNewTcPr();
        }
        cell.getCTTc().getTcPr().addNewGridSpan().setVal(BigInteger.valueOf(span));
    }

    private static void setVerticalMerge(XWPFTableCell cell, STMerge.Enum value) {
        if (cell.getCTTc().getTcPr() == null) {
            cell.getCTTc().addNewTcPr();
        }
        cell.getCTTc().getTcPr().addNewVMerge().setVal(value);
    }
}
