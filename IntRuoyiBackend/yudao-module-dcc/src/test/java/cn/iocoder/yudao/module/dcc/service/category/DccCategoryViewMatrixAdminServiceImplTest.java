package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileCategoryPermissionSupport;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileViewMatrixAccessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_VIEW_MATRIX_EFFECTIVE_ACCESS_BLOCKED;
import static cn.iocoder.yudao.module.dcc.service.file.DccControlledFileViewMatrixAccessService.SOURCE_CURRENT_VIEW_MATRIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import({
        DccCategoryViewMatrixAdminServiceImpl.class,
        DccControlledFileViewMatrixAccessService.class,
        DccControlledFileCategoryPermissionSupport.class
})
class DccCategoryViewMatrixAdminServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccCategoryViewMatrixAdminServiceImpl viewMatrixAdminService;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccCategoryViewMatrixRuleMapper ruleMapper;

    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;

    @Test
    void savePreviewRowsAndUserLookup_resolveCurrentViewMatrixSource() {
        DccFileCategoryDO category = createCategory("VIEW-MATRIX-1", "文件查阅矩阵测试");
        when(adminUserApi.getUser(201L)).thenReturn(new AdminUserRespDTO().setId(201L).setNickname("赵杰"));
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(201L)).thenReturn(false);

        DccCategoryViewMatrixSaveReqVO reqVO = saveReq(rule("A", "文控", "●", "USER", 201L));

        var preview = viewMatrixAdminService.previewViewMatrix(category.getId(), reqVO);
        assertFalse(Boolean.TRUE.equals(preview.getBlocking()));
        assertEquals(List.of(201L), preview.getViewSubjects().stream().map(item -> item.getUserId()).toList());

        var savedRules = viewMatrixAdminService.saveViewMatrix(category.getId(), reqVO);
        assertEquals(1, savedRules.size());
        assertEquals(1, ruleMapper.selectList(DccCategoryViewMatrixRuleDO::getCategoryId, category.getId()).size());

        var rows = viewMatrixAdminService.getViewMatrixRows("VIEW-MATRIX-1", null, true, true);
        assertEquals(1, rows.size());
        assertTrue(Boolean.TRUE.equals(rows.get(0).getConfigured()));
        assertEquals("当前查看矩阵参与人可浏览、查看详情和预览已发布受控副本",
                rows.get(0).getViewRuleSummary());
        assertEquals(List.of("赵杰"),
                rows.get(0).getViewSubjects().stream().map(item -> item.getUserName()).toList());

        var lookupRows = viewMatrixAdminService.getUserViewMatrixAccess(201L);
        assertEquals(1, lookupRows.size());
        assertEquals("YES", lookupRows.get(0).getBrowseStatus());
        assertEquals(SOURCE_CURRENT_VIEW_MATRIX, lookupRows.get(0).getBrowseSource());
        assertEquals(SOURCE_CURRENT_VIEW_MATRIX, lookupRows.get(0).getDetailSource());
        assertEquals(SOURCE_CURRENT_VIEW_MATRIX, lookupRows.get(0).getPublishedPreviewSource());
        assertEquals("ROUTE_SNAPSHOT", lookupRows.get(0).getPendingPreviewSource());
        assertEquals("DOWNLOAD_POLICY", lookupRows.get(0).getDownloadSource());
    }

    @Test
    void saveViewMatrix_resolvesDepartmentManagerScopeToDeptLeaderAndPersistsRules() {
        DccFileCategoryDO category = createCategory("VIEW-MATRIX-2", "部门负责人解析测试");
        when(deptApi.getDept(300L)).thenReturn(deptWithLeader(300L, "新品开发部", null, 205L));
        when(deptApi.getChildDeptList(300L)).thenReturn(List.of());
        when(deptApi.getDeptList(List.of(300L))).thenReturn(List.of(deptWithLeader(300L, "新品开发部", null, 205L)));
        when(adminUserApi.getUser(205L)).thenReturn(new AdminUserRespDTO().setId(205L).setNickname("新品开发负责人"));

        DccCategoryViewMatrixSaveReqVO reqVO = saveReq(rule("B", "新品开发部", "▲", "DEPT", 300L));

        var preview = viewMatrixAdminService.previewViewMatrix(category.getId(), reqVO);
        assertFalse(Boolean.TRUE.equals(preview.getBlocking()));
        assertEquals(List.of(205L), preview.getViewSubjects().stream().map(item -> item.getUserId()).toList());
        assertTrue(preview.getViewSubjects().get(0).getReason().contains("负责人解析"));

        var savedRules = viewMatrixAdminService.saveViewMatrix(category.getId(), reqVO);
        assertEquals(1, savedRules.size());
        assertEquals(1, ruleMapper.selectList(DccCategoryViewMatrixRuleDO::getCategoryId, category.getId()).size());
    }

    @Test
    void saveViewMatrix_ignoresIncomingRuleIdWhenRebuildingRules() {
        DccFileCategoryDO category = createCategory("VIEW-MATRIX-ID", "旧主键重建测试");
        when(adminUserApi.getUser(201L)).thenReturn(new AdminUserRespDTO().setId(201L).setNickname("赵杰"));
        ruleMapper.insert(DccCategoryViewMatrixRuleDO.builder()
                .id(766L)
                .categoryId(category.getId())
                .excelColumnLetter("A")
                .subjectLabel("文控")
                .marker("●")
                .scopeType(DccControlledFileViewMatrixAccessService.SCOPE_ALL_MEMBERS)
                .subjectType(DccControlledFileViewMatrixAccessService.SUBJECT_USER)
                .subjectId(201L)
                .active(Boolean.TRUE)
                .build());
        DccCategoryViewMatrixSaveReqVO.Rule rule = rule("A", "文控", "●", "USER", 201L);
        rule.setId(766L);

        var savedRules = viewMatrixAdminService.saveViewMatrix(category.getId(), saveReq(rule));

        assertEquals(1, savedRules.size());
        assertNotEquals(766L, savedRules.get(0).getId());
        assertEquals(1, ruleMapper.selectList(DccCategoryViewMatrixRuleDO::getCategoryId, category.getId()).size());
    }

    @Test
    void saveViewMatrix_blocksUnsupportedManagerScopeAndDoesNotPersistRules() {
        DccFileCategoryDO category = createCategory("VIEW-MATRIX-2", "主管范围阻塞测试");
        DccCategoryViewMatrixSaveReqVO reqVO = saveReq(rule("B", "QA 主管及以上", "▲", "POST", 300L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> viewMatrixAdminService.saveViewMatrix(category.getId(), reqVO));

        assertEquals(CATEGORY_VIEW_MATRIX_EFFECTIVE_ACCESS_BLOCKED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("没有明确主管及以上解析规则"));
        assertTrue(ruleMapper.selectList(DccCategoryViewMatrixRuleDO::getCategoryId, category.getId()).isEmpty());
    }

    @Test
    void importViewMatrix_allowsEmptyRulesWithoutBlockingPreview() {
        DccFileCategoryDO category = createCategory("VIEW-MATRIX-IMPORT", "导入空矩阵测试");
        ruleMapper.insert(DccCategoryViewMatrixRuleDO.builder()
                .categoryId(category.getId())
                .excelColumnLetter("A")
                .subjectLabel("文控")
                .marker("●")
                .scopeType(DccControlledFileViewMatrixAccessService.SCOPE_ALL_MEMBERS)
                .subjectType(DccControlledFileViewMatrixAccessService.SUBJECT_USER)
                .subjectId(201L)
                .active(Boolean.TRUE)
                .build());

        DccCategoryViewMatrixSaveReqVO reqVO = saveReq();

        ServiceException saveException = assertThrows(ServiceException.class,
                () -> viewMatrixAdminService.saveViewMatrix(category.getId(), reqVO));
        assertEquals(CATEGORY_VIEW_MATRIX_EFFECTIVE_ACCESS_BLOCKED.getCode(), saveException.getCode());

        var importedRules = viewMatrixAdminService.importViewMatrix(category.getId(), reqVO);

        assertTrue(importedRules.isEmpty());
        assertTrue(ruleMapper.selectList(DccCategoryViewMatrixRuleDO::getCategoryId, category.getId()).isEmpty());
    }

    @Test
    void getViewMatrixRows_resolvesDepartmentTreePathForDepartmentRule() {
        DccFileCategoryDO category = createCategory("VIEW-MATRIX-3", "部门路径测试");
        ruleMapper.insert(DccCategoryViewMatrixRuleDO.builder()
                .categoryId(category.getId())
                .excelColumnLetter("G")
                .subjectLabel("QMS")
                .marker("●")
                .scopeType(DccControlledFileViewMatrixAccessService.SCOPE_ALL_MEMBERS)
                .subjectType(DccControlledFileViewMatrixAccessService.SUBJECT_DEPT)
                .subjectId(300L)
                .active(Boolean.TRUE)
                .build());
        when(deptApi.getDept(300L)).thenReturn(dept(300L, "QMS", 200L));
        when(deptApi.getDept(200L)).thenReturn(dept(200L, "质量体系中心", 100L));
        when(deptApi.getDept(100L)).thenReturn(dept(100L, "瑛泰医疗", 0L));

        var rows = viewMatrixAdminService.getViewMatrixRows("VIEW-MATRIX-3", null, true, true);

        assertEquals(1, rows.size());
        assertEquals("瑛泰医疗-质量体系中心-QMS",
                rows.get(0).getRules().get(0).getSubjectDepartmentPath());
    }

    @Test
    void getViewMatrixRows_returnsCategorySortForActiveToggleUpdate() {
        createCategory("VIEW-MATRIX-SORT", "排序字段测试", 37);

        var rows = viewMatrixAdminService.getViewMatrixRows("VIEW-MATRIX-SORT", null, true, null);

        assertEquals(1, rows.size());
        assertEquals(37, rows.get(0).getSort());
    }

    @Test
    void getViewMatrixRows_reusesDepartmentResolutionForRepeatedDepartmentRules() {
        DccFileCategoryDO first = createCategory("VIEW-MATRIX-CACHE-1", "总览缓存测试一");
        DccFileCategoryDO second = createCategory("VIEW-MATRIX-CACHE-2", "总览缓存测试二");
        insertDepartmentRule(first.getId(), 300L);
        insertDepartmentRule(second.getId(), 300L);
        when(deptApi.getChildDeptList(300L)).thenReturn(List.of());
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(201L).setNickname("赵杰")));
        when(deptApi.getDept(300L)).thenReturn(dept(300L, "QMS", 200L));
        when(deptApi.getDept(200L)).thenReturn(dept(200L, "质量体系中心", 100L));
        when(deptApi.getDept(100L)).thenReturn(dept(100L, "瑛泰医疗", 0L));

        var rows = viewMatrixAdminService.getViewMatrixRows("VIEW-MATRIX-CACHE", null, true, true);

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.getViewSubjects().size() == 1));
        assertTrue(rows.stream().allMatch(row -> "瑛泰医疗-质量体系中心-QMS"
                .equals(row.getRules().get(0).getSubjectDepartmentPath())));
        verify(deptApi, times(1)).getChildDeptList(300L);
        verify(adminUserApi, times(1)).getUserListByDeptIds(List.of(300L));
        verify(deptApi, times(1)).getDept(300L);
        verify(deptApi, times(1)).getDept(200L);
        verify(deptApi, times(1)).getDept(100L);
    }

    private DccFileCategoryDO createCategory(String code, String name) {
        return createCategory(code, name, 1);
    }

    private DccFileCategoryDO createCategory(String code, String name, Integer sort) {
        DccFileCategoryDO category = DccFileCategoryDO.builder()
                .id(randomLongId())
                .code(code)
                .name(name)
                .active(Boolean.TRUE)
                .sort(sort)
                .source("LOCAL")
                .description(name)
                .lifecycleStage("PLAN")
                .distributionRequired(Boolean.FALSE)
                .trainingRequired(Boolean.FALSE)
                .build();
        categoryMapper.insert(category);
        return category;
    }

    private DccCategoryViewMatrixSaveReqVO saveReq(DccCategoryViewMatrixSaveReqVO.Rule... rules) {
        DccCategoryViewMatrixSaveReqVO reqVO = new DccCategoryViewMatrixSaveReqVO();
        reqVO.setRules(List.of(rules));
        return reqVO;
    }

    private DccCategoryViewMatrixSaveReqVO.Rule rule(String column, String label, String marker,
                                                    String subjectType, Long subjectId) {
        DccCategoryViewMatrixSaveReqVO.Rule rule = new DccCategoryViewMatrixSaveReqVO.Rule();
        rule.setExcelFileName("电子文控系统推进计划及需求表.xlsx");
        rule.setExcelRowNo(12);
        rule.setExcelColumnLetter(column);
        rule.setSubjectLabel(label);
        rule.setMarker(marker);
        rule.setSubjectType(subjectType);
        rule.setSubjectId(subjectId);
        rule.setActive(Boolean.TRUE);
        return rule;
    }

    private void insertDepartmentRule(Long categoryId, Long deptId) {
        ruleMapper.insert(DccCategoryViewMatrixRuleDO.builder()
                .categoryId(categoryId)
                .excelColumnLetter("G")
                .subjectLabel("QMS")
                .marker("●")
                .scopeType(DccControlledFileViewMatrixAccessService.SCOPE_ALL_MEMBERS)
                .subjectType(DccControlledFileViewMatrixAccessService.SUBJECT_DEPT)
                .subjectId(deptId)
                .active(Boolean.TRUE)
                .build());
    }

    private DeptRespDTO dept(Long id, String name, Long parentId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(parentId);
        return dept;
    }

    private DeptRespDTO deptWithLeader(Long id, String name, Long parentId, Long leaderUserId) {
        DeptRespDTO dept = dept(id, name, parentId);
        dept.setLeaderUserId(leaderUserId);
        return dept;
    }
}
