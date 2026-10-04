package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual A submission snapshot method -> persisted BPM mapping -> B immutable snapshots, on H2. */
class DccWorkflowAttributesIntegrationTest extends BaseDbUnitTest {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper g25Reservations;

    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager manager;
    @Resource DccProjectCodeMapper projects;
    @Resource DccProjectApplicationAttributesMapper snapshots;
    @Resource DccControlledFileMapper files;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper masters;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper checkouts;
    JdbcTemplate jdbc;DccControlledFileWorkflowServiceImpl workflow;
    DccProjectAttributesService attributes;DccApplicationRoundService rounds;
    private final DccProjectAttributes original=new DccProjectAttributes(List.of("NMPA"),null,"Y","N","N",null);
    private final DccProjectAttributes actual=new DccProjectAttributes(List.of("CE"),null,"N","Y","Y","目标方");
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_application_round_link(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,project_id BIGINT,application_type VARCHAR(16),application_id BIGINT,bpm_round VARCHAR(64),attribute_round INT,UNIQUE(tenant_id,application_type,application_id,bpm_round),UNIQUE(tenant_id,application_type,application_id,attribute_round))");
        jdbc.update("DELETE FROM dcc_application_round_link");
        attributes=new DccProjectAttributesService();
        var access=mock(DccProjectAccessService.class);
        wire(attributes,"projectCodeMapper",projects,"attributesMapper",snapshots,"accessService",access);
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,status,default_attributes_json,deleted) VALUES(5,1,'snapshot-project','ENABLE',?,0)",attributes.encode(original));
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,file_name,file_number,status,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number) VALUES(10,1,2,'SOP','N-1','ACTIVE_CHAIN',5,6,'N-1')");
        jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,submitter_id,requester_id,process_type,change_type,status,dcc_project_code_id,process_instance_id,process_definition_key,deleted) VALUES(20,1,10,2,3,100,100,'SOP.pdf','SOP','N-1','A/1',99,99,'CONTROLLED_FILE','NEW','WORKING',5,NULL,NULL,0)");
        jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='SOP.pdf',source_sha256='body-hash',file_type_taxonomy_id=6 WHERE id=20");
        rounds=new DccApplicationRoundService();wire(rounds,"jdbc",jdbc,"projects",projects,"files",files);
        workflow=new DccControlledFileWorkflowServiceImpl();
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(any(),any())).thenReturn(true);
        wire(workflow,"assignmentScopeService",scope);
        wire(workflow,"applicationRoundService",rounds,"projectAttributesService",attributes,"controlledFileMapper",files,"checkoutMapper",checkouts);
        var bridge=new DccProjectApplicationSnapshotService();wire(bridge,"rounds",rounds,"attributes",attributes,"files",files,"masters",masters,"snapshots",snapshots);
        // New public workflow wiring is added only after the actual business RED below.
        this.bridge=bridge;
        wire(workflow,"projectApplicationSnapshots",bridge);
        var permissions=mock(DccControlledFileCategoryPermissionSupport.class);
        when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        wire(workflow,"controlledFileMasterMapper",masters,"projectAccessService",access,"categoryPermissionSupport",permissions,"projectCodeMapper",projects);
        var initializer=new DccWorkingApplicationDraftInitializer();
        wire(initializer,"projectMapper",projects,"masterMapper",masters,"fileMapper",files,"rounds",rounds,"snapshots",bridge,"attributesMapper",snapshots,"projectAccess",access,"categoryPermission",permissions);
        wire(workflow,"workingApplicationDraftInitializer",initializer);
    }
    DccProjectApplicationSnapshotService bridge;
    @AfterEach void clear(){jdbc.update("DELETE FROM dcc_application_round_link");TenantContextHolder.clear();}
    @Test void actualValuesAndServerDefaultSourceRemainSeparateAfterProjectDefaultsChange() {
        freeze(actual);
        var saved=attributes.readSaved(99L,5L,"UPLOAD",20L,rounds.require("UPLOAD",20L,"opaque-bpm-round"));
        assertTrue(saved.getSubmitted());assertEquals(original,attributes.readValue(saved.getDefaultSourceJson()));
        assertEquals(actual,attributes.readValue(saved.getActualAttributesJson()));
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(actual));
        assertEquals(original,attributes.readValue(snapshots.find("UPLOAD",20L,1).getDefaultSourceJson()));
        assertEquals(actual,attributes.readValue(snapshots.find("UPLOAD",20L,1).getActualAttributesJson()));
    }
    @Test void outerBpmFailureRollsBackMappingAndBothAttributeSnapshots() {
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->{
            workflow.prepareApplicationDraft(99L,files.selectById(20L),actual);
            bindInitialBpm();workflow.freezeApplicationAttributes(99L,files.selectById(20L),actual);throw new IllegalStateException("BPM commit failed");}));
        assertNull(snapshots.find("UPLOAD",20L,1));
        assertThrows(IllegalStateException.class,()->rounds.require("UPLOAD",20L,"opaque-bpm-round"));
    }
    @Test void noUserChangeUsesRealProjectDefaultsAndNeverCreatesGuessedNoValues() {
        freeze(null);var saved=snapshots.find("UPLOAD",20L,1);
        assertEquals(original,attributes.readValue(saved.getActualAttributesJson()));
        assertEquals(original,attributes.readValue(saved.getDefaultSourceJson()));
    }
    @Test void wrongTenantCannotCreateAnAttributeRoundForAnotherTenantsFile() {
        TenantContextHolder.setTenantId(122L);
        assertThrows(RuntimeException.class,()->freeze(actual));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));
    }
    @Test void h02SubmissionBindsTheRealReservedRoundAndPreservesSavedSourceAfterDefaultChanges() {
        new TransactionTemplate(manager).executeWithoutResult(s->{
            bridge.prepareDraft(99L,5L,"UPLOAD",20L,actual);
            jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(actual));
        });
        assertDoesNotThrow(()->new TransactionTemplate(manager).executeWithoutResult(s->{bindInitialBpm();workflow.freezeApplicationAttributes(99L,files.selectById(20L),null);}),"A must bind the real reserved round, not allocate a new one");
        int realRound=rounds.require("UPLOAD",20L,"opaque-bpm-round");
        var saved=snapshots.find("UPLOAD",20L,realRound);
        assertTrue(saved.getSubmitted());assertEquals(original,attributes.readValue(saved.getDefaultSourceJson()));
        assertEquals(actual,attributes.readValue(saved.getActualAttributesJson()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
    }
    @Test void h02TwoCrossFileReworksForkSubmittedSourceAndActualRatherThanTodaysDefaults() {
        freeze(actual);String source=snapshots.find("UPLOAD",20L,1).getDefaultSourceJson();
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(actual));
        jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED' WHERE id=20");
        var first=newWorking(30L,20L);files.insert(first);
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,first,null));
        var fork=snapshots.find("UPLOAD",30L,rounds.requireDraft("UPLOAD",30L));
        assertEquals(source,fork.getDefaultSourceJson());assertEquals(actual,attributes.readValue(fork.getActualAttributesJson()));
        assertEquals(20L,fork.getSourceApplicationId());
        new TransactionTemplate(manager).executeWithoutResult(s->{bridge.saveReservedDraft(99L,5L,"UPLOAD",30L,original);
            first.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD);first.setProcessInstanceId("rework-one");
            workflow.freezeApplicationAttributes(99L,first,null);});
        jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED',process_instance_id='rework-one',process_definition_key='dcc-controlled-file-upload' WHERE id=30");
        var second=newWorking(40L,30L);files.insert(second);
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,second,null));
        var next=snapshots.find("UPLOAD",40L,rounds.requireDraft("UPLOAD",40L));
        assertEquals(source,next.getDefaultSourceJson());assertEquals(original,attributes.readValue(next.getActualAttributesJson()));
        assertEquals(30L,next.getSourceApplicationId());assertEquals(actual,attributes.readValue(snapshots.find("UPLOAD",20L,1).getActualAttributesJson()));
    }
    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO newWorking(Long id,Long predecessor){return cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder()
        .id(id).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L).sourceFileId(100L).originalFileId(100L).fileName("SOP.pdf").title("SOP").fileNumber("N-1")
        .sourceOriginalFileName("SOP.pdf").sourceSha256("body-hash").fileTypeTaxonomyId(6L).versionNo(id==40L?"A/1-2":"A/1-1").requesterId(99L).submitterId(99L).processType("CONTROLLED_FILE").changeType("NEW").status("WORKING").dccProjectCodeId(5L).predecessorControlledFileId(predecessor).build();}
    @Test void h02AuthorizedOrdinarySaveUsesOnlyTheExistingReservedSnapshotAndKeepsItsSource() {
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',process_instance_id=NULL,process_definition_key=NULL WHERE id=20");
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(20L),actual));
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(actual));
        assertDoesNotThrow(()->new TransactionTemplate(manager).executeWithoutResult(s->saveThroughA(99L,20L,original)),"ordinary attribute save must have an authorized A service entry");
        var saved=bridge.readReservedDraft(99L,5L,"UPLOAD",20L);
        assertEquals(original,attributes.readValue(saved.getDefaultSourceJson()));assertEquals(original,attributes.readValue(saved.getActualAttributesJson()));
        assertFalse(saved.getSubmitted());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE bpm_round IS NULL",Integer.class));
    }
    private void saveThroughA(Long actor,Long file,DccProjectAttributes value) {
        workflow.saveWorkingApplicationAttributes(actor,file,value);
    }
    @ParameterizedTest @ValueSource(strings={"OTHER_APPLICANT","FOREIGN_TENANT","BOUND_BPM","FROZEN_STATUS","NO_RESERVE"})
    void h02OrdinarySaveCannotBypassItsRealFileOrCreateAnUnreservedSnapshot(String violation) {
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',process_instance_id=NULL,process_definition_key=NULL WHERE id=20");
        if(!"NO_RESERVE".equals(violation))new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(20L),actual));
        var before=snapshots.find("UPLOAD",20L,1);String actualBefore=before==null?null:before.getActualAttributesJson();
        Long actor="OTHER_APPLICANT".equals(violation)?100L:99L;
        if("FOREIGN_TENANT".equals(violation))TenantContextHolder.setTenantId(122L);
        if("BOUND_BPM".equals(violation))jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='actual-bound' WHERE id=20");
        if("FROZEN_STATUS".equals(violation))jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_REVIEW' WHERE id=20");
        assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->saveThroughA(actor,20L,original)));
        TenantContextHolder.setTenantId(1L);var after=snapshots.find("UPLOAD",20L,1);
        assertEquals(actualBefore,after==null?null:after.getActualAttributesJson());
        assertEquals("NO_RESERVE".equals(violation)?0:1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
    }
    @Test void h02OrdinarySaveLateOuterFailureRestoresTheSavedActualAndSource() {
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',process_instance_id=NULL,process_definition_key=NULL WHERE id=20");
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(20L),actual));
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->{saveThroughA(99L,20L,original);throw new IllegalStateException("late save failure");}));
        var saved=bridge.readReservedDraft(99L,5L,"UPLOAD",20L);assertEquals(actual,attributes.readValue(saved.getActualAttributesJson()));
        assertEquals(original,attributes.readValue(saved.getDefaultSourceJson()));
    }
    @Test void h02PublicFileCreationCapturesRealDefaultsAndActualBeforeAnyBpmExists() {
        var ticket=configurePublicCreation();var request=creationRequest();
        long id=new TransactionTemplate(manager).execute(s->workflow.createWorkingControlledFile(99L,request));
        var created=files.selectById(id);assertEquals("WORKING",created.getStatus());assertNull(created.getProcessInstanceId());
        var saved=bridge.readReservedDraft(99L,5L,"UPLOAD",id);
        assertEquals(original,attributes.readValue(saved.getDefaultSourceJson()));assertEquals(actual,attributes.readValue(saved.getActualAttributesJson()));
        assertFalse(saved.getSubmitted());Long replay=new TransactionTemplate(manager).execute(s->workflow.createWorkingControlledFile(99L,creationRequest()));assertEquals(Long.valueOf(id),replay);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        verify(ticket,times(1)).markBound(any());
    }
    @Test void h02PublicFileCreationLateTicketFailureRollsBackTheRealFileMasterClaimRoundAndAttributes() {
        var ticket=configurePublicCreation();doThrow(new IllegalStateException("late ticket binding failure")).when(ticket).markBound(any());
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->workflow.createWorkingControlledFile(99L,creationRequest())));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_master",Integer.class));
        for(String table:List.of("dcc_controlled_file_name_claim","dcc_application_round_link","dcc_project_application_attributes"))
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class));
    }
    @Test void h02UnsubmittedSourceMustKeepItsSavedSourceWhenAnotherRealFileIsAllocated() {
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',process_instance_id=NULL,process_definition_key=NULL WHERE id=20");
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(20L),actual));
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(actual));
        var selected=newWorking(30L,20L);files.insert(selected);
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,selected,null));
        var saved=bridge.readReservedDraft(99L,5L,"UPLOAD",30L);
        assertEquals(original,attributes.readValue(saved.getDefaultSourceJson()),"unsent source snapshot must not become today's defaults");
        assertEquals(actual,attributes.readValue(saved.getActualAttributesJson()));
    }
    @Test void h08TwoDirectPredecessorsKeepNmpaAndCeAfterDefaultsBecomeFda() {
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',process_instance_id=NULL,process_definition_key=NULL WHERE id=20");
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(20L),actual));
        var fda=new DccProjectAttributes(List.of("FDA"),null,"N","N","N",null);
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(fda));
        long previous=20L;
        for(long id:new long[]{30L,40L}) {
            var target=newWorking(id,previous);files.insert(target);
            new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,target,null));
            var saved=bridge.readReservedDraft(99L,5L,"UPLOAD",id);
            assertEquals(original,attributes.readValue(saved.getDefaultSourceJson()));assertEquals(actual,attributes.readValue(saved.getActualAttributesJson()));
            assertEquals(previous,saved.getSourceApplicationId());previous=id;
        }
    }
    @Test void h08InitializerReplayNeverOverwritesAUsersLaterActualWithTheOldRequest() {
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',process_instance_id=NULL,process_definition_key=NULL WHERE id=20");
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(20L),actual));
        var target=newWorking(30L,20L);files.insert(target);
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,target,actual));
        new TransactionTemplate(manager).executeWithoutResult(s->bridge.saveReservedDraft(99L,5L,"UPLOAD",30L,original));
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,target,actual));
        assertEquals(original,attributes.readValue(bridge.readReservedDraft(99L,5L,"UPLOAD",30L).getActualAttributesJson()));
    }
    @Test void h08MissingDirectPredecessorSnapshotDoesNotWalkBackOrRecreateTodaysDefaults() {
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',process_instance_id=NULL,process_definition_key=NULL WHERE id=20");
        var target=newWorking(30L,20L);files.insert(target);
        assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,target,null)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));
    }
    @Test void publicWorkingReadAndExplicitRestoreKeepTheOriginalSavedSource() {
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(20L),actual));
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(actual));
        Object read=new TransactionTemplate(manager).execute(s->ReflectionTestUtils.invokeMethod(workflow,"readWorkingApplicationAttributes",99L,20L));
        assertEquals(original,ReflectionTestUtils.invokeMethod(read,"defaultSource"));
        assertEquals(actual,ReflectionTestUtils.invokeMethod(read,"actual"));
        new TransactionTemplate(manager).executeWithoutResult(s->ReflectionTestUtils.invokeMethod(workflow,"restoreWorkingApplicationAttributes",99L,20L));
        var saved=bridge.readReservedDraft(99L,5L,"UPLOAD",20L);
        assertEquals(original,attributes.readValue(saved.getDefaultSourceJson()));
        assertEquals(original,attributes.readValue(saved.getActualAttributesJson()));
        assertNull(files.selectById(20L).getProcessInstanceId());
    }
    @Test void publicWorkingReadRejectsAnotherApplicantAndMissingReservation() {
        assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).execute(s->ReflectionTestUtils.invokeMethod(workflow,"readWorkingApplicationAttributes",99L,20L)));
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(20L),actual));
        assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).execute(s->ReflectionTestUtils.invokeMethod(workflow,"readWorkingApplicationAttributes",100L,20L)));
    }
    @Test void publicWorkingCheckedOutDraftCanBeReadButCannotSubmitOrRestore(){
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(20L),actual));
        jdbc.update("UPDATE dcc_controlled_file SET checked_out_by=99 WHERE id=20");
        var saved=new TransactionTemplate(manager).execute(s->workflow.readWorkingApplicationAttributes(99L,20L));
        assertFalse(saved.canSubmit());assertNotNull(saved.unavailableReason());assertEquals(actual,saved.actual());
        assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->workflow.restoreWorkingApplicationAttributes(99L,20L)));
    }
    cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService configurePublicCreation() {
        jdbc.update("UPDATE dcc_project_code SET project_code='SNAPSHOT-P' WHERE id=5");
        var categories=mock(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class);
        when(categories.selectById(2L)).thenReturn(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder().id(2L).code("SOP").active(true).fileTypeTaxonomyId(6L).build());
        var bindings=mock(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper.class);
        when(bindings.selectActiveByCategoryId(2L)).thenReturn(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO.builder().id(1L).categoryId(2L).directoryId(3L).active(true).build());
        var directories=mock(cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper.class);
        when(directories.selectEnabledList()).thenReturn(List.of(cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO.builder().id(3L).active(true).name("SOP").build()));
        var taxonomy=mock(cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService.class);
        var path=new cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath(6L,"1","2","3",null,null);
        when(taxonomy.resolveActivePath(6L)).thenReturn(path);when(taxonomy.listActiveDescendantIds(6L)).thenReturn(List.of(6L));when(taxonomy.listActiveDescendantPaths(6L)).thenReturn(List.of(path));
        var claims=new DccControlledFileNameClaimService();org.springframework.test.util.ReflectionTestUtils.setField(claims,"reservationMapper",g25Reservations);wire(claims,"masterMapper",masters,"claimMapper",nameClaims,"fileMapper",files);
        var sources=mock(DccControlledFileSourceOwnershipService.class);
        when(sources.prepareSubmissionSource(100L,false)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"real-fixture-source-hash",false));
        var physical=mock(cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper.class);
        when(physical.selectById(100L)).thenReturn(cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO.builder().id(100L).name("SOP.pdf").type("application/pdf").build());
        var ticket=mock(cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService.class);
        when(ticket.resolveForBinding(any())).thenReturn(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile("test-ticket",100L,"SOP.pdf","application/pdf",8L));
        var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Singapore");
        wire(workflow,"categoryMapper",categories,"categoryDirectoryBindingMapper",bindings,"directoryMapper",directories,"projectCodeMapper",projects,
            "fileTypeTaxonomyAdminService",taxonomy,"projectFileTemplateService",mock(cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectFileTemplateService.class),
            "nameClaimService",claims,"revisionService",new DccControlledFileRevisionServiceImpl(),"sourceOwnershipService",sources,"fileMapper",physical,
            "uploadTicketService",ticket,"relatedFileService",mock(DccControlledFileRelatedFileService.class),"attachmentService",mock(DccControlledFileAttachmentService.class),
            "submitMutex",new DccControlledFileSubmitMutex(),"workflowDatePolicy",dates);
        return ticket;
    }
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNameClaimMapper nameClaims;
    cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO creationRequest(){var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO();
        request.setCategoryId(2L);request.setDirectoryId(3L);request.setDccProjectCodeId(5L);request.setFileTypeTaxonomyId(6L);
        request.setChangeType("NEW");request.setProcessType("CONTROLLED_FILE");request.setFileName("SOP");request.setFileNumber("N-2");request.setEffectiveDate(java.time.LocalDate.now().plusDays(15));
        request.setProjectAttributes(actual);request.setOriginalUploadTicket("test-ticket");request.setSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.newUploadPrefix(5L,6L,"SOP"),"new-draft"));request.setIdempotencyKey("h02-real-create");return request;}
    private void bindInitialBpm(){jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_REVIEW',process_instance_id='opaque-bpm-round',process_definition_key='dcc-controlled-file-upload' WHERE id=20");}
    private void freeze(DccProjectAttributes value){new TransactionTemplate(manager).executeWithoutResult(s->
            {workflow.prepareApplicationDraft(99L,files.selectById(20L),value);bindInitialBpm();workflow.freezeApplicationAttributes(99L,files.selectById(20L),value);});}
    private void wire(Object target,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(target,(String)pairs[i],pairs[i+1]);}
}
