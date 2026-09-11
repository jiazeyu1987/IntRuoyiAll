package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

@Schema(description = "管理后台 - 赋予角色菜单 Request VO")
@Data
public class PermissionAssignRoleMenuReqVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @Schema(description = "菜单编号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "1,3,5")
    @NotNull(message = "菜单编号列表不能为空")
    private Set<Long> menuIds;

    @Schema(description = "GxP 审计变更原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "新增 QA 审计菜单权限")
    @NotBlank(message = "GxP 审计变更原因不能为空")
    private String reason;

    @Schema(description = "GxP 审计幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "SYSTEM-PERM-ROLE-MENU-1-uuid")
    @NotBlank(message = "GxP 审计幂等键不能为空")
    private String idempotencyKey;

}
