package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionHistory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionHistoryJobTest {
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final ProjectionHistoryService service=mock(ProjectionHistoryService.class);final Evidence evidence=mock(Evidence.class);
    final ProjectionHistoryJob job=new ProjectionHistoryJob(service,evidence,mapper);
    final Request request=new Request(Target.OUTBOX,Scope.GLOBAL,0,null,1L,60L,90L,5L,"reason","approval",new Budget(100,1000,100000L,100));
    @BeforeEach void setup() {when(evidence.configuredRequest("approval")).thenReturn(request);}
    @AfterEach void clear() {TenantContextHolder.clear();}
    String command(String op,Scope scope,Long tenant,String hash) throws Exception {return mapper.writeValueAsString(new ProjectionHistoryJob.Command(op,scope,tenant,"approval",hash));}
    @Test void globalEntryIsExplicitNoFanoutAndRestoresTenantEvenAfterFailure() throws Exception {
        assertNull(ProjectionHistoryJob.class.getMethod("execute",String.class).getAnnotation(TenantJob.class));
        TenantContextHolder.setTenantId(99L);TenantContextHolder.setIgnore(false);
        doAnswer(i->{assertEquals(0L,TenantContextHolder.getTenantId());assertTrue(TenantContextHolder.isIgnore());return null;}).when(service).initialize(request);
        assertEquals("GLOBAL",mapper.readTree(job.execute(command("init",Scope.GLOBAL,0L,null))).get("scope").asText());
        assertEquals(99L,TenantContextHolder.getTenantId());assertFalse(TenantContextHolder.isIgnore());
        var failure=new IllegalStateException("failure");doThrow(failure).when(service).initialize(request);
        assertSame(failure,assertThrows(IllegalStateException.class,()->job.execute(command("init",Scope.GLOBAL,0L,null))));assertEquals(99L,TenantContextHolder.getTenantId());assertFalse(TenantContextHolder.isIgnore());
    }
    @Test void missingOrMismatchedScopeAndExtraParametersStopBeforeMaintenance() throws Exception {
        assertThrows(IllegalArgumentException.class,()->job.execute(command("init",null,0L,null)));
        assertThrows(IllegalArgumentException.class,()->job.execute(command("init",Scope.GLOBAL,null,null)));
        assertThrows(IllegalStateException.class,()->job.execute(command("init",Scope.TENANT,2L,null)));
        assertThrows(IllegalArgumentException.class,()->job.execute(command("observe",Scope.GLOBAL,0L,"a".repeat(64))));
        assertThrows(Exception.class,()->job.execute("{\"op\":\"init\",\"scope\":\"GLOBAL\",\"tenant\":0,\"ref\":\"approval\",\"path\":\"file\"}"));verifyNoInteractions(service);
    }
    @Test void largestCommandFitsSchedulerAndResultDoesNotExposePayloadOrApprovalContent() throws Exception {
        String json=mapper.writeValueAsString(new ProjectionHistoryJob.Command("register",Scope.TENANT,Long.MAX_VALUE,"a".repeat(32),"f".repeat(64)));assertTrue(json.length()<=255);
        assertThrows(IllegalArgumentException.class,()->job.execute(" ".repeat(256)));
        var item=new Item("OUTBOX",List.of("secret-key"),ProjectionCodec.hashText("event"),ProjectionCodec.hashText("row"),1,null,List.of());
        var snapshot=new Snapshot(new Global(1,0,"r"),List.of(),0,0,List.of(),1,"batch",ProjectionCodec.hashText("batch"),new Observation(1000,401000,60,90,5,"approval"),Collections.nCopies(1000,item));
        Plan plan=new Plan(new Candidate(request,snapshot),ProjectionCodec.hashText("plan"));when(service.preview(request)).thenReturn(plan);
        String result=job.execute(command("preview",Scope.GLOBAL,0L,null));assertTrue(result.length()<4000);assertFalse(result.contains("secret-key"));assertFalse(result.contains("reason"));
        assertEquals(1000,mapper.readTree(result).get("rows").asInt());assertEquals(plan.planHash(),mapper.readTree(result).get("planHash").asText());
    }
}
