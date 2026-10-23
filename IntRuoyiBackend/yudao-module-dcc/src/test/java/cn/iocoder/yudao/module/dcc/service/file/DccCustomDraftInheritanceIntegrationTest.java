package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrixService.SourceContext;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** A34: actual Query/Revision, execution-contract/custom canonical services, initializer/rounds and SQL.
 * Storage and external permission/directory ports stay explicit; this is not the complete G03 BPM resubmission suite. */
@org.springframework.context.annotation.Import({DccWorkingApplicationDraftInitializer.class,DccApplicationRoundService.class,
        cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectApplicationSnapshotService.class,
        cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributesService.class,DccCustomDraftInheritanceIntegrationTest.Beans.class})
@org.springframework.test.context.TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:g06_custom_inheritance;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;LOCK_TIMEOUT=5000")
class DccCustomDraftInheritanceIntegrationTest extends cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class Beans {
        @org.springframework.context.annotation.Bean org.springframework.jdbc.core.JdbcTemplate jdbc(javax.sql.DataSource source){return new org.springframework.jdbc.core.JdbcTemplate(source);}
    }
    @jakarta.annotation.Resource DccWorkingApplicationDraftInitializer initializer;
    @jakarta.annotation.Resource DccApplicationRoundService rounds;
    @jakarta.annotation.Resource cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectApplicationSnapshotService snapshots;
    @jakarta.annotation.Resource cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributesService attributes;
    @jakarta.annotation.Resource(name="dccControlledFileMapper") DccControlledFileMapper files;
    @jakarta.annotation.Resource DccControlledFileMasterMapper masters;
    @jakarta.annotation.Resource cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper attributeRows;
    @jakarta.annotation.Resource org.springframework.jdbc.core.JdbcTemplate jdbc;
    @jakarta.annotation.Resource org.springframework.transaction.PlatformTransactionManager transactions;
    @org.springframework.test.context.bean.override.mockito.MockitoBean cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService access;
    @org.springframework.test.context.bean.override.mockito.MockitoBean DccControlledFileCategoryPermissionSupport permissions;
    DccApplicationExecutionContractService contracts;
    org.flowable.engine.ProcessEngine engine;
    final cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes actual=
            new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(List.of("CE"),null,"Y","N","N",null);
    DccControlledFileQueryServiceImpl query;
    DccControlledFileRevisionServiceImpl revision;
    DccControlledFileCheckoutMapper checkouts;
    DccControlledFileSourceOwnershipService storage;
    String definition,digest;
    final String contextKey="g06-a34-saved-custom";
    final DccApplicationCustomMatrix matrix=new DccApplicationCustomMatrix(71L,72L);
    final List<Long> departments=List.of(51L,52L);
    final String sourceHash="a".repeat(64);

    @BeforeEach void a34Setup() throws Exception {
        seedSourceFamily();
        // Private H2 attachment shape matches the existing ordinary/training fact migrations; no shared fixture edits.
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_controlled_file_attachment(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,controlled_file_id BIGINT,storage_file_id BIGINT,original_file_name VARCHAR(512),content_type VARCHAR(255),file_size BIGINT,file_sha256 VARCHAR(64),sort_no INT,attachment_purpose VARCHAR(32),application_process_instance_id VARCHAR(128),uploaded_by BIGINT,uploaded_username_snapshot VARCHAR(128),uploaded_nickname_snapshot VARCHAR(128),uploaded_at TIMESTAMP,creator VARCHAR(64),create_time TIMESTAMP,updater VARCHAR(64),update_time TIMESTAMP,deleted TINYINT DEFAULT 0)");
        var sql=cn.hutool.extra.spring.SpringUtil.getBean(SqlSessionTemplate.class);
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_controlled_file_route_snapshot(id BIGINT AUTO_INCREMENT PRIMARY KEY,controlled_file_id BIGINT,route_version_no INT,source_approval_matrix_id BIGINT,source_approval_matrix_version INT,source_approval_matrix_digest VARCHAR(64),derivation_version VARCHAR(64),source_approval_matrix_json CLOB,runtime_approval_nodes_json CLOB,stage_no INT,stage_code VARCHAR(64),stage_name VARCHAR(128),stage_order INT,candidate_source_type VARCHAR(32),candidate_source_id BIGINT,candidate_source_ids VARCHAR(1000),resolved_user_ids VARCHAR(1000),approve_method VARCHAR(32),approve_ratio INT,require_all_approvals TINYINT DEFAULT 0,tenant_id BIGINT DEFAULT 1,create_time TIMESTAMP,update_time TIMESTAMP,creator VARCHAR(64),updater VARCHAR(64),deleted TINYINT DEFAULT 0)");
        var routeMapper=sql.getMapper(DccControlledFileRouteSnapshotMapper.class);
        ReflectionTestUtils.setField(contracts,"routeSnapshots",routeMapper);
        checkouts=sql.getMapper(DccControlledFileCheckoutMapper.class);
        definition=deploy(Files.readString(DccRevisionAttachmentsDatabaseTest.Fixture.requireRepositoryRoot()
                .resolve("IntRuoyiBackend/config/dcc-workflow-v6/upload.bpmn20.xml")));
        digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(definition));
        var verified=contracts.requireOrdinaryPublic(DccApplicationExecutionContractService.PUBLIC_V2,definition,digest,DccControlledFileProcessDefinitionKeys.UPLOAD);
        int round=rounds.requireDraft("UPLOAD",20L);
        String canonical=new DccApplicationCustomMatrixService().canonicalize(context(20L,round),matrix,departments).canonical();
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=?,draft_approval_source_mode='APPLICATION_CUSTOM',draft_application_custom_matrix_json=?,draft_application_context_key=?,effective_mode='DELAYED',need_training=0,selected_signoff_department_ids_json='[\"51\",\"52\"]',distribution_department_ids_json='[]' WHERE id=20",
                verified.executionContract(),definition,digest,canonical,contextKey);
        storage=mock(DccControlledFileSourceOwnershipService.class);
        when(storage.inspectSource(anyLong())).thenAnswer(call->new DccControlledFilePreparedSource(call.getArgument(0),call.getArgument(0),sourceHash,false));
        when(storage.prepareSubmissionSource(anyLong(),eq(false))).thenAnswer(call->new DccControlledFilePreparedSource(call.<Long>getArgument(0)+1000,call.getArgument(0),sourceHash,true));
        when(storage.createVerifiedCopy(anyLong())).thenAnswer(call->new DccControlledFilePreparedSource(call.<Long>getArgument(0)+2000,call.getArgument(0),sourceHash,true));
        when(storage.rollbackCleanup(any())).thenReturn(()->{});
        var names=mock(DccControlledFileNameClaimService.class);when(names.requireSourceName(any())).thenReturn("SOP.pdf");
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(eq(99L),anyLong())).thenReturn(true);
        var attachments=mock(DccControlledFileAttachmentService.class);
        when(attachments.readOrdinaryAttachmentManifest(eq(99L),anyLong())).thenAnswer(call->{
            Long id=call.getArgument(1);assertTrue(sql.getMapper(DccControlledFileAttachmentMapper.class).selectListByControlledFileId(id).isEmpty());
            String payload=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Arrays.asList("DCC_ORDINARY_ATTACHMENT_MANIFEST_V1",1L,id,List.of()));
            return new DccControlledFileAttachmentService.OrdinaryAttachmentManifest(List.of(),DigestUtil.sha256Hex(payload));
        });
        var guard=new DccControlledFileDetailAuthorizationGuard();ports(guard);wire(guard,"assignmentScopeService",scope);
        query=new DccControlledFileQueryServiceImpl();ports(query);
        wire(query,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"projectCodeMapper",sql.getMapper(DccProjectCodeMapper.class),
                "checkoutMapper",checkouts,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"sourceOwnershipService",storage,
                "fileMapper",mock(FileMapper.class),"nameClaimService",names,"permissionSupport",permissions,"projectAccessService",access,
                "assignmentScopeService",scope,"workingApplicationDraftInitializer",initializer,"executionContracts",contracts,"applicationRoundService",rounds,
                "attachmentService",attachments,"checkinAttachmentMapper",sql.getMapper(DccControlledFileAttachmentMapper.class),
                "detailAuthorizationGuard",guard,"downloadPolicyService",new cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService());
        revision=new DccControlledFileRevisionServiceImpl();ports(revision);
        var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Shanghai");
        wire(revision,"fileMapper",files,"masterMapper",masters,"checkoutMapper",checkouts,"projectAccessService",access,
                "assignmentScopeService",scope,"permissionSupport",permissions,"sourceOwnershipService",storage,"nameClaimService",names,
                "versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"datePolicy",dates,"executionContracts",contracts,
                "applicationRoundService",rounds,"attachmentService",attachments);
        // The dependency is new only when the formal candidate needs a real reserved custom round.
        if(Arrays.stream(revision.getClass().getDeclaredFields()).anyMatch(field->field.getName().equals("workingApplicationDraftInitializer")))
            wire(revision,"workingApplicationDraftInitializer",initializer);
    }
    void seedSourceFamily() throws Exception {
        TenantContextHolder.setTenantId(1L);
        try(var connection=Objects.requireNonNull(jdbc.getDataSource()).getConnection()){
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status,default_attributes_json,deleted) VALUES(5,1,'G06-A34','G06-A34','ENABLE',?,0)",attributes.encode(actual));
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,status,deleted) VALUES(10,1,2,3,'SOP','PREPARED-1',5,6,'PREPARED-1','ACTIVE_CHAIN',0)");
        when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        engine=org.flowable.engine.ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:g06_a34_definition_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate("true").setAsyncExecutorActivate(false).buildProcessEngine();
        var definitions=mock(cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService.class);
        when(definitions.getProcessDefinition(anyString())).thenAnswer(call->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(call.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(call->engine.getRepositoryService().getBpmnModel(call.getArgument(0)));
        contracts=new DccApplicationExecutionContractService();
        wire(contracts,"definitions",definitions,"files",files,"rounds",rounds,"sourceAttributes",attributeRows,
                "projectAccess",access,"categoryPermissions",permissions,"masters",masters);
        addBody(20L,null);tx().executeWithoutResult(status->initializer.initialize(99L,null,20L,actual));
    }
    @AfterEach void close(){if(engine!=null)engine.close();TenantContextHolder.clear();}
    org.springframework.transaction.support.TransactionTemplate tx(){return new org.springframework.transaction.support.TransactionTemplate(transactions);}
    static void wire(Object target,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(target,(String)pairs[i],pairs[i+1]);}
    String deploy(String xml){var deployment=engine.getRepositoryService().createDeployment().tenantId("1").addString("actual-upload.bpmn20.xml",xml).deploy();return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult().getId();}
    void addBody(Long id,Long source){files.insert(DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).dccProjectCodeId(5L).categoryId(2L).directoryId(3L)
            .fileTypeTaxonomyId(6L).fileNumber("PREPARED-1").fileName("SOP").title("SOP").sourceOriginalFileName("SOP.pdf").sourceFileId(100L).originalFileId(100L).sourceSha256(sourceHash)
            .requesterId(99L).submitterId(99L).versionNo(source==null?"A/1":"A/1-1").changeType("NEW").processType("CONTROLLED_FILE").status("WORKING").predecessorControlledFileId(source)
            .previousSourceSha256(source==null?null:sourceHash).build());}
    void copy(Long id,Long source){addBody(id,source);tx().executeWithoutResult(status->initializer.initialize(99L,source,id,null));}
    static void ports(Object service){for(var field:service.getClass().getDeclaredFields())if(field.getAnnotation(jakarta.annotation.Resource.class)!=null)ReflectionTestUtils.setField(service,field.getName(),mock(field.getType()));}
    SourceContext context(Long id,int round){return new SourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,1L,5L,2L,6L,99L,id,round,"UPLOAD",contextKey,definition,digest);}
    DccControlledFileCheckinReqVO input(){var input=new DccControlledFileCheckinReqVO();input.setChangeDescription("preserve actual custom choices");input.setRemark("real metadata change");input.setNeedTraining(false);input.setAttachmentUploadTickets(List.of());return input;}
    void checkout(){var reason=new DccControlledFileCheckoutReqVO();reason.setReason("A34 exact custom checkin");tx().execute(s->query.checkoutControlledFile(99L,20L,reason));}
    void assertFreshCustom(Long id,String sourceCanonical){var target=files.selectById(id);assertEquals("APPLICATION_CUSTOM",target.getDraftApprovalSourceMode());
        String sourceKey=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(sourceCanonical).get("context").get("applicationContextKey").asText();
        assertNotEquals(sourceKey,target.getDraftApplicationContextKey());assertNotNull(target.getDraftApplicationContextKey());
        int round=rounds.requireDraft("UPLOAD",id);
        var targetContext=new SourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,1L,5L,2L,6L,99L,id,round,"UPLOAD",target.getDraftApplicationContextKey(),definition,digest);
        var actual=new DccApplicationCustomMatrixService().readCanonical(target.getDraftApplicationCustomMatrixJson(),DigestUtil.sha256Hex(target.getDraftApplicationCustomMatrixJson()),targetContext);
        assertEquals(matrix,actual.applicationCustomMatrix());assertEquals(departments,actual.selectedSignoffDepartmentIds());assertNotEquals(sourceCanonical,target.getDraftApplicationCustomMatrixJson());}

    @Test void a34_queryCheckinCallsActualTypedServiceAndPersistsNewVersionRoundCanonical() {
        String source=files.selectById(20L).getDraftApplicationCustomMatrixJson();checkout();
        long id=tx().execute(s->query.checkinControlledFile(99L,20L,input())).getId();
        assertFreshCustom(id,source);assertEquals(source,files.selectById(20L).getDraftApplicationCustomMatrixJson());
    }
    @Test void a34_formalCandidateCallsActualTypedServiceAfterCreatingItsRealReservedRound() {
        prepareCustomWorkingBody();
        String source=files.selectById(21L).getDraftApplicationCustomMatrixJson();
        var created=tx().execute(s->revision.createInitialCandidate(99L,21L,initialRequest()));
        assertFreshCustom(created.getId(),source);assertEquals(source,files.selectById(21L).getDraftApplicationCustomMatrixJson());
    }
    void prepareCustomWorkingBody() {
        copy(21L,20L);tx().execute(s->{contracts.inheritPrepared(99L,21L,20L,rounds.requireDraft("UPLOAD",20L));
            var custom=contracts.readInheritedCustomDraft(99L,21L,20L,rounds.requireDraft("UPLOAD",20L));
            jdbc.update("UPDATE dcc_controlled_file SET draft_approval_source_mode=?,draft_application_custom_matrix_json=?,draft_application_context_key=?,effective_mode='DELAYED',need_training=0,selected_signoff_department_ids_json='[\"51\",\"52\"]',distribution_department_ids_json='[]' WHERE id=21",custom.draftApprovalSourceMode(),custom.draftApplicationCustomMatrixJson(),custom.draftApplicationContextKey());return null;});
    }
    DccControlledFileSubmitIterationReqVO initialRequest() {
        var request=new DccControlledFileSubmitIterationReqVO();request.setIdempotencyKey("a34-initial-custom");request.setRevisionChangeType("INITIAL");request.setChangeDescription("A34 exact custom candidate");request.setNeedTraining(false);request.setEffectiveMode("DELAYED");request.setProjectAttributes(actual);request.setSelectedSignoffDepartmentIds(departments);request.setSelectedDistributionDepartmentIds(List.of());request.setExecutionContract(DccApplicationExecutionContractService.PUBLIC_V2);request.setExpectedProcessDefinitionId(definition);request.setExpectedDefinitionDigest(digest);request.setExpectedOrdinaryAttachmentIds(List.of());
        var port=(DccControlledFileAttachmentService)ReflectionTestUtils.getField(revision,"attachmentService");request.setExpectedOrdinaryAttachmentManifestHash(port.readOrdinaryAttachmentManifest(99L,21L).payloadHash());
        return request;
    }
    @Test void a34_customCasFailureRollsBackCreatedVersionAndRoundThenRetries() {
        checkout();int filesBefore=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class);int attributesBefore=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class);
        jdbc.execute("ALTER TABLE dcc_controlled_file ADD CONSTRAINT a34_custom_save_fail CHECK(id=20 OR draft_application_custom_matrix_json IS NULL)");
        try {
            assertThrows(RuntimeException.class,()->tx().execute(s->query.checkinControlledFile(99L,20L,input())));
            assertEquals(filesBefore,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));assertEquals(attributesBefore,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));assertNotNull(checkouts.selectActiveByMasterIdForRead(1L,10L));
        } finally {jdbc.execute("ALTER TABLE dcc_controlled_file DROP CONSTRAINT a34_custom_save_fail");}
        long id=tx().execute(s->query.checkinControlledFile(99L,20L,input())).getId();assertFreshCustom(id,files.selectById(20L).getDraftApplicationCustomMatrixJson());
    }
    @Test void a34_formalCandidateNeverPersistsCanonicalForAnotherFileEvenDuringItsInsert() {
        prepareCustomWorkingBody();
        jdbc.execute("ALTER TABLE dcc_controlled_file ADD CONSTRAINT a34_exact_custom_file CHECK(draft_application_custom_matrix_json IS NULL OR LOCATE('\"controlledFileId\":\"'||CAST(id AS VARCHAR)||'\"',draft_application_custom_matrix_json)>0)");
        try {
            var created=tx().execute(s->revision.createInitialCandidate(99L,21L,initialRequest()));
            assertFreshCustom(created.getId(),files.selectById(21L).getDraftApplicationCustomMatrixJson());
        } finally {jdbc.execute("ALTER TABLE dcc_controlled_file DROP CONSTRAINT a34_exact_custom_file");}
    }
    @Test void a34_candidateCustomCasFailureRollsBackNewBodyAndReservedRoundThenRetries() {
        prepareCustomWorkingBody();int filesBefore=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class);int attributesBefore=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class);
        jdbc.execute("ALTER TABLE dcc_controlled_file ADD CONSTRAINT a34_candidate_save_fail CHECK(id IN(20,21) OR draft_application_custom_matrix_json IS NULL)");
        try {
            assertThrows(RuntimeException.class,()->tx().execute(s->revision.createInitialCandidate(99L,21L,initialRequest())));
            assertEquals(filesBefore,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));assertEquals(attributesBefore,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));
        } finally {jdbc.execute("ALTER TABLE dcc_controlled_file DROP CONSTRAINT a34_candidate_save_fail");}
        var created=tx().execute(s->revision.createInitialCandidate(99L,21L,initialRequest()));assertFreshCustom(created.getId(),files.selectById(21L).getDraftApplicationCustomMatrixJson());
    }
    @Test void a34_publicLegacySourceDoesNotAcquireCustomFieldsOrAnotherCustomRound() throws Exception {
        String migration=Files.readString(DccRevisionAttachmentsDatabaseTest.Fixture.requireRepositoryRoot()
                .resolve("IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql"));
        var model=java.util.regex.Pattern.compile("SET @dcc_upload_bpmn = '([\\s\\S]*?)';").matcher(migration);assertTrue(model.find());
        String legacyDefinition=deploy(model.group(1));
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=NULL,prepared_process_definition_id=NULL,prepared_definition_digest=NULL,draft_approval_source_mode='PUBLIC_MATRIX',draft_application_custom_matrix_json=NULL,draft_application_context_key=NULL WHERE id=20");
        tx().executeWithoutResult(status->contracts.prepareLegacy(99L,files.selectById(20L),legacyDefinition));
        checkout();var saved=tx().execute(status->query.checkinControlledFile(99L,20L,input()));var target=files.selectById(saved.getId());
        assertEquals(DccApplicationExecutionContractService.LEGACY,target.getPreparedWorkflowContract());assertEquals(legacyDefinition,target.getPreparedProcessDefinitionId());
        assertNull(target.getDraftApprovalSourceMode());assertNull(target.getDraftApplicationCustomMatrixJson());assertNull(target.getDraftApplicationContextKey());
        assertNull(tx().execute(status->contracts.readInheritedCustomDraft(99L,target.getId(),20L,rounds.requireDraft("UPLOAD",20L))));
        assertEquals("PUBLIC_MATRIX",files.selectById(20L).getDraftApprovalSourceMode());
    }
}
