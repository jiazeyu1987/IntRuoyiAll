package cn.iocoder.yudao.module.dcc.service.productcatalog.packageio;

import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.DccCatalogPackageController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DccCatalogPackageControllerTest {
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
