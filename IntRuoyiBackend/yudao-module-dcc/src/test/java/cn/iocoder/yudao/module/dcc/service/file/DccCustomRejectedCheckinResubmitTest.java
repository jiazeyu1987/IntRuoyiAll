package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real check-in/body ownership, typed inheritance, candidate allocation, HMAC and Flowable.
 * Rejection's already-terminal persisted fact is explicit setup, not a claim to test the signature UI. */
class DccCustomRejectedCheckinResubmitTest extends DccWorkflowSelectedIterationDatabaseTest {
    String exact,digest;final String contextKey="custom-rejected-source";
    final DccApplicationCustomMatrix config=new DccApplicationCustomMatrix(77L,66L);
    DccApprovalMatrixTokenService tokenService;
    cn.iocoder.yudao.module.system.api.user.AdminUserApi directory;
    DccControlledFileApprovalRouteAssigneeResolver customResolver;
    DccControlledFileAttachmentServiceImpl actualAttachments;
    final Set<Long> ownedFiles=new HashSet<>(Set.of(20L));
    final Set<String> ownedProcesses=new HashSet<>();
    boolean customFixture;
    static Path root(){Path p=Path.of("").toAbsolutePath();while(p!=null&&!Files.isRegularFile(p.resolve("AGENTS.md")))p=p.getParent();if(p==null)throw new IllegalStateException("Repository fixture missing");return p;}
    Object target(Object bean)throws Exception{return bean instanceof org.springframework.aop.framework.Advised proxy?proxy.getTargetSource().getTarget():bean;}
    <T>T mapper(Class<T> type){return cn.hutool.extra.spring.SpringUtil.getBean(org.mybatis.spring.SqlSessionTemplate.class).getMapper(type);}
    @Override @AfterEach void cleanup(){
        if(!customFixture){super.cleanup();return;}
        if(engine!=null)for(String process:ownedProcesses)if(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(process).singleResult()!=null)engine.getRuntimeService().deleteProcessInstance(process,"G03 exact owned rework fixture cleanup after assertions");
        for(Long file:ownedFiles)jdbc.update("DELETE FROM dcc_application_round_link WHERE tenant_id=1 AND application_type='UPLOAD' AND application_id=?",file);
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    @BeforeEach void customSources(TestInfo testInfo)throws Exception{
        // Inherited tests keep their original legacy fixture; only this class's scenarios use V6.
        customFixture=testInfo.getTestMethod().orElseThrow().getDeclaringClass()==DccCustomRejectedCheckinResubmitTest.class;
        if(!customFixture)return;
        initial();var actualWorkflow=target(workflow);
        cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.setLoginUser(new cn.iocoder.yudao.framework.security.core.LoginUser().setId(99L).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
        var candidates=mock(cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker.class);when(candidates.calculateUserListByTask(any())).thenAnswer(c->{var e=c.<org.flowable.engine.delegate.DelegateExecution>getArgument(0);var selected=(Map<String,List<Long>>)e.getVariable("PROCESS_START_USER_SELECT_ASSIGNEES");var assigned=selected.get(e.getCurrentActivityId());if(assigned==null)assigned=((Map<String,List<Long>>)e.getVariable("PROCESS_APPROVE_USER_SELECT_ASSIGNEES")).get(e.getCurrentActivityId());assertNotNull(assigned);return assigned;});when(candidates.calculateUsersByTask(any())).thenAnswer(c->new LinkedHashSet<>(candidates.calculateUserListByTask(c.getArgument(0))));
        ((cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory)((org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl)engine.getProcessEngineConfiguration()).getActivityBehaviorFactory()).setTaskCandidateInvoker(candidates);
        var deployed=engine.getRepositoryService().createDeployment().tenantId("1").addString("custom-upload.bpmn20.xml",Files.readString(root().resolve("IntRuoyiBackend/config/dcc-workflow-v6/upload.bpmn20.xml"))).deploy();
        exact=engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployed.getId()).singleResult().getId();digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(exact));
        var properties=new DccApprovalMatrixTokenProperties();properties.setSigningSecret("custom-rejected-formal-technical-key-at-least-32-bytes");tokenService=new DccApprovalMatrixTokenService();wire(tokenService,"properties",properties);
        var categories=mapper(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class);categories.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder().id(2L).code("G03").name("正式自定义").active(true).sort(1).source("LOCAL").lifecycleStage("OUTPUT").approvalSourceMode("APPLICATION_CUSTOM").fileTypeTaxonomyId(6L).build());jdbc.update("UPDATE dcc_file_category SET tenant_id=1 WHERE id=2");
        directory=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);when(directory.getUser(anyLong())).thenAnswer(c->new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(c.getArgument(0)).setTenantId(1L).setStatus(0).setDeptId(51L).setUsername("g03").setNickname("明确账号").setPostIds(Set.of(10L)));when(directory.getUserList(any())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(directory::getUser).toList());
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_controlled_file_attachment(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,controlled_file_id BIGINT NOT NULL,storage_file_id BIGINT NOT NULL,original_file_name VARCHAR(512) NOT NULL,content_type VARCHAR(255),file_size BIGINT,file_sha256 VARCHAR(64),sort_no INT NOT NULL,attachment_purpose VARCHAR(32),application_process_instance_id VARCHAR(128),uploaded_by BIGINT,uploaded_username_snapshot VARCHAR(128),uploaded_nickname_snapshot VARCHAR(128),uploaded_at TIMESTAMP,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,creator VARCHAR(64),updater VARCHAR(64),deleted BIT DEFAULT 0)");
        actualAttachments=new DccControlledFileAttachmentServiceImpl();wire(actualAttachments,"attachmentMapper",mapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper.class),"controlledFileMapper",files,"users",directory,"permissionSupport",permissions,"projectAccessService",access,"assignmentScopeService",scope,"sourceOwnershipService",bodies,"versionPolicy",policy);
        wire(actualWorkflow,"attachmentService",actualAttachments);wire(target(query),"attachmentService",actualAttachments);wire(revisions,"attachmentService",actualAttachments);
        wire(actualAttachments,"publicUploadPlacementService",mock(DccPublicUploadPlacementService.class),"checkoutMapper",checkouts);
        var dept=mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class);when(dept.getDeptList(any())).thenReturn(List.of(new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(51L).setStatus(0).setName("质量").setLeaderUserId(99L)));
        var org=mock(DccDirectSupervisorSource.class);when(org.require(any())).thenAnswer(c->{var v=c.<DccDirectSupervisorSource.Context>getArgument(0);return new DccDirectSupervisorSource.Evidence(v.tenantId(),v.requesterId(),88L,"FORMAL_ORG","relation99","v1");});var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(org);
        customResolver=new DccControlledFileApprovalRouteAssigneeResolver();wire(customResolver,"adminUserApi",directory,"deptApi",dept,"nativeSupervisors",provider,"nativeSourceTokens",tokenService,"customRounds",rounds);
        var ready=new DccControlledFileRouteReadinessService();var permission=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(permission.hasAnyPermissions(anyLong(),any(String[].class))).thenReturn(true);var auth=mock(DccElectronicSignatureAuthorizationService.class);when(auth.getAuthorizationMap(any())).thenAnswer(c->{var m=new HashMap<Long,Boolean>();c.<Collection<Long>>getArgument(0).forEach(id->m.put(id,true));return m;});var image=mock(DccElectronicSignatureImageService.class);when(image.requireActiveSnapshot(anyLong())).thenReturn(DccElectronicSignatureImageSnapshot.builder().imageId(10L).fileId(10L).verifiedStatus("VALID").build());wire(ready,"adminUserApi",directory,"permissionApi",permission,"signatureAuthorizationService",auth,"signatureImageService",image);
        var reader=new cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl();wire(reader,"runtimeService",engine.getRuntimeService(),"historyService",engine.getHistoryService());
        var definitions=(cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService)ReflectionTestUtils.getField(executionContracts,"definitions");var bound=new DccBoundWorkflowContractResolver();wire(bound,"definitions",definitions,"processes",reader,"rounds",rounds,"attributes",attributeRows,"routes",routes,"nativeSourceTokens",tokenService,"customRouteResolver",customResolver);wire(executionContracts,"boundContracts",bound,"routeResolver",customResolver,"routeSnapshots",routes);
        wire(actualWorkflow,"categoryMapper",categories,"adminUserApi",directory,"deptApi",dept,"approvalMatrixTokens",tokenService,"approvalRouteAssigneeResolver",customResolver,"routeReadinessService",ready);
        var ctx=new DccApplicationCustomMatrixService.SourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,1L,5L,2L,6L,99L,20L,rounds.requireDraft("UPLOAD",20L),"UPLOAD",contextKey,exact,digest);
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=?,draft_approval_source_mode='APPLICATION_CUSTOM',draft_application_context_key=?,draft_application_custom_matrix_json=?,effective_mode='DELAYED',selected_signoff_department_ids_json='[\"51\"]',distribution_department_ids_json='[]',need_training=0 WHERE id=20",DccApplicationExecutionContractService.PUBLIC_V2,exact,digest,contextKey,new DccApplicationCustomMatrixService().canonicalize(ctx,config,List.of(51L)).canonical());
        wire(revisions,"deptApi",dept,"bpmProcessInstanceService",reader,"workingApplicationDraftInitializer",initializer);
    }
    DccControlledFileSubmitIterationReqVO request(Long file,String key)throws Exception{
        var saved=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,file));
        var p=new DccControlledFileRoutePreviewReqVO();p.setControlledFileId(file);p.setCategoryId(2L);p.setDccProjectCodeId(5L);p.setActionType("NEW");p.setApplicationContextKey(saved.applicationContextKey());p.setApplicationCustomMatrix(config);p.setSelectedSignoffDepartmentIds(List.of(51L));p.setExecutionContract(DccApplicationExecutionContractService.PUBLIC_V2);p.setExpectedProcessDefinitionId(exact);p.setExpectedDefinitionDigest(digest);
        var response=tx().execute(s->workflow.previewRoute(99L,p));assertTrue(response.getReady());var r=super.request("INITIAL");r.setIdempotencyKey(key);r.setApplicationCustomMatrix(config);r.setApplicationContextKey(saved.applicationContextKey());r.setApplicationCustomMatrixPhase("RESERVED");r.setExpectedApplicationCustomMatrixDigest(response.getApplicationCustomMatrixDigest());r.setApplicationCustomMatrixToken(response.getApplicationCustomMatrixToken());r.setExpectedSupervisorSourceDigest(response.getSupervisorSourceDigest());r.setSupervisorSourceToken(response.getSupervisorSourceToken());r.setExecutionContract(DccApplicationExecutionContractService.PUBLIC_V2);r.setExpectedProcessDefinitionId(exact);r.setExpectedDefinitionDigest(digest);r.setEffectiveMode("DELAYED");r.setEffectiveDate(null);r.setSelectedSignoffDepartmentIds(List.of(51L));var manifest=tx().execute(s->actualAttachments.readOrdinaryAttachmentManifest(99L,file));r.setExpectedOrdinaryAttachmentIds(manifest.attachmentIds());r.setExpectedOrdinaryAttachmentManifestHash(manifest.payloadHash());return r;
    }
    @Test void submittedCustomInitialThenCheckedInWorkingSubmitsWithItsActuallySavedContextKey() throws Exception {
        Long source = submittedControlledCustomSource();
        var sourceBefore = customSourceHistory(source);
        String sourceKey = files.selectById(source).getDraftApplicationContextKey();
        Long body = checkinDistinctBody(source, "context-key-next-working", new byte[]{31, 32, 33, 34});
        ownedFiles.add(body);
        var saved = tx().execute(status -> workflow.readWorkingApplicationAttributes(99L, body));
        assertEquals(config, saved.applicationCustomMatrix());
        assertEquals(List.of("51"), saved.selectedSignoffDepartmentIds());
        assertNull(files.selectById(body).getSubmitIdempotencyKey());
        assertEquals(files.selectById(body).getDraftApplicationContextKey(), saved.applicationContextKey());
        var request = requestUsingSavedCustomContext(body, "PARTIAL");
        // Reproduce the exact frontend contract: saved context is the submit key, never a fresh UUID.
        assertEquals(saved.applicationContextKey(), request.getIdempotencyKey());

        Long candidate = assertDoesNotThrow(() -> tx().execute(status ->
                workflow.submitWorkingIteration(99L, body, request)),
                "a new working application must not collide with its source's already submitted key");

        ownedFiles.add(candidate);
        ownedProcesses.add(files.selectById(candidate).getProcessInstanceId());
        assertNotEquals(sourceKey, saved.applicationContextKey());
        assertEquals("PENDING_SUPERVISOR_REVIEW", files.selectById(candidate).getStatus());
        assertEquals(saved.preparedProcessDefinitionId(), files.selectById(candidate).getPreparedProcessDefinitionId());
        assertEquals(saved.applicationContextKey(), files.selectById(candidate).getSubmitIdempotencyKey());
        var frozen = customSourceHistory(candidate);
        assertEquals(candidate, tx().execute(status -> workflow.submitWorkingIteration(99L, body, request)));
        assertEquals(frozen, customSourceHistory(candidate));
        assertEquals(sourceBefore, customSourceHistory(source));
    }

    @Test void inheritedCustomTargetsHaveDistinctStableKeysAndPreserveSourceMatrixAndHistory() throws Exception {
        Long source = submittedControlledCustomSource();
        var sourceBefore = customSourceHistory(source);
        Long first = checkinDistinctBody(source, "first-custom-target", new byte[]{41, 42, 43, 44});
        ownedFiles.add(first);
        var firstRead = tx().execute(status -> workflow.readWorkingApplicationAttributes(99L, first));
        var repeated = tx().execute(status -> workflow.readWorkingApplicationAttributes(99L, first));
        assertEquals(firstRead.applicationContextKey(), repeated.applicationContextKey());
        assertEquals(firstRead.applicationCustomMatrixDigest(), repeated.applicationCustomMatrixDigest());
        var firstFile = files.selectById(first);
        var inheritedAgain = tx().execute(status -> executionContracts.readInheritedCustomDraft(99L, first, source,
                rounds.require("UPLOAD", source, files.selectById(source).getProcessInstanceId())));
        assertEquals(firstRead.applicationContextKey(), inheritedAgain.draftApplicationContextKey());
        assertEquals(firstFile.getDraftApplicationCustomMatrixJson(), inheritedAgain.draftApplicationCustomMatrixJson());
        Long second = checkinDistinctBody(first, "second-custom-target", new byte[]{51, 52, 53, 54});
        ownedFiles.add(second);
        var secondRead = tx().execute(status -> workflow.readWorkingApplicationAttributes(99L, second));
        assertAll("each real target/round keeps its own stable context and inherited business selections",
                () -> assertNotEquals(files.selectById(source).getDraftApplicationContextKey(), firstRead.applicationContextKey()),
                () -> assertNotEquals(firstRead.applicationContextKey(), secondRead.applicationContextKey()),
                () -> assertTrue(firstRead.applicationContextKey().length() <= 128),
                () -> assertTrue(secondRead.applicationContextKey().length() <= 128),
                () -> assertEquals(config, firstRead.applicationCustomMatrix()),
                () -> assertEquals(config, secondRead.applicationCustomMatrix()),
                () -> assertEquals(firstRead.selectedSignoffDepartmentIds(), secondRead.selectedSignoffDepartmentIds()),
                () -> assertEquals(firstRead.actual(), secondRead.actual()),
                () -> assertEquals(firstRead.preparedProcessDefinitionId(), secondRead.preparedProcessDefinitionId()),
                () -> assertEquals(firstRead.preparedDefinitionDigest(), secondRead.preparedDefinitionDigest()),
                () -> assertEquals(sourceBefore, customSourceHistory(source)));
    }

    @Test void replacementHttpReadPreservesSavedCustomProjectionWithoutWrites() throws Exception {
        Long source = submittedControlledCustomSource();
        Long body = checkinDistinctBody(source, "replacement-custom-read", new byte[]{61, 62, 63, 64});
        ownedFiles.add(body);
        var sourceBefore = customSourceHistory(source);
        var bodyBefore = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(files.selectById(body));
        int round = rounds.requireDraft("REVISION", body);
        var attributesBefore = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(attributeRows.find("REVISION", body, round));
        int snapshotCountBefore = count("dcc_project_application_attributes");
        var saved = tx().execute(status -> workflow.readWorkingApplicationAttributes(99L, body));
        assertEquals("APPLICATION_CUSTOM", saved.approvalSourceMode());
        assertEquals(config, saved.applicationCustomMatrix());
        assertNotNull(saved.applicationContextKey());
        assertEquals(files.selectById(body).getDraftApplicationContextKey(), saved.applicationContextKey());
        assertEquals(round, saved.attributeRound());
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(files.selectById(body).getDraftApplicationCustomMatrixJson()),
                saved.applicationCustomMatrixDigest());
        assertNotNull(saved.executionContract());
        assertNotNull(saved.preparedProcessDefinitionId());
        assertNotNull(saved.preparedDefinitionDigest());
        var controller = new cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController();
        wire(controller, "workflowService", workflow);
        var http = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        try (var login = mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class)) {
            login.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            for (String endpoint : List.of("working-attributes", "replacement-attributes")) {
                var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/dcc/controlled-files/" + body + "/" + endpoint);
                if (endpoint.equals("replacement-attributes")) request.param("controlledBaselineId", source.toString());
                var response = http.perform(request).andReturn().getResponse();
                assertEquals(200, response.getStatus());
                var envelope = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                        response.getContentAsString(), com.fasterxml.jackson.databind.JsonNode.class);
                assertEquals(0, envelope.path("code").asInt(-1), endpoint + ": " + envelope);
                var actual = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                        envelope.path("data").toString(), DccWorkingApplicationAttributes.class);
                assertNotNull(actual, endpoint + " must return saved attributes");
                assertAll(endpoint + " must preserve the exact saved domain fields",
                        () -> assertEquals(saved.approvalSourceMode(), actual.approvalSourceMode(), "approvalSourceMode"),
                        () -> assertEquals(saved.applicationCustomMatrix(), actual.applicationCustomMatrix(), "applicationCustomMatrix"),
                        () -> assertEquals(saved.applicationContextKey(), actual.applicationContextKey(), "applicationContextKey"),
                        () -> assertEquals(saved.attributeRound(), actual.attributeRound(), "attributeRound"),
                        () -> assertEquals(saved.applicationCustomMatrixDigest(), actual.applicationCustomMatrixDigest(), "applicationCustomMatrixDigest"),
                        () -> assertEquals(saved.executionContract(), actual.executionContract(), "executionContract"),
                        () -> assertEquals(saved.preparedProcessDefinitionId(), actual.preparedProcessDefinitionId(), "preparedProcessDefinitionId"),
                        () -> assertEquals(saved.preparedDefinitionDigest(), actual.preparedDefinitionDigest(), "preparedDefinitionDigest"),
                        () -> assertEquals(saved.controlledFileId(), actual.controlledFileId(), "controlledFileId"),
                        () -> assertEquals(saved.projectId(), actual.projectId(), "projectId"),
                        () -> assertEquals(saved.applicationType(), actual.applicationType(), "applicationType"),
                        () -> assertEquals(saved.defaultSource(), actual.defaultSource(), "defaultSource"),
                        () -> assertEquals(saved.actual(), actual.actual(), "actual"),
                        () -> assertEquals(saved.selectedSignoffDepartmentIds(), actual.selectedSignoffDepartmentIds(), "selectedSignoffDepartmentIds"),
                        () -> assertEquals(saved.selectedDistributionDepartmentIds(), actual.selectedDistributionDepartmentIds(), "selectedDistributionDepartmentIds"),
                        () -> assertEquals(saved.effectiveMode(), actual.effectiveMode(), "effectiveMode"),
                        () -> assertEquals(saved.effectiveDate(), actual.effectiveDate(), "effectiveDate"),
                        () -> assertEquals(saved.needTraining(), actual.needTraining(), "needTraining"),
                        () -> assertEquals(saved.changeDescription(), actual.changeDescription(), "changeDescription"));
            }
        } finally {
            assertAll("attribute GETs leave the exact source and working draft unchanged",
                    () -> assertEquals(sourceBefore, customSourceHistory(source)),
                    () -> assertEquals(bodyBefore, cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(files.selectById(body))),
                    () -> assertEquals(attributesBefore, cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(attributeRows.find("REVISION", body, round))),
                    () -> assertEquals(snapshotCountBefore, count("dcc_project_application_attributes")));
        }
    }

    private Long submittedControlledCustomSource() throws Exception {
        Long source = tx().execute(status -> {
            try { return workflow.submitWorkingIteration(99L, 20L, request(20L, contextKey)); }
            catch (Exception error) { throw new IllegalStateException(error); }
        });
        ownedFiles.add(source);
        var original = files.selectById(source);
        ownedProcesses.add(original.getProcessInstanceId());
        assertEquals(contextKey, original.getSubmitIdempotencyKey());
        // Existing fixture convention: real submitted BPM/history retained; control completion itself
        // is not the subject of this check-in/custom-context regression.
        engine.getRuntimeService().deleteProcessInstance(original.getProcessInstanceId(), "private controlled-source history fixture");
        var controlledAt = java.time.LocalDateTime.of(2026, 10, 17, 12, 0, 0, 123456000);
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=?,activated_time=?,revision_change_type='INITIAL' WHERE id=?",
                controlledAt, controlledAt, source);
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=?,current_active_controlled_file_id=? WHERE id=10",
                source, source);
        jdbc.update("INSERT INTO dcc_current_file_relation_set(tenant_id,source_master_id,controlled_file_id,row_version) VALUES(1,10,?,0)", source);
        var deployment = engine.getRepositoryService().createDeployment().tenantId("1")
                .addString("context-revision.bpmn20.xml", Files.readString(root().resolve("IntRuoyiBackend/config/dcc-workflow-v6/revision.bpmn20.xml"))).deploy();
        var revisionDefinition = engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        var transitions = (DccExecutionDefinitionTransitions) ReflectionTestUtils.getField(executionContracts, "transitions");
        var mappings = new ArrayList<>(transitions.getDefinitionTransitions());
        mappings.add(new DccExecutionDefinitionTransitions.Transition(original.getPreparedProcessDefinitionId(), original.getPreparedDefinitionDigest(),
                revisionDefinition.getId(), DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(revisionDefinition.getId()))));
        transitions.setDefinitionTransitions(mappings);
        wire(revisions, "bpmProcessInstanceService", ReflectionTestUtils.getField(executionContracts, "processes"));
        return source;
    }

    private DccControlledFileSubmitIterationReqVO requestUsingSavedCustomContext(Long body, String intent) throws Exception {
        var saved = tx().execute(status -> workflow.readWorkingApplicationAttributes(99L, body));
        var p = new DccControlledFileRoutePreviewReqVO();
        p.setControlledFileId(body); p.setCategoryId(2L); p.setDccProjectCodeId(5L); p.setActionType("REVISION");
        p.setApplicationContextKey(saved.applicationContextKey()); p.setApplicationCustomMatrix(saved.applicationCustomMatrix());
        p.setSelectedSignoffDepartmentIds(List.of(51L)); p.setExecutionContract(saved.executionContract());
        p.setExpectedProcessDefinitionId(saved.preparedProcessDefinitionId()); p.setExpectedDefinitionDigest(saved.preparedDefinitionDigest());
        var preview = tx().execute(status -> workflow.previewRoute(99L, p));
        assertTrue(preview.getReady());
        var r = super.request(intent);
        r.setIdempotencyKey(saved.applicationContextKey()); r.setApplicationContextKey(saved.applicationContextKey());
        r.setApplicationCustomMatrix(saved.applicationCustomMatrix()); r.setApplicationCustomMatrixPhase("RESERVED");
        r.setExpectedApplicationCustomMatrixDigest(preview.getApplicationCustomMatrixDigest()); r.setApplicationCustomMatrixToken(preview.getApplicationCustomMatrixToken());
        r.setExpectedSupervisorSourceDigest(preview.getSupervisorSourceDigest()); r.setSupervisorSourceToken(preview.getSupervisorSourceToken());
        r.setExecutionContract(saved.executionContract()); r.setExpectedProcessDefinitionId(saved.preparedProcessDefinitionId()); r.setExpectedDefinitionDigest(saved.preparedDefinitionDigest());
        r.setEffectiveMode("DELAYED"); r.setEffectiveDate(null); r.setNeedTraining(false); r.setProjectAttributes(saved.actual());
        r.setSelectedSignoffDepartmentIds(List.of(51L)); r.setSelectedDistributionDepartmentIds(List.of());
        var manifest = tx().execute(status -> actualAttachments.readOrdinaryAttachmentManifest(99L, body));
        r.setExpectedOrdinaryAttachmentIds(manifest.attachmentIds()); r.setExpectedOrdinaryAttachmentManifestHash(manifest.payloadHash());
        return r;
    }

    private Map<String, Object> customSourceHistory(Long source) {
        var row = files.selectById(source);
        var result = new TreeMap<String, Object>();
        result.put("canonical", row.getDraftApplicationCustomMatrixJson());
        result.put("key", row.getDraftApplicationContextKey());
        result.put("submitKey", row.getSubmitIdempotencyKey());
        result.put("payloadHash", row.getSubmitPayloadHash());
        result.put("definition", row.getPreparedProcessDefinitionId());
        result.put("digest", row.getPreparedDefinitionDigest());
        result.put("attributes", jdbc.queryForList("SELECT * FROM dcc_project_application_attributes WHERE application_id=? ORDER BY application_round", source));
        result.put("routes", jdbc.queryForList("SELECT * FROM dcc_controlled_file_route_snapshot WHERE controlled_file_id=? ORDER BY id", source));
        result.put("signatures", jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature WHERE controlled_file_id=? ORDER BY id", source));
        var history = engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(row.getProcessInstanceId()).includeProcessVariables().singleResult();
        assertNotNull(history);
        result.put("historyProcessId", history.getId());
        result.put("historyDefinition", history.getProcessDefinitionId());
        result.put("historyVariables", history.getProcessVariables());
        return result;
    }

    @Test void rejectedFrozenCustomSurvivesActualCheckinAndCurrentPersonnelIsRecheckedOnNewCandidate()throws Exception{
        Long source=tx().execute(s->{try{return workflow.submitWorkingIteration(99L,20L,request(20L,"source-submit"));}catch(Exception e){throw new IllegalStateException(e);}});var original=files.selectById(source);ownedProcesses.add(original.getProcessInstanceId());String sourceCanonical=original.getDraftApplicationCustomMatrixJson();
        engine.getRuntimeService().deleteProcessInstance(original.getProcessInstanceId(),"explicit terminal rejected fixture");jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED' WHERE id=?",source);
        Long body=checkinDistinctBody(source,"custom-rejected-checkin",new byte[]{9,8,7,6});ownedFiles.add(body);var checked=files.selectById(body);assertEquals("APPLICATION_CUSTOM",checked.getDraftApprovalSourceMode());assertNotEquals(sourceCanonical,checked.getDraftApplicationCustomMatrixJson());
        var saved=tx().execute(s->workflow.readWorkingApplicationAttributes(99L,body));assertEquals(config,saved.applicationCustomMatrix());assertEquals(checked.getDraftApplicationContextKey(),saved.applicationContextKey());assertNotEquals(original.getDraftApplicationContextKey(),saved.applicationContextKey());
        var next=request(body,"resubmit-custom");when(directory.getUser(77L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(77L).setTenantId(1L).setStatus(1).setPostIds(Set.of(10L)));
        int before=files.selectList().size();assertThrows(RuntimeException.class,()->tx().execute(s->workflow.submitWorkingIteration(99L,body,next)));assertEquals(before,files.selectList().size());assertEquals(sourceCanonical,files.selectById(source).getDraftApplicationCustomMatrixJson());
        when(directory.getUser(77L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(77L).setTenantId(1L).setStatus(0).setPostIds(Set.of(10L)));
        Long candidate=tx().execute(s->workflow.submitWorkingIteration(99L,body,next));ownedFiles.add(candidate);ownedProcesses.add(files.selectById(candidate).getProcessInstanceId());assertNotEquals(body,candidate);assertEquals("PENDING_SUPERVISOR_REVIEW",files.selectById(candidate).getStatus());assertEquals(config,new DccApplicationCustomMatrixService().readCanonical(files.selectById(candidate).getDraftApplicationCustomMatrixJson(),cn.hutool.crypto.digest.DigestUtil.sha256Hex(files.selectById(candidate).getDraftApplicationCustomMatrixJson()),customResolver.restoreFrozenRoute(files.selectById(candidate),routes.selectListByControlledFileId(candidate)).applicationCustomSource().reservedContext()).applicationCustomMatrix());
    }
}
