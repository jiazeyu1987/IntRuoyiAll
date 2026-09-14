package cn.iocoder.yudao.module.mdm.controller.admin.enterprise;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterprisePageReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseSaveReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseSimpleRespVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseStatusEnum;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmEnterpriseControllerTest {

    @Mock
    private MdmEnterpriseService enterpriseService;

    private MdmEnterpriseController controller;

    @BeforeEach
    void setUp() {
        controller = new MdmEnterpriseController();
        ReflectionTestUtils.setField(controller, "enterpriseService", enterpriseService);
    }

    @Test
    void classMapsAssociatedCompanyBaseRoute() {
        RequestMapping mapping = MdmEnterpriseController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertEquals("/mdm/enterprise", mapping.value()[0]);
    }

    @Test
    void getEnterprisePageMapsQueryPermissionAndDelegates() throws Exception {
        Method method = MdmEnterpriseController.class.getDeclaredMethod("getEnterprisePage",
                MdmEnterprisePageReqVO.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        MdmEnterprisePageReqVO reqVO = new MdmEnterprisePageReqVO();
        MdmEnterpriseDO row = enterprise(301L);
        when(enterpriseService.getEnterprisePage(reqVO)).thenReturn(new PageResult<>(List.of(row), 1L));

        CommonResult<PageResult<MdmEnterpriseRespVO>> result = controller.getEnterprisePage(reqVO);

        assertEquals("/page", mapping.value()[0]);
        assertTrue(preAuthorize.value().contains("mdm:enterprise:query"));
        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(1L, result.getData().getTotal());
        assertEquals("COMP-001", result.getData().getList().get(0).getEnterpriseCode());
        verify(enterpriseService).getEnterprisePage(reqVO);
    }

    @Test
    void createEnterpriseMapsCreatePermissionAndDelegates() throws Exception {
        Method method = MdmEnterpriseController.class.getDeclaredMethod("createEnterprise",
                MdmEnterpriseSaveReqVO.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        MdmEnterpriseSaveReqVO reqVO = saveRequest(null);
        when(enterpriseService.createEnterprise(reqVO)).thenReturn(301L);

        CommonResult<Long> result = controller.createEnterprise(reqVO);

        assertEquals("/create", mapping.value()[0]);
        assertTrue(preAuthorize.value().contains("mdm:enterprise:create"));
        assertEquals(301L, result.getData());
        verify(enterpriseService).createEnterprise(reqVO);
    }

    @Test
    void updateEnterpriseMapsUpdatePermissionAndDelegates() throws Exception {
        Method method = MdmEnterpriseController.class.getDeclaredMethod("updateEnterprise",
                MdmEnterpriseSaveReqVO.class);
        PutMapping mapping = method.getAnnotation(PutMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        MdmEnterpriseSaveReqVO reqVO = saveRequest(301L);

        CommonResult<Boolean> result = controller.updateEnterprise(reqVO);

        assertEquals("/update", mapping.value()[0]);
        assertTrue(preAuthorize.value().contains("mdm:enterprise:update"));
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(enterpriseService).updateEnterprise(reqVO);
    }

    @Test
    void deleteEnterpriseMapsDeletePermissionAndDelegates() throws Exception {
        Method method = MdmEnterpriseController.class.getDeclaredMethod("deleteEnterprise", Long.class);
        DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        CommonResult<Boolean> result = controller.deleteEnterprise(301L);

        assertEquals("/delete", mapping.value()[0]);
        assertTrue(preAuthorize.value().contains("mdm:enterprise:delete"));
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(enterpriseService).deleteEnterprise(301L);
    }

    @Test
    void updateEnterpriseStatusMapsUpdatePermissionAndDelegates() throws Exception {
        Method method = MdmEnterpriseController.class.getDeclaredMethod("updateEnterpriseStatus",
                Long.class, String.class);
        PutMapping mapping = method.getAnnotation(PutMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        CommonResult<Boolean> result = controller.updateEnterpriseStatus(301L,
                MdmEnterpriseStatusEnum.DISABLE.getStatus());

        assertEquals("/update-status", mapping.value()[0]);
        assertTrue(preAuthorize.value().contains("mdm:enterprise:update"));
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(enterpriseService).updateEnterpriseStatus(301L, MdmEnterpriseStatusEnum.DISABLE.getStatus());
    }

    @Test
    void getSimpleEnterpriseListReturnsCompaniesForSelectors() throws Exception {
        Method method = MdmEnterpriseController.class.getDeclaredMethod("getSimpleEnterpriseList",
                String.class, String.class, String.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        when(enterpriseService.listSimpleEnterprises(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), "七木")).thenReturn(List.of(enterprise(301L)));

        CommonResult<List<MdmEnterpriseSimpleRespVO>> result = controller.getSimpleEnterpriseList(
                MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), MdmEnterpriseStatusEnum.ENABLE.getStatus(), "七木");

        assertEquals("/simple-list", mapping.value()[0]);
        assertTrue(preAuthorize.value().contains("mdm:enterprise:query"));
        assertEquals(1, result.getData().size());
        assertEquals("COMP-001", result.getData().get(0).getEnterpriseCode());
        verify(enterpriseService).listSimpleEnterprises(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), "七木");
    }

    private MdmEnterpriseDO enterprise(Long id) {
        MdmEnterpriseDO enterprise = MdmEnterpriseDO.builder()
                .id(id)
                .enterpriseCode("COMP-001")
                .name("上海七木医疗器械有限公司")
                .type(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())
                .status(MdmEnterpriseStatusEnum.ENABLE.getStatus())
                .revision(2)
                .build();
        enterprise.setCreateTime(LocalDateTime.of(2026, 8, 29, 10, 0));
        enterprise.setUpdateTime(LocalDateTime.of(2026, 8, 29, 11, 0));
        return enterprise;
    }

    private MdmEnterpriseSaveReqVO saveRequest(Long id) {
        MdmEnterpriseSaveReqVO reqVO = new MdmEnterpriseSaveReqVO();
        reqVO.setId(id);
        reqVO.setEnterpriseCode("COMP-001");
        reqVO.setName("上海七木医疗器械有限公司");
        reqVO.setType(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());
        reqVO.setStatus(MdmEnterpriseStatusEnum.ENABLE.getStatus());
        return reqVO;
    }
}
