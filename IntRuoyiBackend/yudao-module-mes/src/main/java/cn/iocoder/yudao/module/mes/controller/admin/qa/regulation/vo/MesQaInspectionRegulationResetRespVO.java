package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - MES QA 检验规程测试重置 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesQaInspectionRegulationResetRespVO {

    @Schema(description = "DCC 项目代码 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "112")
    private Long dccProjectCodeId;

    @Schema(description = "被重置的 QA 规程 ID；项目原本未配置时为空", example = "60")
    private Long regulationId;

    @Schema(description = "软删除的版本数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer versionCount;

    @Schema(description = "软删除的 QA 工序数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    private Integer processCount;

    @Schema(description = "软删除的检验项目行数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "40")
    private Integer itemCount;

    @Schema(description = "软删除的设备绑定行数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "6")
    private Integer itemEquipmentCount;
}
