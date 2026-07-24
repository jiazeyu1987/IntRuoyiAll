package cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 供应商准入启停 Request VO")
@Data
public class SrmSupplierAccessEnableReqVO {

    @Schema(description = "准入档案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "准入档案编号不能为空")
    private Long id;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    @Schema(description = "操作备注", example = "恢复合作资格")
    private String operationRemark;
}
