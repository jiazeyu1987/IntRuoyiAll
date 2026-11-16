package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesEdhrRecordChangeEditGuardTest {
    @InjectMocks MesEdhrRecordChangeEditGuard guard;
    @Mock MesProEdhrRecordChangeEventMapper events;
    @Mock MesProEdhrWorkTaskMapper tasks;
    @Mock MesProBatchRecordExecutionArchiveMapper archives;
    @Mock MesEdhrRecordChangeApprovalEvidence evidence;

    @Test void statusFiveWithoutFormalDecisionDoesNotGrantEditing() {
        assertThrows(IllegalStateException.class, () -> guard.requireReopened(
                new MesProBatchRecordExecutionDO().setId(1L).setStatus(5), 10L));
        verifyNoInteractions(events, tasks, archives, evidence);
    }

    @Test void approvedReopenRequiresItsOwnReworkTaskAndNoLaterArchive() {
        var execution = new MesProBatchRecordExecutionDO().setId(1L).setStatus(5).setReopenedByChangeEventId(7L);
        var event = new MesProEdhrRecordChangeEventDO().setId(7L).setChangeType("REOPEN")
                .setTargetScope("EXECUTION").setExecutionId(1L);
        var task = new MesProEdhrWorkTaskDO().setId(10L).setTaskType("REWORK").setBusinessScopeType("RECORD_CHANGE")
                .setBusinessScopeId(7L).setExecutionId(1L);
        when(events.selectById(7L)).thenReturn(event);
        when(tasks.selectById(10L)).thenReturn(task);
        var archive = new MesProBatchRecordExecutionArchiveDO().setArchiveValidFlag(false).setInvalidatedByChangeEventId(7L);
        when(archives.selectLatestByExecutionId(1L)).thenReturn(archive);
        assertDoesNotThrow(() -> guard.requireReopened(execution, 10L));
        verify(evidence).requireApproved(event);
        archive.setArchiveValidFlag(true);
        assertThrows(IllegalStateException.class, () -> guard.requireReopened(execution, 10L));
        archive.setArchiveValidFlag(false);
        task.setBusinessScopeId(8L);
        assertThrows(IllegalStateException.class, () -> guard.requireReopened(execution, 10L));
    }
}
