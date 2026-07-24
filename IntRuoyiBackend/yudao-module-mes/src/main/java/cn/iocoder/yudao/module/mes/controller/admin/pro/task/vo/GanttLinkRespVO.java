package cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 甘特图依赖线 Response VO")
@Data
@Accessors(chain = true)
public class GanttLinkRespVO {

    @Schema(description = "依赖线 ID", example = "1")
    private String id;

    @Schema(description = "源节点 ID", example = "303_1")
    private String source;

    @Schema(description = "目标节点 ID", example = "303_2")
    private String target;

    @Schema(description = "依赖类型", example = "0")
    private String type;

}
