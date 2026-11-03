package cn.iocoder.yudao.module.showroom.cover;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.showroom.dal.dataobject.cover.ShowroomProductCoverBatchTaskDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.cover.ShowroomProductCoverBatchTaskItemDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.cover.ShowroomProductCoverBatchTaskMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.cover.ShowroomProductCoverBatchTaskItemMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomProductCoverStartupRecoveryTest extends BaseMockitoUnitTest {

    @Mock
    private ShowroomProductCoverBatchTaskMapper taskMapper;
    @Mock
    private ShowroomProductCoverBatchTaskItemMapper itemMapper;
    @Mock
    private PlatformTransactionManager transactionManager;
    @InjectMocks
    private ShowroomProductCoverBatchTaskService service;

    @Test
    void noUnfinishedTasksDoesNotWrite() {
        service.recoverInterruptedTasksOnStartup();

        verify(taskMapper, never()).recoverRunningTasksToWaiting(any(LocalDateTime.class));
        verify(itemMapper, never()).recoverRunningItemsToWaiting(anyLong());
    }

    @Test
    void waitingTaskWithoutRunningItemsDoesNotWrite() {
        when(taskMapper.selectUnfinishedTasks()).thenReturn(List.of(task("WAITING")));

        service.recoverInterruptedTasksOnStartup();

        verify(taskMapper, never()).recoverRunningTasksToWaiting(any(LocalDateTime.class));
        verify(itemMapper, never()).recoverRunningItemsToWaiting(anyLong());
    }

    @Test
    void runningTaskAndItemsStillRecover() {
        when(taskMapper.selectUnfinishedTasks()).thenReturn(List.of(task("RUNNING")));
        when(itemMapper.selectRunningItemsByTaskId(77L)).thenReturn(List.of(item()));

        service.recoverInterruptedTasksOnStartup();

        verify(taskMapper).recoverRunningTasksToWaiting(any(LocalDateTime.class));
        verify(itemMapper).recoverRunningItemsToWaiting(77L);
    }

    @Test
    void waitingTaskWithRunningItemsOnlyRecoversItems() {
        when(taskMapper.selectUnfinishedTasks()).thenReturn(List.of(task("WAITING")));
        when(itemMapper.selectRunningItemsByTaskId(77L)).thenReturn(List.of(item()));

        service.recoverInterruptedTasksOnStartup();

        verify(taskMapper, never()).recoverRunningTasksToWaiting(any(LocalDateTime.class));
        verify(itemMapper).recoverRunningItemsToWaiting(77L);
    }

    @Test
    void requiredTaskRecoveryFailurePropagates() {
        when(taskMapper.selectUnfinishedTasks()).thenReturn(List.of(task("RUNNING")));
        IllegalStateException failure = new IllegalStateException("read-only recovery blocked");
        when(taskMapper.recoverRunningTasksToWaiting(any(LocalDateTime.class))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, service::recoverInterruptedTasksOnStartup));
    }

    @Test
    void requiredItemRecoveryFailurePropagates() {
        when(taskMapper.selectUnfinishedTasks()).thenReturn(List.of(task("WAITING")));
        when(itemMapper.selectRunningItemsByTaskId(77L)).thenReturn(List.of(item()));
        IllegalStateException failure = new IllegalStateException("read-only item recovery blocked");
        when(itemMapper.recoverRunningItemsToWaiting(77L)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, service::recoverInterruptedTasksOnStartup));
    }

    private static ShowroomProductCoverBatchTaskDO task(String status) {
        return ShowroomProductCoverBatchTaskDO.builder().id(77L).status(status).build();
    }

    private static ShowroomProductCoverBatchTaskItemDO item() {
        return ShowroomProductCoverBatchTaskItemDO.builder().id(88L).taskId(77L).status("RUNNING").build();
    }
}
