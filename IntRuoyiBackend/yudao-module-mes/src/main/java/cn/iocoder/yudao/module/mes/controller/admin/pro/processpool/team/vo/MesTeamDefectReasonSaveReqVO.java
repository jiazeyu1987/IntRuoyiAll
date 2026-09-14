package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组不良原因新增 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamDefectReasonSaveReqVO {

    @Schema(description = "路线工序编号", example = "5001")
    private Long routeProcessId;

    @Schema(description = "工序编号", example = "6001")
    private Long processId;

    @Schema(description = "原因类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOSS")
    @NotBlank
    private String reasonType;

    @Schema(description = "原因编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOSS-001")
    @NotBlank
    private String reasonCode;

    @Schema(description = "原因名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "损耗")
    @NotBlank
    private String reasonName;
}
