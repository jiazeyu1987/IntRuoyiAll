package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixEffectivePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixUserLookupRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileCategoryPermissionSupport;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileReviewMatrixAccessService;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        DccCategoryApprovalMatrixAdminServiceImpl.class,
        DccControlledFileReviewMatrixAccessService.class,
        DccControlledFileCategoryPermissionSupport.class
})
class DccCategoryApprovalMatrixAdminServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccCategoryApprovalMatrixAdminServiceImpl matrixAdminService;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccApprovalPositionMapper positionMapper;
    @Resource
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private DccCategoryApprovalRouteMapper routeMapper;
    @Resource
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private DccApprovalPositionRuntimeResolver positionRuntimeResolver;
    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private RoleApi roleApi;
    @MockitoBean
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;

    @Test
    void getApprovalMatrix_returnsRuleRowsInsteadOfLegacyPositionArrays() {
        DccFileCategoryDO category = createCategory("INTAUTH-RULE-1", "规则行受控文件");
        DccApprovalPositionDO docControl = createPosition("POS-DOC-RULE", "文控");
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .versionNo(2)
                .active(Boolean.TRUE)
                .effectiveTime(LocalDateTime.of(2026, 6, 25, 15, 0))
                .remark("rule-editor")
                .build();
        routeMapper.insert(route);
        routeNodeMapper.insert(createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                docControl.getId(), List.of(docControl.getId()), "ANY", false));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "审核",
                2001L, List.of(2001L), "ALL", true));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "批准",
                3001L, List.of(3001L), "ANY", false));
        routeNodeMapper.insert(createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                docControl.getId(), List.of(docControl.getId()), "ANY", false));

        DccCategoryApprovalMatrixRespVO respVO = matrixAdminService.getApprovalMatrix(category.getId());

        assertNotNull(respVO.getRules(), "审阅矩阵读取必须返回规则行集合");
        assertEquals(2, respVO.getRules().size(), "审阅矩阵只应暴露审核/批准两类可编辑规则");
        assertTrue(respVO.getRules().stream().anyMatch(rule -> "SIGNOFF".equals(rule.getStageType())));
        assertTrue(respVO.getRules().stream().anyMatch(rule -> "APPROVAL".equals(rule.getStageType())));
    }

    @Test
    void saveApprovalMatrix_persistsRuleMetadataIntoRouteNodes() {
        DccFileCategoryDO category = createCategory("INTAUTH-RULE-2", "规则元数据文件");
        DccApprovalPositionDO docControl = createPosition("POS-DOC-RULE-2", "文控");
        positionAssignmentMapper.insert(createUserAssignment(docControl.getId(), 9001L));
        stubNonUploaderPositions();
        stubUserNames(9001L, 9002L, 9003L, 9004L);
        when(deptApi.getDeptList(List.of(122L))).thenReturn(List.of(
                new DeptRespDTO().setId(122L).setName("质量体系中心").setLeaderUserId(9002L)));
        when(adminUserApi.getUser(9002L)).thenReturn(new AdminUserRespDTO().setId(9002L).setNickname("QMS负责人"));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(233L))).thenReturn(Set.of(9003L, 9004L));
        when(adminUserApi.getUserList(anyList())).thenReturn(List.of(
                new AdminUserRespDTO().setId(9001L).setNickname("文控"),
                new AdminUserRespDTO().setId(9002L).setNickname("QMS负责人"),
                new AdminUserRespDTO().setId(9003L).setNickname("角色成员A"),
                new AdminUserRespDTO().setId(9004L).setNickname("角色成员B")));

        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 6, 25, 15, 30));
        reqVO.setRemark("rule-editor-save");
        reqVO.setRules(List.of(
                new DccCategoryApprovalMatrixSaveReqVO.Rule()
                        .setStageType("SIGNOFF")
                        .setActive(true)
                        .setSubjectLabel("QMS")
                        .setMarker("▲")
                        .setSubjectType("DEPT")
                        .setSubjectId(122L)
                        .setSubjectDepartmentPath("瑛泰医疗-质量体系中心-QMS")
                        .setRemark("审核部门"),
                new DccCategoryApprovalMatrixSaveReqVO.Rule()
                        .setStageType("APPROVAL")
                        .setActive(true)
                        .setSubjectLabel("技术委员会")
                        .setMarker("▲")
                        .setSubjectType("ROLE")
                        .setSubjectId(233L)
                        .setSubjectName("技术委员会")
                        .setRemark("批准部门")));

        DccCategoryApprovalRouteDO route = matrixAdminService.saveApprovalMatrix(category.getId(), reqVO);

        List<DccCategoryApprovalRouteNodeDO> nodes = routeNodeMapper.selectListByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageOrder))
                .toList();
        assertEquals("DEPT", nodes.get(1).getCandidateSourceType());
        assertEquals("122", nodes.get(1).getCandidateSourceIds());
        assertEquals("ROLE", nodes.get(2).getCandidateSourceType());
        assertEquals("233", nodes.get(2).getCandidateSourceIds());
        assertEquals("▲", nodes.get(1).getMarker(), "审核节点必须统一持久化 ▲ 标记");
        assertEquals("▲", nodes.get(2).getMarker(), "批准节点必须统一持久化 ▲ 标记");
        assertTrue(nodes.get(1).getRuleRemark() == null || nodes.get(1).getRuleRemark().isEmpty(),
                "审核节点不应继续持久化行备注");
        assertTrue(nodes.get(2).getRuleRemark() == null || nodes.get(2).getRuleRemark().isEmpty(),
                "批准节点不应继续持久化行备注");
        assertNotNull(nodes.get(2).getSubjectLabel(), "批准节点必须持久化主体标签元数据");
        verify(roleApi).validRoleList(List.of(233L));
    }

    @Test
    void saveApprovalMatrix_persistsDerivedFixedFourStages() {
        DccFileCategoryDO category = createCategory("INTAUTH-1", "产品技术要求");
        DccApprovalPositionDO docControl = createPosition("POS-DOC", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA", "QA");
        DccApprovalPositionDO qms = createPosition("POS-QMS", "QMS");
        DccApprovalPositionDO deptOwner = createPosition("POS-DEPT-OWNER", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-AUTH-REP", "授权代表");
        positionAssignmentMapper.insert(createUserAssignment(docControl.getId(), 201L));
        positionAssignmentMapper.insert(createUserAssignment(qa.getId(), 202L));
        positionAssignmentMapper.insert(createUserAssignment(qms.getId(), 203L));
        positionAssignmentMapper.insert(createUserAssignment(deptOwner.getId(), 204L));
        positionAssignmentMapper.insert(createUserAssignment(delegate.getId(), 205L));
        stubNonUploaderPositions();
        stubUserNames(201L, 202L, 203L, 204L, 205L);

        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 5, 15, 20, 0));
        reqVO.setRemark("seed");
        reqVO.setRules(List.of(
                dccPositionRule("SIGNOFF", "QA", qa.getId(), "QA"),
                dccPositionRule("SIGNOFF", "QMS", qms.getId(), "QMS"),
                dccPositionRule("APPROVAL", "编制部门负责人", deptOwner.getId(), "编制部门负责人"),
                dccPositionRule("APPROVAL", "授权代表", delegate.getId(), "授权代表")));

        DccCategoryApprovalRouteDO route = matrixAdminService.saveApprovalMatrix(category.getId(), reqVO);

        List<DccCategoryApprovalRouteNodeDO> nodes = routeNodeMapper.selectListByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageOrder))
                .toList();
        assertEquals(6, nodes.size());
        assertIterableEquals(List.of("DOC_CONTROL_REVIEW", "MATRIX_REVIEW", "MATRIX_REVIEW", "MATRIX_APPROVAL",
                        "MATRIX_APPROVAL", "DOC_CONTROL_APPROVAL"),
                nodes.stream().map(DccCategoryApprovalRouteNodeDO::getStageCode).toList());
        assertIterableEquals(List.of("ANY", "ALL", "ALL", "ANY", "ANY", "ANY"),
                nodes.stream().map(DccCategoryApprovalRouteNodeDO::getApproveMethod).toList());
        assertIterableEquals(List.of(Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE,
                        Boolean.FALSE),
                nodes.stream().map(DccCategoryApprovalRouteNodeDO::getRequireAllApprovals).toList());
        assertEquals(String.valueOf(docControl.getId()), nodes.get(0).getCandidateSourceIds());
        assertEquals(String.valueOf(qa.getId()), nodes.get(1).getCandidateSourceIds());
        assertEquals(String.valueOf(qms.getId()), nodes.get(2).getCandidateSourceIds());
        assertEquals(String.valueOf(deptOwner.getId()), nodes.get(3).getCandidateSourceIds());
        assertEquals(String.valueOf(delegate.getId()), nodes.get(4).getCandidateSourceIds());
        assertEquals(String.valueOf(docControl.getId()), nodes.get(5).getCandidateSourceIds());
    }

    @Test
    void saveApprovalMatrix_missingSignoffPositions_failFast() {
        DccFileCategoryDO category = createCategory("INTAUTH-2", "生产用设备清单");
        createPosition("POS-DOC", "文控");
        DccApprovalPositionDO deptOwner = createPosition("POS-DEPT-OWNER", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-AUTH-REP", "授权代表");

        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 5, 15, 20, 0));
        reqVO.setRules(List.of(
                dccPositionRule("APPROVAL", "编制部门负责人", deptOwner.getId(), "编制部门负责人"),
                dccPositionRule("APPROVAL", "授权代表", delegate.getId(), "授权代表")));

        assertServiceException(() -> matrixAdminService.saveApprovalMatrix(category.getId(), reqVO),
                DccCategoryApprovalMatrixAdminServiceImpl.CATEGORY_APPROVAL_MATRIX_SIGNOFF_EMPTY);
    }

    @Test
    void saveApprovalMatrix_approvalStageMustContainExactlyTwoPositions() {
        DccFileCategoryDO category = createCategory("INTAUTH-3", "检验用设备清单");
        createPosition("POS-DOC", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA", "QA");
        DccApprovalPositionDO deptOwner = createPosition("POS-DEPT-OWNER", "编制部门负责人");

        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 5, 15, 20, 0));
        reqVO.setRules(List.of(
                dccPositionRule("SIGNOFF", "QA", qa.getId(), "QA")));

        assertServiceException(() -> matrixAdminService.saveApprovalMatrix(category.getId(), reqVO),
                DccCategoryApprovalMatrixAdminServiceImpl.CATEGORY_APPROVAL_MATRIX_APPROVAL_COUNT_INVALID);
    }

    @Test
    void getApprovalMatrix_readsLatestDerivedSelections() {
        DccFileCategoryDO category = createCategory("INTAUTH-4", "说明书");
        DccApprovalPositionDO docControl = createPosition("POS-DOC", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA", "QA");
        DccApprovalPositionDO qms = createPosition("POS-QMS", "QMS");
        DccApprovalPositionDO deptOwner = createPosition("POS-DEPT-OWNER", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-AUTH-REP", "授权代表");
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .versionNo(3)
                .active(Boolean.TRUE)
                .effectiveTime(LocalDateTime.of(2026, 5, 16, 8, 0))
                .remark("seed")
                .build();
        routeMapper.insert(route);
        routeNodeMapper.insert(createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核", docControl.getId(), List.of(docControl.getId()), "ANY", false));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "审核会签", qa.getId(), List.of(qa.getId()), "ALL", true));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "审核会签", qms.getId(), List.of(qms.getId()), "ALL", true));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "批准", deptOwner.getId(), List.of(deptOwner.getId()), "ANY", false));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "批准", delegate.getId(), List.of(delegate.getId()), "ANY", false));
        routeNodeMapper.insert(createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准", docControl.getId(), List.of(docControl.getId()), "ANY", false));

        DccCategoryApprovalMatrixRespVO respVO = matrixAdminService.getApprovalMatrix(category.getId());

        assertEquals("2026-05-16T08:00", respVO.getEffectiveTime().toString());
        assertEquals(2, respVO.getRules().stream().filter(rule -> "SIGNOFF".equals(rule.getStageType())).count());
        assertEquals(2, respVO.getRules().stream().filter(rule -> "APPROVAL".equals(rule.getStageType())).count());
    }

    @Test
    void getActiveMatrixPositionIdsByCategoryIds_readsLatestActiveRoutePositionNodes() {
        DccFileCategoryDO category = createCategory("INTAUTH-26", "技术调研报告");
        DccApprovalPositionDO docControl = createPosition("POS-DOC-PROJECTION", "文控");
        DccApprovalPositionDO writerLeader = createPosition("POS-WRITER-LEADER", "编制人直接主管");
        DccApprovalPositionDO qms = createPosition("POS-QMS-PROJECTION", "QMS");
        DccApprovalPositionDO documentAdmin = createPosition("POS-DOC-ADMIN", "文档管理员");
        DccApprovalPositionDO deptOwner = createPosition("POS-DEPT-OWNER-PROJECTION", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-AUTH-REP-PROJECTION", "授权代表");
        DccCategoryApprovalRouteDO oldRoute = DccCategoryApprovalRouteDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .versionNo(1)
                .active(Boolean.TRUE)
                .effectiveTime(LocalDateTime.of(2026, 5, 1, 8, 0))
                .remark("old")
                .build();
        routeMapper.insert(oldRoute);
        routeNodeMapper.insert(createRouteNode(oldRoute.getId(), 2, "MATRIX_REVIEW", "审核会签",
                writerLeader.getId(), List.of(writerLeader.getId()), "ALL", true));
        DccCategoryApprovalRouteDO latestRoute = DccCategoryApprovalRouteDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .versionNo(2)
                .active(Boolean.TRUE)
                .effectiveTime(LocalDateTime.of(2026, 5, 2, 8, 0))
                .remark("latest")
                .build();
        routeMapper.insert(latestRoute);
        routeNodeMapper.insert(createRouteNode(latestRoute.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                docControl.getId(), List.of(docControl.getId()), "ANY", false));
        insertRouteNodeWithSort(latestRoute.getId(), 2, "MATRIX_REVIEW", "审核会签",
                writerLeader.getId(), List.of(writerLeader.getId()), "ALL", true, 1);
        insertRouteNodeWithSort(latestRoute.getId(), 2, "MATRIX_REVIEW", "审核会签",
                qms.getId(), List.of(qms.getId()), "ALL", true, 2);
        insertRouteNodeWithSort(latestRoute.getId(), 2, "MATRIX_REVIEW", "审核会签",
                documentAdmin.getId(), List.of(documentAdmin.getId()), "ALL", true, 3);
        insertRouteNodeWithSort(latestRoute.getId(), 3, "MATRIX_APPROVAL", "批准",
                deptOwner.getId(), List.of(deptOwner.getId()), "ANY", false, 1);
        insertRouteNodeWithSort(latestRoute.getId(), 3, "MATRIX_APPROVAL", "批准",
                delegate.getId(), List.of(delegate.getId()), "ANY", false, 2);
        routeNodeMapper.insert(createRouteNode(latestRoute.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                docControl.getId(), List.of(docControl.getId()), "ANY", false));

        var result = matrixAdminService.getActiveMatrixPositionIdsByCategoryIds(
                List.of(category.getId(), randomLongId()));

        assertEquals(List.of(writerLeader.getId(), qms.getId(), documentAdmin.getId()),
                result.get(category.getId()).signoffPositionIds());
        assertEquals(List.of(deptOwner.getId(), delegate.getId()),
                result.get(category.getId()).approvalPositionIds());
        assertEquals(1, result.size());
    }

    @Test
    void getApprovalMatrix_normalizesLegacyCandidateSourceIdsIntoRuleRows() {
        DccFileCategoryDO category = createCategory("INTAUTH-4B", "旧矩阵值回填");
        DccApprovalPositionDO docControl = createPosition("POS-DOC-LEGACY", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA-LEGACY", "QA");
        DccApprovalPositionDO qms = createPosition("POS-QMS-LEGACY", "QMS");
        DccApprovalPositionDO owner = createPosition("POS-OWNER-LEGACY", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-DELEGATE-LEGACY", "授权代表");
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .versionNo(1)
                .active(Boolean.TRUE)
                .effectiveTime(LocalDateTime.of(2026, 5, 16, 8, 0))
                .remark("legacy")
                .build();
        routeMapper.insert(route);
        DccCategoryApprovalRouteNodeDO docReviewNode = createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                docControl.getId(), List.of(docControl.getId()), "ANY", false);
        DccCategoryApprovalRouteNodeDO signoffNode = createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "审核会签",
                qa.getId(), List.of(qa.getId(), qms.getId()), "ALL", true);
        DccCategoryApprovalRouteNodeDO approvalNode = createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "批准",
                owner.getId(), List.of(owner.getId(), delegate.getId()), "ANY", false);
        DccCategoryApprovalRouteNodeDO docApprovalNode = createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                docControl.getId(), List.of(docControl.getId()), "ANY", false);
        routeNodeMapper.insert(docReviewNode);
        routeNodeMapper.insert(signoffNode);
        routeNodeMapper.insert(approvalNode);
        routeNodeMapper.insert(docApprovalNode);
        routeNodeMapper.updateById(DccCategoryApprovalRouteNodeDO.builder()
                .id(signoffNode.getId())
                .marker("●")
                .ruleRemark("历史备注")
                .build());
        routeNodeMapper.updateById(DccCategoryApprovalRouteNodeDO.builder()
                .id(approvalNode.getId())
                .marker("●")
                .ruleRemark("批准历史备注")
                .build());

        DccCategoryApprovalMatrixRespVO respVO = matrixAdminService.getApprovalMatrix(category.getId());

        assertEquals(List.of("QA", "QMS"), respVO.getRules().stream()
                .filter(rule -> "SIGNOFF".equals(rule.getStageType()))
                .map(DccCategoryApprovalMatrixRespVO.Rule::getSubjectName)
                .toList());
        assertEquals(List.of("编制部门负责人", "授权代表"), respVO.getRules().stream()
                .filter(rule -> "APPROVAL".equals(rule.getStageType()))
                .map(DccCategoryApprovalMatrixRespVO.Rule::getSubjectName)
                .toList());
        assertTrue(respVO.getRules().stream().allMatch(rule -> "DCC_POSITION".equals(rule.getSubjectType())));
        assertTrue(respVO.getRules().stream().allMatch(rule -> "▲".equals(rule.getMarker())),
                "旧 marker=● 读取时必须统一回显为 ▲");
        assertTrue(respVO.getRules().stream().allMatch(rule -> rule.getRemark() == null || rule.getRemark().isEmpty()),
                "旧行备注读取时必须清空，不再回显到规则行");
    }

    @Test
    void listReviewMatrixRows_returnsConfiguredAndUnconfiguredCategories() {
        DccFileCategoryDO configured = createCategory("INTAUTH-5", "作业指导书");
        DccFileCategoryDO unconfigured = createCategory("INTAUTH-6", "检验记录");
        DccApprovalPositionDO docControl = createPosition("POS-DOC", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA", "QA");
        DccApprovalPositionDO qms = createPosition("POS-QMS", "QMS");
        DccApprovalPositionDO owner = createPosition("POS-OWNER", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-DELEGATE", "授权代表");
        positionAssignmentMapper.insert(createUserAssignment(docControl.getId(), 201L));
        positionAssignmentMapper.insert(createUserAssignment(qa.getId(), 202L));
        positionAssignmentMapper.insert(createUserAssignment(qms.getId(), 203L));
        positionAssignmentMapper.insert(createUserAssignment(owner.getId(), 204L));
        positionAssignmentMapper.insert(createUserAssignment(delegate.getId(), 205L));
        stubNonUploaderPositions();
        stubUserNames(201L, 202L, 203L, 204L, 205L);
        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 6, 22, 9, 0));
        reqVO.setRemark("matrix");
        reqVO.setRules(List.of(
                dccPositionRule("SIGNOFF", "QA", qa.getId(), "QA"),
                dccPositionRule("SIGNOFF", "QMS", qms.getId(), "QMS"),
                dccPositionRule("APPROVAL", "编制部门负责人", owner.getId(), "编制部门负责人"),
                dccPositionRule("APPROVAL", "授权代表", delegate.getId(), "授权代表")));
        matrixAdminService.saveApprovalMatrix(configured.getId(), reqVO);

        List<DccCategoryReviewMatrixRowRespVO> rows = matrixAdminService.getReviewMatrixRows("INTAUTH", null, null,
                null);

        assertEquals(2, rows.size());
        DccCategoryReviewMatrixRowRespVO configuredRow = rows.stream()
                .filter(item -> configured.getId().equals(item.getCategoryId()))
                .findFirst()
                .orElseThrow();
        DccCategoryReviewMatrixRowRespVO unconfiguredRow = rows.stream()
                .filter(item -> unconfigured.getId().equals(item.getCategoryId()))
                .findFirst()
                .orElseThrow();
        assertTrue(Boolean.TRUE.equals(configuredRow.getConfigured()));
        assertEquals(configured.getLifecycleStage(), configuredRow.getLifecycleStage());
        assertEquals(2, configuredRow.getRules().stream().filter(rule -> "SIGNOFF".equals(rule.getStageType())).count());
        assertEquals(2, configuredRow.getRules().stream().filter(rule -> "APPROVAL".equals(rule.getStageType())).count());
        assertEquals(1, configuredRow.getRouteVersionNo());
        assertFalse(Boolean.TRUE.equals(unconfiguredRow.getConfigured()));
        assertEquals(unconfigured.getLifecycleStage(), unconfiguredRow.getLifecycleStage());
        assertEquals(List.of(), unconfiguredRow.getRules());
    }

    @Test
    void listReviewMatrixRows_returnsViewSubjectsDownloadSummaryAndRisks() {
        DccFileCategoryDO category = createCategory("INTAUTH-8", "质量手册");
        DccApprovalPositionDO docControl = createPosition("POS-DOC", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA", "QA");
        DccApprovalPositionDO qms = createPosition("POS-QMS", "QMS");
        DccApprovalPositionDO owner = createPosition("POS-OWNER", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-DELEGATE", "授权代表");
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .versionNo(1)
                .active(Boolean.TRUE)
                .effectiveTime(LocalDateTime.of(2026, 6, 22, 11, 0))
                .remark("matrix")
                .build();
        routeMapper.insert(route);
        routeNodeMapper.insert(createRouteNode(route.getId(), 1, "DOC_CONTROL_REVIEW", "文控审核",
                docControl.getId(), List.of(docControl.getId()), "ANY", false));
        routeNodeMapper.insert(createRouteNode(route.getId(), 2, "MATRIX_REVIEW", "审核会签",
                qa.getId(), List.of(qa.getId(), qms.getId()), "ALL", true));
        routeNodeMapper.insert(createRouteNode(route.getId(), 3, "MATRIX_APPROVAL", "批准",
                owner.getId(), List.of(owner.getId(), delegate.getId()), "ANY", false));
        routeNodeMapper.insert(createRouteNode(route.getId(), 4, "DOC_CONTROL_APPROVAL", "文控批准",
                docControl.getId(), List.of(docControl.getId()), "ANY", false));
        positionAssignmentMapper.insert(DccPositionAssignmentDO.builder()
                .id(randomLongId())
                .positionId(docControl.getId())
                .assignmentType("USER")
                .userId(201L)
                .active(Boolean.TRUE)
                .build());
        positionAssignmentMapper.insert(DccPositionAssignmentDO.builder()
                .id(randomLongId())
                .positionId(qa.getId())
                .assignmentType("USER")
                .userId(202L)
                .active(Boolean.TRUE)
                .build());
        positionAssignmentMapper.insert(DccPositionAssignmentDO.builder()
                .id(randomLongId())
                .positionId(owner.getId())
                .assignmentType("USER")
                .userId(203L)
                .active(Boolean.TRUE)
                .build());
        when(positionRuntimeResolver.isUploaderDerivedPosition(anyLong())).thenReturn(false);
        when(adminUserApi.getUserList(anyList())).thenReturn(List.of(
                new AdminUserRespDTO().setId(201L).setNickname("文控A"),
                new AdminUserRespDTO().setId(202L).setNickname("会签A"),
                new AdminUserRespDTO().setId(203L).setNickname("批准A")));
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .actionType("DOWNLOAD")
                .subjectType("USER")
                .subjectId(203L)
                .active(Boolean.TRUE)
                .remark("download")
                .build());

        DccCategoryReviewMatrixRowRespVO row = matrixAdminService.getReviewMatrixRows("INTAUTH-8", null, null,
                        null)
                .get(0);

        assertEquals(List.of("文控A", "会签A", "批准A"), row.getViewSubjects().stream()
                .map(DccCategoryReviewMatrixRowRespVO.Subject::getUserName)
                .toList());
        assertEquals("当前审阅矩阵参与人可浏览、查看详情和预览已发布受控副本", row.getViewRuleSummary());
        assertEquals("进行中文件待审原件预览继续按提交时 route snapshot 参与人放行", row.getPendingPreviewRuleSummary());
        assertEquals(List.of("USER#203"), row.getDownloadRuleSubjects());
        assertTrue(row.getRisks().stream().anyMatch(risk -> "POSITION_EMPTY".equals(risk.getCode())));
    }

    @Test
    void previewApprovalMatrix_returnsEffectiveUsersStagePreviewAndExplicitRisks() {
        DccFileCategoryDO category = createCategory("INTAUTH-9", "程序文件");
        DccApprovalPositionDO docControl = createPosition("POS-DOC", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA", "QA");
        DccApprovalPositionDO qms = createPosition("POS-QMS", "QMS");
        DccApprovalPositionDO owner = createPosition("POS-OWNER", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-DELEGATE", "授权代表");
        positionAssignmentMapper.insert(createUserAssignment(docControl.getId(), 201L));
        positionAssignmentMapper.insert(createUserAssignment(qa.getId(), 202L));
        positionAssignmentMapper.insert(createUserAssignment(owner.getId(), 203L));
        controlledFileMapper.insert(createPendingFile(category.getId(), 301L));
        routeSnapshotMapper.insert(DccControlledFileRouteSnapshotDO.builder()
                .id(randomLongId())
                .controlledFileId(301L)
                .routeVersionNo(1)
                .stageNo(2)
                .stageCode("MATRIX_REVIEW")
                .stageName("审核会签")
                .stageOrder(2)
                .candidateSourceType("POSITION")
                .candidateSourceId(qa.getId())
                .candidateSourceIds(String.valueOf(qa.getId()))
                .resolvedUserIds("999")
                .approveMethod("ALL")
                .requireAllApprovals(Boolean.TRUE)
                .build());
        when(positionRuntimeResolver.isUploaderDerivedPosition(anyLong())).thenReturn(false);
        when(adminUserApi.getUserList(anyList())).thenReturn(List.of(
                new AdminUserRespDTO().setId(201L).setNickname("文控A"),
                new AdminUserRespDTO().setId(202L).setNickname("会签A"),
                new AdminUserRespDTO().setId(203L).setNickname("批准A")));

        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 6, 23, 9, 0));
        reqVO.setRemark("preview");
        reqVO.setRules(List.of(
                dccPositionRule("SIGNOFF", "QA", qa.getId(), "QA"),
                dccPositionRule("SIGNOFF", "QMS", qms.getId(), "QMS"),
                dccPositionRule("APPROVAL", "编制部门负责人", owner.getId(), "编制部门负责人"),
                dccPositionRule("APPROVAL", "授权代表", delegate.getId(), "授权代表")));

        DccCategoryReviewMatrixEffectivePreviewRespVO preview = matrixAdminService
                .previewApprovalMatrix(category.getId(), reqVO);

        assertEquals(List.of(1, 2, 3, 4), preview.getStages().stream()
                .map(DccCategoryReviewMatrixEffectivePreviewRespVO.StagePreview::getStageNo)
                .toList());
        assertEquals(List.of("文控A", "会签A", "批准A"), preview.getViewSubjects().stream()
                .map(DccCategoryReviewMatrixEffectivePreviewRespVO.Subject::getUserName)
                .toList());
        assertTrue(preview.getStages().stream().anyMatch(stage -> "按 DCC 岗位解析".equals(stage.getSourceRule())));
        assertTrue(preview.getRisks().stream().anyMatch(risk ->
                "POSITION_EMPTY".equals(risk.getCode()) && Boolean.TRUE.equals(risk.getBlocking())));
        assertTrue(preview.getRisks().stream().anyMatch(risk ->
                "SNAPSHOT_DRIFT".equals(risk.getCode()) && "WARNING".equals(risk.getSeverity())));
    }

    @Test
    void previewApprovalMatrix_usesDeptLeaderAndRoleSourceRules() {
        DccFileCategoryDO category = createCategory("INTAUTH-9B", "负责人角色预览");
        DccApprovalPositionDO docControl = createPosition("POS-DOC-9B", "文控");
        positionAssignmentMapper.insert(createUserAssignment(docControl.getId(), 210L));
        when(positionRuntimeResolver.isUploaderDerivedPosition(anyLong())).thenReturn(false);
        when(deptApi.getDeptList(List.of(321L))).thenReturn(List.of(
                new DeptRespDTO().setId(321L).setName("新品开发部").setLeaderUserId(211L)));
        when(adminUserApi.getUser(211L)).thenReturn(new AdminUserRespDTO().setId(211L).setNickname("开发部负责人"));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(654L))).thenReturn(Set.of(212L));
        when(adminUserApi.getUserList(anyList())).thenReturn(List.of(
                new AdminUserRespDTO().setId(210L).setNickname("文控A"),
                new AdminUserRespDTO().setId(211L).setNickname("开发部负责人"),
                new AdminUserRespDTO().setId(212L).setNickname("角色成员A")));

        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 6, 25, 18, 0));
        reqVO.setRemark("owner-role");
        reqVO.setRules(List.of(
                new DccCategoryApprovalMatrixSaveReqVO.Rule()
                        .setStageType("SIGNOFF")
                        .setActive(true)
                        .setSubjectLabel("新品开发部")
                        .setMarker("▲")
                        .setSubjectType("DEPT")
                        .setSubjectId(321L)
                        .setSubjectDepartmentPath("瑛泰医疗-研发创新中心-新品开发部")
                        .setRemark("负责人"),
                new DccCategoryApprovalMatrixSaveReqVO.Rule()
                        .setStageType("APPROVAL")
                        .setActive(true)
                        .setSubjectLabel("技术委员会")
                        .setMarker("▲")
                        .setSubjectType("ROLE")
                        .setSubjectId(654L)
                        .setSubjectName("技术委员会")
                        .setRemark("角色")));

        DccCategoryReviewMatrixEffectivePreviewRespVO preview = matrixAdminService
                .previewApprovalMatrix(category.getId(), reqVO);

        assertTrue(preview.getStages().stream().anyMatch(stage -> "按部门负责人解析".equals(stage.getSourceRule())));
        assertTrue(preview.getStages().stream().anyMatch(stage -> "按系统角色解析".equals(stage.getSourceRule())));
        assertTrue(preview.getStages().stream().anyMatch(stage ->
                stage.getPositionNames() != null && stage.getPositionNames().contains("新品开发部 ▲")));
        assertTrue(preview.getStages().stream().anyMatch(stage ->
                stage.getPositionNames() != null && stage.getPositionNames().contains("技术委员会 ▲")));
    }

    @Test
    void saveApprovalMatrix_blocksWhenEffectivePreviewHasBlockingRisks() {
        DccFileCategoryDO category = createCategory("INTAUTH-10", "作业规程");
        createPosition("POS-DOC", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA", "QA");
        DccApprovalPositionDO qms = createPosition("POS-QMS", "QMS");
        DccApprovalPositionDO owner = createPosition("POS-OWNER", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-DELEGATE", "授权代表");
        when(positionRuntimeResolver.isUploaderDerivedPosition(anyLong())).thenReturn(false);
        when(adminUserApi.getUserList(anyList())).thenReturn(List.of());

        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 6, 23, 10, 0));
        reqVO.setRemark("blocked");
        reqVO.setRules(List.of(
                dccPositionRule("SIGNOFF", "QA", qa.getId(), "QA"),
                dccPositionRule("SIGNOFF", "QMS", qms.getId(), "QMS"),
                dccPositionRule("APPROVAL", "编制部门负责人", owner.getId(), "编制部门负责人"),
                dccPositionRule("APPROVAL", "授权代表", delegate.getId(), "授权代表")));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> matrixAdminService.saveApprovalMatrix(category.getId(), reqVO));
        assertEquals(DccCategoryApprovalMatrixAdminServiceImpl.CATEGORY_APPROVAL_MATRIX_EFFECTIVE_ACCESS_BLOCKED.getCode(),
                exception.getCode());
        assertTrue(exception.getMessage().contains("当前审阅矩阵未解析到任何实际用户")
                || exception.getMessage().contains("未分配任何有效用户"));
    }

    @Test
    void saveApprovalMatrix_blocksWhenDeptLeaderOrRoleUsersMissing() {
        DccFileCategoryDO category = createCategory("INTAUTH-10B", "负责人阻塞");
        DccApprovalPositionDO docControl = createPosition("POS-DOC-10B", "文控");
        positionAssignmentMapper.insert(createUserAssignment(docControl.getId(), 9101L));
        stubNonUploaderPositions();
        stubUserNames(9101L);
        when(deptApi.getDeptList(List.of(7001L))).thenReturn(List.of(
                new DeptRespDTO().setId(7001L).setName("质量体系中心").setLeaderUserId(null)));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(8001L))).thenReturn(Set.of());

        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 6, 25, 19, 0));
        reqVO.setRemark("blocked-owner-role");
        reqVO.setRules(List.of(
                new DccCategoryApprovalMatrixSaveReqVO.Rule()
                        .setStageType("SIGNOFF")
                        .setActive(true)
                        .setSubjectLabel("质量体系中心")
                        .setMarker("▲")
                        .setSubjectType("DEPT")
                        .setSubjectId(7001L)
                        .setSubjectDepartmentPath("瑛泰医疗-质量体系中心"),
                new DccCategoryApprovalMatrixSaveReqVO.Rule()
                        .setStageType("APPROVAL")
                        .setActive(true)
                        .setSubjectLabel("技术委员会")
                        .setMarker("▲")
                        .setSubjectType("ROLE")
                        .setSubjectId(8001L)
                        .setSubjectName("技术委员会")));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> matrixAdminService.saveApprovalMatrix(category.getId(), reqVO));

        assertEquals(DccCategoryApprovalMatrixAdminServiceImpl.CATEGORY_APPROVAL_MATRIX_EFFECTIVE_ACCESS_BLOCKED.getCode(),
                exception.getCode());
        assertTrue(exception.getMessage().contains("部门未配置负责人")
                || exception.getMessage().contains("负责人用户不存在")
                || exception.getMessage().contains("角色")
                || exception.getMessage().contains("DEPT_LEADER_MISSING")
                || exception.getMessage().contains("ROLE_EMPTY"));
    }

    @Test
    void importApprovalMatrix_persistsUploaderDerivedRulesWithoutBlockingPreview() {
        DccFileCategoryDO category = createCategory("INTAUTH-10C", "导入审批矩阵");
        DccApprovalPositionDO docControl = createPosition("POS-DOC-10C", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA-10C", "QA");
        DccApprovalPositionDO deptOwner = createPosition("POS-OWNER-10C", "部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-DELEGATE-10C", "授权代表");
        positionAssignmentMapper.insert(createUserAssignment(docControl.getId(), 9201L));
        positionAssignmentMapper.insert(createUserAssignment(qa.getId(), 9202L));
        when(positionRuntimeResolver.isUploaderDerivedPosition(docControl.getId())).thenReturn(false);
        when(positionRuntimeResolver.isUploaderDerivedPosition(qa.getId())).thenReturn(false);
        when(positionRuntimeResolver.isUploaderDerivedPosition(deptOwner.getId())).thenReturn(true);
        when(positionRuntimeResolver.isUploaderDerivedPosition(delegate.getId())).thenReturn(true);
        stubUserNames(9201L, 9202L);

        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 6, 30, 20, 0));
        reqVO.setRemark("import-uploader-derived");
        reqVO.setRules(List.of(
                dccPositionRule("SIGNOFF", "QA", qa.getId(), "QA"),
                dccPositionRule("APPROVAL", "部门负责人", deptOwner.getId(), "部门负责人"),
                dccPositionRule("APPROVAL", "授权代表", delegate.getId(), "授权代表")));

        ServiceException saveException = assertThrows(ServiceException.class,
                () -> matrixAdminService.saveApprovalMatrix(category.getId(), reqVO));
        assertEquals(DccCategoryApprovalMatrixAdminServiceImpl.CATEGORY_APPROVAL_MATRIX_EFFECTIVE_ACCESS_BLOCKED.getCode(),
                saveException.getCode());

        DccCategoryApprovalRouteDO route = matrixAdminService.importApprovalMatrix(category.getId(), reqVO);

        assertNotNull(route.getId());
        DccCategoryApprovalRouteDO savedRoute = routeMapper.selectById(route.getId());
        assertTrue(Boolean.TRUE.equals(savedRoute.getActive()));
        List<DccCategoryApprovalRouteNodeDO> nodes = routeNodeMapper.selectListByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageNo)
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getSort))
                .toList();
        assertEquals(5, nodes.size());
        assertEquals(List.of(docControl.getId(), qa.getId(), deptOwner.getId(), delegate.getId(), docControl.getId()),
                nodes.stream().map(DccCategoryApprovalRouteNodeDO::getSubjectId).toList());
        assertEquals(List.of("DCC_POSITION", "DCC_POSITION", "DCC_POSITION", "DCC_POSITION", "DCC_POSITION"),
                nodes.stream().map(DccCategoryApprovalRouteNodeDO::getSubjectType).toList());
        assertEquals(List.of("文控", "QA", "部门负责人", "授权代表", "文控"),
                nodes.stream().map(DccCategoryApprovalRouteNodeDO::getSubjectName).toList());
    }

    @Test
    void getUserReviewMatrixAccess_returnsCategoryCapabilitiesSourcesAndDownloadRuleReason() {
        DccFileCategoryDO category = createCategory("INTAUTH-11", "外来文件");
        DccApprovalPositionDO docControl = createPosition("POS-DOC", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA", "QA");
        DccApprovalPositionDO owner = createPosition("POS-OWNER", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-DELEGATE", "授权代表");
        positionAssignmentMapper.insert(createUserAssignment(docControl.getId(), 201L));
        positionAssignmentMapper.insert(createUserAssignment(qa.getId(), 202L));
        positionAssignmentMapper.insert(createUserAssignment(owner.getId(), 203L));
        positionAssignmentMapper.insert(createUserAssignment(delegate.getId(), 204L));
        when(positionRuntimeResolver.isUploaderDerivedPosition(anyLong())).thenReturn(false);
        when(adminUserApi.getUserList(anyList())).thenReturn(List.of(
                new AdminUserRespDTO().setId(201L).setNickname("文控A"),
                new AdminUserRespDTO().setId(202L).setNickname("会签A"),
                new AdminUserRespDTO().setId(203L).setNickname("批准A"),
                new AdminUserRespDTO().setId(204L).setNickname("授权代表A")));
        when(adminUserApi.getUser(201L)).thenReturn(new AdminUserRespDTO().setId(201L)
                .setNickname("文控A")
                .setDeptId(10L)
                .setPostIds(Set.of()));
        when(permissionApi.getUserRoleIdListByRoleIds(anyList())).thenReturn(Set.of());
        when(deptApi.getChildDeptList(anyLong())).thenReturn(List.of());
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(201L)).thenReturn(false);
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .actionType("DOWNLOAD")
                .subjectType("USER")
                .subjectId(201L)
                .active(Boolean.TRUE)
                .remark("download")
                .build());
        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 6, 23, 11, 0));
        reqVO.setRemark("lookup");
        reqVO.setRules(List.of(
                dccPositionRule("SIGNOFF", "QA", qa.getId(), "QA"),
                dccPositionRule("APPROVAL", "编制部门负责人", owner.getId(), "编制部门负责人"),
                dccPositionRule("APPROVAL", "授权代表", delegate.getId(), "授权代表")));
        matrixAdminService.saveApprovalMatrix(category.getId(), reqVO);

        List<DccCategoryReviewMatrixUserLookupRespVO> items = matrixAdminService.getUserReviewMatrixAccess(201L);

        assertEquals(1, items.size());
        DccCategoryReviewMatrixUserLookupRespVO item = items.get(0);
        assertEquals(category.getId(), item.getCategoryId());
        assertEquals("YES", item.getBrowseStatus());
        assertEquals("CURRENT_REVIEW_MATRIX", item.getBrowseSource());
        assertEquals("YES", item.getPublishedPreviewStatus());
        assertEquals("CONDITIONAL", item.getPendingPreviewStatus());
        assertEquals("ROUTE_SNAPSHOT", item.getPendingPreviewSource());
        assertEquals("CONDITIONAL", item.getDownloadStatus());
        assertEquals("DOWNLOAD_RULE", item.getDownloadSource());
    }

    @Test
    void deleteApprovalMatrix_deactivatesActiveRouteAndRemovesReviewApproveRules() {
        DccFileCategoryDO category = createCategory("INTAUTH-7", "包装规范");
        DccApprovalPositionDO docControl = createPosition("POS-DOC", "文控");
        DccApprovalPositionDO qa = createPosition("POS-QA", "QA");
        DccApprovalPositionDO qms = createPosition("POS-QMS", "QMS");
        DccApprovalPositionDO owner = createPosition("POS-OWNER", "编制部门负责人");
        DccApprovalPositionDO delegate = createPosition("POS-DELEGATE", "授权代表");
        positionAssignmentMapper.insert(createUserAssignment(docControl.getId(), 201L));
        positionAssignmentMapper.insert(createUserAssignment(qa.getId(), 202L));
        positionAssignmentMapper.insert(createUserAssignment(qms.getId(), 203L));
        positionAssignmentMapper.insert(createUserAssignment(owner.getId(), 204L));
        positionAssignmentMapper.insert(createUserAssignment(delegate.getId(), 205L));
        stubNonUploaderPositions();
        stubUserNames(201L, 202L, 203L, 204L, 205L);
        DccCategoryApprovalMatrixSaveReqVO reqVO = new DccCategoryApprovalMatrixSaveReqVO();
        reqVO.setEffectiveTime(LocalDateTime.of(2026, 6, 22, 10, 0));
        reqVO.setRemark("matrix");
        reqVO.setRules(List.of(
                dccPositionRule("SIGNOFF", "QA", qa.getId(), "QA"),
                dccPositionRule("SIGNOFF", "QMS", qms.getId(), "QMS"),
                dccPositionRule("APPROVAL", "编制部门负责人", owner.getId(), "编制部门负责人"),
                dccPositionRule("APPROVAL", "授权代表", delegate.getId(), "授权代表")));
        DccCategoryApprovalRouteDO route = matrixAdminService.saveApprovalMatrix(category.getId(), reqVO);
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .actionType("REVIEW")
                .subjectType("USER")
                .subjectId(101L)
                .active(Boolean.TRUE)
                .remark("legacy-review")
                .build());
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .actionType("APPROVE")
                .subjectType("USER")
                .subjectId(102L)
                .active(Boolean.TRUE)
                .remark("legacy-approve")
                .build());
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .id(randomLongId())
                .categoryId(category.getId())
                .actionType("VIEW")
                .subjectType("USER")
                .subjectId(103L)
                .active(Boolean.TRUE)
                .remark("keep-view")
                .build());

        matrixAdminService.deleteApprovalMatrix(category.getId());

        DccCategoryApprovalRouteDO savedRoute = routeMapper.selectById(route.getId());
        assertFalse(Boolean.TRUE.equals(savedRoute.getActive()));
        assertEquals(1, permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId, category.getId()).size());
        assertEquals(List.of("VIEW"), permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId, category.getId()).stream()
                .map(DccFileCategoryPermissionRuleDO::getActionType)
                .toList());
    }

    private DccFileCategoryDO createCategory(String code, String name) {
        DccFileCategoryDO category = DccFileCategoryDO.builder()
                .id(randomLongId())
                .code(code)
                .name(name)
                .lifecycleStage("PLAN")
                .active(Boolean.TRUE)
                .sort(1)
                .source("LOCAL")
                .remark("seed")
                .description(name)
                .distributionRequired(Boolean.FALSE)
                .trainingRequired(Boolean.FALSE)
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
                .remark("seed")
                .build();
        positionMapper.insert(position);
        return position;
    }

    private DccPositionAssignmentDO createUserAssignment(Long positionId, Long userId) {
        return DccPositionAssignmentDO.builder()
                .id(randomLongId())
                .positionId(positionId)
                .assignmentType("USER")
                .userId(userId)
                .active(Boolean.TRUE)
                .build();
    }

    private DccControlledFileDO createPendingFile(Long categoryId, Long fileId) {
        return DccControlledFileDO.builder()
                .id(fileId)
                .masterId(fileId)
                .categoryId(categoryId)
                .directoryId(20L)
                .sourceFileId(499L)
                .requesterId(88L)
                .submitterId(88L)
                .originalFileId(500L)
                .fileName("pending-file.docx")
                .title("Pending file")
                .fileNumber("SOP-PENDING")
                .needTraining(Boolean.FALSE)
                .processType("CREATE")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .build();
    }

    private DccCategoryApprovalMatrixSaveReqVO.Rule dccPositionRule(String stageType, String label,
                                                                    Long positionId, String positionName) {
        return new DccCategoryApprovalMatrixSaveReqVO.Rule()
                .setStageType(stageType)
                .setActive(true)
                .setSubjectLabel(label)
                .setMarker("●")
                .setSubjectType("DCC_POSITION")
                .setSubjectId(positionId)
                .setSubjectName(positionName)
                .setRemark(label);
    }

    private void stubNonUploaderPositions() {
        when(positionRuntimeResolver.isUploaderDerivedPosition(anyLong())).thenReturn(false);
    }

    private void stubUserNames(Long... userIds) {
        when(adminUserApi.getUserList(anyList())).thenReturn(Arrays.stream(userIds)
                .map(id -> new AdminUserRespDTO().setId(id).setNickname("用户#" + id))
                .toList());
    }

    private DccCategoryApprovalRouteNodeDO createRouteNode(Long routeId, int stageNo, String stageCode, String stageName,
                                                           Long candidateSourceId, List<Long> candidateSourceIds,
                                                           String approveMethod, boolean requireAllApprovals) {
        return DccCategoryApprovalRouteNodeDO.builder()
                .id(randomLongId())
                .routeId(routeId)
                .stageNo(stageNo)
                .stageCode(stageCode)
                .stageName(stageName)
                .stageOrder(stageNo)
                .candidateSourceType("POSITION")
                .candidateSourceId(candidateSourceId)
                .candidateSourceIds(candidateSourceIds.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse(""))
                .approveMethod(approveMethod)
                .approveRatio("ALL".equals(approveMethod) ? 100 : null)
                .requireAllApprovals(requireAllApprovals)
                .required(Boolean.TRUE)
                .sort(stageNo)
                .build();
    }

    private void insertRouteNodeWithSort(Long routeId, int stageNo, String stageCode, String stageName,
                                         Long candidateSourceId, List<Long> candidateSourceIds,
                                         String approveMethod, boolean requireAllApprovals, int sort) {
        DccCategoryApprovalRouteNodeDO node = createRouteNode(routeId, stageNo, stageCode, stageName,
                candidateSourceId, candidateSourceIds, approveMethod, requireAllApprovals);
        node.setSort(sort);
        routeNodeMapper.insert(node);
    }
}
