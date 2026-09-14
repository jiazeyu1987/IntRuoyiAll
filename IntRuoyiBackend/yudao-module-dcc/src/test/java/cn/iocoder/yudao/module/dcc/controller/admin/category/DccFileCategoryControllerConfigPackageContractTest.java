package cn.iocoder.yudao.module.dcc.controller.admin.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccAdminFullConfigPackageImportRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileCategoryRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.category.DccAdminFullConfigPackageService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixSeedService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryDistributionRuleAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryPermissionAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryTrainingRuleAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccCategoryViewMatrixAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileCategoryAdminService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileCategoryPermissionSupport;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCategoryList_projectsCurrentUserUploadPermission() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(99L);
        SecurityFrameworkUtils.setLoginUser(loginUser, new MockHttpServletRequest());
        when(categoryAdminService.getCategoryDirectoryBindingMap()).thenReturn(Map.of(10L, 100L, 11L, 101L));
        when(categoryAdminService.getCategoryList()).thenReturn(List.of(
                DccFileCategoryDO.builder().id(10L).code("SOP").name("SOP").active(Boolean.TRUE).sort(1).build(),
                DccFileCategoryDO.builder().id(11L).code("WI").name("WI").active(Boolean.TRUE).sort(2).build()
        ));
        when(approvalMatrixAdminService.getActiveMatrixPositionIdsByCategoryIds(List.of(10L, 11L)))
                .thenReturn(Map.of());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.UPLOAD))
                .thenReturn(true);
        when(permissionSupport.hasCategoryPermission(11L, 99L, DccFileCategoryPermissionActionEnum.UPLOAD))
                .thenReturn(false);

        List<DccFileCategoryRespVO> result = controller.getCategoryList().getCheckedData();

        assertEquals(2, result.size());
        assertTrue(result.get(0).getCanUpload());
        assertEquals(false, result.get(1).getCanUpload());
    }

    @Test
    void getCategoryList_projectsActiveApprovalMatrixPositionIds() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(99L);
        SecurityFrameworkUtils.setLoginUser(loginUser, new MockHttpServletRequest());
        when(categoryAdminService.getCategoryDirectoryBindingMap()).thenReturn(Map.of(26L, 126L));
        when(categoryAdminService.getCategoryList()).thenReturn(List.of(
                DccFileCategoryDO.builder()
                        .id(26L)
                        .code("INTAUTH-26")
                        .name("技术调研报告")
                        .active(Boolean.TRUE)
                        .sort(26)
                        .build()
        ));
        when(approvalMatrixAdminService.getActiveMatrixPositionIdsByCategoryIds(List.of(26L)))
                .thenReturn(Map.of(26L, new DccCategoryApprovalMatrixAdminService.MatrixPositionIds(
                        List.of(201L, 202L, 203L), List.of(301L, 302L))));
        when(permissionSupport.hasCategoryPermission(26L, 99L, DccFileCategoryPermissionActionEnum.UPLOAD))
                .thenReturn(true);

        List<DccFileCategoryRespVO> result = controller.getCategoryList().getCheckedData();

        assertEquals(1, result.size());
        assertEquals("INTAUTH-26", result.get(0).getCode());
        assertEquals(List.of(201L, 202L, 203L), result.get(0).getSignoffPositionIds());
        assertEquals(List.of(301L, 302L), result.get(0).getApprovalPositionIds());
        assertTrue(result.get(0).getCanUpload());
    }

    @Test
    void getCategoryList_withoutLoginUserDoesNotGrantUploadProjection() {
        when(categoryAdminService.getCategoryDirectoryBindingMap()).thenReturn(Map.of(10L, 100L));
        when(categoryAdminService.getCategoryList()).thenReturn(List.of(
                DccFileCategoryDO.builder().id(10L).code("SOP").name("SOP").active(Boolean.TRUE).sort(1).build()
        ));
        when(approvalMatrixAdminService.getActiveMatrixPositionIdsByCategoryIds(List.of(10L)))
                .thenReturn(Map.of());

        List<DccFileCategoryRespVO> result = controller.getCategoryList().getCheckedData();

        assertEquals(false, result.get(0).getCanUpload());
        verifyNoInteractions(permissionSupport);
    }

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
