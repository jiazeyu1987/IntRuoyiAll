package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 生产工单单行临时冻结更新 Request VO")
@Data
public class MesProWorkOrderUpdateTemporaryFrozenReqVO {

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工单编号不能为空")
    private Long id;

    @Schema(description = "是否临时冻结", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "临时冻结状态不能为空")
    private Boolean temporaryFrozen;

}
