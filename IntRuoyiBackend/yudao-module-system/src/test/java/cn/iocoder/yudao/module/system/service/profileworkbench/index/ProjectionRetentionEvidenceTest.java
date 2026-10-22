package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionRetention.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectionRetentionEvidenceTest {
    @TempDir Path folder;
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final Request request=new Request(2,"g",60L,90L,"reason","approval-1");
    ApprovalScope scope(String hash) { return new ApprovalScope("purge",request,1,hash); }
    @Test void missingAndMalformedFileFailWithoutCreatingApproval() throws Exception {
        Path path=folder.resolve("absent.json");
        assertThrows(IllegalStateException.class,()->new FileProjectionRetentionEvidence("",mapper).requireApproved(scope(ProjectionCodec.hashText("p")),false,1));
        assertThrows(IllegalStateException.class,()->new FileProjectionRetentionEvidence(path.toString(),mapper).requireApproved(scope(ProjectionCodec.hashText("p")),false,1));
        assertFalse(Files.exists(path));
        Files.writeString(path,"{} trailing invalid");
        assertThrows(IllegalStateException.class,()->new FileProjectionRetentionEvidence(path.toString(),mapper).requireApproved(scope(ProjectionCodec.hashText("p")),false,1));
    }
    @Test void scopePlanBatchTenantExpiryAuditClockAndIncompleteApprovalAreBound() throws Exception {
        String hash=ProjectionCodec.hashText("p");Path path=folder.resolve("approval.json");
        var approval=new FileProjectionRetentionEvidence.Approval(scope(hash),"APPROVED","operator",100L,true,true,false);
        Files.writeString(path,mapper.writeValueAsString(approval));byte[] original=Files.readAllBytes(path);
        var evidence=new FileProjectionRetentionEvidence(path.toString(),mapper);
        assertDoesNotThrow(()->evidence.requireApproved(scope(hash),false,99));
        assertThrows(IllegalStateException.class,()->evidence.requireApproved(scope(hash),false,100));
        assertThrows(IllegalStateException.class,()->evidence.requireApproved(scope(hash),true,99));
        assertThrows(IllegalStateException.class,()->evidence.requireApproved(scope(ProjectionCodec.hashText("other")),false,99));
        assertThrows(IllegalStateException.class,()->evidence.requireApproved(new ApprovalScope("purge",request,1000,hash),false,99));
        assertThrows(IllegalStateException.class,()->evidence.requireApproved(new ApprovalScope("purge",new Request(3,"g",60L,90L,"reason","approval-1"),1,hash),false,99));
        assertArrayEquals(original,Files.readAllBytes(path));
        for(var rejected:new FileProjectionRetentionEvidence.Approval[]{
            new FileProjectionRetentionEvidence.Approval(scope(hash),"APPROVED","operator",100L,false,true,true),
            new FileProjectionRetentionEvidence.Approval(scope(hash),"APPROVED","operator",100L,true,false,true),
            new FileProjectionRetentionEvidence.Approval(scope(hash),"REJECTED","operator",100L,true,true,true)}) {
            Files.writeString(path,mapper.writeValueAsString(rejected));assertThrows(IllegalStateException.class,()->evidence.requireApproved(scope(hash),false,99));
        }
    }
    @Test void configuredDurationsAreRequiredEvenBeforePreviewAndNeverUseCommandDefaults() throws Exception {
        Path path=folder.resolve("config.json");
        var approval=new FileProjectionRetentionEvidence.Approval(new ApprovalScope("retire",request,null,null),"APPROVED","operator",100L,true,true,true);
        String json=mapper.writeValueAsString(approval);Files.writeString(path,json);
        var evidence=new FileProjectionRetentionEvidence(path.toString(),mapper);
        assertEquals(request,evidence.configuredRequest(2,"g","approval-1"));
        assertThrows(IllegalStateException.class,()->evidence.configuredRequest(3,"g","approval-1"));
        assertThrows(IllegalStateException.class,()->evidence.configuredRequest(2,"g","different"));
        Files.writeString(path,json.replace("\"retentionSeconds\":60","\"retentionSeconds\":null"));
        assertThrows(IllegalStateException.class,()->evidence.configuredRequest(2,"g","approval-1"));
        Files.writeString(path,json.replace("\"rollbackProtectionSeconds\":90","\"rollbackProtectionSeconds\":0"));
        assertThrows(IllegalStateException.class,()->evidence.configuredRequest(2,"g","approval-1"));
    }

}
