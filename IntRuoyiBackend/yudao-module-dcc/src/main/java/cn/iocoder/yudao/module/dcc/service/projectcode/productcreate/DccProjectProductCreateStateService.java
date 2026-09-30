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

@Service
public class DccProjectProductCreateStateService {

    @Resource
    private DccProjectProductCreateRequestMapper requestMapper;
    @Resource
    private DccProjectProductIdentityClaimMapper identityClaimMapper;

    @Transactional(rollbackFor = Exception.class)
    public DccProjectProductCreateRequestDO markApprovalDecision(Long operatorUserId, Long requestId,
                                                                 String reason, boolean approve) {
        DccProjectProductCreateRequestDO request = requireRequestForUpdate(requestId);
        if (!DccProjectProductCreateStatusConstants.PENDING_APPROVAL.equals(request.getStatus())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        request.setApproverUserId(operatorUserId);
        request.setApprovedTime(LocalDateTime.now());
        if (approve) {
            request.setApprovalReason(required(reason));
            request.setStatus(DccProjectProductCreateStatusConstants.WRITING);
        } else {
            request.setRejectReason(required(reason));
            request.setStatus(DccProjectProductCreateStatusConstants.REJECTED);
            identityClaimMapper.deleteByRequestId(request.getId());
        }
        requestMapper.updateById(request);
        return request;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccProjectProductCreateRequestDO markRetryWriting(Long requestId) {
        DccProjectProductCreateRequestDO request = requireRequestForUpdate(requestId);
        if (!DccProjectProductCreateStatusConstants.WRITE_FAILED.equals(request.getStatus())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        request.setStatus(DccProjectProductCreateStatusConstants.WRITING);
        requestMapper.updateById(request);
        return request;
    }

    private DccProjectProductCreateRequestDO requireRequestForUpdate(Long requestId) {
        DccProjectProductCreateRequestDO request = requestMapper.selectByIdForUpdate(requestId);
        if (request == null) {
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
