package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exact current public BPM/round/frozen HMAC provenance; not a mock READY result. */
class DccPublicRoundNativeSourceGuardTest extends DccRoundFlowContractTest {
    private Map<String,Object> signedPublicSource(String bpm)throws Exception{
        publicProvenance();var file=files.selectById(20L);var rows=routeSnapshots.selectListByControlledFileId(20L);
        var baseline=DccApprovalMatrixSourceService.readCanonical(rows.get(0).getSourceApprovalMatrixJson());
        var properties=new DccApprovalMatrixTokenProperties();properties.setSigningSecret("rf11-actual-native-source-key-at-least-32-bytes");var tokens=new DccApprovalMatrixTokenService();wire(tokens,"properties",properties);wire(resolver,"nativeSourceTokens",tokens);
        var proof=tokens.issueNativeSupervisor(new DccApprovalMatrixTokenService.Context(1L,99L,5L,2L,6L,"NEW",20L,"rf11-current-round"),baseline.getMatrixDigest(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest(),List.of(51L),new DccDirectSupervisorSource.Evidence(1L,99L,99L,"FORMAL_ORG","relation99","v1"));
        var frozen=new TreeMap<String,Object>();frozen.put("payload",proof.payload());frozen.put("sourceDigest",proof.sourceDigest());frozen.put("sourceToken",proof.sourceToken());
        var runtime=JsonUtils.parseObject(rows.get(0).getRuntimeApprovalNodesJson(),Map.class);runtime.put("nativeSupervisorSource",frozen);
        jdbc.update("UPDATE dcc_controlled_file_route_snapshot SET runtime_approval_nodes_json=? WHERE controlled_file_id=20",JsonUtils.toJsonString(runtime));return frozen;
    }
    @Test void currentPublicRoundWithFrozenNativeProofButMissingActualBpmVariableRejects()throws Exception{
        String bpm=publicActive(false);signedPublicSource(bpm);
        assertThrows(IllegalStateException.class,()->resolve(summary("UPLOAD",bpm,1)));
    }
    @Test void matchingActualNativeVariableIsReadyButMismatchCannotBorrowFrozenSource()throws Exception{
        String bpm=publicActive(false);var frozen=signedPublicSource(bpm);engine.getRuntimeService().setVariable(bpm,"DCC_NATIVE_SUPERVISOR_SOURCE",frozen);
        assertEquals("READY",field(resolve(summary("UPLOAD",bpm,1)),"status").toString());
        var changed=new TreeMap<>(frozen);changed.put("sourceDigest","f".repeat(64));engine.getRuntimeService().setVariable(bpm,"DCC_NATIVE_SUPERVISOR_SOURCE",changed);
        assertThrows(IllegalStateException.class,()->resolve(summary("UPLOAD",bpm,1)));
    }
}
