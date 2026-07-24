package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 排程日历月份视图 Response VO")
@Data
public class MesProScheduleCalendarMonthRespVO {

    private String month;

    private String simulationCurrentDate;

    private CurrentScheduleStatus currentScheduleStatus;

    private List<DaySummaryItem> days;

    @Data
    @Builder
    public static class CurrentScheduleStatus {
        private Boolean hasCurrentSchedule;
        private String updatedAt;
        private Integer totalTaskCount;
    }

    @Data
    @Builder
    public static class DaySummaryItem {
        private String date;
        private Boolean holiday;
        private String dateShiftMode;
        private Integer totalTaskCount;
        private Integer totalOrderCount;
        private Integer dayShiftTaskCount;
        private Integer nightShiftTaskCount;
        private Integer shortageCount;
    }

}
