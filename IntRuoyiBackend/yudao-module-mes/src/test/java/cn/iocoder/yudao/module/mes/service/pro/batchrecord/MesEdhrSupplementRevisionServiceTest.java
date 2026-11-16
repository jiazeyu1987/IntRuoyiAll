package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesEdhrSupplementRevisionServiceTest {
    @InjectMocks MesEdhrSupplementRevisionService service;
    @Mock MesProBatchRecordExecutionMapper executions;
    @Mock MesProEdhrBatchExecutionTaskMapper batchTasks;

    @Test void draftCopiesFrozenContractWithoutRewritingSignedOriginal() {
        var original = new MesProBatchRecordExecutionDO().setId(1L).setExecutionCode("ORIGINAL")
                .setStatus(3).setBatchExecutionId(2L).setBatchRecordVersionId(3L)
                .setCellValuesJson("[{\"value\":42}]").setCellValuesHash("signed-values")
                .setFieldAuditRevision(25L).setFieldAuditHeadHash("signed-head")
                .setActiveRevisionFlag(true).setRevisionNo(1).setRevisionRootExecutionId(1L)
                .setProcessInstanceId("old-process").setApprovedBy(100L)
                .setActiveContextKey("formal-context").setExecutionSnapshotJson("frozen-layout");
        when(executions.insert(any(MesProBatchRecordExecutionDO.class))).thenAnswer(call -> {
            ((MesProBatchRecordExecutionDO) call.getArgument(0)).setId(9L); return 1;
        });
        when(batchTasks.selectList(any())).thenReturn(List.of(new MesProEdhrBatchExecutionTaskDO().setId(11L).setExecutionId(1L)));
        when(batchTasks.updateById(any(MesProEdhrBatchExecutionTaskDO.class))).thenReturn(1);
        var revision = service.create(new MesProEdhrRecordChangeEventDO().setId(7L).setReasonText("补录说明"), original);
        assertEquals(0, revision.getStatus());
        assertEquals(2, revision.getRevisionNo());
        assertEquals(1L, revision.getSupplementSourceExecutionId());
        assertEquals("signed-head", revision.getRevisionParentHash());
        assertEquals(original.getCellValuesJson(), revision.getCellValuesJson());
        assertEquals(original.getExecutionSnapshotJson(), revision.getExecutionSnapshotJson());
        assertEquals(0L, revision.getFieldAuditRevision());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH, revision.getFieldAuditHeadHash());
        assertNull(revision.getApprovedBy()); assertNull(revision.getProcessInstanceId());
        assertNull(revision.getSubmittedBy()); assertNull(revision.getClosedAt());
        assertEquals(3, original.getStatus()); assertEquals("signed-values", original.getCellValuesHash());
        assertEquals("signed-head", original.getFieldAuditHeadHash()); assertEquals(100L, original.getApprovedBy());
        verify(executions).clearActiveContextKey(1L);
    }

    @Test void historicalVersionCannotGenerateAnotherParallelDraft() {
        assertThrows(IllegalStateException.class, () -> service.create(new MesProEdhrRecordChangeEventDO(),
                new MesProBatchRecordExecutionDO().setActiveRevisionFlag(false)));
        verifyNoInteractions(executions, batchTasks);
    }
}
