package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPaperDistributionRecordRespVO;

import java.util.List;

public interface DccPaperDistributionAckService {

    List<DccPaperDistributionRecordRespVO> getPaperDistributionRecords(Long controlledFileId);

    void acknowledgePaperDistribution(Long userId, Long controlledFileId, Long distributionId,
                                      List<Long> recipientUserIds);

    void recoverPaperDistribution(Long userId, Long controlledFileId, Long distributionId);
}
