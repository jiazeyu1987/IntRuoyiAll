package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccControlledFileApprovalRouteAssigneeResolverTest extends BaseMockitoUnitTest {

    @Mock
    private DccCategoryApprovalRouteMapper routeMapper;
    @Mock
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Mock
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Mock
    private DccApprovalPositionRuntimeResolver positionRuntimeResolver;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private DccApprovalParticipantPostValidator approvalParticipantPostValidator;

    @InjectMocks
    private DccControlledFileApprovalRouteAssigneeResolver resolver;

    @Test
    void resolveStartUserSelectAssignees_userSourceReturnsDocControlReviewOnly() {
        DccControlledFileDO file = DccControlledFileDO.builder().id(900L).categoryId(10L).build();
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(20L).categoryId(10L).versionNo(3).active(Boolean.TRUE).build();
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(route);
        when(routeNodeMapper.selectListByRouteId(20L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "USER", null,
                        "914518,914519", 1),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "USER", 914520L,
                        null, 2),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "USER", 914521L,
                        null, 3),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "USER", 914522L,
                        null, 4)
        ));

        Map<String, List<Long>> result = resolver.resolveStartUserSelectAssignees(file, 99L);

        assertEquals(Map.of(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), List.of(914518L, 914519L)),
                result);
        verifyNoInteractions(positionAssignmentMapper, positionRuntimeResolver);
        verify(adminUserApi).validateUserList(List.of(914518L, 914519L));
        verify(adminUserApi).validateUserList(List.of(914520L));
    }

    @Test
    void resolveStartUserSelectAssignees_positionSourceReturnsAssignedUsersAndPostUsers() {
        DccControlledFileDO file = DccControlledFileDO.builder().id(901L).categoryId(11L).build();
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(21L).categoryId(11L).versionNo(1).active(Boolean.TRUE).build();
        when(routeMapper.selectLatestActiveByCategoryId(11L)).thenReturn(route);
        when(routeNodeMapper.selectListByRouteId(21L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "POSITION", 301L,
                        null, 1),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "USER", 914520L,
                        null, 2),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "USER", 914521L,
                        null, 3),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "USER", 914522L,
                        null, 4)
        ));
        when(positionRuntimeResolver.isUploaderDerivedPosition(301L)).thenReturn(false);
        when(positionAssignmentMapper.selectActiveListByPositionId(301L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().positionId(301L).userId(914518L).active(Boolean.TRUE).build(),
                DccPositionAssignmentDO.builder().positionId(301L).assignmentType("POST").systemPostId(500L)
                        .active(Boolean.TRUE).build()
        ));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(914519L)));

        Map<String, List<Long>> result = resolver.resolveStartUserSelectAssignees(file, 99L);

        assertEquals(Map.of(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), List.of(914518L, 914519L)),
                result);
    }

    @Test
    void resolveRoute_deptSourceResolvesDepartmentLeadersAndKeepsDuplicateLeaderObligations() {
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(26L).categoryId(16L).versionNo(1).active(Boolean.TRUE).build();
        when(routeMapper.selectLatestActiveByCategoryId(16L)).thenReturn(route);
        when(routeNodeMapper.selectListByRouteId(26L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "USER", 914518L,
                        null, 1),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "DEPT", null,
                        "81,82", 2),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "USER", 914521L,
                        null, 3),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "USER", 914522L,
                        null, 4)
        ));
        when(deptApi.getDeptList(List.of(81L, 82L))).thenReturn(List.of(
                dept(81L, "生产部", 700L),
                dept(82L, "质量部", 700L)
        ));

        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute result = resolver.resolveRoute(16L, 99L);

        assertEquals(List.of(700L, 700L), result.nodes().get(1).resolvedUserIds());
        assertEquals(List.of(81L, 82L), result.nodes().get(1).candidateSourceIds());
    }

    @Test
    void resolveRoute_deptSourceWithoutLeaderFailsFastForWholeRoute() {
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(27L).categoryId(17L).versionNo(1).active(Boolean.TRUE).build();
        when(routeMapper.selectLatestActiveByCategoryId(17L)).thenReturn(route);
        when(routeNodeMapper.selectListByRouteId(27L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "USER", 914518L,
                        null, 1),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "DEPT", 81L,
                        null, 2),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "USER", 914521L,
                        null, 3),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "USER", 914522L,
                        null, 4)
        ));
        when(deptApi.getDeptList(List.of(81L))).thenReturn(List.of(dept(81L, "生产部", null)));

        assertServiceException(() -> resolver.resolveRoute(17L, 99L),
                ROUTE_PREVIEW_APPROVER_NOT_FOUND);
    }

    @Test
    void resolveRoute_actionTypeUsesIndependentRouteAndDoesNotFallbackToLegacy() {
        when(routeMapper.selectLatestActiveByCategoryIdAndActionType(18L, "NEW")).thenReturn(null);

        assertServiceException(() -> resolver.resolveRoute(18L, 99L, "NEW"),
                CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        verify(routeMapper, never()).selectLatestActiveByCategoryId(18L);
    }

    @Test
    void resolveRoute_actionTypeStartsWithMatrixReview() {
        DccCategoryApprovalRouteDO uploadRoute = DccCategoryApprovalRouteDO.builder()
                .id(29L).categoryId(19L).versionNo(1).actionType("NEW").active(Boolean.TRUE).build();
        when(routeMapper.selectLatestActiveByCategoryIdAndActionType(19L, "NEW")).thenReturn(uploadRoute);
        when(routeNodeMapper.selectListByRouteId(29L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "DEPT", null, "81,82", 1),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "USER", 914521L, null, 2),
                routeNode(3, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "USER", 914522L, null, 3)
        ));
        when(deptApi.getDeptList(List.of(81L, 82L))).thenReturn(List.of(
                dept(81L, "生产部", 700L),
                dept(82L, "质量部", 701L)
        ));

        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute result =
                resolver.resolveRoute(19L, 99L, "NEW");
        Map<String, List<Long>> startAssignees = resolver.buildStartUserSelectAssigneeMap(result.nodes());
        Map<String, List<Long>> approveAssignees = resolver.buildApproveUserSelectAssigneeMap(result.nodes());

        assertEquals(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), result.nodes().get(0).stageCode());
        assertEquals(Map.of(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), List.of(700L, 701L)),
                startAssignees);
        assertEquals(List.of(914521L), approveAssignees.get(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode()));
        assertEquals(List.of(914522L), approveAssignees.get(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode()));
    }

    @Test
    void resolveStartUserSelectAssignees_routeMissingFailsFast() {
        DccControlledFileDO file = DccControlledFileDO.builder().id(902L).categoryId(12L).build();
        when(routeMapper.selectLatestActiveByCategoryId(12L)).thenReturn(null);

        assertServiceException(() -> resolver.resolveStartUserSelectAssignees(file, 99L),
                CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
    }

    @Test
    void resolveStartUserSelectAssignees_emptyApproverFailsFast() {
        DccControlledFileDO file = DccControlledFileDO.builder().id(903L).categoryId(13L).build();
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(23L).categoryId(13L).versionNo(1).active(Boolean.TRUE).build();
        when(routeMapper.selectLatestActiveByCategoryId(13L)).thenReturn(route);
        when(routeNodeMapper.selectListByRouteId(23L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "POSITION", 303L,
                        null, 1),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "USER", 914520L,
                        null, 2),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "USER", 914521L,
                        null, 3),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "USER", 914522L,
                        null, 4)
        ));
        when(positionRuntimeResolver.isUploaderDerivedPosition(303L)).thenReturn(false);
        when(positionAssignmentMapper.selectActiveListByPositionId(303L)).thenReturn(List.of());

        assertServiceException(() -> resolver.resolveStartUserSelectAssignees(file, 99L),
                ROUTE_PREVIEW_APPROVER_NOT_FOUND);
    }

    @Test
    void resolveStartUserSelectAssignees_userWithoutPostFailsFast() {
        DccControlledFileDO file = DccControlledFileDO.builder().id(904L).categoryId(14L).build();
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(24L).categoryId(14L).versionNo(1).active(Boolean.TRUE).build();
        when(routeMapper.selectLatestActiveByCategoryId(14L)).thenReturn(route);
        when(routeNodeMapper.selectListByRouteId(24L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "USER", 914518L,
                        null, 1),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "USER", 914520L,
                        null, 2),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "USER", 914521L,
                        null, 3),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "USER", 914522L,
                        null, 4)
        ));
        org.mockito.Mockito.doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil
                        .exception(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants
                                .CONTROLLED_FILE_APPROVER_POST_REQUIRED))
                .when(approvalParticipantPostValidator).requireConfiguredPosts(List.of(914518L));

        assertServiceException(() -> resolver.resolveStartUserSelectAssignees(file, 99L),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_APPROVER_POST_REQUIRED);
    }

    @Test
    void resolveRouteForReadiness_unsupportedFixedApprovalPolicyFailsFast() {
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(25L).categoryId(15L).versionNo(1).active(Boolean.TRUE).build();
        DccCategoryApprovalRouteNodeDO unsupportedApprovalNode = routeNode(3,
                DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "USER", 914521L, null, 3);
        unsupportedApprovalNode.setApproveMethod("ALL");
        unsupportedApprovalNode.setApproveRatio(100);
        unsupportedApprovalNode.setRequireAllApprovals(Boolean.TRUE);
        when(routeMapper.selectLatestActiveByCategoryId(15L)).thenReturn(route);
        when(routeNodeMapper.selectListByRouteId(25L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "USER", 914518L,
                        null, 1),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "USER", 914520L,
                        null, 2),
                unsupportedApprovalNode,
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "USER", 914522L,
                        null, 4)
        ));

        assertServiceException(() -> resolver.resolveRouteForReadiness(15L, 99L),
                CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        verifyNoInteractions(positionAssignmentMapper, positionRuntimeResolver, adminUserApi);
    }

    @Test
    void buildApproveUserSelectAssigneeMap_duplicateStageCodeFailsFast() {
        List<DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode> nodes = List.of(
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(
                        1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "文控预审", 1,
                        "USER", 200L, List.of(200L), "ANY", null, Boolean.FALSE, List.of(200L)),
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(
                        2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "会签审核-第一组", 2,
                        "USER", 201L, List.of(201L), "ALL", 100, Boolean.TRUE, List.of(201L)),
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(
                        2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "会签审核-第二组", 2,
                        "USER", 202L, List.of(202L), "ALL", 100, Boolean.TRUE, List.of(202L)),
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(
                        3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "会签批准", 3,
                        "USER", 203L, List.of(203L), "ANY", null, Boolean.FALSE, List.of(203L))
        );

        assertServiceException(() -> resolver.buildApproveUserSelectAssigneeMap(nodes),
                CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
    }

    @Test void selectedDepartmentAdditionRemovalRetainsSeparateObligationsForOneLeaderAndLaterActionCandidates() {
        var original=selectedRoute("NEW");
        when(deptApi.getDeptList(List.of(51L,52L))).thenReturn(List.of(dept(51L,"生产",99L),dept(52L,"质量",99L)));
        var selected=resolver.withSelectedSignoffDepartments(original,List.of(52L,51L));
        assertEquals(List.of(51L,52L),selected.nodes().get(0).candidateSourceIds());
        assertEquals(List.of(99L,99L),selected.nodes().get(0).resolvedUserIds());
        assertEquals(Boolean.TRUE,selected.nodes().get(0).requireAllApprovals());
        assertSame(original.nodes().get(1),selected.nodes().get(1));assertSame(original.nodes().get(2),selected.nodes().get(2));
        assertEquals(List.of(40L),original.nodes().get(0).candidateSourceIds());
        verify(deptApi).validateDeptList(List.of(51L,52L));verify(adminUserApi).validateUserList(List.of(99L,99L));
    }
    @Test void selectedDepartmentsMustBeNonemptyPositiveAndUniqueBeforeAnyDirectoryLookup() {
        for(var selected:List.of(List.<Long>of(),List.of(51L,51L),List.of(0L),List.of(-1L)))
            assertServiceException(()->resolver.withSelectedSignoffDepartments(selectedRoute("REVISION"),selected),CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        verifyNoInteractions(deptApi,adminUserApi);
    }
    @Test void aSelectedDepartmentMissingItsLeaderIsNotFilledFromTheDefaultDepartment() {
        when(deptApi.getDeptList(List.of(51L))).thenReturn(List.of(dept(51L,"质量",null)));
        assertServiceException(()->resolver.withSelectedSignoffDepartments(selectedRoute("NEW"),List.of(51L)),ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        verify(adminUserApi,never()).validateUserList(any());
    }
    @Test void duplicateDirectoryIdentitiesCannotCreateAmbiguousSelectedDepartmentLeaders() {
        when(deptApi.getDeptList(List.of(51L))).thenReturn(List.of(dept(51L,"质量",99L),dept(51L,"重复",100L)));
        assertServiceException(()->resolver.withSelectedSignoffDepartments(selectedRoute("NEW"),List.of(51L)),ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        verify(adminUserApi,never()).validateUserList(any());
    }
    @Test void obsoleteDepartmentSelectionStillContainsOnlyItsTwoConfiguredStages() {
        var all=selectedRoute("OBSOLETE");var obsolete=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(all.route(),all.nodes().subList(0,2));
        when(deptApi.getDeptList(List.of(51L))).thenReturn(List.of(dept(51L,"质量",99L)));
        var resolved=resolver.withSelectedSignoffDepartments(obsolete,List.of(51L));
        assertEquals(List.of("MATRIX_REVIEW","MATRIX_APPROVAL"),resolved.nodes().stream().map(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode::stageCode).toList());
        assertSame(obsolete.nodes().get(1),resolved.nodes().get(1));
    }
    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute selectedRoute(String action) {
        return new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(DccCategoryApprovalRouteDO.builder().id(20L).actionType(action).build(),List.of(
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(1,"MATRIX_REVIEW","会签",1,"DEPT",40L,List.of(40L),"ALL",100,true,List.of(90L)),
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(2,"MATRIX_APPROVAL","批准",2,"USER",100L,List.of(100L),"ANY",null,false,List.of(100L)),
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(3,"DOC_CONTROL_REVIEW","文控审核",3,"USER",101L,List.of(101L),"ANY",null,false,List.of(101L))));
    }

    private DccCategoryApprovalRouteNodeDO routeNode(Integer stageNo, String stageCode, String candidateSourceType,
                                                    Long candidateSourceId, String candidateSourceIds, Integer sort) {
        boolean matrixReview = DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode().equals(stageCode);
        return DccCategoryApprovalRouteNodeDO.builder()
                .routeId(20L)
                .stageNo(stageNo)
                .stageCode(stageCode)
                .stageName(stageCode)
                .stageOrder(stageNo)
                .candidateSourceType(candidateSourceType)
                .candidateSourceId(candidateSourceId)
                .candidateSourceIds(candidateSourceIds)
                .approveMethod(matrixReview ? "ALL" : "ANY")
                .approveRatio(matrixReview ? 100 : null)
                .requireAllApprovals(matrixReview)
                .required(Boolean.TRUE)
                .sort(sort)
                .build();
    }

    private DeptRespDTO dept(Long id, String name, Long leaderUserId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setLeaderUserId(leaderUserId);
        return dept;
    }
}
