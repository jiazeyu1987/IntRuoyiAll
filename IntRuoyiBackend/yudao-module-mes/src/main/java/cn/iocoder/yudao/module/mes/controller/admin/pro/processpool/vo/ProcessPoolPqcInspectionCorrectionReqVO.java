package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolPqcInspectionCorrectionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES PQC 表单修改 Request VO")
@Data
public class ProcessPoolPqcInspectionCorrectionReqVO {

    @Schema(description = "工序池 PQC 提交事件编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工序池提交事件不能为空")
    private Long eventId;

    @Schema(description = "实际检验数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "实际检验数量不能为空")
    @Min(value = 1, message = "实际检验数量必须大于 0")
    private Integer actualInspectionQuantity;

    @Schema(description = "损耗数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "损耗数量不能为空")
    @Min(value = 0, message = "损耗数量不能小于 0")
    private Integer scrapQuantity;

    @Schema(description = "PQC 项目级检验结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "PQC 检验项目不能为空")
    @Valid
    private List<ItemResultReqVO> itemResults;

    @Schema(description = "修改原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "修改原因不能为空")
    private String changeReason;

    @Schema(description = "签名密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "签名密码不能为空")
    private String signaturePassword;

    public MesProcessPoolPqcInspectionCorrectionCommand toCommand() {
        return new MesProcessPoolPqcInspectionCorrectionCommand()
                .setEventId(eventId)
                .setActualInspectionQuantity(actualInspectionQuantity)
                .setScrapQuantity(scrapQuantity)
                .setItemResults(itemResults.stream()
                        .map(ItemResultReqVO::toCommand)
                        .toList())
                .setChangeReason(changeReason)
                .setSignaturePassword(signaturePassword);
    }

    @Schema(description = "PQC 项目级检验结果")
    @Data
    public static class ItemResultReqVO {

        @Schema(description = "检验项目编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验项目编码不能为空")
        private String itemCode;

        @Schema(description = "实际检验设备ID")
        private Long selectedEquipmentId;

        @Schema(description = "实际检验设备编号")
        private String selectedEquipmentNumber;

        @Schema(description = "逐件样本值", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "逐件样本值不能为空")
        private List<@NotBlank(message = "逐件样本值不能为空") String> sampleValues;

        private MesProcessPoolPqcInspectionCorrectionCommand.ItemResultCommand toCommand() {
            return new MesProcessPoolPqcInspectionCorrectionCommand.ItemResultCommand()
                    .setItemCode(itemCode)
                    .setSelectedEquipmentId(selectedEquipmentId)
                    .setSelectedEquipmentNumber(selectedEquipmentNumber)
                    .setSampleValues(sampleValues);
        }
    }
}
