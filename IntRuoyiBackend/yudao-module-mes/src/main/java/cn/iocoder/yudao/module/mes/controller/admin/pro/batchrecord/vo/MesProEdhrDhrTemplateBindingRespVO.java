package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;

@Data
public class MesProEdhrDhrTemplateBindingRespVO {

    private Long id;

    private Long templateId;

    private String bindingType;

    private Long bindingObjectId;

    private String bindingObjectCode;

    private String bindingObjectName;
}
