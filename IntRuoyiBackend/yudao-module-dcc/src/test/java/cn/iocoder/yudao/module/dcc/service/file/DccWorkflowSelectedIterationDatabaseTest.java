package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.service.upload.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** H09: actual Query/A initializer/B/SQL/body ownership, Spring transactions and real V4 Flowable.
 * Account/category/storage/route readiness and platform integration are explicit external test ports.
 * No initializer, snapshot, round, lifecycle Mapper, version allocation or body ownership is mocked. */
@Import({DccWorkingApplicationDraftInitializer.class,DccApplicationRoundService.class,
        DccProjectApplicationSnapshotService.class,DccProjectAttributesService.class,
        DccControlledFileAccessAuditService.class,DccWorkflowSelectedIterationDatabaseTest.Beans.class})
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_a_selected_iteration;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;LOCK_TIMEOUT=5000")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DccWorkflowSelectedIterationDatabaseTest extends BaseDbUnitTest {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper g25Reservations;

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class Beans {
        @org.springframework.context.annotation.Bean JdbcTemplate jdbc(DataSource source){return new JdbcTemplate(source);}
    }
    @Resource DccWorkingApplicationDraftInitializer initializer;
    @Resource DccProjectApplicationSnapshotService snapshots;
    @Resource DccProjectAttributesService attributes;
    @Resource DccApplicationRoundService rounds;
    @Resource DccControlledFileAccessAuditService audit;
    @Resource(name="dccControlledFileMapper") DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccControlledFileCheckoutMapper checkouts;
    @Resource DccControlledFileNameClaimMapper claims;
    @Resource DccControlledFileSourceOwnershipMapper ownership;
    @Resource DccControlledFileRelatedFileMapper relations;
    @Resource DccControlledFileRouteSnapshotMapper routes;
    @Resource DccControlledFileTaskAssigneeSnapshotMapper obligations;
    @MockitoSpyBean DccProjectCodeMapper projects;
    @Resource DccProjectApplicationAttributesMapper attributeRows;
    @Resource DccProjectCodeAssignmentFileMapper assignmentFiles;
    @Resource DccControlledFileDistributionRecipientMapper recipients;
    @Resource FileMapper infraFiles;
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager manager;
    @Resource org.mybatis.spring.SqlSessionTemplate sql;
    @MockitoBean DccProjectAccessService access;
    @MockitoBean DccControlledFileCategoryPermissionSupport permissions;
    JdbcTemplate jdbc;
    DccControlledFileQueryServiceImpl query;
    DccControlledFileWorkflowServiceImpl workflow;
    DccControlledFileRevisionServiceImpl revisions;
    DccControlledFileSourceOwnershipService bodies;
    DccControlledFileRelatedFileServiceImpl related;
    DccControlledFileAssignmentScopeService scope;
    FileService storage;
    ProcessEngine engine;
    final Map<String,byte[]> bytes=new ConcurrentHashMap<>();
    final Map<Long,String> bodyPaths=new ConcurrentHashMap<>();
    final AtomicLong bodyIds=new AtomicLong(1000);
    final DccProjectAttributes nmpa=attrs("NMPA"),ce=attrs("CE"),fda=attrs("FDA");
    final DccControlledFileVersionPolicy policy=DccControlledFileVersionPolicy.defaultPolicy();
    static DccProjectAttributes attrs(String market){return new DccProjectAttributes(List.of(market),null,"Y","N","N",null);}
    TransactionTemplate tx(){return new TransactionTemplate(manager);}
    static void wire(Object bean,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(bean,(String)pairs[i],pairs[i+1]);}
    static void defaults(Object bean){for(var f:bean.getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())
            &&(f.getAnnotation(Resource.class)!=null || f.getAnnotation(org.springframework.beans.factory.annotation.Autowired.class)!=null))
        ReflectionTestUtils.setField(bean,f.getName(),mock(f.getType()));}

    @BeforeEach void seed() throws Exception {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);
        try(var connection=dataSource.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_application_round_link(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,project_id BIGINT,application_type VARCHAR(16),application_id BIGINT,bpm_round VARCHAR(64),attribute_round INT,UNIQUE(tenant_id,application_type,application_id,bpm_round),UNIQUE(tenant_id,application_type,application_id,attribute_round))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS infra_file(id BIGINT PRIMARY KEY,config_id BIGINT,name VARCHAR(256),path VARCHAR(1024),url VARCHAR(1024),type VARCHAR(128),size BIGINT,creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0)");
        jdbc.update("DELETE FROM dcc_application_round_link");jdbc.update("DELETE FROM dcc_controlled_file_source_ownership");jdbc.update("DELETE FROM dcc_controlled_file_related_file");jdbc.update("DELETE FROM infra_file");bytes.clear();bodyPaths.clear();
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status,default_attributes_json,deleted) VALUES(5,1,'C-H08','C-H08','ENABLE',?,0)",attributes.encode(nmpa));
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,status,deleted) VALUES(10,1,2,3,'SOP','N-1',5,6,'N-1','ACTIVE_CHAIN',0)");
        var stored=new FileDO();stored.setId(100L);stored.setConfigId(1L);stored.setName("SOP.pdf");stored.setPath("original/SOP.pdf");stored.setUrl("isolated-test");stored.setType("application/pdf");stored.setSize(4L);infraFiles.insert(stored);bytes.put(stored.getPath(),new byte[]{1,2,3,4});
        storage=mock(FileService.class);
        when(storage.getFileContent(anyLong(),anyString())).thenAnswer(c->bytes.get(c.getArgument(1)));
        when(storage.createFileAndReturnId(any(byte[].class),anyString(),anyString(),anyString())).thenAnswer(c->{
            long id=bodyIds.incrementAndGet();String path=c.getArgument(2);byte[] data=c.getArgument(0);
            var row=new FileDO();row.setId(id);row.setConfigId(1L);row.setName(c.getArgument(1));row.setPath(path);row.setUrl("isolated-test");row.setType(c.getArgument(3));row.setSize((long)data.length);infraFiles.insert(row);bytes.put(path,data.clone());bodyPaths.put(id,path);return id;
        });
        doAnswer(c->{Long id=c.getArgument(0);String path=bodyPaths.remove(id);if(path!=null)bytes.remove(path);infraFiles.deleteById(id);return null;}).when(storage).deleteFile(anyLong());
        bodies=new DccControlledFileSourceOwnershipService();wire(bodies,"controlledFileMapper",files,"ownershipMapper",ownership,"fileMapper",infraFiles,"fileService",storage);
        var configs=mock(cn.iocoder.yudao.module.infra.service.file.FileConfigService.class);
        var client=mock(cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient.class);
        when(configs.getFileClient(1L)).thenReturn(client);
        doAnswer(c->{String path=c.getArgument(0);bytes.remove(path);bodyPaths.entrySet().removeIf(e->e.getValue().equals(path));return null;}).when(client).delete(anyString());
        wire(bodies,"fileConfigService",configs);
        files.insert(working(20L,null,"A/1"));
        when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        scope=new DccControlledFileAssignmentScopeService();defaults(scope);wire(scope,"assignmentFileMapper",assignmentFiles,"distributionRecipientMapper",recipients);
        related=new DccControlledFileRelatedFileServiceImpl();defaults(related);wire(related,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"relatedFileMapper",relations);
        var names=new DccControlledFileNameClaimService();org.springframework.test.util.ReflectionTestUtils.setField(names,"reservationMapper",g25Reservations);wire(names,"claimMapper",claims,"masterMapper",masters,"fileMapper",files);
        query=new DccControlledFileQueryServiceImpl();defaults(query);
        wire(query,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"projectCodeMapper",projects,
            "checkoutMapper",checkouts,"versionPolicy",policy,"sourceOwnershipMapper",ownership,"sourceOwnershipService",bodies,
            "fileMapper",infraFiles,"relatedFileService",related,"permissionSupport",permissions,"projectAccessService",access,
            "assignmentScopeService",scope,"nameClaimService",names,"lifecycleAuditService",audit);
        // Reflection permits the pre-hook RED to compile without replacing the actual initializer by a mock.
        if(Arrays.stream(query.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("workingApplicationDraftInitializer")))
            wire(query,"workingApplicationDraftInitializer",initializer);
        var directory=mock(DccDirectoryAccessPermissionService.class);when(directory.hasDirectoryManagementPermission(anyLong())).thenReturn(true);
        wire(query,"directoryAccessPermissionService",directory);
        revisions=new DccControlledFileRevisionServiceImpl();defaults(revisions);
        var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Singapore");
        wire(revisions,"fileMapper",files,"masterMapper",masters,"checkoutMapper",checkouts,"projectAccessService",access,
            "permissionSupport",permissions,"sourceOwnershipService",bodies,"relatedFileService",related,"nameClaimService",names,
            "versionPolicy",policy,"assignmentScopeService",scope,"datePolicy",dates);
        workflow=new DccControlledFileWorkflowServiceImpl();defaults(workflow);
        wire(workflow,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"projectCodeMapper",projects,"checkoutMapper",checkouts,
            "revisionService",revisions,"applicationRoundService",rounds,"projectApplicationSnapshots",snapshots,"projectAttributesService",attributes,
            "workingApplicationDraftInitializer",initializer,"versionPolicy",policy,"workflowDatePolicy",dates,"projectAccessService",access,
            "categoryPermissionSupport",permissions,"routeSnapshotMapper",routes,"taskAssigneeSnapshotMapper",obligations,"assignmentScopeService",scope);
        var readiness=mock(DccControlledFileRouteReadinessService.class);
        when(readiness.evaluateDepartments(any(),any(),any(),any())).thenAnswer(c->{
            var route=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(DccCategoryApprovalRouteDO.builder().id(30L).versionNo(1).actionType(c.getArgument(3)).build(),
                List.of(node(1,"MATRIX_REVIEW","DEPT",51L),node(2,"MATRIX_APPROVAL","USER",99L),node(3,"DOC_CONTROL_REVIEW","USER",99L)));
            return new DccControlledFileRouteReadinessService.RouteReadinessEvaluation(route,DccControlledFileRouteReadinessRespVO.builder().ready(true).build());
        });
        var dept=mock(DeptApi.class);when(dept.getDeptList(any())).thenReturn(List.of(new DeptRespDTO().setId(51L).setName("质量部").setLeaderUserId(99L)));
        var users=mock(AdminUserApi.class);when(users.getUserList(any())).thenReturn(List.of(new AdminUserRespDTO().setId(99L).setNickname("真实测试申请人")));
        wire(workflow,"routeReadinessService",readiness,"approvalRouteAssigneeResolver",new DccControlledFileApprovalRouteAssigneeResolver(),"deptApi",dept,"adminUserApi",users);
        if(engine==null){
            var candidatePort=mock(BpmTaskCandidateInvoker.class);when(candidatePort.calculateUserListByTask(any())).thenReturn(List.of(99L));when(candidatePort.calculateUsersByTask(any())).thenReturn(Set.of(99L));
            var factory=new BpmActivityBehaviorFactory();factory.setTaskCandidateInvoker(candidatePort);
            var cfg=new SpringProcessEngineConfiguration();cfg.setDataSource(dataSource);cfg.setTransactionManager(manager);cfg.setDatabaseSchemaUpdate("true");cfg.setAsyncExecutorActivate(false);cfg.setDisableIdmEngine(true);cfg.setActivityBehaviorFactory(factory);
            // Flowable's H2 DDL uses the legacy IDENTITY type. Only engine schema installation uses LEGACY;
            // all DCC business transactions execute in the repository's explicit MYSQL-mode H2 fixture.
            jdbc.execute("SET MODE LEGACY");try{engine=cfg.buildProcessEngine();}finally{jdbc.execute("SET MODE MYSQL");}
            String text=Files.readString(Path.of("../sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql"));
            for(String type:List.of("upload","revision")){String marker="SET @dcc_"+type+"_bpmn = '";int start=text.indexOf(marker)+marker.length();String xml=text.substring(start,text.indexOf("';",start));engine.getRepositoryService().createDeployment().tenantId("1").addString(type+".bpmn20.xml",xml).deploy();}
        }
        var bpm=mock(BpmProcessInstanceApi.class);
        when(bpm.createProcessInstance(anyLong(),any())).thenAnswer(c->{BpmProcessInstanceCreateReqDTO req=c.getArgument(1);engine.getIdentityService().setAuthenticatedUserId(c.getArgument(0).toString());try{return engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(req.getProcessDefinitionKey(),req.getBusinessKey(),req.getVariables(),"1").getId();}finally{engine.getIdentityService().setAuthenticatedUserId(null);}});
        wire(workflow,"bpmProcessInstanceApi",bpm);
        query=transactionProxy(query);workflow=transactionProxy(workflow);
    }
    @SuppressWarnings("unchecked") <T> T transactionProxy(T bean){var factory=new org.springframework.aop.framework.ProxyFactory(bean);factory.setProxyTargetClass(true);factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));return (T)factory.getProxy();}
    @AfterEach void cleanup(){if(engine!=null)for(var p:engine.getRuntimeService().createProcessInstanceQuery().list())engine.getRuntimeService().deleteProcessInstance(p.getId(),"isolated C test cleanup");jdbc.update("DELETE FROM dcc_application_round_link");TenantContextHolder.clear();}
    @AfterAll void close(){if(engine!=null)engine.close();}
    DccControlledFileDO working(Long id,Long predecessor,String version){return DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L)
        .dccProjectCodeId(5L).fileTypeTaxonomyId(6L).fileNumber("N-1").fileName("SOP").title("SOP").sourceOriginalFileName("SOP.pdf")
        .sourceFileId(100L).originalFileId(100L).sourceSha256(bodies.inspectSource(100L).sourceSha256()).requesterId(99L).submitterId(99L)
        .versionNo(version).changeType("NEW").processType("CONTROLLED_FILE").status("WORKING").predecessorControlledFileId(predecessor).build();}
    DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node(int stage,String code,String source,Long sourceId){return new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(stage,code,code,stage,source,sourceId,List.of(sourceId),stage==1?"ALL":"ANY",stage==1?100:null,true,List.of(99L));}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    void initial(){tx().executeWithoutResult(s->initializer.initialize(99L,null,20L,ce));}
    void changeDefault(){jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(fda));}
    void checkout(Long id){var r=new DccControlledFileCheckoutReqVO();r.setReason("C actual edit");tx().execute(s->query.checkoutControlledFile(99L,id,r));}
    DccControlledFileCheckinReqVO edit(String remark){var r=new DccControlledFileCheckinReqVO();r.setRemark(remark);r.setChangeDescription("C real checkin "+remark);return r;}
    Long checkin(Long id,String remark){checkout(id);return tx().execute(s->query.checkinControlledFile(99L,id,edit(remark))).getId();}
    Long checkinDistinctBody(Long id,String remark,byte[] body) throws Exception {
        checkout(id);
        Long upload=storage.createFileAndReturnId(body,"SOP.pdf","selected-test-upload/"+remark,"application/pdf");
        var req=edit(remark);req.setUploadTicket("selected-ticket-"+remark);
        req.setSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.checkinPrefix(id),remark));
        var tickets=mock(DccUploadTicketService.class);
        when(tickets.resolveForBinding(any())).thenReturn(new DccUploadTicketBoundFile(req.getUploadTicket(),upload,"SOP.pdf","application/pdf",(long)body.length));
        wire(query,"uploadTicketService",tickets);
        return tx().execute(s->query.checkinControlledFile(99L,id,req)).getId();
    }
    DccControlledFileSubmitIterationReqVO request(String intent){var r=new DccControlledFileSubmitIterationReqVO();r.setRevisionChangeType(intent);r.setIdempotencyKey("C-"+UUID.randomUUID());r.setChangeDescription("C selected actual body");r.setNeedTraining(false);r.setEffectiveDate(LocalDate.of(2099,12,20));r.setProjectAttributes(ce);r.setSelectedSignoffDepartmentIds(List.of(51L));return r;}
    void controlled(String version){jdbc.update("UPDATE dcc_controlled_file SET version_no=?,status='ACTIVE',controlled_time=?,revision_change_type='INITIAL' WHERE id=20",version,LocalDateTime.of(2026,10,1,12,0));jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=20 WHERE id=10");}

    @Test void actualInitializerProxyRequiresOuterTransaction(){assertThrows(IllegalTransactionStateException.class,()->initializer.initialize(99L,null,20L,ce));assertEquals(0,count("dcc_application_round_link"));}
    @Test void twoCheckinsThenOrdinarySaveKeepDirectSourceAndManualActual(){
        initial();changeDefault();long first=checkin(20L,"one");long second=checkin(first,"two");
        assertEquals("A/1-1",files.selectById(first).getVersionNo());assertEquals("A/1-2",files.selectById(second).getVersionNo());
        var one=snapshots.readReservedDraft(99L,5L,"UPLOAD",first);var two=snapshots.readReservedDraft(99L,5L,"UPLOAD",second);
        assertEquals(20L,one.getSourceApplicationId());assertEquals(first,two.getSourceApplicationId());assertEquals(one.getApplicationRound(),two.getSourceApplicationRound());
        assertEquals(nmpa,attributes.readValue(two.getDefaultSourceJson()));assertEquals(ce,attributes.readValue(two.getActualAttributesJson()));
        tx().executeWithoutResult(s->workflow.saveWorkingApplicationAttributes(99L,second,attrs("MADSAP")));
        assertEquals(attrs("MADSAP"),attributes.readValue(snapshots.readReservedDraft(99L,5L,"UPLOAD",second).getActualAttributesJson()));
        assertEquals(ce,attributes.readValue(one.getActualAttributesJson()));assertEquals(3,count("dcc_application_round_link"));
    }
    @ParameterizedTest @ValueSource(strings={"INITIAL","PARTIAL_A1","PARTIAL_A9","REPLACEMENT"})
    void realEarlierTwoCheckinsSaveAndV4SubmissionPreserveSelectedFacts(String branch) throws Exception {
        boolean isInitial=branch.equals("INITIAL");String base=branch.equals("PARTIAL_A9")?"A/9":branch.equals("REPLACEMENT")?"A/3":"A/1";
        if(isInitial)initial();else controlled(base);changeDefault();
        long first=checkinDistinctBody(20L,"first",new byte[]{10,11,12});
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(attrs("MADSAP")));
        long second=checkinDistinctBody(first,"second",new byte[]{20,21,22});
        String type=isInitial?"UPLOAD":"REVISION";
        tx().executeWithoutResult(s->{workflow.saveWorkingApplicationAttributes(99L,first,ce);workflow.saveWorkingApplicationAttributes(99L,second,attrs("MADSAP"));});
        jdbc.update("INSERT INTO dcc_controlled_file_signature(controlled_file_id,task_id,actor_id,action_type,signature_mode,version_no,evidence_hash,tenant_id) VALUES(20,'previous-history',99,'APPROVE','PASSWORD',?,'unchanged-historical-evidence',1)",base);
        String originalBefore=JsonUtils.toJsonString(files.selectById(20L));
        String firstBefore=JsonUtils.toJsonString(files.selectById(first));String secondBefore=JsonUtils.toJsonString(files.selectById(second));
        var signaturesBefore=jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature");
        assertNotEquals(files.selectById(first).getSourceSha256(),files.selectById(second).getSourceSha256());
        var req=request(isInitial?"INITIAL":branch.equals("REPLACEMENT")?"REPLACEMENT":"PARTIAL");
        long candidate=assertDoesNotThrow(()->tx().execute(s->workflow.submitWorkingIteration(99L,first,req)),"earlier legal selected body must be accepted for "+branch);var result=files.selectById(candidate);
        assertEquals(isInitial?"A/1":branch.equals("PARTIAL_A1")?"A/2":"B/1",result.getVersionNo());assertEquals(req.getRevisionChangeType(),result.getRevisionChangeType());
        assertEquals(first,result.getSelectedIterationControlledFileId());assertEquals(files.selectById(first).getVersionNo(),result.getSelectedIterationVersionNo());
        assertEquals(files.selectById(first).getSourceSha256(),result.getSourceSha256());assertArrayEquals(new byte[]{10,11,12},bytes.get(infraFiles.selectById(result.getSourceFileId()).getPath()));
        assertEquals(originalBefore,JsonUtils.toJsonString(files.selectById(20L)));assertEquals(firstBefore,JsonUtils.toJsonString(files.selectById(first)));assertEquals(secondBefore,JsonUtils.toJsonString(files.selectById(second)));
        assertEquals(signaturesBefore,jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature"));assertNull(result.getControlledTime());
        int round=rounds.require(type,candidate,result.getProcessInstanceId());var frozen=attributeRows.find(type,candidate,round);
        assertTrue(frozen.getSubmitted());assertEquals(isInitial?nmpa:fda,attributes.readValue(frozen.getDefaultSourceJson()));assertEquals(ce,attributes.readValue(frozen.getActualAttributesJson()));
        assertEquals(first,frozen.getSourceApplicationId());assertEquals(attrs("MADSAP"),attributes.readValue(snapshots.readReservedDraft(99L,5L,type,second).getActualAttributesJson()));
        assertNotNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(result.getProcessInstanceId()).singleResult());
        assertEquals(String.valueOf(candidate),engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(result.getProcessInstanceId()).singleResult().getBusinessKey());
        assertTrue(engine.getTaskService().createTaskQuery().processInstanceId(result.getProcessInstanceId()).count()>0);
        assertEquals(isInitial?null:20L,masters.selectById(10L).getCurrentActiveControlledFileId());assertEquals(isInitial?null:20L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals(Long.valueOf(candidate),tx().execute(s->workflow.submitWorkingIteration(99L,first,req)));
        assertThrows(RuntimeException.class,()->tx().execute(s->workflow.submitWorkingIteration(99L,second,req)));
        req.setProjectAttributes(fda);assertThrows(RuntimeException.class,()->tx().execute(s->workflow.submitWorkingIteration(99L,first,req)));req.setProjectAttributes(ce);
        req.setEffectiveDate(LocalDate.of(2099,12,21));assertThrows(RuntimeException.class,()->tx().execute(s->workflow.submitWorkingIteration(99L,first,req)));
        assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());assertEquals(4,count("dcc_controlled_file"));
    }
    @Test void olderSelectedIterationMustBeAcceptedAsTheActualInitialApprovalBody(){initial();long first=checkin(20L,"older");long second=checkin(first,"latest");
        var req=request("INITIAL");
        long candidate=assertDoesNotThrow(()->tx().execute(s->workflow.submitWorkingIteration(99L,first,req)),"legal earlier WORKING body must not be rejected by latest-only guard");
        assertEquals(first,files.selectById(candidate).getSelectedIterationControlledFileId());
        assertEquals(files.selectById(first).getSourceSha256(),files.selectById(candidate).getSourceSha256());
        assertEquals("WORKING",files.selectById(first).getStatus());assertEquals("WORKING",files.selectById(second).getStatus());
        assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    @ParameterizedTest @ValueSource(strings={"TENANT","PROJECT","ACTOR","MASTER","CATEGORY","CHECKOUT","IN_FLIGHT","BASELINE","BODY"})
    void earlierSelectionStillRejectsEveryOriginalIdentityLockAndContentBoundary(String violation) throws Exception {
        controlled("A/1");changeDefault();long first=checkinDistinctBody(20L,"guard-first",new byte[]{1,10,1});long second=checkinDistinctBody(first,"guard-second",new byte[]{2,20,2});
        Long actor=99L;
        switch(violation){
            case "TENANT" -> TenantContextHolder.setTenantId(2L);
            case "PROJECT" -> jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=6 WHERE id=?",first);
            case "ACTOR" -> actor=88L;
            case "MASTER" -> jdbc.update("UPDATE dcc_controlled_file SET master_id=11 WHERE id=?",first);
            case "CATEGORY" -> when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(false);
            case "CHECKOUT" -> checkout(second);
            case "IN_FLIGHT" -> jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_REVIEW',process_instance_id='separate-actual-application' WHERE id=?",second);
            case "BASELINE" -> jdbc.update("UPDATE dcc_controlled_file SET revision_base_active_controlled_file_id=99 WHERE id=?",first);
            case "BODY" -> bytes.put(infraFiles.selectById(files.selectById(first).getSourceFileId()).getPath(),new byte[]{9,9,9});
            default -> throw new AssertionError(violation);
        }
        Long selectedActor=actor;int fileRows=count("dcc_controlled_file"),roundRows=count("dcc_application_round_link"),attrsRows=count("dcc_project_application_attributes"),bodyCount=bytes.size();
        assertThrows(RuntimeException.class,()->tx().execute(s->workflow.submitWorkingIteration(selectedActor,first,request("PARTIAL"))));
        TenantContextHolder.setTenantId(1L);
        assertEquals(fileRows,count("dcc_controlled_file"));assertEquals(roundRows,count("dcc_application_round_link"));assertEquals(attrsRows,count("dcc_project_application_attributes"));
        assertEquals(bodyCount,bytes.size());assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    @ParameterizedTest @ValueSource(strings={"INITIAL","PARTIAL","REPLACEMENT"})
    void selectedEarlierCandidateAndItsActualBpmSnapshotAndIsolatedBodyRollBackOnLateFailure(String intent) throws Exception {
        if("INITIAL".equals(intent))initial();else controlled("A/1");changeDefault();
        long first=checkinDistinctBody(20L,"rollback-first",new byte[]{10,11,12});long second=checkinDistinctBody(first,"rollback-second",new byte[]{20,21,22});
        int filesBefore=count("dcc_controlled_file"),infraBefore=count("infra_file"),roundsBefore=count("dcc_application_round_link"),attrsBefore=count("dcc_project_application_attributes"),bodiesBefore=bytes.size();
        String firstBefore=JsonUtils.toJsonString(files.selectById(first));String secondBefore=JsonUtils.toJsonString(files.selectById(second));
        var req=request(intent);
        assertThrows(IllegalStateException.class,()->tx().execute(s->{long candidate=workflow.submitWorkingIteration(99L,first,req);
            assertEquals(first,files.selectById(candidate).getSelectedIterationControlledFileId());assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
            assertTrue(attributeRows.find("INITIAL".equals(intent)?"UPLOAD":"REVISION",candidate,rounds.require("INITIAL".equals(intent)?"UPLOAD":"REVISION",candidate,files.selectById(candidate).getProcessInstanceId())).getSubmitted());
            throw new IllegalStateException("late after selected candidate, real V4 BPM, immutable snapshot and verified isolated body");}));
        assertEquals(filesBefore,count("dcc_controlled_file"));assertEquals(infraBefore,count("infra_file"));assertEquals(roundsBefore,count("dcc_application_round_link"));assertEquals(attrsBefore,count("dcc_project_application_attributes"));
        assertEquals(bodiesBefore,bytes.size());assertEquals(firstBefore,JsonUtils.toJsonString(files.selectById(first)));assertEquals(secondBefore,JsonUtils.toJsonString(files.selectById(second)));
        assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());assertEquals(0,engine.getTaskService().createTaskQuery().count());
        assertNull(checkouts.selectActiveByMasterIdForRead(1L,10L));
    }
    @Test void controlledBaselineDifferentRequesterStartsNewApplicationWithActualOwner(){
        controlled("A/1");jdbc.update("UPDATE dcc_controlled_file SET requester_id=88 WHERE id=20");
        when(access.hasProjectOwner(99L,5L)).thenReturn(true);changeDefault();long target=checkin(20L,"actual owner");
        assertEquals(99L,files.selectById(target).getRequesterId());var saved=snapshots.readReservedDraft(99L,5L,"REVISION",target);
        assertEquals(fda,attributes.readValue(saved.getDefaultSourceJson()));assertNull(saved.getSourceApplicationId());verify(access,never()).assertProjectEditorOrOwner(88L,5L);
    }
    @Test void actualSubmittedRejectedSourceForkThenSecondCheckinKeepOriginalSnapshot() throws Exception {
        initial();long source=tx().execute(s->workflow.submitWorkingIteration(99L,20L,request("INITIAL")));
        String nativeBpm=files.selectById(source).getProcessInstanceId();assertNotNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(nativeBpm).singleResult());
        // A owns BPM rejection/termination. This fixture supplies its persisted rejected fact;
        // native process id and frozen mapping remain real, without claiming the rejection task was exercised.
        jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED' WHERE id=?",source);changeDefault();checkout(source);
        var upload=new FileDO();upload.setId(101L);upload.setConfigId(1L);upload.setName("SOP.pdf");upload.setPath("incoming/SOP.pdf");upload.setUrl("isolated-test");upload.setType("application/pdf");upload.setSize(4L);infraFiles.insert(upload);bytes.put(upload.getPath(),new byte[]{9,8,7,6});
        var tickets=mock(cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService.class);
        when(tickets.resolveForBinding(any())).thenReturn(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile("fresh-ticket",101L,"SOP.pdf","application/pdf",4L));
        // Set the explicit external ticket port on the proxy's real target, not a mock business service.
        Object target=((org.springframework.aop.framework.Advised)query).getTargetSource().getTarget();wire(target,"uploadTicketService",tickets);
        var r=edit("fork fresh body");r.setUploadTicket("fresh-ticket");r.setSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.checkinPrefix(source),"C-fork"));
        long fork=tx().execute(s->query.checkinControlledFile(99L,source,r)).getId();long next=checkin(fork,"fork second");
        var one=snapshots.readReservedDraft(99L,5L,"UPLOAD",fork);var two=snapshots.readReservedDraft(99L,5L,"UPLOAD",next);
        assertEquals(source,one.getSourceApplicationId());assertEquals(rounds.require("UPLOAD",source,nativeBpm),one.getSourceApplicationRound());assertEquals(fork,two.getSourceApplicationId());
        assertEquals(nmpa,attributes.readValue(two.getDefaultSourceJson()));assertEquals(ce,attributes.readValue(two.getActualAttributesJson()));assertTrue(attributeRows.find("UPLOAD",source,one.getSourceApplicationRound()).getSubmitted());
    }
    @ParameterizedTest @ValueSource(strings={"ROUND","ATTRIBUTES","SOURCE_JSON","ACTUAL_JSON"})
    void missingSavedSourceRejectsAndPreservesCheckout(String missing){initial();checkout(20L);
        if(missing.equals("ROUND"))jdbc.update("DELETE FROM dcc_application_round_link");else if(missing.equals("ATTRIBUTES"))jdbc.update("DELETE FROM dcc_project_application_attributes");else if(missing.equals("SOURCE_JSON"))jdbc.update("UPDATE dcc_project_application_attributes SET default_source_json='' WHERE application_id=20");else jdbc.update("UPDATE dcc_project_application_attributes SET actual_attributes_json='' WHERE application_id=20");
        int before=count("dcc_application_round_link");assertThrows(RuntimeException.class,()->tx().execute(s->query.checkinControlledFile(99L,20L,edit("missing"))));
        assertEquals(1,count("dcc_controlled_file"));assertEquals(1,count("infra_file"));assertEquals(0,count("dcc_controlled_file_source_ownership"));assertEquals(before,count("dcc_application_round_link"));assertNotNull(checkouts.selectActiveByMasterIdForRead(1L,10L));assertEquals(99L,files.selectById(20L).getCheckedOutBy());
    }
    @Test void preciseCheckinReplayDoesNotCopyOrInitializeAgain(){initial();checkout(20L);var req=edit("replay");long first=tx().execute(s->query.checkinControlledFile(99L,20L,req)).getId();int bodyCount=bytes.size();int mappings=count("dcc_application_round_link");int attributeCount=count("dcc_project_application_attributes");assertEquals(first,tx().execute(s->query.checkinControlledFile(99L,20L,req)).getId());assertEquals(bodyCount,bytes.size());assertEquals(mappings,count("dcc_application_round_link"));assertEquals(attributeCount,count("dcc_project_application_attributes"));verify(storage,times(1)).createFileAndReturnId(any(),any(),any(),any());}
    @ParameterizedTest @ValueSource(strings={"PROJECT","CATEGORY","ACTOR","TENANT","SCOPE"})
    void permissionFailuresHaveNoNewBusinessWrites(String denied){initial();checkout(20L);
        if(denied.equals("PROJECT"))doThrow(new IllegalArgumentException("VIEW-only project actor")).when(access).assertProjectEditorOrOwner(99L,5L);
        if(denied.equals("CATEGORY"))when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(false);
        if(denied.equals("SCOPE")){var port=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(port.hasAnyPermissions(eq(99L),any(String[].class))).thenAnswer(c->Arrays.asList((String[])c.getArgument(1)).contains("dcc:project-code-assignment:execute"));wire(scope,"permissionApi",port);}
        if(denied.equals("TENANT"))TenantContextHolder.setTenantId(2L);
        Long actor=denied.equals("ACTOR")?88L:99L;
        assertThrows(RuntimeException.class,()->tx().execute(s->query.checkinControlledFile(actor,20L,edit("denied"))));TenantContextHolder.setTenantId(1L);
        assertEquals(1,count("dcc_controlled_file"));assertEquals(1,count("infra_file"));assertEquals(1,count("dcc_application_round_link"));assertEquals(1,count("dcc_project_application_attributes"));assertNotNull(checkouts.selectActiveByMasterIdForRead(1L,10L));verify(storage,never()).createFileAndReturnId(any(),any(),any(),any());
    }
    @ParameterizedTest @ValueSource(strings={"BODY_CLAIM","RELATION","CHECKOUT_CAS","OUTER","CANDIDATE"})
    void lateFailureRollsBackRealFileBodyCheckoutRoundsAttributesAndCandidate(String stage){initial();checkout(20L);
        if(stage.equals("BODY_CLAIM"))jdbc.execute("ALTER TABLE dcc_controlled_file_source_ownership ADD CONSTRAINT c_test_body_fail CHECK (ownership_type <> 'CHECKIN')");
        if(stage.equals("RELATION")){jdbc.update("INSERT INTO dcc_controlled_file_related_file(controlled_file_id,related_controlled_file_id,project_code_id,relation_source,tenant_id,deleted) VALUES(20,777,5,'UPLOAD',1,0)");jdbc.execute("ALTER TABLE dcc_controlled_file_related_file ADD CONSTRAINT c_test_relation_fail CHECK (controlled_file_id=20)");}
        if(stage.equals("CHECKOUT_CAS")){jdbc.execute("ALTER TABLE dcc_controlled_file_checkout ADD CONSTRAINT c_test_checkout_fail CHECK (status <> 'CHECKED_IN')");}
        try{assertThrows(RuntimeException.class,()->tx().execute(s->{var r=query.checkinControlledFile(99L,20L,edit("late"));if(stage.equals("CANDIDATE")){workflow.submitWorkingIteration(99L,r.getId(),request("INITIAL"));throw new IllegalStateException("late after actual BPM/candidate freeze");}if(stage.equals("OUTER"))throw new IllegalStateException("late caller after successful Query");return r;}));}
        finally{if(stage.equals("BODY_CLAIM"))jdbc.execute("ALTER TABLE dcc_controlled_file_source_ownership DROP CONSTRAINT c_test_body_fail");if(stage.equals("CHECKOUT_CAS"))jdbc.execute("ALTER TABLE dcc_controlled_file_checkout DROP CONSTRAINT c_test_checkout_fail");if(stage.equals("RELATION"))jdbc.execute("ALTER TABLE dcc_controlled_file_related_file DROP CONSTRAINT c_test_relation_fail");}
        assertEquals(1,count("dcc_controlled_file"));assertEquals(1,count("infra_file"));assertEquals(0,count("dcc_controlled_file_source_ownership"));assertEquals(1,count("dcc_application_round_link"));assertEquals(1,count("dcc_project_application_attributes"));assertEquals(1,bytes.size());assertNotNull(checkouts.selectActiveByMasterIdForRead(1L,10L));assertEquals(99L,files.selectById(20L).getCheckedOutBy());assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
        if(stage.equals("OUTER") || stage.equals("CANDIDATE"))assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_access_log WHERE action_type='CHECKIN' AND result='SUCCESS'",Integer.class)); // Final C SUCCESS joins the business transaction and rolls back with the selected candidate.
    }
    @Test void realProjectLockMustPrecedeMasterAndFileAcrossTwoConnections() throws Exception {initial();checkout(20L);
        var requested=new CountDownLatch(1);var held=new CountDownLatch(1);var release=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        doAnswer(c->{if(Thread.currentThread().getName().contains("C-checkin"))requested.countDown();return sql.selectOne(DccProjectCodeMapper.class.getName()+".selectByIdForUpdate",Map.of("id",c.getArgument(0)));}).when(projects).selectByIdForUpdate(5L);
        try{
            var owner=executor.submit(()->{TenantContextHolder.setTenantId(1L);try{tx().executeWithoutResult(s->{projects.selectByIdForUpdate(5L);held.countDown();await(release);masters.selectByIdForUpdate(10L);files.selectByIdAndTenantForUpdate(1L,20L);});}finally{TenantContextHolder.clear();}});
            assertTrue(held.await(5,TimeUnit.SECONDS));var edit=executor.submit(()->{Thread.currentThread().setName("C-checkin-lock");TenantContextHolder.setTenantId(1L);try{return tx().execute(s->query.checkinControlledFile(99L,20L,edit("lock")));}finally{TenantContextHolder.clear();}});
            assertTrue(requested.await(5,TimeUnit.SECONDS));release.countDown();owner.get(8,TimeUnit.SECONDS);assertNotNull(edit.get(8,TimeUnit.SECONDS));
        }finally{release.countDown();executor.shutdownNow();}
    }
    static void await(CountDownLatch latch){try{if(!latch.await(8,TimeUnit.SECONDS))throw new IllegalStateException("lock latch timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
    @Test void checkinHoldingProjectSerializesActualBSavedDraftOnAnotherConnection() throws Exception {
        initial();checkout(20L);var projectHeld=new CountDownLatch(1);var release=new CountDownLatch(1);var bRequested=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        doAnswer(c->{String thread=Thread.currentThread().getName();if(thread.equals("B-save-lock"))bRequested.countDown();
            var value=sql.selectOne(DccProjectCodeMapper.class.getName()+".selectByIdForUpdate",Map.of("id",c.getArgument(0)));
            if(thread.equals("C-first-lock") && projectHeld.getCount()>0){projectHeld.countDown();await(release);}return value;
        }).when(projects).selectByIdForUpdate(5L);
        try{
            var c=pool.submit(()->{Thread.currentThread().setName("C-first-lock");TenantContextHolder.setTenantId(1L);try{return tx().execute(s->query.checkinControlledFile(99L,20L,edit("serial"))).getId();}finally{TenantContextHolder.clear();}});
            assertTrue(projectHeld.await(5,TimeUnit.SECONDS));var b=pool.submit(()->{Thread.currentThread().setName("B-save-lock");TenantContextHolder.setTenantId(1L);try{tx().executeWithoutResult(s->snapshots.saveReservedDraft(99L,5L,"UPLOAD",20L,attrs("MADSAP")));}finally{TenantContextHolder.clear();}});
            assertTrue(bRequested.await(5,TimeUnit.SECONDS));release.countDown();long target=c.get(8,TimeUnit.SECONDS);b.get(8,TimeUnit.SECONDS);
            assertEquals(ce,attributes.readValue(snapshots.readReservedDraft(99L,5L,"UPLOAD",target).getActualAttributesJson()));assertEquals(attrs("MADSAP"),attributes.readValue(snapshots.readReservedDraft(99L,5L,"UPLOAD",20L).getActualAttributesJson()));
        }finally{release.countDown();pool.shutdownNow();}
    }
}
