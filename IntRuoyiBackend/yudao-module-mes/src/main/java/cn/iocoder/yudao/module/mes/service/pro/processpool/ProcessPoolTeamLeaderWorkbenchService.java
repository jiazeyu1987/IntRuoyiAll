package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTeamLeaderWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;

public interface ProcessPoolTeamLeaderWorkbenchService {

    ProcessPoolTeamLeaderWorkbenchRespVO getWorkbench(ProcessPoolTimelinePageReqVO reqVO);

    ProcessPoolTimelineDetailRespVO getDetail(Long eventId);

}
