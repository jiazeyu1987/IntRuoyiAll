package cn.iocoder.yudao.framework.quartz.core.scheduler;

import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.util.Properties;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class SchedulerManagerUniqueJobTest {
    private static CountDownLatch entered;
    private static CountDownLatch release;

    public static class BlockingJob implements Job {
        public void execute(JobExecutionContext context) throws JobExecutionException {
            entered.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new JobExecutionException("test timeout");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new JobExecutionException(ex);
            }
            if (context.getJobDetail().getKey().getName().equals("material")) {
                throw new JobExecutionException("simulated ERP failure");
            }
        }
    }

    @Test
    void rejectsConcurrentSameTableButRunsDifferentTablesAndReleasesAfterFailure() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", "full-sync-admission-test");
        properties.setProperty("org.quartz.threadPool.threadCount", "3");
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        Scheduler scheduler = new StdSchedulerFactory(properties).getScheduler();
        SchedulerManager manager = new SchedulerManager(scheduler);
        entered = new CountDownLatch(2);
        release = new CountDownLatch(1);
        ExecutorService callers = Executors.newFixedThreadPool(3);
        try {
            for (String table : List.of("material", "bom")) {
                scheduler.addJob(JobBuilder.newJob(BlockingJob.class).withIdentity(table).storeDurably().build(), false);
            }
            CountDownLatch start = new CountDownLatch(1);
            Callable<Boolean> submit = () -> {
                start.await();
                try {
                    manager.triggerUniqueJob(1L, "material", "FULL_SYNC", "material");
                    return true;
                } catch (ObjectAlreadyExistsException busy) {
                    return false;
                }
            };
            var first = callers.submit(submit);
            var second = callers.submit(submit);
            var third = callers.submit(submit);
            start.countDown();
            assertEquals(1, (first.get(5, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(5, TimeUnit.SECONDS) ? 1 : 0) + (third.get(5, TimeUnit.SECONDS) ? 1 : 0));
            manager.triggerUniqueJob(2L, "bom", "FULL_SYNC", "bom");
            scheduler.start();
            assertTrue(entered.await(5, TimeUnit.SECONDS), "different tables must run concurrently");
            assertThrows(ObjectAlreadyExistsException.class,
                    () -> manager.triggerUniqueJob(1L, "material", "FULL_SYNC", "material"));
            release.countDown();
            TriggerKey key = new TriggerKey("material", "ERP_FULL_SYNC");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (scheduler.checkExists(key) && System.nanoTime() < deadline) Thread.sleep(10);
            assertFalse(scheduler.checkExists(key), "failed job must release its trigger");
            assertDoesNotThrow(() -> manager.triggerUniqueJob(1L, "material", "FULL_SYNC", "material"));
        } finally {
            release.countDown();
            callers.shutdownNow();
            scheduler.shutdown(true);
        }
    }
}
