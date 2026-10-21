package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionS4FlowableTest {
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void formalJobTenantExistsBeforeAsyncCommandAndIsRestoredOnFailure(){
        var configuration=new SpringProcessEngineConfiguration();var fence=mock(ProjectionWriteBoundaryAspect.class);
        IllegalStateException failure=new IllegalStateException("async formal failure");
        configuration.setAsyncExecutorExecuteAsyncRunnableFactory((job,jobs)->()->{assertEquals(2L,TenantContextHolder.getTenantId());throw failure;});
        new ProjectionFlowableConfiguration().projectionEngineFence(fence).configure(configuration);
        JobInfo job=mock(JobInfo.class);when(job.getTenantId()).thenReturn("2");
        Runnable runnable=configuration.getAsyncExecutorExecuteAsyncRunnableFactory().createExecuteAsyncRunnable(job,new JobServiceConfiguration("process"));
        TenantContextHolder.setTenantId(9L);
        assertSame(failure,assertThrows(IllegalStateException.class,runnable::run));assertEquals(9L,TenantContextHolder.getTenantId());
        assertEquals(1,configuration.getCustomPreCommandInterceptors().size());
    }
    @Test void missingOrMalformedJobTenantCannotExecuteFormalCallbacks(){
        var configuration=new SpringProcessEngineConfiguration();Runnable business=mock(Runnable.class);
        configuration.setAsyncExecutorExecuteAsyncRunnableFactory((job,jobs)->business);
        new ProjectionFlowableConfiguration().projectionEngineFence(mock(ProjectionWriteBoundaryAspect.class)).configure(configuration);
        for(String tenant:new String[]{null,"","-1","2,3","9223372036854775808"}) {
            JobInfo job=mock(JobInfo.class);when(job.getTenantId()).thenReturn(tenant);
            assertThrows(IllegalStateException.class,()->configuration.getAsyncExecutorExecuteAsyncRunnableFactory().createExecuteAsyncRunnable(job,new JobServiceConfiguration("process")));
        }
        verifyNoInteractions(business);
    }
}
