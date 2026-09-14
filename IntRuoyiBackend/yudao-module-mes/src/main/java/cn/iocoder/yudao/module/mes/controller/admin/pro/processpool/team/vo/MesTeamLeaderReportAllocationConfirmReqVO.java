package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 班组长报工分配确认 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderReportAllocationConfirmReqVO {

    @Schema(description = "工序池提交事件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull
    private Long eventId;

    @Schema(description = "班组长类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCTION")
    @NotBlank
    private String leaderType;

    @Schema(description = "分配模式，FIFO 或 MANUAL", requiredMode = Schema.RequiredMode.REQUIRED, example = "MANUAL")
    @NotBlank
    private String allocationMode;

    @Schema(description = "复核说明", example = "现场调整")
    private String reviewRemark;

    @Schema(description = "电子签名密码，复核入口提交时提供", example = "******")
    private String signaturePassword;

    @Schema(description = "客户端读取的分配版本", example = "1")
    private Integer expectedVersion;

    @Schema(description = "请求幂等键", example = "allocation-1001-uuid")
    private String idempotencyKey;

    @Schema(description = "分配明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    private List<MesTeamLeaderReportAllocationLineReqVO> allocations;
}
