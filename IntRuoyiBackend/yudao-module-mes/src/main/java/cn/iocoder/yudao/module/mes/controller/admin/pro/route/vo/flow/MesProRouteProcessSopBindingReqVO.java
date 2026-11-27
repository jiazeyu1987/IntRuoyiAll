package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 仅修改候选版本中的工序 SOP，不修改共享工序主数据。 */
@Data
public class MesProRouteProcessSopBindingReqVO {
    @NotNull
    private Long routeProcessId;
    @NotNull
    private Long processId;
    @Schema(description = "SOP 文件地址，null 表示解除当前候选版本中的绑定")
    @Size(max = 2048)
    @Pattern(regexp = "^(https?://|/)[^\\s]+$", message = "SOP 文件地址必须为 HTTP(S) 地址或站内路径")
    private String sopFileUrl;
}
