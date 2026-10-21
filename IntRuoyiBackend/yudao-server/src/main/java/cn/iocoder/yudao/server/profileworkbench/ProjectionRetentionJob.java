package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.*;

/** Single tenant; short explicit command fits infra_job/infra_job_log varchar(255). */
@Component("workbenchProjectionRetentionJob")
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionRetentionJob implements JobHandler {
    public record Command(@JsonProperty("op") String operation,@JsonProperty("tenant") Long tenantId,String generation,
                          @JsonProperty("ref") String approvalReference,@JsonProperty("batch") Integer batchSize,
                          @JsonProperty("plan") String planHash) { }
    public record Result(String operation,long tenant,String generation,State state,Stage stage,int rows,
                         Integer batchSize,String planHash,long purgeNotBeforeMillis,boolean hasMore) { }
    private final ProjectionRetentionService service;
    private final Evidence evidence;
    private final ObjectMapper mapper;
    public ProjectionRetentionJob(ProjectionRetentionService service,Evidence evidence,ObjectMapper mapper) {
        this.service=service;this.evidence=evidence;this.mapper=mapper;
    }
    @Override public String execute(String param) throws Exception {
        if(param==null||param.length()>255)throw new IllegalArgumentException("Explicit command must fit the 255 character job contract");
        Command command=mapper.readerFor(Command.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(param);
        if(command==null||command.tenantId()==null||command.operation()==null)throw new IllegalArgumentException("Explicit operation and tenant required");
        scope(command.tenantId(),command.generation());
        if(command.approvalReference()==null||!command.approvalReference().matches("[A-Za-z0-9_-]{1,32}"))
            throw new IllegalArgumentException("Explicit bounded approval reference required");
        Request request=evidence.configuredRequest(command.tenantId(),command.generation(),command.approvalReference());
        if(request==null||request.tenant()!=command.tenantId()||!request.generation().equals(command.generation())||!request.approvalReference().equals(command.approvalReference()))
            throw new IllegalStateException("Configured required retention parameters do not match the command");
        Long originalTenant=TenantContextHolder.getTenantId();boolean originalIgnore=TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(request.tenant());TenantContextHolder.setIgnore(false);
            Result result=switch(command.operation()) {
                case "retire" -> {
                    if(command.batchSize()!=null||command.planHash()!=null)throw new IllegalArgumentException("Retire does not accept purge parameters");
                    Retirement r=service.retire(request);
                    yield new Result("retire",request.tenant(),request.generation(),r.state(),null,0,null,null,r.purgeNotBeforeMillis(),r.state()!=State.PURGED);
                }
                case "purge-preview" -> {
                    if(command.planHash()!=null)throw new IllegalArgumentException("Preview does not accept a prior planHash");
                    yield summary("purge-preview",service.preview(request,command.batchSize()));
                }
                case "purge" -> summary("purge",service.purge(request,command.batchSize(),command.planHash()));
                default -> throw new IllegalArgumentException("Unknown explicit retention operation");
            };
            return mapper.writeValueAsString(result);
        } finally {
            TenantContextHolder.setTenantId(originalTenant);TenantContextHolder.setIgnore(originalIgnore);
        }
    }
    private static Result summary(String operation,Plan plan) {
        Candidate c=plan.candidate();Request r=c.retirement().request();
        boolean finished="purge".equals(operation)&&c.selection().stage()==Stage.METADATA;
        return new Result(operation,r.tenant(),r.generation(),finished?State.PURGED:c.retirement().state(),
            c.selection().stage(),c.selection().rows().size(),c.batchSize(),plan.planHash(),c.retirement().purgeNotBeforeMillis(),plan.hasMore());
    }
}
