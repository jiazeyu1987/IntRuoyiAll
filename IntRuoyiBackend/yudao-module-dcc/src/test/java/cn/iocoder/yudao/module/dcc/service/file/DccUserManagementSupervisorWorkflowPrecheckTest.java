package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserDirectSupervisorRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Public Workflow preview with the actual Spring producer, H2 matrix/attributes and exact Flowable model.
 * Original 73 complete native cases are inherited unchanged. New cases only mock system account/API reads. */
class DccUserManagementSupervisorWorkflowPrecheckTest extends DccNativeUploadSupervisorWorkflowTest {
    AnnotationConfigApplicationContext sourceContext;
    AdminUserDirectSupervisorRespDTO systemRelation;
    AdminUserApi systemUsers;
    @BeforeEach void registeredUserManagementSource(TestInfo info){
        if(info.getTestMethod().orElseThrow().getDeclaringClass()!=DccUserManagementSupervisorWorkflowPrecheckTest.class)return;
        systemUsers=(AdminUserApi)ReflectionTestUtils.getField(workflow,"adminUserApi");
        systemRelation=new AdminUserDirectSupervisorRespDTO(1L,99L,88L,3L);
        when(systemUsers.getDirectSupervisorRelation(99L)).thenAnswer(ignored->systemRelation);
        sourceContext=new AnnotationConfigApplicationContext();sourceContext.registerBean("adminUserApi",AdminUserApi.class,()->systemUsers);
        sourceContext.register(DccUserManagementDirectSupervisorSource.class);sourceContext.refresh();
        wire(nativeResolver,"nativeSupervisors",sourceContext.getBeanProvider(DccDirectSupervisorSource.class));
    }
    @AfterEach void closeOwnSourceContext(){if(sourceContext!=null)sourceContext.close();}
    @Test void registeredProductionSourceReachesTheActualPublicPreviewWithoutWrites(){
        var originalFile=files.selectById(20L);var originalAttributes=jdbc.queryForList("SELECT * FROM dcc_project_application_attributes ORDER BY id");
        var response=tx().execute(s->workflow.previewRoute(99L,nativePreview()));assertTrue(response.getReady());
        assertEquals(List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW"),response.getNodes().stream().map(n->n.getStageCode()).toList());assertEquals(List.of(88L),response.getNodes().get(0).getResolvedUserIds());
        var proof=tokens.verifyRecordedNativeSupervisor(response.getSupervisorSourceDigest(),response.getSupervisorSourceToken());var evidence=(java.util.Map<?,?>)proof.get("evidence");assertEquals("SYSTEM_USER_MANAGEMENT",evidence.get("sourceSystem"));assertEquals("SYSTEM_USER_MANAGEMENT:DIRECT_SUPERVISOR:1:99",evidence.get("sourceIdentity"));assertEquals("3",evidence.get("sourceVersion"));
        assertEquals(originalFile,files.selectById(20L));assertEquals(originalAttributes,jdbc.queryForList("SELECT * FROM dcc_project_application_attributes ORDER BY id"));assertEquals(0L,routeSnapshots.selectCount(null));assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());verifyNoInteractions(organization);
    }
    @Test void missingUserManagementSettingRejectsPublicPrecheckBeforeAnyBpmOrSnapshot(){
        systemRelation.setSupervisorUserId(null);var original=files.selectById(20L);var response=tx().execute(s->workflow.previewRoute(99L,nativePreview()));assertFalse(response.getReady());assertEquals("DIRECT_SUPERVISOR_NOT_CONFIGURED",response.getBlockers().get(0).getReasonCode());assertTrue(response.getBlockers().get(0).getMessage().contains("请先在用户管理设置直属主管"));assertNull(response.getSupervisorSourceDigest());assertNull(response.getSupervisorSourceToken());assertEquals(original,files.selectById(20L));assertEquals(0L,routeSnapshots.selectCount(null));assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());verifyNoInteractions(organization);
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"RELATION","REQUESTER","SUPERVISOR"})
    void domainSourceFailuresAreAccurateBlockersRatherThanMatrixUnsupported(String bad){
        String expected;switch(bad){case "RELATION"->{systemRelation.setRelationVersion(-1L);expected="DIRECT_SUPERVISOR_RELATION_INVALID";}case "REQUESTER"->{systemRelation.setUserId(100L);expected="DIRECT_SUPERVISOR_RELATION_INVALID";}default->{doReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(88L).setTenantId(1L).setStatus(1)).when(systemUsers).getUser(88L);expected="DIRECT_SUPERVISOR_ACCOUNT_INVALID";}}
        var response=tx().execute(s->workflow.previewRoute(99L,nativePreview()));assertFalse(response.getReady());assertEquals(expected,response.getBlockers().get(0).getReasonCode());assertNull(response.getSupervisorSourceToken());assertEquals(0L,routeSnapshots.selectCount(null));
    }
    @Test void actualTechnicalSystemFailureRemainsAnException(){
        var failure=new IllegalStateException("actual system relation storage failure");doThrow(failure).when(systemUsers).getDirectSupervisorRelation(99L);assertSame(failure,assertThrows(IllegalStateException.class,()->tx().execute(s->workflow.previewRoute(99L,nativePreview()))));assertEquals(0L,routeSnapshots.selectCount(null));
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"INVALID","VERSION_INVALID"})
    void actualDeclaredSystemCodesBecomeRelationBlockersWithoutMatrixMisclassification(String bad){
        var error=new cn.iocoder.yudao.framework.common.exception.ServiceException("INVALID".equals(bad)?cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorErrors.INVALID:cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorErrors.VERSION_INVALID);doThrow(error).when(systemUsers).getDirectSupervisorRelation(99L);
        var result=tx().execute(s->workflow.previewRoute(99L,nativePreview()));assertFalse(result.getReady());assertEquals("DIRECT_SUPERVISOR_RELATION_INVALID",result.getBlockers().get(0).getReasonCode());assertNull(result.getSupervisorSourceToken());
    }
    @Test void changedPersistedRelationVersionChangesPublicProofEvenForTheSameSupervisor(){
        var original=files.selectById(20L);var first=tx().execute(s->workflow.previewRoute(99L,nativePreview()));systemRelation.setRelationVersion(4L);var second=tx().execute(s->workflow.previewRoute(99L,nativePreview()));assertTrue(first.getReady());assertTrue(second.getReady());assertEquals(first.getNodes(),second.getNodes());assertNotEquals(first.getSupervisorSourceDigest(),second.getSupervisorSourceDigest());assertEquals(original,files.selectById(20L));assertEquals(0L,routeSnapshots.selectCount(null));
    }
}
