package cn.iocoder.yudao.module.crm.controller.admin.customer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.crm.controller.admin.customer.vo.customer.*;
import cn.iocoder.yudao.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.iocoder.yudao.module.crm.service.customer.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;

class CrmCustomerExcelRoundTripTest {
    @Test
    void oldExportHeadersAreRejectedBeforeAnyCustomerWrite() throws Exception {
        var controller = new CrmCustomerController();
        var service = mock(CrmCustomerService.class);
        ReflectionTestUtils.setField(controller, "customerService", service);
        var output = new java.io.ByteArrayOutputStream();
        cn.idev.excel.FastExcelFactory.write(output).sheet("数据").doWrite(List.of(
                List.of("客户名称", "wechat", "email", "地区编号", "地区名称"),
                List.of("客户01", "wechat01", "customer@example.com", "110101", "北京市 东城区")));
        var file = new MockMultipartFile("file", output.toByteArray());
        var request = CrmCustomerImportReqVO.builder().file(file).updateSupport(false).build();
        var error = org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> controller.importExcel(request));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("重新导出"));
        verifyNoInteractions(service);
    }

    @Test
    void controllerExportPreservesWechatEmailAndFullAreaPathOnImport() throws Exception {
        var controller = new CrmCustomerController();
        var service = mock(CrmCustomerService.class);
        var users = mock(AdminUserApi.class);
        var departments = mock(DeptApi.class);
        ReflectionTestUtils.setField(controller, "customerService", service);
        ReflectionTestUtils.setField(controller, "adminUserApi", users);
        ReflectionTestUtils.setField(controller, "deptApi", departments);
        ReflectionTestUtils.setField(controller, "customerPoolConfigService", mock(CrmCustomerPoolConfigService.class));
        var customer = new CrmCustomerDO();
        customer.setId(1L);
        customer.setName("客户01");
        customer.setWechat("wechat01");
        customer.setEmail("customer@example.com");
        customer.setAreaId(110101);
        when(service.getCustomerPage(any(), nullable(Long.class))).thenReturn(new PageResult<>(List.of(customer), 1L));
        when(users.getUserMap(anyCollection())).thenReturn(new HashMap<>());
        when(departments.getDeptMap(anyCollection())).thenReturn(new HashMap<>());
        var response = new MockHttpServletResponse();
        controller.exportCustomerExcel(new CrmCustomerPageReqVO(), response);
        var file = new MockMultipartFile("file", "客户.xls", "application/octet-stream", response.getContentAsByteArray());
        for (boolean update : List.of(false, true)) {
            var request = CrmCustomerImportReqVO.builder().file(file).updateSupport(update).build();
            controller.importExcel(request);
            verify(service).importCustomerList(argThat(rows -> rows.size() == 1
                    && "客户01".equals(rows.get(0).getName())
                    && "wechat01".equals(rows.get(0).getWechat())
                    && "customer@example.com".equals(rows.get(0).getEmail())
                    && Integer.valueOf(110101).equals(rows.get(0).getAreaId())), same(request));
        }
    }
}
