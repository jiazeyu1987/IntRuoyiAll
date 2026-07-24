package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationAuditMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;

@Service
@Validated
public class DccElectronicSignatureAuthorizationAuditServiceImpl
        implements DccElectronicSignatureAuthorizationAuditService {

    @Resource
    private DccElectronicSignatureAuthorizationAuditMapper authorizationAuditMapper;

    @Override
    public void recordAuthorizationChange(DccElectronicSignatureAuthorizationAuditDO audit) {
        if (audit == null || StrUtil.isBlank(audit.getReason())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED);
        }
        if (audit.getOperatedAt() == null) {
            audit.setOperatedAt(LocalDateTime.now());
        }
        audit.setReason(StrUtil.trim(audit.getReason()));
        if (authorizationAuditMapper.insert(audit) <= 0) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
    }
}
