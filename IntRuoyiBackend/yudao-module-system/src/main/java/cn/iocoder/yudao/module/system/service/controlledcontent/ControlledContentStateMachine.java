package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

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
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REGISTER_READY_CANDIDATE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REQUEST_REWORK;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.RETRY_FINALIZATION;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.START_FINALIZATION;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.SUBMIT;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.SUPERSEDE_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.WITHDRAW;

/**
 * Pure platform lifecycle policy shared by MES and DCC adapters.
 */
@Component
public class ControlledContentStateMachine {

    private static final Set<ControlledContentCanonicalStatus> EDITABLE_STATUSES = EnumSet.of(DRAFT, REWORK);
    private static final Set<ControlledContentCanonicalStatus> OPEN_CANDIDATE_STATUSES = EnumSet.of(
            DRAFT, REWORK, IN_REVIEW, READY_TO_PUBLISH, FINALIZING, FINALIZATION_FAILED);

    private static final Set<TransitionRule> ALLOWED_TRANSITIONS = Set.of(
            new TransitionRule(null, ACTIVE, REGISTER_ACTIVE),
            new TransitionRule(null, READY_TO_PUBLISH, REGISTER_READY_CANDIDATE),
            new TransitionRule(DRAFT, IN_REVIEW, SUBMIT),
            new TransitionRule(REWORK, IN_REVIEW, SUBMIT),
            new TransitionRule(IN_REVIEW, READY_TO_PUBLISH, APPROVE),
            new TransitionRule(IN_REVIEW, REWORK, REQUEST_REWORK),
            new TransitionRule(IN_REVIEW, DRAFT, WITHDRAW),
            new TransitionRule(IN_REVIEW, WITHDRAWN, WITHDRAW),
            new TransitionRule(IN_REVIEW, REJECTED, REJECT),
            new TransitionRule(DRAFT, CANCELLED, CANCEL),
            new TransitionRule(REWORK, CANCELLED, CANCEL),
            new TransitionRule(READY_TO_PUBLISH, CANCELLED, CANCEL),
            new TransitionRule(REJECTED, CANCELLED, CANCEL),
            new TransitionRule(READY_TO_PUBLISH, FINALIZING, START_FINALIZATION),
            new TransitionRule(READY_TO_PUBLISH, IN_REVIEW, SUBMIT),
            new TransitionRule(READY_TO_PUBLISH, ACTIVE, PUBLISH),
            new TransitionRule(FINALIZING, ACTIVE, FINALIZE_SUCCESS),
            new TransitionRule(FINALIZING, OBSOLETE, FINALIZE_SUCCESS),
            new TransitionRule(FINALIZING, FINALIZATION_FAILED, FINALIZE_FAILED),
            new TransitionRule(FINALIZATION_FAILED, FINALIZING, RETRY_FINALIZATION),
            new TransitionRule(ACTIVE, SUPERSEDED, SUPERSEDE_ACTIVE),
            new TransitionRule(ACTIVE, OBSOLETE, OBSOLETE_ACTIVE)
    );

    public boolean canTransition(ControlledContentCanonicalStatus from, ControlledContentCanonicalStatus to,
                                 ControlledContentTransitionAction action) {
        if (to == null || action == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.contains(new TransitionRule(from, to, action));
    }

    public void validateTransition(ControlledContentCanonicalStatus from, ControlledContentCanonicalStatus to,
                                   ControlledContentTransitionAction action) {
        if (!canTransition(from, to, action)) {
            throw new IllegalStateException("controlled content transition is forbidden: "
                    + from + " -> " + to + " by " + action);
        }
    }

    public boolean isEditable(ControlledContentCanonicalStatus status) {
        return EDITABLE_STATUSES.contains(status);
    }

    public void validateEditable(ControlledContentCanonicalStatus status) {
        if (!isEditable(status)) {
            throw new IllegalStateException("controlled content status is read-only: " + status);
        }
    }

    public boolean isOpenCandidate(ControlledContentCanonicalStatus status) {
        return OPEN_CANDIDATE_STATUSES.contains(status);
    }

    public boolean isActive(ControlledContentCanonicalStatus status) {
        return ACTIVE == status;
    }

    private record TransitionRule(ControlledContentCanonicalStatus from, ControlledContentCanonicalStatus to,
                                  ControlledContentTransitionAction action) {
    }

}
