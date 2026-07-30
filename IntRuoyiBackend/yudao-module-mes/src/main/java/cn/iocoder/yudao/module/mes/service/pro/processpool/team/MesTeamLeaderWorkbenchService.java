package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;

public interface MesTeamLeaderWorkbenchService {

    PageResult<ProcessPoolTimelineEventRespVO> getSubmissionPage(Long leaderUserId, String leaderType,
                                                                 MesTeamLeaderSubmissionPageReqVO reqVO);

    ProcessPoolTimelineDetailRespVO getSubmissionDetail(Long leaderUserId, String leaderType, Long eventId);
}
