package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - MES eDHR 独立表单草稿保存 Request VO")
@Data
public class MesProEdhrFormInstanceSaveDraftReqVO {

    @Schema(description = "实例 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "实例 ID 不能为空")
    private Long id;

    @Schema(description = "字段值")
    private Map<String, Object> values;

    @Schema(description = "备注")
    private String remark;
}
