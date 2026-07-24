package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrDhrTemplateImpactReqVO {

    @NotNull(message = "DHR模板ID不能为空")
    private Long id;

    @NotBlank(message = "影响范围不能为空")
    private String impactScopeJson;

    @NotNull(message = "必须确认影响范围")
    private Boolean impactConfirmed;
}
