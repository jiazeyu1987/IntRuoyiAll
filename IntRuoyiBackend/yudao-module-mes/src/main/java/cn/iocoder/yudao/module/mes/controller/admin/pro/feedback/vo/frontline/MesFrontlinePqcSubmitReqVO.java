package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - MES PQC 检验提交 Request VO")
@Data
public class MesFrontlinePqcSubmitReqVO {

    @Schema(description = "活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long activeOrderId;

    @Schema(description = "PQC 检验任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "PQC 检验任务不能为空")
    private Long pqcTaskId;

    @Schema(description = "QA 规程发布版本编号")
    private Long regulationVersionId;

    @Schema(description = "生产工单编号")
    private Long workOrderId;

    @Schema(description = "工艺路线编号")
    private Long routeId;

    @Schema(description = "QA 工序编号")
    private Long qaProcessId;

    @Schema(description = "检验类型")
    private String inspectionType;

    @Schema(description = "业务日期")
    private LocalDate businessDate;

    @Schema(description = "班次编码")
    private String shiftCode;

    @Schema(description = "轮次")
    private Integer roundNo;

    @Schema(description = "实际检验数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "实际检验数量不能为空")
    @Min(value = 1, message = "实际检验数量必须大于 0")
    private Integer actualInspectionQuantity;

    @Schema(description = "电子签名密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "电子签名密码不能为空")
    private String signaturePassword;

    @Schema(description = "结构化损耗数量")
    @Min(value = 0, message = "损耗数量不能小于 0")
    private Integer scrapQuantity;

    @Schema(description = "PQC 手动不良说明")
    private String nonconformanceDescription;

    @Schema(description = "PQC 项目级正式检验结果")
    private List<ItemResult> itemResults;

    @Schema(description = "PQC 原始提交内容")
    private Map<String, Object> rawPayload;

    @Schema(description = "客户端提交时间")
    private LocalDateTime clientSubmitTime;

    @Schema(description = "PQC 项目级检验结果")
    @Data
    public static class ItemResult {

        @Schema(description = "检验项目编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验项目编码不能为空")
        private String itemCode;

        @Schema(description = "实际检验设备ID；选择检验设备时传")
        private Long selectedEquipmentId;

        @Schema(description = "实际检验设备编号；选择检验设备时传")
        private String selectedEquipmentNumber;

        @Schema(description = "逐件样本值", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "逐件样本值不能为空")
        private List<String> sampleValues;
    }
}
