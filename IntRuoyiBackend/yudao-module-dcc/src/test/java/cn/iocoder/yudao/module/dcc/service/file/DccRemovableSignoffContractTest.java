package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual PUBLIC preview/resolver/HMAC/readiness and final frozen evidence guard.
 * Organization, permissions and prepared public-source selection are explicit isolated ports.
 * No HTTP/E2E, database writes, signature creation or live organization validation is claimed.
 */
class DccRemovableSignoffContractTest {
    private final DccControlledFileApprovalRouteAssigneeResolver resolver = new DccControlledFileApprovalRouteAssigneeResolver();
    private final DccControlledFileWorkflowServiceImpl workflow = new DccControlledFileWorkflowServiceImpl();
    private final DccApprovalMatrixTokenService tokens = new DccApprovalMatrixTokenService();
    private DeptApi departments;
    private AdminUserApi users;
    private DccProjectAccessService access;
    private DccDirectSupervisorSource organization;
    private DccApprovalMatrixSourceService.Source source;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(71L).setTenantId(1L).setUserType(2),new MockHttpServletRequest());
        var raw = new ArrayList<>(new DccPublicMatrixNativeDerivationTest().raw());
        raw.get(2).setSubjectType("USER");raw.get(2).setCandidateSourceType("USER");raw.get(2).setCandidateSourceIds("100");
        raw.add(DccCategoryApprovalRouteNodeDO.builder().id(4L).stageType("SIGNOFF").marker("◎").subjectType("DEPT")
                .candidateSourceType("DEPT").candidateSourceIds("52").approveMethod("ALL").approveRatio(100).requireAllApprovals(true).required(true).build());
        var baseline = new DccApprovalMatrixBaselineVO();
        baseline.setTenantId(1L);baseline.setCategoryId(2L);baseline.setFileTypeTaxonomyId(4L);baseline.setRouteId(20L);
        baseline.setRouteVersionNo(1);baseline.setActionType("LEGACY");baseline.setMatrixDigest("a".repeat(64));
        source = new DccApprovalMatrixSourceService.Source(DccCategoryApprovalRouteDO.builder().id(20L).categoryId(2L)
                .actionType("LEGACY").versionNo(1).build(),raw,baseline,"isolated-public-source-selection");
        departments = mock(DeptApi.class);
        when(departments.getDeptList(anyCollection())).thenAnswer(call -> call.<Collection<Long>>getArgument(0).stream()
                .map(id -> new DeptRespDTO().setId(id).setName("部门"+id).setStatus(0).setLeaderUserId(90L)).toList());
        users = mock(AdminUserApi.class);
        when(users.getUser(anyLong())).thenAnswer(call -> account(call.getArgument(0)));
        when(users.getUserList(anyCollection())).thenAnswer(call -> call.<Collection<Long>>getArgument(0).stream().map(this::account).toList());
        var properties = new DccApprovalMatrixTokenProperties();
        properties.setSigningSecret("G04-isolated-native-source-HMAC-key-at-least-32-bytes");
        ReflectionTestUtils.setField(tokens,"properties",properties);
        var providers = mock(ObjectProvider.class);
        organization = mock(DccDirectSupervisorSource.class);
        when(providers.getIfAvailable()).thenReturn(organization);
        when(organization.require(any())).thenReturn(new DccDirectSupervisorSource.Evidence(1L,71L,88L,"FORMAL_TEST_PORT","actor-71","org-v1"));
        wire(resolver,"deptApi",departments,"adminUserApi",users,"nativeSupervisors",providers,"nativeSourceTokens",tokens);
        var readiness = new DccControlledFileRouteReadinessService();
        var permission = mock(PermissionApi.class);when(permission.hasAnyPermissions(anyLong(),any(String[].class))).thenReturn(true);
        var authorization = mock(DccElectronicSignatureAuthorizationService.class);
        when(authorization.getAuthorizationMap(anyCollection())).thenAnswer(call -> call.<Collection<Long>>getArgument(0).stream()
                .collect(Collectors.toMap(id -> id,id -> true)));
        var images = mock(DccElectronicSignatureImageService.class);
        when(images.requireActiveSnapshot(anyLong())).thenReturn(DccElectronicSignatureImageSnapshot.builder()
                .imageId(1L).fileId(2L).verifiedStatus("VALID").build());
        wire(readiness,"adminUserApi",users,"permissionApi",permission,"signatureAuthorizationService",authorization,"signatureImageService",images);
        var categories = mock(DccFileCategoryMapper.class);
        when(categories.selectById(2L)).thenReturn(DccFileCategoryDO.builder().id(2L).active(true).build());
        access = mock(DccProjectAccessService.class);
        var baselines = mock(DccApprovalMatrixBaselineService.class);
        when(baselines.preview(eq(71L),any())).thenReturn(new DccApprovalMatrixBaselineService.Comparison(
                new DccApprovalMatrixTokenService.Context(1L,71L,3L,2L,4L,"NEW",null,"g04-default-context"),
                source.canonical(),baseline,source,"upstream-baseline-port",null,false));
        var execution = mock(DccApplicationExecutionContractService.class);
        when(execution.preparePublicPreview(anyString())).thenAnswer(call -> new DccApplicationExecutionContractService.Prepared(
                DccApplicationExecutionContractService.PUBLIC,"g04-exact-definition","b".repeat(64),call.getArgument(0)));
        wire(workflow,"categoryMapper",categories,"projectAccessService",access,"approvalMatrixBaselines",baselines,
                "executionContracts",execution,"approvalRouteAssigneeResolver",resolver,"routeReadinessService",readiness);
    }
    @AfterEach void clear() {
        TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    private AdminUserRespDTO account(Long id) {
        return new AdminUserRespDTO().setId(id).setTenantId(1L).setStatus(0).setPostIds(Set.of(10L)).setNickname("正式账号"+id);
    }
    private void wire(Object target,Object... fields) {
        for(int index=0;index<fields.length;index+=2)ReflectionTestUtils.setField(target,(String)fields[index],fields[index+1]);
    }
    private DccControlledFileRouteReadinessRespVO preview(List<Long> selected) {
        var request = new DccControlledFileRoutePreviewReqVO();request.setCategoryId(2L);request.setDccProjectCodeId(3L);
        request.setActionType("NEW");request.setExecutionContract(DccApplicationExecutionContractService.PUBLIC);
        request.setApplicationContextKey("g04-default-context");request.setSelectedSignoffDepartmentIds(selected);
        return workflow.previewRoute(71L,request);
    }
    @Test void firstActualPublicPreviewWithOptionalDefaultReturnsBothDepartmentsAndBoundSupervisorProof() {
        var before = source.rawNodes().stream().map(DccCategoryApprovalRouteNodeDO::toString).toList();
        var result = preview(null);
        assertTrue(result.getReady(),() -> "Actual preview blockers: "+result.getBlockers());
        assertEquals(List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW"),
                result.getNodes().stream().map(DccControlledFileRoutePreviewRespVO::getStageCode).toList());
        var signoff = result.getNodes().get(1);
        assertEquals(List.of(51L,52L),signoff.getCandidateSourceIds());assertEquals(List.of(90L,90L),signoff.getResolvedUserIds());
        assertEquals("ALL",signoff.getApproveMethod());assertTrue(signoff.getRequireAllApprovals());
        assertEquals(List.of("51","52"),tokens.verifyRecordedNativeSupervisor(result.getSupervisorSourceDigest(),result.getSupervisorSourceToken()).get("selectedDepartments"));
        assertEquals(before,source.rawNodes().stream().map(DccCategoryApprovalRouteNodeDO::toString).toList());
        verify(access).assertProjectEditorOrOwner(71L,3L);
    }
    @Test void actualExplicitPreviewDeletesDefaultAAndBindsBothBAndAddedCWithoutMergingTheirLeader() {
        var removed = preview(List.of(52L));assertTrue(removed.getReady());
        assertEquals(List.of(52L),removed.getNodes().get(1).getCandidateSourceIds());
        var added = preview(List.of(53L,52L));assertTrue(added.getReady());
        assertEquals(List.of(52L,53L),added.getNodes().get(1).getCandidateSourceIds());
        assertEquals(List.of(90L,90L),added.getNodes().get(1).getResolvedUserIds());
        assertEquals(List.of("52","53"),tokens.verifyRecordedNativeSupervisor(added.getSupervisorSourceDigest(),added.getSupervisorSourceToken()).get("selectedDepartments"));
        assertEquals("ALL",added.getNodes().get(1).getApproveMethod());assertEquals(100,added.getNodes().get(1).getApproveRatio());
        assertFalse(added.getRuntimeRuleGroups().stream().filter(g -> g.sourceNodeId().equals(1L)).findFirst().orElseThrow().applied());
        verify(departments,never()).getDeptList(List.of(51L));
    }
    @Test void emptyActualPreviewReturnsTheRequiredHintAndDoesNotLookupOrRestoreDefaultDepartments() {
        var result = preview(List.of());assertFalse(result.getReady());assertTrue(result.getNodes().isEmpty());
        assertTrue(result.getBlockers().get(0).getMessage().contains("请至少选择一个会签部门"));
        verifyNoInteractions(departments,organization);
    }
    @Test void sourcePermissionMissingLeaderAndDisabledAccountsAreStillRealPreviewFailures() {
        doThrow(new IllegalStateException("formal project permission denied")).when(access).assertProjectEditorOrOwner(71L,3L);
        assertThrows(IllegalStateException.class,() -> preview(List.of(52L)));
        reset(access);when(departments.getDeptList(List.of(52L))).thenReturn(List.of(new DeptRespDTO().setId(52L).setName("B")));
        assertThrows(ServiceException.class,() -> preview(List.of(52L)));
        when(departments.getDeptList(List.of(52L))).thenReturn(List.of(new DeptRespDTO().setId(52L).setName("B").setLeaderUserId(90L)));
        when(users.getUserList(anyCollection())).thenAnswer(call -> call.<Collection<Long>>getArgument(0).stream()
                .map(id -> account(id).setStatus(id.equals(90L)?1:0)).toList());
        var disabled = preview(List.of(52L));assertFalse(disabled.getReady());
        assertTrue(disabled.getBlockers().stream().anyMatch(b -> "APPROVER_ACCOUNT_INVALID".equals(b.getReasonCode())));
    }

    // Finalization software evidence checks consume the actual newly derived B/C roster.
    // These records are isolated test evidence, not a claim to have created real electronic signatures.
    private DccControlledFileDO file() {
        return DccControlledFileDO.builder().id(200L).versionNo("A/1").processInstanceId("g04-current-round")
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).build();
    }
    private List<DccControlledFileRouteSnapshotDO> roster() {
        var route = resolver.resolvePublicMatrix(source,71L,"NEW",List.of(52L,53L));
        return route.nodes().stream().map(n -> DccControlledFileRouteSnapshotDO.builder().controlledFileId(200L)
                .stageCode(n.stageCode()).candidateSourceType(n.candidateSourceType())
                .candidateSourceIds(n.candidateSourceIds().stream().map(String::valueOf).collect(Collectors.joining(",")))
                .resolvedUserIds(n.resolvedUserIds().stream().map(String::valueOf).collect(Collectors.joining(",")))
                .approveMethod(n.approveMethod()).requireAllApprovals(n.requireAllApprovals()).build()).toList();
    }
    private List<DccControlledFileTaskAssigneeSnapshotDO> assignments() {
        return List.of(52L,53L).stream().map(id -> DccControlledFileTaskAssigneeSnapshotDO.builder().controlledFileId(200L)
                .departmentId(id).stageCode("MATRIX_REVIEW").processInstanceId("g04-current-round").leaderUserId(90L)
                .assigneeUserId(90L).assignmentSignatureId(id).assignedTime(LocalDateTime.of(2026,10,15,9,0))
                .bpmTaskId("review-"+id).build()).toList();
    }
    private DccControlledFileSignatureDO signature(long id,long actor,String task,String stage,String action) {
        return DccControlledFileSignatureDO.builder().id(id).controlledFileId(200L).revisionId(200L).versionNo("A/1")
                .actorId(actor).taskId(task).processInstanceId("g04-current-round").actionType(action).meaningCode(stage+"_"+action)
                .passwordVerified(true).signedAt(LocalDateTime.of(2026,10,15,9,0)).evidencePayloadVersion("v4-workflow")
                .evidenceHash("isolated-test-evidence").evidenceStatus("VALID").build();
    }
    private List<DccControlledFileSignatureDO> evidence() {
        return new ArrayList<>(List.of(signature(52L,90L,"review-52","MATRIX_REVIEW","ASSIGN"),
                signature(152L,90L,"review-52","MATRIX_REVIEW","APPROVE"),
                signature(53L,90L,"review-53","MATRIX_REVIEW","ASSIGN"),
                signature(153L,90L,"review-53","MATRIX_REVIEW","APPROVE"),
                signature(199L,99L,"approval","MATRIX_APPROVAL","APPROVE"),
                signature(200L,100L,"control","DOC_CONTROL_REVIEW","APPROVE")));
    }
    @Test void bothSelectedBAndAddedCNeedSeparateAssignmentAndPassedSignatureEvenForOneLeader() {
        assertDoesNotThrow(() -> DccFrozenApprovalSignatures.requireComplete(file(),roster(),evidence(),assignments()));
        var missingC = evidence();missingC.remove(3);
        assertThrows(ServiceException.class,() -> DccFrozenApprovalSignatures.requireComplete(file(),roster(),missingC,assignments()));
        missingC.add(missingC.get(1)); // Same B task twice never satisfies C.
        assertThrows(ServiceException.class,() -> DccFrozenApprovalSignatures.requireComplete(file(),roster(),missingC,assignments()));
        assertThrows(ServiceException.class,() -> DccFrozenApprovalSignatures.requireComplete(file(),roster(),evidence(),assignments().subList(0,1)));
    }
    @Test void rejectedWrongRoundWrongVersionOrUnsignedAddedCPreventsCompletion() {
        for(var bad:List.of("REJECT","ROUND","VERSION","UNSIGNED","ASSIGN")) {
            var signatures = evidence();var c = signatures.get(3);
            switch(bad) {
                case "REJECT" -> c.setActionType("REJECT");
                case "ROUND" -> c.setProcessInstanceId("g04-previous-round");
                case "VERSION" -> c.setVersionNo("A/2");
                case "UNSIGNED" -> c.setPasswordVerified(false);
                case "ASSIGN" -> signatures.remove(2);
            }
            assertThrows(ServiceException.class,() -> DccFrozenApprovalSignatures.requireComplete(file(),roster(),signatures,assignments()),bad);
        }
    }
}
