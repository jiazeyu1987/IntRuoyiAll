package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线 DCC 项目代码绑定保存 Request VO")
@Data
@Accessors(chain = true)
public class MesRouteDccProjectBindingSaveReqVO {

    @Schema(description = "工艺路线ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "工艺路线ID不能为空")
    private Long routeId;

    @Schema(description = "当前编辑的草稿版本ID；传入时同时确认并解除该草稿中的产品关联")
    private Long routeVersionId;

    @Schema(description = "DCC项目代码ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    @NotNull(message = "DCC项目代码ID不能为空")
    private Long dccProjectCodeId;

    @Schema(description = "前端读取到的关系版本", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "关系版本不能为空")
    private Long expectedVersion;

    @Schema(description = "用户确认解除关联的正式产品及当前草稿产品物料ID并集；无关联产品时传空数组")
    @NotNull(message = "请确认当前关联产品后保存")
    private List<@NotNull Long> confirmedProductItemIds = List.of();
}
