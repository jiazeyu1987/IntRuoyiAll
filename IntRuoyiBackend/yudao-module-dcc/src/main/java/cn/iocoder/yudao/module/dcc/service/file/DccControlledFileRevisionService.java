package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;

public interface DccControlledFileRevisionService {
    /** CC-2: freezes a NEW chain's selected working body as its first formal candidate. */
    DccControlledFileDO createInitialCandidate(Long userId, Long selectedIterationId,
                                               DccControlledFileSubmitIterationReqVO request);
    /** A must call this inside its formal submission transaction before starting approval. */
    DccControlledFileDO createRevision(Long userId, Long controlledBaselineId, Long selectedIterationId,
                                      DccControlledFileSubmitIterationReqVO request);

    /** NEW submission calls before insert; historical rows must never be reclassified. */
    void recordInitialIntent(DccControlledFileDO newFile);
}
