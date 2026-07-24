package cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - 用户列表列配置保存 Request VO")
@Data
@Accessors(chain = true)
public class UserTableColumnConfigSaveReqVO {

    @Schema(description = "列表唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "mes.pro.scheduleOrder.main")
    @NotBlank(message = "tableKey 不能为空")
    private String tableKey;

    @Schema(description = "列配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "columns 不能为空")
    private List<Column> columns;

    @Schema(description = "管理后台 - 用户列表列配置保存列项")
    @Data
    @Accessors(chain = true)
    public static class Column {

        @Schema(description = "列唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "productName")
        @NotBlank(message = "column key 不能为空")
        private String key;

        @Schema(description = "是否显示", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        @NotNull(message = "visible 不能为空")
        private Boolean visible;

        @Schema(description = "列宽，单位 px", example = "180")
        private Integer width;

    }

}
