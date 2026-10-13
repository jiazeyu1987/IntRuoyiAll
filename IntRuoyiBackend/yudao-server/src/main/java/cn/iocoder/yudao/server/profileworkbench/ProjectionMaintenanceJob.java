package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Set;

/** Explicit administrator job, with no default operation or automatic engine switch. */
@Component("workbenchProjectionMaintenanceJob")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionMaintenanceJob implements JobHandler {
    public record Command(String operation,String generation,int batches) { }
    private final ProjectionGenerationService generations;private final ProjectionBatchConsumer consumer;
    private final WorkbenchSourceRegistry registry;private final ObjectMapper mapper;
    private final ProjectionControlInitializer initializer;
    public ProjectionMaintenanceJob(ProjectionGenerationService generations,ProjectionBatchConsumer consumer,WorkbenchSourceRegistry registry,ObjectMapper mapper,ProjectionControlInitializer initializer){
        this.generations=generations;this.consumer=consumer;this.registry=registry;this.mapper=mapper;this.initializer=initializer;
    }
    @Override @TenantJob public String execute(String param) throws Exception {
        Command command=mapper.readValue(param,Command.class);Long tenant=TenantContextHolder.getTenantId();
        if(tenant==null||command==null||command.generation()==null||!command.generation().matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Explicit tenant/generation required");
        Set<String> sources=registry.enabledSourceIds();
        return switch(command.operation()) {
            case "initialize" -> {initializer.initialize(tenant,registry.revision());yield "Control rows initialized; no generation published";}
            case "rebuild" -> generations.rebuild(command.generation(),tenant,sources).toString();
            case "consume" -> {
                if(command.batches()<1||command.batches()>100) throw new IllegalArgumentException("batches must be 1..100");
                for(int i=0;i<command.batches();i++){consumer.consumeGlobalForTenant(command.generation(),tenant);consumer.consumeTenant(command.generation(),tenant);}
                yield "Requested complete batches processed; equality requires verify";
            }
            case "verify" -> generations.verify(command.generation(),tenant,sources);
            case "publish", "rollback" -> generations.verifyAndPublish(command.generation(),tenant,sources);
            default -> throw new IllegalArgumentException("Unknown explicit projection operation");
        };
    }
}
