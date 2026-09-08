package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureArchiveRecoveryDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureArchiveRecoveryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID;

@Service
public class ElectronicSignatureArchiveRecoveryService {

    private static final String RESULT_VERIFIED = "VERIFIED";

    @Resource
    private ElectronicSignatureArchiveRecoveryMapper archiveRecoveryMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long recordRecoveryVerification(Long archiveId, Long restoredBy, String businessRecordHash,
                                           String signatureRecordHash, String snapshotHash) {
        if (archiveId == null || restoredBy == null
                || StrUtil.hasBlank(businessRecordHash, signatureRecordHash, snapshotHash)) {
            throw exception(ESIGN_COMMAND_INVALID, "归档、恢复人、业务记录哈希、签名记录哈希和快照哈希不能为空");
        }
        LocalDateTime restoredAt = LocalDateTime.now();
        String restoreEvidenceHash = DigestUtil.sha256Hex(String.join("|",
                String.valueOf(TenantContextHolder.getRequiredTenantId()), String.valueOf(archiveId),
                restoredAt.toString(), String.valueOf(restoredBy), businessRecordHash, signatureRecordHash,
                snapshotHash, RESULT_VERIFIED));
        ElectronicSignatureArchiveRecoveryDO recovery = ElectronicSignatureArchiveRecoveryDO.builder()
                .archiveId(archiveId)
                .restoredAt(restoredAt)
                .restoredBy(restoredBy)
                .businessRecordHash(businessRecordHash)
                .signatureRecordHash(signatureRecordHash)
                .snapshotHash(snapshotHash)
                .restoreEvidenceHash(restoreEvidenceHash)
                .resultStatus(RESULT_VERIFIED)
                .build();
        recovery.setTenantId(TenantContextHolder.getRequiredTenantId());
        archiveRecoveryMapper.insert(recovery);
        return recovery.getId();
    }

}
