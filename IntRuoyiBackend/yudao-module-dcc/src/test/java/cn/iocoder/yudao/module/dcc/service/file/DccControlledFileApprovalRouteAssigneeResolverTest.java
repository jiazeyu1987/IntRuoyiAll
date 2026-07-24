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
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
                        null, 2)
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
                        null, 2)
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
                        null, 1)
        ));
        when(positionRuntimeResolver.isUploaderDerivedPosition(303L)).thenReturn(false);
        when(positionAssignmentMapper.selectActiveListByPositionId(303L)).thenReturn(List.of());

        assertServiceException(() -> resolver.resolveStartUserSelectAssignees(file, 99L),
                ROUTE_PREVIEW_APPROVER_NOT_FOUND);
    }

    private DccCategoryApprovalRouteNodeDO routeNode(Integer stageNo, String stageCode, String candidateSourceType,
                                                    Long candidateSourceId, String candidateSourceIds, Integer sort) {
        return DccCategoryApprovalRouteNodeDO.builder()
                .routeId(20L)
                .stageNo(stageNo)
                .stageCode(stageCode)
                .stageName(stageCode)
                .stageOrder(stageNo)
                .candidateSourceType(candidateSourceType)
                .candidateSourceId(candidateSourceId)
                .candidateSourceIds(candidateSourceIds)
                .approveMethod("ANY")
                .approveRatio(1)
                .requireAllApprovals(Boolean.FALSE)
                .sort(sort)
                .build();
    }
}
