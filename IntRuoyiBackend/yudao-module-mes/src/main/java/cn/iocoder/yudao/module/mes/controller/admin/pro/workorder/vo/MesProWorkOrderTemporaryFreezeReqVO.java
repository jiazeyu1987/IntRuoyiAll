package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 生产工单临时冻结 Request VO")
@Data
public class MesProWorkOrderTemporaryFreezeReqVO {

    @Schema(description = "是否开启临时冻结", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "临时冻结开关不能为空")
    private Boolean enabled;

}
