package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DccApprovalMatrixTokenServiceTest {
    @Test void retiredKeyVerifiesOnlyRecordedEvidenceAndRejectsTampering() {
        var old=service();
        var proof=old.issueNativeSupervisor(context(),"a".repeat(64),"definition","b".repeat(64),List.of(11L),
                new DccDirectSupervisorSource.Evidence(1L,7L,12L,"FORMAL_ORG","relation","v1"));
        var properties=new DccApprovalMatrixTokenProperties();
        properties.setSigningSecret("new-current-test-key-at-least-32-bytes");
        var rotated=new DccApprovalMatrixTokenService();ReflectionTestUtils.setField(rotated,"properties",properties);
        assertThrows(IllegalArgumentException.class,()->rotated.verifyRecordedNativeSupervisor(proof.sourceDigest(),proof.sourceToken()));
        properties.setRecordedVerificationSecrets(List.of("w05-private-test-explicit-key-32-bytes-long"));
        assertEquals(proof.payload(),rotated.verifyRecordedNativeSupervisor(proof.sourceDigest(),proof.sourceToken()));
        assertThrows(IllegalArgumentException.class,()->rotated.requireNativeSupervisor(proof,proof.sourceDigest(),proof.sourceToken()));
        assertThrows(IllegalArgumentException.class,()->rotated.verifyRecordedNativeSupervisor("c".repeat(64),proof.sourceToken()));
        String tampered=proof.sourceToken().substring(0,proof.sourceToken().indexOf('.')+1)+"AAAA";
        assertThrows(IllegalArgumentException.class,()->rotated.verifyRecordedNativeSupervisor(proof.sourceDigest(),tampered));
        var current=rotated.issueNativeSupervisor(context(),"a".repeat(64),"definition","b".repeat(64),List.of(11L),
                new DccDirectSupervisorSource.Evidence(1L,7L,12L,"FORMAL_ORG","relation","v1"));
        rotated.requireNativeSupervisor(current,current.sourceDigest(),current.sourceToken());
    }
    DccApprovalMatrixTokenService service(){var properties=new DccApprovalMatrixTokenProperties();properties.setSigningSecret("w05-private-test-explicit-key-32-bytes-long");var service=new DccApprovalMatrixTokenService();ReflectionTestUtils.setField(service,"properties",properties);return service;}
    DccApprovalMatrixTokenService.Context context(){return new DccApprovalMatrixTokenService.Context(1L,7L,8L,9L,10L,"NEW",null,"upload-operation-1");}
    String baseline()throws Exception{Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();return DccApprovalMatrixCanonicalizer.canonicalize(JsonUtils.parseObject(Files.readString(root.resolve("doc/tasks/20261011-dcc-w02-types-matrix/immutable-public-matrix-v2.fixture.json")),Map.class));}
    @Test void baselineAndConfirmationAreRealSignedDifferentPurposesAndBindExactContext()throws Exception{
        var service=service();var context=context();String baseline=baseline();String token=service.issueBaseline(context,baseline);
        assertEquals(baseline,service.verifyBaseline(context,token));
        String confirmed=service.issueConfirmation(context,baseline,11L,3,"b".repeat(64));
        service.requireConfirmation(context,confirmed,baseline,11L,3,"b".repeat(64));
        assertThrows(IllegalArgumentException.class,()->service.verifyBaseline(context,confirmed));
        assertThrows(IllegalArgumentException.class,()->service.requireConfirmation(context,confirmed,baseline,12L,4,"c".repeat(64)));
    }
    @Test void tamperWrongActorTenantTypeAndAbsentKeyFailBeforeTrustingBaseline()throws Exception{
        var service=service();String token=service.issueBaseline(context(),baseline());
        assertThrows(IllegalArgumentException.class,()->service.verifyBaseline(context(),token.substring(0,token.length()-1)+"x"));
        assertThrows(IllegalArgumentException.class,()->service.verifyBaseline(new DccApprovalMatrixTokenService.Context(2L,7L,8L,9L,10L,"NEW",null,"upload-operation-1"),token));
        assertThrows(IllegalArgumentException.class,()->service.verifyBaseline(new DccApprovalMatrixTokenService.Context(1L,8L,8L,9L,10L,"NEW",null,"upload-operation-1"),token));
        assertThrows(IllegalArgumentException.class,()->service.verifyBaseline(new DccApprovalMatrixTokenService.Context(1L,7L,8L,9L,10L,"REVISION",null,"upload-operation-1"),token));
        var missing=new DccApprovalMatrixTokenService();ReflectionTestUtils.setField(missing,"properties",new DccApprovalMatrixTokenProperties());
        assertThrows(IllegalStateException.class,()->missing.issueBaseline(context(),baseline()));
    }
}
