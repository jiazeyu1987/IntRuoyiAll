package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_INVALID_EXCEL;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_ROUTE_NO_STEP;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_SHEET1_HEADERS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_SHEET1_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_SHEET1_PRODUCT_DUPLICATE;

@Component
public class Sheet1RouteExcelParser {

    private static final String SHEET_NAME = "Sheet1";
    private static final List<String> REQUIRED_HEADERS = List.of(
            "产品名称", "物料编码", "设备编码", "工序名称", "设备名称", "设备数量", "10.5小时日产能", "人工");

    public ParseResult parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw exception(PRO_ROUTE_IMPORT_SHEET1_MISSING);
            }
            validateHeaders(sheet);
            return parseRoutes(sheet);
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(PRO_ROUTE_IMPORT_INVALID_EXCEL);
        }
    }

    private void validateHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw exception(PRO_ROUTE_IMPORT_SHEET1_HEADERS_INVALID);
        }
        DataFormatter formatter = new DataFormatter();
        List<String> actualHeaders = new ArrayList<>(REQUIRED_HEADERS.size());
        for (int index = 0; index < REQUIRED_HEADERS.size(); index++) {
            actualHeaders.add(normalize(formatter.formatCellValue(headerRow.getCell(index))));
        }
        if (!REQUIRED_HEADERS.equals(actualHeaders)) {
            throw exception(PRO_ROUTE_IMPORT_SHEET1_HEADERS_INVALID);
        }
    }

    private ParseResult parseRoutes(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        List<Route> routes = new ArrayList<>();
        Set<String> routeNames = new LinkedHashSet<>();
        RouteBuilder currentRoute = null;
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String routeName = getCellValue(row, 0, formatter);
            String materialCodesText = getCellValue(row, 1, formatter);
            String processName = getCellValue(row, 3, formatter);
            if (!routeName.isBlank()) {
                if (!routeNames.add(routeName)) {
                    throw exception(PRO_ROUTE_IMPORT_SHEET1_PRODUCT_DUPLICATE, routeName);
                }
                if (currentRoute != null) {
                    routes.add(currentRoute.build());
                }
                currentRoute = new RouteBuilder(routeName, splitMaterialCodes(materialCodesText));
            }
            if (currentRoute != null && !processName.isBlank()) {
                currentRoute.addProcess(processName);
            }
        }
        if (currentRoute != null) {
            routes.add(currentRoute.build());
        }
        if (routes.isEmpty()) {
            throw exception(PRO_ROUTE_IMPORT_SHEET1_MISSING);
        }
        return new ParseResult(routes);
    }

    private List<String> splitMaterialCodes(String materialCodesText) {
        if (materialCodesText == null || materialCodesText.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : materialCodesText.split("\\R")) {
            String normalized = normalize(part);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        return normalize(formatter.formatCellValue(row.getCell(cellIndex)));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record ParseResult(List<Route> routes) {
    }

    public record Route(String routeName, List<String> materialCodes, List<Step> steps) {
    }

    public record Step(String processName) {
    }

    private static final class RouteBuilder {

        private final String routeName;
        private final List<String> materialCodes;
        private final Map<String, Step> stepByName = new LinkedHashMap<>();

        private RouteBuilder(String routeName, List<String> materialCodes) {
            this.routeName = routeName;
            this.materialCodes = materialCodes;
        }

        private void addProcess(String processName) {
            stepByName.putIfAbsent(processName, new Step(processName));
        }

        private Route build() {
            if (stepByName.isEmpty()) {
                throw exception(PRO_ROUTE_IMPORT_ROUTE_NO_STEP, routeName);
            }
            return new Route(routeName, List.copyOf(materialCodes), List.copyOf(stepByName.values()));
        }
    }

}
