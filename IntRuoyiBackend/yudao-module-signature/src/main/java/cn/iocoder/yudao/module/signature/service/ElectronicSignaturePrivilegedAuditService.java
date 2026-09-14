package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignaturePrivilegedAuditDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignaturePrivilegedAuditMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID;

@Service
public class ElectronicSignaturePrivilegedAuditService {

    private static final String RESULT_RECORDED = "RECORDED";

    @Resource
    private ElectronicSignaturePrivilegedAuditMapper privilegedAuditMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long recordPrivilegedAccess(Long reviewerUserId, String operationCode, String reason) {
        if (reviewerUserId == null || StrUtil.hasBlank(operationCode, reason)) {
            throw exception(ESIGN_COMMAND_INVALID, "特权审计用户、操作和原因不能为空");
        }
        LocalDateTime auditedAt = LocalDateTime.now();
        String evidenceHash = DigestUtil.sha256Hex(String.join("|",
                String.valueOf(TenantContextHolder.getRequiredTenantId()), String.valueOf(reviewerUserId),
                operationCode, reason, auditedAt.toString(), RESULT_RECORDED));
        ElectronicSignaturePrivilegedAuditDO audit = ElectronicSignaturePrivilegedAuditDO.builder()
                .reviewerUserId(reviewerUserId)
                .operationCode(operationCode)
                .reason(reason)
                .resultStatus(RESULT_RECORDED)
                .evidenceHash(evidenceHash)
                .auditedAt(auditedAt)
                .build();
        audit.setTenantId(TenantContextHolder.getRequiredTenantId());
        privilegedAuditMapper.insert(audit);
        return audit.getId();
    }

}
