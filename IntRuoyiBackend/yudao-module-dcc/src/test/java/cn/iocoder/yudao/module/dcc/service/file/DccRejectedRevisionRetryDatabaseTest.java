package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Query checkin, C allocation, A submission, B attributes, H2 locks and real Flowable history. */
class DccRejectedRevisionRetryDatabaseTest extends DccWorkflowSelectedIterationDatabaseTest {
    @org.junit.jupiter.api.BeforeEach void realProcessHistoryPort() {
        var bpm=org.mockito.Mockito.mock(cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService.class);
        org.mockito.Mockito.when(bpm.getProcessInstance(org.mockito.ArgumentMatchers.anyString())).thenAnswer(c->engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(c.getArgument(0)).singleResult());
        org.mockito.Mockito.when(bpm.getHistoricProcessInstance(org.mockito.ArgumentMatchers.anyString())).thenAnswer(c->engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(c.getArgument(0)).singleResult());
        org.mockito.Mockito.doAnswer(c->{var req=c.<cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO>getArgument(1);engine.getRuntimeService().deleteProcessInstance(req.getId(),req.getReason());return null;}).when(bpm).cancelProcessInstanceByStartUser(org.mockito.ArgumentMatchers.eq(99L),org.mockito.ArgumentMatchers.any());
        wire(revisions,"bpmProcessInstanceService",bpm);
        wire(workflow,"bpmProcessInstanceService",bpm);
        var tasks=org.mockito.Mockito.mock(cn.iocoder.yudao.module.bpm.service.task.BpmTaskService.class);
        org.mockito.Mockito.when(tasks.getRunningTaskListByProcessInstanceId(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.isNull(),org.mockito.ArgumentMatchers.isNull())).thenAnswer(c->engine.getTaskService().createTaskQuery().processInstanceId(c.getArgument(0)).list());
        wire(revisions,"bpmTaskService",tasks);
    }
    void rejectFixture(long fileId) {
        var file=files.selectById(fileId);
        // An already completed signature fact is a fixture here; this test verifies retention, not signature generation.
        jdbc.update("INSERT INTO dcc_controlled_file_signature(controlled_file_id,task_id,actor_id,action_type,signature_mode,comment,process_instance_id,version_no,source_file_id,tenant_id) VALUES(?,'failed-task',99,'REJECT','FIXTURE','本轮签名意见',?,?,?,1)",fileId,file.getProcessInstanceId(),file.getVersionNo(),file.getSourceFileId());
        tx().executeWithoutResult(s->{engine.getRuntimeService().deleteProcessInstance(file.getProcessInstanceId(),"正式失败轮次测试");
            jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED',reject_reason='本轮失败意见',rejected_time=CURRENT_TIMESTAMP WHERE id=?",fileId);});
    }
    @Test void twoRejectedAttemptsRetainA2AndIndependentBodiesBpmAndFrozenAttributes() throws Exception {
        controlled("A/1");long selected=checkinDistinctBody(20L,"attempt-one",new byte[]{2,2,2});
        long first=workflow.submitWorkingIteration(99L,selected,request("PARTIAL"));rejectFixture(first);
        var signature=jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE controlled_file_id=?",first);
        var firstRow=files.selectById(first);var firstAttributes=jdbc.queryForMap("SELECT default_source_json,actual_attributes_json,application_round FROM dcc_project_application_attributes WHERE application_id=? AND submitted=1",first);
        changeDefault();long revised=checkinDistinctBody(first,"attempt-two",new byte[]{3,3,3});
        long extra=checkin(revised,"second correction before resubmit");
        org.mockito.Mockito.when(access.hasProjectEditorOrOwner(99L,5L)).thenReturn(true);
        org.mockito.Mockito.when(access.hasProjectOwner(99L,5L)).thenReturn(true);
        var options=query.getRevisionOptions(99L,20L);assertNull(options.partialTarget().unavailableReason());
        var option=options.iterations().stream().filter(r->r.id().equals(extra)).findFirst().orElseThrow();assertTrue(option.canPartial());assertFalse(option.canReplacement());
        var requestTwo=request("PARTIAL");requestTwo.setProjectAttributes(fda);
        long second=workflow.submitWorkingIteration(99L,extra,requestTwo);assertEquals("A/2",files.selectById(second).getVersionNo());
        assertEquals(second,workflow.submitWorkingIteration(99L,extra,requestTwo));rejectFixture(second);
        long thirdBody=checkinDistinctBody(second,"attempt-three",new byte[]{4,4,4});
        long third=workflow.submitWorkingIteration(99L,thirdBody,request("PARTIAL"));assertEquals("A/2",files.selectById(third).getVersionNo());
        assertEquals("A/2",files.selectById(first).getVersionNo());assertEquals("本轮失败意见",files.selectById(first).getRejectReason());
        assertEquals(firstRow.getSourceFileId(),files.selectById(first).getSourceFileId());
        assertEquals(firstAttributes,jdbc.queryForMap("SELECT default_source_json,actual_attributes_json,application_round FROM dcc_project_application_attributes WHERE application_id=? AND submitted=1",first));
        var attempts=List.of(files.selectById(first),files.selectById(second),files.selectById(third));
        assertEquals(3,attempts.stream().map(f->f.getProcessInstanceId()).distinct().count());
        assertEquals(3,attempts.stream().map(f->f.getSourceFileId()).distinct().count());
        assertNotNull(engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(firstRow.getProcessInstanceId()).finished().singleResult());
        assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
        assertEquals("ACTIVE",files.selectById(20L).getStatus());assertEquals(20L,masters.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(signature,jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE controlled_file_id=?",first));
        assertEquals(List.of(1,2,3),attempts.stream().map(f->f.getRevisionAttemptNo()).toList());
        assertEquals(first,files.selectById(second).getReworkPredecessorControlledFileId());
        assertEquals(second,files.selectById(third).getReworkPredecessorControlledFileId());
    }
    @Test void signedRejectionEndsOldTasksAndStartsFullNewRoundKeepingOriginalEvidence() throws Exception {
        controlled("A/1");long selected=checkinDistinctBody(20L,"return-first",new byte[]{2,8,2});
        long failed=workflow.submitWorkingIteration(99L,selected,request("PARTIAL"));var old=files.selectById(failed);
        var beforeAttributes=jdbc.queryForMap("SELECT * FROM dcc_project_application_attributes WHERE application_id=? AND submitted=1",failed);
        var oldBody=bytes.get(infraFiles.selectById(old.getSourceFileId()).getPath()).clone();
        var review=engine.getTaskService().createTaskQuery().processInstanceId(old.getProcessInstanceId()).singleResult();engine.getTaskService().complete(review.getId());
        var approval=engine.getTaskService().createTaskQuery().processInstanceId(old.getProcessInstanceId()).singleResult();assertEquals("MATRIX_APPROVAL",approval.getTaskDefinitionKey());
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_APPROVAL' WHERE id=?",failed);
        wireActualSignedRejection();
        var command=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO();command.setTaskId(approval.getId());command.setPassword("isolated-signature-credential");command.setReason("原始退回意见");
        var signed=workflow.rejectTask(99L,failed,command);assertEquals("REJECTED",signed.getTaskActionResult());assertNotNull(signed.getSignatureId());
        var unified=jdbc.queryForMap("SELECT * FROM system_electronic_signature");assertEquals("REJECT",unified.get("action_code"));assertEquals(99L,unified.get("actor_id"));assertEquals("VALID",unified.get("verification_status"));
        assertEquals(old.getProcessInstanceId(),unified.get("process_instance_id"));assertEquals(approval.getId(),unified.get("task_id"));
        assertEquals(0,engine.getTaskService().createTaskQuery().processInstanceId(old.getProcessInstanceId()).count());
        var signature=jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE id=?",signed.getSignatureId());
        assertEquals("VALID",signature.get("evidence_status"));assertEquals("REJECT",signature.get("action_type"));assertEquals(approval.getId(),signature.get("task_id"));
        assertNotNull(signature.get("source_file_hash"));assertNotNull(signature.get("evidence_hash"));
        long correction=checkinDistinctBody(failed,"return-second",new byte[]{3,9,3});
        long next=workflow.submitWorkingIteration(99L,correction,request("PARTIAL"));
        assertEquals("A/2",files.selectById(next).getVersionNo());assertEquals("REJECTED",files.selectById(failed).getStatus());
        assertEquals("原始退回意见",files.selectById(failed).getRejectReason());assertEquals(old.getSourceFileId(),files.selectById(failed).getSourceFileId());
        assertArrayEquals(oldBody,bytes.get(infraFiles.selectById(old.getSourceFileId()).getPath()));assertArrayEquals(new byte[]{3,9,3},bytes.get(infraFiles.selectById(files.selectById(next).getSourceFileId()).getPath()));
        assertEquals(beforeAttributes,jdbc.queryForMap("SELECT * FROM dcc_project_application_attributes WHERE application_id=? AND submitted=1",failed));
        assertEquals(signature,jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE id=?",signed.getSignatureId()));
        assertEquals(unified,jdbc.queryForMap("SELECT * FROM system_electronic_signature"));
        assertNotNull(engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(old.getProcessInstanceId()).finished().singleResult());
        assertNotEquals(old.getProcessInstanceId(),files.selectById(next).getProcessInstanceId());
        assertEquals("MATRIX_REVIEW",engine.getTaskService().createTaskQuery().processInstanceId(files.selectById(next).getProcessInstanceId()).singleResult().getTaskDefinitionKey());
        assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());assertEquals(20L,masters.selectById(10L).getCurrentActiveControlledFileId());
    }
    void wireActualSignedRejection()throws Exception {
        // Real DCC evidence HMAC/projection and real unified sign writer. Only directory,
        // password reauthentication, image availability and append audit are external ports.
        var admin=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.user.AdminUserService.class);
        var user=new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();user.setId(99L);user.setUsername("actual-rejector");user.setNickname("真实拒绝签名人");user.setDeptId(51L);user.setPostIds(Set.of(10L));
        org.mockito.Mockito.when(admin.getUser(99L)).thenReturn(user);
        var posts=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.dept.PostService.class);
        var post=new cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO();post.setId(10L);post.setName("审核岗位");org.mockito.Mockito.when(posts.getPostList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(post));
        var departments=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.dept.DeptService.class);
        var department=new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO();department.setId(51L);department.setName("质量部");org.mockito.Mockito.when(departments.getDept(51L)).thenReturn(department);
        var rolePermissions=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.permission.PermissionService.class);
        org.mockito.Mockito.when(rolePermissions.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(10L));
        var roles=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.permission.RoleService.class);
        var role=new cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO();role.setId(10L);role.setName("审批人");org.mockito.Mockito.when(roles.getRoleList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(role));
        var imagePort=org.mockito.Mockito.mock(DccElectronicSignatureImageService.class);
        org.mockito.Mockito.when(imagePort.requireActiveSnapshot(99L)).thenReturn(DccElectronicSignatureImageSnapshot.builder().imageId(10L).versionNo(1).fileId(100L)
                .fileUrl("isolated-signature-image").sha256("c".repeat(64)).contentType("image/png").fileSize(4L).imageStatus("ENABLED").verifiedStatus("VALID").build());
        var properties=new DccSignatureEvidenceProperties();properties.setHmacSecret("r04-isolated-domain-signature-hmac-key");properties.setKeyVersion("r04-private-v1");
        org.mockito.Mockito.when(storage.getFile(org.mockito.ArgumentMatchers.anyLong())).thenAnswer(c->infraFiles.selectById(c.getArgument(0)));
        var evidence=new DccControlledFileSignatureEvidenceServiceImpl();wire(evidence,"controlledFileMapper",files,"fileService",storage,"signatureEvidenceProperties",properties);
        java.nio.file.Path root=java.nio.file.Path.of("").toAbsolutePath();while(root!=null&&!java.nio.file.Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        String signatureSchema=java.nio.file.Files.readString(root.resolve("IntRuoyiBackend/yudao-module-signature/src/test/resources/sql/create_tables.sql"));
        jdbc.execute(signatureSchema.substring(0,signatureSchema.indexOf("CREATE TABLE IF NOT EXISTS \"system_electronic_signature_review\"")));
        jdbc.update("DELETE FROM system_electronic_signature");
        var configuration=sql.getConfiguration();
        if(!configuration.hasMapper(cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper.class))configuration.addMapper(cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper.class);
        var unified=new cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl();
        wire(unified,"adminUserApi",org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class),
                "signatureRecordMapper",sql.getMapper(cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper.class),
                "gxpAuditService",org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class),"subjectAdapters",List.of(new DccControlledFileSignatureSubjectAdapter()));
        var verifier=new DccSignatureVerificationServiceImpl();wire(verifier,"adminUserService",admin,"deptService",departments,"postService",posts,"permissionService",rolePermissions,"roleService",roles,
                "electronicSignatureAuthorizationService",org.mockito.Mockito.mock(DccElectronicSignatureAuthorizationService.class),"signatureEvidenceService",evidence,
                "signatureImageService",imagePort,"signatureMapper",sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper.class),"electronicSignatureService",unified);
        var processReader=new cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl();wire(processReader,"runtimeService",engine.getRuntimeService(),"historyService",engine.getHistoryService());
        var modelPort=org.mockito.Mockito.mock(cn.iocoder.yudao.module.bpm.service.definition.BpmModelService.class);
        org.mockito.Mockito.when(modelPort.getBpmnModelByDefinitionId(org.mockito.ArgumentMatchers.anyString())).thenAnswer(c->engine.getRepositoryService().getBpmnModel(c.getArgument(0)));
        var taskService=new cn.iocoder.yudao.module.bpm.service.task.BpmTaskServiceImpl();wire(taskService,"taskService",engine.getTaskService(),"runtimeService",engine.getRuntimeService(),"historyService",engine.getHistoryService(),"processInstanceService",processReader,"modelService",modelPort);
        var signedAction=new DccWorkflowSignoffAssignmentService();wire(signedAction,"taskService",engine.getTaskService());
        var permissionPort=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);
        org.mockito.Mockito.when(permissionPort.hasAnyPermissions(org.mockito.ArgumentMatchers.eq(99L),org.mockito.ArgumentMatchers.any(String[].class))).thenReturn(true);
        wire(workflow,"signatureVerificationService",verifier,"signoffAssignmentService",signedAction,"bpmTaskService",taskService,"permissionApi",permissionPort);
        wire(revisions,"bpmTaskService",taskService);
    }
    @Test void localApplicantReworkCannotFabricateARealTaskOrChangeOldEvidence()throws Exception{
        controlled("A/1");long selected=checkinDistinctBody(20L,"spoof-first",new byte[]{2,8,2});long failed=workflow.submitWorkingIteration(99L,selected,request("PARTIAL"));
        var old=files.selectById(failed);var review=engine.getTaskService().createTaskQuery().processInstanceId(old.getProcessInstanceId()).singleResult();engine.getTaskService().complete(review.getId());
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_APPLICANT_REWORK',reject_reason='原始退回意见' WHERE id=?",failed);
        long correction=checkinDistinctBody(failed,"spoof-second",new byte[]{3,9,3});var original=files.selectById(failed);var attrs=jdbc.queryForList("SELECT * FROM dcc_project_application_attributes");
        int before=count("dcc_controlled_file");assertThrows(IllegalArgumentException.class,()->workflow.submitWorkingIteration(99L,correction,request("PARTIAL")));
        assertEquals(before,count("dcc_controlled_file"));assertEquals(original,files.selectById(failed));assertEquals(attrs,jdbc.queryForList("SELECT * FROM dcc_project_application_attributes"));
        assertEquals("MATRIX_APPROVAL",engine.getTaskService().createTaskQuery().processInstanceId(old.getProcessInstanceId()).singleResult().getTaskDefinitionKey());
        assertEquals(0,count("dcc_controlled_file_signature"));assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    private long correction() throws Exception {
        controlled("A/1");long source=checkinDistinctBody(20L,"initial-body",new byte[]{5,5,5});
        long failed=workflow.submitWorkingIteration(99L,source,request("PARTIAL"));rejectFixture(failed);
        return checkinDistinctBody(failed,"correction-body",new byte[]{6,6,6});
    }
    @Test void liveBpmCannotBeReplacedBySpoofingRejectedLocalStatus() throws Exception {
        controlled("A/1");long source=checkinDistinctBody(20L,"live-body",new byte[]{7,7,7});
        long failed=workflow.submitWorkingIteration(99L,source,request("PARTIAL"));
        jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED' WHERE id=?",failed);
        long selected=checkinDistinctBody(failed,"live-correction",new byte[]{8,8,8});int before=count("dcc_controlled_file");int bodiesBefore=bytes.size();
        assertThrows(IllegalArgumentException.class,()->workflow.submitWorkingIteration(99L,selected,request("PARTIAL")));
        assertEquals(before,count("dcc_controlled_file"));assertEquals(bodiesBefore,bytes.size());assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    @ParameterizedTest @ValueSource(strings={"PREDECESSOR","REQUESTER","BASELINE","NAME","UNRELATED_CANDIDATE","STALE_BODY","INTENT"})
    void onlyExactFailedLineageMayReuseTarget(String violation) throws Exception {
        long selected=correction();var chosen=files.selectById(selected);String intent="PARTIAL";
        switch(violation){
            case "PREDECESSOR" -> jdbc.update("UPDATE dcc_controlled_file SET predecessor_controlled_file_id=20 WHERE id=?",selected);
            case "REQUESTER" -> jdbc.update("UPDATE dcc_controlled_file SET requester_id=88 WHERE id=?",selected);
            case "BASELINE" -> jdbc.update("UPDATE dcc_controlled_file SET revision_base_active_controlled_file_id=999 WHERE id=?",selected);
            case "NAME" -> jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='other.pdf' WHERE id=?",selected);
            case "UNRELATED_CANDIDATE" -> {var other=working(999L,null,"B/1");other.setStatus("PENDING_MATRIX_REVIEW");files.insert(other);}
            case "STALE_BODY" -> selected=files.selectById(chosen.getPredecessorControlledFileId()).getSelectedIterationControlledFileId();
            case "INTENT" -> intent="REPLACEMENT";
        }
        long id=selected;var req=request(intent);int before=count("dcc_controlled_file"),bodiesBefore=bytes.size();
        assertThrows(RuntimeException.class,()->workflow.submitWorkingIteration(99L,id,req));
        assertEquals(before,count("dcc_controlled_file"));assertEquals(bodiesBefore,bytes.size());assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void concurrentRetrySerializesUnderMasterAndPreservesReplayIdentity(boolean sameKey) throws Exception {
        long selected=correction();var first=request("PARTIAL");var second=request("PARTIAL");if(sameKey)second.setIdempotencyKey(first.getIdempotencyKey());
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var futures=new ArrayList<Future<Object>>();
            for(var req:List.of(first,second))futures.add(pool.submit(()->{
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
                authenticateFixtureActor();
                try {assertTrue(start.await(5,TimeUnit.SECONDS));return workflow.submitWorkingIteration(99L,selected,req);}
                catch(cn.iocoder.yudao.framework.common.exception.ServiceException | IllegalArgumentException denied){return denied;}
                finally {cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
            }));
            start.countDown();var values=List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS));
            assertEquals(sameKey?2:1,values.stream().filter(v->v instanceof Long).count());
            if(sameKey)assertEquals(values.get(0),values.get(1));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE version_no='A/2'",Integer.class));
            assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
        } finally {start.countDown();pool.shutdownNow();}
    }
}
