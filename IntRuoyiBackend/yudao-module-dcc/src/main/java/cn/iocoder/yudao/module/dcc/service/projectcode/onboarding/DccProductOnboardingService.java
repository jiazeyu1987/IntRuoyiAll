package cn.iocoder.yudao.module.dcc.service.projectcode.onboarding;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.onboarding.DccProductOnboardingCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProductOnboardingRequestDO;

import java.util.List;

public interface DccProductOnboardingService {

    Long createRequest(Long applicantUserId, DccProductOnboardingCreateReqVO reqVO);

    List<DccProductOnboardingRequestDO> getPendingRequests();

    DccProductOnboardingRequestDO approveRequest(Long approverUserId, Long requestId);
}
