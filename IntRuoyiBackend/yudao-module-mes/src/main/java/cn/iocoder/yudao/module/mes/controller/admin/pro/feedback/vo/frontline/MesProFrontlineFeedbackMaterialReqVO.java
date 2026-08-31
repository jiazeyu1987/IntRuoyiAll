package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 一线报工逐物料明细 Request VO")
@Data
@Accessors(chain = true)
public class MesProFrontlineFeedbackMaterialReqVO {

    @Schema(description = "正式 MES 物料编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "报工物料编号不能为空")
    private Long materialId;

    @Schema(description = "完成数量；明确填写 0 时为 0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "报工物料完成数量不能为空")
    private BigDecimal outputQuantity;

    @Schema(description = "损耗数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "报工物料损耗数量不能为空")
    private BigDecimal lossQuantity;

    @Valid
    @Schema(description = "损耗原因明细")
    private List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> lossDetails;

    @Valid
    @Schema(description = "选用设备快照")
    private MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice;

    @Valid
    @Schema(description = "设备参数读数")
    private List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> deviceParameterReadings;
}
