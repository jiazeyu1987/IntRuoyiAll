package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductIdentityClaimMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectProductCreateStatusConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.WRITE_INCOMPLETE;

@Service
public class DccProjectProductCreateStateService {

    @Resource
    private DccProjectProductCreateRequestMapper requestMapper;
    @Resource
    private DccProjectProductIdentityClaimMapper identityClaimMapper;
    @Resource private DccProjectProductAuditService productAudit;
    @Resource private DccProjectProductNotificationService notifications;

    @Transactional(rollbackFor = Exception.class)
    public DccProjectProductCreateRequestDO markApprovalDecision(Long operatorUserId, Long requestId,
                                                                 String reason, boolean approve) {
        DccProjectProductCreateRequestDO request = requireRequestForUpdate(requestId);
        if (!DccProjectProductCreateStatusConstants.PENDING_APPROVAL.equals(request.getStatus())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        productAudit.validateReason(reason);
        var before=productAudit.snapshot(requestId);
        request.setApproverUserId(operatorUserId);
        request.setApprovedTime(LocalDateTime.now());
        if (approve) {
            request.setApprovalReason(required(reason));
            request.setStatus(DccProjectProductCreateStatusConstants.WRITING);
            request.setWriteAttemptNo(1);request.setWriteReason(required(reason));request.setWriteOperatorUserId(operatorUserId);
        } else {
            request.setRejectReason(required(reason));
            request.setStatus(DccProjectProductCreateStatusConstants.REJECTED);
            identityClaimMapper.deleteByRequestId(request.getId());
        }
        if (requestMapper.updateById(request) != 1) throw exception(WRITE_INCOMPLETE, "批准决定");
        productAudit.append("dcc.project-product.approve",requestId,"1",reason,before);
        if (!approve) notifications.rejected(request, true);
        return request;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccProjectProductCreateRequestDO markRetryWriting(Long operatorUserId, Long requestId, String reason) {
        DccProjectProductCreateRequestDO request = requireRequestForUpdate(requestId);
        if (!DccProjectProductCreateStatusConstants.WRITE_FAILED.equals(request.getStatus())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        if(operatorUserId==null || operatorUserId<=0) throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        productAudit.validateReason(reason);
        var before=productAudit.snapshot(requestId);
        productAudit.attemptVersion(request.getWriteAttemptNo());
        request.setWriteAttemptNo(Math.addExact(request.getWriteAttemptNo(),1));
        request.setWriteReason(required(reason));request.setWriteOperatorUserId(operatorUserId);
        request.setStatus(DccProjectProductCreateStatusConstants.WRITING);
        if (requestMapper.updateById(request) != 1) throw exception(WRITE_INCOMPLETE, "申请重试状态");
        productAudit.append("dcc.project-product.retry",requestId,productAudit.attemptVersion(request.getWriteAttemptNo()),reason,before);
        return request;
    }

    private DccProjectProductCreateRequestDO requireRequestForUpdate(Long requestId) {
        DccProjectProductCreateRequestDO request = requestMapper.selectByIdForUpdate(requestId);
        if (request == null || !java.util.Objects.equals(request.getTenantId(),
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS);
        }
        return request;
    }

    private String required(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        return normalized;
    }
}
