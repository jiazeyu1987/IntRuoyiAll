package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - PQC 检验项目设备配置 Response VO")
@Data
@Accessors(chain = true)
public class MesPqcItemEquipmentConfigRespVO {

    @Schema(description = "检验项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemCode;

    @Schema(description = "检验项目名称")
    private String itemName;

    @Schema(description = "检验设备组")
    private List<EquipmentGroup> equipmentGroups;

    @Data
    @Accessors(chain = true)
    public static class EquipmentGroup {

        @Schema(description = "配置编号")
        private Long id;

        @Schema(description = "设备台账ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long equipmentId;

        @Schema(description = "设备编码")
        private String equipmentCode;

        @Schema(description = "设备名称")
        private String equipmentName;

        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "默认排序标识")
        private Boolean defaultFlag;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "设备编号列表")
        private List<EquipmentNumber> equipmentNumbers;
    }

    @Data
    @Accessors(chain = true)
    public static class EquipmentNumber {

        @Schema(description = "编号配置ID")
        private Long id;

        @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED)
        private String equipmentNumber;

        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "排序")
        private Integer sort;
    }
}
