package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.CANCELLED;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.DRAFT;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.FINALIZATION_FAILED;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.FINALIZING;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.IN_REVIEW;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.OBSOLETE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.READY_TO_PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.REJECTED;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.REWORK;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.SUPERSEDED;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.WITHDRAWN;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_CONTROLLED_FILE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.MES_ROUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlledContentOpenCandidateConstraintTest {

    private final ControlledContentStateMachine stateMachine = new ControlledContentStateMachine();

    @Test
    void shouldTreatOnlyUnfinishedCandidateStatusesAsOpenCandidate() {
        assertOpen(DRAFT, REWORK, IN_REVIEW, READY_TO_PUBLISH, FINALIZING, FINALIZATION_FAILED);
        assertClosed(ACTIVE, SUPERSEDED, OBSOLETE, REJECTED, WITHDRAWN, CANCELLED);
    }

    @Test
    void shouldTreatOnlyActiveAsSingleActiveStatus() {
        assertTrue(stateMachine.isActive(ACTIVE));
        for (ControlledContentCanonicalStatus status : ControlledContentCanonicalStatus.values()) {
            if (status != ACTIVE) {
                assertFalse(stateMachine.isActive(status), status + " must not occupy active slot");
            }
        }
    }

    @Test
    void shouldBuildStableContentKeyPerTenantTypeAndNativeKey() {
        ControlledContentKey routeKey = ControlledContentKey.of(122L, MES_ROUTE, "route-1001");
        ControlledContentKey sameRouteKey = ControlledContentKey.of(122L, MES_ROUTE, "route-1001");
        ControlledContentKey otherTenantKey = ControlledContentKey.of(123L, MES_ROUTE, "route-1001");
        ControlledContentKey dccKey = ControlledContentKey.of(122L, DCC_CONTROLLED_FILE, "master-1001");

        assertEquals(routeKey, sameRouteKey);
        assertEquals(routeKey.hashCode(), sameRouteKey.hashCode());
        assertNotEquals(routeKey, otherTenantKey);
        assertNotEquals(routeKey, dccKey);
        assertEquals("122:MES_ROUTE:route-1001", routeKey.toUniqueKey());
    }

    @Test
    void shouldRejectInvalidContentKeyInputs() {
        assertThrows(IllegalArgumentException.class, () -> ControlledContentKey.of(null, MES_ROUTE, "route-1001"));
        assertThrows(IllegalArgumentException.class, () -> ControlledContentKey.of(122L, null, "route-1001"));
        assertThrows(IllegalArgumentException.class, () -> ControlledContentKey.of(122L, MES_ROUTE, " "));
    }

    private void assertOpen(ControlledContentCanonicalStatus... statuses) {
        for (ControlledContentCanonicalStatus status : statuses) {
            assertTrue(stateMachine.isOpenCandidate(status), status + " must occupy open candidate slot");
        }
    }

    private void assertClosed(ControlledContentCanonicalStatus... statuses) {
        for (ControlledContentCanonicalStatus status : statuses) {
            assertFalse(stateMachine.isOpenCandidate(status), status + " must release open candidate slot");
        }
    }

}
