package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.dcc.enums.DccProjectProductCreateStatusConstants.WRITE_FAILED;

@Service
public class DccProjectProductCreateFailureService {

    @Resource
    private DccProjectProductCreateRequestMapper requestMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markWriteFailed(Long requestId, String errorCode, String errorMessage) {
        DccProjectProductCreateRequestDO request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new IllegalStateException("DCC_PROJECT_PRODUCT_CREATE_REQUEST_NOT_FOUND: " + requestId);
        }
        request.setStatus(WRITE_FAILED);
        request.setWriteErrorCode(errorCode);
        request.setWriteErrorMessage(errorMessage);
        request.setFailedTime(LocalDateTime.now());
        requestMapper.updateById(request);
    }
}
