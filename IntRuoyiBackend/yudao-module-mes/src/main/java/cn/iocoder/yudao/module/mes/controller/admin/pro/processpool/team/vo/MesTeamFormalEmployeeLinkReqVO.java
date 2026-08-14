package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产人员正式工关联 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamFormalEmployeeLinkReqVO {

    @Schema(description = "正式工系统用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "正式工系统用户编号不能为空")
    private Long systemUserId;

    @Schema(description = "显示名；为空时使用用户昵称/账号", example = "张三-A")
    private String displayName;
}
