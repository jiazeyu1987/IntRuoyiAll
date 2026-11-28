package cn.iocoder.yudao.module.mes.service.pro.route;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MesProRouteSnapshotSopContractTest {
    private final MesProRouteSnapshotCanonicalizer validator = new MesProRouteSnapshotCanonicalizer();

    @Test
    void frozenAndCandidateNodesRejectAbsentOrInvalidSopButAcceptExplicitNullAndUrl() {
        for (String identity : new String[]{"\"routeProcessId\":2", "\"clientRouteProcessId\":-2"}) {
            for (String binding : new String[]{"", ",\"sopFileUrl\":123", ",\"sopFileUrl\":null", ",\"sopFileUrl\":\"/frozen.pdf\""}) {
                String json = "{\"routeId\":1,\"configSnapshots\":{\"flowGraph\":{\"nodes\":[{" + identity
                        + ",\"processId\":3" + binding + "}]}}}";
                boolean invalid = binding.isEmpty() || binding.contains("123");
                assertEquals(invalid, hasSopBlocker(validator.validateCandidate(1L, json)), json);
                assertEquals(!invalid, validator.validateCandidate(1L, json).ready(), json);
                assertEquals(!invalid, validator.validateExistingSopBindings(json).ready(), json);
                if (identity.contains("routeProcessId")) {
                    assertEquals(invalid, hasSopBlocker(validator.validate(1L, json)), json);
                    assertEquals(!invalid, validator.validate(1L, json).ready(), json);
                }
            }
        }
    }

    private boolean hasSopBlocker(MesProRouteSnapshotCanonicalizer.Validation result) {
        return result.blockers().stream().anyMatch(blocker -> blocker.reasonCode().startsWith("SOP_BINDING_"));
    }
}
