package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackMaterialDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MesProProcessPoolTimelineReadMapper {

    Long selectTimelineCount(@Param("reqVO") ProcessPoolTimelinePageReqVO reqVO);

    List<ProcessPoolTimelineEventReadDO> selectTimelinePage(@Param("reqVO") ProcessPoolTimelinePageReqVO reqVO);

    ProcessPoolTimelineEventReadDO selectTimelineDetailById(@Param("id") Long id);

    List<ProcessPoolTimelineReportAllocationReadDO> selectReportAllocationsByEventIds(
            @Param("eventIds") List<Long> eventIds);

    List<MesProFeedbackMaterialDO> selectFeedbackMaterialsByFeedbackIds(@Param("feedbackIds") List<Long> feedbackIds);

}
