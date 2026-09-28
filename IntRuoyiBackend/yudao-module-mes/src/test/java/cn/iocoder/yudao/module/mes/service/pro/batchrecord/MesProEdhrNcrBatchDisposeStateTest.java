package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesProEdhrNcrBatchDisposeStateTest {

    @Test
    void pendingReviewCanBeDisposedWhenBatchHasAdvancedToNonTerminalStatus() {
        var service = new MesProEdhrNonconformanceReviewServiceImpl();
        var review = pendingReview().setPreviousBatchStatus(
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS);
        var batch = new MesProEdhrBatchExecutionDO().setStatus(
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                service, "validateBatchCanDisposeReview", batch, review));
    }

    @Test
    void terminalBatchCannotBeDisposed() {
        var service = new MesProEdhrNonconformanceReviewServiceImpl();
        var review = pendingReview().setPreviousBatchStatus(
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS);
        var batch = new MesProEdhrBatchExecutionDO().setStatus(
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED);

        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "validateBatchCanDisposeReview", batch, review));
    }

    private MesProEdhrNonconformanceReviewDO pendingReview() {
        return new MesProEdhrNonconformanceReviewDO()
                .setReviewStatus(MesProEdhrNonconformanceReviewService.STATUS_PENDING_REVIEW);
    }
}
