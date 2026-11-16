package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class Sheet1MachineryProcessExcelParser {

    public static final String SHEET_NAME = "Sheet1";
    public static final String PLACEHOLDER_CODE = "/";
    public static final BigDecimal DAY_HOURS = new BigDecimal("10.5");

    private static final List<String> EXPECTED_HEADERS = List.of(
            "\u4EA7\u54C1\u540D\u79F0",
            "\u7269\u6599\u7F16\u7801",
            "\u8BBE\u5907\u7F16\u7801",
            "\u5DE5\u5E8F\u540D\u79F0",
            "\u8BBE\u5907\u540D\u79F0",
            "\u8BBE\u5907\u6570\u91CF",
            "10.5\u5C0F\u65F6\u65E5\u4EA7\u80FD",
            "\u4EBA\u5DE5"
    );

    public ParsedSheet parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("Excel \u6587\u4EF6\u4E0D\u80FD\u4E3A\u7A7A");
        }
        DataFormatter formatter = new DataFormatter();
        List<DeviceRow> deviceRows = new ArrayList<>();
        List<ManualRow> manualRows = new ArrayList<>();
        int ignoredPlaceholderRowCount = 0;
        String currentLineName = "";
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw ServiceExceptionUtil.invalidParamException("Excel \u7F3A\u5C11 {} \u5DE5\u4F5C\u8868", SHEET_NAME);
            }
            validateHeader(sheet.getRow(0), formatter);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }
                int sourceRowNo = rowIndex + 1;

                String rowLineName = readString(row.getCell(0), formatter);
                if (StrUtil.isNotBlank(rowLineName)) {
                    currentLineName = rowLineName;
                }
                if (StrUtil.isBlank(currentLineName)) {
                    throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C\u4EA7\u7EBF\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A", sourceRowNo);
                }

                String machineryCode = readString(row.getCell(2), formatter);
                String processName = readString(row.getCell(3), formatter);
                if (StrUtil.isBlank(processName)) {
                    throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C\u5DE5\u5E8F\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A", sourceRowNo);
                }
                if (StrUtil.isBlank(machineryCode)) {
                    throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C\u8BBE\u5907\u7F16\u7801\u4E0D\u80FD\u4E3A\u7A7A", sourceRowNo);
                }

                if (PLACEHOLDER_CODE.equals(machineryCode)) {
                    String rawManualCapacity = readString(row.getCell(7), formatter);
                    if (StrUtil.isBlank(rawManualCapacity) || PLACEHOLDER_CODE.equals(rawManualCapacity)) {
                        ignoredPlaceholderRowCount++;
                        continue;
                    }
                    BigDecimal manualDailyCapacity = parsePositiveDecimal(rawManualCapacity, "\u4EBA\u5DE5", sourceRowNo);
                    manualRows.add(new ManualRow(sourceRowNo, currentLineName, processName, manualDailyCapacity,
                            manualDailyCapacity.divide(DAY_HOURS, 6, RoundingMode.HALF_UP)));
                    continue;
                }

                String deviceName = readString(row.getCell(4), formatter);
                if (StrUtil.isBlank(deviceName) || PLACEHOLDER_CODE.equals(deviceName)) {
                    throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C\u8BBE\u5907\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A", sourceRowNo);
                }
                BigDecimal deviceQuantity = readPositiveDecimal(row.getCell(5), formatter, "\u8BBE\u5907\u6570\u91CF", sourceRowNo);
                BigDecimal tenHalfHourDailyCapacity = readPositiveDecimal(row.getCell(6), formatter,
                        "10.5\u5C0F\u65F6\u65E5\u4EA7\u80FD", sourceRowNo);
                BigDecimal standardHourlyCapacity = tenHalfHourDailyCapacity
                        .divide(DAY_HOURS, 6, RoundingMode.HALF_UP)
                        .divide(deviceQuantity, 6, RoundingMode.HALF_UP);
                deviceRows.add(new DeviceRow(sourceRowNo, currentLineName, machineryCode, processName, deviceName,
                        deviceQuantity, tenHalfHourDailyCapacity, standardHourlyCapacity));
            }
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ServiceExceptionUtil.invalidParamException("\u89E3\u6790 Excel \u5931\u8D25: {}", exception.getMessage());
        }
        return new ParsedSheet(deviceRows, manualRows, ignoredPlaceholderRowCount);
    }

    private void validateHeader(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            throw ServiceExceptionUtil.invalidParamException("Excel \u7F3A\u5C11\u8868\u5934");
        }
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            String actual = readString(headerRow.getCell(i), formatter);
            String expected = EXPECTED_HEADERS.get(i);
            if (!Objects.equals(expected, actual)) {
                throw ServiceExceptionUtil.invalidParamException(
                        "Excel \u8868\u5934\u4E0D\u7B26\u5408\u9884\u671F: \u7B2C {} \u5217\u5E94\u4E3A [{}], \u5B9E\u9645\u4E3A [{}]",
                        i + 1, expected, actual);
            }
        }
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            if (StrUtil.isNotBlank(readString(row.getCell(i), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String readString(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return StrUtil.trim(formatter.formatCellValue(cell));
    }

    private BigDecimal readPositiveDecimal(Cell cell, DataFormatter formatter, String fieldName, int sourceRowNo) {
        String raw = readString(cell, formatter);
        if (StrUtil.isBlank(raw) || PLACEHOLDER_CODE.equals(raw)) {
            throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C{} \u4E0D\u80FD\u4E3A\u7A7A", sourceRowNo, fieldName);
        }
        return parsePositiveDecimal(raw, fieldName, sourceRowNo);
    }

    private BigDecimal parsePositiveDecimal(String raw, String fieldName, int sourceRowNo) {
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C{} \u5FC5\u987B\u5927\u4E8E 0", sourceRowNo, fieldName);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw ServiceExceptionUtil.invalidParamException("Excel \u7B2C {} \u884C{} \u4E0D\u662F\u6709\u6548\u6570\u503C", sourceRowNo, fieldName);
        }
    }

    public record ParsedSheet(List<DeviceRow> deviceRows,
                              List<ManualRow> manualRows,
                              Integer ignoredPlaceholderRowCount) {
    }

    public record DeviceRow(Integer sourceRowNo,
                            String lineName,
                            String machineryCode,
                            String processName,
                            String deviceName,
                            BigDecimal deviceQuantity,
                            BigDecimal tenHalfHourDailyCapacity,
                            BigDecimal standardHourlyCapacity) {
    }

    public record ManualRow(Integer sourceRowNo,
                            String lineName,
                            String processName,
                            BigDecimal manualDailyCapacity,
                            BigDecimal singleStandardHourlyCapacity) {
    }
}
