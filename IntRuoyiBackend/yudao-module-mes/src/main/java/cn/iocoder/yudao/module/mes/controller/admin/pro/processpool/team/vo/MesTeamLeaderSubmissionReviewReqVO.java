package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长复核员工提交 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderSubmissionReviewReqVO {

    @Schema(description = "工序池提交事件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull
    private Long eventId;

    @Schema(description = "班组长类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PQC")
    @NotBlank
    private String leaderType;

    @Schema(description = "复核状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVED")
    @NotBlank
    private String reviewStatus;

    @Schema(description = "复核说明", example = "已复核")
    private String reviewRemark;
}
