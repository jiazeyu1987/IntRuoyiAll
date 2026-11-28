package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DccNodeRecordProjectionTest {
    DccControlledFileDO file(){return DccControlledFileDO.builder().id(20L).tenantId(1L).versionNo("A/1").build();}
    HistoricTaskInstance task(String id,String stage){var t=mock(HistoricTaskInstance.class);when(t.getId()).thenReturn(id);when(t.getProcessInstanceId()).thenReturn("bpm");when(t.getTenantId()).thenReturn("1");when(t.getTaskDefinitionKey()).thenReturn(stage);when(t.getName()).thenReturn(stage);return t;}
    DccControlledFileSignatureSummaryRespVO signed(long id,String task){var s=new DccControlledFileSignatureSummaryRespVO();s.setId(id);s.setTaskId(task);s.setProcessInstanceId("bpm");s.setVersionNo("A/1");s.setActorId(99L);s.setActionType("APPROVE");s.setSignedAt(LocalDateTime.of(2026,10,14,19,0).plusMinutes(id));return s;}
    @Test void sameActorActualSignoffAndFinalApprovalAreSeparateRecords(){var a=signed(1,"signoff");var b=signed(2,"approve");var rows=DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(a,b),List.of(task("signoff","MATRIX_REVIEW"),task("approve","MATRIX_APPROVAL")),List.of());assertEquals(2,rows.size());assertEquals(99L,rows.get(0).actorId());assertEquals(99L,rows.get(1).actorId());assertNotEquals(rows.get(0).taskId(),rows.get(1).taskId());}
    DccControlledFileTaskAssigneeSnapshotDO assignment(DccControlledFileSignatureSummaryRespVO signature){
        return DccControlledFileTaskAssigneeSnapshotDO.builder().id(81L).tenantId(1L).controlledFileId(20L).processInstanceId("bpm")
                .bpmTaskId(signature.getTaskId()).stageCode("MATRIX_REVIEW").assignmentSignatureId(signature.getId())
                .leaderUserId(signature.getActorId()).assigneeUserId(12L).assigneeName("实际会签人").departmentName("义务部门")
                .assignedTime(signature.getSignedAt().plusSeconds(1)).build();
    }
    @ParameterizedTest @ValueSource(longs={-60,0,1,60})
    void exactAssignmentBindingDoesNotUseTimeEqualityOrTimeTolerance(long seconds){
        var s=signed(1,"signoff");s.setActionType("ASSIGN");var a=assignment(s);
        a.setAssignedTime(s.getSignedAt().plusSeconds(seconds));var originalTime=a.getAssignedTime();
        var row=DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(s),List.of(task("signoff","MATRIX_REVIEW")),List.of(a)).get(0);
        assertEquals(1L,row.signatureId());assertEquals(12L,row.assignmentTarget().userId());
        assertEquals("实际会签人",row.assignmentTarget().nickname());assertEquals(s.getSignedAt(),row.occurredAt());
        assertEquals(originalTime,a.getAssignedTime());assertEquals(2,row.confirmedFields().size());
    }
    @ParameterizedTest @ValueSource(strings={"signature","duplicate","tenant","file","round","task","stage","actor","deleted","assignee","time"})
    void assignmentStillRequiresUniqueExactIdentityEvenWhenNamesAndTimesMatch(String invalid){
        var s=signed(1,"signoff");s.setActionType("ASSIGN");var a=assignment(s);a.setAssignedTime(s.getSignedAt());
        switch(invalid){case "signature"->a.setAssignmentSignatureId(2L);case "tenant"->a.setTenantId(2L);
            case "file"->a.setControlledFileId(21L);case "round"->a.setProcessInstanceId("other-round");
            case "task"->a.setBpmTaskId("other-task");case "stage"->a.setStageCode("MATRIX_APPROVAL");
            case "actor"->a.setLeaderUserId(100L);case "deleted"->a.setDeleted(true);
            case "assignee"->a.setAssigneeUserId(null);case "time"->a.setAssignedTime(null);}
        assertThrows(IllegalStateException.class,()->DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(s),
                List.of(task("signoff","MATRIX_REVIEW")),"duplicate".equals(invalid)?List.of(a,a):List.of(a)));
    }
    @Test void rejectedSignoffUsesAssignmentSignatureOfExactTaskAndRound(){
        var reject=signed(2,"one");reject.setActionType("REJECT");
        assertEquals("MATRIX_REVIEW_ASSIGN",DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(reject),List.of(task("one","MATRIX_REVIEW")),List.of()).get(0).stageCode());
        var assign=signed(1,"one");assign.setActionType("ASSIGN");var snapshot=assignment(assign);snapshot.setAssigneeUserId(assign.getActorId());
        var rows=DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(assign,reject),List.of(task("one","MATRIX_REVIEW")),List.of(snapshot));
        assertEquals("MATRIX_REVIEW_ASSIGN",rows.get(0).stageCode());assertEquals("MATRIX_REVIEW",rows.get(1).stageCode());
        assertEquals("ASSIGN",assign.getActionType());assertEquals("REJECT",reject.getActionType());assertEquals(assign.getId(),rows.get(0).signatureId());assertEquals(reject.getTaskId(),rows.get(1).taskId());
        reject.setTaskId("two");
        rows=DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(assign,reject),List.of(task("one","MATRIX_REVIEW"),task("two","MATRIX_REVIEW")),List.of(snapshot));
        assertEquals("MATRIX_REVIEW_ASSIGN",rows.get(1).stageCode());
        reject.setTaskId("one");reject.setSignedAt(assign.getSignedAt().minusSeconds(1));
        assertThrows(IllegalStateException.class,()->DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(assign,reject),List.of(task("one","MATRIX_REVIEW")),List.of(snapshot)));
    }
    @Test void rejectsForeignTenantRoundTaskVersionAndRepeatedSignature(){
        var t=task("one","MATRIX_REVIEW");var s=signed(1,"one");
        when(t.getTenantId()).thenReturn("2");assertThrows(IllegalStateException.class,()->DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(s),List.of(t),List.of()));
        when(t.getTenantId()).thenReturn("1");s.setProcessInstanceId("foreign");assertThrows(IllegalStateException.class,()->DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(s),List.of(t),List.of()));
        s.setProcessInstanceId("bpm");s.setVersionNo("B/1");assertThrows(IllegalStateException.class,()->DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(s),List.of(t),List.of()));
        s.setVersionNo("A/1");assertThrows(IllegalStateException.class,()->DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(s,s),List.of(t),List.of()));
        s.setTaskId("another");assertThrows(IllegalStateException.class,()->DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(s),List.of(t),List.of()));
    }
    @Test void realUnsignedCompletedTaskKeepsHistoricalAbsenceInsteadOfInventedSigning(){var t=task("training","APPLICANT_TRAINING_RECORD");when(t.getEndTime()).thenReturn(new Date());when(t.getAssignee()).thenReturn("99");when(t.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS",2));var rows=DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(),List.of(t),List.of());assertEquals(1,rows.size());assertNull(rows.get(0).signatureId());assertNull(rows.get(0).actorNicknameSnapshot());}
    @Test void effectiveBusinessFactRequiresItsExactSignatureIdentityRatherThanMatchingTimestamp(){
        var f=file();var s=signed(1,"doc");var t=task("doc","DOC_CONTROL_STANDARDIZATION");
        f.setProcessInstanceId("bpm");f.setEffectiveTime(s.getSignedAt());f.setEffectiveSignatureId(99L);f.setEffectiveProcessInstanceId("bpm");
        var unrelated=DccApplicationNodeRecordProjection.project(f,"bpm",List.of(s),List.of(t),List.of());assertTrue(unrelated.get(0).confirmedFields().isEmpty());
        f.setEffectiveSignatureId(1L);var exact=DccApplicationNodeRecordProjection.project(f,"bpm",List.of(s),List.of(t),List.of());assertEquals("effectiveTime",exact.get(0).confirmedFields().get(0).field());
    }
}
