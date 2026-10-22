package cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class MesProCraftBindingTargetReqVO {
    @NotNull private Long routeVersionId;
    @NotNull private Long routeProcessId;
    @NotBlank private String expectedRouteSnapshotSha256;
}
