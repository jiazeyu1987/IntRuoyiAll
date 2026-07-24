package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MesProEdhrLabelTemplateCreateReqVO {

    @NotBlank(message = "标签模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "标签模板名称不能为空")
    private String templateName;

    @NotBlank(message = "标签模板版本不能为空")
    private String templateVersion;

    @NotBlank(message = "业务对象类型不能为空")
    private String businessObjectType;

    @NotBlank(message = "字段模型不能为空")
    private String fieldModelJson;

    @NotBlank(message = "布局不能为空")
    private String layoutJson;

    @NotBlank(message = "解析版本不能为空")
    private String parserVersion;

    private String watermarkTemplate;

    private String remark;
}
