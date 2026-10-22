package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.*;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class DccSelectedDepartmentReadinessTest extends BaseMockitoUnitTest {
    @Mock DccCategoryApprovalRouteMapper routes;
    @Mock DccCategoryApprovalRouteNodeMapper nodes;
    @Mock DeptApi departments;
    @Mock AdminUserApi users;
    @Mock PermissionApi permissions;
    @Mock DccElectronicSignatureAuthorizationService authorizations;
    @Mock DccElectronicSignatureImageService images;
    @Mock DccApprovalParticipantPostValidator posts;
    private final DccControlledFileApprovalRouteAssigneeResolver resolver = new DccControlledFileApprovalRouteAssigneeResolver();
    private final DccControlledFileRouteReadinessService readiness = new DccControlledFileRouteReadinessService();
    private List<DccCategoryApprovalRouteNodeDO> configuredNodes;

    @BeforeEach void fixture() {
        ReflectionTestUtils.setField(resolver,"routeMapper",routes);
        ReflectionTestUtils.setField(resolver,"routeNodeMapper",nodes);
        ReflectionTestUtils.setField(resolver,"deptApi",departments);
        ReflectionTestUtils.setField(resolver,"adminUserApi",users);
        ReflectionTestUtils.setField(resolver,"approvalParticipantPostValidator",posts);
        ReflectionTestUtils.setField(readiness,"routeAssigneeResolver",resolver);
        ReflectionTestUtils.setField(readiness,"adminUserApi",users);
        ReflectionTestUtils.setField(readiness,"permissionApi",permissions);
        ReflectionTestUtils.setField(readiness,"signatureAuthorizationService",authorizations);
        ReflectionTestUtils.setField(readiness,"signatureImageService",images);
        when(routes.selectLatestActiveByCategoryIdAndActionType(10L,"NEW"))
                .thenReturn(DccCategoryApprovalRouteDO.builder().id(20L).categoryId(10L).actionType("NEW").active(true).build());
        configuredNodes=List.of(node(1,"MATRIX_REVIEW","DEPT",40L),node(2,"MATRIX_APPROVAL","USER",201L),node(3,"DOC_CONTROL_REVIEW","USER",301L));
        when(nodes.selectListByRouteId(20L)).thenReturn(configuredNodes);
        when(departments.getDeptList(List.of(40L))).thenReturn(List.of());
        when(departments.getDeptList(List.of(51L))).thenReturn(List.of(new DeptRespDTO().setId(51L).setLeaderUserId(101L).setName("质量")));
        when(users.getUserList(anyCollection())).thenAnswer(call -> call.<Collection<Long>>getArgument(0).stream()
                .map(id -> new AdminUserRespDTO().setId(id).setNickname("人员"+id).setStatus(0).setPostIds(Set.of(1L))).toList());
        when(permissions.hasAnyPermissions(anyLong(),anyString())).thenReturn(true);
        when(authorizations.getAuthorizationMap(anyCollection())).thenAnswer(call -> call.<Collection<Long>>getArgument(0).stream().collect(Collectors.toMap(id->id,id->true)));
        when(images.requireActiveSnapshot(anyLong())).thenReturn(DccElectronicSignatureImageSnapshot.builder()
                .imageId(1L).fileId(2L).verifiedStatus("VALID").build());
    }

    @Test void removedDefaultDepartmentIsNotResolvedBeforeItsExplicitReplacement() {
        var result=readiness.evaluateDepartments(10L,99L,List.of(51L),"NEW");
        assertTrue(result.response().getReady());
        assertEquals(List.of(51L),result.resolvedRoute().nodes().get(0).candidateSourceIds());
        assertEquals(List.of(101L),result.resolvedRoute().nodes().get(0).resolvedUserIds());
        assertEquals(List.of(201L),result.resolvedRoute().nodes().get(1).resolvedUserIds());
        assertEquals(List.of(301L),result.resolvedRoute().nodes().get(2).resolvedUserIds());
        assertEquals(40L,configuredNodes.get(0).getCandidateSourceId());
        verify(departments,never()).getDeptList(List.of(40L));
    }

    @Test void omittedSelectionRetainsTheActualDefaultAndDoesNotInventAReplacement() {
        assertThrows(RuntimeException.class,()->readiness.evaluateDepartments(10L,99L,null,"NEW"));
        verify(departments).getDeptList(List.of(40L));
    }

    @Test void invalidExplicitSelectionsRejectBeforeDirectoryLookups() {
        for (var selected : List.of(List.<Long>of(),List.of(51L,51L),List.of(0L)))
            assertThrows(RuntimeException.class,()->readiness.evaluateDepartments(10L,99L,selected,"NEW"));
        verifyNoInteractions(departments,users);
    }

    @Test void publicHttpPreviewRunsTheActualWorkflowReadinessWithEditedDepartments() throws Exception {
        var categoryMapper=mock(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class);
        when(categoryMapper.selectById(10L)).thenReturn(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder()
                .id(10L).active(true).source("LOCAL").build());
        var workflow=new DccControlledFileWorkflowServiceImpl();
        ReflectionTestUtils.setField(workflow,"categoryMapper",categoryMapper);
        ReflectionTestUtils.setField(workflow,"routeReadinessService",readiness);
        var controller=new cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController();
        ReflectionTestUtils.setField(controller,"workflowService",workflow);
        try(var auth=mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class)) {
            auth.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build()
                    .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/dcc/controlled-files/route-preview")
                            .contentType("application/json").content("{\"categoryId\":10,\"actionType\":\"NEW\",\"selectedSignoffDepartmentIds\":[\"51\"]}"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.ready").value(true))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.nodes[0].candidateSourceIds[0]").value(51));
        }
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->workflow.previewRoute(99L,10L,List.of(999L),List.of(51L),"NEW"));
        verify(departments,never()).getDeptList(List.of(40L));
    }

    @Test void formalObsoleteSelectionKeepsItsTwoStagesAndConfiguredPostChecks() {
        when(routes.selectLatestActiveByCategoryIdAndActionType(10L,"OBSOLETE"))
                .thenReturn(DccCategoryApprovalRouteDO.builder().id(20L).categoryId(10L).actionType("OBSOLETE").active(true).build());
        when(nodes.selectListByRouteId(20L)).thenReturn(configuredNodes.subList(0,2));
        var result=resolver.resolveRoute(10L,99L,"OBSOLETE",List.of(51L));
        assertEquals(List.of("MATRIX_REVIEW","MATRIX_APPROVAL"),result.nodes().stream().map(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode::stageCode).toList());
        assertEquals(List.of(51L),result.nodes().get(0).candidateSourceIds());
        verify(posts).requireConfiguredPosts(List.of(101L));
        verify(posts).requireConfiguredPosts(List.of(201L));
        verify(departments,never()).getDeptList(List.of(40L));
    }

    private DccCategoryApprovalRouteNodeDO node(int no,String code,String source,long id) {
        boolean review=no==1;
        return DccCategoryApprovalRouteNodeDO.builder().routeId(20L).stageNo(no).stageOrder(no).sort(no)
                .stageCode(code).stageName(code).candidateSourceType(source).candidateSourceId(id)
                .candidateSourceIds(String.valueOf(id)).required(true).requireAllApprovals(review)
                .approveMethod(review?"ALL":"ANY").approveRatio(review?100:null).build();
    }
}
