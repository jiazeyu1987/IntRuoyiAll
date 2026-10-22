package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionHistory.*;

/** Operator-owned read-only configuration; the job cannot supply a filesystem path. */
@Component
@ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class FileProjectionHistoryEvidence implements Evidence {
    public record Approval(ApprovalScope scope,String decision,String approvedBy,Long expiresAtMillis,
                           boolean retentionAndAuditApproved,boolean trustedClockPassed,boolean payloadDeletionApproved) { }
    private final String path;
    private final ObjectMapper mapper;
    public FileProjectionHistoryEvidence(@Value("${profile.workbench.history-evidence:}") String path,ObjectMapper mapper) {
        this.path=path;this.mapper=mapper;
    }
    private Approval read() {
        if(path==null||path.isBlank())throw new IllegalStateException("Explicit history configuration/approval file required");
        try {
            Path file=Path.of(path);
            if(!Files.isRegularFile(file)||Files.size(file)>65536)throw new IllegalStateException("Bounded regular history approval file required");
            Approval approval=mapper.readerFor(Approval.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).readValue(Files.readAllBytes(file));
            if(approval==null||approval.scope()==null||approval.scope().request()==null)throw new IllegalStateException("Required history configuration missing");
            return approval;
        } catch(Exception failure) {throw new IllegalStateException("History approval cannot be read",failure);}
    }
    @Override public Request configuredRequest(String reference) {
        Request request=read().scope().request();
        if(!request.approvalReference().equals(reference))throw new IllegalStateException("History approval reference mismatch");
        return request;
    }
    @Override public void requireApproved(ApprovalScope scope,long now) {
        Approval approval=read();
        if(!scope.equals(approval.scope())||!"APPROVED".equals(approval.decision())||approval.approvedBy()==null||approval.approvedBy().isBlank()
            ||approval.expiresAtMillis()==null||now<0||now>=approval.expiresAtMillis()||!approval.retentionAndAuditApproved()||!approval.trustedClockPassed()
            ||("purge".equals(scope.operation())&&!approval.payloadDeletionApproved()))throw new IllegalStateException("Matching unexpired history/audit/clock/payload approval required");
    }
}
