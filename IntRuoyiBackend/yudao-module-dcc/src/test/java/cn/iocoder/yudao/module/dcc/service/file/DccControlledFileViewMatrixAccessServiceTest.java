package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyCollection;
import static cn.iocoder.yudao.module.dcc.service.file.DccControlledFileViewMatrixAccessService.SUBJECT_DCC_POSITION;
import static cn.iocoder.yudao.module.dcc.service.file.DccControlledFileViewMatrixAccessService.SUBJECT_DEPT;
import static cn.iocoder.yudao.module.dcc.service.file.DccControlledFileViewMatrixAccessService.SUBJECT_POST;
import static cn.iocoder.yudao.module.dcc.service.file.DccControlledFileViewMatrixAccessService.SUBJECT_UNMAPPED_EXCEL;
import static cn.iocoder.yudao.module.dcc.service.file.DccControlledFileViewMatrixAccessService.SUBJECT_USER;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileViewMatrixAccessService.ViewMatrixRuleInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DccControlledFileViewMatrixAccessServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccCategoryViewMatrixRuleMapper ruleMapper;
    @Mock
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Mock
    private DccApprovalPositionMapper approvalPositionMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;

    @InjectMocks
    private DccControlledFileViewMatrixAccessService accessService;

    @Test
    void previewViewMatrixAccessDetails_resolvesUserDepartmentPostAndReportsDuplicateUser() {
        when(adminUserApi.getUser(201L)).thenReturn(user(201L, "文控"));
        when(deptApi.getChildDeptList(300L)).thenReturn(List.of());
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(user(201L, "文控"), user(202L, "QA")));
        when(adminUserApi.getUserListByPostIds(List.of(400L))).thenReturn(List.of(user(203L, "工艺")));

        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("A", "文控", "●", SUBJECT_USER, 201L),
                rule("B", "QA", "●", SUBJECT_DEPT, 300L),
                rule("C", "生产", "●", SUBJECT_POST, 400L)));

        assertEquals(List.of(201L, 201L, 202L, 203L),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userId)
                        .toList());
        assertTrue(result.risks().stream().anyMatch(risk -> "VIEW_MATRIX_DUPLICATE_USER".equals(risk.code())));
        assertFalse(result.risks().stream().anyMatch(DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk::blocking));
        assertTrue(result.subjects().stream().allMatch(subject ->
                DccControlledFileViewMatrixAccessService.SOURCE_CURRENT_VIEW_MATRIX.equals(subject.source())));
    }

    @Test
    void previewViewMatrixAccessDetails_resolvesDepartmentManagerScopeToDeptLeader() {
        when(deptApi.getDeptList(List.of(300L))).thenReturn(List.of(dept(300L, "新品开发部", 205L)));
        when(adminUserApi.getUser(205L)).thenReturn(user(205L, "新品开发部负责人"));

        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("E", "新品开发部", "▲", SUBJECT_DEPT, 300L)));

        assertEquals(List.of(205L),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userId)
                        .toList());
        assertEquals(List.of("新品开发部负责人"),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userName)
                        .toList());
        assertTrue(result.risks().isEmpty());
        assertTrue(result.subjects().stream().allMatch(subject ->
                DccControlledFileViewMatrixAccessService.SCOPE_MANAGER_AND_ABOVE.equals(subject.scopeType())));
        assertTrue(result.subjects().stream().allMatch(subject ->
                subject.reason().contains("负责人解析")));
    }

    @Test
    void previewViewMatrixAccessDetails_resolvesDepartmentAllMembersRecursively() {
        when(deptApi.getChildDeptList(300L)).thenReturn(List.of(
                dept(301L, "注册部", null),
                dept(302L, "QMS", null)));
        when(adminUserApi.getUserListByDeptIds(List.of(300L, 301L, 302L))).thenReturn(List.of(
                user(201L, "上级部门人员"),
                user(202L, "注册部人员"),
                user(203L, "QMS 人员")));

        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("G", "质量体系中心", "●", SUBJECT_DEPT, 300L)));

        assertEquals(List.of(201L, 202L, 203L),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userId)
                        .toList());
        assertFalse(result.risks().stream().anyMatch(DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk::blocking));
    }

    @Test
    void resolveRules_reusesDepartmentTreeAndUserListWithinResolutionContext() {
        when(deptApi.getChildDeptList(300L)).thenReturn(List.of());
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(user(201L, "赵杰")));

        var result = accessService.resolveRules(10L, List.of(
                rule("G", "QMS", "●", SUBJECT_DEPT, 300L),
                rule("H", "QMS", "●", SUBJECT_DEPT, 300L)));

        assertEquals(List.of(201L, 201L),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userId)
                        .toList());
        verify(deptApi, times(1)).getChildDeptList(300L);
        verify(adminUserApi, times(1)).getUserListByDeptIds(List.of(300L));
    }

    @Test
    void previewViewMatrixAccessDetails_resolvesDepartmentManagerScopeOnlyToSelectedDeptLeader() {
        when(deptApi.getDeptList(List.of(300L))).thenReturn(List.of(dept(300L, "质量体系中心", 205L)));
        when(adminUserApi.getUser(205L)).thenReturn(user(205L, "质量体系中心负责人"));

        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("H", "质量体系中心", "▲", SUBJECT_DEPT, 300L)));

        assertEquals(List.of(205L),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userId)
                        .toList());
        assertTrue(result.subjects().stream().allMatch(subject ->
                DccControlledFileViewMatrixAccessService.SCOPE_MANAGER_AND_ABOVE.equals(subject.scopeType())));
        assertFalse(result.risks().stream().anyMatch(DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk::blocking));
        verify(deptApi, times(0)).getChildDeptList(300L);
    }

    @Test
    void previewViewMatrixAccessDetails_blocksDepartmentManagerScopeWhenDeptHasNoLeader() {
        when(deptApi.getDeptList(List.of(300L))).thenReturn(List.of(dept(300L, "新品开发部", null)));

        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("E", "新品开发部", "▲", SUBJECT_DEPT, 300L)));

        assertTrue(result.subjects().isEmpty());
        assertEquals(Set.of("VIEW_MATRIX_DEPT_LEADER_MISSING"), riskCodes(result));
        assertTrue(result.risks().stream().allMatch(DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk::blocking));
    }

    @Test
    void previewViewMatrixAccessDetails_blocksDepartmentManagerScopeWhenLeaderUserIsMissing() {
        when(deptApi.getDeptList(List.of(300L))).thenReturn(List.of(dept(300L, "新品开发部", 205L)));
        when(adminUserApi.getUser(205L)).thenReturn(null);

        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("E", "新品开发部", "▲", SUBJECT_DEPT, 300L)));

        assertTrue(result.subjects().isEmpty());
        assertEquals(Set.of("VIEW_MATRIX_DEPT_LEADER_USER_NOT_FOUND"), riskCodes(result));
        assertTrue(result.risks().stream().allMatch(DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk::blocking));
    }

    @Test
    void previewViewMatrixAccessDetails_blocksUnmappedExcelAndUnsupportedManagerScopeRule() {
        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("D", "市场 / 注册", "●", SUBJECT_UNMAPPED_EXCEL, null),
                rule("E", "生产主管及以上", "▲", SUBJECT_POST, 300L)));

        assertTrue(result.subjects().isEmpty());
        assertEquals(Set.of("VIEW_MATRIX_SUBJECT_UNMAPPED", "VIEW_MATRIX_MANAGER_SCOPE_UNRESOLVED"),
                riskCodes(result));
        assertTrue(result.risks().stream().allMatch(DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk::blocking));
    }

    @Test
    void previewViewMatrixAccessDetails_blocksEmptyPostAndMissingSubjectId() {
        when(adminUserApi.getUserListByPostIds(List.of(400L))).thenReturn(List.of());

        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("F", "空岗位", "●", SUBJECT_POST, 400L),
                rule("G", "缺少主体", "●", SUBJECT_USER, null)));

        assertTrue(result.subjects().isEmpty());
        assertEquals(Set.of("VIEW_MATRIX_SUBJECT_EMPTY", "VIEW_MATRIX_SUBJECT_ID_MISSING"), riskCodes(result));
        assertTrue(result.risks().stream().allMatch(DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk::blocking));
    }

    @Test
    void previewViewMatrixAccessDetails_resolvesDccPositionAssignmentsAndFillsUserNames() {
        when(approvalPositionMapper.selectById(500L)).thenReturn(DccApprovalPositionDO.builder()
                .id(500L).name("文档管理员").build());
        when(positionAssignmentMapper.selectActiveListByPositionId(500L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(1L).positionId(500L).assignmentType("USER")
                        .userId(301L).active(Boolean.TRUE).build(),
                DccPositionAssignmentDO.builder().id(2L).positionId(500L).assignmentType("POST")
                        .systemPostId(401L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(401L))).thenReturn(List.of(user(302L, null)));
        when(adminUserApi.getUserList(List.of(301L, 302L))).thenReturn(List.of(user(301L, "赵杰"), user(302L, "瑛泰管理员")));

        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("H", "文档管理员", "●", SUBJECT_DCC_POSITION, 500L)));

        assertEquals(List.of(301L, 302L),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userId)
                        .toList());
        assertEquals(List.of("赵杰", "瑛泰管理员"),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userName)
                        .toList());
        assertTrue(result.risks().isEmpty());
    }

    @Test
    void previewViewMatrixAccessDetails_resolvesRoleSubjectForManagerScopeAndBlocksEmptyRole() {
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(900L))).thenReturn(Set.of(301L, 302L));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(901L))).thenReturn(Set.of());
        when(adminUserApi.getUserList(anyCollection())).thenReturn(List.of(user(301L, "主管甲"), user(302L, "主管乙")));

        var result = accessService.previewViewMatrixAccessDetails(10L, List.of(
                rule("D", "新品开发部", "▲", "ROLE", 900L),
                rule("E", "空主管角色", "▲", "ROLE", 901L)));

        assertEquals(List.of(301L, 302L),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userId)
                        .toList());
        assertEquals(List.of("主管甲", "主管乙"),
                result.subjects().stream().map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userName)
                        .toList());
        assertTrue(result.risks().stream().anyMatch(risk -> "VIEW_MATRIX_SUBJECT_EMPTY".equals(risk.code())
                && Boolean.TRUE.equals(risk.blocking())));
    }

    private ViewMatrixRuleInput rule(String column, String label, String marker, String subjectType, Long subjectId) {
        return new ViewMatrixRuleInput(null, 10L, "电子文控系统推进计划及需求表.xlsx", 12, column, label,
                label, null, marker, null, subjectType, subjectId, Boolean.TRUE, null);
    }

    private AdminUserRespDTO user(Long id, String nickname) {
        return new AdminUserRespDTO().setId(id).setNickname(nickname);
    }

    private DeptRespDTO dept(Long id, String name, Long leaderUserId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setLeaderUserId(leaderUserId);
        return dept;
    }

    private Set<String> riskCodes(DccControlledFileViewMatrixAccessService.ViewMatrixAccessResolution result) {
        return result.risks().stream()
                .map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk::code)
                .collect(Collectors.toSet());
    }
}
