package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureTimeEvidenceDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureTimeEvidenceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID;

@Service
public class ElectronicSignatureTrustedTimeService {

    public static final long MAX_TRUSTED_TIME_DRIFT_MILLIS = 1000L;
    private static final String STATUS_TRUSTED = "TRUSTED";

    @Resource
    private ElectronicSignatureTimeEvidenceMapper timeEvidenceMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createTrustedTimeEvidence(String trustedTimeSource, LocalDateTime sourceReportedAt,
                                          LocalDateTime serverObservedAt) {
        if (StrUtil.isBlank(trustedTimeSource) || sourceReportedAt == null || serverObservedAt == null) {
            throw exception(ESIGN_COMMAND_INVALID, "可信时间源、源时间和服务器观测时间不能为空");
        }
        long driftMillis = Math.abs(Duration.between(sourceReportedAt, serverObservedAt).toMillis());
        if (driftMillis > MAX_TRUSTED_TIME_DRIFT_MILLIS) {
            throw exception(ESIGN_COMMAND_INVALID, "可信时间源漂移超过阈值");
        }
        String evidenceHash = DigestUtil.sha256Hex(String.join("|",
                String.valueOf(TenantContextHolder.getRequiredTenantId()), trustedTimeSource,
                sourceReportedAt.toString(), serverObservedAt.toString(), String.valueOf(driftMillis), STATUS_TRUSTED));
        ElectronicSignatureTimeEvidenceDO evidence = ElectronicSignatureTimeEvidenceDO.builder()
                .trustedTimeSource(trustedTimeSource)
                .sourceReportedAt(sourceReportedAt)
                .serverObservedAt(serverObservedAt)
                .driftMillis(driftMillis)
                .evidenceHash(evidenceHash)
                .status(STATUS_TRUSTED)
                .build();
        evidence.setTenantId(TenantContextHolder.getRequiredTenantId());
        timeEvidenceMapper.insert(evidence);
        return evidence.getId();
    }

}
