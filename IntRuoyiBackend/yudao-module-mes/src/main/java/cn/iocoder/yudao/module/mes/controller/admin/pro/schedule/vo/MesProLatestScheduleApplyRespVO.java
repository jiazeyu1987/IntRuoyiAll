package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 最近一次成功排产 Response VO")
@Data
public class MesProLatestScheduleApplyRespVO {

    private Boolean hasData;

    private LocalDateTime appliedAt;

    private String operationType;

    private Long scheduleOrderId;

    private String scheduleOrderCode;

    private Long operatorId;

    private String operatorName;

    private String reason;

}
