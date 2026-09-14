package cn.iocoder.yudao.framework.excel.core.util;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcelUtilsTest {

    @Test
    void read_rejectsEmptyWorkbookAfterParsing() {
        ByteArrayInputStream inputStream = workbook(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ExcelUtils.read(inputStream, ImportRow.class));

        assertEquals("Excel import data rows are required", exception.getMessage());
    }

    @Test
    void read_rejectsRowsViolatingBeanValidation() {
        ByteArrayInputStream inputStream = workbook(List.of(new ImportRow("", 0)));

        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class,
                () -> ExcelUtils.read(inputStream, ImportRow.class));

        assertEquals("Excel import row 2 validation failed", exception.getMessage());
        assertEquals(2, exception.getConstraintViolations().size());
    }

    @Test
    void read_returnsRowsOnlyAfterValidationPasses() {
        ByteArrayInputStream inputStream = workbook(List.of(new ImportRow("M-001", 1)));

        List<ImportRow> rows = ExcelUtils.read(inputStream, ImportRow.class);

        assertEquals(1, rows.size());
        assertEquals("M-001", rows.get(0).getCode());
        assertEquals(1, rows.get(0).getQuantity());
    }

    private ByteArrayInputStream workbook(List<ImportRow> rows) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            FastExcelFactory.write(outputStream, ImportRow.class)
                    .autoCloseStream(false)
                    .sheet("data")
                    .doWrite(rows);
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportRow {

        @ExcelProperty("物料编码")
        @NotBlank
        private String code;

        @ExcelProperty("数量")
        @Min(1)
        private Integer quantity;
    }
}
