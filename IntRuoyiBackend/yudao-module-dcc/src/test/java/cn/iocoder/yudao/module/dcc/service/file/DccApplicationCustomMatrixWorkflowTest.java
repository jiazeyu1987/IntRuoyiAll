package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Current Workflow + Resolver + Token + Readiness + real H2 facts and same-transaction Flowable.
 * Account/organization/authorization directories and external storage are explicit isolated ports. */
class DccApplicationCustomMatrixWorkflowTest extends DccPreparedExecutionInheritanceDatabaseTest {
    @Resource DccFileCategoryMapper categories;
    DccControlledFileWorkflowServiceImpl workflow;
    DccApprovalMatrixTokenService tokens;
    cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    DccDirectSupervisorSource organization;
    String definitionId,digest;
    final DccApplicationCustomMatrix custom=new DccApplicationCustomMatrix(77L,66L);
    final String scope="g03-exact-working-20";
    private static Path repositoryRoot(){Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();if(root==null)throw new IllegalStateException("Repository test resources are unavailable");return root;}
    DccControlledFileSubmitIterationReqVO savedRequest;
    DccControlledFileAttachmentServiceImpl attachments;
    long caseProcessCount(){return engine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(definitionId).count();}
    @AfterEach void cleanOnlyThisCaseDeploymentProcesses(){
        if(engine!=null&&definitionId!=null)for(var process:engine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(definitionId).list())
            engine.getRuntimeService().deleteProcessInstance(process.getId(),"G03 exact owned deployment fixture cleanup after assertions");
    }
    List<Map<String,Object>> stableRows(String table){var result=jdbc.queryForList("SELECT * FROM "+table);result.forEach(row->row.replaceAll((k,v)->v instanceof byte[] bytes?HexFormat.of().formatHex(bytes):v));return result;}
    static void defaults(Object target){for(var f:target.getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())&&f.getAnnotation(Resource.class)!=null)ReflectionTestUtils.setField(target,f.getName(),mock(f.getType()));}
    @BeforeEach void customSetup()throws Exception{
        // Explicit attachment fixture shared by Maven/private runs, aligned with 20260920 attachment
        // and 20261011 training-record provenance migrations. The runner never patches table schemas.
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_controlled_file_attachment(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,controlled_file_id BIGINT NOT NULL,storage_file_id BIGINT NOT NULL,original_file_name VARCHAR(512) NOT NULL,content_type VARCHAR(255),file_size BIGINT,file_sha256 VARCHAR(64),sort_no INT NOT NULL,attachment_purpose VARCHAR(32),application_process_instance_id VARCHAR(128),uploaded_by BIGINT,uploaded_username_snapshot VARCHAR(128),uploaded_nickname_snapshot VARCHAR(128),uploaded_at TIMESTAMP,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,creator VARCHAR(64),updater VARCHAR(64),deleted BIT DEFAULT 0)");
        categories.insert(DccFileCategoryDO.builder().id(2L).code("G03").name("任意类型").active(true).source("LOCAL").approvalSourceMode("APPLICATION_CUSTOM").lifecycleStage("OUTPUT").fileTypeTaxonomyId(6L).build());
        jdbc.update("UPDATE dcc_file_category SET tenant_id=1 WHERE id=2");
        cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.setLoginUser(new cn.iocoder.yudao.framework.security.core.LoginUser().setId(99L).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
        var properties=new DccApprovalMatrixTokenProperties();properties.setSigningSecret("g03-workflow-actual-explicit-secret-at-least-32-bytes");tokens=new DccApprovalMatrixTokenService();wire(tokens,"properties",properties);
        users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        when(users.getUser(anyLong())).thenAnswer(c->new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(c.getArgument(0)).setTenantId(1L).setStatus(0).setDeptId(51L).setPostIds(Set.of(10L)).setUsername("g03-actor").setNickname("明确人员"));
        when(users.getUserList(any())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(users::getUser).toList());
        var departments=mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class);
        when(departments.getDeptList(any())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(id->new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(id).setName("正式部门"+id).setStatus(0).setLeaderUserId(99L)).toList());
        organization=mock(DccDirectSupervisorSource.class);when(organization.require(any())).thenAnswer(c->{var ctx=c.<DccDirectSupervisorSource.Context>getArgument(0);return new DccDirectSupervisorSource.Evidence(ctx.tenantId(),ctx.requesterId(),88L,"FORMAL_ORGANIZATION","relation-99","v1");});
        var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(organization);
        var resolver=new DccControlledFileApprovalRouteAssigneeResolver();wire(resolver,"nativeSupervisors",provider,"nativeSourceTokens",tokens,"adminUserApi",users,"deptApi",departments,"customRounds",rounds);
        var ready=new DccControlledFileRouteReadinessService();var permission=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(permission.hasAnyPermissions(anyLong(),any(String[].class))).thenReturn(true);
        var auth=mock(DccElectronicSignatureAuthorizationService.class);when(auth.getAuthorizationMap(any())).thenAnswer(c->{var result=new HashMap<Long,Boolean>();c.<Collection<Long>>getArgument(0).forEach(id->result.put(id,true));return result;});
        var images=mock(DccElectronicSignatureImageService.class);when(images.requireActiveSnapshot(anyLong())).thenReturn(DccElectronicSignatureImageSnapshot.builder().imageId(10L).fileId(10L).verifiedStatus("VALID").build());
        wire(ready,"adminUserApi",users,"permissionApi",permission,"signatureAuthorizationService",auth,"signatureImageService",images);
        workflow=new DccControlledFileWorkflowServiceImpl();defaults(workflow);
        var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Shanghai");
        var source=new DccApprovalMatrixSourceService();wire(source,"categories",categories,"routes",sqlMapper(cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper.class),"nodes",sqlMapper(cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper.class),"datePolicy",dates);
        var baselines=new DccApprovalMatrixBaselineService();wire(baselines,"sources",source,"tokens",tokens,"files",files,"executionContracts",contracts);
        var scopePort=mock(DccControlledFileAssignmentScopeService.class);when(scopePort.isWithinAssignedFileScope(any(),any())).thenReturn(true);
        wire(workflow,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"projectCodeMapper",sqlMapper(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class),
                "categoryMapper",categories,"projectAccessService",access,"categoryPermissionSupport",permissions,"workflowDatePolicy",dates,"projectApplicationSnapshots",snapshots,
                "projectAttributesService",attributes,"applicationRoundService",rounds,"workingApplicationDraftInitializer",initializer,"executionContracts",contracts,
                "assignmentScopeService",scopePort,"approvalRouteAssigneeResolver",resolver,"routeReadinessService",ready,"adminUserApi",users,"deptApi",departments,"approvalMatrixTokens",tokens,
                "routeSnapshotMapper",routeSnapshots,"taskAssigneeSnapshotMapper",sqlMapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper.class),
                "checkoutMapper",sqlMapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper.class),"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
        wire(workflow,"approvalMatrixSources",source,"approvalMatrixBaselines",baselines);
        attachments=new DccControlledFileAttachmentServiceImpl();wire(attachments,"attachmentMapper",sqlMapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper.class),"controlledFileMapper",files,"users",users,"permissionSupport",permissions,"projectAccessService",access,"assignmentScopeService",scopePort);
        wire(workflow,"attachmentService",attachments);
        engine.close();var candidates=mock(cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker.class);
        when(candidates.calculateUserListByTask(any())).thenAnswer(c->{var e=c.<org.flowable.engine.delegate.DelegateExecution>getArgument(0);return ((Map<String,List<Long>>)e.getVariable("PROCESS_START_USER_SELECT_ASSIGNEES")).get(e.getCurrentActivityId());});
        when(candidates.calculateUsersByTask(any())).thenAnswer(c->new LinkedHashSet<>(candidates.calculateUserListByTask(c.getArgument(0))));
        var behavior=new cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory();behavior.setTaskCandidateInvoker(candidates);
        var config=new org.flowable.spring.SpringProcessEngineConfiguration();config.setDataSource(jdbc.getDataSource());config.setTransactionManager(transactions);config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);config.setActivityBehaviorFactory(behavior);
        jdbc.execute("SET MODE LEGACY");try{engine=config.buildProcessEngine();}finally{jdbc.execute("SET MODE MYSQL");}
        definitionId=deploy(Files.readString(repositoryRoot().resolve("IntRuoyiBackend/config/dcc-workflow-v6/upload.bpmn20.xml")));
        digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(definitionId));
        when(definitions.getActiveProcessDefinition(anyString())).thenAnswer(c->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult());
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=?,effective_mode='DELAYED',need_training=0,selected_signoff_department_ids_json='[\"51\",\"52\"]',distribution_department_ids_json='[]' WHERE id=20",DccApplicationExecutionContractService.PUBLIC_V2,definitionId,digest);
        var bpm=mock(cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi.class);
        when(bpm.createProcessInstance(any(),any())).thenAnswer(c->{var request=c.<cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO>getArgument(1);return engine.getRuntimeService().startProcessInstanceById(request.getProcessDefinitionId(),request.getBusinessKey(),request.getVariables()).getId();});
        wire(workflow,"bpmProcessInstanceApi",bpm);
        var reader=new cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl();wire(reader,"runtimeService",engine.getRuntimeService(),"historyService",engine.getHistoryService());
        var bound=new DccBoundWorkflowContractResolver();wire(bound,"definitions",definitions,"processes",reader,"rounds",rounds,"attributes",attributeRows,"routes",routeSnapshots,"nativeSourceTokens",tokens,"customRouteResolver",resolver);
        wire(contracts,"boundContracts",bound);
    }
    <T>T sqlMapper(Class<T> type){return cn.hutool.extra.spring.SpringUtil.getBean(org.mybatis.spring.SqlSessionTemplate.class).getMapper(type);}
    DccControlledFileRoutePreviewReqVO previewRequest(){var r=new DccControlledFileRoutePreviewReqVO();r.setCategoryId(2L);r.setDccProjectCodeId(5L);r.setControlledFileId(20L);r.setActionType("NEW");r.setApplicationContextKey(scope);r.setExecutionContract(DccApplicationExecutionContractService.PUBLIC_V2);r.setExpectedProcessDefinitionId(definitionId);r.setExpectedDefinitionDigest(digest);r.setSelectedSignoffDepartmentIds(List.of(51L,52L));r.setApplicationCustomMatrix(custom);return r;}
    DccControlledFileSubmitIterationReqVO prepareSavedCustomRequest()throws Exception{
        var preview=tx().execute(s->workflow.previewRoute(99L,previewRequest()));
        assertEquals("APPLICATION_CUSTOM",preview.getApprovalSourceMode());assertTrue(preview.getReady());assertNull(preview.getRouteId());assertNull(preview.getApprovalMatrix());
        var save=new DccWorkingApplicationSaveReqVO();save.setActual(actual);save.setEffectiveMode("DELAYED");save.setNeedTraining(false);save.setSelectedSignoffDepartmentIds(List.of(51L,52L));save.setSelectedDistributionDepartmentIds(List.of());save.setApplicationCustomMatrix(custom);save.setApplicationContextKey(scope);
        save.setExecutionContract(DccApplicationExecutionContractService.PUBLIC_V2);save.setExpectedProcessDefinitionId(definitionId);save.setExpectedDefinitionDigest(digest);save.setExpectedApplicationCustomMatrixDigest(preview.getApplicationCustomMatrixDigest());save.setApplicationCustomMatrixToken(preview.getApplicationCustomMatrixToken());save.setApplicationCustomMatrixPhase(preview.getApplicationCustomMatrixPhase());
        tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,20L,save));
        var restored=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,20L));assertEquals(custom,restored.applicationCustomMatrix());assertEquals("APPLICATION_CUSTOM",restored.approvalSourceMode());assertNull(restored.draftApprovalMatrix());
        preview=tx().execute(s->workflow.previewRoute(99L,previewRequest()));
        var request=new DccControlledFileSubmitIterationReqVO();request.setIdempotencyKey("custom-submit-20");request.setEffectiveMode("DELAYED");request.setEffectiveDate(null);request.setNeedTraining(false);request.setRevisionChangeType("INITIAL");request.setChangeDescription("真实其他类型申请");request.setProjectAttributes(actual);request.setSelectedSignoffDepartmentIds(List.of(51L,52L));request.setSelectedDistributionDepartmentIds(List.of());request.setApplicationCustomMatrix(custom);request.setApplicationContextKey(scope);request.setExecutionContract(DccApplicationExecutionContractService.PUBLIC_V2);request.setExpectedProcessDefinitionId(definitionId);request.setExpectedDefinitionDigest(digest);request.setExpectedApplicationCustomMatrixDigest(preview.getApplicationCustomMatrixDigest());request.setApplicationCustomMatrixToken(preview.getApplicationCustomMatrixToken());request.setApplicationCustomMatrixPhase(preview.getApplicationCustomMatrixPhase());request.setExpectedSupervisorSourceDigest(preview.getSupervisorSourceDigest());request.setSupervisorSourceToken(preview.getSupervisorSourceToken());
        savedRequest=request;
        var ordinary=tx().execute(s->attachments.readOrdinaryAttachmentManifest(99L,20L));request.setExpectedOrdinaryAttachmentIds(ordinary.attachmentIds());request.setExpectedOrdinaryAttachmentManifestHash(ordinary.payloadHash());
        return request;
    }
    @Test void actualCustomPreviewSaveRestoreSubmitStartsSupervisorAndFreezesOwnSourceWithoutPublicMatrix()throws Exception{
        var publicBefore=jdbc.queryForList("SELECT * FROM dcc_category_approval_route");var nodesBefore=jdbc.queryForList("SELECT * FROM dcc_category_approval_route_node");
        var request=prepareSavedCustomRequest();
        assertEquals(Long.valueOf(20L),tx().execute(s->workflow.submitWorkingIteration(99L,20L,request)));
        var file=files.selectById(20L);assertEquals("PENDING_SUPERVISOR_REVIEW",file.getStatus());assertNotNull(file.getProcessInstanceId());assertEquals("SUPERVISOR_REVIEW",engine.getTaskService().createTaskQuery().processInstanceId(file.getProcessInstanceId()).singleResult().getTaskDefinitionKey());
        var snapshots=routeSnapshots.selectListByControlledFileId(20L);assertEquals(4,snapshots.size());snapshots.forEach(row->{assertNull(row.getRouteVersionNo());assertNull(row.getSourceApprovalMatrixId());assertNull(row.getSourceApprovalMatrixJson());assertNotNull(row.getRuntimeApprovalNodesJson());});
        assertEquals(publicBefore,jdbc.queryForList("SELECT * FROM dcc_category_approval_route"));assertEquals(nodesBefore,jdbc.queryForList("SELECT * FROM dcc_category_approval_route_node"));
    }
    @Test void exactCommittedReplayReturnsOneInstanceButChangedCustomPayloadRejects()throws Exception{
        actualCustomPreviewSaveRestoreSubmitStartsSupervisorAndFreezesOwnSourceWithoutPublicMatrix();
        var before=jdbc.queryForList("SELECT * FROM dcc_controlled_file_route_snapshot");String processId=files.selectById(20L).getProcessInstanceId();
        var taskIds=engine.getTaskService().createTaskQuery().processInstanceId(processId).list().stream().map(org.flowable.task.api.Task::getId).toList();var filesBefore=stableRows("dcc_controlled_file");var attrsBefore=stableRows("dcc_project_application_attributes");
        assertEquals(Long.valueOf(20L),tx().execute(s->workflow.submitWorkingIteration(99L,20L,savedRequest)));
        assertEquals(1,caseProcessCount());assertEquals(processId,files.selectById(20L).getProcessInstanceId());assertEquals(taskIds,engine.getTaskService().createTaskQuery().processInstanceId(processId).list().stream().map(org.flowable.task.api.Task::getId).toList());assertEquals(before,jdbc.queryForList("SELECT * FROM dcc_controlled_file_route_snapshot"));assertEquals(filesBefore,stableRows("dcc_controlled_file"));assertEquals(attrsBefore,stableRows("dcc_project_application_attributes"));
        savedRequest.setApplicationCustomMatrix(new DccApplicationCustomMatrix(78L,66L));
        assertThrows(RuntimeException.class,()->tx().execute(s->workflow.submitWorkingIteration(99L,20L,savedRequest)));
        assertEquals(before,jdbc.queryForList("SELECT * FROM dcc_controlled_file_route_snapshot"));assertEquals(1,caseProcessCount());assertEquals(filesBefore,stableRows("dcc_controlled_file"));assertEquals(attrsBefore,stableRows("dcc_project_application_attributes"));assertEquals(taskIds,engine.getTaskService().createTaskQuery().processInstanceId(processId).list().stream().map(org.flowable.task.api.Task::getId).toList());
    }
    @Test void lateFailureRollsBackSavedCustomAttributesSnapshotsAndActualFlowableThenCanRetry()throws Exception{
        var before=stableRows("dcc_controlled_file");var attrs=stableRows("dcc_project_application_attributes");
        var otherProcesses=engine.getRuntimeService().createProcessInstanceQuery().list().stream().map(org.flowable.engine.runtime.ProcessInstance::getId).sorted().toList();
        assertThrows(IllegalStateException.class,()->tx().executeWithoutResult(s->{try{actualCustomPreviewSaveRestoreSubmitStartsSupervisorAndFreezesOwnSourceWithoutPublicMatrix();}catch(Exception e){throw new IllegalStateException(e);}throw new IllegalStateException("late real BPM failure");}));
        assertEquals(before,stableRows("dcc_controlled_file"));assertEquals(attrs,stableRows("dcc_project_application_attributes"));
        assertEquals(0,caseProcessCount());assertEquals(otherProcesses,engine.getRuntimeService().createProcessInstanceQuery().list().stream().map(org.flowable.engine.runtime.ProcessInstance::getId).sorted().toList());assertEquals(0L,routeSnapshots.selectCount(null));
        actualCustomPreviewSaveRestoreSubmitStartsSupervisorAndFreezesOwnSourceWithoutPublicMatrix();
    }
    @Test void sourceModeChangeNeverReplacesSavedCustomWithPublicAndUnavailableAccountCannotAdvance()throws Exception{
        var preview=tx().execute(s->workflow.previewRoute(99L,previewRequest()));
        var context=new DccApplicationCustomMatrixService.SourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,1L,5L,2L,6L,99L,20L,1,"UPLOAD",scope,definitionId,digest);
        jdbc.update("UPDATE dcc_controlled_file SET draft_approval_source_mode='APPLICATION_CUSTOM',draft_application_context_key=?,draft_application_custom_matrix_json=? WHERE id=20",scope,new DccApplicationCustomMatrixService().canonicalize(context,custom,List.of(51L,52L)).canonical());
        jdbc.update("UPDATE dcc_file_category SET approval_source_mode='PUBLIC_MATRIX' WHERE id=2");
        var failure=assertThrows(IllegalArgumentException.class,()->tx().execute(s->workflow.previewRoute(99L,previewRequest())));assertTrue(failure.getMessage().contains("审批来源"));
        assertEquals(custom,tx().execute(s->workflow.readWorkingApplicationAttributes(99L,20L)).applicationCustomMatrix());
        jdbc.update("UPDATE dcc_file_category SET approval_source_mode='APPLICATION_CUSTOM' WHERE id=2");
        when(users.getUser(77L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(77L).setTenantId(1L).setStatus(1).setPostIds(Set.of(10L)));
        var notReady=tx().execute(s->workflow.previewRoute(99L,previewRequest()));assertFalse(notReady.getReady());assertTrue(notReady.getBlockers().stream().anyMatch(b->Objects.equals(77L,b.getUserId())));
        assertEquals(0,caseProcessCount());assertEquals(0L,routeSnapshots.selectCount(null));
    }
    @Test void actualFrozenCustomRouteAndBoundBpmRestoreWithoutPublicIdentityAndRejectCorruption()throws Exception{
        actualCustomPreviewSaveRestoreSubmitStartsSupervisorAndFreezesOwnSourceWithoutPublicMatrix();
        var file=files.selectById(20L);var resolver=(DccControlledFileApprovalRouteAssigneeResolver)ReflectionTestUtils.getField(workflow,"approvalRouteAssigneeResolver");
        var restored=resolver.restoreFrozenRoute(file,routeSnapshots.selectListByControlledFileId(20L));assertNull(restored.route());assertEquals(custom,new DccApplicationCustomMatrixService().readCanonical(restored.applicationCustomSource().selection().canonical(),restored.applicationCustomSource().selection().digest(),restored.applicationCustomSource().reservedContext()).applicationCustomMatrix());
        assertEquals(definitionId,contracts.resolveBoundExecution(file).processDefinitionId());
        jdbc.update("UPDATE dcc_controlled_file_route_snapshot SET route_version_no=1 WHERE controlled_file_id=20");
        assertThrows(IllegalArgumentException.class,()->resolver.restoreFrozenRoute(file,routeSnapshots.selectListByControlledFileId(20L)));
        assertThrows(RuntimeException.class,()->contracts.resolveBoundExecution(file));
    }
    @Test void currentUnavailableAccountChangedProofOrManifestCannotCreateBpmOrMutateSavedFacts()throws Exception{
        var request=prepareSavedCustomRequest();var before=stableRows("dcc_controlled_file");var attrs=stableRows("dcc_project_application_attributes");
        when(users.getUser(77L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(77L).setTenantId(1L).setStatus(1).setPostIds(Set.of(10L)));
        assertThrows(RuntimeException.class,()->tx().execute(s->workflow.submitWorkingIteration(99L,20L,request)));
        assertEquals(before,stableRows("dcc_controlled_file"));assertEquals(attrs,stableRows("dcc_project_application_attributes"));assertEquals(0,caseProcessCount());assertEquals(0L,routeSnapshots.selectCount(null));
        when(users.getUser(77L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(77L).setTenantId(1L).setStatus(0).setPostIds(Set.of(10L)));
        request.setApplicationCustomMatrix(new DccApplicationCustomMatrix(78L,66L));assertThrows(IllegalArgumentException.class,()->tx().execute(s->workflow.submitWorkingIteration(99L,20L,request)));
        request.setApplicationCustomMatrix(custom);request.setExpectedOrdinaryAttachmentManifestHash("0".repeat(64));assertThrows(IllegalArgumentException.class,()->tx().execute(s->workflow.submitWorkingIteration(99L,20L,request)));
        assertEquals(before,stableRows("dcc_controlled_file"));assertEquals(attrs,stableRows("dcc_project_application_attributes"));assertEquals(0,caseProcessCount());assertEquals(0L,routeSnapshots.selectCount(null));
    }
    @Test void actualUnallocatedPreviewCreatesOwnReservedDraftAndRestoresExactConfiguration()throws Exception{newApplication(false);}
    @Test void actualDirectNewCustomSubmissionBindsRealReservedRoundAndStartsSameSupervisorModel()throws Exception{newApplication(true);}
    @Test void actualPublicV2PreviewUsesPreparedV2ThenSavedAndSubmittedSourceKeepsRealPublicVersion()throws Exception{
        jdbc.update("UPDATE dcc_file_category SET approval_source_mode='PUBLIC_MATRIX' WHERE id=2");
        var routes=sqlMapper(cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper.class);var nodes=sqlMapper(cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper.class);
        routes.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder().id(31L).categoryId(2L).versionNo(1).actionType("LEGACY").active(true).build());jdbc.update("UPDATE dcc_category_approval_route SET tenant_id=1 WHERE id=31");
        var raw=List.of(
            cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.builder().id(1L).stageNo(1).stageCode("MATRIX_REVIEW").stageName("会签").stageOrder(1).sort(1).stageType("SIGNOFF").marker("●").subjectType("DEPT").subjectId(51L).subjectName("质量").candidateSourceType("DEPT").candidateSourceId(51L).candidateSourceIds("51,52").approveMethod("ALL").approveRatio(100).requireAllApprovals(true).required(true).build(),
            cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.builder().id(2L).stageNo(3).stageCode("MATRIX_APPROVAL").stageName("批准").stageOrder(3).sort(2).stageType("APPROVAL").marker("●").subjectType("USER").subjectId(77L).subjectName("批准甲").candidateSourceType("USER").candidateSourceId(77L).candidateSourceIds("77").approveMethod("ANY").requireAllApprovals(false).required(true).build(),
            cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.builder().id(3L).stageNo(4).stageCode("DOC_CONTROL_REVIEW").stageName("文控").stageOrder(4).sort(3).stageType("DOC_CONTROL").marker("●").subjectType("USER").subjectId(66L).subjectName("文控乙").candidateSourceType("USER").candidateSourceId(66L).candidateSourceIds("66").approveMethod("ANY").requireAllApprovals(false).required(true).build());
        for(var node:raw){node.setRouteId(31L);nodes.insert(node);jdbc.update("UPDATE dcc_category_approval_route_node SET tenant_id=1 WHERE id=?",node.getId());}
        var source=(DccApprovalMatrixSourceService)ReflectionTestUtils.getField(workflow,"approvalMatrixSources");jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=? WHERE id=20",source.read(2L).canonical());
        var p=previewRequest();p.setApplicationCustomMatrix(null);var preview=tx().execute(s->workflow.previewRoute(99L,p));assertEquals(DccApplicationExecutionContractService.PUBLIC_V2,preview.getExecutionContract());assertEquals(definitionId,preview.getPreparedProcessDefinitionId());assertEquals(digest,preview.getPreparedDefinitionDigest());assertEquals(31L,preview.getRouteId());
        var save=new DccWorkingApplicationSaveReqVO();save.setActual(actual);save.setEffectiveMode("DELAYED");save.setNeedTraining(false);save.setSelectedSignoffDepartmentIds(List.of(51L,52L));save.setSelectedDistributionDepartmentIds(List.of());save.setApplicationContextKey(scope);save.setExecutionContract(DccApplicationExecutionContractService.PUBLIC_V2);save.setExpectedProcessDefinitionId(definitionId);save.setExpectedDefinitionDigest(digest);save.setApprovalMatrixBaselineToken(preview.getApprovalMatrixBaselineToken());tx().executeWithoutResult(s->workflow.saveWorkingApplication(99L,20L,save));
        var returned=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,20L));assertEquals(definitionId,returned.preparedProcessDefinitionId());assertNotNull(returned.draftApprovalMatrix());
        var r=new DccControlledFileSubmitIterationReqVO();r.setIdempotencyKey("public-v2-submit");r.setEffectiveMode("DELAYED");r.setNeedTraining(false);r.setRevisionChangeType("INITIAL");r.setChangeDescription("真实公共V2申请");r.setProjectAttributes(actual);r.setSelectedSignoffDepartmentIds(List.of(51L,52L));r.setSelectedDistributionDepartmentIds(List.of());r.setApplicationContextKey(scope);r.setExecutionContract(preview.getExecutionContract());r.setExpectedProcessDefinitionId(definitionId);r.setExpectedDefinitionDigest(digest);r.setExpectedApprovalRouteId(preview.getRouteId());r.setExpectedApprovalRouteVersionNo(preview.getRouteVersionNo());r.setExpectedApprovalMatrixDigest(preview.getMatrixDigest());r.setApprovalMatrixBaselineToken(preview.getApprovalMatrixBaselineToken());r.setMatrixChangeConfirmed(false);r.setExpectedSupervisorSourceDigest(preview.getSupervisorSourceDigest());r.setSupervisorSourceToken(preview.getSupervisorSourceToken());var manifest=tx().execute(s->attachments.readOrdinaryAttachmentManifest(99L,20L));r.setExpectedOrdinaryAttachmentIds(manifest.attachmentIds());r.setExpectedOrdinaryAttachmentManifestHash(manifest.payloadHash());
        assertEquals(Long.valueOf(20),tx().execute(s->workflow.submitWorkingIteration(99L,20L,r)));routeSnapshots.selectListByControlledFileId(20L).forEach(row->{assertEquals(1,row.getRouteVersionNo());assertEquals(31L,row.getSourceApprovalMatrixId());});
    }
    void newApplication(boolean direct)throws Exception{
        var p=previewRequest();p.setControlledFileId(null);var preview=tx().execute(s->workflow.previewRoute(99L,p));
        assertEquals("PREVIEW",preview.getApplicationCustomMatrixPhase());assertFalse(preview.getApplicationCustomMatrixCanonical().contains("controlledFileId"));
        var tax=mock(cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService.class);var path=new cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath(6L,"技术文档","输出","任意类型",null,null);
        when(tax.resolveActivePath(6L)).thenReturn(path);when(tax.listActiveDescendantIds(6L)).thenReturn(List.of(6L));when(tax.listActiveDescendantPaths(6L)).thenReturn(List.of(path));when(tax.resolveActiveCategoryId(6L)).thenReturn(2L);
        var physical=mock(cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper.class);when(physical.selectById(101L)).thenReturn(cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO.builder().id(101L).name("Custom-new.pdf").type("application/pdf").size(8L).build());
        var bodies=mock(DccControlledFileSourceOwnershipService.class);when(bodies.prepareSubmissionSource(101L,false)).thenReturn(new DccControlledFilePreparedSource(101L,101L,"a".repeat(64),false));
        var tickets=mock(cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService.class);when(tickets.resolveForBinding(any())).thenReturn(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile("g03-source-ticket",101L,"Custom-new.pdf","application/pdf",8L));
        var directories=mock(cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper.class);var directory=cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO.builder().id(3L).active(true).name("SOP").build();when(directories.selectById(3L)).thenReturn(directory);when(directories.selectEnabledList()).thenReturn(List.of(directory));
        wire(workflow,"fileTypeTaxonomyAdminService",tax,"fileMapper",physical,"sourceOwnershipService",bodies,"uploadTicketService",tickets,"directoryMapper",directories,"revisionService",new DccControlledFileRevisionServiceImpl(),"submitMutex",new DccControlledFileSubmitMutex(),"nameClaimService",mock(DccControlledFileNameClaimService.class));
        var r=new DccControlledFileSubmitReqVO();r.setIdempotencyKey("g03-new-draft");r.setCategoryId(2L);r.setDccProjectCodeId(5L);r.setProjectFolderId(500L);r.setFileTypeTaxonomyId(6L);r.setFileName("Custom-new");r.setFileNumber("G03-NEW");r.setVersionNo("A/1");r.setChangeType("NEW");r.setProcessType("CONTROLLED_FILE");r.setNeedTraining(false);r.setEffectiveMode("DELAYED");r.setSelectedSignoffDepartmentIds(List.of(51L,52L));r.setSelectedDistributionDepartmentIds(List.of());r.setProjectAttributes(actual);r.setOriginalUploadTicket("g03-source-ticket");r.setSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.newUploadPrefix(5L,6L,"Custom-new"),"g03-new"));r.setApplicationCustomMatrix(custom);r.setApplicationContextKey(scope);r.setApplicationCustomMatrixPhase("PREVIEW");r.setExpectedApplicationCustomMatrixDigest(preview.getApplicationCustomMatrixDigest());r.setApplicationCustomMatrixToken(preview.getApplicationCustomMatrixToken());r.setExpectedSupervisorSourceDigest(preview.getSupervisorSourceDigest());r.setSupervisorSourceToken(preview.getSupervisorSourceToken());r.setExecutionContract(preview.getExecutionContract());r.setExpectedProcessDefinitionId(definitionId);r.setExpectedDefinitionDigest(digest);
        Long id=tx().execute(s->direct?workflow.submitNewWithDerivedStorage(99L,r,new DccDerivedUploadStorage(1L,99L,5L,500L,2L,1L,3L,3L,false)):workflow.createWorkingWithDerivedStorage(99L,r,new DccDerivedUploadStorage(1L,99L,5L,500L,2L,1L,3L,3L,false)));
        assertNotEquals(20L,id);var file=files.selectById(id);assertEquals("APPLICATION_CUSTOM",file.getDraftApprovalSourceMode());assertNull(file.getDraftApprovalMatrixJson());assertTrue(file.getDraftApplicationCustomMatrixJson().contains("\"controlledFileId\":\""+id+"\""));
        if(direct){
            assertEquals("PENDING_SUPERVISOR_REVIEW",file.getStatus());assertNotNull(file.getProcessInstanceId());assertEquals(4,routeSnapshots.selectListByControlledFileId(id).size());assertEquals("SUPERVISOR_REVIEW",engine.getTaskService().createTaskQuery().processInstanceId(file.getProcessInstanceId()).singleResult().getTaskDefinitionKey());
            var before=stableRows("dcc_controlled_file");var frozen=stableRows("dcc_controlled_file_route_snapshot");String process=file.getProcessInstanceId();
            assertEquals(id,tx().execute(s->workflow.submitNewWithDerivedStorage(99L,r,new DccDerivedUploadStorage(1L,99L,5L,500L,2L,1L,3L,3L,false))));
            assertEquals(process,files.selectById(id).getProcessInstanceId());assertEquals(before,stableRows("dcc_controlled_file"));assertEquals(frozen,stableRows("dcc_controlled_file_route_snapshot"));
            r.setApplicationCustomMatrix(new DccApplicationCustomMatrix(78L,66L));assertThrows(RuntimeException.class,()->tx().execute(s->workflow.submitNewWithDerivedStorage(99L,r,new DccDerivedUploadStorage(1L,99L,5L,500L,2L,1L,3L,3L,false))));assertEquals(before,stableRows("dcc_controlled_file"));assertEquals(frozen,stableRows("dcc_controlled_file_route_snapshot"));
        }
        else{var restored=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,id));assertEquals(custom,restored.applicationCustomMatrix());assertEquals(scope,restored.applicationContextKey());assertEquals(1,restored.attributeRound());assertEquals(0L,routeSnapshots.selectCount(null));}
    }
}
