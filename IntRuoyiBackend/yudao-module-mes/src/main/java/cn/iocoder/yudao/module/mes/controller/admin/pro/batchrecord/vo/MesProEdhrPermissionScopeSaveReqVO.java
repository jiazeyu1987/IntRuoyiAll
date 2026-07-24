package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionScopeSaveReqVO {

    private Long scopeId;

    @NotBlank(message = "权限范围名称不能为空")
    private String scopeName;

    @NotBlank(message = "对象类型不能为空")
    private String objectType;

    @NotBlank(message = "对象编号不能为空")
    private String objectId;

    private Long parentScopeId;

    private Integer expectedVersion;

    @Valid
    private List<MesProEdhrPermissionRuleSaveReqVO> rules;
}
