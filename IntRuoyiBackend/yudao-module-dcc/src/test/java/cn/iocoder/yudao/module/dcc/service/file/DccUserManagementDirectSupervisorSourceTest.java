package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real registered producer + original Resolver/Token/HMAC. System API/account directory is an external port;
 * no test double for DccDirectSupervisorSource is used in new cases. Original 13 source tests remain inherited. */
class DccUserManagementDirectSupervisorSourceTest extends DccNativeSupervisorSourceTest {
    AnnotationConfigApplicationContext application;
    DccUserManagementDirectSupervisorSource producer;
    AdminUserDirectSupervisorRespDTO relation;

    @BeforeEach void productionSourceFixture(TestInfo info){
        if(info.getTestMethod().orElseThrow().getDeclaringClass()!=DccUserManagementDirectSupervisorSourceTest.class)return;
        relation=new AdminUserDirectSupervisorRespDTO(1L,99L,88L,7L);
        when(users.getDirectSupervisorRelation(99L)).thenAnswer(ignored->relation);
        application=new AnnotationConfigApplicationContext();application.registerBean("adminUserApi",AdminUserApi.class,()->users);
        application.register(DccUserManagementDirectSupervisorSource.class);application.refresh();
        producer=application.getBean(DccUserManagementDirectSupervisorSource.class);providers=application.getBeanProvider(DccDirectSupervisorSource.class);
        ReflectionTestUtils.setField(resolver,"nativeSupervisors",providers);
    }
    @AfterEach void closeProductionContext(){if(application!=null)application.close();}

    @Test void productionSourceIsARegisteredImplementationOfTheExistingPort()throws Exception{
        Class<?> type=DccUserManagementDirectSupervisorSource.class;
        assertTrue(DccDirectSupervisorSource.class.isAssignableFrom(type));
        assertNotNull(type.getAnnotation(Service.class));
        assertEquals(1,application.getBeansOfType(DccDirectSupervisorSource.class).size());assertSame(producer,providers.getIfAvailable());
    }
    DccDirectSupervisorSource.Context context(String type){return new DccDirectSupervisorSource.Context(1L,99L,"UPLOAD".equals(type)?null:20L,5L,type);}
    DccDirectSupervisorSource.Evidence require(){return producer.require(context("UPLOAD"));}
    @ParameterizedTest @ValueSource(strings={"UPLOAD","REVISION","OBSOLETE"})
    void allApplicationsUseThePersistedExactUserRelation(String type){
        var evidence=producer.require(context(type));assertEquals(1L,evidence.tenantId());assertEquals(99L,evidence.requesterId());assertEquals(88L,evidence.supervisorUserId());
        assertEquals("SYSTEM_USER_MANAGEMENT",evidence.sourceSystem());assertEquals("SYSTEM_USER_MANAGEMENT:DIRECT_SUPERVISOR:1:99",evidence.sourceIdentity());assertEquals("7",evidence.sourceVersion());
        verify(users).getDirectSupervisorRelation(99L);verify(users,never()).getUserListBySubordinate(anyLong());verify(users,never()).getUserListByDeptIds(any());
        verifyNoInteractions(organization);
    }
    @ParameterizedTest @ValueSource(longs={0L,Long.MAX_VALUE})
    void relationVersionPreservesTheActualLongIncludingInitialZero(long version){relation.setRelationVersion(version);assertEquals(Long.toString(version),require().sourceVersion());}
    @ParameterizedTest @ValueSource(strings={"MISSING","TENANT","USER","NULL_VERSION","NEGATIVE_VERSION","UNSET","SELF","ZERO_ID","NEGATIVE_ID","REQUESTER_MISSING","REQUESTER_TENANT","REQUESTER_ID","REQUESTER_DISABLED","SUPERVISOR_MISSING","SUPERVISOR_TENANT","SUPERVISOR_ID","SUPERVISOR_DISABLED"})
    void missingInvalidOrDisabledRelationCannotBecomeAuthority(String invalid){
        switch(invalid){
            case "MISSING"->relation=null;case "TENANT"->relation.setTenantId(2L);case "USER"->relation.setUserId(100L);
            case "NULL_VERSION"->relation.setRelationVersion(null);case "NEGATIVE_VERSION"->relation.setRelationVersion(-1L);
            case "UNSET"->relation.setSupervisorUserId(null);case "SELF"->relation.setSupervisorUserId(99L);case "ZERO_ID"->relation.setSupervisorUserId(0L);case "NEGATIVE_ID"->relation.setSupervisorUserId(-1L);
            case "REQUESTER_MISSING"->doReturn(null).when(users).getUser(99L);case "REQUESTER_TENANT"->account(99L,99L,2L,0);case "REQUESTER_ID"->account(99L,100L,1L,0);case "REQUESTER_DISABLED"->account(99L,99L,1L,1);
            case "SUPERVISOR_MISSING"->doReturn(null).when(users).getUser(88L);case "SUPERVISOR_TENANT"->account(88L,88L,2L,0);case "SUPERVISOR_ID"->account(88L,89L,1L,0);case "SUPERVISOR_DISABLED"->account(88L,88L,1L,1);
        }
        var failure=assertThrows(IllegalStateException.class,this::require);if("UNSET".equals(invalid))assertTrue(failure.getMessage().contains("请先在用户管理设置直属主管"));
        verifyNoInteractions(organization);
    }
    void account(Long lookup,Long actual,Long tenant,Integer status){doReturn(new AdminUserRespDTO().setId(actual).setTenantId(tenant).setStatus(status).setPostIds(Set.of(10L))).when(users).getUser(lookup);}
    @ParameterizedTest @ValueSource(strings={"NULL_CONTEXT","TENANT","REQUESTER","ZERO_REQUESTER","TYPE","NULL_TYPE","IGNORE"})
    void invalidTenantContextNeverCallsSystemDirectory(String invalid){
        var input=context("UPLOAD");switch(invalid){case "NULL_CONTEXT"->input=null;case "TENANT"->input=new DccDirectSupervisorSource.Context(2L,99L,null,5L,"UPLOAD");case "REQUESTER"->input=new DccDirectSupervisorSource.Context(1L,null,null,5L,"UPLOAD");case "ZERO_REQUESTER"->input=new DccDirectSupervisorSource.Context(1L,0L,null,5L,"UPLOAD");case "TYPE"->input=context("OTHER");case "NULL_TYPE"->input=context(null);case "IGNORE"->TenantContextHolder.setIgnore(true);}
        clearInvocations(users);var invalidContext=input;assertThrows(IllegalArgumentException.class,()->producer.require(invalidContext));verifyNoInteractions(users);
    }
    @Test void systemReadFailureIsPropagatedWithoutGuessingSource(){
        var failure=new IllegalStateException("actual system directory failure");doThrow(failure).when(users).getDirectSupervisorRelation(99L);assertSame(failure,assertThrows(IllegalStateException.class,this::require));
    }
    @ParameterizedTest @ValueSource(strings={"INVALID","VERSION_INVALID"})
    void declaredSystemRelationFailuresBecomeTypedDomainUnready(String kind){
        var code="INVALID".equals(kind)?cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorErrors.INVALID:cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorErrors.VERSION_INVALID;
        var original=new cn.iocoder.yudao.framework.common.exception.ServiceException(code);doThrow(original).when(users).getDirectSupervisorRelation(99L);
        var failure=assertThrows(IllegalStateException.class,this::require);assertEquals("DIRECT_SUPERVISOR_RELATION_INVALID",ReflectionTestUtils.invokeMethod(failure,"getReasonCode"));assertSame(original,failure.getCause());
    }
    @Test void otherSystemBusinessErrorsAreNeverRelabeledAsSupervisorConfiguration(){
        var original=new cn.iocoder.yudao.framework.common.exception.ServiceException(cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorErrors.FORBIDDEN);doThrow(original).when(users).getDirectSupervisorRelation(99L);assertSame(original,assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,this::require));
    }
    @ParameterizedTest @ValueSource(strings={"NEW","REVISION"})
    void actualRegisteredProducerReachesPublicResolverAndTechnicalHmac(String type){
        var before=raw.canonical();var pure=resolver.resolvePublicMatrix(raw,99L,type,List.of(51L));
        var result=resolver.resolvePublicNative(raw,99L,type,20L,5L,"production-source-context","actual-exact-definition","a".repeat(64),List.of(51L));
        assertEquals(4,result.route().nodes().size());assertEquals(List.of(88L),result.route().nodes().get(0).resolvedUserIds());assertEquals(pure.nodes(),result.route().nodes().subList(1,4));
        assertEquals("SYSTEM_USER_MANAGEMENT",result.evidence().sourceSystem());assertEquals("7",result.evidence().sourceVersion());
        resolver.requireNativeExpectation(result,result.sourceDigest(),result.sourceToken());assertEquals(result.proof().payload(),tokens.verifyRecordedNativeSupervisor(result.sourceDigest(),result.sourceToken()));assertEquals(before,raw.canonical());verifyNoInteractions(organization);
    }
    @Test void unchangedRelationPreservesProofAndRealRevisionInvalidatesPreviousPreview(){
        var first=resolver.resolvePublicNative(raw,99L,"NEW",20L,5L,"production-source-context","actual-exact-definition","a".repeat(64),List.of(51L));
        var same=resolver.resolvePublicNative(raw,99L,"NEW",20L,5L,"production-source-context","actual-exact-definition","a".repeat(64),List.of(51L));assertEquals(first.sourceDigest(),same.sourceDigest());assertEquals(first.sourceToken(),same.sourceToken());
        relation.setSupervisorUserId(89L);relation.setRelationVersion(8L);
        var changed=resolver.resolvePublicNative(raw,99L,"NEW",20L,5L,"production-source-context","actual-exact-definition","a".repeat(64),List.of(51L));assertNotEquals(first.sourceDigest(),changed.sourceDigest());assertEquals(List.of(89L),changed.route().nodes().get(0).resolvedUserIds());assertThrows(IllegalArgumentException.class,()->resolver.requireNativeExpectation(changed,first.sourceDigest(),first.sourceToken()));
        assertEquals(first.proof().payload(),tokens.verifyRecordedNativeSupervisor(first.sourceDigest(),first.sourceToken()));
    }
    @Test void publicPrecheckCannotIssueAReadySupervisorWhenUserManagementHasNoSetting(){
        relation.setSupervisorUserId(null);var before=raw.canonical();var failure=assertThrows(IllegalStateException.class,()->resolver.resolvePublicNative(raw,99L,"NEW",null,5L,"production-source-context","actual-exact-definition","a".repeat(64),List.of(51L)));assertTrue(failure.getMessage().contains("DIRECT_SUPERVISOR_NOT_CONFIGURED"));assertEquals(before,raw.canonical());verifyNoInteractions(organization);
    }
    @Test void missingSettingHasATypedReasonCodeForPublicCallerMapping(){
        relation.setSupervisorUserId(null);var failure=assertThrows(IllegalStateException.class,this::require);
        assertEquals("DIRECT_SUPERVISOR_NOT_CONFIGURED",ReflectionTestUtils.invokeMethod(failure,"getReasonCode"));
    }
    @Test void domainSourceFailureHasAnExplicitBusinessErrorForHttpWithoutHidingTechnicalFailures(){
        relation.setSupervisorUserId(null);var failure=assertThrows(IllegalStateException.class,this::require);
        var business=ReflectionTestUtils.<cn.iocoder.yudao.framework.common.exception.ServiceException>invokeMethod(failure,"toServiceException");assertEquals(1080000365,business.getCode());assertEquals(failure.getMessage(),business.getMessage());
        var result=new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("ic32-task",mock(cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi.class)).serviceExceptionHandler(business);assertEquals(business.getCode(),result.getCode());assertTrue(result.getMsg().contains("请先在用户管理设置直属主管"));
    }
    @Test void oldFrozenRouteRetainsTheOriginalSupervisorWithoutReadingTodaysRelation(){
        var source=resolver.resolvePublicNative(raw,99L,"NEW",20L,5L,"production-source-context","actual-exact-definition","a".repeat(64),List.of(51L));var route=source.route();
        var file=DccControlledFileDO.builder().id(20L).tenantId(1L).submitterId(99L).dccProjectCodeId(5L).categoryId(2L).fileTypeTaxonomyId(6L).changeType("NEW").processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).preparedProcessDefinitionId("actual-exact-definition").preparedDefinitionDigest("a".repeat(64)).status("REJECTED").build();
        String runtime=JsonUtils.toJsonString(Map.of("formatVersion","DCC_RUNTIME_APPROVAL_NODES_V1","nodes",route.nodes(),"groups",route.runtimeGroups(),"nativeSupervisorSource",source.frozenSource()));
        var snapshots=route.nodes().stream().map(n->DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).routeVersionNo(1).stageNo(n.stageNo()).stageCode(n.stageCode()).stageName(n.stageName()).stageOrder(n.stageOrder()).candidateSourceType(n.candidateSourceType()).candidateSourceId(n.candidateSourceId()).candidateSourceIds(n.candidateSourceIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))).resolvedUserIds(n.resolvedUserIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))).approveMethod(n.approveMethod()).approveRatio(n.approveRatio()).requireAllApprovals(n.requireAllApprovals()).sourceApprovalMatrixId(raw.baseline().getRouteId()).sourceApprovalMatrixVersion(1).sourceApprovalMatrixDigest(raw.baseline().getMatrixDigest()).derivationVersion(route.derivationVersion()).sourceApprovalMatrixJson(raw.canonical()).runtimeApprovalNodesJson(runtime).build()).toList();
        var original=JsonUtils.toJsonString(snapshots);clearInvocations(users);doThrow(new IllegalStateException("today relation is unavailable")).when(users).getDirectSupervisorRelation(anyLong());
        var restored=resolver.restoreFrozenRoute(file,snapshots);assertEquals(List.of(88L),restored.nodes().get(0).resolvedUserIds());assertEquals(route.nodes(),restored.nodes());assertEquals(original,JsonUtils.toJsonString(snapshots));verify(users,never()).getDirectSupervisorRelation(anyLong());
    }
}
