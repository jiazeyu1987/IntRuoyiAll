package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 自动排产摘要 Response VO")
@Data
public class MesProAutoScheduleSummaryRespVO {

    private Integer workOrderCount;

    private Integer generatedTaskCount;

    private Integer preservedTaskCount;

    private Integer blockingIssueCount;

    private Integer shortageCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

}
