package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportCorrectionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 生产报工业务字段修订 Request VO")
@Data
@Accessors(chain = true)
public class ProcessPoolProductionReportCorrectionReqVO {

    @NotNull(message = "提交事件不能为空")
    private Long eventId;

    @NotNull(message = "完成数量不能为空")
    @DecimalMin(value = "0.001", message = "完成数量必须大于 0")
    private BigDecimal outputQuantity;

    @Valid
    @NotNull(message = "损耗明细不能为空")
    private List<LossDetailReqVO> lossDetails;

    @Valid
    private List<DeviceParameterReadingReqVO> deviceParameterReadings;

    @NotBlank(message = "修改原因不能为空")
    @Size(max = 500, message = "修改原因不能超过 500 个字符")
    private String changeReason;

    @NotBlank(message = "签名密码不能为空")
    private String signaturePassword;

    public MesProcessPoolProductionReportCorrectionCommand toCommand() {
        return new MesProcessPoolProductionReportCorrectionCommand()
                .setEventId(eventId)
                .setOutputQuantity(outputQuantity)
                .setLossDetails(lossDetails.stream().map(LossDetailReqVO::toCommand).toList())
                .setDeviceParameterReadings((deviceParameterReadings == null ? List.<DeviceParameterReadingReqVO>of()
                        : deviceParameterReadings).stream()
                        .map(DeviceParameterReadingReqVO::toCommand).toList())
                .setChangeReason(changeReason)
                .setSignaturePassword(signaturePassword);
    }

    @Data
    @Accessors(chain = true)
    public static class LossDetailReqVO {
        @NotNull(message = "损耗原因不能为空")
        private Long reasonId;

        @NotNull(message = "损耗数量不能为空")
        @DecimalMin(value = "0.001", message = "损耗数量必须大于 0")
        private BigDecimal quantity;

        private MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand toCommand() {
            return new MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand()
                    .setReasonId(reasonId)
                    .setQuantity(quantity);
        }
    }

    @Data
    @Accessors(chain = true)
    public static class DeviceParameterReadingReqVO {
        private Long deviceId;

        private String parameterCode;

        private BigDecimal value;

        private MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand toCommand() {
            return new MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand()
                    .setDeviceId(deviceId)
                    .setParameterCode(parameterCode)
                    .setValue(value);
        }
    }
}
