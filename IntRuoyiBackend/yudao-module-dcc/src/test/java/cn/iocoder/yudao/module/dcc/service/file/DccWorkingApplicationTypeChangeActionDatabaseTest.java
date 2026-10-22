package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFilePlacementMapper;
import cn.iocoder.yudao.module.dcc.service.category.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual action/name/proof/round services and SQL transactions. Directory, storage and signature APIs are declared external ports. */
class DccWorkingApplicationTypeChangeActionDatabaseTest extends DccWorkingApplicationTypeChangeDatabaseTest {
    @Resource DccControlledFileNameClaimMapper claims;
    @Resource DccSourceNameReservationMapper reservations;
    @Resource DccControlledFileSourceOwnershipMapper ownershipRows;
    @Resource DccControlledFileCheckoutMapper checkoutRows;
    @Resource DccCategoryDirectoryBindingMapper bindingRows;
    @Resource DccFileDirectoryMapper directoryRows;
    @Resource DccProjectFilePlacementMapper placementRows;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderStorageMappingMapper actionStorageMappings;
    DccWorkingApplicationTypeChangeService action;
    DccControlledFileSourceOwnershipService storage;
    AdminUserApi users;
    DccControlledFileApprovalRouteAssigneeResolver actionResolver;

    // Shared clean.sql already clears claims/reservations/bindings/directories, but not this owned storage fixture.
    @org.junit.jupiter.api.BeforeEach void clearOwnedActionSource(){
        jdbc.update("DELETE FROM dcc_controlled_file_source_ownership WHERE tenant_id=1 AND id=500 AND controlled_file_id=22 AND claimed_by=99");
    }

    void actionFixture() throws Exception {
        rejectedCopies();targetType();wire(contracts,"typeChanges",typeChanges);
        cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.setLoginUser(
                new cn.iocoder.yudao.framework.security.core.LoginUser().setId(99L).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
        directoryRows.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO.builder()
                .id(3L).tenantId(1L).parentId(0L).code("TYPE_CHANGE_DIRECTORY").name("Task directory").active(true).build());
        for(long category:List.of(2L,3L)){
            bindingRows.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO.builder()
                    .id(500L+category).categoryId(category).directoryId(3L).active(true).build());
            jdbc.update("UPDATE dcc_category_directory_binding SET tenant_id=1 WHERE category_id=?",category);
        }
        ownershipRows.insert(DccControlledFileSourceOwnershipDO.builder().id(500L).tenantId(1L).controlledFileId(22L)
                .sourceFileId(100L).originSourceFileId(100L).sourceSha256("a".repeat(64)).claimedBy(99L).claimedTime(LocalDateTime.now()).ownershipType("CHECKIN").build());
        claims.insert(DccControlledFileNameClaimDO.builder().id(600L).tenantId(1L).masterId(10L).normalizedName("SOP.pdf")
                .sourceOriginalFileName("SOP.pdf").dccProjectCodeId(5L).fileTypeTaxonomyLeafId(6L).normalizedFileNumber("PREPARED-1").build());
        assertEquals(1,reservations.insertModern(1L,"SOP.pdf",600L,10L));
        var names=new DccControlledFileNameClaimService();wire(names,"claimMapper",claims,"reservationMapper",reservations,"masterMapper",masters,"fileMapper",files);
        var placements=new DccPublicUploadPlacementService();wire(placements,"projects",projects,"files",files,"access",access,"placementRows",placementRows,"storageMappings",actionStorageMappings);
        storage=mock(DccControlledFileSourceOwnershipService.class);
        when(storage.inspectSource(100L)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"a".repeat(64),false));
        var taxonomy=mock(DccFileTypeTaxonomyAdminService.class);
        when(taxonomy.resolveActiveCategoryId(6L)).thenReturn(2L);when(taxonomy.resolveActiveCategoryId(8L)).thenReturn(3L);
        when(taxonomy.resolveActivePath(8L)).thenReturn(new DccFileTypeTaxonomyPath(8L,"Documents","Output","C10","Configuration","T2"));
        users=mock(AdminUserApi.class);
        when(users.getUser(anyLong())).thenAnswer(c->new AdminUserRespDTO().setId(c.getArgument(0)).setTenantId(1L).setStatus(0).setPostIds(Set.of(10L)).setDeptId(51L).setNickname("Task user"));
        when(users.getUserList(anyCollection())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(id->new AdminUserRespDTO()
                .setId(id).setTenantId(1L).setStatus(0).setPostIds(Set.of(10L)).setNickname("Task user "+id)).toList());
        var departments=mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class);
        when(departments.getDeptList(anyCollection())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(id->
                new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(id).setName("Task dept "+id).setStatus(0).setLeaderUserId(id==51L?88L:89L)).toList());
        var positions=mock(cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver.class);
        when(positions.isUploaderDerivedPosition(anyLong())).thenReturn(true);when(positions.resolveUserIds(anyLong(),anyLong(),anyBoolean())).thenReturn(List.of(101L));
        var resolver=new DccControlledFileApprovalRouteAssigneeResolver();wire(resolver,"adminUserApi",users,"deptApi",departments,"positionRuntimeResolver",positions);
        var organization=mock(DccDirectSupervisorSource.class);
        when(organization.require(any())).thenReturn(new DccDirectSupervisorSource.Evidence(1L,99L,88L,"TASK_ORGANIZATION","declared-relation-99","v1"));
        var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(organization);
        wire(resolver,"nativeSupervisors",provider,"nativeSourceTokens",tokens);actionResolver=resolver;
        var permission=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(permission.hasAnyPermissions(anyLong(),any(String[].class))).thenReturn(true);
        var auth=mock(DccElectronicSignatureAuthorizationService.class);when(auth.getAuthorizationMap(anyCollection())).thenAnswer(c->{var m=new HashMap<Long,Boolean>();c.<Collection<Long>>getArgument(0).forEach(id->m.put(id,true));return m;});
        var images=mock(DccElectronicSignatureImageService.class);when(images.requireActiveSnapshot(anyLong())).thenReturn(DccElectronicSignatureImageSnapshot.builder().imageId(10L).fileId(10L).verifiedStatus("VALID").build());
        var ready=new DccControlledFileRouteReadinessService();wire(ready,"adminUserApi",users,"permissionApi",permission,"signatureAuthorizationService",auth,"signatureImageService",images);
        action=new DccWorkingApplicationTypeChangeService();wire(action,"files",files,"masters",masters,"projects",projects,"categories",categories,
                "bindings",bindingRows,"directories",directoryRows,"taxonomies",taxonomy,"access",access,"permissions",permissions,
                "ownership",ownershipRows,"sourceOwnership",storage,"checkouts",checkoutRows,"placements",placements,"names",names,
                "typeChanges",typeChanges,"contracts",contracts,"rounds",rounds,"matrices",matrixSources,"resolver",resolver,"readiness",ready,"departments",departments,
                "placementRows",placementRows,"storageMappings",actionStorageMappings);
        wire(workflow,"approvalRouteAssigneeResolver",resolver,"routeReadinessService",ready);
    }

    @org.junit.jupiter.api.AfterEach void clearActionLogin(){clearOwnedActionSource();org.springframework.security.core.context.SecurityContextHolder.clearContext();}

    DccWorkingApplicationTypeChangeReqVO previewRequest(){var r=new DccWorkingApplicationTypeChangeReqVO();r.setTargetFileTypeTaxonomyId(8L);return r;}
    DccWorkingApplicationTypeChangeReqVO command(){
        var p=tx().execute(s->action.preview(99L,22L,previewRequest()));var r=previewRequest();
        r.setConfirmed(true);r.setRequestId(REQUEST);r.setChangeReason("Explicit applicant T1 to T2");
        r.setExpectedSourceAttributeRound(p.sourceAttributeRound());r.setExpectedSourceCategoryId(Long.valueOf(p.sourceCategoryId()));
        r.setExpectedSourceTaxonomyId(Long.valueOf(p.sourceTaxonomyId()));r.setExpectedSourceSha256(p.sourceSha256());r.setExpectedSourceMatrixDigest(p.sourceMatrixDigest());
        r.setExpectedApprovalRouteId(p.targetMatrix().getRouteId());r.setExpectedApprovalRouteVersionNo(p.targetMatrix().getRouteVersionNo());r.setExpectedApprovalMatrixDigest(p.targetMatrix().getMatrixDigest());
        r.setSelectedSignoffDepartmentIds(p.selectedSignoffDepartmentIds().stream().map(Long::valueOf).toList());
        r.setSelectedDistributionDepartmentIds(p.selectedDistributionDepartmentIds().stream().map(Long::valueOf).toList());return r;
    }
    void unchanged(Map<String,Object> before){assertEquals(before,original(22L));assertEquals(6L,masters.selectById(10L).getFileTypeTaxonomyLeafId());
        assertEquals(6L,claims.selectById(600L).getFileTypeTaxonomyLeafId());assertEquals(0,typeChanges.selectListBySource(SOURCE).size());}

    @Test void previewAndCancelDoNotWriteIdentityClaimOrProof() throws Exception {
        actionFixture();var before=original(22L);var old=original(20L);var sourceRows=routeSnapshots.selectListByControlledFileId(20L);
        var result=tx().execute(s->action.preview(99L,22L,previewRequest()));
        assertEquals(32L,result.beforeMatrix().getRouteId());assertEquals(44L,result.targetMatrix().getRouteId());assertTrue(result.readiness().getReady());
        unchanged(before);assertEquals(old,original(20L));assertEquals(sourceRows,routeSnapshots.selectListByControlledFileId(20L));
    }
    @Test void concreteConfirmAtomicallyKeepsSameBodyAndHistoricalRows() throws Exception {
        actionFixture();var old=original(20L);var intermediate=original(21L);var r=command();
        var result=tx().execute(s->action.confirm(99L,22L,r));assertEquals("22",result.controlledFileId());
        assertEquals("2",result.sourceCategoryId());assertEquals("6",result.sourceTaxonomyId());
        assertEquals("3",result.targetCategoryId());assertEquals("8",result.targetTaxonomyId());
        assertEquals(32L,result.beforeMatrix().getRouteId());assertEquals(44L,result.targetMatrix().getRouteId());
        assertEquals(10L,files.selectById(22L).getMasterId());assertEquals(100L,files.selectById(22L).getSourceFileId());
        assertEquals("PREPARED-1",files.selectById(22L).getFileNumber());assertEquals("a".repeat(64),files.selectById(22L).getSourceSha256());
        assertEquals(8L,masters.selectById(10L).getFileTypeTaxonomyLeafId());assertEquals(8L,claims.selectById(600L).getFileTypeTaxonomyLeafId());
        assertEquals(600L,reservations.readName(1L,"SOP.pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8)).getModernClaimId());
        assertEquals(1,typeChanges.selectListBySource(SOURCE).size());assertEquals(old,original(20L));assertEquals(intermediate,original(21L));
        assertEquals(21L,attributeRows.find("UPLOAD",22L,1).getSourceApplicationId());assertEquals(3,files.selectListByMasterId(10L).size());
    }
    @Test void exactReplayIsReadOnlyAndPayloadConflictIsRejected() throws Exception {
        actionFixture();var r=command();tx().execute(s->action.confirm(99L,22L,r));var saved=original(22L);
        var proof=typeChanges.selectListBySource(SOURCE).get(0);tx().execute(s->action.confirm(99L,22L,r));
        assertEquals(saved,original(22L));assertEquals(List.of(proof),typeChanges.selectListBySource(SOURCE));
        r.setChangeReason("A different payload");var failure=assertThrows(IllegalArgumentException.class,()->tx().execute(s->action.confirm(99L,22L,r)));
        assertEquals("WORKING_TYPE_CHANGE_IDEMPOTENCY_CONFLICT",failure.getMessage());assertEquals(saved,original(22L));
    }
    @ParameterizedTest @ValueSource(strings={"HASH","ROUND","MATRIX","TARGET_PERMISSION","ACTOR","STORAGE_HASH","BINDING","FORMAL"})
    void concreteFailureHasExplicitReasonAndZeroPartialWrites(String damage) throws Exception {
        actionFixture();var r=command();String reason;
        switch(damage){
            case "HASH" -> {r.setExpectedSourceSha256("b".repeat(64));reason="SOURCE_EXPECTATION_STALE";}
            case "ROUND" -> {r.setExpectedSourceAttributeRound(2);reason="SOURCE_EXPECTATION_STALE";}
            case "MATRIX" -> {r.setExpectedApprovalMatrixDigest("b".repeat(64));reason="TARGET_MATRIX_EXPECTATION_STALE";}
            case "TARGET_PERMISSION" -> {when(permissions.hasCategoryPermission(3L,99L,DccFileCategoryPermissionActionEnum.UPLOAD)).thenReturn(false);reason="UPLOAD_PERMISSION_DENIED";}
            case "ACTOR" -> {reason="SOURCE_INVALID";}
            case "STORAGE_HASH" -> {when(storage.inspectSource(100L)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"b".repeat(64),false));reason="SOURCE_HASH_OR_OWNERSHIP_INVALID";}
            case "BINDING" -> {jdbc.update("DELETE FROM dcc_category_directory_binding WHERE category_id=3");reason="TARGET_BINDING_INVALID";}
            case "FORMAL" -> {jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP WHERE id=20");reason="UNRELEASED_NEW_REQUIRED";}
            default -> throw new AssertionError(damage);
        }
        var before=original(22L);var failure=assertThrows(IllegalArgumentException.class,()->tx().execute(s->action.confirm(damage.equals("ACTOR")?98L:99L,22L,r)));
        assertEquals("WORKING_TYPE_CHANGE_"+reason,failure.getMessage());unchanged(before);
    }
    @Test void targetNumberConflictDoesNotRebindCurrentIdentity() throws Exception {
        actionFixture();var r=command();
        masters.insert(DccControlledFileMasterDO.builder().id(11L).tenantId(1L).categoryId(3L).directoryId(3L).fileName("Other").fileNumber("PREPARED-1")
                .dccProjectCodeId(5L).fileTypeTaxonomyLeafId(8L).normalizedFileNumber("PREPARED-1").status("ACTIVE_CHAIN").build());
        var before=original(22L);var failure=assertThrows(IllegalArgumentException.class,()->tx().execute(s->action.confirm(99L,22L,r)));
        assertEquals("WORKING_TYPE_CHANGE_IDENTITY_CONFLICT",failure.getMessage());unchanged(before);
    }
    @Test void lateFailureRollsBackFileMasterClaimAndProofTogether() throws Exception {
        actionFixture();var r=command();var before=original(22L);var old=original(20L);
        var failure=assertThrows(IllegalStateException.class,()->tx().executeWithoutResult(s->{action.confirm(99L,22L,r);throw new IllegalStateException("TEST_LATE_FAILURE");}));
        assertEquals("TEST_LATE_FAILURE",failure.getMessage());unchanged(before);assertEquals(old,original(20L));
    }
    @Test void disabledTargetParticipantBlocksConfirmBeforeAnyWrite() throws Exception {
        actionFixture();var r=command();
        when(users.getUserList(anyCollection())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(id->new AdminUserRespDTO()
                .setId(id).setTenantId(1L).setStatus(id==77L?1:0).setPostIds(Set.of(10L)).setNickname("Task user "+id)).toList());
        var before=original(22L);var failure=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx().execute(s->action.confirm(99L,22L,r)));
        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_READY.getCode(),failure.getCode());
        assertTrue(failure.getMessage().contains("停用"));unchanged(before);
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void rejectedCheckinSaveUsesThePreviewedFrozenMatrixAndRejectsForgedProof(boolean forged) throws Exception {
        actionFixture();
        var file=files.selectById(22L);var history=original(20L);var before=original(22L);
        var rows=routeSnapshots.selectListByControlledFileId(20L);
        var previewRequest=rejectedPreviewRequest();
        previewRequest.setExecutionContract(file.getPreparedWorkflowContract());
        previewRequest.setExpectedProcessDefinitionId(file.getPreparedProcessDefinitionId());
        previewRequest.setExpectedDefinitionDigest(file.getPreparedDefinitionDigest());
        previewRequest.setSelectedSignoffDepartmentIds(List.of(51L));
        var preview=tx().execute(s->workflow.previewRoute(99L,previewRequest));
        assertTrue(preview.getReady());assertFalse(preview.getMatrixChangeRequired());
        assertEquals(32L,preview.getRouteId());assertEquals(33L,matrixSources.read(2L).baseline().getRouteId());
        var save=new DccWorkingApplicationSaveReqVO();save.setActual(actual);
        save.setEffectiveMode("AFTER_STANDARDIZATION_SIGNATURE");save.setNeedTraining(false);
        save.setSelectedSignoffDepartmentIds(List.of(51L));save.setSelectedDistributionDepartmentIds(List.of(52L));
        save.setChangeDescription("原驳回申请沿冻结矩阵保存重提草稿");
        save.setExecutionContract(file.getPreparedWorkflowContract());
        save.setExpectedProcessDefinitionId(file.getPreparedProcessDefinitionId());
        save.setExpectedDefinitionDigest(file.getPreparedDefinitionDigest());
        save.setApplicationContextKey(previewRequest.getApplicationContextKey());
        save.setApprovalMatrixBaselineToken(preview.getApprovalMatrixBaselineToken());
        save.setExpectedSupervisorSourceDigest(preview.getSupervisorSourceDigest());
        save.setSupervisorSourceToken(forged?"forged-source-token":preview.getSupervisorSourceToken());
        if(forged){
            assertThrows(IllegalArgumentException.class,()->tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,22L,save)));
            assertEquals(before,original(22L));
        }else{
            assertDoesNotThrow(()->tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,22L,save)));
            var reopened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,22L));
            assertEquals(save.getChangeDescription(),files.selectById(22L).getChangeDescription());
            assertEquals(List.of("51"),reopened.selectedSignoffDepartmentIds());
            assertEquals(List.of("52"),reopened.selectedDistributionDepartmentIds());
            assertEquals(canonicalMatrix(2),files.selectById(22L).getDraftApprovalMatrixJson());
        }
        assertEquals(history,original(20L));assertEquals(rows,routeSnapshots.selectListByControlledFileId(20L));
    }

    @Test void legalListSaveKeepsConfirmedTargetAndOriginalSupervisorAfterPublicChanges() throws Exception {
        actionFixture();var r=command();tx().execute(s->action.confirm(99L,22L,r));changeTargetPublicMatrix();
        var file=files.selectById(22L);String context="c10-target-body-22";var signoff=List.of(51L,52L);
        var nativeSource=actionResolver.resolvePublicNative(targetMatrix,99L,"NEW",22L,5L,context,
                file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest(),signoff);
        var compared=tx().execute(s->baselines.preview(99L,targetRequest(22L)));
        var save=new DccWorkingApplicationSaveReqVO();save.setActual(actual);save.setEffectiveMode("AFTER_STANDARDIZATION_SIGNATURE");
        save.setSelectedSignoffDepartmentIds(signoff);save.setSelectedDistributionDepartmentIds(List.of(52L,53L));save.setNeedTraining(false);
        save.setExecutionContract(file.getPreparedWorkflowContract());save.setExpectedProcessDefinitionId(file.getPreparedProcessDefinitionId());save.setExpectedDefinitionDigest(file.getPreparedDefinitionDigest());
        save.setApplicationContextKey(context);save.setApprovalMatrixBaselineToken(compared.baselineToken());
        save.setExpectedSupervisorSourceDigest(nativeSource.sourceDigest());save.setSupervisorSourceToken(nativeSource.sourceToken());
        assertDoesNotThrow(()->tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,22L,save)));
        assertEquals("[\"51\",\"52\"]",files.selectById(22L).getSelectedSignoffDepartmentIdsJson());
        assertEquals("[\"52\",\"53\"]",files.selectById(22L).getDistributionDepartmentIdsJson());
        assertEquals(targetMatrix.canonical(),tx().execute(s->baselines.preview(99L,targetRequest(22L))).after().canonical());
        assertEquals(List.of(88L),nativeSource.route().nodes().get(0).resolvedUserIds());
    }

    String rejectedTypeChangedInitialAndCorrection() throws Exception {
        return rejectedTypeChangedInitialAndCorrection(true);
    }
    String rejectedTypeChangedInitialAndCorrection(boolean ended) throws Exception {
        actionFixture();var command=command();tx().executeWithoutResult(s->action.confirm(99L,22L,command));
        var formal=new DccControlledFileDO();org.springframework.beans.BeanUtils.copyProperties(files.selectById(22L),formal);
        formal.setId(23L);formal.setVersionNo("A/1");formal.setPredecessorControlledFileId(22L);
        formal.setSelectedIterationControlledFileId(22L);formal.setSelectedIterationVersionNo(files.selectById(22L).getVersionNo());
        formal.setRevisionChangeType("INITIAL");formal.setPreviousSourceSha256(null);files.insert(formal);
        tx().executeWithoutResult(s->{contracts.inheritPrepared(99L,23L,22L,1);initializer.initialize(99L,22L,23L,null);});
        var source=files.selectById(23L);
        engine.getIdentityService().setAuthenticatedUserId("99");
        String bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(source.getPreparedProcessDefinitionId())
                .businessKey("23").variables(Map.of("controlledFileId",23L)).start().getId();
        engine.getIdentityService().setAuthenticatedUserId(null);
        tx().executeWithoutResult(s->snapshots.submitReservedDraft(99L,5L,"UPLOAD",23L,1,bpm,actual));
        var route=actionResolver.resolvePublicNative(targetMatrix,99L,"NEW",23L,5L,"c10-formal23",source.getPreparedProcessDefinitionId(),source.getPreparedDefinitionDigest(),List.of(51L)).route();
        var snapshotWriter=new DccControlledFileWorkflowServiceImpl();wire(snapshotWriter,"routeSnapshotMapper",routeSnapshots);
        tx().executeWithoutResult(s->org.springframework.test.util.ReflectionTestUtils.invokeMethod(snapshotWriter,"persistApprovalRouteSnapshots",23L,route));
        jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED',process_instance_id=?,process_definition_key=? WHERE id=23",bpm,DccControlledFileProcessDefinitionKeys.UPLOAD);
        if(ended)engine.getRuntimeService().deleteProcessInstance(bpm,"task-owned rejected initial after type change");
        var correction=new DccControlledFileDO();org.springframework.beans.BeanUtils.copyProperties(files.selectById(22L),correction);
        correction.setId(24L);correction.setVersionNo("A/1-3");correction.setPredecessorControlledFileId(23L);
        correction.setSourceFileId(101L);correction.setOriginalFileId(101L);correction.setSourceSha256("b".repeat(64));
        files.insert(correction);
        tx().executeWithoutResult(s->{initializer.initialize(99L,23L,24L,null);contracts.inheritPrepared(99L,24L,23L,1);});
        return bpm;
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void rejectedSubmittedTypeChangeHistoryUsesBoundRoundAndRejectsForgedBpm(boolean forged) throws Exception {
        rejectedTypeChangedInitialAndCorrection();
        if(forged)jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='forged-bpm' WHERE id=23");
        var history=original(20L);var formal=original(23L);var proof=typeChanges.selectListBySource(SOURCE);
        if(forged)assertThrows(RuntimeException.class,()->tx().execute(s->contracts.isRecordedNewTypeChangeHistory(99L,files.selectById(24L),files.selectById(20L))));
        else assertEquals(Boolean.TRUE,tx().execute(s->contracts.isRecordedNewTypeChangeHistory(99L,files.selectById(24L),files.selectById(20L))));
        assertEquals(history,original(20L));assertEquals(formal,original(23L));assertEquals(proof,typeChanges.selectListBySource(SOURCE));
    }

    DccControlledFileRevisionServiceImpl initialRetryService(){
        var revision=new DccControlledFileRevisionServiceImpl();
        for(var f:revision.getClass().getDeclaredFields())if(f.isAnnotationPresent(Resource.class))wire(revision,f.getName(),mock(f.getType()));
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(eq(99L),anyLong())).thenReturn(true);
        when(storage.createVerifiedCopy(101L)).thenReturn(new DccControlledFilePreparedSource(2101L,101L,"b".repeat(64),true));
        when(storage.rollbackCleanup(any())).thenReturn(()->{});
        var attachments=mock(DccControlledFileAttachmentService.class);
        when(attachments.readOrdinaryAttachmentManifest(anyLong(),anyLong())).thenReturn(new DccControlledFileAttachmentService.OrdinaryAttachmentManifest(List.of(),"e".repeat(64)));
        var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Shanghai");
        wire(revision,"fileMapper",files,"masterMapper",masters,"checkoutMapper",checkoutRows,"projectAccessService",access,
                "permissionSupport",permissions,"assignmentScopeService",scope,"sourceOwnershipService",storage,
                "versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"datePolicy",dates,"applicationRoundService",rounds,
                "executionContracts",contracts,"attachmentService",attachments,"bpmProcessInstanceService",processes);
        return revision;
    }
    DccControlledFileSubmitIterationReqVO initialRetryRequest(){
        var selected=files.selectById(24L);var r=new DccControlledFileSubmitIterationReqVO();
        r.setRevisionChangeType("INITIAL");r.setIdempotencyKey("c10-rejected-initial-retry-24");r.setChangeDescription("Frozen T2 rejected initial correction");
        r.setNeedTraining(false);r.setEffectiveMode("AFTER_STANDARDIZATION_SIGNATURE");r.setProjectAttributes(actual);
        r.setSelectedSignoffDepartmentIds(List.of(51L));r.setSelectedDistributionDepartmentIds(List.of(52L));
        r.setExecutionContract(selected.getPreparedWorkflowContract());r.setExpectedProcessDefinitionId(selected.getPreparedProcessDefinitionId());
        r.setExpectedDefinitionDigest(selected.getPreparedDefinitionDigest());r.setExpectedOrdinaryAttachmentIds(List.of());
        r.setExpectedOrdinaryAttachmentManifestHash("e".repeat(64));return r;
    }
    @ParameterizedTest @ValueSource(strings={"VALID","UNRELATED_INITIAL","RUNNING_BPM","SELECTED_POINTER","SELECTED_VERSION","ATTEMPT","LATE_FAILURE"})
    void rejectedTypeChangedInitialAllocatesOnlyItsExactNextAttempt(String damage) throws Exception {
        rejectedTypeChangedInitialAndCorrection(!damage.equals("RUNNING_BPM"));
        if(damage.equals("UNRELATED_INITIAL")){
            var other=new DccControlledFileDO();org.springframework.beans.BeanUtils.copyProperties(files.selectById(23L),other);
            other.setId(25L);other.setRevisionAttemptNo(2);other.setReworkPredecessorControlledFileId(23L);files.insert(other);
        }
        if(damage.equals("SELECTED_POINTER"))jdbc.update("UPDATE dcc_controlled_file SET selected_iteration_controlled_file_id=21 WHERE id=23");
        if(damage.equals("SELECTED_VERSION"))jdbc.update("UPDATE dcc_controlled_file SET selected_iteration_version_no='A/1-99' WHERE id=23");
        if(damage.equals("ATTEMPT"))jdbc.update("UPDATE dcc_controlled_file SET revision_attempt_no=9 WHERE id=23");
        var before=files.selectListByMasterId(10L).stream().map(f->original(f.getId())).toList();var proof=typeChanges.selectListBySource(SOURCE);
        var revision=initialRetryService();var request=initialRetryRequest();
        if(damage.equals("VALID")){
            var candidate=tx().execute(s->revision.createInitialCandidate(99L,24L,request));
            assertEquals("A/1",candidate.getVersionNo());assertEquals(2,candidate.getRevisionAttemptNo());
            assertEquals(23L,candidate.getReworkPredecessorControlledFileId());assertEquals(24L,candidate.getPredecessorControlledFileId());
            assertEquals(24L,candidate.getSelectedIterationControlledFileId());assertEquals("b".repeat(64),candidate.getSourceSha256());
            assertEquals(targetMatrix.canonical(),candidate.getDraftApprovalMatrixJson());
            assertEquals(candidate.getId(),tx().execute(s->revision.createInitialCandidate(99L,24L,request)).getId());
            verify(storage,times(1)).createVerifiedCopy(101L);
        }else{
            var failure=assertThrows(RuntimeException.class,()->tx().execute(s->{
                revision.createInitialCandidate(99L,24L,request);throw new IllegalStateException("C10_LATE_INITIAL_RETRY_FAILURE");}));
            if(damage.equals("LATE_FAILURE"))assertEquals("C10_LATE_INITIAL_RETRY_FAILURE",failure.getMessage());
            if(damage.equals("RUNNING_BPM"))assertEquals("failed attempt BPM must be terminal before resubmission",failure.getMessage());
            assertEquals(before.size(),files.selectListByMasterId(10L).size());
        }
        assertEquals(before,files.selectListByMasterId(10L).stream().filter(f->before.stream().anyMatch(row->Objects.equals(row.get("id"),f.getId())))
                .map(f->original(f.getId())).toList());
        assertEquals(proof,typeChanges.selectListBySource(SOURCE));
    }
}
