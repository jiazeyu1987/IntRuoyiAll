package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.idev.excel.FastExcelFactory;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayOutputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserImportControllerTest {
    @Test
    void headerOnlyWorkbookDoesNotInvokeService() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FastExcelFactory.write(output, UserImportExcelVO.class).sheet("用户").doWrite(List.of());
        assertRejected(output.toByteArray(), "导入用户数据不能为空");
    }

    @Test
    void corruptWorkbookDoesNotInvokeService() {
        assertRejected(new byte[] {1, 2, 3, 4}, "文件格式错误");
    }

    @Test
    void emptyFileDoesNotInvokeService() {
        assertRejected(new byte[0], "文件不能为空");
    }

    @Test
    void truncatedZipDoesNotInvokeService() {
        assertRejected(new byte[] {0x50, 0x4b, 3, 4}, "文件格式错误");
    }

    @Test
    void truncatedOleWorkbookDoesNotInvokeService() {
        assertRejected(new byte[] {(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0,
                (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1}, "文件格式错误");
    }

    @Test
    void validXlsAndXlsxReachServiceWithConvertedFields() throws Exception {
        for (cn.idev.excel.support.ExcelTypeEnum type : List.of(
                cn.idev.excel.support.ExcelTypeEnum.XLS, cn.idev.excel.support.ExcelTypeEnum.XLSX)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            FastExcelFactory.write(output).excelType(type).sheet("用户").doWrite(List.of(
                    List.of("登录名称", "用户名称", "部门编号", "用户邮箱", "手机号码", "用户性别", "账号状态"),
                    List.of("operator_01", "操作员", "7", "operator@example.com", "13800138000", "男", "开启")));
            UserController controller = new UserController();
            AdminUserService service = mock(AdminUserService.class);
            ReflectionTestUtils.setField(controller, "userService", service);
            try (org.mockito.MockedStatic<cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils> dict =
                         mockStatic(cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.class)) {
                dict.when(() -> cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.parseDictDataValue("system_user_sex", "男"))
                        .thenReturn("1");
                dict.when(() -> cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils.parseDictDataValue("common_status", "开启"))
                        .thenReturn("0");
                controller.importExcel(new MockMultipartFile("file", "users.xlsx",
                        "application/octet-stream", output.toByteArray()), true);
                verify(service).importUserList(argThat(rows -> rows.size() == 1
                        && rows.get(0).getDeptId().equals(7L)
                        && rows.get(0).getSex().equals(1)
                        && rows.get(0).getStatus().equals(0)
                        && rows.get(0).getUsername().equals("operator_01")), eq(true));
            }
        }
    }

    @Test
    void ordinaryPreviewReadsBothFormatsWithoutWrites() throws Exception {
        for (cn.idev.excel.support.ExcelTypeEnum type : List.of(
                cn.idev.excel.support.ExcelTypeEnum.XLS, cn.idev.excel.support.ExcelTypeEnum.XLSX)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            FastExcelFactory.write(output).excelType(type).sheet("用户").doWrite(List.of(
                    List.of("登录名称", "用户名称", "部门编号"),
                    List.of("operator_01", "操作员", "7"), List.of("operator_02", "", "7")));
            UserController controller = new UserController();
            AdminUserService service = mock(AdminUserService.class);
            ReflectionTestUtils.setField(controller, "userService", service);
            var result = controller.previewImport(new MockMultipartFile("file", output.toByteArray())).getData();
            assertEquals(2, result.getRowCount());
            assertEquals(List.of("部门编号：7"), result.getOrganizations());
            assertEquals(List.of("operator_01 / 操作员", "operator_02 / 第2条记录：姓名缺失"), result.getPersonnel());
            verifyNoInteractions(service);
        }
    }

    @Test
    void dingTalkPreviewUsesRealParserWithoutWrites() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FastExcelFactory.write(output).sheet("用户").doWrite(List.of(
                List.of("员工UserID", "姓名", "部门主管", "1级部门", "2级部门", "3级部门", "4级部门", "5级部门", "6级部门", "7级部门"),
                List.of("employee01", "张三", "", "集团", "生产部", "一车间"),
                List.of("employee02", "李四", "", "集团", "生产部", "一车间")));
        UserController controller = new UserController();
        AdminUserService service = mock(AdminUserService.class);
        ReflectionTestUtils.setField(controller, "userService", service);
        ReflectionTestUtils.setField(controller, "userDingTalkImportExcelParser",
                new cn.iocoder.yudao.module.system.service.user.UserDingTalkImportExcelParser());
        var result = controller.previewDingTalkImport(new MockMultipartFile("file", output.toByteArray())).getData();
        assertEquals(2, result.getRowCount());
        assertEquals(List.of("集团 / 生产部 / 一车间"), result.getOrganizations());
        assertEquals(List.of("employee01 / 张三", "employee02 / 李四"), result.getPersonnel());
        verifyNoInteractions(service);
    }

    @Test
    void previewsRequireImportPermission() throws Exception {
        for (String method : List.of("previewImport", "previewDingTalkImport")) {
            var permission = UserController.class.getMethod(method, org.springframework.web.multipart.MultipartFile.class)
                    .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            assertNotNull(permission);
            assertEquals("@ss.hasPermission('system:user:import')", permission.value());
        }
    }

    private void assertRejected(byte[] bytes, String message) {
        UserController controller = new UserController();
        AdminUserService service = mock(AdminUserService.class);
        ReflectionTestUtils.setField(controller, "userService", service);
        MockMultipartFile file = new MockMultipartFile("file", "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        ServiceException error = assertThrows(ServiceException.class,
                () -> controller.importExcel(file, true));
        assertTrue(error.getMessage().contains(message));
        verifyNoInteractions(service);
    }
}

