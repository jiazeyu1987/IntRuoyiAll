package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.APPROVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.CANCEL;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.CREATE_CANDIDATE;
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
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_CONTROLLED_FILE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.MES_ROUTE;

public record ControlledContentTransitionProfile(
        ControlledContentType contentType,
        Set<ControlledContentTransitionAction> supportedActions) {

    private static final Map<ControlledContentType, ControlledContentTransitionProfile> PROFILES = Map.of(
            MES_ROUTE, new ControlledContentTransitionProfile(
                    MES_ROUTE,
                    EnumSet.of(REGISTER_ACTIVE, CREATE_CANDIDATE, SUBMIT, WITHDRAW, CANCEL, APPROVE, REQUEST_REWORK, REJECT,
                            PUBLISH, SUPERSEDE_ACTIVE)),
            DCC_CONTROLLED_FILE, new ControlledContentTransitionProfile(
                    DCC_CONTROLLED_FILE,
                    EnumSet.of(REGISTER_ACTIVE, CREATE_CANDIDATE, SUBMIT, WITHDRAW, CANCEL, APPROVE, REQUEST_REWORK, REJECT,
                            START_FINALIZATION, RETRY_FINALIZATION, FINALIZE_SUCCESS, FINALIZE_FAILED,
                            SUPERSEDE_ACTIVE, OBSOLETE_ACTIVE)),
            DCC_REGISTRATION_CERTIFICATE, new ControlledContentTransitionProfile(
                    DCC_REGISTRATION_CERTIFICATE,
                    EnumSet.of(REGISTER_ACTIVE, REGISTER_READY_CANDIDATE, PUBLISH, SUPERSEDE_ACTIVE)));

    public ControlledContentTransitionProfile {
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        if (supportedActions == null || supportedActions.isEmpty()) {
            throw new IllegalArgumentException("supportedActions must not be empty");
        }
        supportedActions = Set.copyOf(supportedActions);
    }

    public static ControlledContentTransitionProfile requiredFor(ControlledContentType contentType) {
        ControlledContentTransitionProfile profile = PROFILES.get(contentType);
        if (profile == null) {
            throw new IllegalStateException("controlled content transition profile is missing: " + contentType);
        }
        return profile;
    }

    public boolean supports(ControlledContentTransitionAction action) {
        return supportedActions.contains(action);
    }

}
