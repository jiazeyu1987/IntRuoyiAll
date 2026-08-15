package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction;
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
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.APPROVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.CANCEL;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.FINALIZE_FAILED;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.FINALIZE_SUCCESS;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.OBSOLETE_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REJECT;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REGISTER_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REQUEST_REWORK;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.RETRY_FINALIZATION;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.START_FINALIZATION;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.SUBMIT;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.SUPERSEDE_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.WITHDRAW;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlledContentStateMachineTest {

    private final ControlledContentStateMachine stateMachine = new ControlledContentStateMachine();

    @Test
    void shouldAllowDocumentedLifecycleTransitions() {
        assertAllowed(null, ACTIVE, REGISTER_ACTIVE);
        assertAllowed(DRAFT, IN_REVIEW, SUBMIT);
        assertAllowed(REWORK, IN_REVIEW, SUBMIT);
        assertAllowed(IN_REVIEW, READY_TO_PUBLISH, APPROVE);
        assertAllowed(IN_REVIEW, REWORK, REQUEST_REWORK);
        assertAllowed(IN_REVIEW, DRAFT, WITHDRAW);
        assertAllowed(IN_REVIEW, WITHDRAWN, WITHDRAW);
        assertAllowed(IN_REVIEW, REJECTED, REJECT);
        assertAllowed(DRAFT, CANCELLED, CANCEL);
        assertAllowed(REWORK, CANCELLED, CANCEL);
        assertAllowed(READY_TO_PUBLISH, CANCELLED, CANCEL);
        assertAllowed(REJECTED, CANCELLED, CANCEL);
        assertAllowed(READY_TO_PUBLISH, FINALIZING, START_FINALIZATION);
        assertAllowed(READY_TO_PUBLISH, IN_REVIEW, SUBMIT);
        assertAllowed(READY_TO_PUBLISH, ACTIVE, PUBLISH);
        assertAllowed(FINALIZING, ACTIVE, FINALIZE_SUCCESS);
        assertAllowed(FINALIZING, OBSOLETE, FINALIZE_SUCCESS);
        assertAllowed(FINALIZING, FINALIZATION_FAILED, FINALIZE_FAILED);
        assertAllowed(FINALIZATION_FAILED, FINALIZING, RETRY_FINALIZATION);
        assertAllowed(ACTIVE, SUPERSEDED, SUPERSEDE_ACTIVE);
        assertAllowed(ACTIVE, OBSOLETE, OBSOLETE_ACTIVE);
    }

    @Test
    void registerReadyCandidate_shouldAllowOnlyInitialReadyProjection() {
        ControlledContentTransitionAction action = assertDoesNotThrow(
                () -> ControlledContentTransitionAction.valueOf("REGISTER_READY_CANDIDATE"));

        assertAllowed(null, READY_TO_PUBLISH, action);
        for (ControlledContentCanonicalStatus from : ControlledContentCanonicalStatus.values()) {
            for (ControlledContentCanonicalStatus to : ControlledContentCanonicalStatus.values()) {
                if (to != READY_TO_PUBLISH) {
                    assertRejected(from, to, action);
                }
            }
        }
        assertRejected(ACTIVE, READY_TO_PUBLISH, action);
    }

    @Test
    void shouldRejectIllegalLifecycleTransitions() {
        assertRejected(DRAFT, ACTIVE, REGISTER_ACTIVE);
        assertRejected(ACTIVE, DRAFT, WITHDRAW);
        assertRejected(IN_REVIEW, DRAFT, PUBLISH);
        assertRejected(SUPERSEDED, ACTIVE, PUBLISH);
        assertRejected(OBSOLETE, ACTIVE, PUBLISH);
        assertRejected(WITHDRAWN, DRAFT, SUBMIT);
        assertRejected(CANCELLED, IN_REVIEW, SUBMIT);
    }

    @Test
    void shouldExposeEditableStatesOnlyForDraftAndRework() {
        assertTrue(stateMachine.isEditable(DRAFT));
        assertTrue(stateMachine.isEditable(REWORK));

        for (ControlledContentCanonicalStatus status : ControlledContentCanonicalStatus.values()) {
            if (status != DRAFT && status != REWORK) {
                assertFalse(stateMachine.isEditable(status), status + " must be read-only");
                assertThrows(IllegalStateException.class, () -> stateMachine.validateEditable(status));
            }
        }
    }

    private void assertAllowed(ControlledContentCanonicalStatus from, ControlledContentCanonicalStatus to,
                               ControlledContentTransitionAction action) {
        assertTrue(stateMachine.canTransition(from, to, action), from + " -> " + to + " by " + action);
        assertDoesNotThrow(() -> stateMachine.validateTransition(from, to, action));
    }

    private void assertRejected(ControlledContentCanonicalStatus from, ControlledContentCanonicalStatus to,
                                ControlledContentTransitionAction action) {
        assertFalse(stateMachine.canTransition(from, to, action), from + " -> " + to + " by " + action);
        assertThrows(IllegalStateException.class, () -> stateMachine.validateTransition(from, to, action));
    }

}
