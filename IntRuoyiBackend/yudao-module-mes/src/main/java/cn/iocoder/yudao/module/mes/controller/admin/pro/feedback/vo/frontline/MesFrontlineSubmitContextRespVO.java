package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 一线正式报工上下文 Response VO")
@Data
@Accessors(chain = true)
public class MesFrontlineSubmitContextRespVO {

    @Schema(description = "生产工单编号")
    private Long workOrderId;
    @Schema(description = "生产工单编码")
    private String workOrderCode;
    @Schema(description = "生产工单名称")
    private String workOrderName;
    @Schema(description = "生产任务编号")
    private Long taskId;
    @Schema(description = "生产任务编码")
    private String taskCode;
    @Schema(description = "产品物料编号")
    private Long itemId;
    @Schema(description = "工艺路线编号")
    private Long routeId;
    @Schema(description = "工艺路线工序编号")
    private Long routeProcessId;
    @Schema(description = "工序编号")
    private Long processId;
    @Schema(description = "工作站编号")
    private Long workstationId;
    @Schema(description = "设备编号")
    private Long deviceId;
    @Schema(description = "当前审批人用户编号")
    private Long approveUserId;
    @Schema(description = "记录本编号")
    private Long recordbookId;
    @Schema(description = "报工类型")
    private Integer feedbackType;
    @Schema(description = "计划数量")
    private BigDecimal scheduledQuantity;
    @Schema(description = "过期/计划完成时间")
    private LocalDateTime expireDate;
}
