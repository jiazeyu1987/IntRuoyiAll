package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.Scope;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionHistory.*;

/** Explicit GLOBAL or TENANT maintenance; intentionally not a TenantJob fan-out. */
@Component("workbenchProjectionHistoryJob")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionHistoryJob implements JobHandler {
    public record Command(@JsonProperty("op") String operation,Scope scope,@JsonProperty("tenant") Long tenant,
                          @JsonProperty("ref") String reference,@JsonProperty("plan") String planHash) { }
    public record Result(String operation,Target target,Scope scope,long tenant,String generation,int rows,
                         long sequence,String planHash,Long purgeNotBeforeMillis) { }
    private final ProjectionHistoryService service;
    private final Evidence evidence;
    private final ObjectMapper mapper;
    public ProjectionHistoryJob(ProjectionHistoryService service,Evidence evidence,ObjectMapper mapper) {this.service=service;this.evidence=evidence;this.mapper=mapper;}
    @Override public String execute(String param) throws Exception {
        if(param==null||param.length()>255)throw new IllegalArgumentException("Explicit history command must fit 255 characters");
        Command c=mapper.readerFor(Command.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(param);
        if(c==null||c.operation()==null||c.scope()==null||c.tenant()==null||c.reference()==null||!c.reference().matches("[A-Za-z0-9_-]{1,32}"))throw new IllegalArgumentException("Explicit operation, scope, tenant and reference required");
        Request r=evidence.configuredRequest(c.reference());
        if(r==null||r.scope()!=c.scope()||r.tenant()!=c.tenant()||!r.approvalReference().equals(c.reference()))throw new IllegalStateException("Configured history scope does not match command");
        if(!"purge".equals(c.operation())&&c.planHash()!=null)throw new IllegalArgumentException("Only purge accepts a plan hash");
        Long originalTenant=TenantContextHolder.getTenantId();boolean originalIgnore=TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(r.tenant());TenantContextHolder.setIgnore(r.scope()==Scope.GLOBAL);
            Plan plan=switch(c.operation()) {
                case "init" -> {service.initialize(r);yield null;}
                case "register" -> service.register(r);
                case "observe" -> service.observe(r);
                case "preview" -> service.preview(r);
                case "purge" -> service.purge(r,c.planHash());
                default -> throw new IllegalArgumentException("Unknown explicit history operation");
            };
            Snapshot s=plan==null?null:plan.candidate().snapshot();
            int rows=s==null?0:s.items().stream().mapToInt(i->1+i.members().size()).sum();
            Result result=new Result(c.operation(),r.target(),r.scope(),r.tenant(),r.generation(),rows,s==null?0:s.sequence(),plan==null?null:plan.planHash(),s==null||s.observation()==null?null:s.observation().purgeNotBeforeMillis());
            return mapper.writeValueAsString(result);
        } finally {TenantContextHolder.setTenantId(originalTenant);TenantContextHolder.setIgnore(originalIgnore);}
    }
}
