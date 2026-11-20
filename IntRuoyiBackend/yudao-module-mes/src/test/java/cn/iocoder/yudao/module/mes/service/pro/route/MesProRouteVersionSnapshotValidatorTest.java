package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesProRouteVersionSnapshotValidatorTest {
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
