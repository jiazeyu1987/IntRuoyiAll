package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionRetentionJobTest {
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final ProjectionRetentionService service=mock(ProjectionRetentionService.class);
    final Evidence evidence=mock(Evidence.class);
    final ProjectionRetentionJob job=new ProjectionRetentionJob(service,evidence,mapper);
    final Request request=new Request(2,"old",60L,90L,"reason","approval");
    @BeforeEach void setup() { when(evidence.configuredRequest(2,"old","approval")).thenReturn(request); }
    String command(Long tenant,String operation,Integer size,String hash) throws Exception {
        return mapper.writeValueAsString(new ProjectionRetentionJob.Command(operation,tenant,"old","approval",size,hash));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void independentJobHasNoFanoutAndRestoresPriorContextOnSuccess() throws Exception {
        assertNull(ProjectionRetentionJob.class.getMethod("execute",String.class).getAnnotation(TenantJob.class));
        TenantContextHolder.setTenantId(99L);TenantContextHolder.setIgnore(true);
        var result=new Retirement(request,State.RETIRED,1000,391000);
        when(service.retire(request)).thenAnswer(i->{assertEquals(2L,TenantContextHolder.getTenantId());assertFalse(TenantContextHolder.isIgnore());return result;});
        var output=mapper.readTree(job.execute(command(2L,"retire",null,null)));
        assertEquals("RETIRED",output.get("state").asText());assertEquals(391000,output.get("purgeNotBeforeMillis").asLong());
        assertEquals(99L,TenantContextHolder.getTenantId());assertTrue(TenantContextHolder.isIgnore());verify(service).retire(request);
    }
    @Test void failuresPropagateAndRestoreTenantInsteadOfReturningAnErrorString() throws Exception {
        TenantContextHolder.setTenantId(99L);TenantContextHolder.setIgnore(true);
        var failure=new IllegalStateException("approval or JDBC failed");when(service.retire(request)).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->job.execute(command(2L,"retire",null,null))));
        assertEquals(99L,TenantContextHolder.getTenantId());assertTrue(TenantContextHolder.isIgnore());
    }
    @Test void noAmbientTenantOrDefaultConfigurationCanAuthorizeMissingCommand() throws Exception {
        assertThrows(IllegalArgumentException.class,()->job.execute(command(null,"retire",null,null)));
        assertThrows(Exception.class,()->job.execute("{\"op\":\"retire\",\"tenant\":2,\"generation\":\"old\"}"));
        assertThrows(IllegalArgumentException.class,()->job.execute(command(2L,"retire",1,null)));verifyNoInteractions(service);
        when(evidence.configuredRequest(2,"old","approval")).thenThrow(new IllegalStateException("required durations missing"));
        assertThrows(IllegalStateException.class,()->job.execute(command(2L,"retire",null,null)));verifyNoInteractions(service);
    }
    @Test void maximumIdentityAndHashFitReal255CharacterSchedulerColumns() throws Exception {
        var command=new ProjectionRetentionJob.Command("purge",Long.MAX_VALUE,"g".repeat(64),"a".repeat(32),1000,"f".repeat(64));
        String json=mapper.writeValueAsString(command);assertTrue(json.length()<=255,json.length()+": "+json);
        assertThrows(IllegalArgumentException.class,()->job.execute(" ".repeat(256)));
    }
    Plan plan(Selection selection) {
        var retirement=new Retirement(request,State.RETIRED,1000,391000);
        var candidate=new Candidate(retirement,new Global(0,0,"r"),new Tenant(2,0,0,"active"),new Metadata(0,0,true,"r",ProjectionCodec.hashText("[]")),1000,selection);
        return new Plan(candidate,ProjectionCodec.hashText("plan"),selection.stage()!=Stage.METADATA);
    }
    @Test void previewAndPurgeReturnBoundedDigestWithoutKeysPayloadOrLargeCandidate() throws Exception {
        Row row=new Row(List.of("x".repeat(160),1L,2L),ProjectionCodec.hashText("row"),null);
        Plan plan=plan(new Selection(Stage.AUDIENCE,Collections.nCopies(1000,row)));
        when(service.preview(request,1000)).thenReturn(plan);when(service.purge(request,1000,plan.planHash())).thenReturn(plan);
        String preview=job.execute(command(2L,"purge-preview",1000,null));assertTrue(preview.length()<4000);assertFalse(preview.contains("x".repeat(160)));
        assertEquals(plan.planHash(),mapper.readTree(preview).get("planHash").asText());
        assertEquals(1000,mapper.readTree(preview).get("rows").asInt());
        job.execute(command(2L,"purge",1000,plan.planHash()));verify(service).purge(request,1000,plan.planHash());
        assertNull(TenantContextHolder.getTenantId());assertFalse(TenantContextHolder.isIgnore());
        Plan finished=plan(new Selection(Stage.METADATA,List.of()));
        when(service.purge(request,1000,finished.planHash())).thenReturn(finished);
        assertEquals("PURGED",mapper.readTree(job.execute(command(2L,"purge",1000,finished.planHash()))).get("state").asText());
    }
}
