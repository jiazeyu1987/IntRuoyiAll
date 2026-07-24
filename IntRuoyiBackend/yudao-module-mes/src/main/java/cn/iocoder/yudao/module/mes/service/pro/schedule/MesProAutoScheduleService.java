package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttLinkRespVO;
import jakarta.validation.Valid;

import java.util.List;

public interface MesProAutoScheduleService {

    MesProAutoSchedulePreviewRespVO preview(@Valid MesProAutoSchedulePreviewReqVO reqVO);

    MesProAutoScheduleApplyRespVO apply(@Valid MesProAutoSchedulePreviewReqVO reqVO);

    MesProAutoScheduleReplanPreviewRespVO replanPreview(@Valid MesProAutoScheduleReplanReqVO reqVO);

    MesProAutoScheduleApplyRespVO replanApply(@Valid MesProAutoScheduleReplanReqVO reqVO);

    MesProAutoScheduleApplyRespVO replanApplyForNightly(@Valid MesProAutoScheduleReplanReqVO reqVO);

    MesProLatestScheduleApplyRespVO getLatestSuccessfulScheduleApply();

    MesProReplanExplanationRespVO getLatestReplanExplanation();

    List<MesProAutoScheduleIssueRespVO> getIssues(MesProAutoScheduleIssueQueryReqVO reqVO);

    Long createIssue(@Valid MesProAutoScheduleIssueCreateReqVO reqVO);

    void resolveIssue(@Valid MesProAutoScheduleIssueResolveReqVO reqVO);

    Long cancelNightShift(@Valid MesProAutoScheduleCancelNightShiftReqVO reqVO);

    List<GanttLinkRespVO> getDependencies(List<Long> workOrderIds, List<Long> taskIds);

}
