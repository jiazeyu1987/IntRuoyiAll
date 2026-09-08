package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 排产工单删除影响预览")
@Data
public class MesProScheduleOrderDeleteImpactRespVO {

    private Long id;
    private String code;
    private Integer status;
    private Boolean frozen;
    private BigDecimal progressPercent;
    private LocalDateTime updateTime;
    private Integer pendingTaskCount;
    private Integer inProgressTaskCount;
    private Integer finishedTaskCount;
    private Integer feedbackCount;
    private Integer activeOrderCount;
    private Boolean productionFactsRetained;
    private Boolean reentryBlockedAfterRemoval;
}

