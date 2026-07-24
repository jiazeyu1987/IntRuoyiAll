package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 自动排产问题 Response VO")
@Data
public class MesProAutoScheduleIssueRespVO {

    private Long id;

    private String issueType;

    private String severity;

    private Long workOrderId;

    private String workOrderCode;

    private Long taskId;

    private Long processId;

    private String processName;

    private Long workstationId;

    private String workstationName;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private LocalDateTime calendarDate;

    private Long shiftId;

    private String shiftName;

    private BigDecimal requiredQty;

    private BigDecimal availableQty;

    private BigDecimal shortageQty;

    private String message;

    private Boolean resolved;

    private String status;

    private String sourceType;

    private Long sourceId;

    private String resolutionReason;

    private Long resolvedBy;

    private LocalDateTime resolvedAt;

}
