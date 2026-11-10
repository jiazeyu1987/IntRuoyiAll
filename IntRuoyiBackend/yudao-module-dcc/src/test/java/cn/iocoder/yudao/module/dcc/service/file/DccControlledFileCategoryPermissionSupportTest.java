package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class DccControlledFileCategoryPermissionSupportTest extends BaseMockitoUnitTest {
    @Mock private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper projectMapper;
    @Mock private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccessService;
    @org.junit.jupiter.api.BeforeEach void tenant() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
    }
    @org.junit.jupiter.api.AfterEach void clearTenant() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
    }

    @Mock
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DeptApi deptApi;

    @InjectMocks
    private DccControlledFileCategoryPermissionSupport permissionSupport;

    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO libraryFile() {
        var library=new cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO();
        library.setId(7L);library.setTenantId(1L);library.setLibraryScope("QMS");
        when(projectMapper.selectById(7L)).thenReturn(library);
        var file=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder()
                .id(10L).dccProjectCodeId(7L).categoryId(100L).build();file.setTenantId(1L);return file;
    }

    @Test void libraryViewAllowsBodyButDoesNotGrantOriginalDownloadOrEditing() {
        var file=libraryFile();when(projectAccessService.listReadableProjectIds(99L)).thenReturn(List.of(7L));
        assertTrue(permissionSupport.hasFilePermission(file,99L,DccFileCategoryPermissionActionEnum.VIEW));
        assertFalse(permissionSupport.hasFilePermission(file,99L,DccFileCategoryPermissionActionEnum.DOWNLOAD));
        assertFalse(permissionSupport.hasEditableProjectCategory(99L,7L,100L));
        org.mockito.Mockito.verifyNoInteractions(permissionRuleMapper);
    }

    @Test void libraryEditAllowsOriginalDownloadWithoutProjectCategoryRules() {
        var file=libraryFile();when(projectAccessService.hasProjectEditorOrOwner(99L,7L)).thenReturn(true);
        assertTrue(permissionSupport.hasFilePermission(file,99L,DccFileCategoryPermissionActionEnum.DOWNLOAD));
        assertTrue(permissionSupport.hasEditableProjectCategory(99L,7L,100L));
        org.mockito.Mockito.verifyNoInteractions(permissionRuleMapper);
    }

    @Test
    void hasCategoryPermission_matchesDepartmentDescendantRule() {
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO()
                .setId(99L)
                .setTenantId(1L).setStatus(0)
                .setDeptId(11L)
                .setPostIds(Set.of(20L)));
        doReturn(List.of(
                rule("VIEW", "DEPT", 10L),
                rule("VIEW", "DEPT", 999L).setActive(false)
        )).when(permissionRuleMapper).selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryPermissionRuleDO, ?>>any(), eq(100L));
        when(deptApi.getChildDeptList(10L)).thenReturn(List.of(childDept(11L, 10L)));

        assertTrue(permissionSupport.hasCategoryPermission(100L, 99L, DccFileCategoryPermissionActionEnum.VIEW));
        assertFalse(permissionSupport.hasCategoryPermission(100L, 99L, DccFileCategoryPermissionActionEnum.DOWNLOAD));
    }

    @Test
    void hasCategoryPermission_rejectsWhenNoSubjectMatches() {
        when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO()
                .setId(99L)
                .setTenantId(1L).setStatus(0)
                .setDeptId(11L)
                .setPostIds(Set.of(20L)));
        doReturn(List.of(
                rule("VIEW", "DEPT", 10L),
                rule("VIEW", "ROLE", 30L),
                rule("VIEW", "POSITION", 21L),
                rule("VIEW", "USER", 100L)
        )).when(permissionRuleMapper).selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryPermissionRuleDO, ?>>any(), eq(100L));
        when(deptApi.getChildDeptList(10L)).thenReturn(List.of());
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(30L))).thenReturn(Set.of(100L));

        assertFalse(permissionSupport.hasCategoryPermission(100L, 99L, DccFileCategoryPermissionActionEnum.VIEW));
    }

    private DccFileCategoryPermissionRuleDO rule(String actionType, String subjectType, Long subjectId) {
        return DccFileCategoryPermissionRuleDO.builder()
                .categoryId(100L)
                .actionType(actionType)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .active(true)
                .build();
    }

    private DeptRespDTO childDept(Long id, Long parentId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setParentId(parentId);
        return dept;
    }
}
