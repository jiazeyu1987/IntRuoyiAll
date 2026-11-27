package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MesProRouteVersionSnapshotValidatorTest {
    @Test
    void sopBindingMustBeExplicitAndCannotBeCoercedToAString() {
        assertFalse(complete(""));
        for (String invalid : new String[]{"123", "true", "[]", "{}"}) {
            assertFalse(complete(",\"sopFileUrl\":" + invalid), invalid);
        }
        for (String valid : new String[]{"null", "\"\"", "\"/sop.pdf\""}) {
            assertTrue(complete(",\"sopFileUrl\":" + valid), valid);
        }
    }

    @Test
    void everyProcessNeedsItsOwnSopBindingField() {
        String snapshot = sopSnapshot(",\"sopFileUrl\":null");
        snapshot = snapshot.replace("\"nodes\":[", "\"nodes\":[{\"routeProcessId\":2,\"processId\":2},");
        snapshot = snapshot.replace("\"productionProcessConfigs\":[", """
                "productionProcessConfigs":[{"routeProcessId":2,"processId":2,
                  "overagePercent":0,"lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]},
                """);
        assertFalse(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(snapshot));
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(snapshot.replace(
                "\"routeProcessId\":2,\"processId\":2}", "\"routeProcessId\":2,\"processId\":2,\"sopFileUrl\":null}")));
    }

    @Test
    void diagnosticsLocateMissingKeysTypesAndSopNodeWithoutWeakeningValidation() {
        String valid = sopSnapshot(",\"sopFileUrl\":null");
        org.junit.jupiter.api.Assertions.assertNull(MesProRouteVersionSnapshotValidator.configSnapshotProblem(valid));
        for (String key : new String[]{"products", "scheduleConfigs", "batchUseConfigs", "scheduleUseConfigs"}) {
            String missing = valid.replace("\"" + key + "\":[],", "");
            assertFalse(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(missing));
            assertTrue(MesProRouteVersionSnapshotValidator.configSnapshotProblem(missing).contains(key));
            String wrong = valid.replace("\"" + key + "\":[]", "\"" + key + "\":true");
            assertFalse(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(wrong));
            assertTrue(MesProRouteVersionSnapshotValidator.configSnapshotProblem(wrong).contains(key));
        }
        assertTrue(MesProRouteVersionSnapshotValidator.configSnapshotProblem(sopSnapshot(""))
                .contains("nodes[0]"));
    }

    private boolean complete(String field) {
        return MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(sopSnapshot(field));
    }

    @Test
    void newCandidateProcessConfigurationMustMatchItsExplicitNegativeClientId() {
        String snapshot = sopSnapshot(",\"sopFileUrl\":null");
        String candidate = snapshot.replace("\"routeProcessId\":1,\"processId\":1,\"sopFileUrl\"",
                "\"clientRouteProcessId\":-1,\"processId\":1,\"sopFileUrl\"")
                .replace("\"routeProcessId\":1,\"processId\":1,\n", "\"routeProcessId\":-1,\"processId\":1,\n");
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(candidate));
        assertFalse(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(
                candidate.replace("\"routeProcessId\":-1", "\"routeProcessId\":-2")));
        assertFalse(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(
                candidate.replace("\"clientRouteProcessId\":-1", "\"clientRouteProcessId\":1")));
        assertFalse(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(
                candidate.replace("\"clientRouteProcessId\":-1,", "")));
        assertFalse(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(
                candidate.replace("\"nodes\":[", "\"nodes\":[{\"clientRouteProcessId\":-1,\"processId\":1,\"sopFileUrl\":null},")));
    }

    private String sopSnapshot(String field) {
        return """
                {"routeId":1,"routeCode":"R1","routeName":"Route",
                 "configSnapshots":{"flowGraph":{"nodes":[{"routeProcessId":1,"processId":1%s}]},
                 "products":[],"scheduleConfigs":[],"batchUseConfigs":[],"scheduleUseConfigs":[],
                 "productionProcessConfigSchemaVersion":1,
                 "productionProcessConfigs":[{"routeProcessId":1,"processId":1,
                   "overagePercent":0,"lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]}]}}
                """.formatted(field);
    }
    @Test
    void publishingRequiresAnExplicitNonemptyLeaderSource() {
        assertThrows(ServiceException.class, () -> validate("null"));
        assertThrows(ServiceException.class, () -> validate("[]"));
        assertThrows(ServiceException.class, () -> validate("[{}]"));
        assertThrows(ServiceException.class, () -> validate(assignment(2, "USER", "[3]")));
        assertThrows(ServiceException.class, () -> validate(assignment(1, "USER", "[]")));
        assertThrows(ServiceException.class, () -> validate(assignment(1, "USER", "[0]")));
        assertThrows(ServiceException.class, () -> validate(assignment(1, "USER", "[\"bad\"]")));
        assertThrows(ServiceException.class, () -> validate(assignment(1, "UNKNOWN", "[3]")));
    }

    @Test
    void publishingAcceptsExplicitUserAndRoleSources() {
        for (String type : new String[]{"USER", "USERS", "ROLE"}) {
            assertDoesNotThrow(() -> validate(assignment(1, type, "[3,4]")));
        }
    }

    private void validate(String assignments) {
        MesProRouteVersionSnapshotValidator.requireProductionLeaderAssignments(
                "{\"routeId\":1,\"configSnapshots\":{\"routeStartProductionLeaders\":" + assignments + "}}");
    }

    private String assignment(long routeId, String type, String ids) {
        return "[{\"productionLineId\":" + routeId + ",\"candidateSourceType\":\"" + type
                + "\",\"candidateSourceIds\":" + ids + "}]";
    }
}
