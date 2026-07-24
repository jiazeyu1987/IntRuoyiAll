package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarWorkOrderAnalysisRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttDataRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttLinkRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 自动排产预览 Response VO")
@Data
public class MesProAutoSchedulePreviewRespVO {

    private Boolean previewOnly;

    private String calendarContextToken;

    private MesProScheduleCalendarRulesRespVO calendarSummary;

    private MesProAutoScheduleSummaryRespVO summary;

    private List<GanttDataRespVO> tasks;

    private List<GanttLinkRespVO> links;

    private List<MesProAutoScheduleIssueRespVO> issues;

    private List<MesProScheduleCalendarWorkOrderAnalysisRespVO> workOrderAnalyses;

}
