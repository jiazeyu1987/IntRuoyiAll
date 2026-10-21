package cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class MesProCraftBindReqVO {
    @NotNull private Long routeVersionId;
    @NotNull private Long routeProcessId;
    @NotNull private Long craftId;
    @NotBlank private String expectedRouteSnapshotSha256;
}
