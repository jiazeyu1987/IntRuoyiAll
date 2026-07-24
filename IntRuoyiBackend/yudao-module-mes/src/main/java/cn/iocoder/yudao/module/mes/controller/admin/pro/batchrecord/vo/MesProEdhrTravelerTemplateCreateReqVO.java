package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrTravelerTemplateCreateReqVO {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "模板版本不能为空")
    private String templateVersion;

    private String applicableProductCode;

    private Long applicableRouteId;

    private String applicableRouteCode;

    private Long applicableProcessId;

    private String applicableProcessCode;

    private String applicableProcessName;

    private String remark;
}
