package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 排程日历产能生成 Response VO")
@Data
@Builder
public class MesProScheduleCalendarCapacityGenerateRespVO {

    @Schema(description = "开始日期")
    private String startDate;

    @Schema(description = "结束日期")
    private String endDate;

    @Schema(description = "纳入生成的产线数量")
    private Integer lineCount;

    @Schema(description = "新增日期班次产能数量")
    private Integer generatedCount;

    @Schema(description = "因已存在日期班次产能而跳过的数量")
    private Integer skippedExistingCount;

    @Schema(description = "因休息日而跳过的产线日期数量")
    private Integer skippedRestCount;

    @Schema(description = "因无可用班次而跳过的产线日期数量")
    private Integer skippedNoShiftCount;

    @Schema(description = "跳过明细")
    private List<SkippedDetail> skippedDetails;

    @Schema(description = "管理后台 - MES 排程日历产能生成跳过明细")
    @Data
    @Builder
    public static class SkippedDetail {

        @Schema(description = "日期")
        private String date;

        @Schema(description = "产线编号")
        private String lineCode;

        @Schema(description = "产线名称")
        private String lineName;

        @Schema(description = "班次名称")
        private String shiftName;

        @Schema(description = "原因码")
        private String reasonCode;

        @Schema(description = "原因说明")
        private String reasonText;

    }

}
