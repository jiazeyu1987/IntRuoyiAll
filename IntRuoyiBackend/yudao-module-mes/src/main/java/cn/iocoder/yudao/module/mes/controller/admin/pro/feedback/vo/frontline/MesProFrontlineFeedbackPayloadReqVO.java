package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 一线报工提交报工载荷 Request VO")
@Data
@Accessors(chain = true)
public class MesProFrontlineFeedbackPayloadReqVO {

    @Schema(description = "报工单编号；为空时由服务端正式编码规则生成", example = "FB-F2-001")
    private String code;

    @Schema(description = "报工类型；为空时服务端按自行报工生成", example = "1")
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

    @Schema(description = "生产工单编号；一线生产不匹配工单时为空", example = "41")
    private Long workOrderId;

    @Schema(description = "生产任务编号；一线生产不匹配任务时为空", example = "51")
    private Long taskId;

    @Schema(description = "排产工单编号", example = "81")
    private Long scheduleOrderId;

    @Schema(description = "排产工单工序编号", example = "82")
    private Long scheduleOrderProcessId;

    @Schema(description = "产品物料编号；一线生产不匹配工单时为空", example = "61")
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

    @Valid
    @Schema(description = "损耗原因明细，数量合计必须等于损耗数量")
    private List<LossDetailReqVO> lossDetails;

    @Valid
    @Schema(description = "选用设备快照")
    private SelectedDeviceReqVO selectedDevice;

    @Valid
    @Schema(description = "选用设备参数读数")
    private List<DeviceParameterReadingReqVO> deviceParameterReadings;

    @Schema(description = "当前审批人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "7001")
    @NotNull(message = "当前审批人不能为空")
    private Long approveUserId;

    @Schema(description = "备注", example = "frontline production")
    private String remark;

    @Data
    @Accessors(chain = true)
    public static class LossDetailReqVO {

        @Schema(description = "损耗原因编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8301")
        @NotNull(message = "损耗原因不能为空")
        private Long reasonId;

        @Schema(description = "损耗原因编码", example = "LOSS-001")
        private String reasonCode;

        @Schema(description = "损耗原因名称", example = "正常损耗")
        private String reasonName;

        @Schema(description = "损耗数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2.500")
        @NotNull(message = "损耗原因数量不能为空")
        private BigDecimal quantity;
    }

    @Data
    @Accessors(chain = true)
    public static class SelectedDeviceReqVO {

        @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "501")
        @NotNull(message = "选用设备不能为空")
        private Long deviceId;

        @Schema(description = "设备编码", example = "PT-A-03")
        private String deviceCode;

        @Schema(description = "设备名称", example = "压力泵")
        private String deviceName;
    }

    @Data
    @Accessors(chain = true)
    public static class DeviceParameterReadingReqVO {

        @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "501")
        @NotNull(message = "设备参数所属设备不能为空")
        private Long deviceId;

        @Schema(description = "设备编码", example = "PT-A-03")
        private String deviceCode;

        @Schema(description = "设备名称", example = "压力泵")
        private String deviceName;

        @Schema(description = "参数编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "pressure")
        @NotNull(message = "设备参数编码不能为空")
        private String parameterCode;

        @Schema(description = "参数名称", example = "压力")
        private String parameterName;

        @Schema(description = "参数单位", example = "MPa")
        private String unit;

        @Schema(description = "参数读数", example = "50")
        private BigDecimal value;

        @Schema(description = "参数文本值，用于下拉、文本类设备参数", example = "合格")
        private String textValue;

        @Schema(description = "配置下限", example = "20")
        private BigDecimal lowerLimit;

        @Schema(description = "配置上限", example = "40")
        private BigDecimal upperLimit;

        @Schema(description = "参数状态：NORMAL/BELOW_LOWER/ABOVE_UPPER", example = "ABOVE_UPPER")
        private String parameterStatus;
    }

}
