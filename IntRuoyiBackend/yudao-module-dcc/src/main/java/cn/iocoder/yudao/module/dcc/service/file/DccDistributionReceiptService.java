package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientAckReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientSignReqVO;

public interface DccDistributionReceiptService {

    void acknowledgeElectronicDistribution(Long userId, Long controlledFileId, Long distributionId,
                                           Long recipientId,
                                           DccControlledFileDistributionRecipientAckReqVO reqVO);

    void createDistributionRecipientSign(Long userId, Long controlledFileId, Long distributionId,
                                         Long recipientId,
                                         DccControlledFileDistributionRecipientSignReqVO reqVO);

}
