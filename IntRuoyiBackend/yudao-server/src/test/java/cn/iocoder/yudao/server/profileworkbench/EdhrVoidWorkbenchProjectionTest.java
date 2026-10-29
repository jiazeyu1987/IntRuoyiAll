package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.bpm.workbench.*;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalFlowableTaskQueryService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.workbench.index.ProjectionExportSupport;
import cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.flowable.common.engine.api.delegate.event.*;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdhrVoidWorkbenchProjectionTest {
    private Path backend() {
        Path p=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while(p!=null){if(Files.isDirectory(p.resolve("IntRuoyiBackend")))return p.resolve("IntRuoyiBackend");if(p.getFileName().toString().equals("IntRuoyiBackend"))return p;p=p.getParent();}
        throw new IllegalStateException("Production XML missing");
    }
    private Configuration configuration() throws Exception {
        var c=new Configuration();
        for(String relative:List.of("yudao-module-system/src/main/resources/mapper/profileworkbench/WorkbenchQuerySql.xml",
                "yudao-module-bpm/src/main/resources/mapper/approval/ApprovalFlowableTaskQueryMapper.xml",
                "yudao-module-bpm/src/main/resources/mapper/workbench/EdhrVoidWorkbenchMapper.xml")) {
            var p=backend().resolve(relative);
            try(var in=Files.newInputStream(p)){new XMLMapperBuilder(in,c,p.toString(),c.getSqlFragments()).parse();}
        }
        return c;
    }
    private Map<String,Object> parameters(boolean global) {
        Map<String,Object> p=new HashMap<>();p.put("tenant",2L);p.put("tenantText","2");p.put("userId",3L);p.put("userText","3");
        p.put("done",false);p.put("global",global);p.put("edhrVoidOnly",true);p.put("candidateGroups",List.of("actual-engine-group"));
        p.put("registrationUpload",false);p.put("registrationDownload",false);p.put("q",ProjectionExportSupport.query(2));return p;
    }
    @Test void allQueryFormsRetainFormalVoidAndExactTenantBoundaries() throws Exception {
        var c=configuration();var p=parameters(false);p.put("fetchLimit",101);
        for(String name:List.of("countBrokenDomain","count","chunk","rows")) {
            var statement=c.getMappedStatement("cn.iocoder.yudao.module.bpm.workbench.EdhrVoidWorkbenchMapper."+name);
            assertEquals(10,statement.getTimeout(),name);
            String sql=statement.getBoundSql(p).getSql().replaceAll("\\s+"," ");
            assertTrue(sql.contains("mes-edhr-batch-execution-void-v1"));assertTrue(sql.contains("EDHR_BATCH_EXECUTION_VOID"));
            assertTrue(sql.contains("br.request_status='PENDING_BPM'"));assertTrue(sql.contains("e.change_status='SUBMITTED'"));
            assertTrue(sql.contains("CAST(t.TENANT_ID_ AS BINARY)"));assertTrue(sql.contains("CAST(p.TENANT_ID_ AS BINARY)"));
            assertTrue(sql.contains("CAST(d.TENANT_ID_ AS BINARY)"));assertTrue(sql.contains("il.TYPE_='candidate'"));
            // Identifier joins must be independent of the MySQL connection collation.
            assertTrue(sql.contains("CAST(CAST(b.id AS CHAR) AS BINARY)=CAST(br.object_id AS BINARY)"), name);
            assertTrue(sql.contains("CAST(CAST(br.id AS CHAR) AS BINARY)=CAST(o.authority_id AS BINARY)"), name);
            assertTrue(sql.contains("b.tenant_id=br.tenant_id"));
            assertTrue(sql.contains("t.ASSIGNEE_=?"));
            assertFalse(sql.contains("${"));
            net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql);
        }
    }
    @Test void definitionIdentityAndReusedMandatoryProvenanceQueriesHaveTenSecondTimeouts() throws Exception {
        var c=configuration();
        assertEquals(10,EdhrVoidWorkbenchMapper.class.getMethod("countVoidDefinition",String.class,String.class)
            .getAnnotation(org.apache.ibatis.annotations.Options.class).timeout());
        for(String name:List.of("countBrokenProvenance","ownerships"))
            assertEquals(10,c.getMappedStatement("cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper."+name).getTimeout(),name);
    }
    @Test void identityRecheckKeepsEngineOwnerAndLocksCurrentTask() throws Exception {
        var c=configuration();var p=parameters(false);p.put("taskId","engine-task");p.put("lockIdentity",true);
        var sql=c.getMappedStatement("cn.iocoder.yudao.module.bpm.workbench.EdhrVoidWorkbenchMapper.rows").getBoundSql(p).getSql();
        assertTrue(sql.contains("t.ID_=?"));assertTrue(sql.contains("t.ASSIGNEE_=?"));assertTrue(sql.contains("FOR UPDATE"));
        assertTrue(sql.contains("ACT_RU_IDENTITYLINK"));
    }
    @Test void brokenFormalRelationFailsInsteadOfBecomingEmptySuccess() {
        var mapper=mock(EdhrVoidWorkbenchMapper.class);var queries=mock(ApprovalFlowableTaskQueryService.class);
        when(queries.edhrVoidWorkbenchParameters(2,1,false)).thenReturn(parameters(false));when(mapper.countBrokenDomain(anyMap())).thenReturn(1L);
        var permissions=mock(PermissionApi.class);when(permissions.hasAnyPermissions(anyLong(),any(String.class))).thenReturn(true);
        var source=new EdhrVoidWorkbenchSource(mapper,queries,permissions);
        assertThrows(IllegalStateException.class,()->source.count(ProjectionExportSupport.query(2)));
        verify(mapper,never()).count(anyMap());
    }
    @Test void missingPageOrReviewPermissionDeniesSourceAndIdentityRecheckBeforeReadingTasks() {
        for(String missing:List.of("bpm:process-instance:query","bpm:task:update")) {
            var mapper=mock(EdhrVoidWorkbenchMapper.class);var queries=mock(ApprovalFlowableTaskQueryService.class);var permissions=mock(PermissionApi.class);
            for(String permission:EdhrVoidWorkbenchSource.REQUIRED_PERMISSIONS)
                when(permissions.hasAnyPermissions(1L,permission)).thenReturn(!permission.equals(missing));
            var source=new EdhrVoidWorkbenchSource(mapper,queries,permissions);var q=ProjectionExportSupport.query(2);
            assertFalse(source.isAuthorized(1,2));
            assertThrows(RuntimeException.class,()->source.count(q));
            assertThrows(RuntimeException.class,()->source.resolveVisibilityIdentity(q,"task"));
            verifyNoInteractions(mapper,queries);
        }
    }
    @Test void projectEngineConfigurationRegistersTheComponentListener() {
        var listener=new ProjectionEdhrVoidEngineListener(mock(EdhrVoidWorkbenchMapper.class));
        @SuppressWarnings("unchecked") org.springframework.beans.factory.ObjectProvider<org.flowable.common.engine.api.delegate.event.FlowableEventListener> listeners=mock(org.springframework.beans.factory.ObjectProvider.class);
        @SuppressWarnings("unchecked") org.springframework.beans.factory.ObjectProvider<org.flowable.common.engine.api.delegate.FlowableFunctionDelegate> functions=mock(org.springframework.beans.factory.ObjectProvider.class);
        when(listeners.iterator()).thenReturn(List.<org.flowable.common.engine.api.delegate.event.FlowableEventListener>of(listener).iterator());
        when(functions.stream()).thenReturn(java.util.stream.Stream.empty());
        var config=mock(org.flowable.spring.SpringProcessEngineConfiguration.class);
        new cn.iocoder.yudao.module.bpm.framework.flowable.config.BpmFlowableConfiguration()
            .bpmProcessEngineConfigurationConfigurer(listeners,functions,mock(cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory.class)).configure(config);
        verify(config).setEventListeners(List.of(listener));
        assertTrue(ProjectionEdhrVoidEngineListener.class.isAnnotationPresent(org.springframework.stereotype.Component.class));
    }
    @Test void candidateGroupReadDuringExportsNeverReentersTheWriteFence() {
        var fence=mock(ProjectionWriteBoundaryAspect.class);
        var config=mock(org.flowable.spring.SpringProcessEngineConfiguration.class);
        new ProjectionFlowableConfiguration().projectionEngineFence(fence).configure(config);
        @SuppressWarnings("rawtypes") var captor=org.mockito.ArgumentCaptor.forClass(List.class);
        verify(config).setCustomPreCommandInterceptors(captor.capture());
        var interceptor=(org.flowable.common.engine.impl.interceptor.CommandInterceptor)captor.getValue().get(0);
        var next=mock(org.flowable.common.engine.impl.interceptor.CommandInterceptor.class);interceptor.setNext(next);
        var command=new ApprovalFlowableTaskQueryService.CandidateGroupsRead(mock(org.flowable.task.service.impl.TaskQueryImpl.class));
        when(next.execute(any(),same(command),any())).thenReturn(Set.of("engine-group"));
        assertEquals(Set.of("engine-group"),interceptor.execute(null,command,null));
        verifyNoInteractions(fence);
    }
    @Test void exportUsesTheSameEngineResponsibilityForEachTenantPermissionHolder() {
        var mapper=mock(EdhrVoidWorkbenchMapper.class);var queries=mock(ApprovalFlowableTaskQueryService.class);
        when(queries.edhrVoidWorkbenchParameters(anyLong(),anyLong(),anyBoolean())).thenAnswer(call->{
            var p=parameters(call.getArgument(2));p.put("actor",call.getArgument(1));return p;
        });
        var row=new EdhrVoidWorkbenchRow();row.setSourceId("BPM_EDHR_VOID");row.setBusinessId("task");row.setTaskKey("BPM_EDHR_VOID:task");
        row.setRowOrderKey("task");row.setTaskType("批记录");row.setSource("批次作废审批");row.setDetail("正式作废");row.setStatusLabel("待审批");row.setProcessInstanceId("process");
        when(mapper.rows(anyMap())).thenAnswer(call->{Map<String,Object> p=call.getArgument(0);
            return Boolean.TRUE.equals(p.get("global"))||Objects.equals(p.get("actor"),3L)?List.of(row):List.of();
        });
        var source=new EdhrVoidWorkbenchSource(mapper,queries,mock(PermissionApi.class));
        var authority=mock(cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport.class);
        when(authority.exportGrants(eq(2L),any())).thenReturn(List.of(
            new cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport.Grant(3,"permission",java.time.Instant.EPOCH,null),
            new cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport.Grant(4,"permission",java.time.Instant.EPOCH,null)));
        var exporter=new EdhrVoidProjectionExport(source,mapper,authority);
        var tasks=exporter.exportTasks(2,new ProjectionModel.Watermark(0,0));
        assertEquals(1,tasks.size());assertEquals(Set.of(3L),tasks.get(0).audience().members());
        assertEquals("task",tasks.get(0).action().payload().get("taskId"));
        verify(queries).edhrVoidWorkbenchParameters(2,3,false);verify(queries).edhrVoidWorkbenchParameters(2,4,false);
    }
    @Test void missingPageGrantProducesNeitherIndexedTaskNorSourceAuthorization() {
        var mapper=mock(EdhrVoidWorkbenchMapper.class);var queries=mock(ApprovalFlowableTaskQueryService.class);
        when(queries.edhrVoidWorkbenchParameters(anyLong(),anyLong(),anyBoolean())).thenAnswer(call->parameters(call.getArgument(2)));
        var row=new EdhrVoidWorkbenchRow();row.setTaskKey("BPM_EDHR_VOID:task");
        when(mapper.rows(anyMap())).thenReturn(List.of(row));
        var source=new EdhrVoidWorkbenchSource(mapper,queries,mock(PermissionApi.class));
        var authority=mock(cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport.class);
        when(authority.exportGrants(eq(2L),any())).thenAnswer(call->{
            cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport.Requirement requirement=call.getArgument(1);
            return requirement.values().contains("bpm:process-instance:query")?List.of():List.of(
                new cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport.Grant(3,"permission",java.time.Instant.EPOCH,null));
        });
        var exporter=new EdhrVoidProjectionExport(source,mapper,authority);var watermark=new ProjectionModel.Watermark(0,0);
        assertTrue(exporter.exportTasks(2,watermark).isEmpty());
        assertTrue(exporter.exportAuthorizations(2,watermark).isEmpty());
        verify(queries,never()).edhrVoidWorkbenchParameters(2,3,false);
    }
    @Test void requiredPermissionGrantIntervalsAreIntersectedWithoutExtendingTemporaryAccess() {
        var mapper=mock(EdhrVoidWorkbenchMapper.class);var queries=mock(ApprovalFlowableTaskQueryService.class);
        when(queries.edhrVoidWorkbenchParameters(anyLong(),anyLong(),anyBoolean())).thenAnswer(call->parameters(call.getArgument(2)));
        when(mapper.rows(anyMap())).thenReturn(List.of());
        var source=new EdhrVoidWorkbenchSource(mapper,queries,mock(PermissionApi.class));
        var authority=mock(cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport.class);
        when(authority.exportGrants(eq(2L),any())).thenAnswer(call->{
            cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport.Requirement requirement=call.getArgument(1);
            String permission=requirement.values().iterator().next();
            long start=permission.equals("bpm:process-instance:query")?10:0;
            long end=permission.equals("bpm:task:update")?20:30;
            return List.of(new cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchAuthorityExport.Grant(3,permission,java.time.Instant.ofEpochSecond(start),java.time.Instant.ofEpochSecond(end)));
        });
        var exporter=new EdhrVoidProjectionExport(source,mapper,authority);
        var grants=exporter.exportAuthorizations(2,new ProjectionModel.Watermark(0,0));
        assertEquals(1,grants.size());assertEquals(java.time.Instant.ofEpochSecond(10),grants.get(0).validFrom());
        assertEquals(java.time.Instant.ofEpochSecond(20),grants.get(0).validUntil());
    }
    @Test void assignmentAndSuspensionEventsDirtyOnlyVoidTasksAndRequireTenantFence() {
        var mapper=mock(EdhrVoidWorkbenchMapper.class);var listener=new ProjectionEdhrVoidEngineListener(mapper);
        var task=mock(Task.class);when(task.getTenantId()).thenReturn("2");when(task.getProcessDefinitionId()).thenReturn("definition");
        var event=mock(FlowableEngineEntityEvent.class);when(event.getEntity()).thenReturn(task);when(event.getType()).thenReturn(FlowableEngineEventType.TASK_ASSIGNED);
        var held=new ProjectionWriteContext(ProjectionModel.Scope.TENANT,2,null);ProjectionWriteContext.CURRENT.set(held);
        try {
            listener.onEvent(event);assertFalse(held.changed);
            when(mapper.countVoidDefinition("2","definition")).thenReturn(1L);
            listener.onEvent(event);assertTrue(held.changed);
            held.changed=false;when(event.getType()).thenReturn(FlowableEngineEventType.ENTITY_SUSPENDED);
            listener.onEvent(event);assertTrue(held.changed);
            when(task.getTenantId()).thenReturn("3");when(mapper.countVoidDefinition("3","definition")).thenReturn(1L);
            assertThrows(IllegalStateException.class,()->listener.onEvent(event));assertTrue(held.failed);
        } finally {ProjectionWriteContext.CURRENT.remove();}
        when(task.getTenantId()).thenReturn("2");assertThrows(IllegalStateException.class,()->listener.onEvent(event));
    }
    @Test void exportCompleteQueriesForMysqlReadOnlyValidation() throws Exception {
        var c=configuration();var queries=new ArrayList<String>();
        for(boolean done:List.of(false,true)) for(boolean global:List.of(false,true)) for(boolean voidOnly:List.of(false,true)) for(String projection:List.of("BPM","DCC_TASK")) for(boolean filtered:List.of(false,true)) {
            var p=parameters(global);p.put("edhrVoidOnly",voidOnly);p.put("done",done);p.put("tenant",1L);p.put("tenantText","1");
            p.put("userId",1L);p.put("userText","1");p.put("projection",projection);p.put("keyword",filtered?"%test%":null);
            p.put("limit",10);p.put("offset",0);p.put("fetchLimit",101);p.put("after",null);
            for(String id:List.of("countBrokenProvenance","ownerships","count","window")) {
                var bound=c.getMappedStatement("cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper."+id).getBoundSql(p);
                String sql=bound.getSql().replaceAll("\\s+"," ");
                for(var mapping:bound.getParameterMappings()) {
                    String key=mapping.getProperty();
                    Object value=bound.hasAdditionalParameter(key)?bound.getAdditionalParameter(key):org.apache.ibatis.reflection.SystemMetaObject.forObject(p).getValue(key);
                    String literal=value==null?"NULL":value instanceof Number?value.toString():value instanceof Boolean?((Boolean)value?"1":"0"):"'"+value.toString().replace("'","''")+"'";
                    sql=sql.replaceFirst("\\?",java.util.regex.Matcher.quoteReplacement(literal));
                }
                queries.add(sql);
            }
        }
        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target/approval-mysql-queries.sql"),queries);
    }
}
