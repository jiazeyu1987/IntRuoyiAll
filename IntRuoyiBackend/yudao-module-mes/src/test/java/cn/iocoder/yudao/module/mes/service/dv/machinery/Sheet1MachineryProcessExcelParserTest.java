package cn.iocoder.yudao.module.mes.service.dv.machinery;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Sheet1MachineryProcessExcelParserTest {

    private final Sheet1MachineryProcessExcelParser parser = new Sheet1MachineryProcessExcelParser();

    @Test
    void parse_keepsManualRowsAndConvertsHourlyCapacity() throws Exception {
        Sheet1MachineryProcessExcelParser.ParsedSheet parsedSheet = parser.parse(buildWorkbook());

        assertEquals(1, parsedSheet.deviceRows().size());
        assertEquals(1, parsedSheet.manualRows().size());
        assertEquals(1, parsedSheet.ignoredPlaceholderRowCount());
        assertEquals("吹球囊成型", parsedSheet.deviceRows().get(0).processName());
        assertEquals(0, parsedSheet.deviceRows().get(0).standardHourlyCapacity()
                .compareTo(new BigDecimal("9.523810")));
        assertEquals("穿显影环", parsedSheet.manualRows().get(0).processName());
        assertEquals(0, parsedSheet.manualRows().get(0).singleStandardHourlyCapacity()
                .compareTo(new BigDecimal("70.476190")));
    }

    private MockMultipartFile buildWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("产品名称");
            header.createCell(1).setCellValue("物料编码");
            header.createCell(2).setCellValue("设备编码");
            header.createCell(3).setCellValue("工序名称");
            header.createCell(4).setCellValue("设备名称");
            header.createCell(5).setCellValue("设备数量");
            header.createCell(6).setCellValue("10.5小时日产能");
            header.createCell(7).setCellValue("人工");

            Row deviceRow = sheet.createRow(1);
            deviceRow.createCell(2).setCellValue("A03190");
            deviceRow.createCell(3).setCellValue("吹球囊成型");
            deviceRow.createCell(4).setCellValue("球囊成型机");
            deviceRow.createCell(5).setCellValue(1);
            deviceRow.createCell(6).setCellValue(100);

            Row placeholderRow = sheet.createRow(2);
            placeholderRow.createCell(2).setCellValue("/");
            placeholderRow.createCell(3).setCellValue("占位设备工序");
            placeholderRow.createCell(4).setCellValue("/");
            placeholderRow.createCell(5).setCellValue("/");
            placeholderRow.createCell(6).setCellValue("/");

            Row manualRow = sheet.createRow(3);
            manualRow.createCell(2).setCellValue("/");
            manualRow.createCell(3).setCellValue("穿显影环");
            manualRow.createCell(4).setCellValue("/");
            manualRow.createCell(5).setCellValue("/");
            manualRow.createCell(6).setCellValue("/");
            manualRow.createCell(7).setCellValue(740);

            workbook.write(outputStream);
            return new MockMultipartFile("file", "balloon.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }
}
