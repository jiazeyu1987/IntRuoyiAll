package cn.iocoder.yudao.module.dcc.service.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteNodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccApprovalModeEnum;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@Import(DccApprovalRouteAdminServiceImpl.class)
class DccApprovalRouteAdminServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccApprovalRouteAdminServiceImpl routeAdminService;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccApprovalPositionMapper positionMapper;
    @Resource
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Resource
    private DccCategoryApprovalRouteMapper routeMapper;
    @Resource
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private DccApprovalPositionRuntimeResolver positionRuntimeResolver;

    @Test
    void testPreviewRoute_categoryMissing_throwsNotExists() {
        DccApprovalRoutePreviewReqVO reqVO = new DccApprovalRoutePreviewReqVO();
        reqVO.setCategoryId(randomLongId());

        assertServiceException(() -> routeAdminService.previewRoute(reqVO), FILE_CATEGORY_NOT_EXISTS);
    }

    @Test
    void testPreviewRoute_positionWithoutAssignments_throwsApproverMissing() {
        DccFileCategoryDO category = createCategory("SOP");
        DccApprovalPositionDO position = createPosition("QA_REVIEW", "QA审核");
        DccCategoryApprovalRouteDO route = createRoute(category.getId());

        routeNodeMapper.insert(createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                "POSITION", position.getId(), "ALL", false, 1));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "会签审核",
                "USER", 201L, "ALL", true, 2));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "会签批准",
                "USER", 202L, "ALL", true, 3));
        routeNodeMapper.insert(createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                "USER", 203L, "ANY", false, 4));

        DccApprovalRoutePreviewReqVO reqVO = new DccApprovalRoutePreviewReqVO();
        reqVO.setCategoryId(category.getId());

        assertServiceException(() -> routeAdminService.previewRoute(reqVO), ROUTE_PREVIEW_APPROVER_NOT_FOUND);
    }

    @Test
    void testPreviewRoute_userNode_success() {
        DccFileCategoryDO category = createCategory("FORM");
        DccCategoryApprovalRouteDO route = createRoute(category.getId());

        routeNodeMapper.insert(createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                "USER", 200L, "ANY", false, 1));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "会签审核",
                "USER", 201L, "ALL", true, 2));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "会签批准",
                "USER", 202L, "ALL", true, 3));
        routeNodeMapper.insert(createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                "USER", 203L, "ANY", false, 4));

        DccApprovalRoutePreviewReqVO reqVO = new DccApprovalRoutePreviewReqVO();
        reqVO.setCategoryId(category.getId());

        List<DccApprovalRoutePreviewRespVO> preview = routeAdminService.previewRoute(reqVO);

        assertEquals(4, preview.size());
        assertEquals(1, preview.get(0).getStageNo());
        assertEquals("DOC_CONTROL_REVIEW", preview.get(0).getStageCode());
        assertEquals(1, preview.get(0).getStageOrder());
        assertEquals(DccApprovalModeEnum.ANY_ONE.getMode(), preview.get(0).getApprovalMode());
        assertEquals("USER", preview.get(0).getCandidateSourceType());
        assertEquals(List.of(200L), preview.get(0).getCandidateSourceIds());
        assertEquals(List.of(200L), preview.get(0).getResolvedUserIds());
    }

    @Test
    void testPreviewRoute_userCandidateInvalid_throwsApproverMissing() {
        DccFileCategoryDO category = createCategory("FORM");
        DccCategoryApprovalRouteDO route = createRoute(category.getId());

        routeNodeMapper.insert(createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                "USER", 200L, "ANY", false, 1));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "会签审核",
                "USER", 201L, "ALL", true, 2));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "会签批准",
                "USER", 202L, "ALL", true, 3));
        routeNodeMapper.insert(createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                "USER", 203L, "ANY", false, 4));
        doThrow(new ServiceException(1_002_000_004, "user disabled"))
                .when(adminUserApi).validateUserList(List.of(200L));

        DccApprovalRoutePreviewReqVO reqVO = new DccApprovalRoutePreviewReqVO();
        reqVO.setCategoryId(category.getId());

        assertServiceException(() -> routeAdminService.previewRoute(reqVO), ROUTE_PREVIEW_APPROVER_NOT_FOUND);
    }

    @Test
    void testPreviewRoute_positionDirectUserInvalid_throwsApproverMissing() {
        DccFileCategoryDO category = createCategory("FORM");
        DccApprovalPositionDO position = createPosition("DOC_CONTROL", "文控");
        positionAssignmentMapper.insert(DccPositionAssignmentDO.builder()
                .id(randomLongId())
                .positionId(position.getId())
                .assignmentType("USER")
                .userId(200L)
                .active(Boolean.TRUE)
                .changeReason("seed")
                .build());
        DccCategoryApprovalRouteDO route = createRoute(category.getId());

        routeNodeMapper.insert(createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                "POSITION", position.getId(), "ANY", false, 1));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "会签审核",
                "USER", 201L, "ALL", true, 2));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "会签批准",
                "USER", 202L, "ALL", true, 3));
        routeNodeMapper.insert(createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                "USER", 203L, "ANY", false, 4));
        doThrow(new ServiceException(1_002_000_004, "user disabled"))
                .when(adminUserApi).validateUserList(List.of(200L));

        DccApprovalRoutePreviewReqVO reqVO = new DccApprovalRoutePreviewReqVO();
        reqVO.setCategoryId(category.getId());

        assertServiceException(() -> routeAdminService.previewRoute(reqVO), ROUTE_PREVIEW_APPROVER_NOT_FOUND);
    }

    @Test
    void testGetRoutePage_listsAllCategoriesAndFiltersByCategory() {
        DccFileCategoryDO sopCategory = createCategory("SOP");
        DccFileCategoryDO formCategory = createCategory("FORM");
        DccApprovalPositionDO docControlPosition = createPosition("DOC_CTRL", "文控岗位");
        DccCategoryApprovalRouteDO sopRoute = createRoute(sopCategory.getId());
        createRoute(formCategory.getId());
        routeNodeMapper.insert(createRouteNode(sopRoute.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                "POSITION", docControlPosition.getId(), "ANY", false, 1));

        DccApprovalRoutePageReqVO allReqVO = new DccApprovalRoutePageReqVO();
        allReqVO.setPageNo(1);
        allReqVO.setPageSize(10);

        PageResult<DccApprovalRouteRespVO> allPage = routeAdminService.getRoutePage(allReqVO);

        assertEquals(2L, allPage.getTotal());
        assertEquals(List.of(sopCategory.getId(), formCategory.getId()).stream().sorted().toList(),
                allPage.getList().stream().map(DccApprovalRouteRespVO::getCategoryId).sorted().toList());

        DccApprovalRoutePageReqVO filteredReqVO = new DccApprovalRoutePageReqVO();
        filteredReqVO.setPageNo(1);
        filteredReqVO.setPageSize(10);
        filteredReqVO.setCategoryId(sopCategory.getId());

        PageResult<DccApprovalRouteRespVO> filteredPage = routeAdminService.getRoutePage(filteredReqVO);

        assertEquals(1L, filteredPage.getTotal());
        assertEquals("SOP", filteredPage.getList().get(0).getCategoryName());
        assertEquals(1, filteredPage.getList().get(0).getNodes().size());
        assertEquals(1, filteredPage.getList().get(0).getNodeCount());
        assertEquals("启用", filteredPage.getList().get(0).getStatusLabel());
        assertEquals("1. 文控审核：文控岗位", filteredPage.getList().get(0).getNodeSummary());
    }

    @Test
    void testSaveRoute_missingFixedStage_throwsExplicitFailure() {
        DccFileCategoryDO category = createCategory("SOP");

        DccApprovalRouteSaveReqVO reqVO = new DccApprovalRouteSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.now());
        reqVO.setRemark("route");
        reqVO.setNodes(List.of(
                createNodeReq(1, "文控审核", "USER", 200L, "ANY", 1),
                createNodeReq(2, "会签审核", "USER", 201L, "ALL", 2),
                createNodeReq(4, "文控批准", "USER", 202L, "ANY", 4)
        ));

        assertServiceException(() -> routeAdminService.saveRoute(category.getId(), reqVO),
                DccApprovalRouteAdminServiceImpl.APPROVAL_ROUTE_FIXED_STAGE_INVALID);
    }

    @Test
    void testSaveRoute_persistsFixedStageMetadata() {
        DccFileCategoryDO category = createCategory("PROC");

        DccApprovalRouteSaveReqVO reqVO = new DccApprovalRouteSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.now());
        reqVO.setRemark("route");
        reqVO.setNodes(List.of(
                createNodeReq(1, "文控审核", "USER", 200L, "ANY", 1),
                createNodeReq(2, "会签审核", "POSITION", 300L, "ALL", 2),
                createNodeReq(3, "会签批准", "POSITION", 301L, "ALL", 3),
                createNodeReq(4, "文控批准", "USER", 202L, "ANY", 4)
        ));

        DccCategoryApprovalRouteDO route = routeAdminService.saveRoute(category.getId(), reqVO);

        List<DccCategoryApprovalRouteNodeDO> nodes = routeNodeMapper.selectListByRouteId(route.getId());
        assertEquals(List.of("DOC_CONTROL_REVIEW", "MATRIX_REVIEW", "MATRIX_APPROVAL", "DOC_CONTROL_APPROVAL"),
                nodes.stream().map(DccCategoryApprovalRouteNodeDO::getStageCode).toList());
        assertEquals(List.of(1, 2, 3, 4),
                nodes.stream().map(DccCategoryApprovalRouteNodeDO::getStageOrder).toList());
        assertEquals(List.of(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
                nodes.stream().map(DccCategoryApprovalRouteNodeDO::getRequireAllApprovals).toList());
    }

    @Test
    void testPreviewRoute_positionPostAssignment_success() {
        DccFileCategoryDO category = createCategory("FORM");
        DccApprovalPositionDO position = createPosition("MATRIX_REVIEW", "会签审核岗位");
        positionAssignmentMapper.insert(DccPositionAssignmentDO.builder()
                .id(randomLongId())
                .positionId(position.getId())
                .assignmentType("POST")
                .systemPostId(500L)
                .active(Boolean.TRUE)
                .changeReason("seed")
                .build());
        when(adminUserApi.getUserListByPostIds(List.of(500L)))
                .thenReturn(List.of(new AdminUserRespDTO().setId(200L), new AdminUserRespDTO().setId(201L)));

        DccCategoryApprovalRouteDO route = createRoute(category.getId());
        routeNodeMapper.insert(createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                "USER", 199L, "ANY", false, 1));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "会签审核",
                "POSITION", position.getId(), "ALL", true, 2));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "会签批准",
                "USER", 202L, "ALL", true, 3));
        routeNodeMapper.insert(createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                "USER", 203L, "ANY", false, 4));

        DccApprovalRoutePreviewReqVO reqVO = new DccApprovalRoutePreviewReqVO();
        reqVO.setCategoryId(category.getId());

        List<DccApprovalRoutePreviewRespVO> preview = routeAdminService.previewRoute(reqVO);

        assertEquals(4, preview.size());
        assertEquals("MATRIX_REVIEW", preview.get(1).getStageCode());
        assertEquals(2, preview.get(1).getStageOrder());
        assertEquals(Boolean.TRUE, preview.get(1).getRequireAllApprovals());
        assertEquals(List.of(200L, 201L), preview.get(1).getResolvedUserIds());
    }

    @Test
    void testPreviewRoute_uploaderDerivedPositionWithoutSubmitterContext_returnsEmptyResolvedUsers() {
        DccFileCategoryDO category = createCategory("SPEC");
        DccApprovalPositionDO position = createPosition("INTAUTH-1", "编制人直接主管");
        when(positionRuntimeResolver.isUploaderDerivedPosition(position.getId())).thenReturn(Boolean.TRUE);
        when(positionRuntimeResolver.resolveUserIds(position.getId(), null, true)).thenReturn(List.of());

        DccCategoryApprovalRouteDO route = createRoute(category.getId());
        routeNodeMapper.insert(createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                "POSITION", position.getId(), "ANY", false, 1));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "会签审核",
                "USER", 201L, "ALL", true, 2));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "会签批准",
                "USER", 202L, "ALL", true, 3));
        routeNodeMapper.insert(createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                "USER", 203L, "ANY", false, 4));

        DccApprovalRoutePreviewReqVO reqVO = new DccApprovalRoutePreviewReqVO();
        reqVO.setCategoryId(category.getId());

        List<DccApprovalRoutePreviewRespVO> preview = routeAdminService.previewRoute(reqVO);

        assertEquals(4, preview.size());
        assertEquals(List.of(), preview.get(0).getResolvedUserIds());
    }

    @Test
    void testDeleteRoute_deletesCurrentRouteVersionAndNodes() {
        DccFileCategoryDO category = createCategory("DEL");
        DccCategoryApprovalRouteDO route = createRoute(category.getId());
        DccCategoryApprovalRouteNodeDO node = createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                "USER", 200L, "ANY", false, 1);
        routeNodeMapper.insert(node);

        routeAdminService.deleteRoute(route.getId());

        assertNull(routeMapper.selectById(route.getId()));
        assertNull(routeNodeMapper.selectById(node.getId()));
        assertEquals(0L, routeMapper.selectCount(DccCategoryApprovalRouteDO::getCategoryId, category.getId()));
        assertEquals(0, routeNodeMapper.selectListByRouteId(route.getId()).size());
    }

    @Test
    void testDeleteRoute_missingRoute_throwsNotExists() {
        assertServiceException(() -> routeAdminService.deleteRoute(randomLongId()), APPROVAL_ROUTE_NOT_EXISTS);
    }

    @Test
    void testSaveRoute_afterDeletingRouteVersion_usesNextHistoricalVersionNumber() {
        DccFileCategoryDO category = createCategory("RESAVE");
        DccCategoryApprovalRouteDO route = createRoute(category.getId());
        DccApprovalRouteSaveReqVO reqVO = new DccApprovalRouteSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.now());
        reqVO.setRemark("resave after delete");
        reqVO.setNodes(List.of(
                createNodeReq(1, "文控审核", "USER", 200L, "ANY", 1),
                createNodeReq(2, "会签审核", "USER", 201L, "ALL", 2),
                createNodeReq(3, "会签批准", "USER", 202L, "ALL", 3),
                createNodeReq(4, "文控批准", "USER", 203L, "ANY", 4)));

        routeAdminService.deleteRoute(route.getId());
        DccCategoryApprovalRouteDO savedRoute = routeAdminService.saveRoute(category.getId(), reqVO);

        assertNotNull(savedRoute.getId());
        assertEquals(2, savedRoute.getVersionNo());
        assertEquals(4, routeNodeMapper.selectListByRouteId(savedRoute.getId()).size());
    }

    private DccFileCategoryDO createCategory(String code) {
        DccFileCategoryDO category = DccFileCategoryDO.builder()
                .id(randomLongId())
                .code(code)
                .name(code)
                .parentId(null)
                .active(Boolean.TRUE)
                .sort(1)
                .source("LOCAL")
                .remark("category")
                .lifecycleStage("PLAN")
                .build();
        categoryMapper.insert(category);
        return category;
    }

    private DccApprovalPositionDO createPosition(String code, String name) {
        DccApprovalPositionDO position = DccApprovalPositionDO.builder()
                .id(randomLongId())
                .code(code)
                .name(name)
                .active(Boolean.TRUE)
                .source("LOCAL")
                .remark("position")
                .build();
        positionMapper.insert(position);
        return position;
    }

    private DccCategoryApprovalRouteDO createRoute(Long categoryId) {
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(randomLongId())
                .categoryId(categoryId)
                .versionNo(1)
                .active(Boolean.TRUE)
                .effectiveTime(LocalDateTime.now())
                .remark("route")
                .build();
        routeMapper.insert(route);
        return route;
    }

    private DccApprovalRouteNodeSaveReqVO createNodeReq(int stageNo, String stageName, String sourceType,
                                                        Long sourceId, String approveMethod, int sort) {
        DccApprovalRouteNodeSaveReqVO reqVO = new DccApprovalRouteNodeSaveReqVO();
        reqVO.setStageNo(stageNo);
        reqVO.setStageName(stageName);
        reqVO.setCandidateSourceType(sourceType);
        reqVO.setCandidateSourceId(sourceId);
        reqVO.setApproveMethod(approveMethod);
        reqVO.setApproveRatio("ALL".equals(approveMethod) ? 100 : null);
        reqVO.setRequired(Boolean.TRUE);
        reqVO.setSort(sort);
        return reqVO;
    }

    private DccCategoryApprovalRouteNodeDO createRouteNode(Long routeId, int stageNo, String stageCode, String stageName,
                                                           String sourceType, Long sourceId, String approveMethod,
                                                           boolean requireAllApprovals, int sort) {
        return DccCategoryApprovalRouteNodeDO.builder()
                .id(randomLongId())
                .routeId(routeId)
                .stageNo(stageNo)
                .stageCode(stageCode)
                .stageName(stageName)
                .stageOrder(stageNo)
                .candidateSourceType(sourceType)
                .candidateSourceId(sourceId)
                .candidateSourceIds(String.valueOf(sourceId))
                .approveMethod(approveMethod)
                .approveRatio("ALL".equals(approveMethod) ? 100 : null)
                .requireAllApprovals(requireAllApprovals)
                .required(Boolean.TRUE)
                .sort(sort)
                .build();
    }
}
