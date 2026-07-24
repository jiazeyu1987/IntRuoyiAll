package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrLabelTemplateRespVO {

    private Long id;

    private String templateCode;

    private String templateName;

    private String templateVersion;

    private String businessObjectType;

    private String fieldModelJson;

    private String layoutJson;

    private String parserVersion;

    private String watermarkTemplate;

    private String status;

    private LocalDateTime activeAt;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
