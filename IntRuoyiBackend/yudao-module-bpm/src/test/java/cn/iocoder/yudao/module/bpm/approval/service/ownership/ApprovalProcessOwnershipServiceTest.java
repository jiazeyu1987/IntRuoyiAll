package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.dto.ApprovalProcessAuthorityDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalProcessOwnershipMapper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ApprovalProcessOwnershipServiceTest {
    @AfterEach void clear() {
        TenantContextHolder.clear();TransactionSynchronizationManager.setActualTransactionActive(false);
    }
    @Test void requiresRealTransactionAndExactTenantBeforeInsert() {
        var mapper=mock(BpmApprovalProcessOwnershipMapper.class);var service=new ApprovalProcessOwnershipService(mapper);
        TenantContextHolder.setTenantId(1L);
        var process=process();var definition=definition();
        assertThrows(IllegalStateException.class,()->service.record(process,definition,authority(),null));
        verifyNoInteractions(mapper);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        when(process.getTenantId()).thenReturn("2");
        assertThrows(IllegalStateException.class,()->service.record(process,definition,authority(),null));
        verifyNoInteractions(mapper);
    }
    @Test void insertReadbackThenReplayKeepsSingleFactAndConflictingVersionRejects() {
        TenantContextHolder.setTenantId(1L);TransactionSynchronizationManager.setActualTransactionActive(true);
        var mapper=mock(BpmApprovalProcessOwnershipMapper.class);var stored=new AtomicReference<BpmApprovalProcessOwnershipDO>();
        when(mapper.selectExact(1L,"process")).thenAnswer(call->stored.get());
        when(mapper.insert(any())).thenAnswer(call->{var row=(BpmApprovalProcessOwnershipDO)call.getArgument(0);row.setId(9L);stored.set(row);return 1;});
        when(mapper.selectSelectedCandidates(1L,"process")).thenReturn(List.of());
        var service=new ApprovalProcessOwnershipService(mapper);
        service.record(process(),definition(),authority(),null);
        service.record(process(),definition(),authority(),null);
        verify(mapper,times(1)).insert(any());
        var conflict=new ApprovalProcessAuthorityDTO(1L,"NATIVE_BPM","BPM_START","start","different","BPM_MANUAL_START",null);
        assertThrows(IllegalStateException.class,()->service.record(process(),definition(),conflict,null));
        verify(mapper,times(1)).insert(any());
    }
    private static ApprovalProcessAuthorityDTO authority() {
        return new ApprovalProcessAuthorityDTO(1L,"NATIVE_BPM","BPM_START","start","version","BPM_MANUAL_START",null);
    }
    private static ProcessInstance process() {
        var process=mock(ProcessInstance.class);when(process.getId()).thenReturn("process");
        when(process.getTenantId()).thenReturn("1");when(process.getProcessDefinitionId()).thenReturn("definition:1");return process;
    }
    private static ProcessDefinition definition() {
        var definition=mock(ProcessDefinition.class);when(definition.getId()).thenReturn("definition:1");
        when(definition.getKey()).thenReturn("definition");return definition;
    }
}
