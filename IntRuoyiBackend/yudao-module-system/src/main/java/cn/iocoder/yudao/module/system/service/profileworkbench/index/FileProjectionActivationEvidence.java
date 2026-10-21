package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.time.Instant;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

/** Explicit operator evidence, bound to the exact verified candidate. Absence never authorizes publication. */
@Component @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class FileProjectionActivationEvidence implements ProjectionGenerationService.ActivationEvidence {
    public record Approval(long tenant,String generation,Watermark watermark,String collectionHash,
        String decision,boolean capacityPassed,boolean writerClosurePassed,boolean safetyPassed,boolean trustedClockPassed,
        boolean realRuntimePassed,String approvedBy,Instant expiresAt) { }
    private final String path; private final ObjectMapper mapper;
    public FileProjectionActivationEvidence(@Value("${profile.workbench.activation-evidence:}") String path,ObjectMapper mapper){this.path=path;this.mapper=mapper;}
    @Override public void requireApproved(long tenant,String generation,Watermark watermark,String hash) {
        if(path.isBlank()) throw PROJECTION_ACTIVATION_REQUIRED.exception();
        final Approval approval;
        try {approval=mapper.readValue(Files.readAllBytes(Path.of(path)),Approval.class);}
        catch(Exception failure){throw PROJECTION_ACTIVATION_REQUIRED.exception(failure);}
        if(approval==null||approval.tenant()!=tenant||!generation.equals(approval.generation())||!watermark.equals(approval.watermark())
            ||!hash.equals(approval.collectionHash())||!"APPROVED".equals(approval.decision())||!approval.capacityPassed()
            ||!approval.writerClosurePassed()||!approval.safetyPassed()||!approval.trustedClockPassed()||!approval.realRuntimePassed()
            ||approval.approvedBy()==null||approval.approvedBy().isBlank()||approval.expiresAt()==null||!Instant.now().isBefore(approval.expiresAt()))
            throw PROJECTION_ACTIVATION_REQUIRED.exception();
    }
}
