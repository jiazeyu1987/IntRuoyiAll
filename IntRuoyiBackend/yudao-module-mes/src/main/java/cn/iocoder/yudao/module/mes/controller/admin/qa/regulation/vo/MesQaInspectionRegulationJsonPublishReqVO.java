package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 表单解析 QA 检验规程 JSON 发布 Request VO")
@Data
public class MesQaInspectionRegulationJsonPublishReqVO {

    @Schema(description = "DCC 项目代码 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "DCC 项目代码不能为空")
    private Long dccProjectCodeId;

    @Schema(description = "当前 QA 检验规程解析 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "QA 检验规程 JSON 不能为空")
    private Map<String, Object> recognitionJson;
}
