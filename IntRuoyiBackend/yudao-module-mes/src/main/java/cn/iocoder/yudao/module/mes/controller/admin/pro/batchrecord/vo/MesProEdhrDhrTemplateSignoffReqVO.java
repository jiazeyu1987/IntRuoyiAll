package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrDhrTemplateSignoffReqVO {

    @NotNull(message = "DHR模板ID不能为空")
    private Long id;

    @NotBlank(message = "DHR模板签核证据不能为空")
    private String signoffEvidenceHash;
}
