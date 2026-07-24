package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MesProEdhrDhrTemplateImpactRespVO {

    private Long id;

    private Long templateId;

    private String actionType;

    private String impactScopeJson;

    private Boolean impactConfirmed;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    private LocalDateTime createTime;
}
