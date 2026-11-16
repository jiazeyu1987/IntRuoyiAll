package cn.iocoder.yudao.module.mdm.controller.admin.companyscope;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopeCreateCompanyReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopeUpdateCompanyReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterprisePageReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MdmEnabledCompanyControllerTest {

    @Test
    void companyPageKeepsDisabledCompaniesAndForcesOwnedType() throws Exception {
        MdmEnterpriseService service = mock(MdmEnterpriseService.class);
        MdmCompanyScopeController controller = new MdmCompanyScopeController();
        ReflectionTestUtils.setField(controller, "enterpriseService", service);
        MdmEnterprisePageReqVO query = new MdmEnterprisePageReqVO();
        query.setPageNo(2);
        query.setPageSize(10);
        query.setKeyword("公司");
        query.setType("ENTRUSTED_PARTY");
        query.setStatus("DISABLE");
        when(service.getEnterprisePage(any())).thenReturn(new PageResult<>(List.of(
                MdmEnterpriseDO.builder().id(301L).name("公司").enterpriseCode("COMP-301")
                        .type("OWNED_COMPANY").status("ENABLE").build()), 11L));

        var result = controller.getOwnedCompanyPage(query).getData();

        verify(service).getEnterprisePage(query);
        assertEquals("OWNED_COMPANY", query.getType());
        assertEquals("DISABLE", query.getStatus());
        assertEquals("公司", query.getKeyword());
        assertEquals(2, query.getPageNo());
        assertEquals(11L, result.getTotal());
        assertEquals(301L, result.getList().get(0).getId());
        assertEquals("COMP-301", result.getList().get(0).getEnterpriseCode());
        assertEquals("@ss.hasPermission('mdm:company-scope:query')",
                MdmCompanyScopeController.class.getMethod("getOwnedCompanyPage", MdmEnterprisePageReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void createEnabledCompanyUsesCompanyScopeCreatePermissionAndCreatesOwnedCompany() throws Exception {
        MdmEnterpriseService service = mock(MdmEnterpriseService.class);
        MdmCompanyScopeController controller = new MdmCompanyScopeController();
        ReflectionTestUtils.setField(controller, "enterpriseService", service);
        MdmCompanyScopeCreateCompanyReqVO request = new MdmCompanyScopeCreateCompanyReqVO();
        request.setName("新公司");
        when(service.createEnabledOwnedCompany("新公司")).thenReturn(301L);

        assertEquals(301L, controller.createEnabledCompany(request).getData());
        verify(service).createEnabledOwnedCompany("新公司");
        assertEquals("@ss.hasPermission('mdm:company-scope:create')",
                MdmCompanyScopeController.class.getMethod("createEnabledCompany",
                                MdmCompanyScopeCreateCompanyReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void editAndStatusChangesRequireUpdatePermissionAndUseOwnedCompanyMethods() throws Exception {
        MdmEnterpriseService service = mock(MdmEnterpriseService.class);
        MdmCompanyScopeController controller = new MdmCompanyScopeController();
        ReflectionTestUtils.setField(controller, "enterpriseService", service);
        MdmCompanyScopeUpdateCompanyReqVO request = new MdmCompanyScopeUpdateCompanyReqVO();
        request.setId(301L);
        request.setName("更新公司");

        assertEquals(true, controller.updateCompany(request).getData());
        assertEquals(true, controller.updateCompanyStatus(301L, "DISABLE").getData());
        assertEquals(true, controller.updateCompanyStatus(301L, "ENABLE").getData());
        verify(service).updateOwnedCompanyName(301L, "更新公司");
        verify(service).updateOwnedCompanyStatus(301L, "DISABLE");
        verify(service).updateOwnedCompanyStatus(301L, "ENABLE");
        for (var method : List.of(
                MdmCompanyScopeController.class.getMethod("updateCompany", MdmCompanyScopeUpdateCompanyReqVO.class),
                MdmCompanyScopeController.class.getMethod("updateCompanyStatus", Long.class, String.class))) {
            assertEquals("@ss.hasPermission('mdm:company-scope:update')",
                    method.getAnnotation(PreAuthorize.class).value());
        }
    }
}
