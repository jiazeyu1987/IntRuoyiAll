package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - PQC 检验项目设备配置保存 Request VO")
@Data
public class MesPqcItemEquipmentConfigSaveReqVO {

    @Schema(description = "检验项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "检验项目编号不能为空")
    private String itemCode;

    @Schema(description = "检验项目名称快照")
    private String itemNameSnapshot;

    @Schema(description = "检验设备组")
    @Valid
    private List<EquipmentGroup> equipmentGroups;

    @Data
    public static class EquipmentGroup {

        @Schema(description = "设备台账ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "检验设备不能为空")
        private Long equipmentId;

        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "默认排序标识")
        private Boolean defaultFlag;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "设备编号列表")
        @Valid
        private List<EquipmentNumber> equipmentNumbers;
    }

    @Data
    public static class EquipmentNumber {

        @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "设备编号不能为空")
        private String equipmentNumber;

        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "排序")
        private Integer sort;
    }
}
