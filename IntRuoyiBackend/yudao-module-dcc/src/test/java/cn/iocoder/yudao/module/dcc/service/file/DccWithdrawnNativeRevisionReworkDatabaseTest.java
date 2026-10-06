package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileWithdrawReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckoutReqVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public WF withdrawal + actual Flowable cancellation/history + real Query correction SQL.
 * Account/signature/platform/storage ports retain the explicitly isolated parent boundaries. */
class DccWithdrawnNativeRevisionReworkDatabaseTest extends DccWorkflowSelectedIterationDatabaseTest {
    private BpmProcessInstanceService processes;

    @BeforeEach
    void realCancelledHistoryPort() {
        var formal=new BpmProcessInstanceServiceImpl();
        ReflectionTestUtils.setField(formal,"runtimeService",engine.getRuntimeService());
        ReflectionTestUtils.setField(formal,"historyService",engine.getHistoryService());
        processes=mock(BpmProcessInstanceService.class);
        when(processes.getHistoricProcessInstance(anyString())).thenAnswer(c->formal.getHistoricProcessInstance(c.getArgument(0)));
        when(processes.getProcessInstance(anyString())).thenAnswer(c->formal.getProcessInstance(c.getArgument(0)));
        doAnswer(c->{
            var request=c.<cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO>getArgument(1);
            var instance=engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(request.getId()).singleResult();
            assertEquals("99",instance.getStartUserId());
            engine.getRuntimeService().setVariable(request.getId(),"PROCESS_STATUS",4);
            engine.getRuntimeService().deleteProcessInstance(request.getId(),request.getReason());
            return null;
        }).when(processes).cancelProcessInstanceByStartUser(eq(99L),any());
        wire(workflow,"bpmProcessInstanceService",processes);wire(revisions,"bpmProcessInstanceService",processes);
        if(Arrays.stream(query.getClass().getSuperclass().getDeclaredFields()).anyMatch(f->f.getName().equals("bpmProcessInstanceService")))
            wire(query,"bpmProcessInstanceService",processes);
    }

    long cancelledRevision() {
        controlled("A/3");changeDefault();long selected=checkin(20L,"saved selected body");
        long candidate=workflow.submitWorkingIteration(99L,selected,request("REPLACEMENT"));
        var reason=new DccControlledFileWithdrawReqVO();reason.setReason("真实撤回保留申请历史");
        workflow.withdrawControlledFile(99L,candidate,reason);
        assertEquals("WITHDRAWN",files.selectById(candidate).getStatus());
        var history=processes.getHistoricProcessInstance(files.selectById(candidate).getProcessInstanceId());
        assertNotNull(history.getEndTime());assertEquals(4,history.getProcessVariables().get("PROCESS_STATUS"));
        return candidate;
    }

    @Test
    void actualWithdrawnNativeRevisionCheckoutCheckinAndSameTargetAttemptPreserveHistory() throws Exception {
        long withdrawn=cancelledRevision();var old=files.selectById(withdrawn);String oldRound=old.getProcessInstanceId();
        var before=jdbc.queryForMap("SELECT default_source_json,actual_attributes_json FROM dcc_project_application_attributes WHERE application_id=? AND submitted=1",withdrawn);
        long corrected=checkinDistinctBody(withdrawn,"withdrawn exact correction",new byte[]{1,9,4});
        assertEquals("B/1-1",files.selectById(corrected).getVersionNo());
        long next=workflow.submitWorkingIteration(99L,corrected,request("REPLACEMENT"));
        assertEquals("B/1",files.selectById(next).getVersionNo());assertEquals(2,files.selectById(next).getRevisionAttemptNo());
        assertEquals(withdrawn,files.selectById(next).getReworkPredecessorControlledFileId());
        assertNotEquals(oldRound,files.selectById(next).getProcessInstanceId());
        assertEquals("WITHDRAWN",files.selectById(withdrawn).getStatus());assertEquals(oldRound,files.selectById(withdrawn).getProcessInstanceId());
        assertEquals("B/1",files.selectById(withdrawn).getVersionNo());assertEquals(old.getSourceFileId(),files.selectById(withdrawn).getSourceFileId());
        assertEquals(before,jdbc.queryForMap("SELECT default_source_json,actual_attributes_json FROM dcc_project_application_attributes WHERE application_id=? AND submitted=1",withdrawn));
        assertEquals(20L,masters.selectById(10L).getCurrentActiveControlledFileId());
        assertNull(checkouts.selectActiveByMasterIdForRead(1L,10L));
        assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
        assertThrows(RuntimeException.class,()->checkout(withdrawn),"a replaced older withdrawn attempt cannot take the current correction lock");
    }

    @Test
    void localWithdrawnStatusCannotEditAnActuallyRunningProcessAndOtherActorCannotAcquireItsLock() {
        controlled("A/3");long selected=checkin(20L,"selected");long candidate=workflow.submitWorkingIteration(99L,selected,request("REPLACEMENT"));
        jdbc.update("UPDATE dcc_controlled_file SET status='WITHDRAWN' WHERE id=?",candidate);
        assertThrows(RuntimeException.class,()->checkout(candidate));assertNull(checkouts.selectActiveByMasterIdForRead(1L,10L));
        engine.getRuntimeService().setVariable(files.selectById(candidate).getProcessInstanceId(),"PROCESS_STATUS",4);
        engine.getRuntimeService().deleteProcessInstance(files.selectById(candidate).getProcessInstanceId(),"isolated cancelled fixture");
        var request=new DccControlledFileCheckoutReqVO();request.setReason("另一人无权接管撤回申请");
        assertThrows(RuntimeException.class,()->query.checkoutControlledFile(88L,candidate,request));
        assertNull(checkouts.selectActiveByMasterIdForRead(1L,10L));
    }

    @Test
    void otherNativeKindsWrongBaselineAndLegacyResubmitCannotConsumeTheAllocatedTarget() {
        long withdrawn=cancelledRevision();
        for(String key:java.util.List.of(DccControlledFileProcessDefinitionKeys.UPLOAD,
                DccControlledFileProcessDefinitionKeys.OBSOLETE,DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL)) {
            jdbc.update("UPDATE dcc_controlled_file SET process_definition_key=? WHERE id=?",key,withdrawn);
            assertThrows(RuntimeException.class,()->checkout(withdrawn));
            assertNull(checkouts.selectActiveByMasterIdForRead(1L,10L));
        }
        jdbc.update("UPDATE dcc_controlled_file SET process_definition_key=? WHERE id=?",DccControlledFileProcessDefinitionKeys.REVISION,withdrawn);
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=? WHERE id=10",withdrawn);
        assertThrows(RuntimeException.class,()->checkout(withdrawn));
        assertNull(checkouts.selectActiveByMasterIdForRead(1L,10L));
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20 WHERE id=10");
        assertThrows(RuntimeException.class,()->workflow.resubmitWithdrawnControlledFile(99L,withdrawn));
        assertThrows(RuntimeException.class,()->workflow.deleteWithdrawnControlledFile(99L,withdrawn));
        assertEquals("WITHDRAWN",files.selectById(withdrawn).getStatus());assertEquals("B/1",files.selectById(withdrawn).getVersionNo());
        assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
}
