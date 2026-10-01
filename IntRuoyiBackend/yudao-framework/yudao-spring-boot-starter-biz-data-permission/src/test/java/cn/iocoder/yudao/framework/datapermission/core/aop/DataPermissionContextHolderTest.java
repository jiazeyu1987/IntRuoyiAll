package cn.iocoder.yudao.framework.datapermission.core.aop;

import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.ttl.TtlRunnable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * {@link DataPermissionContextHolder} 的单元测试
 *
 * @author 瑛泰源码
 */
class DataPermissionContextHolderTest {

    @BeforeEach
    public void setUp() {
        DataPermissionContextHolder.clear();
    }

    @Test
    public void testGet() {
        // mock 方法
        DataPermission dataPermission01 = mock(DataPermission.class);
        DataPermissionContextHolder.add(dataPermission01);
        DataPermission dataPermission02 = mock(DataPermission.class);
        DataPermissionContextHolder.add(dataPermission02);

        // 调用
        DataPermission result = DataPermissionContextHolder.get();
        // 断言
        assertSame(result, dataPermission02);
    }

    @Test
    public void testPush() {
        // 调用
        DataPermission dataPermission01 = mock(DataPermission.class);
        DataPermissionContextHolder.add(dataPermission01);
        DataPermission dataPermission02 = mock(DataPermission.class);
        DataPermissionContextHolder.add(dataPermission02);
        // 断言
        DataPermission first = DataPermissionContextHolder.getAll().get(0);
        DataPermission second = DataPermissionContextHolder.getAll().get(1);
        assertSame(dataPermission01, first);
        assertSame(dataPermission02, second);
    }

    @Test
    public void testRemove() {
        // mock 方法
        DataPermission dataPermission01 = mock(DataPermission.class);
        DataPermissionContextHolder.add(dataPermission01);
        DataPermission dataPermission02 = mock(DataPermission.class);
        DataPermissionContextHolder.add(dataPermission02);

        // 调用
        DataPermission result = DataPermissionContextHolder.remove();
        // 断言
        assertSame(result, dataPermission02);
        assertEquals(1, DataPermissionContextHolder.getAll().size());
    }

    @Test
    public void testChildThreadCopiesContextBeforeMutation() throws Exception {
        DataPermission parentPermission = mock(DataPermission.class);
        DataPermission childPermission = mock(DataPermission.class);
        CountDownLatch childMutated = new CountDownLatch(1);
        CountDownLatch releaseChild = new CountDownLatch(1);
        AtomicReference<Throwable> childFailure = new AtomicReference<>();
        DataPermissionContextHolder.add(parentPermission);

        Thread child = new Thread(() -> {
            try {
                assertSame(parentPermission, DataPermissionContextHolder.get());
                DataPermissionContextHolder.add(childPermission);
                assertEquals(List.of(parentPermission, childPermission), DataPermissionContextHolder.getAll());
                childMutated.countDown();
                assertTrue(releaseChild.await(5, TimeUnit.SECONDS));
                assertSame(childPermission, DataPermissionContextHolder.remove());
            } catch (Throwable throwable) {
                childFailure.set(throwable);
                childMutated.countDown();
            }
        });

        try {
            child.start();
            assertTrue(childMutated.await(5, TimeUnit.SECONDS));
            assertNull(childFailure.get());
            assertEquals(List.of(parentPermission), DataPermissionContextHolder.getAll());
        } finally {
            releaseChild.countDown();
            child.join(5_000);
            assertFalse(child.isAlive());
            DataPermissionContextHolder.clear();
        }
        assertNull(childFailure.get());
    }

    @Test
    public void testTtlRunnableCopiesContextBeforeMutation() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        DataPermission parentPermission = mock(DataPermission.class);
        DataPermission taskPermission = mock(DataPermission.class);
        CountDownLatch taskMutated = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);
        AtomicReference<Throwable> taskFailure = new AtomicReference<>();

        try {
            Future<?> workerStarted = executor.submit(DataPermissionContextHolder::clear);
            workerStarted.get(5, TimeUnit.SECONDS);

            DataPermissionContextHolder.add(parentPermission);
            Future<?> task = executor.submit(TtlRunnable.get(() -> {
                try {
                    assertSame(parentPermission, DataPermissionContextHolder.get());
                    DataPermissionContextHolder.add(taskPermission);
                    assertEquals(List.of(parentPermission, taskPermission), DataPermissionContextHolder.getAll());
                    taskMutated.countDown();
                    assertTrue(releaseTask.await(5, TimeUnit.SECONDS));
                    assertSame(taskPermission, DataPermissionContextHolder.remove());
                } catch (Throwable throwable) {
                    taskFailure.set(throwable);
                    taskMutated.countDown();
                }
            }));
            assertTrue(taskMutated.await(5, TimeUnit.SECONDS));
            assertNull(taskFailure.get());
            assertEquals(List.of(parentPermission), DataPermissionContextHolder.getAll());
            releaseTask.countDown();
            task.get(5, TimeUnit.SECONDS);
        } finally {
            releaseTask.countDown();
            executor.shutdownNow();
            DataPermissionContextHolder.clear();
        }
        assertNull(taskFailure.get());
    }

}
