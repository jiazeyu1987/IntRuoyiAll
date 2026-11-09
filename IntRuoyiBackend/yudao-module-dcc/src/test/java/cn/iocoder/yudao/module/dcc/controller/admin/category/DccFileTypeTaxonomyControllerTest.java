package cn.iocoder.yudao.module.dcc.controller.admin.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomyRespVO;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccFileTypeTaxonomyControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccFileTypeTaxonomyController controller;

    @Mock
    private DccFileTypeTaxonomyAdminService taxonomyAdminService;

    @Mock private cn.iocoder.yudao.module.dcc.service.category.DccFileTypeConfigurationService configurationService;

    @Test void createUsesAtomicConfigurationRatherThanStandaloneTaxonomyWrite() {
        var request = new cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomySaveReqVO();
        when(configurationService.create(request)).thenReturn(88L);
        assertEquals(88L, controller.createTaxonomy(request).getCheckedData());
        verify(configurationService).create(request);
        verify(taxonomyAdminService, never()).createTaxonomy(request);
    }

    @Test
    void getUploadTaxonomyOptions_returnsFormalServiceProjectionIncludingBlockers() {
        var available = new DccFileTypeTaxonomyRespVO();
        available.setId(10L);
        available.setName("可上传分类");
        available.setActive(true);
        available.setApprovalSourceMode("APPLICATION_CUSTOM");
        var blocked = new DccFileTypeTaxonomyRespVO();
        blocked.setId(11L);
        blocked.setName("映射待维护分类");
        blocked.setActive(true);
        blocked.setUploadBlocker("未配置启用文件类别，请维护正式类型映射");
        var projection = List.of(available, blocked);
        when(taxonomyAdminService.getUploadTaxonomyOptions()).thenReturn(projection);

        List<DccFileTypeTaxonomyRespVO> result = controller.getUploadTaxonomyOptions().getCheckedData();

        assertSame(projection, result);
        assertEquals("APPLICATION_CUSTOM", result.get(0).getApprovalSourceMode());
        assertEquals("未配置启用文件类别，请维护正式类型映射", result.get(1).getUploadBlocker());
        verify(taxonomyAdminService).getUploadTaxonomyOptions();
        verify(taxonomyAdminService, never()).getTaxonomyList();
    }

    @Test
    void readOptionsAllowIndependentEditWithoutGrantingConfigurationWrites() throws Exception {
        Method uploadOptions = DccFileTypeTaxonomyController.class.getDeclaredMethod("getUploadTaxonomyOptions");
        assertArrayEquals(new String[]{"/upload-options"}, uploadOptions.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasAnyPermissions('dcc:controlled-file:edit','dcc:controlled-file:submit')",
                uploadOptions.getAnnotation(PreAuthorize.class).value());

        Method managementList = DccFileTypeTaxonomyController.class.getDeclaredMethod("getTaxonomyList");
        assertEquals("@ss.hasAnyPermissions('dcc:controlled-file:edit','dcc:controlled-file:category:manage')",
                managementList.getAnnotation(PreAuthorize.class).value());
    }
    @Test void taxonomyWritesStillRequireManagementPermission() throws Exception {
        var save = cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomySaveReqVO.class;
        for (Method method : List.of(
                DccFileTypeTaxonomyController.class.getDeclaredMethod("createTaxonomy", save),
                DccFileTypeTaxonomyController.class.getDeclaredMethod("updateTaxonomy", Long.class, save),
                DccFileTypeTaxonomyController.class.getDeclaredMethod("deleteTaxonomy", Long.class))) {
            assertEquals("@ss.hasPermission('dcc:controlled-file:category:manage')", method.getAnnotation(PreAuthorize.class).value());
        }
    }

    @Test void activeCategoryAllowsFormalCategoryMaintenanceAndStillKeepsManagementListPermission() throws Exception {
        Method method = DccFileTypeTaxonomyController.class.getDeclaredMethod("activeCategory", Long.class);
        assertEquals("@ss.hasAnyPermissions('dcc:controlled-file:edit','dcc:controlled-file:submit','dcc:controlled-file:category:manage')",
                method.getAnnotation(PreAuthorize.class).value());
        when(taxonomyAdminService.resolveActiveCategoryId(31L)).thenReturn(51L);
        assertEquals(51L, controller.activeCategory(31L).getCheckedData());
        assertEquals("@ss.hasAnyPermissions('dcc:controlled-file:edit','dcc:controlled-file:category:manage')",
                DccFileTypeTaxonomyController.class.getDeclaredMethod("getTaxonomyList").getAnnotation(PreAuthorize.class).value());
    }
}
