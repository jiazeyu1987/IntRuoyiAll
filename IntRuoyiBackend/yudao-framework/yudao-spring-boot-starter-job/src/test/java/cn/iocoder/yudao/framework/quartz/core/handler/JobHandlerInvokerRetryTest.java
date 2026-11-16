package cn.iocoder.yudao.framework.quartz.core.handler;

import cn.iocoder.yudao.framework.quartz.core.enums.JobDataKeyEnum;
import cn.iocoder.yudao.framework.quartz.core.service.JobLogFrameworkService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class JobHandlerInvokerRetryTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handlerCanDisableWholeJobRetriesAndStillRecordsFailure(boolean allowRetry) throws Exception {
        ApplicationContext application = mock(ApplicationContext.class);
        JobLogFrameworkService logs = mock(JobLogFrameworkService.class);
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobHandler handler = new JobHandler() {
            @Override public boolean supportsAutomaticRetry() { return allowRetry; }
            @Override public String execute(String param) { throw new IllegalStateException("page attempts exhausted"); }
        };
        when(application.getBean("testHandler", JobHandler.class)).thenReturn(handler);
        JobDataMap data = new JobDataMap();
        data.put(JobDataKeyEnum.JOB_ID.name(), 1L);
        data.put(JobDataKeyEnum.JOB_HANDLER_NAME.name(), "testHandler");
        data.put(JobDataKeyEnum.JOB_HANDLER_PARAM.name(), "FULL_SYNC");
        data.put(JobDataKeyEnum.JOB_RETRY_COUNT.name(), 3);
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(logs.createJobLog(anyLong(), any(), anyString(), anyString(), anyInt())).thenReturn(9L);
        JobHandlerInvoker invoker = new JobHandlerInvoker();
        ReflectionTestUtils.setField(invoker, "applicationContext", application);
        ReflectionTestUtils.setField(invoker, "jobLogFrameworkService", logs);
        JobExecutionException failure = assertThrows(JobExecutionException.class, () -> invoker.executeInternal(context));
        assertEquals(allowRetry, failure.refireImmediately());
        verify(logs).updateJobLogResultAsync(eq(9L), any(), anyInt(), eq(false), contains("page attempts exhausted"));
    }
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void cancelledTaskNeverRestartsEvenWhenJobRetryIsConfigured(int wrapping) throws Exception {
        ApplicationContext application = mock(ApplicationContext.class);
        JobLogFrameworkService logs = mock(JobLogFrameworkService.class);
        JobExecutionContext context = mock(JobExecutionContext.class);
        RuntimeException cancellation = new java.util.concurrent.CancellationException("user delete");
        IllegalStateException aggregated = new IllegalStateException("Tenant job failed");
        aggregated.addSuppressed(new IllegalStateException("tenant worker stopped", cancellation));
        RuntimeException reported = switch (wrapping) {
            case 0 -> new IllegalStateException("stopped", cancellation);
            case 1 -> aggregated;
            default -> new IllegalStateException("scheduler wrapper", aggregated);
        };
        JobHandler handler = param -> { throw reported; };
        when(application.getBean("testHandler", JobHandler.class)).thenReturn(handler);
        JobDataMap data = new JobDataMap();
        data.put(JobDataKeyEnum.JOB_ID.name(), 1L);
        data.put(JobDataKeyEnum.JOB_HANDLER_NAME.name(), "testHandler");
        data.put(JobDataKeyEnum.JOB_HANDLER_PARAM.name(), "FULL_SYNC");
        data.put(JobDataKeyEnum.JOB_RETRY_COUNT.name(), 3);
        when(context.getMergedJobDataMap()).thenReturn(data);
        JobHandlerInvoker invoker = new JobHandlerInvoker();
        ReflectionTestUtils.setField(invoker, "applicationContext", application);
        ReflectionTestUtils.setField(invoker, "jobLogFrameworkService", logs);
        JobExecutionException failure = assertThrows(JobExecutionException.class, () -> invoker.executeInternal(context));
        assertFalse(failure.refireImmediately());
    }
}
