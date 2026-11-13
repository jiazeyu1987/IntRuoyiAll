package cn.iocoder.yudao.module.dcc.service.productcatalog.packageio;

import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.DccCatalogPackageController;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class DccCatalogPackageControllerTest {
    @Test void projectCodeAdministratorCanImportWithoutReceivingExportAccess() throws Exception {
        var security = mock(SecurityFrameworkService.class);
        when(security.hasAnyRoles("super_admin", "tenant_admin", "dcc_project_code_admin")).thenReturn(true);
        assertTrue(authorized("importWorkbook", security, org.springframework.web.multipart.MultipartFile.class));
        assertFalse(authorized("exportWorkbook", security, jakarta.servlet.http.HttpServletResponse.class));
    }
    @Test void ordinaryUserCannotImportWithoutAllRequiredPermissions() throws Exception {
        var security = mock(SecurityFrameworkService.class);
        assertFalse(authorized("importWorkbook", security, org.springframework.web.multipart.MultipartFile.class));
        when(security.hasPermission(anyString())).thenReturn(true);
        assertTrue(authorized("importWorkbook", security, org.springframework.web.multipart.MultipartFile.class));
        when(security.hasPermission("dcc:registration-certificate:config:update")).thenReturn(false);
        assertFalse(authorized("importWorkbook", security, org.springframework.web.multipart.MultipartFile.class));
    }
    private boolean authorized(String method, SecurityFrameworkService security, Class<?> parameter) throws Exception {
        var expression = DccCatalogPackageController.class.getMethod(method, parameter).getAnnotation(PreAuthorize.class).value();
        var context = new StandardEvaluationContext();
        context.setBeanResolver((evaluationContext, name) -> {
            if (!"ss".equals(name)) throw new IllegalArgumentException(name);
            return security;
        });
        return Boolean.TRUE.equals(new SpelExpressionParser().parseExpression(expression).getValue(context, Boolean.class));
    }
    @Test void returnsWorkbookValidationMessageToTheImportDialog() throws Exception {
        var service = mock(DccCatalogPackageService.class);
        when(service.importWorkbook(any())).thenThrow(new IllegalArgumentException("注册证：绑定缺失 owner_company_id=990819001"));
        var controller = new DccCatalogPackageController();
        ReflectionTestUtils.setField(controller, "service", service);
        MockMvcBuilders.standaloneSetup(controller).build()
            .perform(multipart("/dcc/product-catalog/workbook/import")
                .file(new MockMultipartFile("file", "catalog.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1})))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("注册证：绑定缺失 owner_company_id=990819001"));
    }
}
