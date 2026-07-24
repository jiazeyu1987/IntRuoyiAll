package cn.iocoder.yudao.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - NAS 目录树读取 Response VO")
@Data
@Accessors(chain = true)
public class FileNasDirectoryTreeRespVO {

    @Schema(description = "根目录名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "quality")
    private String rootName;

    @Schema(description = "根目录完整路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "\\\\nas\\share\\quality")
    private String rootPath;

    @Schema(description = "目录总数，包含根目录", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Integer directoryCount;

    @Schema(description = "根目录下的子目录节点")
    private List<Node> children = new ArrayList<>();

    @Schema(description = "被跳过的目录列表")
    private List<SkippedNode> skipped = new ArrayList<>();

    @Schema(description = "管理后台 - NAS 目录节点")
    @Data
    @Accessors(chain = true)
    public static class Node {

        @Schema(description = "目录名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "SOP")
        private String name;

        @Schema(description = "目录完整路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "\\\\nas\\share\\quality\\SOP")
        private String path;

        @Schema(description = "子目录节点")
        private List<Node> children = new ArrayList<>();
    }

    @Schema(description = "管理后台 - NAS 被跳过目录")
    @Data
    @Accessors(chain = true)
    public static class SkippedNode {

        @Schema(description = "被跳过目录路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "#recycle")
        private String path;

        @Schema(description = "跳过原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "access_denied")
        private String reason;
    }
}
