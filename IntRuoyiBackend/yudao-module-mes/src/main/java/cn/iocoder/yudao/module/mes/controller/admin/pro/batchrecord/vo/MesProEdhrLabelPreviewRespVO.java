package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrLabelPreviewRespVO {

    private Long templateId;

    private String templateCode;

    private String templateVersion;

    private String businessType;

    private Long businessObjectId;

    private String businessObjectCode;

    private String parserVersion;

    private String renderSnapshotJson;
}
