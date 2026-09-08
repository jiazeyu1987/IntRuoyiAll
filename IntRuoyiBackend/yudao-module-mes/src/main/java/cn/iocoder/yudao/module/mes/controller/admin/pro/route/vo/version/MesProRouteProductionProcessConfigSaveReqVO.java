package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - MES 工艺路线版本生产工序配置保存 Request VO")
@Data
public class MesProRouteProductionProcessConfigSaveReqVO {

    @Schema(description = "候选路线版本编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "候选路线版本编号不能为空")
    private Long routeVersionId;

    @Schema(description = "生产工序配置 schema 版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生产工序配置 schema 版本不能为空")
    private Integer schemaVersion;

    @Schema(description = "生产工序配置列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "生产工序配置不能为空")
    private List<Map<String, Object>> productionProcessConfigs;

}
