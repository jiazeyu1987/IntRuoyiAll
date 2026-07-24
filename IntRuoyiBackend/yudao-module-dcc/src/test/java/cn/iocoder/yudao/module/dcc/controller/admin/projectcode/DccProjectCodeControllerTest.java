package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeAssociatedFileAiCategoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeUpdateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class DccProjectCodeControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccProjectCodeController controller;

    @Mock
    private DccProjectCodeService projectCodeService;

    @Test
    void createProjectCode_mapsCreateEndpointRequiresPermissionAndDelegates() throws Exception {
        Method method = DccProjectCodeController.class.getDeclaredMethod("createProjectCode", DccProjectCodeSaveReqVO.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(mapping);
        assertEquals("/create", mapping.value()[0]);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("dcc:project-code:create"));

        DccProjectCodeSaveReqVO reqVO = new DccProjectCodeSaveReqVO();
        reqVO.setProjectName("项目A");
        reqVO.setProjectCode("CODE-A");
        reqVO.setStatus("ENABLE");
        when(projectCodeService.createProjectCode(reqVO)).thenReturn(101L);

        CommonResult<Long> result = controller.createProjectCode(reqVO);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(101L, result.getData());
        verify(projectCodeService).createProjectCode(reqVO);
    }

    @Test
    void updateProjectCode_mapsUpdateEndpointRequiresPermissionAndDelegates() throws Exception {
        Method method = DccProjectCodeController.class.getDeclaredMethod("updateProjectCode", DccProjectCodeUpdateReqVO.class);
        PutMapping mapping = method.getAnnotation(PutMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(mapping);
        assertEquals("/update", mapping.value()[0]);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("dcc:project-code:update"));

        DccProjectCodeUpdateReqVO reqVO = new DccProjectCodeUpdateReqVO();
        reqVO.setId(101L);
        reqVO.setProjectName("项目A");
        reqVO.setProjectCode("CODE-A");
        reqVO.setStatus("DISABLE");

        CommonResult<Boolean> result = controller.updateProjectCode(reqVO);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(projectCodeService).updateProjectCode(reqVO);
    }

    @Test
    void deleteProjectCode_mapsDeleteEndpointRequiresPermissionAndDelegates() throws Exception {
        Method method = DccProjectCodeController.class.getDeclaredMethod("deleteProjectCode", Long.class);
        DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(mapping);
        assertEquals("/delete", mapping.value()[0]);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("dcc:project-code:delete"));

        CommonResult<Boolean> result = controller.deleteProjectCode(101L);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(projectCodeService).deleteProjectCode(101L);
    }

    @Test
    void getProjectCodePage_mapsAssociatedFileCountToResponse() throws Exception {
        Method method = DccProjectCodeController.class.getDeclaredMethod("getProjectCodePage",
                DccProjectCodePageReqVO.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(mapping);
        assertEquals("/page", mapping.value()[0]);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("dcc:project-code:query"));

        DccProjectCodePageReqVO reqVO = new DccProjectCodePageReqVO();
        DccProjectCodeDO row = DccProjectCodeDO.builder()
                .id(101L)
                .projectCode("IRPTCA")
                .projectName("IRPTCA")
                .status("ENABLE")
                .associatedFileCount(7L)
                .build();
        CommonResult<PageResult<DccProjectCodeRespVO>> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(projectCodeService.getProjectCodePage(99L, reqVO)).thenReturn(new PageResult<>(List.of(row), 1L));
            result = controller.getProjectCodePage(reqVO);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(7L, result.getData().getList().get(0).getAssociatedFileCount());
        verify(projectCodeService).getProjectCodePage(99L, reqVO);
    }

    @Test
    void getAssociatedFileAiCategoryCandidates_mapsEndpointRequiresUpdatePermissionAndDelegates() throws Exception {
        Method method = DccProjectCodeController.class.getDeclaredMethod("getAssociatedFileAiCategoryCandidates",
                Long.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(mapping);
        assertEquals("/{id:\\d+}/associated-files/ai-category-candidates", mapping.value()[0]);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("dcc:project-code:update"));
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:update"));

        DccProjectCodeAssociatedFileAiCategoryRespVO candidate =
                DccProjectCodeAssociatedFileAiCategoryRespVO.builder()
                        .fileId(201L)
                        .fileName("项目策划书.pdf")
                        .currentStage(null)
                        .currentFileType(null)
                        .matched(Boolean.FALSE)
                        .build();
        CommonResult<List<DccProjectCodeAssociatedFileAiCategoryRespVO>> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(projectCodeService.getAssociatedFileAiCategoryCandidates(99L, 101L)).thenReturn(List.of(candidate));
            result = controller.getAssociatedFileAiCategoryCandidates(101L);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(201L, result.getData().get(0).getFileId());
        verify(projectCodeService).getAssociatedFileAiCategoryCandidates(99L, 101L);
    }

    @Test
    void classifyAssociatedFileByName_mapsEndpointRequiresUpdatePermissionAndDelegates() throws Exception {
        Method method = DccProjectCodeController.class.getDeclaredMethod("classifyAssociatedFileByName",
                Long.class, Long.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(mapping);
        assertEquals("/{id:\\d+}/associated-files/{fileId:\\d+}/ai-category", mapping.value()[0]);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("dcc:project-code:update"));
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:update"));

        DccProjectCodeAssociatedFileAiCategoryRespVO response =
                DccProjectCodeAssociatedFileAiCategoryRespVO.builder()
                        .fileId(201L)
                        .fileName("项目策划书.pdf")
                        .targetStage("01 plan 策划")
                        .targetFileType("项目策划书")
                        .matched(Boolean.TRUE)
                        .build();
        CommonResult<DccProjectCodeAssociatedFileAiCategoryRespVO> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(projectCodeService.classifyAssociatedFileByName(99L, 101L, 201L)).thenReturn(response);
            result = controller.classifyAssociatedFileByName(101L, 201L);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals("01 plan 策划", result.getData().getTargetStage());
        verify(projectCodeService).classifyAssociatedFileByName(99L, 101L, 201L);
    }
}
