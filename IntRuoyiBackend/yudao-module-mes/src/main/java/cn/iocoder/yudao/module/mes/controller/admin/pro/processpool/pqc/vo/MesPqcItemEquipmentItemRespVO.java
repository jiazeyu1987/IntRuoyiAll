package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.pqc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - PQC 可配置检验项目 Response VO")
@Data
@Accessors(chain = true)
public class MesPqcItemEquipmentItemRespVO {

    @Schema(description = "DCC 项目代码 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dccProjectCodeId;

    @Schema(description = "检验项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemCode;

    @Schema(description = "同名检验项目对应的全部检验项目编号")
    private List<String> itemCodes;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String projectName;

    @Schema(description = "检验项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemName;

    @Schema(description = "检验方法")
    private String inspectionMethod;

    @Schema(description = "接收标准")
    private String standardText;

    @Schema(description = "抽样规则")
    private String samplingPlanText;
}
