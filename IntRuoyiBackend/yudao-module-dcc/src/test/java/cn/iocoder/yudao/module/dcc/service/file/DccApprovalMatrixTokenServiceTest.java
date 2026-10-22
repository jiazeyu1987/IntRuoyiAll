package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DccApprovalMatrixTokenServiceTest {
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
