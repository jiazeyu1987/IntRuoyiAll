package cn.iocoder.yudao.module.dcc.service.projectcode.onboarding;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.onboarding.DccProductOnboardingCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProductOnboardingRequestDO;

public interface DccProductOnboardingService {

    Long createRequest(Long applicantUserId, DccProductOnboardingCreateReqVO reqVO);

    DccProductOnboardingRequestDO approveRequest(Long approverUserId, Long requestId);
}
