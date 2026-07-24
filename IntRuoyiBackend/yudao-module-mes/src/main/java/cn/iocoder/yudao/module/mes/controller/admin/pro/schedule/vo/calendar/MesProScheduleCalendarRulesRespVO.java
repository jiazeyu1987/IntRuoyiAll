package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - MES 排程日历规则 Response VO")
@Data
public class MesProScheduleCalendarRulesRespVO {

    private Long id;

    private Boolean skipStatutoryHolidays;

    private String weekendRestMode;

    private Map<String, String> dateShiftModeByDate;

    private String simulationCurrentDate;

    private Boolean temporaryFreezeEnabled;

    private String calendarContextToken;

}
