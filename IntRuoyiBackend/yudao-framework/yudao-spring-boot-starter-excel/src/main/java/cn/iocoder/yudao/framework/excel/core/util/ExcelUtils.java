package cn.iocoder.yudao.framework.excel.core.util;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.framework.excel.core.handler.SelectSheetWriteHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Excel 工具类
 *
 * @author 瑛泰源码
 */
public class ExcelUtils {

    private static final Validator EXCEL_ROW_VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String EXPORT_RECORD_COUNT_HEADER = "X-Excel-Export-Record-Count";

    /**
     * 将列表以 Excel 响应给前端
     *
     * @param response  响应
     * @param filename  文件名
     * @param sheetName Excel sheet 名
     * @param head      Excel head 头
     * @param data      数据列表哦
     * @param <T>       泛型，保证 head 和 data 类型的一致性
     * @throws IOException 写入失败的情况
     */
    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data) throws IOException {
        validateWriteArguments(response, filename, sheetName, head, data);
        // 输出 Excel
        FastExcelFactory.write(response.getOutputStream(), head)
                .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy()) // 基于 column 长度，自动适配。最大 255 宽度
                .registerWriteHandler(new SelectSheetWriteHandler(head)) // 基于固定 sheet 实现下拉框
                .registerConverter(new LongStringConverter()) // 避免 Long 类型丢失精度
                .sheet(sheetName).doWrite(data);
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.addHeader(EXPORT_RECORD_COUNT_HEADER, String.valueOf(data.size()));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
    }

    /**
     * 将列表以 Excel 响应给前端，并只输出指定列下标。
     */
    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data, Set<Integer> includeColumnIndexes) throws IOException {
        validateWriteArguments(response, filename, sheetName, head, data);
        if (includeColumnIndexes == null || includeColumnIndexes.isEmpty()) {
            throw new IllegalArgumentException("Excel export include columns are required");
        }
        FastExcelFactory.write(response.getOutputStream(), head)
                .autoCloseStream(false)
                .includeColumnIndexes(includeColumnIndexes)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerWriteHandler(new SelectSheetWriteHandler(head))
                .registerConverter(new LongStringConverter())
                .sheet(sheetName).doWrite(data);
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.addHeader(EXPORT_RECORD_COUNT_HEADER, String.valueOf(data.size()));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
    }

    public static <T> List<T> read(MultipartFile file, Class<T> head) throws IOException {
        // 参考 https://t.zsxq.com/zM77F 帖子，增加 try 处理，兼容 windows 场景
        try (InputStream inputStream = file.getInputStream()) {
            return validateImportedRows(FastExcelFactory.read(inputStream, head, null)
                    .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                    .doReadAllSync(), head);
        }
    }

    public static <T> List<T> read(InputStream inputStream, Class<T> head) {
        return validateImportedRows(FastExcelFactory.read(inputStream, head, null)
                .autoCloseStream(false)
                .doReadAllSync(), head);
    }

    private static <T> void validateWriteArguments(HttpServletResponse response, String filename, String sheetName,
                                                   Class<T> head, List<T> data) {
        if (response == null || head == null || filename == null || filename.isBlank()
                || sheetName == null || sheetName.isBlank() || data == null) {
            throw new IllegalArgumentException("Excel export arguments are required");
        }
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) == null) {
                throw new IllegalArgumentException("Excel export row " + (i + 1) + " is required");
            }
        }
    }

    private static <T> List<T> validateImportedRows(List<T> rows, Class<T> head) {
        if (head == null) {
            throw new IllegalArgumentException("Excel import head class is required");
        }
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Excel import data rows are required");
        }
        for (int i = 0; i < rows.size(); i++) {
            T row = rows.get(i);
            if (row == null) {
                throw new IllegalArgumentException("Excel import row " + (i + 2) + " is required");
            }
            Set<ConstraintViolation<T>> violations = EXCEL_ROW_VALIDATOR.validate(row);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException("Excel import row " + (i + 2) + " validation failed",
                        new HashSet<>(violations));
            }
        }
        return rows;
    }

}
