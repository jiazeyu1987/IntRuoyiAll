package cn.iocoder.yudao.module.system.controller.admin.permission.vo.rolecategory;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 角色分类创建/更新 Request VO")
@Data
public class RoleCategorySaveReqVO {

    @Schema(description = "分类编号", example = "1")
    private Long id;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "展厅")
    @NotBlank(message = "角色分类名称不能为空")
    @Size(max = 30, message = "角色分类名称长度不能超过 30 个字符")
    private String name;

    @Schema(description = "分类标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "showroom")
    @NotBlank(message = "角色分类标识不能为空")
    @Size(max = 64, message = "角色分类标识长度不能超过 64 个字符")
    private String code;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "显示顺序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "备注", example = "展厅相关权限角色")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}
