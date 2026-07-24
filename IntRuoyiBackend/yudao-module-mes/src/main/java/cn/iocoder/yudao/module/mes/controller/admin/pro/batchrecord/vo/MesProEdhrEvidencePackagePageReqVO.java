package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 交付证据包分页 Request VO")
@Data
public class MesProEdhrEvidencePackagePageReqVO extends PageParam {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Schema(description = "证据包状态", example = "MISSING")
    private String packageStatus;

    @Schema(description = "证据状态", example = "MISSING")
    private String evidenceStatus;
}
