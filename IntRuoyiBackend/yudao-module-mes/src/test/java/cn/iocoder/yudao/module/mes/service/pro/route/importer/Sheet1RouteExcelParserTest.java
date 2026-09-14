package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_ROUTE_NO_STEP;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_SHEET1_HEADERS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_SHEET1_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_SHEET1_PRODUCT_DUPLICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Sheet1RouteExcelParserTest {

    private final Sheet1RouteExcelParser parser = new Sheet1RouteExcelParser();

    @Test
    void parseWithoutSheet1_failsFast() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.createSheet("Other");
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        ServiceException exception = assertThrows(ServiceException.class,
                () -> parser.parse(new ByteArrayInputStream(workbookBytes)));
        assertEquals(PRO_ROUTE_IMPORT_SHEET1_MISSING.getCode(), exception.getCode());
    }

    @Test
    void parseWithInvalidHeaders_failsFast() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("错误列A");
            header.createCell(1).setCellValue("错误列B");
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        ServiceException exception = assertThrows(ServiceException.class,
                () -> parser.parse(new ByteArrayInputStream(workbookBytes)));
        assertEquals(PRO_ROUTE_IMPORT_SHEET1_HEADERS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void parseWithDuplicateProductName_failsFast() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            var header = sheet.createRow(0);
            List.of("产品名称", "物料编码", "设备编码", "工序名称", "设备名称", "设备数量", "10.5小时日产能", "人工")
                    .forEach(value -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(value));

            var row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("重复产品");
            row1.createCell(1).setCellValue("A01");
            row1.createCell(3).setCellValue("工序1");

            var row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("重复产品");
            row2.createCell(1).setCellValue("A02");
            row2.createCell(3).setCellValue("工序2");
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        ServiceException exception = assertThrows(ServiceException.class,
                () -> parser.parse(new ByteArrayInputStream(workbookBytes)));
        assertEquals(PRO_ROUTE_IMPORT_SHEET1_PRODUCT_DUPLICATE.getCode(), exception.getCode());
    }

    @Test
    void parseWithProductBlockWithoutAnyProcess_failsFast() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            var header = sheet.createRow(0);
            List.of("产品名称", "物料编码", "设备编码", "工序名称", "设备名称", "设备数量", "10.5小时日产能", "人工")
                    .forEach(value -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(value));

            var row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("空工序产品");
            row1.createCell(1).setCellValue("A01");
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        ServiceException exception = assertThrows(ServiceException.class,
                () -> parser.parse(new ByteArrayInputStream(workbookBytes)));
        assertEquals(PRO_ROUTE_IMPORT_ROUTE_NO_STEP.getCode(), exception.getCode());
    }

}
