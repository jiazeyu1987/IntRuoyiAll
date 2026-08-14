package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序开始生产组长 Response VO")
@Data
@Accessors(chain = true)
public class MesProRouteStartProductionLeaderRespVO {

    @Schema(description = "负责范围 ID，当前等于工艺路线 ID", example = "7001")
    private Long productionLineId;

    @Schema(description = "负责范围编码，当前等于工艺路线编码", example = "LINE-A")
    private String productionLineCode;

    @Schema(description = "负责范围名称，当前等于工艺路线名称", example = "压力泵一线")
    private String productionLineName;

    @Schema(description = "候选来源类型：USERS/ROLE", example = "USERS")
    private String candidateSourceType;

    @Schema(description = "候选来源 ID 列表")
    private List<Long> candidateSourceIds;

    @Schema(description = "候选来源名称快照")
    private List<String> candidateSourceNames;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
