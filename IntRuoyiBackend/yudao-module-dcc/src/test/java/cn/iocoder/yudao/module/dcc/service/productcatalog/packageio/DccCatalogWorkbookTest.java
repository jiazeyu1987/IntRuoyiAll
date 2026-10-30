package cn.iocoder.yudao.module.dcc.service.productcatalog.packageio;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DccCatalogWorkbookTest {
    @Test void reportsOldWorkbookVersionBeforeBusinessColumnMismatch() throws Exception {
        var oldSheets = List.of(
            new DccCatalogWorkbook.SheetData("导入说明", List.of("format"), List.of(Map.of("format", "dcc-catalog-workbook.v3"))),
            new DccCatalogWorkbook.SheetData("DCC产品目录", List.of("id", "origin_tenant_id"), List.of()));
        var schema = List.of(
            new DccCatalogWorkbook.SheetData("导入说明", List.of("format"), List.of(Map.of("format", "dcc-catalog-workbook.v4"))),
            new DccCatalogWorkbook.SheetData("DCC产品目录", List.of("id"), List.of()));
        var error = assertThrows(IllegalArgumentException.class,
            () -> DccCatalogWorkbook.read(DccCatalogWorkbook.write(oldSheets), schema));
        assertEquals("工作簿版本不符，请在当前系统重新导出完整工作簿后再导入", error.getMessage());
    }
    @Test void preservesLongIdsLeadingZeroesNullAndFormulaLikeText() throws Exception {
        Map<String,String> row = new LinkedHashMap<>();
        row.put("id", "9223372036854775806"); row.put("code", "0000123"); row.put("text", "=SUM(A1:A2)"); row.put("null", null); row.put("empty", "");
        var sheets = List.of(new DccCatalogWorkbook.SheetData("DCC产品目录", new ArrayList<>(row.keySet()), List.of(row)));
        assertEquals(sheets, DccCatalogWorkbook.read(DccCatalogWorkbook.write(sheets), sheets));
    }
    @Test void rejectsFormulaAndMissingSheet() throws Exception {
        var sheets = List.of(new DccCatalogWorkbook.SheetData("注册证", List.of("id"), List.of(Map.of("id", "12"))));
        byte[] bytes = DccCatalogWorkbook.write(sheets);
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(bytes)); var out = new ByteArrayOutputStream()) {
            book.getSheetAt(0).getRow(1).getCell(0).setCellFormula("1+1"); book.write(out);
            assertThrows(IllegalArgumentException.class, () -> DccCatalogWorkbook.read(out.toByteArray(), sheets));
        }
        assertThrows(IllegalArgumentException.class, () -> DccCatalogWorkbook.read(bytes,
            List.of(new DccCatalogWorkbook.SheetData("不存在", List.of("id"), List.of()))));
    }
}
