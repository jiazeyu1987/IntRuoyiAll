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
    @Test void actualApplicantReworkTaskEndsBeforeResubmissionAndKeepsOriginalReason() throws Exception {
        String xml="""
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="test">
            <process id="dcc-controlled-file-revision" isExecutable="true"><startEvent id="start"/>
            <sequenceFlow id="one" sourceRef="start" targetRef="MATRIX_REVIEW"/>
            <userTask id="MATRIX_REVIEW" flowable:assignee="99"/><sequenceFlow id="two" sourceRef="MATRIX_REVIEW" targetRef="APPLICANT_REWORK"/>
            <userTask id="APPLICANT_REWORK" flowable:assignee="99"/><sequenceFlow id="three" sourceRef="APPLICANT_REWORK" targetRef="end"/><endEvent id="end"/>
            </process></definitions>
            """;
        var deployment=engine.getRepositoryService().createDeployment().tenantId("1").addString("rework.bpmn20.xml",xml).deploy();
        try {
            controlled("A/1");long selected=checkinDistinctBody(20L,"return-first",new byte[]{2,8,2});
            long failed=workflow.submitWorkingIteration(99L,selected,request("PARTIAL"));var old=files.selectById(failed);
            var review=engine.getTaskService().createTaskQuery().processInstanceId(old.getProcessInstanceId()).singleResult();engine.getTaskService().complete(review.getId());
            jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_APPLICANT_REWORK',reject_reason='原始退回意见' WHERE id=?",failed);
            long correction=checkinDistinctBody(failed,"return-second",new byte[]{3,9,3});
            long next=workflow.submitWorkingIteration(99L,correction,request("PARTIAL"));
            assertEquals("A/2",files.selectById(next).getVersionNo());assertEquals("WITHDRAWN",files.selectById(failed).getStatus());
            assertEquals("原始退回意见",files.selectById(failed).getRejectReason());
            assertNotNull(engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(old.getProcessInstanceId()).finished().singleResult());
            assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
        } finally {engine.getRepositoryService().deleteDeployment(deployment.getId(),true);}
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
                try {assertTrue(start.await(5,TimeUnit.SECONDS));return workflow.submitWorkingIteration(99L,selected,req);}
                catch(cn.iocoder.yudao.framework.common.exception.ServiceException | IllegalArgumentException denied){return denied;}
                finally {cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();}
            }));
            start.countDown();var values=List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS));
            assertEquals(sameKey?2:1,values.stream().filter(v->v instanceof Long).count());
            if(sameKey)assertEquals(values.get(0),values.get(1));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE version_no='A/2'",Integer.class));
            assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
        } finally {start.countDown();pool.shutdownNow();}
    }
}
