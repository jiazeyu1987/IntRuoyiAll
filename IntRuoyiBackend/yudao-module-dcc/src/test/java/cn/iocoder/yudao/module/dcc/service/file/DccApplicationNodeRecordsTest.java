package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Date;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/** Uses the actual authorized application reader, including its existing mapping/history guards. */
class DccApplicationNodeRecordsTest extends DccW08ApplicationRelationEvidenceTest {
    @SuppressWarnings("unchecked") private <T>T dependency(String name){return (T)ReflectionTestUtils.getField(query,name);}
    @Test void exactApplicationEvidenceIncludesAssignmentAndIndependentActualSignoff() throws Exception {
        var relations=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRoundEvidence(1L,23L,12L,5L,"REVISION",7,"actual-bpm",false,"none",List.of(),List.of(),List.of(),"a".repeat(64));
        when(stages.readRoundEvidence(99L,23L,"REVISION","actual-bpm")).thenReturn(relations);
        var assign=DccControlledFileSignatureDO.builder().id(71L).controlledFileId(23L).processInstanceId("actual-bpm").taskId("task-1")
                .versionNo("A/1").actorId(11L).actorNicknameSnapshot("负责人").actorDeptNameSnapshot("本部门").actionType("ASSIGN")
                .signedAt(LocalDateTime.of(2026,10,14,18,0)).comment("跨部门指派").evidenceStatus("VALID").build();
        var sign=DccControlledFileSignatureDO.builder().id(72L).controlledFileId(23L).processInstanceId("actual-bpm").taskId("task-1")
                .versionNo("A/1").actorId(12L).actorNicknameSnapshot("跨部门会签人").actorDeptNameSnapshot("其他部门").actionType("APPROVE")
                .signedAt(LocalDateTime.of(2026,10,14,18,1)).comment("实际会签").evidenceStatus("VALID").build();
        DccControlledFileSignatureMapper signatures=dependency("signatureMapper");when(signatures.selectListByControlledFileId(23L)).thenReturn(List.of(assign,sign));
        HistoricTaskInstance task=mock(HistoricTaskInstance.class);when(task.getId()).thenReturn("task-1");when(task.getProcessInstanceId()).thenReturn("actual-bpm");
        when(task.getTenantId()).thenReturn("1");when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");when(task.getName()).thenReturn("部门会签");when(task.getEndTime()).thenReturn(new Date());
        BpmTaskService tasks=dependency("bpmTaskService");when(tasks.getTaskListByProcessInstanceId("actual-bpm",true)).thenReturn(List.of(task));
        var snapshot=DccControlledFileTaskAssigneeSnapshotDO.builder().id(81L).tenantId(1L).controlledFileId(23L).processInstanceId("actual-bpm")
                .bpmTaskId("task-1").stageCode("MATRIX_REVIEW").assignmentSignatureId(71L).assignedTime(assign.getSignedAt()).leaderUserId(11L)
                .assigneeUserId(12L).assigneeName("跨部门会签人").departmentId(8L).departmentName("本部门").build();
        DccControlledFileTaskAssigneeSnapshotMapper snapshots=dependency("taskAssigneeSnapshotMapper");when(snapshots.selectListByControlledFileId(23L)).thenReturn(List.of(snapshot));
        var response=query.getApplicationEvidence(99L,23L,"REVISION","actual-bpm");
        var records=(List<?>)response.getClass().getMethod("nodeRecords").invoke(response);assertEquals(2,records.size());
        var first=records.get(0);assertEquals("ASSIGN",first.getClass().getMethod("actionType").invoke(first));
        assertNotNull(first.getClass().getMethod("assignmentTarget").invoke(first));
        assertEquals(71L,first.getClass().getMethod("signatureId").invoke(first));
        assertEquals(72L,records.get(1).getClass().getMethod("signatureId").invoke(records.get(1)));
        verify(history).require(file,"REVISION","actual-bpm");
    }
}
