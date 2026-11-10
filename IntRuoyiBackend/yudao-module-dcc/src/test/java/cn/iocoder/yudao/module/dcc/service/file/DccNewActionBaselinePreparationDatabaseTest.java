package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real H2 raw matrix/attributes/CAS + registered PUBLIC two-key Flowable identities and real HMAC.
 * Candidate account/readiness are external directory ports; no runtime-ready supervisor claim. */
class DccNewActionBaselinePreparationDatabaseTest extends DccPreparedExecutionInheritanceDatabaseTest {
    @Resource DccFileCategoryMapper categories;
    @Resource DccCategoryApprovalRouteMapper rawRoutes;
    @Resource DccCategoryApprovalRouteNodeMapper rawNodes;
    @Resource DccProjectCodeMapper projects;
    DccControlledFileWorkflowServiceImpl workflow;
    DccApprovalMatrixSourceService matrixSources;
    DccApprovalMatrixBaselineService baselines;
    DccApprovalMatrixTokenService tokens;
    DccWorkflowDatePolicy dates;
    String revisionId;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper importOwnership;
    void importedRevisionBody() throws Exception {
        wire(contracts,"sourceOwnership",importOwnership);
        jdbc.update("DELETE FROM dcc_project_application_attributes WHERE application_id=20");
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=CURRENT_TIMESTAMP,effective_time=CURRENT_TIMESTAMP,activated_time=CURRENT_TIMESTAMP,effective_mode='CONTROLLED_IMPORT',import_source='CONTROLLED_IMPORT',imported_by=99,imported_time=CURRENT_TIMESTAMP,revision_change_type='INITIAL' WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=20 WHERE id=10");
        importOwnership.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO.builder()
                .tenantId(1L).controlledFileId(20L).sourceFileId(100L).originSourceFileId(100L).sourceSha256("a".repeat(64))
                .ownershipType("CONTROLLED_IMPORT").claimedBy(99L).claimedTime(java.time.LocalDateTime.now()).build());
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        revisionId=deploy(Files.readString(root.resolve("IntRuoyiBackend/config/dcc-workflow-v6/revision.bpmn20.xml")));
        when(definitions.getActiveProcessDefinition(DccControlledFileProcessDefinitionKeys.REVISION)).thenReturn(
                engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(revisionId).singleResult());
        add(21L,20L);
        jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION',revision_base_active_controlled_file_id=20,version_no='A/1-1' WHERE id=21");
        tx().executeWithoutResult(s->initializer.initialize(99L,20L,21L,null));
    }
    @Test void controlledImportStartsOwnRevisionAndRepeatedCheckinKeepsPinnedDefinition() throws Exception {
        importedRevisionBody();var before=original(20L);
        tx().executeWithoutResult(s->{
            var prepared=contracts.inheritPrepared(99L,21L,20L,null);
            assertEquals(revisionId,prepared.processDefinitionId());
            assertEquals(DccApplicationExecutionContractService.PUBLIC_V2,prepared.executionContract());
            assertEquals(prepared,contracts.verifyInheritedPrepared(99L,21L,20L,null));
            assertTrue(contracts.readInheritedDraftFacts(99L,21L,20L,null).newActionBaselineRequired());
            assertNull(contracts.readRejectedDraftFacts(99L,files.selectById(21L)));
        });
        nextUnpreparedRevisionBody();
        tx().executeWithoutResult(s->{
            assertEquals(revisionId,contracts.requireNewActionBaselinePreparation(99L,22L).processDefinitionId());
            assertTrue(contracts.readInheritedDraftFacts(99L,22L,21L,1).newActionBaselineRequired());
        });
        assertEquals(before,original(20L));assertNull(files.selectById(20L).getProcessInstanceId());
        verify(definitions,times(1)).getActiveProcessDefinition(DccControlledFileProcessDefinitionKeys.REVISION);
    }
    @ParameterizedTest @ValueSource(strings={"UNMARKED","INCOMPLETE","NOT_ACTIVE","NOT_CURRENT","WRONG_OWNER","WRONG_ARTIFACT","INVENTED_ROUND","MISSING_DEFINITION"})
    void controlledImportCannotBypassInvalidSourceOrMissingDefinition(String violation) throws Exception {
        importedRevisionBody();
        switch(violation){
            case "UNMARKED" -> jdbc.update("UPDATE dcc_controlled_file SET import_source=NULL WHERE id=20");
            case "INCOMPLETE" -> jdbc.update("UPDATE dcc_controlled_file SET imported_by=NULL WHERE id=20");
            case "NOT_ACTIVE" -> jdbc.update("UPDATE dcc_controlled_file SET status='CONTROLLED_PENDING_EFFECTIVE' WHERE id=20");
            case "NOT_CURRENT" -> jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL WHERE id=10");
            case "WRONG_OWNER" -> jdbc.update("UPDATE dcc_controlled_file_source_ownership SET claimed_by=88 WHERE controlled_file_id=20");
            case "WRONG_ARTIFACT" -> jdbc.update("UPDATE dcc_controlled_file_source_ownership SET source_file_id=101 WHERE controlled_file_id=20");
            case "MISSING_DEFINITION" -> when(definitions.getActiveProcessDefinition(anyString())).thenReturn(null);
            case "INVENTED_ROUND" -> { }
        }
        assertThrows(RuntimeException.class,()->tx().execute(s->contracts.inheritPrepared(99L,21L,20L,"INVENTED_ROUND".equals(violation)?1:null)));
        assertNull(files.selectById(21L).getPreparedProcessDefinitionId());
    }
    void freshCustomImport() throws Exception {
        importedRevisionBody();
        tx().executeWithoutResult(s->contracts.inheritPrepared(99L,21L,20L,null));
        jdbc.update("UPDATE dcc_file_category SET approval_source_mode='APPLICATION_CUSTOM' WHERE id=2");
        wire(workflow,"customMatrices",new DccApplicationCustomMatrixService(),"approvalMatrixTokens",tokens);
    }
    @Test void importedFreshCustomProjectionUsesOwnIdentityAndWritesNothingForBothRevisionIntents() throws Exception {
        freshCustomImport();var source=original(20L);var target=original(21L);
        var attributesBefore=jdbc.queryForList("SELECT * FROM dcc_project_application_attributes");
        var partial=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L));
        var replacement=tx().execute(s->workflow.readReplacementApplicationAttributes(99L,21L,20L));
        assertEquals(partial,replacement);assertEquals("APPLICATION_CUSTOM",partial.approvalSourceMode());
        assertEquals("dcc-custom-v1:1:REVISION:21:1",partial.applicationContextKey());assertEquals(1,partial.attributeRound());
        assertNull(partial.applicationCustomMatrix());assertNull(partial.draftApprovalMatrix());assertFalse(partial.canSubmit());
        assertEquals("本次自定义审批配置尚未保存",partial.unavailableReason());
        assertFalse(partial.newActionBaselineRequired());assertFalse(partial.newActionBaselinePreparationAllowed());
        assertEquals(revisionId,partial.preparedProcessDefinitionId());assertNotNull(partial.applicationCustomMatrixDigest());
        var context=new DccApplicationCustomMatrixService.SourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,
                1L,5L,2L,6L,99L,21L,1,"REVISION",partial.applicationContextKey(),partial.preparedProcessDefinitionId(),partial.preparedDefinitionDigest());
        var matrices=new DccApplicationCustomMatrixService();var selection=matrices.canonicalize(context,null,null);
        assertEquals(selection.digest(),partial.applicationCustomMatrixDigest());
        var wrongRound=new DccApplicationCustomMatrixService.SourceContext(context.approvalSourceMode(),1L,5L,2L,6L,99L,21L,2,"REVISION",context.applicationContextKey(),context.processDefinitionId(),context.definitionDigest());
        assertThrows(IllegalArgumentException.class,()->matrices.readCanonical(selection.canonical(),selection.digest(),wrongRound));
        var wrongTenant=new DccApplicationCustomMatrixService.SourceContext(context.approvalSourceMode(),2L,5L,2L,6L,99L,21L,1,"REVISION",context.applicationContextKey(),context.processDefinitionId(),context.definitionDigest());
        assertThrows(IllegalArgumentException.class,()->matrices.readCanonical(selection.canonical(),selection.digest(),wrongTenant));
        assertEquals(source,original(20L));assertEquals(target,original(21L));
        assertEquals(attributesBefore,jdbc.queryForList("SELECT * FROM dcc_project_application_attributes"));
    }
    @Test void freshCustomProjectionCanPreviewAndSaveOnlyItsReservedContext() throws Exception {
        freshCustomImport();var source=original(20L);
        var opened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L));
        var resolver=(DccControlledFileApprovalRouteAssigneeResolver)ReflectionTestUtils.getField(workflow,"approvalRouteAssigneeResolver");
        doThrow(new IllegalArgumentException("CUSTOM_MATRIX_SIGNOFF_REQUIRED：请至少选择一个会签部门")).when(resolver).resolveCustomReserved(any(),any(),any());
        var preview=previewRequest(21L,false);preview.setApplicationContextKey(opened.applicationContextKey());preview.setSelectedSignoffDepartmentIds(null);
        preview.setExecutionContract(opened.executionContract());
        var proof=tx().execute(s->workflow.previewRoute(99L,preview));
        var save=saveRequest(21L,proof,false);save.setExecutionContract(opened.executionContract());save.setSelectedSignoffDepartmentIds(null);
        save.setEffectiveMode("DELAYED");save.setEffectiveDate(null);
        save.setApplicationContextKey(opened.applicationContextKey());save.setExpectedApplicationCustomMatrixDigest(proof.getApplicationCustomMatrixDigest());
        save.setApplicationCustomMatrixToken(proof.getApplicationCustomMatrixToken());save.setApplicationCustomMatrixPhase("RESERVED");
        save.setApplicationContextKey(opened.applicationContextKey()+"-other");
        assertThrows(IllegalArgumentException.class,()->tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,save)));
        assertNull(files.selectById(21L).getDraftApprovalSourceMode());
        save.setApplicationContextKey(opened.applicationContextKey());
        tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,save));
        var reopened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L));
        assertEquals("APPLICATION_CUSTOM",files.selectById(21L).getDraftApprovalSourceMode());
        assertEquals(opened.applicationContextKey(),reopened.applicationContextKey());assertEquals(opened.applicationCustomMatrixDigest(),reopened.applicationCustomMatrixDigest());
        assertEquals(source,original(20L));
    }
    @ParameterizedTest @ValueSource(strings={"PUBLIC_MODE","PUBLIC_BASELINE","ORPHAN_CUSTOM"})
    void categoryCannotConvertExistingPublicOrUnboundCustomConfiguration(String saved) throws Exception {
        freshCustomImport();
        switch(saved){
            case "PUBLIC_MODE" -> jdbc.update("UPDATE dcc_controlled_file SET draft_approval_source_mode='PUBLIC_MATRIX' WHERE id=21");
            case "PUBLIC_BASELINE" -> jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=? WHERE id=21",canonicalMatrix(1));
            case "ORPHAN_CUSTOM" -> jdbc.update("UPDATE dcc_controlled_file SET draft_application_context_key='old-application' WHERE id=21");
        }
        var before=original(21L);
        if("ORPHAN_CUSTOM".equals(saved)){
            assertThrows(IllegalArgumentException.class,()->tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L)));
            assertThrows(IllegalArgumentException.class,()->tx().execute(s->workflow.readReplacementApplicationAttributes(99L,21L,20L)));
        }else{
            assertNull(tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L)).approvalSourceMode());
            assertNull(tx().execute(s->workflow.readReplacementApplicationAttributes(99L,21L,20L)).approvalSourceMode());
            assertThrows(IllegalArgumentException.class,()->preview(21L,false));
        }
        assertEquals(before,original(21L));
    }
    static void flag(Object request,Boolean value){
        // Behavioral RED still compiles before the optional field exists; no API substitute.
        if(Arrays.stream(request.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("prepareNewActionBaseline")))
            ReflectionTestUtils.setField(request,"prepareNewActionBaseline",value);
    }
    static Object property(Object record,String name){return ReflectionTestUtils.invokeMethod(record,name);}
    static void defaults(Object target){for(var f:target.getClass().getDeclaredFields())
        if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())&&(f.getAnnotation(Resource.class)!=null||f.getAnnotation(org.springframework.beans.factory.annotation.Autowired.class)!=null))
            ReflectionTestUtils.setField(target,f.getName(),mock(f.getType()));}
    @BeforeEach void configureActualWorkingResources()throws Exception{
        dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Shanghai");
        var category=cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder().id(2L).code("IC21").name("新升版测试")
                .active(true).source("LOCAL").approvalSourceMode("PUBLIC_MATRIX").lifecycleStage("DEVELOPMENT").fileTypeTaxonomyId(6L).build();categories.insert(category);
        jdbc.update("UPDATE dcc_file_category SET tenant_id=1 WHERE id=2");
        matrixSources=new DccApprovalMatrixSourceService();wire(matrixSources,"categories",categories,"routes",rawRoutes,"nodes",rawNodes,"datePolicy",dates);
        var key=new DccApprovalMatrixTokenProperties();key.setSigningSecret("ic21-isolated-technical-baseline-key-at-least-32-bytes");
        tokens=new DccApprovalMatrixTokenService();wire(tokens,"properties",key);
        var savedTypeAvailability=mock(cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService.class);
        when(savedTypeAvailability.resolveActiveCategoryId(6L)).thenReturn(2L);
        when(savedTypeAvailability.resolveActiveCategoryId(8L)).thenReturn(3L);
        baselines=new DccApprovalMatrixBaselineService();wire(baselines,"sources",matrixSources,"tokens",tokens,"files",files,
                "categories",categories,"taxonomies",savedTypeAvailability);
        if(Arrays.stream(baselines.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("executionContracts")))wire(baselines,"executionContracts",contracts);
        workflow=new DccControlledFileWorkflowServiceImpl();defaults(workflow);
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(any(),any())).thenReturn(true);
        wire(workflow,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"projectCodeMapper",projects,
                "projectAccessService",access,"categoryPermissionSupport",permissions,"categoryMapper",categories,"workflowDatePolicy",dates,
                "projectApplicationSnapshots",snapshots,"projectAttributesService",attributes,"applicationRoundService",rounds,
                "workingApplicationDraftInitializer",initializer,"approvalMatrixBaselines",baselines,"executionContracts",contracts,
                "approvalMatrixSources",matrixSources,"assignmentScopeService",scope,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
        var resolver=mock(DccControlledFileApprovalRouteAssigneeResolver.class);
        // Baseline tests have no organizational provider. They keep an explicit not-ready
        // dependency, while IC31's dedicated Workflow test wires the real native source resolver.
        doThrow(new DccControlledFileApprovalRouteAssigneeResolver.NativeSupervisorNotReadyException()).when(resolver)
                .resolvePublicNative(any(),any(),any(),any(),any(),any(),any(),any(),any());
        when(resolver.resolvePublicMatrix(any(),eq(99L),eq("REVISION"),any())).thenAnswer(c->{var source=c.<DccApprovalMatrixSourceService.Source>getArgument(0);
            return new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(source.route(),List.of(
                    new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(1,"MATRIX_REVIEW","会签",1,"DEPT",51L,List.of(51L),"ALL",100,true,List.of(99L))),
                    source.baseline(),cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.VERSION,List.of());});
        var readiness=mock(DccControlledFileRouteReadinessService.class);
        when(readiness.evaluateFrozenRoute(any())).thenAnswer(c->new DccControlledFileRouteReadinessService.RouteReadinessEvaluation(c.getArgument(0),
                DccControlledFileRouteReadinessRespVO.builder().ready(false).nodes(List.of()).blockers(List.of()).build()));
        wire(workflow,"approvalRouteAssigneeResolver",resolver,"routeReadinessService",readiness);
        var departments=mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class);
        when(departments.getDeptList(any())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(id->
                new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(id).setStatus(0).setName("明确部门"+id)).toList());
        wire(workflow,"deptApi",departments);
        currentMatrix(1);
    }
    void currentMatrix(int version)throws Exception{
        jdbc.update("UPDATE dcc_category_approval_route SET active=0 WHERE category_id=2");
        var canonical=canonicalMatrix(version);var baseline=DccApprovalMatrixSourceService.readCanonical(canonical);
        var root=DccCategoryApprovalRouteDO.builder().id(baseline.getRouteId()).categoryId(2L).versionNo(version).actionType("LEGACY")
                .active(true).effectiveTime(java.time.LocalDateTime.of(2026,10,1,12,0)).remark("本次明确原配置"+version).build();rawRoutes.insert(root);
        jdbc.update("UPDATE dcc_category_approval_route SET tenant_id=1 WHERE id=?",root.getId());
        var raw=JsonUtils.parseObject(canonical,Map.class);var nodes=JsonUtils.parseArray(JsonUtils.toJsonString(raw.get("nodes")),DccCategoryApprovalRouteNodeDO.class);
        for(var node:nodes){node.setId(version*10L+node.getId());node.setRouteId(root.getId());rawNodes.insert(node);jdbc.update("UPDATE dcc_category_approval_route_node SET tenant_id=1 WHERE id=?",node.getId());}
    }
    void publicRevisionBody()throws Exception{
        String uploadId=rejectedPublicSource();
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=CURRENT_TIMESTAMP WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=20 WHERE id=10");
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        revisionId=deploy(Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/revision.bpmn20.xml")));
        transitions.setDefinitionTransitions(List.of(new DccExecutionDefinitionTransitions.Transition(uploadId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(uploadId)),revisionId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(revisionId)))));
        add(21L,20L);jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION',revision_base_active_controlled_file_id=20,version_no='A/1-1' WHERE id=21");
        tx().executeWithoutResult(s->{initializer.initialize(99L,20L,21L,null);contracts.inheritPrepared(99L,21L,20L,1);});
    }
    void nextUnpreparedRevisionBody() {
        add(22L,21L);
        jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION',revision_base_active_controlled_file_id=20,version_no='A/1-2' WHERE id=22");
        tx().executeWithoutResult(s->{initializer.initialize(99L,21L,22L,null);contracts.inheritPrepared(99L,22L,21L,1);});
    }
    @Test void editorCanPrepareAnotherRequestersRevisionMatrixWithoutChangingItsApplicant()throws Exception{
        publicRevisionBody();jdbc.update("UPDATE dcc_controlled_file SET requester_id=88,submitter_id=88 WHERE id=21");
        when(access.hasProjectEditorOrOwner(99L,5L)).thenReturn(true);
        var opened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L));
        assertTrue(opened.newActionBaselineRequired());
        var result=preview(21L,true);var request=saveRequest(21L,result,true);
        tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,request));
        assertFalse(tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L)).newActionBaselineRequired());
        assertEquals(88L,files.selectById(21L).getRequesterId());assertNull(files.selectById(21L).getProcessInstanceId());
        when(permissions.hasFilePermission(any(),eq(99L),eq(cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.EDIT))).thenReturn(false);
        assertThrows(RuntimeException.class,()->tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L)));
        assertThrows(RuntimeException.class,()->tx().execute(s->contracts.readConfirmedTypeChangeBaseline(99L,files.selectById(21L))));
    }
    @ParameterizedTest @ValueSource(strings={"UPLOAD","FORMAL_CANDIDATE","BOUND","CONTROLLED"})
    void sharedEditorPreparationNeverTakesOverAnUploadOrFormalApplication(String state)throws Exception{
        publicRevisionBody();jdbc.update("UPDATE dcc_controlled_file SET requester_id=88,submitter_id=88 WHERE id=21");
        switch(state){
            case "UPLOAD" -> jdbc.update("UPDATE dcc_controlled_file SET change_type='NEW' WHERE id=21");
            case "FORMAL_CANDIDATE" -> jdbc.update("UPDATE dcc_controlled_file SET revision_change_type='PARTIAL' WHERE id=21");
            case "BOUND" -> jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='already-bound' WHERE id=21");
            case "CONTROLLED" -> jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP WHERE id=21");
            default -> throw new AssertionError(state);
        }
        when(access.hasProjectEditorOrOwner(99L,5L)).thenReturn(true);
        assertThrows(RuntimeException.class,()->tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L)));
        assertThrows(RuntimeException.class,()->tx().execute(s->contracts.readConfirmedTypeChangeBaseline(99L,files.selectById(21L))));
        assertEquals(88L,files.selectById(21L).getRequesterId());
    }
    @Test void repeatedCheckinCarriesLegitimatePendingNewActionBaselineWithoutInventingMatrix()throws Exception{
        publicRevisionBody();
        jdbc.update("UPDATE dcc_controlled_file SET effective_mode='PRESET_DATE',effective_date='2099-12-20',selected_signoff_department_ids_json='[\"51\"]',distribution_department_ids_json='[]',need_training=0 WHERE id=21");
        var sourceBefore=original(21L);nextUnpreparedRevisionBody();
        var inherited=tx().execute(s->contracts.readInheritedDraftFacts(99L,22L,21L,1));
        assertTrue(inherited.sameApplicationType());assertTrue(inherited.newActionBaselineRequired());assertNull(inherited.approvalMatrixCanonical());
        assertEquals("PRESET_DATE",inherited.effectiveMode());assertEquals(LocalDate.of(2099,12,20),inherited.effectiveDate());
        assertEquals("[\"51\"]",inherited.selectedSignoffDepartmentIdsJson());assertEquals("[]",inherited.distributionDepartmentIdsJson());assertFalse(inherited.needTraining());
        saveFactsToNewTarget(22L,inherited);
        var opened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,22L));
        assertTrue(opened.newActionBaselineRequired());assertTrue(opened.newActionBaselinePreparationAllowed());assertFalse(opened.canSubmit());assertNull(opened.draftApprovalMatrix());
        assertEquals(sourceBefore,original(21L));verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @Test void repeatedMissingBaselineStillRejectsAncestryThatDoesNotReachActualUpload()throws Exception{
        publicRevisionBody();nextUnpreparedRevisionBody();
        jdbc.update("UPDATE dcc_project_application_attributes SET source_application_id=20,source_application_round=1 WHERE application_id=21");
        var sourceBefore=original(21L);var targetBefore=original(22L);
        assertThrows(RuntimeException.class,()->tx().execute(s->contracts.readInheritedDraftFacts(99L,22L,21L,1)));
        assertEquals(sourceBefore,original(21L));assertEquals(targetBefore,original(22L));
    }
    @Test void repeatedCheckinCanSaveNewRevisionBaselineWithSupervisorExpectationAndCompareItAfterReopen()throws Exception{
        publicRevisionBody();nextUnpreparedRevisionBody();
        var uploadBefore=original(20L);var predecessorBefore=original(21L);
        var result=preview(22L,true);var request=saveRequest(22L,result,true);
        request.setExpectedSupervisorSourceDigest("supervisor-digest");request.setSupervisorSourceToken("supervisor-token");
        var resolver=(DccControlledFileApprovalRouteAssigneeResolver)ReflectionTestUtils.getField(workflow,"approvalRouteAssigneeResolver");
        var source=mock(DccControlledFileApprovalRouteAssigneeResolver.NativeApprovalSource.class);
        doReturn(source).when(resolver).resolvePublicNative(any(),any(),any(),any(),any(),any(),any(),any(),any());
        tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,22L,request));
        verify(resolver).requireNativeExpectation(source,"supervisor-digest","supervisor-token");
        var reopened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,22L));
        assertNotNull(reopened.draftApprovalMatrix());assertFalse(reopened.newActionBaselineRequired());
        assertEquals(request.getChangeDescription(),reopened.changeDescription());
        assertFalse(tx().execute(s->baselines.preview(99L,previewRequest(22L,false))).changed());
        currentMatrix(2);
        assertTrue(tx().execute(s->baselines.preview(99L,previewRequest(22L,false))).changed());
        assertEquals(uploadBefore,original(20L));assertEquals(predecessorBefore,original(21L));
    }
    DccControlledFileRoutePreviewReqVO previewRequest(Long id,Boolean explicit){
        var file=files.selectById(id);var req=new DccControlledFileRoutePreviewReqVO();req.setControlledFileId(id);req.setCategoryId(2L);req.setDccProjectCodeId(5L);
        req.setActionType("REVISION");req.setExecutionContract(DccApplicationExecutionContractService.PUBLIC);req.setApplicationContextKey("exact-body-"+id);
        req.setExpectedProcessDefinitionId(file.getPreparedProcessDefinitionId());req.setExpectedDefinitionDigest(file.getPreparedDefinitionDigest());req.setSelectedSignoffDepartmentIds(List.of(51L));flag(req,explicit);return req;
    }
    DccControlledFileRouteReadinessRespVO preview(Long id,Boolean explicit){return tx().execute(s->workflow.previewRoute(99L,previewRequest(id,explicit)));}
    DccWorkingApplicationSaveReqVO saveRequest(Long id,DccControlledFileRouteReadinessRespVO result,Boolean explicit){
        var req=new DccWorkingApplicationSaveReqVO();req.setActual(actual);req.setEffectiveMode(DccApplicationEffectiveSelection.PRESET_DATE);req.setEffectiveDate(LocalDate.of(2099,12,20));
        req.setSelectedSignoffDepartmentIds(List.of(51L));req.setSelectedDistributionDepartmentIds(List.of(52L,53L));req.setNeedTraining(true);req.setChangeDescription("本次新升版真实输入");
        req.setExecutionContract(DccApplicationExecutionContractService.PUBLIC);req.setExpectedProcessDefinitionId(files.selectById(id).getPreparedProcessDefinitionId());req.setExpectedDefinitionDigest(files.selectById(id).getPreparedDefinitionDigest());
        req.setExpectedApprovalRouteId(result.getRouteId());req.setExpectedApprovalRouteVersionNo(result.getRouteVersionNo());req.setExpectedApprovalMatrixDigest(result.getMatrixDigest());
        req.setApprovalMatrixBaselineToken(result.getApprovalMatrixBaselineToken());req.setApprovalMatrixConfirmationToken(result.getApprovalMatrixConfirmationToken());req.setMatrixChangeConfirmed(result.getMatrixChangeRequired());
        req.setApplicationContextKey("exact-body-"+id);flag(req,explicit);return req;
    }
    @Test void workingGetDiscoversRegisteredPreparationAndExplicitPreviewCancelWritesNothing()throws Exception{
        publicRevisionBody();var before=original(21L);var sourceBefore=original(20L);var attrsBefore=jdbc.queryForList("SELECT * FROM dcc_project_application_attributes");
        var opened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L));
        assertEquals(true,property(opened,"newActionBaselineRequired"));assertEquals(true,property(opened,"newActionBaselinePreparationAllowed"));assertNull(opened.draftApprovalMatrix());assertFalse(opened.canSubmit());
        assertEquals(DccApplicationExecutionContractService.PUBLIC,property(opened,"executionContract"));assertEquals(revisionId,property(opened,"preparedProcessDefinitionId"));
        assertEquals(files.selectById(21L).getPreparedDefinitionDigest(),property(opened,"preparedDefinitionDigest"));
        var req=previewRequest(21L,true);req.setExecutionContract((String)property(opened,"executionContract"));req.setExpectedProcessDefinitionId((String)property(opened,"preparedProcessDefinitionId"));req.setExpectedDefinitionDigest((String)property(opened,"preparedDefinitionDigest"));
        var result=tx().execute(s->workflow.previewRoute(99L,req));assertFalse(result.getReady());assertEquals(revisionId,result.getPreparedProcessDefinitionId());assertEquals(31L,result.getRouteId());
        assertNotNull(result.getApprovalMatrixBaselineToken());assertEquals(true,ReflectionTestUtils.getField(result,"newActionBaselinePreparationAllowed"));
        assertEquals(before,original(21L));assertEquals(sourceBefore,original(20L));assertEquals(attrsBefore,jdbc.queryForList("SELECT * FROM dcc_project_application_attributes"));
        var save=saveRequest(21L,result,true);save.setExecutionContract(opened.executionContract());save.setExpectedProcessDefinitionId(opened.preparedProcessDefinitionId());save.setExpectedDefinitionDigest(opened.preparedDefinitionDigest());
        tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,save));var reopened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L));
        assertEquals(revisionId,reopened.preparedProcessDefinitionId());assertNotNull(reopened.draftApprovalMatrix());assertEquals(sourceBefore,original(20L));
        verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @Test void actualFileTokenSavesRealNewBaselineAndFieldsThenReopensWithoutRebasing()throws Exception{
        publicRevisionBody();var sourceBefore=original(20L);var result=preview(21L,true);var req=saveRequest(21L,result,true);
        tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,req));String baseline=files.selectById(21L).getDraftApprovalMatrixJson();
        assertEquals(matrixSources.read(2L).canonical(),baseline);var reopened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L));
        assertEquals(false,property(reopened,"newActionBaselineRequired"));assertEquals(false,property(reopened,"newActionBaselinePreparationAllowed"));
        assertEquals(List.of("51"),reopened.selectedSignoffDepartmentIds());assertEquals(List.of("52","53"),reopened.selectedDistributionDepartmentIds());assertTrue(reopened.needTraining());assertEquals(req.getEffectiveDate(),reopened.effectiveDate());
        currentMatrix(2);assertThrows(RuntimeException.class,()->tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,req)),"true cannot reset a saved before baseline");
        assertEquals(baseline,files.selectById(21L).getDraftApprovalMatrixJson());assertEquals(sourceBefore,original(20L));
        var ordinary=preview(21L,false);assertTrue(ordinary.getMatrixChangeRequired());assertEquals(31L,ordinary.getMatrixBefore().getRouteId());assertEquals(32L,ordinary.getMatrixAfter().getRouteId());
        var ordinarySave=saveRequest(21L,ordinary,false);tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,ordinarySave));assertEquals(baseline,files.selectById(21L).getDraftApprovalMatrixJson());
    }
    @Test void changedMatrixRequiresRealConfirmationAndLateCallerFailureRollsBackBaselineAndFields()throws Exception{
        publicRevisionBody();var first=preview(21L,true);currentMatrix(2);var req=saveRequest(21L,first,true);var before=original(21L);
        var change=assertThrows(DccApprovalMatrixChangeException.class,()->tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,req)));
        assertEquals(before,original(21L));assertNotNull(change.getApprovalMatrixConfirmationToken());
        var fresh=previewRequest(21L,true);fresh.setApprovalMatrixBaselineToken(change.getApprovalMatrixBaselineToken());var confirmation=tx().execute(s->workflow.previewRoute(99L,fresh));assertTrue(confirmation.getMatrixChangeRequired());
        var approved=saveRequest(21L,confirmation,true);approved.setMatrixChangeConfirmed(false);assertThrows(DccApprovalMatrixChangeException.class,()->tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,approved)));
        approved.setMatrixChangeConfirmed(true);
        assertThrows(IllegalStateException.class,()->tx().executeWithoutResult(s->{workflow.saveWorkingApplication(99L,21L,approved);throw new IllegalStateException("late caller rollback");}));assertEquals(before,original(21L));
        tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,approved));assertEquals(31L,DccApprovalMatrixSourceService.readCanonical(files.selectById(21L).getDraftApprovalMatrixJson()).getRouteId());
    }
    @ParameterizedTest @ValueSource(strings={"NULL_FLAG","FALSE_FLAG","WRONG_ACTOR_TOKEN","WRONG_FILE_TOKEN","WRONG_CONTEXT","WRONG_DEFINITION","SOURCE_CHANGED","ATTRIBUTE_SOURCE","SAME_TYPE_MISSING",
            "MISSING_TOKEN","MISSING_EXPECTED_MATRIX","MISSING_EXPECTED_DIGEST","WRONG_TENANT","BAD_REGISTRY","LEGACY_CLAIM","CREATION_TOKEN"})
    void ordinaryOrWrongScopeCannotInitializeMissingBaseline(String violation)throws Exception{
        publicRevisionBody();var result=preview(21L,true);var req=saveRequest(21L,result,true);
        switch(violation){
            case "NULL_FLAG"->flag(req,null);case "FALSE_FLAG"->flag(req,false);
            case "WRONG_ACTOR_TOKEN"->req.setApprovalMatrixBaselineToken(tokens.issueBaseline(new DccApprovalMatrixTokenService.Context(1L,88L,5L,2L,6L,"REVISION",21L,"exact-body-21"),matrixSources.read(2L).canonical()));
            case "WRONG_FILE_TOKEN"->req.setApprovalMatrixBaselineToken(tokens.issueBaseline(new DccApprovalMatrixTokenService.Context(1L,99L,5L,2L,6L,"REVISION",22L,"exact-body-21"),matrixSources.read(2L).canonical()));
            case "WRONG_CONTEXT"->req.setApplicationContextKey("wrong-context");case "WRONG_DEFINITION"->req.setExpectedProcessDefinitionId("missing");
            case "SOURCE_CHANGED"->jdbc.update("UPDATE dcc_controlled_file SET source_sha256=? WHERE id=20","b".repeat(64));
            case "ATTRIBUTE_SOURCE"->jdbc.update("UPDATE dcc_project_application_attributes SET source_application_id=20,source_application_round=1 WHERE application_id=21");
            case "SAME_TYPE_MISSING"->jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION' WHERE id=20");
            case "MISSING_TOKEN"->req.setApprovalMatrixBaselineToken(null);case "MISSING_EXPECTED_MATRIX"->req.setExpectedApprovalRouteId(null);
            case "MISSING_EXPECTED_DIGEST"->req.setExpectedDefinitionDigest(null);case "WRONG_TENANT"->jdbc.update("UPDATE dcc_controlled_file SET tenant_id=2 WHERE id=20");
            case "BAD_REGISTRY"->transitions.setDefinitionTransitions(List.of());case "LEGACY_CLAIM"->req.setExecutionContract(DccApplicationExecutionContractService.LEGACY);
            case "CREATION_TOKEN"->req.setApprovalMatrixBaselineToken(tokens.issueBaseline(new DccApprovalMatrixTokenService.Context(1L,99L,5L,2L,6L,"REVISION",null,"exact-body-21"),matrixSources.read(2L).canonical()));
        }
        var before=original(21L);var sourceBefore=original(20L);var attrsBefore=jdbc.queryForList("SELECT * FROM dcc_project_application_attributes");
        assertThrows(RuntimeException.class,()->tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,req)));
        assertEquals(before,original(21L));assertEquals(sourceBefore,original(20L));assertEquals(attrsBefore,jdbc.queryForList("SELECT * FROM dcc_project_application_attributes"));
    }
    @Test void legacyMissingFactsNeverGainPreparationFlagsOrInferredPublicIdentity(){
        var opened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,20L));assertFalse(opened.newActionBaselineRequired());assertFalse(opened.newActionBaselinePreparationAllowed());
        assertNull(opened.executionContract());assertNull(opened.preparedProcessDefinitionId());assertNull(opened.preparedDefinitionDigest());
    }
    @Test void completeSavedChoicesStillCannotSubmitWithoutRawAndCheckedOutPreparationIsNotAllowed()throws Exception{
        publicRevisionBody();jdbc.update("UPDATE dcc_controlled_file SET effective_mode='PRESET_DATE',effective_date='2099-12-20',selected_signoff_department_ids_json='[\"51\"]',distribution_department_ids_json='[]',need_training=0 WHERE id=21");
        var opened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L));assertTrue(opened.newActionBaselineRequired());assertTrue(opened.newActionBaselinePreparationAllowed());assertFalse(opened.canSubmit());
        jdbc.update("UPDATE dcc_controlled_file SET checked_out_by=99 WHERE id=21");opened=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,21L));assertTrue(opened.newActionBaselineRequired());assertFalse(opened.newActionBaselinePreparationAllowed());assertFalse(opened.canSubmit());
        assertThrows(RuntimeException.class,()->preview(21L,true));assertNull(files.selectById(21L).getDraftApprovalMatrixJson());
    }
    @Test void lateActualAttributeFailureRollsBackTheAlreadyVerifiedBaselineCas()throws Exception{
        publicRevisionBody();var result=preview(21L,true);var req=saveRequest(21L,result,true);req.setActual(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(List.of(),null,"Y","N","N",null));
        var before=original(21L);var attrsBefore=jdbc.queryForList("SELECT * FROM dcc_project_application_attributes");
        assertThrows(RuntimeException.class,()->tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,21L,req)));
        assertEquals(before,original(21L));assertEquals(attrsBefore,jdbc.queryForList("SELECT * FROM dcc_project_application_attributes"));
    }
    @Test void actualCrossActionMustRejectWrongPreviewActionProjectAndCategoryBeforeReadingMatrix()throws Exception{
        publicRevisionBody();var before=original(21L);
        for(String wrong:List.of("ACTION","PROJECT","CATEGORY")){
            var req=previewRequest(21L,true);if(wrong.equals("ACTION"))req.setActionType("NEW");else if(wrong.equals("PROJECT"))req.setDccProjectCodeId(6L);else req.setCategoryId(3L);
            assertThrows(RuntimeException.class,()->tx().execute(s->workflow.previewRoute(99L,req)));assertEquals(before,original(21L));
        }
    }
}
