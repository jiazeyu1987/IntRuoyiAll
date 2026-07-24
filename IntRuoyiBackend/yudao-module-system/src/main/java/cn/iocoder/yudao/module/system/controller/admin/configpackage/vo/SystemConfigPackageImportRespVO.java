package cn.iocoder.yudao.module.system.controller.admin.configpackage.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Schema(description = "管理后台 - 系统配置包导入 Response VO")
@Data
@Accessors(chain = true)
public class SystemConfigPackageImportRespVO {

    @Schema(description = "是否已恢复")
    private Boolean restored;

    @Schema(description = "导入前目标快照哈希")
    private String targetSnapshotSha256;

    @Schema(description = "各 Sheet 导入行数")
    private Map<String, Integer> restoredCounts;
}
