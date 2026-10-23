package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DccApprovalMatrixCanonicalizerTest {
    Map<String,Object> fixture()throws Exception {
        Path root=Path.of("").toAbsolutePath();while(root!=null && !Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        if(root==null)throw new IllegalStateException("Repository fixture missing");
        return JsonUtils.parseObject(Files.readString(root.resolve("doc/tasks/20261011-dcc-w02-types-matrix/immutable-public-matrix-v2.fixture.json"),StandardCharsets.UTF_8),Map.class);
    }
    @Test void actualJavaCanonicalizerMatchesW02ImmutableV2GoldenAndDoesNotMutateRaw()throws Exception {
        var raw=fixture();String before=JsonUtils.toJsonString(raw);
        assertEquals("0fa78a8a9ea60585fe40d979eb0be32ef6e72b4cdba162c0239756094cc666f5",DccApprovalMatrixCanonicalizer.digest(raw));
        String canonical=DccApprovalMatrixCanonicalizer.canonicalize(raw);
        assertTrue(canonical.contains("11, 22"));assertTrue(canonical.contains("\\r\\n"));
        assertEquals(before,JsonUtils.toJsonString(raw));
    }
    @Test void activeFieldsAndApplicationListsAreRejectedRatherThanAffectingImmutableDigest()throws Exception {
        var raw=fixture();raw.put("active",true);assertThrows(IllegalArgumentException.class,()->DccApprovalMatrixCanonicalizer.digest(raw));
        raw.remove("active");raw.put("selectedSignoffDepartmentIds",List.of("52"));
        assertThrows(IllegalArgumentException.class,()->DccApprovalMatrixCanonicalizer.digest(raw));
    }
    @Test void markerCsvNullAndWhitespaceAreRealImmutableDifferences()throws Exception {
        var raw=fixture();var nodes=(List<Map<String,Object>>)raw.get("nodes");nodes.get(0).put("candidateSourceIds","11,22");
        assertNotEquals("0fa78a8a9ea60585fe40d979eb0be32ef6e72b4cdba162c0239756094cc666f5",DccApprovalMatrixCanonicalizer.digest(raw));
        nodes.get(0).put("marker","◎");String digest=DccApprovalMatrixCanonicalizer.digest(raw);
        nodes.get(0).put("ruleRemark",null);assertNotEquals(digest,DccApprovalMatrixCanonicalizer.digest(raw));
    }
    @Test void unsafeIdentitiesMissingFieldsAndSurrogatesAreRejected()throws Exception {
        var raw=fixture();raw.put("routeId",9007199254740993d);final var unsafe=raw;assertThrows(IllegalArgumentException.class,()->DccApprovalMatrixCanonicalizer.digest(unsafe));
        raw=fixture();raw.remove("routeRemark");final var missing=raw;assertThrows(IllegalArgumentException.class,()->DccApprovalMatrixCanonicalizer.digest(missing));
        raw=fixture();raw.put("routeRemark","\ud800");final var broken=raw;assertThrows(IllegalArgumentException.class,()->DccApprovalMatrixCanonicalizer.digest(broken));
    }
}
