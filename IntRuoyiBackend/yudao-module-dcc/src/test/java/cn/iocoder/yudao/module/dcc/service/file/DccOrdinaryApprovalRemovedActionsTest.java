package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTransferTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Removed ordinary-file actions must fail before accessing persistence or workflow dependencies. */
class DccOrdinaryApprovalRemovedActionsTest {
    private final DccControlledFileWorkflowServiceImpl service = new DccControlledFileWorkflowServiceImpl();

    @Test
    void rejectsReturnWithoutCreatingSignatureOrChangingTasks() {
        assertRejected(() -> service.returnTask(1L, 2L, new DccControlledFileReturnTaskReqVO()));
    }

    @Test
    void rejectsTransferWithoutChangingAssignees() {
        assertRejected(() -> service.transferTask(1L, 2L, new DccControlledFileTransferTaskReqVO()));
    }

    @Test
    void rejectsBothSignPositions() {
        for (String type : List.of("before", "after")) {
            DccControlledFileCreateSignTaskReqVO request = new DccControlledFileCreateSignTaskReqVO();
            request.setType(type);
            assertRejected(() -> service.createSignTask(1L, 2L, request));
        }
    }

    @Test
    void rejectsConcurrentStaleRequestsWithoutCallingDependencies() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> transfer = () -> {
                rejectsTransferWithoutChangingAssignees();
                return null;
            };
            Callable<Void> sign = () -> {
                rejectsBothSignPositions();
                return null;
            };
            for (var result : executor.invokeAll(List.of(transfer, sign))) {
                result.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertRejected(Runnable action) {
        ServiceException failure = assertThrows(ServiceException.class, action::run);
        assertEquals(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED.getCode(), failure.getCode());
    }
}
