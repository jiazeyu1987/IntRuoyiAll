package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

@Schema(description = "管理后台 - 赋予角色数据权限 Request VO")
@Data
public class PermissionAssignRoleDataScopeReqVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @Schema(description = "数据范围，参见 DataScopeEnum 枚举类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "数据范围不能为空")
    @InEnum(value = DataScopeEnum.class, message = "数据范围必须是 {value}")
    private Integer dataScope;

    @Schema(description = "部门编号列表，非自定义部门范围时传空集合", requiredMode = Schema.RequiredMode.REQUIRED, example = "1,3,5")
    @NotNull(message = "部门编号列表不能为空")
    private Set<Long> dataScopeDeptIds;

    @Schema(description = "GxP 审计变更原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "限定 QA 角色数据访问范围")
    @NotBlank(message = "GxP 审计变更原因不能为空")
    private String reason;

    @Schema(description = "GxP 审计幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "SYSTEM-PERM-DATA-SCOPE-1-uuid")
    @NotBlank(message = "GxP 审计幂等键不能为空")
    private String idempotencyKey;

}
