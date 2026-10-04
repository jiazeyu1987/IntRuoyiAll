package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.dcc.enums.DccProjectProductCreateStatusConstants.WRITE_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectProductCreateStatusConstants.WRITING;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.WRITE_INCOMPLETE;

@Service
public class DccProjectProductCreateFailureService {

    @Resource
    private DccProjectProductCreateRequestMapper requestMapper;
    @Resource private DccProjectProductAuditService productAudit;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markWriteFailed(Long requestId, String errorCode, String errorMessage) {
        DccProjectProductCreateRequestDO request = requestMapper.selectByIdForUpdate(requestId);
        if (request == null || !java.util.Objects.equals(request.getTenantId(),
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS);
        }
        if (!WRITING.equals(request.getStatus())) throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        String version=productAudit.attemptVersion(request.getWriteAttemptNo());
        productAudit.validateReason(request.getWriteReason());
        var before=productAudit.snapshot(requestId);
        request.setStatus(WRITE_FAILED);
        request.setWriteErrorCode(errorCode);
        request.setWriteErrorMessage(errorMessage);
        request.setFailedTime(LocalDateTime.now());
        if (requestMapper.updateById(request) != 1) throw exception(WRITE_INCOMPLETE, "写入失败记录");
        productAudit.append("dcc.project-product.write-failed",requestId,version,request.getWriteReason(),before);
    }
}
