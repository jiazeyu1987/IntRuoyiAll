package cn.iocoder.yudao.server.profileworkbench;

import org.flowable.common.engine.impl.interceptor.*;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import org.flowable.job.service.impl.asyncexecutor.AbstractAsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.ExecuteAsyncRunnable;
import java.util.*;

/** Includes timer/engine callbacks before Flowable's transaction interceptor acquires business locks. */
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionFlowableConfiguration {
    @Bean EngineConfigurationConfigurer<SpringProcessEngineConfiguration> projectionEngineFence(ProjectionWriteBoundaryAspect fence) {
        return configuration->{
            // Async commands must receive the formal job tenant before the command transaction starts.
            var originalFactory=configuration.getAsyncExecutorExecuteAsyncRunnableFactory();
            configuration.setAsyncExecutorExecuteAsyncRunnableFactory((job,jobs)->{
                String raw=job.getTenantId();
                if(raw==null||!raw.matches("0|[1-9][0-9]*")) throw new IllegalStateException("Flowable projection job tenant missing or invalid");
                long tenant;
                try {tenant=Long.parseLong(raw);} catch(NumberFormatException failure){throw new IllegalStateException("Flowable projection job tenant exceeds supported range",failure);}
                Runnable original;
                if(originalFactory!=null) original=originalFactory.createExecuteAsyncRunnable(job,jobs);
                else {
                    if(!(jobs.getAsyncExecutor() instanceof AbstractAsyncExecutor executor)) throw new IllegalStateException("Unsupported Flowable projection async executor");
                    original=new ExecuteAsyncRunnable(job,jobs,jobs.getJobEntityManager(),executor.getAsyncRunnableExecutionExceptionHandler(),executor.getJobExecutionObservationProvider());
                }
                if(original==null) throw new IllegalStateException("Flowable async job runnable missing");
                return ()->TenantUtils.execute(tenant,original);
            });
            List<CommandInterceptor> interceptors=new ArrayList<>();
            interceptors.add(new CommandInterceptor(){
                private CommandInterceptor next;
                public CommandInterceptor getNext(){return next;}
                public void setNext(CommandInterceptor value){next=value;}
                @SuppressWarnings("unchecked") public <T>T execute(CommandConfig config,Command<T> command,CommandExecutor executor) {
                    try {return (T)fence.engineWrite(()->next.execute(config,command,executor));}
                    catch(RuntimeException|Error failure){throw failure;}
                    catch(Throwable failure){throw new IllegalStateException("Projection-fenced engine command failed",failure);}
                }
            });
            if(configuration.getCustomPreCommandInterceptors()!=null) interceptors.addAll(configuration.getCustomPreCommandInterceptors());
            configuration.setCustomPreCommandInterceptors(interceptors);
        };
    }
}
