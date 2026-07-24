package cn.iocoder.yudao.module.showroom.workflow.service;

import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestSignatureDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestSignatureMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ShowroomApprovalSignatureService {

    public static final String APPROVAL_STAGE_SUPERVISOR = "SUPERVISOR";
    public static final String APPROVAL_STAGE_PUBLICITY = "PUBLICITY";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String SIGNATURE_MODE_PASSWORD = "PASSWORD";

    private final AdminUserService adminUserService;
    private final DccElectronicSignatureAuthorizationService authorizationService;
    private final ShowroomChangeRequestSignatureMapper signatureMapper;

    public ShowroomApprovalSignatureService(AdminUserService adminUserService,
                                            DccElectronicSignatureAuthorizationService authorizationService,
                                            ShowroomChangeRequestSignatureMapper signatureMapper) {
        this.adminUserService = adminUserService;
        this.authorizationService = authorizationService;
        this.signatureMapper = signatureMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordSignedDecision(Long changeRequestId, String approvalStage, String actionType,
                                     Long actorId, String password, String comment) {
        requireNonNull(changeRequestId, "SHOWROOM_APPROVAL_SIGNATURE_MISSING: change request id is required");
        requireText(approvalStage, "SHOWROOM_APPROVAL_SIGNATURE_MISSING: approval stage is required");
        requireText(actionType, "SHOWROOM_APPROVAL_SIGNATURE_MISSING: action type is required");
        requireNonNull(actorId, "SHOWROOM_APPROVAL_SIGNATURE_MISSING: actor is required");
        requireText(password, "SHOWROOM_APPROVAL_SIGNATURE_PASSWORD_REQUIRED: password is required");
        if (!authorizationService.isElectronicSignatureEnabled(actorId)) {
            throw new IllegalStateException(
                    "SHOWROOM_APPROVAL_SIGNATURE_NOT_AUTHORIZED: current user is not authorized for electronic signature");
        }
        AdminUserDO user = adminUserService.getUser(actorId);
        if (user == null || user.getPassword() == null
                || !adminUserService.isPasswordMatch(password, user.getPassword())) {
            throw new IllegalStateException(
                    "SHOWROOM_APPROVAL_SIGNATURE_PASSWORD_INVALID: current password is invalid");
        }
        int inserted = signatureMapper.insert(ShowroomChangeRequestSignatureDO.builder()
                .changeRequestId(changeRequestId)
                .approvalStage(approvalStage)
                .actionType(actionType)
                .actorId(actorId)
                .signatureMode(SIGNATURE_MODE_PASSWORD)
                .passwordVerified(Boolean.TRUE)
                .comment(nullableText(comment))
                .signedAt(LocalDateTime.now())
                .build());
        if (inserted <= 0) {
            throw new IllegalStateException(
                    "SHOWROOM_APPROVAL_SIGNATURE_PERSIST_FAILED: signature record was not persisted");
        }
    }

    private static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
