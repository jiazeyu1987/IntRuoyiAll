package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignoffAssignmentReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** W05 orchestration + real W04 prepare/freeze/remediation and actual private H2 Spring transaction. */
class DccWorkflowRelationStageIntegrationTest extends DccRelationStageSnapshotServiceTest {
    DccWorkflowRelationStageIntegrationTest() throws Exception { super(); }

    DccWorkflowSignoffAssignmentService assignment(boolean lateFailure) {
        jdbc.execute("CREATE TABLE w05_assignment_fact(id BIGINT)");
        jdbc.execute("CREATE TABLE w05_bpm_fact(id BIGINT)");
        var file=DccControlledFileDO.builder().id(100L).tenantId(1L).masterId(10L).dccProjectCodeId(2L)
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION).processInstanceId("round-1").status("PENDING_MATRIX_REVIEW").build();
        var fileMapper=mock(DccControlledFileMapper.class);when(fileMapper.selectById(100L)).thenReturn(file);
        when(fileMapper.selectByIdAndTenantForUpdate(1L,100L)).thenReturn(file);
        var masters=mock(DccControlledFileMasterMapper.class);
        when(masters.selectByIdForUpdate(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(1L).build());
        var row=DccControlledFileTaskAssigneeSnapshotDO.builder().id(1L).tenantId(1L).controlledFileId(100L).stageCode("MATRIX_REVIEW")
                .departmentId(5L).leaderUserId(7L).assigneeUserId(7L).processInstanceId("round-1")
                .obligationId("dept-5").nodeInstanceId("node-t1").build();
        var snapshots=mock(DccControlledFileTaskAssigneeSnapshotMapper.class);
        when(snapshots.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(row);
        when(snapshots.updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class))).thenAnswer(call->{
            var update=call.<DccControlledFileTaskAssigneeSnapshotDO>getArgument(0);
            row.setAssignmentSignatureId(update.getAssignmentSignatureId());row.setAssignmentPayloadHash(update.getAssignmentPayloadHash());
            row.setAssignedTime(update.getAssignedTime());row.setBpmTaskId(update.getBpmTaskId());
            row.setAssigneeUserId(update.getAssigneeUserId());jdbc.update("INSERT INTO w05_assignment_fact VALUES(1)");return 1;
        });
        var task=mock(Task.class);when(task.getId()).thenReturn("t1");when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(task.getExecutionId()).thenReturn("node-t1");when(task.getProcessInstanceId()).thenReturn("round-1");
        when(task.getTenantId()).thenReturn("1");when(task.getProcessDefinitionId()).thenReturn("definition-revision-4");
        when(task.getTaskLocalVariables()).thenReturn(Map.of(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID,"dept-5"));
        var bpm=mock(BpmTaskService.class);when(bpm.getTask("t1")).thenReturn(task);when(bpm.validateTask(7L,"t1")).thenReturn(task);
        var definitions=mock(BpmProcessDefinitionService.class);var definition=mock(org.flowable.engine.repository.ProcessDefinition.class);
        when(definition.getId()).thenReturn("definition-revision-4");when(definition.getTenantId()).thenReturn("1");
        when(definition.getKey()).thenReturn(DccControlledFileProcessDefinitionKeys.REVISION);
        when(definitions.getProcessDefinition("definition-revision-4")).thenReturn(definition);
        var users=mock(AdminUserApi.class);when(users.getUser(8L)).thenReturn(new AdminUserRespDTO().setId(8L).setTenantId(1L)
                .setStatus(0).setDeptId(5L).setNickname("本部门实际会签人").setPostIds(Set.of(10L)));
        var signatures=mock(DccSignatureVerificationService.class);
        when(signatures.verifyPasswordAndCreateWorkflowSignature(eq(7L),eq(100L),eq("t1"),eq("round-1"),eq("MATRIX_REVIEW"),eq("ASSIGN"),anyString(),anyString()))
                .thenAnswer(call->{
                    String reason=call.getArgument(7);
                    assertTrue(reason.contains("dccRelationPayloadHash="));
                    jdbc.update("INSERT INTO dcc_controlled_file_signature VALUES(99,100,'t1','round-1',7,?,'VALID','evidence-99','ASSIGN',?,0)",signedAt,reason);
                    return DccUnifiedSignatureResult.builder().signatureId(99L).signedAt(signedAt).evidenceHash("evidence-99").evidenceStatus("VALID").build();
                });
        var signatureMapper=mock(DccControlledFileSignatureMapper.class);
        when(signatureMapper.selectActionSignature(100L,"t1",7L,"ASSIGN")).thenReturn(DccControlledFileSignatureDO.builder()
                .id(99L).processInstanceId("round-1").signedAt(signedAt).evidenceHash("evidence-99").evidenceStatus("VALID").build());
        var tasks=mock(TaskService.class);doAnswer(call->{jdbc.update("INSERT INTO w05_bpm_fact VALUES(1)");
            if(lateFailure)throw new IllegalStateException("actual BPM port rejected after signed stage freeze");return null;
        }).when(tasks).setAssignee("t1","8");
        var result=new DccWorkflowSignoffAssignmentService();
        var fields=Map.<String,Object>ofEntries(Map.entry("fileMapper",fileMapper),Map.entry("masterMapper",masters),Map.entry("snapshotMapper",snapshots),
                Map.entry("bpmTaskService",bpm),Map.entry("definitions",definitions),Map.entry("taskService",tasks),Map.entry("adminUserApi",users),
                Map.entry("readinessService",mock(DccControlledFileRouteReadinessService.class)),Map.entry("signatureService",signatures),
                Map.entry("signatureMapper",signatureMapper),Map.entry("relationStageService",service),Map.entry("remediationService",remediation));
        fields.forEach((name,value)->ReflectionTestUtils.setField(result,name,value));
        ReflectionTestUtils.setField(result,"rounds",rounds);
        return result;
    }
    DccSignoffAssignmentReqVO command() {
        var command=new DccSignoffAssignmentReqVO();command.setTaskId("t1");command.setAssigneeUserId(8L);command.setPassword("isolated-password");
        command.setReason("补充关联并指派");command.setSupplementalRelatedControlledFileIds(List.of(300L));
        command.setRelationArrangements(List.of(new Arrangement(30L,8L,signedAt.plusDays(2))));return command;
    }
    @Test void realAssignmentFreezesUploadPlusSupplementAndArrangementThenExactReplayUsesOriginalSignature() {
        var assignment=assignment(false);var command=command();
        tx.executeWithoutResult(status->assignment.assign(7L,100L,command));
        assertEquals(1,count("dcc_relation_stage_snapshot"));assertEquals(2,count("dcc_relation_stage_member"));
        assertEquals(1,count("dcc_relation_arrangement"));assertEquals(1,count("w05_assignment_fact"));assertEquals(1,count("w05_bpm_fact"));
        var frozen=jdbc.queryForList("SELECT * FROM dcc_relation_stage_member ORDER BY related_master_id");
        tx.executeWithoutResult(status->assignment.assign(7L,100L,command));
        assertEquals(1,count("dcc_controlled_file_signature"));assertEquals(1,count("w05_bpm_fact"));
        assertEquals(frozen,jdbc.queryForList("SELECT * FROM dcc_relation_stage_member ORDER BY related_master_id"));
        assertEquals(200L,jdbc.queryForObject("SELECT related_controlled_file_id FROM dcc_controlled_file_related_file",Long.class));
        var omitted=command();omitted.setSupplementalRelatedControlledFileIds(null);
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(status->assignment.assign(7L,100L,omitted)));
    }
    @Test void actualLateBpmFailureRollsBackUnifiedAssignmentProjectionStageArrangementAndTaskTogether() {
        var assignment=assignment(true);
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->assignment.assign(7L,100L,command())));
        for(var table:List.of("dcc_controlled_file_signature","dcc_relation_stage_snapshot","dcc_relation_stage_member",
                "dcc_relation_arrangement","w05_assignment_fact","w05_bpm_fact"))assertEquals(0,count(table),table);
        assertEquals(200L,jdbc.queryForObject("SELECT related_controlled_file_id FROM dcc_controlled_file_related_file",Long.class));
    }
}
