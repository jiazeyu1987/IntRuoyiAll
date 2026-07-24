package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 自动排产发布 Response VO")
@Data
public class MesProAutoScheduleApplyRespVO {

    private Boolean applied;

    private MesProAutoScheduleSummaryRespVO summary;

    private List<Long> createdTaskIds;

    private List<Long> deletedTaskIds;

    private List<Long> preservedTaskIds;

    private List<MesProAutoScheduleIssueRespVO> issues;

}
