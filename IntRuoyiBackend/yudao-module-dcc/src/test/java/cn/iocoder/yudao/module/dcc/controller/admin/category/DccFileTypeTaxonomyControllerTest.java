package cn.iocoder.yudao.module.dcc.controller.admin.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomyRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
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
import static org.mockito.Mockito.when;

class DccFileTypeTaxonomyControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccFileTypeTaxonomyController controller;

    @Mock
    private DccFileTypeTaxonomyAdminService taxonomyAdminService;

    @Test
    void getUploadTaxonomyOptions_returnsOnlyActiveRows() {
        when(taxonomyAdminService.getTaxonomyList()).thenReturn(List.of(
                DccFileTypeTaxonomyDO.builder().id(10L).name("启用分类").active(true).build(),
                DccFileTypeTaxonomyDO.builder().id(11L).name("停用分类").active(false).build()
        ));

        List<DccFileTypeTaxonomyRespVO> result = controller.getUploadTaxonomyOptions().getCheckedData();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("启用分类", result.get(0).getName());
    }

    @Test
    void uploadOptions_usesSubmitPermissionWithoutWeakeningManagementList() throws Exception {
        Method uploadOptions = DccFileTypeTaxonomyController.class.getDeclaredMethod("getUploadTaxonomyOptions");
        assertArrayEquals(new String[]{"/upload-options"}, uploadOptions.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('dcc:controlled-file:submit')",
                uploadOptions.getAnnotation(PreAuthorize.class).value());

        Method managementList = DccFileTypeTaxonomyController.class.getDeclaredMethod("getTaxonomyList");
        assertEquals("@ss.hasPermission('dcc:controlled-file:category:manage')",
                managementList.getAnnotation(PreAuthorize.class).value());
    }
}
