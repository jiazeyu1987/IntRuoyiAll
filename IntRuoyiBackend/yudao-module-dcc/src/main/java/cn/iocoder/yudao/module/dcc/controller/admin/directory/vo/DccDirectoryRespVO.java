package cn.iocoder.yudao.module.dcc.controller.admin.directory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - DCC 目录 Response VO")
@Data
public class DccDirectoryRespVO {

    @Schema(description = "目录编号", example = "1")
    private Long id;
    @Schema(description = "父目录编号", example = "100")
    private Long parentId;
    @Schema(description = "目录编码", example = "SOP_LIBRARY")
    private String code;
    @Schema(description = "目录名称", example = "SOP库")
    private String name;
    @Schema(description = "是否启用", example = "true")
    private Boolean active;
    @Schema(description = "排序", example = "1")
    private Integer sort;
    @Schema(description = "备注", example = "质量体系目录")
    private String remark;
    @Schema(description = "是否存在可见子目录", example = "true")
    private Boolean hasChildren;
    @Schema(description = "目录路径", example = "DMR/图纸")
    private String directoryPath;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "子目录")
    private List<DccDirectoryRespVO> children;
}
