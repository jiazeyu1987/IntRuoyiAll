package cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 用户列表列配置 Response VO")
@Data
@Accessors(chain = true)
public class UserTableColumnConfigRespVO {

    @Schema(description = "配置版本", example = "1")
    private Integer schemaVersion;

    @Schema(description = "列表唯一标识", example = "mes.pro.scheduleOrder.main")
    private String tableKey;

    @Schema(description = "列配置")
    private List<Column> columns;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "管理后台 - 用户列表列配置列项")
    @Data
    @Accessors(chain = true)
    public static class Column {

        @Schema(description = "列唯一标识", example = "productName")
        private String key;

        @Schema(description = "是否显示", example = "true")
        private Boolean visible;

        @Schema(description = "列宽，单位 px", example = "180")
        private Integer width;

    }

}
