package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线基础工序 Response VO")
@Data
public class MesProRouteProcessBaseRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long routeId;

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long processId;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer sort;

    @Schema(description = "直接前置工序")
    private MesProRouteProcessRelationRespVO predecessor;

    @Schema(description = "直接前置工序列表")
    private List<MesProRouteProcessRelationRespVO> predecessors;

    @Schema(description = "直接后续工序")
    private List<MesProRouteProcessRelationRespVO> successors;

    @Schema(description = "准备时间（分钟）", example = "10")
    private Integer prepareTime;

    @Schema(description = "等待时间（分钟）", example = "5")
    private Integer waitTime;

    @Schema(description = "甘特图显示颜色", example = "#00AEF3")
    private String colorCode;

    @Schema(description = "是否关键工序", example = "false")
    private Boolean keyFlag;

    @Schema(description = "是否质检工序", example = "false")
    private Boolean checkFlag;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
