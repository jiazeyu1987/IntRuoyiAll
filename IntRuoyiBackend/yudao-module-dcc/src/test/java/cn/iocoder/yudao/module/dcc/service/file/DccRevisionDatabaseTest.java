package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckoutReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckinReqVO;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Modifier;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** H2 proves isolated transaction/locking behavior only, never MySQL character-index semantics. */
@Import({DccControlledFileRevisionServiceImpl.class, DccControlledFileNameClaimService.class,
        DccControlledFileVersionPolicy.class, DccControlledFileVersionPolicyProperties.class,DccObsoleteRetentionService.class,
        DccControlledFileAssignmentScopeService.class, DccWorkflowDatePolicy.class})
class DccRevisionDatabaseTest extends BaseDbUnitTest {
    @Resource private DccControlledFileRevisionService revisionService;
    @Resource private DccControlledFileNameClaimService claimService;
    @Resource private DccObsoleteRetentionService retentionService;
    @Resource private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Resource private DccWorkflowDatePolicy datePolicy;
    @Resource(name = "dccControlledFileMapper") private DccControlledFileMapper fileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccControlledFileCheckoutMapper checkoutMapper;
    @Resource private DccControlledFileVersionPolicy policy;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private DataSource dataSource;
    @MockBean private DccProjectAccessService projectAccessService;
    @MockBean private DccControlledFileCategoryPermissionSupport permissionSupport;
    @MockBean private DccControlledFileSourceOwnershipService sourceOwnershipService;
    @MockBean private DccControlledFileRelatedFileService relatedFileService;
    @MockBean private DccPublicUploadPlacementService publicUploadPlacementService;
    @MockBean private cn.iocoder.yudao.module.system.api.permission.PermissionApi scopePermissionApi;
    @MockBean private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService bpmProcessInstanceService;
    @MockBean private cn.iocoder.yudao.module.bpm.service.task.BpmTaskService bpmTaskService;
    private JdbcTemplate jdbc;
    private final LocalDateTime obsolete = LocalDateTime.of(2026,9,30,12,0);
    private final LocalDateTime deadline = obsolete.plusDays(17); // Explicit test fixture, no production default.
    private final DccControlledFilePreparedSource frozen = new DccControlledFilePreparedSource(300L,100L,"hash",true);

    @BeforeEach void fixtures() throws Exception {
        datePolicy.setZoneId("Asia/Singapore");
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
        TenantContextHolder.setTenantId(1L);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,status,current_active_controlled_file_id,latest_controlled_file_id,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number) VALUES(10,1,2,3,'template','SOP-001','ACTIVE_CHAIN',20,20,5,6,'SOP-001')");
        fileMapper.insert(file(20L,"A/1","ACTIVE"));
        fileMapper.insert(file(21L,"A/1-1","WORKING"));
        when(permissionSupport.hasCategoryPermission(2L,99L,DccFileCategoryPermissionActionEnum.UPLOAD)).thenReturn(true);
        when(sourceOwnershipService.createVerifiedCopy(100L)).thenReturn(frozen);
        when(sourceOwnershipService.rollbackCleanup(any())).thenAnswer(c->{var source=c.<DccControlledFilePreparedSource>getArgument(0);return (Runnable)()->sourceOwnershipService.cleanupPreparedSource(source);});
    }

    @Test void outerSubmissionFailureRollsBackAllocatedRowAndCleansFrozenBody() {
        assertThrows(IllegalStateException.class, () -> tx().execute(status -> {
            revisionService.createRevision(99L,20L,21L,request("PARTIAL","outer-failure"));
            throw new IllegalStateException("approval start failed");
        }));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(20L, masterMapper.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        verify(sourceOwnershipService).cleanupPreparedSource(frozen);
    }
    @Test void childFailureActuallyRollsBackRootInsert() {
        doThrow(new IllegalStateException("relationship failed")).when(relatedFileService).inheritRelatedFiles(eq(21L),anyLong());
        assertThrows(RuntimeException.class, () -> revisionService.createRevision(99L,20L,21L,request("PARTIAL","child")));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        verify(sourceOwnershipService).cleanupPreparedSource(frozen);
    }
    @Test void formalFactsPersistAndReplayWithoutNewSource() {
        var result=revisionService.createRevision(99L,20L,21L,request("PARTIAL","replay"));
        var stored=fileMapper.selectById(result.getId());
        assertEquals("PARTIAL",stored.getRevisionChangeType()); assertEquals("A/1",stored.getRevisionSourceVersionNo());
        assertEquals("A/1-1",stored.getSelectedIterationVersionNo()); assertEquals(300L,stored.getSourceFileId());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE master_id=10 AND source_original_file_name='SOP.pdf' AND normalized_file_number='SOP-001' AND deleted=0",Integer.class));
        assertEquals(result.getId(),revisionService.createRevision(99L,20L,21L,request("PARTIAL","replay")).getId());
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
        assertThrows(RuntimeException.class, () -> revisionService.createRevision(99L,20L,21L,request("REPLACEMENT","replay")));
    }
    private void newInitialChain() {
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL,latest_controlled_file_id=NULL WHERE id=10");
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',change_type='NEW',controlled_time=NULL WHERE master_id=10");
        jdbc.update("UPDATE dcc_controlled_file SET revision_change_type='INITIAL' WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file SET predecessor_controlled_file_id=20,revision_base_active_controlled_file_id=NULL WHERE id=21");
    }
    @Test void initialCandidatePersistsSelectedBodyAndDateWhileBothOriginalNamesRemainUnchanged() {
        newInitialChain();
        var created=revisionService.createInitialCandidate(99L,21L,request("INITIAL","first-initial"));
        assertEquals("A/1",created.getVersionNo()); assertEquals("INITIAL",created.getRevisionChangeType());
        assertEquals("A/1",fileMapper.selectById(20L).getVersionNo());
        assertEquals("A/1-1",fileMapper.selectById(21L).getVersionNo());
        assertEquals(100L,fileMapper.selectById(20L).getSourceFileId());
        assertEquals(100L,fileMapper.selectById(21L).getSourceFileId());
        assertEquals(300L,fileMapper.selectById(created.getId()).getSourceFileId());
        assertEquals(request("INITIAL","first-initial").getEffectiveDate(),fileMapper.selectById(created.getId()).getEffectiveDate());
        assertNull(masterMapper.selectById(10L).getCurrentActiveControlledFileId());
        assertNull(masterMapper.selectById(10L).getLatestControlledFileId());
        assertEquals(created.getId(),revisionService.createInitialCandidate(99L,21L,request("INITIAL","first-initial")).getId());
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
    }
    @Test void initialCandidateConcurrentAllocationHasOneWinnerAndKeepsInitialRoleUnique() throws Exception {
        newInitialChain();
        assertEquals(1,race(i -> revisionService.createInitialCandidate(99L,21L,request("INITIAL","initial-"+i))));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE revision_change_type='INITIAL' AND selected_iteration_controlled_file_id IS NOT NULL",Integer.class));
        var duplicate=file(null,"A/1","WORKING");duplicate.setRevisionChangeType("INITIAL");duplicate.setSelectedIterationControlledFileId(21L);
        assertThrows(org.springframework.dao.DuplicateKeyException.class,() -> fileMapper.insert(duplicate));
    }
    @Test void initialCandidateOuterLateFailureRollsBackClaimCandidateAndCleansOnlyCopy() {
        newInitialChain();
        assertThrows(IllegalStateException.class,() -> tx().execute(status -> {
            revisionService.createInitialCandidate(99L,21L,request("INITIAL","late-failure"));
            throw new IllegalStateException("BPM creation failed after freeze");
        }));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        assertEquals(100L,fileMapper.selectById(21L).getSourceFileId());
        verify(sourceOwnershipService).cleanupPreparedSource(frozen);
    }
    @Test void firstRealCheckinThenInitialSubmissionFreezesNewSelectedIterationWithoutBaseline() {
        newInitialChain();
        jdbc.update("DELETE FROM dcc_controlled_file WHERE id=21"); // This isolated scenario starts before the first checkin.
        var query=queryWithRealLifecycleMappers();
        var reason=new DccControlledFileCheckoutReqVO();reason.setReason("first new chain edit");
        tx().execute(status -> query.checkoutControlledFile(99L,20L,reason));
        when(sourceOwnershipService.prepareSubmissionSource(100L,false)).thenReturn(new DccControlledFilePreparedSource(301L,100L,"hash",true));
        var edit=new DccControlledFileCheckinReqVO();edit.setRemark("first edit");edit.setChangeDescription("new chain first checkin");
        var working=tx().execute(status -> query.checkinControlledFile(99L,20L,edit));
        assertEquals("A/1-1",working.getVersionNo());
        when(sourceOwnershipService.createVerifiedCopy(301L)).thenReturn(new DccControlledFilePreparedSource(302L,301L,"hash",true));
        var candidate=revisionService.createInitialCandidate(99L,working.getId(),request("INITIAL","first-submission"));
        assertEquals("A/1",candidate.getVersionNo());assertEquals(302L,candidate.getSourceFileId());
        assertNull(candidate.getRevisionSourceControlledFileId());assertNull(candidate.getRevisionBaseActiveControlledFileId());
        assertEquals("A/1-1",fileMapper.selectById(working.getId()).getVersionNo());
        assertEquals("A/1",fileMapper.selectById(20L).getVersionNo());
        assertNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));
    }
    @Test void actualDatePersistsAndOwnRevisionHashRejectsChangedDateOrMissingZone() {
        jdbc.update("UPDATE dcc_controlled_file SET effective_date='2026-10-01' WHERE id=21");
        var input=request("PARTIAL","date-replay");
        var created=revisionService.createRevision(99L,20L,21L,input);
        assertEquals(input.getEffectiveDate(),fileMapper.selectById(created.getId()).getEffectiveDate());
        assertEquals(java.time.LocalDate.of(2026,10,1),fileMapper.selectById(21L).getEffectiveDate());
        input.setEffectiveDate(input.getEffectiveDate().plusDays(1));
        assertThrows(IllegalArgumentException.class,() -> revisionService.createRevision(99L,20L,21L,input));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
        datePolicy.setZoneId(null);
        var exact = request("PARTIAL", "date-replay");
        assertEquals(created.getId(), revisionService.createRevision(99L,20L,21L,exact).getId());
        assertThrows(IllegalStateException.class, () -> revisionService.createRevision(99L,20L,21L,request("PARTIAL","missing-zone-new")));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
    }
    @Test void revisionPreparationMatchesRealAllocationAndShowsFrozenCandidateWithoutWriting() {
        var query = queryWithRealLifecycleMappers();
        when(projectAccessService.hasProjectEditorOrOwner(99L, 5L)).thenReturn(true);
        when(projectAccessService.hasProjectOwner(99L, 5L)).thenReturn(true);
        var options = tx().execute(status -> query.getRevisionOptions(99L, 20L));
        assertEquals("A/2", options.partialTarget().versionNo());
        assertEquals("B/1", options.replacementTarget().versionNo());
        assertEquals(21L, options.iterations().get(0).id());
        assertTrue(options.iterations().get(0).canPartial());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
        var created = revisionService.createRevision(99L, options.controlledBaselineId(), options.iterations().get(0).id(), request("PARTIAL", "options-submit"));
        assertEquals(options.partialTarget().versionNo(), created.getVersionNo());
        var pending = tx().execute(status -> query.getRevisionOptions(99L, 20L));
        assertEquals("FORMAL_REVISION_IN_PROGRESS", pending.lockedReason());
        assertFalse(pending.iterations().get(0).canPartial());
        assertFalse(pending.iterations().get(0).canReplacement());
        assertEquals("TARGET_ALREADY_ALLOCATED", pending.partialTarget().unavailableReason());
        assertEquals(1, pending.iterations().size());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        verify(sourceOwnershipService, times(1)).createVerifiedCopy(100L);
    }
    @Test void missingTrainingFactCannotCreateFormalCandidateOrReservation() {
        var input = request("PARTIAL", "training-choice"); input.setNeedTraining(null);
        assertThrows(IllegalArgumentException.class, () -> revisionService.createRevision(99L, 20L, 21L, input));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
        var created = revisionService.createRevision(99L, 20L, 21L, request("PARTIAL", "training-choice"));
        assertFalse(fileMapper.selectById(created.getId()).getNeedTraining());
        var changed = request("PARTIAL", "training-choice"); changed.setNeedTraining(true);
        assertThrows(IllegalArgumentException.class, () -> revisionService.createRevision(99L, 20L, 21L, changed));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        verify(sourceOwnershipService, times(1)).createVerifiedCopy(100L);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(longs = {19L})
    void selectedOriginMismatchRejectsBeforeClaimAndCandidateInTheRealDatabase(Long recordedSource) {
        jdbc.update("UPDATE dcc_controlled_file SET revision_base_active_controlled_file_id=? WHERE id=21", recordedSource);
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> revisionService.createRevision(99L, 20L, 21L, request("PARTIAL", "recorded-source")),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
    }
    @Test void concurrentFormalAllocationCreatesOneCandidate() throws Exception {
        int winners=race(i -> revisionService.createRevision(99L,20L,21L,request("PARTIAL","key-"+i)));
        assertEquals(1,winners);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE version_no='A/2'",Integer.class));
    }
    @Test void concurrentCheckoutUsesNativeMasterLockAndSingleActiveConstraint() throws Exception {
        int winners=race(i -> tx().execute(status -> {
            masterMapper.selectByIdForUpdate(10L);
            if(checkoutMapper.selectActiveByMasterId(1L,10L)!=null) throw new IllegalStateException("checked out");
            var lock=DccControlledFileCheckoutDO.builder().masterId(10L).baseIterationId(21L).actorId((long)i)
                    .reason("concurrent checkout").baseSourceSha256("hash").status("ACTIVE").build();
            lock.setTenantId(1L); checkoutMapper.insert(lock); return lock;
        }));
        assertEquals(1,winners);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_checkout WHERE status='ACTIVE'",Integer.class));
    }
    @Test void concurrentWorkingAllocationLocksAndProducesUniqueHyphenNumbers() throws Exception {
        int winners=race(i -> tx().execute(status -> {
            masterMapper.selectByIdForUpdate(10L);
            var base=fileMapper.selectByIdAndTenantForUpdate(1L,21L);
            var version=policy.nextWorking(base,fileMapper.selectListByMasterIdForUpdate(10L));
            var next=file(null,version.display(),"WORKING"); fileMapper.insert(next); return next;
        }));
        assertEquals(2,winners);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE version_no='A/1-2'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE version_no='A/1-3'",Integer.class));
    }
    @Test void publicCheckoutAndFormalRevisionRaceShareTheSameMasterLock() throws Exception {
        var query=queryWithRealLifecycleMappers();
        var reason=new DccControlledFileCheckoutReqVO(); reason.setReason("same chain edit");
        int winners=race(actor -> actor==101
                ? tx().execute(status -> query.checkoutControlledFile(99L,21L,reason))
                : revisionService.createRevision(99L,20L,21L,request("PARTIAL","race")));
        assertEquals(1,winners);
        assertEquals(1,jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM dcc_controlled_file_checkout WHERE status='ACTIVE') + (SELECT COUNT(*) FROM dcc_controlled_file WHERE revision_change_type='PARTIAL')",Integer.class));
    }
    @Test void formalConflictReadsLogicalActorAndDoesNotChangeOtherWorkingLock() {
        var later = file(22L, "A/1-2", "WORKING"); later.setPredecessorControlledFileId(21L);
        fileMapper.insert(later);
        var query = queryWithRealLifecycleMappers();
        var reason = new DccControlledFileCheckoutReqVO(); reason.setReason("edit another working iteration");
        tx().execute(status -> query.checkoutControlledFile(99L, 22L, reason));
        assertNull(fileMapper.selectById(21L).getCheckedOutBy());
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> revisionService.createRevision(99L, 20L, 21L, request("PARTIAL", "locked-elsewhere")),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ALREADY_CHECKED_OUT, 99L);
        assertEquals(99L, checkoutMapper.selectActiveByMasterIdForRead(1L, 10L).getActorId());
        assertEquals(22L, checkoutMapper.selectActiveByMasterIdForRead(1L, 10L).getBaseIterationId());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
    }
    @Test void publicCheckinChildFailureRollsBackVersionAndPreservesCheckoutLock() {
        var query=queryWithRealLifecycleMappers();
        var reason=new DccControlledFileCheckoutReqVO(); reason.setReason("edit selected body");
        tx().execute(status -> query.checkoutControlledFile(99L,21L,reason));
        var source=new DccControlledFilePreparedSource(301L,100L,"hash",true);
        when(sourceOwnershipService.prepareSubmissionSource(100L,false)).thenReturn(source);
        doThrow(new IllegalStateException("child checkin failure")).when(relatedFileService).inheritRelatedFiles(eq(21L),anyLong());
        var request=new DccControlledFileCheckinReqVO(); request.setChangeDescription("remark changed"); request.setRemark("new metadata");
        assertThrows(IllegalStateException.class, () -> tx().execute(status -> query.checkinControlledFile(99L,21L,request)));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(99L,fileMapper.selectById(21L).getCheckedOutBy());
        assertEquals("ACTIVE",checkoutMapper.selectActiveByMasterId(1L,10L).getStatus());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        verify(sourceOwnershipService).cleanupPreparedSource(source);
    }
    @Test void publicCheckinSuccessCommitsTheClaimAndClearsOnlyItsLogicalLock() {
        var query=queryWithRealLifecycleMappers();
        var reason=new DccControlledFileCheckoutReqVO(); reason.setReason("edit selected body");
        tx().execute(status -> query.checkoutControlledFile(99L,21L,reason));
        when(sourceOwnershipService.prepareSubmissionSource(100L,false))
                .thenReturn(new DccControlledFilePreparedSource(301L,100L,"hash",true));
        var request=new DccControlledFileCheckinReqVO(); request.setChangeDescription("remark changed"); request.setRemark("new metadata");
        var result=tx().execute(status -> query.checkinControlledFile(99L,21L,request));
        assertEquals("A/1-2",result.getVersionNo()); assertNull(result.getRevisionChangeType());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE source_original_file_name='SOP.pdf' AND normalized_file_number='SOP-001' AND deleted=0",Integer.class));
        assertNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));
        assertNull(fileMapper.selectById(21L).getCheckedOutBy());
        assertEquals(20L,masterMapper.selectById(10L).getCurrentActiveControlledFileId());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"version", "missing", "obsolete", "sourceName", "missingOrigin"})
    void checkinOriginFailurePreservesRealCheckoutAndDoesNotReserveOrCopy(String invalid) {
        var query = queryWithRealLifecycleMappers();
        var checkout = new DccControlledFileCheckoutReqVO(); checkout.setReason("working revision edit");
        tx().execute(status -> query.checkoutControlledFile(99L, 21L, checkout));
        jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION' WHERE id=21");
        switch (invalid) {
            case "version" -> jdbc.update("UPDATE dcc_controlled_file SET version_no='A/2-1' WHERE id=21");
            case "missing" -> jdbc.update("UPDATE dcc_controlled_file SET deleted=1 WHERE id=20");
            case "obsolete" -> jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=20");
            case "sourceName" -> jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='other.pdf' WHERE id=20");
            case "missingOrigin" -> jdbc.update("UPDATE dcc_controlled_file SET revision_base_active_controlled_file_id=NULL WHERE id=21");
        }
        var input = new DccControlledFileCheckinReqVO(); input.setChangeDescription("actual origin guard");
        input.setRemark("metadata change");
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> tx().execute(status -> query.checkinControlledFile(99L, 21L, input)),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        assertEquals(99L, checkoutMapper.selectActiveByMasterIdForRead(1L, 10L).getActorId());
        assertEquals(21L, checkoutMapper.selectActiveByMasterIdForRead(1L, 10L).getBaseIterationId());
        assertEquals(99L, fileMapper.selectById(21L).getCheckedOutBy());
        verify(sourceOwnershipService, never()).prepareSubmissionSource(anyLong(), anyBoolean());
    }
    @Test void concurrentSameNameHasOneOwner() throws Exception {
        additionalMaster(101L); additionalMaster(102L);
        assertEquals(1,race(i -> { claimService.claimIdentity(1L,"SOP.pdf",(long)i,6L,"SOP-001",(long)i); return true; }));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE deleted=0",Integer.class));
    }
    @Test void exactBinaryIdentityAndNumberGuardUseIndependentKeys() {
        for (long id=11;id<=15;id++) additionalMaster(id);
        jdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=id-4 WHERE id BETWEEN 11 AND 13");
        claimService.claimExistingVersion(1L,"SOP.pdf",5L,6L,"SOP-001",10L,20L);
        claimService.claimIdentity(1L,"sop.pdf",7L,6L,"SOP-001",11L);
        claimService.claimIdentity(1L,"SOP.PDF",8L,6L,"SOP-001",12L);
        claimService.claimIdentity(1L,"SOP.docx",9L,6L,"SOP-001",13L);
        assertThrows(RuntimeException.class, () -> claimService.claimIdentity(1L,"SOP.pdf",99L,6L,"different",14L));
        assertThrows(RuntimeException.class, () -> claimService.claimIdentity(1L,"new.pdf",5L,6L,"SOP-001",15L));
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
    }
    @Test void retentionRejectsNewChainAndAllowsSameChain() {
        additionalMaster(11L);
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline.minusSeconds(1)));
        claimService.claimExistingVersion(1L,"SOP.pdf",5L,6L,"SOP-001",10L,20L);
        assertThrows(RuntimeException.class, () -> claimService.claimIdentity(1L,"SOP.pdf",5L,6L,"SOP-002",11L));
        assertThrows(RuntimeException.class, () -> claimService.claimIdentity(1L,"new.pdf",5L,6L,"SOP-001",11L));
    }
    @Test void expiredRetentionCannotReleaseStillActiveOrWorkingChain() {
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline));
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=20");
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline));
    }
    @Test void expiredUnusedIdentityReleasesBothKeysWithoutDeletingHistory() {
        additionalMaster(11L);
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE'");
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL,status='OBSOLETE_CHAIN' WHERE id=10");
        assertTrue(claimService.releaseExpiredIdentity(1L,10L,deadline));
        claimService.claimIdentity(1L,"SOP.pdf",5L,6L,"SOP-001",11L);
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
    }
    @Test void stillAssignedExecutionPointerCannotBeReleasedEvenWithTerminalRows() {
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE'");
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE deleted=0",Integer.class));
    }
    @Test void replayCannotShortenRetentionAndMissingDeadlineIsRejected() {
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        claimService.retainObsoleteIdentity(1L,10L,obsolete,obsolete.plusDays(1));
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> claimService.retainObsoleteIdentity(1L,10L,obsolete,null));
        assertThrows(IllegalArgumentException.class, () -> claimService.retainObsoleteIdentity(1L,10L,obsolete,obsolete));
    }
    @Test void unresolvedHistoricalSourceIdentityBlocksNewClaims() {
        additionalMaster(11L);
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(tenant_id,master_id,normalized_name) VALUES(1,10,'legacy-template')");
        assertThrows(IllegalStateException.class, () -> claimService.claimIdentity(1L,"SOP.pdf",5L,6L,"SOP-001",11L));
    }
    @Test void wrongTenantCannotReserveOrReleaseIdentity() {
        assertThrows(IllegalArgumentException.class, () -> claimService.claimIdentity(2L,"SOP.pdf",5L,6L,"SOP-001",10L));
        assertThrows(IllegalArgumentException.class, () -> claimService.releaseExpiredIdentity(2L,10L,deadline));
    }
    @Test void concurrentSameChainReuseAndExpiryReleaseKeepInUseIdentityReserved() throws Exception {
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE'");
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL,status='OBSOLETE_CHAIN' WHERE id=10");
        assertEquals(2,race(actor -> actor==101 ? tx().execute(status -> {
            masterMapper.selectByIdForUpdate(10L);
            claimService.claimExistingVersion(1L,"SOP.pdf",5L,6L,"SOP-001",10L,20L);
            fileMapper.insert(file(null,"A/2","ACTIVE")); return true;
        }) : claimService.releaseExpiredIdentity(1L,10L,deadline)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE deleted=0",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE status='ACTIVE'",Integer.class));
    }
    @Test void confirmedTwentyYearServiceUsesRealClaimAndRollsBackWithCaller() {
        reserve();
        assertThrows(IllegalStateException.class,() -> tx().execute(status -> {
            retentionService.retain(1L,10L,obsolete);throw new IllegalStateException("obsolete approval failed");
        }));
        assertNull(jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE master_id=10",java.sql.Timestamp.class));
        tx().execute(status -> {retentionService.retain(1L,10L,obsolete);return null;});
        assertEquals(obsolete.plusYears(20),jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE master_id=10",java.sql.Timestamp.class).toLocalDateTime());
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,obsolete.plusYears(20).minusSeconds(1)));
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,obsolete.plusYears(20)));
    }
    @Test void actualScopeServiceStopsFormalAndCheckoutBeforeWritingInMemoryDatabase() {
        when(scopePermissionApi.hasAnyPermissions(99L, "dcc:project-code-assignment:execute")).thenReturn(true);
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> revisionService.createRevision(99L, 20L, 21L, request("PARTIAL", "out-of-scope")),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        var query = queryWithRealLifecycleMappers();
        var checkout = new DccControlledFileCheckoutReqVO(); checkout.setReason("outside assigned scope");
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> tx().execute(status -> query.checkoutControlledFile(99L, 20L, checkout)),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_checkout", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
        verify(sourceOwnershipService, never()).prepareSubmissionSource(anyLong(), anyBoolean());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"CHECKOUT,project", "CHECKOUT,taxonomy", "CHECKOUT,number",
            "CHECKIN,project", "CHECKIN,taxonomy", "CHECKIN,number"})
    void realLifecycleRequiresFormalMasterIdentityAndOwnerCanStillCancel(String action, String changed) {
        var query = queryWithRealLifecycleMappers();
        var checkout = new DccControlledFileCheckoutReqVO(); checkout.setReason("edit working iteration");
        if ("CHECKIN".equals(action)) tx().execute(status -> query.checkoutControlledFile(99L, 21L, checkout));
        switch (changed) {
            case "project" -> jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=55 WHERE id=21");
            case "taxonomy" -> jdbc.update("UPDATE dcc_controlled_file SET file_type_taxonomy_id=66 WHERE id=21");
            case "number" -> jdbc.update("UPDATE dcc_controlled_file SET file_number='OTHER-001' WHERE id=21");
        }
        if ("CHECKOUT".equals(action)) {
            cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                    () -> tx().execute(status -> query.checkoutControlledFile(99L, 21L, checkout)),
                    cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKOUT_NOT_ALLOWED);
        } else {
            var request = new DccControlledFileCheckinReqVO(); request.setChangeDescription("metadata edit");
            request.setRemark("changed metadata");
            cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                    () -> tx().execute(status -> query.checkinControlledFile(99L, 21L, request)),
                    cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        assertEquals("CHECKIN".equals(action) ? 1 : 0,
                jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_checkout WHERE status='ACTIVE'", Integer.class));
        verify(sourceOwnershipService, never()).prepareSubmissionSource(anyLong(), anyBoolean());
        if ("CHECKIN".equals(action)) {
            var cancel = new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCancelCheckoutReqVO();
            cancel.setReason("abandon inconsistent working identity");
            tx().execute(status -> query.cancelCheckoutControlledFile(99L, 21L, cancel));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_checkout WHERE status='ACTIVE'", Integer.class));
            assertNull(fileMapper.selectById(21L).getCheckedOutBy());
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        }
    }

    @Test void legacyRevisionUsesVerifiedReadProjectionAndSuccessfulCandidateNeverBackfillsOldRows() {
        installLegacyScope();
        when(sourceOwnershipService.createVerifiedCopy(100L)).thenReturn(new DccControlledFilePreparedSource(300L,100L,"a".repeat(64),true));
        var candidate=revisionService.createRevision(99L,20L,21L,request("PARTIAL","legacy-body"));
        assertEquals("A/2",candidate.getVersionNo());assertEquals("SOP.pdf",candidate.getSourceOriginalFileName());
        assertNull(fileMapper.selectById(20L).getSourceOriginalFileName());assertNull(fileMapper.selectById(21L).getSourceOriginalFileName());
        assertNull(jdbc.queryForObject("SELECT source_original_file_name FROM dcc_controlled_file_name_claim WHERE master_id=10",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
    }
    @Test void legacyPublicCheckinAllocatesModernWorkingBodyAndNeverWritesOldSourceNames() {
        installLegacyScope();var query=queryWithRealLifecycleMappers();
        when(sourceOwnershipService.inspectSource(100L)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"a".repeat(64),false));
        var reason=new DccControlledFileCheckoutReqVO();reason.setReason("edit verified historic body");tx().execute(s->query.checkoutControlledFile(99L,21L,reason));
        when(sourceOwnershipService.prepareSubmissionSource(100L,false)).thenReturn(new DccControlledFilePreparedSource(301L,100L,"a".repeat(64),true));
        var input=new DccControlledFileCheckinReqVO();input.setChangeDescription("verified selected version");input.setRemark("updated metadata");
        var result=tx().execute(s->query.checkinControlledFile(99L,21L,input));
        assertEquals("A/1-2",result.getVersionNo());assertEquals("SOP.pdf",fileMapper.selectById(result.getId()).getSourceOriginalFileName());
        assertNull(fileMapper.selectById(20L).getSourceOriginalFileName());assertNull(fileMapper.selectById(21L).getSourceOriginalFileName());
        assertNull(jdbc.queryForObject("SELECT source_original_file_name FROM dcc_controlled_file_name_claim WHERE master_id=10",String.class));
        assertNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));
    }
    void installLegacyScope() {
        jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name=NULL,source_sha256=? WHERE master_id=10","a".repeat(64));
        jdbc.update("INSERT INTO infra_file(id,config_id,name,path,size) VALUES(100,28,'SOP.pdf','sealed/body',4)");
        jdbc.update("INSERT INTO infra_file_config(id,storage,config) VALUES(28,20,?)","{\"endpoint\":\"http://127.0.0.1:9000\",\"bucket\":\"originals\",\"region\":\"us-east-1\",\"enablePathStyleAccess\":true}");
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(id,tenant_id,master_id,normalized_name) VALUES(7,1,10,'old-template')");
        jdbc.update("INSERT INTO dcc_legacy_source_name_scope(id,tenant_id,scope_id,manifest_sha256,facts_sha256,bytes_receipt_sha256,user_decision_sha256,scope_identity_sha256,status,claim_count,version_count,source_count,edge_count,name_count,verified_at,activated_at,actor_id,reason,request_id) VALUES(1,1,'legacy',?,?,?,?,?,'VERIFIED',1,2,1,1,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,99,'verified historic files','legacy')","b".repeat(64),"c".repeat(64),"d".repeat(64),"e".repeat(64),"f".repeat(64));
        for(long id:new long[]{20,21})jdbc.update("INSERT INTO dcc_legacy_source_name_evidence(tenant_id,verification_scope_id,legacy_claim_id,legacy_master_id,controlled_file_id,source_file_id,config_id,source_path,storage_type,storage_endpoint,storage_bucket,storage_region,storage_path_style,version_no,source_original_file_name,expected_sha256,actual_sha256,expected_size,actual_size,bytes_status,expected_version_count,claim_normalized_name,master_project_id,master_leaf_id,master_number,metadata_identity_sha256,preimage_sha256,proof_row_sha256) VALUES(1,1,7,10,?,100,28,'sealed/body',20,'http://127.0.0.1:9000','originals','us-east-1',1,?,'SOP.pdf',?,?,4,4,'MATCH',2,'old-template',5,6,'SOP-001',?,?,?)",id,id==20?"A/1":"A/1-1","a".repeat(64),"a".repeat(64),"b".repeat(64),"c".repeat(64),"d".repeat(64));
        jdbc.update("INSERT INTO dcc_source_name_reservation(tenant_id,source_original_file_name,reservation_kind,verification_scope_id,generation,active) VALUES(1,'SOP.pdf','LEGACY_GROUP',1,1,1)");
    }
    @AfterEach void cleanLegacyStorage() {jdbc.update("DELETE FROM infra_file WHERE id=100");jdbc.update("DELETE FROM infra_file_config WHERE id=28");}
    private void reserve() { claimService.claimIdentity(1L,"SOP.pdf",5L,6L,"SOP-001",10L); }
    private void additionalMaster(Long id) {
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,status,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number) VALUES(?,1,2,3,?,'SOP-001','ACTIVE_CHAIN',?,6,'SOP-001')",id,"master-"+id,id>=100?id:5L);
    }
    private DccControlledFileQueryServiceImpl queryWithRealLifecycleMappers() {
        var query=new DccControlledFileQueryServiceImpl();
        for (var field:DccControlledFileQueryServiceImpl.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && (field.getAnnotation(Resource.class)!=null
                    || field.getAnnotation(org.springframework.beans.factory.annotation.Autowired.class)!=null)) {
                ReflectionTestUtils.setField(query,field.getName(),mock(field.getType()));
            }
        }
        ReflectionTestUtils.setField(query,"controlledFileMapper",fileMapper);
        ReflectionTestUtils.setField(query,"controlledFileMasterMapper",masterMapper);
        var projects=mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class);
        var project=cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO.builder()
                .id(5L).status("ENABLE").build();project.setTenantId(1L);
        when(projects.selectByIdForUpdate(5L)).thenReturn(project);
        ReflectionTestUtils.setField(query,"projectCodeMapper",projects);
        ReflectionTestUtils.setField(query,"checkoutMapper",checkoutMapper);
        ReflectionTestUtils.setField(query,"versionPolicy",policy);
        ReflectionTestUtils.setField(query,"sourceOwnershipService",sourceOwnershipService);
        ReflectionTestUtils.setField(query,"relatedFileService",relatedFileService);
        ReflectionTestUtils.setField(query,"permissionSupport",permissionSupport);
        ReflectionTestUtils.setField(query,"projectAccessService",projectAccessService);
        ReflectionTestUtils.setField(query,"nameClaimService",claimService);
        ReflectionTestUtils.setField(query,"assignmentScopeService",assignmentScopeService);
        var directory=mock(DccDirectoryAccessPermissionService.class);
        when(directory.hasDirectoryManagementPermission(99L)).thenReturn(true);
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",directory);
        when(sourceOwnershipService.inspectSource(100L)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"hash",false));
        return query;
    }
    private TransactionTemplate tx() { return new TransactionTemplate(transactionManager); }
    private DccControlledFileSubmitIterationReqVO request(String intent,String key) {
        var request=new DccControlledFileSubmitIterationReqVO(); request.setRevisionChangeType(intent);
        request.setIdempotencyKey(key); request.setChangeDescription("selected body"); request.setNeedTraining(false);
        request.setEffectiveDate(java.time.LocalDate.of(2099,12,20));
        request.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(
                java.util.List.of("CE"), null, "Y", "N", "N", null));
        request.setSelectedSignoffDepartmentIds(java.util.List.of(101L, 102L));
        return request;
    }
    private DccControlledFileDO file(Long id,String version,String status) {
        return DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L)
                .dccProjectCodeId(5L).fileTypeTaxonomyId(6L).fileNumber("SOP-001").fileName("template")
                .sourceOriginalFileName("SOP.pdf").sourceFileId(100L).originalFileId(100L).title("SOP").sourceSha256("hash")
                .requesterId(99L).submitterId(99L).versionNo(version).status(status)
                .revisionBaseActiveControlledFileId(version.contains("-") ? 20L : null)
                .controlledTime("WORKING".equals(status) ? null : java.time.LocalDateTime.of(2026,10,1,12,0)).build();
    }
    private interface Operation { Object call(int actor) throws Exception; }
    private int race(Operation operation) throws Exception {
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1); var winners=new AtomicInteger();
        try {
            var first=pool.submit(() -> run(start,winners,operation,101));
            var second=pool.submit(() -> run(start,winners,operation,102));
            start.countDown(); first.get(15,TimeUnit.SECONDS); second.get(15,TimeUnit.SECONDS);
            return winners.get();
        } finally { pool.shutdownNow(); }
    }
    private void run(CountDownLatch start,AtomicInteger winners,Operation operation,int actor) {
        TenantContextHolder.setTenantId(1L);
        try { start.await(); operation.call(actor); winners.incrementAndGet(); }
        catch (cn.iocoder.yudao.framework.common.exception.ServiceException | IllegalArgumentException | IllegalStateException conflict) {
            // Expected losing business request; database/environment failures are not accepted as conflicts.
        } catch (Exception failure) { throw new RuntimeException(failure); }
        finally { TenantContextHolder.clear(); }
    }
}
