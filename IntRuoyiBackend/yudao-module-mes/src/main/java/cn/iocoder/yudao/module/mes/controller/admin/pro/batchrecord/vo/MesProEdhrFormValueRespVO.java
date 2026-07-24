package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES eDHR 独立表单字段值 Response VO")
@Data
public class MesProEdhrFormValueRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "实例 ID")
    private Long instanceId;

    @Schema(description = "字段键")
    private String fieldKey;

    @Schema(description = "字段标签")
    private String fieldLabel;

    @Schema(description = "字段类型")
    private String fieldType;

    @Schema(description = "字段值文本")
    private String valueText;

    @Schema(description = "字段值 JSON")
    private String valueJson;
}
