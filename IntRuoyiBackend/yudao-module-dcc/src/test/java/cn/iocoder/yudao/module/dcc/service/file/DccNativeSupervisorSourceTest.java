package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Existing actual raw projection, token/HMAC and account validation; provider/directory explicit isolated ports. */
class DccNativeSupervisorSourceTest {
    @Test void defaultDepartmentPreviewSignsActualDerivedDepartmentObligations(){
        var source=resolver.resolvePublicNative(raw,99L,"NEW",null,3L,"default-department-context","actual-exact-definition","a".repeat(64),null);
        var recorded=tokens.verifyRecordedNativeSupervisor(source.sourceDigest(),source.sourceToken());
        assertEquals(List.of("51"),recorded.get("selectedDepartments"));
        assertEquals(List.of(51L),source.route().nodes().stream().filter(n->"MATRIX_REVIEW".equals(n.stageCode())).findFirst().orElseThrow().candidateSourceIds());
    }
    DccControlledFileApprovalRouteAssigneeResolver resolver;
    DccApprovalMatrixTokenService tokens;
    DccApprovalMatrixSourceService.Source raw;
    org.springframework.beans.factory.ObjectProvider<DccDirectSupervisorSource> providers;
    DccDirectSupervisorSource organization;
    cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @BeforeEach void setup()throws Exception{
        TenantContextHolder.setTenantId(1L);SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(99L).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
        resolver=new DccControlledFileApprovalRouteAssigneeResolver();var departments=mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class);when(departments.getDeptList(any())).thenReturn(List.of(new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(51L).setStatus(0).setLeaderUserId(99L)));
        users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);when(users.getUser(anyLong())).thenAnswer(c->new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(c.getArgument(0)).setTenantId(1L).setStatus(0).setPostIds(Set.of(10L)).setUsername("source-user").setNickname("真实候选"));when(users.getUserList(any())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(users::getUser).toList());
        ReflectionTestUtils.setField(resolver,"deptApi",departments);ReflectionTestUtils.setField(resolver,"adminUserApi",users);
        var position=mock(cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver.class);var assignments=mock(cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper.class);when(assignments.selectActiveListByPositionId(any())).thenReturn(List.of());ReflectionTestUtils.setField(resolver,"positionAssignmentMapper",assignments);ReflectionTestUtils.setField(resolver,"positionRuntimeResolver",position);
        String canonical=new DccPreparedExecutionInheritanceDatabaseTest().canonicalMatrix(1);var baseline=DccApprovalMatrixSourceService.readCanonical(canonical);var document=JsonUtils.parseObject(canonical,Map.class);var rows=JsonUtils.parseArray(JsonUtils.toJsonString(document.get("nodes")),DccCategoryApprovalRouteNodeDO.class);rows.get(2).setCandidateSourceType("USER");rows.get(2).setCandidateSourceIds("99");var canonicalRows=(List<Map<String,Object>>)document.get("nodes");canonicalRows.get(2).put("candidateSourceType","USER");canonicalRows.get(2).put("candidateSourceIds","99");canonicalRows.get(2).put("subjectType","USER");canonical=DccApprovalMatrixCanonicalizer.canonicalize(document);baseline=DccApprovalMatrixSourceService.readCanonical(canonical);raw=new DccApprovalMatrixSourceService.Source(DccCategoryApprovalRouteDO.builder().id(baseline.getRouteId()).categoryId(2L).actionType("LEGACY").versionNo(1).build(),rows,baseline,canonical);
        providers=mock(org.springframework.beans.factory.ObjectProvider.class);organization=mock(DccDirectSupervisorSource.class);when(providers.getIfAvailable()).thenReturn(organization);when(organization.require(any())).thenAnswer(c->{var context=c.<DccDirectSupervisorSource.Context>getArgument(0);return new DccDirectSupervisorSource.Evidence(context.tenantId(),context.requesterId(),88L,"FORMAL_DIRECTORY_PORT","org-person-99","org-v1");});
        tokens=new DccApprovalMatrixTokenService();var key=new DccApprovalMatrixTokenProperties();key.setSigningSecret("ic31-native-technical-HMAC-key-at-least-32-bytes");ReflectionTestUtils.setField(tokens,"properties",key);
        for(var name:List.of("nativeSupervisors","nativeSourceTokens"))if(Arrays.stream(resolver.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals(name)))ReflectionTestUtils.setField(resolver,name,name.equals("nativeSupervisors")?providers:tokens);
    }
    @AfterEach void clear(){TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    Object resolve(String type,Long file){return ReflectionTestUtils.invokeMethod(resolver,"resolvePublicNative",raw,99L,type,file,5L,"native-upload-context","actual-exact-definition","a".repeat(64),List.of(51L));}
    @ParameterizedTest @ValueSource(strings={"NEW","REVISION"})
    void nativeSourceConsumesFormalProviderAndAddsSupervisorWithoutChangingPureMatrixOrObsoleteDerivation(String type){
        var pure=resolver.resolvePublicMatrix(raw,99L,type,List.of(51L));assertEquals(3,pure.nodes().size());Object source=resolve(type,null);var route=ReflectionTestUtils.<DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute>invokeMethod(source,"route");assertEquals(List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW"),route.nodes().stream().map(r->r.stageCode()).toList());assertEquals(List.of(88L),route.nodes().get(0).resolvedUserIds());assertEquals(0,route.nodes().get(0).stageNo());assertEquals("ANY",route.nodes().get(0).approveMethod());assertEquals(pure.nodes(),route.nodes().subList(1,4));assertTrue(((String)ReflectionTestUtils.invokeMethod(source,"sourceDigest")).matches("[a-f0-9]{64}"));assertNotNull(ReflectionTestUtils.invokeMethod(source,"sourceToken"));verify(organization).require(new DccDirectSupervisorSource.Context(1L,99L,null,5L,type.equals("NEW")?"UPLOAD":"REVISION"));assertEquals(2,resolver.resolvePublicMatrix(raw,99L,"OBSOLETE",List.of(51L)).nodes().size());
    }
    @ParameterizedTest @ValueSource(strings={"NO_PROVIDER","TENANT","ACTOR","SOURCE","ACCOUNT","DISABLED","POST","AUTH","PROVIDER_FAILURE"})
    void invalidSourceCannotBecomeNativeSupervisor(String bad){
        switch(bad){case "NO_PROVIDER"->when(providers.getIfAvailable()).thenReturn(null);case "TENANT"->doReturn(new DccDirectSupervisorSource.Evidence(2L,99L,88L,"FORMAL","id","v1")).when(organization).require(any());case "ACTOR"->doReturn(new DccDirectSupervisorSource.Evidence(1L,77L,88L,"FORMAL","id","v1")).when(organization).require(any());case "SOURCE"->doReturn(new DccDirectSupervisorSource.Evidence(1L,99L,88L,"","id","v1")).when(organization).require(any());case "ACCOUNT"->when(users.getUser(88L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(88L).setTenantId(2L).setStatus(0).setPostIds(Set.of(10L)));case "DISABLED"->when(users.getUser(88L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(88L).setTenantId(1L).setStatus(1).setPostIds(Set.of(10L)));case "POST"->when(users.getUser(88L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(88L).setTenantId(1L).setStatus(0).setPostIds(Set.of()));case "AUTH"->SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(77L).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());case "PROVIDER_FAILURE"->doThrow(new IllegalStateException("actual organizational reader failed")).when(organization).require(any());}
        assertThrows(RuntimeException.class,()->resolve("NEW",null));
    }
    @Test void changedFormalEvidenceCannotPassPreviousPreviewExpectation(){
        Object first=resolve("NEW",null);String digest=ReflectionTestUtils.invokeMethod(first,"sourceDigest"),token=ReflectionTestUtils.invokeMethod(first,"sourceToken");ReflectionTestUtils.invokeMethod(resolver,"requireNativeExpectation",first,digest,token);
        doReturn(new DccDirectSupervisorSource.Evidence(1L,99L,89L,"FORMAL_DIRECTORY_PORT","org-person-99","org-v2")).when(organization).require(any());Object changed=resolve("NEW",null);assertThrows(IllegalArgumentException.class,()->ReflectionTestUtils.invokeMethod(resolver,"requireNativeExpectation",changed,digest,token));
    }
    @Test void frozenPublicFourStagesRestoreTheirSupervisorWithoutApplyingLegacyThreeStagePolicy(){
        var nativeSource=ReflectionTestUtils.<DccControlledFileApprovalRouteAssigneeResolver.NativeApprovalSource>invokeMethod(resolver,"resolvePublicNative",raw,99L,"NEW",20L,5L,"native-upload-context","actual-exact-definition","a".repeat(64),List.of(51L));var route=nativeSource.route();var file=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().id(20L).tenantId(1L).dccProjectCodeId(5L).categoryId(2L).fileTypeTaxonomyId(6L).changeType("NEW").processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).status("REJECTED").build();String runtime=JsonUtils.toJsonString(Map.of("formatVersion","DCC_RUNTIME_APPROVAL_NODES_V1","nodes",route.nodes(),"groups",route.runtimeGroups()));var snapshots=route.nodes().stream().map(n->cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).routeVersionNo(1).stageNo(n.stageNo()).stageCode(n.stageCode()).stageName(n.stageName()).stageOrder(n.stageOrder()).candidateSourceType(n.candidateSourceType()).candidateSourceId(n.candidateSourceId()).candidateSourceIds(n.candidateSourceIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))).resolvedUserIds(n.resolvedUserIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))).approveMethod(n.approveMethod()).approveRatio(n.approveRatio()).requireAllApprovals(n.requireAllApprovals()).sourceApprovalMatrixId(raw.baseline().getRouteId()).sourceApprovalMatrixVersion(1).sourceApprovalMatrixDigest(raw.baseline().getMatrixDigest()).derivationVersion(route.derivationVersion()).sourceApprovalMatrixJson(raw.canonical()).runtimeApprovalNodesJson(runtime).build()).toList();var restored=resolver.restoreFrozenRoute(file,snapshots);assertEquals(route.nodes(),restored.nodes());
    }
}
