package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrLabelPreviewReqVO {

    @NotNull(message = "标签模板ID不能为空")
    private Long templateId;

    @NotBlank(message = "业务对象类型不能为空")
    private String businessType;

    @NotNull(message = "业务对象ID不能为空")
    private Long businessObjectId;

    @NotBlank(message = "业务对象编码不能为空")
    private String businessObjectCode;

    @NotBlank(message = "业务对象字段快照不能为空")
    private String businessObjectPayloadJson;
}
