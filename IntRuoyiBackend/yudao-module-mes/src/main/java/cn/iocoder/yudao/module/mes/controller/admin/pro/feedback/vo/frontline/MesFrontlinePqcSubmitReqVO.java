package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
    @NotNull(message = "活跃订单编号不能为空")
    private Long activeOrderId;

    @Schema(description = "PQC 检验任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "PQC 检验任务不能为空")
    private Long pqcTaskId;

    @Schema(description = "QA 规程发布版本编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "QA 规程发布版本不能为空")
    private Long regulationVersionId;

    @Schema(description = "生产工单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生产工单编号不能为空")
    private Long workOrderId;

    @Schema(description = "绑定的生产提交工序池事件编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "绑定的生产提交事件不能为空")
    private Long productionSubmitEventId;

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线编号不能为空")
    private Long routeId;

    @Schema(description = "工艺路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线工序编号不能为空")
    private Long routeProcessId;

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工序编号不能为空")
    private Long processId;

    @Schema(description = "检验类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "检验类型不能为空")
    private String inspectionType;

    @Schema(description = "业务日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业务日期不能为空")
    private LocalDate businessDate;

    @Schema(description = "班次编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "班次编码不能为空")
    private String shiftCode;

    @Schema(description = "轮次", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "轮次不能为空")
    private Integer roundNo;

    @Schema(description = "实际检验数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "实际检验数量不能为空")
    private Integer actualInspectionQuantity;

    @Schema(description = "实际填写员工编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "实际填写员工不能为空")
    private Long actualEmployeeId;

    @Schema(description = "设备账号编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备账号不能为空")
    private Long deviceAccountId;

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备不能为空")
    private Long deviceId;

    @Schema(description = "工作站编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工作站不能为空")
    private Long workstationId;

    @Schema(description = "PQC 提交幂等键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "PQC 提交幂等键不能为空")
    private String pqcSubmissionIdempotencyKey;

    @Schema(description = "电子签名编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "电子签名不能为空")
    private Long signatureId;

    @Schema(description = "电子签名员工编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "电子签名员工不能为空")
    private Long signatureEmployeeId;

    @Schema(description = "电子签名快照")
    private String signatureSnapshot;

    @Schema(description = "模板类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @Schema(description = "PQC 检验结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "PQC 检验结果不能为空")
    private String inspectionResult;

    @Schema(description = "PQC 手动不良说明")
    private String nonconformanceDescription;

    @Schema(description = "PQC 项目级正式检验结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "PQC 项目级检验结果不能为空")
    private List<ItemResult> itemResults;

    @Schema(description = "PQC 原始提交内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "PQC 原始提交内容不能为空")
    private Map<String, Object> rawPayload;

    @Schema(description = "客户端提交时间")
    private LocalDateTime clientSubmitTime;

    @Schema(description = "PQC 项目级检验结果")
    @Data
    public static class ItemResult {

        @Schema(description = "检验项目编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验项目编码不能为空")
        private String itemCode;

        @Schema(description = "实际检验设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "检验设备不能为空")
        private Long selectedEquipmentId;

        @Schema(description = "实际检验设备编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验设备编号不能为空")
        private String selectedEquipmentNumber;

        @Schema(description = "逐件样本值", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "逐件样本值不能为空")
        private List<String> sampleValues;
    }
}
