package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionHistory.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectionHistoryEvidenceTest {
    @TempDir Path directory;
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final Request request=ProjectionHistoryTest.request(Target.OUTBOX,Scope.GLOBAL,0,null);
    final ApprovalScope scope=new ApprovalScope("purge",request,ProjectionCodec.hashText("plan"));
    FileProjectionHistoryEvidence evidence() throws Exception {
        Path path=directory.resolve("approval.json");mapper.writeValue(path.toFile(),new FileProjectionHistoryEvidence.Approval(scope,"APPROVED","operator",999999L,true,true,true));
        return new FileProjectionHistoryEvidence(path.toString(),mapper);
    }
    @Test void exactScopeReferenceHashExpiryAndPayloadApprovalAreRequired() throws Exception {
        var evidence=evidence();assertEquals(request,evidence.configuredRequest("approval"));assertDoesNotThrow(()->evidence.requireApproved(scope,1));
        assertThrows(IllegalStateException.class,()->evidence.configuredRequest("different"));
        assertThrows(IllegalStateException.class,()->evidence.requireApproved(scope,999999));
        assertThrows(IllegalStateException.class,()->evidence.requireApproved(new ApprovalScope("observe",request,null),1));
        mapper.writeValue(directory.resolve("approval.json").toFile(),new FileProjectionHistoryEvidence.Approval(scope,"APPROVED","operator",999999L,true,true,false));
        assertThrows(IllegalStateException.class,()->evidence.requireApproved(scope,1));
    }
    @Test void absentMalformedUnknownAndUnboundedConfigurationNeverBecomesApproved() throws Exception {
        assertThrows(IllegalStateException.class,()->new FileProjectionHistoryEvidence("",mapper).configuredRequest("approval"));
        var evidence=evidence();Path file=directory.resolve("approval.json");String original=Files.readString(file);
        for(String text:new String[]{original+" {}",original.replace("\"decision\":","\"unknown\":1,\"decision\":"),original.replace("\"tenant\":0,",""),original.replace("\"tenant\":0","\"tenant\":null"),"x".repeat(65537)}) {
            Files.writeString(file,text);assertThrows(IllegalStateException.class,()->evidence.configuredRequest("approval"));
        }
        Files.writeString(file,original.replace("\"retentionSeconds\":60","\"retentionSeconds\":null"));assertThrows(IllegalStateException.class,()->evidence.configuredRequest("approval"));
    }
}
