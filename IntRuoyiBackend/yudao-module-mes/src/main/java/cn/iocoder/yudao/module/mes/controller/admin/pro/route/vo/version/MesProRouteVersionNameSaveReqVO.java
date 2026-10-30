package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "管理后台 - 工艺路线草稿名称保存")
public class MesProRouteVersionNameSaveReqVO {
    @NotNull(message = "候选路线版本编号不能为空")
    private Long routeVersionId;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "当前候选路线快照 SHA-256 不能为空")
    private String expectedRouteSnapshotSha256;
}
