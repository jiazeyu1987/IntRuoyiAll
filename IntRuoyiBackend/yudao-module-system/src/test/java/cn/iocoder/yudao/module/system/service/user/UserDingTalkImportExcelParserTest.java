package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDingTalkImportExcelVO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class UserDingTalkImportExcelParserTest {
    private static final List<String> HEADERS = List.of("员工UserID", "姓名", "1级部门", "2级部门",
            "3级部门", "4级部门", "5级部门", "6级部门", "7级部门", "部门主管");
    private final UserDingTalkImportExcelParser parser = new UserDingTalkImportExcelParser();

    @Test
    void parsesEmployeeTableWithoutInventingMissingFields() throws Exception {
        var result = parser.parse(new ByteArrayInputStream(workbook(HEADERS,
                List.of("001234567890123456", "测试员工", "测试公司", "质量部", "检验组", "", "", "", "", "测试主管"))));
        assertEquals(1, result.size());
        var employee = result.get(0);
        assertEquals("001234567890123456", employee.getEmployeeUserId());
        assertEquals("检验组", employee.getLevel3DepartmentName());
        assertEquals("测试主管", employee.getDepartmentManagerName());
        assertEquals("", employee.getEmail());
        assertEquals("", employee.getEmployeeNo());
        assertEquals("", employee.getSourceDepartmentId());
    }

    @Test
    void rejectsDepartmentGapInEmployeeTable() throws Exception {
        byte[] bytes = workbook(HEADERS, List.of("001", "测试员工", "测试公司", "", "检验组", "", "", "", "", ""));
        assertServiceException(() -> parser.parse(new ByteArrayInputStream(bytes)), USER_DING_TALK_IMPORT_LEVEL_GAP);
    }

    @Test
    void requiresSourceIdWhenTheColumnIsPresent() throws Exception {
        var headers = new java.util.ArrayList<>(HEADERS);
        headers.add("主部门ID");
        byte[] bytes = workbook(headers, List.of("001", "测试员工", "测试公司", "质量部", "", "", "", "", "", "", ""));
        assertServiceException(() -> parser.parse(new ByteArrayInputStream(bytes)), USER_DING_TALK_IMPORT_SOURCE_DEPT_ID_REQUIRED);
    }

    @Test
    @EnabledIfSystemProperty(named = "dingtalk.import.source", matches = ".+")
    void parsesRequestedWorkbookWithoutWritingBusinessData() throws Exception {
        try (var input = Files.newInputStream(Path.of(System.getProperty("dingtalk.import.source")))) {
            List<UserDingTalkImportExcelVO> result = parser.parse(input);
            assertEquals(1077, result.size());
            assertTrue(result.stream().allMatch(row -> !row.getName().isBlank() && !row.getCompanyName().isBlank()));
            assertTrue(result.stream().allMatch(row -> row.getSourceDepartmentId().isEmpty() && row.getEmployeeNo().isEmpty() && row.getEmail().isEmpty()));
            System.out.println("Employee workbook parsed successfully: " + result.size() + " rows; no database writes.");
        }
    }

    private byte[] workbook(List<String> headers, List<String> values) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("员工信息");
            var header = sheet.createRow(0);
            var row = sheet.createRow(1);
            for (int i = 0; i < headers.size(); i++) header.createCell(i).setCellValue(headers.get(i));
            for (int i = 0; i < values.size(); i++) row.createCell(i).setCellValue(values.get(i));
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
