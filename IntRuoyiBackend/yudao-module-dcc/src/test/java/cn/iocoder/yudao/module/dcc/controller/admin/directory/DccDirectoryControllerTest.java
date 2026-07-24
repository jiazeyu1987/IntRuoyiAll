package cn.iocoder.yudao.module.dcc.controller.admin.directory;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryAccessRuleDirectoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryAccessRuleRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryDeleteSubtreeReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryDeleteSubtreeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessRuleDirectorySummary;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryDeleteSubtreeResult;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAdminService;
import cn.iocoder.yudao.module.dcc.service.directory.DccVisibleDirectoryNode;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DccDirectoryControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccDirectoryController directoryController;

    @Mock
    private DccDirectoryAdminService directoryAdminService;

    @Test
    void getChildren_returnsOnlyDirectVisibleChildrenWithHasChildrenFlag() {
        when(directoryAdminService.listVisibleChildDirectories(null, null)).thenReturn(List.of(
                new DccVisibleDirectoryNode(directory(10L, null, "体系文件"), true, "体系文件"),
                new DccVisibleDirectoryNode(directory(11L, null, "DMR"), false, "DMR")));

        CommonResult<List<DccDirectoryRespVO>> result = directoryController.getDirectoryChildren(null);

        assertEquals(2, result.getData().size());
        assertEquals(10L, result.getData().get(0).getId());
        assertEquals(Boolean.TRUE, result.getData().get(0).getHasChildren());
        assertEquals("体系文件", result.getData().get(0).getDirectoryPath());
        assertEquals(Boolean.FALSE, result.getData().get(1).getHasChildren());
    }

    @Test
    void search_returnsVisibleFlatDirectoriesWithPath() {
        when(directoryAdminService.searchVisibleDirectories(null, "图纸", 50)).thenReturn(List.of(
                new DccVisibleDirectoryNode(directory(21L, 20L, "图纸"), false, "DMR/图纸")));

        CommonResult<List<DccDirectoryRespVO>> result = directoryController.searchDirectories(" 图纸 ", 50);

        assertEquals(1, result.getData().size());
        assertEquals(21L, result.getData().get(0).getId());
        assertEquals("DMR/图纸", result.getData().get(0).getDirectoryPath());
    }

    @Test
    void getAccessRules_returnsStringSubjectTypeForRestoredNasRules() {
        DccDirectoryAccessRuleDO rule = DccDirectoryAccessRuleDO.builder()
                .id(7001L)
                .directoryId(9001L)
                .subjectType("USER")
                .subjectId(1101L)
                .canQuery(Boolean.TRUE)
                .canPreview(Boolean.TRUE)
                .canDownload(Boolean.FALSE)
                .active(Boolean.TRUE)
                .changeReason("restore from NAS ACL snapshot")
                .build();
        when(directoryAdminService.getAccessRules(9001L)).thenReturn(List.of(rule));

        CommonResult<List<DccDirectoryAccessRuleRespVO>> result = directoryController.getAccessRules(9001L);

        assertEquals("USER", result.getData().get(0).getSubjectType());
    }

    @Test
    void getAccessRules_mergesLegacyPreviewOnlyRuleIntoUnifiedReadPermissionResponse() {
        DccDirectoryAccessRuleDO rule = DccDirectoryAccessRuleDO.builder()
                .id(7002L)
                .directoryId(9002L)
                .subjectType("USER")
                .subjectId(1102L)
                .canQuery(Boolean.FALSE)
                .canPreview(Boolean.TRUE)
                .canDownload(Boolean.FALSE)
                .active(Boolean.TRUE)
                .changeReason("legacy preview only")
                .build();
        when(directoryAdminService.getAccessRules(9002L)).thenReturn(List.of(rule));

        CommonResult<List<DccDirectoryAccessRuleRespVO>> result = directoryController.getAccessRules(9002L);

        assertEquals(Boolean.TRUE, result.getData().get(0).getCanQuery());
        assertEquals(Boolean.TRUE, result.getData().get(0).getCanPreview());
        assertEquals(Boolean.FALSE, result.getData().get(0).getCanDownload());
    }

    @Test
    void listAccessRuleDirectories_returnsDirectoryPathSummaries() {
        when(directoryAdminService.listAccessRuleDirectories()).thenReturn(List.of(
                new DccDirectoryAccessRuleDirectorySummary(100L, "经营体系管理制度", "质量管理/1.QMS documents/4 经营体系管理制度"),
                new DccDirectoryAccessRuleDirectorySummary(101L, "生产制造中心", "质量管理/部门目录/生产制造中心")));

        CommonResult<List<DccDirectoryAccessRuleDirectoryRespVO>> result =
                directoryController.listAccessRuleDirectories();

        assertEquals(2, result.getData().size());
        assertEquals(100L, result.getData().get(0).getId());
        assertEquals("经营体系管理制度", result.getData().get(0).getName());
        assertEquals("质量管理/1.QMS documents/4 经营体系管理制度", result.getData().get(0).getDirectoryPath());
    }

    @Test
    void idBasedMappings_requireNumericPathVariables() throws Exception {
        assertEquals("/{id:\\d+}", getMappingPath("updateDirectory", Long.class, DccDirectorySaveReqVO.class));
        assertEquals("/{id:\\d+}", getMappingPath("getDirectory", Long.class));
        assertEquals("/{id:\\d+}/delete-subtree", getMappingPath("deleteSubtree", Long.class, DccDirectoryDeleteSubtreeReqVO.class));
        assertEquals("/{id:\\d+}/active-nas-transfer", getMappingPath("getActiveNasTransfer", Long.class));
        assertEquals("/{id:\\d+}/active-nas-transfer/stop", getMappingPath("stopActiveNasTransfer", Long.class));
        assertEquals("/{id:\\d+}/access-rules", getMappingPath("getAccessRules", Long.class));
        assertEquals("/{id:\\d+}/access-rules", getMappingPath("deleteAccessRules", Long.class));
        assertEquals("/{id:\\d+}/access-rules", getMappingPath("replaceAccessRules", Long.class, List.class));
    }

    @Test
    void accessRuleDirectories_staticRouteIsNotCapturedByIdRoute() throws Exception {
        when(directoryAdminService.listAccessRuleDirectories()).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(directoryController).build();

        mockMvc.perform(get("/dcc/directories/access-rule-directories"))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(DccDirectoryController.class))
                .andExpect(handler().methodName("listAccessRuleDirectories"));
    }

    @Test
    void deleteAccessRules_deletesWholeDirectoryRuleSet() {
        CommonResult<Boolean> result = directoryController.deleteAccessRules(9001L);

        assertEquals(Boolean.TRUE, result.getData());
    }

    @Test
    void deleteSubtree_trimsConfirmTextAndReturnsSummary() {
        DccDirectoryDeleteSubtreeReqVO reqVO = new DccDirectoryDeleteSubtreeReqVO();
        reqVO.setConfirmText(" PROD ");
        when(directoryAdminService.deleteDirectorySubtree(9001L, " PROD "))
                .thenReturn(new DccDirectoryDeleteSubtreeResult(2, 3, 1, 6));

        CommonResult<DccDirectoryDeleteSubtreeRespVO> result = directoryController.deleteSubtree(9001L, reqVO);

        assertEquals(2, result.getData().getDirectoryCount());
        assertEquals(3, result.getData().getControlledFileCount());
        assertEquals(1, result.getData().getMasterCount());
        assertEquals(6, result.getData().getInfraFileCount());
    }

    @Test
    void listAccessRuleDirectories_propagatesMissingDirectoryFailure() {
        when(directoryAdminService.listAccessRuleDirectories()).thenThrow(serviceException(FILE_DIRECTORY_NOT_EXISTS));

        cn.iocoder.yudao.framework.common.exception.ServiceException exception = assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> directoryController.listAccessRuleDirectories());

        assertEquals(FILE_DIRECTORY_NOT_EXISTS.getCode(), exception.getCode());
    }

    private cn.iocoder.yudao.framework.common.exception.ServiceException serviceException(ErrorCode errorCode) {
        return new cn.iocoder.yudao.framework.common.exception.ServiceException(errorCode);
    }

    private cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO directory(Long id,
                                                                                              Long parentId,
                                                                                              String name) {
        return cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO.builder()
                .id(id)
                .parentId(parentId)
                .code("DIR-" + id)
                .name(name)
                .active(Boolean.TRUE)
                .sort(1)
                .build();
    }

    private String getMappingPath(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = DccDirectoryController.class.getDeclaredMethod(methodName, parameterTypes);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            return firstDeclaredPath(getMapping.value(), getMapping.path());
        }
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            return firstDeclaredPath(putMapping.value(), putMapping.path());
        }
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            return firstDeclaredPath(postMapping.value(), postMapping.path());
        }
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            return firstDeclaredPath(deleteMapping.value(), deleteMapping.path());
        }
        throw new IllegalStateException("missing mapping annotation for method " + methodName);
    }

    private String firstDeclaredPath(String[] values, String[] paths) {
        if (values.length > 0) {
            return values[0];
        }
        if (paths.length > 0) {
            return paths[0];
        }
        throw new IllegalStateException("mapping path is empty");
    }

}
