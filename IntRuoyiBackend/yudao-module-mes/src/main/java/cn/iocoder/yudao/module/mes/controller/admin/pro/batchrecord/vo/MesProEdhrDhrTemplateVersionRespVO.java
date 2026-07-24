package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MesProEdhrDhrTemplateVersionRespVO {

    private Long id;

    private Long templateId;

    private String versionNo;

    private String templateSnapshotJson;

    private String changeSummary;

    private LocalDateTime createTime;
}
