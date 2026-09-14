package cn.iocoder.yudao.module.system.enums.controlledcontent;

/**
 * Business action that causes a controlled content lifecycle transition.
 */
public enum ControlledContentTransitionAction {

    REGISTER_ACTIVE,
    REGISTER_READY_CANDIDATE,
    CREATE_CANDIDATE,
    SUBMIT,
    WITHDRAW,
    CANCEL,
    APPROVE,
    REQUEST_REWORK,
    REJECT,
    START_FINALIZATION,
    RETRY_FINALIZATION,
    FINALIZE_SUCCESS,
    FINALIZE_FAILED,
    PUBLISH,
    SUPERSEDE_ACTIVE,
    OBSOLETE_ACTIVE

}
