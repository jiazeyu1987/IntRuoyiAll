package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDingTalkImportExcelVO;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

@Component
public class UserDingTalkImportExcelParser {

    private static final List<String> REQUIRED_HEADERS = List.of(
            "员工UserID", "姓名", "邮箱", "工号", "部门主管", "1级部门", "主部门ID",
            "2级部门", "3级部门", "4级部门", "5级部门", "6级部门", "7级部门"
    );

    public List<UserDingTalkImportExcelVO> parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() <= 0 || workbook.getSheetAt(0).getPhysicalNumberOfRows() <= 0) {
                throw exception(USER_DING_TALK_IMPORT_HEADERS_MISSING);
            }
            var sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw exception(USER_DING_TALK_IMPORT_HEADERS_MISSING);
            }
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> headerIndexMap = new LinkedHashMap<>();
            for (int index = headerRow.getFirstCellNum(); index < headerRow.getLastCellNum(); index++) {
                headerIndexMap.put(normalize(formatter.formatCellValue(headerRow.getCell(index))), index);
            }
            if (!headerIndexMap.keySet().containsAll(REQUIRED_HEADERS)) {
                throw exception(USER_DING_TALK_IMPORT_HEADERS_MISSING);
            }

            List<UserDingTalkImportExcelVO> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                UserDingTalkImportExcelVO item = UserDingTalkImportExcelVO.builder()
                        .employeeUserId(getCellValue(row, headerIndexMap, "员工UserID", formatter))
                        .name(getCellValue(row, headerIndexMap, "姓名", formatter))
                        .email(getCellValue(row, headerIndexMap, "邮箱", formatter))
                        .employeeNo(getCellValue(row, headerIndexMap, "工号", formatter))
                        .departmentManagerName(getCellValue(row, headerIndexMap, "部门主管", formatter))
                        .companyName(getCellValue(row, headerIndexMap, "1级部门", formatter))
                        .sourceDepartmentId(getCellValue(row, headerIndexMap, "主部门ID", formatter))
                        .level2DepartmentName(getCellValue(row, headerIndexMap, "2级部门", formatter))
                        .level3DepartmentName(getCellValue(row, headerIndexMap, "3级部门", formatter))
                        .level4DepartmentName(getCellValue(row, headerIndexMap, "4级部门", formatter))
                        .level5DepartmentName(getCellValue(row, headerIndexMap, "5级部门", formatter))
                        .level6DepartmentName(getCellValue(row, headerIndexMap, "6级部门", formatter))
                        .level7DepartmentName(getCellValue(row, headerIndexMap, "7级部门", formatter))
                        .build();
                if (isBlankRow(item)) {
                    continue;
                }
                validateRow(item);
                rows.add(item);
            }
            if (rows.isEmpty()) {
                throw exception(USER_DING_TALK_IMPORT_LIST_IS_EMPTY);
            }
            return rows;
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(USER_DING_TALK_IMPORT_HEADERS_MISSING);
        }
    }

    private void validateRow(UserDingTalkImportExcelVO row) {
        if (isBlank(row.getName())) {
            throw exception(USER_DING_TALK_IMPORT_NAME_REQUIRED);
        }
        if (isBlank(row.getCompanyName())) {
            throw exception(USER_DING_TALK_IMPORT_COMPANY_REQUIRED);
        }
        List<String> levels = Arrays.asList(
                row.getLevel2DepartmentName(), row.getLevel3DepartmentName(), row.getLevel4DepartmentName(),
                row.getLevel5DepartmentName(), row.getLevel6DepartmentName(), row.getLevel7DepartmentName()
        );
        boolean blankSeen = false;
        boolean hasDepartment = false;
        for (String level : levels) {
            if (isBlank(level)) {
                blankSeen = true;
                continue;
            }
            hasDepartment = true;
            if (blankSeen) {
                throw exception(USER_DING_TALK_IMPORT_LEVEL_GAP);
            }
        }
        if (hasDepartment && isBlank(row.getSourceDepartmentId())) {
            throw exception(USER_DING_TALK_IMPORT_SOURCE_DEPT_ID_REQUIRED);
        }
    }

    private boolean isBlankRow(UserDingTalkImportExcelVO row) {
        return isBlank(row.getEmployeeUserId()) && isBlank(row.getName()) && isBlank(row.getEmail())
                && isBlank(row.getEmployeeNo()) && isBlank(row.getDepartmentManagerName())
                && isBlank(row.getCompanyName()) && isBlank(row.getSourceDepartmentId())
                && isBlank(row.getLevel2DepartmentName()) && isBlank(row.getLevel3DepartmentName())
                && isBlank(row.getLevel4DepartmentName()) && isBlank(row.getLevel5DepartmentName())
                && isBlank(row.getLevel6DepartmentName()) && isBlank(row.getLevel7DepartmentName());
    }

    private String getCellValue(Row row, Map<String, Integer> headerIndexMap, String header, DataFormatter formatter) {
        Integer index = headerIndexMap.get(header);
        if (index == null) {
            return "";
        }
        return normalize(formatter.formatCellValue(row.getCell(index)));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
