package cn.iocoder.yudao.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - NAS 目录浏览 Response VO")
@Data
@Accessors(chain = true)
public class FileNasListRespVO {

    @Schema(description = "当前相对路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    private String currentPath = "";

    @Schema(description = "父级相对路径", example = "QMS")
    private String parentPath;

    @Schema(description = "NAS 共享根 UNC 路径", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "\\\\172.30.30.4\\it共享")
    private String rootPath = "";

    @Schema(description = "当前目录下的文件和目录列表")
    private List<Item> items = new ArrayList<>();

    @Schema(description = "管理后台 - NAS 条目")
    @Data
    @Accessors(chain = true)
    public static class Item {

        @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.QMS documents")
        private String name;

        @Schema(description = "相对路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "QMS/1.QMS documents")
        private String path;

        @Schema(description = "是否目录", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        private Boolean dir;

        @Schema(description = "是否系统目录/文件", example = "false")
        private Boolean system;

        @Schema(description = "是否隐藏目录/文件", example = "false")
        private Boolean hidden;

        @Schema(description = "大小，目录固定为 0", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
        private Long size;

        @Schema(description = "最后修改时间戳（毫秒）", example = "1710000000000")
        private Long modifiedAt;
    }
}
