package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrDhrTemplateLifecycleReqVO {

    @NotNull(message = "DHR模板ID不能为空")
    private Long id;
}
