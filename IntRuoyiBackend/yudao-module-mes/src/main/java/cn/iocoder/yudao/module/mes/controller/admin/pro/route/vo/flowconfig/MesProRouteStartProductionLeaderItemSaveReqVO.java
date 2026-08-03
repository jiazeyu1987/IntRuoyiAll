package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序开始生产组长明细保存 Request VO")
@Data
@Accessors(chain = true)
public class MesProRouteStartProductionLeaderItemSaveReqVO {

    @Schema(description = "负责产线 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "7001")
    @NotNull(message = "生产组长负责产线不能为空")
    private Long productionLineId;

    @Schema(description = "候选来源类型：USERS/ROLE", requiredMode = Schema.RequiredMode.REQUIRED, example = "USERS")
    @NotEmpty(message = "生产组长来源不能为空")
    private String candidateSourceType;

    @Schema(description = "候选来源 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "生产组长不能为空")
    private List<Long> candidateSourceIds;

    @Schema(description = "候选来源名称快照")
    private List<String> candidateSourceNames;

    @Schema(description = "备注")
    private String remark;
}
