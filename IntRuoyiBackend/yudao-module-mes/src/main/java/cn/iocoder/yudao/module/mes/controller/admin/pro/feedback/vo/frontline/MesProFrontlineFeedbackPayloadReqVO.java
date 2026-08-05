package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 一线报工提交报工载荷 Request VO")
@Data
@Accessors(chain = true)
public class MesProFrontlineFeedbackPayloadReqVO {

    @Schema(description = "报工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "FB-F2-001")
    @NotNull(message = "报工单编号不能为空")
    private String code;

    @Schema(description = "报工类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "报工类型不能为空")
    private Integer type;

    @Schema(description = "工作站编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11")
    @NotNull(message = "工作站不能为空")
    private Long workstationId;

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "21")
    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "31")
    @NotNull(message = "工序不能为空")
    private Long processId;

    @Schema(description = "生产工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "41")
    @NotNull(message = "生产工单不能为空")
    private Long workOrderId;

    @Schema(description = "生产任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "51")
    @NotNull(message = "生产任务不能为空")
    private Long taskId;

    @Schema(description = "排产工单编号", example = "81")
    private Long scheduleOrderId;

    @Schema(description = "排产工单工序编号", example = "82")
    private Long scheduleOrderProcessId;

    @Schema(description = "产品物料编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "61")
    @NotNull(message = "产品物料不能为空")
    private Long itemId;

    @Schema(description = "过期日期")
    private LocalDateTime expireDate;

    @Schema(description = "排产数量", example = "300.000")
    private BigDecimal scheduledQuantity;

    @Schema(description = "输出数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.500")
    @NotNull(message = "输出数量不能为空")
    private BigDecimal outputQuantity;

    @Schema(description = "损耗数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2.500")
    @NotNull(message = "损耗数量不能为空")
    private BigDecimal lossQuantity;

    @Schema(description = "工废数量", example = "1.000")
    private BigDecimal laborScrapQuantity;

    @Schema(description = "料废数量", example = "1.500")
    private BigDecimal materialScrapQuantity;

    @Schema(description = "其他废品数量", example = "0.000")
    private BigDecimal otherScrapQuantity;

    @Schema(description = "损耗原因 ID，来自当前工序后端配置", example = "8301")
    private Long lossReasonId;

    @Schema(description = "当前审批人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "7001")
    @NotNull(message = "当前审批人不能为空")
    private Long approveUserId;

    @Schema(description = "备注", example = "frontline production")
    private String remark;

}
