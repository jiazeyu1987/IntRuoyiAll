package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccApplicantCancellationGuardTest {
    final DccControlledFileMapper files = mock(DccControlledFileMapper.class);
    final DccControlledFileMasterMapper masters = mock(DccControlledFileMasterMapper.class);
    final FormActionInstanceMapper forms = mock(FormActionInstanceMapper.class);
    final HistoryService history = mock(HistoryService.class);
    final org.flowable.task.api.history.HistoricTaskInstanceQuery taskQuery = mock(org.flowable.task.api.history.HistoricTaskInstanceQuery.class, RETURNS_SELF);
    final org.flowable.engine.history.HistoricActivityInstanceQuery activityQuery = mock(org.flowable.engine.history.HistoricActivityInstanceQuery.class, RETURNS_SELF);
    final ProcessInstance process = mock(ProcessInstance.class);
    final HistoricTaskInstance task = mock(HistoricTaskInstance.class);
    final DccApplicantCancellationGuard guard = new DccApplicantCancellationGuard(files, masters, forms, history);
    DccControlledFileDO file;
    @BeforeEach void setup() {
        when(history.createHistoricTaskInstanceQuery()).thenReturn(taskQuery);
        when(history.createHistoricActivityInstanceQuery()).thenReturn(activityQuery);
        TenantContextHolder.setTenantId(1L);
        when(process.getId()).thenReturn("round-1"); when(process.getTenantId()).thenReturn("1");
        when(process.getStartUserId()).thenReturn("7"); when(process.getBusinessKey()).thenReturn("10");
        when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.UPLOAD);
        file = DccControlledFileDO.builder().id(10L).tenantId(1L).masterId(20L).requesterId(7L)
                .versionNo("A/1").status("PENDING_MATRIX_REVIEW").processInstanceId("round-1")
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).build();
        when(files.selectById(10L)).thenReturn(file);
        when(files.selectByIdAndTenantForUpdate(1L,10L)).thenReturn(file);
        when(masters.selectByIdForUpdate(20L)).thenReturn(DccControlledFileMasterDO.builder().id(20L).tenantId(1L).build());
        when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(history.createHistoricTaskInstanceQuery().processInstanceId("round-1").unfinished().list()).thenReturn(List.of(task));
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }
    @ParameterizedTest @ValueSource(strings={"dcc-controlled-file-upload","dcc-controlled-file-revision"})
    void allowsAllStagesBeforeDocumentControlWithTheSameApprovalLocks(String key) {
        when(process.getProcessDefinitionKey()).thenReturn(key); file.setProcessDefinitionKey(key);
        for (String stage : List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL")) {
            file.setStatus("PENDING_"+stage); when(task.getTaskDefinitionKey()).thenReturn(stage);
            assertDoesNotThrow(()->guard.validate(7L,process,"修订申请内容"));
        }
        var order=inOrder(masters,files); order.verify(files).selectById(10L);
        order.verify(masters).selectByIdForUpdate(20L); order.verify(files).selectByIdAndTenantForUpdate(1L,10L);
    }
    @ParameterizedTest @ValueSource(strings={"DOC_CONTROL_REVIEW","DOC_CONTROL_APPROVAL"})
    void rejectsEnteredUnsignedOrPreviouslyEnteredDocumentControl(String stage) {
        var selectedActivity = mock(org.flowable.engine.history.HistoricActivityInstanceQuery.class, RETURNS_SELF);
        when(activityQuery.activityId(stage)).thenReturn(selectedActivity);
        when(selectedActivity.count()).thenReturn(1L);
        assertThrows(IllegalStateException.class,()->guard.validate(7L,process,"修改"));
    }
    @ParameterizedTest @ValueSource(strings={"PENDING_DOC_CONTROL_REVIEW","PENDING_DOC_CONTROL_APPROVAL",
            "PENDING_APPLICANT_TRAINING_RECORD","ACTIVE","OBSOLETE","WITHDRAWN"})
    void rechecksLockedStateAndRejectsLateCancellation(String status) {
        var locked=DccControlledFileDO.builder().id(10L).tenantId(1L).masterId(20L).requesterId(7L)
                .processInstanceId("round-1").processDefinitionKey(file.getProcessDefinitionKey()).status(status).build();
        when(files.selectByIdAndTenantForUpdate(1L,10L)).thenReturn(locked);
        assertThrows(IllegalStateException.class,()->guard.validate(7L,process,"修改"));
    }
    @Test void rejectsOtherApplicantTenantRoundBlankReasonAndNoLiveTask() {
        assertThrows(IllegalArgumentException.class,()->guard.validate(7L,process,"  "));
        assertThrows(IllegalStateException.class,()->guard.validate(8L,process,"修改"));
        file.setProcessInstanceId("new-round"); assertThrows(IllegalStateException.class,()->guard.validate(7L,process,"修改"));
        file.setProcessInstanceId("round-1"); when(process.getTenantId()).thenReturn("2");
        assertThrows(IllegalStateException.class,()->guard.validate(7L,process,"修改"));
        when(process.getTenantId()).thenReturn("1");
        when(history.createHistoricTaskInstanceQuery().processInstanceId("round-1").unfinished().list()).thenReturn(List.of());
        assertThrows(IllegalStateException.class,()->guard.validate(7L,process,"修改"));
    }
    @Test void obsoleteCancellationPreservesOriginalAndRequiresExactActiveApplication() {
        when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.OBSOLETE);
        var form=FormActionInstanceDO.builder().id(30L).tenantId(1L).bpmProcessInstanceId("round-1")
                .applicantUserId(7L).systemCode("DCC").objectType("CONTROLLED_FILE").actionCode("OBSOLETE")
                .objectId("10").objectVersion("A/1").status("IN_APPROVAL").build();
        when(forms.selectOwnershipByTenantAndProcessId(1L,"round-1")).thenReturn(List.of(form));
        when(forms.selectByIdAndTenantForUpdate(1L,30L)).thenReturn(form); file.setStatus("ACTIVE");
        for(String stage:List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL")) {
            when(task.getTaskDefinitionKey()).thenReturn(stage); assertDoesNotThrow(()->guard.validate(7L,process,"重新核对"));
            assertEquals("ACTIVE",file.getStatus());
        }
        form.setStatus("EFFECTIVE"); assertThrows(IllegalStateException.class,()->guard.validate(7L,process,"修改"));
        form.setStatus("IN_APPROVAL");file.setStatus("OBSOLETE");
        assertThrows(IllegalStateException.class,()->guard.validate(7L,process,"修改"));
    }
}
