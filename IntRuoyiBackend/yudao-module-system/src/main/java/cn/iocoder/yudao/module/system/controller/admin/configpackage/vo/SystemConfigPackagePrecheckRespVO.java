package cn.iocoder.yudao.module.system.controller.admin.configpackage.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - 系统配置包导入预检 Response VO")
@Data
@Accessors(chain = true)
public class SystemConfigPackagePrecheckRespVO {

    @Schema(description = "是否可导入", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean valid;

    @Schema(description = "配置包内容校验哈希")
    private String packageSha256;

    @Schema(description = "目标环境配置快照哈希，确认导入时必须原样带回")
    private String targetSnapshotSha256;

    @Schema(description = "阻断错误")
    private List<String> blockingErrors;

    @Schema(description = "提示信息")
    private List<String> warnings;

    @Schema(description = "Sheet 差异明细")
    private List<SheetDiff> sheetDiffs;

    @Schema(description = "管理后台 - 系统配置包 Sheet 差异")
    @Data
    @Accessors(chain = true)
    public static class SheetDiff {

        @Schema(description = "Sheet 名称")
        private String sheetName;

        @Schema(description = "配置包行数")
        private Integer packageCount;

        @Schema(description = "目标环境当前行数")
        private Integer currentCount;

        @Schema(description = "新增数量")
        private Integer createCount;

        @Schema(description = "更新数量")
        private Integer updateCount;

        @Schema(description = "删除数量")
        private Integer deleteCount;
    }
}
