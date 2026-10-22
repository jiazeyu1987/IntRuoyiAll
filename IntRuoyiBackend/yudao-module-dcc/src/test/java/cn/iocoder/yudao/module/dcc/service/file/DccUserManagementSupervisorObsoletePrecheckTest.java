package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserDirectSupervisorRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual obsolete FormCenter/round/attribute/H2 and production user-management producer.
 * Original open-draft19 cases stay complete; account/API/directory services are external task ports. */
class DccUserManagementSupervisorObsoletePrecheckTest extends DccObsoleteFormOpenDraftDatabaseTest {
    AnnotationConfigApplicationContext sourceContext;
    AdminUserDirectSupervisorRespDTO relation;
    @BeforeEach void productionSourceForOwnCase(TestInfo info)throws Exception{
        if(info.getTestMethod().orElseThrow().getDeclaringClass()!=DccUserManagementSupervisorObsoletePrecheckTest.class)return;
        currentMatrix(1);relation=new AdminUserDirectSupervisorRespDTO(1L,99L,88L,5L);when(users.getDirectSupervisorRelation(99L)).thenAnswer(ignored->relation);
        sourceContext=new AnnotationConfigApplicationContext();sourceContext.registerBean("adminUserApi",AdminUserApi.class,()->users);sourceContext.register(DccUserManagementDirectSupervisorSource.class);sourceContext.refresh();wire(source,"supervisors",sourceContext.getBeanProvider(DccDirectSupervisorSource.class));
    }
    @AfterEach void closeOwnSourceContext(){if(sourceContext!=null)sourceContext.close();}
    @Test void obsoletePreviewReportsTheActualUnsetUserManagementRelationWithoutPreparingAnything(){
        var form=open(request("OPEN-SAVE"));relation.setSupervisorUserId(null);var originalForms=jdbc.queryForList("SELECT * FROM bpm_form_action_instance ORDER BY id");var originalFile=files.selectById(20L);var result=phase(phaseRequest(form,"PREVIEW"));assertFalse(result.isPreparationReady());assertTrue(result.getUnavailableReason().contains("DIRECT_SUPERVISOR_NOT_CONFIGURED"));assertTrue(result.getUnavailableReason().contains("请先在用户管理设置直属主管"));assertEquals(originalForms,jdbc.queryForList("SELECT * FROM bpm_form_action_instance ORDER BY id"));assertEquals(originalFile,files.selectById(20L));
    }
    @Test void obsoletePreviewPreservesTechnicalDirectoryFailure(){
        var form=open(request("OPEN-SAVE"));var failure=new IllegalStateException("actual obsolete system relation reader failure");doThrow(failure).when(users).getDirectSupervisorRelation(99L);assertSame(failure,assertThrows(IllegalStateException.class,()->phase(phaseRequest(form,"PREVIEW"))));
    }
    @Test void obsoletePreparationUsesTheSameProductionRelationAndFreezesItsActualVersion()throws Exception{
        authoritativePreparationPorts();wire(source,"supervisors",sourceContext.getBeanProvider(DccDirectSupervisorSource.class));var form=open(request("OPEN-SAVE"));var result=phase(prepareRequest(form));assertTrue(result.isPreparationReady());var saved=forms.selectById(form.getId());var data=JsonUtils.parseObject(saved.getFormDataJson(),Map.class);var prepared=(Map<?,?>)data.get("dccExecutionPreparation");var evidence=(Map<?,?>)prepared.get("supervisorProvenance");assertEquals("SYSTEM_USER_MANAGEMENT",evidence.get("sourceSystem"));assertEquals("88",evidence.get("supervisorUserId"));assertEquals("5",evidence.get("sourceVersion"));assertEquals("DRAFT",saved.getStatus());assertNull(saved.getBpmProcessInstanceId());assertEquals(0,preparationEngine.getRuntimeService().createProcessInstanceQuery().count());
    }
    @Test void savedPreparedDraftGetAndPlainSaveNeverReadTodaysRelationButExplicitPreviewDoes()throws Exception{
        authoritativePreparationPorts();wire(source,"supervisors",sourceContext.getBeanProvider(DccDirectSupervisorSource.class));
        var form=open(request("OPEN-SAVE"));var prepared=phase(prepareRequest(form));assertTrue(prepared.isPreparationReady());
        var savedProof=JsonUtils.parseObject(forms.selectById(form.getId()).getFormDataJson(),Map.class).get("dccExecutionPreparation");
        var savedAttributes=attributesRows.find("OBSOLETE",20L,ownRound(form));var originalFile=files.selectById(20L);
        var failure=new IllegalStateException("today user-management directory unavailable after preparation");clearInvocations(users);doThrow(failure).when(users).getDirectSupervisorRelation(99L);
        var reopened=getDraft();assertTrue(reopened.isPreparationReady());assertNull(reopened.getUnavailableReason());
        var plain=phaseRequest(form,"SAVE");plain.setPrepareExecution(false);assertTrue(phase(plain).isPreparationReady());
        verify(users,never()).getDirectSupervisorRelation(org.mockito.ArgumentMatchers.anyLong());
        assertEquals(savedProof,JsonUtils.parseObject(forms.selectById(form.getId()).getFormDataJson(),Map.class).get("dccExecutionPreparation"));assertEquals(savedAttributes,attributesRows.find("OBSOLETE",20L,ownRound(form)));assertEquals(originalFile,files.selectById(20L));
        var formBeforePreview=forms.selectById(form.getId());assertSame(failure,assertThrows(IllegalStateException.class,()->phase(phaseRequest(form,"PREVIEW"))));verify(users,times(1)).getDirectSupervisorRelation(99L);assertEquals(formBeforePreview,forms.selectById(form.getId()));
    }
}
