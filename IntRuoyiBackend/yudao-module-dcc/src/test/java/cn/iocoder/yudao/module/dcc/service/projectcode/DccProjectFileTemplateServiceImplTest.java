package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateItemSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFileTemplateItemDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFileTemplateItemMapper;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_DUPLICATE_ITEM;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_CATEGORY_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_SELECTION_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccProjectFileTemplateServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccProjectFileTemplateServiceImpl service;

    @Mock
    private DccProjectFileTemplateItemMapper templateItemMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccFileCategoryMapper categoryMapper;
    @Mock
    private DccFileTypeTaxonomyAdminService taxonomyAdminService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void replaceProjectTemplate_normalizesPersistsAndReturnsResolvedStageType() {
        mockValidTemplateDependencies();
        doAnswer(invocation -> {
            DccProjectFileTemplateItemDO row = invocation.getArgument(0);
            row.setId(900L);
            return 1;
        }).when(templateItemMapper).insert(any(DccProjectFileTemplateItemDO.class));

        DccProjectFileTemplateRespVO result = service.replaceProjectTemplate(100L,
                saveRequest(item(103L, "总装图", 1)));

        ArgumentCaptor<DccProjectFileTemplateItemDO> captor =
                ArgumentCaptor.forClass(DccProjectFileTemplateItemDO.class);
        verify(templateItemMapper).deleteByProjectCodeId(100L);
        verify(templateItemMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getProjectCodeId());
        assertEquals(103L, captor.getValue().getFileTypeTaxonomyId());
        assertEquals("总装图", captor.getValue().getFileName());
        assertEquals("设计阶段", result.getItems().get(0).getStageName());
        assertEquals(102L, result.getItems().get(0).getStageTaxonomyId());
        assertEquals("产品图纸", result.getItems().get(0).getFileTypeName());
        assertEquals(103L, result.getItems().get(0).getFileTypeNodeId());
        assertEquals(List.of(101L, 102L, 103L), result.getTaxonomyOptions().stream()
                .map(option -> option.getId()).toList());
    }

    @Test
    void replaceProjectTemplate_duplicateTaxonomyAndNameFailsBeforeMutation() {
        mockValidTemplateDependencies();
        DccProjectFileTemplateSaveReqVO reqVO = saveRequest(
                item(103L, "总装图", 1),
                item(103L, " 总装图 ", 2));

        assertServiceException(() -> service.replaceProjectTemplate(100L, reqVO),
                PROJECT_FILE_TEMPLATE_DUPLICATE_ITEM);

        verify(templateItemMapper, never()).deleteByProjectCodeId(100L);
        verify(templateItemMapper, never()).insert(any(DccProjectFileTemplateItemDO.class));
    }

    @Test
    void replaceProjectTemplate_withoutUniqueActiveCategoryFailsBeforeMutation() {
        mockValidTemplateDependencies();
        when(categoryMapper.selectList()).thenReturn(List.of());

        assertServiceException(() -> service.replaceProjectTemplate(100L,
                        saveRequest(item(103L, "总装图", 1))),
                PROJECT_FILE_TEMPLATE_CATEGORY_INVALID);

        verify(templateItemMapper, never()).deleteByProjectCodeId(100L);
        verify(templateItemMapper, never()).insert(any(DccProjectFileTemplateItemDO.class));
    }

    @Test
    void validateUploadSelection_requiresConfiguredExactProjectCombination() {
        when(templateItemMapper.selectListByProjectCodeId(100L)).thenReturn(List.of());

        assertServiceException(() -> service.validateUploadSelection(100L, 103L, "总装图"),
                PROJECT_FILE_TEMPLATE_NOT_CONFIGURED);

        when(templateItemMapper.selectListByProjectCodeId(100L)).thenReturn(List.of(
                DccProjectFileTemplateItemDO.builder()
                        .id(900L).projectCodeId(100L).fileTypeTaxonomyId(103L)
                        .fileName("零件图").sortOrder(1).build()));

        assertServiceException(() -> service.validateUploadSelection(100L, 103L, "总装图"),
                PROJECT_FILE_TEMPLATE_SELECTION_INVALID);

        service.validateUploadSelection(100L, 103L, "零件图");
    }

    private static DccProjectCodeDO projectCode() {
        return DccProjectCodeDO.builder().id(100L).projectName("项目A").projectCode("A-001")
                .status("ENABLE").build();
    }

    private void mockValidTemplateDependencies() {
        when(projectCodeMapper.selectByIdForUpdate(100L)).thenReturn(projectCode());
        when(taxonomyAdminService.getTaxonomyList()).thenReturn(taxonomyRows());
        when(taxonomyAdminService.resolveActivePath(103L)).thenReturn(
                new DccFileTypeTaxonomyPath(103L, "技术文档", "设计阶段", "产品图纸", null, null));
        when(categoryMapper.selectList()).thenReturn(List.of(DccFileCategoryDO.builder()
                .id(501L).name("产品图纸").active(true).fileTypeTaxonomyId(103L).build()));
    }

    private static List<DccFileTypeTaxonomyDO> taxonomyRows() {
        return List.of(
                DccFileTypeTaxonomyDO.builder().id(101L).parentId(0L).levelNo(1)
                        .code("TECH").name("技术文档").active(true).sort(1).build(),
                DccFileTypeTaxonomyDO.builder().id(102L).parentId(101L).levelNo(2)
                        .code("DESIGN").name("设计阶段").active(true).sort(1).build(),
                DccFileTypeTaxonomyDO.builder().id(103L).parentId(102L).levelNo(3)
                        .code("DRAWING").name("产品图纸").active(true).sort(1).build());
    }

    private static DccProjectFileTemplateItemSaveReqVO item(Long taxonomyId, String fileName, int sortOrder) {
        DccProjectFileTemplateItemSaveReqVO item = new DccProjectFileTemplateItemSaveReqVO();
        item.setFileTypeTaxonomyId(taxonomyId);
        item.setFileName(fileName);
        item.setSortOrder(sortOrder);
        return item;
    }

    private static DccProjectFileTemplateSaveReqVO saveRequest(DccProjectFileTemplateItemSaveReqVO... items) {
        DccProjectFileTemplateSaveReqVO reqVO = new DccProjectFileTemplateSaveReqVO();
        reqVO.setItems(List.of(items));
        return reqVO;
    }
}
