package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 自动排产问题查询 Request VO")
@Data
public class MesProAutoScheduleIssueQueryReqVO {

    @Schema(description = "工单编号")
    private Long workOrderId;

    @Schema(description = "任务编号")
    private Long taskId;

    @Schema(description = "问题类型")
    private String issueType;

    @Schema(description = "严重级别")
    private String severity;

}
