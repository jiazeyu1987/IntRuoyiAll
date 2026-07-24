package cn.iocoder.yudao.module.dcc.controller.admin.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccAdminFullConfigPackageImportRespVO;
import cn.iocoder.yudao.module.dcc.service.category.DccAdminFullConfigPackageService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixSeedService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryDistributionRuleAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryPermissionAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryTrainingRuleAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryViewMatrixAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileCategoryAdminService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccFileCategoryControllerConfigPackageContractTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccFileCategoryController controller;

    @Mock
    private DccFileCategoryAdminService categoryAdminService;
    @Mock
    private DccCategoryPermissionAdminService permissionAdminService;
    @Mock
    private DccCategoryDistributionRuleAdminService distributionRuleAdminService;
    @Mock
    private DccCategoryTrainingRuleAdminService trainingRuleAdminService;
    @Mock
    private DccCategoryApprovalMatrixAdminService approvalMatrixAdminService;
    @Mock
    private DccCategoryViewMatrixAdminService viewMatrixAdminService;
    @Mock
    private DccCategoryApprovalMatrixSeedService approvalMatrixSeedService;
    @Mock
    private DccAdminFullConfigPackageService adminFullConfigPackageService;

    @Test
    void exportAdminConfigPackage_writesJsonAttachment() throws IOException {
        byte[] expected = "{\"packageVersion\":\"dcc-admin-full-config-package.v1\"}"
                .getBytes(StandardCharsets.UTF_8);
        when(adminFullConfigPackageService.exportPackage()).thenReturn(expected);
        HttpServletResponse response = new MockHttpServletResponse();

        controller.exportAdminConfigPackage(response);

        assertArrayEquals(expected, ((MockHttpServletResponse) response).getContentAsByteArray());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("attachment;filename="));
        assertTrue(response.getHeader("Content-Disposition").contains(".json"));
        verify(adminFullConfigPackageService).exportPackage();
    }

    @Test
    void importAdminConfigPackage_returnsSummary() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "dcc-admin.json",
                "application/json", "{\"packageVersion\":\"dcc-admin-full-config-package.v1\"}"
                .getBytes(StandardCharsets.UTF_8));
        DccAdminFullConfigPackageImportRespVO respVO = new DccAdminFullConfigPackageImportRespVO();
        respVO.setApprovalPositionCount(2);
        respVO.setDirectoryCount(3);
        respVO.setCategoryCount(4);
        when(adminFullConfigPackageService.importPackage(file.getBytes())).thenReturn(respVO);

        DccAdminFullConfigPackageImportRespVO result =
                controller.importAdminConfigPackage(file).getCheckedData();

        assertEquals(2, result.getApprovalPositionCount());
        assertEquals(3, result.getDirectoryCount());
        assertEquals(4, result.getCategoryCount());
        verify(adminFullConfigPackageService).importPackage(file.getBytes());
    }
}
