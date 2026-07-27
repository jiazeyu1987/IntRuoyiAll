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
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Sheet1RouteExcelParserTest {

    private final Sheet1RouteExcelParser parser = new Sheet1RouteExcelParser();

    @Test
    void parseFixture_returnsTwoRoutesWithFirstAppearanceDeduplicatedSteps() throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(
                Sheet1RouteExcelTestFixtures.balloonCatheterProcessWorkbookBytes())) {
            Sheet1RouteExcelParser.ParseResult result = parser.parse(inputStream);

            assertEquals(2, result.routes().size());
            Sheet1RouteExcelParser.Route first = result.routes().get(0);
            assertEquals("球囊扩张导管", first.routeName());
            assertEquals(21, first.materialCodes().size());
            assertEquals(24, first.steps().size());
            assertIterableEquals(List.of(
                    "吹球囊成型", "球囊裁剪", "外管拉伸2", "内管拉伸2", "外管与球囊焊接", "外管切缝", "裁剪管材", "穿显影环",
                    "尖端管与内管焊接", "压显影环", "焊接远端锥度", "裁剪圆角", "焊接圆角", "快速交换口焊接", "RX口检测",
                    "点胶海波管", "球囊涂层", "球囊组件与海波管焊接", "球囊压握", "球囊盘管（机器）", "球囊测漏及全检",
                    "包套装管", "纸塑袋封口（包装）", "全检导丝"
            ), first.steps().stream().map(Sheet1RouteExcelParser.Step::processName).toList());

            Sheet1RouteExcelParser.Route second = result.routes().get(1);
            assertEquals("棘突球囊扩张导管", second.routeName());
            assertEquals(17, second.materialCodes().size());
            assertEquals(26, second.steps().size());
        }
    }

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
