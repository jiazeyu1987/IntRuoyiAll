package cn.iocoder.yudao.framework.quartz.core.scheduler;

import cn.iocoder.yudao.framework.quartz.core.enums.JobDataKeyEnum;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandlerInvoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchedulerManagerDefinitionTest {
    private final Scheduler scheduler = mock(Scheduler.class);
    private final SchedulerManager manager = new SchedulerManager(scheduler);

    private JobDetail detail() {
        return JobBuilder.newJob(JobHandlerInvoker.class).withIdentity("handler")
                .usingJobData(JobDataKeyEnum.JOB_ID.name(), 7L)
                .usingJobData(JobDataKeyEnum.JOB_HANDLER_NAME.name(), "handler").build();
    }

    private CronTrigger trigger() {
        return TriggerBuilder.newTrigger().withIdentity("handler").forJob("handler")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 12 * * ?"))
                .usingJobData(JobDataKeyEnum.JOB_HANDLER_PARAM.name(), "input")
                .usingJobData(JobDataKeyEnum.JOB_RETRY_COUNT.name(), 2)
                .usingJobData(JobDataKeyEnum.JOB_RETRY_INTERVAL.name(), 3).build();
    }

    private boolean matches(boolean paused) throws SchedulerException {
        return manager.matchesJob(7L, "handler", "input", "0 0 12 * * ?", 2, 3, paused);
    }

    private void existing(JobDetail detail, Trigger trigger, Trigger.TriggerState state) throws Exception {
        when(scheduler.getJobDetail(new JobKey("handler"))).thenReturn(detail);
        lenient().when(scheduler.getTrigger(new TriggerKey("handler"))).thenReturn(trigger);
        lenient().when(scheduler.getTriggerState(new TriggerKey("handler"))).thenReturn(state);
    }

    @Test
    void identicalDefinitionIsReadOnly() throws Exception {
        existing(detail(), trigger(), Trigger.TriggerState.NORMAL);
        assertTrue(matches(false));
        verify(scheduler).getJobDetail(new JobKey("handler"));
        verify(scheduler).getTrigger(new TriggerKey("handler"));
        verify(scheduler).getTriggerState(new TriggerKey("handler"));
        verifyNoMoreInteractions(scheduler);
    }

    @Test
    void pausedAndBlockedStatesMatchWithoutResetting() throws Exception {
        existing(detail(), trigger(), Trigger.TriggerState.PAUSED);
        assertTrue(matches(true));
        assertFalse(matches(false));
        when(scheduler.getTriggerState(new TriggerKey("handler"))).thenReturn(Trigger.TriggerState.BLOCKED);
        assertTrue(matches(false));
        assertFalse(matches(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"job-missing", "trigger-missing", "job-id", "handler", "class", "durable",
            "recovery", "extra-job-data", "param", "retry-count", "retry-interval", "cron", "timezone",
            "misfire", "priority", "calendar", "end", "job-key", "trigger-key", "state-error", "state-complete", "state-none"})
    void differencesNeverPretendToMatch(String difference) throws Exception {
        JobDetail job = detail();
        Trigger trig = trigger();
        Trigger.TriggerState state = Trigger.TriggerState.NORMAL;
        switch (difference) {
            case "job-missing" -> job = null;
            case "trigger-missing" -> trig = null;
            case "job-id" -> job.getJobDataMap().put(JobDataKeyEnum.JOB_ID.name(), 8L);
            case "handler" -> job.getJobDataMap().put(JobDataKeyEnum.JOB_HANDLER_NAME.name(), "different");
            case "class" -> job = job.getJobBuilder().ofType(OtherJob.class).build();
            case "durable" -> job = job.getJobBuilder().storeDurably().build();
            case "recovery" -> job = job.getJobBuilder().requestRecovery().build();
            case "extra-job-data" -> job.getJobDataMap().put("unknown", "value");
            case "param" -> trig.getJobDataMap().put(JobDataKeyEnum.JOB_HANDLER_PARAM.name(), "different");
            case "retry-count" -> trig.getJobDataMap().put(JobDataKeyEnum.JOB_RETRY_COUNT.name(), 4);
            case "retry-interval" -> trig.getJobDataMap().put(JobDataKeyEnum.JOB_RETRY_INTERVAL.name(), 4);
            case "cron" -> trig = trigger().getTriggerBuilder().withSchedule(CronScheduleBuilder.cronSchedule("0 0 13 * * ?")).build();
            case "timezone" -> trig = trigger().getTriggerBuilder().withSchedule(CronScheduleBuilder.cronSchedule("0 0 12 * * ?")
                    .inTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID().equals("UTC") ? "Asia/Shanghai" : "UTC"))).build();
            case "misfire" -> trig = trigger().getTriggerBuilder().withSchedule(CronScheduleBuilder.cronSchedule("0 0 12 * * ?")
                    .withMisfireHandlingInstructionDoNothing()).build();
            case "priority" -> trig = trigger().getTriggerBuilder().withPriority(9).build();
            case "calendar" -> trig = trigger().getTriggerBuilder().modifiedByCalendar("holiday").build();
            case "end" -> trig = trigger().getTriggerBuilder().endAt(new java.util.Date(System.currentTimeMillis()+86400000)).build();
            case "job-key" -> trig = trigger().getTriggerBuilder().forJob("different").build();
            case "trigger-key" -> trig = trigger().getTriggerBuilder().withIdentity("different").build();
            case "state-error" -> state = Trigger.TriggerState.ERROR;
            case "state-complete" -> state = Trigger.TriggerState.COMPLETE;
            case "state-none" -> state = Trigger.TriggerState.NONE;
            default -> fail("Unknown case");
        }
        existing(job, trig, state);
        assertFalse(matches(false), difference);
    }

    @Test
    void queryFailurePropagates() throws Exception {
        when(scheduler.getJobDetail(new JobKey("handler"))).thenThrow(new SchedulerException("read failed"));
        assertThrows(SchedulerException.class, () -> matches(false));
    }

    @Test
    void realQuartzRetainsTriggerAndNextFireTime() throws Exception {
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "definition-read-only-test");
        props.setProperty("org.quartz.threadPool.threadCount", "1");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        Scheduler real = new StdSchedulerFactory(props).getScheduler();
        try {
            SchedulerManager actual = new SchedulerManager(real);
            actual.addJob(7L, "handler", "input", "0 0 12 * * ?", 2, 3);
            Trigger before = real.getTrigger(new TriggerKey("handler"));
            assertTrue(actual.matchesJob(7L, "handler", "input", "0 0 12 * * ?", 2, 3, false));
            assertEquals(before.getStartTime(), real.getTrigger(before.getKey()).getStartTime());
            assertEquals(before.getNextFireTime(), real.getTrigger(before.getKey()).getNextFireTime());
            real.pauseJob(new JobKey("handler"));
            assertTrue(actual.matchesJob(7L, "handler", "input", "0 0 12 * * ?", 2, 3, true));
        } finally {
            real.shutdown(true);
        }
    }

    public static class OtherJob implements Job {
        public void execute(JobExecutionContext context) { }
    }
}
