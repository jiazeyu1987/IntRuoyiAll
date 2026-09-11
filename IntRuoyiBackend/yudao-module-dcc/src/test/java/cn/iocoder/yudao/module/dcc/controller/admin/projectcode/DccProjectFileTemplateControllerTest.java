package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateSaveReqVO;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectFileTemplateService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccProjectFileTemplateControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccProjectFileTemplateController controller;

    @Mock
    private DccProjectFileTemplateService templateService;
    @Mock
    private DccProjectCodeService projectCodeService;

    @Test
    void getProjectTemplate_allowsQueryOrSubmitAndChecksUserProjectScope() throws Exception {
        Method method = DccProjectFileTemplateController.class.getDeclaredMethod("getProjectTemplate", Long.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        PreAuthorize permission = method.getAnnotation(PreAuthorize.class);
        assertNotNull(mapping);
        assertEquals("", mapping.value()[0]);
        assertTrue(permission.value().contains("dcc:project-code:query"));
        assertTrue(permission.value().contains("dcc:controlled-file:submit"));

        DccProjectFileTemplateRespVO response = new DccProjectFileTemplateRespVO();
        response.setProjectCodeId(100L);
        response.setItems(List.of());
        when(templateService.getProjectTemplate(100L)).thenReturn(response);

        CommonResult<DccProjectFileTemplateRespVO> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            result = controller.getProjectTemplate(100L);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(100L, result.getData().getProjectCodeId());
        verify(projectCodeService).getProjectCode(99L, 100L);
        verify(templateService).getProjectTemplate(100L);
    }

    @Test
    void replaceProjectTemplate_requiresProjectUpdateAndDelegates() throws Exception {
        Method method = DccProjectFileTemplateController.class.getDeclaredMethod(
                "replaceProjectTemplate", Long.class, DccProjectFileTemplateSaveReqVO.class);
        PutMapping mapping = method.getAnnotation(PutMapping.class);
        PreAuthorize permission = method.getAnnotation(PreAuthorize.class);
        assertNotNull(mapping);
        assertEquals("", mapping.value()[0]);
        assertTrue(permission.value().contains("dcc:project-code:update"));

        DccProjectFileTemplateSaveReqVO reqVO = new DccProjectFileTemplateSaveReqVO();
        reqVO.setItems(List.of());
        DccProjectFileTemplateRespVO response = new DccProjectFileTemplateRespVO();
        response.setProjectCodeId(100L);
        response.setItems(List.of());
        when(templateService.replaceProjectTemplate(100L, reqVO)).thenReturn(response);

        CommonResult<DccProjectFileTemplateRespVO> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            result = controller.replaceProjectTemplate(100L, reqVO);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        verify(projectCodeService).getProjectCode(99L, 100L);
        verify(templateService).replaceProjectTemplate(100L, reqVO);
    }
}
