package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Schema(description = "管理后台 - MES 排产工单待同步差异分页 Response VO")
@Data
public class MesProScheduleOrderAdmissionDiffPageRespVO {

    @Schema(description = "总数", example = "20")
    private Long total = 0L;

    @Schema(description = "差异行")
    private List<MesProScheduleOrderAdmissionDiffRespVO> list = Collections.emptyList();

    @Schema(description = "统计")
    private MesProScheduleOrderAdmissionDiffSummaryRespVO summary = new MesProScheduleOrderAdmissionDiffSummaryRespVO();

}
