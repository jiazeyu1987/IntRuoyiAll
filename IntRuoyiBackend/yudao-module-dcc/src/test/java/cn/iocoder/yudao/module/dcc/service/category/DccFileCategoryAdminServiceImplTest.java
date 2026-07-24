package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileCategorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDirectoryBindingSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DELETE_CHILD_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DELETE_RELATION_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DELETE_REFERENCED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_LIFECYCLE_STAGE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_FILE_CATEGORY_SYNC_AMBIGUOUS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_FILE_CATEGORY_SYNC_CONFIG_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(DccFileCategoryAdminServiceImpl.class)
class DccFileCategoryAdminServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccFileCategoryAdminServiceImpl categoryAdminService;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccCategoryDirectoryBindingMapper bindingMapper;
    @Resource
    private DccCategoryViewMatrixRuleMapper viewMatrixRuleMapper;
    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Resource
    private DccFileCategoryDistributionRuleMapper distributionRuleMapper;
    @Resource
    private DccFileCategoryTrainingRuleMapper trainingRuleMapper;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccCategoryApprovalRouteMapper routeMapper;
    @Resource
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DataSource dataSource;
    @MockitoBean
    private DccIntAuthFileCategoryClient intAuthFileCategoryClient;
    @MockitoBean
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;

    @Test
    void getCategoryList_readsLocalTableWithoutCallingIntAuth() {
        createCategory("LOCAL-SOP", "SOP", "LOCAL", true, 1);

        List<DccFileCategoryDO> categories = categoryAdminService.getCategoryList();

        assertEquals(1, categories.size());
        assertEquals("SOP", categories.get(0).getName());
        verifyNoInteractions(intAuthFileCategoryClient);
    }

    @Test
    void importCategoriesFromIntAuth_createsMissingLocalCategory() {
        when(intAuthFileCategoryClient.listFileCategories()).thenReturn(List.of(
                new DccIntAuthFileCategoryClient.IntAuthFileCategory(11L, "设计转移方案/报告", true, true)
        ));

        DccFileCategoryImportResult result = categoryAdminService.importCategoriesFromIntAuth();

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getAdoptedCount());
        assertEquals(0, result.getUpdatedCount());
        List<DccFileCategoryDO> categories = categoryMapper.selectList();
        assertEquals(1, categories.size());
        assertEquals("INTAUTH-11", categories.get(0).getCode());
        assertEquals("INTAUTH:11", categories.get(0).getSource());
        assertEquals("设计转移方案/报告", categories.get(0).getName());
        assertEquals("TRANSFER", categories.get(0).getLifecycleStage());
        assertTrue(Boolean.TRUE.equals(categories.get(0).getActive()));
        assertTrue(Boolean.TRUE.equals(categories.get(0).getDistributionRequired()));
        assertTrue(Boolean.TRUE.equals(categories.get(0).getTrainingRequired()));
    }

    @Test
    void createCategory_missingRequirementFlags_defaultsToTrue() {
        DccFileCategorySaveReqVO reqVO = new DccFileCategorySaveReqVO();
        reqVO.setCode("LOCAL-REQ");
        reqVO.setName("Local Requirement");
        reqVO.setActive(Boolean.TRUE);
        reqVO.setSort(1);
        reqVO.setSource("LOCAL");
        reqVO.setRemark("seed");
        reqVO.setDescription("seed");
        reqVO.setLifecycleStage("CLIENT_IGNORED");
        reqVO.setFileTypeTaxonomyId(8801L);
        mockTaxonomyPath(8801L, "设计和开发策划阶段");

        Long categoryId = categoryAdminService.createCategory(reqVO);

        DccFileCategoryDO category = categoryMapper.selectById(categoryId);
        assertTrue(Boolean.TRUE.equals(category.getDistributionRequired()));
        assertTrue(Boolean.TRUE.equals(category.getTrainingRequired()));
        assertEquals("PLAN", category.getLifecycleStage());
    }

    @Test
    void createCategory_withoutClientSource_persistsLocalSource() {
        DccFileCategorySaveReqVO reqVO = new DccFileCategorySaveReqVO();
        reqVO.setCode("LOCAL-NO-SOURCE");
        reqVO.setName("Local No Source");
        reqVO.setActive(Boolean.TRUE);
        reqVO.setSort(1);
        reqVO.setLifecycleStage("CLIENT_IGNORED");
        reqVO.setFileTypeTaxonomyId(8802L);
        mockTaxonomyPath(8802L, "设计和开发输入阶段");

        Long categoryId = categoryAdminService.createCategory(reqVO);

        DccFileCategoryDO category = categoryMapper.selectById(categoryId);
        assertEquals("LOCAL", category.getSource());
        assertEquals("INPUT", category.getLifecycleStage());
    }

    @Test
    void createCategory_withFileTypeTaxonomy_validatesActiveTaxonomyPath() {
        DccFileCategorySaveReqVO reqVO = new DccFileCategorySaveReqVO();
        reqVO.setCode("LOCAL-TAXONOMY");
        reqVO.setName("Local Taxonomy");
        reqVO.setActive(Boolean.TRUE);
        reqVO.setSort(1);
        reqVO.setSource("LOCAL");
        reqVO.setLifecycleStage("CLIENT_IGNORED");
        reqVO.setFileTypeTaxonomyId(8801L);
        mockTaxonomyPath(8801L, "设计和开发策划阶段");

        Long categoryId = categoryAdminService.createCategory(reqVO);

        DccFileCategoryDO category = categoryMapper.selectById(categoryId);
        assertEquals(8801L, category.getFileTypeTaxonomyId());
        assertEquals("PLAN", category.getLifecycleStage());
        verify(fileTypeTaxonomyAdminService).resolveActivePath(8801L);
    }

    @Test
    void createCategory_missingFileTypeTaxonomy_failFast() {
        DccFileCategorySaveReqVO reqVO = new DccFileCategorySaveReqVO();
        reqVO.setCode("LOCAL-NO-STAGE");
        reqVO.setName("Local Missing Stage");
        reqVO.setActive(Boolean.TRUE);
        reqVO.setSort(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> categoryAdminService.createCategory(reqVO));
        assertEquals(FILE_CATEGORY_LIFECYCLE_STAGE_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("fileTypeTaxonomyId"));
    }

    @Test
    void updateCategory_ignoresClientLifecycleStageAndDerivesFromTaxonomy() {
        DccFileCategoryDO category = createCategory("LOCAL-STAGE", "Stage", "LOCAL", true, 1);
        DccFileCategorySaveReqVO reqVO = new DccFileCategorySaveReqVO();
        reqVO.setId(category.getId());
        reqVO.setCode(category.getCode());
        reqVO.setName(category.getName());
        reqVO.setActive(Boolean.TRUE);
        reqVO.setSort(2);
        reqVO.setLifecycleStage("UNKNOWN");
        reqVO.setFileTypeTaxonomyId(8803L);
        mockTaxonomyPath(8803L, "设计和开发输出阶段");

        categoryAdminService.updateCategory(reqVO);

        DccFileCategoryDO updated = categoryMapper.selectById(category.getId());
        assertEquals("OUTPUT", updated.getLifecycleStage());
        assertEquals(8803L, updated.getFileTypeTaxonomyId());
    }

    @Test
    void importCategoriesFromIntAuth_reusesSameNameLocalCategoryAndPreservesBinding() {
        DccFileCategoryDO localCategory = createCategory("DCC_FVM_DHF_004", "项目立项书", "LOCAL", true, 5);
        bindingMapper.insert(DccCategoryDirectoryBindingDO.builder()
                .id(randomLongId())
                .categoryId(localCategory.getId())
                .directoryId(88L)
                .active(Boolean.TRUE)
                .build());

        when(intAuthFileCategoryClient.listFileCategories()).thenReturn(List.of(
                new DccIntAuthFileCategoryClient.IntAuthFileCategory(11L, "项目立项书", false, true)
        ));

        DccFileCategoryImportResult result = categoryAdminService.importCategoriesFromIntAuth();

        assertEquals(1, result.getAdoptedCount());
        DccFileCategoryDO category = categoryMapper.selectById(localCategory.getId());
        assertEquals("DCC_FVM_DHF_004", category.getCode());
        assertEquals("INTAUTH:11", category.getSource());
        assertEquals(1, bindingMapper.selectList(DccCategoryDirectoryBindingDO::getCategoryId, localCategory.getId()).size());
    }

    @Test
    void importCategoriesFromIntAuth_updatesMappedCategory() {
        DccFileCategoryDO importedCategory = createCategory("INTAUTH-11", "Old Name", "INTAUTH:11", false, 3);

        when(intAuthFileCategoryClient.listFileCategories()).thenReturn(List.of(
                new DccIntAuthFileCategoryClient.IntAuthFileCategory(11L, "风险管理报告", true, true)
        ));

        DccFileCategoryImportResult result = categoryAdminService.importCategoriesFromIntAuth();

        assertEquals(1, result.getUpdatedCount());
        DccFileCategoryDO category = categoryMapper.selectById(importedCategory.getId());
        assertEquals("风险管理报告", category.getName());
        assertEquals("OUTPUT", category.getLifecycleStage());
        assertTrue(Boolean.TRUE.equals(category.getActive()));
    }

    @Test
    void importCategoriesFromIntAuth_duplicateSameNameLocalCategory_failFast() {
        createCategory("LOCAL-SOP-1", "SOP", "LOCAL", true, 1);
        createCategory("LOCAL-SOP-2", "SOP", "LOCAL", true, 2);

        when(intAuthFileCategoryClient.listFileCategories()).thenReturn(List.of(
                new DccIntAuthFileCategoryClient.IntAuthFileCategory(11L, "SOP", true, true)
        ));

        assertServiceException(categoryAdminService::importCategoriesFromIntAuth, INTAUTH_FILE_CATEGORY_SYNC_AMBIGUOUS);
    }

    @Test
    void importCategoriesFromIntAuth_missingIntAuthConfig_failFast() {
        when(intAuthFileCategoryClient.listFileCategories())
                .thenThrow(exception(INTAUTH_FILE_CATEGORY_SYNC_CONFIG_MISSING, "yudao.dcc.int-auth.internal-service-token"));

        assertServiceException(categoryAdminService::importCategoriesFromIntAuth, INTAUTH_FILE_CATEGORY_SYNC_CONFIG_MISSING);
    }

    @Test
    void bindDirectory_rebindingWithHistoricalDeletedBinding_doesNotHitDuplicateKey() {
        DccFileCategoryDO category = createCategory("LOCAL-SOP", "SOP", "LOCAL", true, 1);
        DccFileDirectoryDO directory = createDirectory("DIR-001", "Root Directory", null, 1);
        DccCategoryDirectoryBindingSaveReqVO reqVO = new DccCategoryDirectoryBindingSaveReqVO();
        reqVO.setDirectoryId(directory.getId());
        reqVO.setActive(Boolean.TRUE);

        insertHistoricalDeletedBinding(category.getId(), directory.getId());

        categoryAdminService.bindDirectory(category.getId(), reqVO);

        List<DccCategoryDirectoryBindingDO> bindings = bindingMapper.selectList(
                DccCategoryDirectoryBindingDO::getCategoryId, category.getId());
        assertEquals(1, bindings.size());
        assertEquals(directory.getId(), bindings.get(0).getDirectoryId());
        assertTrue(Boolean.TRUE.equals(bindings.get(0).getActive()));
    }

    @Test
    void getCategoryDirectoryBindingMap_returnsOnlyActiveBindings() {
        DccFileCategoryDO activeCategory = createCategory("LOCAL-ACTIVE", "Active", "LOCAL", true, 1);
        DccFileCategoryDO inactiveCategory = createCategory("LOCAL-INACTIVE", "Inactive", "LOCAL", true, 2);
        DccFileDirectoryDO activeDirectory = createDirectory("DIR-ACTIVE", "Active Directory", null, 1);
        DccFileDirectoryDO inactiveDirectory = createDirectory("DIR-INACTIVE", "Inactive Directory", null, 2);
        bindingMapper.insert(DccCategoryDirectoryBindingDO.builder()
                .id(randomLongId())
                .categoryId(activeCategory.getId())
                .directoryId(activeDirectory.getId())
                .active(Boolean.TRUE)
                .build());
        bindingMapper.insert(DccCategoryDirectoryBindingDO.builder()
                .id(randomLongId())
                .categoryId(inactiveCategory.getId())
                .directoryId(inactiveDirectory.getId())
                .active(Boolean.FALSE)
                .build());

        Map<Long, Long> bindingMap = categoryAdminService.getCategoryDirectoryBindingMap();

        assertEquals(activeDirectory.getId(), bindingMap.get(activeCategory.getId()));
        assertFalse(bindingMap.containsKey(inactiveCategory.getId()));
    }

    @Test
    void deleteCategory_whenNoRelations_removesCategory() {
        DccFileCategoryDO category = createCategory("LOCAL-SOP", "SOP", "LOCAL", true, 1);

        categoryAdminService.deleteCategory(category.getId());

        assertEquals(0, categoryMapper.selectList(DccFileCategoryDO::getId, category.getId()).size());
    }

    @Test
    void deleteCategory_whenChildCategoryExists_failFast() {
        DccFileCategoryDO parent = createCategory("LOCAL-PARENT", "Parent", "LOCAL", true, 1);
        createCategory("LOCAL-CHILD", "Child", "LOCAL", true, 2, parent.getId());

        assertServiceException(() -> categoryAdminService.deleteCategory(parent.getId()),
                FILE_CATEGORY_DELETE_CHILD_EXISTS);
    }

    @Test
    void deleteCategory_whenControlledFileMasterExists_failFast() {
        DccFileCategoryDO category = createCategory("LOCAL-SOP", "SOP", "LOCAL", true, 1);
        controlledFileMasterMapper.insert(DccControlledFileMasterDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .fileName("seed.pdf")
                .fileNumber("DOC-001")
                .currentActiveControlledFileId(null)
                .status("ACTIVE")
                .build());

        assertServiceException(() -> categoryAdminService.deleteCategory(category.getId()),
                FILE_CATEGORY_DELETE_REFERENCED);
    }

    @Test
    void deleteCategory_whenGovernanceRelationExists_failFast() {
        DccFileCategoryDO category = createCategory("LOCAL-SOP", "SOP", "LOCAL", true, 1);
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .actionType("VIEW")
                .subjectType("USER")
                .subjectId(99L)
                .active(Boolean.TRUE)
                .build());

        assertServiceException(() -> categoryAdminService.deleteCategory(category.getId()),
                FILE_CATEGORY_DELETE_RELATION_EXISTS);
        assertEquals(1, categoryMapper.selectList(DccFileCategoryDO::getId, category.getId()).size());
        assertEquals(1, permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId,
                category.getId()).size());
    }

    private void mockTaxonomyPath(Long id, String stageName) {
        when(fileTypeTaxonomyAdminService.resolveActivePath(id)).thenReturn(new DccFileTypeTaxonomyPath(
                id, "技术文档", stageName, "测试文件类型", null, null));
    }

    private DccFileCategoryDO createCategory(String code, String name, String source, boolean active, int sort) {
        return createCategory(code, name, source, active, sort, null);
    }

    private DccFileCategoryDO createCategory(String code, String name, String source, boolean active, int sort,
                                             Long parentId) {
        DccFileCategoryDO category = DccFileCategoryDO.builder()
                .id(randomLongId())
                .code(code)
                .name(name)
                .parentId(parentId)
                .active(active)
                .sort(sort)
                .source(source)
                .remark("seed")
                .description(code + "-DESC")
                .lifecycleStage("PLAN")
                .distributionRequired(Boolean.FALSE)
                .trainingRequired(Boolean.FALSE)
                .build();
        categoryMapper.insert(category);
        return category;
    }

    private DccFileDirectoryDO createDirectory(String code, String name, Long parentId, int sort) {
        DccFileDirectoryDO directory = DccFileDirectoryDO.builder()
                .id(randomLongId())
                .code(code)
                .name(name)
                .parentId(parentId)
                .active(Boolean.TRUE)
                .sort(sort)
                .remark("seed")
                .build();
        directoryMapper.insert(directory);
        return directory;
    }

    private void insertHistoricalDeletedBinding(Long categoryId, Long directoryId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO dcc_category_directory_binding
                         (id, category_id, directory_id, active, tenant_id, create_time, update_time, creator, updater, deleted)
                     VALUES
                         (?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', ?)
                     """)) {
            statement.setLong(1, randomLongId());
            statement.setLong(2, categoryId);
            statement.setLong(3, directoryId);
            statement.setBoolean(4, true);
            statement.setBoolean(5, true);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

}
