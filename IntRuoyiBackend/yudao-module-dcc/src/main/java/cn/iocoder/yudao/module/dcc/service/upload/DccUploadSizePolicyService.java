package cn.iocoder.yudao.module.dcc.service.upload;

import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileUploadPolicyDO;

import java.time.LocalDateTime;
import java.util.List;

public interface DccUploadSizePolicyService {

    List<DccControlledFileUploadPolicyDO> getPolicyList();

    Long createPolicy(DccUploadSizePolicySaveCommand command);

    void updatePolicy(Long id, DccUploadSizePolicySaveCommand command);

    DccUploadSizePolicyMatch resolveEffectivePolicy(Long categoryId, String purpose, LocalDateTime now);

    DccUploadSizePolicyMatch validateUploadSize(Long categoryId, String purpose, long fileSize, LocalDateTime now);

}
