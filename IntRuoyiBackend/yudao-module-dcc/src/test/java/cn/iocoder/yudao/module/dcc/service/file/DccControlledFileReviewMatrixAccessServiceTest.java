package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileReviewMatrixAccessServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccCategoryApprovalRouteMapper routeMapper;
    @Mock
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Mock
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Mock
    private DccApprovalPositionMapper approvalPositionMapper;
    @Mock
    private DccApprovalPositionRuntimeResolver positionRuntimeResolver;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private RoleApi roleApi;

    @InjectMocks
    private DccControlledFileReviewMatrixAccessService accessService;

    @Test
    void canAccessCurrentReviewMatrix_resolvesAssignedPostsUsersAndUploaderDerivedParticipants() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .categoryId(10L)
                .requesterId(88L)
                .build();
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(DccCategoryApprovalRouteDO.builder()
                .id(100L)
                .categoryId(10L)
                .active(Boolean.TRUE)
                .build());
        when(routeNodeMapper.selectListByRouteId(100L)).thenReturn(List.of(
                DccCategoryApprovalRouteNodeDO.builder()
                        .id(201L)
                        .routeId(100L)
                        .stageNo(1)
                        .candidateSourceType("POSITION")
                        .candidateSourceId(50L)
                        .candidateSourceIds("50")
                        .build(),
                DccCategoryApprovalRouteNodeDO.builder()
                        .id(202L)
                        .routeId(100L)
                        .stageNo(2)
                        .candidateSourceType("POSITION")
                        .candidateSourceId(51L)
                        .candidateSourceIds("51")
                        .build(),
                DccCategoryApprovalRouteNodeDO.builder()
                        .id(203L)
                        .routeId(100L)
                        .stageNo(3)
                        .candidateSourceType("USER")
                        .candidateSourceId(301L)
                        .candidateSourceIds("301")
                        .build()));
        when(positionRuntimeResolver.isUploaderDerivedPosition(50L)).thenReturn(true);
        when(positionRuntimeResolver.resolveUserIds(50L, 88L, false)).thenReturn(List.of(200L));
        when(positionRuntimeResolver.isUploaderDerivedPosition(51L)).thenReturn(false);
        when(approvalPositionMapper.selectById(50L)).thenReturn(DccApprovalPositionDO.builder().id(50L).name("上传人岗位").build());
        when(approvalPositionMapper.selectById(51L)).thenReturn(DccApprovalPositionDO.builder().id(51L).name("会签岗位").build());
        when(positionAssignmentMapper.selectActiveListByPositionId(51L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder()
                        .id(401L)
                        .positionId(51L)
                        .assignmentType("POST")
                        .systemPostId(601L)
                        .active(Boolean.TRUE)
                        .build()));
        when(adminUserApi.getUserListByPostIds(List.of(601L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(201L),
                new AdminUserRespDTO().setId(202L)));

        Set<Long> result = accessService.resolveCurrentReviewMatrixUserIds(file);

        assertEquals(Set.of(200L, 201L, 202L, 301L), result);
        assertTrue(accessService.canAccessCurrentReviewMatrix(201L, file));
        assertFalse(accessService.canAccessCurrentReviewMatrix(999L, file));
    }

    @Test
    void resolveCurrentReviewMatrixAccessDetails_explainsSubjectsAndEmptyPositions() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .categoryId(20L)
                .requesterId(99L)
                .build();
        when(routeMapper.selectLatestActiveByCategoryId(20L)).thenReturn(DccCategoryApprovalRouteDO.builder()
                .id(200L)
                .categoryId(20L)
                .versionNo(4)
                .active(Boolean.TRUE)
                .build());
        when(routeNodeMapper.selectListByRouteId(200L)).thenReturn(List.of(
                DccCategoryApprovalRouteNodeDO.builder()
                        .id(301L)
                        .routeId(200L)
                        .stageNo(2)
                        .stageName("审核会签")
                        .candidateSourceType("POSITION")
                        .candidateSourceId(71L)
                        .candidateSourceIds("71,72")
                        .build()));
        when(positionRuntimeResolver.isUploaderDerivedPosition(71L)).thenReturn(false);
        when(positionRuntimeResolver.isUploaderDerivedPosition(72L)).thenReturn(false);
        when(approvalPositionMapper.selectById(71L)).thenReturn(DccApprovalPositionDO.builder().id(71L).name("会签岗位A").build());
        when(approvalPositionMapper.selectById(72L)).thenReturn(DccApprovalPositionDO.builder().id(72L).name("会签岗位B").build());
        when(positionAssignmentMapper.selectActiveListByPositionId(71L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder()
                        .id(501L)
                        .positionId(71L)
                        .assignmentType("USER")
                        .userId(401L)
                        .active(Boolean.TRUE)
                        .build()));
        when(positionAssignmentMapper.selectActiveListByPositionId(72L)).thenReturn(List.of());
        when(adminUserApi.getUserList(List.of(401L))).thenReturn(List.of(new AdminUserRespDTO()
                .setId(401L)
                .setNickname("会签用户")));

        DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessResolution result =
                accessService.resolveCurrentReviewMatrixAccessDetails(file);

        assertEquals(1, result.subjects().size());
        assertEquals(401L, result.subjects().get(0).userId());
        assertEquals("会签用户", result.subjects().get(0).userName());
        assertEquals(2, result.subjects().get(0).stageNo());
        assertEquals(71L, result.subjects().get(0).positionId());
        assertEquals("CURRENT_REVIEW_MATRIX", result.subjects().get(0).source());
        assertTrue(result.risks().stream().anyMatch(risk ->
                "POSITION_EMPTY".equals(risk.code()) && risk.message().contains("72")));
    }

    @Test
    void resolveCurrentReviewMatrixAccessDetails_resolvesDeptLeaderAndRoleUsers() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .categoryId(30L)
                .requesterId(100L)
                .build();
        when(routeMapper.selectLatestActiveByCategoryId(30L)).thenReturn(DccCategoryApprovalRouteDO.builder()
                .id(300L)
                .categoryId(30L)
                .versionNo(5)
                .active(Boolean.TRUE)
                .build());
        when(routeNodeMapper.selectListByRouteId(300L)).thenReturn(List.of(
                DccCategoryApprovalRouteNodeDO.builder()
                        .id(401L)
                        .routeId(300L)
                        .stageNo(2)
                        .stageName("审核")
                        .candidateSourceType("DEPT")
                        .candidateSourceId(81L)
                        .candidateSourceIds("81")
                        .build(),
                DccCategoryApprovalRouteNodeDO.builder()
                        .id(402L)
                        .routeId(300L)
                        .stageNo(3)
                        .stageName("批准")
                        .candidateSourceType("ROLE")
                        .candidateSourceId(91L)
                        .candidateSourceIds("91")
                        .build()));
        when(deptApi.getDeptList(List.of(81L))).thenReturn(List.of(
                new DeptRespDTO().setId(81L).setName("研发创新中心").setLeaderUserId(701L)));
        when(adminUserApi.getUser(701L)).thenReturn(new AdminUserRespDTO().setId(701L).setNickname("部门负责人"));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(91L))).thenReturn(Set.of(801L, 802L));
        when(adminUserApi.getUserList(List.of(701L, 801L, 802L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(701L).setNickname("部门负责人"),
                new AdminUserRespDTO().setId(801L).setNickname("角色成员A"),
                new AdminUserRespDTO().setId(802L).setNickname("角色成员B")));

        DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessResolution result =
                accessService.resolveCurrentReviewMatrixAccessDetails(file);

        assertEquals(Set.of(701L, 801L, 802L), result.subjects().stream()
                .map(DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessSubject::userId)
                .collect(java.util.stream.Collectors.toSet()));
        assertTrue(result.subjects().stream().anyMatch(subject ->
                subject.userId().equals(701L) && subject.reason().contains("负责人解析")));
        assertTrue(result.subjects().stream().anyMatch(subject ->
                subject.userId().equals(801L) && subject.reason().contains("系统角色解析")));
        verify(roleApi).validRoleList(List.of(91L));
    }

    @Test
    void resolveCurrentReviewMatrixAccessDetails_blocksWhenDeptLeaderOrRoleUsersMissing() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .categoryId(40L)
                .requesterId(100L)
                .build();
        when(routeMapper.selectLatestActiveByCategoryId(40L)).thenReturn(DccCategoryApprovalRouteDO.builder()
                .id(400L)
                .categoryId(40L)
                .versionNo(2)
                .active(Boolean.TRUE)
                .build());
        when(routeNodeMapper.selectListByRouteId(400L)).thenReturn(List.of(
                DccCategoryApprovalRouteNodeDO.builder()
                        .id(501L)
                        .routeId(400L)
                        .stageNo(2)
                        .stageName("审核")
                        .candidateSourceType("DEPT")
                        .candidateSourceId(82L)
                        .candidateSourceIds("82")
                        .build(),
                DccCategoryApprovalRouteNodeDO.builder()
                        .id(502L)
                        .routeId(400L)
                        .stageNo(3)
                        .stageName("批准")
                        .candidateSourceType("ROLE")
                        .candidateSourceId(92L)
                        .candidateSourceIds("92")
                        .build()));
        when(deptApi.getDeptList(List.of(82L))).thenReturn(List.of(
                new DeptRespDTO().setId(82L).setName("质量体系中心").setLeaderUserId(null)));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(92L))).thenReturn(Set.of());

        DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessResolution result =
                accessService.resolveCurrentReviewMatrixAccessDetails(file);

        assertTrue(result.subjects().isEmpty());
        assertTrue(result.risks().stream().anyMatch(risk -> "DEPT_LEADER_MISSING".equals(risk.code())));
        assertTrue(result.risks().stream().anyMatch(risk -> "ROLE_EMPTY".equals(risk.code())));
    }
}
