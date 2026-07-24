package cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - SRM NAS定位文件 Response VO")
@Data
public class SrmNasLocatorFileRespVO {

    @Schema(description = "索引记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "质量手册.docx")
    private String fileName;

    @Schema(description = "NAS目录", requiredMode = Schema.RequiredMode.REQUIRED, example = "受控文件/程序文件")
    private String directoryPath;

    @Schema(description = "完整相对路径", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "受控文件/程序文件/质量手册.docx")
    private String fullPath;

    @Schema(description = "文件大小，单位字节", example = "1024")
    private Long size;

    @Schema(description = "最后修改时间戳（毫秒）", example = "1710000000000")
    private Long modifiedAt;
}
