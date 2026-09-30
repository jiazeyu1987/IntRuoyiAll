package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;

import java.util.List;

public interface DccProjectProductCreateService {

    Long createRequest(Long applicantUserId, DccProjectProductCreateReqVO reqVO);

    List<DccProjectProductCreateRequestDO> getPendingRequests();

    DccProjectProductCreateRequestDO review(Long operatorUserId, Long requestId, String reason, boolean approve);

    DccProjectProductCreateRequestDO approve(Long operatorUserId, Long requestId, String reason, boolean approve);

    DccProjectProductCreateRequestDO retryWrite(Long operatorUserId, Long requestId);
}
