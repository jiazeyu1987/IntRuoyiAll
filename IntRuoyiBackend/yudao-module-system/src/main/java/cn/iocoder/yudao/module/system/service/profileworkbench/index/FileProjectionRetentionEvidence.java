package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.*;

/** Read-only operator approval for this exact irreversible operation; no command-supplied file paths. */
@Component
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class FileProjectionRetentionEvidence implements Evidence {
    public record Approval(ApprovalScope scope,String decision,String approvedBy,Long expiresAtMillis,
                           boolean retentionAndAuditApproved,boolean trustedClockPassed,boolean abandonIncompleteApproved) { }
    private final String path;
    private final ObjectMapper mapper;
    public FileProjectionRetentionEvidence(@Value("${profile.workbench.retention-evidence:}") String path,ObjectMapper mapper) {
        this.path=path;this.mapper=mapper;
    }
    private Approval read() {
        if(path==null||path.isBlank())throw new IllegalStateException("Explicit retention configuration/approval file required");
        try {
            Path file=Path.of(path);
            if(!Files.isRegularFile(file)||Files.size(file)>65536)throw new IllegalStateException("Bounded regular approval file required");
            Approval approval=mapper.readerFor(Approval.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readValue(Files.readAllBytes(file));
            if(approval==null||approval.scope()==null||approval.scope().request()==null)
                throw new IllegalStateException("Explicit required retention configuration missing");
            return approval;
        } catch(Exception failure) { throw new IllegalStateException("Retention configuration/approval cannot be read",failure); }
    }
    @Override public Request configuredRequest(long tenant,String generation,String reference) {
        scope(tenant,generation);Request request=read().scope().request();
        if(request.tenant()!=tenant||!request.generation().equals(generation)||!request.approvalReference().equals(reference))
            throw new IllegalStateException("Configured retention scope mismatch");
        return request;
    }
    @Override public void requireApproved(ApprovalScope scope,boolean abandoningIncomplete,long nowMillis) {
        Approval approval=read();
        if(!scope.equals(approval.scope())||!"APPROVED".equals(approval.decision())
            ||approval.approvedBy()==null||approval.approvedBy().isBlank()||approval.expiresAtMillis()==null
            ||nowMillis<0||nowMillis>=approval.expiresAtMillis()||!approval.retentionAndAuditApproved()||!approval.trustedClockPassed()
            ||(abandoningIncomplete&&!approval.abandonIncompleteApproved()))
            throw new IllegalStateException("Matching unexpired retention/audit/clock approval required");
    }
}
