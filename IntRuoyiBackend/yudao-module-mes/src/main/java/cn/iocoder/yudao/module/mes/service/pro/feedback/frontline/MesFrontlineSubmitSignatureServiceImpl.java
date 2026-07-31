package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SIGNATURE_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SIGNATURE_PASSWORD_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SIGNATURE_PERSIST_FAILED;

@Service
@RequiredArgsConstructor
public class MesFrontlineSubmitSignatureServiceImpl implements MesFrontlineSubmitSignatureService {

    private static final Long FRONTLINE_SIGNATURE_EXECUTION_ID = 0L;
    private static final String SIGNATURE_PURPOSE = "一线报工提交";
    private static final String SNAPSHOT_STATUS_CAPTURED = "CAPTURED";
    private static final String AUTHORIZATION_BASIS = "一线报工实际员工电子签名授权启用";

    private final AdminUserService adminUserService;
    private final DccElectronicSignatureAuthorizationService authorizationService;
    private final MesProBatchRecordExecutionSignatureMapper signatureMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordSubmitSignature(Long actualEmployeeId, String password, String comment) {
        if (actualEmployeeId == null || StrUtil.isBlank(password)) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SIGNATURE_PERSIST_FAILED, "signature context");
        }
        if (!authorizationService.isElectronicSignatureEnabled(actualEmployeeId)) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SIGNATURE_NOT_AUTHORIZED, actualEmployeeId);
        }
        AdminUserDO user = adminUserService.getUser(actualEmployeeId);
        if (user == null || StrUtil.isBlank(user.getPassword())
                || !adminUserService.isPasswordMatch(password, user.getPassword())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SIGNATURE_PASSWORD_INVALID, actualEmployeeId);
        }

        LocalDateTime signedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        MesProBatchRecordExecutionSignatureDO signature = MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(FRONTLINE_SIGNATURE_EXECUTION_ID)
                .actorId(actualEmployeeId)
                .actionType(MesProBatchRecordExecutionSignatureService.ACTION_SUBMIT)
                .signatureMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_PASSWORD)
                .passwordVerified(Boolean.TRUE)
                .comment(StrUtil.blankToDefault(StrUtil.trim(comment), SIGNATURE_PURPOSE))
                .signedAt(signedAt)
                .signatureDisplayAt(signedAt)
                .signatureTimeMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_MODE_SERVER)
                .selectedTimeZone(MesProBatchRecordExecutionSignatureService.DEFAULT_SIGNATURE_TIME_ZONE)
                .selectedTimePolicyVersion(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_POLICY_VERSION)
                .selectedTimeAuditHash(DigestUtil.sha256Hex(String.join("|",
                        MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_POLICY_VERSION,
                        String.valueOf(FRONTLINE_SIGNATURE_EXECUTION_ID),
                        MesProBatchRecordExecutionSignatureService.ACTION_SUBMIT,
                        String.valueOf(actualEmployeeId),
                        String.valueOf(signedAt),
                        MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_MODE_SERVER,
                        String.valueOf(signedAt),
                        "",
                        MesProBatchRecordExecutionSignatureService.DEFAULT_SIGNATURE_TIME_ZONE,
                        "")))
                .actorName(user.getNickname())
                .actorUsernameSnapshot(user.getUsername())
                .actorNicknameSnapshot(user.getNickname())
                .actorDeptIdSnapshot(user.getDeptId())
                .signaturePurpose(SIGNATURE_PURPOSE)
                .authorizationBasis(AUTHORIZATION_BASIS)
                .authenticationMethod(MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_PASSWORD)
                .snapshotStatus(SNAPSHOT_STATUS_CAPTURED)
                .build();
        int inserted = signatureMapper.insert(signature);
        if (inserted <= 0 || signature.getId() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SIGNATURE_PERSIST_FAILED, actualEmployeeId);
        }
        return signature.getId();
    }
}
