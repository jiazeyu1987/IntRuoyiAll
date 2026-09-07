package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationFollowupPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationFollowupRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactTaskRespVO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

public interface DccPublicationFollowupQueryService {
    DccPublicationFollowupRespVO getFileFollowup(Long userId, Long controlledFileId);
    PageResult<DccPublicationFollowupRespVO> getManagementPage(Long userId, DccPublicationFollowupPageReqVO reqVO);
    PageResult<DccPublicationImpactTaskRespVO> getMyImpactTasks(Long userId, DccPublicationImpactTaskPageReqVO reqVO);
}
