package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrHistoryReadOnlyTest {
    private final MesProEdhrBatchExecutionServiceImpl service = new MesProEdhrBatchExecutionServiceImpl();
    private final MesProEdhrReleaseTransactionMapper releases = mock(MesProEdhrReleaseTransactionMapper.class);
    private final MesProEdhrBatchExecutionTaskMapper tasks = mock(MesProEdhrBatchExecutionTaskMapper.class);

    MesProEdhrHistoryReadOnlyTest() {
        ReflectionTestUtils.setField(service, "releaseTransactionMapper", releases);
        ReflectionTestUtils.setField(service, "batchTaskMapper", tasks);
    }

    @Test
    void releasedHistoryCannotEnterAnyReadSynchronizationPath() {
        var batch = new MesProEdhrBatchExecutionDO().setId(9001L).setStatus(10);
        when(releases.selectByBatchExecutionId(9001L)).thenReturn(new MesProEdhrReleaseTransactionDO().setReleaseStatus("RELEASED"));
        assertEquals(false, ReflectionTestUtils.invokeMethod(service, "isActiveBatch", batch));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "syncIfActive", batch));
        assertSame(batch, ReflectionTestUtils.invokeMethod(service, "recoverMissingRouteFormTasksBeforeRead", batch));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "recoverMissingRouteFormTasksBeforePageRendering", batch));
        verifyNoInteractions(tasks);
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 40, 50, 60})
    void terminalBatchCannotEnterReadSynchronization(int status) {
        var batch = new MesProEdhrBatchExecutionDO().setId(9001L).setStatus(status);
        assertEquals(false, ReflectionTestUtils.invokeMethod(service, "isActiveBatch", batch));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "syncIfActive", batch));
        verifyNoInteractions(tasks);
    }

    @Test
    void activeUnreleasedBatchRetainsItsOriginalLifecycle() {
        var batch = new MesProEdhrBatchExecutionDO().setId(9001L).setStatus(10);
        when(releases.selectByBatchExecutionId(9001L)).thenReturn(new MesProEdhrReleaseTransactionDO().setReleaseStatus("DRAFT"));
        assertEquals(true, ReflectionTestUtils.invokeMethod(service, "isActiveBatch", batch));
    }
}
