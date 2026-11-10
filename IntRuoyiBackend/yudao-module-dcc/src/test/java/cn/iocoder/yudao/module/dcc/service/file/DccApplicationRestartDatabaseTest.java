package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAttachmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual restart transaction, attribute/matrix inheritance, byte copies, SQL and Flowable history. */
class DccApplicationRestartDatabaseTest extends DccWorkflowSelectedIterationDatabaseTest {
    BpmProcessInstanceService histories;
    @BeforeEach void restartPorts() {
        histories=mock(BpmProcessInstanceService.class);
        when(histories.getHistoricProcessInstance(anyString())).thenAnswer(c->engine.getHistoryService()
                .createHistoricProcessInstanceQuery().processInstanceId(c.getArgument(0)).includeProcessVariables().singleResult());
        when(histories.getProcessInstance(anyString())).thenAnswer(c->engine.getRuntimeService()
                .createProcessInstanceQuery().processInstanceId(c.getArgument(0)).includeProcessVariables().singleResult());
        wire(query,"bpmProcessInstanceService",histories);wire(revisions,"bpmProcessInstanceService",histories);
        wire(workflow,"bpmProcessInstanceService",histories);
        when(access.hasProjectEditorOrOwner(99L,5L)).thenReturn(true);
        when(access.hasProjectOwner(99L,5L)).thenReturn(true);
    }
    void fail(long id,String status) {
        var file=files.selectById(id);
        jdbc.update("INSERT INTO dcc_controlled_file_signature(controlled_file_id,task_id,actor_id,action_type,signature_mode,comment,process_instance_id,version_no,source_file_id,tenant_id) VALUES(?,'failed-task',99,'REJECT','FIXTURE','原轮签名',?,?,?,1)",id,file.getProcessInstanceId(),file.getVersionNo(),file.getSourceFileId());
        tx().executeWithoutResult(s->{engine.getRuntimeService().setVariable(file.getProcessInstanceId(),"PROCESS_STATUS","REJECTED".equals(status)?3:4);
            engine.getRuntimeService().deleteProcessInstance(file.getProcessInstanceId(),"原失败轮次");
            jdbc.update("UPDATE dcc_controlled_file SET status=? WHERE id=?",status,id);});
    }
    DccControlledFileAttachmentMapper useRealAttachments() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_controlled_file_attachment(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,controlled_file_id BIGINT,storage_file_id BIGINT,original_file_name VARCHAR(512),content_type VARCHAR(255),file_size BIGINT,file_sha256 VARCHAR(64),sort_no INT,attachment_purpose VARCHAR(32),application_process_instance_id VARCHAR(128),uploaded_by BIGINT,uploaded_username_snapshot VARCHAR(128),uploaded_nickname_snapshot VARCHAR(128),uploaded_at TIMESTAMP,creator VARCHAR(64),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updater VARCHAR(64),update_time TIMESTAMP,deleted BIT DEFAULT 0)");
        jdbc.update("DELETE FROM dcc_controlled_file_attachment");
        var mapper=sql.getMapper(DccControlledFileAttachmentMapper.class);
        var attachments=new DccControlledFileAttachmentServiceImpl();defaults(attachments);
        var accounts=mock(AdminUserApi.class);when(accounts.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0).setUsername("requester").setNickname("原申请人"));
        var actorScope=mock(DccControlledFileAssignmentScopeService.class);when(actorScope.isWithinAssignedFileScope(anyLong(),anyLong())).thenReturn(true);
        wire(attachments,"attachmentMapper",mapper,"controlledFileMapper",files,"fileMapper",infraFiles,"users",accounts,
                "sourceOwnershipService",bodies,"assignmentScopeService",actorScope,"permissionSupport",permissions,"projectAccessService",access);
        wire(query,"checkinAttachmentMapper",mapper,"attachmentService",attachments);
        return mapper;
    }
    @ParameterizedTest @ValueSource(strings={"REJECTED","WITHDRAWN"})
    void failedFormalRestartPreservesAllFactsWithoutCheckoutAndIsIdempotent(String status) throws Exception {
        controlled("A/3");long selected=checkin(20L,"original body");
        long failed=workflow.submitWorkingIteration(99L,selected,a29ExpectedServerManifest(selected,request("REPLACEMENT")));
        fail(failed,status);var original=files.selectById(failed);
        var signature=jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE controlled_file_id=?",failed);
        var attrs=jdbc.queryForMap("SELECT default_source_json,actual_attributes_json FROM dcc_project_application_attributes WHERE application_id=? AND submitted=1",failed);
        var mapper=useRealAttachments();
        Long stored=storage.createFileAndReturnId(new byte[]{9,8,7},"证据.pdf","restart-attachment/original","application/pdf");
        var attachment=DccControlledFileAttachmentDO.builder().controlledFileId(failed).storageFileId(stored).originalFileName("证据.pdf").contentType("application/pdf").fileSize(3L)
                .fileSha256(bodies.inspectSource(stored).sourceSha256()).sortNo(0).attachmentPurpose("ATTACHMENT").uploadedBy(99L).uploadedUsernameSnapshot("requester").uploadedNicknameSnapshot("原申请人").uploadedAt(LocalDateTime.of(2026,10,1,9,30)).build();attachment.setTenantId(1L);mapper.insert(attachment);
        jdbc.update("INSERT INTO dcc_controlled_file_related_file(tenant_id,controlled_file_id,project_code_id,related_master_id,related_controlled_file_id,related_file_number_snapshot,related_file_name_snapshot,related_version_no_snapshot,relation_source) VALUES(1,?,5,88,89,'R-88','原关联','A/1','MANUAL')",failed);
        changeDefault();int countBefore=count("dcc_controlled_file");
        long next=query.prepareApplicationRestart(99L,failed);
        assertEquals(countBefore+1,count("dcc_controlled_file"));assertEquals(next,query.prepareApplicationRestart(99L,failed));assertEquals(countBefore+1,count("dcc_controlled_file"));
        var draft=files.selectById(next);assertEquals("WORKING",draft.getStatus());assertNull(draft.getProcessInstanceId());assertNull(draft.getRevisionChangeType());
        assertEquals(failed,draft.getPredecessorControlledFileId());assertNotEquals(original.getSourceFileId(),draft.getSourceFileId());assertEquals(original.getSourceSha256(),draft.getSourceSha256());
        assertEquals(original.getDraftApprovalMatrixJson(),draft.getDraftApprovalMatrixJson());assertEquals(original.getSelectedSignoffDepartmentIdsJson(),draft.getSelectedSignoffDepartmentIdsJson());
        assertEquals(attrs,jdbc.queryForMap("SELECT default_source_json,actual_attributes_json FROM dcc_project_application_attributes WHERE application_id=? AND submitted=0",next));
        assertEquals(signature,jdbc.queryForMap("SELECT * FROM dcc_controlled_file_signature WHERE controlled_file_id=?",failed));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_signature WHERE controlled_file_id=?",Integer.class,next));
        assertEquals(1,relations.selectListByControlledFileId(next).size());assertEquals("原关联",relations.selectListByControlledFileId(next).get(0).getRelatedFileNameSnapshot());
        var copied=mapper.selectListByControlledFileId(next).get(0);assertNotEquals(stored,copied.getStorageFileId());assertEquals(attachment.getFileSha256(),copied.getFileSha256());assertEquals(attachment.getUploadedAt(),copied.getUploadedAt());assertNull(copied.getApplicationProcessInstanceId());
        assertNull(checkouts.selectActiveByMasterIdForRead(1L,10L));assertEquals(20L,masters.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(status,files.selectById(failed).getStatus());assertEquals(original.getProcessInstanceId(),files.selectById(failed).getProcessInstanceId());
        assertThrows(RuntimeException.class,()->query.prepareApplicationRestart(88L,failed));
        TenantContextHolder.setTenantId(2L);try{assertThrows(RuntimeException.class,()->query.prepareApplicationRestart(99L,failed));}finally{TenantContextHolder.setTenantId(1L);}
    }
    @ParameterizedTest @ValueSource(strings={"PARTIAL","REPLACEMENT"})
    void originalWorkingBodyCanImmediatelyStartAnotherAttemptOrSwitchIntent(String intent) {
        controlled("A/3");long selected=checkin(20L,"original selected");
        long first=workflow.submitWorkingIteration(99L,selected,a29ExpectedServerManifest(selected,request(intent)));fail(first,"WITHDRAWN");
        var options=query.getRevisionOptions(99L,20L);var body=options.iterations().stream().filter(i->i.id().equals(selected)).findFirst().orElseThrow();
        assertTrue(body.canPartial());assertTrue(body.canReplacement());assertTrue(options.iterations().stream().noneMatch(i->i.id().equals(first)));
        long second=workflow.submitWorkingIteration(99L,selected,a29ExpectedServerManifest(selected,request(intent)));
        assertEquals(2,files.selectById(second).getRevisionAttemptNo());fail(second,"REJECTED");
        String other="PARTIAL".equals(intent)?"REPLACEMENT":"PARTIAL";
        long switched=workflow.submitWorkingIteration(99L,selected,a29ExpectedServerManifest(selected,request(other)));
        assertEquals(other,files.selectById(switched).getRevisionChangeType());assertNull(checkouts.selectActiveByMasterIdForRead(1L,10L));
    }
}
