package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class DccApplicationHistoryGuardTest {
    @ParameterizedTest @ValueSource(strings={"TENANT","APPLICANT","BUSINESS","DEFINITION"})
    void nativeContentHistoryCannotBorrowForeignIdentity(String invalid) {
        var file=DccControlledFileDO.builder().id(20L).tenantId(1L).requesterId(99L).versionNo("A/2").build();
        var historic=mock(HistoricProcessInstance.class);when(historic.getId()).thenReturn("actual");when(historic.getTenantId()).thenReturn("1");
        when(historic.getStartUserId()).thenReturn("99");when(historic.getBusinessKey()).thenReturn("20");when(historic.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.REVISION);
        switch(invalid){case "TENANT"->when(historic.getTenantId()).thenReturn("2");case "APPLICANT"->when(historic.getStartUserId()).thenReturn("100");case "BUSINESS"->when(historic.getBusinessKey()).thenReturn("21");case "DEFINITION"->when(historic.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.UPLOAD);}
        var processes=mock(BpmProcessInstanceService.class);when(processes.getHistoricProcessInstance("actual")).thenReturn(historic);var guard=new DccApplicationHistoryGuard();DccWorkflowSelectedIterationDatabaseTest.wire(guard,"processes",processes);
        assertThrows(IllegalStateException.class,()->guard.require(file,"REVISION","actual"));
    }
    @Test void historyInfrastructureFailurePropagatesUnchanged() {
        var processes=mock(BpmProcessInstanceService.class);var error=new IllegalStateException("actual history unavailable");when(processes.getHistoricProcessInstance("actual")).thenThrow(error);
        var guard=new DccApplicationHistoryGuard();DccWorkflowSelectedIterationDatabaseTest.wire(guard,"processes",processes);
        assertSame(error,assertThrows(IllegalStateException.class,()->guard.require(DccControlledFileDO.builder().id(20L).tenantId(1L).build(),"REVISION","actual")));
    }
}
