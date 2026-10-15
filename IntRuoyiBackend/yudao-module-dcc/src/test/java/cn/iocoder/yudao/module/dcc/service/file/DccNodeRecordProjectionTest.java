package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DccNodeRecordProjectionTest {
    DccControlledFileDO file(){return DccControlledFileDO.builder().id(20L).tenantId(1L).versionNo("A/1").build();}
    HistoricTaskInstance task(String id,String stage){var t=mock(HistoricTaskInstance.class);when(t.getId()).thenReturn(id);when(t.getProcessInstanceId()).thenReturn("bpm");when(t.getTenantId()).thenReturn("1");when(t.getTaskDefinitionKey()).thenReturn(stage);when(t.getName()).thenReturn(stage);return t;}
    DccControlledFileSignatureSummaryRespVO signed(long id,String task){var s=new DccControlledFileSignatureSummaryRespVO();s.setId(id);s.setTaskId(task);s.setProcessInstanceId("bpm");s.setVersionNo("A/1");s.setActorId(99L);s.setActionType("APPROVE");s.setSignedAt(LocalDateTime.of(2026,10,14,19,0).plusMinutes(id));return s;}
    @Test void sameActorActualSignoffAndFinalApprovalAreSeparateRecords(){var a=signed(1,"signoff");var b=signed(2,"approve");var rows=DccApplicationNodeRecordProjection.project(file(),"bpm",List.of(a,b),List.of(task("signoff","MATRIX_REVIEW"),task("approve","MATRIX_APPROVAL")),List.of());assertEquals(2,rows.size());assertEquals(99L,rows.get(0).actorId());assertEquals(99L,rows.get(1).actorId());assertNotEquals(rows.get(0).taskId(),rows.get(1).taskId());}
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
