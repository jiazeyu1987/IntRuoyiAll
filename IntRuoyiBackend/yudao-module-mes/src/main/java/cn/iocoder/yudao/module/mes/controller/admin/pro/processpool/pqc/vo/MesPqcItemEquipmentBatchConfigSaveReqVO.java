package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Schema(description = "管理后台 - 同名 PQC 检验项目设备配置批量保存 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MesPqcItemEquipmentBatchConfigSaveReqVO extends MesPqcItemEquipmentConfigSaveReqVO {

    @Schema(description = "当前 QA 项目代码 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "DCC 项目代码不能为空")
    private Long dccProjectCodeId;

    @Schema(description = "同名检验项目对应的全部检验项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "检验项目编号列表不能为空")
    private List<String> itemCodes;
}
